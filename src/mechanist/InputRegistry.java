package mechanist;

import java.util.*;

/** Thread-safe source-aware input registry. Keyboard and gamepad own separate states. */
final class InputRegistry {
    static final String VERSION = "0.9.10et";
    private final EnumMap<InputAction, EnumMap<InputSource, Boolean>> digital = new EnumMap<>(InputAction.class);
    private final EnumMap<InputAction, EnumMap<InputSource, Float>> analog = new EnumMap<>(InputAction.class);
    private final EnumMap<InputAction, EnumMap<InputSource, Boolean>> consumedDown = new EnumMap<>(InputAction.class);

    InputRegistry() {
        for (InputAction a : InputAction.values()) {
            EnumMap<InputSource, Boolean> d = new EnumMap<>(InputSource.class);
            EnumMap<InputSource, Float> f = new EnumMap<>(InputSource.class);
            EnumMap<InputSource, Boolean> c = new EnumMap<>(InputSource.class);
            for (InputSource s : InputSource.values()) {
                d.put(s, Boolean.FALSE);
                f.put(s, 0.0f);
                c.put(s, Boolean.FALSE);
            }
            digital.put(a, d);
            analog.put(a, f);
            consumedDown.put(a, c);
        }
    }

    synchronized void setDigital(InputSource source, InputAction action, boolean active) {
        digital.get(action).put(source, active);
        if (!active) consumedDown.get(action).put(source, Boolean.FALSE);
    }

    synchronized void setAnalog(InputSource source, InputAction action, float value) {
        analog.get(action).put(source, Math.max(-1.0f, Math.min(1.0f, value)));
    }

    synchronized boolean isActive(InputAction action) {
        for (InputSource s : InputSource.values()) if (isActiveFromSourceLocked(action, s)) return true;
        return false;
    }

    synchronized boolean isActiveFromSource(InputAction action, InputSource source) {
        return isActiveFromSourceLocked(action, source);
    }

    private boolean isActiveFromSourceLocked(InputAction action, InputSource source) {
        return Boolean.TRUE.equals(digital.get(action).get(source)) || Math.abs(analog.get(action).get(source)) > 0.01f;
    }

    synchronized boolean consumePressed(InputAction action, InputSource source) {
        boolean active = isActiveFromSourceLocked(action, source);
        boolean consumed = Boolean.TRUE.equals(consumedDown.get(action).get(source));
        if (active && !consumed) {
            consumedDown.get(action).put(source, Boolean.TRUE);
            return true;
        }
        if (!active) consumedDown.get(action).put(source, Boolean.FALSE);
        return false;
    }

    synchronized void clearSource(InputSource source) {
        for (InputAction a : InputAction.values()) {
            digital.get(a).put(source, Boolean.FALSE);
            analog.get(a).put(source, 0.0f);
            consumedDown.get(a).put(source, Boolean.FALSE);
        }
    }

    synchronized String compactStatus() {
        ArrayList<String> active = new ArrayList<>();
        for (InputAction a : InputAction.values()) if (isActive(a)) active.add(a.name());
        return active.isEmpty() ? "no active abstract input" : String.join(", ", active);
    }

    static String auditSummary() {
        return "inputRegistry version=" + VERSION + " sources=keyboard+gamepad model=source-aware-union coexistence=true threadSafe=synchronized";
    }
}
