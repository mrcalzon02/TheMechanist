package mechanist;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/** In-memory editor repository; Swing views mutate through command objects rather than direct simulation state. */
final class SimulationEditorRepository {
    static final List<String> EDITORS = List.of("Sector Editor", "Room Editor", "Faction Editor", "Item Editor", "Knowledge Editor", "Infopedia Editor");

    private final Map<String, List<EditableEntity>> entitiesByEditor = new LinkedHashMap<>();
    private final Set<EntityRef> selectedForModScope = new LinkedHashSet<>();

    private String modName = "Mechanist Local Mod";
    private String modVersion = "0.1.0";
    private String modAuthor = System.getProperty("user.name", "local-author");
    private String modDescription = "A local Mechanist editor export.";
    private String modTags = "simulation,editor,mechanist";
    private String modDependencies = "base>=0.9.10";
    private long steamAppId = 0L;
    private long publishedFileId = 0L;

    SimulationEditorRepository() { seedDefaults(); }

    synchronized List<EditableEntity> entities(String editorName) {
        List<EditableEntity> source = entitiesByEditor.getOrDefault(editorName, List.of());
        ArrayList<EditableEntity> copy = new ArrayList<>();
        for (EditableEntity e : source) copy.add(e.copy());
        return copy;
    }

    synchronized EditableEntity entity(EntityRef ref) {
        if (ref == null) return null;
        for (EditableEntity e : entitiesByEditor.getOrDefault(ref.editorName(), List.of())) {
            if (e.id().equals(ref.entityId())) return e.copy();
        }
        return null;
    }

    synchronized Object property(EntityRef ref, String propertyName) {
        EditableEntity entity = internalEntity(ref);
        return entity == null ? null : entity.properties().get(propertyName);
    }

    synchronized void setProperty(EntityRef ref, String propertyName, Object value) {
        EditableEntity entity = internalEntity(ref);
        if (entity != null && propertyName != null) entity.properties().put(propertyName, normalizeValue(value));
    }

    synchronized EditableEntity createBlankEntity(String editorName) {
        String editor = EDITORS.contains(editorName) ? editorName : EDITORS.get(0);
        int next = entitiesByEditor.getOrDefault(editor, List.of()).size() + 1;
        String slug = slug(editor.replace(" Editor", ""));
        EditableEntity entity = entity(slug + "-new-" + next, "New " + editor.replace(" Editor", "") + " " + next, defaultPropertiesFor(editor));
        addEntity(editor, entity);
        return entity.copy();
    }

    synchronized void addEntity(String editorName, EditableEntity entity) {
        if (entity == null) return;
        String editor = EDITORS.contains(editorName) ? editorName : EDITORS.get(0);
        ArrayList<EditableEntity> list = (ArrayList<EditableEntity>)entitiesByEditor.computeIfAbsent(editor, ignored -> new ArrayList<>());
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).id().equals(entity.id())) {
                list.set(i, entity.copy());
                return;
            }
        }
        list.add(entity.copy());
    }

    synchronized void removeEntity(EntityRef ref) {
        if (ref == null) return;
        List<EditableEntity> list = entitiesByEditor.get(ref.editorName());
        if (list == null) return;
        list.removeIf(e -> e.id().equals(ref.entityId()));
        selectedForModScope.remove(ref);
    }

    private static LinkedHashMap<String, Object> defaultPropertiesFor(String editorName) {
        return switch (editorName) {
            case "Sector Editor" -> props("x", 0, "y", 0, "z", 0, "hazard", "None", "factionControl", "Unassigned", "radiation", 0, "piracy", 0);
            case "Room Editor" -> props("width", 8, "height", 6, "oxygenSeal", true, "securityTerminal", "None", "placementNodes", 0, "floorMaterial", "worn-plasteel", "layoutCells", "");
            case "Faction Editor" -> props("lawful", 5, "mercantile", 5, "technocratic", 5, "resources", 100, "aggression", 3, "culture", "Order / Deterrence");
            case "Item Editor" -> props("techTier", 1, "massKg", 1.0, "components", "steel, wire", "durability", 50, "decayRate", 0.01, "isUnlocked", false);
            case "Knowledge Editor" -> props("parent", "root", "breakthroughCost", 1, "blueprint", "none", "unlocks", "new doctrine", "tier", 1, "isUnlocked", false);
            case "Infopedia Editor" -> props("category", "World", "body", "New entry body.", "tags", "new", "revision", 1);
            default -> props("name", "New Entry", "isUnlocked", false);
        };
    }

    synchronized boolean selected(EntityRef ref) { return selectedForModScope.contains(ref); }

    synchronized void setSelected(EntityRef ref, boolean selected) {
        if (ref == null) return;
        if (selected) selectedForModScope.add(ref);
        else selectedForModScope.remove(ref);
    }

    synchronized List<ScopedEntity> selectedEntities() {
        ArrayList<ScopedEntity> out = new ArrayList<>();
        for (EntityRef ref : selectedForModScope) {
            EditableEntity e = internalEntity(ref);
            if (e != null) out.add(new ScopedEntity(ref.editorName(), e.id(), e.name(), new LinkedHashMap<>(e.properties())));
        }
        return Collections.unmodifiableList(out);
    }

    synchronized List<ScopedEntity> allScopedEntities() {
        ArrayList<ScopedEntity> out = new ArrayList<>();
        for (String editor : EDITORS) {
            for (EditableEntity e : entitiesByEditor.getOrDefault(editor, List.of())) {
                out.add(new ScopedEntity(editor, e.id(), e.name(), new LinkedHashMap<>(e.properties())));
            }
        }
        return Collections.unmodifiableList(out);
    }

    synchronized List<EntityRef> allEntityRefs() {
        ArrayList<EntityRef> out = new ArrayList<>();
        for (String editor : EDITORS) for (EditableEntity e : entitiesByEditor.getOrDefault(editor, List.of())) out.add(new EntityRef(editor, e.id()));
        return out;
    }

    synchronized void updateModMetadata(String name, String version, String author, String description, String tags, String dependencies, long appId, long fileId) {
        modName = sane(name, "Mechanist Local Mod");
        modVersion = sane(version, "0.1.0");
        modAuthor = sane(author, System.getProperty("user.name", "local-author"));
        modDescription = sane(description, "A local Mechanist editor export.");
        modTags = sane(tags, "simulation,editor,mechanist");
        modDependencies = sane(dependencies, "base>=0.9.10");
        steamAppId = Math.max(0L, appId);
        publishedFileId = Math.max(0L, fileId);
    }

    synchronized ModMetadata metadata() {
        return new ModMetadata(modName, modVersion, modAuthor, modDescription, csv(modTags), csv(modDependencies), steamAppId, publishedFileId);
    }

    synchronized String auditLine() {
        int total = 0;
        for (List<EditableEntity> list : entitiesByEditor.values()) total += list.size();
        return "editors=" + entitiesByEditor.size() + " entities=" + total + " selected=" + selectedForModScope.size() + " mod=" + modName + " version=" + modVersion;
    }

    private EditableEntity internalEntity(EntityRef ref) {
        if (ref == null) return null;
        for (EditableEntity e : entitiesByEditor.getOrDefault(ref.editorName(), List.of())) if (e.id().equals(ref.entityId())) return e;
        return null;
    }

    private void seedDefaults() {
        entitiesByEditor.put("Sector Editor", new ArrayList<>(List.of(
                entity("sector-hive-core", "Hive Core Sector", props("x", 0, "y", 0, "z", 12, "hazard", "Industrial Smog", "factionControl", "Adeptus Administratum", "radiation", 2, "piracy", 3)),
                entity("sector-ash-wastes", "Ash Wastes Spur", props("x", -4, "y", 7, "z", 1, "hazard", "Ash Storms", "factionControl", "Scavenger Clans", "radiation", 4, "piracy", 7)),
                entity("sector-dock-ring", "Orbital Dock Ring", props("x", 2, "y", -3, "z", 35, "hazard", "Vacuum Breach", "factionControl", "Guild Factors", "radiation", 1, "piracy", 5))
        )));
        entitiesByEditor.put("Room Editor", new ArrayList<>(List.of(
                entity("room-hab-cell", "Hab Cell Block", props("width", 4, "height", 4, "oxygenSeal", true, "securityTerminal", "None", "placementNodes", 3, "floorMaterial", "worn-plasteel")),
                entity("room-generator", "Utility Generator Room", props("width", 8, "height", 6, "oxygenSeal", true, "securityTerminal", "Local Panel", "placementNodes", 9, "floorMaterial", "oil-stained iron")),
                entity("room-chapel", "Cult Imperialis Chapel", props("width", 12, "height", 8, "oxygenSeal", false, "securityTerminal", "Reliquary Seal", "placementNodes", 11, "floorMaterial", "stone and brass"))
        )));
        entitiesByEditor.put("Faction Editor", new ArrayList<>(List.of(
                entity("faction-arbites", "Adeptus Arbites", props("lawful", 9, "mercantile", 2, "technocratic", 4, "resources", 650, "aggression", 6, "culture", "Order / Deterrence")),
                entity("faction-guild", "Dock Guild", props("lawful", 4, "mercantile", 9, "technocratic", 6, "resources", 820, "aggression", 3, "culture", "Contracts / Leverage")),
                entity("faction-scav", "Scavenger Clans", props("lawful", 2, "mercantile", 5, "technocratic", 3, "resources", 210, "aggression", 7, "culture", "Survival / Salvage"))
        )));
        entitiesByEditor.put("Item Editor", new ArrayList<>(List.of(
                entity("item-stubcarbine", "Stubcarbine", props("techTier", 2, "massKg", 3.4, "components", "steel, springs, firing pin", "durability", 85, "decayRate", 0.04)),
                entity("item-sealed-ration", "Sealed Ration", props("techTier", 1, "massKg", 0.45, "components", "nutrient brick, wax paper", "durability", 60, "decayRate", 0.01)),
                entity("item-cogitator-core", "Cogitator Core", props("techTier", 6, "massKg", 18.0, "components", "copper, crystal matrix, logic stack", "durability", 92, "decayRate", 0.02))
        )));
        entitiesByEditor.put("Knowledge Editor", new ArrayList<>(List.of(
                entity("knowledge-basic-underhive", "Underhive Basics", props("parent", "root", "breakthroughCost", 0, "blueprint", "none", "unlocks", "movement, scavenge, inspection", "tier", 0)),
                entity("knowledge-machinery", "Applied Machinery", props("parent", "Underhive Basics", "breakthroughCost", 45, "blueprint", "micro-assembler", "unlocks", "machine inspection, repair queue", "tier", 2)),
                entity("knowledge-void-safety", "Void Safety Protocols", props("parent", "Applied Machinery", "breakthroughCost", 90, "blueprint", "sealed hatch", "unlocks", "vacuum hazard mitigation", "tier", 4))
        )));
        entitiesByEditor.put("Infopedia Editor", new ArrayList<>(List.of(
                entity("info-sector-taxonomy", "Sector Taxonomy", props("category", "World", "body", "Classification rules for hive sectors, spurs, transit layers, and void-facing infrastructure.", "tags", "sector,world,hive", "revision", 1)),
                entity("info-faction-index", "Faction Index", props("category", "Factions", "body", "A cross-reference for lawful, mercantile, technocratic, insurgent, and devotional entities.", "tags", "faction,politics,economy", "revision", 1)),
                entity("info-lore-crawl", "Historical Crawl", props("category", "Lore", "body", "A long-form historical text node for sector provenance and grim industrial memory.", "tags", "history,lore,provenance", "revision", 1))
        )));
        selectedForModScope.add(new EntityRef("Sector Editor", "sector-hive-core"));
        selectedForModScope.add(new EntityRef("Room Editor", "room-generator"));
        selectedForModScope.add(new EntityRef("Item Editor", "item-cogitator-core"));
    }

    private static EditableEntity entity(String id, String name, LinkedHashMap<String, Object> props) { return new EditableEntity(id, name, props); }

    private static LinkedHashMap<String, Object> props(Object... pairs) {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        for (int i = 0; i + 1 < pairs.length; i += 2) out.put(String.valueOf(pairs[i]), normalizeValue(pairs[i + 1]));
        return out;
    }

    private static Object normalizeValue(Object value) {
        if (value instanceof String s) return s.trim();
        return value;
    }

    static String sane(String text, String fallback) {
        String clean = text == null ? "" : text.trim();
        return clean.isEmpty() ? fallback : clean;
    }

    static List<String> csv(String text) {
        ArrayList<String> out = new ArrayList<>();
        if (text != null) {
            for (String part : text.split(",")) {
                String clean = part.trim();
                if (!clean.isEmpty()) out.add(clean);
            }
        }
        return Collections.unmodifiableList(out);
    }

    static String slug(String value) {
        String raw = sane(value, "mechanist-mod").toLowerCase(Locale.ROOT);
        String slug = raw.replaceAll("[^a-z0-9._-]+", "-").replaceAll("-+", "-");
        slug = slug.replaceAll("^-|-$", "");
        return slug.isBlank() ? "mechanist-mod-" + UUID.randomUUID() : slug;
    }

    record EntityRef(String editorName, String entityId) { }
    record ScopedEntity(String editorName, String id, String name, Map<String, Object> properties) { }
    record ModMetadata(String name, String version, String author, String description, List<String> tags, List<String> dependencies, long steamAppId, long publishedFileId) { }

    static final class EditableEntity {
        private final String id;
        private final String name;
        private final LinkedHashMap<String, Object> properties;

        EditableEntity(String id, String name, LinkedHashMap<String, Object> properties) {
            this.id = sane(id, UUID.randomUUID().toString());
            this.name = sane(name, this.id);
            this.properties = properties == null ? new LinkedHashMap<>() : properties;
        }

        String id() { return id; }
        String name() { return name; }
        LinkedHashMap<String, Object> properties() { return properties; }
        EditableEntity copy() { return new EditableEntity(id, name, new LinkedHashMap<>(properties)); }
    }
}
