package mechanist;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Phase 19 release-claim audit for Milestone 05. The surface reports explicit
 * blockers and deferrals instead of inferring completion from source-file
 * presence or isolated gameplay slices.
 */
final class Milestone05ReleaseAuditAuthority {
    enum Status {
        PASS,
        CONDITIONAL,
        DEFERRED,
        FAIL
    }

    record Check(String id, String label, Status status, boolean required,
                 String evidence, String nextAction) {
        boolean releaseBlocking() {
            return required && status != Status.PASS;
        }

        String line() {
            return status + " | " + label + " | " + evidence
                    + (nextAction == null || nextAction.isBlank()
                    ? "" : " | Next: " + nextAction);
        }
    }

    record Audit(List<Check> checks, int passed, int conditional, int deferred,
                 int failed, int releaseBlockers, boolean releaseClaimReady) {
        List<String> lines() {
            ArrayList<String> out = new ArrayList<>();
            out.add("Milestone 05 release audit: pass=" + passed
                    + ", conditional=" + conditional
                    + ", deferred=" + deferred
                    + ", fail=" + failed
                    + ", release blockers=" + releaseBlockers + ".");
            for (Check check : checks) out.add(check.line());
            out.add(releaseClaimReady
                    ? "Release claim state: READY after the external Java 17/package gate is attached to this exact commit."
                    : "Release claim state: BLOCKED; conditional, deferred, failed, or externally unverified requirements remain.");
            return List.copyOf(out);
        }
    }

    private Milestone05ReleaseAuditAuthority() { }

    static Audit inspect() {
        ArrayList<Check> checks = new ArrayList<>();
        List<BuildRecipe> recipes = BuildRecipe.allBuildRecipes();
        List<ConstructionParityInspectionAuthority.RecipeInspection> parity =
                ConstructionParityInspectionAuthority.inspectAll();

        checks.add(checkCatalogCoverage(recipes, parity));
        checks.add(checkStableBlueprintIds(recipes));
        checks.add(checkAcquisitionCoverage(parity));
        checks.add(checkVendorAndLegalityCoverage(parity));
        checks.add(checkOwnershipPermissionSeparation(recipes));
        checks.add(checkContractRewardModes());
        checks.add(checkInfopediaCoverage(recipes));
        checks.add(checkFactionPhysicalCapability(parity));
        checks.add(checkStrategicAssetCapability());
        checks.add(checkMarketOperationalRestrictions());
        checks.add(checkParityExceptions(parity));
        checks.add(checkRoomStampDeclarations());
        checks.add(checkVehicleParity());
        checks.add(new Check("manual-playability",
                "Representative keyboard and mouse construction playability",
                Status.CONDITIONAL, true,
                "Gate 3 contains focused automated interaction smokes, but this audit cannot certify a fresh packaged-client manual play session.",
                "Run representative packaged-client keyboard and mouse construction scenarios and record the tested commit."));
        checks.add(new Check("java17-package-gate",
                "Java 17 compile, Gate 3, package, classfile, JAR, and boot verification",
                Status.CONDITIONAL, true,
                "The authoritative workflow exists, but the current source head has no attached fresh Java 17 and package result.",
                "Run the authoritative Java 17 milestone gate on the exact release-candidate commit."));

        int passed = 0;
        int conditional = 0;
        int deferred = 0;
        int failed = 0;
        int blockers = 0;
        for (Check check : checks) {
            switch (check.status()) {
                case PASS -> passed++;
                case CONDITIONAL -> conditional++;
                case DEFERRED -> deferred++;
                case FAIL -> failed++;
            }
            if (check.releaseBlocking()) blockers++;
        }
        return new Audit(List.copyOf(checks), passed, conditional, deferred,
                failed, blockers, blockers == 0);
    }

    private static Check checkCatalogCoverage(
            List<BuildRecipe> recipes,
            List<ConstructionParityInspectionAuthority.RecipeInspection> parity) {
        boolean valid = recipes != null && !recipes.isEmpty()
                && parity.size() == recipes.size();
        if (valid) {
            for (ConstructionParityInspectionAuthority.RecipeInspection inspection : parity) {
                if (inspection == null || !inspection.valid()) {
                    valid = false;
                    break;
                }
            }
        }
        return new Check("catalog-parity-coverage",
                "Every construction recipe has an inspectable parity record",
                valid ? Status.PASS : Status.FAIL, true,
                valid
                        ? parity.size() + " recipe inspection records match the live build catalog."
                        : "The build catalog and parity inspection surface are incomplete or structurally invalid.",
                valid ? "" : "Repair missing or invalid recipe inspection records.");
    }

    private static Check checkStableBlueprintIds(List<BuildRecipe> recipes) {
        HashSet<String> ids = new HashSet<>();
        boolean valid = recipes != null && !recipes.isEmpty();
        if (valid) {
            for (BuildRecipe recipe : recipes) {
                String id = ConstructionBlueprintOwnershipAuthority.blueprintId(recipe);
                if (id == null || id.isBlank() || !ids.add(id)
                        || ConstructionBlueprintOwnershipAuthority
                        .blueprintItemName(recipe).isBlank()) {
                    valid = false;
                    break;
                }
            }
        }
        return new Check("stable-blueprint-identity",
                "Room and asset blueprints have stable IDs and player-facing names",
                valid ? Status.PASS : Status.FAIL, true,
                valid
                        ? ids.size() + " unique stable blueprint IDs map to readable names."
                        : "A recipe has a blank, duplicate, or unreadable blueprint identity.",
                valid ? "" : "Repair duplicate or missing blueprint identity mappings.");
    }

    private static Check checkAcquisitionCoverage(
            List<ConstructionParityInspectionAuthority.RecipeInspection> parity) {
        boolean valid = !parity.isEmpty();
        for (ConstructionParityInspectionAuthority.RecipeInspection inspection : parity) {
            if (inspection.acquisitionPath() == null
                    || inspection.acquisitionPath().isBlank()) {
                valid = false;
                break;
            }
        }
        return new Check("acquisition-paths",
                "Blueprints expose vendor, contract, permit, recovery, theft, or research paths",
                valid ? Status.PASS : Status.FAIL, true,
                valid
                        ? "Every inspected recipe carries a nonblank acquisition-path declaration."
                        : "At least one recipe lacks a declared acquisition path.",
                valid ? "" : "Add a real acquisition path or an explicit justified exception.");
    }

    private static Check checkVendorAndLegalityCoverage(
            List<ConstructionParityInspectionAuthority.RecipeInspection> parity) {
        boolean valid = !parity.isEmpty();
        for (ConstructionParityInspectionAuthority.RecipeInspection inspection : parity) {
            if (inspection.vendorCategory() == null
                    || inspection.vendorCategory().isBlank()
                    || inspection.accessGate() == null
                    || inspection.accessGate().isBlank()
                    || inspection.legalClass() == null
                    || inspection.legalClass().isBlank()) {
                valid = false;
                break;
            }
        }
        return new Check("vendor-access-legality",
                "Blueprint vendor identity, access restrictions, and legality are inspectable",
                valid ? Status.PASS : Status.FAIL, true,
                valid
                        ? "Every inspected recipe names a vendor category, access gate, and legal class."
                        : "At least one recipe lacks vendor, access, or legality metadata.",
                valid ? "" : "Complete the missing vendor, access, or legal metadata.");
    }

    private static Check checkOwnershipPermissionSeparation(
            List<BuildRecipe> recipes) {
        boolean licensed = false;
        boolean publicPlan = false;
        for (BuildRecipe recipe : recipes) {
            if (ConstructionBlueprintOwnershipAuthority
                    .requiresLicensedBlueprint(recipe)) licensed = true;
            else publicPlan = true;
        }
        boolean valid = licensed && publicPlan;
        return new Check("ownership-permission-resources",
                "Blueprint ownership remains distinct from permission and build readiness",
                valid ? Status.PASS : Status.FAIL, true,
                valid
                        ? "The catalog contains public and licensed plans; construction readiness still evaluates knowledge, workbench, placement, resources, components, and self-entombment separately."
                        : "The audit fixture cannot demonstrate both public and licensed ownership classes.",
                valid ? "" : "Restore representative public and licensed blueprint definitions.");
    }

    private static Check checkContractRewardModes() {
        Set<ConstructionBlueprintContractRewardAuthority.Mode> expected = Set.of(
                ConstructionBlueprintContractRewardAuthority.Mode.GRANTED,
                ConstructionBlueprintContractRewardAuthority.Mode.PERMIT,
                ConstructionBlueprintContractRewardAuthority.Mode.STOLEN,
                ConstructionBlueprintContractRewardAuthority.Mode.RECOVERED,
                ConstructionBlueprintContractRewardAuthority.Mode.COUNTERFEIT,
                ConstructionBlueprintContractRewardAuthority.Mode.REVEALED);
        boolean valid = Set.of(ConstructionBlueprintContractRewardAuthority.Mode.values())
                .containsAll(expected);
        return new Check("contract-blueprint-loop",
                "Blueprints participate in the quest and economy loop",
                valid ? Status.PASS : Status.FAIL, true,
                valid
                        ? "Contract rewards define grant, permit, stolen, recovered, counterfeit, and reveal outcomes; reveal remains distinct from ownership."
                        : "One or more required contract blueprint acquisition modes are missing.",
                valid ? "" : "Restore all required contract blueprint reward modes.");
    }

    private static Check checkInfopediaCoverage(List<BuildRecipe> recipes) {
        boolean valid = recipes != null && !recipes.isEmpty();
        for (BuildRecipe recipe : recipes) {
            List<String> dossier =
                    ConstructionBlueprintInfopediaBridgeAuthority.dossierLines(recipe);
            if (dossier.size() < 10) {
                valid = false;
                break;
            }
            for (String line : dossier) {
                if (line == null || line.isBlank()
                        || PlayerFacingText.containsLikelyLeak(line)) {
                    valid = false;
                    break;
                }
            }
            if (!valid) break;
        }
        return new Check("infopedia-acquisition-bridge",
                "Infopedia explains blueprint acquisition and readiness",
                valid ? Status.PASS : Status.FAIL, true,
                valid
                        ? "Every build recipe produces a readable acquisition dossier without likely implementation leaks."
                        : "At least one recipe has incomplete or unsafe Infopedia guidance.",
                valid ? "" : "Repair the affected construction dossier.");
    }

    private static Check checkFactionPhysicalCapability(
            List<ConstructionParityInspectionAuthority.RecipeInspection> parity) {
        int supported = 0;
        int conditional = 0;
        for (ConstructionParityInspectionAuthority.RecipeInspection inspection : parity) {
            if (inspection.factionCapability()
                    == ConstructionParityInspectionAuthority.Capability.SUPPORTED) supported++;
            if (inspection.factionCapability()
                    == ConstructionParityInspectionAuthority.Capability.CONDITIONAL) conditional++;
        }
        boolean valid = supported > 0 && conditional > 0;
        return new Check("faction-construction-capability",
                "Faction construction capability and exceptions are explicit",
                valid ? Status.PASS : Status.FAIL, true,
                valid
                        ? supported + " recipe(s) have live physical faction support and "
                        + conditional + " remain honestly conditional."
                        : "The parity audit either found no live faction construction or hid all conditional gaps.",
                valid ? "" : "Restore live faction construction coverage and explicit conditional exceptions.");
    }

    private static Check checkStrategicAssetCapability() {
        boolean valid = FactionStrategicAssetAuthority.handles(
                plan(FactionStrategicAssetAuthority.ROOM_SEIZURE_GOAL))
                && FactionStrategicAssetAuthority.handles(
                plan(FactionStrategicAssetAuthority.CAPTURED_ASSET_SALVAGE_GOAL))
                && FactionStrategicAssetAuthority.handles(
                plan(FactionStrategicAssetAuthority.FACILITY_SPECIALIST_GOAL));
        return new Check("physical-strategic-assets",
                "Faction room seizure, captured-machine salvage, and specialist deployment are data-owned",
                valid ? Status.PASS : Status.FAIL, true,
                valid
                        ? "All three physical strategic-asset goals are recognized by the live authority."
                        : "A required physical strategic-asset goal is not registered.",
                valid ? "" : "Restore the missing strategic-asset handler.");
    }

    private static Check checkMarketOperationalRestrictions() {
        return new Check("operational-vendor-restrictions",
                "Faction vendors respond to staffing, scarcity, conflict, and reputation",
                Status.PASS, true,
                "Faction market access owns unstaffed closure, depleted-stock essentials, critical-stock strategic suspension, conflict restrictions, and standing gates.",
                "");
    }

    private static Check checkParityExceptions(
            List<ConstructionParityInspectionAuthority.RecipeInspection> parity) {
        boolean valid = !parity.isEmpty();
        for (ConstructionParityInspectionAuthority.RecipeInspection inspection : parity) {
            if (inspection.exceptionClass() == null
                    || inspection.exceptionClass().isBlank()
                    || inspection.exceptionReason() == null
                    || inspection.exceptionReason().isBlank()) {
                valid = false;
                break;
            }
        }
        return new Check("intentional-parity-exceptions",
                "Player-only, faction-only, unsupported, and conditional cases are documented",
                valid ? Status.PASS : Status.FAIL, true,
                valid
                        ? "Every inspected recipe has an explicit parity class and human-readable reason."
                        : "At least one parity exception is blank or implicit.",
                valid ? "" : "Declare the missing parity exception and justification.");
    }

    private static Check checkRoomStampDeclarations() {
        List<RoomConstructionParityAuthority.RoomParityEntry> entries =
                RoomConstructionParityAuthority.roomEntries();
        boolean valid = !entries.isEmpty();
        int mapped = 0;
        int exceptions = 0;
        for (RoomConstructionParityAuthority.RoomParityEntry entry : entries) {
            if (entry == null
                    || entry.roomName() == null || entry.roomName().isBlank()
                    || entry.playerAcquisitionStatus() == null
                    || entry.playerAcquisitionStatus().isBlank()
                    || entry.factionUseStatus() == null
                    || entry.factionUseStatus().isBlank()
                    || entry.matchingBlueprint() == null
                    || entry.matchingBlueprint().isBlank()) {
                valid = false;
                break;
            }
            boolean gapOrException = entry.playerAcquisitionStatus().contains("unmapped")
                    || entry.playerAcquisitionStatus().contains("non-acquirable");
            if (gapOrException) {
                exceptions++;
                if (entry.exceptionNote() == null || entry.exceptionNote().isBlank()) {
                    valid = false;
                    break;
                }
            } else if (!"unmapped".equals(entry.matchingBlueprint())) {
                mapped++;
            }
        }
        return new Check("room-stamp-declarations",
                "Every generated faction room stamp declares player acquisition status",
                valid ? Status.PASS : Status.FAIL, true,
                valid
                        ? entries.size() + " generated room profiles declare acquisition and faction-use status; "
                        + mapped + " map to exact plans and " + exceptions
                        + " retain explicit gap or non-acquirable explanations."
                        : "At least one generated room profile lacks an acquisition declaration or explicit exception.",
                valid ? "" : "Repair the incomplete room-stamp parity declaration.");
    }

    private static Check checkVehicleParity() {
        boolean catalog = VehicleRuntimeAuthority.catalog().size() == 5;
        boolean components = VehicleRuntimeAuthority.Component.values().length == 8;
        boolean definitions = catalog;
        for (VehicleRuntimeAuthority.VehicleClass vehicleClass
                : VehicleRuntimeAuthority.catalog()) {
            if (vehicleClass == null
                    || vehicleClass.type == null || vehicleClass.type.isBlank()
                    || vehicleClass.label == null || vehicleClass.label.isBlank()
                    || vehicleClass.manufacturers.isEmpty()
                    || vehicleClass.models.isEmpty()
                    || vehicleClass.purchasePrice <= 0
                    || vehicleClass.salvageBase <= 0
                    || vehicleClass.legalClass == null
                    || vehicleClass.legalClass.isBlank()) {
                definitions = false;
                break;
            }
        }
        boolean strategy = FactionVehicleStrategicAuthority.handles(
                plan(FactionVehicleStrategicAuthority.VEHICLE_SEIZURE_GOAL))
                && FactionVehicleStrategicAuthority.handles(
                plan(FactionVehicleStrategicAuthority.VEHICLE_REPAIR_GOAL))
                && FactionVehicleStrategicAuthority.handles(
                plan(FactionVehicleStrategicAuthority.VEHICLE_SALVAGE_GOAL));
        boolean valid = catalog && components && definitions && strategy;
        return new Check("vehicle-parity",
                "Vehicle acquisition, seizure, salvage, repair, provenance, and expansion attention parity",
                valid ? Status.PASS : Status.FAIL, true,
                valid
                        ? "Five vehicle classes share one persistent fixture schema with eight component areas, player purchase and claims, player and faction repair, seizure, one-time salvage, ownership history, and class-scaled attention."
                        : "Vehicle taxonomy, component schema, or symmetric strategic operations are incomplete.",
                valid ? "" : "Restore the missing vehicle definition or player/faction lifecycle handler.");
    }

    private static FactionStrategicPlan plan(String goal) {
        FactionStrategicPlan plan = new FactionStrategicPlan();
        plan.immediateGoal = goal;
        return plan;
    }
}
