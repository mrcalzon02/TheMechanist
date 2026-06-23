package mechanist;

import mechanist.assets.AssetMetadata;
import mechanist.assets.AssetRegistry;
import mechanist.assets.AssetType;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Game-owned Infopedia bridge for the Stage 2 Semantic Asset Browser.
 *
 * This class deliberately exposes registry contents to the existing in-game
 * Infopedia surface rather than creating a detached Swing dialog. The visual
 * shell remains TheMechanist's normal menu renderer; this authority supplies
 * navigable labels, type filtering, searchable details, and stable parsing of
 * the 8-character semantic asset ID from a displayed row.
 */
public final class SemanticAssetInfopediaAuthority {
    public static final String VERSION = "0.9.10ju-stage2-semantic-asset-infopedia";

    private static final Pattern ID_PREFIX = Pattern.compile("^[A-Z0-9]{3,4}-[A-Z0-9]{3,4}\\b");
    private static final String MECHANIC_PREFIX = "MECHANIC - ";

    private static final AssetType[] BROWSE_TYPES = {
            AssetType.PORTRAIT,
            AssetType.WALL_TILE,
            AssetType.FLOOR_TILE,
            AssetType.ROAD_TILE,
            AssetType.SIDEWALK_TILE,
            AssetType.CORRIDOR_TILE,
            AssetType.OBJECT,
            AssetType.FIXTURE,
            AssetType.MACHINE,
            AssetType.ITEM_ICON,
            AssetType.WEAPON_ICON,
            AssetType.ARMOR_ICON,
            AssetType.UI_ICON,
            AssetType.CORPSE_DECAY
    };

    private SemanticAssetInfopediaAuthority() {
    }

    public static AssetType[] browseTypes() {
        return BROWSE_TYPES.clone();
    }

    public static String typeLabel(AssetType type) {
        return type == null ? "All semantic assets" : type.displayName();
    }

    public static List<String> entries(AssetRegistry registry, AssetType selectedType, String filter) {
        List<String> out = new ArrayList<>();
        AssetRegistry safe = registry == null ? AssetRegistry.empty() : registry;

        List<AssetMetadata> source = selectedType == null ? safe.all() : safe.byType(selectedType);
        String q = filter == null ? "" : filter.trim();
        int added = 0;
        if (selectedType == null) {
            for (MechanicEntry entry : mechanicEntries()) {
                if (!entry.matchesFilter(q)) continue;
                out.add(formatMechanicEntry(entry));
                added++;
            }
        }
        for (AssetMetadata metadata : source) {
            if (!metadata.matchesFilter(q)) continue;
            out.add(formatEntry(metadata));
            added++;
        }
        if (added == 0) {
            out.add("No assets match the active type/filter.");
        }
        return out;
    }

    public static String formatEntry(AssetMetadata metadata) {
        return metadata.id() + " — " + metadata.name() + " [" + metadata.type().displayName() + "]";
    }

    public static Optional<String> assetIdFromEntry(String entry) {
        if (entry == null) return Optional.empty();
        String trimmed = entry.trim().toUpperCase(Locale.ROOT);
        if (trimmed.length() < 8) return Optional.empty();
        String candidate = trimmed.substring(0, 8);
        if (!ID_PREFIX.matcher(candidate).find()) return Optional.empty();
        return Optional.of(candidate);
    }

    public static Optional<AssetMetadata> metadataForEntry(AssetRegistry registry, String entry) {
        AssetRegistry safe = registry == null ? AssetRegistry.empty() : registry;
        return assetIdFromEntry(entry).flatMap(safe::find);
    }

    public static List<String> detailLines(AssetRegistry registry, String entry, AssetType selectedType, String filter) {
        List<String> lines = new ArrayList<>();
        AssetRegistry safe = registry == null ? AssetRegistry.empty() : registry;
        Optional<MechanicEntry> mechanic = mechanicForEntry(entry);
        if (mechanic.isPresent()) {
            return mechanicDetailLines(mechanic.get());
        }
        Optional<AssetMetadata> metadata = metadataForEntry(safe, entry);
        if (metadata.isEmpty()) {
            lines.add(entry == null || entry.isBlank() ? "Semantic Asset Index" : entry);
            lines.add("Reference ID: semantic-registry");
            lines.add("Authority: " + VERSION);
            lines.add("");
            lines.add("Purpose: The Semantic Asset Index is the durable bridge between graphical files and player-facing meaning. It lets the InfoPedia show what each sprite is supposed to represent before the live renderer is migrated away from scattered file paths.");
            lines.add("Active type filter: " + typeLabel(selectedType));
            lines.add("Active search filter: " + (filter == null || filter.isBlank() ? "<none>" : filter.trim()));
            AssetRegistry.RegistryAudit audit = safe.audit(false);
            lines.add("Registered assets: " + audit.entryCount());
            lines.add("Registered categories with entries: " + audit.categoryCount());
            lines.add("Unknown type rows: " + audit.unknownTypeCount());
            lines.add("Blank descriptions: " + audit.blankDescriptionCount());
            lines.add("");
            lines.add("How to use: click an asset row to inspect its exact 8-character ID, image preview, purpose/type, path, and semantic description. Press / or the FILTER button to focus search, type to search ID/name/path/description, Backspace to delete, Enter to return to list navigation, and CLEAR to reset.");
            lines.add("");
            lines.add("Stage status: Stage 2 completes the internal browser/audit surface. It does not yet replace item icons, tile rendering, machine art, portraits, or world rendering paths. That remains Stage 3+ work.");
            return lines;
        }

        AssetMetadata m = metadata.get();
        lines.add(m.name());
        lines.add("Reference ID: " + m.id());
        lines.add("Type / purpose: " + m.type().displayName());
        lines.add("Registry path/URI: " + m.pathOrUri());
        lines.add("");
        lines.add("Semantic description:");
        lines.add(m.semanticDescription());
        lines.add("");
        if (m.type() == AssetType.PORTRAIT) {
            List<String> partitionLines = PortraitSemanticAssetAuthority.infopediaLines(safe.projectRoot(), m.id());
            if (!partitionLines.isEmpty()) {
                lines.add("Stage 8 portrait/entity partition:");
                lines.addAll(partitionLines);
                lines.add("");
            }
        }
        lines.add("Migration note: callers should eventually reference this art with AssetManager.getAsset(\"" + m.id() + "\") instead of direct file-path loading. This row is now inspectable from the game-owned InfoPedia before renderer migration begins.");
        return lines;
    }

    static List<String> mechanicEntryRows(String filter) {
        ArrayList<String> rows = new ArrayList<>();
        String q = filter == null ? "" : filter.trim();
        for (MechanicEntry entry : mechanicEntries()) {
            if (entry.matchesFilter(q)) rows.add(formatMechanicEntry(entry));
        }
        return rows;
    }

    static List<String> mechanicDetailLinesByKey(String key) {
        return mechanicEntries().stream()
                .filter(e -> e.key().equalsIgnoreCase(key == null ? "" : key.trim()))
                .findFirst()
                .map(SemanticAssetInfopediaAuthority::mechanicDetailLines)
                .orElseGet(() -> List.of("Mechanic reference", "No matching mechanic entry is registered yet."));
    }

    static Optional<String> mechanicEntryRowByKey(String key) {
        return mechanicByKey(key).map(SemanticAssetInfopediaAuthority::formatMechanicEntry);
    }

    private static List<MechanicEntry> mechanicEntries() {
        return List.of(
                new MechanicEntry("look-examine", "Look and Examine", "Inspection",
                        "Look gives immediate surface facts. Examine spends attention on the selected visible target and deepens the read without exposing hidden records.",
                        List.of(
                                ControlReferenceTextSubsystem.contextPromptLine("Look", 0, InputAction.EXAMINE, InputAction.CANCEL, "Examine the selected visible target or back out."),
                                "Readable boundary: surface observations first; repeated examination can add intent, state, equipment, condition, and context.",
                                "Guard: Milestone02LookExamineReadabilitySmoke checks depth wording, route/examine sanitization, and prompt coverage."
                        ),
                        List.of("movement-planning", "context-prompts")),
                new MechanicEntry("movement-planning", "Movement Planning", "Movement",
                        "Movement planning lets the player place a ghost target, preview or understand the route, and receive clear refusal text before committing.",
                        movementPlanningInfopediaLines(),
                        List.of("look-examine", "context-prompts", "menu-uniformity")),
                new MechanicEntry("context-prompts", "Context Prompts", "Controls",
                        "Context prompts show the current action names and active keyboard/controller bindings for the panel the player is using.",
                        contextPromptInfopediaLines(),
                        List.of("look-examine", "movement-planning", "menu-uniformity")),
                new MechanicEntry("menu-uniformity", "Menu Uniformity", "Menus",
                        "Menu uniformity keeps ordinary game menus aligned around purpose, back behavior, panes, prompts, disabled-state explanations, and transfer rules.",
                        menuUniformityInfopediaLines(),
                        List.of("context-prompts", "movement-planning", "input-rebinding-audit")),
                new MechanicEntry("input-rebinding-audit", "Input Rebinding Audit", "Controls",
                        "Input rebinding audit rows expose action labels, contexts, default bindings, controller prompts, conflict notes, required recovery actions, and current-profile readiness.",
                        InputRebindingAuditAuthority.infopediaLines(),
                        List.of("context-prompts", "movement-planning", "menu-uniformity")),
                new MechanicEntry("body-condition", "Body Condition", "Health",
                        "Body condition translates wounds, bleeding, infection risk, pain, fatigue, hunger, thirst, clothing, and held equipment into readable readiness and danger.",
                        List.of(
                                "The Character panel distinguishes immediate danger, impaired readiness, and stable condition without exposing internal health records.",
                                "Wounds and bleeding demand attention first; infection risk, pain, fatigue, food, and water can then explain reduced readiness.",
                                "Guard: Milestone02BodyConditionReadabilitySmoke checks readable condition bands, danger priorities, and leak-free wording."
                        ),
                        List.of("medical-treatment", "menu-uniformity")),
                new MechanicEntry("medical-treatment", "Medical Treatment Readiness", "Health",
                        "Treatment readiness explains which carried supplies can address current injuries and which conditions still lack a supported treatment action.",
                        List.of(
                                "Treatment previews are informational until the panel offers an explicit treatment command; they do not silently consume items.",
                                "The readout distinguishes bleeding control, wound care, infection control, and pain relief from unsupported or missing treatment paths.",
                                "Guard: Milestone02MedicalTreatmentReadabilitySmoke checks treatment priorities, supply recognition, and honest unavailable-state wording."
                        ),
                        List.of("body-condition", "transfer-workflows")),
                new MechanicEntry("inventory-equipment", "Inventory and Equipment", "Inventory",
                        "Inventory rows represent individual units while item-family matching connects quality or manufacturing variants to equipment and catalog rules.",
                        List.of(
                                "The detail pane separates exact selected-quality units from related units in the same item family and reports how many quality grades are present.",
                                "Use, Equip, Store, and Take affect one selected unit at a time; repeated commands are required for additional units.",
                                "Quality changes item identity and value, while condition remains unavailable unless a separate durability record is explicitly attached.",
                                "Quality bands run Junk, Shoddy, Common, Serviceable, Fine, Masterwork, Noble, and Archeotech; Shoddy is a degradation quality rather than a target doctrine school.",
                                "Mission, evidence, and intelligence goods receive protection warnings and cannot be sold through ordinary vendor trade.",
                                "Guard: Milestone02InventoryReadabilitySmoke checks quality, legality, equipment, transfer consequences, mixed-quality counts, and one-unit action scope.",
                                "Guard: Milestone03QualityBandDefinitionAuditSmoke checks shared quality order, doctrine bands, Shoddy boundary, item value/charge multipliers, profile roles, capping rule, and raw-ID hiding."
                        ),
                        List.of("transfer-workflows", "body-condition", "medical-treatment", "contract-evidence")),
                new MechanicEntry("production-forecast", "Production Forecast", "Production",
                        "Production forecasts explain the selected recipe's output, quality, machine condition, work time, fatigue, knowledge, faction pattern, inputs, and exact blocker before crafting.",
                        List.of(
                                "The Craft command performs the immediate player-operated recipe; it does not create a queued machine job.",
                                "Forecasts identify required supplies, parts, named items, machine state, staffing, queue metadata, utility boundaries, knowledge, and expected carried-inventory output.",
                                "Output quality is capped by known doctrine, recipe pattern, machine quality, the lowest quality among named input units that will actually be consumed, and the claimed production room's facility quality.",
                                "Facility quality counts serviceable production stations in the selected machine's claimed room: one supports Common, two or three Serviceable, four or five Fine, and six or more Masterwork. Broken stations do not contribute.",
                                "An equipped fabrication or repair tool contributes its quality as a cap. If neither hand holds a qualifying tool, integrated machine tooling leaves the hand-tool hook open; unrelated carried items never silently cap production.",
                                "Named material selection follows immediate Craft execution: matching carried units first, then matching base-storage units. Abstract supplies and machine parts do not invent a material-quality cap.",
                                "Machine integrity zero blocks production; critical and worn machines add visible defect risk. Owned machines can spend one machine part and one turn on bounded field repair toward serviceable integrity.",
                                "Manual operator skill maps the recipe's named skill onto an existing core stat. Novice, practiced, skilled, and expert operators support Common, Serviceable, Fine, and Masterwork output respectively while also adjusting defect risk.",
                                "Assigned recruit skill has a readable potential worker-quality tier, but manual Craft remains player-operated; worker quality becomes active only when a staffed queued-production owner is implemented.",
                                "A machine may store one installed recipe doctrine. Teach Machine requires the player to know the selected doctrine; afterward the machine can supply that recipe knowledge independently and persists it with the base object.",
                                "A claimed production facility may share doctrine from another serviceable station in the same room. Broken stations, stations outside the room, and unclaimed work areas cannot provide facility knowledge.",
                                "The production knowledge-source audit names player knowledge, selected-machine doctrine, claimed-facility doctrine, effective knowledge union, and the boundary that source audits do not grant knowledge or start production.",
                                "Crafted-item provenance preserves output quality, knowledge source, machine quality, facility quality, equipped tool quality, and the limiting cap through save/load and transfers.",
                                "Outcome forecasts compare estimated value, usable charges, and quality-sensitive defect risk. Manual Craft assigns one batch ID and inspection disposition to all units from the action; flagged batches receive a visible 40% ordinary-trader resale penalty while item statistics remain unchanged.",
                                "Guard: Milestone02ProductionReadabilitySmoke checks output, timing, requirements, destination, and blocker wording.",
                                "Faction production mutation is visible before crafting: the faction profile names the output prefix and its effective value, charge, and defect-pressure consequences, then item provenance preserves that mutation after production and transfer.",
                                "The faction production mutation audit names the profile owner, value/charge/defect-pressure formula inputs, provenance owner, and the boundary that law enforcement, reputation, seizure, corruption, and faction hostility remain future owners.",
                                "Manual Craft uses the established fatigue readiness bands. Slightly tired and tired operators add visible defect pressure; the exhausted band blocks machinery operation until the player rests. Produced-item provenance records the pressure present during the run.",
                                "Forecast and item inspection preserve the producing room and claimed facility separately. Unclaimed world workspaces remain explicitly unclaimed rather than inheriting the player's base identity.",
                                "Forecast and item inspection preserve the exact producing station by name, role, and coordinates so same-quality machines remain distinguishable after transfer.",
                                "Item provenance records the immediate manual operator separately from skill. Assigned workers and supervisors remain uncredited until a queued staffed-production owner executes their work.",
                                "After a successful manual Craft finishes, one completion record enters shared machine-operation history. The history is audit-only here; existing Craft code remains responsible for inputs, outputs, wear, fatigue, and provenance.",
                                "A staffed generated-production assignment can execute one queued run when worker, knowledge, machine, room ownership, and inputs are ready. The run consumes concrete inputs, stores output in base storage, records provenance, decrements the queue, and writes shared operation history.",
                                "Item provenance records whether the run was immediate manual Craft or staffed queued production, so inspection can distinguish manual or staffed workforce ownership after transfer.",
                                "Generated staffed output preserves the variant law status used by production access rules: lawful, restricted, black-market, contraband, profaned, or hostile-social context. This is provenance only until a later law-enforcement owner consumes it.",
                                "Generated staffed output also preserves the generated recipe source and note that produced the variant. This is recipe/source provenance, not blueprint ownership.",
                                "Batch issue tags preserve Phase 9.3 batch signals such as good, defective, contaminated, unstable, restricted, stolen-risk, counterfeit, or faction-certified when those signals are present in inspection, recipe, law, or source metadata. Only the existing defect appraisal changes ordinary resale value.",
                                "The batch issue definition audit names defect, contamination, instability, counterfeit, stolen-risk, restricted, faction-certified, and reserved recall flags while keeping recall enforcement future-owned.",
                                "Repair/modification history records the current field-repair note from a repaired production machine onto later output. Item modification history remains blank until a real item-modification owner exists.",
                                "The quality band definition audit names shared quality order, doctrine bands, item value and charge multipliers, production profile roles, and the central capping rule.",
                                "The production quality definition audit names cap inputs, limiter ownership, batch ownership, issue-tag ownership, provenance fields, and future effect boundaries without adding hidden item effects.",
                                "Guard: Milestone03ProductionQualityTraceSmoke, Milestone03ProductionMaterialQualitySmoke, Milestone03ProductionFacilityQualitySmoke, Milestone03ProductionToolQualitySmoke, Milestone03ProductionFactionMutationSmoke, Milestone03ProductionFactionMutationAuditSmoke, Milestone03ProductionFatiguePressureSmoke, Milestone03ProductionLocationProvenanceSmoke, Milestone03ProductionMachineProvenanceSmoke, Milestone03ProductionOperatorProvenanceSmoke, Milestone03ProductionWorkforceModeProvenanceSmoke, Milestone03ProductionLegalStatusProvenanceSmoke, Milestone03ProductionSourceProvenanceSmoke, Milestone03ProductionBatchIssueTagsSmoke, Milestone03BatchIssueDefinitionAuditSmoke, Milestone03ProductionRepairHistoryProvenanceSmoke, Milestone03ManualProductionOperationRecordSmoke, Milestone03StaffedProductionExecutionSmoke, Milestone03ProductionDefectAppraisalSmoke, Milestone03ProductionWorkerQualitySmoke, Milestone03MachineKnowledgeSourceSmoke, Milestone03ProductionFacilityKnowledgeSmoke, Milestone03ProductionKnowledgeSourceAuditSmoke, Milestone03ProductionBatchProvenanceSmoke, Milestone03QualityProvenanceSmoke, Milestone03ProductionOutcomeForecastSmoke, Milestone03MachineConditionProductionSmoke, Milestone03MachineRepairWorkflowSmoke, and Milestone03ProductionOperatorSkillSmoke check quality caps, materials, facilities, tools, faction mutations, faction mutation audit, fatigue pressure, production location, station and operator identity, workforce mode, legal status, production source, batch issue tags, batch issue definitions, repair history, operation history, staffed execution, defect appraisal, staffing boundaries, machine and facility doctrine, knowledge-source audit, batches, persistence, condition, repair, skill, and outcome estimates."
                                + " Guard: Milestone03ProductionQualityDefinitionAuditSmoke checks cap inputs, owner boundaries, batch consequences, issue tags, provenance fields, and ordinary-effect limits."
                                + " Guard: Milestone03BatchIssueDefinitionAuditSmoke checks issue owners, supported tags, recall reservation, effect boundaries, evidence boundaries, and raw-ID hiding."
                                + " Guard: Milestone03QualityBandDefinitionAuditSmoke checks shared quality order, doctrine bands, Shoddy boundary, item value/charge multipliers, profile roles, capping rule, and raw-ID hiding."
                        ),
                        List.of("skill-progression", "construction-blueprints", "transfer-workflows", "menu-uniformity")),
                new MechanicEntry("skill-progression", "Skill Progression", "Character",
                        "Skill progression spends XP on durable capabilities while the Knowledge Tree remains responsible for recipes, doctrines, recognition, and explanations.",
                        SkillTreeProgressionAuthority.infopediaLines(),
                        List.of("production-forecast", "faction-personnel", "contract-evidence")),
                new MechanicEntry("construction-blueprints", "Construction Blueprints", "Construction",
                        "Construction organizes all available blueprints by category and page, then forecasts placement, materials, components, quality, knowledge, faction, and workbench requirements.",
                        List.of(
                                "A blueprint remains a preview until placement is explicitly confirmed at the construction cursor.",
                                "Category and page controls keep the complete catalog reachable while the detail pane explains exact placement refusal or readiness.",
                                "The blueprint definition audit names the room-blueprint schema, relative cells, anchors, object matrix, itemized cost/labor mapping, collisionless ghost rule, no-self-entombment exit warnings, and the current boundary that schematic quality upgrades remain future work.",
                                "The blueprint parity audit names catalog coverage, player-facing names, category coverage, sample room mapping, faction and knowledge gates, and future owners for vendor stock, acquisition paths, heat, and suspicion.",
                                "The blueprint acquisition audit maps construction plans to public markets, faction representatives, permit offices, quartermasters, noble factors, contract rewards, salvage research, and black-market routes while keeping live vendor offers future-owned.",
                                "Construction previews distinguish owning a blueprint from having permission, reputation, license, permit, materials, workbench, knowledge, placement access, utilities, and labor to build it.",
                                "The blueprint permission readiness audit classifies public, permit-or-license, faction-standing, restricted legal-access, and illicit or stolen-risk gates while keeping live purchase and reputation systems future-owned.",
                                "The blueprint faction construction capability audit marks which existing blueprints are plausible faction-construction candidates and which planning blockers remain, such as permission readiness, faction budget, crew, room claim, or materials.",
                                "The blueprint faction construction job definition audit names future job lifecycle states and required fields before any faction job queue, crew assignment, room claim, material reservation, or world mutation exists.",
                                "The blueprint faction construction material reservation audit derives supplies, parts, and named component requirements before any future faction job can reserve inventory, stage materials, or complete construction.",
                                "The blueprint faction construction crew assignment audit names required crew profiles, assigned crew previews, and labor-turn requirements before any future faction job can bind workers or schedule construction.",
                                "The blueprint faction construction site readiness audit names room-claim, placement, access-route, no-self-entombment, utility, and staged-construction handoff checks before any future faction job can reserve a target site.",
                                "The blueprint faction construction budget and heat authorization audit estimates faction budget needs and projected heat or suspicion before any future faction job can spend funds, stage construction, or mutate attention meters.",
                                "The blueprint faction construction cancellation release audit requires a cancellation or failure reason before any future faction job can release site, crew, material, budget, or attention holds.",
                                "Live placement guards also reject the player's current tile, NPC-occupied tiles, and claimed-room placements that would leave no valid access path to a door or exit.",
                                "The construction utility readiness audit names blueprint and room utility hooks for power, water, waste, ventilation, exhaust, light, data, road access, storage access, and staffing access while keeping live utility networks future-owned.",
                                "The room construction parity audit marks faction room stamps with player acquisition status and player construction blueprints with faction-use status, including documented non-acquirable utility and transition exceptions.",
                                "The blueprint expansion heat audit projects heat and suspicion from blueprint category, visible commerce, defenses, production footprint, laboratories or clinics, legality gates, and faction-visible assets without mutating live meters.",
                                "The progressive construction audit names staged site fields for required materials, inserted materials, labor, visual progress, final symbol, original walkable tile, quality, faction, save/load persistence, ghost visuals, and tool-adjusted time costs; live placement can now start prepaid or partial staged sites, reserve the world tile with a construction placeholder, restore the final tile on completion, restore the original tile on dismantle, show map and inspection text, run a save/load round-trip guard, stage available missing materials through Work, and recover inserted materials from unfinished sites through Dismantle.",
                                "Guard: Milestone02ConstructionReadabilitySmoke and Milestone02ConstructionCategorySmoke check forecast truthfulness and full-catalog reachability.",
                                "Guard: Milestone03BlueprintDefinitionAuditSmoke checks blueprint schema coverage, quality boundary, preflight rules, ghost collision boundary, sample cost/labor, and raw-ID hiding. Guard: Milestone03BlueprintNoSelfEntombmentAuditSmoke checks anchorless-room exit warnings. Guard: Milestone03SelfEntombmentRuntimeGuardSmoke checks live player-tile, NPC-tile, and claimed-room exit-path guards.",
                                "Guard: Milestone03BlueprintParityAuditSmoke checks catalog names, category coverage, sample room mapping, access explanations, future-owner gaps, parity boundaries, and raw-ID hiding.",
                                "Guard: Milestone03BlueprintAcquisitionPathAuditSmoke checks path coverage, representative labels, access gates, sample acquisition paths, future-owner boundaries, and raw-ID hiding. Guard: Milestone03BlueprintOwnershipPermissionReadabilitySmoke checks blueprint ownership versus build permission and resource readability. Guard: Milestone03BlueprintPermissionReadinessAuditSmoke checks permission classes, sample blockers, ownership separation, future-owner boundaries, and raw-ID hiding. Guard: Milestone03BlueprintFactionConstructionCapabilityAuditSmoke checks faction candidate coverage, readiness blockers, sample capability lines, future-owner boundaries, and raw-ID hiding. Guard: Milestone03BlueprintFactionConstructionJobDefinitionAuditSmoke checks job lifecycle states, required fields, sample definitions, future execution boundaries, and raw-ID hiding. Guard: Milestone03BlueprintFactionConstructionMaterialReservationAuditSmoke checks requirement ledgers, sample reservation readiness, missing-material blockers, future execution boundaries, and raw-ID hiding. Guard: Milestone03BlueprintFactionConstructionCrewAssignmentAuditSmoke checks crew profiles, sample readiness, labor requirements, future execution boundaries, and raw-ID hiding. Guard: Milestone03BlueprintFactionConstructionSiteReadinessAuditSmoke checks room claim readiness, placement and exit-path blockers, utility profiles, future execution boundaries, and raw-ID hiding. Guard: Milestone03BlueprintFactionConstructionBudgetHeatAuthorizationAuditSmoke checks budget estimates, heat projections, sample readiness, future execution boundaries, and raw-ID hiding. Guard: Milestone03BlueprintFactionConstructionCancellationReleaseAuditSmoke checks release requirements, cancellation reasons, sample readiness, future execution boundaries, and raw-ID hiding.",
                                "Guard: Milestone03RoomConstructionParityAuditSmoke checks faction-room player acquisition status, player-blueprint faction-use status, documented exceptions, sample mappings, future-owner boundaries, and raw-ID hiding.",
                                "Guard: Milestone03BlueprintExpansionHeatAuditSmoke checks blueprint heat coverage, driver categories, sample impacts, readability bands, future-owner boundaries, and raw-ID hiding. Guard: Milestone03BlueprintUtilityReadinessAuditSmoke checks utility-bearing room and blueprint coverage, fail-closed missing utility behavior, passability interaction, future-owner boundaries, and raw-ID hiding.",
                                "Guard: Milestone03ProgressiveConstructionDefinitionAuditSmoke checks staged site metadata, progress text, visual fade, tool timing, persistence fields, boundaries, and raw-ID hiding. Guard: Milestone03ProgressiveConstructionPersistenceSmoke checks staged-site save/load round trips. Guard: Milestone03ProgressiveConstructionDismantleSmoke checks unfinished-site removal and material recovery. Guard: Milestone03ProgressiveConstructionTileSyncSmoke checks live placement and completion tile sync. Guard: Milestone03ProgressiveConstructionOriginalTileSmoke checks original-tile preservation and restoration."
                        ),
                        List.of("production-forecast", "movement-planning", "menu-uniformity")),
                new MechanicEntry("expansion-heat", "Expansion Heat", "Economy",
                        "Expansion heat summarizes suspicion and gang attention created by visible commerce, defenses, production, laboratories, clinics, restricted assets, and recorded business heat.",
                        List.of(
                                "Auspex reports readable attention bands and likely drivers rather than exposing internal heat fields.",
                                "Blueprint construction can project heat and suspicion before live mutation: commerce, defenses, production footprint, laboratories or clinics, legality gates, and faction-visible assets all contribute audit-only drivers.",
                                "Relief guidance identifies lower-profile development and heat reduction paths without promising that attention disappears immediately.",
                                "Guard: Milestone02ExpansionHeatReadabilitySmoke checks attention bands, asset drivers, business heat, and relief guidance.",
                                "Guard: Milestone03BlueprintExpansionHeatAuditSmoke checks blueprint heat coverage, driver categories, sample impacts, readability bands, future-owner boundaries, and raw-ID hiding."
                        ),
                        List.of("contract-evidence", "construction-blueprints")),
                new MechanicEntry("interaction-approach", "Interaction Approach Planning", "Movement",
                        "Approach planning finds the shortest reachable tile adjacent to a visible interaction target and opens the existing movement ghost for confirmation.",
                        List.of(
                                "Approach never moves the character automatically; the player can inspect, adjust, confirm, or cancel the proposed route.",
                                "NPCs, animals, vendors, machines, containers, and base objects can request the same adjacent-tile planning rule.",
                                "Guard: Milestone02InteractionApproachSmoke checks shortest reachable adjacency, blocked targets, and explicit confirmation."
                        ),
                        List.of("movement-planning", "look-examine", "context-prompts")),
                new MechanicEntry("contract-evidence", "Contract Objectives and Evidence", "Quests",
                        "Contract summaries explain the objective, route certainty, required proof or delivery item, current evidence location, and promised reward without exposing contract IDs.",
                        List.of(
                                "Evidence may be carried, stored at base, or missing; the Map objective pane reports the known state directly.",
                                "Route wording distinguishes a known destination from uncertain guidance instead of inventing precision.",
                                "Contract summaries can now include skill and knowledge proof readiness for relevant jobs, such as Certified Market Appraisal, Investigation Trace Reading, Streetwise Appraisal, fabrication inspection, Contract Negotiation, or Scrap-Forging Doctrine.",
                                "Skill proof is explanatory only: it does not complete the contract, pay rewards, bypass hand-in rules, or reveal hidden target identity before the owning contract flow allows it.",
                                "Guard: Milestone02ContractObjectiveReadabilitySmoke checks objective, evidence location, reward, route certainty, and ID sanitization.",
                                "Guard: Milestone03ContractSkillProofSmoke checks trained and untrained skill/knowledge proof readiness without leaking contract IDs.",
                                "Guard: Milestone03ContractSkillProofAuditSmoke checks the audit owner, readiness states, no-completion boundary, and raw-ID hiding."
                        ),
                        List.of("expansion-heat", "transfer-workflows", "movement-planning")),
                new MechanicEntry("transfer-workflows", "Transfer Workflows", "Inventory",
                        "Inventory, containers, and trade share a transfer grammar covering source, destination, quantity, permission, capacity, protected evidence, reversibility, confirmation, and cancellation.",
                        List.of(
                                "A preview describes one item at a time and preserves each surface's actual execution rules.",
                                "Mission or evidence goods receive explicit protection warnings; capacity or permission failures explain why the transfer cannot proceed.",
                                "Ordinary vendor trade refuses protected mission, evidence, or intelligence sales until a dedicated hand-in or explicit release flow owns the transaction.",
                                "Guard: Milestone02TransferWorkflowConsistencySmoke checks shared grammar across storage, container, and vendor previews."
                        ),
                        List.of("inventory-equipment", "contract-evidence", "medical-treatment", "production-forecast", "menu-uniformity")),
                new MechanicEntry("faction-personnel", "Faction Personnel and Staffing", "Management",
                        "Faction personnel references separate player command membership from the NPC worker roster and explain the supported station-assignment path without inventing member inventory control.",
                        List.of(
                                "Player command roles and NPC worker records remain separate tracks even when they share a comparable command tier scale.",
                                "Recruited workers may be assigned to supported machine or defense stations through station management with role and skill validation.",
                                "Direct duty editing, member inventory transfer, and member equipment commands remain unavailable because this compact recruit roster has no rank, location, or personal item ledger.",
                                "Guard: Milestone02FactionRosterReadabilitySmoke checks command separation, station assignment visibility, equipment privacy, record limits, loyalty, and staffing availability."
                        ),
                        List.of("production-forecast", "skill-progression", "construction-blueprints", "transfer-workflows", "menu-uniformity"))
        );
    }

    private static List<String> contextPromptInfopediaLines() {
        ArrayList<String> lines = new ArrayList<>();
        lines.add("Prompt source: ControlReferenceTextSubsystem composes context prompts from shared action labels and binding prompts.");
        lines.add("Keyboard/mouse users see keyboard prompts directly; controller views include controller text plus keyboard fallback for recovery.");
        lines.addAll(ControlReferenceTextSubsystem.contextPromptLines(4, ControlReferenceTextSubsystem.defaultContextPrompts()));
        lines.add("Guard: Milestone02ContextPromptReadabilitySmoke checks major-panel coverage and leak-free keyboard/controller prompt variants.");
        return lines;
    }

    private static List<String> movementPlanningInfopediaLines() {
        ArrayList<String> lines = new ArrayList<>();
        lines.add(ControlReferenceTextSubsystem.contextPromptLine("Movement planning", 0, InputAction.CONFIRM, InputAction.CANCEL, "Confirm the ghost target, nudge it, or cancel safely."));
        lines.add("Readable outcomes: Movement target selected, Partial route, Destination occupied, Path blocked, Cannot reach from here, or outside the current area.");
        lines.add("Guard: Milestone02MovementPlanningReadabilitySmoke checks selected, partial, occupied, blocked, out-of-area, and unreachable routes.");
        lines.addAll(MovementPlanningDefinitionAuditAuthority.infopediaLines(4));
        return lines;
    }

    private static List<String> menuUniformityInfopediaLines() {
        UniversalWindowAuthority authority = new UniversalWindowAuthority();
        ArrayList<String> lines = new ArrayList<>();
        lines.add(authority.playerFacingSummary());
        lines.addAll(authority.playerFacingMenuAuditLines());
        lines.addAll(MenuDefinitionAuditAuthority.playerFacingLines(authority));
        lines.add("Guard: Milestone02MenuUniformityReadabilitySmoke checks major-window coverage and leak-free menu audit wording.");
        lines.add("Guard: Milestone02MenuDefinitionAuditSmoke checks structured ownership, data, panes, actions, safeguards, and complete registered-window coverage.");
        return lines;
    }

    private static String formatMechanicEntry(MechanicEntry entry) {
        return MECHANIC_PREFIX + entry.title() + " [" + entry.category() + "]";
    }

    private static Optional<MechanicEntry> mechanicForEntry(String row) {
        if (row == null) return Optional.empty();
        String value = row.trim();
        if (!value.startsWith(MECHANIC_PREFIX)) return Optional.empty();
        String title = value.substring(MECHANIC_PREFIX.length());
        int bracket = title.lastIndexOf(" [");
        if (bracket >= 0) title = title.substring(0, bracket);
        String wanted = title.trim();
        return mechanicEntries().stream()
                .filter(e -> e.title().equalsIgnoreCase(wanted) || e.key().equalsIgnoreCase(wanted))
                .findFirst();
    }

    private static List<String> mechanicDetailLines(MechanicEntry entry) {
        ArrayList<String> lines = new ArrayList<>();
        lines.add(entry.title());
        lines.add("Reference: " + entry.key().replace('-', ' '));
        lines.add("Category: " + entry.category());
        lines.add("");
        lines.add("Purpose:");
        lines.add(PlayerFacingText.sanitize(entry.summary()));
        lines.add("");
        lines.add("Player-facing rules:");
        for (String line : entry.lines()) {
            lines.add(PlayerFacingText.sanitize(line));
        }
        lines.add("");
        List<String> related = relatedEntryRowsFor(entry);
        lines.add("Related entries:");
        if (related.isEmpty()) lines.add("None registered yet.");
        else lines.addAll(related);
        return lines;
    }

    static List<String> relatedRowsForEntry(AssetRegistry registry, String row, AssetType selectedType) {
        Optional<MechanicEntry> mechanic = mechanicForEntry(row);
        if (mechanic.isPresent()) return relatedEntryRowsFor(mechanic.get());
        Optional<AssetMetadata> metadata = metadataForEntry(registry, row);
        if (metadata.isEmpty()) return List.of();
        AssetMetadata m = metadata.get();
        ArrayList<String> related = new ArrayList<>();
        AssetRegistry safe = registry == null ? AssetRegistry.empty() : registry;
        for (AssetMetadata candidate : safe.byType(m.type())) {
            if (candidate.id().equals(m.id())) continue;
            related.add(formatEntry(candidate));
            if (related.size() >= 3) break;
        }
        return List.copyOf(related);
    }

    static Optional<String> firstRelatedRowForEntry(AssetRegistry registry, String row, AssetType selectedType) {
        List<String> rows = relatedRowsForEntry(registry, row, selectedType);
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }

    private static List<String> relatedEntryRowsFor(MechanicEntry entry) {
        ArrayList<String> rows = new ArrayList<>();
        if (entry == null) return rows;
        for (String key : entry.relatedKeys()) {
            mechanicByKey(key).map(SemanticAssetInfopediaAuthority::formatMechanicEntry).ifPresent(rows::add);
        }
        return List.copyOf(rows);
    }

    private static Optional<MechanicEntry> mechanicByKey(String key) {
        String wanted = key == null ? "" : key.trim();
        return mechanicEntries().stream()
                .filter(e -> e.key().equalsIgnoreCase(wanted))
                .findFirst();
    }

    private record MechanicEntry(String key, String title, String category, String summary, List<String> lines, List<String> relatedKeys) {
        boolean matchesFilter(String filter) {
            if (filter == null || filter.isBlank()) return true;
            String q = filter.toLowerCase(Locale.ROOT);
            if (key.toLowerCase(Locale.ROOT).contains(q)
                    || title.toLowerCase(Locale.ROOT).contains(q)
                    || category.toLowerCase(Locale.ROOT).contains(q)
                    || summary.toLowerCase(Locale.ROOT).contains(q)) {
                return true;
            }
            for (String line : lines) {
                if (line != null && line.toLowerCase(Locale.ROOT).contains(q)) return true;
            }
            return false;
        }
    }
}
