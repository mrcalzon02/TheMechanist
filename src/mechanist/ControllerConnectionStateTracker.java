package mechanist;

/** Tracks player-facing controller connection transitions without owning hardware polling. */
final class ControllerConnectionStateTracker {
    static final String VERSION = "0.9.10kb";

    private boolean wasConnected = false;
    private String lastControllerName = "no controller";
    private ConnectionRead lastRead = ConnectionRead.initial();

    ConnectionRead update(GamepadControllerSnapshot snapshot, long nowMs) {
        boolean connected = snapshot != null && snapshot.connected;
        String name = connected ? safeName(snapshot.name) : lastControllerName;
        ConnectionEvent event;
        String notice;
        if (connected && !wasConnected) {
            event = "no controller".equals(lastControllerName) ? ConnectionEvent.CONNECTED : ConnectionEvent.RECONNECTED;
            notice = event == ConnectionEvent.CONNECTED
                    ? "Controller connected: " + name + ". Keyboard and mouse fallback remain active."
                    : "Controller reconnected: " + name + ". Keyboard and mouse fallback remain active.";
        } else if (!connected && wasConnected) {
            event = ConnectionEvent.DISCONNECTED;
            notice = "Controller disconnected. Keyboard and mouse fallback remain active.";
        } else if (connected) {
            event = ConnectionEvent.STEADY_CONNECTED;
            notice = "Controller active: " + name + ".";
        } else {
            event = ConnectionEvent.STEADY_DISCONNECTED;
            notice = "No controller detected. Keyboard and mouse fallback remain active.";
        }

        wasConnected = connected;
        if (connected) lastControllerName = name;
        lastRead = new ConnectionRead(connected, lastControllerName, event, nowMs, notice);
        return lastRead;
    }

    void clear() {
        wasConnected = false;
        lastControllerName = "no controller";
        lastRead = ConnectionRead.initial();
    }

    ConnectionRead lastRead() {
        return lastRead;
    }

    static String auditSummary() {
        return "controllerConnectionStateTracker version=" + VERSION + " transitions=connected+reconnected+disconnected fallbackNotice=keyboardMouse";
    }

    private static String safeName(String name) {
        if (name == null || name.isBlank()) return "generic controller";
        String cleaned = name.replace('\r', ' ').replace('\n', ' ').replace('\t', ' ').trim();
        return cleaned.isBlank() ? "generic controller" : cleaned;
    }

    enum ConnectionEvent {
        CONNECTED,
        RECONNECTED,
        DISCONNECTED,
        STEADY_CONNECTED,
        STEADY_DISCONNECTED
    }

    record ConnectionRead(boolean connected, String controllerName, ConnectionEvent event, long observedAtMs, String playerFacingNotice) {
        static ConnectionRead initial() {
            return new ConnectionRead(false, "no controller", ConnectionEvent.STEADY_DISCONNECTED, 0L,
                    "No controller detected. Keyboard and mouse fallback remain active.");
        }

        boolean transition() {
            return event == ConnectionEvent.CONNECTED || event == ConnectionEvent.RECONNECTED || event == ConnectionEvent.DISCONNECTED;
        }
    }
}
