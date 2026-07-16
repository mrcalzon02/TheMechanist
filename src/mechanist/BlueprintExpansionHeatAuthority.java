package mechanist;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Projects and applies heat and suspicion from player construction. */
final class BlueprintExpansionHeatAuthority {
    record HeatProfile(String blueprintName, String category, String legalLabel,
                       int attentionScore, int heatImpact, int suspicionImpact, String driverSummary) { }

    record AppliedAttention(int heatBefore, int heatAfter, int suspicionBefore, int suspicionAfter,
                            int heatImpact, int suspicionImpact, String driverSummary) {
        String summary() {
            return "Construction attention applied: gang heat " + heatBefore + "->" + heatAfter
                    + " (+" + heatImpact + "), suspicion " + suspicionBefore + "->" + suspicionAfter
                    + " (+" + suspicionImpact + "); drivers " + driverSummary + ".";
        }
    }

    private BlueprintExpansionHeatAuthority() { }

    static List<HeatProfile> profiles() {
        ArrayList<HeatProfile> profiles = new ArrayList<>();
        for (BuildRecipe recipe : BuildRecipe.allBuildRecipes()) {
            if (recipe == null) continue;
            profiles.add(profileFor(recipe));
        }
        return List.copyOf(profiles);
    }

    static HeatProfile profileFor(BuildRecipe recipe) {
        BlueprintAcquisitionPathAuthority.AcquisitionPath path = BlueprintAcquisitionPathAuthority.pathFor(recipe);
        String text = text(recipe.name, recipe.description, path.legalLabel(), path.accessLabel(), path.constructionCategory());
        int attention = Math.max(0, recipe.attention);
        int heat = Math.max(0, attention / 2);
        int suspicion = Math.max(0, attention / 3);
        ArrayList<String> drivers = new ArrayList<>();
        if (isBusinessBlueprint(recipe.symbol) || text.contains("shop") || text.contains("market") || text.contains("counter")) {
            heat += 2;
            suspicion += 1;
            drivers.add("visible commerce");
        }
        if (containsAny(text, "defense", "turret", "barricade", "guard", "security", "armory", "wall", "door")) {
            heat += 3;
            suspicion += 2;
            drivers.add("armed defenses");
        }
        if (MachineTierAuthority.isMachineOrFacilitySymbol(recipe.symbol) || containsAny(text, "machine", "forge", "production", "logistics", "supply")) {
            heat += 2;
            suspicion += 1;
            drivers.add("industrial footprint");
        }
        if (containsAny(text, "laboratory", "lab", "chemical", "fume", "medicae", "clinic", "injector")) {
            heat += 2;
            suspicion += 2;
            drivers.add("laboratory or clinic");
        }
        if (containsAny(path.legalLabel(), "restricted", "military", "black-market", "stolen", "noble", "license", "permit")) {
            heat += 2;
            suspicion += 3;
            drivers.add("access or legality risk");
        }
        if (recipe.requiredFaction != null && FactionInventoryStockAuthority.normalizeFaction(recipe.requiredFaction) != Faction.NONE) {
            heat += 1;
            suspicion += 2;
            drivers.add("faction-visible asset");
        }
        if (drivers.isEmpty()) drivers.add("ordinary footprint");
        return new HeatProfile(path.blueprintName(), path.constructionCategory(), path.legalLabel(), attention,
                heat, suspicion, String.join(", ", drivers));
    }

    static AppliedAttention applyConstructionStart(GamePanel game, BuildRecipe recipe) {
        HeatProfile profile = profileFor(recipe);
        int heatBefore = game == null ? 0 : Math.max(0, game.gangHeat);
        int suspicionBefore = game == null ? 0 : Math.max(0, game.suspicion);
        int heatAfter = heatBefore + profile.heatImpact();
        int suspicionAfter = suspicionBefore + profile.suspicionImpact();
        if (game != null) {
            game.gangHeat = heatAfter;
            game.suspicion = suspicionAfter;
        }
        return new AppliedAttention(heatBefore, heatAfter, suspicionBefore, suspicionAfter,
                profile.heatImpact(), profile.suspicionImpact(), profile.driverSummary());
    }

    static List<String> definitionAuditLines() {
        List<HeatProfile> profiles = profiles();
        int heatBearing = 0;
        int restricted = 0;
        int commerce = 0;
        int defenses = 0;
        int laboratories = 0;
        int production = 0;
        int maxHeat = 0;
        int maxSuspicion = 0;
        for (HeatProfile profile : profiles) {
            if (profile.heatImpact > 0 || profile.suspicionImpact > 0) heatBearing++;
            if (profile.driverSummary.contains("access or legality risk")) restricted++;
            if (profile.driverSummary.contains("visible commerce")) commerce++;
            if (profile.driverSummary.contains("armed defenses")) defenses++;
            if (profile.driverSummary.contains("laboratory or clinic")) laboratories++;
            if (profile.driverSummary.contains("industrial footprint")) production++;
            maxHeat = Math.max(maxHeat, profile.heatImpact);
            maxSuspicion = Math.max(maxSuspicion, profile.suspicionImpact);
        }
        return List.of(
                "Blueprint expansion heat audit: owner=BlueprintExpansionHeatAuthority, blueprintOwner=BuildRecipe, acquisitionOwner=BlueprintAcquisitionPathAuthority, heatReadabilityOwner=ExpansionHeatReadabilityAuthority, ordinaryUiRawIds=false.",
                "Blueprint heat catalog audit: blueprintProfiles=" + profiles.size()
                        + ", heatBearingProfiles=" + heatBearing
                        + ", restrictedOrPermitProfiles=" + restricted
                        + ", commerceProfiles=" + commerce
                        + ", defenseProfiles=" + defenses
                        + ", laboratoryOrClinicProfiles=" + laboratories
                        + ", productionFootprintProfiles=" + production
                        + ", maxHeatImpact=" + maxHeat
                        + ", maxSuspicionImpact=" + maxSuspicion + ".",
                "Blueprint heat driver audit: projected drivers include visible commerce, armed defenses, industrial footprint, laboratory or clinic footprint, access or legality risk, and faction-visible assets.",
                "Blueprint heat band audit: projected heat and suspicion use the existing quiet, low, noticeable, high, and critical readability bands from ExpansionHeatReadabilityAuthority.",
                "Blueprint heat sample audit: " + sampleLine("Licensed Shop Counter")
                        + " | " + sampleLine("Security Sensor Mast")
                        + " | " + sampleLine("Fume hood") + ".",
                "Blueprint heat execution audit: a successful player construction start adds the previewed heat and suspicion once; blocked placement adds none, while law response and faction reactions remain later owners.",
                "Guard: Milestone03BlueprintExpansionHeatAuditSmoke checks blueprint heat coverage, driver categories, sample impacts, readability bands, future-owner boundaries, and raw-ID hiding."
        );
    }

    private static String sampleLine(String recipeName) {
        for (HeatProfile profile : profiles()) {
            if (profile.blueprintName.equalsIgnoreCase(recipeName)) {
                return profile.blueprintName + " heat+" + profile.heatImpact
                        + " suspicion+" + profile.suspicionImpact + " drivers=" + profile.driverSummary;
            }
        }
        return recipeName + " heat profile missing";
    }

    private static boolean containsAny(String text, String... needles) {
        if (text == null) return false;
        for (String needle : needles) if (needle != null && text.contains(needle)) return true;
        return false;
    }

    private static boolean isBusinessBlueprint(char symbol) {
        return "wueflsBM".indexOf(symbol) >= 0;
    }

    private static String text(String... values) {
        StringBuilder out = new StringBuilder();
        if (values != null) for (String value : values) if (value != null) out.append(value).append(' ');
        return out.toString().toLowerCase(Locale.ROOT);
    }
}
