package mechanist;

import java.util.Locale;

/** Owns per-machine background queue blocker and output-routing choices. */
final class ProductionQueuePolicyAuthority {
    enum MaterialPolicy {
        WAIT("Wait / keep worker"), RELEASE("Pause / release worker"), CANCEL("Cancel remaining queue");
        final String label;
        MaterialPolicy(String label) { this.label = label; }
    }

    enum OutputPolicy {
        BASE("Base Storage"), NEAREST("Nearest Cache"), FLOOR("Floor Pile");
        final String label;
        OutputPolicy(String label) { this.label = label; }
    }

    enum NoRoomPolicy {
        WAIT("Wait / keep worker"), RELEASE("Pause / release worker"), CANCEL("Cancel remaining queue"), FLOOR("Dump to Floor");
        final String label;
        NoRoomPolicy(String label) { this.label = label; }
    }

    enum DestinationKind { BASE_STORAGE, CONTAINER, FLOOR }

    record DestinationPlan(boolean ready, DestinationKind kind, String containerId,
                           String label, int x, int y, String blocker) { }

    private ProductionQueuePolicyAuthority() { }

    static MaterialPolicy materialPolicy(BaseObject machine) {
        return parse(MaterialPolicy.class, machine == null ? null : machine.productionMaterialPolicy, MaterialPolicy.WAIT);
    }

    static OutputPolicy outputPolicy(BaseObject machine) {
        return parse(OutputPolicy.class, machine == null ? null : machine.productionOutputPolicy, OutputPolicy.BASE);
    }

    static NoRoomPolicy noRoomPolicy(BaseObject machine) {
        return parse(NoRoomPolicy.class, machine == null ? null : machine.productionNoRoomPolicy, NoRoomPolicy.WAIT);
    }

    static String cycleMaterialPolicy(BaseObject machine) {
        MaterialPolicy next = next(materialPolicy(machine));
        if (machine != null) machine.productionMaterialPolicy = next.name();
        return "Material shortage policy: " + next.label + ".";
    }

    static String cycleOutputPolicy(BaseObject machine) {
        OutputPolicy next = next(outputPolicy(machine));
        if (machine != null) machine.productionOutputPolicy = next.name();
        return "Production output destination: " + next.label + ".";
    }

    static String cycleNoRoomPolicy(BaseObject machine) {
        NoRoomPolicy next = next(noRoomPolicy(machine));
        if (machine != null) machine.productionNoRoomPolicy = next.name();
        return "No-room policy: " + next.label + ".";
    }

    static String policyLine(BaseObject machine) {
        if (machine == null) return "Queue policies unavailable: no selected machine.";
        String blocker = machine.productionLastBlocker == null || machine.productionLastBlocker.isBlank()
                ? "none" : machine.productionLastBlocker;
        return "Queue policies: materials " + materialPolicy(machine).label
                + "; output " + outputPolicy(machine).label
                + "; no room " + noRoomPolicy(machine).label
                + "; last blocker " + blocker + ".";
    }

    static DestinationPlan destinationPlan(GamePanel game, BaseObject machine, int outputCount) {
        if (game == null || machine == null) return blocked("No active production destination.");
        OutputPolicy policy = outputPolicy(machine);
        if (policy == OutputPolicy.BASE) {
            return new DestinationPlan(true, DestinationKind.BASE_STORAGE, GamePanel.CONTAINER_BASE_STORAGE,
                    "Base Storage", machine.x, machine.y, "");
        }
        if (policy == OutputPolicy.FLOOR) return floorPlan(game, machine);

        MapObjectState nearest = nearestFactionContainer(game, machine, Math.max(1, outputCount));
        if (nearest == null) return blocked("No claimed-room faction container has room for the output");
        String id = game.persistentContainerIdForObject(nearest);
        return new DestinationPlan(true, DestinationKind.CONTAINER, id,
                nearest.label == null || nearest.label.isBlank() ? "nearest faction container" : nearest.label,
                nearest.x, nearest.y, "");
    }

    static DestinationPlan floorPlan(GamePanel game, BaseObject machine) {
        if (game == null || game.world == null || machine == null) return blocked("No loaded floor can receive production output");
        MapObjectState existing = floorPile(game, machine);
        if (existing != null) return new DestinationPlan(true, DestinationKind.FLOOR,
                game.persistentContainerIdForObject(existing), existing.label, existing.x, existing.y, "");
        for (int radius = 1; radius <= 3; radius++) {
            for (int dx = -radius; dx <= radius; dx++) for (int dy = -radius; dy <= radius; dy++) {
                if (Math.max(Math.abs(dx), Math.abs(dy)) != radius) continue;
                int x = machine.x + dx, y = machine.y + dy;
                if (!game.world.inBounds(x, y) || !game.world.walkable(x, y)
                        || game.world.mapObjectAt(x, y) != null || baseObjectAt(game, x, y) != null) continue;
                return new DestinationPlan(true, DestinationKind.FLOOR, floorContainerId(machine),
                        "Production output pile for " + machine.name, x, y, "");
            }
        }
        return blocked("No clear floor tile is available near " + machine.name);
    }

    static void routeOutput(GamePanel game, BaseObject machine, DestinationPlan plan,
                            String output, ItemProvenanceRecord provenance) {
        if (game == null || plan == null || !plan.ready() || output == null || output.isBlank()) return;
        provenance.route = (provenance.route == null || provenance.route.isBlank() ? "produced" : provenance.route)
                + " -> " + plan.label();
        if (plan.kind() == DestinationKind.BASE_STORAGE) {
            game.baseStorage.add(output);
            game.rememberItemProvenance(output, provenance);
            return;
        }
        String containerId = plan.containerId();
        String label = plan.label();
        if (plan.kind() == DestinationKind.FLOOR) {
            MapObjectState pile = ensureFloorPile(game, machine, plan);
            containerId = game.persistentContainerIdForObject(pile);
            label = pile.label;
        }
        game.addItemToContainerResult(containerId, label, output, provenance, machine,
                "staffed production output routing");
        game.rememberItemProvenance(output, provenance);
    }

    static int containerCapacity(MapObjectState container) {
        if (container == null) return 0;
        String raw = MapObjectState.stockValue(container.stockState, "capacity");
        try { return Math.max(0, Integer.parseInt(raw)); }
        catch (Exception ignored) { return 20; }
    }

    private static MapObjectState nearestFactionContainer(GamePanel game, BaseObject machine, int needed) {
        if (game.world == null) return null;
        MapObjectState best = null;
        int bestDistance = Integer.MAX_VALUE;
        for (MapObjectState object : game.world.mapObjects) {
            if (object == null || object.type == null) continue;
            String type = object.type.toLowerCase(Locale.ROOT);
            if (!type.contains("container") || type.contains("production-output")) continue;
            if (!game.isInClaimedRoom(object.x, object.y)) continue;
            String owner = MapObjectState.stockValue(object.stockState, "ownerFaction");
            if (!owner.isBlank() && !owner.equalsIgnoreCase("PLAYER")) {
                if (machine.faction == null || machine.faction == Faction.NONE
                        || !owner.equalsIgnoreCase(machine.faction.name())) continue;
            }
            int free = Math.max(0, containerCapacity(object) - game.containerItemCount(game.persistentContainerIdForObject(object)));
            if (free < needed) continue;
            int distance = Math.abs(object.x - machine.x) + Math.abs(object.y - machine.y);
            if (distance < bestDistance) { best = object; bestDistance = distance; }
        }
        return best;
    }

    private static MapObjectState ensureFloorPile(GamePanel game, BaseObject machine, DestinationPlan plan) {
        MapObjectState existing = floorPile(game, machine);
        if (existing != null) return existing;
        MapObjectState pile = new MapObjectState();
        pile.id = floorPileId(machine);
        pile.type = "production-output-container";
        pile.label = plan.label();
        pile.x = plan.x(); pile.y = plan.y(); pile.glyph = 'o';
        char underlying = game.world.inBounds(pile.x, pile.y) ? game.world.tiles[pile.x][pile.y] : '.';
        pile.stockState = "ownerFaction=PLAYER;capacity=999;under=" + (int)underlying;
        game.world.mapObjects.add(pile);
        if (game.world.inBounds(pile.x, pile.y)) game.world.tiles[pile.x][pile.y] = pile.glyph;
        game.ensureContainer(game.persistentContainerIdForObject(pile), pile.label);
        return pile;
    }

    private static MapObjectState floorPile(GamePanel game, BaseObject machine) {
        if (game == null || game.world == null || machine == null) return null;
        String id = floorPileId(machine);
        for (MapObjectState object : game.world.mapObjects) if (object != null && id.equals(object.id)) return object;
        return null;
    }

    private static BaseObject baseObjectAt(GamePanel game, int x, int y) {
        for (BaseObject object : game.baseObjects) if (object != null && object.x == x && object.y == y) return object;
        return null;
    }

    private static String floorPileId(BaseObject machine) {
        return "PRODUCTION-OUTPUT-" + Math.abs(java.util.Objects.hash(machine.name, machine.x, machine.y));
    }

    private static String floorContainerId(BaseObject machine) {
        return GamePanel.CONTAINER_ROOM_CACHE_PREFIX + floorPileId(machine);
    }

    private static DestinationPlan blocked(String reason) {
        return new DestinationPlan(false, DestinationKind.BASE_STORAGE, "", "", -1, -1, reason);
    }

    private static <E extends Enum<E>> E parse(Class<E> type, String raw, E fallback) {
        if (raw == null || raw.isBlank()) return fallback;
        try { return Enum.valueOf(type, raw.trim().toUpperCase(Locale.ROOT)); }
        catch (Exception ignored) { return fallback; }
    }

    private static <E extends Enum<E>> E next(E value) {
        E[] values = value.getDeclaringClass().getEnumConstants();
        return values[(value.ordinal() + 1) % values.length];
    }
}
