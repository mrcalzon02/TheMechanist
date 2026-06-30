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
                                "Item provenance records the immediate manual operator separately from skill. Staffed queued output records the staffed workforce mode when that queue run executes.",
                                "After a successful manual Craft finishes, one completion record enters shared machine-operation history.",
                                "A staffed generated-production assignment can execute one queued run when worker, knowledge, machine, room ownership, and inputs are ready. The run consumes concrete inputs, stores output in base storage, records provenance, decrements the queue, and writes shared operation history.",
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
                                "Work on a staged site can stage available missing materials and then add labor when materials are complete.",
                                "Dismantle removes an unfinished staged site, restores the original tile, recovers inserted materials, and loses labor progress.",
                                "Finished staged sites restore the final built symbol, become normal base objects, and can then use their completed facility behavior.",
                                "The construction progress command reports active staged-site count, ready-for-labor count, material-blocked count, material-ready count, in-work-reach count, nearly complete count, and next action lines for waiting sites.",
                                "When several staged sites are waiting, construction progress lists nearly complete and labor-ready work before material-blocked sites so the next useful action is easier to find.",
                                "Material-blocked staged sites name which missing materials are currently available to stage and which are still missing from base storage.",
                                "Construction progress marks sites already within work reach and tells the player to stand adjacent when a staged site is too far away to work.",
                                "The construction work command uses the same priority as construction progress, so adjacent labor-ready or material-ready sites are chosen before less useful blocked work."
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
                new MechanicEntry("interaction-approach", "Interaction Approach Planning", "Movement",
                        "Approach planning finds the shortest reachable tile adjacent to a visible interaction target and opens the existing movement ghost for confirmation.",
                        List.of(
                                "Approach never moves the character automatically; the player can inspect, adjust, confirm, or cancel the proposed route.",
                                "NPCs, animals, vendors, machines, containers, and base objects can request the same adjacent-tile planning rule."
                        ),
                        List.of("movement-planning", "look-examine", "context-prompts")),
                new MechanicEntry("contract-evidence", "Contract Objectives and Evidence", "Quests",
                        "Contract summaries explain the objective, route certainty, required proof or delivery item, current evidence location, and promised reward without exposing contract IDs.",
                        List.of(
                                "Evidence may be carried, stored at base, or missing; the Map objective pane reports the known state directly.",
                                "Route wording distinguishes a known destination from uncertain guidance instead of inventing precision.",
                                "Contract summaries include skill and knowledge proof readiness for relevant jobs, such as Certified Market Appraisal, Investigation Trace Reading, Streetwise Appraisal, fabrication inspection, Contract Negotiation, or Scrap-Forging Doctrine.",
                                "Skill proof is explanatory only: it does not complete the contract, pay rewards, bypass hand-in rules, or reveal hidden target identity before the contract allows it."
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
                                "Recruited workers may be assigned to supported machine or defense stations through station management with role and skill validation.",
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
