package mechanist;

import java.util.*;

/**
 * Infrastructure promotion registry.
 *
 * Connects fixture families to constructible infrastructure profiles, build
 * recipes, decomposition classes, knowledge gates, and operation lanes without
 * creating duplicate production systems.
 */
final class InfrastructurePromotionRegistry {
    static final String VERSION = "0.9.10aj";

    enum DecompositionClass {
        LIGHT_FIXTURE("light fixture", "simple frame, rivets, small fittings"),
        MEDICAE_STALL("medicae stall", "sterile surface, cabinet, vial rack, filter cloth, chemical bottle"),
        LAB_BENCH("laboratory bench", "bench frame, reagent rack, glassware, circuit faceplate, waste trap"),
        CHEM_COLUMN("chemical column", "sealed column, coils, pipe couplings, pressure hose, waste trap"),
        FORGE_MACHINE("forge machine", "heavy frame, gear train, motor coil, bearings, heat sink, insulators"),
        CIVIC_COUNTER("civic counter", "counter shell, ledger drawer, terminal plate, queue marker"),
        BAR_SERVICE("bar service", "counter, keg line, cooler, radio/pict receiver, seating"),
        ARBITES_PRECINCT("Arbites precinct fixture", "serialized casing, lock core, civic tags, official surface, security fittings"),
        NOBLE_SECURITY("noble estate security", "gilded casing, house hallmark, lock core, sensor lens, power coupling, ornamental plating"),
        GUARD_PDF_DEFENSE("Guard/PDF field defense", "drab plate, sandbag cloth, serial stencil, armament fittings, power coupling, field fasteners"),
        ROAD_TRANSIT("road transit", "booth, sign plate, service terminal, vehicle set-down marker"),
        FOOD_BIO_PRODUCTION("food/bio production", "grow bed, vat casing, nutrient line, cold-store seal, animal pen hardware"),
        DOMESTIC_HAB_FIXTURE("domestic hab fixture", "bed frame, water vessel, cold-box panel, cabinet shell, table boards, small fittings");

        final String label;
        final String summary;
        DecompositionClass(String label, String summary){ this.label = label; this.summary = summary; }
    }

    enum OperationLane {
        PLAYER_BUILDABLE,
        FACTION_BUILDABLE,
        ASSIGNABLE_WORKER_MACHINE,
        SERVICE_PREVIEW_ONLY,
        RECIPE_PROCESSOR_HANDOFF
    }

    static final class Promotion {
        final String id;
        final String fixtureType;
        final FixtureInteractionRegistry.Family family;
        final DecompositionClass decomposition;
        final OperationLane[] lanes;
        final String buildRecipeName;
        final String requiredKnowledge;
        final String qualityFloor;
        final String artSemanticKey;
        final String currentStatus;
        final String currentScope;

        Promotion(String id, String fixtureType, FixtureInteractionRegistry.Family family,
                  DecompositionClass decomposition, String buildRecipeName, String requiredKnowledge,
                  String qualityFloor, String artSemanticKey, String currentStatus, String currentScope,
                  OperationLane... lanes) {
            this.id = id;
            this.fixtureType = fixtureType;
            this.family = family;
            this.decomposition = decomposition;
            this.buildRecipeName = buildRecipeName;
            this.requiredKnowledge = requiredKnowledge == null ? "" : requiredKnowledge;
            this.qualityFloor = qualityFloor == null || qualityFloor.isBlank() ? "Common" : qualityFloor;
            this.artSemanticKey = artSemanticKey == null ? "" : artSemanticKey;
            this.currentStatus = currentStatus == null ? "" : currentStatus;
            this.currentScope = currentScope == null ? "" : currentScope;
            this.lanes = lanes == null ? new OperationLane[0] : lanes;
        }

        String compactLine() {
            return id + " -> " + buildRecipeName + " | fixture=" + fixtureType + " | family=" + family.label +
                    " | decomposition=" + decomposition.label + " | knowledge=" + (requiredKnowledge.isBlank()?"none":requiredKnowledge) +
                    " | quality>=" + qualityFloor + " | lanes=" + Arrays.toString(lanes);
        }

        String playerFeedback() {
            String scope = currentScope.isBlank() ? "No additional scope text." : currentScope;
            return "INFRASTRUCTURE PROFILE: " + buildRecipeName + ". " +
                    "Decomposition: " + decomposition.summary + ". Status: " + currentStatus +
                    ". Scope: " + scope;
        }
    }

    private static final ArrayList<Promotion> PROMOTIONS = new ArrayList<>();
    private static final Map<String, Promotion> BY_FIXTURE = new LinkedHashMap<>();
    private static final Map<String, Promotion> BY_RECIPE = new LinkedHashMap<>();

    static {
        promote(new Promotion(
                "infra.medicae.room-fixture",
                RoomFixtureInteractionAuthority.MEDICAE_FIXTURE,
                FixtureInteractionRegistry.Family.MEDICAE,
                DecompositionClass.MEDICAE_STALL,
                "Backroom Medicae Stall",
                "Field Medicae Practices",
                "Common",
                "build_backroom_medicae_stall",
                "Generic medicae room fixture remains a compatibility and low-detail clinic surface.",
                "Service profile: clinic identity, recovery preview, and medicae-family handoff.",
                OperationLane.PLAYER_BUILDABLE, OperationLane.FACTION_BUILDABLE, OperationLane.SERVICE_PREVIEW_ONLY));

        promote(new Promotion(
                "infra.medicae.backroom-stall",
                AssetIntegrationDisciplineAuthority.MEDICAE_BACKROOM_STALL,
                FixtureInteractionRegistry.Family.MEDICAE,
                DecompositionClass.MEDICAE_STALL,
                "Backroom Medicae Stall",
                "Field Medicae Practices",
                "Common",
                "build_backroom_medicae_stall",
                "Fixture can be inspected and mapped to the existing clinic-stall build recipe.",
                "Clinic-service profile: patch-up, recovery, infection treatment, staffing, and medicae loot categories.",
                OperationLane.PLAYER_BUILDABLE, OperationLane.FACTION_BUILDABLE, OperationLane.SERVICE_PREVIEW_ONLY));

        promote(new Promotion(
                "infra.medicae.clinic-stall",
                AssetIntegrationDisciplineAuthority.MEDICAE_CLINIC_STALL,
                FixtureInteractionRegistry.Family.MEDICAE,
                DecompositionClass.MEDICAE_STALL,
                "Backroom Medicae Stall",
                "Field Medicae Practices",
                "Common",
                "build_clinic_stall",
                "Clinic stall is a readable treatment counter using the same early medicae build lane.",
                "Service profile: wound-room counter, patch-up preview, bandage issue, and medicae-room identity.",
                OperationLane.PLAYER_BUILDABLE, OperationLane.FACTION_BUILDABLE, OperationLane.SERVICE_PREVIEW_ONLY));

        promote(new Promotion(
                "infra.medicae.clean-bench",
                AssetIntegrationDisciplineAuthority.STERILE_MEDICAE_CLEAN_BENCH,
                FixtureInteractionRegistry.Family.MEDICAE,
                DecompositionClass.MEDICAE_STALL,
                "Sterile medicae clean bench",
                "Fine Medical Processing Patterns",
                "Fine",
                "build_sterile_medicae_clean_bench",
                "Existing BuildRecipe is available as a medicae production target rather than generic lab decor.",
                "Production profile: ampoules, sterile compounds, clinic-grade processing, and staffing parity.",
                OperationLane.PLAYER_BUILDABLE, OperationLane.FACTION_BUILDABLE, OperationLane.ASSIGNABLE_WORKER_MACHINE, OperationLane.RECIPE_PROCESSOR_HANDOFF));

        promote(new Promotion(
                "infra.lab.micro-lab",
                RoomFixtureInteractionAuthority.LAB_FIXTURE,
                FixtureInteractionRegistry.Family.LABORATORY,
                DecompositionClass.LAB_BENCH,
                "EMM Micro Lab",
                "",
                "Common",
                "build_emm_micro_lab",
                "Existing emergency build recipe becomes the entry lab infrastructure profile.",
                "Research profile: sample assay, knowledge slots, research assignment, and lab staffing parity.",
                OperationLane.PLAYER_BUILDABLE, OperationLane.FACTION_BUILDABLE, OperationLane.ASSIGNABLE_WORKER_MACHINE, OperationLane.RECIPE_PROCESSOR_HANDOFF));

        promote(new Promotion(
                "infra.lab.crude-chem-bench",
                AssetIntegrationDisciplineAuthority.CRUDE_CHEM_BENCH_FIXTURE,
                FixtureInteractionRegistry.Family.LABORATORY,
                DecompositionClass.LAB_BENCH,
                "Crude chem bench",
                "Junk Chemical Synthesis Patterns",
                "Shoddy",
                "build_crude_chem_bench",
                "Existing BuildRecipe is promoted as the low-tier chemical processing station.",
                "Process profile: reagent mixing, solvent handling, drug/medicine precursors, contamination, and waste output.",
                OperationLane.PLAYER_BUILDABLE, OperationLane.FACTION_BUILDABLE, OperationLane.ASSIGNABLE_WORKER_MACHINE, OperationLane.RECIPE_PROCESSOR_HANDOFF));

        promote(new Promotion(
                "infra.lab.reagent-preparation-bench",
                AssetIntegrationDisciplineAuthority.REAGENT_PREPARATION_BENCH_FIXTURE,
                FixtureInteractionRegistry.Family.LABORATORY,
                DecompositionClass.LAB_BENCH,
                "Reagent preparation bench",
                "Common Chemical Synthesis Patterns",
                "Common",
                "build_reagent_preparation_bench",
                "Existing BuildRecipe is promoted as the stable reagent preparation station.",
                "Process profile: reagent preparation, medical precursor handling, stable compounds, and lab staffing parity.",
                OperationLane.PLAYER_BUILDABLE, OperationLane.FACTION_BUILDABLE, OperationLane.ASSIGNABLE_WORKER_MACHINE, OperationLane.RECIPE_PROCESSOR_HANDOFF));

        promote(new Promotion(
                "infra.lab.distillation-column",
                AssetIntegrationDisciplineAuthority.DISTILLATION_COLUMN_FIXTURE,
                FixtureInteractionRegistry.Family.LABORATORY,
                DecompositionClass.CHEM_COLUMN,
                "Distillation column",
                "Serviceable Chemical Synthesis Patterns",
                "Serviceable",
                "build_distillation_column",
                "Existing BuildRecipe is promoted for liquids, fumes, solvents, and amasec-type refining.",
                "Process profile: liquid refining, volatile hazards, industrial heat/noise, and legal-status metadata.",
                OperationLane.PLAYER_BUILDABLE, OperationLane.FACTION_BUILDABLE, OperationLane.ASSIGNABLE_WORKER_MACHINE, OperationLane.RECIPE_PROCESSOR_HANDOFF));

        promote(new Promotion(
                "infra.lab.fume-hood",
                AssetIntegrationDisciplineAuthority.FUME_HOOD_FIXTURE,
                FixtureInteractionRegistry.Family.LABORATORY,
                DecompositionClass.CHEM_COLUMN,
                "Fume hood",
                "Fine Chemical Synthesis Patterns",
                "Fine",
                "build_fume_hood",
                "Existing BuildRecipe is promoted as the toxic and volatile handling station.",
                "Process profile: toxic handling, aerosol work, volatile safety, hazard mitigation, and lab staffing parity.",
                OperationLane.PLAYER_BUILDABLE, OperationLane.FACTION_BUILDABLE, OperationLane.ASSIGNABLE_WORKER_MACHINE, OperationLane.RECIPE_PROCESSOR_HANDOFF));

        promote(new Promotion(
                "infra.lab.injector-filling-station",
                AssetIntegrationDisciplineAuthority.INJECTOR_FILLING_STATION_FIXTURE,
                FixtureInteractionRegistry.Family.LABORATORY,
                DecompositionClass.LAB_BENCH,
                "Injector filling station",
                "Serviceable Medical Processing Patterns",
                "Serviceable",
                "build_injector_filling_station",
                "Existing BuildRecipe is promoted as the ampoule and precision-dosing station.",
                "Process profile: ampoule filling, injector packaging, medicae precursor handling, and dosing-station staffing parity.",
                OperationLane.PLAYER_BUILDABLE, OperationLane.FACTION_BUILDABLE, OperationLane.ASSIGNABLE_WORKER_MACHINE, OperationLane.RECIPE_PROCESSOR_HANDOFF));

        promote(new Promotion(
                "infra.forge.scrap-workbench",
                AssetIntegrationDisciplineAuthority.SCRAP_WORKBENCH_FIXTURE,
                FixtureInteractionRegistry.Family.FORGE,
                DecompositionClass.FORGE_MACHINE,
                "Scrap Workbench",
                "",
                "Common",
                "build_scrap_workbench",
                "Existing BuildRecipe is promoted as the entry workshop and repair bench profile.",
                "Industrial profile: manual fabrication, salvage sorting, repair actions, basic part staging, and worker-machine handoff.",
                OperationLane.PLAYER_BUILDABLE, OperationLane.FACTION_BUILDABLE, OperationLane.ASSIGNABLE_WORKER_MACHINE, OperationLane.RECIPE_PROCESSOR_HANDOFF));

        promote(new Promotion(
                "infra.forge.micro-forge",
                AssetIntegrationDisciplineAuthority.EMM_MICRO_FORGE_FIXTURE,
                FixtureInteractionRegistry.Family.FORGE,
                DecompositionClass.FORGE_MACHINE,
                "EMM Micro Forge",
                "Scrap-Forging Doctrine",
                "Common",
                "build_emm_micro_forge",
                "Existing Mechanicus-aligned BuildRecipe becomes the entry industrial machine anchor.",
                "Industrial profile: construction parity, repair parts, machine maintenance, decomposition outputs, and workshop staffing.",
                OperationLane.PLAYER_BUILDABLE, OperationLane.FACTION_BUILDABLE, OperationLane.ASSIGNABLE_WORKER_MACHINE, OperationLane.RECIPE_PROCESSOR_HANDOFF));

        promote(new Promotion(
                "infra.forge.atmospheric-condenser",
                AssetIntegrationDisciplineAuthority.EMM_ATMOSPHERIC_CONDENSER_FIXTURE,
                FixtureInteractionRegistry.Family.FORGE,
                DecompositionClass.FORGE_MACHINE,
                "EMM Atmospheric Condenser",
                "Condensation Handling",
                "Common",
                "build_emm_atmospheric_condenser",
                "Existing BuildRecipe is promoted as the water-capture and pipe-room support machine profile.",
                "Industrial profile: water capture, filtration support, reclamation-adjacent processing, and utility-room staffing.",
                OperationLane.PLAYER_BUILDABLE, OperationLane.FACTION_BUILDABLE, OperationLane.ASSIGNABLE_WORKER_MACHINE, OperationLane.RECIPE_PROCESSOR_HANDOFF));

        promote(new Promotion(
                "infra.forge.machine-maintenance-rack",
                AssetIntegrationDisciplineAuthority.MACHINE_MAINTENANCE_RACK_FIXTURE,
                FixtureInteractionRegistry.Family.FORGE,
                DecompositionClass.FORGE_MACHINE,
                "Business Add-on Fixture",
                "Commercial Ledgers",
                "Common",
                "build_business_addon_fixture",
                "Existing add-on fixture recipe is used as the light machine-service rack profile rather than a new build lane.",
                "Industrial profile: machine upkeep, tool issue, component staging, and workshop support metadata.",
                OperationLane.PLAYER_BUILDABLE, OperationLane.FACTION_BUILDABLE, OperationLane.SERVICE_PREVIEW_ONLY));


        promote(new Promotion(
                "infra.social.faction-bar",
                AssetIntegrationDisciplineAuthority.FACTION_BAR_INTERIOR_FIXTURE,
                FixtureInteractionRegistry.Family.FACTION_BAR,
                DecompositionClass.BAR_SERVICE,
                "Business Add-on Fixture",
                "Commercial Ledgers",
                "Common",
                "feature_faction_rep_bar_counter",
                "Existing bar fixture is promoted as a faction representative and recovery-anchor service profile.",
                "Social profile: representative contact, rumors, fatigue relief, patron density, and reputation-service preview.",
                OperationLane.FACTION_BUILDABLE, OperationLane.SERVICE_PREVIEW_ONLY));

        promote(new Promotion(
                "infra.social.bar-counter",
                AssetIntegrationDisciplineAuthority.BAR_COUNTER_LONG_FIXTURE,
                FixtureInteractionRegistry.Family.FACTION_BAR,
                DecompositionClass.BAR_SERVICE,
                "Licensed Shop Counter",
                "Commercial Ledgers",
                "Common",
                "feature_bar_counter_long",
                "Existing shop-counter recipe is used as the trade/service counter profile for bars and taverns.",
                "Social profile: drinks, paid rumors, barter, public sales, and controlled service counter behavior.",
                OperationLane.PLAYER_BUILDABLE, OperationLane.FACTION_BUILDABLE, OperationLane.SERVICE_PREVIEW_ONLY));

        promote(new Promotion(
                "infra.social.bar-booth",
                AssetIntegrationDisciplineAuthority.BAR_BOOTH_FIXTURE,
                FixtureInteractionRegistry.Family.FACTION_BAR,
                DecompositionClass.BAR_SERVICE,
                "Business Add-on Fixture",
                "Commercial Ledgers",
                "Common",
                "feature_bar_booth",
                "Existing business add-on recipe is used as the seating/privacy service profile.",
                "Social profile: quiet meetings, rumor checks, patron tables, and recovery seating.",
                OperationLane.PLAYER_BUILDABLE, OperationLane.FACTION_BUILDABLE, OperationLane.SERVICE_PREVIEW_ONLY));

        promote(new Promotion(
                "infra.social.bar-stool",
                AssetIntegrationDisciplineAuthority.BAR_STOOL_FIXTURE,
                FixtureInteractionRegistry.Family.FACTION_BAR,
                DecompositionClass.BAR_SERVICE,
                "Business Add-on Fixture",
                "Commercial Ledgers",
                "Common",
                "feature_bar_stool",
                "Existing business add-on recipe is used as the compact patron-density seating profile.",
                "Social profile: listening posts, fatigue relief, crowd density, and low-risk social hooks.",
                OperationLane.PLAYER_BUILDABLE, OperationLane.FACTION_BUILDABLE, OperationLane.SERVICE_PREVIEW_ONLY));

        promote(new Promotion(
                "infra.social.bottle-shelf",
                AssetIntegrationDisciplineAuthority.BAR_BOTTLE_SHELF_FIXTURE,
                FixtureInteractionRegistry.Family.FACTION_BAR,
                DecompositionClass.BAR_SERVICE,
                "Business Add-on Fixture",
                "Commercial Ledgers",
                "Common",
                "feature_bar_bottle_shelf",
                "Existing business add-on recipe is used as the stock-display and quality-cue profile.",
                "Social profile: amasec quality cues, bar inventory, contraband suspicion, and service stock metadata.",
                OperationLane.PLAYER_BUILDABLE, OperationLane.FACTION_BUILDABLE, OperationLane.SERVICE_PREVIEW_ONLY));

        promote(new Promotion(
                "infra.social.service-keg",
                AssetIntegrationDisciplineAuthority.SERVICE_KEG_FIXTURE,
                FixtureInteractionRegistry.Family.FACTION_BAR,
                DecompositionClass.BAR_SERVICE,
                "Business Add-on Fixture",
                "Commercial Ledgers",
                "Common",
                "feature_service_keg",
                "Existing business add-on recipe is used as the drink-stock and service-keg profile.",
                "Social profile: drink supply, spoilage checks, keg stock, and bar-service inventory handoff.",
                OperationLane.PLAYER_BUILDABLE, OperationLane.FACTION_BUILDABLE, OperationLane.SERVICE_PREVIEW_ONLY));

        promote(new Promotion(
                "infra.social.market-counter",
                AssetIntegrationDisciplineAuthority.MARKET_COUNTER_FIXTURE,
                FixtureInteractionRegistry.Family.FACTION_BAR,
                DecompositionClass.CIVIC_COUNTER,
                "Licensed Shop Counter",
                "Commerce Permits",
                "Common",
                "build_licensed_shop_counter",
                "Existing shop-counter recipe is used as the compact market/trade counter profile.",
                "Market profile: barter, licensed sales, informal trade, merchant frontage, and commerce preview.",
                OperationLane.PLAYER_BUILDABLE, OperationLane.FACTION_BUILDABLE, OperationLane.SERVICE_PREVIEW_ONLY));


        promote(new Promotion(
                "infra.arbites.command-desk",
                AssetIntegrationDisciplineAuthority.ARBITES_COMMAND_DESK_FIXTURE,
                FixtureInteractionRegistry.Family.ARBITES_SECURITY,
                DecompositionClass.ARBITES_PRECINCT,
                "Precinct Defensive Fixture Set",
                "Security Cogitator Rites",
                "Common",
                "feature_arbites_command_desk",
                "Existing precinct fixture set is promoted as the command-desk and complaint-routing profile.",
                "Security profile: complaint intake, custody routing, bounty notice, patrol assignment, and faction-service preview.",
                OperationLane.FACTION_BUILDABLE, OperationLane.SERVICE_PREVIEW_ONLY));

        promote(new Promotion(
                "infra.arbites.sergeant-desk",
                AssetIntegrationDisciplineAuthority.ARBITES_SERGEANT_DESK_FIXTURE,
                FixtureInteractionRegistry.Family.ARBITES_SECURITY,
                DecompositionClass.ARBITES_PRECINCT,
                "Precinct Defensive Fixture Set",
                "Security Cogitator Rites",
                "Common",
                "feature_arbites_sergeant_desk",
                "Existing precinct fixture set is promoted as the duty-desk and patrol-control profile.",
                "Security profile: patrol orders, checkpoint ownership, duty rota, and precinct staffing preview.",
                OperationLane.FACTION_BUILDABLE, OperationLane.SERVICE_PREVIEW_ONLY));

        promote(new Promotion(
                "infra.arbites.coffee-maker",
                AssetIntegrationDisciplineAuthority.ARBITES_COFFEE_MAKER_FIXTURE,
                FixtureInteractionRegistry.Family.ARBITES_SECURITY,
                DecompositionClass.ARBITES_PRECINCT,
                "Precinct Defensive Fixture Set",
                "Security Cogitator Rites",
                "Common",
                "feature_arbites_coffee_maker",
                "Existing precinct fixture set is promoted as duty-room support clutter rather than a separate economy object.",
                "Service profile: duty-room fatigue relief, mess-room identity, shift continuity, and morale preview.",
                OperationLane.FACTION_BUILDABLE, OperationLane.SERVICE_PREVIEW_ONLY));

        promote(new Promotion(
                "infra.arbites.weapon-locker",
                AssetIntegrationDisciplineAuthority.ARBITES_WEAPON_LOCKER_FIXTURE,
                FixtureInteractionRegistry.Family.ARBITES_SECURITY,
                DecompositionClass.ARBITES_PRECINCT,
                "Precinct Defensive Fixture Set",
                "Security Cogitator Rites",
                "Common",
                "feature_arbites_weapon_locker",
                "Existing precinct fixture set is promoted as the controlled weapon-locker and armory-custody profile.",
                "Security profile: controlled gear issue, armory audit, precinct stores, and faction-security preview.",
                OperationLane.FACTION_BUILDABLE, OperationLane.SERVICE_PREVIEW_ONLY));

        promote(new Promotion(
                "infra.arbites.perp-bench",
                AssetIntegrationDisciplineAuthority.ARBITES_PERP_BENCH_FIXTURE,
                FixtureInteractionRegistry.Family.ARBITES_SECURITY,
                DecompositionClass.ARBITES_PRECINCT,
                "Precinct Defensive Fixture Set",
                "Security Cogitator Rites",
                "Common",
                "feature_arbites_perp_bench",
                "Existing precinct fixture set is promoted as the custody-intake and witness-waiting profile.",
                "Custody profile: detention intake, complaint overflow, witness waiting, and restraint preview.",
                OperationLane.FACTION_BUILDABLE, OperationLane.SERVICE_PREVIEW_ONLY));

        promote(new Promotion(
                "infra.arbites.interrogation-table",
                AssetIntegrationDisciplineAuthority.ARBITES_INTERROGATION_TABLE_FIXTURE,
                FixtureInteractionRegistry.Family.ARBITES_SECURITY,
                DecompositionClass.ARBITES_PRECINCT,
                "Precinct Defensive Fixture Set",
                "Security Cogitator Rites",
                "Common",
                "feature_arbites_interrogation_table",
                "Existing precinct fixture set is promoted as the questioning and evidence-review profile.",
                "Custody profile: questioning surface, evidence review, investigation context, and service preview.",
                OperationLane.FACTION_BUILDABLE, OperationLane.SERVICE_PREVIEW_ONLY));

        promote(new Promotion(
                "infra.arbites.holding-cell",
                AssetIntegrationDisciplineAuthority.ARBITES_HOLDING_CELL_FIXTURE,
                FixtureInteractionRegistry.Family.ARBITES_SECURITY,
                DecompositionClass.ARBITES_PRECINCT,
                "Precinct Defensive Fixture Set",
                "Security Cogitator Rites",
                "Common",
                "feature_arbites_holding_cell",
                "Existing precinct fixture set is promoted as the holding-cell and prisoner-overflow profile.",
                "Custody profile: detainment space, cell assignment, prisoner overflow, and noncombat security preview.",
                OperationLane.FACTION_BUILDABLE, OperationLane.SERVICE_PREVIEW_ONLY));

        promote(new Promotion(
                "infra.arbites.precinct-door",
                AssetIntegrationDisciplineAuthority.ARBITES_PRECINCT_DOOR_FIXTURE,
                FixtureInteractionRegistry.Family.ARBITES_SECURITY,
                DecompositionClass.ARBITES_PRECINCT,
                "Precinct Defensive Fixture Set",
                "Security Cogitator Rites",
                "Common",
                "feature_arbites_precinct_door",
                "Existing precinct fixture set is promoted as the access-control partition and precinct-door profile.",
                "Security profile: access control, checkpoint partitioning, door-hardening, and defense preview.",
                OperationLane.FACTION_BUILDABLE, OperationLane.SERVICE_PREVIEW_ONLY));

        promote(new Promotion(
                "infra.arbites.precinct-sign",
                AssetIntegrationDisciplineAuthority.ARBITES_PRECINCT_SIGN_FIXTURE,
                FixtureInteractionRegistry.Family.ARBITES_SECURITY,
                DecompositionClass.ARBITES_PRECINCT,
                "Precinct Defensive Fixture Set",
                "Security Cogitator Rites",
                "Common",
                "feature_arbites_precinct_sign",
                "Existing precinct fixture set is promoted as the public sign and jurisdiction-routing profile.",
                "Service profile: precinct identification, complaint routing, citation payment, and public-order preview.",
                OperationLane.FACTION_BUILDABLE, OperationLane.SERVICE_PREVIEW_ONLY));

        promote(new Promotion(
                "infra.arbites.alarm-panel",
                AssetIntegrationDisciplineAuthority.ARBITES_ALARM_PANEL_FIXTURE,
                FixtureInteractionRegistry.Family.ARBITES_SECURITY,
                DecompositionClass.ARBITES_PRECINCT,
                "Precinct Defensive Fixture Set",
                "Security Cogitator Rites",
                "Common",
                "feature_arbites_alarm_panel",
                "Existing precinct fixture set is promoted as the alarm-panel and sensor-reporting profile.",
                "Security profile: alarm routing, lockdown context, sensor reporting, and defense-system preview.",
                OperationLane.FACTION_BUILDABLE, OperationLane.SERVICE_PREVIEW_ONLY));

        promote(new Promotion(
                "infra.arbites.evidence-locker",
                AssetIntegrationDisciplineAuthority.ARBITES_EVIDENCE_LOCKER_FIXTURE,
                FixtureInteractionRegistry.Family.ARBITES_SECURITY,
                DecompositionClass.ARBITES_PRECINCT,
                "Precinct Defensive Fixture Set",
                "Security Cogitator Rites",
                "Common",
                "feature_arbites_evidence_locker",
                "Existing precinct fixture set is promoted as the evidence-locker and property-custody profile.",
                "Security profile: evidence custody, contraband storage, property audit, and investigation-service preview.",
                OperationLane.FACTION_BUILDABLE, OperationLane.SERVICE_PREVIEW_ONLY));

        promote(new Promotion(
                "infra.noble.wall-panel",
                AssetIntegrationDisciplineAuthority.NOBLE_WALL_PANEL_FIXTURE,
                FixtureInteractionRegistry.Family.NOBLE_SECURITY,
                DecompositionClass.NOBLE_SECURITY,
                "Reinforced Wall Panel",
                "Commercial Ledgers",
                "Serviceable",
                "build_noble_wall_panel",
                "Existing reinforced-wall build lane is promoted as the noble wall-panel and estate partition profile.",
                "Security profile: estate perimeter, panic-room partitioning, noble boundary identity, and defensive construction preview.",
                OperationLane.FACTION_BUILDABLE, OperationLane.SERVICE_PREVIEW_ONLY));

        promote(new Promotion(
                "infra.noble.gate",
                AssetIntegrationDisciplineAuthority.NOBLE_GATE_FIXTURE,
                FixtureInteractionRegistry.Family.NOBLE_SECURITY,
                DecompositionClass.NOBLE_SECURITY,
                "Reinforced Door",
                "Commercial Ledgers",
                "Serviceable",
                "build_noble_gate",
                "Existing reinforced-door build lane is promoted as the noble access-gate and service-checkpoint profile.",
                "Security profile: estate access control, guest filtering, servant routing, and reinforced-door preview.",
                OperationLane.FACTION_BUILDABLE, OperationLane.SERVICE_PREVIEW_ONLY));

        promote(new Promotion(
                "infra.noble.corner-tower",
                AssetIntegrationDisciplineAuthority.NOBLE_CORNER_TOWER_FIXTURE,
                FixtureInteractionRegistry.Family.NOBLE_SECURITY,
                DecompositionClass.NOBLE_SECURITY,
                "Watch Post",
                "Commercial Ledgers",
                "Serviceable",
                "build_noble_corner_tower",
                "Existing watch-post build lane is promoted as the noble corner-tower and overwatch profile.",
                "Security profile: guard overwatch, estate sightline, patrol anchoring, and house-security preview.",
                OperationLane.FACTION_BUILDABLE, OperationLane.SERVICE_PREVIEW_ONLY));

        promote(new Promotion(
                "infra.noble.gilded-sentry",
                AssetIntegrationDisciplineAuthority.NOBLE_GILDED_SENTRY_FIXTURE,
                FixtureInteractionRegistry.Family.NOBLE_SECURITY,
                DecompositionClass.NOBLE_SECURITY,
                "Gilded Sentry Turret",
                "Commercial Ledgers",
                "Fine",
                "build_noble_turret",
                "Existing noble turret build lane is promoted as the readable private sentry profile.",
                "Security profile: private turret placement, ownership binding, fire-arc metadata, and defense-profile preview.",
                OperationLane.FACTION_BUILDABLE, OperationLane.SERVICE_PREVIEW_ONLY));

        promote(new Promotion(
                "infra.noble.shield-relay",
                AssetIntegrationDisciplineAuthority.NOBLE_SHIELD_RELAY_FIXTURE,
                FixtureInteractionRegistry.Family.NOBLE_SECURITY,
                DecompositionClass.NOBLE_SECURITY,
                "Shield Relay",
                "Mechanicus Fabrication Rites",
                "Serviceable",
                "build_noble_shield_relay",
                "Existing shield-relay build lane is promoted as the noble protected-room relay profile.",
                "Security profile: ward coverage, power dependency, protected-room identity, and shield-system preview.",
                OperationLane.FACTION_BUILDABLE, OperationLane.SERVICE_PREVIEW_ONLY));

        promote(new Promotion(
                "infra.noble.void-shield-dome",
                AssetIntegrationDisciplineAuthority.NOBLE_VOID_SHIELD_DOME_FIXTURE,
                FixtureInteractionRegistry.Family.NOBLE_SECURITY,
                DecompositionClass.NOBLE_SECURITY,
                "Shield Relay",
                "Mechanicus Fabrication Rites",
                "Fine",
                "build_noble_void_shield_dome",
                "Existing shield-relay build lane is promoted as the high-tier estate dome emitter profile.",
                "Security profile: panic-room warding, field coverage metadata, utility dependency, and shield-system preview.",
                OperationLane.FACTION_BUILDABLE, OperationLane.SERVICE_PREVIEW_ONLY));

        promote(new Promotion(
                "infra.noble.laser-pylon",
                AssetIntegrationDisciplineAuthority.NOBLE_LASER_PYLON_FIXTURE,
                FixtureInteractionRegistry.Family.NOBLE_SECURITY,
                DecompositionClass.NOBLE_SECURITY,
                "Powered Defense Turret",
                "Security Cogitator Rites",
                "Fine",
                "build_noble_laser_pylon",
                "Existing powered-defense build lane is promoted as the noble beam-denial pylon profile.",
                "Security profile: beam lane, line-of-sight metadata, power draw, and private-security preview.",
                OperationLane.FACTION_BUILDABLE, OperationLane.SERVICE_PREVIEW_ONLY));

        promote(new Promotion(
                "infra.noble.energy-fence",
                AssetIntegrationDisciplineAuthority.NOBLE_ENERGY_FENCE_FIXTURE,
                FixtureInteractionRegistry.Family.NOBLE_SECURITY,
                DecompositionClass.NOBLE_SECURITY,
                "Security Sensor Mast",
                "Security Cogitator Rites",
                "Serviceable",
                "build_noble_energy_fence",
                "Existing sensor/security build lane is promoted as the noble perimeter-denial fence profile.",
                "Security profile: access-channel shaping, hazard metadata, sensor dependency, and estate-defense preview.",
                OperationLane.FACTION_BUILDABLE, OperationLane.SERVICE_PREVIEW_ONLY));

        promote(new Promotion(
                "infra.noble.security-panel",
                AssetIntegrationDisciplineAuthority.NOBLE_SECURITY_PANEL_FIXTURE,
                FixtureInteractionRegistry.Family.NOBLE_SECURITY,
                DecompositionClass.NOBLE_SECURITY,
                "Security Cogitator Node",
                "Security Cogitator Rites",
                "Serviceable",
                "feature_noble_security_panel",
                "Existing security-node build lane is promoted as the estate alarm and lock-control panel profile.",
                "Security profile: alarm routing, lock coordination, sensor status, service override, and estate-control preview.",
                OperationLane.FACTION_BUILDABLE, OperationLane.SERVICE_PREVIEW_ONLY));

        promote(new Promotion(
                "infra.guard-pdf.wall-panel",
                AssetIntegrationDisciplineAuthority.PDF_WALL_PANEL_FIXTURE,
                FixtureInteractionRegistry.Family.GUARD_PDF_SECURITY,
                DecompositionClass.GUARD_PDF_DEFENSE,
                "Reinforced Wall Panel",
                "Security Cogitator Rites",
                "Serviceable",
                "build_pdf_wall_panel",
                "Existing reinforced-wall build lane is promoted as the PDF wall-panel profile.",
                "Security profile: field perimeter, checkpoint hardening, passive cover, and defense-art reconciliation.",
                OperationLane.FACTION_BUILDABLE, OperationLane.SERVICE_PREVIEW_ONLY));

        promote(new Promotion(
                "infra.guard-pdf.wall-corner",
                AssetIntegrationDisciplineAuthority.PDF_WALL_CORNER_FIXTURE,
                FixtureInteractionRegistry.Family.GUARD_PDF_SECURITY,
                DecompositionClass.GUARD_PDF_DEFENSE,
                "Reinforced Wall Panel",
                "Security Cogitator Rites",
                "Serviceable",
                "build_pdf_wall_corner",
                "Existing reinforced-wall build lane is promoted as the PDF wall-corner profile.",
                "Security profile: corner hardpoint, roadblock shaping, perimeter turn, and passive cover metadata.",
                OperationLane.FACTION_BUILDABLE, OperationLane.SERVICE_PREVIEW_ONLY));

        promote(new Promotion(
                "infra.guard-pdf.gate",
                AssetIntegrationDisciplineAuthority.PDF_GATE_FIXTURE,
                FixtureInteractionRegistry.Family.GUARD_PDF_SECURITY,
                DecompositionClass.GUARD_PDF_DEFENSE,
                "Reinforced Door",
                "Security Cogitator Rites",
                "Serviceable",
                "build_pdf_gate",
                "Existing reinforced-door build lane is promoted as the PDF checkpoint-gate profile.",
                "Security profile: access control, vehicle lane filtering, checkpoint identity, and reinforced-door preview.",
                OperationLane.FACTION_BUILDABLE, OperationLane.SERVICE_PREVIEW_ONLY));

        promote(new Promotion(
                "infra.guard-pdf.damaged-wall",
                AssetIntegrationDisciplineAuthority.PDF_WALL_DAMAGED_FIXTURE,
                FixtureInteractionRegistry.Family.GUARD_PDF_SECURITY,
                DecompositionClass.GUARD_PDF_DEFENSE,
                "Reinforced Wall Panel",
                "Security Cogitator Rites",
                "Common",
                "build_pdf_wall_damaged",
                "Existing reinforced-wall build lane is promoted as the damaged PDF wall and repair-state profile.",
                "Security profile: damaged cover, repair/decomposition target, battlefield wear, and passive resistance metadata.",
                OperationLane.FACTION_BUILDABLE, OperationLane.SERVICE_PREVIEW_ONLY));

        promote(new Promotion(
                "infra.guard-pdf.turret-mk1",
                AssetIntegrationDisciplineAuthority.PDF_TURRET_MK1_FIXTURE,
                FixtureInteractionRegistry.Family.GUARD_PDF_SECURITY,
                DecompositionClass.GUARD_PDF_DEFENSE,
                "Heavy Stub Turret",
                "Security Cogitator Rites",
                "Serviceable",
                "build_pdf_turret_mk1",
                "Existing heavy-stub turret build lane is promoted as the PDF turret Mk I profile.",
                "Security profile: dormant turret, ammunition/staffing metadata, field fire-lane identity, and no autonomous targeting.",
                OperationLane.FACTION_BUILDABLE, OperationLane.SERVICE_PREVIEW_ONLY));

        promote(new Promotion(
                "infra.guard-pdf.turret-mk2",
                AssetIntegrationDisciplineAuthority.PDF_TURRET_MK2_FIXTURE,
                FixtureInteractionRegistry.Family.GUARD_PDF_SECURITY,
                DecompositionClass.GUARD_PDF_DEFENSE,
                "Heavy Stub Turret",
                "Security Cogitator Rites",
                "Fine",
                "build_pdf_turret_mk2",
                "Existing heavy-stub turret build lane is promoted as the PDF turret Mk II profile.",
                "Security profile: dormant turret, stronger field hardpoint, crew-served metadata, and no autonomous targeting.",
                OperationLane.FACTION_BUILDABLE, OperationLane.SERVICE_PREVIEW_ONLY));

        promote(new Promotion(
                "infra.guard-pdf.turret-mk3",
                AssetIntegrationDisciplineAuthority.PDF_TURRET_MK3_FIXTURE,
                FixtureInteractionRegistry.Family.GUARD_PDF_SECURITY,
                DecompositionClass.GUARD_PDF_DEFENSE,
                "Powered Defense Turret",
                "Security Cogitator Rites",
                "Fine",
                "build_pdf_turret_mk3",
                "Existing powered-defense turret lane is promoted as the PDF turret Mk III profile.",
                "Security profile: dormant heavy mount, power/ammo/staffing metadata, fortified fire-lane identity, and no autonomous targeting.",
                OperationLane.FACTION_BUILDABLE, OperationLane.SERVICE_PREVIEW_ONLY));

        promote(new Promotion(
                "infra.guard-pdf.sandbag-barricade",
                AssetIntegrationDisciplineAuthority.PDF_SANDBAG_BARRICADE_FIXTURE,
                FixtureInteractionRegistry.Family.GUARD_PDF_SECURITY,
                DecompositionClass.GUARD_PDF_DEFENSE,
                "Sandbag Line",
                "",
                "Common",
                "build_sandbag_barricade",
                "Existing sandbag-line build lane is promoted as the field sandbag barricade profile.",
                "Security profile: cheap ballistic cover, roadblock identity, battlefield clutter, and passive resistance metadata.",
                OperationLane.FACTION_BUILDABLE, OperationLane.SERVICE_PREVIEW_ONLY));

        promote(new Promotion(
                "infra.guard-pdf.sandbag-corner",
                AssetIntegrationDisciplineAuthority.PDF_SANDBAG_CORNER_FIXTURE,
                FixtureInteractionRegistry.Family.GUARD_PDF_SECURITY,
                DecompositionClass.GUARD_PDF_DEFENSE,
                "Sandbag Line",
                "",
                "Common",
                "build_sandbag_corner",
                "Existing sandbag-line build lane is promoted as the field sandbag corner profile.",
                "Security profile: cornered cover, roadblock shaping, firing-position identity, and passive resistance metadata.",
                OperationLane.FACTION_BUILDABLE, OperationLane.SERVICE_PREVIEW_ONLY));

        promote(new Promotion(
                "infra.guard-pdf.barracks-anchor",
                AssetIntegrationDisciplineAuthority.GUARD_BARRACKS_ANCHOR_FIXTURE,
                FixtureInteractionRegistry.Family.GUARD_PDF_SECURITY,
                DecompositionClass.GUARD_PDF_DEFENSE,
                "Guard Barracks",
                "Recruit Berthing Doctrine",
                "Common",
                "build_pdf_wall_panel",
                "Existing guard-barracks build lane is promoted as a military staffing and response anchor.",
                "Security profile: barracks defense, staffing anchor, response-point metadata, and passive raid-readiness support.",
                OperationLane.FACTION_BUILDABLE, OperationLane.SERVICE_PREVIEW_ONLY));

        promote(new Promotion(
                "infra.guard-pdf.watch-post",
                AssetIntegrationDisciplineAuthority.GUARD_WATCH_POST_FIXTURE,
                FixtureInteractionRegistry.Family.GUARD_PDF_SECURITY,
                DecompositionClass.GUARD_PDF_DEFENSE,
                "Watch Post",
                "",
                "Common",
                "feature_alarm_sensor_post",
                "Existing watch-post build lane is promoted as the Guard/PDF sentry profile.",
                "Security profile: warning post, sightline support, passive sensor metadata, and guard-response preview.",
                OperationLane.FACTION_BUILDABLE, OperationLane.SERVICE_PREVIEW_ONLY));

        promote(new Promotion(
                "infra.guard-pdf.supply-post",
                AssetIntegrationDisciplineAuthority.GUARD_SUPPLY_POST_FIXTURE,
                FixtureInteractionRegistry.Family.GUARD_PDF_SECURITY,
                DecompositionClass.GUARD_PDF_DEFENSE,
                "Supply Post",
                "Workshop Labor Discipline",
                "Serviceable",
                "build_sandbag_barricade",
                "Existing supply-post build lane is promoted as the Guard/PDF checkpoint-support profile.",
                "Security profile: ration/munition staging, checkpoint service, logistics-security identity, and passive support metadata.",
                OperationLane.FACTION_BUILDABLE, OperationLane.SERVICE_PREVIEW_ONLY));

        promote(new Promotion(
                "infra.road-transit.taxi-booth",
                AssetIntegrationDisciplineAuthority.TAXI_BOOTH,
                FixtureInteractionRegistry.Family.ROAD_TRANSIT,
                DecompositionClass.ROAD_TRANSIT,
                "Transit Booth",
                "",
                "Common",
                "feature_public_info_column",
                "Existing public information fixture lane is promoted as the taxi-call/transit booth profile.",
                "Transit profile: call surface, toll marker, route metadata, and passive service inspection without active boarding.",
                OperationLane.SERVICE_PREVIEW_ONLY));

        promote(new Promotion(
                "infra.road-transit.parking-marker",
                AssetIntegrationDisciplineAuthority.PARKING_LOT_MARKER,
                FixtureInteractionRegistry.Family.ROAD_TRANSIT,
                DecompositionClass.ROAD_TRANSIT,
                "Vehicle Set-down Marker",
                "",
                "Common",
                "tile_road_intersection",
                "Existing road/parking marker lane is promoted as the parking set-down profile.",
                "Transit profile: parking bay identity, access metadata, passive vehicle staging, and no live driving behavior.",
                OperationLane.SERVICE_PREVIEW_ONLY));

        promote(new Promotion(
                "infra.road-transit.parked-vehicle",
                AssetIntegrationDisciplineAuthority.PARKED_CIVILIAN_CAR,
                FixtureInteractionRegistry.Family.ROAD_TRANSIT,
                DecompositionClass.ROAD_TRANSIT,
                "Parked Vehicle Profile",
                "",
                "Common",
                "entity_car",
                "Existing vehicle art lane is promoted as a passive parked-vehicle profile.",
                "Transit profile: armor/seats metadata, readable street clutter, and no ownership, fuel, storage, or combat loop.",
                OperationLane.SERVICE_PREVIEW_ONLY));

        promote(new Promotion(
                "infra.food-bio.algae-tank",
                AssetIntegrationDisciplineAuthority.ALGAE_TANK_FIXTURE,
                FixtureInteractionRegistry.Family.FOOD_BIO,
                DecompositionClass.FOOD_BIO_PRODUCTION,
                "Algae Tank Fixture",
                "Common Agricultural Processing Patterns",
                "Common",
                "feature_algae_tank",
                "Semantic algae-tank art is promoted as a readable vat-culture fixture.",
                "Food/bio profile: algae culture, soylens feedstock, water/nutrient handling, and operation-profile handoff without a separate food economy.",
                OperationLane.FACTION_BUILDABLE, OperationLane.SERVICE_PREVIEW_ONLY, OperationLane.RECIPE_PROCESSOR_HANDOFF));

        promote(new Promotion(
                "infra.food-bio.hydroponics-bed",
                AssetIntegrationDisciplineAuthority.HYDROPONICS_BED_FIXTURE,
                FixtureInteractionRegistry.Family.FOOD_BIO,
                DecompositionClass.FOOD_BIO_PRODUCTION,
                "Hydroponics Bed Fixture",
                "Hydroponic Farm Patterns",
                "Common",
                "feature_hydroponics_bed",
                "Semantic hydroponics art is promoted as a crop-growth fixture.",
                "Food/bio profile: crop stock, leaf/grain trays, controlled grow beds, and agriculture-staffing handoff metadata.",
                OperationLane.FACTION_BUILDABLE, OperationLane.SERVICE_PREVIEW_ONLY, OperationLane.RECIPE_PROCESSOR_HANDOFF));

        promote(new Promotion(
                "infra.food-bio.animal-pen",
                AssetIntegrationDisciplineAuthority.ANIMAL_PEN_FIXTURE,
                FixtureInteractionRegistry.Family.FOOD_BIO,
                DecompositionClass.FOOD_BIO_PRODUCTION,
                "Animal Pen Fixture",
                "Civilian Provisioning Patterns",
                "Common",
                "feature_animal_pen",
                "Semantic animal-pen art is promoted as a livestock custody fixture.",
                "Food/bio profile: livestock holding, farm-beast care, meat/stock custody, and animal-handler metadata.",
                OperationLane.FACTION_BUILDABLE, OperationLane.SERVICE_PREVIEW_ONLY));

        promote(new Promotion(
                "infra.food-bio.cloning-vat",
                AssetIntegrationDisciplineAuthority.CLONING_VAT_FIXTURE,
                FixtureInteractionRegistry.Family.FOOD_BIO,
                DecompositionClass.FOOD_BIO_PRODUCTION,
                "Cloning Vat Fixture",
                "Common Agricultural Processing Patterns",
                "Serviceable",
                "feature_cloning_vat",
                "Semantic cloning-vat art is promoted as a sealed bio-growth surface.",
                "Food/bio profile: tissue culture, growth vat identity, medical-food crossover, and specialist-vat handoff metadata.",
                OperationLane.FACTION_BUILDABLE, OperationLane.SERVICE_PREVIEW_ONLY));

        promote(new Promotion(
                "infra.food-bio.fungal-grow-tray",
                AssetIntegrationDisciplineAuthority.FUNGAL_GROW_TRAY_FIXTURE,
                FixtureInteractionRegistry.Family.FOOD_BIO,
                DecompositionClass.FOOD_BIO_PRODUCTION,
                "Fungal Grow Tray Bank",
                "Common Agricultural Processing Patterns",
                "Common",
                "feature_hydroponics_bed",
                "Existing fungal grow tray build lane is promoted as the underhive food-growth fixture profile.",
                "Food/bio profile: fungus culture, sump food, spore stock, and underhive agriculture handoff metadata.",
                OperationLane.PLAYER_BUILDABLE, OperationLane.FACTION_BUILDABLE, OperationLane.SERVICE_PREVIEW_ONLY, OperationLane.RECIPE_PROCESSOR_HANDOFF));

        promote(new Promotion(
                "infra.food-bio.refrigerated-store",
                AssetIntegrationDisciplineAuthority.REFRIGERATED_FOOD_STORE_FIXTURE,
                FixtureInteractionRegistry.Family.FOOD_BIO,
                DecompositionClass.FOOD_BIO_PRODUCTION,
                "Refrigerated Food Store",
                "Civilian Provisioning Patterns",
                "Common",
                "feature_refrigerator",
                "Semantic refrigerator art is promoted as a cold-storage food fixture.",
                "Food/bio profile: cold storage, spoilage control, ration custody, and pantry-service handoff metadata.",
                OperationLane.FACTION_BUILDABLE, OperationLane.SERVICE_PREVIEW_ONLY));

        promote(new Promotion(
                "infra.food-bio.nutrient-vat",
                AssetIntegrationDisciplineAuthority.NUTRIENT_VAT_FIXTURE,
                FixtureInteractionRegistry.Family.FOOD_BIO,
                DecompositionClass.FOOD_BIO_PRODUCTION,
                "Nutrient Vat Fixture",
                "Civilian Provisioning Patterns",
                "Common",
                "feature_algae_tank",
                "Semantic vat art is promoted as a nutrient-slurry and ration-paste surface.",
                "Food/bio profile: vat nutrient slurry, ration paste input, synthetic food base, and provisioning-vat handoff metadata.",
                OperationLane.FACTION_BUILDABLE, OperationLane.SERVICE_PREVIEW_ONLY, OperationLane.RECIPE_PROCESSOR_HANDOFF));
        promoteDomestic("hab-cot", AssetIntegrationDisciplineAuthority.HAB_COT_FIXTURE, "Hab Cot Fixture", "feature_hab_cot_plain", "Plain cot art is promoted as a hab-cell sleep surface.", "Domestic profile: sleep surface, personal effects trunk, and hab-cell staging metadata.");
        promoteDomestic("guard-cot", AssetIntegrationDisciplineAuthority.GUARD_COT_FIXTURE, "Guard Cot Fixture", "feature_hab_cot_guard", "Guard cot art is promoted as a billet sleep surface.", "Domestic profile: barracks rest surface, uniform kit slot, and troop-room staging metadata.");
        promoteDomestic("worn-bed", AssetIntegrationDisciplineAuthority.WORN_BED_FIXTURE, "Worn Bed Fixture", "feature_hab_cot_worn", "Worn bed art is promoted as an underhive sleeping surface.", "Domestic profile: personal stash point, dormitory clutter, and low-status hab staging metadata.");
        promoteDomestic("noble-bed", AssetIntegrationDisciplineAuthority.NOBLE_BED_FIXTURE, "Noble Bed Fixture", "feature_hab_bed_noble", "Noble bed art is promoted as an estate bedroom surface.", "Domestic profile: noble rest surface, servant access, and estate-room metadata.");
        promoteDomestic("bunk-bed", AssetIntegrationDisciplineAuthority.BUNK_BED_FIXTURE, "Bunk Bed Fixture", "feature_hab_bunk_bed", "Bunk bed art is promoted as a dormitory capacity surface.", "Domestic profile: shared sleep capacity, locker staging, and compressed housing metadata.");
        promoteDomestic("water-storage", AssetIntegrationDisciplineAuthority.DOMESTIC_WATER_STORAGE_FIXTURE, "Domestic Water Storage Fixture", "feature_domestic_water_dispenser", "Domestic water-storage art is promoted as a household utility surface.", "Domestic profile: potable water custody, wash access, and utility-room metadata.");
        promoteDomestic("refrigerator", AssetIntegrationDisciplineAuthority.DOMESTIC_REFRIGERATOR_FIXTURE, "Domestic Refrigerator Fixture", "feature_domestic_refrigerator_white", "Domestic refrigerator art is promoted as a household cold-storage surface.", "Domestic profile: ration custody, cold-box state, and kitchenette metadata.");
        promoteDomestic("sink", AssetIntegrationDisciplineAuthority.DOMESTIC_SINK_FIXTURE, "Domestic Sink Fixture", "feature_domestic_sink_counter", "Sink counter art is promoted as a washing and prep surface.", "Domestic profile: water access, dish/wash surface, and kitchenette metadata.");
        promoteDomestic("stove", AssetIntegrationDisciplineAuthority.DOMESTIC_STOVE_FIXTURE, "Domestic Stove Fixture", "feature_domestic_stove_counter", "Stove counter art is promoted as a household cooking surface.", "Domestic profile: ration prep, heat surface, and apartment kitchen metadata.");
        promoteDomestic("prep-counter", AssetIntegrationDisciplineAuthority.DOMESTIC_PREP_COUNTER_FIXTURE, "Domestic Prep Counter Fixture", "feature_domestic_prep_counter", "Prep counter art is promoted as a small domestic worktop.", "Domestic profile: food prep, small manual work, and household staging metadata.");
        promoteDomestic("storage-cabinet", AssetIntegrationDisciplineAuthority.DOMESTIC_STORAGE_CABINET_FIXTURE, "Domestic Storage Cabinet Fixture", "feature_domestic_cabinet_counter", "Storage cabinet art is promoted as a personal/domestic container surface.", "Domestic profile: household storage, effects custody, and small-container metadata.");
        promoteDomestic("plank-table", AssetIntegrationDisciplineAuthority.DOMESTIC_PLANK_TABLE_FIXTURE, "Plank Table Fixture", "feature_domestic_plank_table", "Plank table art is promoted as a low-status meal/social surface.", "Domestic profile: common meal surface, social hook, and hab-room metadata.");
        promoteDomestic("round-table", AssetIntegrationDisciplineAuthority.DOMESTIC_ROUND_TABLE_FIXTURE, "Round Table Fixture", "feature_domestic_round_table", "Round table art is promoted as an apartment meal/social surface.", "Domestic profile: dining surface, social hook, and shared-room metadata.");
        promoteDomestic("mess-table", AssetIntegrationDisciplineAuthority.DOMESTIC_MESS_TABLE_FIXTURE, "Mess Table Fixture", "feature_domestic_square_mess_table", "Mess table art is promoted as a shared eating surface.", "Domestic profile: mess seating, ration handoff, and barracks/apartment metadata.");
        promoteDomestic("ornate-table", AssetIntegrationDisciplineAuthority.DOMESTIC_ORNATE_TABLE_FIXTURE, "Ornate Dining Table Fixture", "feature_domestic_ornate_dining_table", "Ornate dining table art is promoted as a noble social surface.", "Domestic profile: servant route staging, noble dining, and estate-service metadata.");
    }

    private static void promoteDomestic(String idSuffix, String fixtureType, String recipeName, String artKey, String status, String scope) {
        promote(new Promotion(
                "infra.domestic-hab." + idSuffix,
                fixtureType,
                FixtureInteractionRegistry.Family.DOMESTIC_HAB,
                DecompositionClass.DOMESTIC_HAB_FIXTURE,
                recipeName,
                "Hab Domestic Infrastructure Patterns",
                "Common",
                artKey,
                status,
                scope,
                OperationLane.FACTION_BUILDABLE, OperationLane.SERVICE_PREVIEW_ONLY));
    }

    private static void promote(Promotion p) {
        PROMOTIONS.add(p);
        BY_RECIPE.putIfAbsent(p.buildRecipeName, p);
        // One generic fixture can map to several promoted targets; preserve the first as default feedback.
        BY_FIXTURE.putIfAbsent(p.fixtureType, p);
    }

    static List<Promotion> promotions() { return Collections.unmodifiableList(PROMOTIONS); }
    static Promotion defaultForFixture(String fixtureType) { return BY_FIXTURE.get(AssetIntegrationDisciplineAuthority.canonicalType(fixtureType)); }
    static Promotion byRecipeName(String recipeName) { return BY_RECIPE.get(recipeName); }

    static List<BuildRecipe> promotedBuildRecipes() {
        ArrayList<BuildRecipe> recipes = new ArrayList<>();
        recipes.add(BuildRecipe.clinicStall());
        recipes.add(BuildRecipe.sterileBench());
        recipes.add(BuildRecipe.microLab());
        recipes.add(BuildRecipe.crudeChemBench());
        recipes.add(BuildRecipe.reagentBench());
        recipes.add(BuildRecipe.distillationColumn());
        recipes.add(BuildRecipe.fumeHood());
        recipes.add(BuildRecipe.injectorStation());
        recipes.add(BuildRecipe.fungalGrowTray());
        recipes.add(BuildRecipe.workbench());
        recipes.add(BuildRecipe.atmosCondenser());
        recipes.add(BuildRecipe.microForge());
        recipes.add(BuildRecipe.businessAddon());
        recipes.add(BuildRecipe.shopCounter());
        recipes.add(BuildRecipe.precinctDefensiveFixtureSet());
        recipes.add(BuildRecipe.reinforcedWallPanel());
        recipes.add(BuildRecipe.reinforcedDoor());
        recipes.add(BuildRecipe.watchPost());
        recipes.add(BuildRecipe.gildedSentryTurret());
        recipes.add(BuildRecipe.shieldRelay());
        recipes.add(BuildRecipe.powerTurret());
        recipes.add(BuildRecipe.securitySensorMast());
        recipes.add(BuildRecipe.securityNode());
        recipes.add(BuildRecipe.sandbagLine());
        recipes.add(BuildRecipe.heavyStubTurret());
        recipes.add(BuildRecipe.supplyPost());
        return recipes;
    }

    static String feedbackForFixture(String fixtureType) {
        Promotion p = defaultForFixture(fixtureType);
        return p == null ? "" : p.playerFeedback();
    }

    static String auditSummary() {
        EnumMap<FixtureInteractionRegistry.Family, Integer> byFamily = new EnumMap<>(FixtureInteractionRegistry.Family.class);
        EnumMap<DecompositionClass, Integer> byDecomp = new EnumMap<>(DecompositionClass.class);
        for (Promotion p : PROMOTIONS) {
            byFamily.put(p.family, byFamily.getOrDefault(p.family, 0) + 1);
            byDecomp.put(p.decomposition, byDecomp.getOrDefault(p.decomposition, 0) + 1);
        }
        return "infrastructurePromotion version=" + VERSION + " promotions=" + PROMOTIONS.size() +
                " families=" + byFamily + " decomposition=" + byDecomp +
                " policy=shared fixture registry/build recipe ownership; low_32 core only";
    }}
