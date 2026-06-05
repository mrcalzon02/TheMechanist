package mechanist;

import mechanist.assets.AssetMetadata;
import mechanist.assets.AssetRegistry;
import mechanist.assets.AssetType;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Game-owned Infopedia bridge for the Stage 2 Semantic Asset Browser.
 *
 * This class deliberately exposes registry contents to the existing in-game
 * Infopedia surface rather than creating a detached Swing dialog. The visual
 * shell remains TheMechanist's normal menu renderer; this authority supplies
 * navigable labels, type filtering, searchable details, and stable parsing of
 * the 8-character semantic asset ID from a displayed row.
 */
public final class SemanticAssetInfopediaAuthority {
    public static final String VERSION = "0.9.10ju-stage2-semantic-asset-infopedia";

    private static final Pattern ID_PREFIX = Pattern.compile("^[A-Z0-9]{3,4}-[A-Z0-9]{3,4}\\b");
    private static final String MECHANIC_PREFIX = "MECHANIC - ";

    private static final AssetType[] BROWSE_TYPES = {
            AssetType.PORTRAIT,
            AssetType.WALL_TILE,
            AssetType.FLOOR_TILE,
            AssetType.ROAD_TILE,
            AssetType.SIDEWALK_TILE,
            AssetType.CORRIDOR_TILE,
            AssetType.OBJECT,
            AssetType.FIXTURE,
            AssetType.MACHINE,
            AssetType.ITEM_ICON,
            AssetType.WEAPON_ICON,
            AssetType.ARMOR_ICON,
            AssetType.UI_ICON,
            AssetType.CORPSE_DECAY
    };

    private SemanticAssetInfopediaAuthority() {
    }

    public static AssetType[] browseTypes() {
        return BROWSE_TYPES.clone();
    }

    public static String typeLabel(AssetType type) {
        return type == null ? "All semantic assets" : type.displayName();
    }

    public static List<String> entries(AssetRegistry registry, AssetType selectedType, String filter) {
        List<String> out = new ArrayList<>();
        AssetRegistry safe = registry == null ? AssetRegistry.empty() : registry;

        List<AssetMetadata> source = selectedType == null ? safe.all() : safe.byType(selectedType);
        String q = filter == null ? "" : filter.trim();
        int added = 0;
        if (selectedType == null) {
            for (MechanicEntry entry : mechanicEntries()) {
                if (!entry.matchesFilter(q)) continue;
                out.add(formatMechanicEntry(entry));
                added++;
            }
        }
        for (AssetMetadata metadata : source) {
            if (!metadata.matchesFilter(q)) continue;
            out.add(formatEntry(metadata));
            added++;
        }
        if (added == 0) {
            out.add("No assets match the active type/filter.");
        }
        return out;
    }

    public static String formatEntry(AssetMetadata metadata) {
        return metadata.id() + " — " + metadata.name() + " [" + metadata.type().displayName() + "]";
    }

    public static Optional<String> assetIdFromEntry(String entry) {
        if (entry == null) return Optional.empty();
        String trimmed = entry.trim().toUpperCase(Locale.ROOT);
        if (trimmed.length() < 8) return Optional.empty();
        String candidate = trimmed.substring(0, 8);
        if (!ID_PREFIX.matcher(candidate).find()) return Optional.empty();
        return Optional.of(candidate);
    }

    public static Optional<AssetMetadata> metadataForEntry(AssetRegistry registry, String entry) {
        AssetRegistry safe = registry == null ? AssetRegistry.empty() : registry;
        return assetIdFromEntry(entry).flatMap(safe::find);
    }

    public static List<String> detailLines(AssetRegistry registry, String entry, AssetType selectedType, String filter) {
        List<String> lines = new ArrayList<>();
        AssetRegistry safe = registry == null ? AssetRegistry.empty() : registry;
        Optional<MechanicEntry> mechanic = mechanicForEntry(entry);
        if (mechanic.isPresent()) {
            return mechanicDetailLines(mechanic.get());
        }
        Optional<AssetMetadata> metadata = metadataForEntry(safe, entry);
        if (metadata.isEmpty()) {
            lines.add(entry == null || entry.isBlank() ? "Semantic Asset Index" : entry);
            lines.add("Reference ID: semantic-registry");
            lines.add("Authority: " + VERSION);
            lines.add("");
            lines.add("Purpose: The Semantic Asset Index is the durable bridge between graphical files and player-facing meaning. It lets the InfoPedia show what each sprite is supposed to represent before the live renderer is migrated away from scattered file paths.");
            lines.add("Active type filter: " + typeLabel(selectedType));
            lines.add("Active search filter: " + (filter == null || filter.isBlank() ? "<none>" : filter.trim()));
            AssetRegistry.RegistryAudit audit = safe.audit(false);
            lines.add("Registered assets: " + audit.entryCount());
            lines.add("Registered categories with entries: " + audit.categoryCount());
            lines.add("Unknown type rows: " + audit.unknownTypeCount());
            lines.add("Blank descriptions: " + audit.blankDescriptionCount());
            lines.add("");
            lines.add("How to use: click an asset row to inspect its exact 8-character ID, image preview, purpose/type, path, and semantic description. Press / or the FILTER button to focus search, type to search ID/name/path/description, Backspace to delete, Enter to return to list navigation, and CLEAR to reset.");
            lines.add("");
            lines.add("Stage status: Stage 2 completes the internal browser/audit surface. It does not yet replace item icons, tile rendering, machine art, portraits, or world rendering paths. That remains Stage 3+ work.");
            return lines;
        }

        AssetMetadata m = metadata.get();
        lines.add(m.name());
        lines.add("Reference ID: " + m.id());
        lines.add("Type / purpose: " + m.type().displayName());
        lines.add("Registry path/URI: " + m.pathOrUri());
        lines.add("");
        lines.add("Semantic description:");
        lines.add(m.semanticDescription());
        lines.add("");
        if (m.type() == AssetType.PORTRAIT) {
            List<String> partitionLines = PortraitSemanticAssetAuthority.infopediaLines(safe.projectRoot(), m.id());
            if (!partitionLines.isEmpty()) {
                lines.add("Stage 8 portrait/entity partition:");
                lines.addAll(partitionLines);
                lines.add("");
            }
        }
        lines.add("Migration note: callers should eventually reference this art with AssetManager.getAsset(\"" + m.id() + "\") instead of direct file-path loading. This row is now inspectable from the game-owned InfoPedia before renderer migration begins.");
        return lines;
    }

    static List<String> mechanicEntryRows(String filter) {
        ArrayList<String> rows = new ArrayList<>();
        String q = filter == null ? "" : filter.trim();
        for (MechanicEntry entry : mechanicEntries()) {
            if (entry.matchesFilter(q)) rows.add(formatMechanicEntry(entry));
        }
        return rows;
    }

    static List<String> mechanicDetailLinesByKey(String key) {
        return mechanicEntries().stream()
                .filter(e -> e.key().equalsIgnoreCase(key == null ? "" : key.trim()))
                .findFirst()
                .map(SemanticAssetInfopediaAuthority::mechanicDetailLines)
                .orElseGet(() -> List.of("Mechanic reference", "No matching mechanic entry is registered yet."));
    }

    private static List<MechanicEntry> mechanicEntries() {
        return List.of(
                new MechanicEntry("look-examine", "Look and Examine", "Inspection",
                        "Look gives immediate surface facts. Examine spends attention on the selected visible target and deepens the read without exposing hidden records.",
                        List.of(
                                ControlReferenceTextSubsystem.contextPromptLine("Look", 0, InputAction.EXAMINE, InputAction.CANCEL, "Examine the selected visible target or back out."),
                                "Readable boundary: surface observations first; repeated examination can add intent, state, equipment, condition, and context.",
                                "Guard: Milestone02LookExamineReadabilitySmoke checks depth wording, route/examine sanitization, and prompt coverage."
                        ),
                        List.of("movement-planning", "context-prompts")),
                new MechanicEntry("movement-planning", "Movement Planning", "Movement",
                        "Movement planning lets the player place a ghost target, preview or understand the route, and receive clear refusal text before committing.",
                        List.of(
                                ControlReferenceTextSubsystem.contextPromptLine("Movement planning", 0, InputAction.CONFIRM, InputAction.CANCEL, "Confirm the ghost target, nudge it, or cancel safely."),
                                "Readable outcomes: Movement target selected, Partial route, Destination occupied, Path blocked, Cannot reach from here, or outside the current area.",
                                "Guard: Milestone02MovementPlanningReadabilitySmoke checks selected, partial, occupied, blocked, out-of-area, and unreachable routes."
                        ),
                        List.of("look-examine", "context-prompts", "menu-uniformity")),
                new MechanicEntry("context-prompts", "Context Prompts", "Controls",
                        "Context prompts show the current action names and active keyboard/controller bindings for the panel the player is using.",
                        contextPromptInfopediaLines(),
                        List.of("look-examine", "movement-planning", "menu-uniformity")),
                new MechanicEntry("menu-uniformity", "Menu Uniformity", "Menus",
                        "Menu uniformity keeps ordinary game menus aligned around purpose, back behavior, panes, prompts, disabled-state explanations, and transfer rules.",
                        menuUniformityInfopediaLines(),
                        List.of("context-prompts", "movement-planning", "input-rebinding-audit")),
                new MechanicEntry("input-rebinding-audit", "Input Rebinding Audit", "Controls",
                        "Input rebinding audit rows expose action labels, contexts, default bindings, controller prompts, conflict notes, required recovery actions, and current-profile readiness.",
                        InputRebindingAuditAuthority.infopediaLines(),
                        List.of("context-prompts", "movement-planning", "menu-uniformity"))
        );
    }

    private static List<String> contextPromptInfopediaLines() {
        ArrayList<String> lines = new ArrayList<>();
        lines.add("Prompt source: ControlReferenceTextSubsystem composes context prompts from shared action labels and binding prompts.");
        lines.add("Keyboard/mouse users see keyboard prompts directly; controller views include controller text plus keyboard fallback for recovery.");
        lines.addAll(ControlReferenceTextSubsystem.contextPromptLines(4, ControlReferenceTextSubsystem.defaultContextPrompts()));
        lines.add("Guard: Milestone02ContextPromptReadabilitySmoke checks major-panel coverage and leak-free keyboard/controller prompt variants.");
        return lines;
    }

    private static List<String> menuUniformityInfopediaLines() {
        UniversalWindowAuthority authority = new UniversalWindowAuthority();
        ArrayList<String> lines = new ArrayList<>();
        lines.add(authority.playerFacingSummary());
        lines.addAll(authority.playerFacingMenuAuditLines());
        lines.add("Guard: Milestone02MenuUniformityReadabilitySmoke checks major-window coverage and leak-free menu audit wording.");
        return lines;
    }

    private static String formatMechanicEntry(MechanicEntry entry) {
        return MECHANIC_PREFIX + entry.title() + " [" + entry.category() + "]";
    }

    private static Optional<MechanicEntry> mechanicForEntry(String row) {
        if (row == null) return Optional.empty();
        String value = row.trim();
        if (!value.startsWith(MECHANIC_PREFIX)) return Optional.empty();
        String title = value.substring(MECHANIC_PREFIX.length());
        int bracket = title.lastIndexOf(" [");
        if (bracket >= 0) title = title.substring(0, bracket);
        String wanted = title.trim();
        return mechanicEntries().stream()
                .filter(e -> e.title().equalsIgnoreCase(wanted) || e.key().equalsIgnoreCase(wanted))
                .findFirst();
    }

    private static List<String> mechanicDetailLines(MechanicEntry entry) {
        ArrayList<String> lines = new ArrayList<>();
        lines.add(entry.title());
        lines.add("Reference: " + entry.key().replace('-', ' '));
        lines.add("Category: " + entry.category());
        lines.add("");
        lines.add("Purpose:");
        lines.add(PlayerFacingText.sanitize(entry.summary()));
        lines.add("");
        lines.add("Player-facing rules:");
        for (String line : entry.lines()) {
            lines.add(PlayerFacingText.sanitize(line));
        }
        lines.add("");
        List<String> related = relatedEntryRowsFor(entry);
        lines.add("Related entries:");
        if (related.isEmpty()) lines.add("None registered yet.");
        else lines.addAll(related);
        return lines;
    }

    static List<String> relatedRowsForEntry(AssetRegistry registry, String row, AssetType selectedType) {
        Optional<MechanicEntry> mechanic = mechanicForEntry(row);
        if (mechanic.isPresent()) return relatedEntryRowsFor(mechanic.get());
        Optional<AssetMetadata> metadata = metadataForEntry(registry, row);
        if (metadata.isEmpty()) return List.of();
        AssetMetadata m = metadata.get();
        ArrayList<String> related = new ArrayList<>();
        AssetRegistry safe = registry == null ? AssetRegistry.empty() : registry;
        for (AssetMetadata candidate : safe.byType(m.type())) {
            if (candidate.id().equals(m.id())) continue;
            related.add(formatEntry(candidate));
            if (related.size() >= 3) break;
        }
        return List.copyOf(related);
    }

    static Optional<String> firstRelatedRowForEntry(AssetRegistry registry, String row, AssetType selectedType) {
        List<String> rows = relatedRowsForEntry(registry, row, selectedType);
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }

    private static List<String> relatedEntryRowsFor(MechanicEntry entry) {
        ArrayList<String> rows = new ArrayList<>();
        if (entry == null) return rows;
        for (String key : entry.relatedKeys()) {
            mechanicByKey(key).map(SemanticAssetInfopediaAuthority::formatMechanicEntry).ifPresent(rows::add);
        }
        return List.copyOf(rows);
    }

    private static Optional<MechanicEntry> mechanicByKey(String key) {
        String wanted = key == null ? "" : key.trim();
        return mechanicEntries().stream()
                .filter(e -> e.key().equalsIgnoreCase(wanted))
                .findFirst();
    }

    private record MechanicEntry(String key, String title, String category, String summary, List<String> lines, List<String> relatedKeys) {
        boolean matchesFilter(String filter) {
            if (filter == null || filter.isBlank()) return true;
            String q = filter.toLowerCase(Locale.ROOT);
            if (key.toLowerCase(Locale.ROOT).contains(q)
                    || title.toLowerCase(Locale.ROOT).contains(q)
                    || category.toLowerCase(Locale.ROOT).contains(q)
                    || summary.toLowerCase(Locale.ROOT).contains(q)) {
                return true;
            }
            for (String line : lines) {
                if (line != null && line.toLowerCase(Locale.ROOT).contains(q)) return true;
            }
            return false;
        }
    }
}
