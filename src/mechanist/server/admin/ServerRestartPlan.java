package mechanist.server.admin;

import java.time.Instant;

public final class ServerRestartPlan {
    public enum State {
        NOT_REQUIRED,
        READY,
        BLOCKED_CLIENTS_CONNECTED,
        BLOCKED_SAVE_ACTIVE,
        SCHEDULED,
        FAILED
    }

    private final State state;
    private final String reason;
    private final boolean updateApplied;
    private final boolean restartRequired;
    private final Instant createdAt;

    public ServerRestartPlan(State state, String reason, boolean updateApplied, boolean restartRequired) {
        this.state = state == null ? State.FAILED : state;
        this.reason = reason == null ? "" : reason;
        this.updateApplied = updateApplied;
        this.restartRequired = restartRequired;
        this.createdAt = Instant.now();
    }

    public State state() { return state; }
    public String reason() { return reason; }
    public boolean updateApplied() { return updateApplied; }
    public boolean restartRequired() { return restartRequired; }
    public Instant createdAt() { return createdAt; }

    public String summary() {
        return state + " updateApplied=" + updateApplied + " restartRequired=" + restartRequired + " reason=" + reason;
    }
}
