package mechanist;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/** UDP STUN discovery and lightweight direct-connect hole-punch handshake helper for fallback hosting. */
final class NatTraversalManager implements AutoCloseable {
    static final String DEFAULT_STUN_HOST = "stun.l.google.com";
    static final int DEFAULT_STUN_PORT = 19302;
    private static final int STUN_BINDING_REQUEST = 0x0001;
    private static final int STUN_BINDING_SUCCESS = 0x0101;
    private static final int STUN_XOR_MAPPED_ADDRESS = 0x0020;
    private static final int STUN_MAPPED_ADDRESS = 0x0001;
    private static final int STUN_MAGIC_COOKIE = 0x2112A442;

    private final ExecutorService ioExecutor;
    private final SecureRandom secureRandom = new SecureRandom();
    private final String stunHost;
    private final int stunPort;

    NatTraversalManager() { this(DEFAULT_STUN_HOST, DEFAULT_STUN_PORT); }

    NatTraversalManager(String stunHost, int stunPort) {
        this.stunHost = stunHost == null || stunHost.isBlank() ? DEFAULT_STUN_HOST : stunHost.trim();
        this.stunPort = stunPort <= 0 || stunPort > 65535 ? DEFAULT_STUN_PORT : stunPort;
        ThreadFactory factory = r -> {
            Thread t = new Thread(r, "mechanist-nat-traversal");
            t.setDaemon(true);
            return t;
        };
        this.ioExecutor = Executors.newCachedThreadPool(factory);
    }

    CompletableFuture<NatDiscoveryResult> discoverAsync(int localPort, Duration timeout) {
        int port = NetworkPortAuthority.portWithinAllowedGameRange(localPort) ? localPort : NetworkPortAuthority.firstAvailableGamePort();
        Duration useTimeout = timeout == null || timeout.isNegative() || timeout.isZero() ? Duration.ofSeconds(3) : timeout;
        return CompletableFuture.supplyAsync(() -> discover(port, useTimeout), ioExecutor);
    }

    NatDiscoveryResult discover(int localPort, Duration timeout) {
        byte[] transaction = new byte[12];
        secureRandom.nextBytes(transaction);
        byte[] request = buildBindingRequest(transaction);
        long started = System.nanoTime();
        try (DatagramSocket socket = new DatagramSocket(localPort)) {
            socket.setSoTimeout((int)Math.min(Integer.MAX_VALUE, Math.max(250, timeout.toMillis())));
            InetAddress stunAddress = InetAddress.getByName(stunHost);
            socket.send(new DatagramPacket(request, request.length, stunAddress, stunPort));
            byte[] response = new byte[768];
            DatagramPacket packet = new DatagramPacket(response, response.length);
            socket.receive(packet);
            StunMappedAddress mapped = parseBindingResponse(response, packet.getLength(), transaction);
            return new NatDiscoveryResult(true, localLanAddress().orElse("127.0.0.1"), mapped.host(), mapped.port(), localPort, stunHost + ":" + stunPort, Duration.ofNanos(System.nanoTime() - started).toMillis(), "STUN mapping discovered", Instant.now().toString());
        } catch (SocketTimeoutException ex) {
            return new NatDiscoveryResult(false, localLanAddress().orElse("127.0.0.1"), "", -1, localPort, stunHost + ":" + stunPort, Duration.ofNanos(System.nanoTime() - started).toMillis(), "STUN timeout: " + ex.getMessage(), Instant.now().toString());
        } catch (IOException | IllegalArgumentException ex) {
            return new NatDiscoveryResult(false, localLanAddress().orElse("127.0.0.1"), "", -1, localPort, stunHost + ":" + stunPort, Duration.ofNanos(System.nanoTime() - started).toMillis(), ex.getClass().getSimpleName() + ": " + ex.getMessage(), Instant.now().toString());
        }
    }

    CompletableFuture<HolePunchResult> punchAsync(UdpPunchToken localToken, UdpPunchToken remoteToken, Duration window) {
        Objects.requireNonNull(localToken, "localToken");
        Objects.requireNonNull(remoteToken, "remoteToken");
        Duration useWindow = window == null || window.isNegative() || window.isZero() ? Duration.ofSeconds(4) : window;
        return CompletableFuture.supplyAsync(() -> punch(localToken, remoteToken, useWindow), ioExecutor);
    }

    HolePunchResult punch(UdpPunchToken localToken, UdpPunchToken remoteToken, Duration window) {
        long deadline = System.nanoTime() + window.toNanos();
        String payload = "MECH_PUNCH|" + localToken.sessionNonce() + "|" + localToken.publicHost() + ":" + localToken.publicPort();
        byte[] bytes = payload.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        int sent = 0;
        try (DatagramSocket socket = new DatagramSocket(localToken.localPort())) {
            socket.setSoTimeout(150);
            InetAddress remote = InetAddress.getByName(remoteToken.publicHost());
            DatagramPacket out = new DatagramPacket(bytes, bytes.length, remote, remoteToken.publicPort());
            byte[] inBuffer = new byte[512];
            while (System.nanoTime() < deadline) {
                socket.send(out);
                sent++;
                try {
                    DatagramPacket in = new DatagramPacket(inBuffer, inBuffer.length);
                    socket.receive(in);
                    String text = new String(in.getData(), in.getOffset(), in.getLength(), java.nio.charset.StandardCharsets.UTF_8);
                    if (text.contains(remoteToken.sessionNonce())) return new HolePunchResult(true, sent, in.getAddress().getHostAddress(), in.getPort(), "reciprocal punch token observed");
                } catch (SocketTimeoutException ignored) { }
                try { TimeUnit.MILLISECONDS.sleep(75); } catch (InterruptedException ex) { Thread.currentThread().interrupt(); return new HolePunchResult(false, sent, remoteToken.publicHost(), remoteToken.publicPort(), "interrupted during punch window"); }
            }
            return new HolePunchResult(false, sent, remoteToken.publicHost(), remoteToken.publicPort(), "punch window elapsed without reciprocal token");
        } catch (IOException ex) {
            return new HolePunchResult(false, sent, remoteToken.publicHost(), remoteToken.publicPort(), ex.getClass().getSimpleName() + ": " + ex.getMessage());
        }
    }

    UdpPunchToken tokenFromDiscovery(NatDiscoveryResult discovery) {
        byte[] nonce = new byte[12];
        secureRandom.nextBytes(nonce);
        String publicHost = discovery != null && discovery.success() ? discovery.publicHost() : localLanAddress().orElse("127.0.0.1");
        int publicPort = discovery != null && discovery.success() ? discovery.publicPort() : (discovery == null ? NetworkPortAuthority.DEFAULT_GAME_PORT : discovery.localPort());
        int localPort = discovery == null ? NetworkPortAuthority.DEFAULT_GAME_PORT : discovery.localPort();
        return new UdpPunchToken(publicHost, publicPort, localLanAddress().orElse("127.0.0.1"), localPort, HexFormat.of().formatHex(nonce), Instant.now().toString());
    }

    static Optional<String> localLanAddress() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            ArrayList<String> candidates = new ArrayList<>();
            while (interfaces.hasMoreElements()) {
                NetworkInterface ni = interfaces.nextElement();
                if (!ni.isUp() || ni.isLoopback() || ni.isVirtual()) continue;
                Enumeration<InetAddress> addresses = ni.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress address = addresses.nextElement();
                    if (!address.isLoopbackAddress() && !address.isLinkLocalAddress()) candidates.add(address.getHostAddress());
                }
            }
            return candidates.stream().filter(s -> !s.contains(":" )).findFirst().or(() -> candidates.stream().findFirst());
        } catch (IOException ex) {
            return Optional.empty();
        }
    }

    private static byte[] buildBindingRequest(byte[] transaction) {
        ByteBuffer b = ByteBuffer.allocate(20).order(ByteOrder.BIG_ENDIAN);
        b.putShort((short)STUN_BINDING_REQUEST).putShort((short)0).putInt(STUN_MAGIC_COOKIE).put(transaction);
        return b.array();
    }

    private static StunMappedAddress parseBindingResponse(byte[] data, int length, byte[] expectedTransaction) throws IOException {
        if (length < 20) throw new IOException("short STUN response");
        ByteBuffer b = ByteBuffer.wrap(data, 0, length).order(ByteOrder.BIG_ENDIAN);
        int type = Short.toUnsignedInt(b.getShort());
        int msgLen = Short.toUnsignedInt(b.getShort());
        int cookie = b.getInt();
        byte[] transaction = new byte[12];
        b.get(transaction);
        if (type != STUN_BINDING_SUCCESS) throw new IOException("unexpected STUN message type 0x" + Integer.toHexString(type));
        if (cookie != STUN_MAGIC_COOKIE) throw new IOException("invalid STUN magic cookie");
        for (int i = 0; i < transaction.length; i++) if (transaction[i] != expectedTransaction[i]) throw new IOException("STUN transaction mismatch");
        int end = Math.min(length, 20 + msgLen);
        while (b.position() + 4 <= end) {
            int attrType = Short.toUnsignedInt(b.getShort());
            int attrLen = Short.toUnsignedInt(b.getShort());
            int valueStart = b.position();
            if ((attrType == STUN_XOR_MAPPED_ADDRESS || attrType == STUN_MAPPED_ADDRESS) && attrLen >= 8) {
                b.get();
                int family = Byte.toUnsignedInt(b.get());
                int port = Short.toUnsignedInt(b.getShort());
                byte[] addr;
                if (family == 0x01 && attrLen >= 8) {
                    addr = new byte[4];
                    b.get(addr);
                    if (attrType == STUN_XOR_MAPPED_ADDRESS) {
                        port ^= (STUN_MAGIC_COOKIE >>> 16);
                        int magic = STUN_MAGIC_COOKIE;
                        addr[0] = (byte)(addr[0] ^ ((magic >>> 24) & 0xff));
                        addr[1] = (byte)(addr[1] ^ ((magic >>> 16) & 0xff));
                        addr[2] = (byte)(addr[2] ^ ((magic >>> 8) & 0xff));
                        addr[3] = (byte)(addr[3] ^ (magic & 0xff));
                    }
                    return new StunMappedAddress(InetAddress.getByAddress(addr).getHostAddress(), port);
                } else if (family == 0x02 && attrLen >= 20) {
                    addr = new byte[16];
                    b.get(addr);
                    if (attrType == STUN_XOR_MAPPED_ADDRESS) {
                        port ^= (STUN_MAGIC_COOKIE >>> 16);
                        byte[] cookieBytes = ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putInt(STUN_MAGIC_COOKIE).array();
                        for (int i = 0; i < 4; i++) addr[i] ^= cookieBytes[i];
                    }
                    return new StunMappedAddress(InetAddress.getByAddress(addr).getHostAddress(), port);
                }
            }
            int padded = (attrLen + 3) & ~3;
            b.position(Math.min(end, valueStart + padded));
        }
        throw new IOException("STUN response did not contain mapped address");
    }

    @Override public void close() { ioExecutor.shutdownNow(); }
}

record NatDiscoveryResult(boolean success, String localLanHost, String publicHost, int publicPort, int localPort, String stunServer, long elapsedMillis, String message, String observedAtIso) {
    String display() { return success ? publicHost + ":" + publicPort + " / LAN " + localLanHost + ":" + localPort : "NAT discovery unavailable: " + message; }
}
record StunMappedAddress(String host, int port) { }
record UdpPunchToken(String publicHost, int publicPort, String localHost, int localPort, String sessionNonce, String createdAtIso) { }
record HolePunchResult(boolean success, int packetsSent, String remoteHost, int remotePort, String message) { }
