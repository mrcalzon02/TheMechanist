package mechanist;

import java.awt.*;
import java.util.*;

/**
 * Road-frontage fixture generation.
 *
 * Places civic map-object fixtures on sidewalks, alleys, and road-adjacent
 * service cells so districts read as usable public space. The fixtures are
 * semantic map objects with current inspection/search behavior owned by the
 * interaction authorities.
 */
final class RoadFrontageFixtureAuthority {
    static final String VERSION = "0.9.10je";

    static final class Result {
        int scannedSidewalks;
        int trashBins;
        int wasteReceptacles;
        int binClusters;
        int oldPaperSources;
        int innVending;
        int publicBenches;
        int pictScreens;
        int radios;
        int infoKiosks;
        int serviceCounters;
        int medicaeFronts;
        int barFronts;
        int roadsideLights;
        int imperialShrines;
        int vehicleDealerships;
        int vehiclePartsStores;
        int vehicleServiceGarages;
        int publicTransitStops;
        int factionMotorPools;
        int factionVehicles;
        int skippedOccupied;
        int skippedProtected;
        String layer = "road-frontage fixture generation";
        String summary(){
            return "layer=" + layer +
                    " scannedSidewalks=" + scannedSidewalks +
                    " trashBins=" + trashBins +
                    " wasteReceptacles=" + wasteReceptacles +
                    " binClusters=" + binClusters +
                    " oldPaperSources=" + oldPaperSources +
                    " innVending=" + innVending +
                    " publicBenches=" + publicBenches +
                    " pictScreens=" + pictScreens +
                    " radios=" + radios +
                    " infoKiosks=" + infoKiosks +
                    " serviceCounters=" + serviceCounters +
                    " medicaeFronts=" + medicaeFronts +
                    " barFronts=" + barFronts +
                    " vehicleDealerships=" + vehicleDealerships +
                    " vehiclePartsStores=" + vehiclePartsStores +
                    " vehicleServiceGarages=" + vehicleServiceGarages +
                    " publicTransitStops=" + publicTransitStops +
                    " factionMotorPools=" + factionMotorPools +
                    " factionVehicles=" + factionVehicles +
                    " skippedOccupied=" + skippedOccupied +
                    " skippedProtected=" + skippedProtected +
                    " rule=sidewalks and alleys gain low32 civic frontage fixtures with semantic interaction hooks";
        }
    }

    private RoadFrontageFixtureAuthority() {}

    static Result apply(World w, Random r){
        Result res = new Result();
        if(w == null) return res;
        if(r == null) r = new Random(w.seed ^ 0x9080F00DL);
        ArrayList<Point> sidewalks = frontageCandidates(w);
        res.scannedSidewalks = sidewalks.size();
        Collections.shuffle(sidewalks, r);

        int sidewalkCount = Math.max(1, sidewalks.size());
        int trashTarget = clamp(sidewalkCount / 18, 4, 18);
        int benchTarget = clamp(sidewalkCount / 28, 3, 12);
        int screenTarget = clamp(sidewalkCount / 40, 2, 8);
        int kioskTarget = clamp(sidewalkCount / 55, 1, 6);
        int serviceTarget = clamp(sidewalkCount / 42, 3, 12);
        int medicaeTarget = 3;
        int barTarget = 4;
        int shrineTarget = clamp(sidewalkCount / 34, 5, 22);
        int roadsideLightTarget = clamp(sidewalkCount / 6, 24, 88);

        for(Point p: sidewalks){
            if((res.trashBins + res.wasteReceptacles + res.binClusters) < trashTarget && maybe(35, r) && placeTrashBin(w,p.x,p.y,r,res)) continue;
            if(res.publicBenches < benchTarget && maybe(24, r) && placePublicServiceFixture(w,p.x,p.y,AssetIntegrationDisciplineAuthority.PUBLIC_RATION_BENCH,'N',r,res)) { res.publicBenches++; continue; }
            if(res.pictScreens < screenTarget && maybe(20, r) && placePublicServiceFixture(w,p.x,p.y,AssetIntegrationDisciplineAuthority.PUBLIC_PICT_SCREEN,'N',r,res)) { res.pictScreens++; continue; }
            if(res.radios < screenTarget && maybe(18, r) && placePublicServiceFixture(w,p.x,p.y,AssetIntegrationDisciplineAuthority.CHEAP_RADIO,'N',r,res)) { res.radios++; continue; }
            if(res.infoKiosks < kioskTarget && maybe(16, r) && placePublicServiceFixture(w,p.x,p.y,AssetIntegrationDisciplineAuthority.PUBLIC_INFO_KIOSK,'T',r,res)) { res.infoKiosks++; continue; }
            if(res.serviceCounters < serviceTarget && maybe(12, r) && placePublicServiceFixture(w,p.x,p.y,AssetIntegrationDisciplineAuthority.PUBLIC_SERVICE_COUNTER,'T',r,res)) { res.serviceCounters++; continue; }
        }

        // Vehicle commerce is a street-frontage anchor, so reserve it before dense ambient fixtures.
        VehicleEconomyFrontageAuthority.Result vehicles = VehicleEconomyFrontageAuthority.apply(w, sidewalks, r);
        res.vehicleDealerships = vehicles.dealerships;
        res.vehiclePartsStores = vehicles.partsStores;
        res.vehicleServiceGarages = vehicles.serviceGarages;
        res.publicTransitStops = vehicles.transitStops;
        res.factionMotorPools = vehicles.motorPools;
        res.factionVehicles = vehicles.personalVehicles + vehicles.publicVehicles + vehicles.factionVehicles;

        // Seed scarce anchor fixtures by searching again so deterministic zones with short sidewalks still get visible targets.
        seedSpecific(w, sidewalks, r, res, medicaeTarget, AssetIntegrationDisciplineAuthority.MEDICAE_FRONTAGE, MedicaeFixtureAuthority.frontageLabel(w.zoneType, r), MedicaeFixtureAuthority.frontageStock(RoadGridIntegrationAuthority.SIDEWALK), 'N');
        seedSpecific(w, sidewalks, r, res, barTarget, AssetIntegrationDisciplineAuthority.BAR_FRONTAGE, BarMarketSocialFixtureAuthority.frontageLabel(w.zoneType, r), BarMarketSocialFixtureAuthority.frontageStock(RoadGridIntegrationAuthority.SIDEWALK), 'N');
        seedInnVending(w, sidewalks, r, res, clamp(sidewalkCount / 14, 8, 34));
        seedImperialShrines(w, sidewalks, r, res, shrineTarget);
        seedRoadsideLights(w, sidewalks, r, res, roadsideLightTarget);
        return res;
    }

    static ArrayList<Point> frontageCandidates(World w){
        ArrayList<Point> out = new ArrayList<>();
        for(int x=2; x<w.w-2; x++) for(int y=2; y<w.h-2; y++){
            char t = w.tiles[x][y];
            if(t != RoadGridIntegrationAuthority.SIDEWALK && t != ',') continue;
            if(RoadGridIntegrationAuthority.roomEnvelopeContains(w, x, y, 0)) continue;
            if(w.mapObjectAt(x,y) != null) continue;
            if(w.isDoorAccessReservedForObject(x,y)) continue;
            if(hasNearbyMapObject(w,x,y,2)) continue;
            out.add(new Point(x,y));
        }
        return out;
    }

    static boolean placePublicServiceFixture(World w, int x, int y, String type, char glyph, Random r, Result res){
        char under = (w != null && w.inBounds(x,y)) ? w.tiles[x][y] : '.';
        String label = PublicServiceMediaAuthority.frontageLabel(type, w == null ? null : w.zoneType, r);
        String stock = PublicServiceMediaAuthority.frontageStock(type, under);
        return placeSimple(w, x, y, type, label, stock, glyph, res);
    }

    static boolean placeTrashBin(World w, int x, int y, Random r, Result res){
        String type = WasteNewsprintScavengeAuthority.chooseFrontageContainerType(w == null ? null : w.zoneType, r);
        String label = WasteNewsprintScavengeAuthority.frontageLabel(type, w == null ? null : w.zoneType, r);
        char under = (w != null && w.inBounds(x,y)) ? w.tiles[x][y] : '.';
        String stock = WasteNewsprintScavengeAuthority.frontageStock(type, under);
        if(!placeSimple(w,x,y,type,label,stock,'N',res)) return false;
        String canonical = AssetIntegrationDisciplineAuthority.canonicalType(type);
        if(AssetIntegrationDisciplineAuthority.PUBLIC_SMALL_BIN_CLUSTER.equals(canonical)) res.binClusters++;
        else if(AssetIntegrationDisciplineAuthority.PUBLIC_WASTE_RECEPTACLE.equals(canonical)) res.wasteReceptacles++;
        else res.trashBins++;
        if(res.oldPaperSources < Math.max(2, (res.trashBins + res.wasteReceptacles + res.binClusters)/3)) res.oldPaperSources++;
        return true;
    }

    static void seedInnVending(World w, ArrayList<Point> candidates, Random r, Result res, int target){
        Collections.shuffle(candidates, r);
        for(Point p: candidates){
            if(res.innVending >= target) break;
            if(w.mapObjectAt(p.x,p.y) != null || w.isDoorAccessReservedForObject(p.x,p.y) || hasNearbyMapObject(w,p.x,p.y,3)) continue;
            char under = w.tiles[p.x][p.y];
            MapObjectState m = MapObjectState.newsVending(p.x,p.y,w.zoneType,under);
            m.stockState = MapObjectState.setStockFlag(m.stockState, "layer", "road-frontage");
            m.stockState = MapObjectState.setStockFlag(m.stockState, "semantic", "feature_newspaper_vending");
            m.stockState = MapObjectState.setStockFlag(m.stockState, "service", "read-buy-rumor-source");
            w.mapObjects.add(m);
            res.innVending++;
        }
    }


    static void seedImperialShrines(World w, ArrayList<Point> candidates, Random r, Result res, int target){
        Collections.shuffle(candidates, r);
        for(Point p: candidates){
            if(res.imperialShrines >= target) break;
            if(w.mapObjectAt(p.x,p.y) != null || w.isDoorAccessReservedForObject(p.x,p.y) || hasNearbyMapObject(w,p.x,p.y,3)) continue;
            if(!nearRoadOrAlcove(w,p.x,p.y)) continue;
            char under = w.tiles[p.x][p.y];
            MapObjectState m = MapObjectState.shrine(p.x, p.y, 'I', w.zoneType);
            m.stockState = m.stockState + ";layer=road-frontage;under=" + (int)under + ";semantic=imperial_shrine";
            w.mapObjects.add(m);
            res.imperialShrines++;
        }
    }

    static void seedRoadsideLights(World w, ArrayList<Point> candidates, Random r, Result res, int target){
        Collections.shuffle(candidates, r);
        for(Point p: candidates){
            if(res.roadsideLights >= target) break;
            if(w.mapObjectAt(p.x,p.y) != null || w.isDoorAccessReservedForObject(p.x,p.y) || hasNearbyMapObject(w,p.x,p.y,2)) continue;
            if(!nearRoadOrAlcove(w,p.x,p.y)) continue;
            char under = w.tiles[p.x][p.y];
            String group = "RL" + Math.abs(Objects.hash(w.seed, p.x, p.y, "roadside-light"));
            ZoneLightSourceRecord l = new ZoneLightSourceRecord("ZL-ROAD-" + Math.abs(Objects.hash(w.seed,p.x,p.y)), "roadside lumen maintained", p.x, p.y, -1, 5 + r.nextInt(3), 56 + r.nextInt(30), "warm white", true, true, false, 99, 0, false, group);
            w.lightSources.add(l);
            MapObjectState m = MapObjectState.lightFixture(l, w.zoneType, under);
            m.stockState = m.stockState + ";layer=road-frontage;under=" + (int)under + ";semantic=roadside_lumen";
            w.mapObjects.add(m);
            res.roadsideLights++;
        }
    }

    static void seedSpecific(World w, ArrayList<Point> candidates, Random r, Result res, int target, String type, String label, String stock, char glyph){
        Collections.shuffle(candidates, r);
        int made = 0;
        for(Point p: candidates){
            if(made >= target) break;
            if(w.mapObjectAt(p.x,p.y) != null || w.isDoorAccessReservedForObject(p.x,p.y) || hasNearbyMapObject(w,p.x,p.y,4)) continue;
            if(!nearRoadOrAlcove(w,p.x,p.y)) continue;
            if(placeSimple(w,p.x,p.y,type,label,stock,glyph,res)){
                made++;
                if(type.startsWith("medicae")) res.medicaeFronts++;
                else if(type.startsWith("bar")) res.barFronts++;
            }
        }
    }

    static boolean placeSimple(World w, int x, int y, String type, String label, String stock, char glyph, Result res){
        if(!w.inBounds(x,y)) return false;
        if(RoadGridIntegrationAuthority.roomEnvelopeContains(w, x, y, 0)){ res.skippedProtected++; return false; }
        if(w.mapObjectAt(x,y) != null){ res.skippedOccupied++; return false; }
        if(w.isDoorAccessReservedForObject(x,y)){ res.skippedProtected++; return false; }
        char under = w.tiles[x][y];
        if(under == 'D' || under == 'S' || under == 'v' || under == 'E' || w.isDoorSymbol(under)){ res.skippedProtected++; return false; }
        MapObjectState m = new MapObjectState();
        m.x=x; m.y=y; m.glyph=glyph; m.type=type;
        m.label=label + " / " + (w.zoneType==null?"Unknown zone":w.zoneType.label);
        m.stockState=stock + ";under=" + (int)under;
        m.cooldownUntilTurn=0; m.vendCount=0;
        m.id="FRONTAGE-" + Math.abs(Objects.hash(type,w.locationKey(),x,y));
        w.mapObjects.add(m);
        return true;
    }

    static boolean nearRoadOrAlcove(World w, int x, int y){
        for(int dx=-2; dx<=2; dx++) for(int dy=-2; dy<=2; dy++){
            if(Math.abs(dx)+Math.abs(dy) > 3) continue;
            int nx=x+dx, ny=y+dy;
            if(!w.inBounds(nx,ny)) continue;
            char t=w.tiles[nx][ny];
            if(t == RoadGridIntegrationAuthority.ROAD_LANE || t == RoadGridIntegrationAuthority.SIDEWALK || t == ',') return true;
        }
        return false;
    }

    static boolean hasNearbyMapObject(World w, int x, int y, int radius){
        if(w.mapObjects == null) return false;
        for(MapObjectState m: w.mapObjects){
            if(m == null) continue;
            if(Math.abs(m.x-x)+Math.abs(m.y-y) <= radius) return true;
        }
        return false;
    }

    static boolean maybe(int pct, Random r){ return Math.floorMod(r.nextInt(),100) < pct; }
    static int clamp(int v, int lo, int hi){ return Math.max(lo, Math.min(hi, v)); }}
