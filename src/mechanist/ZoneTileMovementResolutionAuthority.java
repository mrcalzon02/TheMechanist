package mechanist;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Resolves simultaneous movement intentions against ZoneTileState actor/content layers, including shove and squeeze movement. */
final class ZoneTileMovementResolutionAuthority {
    static final String VERSION = "0.9.10kf";

    private ZoneTileMovementResolutionAuthority() { }

    record MoveIntent(String actorId, int fromX, int fromY, int toX, int toY, int priority) {
        MoveIntent {
            actorId = actorId == null ? "" : actorId.trim();
        }
        boolean waitsInPlace() { return fromX == toX && fromY == toY; }
        int dx() { return Integer.compare(toX, fromX); }
        int dy() { return Integer.compare(toY, fromY); }
    }

    record MoveOutcome(String actorId, boolean moved, int finalX, int finalY, String reason) { }

    record Resolution(List<MoveOutcome> outcomes, List<String> debugLog, boolean failsafeUsed, String summary) {
        MoveOutcome outcomeFor(String actorId) {
            if (actorId == null) return null;
            for (MoveOutcome outcome : outcomes) if (actorId.equals(outcome.actorId())) return outcome;
            return null;
        }
    }

    static Resolution resolve(ZoneTileState[][] tiles, List<MoveIntent> intents) {
        LinkedHashMap<String, MoveOutcome> outcomes = new LinkedHashMap<>();
        ArrayList<String> debug = new ArrayList<>();
        debug.add("Movement routing start: actor-layer push/squeeze resolver.");
        if (tiles == null || tiles.length == 0 || intents == null || intents.isEmpty()) {
            debug.add("Failsafe: no usable tile grid or movement intents were supplied.");
            return new Resolution(List.copyOf(outcomes.values()), List.copyOf(debug), true, "No movement intents to resolve.");
        }

        Map<String, String> occupantByPosition = occupiedActors(tiles);
        Map<String, String> positionByActor = actorPositions(occupantByPosition);
        ArrayList<MoveIntent> ordered = orderedIntents(intents);
        boolean failsafe = false;

        for (MoveIntent intent : ordered) {
            if (intent == null || intent.actorId().isBlank() || outcomes.containsKey(intent.actorId())) continue;
            debug.add("Intent: " + intent.actorId() + " " + key(intent.fromX(), intent.fromY()) + " -> " + key(intent.toX(), intent.toY()) + " priority " + intent.priority() + ".");
            String sourceKey = key(intent.fromX(), intent.fromY());
            if (!intent.actorId().equals(occupantByPosition.get(sourceKey))) {
                failsafe = true;
                addOutcome(outcomes, debug, new MoveOutcome(intent.actorId(), false, intent.fromX(), intent.fromY(), "Actor is not registered in the source tile actor layer."));
                continue;
            }
            if (intent.waitsInPlace()) {
                addOutcome(outcomes, debug, new MoveOutcome(intent.actorId(), false, intent.fromX(), intent.fromY(), "Actor waited in place."));
                continue;
            }
            MoveAttempt attempt = tryMoveOrPush(intent.actorId(), intent.fromX(), intent.fromY(), intent.toX(), intent.toY(), intent.dx(), intent.dy(), tiles, occupantByPosition, positionByActor, outcomes, debug, new ArrayList<>(), 0);
            if (!attempt.moved()) {
                failsafe = true;
                addOutcome(outcomes, debug, new MoveOutcome(intent.actorId(), false, intent.fromX(), intent.fromY(), attempt.reason()));
            }
        }

        for (Map.Entry<String, String> entry : positionByActor.entrySet()) {
            if (!outcomes.containsKey(entry.getKey())) {
                int[] xy = parseKey(entry.getValue());
                addOutcome(outcomes, debug, new MoveOutcome(entry.getKey(), false, xy[0], xy[1], "Actor held position after crowd movement resolution."));
            }
        }
        debug.add("Movement routing complete: " + playerFacingSummary(outcomes.values()));
        return new Resolution(List.copyOf(outcomes.values()), List.copyOf(debug), failsafe, playerFacingSummary(outcomes.values()));
    }

    static RoutingDebugPlan debugPlanFor(ZoneTileState[][] tiles, MoveIntent intent) {
        ArrayList<String> lines = new ArrayList<>();
        if (intent == null) return new RoutingDebugPlan(false, List.of("No movement intent supplied."));
        lines.add("Route debug for " + intent.actorId() + ": " + key(intent.fromX(), intent.fromY()) + " -> " + key(intent.toX(), intent.toY()) + ".");
        if (tiles == null || tiles.length == 0) {
            lines.add("Failsafe route: no tile grid available; use legacy occupied-tile denial instead of mutating actor slots.");
            return new RoutingDebugPlan(false, List.copyOf(lines));
        }
        if (!inBounds(tiles, intent.fromX(), intent.fromY())) lines.add("Source is outside the zone.");
        if (!inBounds(tiles, intent.toX(), intent.toY())) lines.add("Destination is outside the zone.");
        if (inBounds(tiles, intent.toX(), intent.toY()) && !canStandOn(tiles, intent.toX(), intent.toY())) lines.add("Destination blocks movement by tile state.");
        Map<String, String> occupied = occupiedActors(tiles);
        String targetOccupant = occupied.get(key(intent.toX(), intent.toY()));
        if (targetOccupant == null) lines.add("Destination actor layer is open.");
        else lines.add("Destination actor layer is occupied by " + targetOccupant + "; shove/squeeze relief search required.");
        return new RoutingDebugPlan(lines.stream().noneMatch(line -> line.contains("outside") || line.contains("blocks movement")), List.copyOf(lines));
    }

    static String auditSummary() {
        return "zoneTileMovementResolutionAuthority version=" + VERSION
                + " occupancy=ZoneTileState.actorLayer"
                + " resolves=push+squeeze+chainPush+swap+confinedCrowd+blockedTiles"
                + " debug=routingTrace+failsafeReasons"
                + " legacyGlyph=notMovementAuthority";
    }

    private static MoveAttempt tryMoveOrPush(String actorId, int fromX, int fromY, int toX, int toY, int dx, int dy,
                                             ZoneTileState[][] tiles, Map<String, String> occupantByPosition,
                                             Map<String, String> positionByActor, LinkedHashMap<String, MoveOutcome> outcomes,
                                             ArrayList<String> debug, ArrayList<String> pushStack, int depth) {
        if (depth > Math.max(8, tiles.length * 4)) return new MoveAttempt(false, "Failsafe stopped excessive push-chain recursion.");
        if (!canStandOn(tiles, toX, toY)) return new MoveAttempt(false, "Destination blocks movement.");
        String targetKey = key(toX, toY);
        String targetOccupant = occupantByPosition.get(targetKey);
        if (targetOccupant == null || targetOccupant.equals(actorId)) {
            moveActor(actorId, fromX, fromY, toX, toY, occupantByPosition, positionByActor);
            addOutcome(outcomes, debug, new MoveOutcome(actorId, true, toX, toY, "Movement resolved through open actor-layer space."));
            return new MoveAttempt(true, "Moved into open space.");
        }
        if (pushStack.contains(targetOccupant)) return new MoveAttempt(false, "Crowd compression cycle has no legal relief tile.");

        pushStack.add(actorId);
        debug.add("Push/squeeze: " + actorId + " requests " + targetKey + ", occupied by " + targetOccupant + ".");
        int[] relief = findReliefTile(targetOccupant, toX, toY, dx, dy, tiles, occupantByPosition, pushStack, debug);
        if (relief == null) return new MoveAttempt(false, "Occupied destination has no shove or squeeze relief tile.");

        MoveAttempt displaced = tryMoveOrPush(targetOccupant, toX, toY, relief[0], relief[1], Integer.compare(relief[0], toX), Integer.compare(relief[1], toY), tiles, occupantByPosition, positionByActor, outcomes, debug, pushStack, depth + 1);
        if (!displaced.moved()) return displaced;
        moveActor(actorId, fromX, fromY, toX, toY, occupantByPosition, positionByActor);
        addOutcome(outcomes, debug, new MoveOutcome(actorId, true, toX, toY, "Movement resolved by shoving through confined actor space."));
        return new MoveAttempt(true, "Moved after shove/squeeze relief.");
    }

    private static int[] findReliefTile(String actorId, int x, int y, int dx, int dy, ZoneTileState[][] tiles,
                                        Map<String, String> occupantByPosition, ArrayList<String> pushStack,
                                        ArrayList<String> debug) {
        int[][] candidates = reliefDirections(dx, dy);
        for (int[] candidate : candidates) {
            int nx = x + candidate[0];
            int ny = y + candidate[1];
            if (!canStandOn(tiles, nx, ny)) continue;
            String occupant = occupantByPosition.get(key(nx, ny));
            if (occupant == null || !pushStack.contains(occupant)) {
                debug.add("Relief candidate for " + actorId + ": " + key(nx, ny) + ".");
                return new int[] { nx, ny };
            }
        }
        debug.add("No legal relief candidate for " + actorId + " at " + key(x, y) + ".");
        return null;
    }

    private static int[][] reliefDirections(int dx, int dy) {
        if (dx != 0) return new int[][] { {dx, 0}, {0, 1}, {0, -1}, {-dx, 0} };
        if (dy != 0) return new int[][] { {0, dy}, {1, 0}, {-1, 0}, {0, -dy} };
        return new int[][] { {1, 0}, {-1, 0}, {0, 1}, {0, -1} };
    }

    private static void moveActor(String actorId, int fromX, int fromY, int toX, int toY,
                                  Map<String, String> occupantByPosition, Map<String, String> positionByActor) {
        occupantByPosition.remove(key(fromX, fromY));
        occupantByPosition.put(key(toX, toY), actorId);
        positionByActor.put(actorId, key(toX, toY));
    }

    private static void addOutcome(LinkedHashMap<String, MoveOutcome> outcomes, ArrayList<String> debug, MoveOutcome outcome) {
        if (outcome == null || outcome.actorId().isBlank()) return;
        outcomes.put(outcome.actorId(), outcome);
        debug.add("Outcome: " + outcome.actorId() + " -> " + key(outcome.finalX(), outcome.finalY()) + " moved=" + outcome.moved() + " reason=" + outcome.reason());
    }

    private static ArrayList<MoveIntent> orderedIntents(List<MoveIntent> intents) {
        ArrayList<MoveIntent> ordered = new ArrayList<>();
        for (MoveIntent intent : intents) if (intent != null) ordered.add(intent);
        ordered.sort((a, b) -> Integer.compare(b.priority(), a.priority()));
        return ordered;
    }

    private static boolean canStandOn(ZoneTileState[][] tiles, int x, int y) {
        return inBounds(tiles, x, y) && tiles[x][y] != null && !tiles[x][y].hasFlag(ZoneTileState.TileFlag.BLOCKS_MOVEMENT);
    }

    private static Map<String, String> occupiedActors(ZoneTileState[][] tiles) {
        HashMap<String, String> occupied = new HashMap<>();
        for (int x = 0; x < tiles.length; x++) {
            ZoneTileState[] column = tiles[x];
            if (column == null) continue;
            for (int y = 0; y < column.length; y++) {
                ZoneTileState tile = column[y];
                String occupantId = tile == null ? "" : tile.occupantEntityId();
                if (occupantId == null || occupantId.isBlank()) continue;
                occupied.put(key(x, y), occupantId);
            }
        }
        return occupied;
    }

    private static Map<String, String> actorPositions(Map<String, String> occupantByPosition) {
        HashMap<String, String> positions = new HashMap<>();
        for (Map.Entry<String, String> entry : occupantByPosition.entrySet()) positions.put(entry.getValue(), entry.getKey());
        return positions;
    }

    private static boolean inBounds(ZoneTileState[][] tiles, int x, int y) {
        return x >= 0 && y >= 0 && x < tiles.length && tiles[x] != null && y < tiles[x].length;
    }

    private static int[] parseKey(String key) {
        if (key == null || !key.contains(",")) return new int[] {0, 0};
        String[] parts = key.split(",", 2);
        try { return new int[] { Integer.parseInt(parts[0]), Integer.parseInt(parts[1]) }; }
        catch (RuntimeException ex) { return new int[] {0, 0}; }
    }

    private static String key(int x, int y) { return x + "," + y; }

    private static String playerFacingSummary(Iterable<MoveOutcome> outcomes) {
        int moved = 0;
        int held = 0;
        for (MoveOutcome outcome : outcomes) {
            if (outcome.moved()) moved++; else held++;
        }
        return PlayerFacingText.sanitize("Movement resolution: " + moved + " moved, " + held + " held; actor-layer push/squeeze used instead of legacy glyphs.");
    }

    private record MoveAttempt(boolean moved, String reason) { }
    record RoutingDebugPlan(boolean routeSafe, List<String> lines) { }
}
