package mechanist.server.admin;

import java.nio.file.Path;
import java.time.Instant;

public final class ServerMaintenanceResult {
    public enum State {
        OK,
        DEFERRED,
        FAILED
    }

    private final State state;
    private final String action;
    private final String message;
    private final Path path;
    private final Instant timestamp;

    public ServerMaintenanceResult(State state, String action, String message, Path path) {
        this.state = state == null ? State.FAILED : state;
        this.action = action == null ? "<unknown>" : action;
        this.message = message == null ? "" : message;
        this.path = path;
        this.timestamp = Instant.now();
    }

    public State state() { return state; }
    public String action() { return action; }
    public String message() { return message; }
    public Path path() { return path; }
    public Instant timestamp() { return timestamp; }

    public String summary() {
        return state + " action=" + action + " path=" + (path == null ? "<none>" : path) + " message=" + message;
    }
}
