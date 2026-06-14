package mechanist;

import java.util.ArrayList;
import java.util.List;

final class Milestone02ExpansionHeatReadabilitySmoke {
    public static void main(String[] args) {
        ArrayList<BaseObject> assets = new ArrayList<>();
        BaseObject shop = new BaseObject("Licensed Shop Counter", 'B', 0, 0, 0, 0);
        shop.businessOpen = true;
        shop.businessHeat = 3;
        assets.add(shop);
        BaseObject turret = new BaseObject("Light Stub Turret", 't', 1, 0, 0, 0);
        turret.faction = Faction.IMPERIAL_GUARD;
        assets.add(turret);
        assets.add(new BaseObject("Fume hood", 'L', 2, 0, 0, 0));

        List<String> lines = ExpansionHeatReadabilityAuthority.summary(7, 13, assets);
        requireContains(lines, "Suspicion: noticeable", "suspicion band");
        requireContains(lines, "Gang attention: high", "gang heat band");
        requireContains(lines, "businesses 1", "business driver");
        requireContains(lines, "defenses 1", "defense driver");
        requireContains(lines, "laboratories/clinics 1", "laboratory driver");
        requireContains(lines, "Recorded business heat: 3", "business heat total");
        requireContains(lines, "not yet automatically added", "simulation boundary");
        for (String line : lines) if (PlayerFacingText.containsLikelyLeak(line)) {
            throw new AssertionError("Expansion heat summary leaked implementation text: " + line);
        }
    }

    private static void requireContains(List<String> lines, String expected, String label) {
        for (String line : lines) if (line != null && line.toLowerCase().contains(expected.toLowerCase())) return;
        throw new AssertionError("Expected " + label + " to contain '" + expected + "': " + lines);
    }

    private Milestone02ExpansionHeatReadabilitySmoke() {}
}
