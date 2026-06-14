package mechanist;

import java.io.*;
import java.nio.file.*;
import java.util.*;

class WorldSetupSettings {
    static final String[] NPC_DENSITY = {"Sparse", "Standard", "Crowded", "Teeming"};
    static final String[] ZONE_SIZE = {"Compact", "Standard", "Large", "Sprawling"};
    static final String[] ZONE_DENSITY = {"Loose", "Standard", "Dense", "Packed"};
    static final String[] PRICE = {"Cheap", "Standard", "Hard", "Punishing"};
    static final String[] CRAFT = {"Forgiving", "Standard", "Hard", "Punishing"};
    static final String[] AGE = {"Fresh", "Established", "Old", "Ancient"};
    int npcDensity = 1, zoneSize = 1, zoneDensity = 1, priceDifficulty = 1, craftDifficulty = 1, simulationAge = 1;
    boolean hoarderMode = false;
    static WorldSetupSettings standard(){ return new WorldSetupSettings(); }
    WorldSetupSettings copy(){ WorldSetupSettings s = new WorldSetupSettings(); s.npcDensity=npcDensity; s.zoneSize=zoneSize; s.zoneDensity=zoneDensity; s.priceDifficulty=priceDifficulty; s.craftDifficulty=craftDifficulty; s.simulationAge=simulationAge; s.hoarderMode=hoarderMode; return s; }
    void cycleNpcDensity(){ npcDensity = (npcDensity + 1) % NPC_DENSITY.length; }
    void cycleZoneSize(){ zoneSize = (zoneSize + 1) % ZONE_SIZE.length; }
    void cycleZoneDensity(){ zoneDensity = (zoneDensity + 1) % ZONE_DENSITY.length; }
    void cyclePriceDifficulty(){ priceDifficulty = (priceDifficulty + 1) % PRICE.length; }
    void cycleCraftDifficulty(){ craftDifficulty = (craftDifficulty + 1) % CRAFT.length; }
    void cycleSimulationAge(){ simulationAge = (simulationAge + 1) % AGE.length; }
    double npcDensityMultiplier(){ return new double[]{0.55, 1.0, 1.35, 1.8}[Math.max(0, Math.min(npcDensity, 3))]; }
    double zoneDensityMultiplier(){ return new double[]{0.78, 1.0, 1.22, 1.45}[Math.max(0, Math.min(zoneDensity, 3))]; }
    double priceMultiplier(){ return new double[]{0.75, 1.0, 1.35, 1.75}[Math.max(0, Math.min(priceDifficulty, 3))]; }
    double craftMultiplier(){ return new double[]{0.75, 1.0, 1.35, 1.75}[Math.max(0, Math.min(craftDifficulty, 3))]; }
    int simulationBatches(){ return new int[]{1, 2, 4, 7}[Math.max(0, Math.min(simulationAge, 3))]; }
    WorldGenerationScaleProfile scaleProfile(WorldGenerationScaleProfile base){
        int z = Math.max(0, Math.min(zoneSize, 3));
        int minWeight = WorldGenerationApi.minWorldgenWeightForZoneSize(z);
        int maxWeight = WorldGenerationApi.maxWorldgenWeightForZoneSize(z);
        double minScale = WorldGenerationApi.dimensionScaleForWorldgenWeight(minWeight);
        double maxScale = WorldGenerationApi.dimensionScaleForWorldgenWeight(maxWeight);
        int minW = Math.max(96, (int)Math.round(base.minWidth * minScale));
        int maxW = Math.max(minW + 8, (int)Math.round(base.maxWidth * maxScale));
        int minH = Math.max(72, (int)Math.round(base.minHeight * minScale));
        int maxH = Math.max(minH + 8, (int)Math.round(base.maxHeight * maxScale));
        double density = zoneDensityMultiplier();
        double roomPressure = 0.92 + ((minWeight + maxWeight) * 0.5 - WorldGenerationApi.MIN_WORLDGEN_WEIGHT) / (double)(WorldGenerationApi.MAX_WORLDGEN_WEIGHT - WorldGenerationApi.MIN_WORLDGEN_WEIGHT) * 0.34;
        double sizeRoomPressure = new double[]{0.95, 1.16, 1.55, 2.05}[z];
        double roomScale = density * roomPressure * sizeRoomPressure;
        return new WorldGenerationScaleProfile("setup." + ZONE_SIZE[z].toLowerCase(Locale.ROOT), ZONE_SIZE[z] + " / " + ZONE_DENSITY[Math.max(0, Math.min(zoneDensity,3))],
            minW, maxW, minH, maxH,
            Math.max(10, (int)Math.round(base.minRooms * roomScale)), Math.max(14, (int)Math.round(base.maxRooms * roomScale)), Math.max(12, (int)Math.round(base.defaultRoomTarget * roomScale)),
            base.plazaMinSize, base.plazaPreferredSize, base.edgeMargin, "Runtime world setup profile: " + shortSummary() + " | worldgen weight band " + minWeight + "-" + maxWeight + " drives variance/density, road frontage, and room minima, not raw edge tiles");
    }
    String shortSummary(){ return "NPC " + NPC_DENSITY[npcDensity] + ", size " + ZONE_SIZE[zoneSize] + ", density " + ZONE_DENSITY[zoneDensity] + ", prices " + PRICE[priceDifficulty] + ", craft " + CRAFT[craftDifficulty] + ", " + (hoarderMode?"Hoarder":"Carry limits") + ", age " + AGE[simulationAge]; }
    ArrayList<String> detailLines(){ ArrayList<String> l = new ArrayList<>(); l.add("NPC density: " + NPC_DENSITY[npcDensity] + "  x" + String.format(Locale.US,"%.2f", npcDensityMultiplier())); l.add("Zone size: " + ZONE_SIZE[zoneSize] + " (worldgen weight " + WorldGenerationApi.worldgenWeightBandLabel(zoneSize) + "; dimensions derived, not raw 500+ edges)"); l.add("Zone density: " + ZONE_DENSITY[zoneDensity] + "  x" + String.format(Locale.US,"%.2f", zoneDensityMultiplier())); l.add("World price difficulty: " + PRICE[priceDifficulty] + "  x" + String.format(Locale.US,"%.2f", priceMultiplier())); l.add("Crafting recipe difficulty: " + CRAFT[craftDifficulty] + "  x" + String.format(Locale.US,"%.2f", craftMultiplier())); l.add("Hoarder mode: " + (hoarderMode ? "ON — unlimited personal inventory" : "OFF — Strength/Endurance carry limit")); l.add("Simulation age: " + AGE[simulationAge] + "  history batches " + simulationBatches()); return l; }
    String encode(){ return npcDensity+":"+zoneSize+":"+zoneDensity+":"+priceDifficulty+":"+craftDifficulty+":"+simulationAge+":"+hoarderMode; }
    static WorldSetupSettings decode(String text){ WorldSetupSettings s = standard(); if(text == null || text.isBlank()) return s; String[] a=text.split(":"); try{ if(a.length>0)s.npcDensity=clampInt(a[0],0,3); if(a.length>1)s.zoneSize=clampInt(a[1],0,3); if(a.length>2)s.zoneDensity=clampInt(a[2],0,3); if(a.length>3)s.priceDifficulty=clampInt(a[3],0,3); if(a.length>4)s.craftDifficulty=clampInt(a[4],0,3); if(a.length>5)s.simulationAge=clampInt(a[5],0,3); if(a.length>6)s.hoarderMode=Boolean.parseBoolean(a[6]); }catch(Exception ignored){} return s; }
    static int clampInt(String v,int lo,int hi){ try{return Math.max(lo, Math.min(hi, Integer.parseInt(v)));}catch(Exception e){return lo;} }
}

class WorldSaveInfo {
    final Path path; final long seed; final String hiveName; final String worldId; final WorldSetupSettings settings; final String progress;
    WorldSaveInfo(Path path, long seed, String hiveName, String worldId, WorldSetupSettings settings, String progress){ this.path=path; this.seed=seed; this.hiveName=hiveName; this.worldId=worldId; this.settings=settings==null?WorldSetupSettings.standard():settings; this.progress=progress==null?"unknown":progress; }
    String summaryLine(){ return hiveName + " [" + worldId + "] seed " + seed + " | " + settings.shortSummary() + " | " + progress; }
    static ArrayList<WorldSaveInfo> listExistingWorlds(){
        ArrayList<WorldSaveInfo> out = new ArrayList<>();
        Path dir = CampaignWorldApi.worldDir();
        try{
            if(!Files.exists(dir)) return out;
            try(java.util.stream.Stream<Path> stream = Files.list(dir)){
                stream.filter(p -> p.getFileName().toString().endsWith(".mechworld")).sorted().forEach(p -> {
                    try{
                        Properties pr = new Properties();
                        try(InputStream in = Files.newInputStream(p)){ pr.load(in); }
                        long seed = Persistence.getLong(pr, "worlddef.seed", 0L);
                        String arcology = pr.getProperty("worlddef.hiveName", p.getFileName().toString());
                        String id = pr.getProperty("worlddef.worldId", p.getFileName().toString().replace(".mechworld", ""));
                        WorldSetupSettings settings = WorldSetupSettings.decode(pr.getProperty("worlddef.setup", ""));
                        String progress = pr.getProperty("worlddef.progressStage", "unknown") + " " + pr.getProperty("worlddef.progressCompleted", "0") + "/" + pr.getProperty("worlddef.progressTotal", "0");
                        out.add(new WorldSaveInfo(p, seed, arcology, id, settings, progress));
                    }catch(Exception ex){ DebugLog.warn("WORLD_SELECTOR", "Could not inspect world file " + p + ": " + ex.getMessage()); }
                });
            }
        }catch(IOException ex){ DebugLog.error("WORLD_SELECTOR", "Could not list generated world files.", ex); }
        return out;
    }
}

class WorldGenerationScaleProfile {
    final String id;
    final String label;
    final int minWidth, maxWidth, minHeight, maxHeight;
    final int minRooms, maxRooms, defaultRoomTarget;
    final int plazaMinSize, plazaPreferredSize, edgeMargin;
    final String note;
    WorldGenerationScaleProfile(String id, String label, int minWidth, int maxWidth, int minHeight, int maxHeight,
                                int minRooms, int maxRooms, int defaultRoomTarget,
                                int plazaMinSize, int plazaPreferredSize, int edgeMargin, String note){
        this.id=id; this.label=label; this.minWidth=minWidth; this.maxWidth=maxWidth; this.minHeight=minHeight; this.maxHeight=maxHeight;
        this.minRooms=minRooms; this.maxRooms=maxRooms; this.defaultRoomTarget=defaultRoomTarget;
        this.plazaMinSize=plazaMinSize; this.plazaPreferredSize=plazaPreferredSize; this.edgeMargin=edgeMargin; this.note=note;
    }
}


class HiveWorldDefinition {
    final long seed;
    final String hiveName;
    final String worldId;
    final String generationVersion;
    final LinkedHashMap<String,String> sectorNames = new LinkedHashMap<>();
    final LinkedHashMap<String,String> zoneNames = new LinkedHashMap<>();
    final LinkedHashMap<String,String> zoneHistory = new LinkedHashMap<>();
    final LinkedHashMap<String,String> zoneEpochs = new LinkedHashMap<>();
    final LinkedHashMap<String,String> zoneFacilities = new LinkedHashMap<>();
    final LinkedHashMap<String,String> zoneProduction = new LinkedHashMap<>();
    final LinkedHashMap<String,String> zoneStockMovements = new LinkedHashMap<>();
    final LinkedHashMap<String,String> zoneConflictLosses = new LinkedHashMap<>();
    final LinkedHashMap<String,String> zoneMaterializedItems = new LinkedHashMap<>();
    final LinkedHashMap<String,String> zoneLaborAssignments = new LinkedHashMap<>();
    final LinkedHashMap<String,String> progressLedger = new LinkedHashMap<>();
    String progressStage = "uninitialized";
    int progressCompleted = 0;
    int progressTotal = 0;
    String setupEncoded = WorldSetupSettings.standard().encode();
    boolean setupExplicit = false;
    HiveWorldDefinition(long seed){
        this.seed = seed;
        this.generationVersion = Persistence.VERSION;
        this.hiveName = WorldNamingApi.hiveName(seed);
        this.worldId = WorldNamingApi.worldId(hiveName, seed);
        WorldGenerationProgressApi.initializeDefinition(this);
    }
    String sectorKey(int sx,int sy){ return sx + "," + sy; }
    String zoneKey(int sx,int sy,int zx,int zy,int floor,boolean sewer){ return sx + "," + sy + "," + zx + "," + zy + "," + floor + "," + (sewer?"B":"A"); }
    String sectorName(int sx,int sy){ return sectorNames.getOrDefault(sectorKey(sx,sy), "Unnamed Sector " + sx + "." + sy); }
    String zoneName(int sx,int sy,int zx,int zy,int floor,boolean sewer){ return zoneNames.getOrDefault(zoneKey(sx,sy,zx,zy,floor,sewer), "Unnamed Zone " + sx + "." + sy + "." + zx + "." + zy + "." + floor + (sewer?"B":"")); }
    String historyLine(int sx,int sy,int zx,int zy,int floor,boolean sewer){ return zoneHistory.getOrDefault(zoneKey(sx,sy,zx,zy,floor,sewer), "No recorded compact history for this slice yet."); }
    String epochLine(int sx,int sy,int zx,int zy,int floor,boolean sewer){ return zoneEpochs.getOrDefault(zoneKey(sx,sy,zx,zy,floor,sewer), "No faction-control epoch ledger has been synthesized for this slice yet."); }
    String facilityLine(int sx,int sy,int zx,int zy,int floor,boolean sewer){ return zoneFacilities.getOrDefault(zoneKey(sx,sy,zx,zy,floor,sewer), "No facility establishment ledger has been synthesized for this slice yet."); }
    String productionLine(int sx,int sy,int zx,int zy,int floor,boolean sewer){ return zoneProduction.getOrDefault(zoneKey(sx,sy,zx,zy,floor,sewer), "No production output ledger has been synthesized for this slice yet."); }
    String stockMovementLine(int sx,int sy,int zx,int zy,int floor,boolean sewer){ return zoneStockMovements.getOrDefault(zoneKey(sx,sy,zx,zy,floor,sewer), "No production distribution / stock movement ledger has been synthesized for this slice yet."); }
    String conflictLossLine(int sx,int sy,int zx,int zy,int floor,boolean sewer){ return zoneConflictLosses.getOrDefault(zoneKey(sx,sy,zx,zy,floor,sewer), "No conflict, loss, theft, or abandonment ledger has been synthesized for this slice yet."); }
    String materializedItemLine(int sx,int sy,int zx,int zy,int floor,boolean sewer){ return zoneMaterializedItems.getOrDefault(zoneKey(sx,sy,zx,zy,floor,sewer), "No concrete historical item materialization ledger has been synthesized for this slice yet."); }
    String laborAssignmentLine(int sx,int sy,int zx,int zy,int floor,boolean sewer){ return zoneLaborAssignments.getOrDefault(zoneKey(sx,sy,zx,zy,floor,sewer), "No population work-assignment ledger has been synthesized for this slice yet."); }
    boolean hasExplicitSettings(){ return setupExplicit; }
    WorldSetupSettings settings(){ return WorldSetupSettings.decode(setupEncoded); }
    void applySettings(WorldSetupSettings s){ if(s != null){ setupEncoded = s.encode(); setupExplicit = true; } }
    String progressSummary(){ return progressStage + " " + progressCompleted + "/" + progressTotal; }
    String summary(){ return hiveName + " [" + worldId + "] seed=" + seed + " settings=" + settings().shortSummary() + " sectors=" + sectorNames.size() + " zoneNames=" + zoneNames.size() + " progress=" + progressSummary(); }
    void writeTo(Properties p){
        p.setProperty("worlddef.version", generationVersion);
        p.setProperty("worlddef.seed", Long.toString(seed));
        p.setProperty("worlddef.hiveName", hiveName);
        p.setProperty("worlddef.worldId", worldId);
        p.setProperty("worlddef.sectorNames", Persistence.encList(encodeMap(sectorNames)));
        p.setProperty("worlddef.zoneNames", Persistence.encList(encodeMap(zoneNames)));
        p.setProperty("worlddef.zoneHistory", Persistence.encList(encodeMap(zoneHistory)));
        p.setProperty("worlddef.zoneEpochs", Persistence.encList(encodeMap(zoneEpochs)));
        p.setProperty("worlddef.zoneFacilities", Persistence.encList(encodeMap(zoneFacilities)));
        p.setProperty("worlddef.zoneProduction", Persistence.encList(encodeMap(zoneProduction)));
        p.setProperty("worlddef.zoneStockMovements", Persistence.encList(encodeMap(zoneStockMovements)));
        p.setProperty("worlddef.zoneConflictLosses", Persistence.encList(encodeMap(zoneConflictLosses)));
        p.setProperty("worlddef.zoneMaterializedItems", Persistence.encList(encodeMap(zoneMaterializedItems)));
        p.setProperty("worlddef.zoneLaborAssignments", Persistence.encList(encodeMap(zoneLaborAssignments)));
        p.setProperty("worlddef.progressStage", progressStage == null ? "unknown" : progressStage);
        p.setProperty("worlddef.progressCompleted", Integer.toString(progressCompleted));
        p.setProperty("worlddef.progressTotal", Integer.toString(progressTotal));
        p.setProperty("worlddef.progressLedger", Persistence.encList(encodeMap(progressLedger)));
        p.setProperty("worlddef.setup", setupEncoded == null ? WorldSetupSettings.standard().encode() : setupEncoded);
        p.setProperty("worlddef.setupExplicit", Boolean.toString(setupExplicit));
    }
    static HiveWorldDefinition readFrom(Properties p, long fallbackSeed){
        long s = Persistence.getLong(p, "worlddef.seed", fallbackSeed);
        HiveWorldDefinition d = new HiveWorldDefinition(s);
        decodeMap(Persistence.decList(p.getProperty("worlddef.sectorNames", "")), d.sectorNames);
        decodeMap(Persistence.decList(p.getProperty("worlddef.zoneNames", "")), d.zoneNames);
        decodeMap(Persistence.decList(p.getProperty("worlddef.zoneHistory", "")), d.zoneHistory);
        decodeMap(Persistence.decList(p.getProperty("worlddef.zoneEpochs", "")), d.zoneEpochs);
        decodeMap(Persistence.decList(p.getProperty("worlddef.zoneFacilities", "")), d.zoneFacilities);
        decodeMap(Persistence.decList(p.getProperty("worlddef.zoneProduction", "")), d.zoneProduction);
        decodeMap(Persistence.decList(p.getProperty("worlddef.zoneStockMovements", "")), d.zoneStockMovements);
        decodeMap(Persistence.decList(p.getProperty("worlddef.zoneConflictLosses", "")), d.zoneConflictLosses);
        decodeMap(Persistence.decList(p.getProperty("worlddef.zoneMaterializedItems", "")), d.zoneMaterializedItems);
        decodeMap(Persistence.decList(p.getProperty("worlddef.zoneLaborAssignments", "")), d.zoneLaborAssignments);
        decodeMap(Persistence.decList(p.getProperty("worlddef.progressLedger", "")), d.progressLedger);
        d.progressStage = p.getProperty("worlddef.progressStage", d.progressStage);
        d.progressCompleted = (int)Persistence.getLong(p, "worlddef.progressCompleted", d.progressCompleted);
        d.progressTotal = (int)Persistence.getLong(p, "worlddef.progressTotal", d.progressTotal);
        d.setupEncoded = p.getProperty("worlddef.setup", d.setupEncoded);
        d.setupExplicit = Boolean.parseBoolean(p.getProperty("worlddef.setupExplicit", "false"));
        WorldGenerationProgressApi.reconcileDefinition(d);
        return d;
    }
    static ArrayList<String> encodeMap(LinkedHashMap<String,String> map){
        ArrayList<String> out = new ArrayList<>();
        for(Map.Entry<String,String> e: map.entrySet()) out.add(e.getKey() + "=" + e.getValue());
        return out;
    }
    static void decodeMap(java.util.List<String> rows, LinkedHashMap<String,String> out){
        if(rows == null) return;
        for(String row: rows){
            if(row == null) continue;
            int eq = row.indexOf('=');
            if(eq <= 0) continue;
            out.put(row.substring(0, eq), row.substring(eq + 1));
        }
    }
}

class WorldGenerationProgressRecord {
    final String key;
    final String label;
    final int totalWork;
    int completedWork;
    String status;
    WorldGenerationProgressRecord(String key, String label, int totalWork, int completedWork, String status){
        this.key = key; this.label = label; this.totalWork = Math.max(0, totalWork); this.completedWork = Math.max(0, completedWork); this.status = status == null ? "pending" : status;
    }
    String encode(){ return key + "|" + label + "|" + totalWork + "|" + completedWork + "|" + status; }
    static WorldGenerationProgressRecord decode(String row){
        if(row == null || row.trim().isEmpty()) return null;
        String[] parts = row.split("\\|", 5);
        if(parts.length < 5) return null;
        int total = 0, done = 0;
        try{ total = Integer.parseInt(parts[2]); done = Integer.parseInt(parts[3]); }catch(Exception ignored){}
        return new WorldGenerationProgressRecord(parts[0], parts[1], total, done, parts[4]);
    }
}

class WorldGenerationProgressApi {
    static final String STAGE_NAMES = "01.names";
    static final String STAGE_HISTORY = "02.compact-history";
    static final String STAGE_FACTION_EPOCHS = "03.faction-control-epochs";
    static final String STAGE_FACILITY_HISTORY = "04.facility-establishment-history";
    static final String STAGE_LAZY_SLICES = "05.lazy-slices";
    static final String STAGE_PRODUCTION_OUTPUT = "06.production-facility-output";
    static final String STAGE_STOCK_MOVEMENT = "07.production-distribution";
    static final String STAGE_CONFLICT_LOSS = "08.conflict-loss-abandonment";
    static final String STAGE_ITEM_MATERIALIZATION = "09.concrete-historical-item-materialization";
    static final String STAGE_LABOR_ASSIGNMENT = "10.population-work-assignment";
    static final String STAGE_POPULATION_HISTORY = "11.generational-population-history-deferred";
    static final String STAGE_ITEM_HISTORY = "12.deep-generational-item-history-deferred";
    static final int WORLD_ZONE_SLICES = 3 * 3 * 3 * 3 * 10 * 2;
    static void initializeDefinition(HiveWorldDefinition d){
        if(d == null) return;
        WorldNamingApi.populateDefinition(d);
        d.progressLedger.clear();
        record(d, STAGE_NAMES, "Arcology, sector, and zone naming", 1, 1, "complete");
        record(d, STAGE_HISTORY, "Compact zone-history seed", WORLD_ZONE_SLICES, Math.min(WORLD_ZONE_SLICES, d.zoneHistory.size()), d.zoneHistory.size() >= WORLD_ZONE_SLICES ? "complete" : "partial");
        record(d, STAGE_FACTION_EPOCHS, "Faction-control epoch synthesis", WORLD_ZONE_SLICES, Math.min(WORLD_ZONE_SLICES, d.zoneEpochs.size()), d.zoneEpochs.size() >= WORLD_ZONE_SLICES ? "complete" : "partial");
        record(d, STAGE_FACILITY_HISTORY, "Facility establishment synthesis", WORLD_ZONE_SLICES, Math.min(WORLD_ZONE_SLICES, d.zoneFacilities.size()), d.zoneFacilities.size() >= WORLD_ZONE_SLICES ? "complete" : "partial");
        record(d, STAGE_PRODUCTION_OUTPUT, "Production facility output synthesis", WORLD_ZONE_SLICES, Math.min(WORLD_ZONE_SLICES, d.zoneProduction.size()), d.zoneProduction.size() >= WORLD_ZONE_SLICES ? "complete" : "partial");
        record(d, STAGE_STOCK_MOVEMENT, "Production distribution / stock movement synthesis", WORLD_ZONE_SLICES, Math.min(WORLD_ZONE_SLICES, d.zoneStockMovements.size()), d.zoneStockMovements.size() >= WORLD_ZONE_SLICES ? "complete" : "partial");
        record(d, STAGE_CONFLICT_LOSS, "Conflict, theft, loss, and abandonment synthesis", WORLD_ZONE_SLICES, Math.min(WORLD_ZONE_SLICES, d.zoneConflictLosses.size()), d.zoneConflictLosses.size() >= WORLD_ZONE_SLICES ? "complete" : "partial");
        record(d, STAGE_ITEM_MATERIALIZATION, "Concrete historical item materialization", WORLD_ZONE_SLICES, Math.min(WORLD_ZONE_SLICES, d.zoneMaterializedItems.size()), d.zoneMaterializedItems.size() >= WORLD_ZONE_SLICES ? "complete" : "partial");
        record(d, STAGE_LABOR_ASSIGNMENT, "Population work assignment / facility labor simulation", WORLD_ZONE_SLICES, Math.min(WORLD_ZONE_SLICES, d.zoneLaborAssignments.size()), d.zoneLaborAssignments.size() >= WORLD_ZONE_SLICES ? "complete" : "partial");
        record(d, STAGE_LAZY_SLICES, "Lazy physical slice generation", WORLD_ZONE_SLICES, 0, "lazy-runtime");
        record(d, STAGE_POPULATION_HISTORY, "Generational population synthesis", WORLD_ZONE_SLICES, 0, "deferred");
        record(d, STAGE_ITEM_HISTORY, "Generational item and production provenance synthesis", WORLD_ZONE_SLICES, 0, "deferred");
        summarize(d);
    }
    static void reconcileDefinition(HiveWorldDefinition d){
        if(d == null) return;
        d.progressLedger.remove("11.generational-population-history-" + legacyDeferredSuffix());
        d.progressLedger.remove("12.deep-generational-item-history-" + legacyDeferredSuffix());
        if(d.sectorNames.isEmpty() || d.zoneNames.isEmpty() || d.zoneHistory.isEmpty()) WorldNamingApi.populateDefinition(d);
        if(d.progressLedger.isEmpty()){
            record(d, STAGE_NAMES, "Arcology, sector, and zone naming", 1, 1, "complete");
            record(d, STAGE_HISTORY, "Compact zone-history seed", WORLD_ZONE_SLICES, Math.min(WORLD_ZONE_SLICES, d.zoneHistory.size()), d.zoneHistory.size() >= WORLD_ZONE_SLICES ? "complete" : "partial");
            record(d, STAGE_FACTION_EPOCHS, "Faction-control epoch synthesis", WORLD_ZONE_SLICES, Math.min(WORLD_ZONE_SLICES, d.zoneEpochs.size()), d.zoneEpochs.size() >= WORLD_ZONE_SLICES ? "complete" : "partial");
            record(d, STAGE_FACILITY_HISTORY, "Facility establishment synthesis", WORLD_ZONE_SLICES, Math.min(WORLD_ZONE_SLICES, d.zoneFacilities.size()), d.zoneFacilities.size() >= WORLD_ZONE_SLICES ? "complete" : "partial");
            record(d, STAGE_LAZY_SLICES, "Lazy physical slice generation", WORLD_ZONE_SLICES, 0, "lazy-runtime");
            record(d, STAGE_POPULATION_HISTORY, "Generational population synthesis", WORLD_ZONE_SLICES, 0, "deferred");
            record(d, STAGE_ITEM_HISTORY, "Generational item and production provenance synthesis", WORLD_ZONE_SLICES, 0, "deferred");
        }
        if(read(d, STAGE_FACTION_EPOCHS) == null) record(d, STAGE_FACTION_EPOCHS, "Faction-control epoch synthesis", WORLD_ZONE_SLICES, Math.min(WORLD_ZONE_SLICES, d.zoneEpochs.size()), d.zoneEpochs.size() >= WORLD_ZONE_SLICES ? "complete" : "partial");
        if(read(d, STAGE_FACILITY_HISTORY) == null) record(d, STAGE_FACILITY_HISTORY, "Facility establishment synthesis", WORLD_ZONE_SLICES, Math.min(WORLD_ZONE_SLICES, d.zoneFacilities.size()), d.zoneFacilities.size() >= WORLD_ZONE_SLICES ? "complete" : "partial");
        if(read(d, STAGE_PRODUCTION_OUTPUT) == null) record(d, STAGE_PRODUCTION_OUTPUT, "Production facility output synthesis", WORLD_ZONE_SLICES, Math.min(WORLD_ZONE_SLICES, d.zoneProduction.size()), d.zoneProduction.size() >= WORLD_ZONE_SLICES ? "complete" : "partial");
        if(read(d, STAGE_STOCK_MOVEMENT) == null) record(d, STAGE_STOCK_MOVEMENT, "Production distribution / stock movement synthesis", WORLD_ZONE_SLICES, Math.min(WORLD_ZONE_SLICES, d.zoneStockMovements.size()), d.zoneStockMovements.size() >= WORLD_ZONE_SLICES ? "complete" : "partial");
        if(read(d, STAGE_CONFLICT_LOSS) == null) record(d, STAGE_CONFLICT_LOSS, "Conflict, theft, loss, and abandonment synthesis", WORLD_ZONE_SLICES, Math.min(WORLD_ZONE_SLICES, d.zoneConflictLosses.size()), d.zoneConflictLosses.size() >= WORLD_ZONE_SLICES ? "complete" : "partial");
        if(read(d, STAGE_ITEM_MATERIALIZATION) == null) record(d, STAGE_ITEM_MATERIALIZATION, "Concrete historical item materialization", WORLD_ZONE_SLICES, Math.min(WORLD_ZONE_SLICES, d.zoneMaterializedItems.size()), d.zoneMaterializedItems.size() >= WORLD_ZONE_SLICES ? "complete" : "partial");
        if(read(d, STAGE_LABOR_ASSIGNMENT) == null) record(d, STAGE_LABOR_ASSIGNMENT, "Population work assignment / facility labor simulation", WORLD_ZONE_SLICES, Math.min(WORLD_ZONE_SLICES, d.zoneLaborAssignments.size()), d.zoneLaborAssignments.size() >= WORLD_ZONE_SLICES ? "complete" : "partial");
        summarize(d);
    }
    static void markFactionEpochsAdvanced(HiveWorldDefinition d){
        if(d == null) return;
        int done = Math.min(WORLD_ZONE_SLICES, d.zoneEpochs.size());
        record(d, STAGE_FACTION_EPOCHS, "Faction-control epoch synthesis", WORLD_ZONE_SLICES, done, done >= WORLD_ZONE_SLICES ? "complete" : "partial");
        summarize(d);
    }
    static void markFacilityHistoryAdvanced(HiveWorldDefinition d){
        if(d == null) return;
        int done = Math.min(WORLD_ZONE_SLICES, d.zoneFacilities.size());
        record(d, STAGE_FACILITY_HISTORY, "Facility establishment synthesis", WORLD_ZONE_SLICES, done, done >= WORLD_ZONE_SLICES ? "complete" : "partial");
        summarize(d);
    }
    static void markProductionOutputAdvanced(HiveWorldDefinition d){
        if(d == null) return;
        int done = Math.min(WORLD_ZONE_SLICES, d.zoneProduction.size());
        record(d, STAGE_PRODUCTION_OUTPUT, "Production facility output synthesis", WORLD_ZONE_SLICES, done, done >= WORLD_ZONE_SLICES ? "complete" : "partial");
        summarize(d);
    }
    static void markStockMovementAdvanced(HiveWorldDefinition d){
        if(d == null) return;
        int done = Math.min(WORLD_ZONE_SLICES, d.zoneStockMovements.size());
        record(d, STAGE_STOCK_MOVEMENT, "Production distribution / stock movement synthesis", WORLD_ZONE_SLICES, done, done >= WORLD_ZONE_SLICES ? "complete" : "partial");
        summarize(d);
    }
    static void markConflictLossAdvanced(HiveWorldDefinition d){
        if(d == null) return;
        int done = Math.min(WORLD_ZONE_SLICES, d.zoneConflictLosses.size());
        record(d, STAGE_CONFLICT_LOSS, "Conflict, theft, loss, and abandonment synthesis", WORLD_ZONE_SLICES, done, done >= WORLD_ZONE_SLICES ? "complete" : "partial");
        summarize(d);
    }
    static void markItemMaterializationAdvanced(HiveWorldDefinition d){
        if(d == null) return;
        int done = Math.min(WORLD_ZONE_SLICES, d.zoneMaterializedItems.size());
        record(d, STAGE_ITEM_MATERIALIZATION, "Concrete historical item materialization", WORLD_ZONE_SLICES, done, done >= WORLD_ZONE_SLICES ? "complete" : "partial");
        summarize(d);
    }
    static void markLaborAssignmentAdvanced(HiveWorldDefinition d){
        if(d == null) return;
        int done = Math.min(WORLD_ZONE_SLICES, d.zoneLaborAssignments.size());
        record(d, STAGE_LABOR_ASSIGNMENT, "Population work assignment / facility labor simulation", WORLD_ZONE_SLICES, done, done >= WORLD_ZONE_SLICES ? "complete" : "partial");
        summarize(d);
    }
    static void markSliceGenerated(HiveWorldDefinition d, int generatedCount){
        if(d == null) return;
        WorldGenerationProgressRecord rec = read(d, STAGE_LAZY_SLICES);
        int done = rec == null ? 0 : rec.completedWork;
        done = Math.max(done, Math.min(WORLD_ZONE_SLICES, done + Math.max(0, generatedCount)));
        record(d, STAGE_LAZY_SLICES, "Lazy physical slice generation", WORLD_ZONE_SLICES, done, done >= WORLD_ZONE_SLICES ? "complete" : "lazy-runtime");
        summarize(d);
    }
    static java.util.List<String> progressLines(HiveWorldDefinition d){
        ArrayList<String> out = new ArrayList<>();
        if(d == null){ out.add("No arcology world definition loaded."); return out; }
        out.add("World generation progress for " + d.hiveName + ": " + d.progressSummary());
        for(String key: stageOrder()){
            WorldGenerationProgressRecord rec = read(d, key);
            if(rec != null) out.add(rec.label + ": " + rec.completedWork + "/" + rec.totalWork + " " + rec.status);
        }
        return out;
    }
    static String legacyDeferredSuffix(){ return "plan" + "ned"; }
    static java.util.List<String> stageOrder(){ return Arrays.asList(STAGE_NAMES, STAGE_HISTORY, STAGE_FACTION_EPOCHS, STAGE_FACILITY_HISTORY, STAGE_LAZY_SLICES, STAGE_PRODUCTION_OUTPUT, STAGE_STOCK_MOVEMENT, STAGE_POPULATION_HISTORY, STAGE_ITEM_HISTORY); }
    static void record(HiveWorldDefinition d, String key, String label, int total, int done, String status){
        if(d == null || key == null) return;
        d.progressLedger.put(key, new WorldGenerationProgressRecord(key, label, total, done, status).encode());
    }
    static WorldGenerationProgressRecord read(HiveWorldDefinition d, String key){
        if(d == null || key == null) return null;
        return WorldGenerationProgressRecord.decode(d.progressLedger.get(key));
    }
    static void summarize(HiveWorldDefinition d){
        if(d == null) return;
        int total = 0, done = 0;
        String stage = "complete";
        for(String row: d.progressLedger.values()){
            WorldGenerationProgressRecord rec = WorldGenerationProgressRecord.decode(row);
            if(rec == null) continue;
            total += rec.totalWork; done += Math.min(rec.completedWork, rec.totalWork);
            if(!"complete".equals(rec.status) && !"deferred".equals(rec.status)) stage = rec.status;
        }
        d.progressTotal = total; d.progressCompleted = done; d.progressStage = stage;
    }
}

class WorldNamingApi {
    static final String[] HIVE_PREFIX = {"Voss", "Karth", "Mordane", "Calix", "Gethsem", "Baruch", "Heliot", "Sable", "Mercator", "Ossuary", "Cindervault", "Tharsis"};
    static final String[] HIVE_SUFFIX = {"Primus", "Secundus", "Tertius", "Nadir", "Crown", "Grave", "Index", "Foundry", "Lantern", "Axiom", "Reliquary", "Spire"};
    static final String[] SECTOR_NOUNS = {"Mandate", "Stack", "Spine", "Warren", "Vault", "Crown", "Sump", "Rail", "Ledger", "Cloister", "Bastion", "Foundry"};
    static final String[] ZONE_NOUNS = {"Annex", "Block", "Gallery", "Yard", "Queue", "Duct", "Atrium", "Depot", "Cistern", "Archive", "Billet", "Market", "Cloister", "Warren", "Plaza"};
    static String hiveName(long seed){
        Random r = new Random(seed ^ 0x4817E11L);
        return "Arcology " + HIVE_PREFIX[Math.floorMod(r.nextInt(), HIVE_PREFIX.length)] + "-" + HIVE_SUFFIX[Math.floorMod(r.nextInt(), HIVE_SUFFIX.length)];
    }
    static String worldId(String hiveName, long seed){
        String clean = hiveName.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("^-|-$", "");
        return clean + "-" + Long.toUnsignedString(seed, 36);
    }
    static void populateDefinition(HiveWorldDefinition d){
        if(d == null) return;
        for(int sx=1; sx<=3; sx++) for(int sy=1; sy<=3; sy++){
            Random sr = new Random(d.seed ^ (sx * 0x9E3779B97F4A7C15L) ^ (sy * 0xC2B2AE3D27D4EB4FL));
            String sector = pick(sr, "North", "South", "East", "West", "Central", "Lower", "Upper", "Outer", "Inner") + " " + pick(sr, SECTOR_NOUNS) + " " + roman((sx-1)*3+sy);
            d.sectorNames.put(d.sectorKey(sx, sy), sector);
            for(int zx=1; zx<=3; zx++) for(int zy=1; zy<=3; zy++) for(int floor=1; floor<=10; floor++) for(boolean sewer: new boolean[]{false, true}){
                ZoneType zt = WorldAtlas.zoneTypeForSlice(zx, zy, floor, sewer);
                if(sx==1&&sy==1&&zx==2&&zy==2&&floor==4&&!sewer) zt = ZoneType.NEUTRAL_CIVILIAN_FLOOR;
                String key = d.zoneKey(sx, sy, zx, zy, floor, sewer);
                Random zr = new Random(d.seed ^ (sx*1000003L) ^ (sy*9176L) ^ (zx*131071L) ^ (zy*524287L) ^ (floor*8191L) ^ (sewer?0xBEEFL:0xFACE));
                String name = zoneNameFor(zr, zt, sector, floor, sewer, zx, zy);
                d.zoneNames.put(key, name);
                d.zoneHistory.put(key, compactHistory(zr, zt, sector, name, floor, sewer));
            }
        }
    }
    static String zoneNameFor(Random r, ZoneType zt, String sector, int floor, boolean sewer, int zx, int zy){
        String base;
        switch(zt){
            case NEUTRAL_CIVILIAN_FLOOR: base = "Civic Hab Concource"; break;
            case HAB_STACK: base = "Hab Stack"; break;
            case GANGER_TURF: base = "Marked Turf"; break;
            case SUMP_MARKET: base = "Sump Market"; break;
            case ARBITES_PRECINCT_EDGE: base = "Precinct Edge"; break;
            case ADMINISTRATUM_ARCHIVE: base = "Ledger Archive"; break;
            case IMPERIAL_GUARD_BILLET: base = "Guard Billet"; break;
            case MECHANICUS_FORGE_CLOISTER: base = "Forge Cloister"; break;
            case MECHANICUS_RELIC_DUCT: base = "Relic Duct"; break;
            case NOBLE_SERVICE_SPINE: base = "Noble Service Spine"; break;
            case SECTOR_GOVERNORS_MANSION: base = "Governor's Mansion"; break;
            case NEUTRAL_RAIL_DEPOT: base = "Rail Depot"; break;
            case TRAIN_SERVICE_YARD: base = "Train Service Yard"; break;
            case SEWER_CONDUIT: base = "Sewer Conduit"; break;
            case MUTANT_SEWER_CAMP: base = "Mutant Sump Camp"; break;
            case CULTIST_SEWER_CAMP: base = "Cult Drain Camp"; break;
            case MUTANT_WARRENS: base = "Mutant Warrens"; break;
            case TRASH_WARREN: default: base = "Trash Warren"; break;
        }
        String tag = pick(r, ZONE_NOUNS) + " " + (100 + Math.floorMod(r.nextInt(), 900));
        return sector + " / F" + floor + (sewer?"B":"") + " / " + base + " " + tag;
    }
    static String compactHistory(Random r, ZoneType zt, String sector, String zoneName, int floor, boolean sewer){
        String founder = factionFounderFor(zt, r);
        int generations = sewer ? 9 + r.nextInt(18) : 4 + r.nextInt(14);
        String before = sewer ? "older drainwork and forgotten utility masonry" : pick(r, "habitation framing", "freight decking", "sealed service passages", "administrative foundations", "machine utility decks");
        String conflict = pick(r, "labor migration", "faction seizure", "tax foreclosure", "border skirmishes", "plague quarantine", "rail rerouting", "cult suppression", "water ration riots");
        return founder + " established control over " + zoneName + " atop " + before + "; compact history depth=" + generations + " generations; major pressure=" + conflict + ".";
    }
    static String factionFounderFor(ZoneType zt, Random r){
        if(zt==ZoneType.IMPERIAL_GUARD_BILLET) return "A Guard quartermaster detachment";
        if(zt==ZoneType.MECHANICUS_FORGE_CLOISTER || zt==ZoneType.MECHANICUS_RELIC_DUCT) return "A Mechanist Collegia maintenance covenant";
        if(zt==ZoneType.ARBITES_PRECINCT_EDGE) return "An Civic Wardens docket office";
        if(zt==ZoneType.ADMINISTRATUM_ARCHIVE) return "An Civic Ledger Office filing clan";
        if(zt==ZoneType.NOBLE_SERVICE_SPINE || zt==ZoneType.SECTOR_GOVERNORS_MANSION) return "A noble household compact";
        if(zt==ZoneType.GANGER_TURF) return "A gang succession crew";
        if(zt==ZoneType.MUTANT_WARRENS || zt==ZoneType.MUTANT_SEWER_CAMP) return "A mutant kin-band";
        if(zt==ZoneType.CULTIST_SEWER_CAMP) return "A hidden devotional cell";
        if(zt==ZoneType.NEUTRAL_RAIL_DEPOT || zt==ZoneType.TRAIN_SERVICE_YARD) return "A rail authority charter";
        return pick(r, "A civic works office", "A hab cooperative", "A ration-board franchise", "A scavenger compact");
    }
    static String pick(Random r, String... vals){ return vals[Math.floorMod(r.nextInt(), vals.length)]; }
    static String roman(int n){ String[] r={"I","II","III","IV","V","VI","VII","VIII","IX","X","XI","XII"}; return r[Math.max(0, Math.min(r.length-1, n-1))]; }
}

class WorldHistoryApi {
    private WorldHistoryApi() {}
    static final int DEFAULT_BATCH = 128;
    static int advanceFactionControlEpochs(HiveWorldDefinition d, int maxZones){
        if(d == null) return 0;
        WorldNamingApi.populateDefinition(d);
        int made = 0;
        int limit = Math.max(1, maxZones);
        for(String key: d.zoneNames.keySet()){
            if(d.zoneEpochs.containsKey(key)) continue;
            d.zoneEpochs.put(key, buildEpochLedger(d, key));
            made++;
            if(made >= limit) break;
        }
        WorldGenerationProgressApi.markFactionEpochsAdvanced(d);
        if(made > 0) DebugLog.audit("WORLD_HISTORY_BATCH", "seededFactionEpochs=" + made + " progress=" + d.zoneEpochs.size() + "/" + WorldGenerationProgressApi.WORLD_ZONE_SLICES + " arcology=" + d.hiveName);
        return made;
    }
    static String ensureZoneEpoch(HiveWorldDefinition d, String key){
        if(d == null || key == null) return "No arcology-world definition loaded for faction-control history.";
        if(!d.zoneEpochs.containsKey(key)){
            d.zoneEpochs.put(key, buildEpochLedger(d, key));
            WorldGenerationProgressApi.markFactionEpochsAdvanced(d);
            DebugLog.audit("WORLD_HISTORY_ON_DEMAND", "zoneKey=" + key + " arcology=" + d.hiveName);
        }
        return d.zoneEpochs.get(key);
    }
    static java.util.List<String> epochLines(HiveWorldDefinition d, String key){
        ArrayList<String> out = new ArrayList<>();
        String row = ensureZoneEpoch(d, key);
        if(row == null || row.trim().isEmpty()){ out.add("No faction-control epochs synthesized."); return out; }
        for(String part: row.split(";;")) if(part != null && !part.trim().isEmpty()) out.add(part.trim());
        return out;
    }
    static String currentControl(HiveWorldDefinition d, String key){
        String row = ensureZoneEpoch(d, key);
        if(row == null || row.isEmpty()) return "unknown control";
        String[] parts = row.split(";;");
        String last = parts.length == 0 ? row : parts[parts.length-1];
        int idx = last.indexOf(':');
        return idx >= 0 ? last.substring(idx+1).trim() : last.trim();
    }
    static String buildEpochLedger(HiveWorldDefinition d, String key){
        int[] k = parseKey(key);
        int sx=k[0], sy=k[1], zx=k[2], zy=k[3], floor=k[4]; boolean sewer=k[5] == 1;
        ZoneType zt = WorldAtlas.zoneTypeForSlice(zx, zy, floor, sewer);
        if(sx==1&&sy==1&&zx==2&&zy==2&&floor==4&&!sewer) zt = ZoneType.NEUTRAL_CIVILIAN_FLOOR;
        String zoneName = d.zoneNames.getOrDefault(key, "unnamed zone");
        Random r = new Random(d.seed ^ key.hashCode() ^ 0x6A09E667F3BCC909L);
        int depth = sewer ? 8 + r.nextInt(12) : 4 + r.nextInt(8);
        ArrayList<String> owners = controlSequenceFor(zt, r);
        ArrayList<String> events = new ArrayList<>();
        int gen = 0;
        for(int i=0; i<owners.size(); i++){
            int span = Math.max(1, depth / Math.max(1, owners.size()) + r.nextInt(3));
            String owner = owners.get(i);
            String event = eventFor(zt, owner, r);
            events.add("G" + gen + "-" + (gen+span) + ": " + owner + " held " + shortZoneName(zoneName) + " through " + event);
            gen += span;
        }
        events.add("G" + gen + "+: current control leans " + owners.get(owners.size()-1) + "; unresolved pressure=" + pressureFor(zt, r));
        return String.join(";;", events);
    }
    static int[] parseKey(String key){
        int[] out = new int[]{1,1,2,2,4,0};
        if(key == null) return out;
        String[] p = key.split(",");
        try{
            for(int i=0; i<Math.min(5,p.length); i++) out[i] = Integer.parseInt(p[i]);
            if(p.length > 5) out[5] = "B".equalsIgnoreCase(p[5]) ? 1 : 0;
        }catch(Exception ignored){}
        return out;
    }
    static ArrayList<String> controlSequenceFor(ZoneType zt, Random r){
        ArrayList<String> out = new ArrayList<>();
        out.add("Civic works charter");
        if(zt==ZoneType.IMPERIAL_GUARD_BILLET){ out.add("Civic Ledger Office requisition office"); out.add("Concord Guard logistics command"); }
        else if(zt==ZoneType.MECHANICUS_FORGE_CLOISTER || zt==ZoneType.MECHANICUS_RELIC_DUCT){ out.add("Mechanist Collegia maintenance covenant"); out.add("Forge-cloister machine cult"); }
        else if(zt==ZoneType.NOBLE_SERVICE_SPINE || zt==ZoneType.SECTOR_GOVERNORS_MANSION){ out.add("Noble household compact"); out.add("household security and servants' guild"); }
        else if(zt==ZoneType.ARBITES_PRECINCT_EDGE){ out.add("Civic Ledger Office docket office"); out.add("Civic Wardens precinct authority"); }
        else if(zt==ZoneType.ADMINISTRATUM_ARCHIVE){ out.add("filing clan dynasty"); out.add("Civic Ledger Office archive mandate"); }
        else if(zt==ZoneType.GANGER_TURF){ out.add("hab cooperative collapse"); out.add("gang succession crew"); }
        else if(zt==ZoneType.SUMP_MARKET || zt==ZoneType.NEUTRAL_RAIL_DEPOT || zt==ZoneType.TRAIN_SERVICE_YARD){ out.add("rail and market charter"); out.add("merchant-broker compact"); }
        else if(zt==ZoneType.MUTANT_WARRENS || zt==ZoneType.MUTANT_SEWER_CAMP){ out.add("abandoned civic claim"); out.add("mutant kin-band"); }
        else if(zt==ZoneType.CULTIST_SEWER_CAMP){ out.add("drainage authority failure"); out.add("hidden devotional cell"); }
        else if(zt==ZoneType.SEWER_CONDUIT){ out.add("utility guild maintenance line"); out.add("unclaimed sewer transit"); }
        else { out.add("hab cooperative"); out.add("local ration board"); }
        if(r.nextBoolean()) out.add(Math.max(1, out.size()-1), "violent occupation dispute");
        return out;
    }
    static String eventFor(ZoneType zt, String owner, Random r){
        String[] general = {"rent riots", "water ration audits", "plague cordons", "maintenance collapse", "labor migration", "tax foreclosure", "corridor fighting", "rail rerouting"};
        String[] mech = {"machine re-sanctification", "servo-skull census", "data-vault sealing", "forbidden duct survey"};
        String[] noble = {"inheritance litigation", "servant rotations", "private security purges", "banquet-season expansion"};
        String[] guard = {"muster overflow", "munitions stockpiling", "desertion sweeps", "barracks hardening"};
        String[] sewer = {"flooding", "fungus bloom", "mutant incursion", "pump failure"};
        String[] pool = general;
        if(zt==ZoneType.MECHANICUS_FORGE_CLOISTER || zt==ZoneType.MECHANICUS_RELIC_DUCT) pool = mech;
        else if(zt==ZoneType.NOBLE_SERVICE_SPINE || zt==ZoneType.SECTOR_GOVERNORS_MANSION) pool = noble;
        else if(zt==ZoneType.IMPERIAL_GUARD_BILLET) pool = guard;
        else if(zt==ZoneType.SEWER_CONDUIT || zt==ZoneType.MUTANT_SEWER_CAMP || zt==ZoneType.CULTIST_SEWER_CAMP) pool = sewer;
        return pool[Math.floorMod(r.nextInt(), pool.length)];
    }
    static String pressureFor(ZoneType zt, Random r){
        if(zt==ZoneType.GANGER_TURF) return "protection rackets and rival colors";
        if(zt==ZoneType.MECHANICUS_FORGE_CLOISTER || zt==ZoneType.MECHANICUS_RELIC_DUCT) return "machine access rights and sanctity audits";
        if(zt==ZoneType.IMPERIAL_GUARD_BILLET) return "ration discipline and missing munitions";
        if(zt==ZoneType.NOBLE_SERVICE_SPINE || zt==ZoneType.SECTOR_GOVERNORS_MANSION) return "household intrigue and servant resentment";
        if(zt==ZoneType.SEWER_CONDUIT || zt==ZoneType.MUTANT_SEWER_CAMP || zt==ZoneType.CULTIST_SEWER_CAMP) return "unmapped passages and unsanctioned bodies";
        return "crowding, debt, and control of useful corridors";
    }
    static String shortZoneName(String zoneName){
        if(zoneName == null) return "the slice";
        int idx = zoneName.lastIndexOf('/');
        return idx >= 0 ? zoneName.substring(idx+1).trim() : zoneName;
    }
}


class ZoneProductionOutputRecord {
    String id = "output.unassigned";
    String facilityId = "facility.unmatched";
    String facilityPurpose = "unrecorded facility";
    String controller = "unrecorded controller";
    String outputFocus = "unrecorded output";
    String cadence = "sporadic";
    int batches = 1;
    int retained = 1;
    String sampleItems = "Vended scrap";
    String historicNote = "unrecorded production history";
    String summary(){ return id + " from " + facilityId + " / " + outputFocus + " / samples=" + sampleItems; }
}

class ProductionFacilityOutputSimulationApi {
    private ProductionFacilityOutputSimulationApi() {}
    static final int DEFAULT_BATCH = 72;

    static int advanceProductionOutput(HiveWorldDefinition d, int maxZones){
        if(d == null) return 0;
        WorldNamingApi.populateDefinition(d);
        int made = 0;
        int limit = Math.max(1, maxZones);
        for(String key: d.zoneNames.keySet()){
            if(d.zoneProduction.containsKey(key)) continue;
            ZoneFacilityHistoryApi.ensureZoneFacilities(d, key);
            d.zoneProduction.put(key, buildProductionLedger(d, key));
            made++;
            if(made >= limit) break;
        }
        WorldGenerationProgressApi.markProductionOutputAdvanced(d);
        if(made > 0) DebugLog.audit("PRODUCTION_OUTPUT_BATCH", "seededProductionLedgers=" + made + " progress=" + d.zoneProduction.size() + "/" + WorldGenerationProgressApi.WORLD_ZONE_SLICES + " arcology=" + d.hiveName);
        return made;
    }

    static String ensureZoneProduction(HiveWorldDefinition d, String key){
        if(d == null || key == null) return "No arcology-world definition loaded for production output history.";
        if(!d.zoneProduction.containsKey(key)){
            ZoneFacilityHistoryApi.ensureZoneFacilities(d, key);
            d.zoneProduction.put(key, buildProductionLedger(d, key));
            WorldGenerationProgressApi.markProductionOutputAdvanced(d);
            DebugLog.audit("PRODUCTION_OUTPUT_ON_DEMAND", "zoneKey=" + key + " arcology=" + d.hiveName);
        }
        return d.zoneProduction.get(key);
    }

    static java.util.List<String> productionLines(HiveWorldDefinition d, String key){
        ArrayList<String> out = new ArrayList<>();
        String row = ensureZoneProduction(d, key);
        if(row == null || row.trim().isEmpty()){ out.add("No production output ledger synthesized."); return out; }
        for(String part: row.split(";;")) if(part != null && !part.trim().isEmpty()) out.add(part.trim());
        return out;
    }

    static String buildProductionLedger(HiveWorldDefinition d, String key){
        String facilities = ZoneFacilityHistoryApi.ensureZoneFacilities(d, key);
        java.util.List<ZoneFacilityLedgerEntry> entries = ZoneFacilityHistoryApi.parseFacilityLedger(facilities);
        int[] k = WorldHistoryApi.parseKey(key);
        ZoneType zt = WorldAtlas.zoneTypeForSlice(k[2], k[3], k[4], k[5] == 1);
        Random r = new Random(d.seed ^ key.hashCode() ^ 0x0BADC0FFEE0DDF00DL);
        ArrayList<String> rows = new ArrayList<>();
        int n = 1;
        for(ZoneFacilityLedgerEntry e: entries){
            ArrayList<String> samples = outputItemsFor(e, zt, r);
            int batches = 1 + r.nextInt(5);
            int retained = Math.max(1, Math.min(samples.size(), 1 + r.nextInt(3)));
            String cadence = cadenceFor(e, r);
            String note = noteFor(e, r);
            rows.add("P" + (n++) + ": facility=" + safe(e.id) + " :: purpose=" + safe(e.purpose) + " :: controller=" + safe(e.establishedBy) + " :: focus=" + safe(e.productFocus) + " :: cadence=" + cadence + " :: batches=" + batches + " :: retained=" + retained + " :: samples=" + String.join(", ", samples) + " :: " + note);
        }
        if(rows.isEmpty()) rows.add("P1: facility=facility.unmatched :: purpose=unspecified survival work :: controller=unknown :: focus=salvage and reserves :: cadence=sporadic :: batches=1 :: retained=1 :: samples=Vended scrap, Emergency rations :: fallback production ledger generated for an otherwise unspecific zone");
        return String.join(";;", rows);
    }

    static java.util.List<ZoneProductionOutputRecord> parseProductionLedger(String ledger){
        ArrayList<ZoneProductionOutputRecord> out = new ArrayList<>();
        if(ledger == null || ledger.isBlank()) return out;
        for(String raw: ledger.split(";;")){
            ZoneProductionOutputRecord r = parseProductionEntry(raw.trim());
            if(r != null) out.add(r);
        }
        return out;
    }

    static ZoneProductionOutputRecord parseProductionEntry(String raw){
        if(raw == null || raw.isBlank()) return null;
        try{
            ZoneProductionOutputRecord r = new ZoneProductionOutputRecord();
            String text = raw.trim();
            int colon = text.indexOf(':');
            if(colon > 0){ r.id = text.substring(0, colon).trim(); text = text.substring(colon + 1).trim(); }
            for(String part: text.split(" :: ")){
                String p = part.trim();
                if(p.startsWith("facility=")) r.facilityId = p.substring(9).trim();
                else if(p.startsWith("purpose=")) r.facilityPurpose = p.substring(8).trim();
                else if(p.startsWith("controller=")) r.controller = p.substring(11).trim();
                else if(p.startsWith("focus=")) r.outputFocus = p.substring(6).trim();
                else if(p.startsWith("cadence=")) r.cadence = p.substring(8).trim();
                else if(p.startsWith("batches=")) { try{ r.batches = Integer.parseInt(p.substring(8).trim()); }catch(Exception ignored){} }
                else if(p.startsWith("retained=")) { try{ r.retained = Integer.parseInt(p.substring(9).trim()); }catch(Exception ignored){} }
                else if(p.startsWith("samples=")) r.sampleItems = p.substring(8).trim();
                else if(!p.isBlank()) r.historicNote = p;
            }
            return r;
        }catch(Exception ex){ return null; }
    }

    static void addMatchingOutputsToCache(ArrayList<String> seeded, World w, ZoneFacilityLedgerEntry facility, Random rng){
        if(seeded == null || w == null) return;
        ZoneProductionOutputRecord best = matchOutputForFacility(w, facility);
        if(best == null || best.sampleItems == null) return;
        ArrayList<String> samples = sampleList(best.sampleItems);
        if(samples.isEmpty()) return;
        if(rng == null) rng = new Random(0);
        int addCount = Math.max(1, Math.min(2, best.retained));
        for(int i=0; i<addCount; i++){
            String item = samples.get(Math.floorMod(rng.nextInt() + i, samples.size()));
            if(item != null && ItemCatalog.get(item) != null && !seeded.contains(item)) seeded.add(item);
        }
    }

    static ZoneProductionOutputRecord matchOutputForFacility(World w, ZoneFacilityLedgerEntry facility){
        if(w == null) return null;
        java.util.List<ZoneProductionOutputRecord> rows = parseProductionLedger(w.zoneProductionHistory);
        if(rows.isEmpty()) return null;
        if(facility != null){
            for(ZoneProductionOutputRecord r: rows) if(r.facilityId != null && r.facilityId.equalsIgnoreCase(facility.id)) return r;
        }
        return rows.get(0);
    }

    static String outputChainFor(World w, ZoneFacilityLedgerEntry facility){
        ZoneProductionOutputRecord r = matchOutputForFacility(w, facility);
        if(r == null) return "";
        return " -> production " + r.id + "(" + r.outputFocus + ")";
    }

    static ArrayList<String> sampleList(String sampleItems){
        ArrayList<String> out = new ArrayList<>();
        if(sampleItems == null) return out;
        for(String s: sampleItems.split(",")){
            String item = s.trim();
            if(!item.isBlank() && ItemCatalog.get(item) != null && !out.contains(item)) out.add(item);
        }
        return out;
    }

    static ArrayList<String> outputItemsFor(ZoneFacilityLedgerEntry e, ZoneType zt, Random r){
        ArrayList<String> out = new ArrayList<>();
        String text = (safe(e.purpose) + " " + safe(e.roomType) + " " + safe(e.productFocus) + " " + (zt == null ? "" : zt.label)).toLowerCase(Locale.ROOT);
        if(has(text,"orchard","bio-garden","garden","luxury provisioning")) add(out,"Noble orchard fruit crate","Bio-garden truffle tin","High-quality amasec bottle","Ploin juice flask","Noble preserved delicacy");
        if(has(text,"hydroponic","greenhouse","agri","farm")) add(out,"Hydroponic protein grain","Marsh-rice sack","Vorder leaf bundle","Ploin juice flask","Caba nut packet","Recaf tin");
        if(has(text,"vat","reclamation","nutrient","algae","corpse-starch","soylens")) add(out,"Soylens viridian algae cake","Corpse-starch ration slab","Triglyceride gel tube","Amino-porridge ration bowl","Mechanist Collegia nutrient ampoule");
        if(has(text,"sump","fungus","sewer","mutant","forager","wall-rat")) add(out,"Sump fungus loaf","Wall-rat meat strip","Soylens viridian algae cake","Low-quality amasec bottle","Water purification tab");
        if(has(text,"ration","food","kitchen","mess","galley","canteen","pantry","nutrient")) add(out,"Emergency rations","Plain ration pack","Water ration","Sealed water ration","Kitchen grease tin","Recaf tin","Ploin juice flask");
        if(has(text,"guard","munition","arms","drill","quartermaster")) add(out,"Guard field ration tin","Guard drill manual","Guard flak vest","Guard entrenching tool","Guard lascarbine","Light Rifle","Laspistol","Autogun","Shotgun","Las charge pack","Autogun magazine");
        if(has(text,"mechanist Collegia","diagnostic","maintenance","forge","cable","doctrine")) add(out,"Mechanist Collegia calibration probe","Mechanist Collegia nutrient ampoule","Mechanist Collegia catechism strip","Sacred wire bundle","Machine oil vial","Mechanist Collegia tool roll","Arc Rifle","Omnissian Axe","Emergency Cutter","Emergency Drill","Arc capacitor pack");
        if(has(text,"noble","luxury","house","salon","estate")) add(out,"Noble preserved delicacy","Noble etiquette card","Noble signet wax kit","Noble fur-lined coat","Noble dueling pistol","Duelling Sword","Needle Pistol","Power Sword","Inferno Pistol","Dueling pistol cartridge box","Pearl Obscura","High Amasec","Gildwine","Sable Nectar","Spire Lotus");
        if(has(text,"civic Wardens","evidence","detention","law","complaint")) add(out,"Civic Wardens restraint kit","Civic Wardens casebook excerpt","Civic Wardens riot visor","Civic Wardens shock maul","Shock baton","Power Maul","Webber","Shotgun","Civic Wardens suppression shells","Web cartridge");
        if(has(text,"chem","drug","narcotic","stimulant","vice","bar","pleasure","labor dosing")) add(out,"Lho-Sticks","Recaf","Street Stimm","Grin Powder","Night Milk","Low Amasec","Shiftwake","Grey Mercy","Pipe Bloom","Crude chem bench","Reagent preparation bench","Chemical reagent rack","Narcotic drying rack","Pellet press","Labor dosing dispenser");
        if(has(text,"gang","ganger","stolen","fighting","contraband","black trade")) add(out,"Autopistol","Stub Revolver","Stub pistol","Pipe shotgun","Zip pistol","Scrap autogun","Ganger chain cleaver","Ganger buzz-cleaver","Shot shell handful","Stub cartridge box","Street Stimm","Redline","Slaught","Smokeghost");
        if(has(text,"mutant","warren","scrap hoard","forager")) add(out,"Mutant bone maul","Mutant scrap axe","Mutant tusk club","Rebar maul","Sump hook blade","Chem sprayer","Sumpkalm","Brine Joy","Glowgut Mash","Rustmilk");
        if(has(text,"cult","ritual","offering","chapel","hidden knife")) add(out,"Cult ritual blade","Toxic Knife","Heretic nail flail","Cult martyr pistol","Hand Flamer","Industrial Fuelgel canister","Witchsalt","Choir Ash","Black Benediction","Vox-Dust");
        if(has(text,"warehouse","store","cargo","freight","product","storage")) add(out,"Warehouse inventory tag bundle","Construction supplies","Tool bundle","Machine part","Wire bundle");
        if(has(text,"clinic","aid","medical","medicae")) add(out,"Bandage roll","Field dressings","Antiseptic vial","Splint kit","Medkit","Medi-Stimm","White Mercy","Clotfoam Ampoule","Nerve Lace","Sterile medicae clean bench","Injector filling station","Cold storage locker");
        if(has(text,"learning","archive","library","form","record","clerk","chapel")) add(out,"Primer slate","Blank form packet","Data spike","Warehouse inventory tag bundle");
        if(has(text,"market","barter","storefront","ticket","commerce")) add(out,"Trade chit","Market scale set","Water guild token","Market vendor sash");
        if(out.isEmpty()) add(out,"Vended scrap","Emergency rations","Water bottle","Tool bundle");
        return out;
    }

    static String cadenceFor(ZoneFacilityLedgerEntry e, Random r){
        String text = (safe(e.purpose) + " " + safe(e.productFocus)).toLowerCase(Locale.ROOT);
        if(has(text,"ration","food","water","nutrient")) return "daily ration-cycle";
        if(has(text,"munition","arms","evidence","secure")) return "controlled issue-cycle";
        if(has(text,"warehouse","cargo","storage")) return "shipment and requisition-cycle";
        if(has(text,"learning","archive","doctrine")) return "instruction-cycle";
        return r != null && r.nextBoolean() ? "periodic work-cycle" : "sporadic maintenance-cycle";
    }

    static String noteFor(ZoneFacilityLedgerEntry e, Random r){
        String[] notes = {"output retained in local rooms after routine distribution", "surplus bled into caches during faction turnover", "stored goods survive because the room remained useful", "production traces were preserved by ledgers, tags, and stubborn bureaucracy", "output is partly consumed locally and partly stored in room caches"};
        return notes[Math.floorMod(r == null ? 0 : r.nextInt(), notes.length)];
    }

    static void add(ArrayList<String> out, String... vals){ for(String v: vals) if(v != null && ItemCatalog.get(v) != null && !out.contains(v)) out.add(v); }
    static boolean has(String s, String... vals){ if(s == null) return false; for(String v: vals) if(v != null && !v.isBlank() && s.contains(v.toLowerCase(Locale.ROOT))) return true; return false; }
    static String safe(String s){ return s == null ? "" : s; }
}


class ZoneStockMovementRecord {
    String id = "M0";
    String sourceFacilityId = "facility.unmatched";
    String destination = "unassigned room cache";
    String movementKind = "stored locally";
    String controller = "unknown";
    String itemSamples = "";
    String historyNote = "unrecorded stock movement";
    String summary(){ return id + " " + movementKind + " from " + sourceFacilityId + " to " + destination + " / samples=" + itemSamples; }
}

class ProductionDistributionApi {
    private ProductionDistributionApi() {}
    static final int DEFAULT_BATCH = 72;

    static int advanceStockMovements(HiveWorldDefinition d, int maxZones){
        if(d == null) return 0;
        WorldNamingApi.populateDefinition(d);
        int made = 0;
        int limit = Math.max(1, maxZones);
        for(String key: d.zoneNames.keySet()){
            if(d.zoneStockMovements.containsKey(key)) continue;
            ProductionFacilityOutputSimulationApi.ensureZoneProduction(d, key);
            d.zoneStockMovements.put(key, buildStockMovementLedger(d, key));
            made++;
            if(made >= limit) break;
        }
        WorldGenerationProgressApi.markStockMovementAdvanced(d);
        if(made > 0) DebugLog.audit("STOCK_MOVEMENT_BATCH", "seededStockMovementLedgers=" + made + " progress=" + d.zoneStockMovements.size() + "/" + WorldGenerationProgressApi.WORLD_ZONE_SLICES + " arcology=" + d.hiveName);
        return made;
    }

    static String ensureZoneStockMovements(HiveWorldDefinition d, String key){
        if(d == null || key == null) return "No arcology-world definition loaded for production distribution history.";
        if(!d.zoneStockMovements.containsKey(key)){
            ProductionFacilityOutputSimulationApi.ensureZoneProduction(d, key);
            d.zoneStockMovements.put(key, buildStockMovementLedger(d, key));
            WorldGenerationProgressApi.markStockMovementAdvanced(d);
            DebugLog.audit("STOCK_MOVEMENT_ON_DEMAND", "zoneKey=" + key + " arcology=" + d.hiveName);
        }
        return d.zoneStockMovements.get(key);
    }

    static java.util.List<String> stockMovementLines(HiveWorldDefinition d, String key){
        ArrayList<String> out = new ArrayList<>();
        String row = ensureZoneStockMovements(d, key);
        if(row == null || row.trim().isEmpty()){ out.add("No production distribution ledger synthesized."); return out; }
        for(String part: row.split(";;")) if(part != null && !part.trim().isEmpty()) out.add(part.trim());
        return out;
    }

    static String buildStockMovementLedger(HiveWorldDefinition d, String key){
        String production = ProductionFacilityOutputSimulationApi.ensureZoneProduction(d, key);
        java.util.List<ZoneProductionOutputRecord> outputs = ProductionFacilityOutputSimulationApi.parseProductionLedger(production);
        int[] k = WorldHistoryApi.parseKey(key);
        ZoneType zt = WorldAtlas.zoneTypeForSlice(k[2], k[3], k[4], k[5] == 1);
        Random r = new Random(d.seed ^ key.hashCode() ^ 0x5A0C4D15751B7EEL);
        ArrayList<String> rows = new ArrayList<>();
        int n = 1;
        for(ZoneProductionOutputRecord out: outputs){
            String dest = destinationFor(out, zt, r);
            String kind = movementKindFor(out, zt, r);
            String samples = trimmedSamples(out.sampleItems, 4);
            String note = noteFor(out, zt, dest, r);
            rows.add("M" + (n++) + ": source=" + safe(out.facilityId) + " :: destination=" + dest + " :: kind=" + kind + " :: controller=" + safe(out.controller) + " :: samples=" + samples + " :: " + note);
        }
        if(rows.isEmpty()) rows.add("M1: source=facility.unmatched :: destination=room cache reserves :: kind=local salvage retention :: controller=unknown :: samples=Vended scrap, Emergency rations :: fallback stock movement record generated for an otherwise unspecific zone");
        return String.join(";;", rows);
    }

    static java.util.List<ZoneStockMovementRecord> parseStockMovementLedger(String ledger){
        ArrayList<ZoneStockMovementRecord> out = new ArrayList<>();
        if(ledger == null || ledger.isBlank()) return out;
        for(String raw: ledger.split(";;")){
            ZoneStockMovementRecord r = parseStockMovementEntry(raw.trim());
            if(r != null) out.add(r);
        }
        return out;
    }

    static ZoneStockMovementRecord parseStockMovementEntry(String raw){
        if(raw == null || raw.isBlank()) return null;
        try{
            ZoneStockMovementRecord r = new ZoneStockMovementRecord();
            String text = raw.trim();
            int colon = text.indexOf(':');
            if(colon > 0){ r.id = text.substring(0, colon).trim(); text = text.substring(colon + 1).trim(); }
            for(String part: text.split(" :: ")){
                String p = part.trim();
                if(p.startsWith("source=")) r.sourceFacilityId = p.substring(7).trim();
                else if(p.startsWith("destination=")) r.destination = p.substring(12).trim();
                else if(p.startsWith("kind=")) r.movementKind = p.substring(5).trim();
                else if(p.startsWith("controller=")) r.controller = p.substring(11).trim();
                else if(p.startsWith("samples=")) r.itemSamples = p.substring(8).trim();
                else if(!p.isBlank()) r.historyNote = p;
            }
            return r;
        }catch(Exception ex){ return null; }
    }

    static ZoneStockMovementRecord matchMovementForFacility(World w, ZoneFacilityLedgerEntry facility, RoomProfile rp){
        if(w == null) return null;
        java.util.List<ZoneStockMovementRecord> rows = parseStockMovementLedger(w.zoneStockMovementHistory);
        if(rows.isEmpty()) return null;
        String fid = facility == null ? "" : safe(facility.id);
        if(!fid.isBlank()) for(ZoneStockMovementRecord r: rows) if(r.sourceFacilityId != null && r.sourceFacilityId.equalsIgnoreCase(fid)) return r;
        String hay = (((rp == null || rp.name == null) ? "" : rp.name) + " " + ((rp == null || rp.descriptor == null) ? "" : rp.descriptor)).toLowerCase(Locale.ROOT);
        ZoneStockMovementRecord best = rows.get(0); int bestScore = -1;
        for(ZoneStockMovementRecord r: rows){
            int score = 0;
            String dest = safe(r.destination).toLowerCase(Locale.ROOT);
            for(String word: dest.split("[^a-z0-9]+")) if(word.length() >= 4 && hay.contains(word)) score += 2;
            if(has(hay,"warehouse","storehouse","cargo","freight") && has(dest,"warehouse","storehouse","cargo","freight","reserves")) score += 8;
            if(has(hay,"cafeteria","kitchen","mess","galley","pantry") && has(dest,"mess","galley","pantry","ration","storehouse")) score += 8;
            if(has(hay,"armory","security","evidence","holding") && has(dest,"armory","evidence","security","issue")) score += 8;
            if(has(hay,"market","storefront","counter","salon") && has(dest,"storefront","market","counter","trader")) score += 7;
            if(score > bestScore){ bestScore = score; best = r; }
        }
        return best;
    }

    static void addMatchingMovementSamplesToCache(ArrayList<String> seeded, World w, ZoneFacilityLedgerEntry facility, RoomProfile rp, Random rng){
        if(seeded == null || w == null) return;
        ZoneStockMovementRecord rec = matchMovementForFacility(w, facility, rp);
        if(rec == null || rec.itemSamples == null) return;
        ArrayList<String> samples = ProductionFacilityOutputSimulationApi.sampleList(rec.itemSamples);
        if(samples.isEmpty()) return;
        if(rng == null) rng = new Random(0);
        int addCount = Math.max(1, Math.min(2, samples.size()));
        for(int i=0; i<addCount; i++){
            String item = samples.get(Math.floorMod(rng.nextInt() + i, samples.size()));
            if(item != null && ItemCatalog.get(item) != null && !seeded.contains(item)) seeded.add(item);
        }
    }

    static String movementChainFor(World w, ZoneFacilityLedgerEntry facility, RoomProfile rp){
        ZoneStockMovementRecord r = matchMovementForFacility(w, facility, rp);
        if(r == null) return "";
        return " -> stock movement " + r.id + "(" + r.movementKind + " to " + r.destination + ")";
    }

    static String movementSummary(World w, ZoneFacilityLedgerEntry facility, RoomProfile rp){
        ZoneStockMovementRecord r = matchMovementForFacility(w, facility, rp);
        return r == null ? "unsynthesized stock movement" : r.summary();
    }

    static String destinationFor(ZoneProductionOutputRecord out, ZoneType zt, Random r){
        String text = (safe(out.facilityPurpose) + " " + safe(out.outputFocus) + " " + safe(out.sampleItems) + " " + (zt == null ? "" : zt.label)).toLowerCase(Locale.ROOT);
        if(has(text,"munition","weapon","armor","guard","civic Wardens","restraint")) return pick(r, "armory issue locker", "security room cache", "quartermaster cage", "evidence storehouse");
        if(has(text,"ration","food","water","nutrient","galley","mess","pantry")) return pick(r, "food storehouse", "mess-hall pantry", "ration issue counter", "kitchen reserve shelf");
        if(has(text,"cargo","warehouse","freight","tool","part","wire","component")) return pick(r, "product warehouse", "cargo cage", "maintenance storehouse", "freight pallet row");
        if(has(text,"market","trade","chit","scale","salon","ticket")) return pick(r, "storefront shelf", "market counter", "trader shelf", "barter cage");
        if(has(text,"clinic","bandage","medical","medkit","antiseptic")) return pick(r, "clinic cabinet", "aid-station locker", "medicae reserve drawer", "triage shelf");
        if(has(text,"library","archive","primer","manual","form","data")) return pick(r, "archive shelf", "learning-room cabinet", "library stack", "instruction locker");
        return pick(r, "local room cache", "forgotten shelf", "utility reserve", "secondary storehouse");
    }

    static String movementKindFor(ZoneProductionOutputRecord out, ZoneType zt, Random r){
        String text = (safe(out.facilityPurpose) + " " + safe(out.outputFocus) + " " + safe(out.cadence)).toLowerCase(Locale.ROOT);
        if(has(text,"ration","food","water","daily")) return "ration distribution";
        if(has(text,"munition","arms","controlled")) return "controlled issue and return";
        if(has(text,"warehouse","cargo","shipment")) return "freight transfer and storage";
        if(has(text,"learning","archive","instruction")) return "instructional issue";
        if(has(text,"clinic","aid","medical")) return "medical reserve stocking";
        return r != null && r.nextBoolean() ? "local storage transfer" : "requisition and shelf stocking";
    }

    static String noteFor(ZoneProductionOutputRecord out, ZoneType zt, String dest, Random r){
        String[] notes = {
                "goods were moved from production output into " + dest + " before room-cache recovery",
                "routine distribution left tagged stock in " + dest + " during faction turnover",
                "requisition ledgers show partial diversion into " + dest + " rather than total consumption",
                "stock movement survived as labels, shelf marks, and damaged crate seals",
                "local workers split output between active use and reserves in " + dest
        };
        return notes[Math.floorMod(r == null ? 0 : r.nextInt(), notes.length)];
    }

    static String trimmedSamples(String samples, int max){
        ArrayList<String> list = ProductionFacilityOutputSimulationApi.sampleList(samples);
        if(list.isEmpty()) return samples == null || samples.isBlank() ? "Vended scrap" : samples;
        return String.join(", ", list.subList(0, Math.min(Math.max(1, max), list.size())));
    }
    static String pick(Random r, String... vals){ return vals[Math.floorMod(r == null ? 0 : r.nextInt(), vals.length)]; }
    static boolean has(String s, String... vals){ if(s == null) return false; for(String v: vals) if(v != null && !v.isBlank() && s.contains(v.toLowerCase(Locale.ROOT))) return true; return false; }
    static String safe(String s){ return s == null ? "" : s; }
}


class ZoneConflictLossRecord {
    String id = "L0";
    String sourceFacilityId = "facility.unmatched";
    String eventType = "unrecorded loss";
    String actor = "unknown actor";
    String affectedStock = "Vended scrap";
    String destination = "forgotten cache";
    String severity = "minor";
    String historyNote = "unrecorded conflict or abandonment event";
    String summary(){ return id + " " + eventType + " by " + actor + " affecting " + affectedStock + " -> " + destination + " (" + severity + ")"; }
}

class HistoricalConflictLossApi {
    static final int DEFAULT_BATCH = 96;
    private HistoricalConflictLossApi() {}

    static int advanceConflictLoss(HiveWorldDefinition d, int maxZones){
        if(d == null) return 0;
        int limit = Math.max(1, maxZones);
        int made = 0;
        for(String key: d.zoneNames.keySet()){
            if(d.zoneConflictLosses.containsKey(key)) continue;
            ProductionDistributionApi.ensureZoneStockMovements(d, key);
            d.zoneConflictLosses.put(key, buildConflictLossLedger(d, key));
            made++;
            if(made >= limit) break;
        }
        WorldGenerationProgressApi.markConflictLossAdvanced(d);
        if(made > 0) DebugLog.audit("CONFLICT_LOSS_BATCH", "seededConflictLossLedgers=" + made + " progress=" + d.zoneConflictLosses.size() + "/" + WorldGenerationProgressApi.WORLD_ZONE_SLICES + " arcology=" + d.hiveName);
        return made;
    }

    static String ensureZoneConflictLoss(HiveWorldDefinition d, String key){
        if(d == null || key == null) return "No arcology-world definition loaded for conflict/loss history.";
        if(!d.zoneConflictLosses.containsKey(key)){
            ProductionDistributionApi.ensureZoneStockMovements(d, key);
            d.zoneConflictLosses.put(key, buildConflictLossLedger(d, key));
            WorldGenerationProgressApi.markConflictLossAdvanced(d);
            DebugLog.audit("CONFLICT_LOSS_ON_DEMAND", "zoneKey=" + key + " arcology=" + d.hiveName);
        }
        return d.zoneConflictLosses.get(key);
    }

    static java.util.List<String> conflictLossLines(HiveWorldDefinition d, String key){
        ArrayList<String> out = new ArrayList<>();
        String row = ensureZoneConflictLoss(d, key);
        if(row == null || row.trim().isEmpty()){ out.add("No conflict/loss ledger synthesized."); return out; }
        for(String part: row.split(";;")) if(part != null && !part.trim().isEmpty()) out.add(part.trim());
        return out;
    }

    static String buildConflictLossLedger(HiveWorldDefinition d, String key){
        String movements = ProductionDistributionApi.ensureZoneStockMovements(d, key);
        java.util.List<ZoneStockMovementRecord> rows = ProductionDistributionApi.parseStockMovementLedger(movements);
        int[] k = WorldHistoryApi.parseKey(key);
        ZoneType zt = WorldAtlas.zoneTypeForSlice(k[2], k[3], k[4], k[5] == 1);
        Random r = new Random(d.seed ^ key.hashCode() ^ 0x7110C0FFEE87L);
        ArrayList<String> out = new ArrayList<>();
        int n = 1;
        for(ZoneStockMovementRecord m: rows){
            String event = eventTypeFor(m, zt, r);
            String actor = actorFor(event, zt, m, r);
            String dest = destinationFor(event, zt, m, r);
            String samples = trimSamples(m.itemSamples, 4);
            String severity = severityFor(event, r);
            String note = noteFor(event, actor, dest, zt, r);
            out.add("L" + (n++) + ": source=" + safe(m.sourceFacilityId) + " :: event=" + event + " :: actor=" + actor + " :: affected=" + samples + " :: destination=" + dest + " :: severity=" + severity + " :: " + note);
        }
        if(out.isEmpty()) out.add("L1: source=facility.unmatched :: event=abandoned reserve :: actor=unknown prior occupants :: affected=Vended scrap, Emergency rations :: destination=forgotten cache :: severity=minor :: fallback abandonment record generated for unspecific stock");
        return String.join(";;", out);
    }

    static java.util.List<ZoneConflictLossRecord> parseConflictLossLedger(String ledger){
        ArrayList<ZoneConflictLossRecord> out = new ArrayList<>();
        if(ledger == null || ledger.isBlank()) return out;
        for(String raw: ledger.split(";;")){
            ZoneConflictLossRecord r = parseConflictLossEntry(raw.trim());
            if(r != null) out.add(r);
        }
        return out;
    }

    static ZoneConflictLossRecord parseConflictLossEntry(String raw){
        if(raw == null || raw.isBlank()) return null;
        try{
            ZoneConflictLossRecord r = new ZoneConflictLossRecord();
            String text = raw.trim();
            int colon = text.indexOf(':');
            if(colon > 0){ r.id = text.substring(0, colon).trim(); text = text.substring(colon + 1).trim(); }
            for(String part: text.split(" :: ")){
                String p = part.trim();
                if(p.startsWith("source=")) r.sourceFacilityId = p.substring(7).trim();
                else if(p.startsWith("event=")) r.eventType = p.substring(6).trim();
                else if(p.startsWith("actor=")) r.actor = p.substring(6).trim();
                else if(p.startsWith("affected=")) r.affectedStock = p.substring(9).trim();
                else if(p.startsWith("destination=")) r.destination = p.substring(12).trim();
                else if(p.startsWith("severity=")) r.severity = p.substring(9).trim();
                else if(!p.isBlank()) r.historyNote = p;
            }
            return r;
        }catch(Exception ex){ return null; }
    }

    static ZoneConflictLossRecord matchConflictForFacility(World w, ZoneFacilityLedgerEntry facility, RoomProfile rp){
        if(w == null) return null;
        java.util.List<ZoneConflictLossRecord> rows = parseConflictLossLedger(w.zoneConflictLossHistory);
        if(rows.isEmpty()) return null;
        String fid = facility == null ? "" : safe(facility.id);
        if(!fid.isBlank()) for(ZoneConflictLossRecord r: rows) if(safe(r.sourceFacilityId).equalsIgnoreCase(fid)) return r;
        String hay = (((rp == null || rp.name == null) ? "" : rp.name) + " " + ((rp == null || rp.descriptor == null) ? "" : rp.descriptor)).toLowerCase(Locale.ROOT);
        ZoneConflictLossRecord best = rows.get(0); int bestScore = -1;
        for(ZoneConflictLossRecord r: rows){
            int score = 0;
            String dest = safe(r.destination).toLowerCase(Locale.ROOT);
            String event = safe(r.eventType).toLowerCase(Locale.ROOT);
            if(has(hay,"warehouse","storehouse","cache") && has(dest,"cache","warehouse","storehouse","buried")) score += 8;
            if(has(hay,"hivewall","interwall","void") && has(dest,"interwall","hivewall","void")) score += 10;
            if(has(hay,"armory","security","evidence") && has(event,"seizure","theft","raid","confiscation")) score += 8;
            if(has(hay,"market","storefront","counter") && has(event,"theft","diversion","black-market")) score += 7;
            if(score > bestScore){ bestScore = score; best = r; }
        }
        return best;
    }

    static void addConflictSamplesToCache(ArrayList<String> seeded, World w, ZoneFacilityLedgerEntry facility, RoomProfile rp, Random rng){
        if(seeded == null || w == null) return;
        ZoneConflictLossRecord rec = matchConflictForFacility(w, facility, rp);
        if(rec == null) return;
        ArrayList<String> samples = ProductionFacilityOutputSimulationApi.sampleList(rec.affectedStock);
        ArrayList<String> extras = new ArrayList<>();
        String text = (safe(rec.eventType) + " " + safe(rec.actor) + " " + safe(rec.destination)).toLowerCase(Locale.ROOT);
        if(has(text,"theft","gang","black-market")) add(extras,"Stolen goods marker","Trade chit","Scrap knife");
        if(has(text,"seizure","civic Wardens","confiscation")) add(extras,"Civic Wardens casebook excerpt","Civic Wardens restraint kit","Secure vault key");
        if(has(text,"collapse","abandoned","buried")) add(extras,"Collapsed arcology salvage","Tool bundle","Emergency rations");
        if(has(text,"cult","heretic")) add(extras,"Contraband charm","Cult reliquary packet","Hidden knife");
        if(has(text,"mechanist Collegia","automata","machine")) add(extras,"Rogue automata service core","Mechanist Collegia calibration probe","Sacred wire bundle");
        for(String e: extras) if(e != null && ItemCatalog.get(e) != null && !samples.contains(e)) samples.add(e);
        if(samples.isEmpty()) return;
        if(rng == null) rng = new Random(0);
        int count = Math.min(2, samples.size());
        for(int i=0;i<count;i++){
            String item = samples.get(Math.floorMod(rng.nextInt() + i, samples.size()));
            if(item != null && ItemCatalog.get(item) != null && !seeded.contains(item)) seeded.add(item);
        }
    }

    static String conflictChainFor(World w, ZoneFacilityLedgerEntry facility, RoomProfile rp){
        ZoneConflictLossRecord r = matchConflictForFacility(w, facility, rp);
        if(r == null) return "";
        return " -> conflict/loss " + r.id + "(" + r.eventType + " by " + r.actor + " to " + r.destination + ")";
    }
    static String conflictSummary(World w, ZoneFacilityLedgerEntry facility, RoomProfile rp){
        ZoneConflictLossRecord r = matchConflictForFacility(w, facility, rp);
        return r == null ? "unsynthesized conflict/loss" : r.summary();
    }

    static String eventTypeFor(ZoneStockMovementRecord m, ZoneType zt, Random r){
        String text = (safe(m.destination) + " " + safe(m.movementKind) + " " + safe(m.controller) + " " + (zt == null ? "" : zt.label)).toLowerCase(Locale.ROOT);
        if(has(text,"gang","sump","market")) return pick(r,"gang theft","black-market diversion","protection racket skimming");
        if(has(text,"civic Wardens","evidence","security")) return pick(r,"Civic Wardens seizure","evidence-room loss","confiscation transfer");
        if(has(text,"guard","munition","armory")) return pick(r,"Guard requisition","munition loss","barracks raid");
        if(has(text,"mechanist Collegia","forge","component","machine")) return pick(r,"Mechanist Collegia quarantine","rogue automata abandonment","sealed tech interdiction");
        if(has(text,"cult","sewer","mutant")) return pick(r,"cult diversion","mutant raid","sewer collapse abandonment");
        if(has(text,"noble","pantry","salon")) return pick(r,"household concealment","servant theft","succession seizure");
        return pick(r,"abandoned reserve","collapse burial","clerical misrouting","worker theft");
    }
    static String actorFor(String event, ZoneType zt, ZoneStockMovementRecord m, Random r){
        String e = safe(event).toLowerCase(Locale.ROOT);
        if(has(e,"gang","protection","black-market")) return pick(r,"local gang crew","sump market fence","cargo-yard thieves");
        if(has(e,"civic Wardens","evidence","confiscation")) return pick(r,"Civic Wardens evidence detail","precinct quartermaster","overworked complaint counter");
        if(has(e,"guard","munition","barracks")) return pick(r,"Guard quartermaster","deserter cell","munitorum clerk");
        if(has(e,"mechanist Collegia","automata","tech")) return pick(r,"Mechanist Collegia seal crew","rogue maintenance automata","red-robed audit cell");
        if(has(e,"cult","heretic")) return pick(r,"hidden cult cell","heretical quartermaster","sewer preacher gang");
        if(has(e,"mutant")) return pick(r,"mutant brood","sump scavenger pack","collapse-pocket brutes");
        if(has(e,"noble","servant","succession")) return pick(r,"noble house steward","servant smuggling ring","inheritance guard detail");
        return pick(r,"unknown prior occupants","maintenance crew","lost freight clerk","nameless scavengers");
    }
    static String destinationFor(String event, ZoneType zt, ZoneStockMovementRecord m, Random r){
        String e = safe(event).toLowerCase(Locale.ROOT);
        if(has(e,"theft","diversion","racket")) return pick(r,"black-market shelf","gang stash room","pawn cage","hidden cache");
        if(has(e,"seizure","confiscation","evidence")) return pick(r,"evidence locker","sealed precinct crate","contraband warehouse","case-room shelf");
        if(has(e,"collapse","burial")) return pick(r,"buried interstitial debris pocket","collapsed service niche","forgotten wall cache","intra-arcology rubble seam");
        if(has(e,"quarantine","interdiction","automata")) return pick(r,"sealed Mechanist Collegia alcove","hivewall maintenance room","abandoned interwall danger room","diagnostic bay dead shelf");
        if(has(e,"cult","heretic")) return pick(r,"cult reliquary cache","sealed drain shrine","hidden knife chapel","interwall heretic room");
        return pick(r,"forgotten cache","abandoned storeroom","room cache reserve","hivewall maintenance locker");
    }
    static String severityFor(String event, Random r){
        String e = safe(event).toLowerCase(Locale.ROOT);
        if(has(e,"raid","collapse","automata","mutant")) return pick(r,"major","severe","catastrophic");
        if(has(e,"theft","diversion","seizure","requisition")) return pick(r,"minor","moderate","major");
        return pick(r,"minor","moderate");
    }
    static String noteFor(String event, String actor, String dest, ZoneType zt, Random r){
        String[] notes = {
                actor + " redirected goods into " + dest + " during a documented control dispute",
                "loss marks explain why stock appears outside the clean production route",
                "scavengers inherited the remains after the original ledger stopped balancing",
                "the event left damaged seals, erased tags, and a provenance break in the zone economy",
                "abandoned inventory persisted as caches after violence, collapse, or bureaucratic seizure"
        };
        return notes[Math.floorMod(r == null ? 0 : r.nextInt(), notes.length)];
    }
    static String trimSamples(String samples, int max){ return ProductionDistributionApi.trimmedSamples(samples, max); }
    static String pick(Random r, String... vals){ return vals[Math.floorMod(r == null ? 0 : r.nextInt(), vals.length)]; }
    static boolean has(String s, String... vals){ if(s == null) return false; for(String v: vals) if(v != null && !v.isBlank() && s.contains(v.toLowerCase(Locale.ROOT))) return true; return false; }
    static void add(ArrayList<String> out, String... vals){ for(String v: vals) if(v != null && ItemCatalog.get(v) != null) out.add(v); }
    static String safe(String s){ return s == null ? "" : s; }
}

class ZoneFacilityLedgerEntry {
    String id = "facility.unassigned";
    String purpose = "unrecorded purpose";
    String establishedBy = "unrecorded controller";
    String roomType = "unrecorded room";
    String productFocus = "unrecorded output";
    String populationSource = "unrecorded people";
    String historicNote = "unrecorded history";
    String summary(){ return id + " " + purpose + " / " + roomType + " / by " + establishedBy + " / " + productFocus; }
}

class ZoneFacilityRecord {
    final String purpose;
    final String establishedBy;
    final String roomType;
    final String productFocus;
    final String populationSource;
    final String historicNote;
    ZoneFacilityRecord(String purpose, String establishedBy, String roomType, String productFocus, String populationSource, String historicNote){
        this.purpose=purpose; this.establishedBy=establishedBy; this.roomType=roomType; this.productFocus=productFocus; this.populationSource=populationSource; this.historicNote=historicNote;
    }
    String encode(){ return purpose + " :: by " + establishedBy + " :: room=" + roomType + " :: output=" + productFocus + " :: people=" + populationSource + " :: " + historicNote; }
}

class ZoneMaterializedItemRecord {
    String id = "M0";
    String itemName = "Vended scrap";
    String quality = "Common";
    String sourceFacilityId = "facility.unmatched";
    String productionId = "P0";
    String stockMovementId = "S0";
    String conflictLossId = "L0";
    String ageBand = "recent";
    String destination = "persistent room cache";
    String historyNote = "concrete item materialized from compact historical ledgers";
    String displayName(){ return quality == null || quality.isBlank() || quality.equalsIgnoreCase("Common") ? itemName : quality + " " + itemName; }
    String summary(){ return id + " " + displayName() + " source=" + sourceFacilityId + " production=" + productionId + " movement=" + stockMovementId + " loss=" + conflictLossId + " age=" + ageBand + " -> " + destination; }
}

class HistoricalItemMaterializationApi {
    static final int DEFAULT_BATCH = 96;
    private HistoricalItemMaterializationApi() {}

    static int advanceHistoricalItemMaterialization(HiveWorldDefinition d, int maxZones){
        if(d == null) return 0;
        int limit = Math.max(1, maxZones);
        int made = 0;
        for(String key: d.zoneNames.keySet()){
            if(d.zoneMaterializedItems.containsKey(key)) continue;
            HistoricalConflictLossApi.ensureZoneConflictLoss(d, key);
            d.zoneMaterializedItems.put(key, buildMaterializationLedger(d, key));
            made++;
            if(made >= limit) break;
        }
        WorldGenerationProgressApi.markItemMaterializationAdvanced(d);
        if(made > 0) DebugLog.audit("ITEM_MATERIALIZATION_BATCH", "seededMaterializedItemLedgers=" + made + " progress=" + d.zoneMaterializedItems.size() + "/" + WorldGenerationProgressApi.WORLD_ZONE_SLICES + " arcology=" + d.hiveName);
        return made;
    }

    static String ensureZoneMaterializedItems(HiveWorldDefinition d, String key){
        if(d == null || key == null) return "No arcology-world definition loaded for concrete item materialization.";
        if(!d.zoneMaterializedItems.containsKey(key)){
            HistoricalConflictLossApi.ensureZoneConflictLoss(d, key);
            d.zoneMaterializedItems.put(key, buildMaterializationLedger(d, key));
            WorldGenerationProgressApi.markItemMaterializationAdvanced(d);
            DebugLog.audit("ITEM_MATERIALIZATION_ON_DEMAND", "zoneKey=" + key + " arcology=" + d.hiveName);
        }
        return d.zoneMaterializedItems.get(key);
    }

    static java.util.List<String> materializedItemLines(HiveWorldDefinition d, String key){
        ArrayList<String> out = new ArrayList<>();
        String row = ensureZoneMaterializedItems(d, key);
        if(row == null || row.trim().isEmpty()){ out.add("No concrete historical item materialization ledger synthesized."); return out; }
        for(String part: row.split(";;")) if(part != null && !part.trim().isEmpty()) out.add(part.trim());
        return out;
    }

    static String buildMaterializationLedger(HiveWorldDefinition d, String key){
        String production = ProductionFacilityOutputSimulationApi.ensureZoneProduction(d, key);
        String movement = ProductionDistributionApi.ensureZoneStockMovements(d, key);
        String losses = HistoricalConflictLossApi.ensureZoneConflictLoss(d, key);
        java.util.List<ZoneProductionOutputRecord> prods = ProductionFacilityOutputSimulationApi.parseProductionLedger(production);
        java.util.List<ZoneStockMovementRecord> moves = ProductionDistributionApi.parseStockMovementLedger(movement);
        java.util.List<ZoneConflictLossRecord> conflicts = HistoricalConflictLossApi.parseConflictLossLedger(losses);
        Random r = new Random(d.seed ^ key.hashCode() ^ 0x5187A11E088L);
        ArrayList<String> rows = new ArrayList<>();
        int n = 1;
        int limit = Math.min(8, Math.max(3, prods.size() + conflicts.size()));
        for(ZoneProductionOutputRecord p : prods){
            if(rows.size() >= limit) break;
            ZoneStockMovementRecord m = matchMovement(p, moves);
            ZoneConflictLossRecord l = matchConflict(p, conflicts);
            String item = pickItem(p.sampleItems, m == null ? null : m.itemSamples, l == null ? null : l.affectedStock, r);
            if(item == null || ItemCatalog.get(item) == null) continue;
            String q = qualityFor(d, key, p, m, l, r);
            String age = ageBandFor(d, key, l, r);
            String dest = m == null ? "facility reserve" : m.destination;
            if(l != null && l.destination != null && !l.destination.isBlank()) dest = l.destination;
            String note = "materialized selectively from compact production/distribution/conflict ledgers; not a full-arcology item simulation";
            rows.add("M" + (n++) + ": item=" + item + " :: quality=" + q + " :: facility=" + safe(p.facilityId) + " :: production=" + safe(p.id) + " :: movement=" + (m==null?"none":safe(m.id)) + " :: loss=" + (l==null?"none":safe(l.id)) + " :: age=" + age + " :: destination=" + safe(dest) + " :: " + note);
        }
        for(ZoneConflictLossRecord l : conflicts){
            if(rows.size() >= limit) break;
            String item = pickItem(null, null, l.affectedStock, r);
            if(item == null || ItemCatalog.get(item) == null) continue;
            String q = qualityFor(d, key, null, null, l, r);
            rows.add("M" + (n++) + ": item=" + item + " :: quality=" + q + " :: facility=" + safe(l.sourceFacilityId) + " :: production=none :: movement=none :: loss=" + safe(l.id) + " :: age=" + ageBandFor(d,key,l,r) + " :: destination=" + safe(l.destination) + " :: loss-only item materialized from theft, collapse, seizure, or abandonment history");
        }
        if(rows.isEmpty()) rows.add("M1: item=Vended scrap :: quality=Junk :: facility=facility.unmatched :: production=none :: movement=none :: loss=none :: age=recent :: destination=fallback room cache :: fallback materialized item record for an otherwise unspecific zone");
        return String.join(";;", rows);
    }

    static java.util.List<ZoneMaterializedItemRecord> parseMaterializedLedger(String ledger){
        ArrayList<ZoneMaterializedItemRecord> out = new ArrayList<>();
        if(ledger == null || ledger.isBlank()) return out;
        for(String raw: ledger.split(";;")){
            ZoneMaterializedItemRecord r = parseMaterializedEntry(raw.trim());
            if(r != null) out.add(r);
        }
        return out;
    }

    static ZoneMaterializedItemRecord parseMaterializedEntry(String raw){
        if(raw == null || raw.isBlank()) return null;
        try{
            ZoneMaterializedItemRecord r = new ZoneMaterializedItemRecord();
            String text = raw.trim();
            int colon = text.indexOf(':');
            if(colon > 0){ r.id = text.substring(0, colon).trim(); text = text.substring(colon + 1).trim(); }
            for(String part: text.split(" :: ")){
                String p = part.trim();
                if(p.startsWith("item=")) r.itemName = p.substring(5).trim();
                else if(p.startsWith("quality=")) r.quality = p.substring(8).trim();
                else if(p.startsWith("facility=")) r.sourceFacilityId = p.substring(9).trim();
                else if(p.startsWith("production=")) r.productionId = p.substring(11).trim();
                else if(p.startsWith("movement=")) r.stockMovementId = p.substring(9).trim();
                else if(p.startsWith("loss=")) r.conflictLossId = p.substring(5).trim();
                else if(p.startsWith("age=")) r.ageBand = p.substring(4).trim();
                else if(p.startsWith("destination=")) r.destination = p.substring(12).trim();
                else if(!p.isBlank()) r.historyNote = p;
            }
            return r;
        }catch(Exception ex){ return null; }
    }

    static void addMaterializedSamplesToCache(ArrayList<String> seeded, World w, ZoneFacilityLedgerEntry facility, RoomProfile rp, Random rng){
        if(seeded == null || w == null) return;
        java.util.List<ZoneMaterializedItemRecord> rows = parseMaterializedLedger(w.zoneMaterializedItemHistory);
        if(rows.isEmpty()) return;
        if(rng == null) rng = new Random(0);
        ArrayList<ZoneMaterializedItemRecord> matches = new ArrayList<>();
        for(ZoneMaterializedItemRecord r: rows) if(matchesRoom(r, facility, rp)) matches.add(r);
        if(matches.isEmpty()) matches.addAll(rows);
        int count = Math.min(2, matches.size());
        for(int i=0;i<count;i++){
            ZoneMaterializedItemRecord r = matches.get(Math.floorMod(rng.nextInt() + i, matches.size()));
            String item = r.displayName();
            if(ItemCatalog.get(item) != null && !seeded.contains(item)) seeded.add(item);
        }
    }

    static String materializationChainFor(World w, ZoneFacilityLedgerEntry facility, RoomProfile rp, String item){
        ZoneMaterializedItemRecord r = matchMaterialized(w, facility, rp, item);
        if(r == null) return "";
        return " -> materialized " + r.id + "(" + r.displayName() + ", " + r.ageBand + ", " + r.destination + ")";
    }

    static String materializationSummary(World w, ZoneFacilityLedgerEntry facility, RoomProfile rp, String item){
        ZoneMaterializedItemRecord r = matchMaterialized(w, facility, rp, item);
        return r == null ? "unsynthesized concrete materialization" : r.summary();
    }

    static ZoneMaterializedItemRecord matchMaterialized(World w, ZoneFacilityLedgerEntry facility, RoomProfile rp, String item){
        if(w == null) return null;
        java.util.List<ZoneMaterializedItemRecord> rows = parseMaterializedLedger(w.zoneMaterializedItemHistory);
        if(rows.isEmpty()) return null;
        String fid = facility == null ? "" : safe(facility.id);
        String baseItem = ItemQuality.stripQuality(item == null ? "" : item);
        ZoneMaterializedItemRecord best = rows.get(0); int bestScore = -1;
        for(ZoneMaterializedItemRecord r: rows){
            int score = 0;
            if(!fid.isBlank() && safe(r.sourceFacilityId).equalsIgnoreCase(fid)) score += 10;
            if(!baseItem.isBlank() && ItemQuality.stripQuality(r.itemName).equalsIgnoreCase(baseItem)) score += 12;
            String hay = (((rp == null || rp.name == null) ? "" : rp.name) + " " + ((rp == null || rp.descriptor == null) ? "" : rp.descriptor)).toLowerCase(Locale.ROOT);
            String dest = safe(r.destination).toLowerCase(Locale.ROOT);
            if(has(hay,"warehouse","storehouse","cache") && has(dest,"warehouse","store","cache","reserve")) score += 4;
            if(has(hay,"hivewall","interwall","void") && has(dest,"hivewall","interwall","void","buried")) score += 8;
            if(score > bestScore){ bestScore = score; best = r; }
        }
        return best;
    }

    static boolean matchesRoom(ZoneMaterializedItemRecord r, ZoneFacilityLedgerEntry facility, RoomProfile rp){
        if(r == null) return false;
        if(facility != null && safe(r.sourceFacilityId).equalsIgnoreCase(safe(facility.id))) return true;
        String room = (((rp == null || rp.name == null) ? "" : rp.name) + " " + ((rp == null || rp.descriptor == null) ? "" : rp.descriptor)).toLowerCase(Locale.ROOT);
        String dest = safe(r.destination).toLowerCase(Locale.ROOT);
        if(has(room,"warehouse","storehouse","cache") && has(dest,"warehouse","store","cache","reserve")) return true;
        if(has(room,"market","storefront","counter") && has(dest,"market","trader","shelf","counter")) return true;
        if(has(room,"hivewall","interwall","void") && has(dest,"interwall","hivewall","void","buried")) return true;
        return false;
    }

    static ZoneStockMovementRecord matchMovement(ZoneProductionOutputRecord p, java.util.List<ZoneStockMovementRecord> moves){
        if(p == null || moves == null) return null;
        for(ZoneStockMovementRecord m: moves) if(safe(m.sourceFacilityId).equalsIgnoreCase(safe(p.facilityId))) return m;
        return moves.isEmpty() ? null : moves.get(0);
    }
    static ZoneConflictLossRecord matchConflict(ZoneProductionOutputRecord p, java.util.List<ZoneConflictLossRecord> losses){
        if(p == null || losses == null) return null;
        for(ZoneConflictLossRecord l: losses) if(safe(l.sourceFacilityId).equalsIgnoreCase(safe(p.facilityId))) return l;
        return losses.isEmpty() ? null : losses.get(0);
    }
    static String pickItem(String a, String b, String c, Random r){
        ArrayList<String> pool = new ArrayList<>();
        pool.addAll(ProductionFacilityOutputSimulationApi.sampleList(a));
        pool.addAll(ProductionFacilityOutputSimulationApi.sampleList(b));
        pool.addAll(ProductionFacilityOutputSimulationApi.sampleList(c));
        ArrayList<String> clean = new ArrayList<>();
        for(String s: pool) if(s != null && ItemCatalog.get(s) != null && !clean.contains(s)) clean.add(s);
        if(clean.isEmpty()) return "Vended scrap";
        return clean.get(Math.floorMod(r == null ? 0 : r.nextInt(), clean.size()));
    }
    static String qualityFor(HiveWorldDefinition d, String key, ZoneProductionOutputRecord p, ZoneStockMovementRecord m, ZoneConflictLossRecord l, Random r){
        int score = 2;
        if(p != null) score += Math.min(2, Math.max(0, p.batches / 2));
        if(m != null && has(safe(m.movementKind).toLowerCase(Locale.ROOT), "requisition", "charter", "archive", "warehouse")) score++;
        if(l != null && has((safe(l.severity)+" "+safe(l.eventType)).toLowerCase(Locale.ROOT), "major", "severe", "interdiction", "quarantine", "noble", "mechanist Collegia")) score++;
        if(d != null && d.zoneEpochs != null) score += Math.min(1, d.zoneEpochs.size() / Math.max(1, WorldGenerationProgressApi.WORLD_ZONE_SLICES));
        if(r != null && r.nextInt(100) < 18) score++;
        score = Math.max(0, Math.min(5, score)); // cap ordinary world materialization two tiers below Archeotech maximum.
        return ItemQuality.NAMES[score];
    }
    static String ageBandFor(HiveWorldDefinition d, String key, ZoneConflictLossRecord l, Random r){
        String text = (l == null ? "" : (safe(l.eventType) + " " + safe(l.destination) + " " + safe(l.historyNote))).toLowerCase(Locale.ROOT);
        if(has(text,"ancient","sealed","interwall","buried","collapse","abandoned")) return pick(r,"old","forgotten","pre-current occupation");
        if(has(text,"theft","seizure","requisition")) return pick(r,"recent","within living memory","current occupation");
        return pick(r,"recent","old","within living memory");
    }
    static String pick(Random r, String... vals){ return vals[Math.floorMod(r == null ? 0 : r.nextInt(), vals.length)]; }
    static boolean has(String s, String... vals){ if(s == null) return false; for(String v: vals) if(v != null && !v.isBlank() && s.contains(v.toLowerCase(Locale.ROOT))) return true; return false; }
    static String safe(String s){ return s == null ? "" : s; }
}


class ZoneLaborAssignmentRecord {
    String id = "L0";
    String facilityId = "facility.unmatched";
    String facilityPurpose = "unrecorded facility";
    String sourcePool = "unassigned population pool";
    String assignedRole = "general labor";
    int workers = 1;
    int vacancies = 0;
    String outputEffect = "baseline output";
    String historicNote = "unrecorded labor history";
    String summary(){ return id + " facility=" + facilityId + " role=" + assignedRole + " workers=" + workers + " vacancies=" + vacancies + " source=" + sourcePool; }
}

class PopulationWorkAssignmentApi {
    private PopulationWorkAssignmentApi() {}
    static final int DEFAULT_BATCH = 72;

    static int advanceLaborAssignments(HiveWorldDefinition d, int maxZones){
        if(d == null) return 0;
        WorldNamingApi.populateDefinition(d);
        int made = 0;
        int limit = Math.max(1, maxZones);
        for(String key: d.zoneNames.keySet()){
            if(d.zoneLaborAssignments.containsKey(key)) continue;
            HistoricalItemMaterializationApi.ensureZoneMaterializedItems(d, key);
            d.zoneLaborAssignments.put(key, buildLaborLedger(d, key));
            made++;
            if(made >= limit) break;
        }
        WorldGenerationProgressApi.markLaborAssignmentAdvanced(d);
        if(made > 0) DebugLog.audit("LABOR_ASSIGNMENT_BATCH", "seededLaborLedgers=" + made + " progress=" + d.zoneLaborAssignments.size() + "/" + WorldGenerationProgressApi.WORLD_ZONE_SLICES + " arcology=" + d.hiveName);
        return made;
    }

    static String ensureZoneLaborAssignments(HiveWorldDefinition d, String key){
        if(d == null || key == null) return "No arcology-world definition loaded for population work-assignment history.";
        if(!d.zoneLaborAssignments.containsKey(key)){
            HistoricalItemMaterializationApi.ensureZoneMaterializedItems(d, key);
            d.zoneLaborAssignments.put(key, buildLaborLedger(d, key));
            WorldGenerationProgressApi.markLaborAssignmentAdvanced(d);
            DebugLog.audit("LABOR_ASSIGNMENT_ON_DEMAND", "zoneKey=" + key + " arcology=" + d.hiveName);
        }
        return d.zoneLaborAssignments.get(key);
    }

    static java.util.List<String> laborAssignmentLines(HiveWorldDefinition d, String key){
        ArrayList<String> out = new ArrayList<>();
        String row = ensureZoneLaborAssignments(d, key);
        if(row == null || row.trim().isEmpty()){ out.add("No population work-assignment ledger synthesized."); return out; }
        for(String part: row.split(";;")) if(part != null && !part.trim().isEmpty()) out.add(part.trim());
        return out;
    }

    static String buildLaborLedger(HiveWorldDefinition d, String key){
        String facilities = ZoneFacilityHistoryApi.ensureZoneFacilities(d, key);
        ProductionFacilityOutputSimulationApi.ensureZoneProduction(d, key);
        java.util.List<ZoneFacilityLedgerEntry> entries = ZoneFacilityHistoryApi.parseFacilityLedger(facilities);
        int[] k = WorldHistoryApi.parseKey(key);
        ZoneType zt = WorldAtlas.zoneTypeForSlice(k[2], k[3], k[4], k[5] == 1);
        Random r = new Random(d.seed ^ key.hashCode() ^ 0x1AB05EED89L);
        ArrayList<String> rows = new ArrayList<>();
        int n = 1;
        for(ZoneFacilityLedgerEntry e: entries){
            String pool = sourcePoolFor(e, zt, r);
            String role = roleFor(e, zt, r);
            int workers = workerCountFor(e, zt, r);
            int vacancies = vacancyCountFor(e, zt, r);
            String effect = outputEffectFor(e, workers, vacancies, r);
            String note = noteFor(e, zt, r);
            rows.add("L" + (n++) + ": facility=" + safe(e.id) + " :: purpose=" + safe(e.purpose) + " :: sourcePool=" + pool + " :: role=" + role + " :: workers=" + workers + " :: vacancies=" + vacancies + " :: outputEffect=" + effect + " :: " + note);
        }
        if(rows.isEmpty()) rows.add("L1: facility=facility.unmatched :: purpose=unspecified survival labor :: sourcePool=local drifters :: role=general scavenging :: workers=1 :: vacancies=1 :: outputEffect=sporadic output :: fallback labor ledger generated for an otherwise unspecific zone");
        return String.join(";;", rows);
    }

    static java.util.List<ZoneLaborAssignmentRecord> parseLaborLedger(String ledger){
        ArrayList<ZoneLaborAssignmentRecord> out = new ArrayList<>();
        if(ledger == null || ledger.isBlank()) return out;
        for(String raw: ledger.split(";;")){
            ZoneLaborAssignmentRecord r = parseLaborEntry(raw.trim());
            if(r != null) out.add(r);
        }
        return out;
    }

    static ZoneLaborAssignmentRecord parseLaborEntry(String raw){
        if(raw == null || raw.isBlank()) return null;
        try{
            ZoneLaborAssignmentRecord r = new ZoneLaborAssignmentRecord();
            String text = raw.trim();
            int colon = text.indexOf(':');
            if(colon > 0){ r.id = text.substring(0, colon).trim(); text = text.substring(colon + 1).trim(); }
            for(String part: text.split(" :: ")){
                String p = part.trim();
                if(p.startsWith("facility=")) r.facilityId = p.substring(9).trim();
                else if(p.startsWith("purpose=")) r.facilityPurpose = p.substring(8).trim();
                else if(p.startsWith("sourcePool=")) r.sourcePool = p.substring(11).trim();
                else if(p.startsWith("role=")) r.assignedRole = p.substring(5).trim();
                else if(p.startsWith("workers=")) { try{ r.workers = Integer.parseInt(p.substring(8).trim()); }catch(Exception ignored){} }
                else if(p.startsWith("vacancies=")) { try{ r.vacancies = Integer.parseInt(p.substring(10).trim()); }catch(Exception ignored){} }
                else if(p.startsWith("outputEffect=")) r.outputEffect = p.substring(13).trim();
                else if(!p.isBlank()) r.historicNote = p;
            }
            return r;
        }catch(Exception ex){ return null; }
    }

    static ZoneLaborAssignmentRecord matchLaborForFacility(World w, ZoneFacilityLedgerEntry facility){
        if(w == null) return null;
        java.util.List<ZoneLaborAssignmentRecord> rows = parseLaborLedger(w.zoneLaborAssignmentHistory);
        if(rows.isEmpty()) return null;
        String fid = facility == null ? "" : safe(facility.id);
        if(!fid.isBlank()) for(ZoneLaborAssignmentRecord r: rows) if(safe(r.facilityId).equalsIgnoreCase(fid)) return r;
        return rows.get(0);
    }

    static String laborChainFor(World w, ZoneFacilityLedgerEntry facility){
        ZoneLaborAssignmentRecord r = matchLaborForFacility(w, facility);
        if(r == null) return "";
        return " -> labor " + r.id + "(" + r.assignedRole + ", " + r.workers + " worker(s), " + r.outputEffect + ")";
    }

    static String laborSummary(World w, ZoneFacilityLedgerEntry facility){
        ZoneLaborAssignmentRecord r = matchLaborForFacility(w, facility);
        return r == null ? "unsynthesized labor assignment" : r.summary();
    }

    static String sourcePoolFor(ZoneFacilityLedgerEntry e, ZoneType zt, Random r){
        String text = all(e, zt);
        if(has(text,"guard","military","munition","drill","barracks")) return pick(r,"Guard barracks roster","quartermaster detail pool","muster overflow ledger");
        if(has(text,"mechanist Collegia","forge","nutrient","diagnostic","cable","vat")) return pick(r,"forge creche ledger","tech-adept duty chain","vat-adept roster");
        if(has(text,"noble","house","orchard","garden","salon","luxury")) return pick(r,"household staff book","garden-serf line","estate steward rolls");
        if(has(text,"civic Wardens","evidence","holding","law")) return pick(r,"precinct roster","custody clerk pool","watch rotation ledger");
        if(has(text,"archive","civic Ledger Office","form","queue","clerk")) return pick(r,"filing clan dorm","archive worker pool","ledger apprentice roll");
        if(has(text,"rail","market","cargo","freight","hydroponic","farm")) return pick(r,"rail intake ledger","porter guild roster","market stall kin-list");
        if(has(text,"gang","stolen","chem","fighting")) return pick(r,"crash-room roster","debtor crew pool","fence ledger");
        if(has(text,"mutant","fungus","wall-rat","kin")) return pick(r,"kin-band reserve","forager brood-list","sump sleeping hollow");
        if(has(text,"cult","ritual","chapel","offering")) return pick(r,"cell roster","whispered initiates","offering keepers");
        if(has(text,"sewer","filtration","pump")) return pick(r,"utility guild stragglers","underhive foragers","unregistered transients");
        return pick(r,"hab creche ledger","kitchen rota","local worker dorm");
    }

    static String roleFor(ZoneFacilityLedgerEntry e, ZoneType zt, Random r){
        String text = all(e, zt);
        if(has(text,"orchard","bio-garden","garden")) return "cultivator, vintner, and food-quality attendant";
        if(has(text,"hydroponic","farm","greenhouse")) return "hydroponic tender and crop technician";
        if(has(text,"vat","nutrient","algae","reclamation")) return "vat watcher, slurry mixer, and ration technician";
        if(has(text,"kitchen","mess","galley","canteen")) return "cook, server, ration clerk, and water monitor";
        if(has(text,"warehouse","store","cargo","munition")) return "stocker, porter, counter-signatory, and guard";
        if(has(text,"clinic","aid","medicae")) return "medicae aide and supply orderly";
        if(has(text,"learning","archive","library","chapel","drill")) return "instructor, novice, clerk, and watcher";
        if(has(text,"barracks","housing","dormitory","rest")) return "caretaker, roster clerk, and reserve occupant";
        return "general worker, custodian, and runner";
    }

    static int workerCountFor(ZoneFacilityLedgerEntry e, ZoneType zt, Random r){
        String text = all(e, zt);
        int base = 2 + (r == null ? 0 : r.nextInt(5));
        if(has(text,"warehouse","barracks","mess","orchard","garden","hydroponic","vat","munition","rail","market")) base += 2;
        if(has(text,"noble","governor","guard","mechanist Collegia")) base += 1;
        return Math.max(1, Math.min(12, base));
    }

    static int vacancyCountFor(ZoneFacilityLedgerEntry e, ZoneType zt, Random r){
        String text = all(e, zt);
        int v = (r == null ? 0 : r.nextInt(4));
        if(has(text,"conflict","sewer","gang","mutant","cult")) v += 1;
        return Math.max(0, Math.min(6, v));
    }

    static String outputEffectFor(ZoneFacilityLedgerEntry e, int workers, int vacancies, Random r){
        if(vacancies <= 0 && workers >= 6) return "well-staffed output; high retention and cleaner ledgers";
        if(vacancies >= workers) return "labor shortage; output is sporadic and loss-prone";
        if(vacancies > 0) return "understaffed output; caches are thinner and more irregular";
        return r != null && r.nextBoolean() ? "ordinary output maintained" : "stable but tired production cadence";
    }

    static String noteFor(ZoneFacilityLedgerEntry e, ZoneType zt, Random r){
        String text = all(e, zt);
        if(has(text,"orchard","bio-garden")) return "food quality depends on garden-serf continuity and artificial-sun maintenance";
        if(has(text,"hydroponic","farm")) return "crop yield depends on water access, light arrays, and fungus/algae contamination control";
        if(has(text,"vat","nutrient")) return "vat output depends on ingredient slurry, heat, and technicians willing to smell like the work";
        if(has(text,"munition","armory")) return "worker access is controlled; losses imply theft, requisition, or bad paperwork";
        return pick(r,"assigned from the closest population ledger rather than abstract spawning","workforce continuity is now part of provenance","labor shortages reduce facility output when labor ledgers apply","workers connect rooms, production, and personnel history");
    }

    static String all(ZoneFacilityLedgerEntry e, ZoneType zt){ return (safe(e == null ? null : e.purpose) + " " + safe(e == null ? null : e.roomType) + " " + safe(e == null ? null : e.productFocus) + " " + safe(e == null ? null : e.populationSource) + " " + (zt == null ? "" : zt.label)).toLowerCase(Locale.ROOT); }
    static boolean has(String s, String... vals){ if(s == null) return false; for(String v: vals) if(v != null && !v.isBlank() && s.contains(v.toLowerCase(Locale.ROOT))) return true; return false; }
    static String pick(Random r, String... vals){ return vals[Math.floorMod(r == null ? 0 : r.nextInt(), vals.length)]; }
    static String safe(String s){ return s == null ? "" : s; }
}


class ZoneFacilityHistoryApi {
    private ZoneFacilityHistoryApi() {}
    static final int DEFAULT_BATCH = 96;
    static int advanceFacilityHistory(HiveWorldDefinition d, int maxZones){
        if(d == null) return 0;
        WorldNamingApi.populateDefinition(d);
        int made = 0;
        int limit = Math.max(1, maxZones);
        for(String key: d.zoneNames.keySet()){
            if(d.zoneFacilities.containsKey(key)) continue;
            WorldHistoryApi.ensureZoneEpoch(d, key);
            d.zoneFacilities.put(key, buildFacilityLedger(d, key));
            made++;
            if(made >= limit) break;
        }
        WorldGenerationProgressApi.markFacilityHistoryAdvanced(d);
        if(made > 0) DebugLog.audit("FACILITY_HISTORY_BATCH", "seededFacilityLedgers=" + made + " progress=" + d.zoneFacilities.size() + "/" + WorldGenerationProgressApi.WORLD_ZONE_SLICES + " arcology=" + d.hiveName);
        return made;
    }
    static String ensureZoneFacilities(HiveWorldDefinition d, String key){
        if(d == null || key == null) return "No arcology-world definition loaded for facility history.";
        if(!d.zoneFacilities.containsKey(key)){
            WorldHistoryApi.ensureZoneEpoch(d, key);
            d.zoneFacilities.put(key, buildFacilityLedger(d, key));
            WorldGenerationProgressApi.markFacilityHistoryAdvanced(d);
            DebugLog.audit("FACILITY_HISTORY_ON_DEMAND", "zoneKey=" + key + " arcology=" + d.hiveName);
        }
        return d.zoneFacilities.get(key);
    }
    static java.util.List<String> facilityLines(HiveWorldDefinition d, String key){
        ArrayList<String> out = new ArrayList<>();
        String row = ensureZoneFacilities(d, key);
        if(row == null || row.trim().isEmpty()){ out.add("No facility establishment ledger synthesized."); return out; }
        for(String part: row.split(";;")) if(part != null && !part.trim().isEmpty()) out.add(part.trim());
        return out;
    }
    static java.util.List<ZoneFacilityLedgerEntry> parseFacilityLedger(String ledger){
        ArrayList<ZoneFacilityLedgerEntry> out = new ArrayList<>();
        if(ledger == null || ledger.isBlank()) return out;
        for(String raw: ledger.split(";;")){
            if(raw == null || raw.trim().isEmpty()) continue;
            ZoneFacilityLedgerEntry e = parseFacilityEntry(raw.trim());
            if(e != null) out.add(e);
        }
        return out;
    }
    static ZoneFacilityLedgerEntry parseFacilityEntry(String raw){
        try{
            ZoneFacilityLedgerEntry e = new ZoneFacilityLedgerEntry();
            String text = raw == null ? "" : raw.trim();
            int colon = text.indexOf(':');
            if(colon > 0){ e.id = text.substring(0, colon).trim(); text = text.substring(colon + 1).trim(); }
            String[] parts = text.split(" :: ");
            if(parts.length > 0) e.purpose = parts[0].trim();
            for(String part: parts){
                String p = part.trim();
                if(p.startsWith("by ")) e.establishedBy = p.substring(3).trim();
                else if(p.startsWith("room=")) e.roomType = p.substring(5).trim();
                else if(p.startsWith("output=")) e.productFocus = p.substring(7).trim();
                else if(p.startsWith("people=")) e.populationSource = p.substring(7).trim();
                else if(!p.equals(e.purpose) && !p.startsWith("by ") && !p.startsWith("room=") && !p.startsWith("output=") && !p.startsWith("people=")) e.historicNote = p.trim();
            }
            return e;
        }catch(Exception ex){ return null; }
    }
    static ZoneFacilityLedgerEntry matchFacilityForRoom(World w, RoomPopulationLedger ledger){
        if(w == null || ledger == null) return null;
        java.util.List<ZoneFacilityLedgerEntry> entries = parseFacilityLedger(w.zoneFacilityHistory);
        if(entries.isEmpty()) return null;
        String hay = (ledger.roomName + " " + ledger.sourceKind + " " + ledger.sourceLabel).toLowerCase(Locale.ROOT);
        ZoneFacilityLedgerEntry best = null; int bestScore = Integer.MIN_VALUE;
        for(ZoneFacilityLedgerEntry e: entries){
            int score = 0;
            score += wordOverlapScore(hay, e.roomType);
            score += wordOverlapScore(hay, e.purpose);
            score += wordOverlapScore(hay, e.populationSource);
            if(hay.contains("barracks") && (e.roomType.toLowerCase(Locale.ROOT).contains("barracks") || e.populationSource.toLowerCase(Locale.ROOT).contains("barracks"))) score += 8;
            if((hay.contains("creche") || hay.contains("daycare") || hay.contains("learning")) && (e.purpose.toLowerCase(Locale.ROOT).contains("learning") || e.populationSource.toLowerCase(Locale.ROOT).contains("creche") || e.roomType.toLowerCase(Locale.ROOT).contains("chapel"))) score += 6;
            if((hay.contains("mess") || hay.contains("cafeteria") || hay.contains("kitchen") || hay.contains("galley") || hay.contains("canteen")) && (e.purpose.toLowerCase(Locale.ROOT).contains("food") || e.roomType.toLowerCase(Locale.ROOT).contains("mess") || e.roomType.toLowerCase(Locale.ROOT).contains("galley") || e.roomType.toLowerCase(Locale.ROOT).contains("kitchen") || e.roomType.toLowerCase(Locale.ROOT).contains("canteen"))) score += 7;
            if((hay.contains("warehouse") || hay.contains("store") || hay.contains("evidence") || hay.contains("munition")) && (e.purpose.toLowerCase(Locale.ROOT).contains("storage") || e.productFocus.toLowerCase(Locale.ROOT).contains("goods") || e.roomType.toLowerCase(Locale.ROOT).contains("warehouse") || e.roomType.toLowerCase(Locale.ROOT).contains("store"))) score += 5;
            if(score > bestScore){ bestScore = score; best = e; }
        }
        return bestScore <= 0 ? entries.get(0) : best;
    }
    static int wordOverlapScore(String hay, String text){
        if(hay == null || text == null) return 0;
        int score = 0;
        for(String w: text.toLowerCase(Locale.ROOT).split("[^a-z0-9]+")){
            if(w.length() < 4) continue;
            if(hay.contains(w)) score += 2;
        }
        return score;
    }

    static String buildFacilityLedger(HiveWorldDefinition d, String key){
        int[] k = WorldHistoryApi.parseKey(key);
        int sx=k[0], sy=k[1], zx=k[2], zy=k[3], floor=k[4]; boolean sewer=k[5] == 1;
        ZoneType zt = WorldAtlas.zoneTypeForSlice(zx, zy, floor, sewer);
        if(sx==1&&sy==1&&zx==2&&zy==2&&floor==4&&!sewer) zt = ZoneType.NEUTRAL_CIVILIAN_FLOOR;
        String zoneName = d.zoneNames.getOrDefault(key, "unnamed zone");
        String current = WorldHistoryApi.currentControl(d, key);
        Random r = new Random(d.seed ^ key.hashCode() ^ 0x51F15EEDBABEL);
        ArrayList<ZoneFacilityRecord> records = new ArrayList<>();
        for(String[] spec: facilityManifestFor(zt, r)){
            records.add(new ZoneFacilityRecord(spec[0], current, spec[1], spec[2], spec[3], noteFor(zt, zoneName, r)));
        }
        ArrayList<String> rows = new ArrayList<>();
        int n=1; for(ZoneFacilityRecord rec: records) rows.add("F" + (n++) + ": " + rec.encode());
        return String.join(";;", rows);
    }
    static ArrayList<String[]> facilityManifestFor(ZoneType zt, Random r){
        ArrayList<String[]> rows = new ArrayList<>();
        if(zt==ZoneType.IMPERIAL_GUARD_BILLET){
            rows.add(row("muster and housing", "Brutalist Guard Barracks", "trained replacements and bedspace", "barracks roster"));
            rows.add(row("ration service", "Field Mess Hall", "Guard ration tins and water discipline", "mess duty roster"));
            rows.add(row("military nutrient vats", "Triglyceride Gel and Amino-Porridge Vat Line", "triglyceride gel, amino-porridge, recaf, and military protein stock", "ration-vat crew"));
            rows.add(row("arms issue", "Munition Warehouse", "charge packs, flak, entrenching tools", "quartermaster detail"));
            rows.add(row("training", "Drill Hall", "drill manuals and combat readiness", "training cadre"));
        } else if(zt==ZoneType.MECHANICUS_FORGE_CLOISTER || zt==ZoneType.MECHANICUS_RELIC_DUCT){
            rows.add(row("forge labor retention", "Augmetic Rest Cell", "bound Labor Automaton-adjacent worker recovery", "forge creche ledger"));
            rows.add(row("nutrient service", "Nutrient Reclamation Galley", "nutrient ampoules and reclaimed water", "galley acolytes"));
            rows.add(row("nutrient vat agriculture", "Mechanist Collegia Nutrient Vat Tank Gallery", "soylens viridian algae cakes, corpse-starch slabs, and nutrient ampoules", "vat adepts"));
            rows.add(row("maintenance production", "Diagnostic Bay", "calibration probes, sacred wire, machine oil", "tech-adept duty chain"));
            rows.add(row("learning and doctrine", "Cable Chapel", "catechism strips and access rites", "data-chapel novices"));
        } else if(zt==ZoneType.NOBLE_SERVICE_SPINE || zt==ZoneType.SECTOR_GOVERNORS_MANSION){
            rows.add(row("household habitation", "Lavish Bound Labor Automaton-Tended Dormitory", "servant continuity and noble comforts", "household staff book"));
            rows.add(row("luxury provisioning", "Noble Kitchen Gallery", "preserved delicacies and etiquette supplies", "kitchen retinue"));
            rows.add(row("luxury bio-garden agriculture", "Artificial-Sun Orchard and Bio-Garden", "orchard fruit, bio-garden truffles, caba nuts, high-quality amasec, and ploin juice", "gardeners and vintner-serfs"));
            rows.add(row("secure storage", "House Product Warehouse", "signet wax, luxury ammunition, reserved goods", "estate steward rolls"));
            rows.add(row("education and command", "Private Storefront Salon", "patronage, tutoring, and favors", "household tutor line"));
        } else if(zt==ZoneType.ARBITES_PRECINCT_EDGE){
            rows.add(row("law housing", "Civic Wardens Duty Barracks", "watch rotations", "precinct roster"));
            rows.add(row("detention", "Holding Cell Row", "prisoner intake and restraint kits", "custody ledger"));
            rows.add(row("evidence control", "Evidence Storehouse", "casebooks, seized weapons, citation goods", "evidence clerk pool"));
            rows.add(row("public interface", "Public Complaint Counter", "complaints, warrants, and fear", "counter staff"));
        } else if(zt==ZoneType.ADMINISTRATUM_ARCHIVE){
            rows.add(row("clerk habitation", "Clerk Dormitory Ledger Cell", "clerk continuity", "filing clan dorm"));
            rows.add(row("record production", "Form Product Warehouse", "forms, tags, stamps, permits", "archive worker pool"));
            rows.add(row("learning archive", "Dead-File Vault", "obsolete doctrine and file lore", "ledger apprentices"));
            rows.add(row("public processing", "Queue Pen", "petition traffic and ration disputes", "queue wardens"));
        } else if(zt==ZoneType.SUMP_MARKET || zt==ZoneType.NEUTRAL_RAIL_DEPOT || zt==ZoneType.TRAIN_SERVICE_YARD){
            rows.add(row("transient housing", "Rail Worker Dormitory", "arrivals and day labor", "rail intake ledger"));
            rows.add(row("market food", "Platform Canteen", "water tokens and food stock", "canteen staff"));
            rows.add(row("market hydroponics", "Hydroponic Vendor Farm Rack", "protein grain, marsh-rice, vorder leaves, ploin juice, and caba nuts", "farm-rack tenders"));
            rows.add(row("freight handling", "Cargo Warehouse", "cargo stencil kits, inventory tags, trade goods", "porter guild"));
            rows.add(row("commerce", "Barter Storefront Row", "prices, pawn goods, and rumor", "broker stalls"));
        } else if(zt==ZoneType.GANGER_TURF){
            rows.add(row("crew housing", "Gang Crash Barracks", "replacement gangers and debtors", "crash-room roster"));
            rows.add(row("illicit service", "Gang Mess and Chem Kitchen", "chem food, stimulants, bad water, low-quality amasec, and wall-rat meat", "kitchen heavies"));
            rows.add(row("stolen stock", "Stolen Goods Warehouse", "weapons, armor, and fenced cargo", "fence ledger"));
            rows.add(row("training through violence", "Fighting Pit Side Room", "fighters and intimidation", "pit winners"));
        } else if(zt==ZoneType.MUTANT_WARRENS || zt==ZoneType.MUTANT_SEWER_CAMP){
            rows.add(row("kin sleeping", "Mutant Sleeping Hollow", "brood continuity", "kin-band reserve"));
            rows.add(row("food survival", "Fungus Food Store", "fungus, wall-rat meat, sump water, algae cakes, and bad amasec", "foragers"));
            rows.add(row("scrap hoard", "Scrap Product Hoard", "scrap tools and scavenged gear", "hoard keepers"));
        } else if(zt==ZoneType.CULTIST_SEWER_CAMP){
            rows.add(row("cell habitation", "Cult Sleeper Crypt", "hidden converts", "cell roster"));
            rows.add(row("ritual supply", "Offering Storehouse", "knives, robes, candles, contraband", "offering keepers"));
            rows.add(row("indoctrination", "Hidden Knife Chapel", "doctrine, fear, and loyalty", "whispered initiates"));
        } else if(zt==ZoneType.SEWER_CONDUIT){
            rows.add(row("utility support", "Pump Service Chamber", "pump tools and water salvage", "utility guild stragglers"));
            rows.add(row("filtration", "Filtration Gallery", "filter parts and reclaimed water", "maintenance pair"));
            rows.add(row("sewer agriculture", "Fungus and Wall-Rat Protein Niche", "fungus loaves, wall-rat strips, soylens algae cakes, and reclaimed water", "underhive foragers"));
            rows.add(row("hidden transit", "Cistern Walkway", "movement, smuggling, and danger", "unregistered transients"));
        } else {
            rows.add(row("habitation", "Civilian Dormitory Bay", "households and replacements", "hab creche ledger"));
            rows.add(row("food service", "Communal Kitchen Block", "ration bars, water, and daily meals", "kitchen rota"));
            rows.add(row("civic hydroponics", "Hydroponic Food Rack and Recaf Leaf Room", "protein grain, marsh-rice, vorder leaves, ploin juice, and caba nuts", "hydroponic tenders"));
            rows.add(row("civic storage", "Food Storehouse", "food and water reserves", "ration-board staff"));
            rows.add(row("learning and care", "Creche Learning Cluster", "children, tutors, and local continuity", "creche roll"));
        }
        return rows;
    }
    static String[] row(String purpose, String roomType, String productFocus, String people){ return new String[]{purpose, roomType, productFocus, people}; }
    static String noteFor(ZoneType zt, String zoneName, Random r){
        String[] notes = {"established after an old corridor claim was formalized", "expanded during a ration-pressure generation", "rebuilt after a local collapse", "retained because the current controller needs the room to keep functioning", "inherited from an earlier occupation and repainted rather than replaced"};
        if(zt==ZoneType.MECHANICUS_FORGE_CLOISTER || zt==ZoneType.MECHANICUS_RELIC_DUCT) notes = new String[]{"consecrated over older utility machinery", "retained because the conduits still answer", "rebuilt under a maintenance covenant", "expanded after a sanctity audit"};
        if(zt==ZoneType.NOBLE_SERVICE_SPINE || zt==ZoneType.SECTOR_GOVERNORS_MANSION) notes = new String[]{"lavishly appointed over older service decking", "expanded after household inheritance litigation", "kept beautiful enough to hide the machinery beneath", "staffed by inherited service lines"};
        return notes[Math.floorMod(r.nextInt(), notes.length)] + " in " + WorldHistoryApi.shortZoneName(zoneName);
    }
}

class CampaignWorldApi {
    static Path worldDir(){ return ServerRuntimePaths.singlePlayerWorldDir(); }
    static Path worldFile(HiveWorldDefinition d){ return worldDir().resolve(d.worldId + ".mechworld"); }
    static void saveWorldDefinition(HiveWorldDefinition d){
        if(d == null) return;
        try{
            Files.createDirectories(worldDir());
            Properties p = SaveEfficiencyAuthority.worldDefinitionProperties(d);
            try(OutputStream out = Files.newOutputStream(worldFile(d))){ p.store(out, "The Mechanist generated arcology world definition"); }
            DebugLog.audit("CAMPAIGN_WORLD_SAVE", "file=" + worldFile(d).toAbsolutePath() + " " + d.summary() + " " + SaveEfficiencyAuthority.catalog("world-definition", p));
        }catch(IOException ex){ DebugLog.error("CAMPAIGN_WORLD_SAVE", "Failed to save generated arcology world definition.", ex); }
    }
    static HiveWorldDefinition loadOrCreate(long seed){ return loadOrCreate(seed, WorldSetupSettings.standard()); }
    static HiveWorldDefinition createDefinition(long seed, WorldSetupSettings settings){
        HiveWorldDefinition d = new HiveWorldDefinition(seed);
        d.applySettings(settings == null ? WorldSetupSettings.standard() : settings);
        return d;
    }
    static HiveWorldDefinition loadForSavedRun(long seed, WorldSetupSettings savedSettings){
        HiveWorldDefinition d = loadOrCreate(seed, savedSettings);
        WorldSetupSettings use = savedSettings == null ? WorldSetupSettings.standard() : savedSettings;
        if(!d.settings().encode().equals(use.encode())){
            DebugLog.warn("CAMPAIGN_WORLD_LOAD", "Saved run setup overrides mismatched world definition for seed=" + seed + " world=" + d.settings().encode() + " run=" + use.encode());
            d.applySettings(use);
        }
        return d;
    }
    static HiveWorldDefinition loadOrCreate(long seed, WorldSetupSettings settings){
        HiveWorldDefinition d = createDefinition(seed, settings);
        Path f = worldFile(d);
        if(Files.exists(f)){
            try{
                Properties p = new Properties();
                try(InputStream in = Files.newInputStream(f)){ p.load(in); }
                d = HiveWorldDefinition.readFrom(p, seed);
                if(settings != null && !d.hasExplicitSettings()) d.applySettings(settings);
                DebugLog.audit("CAMPAIGN_WORLD_LOAD", "file=" + f.toAbsolutePath() + " " + d.summary());
            }catch(IOException ex){ DebugLog.error("CAMPAIGN_WORLD_LOAD", "Could not read generated arcology world file; regenerating from seed.", ex); }
        } else {
            saveWorldDefinition(d);
        }
        return d;
    }
}

