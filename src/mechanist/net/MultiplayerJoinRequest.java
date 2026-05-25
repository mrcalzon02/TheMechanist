package mechanist.net;

import java.time.Instant;

public final class MultiplayerJoinRequest {
    private final String serverAddress;
    private final int port;
    private final String playerName;
    private final char[] password;
    private final boolean streamSafeAddressDisplay;
    private final Instant createdAt;

    public MultiplayerJoinRequest(String serverAddress, int port, String playerName, char[] password, boolean streamSafeAddressDisplay) {
        this.serverAddress = serverAddress == null ? "" : serverAddress.trim();
        this.port = Math.max(1, Math.min(65535, port));
        this.playerName = playerName == null ? "" : playerName.trim();
        this.password = password == null ? new char[0] : password.clone();
        this.streamSafeAddressDisplay = streamSafeAddressDisplay;
        this.createdAt = Instant.now();
    }

    public String serverAddress() { return serverAddress; }
    public int port() { return port; }
    public String playerName() { return playerName; }
    public char[] password() { return password.clone(); }
    public boolean streamSafeAddressDisplay() { return streamSafeAddressDisplay; }
    public Instant createdAt() { return createdAt; }

    public String endpoint() {
        if (serverAddress.contains(":")) return serverAddress;
        return serverAddress + ":" + port;
    }

    public String redactedSummary() {
        return "server=" + StreamSafeTextFields.redactAddress(endpoint())
                + " player=" + (playerName.isBlank() ? "<empty>" : playerName)
                + " password=" + StreamSafeTextFields.redactPassword(password)
                + " streamSafe=" + streamSafeAddressDisplay;
    }
}
