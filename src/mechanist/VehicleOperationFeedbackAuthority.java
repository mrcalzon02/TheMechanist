package mechanist;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Bounded runtime feedback for truthful vehicle operation state.
 *
 * The persistent vehicle fixture remains authoritative. This class owns only
 * short-lived visual and one-shot audio feedback, so save/load cannot resurrect
 * an engine loop or leave a vehicle permanently marked as running.
 */
final class VehicleOperationFeedbackAuthority {
    static final long ACTIVE_FEEDBACK_MILLIS = 650L;
    static final long TRAILING_FEEDBACK_MILLIS = 1_650L;
    private static final int MAX_SESSIONS = 64;

    record Feedback(String state, String soundCue, boolean headlights,
                    int headlightRange, String summary) { }

    record VisualState(boolean visible, boolean activelyOperating,
                       boolean recentlyParked, double pulseScale, int pulseAlpha,
                       boolean headlightsVisible, int facingDx, int facingDy,
                       int headlightRange, int headlightIntensity,
                       boolean degraded, String soundCue, String summary) {
        static VisualState inactive() {
            return new VisualState(false, false, false, 1.0, 0,
                    false, 1, 0, 0, 0, false, "", "Vehicle feedback is inactive.");
        }
    }

    private record Session(String vehicleKey, long startedAtMillis,
                           long activeUntilMillis, long expiresAtMillis,
                           int steps, int fromX, int fromY, int toX, int toY,
                           int facingDx, int facingDy, String soundCue,
                           boolean completed) { }

    private static final LinkedHashMap<String, Session> SESSIONS =
            new LinkedHashMap<>(16, 0.75f, true);
    private static final LinkedHashMap<String, Long> LAST_SOUND_AT =
            new LinkedHashMap<>(16, 0.75f, true);

    private VehicleOperationFeedbackAuthority() { }

    static Feedback begin(GamePanel game, MapObjectState vehicle, int steps,
                          int fromX, int fromY, int toX, int toY,
                          int facingDx, int facingDy) {
        return begin(game, vehicle, steps, fromX, fromY, toX, toY,
                facingDx, facingDy, System.currentTimeMillis(), true);
    }

    static synchronized Feedback begin(GamePanel game, MapObjectState vehicle,
                                       int steps, int fromX, int fromY,
                                       int toX, int toY, int facingDx,
                                       int facingDy, long nowMillis,
                                       boolean playSound) {
        if (vehicle == null || !VehicleRuntimeAuthority.isVehicle(vehicle)) {
            return new Feedback("blocked", "", false, 0,
                    "No vehicle operation feedback was started.");
        }
        VehicleRuntimeAuthority.ensureInitialized(game == null ? null : game.world, vehicle);
        String key = key(vehicle);
        int dx = normalizeFacing(facingDx, facingDy)[0];
        int dy = normalizeFacing(facingDx, facingDy)[1];
        String cue = soundCue(vehicle);
        long activeUntil = Math.max(nowMillis + 180L,
                nowMillis + Math.min(ACTIVE_FEEDBACK_MILLIS,
                        260L + Math.max(0, steps) * 14L));
        long expires = activeUntil + TRAILING_FEEDBACK_MILLIS;
        SESSIONS.put(key, new Session(key, nowMillis, activeUntil, expires,
                Math.max(0, steps), fromX, fromY, toX, toY, dx, dy, cue, false));
        set(vehicle, "operationState", "running");
        set(vehicle, "headlightsActive", Boolean.toString(component(vehicle,
                VehicleRuntimeAuthority.Component.LIGHTS) > 0));
        set(vehicle, "operationFeedback", "route-started");
        set(vehicle, "facingDx", Integer.toString(dx));
        set(vehicle, "facingDy", Integer.toString(dy));
        if (playSound) playCue(game, vehicle, cue, nowMillis, "start");
        prune(nowMillis);
        int range = headlightRange(vehicle);
        boolean lights = component(vehicle, VehicleRuntimeAuthority.Component.LIGHTS) > 0;
        return new Feedback("running", cue, lights, range,
                "Vehicle operation feedback started with a bounded " + cue
                        + " cue and " + (lights ? range + "-tile forward lighting." : "no working headlights."));
    }

    static Feedback complete(GamePanel game, MapObjectState vehicle, int steps,
                             int fromX, int fromY, int toX, int toY,
                             int facingDx, int facingDy) {
        return complete(game, vehicle, steps, fromX, fromY, toX, toY,
                facingDx, facingDy, System.currentTimeMillis(), true);
    }

    static synchronized Feedback complete(GamePanel game, MapObjectState vehicle,
                                          int steps, int fromX, int fromY,
                                          int toX, int toY, int facingDx,
                                          int facingDy, long nowMillis,
                                          boolean playSound) {
        if (vehicle == null || !VehicleRuntimeAuthority.isVehicle(vehicle)) {
            return new Feedback("blocked", "", false, 0,
                    "No vehicle operation feedback was completed.");
        }
        VehicleRuntimeAuthority.ensureInitialized(game == null ? null : game.world, vehicle);
        String key = key(vehicle);
        int[] facing = normalizeFacing(facingDx, facingDy);
        String cue = soundCue(vehicle);
        long activeUntil = nowMillis + Math.min(ACTIVE_FEEDBACK_MILLIS,
                300L + Math.max(0, steps) * 10L);
        long expires = activeUntil + TRAILING_FEEDBACK_MILLIS;
        SESSIONS.put(key, new Session(key, nowMillis, activeUntil, expires,
                Math.max(0, steps), fromX, fromY, toX, toY,
                facing[0], facing[1], cue, true));
        set(vehicle, "operationState", "parked");
        set(vehicle, "headlightsActive", "false");
        set(vehicle, "operationFeedback", "recently-parked");
        set(vehicle, "lastOperationSteps", Integer.toString(Math.max(0, steps)));
        set(vehicle, "lastOperationTurn", Integer.toString(game == null ? 0 : Math.max(0, game.turn)));
        if (playSound) playCue(game, vehicle, cue, nowMillis, "stop");
        prune(nowMillis);
        return new Feedback("parked", cue, false, headlightRange(vehicle),
                "Vehicle parked after " + Math.max(0, steps)
                        + " validated road step(s); its arrival pulse and one-shot sound tail are bounded and transient.");
    }

    static synchronized VisualState visualState(GamePanel game,
                                                MapObjectState vehicle,
                                                long nowMillis) {
        if (vehicle == null || !VehicleRuntimeAuthority.isVehicle(vehicle)) {
            return VisualState.inactive();
        }
        prune(nowMillis);
        Session session = SESSIONS.get(key(vehicle));
        if (session == null || nowMillis > session.expiresAtMillis()) {
            return VisualState.inactive();
        }
        boolean active = nowMillis <= session.activeUntilMillis();
        boolean recent = session.completed() && nowMillis <= session.expiresAtMillis();
        double elapsed = Math.max(0L, nowMillis - session.startedAtMillis());
        double phase = (elapsed % 720L) / 720.0 * Math.PI * 2.0;
        double pulseScale = 1.0 + Math.sin(phase) * (active ? 0.12 : 0.06);
        double remaining = Math.max(0.0,
                (session.expiresAtMillis() - nowMillis)
                        / (double)Math.max(1L, session.expiresAtMillis() - session.startedAtMillis()));
        int alpha = clamp((int)Math.round((active ? 190 : 120) * Math.max(0.22, remaining)), 24, 210);
        int power = component(vehicle, VehicleRuntimeAuthority.Component.POWERPLANT);
        int mobility = component(vehicle, VehicleRuntimeAuthority.Component.MOBILITY);
        int lights = component(vehicle, VehicleRuntimeAuthority.Component.LIGHTS);
        boolean degraded = power < 55 || mobility < 55 || lights < 55;
        boolean headlights = active && lights > 0;
        int range = headlights ? headlightRange(vehicle) : 0;
        int intensity = headlights ? headlightIntensity(vehicle) : 0;
        String state = active ? "operating" : recent ? "recently parked" : "feedback tail";
        return new VisualState(true, active, recent, pulseScale, alpha,
                headlights, session.facingDx(), session.facingDy(), range,
                intensity, degraded, session.soundCue(),
                "Vehicle is " + state + "; pulse " + alpha + " alpha"
                        + (headlights ? ", headlights range " + range : ", headlights inactive")
                        + (degraded ? ", degraded feedback." : "."));
    }

    static ArrayList<WorldLightEmitterAuthority.Emitter> gameplayEmitters(
            GamePanel game, long nowMillis) {
        ArrayList<WorldLightEmitterAuthority.Emitter> out = new ArrayList<>();
        appendEmitters(game, out, nowMillis, 0, 0,
                game == null || game.world == null ? 0 : game.world.w,
                game == null || game.world == null ? 0 : game.world.h,
                Integer.MAX_VALUE);
        return out;
    }

    static ArrayList<WorldLightEmitterAuthority.Emitter> viewportEmitters(
            GamePanel game, int camX, int camY, int cols, int rows,
            long nowMillis) {
        ArrayList<WorldLightEmitterAuthority.Emitter> out = new ArrayList<>();
        appendEmitters(game, out, nowMillis, camX, camY, cols, rows, 5);
        return out;
    }

    private static void appendEmitters(GamePanel game,
                                       ArrayList<WorldLightEmitterAuthority.Emitter> out,
                                       long nowMillis, int camX, int camY,
                                       int cols, int rows, int pad) {
        if (game == null || game.world == null || out == null
                || game.world.mapObjects == null) return;
        int minX = Math.max(0, camX - Math.max(0, pad));
        int minY = Math.max(0, camY - Math.max(0, pad));
        int maxX = Math.min(game.world.w - 1,
                camX + Math.max(0, cols) + Math.max(0, pad));
        int maxY = Math.min(game.world.h - 1,
                camY + Math.max(0, rows) + Math.max(0, pad));
        for (MapObjectState vehicle : game.world.mapObjects) {
            if (!VehicleRuntimeAuthority.isVehicle(vehicle)
                    || vehicle.x < minX || vehicle.y < minY
                    || vehicle.x > maxX || vehicle.y > maxY) continue;
            VisualState state = visualState(game, vehicle, nowMillis);
            if (!state.visible()) continue;
            int pulseIntensity = clamp(8 + state.pulseAlpha() / 8, 10, 34);
            Color pulseColor = state.degraded()
                    ? new Color(220, 120, 72) : new Color(232, 190, 96);
            out.add(new WorldLightEmitterAuthority.Emitter(
                    "vehicle-operation-pulse-" + key(vehicle), vehicle.x, vehicle.y,
                    state.activelyOperating() ? 2 : 1, pulseIntensity,
                    pulseColor, state.degraded()));
            if (!state.headlightsVisible()) continue;
            int dx = state.facingDx();
            int dy = state.facingDy();
            int sideX = dy;
            int sideY = -dx;
            for (int step = 1; step <= state.headlightRange(); step++) {
                int x = vehicle.x + dx * step;
                int y = vehicle.y + dy * step;
                if (!game.world.inBounds(x, y)) break;
                int falloff = Math.max(5, state.headlightIntensity() - step * 5);
                int radius = step <= 2 ? 1 : 2;
                out.add(new WorldLightEmitterAuthority.Emitter(
                        "vehicle-headlight-" + key(vehicle) + "-" + step,
                        x, y, radius, falloff, new Color(238, 220, 166),
                        state.degraded()));
                if (step >= 2 && falloff > 8) {
                    addSideEmitter(game, out, vehicle, x + sideX, y + sideY,
                            step, falloff / 2, "l", state.degraded());
                    addSideEmitter(game, out, vehicle, x - sideX, y - sideY,
                            step, falloff / 2, "r", state.degraded());
                }
            }
        }
    }

    private static void addSideEmitter(GamePanel game,
                                       ArrayList<WorldLightEmitterAuthority.Emitter> out,
                                       MapObjectState vehicle, int x, int y,
                                       int step, int intensity, String side,
                                       boolean degraded) {
        if (!game.world.inBounds(x, y)) return;
        out.add(new WorldLightEmitterAuthority.Emitter(
                "vehicle-headlight-" + key(vehicle) + "-" + step + "-" + side,
                x, y, 1, Math.max(4, intensity), new Color(228, 208, 154), degraded));
    }

    static String soundCue(MapObjectState vehicle) {
        if (vehicle == null) return "ambient_machine";
        int power = component(vehicle, VehicleRuntimeAuthority.Component.POWERPLANT);
        if (power < 35) return "ambient_sparks";
        return switch (VehicleRuntimeAuthority.vehicleClass(vehicle.type)) {
            case UTILITY_BIKE -> "ambient_pipe";
            case CIVILIAN_CAR -> "ambient_machine";
            case CARGO_TRUCK, ARMORED_CAR, TANK -> "ambient_press";
        };
    }

    static int headlightRange(MapObjectState vehicle) {
        if (vehicle == null) return 0;
        int base = switch (VehicleRuntimeAuthority.vehicleClass(vehicle.type)) {
            case UTILITY_BIKE -> 2;
            case CIVILIAN_CAR -> 3;
            case CARGO_TRUCK, ARMORED_CAR -> 4;
            case TANK -> 5;
        };
        int lights = component(vehicle, VehicleRuntimeAuthority.Component.LIGHTS);
        if (lights <= 0) return 0;
        if (lights < 40) base--;
        return Math.max(1, base);
    }

    static int headlightIntensity(MapObjectState vehicle) {
        int lights = component(vehicle, VehicleRuntimeAuthority.Component.LIGHTS);
        int base = switch (VehicleRuntimeAuthority.vehicleClass(vehicle.type)) {
            case UTILITY_BIKE -> 24;
            case CIVILIAN_CAR -> 30;
            case CARGO_TRUCK -> 34;
            case ARMORED_CAR -> 36;
            case TANK -> 40;
        };
        return clamp(base * Math.max(0, lights) / 100, 0, 48);
    }

    static synchronized int activeSessionCount(long nowMillis) {
        prune(nowMillis);
        return SESSIONS.size();
    }

    static synchronized void clearTransientFeedback() {
        SESSIONS.clear();
        LAST_SOUND_AT.clear();
    }

    private static void playCue(GamePanel game, MapObjectState vehicle,
                                String cue, long nowMillis, String phase) {
        if (game == null || game.sounds == null || cue == null || cue.isBlank()) return;
        String soundKey = key(vehicle) + ":" + phase;
        Long prior = LAST_SOUND_AT.get(soundKey);
        if (prior != null && nowMillis - prior < 300L) return;
        LAST_SOUND_AT.put(soundKey, nowMillis);
        int distance = Math.max(1, Math.abs(game.playerX - vehicle.x)
                + Math.abs(game.playerY - vehicle.y));
        game.sounds.playDistantCue(cue, distance, game.options);
    }

    private static int[] normalizeFacing(int dx, int dy) {
        if (Math.abs(dx) >= Math.abs(dy) && dx != 0) return new int[]{Integer.signum(dx), 0};
        if (dy != 0) return new int[]{0, Integer.signum(dy)};
        return new int[]{1, 0};
    }

    private static int component(MapObjectState vehicle,
                                 VehicleRuntimeAuthority.Component component) {
        String componentKey = "vehicleComponent" + component.name().charAt(0)
                + component.name().substring(1).toLowerCase(Locale.ROOT);
        try {
            return clamp(Integer.parseInt(MapObjectState.stockValue(
                    vehicle.stockState, componentKey)), 0, 100);
        } catch (Exception ignored) {
            return 100;
        }
    }

    private static String key(MapObjectState vehicle) {
        if (vehicle == null) return "vehicle-none";
        if (vehicle.id != null && !vehicle.id.isBlank()) {
            return vehicle.id.replaceAll("[^A-Za-z0-9._-]", "_");
        }
        return "vehicle-" + Integer.toUnsignedString(System.identityHashCode(vehicle));
    }

    private static void set(MapObjectState vehicle, String key, String value) {
        vehicle.stockState = MapObjectState.setStockFlag(vehicle.stockState,
                key, value == null ? "" : value.replace(';', ',').replace('|', '/'));
    }

    private static synchronized void prune(long nowMillis) {
        Iterator<Map.Entry<String, Session>> iterator = SESSIONS.entrySet().iterator();
        while (iterator.hasNext()) {
            if (nowMillis > iterator.next().getValue().expiresAtMillis()) iterator.remove();
        }
        while (SESSIONS.size() > MAX_SESSIONS) {
            Iterator<String> keys = SESSIONS.keySet().iterator();
            if (!keys.hasNext()) break;
            keys.next();
            keys.remove();
        }
        Iterator<Map.Entry<String, Long>> soundIterator = LAST_SOUND_AT.entrySet().iterator();
        while (soundIterator.hasNext()) {
            if (nowMillis - soundIterator.next().getValue() > 10_000L) soundIterator.remove();
        }
    }

    private static int clamp(int value, int minimum, int maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }
}
