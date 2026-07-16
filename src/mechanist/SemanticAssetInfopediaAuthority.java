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
    public static final String VERSION = "0.9.10kl-stage4-semantic-asset-infopedia";

    private static final Pattern ID_PREFIX = Pattern.compile("^[A-Z0-9]{3,4}-[A-Z0-9]{3,4}\\b");
    private static final String MECHANIC_PREFIX = "MECHANIC - ";
    private static final String ITEM_PREFIX = "ITEM - ";

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
            for (ItemDef definition : ItemCatalog.ITEMS.values()) {
                if (!itemMatchesFilter(definition, q)) continue;
                out.add(formatItemEntry(definition));
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
        Optional<ItemDef> item = itemDefinitionForEntry(entry);
        if (item.isPresent()) return itemDetailLines(item.get());
        Optional<AssetMetadata> metadata = metadataForEntry(safe, entry);
        if (metadata.isEmpty()) {
            lines.add(entry == null || entry.isBlank() ? "Semantic Asset Index" : entry);
            lines.add("Reference ID: semantic-registry");
            lines.add("");
            lines.add("Purpose: The Semantic Asset Index explains what registered art, icons, portraits, tiles, objects, and fixtures are meant to represent in the game.");
            lines.add("Active type filter: " + typeLabel(selectedType));
            lines.add("Active search filter: " + (filter == null || filter.isBlank() ? "<none>" : filter.trim()));
            lines.add("");
            lines.add("How to use: click an asset row to inspect its exact 8-character ID, image preview, purpose/type, path, and semantic description. Press / or the FILTER button to focus search, type to search ID/name/path/description, Backspace to delete, Enter to return to list navigation, and CLEAR to reset.");
            return lines;
        }

        AssetMetadata m = metadata.get();
        lines.add(m.name());
        lines.add("Reference ID: " + m.id());
        lines.add("Type / purpose: " + m.type().displayName());
        lines.add("");
        lines.add("Semantic description:");
        lines.add(m.semanticDescription());
        lines.add("");
        if (m.type() == AssetType.PORTRAIT) {
            List<String> partitionLines = PortraitSemanticAssetAuthority.infopediaLines(safe.projectRoot(), m.id());
            if (!partitionLines.isEmpty()) {
                lines.add("Portrait usage:");
                lines.addAll(partitionLines);
                lines.add("");
            }
        }
        lines.add("This row explains what the art represents when it appears in the game.");
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
                                "Look reports surface observations first; Examine can add intent, state, equipment, condition, and nearby context when the target supports a deeper read."
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
                        List.of("context-prompts", "movement-planning", "input-rebinding")),
                new MechanicEntry("input-rebinding", "Input Rebinding", "Controls",
                        "Input rebinding shows action labels, contexts, default bindings, controller prompts, conflict notes, required recovery actions, and current profile readiness.",
                        List.of(
                                "The Controls screen groups actions by context so movement, menu navigation, interaction, combat, and system recovery bindings can be inspected separately.",
                                "Required recovery actions keep a usable default path so the player can still back out, confirm, or reopen controls after changing bindings.",
                                "Conflict notes explain when one input overlaps another; the active panel decides which action the input performs.",
                                "Controller prompt tuning changes how controller hints are displayed while keyboard and mouse prompts remain available as fallback text."
                        ),
                        List.of("context-prompts", "movement-planning", "menu-uniformity")),
                new MechanicEntry("body-condition", "Body Condition", "Health",
                        "Body condition translates wounds, bleeding, infection risk, pain, fatigue, hunger, thirst, clothing, and held equipment into readable readiness and danger.",
                        List.of(
                                "The Character panel distinguishes immediate danger, impaired readiness, and stable condition without exposing internal health records.",
                                "Wounds and bleeding demand attention first; infection risk, pain, fatigue, food, and water can then explain reduced readiness."
                        ),
                        List.of("medical-treatment", "menu-uniformity")),
                new MechanicEntry("medical-treatment", "Medical Treatment Readiness", "Health",
                        "Treatment readiness explains which carried supplies can address current injuries and which conditions still lack a supported treatment action.",
                        List.of(
                                "Treatment previews are informational until the panel offers an explicit treatment command; they do not silently consume items.",
                                "The readout distinguishes bleeding control, wound care, infection control, and pain relief from unsupported or missing treatment paths."
                        ),
                        List.of("body-condition", "transfer-workflows")),
                new MechanicEntry("inventory-equipment", "Inventory and Equipment", "Inventory",
                        "Inventory rows represent individual units while item-family matching connects quality or manufacturing variants to equipment and catalog rules.",
                        List.of(
                                "The detail pane separates exact selected-quality units from related units in the same item family and reports how many quality grades are present.",
                                "Use, Equip, Store, and Take affect one selected unit at a time; repeated commands are required for additional units.",
                                "Quality changes item identity and value, while condition remains unavailable unless a separate durability record is explicitly attached.",
                                "Quality bands run Junk, Shoddy, Common, Serviceable, Fine, Masterwork, Noble, and Archeotech; Shoddy is a degradation quality rather than a target doctrine school.",
                                "Mission, evidence, and intelligence goods receive protection warnings and cannot be sold through ordinary vendor trade."
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
                                "A forge tutor, artificer, mechanic, machinist, engineer, or repair master can offer Train during conversation. Forge-Tutored Repair then lets one machine part restore a broken owned machine directly to serviceable integrity.",
                                "Manual operator skill maps the recipe's named skill onto an existing core stat. Novice, practiced, skilled, and expert operators support Common, Serviceable, Fine, and Masterwork output respectively while also adjusting defect risk.",
                                "Assigned recruit skill has a readable potential worker-quality tier. Manual Craft remains player-operated, while staffed queue runs use worker readiness when a staffed assignment executes.",
                                "A machine may store one installed recipe doctrine. Teach Machine requires the player to know the selected doctrine; afterward the machine can supply that recipe knowledge independently and persists it with the base object.",
                                "A claimed production facility may share doctrine from another serviceable station in the same room. Broken stations, stations outside the room, and unclaimed work areas cannot provide facility knowledge.",
                                "Recipe knowledge can come from the player, the selected machine's installed doctrine, or another serviceable station in the same claimed facility.",
                                "Crafted-item provenance preserves output quality, knowledge source, machine quality, facility quality, equipped tool quality, and the limiting cap through save/load and transfers.",
                                "Outcome forecasts compare estimated value, usable charges, and quality-sensitive defect risk. Manual Craft assigns one batch ID and inspection disposition to all units from the action; flagged batches receive a visible 40% ordinary-trader resale penalty while item statistics remain unchanged.",
                                "Faction production mutation is visible before crafting: the faction profile names the output prefix and its effective value, charge, and defect-pressure consequences, then item provenance preserves that mutation after production and transfer.",
                                "Faction-pattern output can change the item prefix, value, charges, and defect pressure shown on the forecast and preserved after production.",
                                "Manual Craft uses the established fatigue readiness bands. Slightly tired and tired operators add visible defect pressure; the exhausted band blocks machinery operation until the player rests. Produced-item provenance records the pressure present during the run.",
                                "Forecast and item inspection preserve the producing room and claimed facility separately. Unclaimed world workspaces remain explicitly unclaimed rather than inheriting the player's base identity.",
                                "Forecast and item inspection preserve the exact producing station by name, role, and coordinates so same-quality machines remain distinguishable after transfer.",
                                "When a workbench is opened through Operate, its forecast and Craft action use that operated machine for compatibility, condition, quality, fatigue, wear, provenance, and completion history. Selecting another recipe keeps that machine-bound workbench context.",
                                "Operate and Craft on a built base machine open the same machine-bound workbench. Its recipe list shows only known recipes compatible with that machine; when none match, the workbench tells the player to operate another machine or learn a matching recipe.",
                                "Item provenance records the immediate manual operator separately from skill. Staffed queued output records the staffed workforce mode when that queue run executes.",
                                "After a successful manual Craft finishes, the event log summarizes final quality, main quality limiter, fatigue band, batch state, and defect risk, and the shared machine-operation history preserves that summary in the completion record. Use the crafting panel Status action or production_status for a base-wide live queue summary. A workbench or machine interaction Status action scopes queue counts, live operations, latest completion, and readiness to the machine being operated; its History action likewise filters completed records. Use crafting panel History or production_history for recent base-wide records.",
                                "A staffed generated-production assignment advances in the background as ordinary game turns pass, including while the player leaves the workbench. Each completed run consumes concrete inputs, routes output through the selected destination, records provenance, decrements the queue, and writes shared operation history.",
                                "Staff Jobs opens the operated machine's generated-production setup. Category, readiness, and page controls keep compatible known jobs reachable; Assign validates access, doctrine, machine quality, and apparatus; Worker selects the next valid recruit; Queue minus and plus maintain a bounded 0 to 20 run queue. Changing jobs clears the prior queue and current-run progress so runs cannot silently change output.",
                                "Materials cycles shortage handling between waiting with the worker assigned, pausing and releasing that worker for another station, or cancelling the remaining queue. A blocked run does not consume inputs, add output, wear the machine, decrement the queue, or write completion history.",
                                "Output routes completed goods to unlimited Base Storage, the nearest claimed-room faction container with available capacity, or an interactable floor pile near the machine.",
                                "No Room cycles destination failure handling between waiting, releasing the worker, cancelling the queue, or dumping output into the nearby floor pile. Clear cancels remaining runs and current progress. Queue policies, progress, and the last blocker persist with the machine.",
                                "Production opens the base-wide production board. It prioritizes blocked and running machines, shows each selected machine's job, worker, queue, progress, policies, and last blocker, and provides the same worker, queue, policy, clear, Status, and History controls without reopening every station. Workbench opens that machine's staffed-job setup.",
                                "Staffed execution revalidates the specifically assigned recruit. A missing or no-longer-recruited assigned worker pauses the queue, and another available recruit never silently substitutes.",
                                "Item provenance records whether the run was immediate manual Craft or staffed queued production, so inspection can distinguish manual or staffed workforce ownership after transfer.",
                                "Generated staffed output preserves the variant law status used by production access rules: lawful, restricted, black-market, contraband, profaned, or hostile-social context.",
                                "Generated staffed output also preserves the generated recipe source and note that produced the variant. This is recipe/source provenance, not blueprint ownership.",
                                "Batch issue tags preserve Phase 9.3 batch signals such as good, defective, contaminated, unstable, restricted, stolen-risk, counterfeit, or faction-certified when those signals are present in inspection, recipe, law, or source metadata. Only the existing defect appraisal changes ordinary resale value.",
                                "Batch issue tags can record defect, contamination, instability, counterfeit, stolen-risk, restricted, faction-certified, or reserved recall signals when those signals are present.",
                                "Repair history records the current field-repair note from a repaired production machine onto later output.",
                                "Item inspection summarizes quality, production source, workforce mode, law status, batch issues, repair history, and the cap that limited the final result."
                        ),
                        List.of("skill-progression", "construction-blueprints", "transfer-workflows", "menu-uniformity")),
                new MechanicEntry("skill-progression", "Skill Progression", "Character",
                        "Skill progression spends XP on durable capabilities while the Knowledge Tree remains responsible for recipes, doctrines, recognition, and explanations.",
                        List.of(
                                "Skill Tree spending uses XP to unlock durable character capabilities; Knowledge Tree unlocks recipes, doctrines, recognition, and explanations.",
                                "Unlocked skill nodes persist separately from unlocked knowledge, and spending XP on a skill does not grant recipe doctrine.",
                                "The Character Skills tab lists readable branches and nodes, shows XP cost, prerequisite, access, stat, specialization, capability, and visible effect, and enables Unlock only through the same validated spending path used by skill commands.",
                                "Qualifying specialist conversations offer Train and open the matching trainer-gated node with temporary in-person access. Leaving the training panel removes that trainer access without removing learned skills.",
                                "Skill nodes can improve field repair, batch appraisal, trace reading, machine operation, leadership handoff, and similar capabilities when their requirements are met.",
                                "Nodes show XP cost, prerequisites, visible effect, and any faction, trainer, equipment, facility, knowledge, stat, or specialization requirement.",
                                "Some specializations are mutually exclusive; choosing one blocks its sibling specialization.",
                                "Use the skill status and skill unlock controls to inspect XP, see available nodes, and spend XP when requirements are met."
                        ),
                        List.of("production-forecast", "faction-personnel", "contract-evidence")),
                new MechanicEntry("construction-blueprints", "Construction Blueprints", "Construction",
                        "Construction organizes all available blueprints by category and page, then forecasts placement, materials, components, quality, knowledge, faction, and workbench requirements.",
                        List.of(
                                "A blueprint remains a preview until placement is explicitly confirmed at the construction cursor.",
                                "Category and page controls keep the complete catalog reachable while the detail pane explains exact placement refusal or readiness.",
                                "Construction previews distinguish owning a blueprint from having permission, materials, workbench access, knowledge, placement access, utilities, and labor to build it.",
                                "Live placement rules reject the player's current tile, NPC-occupied tiles, and claimed-room placements that would leave no valid access path to a door or exit.",
                                "Construction previews can show heat and suspicion projections from visible commerce, defenses, production footprint, laboratories or clinics, legality gates, and faction-visible assets.",
                                "Confirmed placement creates a staged construction site instead of a finished facility. The site reserves the tile with a construction placeholder and remembers the final built symbol.",
                                "Staged sites show material progress, labor progress, missing materials, final symbol, map and inspection text, quality, faction, and saved progress.",
                                "Work on a staged site can stage available missing materials and then add labor when materials are complete; Work requires the selected staged site to still exist and be adjacent and spends a turn only when the site actually changes.",
                                "Dismantle removes an adjacent unfinished staged site, reports the site location, restores the original tile, recovers inserted materials, and loses labor progress.",
                                "The construction dismantle command removes an adjacent unfinished staged site through the same recovery rules as the Dismantle interaction and spends one turn when a site is removed.",
                                "When several unfinished staged sites are adjacent, the construction dismantle command prefers the least-complete site first.",
                                "Construction progress and construction status show the adjacent work target, its next action, and adjacent dismantle target before either command is run.",
                                "If no staged site is adjacent, construction progress and construction status say no work target is in reach and point to the nearest staged site.",
                                "When no staged site is adjacent, construction progress and construction status say no dismantle target is in reach and point to the nearest staged site.",
                                "If construction dismantle finds staged sites but none are adjacent, it asks the player to stand adjacent and points to the nearest staged site.",
                                "The command help for construction status, construction progress, and construction dismantle names the same dismantle target guidance.",
                                "Finished staged sites restore the final built symbol, become normal base objects, and can then use their completed facility behavior.",
                                "The construction progress and construction status commands show the same staged-site packet.",
                                "That packet reports active staged-site count, ready-for-labor count, material-blocked count, material-ready count, in-work-reach count, nearly complete count, and next action lines for waiting sites.",
                                "When no staged construction sites are waiting, that packet says there is no next construction action, no work target, and no dismantle target.",
                                "When several staged sites are waiting, construction progress lists nearly complete and labor-ready work before material-blocked sites so the next useful action is easier to find.",
                                "If more staged sites are waiting than the progress packet lists, the overflow line names the next unlisted site, its location, progress, and next action.",
                                "Material-blocked staged sites name which missing materials are currently available to stage and which are still missing from base storage.",
                                "Construction progress marks sites already within work reach and shows distance and direction guidance when a staged site is too far away to work.",
                                "The construction work command uses the same priority as construction progress, so adjacent labor-ready or material-ready sites are chosen before less useful blocked work.",
                                "Construction work help names its 1 to 20 turn range, progress-priority target choice, and nearest-site guidance when no staged site is adjacent.",
                                "Construction work accepts 1 to 20 turns at a time and reports when a requested turn count is adjusted into that range.",
                                "Construction work spends the actual productive work turns it uses; material-only staging spends one turn, and held tools can reduce spent turns by adding more labor per turn.",
                                "Construction work progress reports name material staging and labor separately, then repeat the staged site's location and next action.",
                                "When construction work completes a staged site, the completion message names the finished structure and its map location.",
                                "If construction work finds no adjacent staged site, it points to the nearest staged site before asking the player to stand adjacent."
                        ),
                        List.of("production-forecast", "movement-planning", "menu-uniformity")),
                new MechanicEntry("expansion-heat", "Expansion Heat", "Economy",
                        "Expansion heat summarizes suspicion and gang attention created by visible commerce, defenses, production, laboratories, clinics, restricted assets, and recorded business heat.",
                        List.of(
                                "Auspex reports readable attention bands and likely drivers rather than exposing internal heat fields.",
                                "Blueprint construction previews can show projected heat and suspicion from commerce, defenses, production footprint, laboratories or clinics, legality gates, and faction-visible assets.",
                                "Relief guidance identifies lower-profile development and heat reduction paths without promising that attention disappears immediately."
                        ),
                        List.of("contract-evidence", "construction-blueprints")),
                new MechanicEntry("world-events", "World Events", "World",
                        "Top-down world events are sector conditions such as train outages, quarantines, export bans, supply shocks, relief shipments, repair periods, tithes, and civic observances. They are recorded separately from faction officers' schemes.",
                        List.of(
                                "Each event records its start and end turn, duration, scope, severity, eligibility reason, public notice channels, economy and population effects, shipment and reinforcement timing, vendor restrictions, local exceptions, physical risks, and aftermath.",
                                "Events are selected from conditions that make them suitable. Severe shortage, losses, delayed freight, or structural harm favor relief or repair instead of adding another arbitrary penalty.",
                                "Active events can advance or delay shipment windows and reinforcement arrivals, change freight risk, replenish limited reserves during relief, and contribute visible pressure to distant faction-network outcomes.",
                                "Import closures, export bans, and off-map sales restrictions state what closes. Local stock, internal faction issue, clinic treatment, relief distribution, and other named exceptions can remain available.",
                                "An event can visibly close or repurpose its exact generated import or market facility. The affected marker and room name show the event use, and a closed import node cannot receive train reinforcements.",
                                "The original marker, route status, room purpose, and room description remain attached to the event. Recovery restores that exact facility instead of leaving permanent unexplained damage.",
                                "Use world_events to read the active event, exact timing, restrictions, exceptions, physical risk, and recovery terms."
                        ),
                        List.of("population-markets", "expansion-heat")),
                new MechanicEntry("population-markets", "Population and Market Pressure", "Economy",
                        "Local population ledgers create visible demand for essential goods and connect resident capacity, workforce, losses, facilities, stock, and prices.",
                        List.of(
                                "NPC traders allocate basic food and water when the local population is large enough to support essential trade; medicine is also allocated when population or casualty pressure warrants it.",
                                "Population identity matters as well as size: duty and custody rosters raise ammunition and medical demand, industrial and transport labor raises tool and work-food demand, noble households raise luxury demand, and arrivals or displaced populations raise immediate food, water, and medical demand.",
                                "Population identity can allocate matching fallback goods such as tool bundles, ammunition, or noble delicacies when the relevant local demand exists.",
                                "Food, water, medicine, tools and components, and security goods compare local demand with the vendor's visible stock. The resulting surplus, balanced, tight, strained, or severe-shortage band changes both purchase prices and player sale value within bounded limits.",
                                "The trade panel names the local population target, assigned workforce, recorded losses, facility-linked population records, strongest pressure, and the selected offer's exact adjustment.",
                                "Population-allocated food and water draw from a finite persisted reserve. Production ledgers and local farms, hydroponics, kitchens, food stores, recyclers, and purifiers are preferred; rail shipments or a small emergency allotment are used only when local supply is unavailable.",
                                "Every essential shelf offer names its stock class, source facility or route, remaining reserve, and refill turn. A successful purchase consumes one unit, while failed purchases and reopened vendor sessions cannot recreate depleted stock.",
                                "Population also consumes matching faction food and water reserves once per world day. Demand follows persisted population capacity; immature crèche cohorts count as additional mouths and consume twice the normal food share while growing.",
                                "Each inhabited floor trades with its sewer layer below. Universal waste runoff feeds sewer fertilizer and basic chemical processing, those finite goods move upward by freight lift, and sewer markets pay a premium for food, clean water, filters, and tools brought down from above.",
                                "Weapon and ammunition shelves use separate finite reserves tied to faction supply policy. Concord military issue, Warden controlled security stock, Mechanist custody weapons, noble household arms, gang black-market stock, hidden-cell weapons, improvised sump arms, and civilian defensive goods keep distinct classifications and legality.",
                                "Security stock resolves to faction-controlled arms production, armories, munition stores, workshops, confiscated evidence, theft or black-market diversion, battlefield recovery, outside-sector rail shipments, or a small faction reserve. Rival-controlled production cannot supply the shelf.",
                                "Blockades close outside-sector arms routes and leave only a one-unit local reserve with a long refill. Failed purchases consume nothing; successful purchases consume the exact weapon or ammunition reserve, and depletion survives reopened traders and save/load.",
                                "Medical and drug shelves use finite reserves tied to clinics, laboratories, private physicians, faction stores, illicit producers, relief intake, rail imports, or event-diverted batches. Rival-controlled laboratories cannot supply the shelf.",
                                "Legal clinic medicine, restricted service medicine, private physician stock, black-market performance drugs, sump sedatives, counterfeit medicine, contaminated medicine, outside-sector pharmaceuticals, and disaster relief stock keep distinct source and legality records.",
                                "Stimulants name stimulant-strain and sleep-debt risk; illicit sedatives name dependency and contamination risk; counterfeit or contaminated batches carry an unsafe batch warning into item provenance. These warnings do not invent an addiction system where none exists.",
                                "Medical blockades close outside shipments and leave a one-unit local reserve. Failed purchases consume nothing, successful purchases consume the exact treatment or drug reserve, and depletion survives reopened traders and save/load.",
                                "Ordinary noble luxuries use finite estate, production, event-diverted, merchant-import, or household reserves. Their provenance preserves house control, source route, prestige or gifting purpose, and blockade, tax, seizure, or tithe effects.",
                                "Rare draughts are protected custody objects rather than ordinary valuables. Each has a minimum Common value of 850 script and records noble-house owner, off-world origin, broker, smuggler, physician, or merchant route, exact vault, quantity, authenticity, event status, and household purpose.",
                                "A house vault may hold a genuine draught for household use, prestige, gifting, bargaining, blackmail, medical privilege, private indulgence, inheritance, or hoarding without placing it on a trader shelf. Generic draught offers are withheld.",
                                "Only an explicit theft, smuggling, black-market, bargaining, or sale event can exceptionally release one draught unit. Genuine, diluted, counterfeit, contaminated, stolen, misdeclared, or house-certified identity and blockade, tax, seizure, or tithe status remain on the purchased item's custody trace.",
                                "Pet, animal, and agricultural shelves use finite reserves tied to compatible faction farms, gardens, fungus rooms, cloning nurseries, rail imports, or living farm, pet, and kennel animals. Rival-owned rooms, production, and animals cannot supply another faction's market.",
                                "Animal products, feed, pet bundles, and veterinary kits name the linked animal, breeder or owner, pen owner, handler, feed source, water station, and care source. Seeds, fungi, and cloning samples preserve their operated room, production facility, or inspected import route.",
                                "Animal disease screening and feed shortages reduce local output to a rationed reserve. Import restrictions close purely imported seed, breeding, and cloning stock instead of creating a replacement source; failed purchases consume nothing, while successful purchases consume one exact unit and depletion survives reopened traders and save/load.",
                                "Raw earth, quarried stone, ferric scrap, recovered industrial salvage, waste biomass, and refined metal stock use separate finite reserves. Sources resolve to compatible local mining, quarrying, salvage, recycling, scavenging, production, facility stockpiles, event relief or seizure, or outside-sector imports.",
                                "Noble, military, black-market, and ordinary merchant material imports retain their supplier class and rail route. Rival-controlled rooms and ledgers cannot supply the shelf, and blockades close outside material routes.",
                                "When an existing production or trade chain requests material before its extraction chain is simulated, only a one-unit bounded faction reserve is allowed. Its assumed source, reason, faction, locality, event modifier, and source-review requirement remain visible; failed purchases consume nothing and depletion survives reopened traders and save/load.",
                                "Controlled faction territories place staffed provision, armory, medical, industrial-blueprint, animal-care, luxury, or black-market counters according to faction identity and the rooms they control. Each trader has a physical market marker inside its supporting room.",
                                "Provisioners guarantee food and water; military quartermasters guarantee weapons, ammunition, and armor; dispensaries guarantee treatment stock; and industrial factors guarantee tools, construction supplies, carried room and machine blueprint records, and vehicle-service components.",
                                "Illicit factions can promote a controlled room into a concealed chem kitchen with a chemical fixture, a working chem cook, a production record, and a black-market dealer carrying weapons, ammunition, narcotics, tools, and restricted goods.",
                                "Noble estates can promote a controlled store into a secured luxury and draught vault. The estate broker sells ordinary luxury stock, while rare draughts remain in exact protected custody unless an explicit release event permits sale.",
                                "Faction-market offers remain visible when standing, faction membership, permits, military issue rules, noble patronage, suspicion, or a world event blocks purchase. Every selected offer reports its market class, exact access requirement, consequence, and local-event exception before money, stock, inventory, or time can change.",
                                "Black-market dealers visibly prefer several narcotic lines and accept underworld credibility, while legal dispensaries and noble brokers gate controlled medicine through standing or access papers. Stolen and counterfeit provenance is disclosed rather than erased, and protected draught custody remains exceptional even when its political value is known.",
                                "Faction representatives generate market supply contracts from the first concrete pressure they can identify: active event response, delayed or intercepted cargo, reinforcement support, depleted essential or material reserves, recorded market pressure, or faction-identity demand. With no market pressure, ordinary production work remains available.",
                                "Market contract turn-in consumes the named item, pays through the normal contract flow, reduces faction market pressure, and updates the exact linked reserve, shipment, reinforcement request, or event response where one exists. Contract completion also enters the public news ledger.",
                                "Active and recovered top-down events enter newspapers and broadcasts as confirmed sector-event notices. Vendor panels and faction representatives show the same restriction and local exception immediately, while ordinary faction rumors and editorial framing can remain incomplete or biased.",
                                "Agricultural civilian, lower-hive, and noble territories can promote a controlled room into a farm and animal-care facility with grow space, feed, water, cleaning, handler, and veterinary support. A staffed animal-supply trader then carries feed, pet supplies, and veterinary kits.",
                                "Incoming external stock receives a persisted shipment manifest naming its supplier and source site, destination faction and facility, arrival node, cargo and value, legality, quality risk, event modifier, interception and delay risk, arrival window, operational mode, and whether the manifest is player-visible.",
                                "Each destination faction applies its own off-map procurement policy. The manifest records total source cost, landed unit-cost floor, base cooldown, a large deterministic positive or negative cooldown variance, effective cooldown, and the exact world turn when that source can provide another shipment.",
                                "Arrived shipment cargo can reach a trader shelf. Delayed cargo remains withheld until its arrival window opens, intercepted cargo never appears as delivered stock, and reopening a trader cannot bypass either state.",
                                "After delivered cargo is exhausted, a refilled linked reserve still cannot create another off-map shipment until the faction source cooldown expires. Military, noble, black-market, Mechanist, civilian, and irregular salvage routes use distinct cooldown and procurement-cost policies.",
                                "A successful purchase consumes one unit from both the shipment manifest and its linked finite reserve; a failed purchase consumes neither. Shipment status, remaining cargo, route, and risk survive save/load.",
                                "Distant stock-movement ledgers can create abstract operational shipments with real manifests, routes, cargo value, and destination ownership, so external supply can support the economy without simulating every remote factory or mine at local detail.",
                                "Zone generation promotes one controlled receiving room into an identifiable import node suited to its setting: sector exchange or rail cargo station, freight elevator, service lift, customs checkpoint, road loading bay, high-level cargo dock, noble private import room, concealed smuggling entry, or sewer freight hoist.",
                                "The promoted room contains a persistent physical arrival marker. External shipment manifests name that exact marker, while Look and Interact report arrived, delayed, scheduled, intercepted, and completed cargo traffic.",
                                "Free slow train reinforcements bind to the generated import intake roster, name the exact arrival marker in personnel provenance, and materialize on an open tile beside it. The early floor-five zone-2,2 sector exchange is selected from world coordinates rather than being the only hardcoded route.",
                                "Distant faction activity uses compact persisted network ledgers instead of ticking every remote actor, room, machine, and item. Each ledger tracks strength, influence, wealth, population and reinforcement pressure, suppliers, routes, shipments, stockpiles, materials, machinery, quality, efficiency, import capacity, rivals, leadership, schemes, events, and player disruption.",
                                "A distant network resolves on a variable review timer. Its success chance is calculated from recorded support and pressure factors, while the deterministic roll, factors, next review turn, and outcome remain visible through the faction import node and the distant_network command.",
                                "Distant outcomes have concrete limits: they may advance or delay incoming shipments and reinforcements, improve or disrupt linked raw-material reserves, or shift persistent faction strength, influence, wealth, and rival pressure. They do not create full remote rooms, actors, or loose item piles.",
                                "A local faction production site uses assigned workers from matching population rosters. An unstaffed site pauses production and exports; a staffed site produces bounded stock, shelf exports consume that stock, and depleted sites contribute no free replacement item.",
                                "Trade supply context states whether a source site is staffed, unstaffed, or depleted. Site-produced shelf goods preserve their facility and production provenance.",
                                "Faction casualties open durable replacement manifests with a semi-random availability turn and a source-specific arrival window. Dead workers leave their assigned roster slot before a replacement can consume reserve population.",
                                "Reinforcement trains are free but slow and require a faction-linked rail intake. Barracks reserve musters are fastest, require barracks or duty infrastructure, and charge a modest equipment-processing fee. Paid local recruitment needs only an available population roster, uses a medium timer, and costs the most script per person.",
                                "A faction representative reports the selected source, exact price, infrastructure prerequisite, availability turn, and whether reinforcements are inbound, ready, route-delayed, capacity-blocked, or expired. Change Source cycles supported methods and resets the manifest timer to the newly selected method.",
                                "Receive Reinforcements admits up to four ready personnel through the selected linked roster or import intake, charges only for personnel who actually arrive, preserves arrival provenance, and spends a turn only when somebody arrives.",
                                "Ready personnel wait when staffed housing or faction-room capacity is full, and a blocked manifest expires if the faction cannot receive it before the stated turn."
                                ,"A planned crèche counts as operating only with at least 24 floor tiles, one care provider, food storage, water storage, a child bed unit, and a teaching station. Missing requirements prevent happiness, newborn intake, and cohort recruitment benefits."
                                ,"Crèche happiness scales gradually to a maximum +25 faction boost at ten operating crèches. One care provider supports up to twelve children, while each dense child bed unit holds four; both care and bed limits apply."
                                ,"Operating crèches accept a small yearly abstract abandoned-or-ward cohort and also accept newborns from faction members whose recorded pregnancy reaches its due world turn. Parent births retain the parent identity; when care or beds are full, the due birth waits."
                                ,"Every immature cohort member adds extra growth-food, clean-water, and pediatric-care market pressure. That child-specific pressure ends when the cohort reaches young adulthood."
                                ,"Children remain aggregate and cannot be recruited. After sixteen full world years, Muster Cohort can materialize up to six mature young adults with crèche upbringing and birth-source provenance."
                                ,"Every adult faction NPC tracks persisted happiness from 0 to 100. Food, water, current pay, a bed, housing appropriate to rank, and faction-owned crèches raise the target; shortages, arrears, homelessness, and rank-inappropriate quarters lower it gradually over time."
                                ,"NPCs at 45 happiness or lower become vulnerable to rival recruitment. The exact chance rises with their unhappiness, the happiness advantage of the destination faction, and the recruiting officer's standing; recent faction changes and full destination housing block recruitment."
                                ,"Faction officers can pursue rival recruitment through the normal planning, execution, and cooldown framework. A successful scheme transfers one named NPC and consumes destination population capacity; a failed approach leaves membership unchanged."
                                ,"Each faction plan leader has a persisted personal scheme cadence from 80% to 125%. Planning and cooldown also receive a bounded cycle-specific jitter of minus four to plus four hours, so officers who begin together do not repeatedly plan or recover in lockstep. The faction ledger shows the officer cadence, current jitter, and next phase turn."
                                ,"Leaving for the general populace is more severe than recruitment: an NPC must remain at 12 happiness or lower with at least three severe failures among food, water, pay, and a bed for seven continuous days. Departure becomes certain only after fourteen continuous days, and the NPC remains in the world as an unaffiliated resident."
                        ),
                        List.of("faction-personnel", "transfer-workflows", "production-forecast")),
                new MechanicEntry("interaction-approach", "Interaction Approach Planning", "Movement",
                        "Approach planning finds the shortest reachable tile adjacent to a visible interaction target and opens the existing movement ghost for confirmation.",
                        List.of(
                                "Approach never moves the character automatically; the player can inspect, adjust, confirm, or cancel the proposed route.",
                                "NPCs, animals, vendors, machines, containers, and base objects can request the same adjacent-tile planning rule."
                        ),
                        List.of("movement-planning", "look-examine", "context-prompts")),
                new MechanicEntry("contract-evidence", "Contract Objectives and Evidence", "Quests",
                        "Contract summaries explain the objective, route certainty, required proof or delivery item, production standard, current evidence location, and promised reward without exposing contract IDs.",
                        List.of(
                                "Evidence may be carried, stored at base, or missing; the Map objective pane reports the known state directly.",
                                "Route wording distinguishes a known destination from uncertain guidance instead of inventing precision.",
                                "Contract summaries include skill and knowledge proof readiness for relevant jobs, such as Certified Market Appraisal, Investigation Trace Reading, Streetwise Appraisal, fabrication inspection, Contract Negotiation, or Scrap-Forging Doctrine.",
                                "A faction representative can issue one production order through Take Work when that faction has no active contract. Accepting the order records it and spends one turn.",
                                "Production orders require the named item at the listed minimum quality, a recorded production origin, and a passed batch inspection; low-quality, untraced, and defect-flagged substitutes remain carried and do not complete the order.",
                                "At a matching faction representative, Turn In requires the carried proof item and rechecks every listed skill, knowledge, quality, production, and inspection requirement. A valid hand-in consumes the qualifying unit, completes the contract, pays script, awards faction standing and listed skill XP, and spends one turn; a blocked hand-in changes nothing."
                        ),
                        List.of("expansion-heat", "transfer-workflows", "movement-planning")),
                new MechanicEntry("transfer-workflows", "Transfer Workflows", "Inventory",
                        "Inventory, containers, and trade share a transfer grammar covering source, destination, quantity, permission, capacity, protected evidence, reversibility, confirmation, and cancellation.",
                        List.of(
                                "A preview describes one item at a time and preserves each surface's actual execution rules.",
                                "Mission or evidence goods receive explicit protection warnings; capacity or permission failures explain why the transfer cannot proceed.",
                                "Ordinary vendor trade refuses protected mission, evidence, or intelligence sales unless the game is using a hand-in or explicit release flow for that item."
                        ),
                        List.of("inventory-equipment", "contract-evidence", "medical-treatment", "production-forecast", "menu-uniformity")),
                new MechanicEntry("faction-personnel", "Faction Personnel and Staffing", "Management",
                        "Faction personnel references separate player command membership from the NPC worker roster and explain the supported station-assignment path without inventing member inventory control.",
                        List.of(
                                "Player command roles and NPC worker records remain separate tracks even when they share a comparable command tier scale.",
                                "Recruited workers may be assigned to supported machine or defense stations through station management with role and skill validation. Machine workbench Staff Jobs can cycle to the next valid recruit while configuring staffed production.",
                                "Direct duty editing, member inventory transfer, and member equipment commands are unavailable in the compact recruit roster because it does not expose rank, location, or personal item ledgers."
                        ),
                        List.of("production-forecast", "skill-progression", "construction-blueprints", "transfer-workflows", "menu-uniformity"))
        );
    }

    private static List<String> contextPromptInfopediaLines() {
        ArrayList<String> lines = new ArrayList<>();
        lines.add("Each major panel shows the actions that are currently meaningful for that panel.");
        lines.add("Keyboard and mouse users see keyboard prompts directly; controller views include controller text plus keyboard fallback for recovery.");
        lines.addAll(ControlReferenceTextSubsystem.contextPromptLines(4, ControlReferenceTextSubsystem.defaultContextPrompts()));
        return lines;
    }

    private static List<String> movementPlanningInfopediaLines() {
        ArrayList<String> lines = new ArrayList<>();
        lines.add(ControlReferenceTextSubsystem.contextPromptLine("Movement planning", 0, InputAction.CONFIRM, InputAction.CANCEL, "Confirm the ghost target, nudge it, or cancel safely."));
        lines.add("Readable outcomes: Movement target selected, Partial route, Destination occupied, Path blocked, Cannot reach from here, or outside the current area.");
        lines.add("The movement ghost is a preview until the player confirms it; cancelling returns control without moving.");
        lines.add("If the selected destination is blocked, occupied, or unreachable, the route preview explains the refusal instead of committing movement.");
        lines.add("Hazard and approach previews can add warnings before the player confirms a route.");
        return lines;
    }

    private static List<String> menuUniformityInfopediaLines() {
        ArrayList<String> lines = new ArrayList<>();
        lines.add("Major game menus use consistent titles, list/detail areas, back behavior, prompts, disabled-state explanations, and transfer previews.");
        lines.add("Back returns to the prior surface; Cancel uses the same safe exit unless a menu explicitly names different behavior.");
        lines.add("Disabled actions explain whether they need a selection, a resource, a permission, a destination, or a different screen.");
        lines.add("Storage, containers, vendors, and inventory screens describe source, destination, selected item, quantity, and protection warnings before transfer.");
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
            String sanitized = PlayerFacingText.sanitize(line);
            if (playerFacingInfopediaLine(sanitized)) lines.add(sanitized);
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
        if (itemDefinitionForEntry(row).isPresent()) {
            ArrayList<String> rows = new ArrayList<>();
            mechanicByKey("population-markets").map(SemanticAssetInfopediaAuthority::formatMechanicEntry).ifPresent(rows::add);
            mechanicByKey("world-events").map(SemanticAssetInfopediaAuthority::formatMechanicEntry).ifPresent(rows::add);
            mechanicByKey("production-forecast").map(SemanticAssetInfopediaAuthority::formatMechanicEntry).ifPresent(rows::add);
            return List.copyOf(rows);
        }
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

    private static String formatItemEntry(ItemDef definition) {
        return ITEM_PREFIX + definition.name + " [" + readableCategory(definition.category) + "]";
    }

    private static Optional<ItemDef> itemDefinitionForEntry(String row) {
        if (row == null || !row.startsWith(ITEM_PREFIX)) return Optional.empty();
        String name = row.substring(ITEM_PREFIX.length());
        int bracket = name.lastIndexOf(" [");
        if (bracket >= 0) name = name.substring(0, bracket);
        return Optional.ofNullable(ItemCatalog.get(name.trim()));
    }

    private static boolean itemMatchesFilter(ItemDef definition, String filter) {
        if (definition == null) return false;
        if (filter == null || filter.isBlank()) return true;
        String q = filter.toLowerCase(Locale.ROOT);
        return (definition.name + " " + definition.category + " " + definition.source + " "
                + definition.description + " " + definition.use).toLowerCase(Locale.ROOT).contains(q);
    }

    private static List<String> itemDetailLines(ItemDef definition) {
        ArrayList<String> lines = new ArrayList<>();
        lines.add(definition.name);
        lines.add("Category: " + readableCategory(definition.category) + (definition.weapon ? " / weapon" : ""));
        lines.add("Common value: " + definition.basePrice + " script before quality, scarcity, shipment, and vendor adjustments.");
        lines.add("");
        lines.add("What it is:");
        lines.add(PlayerFacingText.sanitize(definition.description));
        lines.add("Common sources and facilities: " + PlayerFacingText.sanitize(definition.source) + ".");
        lines.add("Common use: " + PlayerFacingText.sanitize(definition.use) + ".");
        lines.add("");
        lines.add("Market and provenance:");
        lines.add("Typical access: " + itemMarketClass(definition) + ".");
        lines.add("Common sellers: " + itemSellerLine(definition) + ".");
        lines.add("Availability: " + itemAvailabilityLine(definition) + ".");
        lines.add("A specific unit can also record local production, salvage, stockpile issue, train or outside-sector shipment, off-world import, relief, confiscation, theft, counterfeit or contaminated origin, or an unresolved fallback source.");
        lines.add("Quality, batch condition, legal status, faction ownership, shipment risk, event restrictions, and source cooldown can change value or access without changing the base item definition.");
        return List.copyOf(lines);
    }

    private static String itemMarketClass(ItemDef definition) {
        String text = itemText(definition);
        if (text.contains("chem/rare-campaign") || text.contains("draught")) return "noble-only protected custody unless an explicit release occurs";
        if (containsAny(text, "chem/cult", "forbidden", "warp", "heretic", "blasphem")) return "forbidden or contraband trade, normally limited to illicit channels";
        if (containsAny(text, "chem/ganger", "narcotic", "obscura", "stimm", "sedative")) return "controlled or illicit medicine whose seller and provenance determine access";
        if (definition.weapon || containsAny(text, "ammo", "munition", "explosive", "security")) return "regulated security stock that may require standing, membership, or access papers";
        if (containsAny(text, "luxury", "noble")) return "luxury or private-house stock that may require patronage or favorable standing";
        if (containsAny(text, "medical", "medicine", "clinic")) return "ordinary treatment stock unless the batch is controlled, restricted, or event-rationed";
        return "ordinary legal stock when a local vendor has a supported source and reserve";
    }

    private static String itemSellerLine(ItemDef definition) {
        String text = itemText(definition);
        if (containsAny(text, "chem/rare-campaign", "draught", "luxury", "noble")) return "noble estate brokers, private physicians, or exceptional custody holders";
        if (containsAny(text, "chem/ganger", "narcotic", "contraband", "forbidden")) return "black-market chem dealers and illicit faction counters";
        if (definition.weapon || containsAny(text, "ammo", "munition", "security")) return "military quartermasters, controlled security counters, industrial factors, or illicit arms dealers";
        if (containsAny(text, "food", "water", "ration")) return "provisioners, farms, kitchens, water services, relief counters, and ordinary markets";
        if (containsAny(text, "medical", "medicine", "clinic")) return "dispensaries, clinics, laboratories, private physicians, and relief counters";
        if (containsAny(text, "animal", "agriculture", "seed", "feed", "fertilizer")) return "farm, garden, animal-care, sewer-reclamation, and agricultural supply counters";
        if (containsAny(text, "material", "component", "tool", "construction")) return "industrial factors, workshops, salvage markets, and shipment-backed stores";
        return "general traders or faction specialists whose facilities and market identity support the item";
    }

    private static String itemAvailabilityLine(ItemDef definition) {
        String text = itemText(definition);
        if (containsAny(text, "chem/rare-campaign", "draught")) return "usually held in a secured estate vault and absent from ordinary shelves";
        if (containsAny(text, "food", "water", "medical", "ammo", "munition")) return "finite local reserves, population demand, production, and emergency allocation determine whether shelf stock remains";
        if (containsAny(text, "raw", "material", "component", "tool", "construction")) return "local extraction, salvage, production, shipment arrival, route safety, and supplier cooldown determine supply";
        return "facility operation, faction preference, finite stock, shipment state, access rules, and active world events can all withhold it";
    }

    private static String itemText(ItemDef definition) {
        return (definition.name + " " + definition.category + " " + definition.source + " " + definition.use)
                .toLowerCase(Locale.ROOT);
    }

    private static boolean containsAny(String text, String... terms) {
        for (String term : terms) if (text.contains(term)) return true;
        return false;
    }

    private static String readableCategory(String category) {
        if (category == null || category.isBlank()) return "uncategorized";
        return category.replace('/', ' ').replace('-', ' ');
    }

    private static boolean playerFacingInfopediaLine(String line) {
        if (line == null || line.isBlank()) return false;
        String l = line.toLowerCase(Locale.ROOT);
        return !l.contains("guard")
                && !l.contains("smoke")
                && !l.contains("milestone")
                && !l.contains("authority")
                && !l.contains("audit")
                && !l.contains("future")
                && !l.contains("owner=")
                && !l.contains("raw-id")
                && !l.contains("raw id")
                && !l.contains("migration note")
                && !l.contains("stage status");
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
                if (playerFacingInfopediaLine(line) && line.toLowerCase(Locale.ROOT).contains(q)) return true;
            }
            return false;
        }
    }
}
