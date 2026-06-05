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
}
