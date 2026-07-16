package mechanist;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class ExpansionHeatReadabilityAuthority {
    private ExpansionHeatReadabilityAuthority() {}

    static List<String> summary(GamePanel game) {
        ArrayList<String> lines = new ArrayList<>(summary(game == null ? 0 : game.suspicion,
                game == null ? 0 : game.gangHeat, game == null ? List.of() : game.baseObjects));
        lines.add(ConstructionExpansionReactionAuthority.statusLine(game));
        return lines;
    }

    static List<String> summary(int suspicion, int gangHeat, List<BaseObject> assets) {
        int businesses = 0;
        int defenses = 0;
        int production = 0;
        int laboratories = 0;
        int restricted = 0;
        int recordedBusinessHeat = 0;
        if (assets != null) for (BaseObject asset : assets) {
            if (asset == null) continue;
            String text = (safe(asset.name, "") + " " + safe(asset.description, "")).toLowerCase(Locale.ROOT);
            if (asset.isBusinessAsset() && (asset.businessOpen || asset.symbol == 'B' || asset.symbol == 'M')) businesses++;
            if (isDefense(asset, text)) defenses++;
            if (MachineTierAuthority.isMachineOrFacilitySymbol(asset.symbol)) production++;
            if (isLaboratory(asset, text)) laboratories++;
            if (isRestricted(asset, text)) restricted++;
            recordedBusinessHeat += Math.max(0, asset.businessHeat);
        }

        ArrayList<String> lines = new ArrayList<>();
        lines.add("Suspicion: " + pressureBand(suspicion) + " (" + Math.max(0, suspicion) + ").");
        lines.add("Gang attention: " + pressureBand(gangHeat) + " (" + Math.max(0, gangHeat) + ").");
        lines.add("Expansion exposure: businesses " + businesses + ", defenses " + defenses + ", production assets "
                + production + ", laboratories/clinics " + laboratories + ", restricted or military assets " + restricted + ".");
        lines.add("Recorded business heat: " + recordedBusinessHeat + ".");
        if (businesses + defenses + production + laboratories + restricted == 0) {
            lines.add("Attention drivers: no obvious faction-scale assets are currently recorded.");
        } else {
            lines.add("Attention drivers: visible commerce, armed defenses, industrial capacity, laboratories, and restricted facilities may attract scrutiny as authority expands.");
        }
        lines.add("Construction rule: starting a player construction site adds its previewed exposure to the global suspicion and gang-attention meters once.");
        lines.add("Relief paths: rest, entertainment, and reconciliation services can reduce tracked pressure where their interaction supports it.");
        return lines;
    }

    static String pressureBand(int value) {
        if (value >= 20) return "critical";
        if (value >= 12) return "high";
        if (value >= 6) return "noticeable";
        if (value > 0) return "low";
        return "quiet";
    }

    private static boolean isDefense(BaseObject asset, String text) {
        return DefenseSemanticIntegration.isDefenseRecipe(asset.name)
                || containsAny(text, "turret", "barricade", "watch post", "guard", "razor wire", "defense", "shield relay");
    }

    private static boolean isLaboratory(BaseObject asset, String text) {
        return asset.symbol == 'l' || asset.symbol == 'L' || asset.symbol == 'M'
                || containsAny(text, "laboratory", "chemical", "reagent", "distillation", "fume hood", "medicae", "clinic", "injector");
    }

    private static boolean isRestricted(BaseObject asset, String text) {
        return asset.faction != null && asset.faction != Faction.NONE && asset.faction != Faction.HIVER
                || containsAny(text, "armory", "military", "arbites", "security", "illegal", "contraband", "ritual", "interrogation");
    }

    private static boolean containsAny(String text, String... needles) {
        for (String needle : needles) if (text.contains(needle)) return true;
        return false;
    }

    private static String safe(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
