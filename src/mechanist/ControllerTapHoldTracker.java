package mechanist;

import mechanist.input.KeyBindingManager;

/**
 * Small runtime timing helper for overloaded controller buttons.
 *
 * It turns a physical press into tap-release and hold-active reads without owning
 * action routing, UI state, or hardware polling.
 */
final class ControllerTapHoldTracker {
    static final String VERSION = "0.9.10ka";

    private long activeSinceMs = -1L;
    private boolean holdReached = false;

    TapHoldRead update(boolean active, long nowMs, KeyBindingManager.ControllerTuningProfile tuning) {
        if (tuning == null) tuning = KeyBindingManager.ControllerTuningProfile.defaults();
        if (active) {
            if (activeSinceMs < 0L) {
                activeSinceMs = nowMs;
                holdReached = false;
            }
            long held = Math.max(0L, nowMs - activeSinceMs);
            boolean holdActive = held >= tuning.holdMillis();
            if (holdActive) holdReached = true;
            return new TapHoldRead(true, false, holdActive, false, held, holdActive ? "hold active" : "pressing");
        }

        if (activeSinceMs >= 0L) {
            long held = Math.max(0L, nowMs - activeSinceMs);
            boolean tapReleased = held <= tuning.tapMillis() && !holdReached;
            boolean holdReleased = holdReached || held >= tuning.holdMillis();
            activeSinceMs = -1L;
            holdReached = false;
            return new TapHoldRead(false, tapReleased, false, holdReleased, held,
                    tapReleased ? "tap released" : (holdReleased ? "hold released" : "press released"));
        }

        return TapHoldRead.idle();
    }

    void clear() {
        activeSinceMs = -1L;
        holdReached = false;
    }

    static String auditSummary() {
        return "controllerTapHoldTracker version=" + VERSION + " model=tap-release+hold-active thresholds=current-controller-profile";
    }

    record TapHoldRead(boolean pressed, boolean tapReleased, boolean holdActive, boolean holdReleased,
                       long heldMillis, String playerFacingState) {
        static TapHoldRead idle() {
            return new TapHoldRead(false, false, false, false, 0L, "idle");
        }
    }
}
