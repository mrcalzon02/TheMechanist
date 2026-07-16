package mechanist;

import java.util.ArrayList;
import java.util.List;
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
        lines.add(observationDepthLine(d));
        lines.add(tileSurfaceLine(game.world.walkable(x, y), ch));
        CompiledTileDescriptor descriptor = TileDataCompilationAuthority.resolve(game.world, x, y, ch);
        if (descriptor != null && d >= 1) lines.add(tileDescriptorLine(descriptor));
        lines.addAll(RoomOwnershipAuthority.inspectionLines(game, x, y, d));
        if (x == game.playerX && y == game.playerY) lines.add("Player position.");

        NpcEntity npc = game.world.npcAt(x, y);
        if (npc != null) addNpcLines(game, lines, npc, d);

        MapObjectState obj = game.world.mapObjectAt(x, y);
        if (obj != null) addObjectLines(game, lines, obj, d);

        BaseObject base = game.baseObjectAt(x, y);
        if (base != null) addBaseObjectLines(game, lines, base, d);

        if (game.isDoorTile(ch)) {
            lines.add("Door or access point: interact can operate this doorway.");
            if (d >= 1) lines.add("Door light: local threshold emitter active; wall-blocked lighting should strike the door/bulkhead but not leak beyond it.");
        }
        return lines;
    }

    static String observationDepthLine(int depth) {
        int d = Math.max(0, Math.min(MAX_DEPTH, depth));
        return "Observation " + d + "/" + MAX_DEPTH + (d < MAX_DEPTH
                ? ": look again for a clearer read."
                : ": close local examination complete.");
    }

    static String tileSurfaceLine(boolean walkable, char tile) {
        String movement = walkable ? "passable" : "blocked";
        return PlayerFacingText.inspectionTile(tileFamilyLabel(tile), "Surface appears " + movement + ".");
    }

    static String tileDescriptorLine(CompiledTileDescriptor descriptor) {
        if (descriptor == null) return PlayerFacingText.inspectionTile("", "");
        StringBuilder detail = new StringBuilder();
        detail.append(descriptor.semanticTag == null || descriptor.semanticTag.isBlank()
                ? familyLabel(descriptor.family)
                : readableToken(descriptor.semanticTag));
        if (descriptor.isDoor) detail.append("; access point");
        else if (descriptor.isFixture) detail.append("; fixture");
        else if (descriptor.isRoad) detail.append("; road surface");
        else if (descriptor.isSidewalk) detail.append("; walkway");
        else if (descriptor.isCorridor) detail.append("; corridor");
        else if (descriptor.isWall) detail.append("; solid boundary");
        if (descriptor.hasOverlay()) detail.append("; layered detail");
        return PlayerFacingText.inspectionTile(tileFamilyLabel(descriptor.sourceGlyph), detail.toString());
    }

    private static void addNpcLines(GamePanel game, ArrayList<String> lines, NpcEntity npc, int depth) {
        lines.add(PlayerFacingText.inspectionActor(safe(npc.name, "unknown figure"), safe(npc.role, "unknown role")));
        if (depth >= 1) lines.add("Faction read: " + (npc.faction == null ? "unknown" : npc.faction.label) + ".");
        if (depth >= 2) lines.add(npc.rankLine());
        if (depth >= 2) lines.add(NpcHappinessAuthority.statusLine(game.world, npc, game.worldTurn));
        if (depth >= 2) lines.add(intentLine(game, npc));
        if (depth >= 3) lines.add("Visible condition: " + EntityIdentityReadabilityAuthority.conditionBand(npc.hp) + " / age " + npc.ageLine() + ".");
        if (depth >= 4) {
            ArrayList<String> loadout = new ArrayList<>();
            if (npc.equippedMeleeWeapon != null && !npc.equippedMeleeWeapon.isBlank()) loadout.add(npc.equippedMeleeWeapon);
            if (npc.equippedRangedWeapon != null && !npc.equippedRangedWeapon.isBlank()) loadout.add(npc.equippedRangedWeapon + (npc.loadedShots > 0 ? " (" + npc.loadedShots + " loaded)" : ""));
            if (npc.equippedArmor != null && !npc.equippedArmor.isBlank()) loadout.add(npc.equippedArmor);
            if (npc.equippedExplosive != null && !npc.equippedExplosive.isBlank()) loadout.add(npc.equippedExplosive);
            lines.add("Visible kit: " + (loadout.isEmpty() ? "no obvious weapons or armor" : String.join(", ", loadout)) + ".");
            if (npc.provenance != null) lines.add("Personnel background: " + npc.provenance.populationPool + "; " + npc.provenance.upbringing + ".");
        }
    }

    private static void addObjectLines(GamePanel game, ArrayList<String> lines, MapObjectState obj, int depth) {
        lines.add(PlayerFacingText.inspectionFixture(game.safeLabel(obj.label, obj.type), "nearby object"));
        if (depth >= 1) lines.add("Object read: " + game.safeLabel(obj.type, "object") + "; stock appears " + game.safeLabel(obj.stockState, "none") + ".");
        if (depth >= 2 && FactionImportNodeGenerationAuthority.isImportNode(obj)) {
            List<String> importLines = FactionImportNodeGenerationAuthority.inspectionLines(game.world, obj, game.worldTurn);
            lines.addAll(importLines);
        }
        if (depth >= 2) {
            FixtureInteractionRegistry.Definition def = FixtureInteractionRegistry.definitionFor(obj.type);
            if (def != null) lines.add("Fixture: " + def.family.label + " / " + def.family.interaction + " / " + def.notes);
        }
        if (depth >= 3) lines.add(ObjectSemanticAssetAuthority.semanticSummaryForName(obj.label));
    }

    private static void addBaseObjectLines(GamePanel game, ArrayList<String> lines, BaseObject base, int depth) {
        lines.add(PlayerFacingText.inspectionFixture(game.safeLabel(base.name, "base object"), "built fixture"));
        if (base.underConstruction && depth >= 1) {
            lines.addAll(ProgressiveConstructionAuthority.inspectionLines(base));
        } else if (depth >= 1 && FactionPhysicalConstructionAuthority.isFactionManaged(base)) {
            String faction = base.faction == null ? Faction.NONE.label : base.faction.label;
            String materials = base.constructionMaterialSource == null || base.constructionMaterialSource.isBlank()
                    ? "reserved faction stock" : base.constructionMaterialSource;
            String plan = base.constructionPlanSource == null || base.constructionPlanSource.isBlank()
                    ? "known faction plan" : base.constructionPlanSource;
            lines.add("Facility custody: " + faction + "; "
                    + FactionPhysicalConstructionAuthority.crewReadback(base)
                    + "; constructed from " + materials + " under " + plan
                    + ". Player operation, repair, staffing, and dismantling are unavailable.");
        }
        if (depth >= 1) lines.add("Build read: " + game.safeLabel(base.qualityName, "Common") + " quality.");
        if (depth >= 2) lines.add(game.safeLabel(base.description, "Built base object."));
        if (depth >= 3) lines.add("Integrity/capacity: " + base.integrity + " / " + base.capacity + ".");
        if (depth >= 4) lines.add("Faction/work: " + (base.faction == null ? "none" : base.faction.label) + "; assignment " + game.safeLabel(base.assignedRecipe, "unassigned") + "; worker " + game.safeLabel(base.assignedWorker, "unassigned") + ".");
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

    private static String tileFamilyLabel(char tile) {
        return switch (tile) {
            case '#', 'L', 'X', 'V', 'D', '|', '1', '2', '3', '4', '5', 'I', 'H' -> "Barrier";
            case '/', '+', '=' -> "Access way";
            case '.', ',', ':', ';' -> "Floor";
            case '~' -> "Hazardous surface";
            case 'r' -> "Road";
            case 's' -> "Walkway";
            case 'T', 'b', 'q', 'c', 'u', 'N' -> "Fixture";
            default -> "Area";
        };
    }

    private static String familyLabel(String value) {
        if (value == null || value.isBlank()) return "local surface";
        return readableToken(value);
    }

    private static String readableToken(String value) {
        if (value == null || value.isBlank()) return "unknown";
        String cleaned = value.replace('_', ' ').replace('-', ' ').trim().toLowerCase(Locale.ROOT);
        return PlayerFacingText.sanitize(cleaned);
    }
}
