package mechanist.assets;

import java.util.Locale;

/**
 * Broad semantic purpose for a graphical asset entry.
 *
 * Stage 1 deliberately supports more than the four initial requested groups so
 * later migration stages can index item icons, roads, corridors, fixtures, and
 * UI art without changing the registry model again.
 */
public enum AssetType {
    PORTRAIT("Portrait"),
    WALL_TILE("Wall Tile"),
    FLOOR_TILE("Floor Tile"),
    ROAD_TILE("Road Tile"),
    SIDEWALK_TILE("Sidewalk Tile"),
    CORRIDOR_TILE("Corridor Tile"),
    OBJECT("Object"),
    MACHINE("Machine"),
    FIXTURE("Fixture"),
    ITEM_ICON("Item Icon"),
    WEAPON_ICON("Weapon Icon"),
    ARMOR_ICON("Armor Icon"),
    UI_ICON("UI Icon"),
    CORPSE_DECAY("Corpse / Decay"),
    INTERNAL("Internal"),
    UNKNOWN("Unknown");

    private final String displayName;

    AssetType(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }

    public static AssetType fromToken(String token) {
        if (token == null || token.isBlank()) {
            return UNKNOWN;
        }
        String normalized = token.trim()
                .toUpperCase(Locale.ROOT)
                .replace('-', '_')
                .replace(' ', '_')
                .replace('/', '_');
        for (AssetType type : values()) {
            if (type.name().equals(normalized)) {
                return type;
            }
        }
        return UNKNOWN;
    }
}
