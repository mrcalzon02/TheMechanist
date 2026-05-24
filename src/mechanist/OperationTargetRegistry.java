package mechanist;

import java.util.*;

/**
 * Shared operation target registry.
 *
 * Defines operation targets that can be handed to the shared machine-operation
 * queue without creating a second production system or mutating saves directly.
 */
final class OperationTargetRegistry {
    static final String VERSION = "0.9.10ai";

    enum OperationLane {
        MEDICAE_SERVICE,
        LAB_ANALYSIS,
        CHEMICAL_PROCESS,
        FORGE_PROCESS,
        MAINTENANCE_SERVICE,
        FOOD_BIO_PROCESS,
        FACTION_CONSTRUCTION
    }

    enum WorkerNeed {
        NONE,
        PATIENT_OR_OPERATOR,
        TRAINED_WORKER,
        SPECIALIST,
        SERVITOR_OR_TECHNICIAN
    }

    static final class OperationQueueTarget {
        final String id;
        final String promotionId;
        final OperationLane lane;
        final WorkerNeed workerNeed;
        final String inputFamily;
        final String outputFamily;
        final int nominalTurns;
        final int powerCost;
        final int fuelCost;
        final int noiseCost;
        final int hazardRisk;
        final boolean playerFactionParity;
        final String notes;

        OperationQueueTarget(String id, String promotionId, OperationLane lane, WorkerNeed workerNeed,
                             String inputFamily, String outputFamily, int nominalTurns, int powerCost,
                             int fuelCost, int noiseCost, int hazardRisk, boolean playerFactionParity,
                             String notes) {
            this.id = id;
            this.promotionId = promotionId;
            this.lane = lane;
            this.workerNeed = workerNeed;
            this.inputFamily = inputFamily;
            this.outputFamily = outputFamily;
            this.nominalTurns = nominalTurns;
            this.powerCost = powerCost;
            this.fuelCost = fuelCost;
            this.noiseCost = noiseCost;
            this.hazardRisk = hazardRisk;
            this.playerFactionParity = playerFactionParity;
            this.notes = notes;
        }
    }

    static final List<OperationQueueTarget> TARGETS = List.of(
        new OperationQueueTarget("medicae_triage_service", "backroom_medicae_stall", OperationLane.MEDICAE_SERVICE,
            WorkerNeed.SPECIALIST, "injured_actor", "stabilized_actor_or_service_receipt", 12, 1, 0, 1, 1, true,
            "Medicae service target for stabilization, triage feedback, and treatment receipts."),
        new OperationQueueTarget("sterile_clean_bench_preparation", "sterile_medicae_clean_bench", OperationLane.MEDICAE_SERVICE,
            WorkerNeed.TRAINED_WORKER, "basic_medical_supplies", "cleaned_medical_supplies", 10, 1, 0, 1, 0, true,
            "Supports infection and treatment chains through a bounded preparation lane."),
        new OperationQueueTarget("micro_lab_assay", "emm_micro_lab", OperationLane.LAB_ANALYSIS,
            WorkerNeed.SPECIALIST, "unknown_sample_or_material", "analysis_note_or_knowledge_progress", 16, 2, 0, 1, 1, true,
            "Lab operation target for knowledge progress and material analysis."),
        new OperationQueueTarget("crude_chem_batch", "crude_chem_bench", OperationLane.CHEMICAL_PROCESS,
            WorkerNeed.TRAINED_WORKER, "simple_chemical_inputs", "crude_chemical_output", 18, 1, 1, 2, 2, true,
            "Low-end chemical process with explicit hazard/noise costs."),
        new OperationQueueTarget("reagent_preparation_batch", "reagent_preparation_bench", OperationLane.CHEMICAL_PROCESS,
            WorkerNeed.TRAINED_WORKER, "stable_reagent_inputs", "prepared_reagent_output", 16, 1, 0, 1, 1, true,
            "Stable reagent preparation target for ordinary chemical and medical precursor chains."),
        new OperationQueueTarget("distillation_batch", "distillation_column", OperationLane.CHEMICAL_PROCESS,
            WorkerNeed.TRAINED_WORKER, "liquid_or_slurry_input", "distilled_fraction_output", 24, 2, 2, 2, 2, true,
            "Connects to water, alcohol, solvents, fuel, and medical reagent chains."),
        new OperationQueueTarget("fume_hood_volatile_process", "fume_hood", OperationLane.CHEMICAL_PROCESS,
            WorkerNeed.SPECIALIST, "volatile_or_toxic_inputs", "contained_volatile_output", 22, 2, 1, 2, 1, true,
            "Toxic and aerosol process target with explicit safety and hazard-mitigation metadata."),
        new OperationQueueTarget("injector_filling_batch", "injector_filling_station", OperationLane.CHEMICAL_PROCESS,
            WorkerNeed.TRAINED_WORKER, "sterile_ampoule_inputs", "filled_injector_output", 18, 1, 0, 1, 1, true,
            "Injector filling and ampoule packaging target for medical utility chains."),
        new OperationQueueTarget("scrap_workbench_salvage_sort", "scrap_workbench", OperationLane.MAINTENANCE_SERVICE,
            WorkerNeed.PATIENT_OR_OPERATOR, "mixed_scrap_or_broken_item", "sorted_parts_or_repair_action", 12, 0, 0, 1, 0, true,
            "Workbench lane for salvage sorting, repair staging, and manual fabrication parity."),
        new OperationQueueTarget("micro_forge_basic_part", "emm_micro_forge", OperationLane.FORGE_PROCESS,
            WorkerNeed.SERVITOR_OR_TECHNICIAN, "metal_scrap_or_stock", "basic_machined_part", 20, 3, 1, 3, 1, true,
            "Forge process lane for industrial decomposition and machine construction parity."),
        new OperationQueueTarget("atmospheric_condenser_capture", "emm_atmospheric_condenser", OperationLane.MAINTENANCE_SERVICE,
            WorkerNeed.TRAINED_WORKER, "bad_air_filter_medium", "potable_water_or_condensate", 16, 2, 0, 1, 0, true,
            "Condenser lane for water capture, utility-room support, and reclamation-adjacent processing."),
        new OperationQueueTarget("machine_maintenance_service", "machine_maintenance_rack", OperationLane.MAINTENANCE_SERVICE,
            WorkerNeed.TRAINED_WORKER, "worn_machine_or_service_parts", "maintenance_status_or_repair_credit", 14, 0, 0, 1, 0, true,
            "Maintenance rack lane for tool issue, upkeep staging, and component repair metadata."),
        new OperationQueueTarget("algae_tank_culture", "algae_tank", OperationLane.FOOD_BIO_PROCESS,
            WorkerNeed.TRAINED_WORKER, "starter_culture_water_and_nutrients", "algae_culture_or_soylens_feedstock", 18, 1, 0, 1, 0, true,
            "Algae tank operation target for synthetic-food feedstock and water/nutrient handling metadata."),
        new OperationQueueTarget("hydroponics_bed_crop", "hydroponics_bed", OperationLane.FOOD_BIO_PROCESS,
            WorkerNeed.TRAINED_WORKER, "seed_culture_water_and_grow_medium", "hydroponic_crop_stock", 24, 2, 0, 1, 0, true,
            "Hydroponics operation target for crop stock, leaf/grain trays, and agriculture staffing metadata."),
        new OperationQueueTarget("fungal_grow_tray_culture", "fungal_grow_tray", OperationLane.FOOD_BIO_PROCESS,
            WorkerNeed.PATIENT_OR_OPERATOR, "fungus_starter_compost_and_filtered_water", "fungus_culture_tray_or_spore_stock", 20, 0, 0, 1, 1, true,
            "Fungal grow tray operation target for underhive agriculture and spore-stock metadata."),
        new OperationQueueTarget("nutrient_vat_slurry", "nutrient_vat", OperationLane.FOOD_BIO_PROCESS,
            WorkerNeed.TRAINED_WORKER, "waste_biomass_water_and_fertilizer", "vat_nutrient_slurry_or_ration_paste_input", 22, 2, 0, 1, 1, true,
            "Nutrient vat operation target for slurry, ration-paste input, and provisioning-vat metadata."),
        new OperationQueueTarget("refrigerated_food_store_service", "refrigerated_food_store", OperationLane.FOOD_BIO_PROCESS,
            WorkerNeed.NONE, "ration_stock_or_perishable_food", "preserved_food_stock_status", 8, 1, 0, 0, 0, true,
            "Cold-storage service target for ration custody and spoilage-control metadata.")
    );

    private OperationTargetRegistry() {}

    static String summary() {
        return "operationTargetRegistry version=" + VERSION + " operationTargets=" + TARGETS.size()
            + " lanes=" + Arrays.toString(OperationLane.values());
    }
}
