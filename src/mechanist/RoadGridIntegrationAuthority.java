package mechanist;

import java.awt.*;
import java.util.*;

/**
 * Road-grid terrain integration.
 *
 * Creates a recognizable four-wide street vocabulary from the plaza outward
 * while preserving rooms, protected exits, doors, special transitions, and
 * the low_32-only core-art policy.
 */
final class RoadGridIntegrationAuthority {
    static final String VERSION = "0.9.10je";
    static final char ROAD_LANE = ';';
    static final char SIDEWALK = '_';
    static final char PARKING_SPACE = '\'';
    static final int STREET_WIDTH = 4;

    static final class Result {
        int horizontalSpines;
        int verticalSpines;
        int roadTiles;
        int sidewalkTiles;
        int parkingTiles;
        int skippedRooms;
        int preservedSpecials;
        int vehicleStagingMarkers;
        int sidewalkRoadPromotions;
        int roadIntersectionTiles;
        int roadIntersectionCandidates;
        int curbParkingRuns;
        int distributedSpinePairs;
        String layer = "road-grid terrain integration";
        String summary(){
            return "layer=" + layer + " streets=" + (horizontalSpines + verticalSpines) +
                    " horizontal=" + horizontalSpines + " vertical=" + verticalSpines +
                    " distributedPairs=" + distributedSpinePairs +
                    " roadTiles=" + roadTiles + " sidewalkTiles=" + sidewalkTiles + " parkingTiles=" + parkingTiles +
                    " sidewalkRoadPromotions=" + sidewalkRoadPromotions +
                    " roadIntersectionTiles=" + roadIntersectionTiles +
                    " roadIntersectionCandidates=" + roadIntersectionCandidates +
                    " curbParkingRuns=" + curbParkingRuns +
                    " skippedRooms=" + skippedRooms + " preservedSpecials=" + preservedSpecials +
                    " vehicleStagingMarkers=" + vehicleStagingMarkers +
                    " rule=roads use atlas rows by type, crossings stamp four-by-four intersection footprints, and rare curb sections widen to sidewalk-parking-lane-lane-sidewalk";
        }
    }

    private RoadGridIntegrationAuthority() {}

    static Result apply(World w, Random r){
        Result res = new Result();
        if(w == null || w.rooms == null || w.rooms.isEmpty()) return res;
        if(r == null) r = new Random(w.seed ^ 0x90810ADL);
        Rectangle plaza = w.rooms.get(0);
        if(plaza == null) return res;
        int cx = plaza.x + plaza.width / 2;
        int cy = plaza.y + plaza.height / 2;
        applySpinesAt(w, r, res, cx, cy, Math.max(12, plaza.width + 7), Math.max(12, plaza.height + 7));
        res.vehicleStagingMarkers += seedVehicleStagingMarkers(w, plaza, r);
        return res;
    }

    static Result applyCoreSpines(World w, Random r){
        Result res = new Result();
        if(w == null) return res;
        if(r == null) r = new Random(w.seed ^ 0x90810ADL);
        int cx = Math.max(6, Math.min(w.w - 7, w.w / 2));
        int cy = Math.max(6, Math.min(w.h - 7, w.h / 2));
        int xOff = Math.max(14, Math.min(32, w.w / 5));
        int yOff = Math.max(14, Math.min(28, w.h / 5));
        applySpinesAt(w, r, res, cx, cy, xOff, yOff);
        return res;
    }

    static void applySpinesAt(World w, Random r, Result res, int cx, int cy, int xOff, int yOff){
        if(w == null || res == null) return;
        // Primary cross is core infrastructure. In road-first generation this pass
        // happens before rooms, so later rooms must respect these street cells rather
        // than requiring street painters to dodge already-carved rooms.
        res.horizontalSpines += carveHorizontalStreet(w, cy, 1, w.w - 2, res);
        res.verticalSpines += carveVerticalStreet(w, cx, 1, w.h - 2, res);

        ArrayList<Integer> verticalCenters = new ArrayList<>();
        ArrayList<Integer> horizontalCenters = new ArrayList<>();
        verticalCenters.add(cx);
        horizontalCenters.add(cy);

        // Larger configured zone sizes need materially more road frontage before
        // rooms are stamped. Keep one plaza-adjacent ring, then distribute additional
        // spine pairs toward the slice edges so road-first room placement has enough
        // valid street segments to satisfy the larger room minima.
        int verticalPairs = roadSpinePairCount(w.w, 120, 70);
        int horizontalPairs = roadSpinePairCount(w.h, 96, 56);
        addSpinePair(w, res, verticalCenters, cx, Math.max(12, xOff), true);
        addSpinePair(w, res, horizontalCenters, cy, Math.max(12, yOff), false);
        int xStep = Math.max(Math.max(16, xOff), (w.w - 16) / Math.max(3, verticalPairs * 2 + 1));
        int yStep = Math.max(Math.max(16, yOff), (w.h - 16) / Math.max(3, horizontalPairs * 2 + 1));
        for(int i=2; i<=verticalPairs; i++) addSpinePair(w, res, verticalCenters, cx, i * xStep, true);
        for(int i=2; i<=horizontalPairs; i++) addSpinePair(w, res, horizontalCenters, cy, i * yStep, false);
        stampIntersectionFootprints(w, res, verticalCenters, horizontalCenters);
        seedCurbParkingShoulders(w, r, res, verticalCenters, horizontalCenters);
        normalizeStreetCrossings(w, res);
    }

    static int roadSpinePairCount(int dimension, int baseline, int perPair){
        int extra = Math.max(0, dimension - baseline);
        return Math.max(1, Math.min(4, 1 + extra / Math.max(32, perPair)));
    }

    static void addSpinePair(World w, Result res, ArrayList<Integer> centers, int center, int offset, boolean vertical){
        if(w == null || res == null || centers == null || offset <= 0) return;
        int lo = center - offset;
        int hi = center + offset;
        int added = 0;
        if(vertical){
            if(addDistinctCenter(centers, lo, w.w)) { res.verticalSpines += carveVerticalStreet(w, lo, 2, w.h - 3, res); added++; }
            if(addDistinctCenter(centers, hi, w.w)) { res.verticalSpines += carveVerticalStreet(w, hi, 2, w.h - 3, res); added++; }
        } else {
            if(addDistinctCenter(centers, lo, w.h)) { res.horizontalSpines += carveHorizontalStreet(w, lo, 2, w.w - 3, res); added++; }
            if(addDistinctCenter(centers, hi, w.h)) { res.horizontalSpines += carveHorizontalStreet(w, hi, 2, w.w - 3, res); added++; }
        }
        if(added > 0) res.distributedSpinePairs++;
    }

    static boolean addDistinctCenter(ArrayList<Integer> centers, int value, int limit){
        if(value < 5 || value > limit - 6) return false;
        for(Integer existing: centers) if(existing != null && Math.abs(existing - value) < STREET_WIDTH + 4) return false;
        centers.add(value);
        return true;
    }

    static int carveHorizontalStreet(World w, int centerY, int x0, int x1, Result res){
        if(centerY < 3 || centerY >= w.h - 3) return 0;
        boolean touched = false;
        int[] offs = {-2,-1,0,1};
        for(int x=Math.max(1,x0); x<=Math.min(w.w-2,x1); x++){
            for(int i=0;i<offs.length;i++){
                int y = centerY + offs[i];
                if(!w.inBounds(x,y)) continue;
                char glyph = (i == 1 || i == 2) ? ROAD_LANE : SIDEWALK;
                if(paintStreetTile(w,x,y,glyph,res)) touched = true;
            }
        }
        return touched ? 1 : 0;
    }

    static int carveVerticalStreet(World w, int centerX, int y0, int y1, Result res){
        if(centerX < 3 || centerX >= w.w - 3) return 0;
        boolean touched = false;
        int[] offs = {-2,-1,0,1};
        for(int y=Math.max(1,y0); y<=Math.min(w.h-2,y1); y++){
            for(int i=0;i<offs.length;i++){
                int x = centerX + offs[i];
                if(!w.inBounds(x,y)) continue;
                char glyph = (i == 1 || i == 2) ? ROAD_LANE : SIDEWALK;
                if(paintStreetTile(w,x,y,glyph,res)) touched = true;
            }
        }
        return touched ? 1 : 0;
    }

    static void stampIntersectionFootprints(World w, Result res, ArrayList<Integer> verticalCenters, ArrayList<Integer> horizontalCenters){
        if(w == null || res == null || verticalCenters == null || horizontalCenters == null) return;
        for(Integer cx: verticalCenters) for(Integer cy: horizontalCenters){
            if(cx == null || cy == null) continue;
            for(int x=cx-2; x<=cx+1; x++) for(int y=cy-2; y<=cy+1; y++){
                if(!w.inBounds(x,y) || roomEnvelopeContains(w, x, y, 0)) continue;
                if(w.tiles[x][y] == ROAD_LANE) {
                    res.roadIntersectionTiles++;
                    continue;
                }
                if(paintStreetTile(w, x, y, ROAD_LANE, res)) res.roadIntersectionTiles++;
            }
        }
    }

    static void seedCurbParkingShoulders(World w, Random r, Result res, ArrayList<Integer> verticalCenters, ArrayList<Integer> horizontalCenters){
        if(w == null || r == null || res == null) return;
        if(horizontalCenters != null) {
            for(Integer cy: horizontalCenters) {
                if(cy == null || r.nextInt(100) >= 45) continue;
                res.curbParkingRuns += seedHorizontalCurbParking(w, r, res, cy, verticalCenters);
            }
        }
        if(verticalCenters != null) {
            for(Integer cx: verticalCenters) {
                if(cx == null || r.nextInt(100) >= 45) continue;
                res.curbParkingRuns += seedVerticalCurbParking(w, r, res, cx, horizontalCenters);
            }
        }
    }

    static int seedHorizontalCurbParking(World w, Random r, Result res, int centerY, ArrayList<Integer> crossingCenters){
        int made = 0;
        int attempts = Math.max(1, w.w / 48);
        for(int a=0; a<attempts; a++){
            boolean northSide = r.nextBoolean();
            int parkingY = centerY + (northSide ? -2 : 1);
            int outerSidewalkY = centerY + (northSide ? -3 : 2);
            int len = 4 + r.nextInt(7);
            int start = 3 + r.nextInt(Math.max(1, w.w - len - 6));
            if(carveHorizontalParkingRun(w, res, start, parkingY, outerSidewalkY, len, crossingCenters)) made++;
        }
        return made;
    }

    static int seedVerticalCurbParking(World w, Random r, Result res, int centerX, ArrayList<Integer> crossingCenters){
        int made = 0;
        int attempts = Math.max(1, w.h / 48);
        for(int a=0; a<attempts; a++){
            boolean westSide = r.nextBoolean();
            int parkingX = centerX + (westSide ? -2 : 1);
            int outerSidewalkX = centerX + (westSide ? -3 : 2);
            int len = 4 + r.nextInt(7);
            int start = 3 + r.nextInt(Math.max(1, w.h - len - 6));
            if(carveVerticalParkingRun(w, res, parkingX, outerSidewalkX, start, len, crossingCenters)) made++;
        }
        return made;
    }

    static boolean carveHorizontalParkingRun(World w, Result res, int startX, int parkingY, int sidewalkY, int len, ArrayList<Integer> crossingCenters){
        if(parkingY < 1 || sidewalkY < 1 || parkingY >= w.h - 1 || sidewalkY >= w.h - 1) return false;
        for(int x=startX; x<startX+len; x++){
            if(nearAnyCenter(x, crossingCenters, 3) || !canPaintParkingCell(w, x, parkingY) || !canPaintOuterSidewalk(w, x, sidewalkY)) return false;
        }
        for(int x=startX; x<startX+len; x++){
            paintStreetTile(w, x, parkingY, PARKING_SPACE, res);
            paintStreetTile(w, x, sidewalkY, SIDEWALK, res);
        }
        return true;
    }

    static boolean carveVerticalParkingRun(World w, Result res, int parkingX, int sidewalkX, int startY, int len, ArrayList<Integer> crossingCenters){
        if(parkingX < 1 || sidewalkX < 1 || parkingX >= w.w - 1 || sidewalkX >= w.w - 1) return false;
        for(int y=startY; y<startY+len; y++){
            if(nearAnyCenter(y, crossingCenters, 3) || !canPaintParkingCell(w, parkingX, y) || !canPaintOuterSidewalk(w, sidewalkX, y)) return false;
        }
        for(int y=startY; y<startY+len; y++){
            paintStreetTile(w, parkingX, y, PARKING_SPACE, res);
            paintStreetTile(w, sidewalkX, y, SIDEWALK, res);
        }
        return true;
    }

    static boolean nearAnyCenter(int value, ArrayList<Integer> centers, int radius){
        if(centers == null) return false;
        for(Integer center: centers) if(center != null && Math.abs(value - center) <= radius) return true;
        return false;
    }

    static boolean canPaintParkingCell(World w, int x, int y){
        if(w == null || !w.inBounds(x,y) || roomEnvelopeContains(w, x, y, 0)) return false;
        char old = w.tiles[x][y];
        return old == SIDEWALK || old == PARKING_SPACE;
    }

    static boolean canPaintOuterSidewalk(World w, int x, int y){
        if(w == null || !w.inBounds(x,y) || roomEnvelopeContains(w, x, y, 0)) return false;
        char old = w.tiles[x][y];
        if(old == ' ' || InterstitialInfrastructureApi.isInterstitialSolid(old)) return false;
        return old == '#' || old == '.' || old == SIDEWALK || old == PARKING_SPACE || w.isCorridorGlyph(old);
    }


    static void normalizeStreetCrossings(World w, Result res){
        if(w == null || w.tiles == null) return;
        int intersections = 0;
        ArrayList<Point> promotions = new ArrayList<>();
        for(int x=1;x<w.w-1;x++) for(int y=1;y<w.h-1;y++){
            if(roomEnvelopeContains(w, x, y, 0)) continue;
            if(w.tiles[x][y] == SIDEWALK) {
                boolean ewSidewalkGap = isRoadLane(w, x-1, y) && isRoadLane(w, x+1, y);
                boolean nsSidewalkGap = isRoadLane(w, x, y-1) && isRoadLane(w, x, y+1);
                if(ewSidewalkGap || nsSidewalkGap) promotions.add(new Point(x, y));
                continue;
            }
            if(w.tiles[x][y] != ROAD_LANE) continue;
            boolean ew = isRoadLane(w, x-1, y) && isRoadLane(w, x+1, y);
            boolean ns = isRoadLane(w, x, y-1) && isRoadLane(w, x, y+1);
            if(ew && ns) intersections++;
        }
        for(Point p: promotions) {
            if(w.inBounds(p.x, p.y) && w.tiles[p.x][p.y] == SIDEWALK && !roomEnvelopeContains(w, p.x, p.y, 0)) {
                w.tiles[p.x][p.y] = ROAD_LANE;
                res.sidewalkRoadPromotions++;
                res.roadTiles++;
            }
        }
        for(int x=1;x<w.w-1;x++) for(int y=1;y<w.h-1;y++){
            if(w.tiles[x][y] != ROAD_LANE || roomEnvelopeContains(w, x, y, 0)) continue;
            boolean ew = isRoadLane(w, x-1, y) && isRoadLane(w, x+1, y);
            boolean ns = isRoadLane(w, x, y-1) && isRoadLane(w, x, y+1);
            if(ew && ns) intersections++;
        }
        res.roadIntersectionCandidates = intersections;
    }

    static boolean isRoadLane(World w, int x, int y){
        return w != null && w.inBounds(x,y) && w.tiles[x][y] == ROAD_LANE && !roomEnvelopeContains(w, x, y, 0);
    }

    static boolean isSidewalk(World w, int x, int y){
        return w != null && w.inBounds(x,y) && w.tiles[x][y] == SIDEWALK && !roomEnvelopeContains(w, x, y, 0);
    }

    static boolean isParkingSpace(World w, int x, int y){
        return w != null && w.inBounds(x,y) && w.tiles[x][y] == PARKING_SPACE && !roomEnvelopeContains(w, x, y, 0);
    }

    static boolean roomEnvelopeContains(World w, int x, int y, int pad){
        if(w == null || !w.inBounds(x,y)) return false;
        if(w.roomIds != null && w.roomIds[x][y] >= 0) return true;
        if(w.rooms == null) return false;
        int p = Math.max(0, pad);
        for(Rectangle rr: w.rooms){
            if(rr == null) continue;
            if(x >= rr.x - p && x < rr.x + rr.width + p && y >= rr.y - p && y < rr.y + rr.height + p) return true;
        }
        return false;
    }

    static boolean paintStreetTile(World w, int x, int y, char glyph, Result res){
        if(!w.inBounds(x,y)) return false;
        if(roomEnvelopeContains(w, x, y, 0)){ res.skippedRooms++; return false; }
        char old = w.tiles[x][y];
        if(old == 'D' || old == 'S' || old == 'v' || old == 'E' || w.isDoorSymbol(old)){
            res.preservedSpecials++; return false;
        }
        if(old == ' ' || InterstitialInfrastructureApi.isInterstitialSolid(old)) return false;
        if(old != '#' && !w.isCorridorGlyph(old) && old != '.' && old != ROAD_LANE && old != SIDEWALK && old != PARKING_SPACE) return false;
        if(old == glyph) return false;
        w.tiles[x][y] = glyph;
        if(glyph == ROAD_LANE) res.roadTiles++;
        else if(glyph == PARKING_SPACE) res.parkingTiles++;
        else res.sidewalkTiles++;
        return true;
    }

    static int seedVehicleStagingMarkers(World w, Rectangle plaza, Random r){
        int made = 0;
        int[][] probes = {
                {plaza.x + plaza.width/2 - 4, plaza.y - 5},
                {plaza.x + plaza.width/2 + 5, plaza.y + plaza.height + 5},
                {plaza.x - 5, plaza.y + plaza.height/2 - 3},
                {plaza.x + plaza.width + 5, plaza.y + plaza.height/2 + 3}
        };
        for(int i=0;i<probes.length;i++){
            int x = probes[i][0], y = probes[i][1];
            if(!w.inBounds(x,y) || roomEnvelopeContains(w, x, y, 0) || w.tiles[x][y] != SIDEWALK) continue;
            if(w.mapObjectAt(x,y) != null) continue;
            RoadTransitFixtureAuthority.VehicleProfile profile = RoadTransitFixtureAuthority.plazaStagingProfile(i);
            MapObjectState m = RoadTransitFixtureAuthority.newVehicleFixture(w, x, y, profile, r, "ROAD-VEHICLE-STAGE");
            w.mapObjects.add(m);
            made++;
        }
        return made;
    }}
