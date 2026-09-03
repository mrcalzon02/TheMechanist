package mechanist;

import java.util.ArrayList;
import java.util.List;

/**
 * Proves renderer/network-facing world snapshots detach from caller-owned
 * collections and normalize nullable nested snapshot state at publication.
 */
final class WorldSnapshotImmutabilitySmoke {
    public static void main(String[] args) {
        ArrayList<String> actions = new ArrayList<>();
        actions.add("committed action");
        ArrayList<TileSnapshot> tiles = new ArrayList<>();
        tiles.add(new TileSnapshot(
                1,
                2,
                '.',
                true,
                true,
                "floor",
                "smoke",
                "",
                0,
                "",
                "",
                "",
                "",
                "tile.smoke"));

        WorldSnapshot snapshot = new WorldSnapshot(
                7L,
                null,
                null,
                tiles,
                null,
                null,
                actions,
                null,
                1L);

        actions.add("late mutation");
        tiles.clear();

        require(snapshot.player() != null,
                "null player snapshot was not normalized");
        require(snapshot.uiState() != null,
                "null UI snapshot was not normalized");
        require(snapshot.visibleNpcs().isEmpty(),
                "null NPC collection was not normalized");
        require(snapshot.visibleObjects().isEmpty(),
                "null object collection was not normalized");
        require(snapshot.recentActions().equals(List.of("committed action")),
                "published action history retained caller mutation authority");
        require(snapshot.visibleTiles().size() == 1,
                "published tile view retained caller mutation authority");
        require(snapshot.compact().contains("player=none@0,0"),
                "normalized snapshot could not render a compact audit line");

        boolean immutable = false;
        try {
            snapshot.recentActions().add("illegal mutation");
        } catch (UnsupportedOperationException expected) {
            immutable = true;
        }
        require(immutable,
                "published action history remained externally mutable");

        System.out.println(
                "WorldSnapshotImmutabilitySmoke PASS"
                        + " detachedCollections=true"
                        + " nullStateNormalized=true"
                        + " compactAuditSafe=true");
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }
}
