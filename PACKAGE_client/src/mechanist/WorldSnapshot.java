package mechanist;

import java.util.ArrayList;
import java.util.List;

/** Immutable renderer/network-facing view of committed authoritative world state. */
record WorldSnapshot(long version,
                     SectorKey currentSector,
                     PlayerSnapshot player,
                     List<TileSnapshot> visibleTiles,
                     List<NpcSnapshot> visibleNpcs,
                     List<ObjectSnapshot> visibleObjects,
                     List<String> recentActions,
                     UiStateSnapshot uiState,
                     long committedAtMillis) {
    static final int TILE_RADIUS = 12;
    static final int MAX_NPCS = 80;
    static final int MAX_OBJECTS = 120;
    static final int MAX_ACTIONS = 12;

    static WorldSnapshot fromGame(long version, GamePanel game, SectorKey sector) {
        if (game == null) {
            return new WorldSnapshot(version, sector, PlayerSnapshot.empty(), List.of(), List.of(), List.of(), List.of(), UiStateSnapshot.empty(), System.currentTimeMillis());
        }
        PlayerSnapshot player = PlayerSnapshot.fromGame(game);
        ArrayList<TileSnapshot> tiles = new ArrayList<>();
        World w = game.world;
        if (w != null && w.tiles != null) {
            int minX = Math.max(0, game.playerX - TILE_RADIUS);
            int maxX = Math.min(w.w - 1, game.playerX + TILE_RADIUS);
            int minY = Math.max(0, game.playerY - TILE_RADIUS);
            int maxY = Math.min(w.h - 1, game.playerY + TILE_RADIUS);
            for (int y = minY; y <= maxY; y++) {
                for (int x = minX; x <= maxX; x++) {
                    boolean visible = game.isVisible(x, y);
                    boolean remembered = game.isRemembered(x, y);
                    if (!visible && !remembered) continue;
                    char glyph = w.tiles[x][y];
                    CompiledTileDescriptor d = TileDataCompilationAuthority.resolve(w, x, y, glyph);
                    tiles.add(TileSnapshot.fromDescriptor(x, y, glyph, visible, remembered, d));
                }
            }
        }
        ArrayList<NpcSnapshot> npcs = new ArrayList<>();
        if (w != null && w.npcs != null) {
            for (NpcEntity n : w.npcs) {
                if (n == null) continue;
                if (!game.isVisible(n.x, n.y) && Math.abs(n.x - game.playerX) + Math.abs(n.y - game.playerY) > 3) continue;
                npcs.add(NpcSnapshot.fromNpc(n));
                if (npcs.size() >= MAX_NPCS) break;
            }
        }
        ArrayList<ObjectSnapshot> objects = new ArrayList<>();
        if (game.baseObjects != null) {
            for (BaseObject o : game.baseObjects) {
                if (o == null) continue;
                if (!game.isVisible(o.x, o.y) && !game.isRemembered(o.x, o.y)) continue;
                objects.add(ObjectSnapshot.fromBaseObject(o));
                if (objects.size() >= MAX_OBJECTS) break;
            }
        }
        if (w != null && w.mapObjects != null && objects.size() < MAX_OBJECTS) {
            for (MapObjectState o : w.mapObjects) {
                if (o == null) continue;
                if (!game.isVisible(o.x, o.y) && !game.isRemembered(o.x, o.y)) continue;
                objects.add(ObjectSnapshot.fromMapObject(o));
                if (objects.size() >= MAX_OBJECTS) break;
            }
        }
        ArrayList<String> actions = new ArrayList<>();
        if (game.eventLog != null) {
            int start = Math.max(0, game.eventLog.size() - MAX_ACTIONS);
            for (int i = start; i < game.eventLog.size(); i++) actions.add(clean(game.eventLog.get(i)));
        }
        return new WorldSnapshot(version, sector, player, List.copyOf(tiles), List.copyOf(npcs), List.copyOf(objects), List.copyOf(actions), UiStateSnapshot.fromGame(game), System.currentTimeMillis());
    }

    String compact() {
        return "snapshot=v" + version
                + " sector=" + (currentSector == null ? "none" : currentSector.compact())
                + " player=" + player.compact()
                + " tiles=" + visibleTiles.size()
                + " npcs=" + visibleNpcs.size()
                + " objects=" + visibleObjects.size()
                + " ui=" + uiState.compact();
    }

    private static String clean(String s) { return s == null ? "" : s.replace('\n', ' ').trim(); }
}

record PlayerSnapshot(String id,
                      String name,
                      int x,
                      int y,
                      long turn,
                      long worldTurn,
                      int food,
                      int water,
                      int sleepNeed,
                      int carriedScript,
                      int heat,
                      int suspicion,
                      String facing,
                      String motionState,
                      String activeAction) {
    static PlayerSnapshot empty() { return new PlayerSnapshot("none", "none", 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, "E", "stationary", "none"); }
    static PlayerSnapshot fromGame(GamePanel game) {
        String action = game.singlePlayerSectorBridge == null ? "none" : game.singlePlayerSectorBridge.activeActionDisplayLine();
        if (action == null || action.isBlank()) action = "none";
        String name = game.active == null ? "none" : game.active.name;
        return new PlayerSnapshot(SinglePlayerSectorRuntimeBridge.LOCAL_PLAYER_ID, clean(name), game.playerX, game.playerY,
                game.turn, game.worldTurn, game.food, game.water, game.sleepNeed, game.countMoney(), game.gangHeat, game.suspicion,
                game.facingLabel(), game.activeMotionStateLabel(), clean(action));
    }
    String compact() { return id + "@" + x + "," + y + " turn=" + turn + " action=" + activeAction; }
    private static String clean(String s) { return s == null ? "" : s.replace('\n', ' ').trim(); }
}

record TileSnapshot(int x,
                    int y,
                    char glyph,
                    boolean visible,
                    boolean remembered,
                    String baseLayer,
                    String family,
                    String shape,
                    int variant,
                    String primaryArtKey,
                    String underlayArtKey,
                    String overlayArtKey,
                    String semanticTag,
                    String composedKey) {
    static TileSnapshot fromDescriptor(int x, int y, char glyph, boolean visible, boolean remembered, CompiledTileDescriptor d) {
        if (d == null) return new TileSnapshot(x, y, glyph, visible, remembered, "fallback", "legacy", "", 0, "", "", "", "", "tile.unknown");
        return new TileSnapshot(x, y, glyph, visible, remembered, clean(d.baseLayer), clean(d.family), clean(d.shape), d.variant,
                clean(d.primaryArtKey), clean(d.underlayArtKey), clean(d.overlayArtKey), clean(d.semanticTag), clean(d.composedKey));
    }
    private static String clean(String s) { return s == null ? "" : s.replace('\n', ' ').trim(); }
}

record NpcSnapshot(String id,
                   String name,
                   String faction,
                   String role,
                   char symbol,
                   int x,
                   int y,
                   int hp,
                   String state) {
    static NpcSnapshot fromNpc(NpcEntity n) {
        return new NpcSnapshot(clean(n.id), clean(n.name), n.faction == null ? "NONE" : n.faction.name(), clean(n.role), n.symbol, n.x, n.y, n.hp, clean(n.state));
    }
    private static String clean(String s) { return s == null ? "" : s.replace('\n', ' ').trim(); }
}

record ObjectSnapshot(String id,
                      String name,
                      String type,
                      char symbol,
                      int x,
                      int y,
                      String state) {
    static ObjectSnapshot fromBaseObject(BaseObject o) {
        return new ObjectSnapshot("base-object-" + o.x + "-" + o.y + "-" + o.symbol, clean(o.name), "base-object", o.symbol, o.x, o.y, clean(o.qualityName));
    }
    static ObjectSnapshot fromMapObject(MapObjectState o) {
        return new ObjectSnapshot(clean(o.id), clean(o.label), clean(o.type), o.glyph, o.x, o.y, clean(o.stockState));
    }
    private static String clean(String s) { return s == null ? "" : s.replace('\n', ' ').trim(); }
}

record UiStateSnapshot(String screen,
                       String panel,
                       int lookX,
                       int lookY,
                       boolean lookCursorActive,
                       String zone,
                       int inventoryCount,
                       String serverAction) {
    static UiStateSnapshot empty() { return new UiStateSnapshot("none", "none", 0, 0, false, "none", 0, "none"); }
    static UiStateSnapshot fromGame(GamePanel game) {
        String zone = game.world == null ? "none" : game.world.zoneType.label + " " + game.world.zoneCoordText();
        String action = game.singlePlayerSectorBridge == null ? "none" : game.singlePlayerSectorBridge.activeActionDisplayLine();
        if (action == null || action.isBlank()) action = "none";
        return new UiStateSnapshot(String.valueOf(game.screen), String.valueOf(game.panelMode), game.lookX, game.lookY, game.lookCursorActive, clean(zone), game.inventory.size(), clean(action));
    }
    String compact() { return screen + "/" + panel + " look=" + lookX + "," + lookY + " action=" + serverAction; }
    private static String clean(String s) { return s == null ? "" : s.replace('\n', ' ').trim(); }
}
