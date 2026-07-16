package mechanist;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Focused Phase 18 smoke for construction-parity inspection and Economy Editor snapshots. */
final class Milestone05ConstructionParityEditorSmoke {
    private static final String RECORD_CLASS = "ConstructionParityInspection";

    public static void main(String[] args) {
        System.setProperty("java.awt.headless", "true");
        verifyInspectionCatalog();
        verifyFilterBehavior();

        GamePanel game = new GamePanel();
        if (game.timer != null) game.timer.stop();
        try {
            game.world = new World(18005L, 32, 32);
            game.turn = 818;
            game.worldTurn = 818L;
            verifyEconomyEditorSnapshots(game);
            verifyInGameEconomyEditor(game);
        } finally {
            game.shutdownRuntime();
        }
        System.out.println("Milestone 05 construction parity editor smoke passed.");
    }

    private static void verifyInspectionCatalog() {
        List<BuildRecipe> recipes = BuildRecipe.allBuildRecipes();
        List<ConstructionParityInspectionAuthority.RecipeInspection> inspections =
                ConstructionParityInspectionAuthority.inspectAll();
        require(!recipes.isEmpty(), "construction recipe catalog should not be empty");
        require(inspections.size() == recipes.size(),
                "inspection coverage should equal the complete recipe catalog: recipes="
                        + recipes.size() + " inspections=" + inspections.size());

        LinkedHashMap<String, BuildRecipe> recipesByName = new LinkedHashMap<>();
        for (BuildRecipe recipe : recipes) {
            require(recipe != null && !blank(recipe.name), "recipe catalog contains a blank recipe");
            require(recipesByName.put(recipe.name, recipe) == null,
                    "recipe names must be unique for one editor record per recipe: " + recipe.name);
            require(!blank(ConstructionBlueprintOwnershipAuthority.blueprintId(recipe)),
                    "recipe has no stable construction blueprint mapping: " + recipe.name);
        }

        Set<String> inspectedNames = new LinkedHashSet<>();
        boolean conditionalSeen = false;
        for (ConstructionParityInspectionAuthority.RecipeInspection inspection : inspections) {
            require(inspection != null, "inspection catalog contains a null record");
            require(inspectedNames.add(inspection.recipeName()),
                    "inspection catalog duplicates recipe " + inspection.recipeName());
            BuildRecipe recipe = recipesByName.get(inspection.recipeName());
            require(recipe != null, "inspection has no BuildRecipe owner: " + inspection.recipeName());
            require(inspection.valid(), "inspection should be complete and valid: " + inspection);
            require(inspection.blueprintMappingValid(),
                    "inspection reports an invalid blueprint mapping: " + inspection.recipeName());
            requireNoBlankFields(inspection);
            require(expectedBlueprintName(recipe).equals(inspection.blueprintName()),
                    "inspection blueprint name diverged from ownership authority for " + recipe.name);

            String expectedException = expectedExceptionClass(
                    inspection.playerCapability(), inspection.factionCapability());
            require(expectedException.equals(inspection.exceptionClass()),
                    "capability exception must remain explicit for " + inspection.recipeName()
                            + ": expected=" + expectedException + " actual=" + inspection.exceptionClass());
            require(!"INVALID".equals(inspection.exceptionClass()),
                    "valid recipes must not be hidden behind INVALID parity entries");
            require(inspection.exceptionReason().toLowerCase(Locale.ROOT).contains("player")
                            && inspection.exceptionReason().toLowerCase(Locale.ROOT).contains("faction"),
                    "parity reason must explain both player and faction paths for "
                            + inspection.recipeName() + ": " + inspection.exceptionReason());
            if (inspection.playerCapability() == ConstructionParityInspectionAuthority.Capability.CONDITIONAL
                    || inspection.factionCapability() == ConstructionParityInspectionAuthority.Capability.CONDITIONAL
                    || "CONDITIONAL".equals(inspection.exceptionClass())) conditionalSeen = true;
        }
        require(inspectedNames.equals(recipesByName.keySet()),
                "inspection names should exactly cover BuildRecipe.allBuildRecipes()");
        require(conditionalSeen, "inspection catalog should preserve conditional capability coverage");

        ConstructionParityInspectionAuthority.RecipeInspection forge =
                requireInspection(inspections, BuildRecipe.microForge().name);
        verifyMicroForgeInspection(forge);
        verifyPublicStorageInspection(requireInspection(inspections, BuildRecipe.storage().name));

        List<String> audit = ConstructionParityInspectionAuthority.auditLines();
        requireContains(audit, "recipes=" + recipes.size(), "audit recipe total");
        requireContains(audit, "valid=" + recipes.size(), "audit valid total");
        requireContains(audit, "invalidMappings=0", "audit invalid mapping total");
        requireContains(audit, "conditionalOrUnsupported=", "audit conditional coverage");
        requireContains(audit, "never upgrades them to symmetric by assumption",
                "audit parity honesty rule");
    }

    private static void verifyFilterBehavior() {
        int expected = BuildRecipe.allBuildRecipes().size();
        require(ConstructionParityInspectionAuthority.editorRows(null).size() == expected,
                "blank editor filter should expose every construction recipe");
        require(ConstructionParityInspectionAuthority.editorRows("   ").size() == expected,
                "whitespace editor filter should expose every construction recipe");

        List<String> forge = ConstructionParityInspectionAuthority.editorRows("  eMm MiCrO fOrGe  ");
        require(forge.size() == 1, "case-insensitive recipe filter should isolate Micro Forge: " + forge);
        requireContains(forge, "EMM Micro Forge | Machines and Utilities",
                "filtered Micro Forge row");
        requireContains(forge, "player=conditional", "filtered player capability");
        requireContains(forge, "factions=supported", "filtered faction capability");
        requireContains(forge, "parity=CONDITIONAL", "filtered parity exception");

        List<String> conditional = ConstructionParityInspectionAuthority.editorRows("conditional");
        require(!conditional.isEmpty()
                        && !conditional.get(0).startsWith("No construction parity entries"),
                "capability filters should find explicit conditional records");

        List<String> noMatch = ConstructionParityInspectionAuthority.editorRows(
                "phase-18-no-such-construction-recipe");
        require(noMatch.equals(List.of("No construction parity entries match the active filter.")),
                "empty filter results should be explicit: " + noMatch);
    }

    private static void verifyEconomyEditorSnapshots(GamePanel game) {
        List<ConstructionParityInspectionAuthority.RecipeInspection> inspections =
                ConstructionParityInspectionAuthority.inspectAll();
        Map<String, ConstructionParityInspectionAuthority.RecipeInspection> expected =
                inspectionsByName(inspections);

        List<SimulationEditorRepository.EditableEntity> snapshot =
                SimulationRuntimeEditorBridgeAuthority.snapshot(
                        game, SimulationToolSuiteRegistry.ECONOMY_EDITOR);
        List<SimulationEditorRepository.EditableEntity> parity = parityRecords(snapshot);
        require(parity.size() == expected.size(),
                "Economy Editor snapshot should contain one construction parity record per recipe: "
                        + parity.size() + " vs " + expected.size());
        verifyUniqueSnapshotRecords(parity, expected, game.worldTurn);
        require(countByClass(snapshot, "ConstructionParityAudit") == 1,
                "Economy Editor should include one construction parity audit summary");

        SimulationEditorRepository.EditableEntity forge = requireRuntimeRecipe(parity,
                BuildRecipe.microForge().name);
        verifyMicroForgeProperties(forge.properties());

        SimulationEditorRepository repository = new SimulationEditorRepository();
        SimulationRuntimeEditorBridgeAuthority.RefreshResult first =
                SimulationRuntimeEditorBridgeAuthority.refresh(
                        game, repository, SimulationToolSuiteRegistry.ECONOMY_EDITOR);
        require(first.supported(), "Economy Editor should support live refresh");
        require(first.records() == snapshot.size(),
                "refresh count should match the complete Economy Editor snapshot");
        requireContains(first.message(), "Refreshed " + first.records()
                        + " economy editor records", "Economy Editor refresh count readback");
        requireContains(first.message(), (expected.size() + 1)
                        + " catalog/definition", "Economy Editor static catalog count");
        requireContains(first.message(), "live-world",
                "Economy Editor live/static refresh boundary");
        List<SimulationEditorRepository.EditableEntity> firstRuntime = runtimeRecords(repository);
        require(firstRuntime.size() == snapshot.size(),
                "repository should contain exactly the refreshed runtime snapshot");
        verifyUniqueSnapshotRecords(parityRecords(firstRuntime), expected, game.worldTurn);

        SimulationEditorRepository.EditableEntity editableForge = requireRuntimeRecipe(
                parityRecords(firstRuntime), BuildRecipe.microForge().name);
        String forgeId = editableForge.id();
        int ownedBefore = game.unlockedConstructionBlueprints.size();
        repository.setProperty(new SimulationEditorRepository.EntityRef(
                        SimulationToolSuiteRegistry.ECONOMY_EDITOR, forgeId),
                "blueprintName", "locally edited snapshot only");
        repository.setProperty(new SimulationEditorRepository.EntityRef(
                        SimulationToolSuiteRegistry.ECONOMY_EDITOR, forgeId),
                "playerCapability", "SUPPORTED");
        ConstructionParityInspectionAuthority.RecipeInspection liveForge =
                ConstructionParityInspectionAuthority.inspect(BuildRecipe.microForge());
        require("EMM Micro Forge licensed blueprint".equals(liveForge.blueprintName())
                        && liveForge.playerCapability()
                        == ConstructionParityInspectionAuthority.Capability.CONDITIONAL,
                "editing an Economy Editor snapshot must not mutate the BuildRecipe parity source");
        require(game.unlockedConstructionBlueprints.size() == ownedBefore,
                "editing an Economy Editor snapshot must not grant blueprint ownership");

        SimulationRuntimeEditorBridgeAuthority.RefreshResult second =
                SimulationRuntimeEditorBridgeAuthority.refresh(
                        game, repository, SimulationToolSuiteRegistry.ECONOMY_EDITOR);
        List<SimulationEditorRepository.EditableEntity> secondRuntime = runtimeRecords(repository);
        require(second.records() == first.records(),
                "repeat refresh should report a stable runtime record count");
        require(secondRuntime.size() == firstRuntime.size(),
                "repeat refresh should replace runtime rows instead of duplicating them");
        require(parityRecords(secondRuntime).size() == expected.size(),
                "repeat refresh should retain one parity row per recipe");
        require(uniqueIds(secondRuntime).size() == secondRuntime.size(),
                "repeat refresh should leave unique runtime entity ids");
        SimulationEditorRepository.EditableEntity refreshedForge = requireRuntimeRecipe(
                parityRecords(secondRuntime), BuildRecipe.microForge().name);
        require("EMM Micro Forge licensed blueprint".equals(
                        refreshedForge.properties().get("blueprintName"))
                        && "CONDITIONAL".equals(
                        refreshedForge.properties().get("playerCapability")),
                "repeat refresh should replace local snapshot edits with current runtime truth");
    }

    private static void verifyInGameEconomyEditor(GamePanel game) {
        game.openInGameEditor(SimulationToolSuiteRegistry.ECONOMY_EDITOR);
        require(game.screen == GamePanel.Screen.EDITOR,
                "construction parity records should be reachable through the in-game Economy Editor");
        require(game.inGameEditorStatus.contains("Refreshed ")
                        && game.inGameEditorStatus.contains("economy editor records")
                        && game.inGameEditorStatus.contains("catalog/definition"),
                "opening the Economy Editor should distinguish live and static records");
        game.setSize(1280, 820);
        BufferedImage image = new BufferedImage(1280, 820, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        game.paintComponent(graphics);
        graphics.dispose();
        require(game.buttons.stream().anyMatch(button -> button != null
                        && "Refresh Live World".equals(button.label)),
                "in-game Economy Editor should expose Refresh Live World for parity records");
    }

    private static void verifyUniqueSnapshotRecords(
            List<SimulationEditorRepository.EditableEntity> records,
            Map<String, ConstructionParityInspectionAuthority.RecipeInspection> expected,
            long worldTurn) {
        Set<String> ids = new LinkedHashSet<>();
        Set<String> recipeNames = new LinkedHashSet<>();
        for (SimulationEditorRepository.EditableEntity entity : records) {
            require(entity != null && ids.add(entity.id()),
                    "construction parity runtime ids must be unique: "
                            + (entity == null ? "null" : entity.id()));
            Map<String, Object> props = entity.properties();
            require(Boolean.TRUE.equals(props.get("runtimeSnapshot")),
                    "construction parity editor row must be a runtime snapshot");
            require(Boolean.FALSE.equals(props.get("liveWorldDependent"))
                            && text(props, "sourceMode").contains("not live ownership"),
                    "construction parity rows must identify their catalog-only truth boundary");
            require(RECORD_CLASS.equals(props.get("recordClass")),
                    "construction parity editor row has the wrong recordClass");
            require(Long.valueOf(worldTurn).equals(props.get("snapshotWorldTurn")),
                    "construction parity editor row has the wrong world turn");
            String recipeName = text(props, "recipeName");
            require(recipeNames.add(recipeName),
                    "Economy Editor duplicates construction recipe " + recipeName);
            ConstructionParityInspectionAuthority.RecipeInspection inspection = expected.get(recipeName);
            require(inspection != null, "Economy Editor emitted an unknown construction recipe " + recipeName);
            requirePropertiesMatch(props, inspection);
        }
        require(recipeNames.equals(expected.keySet()),
                "Economy Editor construction rows should exactly cover the inspection catalog");
    }

    private static void requirePropertiesMatch(
            Map<String, Object> props,
            ConstructionParityInspectionAuthority.RecipeInspection inspection) {
        require(inspection.recipeName().equals(props.get("recipeName")), "recipeName snapshot mismatch");
        require(inspection.category().equals(props.get("category")), "category snapshot mismatch");
        require(inspection.playerCapability().name().equals(props.get("playerCapability")),
                "playerCapability snapshot mismatch for " + inspection.recipeName());
        require(inspection.factionCapability().name().equals(props.get("factionCapability")),
                "factionCapability snapshot mismatch for " + inspection.recipeName());
        require(inspection.blueprintName().equals(props.get("blueprintName")),
                "blueprintName snapshot mismatch for " + inspection.recipeName());
        require(Boolean.valueOf(inspection.blueprintMappingValid()).equals(props.get("blueprintMappingValid")),
                "blueprintMappingValid snapshot mismatch for " + inspection.recipeName());
        require(inspection.issuingFaction().equals(props.get("issuingFaction")),
                "issuingFaction snapshot mismatch for " + inspection.recipeName());
        require(inspection.vendorCategory().equals(props.get("vendorCategory")),
                "vendorCategory snapshot mismatch for " + inspection.recipeName());
        require(inspection.acquisitionPath().equals(props.get("acquisitionPath")),
                "acquisitionPath snapshot mismatch for " + inspection.recipeName());
        require(inspection.accessGate().equals(props.get("accessGate")),
                "accessGate snapshot mismatch for " + inspection.recipeName());
        require(inspection.legalClass().equals(props.get("legalClass")),
                "legalClass snapshot mismatch for " + inspection.recipeName());
        require(inspection.materialSummary().equals(props.get("materialSummary")),
                "materialSummary snapshot mismatch for " + inspection.recipeName());
        require(inspection.workforceSummary().equals(props.get("workforceSummary")),
                "workforceSummary snapshot mismatch for " + inspection.recipeName());
        require(inspection.exceptionClass().equals(props.get("exceptionClass")),
                "exceptionClass snapshot mismatch for " + inspection.recipeName());
        require(inspection.exceptionReason().equals(props.get("exceptionReason")),
                "exceptionReason snapshot mismatch for " + inspection.recipeName());
    }

    private static void verifyMicroForgeInspection(
            ConstructionParityInspectionAuthority.RecipeInspection forge) {
        require("EMM Micro Forge".equals(forge.recipeName()), "Micro Forge recipe name changed");
        require("Machines and Utilities".equals(forge.category()), "Micro Forge category changed");
        require(forge.playerCapability() == ConstructionParityInspectionAuthority.Capability.CONDITIONAL,
                "Micro Forge player path should remain conditional on licensed live access gates");
        require(forge.factionCapability() == ConstructionParityInspectionAuthority.Capability.SUPPORTED,
                "Micro Forge faction path should expose live physical construction support");
        require("EMM Micro Forge licensed blueprint".equals(forge.blueprintName()),
                "Micro Forge licensed blueprint mapping changed");
        require(forge.blueprintMappingValid(), "Micro Forge blueprint mapping should be valid");
        require("Mechanist Collegia".equals(forge.issuingFaction()), "Micro Forge issuing faction changed");
        require("industrial-blueprints".equals(forge.vendorCategory()),
                "Micro Forge vendor category changed");
        require("earn standing, buy from Mechanist Collegia vendor after license check, receive as contract reward, or salvage/research a comparable plan"
                        .equals(forge.acquisitionPath()),
                "Micro Forge acquisition paths changed: " + forge.acquisitionPath());
        require("faction-approved blueprint for Mechanist Collegia, forge license-bound, quality Common, knowledge gate: Scrap-Forging Doctrine, workbench required"
                        .equals(forge.accessGate()),
                "Micro Forge access gates changed: " + forge.accessGate());
        require("forge license-bound".equals(forge.legalClass()), "Micro Forge legal class changed");
        require("4 construction supplies, 3 machine parts, 1 Gear train, 1 Bearing set, 1 Motor coil pack, 1 Ceramic insulator blank, 1 Heat sink"
                        .equals(forge.materialSummary()),
                "Micro Forge material summary changed: " + forge.materialSummary());
        require("player labor uses staged construction work; faction labor uses assigned room workers; workbench operation applies"
                        .equals(forge.workforceSummary()),
                "Micro Forge workforce summary changed: " + forge.workforceSummary());
        require("CONDITIONAL".equals(forge.exceptionClass()),
                "Micro Forge must not be mislabeled symmetric while player access remains conditional");
        require("player requires the licensed blueprint and live access gates; faction capability is supported through the live physical construction authority; knowledge gate Scrap-Forging Doctrine; workbench required"
                        .equals(forge.exceptionReason()),
                "Micro Forge exception reason changed: " + forge.exceptionReason());
    }

    private static void verifyPublicStorageInspection(
            ConstructionParityInspectionAuthority.RecipeInspection storage) {
        require("Storage Crate".equals(storage.recipeName()), "Storage recipe name changed");
        require(storage.playerCapability() == ConstructionParityInspectionAuthority.Capability.SUPPORTED,
                "public Storage Crate should remain player-supported");
        require(storage.factionCapability() == ConstructionParityInspectionAuthority.Capability.CONDITIONAL,
                "Storage Crate faction construction should remain explicitly conditional");
        require("Storage Crate public construction plan".equals(storage.blueprintName()),
                "Storage Crate should expose its public plan mapping");
        require("public construction catalog".equals(storage.issuingFaction())
                        && "not applicable - public catalog".equals(storage.vendorCategory()),
                "Storage Crate should expose catalog availability without inventing a vendor");
        require("public civilian".equals(storage.legalClass()),
                "Storage Crate should remain public civilian construction");
        require(storage.acquisitionPath().contains("available by default")
                        && storage.acquisitionPath().contains("no separate plan unlock"),
                "Storage Crate should expose its automatic public-plan availability");
        require("CONDITIONAL".equals(storage.exceptionClass()),
                "Storage Crate faction gap should remain an explicit conditional exception");
    }

    private static void verifyMicroForgeProperties(Map<String, Object> props) {
        require("EMM Micro Forge".equals(props.get("recipeName")), "Micro Forge editor recipe changed");
        require("Machines and Utilities".equals(props.get("category")), "Micro Forge editor category changed");
        require("CONDITIONAL".equals(props.get("playerCapability")),
                "Micro Forge editor should expose conditional player capability");
        require("SUPPORTED".equals(props.get("factionCapability")),
                "Micro Forge editor should expose supported faction capability");
        require(Boolean.TRUE.equals(props.get("blueprintMappingValid")),
                "Micro Forge editor should expose a valid blueprint mapping");
        require("Mechanist Collegia".equals(props.get("issuingFaction")),
                "Micro Forge editor should expose its issuing faction");
        require("industrial-blueprints".equals(props.get("vendorCategory")),
                "Micro Forge editor should expose its live vendor category");
        require("forge license-bound".equals(props.get("legalClass")),
                "Micro Forge editor should expose its legal class");
        require("CONDITIONAL".equals(props.get("exceptionClass")),
                "Micro Forge editor must preserve its explicit conditional exception");
        require(text(props, "exceptionReason").contains("live physical construction authority"),
                "Micro Forge editor should name the live faction construction path");
    }

    private static void requireNoBlankFields(
            ConstructionParityInspectionAuthority.RecipeInspection inspection) {
        require(!blank(inspection.recipeName()), "blank recipeName");
        require(!blank(inspection.category()), "blank category for " + inspection.recipeName());
        require(inspection.playerCapability() != null,
                "blank player capability for " + inspection.recipeName());
        require(inspection.factionCapability() != null,
                "blank faction capability for " + inspection.recipeName());
        require(!blank(inspection.blueprintName()), "blank blueprint name for " + inspection.recipeName());
        require(!blank(inspection.issuingFaction()), "blank issuing faction for " + inspection.recipeName());
        require(!blank(inspection.vendorCategory()), "blank vendor category for " + inspection.recipeName());
        require(!blank(inspection.acquisitionPath()), "blank acquisition path for " + inspection.recipeName());
        require(!blank(inspection.accessGate()), "blank access gate for " + inspection.recipeName());
        require(!blank(inspection.legalClass()), "blank legal class for " + inspection.recipeName());
        require(!blank(inspection.materialSummary()), "blank material summary for " + inspection.recipeName());
        require(!blank(inspection.workforceSummary()), "blank workforce summary for " + inspection.recipeName());
        require(!blank(inspection.exceptionClass()), "blank exception class for " + inspection.recipeName());
        require(!blank(inspection.exceptionReason()), "blank exception reason for " + inspection.recipeName());
    }

    private static String expectedBlueprintName(BuildRecipe recipe) {
        return ConstructionBlueprintOwnershipAuthority.requiresLicensedBlueprint(recipe)
                ? ConstructionBlueprintOwnershipAuthority.blueprintItemName(recipe)
                : recipe.name + " public construction plan";
    }

    private static String expectedExceptionClass(
            ConstructionParityInspectionAuthority.Capability player,
            ConstructionParityInspectionAuthority.Capability faction) {
        if (player == ConstructionParityInspectionAuthority.Capability.NOT_SUPPORTED
                && faction == ConstructionParityInspectionAuthority.Capability.NOT_SUPPORTED) return "UNSUPPORTED";
        if (player == ConstructionParityInspectionAuthority.Capability.NOT_SUPPORTED) return "FACTION_ONLY";
        if (faction == ConstructionParityInspectionAuthority.Capability.NOT_SUPPORTED) return "PLAYER_ONLY";
        if (player == ConstructionParityInspectionAuthority.Capability.SUPPORTED
                && faction == ConstructionParityInspectionAuthority.Capability.SUPPORTED) return "SYMMETRIC";
        return "CONDITIONAL";
    }

    private static ConstructionParityInspectionAuthority.RecipeInspection requireInspection(
            List<ConstructionParityInspectionAuthority.RecipeInspection> inspections, String recipeName) {
        for (ConstructionParityInspectionAuthority.RecipeInspection inspection : inspections) {
            if (inspection != null && recipeName.equals(inspection.recipeName())) return inspection;
        }
        throw new AssertionError("missing construction parity inspection for " + recipeName);
    }

    private static Map<String, ConstructionParityInspectionAuthority.RecipeInspection> inspectionsByName(
            List<ConstructionParityInspectionAuthority.RecipeInspection> inspections) {
        LinkedHashMap<String, ConstructionParityInspectionAuthority.RecipeInspection> out = new LinkedHashMap<>();
        for (ConstructionParityInspectionAuthority.RecipeInspection inspection : inspections) {
            require(out.put(inspection.recipeName(), inspection) == null,
                    "duplicate expected construction inspection " + inspection.recipeName());
        }
        return out;
    }

    private static List<SimulationEditorRepository.EditableEntity> parityRecords(
            List<SimulationEditorRepository.EditableEntity> entities) {
        return entities.stream()
                .filter(entity -> entity != null && RECORD_CLASS.equals(entity.properties().get("recordClass")))
                .toList();
    }

    private static List<SimulationEditorRepository.EditableEntity> runtimeRecords(
            SimulationEditorRepository repository) {
        return repository.entities(SimulationToolSuiteRegistry.ECONOMY_EDITOR).stream()
                .filter(entity -> entity != null
                        && Boolean.TRUE.equals(entity.properties().get("runtimeSnapshot")))
                .toList();
    }

    private static SimulationEditorRepository.EditableEntity requireRuntimeRecipe(
            List<SimulationEditorRepository.EditableEntity> entities, String recipeName) {
        for (SimulationEditorRepository.EditableEntity entity : entities) {
            if (recipeName.equals(entity.properties().get("recipeName"))) return entity;
        }
        throw new AssertionError("missing Economy Editor construction row for " + recipeName);
    }

    private static int countByClass(List<SimulationEditorRepository.EditableEntity> entities,
                                    String recordClass) {
        int count = 0;
        for (SimulationEditorRepository.EditableEntity entity : entities) {
            if (entity != null && recordClass.equals(entity.properties().get("recordClass"))) count++;
        }
        return count;
    }

    private static Set<String> uniqueIds(List<SimulationEditorRepository.EditableEntity> entities) {
        LinkedHashSet<String> ids = new LinkedHashSet<>();
        for (SimulationEditorRepository.EditableEntity entity : entities) {
            if (entity != null) ids.add(entity.id());
        }
        return ids;
    }

    private static String text(Map<String, Object> values, String key) {
        Object value = values.get(key);
        return value == null ? "" : String.valueOf(value);
    }

    private static boolean blank(String text) {
        return text == null || text.isBlank();
    }

    private static void requireContains(List<String> lines, String expected, String label) {
        require(lines != null && lines.toString().contains(expected),
                label + " missing '" + expected + "': " + lines);
    }

    private static void requireContains(String text, String expected, String label) {
        require(text != null && text.contains(expected),
                label + " missing '" + expected + "': " + text);
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private Milestone05ConstructionParityEditorSmoke() { }
}
