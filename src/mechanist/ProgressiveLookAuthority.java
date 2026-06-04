package mechanist;

import java.util.ArrayList;
import java.util.Locale;

/**
 * Progressive target inspection. A passive hover/selection shows surface facts;
 * repeated LOOK/EXAMINE on the same tile deepens the read.
 */
final class ProgressiveLookAuthority {
    private static final int NO_TARGET = Integer.MIN_VALUE;
    private static final int MAX_DEPTH = 4;

    private ProgressiveLookAuthority() {}

    static void reset(GamePanel game, String reason) {
        if (game == null) return;
        game.lookFocusX = NO_TARGET;
        game.lookFocusY = NO_TARGET;
        game.lookFocusDepth = 0;
    }

    static int advance(GamePanel game, int x, int y) {
        if (game == null) return 0;
        if (game.lookFocusX == x && game.lookFocusY == y) {
            game.lookFocusDepth = Math.min(MAX_DEPTH, game.lookFocusDepth + 1);
        } else {
            game.lookFocusX = x;
            game.lookFocusY = y;
            game.lookFocusDepth = 1;
        }
        return game.lookFocusDepth;
    }

    static int depthFor(GamePanel game, int x, int y) {
        if (game == null || game.lookFocusX != x || game.lookFocusY != y) return 0;
        return Math.max(0, Math.min(MAX_DEPTH, game.lookFocusDepth));
    }

    static ArrayList<String> tileStackAt(GamePanel game, int x, int y) {
        return tileStackAt(game, x, y, depthFor(game, x, y));
    }

    static ArrayList<String> tileStackAt(GamePanel game, int x, int y, int depth) {
        ArrayList<String> lines = new ArrayList<>();
        if (game == null || game.world == null) {
            lines.add("No world loaded.");
            return lines;
        }
        if (!game.world.inBounds(x, y)) {
            lines.add("Out of bounds target " + x + "," + y + ".");
            return lines;
        }
        int d = Math.max(0, Math.min(MAX_DEPTH, depth));
        char ch = game.world.tiles[x][y];
        lines.add("Observation depth " + d + "/" + MAX_DEPTH + (d < MAX_DEPTH ? " - look again for more." : " - full local read."));
        lines.add("Tile glyph " + ch + " / walkable " + game.world.walkable(x, y));
        CompiledTileDescriptor descriptor = TileDataCompilationAuthority.resolve(game.world, x, y, ch);
        if (descriptor != null && d >= 1) lines.add(descriptor.inspectLine());
        if (x == game.playerX && y == game.playerY) lines.add("Player position.");

        NpcEntity npc = game.world.npcAt(x, y);
        if (npc != null) addNpcLines(game, lines, npc, d);

        MapObjectState obj = game.world.mapObjectAt(x, y);
        if (obj != null) addObjectLines(game, lines, obj, d);

        BaseObject base = game.baseObjectAt(x, y);
        if (base != null) addBaseObjectLines(game, lines, base, d);

        if (game.isDoorTile(ch)) {
            lines.add("Door/access tile: interact can operate this doorway.");
            if (d >= 1) lines.add("Door light: local threshold emitter active; wall-blocked lighting should strike the door/bulkhead but not leak beyond it.");
        }
        return lines;
    }

    private static void addNpcLines(GamePanel game, ArrayList<String> lines, NpcEntity npc, int depth) {
        lines.add("Figure: " + safe(npc.name, "unknown") + " / " + safe(npc.role, "unknown role") + ".");
        if (depth >= 1) lines.add("Faction read: " + (npc.faction == null ? "None" : npc.faction.label) + " / symbol " + (npc.symbol == 0 ? '@' : npc.symbol) + ".");
        if (depth >= 2) lines.add(npc.rankLine());
        if (depth >= 2) lines.add(intentLine(game, npc));
        if (depth >= 3) lines.add("State: " + safe(npc.state, "unknown") + " / HP " + npc.hp + " / age " + npc.ageLine() + ".");
        if (depth >= 4) {
            ArrayList<String> loadout = new ArrayList<>();
            if (npc.equippedMeleeWeapon != null && !npc.equippedMeleeWeapon.isBlank()) loadout.add(npc.equippedMeleeWeapon);
            if (npc.equippedRangedWeapon != null && !npc.equippedRangedWeapon.isBlank()) loadout.add(npc.equippedRangedWeapon + (npc.loadedShots > 0 ? " (" + npc.loadedShots + " loaded)" : ""));
            if (npc.equippedArmor != null && !npc.equippedArmor.isBlank()) loadout.add(npc.equippedArmor);
            if (npc.equippedExplosive != null && !npc.equippedExplosive.isBlank()) loadout.add(npc.equippedExplosive);
            lines.add("Visible kit: " + (loadout.isEmpty() ? "no obvious weapons or armor" : String.join(", ", loadout)) + ".");
            if (npc.provenance != null) lines.add("Personnel source: " + npc.provenance.populationPool + " / " + npc.provenance.upbringing + ".");
        }
    }

    private static void addObjectLines(GamePanel game, ArrayList<String> lines, MapObjectState obj, int depth) {
        lines.add("Object: " + game.safeLabel(obj.label, obj.type) + ".");
        if (depth >= 1) lines.add("Type: " + game.safeLabel(obj.type, "object") + " / stock: " + game.safeLabel(obj.stockState, "none") + ".");
        if (depth >= 2) {
            FixtureInteractionRegistry.Definition def = FixtureInteractionRegistry.definitionFor(obj.type);
            if (def != null) lines.add("Fixture: " + def.family.label + " / " + def.family.interaction + " / " + def.notes);
        }
        if (depth >= 3) lines.add(ObjectSemanticAssetAuthority.semanticSummaryForName(obj.label));
    }

    private static void addBaseObjectLines(GamePanel game, ArrayList<String> lines, BaseObject base, int depth) {
        lines.add("Base object: " + game.safeLabel(base.name, "base object") + ".");
        if (depth >= 1) lines.add("Build read: " + base.symbol + " / " + game.safeLabel(base.qualityName, "Common") + " quality.");
        if (depth >= 2) lines.add(game.safeLabel(base.description, "Built base object."));
        if (depth >= 3) lines.add("Integrity/capacity: " + base.integrity + " / " + base.capacity + ".");
        if (depth >= 4) lines.add("Faction/work: " + (base.faction == null ? "None" : base.faction.label) + " / recipe " + game.safeLabel(base.assignedRecipe, "unassigned") + " / worker " + game.safeLabel(base.assignedWorker, "unassigned") + ".");
    }

    private static String intentLine(GamePanel game, NpcEntity npc) {
        String state = npc.state == null ? "" : npc.state.toLowerCase(Locale.ROOT);
        String role = npc.role == null ? "" : npc.role.toLowerCase(Locale.ROOT);
        boolean hostile = npc.faction != null && game.temporaryHostileTurns.getOrDefault(npc.faction, 0) > game.turn;
        if (hostile || state.contains("attack") || state.contains("hostile")) return "Intent read: hostile posture; expect movement or violence if line is open.";
        if (state.contains("contract") || role.contains("representative") || role.contains("trader")) return "Intent read: service/social posture; likely interaction route rather than combat.";
        if (state.contains("patrol") || role.contains("guard") || role.contains("warden")) return "Intent read: area-control posture; likely to challenge suspicious movement.";
        if (npc.isAnimalActor()) return "Intent read: animal behavior; watch proximity and handler context.";
        return "Intent read: no immediate attack tell; behavior appears tied to " + safe(npc.state, "local routine") + ".";
    }

    private static String safe(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
