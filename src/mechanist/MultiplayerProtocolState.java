package mechanist;

/** Network transport selected for a multiplayer server or join attempt. */
enum MultiplayerProtocolState {
    STEAM_RELAY("Steam Datagram Relay / Steam Networking Sockets"),
    NETTY_IPV6("Netty TCP IPv6"),
    NETTY_IPV4("Netty TCP IPv4"),
    NATIVE_IPV6("Java NIO TCP IPv6 fallback"),
    NATIVE_IPV4("Java NIO TCP IPv4 fallback"),
    CLOSED("No network binding active");

    final String label;
    MultiplayerProtocolState(String label) { this.label = label; }
    boolean networkOpen() { return this != CLOSED; }
    boolean ipv6() { return this == NETTY_IPV6 || this == NATIVE_IPV6; }
    boolean steam() { return this == STEAM_RELAY; }
}
