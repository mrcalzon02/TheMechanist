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
    static final List<String> EDITORS = SimulationToolSuiteRegistry.editorNames();

    private final Map<String, List<EditableEntity>> entitiesByEditor = new LinkedHashMap<>();
    private final Set<EntityRef> selectedForModScope = new LinkedHashSet<>();

    private final String generatedPackageId = randomId();
    private String modName = "Mechanist Local Mod " + generatedPackageId;
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
        String editor = SimulationToolSuiteRegistry.isKnownEditor(editorName) ? editorName : SimulationToolSuiteRegistry.fallbackEditor();
        String slug = slug(editor.replace(" Editor", ""));
        String id = randomId();
        EditableEntity entity = entity(slug + "-" + id, "New " + editor.replace(" Editor", "") + " " + id, defaultPropertiesFor(editor));
        addEntity(editor, entity);
        return entity.copy();
    }

    synchronized void addEntity(String editorName, EditableEntity entity) {
        if (entity == null) return;
        String editor = SimulationToolSuiteRegistry.isKnownEditor(editorName) ? editorName : SimulationToolSuiteRegistry.fallbackEditor();
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
        return SimulationToolSuiteRegistry.defaultPropertiesFor(editorName);
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
        return "editors=" + entitiesByEditor.size() + " entities=" + total + " selected=" + selectedForModScope.size() + " mod=" + modName + " packageId=" + generatedPackageId + " version=" + modVersion;
    }

    private EditableEntity internalEntity(EntityRef ref) {
        if (ref == null) return null;
        for (EditableEntity e : entitiesByEditor.getOrDefault(ref.editorName(), List.of())) if (e.id().equals(ref.entityId())) return e;
        return null;
    }

    private void seedDefaults() {
        for (String editor : EDITORS) {
            entitiesByEditor.put(editor, new ArrayList<>(SimulationToolSuiteRegistry.seedEntitiesFor(editor)));
        }
        selectedForModScope.addAll(SimulationToolSuiteRegistry.defaultSelectedRefs());
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

    static String randomId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase(Locale.ROOT);
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
