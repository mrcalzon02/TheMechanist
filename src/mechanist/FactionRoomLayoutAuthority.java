package mechanist;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.util.Locale;
import java.util.Random;

/**
 * Faction-aware room layout planner for road-first room stamping.
 *
 * This authority keeps size-band, layout-shape, and faction-quality decisions
 * out of the raw World carving loop while still letting World own collision,
 * connection, and tile mutation.
 */
final class FactionRoomLayoutAuthority {
    static final String VERSION = "faction-room-layout-0.9.10ja";

    enum RoomScale { SMALL, MEDIUM, LARGE }
    enum LayoutShape { COMPACT, LONG_BAY, WIDE_BAY, ALCOVE, CELL_ROW, GALLERY, SERVICE_SPINE, WORKSHOP_GRID }

    static final class LayoutPlan {
        final RoomScale scale;
        final LayoutShape shape;
        final int width;
        final int height;
        final int factionQualityTier;
        final String factionQualityLabel;
        final int intendedFeatures;
        final int blockingFeatures;
        final int interactiveFeatures;
        final boolean qcAccepted;
        final String note;

        LayoutPlan(RoomScale scale,
                   LayoutShape shape,
                   int width,
                   int height,
                   int factionQualityTier,
                   String factionQualityLabel,
                   int intendedFeatures,
                   int blockingFeatures,
                   int interactiveFeatures,
                   boolean qcAccepted,
                   String note) {
            this.scale = scale == null ? RoomScale.MEDIUM : scale;
            this.shape = shape == null ? LayoutShape.COMPACT : shape;
            this.width = Math.max(4, width);
            this.height = Math.max(4, height);
            this.factionQualityTier = Math.max(1, Math.min(5, factionQualityTier));
            this.factionQualityLabel = factionQualityLabel == null || factionQualityLabel.isBlank() ? "Common" : factionQualityLabel;
            this.intendedFeatures = Math.max(0, intendedFeatures);
            this.blockingFeatures = Math.max(0, blockingFeatures);
            this.interactiveFeatures = Math.max(0, interactiveFeatures);
            this.qcAccepted = qcAccepted;
            this.note = note == null ? "" : note;
        }

        Dimension size() { return new Dimension(width, height); }

        String summary() {
            return "layout=" + shape + " scale=" + scale + " quality=" + factionQualityLabel
                    + " features=" + intendedFeatures + "/" + blockingFeatures + "/" + interactiveFeatures
                    + " qc=" + (qcAccepted ? "accepted" : "expanded") + (note.isBlank() ? "" : " " + note);
        }
    }

    private FactionRoomLayoutAuthority() {}

    static LayoutPlan planFor(StampedRoomSpec spec, int salt, Random rng) {
        Random r = rng == null ? new Random(salt * 1103515245L + 12345L) : rng;
        String kind = normalizedKind(spec == null ? null : spec.kind);
        Faction faction = spec == null || spec.faction == null ? Faction.NONE : spec.faction;
        int quality = factionQualityTier(faction);
        RoomScale scale = chooseScale(kind, quality, salt, r);
        LayoutShape shape = chooseShape(kind, faction, scale, salt, r);
        Dimension size = baseSize(kind, scale, shape);
        if ((salt & 1) == 1 && shape != LayoutShape.LONG_BAY && shape != LayoutShape.WIDE_BAY) {
            size = new Dimension(size.height, size.width);
        }
        int jitter = scale == RoomScale.LARGE ? 2 : 1;
        size.width = Math.max(4, Math.min(13, size.width + r.nextInt(jitter + 1)));
        size.height = Math.max(4, Math.min(10, size.height + r.nextInt(jitter + 1)));

        int intended = intendedFeatureCount(kind, scale, quality);
        int blocking = Math.max(1, intended / 2);
        int interactive = Math.max(1, intended / 3);
        String label = (spec == null ? "room" : spec.name) + " " + kind + " " + scale + " " + shape;
        WorldGenerationApi.Phase3RoomFeatureLoad load =
                WorldGenerationApi.estimatePhase3RoomFeatureLoadFromCount(intended, blocking, interactive);
        boolean accepted = WorldGenerationApi.shouldAcceptPhase3RoomProfileOrStamp(
                size.width, size.height, load, label, true, true, true);
        if (!accepted && scale != RoomScale.LARGE) {
            scale = scale == RoomScale.SMALL ? RoomScale.MEDIUM : RoomScale.LARGE;
            size = baseSize(kind, scale, shape);
            load = WorldGenerationApi.estimatePhase3RoomFeatureLoadFromCount(intended, blocking, interactive);
            accepted = WorldGenerationApi.shouldAcceptPhase3RoomProfileOrStamp(
                    size.width, size.height, load, label + " expanded", true, true, true);
        }
        return new LayoutPlan(scale, shape, size.width, size.height, quality, qualityLabel(quality),
                intended, blocking, interactive, accepted, "faction=" + faction.label);
    }

    static String decorateFeatureText(String existing, LayoutPlan plan) {
        if (plan == null) return existing == null ? "" : existing;
        String base = existing == null || existing.isBlank() ? "Generated room." : existing;
        return base + " Layout plan: " + plan.summary() + ".";
    }

    static void applyInteriorLayout(World world, int roomId, Rectangle room, StampedRoomSpec spec, LayoutPlan plan) {
        if (world == null || room == null || plan == null) return;
        int minX = room.x + 1;
        int maxX = room.x + room.width - 2;
        int minY = room.y + 1;
        int maxY = room.y + room.height - 2;
        if (minX > maxX || minY > maxY) return;

        switch (plan.shape) {
            case CELL_ROW -> stampCellRow(world, roomId, minX, maxX, minY, maxY);
            case GALLERY -> stampGallery(world, minX, maxX, minY, maxY, plan);
            case SERVICE_SPINE -> stampServiceSpine(world, minX, maxX, minY, maxY);
            case WORKSHOP_GRID -> stampWorkshopGrid(world, minX, maxX, minY, maxY, spec);
            case ALCOVE -> stampAlcovePartitions(world, roomId, minX, maxX, minY, maxY);
            case LONG_BAY, WIDE_BAY -> stampBayLines(world, minX, maxX, minY, maxY, plan);
            case COMPACT -> stampCompactFocus(world, minX, maxX, minY, maxY, spec);
        }
        stampQualityCue(world, minX, maxX, minY, maxY, plan);
    }

    private static String normalizedKind(String raw) {
        return raw == null ? "ROOM" : raw.trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
    }

    private static RoomScale chooseScale(String kind, int quality, int salt, Random r) {
        int roll = Math.floorMod(salt + r.nextInt(100) + quality * 7, 100);
        if (kind.contains("WAREHOUSE") || kind.contains("CAFETERIA") || kind.contains("BARRACKS") || kind.contains("TRAINING")) {
            if (roll < 28) return RoomScale.SMALL;
            if (roll < 72) return RoomScale.MEDIUM;
            return RoomScale.LARGE;
        }
        if (kind.contains("CLINIC") || kind.contains("SANITATION") || kind.contains("DAYCARE")) {
            if (roll < 42) return RoomScale.SMALL;
            if (roll < 86) return RoomScale.MEDIUM;
            return RoomScale.LARGE;
        }
        if (quality >= 4 && roll > 62) return RoomScale.LARGE;
        if (roll < 35) return RoomScale.SMALL;
        if (roll < 82) return RoomScale.MEDIUM;
        return RoomScale.LARGE;
    }

    private static LayoutShape chooseShape(String kind, Faction faction, RoomScale scale, int salt, Random r) {
        String family = faction == null ? "" : faction.name();
        if (kind.contains("DORM") || kind.contains("BARRACKS") || kind.contains("SECURITY")) return LayoutShape.CELL_ROW;
        if (kind.contains("CAFETERIA") || kind.contains("STOREFRONT")) return family.contains("NOBLE") ? LayoutShape.GALLERY : LayoutShape.LONG_BAY;
        if (kind.contains("WAREHOUSE") || kind.contains("FOOD_STORE") || kind.contains("LOGISTICS")) return LayoutShape.SERVICE_SPINE;
        if (kind.contains("MACHINERY") || kind.contains("WORKSHOP") || family.contains("MECHAN")) return LayoutShape.WORKSHOP_GRID;
        if (kind.contains("LIBRARY") || kind.contains("LEARNING") || family.contains("LEDGER") || family.contains("INN")) return LayoutShape.GALLERY;
        if (scale == RoomScale.SMALL) return LayoutShape.COMPACT;
        if (Math.floorMod(salt + r.nextInt(7), 3) == 0) return LayoutShape.ALCOVE;
        return scale == RoomScale.LARGE ? LayoutShape.WIDE_BAY : LayoutShape.COMPACT;
    }

    private static Dimension baseSize(String kind, RoomScale scale, LayoutShape shape) {
        int w = 5;
        int h = 5;
        switch (scale) {
            case SMALL -> { w = 4; h = 4; }
            case MEDIUM -> { w = 6; h = 5; }
            case LARGE -> { w = 9; h = 7; }
        }
        if (kind.contains("CAFETERIA") || kind.contains("WAREHOUSE") || kind.contains("FOOD_STORE")) w += 2;
        if (kind.contains("BARRACKS") || kind.contains("DORMITORY")) w += 1;
        if (kind.contains("CLINIC") || kind.contains("SANITATION")) h = Math.max(4, h - 1);
        if (shape == LayoutShape.LONG_BAY) w += 2;
        if (shape == LayoutShape.WIDE_BAY) h += 1;
        if (shape == LayoutShape.CELL_ROW) w += 1;
        if (shape == LayoutShape.WORKSHOP_GRID) { w += 1; h += 1; }
        return new Dimension(w, h);
    }

    private static int intendedFeatureCount(String kind, RoomScale scale, int quality) {
        int base = switch (scale) {
            case SMALL -> 3;
            case MEDIUM -> 5;
            case LARGE -> 8;
        };
        if (kind.contains("WAREHOUSE") || kind.contains("FOOD_STORE") || kind.contains("MACHINERY")) base += 2;
        if (kind.contains("CAFETERIA") || kind.contains("BARRACKS") || kind.contains("SECURITY")) base += 1;
        return Math.max(2, base + Math.max(0, quality - 3));
    }

    private static int factionQualityTier(Faction faction) {
        if (faction == null || faction == Faction.NONE) return 2;
        String n = faction.name();
        if (n.contains("NOBLE")) return 5;
        if (n.contains("MECHAN")) return 4;
        if (n.contains("GUARD") || n.contains("WARDEN") || n.contains("ARBITES") || n.contains("INN") || n.contains("LEDGER") || n.contains("ADMIN")) return 3;
        if (n.contains("BANDIT") || n.contains("GANGER") || n.contains("MUTANT") || n.contains("CULT") || n.contains("HERETIC")) return 1;
        return 2;
    }

    private static String qualityLabel(int tier) {
        return switch (Math.max(1, Math.min(5, tier))) {
            case 1 -> "Rough";
            case 2 -> "Common";
            case 3 -> "Regulated";
            case 4 -> "Precise";
            default -> "Luxurious";
        };
    }

    private static void stampCellRow(World world, int roomId, int minX, int maxX, int minY, int maxY) {
        if (maxX - minX < 3) return;
        for (int x = minX + 2; x < maxX; x += 3) {
            for (int y = minY; y <= maxY; y++) {
                if (y == (minY + maxY) / 2) continue;
                setInterior(world, roomId, x, y, '#');
            }
        }
    }

    private static void stampGallery(World world, int minX, int maxX, int minY, int maxY, LayoutPlan plan) {
        for (int x = minX; x <= maxX; x += Math.max(2, 5 - Math.min(4, plan.factionQualityTier))) {
            setInterior(world, -1, x, minY, 'l');
            setInterior(world, -1, x, maxY, plan.factionQualityTier >= 4 ? 'q' : 'b');
        }
    }

    private static void stampServiceSpine(World world, int minX, int maxX, int minY, int maxY) {
        int spineY = (minY + maxY) / 2;
        for (int x = minX; x <= maxX; x++) setInterior(world, -1, x, spineY, 'b');
        for (int y = minY; y <= maxY; y += 2) {
            setInterior(world, -1, minX, y, 'u');
            setInterior(world, -1, maxX, y, 'q');
        }
    }

    private static void stampWorkshopGrid(World world, int minX, int maxX, int minY, int maxY, StampedRoomSpec spec) {
        char primary = spec == null ? 'N' : spec.primaryGlyph();
        for (int y = minY; y <= maxY; y += 2) {
            for (int x = minX; x <= maxX; x += 2) {
                setInterior(world, -1, x, y, (x + y) % 4 == 0 ? primary : 'N');
            }
        }
    }

    private static void stampAlcovePartitions(World world, int roomId, int minX, int maxX, int minY, int maxY) {
        if (maxX - minX < 4 || maxY - minY < 3) return;
        int cutX = minX + Math.max(1, (maxX - minX) / 3);
        int cutY = minY + Math.max(1, (maxY - minY) / 3);
        for (int x = minX; x <= cutX; x++) setInterior(world, roomId, x, cutY, '#');
        for (int y = minY; y <= cutY; y++) setInterior(world, roomId, cutX, y, '#');
        setInterior(world, roomId, cutX, cutY, '.');
    }

    private static void stampBayLines(World world, int minX, int maxX, int minY, int maxY, LayoutPlan plan) {
        boolean longBay = plan.shape == LayoutShape.LONG_BAY;
        if (longBay) {
            for (int x = minX; x <= maxX; x += 3) {
                setInterior(world, -1, x, minY, 'T');
                setInterior(world, -1, x, maxY, 'b');
            }
        } else {
            for (int y = minY; y <= maxY; y += 2) {
                setInterior(world, -1, minX, y, 'T');
                setInterior(world, -1, maxX, y, 'b');
            }
        }
    }

    private static void stampCompactFocus(World world, int minX, int maxX, int minY, int maxY, StampedRoomSpec spec) {
        setInterior(world, -1, (minX + maxX) / 2, (minY + maxY) / 2, spec == null ? 'q' : spec.primaryGlyph());
    }

    private static void stampQualityCue(World world, int minX, int maxX, int minY, int maxY, LayoutPlan plan) {
        char cue = switch (plan.factionQualityTier) {
            case 1 -> 'p';
            case 3 -> 'q';
            case 4 -> 'R';
            case 5 -> 'Q';
            default -> 'b';
        };
        setInterior(world, -1, maxX, maxY, cue);
    }

    private static void setInterior(World world, int roomId, int x, int y, char glyph) {
        if (world == null || !world.inBounds(x, y)) return;
        if (roomId >= 0 && glyph == '#') world.roomIds[x][y] = roomId;
        world.tiles[x][y] = glyph;
    }
}
