package mechanist;

/**
 * Classpath probe for an optional Netty transport. The shipped desktop zip stays runnable without
 * external jars; when a verified Netty adapter is added to the classpath, this authority is the seam
 * that the headless host uses before falling back to the native Java NIO relay.
 */
final class ReflectiveNettyBindingService {
    static final String VERSION = "reflective-netty-binding-service-0.9.10hs";

    private ReflectiveNettyBindingService() { }

    static HostBindingResult tryBind(ServerConfig config, boolean ipv6) {
        String requested = config == null ? "" : config.boundAddress();
        String address = requested == null || requested.isBlank() ? (ipv6 ? "::" : "0.0.0.0") : requested.trim();
        MultiplayerProtocolState protocol = ipv6 ? MultiplayerProtocolState.NETTY_IPV6 : MultiplayerProtocolState.NETTY_IPV4;
        ServerConfig exact = config.withBinding(address, config.port(), protocol);
        NettyClasspathStatus status = probeClasspath();
        if (!status.nettyCorePresent()) {
            return HostBindingResult.failure(exact, protocol,
                    "Netty classes are not on the runtime classpath; native Java NIO relay will be tried next for " + address + ".");
        }
        if (!status.nioServerSocketChannelPresent()) {
            return HostBindingResult.failure(exact, protocol,
                    "Netty core is present but NioServerSocketChannel is unavailable for " + address + ".");
        }
        // Do not synthesize a fake Netty server. Without a compiled ChannelInitializer/handler adapter
        // for the exact Netty version, binding reflectively would be brittle and unsafe. The fallback
        // relay remains real and closeable while preserving this optional seam.
        return HostBindingResult.failure(exact, protocol,
                "Netty is visible, but no verified Mechanist Netty channel adapter is packaged in this build for " + address + ".");
    }

    static NettyClasspathStatus probeClasspath() {
        return new NettyClasspathStatus(
                present("io.netty.bootstrap.ServerBootstrap"),
                present("io.netty.channel.nio.NioEventLoopGroup"),
                present("io.netty.channel.socket.nio.NioServerSocketChannel"),
                present("io.netty.channel.epoll.EpollServerSocketChannel"));
    }

    static String auditSummary() {
        NettyClasspathStatus s = probeClasspath();
        return "authority=" + VERSION + " bootstrap=" + s.bootstrapPresent()
                + " nioGroup=" + s.nioEventLoopPresent()
                + " nioServer=" + s.nioServerSocketChannelPresent()
                + " epollServer=" + s.epollServerSocketChannelPresent();
    }

    private static boolean present(String className) {
        try {
            Class.forName(className, false, ReflectiveNettyBindingService.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException | LinkageError ex) {
            return false;
        }
    }

    record NettyClasspathStatus(boolean bootstrapPresent,
                                boolean nioEventLoopPresent,
                                boolean nioServerSocketChannelPresent,
                                boolean epollServerSocketChannelPresent) {
        boolean nettyCorePresent() { return bootstrapPresent && nioEventLoopPresent; }
    }
}
