package mechanist.server.admin;

import java.time.Instant;

public final class ServerUpdateStatus {
    public enum State {
        UNKNOWN,
        CURRENT,
        UPDATE_AVAILABLE,
        CHECK_FAILED,
        UPDATE_DEFERRED,
        UPDATE_READY_FOR_RESTART
    }

    private final State state;
    private final String currentVersion;
    private final String availableVersion;
    private final String channel;
    private final String message;
    private final Instant checkedAt;
    private final boolean restartRequired;

    public ServerUpdateStatus(State state, String currentVersion, String availableVersion,
                              String channel, String message, boolean restartRequired) {
        this.state = state == null ? State.UNKNOWN : state;
        this.currentVersion = currentVersion == null ? "<unknown>" : currentVersion;
        this.availableVersion = availableVersion == null ? "<unknown>" : availableVersion;
        this.channel = channel == null || channel.isBlank() ? "main" : channel;
        this.message = message == null ? "" : message;
        this.restartRequired = restartRequired;
        this.checkedAt = Instant.now();
    }

    public State state() { return state; }
    public String currentVersion() { return currentVersion; }
    public String availableVersion() { return availableVersion; }
    public String channel() { return channel; }
    public String message() { return message; }
    public Instant checkedAt() { return checkedAt; }
    public boolean restartRequired() { return restartRequired; }

    public String summary() {
        return state + " current=" + currentVersion + " available=" + availableVersion + " channel=" + channel + " restartRequired=" + restartRequired + " message=" + message;
    }
}
