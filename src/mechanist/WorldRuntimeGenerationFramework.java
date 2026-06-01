package mechanist;

import java.awt.*;
import java.util.*;

class WorldAtlas {
    final long seed;
    final HiveWorldDefinition hiveWorld;
    final World[][][][][] slices = new World[3][3][3][3][20]; // sector x/y, zone x/y, layer index 0..19; even=floor, odd=sewer
    int sectorX=1, sectorY=1, zoneX=2, zoneY=2, floor=4; boolean sewer=false;
    WorldAtlas(long seed){ this(seed, WorldSetupSettings.standard()); }
    WorldAtlas(long seed, WorldSetupSettings settings){
        this.seed=seed;
        WorldSetupSettings use = settings == null ? WorldSetupSettings.standard() : settings.copy();
        WorldGenerationApi.setActiveSettings(use);
        this.hiveWorld=CampaignWorldApi.loadOrCreate(seed, use);
        int batches = Math.max(1, use.simulationBatches());
        for(int i=0;i<batches;i++){
            WorldHistoryApi.advanceFactionControlEpochs(this.hiveWorld, WorldHistoryApi.DEFAULT_BATCH);
            ZoneFacilityHistoryApi.advanceFacilityHistory(this.hiveWorld, ZoneFacilityHistoryApi.DEFAULT_BATCH);
            ProductionFacilityOutputSimulationApi.advanceProductionOutput(this.hiveWorld, ProductionFacilityOutputSimulationApi.DEFAULT_BATCH);
            ProductionDistributionApi.advanceStockMovements(this.hiveWorld, ProductionDistributionApi.DEFAULT_BATCH);
            HistoricalConflictLossApi.advanceConflictLoss(this.hiveWorld, HistoricalConflictLossApi.DEFAULT_BATCH);
            HistoricalItemMaterializationApi.advanceHistoricalItemMaterialization(this.hiveWorld, HistoricalItemMaterializationApi.DEFAULT_BATCH);
            PopulationWorkAssignmentApi.advanceLaborAssignments(this.hiveWorld, PopulationWorkAssignmentApi.DEFAULT_BATCH);
        }
        CampaignWorldApi.saveWorldDefinition(this.hiveWorld);
    }
    void generateScaffold(){
        // 0.8.19: Do not synchronously pregenerate the entire 3x3x3x3x10x2 atlas during
        // new-game loading. The structured plaza generator is heavier now; building all 162
        // slices up front can look like a hard freeze on older Linux test hardware. Generate
        // the insertion slice immediately and leave all other slices lazy through currentWorld().
        int generated = createSlice(sectorX, sectorY, zoneX, zoneY, floor, sewer);
        DebugLog.audit("WORLD_ATLAS", "lazyScaffold=true generatedInsertionSlices=" + generated + " seed=" + seed + " arcology=" + hiveWorld.hiveName + " start=floor4 zone2,2 sector1,1");
    }
    int createSlice(int sx,int sy,int zx,int zy,int fl,boolean sewerLayer){
        long s = seed ^ (sx*1000003L) ^ (sy*9176L) ^ (zx*131071L) ^ (zy*524287L) ^ (fl*8191L) ^ (sewerLayer?0xBEEFL:0xFACE);
        Dimension sliceSize = WorldGenerationApi.zoneSliceSize(s);
        World w = new World(s, sliceSize.width, sliceSize.height);
        w.sectorX=sx; w.sectorY=sy; w.zoneX=zx; w.zoneY=zy; w.floor=fl; w.sewerLayer=sewerLayer;
        w.hiveName = hiveWorld.hiveName; w.sectorName = hiveWorld.sectorName(sx, sy); w.zoneName = hiveWorld.zoneName(sx, sy, zx, zy, fl, sewerLayer); w.zoneHistory = hiveWorld.historyLine(sx, sy, zx, zy, fl, sewerLayer); w.zoneEpochHistory = WorldHistoryApi.ensureZoneEpoch(hiveWorld, hiveWorld.zoneKey(sx, sy, zx, zy, fl, sewerLayer)); w.zoneFacilityHistory = ZoneFacilityHistoryApi.ensureZoneFacilities(hiveWorld, hiveWorld.zoneKey(sx, sy, zx, zy, fl, sewerLayer)); w.zoneProductionHistory = ProductionFacilityOutputSimulationApi.ensureZoneProduction(hiveWorld, hiveWorld.zoneKey(sx, sy, zx, zy, fl, sewerLayer)); w.zoneStockMovementHistory = ProductionDistributionApi.ensureZoneStockMovements(hiveWorld, hiveWorld.zoneKey(sx, sy, zx, zy, fl, sewerLayer)); w.zoneConflictLossHistory = HistoricalConflictLossApi.ensureZoneConflictLoss(hiveWorld, hiveWorld.zoneKey(sx, sy, zx, zy, fl, sewerLayer)); w.zoneMaterializedItemHistory = HistoricalItemMaterializationApi.ensureZoneMaterializedItems(hiveWorld, hiveWorld.zoneKey(sx, sy, zx, zy, fl, sewerLayer)); w.zoneLaborAssignmentHistory = PopulationWorkAssignmentApi.ensureZoneLaborAssignments(hiveWorld, hiveWorld.zoneKey(sx, sy, zx, zy, fl, sewerLayer));
        w.zoneType = zoneTypeForSlice(zx, zy, fl, sewerLayer);
        if(sx==1&&sy==1&&zx==2&&zy==2&&fl==4&&!sewerLayer) w.zoneType = ZoneType.NEUTRAL_CIVILIAN_FLOOR;
        w.generate();
        slices[sx-1][sy-1][zx-1][zy-1][(fl-1)*2+(sewerLayer?1:0)] = w;
        WorldGenerationProgressApi.markSliceGenerated(hiveWorld, 1);
        CampaignWorldApi.saveWorldDefinition(hiveWorld);
        DebugLog.audit("WORLDGEN_PROGRESS", String.join(" | ", WorldGenerationProgressApi.progressLines(hiveWorld)));
        return 1;
    }
    static ZoneType zoneTypeForSlice(int zx, int zy, int fl, boolean sewerLayer){
        if(sewerLayer) {
            // Sewer layers are not always merely pipes. The deeper the sector stack,
            // the more often they become mutant or cultist encampment-sewers.
            // Floor 1B is worst; Floor 10B is usually maintained conduit.
            int depth = 11 - Math.max(1, Math.min(10, fl));
            int roll = Math.floorMod(zx*37 + zy*53 + fl*91, 100);
            int danger = Math.min(70, 8 + depth * 7);
            if(roll < danger / 2) return ZoneType.MUTANT_SEWER_CAMP;
            if(roll < danger) return ZoneType.CULTIST_SEWER_CAMP;
            return ZoneType.SEWER_CONDUIT;
        }
        if(fl == 1) return ((zx + zy) % 2 == 0) ? ZoneType.MUTANT_WARRENS : ZoneType.TRASH_WARREN;
        if(fl >= 2 && fl <= 4) return ((zx + zy + fl) % 3 == 0) ? ZoneType.HAB_STACK : ZoneType.GANGER_TURF;
        if(fl == 4 && zx == 2 && zy == 2) return ZoneType.NEUTRAL_CIVILIAN_FLOOR;
        if(fl == 5 && zx == 2 && zy == 2) return ZoneType.NEUTRAL_RAIL_DEPOT;
        if(fl == 5 && zy == 3) return ZoneType.TRAIN_SERVICE_YARD;
        if(fl == 5) return (zx == 1 ? ZoneType.ADMINISTRATUM_ARCHIVE : (zx == 3 ? ZoneType.ARBITES_PRECINCT_EDGE : ZoneType.SUMP_MARKET));
        if(fl == 10 && zx == 2 && zy == 2) return ZoneType.SECTOR_GOVERNORS_MANSION;
        if(fl >= 6) {
            if(fl == 6 && zx == 1 && zy == 2) return ZoneType.IMPERIAL_NEWS_NETWORK;
            if(zx == 2 && zy == 2) return ZoneType.NOBLE_SERVICE_SPINE;
            if((zx + zy + fl) % 4 == 0) return ZoneType.IMPERIAL_GUARD_BILLET;
            if((zx * 2 + zy + fl) % 5 == 0) return ZoneType.MECHANICUS_RELIC_DUCT;
            if((zx + zy + fl) % 3 == 0) return ZoneType.MECHANICUS_FORGE_CLOISTER;
            return ZoneType.NOBLE_SERVICE_SPINE;
        }
        return ZoneType.HAB_STACK;
    }

    World currentWorld(){
        World w = slices[sectorX-1][sectorY-1][zoneX-1][zoneY-1][(floor-1)*2+(sewer?1:0)];
        if(w == null){ createSlice(sectorX,sectorY,zoneX,zoneY,floor,sewer); w = slices[sectorX-1][sectorY-1][zoneX-1][zoneY-1][(floor-1)*2+(sewer?1:0)]; }
        return w;
    }
    String summary(){ return hiveWorld.hiveName + " — 3x3 sectors / 3x3 zones / 10 floors + sewers; 0.8.90 weapon catalog / underhive armory integration API; current sector="+sectorX+","+sectorY+" ("+hiveWorld.sectorName(sectorX,sectorY)+") zone="+zoneX+","+zoneY+" floor="+floor+(sewer?"B":""); }
}

class World {
    long seed; int w,h; char[][] tiles; CompiledTileDescriptor[][] compiledTileDescriptors; String compiledTileDescriptorSummary = "Tile descriptor compilation has not run."; int[][] roomIds; Random r; ArrayList<Rectangle> rooms = new ArrayList<>();
    ArrayList<NpcEntity> npcs = new ArrayList<>();
    ArrayList<PersonnelReplacementRequest> replacementQueue = new ArrayList<>();
    ArrayList<RoomPopulationLedger> roomPopulationLedgers = new ArrayList<>();
    ArrayList<MapObjectState> mapObjects = new ArrayList<>();
    ArrayList<ZoneLightSourceRecord> lightSources = new ArrayList<>();
    ArrayList<ZoneNoiseSourceRecord> noiseSources = new ArrayList<>();
    ArrayList<EnvironmentalHazardRecord> hazardWarnings = new ArrayList<>();
    ArrayList<TrapRecord> trapRecords = new ArrayList<>();
    int[][] noiseField;
    int noiseFieldTurn = -1;
    int dirtyLightRevision = 1;
    int dirtyNoiseRevision = 1;
    int dirtyVisionRevision = 1;
    int dirtyHazardRevision = 1;
    String hearingFieldSummary = "No cached noise/hearing field generated.";
    String trapInteractionSummary = "No trap / booby-trap interaction metadata generated.";
    String lightNoiseSummary = "No light/noise metadata generated.";
    String hazardVisibilitySummary = "No hazard warning overlays generated.";
    String economicTopologyGenerationSummary = "Economic topology generation bias has not been applied.";
    ArrayList<String> economicTopologyGenerationNotes = new ArrayList<>();
    EconomicLocalTopologyMetadataSurfaceAuthority.Surface localTopologyMetadataSurface = null;
    String localTopologyMetadataSummary = "Local topology metadata surface has not been cached.";
    ArrayList<String> localTopologyMetadataNotes = new ArrayList<>();
    EconomicTopologyReportingOverlayAuthority.Surface economicTopologyReportingOverlaySurface = null;
    String economicTopologyReportingOverlaySummary = "Economic topology reporting overlay has not been built.";
    ArrayList<String> economicTopologyReportingOverlayNotes = new ArrayList<>();
    EconomicTopologyMapIntelBridgeAuthority.Surface economicTopologyMapIntelBridgeSurface = null;
    String economicTopologyMapIntelBridgeSummary = "Economic topology map/intel bridge has not been built.";
    ArrayList<String> economicTopologyMapIntelBridgeNotes = new ArrayList<>();
    RetargetReadinessAuditAuthority.Surface retargetReadinessAuditSurface = null;
    String retargetReadinessAuditSummary = "Retarget readiness audit has not run.";
    ArrayList<String> retargetReadinessAuditNotes = new ArrayList<>();
    ArrayList<RoomProfile> roomProfiles = new ArrayList<>();
    ArrayList<Faction> roomFactions = new ArrayList<>();
    ArrayList<Boolean> roomSpecials = new ArrayList<>();
    ZoneType zoneType = ZoneType.TRASH_WARREN;
    int sectorX = 1, sectorY = 1, zoneX = 1, zoneY = 1, floor = 1;
    boolean sewerLayer = false;
    String hiveName = "Unnamed Arcology", sectorName = "Unnamed Sector", zoneName = "Unnamed Zone", zoneHistory = "No compact history recorded.", zoneEpochHistory = "No faction-control epoch history recorded.", zoneFacilityHistory = "No facility establishment history recorded.", zoneProductionHistory = "No production output history recorded.", zoneStockMovementHistory = "No production distribution / stock movement history recorded.", zoneConflictLossHistory = "No conflict, loss, theft, or abandonment history recorded.", zoneMaterializedItemHistory = "No concrete historical item materialization ledger recorded.", zoneLaborAssignmentHistory = "No population work-assignment ledger recorded.";
    boolean[][] visitedZones = new boolean[3][3];
    World(long seed,int w,int h){this.seed=seed;this.w=w;this.h=h;this.r=new Random(seed);tiles=new char[w][h]; roomIds=new int[w][h]; noiseField=new int[w][h]; for(int x=0;x<w;x++) for(int y=0;y<h;y++) roomIds[x][y]=-1;}
    void generate(){
        // 0.9.10ji PLAZA-FIRST / ROAD-SECOND SECTOR SEED:
        // The plaza is the first physical claim placed in every zone. Roads then grow
        // from that central civic tile mass, while later rooms are bumped down the
        // generation queue and must respect both the plaza and road network.
        int target = 0;
        int attempts = 0;
        resetGenerationState(seed ^ 0x91010ADEL);
        for(int x=0;x<w;x++) for(int y=0;y<h;y++) tiles[x][y]='#';
        SectorGenerationTraceAuthority.record(this, "RESET", "Plaza-first zone substrate filled before central anchor placement.");
        visitedZones[Math.max(0,Math.min(2,zoneX-1))][Math.max(0,Math.min(2,zoneY-1))] = true;

        Rectangle centralPlaza = seedCentralPlazaAnchor();
        SectorGenerationTraceAuthority.record(this, "PLAZA", "Central plaza placed before roads, rooms, fixtures, and transitions.", centralPlaza, false);
        int plazaApron = seedCentralPlazaRoadApron(centralPlaza);
        SectorGenerationTraceAuthority.record(this, "PLAZA", "Central plaza one-tile street apron cut before road placement apronTiles=" + plazaApron + ".", centralPlaza, false);

        RoadGridIntegrationAuthority.Result roadGridResult = RoadGridIntegrationAuthority.apply(this, r);
        int plazaStreetLinks = connectCentralPlazaToStreetGrid(centralPlaza);
        SectorGenerationTraceAuthority.record(this, "ROADS", "Core road grid placed after the central plaza anchor: " + roadGridResult.summary() + " plazaStreetLinks=" + plazaStreetLinks);
        DebugLog.audit("PLAZA_FIRST_ROAD_GRID", roadGridResult.summary() + " plazaStreetLinks=" + plazaStreetLinks);

        decorateCentralPlaza(centralPlaza);
        SectorGenerationTraceAuthority.record(this, "ASSET-SWEEP", "Central plaza decoration sweep completed after roads so plaza remains the sole first generation claim.", centralPlaza, false);

        target = roadFirstRoomTarget();
        attempts = buildRoadFirstRoomLayout(target);
        SectorGenerationTraceAuthority.record(this, "ROOM", "Road-first room seed completed rooms=" + rooms.size() + " target=" + target + " attempts=" + attempts);

        ensureRoomQuotaFromStreetBlocks(target);
        applyFactionRoomManifest();
        SectorGenerationTraceAuthority.record(this, "MANIFEST", "Faction room manifest applied to road-first rooms.");
        markSpecialRooms();
        SectorGenerationTraceAuthority.record(this, "SPECIAL", "Special room flags marked.");
        placeMartianEmergencyMachineRooms();
        SectorGenerationTraceAuthority.record(this, "SPECIAL", "Emergency machine rooms placed where validation allowed.");
        int shellRepairs = normalizeRoomShellsAndCorridorHalos();
        SectorGenerationTraceAuthority.record(this, "ROOM", "Room shell normalization enforced one wall layer and access halos repairs=" + shellRepairs + ".");
        assignRoomFactions();
        SectorGenerationTraceAuthority.record(this, "FACTIONS", "Room faction ownership assigned.");
        seedNeutralContestRooms();
        SectorGenerationTraceAuthority.record(this, "CONTEST", "Neutral contest rooms seeded.");
        EconomicGenerationBiasAuthority.Result economicBiasResult = EconomicGenerationBiasAuthority.apply(this, r);
        DebugLog.audit("ECONOMIC_GENERATION_BIAS", economicBiasResult.summary());
        placeSpecialInReachableRoom(sewerLayer ? 'v' : 'S', 0.18, 0.20);
        placeSpecialInReachableRoom('E', 0.82, 0.20);
        stampFactionRepresentativeBarNearTransit();

        RoadAdjacencyIntegrationAuthority.Result roadAdjacencyResult = RoadAdjacencyIntegrationAuthority.apply(this, r);
        SectorGenerationTraceAuthority.record(this, "ROADS", "Road-adjacent civic structures applied after room placement: " + roadAdjacencyResult.summary());
        DebugLog.audit("ROAD_ADJACENCY_FOUNDATION", roadAdjacencyResult.summary());
        RoadFrontageFixtureAuthority.Result roadFrontageResult = RoadFrontageFixtureAuthority.apply(this, r);
        SectorGenerationTraceAuthority.record(this, "FRONTAGE", "Roadside fixtures/lights/shrines/vending sweep applied: " + roadFrontageResult.summary());
        DebugLog.audit("ROAD_FRONTAGE_FIXTURES", roadFrontageResult.summary());
        RoomFixtureInteractionAuthority.Result roomFixtureResult = RoomFixtureInteractionAuthority.apply(this, r);
        SectorGenerationTraceAuthority.record(this, "ROOM-ASSETS", "Room fixture interaction sweep applied: " + roomFixtureResult.summary());
        DebugLog.audit("ROOM_FIXTURE_INTERACTIONS", roomFixtureResult.summary());
        validateSpecialTransitions();
        validateWorldgenSelfReport("POST_ROAD_FIRST_TRANSITION_STAMP");
        int repairs = repairWorldgenValidationIssues("POST_ROAD_FIRST_TRANSITION_STAMP");
        int widenedCorridors = widenOneTileCorridors("POST_REPAIR_TWO_WIDE_PREFERENCE");
        if(widenedCorridors > 0) {
            validateSpecialTransitions();
            repairs += repairWorldgenValidationIssues("POST_CORRIDOR_WIDEN");
        }
        if(repairs > 0 || widenedCorridors > 0) validateWorldgenSelfReport("POST_REPAIR");
        if(!allRoomsReachableStrict()) {
            DebugLog.warn("LEVELGEN_POST_VALIDATE", "Road-first validation still found detached content; preserving rooms for Zone Audit trace. seed="+seed+" zone="+zoneType.label);
        }
        validateWorldgenSelfReport("FINAL_PRE_POPULATE");
        applyBoundedOuterHiveWall();
        SectorGenerationTraceAuthority.record(this, "BOUNDARY", "Bounded outer arcology wall/interwall envelope applied.");
        int sectorDoorRepairs = stampRoadMaintenanceTransitionDoors();
        if(sectorDoorRepairs > 0) SectorGenerationTraceAuthority.record(this, "TRANSITIONS", "Road-end double doors stamped against the inner maintenance bulkhead, not map tile edges.");
        int boundaryRepairs = repairUnreachableRooms("POST_BOUNDARY_LAYER");
        if(boundaryRepairs > 0){
            repairs += boundaryRepairs;
            validateWorldgenSelfReport("POST_BOUNDARY_REPAIR");
        }
        applyInterstitialHiveMass();
        SectorGenerationTraceAuthority.record(this, "INTERWALL", "Interstitial arcology mass and hidden features seeded after road-first room validation.");
        PersonnelPopulationApi.ensureLedgers(this, r);
        EnvironmentalHazardVisibilityApi.Result hazardResult = EnvironmentalHazardVisibilityApi.seed(this, r);
        DebugLog.audit("HAZARD_WARNING_OVERLAY", hazardResult.summary());
        TrapInteractionApi.Result trapResult = TrapInteractionApi.seed(this, r);
        DebugLog.audit("TRAP_BOOBYTRAP_FOUNDATION", trapResult.summary());
        EconomicLocalTopologyMetadataSurfaceAuthority.Result localTopologyResult = EconomicLocalTopologyMetadataSurfaceAuthority.apply(this);
        DebugLog.audit("LOCAL_TOPOLOGY_METADATA", localTopologyResult.summary());
        EconomicTopologyReportingOverlayAuthority.Result topologyOverlayResult = EconomicTopologyReportingOverlayAuthority.apply(this);
        DebugLog.audit("ECONOMIC_TOPOLOGY_REPORTING_OVERLAY", topologyOverlayResult.summary());
        EconomicTopologyMapIntelBridgeAuthority.Result mapIntelResult = EconomicTopologyMapIntelBridgeAuthority.apply(this);
        DebugLog.audit("ECONOMIC_TOPOLOGY_MAP_INTEL_BRIDGE", mapIntelResult.summary());
        RetargetReadinessAuditAuthority.Result retargetResult = RetargetReadinessAuditAuthority.apply(this);
        DebugLog.audit("RETARGET_READINESS_AUDIT", retargetResult.summary());
        DebugLog.audit("WORLDGEN_ACCEPTANCE", "status=" + (repairs>0 ? "REPAIRED" : "ACCEPTED") + " repairs="+repairs+" rooms="+rooms.size()+" zone="+zoneType.label+" layer="+layerText()+" seed="+seed);
        populate();
        SectorGenerationTraceAuthority.record(this, "POPULATION", "Population, resident amenities, and ordinary entities seeded after road-first terrain/facility layout.");
        int doorwayObjectRepairs = 0;
        for(int pass=0; pass<4; pass++){
            int fixed = sanitizeGeneratedObjectDoorAccess();
            doorwayObjectRepairs += fixed;
            if(fixed == 0) break;
        }
        if(doorwayObjectRepairs > 0) DebugLog.audit("OBJECT_DOORWAY_CLEARANCE_REPAIR", "relocatedOrRemoved="+doorwayObjectRepairs+" zone="+zoneType.label+" layer="+layerText()+" seed="+seed);
        TileDataCompilationAuthority.Result tileCompileResult = TileDataCompilationAuthority.compile(this);
        SectorGenerationTraceAuthority.record(this, "TILE-COMPILE", "Tile descriptors compiled for renderer and audit view: " + tileCompileResult.summary());
        DebugLog.audit("TILE_DATA_COMPILE", tileCompileResult.summary());
        DebugLog.log("Generated road-first world seed="+seed+" zone="+zoneType.label+" layer="+layerText()+" rooms="+rooms.size()+" attempts="+attempts+" roads=core-first");
    }

    void applyBoundedOuterHiveWall(){
        // 0.8.79 BOUNDED OUTER ARCOLOGY WALL PASS:
        // After the normal zone body validates, bolt a high-wall maintenance layer onto
        // the exterior of the slice. This adds a maintenance room and rare abandoned
        // interwall rooms outside the ordinary room quota, lays an exterior maintenance
        // corridor loop, and marks true outside-map abyss as void space. The first
        // implementation uses a conservative rectangular envelope for speed and safety;
        // later passes can replace the envelope with closer contour tracing.
        BoundedOuterHiveWallApi.Result result = BoundedOuterHiveWallApi.apply(this, r);
        DebugLog.audit("BOUNDED_HIVEWALL", "zone="+zoneType.label+" layer="+layerText()+" " + result.summary());
    }

    void applyInterstitialHiveMass(){
        // 0.8.77 INTERSTITIAL ARCOLOGY MASS PASS:
        // After rooms, corridors, transitions, validation, and repair are finished, convert deep unused wall
        // mass into readable intra-arcology infrastructure instead of leaving every solid cell as anonymous '#'.
        // One-tile bulkheads immediately bordering walkable space remain '#'; deeper solids become beams,
        // gantries, conveyor ways, pipe bundles, collapsed debris, and buried caches. This must run late so
        // generation/repair code can still use '#' as the carveable wall substrate.
        int converted = InterstitialInfrastructureApi.applyInterstitialMass(this, r);
        int buried = InterstitialInfrastructureApi.seedBuriedFeatures(this, r, 12);
        DebugLog.audit("INTERSTITIAL_HIVE_MASS", "zone="+zoneType.label+" layer="+layerText()+" converted="+converted+" buriedFeatures="+buried+" seed="+seed);
    }

    void resetGenerationState(long effectiveSeed){
        this.r = new Random(effectiveSeed);
        rooms.clear(); npcs.clear(); replacementQueue.clear(); roomPopulationLedgers.clear(); mapObjects.clear(); lightSources.clear(); noiseSources.clear(); hazardWarnings.clear(); trapRecords.clear(); noiseFieldTurn = -1; dirtyLightRevision++; dirtyNoiseRevision++; dirtyVisionRevision++; dirtyHazardRevision++; hearingFieldSummary = "No cached noise/hearing field generated."; lightNoiseSummary = "No light/noise metadata generated."; hazardVisibilitySummary = "No hazard warning overlays generated."; trapInteractionSummary = "No trap / booby-trap interaction metadata generated."; economicTopologyGenerationSummary = "Economic topology generation bias has not been applied."; economicTopologyGenerationNotes.clear(); localTopologyMetadataSurface = null; localTopologyMetadataSummary = "Local topology metadata surface has not been cached."; localTopologyMetadataNotes.clear(); economicTopologyReportingOverlaySurface = null; economicTopologyReportingOverlaySummary = "Economic topology reporting overlay has not been built."; economicTopologyReportingOverlayNotes.clear(); economicTopologyMapIntelBridgeSurface = null; economicTopologyMapIntelBridgeSummary = "Economic topology map/intel bridge has not been built."; economicTopologyMapIntelBridgeNotes.clear(); roomProfiles.clear(); roomFactions.clear(); roomSpecials.clear();
        for(int x=0;x<w;x++) for(int y=0;y<h;y++) roomIds[x][y] = -1;
    }


    Rectangle seedCentralPlazaAnchor(){
        Rectangle plaza = centralPlazaRect();
        carve(plaza);
        rooms.add(plaza);
        if(!roomProfiles.isEmpty()) roomProfiles.set(0, RoomProfile.centralPlaza(zoneType, sewerLayer, r));
        if(!roomFactions.isEmpty()) roomFactions.set(0, plazaFactionForZone());
        if(!roomSpecials.isEmpty()) roomSpecials.set(0, Boolean.TRUE);
        DebugLog.audit("PLAZA_FIRST_ANCHOR", "central plaza room=0 rect="+plaza.x+","+plaza.y+","+plaza.width+"x"+plaza.height+" zone="+zoneType.label+" layer="+layerText()+" seed="+seed);
        return plaza;
    }

    int seedCentralPlazaRoadApron(Rectangle plaza){
        if(plaza == null) return 0;
        int made = 0;
        for(int x=plaza.x-1; x<=plaza.x+plaza.width; x++){
            made += paintPlazaApronTile(x, plaza.y-1);
            made += paintPlazaApronTile(x, plaza.y+plaza.height);
        }
        for(int y=plaza.y; y<plaza.y+plaza.height; y++){
            made += paintPlazaApronTile(plaza.x-1, y);
            made += paintPlazaApronTile(plaza.x+plaza.width, y);
        }
        DebugLog.audit("PLAZA_ROAD_APRON", "apronTiles="+made+" rect="+plaza.x+","+plaza.y+","+plaza.width+"x"+plaza.height+" zone="+zoneType.label+" seed="+seed);
        return made;
    }

    int paintPlazaApronTile(int x, int y){
        if(!inBounds(x,y) || x <= 0 || y <= 0 || x >= w-1 || y >= h-1) return 0;
        if(roomIds[x][y] >= 0) return 0;
        char old = tiles[x][y];
        if(old != '#') return 0;
        tiles[x][y] = RoadGridIntegrationAuthority.SIDEWALK;
        return 1;
    }

    int connectCentralPlazaToStreetGrid(Rectangle plaza){
        if(plaza == null) return 0;
        int made = 0;
        for(int side=0; side<4; side++){
            Point door = centralPlazaDoor(plaza, side);
            if(door == null || !inBounds(door.x, door.y)) continue;
            Point outside = outsideOfDoor(plaza, door);
            if(outside == null || !inBounds(outside.x, outside.y)) continue;
            char out = tiles[outside.x][outside.y];
            if(out == RoadGridIntegrationAuthority.SIDEWALK || out == RoadGridIntegrationAuthority.ROAD_LANE || isCorridorGlyph(out)){
                tiles[door.x][door.y] = DoorType.forZone(zoneType, r).symbol;
                made++;
            }
        }
        DebugLog.audit("PLAZA_STREET_LINK", "doors="+made+" zone="+zoneType.label+" seed="+seed);
        return made;
    }

    int roadFirstRoomTarget(){
        int base = WorldGenerationApi.clampRoomTarget(targetRoomCount());
        int boosted = base * 2;
        boosted += Math.max(10, base / 3);
        if(zoneType == ZoneType.NEUTRAL_CIVILIAN_FLOOR || zoneType == ZoneType.HAB_STACK) boosted += 12;
        if(zoneType == ZoneType.SUMP_MARKET || zoneType == ZoneType.NEUTRAL_RAIL_DEPOT) boosted += 10;
        if(zoneType == ZoneType.IMPERIAL_GUARD_BILLET || zoneType == ZoneType.ADMINISTRATUM_ARCHIVE) boosted += 8;
        int mapLimit = Math.max(48, (w * h) / 150);
        return Math.max(48, Math.min(Math.max(56, mapLimit), boosted));
    }

    int buildRoadFirstRoomLayout(int target){
        int attempts = 0;
        ArrayList<StampedRoomSpec> specs = new ArrayList<>();
        java.util.List<StampedRoomSpec> manifest = RoomManifestApi.requiredRoomManifest(zoneType);
        if(manifest != null) specs.addAll(manifest);
        // A road-first zone needs functional redundancy: habitation, food, logistics,
        // production/work, care, security/social rooms should physically exist, not just
        // be descriptive labels assigned after the fact.
        while(specs.size() < Math.min(target, 18)){
            RoomProfile rp = RoomProfile.forZone(zoneType, r);
            specs.add(new StampedRoomSpec(roomKindFromProfile(rp), rp.name, rp.descriptor, rp.scavengeChance, rp.faction, rp.loot, rp.contents));
        }
        int salt = 0;
        for(StampedRoomSpec spec: specs){
            if(rooms.size() >= target) break;
            attempts += tryPlaceRoadFirstRoom(spec, salt++);
        }
        while(rooms.size() < target && attempts < target * 180){
            RoomProfile rp = RoomProfile.forZone(zoneType, r);
            StampedRoomSpec spec = new StampedRoomSpec(roomKindFromProfile(rp), rp.name, rp.descriptor, rp.scavengeChance, rp.faction, rp.loot, rp.contents);
            attempts += tryPlaceRoadFirstRoom(spec, salt++);
        }
        DebugLog.audit("ROAD_FIRST_ROOMS", "zone="+zoneType.label+" target="+target+" rooms="+rooms.size()+" attempts="+attempts+" rule=roads-before-room-seeding");
        return attempts;
    }

    void ensureRoomQuotaFromStreetBlocks(int target){
        int before = rooms.size();
        int attempts = 0;
        while(rooms.size() < target && attempts++ < target * 120){
            RoomProfile rp = RoomProfile.forZone(zoneType, r);
            StampedRoomSpec spec = new StampedRoomSpec(roomKindFromProfile(rp), rp.name, rp.descriptor, rp.scavengeChance, rp.faction, rp.loot, rp.contents);
            tryPlaceRoadFirstRoom(spec, 10000 + attempts);
        }
        if(rooms.size() < target){
            DebugLog.warn("ROAD_FIRST_ROOM_QUOTA", "zone="+zoneType.label+" target="+target+" rooms="+rooms.size()+" before="+before+" attempts="+attempts+" map may need larger street-block space");
        }
    }

    int tryPlaceRoadFirstRoom(StampedRoomSpec spec, int salt){
        if(spec == null) return 0;
        ArrayList<Point> anchors = roadFirstStreetAnchors();
        if(anchors.isEmpty()) return 0;
        Collections.shuffle(anchors, r);
        Dimension size = roadFirstRoomSize(spec, salt);
        int attempts = 0;
        for(Point street: anchors){
            if(attempts++ > 96) break;
            int[][] dirs = frontageDirectionsForStreet(street);
            if(dirs.length == 0) continue;
            for(int[] dir: dirs){
                Rectangle rr = roadFirstRoomRectFromStreet(street, dir, size.width, size.height);
                if(rr == null) continue;
                if(!rectInMap(rr) || rr.x < 2 || rr.y < 2 || rr.x+rr.width >= w-2 || rr.y+rr.height >= h-2){
                    SectorGenerationTraceAuthority.reject(this, "REJECT", "Road-first room outside safe map bounds.", rr);
                    continue;
                }
                if(regionTouchesNonWall(rr)){
                    SectorGenerationTraceAuthority.reject(this, "REJECT", "Road-first room refused because its own one-wall footprint overlapped a road, corridor, room, transition, or prior asset.", rr);
                    continue;
                }
                int before = rooms.size();
                carve(rr);
                rooms.add(rr);
                RoomProfile rp = spec.toProfile(zoneType, r);
                if(before < roomProfiles.size()) roomProfiles.set(before, rp);
                if(before < roomFactions.size()) roomFactions.set(before, spec.faction == null ? Faction.NONE : spec.faction);
                if(before < roomSpecials.size()) roomSpecials.set(before, Boolean.FALSE);
                if(!connectRoadFirstRoomToStreet(before, rr, street)){
                    SectorGenerationTraceAuthority.reject(this, "REJECT", "Road-first room could not connect to its owning street and was removed.", rr);
                    removeRoomAtIndex(before);
                    continue;
                }
                stampRoomPurposeFeatures(rr, spec);
                SectorGenerationTraceAuthority.record(this, "ROOM", "Road-first " + spec.kind + " room " + before + " placed from street frontage.", rr, false);
                return attempts;
            }
        }
        return attempts;
    }

    ArrayList<Point> roadFirstStreetAnchors(){
        ArrayList<Point> out = new ArrayList<>();
        for(int x=2; x<w-2; x++) for(int y=2; y<h-2; y++){
            char t = tiles[x][y];
            if(t == RoadGridIntegrationAuthority.ROAD_LANE) continue;
            if(t != RoadGridIntegrationAuthority.SIDEWALK && !isCorridorGlyph(t)) continue;
            if(roomIds[x][y] >= 0) continue;
            if(mapObjectAt(x,y) != null) continue;
            if(hasAdjacentWallMass(x,y)) out.add(new Point(x,y));
        }
        return out;
    }

    boolean hasAdjacentWallMass(int x, int y){
        return inBounds(x+1,y) && tiles[x+1][y]=='#' ||
               inBounds(x-1,y) && tiles[x-1][y]=='#' ||
               inBounds(x,y+1) && tiles[x][y+1]=='#' ||
               inBounds(x,y-1) && tiles[x][y-1]=='#';
    }

    int[][] frontageDirectionsForStreet(Point street){
        if(street == null || !inBounds(street.x, street.y)) return new int[0][0];
        ArrayList<int[]> dirs = new ArrayList<>();
        int[][] base = shuffledCardinals();
        for(int[] d: base){
            int nx = street.x + d[0], ny = street.y + d[1];
            if(!inBounds(nx,ny) || tiles[nx][ny] != '#') continue;
            if(roomIds[nx][ny] >= 0) continue;
            char here = tiles[street.x][street.y];
            if(here == RoadGridIntegrationAuthority.SIDEWALK){
                int ox = street.x - d[0], oy = street.y - d[1];
                boolean roadBehind = inBounds(ox,oy) && tiles[ox][oy] == RoadGridIntegrationAuthority.ROAD_LANE;
                boolean streetBeside = inBounds(street.x + (d[1] == 0 ? 0 : 1), street.y + (d[0] == 0 ? 0 : 1));
                if(!roadBehind && !sidewalkRoadLaneBeside(street.x, street.y, d)) continue;
            }
            dirs.add(new int[]{d[0], d[1]});
        }
        return dirs.toArray(new int[dirs.size()][]);
    }

    boolean sidewalkRoadLaneBeside(int x, int y, int[] wallDir){
        if(wallDir == null) return false;
        int[][] probes;
        if(wallDir[0] != 0) probes = new int[][]{{0,1},{0,-1},{-wallDir[0],0}};
        else probes = new int[][]{{1,0},{-1,0},{0,-wallDir[1]}};
        for(int[] p: probes) if(inBounds(x+p[0], y+p[1]) && tiles[x+p[0]][y+p[1]] == RoadGridIntegrationAuthority.ROAD_LANE) return true;
        return false;
    }

    int[][] shuffledCardinals(){
        int[][] dirs = {{1,0},{-1,0},{0,1},{0,-1}};
        for(int i=dirs.length-1;i>0;i--){ int j=r.nextInt(i+1); int[] tmp=dirs[i]; dirs[i]=dirs[j]; dirs[j]=tmp; }
        return dirs;
    }

    Dimension roadFirstRoomSize(StampedRoomSpec spec, int salt){
        String k = spec.kind == null ? "" : spec.kind;
        int rw=5, rh=5;
        if(k.contains("CAFETERIA") || k.contains("WAREHOUSE") || k.contains("FOOD_STORE")){ rw=8; rh=5; }
        else if(k.contains("BARRACKS") || k.contains("DORMITORY")){ rw=7; rh=5; }
        else if(k.contains("APARTMENT") || k.contains("DAYCARE")){ rw=5; rh=5; }
        else if(k.contains("SECURITY") || k.contains("TRAINING")){ rw=7; rh=4; }
        else if(k.contains("LIBRARY") || k.contains("LEARNING")){ rw=6; rh=5; }
        else if(k.contains("CLINIC") || k.contains("SANITATION")){ rw=5; rh=4; }
        else if(k.contains("MACHINERY") || k.contains("WORKSHOP") || k.contains("LOGISTICS")){ rw=7; rh=5; }
        if((salt & 1) == 1){ int t=rw; rw=rh; rh=t; }
        rw = Math.max(4, Math.min(10, rw + r.nextInt(2)));
        rh = Math.max(4, Math.min(8, rh + r.nextInt(2)));
        return new Dimension(rw, rh);
    }

    Rectangle roadFirstRoomRectFromStreet(Point street, int[] dir, int rw, int rh){
        if(street == null || dir == null) return null;
        int gap = 2;
        int x, y;
        if(dir[0] > 0){ x = street.x + gap; y = street.y - rh/2; }
        else if(dir[0] < 0){ x = street.x - gap - rw + 1; y = street.y - rh/2; }
        else if(dir[1] > 0){ x = street.x - rw/2; y = street.y + gap; }
        else { x = street.x - rw/2; y = street.y - gap - rh + 1; }
        return new Rectangle(x, y, rw, rh);
    }

    boolean connectRoadFirstRoomToStreet(int roomIndex, Rectangle rr, Point street){
        if(rr == null || street == null || roomIndex < 0) return false;
        Point door;
        if(street.x < rr.x) door = new Point(rr.x, Math.max(rr.y+1, Math.min(rr.y+rr.height-2, street.y)));
        else if(street.x >= rr.x + rr.width) door = new Point(rr.x+rr.width-1, Math.max(rr.y+1, Math.min(rr.y+rr.height-2, street.y)));
        else if(street.y < rr.y) door = new Point(Math.max(rr.x+1, Math.min(rr.x+rr.width-2, street.x)), rr.y);
        else door = new Point(Math.max(rr.x+1, Math.min(rr.x+rr.width-2, street.x)), rr.y+rr.height-1);
        if(!inBounds(door.x, door.y)) return false;
        Point outside = outsideOfDoor(rr, door);
        if(outside == null || !inBounds(outside.x, outside.y)) return false;
        ArrayList<Point> path = new ArrayList<>();
        int x = outside.x, y = outside.y;
        int guard = 0;
        while(inBounds(x,y) && guard++ < (w+h)){
            if(x == street.x && y == street.y) break;
            path.add(new Point(x,y));
            if(Math.abs(street.x - x) >= Math.abs(street.y - y)) x += Integer.compare(street.x, x);
            else y += Integer.compare(street.y, y);
        }
        if(!inBounds(x,y)) return false;
        if(!(tiles[x][y] == RoadGridIntegrationAuthority.ROAD_LANE || tiles[x][y] == RoadGridIntegrationAuthority.SIDEWALK || isCorridorGlyph(tiles[x][y]))) return false;
        for(Point p: path){
            if(!inBounds(p.x,p.y)) return false;
            if(roomIds[p.x][p.y] >= 0) return false;
            char old = tiles[p.x][p.y];
            if(old != '#') return false;
        }
        char cg = zoneType.corridorGlyph(r);
        tiles[door.x][door.y] = DoorType.forZone(zoneType, r).symbol;
        for(Point p: path) tiles[p.x][p.y] = cg;
        reinforceCorridorWalls(path);
        SectorGenerationTraceAuthority.record(this, "CORRIDOR", "Road-first room corridor linked room " + roomIndex + " to street frontage.", rr, false);
        return true;
    }

    String roomKindFromProfile(RoomProfile rp){
        if(rp == null) return "ROOM";
        String n = ((rp.name == null ? "" : rp.name) + " " + (rp.descriptor == null ? "" : rp.descriptor)).toUpperCase(Locale.ROOT);
        if(n.contains("DORM") || n.contains("BARRACK") || n.contains("SLEEP") || n.contains("BUNK")) return "DORMITORY";
        if(n.contains("CAFETERIA") || n.contains("MESS") || n.contains("CANTEEN") || n.contains("EATING")) return "CAFETERIA";
        if(n.contains("FOOD") || n.contains("RATION") || n.contains("PANTRY")) return "FOOD_STORE";
        if(n.contains("WAREHOUSE") || n.contains("STORAGE") || n.contains("STOREHOUSE")) return "WAREHOUSE";
        if(n.contains("CLINIC") || n.contains("MEDIC") || n.contains("WOUND") || n.contains("CARE")) return "CLINIC";
        if(n.contains("WORKSHOP") || n.contains("MACHINE") || n.contains("FORGE") || n.contains("RELAY")) return "MACHINERY";
        if(n.contains("LIBRARY") || n.contains("LEARNING") || n.contains("ARCHIVE")) return "LIBRARY";
        if(n.contains("SECURITY") || n.contains("CELL") || n.contains("HOLDING")) return "SECURITY";
        if(n.contains("COUNTER") || n.contains("STORE") || n.contains("SHOP")) return "STOREFRONT";
        return "ROOM";
    }

    void stampRoadFirstEdgeDoors(){
        // 0.9.10ji: retained only as a disabled compatibility seam. Sector
        // transitions are no longer stamped on the literal map edge.
    }

    void stampRoadEdgeDoorPair(char edge, int x, int y, int ox, int oy){
        // 0.9.10ji: map-edge transition stamping is prohibited. Use
        // stampRoadMaintenanceTransitionDoors after the maintenance envelope exists.
    }

    int stampRoadMaintenanceTransitionDoors(){
        if(tiles == null || w < 20 || h < 20) return 0;
        int made = 0;
        made += stampTransitionDoorsOnMaintenanceSide('N');
        made += stampTransitionDoorsOnMaintenanceSide('S');
        made += stampTransitionDoorsOnMaintenanceSide('W');
        made += stampTransitionDoorsOnMaintenanceSide('E');
        DebugLog.audit("ROAD_MAINTENANCE_TRANSITIONS", "doors="+made+" rule=sector double doors live at road ends against maintenance-corridor bulkheads, never at map tile edges zone="+zoneType.label+" seed="+seed);
        return made;
    }

    int stampTransitionDoorsOnMaintenanceSide(char side){
        int made = 0;
        if(side == 'N' || side == 'S'){
            int y = side == 'N' ? firstMaintenanceLoopY(true) : firstMaintenanceLoopY(false);
            if(y < 0) return 0;
            int doorY = side == 'N' ? y + 1 : y - 1;
            int roadY = side == 'N' ? doorY + 1 : doorY - 1;
            int bestX = nearestRoadXAtY(roadY, w/2);
            if(bestX >= 1) made += stampHorizontalBulkheadDoorPair(bestX, doorY, side);
        } else {
            int x = side == 'W' ? firstMaintenanceLoopX(true) : firstMaintenanceLoopX(false);
            if(x < 0) return 0;
            int doorX = side == 'W' ? x + 1 : x - 1;
            int roadX = side == 'W' ? doorX + 1 : doorX - 1;
            int bestY = nearestRoadYAtX(roadX, h/2);
            if(bestY >= 1) made += stampVerticalBulkheadDoorPair(doorX, bestY, side);
        }
        return made;
    }

    int firstMaintenanceLoopY(boolean top){
        if(top){ for(int y=1; y<h/2; y++) for(int x=1; x<w-1; x++) if(tiles[x][y] == BoundedOuterHiveWallApi.HIVEWALL_CORRIDOR) return y; }
        else { for(int y=h-2; y>=h/2; y--) for(int x=1; x<w-1; x++) if(tiles[x][y] == BoundedOuterHiveWallApi.HIVEWALL_CORRIDOR) return y; }
        return -1;
    }

    int firstMaintenanceLoopX(boolean left){
        if(left){ for(int x=1; x<w/2; x++) for(int y=1; y<h-1; y++) if(tiles[x][y] == BoundedOuterHiveWallApi.HIVEWALL_CORRIDOR) return x; }
        else { for(int x=w-2; x>=w/2; x--) for(int y=1; y<h-1; y++) if(tiles[x][y] == BoundedOuterHiveWallApi.HIVEWALL_CORRIDOR) return x; }
        return -1;
    }

    int nearestRoadXAtY(int y, int preferredX){
        int best=-1, bestD=Integer.MAX_VALUE;
        if(!inBounds(1, y)) return -1;
        for(int x=2; x<w-2; x++){
            char t = tiles[x][y];
            if(t != RoadGridIntegrationAuthority.ROAD_LANE && t != RoadGridIntegrationAuthority.SIDEWALK && !isCorridorGlyph(t)) continue;
            int d = Math.abs(x - preferredX);
            if(d < bestD){ bestD=d; best=x; }
        }
        return best;
    }

    int nearestRoadYAtX(int x, int preferredY){
        int best=-1, bestD=Integer.MAX_VALUE;
        if(!inBounds(x, 1)) return -1;
        for(int y=2; y<h-2; y++){
            char t = tiles[x][y];
            if(t != RoadGridIntegrationAuthority.ROAD_LANE && t != RoadGridIntegrationAuthority.SIDEWALK && !isCorridorGlyph(t)) continue;
            int d = Math.abs(y - preferredY);
            if(d < bestD){ bestD=d; best=y; }
        }
        return best;
    }

    int stampHorizontalBulkheadDoorPair(int centerX, int y, char side){
        int made = 0;
        for(int dx=0; dx<2; dx++){
            int x = Math.max(1, Math.min(w-2, centerX + dx));
            if(!inBounds(x,y) || roomIds[x][y] >= 0) continue;
            tiles[x][y] = 'D';
            made++;
        }
        return made;
    }

    int stampVerticalBulkheadDoorPair(int x, int centerY, char side){
        int made = 0;
        for(int dy=0; dy<2; dy++){
            int y = Math.max(1, Math.min(h-2, centerY + dy));
            if(!inBounds(x,y) || roomIds[x][y] >= 0) continue;
            tiles[x][y] = 'D';
            made++;
        }
        return made;
    }

    int targetRoomCount(){
        // 0.8.66 API sectioning: room-count policy is now delegated to the
        // world-generation scale surface. The current profile intentionally preserves
        // the 0.8.61 dense-zone numbers as the minimum reserved scaling tier.
        return WorldGenerationApi.targetRoomCount(zoneType, r);
    }

    int buildCentralPlazaLayout(int target){
        target = WorldGenerationApi.clampRoomTarget(target);
        Rectangle plaza = centralPlazaRect();
        carve(plaza);
        rooms.add(plaza);
        SectorGenerationTraceAuthority.record(this, "ROOM", "Central plaza carved as room 0.", plaza, false);
        if(!roomProfiles.isEmpty()) roomProfiles.set(0, RoomProfile.centralPlaza(zoneType, sewerLayer, r));
        if(!roomFactions.isEmpty()) roomFactions.set(0, plazaFactionForZone());
        if(!roomSpecials.isEmpty()) roomSpecials.set(0, Boolean.TRUE);
        decorateCentralPlaza(plaza);
        SectorGenerationTraceAuthority.record(this, "ASSET-SWEEP", "Central plaza decoration sweep completed.", plaza, false);
        stampCultImperialisTempleNearPlaza(plaza);
        SectorGenerationTraceAuthority.record(this, "ROOM", "Cult Imperialis temple module attempted near plaza.", plaza, false);
        seedCompassPlazaCorridors(plaza);
        SectorGenerationTraceAuthority.record(this, "CORRIDOR", "Compass plaza corridors seeded.", plaza, false);
        stampStructuredZoneModules(plaza, target);
        SectorGenerationTraceAuthority.record(this, "ROOM", "Structured zone modules attempted before organic branch rooms.", plaza, false);

        int attempts = 0;
        int branch = 0;
        while(rooms.size() < target && attempts < 700){
            attempts++;
            Rectangle rr = proposeRoomNearPlazaBranch(branch++);
            if(rr == null) continue;
            Rectangle grown = new Rectangle(rr.x-1, rr.y-1, rr.width+2, rr.height+2);
            if(rr.x < 1 || rr.y < 1 || rr.x+rr.width >= w-1 || rr.y+rr.height >= h-1) { SectorGenerationTraceAuthority.reject(this, "REJECT", "Room proposal outside safe map bounds.", rr); continue; }
            if(regionTouchesNonWall(grown)) { SectorGenerationTraceAuthority.reject(this, "REJECT", "Room proposal touched carved content, corridor, road, or protected space and was refused.", rr); continue; }
            int before = rooms.size();
            carve(rr);
            rooms.add(rr);
            SectorGenerationTraceAuthority.record(this, "ROOM", "Organic room " + before + " carved before corridor validation.", rr, false);
            ConnectionChoice cc = previewBestConnectionToConnectedSet(before);
            if(cc == null){
                SectorGenerationTraceAuthority.reject(this, "REJECT", "Room " + before + " had no valid corridor connection and was removed.", rr);
                removeRoomAtIndex(before);
                continue;
            }
            commitStrictConnection(cc);
            SectorGenerationTraceAuthority.record(this, "CORRIDOR", "Room " + before + " connected to the reachable set.", rr, false);
        }
        DebugLog.audit("LEVELGEN_PLAZA", "central plaza built rooms="+rooms.size()+" target="+target+" attempts="+attempts+" zone="+zoneType.label+" sewer="+sewerLayer);
        return attempts;
    }

    void buildCentralPlazaLayoutFallback(int target){
        // 0.8.22 LOG-DRIVEN WORLDGEN FIX:
        // The user log from the frozen new-game run showed Sewer Conduit generation repeatedly
        // failing strict plaza acceptance with 3,000-attempt organic retries. That was not a
        // thrown exception; it was an unbounded-feeling generation churn. This fallback no
        // longer reuses the same organic branch proposer. It builds a deterministic connected
        // plaza lattice directly, then carves controlled corridors from the plaza to each room.
        target = WorldGenerationApi.clampRoomTarget(target);
        Rectangle plaza = centralPlazaRect();
        carve(plaza); rooms.add(plaza);
        SectorGenerationTraceAuthority.record(this, "ROOM", "Fallback central plaza carved as room 0.", plaza, false);
        if(!roomProfiles.isEmpty()) roomProfiles.set(0, RoomProfile.centralPlaza(zoneType, sewerLayer, r));
        if(!roomFactions.isEmpty()) roomFactions.set(0, plazaFactionForZone());
        if(!roomSpecials.isEmpty()) roomSpecials.set(0, Boolean.TRUE);
        decorateCentralPlaza(plaza);
        SectorGenerationTraceAuthority.record(this, "ASSET-SWEEP", "Fallback central plaza decoration sweep completed.", plaza, false);
        stampCultImperialisTempleNearPlaza(plaza);
        SectorGenerationTraceAuthority.record(this, "ROOM", "Fallback Cult Imperialis temple module attempted near plaza.", plaza, false);
        seedCompassPlazaCorridors(plaza);
        SectorGenerationTraceAuthority.record(this, "CORRIDOR", "Fallback compass plaza corridors seeded.", plaza, false);

        ArrayList<Rectangle> candidateRooms = deterministicFallbackRooms(plaza, target-1);
        for(Rectangle rr: candidateRooms){
            if(rooms.size() >= target) break;
            Rectangle grown = new Rectangle(rr.x-1, rr.y-1, rr.width+2, rr.height+2);
            if(!rectInMap(grown) || regionTouchesNonWall(grown)) { SectorGenerationTraceAuthority.reject(this, "REJECT", "Fallback lattice room refused by map bounds or carved-space overlap.", rr); continue; }
            carve(rr); rooms.add(rr);
            SectorGenerationTraceAuthority.record(this, "ROOM", "Fallback lattice room " + (rooms.size()-1) + " carved.", rr, false);
            connectRoomToPlazaFallback(plaza, rr);
            SectorGenerationTraceAuthority.record(this, "CORRIDOR", "Fallback lattice room " + (rooms.size()-1) + " connected to plaza.", rr, false);
        }

        if(rooms.size() < target){
            // Last-resort compact ring: preserve playability over strict beauty. These rooms are
            // intentionally small and placed in predictable spokes where the map has space.
            for(int side=0; side<4 && rooms.size()<target; side++){
                for(int n=0; n<5 && rooms.size()<target; n++){
                    Rectangle rr = fallbackSpokeRoom(plaza, side, n);
                    if(rr == null) continue;
                    Rectangle grown = new Rectangle(rr.x-1, rr.y-1, rr.width+2, rr.height+2);
                    if(!rectInMap(grown) || regionTouchesNonWall(grown)) { SectorGenerationTraceAuthority.reject(this, "REJECT", "Fallback spoke room refused by map bounds or carved-space overlap.", rr); continue; }
                    carve(rr); rooms.add(rr);
                    SectorGenerationTraceAuthority.record(this, "ROOM", "Fallback spoke room " + (rooms.size()-1) + " carved.", rr, false);
                    connectRoomToPlazaFallback(plaza, rr);
                    SectorGenerationTraceAuthority.record(this, "CORRIDOR", "Fallback spoke room " + (rooms.size()-1) + " connected to plaza.", rr, false);
                }
            }
        }

        if(rooms.size() < target){
            buildGuaranteedRoomLattice(target);
        }
        boolean reachable = allRoomsReachableStrict();
        if(rooms.size() < 20 || !reachable){
            DebugLog.warn("LEVELGEN_PLAZA_FALLBACK", "Dense fallback still needs repair. rooms="+rooms.size()+" target="+target+" reachable="+reachable+" zone="+zoneType.label);
        } else {
            DebugLog.audit("LEVELGEN_PLAZA_FALLBACK", "deterministic connected fallback rooms="+rooms.size()+" target="+target+" zone="+zoneType.label+" sewer="+sewerLayer);
        }
    }

    ArrayList<Rectangle> deterministicFallbackRooms(Rectangle plaza, int needed){
        ArrayList<Rectangle> out = new ArrayList<>();
        int m = safeRoomEdgeMargin();
        int[][] anchors = {
            {plaza.x-12, plaza.y-9}, {plaza.x+plaza.width+4, plaza.y-9},
            {plaza.x-12, plaza.y+plaza.height+4}, {plaza.x+plaza.width+4, plaza.y+plaza.height+4},
            {plaza.x+2, plaza.y-10}, {plaza.x+8, plaza.y-10},
            {plaza.x+2, plaza.y+plaza.height+4}, {plaza.x+8, plaza.y+plaza.height+4},
            {plaza.x-13, plaza.y+2}, {plaza.x-13, plaza.y+9},
            {plaza.x+plaza.width+4, plaza.y+2}, {plaza.x+plaza.width+4, plaza.y+9},
            {m,m}, {w-m-10,m}, {m,h-m-8}, {w-m-10,h-m-8},
            {w/2-5,m}, {w/2-5,h-m-8}, {m,h/2-3}, {w-m-10,h/2-3}
        };
        for(int[] a: anchors){
            if(out.size() >= needed) break;
            int rw = 6 + Math.floorMod(a[0]+a[1]+(int)seed, 4);
            int rh = 5 + Math.floorMod(a[0]*3+a[1]+(int)(seed>>4), 3);
            int x = clampRoomX(a[0], rw);
            int y = clampRoomY(a[1], rh);
            out.add(new Rectangle(x,y,rw,rh));
        }
        return out;
    }

    int safeRoomEdgeMargin(){ return Math.max(20, Math.min(48, WorldGenerationApi.currentScale().edgeMargin)); }
    int clampRoomX(int x, int rw){ int m=safeRoomEdgeMargin(); return Math.max(m, Math.min(Math.max(m, w-rw-m), x)); }
    int clampRoomY(int y, int rh){ int m=safeRoomEdgeMargin(); return Math.max(m, Math.min(Math.max(m, h-rh-m), y)); }

    Rectangle fallbackSpokeRoom(Rectangle plaza, int side, int n){
        int rw=5, rh=4;
        if(side==0) return new Rectangle(clampRoomX(plaza.x + 1 + n*3, rw), clampRoomY(plaza.y - 8 - n, rh), rw, rh);
        if(side==2) return new Rectangle(clampRoomX(plaza.x + 1 + n*3, rw), clampRoomY(plaza.y+plaza.height+4+n, rh), rw, rh);
        if(side==3) return new Rectangle(clampRoomX(plaza.x - 9 - n, rw), clampRoomY(plaza.y + 1 + n*3, rh), rw, rh);
        return new Rectangle(clampRoomX(plaza.x+plaza.width+4+n, rw), clampRoomY(plaza.y + 1 + n*3, rh), rw, rh);
    }

    void connectRoomToPlazaFallback(Rectangle plaza, Rectangle rr){
        // 0.8.63: emergency repair corridors attach an isolated room to the nearest
        // already-valid corridor network using only orthogonal architectural segments:
        // straight runs with explicit 90-degree turns, never flood-fill serpentine cuts.
        java.util.List<Point> doors = doorCandidates(rr, center(plaza));
        Point bestDoor = null;
        java.util.List<Point> bestPath = null;
        for(Point candidateDoor: doors){
            Point start = outsideOfDoor(rr, candidateDoor);
            java.util.List<Point> path = emergencyPathToNearestReachableCorridor(start);
            if(path == null) continue;
            if(bestPath == null || path.size() < bestPath.size()){
                bestDoor = candidateDoor;
                bestPath = path;
            }
        }
        if(bestDoor != null && bestPath != null){
            tiles[bestDoor.x][bestDoor.y] = DoorType.forZone(zoneType, r).symbol;
            carveEmergencyRepairCorridor(bestPath);
            Point anchor = bestPath.get(bestPath.size()-1);
            DebugLog.audit("LEVELGEN_EMERGENCY_CONNECT", "room=" + roomIndexOf(rr) + " door=" + bestDoor.x + "," + bestDoor.y + " anchor=" + anchor.x + "," + anchor.y + " path=" + bestPath.size() + " zone=" + zoneType.label);
            return;
        }

        // Last resort: if there is somehow no reachable corridor glyph yet, retain a
        // bounded legacy connector so the fallback cannot leave a sealed room. This
        // should be rare because the centered plaza seeds compass corridors first.
        Point b = adjustedLargeRoomDoorWithClearance(rr, edgePointToward(rr, center(plaza)), 3);
        Point nearest = nearestPathableCorridorOrReachableTile(center(rr));
        if(b == null || nearest == null || !inBounds(b.x,b.y) || !inBounds(nearest.x,nearest.y)) return;
        tiles[b.x][b.y] = DoorType.forZone(zoneType, r).symbol;
        carveSimpleFallbackCorridor(outsideOfDoor(rr, b), nearest);
        DebugLog.warn("LEVELGEN_EMERGENCY_CONNECT", "legacy bounded connector used for room=" + roomIndexOf(rr) + " toward=" + nearest.x + "," + nearest.y + " zone=" + zoneType.label);
    }

    int roomIndexOf(Rectangle rr){
        for(int i=0;i<rooms.size();i++) if(rooms.get(i) == rr || rooms.get(i).equals(rr)) return i;
        return -1;
    }

    java.util.List<Point> emergencyPathToNearestReachableCorridor(Point start){
        // 0.8.64 API sectioning: the room-generation repair caller remains here,
        // but emergency corridor selection now lives behind WorldGenerationApi.
        // This is the first class-sectioning step for world generation: Game/world
        // state still owns tiles, while path policy moves behind an API-shaped surface.
        return WorldGenerationApi.emergencyPathToNearestReachableCorridor(this, start);
    }

    java.util.List<java.util.List<Point>> emergencyOrthogonalCandidates(Point start, Point anchor){
        ArrayList<java.util.List<Point>> out = new ArrayList<>();
        out.add(manhattanCorridorPath(start, anchor, true));
        out.add(manhattanCorridorPath(start, anchor, false));
        // If the direct L-shapes are blocked by dense rooms, try one controlled dogleg.
        // These remain architectural: horizontal/vertical/horizontal or vertical/horizontal/vertical.
        int minX = Math.max(1, Math.min(start.x, anchor.x) - 12);
        int maxX = Math.min(w-2, Math.max(start.x, anchor.x) + 12);
        int minY = Math.max(1, Math.min(start.y, anchor.y) - 12);
        int maxY = Math.min(h-2, Math.max(start.y, anchor.y) + 12);
        for(int x=minX; x<=maxX; x+=2){
            out.add(polylinePath(new Point(start.x,start.y), new Point(x,start.y), new Point(x,anchor.y), new Point(anchor.x,anchor.y)));
        }
        for(int y=minY; y<=maxY; y+=2){
            out.add(polylinePath(new Point(start.x,start.y), new Point(start.x,y), new Point(anchor.x,y), new Point(anchor.x,anchor.y)));
        }
        return out;
    }

    java.util.List<Point> polylinePath(Point... points){
        ArrayList<Point> out = new ArrayList<>();
        if(points == null || points.length == 0) return out;
        Point cur = points[0];
        out.add(new Point(cur.x, cur.y));
        for(int i=1; i<points.length; i++){
            Point next = points[i];
            while(cur.x != next.x){ cur = new Point(cur.x + (cur.x < next.x ? 1 : -1), cur.y); out.add(cur); }
            while(cur.y != next.y){ cur = new Point(cur.x, cur.y + (cur.y < next.y ? 1 : -1)); out.add(cur); }
        }
        return out;
    }

    boolean validEmergencyOrthogonalPath(java.util.List<Point> path, Point start, Point anchor){
        if(path == null || path.isEmpty()) return false;
        if(!path.get(0).equals(start)) return false;
        if(!path.get(path.size()-1).equals(anchor)) return false;
        HashSet<String> seen = new HashSet<>();
        for(int i=0; i<path.size(); i++){
            Point p = path.get(i);
            if(!inBounds(p.x,p.y) || roomIds[p.x][p.y] >= 0) return false;
            if(i > 0){
                Point prev = path.get(i-1);
                int md = Math.abs(p.x-prev.x) + Math.abs(p.y-prev.y);
                if(md != 1) return false; // no diagonal jumps or smear-carving
            }
            char t = tiles[p.x][p.y];
            boolean isFinalAnchor = i == path.size()-1 && p.equals(anchor);
            if(isFinalAnchor){
                if(!(isCorridorGlyph(t) && walkable(p.x,p.y))) return false;
            } else if(t != '#') return false;
            if(!seen.add(p.x+","+p.y)) return false;
        }
        return true;
    }

    int countOrthogonalTurns(java.util.List<Point> path){
        if(path == null || path.size() < 3) return 0;
        int turns = 0;
        for(int i=2; i<path.size(); i++){
            Point a = path.get(i-2), b = path.get(i-1), c = path.get(i);
            int dx1 = Integer.compare(b.x-a.x, 0), dy1 = Integer.compare(b.y-a.y, 0);
            int dx2 = Integer.compare(c.x-b.x, 0), dy2 = Integer.compare(c.y-b.y, 0);
            if(dx1 != dx2 || dy1 != dy2) turns++;
        }
        return turns;
    }

    void carveEmergencyRepairCorridor(java.util.List<Point> path){
        if(path == null || path.isEmpty()) return;
        char cg = zoneType.corridorGlyph(r);
        for(Point p: path){
            if(!inBounds(p.x,p.y) || roomIds[p.x][p.y] >= 0) continue;
            if(tiles[p.x][p.y] == '#') tiles[p.x][p.y] = cg;
        }
        reinforceCorridorWalls(path);
    }

    Point nearestPathableCorridorOrReachableTile(Point from){
        boolean[][] reachable = reachableFromStart();
        Point best = null;
        int bestD = Integer.MAX_VALUE;
        for(int x=0;x<w;x++) for(int y=0;y<h;y++){
            if(!reachable[x][y] || !walkable(x,y)) continue;
            if(roomIds[x][y] >= 0 && !isCorridorGlyph(tiles[x][y])) continue;
            int d = Math.abs(x-from.x) + Math.abs(y-from.y);
            if(d < bestD){ bestD = d; best = new Point(x,y); }
        }
        return best;
    }

    void carveSimpleFallbackCorridor(Point start, Point end){
        if(start == null || end == null || !inBounds(start.x,start.y) || !inBounds(end.x,end.y)) return;
        char cg = zoneType.corridorGlyph(r);
        int x=start.x, y=start.y;
        int guard=0;
        while(x != end.x && guard++ < w+h+40){
            carveConnectorTile(x,y,cg);
            x += x < end.x ? 1 : -1;
        }
        while(y != end.y && guard++ < (w+h)*2+80){
            carveConnectorTile(x,y,cg);
            y += y < end.y ? 1 : -1;
        }
        carveConnectorTile(end.x,end.y,cg);
    }

    void carveConnectorTile(int x, int y, char cg){
        if(!inBounds(x,y)) return;
        if(roomIds[x][y] < 0) tiles[x][y] = cg;
        else if(tiles[x][y] == '#') tiles[x][y] = DoorType.forZone(zoneType, r).symbol;
    }

    int widenOneTileCorridors(String phase){
        // 0.9.06k: corridor doctrine now prefers two-wide travel lanes where the validated
        // layout has spare wall mass. This is conservative: it widens only non-room corridor
        // runs into adjacent anonymous wall cells and never cuts through rooms, doors, machines,
        // interstitial infrastructure, or void space. One-wide corridors can still exist where
        // architecture is too tight; push-past handles those chokepoints at runtime.
        ArrayList<Point> additions = new ArrayList<>();
        for(int x=1; x<w-1; x++) for(int y=1; y<h-1; y++){
            if(roomIds[x][y] >= 0 || !isCorridorGlyph(tiles[x][y]) || !walkable(x,y)) continue;
            boolean hRun = corridorRunNeighbor(x-1,y) || corridorRunNeighbor(x+1,y);
            boolean vRun = corridorRunNeighbor(x,y-1) || corridorRunNeighbor(x,y+1);
            if(hRun && !vRun){
                if(canWidenCorridorInto(x,y+1)) additions.add(new Point(x,y+1));
                else if(canWidenCorridorInto(x,y-1)) additions.add(new Point(x,y-1));
            } else if(vRun && !hRun){
                if(canWidenCorridorInto(x+1,y)) additions.add(new Point(x+1,y));
                else if(canWidenCorridorInto(x-1,y)) additions.add(new Point(x-1,y));
            }
        }
        int widened = 0;
        char cg = zoneType.corridorGlyph(r);
        HashSet<String> seen = new HashSet<>();
        for(Point p: additions){
            if(p == null || !inBounds(p.x,p.y) || !seen.add(occKey(p.x,p.y))) continue;
            if(canWidenCorridorInto(p.x,p.y)){ tiles[p.x][p.y] = cg; widened++; }
        }
        if(widened > 0) DebugLog.audit("CORRIDOR_TWO_WIDE", "phase="+phase+" widened="+widened+" zone="+zoneType.label+" layer="+layerText()+" seed="+seed);
        return widened;
    }

    boolean corridorRunNeighbor(int x,int y){
        return inBounds(x,y) && roomIds[x][y] < 0 && (isCorridorGlyph(tiles[x][y]) || tiles[x][y] == 'D' || tiles[x][y] == '/');
    }

    boolean canWidenCorridorInto(int x,int y){
        return inBounds(x,y) && x>0 && y>0 && x<w-1 && y<h-1 && roomIds[x][y] < 0 && tiles[x][y] == '#';
    }

    Rectangle centralPlazaRect(){
        // 0.8.66 API sectioning: plaza sizing/centering belongs to the
        // world-generation scale surface so large arcology presets can change
        // footprint without every room-placement helper learning new constants.
        return WorldGenerationApi.centralPlazaRect(w, h);
    }



    int normalizeRoomShellsAndCorridorHalos(){
        if(rooms == null || rooms.isEmpty()) return 0;
        int changed = 0;
        char corridor = zoneType.corridorGlyph(r);
        for(int i=0; i<rooms.size(); i++){
            Rectangle rr = rooms.get(i);
            if(rr == null) continue;
            for(int x=rr.x; x<rr.x+rr.width; x++){
                changed += normalizeRoomBoundaryCell(x, rr.y);
                changed += normalizeRoomBoundaryCell(x, rr.y+rr.height-1);
                if(i > 0){
                    changed += carveRoomAccessHalo(x, rr.y-1, corridor);
                    changed += carveRoomAccessHalo(x, rr.y+rr.height, corridor);
                }
            }
            for(int y=rr.y+1; y<rr.y+rr.height-1; y++){
                changed += normalizeRoomBoundaryCell(rr.x, y);
                changed += normalizeRoomBoundaryCell(rr.x+rr.width-1, y);
                if(i > 0){
                    changed += carveRoomAccessHalo(rr.x-1, y, corridor);
                    changed += carveRoomAccessHalo(rr.x+rr.width, y, corridor);
                }
            }
        }
        if(changed > 0) DebugLog.audit("ROOM_SHELL_NORMALIZE", "changed="+changed+" rooms="+rooms.size()+" rule=one authoritative room wall shell plus non-room access halo zone="+zoneType.label+" seed="+seed);
        return changed;
    }

    int normalizeRoomBoundaryCell(int x, int y){
        if(!inBounds(x,y) || roomIds[x][y] < 0) return 0;
        char old = tiles[x][y];
        if(isDoorSymbol(old) || old == 'D' || old == 'S' || old == 'E' || old == 'v') return 0;
        if(old == '#') return 0;
        tiles[x][y] = '#';
        return 1;
    }

    int carveRoomAccessHalo(int x, int y, char corridor){
        if(!inBounds(x,y) || x <= 1 || y <= 1 || x >= w-2 || y >= h-2) return 0;
        if(roomIds[x][y] >= 0 || mapObjectAt(x,y) != null) return 0;
        char old = tiles[x][y];
        if(old != '#') return 0;
        if(exteriorEnvelopeNear(x,y)) return 0;
        tiles[x][y] = corridor;
        return 1;
    }

    boolean exteriorEnvelopeNear(int x, int y){
        for(int dx=-1; dx<=1; dx++) for(int dy=-1; dy<=1; dy++){
            if(!inBounds(x+dx,y+dy)) return true;
            char c = tiles[x+dx][y+dy];
            if(c == InterstitialInfrastructureApi.VOID_SPACE || c == '-') return true;
        }
        return false;
    }

    void applyFactionRoomManifest(){
        // 0.8.66 ROOM MANIFEST API PASS:
        // The generator already creates dense rooms; this layer assigns the first tranche
        // of non-plaza rooms faction-specific civic/production identities before random
        // special overlays are applied. It preserves current physical generation while
        // moving room-purpose authority behind RoomManifestApi.
        java.util.List<StampedRoomSpec> manifest = RoomManifestApi.requiredRoomManifest(zoneType);
        if(manifest == null || manifest.isEmpty() || rooms.size() <= 1) return;
        int applied = 0;
        for(int id=1; id<rooms.size() && applied<manifest.size(); id++){
            StampedRoomSpec spec = manifest.get(applied++);
            roomProfiles.set(id, spec.toProfile(zoneType, r));
            roomFactions.set(id, spec.faction);
            stampRoomPurposeFeatures(rooms.get(id), spec);
        }
        DebugLog.audit("ROOM_MANIFEST_API", "zone="+zoneType.label+" requiredManifest="+manifest.size()+" applied="+applied+" rooms="+rooms.size());
    }

    void stampRoomPurposeFeatures(Rectangle rr, StampedRoomSpec spec){
        if(rr == null || spec == null) return;
        String kind = spec.kind;
        int minX=rr.x+1, maxX=rr.x+rr.width-2, minY=rr.y+1, maxY=rr.y+rr.height-2;
        if(minX>maxX || minY>maxY) return;
        if("CAFETERIA".equals(kind)){
            // Ordered long-room service: tables/benches in ranks, bins by the edge,
            // counter/kitchen symbols at one end. Military versions look like drill,
            // noble versions like service galleries, etc. through the profile text.
            for(int y=minY; y<=maxY; y+=2) for(int x=minX; x<=maxX; x+=3){ if(inBounds(x,y)) tiles[x][y]='T'; if(inBounds(x+1,y)) tiles[x+1][y]='b'; }
            for(int y=minY; y<=maxY; y++){ if(inBounds(maxX,y)) tiles[maxX][y]='q'; }
            if(inBounds(minX,maxY)) tiles[minX][maxY]='N'; if(inBounds(minX+1,maxY)) tiles[minX+1][maxY]='N';
        } else if("KITCHEN".equals(kind)){
            for(int x=minX; x<=maxX; x+=2){ if(inBounds(x,minY)) tiles[x][minY]='q'; if(inBounds(x,maxY)) tiles[x][maxY]='N'; }
            if(inBounds(maxX,maxY)) tiles[maxX][maxY]='F';
        } else if("BARRACKS".equals(kind) || "DORMITORY".equals(kind)){
            for(int y=minY; y<=maxY; y+=2){ if(inBounds(minX,y)) tiles[minX][y]='c'; if(inBounds(maxX,y)) tiles[maxX][y]='s'; }
            if(inBounds((minX+maxX)/2, minY)) tiles[(minX+maxX)/2][minY]='u';
        } else if("APARTMENT".equals(kind)){
            if(inBounds(minX,minY)) tiles[minX][minY]='q';
            if(inBounds(maxX,minY)) tiles[maxX][minY]='c';
            if(inBounds(minX,maxY)) tiles[minX][maxY]='u';
            if(inBounds(maxX,maxY)) tiles[maxX][maxY]='s';
        } else if("FOOD_STORE".equals(kind)){
            for(int y=minY; y<=maxY; y++) for(int x=minX; x<=maxX; x+=2) if(inBounds(x,y)) tiles[x][y]=(x % 4 == 0 ? 'u' : 'q');
        } else if("WAREHOUSE".equals(kind)){
            for(int y=minY; y<=maxY; y+=2) for(int x=minX; x<=maxX; x+=2) if(inBounds(x,y)) tiles[x][y]='b';
        } else if("STOREFRONT".equals(kind)){
            for(int x=minX; x<=maxX; x++) if(inBounds(x,minY)) tiles[x][minY]='T';
            if(inBounds(maxX,maxY)) tiles[maxX][maxY]='$';
        } else if("LIBRARY".equals(kind) || "LEARNING".equals(kind)){
            for(int x=minX; x<=maxX; x+=2) for(int y=minY; y<=maxY; y++) if(inBounds(x,y)) tiles[x][y]='l';
            if(inBounds(maxX,minY)) tiles[maxX][minY]='q';
        } else if("DAYCARE".equals(kind)){
            for(int x=minX; x<=maxX; x+=2) if(inBounds(x,minY)) tiles[x][minY]='d';
            if(inBounds(minX,maxY)) tiles[minX][maxY]='c'; if(inBounds(maxX,maxY)) tiles[maxX][maxY]='T';
        } else if("TRAINING".equals(kind)){
            for(int x=minX; x<=maxX; x++) if(inBounds(x,minY)) tiles[x][minY]='!';
            for(int y=minY+1; y<=maxY; y+=2) if(inBounds(maxX,y)) tiles[maxX][y]='b';
        } else if("CLINIC".equals(kind)){
            if(inBounds(minX,minY)) tiles[minX][minY]='u'; if(inBounds(maxX,minY)) tiles[maxX][minY]='c'; if(inBounds((minX+maxX)/2,maxY)) tiles[(minX+maxX)/2][maxY]='q';
        } else if("SECURITY".equals(kind)){
            for(int x=minX; x<=maxX; x+=2) if(inBounds(x,minY)) tiles[x][minY]='X';
            if(inBounds(maxX,maxY)) tiles[maxX][maxY]='!';
        } else if("SANITATION".equals(kind)){
            for(int x=minX; x<=maxX; x+=2){ if(inBounds(x,minY)) tiles[x][minY]='u'; if(inBounds(x,maxY)) tiles[x][maxY]='N'; }
        } else if("MACHINERY".equals(kind) || "WORKSHOP".equals(kind)){
            for(int x=minX; x<=maxX; x+=2){ if(inBounds(x,minY)) tiles[x][minY]='R'; if(inBounds(x,maxY)) tiles[x][maxY]='N'; }
            if(inBounds(maxX,maxY)) tiles[maxX][maxY]='q';
        } else if("LOGISTICS".equals(kind)){
            for(int y=minY; y<=maxY; y+=2) for(int x=minX; x<=maxX; x+=2) if(inBounds(x,y)) tiles[x][y]='b';
            if(inBounds(maxX,minY)) tiles[maxX][minY]='u';
        } else if("SHRINE".equals(kind)){
            for(int x=minX; x<=maxX; x+=2) if(inBounds(x,minY)) tiles[x][minY]='h';
            if(inBounds((minX+maxX)/2, maxY)) tiles[(minX+maxX)/2][maxY]='n';
        } else if("SPECIALTY".equals(kind)){
            for(int y=minY; y<=maxY; y+=2) for(int x=minX; x<=maxX; x+=2) if(inBounds(x,y)) tiles[x][y]=spec.primaryGlyph();
        }
    }

    void stampCultImperialisTempleNearPlaza(Rectangle plaza){
        // 0.9.04a ECCLESIARCHY TEMPLE PASS:
        // Every generated zone receives a neutral Cult Imperialis temple near the central plaza.
        // It is stamped before compass corridors and faction modules so the sanctuary owns space
        // close to the plaza instead of being pushed to random room assignment.
        if(plaza == null || EcclesiarchyTempleApi.templeAlreadyStamped(roomProfiles)) return;
        int[] offsets = {0, -6, 6, -10, 10};
        for(int oi=0; oi<offsets.length; oi++){
            for(int side=0; side<4; side++){
                if(stampTempleRoomAt(plaza, side, offsets[oi])){
                    DebugLog.audit("ECCLESIARCHY_TEMPLE_STAMP", "zone="+zoneType.label+" side="+side+" offset="+offsets[oi]+" rooms="+rooms.size()+" layer="+layerText());
                    return;
                }
            }
        }
        DebugLog.warn("ECCLESIARCHY_TEMPLE_STAMP", "failed to stamp large temple near plaza; generator will fall back to manifest/room profile hooks. zone="+zoneType.label+" layer="+layerText());
    }

    boolean stampTempleRoomAt(Rectangle plaza, int side, int offset){
        boolean horizontal = side==1 || side==3;
        int len = 4, cw = 3;
        int naveW = horizontal ? 18 : 9;
        int naveH = horizontal ? 9 : 18;
        int cx, cy;
        if(side==1){ cx = plaza.x+plaza.width; cy = plaza.y+plaza.height/2-cw/2+offset; }
        else if(side==3){ cx = plaza.x-len; cy = plaza.y+plaza.height/2-cw/2+offset; }
        else if(side==0){ cx = plaza.x+plaza.width/2-cw/2+offset; cy = plaza.y-len; }
        else { cx = plaza.x+plaza.width/2-cw/2+offset; cy = plaza.y+plaza.height; }
        Rectangle corridor = horizontal ? new Rectangle(cx, cy, len, cw) : new Rectangle(cx, cy, cw, len);
        Rectangle nave;
        if(side==1) nave = new Rectangle(corridor.x+corridor.width, corridor.y+corridor.height/2-naveH/2, naveW, naveH);
        else if(side==3) nave = new Rectangle(corridor.x-naveW, corridor.y+corridor.height/2-naveH/2, naveW, naveH);
        else if(side==0) nave = new Rectangle(corridor.x+corridor.width/2-naveW/2, corridor.y-naveH, naveW, naveH);
        else nave = new Rectangle(corridor.x+corridor.width/2-naveW/2, corridor.y+corridor.height, naveW, naveH);
        ArrayList<Rectangle> candidateRooms = new ArrayList<>(); candidateRooms.add(nave);
        if(!moduleAreaLegal(corridor, candidateRooms)) return false;
        char cg = zoneType.corridorGlyph(r);
        for(int x=corridor.x; x<corridor.x+corridor.width; x++) for(int y=corridor.y; y<corridor.y+corridor.height; y++) if(inBounds(x,y)) tiles[x][y]=cg;
        Point pd = plazaDoorForModule(plaza, corridor);
        if(pd != null && inBounds(pd.x,pd.y)) tiles[pd.x][pd.y] = DoorType.forZone(zoneType, r).symbol;
        int idx = rooms.size();
        carve(nave); rooms.add(nave);
        RoomProfile rp = EcclesiarchyTempleApi.templeProfile(zoneType, r);
        if(idx < roomProfiles.size()) roomProfiles.set(idx, rp);
        if(idx < roomFactions.size()) roomFactions.set(idx, Faction.MINISTORUM);
        if(idx < roomSpecials.size()) roomSpecials.set(idx, Boolean.TRUE);
        Point d = doorBetweenRoomAndCorridor(nave, corridor);
        if(d != null && inBounds(d.x,d.y)) tiles[d.x][d.y] = DoorType.forZone(zoneType, r).symbol;
        stampCultImperialisTempleFeatures(nave, horizontal);
        return true;
    }

    void stampCultImperialisTempleFeatures(Rectangle rr, boolean horizontal){
        if(rr == null) return;
        int minX=rr.x+1, maxX=rr.x+rr.width-2, minY=rr.y+1, maxY=rr.y+rr.height-2;
        if(minX>maxX || minY>maxY) return;
        // Nave aisle, pillars/columns, saint alcoves, candle racks, relics, donation box, and supplicant kitchen.
        if(horizontal){
            int midY = (minY+maxY)/2;
            for(int x=minX+2; x<=maxX-3; x+=4){ if(inBounds(x,minY+1)) tiles[x][minY+1]='I'; if(inBounds(x,maxY-1)) tiles[x][maxY-1]='I'; }
            for(int x=minX+1; x<=maxX-1; x+=5){ if(inBounds(x,minY)) tiles[x][minY]='W'; if(inBounds(x,maxY)) tiles[x][maxY]='W'; }
            for(int x=minX+2; x<=maxX-5; x+=4){ if(inBounds(x,midY-1)) tiles[x][midY-1]='b'; if(inBounds(x,midY+1)) tiles[x][midY+1]='b'; }
            if(inBounds(maxX-1,midY)) tiles[maxX-1][midY]='I';
            if(inBounds(maxX-2,midY-1)) tiles[maxX-2][midY-1]='$';
            for(int y=minY; y<=maxY; y++){ if(inBounds(minX,y)) tiles[minX][y]=(y%2==0?'q':'u'); }
            if(inBounds(minX+1,maxY)) tiles[minX+1][maxY]='T';
            if(inBounds(minX+2,maxY)) tiles[minX+2][maxY]='N';
        } else {
            int midX = (minX+maxX)/2;
            for(int y=minY+2; y<=maxY-3; y+=4){ if(inBounds(minX+1,y)) tiles[minX+1][y]='I'; if(inBounds(maxX-1,y)) tiles[maxX-1][y]='I'; }
            for(int y=minY+1; y<=maxY-1; y+=5){ if(inBounds(minX,y)) tiles[minX][y]='W'; if(inBounds(maxX,y)) tiles[maxX][y]='W'; }
            for(int y=minY+2; y<=maxY-5; y+=4){ if(inBounds(midX-1,y)) tiles[midX-1][y]='b'; if(inBounds(midX+1,y)) tiles[midX+1][y]='b'; }
            if(inBounds(midX,maxY-1)) tiles[midX][maxY-1]='I';
            if(inBounds(midX-1,maxY-2)) tiles[midX-1][maxY-2]='$';
            for(int x=minX; x<=maxX; x++){ if(inBounds(x,minY)) tiles[x][minY]=(x%2==0?'q':'u'); }
            if(inBounds(maxX,minY+1)) tiles[maxX][minY+1]='T';
            if(inBounds(maxX,minY+2)) tiles[maxX][minY+2]='N';
        }
    }


    void stampFactionRepresentativeBarNearTransit(){
        // 0.9.07w FACTION REPRESENTATIVE BAR STAMP:
        // Every generated zone receives a recognizable small faction-aligned bar near a level-transition edge.
        // This is a hard recovery anchor: if a faction loses all other rooms, the bar representative is still a
        // protected civic/political contact who can restore standing, explain recovery work, and serve as provisional leadership.
        if(factionRepBarAlreadyStamped()) return;
        Faction f = dominantContinuityFactionForZone();
        int[][] edgeSeeds = {{1,h/2,1,0},{w-2,h/2,-1,0},{w/2,1,0,1},{w/2,h-2,0,-1}};
        int[] offsets = {-8,-4,0,4,8,12,-12};
        for(int[] seedPoint: edgeSeeds){
            Point corridorPoint = nearestTransitCorridorPoint(seedPoint[0], seedPoint[1]);
            if(corridorPoint == null) continue;
            for(int off: offsets){
                for(int side=0; side<4; side++){
                    if(stampFactionRepBarAtCorridor(corridorPoint, side, off, f)){
                        DebugLog.audit("FACTION_REP_BAR_STAMP", "zone="+zoneType.label+" faction="+f.label+" room="+(rooms.size()-1)+" anchor="+corridorPoint.x+","+corridorPoint.y+" side="+side+" offset="+off+" layer="+layerText());
                        return;
                    }
                }
            }
        }
        DebugLog.warn("FACTION_REP_BAR_STAMP", "failed to stamp continuity bar near transition; faction continuity logic remains active. zone="+zoneType.label+" faction="+f.label+" seed="+seed);
    }

    boolean factionRepBarAlreadyStamped(){
        for(RoomProfile rp: roomProfiles) if(isFactionRepBarProfile(rp)) return true;
        return false;
    }

    boolean isFactionRepBarProfile(RoomProfile rp){
        if(rp == null || rp.name == null) return false;
        String low = rp.name.toLowerCase(Locale.ROOT) + " " + (rp.featureText == null ? "" : rp.featureText.toLowerCase(Locale.ROOT));
        return low.contains("faction representative bar") || low.contains("continuity bar");
    }

    Faction dominantContinuityFactionForZone(){
        if(zoneType==ZoneType.MUTANT_SEWER_CAMP || zoneType==ZoneType.MUTANT_WARRENS) return Faction.MUTANT;
        if(zoneType==ZoneType.CULTIST_SEWER_CAMP) return Faction.CULTIST;
        if(zoneType==ZoneType.ARBITES_PRECINCT_EDGE) return Faction.CIVIC_WARDENS;
        if(zoneType==ZoneType.IMPERIAL_GUARD_BILLET) return Faction.IMPERIAL_GUARD;
        if(zoneType==ZoneType.MECHANICUS_FORGE_CLOISTER || zoneType==ZoneType.MECHANICUS_RELIC_DUCT) return Faction.MECHANIST_COLLEGIA;
        if(zoneType==ZoneType.GANGER_TURF) return Faction.BANDIT;
        if(zoneType==ZoneType.SECTOR_GOVERNORS_MANSION || zoneType==ZoneType.NOBLE_SERVICE_SPINE) return Faction.NOBLE;
        if(zoneType==ZoneType.IMPERIAL_NEWS_NETWORK) return Faction.INN;
        if(zoneType==ZoneType.ADMINISTRATUM_ARCHIVE) return Faction.CIVIC_LEDGER_OFFICE;
        if(zoneType==ZoneType.NEUTRAL_RAIL_DEPOT || zoneType==ZoneType.SUMP_MARKET) return Faction.HIVER;
        if(zoneType==ZoneType.HAB_STACK || zoneType==ZoneType.NEUTRAL_CIVILIAN_FLOOR) return Faction.HIVER;
        if(zoneType==ZoneType.SEWER_CONDUIT) return Faction.SCAVENGER;
        return Faction.HIVER;
    }

    Point nearestTransitCorridorPoint(int sx, int sy){
        Point best=null; int bd=999999;
        for(int x=1; x<w-1; x++) for(int y=1; y<h-1; y++){
            if(!(isCorridorGlyph(tiles[x][y]) || tiles[x][y]=='=' || tiles[x][y]==':' || tiles[x][y]=='+' || tiles[x][y]=='D')) continue;
            int edgeDist = Math.min(Math.min(x, w-1-x), Math.min(y, h-1-y));
            if(edgeDist > Math.max(10, Math.min(w,h)/5)) continue;
            int d = Math.abs(x-sx)+Math.abs(y-sy);
            if(d < bd){ bd=d; best=new Point(x,y); }
        }
        return best;
    }

    boolean stampFactionRepBarAtCorridor(Point anchor, int side, int offset, Faction f){
        if(anchor == null) return false;
        boolean horizontal = side==1 || side==3;
        int bw = horizontal ? 13 : 7;
        int bh = horizontal ? 7 : 13;
        int cx = anchor.x, cy = anchor.y;
        Rectangle rr;
        if(side==1) rr = new Rectangle(cx+2, cy-bh/2+offset, bw, bh);
        else if(side==3) rr = new Rectangle(cx-bw-2, cy-bh/2+offset, bw, bh);
        else if(side==0) rr = new Rectangle(cx-bw/2+offset, cy-bh-2, bw, bh);
        else rr = new Rectangle(cx-bw/2+offset, cy+2, bw, bh);
        if(!repBarAreaLegal(anchor, rr)) return false;
        int idx = rooms.size();
        carve(rr); rooms.add(rr);
        RoomProfile rp = RoomProfile.factionRepresentativeBar(zoneType, f, r);
        if(idx < roomProfiles.size()) roomProfiles.set(idx, rp);
        if(idx < roomFactions.size()) roomFactions.set(idx, f);
        if(idx < roomSpecials.size()) roomSpecials.set(idx, Boolean.TRUE);
        Point door = doorBetweenRoomAndCorridor(rr, new Rectangle(anchor.x, anchor.y, 1, 1));
        if(door == null) door = edgePointToward(rr, anchor);
        if(door != null && inBounds(door.x, door.y)) tiles[door.x][door.y] = DoorType.forZone(zoneType, r).symbol;
        carveLine(adjacentOutsideDoor(door, anchor), anchor);
        stampFactionRepBarFeatures(rr, horizontal, f);
        return true;
    }

    boolean repBarAreaLegal(Point anchor, Rectangle rr){
        if(rr == null || anchor == null) return false;
        if(rr.x < 3 || rr.y < 3 || rr.x+rr.width >= w-3 || rr.y+rr.height >= h-3) return false;
        Rectangle grown = new Rectangle(rr.x-1, rr.y-1, rr.width+2, rr.height+2);
        for(int x=grown.x; x<grown.x+grown.width; x++) for(int y=grown.y; y<grown.y+grown.height; y++){
            if(!inBounds(x,y)) return false;
            if(x==anchor.x && y==anchor.y) continue;
            if(roomIds[x][y] >= 0) return false;
            char t = tiles[x][y];
            if(t != '#' && !isCorridorGlyph(t) && t != '=' && t != ':' && t != '+') return false;
        }
        return true;
    }

    void stampFactionRepBarFeatures(Rectangle rr, boolean horizontal, Faction f){
        if(rr == null) return;
        int minX=rr.x+1, maxX=rr.x+rr.width-2, minY=rr.y+1, maxY=rr.y+rr.height-2;
        if(minX>maxX || minY>maxY) return;
        if(horizontal){
            int barY = minY;
            for(int x=minX; x<=maxX; x++) if(inBounds(x,barY)) tiles[x][barY]='T';
            for(int x=minX+1; x<=maxX-1; x+=2) if(inBounds(x,barY+1)) tiles[x][barY+1]='b';
            if(inBounds(maxX,barY+1)) tiles[maxX][barY+1]='q'; // cheap radio / rep desk
            if(inBounds(maxX-1,barY+1)) tiles[maxX-1][barY+1]='1'; // pict viewer / news box
            if(inBounds(minX,barY+1)) tiles[minX][barY+1]='u'; // refrigerator/cooler
            if(inBounds(minX+1,barY+1)) tiles[minX+1][barY+1]='N'; // service keg/pump
        } else {
            int barX = minX;
            for(int y=minY; y<=maxY; y++) if(inBounds(barX,y)) tiles[barX][y]='T';
            for(int y=minY+1; y<=maxY-1; y+=2) if(inBounds(barX+1,y)) tiles[barX+1][y]='b';
            if(inBounds(barX+1,maxY)) tiles[barX+1][maxY]='q';
            if(inBounds(barX+1,maxY-1)) tiles[barX+1][maxY-1]='1';
            if(inBounds(barX+1,minY)) tiles[barX+1][minY]='u';
            if(inBounds(barX+1,minY+1)) tiles[barX+1][minY+1]='N';
        }
        // Leave center/outer floor open for patrons and the protected representative.
    }

    void seedFactionContinuityBarStaff(){
        int bars = 0, reps = 0, patrons = 0;
        for(int i=1; i<rooms.size(); i++){
            if(i >= roomProfiles.size() || !isFactionRepBarProfile(roomProfiles.get(i))) continue;
            bars++;
            Rectangle rr = rooms.get(i);
            Faction f = roomFaction(i);
            if(f == Faction.NONE) f = dominantContinuityFactionForZone();
            Point rp = randomOpenPointInRoom(rr);
            if(rp != null && npcAt(rp.x,rp.y)==null){
                NpcEntity rep = NpcEntity.factionRepresentative(f, rp.x, rp.y, r);
                int held = countRoomsControlledByFaction(f, true);
                if(held <= 1){ rep.factionRank = 1; rep.factionRankTitle = FactionRosterAuthority.titleForRank(f, 1); rep.factionRankScope = "provisional continuity leader while the faction has no other secure rooms"; }
                rep.state = held <= 1 ? "Provisional Leadership" : "Contract Desk";
                PersonnelPopulationApi.attachExistingNpcToRoomLedger(rep, this, i, r);
                npcs.add(rep); reps++;
            }
            int crowd = 3 + r.nextInt(4);
            for(int p=0; p<crowd; p++){
                Point pt = randomOpenPointInRoom(rr);
                if(pt != null && npcAt(pt.x,pt.y)==null){
                    Faction pf = (r.nextDouble()<0.45) ? Faction.HIVER : f;
                    NpcEntity n = PersonnelPopulationApi.createResidentFromRoom(this, i, pf, pt.x, pt.y, r);
                    n.role = "Bar Patron";
                    n.state = "Milling";
                    n.symbol = 'h';
                    npcs.add(n); patrons++;
                }
            }
        }
        if(bars > 0) DebugLog.audit("FACTION_REP_BAR_STAFF", "zone="+zoneType.label+" bars="+bars+" reps="+reps+" patrons="+patrons+" layer="+layerText());
    }

    void seedFactionLeadershipResidency(){
        HashSet<Faction> handled = new HashSet<>();
        for(int i=1; i<rooms.size(); i++){
            Faction f = roomFaction(i);
            if(f == null || f == Faction.NONE || handled.contains(f)) continue;
            if(isFactionRepBarProfile(roomProfile(i))) continue;
            int held = countRoomsControlledByFaction(f, false);
            if(held <= 0) continue;
            handled.add(f);
            Rectangle rr = rooms.get(i);
            Point p = randomOpenPointInRoom(rr);
            if(p == null || npcAt(p.x,p.y)!=null) continue;
            NpcEntity leader = NpcEntity.create(f, zoneType, p.x, p.y, r);
            leader.role = "Faction Leader";
            leader.state = "Base of Operations";
            leader.factionRank = 1;
            leader.factionRankTitle = FactionRosterAuthority.titleForRank(f, 1);
            leader.factionRankScope = "controls the faction's current local base of operations";
            leader.name = leader.factionRankTitle + " " + CharacterCreationAuthority.formalName(r);
            leader.symbol = 'L';
            PersonnelPopulationApi.attachExistingNpcToRoomLedger(leader, this, i, r);
            npcs.add(leader);
            DebugLog.audit("FACTION_LEADER_RESIDENCY", "zone="+zoneType.label+" faction="+f.label+" room="+i+" pos="+p.x+","+p.y+" heldRooms="+held);
        }
    }

    int countRoomsControlledByFaction(Faction f, boolean includeRepBars){
        if(f == null || f == Faction.NONE) return 0;
        int n = 0;
        for(int i=0; i<roomFactions.size(); i++){
            if(roomFactions.get(i) != f) continue;
            if(!includeRepBars && isFactionRepBarProfile(roomProfile(i))) continue;
            n++;
        }
        return n;
    }

    void stampStructuredZoneModules(Rectangle plaza, int targetRooms){
        // 0.8.67 STRUCTURED ROOM MODULE API PASS:
        // Manifest entries are now allowed to become physical stamped architecture, not
        // only labels applied to whatever organic room happened to exist. RoomManifestApi
        // chooses the faction module set; World still owns collision checks, carving,
        // tile arrays, and corridor attachment.
        java.util.List<StampedModuleSpec> modules = RoomManifestApi.structuredModulesFor(zoneType);
        int made = 0;
        int moduleIndex = 0;
        for(StampedModuleSpec module: modules){
            if(rooms.size() >= targetRooms) break;
            boolean placed = false;
            for(int attempt=0; attempt<4 && !placed; attempt++){
                int side = (module.preferredSide + attempt) % 4;
                int count = stampFactionStructuredModule(plaza, side, module);
                if(count > 0){ made += count; placed = true; }
            }
            moduleIndex++;
        }
        // Keep the older compact hab modules as compatibility fallbacks for ordinary
        // civilian space if the new API modules did not reach the desired density.
        if(rooms.size() < targetRooms && (zoneType==ZoneType.HAB_STACK || zoneType==ZoneType.NEUTRAL_CIVILIAN_FLOOR)){
            int desired = Math.max(0, Math.min(6, targetRooms - rooms.size()));
            for(int side=0; side<4 && made<desired; side++) if(stampDormitorySegment(plaza, side)) made += 4;
            for(int side=3; side>=0 && rooms.size()<targetRooms; side--) if(stampApartmentCluster(plaza, side)) made += 4;
        }
        DebugLog.audit("ROOM_MODULE_API", "zone="+zoneType.label+" modules="+modules.size()+" stampedRooms="+made+" roomsAfter="+rooms.size()+" target="+targetRooms);
    }

    int stampFactionStructuredModule(Rectangle plaza, int side, StampedModuleSpec module){
        if(plaza == null || module == null || module.rooms.length == 0) return 0;
        boolean horizontal = side==1 || side==3;
        int len = module.corridorLength;
        int cw = module.corridorWidth;
        int off = module.laneOffset;
        int cx, cy;
        if(side==1){ cx = plaza.x+plaza.width; cy = plaza.y+plaza.height/2-cw/2+off; }
        else if(side==3){ cx = plaza.x-len; cy = plaza.y+plaza.height/2-cw/2+off; }
        else if(side==0){ cx = plaza.x+plaza.width/2-cw/2+off; cy = plaza.y-len; }
        else { cx = plaza.x+plaza.width/2-cw/2+off; cy = plaza.y+plaza.height; }
        Rectangle corridor = horizontal ? new Rectangle(cx, cy, len, cw) : new Rectangle(cx, cy, cw, len);
        java.util.List<Rectangle> candidateRooms = moduleCandidateRooms(corridor, side, module);
        if(candidateRooms.isEmpty() || !moduleAreaLegal(corridor, candidateRooms)) return 0;
        char cg = zoneType.corridorGlyph(r);
        for(int x=corridor.x; x<corridor.x+corridor.width; x++) for(int y=corridor.y; y<corridor.y+corridor.height; y++) if(inBounds(x,y)) tiles[x][y]=cg;
        dressModuleCorridor(corridor, module);
        Point pd = plazaDoorForModule(plaza, corridor);
        if(pd != null && inBounds(pd.x,pd.y)) tiles[pd.x][pd.y] = DoorType.forZone(zoneType, r).symbol;
        int made = 0;
        for(int i=0;i<candidateRooms.size() && i<module.rooms.length;i++){
            Rectangle rr = candidateRooms.get(i);
            StampedRoomSpec spec = module.rooms[i];
            int idx=rooms.size(); carve(rr); rooms.add(rr);
            if(idx < roomProfiles.size()) roomProfiles.set(idx, spec.toProfile(zoneType, r));
            if(idx < roomFactions.size()) roomFactions.set(idx, spec.faction);
            stampRoomPurposeFeatures(rr, spec);
            Point d = doorBetweenRoomAndCorridor(rr, corridor);
            if(d != null && inBounds(d.x,d.y)) tiles[d.x][d.y] = DoorType.forZone(zoneType, r).symbol;
            made++;
        }
        DebugLog.audit("ROOM_MODULE_STAMP", "zone="+zoneType.label+" module="+module.name+" side="+side+" rooms="+made+" corridor="+corridor.x+","+corridor.y+","+corridor.width+","+corridor.height);
        return made;
    }

    java.util.List<Rectangle> moduleCandidateRooms(Rectangle corridor, int side, StampedModuleSpec module){
        ArrayList<Rectangle> out = new ArrayList<>();
        boolean horizontal = side==1 || side==3;
        int rw = module.roomWidth;
        int rh = module.roomHeight;
        int count = Math.min(module.rooms.length, module.maxRooms);
        for(int i=0;i<count;i++){
            int lane = i % 2;
            int step = i / 2;
            if(horizontal){
                int usable = Math.max(1, corridor.width - rw - 2);
                int base = Math.max(1, Math.min(corridor.width - rw - 1, 2 + step * (rw + 1)));
                int rx = side==1 ? corridor.x + base : corridor.x + corridor.width - base - rw;
                int ry = lane==0 ? corridor.y-rh : corridor.y+corridor.height;
                out.add(new Rectangle(rx, ry, rw, rh));
            } else {
                int base = Math.max(1, Math.min(corridor.height - rh - 1, 2 + step * (rh + 1)));
                int rx = lane==0 ? corridor.x-rw : corridor.x+corridor.width;
                int ry = side==2 ? corridor.y + base : corridor.y + corridor.height - base - rh;
                out.add(new Rectangle(rx, ry, rw, rh));
            }
        }
        return out;
    }

    void dressModuleCorridor(Rectangle corridor, StampedModuleSpec module){
        if(corridor == null || module == null) return;
        boolean horizontal = corridor.width >= corridor.height;
        if(module.corridorDress == null || module.corridorDress.length()==0) return;
        if(module.corridorDress.contains("CAFETERIA")){
            if(horizontal){
                int y = corridor.y + corridor.height/2;
                for(int x=corridor.x+1; x<corridor.x+corridor.width-1; x+=3){ if(inBounds(x,y)) tiles[x][y]='T'; if(inBounds(x+1,y)) tiles[x+1][y]='b'; }
                if(inBounds(corridor.x+corridor.width-2, y)) tiles[corridor.x+corridor.width-2][y]='N';
            } else {
                int x = corridor.x + corridor.width/2;
                for(int y=corridor.y+1; y<corridor.y+corridor.height-1; y+=3){ if(inBounds(x,y)) tiles[x][y]='T'; if(inBounds(x,y+1)) tiles[x][y+1]='b'; }
                if(inBounds(x, corridor.y+corridor.height-2)) tiles[x][corridor.y+corridor.height-2]='N';
            }
        } else if(module.corridorDress.contains("CELL_ROW")){
            for(int x=corridor.x; x<corridor.x+corridor.width; x++) for(int y=corridor.y; y<corridor.y+corridor.height; y++) if(inBounds(x,y) && ((horizontal?x:y)%3==0)) tiles[x][y]='X';
        } else if(module.corridorDress.contains("MARKET_ROW")){
            for(int x=corridor.x; x<corridor.x+corridor.width; x+=3) for(int y=corridor.y; y<corridor.y+corridor.height; y++) if(inBounds(x,y)) tiles[x][y]='$';
        } else if(module.corridorDress.contains("DATA_STACK")){
            for(int x=corridor.x; x<corridor.x+corridor.width; x+=2) for(int y=corridor.y; y<corridor.y+corridor.height; y+=2) if(inBounds(x,y)) tiles[x][y]='l';
        }
    }

    boolean stampDormitorySegment(Rectangle plaza, int side){
        // Long trunk corridor with small hab-cell rooms directly off the sides.
        // Each dorm cell is constrained to a tiny 3x4 footprint so it behaves like
        // the requested 2x4 micro-room while still leaving at least one interior tile
        // for an inspectable cot/sink/storage feature in the current tile renderer.
        boolean horizontal = side==1 || side==3;
        int len = 18, cw = 3, cellW = horizontal ? 3 : 4, cellH = horizontal ? 4 : 3;
        int cx, cy;
        if(side==1){ cx = plaza.x+plaza.width; cy = plaza.y+plaza.height/2-1; }
        else if(side==3){ cx = plaza.x-len; cy = plaza.y+plaza.height/2-1; }
        else if(side==0){ cx = plaza.x+plaza.width/2-1; cy = plaza.y-len; }
        else { cx = plaza.x+plaza.width/2-1; cy = plaza.y+plaza.height; }
        Rectangle corridor = horizontal ? new Rectangle(cx, cy, len, cw) : new Rectangle(cx, cy, cw, len);
        java.util.List<Rectangle> candidateRooms = new ArrayList<>();
        for(int i=0;i<4;i++){
            int offset = 3 + i*4;
            if(horizontal){
                int rx = (side==1 ? corridor.x+offset : corridor.x+corridor.width-offset-cellW);
                candidateRooms.add(new Rectangle(rx, corridor.y-cellH, cellW, cellH));
                candidateRooms.add(new Rectangle(rx, corridor.y+corridor.height, cellW, cellH));
            } else {
                int ry = (side==2 ? corridor.y+offset : corridor.y+corridor.height-offset-cellH);
                candidateRooms.add(new Rectangle(corridor.x-cellW, ry, cellW, cellH));
                candidateRooms.add(new Rectangle(corridor.x+corridor.width, ry, cellW, cellH));
            }
        }
        // keep only a handful so modules do not overrun the room quota too aggressively
        while(candidateRooms.size()>4) candidateRooms.remove(candidateRooms.size()-1);
        if(!moduleAreaLegal(corridor, candidateRooms)) return false;
        char cg = zoneType.corridorGlyph(r);
        for(int x=corridor.x; x<corridor.x+corridor.width; x++) for(int y=corridor.y; y<corridor.y+corridor.height; y++) if(inBounds(x,y)) tiles[x][y]=cg;
        Point pd = plazaDoorForModule(plaza, corridor);
        if(pd != null && inBounds(pd.x,pd.y)) tiles[pd.x][pd.y] = DoorType.forZone(zoneType, r).symbol;
        for(Rectangle rr: candidateRooms){
            int idx=rooms.size(); carve(rr); rooms.add(rr);
            if(idx < roomProfiles.size()) roomProfiles.set(idx, RoomProfile.dormitoryCell(zoneType, r));
            placeDormitoryFeatures(rr);
            Point d = doorBetweenRoomAndCorridor(rr, corridor);
            if(d != null && inBounds(d.x,d.y)) tiles[d.x][d.y] = DoorType.forZone(zoneType, r).symbol;
        }
        return true;
    }

    boolean stampApartmentCluster(Rectangle plaza, int side){
        // A modest, predictable apartment segment: one short corridor spine with
        // a living room plus bedroom/bath/dining cells, all 4x4 footprints.
        boolean horizontal = side==1 || side==3;
        int len = 14, cw = 3, roomSize = 4;
        int cx, cy;
        if(side==1){ cx = plaza.x+plaza.width; cy = plaza.y+plaza.height/2+5; }
        else if(side==3){ cx = plaza.x-len; cy = plaza.y+plaza.height/2+5; }
        else if(side==0){ cx = plaza.x+plaza.width/2+5; cy = plaza.y-len; }
        else { cx = plaza.x+plaza.width/2+5; cy = plaza.y+plaza.height; }
        Rectangle corridor = horizontal ? new Rectangle(cx, cy, len, cw) : new Rectangle(cx, cy, cw, len);
        java.util.List<Rectangle> candidateRooms = new ArrayList<>();
        if(horizontal){
            int baseX = side==1 ? corridor.x+3 : corridor.x+corridor.width-7;
            candidateRooms.add(new Rectangle(baseX, corridor.y-roomSize, roomSize, roomSize));
            candidateRooms.add(new Rectangle(baseX+5*(side==1?1:-1), corridor.y-roomSize, roomSize, roomSize));
            candidateRooms.add(new Rectangle(baseX, corridor.y+corridor.height, roomSize, roomSize));
            candidateRooms.add(new Rectangle(baseX+5*(side==1?1:-1), corridor.y+corridor.height, roomSize, roomSize));
        } else {
            int baseY = side==2 ? corridor.y+3 : corridor.y+corridor.height-7;
            candidateRooms.add(new Rectangle(corridor.x-roomSize, baseY, roomSize, roomSize));
            candidateRooms.add(new Rectangle(corridor.x-roomSize, baseY+5*(side==2?1:-1), roomSize, roomSize));
            candidateRooms.add(new Rectangle(corridor.x+corridor.width, baseY, roomSize, roomSize));
            candidateRooms.add(new Rectangle(corridor.x+corridor.width, baseY+5*(side==2?1:-1), roomSize, roomSize));
        }
        if(!moduleAreaLegal(corridor, candidateRooms)) return false;
        char cg = zoneType.corridorGlyph(r);
        for(int x=corridor.x; x<corridor.x+corridor.width; x++) for(int y=corridor.y; y<corridor.y+corridor.height; y++) if(inBounds(x,y)) tiles[x][y]=cg;
        Point pd = plazaDoorForModule(plaza, corridor);
        if(pd != null && inBounds(pd.x,pd.y)) tiles[pd.x][pd.y] = DoorType.forZone(zoneType, r).symbol;
        String[] labels = {"Apartment Living Room", "Apartment Bedroom", "Apartment Washroom", "Apartment Dining Nook"};
        for(int i=0;i<candidateRooms.size();i++){
            Rectangle rr = candidateRooms.get(i);
            int idx=rooms.size(); carve(rr); rooms.add(rr);
            if(idx < roomProfiles.size()) roomProfiles.set(idx, RoomProfile.apartmentRoom(labels[i], zoneType, r));
            placeApartmentFeatures(rr, i);
            Point d = doorBetweenRoomAndCorridor(rr, corridor);
            if(d != null && inBounds(d.x,d.y)) tiles[d.x][d.y] = DoorType.forZone(zoneType, r).symbol;
        }
        return true;
    }

    boolean moduleAreaLegal(Rectangle corridor, java.util.List<Rectangle> moduleRooms){
        // 0.8.25 CORRIDOR SHAPE AUTHORITY:
        // Module corridors may be double-/triple-wide service ways, but they must still read
        // as corridors: their length must exceed their width. This prevents square blobs from
        // being stamped as corridor space and preserves branchable hallway geometry.
        if(!validCorridorRectangleShape(corridor)) return false;
        Rectangle grownC = new Rectangle(corridor.x-1, corridor.y-1, corridor.width+2, corridor.height+2);
        if(!rectInMap(grownC)) return false;
        for(int x=corridor.x; x<corridor.x+corridor.width; x++) for(int y=corridor.y; y<corridor.y+corridor.height; y++){
            if(!inBounds(x,y) || roomIds[x][y] >= 0 || tiles[x][y] != '#') return false;
        }
        for(Rectangle rr: moduleRooms){
            Rectangle grown = new Rectangle(rr.x-1, rr.y-1, rr.width+2, rr.height+2);
            if(!rectInMap(grown)) return false;
            for(int x=grown.x; x<grown.x+grown.width; x++) for(int y=grown.y; y<grown.y+grown.height; y++){
                if(!inBounds(x,y)) return false;
                if(roomIds[x][y] >= 0 || tiles[x][y] != '#') return false;
            }
        }
        return true;
    }

    boolean rectInMap(Rectangle rr){ return rr.x>=1 && rr.y>=1 && rr.x+rr.width < w-1 && rr.y+rr.height < h-1; }

    boolean validCorridorRectangleShape(Rectangle corridor){
        if(corridor == null) return false;
        int len = Math.max(corridor.width, corridor.height);
        int wide = Math.min(corridor.width, corridor.height);
        return len >= 2 && len > wide;
    }

    Point plazaDoorForModule(Rectangle plaza, Rectangle corridor){
        Point desired = null;
        if(corridor.x == plaza.x+plaza.width) desired = new Point(plaza.x+plaza.width-1, Math.max(plaza.y+1, Math.min(plaza.y+plaza.height-2, corridor.y+corridor.height/2)));
        else if(corridor.x+corridor.width == plaza.x) desired = new Point(plaza.x, Math.max(plaza.y+1, Math.min(plaza.y+plaza.height-2, corridor.y+corridor.height/2)));
        else if(corridor.y == plaza.y+plaza.height) desired = new Point(Math.max(plaza.x+1, Math.min(plaza.x+plaza.width-2, corridor.x+corridor.width/2)), plaza.y+plaza.height-1);
        else if(corridor.y+corridor.height == plaza.y) desired = new Point(Math.max(plaza.x+1, Math.min(plaza.x+plaza.width-2, corridor.x+corridor.width/2)), plaza.y);
        if(desired == null) return null;
        return adjustedPlazaDoorWithClearance(plaza, desired, 3);
    }

    void seedCompassPlazaCorridors(Rectangle plaza){
        // 0.8.23 PLAZA SPOKE PASS:
        // Give the central plaza long cardinal trunks before organic room retries begin.
        // These trunks create branchable circulation space and stop the generator from
        // depending on tiny one-off door proposals around a 15x15 edge.
        int len = Math.max(7, Math.min(12, Math.min(w, h) / 8));
        int[][] dirs = {{0,-1},{1,0},{0,1},{-1,0}};
        int made = 0;
        for(int i=0;i<dirs.length;i++){
            Point door = centralPlazaDoor(plaza, i);
            door = adjustedPlazaDoorWithClearance(plaza, door, 3);
            if(door == null || !inBounds(door.x, door.y)) continue;
            Point start = outsideOfDoor(plaza, door);
            if(start == null || !inBounds(start.x, start.y)) continue;
            if(roomIds[start.x][start.y] >= 0) continue;
            if(tiles[start.x][start.y] != '#' && !isCorridorGlyph(tiles[start.x][start.y])) continue;
            tiles[door.x][door.y] = DoorType.forZone(zoneType, r).symbol;
            int carved = carveCompassCorridor(start.x, start.y, dirs[i][0], dirs[i][1], len);
            if(carved > 0) made++;
        }
        DebugLog.audit("LEVELGEN_PLAZA_SPOKES", "seeded compass plaza corridors="+made+" len="+len+" zone="+zoneType.label+" sewer="+sewerLayer);
    }

    Point centralPlazaDoor(Rectangle plaza, int side){
        if(side==0) return new Point(plaza.x + plaza.width/2, plaza.y);
        if(side==1) return new Point(plaza.x + plaza.width-1, plaza.y + plaza.height/2);
        if(side==2) return new Point(plaza.x + plaza.width/2, plaza.y + plaza.height-1);
        return new Point(plaza.x, plaza.y + plaza.height/2);
    }

    int carveCompassCorridor(int sx, int sy, int dx, int dy, int len){
        char cg = zoneType.corridorGlyph(r);
        int carved = 0;
        int x = sx, y = sy;
        for(int i=0;i<len;i++, x+=dx, y+=dy){
            if(!inBounds(x,y) || x<=0 || y<=0 || x>=w-1 || y>=h-1) break;
            if(roomIds[x][y] >= 0) break;
            if(tiles[x][y] != '#' && !isCorridorGlyph(tiles[x][y])) break;
            tiles[x][y] = cg;
            carved++;
        }
        return carved;
    }

    boolean isCorridorGlyph(char c){
        return c=='.' || c==',' || c==':' || c=='=' || c=='~' || c=='-' || c=='+' || c==';' || c=='_';
    }

    boolean isDoorSymbol(char c){
        return c=='/' || c=='|' || c=='L' || c=='X' || c=='V' || c=='Z';
    }

    ArrayList<Point> plazaDoorCandidatesCompassFirst(Rectangle plaza, Point target, ArrayList<Point> raw){
        ArrayList<Point> preferred = new ArrayList<>();
        for(int side=0; side<4; side++){
            Point p = adjustedPlazaDoorWithClearance(plaza, centralPlazaDoor(plaza, side), 3);
            if(p != null && containsPoint(raw, p) && !containsPoint(preferred, p)) preferred.add(p);
        }
        Collections.sort(raw, (p1,p2) -> Integer.compare(Math.abs(p1.x-target.x)+Math.abs(p1.y-target.y), Math.abs(p2.x-target.x)+Math.abs(p2.y-target.y)));
        for(Point p: raw) if(!containsPoint(preferred, p)) preferred.add(p);
        return preferred;
    }

    boolean containsPoint(java.util.List<Point> pts, Point p){
        for(Point q: pts) if(q.x==p.x && q.y==p.y) return true;
        return false;
    }

    Point adjustedPlazaDoorWithClearance(Rectangle plaza, Point desired, int minSpacing){
        if(plaza == null || desired == null) return desired;
        int side = plazaWallSide(plaza, desired);
        if(side < 0) return desired;
        ArrayList<Point> candidates = new ArrayList<>();
        candidates.add(desired);
        int maxShift = Math.max(plaza.width, plaza.height);
        for(int d=1; d<=maxShift; d++){
            Point a = shiftedAlongPlazaWall(plaza, desired, side, d);
            Point b = shiftedAlongPlazaWall(plaza, desired, side, -d);
            if(a != null) candidates.add(a);
            if(b != null) candidates.add(b);
        }
        for(Point p: candidates){
            if(p != null && plazaDoorHasWallClearance(plaza, p, minSpacing)) return p;
        }
        return desired;
    }

    int plazaWallSide(Rectangle plaza, Point p){
        if(p.y == plaza.y && p.x > plaza.x && p.x < plaza.x+plaza.width-1) return 0;
        if(p.x == plaza.x+plaza.width-1 && p.y > plaza.y && p.y < plaza.y+plaza.height-1) return 1;
        if(p.y == plaza.y+plaza.height-1 && p.x > plaza.x && p.x < plaza.x+plaza.width-1) return 2;
        if(p.x == plaza.x && p.y > plaza.y && p.y < plaza.y+plaza.height-1) return 3;
        return -1;
    }

    Point shiftedAlongPlazaWall(Rectangle plaza, Point p, int side, int delta){
        if(side==0 || side==2){
            int x = p.x + delta;
            if(x <= plaza.x || x >= plaza.x+plaza.width-1) return null;
            return new Point(x, p.y);
        } else {
            int y = p.y + delta;
            if(y <= plaza.y || y >= plaza.y+plaza.height-1) return null;
            return new Point(p.x, y);
        }
    }

    boolean plazaDoorHasWallClearance(Rectangle plaza, Point candidate, int minSpacing){
        int side = plazaWallSide(plaza, candidate);
        if(side < 0) return false;
        for(int x=plaza.x; x<plaza.x+plaza.width; x++){
            for(int y=plaza.y; y<plaza.y+plaza.height; y++){
                if(!isPlazaWallTile(plaza, x, y)) continue;
                if(x==candidate.x && y==candidate.y) continue;
                if(!isDoorSymbol(tiles[x][y])) continue;
                Point other = new Point(x,y);
                if(plazaWallSide(plaza, other) != side) continue;
                int dist = (side==0 || side==2) ? Math.abs(candidate.x-other.x) : Math.abs(candidate.y-other.y);
                if(dist < minSpacing) return false;
            }
        }
        return true;
    }

    Point adjustedLargeRoomDoorWithClearance(Rectangle rr, Point desired, int minSpacing){
        // 0.8.24 LARGE-ROOM DOOR CLEARANCE:
        // Apply the plaza spacing rule to any room larger than the baseline tiny room.
        // Large rooms need usable wall around each opening for furniture, labels, branch
        // corridors, and reserved door-state/cover logic instead of becoming combs of
        // adjacent doors on one wall.
        if(rr == null || desired == null || !isLargeRoomForDoorSpacing(rr)) return desired;
        Rectangle plaza = rooms.isEmpty() ? null : rooms.get(0);
        if(plaza != null && rr.equals(plaza)) return adjustedPlazaDoorWithClearance(rr, desired, minSpacing);
        int side = roomWallSide(rr, desired);
        if(side < 0) return desired;
        ArrayList<Point> candidates = new ArrayList<>();
        candidates.add(desired);
        int maxShift = Math.max(rr.width, rr.height);
        for(int d=1; d<=maxShift; d++){
            Point a = shiftedAlongRoomWall(rr, desired, side, d);
            Point b = shiftedAlongRoomWall(rr, desired, side, -d);
            if(a != null) candidates.add(a);
            if(b != null) candidates.add(b);
        }
        for(Point p: candidates){
            if(p != null && roomDoorHasWallClearance(rr, p, minSpacing)) return p;
        }
        return desired;
    }

    boolean isSmallRoomOneDoorPerWall(Rectangle rr){
        // 0.8.25: baseline 4x4 rooms and smaller micro-cells may expose one doorway
        // per wall at most. A tiny room can still connect north/south/east/west, but
        // never receives multiple adjacent doors along the same wall.
        return rr != null && rr.width <= 4 && rr.height <= 4;
    }

    boolean isLargeRoomForDoorSpacing(Rectangle rr){
        // The current smallest deliberate room footprint is 4x4. Rooms larger than
        // that footprint get same-wall clearance enforcement.
        return rr != null && (rr.width > 4 || rr.height > 4);
    }

    boolean roomWallAlreadyHasDoor(Rectangle rr, int side){
        if(rr == null || side < 0) return false;
        for(int x=rr.x; x<rr.x+rr.width; x++){
            for(int y=rr.y; y<rr.y+rr.height; y++){
                if(!isRoomWallTile(rr, x, y)) continue;
                Point p = new Point(x,y);
                if(roomWallSide(rr, p) == side && isDoorSymbol(tiles[x][y])) return true;
            }
        }
        return false;
    }

    int roomWallSide(Rectangle rr, Point p){
        if(p.y == rr.y && p.x > rr.x && p.x < rr.x+rr.width-1) return 0;
        if(p.x == rr.x+rr.width-1 && p.y > rr.y && p.y < rr.y+rr.height-1) return 1;
        if(p.y == rr.y+rr.height-1 && p.x > rr.x && p.x < rr.x+rr.width-1) return 2;
        if(p.x == rr.x && p.y > rr.y && p.y < rr.y+rr.height-1) return 3;
        return -1;
    }

    Point shiftedAlongRoomWall(Rectangle rr, Point p, int side, int delta){
        if(side==0 || side==2){
            int x = p.x + delta;
            if(x <= rr.x || x >= rr.x+rr.width-1) return null;
            return new Point(x, p.y);
        } else {
            int y = p.y + delta;
            if(y <= rr.y || y >= rr.y+rr.height-1) return null;
            return new Point(p.x, y);
        }
    }

    boolean roomDoorHasWallClearance(Rectangle rr, Point candidate, int minSpacing){
        int side = roomWallSide(rr, candidate);
        if(side < 0) return false;
        for(int x=rr.x; x<rr.x+rr.width; x++){
            for(int y=rr.y; y<rr.y+rr.height; y++){
                if(!isRoomWallTile(rr, x, y)) continue;
                if(x==candidate.x && y==candidate.y) continue;
                if(!isDoorSymbol(tiles[x][y])) continue;
                Point other = new Point(x,y);
                if(roomWallSide(rr, other) != side) continue;
                int dist = (side==0 || side==2) ? Math.abs(candidate.x-other.x) : Math.abs(candidate.y-other.y);
                if(dist < minSpacing) return false;
            }
        }
        return true;
    }

    boolean isRoomWallTile(Rectangle rr, int x, int y){
        boolean onWall = x==rr.x || x==rr.x+rr.width-1 || y==rr.y || y==rr.y+rr.height-1;
        boolean notCorner = !( (x==rr.x || x==rr.x+rr.width-1) && (y==rr.y || y==rr.y+rr.height-1) );
        return onWall && notCorner;
    }

    boolean isPlazaWallTile(Rectangle plaza, int x, int y){
        boolean onWall = x==plaza.x || x==plaza.x+plaza.width-1 || y==plaza.y || y==plaza.y+plaza.height-1;
        boolean notCorner = !( (x==plaza.x || x==plaza.x+plaza.width-1) && (y==plaza.y || y==plaza.y+plaza.height-1) );
        return onWall && notCorner;
    }

    Point doorBetweenRoomAndCorridor(Rectangle rr, Rectangle corridor){
        Point desired = null;
        if(rr.y+rr.height == corridor.y) desired = new Point(Math.max(rr.x+1, Math.min(rr.x+rr.width-2, rr.x+rr.width/2)), rr.y+rr.height-1);
        else if(rr.y == corridor.y+corridor.height) desired = new Point(Math.max(rr.x+1, Math.min(rr.x+rr.width-2, rr.x+rr.width/2)), rr.y);
        else if(rr.x+rr.width == corridor.x) desired = new Point(rr.x+rr.width-1, Math.max(rr.y+1, Math.min(rr.y+rr.height-2, rr.y+rr.height/2)));
        else if(rr.x == corridor.x+corridor.width) desired = new Point(rr.x, Math.max(rr.y+1, Math.min(rr.y+rr.height-2, rr.y+rr.height/2)));
        if(desired == null) return null;
        return adjustedLargeRoomDoorWithClearance(rr, desired, 3);
    }

    void placeDormitoryFeatures(Rectangle rr){
        int ix = rr.x+1, iy = rr.y+1;
        if(inBounds(ix,iy)) tiles[ix][iy] = 'c';
        if(inBounds(ix,iy+1)) tiles[ix][iy+1] = 'u';
        if(inBounds(ix+1,iy)) tiles[ix+1][iy] = 's';
    }

    void placeApartmentFeatures(Rectangle rr, int kind){
        int ix=rr.x+1, iy=rr.y+1;
        if(!inBounds(ix,iy)) return;
        if(kind==0){ tiles[ix][iy]='q'; if(inBounds(ix+1,iy)) tiles[ix+1][iy]='b'; }
        else if(kind==1){ tiles[ix][iy]='c'; if(inBounds(ix+1,iy)) tiles[ix+1][iy]='s'; }
        else if(kind==2){ tiles[ix][iy]='u'; if(inBounds(ix+1,iy)) tiles[ix+1][iy]='N'; }
        else { tiles[ix][iy]='T'; if(inBounds(ix+1,iy)) tiles[ix+1][iy]='b'; }
    }

    Faction plazaFactionForZone(){
        switch(zoneType){
            case GANGER_TURF: return Faction.BANDIT;
            case ARBITES_PRECINCT_EDGE: return Faction.CIVIC_WARDENS;
            case MECHANICUS_RELIC_DUCT:
            case MECHANICUS_FORGE_CLOISTER: return Faction.MECHANIST_COLLEGIA;
            case MUTANT_WARRENS:
            case MUTANT_SEWER_CAMP: return Faction.MUTANT;
            case CULTIST_SEWER_CAMP: return Faction.CULTIST;
            case ADMINISTRATUM_ARCHIVE: return Faction.CIVIC_LEDGER_OFFICE;
            case IMPERIAL_GUARD_BILLET: return Faction.IMPERIAL_GUARD;
            case NOBLE_SERVICE_SPINE: return Faction.NOBLE;
            default: return Faction.NONE;
        }
    }

    void decorateCentralPlaza(Rectangle plaza){
        char[] civic = {'T','q','b','h','n'};
        char[] gang = {'g','q','b','p','N'};
        char[] civicWardens = {'A','q','b','n','N'};
        char[] mech = {'N','R','Z','Y','J','P','F'};
        char[] sewerUtility = {'N','Z','Y','F','v','m','p'};
        char[] cult = {'c','c','o','b','p'};
        char[] mutant = {'m','m','o','b','p'};
        char[] noble = {'n','q','b','A','T'};
        char[] pool = civic;
        if(sewerLayer || zoneType==ZoneType.SEWER_CONDUIT || zoneType==ZoneType.MUTANT_SEWER_CAMP || zoneType==ZoneType.CULTIST_SEWER_CAMP) pool = sewerUtility;
        else if(zoneType==ZoneType.GANGER_TURF) pool = gang;
        else if(zoneType==ZoneType.ARBITES_PRECINCT_EDGE) pool = civicWardens;
        else if(zoneType==ZoneType.MECHANICUS_RELIC_DUCT || zoneType==ZoneType.MECHANICUS_FORGE_CLOISTER) pool = mech;
        else if(zoneType==ZoneType.CULTIST_SEWER_CAMP) pool = cult;
        else if(zoneType==ZoneType.MUTANT_WARRENS || zoneType==ZoneType.MUTANT_SEWER_CAMP) pool = mutant;
        else if(zoneType==ZoneType.NOBLE_SERVICE_SPINE) pool = noble;
        int count = 10 + r.nextInt(10);
        for(int i=0;i<count;i++){
            int x = plaza.x+2+r.nextInt(Math.max(1, plaza.width-4));
            int y = plaza.y+2+r.nextInt(Math.max(1, plaza.height-4));
            if(inBounds(x,y) && roomIds[x][y]==0 && tiles[x][y] != '#') tiles[x][y] = pool[r.nextInt(pool.length)];
        }
    }

    Rectangle proposeRoomNearPlazaBranch(int n){
        Rectangle p = rooms.isEmpty() ? centralPlazaRect() : rooms.get(0);
        int[] dims = zoneType.roomSize(r);
        int rw = Math.max(3, dims[0]), rh = Math.max(3, dims[1]);
        int dir = n % 4;
        int lane = n / 4;
        int gap = 3 + r.nextInt(6) + (lane%3);
        int x, y;
        if(dir==0){ // north
            x = p.x - 10 + r.nextInt(Math.max(1, p.width+20-rw));
            y = p.y - gap - rh;
        } else if(dir==1){ // east
            x = p.x + p.width + gap;
            y = p.y - 9 + r.nextInt(Math.max(1, p.height+18-rh));
        } else if(dir==2){ // south
            x = p.x - 10 + r.nextInt(Math.max(1, p.width+20-rw));
            y = p.y + p.height + gap;
        } else { // west
            x = p.x - gap - rw;
            y = p.y - 9 + r.nextInt(Math.max(1, p.height+18-rh));
        }
        x = Math.max(1, Math.min(w-rw-2, x));
        y = Math.max(1, Math.min(h-rh-2, y));
        return new Rectangle(x,y,rw,rh);
    }

    ConnectionChoice previewBestConnectionToConnectedSet(int newRoomIdx){
        if(newRoomIdx <= 0 || newRoomIdx >= rooms.size()) return null;
        ConnectionChoice best = null;
        for(int i=0;i<newRoomIdx;i++){
            ConnectionChoice cc = previewBestConnection(i, newRoomIdx);
            if(cc != null && (best == null || cc.score < best.score)) best = cc;
        }
        return best;
    }


    boolean connectAllRoomsStrict(){
        if(rooms.size() <= 1) return true;
        HashSet<Integer> connected = new HashSet<>();
        connected.add(0);
        int guard = 0;
        while(connected.size() < rooms.size() && guard++ < rooms.size()*rooms.size()*4){
            ConnectionChoice best = null;
            for(Integer ci: connected){
                for(int ui=0; ui<rooms.size(); ui++){
                    if(connected.contains(ui)) continue;
                    ConnectionChoice cc = previewBestConnection(ci, ui);
                    if(cc != null && (best == null || cc.score < best.score)) best = cc;
                }
            }
            if(best == null){
                DebugLog.audit("LEVELGEN_CONNECT_FAIL", "no legal corridor proposal could connect remaining rooms. connected="+connected.size()+" total="+rooms.size()+" zone="+zoneType.label);
                return false;
            }
            commitStrictConnection(best);
            connected.add(best.roomB);
            DebugLog.audit("LEVELGEN_CONNECT", "room="+best.roomA+"->"+best.roomB+" path="+best.path.size()+" doorA="+best.doorA.x+","+best.doorA.y+" doorB="+best.doorB.x+","+best.doorB.y);
        }
        return connected.size() == rooms.size();
    }

    static class ConnectionChoice {
        int roomA, roomB, score; Point doorA, doorB; java.util.List<Point> path; DoorType typeA, typeB;
        ConnectionChoice(int a,int b,int score,Point da,Point db,java.util.List<Point> path,DoorType ta,DoorType tb){this.roomA=a;this.roomB=b;this.score=score;this.doorA=da;this.doorB=db;this.path=path;this.typeA=ta;this.typeB=tb;}
    }

    ConnectionChoice previewBestConnection(int aIdx, int bIdx){
        Rectangle aRoom = rooms.get(aIdx), bRoom = rooms.get(bIdx);
        java.util.List<Point> aDoors = doorCandidates(aRoom, center(bRoom));
        java.util.List<Point> bDoors = doorCandidates(bRoom, center(aRoom));
        ConnectionChoice best = null;
        for(Point da: aDoors){
            Point start = outsideOfDoor(aRoom, da);
            if(!legalCorridorEndpoint(start)) continue;
            for(Point db: bDoors){
                Point end = outsideOfDoor(bRoom, db);
                if(!legalCorridorEndpoint(end)) continue;
                java.util.List<Point> path = corridorPathStrict(start, end);
                if(!validCorridorPathStrict(path)) continue;
                int score = path.size() + Math.abs(da.x-db.x) + Math.abs(da.y-db.y);
                if(best == null || score < best.score) best = new ConnectionChoice(aIdx,bIdx,score,da,db,path,DoorType.forZone(zoneType, r),DoorType.forZone(zoneType, r));
            }
        }
        return best;
    }

    java.util.List<Point> doorCandidates(Rectangle rr, Point target){
        ArrayList<Point> out = new ArrayList<>();
        for(int x=rr.x+1; x<rr.x+rr.width-1; x++){ out.add(new Point(x, rr.y)); out.add(new Point(x, rr.y+rr.height-1)); }
        for(int y=rr.y+1; y<rr.y+rr.height-1; y++){ out.add(new Point(rr.x, y)); out.add(new Point(rr.x+rr.width-1, y)); }
        Rectangle plaza = rooms.isEmpty() ? null : rooms.get(0);
        if(plaza != null && rr.equals(plaza)){
            out = plazaDoorCandidatesCompassFirst(plaza, target, out);
        } else {
            Collections.sort(out, (p1,p2) -> Integer.compare(Math.abs(p1.x-target.x)+Math.abs(p1.y-target.y), Math.abs(p2.x-target.x)+Math.abs(p2.y-target.y)));
            if(isLargeRoomForDoorSpacing(rr)){
                ArrayList<Point> spaced = new ArrayList<>();
                ArrayList<Point> overflow = new ArrayList<>();
                for(Point p: out){
                    Point adjusted = adjustedLargeRoomDoorWithClearance(rr, p, 3);
                    if(adjusted != null && !containsPoint(spaced, adjusted) && roomDoorHasWallClearance(rr, adjusted, 3)) spaced.add(adjusted);
                    if(!containsPoint(overflow, p)) overflow.add(p);
                }
                for(Point p: overflow) if(!containsPoint(spaced, p)) spaced.add(p);
                out = spaced;
            } else if(isSmallRoomOneDoorPerWall(rr)){
                ArrayList<Point> filtered = new ArrayList<>();
                boolean[] offeredSide = new boolean[4];
                for(Point p: out){
                    int side = roomWallSide(rr, p);
                    if(side < 0) continue;
                    // 4x4-style micro rooms can have one opening per wall, never a comb of
                    // adjacent small-room doors. Existing doors on the same wall block new
                    // candidates; otherwise we offer only the best target-facing candidate.
                    if(offeredSide[side] || roomWallAlreadyHasDoor(rr, side)) continue;
                    filtered.add(p);
                    offeredSide[side] = true;
                }
                out = filtered;
            }
        }
        // 0.8.23: plazas need broad edge availability. The old 24-candidate cap made a
        // 15x15 plaza behave as if half of its perimeter doors did not exist. Keep small-room
        // searches bounded, but allow plaza edges to be fully tested with compass points first.
        // 0.8.24: large non-plaza rooms receive the same same-wall spacing rule and a broader
        // candidate budget so clear openings are not lost behind nearest-target sorting.
        int cap = (plaza != null && rr.equals(plaza)) ? 64 : (isLargeRoomForDoorSpacing(rr) ? 48 : 24);
        if(out.size() > cap) return new ArrayList<Point>(out.subList(0,cap));
        return out;
    }

    Point outsideOfDoor(Rectangle rr, Point door){
        if(door.x == rr.x) return new Point(door.x-1, door.y);
        if(door.x == rr.x+rr.width-1) return new Point(door.x+1, door.y);
        if(door.y == rr.y) return new Point(door.x, door.y-1);
        if(door.y == rr.y+rr.height-1) return new Point(door.x, door.y+1);
        return new Point(door.x, door.y);
    }

    boolean legalCorridorEndpoint(Point p){
        return p != null && inBounds(p.x,p.y) && roomIds[p.x][p.y] < 0 && tiles[p.x][p.y] == '#';
    }

    java.util.List<Point> corridorPathStrict(Point start, Point end){
        // 0.8.61 dense-generation speed path: bounded Manhattan corridors instead of
        // full-grid BFS for every candidate door pair. Dense 20-30 room slices need fast
        // validation; if both L-shapes are blocked, the candidate is rejected and another
        // nearby room/door attempt is tried.
        if(!legalCorridorEndpoint(start) || !legalCorridorEndpoint(end)) return null;
        java.util.List<Point> a = manhattanCorridorPath(start, end, true);
        if(validCorridorPathStrict(a)) return a;
        java.util.List<Point> b = manhattanCorridorPath(start, end, false);
        if(validCorridorPathStrict(b)) return b;
        return null;
    }

    java.util.List<Point> manhattanCorridorPath(Point start, Point end, boolean horizontalFirst){
        ArrayList<Point> out = new ArrayList<>();
        int x=start.x, y=start.y;
        out.add(new Point(x,y));
        if(horizontalFirst){
            while(x != end.x){ x += x < end.x ? 1 : -1; out.add(new Point(x,y)); }
            while(y != end.y){ y += y < end.y ? 1 : -1; out.add(new Point(x,y)); }
        } else {
            while(y != end.y){ y += y < end.y ? 1 : -1; out.add(new Point(x,y)); }
            while(x != end.x){ x += x < end.x ? 1 : -1; out.add(new Point(x,y)); }
        }
        return out;
    }

    boolean validCorridorPathStrict(java.util.List<Point> path){
        // Corridors must be at least two tiles long once carved. Single-tile necks
        // are door stubs, not corridors, and they do not provide branch space.
        if(path == null || path.size() < 2) return false;
        HashSet<String> seen = new HashSet<>();
        for(Point p: path){
            if(!inBounds(p.x,p.y)) return false;
            if(roomIds[p.x][p.y] >= 0) return false;
            if(tiles[p.x][p.y] != '#') return false;
            if(!seen.add(p.x+","+p.y)) return false;
        }
        return true;
    }

    void commitStrictConnection(ConnectionChoice cc){
        tiles[cc.doorA.x][cc.doorA.y] = cc.typeA.symbol;
        tiles[cc.doorB.x][cc.doorB.y] = cc.typeB.symbol;
        char cg = zoneType.corridorGlyph(r);
        for(Point p: cc.path){
            if(inBounds(p.x,p.y) && roomIds[p.x][p.y] < 0 && tiles[p.x][p.y] == '#') tiles[p.x][p.y] = cg;
        }
        reinforceCorridorWalls(cc.path);
    }

    boolean allRoomsReachableStrict(){
        if(rooms.isEmpty()) return false;
        boolean[][] seen = reachableFromStart();
        for(int i=0;i<rooms.size();i++){
            Rectangle rr = rooms.get(i);
            boolean ok=false;
            for(int x=rr.x; x<rr.x+rr.width && !ok; x++) for(int y=rr.y; y<rr.y+rr.height; y++){
                if(inBounds(x,y) && roomIds[x][y]==i && seen[x][y]) { ok=true; break; }
            }
            if(!ok) return false;
        }
        return true;
    }

    void removeRoomAtIndex(int idx){
        if(idx <= 0 || idx >= rooms.size()) return;
        Rectangle rr = rooms.get(idx);
        for(int x=rr.x; x<rr.x+rr.width; x++) for(int y=rr.y; y<rr.y+rr.height; y++) if(inBounds(x,y) && roomIds[x][y]==idx){ roomIds[x][y] = -1; tiles[x][y] = '#'; }
        rooms.remove(idx); roomProfiles.remove(idx); roomFactions.remove(idx); roomSpecials.remove(idx);
        for(int x=0;x<w;x++) for(int y=0;y<h;y++) if(roomIds[x][y] > idx) roomIds[x][y]--;
    }

    void cullUnreachableRoomsHard(){
        boolean[][] seen = reachableFromStart();
        for(int i=rooms.size()-1; i>=1; i--){
            Rectangle rr = rooms.get(i); boolean ok=false;
            for(int x=rr.x; x<rr.x+rr.width && !ok; x++) for(int y=rr.y; y<rr.y+rr.height; y++) if(inBounds(x,y)&&roomIds[x][y]==i&&seen[x][y]) { ok=true; break; }
            if(!ok) { DebugLog.audit("ROOM_CONNECTIVITY", "hard-culling unreachable room id="+i+" rect="+rr.x+","+rr.y+","+rr.width+","+rr.height); removeRoomAtIndex(i); }
        }
    }

    boolean regionTouchesNonWall(Rectangle rr){ for(int x=Math.max(0,rr.x); x<Math.min(w,rr.x+rr.width); x++) for(int y=Math.max(0,rr.y); y<Math.min(h,rr.y+rr.height); y++) if(roomIds[x][y] >= 0 || tiles[x][y]!='#') return true; return false; }
    void carve(Rectangle rr){ int id=rooms.size(); RoomProfile rp = RoomProfile.forZone(zoneType, r); roomProfiles.add(rp); roomFactions.add(Faction.NONE); roomSpecials.add(Boolean.FALSE); char floorChar = zoneType.floorGlyph(r); for(int x=rr.x;x<rr.x+rr.width;x++) for(int y=rr.y;y<rr.y+rr.height;y++){ roomIds[x][y]=id; boolean wall = (x==rr.x || y==rr.y || x==rr.x+rr.width-1 || y==rr.y+rr.height-1); tiles[x][y]= wall ? '#' : floorChar; } }

    void buildGuaranteedRoomLattice(int target){
        target = WorldGenerationApi.clampRoomTarget(target);
        int cols = 6;
        int rows = 5;
        int m = safeRoomEdgeMargin();
        int usableW = Math.max(40, w - m * 2);
        int usableH = Math.max(40, h - m * 2);
        int cellW = Math.max(10, usableW / cols);
        int cellH = Math.max(8, usableH / rows);
        int placed = 0;
        for(int gy=0; gy<rows && rooms.size()<target; gy++){
            for(int gx=0; gx<cols && rooms.size()<target; gx++){
                int maxRw = Math.max(4, Math.min(10, cellW-4));
                int maxRh = Math.max(4, Math.min(8, cellH-4));
                int minRw = Math.min(5, maxRw);
                int minRh = Math.min(4, maxRh);
                int rw = minRw + r.nextInt(Math.max(1, maxRw-minRw+1));
                int rh = minRh + r.nextInt(Math.max(1, maxRh-minRh+1));
                int baseX = m + gx*cellW;
                int baseY = m + gy*cellH;
                int jitterX = Math.max(0, cellW-rw-2);
                int jitterY = Math.max(0, cellH-rh-2);
                int x = clampRoomX(baseX + (jitterX==0 ? 0 : r.nextInt(jitterX+1)), rw);
                int y = clampRoomY(baseY + (jitterY==0 ? 0 : r.nextInt(jitterY+1)), rh);
                if(x < m || y < m || x+rw >= w-m || y+rh >= h-m) continue;
                Rectangle rr = new Rectangle(x,y,rw,rh);
                Rectangle grown = new Rectangle(x-1,y-1,rw+2,rh+2);
                if(regionTouchesNonWall(grown)) continue;
                carve(rr);
                rooms.add(rr);
                // 0.9.10fs: large-zone lattice rooms must not rely solely on
                // later pairwise repair. Immediately anchor them toward the
                // central plaza network so the audit does not preserve isolated
                // border/lattice islands for inspection.
                connectRoomToPlazaFallback(centralPlazaRect(), rr);
                placed++;
            }
        }
        boolean connected = connectAllRoomsStrict();
        if(!connected || !allRoomsReachableStrict() || rooms.size() < 20){
            DebugLog.warn("LEVELGEN_LATTICE", "Guaranteed lattice needs post-repair connectivity. rooms="+rooms.size()+" connected="+connected+" reachable="+allRoomsReachableStrict()+" zone="+zoneType.label);
        } else {
            DebugLog.audit("LEVELGEN_LATTICE", "guaranteed connected room lattice rooms="+rooms.size()+" target="+target+" zone="+zoneType.label+" size="+w+"x"+h);
        }
    }
    Point center(Rectangle rr){return new Point(rr.x+rr.width/2,rr.y+rr.height/2);}    
    void connectRooms(Rectangle aRoom, Rectangle bRoom){
        Point a = adjustedLargeRoomDoorWithClearance(aRoom, edgePointToward(aRoom, center(bRoom)), 3);
        Point b = adjustedLargeRoomDoorWithClearance(bRoom, edgePointToward(bRoom, center(aRoom)), 3);
        if(!inBounds(a.x,a.y) || !inBounds(b.x,b.y)){
            DebugLog.error("LEVELGEN_CONNECT_BOUNDS", "Door endpoint outside map: a="+a.x+","+a.y+" b="+b.x+","+b.y+" size="+w+"x"+h, null);
            return;
        }
        DoorType da = DoorType.forZone(zoneType, r);
        DoorType db = DoorType.forZone(zoneType, r);
        tiles[a.x][a.y] = da.symbol;
        tiles[b.x][b.y] = db.symbol;
        carveCorridorBetween(a, b);
        DebugLog.audit("DOOR_GEN", "zone="+zoneType.label+" doorA="+da.label+" doorB="+db.label+" from="+a.x+","+a.y+" to="+b.x+","+b.y);
    }

    Point edgePointToward(Rectangle rr, Point target){
        int cx=rr.x+rr.width/2, cy=rr.y+rr.height/2;
        int dx=target.x-cx, dy=target.y-cy;
        if(Math.abs(dx) > Math.abs(dy)){
            int x = dx>=0 ? rr.x+rr.width-1 : rr.x;
            int y = Math.max(rr.y+1, Math.min(rr.y+rr.height-2, cy));
            return new Point(x,y);
        } else {
            int y = dy>=0 ? rr.y+rr.height-1 : rr.y;
            int x = Math.max(rr.x+1, Math.min(rr.x+rr.width-2, cx));
            return new Point(x,y);
        }
    }

    void carveCorridorBetween(Point a, Point b){
        Point start = adjacentOutsideDoor(a, b);
        Point end = adjacentOutsideDoor(b, a);
        java.util.List<Point> path = corridorPath(start, end);
        if(!validCorridorPath(path)) {
            DebugLog.audit("CORRIDOR_VALIDATE", "corridor rejected; creating door-authoritative closet stubs from="+a.x+","+a.y+" to="+b.x+","+b.y);
            createClosetBeyondDoor(a, b);
            createClosetBeyondDoor(b, a);
            return;
        }
        char cg = zoneType.corridorGlyph(r);
        for(Point p: path) if(inBounds(p.x,p.y) && roomIds[p.x][p.y] < 0 && tiles[p.x][p.y] == '#') tiles[p.x][p.y] = cg;
        reinforceCorridorWalls(path);
    }

    boolean validCorridorPath(java.util.List<Point> path){
        if(path == null || path.size() < 5) return false;
        HashSet<String> seen = new HashSet<>();
        for(Point p: path){
            if(!inBounds(p.x,p.y)) return false;
            if(roomIds[p.x][p.y] >= 0) return false;
            char t = tiles[p.x][p.y];
            if(t != '#') return false;
            String key=p.x+","+p.y;
            if(!seen.add(key)) return false;
        }
        return true;
    }

    void reinforceCorridorWalls(java.util.List<Point> path){
        // Corridors are carved through solid wall mass only. The untouched '#' around them
        // remains their side wall. We explicitly avoid changing existing room walls or rooms.
        for(Point p: path){
            for(int ox=-1; ox<=1; ox++) for(int oy=-1; oy<=1; oy++){
                if(Math.abs(ox)+Math.abs(oy)!=1) continue;
                int nx=p.x+ox, ny=p.y+oy;
                if(inBounds(nx,ny) && roomIds[nx][ny] < 0 && tiles[nx][ny] != '+' && tiles[nx][ny] != '=' && tiles[nx][ny] != '~' && tiles[nx][ny] != ',' && tiles[nx][ny] != ':' && tiles[nx][ny] != ';') {
                    if(tiles[nx][ny] == '#') tiles[nx][ny] = '#';
                }
            }
        }
    }

    void createClosetBeyondDoor(Point door, Point toward){
        Point p = adjacentOutsideDoor(door, toward);
        if(!inBounds(p.x,p.y) || tiles[p.x][p.y] != '#' || roomIds[p.x][p.y] >= 0) return;
        int dx = Integer.compare(p.x - door.x, 0);
        int dy = Integer.compare(p.y - door.y, 0);
        int cx = p.x, cy = p.y;
        if(!inBounds(cx,cy) || tiles[cx][cy] != '#') return;
        int id = rooms.size();
        Rectangle stub = new Rectangle(cx, cy, 1, 1);
        tiles[cx][cy] = zoneType.floorGlyph(r);
        roomIds[cx][cy] = id;
        rooms.add(stub);
        RoomProfile closet = RoomProfile.closetStub(zoneType, r);
        roomProfiles.add(closet);
        roomFactions.add(Faction.NONE);
        roomSpecials.add(Boolean.TRUE);
        // Place one inspectable feature if there is a safe tile just beyond the stub.
        int fx = cx + dx, fy = cy + dy;
        if(inBounds(fx,fy) && tiles[fx][fy] == '#') {
            tiles[fx][fy] = closet.contentSymbol(r);
        }
        DebugLog.audit("CLOSET_STUB", "door="+door.x+","+door.y+" stub="+cx+","+cy+" feature="+closet.featureText);
    }
    Point adjacentOutsideDoor(Point door, Point target){
        // Door-authoritative rule: the corridor tile is outside the room on the same
        // side as the target room. The previous implementation used the reverse
        // vector, which pointed back into the originating room and caused nearly all
        // corridors to be rejected as overlaps. This also contributed to occasional
        // edge indexing failures during large atlas generation.
        int dx = Integer.compare(target.x - door.x, 0);
        int dy = Integer.compare(target.y - door.y, 0);
        if(Math.abs(target.x-door.x) >= Math.abs(target.y-door.y) && dx != 0) return new Point(door.x + dx, door.y);
        if(dy != 0) return new Point(door.x, door.y + dy);
        return new Point(door.x, door.y);
    }
    java.util.List<Point> corridorPath(Point start, Point end){
        if(start == null || end == null || !inBounds(start.x,start.y)||!inBounds(end.x,end.y)) {
            DebugLog.audit("CORRIDOR_VALIDATE", "path endpoints out of bounds start="+(start==null?"null":start.x+","+start.y)+" end="+(end==null?"null":end.x+","+end.y)+" size="+w+"x"+h);
            return null;
        }
        boolean[][] seen=new boolean[w][h]; Point[][] prev=new Point[w][h]; ArrayDeque<Point> q=new ArrayDeque<>();
        seen[start.x][start.y]=true; q.add(start); int[] dx={1,-1,0,0}, dy={0,0,1,-1};
        while(!q.isEmpty()){
            Point p=q.removeFirst(); if(p.equals(end)) break;
            for(int i=0;i<4;i++){int nx=p.x+dx[i], ny=p.y+dy[i];
                if(!inBounds(nx,ny)||seen[nx][ny]) continue;
                boolean endpoint = (nx==end.x && ny==end.y);
                if(!endpoint && roomIds[nx][ny] >= 0) continue;
                if(!endpoint && tiles[nx][ny] != '#') continue;
                seen[nx][ny]=true; prev[nx][ny]=p; q.add(new Point(nx,ny));
            }
        }
        if(!inBounds(end.x,end.y) || !seen[end.x][end.y]) return null;
        ArrayList<Point> out=new ArrayList<>(); Point cur=end;
        while(cur!=null && inBounds(cur.x,cur.y) && !(cur.x==start.x && cur.y==start.y)){ out.add(cur); cur=prev[cur.x][cur.y]; }
        if(cur == null) return null;
        out.add(start); Collections.reverse(out); return out;
    }
    void carveCorridorFallback(Point a, Point b){
        // Deprecated emergency path: kept only for older saved atlas data. The current generator
        // uses validCorridorPath + closet fallback instead of carving risky L-corridors.
        ArrayList<Point> trial = new ArrayList<>();
        int x=a.x, y=a.y;
        while(x!=b.x){ if(!inBounds(x,y) || roomIds[x][y] >= 0 || tiles[x][y] != '#') { DebugLog.audit("CORRIDOR_VALIDATE", "fallback rejected horizontal overlap"); return; } trial.add(new Point(x,y)); x += x<b.x?1:-1; }
        while(y!=b.y){ if(!inBounds(x,y) || roomIds[x][y] >= 0 || tiles[x][y] != '#') { DebugLog.audit("CORRIDOR_VALIDATE", "fallback rejected vertical overlap"); return; } trial.add(new Point(x,y)); y += y<b.y?1:-1; }
        if(!validCorridorPath(trial)) { DebugLog.audit("CORRIDOR_VALIDATE", "fallback rejected by strict validation"); return; }
        char cg = zoneType.corridorGlyph(r);
        for(Point p: trial) tiles[p.x][p.y]=cg;
        reinforceCorridorWalls(trial);
    }
    void stampZoneExits(){
        // Edge transitions must be reachable. We carve from an edge marker to the nearest generated room center.
        placeEdgeDoor('N'); placeEdgeDoor('S'); placeEdgeDoor('W'); placeEdgeDoor('E');
        placeSpecialInReachableRoom(sewerLayer ? 'v' : 'S', 0.18, 0.20);
        placeSpecialInReachableRoom('E', 0.82, 0.20);
    }

    void placeEdgeDoor(char edge){
        // 0.9.10ji: legacy callers are redirected away from literal edge stamping.
        // Double-door transitions now belong at road ends abutting the inner
        // maintenance-corridor bulkhead, never on the map's outermost tile edge.
        stampTransitionDoorsOnMaintenanceSide(edge);
    }

    Point carveEdgeConnector(char edge, int ex, int ey, int dx, int dy, int len){
        char cg = zoneType.corridorGlyph(r);
        Point last = null;
        for(int i=1; i<=len; i++){
            int bx = ex + dx*i;
            int by = ey + dy*i;
            if(!inBounds(bx,by) || bx<=0 || by<=0 || bx>=w-1 || by>=h-1) break;
            if(edge=='N' || edge=='S'){
                for(int ox=0; ox<2; ox++){
                    int x = bx+ox, y = by;
                    if(inBounds(x,y) && roomIds[x][y] < 0 && (tiles[x][y]=='#' || isCorridorGlyph(tiles[x][y]))) tiles[x][y] = cg;
                }
                last = new Point(bx, by);
            } else {
                for(int oy=0; oy<2; oy++){
                    int x = bx, y = by+oy;
                    if(inBounds(x,y) && roomIds[x][y] < 0 && (tiles[x][y]=='#' || isCorridorGlyph(tiles[x][y]))) tiles[x][y] = cg;
                }
                last = new Point(bx, by);
            }
        }
        if(last != null) DebugLog.audit("EDGE_CONNECTOR", "edge="+edge+" len="+len+" end="+last.x+","+last.y+" zone="+zoneType.label);
        return last;
    }

    void placeSpecialInReachableRoom(char symbol, double fx, double fy){
        if (rooms.isEmpty()) return;
        int tx=(int)(w*fx), ty=(int)(h*fy); Rectangle best=rooms.get(0); double bestD=Double.MAX_VALUE;
        for(Rectangle rr: rooms){ Point c=center(rr); double d=Math.abs(c.x-tx)+Math.abs(c.y-ty); if(d<bestD){bestD=d; best=rr;} }
        Point c=center(best); if(inBounds(c.x,c.y)) tiles[c.x][c.y]=symbol;
    }

    void carveLine(Point a, Point b){
        int x=a.x, y=a.y;
        char cg = zoneType.corridorGlyph(r);
        while(x!=b.x){ carveConnectorTile(x,y,cg); x += x<b.x?1:-1; }
        while(y!=b.y){ carveConnectorTile(x,y,cg); y += y<b.y?1:-1; }
        carveConnectorTile(x,y,cg);
    }

    boolean inBounds(int x,int y){ return x>=0&&y>=0&&x<w&&y<h; }

    void validateSpecialTransitions(){
        boolean[][] seen = reachableFromStart();
        int fixed=0;
        for(int x=0;x<w;x++) for(int y=0;y<h;y++) if((tiles[x][y]=='D'||tiles[x][y]=='S'||tiles[x][y]=='v'||tiles[x][y]=='E') && !seen[x][y]){
            Point nearest = nearestReachable(seen, x, y);
            if(nearest != null){ carveLine(new Point(x,y), nearest); fixed++; }
        }
        if(fixed>0) DebugLog.audit("MAP_VALIDATE", "carved reachability repairs for transitions=" + fixed);
    }


    int repairWorldgenValidationIssues(String phase){
        // 0.8.29 WORLDGEN REPAIR PASS:
        // The validator is now allowed to perform conservative repairs before population.
        // Repairs favor playability and navigation sanity: connect isolated rooms, rebuild
        // missing transition access corridors, erase tiny dangling corridor flecks, and remove
        // extra same-wall doors only when another legal door remains available.
        // 0.9.10ji: literal map-edge double doors are forbidden by design; transition
        // doors are stamped against the inner maintenance bulkhead instead, so the
        // old edge-door repair pass must not recreate the obsolete edge band.
        int repairs = 0;
        repairs += repairUnreachableRooms(phase);
        repairs += repairWeakDoors(phase);
        repairs += repairDoorSpacingViolations(phase);
        repairs += repairCorridorFragments(phase);
        if(repairs > 0) DebugLog.audit("WORLDGEN_REPAIR", "phase="+phase+" repairs="+repairs+" status=REPAIRED zone="+zoneType.label+" layer="+layerText()+" seed="+seed);
        else DebugLog.audit("WORLDGEN_REPAIR", "phase="+phase+" repairs=0 status=ACCEPTED zone="+zoneType.label+" layer="+layerText()+" seed="+seed);
        return repairs;
    }

    int repairUnreachableRooms(String phase){
        int repairs = 0;
        boolean[][] seen = reachableFromStart();
        for(int i=0;i<rooms.size();i++){
            Rectangle rr = rooms.get(i);
            Point rp = representativeRoomWalkablePoint(i);
            if(rp == null) continue;
            if(seen[rp.x][rp.y]) continue;
            Point nearest = nearestReachable(seen, rp.x, rp.y);
            if(nearest == null) continue;
            carveLine(nearest, rp);
            repairs++;
            DebugLog.audit("WORLDGEN_REPAIR", "phase="+phase+" connectedUnreachableRoom id="+i+" from="+nearest.x+","+nearest.y+" to="+rp.x+","+rp.y+" rect="+rectText(rr)+" zone="+zoneType.label);
            seen = reachableFromStart();
        }
        return repairs;
    }

    Point representativeRoomWalkablePoint(int roomIndex){
        if(roomIndex < 0 || roomIndex >= rooms.size()) return null;
        Rectangle rr = rooms.get(roomIndex);
        Point c = center(rr);
        if(inBounds(c.x,c.y) && roomIds[c.x][c.y]==roomIndex && walkable(c.x,c.y)) return c;
        for(int x=rr.x;x<rr.x+rr.width;x++) for(int y=rr.y;y<rr.y+rr.height;y++){
            if(inBounds(x,y) && roomIds[x][y]==roomIndex && walkable(x,y)) return new Point(x,y);
        }
        return null;
    }

    int repairEdgeDoorConnectors(String phase){
        int repairs = 0;
        char[] edges = {'N','S','W','E'};
        for(char edge: edges){
            int before = repairs;
            boolean found = false;
            for(int x=0;x<w;x++) for(int y=0;y<h;y++){
                if(tiles[x][y] != 'D' || !isEdgeTileFor(edge,x,y)) continue;
                found = true;
                if(!edgeDoorHasInwardConnector(edge, x, y, 16)){
                    Point end = carveConnectorFromExistingEdgeDoor(edge, x, y, 8 + r.nextInt(9));
                    Point nearest = nearestWalkableTo(end.x, end.y);
                    carveLine(end, nearest);
                    repairs++;
                    DebugLog.audit("WORLDGEN_REPAIR", "phase="+phase+" rebuiltEdgeConnector edge="+edge+" door="+x+","+y+" end="+end.x+","+end.y+" nearest="+nearest.x+","+nearest.y+" zone="+zoneType.label);
                }
            }
            if(!found){
                placeEdgeDoor(edge);
                repairs++;
                DebugLog.audit("WORLDGEN_REPAIR", "phase="+phase+" createdMissingEdgeDoor edge="+edge+" zone="+zoneType.label);
            }
            if(repairs == before) DebugLog.audit("WORLDGEN_REPAIR", "phase="+phase+" edge="+edge+" connectorsOk zone="+zoneType.label);
        }
        return repairs;
    }

    boolean isEdgeTileFor(char edge, int x, int y){
        return (edge=='N' && y==0) || (edge=='S' && y==h-1) || (edge=='W' && x==0) || (edge=='E' && x==w-1);
    }

    Point carveConnectorFromExistingEdgeDoor(char edge, int ex, int ey, int len){
        int dx=0, dy=0;
        if(edge=='N') dy=1; else if(edge=='S') dy=-1; else if(edge=='W') dx=1; else if(edge=='E') dx=-1;
        return carveEdgeConnector(edge, ex, ey, dx, dy, Math.max(4, Math.min(16, len)));
    }

    int repairWeakDoors(String phase){
        int repairs = 0;
        for(int x=1;x<w-1;x++) for(int y=1;y<h-1;y++){
            if(!isDoorSymbol(tiles[x][y])) continue;
            if(countAdjacentNonWall(x,y) >= 2) continue;
            Point nearest = nearestWalkableTo(x,y);
            carveLine(new Point(x,y), nearest);
            repairs++;
            DebugLog.audit("WORLDGEN_REPAIR", "phase="+phase+" strengthenedWeakDoor at="+x+","+y+" to="+nearest.x+","+nearest.y+" zone="+zoneType.label);
        }
        return repairs;
    }

    int repairDoorSpacingViolations(String phase){
        int repairs = 0;
        for(int i=0;i<rooms.size();i++){
            Rectangle rr = rooms.get(i);
            for(int side=0; side<4; side++){
                ArrayList<Point> pts = doorsOnRoomWall(rr, side);
                if(isSmallRoomOneDoorPerWall(rr) && pts.size() > 1){
                    for(int n=1; n<pts.size(); n++){
                        Point p = pts.get(n);
                        tiles[p.x][p.y] = '#';
                        repairs++;
                        DebugLog.audit("WORLDGEN_REPAIR", "phase="+phase+" sealedExtraSmallRoomDoor room="+i+" side="+side+" at="+p.x+","+p.y+" zone="+zoneType.label);
                    }
                } else if(isLargeRoomForDoorSpacing(rr) && pts.size() > 1){
                    final int wallSide = side;
                    Collections.sort(pts, (a,b) -> Integer.compare(wallSide==0||wallSide==2 ? a.x : a.y, wallSide==0||wallSide==2 ? b.x : b.y));
                    Point keep = pts.get(0);
                    for(int n=1; n<pts.size(); n++){
                        Point p = pts.get(n);
                        int dist = (side==0 || side==2) ? Math.abs(p.x-keep.x) : Math.abs(p.y-keep.y);
                        if(dist < 3){
                            tiles[p.x][p.y] = '#';
                            repairs++;
                            DebugLog.audit("WORLDGEN_REPAIR", "phase="+phase+" sealedCrowdedLargeRoomDoor room="+i+" side="+side+" at="+p.x+","+p.y+" kept="+keep.x+","+keep.y+" zone="+zoneType.label);
                        } else keep = p;
                    }
                }
            }
        }
        return repairs;
    }

    int repairCorridorFragments(String phase){
        int repairs = 0;
        boolean[][] checked = new boolean[w][h];
        for(int x=0;x<w;x++) for(int y=0;y<h;y++){
            if(checked[x][y] || roomIds[x][y] >= 0 || !isCorridorGlyph(tiles[x][y])) continue;
            ArrayList<Point> pts = collectCorridorComponentPoints(x, y, checked);
            CorridorComponent cc = summarizeCorridorComponent(pts);
            if(cc.size < 2){
                for(Point p: pts) if(inBounds(p.x,p.y) && roomIds[p.x][p.y] < 0) tiles[p.x][p.y] = '#';
                repairs++;
                DebugLog.audit("WORLDGEN_REPAIR", "phase="+phase+" removedShortCorridor size="+cc.size+" bounds="+cc.boundsText()+" zone="+zoneType.label);
            } else if(cc.size > 1 && cc.longAxis() <= cc.shortAxis()){
                Point a = pts.get(0);
                Point b = farthestPointInComponent(a, pts);
                if(b != null){
                    int dx = Math.abs(b.x-a.x) >= Math.abs(b.y-a.y) ? (b.x>=a.x?1:-1) : 0;
                    int dy = dx==0 ? (b.y>=a.y?1:-1) : 0;
                    int sx = b.x + dx, sy = b.y + dy;
                    int added = carveCompassCorridor(sx, sy, dx, dy, 2);
                    if(added > 0){
                        repairs++;
                        DebugLog.audit("WORLDGEN_REPAIR", "phase="+phase+" lengthenedSquatCorridor added="+added+" bounds="+cc.boundsText()+" zone="+zoneType.label);
                    }
                }
            }
        }
        return repairs;
    }

    ArrayList<Point> collectCorridorComponentPoints(int sx, int sy, boolean[][] checked){
        ArrayList<Point> pts = new ArrayList<>();
        ArrayDeque<Point> q = new ArrayDeque<>();
        checked[sx][sy]=true; q.add(new Point(sx,sy));
        int[] dx={1,-1,0,0}, dy={0,0,1,-1};
        while(!q.isEmpty()){
            Point p=q.removeFirst(); pts.add(p);
            for(int i=0;i<4;i++){
                int nx=p.x+dx[i], ny=p.y+dy[i];
                if(!inBounds(nx,ny) || checked[nx][ny]) continue;
                if(roomIds[nx][ny] >= 0 || !isCorridorGlyph(tiles[nx][ny])) continue;
                checked[nx][ny]=true; q.add(new Point(nx,ny));
            }
        }
        return pts;
    }

    CorridorComponent summarizeCorridorComponent(java.util.List<Point> pts){
        CorridorComponent cc = new CorridorComponent();
        for(Point p: pts) cc.add(p);
        return cc;
    }

    Point farthestPointInComponent(Point from, java.util.List<Point> pts){
        Point best = null; int bd = -1;
        for(Point p: pts){ int d = Math.abs(p.x-from.x)+Math.abs(p.y-from.y); if(d > bd){ bd=d; best=p; } }
        return best;
    }

    void validateWorldgenSelfReport(String phase){
        // 0.8.28 WORLDGEN VALIDATION PASS:
        // This is a post-build sanity report for the new plaza/corridor/door rules. It is
        // intentionally log-first: generation should remain playable, but every violation
        // gives us a concrete breadcrumb instead of another silent corridor nightmare.
        boolean[][] seen = reachableFromStart();
        int unreachableRooms = 0;
        int doorAdjacencyProblems = 0;
        int smallRoomWallDoorViolations = 0;
        int largeRoomSpacingViolations = 0;
        int shortCorridorComponents = 0;
        int squatCorridorComponents = 0;
        int edgeDoorProblems = 0;

        for(int i=0;i<rooms.size();i++){
            Rectangle rr = rooms.get(i);
            boolean reached=false;
            for(int x=rr.x;x<rr.x+rr.width && !reached;x++){
                for(int y=rr.y;y<rr.y+rr.height;y++){
                    if(inBounds(x,y) && roomIds[x][y]==i && seen[x][y]) { reached=true; break; }
                }
            }
            if(!reached){
                unreachableRooms++;
                DebugLog.warn("WORLDGEN_VALIDATE", "phase="+phase+" unreachableRoom id="+i+" rect="+rectText(rr)+" zone="+zoneType.label+" layer="+layerText());
            }
            if(isSmallRoomOneDoorPerWall(rr)) smallRoomWallDoorViolations += countSmallRoomDoorWallViolations(rr, i, phase);
            if(isLargeRoomForDoorSpacing(rr)) largeRoomSpacingViolations += countLargeRoomDoorSpacingViolations(rr, i, phase, 3);
        }

        for(int x=0;x<w;x++) for(int y=0;y<h;y++) if(isDoorSymbol(tiles[x][y])){
            if(countAdjacentNonWall(x,y) < 2){
                doorAdjacencyProblems++;
                DebugLog.warn("WORLDGEN_VALIDATE", "phase="+phase+" weakDoorAdjacency door="+tiles[x][y]+" at="+x+","+y+" adjacentNonWall="+countAdjacentNonWall(x,y)+" zone="+zoneType.label+" layer="+layerText());
            }
        }

        boolean[][] checked = new boolean[w][h];
        for(int x=0;x<w;x++) for(int y=0;y<h;y++){
            if(checked[x][y] || roomIds[x][y] >= 0 || !isCorridorGlyph(tiles[x][y])) continue;
            CorridorComponent cc = collectCorridorComponent(x, y, checked);
            if(cc.size < 2){
                shortCorridorComponents++;
                DebugLog.warn("WORLDGEN_VALIDATE", "phase="+phase+" shortCorridor size="+cc.size+" bounds="+cc.boundsText()+" zone="+zoneType.label+" layer="+layerText());
            }
            if(cc.size > 1 && cc.longAxis() <= cc.shortAxis()){
                squatCorridorComponents++;
                DebugLog.warn("WORLDGEN_VALIDATE", "phase="+phase+" squatCorridor size="+cc.size+" bounds="+cc.boundsText()+" zone="+zoneType.label+" layer="+layerText());
            }
        }

        edgeDoorProblems += validateMaintenanceBulkheadTransitions(phase);

        int totalProblems = unreachableRooms + doorAdjacencyProblems + smallRoomWallDoorViolations + largeRoomSpacingViolations + shortCorridorComponents + squatCorridorComponents + edgeDoorProblems;
        String summary = "phase="+phase+" problems="+totalProblems+" rooms="+rooms.size()+" unreachableRooms="+unreachableRooms+" weakDoors="+doorAdjacencyProblems+" smallRoomDoorWallViolations="+smallRoomWallDoorViolations+" largeDoorSpacingViolations="+largeRoomSpacingViolations+" shortCorridors="+shortCorridorComponents+" squatCorridors="+squatCorridorComponents+" edgeDoorProblems="+edgeDoorProblems+" zone="+zoneType.label+" layer="+layerText()+" seed="+seed;
        if(totalProblems > 0) DebugLog.warn("WORLDGEN_VALIDATE", summary);
        else DebugLog.audit("WORLDGEN_VALIDATE", summary);
    }

    String rectText(Rectangle rr){ return rr.x+","+rr.y+","+rr.width+","+rr.height; }

    int countSmallRoomDoorWallViolations(Rectangle rr, int roomIndex, String phase){
        int violations = 0;
        for(int side=0; side<4; side++){
            int count = countDoorsOnRoomWall(rr, side);
            if(count > 1){
                violations += count - 1;
                DebugLog.warn("WORLDGEN_VALIDATE", "phase="+phase+" smallRoomMultipleDoors room="+roomIndex+" side="+side+" doors="+count+" rect="+rectText(rr)+" zone="+zoneType.label);
            }
        }
        return violations;
    }

    int countLargeRoomDoorSpacingViolations(Rectangle rr, int roomIndex, String phase, int minSpacing){
        int violations = 0;
        for(int side=0; side<4; side++){
            ArrayList<Point> pts = doorsOnRoomWall(rr, side);
            for(int i=0;i<pts.size();i++) for(int j=i+1;j<pts.size();j++){
                Point a=pts.get(i), b=pts.get(j);
                int dist = (side==0 || side==2) ? Math.abs(a.x-b.x) : Math.abs(a.y-b.y);
                if(dist < minSpacing){
                    violations++;
                    DebugLog.warn("WORLDGEN_VALIDATE", "phase="+phase+" largeRoomDoorTooClose room="+roomIndex+" side="+side+" a="+a.x+","+a.y+" b="+b.x+","+b.y+" dist="+dist+" rect="+rectText(rr)+" zone="+zoneType.label);
                }
            }
        }
        return violations;
    }

    int countDoorsOnRoomWall(Rectangle rr, int side){ return doorsOnRoomWall(rr, side).size(); }

    ArrayList<Point> doorsOnRoomWall(Rectangle rr, int side){
        ArrayList<Point> pts = new ArrayList<>();
        for(int x=rr.x; x<rr.x+rr.width; x++) for(int y=rr.y; y<rr.y+rr.height; y++){
            if(!isRoomWallTile(rr, x, y)) continue;
            Point p = new Point(x,y);
            if(roomWallSide(rr, p)==side && isDoorSymbol(tiles[x][y])) pts.add(p);
        }
        return pts;
    }

    int countAdjacentNonWall(int x, int y){
        int count=0; int[] dx={1,-1,0,0}, dy={0,0,1,-1};
        for(int i=0;i<4;i++){
            int nx=x+dx[i], ny=y+dy[i];
            if(inBounds(nx,ny) && tiles[nx][ny] != '#') count++;
        }
        return count;
    }

    static class CorridorComponent {
        int size=0, minX=99999, minY=99999, maxX=-1, maxY=-1;
        void add(Point p){ size++; minX=Math.min(minX,p.x); minY=Math.min(minY,p.y); maxX=Math.max(maxX,p.x); maxY=Math.max(maxY,p.y); }
        int longAxis(){ return Math.max(maxX-minX+1, maxY-minY+1); }
        int shortAxis(){ return Math.min(maxX-minX+1, maxY-minY+1); }
        String boundsText(){ return minX+","+minY+".."+maxX+","+maxY+" axes="+longAxis()+"x"+shortAxis(); }
    }

    CorridorComponent collectCorridorComponent(int sx, int sy, boolean[][] checked){
        CorridorComponent cc = new CorridorComponent();
        ArrayDeque<Point> q = new ArrayDeque<>();
        checked[sx][sy]=true; q.add(new Point(sx,sy));
        int[] dx={1,-1,0,0}, dy={0,0,1,-1};
        while(!q.isEmpty()){
            Point p=q.removeFirst(); cc.add(p);
            for(int i=0;i<4;i++){
                int nx=p.x+dx[i], ny=p.y+dy[i];
                if(!inBounds(nx,ny) || checked[nx][ny]) continue;
                if(roomIds[nx][ny] >= 0 || !isCorridorGlyph(tiles[nx][ny])) continue;
                checked[nx][ny]=true; q.add(new Point(nx,ny));
            }
        }
        return cc;
    }



    boolean maintenanceEnvelopePresent(){
        for(int x=0;x<w;x++) for(int y=0;y<h;y++){
            if(tiles[x][y] == BoundedOuterHiveWallApi.HIVEWALL_CORRIDOR) return true;
        }
        return false;
    }

    int validateMaintenanceBulkheadTransitions(String phase){
        boolean[][] seen = reachableFromStart();
        int doors = 0;
        int edgeDoors = 0;
        int reachable = 0;
        for(int x=0;x<w;x++) for(int y=0;y<h;y++){
            if(tiles[x][y] != 'D') continue;
            doors++;
            if(x == 0 || y == 0 || x == w - 1 || y == h - 1) edgeDoors++;
            else if(seen[x][y]) reachable++;
        }
        int problems = 0;
        if(doors < 4 && !maintenanceEnvelopePresent()){
            return 0;
        }
        if(doors < 4){
            problems++;
            DebugLog.warn("WORLDGEN_VALIDATE", "phase="+phase+" missingMaintenanceBulkheadDoor count="+doors+" zone="+zoneType.label+" layer="+layerText());
        }
        if(edgeDoors > 0){
            problems += edgeDoors;
            DebugLog.warn("WORLDGEN_VALIDATE", "phase="+phase+" forbiddenLiteralEdgeDoor count="+edgeDoors+" zone="+zoneType.label+" layer="+layerText());
        }
        if(doors > edgeDoors && reachable < Math.min(4, doors - edgeDoors)){
            problems++;
            DebugLog.warn("WORLDGEN_VALIDATE", "phase="+phase+" maintenanceBulkheadDoorReachability doors="+doors+" reachable="+reachable+" zone="+zoneType.label+" layer="+layerText());
        }
        return problems;
    }

    int validateEdgeDoorConnectorsFor(char edge, String phase){
        int edgeDoors = 0;
        int connected = 0;
        for(int x=0;x<w;x++) for(int y=0;y<h;y++){
            if(tiles[x][y] != 'D') continue;
            boolean onEdge = (edge=='N' && y==0) || (edge=='S' && y==h-1) || (edge=='W' && x==0) || (edge=='E' && x==w-1);
            if(!onEdge) continue;
            edgeDoors++;
            if(edgeDoorHasInwardConnector(edge, x, y, 16)) connected++;
        }
        int problems = 0;
        if(edgeDoors < 1){
            problems++;
            DebugLog.warn("WORLDGEN_VALIDATE", "phase="+phase+" missingEdgeDoor edge="+edge+" zone="+zoneType.label+" layer="+layerText());
        } else if(connected < 1){
            problems++;
            DebugLog.warn("WORLDGEN_VALIDATE", "phase="+phase+" edgeDoorNoConnector edge="+edge+" doors="+edgeDoors+" connected="+connected+" zone="+zoneType.label+" layer="+layerText());
        }
        return problems;
    }

    boolean edgeDoorHasInwardConnector(char edge, int x, int y, int maxLen){
        int dx=0, dy=0;
        if(edge=='N') dy=1; else if(edge=='S') dy=-1; else if(edge=='W') dx=1; else if(edge=='E') dx=-1;
        for(int i=1;i<=maxLen;i++){
            int nx=x+dx*i, ny=y+dy*i;
            if(!inBounds(nx,ny)) return false;
            if(roomIds[nx][ny] < 0 && isCorridorGlyph(tiles[nx][ny])) return true;
            if(walkable(nx,ny)) return true;
        }
        return false;
    }

    boolean[][] reachableFromStart(){
        // WORLDGEN TOPOLOGY REACHABILITY:
        // This method is used by generation validation and the Zone Audit tool, not
        // by player movement. Treat doors, hatches, elevators, and transition seals as
        // topological connectors even when their gameplay symbol is currently locked,
        // tool-gated, or otherwise not walkable for the player. Otherwise the audit
        // falsely reports deliberately locked rooms as detached geometry and the repair
        // pass carves needless corridors through otherwise valid floorplans.
        boolean[][] seen=new boolean[w][h]; if(rooms.isEmpty()) return seen; Point st=startPoint(); ArrayDeque<Point> q=new ArrayDeque<>(); if(layoutReachableTile(st.x,st.y)){seen[st.x][st.y]=true;q.add(st);}
        int[] dx={1,-1,0,0}, dy={0,0,1,-1};
        while(!q.isEmpty()){Point p=q.removeFirst(); for(int i=0;i<4;i++){int nx=p.x+dx[i],ny=p.y+dy[i]; if(inBounds(nx,ny)&&!seen[nx][ny]&&layoutReachableTile(nx,ny)){seen[nx][ny]=true;q.add(new Point(nx,ny));}}}
        return seen;
    }

    boolean layoutReachableTile(int x, int y){
        if(!inBounds(x,y)) return false;
        if(walkable(x,y)) return true;
        char t = tiles[x][y];
        return isDoorSymbol(t) || t=='D' || t=='S' || t=='v' || t=='E';
    }

    Point nearestReachable(boolean[][] seen,int x,int y){
        Point best=null; int bd=99999; for(int ix=0;ix<w;ix++) for(int iy=0;iy<h;iy++) if(seen[ix][iy]){int d=Math.abs(ix-x)+Math.abs(iy-y); if(d<bd){bd=d; best=new Point(ix,iy);}} return best;
    }


    void validateRoomConnectivityOrCull(){
        boolean[][] seen = reachableFromStart();
        int isolated = 0;
        for(int i=0;i<rooms.size();i++){
            Rectangle rr = rooms.get(i);
            boolean ok=false;
            for(int x=rr.x;x<rr.x+rr.width && !ok;x++) for(int y=rr.y;y<rr.y+rr.height;y++) if(inBounds(x,y) && roomIds[x][y]==i && seen[x][y]) { ok=true; break; }
            if(!ok){
                isolated++;
                DebugLog.audit("ROOM_CONNECTIVITY", "isolated room culled id="+i+" rect="+rr.x+","+rr.y+","+rr.width+","+rr.height+" zone="+zoneType.label);
                for(int x=rr.x;x<rr.x+rr.width;x++) for(int y=rr.y;y<rr.y+rr.height;y++) if(inBounds(x,y) && roomIds[x][y]==i){ roomIds[x][y] = -1; tiles[x][y] = '#'; }
                roomSpecials.set(i, Boolean.TRUE);
                roomFactions.set(i, Faction.NONE);
                roomProfiles.set(i, RoomProfile.closetStub(zoneType, r).withFeatures("Culled unreachable room space; generation authority rejected this room as unattached."));
            }
        }
        if(isolated>0) DebugLog.audit("ROOM_CONNECTIVITY", "culledIsolatedRooms="+isolated+" remainingPhysicalRooms="+(rooms.size()-isolated));
    }

    void placeMartianEmergencyMachineRooms(){
        if (rooms.isEmpty()) return;
        char[] machines = {'Y','J','B','K','O','Z','P','F'};
        // Each generated zone has a chance to include one or more Martian emergency machine room features.
        // Mechanist Collegia and industrial-adjacent layers bias higher; sewer/cult/mutant layers bias lower but may contain derelict units.
        double bias = (zoneType == ZoneType.MECHANICUS_FORGE_CLOISTER || zoneType == ZoneType.MECHANICUS_RELIC_DUCT) ? 0.55 : (sewerLayer ? 0.18 : 0.28);
        int placed = 0;
        for (int i=0; i<rooms.size() && placed<3; i++) {
            if (i == 0) continue;
            if (!roomSpecials.get(i) && r.nextDouble() > bias) continue;
            Rectangle rr = rooms.get(i);
            if (rr.width < 3 || rr.height < 3) continue;
            Point c = center(rr);
            if (!inBounds(c.x,c.y) || roomIds[c.x][c.y] != i) continue;
            char current = tiles[c.x][c.y];
            if (current=='T'||current=='I'||current=='H'||current=='1'||current=='2'||current=='3'||current=='4'||current=='5') continue;
            char m = machines[Math.floorMod((int)(seed + i*31 + placed*7 + zoneType.ordinal()), machines.length)];
            tiles[c.x][c.y] = m;
            roomSpecials.set(i, Boolean.TRUE);
            RoomProfile base = roomProfiles.get(i);
            roomProfiles.set(i, base.asMachineRoom(machineRoomName(m), machineRoomDescription(m), m));
            mapObjects.add(MapObjectState.emergencyMachine(c.x,c.y,m,zoneType));
            placed++;
        }
        if (placed > 0) DebugLog.audit("EMM_ROOM_GEN", "zone="+zoneType.label+" placed="+placed+" layer="+layerText());
    }
    String machineRoomName(char m){
        switch(m){ case 'Y': return "Atmospheric Condenser Cell"; case 'J': return "Emergency Assembler Niche"; case 'B': return "Emergency Boiler Annex"; case 'K': return "Micro Laboratorium Closet"; case 'O': return "Emergency Miner Bay"; case 'Z': return "Power Grid Relay Room"; case 'P': return "Emergency Smelter Cell"; case 'F': return "Steam Engine Chamber"; default: return "Martian Machine Room"; }
    }
    String machineRoomDescription(char m){
        switch(m){
            case 'Y': return "A Martian micro condenser sweats drinkable water from poisoned underhive air.";
            case 'J': return "A cramped emergency assembler waits for designated parts and unsafe instructions.";
            case 'B': return "A boiler rig can distill clean water or be stripped for fuel and dirty reserve.";
            case 'K': return "A tiny laboratorium frame hums for sample work, diagnosis, and reserved research.";
            case 'O': return "A loud emergency miner can chew useful ore from local substrate at the cost of attention.";
            case 'Z': return "A relay grid routes power without consuming a power slot.";
            case 'P': return "A micro smelter accepts raw ores and returns usable metal.";
            case 'F': return "A steam engine can power two machines and can also be sabotaged to black out the slice.";
            default: return "A Martian emergency machine sits in ritualized disrepair.";
        }
    }

    void markSpecialRooms(){
        if(rooms.isEmpty()) return;
        int desired = Math.max(4, Math.min(9, 4 + r.nextInt(6)));
        int marked=0, guard=0;
        while(marked < desired && marked < rooms.size() && guard++ < rooms.size()*6){
            int i = r.nextInt(rooms.size());
            if(i==0 || roomSpecials.get(i)) continue;
            roomSpecials.set(i, Boolean.TRUE);
            RoomProfile base = roomProfiles.get(i);
            roomProfiles.set(i, base.asSpecial(zoneType, r));
            marked++;
        }
        DebugLog.audit("ROOM_SPECIALS", "zone="+zoneType.label+" rooms="+rooms.size()+" specialRooms="+marked);
    }

    void assignRoomFactions(){
        for(int i=0;i<roomFactions.size();i++){
            RoomProfile rp = i < roomProfiles.size() ? roomProfiles.get(i) : null;
            if (EcclesiarchyTempleApi.isTempleRoom(rp)) { roomFactions.set(i, Faction.MINISTORUM); continue; }
            Faction existing = roomFactions.get(i);
            if(existing != null && existing != Faction.NONE) continue;
            roomFactions.set(i, factionForGeneratedRoom(i));
        }
    }

    int seedNeutralContestRooms(){
        // 0.9.07a LEADERSHIP / CONTESTABLE SPACE PASS:
        // Every generated zone attempts to add 2-5 extra empty neutral rooms after
        // ordinary faction assignment. These rooms are deliberately unoccupied, low-loot,
        // and connected to the existing corridor network so later faction schemes can
        // take, lose, fortify, or repurpose them without stealing established special rooms.
        int desired = 2 + r.nextInt(4);
        int added = 0;
        int attempts = 0;
        int branch = 9000 + Math.floorMod((int)seed, 997);
        while(added < desired && attempts++ < 650){
            Rectangle rr = proposeRoomNearPlazaBranch(branch++);
            if(rr == null) continue;
            Rectangle grown = new Rectangle(rr.x-1, rr.y-1, rr.width+2, rr.height+2);
            if(rr.x < 1 || rr.y < 1 || rr.x+rr.width >= w-1 || rr.y+rr.height >= h-1) continue;
            if(regionTouchesNonWall(grown)) continue;
            int before = rooms.size();
            carve(rr);
            rooms.add(rr);
            if(before < roomProfiles.size()) roomProfiles.set(before, RoomProfile.neutralContestRoom(zoneType, r));
            if(before < roomFactions.size()) roomFactions.set(before, Faction.NONE);
            if(before < roomSpecials.size()) roomSpecials.set(before, Boolean.FALSE);
            ConnectionChoice cc = previewBestConnectionToConnectedSet(before);
            if(cc == null){
                removeRoomAtIndex(before);
                continue;
            }
            commitStrictConnection(cc);
            added++;
        }
        DebugLog.audit("NEUTRAL_CONTEST_ROOMS", "zone="+zoneType.label+" layer="+layerText()+" desired="+desired+" added="+added+" totalRooms="+rooms.size()+" seed="+seed);
        return added;
    }

    Faction factionForGeneratedRoom(int i){
        if (i == 0) return Faction.NONE; // start room remains neutral unless claimed by player
        if (i < roomProfiles.size() && EcclesiarchyTempleApi.isTempleRoom(roomProfiles.get(i))) return Faction.MINISTORUM;
        if (floor == 1 || sewerLayer) return r.nextBoolean() ? Faction.MUTANT : Faction.CULTIST;
        if (floor >= 2 && floor <= 4) {
            Faction[] gangs = {Faction.GANGER_IRON_RATS,Faction.GANGER_BLACK_SUMP,Faction.GANGER_CANDLE_JACKS,Faction.GANGER_RED_GRIN,Faction.GANGER_CHAIN_SAINTS,Faction.GANGER_ASH_MARKET,Faction.GANGER_WIRE_WOLVES,Faction.GANGER_DROWNED_9TH,Faction.HIVER_BLOCK_AUREL,Faction.HIVER_BLOCK_MARROW,Faction.HIVER_BLOCK_SUMPLEDGER};
            return r.nextDouble()<0.45 ? gangs[r.nextInt(gangs.length)] : Faction.NONE;
        }
        if (floor == 5) return r.nextDouble()<0.25 ? Faction.CIVIC_LEDGER_OFFICE : Faction.NONE;
        if (zoneType == ZoneType.IMPERIAL_NEWS_NETWORK) return Faction.INN;
        Faction[] upper = {Faction.NOBLE_HOUSE_VARN,Faction.NOBLE_HOUSE_KASTOR,Faction.NOBLE_HOUSE_MORVAIN,Faction.NOBLE_HOUSE_CYRA,Faction.NOBLE_HOUSE_DRAKE,Faction.NOBLE_HOUSE_TOLL,Faction.NOBLE_HOUSE_OSSUARY,Faction.CIVIC_WARDENS,Faction.IMPERIAL_GUARD,Faction.MECHANICUS_CLOISTER_RED,Faction.MECHANICUS_CLOISTER_RUST,Faction.MECHANICUS_CLOISTER_VOID};
        return r.nextDouble()<0.55 ? upper[r.nextInt(upper.length)] : Faction.NONE;
    }

    void populate(){
        npcs.clear();
        mapObjects.clear();
        for(int i=1;i<rooms.size();i++){
            NpcEntity profileNoble = NameLockedProfilePortraitAuthority.maybeCreateNobleSeed(this, i, r);
            if (profileNoble != null) {
                PersonnelPopulationApi.attachExistingNpcToRoomLedger(profileNoble, this, i, r);
                npcs.add(profileNoble);
                DebugLog.audit("NAME_LOCKED_PROFILE_NOBLE", "seeded=" + profileNoble.name + " key=" + profileNoble.nameLockedProfileKey + " zone=" + zoneType.label + " room=" + i);
            }
            double density = (roomSpecials.size() > i && roomSpecials.get(i) ? 0.92 : 0.66) * WorldGenerationApi.settings().npcDensityMultiplier();
            density = Math.max(0.05, Math.min(0.98, density));
            if(r.nextDouble()<density){
                Rectangle rr=rooms.get(i);
                Faction f = npcFactionForRoom(i);
                int count = 1 + (r.nextDouble()<0.20 ? 1 : 0);
                for(int n=0;n<count;n++){
                    Point p = randomOpenPointInRoom(rr);
                    if(p != null) { NpcEntity created = PersonnelPopulationApi.createResidentFromRoom(this, i, f, p.x, p.y, r); npcs.add(created); }
                }
            }
        }
        addRoomProps();
        if (zoneType == ZoneType.NEUTRAL_RAIL_DEPOT && rooms.size()>2) { Point c=center(rooms.get(1)); tiles[c.x][c.y]='T'; mapObjects.add(MapObjectState.shop(c.x,c.y,zoneType)); }
        if (zoneType == ZoneType.SECTOR_GOVERNORS_MANSION && rooms.size()>0) { Point c=center(rooms.get(0)); tiles[c.x][c.y]='Q'; mapObjects.add(MapObjectState.governor(c.x,c.y,zoneType, seed)); }
        if ((zoneType == ZoneType.NEUTRAL_RAIL_DEPOT || zoneType == ZoneType.SUMP_MARKET || zoneType == ZoneType.NEUTRAL_CIVILIAN_FLOOR) && rooms.size()>1) {
            Point tp = randomOpenPointInRoom(rooms.get(1));
            if (tp != null && npcAt(tp.x,tp.y)==null) { NpcEntity clerk = NpcEntity.tradeClerkFor(zoneType, tp.x, tp.y, r); PersonnelPopulationApi.attachExistingNpcToRoomLedger(clerk, this, Math.max(0, roomIds[tp.x][tp.y]), r); npcs.add(clerk); mapObjects.add(MapObjectState.shop(tp.x,tp.y,zoneType)); }
        }
        ImperialNewsNetworkApi.seedNewsObjects(this, r);
        BankingApi.seedBankObjects(this, r);
        enforcePassiveNpcCount();
        ensureResidentSupportRoomsAndAmenities();
        seedEcclesiarchyTempleStaff();
        seedFactionContinuityBarStaff();
        seedFactionLeadershipResidency();
        AnimalPopulationApi.Result animalResult = AnimalPopulationApi.seedAnimalsAndServants(this, r);
        int hivewallThreats = BoundedOuterHiveWallApi.populateDangerRooms(this, r);
        LightingNoiseMetadataApi.Result ln = LightingNoiseMetadataApi.seed(this, r);
        lightNoiseSummary = ln.summary();
        DebugLog.audit("NPC_POPULATE", "zone="+zoneType.label+" layer="+layerText()+" npcs="+npcs.size()+" rooms="+rooms.size()+" target="+targetPassiveEntityCount()+" animals="+animalResult.summary()+" hivewallThreats="+hivewallThreats+" lighting="+ln.summary());
    }


    void seedEcclesiarchyTempleStaff(){
        int templeRooms = 0, clergy = 0, pilgrims = 0, sisters = 0;
        for(int i=1; i<rooms.size(); i++){
            if(i >= roomProfiles.size() || !EcclesiarchyTempleApi.isTempleRoom(roomProfiles.get(i))) continue;
            templeRooms++;
            Rectangle rr = rooms.get(i);
            Point head = randomOpenPointInRoom(rr);
            if(head != null && npcAt(head.x, head.y)==null){
                NpcEntity n = NpcEntity.headClericFor(zoneType, head.x, head.y, r);
                PersonnelPopulationApi.attachExistingNpcToRoomLedger(n, this, i, r);
                npcs.add(n); clergy++;
            }
            for(int p=0; p<2; p++){
                Point pt = randomOpenPointInRoom(rr);
                if(pt != null && npcAt(pt.x, pt.y)==null){
                    NpcEntity n = NpcEntity.ministorumPriestOrPilgrim(zoneType, pt.x, pt.y, r);
                    PersonnelPopulationApi.attachExistingNpcToRoomLedger(n, this, i, r);
                    npcs.add(n); pilgrims++;
                }
            }
            for(int s=0; s<2; s++){
                Point pt = randomOpenPointInRoom(rr);
                if(pt != null && npcAt(pt.x, pt.y)==null){
                    NpcEntity n = NpcEntity.sororitasGuardFor(zoneType, pt.x, pt.y, r);
                    PersonnelPopulationApi.attachExistingNpcToRoomLedger(n, this, i, r);
                    npcs.add(n); sisters++;
                }
            }
        }
        if(templeRooms > 0) DebugLog.audit("ECCLESIARCHY_TEMPLE_STAFF", "zone="+zoneType.label+" templeRooms="+templeRooms+" headClerics="+clergy+" priestsPilgrims="+pilgrims+" sisters="+sisters+" layer="+layerText());
    }

    int targetPassiveEntityCount(){
        int base = 5 + Math.floorMod((int)(seed ^ (floor*37L) ^ (zoneX*11L) ^ (zoneY*17L)), 26);
        if(zoneType==ZoneType.MUTANT_SEWER_CAMP || zoneType==ZoneType.CULTIST_SEWER_CAMP || zoneType==ZoneType.MUTANT_WARRENS) base += Math.max(0, 6 - floor) * 2;
        if(zoneType==ZoneType.NEUTRAL_CIVILIAN_FLOOR) base += 24;
        if(zoneType==ZoneType.HAB_STACK) base += 18;
        if(zoneType==ZoneType.SUMP_MARKET || zoneType==ZoneType.NEUTRAL_RAIL_DEPOT) base += 16;
        if(zoneType==ZoneType.ADMINISTRATUM_ARCHIVE || zoneType==ZoneType.IMPERIAL_NEWS_NETWORK) base += 10;
        if(zoneType==ZoneType.NOBLE_SERVICE_SPINE || zoneType==ZoneType.IMPERIAL_GUARD_BILLET) base += Math.max(0, floor - 5);
        base = (int)Math.round(base * WorldGenerationApi.settings().npcDensityMultiplier());
        base *= 2;
        return Math.max(8, Math.min(192, base));
    }
    void enforcePassiveNpcCount(){
        int target = targetPassiveEntityCount();
        while(npcs.size() < target && rooms.size() > 1){
            Rectangle rr = rooms.get(1 + r.nextInt(rooms.size()-1));
            Point p = randomOpenPointInRoom(rr);
            if(p == null) break;
            int rid = Math.max(0, roomIds[p.x][p.y]); NpcEntity created = PersonnelPopulationApi.createResidentFromRoom(this, rid, npcFactionForRoom(rid), p.x, p.y, r); npcs.add(created);
        }
        while(npcs.size() > target && npcs.size() > 0) npcs.remove(npcs.size()-1);
    }

    void ensureResidentSupportRoomsAndAmenities(){
        int roomsTouched = 0, cots = 0, food = 0;
        for(int i=1; i<rooms.size(); i++){
            if(countNpcsInRoom(i) <= 0 && roomFaction(i) == Faction.NONE) continue;
            Rectangle rr = rooms.get(i);
            if(!roomHasAny(rr, new char[]{'c'})) { Point p = randomOpenPointInRoom(rr); if(p != null){ tiles[p.x][p.y]='c'; cots++; } }
            if(!roomHasAny(rr, new char[]{'u','N','T','q','b'})) { Point p = randomOpenPointInRoom(rr); if(p != null){ tiles[p.x][p.y]='u'; food++; } }
            roomsTouched++;
        }
        DebugLog.audit("RESIDENT_SUPPORT_AMENITIES", "zone="+zoneType.label+" roomsTouched="+roomsTouched+" cots="+cots+" foodSources="+food+" rule=resident-rooms-carry-sleep-and-food-support");
    }

    int countNpcsInRoom(int roomId){
        if(npcs == null) return 0;
        int n = 0;
        for(NpcEntity e: npcs){
            if(e == null || !inBounds(e.x,e.y)) continue;
            if(roomIds[e.x][e.y] == roomId) n++;
        }
        return n;
    }

    boolean roomHasAny(Rectangle rr, char[] chars){
        if(rr == null || chars == null) return false;
        for(int x=rr.x+1; x<rr.x+rr.width-1; x++) for(int y=rr.y+1; y<rr.y+rr.height-1; y++){
            if(!inBounds(x,y)) continue;
            char t = tiles[x][y];
            for(char c: chars) if(t == c) return true;
        }
        return false;
    }

    Faction npcFactionForRoom(int roomId){
        if(roomId >= 0 && roomId < roomProfiles.size() && EcclesiarchyTempleApi.isTempleRoom(roomProfiles.get(roomId))) return Faction.MINISTORUM;
        if(zoneType==ZoneType.MUTANT_SEWER_CAMP || zoneType==ZoneType.MUTANT_WARRENS) return Faction.MUTANT;
        if(zoneType==ZoneType.CULTIST_SEWER_CAMP) return Faction.CULTIST;
        if(zoneType==ZoneType.ARBITES_PRECINCT_EDGE) return Faction.CIVIC_WARDENS;
        if(zoneType==ZoneType.IMPERIAL_GUARD_BILLET) return Faction.IMPERIAL_GUARD;
        if(zoneType==ZoneType.MECHANICUS_FORGE_CLOISTER || zoneType==ZoneType.MECHANICUS_RELIC_DUCT) return r.nextBoolean()?Faction.MECHANICUS_CLOISTER_RED:Faction.MECHANIST_COLLEGIA;
        if(zoneType==ZoneType.GANGER_TURF) { Faction[] gangs={Faction.GANGER_IRON_RATS,Faction.GANGER_BLACK_SUMP,Faction.GANGER_CANDLE_JACKS,Faction.GANGER_RED_GRIN,Faction.GANGER_CHAIN_SAINTS,Faction.GANGER_ASH_MARKET,Faction.GANGER_WIRE_WOLVES,Faction.GANGER_DROWNED_9TH}; return gangs[r.nextInt(gangs.length)]; }
        if(zoneType==ZoneType.SECTOR_GOVERNORS_MANSION) return Faction.NOBLE;
        if(zoneType==ZoneType.NOBLE_SERVICE_SPINE) { Faction[] nobles={Faction.NOBLE_HOUSE_VARN,Faction.NOBLE_HOUSE_KASTOR,Faction.NOBLE_HOUSE_MORVAIN,Faction.NOBLE_HOUSE_CYRA,Faction.NOBLE_HOUSE_DRAKE,Faction.NOBLE_HOUSE_TOLL,Faction.NOBLE_HOUSE_OSSUARY}; return nobles[r.nextInt(nobles.length)]; }
        if(zoneType==ZoneType.IMPERIAL_NEWS_NETWORK) return Faction.INN;
        if(zoneType==ZoneType.ADMINISTRATUM_ARCHIVE || zoneType==ZoneType.NEUTRAL_CIVILIAN_FLOOR || zoneType==ZoneType.SUMP_MARKET || zoneType==ZoneType.NEUTRAL_RAIL_DEPOT) return r.nextDouble()<0.18?Faction.CIVIC_WARDENS:Faction.CIVIC_LEDGER_OFFICE;
        if(zoneType==ZoneType.HAB_STACK) return Faction.HIVER;
        if(zoneType==ZoneType.SEWER_CONDUIT) return r.nextDouble()<0.12?Faction.MUTANT:Faction.NONE;
        return roomFaction(roomId)==Faction.NONE?Faction.SCAVENGER:roomFaction(roomId);
    }

    Point randomOpenPointInRoom(Rectangle rr){
        for(int tries=0; tries<80; tries++){
            int x=rr.x+1+r.nextInt(Math.max(1, rr.width-2));
            int y=rr.y+1+r.nextInt(Math.max(1, rr.height-2));
            if(inBounds(x,y) && walkable(x,y) && roomIds[x][y]>=0 && npcAt(x,y)==null) return new Point(x,y);
        }
        Point c=center(rr);
        if(inBounds(c.x,c.y) && walkable(c.x,c.y) && npcAt(c.x,c.y)==null) return c;
        for(int x=rr.x;x<rr.x+rr.width;x++) for(int y=rr.y;y<rr.y+rr.height;y++) if(inBounds(x,y) && walkable(x,y) && roomIds[x][y]>=0 && npcAt(x,y)==null) return new Point(x,y);
        return null;
    }

    boolean isTransitionOrDoorGlyph(char ch){
        return ch == 'D' || ch == 'S' || ch == 'v' || ch == 'E' || isDoorSymbol(ch);
    }

    boolean isDoorAccessReservedForObject(int x, int y){
        if(!inBounds(x,y)) return true;
        for(int dx=-2; dx<=2; dx++) for(int dy=-2; dy<=2; dy++){
            if(Math.abs(dx) + Math.abs(dy) > 2) continue;
            int nx = x + dx, ny = y + dy;
            if(!inBounds(nx,ny)) continue;
            char t = tiles[nx][ny];
            if(t == 'D' || t == 'S' || t == 'v' || t == 'E') return true;
            // Several service fixtures use glyphs that overlap old door glyphs (notably bank vault X).
            // Treat those as fixtures, not doorway-clearance anchors, once a semantic map object owns the cell.
            if(isDoorSymbol(t) && mapObjectAt(nx,ny) == null) return true;
        }
        return false;
    }

    boolean objectPlacementLegal(int x, int y){
        return inBounds(x,y) && walkable(x,y) && roomIds[x][y] >= 0 && mapObjectAt(x,y) == null && !isDoorAccessReservedForObject(x,y);
    }

    boolean roomPropPlacementLegal(int x, int y){
        return inBounds(x,y) && tiles[x][y] == '.' && roomIds[x][y] >= 0 && mapObjectAt(x,y) == null && !isDoorAccessReservedForObject(x,y);
    }

    Point randomObjectPointInRoom(Rectangle rr){
        if(rr == null) return null;
        for(int tries=0; tries<120; tries++){
            int x=rr.x+1+r.nextInt(Math.max(1, rr.width-2));
            int y=rr.y+1+r.nextInt(Math.max(1, rr.height-2));
            if(roomPropPlacementLegal(x,y) && npcAt(x,y)==null) return new Point(x,y);
        }
        Point c=center(rr);
        if(roomPropPlacementLegal(c.x,c.y) && npcAt(c.x,c.y)==null) return c;
        for(int x=rr.x; x<rr.x+rr.width; x++) for(int y=rr.y; y<rr.y+rr.height; y++) if(roomPropPlacementLegal(x,y) && npcAt(x,y)==null) return new Point(x,y);
        return null;
    }

    Point relocationTargetForObject(MapObjectState m){
        if(m == null) return null;
        int preferredRoom = inBounds(m.x,m.y) ? roomIds[m.x][m.y] : -1;
        if(preferredRoom >= 0 && preferredRoom < rooms.size()){
            Point p = randomObjectPointInRoom(rooms.get(preferredRoom));
            if(p != null) return p;
        }
        for(int tries=0; tries<160 && rooms.size()>1; tries++){
            Rectangle rr = rooms.get(1 + r.nextInt(rooms.size()-1));
            Point p = randomObjectPointInRoom(rr);
            if(p != null) return p;
        }
        return null;
    }

    char fallbackUnderForObject(int x, int y, char old){
        if(old == 0 || old == InterstitialInfrastructureApi.VOID_SPACE) return roomIds[x][y] >= 0 ? '.' : '#';
        if(old == '$' || old == '1' || old == '2' || old == '3' || old == '4' || old == '5' || old == 'H' || old == 'I' || old == 'T' || old == 'X' || old == 'a' || old == 'N' || old == 'o') return roomIds[x][y] >= 0 ? '.' : '#';
        return old;
    }

    int sanitizeGeneratedObjectDoorAccess(){
        if(mapObjects == null || mapObjects.isEmpty()) return 0;
        int repaired = 0;
        ArrayList<MapObjectState> remove = new ArrayList<>();
        for(MapObjectState m: mapObjects){
            if(m == null) continue;
            boolean illegal = !inBounds(m.x,m.y) || isDoorAccessReservedForObject(m.x,m.y);
            if(!illegal) continue;
            int oldX = m.x, oldY = m.y;
            char oldTile = inBounds(oldX,oldY) ? tiles[oldX][oldY] : 0;
            char underlying = MapObjectState.underlyingTileFromStock(m.stockState);
            if(inBounds(oldX,oldY) && oldTile == m.glyph) tiles[oldX][oldY] = fallbackUnderForObject(oldX, oldY, underlying == 0 ? oldTile : underlying);
            Point p = relocationTargetForObject(m);
            if(p == null){ remove.add(m); repaired++; continue; }
            m.x = p.x; m.y = p.y;
            if(inBounds(p.x,p.y) && (tiles[p.x][p.y] == '.' || walkable(p.x,p.y))) tiles[p.x][p.y] = m.glyph;
            repaired++;
        }
        if(!remove.isEmpty()) mapObjects.removeAll(remove);
        return repaired;
    }


    Point randomOpenPoint(Random rrng){
        Random use = rrng == null ? r : rrng;
        if(!rooms.isEmpty()) {
            for(int tries=0; tries<120; tries++){
                Rectangle rr = rooms.get(use.nextInt(rooms.size()));
                Point p = randomOpenPointInRoom(rr);
                if(p != null && inBounds(p.x,p.y) && walkable(p.x,p.y) && npcAt(p.x,p.y)==null) return p;
            }
        }
        for(int tries=0; tries<300; tries++){
            int x=1+use.nextInt(Math.max(1,w-2));
            int y=1+use.nextInt(Math.max(1,h-2));
            if(inBounds(x,y) && walkable(x,y) && npcAt(x,y)==null) return new Point(x,y);
        }
        return ensurePlayerSpawnPoint();
    }

    boolean walkableAdjacentOrSame(int x,int y){ if(walkable(x,y)) return true; int[][] dirs={{1,0},{-1,0},{0,1},{0,-1}}; for(int[] d:dirs) if(walkable(x+d[0],y+d[1])) return true; return false; }

    MapObjectState mapObjectAt(int x,int y){
        for(MapObjectState m: mapObjects) if(m.x==x && m.y==y) return m;
        return null;
    }

    Point findNeedProvider(int x, int y, String need){
        Point best=null; int bd=999999;
        for(int ix=0; ix<w; ix++) for(int iy=0; iy<h; iy++){
            char t=tiles[ix][iy]; boolean ok=false;
            if("food".equals(need)) ok = (t=='T' || t=='1' || t=='2' || t=='3' || t=='b' || t=='h' || t=='n');
            else if("water".equals(need)) ok = (t=='T' || t=='1' || t=='Y' || t=='B' || t=='S' || t=='v');
            else if("sleep".equals(need)) ok = (t=='b' || t=='h' || t=='n');
            if(ok && walkableAdjacentOrSame(ix,iy)) { int d=Math.abs(ix-x)+Math.abs(iy-y); if(d<bd){bd=d; best=new Point(ix,iy);} }
        }
        return best;
    }

    NpcEntity npcAt(int x,int y){ for(NpcEntity n:npcs) if(n.x==x && n.y==y) return n; return null; }
    NpcEntity npcById(String id){ if(id == null || id.isBlank()) return null; for(NpcEntity n:npcs) if(n != null && id.equals(n.id)) return n; return null; }
    boolean hasFlickeringLights(){ for(ZoneLightSourceRecord l: lightSources) if(l != null && l.flicker && l.on && l.powered) return true; return false; }

    String occKey(int x, int y) { return x + "," + y; }

    boolean npcOccupancyLegal(int x, int y, int avoidX, int avoidY, HashSet<String> occupied) {
        return inBounds(x,y) && walkable(x,y) && !(x == avoidX && y == avoidY) && (occupied == null || !occupied.contains(occKey(x,y)));
    }

    Point nearestUnoccupiedWalkableTo(int sx, int sy, int avoidX, int avoidY) {
        return nearestUnoccupiedWalkableTo(sx, sy, avoidX, avoidY, null);
    }

    Point nearestUnoccupiedWalkableTo(int sx, int sy, int avoidX, int avoidY, HashSet<String> occupied) {
        sx = Math.max(0, Math.min(w-1, sx));
        sy = Math.max(0, Math.min(h-1, sy));
        boolean[][] seen = new boolean[w][h];
        ArrayDeque<Point> q = new ArrayDeque<>();
        q.add(new Point(sx, sy));
        seen[sx][sy] = true;
        int[] dx = {1,-1,0,0};
        int[] dy = {0,0,1,-1};
        while(!q.isEmpty()) {
            Point p = q.removeFirst();
            if (npcOccupancyLegal(p.x, p.y, avoidX, avoidY, occupied)) return p;
            for(int i=0;i<4;i++) {
                int nx = p.x + dx[i], ny = p.y + dy[i];
                if(!inBounds(nx,ny) || seen[nx][ny]) continue;
                seen[nx][ny] = true;
                q.add(new Point(nx,ny));
            }
        }
        return null;
    }

    int repairNpcOccupancy(int playerX, int playerY, Random rrng) {
        if (npcs == null || npcs.isEmpty()) return 0;
        int repaired = 0;
        int terrainRescued = 0;
        int stacksRescued = 0;
        HashSet<String> occupied = new HashSet<>();
        for (NpcEntity n : npcs) {
            if (n == null) continue;
            String key = occKey(n.x, n.y);
            boolean badTerrain = !inBounds(n.x,n.y) || !walkable(n.x,n.y);
            boolean stacked = (n.x == playerX && n.y == playerY) || occupied.contains(key);
            boolean illegal = badTerrain || stacked;
            if (illegal) {
                int oldX = n.x, oldY = n.y;
                Point p = nearestUnoccupiedWalkableTo(n.x, n.y, playerX, playerY, occupied);
                if (p == null && rrng != null) p = randomOpenPoint(rrng);
                if (p != null && !(p.x == playerX && p.y == playerY) && !occupied.contains(occKey(p.x,p.y))) {
                    n.x = p.x;
                    n.y = p.y;
                    n.homeX = p.x;
                    n.homeY = p.y;
                    n.state = badTerrain ? "Emergency Repath" : "Displaced";
                    repaired++;
                    if (badTerrain) terrainRescued++;
                    if (stacked) stacksRescued++;
                    DebugLog.audit("NPC_EMERGENCY_REPATH", "npc=" + n.id + " old=" + oldX + "," + oldY + " new=" + n.x + "," + n.y + " badTerrain=" + badTerrain + " stacked=" + stacked + " zone=" + zoneType.label + " layer=" + layerText());
                    key = occKey(n.x, n.y);
                }
            }
            occupied.add(key);
        }
        if(repaired > 0) DebugLog.audit("NPC_OCCUPANCY_REPAIR", "repaired="+repaired+" terrain="+terrainRescued+" stacked="+stacksRescued+" zone="+zoneType.label+" layer="+layerText());
        return repaired;
    }

    void addRoomProps(){
        char[] props = propPaletteForZone();
        for(int i=0;i<rooms.size();i++){
            Rectangle rr=rooms.get(i);
            int count = r.nextInt(3);
            for(int n=0;n<count;n++){
                int x=rr.x+r.nextInt(Math.max(1,rr.width)); int y=rr.y+r.nextInt(Math.max(1,rr.height));
                if(roomPropPlacementLegal(x,y)) tiles[x][y]=props[r.nextInt(props.length)];
            }
        }
        addPassiveFixtures();
    }

    void addPassiveFixtures(){
        int vending = 0, shrines = 0;
        for(int i=1;i<rooms.size();i++){
            Rectangle rr = rooms.get(i);
            if(rr == null) continue;
            double vChance = vendingChanceForZone();
            double sChance = shrineChanceForZone();
            if(r.nextDouble() < vChance){
                Point p = randomObjectPointInRoom(rr);
                if(p != null && tiles[p.x][p.y]=='.'){ char vg = vendingGlyphForZone(); tiles[p.x][p.y] = vg; mapObjects.add(MapObjectState.vending(p.x,p.y,vg,zoneType)); vending++; }
            }
            if(r.nextDouble() < sChance){
                Point p = randomObjectPointInRoom(rr);
                if(p != null && tiles[p.x][p.y]=='.'){ char sg = shrineGlyphForZone(); tiles[p.x][p.y] = sg; mapObjects.add(MapObjectState.shrine(p.x,p.y,sg,zoneType)); shrines++; }
            }
        }
        // Double-wide corridors host more public vending, but never in door access cells.
        int corridorTries = Math.max(18, rooms.size()*5);
        for(int t=0; t<corridorTries; t++){
            if(r.nextDouble() > 0.16) continue;
            int x=2+r.nextInt(Math.max(1,w-4)), y=2+r.nextInt(Math.max(1,h-4));
            if(tiles[x][y]=='=' || tiles[x][y]==':' || tiles[x][y]=='+'){
                int[][] dirs={{1,0},{-1,0},{0,1},{0,-1}};
                for(int[] d:dirs){ int px=x+d[0], py=y+d[1]; if(inBounds(px,py) && tiles[px][py]=='#' && roomIds[px][py]<0 && mapObjectAt(px,py)==null && !isDoorAccessReservedForObject(px,py)){ char vg=vendingGlyphForZone(); tiles[px][py]=vg; mapObjects.add(MapObjectState.vending(px,py,vg,zoneType)); vending++; break; } }
            }
        }
        DebugLog.audit("PASSIVE_FIXTURES", "zone="+zoneType.label+" vending="+vending+" shrines="+shrines+" layer="+layerText());
    }

    double vendingChanceForZone(){
        if(zoneType==ZoneType.NEUTRAL_RAIL_DEPOT || zoneType==ZoneType.SUMP_MARKET) return 0.78;
        if(zoneType==ZoneType.NEUTRAL_CIVILIAN_FLOOR || zoneType==ZoneType.HAB_STACK) return 0.52;
        if(zoneType==ZoneType.ADMINISTRATUM_ARCHIVE || zoneType==ZoneType.IMPERIAL_NEWS_NETWORK) return 0.42;
        if(zoneType==ZoneType.ARBITES_PRECINCT_EDGE || zoneType==ZoneType.IMPERIAL_GUARD_BILLET || zoneType==ZoneType.MECHANICUS_FORGE_CLOISTER) return 0.28;
        if(zoneType==ZoneType.MUTANT_SEWER_CAMP || zoneType==ZoneType.CULTIST_SEWER_CAMP || zoneType==ZoneType.MUTANT_WARRENS) return 0.08;
        return 0.18;
    }
    double shrineChanceForZone(){
        if(zoneType==ZoneType.CULTIST_SEWER_CAMP) return 0.65;
        if(zoneType==ZoneType.MUTANT_SEWER_CAMP || zoneType==ZoneType.MUTANT_WARRENS) return 0.10;
        if(zoneType==ZoneType.ADMINISTRATUM_ARCHIVE || zoneType==ZoneType.NEUTRAL_CIVILIAN_FLOOR || zoneType==ZoneType.NOBLE_SERVICE_SPINE) return 0.22;
        if(zoneType==ZoneType.MECHANICUS_FORGE_CLOISTER) return 0.12;
        return 0.06;
    }
    char vendingGlyphForZone(){
        if(zoneType==ZoneType.ARBITES_PRECINCT_EDGE || zoneType==ZoneType.IMPERIAL_GUARD_BILLET) return r.nextBoolean() ? '2' : '3';
        if(zoneType==ZoneType.MECHANICUS_FORGE_CLOISTER || zoneType==ZoneType.MECHANICUS_RELIC_DUCT) return r.nextBoolean() ? '4' : '5';
        if(zoneType==ZoneType.SUMP_MARKET || zoneType==ZoneType.NEUTRAL_RAIL_DEPOT) return (char)('1' + r.nextInt(5));
        if(zoneType==ZoneType.GANGER_TURF) return r.nextBoolean() ? '3' : '1';
        return r.nextBoolean() ? '1' : '5';
    }
    char shrineGlyphForZone(){
        if(zoneType==ZoneType.CULTIST_SEWER_CAMP) return 'H';
        if(zoneType==ZoneType.SEWER_CONDUIT && floor <= 2 && r.nextDouble()<0.35) return 'H';
        if(zoneType==ZoneType.MUTANT_SEWER_CAMP || zoneType==ZoneType.MUTANT_WARRENS) return r.nextDouble()<0.45 ? 'H' : 'I';
        return 'I';
    }

    char[] propPaletteForZone(){
        if(zoneType==ZoneType.SEWER_CONDUIT) return new char[]{'o','~','N'};
        if(zoneType==ZoneType.MUTANT_SEWER_CAMP) return new char[]{'m','o','~','b'};
        if(zoneType==ZoneType.CULTIST_SEWER_CAMP) return new char[]{'c','o','~','b'};
        if(zoneType==ZoneType.MECHANICUS_FORGE_CLOISTER || zoneType==ZoneType.MECHANICUS_RELIC_DUCT) return new char[]{'N','q','R'};
        if(zoneType==ZoneType.ADMINISTRATUM_ARCHIVE || zoneType==ZoneType.NEUTRAL_CIVILIAN_FLOOR) return new char[]{'k','t','o'};
        if(zoneType==ZoneType.GANGER_TURF) return new char[]{'k','o','b'};
        if(zoneType==ZoneType.ARBITES_PRECINCT_EDGE || zoneType==ZoneType.IMPERIAL_GUARD_BILLET) return new char[]{'k','N','b'};
        return new char[]{'o','u','t'};
    }

    char entitySymbolForZone(Faction f){
        if(zoneType==ZoneType.MUTANT_SEWER_CAMP) return 'm';
        if(zoneType==ZoneType.CULTIST_SEWER_CAMP) return 'c';
        if(zoneType==ZoneType.NEUTRAL_CIVILIAN_FLOOR || zoneType==ZoneType.SUMP_MARKET || zoneType==ZoneType.TRAIN_SERVICE_YARD || zoneType==ZoneType.NEUTRAL_RAIL_DEPOT) return r.nextDouble()<0.18 ? 'A' : 'h';
        if(zoneType==ZoneType.ARBITES_PRECINCT_EDGE) return 'A';
        if(zoneType==ZoneType.IMPERIAL_GUARD_BILLET) return 'M';
        if(zoneType==ZoneType.MECHANICUS_FORGE_CLOISTER || zoneType==ZoneType.MECHANICUS_RELIC_DUCT) return r.nextDouble()<0.65 ? 'R' : 'q';
        if(zoneType==ZoneType.MUTANT_WARRENS) return 'm';
        if(zoneType==ZoneType.GANGER_TURF) return 'g';
        if(zoneType==ZoneType.NOBLE_SERVICE_SPINE) return 'n';
        if(zoneType==ZoneType.ADMINISTRATUM_ARCHIVE || zoneType==ZoneType.HAB_STACK) return 'h';
        if(zoneType==ZoneType.SEWER_CONDUIT) return floor==1 || r.nextDouble()<0.55 ? 'm' : 'H';
        return f==Faction.CULTIST ? 'H' : (f==Faction.MUTANT ? 'm' : 'h');
    }
    int roomIdAt(int x,int y){ if(x<0||y<0||x>=w||y>=h)return -1; return roomIds[x][y]; }
    Rectangle roomRect(int id){ if(id<0 || id>=rooms.size()) return null; return rooms.get(id); }
    RoomProfile roomProfile(int id){ if(id<0 || id>=roomProfiles.size()) return RoomProfile.generic(); return roomProfiles.get(id); }
    Faction roomFaction(int id){ if(id<0 || id>=roomFactions.size()) return Faction.NONE; return roomFactions.get(id); }
    Point startPoint(){
        if(rooms.isEmpty()) return new Point(1,1);
        Rectangle spawnRoom = rooms.get(0);
        Point c=center(spawnRoom);
        if(inBounds(c.x,c.y) && walkable(c.x,c.y) && roomIds[c.x][c.y] == 0) return c;
        for(int x=spawnRoom.x;x<spawnRoom.x+spawnRoom.width;x++) for(int y=spawnRoom.y;y<spawnRoom.y+spawnRoom.height;y++) if(inBounds(x,y)&&walkable(x,y)&&roomIds[x][y]==0) return new Point(x,y);
        return ensurePlayerSpawnPoint();
    }
    Point ensurePlayerSpawnPoint(){
        for(int i=0;i<rooms.size();i++){
            Rectangle rr = rooms.get(i);
            Point c = center(rr);
            if(inBounds(c.x,c.y) && walkable(c.x,c.y) && roomIds[c.x][c.y] == i) return c;
            for(int x=rr.x;x<rr.x+rr.width;x++) for(int y=rr.y;y<rr.y+rr.height;y++) if(inBounds(x,y)&&walkable(x,y)&&roomIds[x][y]==i) return new Point(x,y);
        }
        // Last-resort repair: carve a tiny insertion room and explicitly assign roomIds so
        // player spawn always has a real room pointer instead of a naked coordinate.
        Rectangle rr = new Rectangle(Math.max(2,w/2-2), Math.max(2,h/2-2), 5, 5);
        int idx = rooms.size();
        carve(rr); rooms.add(rr);
        if(idx < roomProfiles.size()) roomProfiles.set(idx, RoomProfile.centralPlaza(zoneType, sewerLayer, r));
        if(idx < roomFactions.size()) roomFactions.set(idx, Faction.NONE);
        if(idx < roomSpecials.size()) roomSpecials.set(idx, Boolean.TRUE);
        Point c = center(rr);
        DebugLog.error("PLAYER_SPAWN_REPAIR", "Created emergency insertion room id="+idx+" at "+rr.x+","+rr.y+","+rr.width+","+rr.height, null);
        return c;
    }
    Point edgeSpawn(char edge){
        if(rooms.isEmpty()) return startPoint();
        return returnEdgeSpawn(edge, edge=='N'||edge=='S' ? new Point(w/2, 0) : new Point(0, h/2));
    }

    Point returnEdgeSpawn(char edge, Point sourceHint){
        if(rooms.isEmpty()) return startPoint();
        int targetAxis = (edge=='N'||edge=='S') ? Math.max(1, Math.min(w-2, sourceHint.x)) : Math.max(1, Math.min(h-2, sourceHint.y));
        Point door = nearestEdgeDoor(edge, targetAxis);
        if(door != null){
            Point inside = inwardFromEdgeDoor(edge, door);
            return nearestWalkableTo(inside.x, inside.y);
        }
        // Repair fallback for older/generated maps without visible D markers on the requested edge.
        int rx = (edge=='W') ? 1 : (edge=='E' ? w-2 : targetAxis);
        int ry = (edge=='N') ? 1 : (edge=='S' ? h-2 : targetAxis);
        return nearestWalkableTo(rx, ry);
    }

    Point nearestEdgeDoor(char edge, int targetAxis){
        Point best=null; int bd=99999;
        for(int x=0;x<w;x++) for(int y=0;y<h;y++){
            if(tiles[x][y] != 'D') continue;
            boolean onEdge = (edge=='N' && y==0) || (edge=='S' && y==h-1) || (edge=='W' && x==0) || (edge=='E' && x==w-1);
            if(!onEdge) continue;
            int axis = (edge=='N'||edge=='S') ? x : y;
            int d = Math.abs(axis-targetAxis);
            if(d < bd){ bd=d; best=new Point(x,y); }
        }
        return best;
    }

    Point inwardFromEdgeDoor(char edge, Point door){
        if(edge=='N') return new Point(Math.max(1, Math.min(w-2, door.x)), 1);
        if(edge=='S') return new Point(Math.max(1, Math.min(w-2, door.x)), h-2);
        if(edge=='W') return new Point(1, Math.max(1, Math.min(h-2, door.y)));
        if(edge=='E') return new Point(w-2, Math.max(1, Math.min(h-2, door.y)));
        return startPoint();
    }

    Point nearestWalkableTo(int sx, int sy){
        sx=Math.max(1, Math.min(w-2, sx)); sy=Math.max(1, Math.min(h-2, sy));
        if(walkable(sx,sy)) return new Point(sx,sy);
        boolean[][] seen=new boolean[w][h]; ArrayDeque<Point> q=new ArrayDeque<>();
        seen[sx][sy]=true; q.add(new Point(sx,sy)); int[] dx={1,-1,0,0}, dy={0,0,1,-1};
        while(!q.isEmpty()){
            Point p=q.removeFirst();
            for(int i=0;i<4;i++){ int nx=p.x+dx[i], ny=p.y+dy[i];
                if(!inBounds(nx,ny)||seen[nx][ny]) continue;
                if(walkable(nx,ny)) return new Point(nx,ny);
                seen[nx][ny]=true; q.add(new Point(nx,ny));
            }
        }
        return ensurePlayerSpawnPoint();
    }
    boolean walkable(int x,int y){ if(x<0||y<0||x>=w||y>=h)return false; char t=tiles[x][y]; if(t=='#' || InterstitialInfrastructureApi.isInterstitialSolid(t) || t==InterstitialInfrastructureApi.VOID_SPACE) return false; if(t=='|'||t=='L'||t=='X'||t=='V'||t=='1'||t=='2'||t=='3'||t=='4'||t=='5'||t=='I'||t=='H'||"YJBKOZPFUSRW D".replace(" ","").indexOf(t)>=0) return false; return true; }
    int locationKey(){ return (((sectorX*10+sectorY)*10+zoneX)*10+zoneY)*100 + (floor+20)*2 + (sewerLayer?1:0); }
    String zoneCoordText(){ return zoneX + "," + zoneY + " in sector " + sectorX + "," + sectorY; }
    String layerText(){ return "Floor " + floor + (sewerLayer ? "B sewer" : ""); }
    java.util.List<String> mapScaffoldLines(){ return diegeticMapLines(floor); }

    java.util.List<String> diegeticMapLines(int layerView){
        ArrayList<String> out = new ArrayList<>();
        out.add("Sector 1,1 known slice. Current generated zone is marked @; unvisited zones remain fogged as [?]. The atlas pregenerates 3x3 sectors, 3x3 zones, 10 floors, and B sewer layers for navigation doctrine.");
        out.add("Layer order is interleaved exactly as traveled: Floor 1, Floor 1B sewer, Floor 2, Floor 2B sewer, through Floor 10 and Floor 10B.");
        out.add("3x3 zone slice:");
        for(int yy=1; yy<=3; yy++){
            StringBuilder row=new StringBuilder("  ");
            for(int xx=1; xx<=3; xx++){
                int currentLayer = (floor-1)*2 + (sewerLayer?1:0);
                if(xx==zoneX && yy==zoneY && layerView==currentLayer) row.append("[@]");
                else if(visitedZones[xx-1][yy-1]) row.append("[v]");
                else row.append("[?]");
            }
            out.add(row.toString());
        }
        out.add("Current local zone type: " + zoneType.label + " — " + zoneType.descriptor);
        out.add("Transition validation: maintenance-bulkhead double doors, sewer hatches, drains, and elevators are carved to reachable rooms during generation.");
        out.add("Faction tinting: claimed rooms inherit their owner color on the tactical map when visible/remembered.");
        out.add("Special symbols: D=maintenance-bulkhead double doors, S=sewer hatch down, v=sewer ladder/drain, E=elevator up.");
        return out;
    }

    java.util.List<String> roomIntelLines(){
        ArrayList<String> out = new ArrayList<>();
        out.add("Current zone type: " + zoneType.label + " — " + zoneType.descriptor);
        out.add("Scavenge chances and owners are generated per room. Faction rooms tint on the tactical map.");
        for(int i=0;i<Math.min(18, roomProfiles.size());i++){
            RoomProfile rp=roomProfiles.get(i); Faction owner=roomFaction(i);
            out.add("room " + i + ": " + (roomSpecials.size()>i && roomSpecials.get(i)?"SPECIAL ":"") + rp.name + " | owner " + owner.label + " | loot " + rp.scavengeChance + "% | " + rp.descriptor + " | features: " + rp.featureText);
        }
        if(roomProfiles.size()>18) out.add("... " + (roomProfiles.size()-18) + " more rooms exist in this zone.");
        return out;
    }
}


class HivewallRoomCacheApi {
    private HivewallRoomCacheApi() {}

    static boolean isHivewallRoom(RoomProfile rp){
        if(rp == null || rp.name == null) return false;
        String n = rp.name.toLowerCase(Locale.ROOT);
        return n.contains("hivewall maintenance") || n.contains("interwall danger");
    }

    static ArrayList<String> seedCacheItems(RoomProfile rp, ZoneType zt, Random rng){
        if(rng == null) rng = new Random(0);
        ArrayList<String> out = new ArrayList<>();
        String low = ((rp==null||rp.name==null?"":rp.name) + " " + (rp==null||rp.descriptor==null?"":rp.descriptor)).toLowerCase(Locale.ROOT);
        String[] maintenance = new String[]{"Sealed maintenance tool chest","Voidside water condenser flask","Collapsed arcology salvage crate","Machine part","Sacred wire bundle","Tool bundle","Water purification tab","Filter mask"};
        String[] danger = new String[]{"Interwall ration reserve crate","Forgotten flak bundle","Abandoned las-locker contents","Rogue automata service core","Sealed tech relic case","Wanted criminal stash roll","Cult-sealed reliquary packet","Collapsed arcology salvage crate","Monoblade sliver","Data spike"};
        String[] pool = low.contains("danger") ? danger : maintenance;
        int count = low.contains("danger") ? 4 + rng.nextInt(3) : 5 + rng.nextInt(3);
        for(int i=0; i<count; i++){
            String item = pool[Math.floorMod(rng.nextInt() + i, pool.length)];
            if(ItemCatalog.get(item) != null) out.add(item);
        }
        if(out.isEmpty()) out.add("Collapsed arcology salvage crate");
        return out;
    }

    static ItemProvenanceRecord cacheProvenance(String item, World w, int roomId, RoomProfile rp, int turn){
        Faction f = Faction.NONE;
        if(w != null && roomId >= 0 && roomId < w.roomFactions.size()) f = w.roomFactions.get(roomId);
        String roomName = rp == null ? "hivewall room" : rp.name;
        String epoch = compactHistorySnippet(w == null ? null : w.zoneEpochHistory);
        String facility = compactHistorySnippet(w == null ? null : w.zoneFacilityHistory);
        String maker = (f == null || f == Faction.NONE ? "abandoned interwall source" : f.label + " abandoned interwall source") + " / " + roomName;
        String inputs = "sealed hivewall cache; faction epochs=" + epoch + "; facility ledger=" + facility;
        String route = "sealed in exterior hivewall/interwall space -> persistent room cache -> recoverable by scavenging";
        ItemProvenanceRecord pr = ItemProvenanceRecord.of(item, f, maker, w, turn, inputs, route);
        pr.place = (w == null ? "unknown hivewall" : w.zoneName + " / " + w.sectorName + " / " + w.zoneType.label) + " / room #" + roomId;
        pr.chain = maker + " -> sealed behind hivewall -> room.cache." + (w==null?0:w.locationKey()) + "." + Math.max(0, roomId);
        return pr;
    }

    static String abandonmentReason(World w, Faction f, Random r){
        String epoch = compactHistorySnippet(w == null ? null : w.zoneEpochHistory);
        String facility = compactHistorySnippet(w == null ? null : w.zoneFacilityHistory);
        if(f == Faction.ROGUE_MACHINE) return "Sealed after a Mechanist Collegia service work went dark; " + facility + "; remembered in epoch: " + epoch + ".";
        if(f == Faction.CULTIST || f == Faction.HERETIC) return "Hidden devotional traffic colonized this interwall void after an occupation dispute; " + epoch + ".";
        if(f == Faction.BANDIT) return "A wanted criminal crew cut into old maintenance space and used facility stores as a refuge; " + facility + ".";
        if(f == Faction.MUTANT) return "Collapsed arcology structure opened a forgotten pocket later claimed by powerful mutants; " + epoch + ".";
        return "Abandoned during old maintenance collapse; " + epoch + "; " + facility + ".";
    }

    static void attachHostileProvenance(NpcEntity npc, World w, int roomId, Faction f, Random r){
        if(npc == null || w == null) return;
        PersonnelProvenanceRecord p = PersonnelProvenanceApi.create(f, w.zoneType, PersonnelProvenanceApi.roomLabel(w, roomId), w.sectorX, w.sectorY, w.zoneX, w.zoneY, w.floor, w.sewerLayer, r);
        p.originMode = f == Faction.ROGUE_MACHINE ? "rogue-automata-origin" : (f == Faction.BANDIT ? "criminal-hideout-arrival" : "interwall-occupation");
        p.originSiteId = "hivewall.interwall." + w.locationKey() + ".r" + roomId;
        p.originRoom = PersonnelProvenanceApi.roomLabel(w, roomId);
        p.populationPool = "sealed interwall threat pocket";
        p.upbringing = "Not drawn from ordinary barracks, creche, or rail population. Presence derives from abandoned hivewall/interwall access and prior faction-control history.";
        p.arrivalRoute = abandonmentReason(w, f, r);
        p.backstory = (f == Faction.ROGUE_MACHINE ? "Rogue automata from abandoned Mechanist Collegia service works" : f == Faction.BANDIT ? "Wanted murderer or criminal hiding in interwall void maintenance spaces" : f == Faction.MUTANT ? "Powerful mutant occupying a collapse pocket in the outer hivewall" : "Cultist or heretic sheltered in a sealed interwall chamber") + "; room=" + p.originRoom + "; route=" + p.arrivalRoute;
        npc.provenance = p;
    }

    static String compactHistorySnippet(String s){
        if(s == null || s.isBlank()) return "unsynthesized history";
        String t = s.replace(";;", " | ").trim();
        return t.length() > 160 ? t.substring(0, 157) + "..." : t;
    }
}

class BoundedOuterHiveWallApi {
    // 0.8.79 IMPLEMENTED/API SECTION:
    // The bounded hivewall layer runs after ordinary zone construction/repair and before
    // interstitial mass conversion. It is deliberately conservative: a rectangular high-wall
    // envelope is stamped around the active zone slice, outside-of-envelope cells become void
    // abyss, an exterior maintenance corridor loop is carved inside the wall, one hivewall
    // maintenance room is bolted onto the loop, and three or four abandoned interwall rooms
    // are added outside the ordinary room quota. A tighter contour implementation can follow
    // room/corridor silhouettes more closely; this code creates the durable API seam and
    // playable map material without expensive tracing.
    static final char HIVEWALL_CORRIDOR = '-';

    static class Result {
        int inset, corridors, maintenanceRooms, dangerRooms, voidTiles, highWallTiles, transitionThroats;
        String summary(){ return "inset="+inset+" corridors="+corridors+" transitionThroats="+transitionThroats+" maintenanceRooms="+maintenanceRooms+" dangerRooms="+dangerRooms+" voidTiles="+voidTiles+" highWallTiles="+highWallTiles; }
    }

    static String policySummary(){
        return "post-validation bounded hivewall: exterior maintenance loop, one bolted-on maintenance room, 3-4 abandoned high-danger interwall rooms, void abyss outside high-wall envelope";
    }
    static int maintenanceRoomTarget(){ return 1; }
    static int dangerRoomTargetMin(){ return 3; }
    static int dangerRoomTargetMax(){ return 4; }
    static String[] dangerOccupantBands(){ return new String[]{"powerful mutant", "wanted criminal", "rogue automata", "cultist cell", "heretic survivor"}; }

    static Result apply(World world, Random r){
        Result res = new Result();
        if(world == null || world.tiles == null || world.w < 40 || world.h < 32) return res;
        int inset = chooseEnvelopeInset(world);
        res.inset = inset;
        res.corridors = carveMaintenanceLoop(world, inset);
        res.highWallTiles = stampExteriorMaintenanceBulkheadBands(world, inset);
        res.voidTiles = stampVoidBeyondExteriorBulkhead(world, inset);
        // Edge transitions must remain connected to the active sector, not marooned as
        // double doors floating in the post-envelope void layer.
        res.transitionThroats = restoreEdgeTransitionCorridors(world, inset);
        res.maintenanceRooms = addMaintenanceRoom(world, r, inset) ? 1 : 0;
        // 0.9.10fu: disable the earlier outside-corridor danger-room pockets until
        // they can be reintroduced inside the new bulkhead/corridor/bulkhead envelope.
        // The space beyond the outer bulkhead is now explicit void, not room-bearing wall mass.
        res.dangerRooms = 0;
        // 0.9.10ji: do not run the bulkhead/throat passes a second time. The repeated
        // pass was the isolated source of doubled top-edge maintenance banding in audit.
        return res;
    }

    static int chooseEnvelopeInset(World world){
        int bestInset = 8;
        int bestScore = Integer.MAX_VALUE;
        for(int inset=6; inset<=14; inset++){
            if(inset >= world.w/3 || inset >= world.h/3) break;
            int score = perimeterRoomCollisionScore(world, inset) * 1000 + Math.abs(inset - 8) * 7;
            if(score < bestScore){ bestScore = score; bestInset = inset; }
        }
        return bestInset;
    }

    static int perimeterRoomCollisionScore(World world, int inset){
        int score = 0;
        for(int x=inset; x<=world.w-1-inset; x++){
            if(world.roomIds[x][inset] >= 0) score++;
            if(world.roomIds[x][world.h-1-inset] >= 0) score++;
        }
        for(int y=inset; y<=world.h-1-inset; y++){
            if(world.roomIds[inset][y] >= 0) score++;
            if(world.roomIds[world.w-1-inset][y] >= 0) score++;
        }
        return score;
    }

    static int stampVoidOutsideEnvelope(World world){
        int made = 0;
        for(int x=0; x<world.w; x++) for(int y=0; y<world.h; y++){
            if(x <= 1 || y <= 1 || x >= world.w-2 || y >= world.h-2){
                if(isTransitionTile(world.tiles[x][y])) continue;
                if(world.tiles[x][y] != InterstitialInfrastructureApi.VOID_SPACE){ world.tiles[x][y] = InterstitialInfrastructureApi.VOID_SPACE; made++; }
            }
        }
        return made;
    }

    static boolean isTransitionTile(char t){ return t=='D' || t=='S' || t=='v' || t=='E'; }

    static int reinforceHighWallEnvelope(World world){
        int made = 0;
        for(int x=2; x<=world.w-3; x++){
            if(world.roomIds[x][2] < 0 && !isTransitionTile(world.tiles[x][2]) && world.tiles[x][2] != '#'){ world.tiles[x][2] = '#'; made++; }
            if(world.roomIds[x][world.h-3] < 0 && !isTransitionTile(world.tiles[x][world.h-3]) && world.tiles[x][world.h-3] != '#'){ world.tiles[x][world.h-3] = '#'; made++; }
        }
        for(int y=2; y<=world.h-3; y++){
            if(world.roomIds[2][y] < 0 && !isTransitionTile(world.tiles[2][y]) && world.tiles[2][y] != '#'){ world.tiles[2][y] = '#'; made++; }
            if(world.roomIds[world.w-3][y] < 0 && !isTransitionTile(world.tiles[world.w-3][y]) && world.tiles[world.w-3][y] != '#'){ world.tiles[world.w-3][y] = '#'; made++; }
        }
        return made;
    }

    static int stampExteriorMaintenanceBulkheadBands(World world, int inset){
        // 0.9.10fu: the hivewall perimeter is no longer a generic cube edge.
        // The explicit layer order is: playable interior -> inner bulkhead ->
        // exterior maintenance corridor -> outer bulkhead -> void abyss.
        int made = 0;
        int lx=inset, rx=world.w-1-inset, ty=inset, by=world.h-1-inset;
        for(int x=lx; x<=rx; x++){
            made += stampBulkheadIfOpen(world, x, ty+1);
            made += stampBulkheadIfOpen(world, x, ty-1);
            made += stampBulkheadIfOpen(world, x, by-1);
            made += stampBulkheadIfOpen(world, x, by+1);
        }
        for(int y=ty; y<=by; y++){
            made += stampBulkheadIfOpen(world, lx+1, y);
            made += stampBulkheadIfOpen(world, lx-1, y);
            made += stampBulkheadIfOpen(world, rx-1, y);
            made += stampBulkheadIfOpen(world, rx+1, y);
        }
        return made;
    }

    static int stampBulkheadIfOpen(World world, int x, int y){
        if(!world.inBounds(x,y) || world.roomIds[x][y] >= 0) return 0;
        char t = world.tiles[x][y];
        if(isTransitionTile(t) || t == HIVEWALL_CORRIDOR) return 0;
        if(t == '#') return 0;
        world.tiles[x][y] = '#';
        return 1;
    }

    static int stampVoidBeyondExteriorBulkhead(World world, int inset){
        int made = 0;
        int lx=inset, rx=world.w-1-inset, ty=inset, by=world.h-1-inset;
        for(int x=0; x<world.w; x++) for(int y=0; y<world.h; y++){
            boolean outsideOuterBulkhead = x < lx-1 || x > rx+1 || y < ty-1 || y > by+1;
            if(!outsideOuterBulkhead) continue;
            if(world.roomIds[x][y] >= 0 || isTransitionTile(world.tiles[x][y])) continue;
            if(world.tiles[x][y] != InterstitialInfrastructureApi.VOID_SPACE){
                world.tiles[x][y] = InterstitialInfrastructureApi.VOID_SPACE;
                made++;
            }
        }
        return made;
    }

    static int restoreEdgeTransitionCorridors(World world, int inset){
        int made = 0;
        for(int x=0; x<world.w; x++){
            if(isTransitionTile(world.tiles[x][0])) made += carveTransitionThroat(world, x, 0, 0, 1, inset);
            if(isTransitionTile(world.tiles[x][world.h-1])) made += carveTransitionThroat(world, x, world.h-1, 0, -1, inset);
        }
        for(int y=0; y<world.h; y++){
            if(isTransitionTile(world.tiles[0][y])) made += carveTransitionThroat(world, 0, y, 1, 0, inset);
            if(isTransitionTile(world.tiles[world.w-1][y])) made += carveTransitionThroat(world, world.w-1, y, -1, 0, inset);
        }
        return made;
    }

    static int carveTransitionThroat(World world, int ex, int ey, int dx, int dy, int inset){
        int made = 0;
        int len = Math.max(4, inset + 3);
        boolean vertical = dy != 0;
        for(int step=1; step<=len; step++){
            for(int off=-1; off<=1; off++){
                int x = ex + dx * step + (vertical ? off : 0);
                int y = ey + dy * step + (vertical ? 0 : off);
                if(!world.inBounds(x,y) || world.roomIds[x][y] >= 0) continue;
                if(isTransitionTile(world.tiles[x][y])) continue;
                char t = world.tiles[x][y];
                if(t == InterstitialInfrastructureApi.VOID_SPACE || t == '#' || t == HIVEWALL_CORRIDOR){
                    world.tiles[x][y] = HIVEWALL_CORRIDOR;
                    made++;
                }
            }
        }
        return made;
    }

    static int carveMaintenanceLoop(World world, int inset){
        int made = 0;
        int lx=inset, rx=world.w-1-inset, ty=inset, by=world.h-1-inset;
        for(int x=lx; x<=rx; x++){
            made += carveHivewallCorridor(world, x, ty);
            made += carveHivewallCorridor(world, x, by);
        }
        for(int y=ty; y<=by; y++){
            made += carveHivewallCorridor(world, lx, y);
            made += carveHivewallCorridor(world, rx, y);
        }
        return made;
    }

    static int carveHivewallCorridor(World world, int x, int y){
        if(!world.inBounds(x,y) || world.roomIds[x][y] >= 0) return 0;
        char t = world.tiles[x][y];
        if(isTransitionTile(t)) return 0;
        if(t == HIVEWALL_CORRIDOR) return 0;
        world.tiles[x][y] = HIVEWALL_CORRIDOR;
        return 1;
    }

    static boolean addMaintenanceRoom(World world, Random r, int inset){
        int rw = 11, rh = 5;
        Rectangle[] candidates = new Rectangle[]{
            new Rectangle(world.w/2-rw/2, inset+1, rw, rh),
            new Rectangle(world.w/2-rw/2, world.h-inset-rh-1, rw, rh),
            new Rectangle(inset+1, world.h/2-rw/2, rh, rw),
            new Rectangle(world.w-inset-rh-1, world.h/2-rw/2, rh, rw)
        };
        for(Rectangle rect: candidates){
            if(!canPlaceInterwallRoom(world, rect, true)) continue;
            int rid = carveAddedRoom(world, rect, new RoomProfile(
                "Hivewall Maintenance Room",
                "bolted-on high-wall service room attached after the normal zone was built; one door returns to the main arcology, while left and right maintenance exits run into the exterior wall loop",
                58, Faction.NONE,
                new String[]{"machine parts","wire bundle","rusted tool","sealed water ration"},
                new char[]{'N','q','b'}), Faction.NONE, true);
            connectRoomToLoop(world, rect, inset);
            connectRoomToNearestInteriorCorridor(world, rect);
            return rid >= 0;
        }
        return false;
    }

    static int addDangerRooms(World world, Random r, int inset, int desired){
        int made = 0;
        ArrayList<Rectangle> candidates = new ArrayList<>();
        int[][] points = new int[][]{
            {world.w/4, inset-5, 9, 4}, {world.w/2+18, inset-5, 9, 4}, {world.w*3/4-8, inset-5, 9, 4},
            {world.w/4, world.h-inset+1, 9, 4}, {world.w/2+14, world.h-inset+1, 9, 4}, {world.w*3/4-10, world.h-inset+1, 9, 4},
            {inset-5, world.h/4, 4, 9}, {inset-5, world.h*3/4-8, 4, 9},
            {world.w-inset+1, world.h/4, 4, 9}, {world.w-inset+1, world.h*3/4-8, 4, 9}
        };
        for(int[] v: points) candidates.add(new Rectangle(v[0], v[1], v[2], v[3]));
        Collections.shuffle(candidates, r==null?new Random(0):r);
        for(Rectangle rect: candidates){
            if(made >= desired) break;
            if(!canPlaceInterwallRoom(world, rect, false)) continue;
            Faction f = dangerFaction(world, r, made);
            RoomProfile rp = new RoomProfile(
                "Abandoned Interwall Danger Room",
                "scrap-choked forgotten chamber between the active zone and the outer arcology wall; old collapse dust, abandoned reserves, and something dangerous have been sealed here too long",
                82, f,
                new String[]{"forgotten weapon cache","old armor bundle","tech salvage","sealed reserve crate","wanted poster scrap"},
                new char[]{'!','?','*','b'});
            int rid = carveAddedRoom(world, rect, rp, f, true);
            if(rid >= 0){ connectDangerRoomToLoop(world, rect, inset); made++; }
        }
        return made;
    }

    static Faction dangerFaction(World world, Random r, int n){
        Faction[] f = new Faction[]{Faction.MUTANT, Faction.CULTIST, Faction.HERETIC, Faction.ROGUE_MACHINE, Faction.BANDIT};
        return f[Math.floorMod((r==null?0:r.nextInt(1000)) + n + (world==null?0:world.zoneType.ordinal()), f.length)];
    }

    static boolean canPlaceInterwallRoom(World world, Rectangle rect, boolean maintenance){
        if(rect.x < 3 || rect.y < 3 || rect.x + rect.width >= world.w-3 || rect.y + rect.height >= world.h-3) return false;
        for(int x=rect.x-1; x<rect.x+rect.width+1; x++) for(int y=rect.y-1; y<rect.y+rect.height+1; y++){
            if(!world.inBounds(x,y)) return false;
            if(world.roomIds[x][y] >= 0) return false;
            char t = world.tiles[x][y];
            if(isTransitionTile(t) || t=='Q' || t=='T') return false;
            if(!maintenance && world.walkable(x,y) && t != HIVEWALL_CORRIDOR) return false;
        }
        return true;
    }

    static int carveAddedRoom(World world, Rectangle rect, RoomProfile profile, Faction faction, boolean special){
        int rid = world.rooms.size();
        world.rooms.add(rect);
        world.roomProfiles.add(profile);
        world.roomFactions.add(faction == null ? Faction.NONE : faction);
        world.roomSpecials.add(special);
        for(int x=rect.x; x<rect.x+rect.width; x++) for(int y=rect.y; y<rect.y+rect.height; y++){
            if(!world.inBounds(x,y)) continue;
            boolean edge = x==rect.x || y==rect.y || x==rect.x+rect.width-1 || y==rect.y+rect.height-1;
            world.tiles[x][y] = edge ? '#' : '.';
            world.roomIds[x][y] = edge ? -1 : rid;
        }
        // Place a few interior scrap/cache markers without blocking the whole room.
        Point c = world.center(rect);
        if(world.inBounds(c.x,c.y)){ world.tiles[c.x][c.y] = special ? '?' : 'N'; world.roomIds[c.x][c.y] = rid; }
        return rid;
    }

    static void connectRoomToLoop(World world, Rectangle rect, int inset){
        int cy = rect.y + rect.height/2;
        int cx = rect.x + rect.width/2;
        Point left = new Point(rect.x, cy), right = new Point(rect.x+rect.width-1, cy);
        world.tiles[left.x][left.y] = '/';
        world.tiles[right.x][right.y] = '/';
        if(rect.y <= inset+2){ carveHorizontalToLoop(world, rect.x-1, cy, -1, inset); carveHorizontalToLoop(world, rect.x+rect.width, cy, 1, world.w-1-inset); }
        else if(rect.y > world.h/2){ carveHorizontalToLoop(world, rect.x-1, cy, -1, inset); carveHorizontalToLoop(world, rect.x+rect.width, cy, 1, world.w-1-inset); }
        else { world.tiles[cx][rect.y] = '/'; world.tiles[cx][rect.y+rect.height-1] = '/'; }
    }

    static void carveHorizontalToLoop(World world, int x, int y, int step, int targetX){
        while(world.inBounds(x,y) && ((step < 0 && x >= targetX) || (step > 0 && x <= targetX))){
            if(world.roomIds[x][y] < 0) world.tiles[x][y] = HIVEWALL_CORRIDOR;
            x += step;
        }
    }

    static void connectDangerRoomToLoop(World world, Rectangle rect, int inset){
        int cx = rect.x + rect.width/2, cy = rect.y + rect.height/2;
        if(rect.y < inset){
            world.tiles[cx][rect.y+rect.height-1] = '/';
            for(int y=rect.y+rect.height; y<=inset; y++) if(world.inBounds(cx,y) && world.roomIds[cx][y] < 0) world.tiles[cx][y] = HIVEWALL_CORRIDOR;
        } else if(rect.y > world.h-inset){
            world.tiles[cx][rect.y] = '/';
            for(int y=rect.y-1; y>=world.h-1-inset; y--) if(world.inBounds(cx,y) && world.roomIds[cx][y] < 0) world.tiles[cx][y] = HIVEWALL_CORRIDOR;
        } else if(rect.x < inset){
            world.tiles[rect.x+rect.width-1][cy] = '/';
            for(int x=rect.x+rect.width; x<=inset; x++) if(world.inBounds(x,cy) && world.roomIds[x][cy] < 0) world.tiles[x][cy] = HIVEWALL_CORRIDOR;
        } else {
            world.tiles[rect.x][cy] = '/';
            for(int x=rect.x-1; x>=world.w-1-inset; x--) if(world.inBounds(x,cy) && world.roomIds[x][cy] < 0) world.tiles[x][cy] = HIVEWALL_CORRIDOR;
        }
    }

    static void connectRoomToNearestInteriorCorridor(World world, Rectangle rect){
        Point start = new Point(rect.x + rect.width/2, rect.y + rect.height);
        if(!world.inBounds(start.x,start.y)) start = new Point(rect.x + rect.width/2, rect.y-1);
        if(!world.inBounds(start.x,start.y)) return;
        if(world.roomIds[start.x][start.y] < 0) world.tiles[start.x][start.y] = '#';
        Point anchor = nearestInteriorCorridor(world, start);
        if(anchor == null) return;
        java.util.List<Point> best = null;
        int bestScore = Integer.MAX_VALUE;
        for(java.util.List<Point> path: WorldGenerationApi.emergencyOrthogonalCandidates(world, start, anchor)){
            if(!validInteriorConnector(world, path, start, anchor)) continue;
            int turns = WorldGenerationApi.countOrthogonalTurns(path);
            int score = path.size() + turns * 10;
            if(score < bestScore){ bestScore = score; best = path; }
        }
        if(best == null) return;
        char cg = world.zoneType.corridorGlyph(world.r);
        for(int i=0; i<best.size()-1; i++){
            Point p = best.get(i);
            if(world.inBounds(p.x,p.y) && world.roomIds[p.x][p.y] < 0) world.tiles[p.x][p.y] = cg;
        }
        if(rect.y + rect.height < world.h-1) world.tiles[rect.x + rect.width/2][rect.y+rect.height-1] = '/';
    }

    static Point nearestInteriorCorridor(World world, Point start){
        Point best = null;
        int bestD = Integer.MAX_VALUE;
        for(int x=0; x<world.w; x++) for(int y=0; y<world.h; y++){
            char t = world.tiles[x][y];
            if(t == HIVEWALL_CORRIDOR || world.roomIds[x][y] >= 0) continue;
            if(!world.isCorridorGlyph(t) || !world.walkable(x,y)) continue;
            if(x < 16 || y < 16 || x > world.w-17 || y > world.h-17) continue;
            int d = Math.abs(x-start.x)+Math.abs(y-start.y);
            if(d < bestD){ bestD = d; best = new Point(x,y); }
        }
        return best;
    }

    static boolean validInteriorConnector(World world, java.util.List<Point> path, Point start, Point anchor){
        if(path == null || path.size() < 2 || !path.get(0).equals(start) || !path.get(path.size()-1).equals(anchor)) return false;
        for(int i=0; i<path.size(); i++){
            Point p = path.get(i);
            if(!world.inBounds(p.x,p.y) || world.roomIds[p.x][p.y] >= 0) return false;
            if(i == path.size()-1) return world.isCorridorGlyph(world.tiles[p.x][p.y]) && world.tiles[p.x][p.y] != HIVEWALL_CORRIDOR;
            char t = world.tiles[p.x][p.y];
            if(t != '#' && t != HIVEWALL_CORRIDOR && !InterstitialInfrastructureApi.isInterstitialSolid(t)) return false;
        }
        return true;
    }

    static int populateDangerRooms(World world, Random r){
        if(world == null || world.rooms == null || world.roomProfiles == null) return 0;
        int made = 0;
        for(int i=0; i<world.roomProfiles.size(); i++){
            RoomProfile rp = world.roomProfiles.get(i);
            if(rp == null || rp.name == null || !rp.name.contains("Interwall Danger")) continue;
            Rectangle rr = world.rooms.get(i);
            int count = 1 + ((r==null?0:r.nextInt(100)) < 35 ? 1 : 0);
            for(int n=0; n<count; n++){
                Point p = world.randomOpenPointInRoom(rr);
                if(p == null || world.npcAt(p.x,p.y) != null) continue;
                Faction f = world.roomFaction(i);
                if(f == Faction.NONE) f = dangerFaction(world, r, i+n);
                NpcEntity npc = NpcEntity.create(f, world.zoneType, p.x, p.y, r==null?new Random():r);
                npc.state = "Hostile";
                npc.role = dangerRole(f);
                HivewallRoomCacheApi.attachHostileProvenance(npc, world, i, f, r==null?new Random():r);
                world.npcs.add(npc);
                made++;
            }
        }
        return made;
    }

    static String dangerRole(Faction f){
        if(f == Faction.ROGUE_MACHINE) return "Rogue Interwall Automata";
        if(f == Faction.MUTANT) return "Powerful Interwall Mutant";
        if(f == Faction.CULTIST || f == Faction.HERETIC) return "Hidden Interwall Heretic";
        if(f == Faction.BANDIT) return "Wanted Interwall Criminal";
        return "Interwall Threat";
    }
}

class InterstitialInfrastructureApi {
    private InterstitialInfrastructureApi() {}

    static final char VOID_SPACE = ' ';
    static final char[] STRUCTURAL_BLOCKERS = new char[]{'%', '&', '^', '8', '0'};
    static final char[] BURIED_FEATURES = new char[]{'*', '?', '!', '%', '&', '^', '8', '0'};

    static boolean isInterstitialSolid(char c){
        return c=='%' || c=='&' || c=='^' || c=='8' || c=='0' || c=='*' || c=='?' || c=='!';
    }

    static String materialName(char c){
        switch(c){
            case '%': return "support beam pack";
            case '&': return "gantry lattice";
            case '^': return "sealed conveyor way";
            case '8': return "pipe and pressure vessel bundle";
            case '0': return "cable conduit column";
            case '*': return "collapsed debris pocket";
            case '?': return "buried cache pocket";
            case '!': return "dangerous buried reserve";
            case ' ': return "void space";
            default: return "bulkhead wall";
        }
    }

    static int applyInterstitialMass(World world, Random r){
        if(world == null || world.tiles == null) return 0;
        int converted = 0;
        for(int x=1; x<world.w-1; x++) for(int y=1; y<world.h-1; y++){
            if(world.tiles[x][y] != '#' || world.roomIds[x][y] >= 0) continue;
            if(isBulkheadShell(world, x, y)) continue;
            world.tiles[x][y] = structuralBlockerFor(world.zoneType, x, y, r);
            converted++;
        }
        return converted;
    }

    static boolean isBulkheadShell(World world, int x, int y){
        // Preserve ordinary wall identity for any solid tile adjacent to rooms, corridors, doors, transitions,
        // fixtures, or map edge. The interstitial layer only takes over deeper mass between constructed spaces.
        if(x <= 1 || y <= 1 || x >= world.w-2 || y >= world.h-2) return true;
        for(int dx=-1; dx<=1; dx++) for(int dy=-1; dy<=1; dy++){
            if(dx==0 && dy==0) continue;
            int nx=x+dx, ny=y+dy;
            if(!world.inBounds(nx,ny)) return true;
            if(world.roomIds[nx][ny] >= 0) return true;
            char t = world.tiles[nx][ny];
            if(t != '#' && !isInterstitialSolid(t)) return true;
        }
        return false;
    }

    static char structuralBlockerFor(ZoneType z, int x, int y, Random r){
        int roll = Math.floorMod((x*31 + y*17 + (z==null?0:z.ordinal()*13) + r.nextInt(997)), 100);
        if(z==ZoneType.MECHANICUS_FORGE_CLOISTER || z==ZoneType.MECHANICUS_RELIC_DUCT){
            if(roll < 30) return '0';
            if(roll < 55) return '^';
            if(roll < 78) return '&';
            if(roll < 92) return '8';
            return '%';
        }
        if(z==ZoneType.TRAIN_SERVICE_YARD || z==ZoneType.NEUTRAL_RAIL_DEPOT){
            if(roll < 35) return '^';
            if(roll < 58) return '%';
            if(roll < 78) return '&';
            if(roll < 92) return '8';
            return '0';
        }
        if(z==ZoneType.SEWER_CONDUIT || z==ZoneType.MUTANT_SEWER_CAMP || z==ZoneType.CULTIST_SEWER_CAMP){
            if(roll < 40) return '8';
            if(roll < 62) return '%';
            if(roll < 80) return '0';
            if(roll < 92) return '&';
            return '^';
        }
        if(z==ZoneType.NOBLE_SERVICE_SPINE || z==ZoneType.SECTOR_GOVERNORS_MANSION){
            if(roll < 34) return '%';
            if(roll < 58) return '&';
            if(roll < 76) return '0';
            if(roll < 90) return '8';
            return '^';
        }
        return STRUCTURAL_BLOCKERS[Math.floorMod(roll, STRUCTURAL_BLOCKERS.length)];
    }

    static int seedBuriedFeatures(World world, Random r, int desired){
        if(world == null || desired <= 0) return 0;
        ArrayList<Point> candidates = new ArrayList<>();
        for(int x=2; x<world.w-2; x++) for(int y=2; y<world.h-2; y++){
            if(!isInterstitialSolid(world.tiles[x][y])) continue;
            if(nearWalkable(world, x, y, 2)) continue;
            candidates.add(new Point(x,y));
        }
        Collections.shuffle(candidates, r);
        int made = 0;
        for(Point p: candidates){
            if(made >= desired) break;
            char feature = featureFor(world.zoneType, r, made);
            world.tiles[p.x][p.y] = feature;
            made++;
        }
        return made;
    }

    static char featureFor(ZoneType z, Random r, int n){
        int roll = r.nextInt(100);
        if(roll < 45) return '*';       // collapsed tile/debris token
        if(roll < 75) return '?';       // forgotten tools/weapons/armor/tech/supplies cache marker
        if(roll < 90) return '!';       // dangerous reserve or hazard marker
        return STRUCTURAL_BLOCKERS[(n + r.nextInt(STRUCTURAL_BLOCKERS.length)) % STRUCTURAL_BLOCKERS.length];
    }

    static boolean nearWalkable(World world, int x, int y, int radius){
        for(int dx=-radius; dx<=radius; dx++) for(int dy=-radius; dy<=radius; dy++){
            int nx=x+dx, ny=y+dy;
            if(world.inBounds(nx,ny) && world.walkable(nx,ny)) return true;
        }
        return false;
    }

    static String[] randomTableLines(){
        return new String[]{
            "% support beam pack — dense vertical load-bearing infrastructure mass",
            "& gantry lattice — sealed catwalk/support lattice between constructed rooms",
            "^ conveyor way — solid buried production/material movement channel",
            "8 pipe and pressure vessel bundle — wet, hot, or chemical utility mass",
            "0 cable conduit column — power/data/vox/logic Engine routing spine",
            "* collapsed debris pocket — rubble from older arcology failures",
            "? buried cache pocket — forgotten tools, weapons, armor, technology, supplies, or reserves",
            "! dangerous buried reserve — cache/hazard candidate for breach or hivewall exploration",
            "space void space — empty intrahive abyss outside a bounded arcology-wall envelope"
        };
    }
}

class WorldGenerationApi {
    private WorldGenerationApi() {}

    // 0.9.10ft: 500-1000 is the clamped world-generation weight / variance budget,
    // not a raw width/height edge clamp. Keep slice dimensions human-sized and let
    // this band drive scale variance, room quotas, and spacing pressure.
    static final int MIN_WORLDGEN_WEIGHT = 500;
    static final int MAX_WORLDGEN_WEIGHT = 1000;

    static final WorldGenerationScaleProfile CURRENT_MINIMUM_SCALE = new WorldGenerationScaleProfile(
        "current.weighted", "Current Weighted Slice", 150, 190, 100, 132,
        18, 34, 27, 10, 17, 10,
        "Current weighted-zone scale profile: zone-size settings select a 500-1000 worldgen weight band; dimensions are derived from that band and are not raw 500+ tile edges."
    );

    // Owns world-generation policy that should not remain mixed into gameplay state.
    // Slice dimensions, room quotas, and plaza anchoring route through this surface.

    static WorldSetupSettings activeSettings = WorldSetupSettings.standard();
    static void setActiveSettings(WorldSetupSettings s){ activeSettings = (s == null ? WorldSetupSettings.standard() : s.copy()); }
    static WorldSetupSettings settings(){ return activeSettings == null ? WorldSetupSettings.standard() : activeSettings; }
    static WorldGenerationScaleProfile currentScale(){ return settings().scaleProfile(CURRENT_MINIMUM_SCALE); }

    static int clampWorldgenWeight(int weight){ return Math.max(MIN_WORLDGEN_WEIGHT, Math.min(MAX_WORLDGEN_WEIGHT, weight)); }
    static int minWorldgenWeightForZoneSize(int zoneSize){
        int[] bands = {500, 600, 720, 850};
        return bands[Math.max(0, Math.min(3, zoneSize))];
    }
    static int maxWorldgenWeightForZoneSize(int zoneSize){
        int[] bands = {620, 760, 900, 1000};
        return bands[Math.max(0, Math.min(3, zoneSize))];
    }
    static String worldgenWeightBandLabel(int zoneSize){
        return minWorldgenWeightForZoneSize(zoneSize) + "-" + maxWorldgenWeightForZoneSize(zoneSize);
    }
    static double dimensionScaleForWorldgenWeight(int weight){
        int w = clampWorldgenWeight(weight);
        return 0.88 + ((w - MIN_WORLDGEN_WEIGHT) / (double)(MAX_WORLDGEN_WEIGHT - MIN_WORLDGEN_WEIGHT)) * 0.62;
    }

    static Dimension zoneSliceSize(long sliceSeed){
        WorldGenerationScaleProfile p = currentScale();
        int minW = Math.max(96, p.minWidth);
        int maxW = Math.max(minW, p.maxWidth);
        int minH = Math.max(72, p.minHeight);
        int maxH = Math.max(minH, p.maxHeight);
        int w = minW + Math.floorMod((int)(sliceSeed>>4), Math.max(1, maxW - minW + 1));
        int h = minH + Math.floorMod((int)(sliceSeed>>9), Math.max(1, maxH - minH + 1));
        return new Dimension(w, h);
    }

    static int clampRoomTarget(int value){
        WorldGenerationScaleProfile p = currentScale();
        return Math.max(p.minRooms, Math.min(p.maxRooms, value));
    }

    static int targetRoomCount(ZoneType zoneType, Random r){
        WorldGenerationScaleProfile p = currentScale();
        int base = p.minRooms + r.nextInt(Math.max(1, p.maxRooms - p.minRooms + 1));
        if(zoneType==ZoneType.SECTOR_GOVERNORS_MANSION || zoneType==ZoneType.NEUTRAL_RAIL_DEPOT || zoneType==ZoneType.TRAIN_SERVICE_YARD) base = Math.max(base, Math.max(p.minRooms, p.maxRooms - 4) + r.nextInt(Math.min(5, Math.max(1, p.maxRooms - Math.max(p.minRooms, p.maxRooms - 4) + 1))));
        if(zoneType==ZoneType.HAB_STACK || zoneType==ZoneType.NEUTRAL_CIVILIAN_FLOOR || zoneType==ZoneType.SUMP_MARKET) base = Math.max(base, Math.max(p.minRooms, p.maxRooms - 6) + r.nextInt(Math.min(7, Math.max(1, p.maxRooms - Math.max(p.minRooms, p.maxRooms - 6) + 1))));
        if(zoneType==ZoneType.MECHANICUS_FORGE_CLOISTER || zoneType==ZoneType.MECHANICUS_RELIC_DUCT || zoneType==ZoneType.IMPERIAL_GUARD_BILLET) base = Math.max(base, Math.max(p.minRooms, p.maxRooms - 7) + r.nextInt(Math.min(7, Math.max(1, p.maxRooms - Math.max(p.minRooms, p.maxRooms - 7) + 1))));
        base = (int)Math.round(base * settings().zoneDensityMultiplier());
        return clampRoomTarget(base);
    }

    static Rectangle centralPlazaRect(int width, int height){
        WorldGenerationScaleProfile p = currentScale();
        int pw = Math.min(p.plazaPreferredSize, Math.max(p.plazaMinSize, width-p.edgeMargin));
        int ph = Math.min(p.plazaPreferredSize, Math.max(p.plazaMinSize, height-p.edgeMargin));
        return new Rectangle(Math.max(2, width/2 - pw/2), Math.max(2, height/2 - ph/2), pw, ph);
    }

    static java.util.List<Point> emergencyPathToNearestReachableCorridor(World world, Point start){
        if(world == null || start == null || !world.inBounds(start.x,start.y) || world.roomIds[start.x][start.y] >= 0) return null;
        char st = world.tiles[start.x][start.y];
        if(world.isCorridorGlyph(st) && world.walkable(start.x,start.y)){
            ArrayList<Point> already = new ArrayList<>();
            already.add(start);
            return already;
        }
        if(st != '#') return null;
        boolean[][] reachable = world.reachableFromStart();
        ArrayList<Point> anchors = new ArrayList<>();
        for(int x=0; x<world.w; x++) for(int y=0; y<world.h; y++){
            if(!reachable[x][y] || world.roomIds[x][y] >= 0) continue;
            if(world.isCorridorGlyph(world.tiles[x][y]) && world.walkable(x,y)) anchors.add(new Point(x,y));
        }
        Collections.sort(anchors, (a,b) -> Integer.compare(Math.abs(a.x-start.x)+Math.abs(a.y-start.y), Math.abs(b.x-start.x)+Math.abs(b.y-start.y)));
        java.util.List<Point> best = null;
        int bestScore = Integer.MAX_VALUE;
        int tested = 0;
        for(Point anchor: anchors){
            // 0.9.10ft: weighted larger profiles can still create broad slices, so avoid
            // exhausting hundreds of long candidate polylines during new-world
            // generation. A nearer valid orthogonal corridor is preferred.
            if(tested++ > 24 && best != null) break;
            for(java.util.List<Point> path: emergencyOrthogonalCandidates(world, start, anchor)){
                if(!validEmergencyOrthogonalPath(world, path, start, anchor)) continue;
                int turns = countOrthogonalTurns(path);
                int score = path.size() + turns * 12 + Math.abs(anchor.x-start.x) + Math.abs(anchor.y-start.y);
                if(score < bestScore){ bestScore = score; best = path; }
            }
        }
        return best;
    }

    static java.util.List<java.util.List<Point>> emergencyOrthogonalCandidates(World world, Point start, Point anchor){
        ArrayList<java.util.List<Point>> out = new ArrayList<>();
        if(world == null || start == null || anchor == null) return out;
        out.add(manhattanCorridorPath(world, start, anchor, true));
        out.add(manhattanCorridorPath(world, start, anchor, false));
        int minX = Math.max(1, Math.min(start.x, anchor.x) - 12);
        int maxX = Math.min(world.w-2, Math.max(start.x, anchor.x) + 12);
        int minY = Math.max(1, Math.min(start.y, anchor.y) - 12);
        int maxY = Math.min(world.h-2, Math.max(start.y, anchor.y) + 12);
        for(int x=minX; x<=maxX; x+=2){
            out.add(polylinePath(new Point(start.x,start.y), new Point(x,start.y), new Point(x,anchor.y), new Point(anchor.x,anchor.y)));
        }
        for(int y=minY; y<=maxY; y+=2){
            out.add(polylinePath(new Point(start.x,start.y), new Point(start.x,y), new Point(anchor.x,y), new Point(anchor.x,anchor.y)));
        }
        return out;
    }

    static java.util.List<Point> manhattanCorridorPath(World world, Point start, Point anchor, boolean horizontalFirst){
        if(start == null || anchor == null) return new ArrayList<>();
        if(horizontalFirst) return polylinePath(start, new Point(anchor.x, start.y), anchor);
        return polylinePath(start, new Point(start.x, anchor.y), anchor);
    }

    static java.util.List<Point> polylinePath(Point... points){
        ArrayList<Point> out = new ArrayList<>();
        if(points == null || points.length == 0) return out;
        Point cur = points[0];
        out.add(new Point(cur.x, cur.y));
        for(int i=1; i<points.length; i++){
            Point next = points[i];
            while(cur.x != next.x){ cur = new Point(cur.x + (cur.x < next.x ? 1 : -1), cur.y); out.add(cur); }
            while(cur.y != next.y){ cur = new Point(cur.x, cur.y + (cur.y < next.y ? 1 : -1)); out.add(cur); }
        }
        return out;
    }

    static boolean validEmergencyOrthogonalPath(World world, java.util.List<Point> path, Point start, Point anchor){
        if(world == null || path == null || path.isEmpty()) return false;
        if(!path.get(0).equals(start)) return false;
        if(!path.get(path.size()-1).equals(anchor)) return false;
        for(int i=0; i<path.size(); i++){
            Point p = path.get(i);
            if(!world.inBounds(p.x,p.y) || world.roomIds[p.x][p.y] >= 0) return false;
            if(i > 0){
                Point prev = path.get(i-1);
                int md = Math.abs(p.x-prev.x) + Math.abs(p.y-prev.y);
                if(md != 1) return false;
                // Reject immediate reversal loops without allocating a per-path
                // string HashSet. The polyline generator produces controlled
                // orthogonal segments, so this catches the meaningful loop case
                // while keeping large-zone repair generation fast.
                if(i > 1){
                    Point prev2 = path.get(i-2);
                    if(p.x == prev2.x && p.y == prev2.y) return false;
                }
            }
            char t = world.tiles[p.x][p.y];
            boolean isFinalAnchor = i == path.size()-1 && p.equals(anchor);
            if(isFinalAnchor){
                if(!(world.isCorridorGlyph(t) && world.walkable(p.x,p.y))) return false;
            } else if(t != '#') return false;
        }
        return true;
    }

    static int countOrthogonalTurns(java.util.List<Point> path){
        if(path == null || path.size() < 3) return 0;
        int turns = 0;
        for(int i=2; i<path.size(); i++){
            Point a = path.get(i-2), b = path.get(i-1), c = path.get(i);
            int dx1 = Integer.compare(b.x-a.x, 0), dy1 = Integer.compare(b.y-a.y, 0);
            int dx2 = Integer.compare(c.x-b.x, 0), dy2 = Integer.compare(c.y-b.y, 0);
            if(dx1 != dx2 || dy1 != dy2) turns++;
        }
        return turns;
    }

    /**
     * Phase 3 scaffolding helper.
     *
     * Creates explicit spatial-generation metadata before later placement systems run.
     * This method is intentionally non-invasive and does not alter existing generation behavior.
     */
    public static ZoneGenerationContext createZoneGenerationContext(ZoneType zoneType, int mapWidth, int mapHeight) {
        ZoneGenerationContext context = ZoneGenerationContext.create(zoneType, mapWidth, mapHeight);
        phase3Debug("ZoneGenerationContext", context.toDebugString());
        return context;
    }


    /**
     * Phase 3 local debug helper.
     *
     * Kept deliberately small until the project-wide debug/logging authority is confirmed.
     */
    private static void phase3Debug(String channel, String message) {
        System.out.println("[Phase3][" + channel + "] " + message);
    }


    /**
     * Phase 3 edge-band reservation scaffold.
     *
     * Returns the safe interior generation bounds derived from the context.
     * This does not yet modify placement behavior; it exposes the reserved bounds for the next patch.
     */
    public static ZoneGenerationContext.Rect getPhase3InteriorGenerationBounds(ZoneType zoneType, int mapWidth, int mapHeight) {
        ZoneGenerationContext context = createZoneGenerationContext(zoneType, mapWidth, mapHeight);
        phase3Debug("InteriorBounds", context.interiorGenerationBounds.toString());
        return context.interiorGenerationBounds;
    }


    /**
     * Phase 3 edge-band placement gate.
     *
     * Checks whether an ordinary placement rectangle stays inside the safe interior bounds.
     * This pass adds the helper only; placement call sites should be attached incrementally.
     */
    public static boolean isOrdinaryPlacementInsidePhase3InteriorBounds(
            ZoneGenerationContext context,
            int x,
            int y,
            int width,
            int height) {
        if (context == null) return false;
        ZoneGenerationContext.Rect candidate = new ZoneGenerationContext.Rect(x, y, width, height);
        boolean allowed = context.allowsOrdinaryPlacement(candidate);
        if (!allowed) {
            phase3Debug("EdgeGateReject", "candidate=" + candidate
                    + ", interior=" + context.interiorGenerationBounds
                    + ", zoneFamily=" + context.zoneFamily
                    + ", edgeBandTiles=" + context.edgeBandTiles);
        }
        return allowed;
    }

    public static boolean isOrdinaryPlacementInsidePhase3InteriorBounds(
            ZoneType zoneType,
            int mapWidth,
            int mapHeight,
            int x,
            int y,
            int width,
            int height) {
        ZoneGenerationContext context = createZoneGenerationContext(zoneType, mapWidth, mapHeight);
        return isOrdinaryPlacementInsidePhase3InteriorBounds(context, x, y, width, height);
    }


    /**
     * Phase 3 room-placement edge-band gate.
     *
     * Intended as the first call-site-specific gate for ordinary room placement loops.
     * This helper is conservative: it only accepts ordinary room rectangles fully inside
     * the context's safe interior generation bounds. Edge-authorized rooms require a
     * later explicit authorization path and must not use this ordinary helper.
     */
    public static boolean isOrdinaryRoomPlacementInsidePhase3InteriorBounds(
            ZoneGenerationContext context,
            int x,
            int y,
            int width,
            int height) {
        return isOrdinaryPlacementInsidePhase3InteriorBounds(context, x, y, width, height);
    }

    /**
     * Convenience overload for callers that have not yet been refactored to carry context.
     */
    public static boolean isOrdinaryRoomPlacementInsidePhase3InteriorBounds(
            ZoneType zoneType,
            int mapWidth,
            int mapHeight,
            int x,
            int y,
            int width,
            int height) {
        return isOrdinaryPlacementInsidePhase3InteriorBounds(zoneType, mapWidth, mapHeight, x, y, width, height);
    }


    /**
     * Phase 3 room-placement failure accounting scaffold.
     *
     * Placement loops should call this when an ordinary room candidate is rejected
     * because it would violate the protected edge safety band. This is intentionally
     * lightweight until the exact room placement loop is wired.
     */
    public static void recordPhase3RoomPlacementRejectedByEdgeBand(
            ZoneGenerationContext context,
            int x,
            int y,
            int width,
            int height,
            int attemptIndex) {
        ZoneGenerationContext.Rect candidate = ZoneGenerationContext.rect(x, y, width, height);
        phase3Debug("RoomEdgeReject", "attempt=" + attemptIndex
                + ", candidate=" + candidate
                + ", interior=" + (context == null ? "null" : context.interiorGenerationBounds)
                + ", zoneFamily=" + (context == null ? "null" : context.zoneFamily));
    }


    /**
     * Phase 3 ordinary room placement decision helper.
     *
     * This method is intended to be called immediately before an ordinary room candidate
     * is committed to the room list/stamp list. It preserves retry behavior by returning
     * false rather than throwing.
     */
    public static boolean shouldAcceptOrdinaryRoomPlacementPhase3(
            ZoneGenerationContext context,
            int x,
            int y,
            int width,
            int height,
            int attemptIndex) {
        boolean accepted = isOrdinaryRoomPlacementInsidePhase3InteriorBounds(context, x, y, width, height);
        if (!accepted) {
            recordPhase3RoomPlacementRejectedByEdgeBand(context, x, y, width, height, attemptIndex);
        }
        return accepted;
    }


    /**
     * Phase 3 traversable connection validation scaffold.
     *
     * This helper intentionally validates only spatial topology rules in this pass.
     * Runtime tile/pathability integration remains a later validation stage.
     *
     * Rules enforced:
     * - diagonal-only corner contact is invalid,
     * - orthogonal adjacency is required,
     * - both sides must expose traversable receiving tiles,
     * - and future pathability checks may extend this helper.
     */
    public static boolean isPhase3TraversableOrthogonalConnection(
            int ax,
            int ay,
            int aw,
            int ah,
            int bx,
            int by,
            int bw,
            int bh) {

        boolean horizontalTouch =
                (ax + aw == bx || bx + bw == ax)
                && !(ay + ah <= by || by + bh <= ay);

        boolean verticalTouch =
                (ay + ah == by || by + bh == ay)
                && !(ax + aw <= bx || bx + bw <= ax);

        return horizontalTouch || verticalTouch;
    }


    /**
     * Phase 3 direct room-to-room adjacency validation.
     *
     * Returns true when two room rectangles share an orthogonal wall segment.
     * Corner-only contact is invalid. This helper does not yet place the door;
     * it validates that a shared inter-wall can exist.
     */
    public static boolean hasPhase3SharedOrthogonalWall(
            int ax,
            int ay,
            int aw,
            int ah,
            int bx,
            int by,
            int bw,
            int bh) {
        return getPhase3SharedWallLength(ax, ay, aw, ah, bx, by, bw, bh) > 0;
    }

    /**
     * Returns the length of the shared orthogonal wall segment between two room rectangles.
     * Returns 0 for diagonal-only contact, separated rooms, or overlapping rectangles.
     */
    public static int getPhase3SharedWallLength(
            int ax,
            int ay,
            int aw,
            int ah,
            int bx,
            int by,
            int bw,
            int bh) {
        boolean verticalSharedWall = (ax + aw == bx || bx + bw == ax);
        boolean horizontalSharedWall = (ay + ah == by || by + bh == ay);

        if (verticalSharedWall) {
            int overlapStart = Math.max(ay, by);
            int overlapEnd = Math.min(ay + ah, by + bh);
            return Math.max(0, overlapEnd - overlapStart);
        }

        if (horizontalSharedWall) {
            int overlapStart = Math.max(ax, bx);
            int overlapEnd = Math.min(ax + aw, bx + bw);
            return Math.max(0, overlapEnd - overlapStart);
        }

        return 0;
    }

    /**
     * Returns true when a direct room-to-room shared-wall door could be placed.
     *
     * Door feasibility currently requires:
     * - shared orthogonal wall,
     * - shared wall segment length of at least 1 tile,
     * - both room rectangles are ordinary-placement safe,
     * - and diagonal-only contact is rejected.
     *
     * Later passes should add tile occupancy/pathability checks for both receiver tiles.
     */
    public static boolean canPlacePhase3DirectRoomToRoomDoor(
            ZoneGenerationContext context,
            int ax,
            int ay,
            int aw,
            int ah,
            int bx,
            int by,
            int bw,
            int bh) {
        if (context == null) return false;

        if (!context.allowsDirectSharedWallRoomAdjacency()) {
            phase3Debug("DirectRoomDoorReject", "zone family adjacency mode does not allow direct shared-wall rooms; mode="
                    + context.roomAdjacencyMode);
            return false;
        }

        boolean firstRoomSafe = isOrdinaryRoomPlacementInsidePhase3InteriorBounds(context, ax, ay, aw, ah);
        boolean secondRoomSafe = isOrdinaryRoomPlacementInsidePhase3InteriorBounds(context, bx, by, bw, bh);
        if (!firstRoomSafe || !secondRoomSafe) {
            phase3Debug("DirectRoomDoorReject", "room outside interior bounds; firstSafe=" + firstRoomSafe
                    + ", secondSafe=" + secondRoomSafe);
            return false;
        }

        int sharedWallLength = getPhase3SharedWallLength(ax, ay, aw, ah, bx, by, bw, bh);
        boolean allowed = sharedWallLength >= 1;
        if (!allowed) {
            phase3Debug("DirectRoomDoorReject", "no valid shared orthogonal wall; sharedWallLength="
                    + sharedWallLength);
        }
        return allowed;
    }


    /**
     * Phase 3 shared-wall door tile planning helper.
     *
     * Returns a candidate door tile on the shared wall between two orthogonally adjacent rooms.
     * The returned Rect is a 1x1 tile in shared-wall coordinates. Later passes must still
     * validate tile occupancy/pathability on both room sides before final placement.
     *
     * Returns null if the rooms do not share an orthogonal wall.
     */
    public static ZoneGenerationContext.Rect choosePhase3SharedWallDoorTile(
            int ax,
            int ay,
            int aw,
            int ah,
            int bx,
            int by,
            int bw,
            int bh) {

        int sharedLength = getPhase3SharedWallLength(ax, ay, aw, ah, bx, by, bw, bh);
        if (sharedLength <= 0) {
            return null;
        }

        boolean bRightOfA = ax + aw == bx;
        boolean aRightOfB = bx + bw == ax;
        boolean bBelowA = ay + ah == by;
        boolean aBelowB = by + bh == ay;

        if (bRightOfA || aRightOfB) {
            int doorY = Math.max(ay, by) + sharedLength / 2;
            int doorX = bRightOfA ? bx : ax;
            return ZoneGenerationContext.rect(doorX, doorY, 1, 1);
        }

        if (bBelowA || aBelowB) {
            int doorX = Math.max(ax, bx) + sharedLength / 2;
            int doorY = bBelowA ? by : ay;
            return ZoneGenerationContext.rect(doorX, doorY, 1, 1);
        }

        return null;
    }

    /**
     * Phase 3 direct adjacency planning helper for room clustering.
     *
     * This combines shared-wall validation, safe interior-bounds validation, and
     * door-tile candidate selection. It remains a planning helper; later passes
     * should attach it to actual room-cluster generation.
     */
    public static ZoneGenerationContext.Rect planPhase3DirectRoomToRoomDoorCandidate(
            ZoneGenerationContext context,
            int ax,
            int ay,
            int aw,
            int ah,
            int bx,
            int by,
            int bw,
            int bh) {

        if (!canPlacePhase3DirectRoomToRoomDoor(context, ax, ay, aw, ah, bx, by, bw, bh)) {
            return null;
        }

        ZoneGenerationContext.Rect door = choosePhase3SharedWallDoorTile(ax, ay, aw, ah, bx, by, bw, bh);
        if (door == null) {
            phase3Debug("DirectRoomDoorReject", "shared wall existed but no candidate door tile was produced");
            return null;
        }

        if (!context.allowsOrdinaryPlacement(door)) {
            phase3Debug("DirectRoomDoorReject", "door candidate outside safe interior bounds; door=" + door
                    + ", interior=" + context.interiorGenerationBounds);
            return null;
        }

        if (!hasPhase3SafeDirectRoomDoorReceiverTiles(context, ax, ay, aw, ah, bx, by, bw, bh, door)) {
            return null;
        }

        return door;
    }


    /**
     * Phase 3 direct room-to-room receiver-tile planning helper.
     *
     * Given two orthogonally adjacent rooms and a selected shared-wall door tile,
     * returns the two 1x1 receiver tiles that should be traversable on either side
     * of the doorway. This is geometry-only; later passes must query real tile
     * occupancy/pathability.
     *
     * Returns null when the rooms are not orthogonally adjacent in a recognized way.
     */
    public static ZoneGenerationContext.Rect[] getPhase3DirectRoomDoorReceiverTiles(
            int ax,
            int ay,
            int aw,
            int ah,
            int bx,
            int by,
            int bw,
            int bh,
            ZoneGenerationContext.Rect doorTile) {

        if (doorTile == null) return null;

        boolean bRightOfA = ax + aw == bx;
        boolean aRightOfB = bx + bw == ax;
        boolean bBelowA = ay + ah == by;
        boolean aBelowB = by + bh == ay;

        if (bRightOfA) {
            return new ZoneGenerationContext.Rect[] {
                    ZoneGenerationContext.rect(doorTile.x - 1, doorTile.y, 1, 1),
                    ZoneGenerationContext.rect(doorTile.x, doorTile.y, 1, 1)
            };
        }

        if (aRightOfB) {
            return new ZoneGenerationContext.Rect[] {
                    ZoneGenerationContext.rect(doorTile.x, doorTile.y, 1, 1),
                    ZoneGenerationContext.rect(doorTile.x - 1, doorTile.y, 1, 1)
            };
        }

        if (bBelowA) {
            return new ZoneGenerationContext.Rect[] {
                    ZoneGenerationContext.rect(doorTile.x, doorTile.y - 1, 1, 1),
                    ZoneGenerationContext.rect(doorTile.x, doorTile.y, 1, 1)
            };
        }

        if (aBelowB) {
            return new ZoneGenerationContext.Rect[] {
                    ZoneGenerationContext.rect(doorTile.x, doorTile.y, 1, 1),
                    ZoneGenerationContext.rect(doorTile.x, doorTile.y - 1, 1, 1)
            };
        }

        return null;
    }

    /**
     * Geometry-only receiver tile validation for direct room-to-room doors.
     *
     * Confirms that planned receiver tiles exist and remain within the safe interior
     * bounds. This does not yet check live tile occupancy, furniture, hazards, or
     * runtime pathability.
     */
    public static boolean hasPhase3SafeDirectRoomDoorReceiverTiles(
            ZoneGenerationContext context,
            int ax,
            int ay,
            int aw,
            int ah,
            int bx,
            int by,
            int bw,
            int bh,
            ZoneGenerationContext.Rect doorTile) {

        if (context == null || doorTile == null) return false;

        ZoneGenerationContext.Rect[] receivers = getPhase3DirectRoomDoorReceiverTiles(
                ax, ay, aw, ah, bx, by, bw, bh, doorTile);

        if (receivers == null || receivers.length != 2) {
            phase3Debug("DirectRoomDoorReject", "receiver tile planning failed");
            return false;
        }

        boolean firstSafe = context.allowsOrdinaryPlacement(receivers[0]);
        boolean secondSafe = context.allowsOrdinaryPlacement(receivers[1]);

        if (!firstSafe || !secondSafe) {
            phase3Debug("DirectRoomDoorReject", "receiver tiles outside safe interior bounds; first="
                    + receivers[0] + ", second=" + receivers[1]
                    + ", interior=" + context.interiorGenerationBounds);
            return false;
        }

        return true;
    }


    /**
     * Phase 3 pathability adapter.
     *
     * This tiny interface lets later map/tile code plug live tile checks into the
     * receiver-tile validation path without coupling this scaffold to a specific map
     * implementation yet.
     */
    public interface Phase3TileAccess {
        boolean isTraversable(int x, int y);
        boolean isBlocked(int x, int y);
    }

    /**
     * Live receiver-tile validation for direct room-to-room doors.
     *
     * This extends the earlier geometry-only receiver-tile check by consulting a
     * tile access adapter when one is available. A null tileAccess intentionally
     * fails closed so real pathability validation is not silently skipped.
     */
    public static boolean hasPhase3LiveTraversableDirectRoomDoorReceiverTiles(
            ZoneGenerationContext context,
            Phase3TileAccess tileAccess,
            int ax,
            int ay,
            int aw,
            int ah,
            int bx,
            int by,
            int bw,
            int bh,
            ZoneGenerationContext.Rect doorTile) {

        if (context == null || tileAccess == null || doorTile == null) {
            phase3Debug("DirectRoomDoorReject", "live receiver validation missing context/tileAccess/doorTile");
            return false;
        }

        if (!hasPhase3SafeDirectRoomDoorReceiverTiles(context, ax, ay, aw, ah, bx, by, bw, bh, doorTile)) {
            return false;
        }

        ZoneGenerationContext.Rect[] receivers = getPhase3DirectRoomDoorReceiverTiles(
                ax, ay, aw, ah, bx, by, bw, bh, doorTile);

        if (receivers == null || receivers.length != 2) {
            phase3Debug("DirectRoomDoorReject", "live receiver validation could not derive receiver tiles");
            return false;
        }

        boolean firstTraversable = tileAccess.isTraversable(receivers[0].x, receivers[0].y)
                && !tileAccess.isBlocked(receivers[0].x, receivers[0].y);
        boolean secondTraversable = tileAccess.isTraversable(receivers[1].x, receivers[1].y)
                && !tileAccess.isBlocked(receivers[1].x, receivers[1].y);

        if (!firstTraversable || !secondTraversable) {
            phase3Debug("DirectRoomDoorReject", "receiver tile pathability failed; first="
                    + receivers[0] + ", firstTraversable=" + firstTraversable
                    + ", second=" + receivers[1] + ", secondTraversable=" + secondTraversable);
            return false;
        }

        return true;
    }


    /**
     * Phase 3 fail-closed tile access.
     *
     * Used when the real map/tile access layer has not yet been adapted.
     * It intentionally reports all tiles as non-traversable/blocked so live
     * pathability cannot be accidentally skipped.
     */
    public static final class FailClosedPhase3TileAccess implements Phase3TileAccess {
        @Override
        public boolean isTraversable(int x, int y) {
            return false;
        }

        @Override
        public boolean isBlocked(int x, int y) {
            return true;
        }
    }

    /**
     * Phase 3 tile access adapter factory scaffold.
     *
     * Later passes should replace or overload this with the actual map/tile runtime type.
     * Until then, this fails closed.
     */
    public static Phase3TileAccess createPhase3TileAccessFailClosed() {
        phase3Debug("TileAccess", "Using fail-closed Phase3TileAccess; live pathability not connected yet");
        return new FailClosedPhase3TileAccess();
    }


    /**
     * Phase 3 tile access adapter scaffold for MapLayerSurfaceAuthority.
     *
     * This overload documents the expected adapter point for the actual map/surface
     * authority. It remains fail-closed until the authority exposes confirmed
     * traversable/blocked tile queries.
     */
    public static Phase3TileAccess createPhase3TileAccess(final MapLayerSurfaceAuthority surfaceAuthority) {
        if (surfaceAuthority == null) {
            phase3Debug("TileAccess", "MapLayerSurfaceAuthority was null; using fail-closed tile access");
            return createPhase3TileAccessFailClosed();
        }

        return new Phase3TileAccess() {
            @Override
            public boolean isTraversable(int x, int y) {
                return surfaceAuthority.isPhase3TraversableTile(x, y);
            }

            @Override
            public boolean isBlocked(int x, int y) {
                return surfaceAuthority.isPhase3BlockedTile(x, y);
            }
        };
    }


    /**
     * Phase 3 surface tile-state initialization helper.
     *
     * Call this during map/surface setup once the width and height are known.
     * Unknown tiles remain blocked until generation explicitly marks floor/receiver tiles.
     */
    public static void initializePhase3SurfaceTileState(
            MapLayerSurfaceAuthority surfaceAuthority,
            int mapWidth,
            int mapHeight) {
        if (surfaceAuthority == null) {
            phase3Debug("TileState", "Cannot initialize Phase 3 tile state: surface authority is null");
            return;
        }

        surfaceAuthority.initializePhase3TileState(mapWidth, mapHeight);
        phase3Debug("TileState", "Initialized Phase 3 tile state width=" + mapWidth + ", height=" + mapHeight);
    }

    /**
     * Phase 3 helper for marking a generated ordinary floor rectangle.
     *
     * This should be called only after placement validation accepts the rectangle.
     */
    public static void markPhase3GeneratedFloorRect(
            MapLayerSurfaceAuthority surfaceAuthority,
            int x,
            int y,
            int width,
            int height) {
        if (surfaceAuthority == null) {
            phase3Debug("TileState", "Cannot mark floor rect: surface authority is null");
            return;
        }

        surfaceAuthority.markPhase3RectAsFloor(x, y, width, height);
    }

    /**
     * Phase 3 helper for marking generated blocking geometry.
     */
    public static void markPhase3GeneratedBlockedRect(
            MapLayerSurfaceAuthority surfaceAuthority,
            int x,
            int y,
            int width,
            int height) {
        if (surfaceAuthority == null) {
            phase3Debug("TileState", "Cannot mark blocked rect: surface authority is null");
            return;
        }

        surfaceAuthority.markPhase3RectAsBlocked(x, y, width, height);
    }


    /**
     * Phase 3 tile-state ensure helper.
     *
     * Safe setup wrapper: initializes tile state only if it does not already exist.
     * Call when a surface authority and map dimensions are available.
     */
    public static void ensurePhase3SurfaceTileState(
            MapLayerSurfaceAuthority surfaceAuthority,
            int mapWidth,
            int mapHeight) {
        if (surfaceAuthority == null) {
            phase3Debug("TileState", "Cannot ensure Phase 3 tile state: surface authority is null");
            return;
        }

        surfaceAuthority.ensurePhase3TileState(mapWidth, mapHeight);
    }


    /**
     * Phase 3 reserved traversal helper.
     *
     * Door receiver tiles must be reserved before random seating/fixtures are placed.
     */
    public static void markPhase3DoorReceiverTilesReserved(
            MapLayerSurfaceAuthority surfaceAuthority,
            ZoneGenerationContext.Rect[] receiverTiles) {
        if (surfaceAuthority == null || receiverTiles == null) {
            phase3Debug("ReservedTraversal", "Cannot reserve receiver tiles: missing surface or receiver array");
            return;
        }

        for (ZoneGenerationContext.Rect receiver : receiverTiles) {
            if (receiver != null) {
                surfaceAuthority.markPhase3ReservedTraversalTile(receiver.x, receiver.y);
            }
        }
    }

    /**
     * Phase 3 random seating / blocking fixture safety gate.
     *
     * Seating and other blocking fixtures may be placed only after door receiver tiles
     * and required traversal lanes are reserved.
     */
    public static boolean canPlacePhase3RandomSeatingOrBlockingFixture(
            MapLayerSurfaceAuthority surfaceAuthority,
            int x,
            int y,
            int width,
            int height) {
        if (surfaceAuthority == null) {
            phase3Debug("FixturePlacementReject", "surface authority is null");
            return false;
        }

        boolean allowed = surfaceAuthority.canPlacePhase3BlockingFixtureRect(x, y, width, height);
        if (!allowed) {
            phase3Debug("FixturePlacementReject", "fixture would block reserved/invalid traversal tile; rect="
                    + ZoneGenerationContext.rect(x, y, width, height));
        }
        return allowed;
    }

    public static void markPhase3RandomSeatingOrBlockingFixturePlaced(
            MapLayerSurfaceAuthority surfaceAuthority,
            int x,
            int y,
            int width,
            int height) {
        if (surfaceAuthority == null) {
            phase3Debug("FixturePlacementReject", "cannot mark fixture: surface authority is null");
            return;
        }

        surfaceAuthority.markPhase3FixtureRectAsBlocked(x, y, width, height);
    }


    /**
     * Phase 3 direct room-to-room door planning with receiver reservation.
     *
     * This should be used by room clustering after candidate rooms are accepted
     * and before random seating / blocking fixtures are placed.
     */
    public static ZoneGenerationContext.Rect planAndReservePhase3DirectRoomToRoomDoorCandidate(
            MapLayerSurfaceAuthority surfaceAuthority,
            ZoneGenerationContext context,
            int ax,
            int ay,
            int aw,
            int ah,
            int bx,
            int by,
            int bw,
            int bh) {

        ZoneGenerationContext.Rect door = planPhase3DirectRoomToRoomDoorCandidate(
                context, ax, ay, aw, ah, bx, by, bw, bh);

        if (door == null) {
            return null;
        }

        ZoneGenerationContext.Rect[] receivers = getPhase3DirectRoomDoorReceiverTiles(
                ax, ay, aw, ah, bx, by, bw, bh, door);

        if (receivers == null || receivers.length != 2) {
            phase3Debug("DirectRoomDoorReject", "cannot reserve receiver tiles: receiver planning failed");
            return null;
        }

        markPhase3DoorReceiverTilesReserved(surfaceAuthority, receivers);
        phase3Debug("DirectRoomDoorReserve", "door=" + door
                + ", receiverA=" + receivers[0]
                + ", receiverB=" + receivers[1]);

        return door;
    }


    /**
     * Phase 3 post-fixture reserved traversal validation.
     *
     * This lightweight helper checks that a reserved traversal tile did not become
     * blocked after random seating or other blocking fixtures were placed.
     */
    public static boolean validatePhase3ReservedTraversalStillOpen(
            MapLayerSurfaceAuthority surfaceAuthority,
            int x,
            int y) {
        if (surfaceAuthority == null) {
            phase3Debug("ReservedTraversalInvalid", "surface authority is null");
            return false;
        }

        boolean reserved = surfaceAuthority.isPhase3ReservedTraversalTile(x, y);
        boolean traversable = surfaceAuthority.isPhase3TraversableTile(x, y);
        boolean blocked = surfaceAuthority.isPhase3BlockedTile(x, y);

        boolean valid = reserved && traversable && !blocked;
        if (!valid) {
            phase3Debug("ReservedTraversalInvalid", "x=" + x + ", y=" + y
                    + ", reserved=" + reserved
                    + ", traversable=" + traversable
                    + ", blocked=" + blocked);
        }

        return valid;
    }


    /**
     * Phase 3 random seating / blocking fixture placement wrapper.
     *
     * This wrapper is intended for random seating, benches, chairs, stools, and
     * similar blocking fixtures. It checks reserved traversal before marking the
     * accepted fixture as blocked. Actual visual/object placement should happen
     * only after this returns true.
     */
    public static boolean tryPlacePhase3RandomSeatingOrBlockingFixture(
            MapLayerSurfaceAuthority surfaceAuthority,
            int x,
            int y,
            int width,
            int height) {

        if (!canPlacePhase3RandomSeatingOrBlockingFixture(surfaceAuthority, x, y, width, height)) {
            return false;
        }

        markPhase3RandomSeatingOrBlockingFixturePlaced(surfaceAuthority, x, y, width, height);
        return true;
    }


    public static int minimumPhase3RoomAreaForFeatureTiles(int featureTileCount) {
        int safeFeatureTiles = Math.max(0, featureTileCount);
        return (int) Math.ceil(safeFeatureTiles * 1.5);
    }

    public static boolean hasPhase3MinimumRoomAreaForFeatures(
            int roomWidth,
            int roomHeight,
            int featureTileCount) {
        int roomArea = Math.max(0, roomWidth) * Math.max(0, roomHeight);
        return roomArea >= minimumPhase3RoomAreaForFeatureTiles(featureTileCount);
    }

    public static boolean canPhase3RoomSupportFeatureCount(
            int roomWidth,
            int roomHeight,
            int featureTileCount,
            int requiredAccessTileCount) {
        int roomArea = Math.max(0, roomWidth) * Math.max(0, roomHeight);
        int safeFeatureTiles = Math.max(0, featureTileCount);
        int safeAccessTiles = Math.max(0, requiredAccessTileCount);
        int minimumArea = Math.max(
                minimumPhase3RoomAreaForFeatureTiles(safeFeatureTiles),
                safeFeatureTiles + safeAccessTiles);

        return roomArea >= minimumArea;
    }

    public static boolean validatePhase3RoomFeatureDensity(
            int roomWidth,
            int roomHeight,
            int featureTileCount,
            int requiredAccessTileCount,
            String roomLabel) {
        boolean allowed = canPhase3RoomSupportFeatureCount(
                roomWidth,
                roomHeight,
                featureTileCount,
                requiredAccessTileCount);

        if (!allowed) {
            int roomArea = Math.max(0, roomWidth) * Math.max(0, roomHeight);
            int minimumArea = Math.max(
                    minimumPhase3RoomAreaForFeatureTiles(featureTileCount),
                    Math.max(0, featureTileCount) + Math.max(0, requiredAccessTileCount));
            phase3Debug("RoomFeatureDensityReject",
                    "room=" + roomLabel
                            + ", size=" + roomWidth + "x" + roomHeight
                            + ", area=" + roomArea
                            + ", featureTiles=" + featureTileCount
                            + ", requiredAccessTiles=" + requiredAccessTileCount
                            + ", minimumArea=" + minimumArea);
        }

        return allowed;
    }


    /**
     * Phase 3 room feature-load model.
     *
     * Represents the intended fixture/feature burden for a room before the room is
     * accepted. This lets room profiles/stamps ask whether the room is large enough
     * for both feature footprint and access space.
     */
    public static final class Phase3RoomFeatureLoad {
        public final int featureTileCount;
        public final int requiredAccessTileCount;
        public final int blockingFeatureTileCount;
        public final int interactiveFeatureCount;

        public Phase3RoomFeatureLoad(
                int featureTileCount,
                int requiredAccessTileCount,
                int blockingFeatureTileCount,
                int interactiveFeatureCount) {
            this.featureTileCount = Math.max(0, featureTileCount);
            this.requiredAccessTileCount = Math.max(0, requiredAccessTileCount);
            this.blockingFeatureTileCount = Math.max(0, blockingFeatureTileCount);
            this.interactiveFeatureCount = Math.max(0, interactiveFeatureCount);
        }

        public int totalDensityTileCount() {
            return featureTileCount + blockingFeatureTileCount;
        }

        public int totalAccessTileRequirement() {
            return requiredAccessTileCount + interactiveFeatureCount;
        }

        @Override
        public String toString() {
            return "Phase3RoomFeatureLoad{" +
                    "featureTileCount=" + featureTileCount +
                    ", requiredAccessTileCount=" + requiredAccessTileCount +
                    ", blockingFeatureTileCount=" + blockingFeatureTileCount +
                    ", interactiveFeatureCount=" + interactiveFeatureCount +
                    '}';
        }
    }

    public static Phase3RoomFeatureLoad phase3RoomFeatureLoad(
            int featureTileCount,
            int requiredAccessTileCount,
            int blockingFeatureTileCount,
            int interactiveFeatureCount) {
        return new Phase3RoomFeatureLoad(
                featureTileCount,
                requiredAccessTileCount,
                blockingFeatureTileCount,
                interactiveFeatureCount);
    }

    public static boolean validatePhase3RoomFeatureLoad(
            int roomWidth,
            int roomHeight,
            Phase3RoomFeatureLoad load,
            String roomLabel) {
        if (load == null) {
            return validatePhase3RoomFeatureDensity(roomWidth, roomHeight, 0, 0, roomLabel);
        }

        return validatePhase3RoomFeatureDensity(
                roomWidth,
                roomHeight,
                load.totalDensityTileCount(),
                load.totalAccessTileRequirement(),
                roomLabel + " " + load.toString());
    }

    /**
     * Suggests a minimum square-ish room side for planning/debug output.
     *
     * This is not a final placement rule; rectangular stamps may satisfy the same
     * minimum area through different dimensions.
     */
    public static int suggestPhase3MinimumRoomSideForFeatureLoad(Phase3RoomFeatureLoad load) {
        if (load == null) return 1;
        int minimumArea = Math.max(
                minimumPhase3RoomAreaForFeatureTiles(load.totalDensityTileCount()),
                load.totalDensityTileCount() + load.totalAccessTileRequirement());
        return (int) Math.ceil(Math.sqrt(Math.max(1, minimumArea)));
    }


    /**
     * Phase 3 room profile / stamp feature-load validation.
     *
     * Call this before selecting or committing a room profile/stamp when the planned
     * feature load is known. This is intentionally independent of specific profile
     * classes because RoomProfile/StampedRoomSpec structures may vary.
     */
    public static boolean validatePhase3RoomProfileFeatureLoad(
            int roomWidth,
            int roomHeight,
            Phase3RoomFeatureLoad featureLoad,
            String profileLabel) {
        boolean valid = validatePhase3RoomFeatureLoad(roomWidth, roomHeight, featureLoad, profileLabel);
        if (!valid) {
            phase3Debug("RoomProfileFeatureLoadReject",
                    "profile=" + profileLabel
                            + ", suggestedMinimumSide="
                            + suggestPhase3MinimumRoomSideForFeatureLoad(featureLoad));
        }
        return valid;
    }

    /**
     * Conservative fallback feature-load estimator.
     *
     * Use this when a room profile only knows a count of intended feature objects
     * rather than exact tile footprints. Each feature is treated as at least one
     * feature tile and one access burden unit.
     */
    public static Phase3RoomFeatureLoad estimatePhase3RoomFeatureLoadFromCount(
            int intendedFeatureCount,
            int blockingFeatureCount,
            int interactiveFeatureCount) {
        int safeIntended = Math.max(0, intendedFeatureCount);
        int safeBlocking = Math.max(0, blockingFeatureCount);
        int safeInteractive = Math.max(0, interactiveFeatureCount);
        int estimatedAccess = safeInteractive + Math.max(0, safeIntended / 2);

        return phase3RoomFeatureLoad(
                safeIntended,
                estimatedAccess,
                safeBlocking,
                safeInteractive);
    }

    /**
     * Suggests whether an overburdened room should expand, downgrade features, or reject.
     */
    public static String suggestPhase3RoomFeatureLoadResolution(
            int roomWidth,
            int roomHeight,
            Phase3RoomFeatureLoad featureLoad) {
        if (validatePhase3RoomFeatureLoad(roomWidth, roomHeight, featureLoad, "resolution-check")) {
            return "accept";
        }

        int suggestedSide = suggestPhase3MinimumRoomSideForFeatureLoad(featureLoad);
        int currentArea = Math.max(0, roomWidth) * Math.max(0, roomHeight);
        int suggestedArea = suggestedSide * suggestedSide;

        if (suggestedArea <= currentArea + 16) {
            return "expand-room";
        }

        return "downgrade-or-reduce-features";
    }


    /**
     * Phase 3 room profile/stamp acceptance gate.
     *
     * Use immediately before committing a RoomProfile/StampedRoomSpec selection when
     * width, height, and intended feature load are known. Returns false rather than
     * throwing so generation can expand, downgrade, retry, or reject.
     */
    public static boolean shouldAcceptPhase3RoomProfileOrStamp(
            int roomWidth,
            int roomHeight,
            Phase3RoomFeatureLoad featureLoad,
            String roomLabel) {
        boolean accepted = validatePhase3RoomProfileFeatureLoad(
                roomWidth,
                roomHeight,
                featureLoad,
                roomLabel);

        if (!accepted) {
            phase3Debug("RoomProfileGateReject",
                    "room=" + roomLabel
                            + ", resolution="
                            + suggestPhase3RoomFeatureLoadResolution(roomWidth, roomHeight, featureLoad));
        }

        return accepted;
    }

    /**
     * Fallback overload for selection code that has counts but not exact footprints.
     */
    public static boolean shouldAcceptPhase3RoomProfileOrStamp(
            int roomWidth,
            int roomHeight,
            int intendedFeatureCount,
            int blockingFeatureCount,
            int interactiveFeatureCount,
            String roomLabel) {
        Phase3RoomFeatureLoad estimatedLoad = estimatePhase3RoomFeatureLoadFromCount(
                intendedFeatureCount,
                blockingFeatureCount,
                interactiveFeatureCount);
        return shouldAcceptPhase3RoomProfileOrStamp(roomWidth, roomHeight, estimatedLoad, roomLabel);
    }


    public enum Phase3RoomFeatureResolution {
        ACCEPT,
        EXPAND_ROOM,
        CHOOSE_LARGER_STAMP,
        REDUCE_FEATURES,
        DOWNGRADE_FEATURE_SET,
        RETRY_PLACEMENT,
        REJECT_ROOM
    }

    public static Phase3RoomFeatureResolution resolvePhase3RoomFeatureLoad(
            int roomWidth,
            int roomHeight,
            Phase3RoomFeatureLoad featureLoad,
            boolean roomCanExpand,
            boolean largerStampAvailable,
            boolean featuresOptional) {

        if (validatePhase3RoomFeatureLoad(roomWidth, roomHeight, featureLoad, "feature-resolution")) {
            return Phase3RoomFeatureResolution.ACCEPT;
        }

        int suggestedSide = suggestPhase3MinimumRoomSideForFeatureLoad(featureLoad);
        int currentMaxSide = Math.max(Math.max(0, roomWidth), Math.max(0, roomHeight));

        if (roomCanExpand && suggestedSide <= currentMaxSide + 4) {
            return Phase3RoomFeatureResolution.EXPAND_ROOM;
        }

        if (largerStampAvailable) {
            return Phase3RoomFeatureResolution.CHOOSE_LARGER_STAMP;
        }

        if (featuresOptional) {
            return Phase3RoomFeatureResolution.REDUCE_FEATURES;
        }

        if (roomCanExpand) {
            return Phase3RoomFeatureResolution.RETRY_PLACEMENT;
        }

        return Phase3RoomFeatureResolution.REJECT_ROOM;
    }

    public static boolean shouldAcceptPhase3RoomProfileOrStamp(
            int roomWidth,
            int roomHeight,
            Phase3RoomFeatureLoad featureLoad,
            String roomLabel,
            boolean roomCanExpand,
            boolean largerStampAvailable,
            boolean featuresOptional) {

        Phase3RoomFeatureResolution resolution = resolvePhase3RoomFeatureLoad(
                roomWidth,
                roomHeight,
                featureLoad,
                roomCanExpand,
                largerStampAvailable,
                featuresOptional);

        if (resolution == Phase3RoomFeatureResolution.ACCEPT) {
            return true;
        }

        phase3Debug("RoomProfileGateReject",
                "room=" + roomLabel
                        + ", resolution=" + resolution
                        + ", size=" + roomWidth + "x" + roomHeight
                        + ", load=" + featureLoad);
        return false;
    }


    public static final class Phase3RoomFeatureCommitDecision {
        public final boolean accepted;
        public final Phase3RoomFeatureResolution resolution;
        public final int suggestedMinimumSide;
        public final String roomLabel;

        public Phase3RoomFeatureCommitDecision(
                boolean accepted,
                Phase3RoomFeatureResolution resolution,
                int suggestedMinimumSide,
                String roomLabel) {
            this.accepted = accepted;
            this.resolution = resolution;
            this.suggestedMinimumSide = suggestedMinimumSide;
            this.roomLabel = roomLabel;
        }

        @Override
        public String toString() {
            return "Phase3RoomFeatureCommitDecision{" +
                    "accepted=" + accepted +
                    ", resolution=" + resolution +
                    ", suggestedMinimumSide=" + suggestedMinimumSide +
                    ", roomLabel='" + roomLabel + '\'' +
                    '}';
        }
    }

    /**
     * Phase 3 commit bridge for room profile/stamp selection.
     *
     * This returns a structured decision object instead of only true/false so the
     * actual generator can decide whether to expand, choose a larger stamp, reduce
     * features, retry, or reject.
     */
    public static Phase3RoomFeatureCommitDecision decidePhase3RoomFeatureCommit(
            int roomWidth,
            int roomHeight,
            Phase3RoomFeatureLoad featureLoad,
            String roomLabel,
            boolean roomCanExpand,
            boolean largerStampAvailable,
            boolean featuresOptional) {

        Phase3RoomFeatureResolution resolution = resolvePhase3RoomFeatureLoad(
                roomWidth,
                roomHeight,
                featureLoad,
                roomCanExpand,
                largerStampAvailable,
                featuresOptional);

        boolean accepted = resolution == Phase3RoomFeatureResolution.ACCEPT;
        int suggestedSide = suggestPhase3MinimumRoomSideForFeatureLoad(featureLoad);

        Phase3RoomFeatureCommitDecision decision = new Phase3RoomFeatureCommitDecision(
                accepted,
                resolution,
                suggestedSide,
                roomLabel);

        if (!accepted) {
            phase3Debug("RoomFeatureCommitDecision", decision.toString());
        }

        return decision;
    }

    /**
     * Fallback overload for rooms that know counts but not exact feature footprint.
     */
    public static Phase3RoomFeatureCommitDecision decidePhase3RoomFeatureCommit(
            int roomWidth,
            int roomHeight,
            int intendedFeatureCount,
            int blockingFeatureCount,
            int interactiveFeatureCount,
            String roomLabel,
            boolean roomCanExpand,
            boolean largerStampAvailable,
            boolean featuresOptional) {

        Phase3RoomFeatureLoad estimatedLoad = estimatePhase3RoomFeatureLoadFromCount(
                intendedFeatureCount,
                blockingFeatureCount,
                interactiveFeatureCount);

        return decidePhase3RoomFeatureCommit(
                roomWidth,
                roomHeight,
                estimatedLoad,
                roomLabel,
                roomCanExpand,
                largerStampAvailable,
                featuresOptional);
    }


    public static final class Phase3RoomFeatureActionPlan {
        public final Phase3RoomFeatureResolution resolution;
        public final int targetWidth;
        public final int targetHeight;
        public final int featureReductionTarget;
        public final boolean retryPlacement;
        public final boolean rejectRoom;
        public final String note;

        public Phase3RoomFeatureActionPlan(
                Phase3RoomFeatureResolution resolution,
                int targetWidth,
                int targetHeight,
                int featureReductionTarget,
                boolean retryPlacement,
                boolean rejectRoom,
                String note) {
            this.resolution = resolution;
            this.targetWidth = Math.max(0, targetWidth);
            this.targetHeight = Math.max(0, targetHeight);
            this.featureReductionTarget = Math.max(0, featureReductionTarget);
            this.retryPlacement = retryPlacement;
            this.rejectRoom = rejectRoom;
            this.note = note == null ? "" : note;
        }

        @Override
        public String toString() {
            return "Phase3RoomFeatureActionPlan{" +
                    "resolution=" + resolution +
                    ", targetWidth=" + targetWidth +
                    ", targetHeight=" + targetHeight +
                    ", featureReductionTarget=" + featureReductionTarget +
                    ", retryPlacement=" + retryPlacement +
                    ", rejectRoom=" + rejectRoom +
                    ", note='" + note + '\'' +
                    '}';
        }
    }

    /**
     * Converts a feature-load commit decision into a concrete action plan for the
     * generator. This is still scaffold logic; actual room expansion or stamp
     * substitution should be performed by the confirmed room-generation commit path.
     */
    public static Phase3RoomFeatureActionPlan planPhase3RoomFeatureResolutionAction(
            int currentWidth,
            int currentHeight,
            Phase3RoomFeatureLoad featureLoad,
            Phase3RoomFeatureCommitDecision decision) {

        if (decision == null) {
            return new Phase3RoomFeatureActionPlan(
                    Phase3RoomFeatureResolution.REJECT_ROOM,
                    currentWidth,
                    currentHeight,
                    0,
                    false,
                    true,
                    "missing decision");
        }

        if (decision.accepted || decision.resolution == Phase3RoomFeatureResolution.ACCEPT) {
            return new Phase3RoomFeatureActionPlan(
                    Phase3RoomFeatureResolution.ACCEPT,
                    currentWidth,
                    currentHeight,
                    featureLoad == null ? 0 : featureLoad.totalDensityTileCount(),
                    false,
                    false,
                    "accepted");
        }

        int suggestedSide = Math.max(1, decision.suggestedMinimumSide);
        int targetWidth = Math.max(currentWidth, suggestedSide);
        int targetHeight = Math.max(currentHeight, suggestedSide);
        int currentFeatureTiles = featureLoad == null ? 0 : featureLoad.totalDensityTileCount();

        switch (decision.resolution) {
            case EXPAND_ROOM:
                return new Phase3RoomFeatureActionPlan(
                        decision.resolution,
                        targetWidth,
                        targetHeight,
                        currentFeatureTiles,
                        true,
                        false,
                        "expand room toward suggested minimum size");
            case CHOOSE_LARGER_STAMP:
                return new Phase3RoomFeatureActionPlan(
                        decision.resolution,
                        targetWidth,
                        targetHeight,
                        currentFeatureTiles,
                        true,
                        false,
                        "select larger room stamp/profile");
            case REDUCE_FEATURES:
            case DOWNGRADE_FEATURE_SET:
                int reducedTarget = Math.max(0, (int) Math.floor(currentFeatureTiles * 0.75));
                return new Phase3RoomFeatureActionPlan(
                        decision.resolution,
                        currentWidth,
                        currentHeight,
                        reducedTarget,
                        true,
                        false,
                        "reduce or downgrade optional feature load");
            case RETRY_PLACEMENT:
                return new Phase3RoomFeatureActionPlan(
                        decision.resolution,
                        targetWidth,
                        targetHeight,
                        currentFeatureTiles,
                        true,
                        false,
                        "retry placement with adjusted bounds");
            case REJECT_ROOM:
            default:
                return new Phase3RoomFeatureActionPlan(
                        Phase3RoomFeatureResolution.REJECT_ROOM,
                        currentWidth,
                        currentHeight,
                        0,
                        false,
                        true,
                        "reject room/profile");
        }
    }

    public static Phase3RoomFeatureActionPlan decideAndPlanPhase3RoomFeatureAction(
            int roomWidth,
            int roomHeight,
            Phase3RoomFeatureLoad featureLoad,
            String roomLabel,
            boolean roomCanExpand,
            boolean largerStampAvailable,
            boolean featuresOptional) {

        Phase3RoomFeatureCommitDecision decision = decidePhase3RoomFeatureCommit(
                roomWidth,
                roomHeight,
                featureLoad,
                roomLabel,
                roomCanExpand,
                largerStampAvailable,
                featuresOptional);

        Phase3RoomFeatureActionPlan actionPlan = planPhase3RoomFeatureResolutionAction(
                roomWidth,
                roomHeight,
                featureLoad,
                decision);

        if (!decision.accepted) {
            phase3Debug("RoomFeatureActionPlan", actionPlan.toString());
        }

        return actionPlan;
    }


    public enum Phase3RoomFeatureActionResult {
        ACCEPTED,
        EXPANSION_REQUIRED,
        LARGER_STAMP_REQUIRED,
        FEATURES_REDUCED_REQUIRED,
        FEATURE_DOWNGRADE_REQUIRED,
        RETRY_REQUIRED,
        REJECTED
    }

    /**
     * Phase 3 feature-action handler scaffold.
     *
     * This method intentionally does not mutate room geometry yet. It converts the
     * action plan into a clear result for the confirmed room-generation commit path.
     */
    public static Phase3RoomFeatureActionResult applyPhase3RoomFeatureActionPlan(
            Phase3RoomFeatureActionPlan actionPlan,
            String roomLabel) {

        if (actionPlan == null) {
            phase3Debug("RoomFeatureActionApply", "room=" + roomLabel + ", missing action plan; rejected");
            return Phase3RoomFeatureActionResult.REJECTED;
        }

        switch (actionPlan.resolution) {
            case ACCEPT:
                return Phase3RoomFeatureActionResult.ACCEPTED;
            case EXPAND_ROOM:
                phase3Debug("RoomFeatureActionApply", "room=" + roomLabel
                        + ", expansion required target="
                        + actionPlan.targetWidth + "x" + actionPlan.targetHeight);
                return Phase3RoomFeatureActionResult.EXPANSION_REQUIRED;
            case CHOOSE_LARGER_STAMP:
                phase3Debug("RoomFeatureActionApply", "room=" + roomLabel
                        + ", larger stamp required target="
                        + actionPlan.targetWidth + "x" + actionPlan.targetHeight);
                return Phase3RoomFeatureActionResult.LARGER_STAMP_REQUIRED;
            case REDUCE_FEATURES:
                phase3Debug("RoomFeatureActionApply", "room=" + roomLabel
                        + ", feature reduction required targetFeatureTiles="
                        + actionPlan.featureReductionTarget);
                return Phase3RoomFeatureActionResult.FEATURES_REDUCED_REQUIRED;
            case DOWNGRADE_FEATURE_SET:
                phase3Debug("RoomFeatureActionApply", "room=" + roomLabel
                        + ", feature downgrade required targetFeatureTiles="
                        + actionPlan.featureReductionTarget);
                return Phase3RoomFeatureActionResult.FEATURE_DOWNGRADE_REQUIRED;
            case RETRY_PLACEMENT:
                phase3Debug("RoomFeatureActionApply", "room=" + roomLabel
                        + ", retry required target="
                        + actionPlan.targetWidth + "x" + actionPlan.targetHeight);
                return Phase3RoomFeatureActionResult.RETRY_REQUIRED;
            case REJECT_ROOM:
            default:
                phase3Debug("RoomFeatureActionApply", "room=" + roomLabel + ", rejected");
                return Phase3RoomFeatureActionResult.REJECTED;
        }
    }

    /**
     * Convenience method for the eventual room profile/stamp commit point.
     */
    public static Phase3RoomFeatureActionResult decidePlanAndApplyPhase3RoomFeatureAction(
            int roomWidth,
            int roomHeight,
            Phase3RoomFeatureLoad featureLoad,
            String roomLabel,
            boolean roomCanExpand,
            boolean largerStampAvailable,
            boolean featuresOptional) {

        Phase3RoomFeatureActionPlan actionPlan = decideAndPlanPhase3RoomFeatureAction(
                roomWidth,
                roomHeight,
                featureLoad,
                roomLabel,
                roomCanExpand,
                largerStampAvailable,
                featuresOptional);

        return applyPhase3RoomFeatureActionPlan(actionPlan, roomLabel);
    }


    public static final class Phase3RoomGenerationDirective {
        public final boolean mayCommit;
        public final boolean shouldExpand;
        public final boolean shouldChooseLargerStamp;
        public final boolean shouldReduceFeatures;
        public final boolean shouldRetry;
        public final boolean shouldReject;
        public final int targetWidth;
        public final int targetHeight;
        public final int targetFeatureTiles;
        public final String reason;

        public Phase3RoomGenerationDirective(
                boolean mayCommit,
                boolean shouldExpand,
                boolean shouldChooseLargerStamp,
                boolean shouldReduceFeatures,
                boolean shouldRetry,
                boolean shouldReject,
                int targetWidth,
                int targetHeight,
                int targetFeatureTiles,
                String reason) {
            this.mayCommit = mayCommit;
            this.shouldExpand = shouldExpand;
            this.shouldChooseLargerStamp = shouldChooseLargerStamp;
            this.shouldReduceFeatures = shouldReduceFeatures;
            this.shouldRetry = shouldRetry;
            this.shouldReject = shouldReject;
            this.targetWidth = Math.max(0, targetWidth);
            this.targetHeight = Math.max(0, targetHeight);
            this.targetFeatureTiles = Math.max(0, targetFeatureTiles);
            this.reason = reason == null ? "" : reason;
        }

        @Override
        public String toString() {
            return "Phase3RoomGenerationDirective{" +
                    "mayCommit=" + mayCommit +
                    ", shouldExpand=" + shouldExpand +
                    ", shouldChooseLargerStamp=" + shouldChooseLargerStamp +
                    ", shouldReduceFeatures=" + shouldReduceFeatures +
                    ", shouldRetry=" + shouldRetry +
                    ", shouldReject=" + shouldReject +
                    ", targetWidth=" + targetWidth +
                    ", targetHeight=" + targetHeight +
                    ", targetFeatureTiles=" + targetFeatureTiles +
                    ", reason='" + reason + '\'' +
                    '}';
        }
    }

    public static Phase3RoomGenerationDirective convertPhase3FeatureActionToGenerationDirective(
            Phase3RoomFeatureActionPlan actionPlan,
            Phase3RoomFeatureActionResult actionResult,
            int currentWidth,
            int currentHeight) {

        if (actionPlan == null || actionResult == null) {
            return new Phase3RoomGenerationDirective(
                    false, false, false, false, false, true,
                    currentWidth, currentHeight, 0,
                    "missing action plan/result");
        }

        switch (actionResult) {
            case ACCEPTED:
                return new Phase3RoomGenerationDirective(
                        true, false, false, false, false, false,
                        currentWidth, currentHeight, actionPlan.featureReductionTarget,
                        "accepted");
            case EXPANSION_REQUIRED:
                return new Phase3RoomGenerationDirective(
                        false, true, false, false, true, false,
                        actionPlan.targetWidth, actionPlan.targetHeight, actionPlan.featureReductionTarget,
                        "expand room and retry");
            case LARGER_STAMP_REQUIRED:
                return new Phase3RoomGenerationDirective(
                        false, false, true, false, true, false,
                        actionPlan.targetWidth, actionPlan.targetHeight, actionPlan.featureReductionTarget,
                        "choose larger stamp and retry");
            case FEATURES_REDUCED_REQUIRED:
            case FEATURE_DOWNGRADE_REQUIRED:
                return new Phase3RoomGenerationDirective(
                        false, false, false, true, true, false,
                        currentWidth, currentHeight, actionPlan.featureReductionTarget,
                        "reduce/downgrade features and retry");
            case RETRY_REQUIRED:
                return new Phase3RoomGenerationDirective(
                        false, actionPlan.targetWidth > currentWidth || actionPlan.targetHeight > currentHeight,
                        false, false, true, false,
                        actionPlan.targetWidth, actionPlan.targetHeight, actionPlan.featureReductionTarget,
                        "retry placement");
            case REJECTED:
            default:
                return new Phase3RoomGenerationDirective(
                        false, false, false, false, false, true,
                        currentWidth, currentHeight, 0,
                        "reject room");
        }
    }

    public static Phase3RoomGenerationDirective decidePhase3RoomGenerationDirective(
            int roomWidth,
            int roomHeight,
            Phase3RoomFeatureLoad featureLoad,
            String roomLabel,
            boolean roomCanExpand,
            boolean largerStampAvailable,
            boolean featuresOptional) {

        Phase3RoomFeatureActionPlan actionPlan = decideAndPlanPhase3RoomFeatureAction(
                roomWidth,
                roomHeight,
                featureLoad,
                roomLabel,
                roomCanExpand,
                largerStampAvailable,
                featuresOptional);

        Phase3RoomFeatureActionResult result = applyPhase3RoomFeatureActionPlan(actionPlan, roomLabel);

        Phase3RoomGenerationDirective directive = convertPhase3FeatureActionToGenerationDirective(
                actionPlan,
                result,
                roomWidth,
                roomHeight);

        if (!directive.mayCommit) {
            phase3Debug("RoomGenerationDirective", directive.toString());
        }

        return directive;
    }


    /**
     * Phase 3 final commit guard for room feature-density decisions.
     *
     * The actual room generator should call this immediately before committing a
     * room profile/stamp. If this returns false, the commit path must apply the
     * directive action or reject/retry instead.
     */
    public static boolean canCommitPhase3RoomAfterDirective(
            Phase3RoomGenerationDirective directive,
            String roomLabel) {

        if (directive == null) {
            phase3Debug("RoomCommitGuardReject", "room=" + roomLabel + ", directive=null");
            return false;
        }

        if (!directive.mayCommit) {
            phase3Debug("RoomCommitGuardReject", "room=" + roomLabel + ", directive=" + directive);
            return false;
        }

        return true;
    }

    /**
     * Convenience method for call sites that need a single guard call from raw
     * room size and feature-load data.
     */
    public static boolean canCommitPhase3RoomAfterFeatureValidation(
            int roomWidth,
            int roomHeight,
            Phase3RoomFeatureLoad featureLoad,
            String roomLabel,
            boolean roomCanExpand,
            boolean largerStampAvailable,
            boolean featuresOptional) {

        Phase3RoomGenerationDirective directive = decidePhase3RoomGenerationDirective(
                roomWidth,
                roomHeight,
                featureLoad,
                roomLabel,
                roomCanExpand,
                largerStampAvailable,
                featuresOptional);

        return canCommitPhase3RoomAfterDirective(directive, roomLabel);
    }


    public static final class Phase3RoomCorrectiveActionDispatch {
        public final Phase3RoomFeatureActionResult result;
        public final boolean shouldAttemptCorrection;
        public final boolean shouldAbortCommit;
        public final boolean mustRevalidate;
        public final String correctionType;
        public final String note;

        public Phase3RoomCorrectiveActionDispatch(
                Phase3RoomFeatureActionResult result,
                boolean shouldAttemptCorrection,
                boolean shouldAbortCommit,
                boolean mustRevalidate,
                String correctionType,
                String note) {
            this.result = result;
            this.shouldAttemptCorrection = shouldAttemptCorrection;
            this.shouldAbortCommit = shouldAbortCommit;
            this.mustRevalidate = mustRevalidate;
            this.correctionType = correctionType == null ? "" : correctionType;
            this.note = note == null ? "" : note;
        }

        @Override
        public String toString() {
            return "Phase3RoomCorrectiveActionDispatch{" +
                    "result=" + result +
                    ", shouldAttemptCorrection=" + shouldAttemptCorrection +
                    ", shouldAbortCommit=" + shouldAbortCommit +
                    ", mustRevalidate=" + mustRevalidate +
                    ", correctionType='" + correctionType + '\'' +
                    ", note='" + note + '\'' +
                    '}';
        }
    }

    /**
     * Converts a failed room commit action result into a controlled corrective-action dispatch.
     *
     * This does not mutate generation directly. The confirmed room commit path should consume
     * this dispatch and then re-run validation before any commit is allowed.
     */
    public static Phase3RoomCorrectiveActionDispatch dispatchPhase3RoomCorrectiveAction(
            Phase3RoomFeatureActionResult result,
            Phase3RoomGenerationDirective directive,
            String roomLabel) {

        if (result == null || directive == null) {
            return new Phase3RoomCorrectiveActionDispatch(
                    Phase3RoomFeatureActionResult.REJECTED,
                    false,
                    true,
                    false,
                    "reject",
                    "missing result or directive");
        }

        switch (result) {
            case ACCEPTED:
                return new Phase3RoomCorrectiveActionDispatch(
                        result,
                        false,
                        false,
                        false,
                        "none",
                        "room accepted");
            case EXPANSION_REQUIRED:
                return new Phase3RoomCorrectiveActionDispatch(
                        result,
                        true,
                        true,
                        true,
                        "expand-room",
                        "expand to " + directive.targetWidth + "x" + directive.targetHeight);
            case LARGER_STAMP_REQUIRED:
                return new Phase3RoomCorrectiveActionDispatch(
                        result,
                        true,
                        true,
                        true,
                        "choose-larger-stamp",
                        "select stamp near " + directive.targetWidth + "x" + directive.targetHeight);
            case FEATURES_REDUCED_REQUIRED:
            case FEATURE_DOWNGRADE_REQUIRED:
                return new Phase3RoomCorrectiveActionDispatch(
                        result,
                        true,
                        true,
                        true,
                        "reduce-or-downgrade-features",
                        "target feature tiles " + directive.targetFeatureTiles);
            case RETRY_REQUIRED:
                return new Phase3RoomCorrectiveActionDispatch(
                        result,
                        true,
                        true,
                        true,
                        "retry-placement",
                        "retry with current or adjusted bounds");
            case REJECTED:
            default:
                return new Phase3RoomCorrectiveActionDispatch(
                        Phase3RoomFeatureActionResult.REJECTED,
                        false,
                        true,
                        false,
                        "reject",
                        "room rejected");
        }
    }

    public static Phase3RoomCorrectiveActionDispatch dispatchPhase3RoomCorrectiveActionFromDirective(
            Phase3RoomGenerationDirective directive,
            String roomLabel) {

        if (directive == null) {
            Phase3RoomCorrectiveActionDispatch dispatch = new Phase3RoomCorrectiveActionDispatch(
                    Phase3RoomFeatureActionResult.REJECTED,
                    false,
                    true,
                    false,
                    "reject",
                    "directive was null");
            phase3Debug("RoomCorrectiveActionDispatch", "room=" + roomLabel + ", dispatch=" + dispatch);
            return dispatch;
        }

        Phase3RoomFeatureActionResult result;
        if (directive.mayCommit) {
            result = Phase3RoomFeatureActionResult.ACCEPTED;
        } else if (directive.shouldExpand) {
            result = Phase3RoomFeatureActionResult.EXPANSION_REQUIRED;
        } else if (directive.shouldChooseLargerStamp) {
            result = Phase3RoomFeatureActionResult.LARGER_STAMP_REQUIRED;
        } else if (directive.shouldReduceFeatures) {
            result = Phase3RoomFeatureActionResult.FEATURES_REDUCED_REQUIRED;
        } else if (directive.shouldRetry) {
            result = Phase3RoomFeatureActionResult.RETRY_REQUIRED;
        } else {
            result = Phase3RoomFeatureActionResult.REJECTED;
        }

        Phase3RoomCorrectiveActionDispatch dispatch = dispatchPhase3RoomCorrectiveAction(
                result,
                directive,
                roomLabel);

        if (dispatch.shouldAbortCommit) {
            phase3Debug("RoomCorrectiveActionDispatch", "room=" + roomLabel + ", dispatch=" + dispatch);
        }

        return dispatch;
    }


    public enum Phase3RoomAttachmentFallback {
        RETRY_ORIGINAL,
        INSERT_SPACER_CORRIDOR,
        TRY_SMALLER_ROOM_TYPE,
        PLACE_CORRIDOR_STUB_CLOSET,
        DENY_INVALID_LOCATION
    }

    public static final class Phase3RoomAttachmentFallbackPlan {
        public final Phase3RoomAttachmentFallback fallback;
        public final int spacerCorridorLength;
        public final boolean requiresRevalidation;
        public final String note;

        public Phase3RoomAttachmentFallbackPlan(
                Phase3RoomAttachmentFallback fallback,
                int spacerCorridorLength,
                boolean requiresRevalidation,
                String note) {
            this.fallback = fallback;
            this.spacerCorridorLength = Math.max(0, spacerCorridorLength);
            this.requiresRevalidation = requiresRevalidation;
            this.note = note == null ? "" : note;
        }

        @Override
        public String toString() {
            return "Phase3RoomAttachmentFallbackPlan{" +
                    "fallback=" + fallback +
                    ", spacerCorridorLength=" + spacerCorridorLength +
                    ", requiresRevalidation=" + requiresRevalidation +
                    ", note='" + note + '\'' +
                    '}';
        }
    }

    /**
     * Phase 3 failed room attachment fallback planner.
     *
     * When repeated attempts to attach a room fail, the generator should not jump
     * straight to denial. It should first try to push the target room away from the
     * blockage by inserting a short spacer corridor, then try smaller/downgraded
     * room options, then fall back to a corridor-stub closet when appropriate.
     */
    public static Phase3RoomAttachmentFallbackPlan planPhase3RoomAttachmentFallback(
            int failedAttemptCount,
            int spacerAttemptCount,
            boolean smallerRoomTypeAvailable,
            boolean corridorStubClosetAllowed) {

        int safeFailures = Math.max(0, failedAttemptCount);
        int safeSpacerAttempts = Math.max(0, spacerAttemptCount);

        if (safeFailures < 3) {
            return new Phase3RoomAttachmentFallbackPlan(
                    Phase3RoomAttachmentFallback.RETRY_ORIGINAL,
                    0,
                    true,
                    "retry original attachment before fallback");
        }

        if (safeSpacerAttempts < 3) {
            int spacerLength = 2 + safeSpacerAttempts;
            return new Phase3RoomAttachmentFallbackPlan(
                    Phase3RoomAttachmentFallback.INSERT_SPACER_CORRIDOR,
                    spacerLength,
                    true,
                    "insert short spacer corridor to clear blockage");
        }

        if (smallerRoomTypeAvailable) {
            return new Phase3RoomAttachmentFallbackPlan(
                    Phase3RoomAttachmentFallback.TRY_SMALLER_ROOM_TYPE,
                    0,
                    true,
                    "try a smaller footprint room type");
        }

        if (corridorStubClosetAllowed) {
            return new Phase3RoomAttachmentFallbackPlan(
                    Phase3RoomAttachmentFallback.PLACE_CORRIDOR_STUB_CLOSET,
                    1,
                    true,
                    "fallback to corridor stub closet");
        }

        return new Phase3RoomAttachmentFallbackPlan(
                Phase3RoomAttachmentFallback.DENY_INVALID_LOCATION,
                0,
                false,
                "deny invalid location after fallback chain exhausted");
    }

    /**
     * Valid spacer corridors are intentionally short. They exist to push a room
     * two to four tiles away from a blocked attachment point, not to generate a
     * new long corridor branch.
     */
    public static boolean isPhase3ValidSpacerCorridorLength(int spacerLength) {
        return spacerLength >= 2 && spacerLength <= 4;
    }


    public static final class Phase3SpacerCorridorValidation {
        public final boolean valid;
        public final String reason;
        public final int startX;
        public final int startY;
        public final int endX;
        public final int endY;
        public final int length;

        public Phase3SpacerCorridorValidation(
                boolean valid,
                String reason,
                int startX,
                int startY,
                int endX,
                int endY,
                int length) {
            this.valid = valid;
            this.reason = reason == null ? "" : reason;
            this.startX = startX;
            this.startY = startY;
            this.endX = endX;
            this.endY = endY;
            this.length = Math.max(0, length);
        }

        @Override
        public String toString() {
            return "Phase3SpacerCorridorValidation{" +
                    "valid=" + valid +
                    ", reason='" + reason + '\'' +
                    ", startX=" + startX +
                    ", startY=" + startY +
                    ", endX=" + endX +
                    ", endY=" + endY +
                    ", length=" + length +
                    '}';
        }
    }

    /**
     * Validates a proposed straight spacer corridor used after room attachment failure.
     *
     * Direction must be orthogonal and one tile thick. The length must remain 2–4 tiles.
     * The candidate must stay inside the safe interior bounds and avoid reserved traversal
     * conflicts before any future placement code commits it.
     */
    public static Phase3SpacerCorridorValidation validatePhase3SpacerCorridorCandidate(
            ZoneGenerationContext context,
            MapLayerSurfaceAuthority surfaceAuthority,
            int startX,
            int startY,
            int dx,
            int dy,
            int length) {

        if (!isPhase3ValidSpacerCorridorLength(length)) {
            return new Phase3SpacerCorridorValidation(false, "invalid spacer corridor length",
                    startX, startY, startX, startY, length);
        }

        boolean orthogonal = (Math.abs(dx) == 1 && dy == 0) || (Math.abs(dy) == 1 && dx == 0);
        if (!orthogonal) {
            return new Phase3SpacerCorridorValidation(false, "spacer corridor direction must be orthogonal",
                    startX, startY, startX, startY, length);
        }

        int endX = startX + dx * (length - 1);
        int endY = startY + dy * (length - 1);
        int rectX = Math.min(startX, endX);
        int rectY = Math.min(startY, endY);
        int rectW = dx == 0 ? 1 : length;
        int rectH = dy == 0 ? 1 : length;

        if (!isOrdinaryPlacementInsidePhase3InteriorBounds(context, rectX, rectY, rectW, rectH)) {
            return new Phase3SpacerCorridorValidation(false, "spacer corridor violates safe interior bounds",
                    startX, startY, endX, endY, length);
        }

        if (surfaceAuthority != null && surfaceAuthority.hasPhase3TileState()) {
            for (int i = 0; i < length; i++) {
                int x = startX + dx * i;
                int y = startY + dy * i;
                if (!surfaceAuthority.isPhase3InBounds(x, y)) {
                    return new Phase3SpacerCorridorValidation(false, "spacer corridor tile out of bounds",
                            startX, startY, endX, endY, length);
                }
                if (surfaceAuthority.isPhase3ReservedTraversalTile(x, y)) {
                    return new Phase3SpacerCorridorValidation(false, "spacer corridor collides with reserved traversal",
                            startX, startY, endX, endY, length);
                }
                if (surfaceAuthority.isPhase3BlockedTile(x, y)) {
                    return new Phase3SpacerCorridorValidation(false, "spacer corridor collides with blocked tile",
                            startX, startY, endX, endY, length);
                }
            }
        }

        return new Phase3SpacerCorridorValidation(true, "valid spacer corridor",
                startX, startY, endX, endY, length);
    }

    /**
     * Marks a validated spacer corridor as traversable floor. Call only after
     * validatePhase3SpacerCorridorCandidate(...).valid is true.
     */
    public static void markPhase3SpacerCorridorFloor(
            MapLayerSurfaceAuthority surfaceAuthority,
            Phase3SpacerCorridorValidation validation,
            int dx,
            int dy) {
        if (surfaceAuthority == null || validation == null || !validation.valid) {
            phase3Debug("SpacerCorridorMarkReject", "missing surface or invalid validation");
            return;
        }

        for (int i = 0; i < validation.length; i++) {
            int x = validation.startX + dx * i;
            int y = validation.startY + dy * i;
            surfaceAuthority.markPhase3FloorTile(x, y);
        }
    }


    public static final class Phase3SpacerCorridorRetryPlan {
        public final boolean found;
        public final Phase3SpacerCorridorValidation validation;
        public final int dx;
        public final int dy;
        public final int retryRoomAnchorX;
        public final int retryRoomAnchorY;
        public final String note;

        public Phase3SpacerCorridorRetryPlan(
                boolean found,
                Phase3SpacerCorridorValidation validation,
                int dx,
                int dy,
                int retryRoomAnchorX,
                int retryRoomAnchorY,
                String note) {
            this.found = found;
            this.validation = validation;
            this.dx = dx;
            this.dy = dy;
            this.retryRoomAnchorX = retryRoomAnchorX;
            this.retryRoomAnchorY = retryRoomAnchorY;
            this.note = note == null ? "" : note;
        }

        @Override
        public String toString() {
            return "Phase3SpacerCorridorRetryPlan{" +
                    "found=" + found +
                    ", validation=" + validation +
                    ", dx=" + dx +
                    ", dy=" + dy +
                    ", retryRoomAnchorX=" + retryRoomAnchorX +
                    ", retryRoomAnchorY=" + retryRoomAnchorY +
                    ", note='" + note + '\'' +
                    '}';
        }
    }

    /**
     * Tries to find a valid spacer corridor from a failed attachment point.
     *
     * Candidate order uses the requested primary direction first, then its opposite,
     * then the perpendicular directions. Lengths are 2, 3, then 4 tiles.
     */
    public static Phase3SpacerCorridorRetryPlan planPhase3SpacerCorridorRetry(
            ZoneGenerationContext context,
            MapLayerSurfaceAuthority surfaceAuthority,
            int attachmentX,
            int attachmentY,
            int preferredDx,
            int preferredDy) {

        int[][] directions = buildPhase3SpacerDirectionOrder(preferredDx, preferredDy);
        int[] lengths = new int[] {2, 3, 4};

        for (int[] direction : directions) {
            int dx = direction[0];
            int dy = direction[1];

            for (int length : lengths) {
                Phase3SpacerCorridorValidation validation = validatePhase3SpacerCorridorCandidate(
                        context,
                        surfaceAuthority,
                        attachmentX,
                        attachmentY,
                        dx,
                        dy,
                        length);

                if (validation.valid) {
                    int retryX = validation.endX + dx;
                    int retryY = validation.endY + dy;
                    Phase3SpacerCorridorRetryPlan plan = new Phase3SpacerCorridorRetryPlan(
                            true,
                            validation,
                            dx,
                            dy,
                            retryX,
                            retryY,
                            "valid spacer corridor retry plan");

                    phase3Debug("SpacerCorridorRetryPlan", plan.toString());
                    return plan;
                }
            }
        }

        Phase3SpacerCorridorRetryPlan failed = new Phase3SpacerCorridorRetryPlan(
                false,
                null,
                0,
                0,
                attachmentX,
                attachmentY,
                "no valid spacer corridor candidate found");
        phase3Debug("SpacerCorridorRetryPlan", failed.toString());
        return failed;
    }

    private static int[][] buildPhase3SpacerDirectionOrder(int preferredDx, int preferredDy) {
        int dx = Integer.compare(preferredDx, 0);
        int dy = Integer.compare(preferredDy, 0);

        if (Math.abs(dx) == 1 && dy == 0) {
            return new int[][] {
                    {dx, 0},
                    {-dx, 0},
                    {0, 1},
                    {0, -1}
            };
        }

        if (Math.abs(dy) == 1 && dx == 0) {
            return new int[][] {
                    {0, dy},
                    {0, -dy},
                    {1, 0},
                    {-1, 0}
            };
        }

        return new int[][] {
                {1, 0},
                {-1, 0},
                {0, 1},
                {0, -1}
        };
    }

    /**
     * Applies the spacer corridor retry plan by marking its corridor floor.
     * The actual room retry should then use retryRoomAnchorX/Y.
     */
    public static boolean applyPhase3SpacerCorridorRetryPlan(
            MapLayerSurfaceAuthority surfaceAuthority,
            Phase3SpacerCorridorRetryPlan plan) {
        if (plan == null || !plan.found || plan.validation == null || !plan.validation.valid) {
            phase3Debug("SpacerCorridorApplyReject", "invalid spacer retry plan");
            return false;
        }

        markPhase3SpacerCorridorFloor(surfaceAuthority, plan.validation, plan.dx, plan.dy);
        return true;
    }


    public enum Phase3CentralBiasedFillMode {
        DISABLED,
        CONSTRAINT_WEIGHTED_FILL,
        WFC_LOCAL_PATCH_FILL,
        WFC_ROOM_CLUSTER_FILL,
        RESERVED_FOR_FULL_WFC_EXPERIMENT
    }

    public static final class Phase3CentralBiasedFillSettings {
        public final Phase3CentralBiasedFillMode mode;
        public final long seed;
        public final int centerX;
        public final int centerY;
        public final int targetDensityPercent;
        public final int productiveRadius;
        public final int maxPatchAttempts;
        public final boolean preserveRoads;
        public final boolean preservePlaza;
        public final boolean preserveReservedTraversal;

        public Phase3CentralBiasedFillSettings(
                Phase3CentralBiasedFillMode mode,
                long seed,
                int centerX,
                int centerY,
                int targetDensityPercent,
                int productiveRadius,
                int maxPatchAttempts,
                boolean preserveRoads,
                boolean preservePlaza,
                boolean preserveReservedTraversal) {
            this.mode = mode == null ? Phase3CentralBiasedFillMode.DISABLED : mode;
            this.seed = seed;
            this.centerX = centerX;
            this.centerY = centerY;
            this.targetDensityPercent = Math.max(0, Math.min(100, targetDensityPercent));
            this.productiveRadius = Math.max(0, productiveRadius);
            this.maxPatchAttempts = Math.max(0, maxPatchAttempts);
            this.preserveRoads = preserveRoads;
            this.preservePlaza = preservePlaza;
            this.preserveReservedTraversal = preserveReservedTraversal;
        }

        @Override
        public String toString() {
            return "Phase3CentralBiasedFillSettings{" +
                    "mode=" + mode +
                    ", seed=" + seed +
                    ", centerX=" + centerX +
                    ", centerY=" + centerY +
                    ", targetDensityPercent=" + targetDensityPercent +
                    ", productiveRadius=" + productiveRadius +
                    ", maxPatchAttempts=" + maxPatchAttempts +
                    ", preserveRoads=" + preserveRoads +
                    ", preservePlaza=" + preservePlaza +
                    ", preserveReservedTraversal=" + preserveReservedTraversal +
                    '}';
        }
    }

    /**
     * Phase 3 central-biased fill scaffold.
     *
     * This is not a full WFC replacement for zone generation. It is a constrained
     * post-plaza/post-road/post-corridor helper layer intended to fill productive
     * space near the central plaza while respecting already-placed anchors, roads,
     * corridors, edge bands, reserved traversal, and density targets.
     */
    public static Phase3CentralBiasedFillSettings createPhase3CentralBiasedFillSettings(
            long zoneSeed,
            int centerX,
            int centerY,
            int targetDensityPercent,
            int productiveRadius) {
        long derivedSeed = derivePhase3DeterministicSeed(zoneSeed, "central-biased-fill", centerX, centerY);
        return new Phase3CentralBiasedFillSettings(
                Phase3CentralBiasedFillMode.CONSTRAINT_WEIGHTED_FILL,
                derivedSeed,
                centerX,
                centerY,
                targetDensityPercent,
                productiveRadius,
                256,
                true,
                true,
                true);
    }

    public static long derivePhase3DeterministicSeed(
            long baseSeed,
            String salt,
            int x,
            int y) {
        long hash = baseSeed;
        String safeSalt = salt == null ? "" : salt;
        for (int i = 0; i < safeSalt.length(); i++) {
            hash = hash * 31L + safeSalt.charAt(i);
        }
        hash = hash * 31L + x;
        hash = hash * 31L + y;
        return hash;
    }

    public static int phase3CentralBiasScore(
            int x,
            int y,
            Phase3CentralBiasedFillSettings settings) {
        if (settings == null || settings.productiveRadius <= 0) {
            return 0;
        }

        int dx = x - settings.centerX;
        int dy = y - settings.centerY;
        int distanceSquared = dx * dx + dy * dy;
        int radiusSquared = settings.productiveRadius * settings.productiveRadius;

        if (distanceSquared >= radiusSquared) {
            return 0;
        }

        return Math.max(0, radiusSquared - distanceSquared);
    }

    /**
     * Returns true if a cell may be considered by the central-biased fill layer.
     * Actual room/corridor placement still requires existing validation gates.
     */
    public static boolean mayPhase3CentralBiasedFillConsiderCell(
            ZoneGenerationContext context,
            MapLayerSurfaceAuthority surfaceAuthority,
            Phase3CentralBiasedFillSettings settings,
            int x,
            int y) {
        if (context == null || settings == null) {
            return false;
        }

        if (!context.allowsOrdinaryPlacement(ZoneGenerationContext.rect(x, y, 1, 1))) {
            return false;
        }

        if (surfaceAuthority != null && surfaceAuthority.hasPhase3TileState()) {
            if (!surfaceAuthority.isPhase3InBounds(x, y)) {
                return false;
            }
            if (surfaceAuthority.isPhase3ReservedTraversalTile(x, y)) {
                return false;
            }
            if (surfaceAuthority.isPhase3BlockedTile(x, y)) {
                return false;
            }
        }

        return phase3CentralBiasScore(x, y, settings) > 0;
    }


    public static final class Phase3GridSuppressionSettings {
        public final int maxParallelRunLength;
        public final int minimumParallelSpacing;
        public final int maxAdjacentParallelBands;
        public final int repetitionPenaltyWeight;
        public final boolean suppressInfiniteRoadGrids;
        public final boolean suppressInfiniteCorridorGrids;

        public Phase3GridSuppressionSettings(
                int maxParallelRunLength,
                int minimumParallelSpacing,
                int maxAdjacentParallelBands,
                int repetitionPenaltyWeight,
                boolean suppressInfiniteRoadGrids,
                boolean suppressInfiniteCorridorGrids) {
            this.maxParallelRunLength = Math.max(1, maxParallelRunLength);
            this.minimumParallelSpacing = Math.max(1, minimumParallelSpacing);
            this.maxAdjacentParallelBands = Math.max(1, maxAdjacentParallelBands);
            this.repetitionPenaltyWeight = Math.max(0, repetitionPenaltyWeight);
            this.suppressInfiniteRoadGrids = suppressInfiniteRoadGrids;
            this.suppressInfiniteCorridorGrids = suppressInfiniteCorridorGrids;
        }

        @Override
        public String toString() {
            return "Phase3GridSuppressionSettings{" +
                    "maxParallelRunLength=" + maxParallelRunLength +
                    ", minimumParallelSpacing=" + minimumParallelSpacing +
                    ", maxAdjacentParallelBands=" + maxAdjacentParallelBands +
                    ", repetitionPenaltyWeight=" + repetitionPenaltyWeight +
                    ", suppressInfiniteRoadGrids=" + suppressInfiniteRoadGrids +
                    ", suppressInfiniteCorridorGrids=" + suppressInfiniteCorridorGrids +
                    '}';
        }
    }

    public static Phase3GridSuppressionSettings createDefaultPhase3GridSuppressionSettings() {
        return new Phase3GridSuppressionSettings(
                12,
                4,
                2,
                25,
                true,
                true);
    }

    /**
     * Scores a proposed road/corridor candidate for grid repetition risk.
     *
     * This scaffold does not yet inspect a full graph. It provides a deterministic
     * penalty model that later corridor/road candidate selection can consume.
     */
    public static int phase3GridRepetitionPenalty(
            boolean candidateIsRoad,
            boolean candidateIsCorridor,
            int parallelRunLength,
            int nearestParallelSpacing,
            int adjacentParallelBandCount,
            Phase3GridSuppressionSettings settings) {

        if (settings == null) {
            settings = createDefaultPhase3GridSuppressionSettings();
        }

        if (candidateIsRoad && !settings.suppressInfiniteRoadGrids) {
            return 0;
        }

        if (candidateIsCorridor && !settings.suppressInfiniteCorridorGrids) {
            return 0;
        }

        int penalty = 0;

        if (parallelRunLength > settings.maxParallelRunLength) {
            penalty += (parallelRunLength - settings.maxParallelRunLength) * settings.repetitionPenaltyWeight;
        }

        if (nearestParallelSpacing > 0 && nearestParallelSpacing < settings.minimumParallelSpacing) {
            penalty += (settings.minimumParallelSpacing - nearestParallelSpacing) * settings.repetitionPenaltyWeight;
        }

        if (adjacentParallelBandCount > settings.maxAdjacentParallelBands) {
            penalty += (adjacentParallelBandCount - settings.maxAdjacentParallelBands) * settings.repetitionPenaltyWeight * 2;
        }

        return penalty;
    }

    public static boolean shouldRejectPhase3GridCandidate(
            boolean candidateIsRoad,
            boolean candidateIsCorridor,
            int parallelRunLength,
            int nearestParallelSpacing,
            int adjacentParallelBandCount,
            Phase3GridSuppressionSettings settings) {

        int penalty = phase3GridRepetitionPenalty(
                candidateIsRoad,
                candidateIsCorridor,
                parallelRunLength,
                nearestParallelSpacing,
                adjacentParallelBandCount,
                settings);

        boolean reject = penalty >= 100;
        if (reject) {
            phase3Debug("GridSuppressionReject",
                    "road=" + candidateIsRoad
                            + ", corridor=" + candidateIsCorridor
                            + ", parallelRunLength=" + parallelRunLength
                            + ", nearestParallelSpacing=" + nearestParallelSpacing
                            + ", adjacentParallelBandCount=" + adjacentParallelBandCount
                            + ", penalty=" + penalty);
        }

        return reject;
    }

    /**
     * Blends central-biased fill with grid suppression.
     *
     * Higher central score helps productive fill near the plaza, but repetitive
     * parallel-road/corridor penalties can still reject grill-like candidates.
     */
    public static int phase3CentralFillScoreWithGridSuppression(
            int x,
            int y,
            Phase3CentralBiasedFillSettings fillSettings,
            Phase3GridSuppressionSettings gridSettings,
            boolean candidateIsRoad,
            boolean candidateIsCorridor,
            int parallelRunLength,
            int nearestParallelSpacing,
            int adjacentParallelBandCount) {

        int centralScore = phase3CentralBiasScore(x, y, fillSettings);
        int penalty = phase3GridRepetitionPenalty(
                candidateIsRoad,
                candidateIsCorridor,
                parallelRunLength,
                nearestParallelSpacing,
                adjacentParallelBandCount,
                gridSettings);

        return centralScore - penalty;
    }


    public static final class Phase3GridNeighborhoodMetrics {
        public final int parallelRunLength;
        public final int nearestParallelSpacing;
        public final int adjacentParallelBandCount;

        public Phase3GridNeighborhoodMetrics(
                int parallelRunLength,
                int nearestParallelSpacing,
                int adjacentParallelBandCount) {
            this.parallelRunLength = Math.max(0, parallelRunLength);
            this.nearestParallelSpacing = Math.max(0, nearestParallelSpacing);
            this.adjacentParallelBandCount = Math.max(0, adjacentParallelBandCount);
        }

        @Override
        public String toString() {
            return "Phase3GridNeighborhoodMetrics{" +
                    "parallelRunLength=" + parallelRunLength +
                    ", nearestParallelSpacing=" + nearestParallelSpacing +
                    ", adjacentParallelBandCount=" + adjacentParallelBandCount +
                    '}';
        }
    }

    /**
     * Phase 3 local grid-neighborhood scanner.
     *
     * This uses Phase 3 tile state as a conservative proxy for existing corridor/road
     * floor bands. Later passes may replace the proxy with explicit road/corridor tags.
     */
    public static Phase3GridNeighborhoodMetrics scanPhase3GridNeighborhoodMetrics(
            MapLayerSurfaceAuthority surfaceAuthority,
            int x,
            int y,
            int dx,
            int dy,
            int scanRadius) {

        if (surfaceAuthority == null || !surfaceAuthority.hasPhase3TileState()) {
            return new Phase3GridNeighborhoodMetrics(0, 0, 0);
        }

        int ox = Math.abs(dx) == 1 ? 0 : 1;
        int oy = Math.abs(dy) == 1 ? 0 : 1;
        int forwardDx = Integer.compare(dx, 0);
        int forwardDy = Integer.compare(dy, 0);

        if (forwardDx == 0 && forwardDy == 0) {
            forwardDx = 1;
        }

        int parallelRun = 0;
        for (int step = -Math.max(1, scanRadius); step <= Math.max(1, scanRadius); step++) {
            int tx = x + forwardDx * step;
            int ty = y + forwardDy * step;
            if (surfaceAuthority.isPhase3InBounds(tx, ty)
                    && surfaceAuthority.isPhase3TraversableTile(tx, ty)
                    && !surfaceAuthority.isPhase3BlockedTile(tx, ty)) {
                parallelRun++;
            }
        }

        int nearestSpacing = 0;
        int bandCount = 0;
        int maxSide = Math.max(1, scanRadius);

        for (int side = 1; side <= maxSide; side++) {
            boolean positiveBand = hasPhase3TraversableBandNear(surfaceAuthority, x + ox * side, y + oy * side, forwardDx, forwardDy, scanRadius);
            boolean negativeBand = hasPhase3TraversableBandNear(surfaceAuthority, x - ox * side, y - oy * side, forwardDx, forwardDy, scanRadius);

            if ((positiveBand || negativeBand) && nearestSpacing == 0) {
                nearestSpacing = side;
            }

            if (positiveBand) bandCount++;
            if (negativeBand) bandCount++;
        }

        return new Phase3GridNeighborhoodMetrics(parallelRun, nearestSpacing, bandCount);
    }

    private static boolean hasPhase3TraversableBandNear(
            MapLayerSurfaceAuthority surfaceAuthority,
            int x,
            int y,
            int dx,
            int dy,
            int scanRadius) {
        int hits = 0;
        int needed = Math.max(2, scanRadius / 2);

        for (int step = -Math.max(1, scanRadius); step <= Math.max(1, scanRadius); step++) {
            int tx = x + dx * step;
            int ty = y + dy * step;
            if (surfaceAuthority.isPhase3InBounds(tx, ty)
                    && surfaceAuthority.isPhase3TraversableTile(tx, ty)
                    && !surfaceAuthority.isPhase3BlockedTile(tx, ty)) {
                hits++;
            }
        }

        return hits >= needed;
    }

    public static boolean shouldRejectPhase3GridCandidateFromNeighborhood(
            MapLayerSurfaceAuthority surfaceAuthority,
            boolean candidateIsRoad,
            boolean candidateIsCorridor,
            int x,
            int y,
            int dx,
            int dy,
            int scanRadius,
            Phase3GridSuppressionSettings settings) {

        Phase3GridNeighborhoodMetrics metrics = scanPhase3GridNeighborhoodMetrics(
                surfaceAuthority,
                x,
                y,
                dx,
                dy,
                scanRadius);

        return shouldRejectPhase3GridCandidate(
                candidateIsRoad,
                candidateIsCorridor,
                metrics.parallelRunLength,
                metrics.nearestParallelSpacing,
                metrics.adjacentParallelBandCount,
                settings);
    }


    public static void markPhase3GeneratedRoadRect(
            MapLayerSurfaceAuthority surfaceAuthority,
            int x,
            int y,
            int width,
            int height) {
        if (surfaceAuthority == null) {
            phase3Debug("TileState", "Cannot mark road rect: surface authority is null");
            return;
        }

        surfaceAuthority.markPhase3RoadRect(x, y, width, height);
    }

    public static void markPhase3GeneratedCorridorRect(
            MapLayerSurfaceAuthority surfaceAuthority,
            int x,
            int y,
            int width,
            int height) {
        if (surfaceAuthority == null) {
            phase3Debug("TileState", "Cannot mark corridor rect: surface authority is null");
            return;
        }

        surfaceAuthority.markPhase3CorridorRect(x, y, width, height);
    }


    public static Phase3GridNeighborhoodMetrics scanPhase3RoadCorridorGridNeighborhoodMetrics(
            MapLayerSurfaceAuthority surfaceAuthority,
            int x,
            int y,
            int dx,
            int dy,
            int scanRadius,
            boolean roadCandidate,
            boolean corridorCandidate) {

        if (surfaceAuthority == null || !surfaceAuthority.hasPhase3TileState()) {
            return new Phase3GridNeighborhoodMetrics(0, 0, 0);
        }

        int ox = Math.abs(dx) == 1 ? 0 : 1;
        int oy = Math.abs(dy) == 1 ? 0 : 1;
        int forwardDx = Integer.compare(dx, 0);
        int forwardDy = Integer.compare(dy, 0);

        if (forwardDx == 0 && forwardDy == 0) {
            forwardDx = 1;
        }

        int parallelRun = 0;
        for (int step = -Math.max(1, scanRadius); step <= Math.max(1, scanRadius); step++) {
            int tx = x + forwardDx * step;
            int ty = y + forwardDy * step;
            if (isPhase3MatchingCirculationTile(surfaceAuthority, tx, ty, roadCandidate, corridorCandidate)) {
                parallelRun++;
            }
        }

        int nearestSpacing = 0;
        int bandCount = 0;
        int maxSide = Math.max(1, scanRadius);

        for (int side = 1; side <= maxSide; side++) {
            boolean positiveBand = hasPhase3MatchingCirculationBandNear(surfaceAuthority, x + ox * side, y + oy * side, forwardDx, forwardDy, scanRadius, roadCandidate, corridorCandidate);
            boolean negativeBand = hasPhase3MatchingCirculationBandNear(surfaceAuthority, x - ox * side, y - oy * side, forwardDx, forwardDy, scanRadius, roadCandidate, corridorCandidate);

            if ((positiveBand || negativeBand) && nearestSpacing == 0) {
                nearestSpacing = side;
            }

            if (positiveBand) bandCount++;
            if (negativeBand) bandCount++;
        }

        return new Phase3GridNeighborhoodMetrics(parallelRun, nearestSpacing, bandCount);
    }

    private static boolean isPhase3MatchingCirculationTile(
            MapLayerSurfaceAuthority surfaceAuthority,
            int x,
            int y,
            boolean roadCandidate,
            boolean corridorCandidate) {
        if (!surfaceAuthority.isPhase3InBounds(x, y)) {
            return false;
        }

        if (roadCandidate && surfaceAuthority.isPhase3RoadTile(x, y)) {
            return true;
        }

        if (corridorCandidate && surfaceAuthority.isPhase3CorridorTile(x, y)) {
            return true;
        }

        return false;
    }

    private static boolean hasPhase3MatchingCirculationBandNear(
            MapLayerSurfaceAuthority surfaceAuthority,
            int x,
            int y,
            int dx,
            int dy,
            int scanRadius,
            boolean roadCandidate,
            boolean corridorCandidate) {
        int hits = 0;
        int needed = Math.max(2, scanRadius / 2);

        for (int step = -Math.max(1, scanRadius); step <= Math.max(1, scanRadius); step++) {
            int tx = x + dx * step;
            int ty = y + dy * step;
            if (isPhase3MatchingCirculationTile(surfaceAuthority, tx, ty, roadCandidate, corridorCandidate)) {
                hits++;
            }
        }

        return hits >= needed;
    }


    /**
     * Phase 3 tag-aware road/corridor candidate scoring.
     *
     * Uses explicit road/corridor tile tags rather than generic traversable floor
     * to avoid counting room/plaza floors as grill-pattern circulation.
     */
    public static int scorePhase3TaggedRoadCorridorCandidate(
            MapLayerSurfaceAuthority surfaceAuthority,
            Phase3CentralBiasedFillSettings fillSettings,
            Phase3GridSuppressionSettings gridSettings,
            boolean candidateIsRoad,
            boolean candidateIsCorridor,
            int x,
            int y,
            int dx,
            int dy,
            int scanRadius) {

        Phase3GridNeighborhoodMetrics metrics = scanPhase3RoadCorridorGridNeighborhoodMetrics(
                surfaceAuthority,
                x,
                y,
                dx,
                dy,
                scanRadius,
                candidateIsRoad,
                candidateIsCorridor);

        return phase3CentralFillScoreWithGridSuppression(
                x,
                y,
                fillSettings,
                gridSettings,
                candidateIsRoad,
                candidateIsCorridor,
                metrics.parallelRunLength,
                metrics.nearestParallelSpacing,
                metrics.adjacentParallelBandCount);
    }

    public static boolean shouldRejectPhase3TaggedRoadCorridorCandidate(
            MapLayerSurfaceAuthority surfaceAuthority,
            Phase3GridSuppressionSettings gridSettings,
            boolean candidateIsRoad,
            boolean candidateIsCorridor,
            int x,
            int y,
            int dx,
            int dy,
            int scanRadius) {

        Phase3GridNeighborhoodMetrics metrics = scanPhase3RoadCorridorGridNeighborhoodMetrics(
                surfaceAuthority,
                x,
                y,
                dx,
                dy,
                scanRadius,
                candidateIsRoad,
                candidateIsCorridor);

        boolean reject = shouldRejectPhase3GridCandidate(
                candidateIsRoad,
                candidateIsCorridor,
                metrics.parallelRunLength,
                metrics.nearestParallelSpacing,
                metrics.adjacentParallelBandCount,
                gridSettings);

        if (reject) {
            phase3Debug("TaggedGridCandidateReject",
                    "road=" + candidateIsRoad
                            + ", corridor=" + candidateIsCorridor
                            + ", x=" + x
                            + ", y=" + y
                            + ", dx=" + dx
                            + ", dy=" + dy
                            + ", metrics=" + metrics);
        }

        return reject;
    }

    /**
     * Combined candidate gate for future road/corridor placement call sites.
     *
     * Returns false when the candidate is unsafe or would intensify an unwanted
     * grill pattern. Actual placement remains deferred to confirmed call sites.
     */
    public static boolean shouldAcceptPhase3TaggedRoadCorridorCandidate(
            ZoneGenerationContext context,
            MapLayerSurfaceAuthority surfaceAuthority,
            Phase3GridSuppressionSettings gridSettings,
            boolean candidateIsRoad,
            boolean candidateIsCorridor,
            int x,
            int y,
            int width,
            int height,
            int dx,
            int dy,
            int scanRadius) {

        if (!isOrdinaryPlacementInsidePhase3InteriorBounds(context, x, y, width, height)) {
            return false;
        }

        if (shouldRejectPhase3TaggedRoadCorridorCandidate(
                surfaceAuthority,
                gridSettings,
                candidateIsRoad,
                candidateIsCorridor,
                x,
                y,
                dx,
                dy,
                scanRadius)) {
            return false;
        }

        return true;
    }


    /**
     * Phase 3 tagged circulation commit bridge.
     *
     * This helper should be called only after a road/corridor candidate has passed
     * placement validation. It marks the accepted candidate as explicit road/corridor
     * circulation so future grid-suppression scans do not rely on generic floor tiles.
     */
    public static boolean commitPhase3TaggedRoadCorridorCandidate(
            ZoneGenerationContext context,
            MapLayerSurfaceAuthority surfaceAuthority,
            Phase3GridSuppressionSettings gridSettings,
            boolean candidateIsRoad,
            boolean candidateIsCorridor,
            int x,
            int y,
            int width,
            int height,
            int dx,
            int dy,
            int scanRadius) {

        if (surfaceAuthority == null) {
            phase3Debug("TaggedCirculationCommitReject", "surface authority is null");
            return false;
        }

        boolean accepted = shouldAcceptPhase3TaggedRoadCorridorCandidate(
                context,
                surfaceAuthority,
                gridSettings,
                candidateIsRoad,
                candidateIsCorridor,
                x,
                y,
                width,
                height,
                dx,
                dy,
                scanRadius);

        if (!accepted) {
            phase3Debug("TaggedCirculationCommitReject",
                    "candidate rejected before commit; road=" + candidateIsRoad
                            + ", corridor=" + candidateIsCorridor
                            + ", rect=" + ZoneGenerationContext.rect(x, y, width, height));
            return false;
        }

        if (candidateIsRoad) {
            markPhase3GeneratedRoadRect(surfaceAuthority, x, y, width, height);
        } else if (candidateIsCorridor) {
            markPhase3GeneratedCorridorRect(surfaceAuthority, x, y, width, height);
        } else {
            phase3Debug("TaggedCirculationCommitReject", "candidate was neither road nor corridor");
            return false;
        }

        return true;
    }

    public static boolean commitPhase3TaggedRoadCandidate(
            ZoneGenerationContext context,
            MapLayerSurfaceAuthority surfaceAuthority,
            Phase3GridSuppressionSettings gridSettings,
            int x,
            int y,
            int width,
            int height,
            int dx,
            int dy,
            int scanRadius) {
        return commitPhase3TaggedRoadCorridorCandidate(
                context,
                surfaceAuthority,
                gridSettings,
                true,
                false,
                x,
                y,
                width,
                height,
                dx,
                dy,
                scanRadius);
    }

    public static boolean commitPhase3TaggedCorridorCandidate(
            ZoneGenerationContext context,
            MapLayerSurfaceAuthority surfaceAuthority,
            Phase3GridSuppressionSettings gridSettings,
            int x,
            int y,
            int width,
            int height,
            int dx,
            int dy,
            int scanRadius) {
        return commitPhase3TaggedRoadCorridorCandidate(
                context,
                surfaceAuthority,
                gridSettings,
                false,
                true,
                x,
                y,
                width,
                height,
                dx,
                dy,
                scanRadius);
    }


    /**
     * Phase 3 migration wrapper for road/corridor placement.
     *
     * Future road/corridor commit call sites should prefer this wrapper while legacy
     * placement is still being migrated. If tagged validation succeeds, the candidate
     * is tagged and committed through the Phase 3 bridge. If validation fails, this
     * returns false so the caller can use its existing retry/fallback logic instead
     * of silently placing a grill-like or out-of-bounds candidate.
     */
    public static boolean tryPhase3TaggedCirculationCommitOrLegacyFallback(
            ZoneGenerationContext context,
            MapLayerSurfaceAuthority surfaceAuthority,
            Phase3GridSuppressionSettings gridSettings,
            boolean candidateIsRoad,
            boolean candidateIsCorridor,
            int x,
            int y,
            int width,
            int height,
            int dx,
            int dy,
            int scanRadius,
            String sourceLabel) {

        boolean committed = commitPhase3TaggedRoadCorridorCandidate(
                context,
                surfaceAuthority,
                gridSettings,
                candidateIsRoad,
                candidateIsCorridor,
                x,
                y,
                width,
                height,
                dx,
                dy,
                scanRadius);

        if (!committed) {
            phase3Debug("TaggedCirculationLegacyFallback",
                    "source=" + sourceLabel
                            + ", road=" + candidateIsRoad
                            + ", corridor=" + candidateIsCorridor
                            + ", rect=" + ZoneGenerationContext.rect(x, y, width, height)
                            + ", caller should retry/fallback rather than direct commit");
        }

        return committed;
    }


    public static final class Phase3CirculationCommitReadiness {
        public final boolean ready;
        public final boolean hasContext;
        public final boolean hasSurfaceAuthority;
        public final boolean hasDirection;
        public final boolean hasDimensions;
        public final String reason;

        public Phase3CirculationCommitReadiness(
                boolean ready,
                boolean hasContext,
                boolean hasSurfaceAuthority,
                boolean hasDirection,
                boolean hasDimensions,
                String reason) {
            this.ready = ready;
            this.hasContext = hasContext;
            this.hasSurfaceAuthority = hasSurfaceAuthority;
            this.hasDirection = hasDirection;
            this.hasDimensions = hasDimensions;
            this.reason = reason == null ? "" : reason;
        }

        @Override
        public String toString() {
            return "Phase3CirculationCommitReadiness{" +
                    "ready=" + ready +
                    ", hasContext=" + hasContext +
                    ", hasSurfaceAuthority=" + hasSurfaceAuthority +
                    ", hasDirection=" + hasDirection +
                    ", hasDimensions=" + hasDimensions +
                    ", reason='" + reason + '\'' +
                    '}';
        }
    }

    /**
     * Phase 3 readiness check for migrating a road/corridor commit point.
     *
     * A commit site is ready only when it has the context, surface authority,
     * orthogonal direction, and dimensions needed by the tagged circulation gate.
     */
    public static Phase3CirculationCommitReadiness checkPhase3CirculationCommitReadiness(
            ZoneGenerationContext context,
            MapLayerSurfaceAuthority surfaceAuthority,
            int width,
            int height,
            int dx,
            int dy,
            String sourceLabel) {

        boolean hasContext = context != null;
        boolean hasSurface = surfaceAuthority != null;
        boolean hasDimensions = width > 0 && height > 0;
        boolean hasDirection = (Math.abs(dx) == 1 && dy == 0) || (Math.abs(dy) == 1 && dx == 0);

        boolean ready = hasContext && hasSurface && hasDimensions && hasDirection;
        String reason = ready
                ? "ready"
                : "not ready for tagged circulation migration: source=" + sourceLabel
                    + ", hasContext=" + hasContext
                    + ", hasSurface=" + hasSurface
                    + ", hasDimensions=" + hasDimensions
                    + ", hasDirection=" + hasDirection;

        if (!ready) {
            phase3Debug("CirculationCommitReadiness", reason);
        }

        return new Phase3CirculationCommitReadiness(
                ready,
                hasContext,
                hasSurface,
                hasDirection,
                hasDimensions,
                reason);
    }

    /**
     * Ready-or-fallback wrapper for future call-site migration.
     *
     * If a site is not ready for tagged migration, this returns false and logs why.
     * The caller should retain legacy behavior only as a temporary fallback and not
     * treat this as validation success.
     */
    public static boolean tryReadyPhase3TaggedCirculationCommit(
            ZoneGenerationContext context,
            MapLayerSurfaceAuthority surfaceAuthority,
            Phase3GridSuppressionSettings gridSettings,
            boolean candidateIsRoad,
            boolean candidateIsCorridor,
            int x,
            int y,
            int width,
            int height,
            int dx,
            int dy,
            int scanRadius,
            String sourceLabel) {

        Phase3CirculationCommitReadiness readiness = checkPhase3CirculationCommitReadiness(
                context,
                surfaceAuthority,
                width,
                height,
                dx,
                dy,
                sourceLabel);

        if (!readiness.ready) {
            return false;
        }

        return tryPhase3TaggedCirculationCommitOrLegacyFallback(
                context,
                surfaceAuthority,
                gridSettings,
                candidateIsRoad,
                candidateIsCorridor,
                x,
                y,
                width,
                height,
                dx,
                dy,
                scanRadius,
                sourceLabel);
    }


    public static final class Phase3SingleSiteCirculationMigrationResult {
        public final boolean migratedCommitSucceeded;
        public final boolean shouldUseLegacyFallback;
        public final boolean shouldRetryCandidate;
        public final String sourceLabel;
        public final String reason;

        public Phase3SingleSiteCirculationMigrationResult(
                boolean migratedCommitSucceeded,
                boolean shouldUseLegacyFallback,
                boolean shouldRetryCandidate,
                String sourceLabel,
                String reason) {
            this.migratedCommitSucceeded = migratedCommitSucceeded;
            this.shouldUseLegacyFallback = shouldUseLegacyFallback;
            this.shouldRetryCandidate = shouldRetryCandidate;
            this.sourceLabel = sourceLabel == null ? "" : sourceLabel;
            this.reason = reason == null ? "" : reason;
        }

        @Override
        public String toString() {
            return "Phase3SingleSiteCirculationMigrationResult{" +
                    "migratedCommitSucceeded=" + migratedCommitSucceeded +
                    ", shouldUseLegacyFallback=" + shouldUseLegacyFallback +
                    ", shouldRetryCandidate=" + shouldRetryCandidate +
                    ", sourceLabel='" + sourceLabel + '\'' +
                    ", reason='" + reason + '\'' +
                    '}';
        }
    }

    /**
     * Phase 3 single-site migration harness.
     *
     * Use this at one confirmed road/corridor commit point first. It gives the
     * caller a clear result: migrated commit succeeded, retry candidate, or temporary
     * legacy fallback. This avoids broad migration before generation tests.
     */
    public static Phase3SingleSiteCirculationMigrationResult attemptPhase3SingleSiteCirculationMigration(
            ZoneGenerationContext context,
            MapLayerSurfaceAuthority surfaceAuthority,
            Phase3GridSuppressionSettings gridSettings,
            boolean candidateIsRoad,
            boolean candidateIsCorridor,
            int x,
            int y,
            int width,
            int height,
            int dx,
            int dy,
            int scanRadius,
            String sourceLabel,
            boolean allowTemporaryLegacyFallback) {

        Phase3CirculationCommitReadiness readiness = checkPhase3CirculationCommitReadiness(
                context,
                surfaceAuthority,
                width,
                height,
                dx,
                dy,
                sourceLabel);

        if (!readiness.ready) {
            Phase3SingleSiteCirculationMigrationResult result = new Phase3SingleSiteCirculationMigrationResult(
                    false,
                    allowTemporaryLegacyFallback,
                    !allowTemporaryLegacyFallback,
                    sourceLabel,
                    "migration site not ready: " + readiness.reason);
            phase3Debug("SingleSiteCirculationMigration", result.toString());
            return result;
        }

        boolean committed = tryPhase3TaggedCirculationCommitOrLegacyFallback(
                context,
                surfaceAuthority,
                gridSettings,
                candidateIsRoad,
                candidateIsCorridor,
                x,
                y,
                width,
                height,
                dx,
                dy,
                scanRadius,
                sourceLabel);

        if (committed) {
            return new Phase3SingleSiteCirculationMigrationResult(
                    true,
                    false,
                    false,
                    sourceLabel,
                    "tagged migration commit succeeded");
        }

        Phase3SingleSiteCirculationMigrationResult rejected = new Phase3SingleSiteCirculationMigrationResult(
                false,
                allowTemporaryLegacyFallback,
                !allowTemporaryLegacyFallback,
                sourceLabel,
                "tagged migration rejected candidate");
        phase3Debug("SingleSiteCirculationMigration", rejected.toString());
        return rejected;
    }


    public static final class Phase3CirculationMigrationTestReport {
        public final String sourceLabel;
        public final int attemptedCandidates;
        public final int taggedCommitsSucceeded;
        public final int retryRequested;
        public final int legacyFallbackRequested;
        public final int rejectedCandidates;

        public Phase3CirculationMigrationTestReport(
                String sourceLabel,
                int attemptedCandidates,
                int taggedCommitsSucceeded,
                int retryRequested,
                int legacyFallbackRequested,
                int rejectedCandidates) {
            this.sourceLabel = sourceLabel == null ? "" : sourceLabel;
            this.attemptedCandidates = Math.max(0, attemptedCandidates);
            this.taggedCommitsSucceeded = Math.max(0, taggedCommitsSucceeded);
            this.retryRequested = Math.max(0, retryRequested);
            this.legacyFallbackRequested = Math.max(0, legacyFallbackRequested);
            this.rejectedCandidates = Math.max(0, rejectedCandidates);
        }

        public boolean looksStableEnoughForNextSite() {
            return attemptedCandidates > 0
                    && taggedCommitsSucceeded > 0
                    && rejectedCandidates == 0;
        }

        @Override
        public String toString() {
            return "Phase3CirculationMigrationTestReport{" +
                    "sourceLabel='" + sourceLabel + '\'' +
                    ", attemptedCandidates=" + attemptedCandidates +
                    ", taggedCommitsSucceeded=" + taggedCommitsSucceeded +
                    ", retryRequested=" + retryRequested +
                    ", legacyFallbackRequested=" + legacyFallbackRequested +
                    ", rejectedCandidates=" + rejectedCandidates +
                    ", stableEnoughForNextSite=" + looksStableEnoughForNextSite() +
                    '}';
        }
    }

    public static Phase3CirculationMigrationTestReport summarizePhase3SingleSiteMigrationResults(
            String sourceLabel,
            Phase3SingleSiteCirculationMigrationResult[] results) {
        if (results == null) {
            return new Phase3CirculationMigrationTestReport(sourceLabel, 0, 0, 0, 0, 0);
        }

        int attempted = 0;
        int succeeded = 0;
        int retries = 0;
        int legacy = 0;
        int rejected = 0;

        for (Phase3SingleSiteCirculationMigrationResult result : results) {
            if (result == null) {
                continue;
            }

            attempted++;
            if (result.migratedCommitSucceeded) succeeded++;
            if (result.shouldRetryCandidate) retries++;
            if (result.shouldUseLegacyFallback) legacy++;
            if (!result.migratedCommitSucceeded && !result.shouldRetryCandidate && !result.shouldUseLegacyFallback) {
                rejected++;
            }
        }

        Phase3CirculationMigrationTestReport report = new Phase3CirculationMigrationTestReport(
                sourceLabel,
                attempted,
                succeeded,
                retries,
                legacy,
                rejected);

        phase3Debug("CirculationMigrationTestReport", report.toString());
        return report;
    }

    public static boolean shouldAdvancePhase3CirculationMigrationAfterReport(
            Phase3CirculationMigrationTestReport report) {
        if (report == null) {
            return false;
        }

        boolean advance = report.looksStableEnoughForNextSite();
        if (!advance) {
            phase3Debug("CirculationMigrationHold",
                    "holding further migration until first site is stable; report=" + report);
        }
        return advance;
    }


    public static final class Phase3FirstSiteWiringChecklist {
        public final String sourceLabel;
        public final boolean hasContext;
        public final boolean hasSurfaceAuthority;
        public final boolean hasCandidateRect;
        public final boolean hasDirection;
        public final boolean hasRoadOrCorridorIdentity;
        public final boolean hasRetryOrFallbackPath;
        public final boolean ready;

        public Phase3FirstSiteWiringChecklist(
                String sourceLabel,
                boolean hasContext,
                boolean hasSurfaceAuthority,
                boolean hasCandidateRect,
                boolean hasDirection,
                boolean hasRoadOrCorridorIdentity,
                boolean hasRetryOrFallbackPath) {
            this.sourceLabel = sourceLabel == null ? "" : sourceLabel;
            this.hasContext = hasContext;
            this.hasSurfaceAuthority = hasSurfaceAuthority;
            this.hasCandidateRect = hasCandidateRect;
            this.hasDirection = hasDirection;
            this.hasRoadOrCorridorIdentity = hasRoadOrCorridorIdentity;
            this.hasRetryOrFallbackPath = hasRetryOrFallbackPath;
            this.ready = hasContext
                    && hasSurfaceAuthority
                    && hasCandidateRect
                    && hasDirection
                    && hasRoadOrCorridorIdentity
                    && hasRetryOrFallbackPath;
        }

        @Override
        public String toString() {
            return "Phase3FirstSiteWiringChecklist{" +
                    "sourceLabel='" + sourceLabel + '\'' +
                    ", hasContext=" + hasContext +
                    ", hasSurfaceAuthority=" + hasSurfaceAuthority +
                    ", hasCandidateRect=" + hasCandidateRect +
                    ", hasDirection=" + hasDirection +
                    ", hasRoadOrCorridorIdentity=" + hasRoadOrCorridorIdentity +
                    ", hasRetryOrFallbackPath=" + hasRetryOrFallbackPath +
                    ", ready=" + ready +
                    '}';
        }
    }

    /**
     * Checklist for the first real road/corridor migration site.
     *
     * This is intentionally stricter than the lower-level readiness check because
     * the first migration site must be testable and reversible.
     */
    public static Phase3FirstSiteWiringChecklist checkPhase3FirstSiteWiringChecklist(
            ZoneGenerationContext context,
            MapLayerSurfaceAuthority surfaceAuthority,
            boolean hasCandidateRect,
            int width,
            int height,
            int dx,
            int dy,
            boolean candidateIsRoad,
            boolean candidateIsCorridor,
            boolean hasRetryOrFallbackPath,
            String sourceLabel) {

        boolean hasDirection = (Math.abs(dx) == 1 && dy == 0) || (Math.abs(dy) == 1 && dx == 0);
        boolean hasIdentity = candidateIsRoad != candidateIsCorridor;
        boolean rectKnown = hasCandidateRect && width > 0 && height > 0;

        Phase3FirstSiteWiringChecklist checklist = new Phase3FirstSiteWiringChecklist(
                sourceLabel,
                context != null,
                surfaceAuthority != null,
                rectKnown,
                hasDirection,
                hasIdentity,
                hasRetryOrFallbackPath);

        if (!checklist.ready) {
            phase3Debug("FirstSiteWiringChecklist", checklist.toString());
        }

        return checklist;
    }

    public static boolean mayWirePhase3FirstSiteCirculationMigration(
            Phase3FirstSiteWiringChecklist checklist) {
        return checklist != null && checklist.ready;
    }


    public static final class Phase3CirculationMigrationDryRunResult {
        public final boolean ready;
        public final boolean wouldAcceptCandidate;
        public final boolean wouldRejectForGrid;
        public final boolean wouldRejectForBounds;
        public final String sourceLabel;
        public final String reason;

        public Phase3CirculationMigrationDryRunResult(
                boolean ready,
                boolean wouldAcceptCandidate,
                boolean wouldRejectForGrid,
                boolean wouldRejectForBounds,
                String sourceLabel,
                String reason) {
            this.ready = ready;
            this.wouldAcceptCandidate = wouldAcceptCandidate;
            this.wouldRejectForGrid = wouldRejectForGrid;
            this.wouldRejectForBounds = wouldRejectForBounds;
            this.sourceLabel = sourceLabel == null ? "" : sourceLabel;
            this.reason = reason == null ? "" : reason;
        }

        @Override
        public String toString() {
            return "Phase3CirculationMigrationDryRunResult{" +
                    "ready=" + ready +
                    ", wouldAcceptCandidate=" + wouldAcceptCandidate +
                    ", wouldRejectForGrid=" + wouldRejectForGrid +
                    ", wouldRejectForBounds=" + wouldRejectForBounds +
                    ", sourceLabel='" + sourceLabel + '\'' +
                    ", reason='" + reason + '\'' +
                    '}';
        }
    }

    /**
     * Phase 3 dry-run harness for first-site road/corridor migration.
     *
     * This checks whether a candidate would pass tagged circulation validation
     * without marking road/corridor tags or changing tile state.
     */
    public static Phase3CirculationMigrationDryRunResult dryRunPhase3SingleSiteCirculationMigration(
            ZoneGenerationContext context,
            MapLayerSurfaceAuthority surfaceAuthority,
            Phase3GridSuppressionSettings gridSettings,
            boolean candidateIsRoad,
            boolean candidateIsCorridor,
            int x,
            int y,
            int width,
            int height,
            int dx,
            int dy,
            int scanRadius,
            String sourceLabel,
            boolean hasRetryOrFallbackPath) {

        Phase3FirstSiteWiringChecklist checklist = checkPhase3FirstSiteWiringChecklist(
                context,
                surfaceAuthority,
                true,
                width,
                height,
                dx,
                dy,
                candidateIsRoad,
                candidateIsCorridor,
                hasRetryOrFallbackPath,
                sourceLabel);

        if (!mayWirePhase3FirstSiteCirculationMigration(checklist)) {
            Phase3CirculationMigrationDryRunResult result = new Phase3CirculationMigrationDryRunResult(
                    false,
                    false,
                    false,
                    false,
                    sourceLabel,
                    "first-site checklist failed: " + checklist);
            phase3Debug("CirculationMigrationDryRun", result.toString());
            return result;
        }

        boolean boundsOk = isOrdinaryPlacementInsidePhase3InteriorBounds(context, x, y, width, height);
        boolean gridReject = shouldRejectPhase3TaggedRoadCorridorCandidate(
                surfaceAuthority,
                gridSettings,
                candidateIsRoad,
                candidateIsCorridor,
                x,
                y,
                dx,
                dy,
                scanRadius);

        boolean wouldAccept = boundsOk && !gridReject;

        Phase3CirculationMigrationDryRunResult result = new Phase3CirculationMigrationDryRunResult(
                true,
                wouldAccept,
                gridReject,
                !boundsOk,
                sourceLabel,
                wouldAccept ? "candidate would pass tagged migration dry-run" : "candidate would be rejected before commit");

        phase3Debug("CirculationMigrationDryRun", result.toString());
        return result;
    }

    public static boolean shouldProceedFromPhase3DryRunToSingleSiteMigration(
            Phase3CirculationMigrationDryRunResult dryRunResult) {
        return dryRunResult != null
                && dryRunResult.ready
                && dryRunResult.wouldAcceptCandidate
                && !dryRunResult.wouldRejectForGrid
                && !dryRunResult.wouldRejectForBounds;
    }


    public static final class Phase3CirculationDryRunBatchReport {
        public final int totalDryRuns;
        public final int readyCount;
        public final int acceptedCount;
        public final int gridRejectedCount;
        public final int boundsRejectedCount;
        public final String bestSourceLabel;
        public final boolean hasMigrationCandidate;

        public Phase3CirculationDryRunBatchReport(
                int totalDryRuns,
                int readyCount,
                int acceptedCount,
                int gridRejectedCount,
                int boundsRejectedCount,
                String bestSourceLabel) {
            this.totalDryRuns = Math.max(0, totalDryRuns);
            this.readyCount = Math.max(0, readyCount);
            this.acceptedCount = Math.max(0, acceptedCount);
            this.gridRejectedCount = Math.max(0, gridRejectedCount);
            this.boundsRejectedCount = Math.max(0, boundsRejectedCount);
            this.bestSourceLabel = bestSourceLabel == null ? "" : bestSourceLabel;
            this.hasMigrationCandidate = acceptedCount > 0 && !this.bestSourceLabel.isEmpty();
        }

        @Override
        public String toString() {
            return "Phase3CirculationDryRunBatchReport{" +
                    "totalDryRuns=" + totalDryRuns +
                    ", readyCount=" + readyCount +
                    ", acceptedCount=" + acceptedCount +
                    ", gridRejectedCount=" + gridRejectedCount +
                    ", boundsRejectedCount=" + boundsRejectedCount +
                    ", bestSourceLabel='" + bestSourceLabel + '\'' +
                    ", hasMigrationCandidate=" + hasMigrationCandidate +
                    '}';
        }
    }

    public static Phase3CirculationDryRunBatchReport summarizePhase3CirculationDryRuns(
            Phase3CirculationMigrationDryRunResult[] dryRuns) {
        if (dryRuns == null) {
            return new Phase3CirculationDryRunBatchReport(0, 0, 0, 0, 0, "");
        }

        int total = 0;
        int ready = 0;
        int accepted = 0;
        int gridRejected = 0;
        int boundsRejected = 0;
        String best = "";

        for (Phase3CirculationMigrationDryRunResult result : dryRuns) {
            if (result == null) {
                continue;
            }

            total++;
            if (result.ready) ready++;
            if (result.wouldAcceptCandidate) {
                accepted++;
                if (best.isEmpty()) {
                    best = result.sourceLabel;
                }
            }
            if (result.wouldRejectForGrid) gridRejected++;
            if (result.wouldRejectForBounds) boundsRejected++;
        }

        Phase3CirculationDryRunBatchReport report = new Phase3CirculationDryRunBatchReport(
                total,
                ready,
                accepted,
                gridRejected,
                boundsRejected,
                best);

        phase3Debug("CirculationDryRunBatchReport", report.toString());
        return report;
    }

    public static boolean shouldSelectPhase3DryRunCandidateForMigration(
            Phase3CirculationDryRunBatchReport report) {
        if (report == null) {
            return false;
        }

        boolean select = report.hasMigrationCandidate
                && report.acceptedCount > 0
                && report.boundsRejectedCount == 0;

        if (!select) {
            phase3Debug("CirculationDryRunSelectionHold",
                    "no safe first-site migration candidate yet; report=" + report);
        }

        return select;
    }


    public static final class Phase3FirstSiteSelectionRegistryEntry {
        public final String sourceLabel;
        public final boolean dryRunAccepted;
        public final boolean gridRejected;
        public final boolean boundsRejected;
        public final int preferenceWeight;

        public Phase3FirstSiteSelectionRegistryEntry(
                String sourceLabel,
                boolean dryRunAccepted,
                boolean gridRejected,
                boolean boundsRejected,
                int preferenceWeight) {
            this.sourceLabel = sourceLabel == null ? "" : sourceLabel;
            this.dryRunAccepted = dryRunAccepted;
            this.gridRejected = gridRejected;
            this.boundsRejected = boundsRejected;
            this.preferenceWeight = Math.max(0, preferenceWeight);
        }

        public boolean isViableFirstMigrationSite() {
            return dryRunAccepted && !gridRejected && !boundsRejected;
        }

        @Override
        public String toString() {
            return "Phase3FirstSiteSelectionRegistryEntry{" +
                    "sourceLabel='" + sourceLabel + '\'' +
                    ", dryRunAccepted=" + dryRunAccepted +
                    ", gridRejected=" + gridRejected +
                    ", boundsRejected=" + boundsRejected +
                    ", preferenceWeight=" + preferenceWeight +
                    ", viable=" + isViableFirstMigrationSite() +
                    '}';
        }
    }

    public static Phase3FirstSiteSelectionRegistryEntry selectPreferredPhase3FirstMigrationSite(
            Phase3FirstSiteSelectionRegistryEntry[] entries) {

        if (entries == null || entries.length == 0) {
            return null;
        }

        Phase3FirstSiteSelectionRegistryEntry best = null;

        for (Phase3FirstSiteSelectionRegistryEntry entry : entries) {
            if (entry == null || !entry.isViableFirstMigrationSite()) {
                continue;
            }

            if (best == null || entry.preferenceWeight > best.preferenceWeight) {
                best = entry;
            }
        }

        if (best != null) {
            phase3Debug("FirstSiteSelection", "selected first migration site: " + best);
        } else {
            phase3Debug("FirstSiteSelection", "no viable first migration site available");
        }

        return best;
    }

    public static boolean shouldHoldPhase3MigrationSelection(
            Phase3FirstSiteSelectionRegistryEntry selectedEntry) {

        return selectedEntry == null || !selectedEntry.isViableFirstMigrationSite();
    }

}
