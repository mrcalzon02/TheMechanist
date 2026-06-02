package mechanist;

import java.util.*;

/**
 * Shared, low-overhead registry for fixture interaction families.
 *
 * Keeps cooldown, loot, sound, dialogue, hazard, and service conventions in one
 * compact runtime authority rather than scattering duplicate rules per fixture.
 */
final class FixtureInteractionRegistry {
    static final String VERSION = "0.9.10aj";

    enum Family {
        WASTE_NEWS("Waste / Newsprint", "search", "ambient_pipe", "loot: paper/scrap/refuse", "searchable refuse; salvage/decomposition handoff"),
        PUBLIC_REST("Public Bench / Rest", "rest", "ambient_machine", "minor fatigue relief / rumors", "public rest; civic/social hooks"),
        BROADCAST("Broadcast Screen / Radio", "listen/read", "ambient_radio", "INN bulletin / rumor", "broadcast information; faction propaganda/dialogue hooks"),
        CIVIC_SERVICE("Civic Service", "inspect/use", "ambient_door_servo", "civic chit / queue / permit status", "civic-service surface; administration and economy access"),
        MEDICAE("Medicae", "inspect/service", "ambient_chime", "triage status / composure", "healing, recovery, drugs, and infection treatment access"),
        LABORATORY("Laboratory", "inspect/process", "ambient_sparks", "chemical/process status", "reagents, assays, drug synthesis, and knowledge slots"),
        FORGE("Forge / Industrial", "inspect/machine", "ambient_press", "machinery class status", "recipes, maintenance, and construction parity"),
        FACTION_BAR("Faction Bar", "inspect/social", "ambient_radio", "rumor/reputation status", "representative services and recovery jobs"),
        ARBITES_SECURITY("Arbites Precinct", "inspect/security", "ambient_door_servo", "custody/security status", "precinct services, custody surfaces, and security previews"),
        NOBLE_SECURITY("Noble Estate Security", "inspect/security", "ambient_door_servo", "estate security status", "private estate security, wards, and access-control previews"),
        GUARD_PDF_SECURITY("Guard / PDF Defense", "inspect/security", "ambient_door_servo", "field defense status", "Astra Militarum/PDF passive defenses, checkpoint surfaces, and construction previews"),
        ROAD_TRANSIT("Road / Transit", "inspect/transit", "ambient_door_servo", "taxi/vehicle recall status", "Vehicle access profile: health, armor, seats, recall"),
        FOOD_BIO("Food / Farm / Bio", "inspect/grow", "ambient_machine", "food-production profile", "agri-food, vat, cold-store, and bio-growth operation previews"),
        DOMESTIC_HAB("Domestic Hab", "inspect/domestic", "ambient_pipe", "hab/storage profile", "beds, water, cold storage, prep counters, and household surfaces"),
        HAZARD("Hazard Fixture", "warn", "ambient_sparks", "hazard warning", "Hazard damage/mitigation handoff");

        final String label;
        final String interaction;
        final String defaultSound;
        final String lootOrFeedback;
        final String followOnGate;
        Family(String label, String interaction, String defaultSound, String lootOrFeedback, String followOnGate) {
            this.label = label; this.interaction = interaction; this.defaultSound = defaultSound; this.lootOrFeedback = lootOrFeedback; this.followOnGate = followOnGate;
        }
    }

    static final class Definition {
        final String type;
        final Family family;
        final int cooldownTurns;
        final boolean searchable;
        final boolean producesDialogue;
        final boolean producesSound;
        final boolean canTouchInventory;
        final boolean canPreviewService;
        final String notes;
        Definition(String type, Family family, int cooldownTurns, boolean searchable, boolean producesDialogue,
                   boolean producesSound, boolean canTouchInventory, boolean canPreviewService, String notes) {
            this.type = type; this.family = family; this.cooldownTurns = cooldownTurns; this.searchable = searchable;
            this.producesDialogue = producesDialogue; this.producesSound = producesSound; this.canTouchInventory = canTouchInventory;
            this.canPreviewService = canPreviewService; this.notes = notes;
        }
    }

    private static final Map<String, Definition> DEFINITIONS = new LinkedHashMap<>();
    static {
        register(AssetIntegrationDisciplineAuthority.PUBLIC_TRASH_BIN, Family.WASTE_NEWS, 24, true, true, true, true, false, "Trash/newsprint frontage source; supports old newspaper recovery and small scavenge loot.");
        register(AssetIntegrationDisciplineAuthority.PUBLIC_WASTE_RECEPTACLE, Family.WASTE_NEWS, 24, true, true, true, true, false, "Promoted civic waste receptacle; bounded paper/refuse/light-salvage search surface.");
        register(AssetIntegrationDisciplineAuthority.PUBLIC_SMALL_BIN_CLUSTER, Family.WASTE_NEWS, 30, true, true, true, true, false, "Promoted small bin cluster; slightly better scrap/newsprint refuse search surface.");
        register(AssetIntegrationDisciplineAuthority.INN_NEWSPAPER_DISPENSER, Family.WASTE_NEWS, 60, false, true, true, true, true, "Promoted INN newspaper dispenser; buy/read sanctioned paper through existing news interaction.");
        register(AssetIntegrationDisciplineAuthority.DISCARDED_NEWSPRINT_SOURCE, Family.WASTE_NEWS, 18, true, true, false, true, false, "Promoted discarded newsprint source; recover stale INN paper or paper mush.");
        register(AssetIntegrationDisciplineAuthority.PUBLIC_RATION_BENCH, Family.PUBLIC_REST, 8, false, true, true, false, false, "Rest/readability fixture; no item generation beyond rumor/social hooks.");
        register(AssetIntegrationDisciplineAuthority.PUBLIC_PICT_SCREEN, Family.BROADCAST, 36, false, true, true, false, true, "INN/public bulletin source; propaganda and faction bulletin variants.");
        register(AssetIntegrationDisciplineAuthority.CHEAP_RADIO, Family.BROADCAST, 24, false, true, true, false, true, "Low-cost radio/broadcast cue; good for bars, streets, workshops, civic offices.");
        register(AssetIntegrationDisciplineAuthority.PUBLIC_INFO_KIOSK, Family.CIVIC_SERVICE, 24, false, true, true, false, true, "Routing/public notice source; uses compact text-key-friendly state.");
        register(AssetIntegrationDisciplineAuthority.PUBLIC_SERVICE_COUNTER, Family.CIVIC_SERVICE, 60, false, true, true, true, true, "Civic chit/permit counter; administration-economy handoff.");
        register(AssetIntegrationDisciplineAuthority.MEDICAE_FRONTAGE, Family.MEDICAE, 20, false, true, true, false, true, "Exterior/frontage medical signifier; clinic service anchor.");
        register(AssetIntegrationDisciplineAuthority.BAR_FRONTAGE, Family.FACTION_BAR, 20, false, true, true, false, true, "Exterior faction recovery-anchor signifier.");

        register(RoomFixtureInteractionAuthority.MEDICAE_FIXTURE, Family.MEDICAE, 18, false, true, true, false, true, "Interior medical fixture surface; recovery/service preview target.");
        register(AssetIntegrationDisciplineAuthority.MEDICAE_CLINIC_STALL, Family.MEDICAE, 18, false, true, true, false, true, "Clinic treatment stall; patch-up and wound-room service preview target.");
        register(AssetIntegrationDisciplineAuthority.MEDICAE_BACKROOM_STALL, Family.MEDICAE, 18, false, true, true, false, true, "Backroom medicae stall; field treatment and recovery service preview target.");
        register(AssetIntegrationDisciplineAuthority.STERILE_MEDICAE_CLEAN_BENCH, Family.MEDICAE, 18, false, true, true, false, true, "Sterile medicae clean bench; sterile-compounding and clinic-grade service preview target.");
        register(RoomFixtureInteractionAuthority.LAB_FIXTURE, Family.LABORATORY, 18, false, true, true, false, true, "Interior micro-lab fixture surface; sample assay and research operation target.");
        register(AssetIntegrationDisciplineAuthority.CRUDE_CHEM_BENCH_FIXTURE, Family.LABORATORY, 18, false, true, true, false, true, "Crude chemical bench; low-tier reagent mixing and contamination-risk process target.");
        register(AssetIntegrationDisciplineAuthority.REAGENT_PREPARATION_BENCH_FIXTURE, Family.LABORATORY, 18, false, true, true, false, true, "Reagent preparation bench; stable reagent and medical precursor process target.");
        register(AssetIntegrationDisciplineAuthority.DISTILLATION_COLUMN_FIXTURE, Family.LABORATORY, 22, false, true, true, false, true, "Distillation column; liquid separation, solvents, and heat/fume process target.");
        register(AssetIntegrationDisciplineAuthority.FUME_HOOD_FIXTURE, Family.LABORATORY, 22, false, true, true, false, true, "Fume hood; toxic handling, aerosol, and volatile-process safety target.");
        register(AssetIntegrationDisciplineAuthority.INJECTOR_FILLING_STATION_FIXTURE, Family.LABORATORY, 20, false, true, true, false, true, "Injector filling station; ampoule packaging and dosing-station process target.");
        register(AssetIntegrationDisciplineAuthority.SCRAP_WORKBENCH_FIXTURE, Family.FORGE, 16, false, true, true, false, true, "Scrap workbench fixture; repair, salvage sorting, and manual fabrication operation target.");
        register(AssetIntegrationDisciplineAuthority.EMM_MICRO_FORGE_FIXTURE, Family.FORGE, 20, false, true, true, false, true, "EMM micro forge fixture; scrap-forging and construction-supply operation target.");
        register(AssetIntegrationDisciplineAuthority.EMM_ATMOSPHERIC_CONDENSER_FIXTURE, Family.FORGE, 18, false, true, true, false, true, "Atmospheric condenser fixture; water capture and reclamation support operation target.");
        register(AssetIntegrationDisciplineAuthority.MACHINE_MAINTENANCE_RACK_FIXTURE, Family.FORGE, 14, false, true, true, false, true, "Machine maintenance rack; repair staging and component issue operation target.");
        register(RoomFixtureInteractionAuthority.BAR_FIXTURE, Family.FACTION_BAR, 18, false, true, true, false, true, "Interior faction bar surface; patron/reputation service target.");
        register(AssetIntegrationDisciplineAuthority.BAR_COUNTER_LONG_FIXTURE, Family.FACTION_BAR, 18, false, true, true, false, true, "Long bar counter; drinks, rumor, and recovery service preview target.");
        register(AssetIntegrationDisciplineAuthority.BAR_BOOTH_FIXTURE, Family.FACTION_BAR, 14, false, true, true, false, true, "Bar booth; private meeting, rumor, and social cover preview target.");
        register(AssetIntegrationDisciplineAuthority.BAR_STOOL_FIXTURE, Family.FACTION_BAR, 10, false, true, true, false, true, "Bar stool cluster; patron density, listening, and short rest preview target.");
        register(AssetIntegrationDisciplineAuthority.BAR_BOTTLE_SHELF_FIXTURE, Family.FACTION_BAR, 18, false, true, true, false, true, "Bottle shelf; bar stock, amasec quality, and contraband-suspicion preview target.");
        register(AssetIntegrationDisciplineAuthority.SERVICE_KEG_FIXTURE, Family.FACTION_BAR, 18, false, true, true, false, true, "Service keg; drink stock, bar supply, and fatigue-relief preview target.");
        register(AssetIntegrationDisciplineAuthority.MARKET_COUNTER_FIXTURE, Family.FACTION_BAR, 18, false, true, true, false, true, "Market counter; barter, licensed shop, and informal trade service preview target.");
        register(AssetIntegrationDisciplineAuthority.ARBITES_COMMAND_DESK_FIXTURE, Family.ARBITES_SECURITY, 20, false, true, true, false, true, "Arbites command desk; complaint intake, patrol assignment, and precinct service preview target.");
        register(AssetIntegrationDisciplineAuthority.ARBITES_SERGEANT_DESK_FIXTURE, Family.ARBITES_SECURITY, 20, false, true, true, false, true, "Arbites sergeant desk; duty orders, checkpoint ownership, and staffing preview target.");
        register(AssetIntegrationDisciplineAuthority.ARBITES_COFFEE_MAKER_FIXTURE, Family.ARBITES_SECURITY, 10, false, true, true, false, true, "Precinct recaf maker; mess-room fatigue relief and shift-continuity preview target.");
        register(AssetIntegrationDisciplineAuthority.ARBITES_WEAPON_LOCKER_FIXTURE, Family.ARBITES_SECURITY, 24, false, true, true, false, true, "Weapon locker; controlled gear issue and armory custody preview target.");
        register(AssetIntegrationDisciplineAuthority.ARBITES_PERP_BENCH_FIXTURE, Family.ARBITES_SECURITY, 16, false, true, true, false, true, "Perp bench; intake, witness waiting, and restraint preview target.");
        register(AssetIntegrationDisciplineAuthority.ARBITES_INTERROGATION_TABLE_FIXTURE, Family.ARBITES_SECURITY, 22, false, true, true, false, true, "Interrogation table; questioning, evidence review, and investigation preview target.");
        register(AssetIntegrationDisciplineAuthority.ARBITES_HOLDING_CELL_FIXTURE, Family.ARBITES_SECURITY, 24, false, true, true, false, true, "Holding cell fixture; detention and custody-space preview target.");
        register(AssetIntegrationDisciplineAuthority.ARBITES_PRECINCT_DOOR_FIXTURE, Family.ARBITES_SECURITY, 24, false, true, true, false, true, "Precinct door fixture; access-control and partitioning preview target.");
        register(AssetIntegrationDisciplineAuthority.ARBITES_PRECINCT_SIGN_FIXTURE, Family.ARBITES_SECURITY, 18, false, true, true, false, true, "Precinct sign; public routing and jurisdiction preview target.");
        register(AssetIntegrationDisciplineAuthority.ARBITES_ALARM_PANEL_FIXTURE, Family.ARBITES_SECURITY, 24, false, true, true, false, true, "Alarm panel; sensor, alarm, and lockdown preview target.");
        register(AssetIntegrationDisciplineAuthority.ARBITES_EVIDENCE_LOCKER_FIXTURE, Family.ARBITES_SECURITY, 24, false, true, true, false, true, "Evidence locker; property custody and investigation-service preview target.");
        register(AssetIntegrationDisciplineAuthority.NOBLE_WALL_PANEL_FIXTURE, Family.NOBLE_SECURITY, 24, false, true, true, false, true, "Noble wall panel; estate perimeter and hard-room partition preview target.");
        register(AssetIntegrationDisciplineAuthority.NOBLE_GATE_FIXTURE, Family.NOBLE_SECURITY, 24, false, true, true, false, true, "Noble security gate; estate access-control and service checkpoint preview target.");
        register(AssetIntegrationDisciplineAuthority.NOBLE_CORNER_TOWER_FIXTURE, Family.NOBLE_SECURITY, 26, false, true, true, false, true, "Noble corner tower; overwatch and estate sightline preview target.");
        register(AssetIntegrationDisciplineAuthority.NOBLE_GILDED_SENTRY_FIXTURE, Family.NOBLE_SECURITY, 28, false, true, true, false, true, "Gilded sentry turret; private turret and ownership-binding preview target.");
        register(AssetIntegrationDisciplineAuthority.NOBLE_SHIELD_RELAY_FIXTURE, Family.NOBLE_SECURITY, 28, false, true, true, false, true, "Noble shield relay; protected-room and ward coverage preview target.");
        register(AssetIntegrationDisciplineAuthority.NOBLE_VOID_SHIELD_DOME_FIXTURE, Family.NOBLE_SECURITY, 30, false, true, true, false, true, "Noble void-shield dome; high-tier warding and panic-room preview target.");
        register(AssetIntegrationDisciplineAuthority.NOBLE_LASER_PYLON_FIXTURE, Family.NOBLE_SECURITY, 28, false, true, true, false, true, "Noble laser pylon; beam-denial hardpoint and line-of-sight preview target.");
        register(AssetIntegrationDisciplineAuthority.NOBLE_ENERGY_FENCE_FIXTURE, Family.NOBLE_SECURITY, 26, false, true, true, false, true, "Noble energy fence; perimeter-denial and access-channel preview target.");
        register(AssetIntegrationDisciplineAuthority.NOBLE_SECURITY_PANEL_FIXTURE, Family.NOBLE_SECURITY, 20, false, true, true, false, true, "Noble security panel; alarm routing, lock coordination, and sensor-status preview target.");
        register(AssetIntegrationDisciplineAuthority.PDF_WALL_PANEL_FIXTURE, Family.GUARD_PDF_SECURITY, 22, false, true, true, false, true, "PDF wall panel; field perimeter and reinforced-wall preview target.");
        register(AssetIntegrationDisciplineAuthority.PDF_WALL_CORNER_FIXTURE, Family.GUARD_PDF_SECURITY, 22, false, true, true, false, true, "PDF wall corner; defensive-line corner and checkpoint-shaping preview target.");
        register(AssetIntegrationDisciplineAuthority.PDF_GATE_FIXTURE, Family.GUARD_PDF_SECURITY, 24, false, true, true, false, true, "PDF checkpoint gate; access-control and reinforced-door preview target.");
        register(AssetIntegrationDisciplineAuthority.PDF_WALL_DAMAGED_FIXTURE, Family.GUARD_PDF_SECURITY, 18, false, true, true, false, true, "Damaged PDF wall; repair/decomposition and battlefield-wear preview target.");
        register(AssetIntegrationDisciplineAuthority.PDF_TURRET_MK1_FIXTURE, Family.GUARD_PDF_SECURITY, 28, false, true, true, false, true, "PDF turret Mk I; dormant turret and heavy-stub preview target.");
        register(AssetIntegrationDisciplineAuthority.PDF_TURRET_MK2_FIXTURE, Family.GUARD_PDF_SECURITY, 30, false, true, true, false, true, "PDF turret Mk II; dormant turret and heavy-stub preview target.");
        register(AssetIntegrationDisciplineAuthority.PDF_TURRET_MK3_FIXTURE, Family.GUARD_PDF_SECURITY, 32, false, true, true, false, true, "PDF turret Mk III; dormant powered-defense preview target.");
        register(AssetIntegrationDisciplineAuthority.PDF_SANDBAG_BARRICADE_FIXTURE, Family.GUARD_PDF_SECURITY, 18, false, true, true, false, true, "Field sandbag barricade; sandbag-line and passive-cover preview target.");
        register(AssetIntegrationDisciplineAuthority.PDF_SANDBAG_CORNER_FIXTURE, Family.GUARD_PDF_SECURITY, 18, false, true, true, false, true, "Field sandbag corner; cornered cover and roadblock preview target.");
        register(AssetIntegrationDisciplineAuthority.GUARD_BARRACKS_ANCHOR_FIXTURE, Family.GUARD_PDF_SECURITY, 24, false, true, true, false, true, "Guard barracks defense anchor; staffing and response-point preview target.");
        register(AssetIntegrationDisciplineAuthority.GUARD_WATCH_POST_FIXTURE, Family.GUARD_PDF_SECURITY, 20, false, true, true, false, true, "Guard watch post; warning/sightline and watch-post preview target.");
        register(AssetIntegrationDisciplineAuthority.GUARD_SUPPLY_POST_FIXTURE, Family.GUARD_PDF_SECURITY, 20, false, true, true, false, true, "Guard supply post; checkpoint supply and logistics-security preview target.");
        register(RoomFixtureInteractionAuthority.CIVIC_FIXTURE, Family.CIVIC_SERVICE, 18, false, true, true, false, true, "Interior public-service fixture surface; civic service target.");

        register(AssetIntegrationDisciplineAuthority.ROAD_ALCOVE_FIXTURE, Family.ROAD_TRANSIT, 18, false, true, true, false, true, "Roadside alcove; frontage attachment and foot-traffic readability surface.");
        register(AssetIntegrationDisciplineAuthority.PARK_OPEN_SPACE, Family.ROAD_TRANSIT, 18, false, true, true, false, true, "Roadside park/open-space fixture; civic readability and low-risk rest/social handoff surface.");
        register(AssetIntegrationDisciplineAuthority.PARKING_LOT_MARKER, Family.ROAD_TRANSIT, 30, false, true, true, false, true, "Vehicle set-down/parking profile with readable access points.");
        register(AssetIntegrationDisciplineAuthority.TAXI_BOOTH, Family.ROAD_TRANSIT, 30, false, true, true, false, true, "Taxi/vehicle call surface; boarding and fare execution use separate vehicle authority.");
        register(AssetIntegrationDisciplineAuthority.ROAD_VEHICLE_STAGING_MARKER, Family.ROAD_TRANSIT, 24, false, true, true, false, true, "Legacy vehicle-staging marker; passive readable road-transit fixture.");
        register(AssetIntegrationDisciplineAuthority.PARKED_CIVILIAN_CAR, Family.ROAD_TRANSIT, 24, false, true, true, false, true, "Parked civilian car; passive vehicle profile with armor/seats metadata.");
        register(AssetIntegrationDisciplineAuthority.PARKED_CARGO_TRUCK, Family.ROAD_TRANSIT, 24, false, true, true, false, true, "Parked cargo truck; passive cargo vehicle profile with sealed cargo metadata.");
        register(AssetIntegrationDisciplineAuthority.PARKED_UTILITY_BIKE, Family.ROAD_TRANSIT, 20, false, true, true, false, true, "Parked utility bike; passive light vehicle profile.");
        register(AssetIntegrationDisciplineAuthority.PARKED_ARMORED_CAR, Family.ROAD_TRANSIT, 28, false, true, true, false, true, "Parked armored car; passive security vehicle profile.");
        register(AssetIntegrationDisciplineAuthority.PARKED_TANK, Family.ROAD_TRANSIT, 32, false, true, true, false, true, "Parked heavy armor marker; passive vehicle profile without live weapon behavior.");
        register(AssetIntegrationDisciplineAuthority.ALGAE_TANK_FIXTURE, Family.FOOD_BIO, 18, false, true, true, false, true, "Algae tank; vat culture, synthetic food base, and water/nutrient handling preview target.");
        register(AssetIntegrationDisciplineAuthority.HYDROPONICS_BED_FIXTURE, Family.FOOD_BIO, 18, false, true, true, false, true, "Hydroponics bed; crop stock, leaf/grain trays, and agriculture staffing preview target.");
        register(AssetIntegrationDisciplineAuthority.ANIMAL_PEN_FIXTURE, Family.FOOD_BIO, 22, false, true, true, false, true, "Animal pen; livestock holding and farm-beast custody preview target.");
        register(AssetIntegrationDisciplineAuthority.CLONING_VAT_FIXTURE, Family.FOOD_BIO, 24, false, true, true, false, true, "Cloning vat; bio-growth and tissue-culture preview target.");
        register(AssetIntegrationDisciplineAuthority.FUNGAL_GROW_TRAY_FIXTURE, Family.FOOD_BIO, 18, false, true, true, false, true, "Fungal grow tray; sump fungus, spore culture, and underhive agriculture preview target.");
        register(AssetIntegrationDisciplineAuthority.REFRIGERATED_FOOD_STORE_FIXTURE, Family.FOOD_BIO, 16, false, true, true, false, true, "Refrigerated food store; cold storage, spoilage control, and ration custody preview target.");
        register(AssetIntegrationDisciplineAuthority.NUTRIENT_VAT_FIXTURE, Family.FOOD_BIO, 20, false, true, true, false, true, "Nutrient vat; slurry, ration paste inputs, and provisioning-vat preview target.");
        register(AssetIntegrationDisciplineAuthority.HAB_COT_FIXTURE, Family.DOMESTIC_HAB, 14, false, true, true, false, true, "Hab cot; sleep surface, personal effects trunk, and hab-cell staging preview target.");
        register(AssetIntegrationDisciplineAuthority.GUARD_COT_FIXTURE, Family.DOMESTIC_HAB, 14, false, true, true, false, true, "Guard cot; billet sleep surface, uniform kit slot, and barracks staging preview target.");
        register(AssetIntegrationDisciplineAuthority.WORN_BED_FIXTURE, Family.DOMESTIC_HAB, 14, false, true, true, false, true, "Worn bed; underhive sleep surface and personal stash preview target.");
        register(AssetIntegrationDisciplineAuthority.NOBLE_BED_FIXTURE, Family.DOMESTIC_HAB, 16, false, true, true, false, true, "Noble bed; estate rest surface and servant-route preview target.");
        register(AssetIntegrationDisciplineAuthority.BUNK_BED_FIXTURE, Family.DOMESTIC_HAB, 14, false, true, true, false, true, "Bunk bed; dormitory sleep capacity and shared locker preview target.");
        register(AssetIntegrationDisciplineAuthority.DOMESTIC_WATER_STORAGE_FIXTURE, Family.DOMESTIC_HAB, 12, false, true, true, false, true, "Domestic water storage; potable water custody and wash-access preview target.");
        register(AssetIntegrationDisciplineAuthority.DOMESTIC_REFRIGERATOR_FIXTURE, Family.DOMESTIC_HAB, 14, false, true, true, false, true, "Domestic refrigerator; household cold-storage and ration custody preview target.");
        register(AssetIntegrationDisciplineAuthority.DOMESTIC_SINK_FIXTURE, Family.DOMESTIC_HAB, 12, false, true, true, false, true, "Sink counter; washing surface and domestic prep preview target.");
        register(AssetIntegrationDisciplineAuthority.DOMESTIC_STOVE_FIXTURE, Family.DOMESTIC_HAB, 14, false, true, true, false, true, "Stove counter; cooking surface and domestic ration-prep preview target.");
        register(AssetIntegrationDisciplineAuthority.DOMESTIC_PREP_COUNTER_FIXTURE, Family.DOMESTIC_HAB, 12, false, true, true, false, true, "Domestic prep counter; small worktop and household preparation preview target.");
        register(AssetIntegrationDisciplineAuthority.DOMESTIC_STORAGE_CABINET_FIXTURE, Family.DOMESTIC_HAB, 12, false, true, true, false, true, "Domestic storage cabinet; personal goods and small-container preview target.");
        register(AssetIntegrationDisciplineAuthority.DOMESTIC_PLANK_TABLE_FIXTURE, Family.DOMESTIC_HAB, 12, false, true, true, false, true, "Plank table; common meal/social surface preview target.");
        register(AssetIntegrationDisciplineAuthority.DOMESTIC_ROUND_TABLE_FIXTURE, Family.DOMESTIC_HAB, 12, false, true, true, false, true, "Round table; apartment meal/social surface preview target.");
        register(AssetIntegrationDisciplineAuthority.DOMESTIC_MESS_TABLE_FIXTURE, Family.DOMESTIC_HAB, 12, false, true, true, false, true, "Mess table; shared eating and barracks/apartment surface preview target.");
        register(AssetIntegrationDisciplineAuthority.DOMESTIC_ORNATE_TABLE_FIXTURE, Family.DOMESTIC_HAB, 16, false, true, true, false, true, "Ornate dining table; noble dining surface and servant-route preview target.");
    }

    private static void register(String type, Family family, int cooldownTurns, boolean searchable, boolean producesDialogue,
                                 boolean producesSound, boolean canTouchInventory, boolean canPreviewService, String notes) {
        DEFINITIONS.put(type, new Definition(type, family, cooldownTurns, searchable, producesDialogue, producesSound, canTouchInventory, canPreviewService, notes));
    }

    static Definition definitionFor(String type) { return DEFINITIONS.get(AssetIntegrationDisciplineAuthority.canonicalType(type)); }
    static Collection<Definition> definitions() { return Collections.unmodifiableCollection(DEFINITIONS.values()); }
    static boolean isKnownFixture(String type) { return DEFINITIONS.containsKey(AssetIntegrationDisciplineAuthority.canonicalType(type)); }

    static int cooldownFor(String type, int fallback) {
        Definition d = definitionFor(type);
        return d == null ? fallback : d.cooldownTurns;
    }

    static String soundFor(String type, String fallback) {
        Definition d = definitionFor(type);
        return d == null || d.family.defaultSound == null ? fallback : d.family.defaultSound;
    }

    static String auditSummary() {
        Map<Family, Integer> counts = new EnumMap<>(Family.class);
        for (Definition d : DEFINITIONS.values()) counts.put(d.family, counts.getOrDefault(d.family, 0) + 1);
        StringBuilder sb = new StringBuilder("fixtureRegistry version=").append(VERSION).append(" definitions=").append(DEFINITIONS.size());
        for (Map.Entry<Family,Integer> e : counts.entrySet()) sb.append("; ").append(e.getKey().name()).append('=').append(e.getValue());
        return sb.toString();
    }}
