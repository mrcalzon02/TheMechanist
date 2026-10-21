package mechanist;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

/**
 * Materializes one deterministic, shared multi-floor blueprint per floor slice.
 * The site coordinates are based on the atlas seed (not each floor's slice
 * seed). Ordinary rooms reserve the same footprints before they are stamped.
 */
final class MultiFloorBuildingPlacementAuthority {
    record Site(VerticalBuildingBlueprintAuthority.Blueprint blueprint, int x, int y) {
        Site {
            if (blueprint == null) throw new IllegalArgumentException("missing building plan");
        }
        Rectangle footprint() {
            return new Rectangle(x, y, blueprint.footprint().width(), blueprint.footprint().height());
        }
        Rectangle translate(VerticalBuildingBlueprintAuthority.Rect local) {
            return new Rectangle(x + local.x(), y + local.y(), local.width(), local.height());
        }
    }

    private MultiFloorBuildingPlacementAuthority() { }

    static List<Site> planSites(long atlasSeed, int sx, int sy, int zx, int zy, int width, int height) {
        if (width < 120 || height < 120) return List.of();
        var plans = VerticalBuildingBlueprintAuthority.forZone(atlasSeed, sx, sy, zx, zy);
        int cx = width / 2, cy = height / 2;
        ArrayList<Site> sites = new ArrayList<>();
        sites.add(new Site(plans.get(0), cx + 48, cy + 44));
        sites.add(new Site(plans.get(1), cx - 48 - plans.get(1).footprint().width(), cy + 44));
        sites.add(new Site(plans.get(2), cx + 48, cy - 46 - plans.get(2).footprint().height()));
        for (Site site : sites) {
            Rectangle rect = site.footprint();
            if (rect.x < 12 || rect.y < 12 || rect.x + rect.width >= width - 12
                    || rect.y + rect.height >= height - 12)
                throw new IllegalArgumentException("building site outside safe zone interior");
        }
        for (int i = 0; i < sites.size(); i++)
            for (int j = i + 1; j < sites.size(); j++)
                if (sites.get(i).footprint().intersects(sites.get(j).footprint()))
                    throw new IllegalArgumentException("multi-floor building footprints overlap");
        return List.copyOf(sites);
    }

    static boolean reserved(World w, Rectangle candidate) {
        if (w == null || candidate == null || w.sewerLayer) return false;
        for (Site site : w.buildingSites)
            if (site.blueprint().containsFloor(w.floor) && site.footprint().intersects(candidate))
                return true;
        return false;
    }

    static int stampFloor(World w) {
        if (w == null || w.sewerLayer || w.buildingSites.isEmpty()) return 0;
        int total = 0;
        for (Site site : w.buildingSites) {
            var plate = site.blueprint().plate(w.floor);
            if (plate == null) continue;
            List<Integer> facade = availableFacade(w, site);
            if (!clearFootprint(w, site) || facade.isEmpty()) {
                DebugLog.warn("MULTI_FLOOR_BUILDING",
                        "blocked building footprint/road approach id=" + site.blueprint().buildingId()
                                + " floor=" + w.floor + " bounds=" + site.footprint());
                continue;
            }
            total += stampPlate(w, site, plate, facade);
            DebugLog.audit("MULTI_FLOOR_BUILDING",
                    "placed id=" + site.blueprint().buildingId() + " floor=" + w.floor
                            + " rooms=" + plate.roomCount() + " bounds=" + site.footprint());
        }
        return total;
    }

    private static boolean clearFootprint(World w, Site site) {
        Rectangle rect = site.footprint();
        if (rect.x < 2 || rect.y < 2 || rect.x + rect.width >= w.w - 2
                || rect.y + rect.height >= w.h - 2) return false;
        for (int x = rect.x; x < rect.x + rect.width; x++)
            for (int y = rect.y; y < rect.y + rect.height; y++)
                if (w.roomIds[x][y] >= 0 || w.tiles[x][y] != '#') return false;
        return true;
    }

    /**
     * Locate a clear straight entrance to the nearest parallel street. Probe
     * first and carve later so a blocked road approach never half-stamps a
     * building. The terminal sidewalk or road cell is never overwritten.
     */
    private static List<Integer> availableFacade(World w, Site site) {
        boolean westSite = site.x() < w.w / 2;
        int dir = westSite ? 1 : -1;
        int start = westSite ? site.x() + site.footprint().width - 1 : site.x();
        int row = site.y() + (site.blueprint().kind()
                == VerticalBuildingBlueprintAuthority.Kind.ATRIUM_HALL ? 14 : 12);
        ArrayList<Integer> cells = new ArrayList<>();
        for (int step = 0, x = start; step < 64 && w.inBounds(x, row); step++, x += dir) {
            if (w.roomIds[x][row] >= 0) return List.of();
            char glyph = w.tiles[x][row];
            if (glyph == RoadGridIntegrationAuthority.SIDEWALK
                    || glyph == RoadGridIntegrationAuthority.ROAD_LANE)
                return List.copyOf(cells);
            if (glyph != '#' && !w.isCorridorGlyph(glyph)) return List.of();
            cells.add(x);
        }
        return List.of();
    }

    private static int stampPlate(World w, Site site,
                                  VerticalBuildingBlueprintAuthority.FloorPlate plate,
                                  List<Integer> facade) {
        char corridor = w.zoneType.corridorGlyph(w.r);
        for (var space : plate.spaces()) {
            if (space.use().countedRoom()) continue;
            Rectangle rect = site.translate(space.bounds());
            char glyph = space.use() == VerticalBuildingBlueprintAuthority.Use.OPEN_TO_BELOW
                    ? InterstitialInfrastructureApi.VOID_SPACE : corridor;
            fill(w, rect, glyph);
            if (space.use() == VerticalBuildingBlueprintAuthority.Use.OPEN_TO_BELOW)
                w.buildingOpenVolumes.add(rect);
        }

        if (site.blueprint().kind() == VerticalBuildingBlueprintAuthority.Kind.ATRIUM_HALL) {
            // An interior bypass joins both upper balconies without stepping
            // across the upper floors' non-walkable atrium opening.
            fill(w, new Rectangle(site.x() + 8, site.y() + 5, 2, 22), corridor);
            fill(w, new Rectangle(site.x() + 6, site.y() + 14, 2, 1), corridor);
        } else {
            fill(w, new Rectangle(site.x() + 6, site.y() + 12, 1, 1), corridor);
            fill(w, new Rectangle(site.x() + 3, site.y() + 8, 1, 2), corridor);
        }

        int facadeY = site.y() + (site.blueprint().kind()
                == VerticalBuildingBlueprintAuthority.Kind.ATRIUM_HALL ? 14 : 12);
        // Connect from the building's actual landing up to the validated road.
        if (site.x() < w.w / 2) {
            fill(w, new Rectangle(site.x() + 29, facadeY, 3, 1), corridor);
        } else {
            fill(w, new Rectangle(site.x(), facadeY, 2, 1), corridor);
        }
        for (int x : facade) w.tiles[x][facadeY] = corridor;

        int roomsMade = 0;
        for (var space : plate.spaces()) {
            if (!space.use().countedRoom()) continue;
            Rectangle rect = site.translate(space.bounds());
            int index = w.rooms.size();
            w.carve(rect);
            w.rooms.add(rect);
            String id = site.blueprint().buildingId();
            String label = site.blueprint().kind().name().replace('_', ' ')
                    + " / " + space.use().name().replace('_', ' ') + " / floor " + w.floor;
            RoomProfile profile = new RoomProfile(label,
                    "Building " + id + " room " + space.id()
                            + ": permanent multi-floor structural membership",
                    35, Faction.HIVER, new String[]{"cloth scraps", "sealed water ration"},
                    new char[]{'q','c','u'});
            w.roomProfiles.set(index, profile);
            w.roomFactions.set(index, Faction.HIVER);
            w.buildingRoomIds.put(space.id(), index);
            connectRoomToHall(w, site, rect, corridor);
            roomsMade++;
        }

        var lift = plate.space(VerticalBuildingBlueprintAuthority.Use.ELEVATOR);
        if (lift != null) {
            Rectangle r = site.translate(lift.bounds());
            w.tiles[r.x + 1][r.y + 1] = 'E';
        }
        return roomsMade;
    }

    private static void fill(World w, Rectangle rect, char glyph) {
        for (int x = rect.x; x < rect.x + rect.width; x++)
            for (int y = rect.y; y < rect.y + rect.height; y++)
                if (w.inBounds(x,y) && w.roomIds[x][y] < 0) w.tiles[x][y] = glyph;
    }

    /**
     * Link individual rooms to common corridors without cutting through other
     * room interiors or the OPEN_TO_BELOW volume. Failed links are left for the
     * normal strict topology validation rather than silently tunneling walls.
     */
    private static void connectRoomToHall(World w, Site site, Rectangle room, char corridor) {
        int x = room.x + room.width / 2;
        boolean upperSide = room.y - site.y() < site.footprint().height / 2;
        int doorY = upperSide ? room.y + room.height - 1 : room.y;
        int dir = upperSide ? 1 : -1;
        ArrayList<Integer> path = new ArrayList<>();
        for (int y = doorY + dir, step = 0; step < 8 && w.inBounds(x,y); step++, y += dir) {
            if (w.roomIds[x][y] >= 0) return;
            if (w.isCorridorGlyph(w.tiles[x][y])) {
                w.tiles[x][doorY] = DoorType.forZone(w.zoneType, w.r).symbol;
                for (int carveY : path) w.tiles[x][carveY] = corridor;
                return;
            }
            if (w.tiles[x][y] != '#') return;
            path.add(y);
        }
    }
}
