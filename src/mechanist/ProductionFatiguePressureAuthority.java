package mechanist;

import java.util.List;

/** Resolves current manual-production pressure from the established fatigue readiness bands. */
final class ProductionFatiguePressureAuthority {
    record FatiguePressure(String band, int currentFatigue, int projectedFatigue, int defectRiskAdd,
                           boolean blocked, List<String> lines) { }

    private ProductionFatiguePressureAuthority() { }

    static FatiguePressure evaluate(GamePanel game, int fatigueCost) {
        int current = game == null ? 0 : Math.max(0, game.fatigue);
        int projected = Math.min(GamePanel.MAX_FOOD_WATER, current + Math.max(0, fatigueCost));
        String band;
        int risk;
        boolean blocked = current >= 75;
        if (current >= 75) {
            band = "exhausted";
            risk = 10;
        } else if (current >= 45) {
            band = "tired";
            risk = 5;
        } else if (current >= 20) {
            band = "slightly tired";
            risk = 2;
        } else {
            band = "ready";
            risk = 0;
        }
        return new FatiguePressure(band, current, projected, risk, blocked, List.of(
                "Production fatigue pressure: " + band + " at " + current + " fatigue; projected " + projected + " after this operation.",
                blocked ? "Fatigue gate: manual Craft is blocked at the exhausted band; rest before operating machinery."
                        : risk == 0 ? "Fatigue defect adjustment: none."
                        : "Fatigue defect adjustment: +" + risk + " percentage points."));
    }
}
