package mechanist;

/** Smoke for controller connection, reconnection, disconnection, and fallback notices. */
final class Milestone02ControllerConnectionSmoke {
    public static void main(String[] args) {
        ControllerConnectionStateTracker tracker = new ControllerConnectionStateTracker();

        ControllerConnectionStateTracker.ConnectionRead idle = tracker.update(GamepadControllerSnapshot.disconnected(), 100L);
        require(!idle.connected(), "initial disconnected state");
        require(idle.event() == ControllerConnectionStateTracker.ConnectionEvent.STEADY_DISCONNECTED, "initial disconnected event");
        requireContains(idle.playerFacingNotice(), "Keyboard and mouse fallback", "initial fallback notice");

        GamepadControllerSnapshot connectedPad = GamepadControllerSnapshot.of("Smoke Pad",
                false, false, false, false,
                false, false, false, false,
                false, false, false, false,
                0.0f, 0.0f, 0.0f, 0.0f);
        ControllerConnectionStateTracker.ConnectionRead connected = tracker.update(connectedPad, 200L);
        require(connected.connected(), "connected state");
        require(connected.event() == ControllerConnectionStateTracker.ConnectionEvent.CONNECTED, "connected event");
        require(connected.transition(), "connected should be a transition");
        requireContains(connected.playerFacingNotice(), "Controller connected", "connected notice");

        ControllerConnectionStateTracker.ConnectionRead steady = tracker.update(connectedPad, 300L);
        require(steady.connected(), "steady connected state");
        require(steady.event() == ControllerConnectionStateTracker.ConnectionEvent.STEADY_CONNECTED, "steady connected event");
        require(!steady.transition(), "steady connected should not be a transition");

        ControllerConnectionStateTracker.ConnectionRead disconnected = tracker.update(GamepadControllerSnapshot.disconnected(), 400L);
        require(!disconnected.connected(), "disconnected state");
        require(disconnected.event() == ControllerConnectionStateTracker.ConnectionEvent.DISCONNECTED, "disconnected event");
        require(disconnected.transition(), "disconnect should be a transition");
        requireContains(disconnected.playerFacingNotice(), "Controller disconnected", "disconnect notice");
        requireContains(disconnected.playerFacingNotice(), "Keyboard and mouse fallback", "disconnect fallback notice");

        ControllerConnectionStateTracker.ConnectionRead reconnected = tracker.update(connectedPad, 500L);
        require(reconnected.connected(), "reconnected state");
        require(reconnected.event() == ControllerConnectionStateTracker.ConnectionEvent.RECONNECTED, "reconnected event");
        requireContains(reconnected.playerFacingNotice(), "Controller reconnected", "reconnected notice");

        GamepadInputEngine engine = new GamepadInputEngine(new InputRegistry());
        ControllerConnectionStateTracker.ConnectionRead engineRead = engine.inspectConnection(connectedPad, 600L);
        require(engineRead.connected(), "engine inspect should reuse connection tracker");
        requireContains(engine.playerFacingConnectionNotice(), "Controller connected", "engine connection notice");
        requireContains(GamepadInputEngine.auditSummary(), "connectionNotices", "engine audit summary");
        requireContains(ControllerConnectionStateTracker.auditSummary(), "fallbackNotice=keyboardMouse", "tracker audit summary");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static void requireContains(String text, String expected, String label) {
        if (text == null || !text.contains(expected)) {
            throw new AssertionError("Expected " + label + " to contain '" + expected + "': " + text);
        }
    }

    private Milestone02ControllerConnectionSmoke() { }
}
