package mechanist;

import java.awt.*;
import java.util.*;

/**
 * Road-adjacent civic structure layer.
 *
 * Vehicles, taxis, and recall anchors are represented as protected fixture
 * records so the zone language remains readable without enabling vehicle
 * movement, seating, armor, or weapon systems.
 */
final class RoadAdjacencyIntegrationAuthority {
    static final String VERSION = "0.9.10iq";

    static final class Result {
        int sidewalkCorridorTiles;
        int alcoves;
        int alleyCutThroughs;
        int parkingLots;
        int parkingTiles;
        int taxiBooths;
        int vehicleHooks;
        int parkSpaces;
        int parkFixtures;
        int preservedSpecials;
        int skippedRooms;
        String layer = "road-adjacent civic structure layer";
        String summary(){
            return "layer=" + layer +
                    " sidewalkCorridorTiles=" + sidewalkCorridorTiles +
                    " alcoves=" + alcoves +
                    " alleyCutThroughs=" + alleyCutThroughs +
                    " parkingLots=" + parkingLots +
                    " parkingTiles=" + parkingTiles +
                    " taxiBooths=" + taxiBooths +
                    " vehicleHooks=" + vehicleHooks +
                    " parkSpaces=" + parkSpaces +
                    " parkFixtures=" + parkFixtures +
                    " skippedRooms=" + skippedRooms +
                    " preservedSpecials=" + preservedSpecials +
                    " rule=roads gain sidewalks-as-corridors, alcoves, alley cut-throughs, parking/taxi hooks, and park-open-space hooks";
        }
    }

    private RoadAdjacencyIntegrationAuthority() {}

    static Result apply(World w, Random r){
        Result res = new Result();
        if(w == null) return res;
        if(r == null) r = new Random(w.seed ^ 0x9080A11EL);

        res.sidewalkCorridorTiles = countSidewalks(w);
        res.alcoves += seedRoadAlcoves(w, r, res, 10);
        res.alleyCutThroughs += carveFootTrafficCutThroughs(w, r, res, 8);
        res.parkingLots += seedParkingLots(w, r, res, 5);
        res.parkSpaces += seedParkOpenSpaces(w, r, res, 3);
        res.taxiBooths += seedTaxiBooths(w, r, res, 3);
        return res;
    }

    static int countSidewalks(World w){
        int n = 0;
        for(int x=0;x<w.w;x++) for(int y=0;y<w.h;y++) if(w.tiles[x][y] == RoadGridIntegrationAuthority.SIDEWALK) n++;
        return n;
    }

    static int seedRoadAlcoves(World w, Random r, Result res, int max){
        ArrayList<Point> candidates = sidewalkCandidates(w);
        Collections.shuffle(candidates, r);
        int made = 0;
        for(Point p: candidates){
            if(made >= max) break;
            int[] dir = outwardWallDirection(w, p.x, p.y);
            if(dir == null) continue;
            int carved = carveRectFacing(w, p.x + dir[0], p.y + dir[1], dir, 3, 2, ',', res);
            if(carved >= 3){
                made++;
                placeRoadFixture(w, p.x + dir[0], p.y + dir[1], AssetIntegrationDisciplineAuthority.ROAD_ALCOVE_FIXTURE, 'N', r, res);
            }
        }
        return made;
    }

    static int carveFootTrafficCutThroughs(World w, Random r, Result res, int max){
        ArrayList<Point> candidates = sidewalkCandidates(w);
        Collections.shuffle(candidates, r);
        int made = 0;
        for(Point p: candidates){
            if(made >= max) break;
            int[] dir = outwardWallDirection(w, p.x, p.y);
            if(dir == null) continue;
            int len = 4 + Math.floorMod(r.nextInt(), 7);
            int carved = 0;
            int x = p.x + dir[0], y = p.y + dir[1];
            for(int i=0;i<len && w.inBounds(x,y);i++,x+=dir[0],y+=dir[1]){
                if(RoadGridIntegrationAuthority.roomEnvelopeContains(w, x, y, 0)){ res.skippedRooms++; break; }
                char old = w.tiles[x][y];
                if(isProtected(old, w)){ res.preservedSpecials++; break; }
                if(old == RoadGridIntegrationAuthority.SIDEWALK || old == RoadGridIntegrationAuthority.ROAD_LANE || w.isCorridorGlyph(old)) break;
                if(old != '#') break;
                w.tiles[x][y] = ',';
                carved++;
            }
            if(carved >= 3) made++;
        }
        return made;
    }

    static int seedParkingLots(World w, Random r, Result res, int max){
        ArrayList<Point> candidates = sidewalkCandidates(w);
        Collections.shuffle(candidates, r);
        int made = 0;
        for(Point p: candidates){
            if(made >= max) break;
            int[] dir = outwardWallDirection(w, p.x, p.y);
            if(dir == null) continue;
            int width = 6, depth = 4;
            ArrayList<Point> cells = rectCellsFacing(w, p.x + dir[0], p.y + dir[1], dir, width, depth);
            if(cells.size() < width * depth) continue;
            boolean ok = true;
            for(Point c: cells){
                if(!canConvertOpenService(w, c.x, c.y)){ ok = false; break; }
            }
            if(!ok) continue;
            for(Point c: cells){ w.tiles[c.x][c.y] = (r.nextInt(100) < 35 ? '_' : '.'); res.parkingTiles++; }
            int cx = averageX(cells), cy = averageY(cells);
            placeRoadFixture(w, cx, cy, AssetIntegrationDisciplineAuthority.PARKING_LOT_MARKER, 'N', r, res);
            seedVehicleProfileInLot(w, cells, r, res);
            made++;
        }
        return made;
    }

    static int seedParkOpenSpaces(World w, Random r, Result res, int max){
        ArrayList<Point> candidates = sidewalkCandidates(w);
        Collections.shuffle(candidates, r);
        int made = 0;
        for(Point p: candidates){
            if(made >= max) break;
            int[] dir = outwardWallDirection(w, p.x, p.y);
            if(dir == null) continue;
            int width = 7, depth = 5;
            ArrayList<Point> cells = rectCellsFacing(w, p.x + dir[0], p.y + dir[1], dir, width, depth);
            if(cells.size() < width * depth) continue;
            boolean ok = true;
            for(Point c: cells){ if(!canConvertOpenService(w,c.x,c.y)){ ok=false; break; } }
            if(!ok) continue;
            for(Point c: cells) w.tiles[c.x][c.y] = (r.nextInt(100) < 25 ? ',' : '.');
            Point fixture = cells.get(Math.floorMod(r.nextInt(), cells.size()));
            placeRoadFixture(w, fixture.x, fixture.y, AssetIntegrationDisciplineAuthority.PARK_OPEN_SPACE, 'N', r, res);
            res.parkFixtures++;
            made++;
        }
        return made;
    }

    static int seedTaxiBooths(World w, Random r, Result res, int max){
        ArrayList<Point> candidates = sidewalkCandidates(w);
        Collections.shuffle(candidates, r);
        int made = 0;
        for(Point p: candidates){
            if(made >= max) break;
            if(w.mapObjectAt(p.x,p.y) != null) continue;
            boolean nearParking = false;
            for(MapObjectState m: w.mapObjects){
                if(m != null && AssetIntegrationDisciplineAuthority.PARKING_LOT_MARKER.equals(AssetIntegrationDisciplineAuthority.canonicalType(m.type)) && Math.abs(m.x-p.x)+Math.abs(m.y-p.y) <= 10){ nearParking = true; break; }
            }
            if(!nearParking && made > 0) continue;
            placeRoadFixture(w, p.x, p.y, AssetIntegrationDisciplineAuthority.TAXI_BOOTH, 'T', r, res);
            made++;
        }
        return made;
    }

    static ArrayList<Point> sidewalkCandidates(World w){
        ArrayList<Point> out = new ArrayList<>();
        for(int x=2;x<w.w-2;x++) for(int y=2;y<w.h-2;y++){
            if(w.tiles[x][y] == RoadGridIntegrationAuthority.SIDEWALK && !RoadGridIntegrationAuthority.roomEnvelopeContains(w, x, y, 0) && w.mapObjectAt(x,y) == null) out.add(new Point(x,y));
        }
        return out;
    }

    static int[] outwardWallDirection(World w, int x, int y){
        int[][] dirs = {{1,0},{-1,0},{0,1},{0,-1}};
        for(int[] d: dirs){
            int ax=x+d[0], ay=y+d[1];
            int bx=x-d[0], by=y-d[1];
            if(w.inBounds(ax,ay) && w.inBounds(bx,by) && w.tiles[ax][ay] == '#' && (w.tiles[bx][by] == RoadGridIntegrationAuthority.ROAD_LANE || w.tiles[bx][by] == RoadGridIntegrationAuthority.SIDEWALK)) return d;
        }
        return null;
    }

    static int carveRectFacing(World w, int sx, int sy, int[] dir, int width, int depth, char glyph, Result res){
        ArrayList<Point> cells = rectCellsFacing(w, sx, sy, dir, width, depth);
        if(cells.size() < width * depth) return 0;
        for(Point c: cells) if(!canConvertOpenService(w,c.x,c.y)) return 0;
        for(Point c: cells) w.tiles[c.x][c.y] = glyph;
        return cells.size();
    }

    static ArrayList<Point> rectCellsFacing(World w, int sx, int sy, int[] dir, int width, int depth){
        ArrayList<Point> cells = new ArrayList<>();
        int px = dir[1], py = -dir[0];
        int halfA = -(width/2);
        for(int d=0; d<depth; d++){
            for(int a=halfA; a<halfA+width; a++){
                int x = sx + dir[0]*d + px*a;
                int y = sy + dir[1]*d + py*a;
                if(!w.inBounds(x,y)) continue;
                cells.add(new Point(x,y));
            }
        }
        return cells;
    }

    static boolean canConvertOpenService(World w, int x, int y){
        if(!w.inBounds(x,y)) return false;
        if(RoadGridIntegrationAuthority.roomEnvelopeContains(w, x, y, 0)) return false;
        char old = w.tiles[x][y];
        if(isProtected(old, w)) return false;
        return old == '#' || old == ',' || old == '.' || old == RoadGridIntegrationAuthority.SIDEWALK;
    }

    static boolean isProtected(char old, World w){
        return old == 'D' || old == 'S' || old == 'v' || old == 'E' || (w != null && w.isDoorSymbol(old));
    }

    static void placeRoadFixture(World w, int x, int y, String type, char glyph, Random r, Result res){
        if(!w.inBounds(x,y) || RoadGridIntegrationAuthority.roomEnvelopeContains(w, x, y, 0) || w.mapObjectAt(x,y) != null) return;
        MapObjectState m = RoadTransitFixtureAuthority.newFixture(w, x, y, type, glyph, r, "ROAD-ADJ");
        w.mapObjects.add(m);
    }

    static void seedVehicleProfileInLot(World w, ArrayList<Point> cells, Random r, Result res){
        if(cells == null || cells.isEmpty()) return;
        Collections.shuffle(cells, r);
        int count = 1 + Math.floorMod(r.nextInt(), 2);
        for(int i=0; i<Math.min(count, cells.size()); i++){
            Point p = cells.get(i);
            if(w.mapObjectAt(p.x,p.y) != null) continue;
            RoadTransitFixtureAuthority.VehicleProfile profile = RoadTransitFixtureAuthority.chooseVehicleProfile(r, w == null ? null : w.zoneType);
            MapObjectState m = RoadTransitFixtureAuthority.newVehicleFixture(w, p.x, p.y, profile, r, "VEHICLE-HOOK");
            w.mapObjects.add(m);
            res.vehicleHooks++;
        }
    }

    static int averageX(ArrayList<Point> cells){ int s=0; for(Point p: cells) s += p.x; return cells.isEmpty()?0:s/cells.size(); }
    static int averageY(ArrayList<Point> cells){ int s=0; for(Point p: cells) s += p.y; return cells.isEmpty()?0:s/cells.size(); }}
