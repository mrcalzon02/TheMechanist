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
 * short-lived visual and audio feedback, so save/load cannot resurrect an
 * engine loop or leave a vehicle permanently marked as running.
 */
final class VehicleOperationFeedbackAuthority {
    static final long ACTIVE_FEEDBACK_MILLIS = 650L;
    static final long TRAILING_FEEDBACK_MILLIS = 1_650L;
    static final long AMBIENT_REFRESH_MILLIS = 900L;
    private static final int MAX_SESSIONS = 64;

    /**
     * Data-owned presentation profile for one vehicle class.
     *
     * Runtime state still belongs to the physical vehicle; this profile only owns
     * the tunable operation-feedback presentation values consumed by pulse,
     * headlight, and ambient-audio rendering.
     */
    record FeedbackProfile(
            String soundCue,
            int audibleRange,
            int headlightRange,
            int headlightIntensity,
            long pulsePeriodMillis,
            double activePulseAmplitude,
            double tailPulseAmplitude,
            int activePulseAlpha,
            int tailPulseAlpha
    ) {
        FeedbackProfile {
            soundCue = soundCue == null || soundCue.isBlank()
                    ? "ambient_machine"
                    : soundCue;
            audibleRange = Math.max(1, audibleRange);
            headlightRange = Math.max(1, headlightRange);
            headlightIntensity = clamp(headlightIntensity, 1, 48);
            pulsePeriodMillis = Math.max(120L, pulsePeriodMillis);
            activePulseAmplitude = Math.max(0.0, Math.min(0.35, activePulseAmplitude));
            tailPulseAmplitude = Math.max(0.0, Math.min(activePulseAmplitude, tailPulseAmplitude));
            activePulseAlpha = clamp(activePulseAlpha, 24, 240);
            tailPulseAlpha = clamp(tailPulseAlpha, 16, activePulseAlpha);
        }

        String auditLine(VehicleRuntimeAuthority.VehicleClass vehicleClass) {
            return "class=" + vehicleClass.name()
                    + " sound=" + soundCue
                    + " audibleRange=" + audibleRange
                    + " headlight=" + headlightRange + "@" + headlightIntensity
                    + " pulse=" + pulsePeriodMillis + "ms/"
                    + activePulseAmplitude + "/" + tailPulseAmplitude
                    + " alpha=" + activePulseAlpha + "/" + tailPulseAlpha;
        }
    }

    private static final Map<VehicleRuntimeAuthority.VehicleClass, FeedbackProfile>
            FEEDBACK_PROFILES = Map.of(
            VehicleRuntimeAuthority.VehicleClass.UTILITY_BIKE,
            new FeedbackProfile("ambient_pipe", 12, 2, 24,
                    720L, 0.12, 0.06, 190, 120),
            VehicleRuntimeAuthority.VehicleClass.CIVILIAN_CAR,
            new FeedbackProfile("ambient_machine", 16, 3, 30,
                    720L, 0.12, 0.06, 190, 120),
            VehicleRuntimeAuthority.VehicleClass.CARGO_TRUCK,
            new FeedbackProfile("ambient_press", 20, 4, 34,
                    720L, 0.12, 0.06, 190, 120),
            VehicleRuntimeAuthority.VehicleClass.ARMORED_CAR,
            new FeedbackProfile("ambient_press", 22, 4, 36,
                    720L, 0.12, 0.06, 190, 120),
            VehicleRuntimeAuthority.VehicleClass.TANK,
            new FeedbackProfile("ambient_press", 24, 5, 40,
                    720L, 0.12, 0.06, 190, 120));

    record Feedback(String state, String soundCue, boolean headlights,
                    int headlightRange, String summary) { }

    record AmbientState(boolean active, boolean audible, String soundCue,
                        int distance, int audibleRange, boolean degraded,
                        String summary) {
        static AmbientState inactive(String summary) {
            return new AmbientState(false, false, "", 0, 0, false, summary);
        }
    }

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
        if (playSound && playCue(game, vehicle, cue, nowMillis, "start")) {
            LAST_SOUND_AT.put(key + ":ambient", nowMillis);
        }
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
        LAST_SOUND_AT.remove(key + ":ambient");
        set(vehicle, "operationState", "parked");
        set(vehicle, "headlightsActive", "false");
        set(vehicle, "operationFeedback", "recently-parked");
        set(vehicle, "lastOperationSteps", Integer.toString(Math.max(0, steps)));
        set(vehicle, "lastOperationTurn", Integer.toString(game == null ? 0 : Math.max(0, game.turn)));
        if (playSound) playCue(game, vehicle, cue, nowMillis, "stop");
        prune(nowMillis);
        return new Feedback("parked", cue, false, headlightRange(vehicle),
                "Vehicle parked after " + Math.max(0, steps)
                        + " validated road step(s); its arrival pulse and sound tail are bounded and transient.");
    }

    static synchronized AmbientState refreshAmbientAudio(
            GamePanel game, MapObjectState vehicle, long nowMillis) {
        return refreshAmbientAudio(game, vehicle, nowMillis, true);
    }

    static synchronized AmbientState refreshAmbientAudio(
            GamePanel game, MapObjectState vehicle, long nowMillis,
            boolean playSound) {
        prune(nowMillis);
        if (game == null || game.world == null || vehicle == null
                || !VehicleRuntimeAuthority.isVehicle(vehicle)
                || game.world.mapObjects == null
                || !game.world.mapObjects.contains(vehicle)) {
            removeTransientSession(vehicle);
            return AmbientState.inactive(
                    "Vehicle ambient audio is inactive because no loaded physical vehicle owns the session.");
        }
        String vehicleKey = key(vehicle);
        Session session = SESSIONS.get(vehicleKey);
        if (session == null) {
            return AmbientState.inactive(
                    "Vehicle ambient audio is inactive because no operation session exists.");
        }
        String operation = MapObjectState.stockValue(vehicle.stockState,
                "operationState").toLowerCase(Locale.ROOT);
        if (terminalOperationState(vehicle)) {
            removeTransientSession(vehicle);
            return AmbientState.inactive(
                    "Vehicle ambient audio stopped because the vehicle entered a terminal state.");
        }
        if (!operation.equals("running")) {
            LAST_SOUND_AT.remove(vehicleKey + ":ambient");
            return AmbientState.inactive(
                    "Vehicle ambient audio stopped because the vehicle is not operating.");
        }

        String cue = soundCue(vehicle);
        int power = component(vehicle, VehicleRuntimeAuthority.Component.POWERPLANT);
        int mobility = component(vehicle, VehicleRuntimeAuthority.Component.MOBILITY);
        boolean degraded = power < 55 || mobility < 55;
        int distance = Math.max(1, Math.abs(game.playerX - vehicle.x)
                + Math.abs(game.playerY - vehicle.y));
        int audibleRange = ambientAudibleRange(vehicle);
        boolean audible = distance <= audibleRange;
        long activeUntil = Math.max(session.activeUntilMillis(),
                nowMillis + ACTIVE_FEEDBACK_MILLIS);
        long expires = Math.max(session.expiresAtMillis(),
                activeUntil + TRAILING_FEEDBACK_MILLIS);
        SESSIONS.put(vehicleKey, new Session(vehicleKey,
                session.startedAtMillis(), activeUntil, expires,
                session.steps(), session.fromX(), session.fromY(),
                session.toX(), session.toY(), session.facingDx(),
                session.facingDy(), cue, false));
        if (playSound && audible) playCue(game, vehicle, cue, nowMillis, "ambient");
        return new AmbientState(true, audible, cue, distance, audibleRange,
                degraded, "Vehicle ambient audio is "
                + (audible ? "audible" : "outside audible range")
                + " at distance " + distance + "/" + audibleRange
                + " with " + cue
                + (degraded ? " degraded powertrain feedback." : " running feedback."));
    }

    static synchronized VisualState visualState(GamePanel game,
                                                MapObjectState vehicle,
                                                long nowMillis) {
        if (vehicle == null || !VehicleRuntimeAuthority.isVehicle(vehicle)) {
            return VisualState.inactive();
        }
        prune(nowMillis);
        String vehicleKey = key(vehicle);
        Session session = SESSIONS.get(vehicleKey);
        if (session == null || nowMillis > session.expiresAtMillis()) {
            return VisualState.inactive();
        }
        if (terminalOperationState(vehicle)) {
            removeTransientSession(vehicle);
            return VisualState.inactive();
        }
        boolean active = nowMillis <= session.activeUntilMillis();
        boolean recent = session.completed() && nowMillis <= session.expiresAtMillis();
        FeedbackProfile profile = profileFor(vehicle);
        double elapsed = Math.max(0L, nowMillis - session.startedAtMillis());
        double phase = (elapsed % profile.pulsePeriodMillis())
                / (double) profile.pulsePeriodMillis() * Math.PI * 2.0;
        double pulseScale = 1.0 + Math.sin(phase)
                * (active ? profile.activePulseAmplitude() : profile.tailPulseAmplitude());
        double remaining = Math.max(0.0,
                (session.expiresAtMillis() - nowMillis)
                        / (double)Math.max(1L, session.expiresAtMillis() - session.startedAtMillis()));
        int alpha = clamp((int)Math.round(
                (active ? profile.activePulseAlpha() : profile.tailPulseAlpha())
                        * Math.max(0.22, remaining)), 24, 240);
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
                0);
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
            refreshAmbientAudio(game, vehicle, nowMillis, true);
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

    static FeedbackProfile profileFor(MapObjectState vehicle) {
        VehicleRuntimeAuthority.VehicleClass vehicleClass =
                vehicle == null
                        ? VehicleRuntimeAuthority.VehicleClass.CIVILIAN_CAR
                        : VehicleRuntimeAuthority.vehicleClass(vehicle.type);
        FeedbackProfile profile = FEEDBACK_PROFILES.get(vehicleClass);
        if (profile == null) {
            throw new IllegalStateException(
                    "Vehicle operation feedback profile is missing for "
                            + vehicleClass.name());
        }
        return profile;
    }

    static String profileAuditSummary(MapObjectState vehicle) {
        VehicleRuntimeAuthority.VehicleClass vehicleClass =
                vehicle == null
                        ? VehicleRuntimeAuthority.VehicleClass.CIVILIAN_CAR
                        : VehicleRuntimeAuthority.vehicleClass(vehicle.type);
        return profileFor(vehicle).auditLine(vehicleClass);
    }

    static String soundCue(MapObjectState vehicle) {
        if (vehicle == null) return profileFor(null).soundCue();
        int power = component(vehicle, VehicleRuntimeAuthority.Component.POWERPLANT);
        if (power < 35) return "ambient_spark";
        return profileFor(vehicle).soundCue();
    }

    static int ambientAudibleRange(MapObjectState vehicle) {
        if (vehicle == null) return 0;
        int base = profileFor(vehicle).audibleRange();
        int power = component(vehicle, VehicleRuntimeAuthority.Component.POWERPLANT);
        if (power < 35) base -= 4;
        else if (power < 60) base -= 2;
        return Math.max(6, base);
    }

    static int headlightRange(MapObjectState vehicle) {
        if (vehicle == null) return 0;
        int base = profileFor(vehicle).headlightRange();
        int lights = component(vehicle, VehicleRuntimeAuthority.Component.LIGHTS);
        if (lights <= 0) return 0;
        if (lights < 40) base--;
        return Math.max(1, base);
    }

    static int headlightIntensity(MapObjectState vehicle) {
        if (vehicle == null) return 0;
        int lights = component(vehicle, VehicleRuntimeAuthority.Component.LIGHTS);
        int base = profileFor(vehicle).headlightIntensity();
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

    private static void removeTransientSession(MapObjectState vehicle) {
        String vehicleKey = key(vehicle);
        SESSIONS.remove(vehicleKey);
        LAST_SOUND_AT.remove(vehicleKey + ":ambient");
    }

    private static boolean terminalOperationState(MapObjectState vehicle) {
        String operation = MapObjectState.stockValue(vehicle.stockState,
                "operationState").toLowerCase(Locale.ROOT);
        String condition = MapObjectState.stockValue(vehicle.stockState,
                "condition").toLowerCase(Locale.ROOT);
        String ownerType = MapObjectState.stockValue(vehicle.stockState,
                "ownerType").toUpperCase(Locale.ROOT);
        return operation.equals("disabled") || operation.equals("dismantled")
                || condition.equals("wreck") || condition.equals("salvaged")
                || ownerType.equals(VehicleRuntimeAuthority.OwnerType.ABANDONED.name())
                || ownerType.equals(VehicleRuntimeAuthority.OwnerType.SALVAGE.name());
    }

    private static boolean playCue(GamePanel game, MapObjectState vehicle,
                                   String cue, long nowMillis, String phase) {
        if (game == null || game.sounds == null || game.options == null
                || !game.options.soundEnabled || game.options.volume <= 0
                || cue == null || cue.isBlank()) return false;
        int distance = Math.max(1, Math.abs(game.playerX - vehicle.x)
                + Math.abs(game.playerY - vehicle.y));
        if (distance > ambientAudibleRange(vehicle)) return false;
        String soundKey = key(vehicle) + ":" + phase;
        Long prior = LAST_SOUND_AT.get(soundKey);
        long cooldown = "ambient".equals(phase) ? AMBIENT_REFRESH_MILLIS : 300L;
        if (prior != null && nowMillis - prior < cooldown) return false;
        LAST_SOUND_AT.put(soundKey, nowMillis);
        game.sounds.playDistantCue(cue, distance, game.options);
        return true;
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
            Session session = iterator.next().getValue();
            if (nowMillis > session.expiresAtMillis()) {
                LAST_SOUND_AT.remove(session.vehicleKey() + ":ambient");
                iterator.remove();
            }
        }
        while (SESSIONS.size() > MAX_SESSIONS) {
            Iterator<String> keys = SESSIONS.keySet().iterator();
            if (!keys.hasNext()) break;
            String removed = keys.next();
            keys.remove();
            LAST_SOUND_AT.remove(removed + ":ambient");
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
