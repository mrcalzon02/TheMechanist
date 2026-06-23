package mechanist;

import java.util.List;
import java.util.Map;

/** Smoke for audit-only faction construction material reservation contracts. */
final class Milestone03BlueprintFactionConstructionMaterialReservationAuditSmoke {
    public static void main(String[] args) {
        List<BlueprintFactionConstructionMaterialReservationAuthority.MaterialReservation> samples =
                BlueprintFactionConstructionMaterialReservationAuthority.sampleReservations();
        List<String> audit = BlueprintFactionConstructionMaterialReservationAuthority.definitionAuditLines();

        require(samples.size() == 3, "expected three sample material reservations");
        requireContains(audit, "owner=BlueprintFactionConstructionMaterialReservationAuthority", "reservation owner");
        requireContains(audit, "jobOwner=BlueprintFactionConstructionJobDefinitionAuthority", "job owner");
        requireContains(audit, "recipeOwner=BuildRecipe", "recipe owner");
        requireContains(audit, "stagedConstructionOwner=ProgressiveConstructionAuthority", "staged construction owner");
        requireContains(audit, "readiness=audit-only", "audit-only boundary");
        requireContains(audit, "ordinaryUiRawIds=false", "raw-ID boundary");
        requireContains(audit, "Construction supplies, Machine parts, and named component costs", "requirement families");
        requireContains(audit, "sampleReservations=3", "sample count");
        requireContains(audit, "ready=1", "ready count");
        requireContains(audit, "blocked=2", "blocked count");
        requireContains(audit, "namedComponentSamples=3", "named component samples");
        requireContains(audit, "job-storage-public material reservation for Storage Crate", "storage sample");
        requireContains(audit, "reservedPreview=Construction supplies x1, Rivet set x1", "ready reservation preview");
        requireContains(audit, "job-sensor-restricted material reservation for Security Sensor Mast", "sensor sample");
        requireContains(audit, "missing Machine parts x2, Circuit wafer x1, Wire bundle x1, Power coupling socket x1", "sensor blockers");
        requireContains(audit, "job-shop-public material reservation for Licensed Shop Counter", "shop sample");
        requireContains(audit, "missing Construction supplies x2, Machine parts x1, Fastener button card x1", "shop blockers");
        requireContains(audit, "reserve materials once", "single reservation rule");
        requireContains(audit, "separate from staged-site inserted materials", "ledger separation rule");
        requireContains(audit, "does not remove supplies, remove machine parts, remove named components", "non-consumption boundary");
        requireContains(audit, "write reservation rows, stage materials into a site, assign crew", "non-mutation boundary");
        requireContains(audit, "Milestone03BlueprintFactionConstructionMaterialReservationAuditSmoke", "guard reference");

        BlueprintFactionConstructionMaterialReservationAuthority.MaterialReservation direct =
                BlueprintFactionConstructionMaterialReservationAuthority.reservationFor("job-direct", Faction.MECHANIST_COLLEGIA,
                        BuildRecipe.microForge(), Map.of("Construction supplies", 4, "Machine parts", 3, "Gear train", 1,
                                "Bearing set", 1, "Motor coil pack", 1, "Ceramic insulator blank", 1, "Heat sink", 1));
        require(direct.reservationReady(), "direct micro forge reservation should be ready");
        requireContains(direct.requiredLedger(), "Construction supplies x4", "direct supplies");
        requireContains(direct.requiredLedger(), "Machine parts x3", "direct parts");
        requireContains(direct.requiredLedger(), "Gear train x1", "direct named component");
        requireContains(direct.reservedLedger(), "Heat sink x1", "direct reserved named component");
        require("none".equals(direct.missingLedger()), "direct reservation should have no missing materials");
        requireContains(direct.boundaryLine(), "audit only, no inventory mutation", "direct boundary");

        for (BlueprintFactionConstructionMaterialReservationAuthority.MaterialReservation sample : samples) {
            requireNotBlank(sample.jobId(), "job id");
            requireNotBlank(sample.blueprintName(), "blueprint name");
            requireNotBlank(sample.factionName(), "faction name");
            requireNotBlank(sample.requiredLedger(), "required ledger");
            requireNotBlank(sample.reservedLedger(), "reserved ledger");
            requireNotBlank(sample.missingLedger(), "missing ledger");
            requireNotBlank(sample.blockerLine(), "blocker line");
            requireNotBlank(sample.boundaryLine(), "boundary line");
        }
        for (String line : audit) {
            if (PlayerFacingText.containsLikelyLeak(line)) {
                throw new AssertionError("Blueprint faction construction material reservation audit leaked implementation text: " + line);
            }
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static void requireNotBlank(String value, String label) {
        require(value != null && !value.isBlank(), "expected nonblank " + label);
    }

    private static void requireContains(String text, String expected, String label) {
        if (text != null && text.contains(expected)) return;
        throw new AssertionError("Expected " + label + " to contain '" + expected + "': " + text);
    }

    private static void requireContains(List<String> lines, String expected, String label) {
        for (String line : lines) if (line != null && line.contains(expected)) return;
        throw new AssertionError("Expected " + label + " to contain '" + expected + "': " + lines);
    }

    private Milestone03BlueprintFactionConstructionMaterialReservationAuditSmoke() { }
}
