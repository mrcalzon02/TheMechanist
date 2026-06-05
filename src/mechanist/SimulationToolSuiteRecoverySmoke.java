package mechanist;

import java.util.List;
import java.util.Set;

/** Smoke coverage for the recovered-first development tool-suite registry. */
public final class SimulationToolSuiteRecoverySmoke {
    private SimulationToolSuiteRecoverySmoke() { }

    public static void main(String[] args) {
        Set<String> required = Set.of(
                SimulationToolSuiteRegistry.SECTOR_EDITOR,
                SimulationToolSuiteRegistry.ZONE_EDITOR,
                SimulationToolSuiteRegistry.FACTION_EDITOR,
                SimulationToolSuiteRegistry.QUEST_EDITOR,
                SimulationToolSuiteRegistry.ITEM_EDITOR,
                SimulationToolSuiteRegistry.TILE_EDITOR,
                SimulationToolSuiteRegistry.OBJECT_EDITOR,
                SimulationToolSuiteRegistry.ENTITY_EDITOR,
                SimulationToolSuiteRegistry.INFOPEDIA_EDITOR,
                SimulationToolSuiteRegistry.KNOWLEDGE_EDITOR,
                SimulationToolSuiteRegistry.SKILL_EDITOR
        );
        for (String editor : required) {
            require(SimulationEditorRepository.EDITORS.contains(editor), "missing editor " + editor);
            require(!SimulationToolSuiteRegistry.defaultPropertiesFor(editor).isEmpty(), "missing defaults " + editor);
            require(!SimulationToolSuiteRegistry.seedEntitiesFor(editor).isEmpty(), "missing seed recovery " + editor);
            require(SimulationToolSuiteRegistry.defaultPropertiesFor(editor).containsKey("priority"), "missing priority field " + editor);
            require(SimulationToolSuiteRegistry.defaultPropertiesFor(editor).containsKey("editingBracket"), "missing bracket field " + editor);
            require(SimulationToolSuiteRegistry.defaultPropertiesFor(editor).containsKey("includedInformation"), "missing included-information field " + editor);
            require(SimulationToolSuiteRegistry.defaultPropertiesFor(editor).containsKey("featureCapabilities"), "missing capability field " + editor);
            require(Boolean.TRUE.equals(SimulationToolSuiteRegistry.defaultPropertiesFor(editor).get("externalModCommit")), "missing external mod commit field " + editor);
        }

        SimulationEditorRepository repository = new SimulationEditorRepository();
        require(hasEntity(repository, SimulationToolSuiteRegistry.SECTOR_EDITOR, "sector-hive-core"), "legacy sector seed not recovered");
        require(hasEntity(repository, SimulationToolSuiteRegistry.ITEM_EDITOR, "item-cogitator-core"), "legacy item seed not recovered");
        require(hasEntity(repository, SimulationToolSuiteRegistry.ZONE_EDITOR, "zone-hive-core"), "zone tool seed missing");
        require(hasEntity(repository, SimulationToolSuiteRegistry.FACTION_EDITOR, "faction-arbites"), "legacy faction seed not recovered");
        require(hasEntity(repository, SimulationToolSuiteRegistry.QUEST_EDITOR, "quest-recover-journal"), "legacy quest editor pointer seed not recovered");
        require(hasEntity(repository, SimulationToolSuiteRegistry.TILE_EDITOR, "tile-floor"), "legacy tile editor pointer seed not recovered");
        require(hasEntity(repository, SimulationToolSuiteRegistry.OBJECT_EDITOR, "object-terminal"), "object tool seed missing");
        require(hasEntity(repository, SimulationToolSuiteRegistry.ENTITY_EDITOR, "entity-arbites-patrol"), "entity tool seed missing");
        require(hasEntity(repository, SimulationToolSuiteRegistry.SKILL_EDITOR, "skill-machine-sense"), "skill tool seed missing");
        require(SimulationToolSuiteRegistry.defaultPropertiesFor(SimulationToolSuiteRegistry.SECTOR_EDITOR).containsKey("sectorWidthZones"), "sector editor missing whole-sector width");
        require(SimulationToolSuiteRegistry.defaultPropertiesFor(SimulationToolSuiteRegistry.SECTOR_EDITOR).containsKey("spawnZoneX"), "sector editor missing spawn zone");
        require(SimulationToolSuiteRegistry.defaultPropertiesFor(SimulationToolSuiteRegistry.SECTOR_EDITOR).containsKey("generationWalk"), "sector editor missing generation walk");
        require(SimulationToolSuiteRegistry.defaultPropertiesFor(SimulationToolSuiteRegistry.SECTOR_EDITOR).containsKey("overlaySet"), "sector editor missing overlays");
        require(SimulationToolSuiteRegistry.defaultPropertiesFor(SimulationToolSuiteRegistry.ITEM_EDITOR).containsKey("description"), "item editor missing text description");
        require(SimulationToolSuiteRegistry.defaultPropertiesFor(SimulationToolSuiteRegistry.INFOPEDIA_EDITOR).containsKey("body"), "infopedia editor missing body text");
        require(SimulationToolSuiteRegistry.defaultPropertiesFor(SimulationToolSuiteRegistry.FACTION_EDITOR).containsKey("schemePosture"), "faction editor missing scheme posture");
        require(SimulationToolSuiteRegistry.defaultPropertiesFor(SimulationToolSuiteRegistry.FACTION_EDITOR).containsKey("journalPolicy"), "faction editor missing journal policy");
        require(SimulationToolSuiteRegistry.defaultPropertiesFor(SimulationToolSuiteRegistry.QUEST_EDITOR).containsKey("lifecycleState"), "quest editor missing lifecycle state");
        require(SimulationToolSuiteRegistry.defaultPropertiesFor(SimulationToolSuiteRegistry.QUEST_EDITOR).containsKey("objectiveGuidance"), "quest editor missing objective guidance");
        require(SimulationToolSuiteRegistry.defaultPropertiesFor(SimulationToolSuiteRegistry.QUEST_EDITOR).containsKey("evidenceRule"), "quest editor missing evidence rule");
        require(SimulationToolSuiteRegistry.defaultPropertiesFor(SimulationToolSuiteRegistry.TILE_EDITOR).containsKey("semanticAssetId"), "tile editor missing semantic asset");
        require(SimulationToolSuiteRegistry.defaultPropertiesFor(SimulationToolSuiteRegistry.TILE_EDITOR).containsKey("walkable"), "tile editor missing walkability");
        require(SimulationToolSuiteRegistry.defaultPropertiesFor(SimulationToolSuiteRegistry.OBJECT_EDITOR).containsKey("functionHook"), "object editor missing function hook");
        require(SimulationToolSuiteRegistry.defaultPropertiesFor(SimulationToolSuiteRegistry.OBJECT_EDITOR).containsKey("lightingHook"), "object editor missing lighting hook");

        String audit = SimulationEditorSuite.auditSummary();
        require(audit.contains("recovered-first=true"), "suite audit missing recovery policy: " + audit);
        require(audit.contains("primary-window-integration=true"), "suite audit missing primary window integration: " + audit);
        require(repository.selectedEntities().stream().anyMatch(e -> SimulationToolSuiteRegistry.ZONE_EDITOR.equals(e.editorName())), "zone seed not selected for mod scope");

        List<String> zoneLinks = SimulationToolSuiteRegistry.linkOptionsFor(SimulationToolSuiteRegistry.ZONE_EDITOR, repository).get("sector");
        require(zoneLinks != null && zoneLinks.contains("Hive Core Sector"), "zone editor did not link recovered sectors");
        require(SimulationToolSuiteRegistry.linkOptionsFor(SimulationToolSuiteRegistry.SECTOR_EDITOR, repository).containsKey("overlaySet"), "sector editor missing overlay link");
        require(SimulationToolSuiteRegistry.linkOptionsFor(SimulationToolSuiteRegistry.SECTOR_EDITOR, repository).containsKey("worldgenTuning"), "sector editor missing worldgen tuning link");
        require(SimulationToolSuiteRegistry.linkOptionsFor(SimulationToolSuiteRegistry.FACTION_EDITOR, repository).containsKey("schemePosture"), "faction editor missing scheme posture link");
        require(SimulationToolSuiteRegistry.linkOptionsFor(SimulationToolSuiteRegistry.QUEST_EDITOR, repository).containsKey("lifecycleState"), "quest editor missing lifecycle link");
        require(SimulationToolSuiteRegistry.linkOptionsFor(SimulationToolSuiteRegistry.QUEST_EDITOR, repository).containsKey("evidenceRule"), "quest editor missing evidence link");
        require(SimulationToolSuiteRegistry.linkOptionsFor(SimulationToolSuiteRegistry.TILE_EDITOR, repository).containsKey("family"), "tile editor missing family link");
        require(SimulationToolSuiteRegistry.linkOptionsFor(SimulationToolSuiteRegistry.OBJECT_EDITOR, repository).containsKey("functionHook"), "object editor missing function link");
        List<String> entityLinks = SimulationToolSuiteRegistry.linkOptionsFor(SimulationToolSuiteRegistry.ENTITY_EDITOR, repository).get("spawnZone");
        require(entityLinks != null && entityLinks.contains("Hive Core Zone"), "entity editor did not link recovered zones");
        require(SimulationToolSuiteRegistry.linkOptionsFor(SimulationToolSuiteRegistry.ZONE_EDITOR, repository).containsKey("lightProfile"), "zone editor missing light profile link");
        require(SimulationToolSuiteRegistry.linkOptionsFor(SimulationToolSuiteRegistry.OBJECT_EDITOR, repository).containsKey("lightingHook"), "object editor missing lighting hook link");

        System.out.println("SimulationToolSuiteRecoverySmoke OK editors=" + SimulationEditorRepository.EDITORS.size()
                + " selected=" + repository.selectedEntities().size());
    }

    private static boolean hasEntity(SimulationEditorRepository repository, String editorName, String id) {
        for (SimulationEditorRepository.EditableEntity entity : repository.entities(editorName)) {
            if (id.equals(entity.id())) return true;
        }
        return false;
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }
}
