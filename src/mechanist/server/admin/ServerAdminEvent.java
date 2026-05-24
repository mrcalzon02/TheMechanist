package mechanist.server.admin;

import java.time.Instant;
import java.util.Objects;

public final class ServerAdminEvent {
    public enum Kind {
        SERVER_START,
        SERVER_STOP,
        UPDATE_CHECK,
        UPDATE_APPLY,
        SAVE_NOW,
        LOAD_WORLD,
        BACKUP_CREATE,
        BACKUP_RESTORE,
        PLAYER_JOIN,
        PLAYER_LEAVE,
        PLAYER_ACTION,
        COMMAND_ACCEPTED,
        COMMAND_DENIED,
        SECURITY_NOTICE
    }

    private final Instant timestamp;
    private final Kind kind;
    private final String actor;
    private final String message;

    public ServerAdminEvent(Kind kind, String actor, String message) {
        this(Instant.now(), kind, actor, message);
    }

    public ServerAdminEvent(Instant timestamp, Kind kind, String actor, String message) {
        this.timestamp = Objects.requireNonNull(timestamp, "timestamp");
        this.kind = Objects.requireNonNull(kind, "kind");
        this.actor = actor == null || actor.isBlank() ? "system" : actor;
        this.message = message == null ? "" : message;
    }

    public Instant timestamp() { return timestamp; }
    public Kind kind() { return kind; }
    public String actor() { return actor; }
    public String message() { return message; }

    public String toLogLine() {
        return timestamp + "\t" + kind + "\t" + actor + "\t" + sanitize(message);
    }

    private static String sanitize(String value) {
        return value.replace('\n', ' ').replace('\r', ' ').replace('\t', ' ');
    }
}
