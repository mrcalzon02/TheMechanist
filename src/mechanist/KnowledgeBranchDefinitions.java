package mechanist;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Composed, game-owned character knowledge tree authority. This class keeps the
 * progression map diegetic and auditable: broad paths converge into specialist
 * doctrine, production quality is gated through a shared fabrication spine, and
 * all registered visible knowledge definitions are placed in at least one branch.
 */
final class KnowledgeBranchDefinitions {
    enum KnowledgeCategory { SURVIVAL, FABRICATION, INFRASTRUCTURE, INDUSTRY, SECURITY, MEDICAL, CIVIC, MECHANIST }
    enum KnowledgeTier { FOUNDATION, PRACTICAL, SKILLED, SPECIALIST, AUTHORITY, FORBIDDEN }
    enum KnowledgeVisibility { ACTIVE, HIDDEN, DEPRECATED }
    enum KnowledgePayloadType { REFERENCE, RECIPE_UNLOCK, CONSTRUCTION_UNLOCK, EQUIPMENT_PERMISSION, FACTION_SERVICE, CHARACTER_PERK, DIALOGUE_GATE, PASSIVE_MODIFIER }

    record KnowledgeMetadata(KnowledgeCategory category, KnowledgeTier tier, KnowledgeVisibility visibility, KnowledgePayloadType payloadType) {}
    record CompositionAudit(List<String> errors, List<String> warnings, int definitionCount, int placedUniqueCount, int branchCount, int totalUniqueCost, int crossListedCount) {
        boolean passed() { return errors.isEmpty(); }
        List<String> reportLines() {
            ArrayList<String> out = new ArrayList<>();
            out.add("Knowledge composition audit: defs=" + definitionCount + " placed=" + placedUniqueCount + " branches=" + branchCount + " uniqueCost=" + totalUniqueCost + " crossListed=" + crossListedCount + " passed=" + passed());
            for (String e : errors) out.add("ERROR: " + e);
            for (String w : warnings) out.add("WARN: " + w);
            return out;
        }
    }

    static final String[] QUALITY_SPINE = {
            "Junk Fabrication Patterns", "Common Fabrication Patterns", "Serviceable Fabrication Patterns",
            "Fine Fabrication Patterns", "Masterwork Fabrication Patterns", "Noble Manufactury Patterns",
            "Archeotech Pattern Recognition"
    };

    private KnowledgeBranchDefinitions() {}

    static LinkedHashMap<String, KnowledgeTree> createBranches(int points, Set<String> unlocked) {
        LinkedHashSet<String> known = new LinkedHashSet<>(unlocked == null ? Set.of() : unlocked);
        LinkedHashMap<String, KnowledgeDef> defs = KnowledgeDef.all();
        LinkedHashMap<String, KnowledgeTree> result = new LinkedHashMap<>();
        addBranch(result, buildSurvival(points, known, defs));
        addBranch(result, buildFabrication(points, known, defs));
        addBranch(result, buildInfrastructure(points, known, defs));
        addBranch(result, buildIndustry(points, known, defs));
        addBranch(result, buildSecurity(points, known, defs));
        addBranch(result, buildMedicine(points, known, defs));
        addBranch(result, buildCivic(points, known, defs));
        addBranch(result, buildMechanist(points, known, defs));
        return result;
    }

    private static void addBranch(LinkedHashMap<String, KnowledgeTree> result, KnowledgeTree tree) {
        result.put(tree.id(), tree);
    }

    private static KnowledgeTree tree(String id, String title, int points, Set<String> known) {
        KnowledgeTree t = new KnowledgeTree(id, title, points);
        t.setExternalUnlockedNodeIds(known);
        return t;
    }

    private static KnowledgeTree buildSurvival(int points, Set<String> known, Map<String, KnowledgeDef> defs) {
        KnowledgeTree t = tree("survival", "Survival & Habitation", points, known);
        add(t, defs, known, "Underhive Basics", 80, 95, List.of());
        chain(t, defs, known, 270, 45, 190, 0, List.of("Underhive Basics"), "Foraged Subsistence", "Ration Sorting", "Food Preservation", "Communal Kitchen Practice", "Industrial Food Processing", "Nutrient Batch Standardization", "Noble-Military Provisioning");
        chain(t, defs, known, 270, 135, 190, 0, List.of("Underhive Basics"), "Condensation Handling", "Water Barrel Handling", "Crude Filtration", "Potable Water Discipline", "Water Recycler Operation", "Municipal Purification Systems", "Closed-Cycle Water Authority");
        chain(t, defs, known, 270, 225, 190, 0, List.of("Underhive Basics"), "Rough Bedding", "Cot Assembly", "Dormitory Layout", "Hab-Cell Furnishing", "Communal Sanitation", "Apartment Block Planning", "Civilian Habitation Authority");
        chain(t, defs, known, 270, 315, 190, 0, List.of("Underhive Basics"), "Scrap Recognition", "Useful Junk Sorting", "Salvage Carrying Discipline", "Repairable Goods Identification", "Reclaimable Components", "Trade-Grade Salvage Assessment");
        add(t, defs, known, "Scavenged Schematics", 650, 405, List.of("Scrap Recognition", "Underhive Research Methods"));
        add(t, defs, known, "Underhive Research Methods", 270, 405, List.of("Underhive Basics"));
        add(t, defs, known, "Recruit Berthing Doctrine", 460, 500, List.of("Cot Assembly"));
        add(t, defs, known, "Basic Civilian Logistics", 650, 500, List.of("Salvage Carrying Discipline", "Recruit Berthing Doctrine"));
        add(t, defs, known, "Urban Survival Doctrine", 1030, 405, List.of("Potable Water Discipline", "Repairable Goods Identification", "Food Preservation"));
        add(t, defs, known, "Settlement Self-Sufficiency", 1220, 500, List.of("Nutrient Batch Standardization", "Closed-Cycle Water Authority", "Civilian Habitation Authority", "Trade-Grade Salvage Assessment"));
        return t;
    }

    private static KnowledgeTree buildFabrication(int points, Set<String> known, Map<String, KnowledgeDef> defs) {
        KnowledgeTree t = tree("fabrication", "Fabrication Doctrine", points, known);
        chain(t, defs, known, 80, 70, 190, 0, List.of(), QUALITY_SPINE);
        add(t, defs, known, "Archeotech Production Rites", 80 + 190 * 7, 70, List.of("Archeotech Pattern Recognition", "Mechanicus Fabrication Rites"));
        int y = 175;
        for (String category : KnowledgeTreeApi.CATEGORIES) {
            addProductionCategory(t, defs, known, category, 80, y, categoryRootPrerequisites(category));
            y += 78;
        }
        return t;
    }

    private static KnowledgeTree buildInfrastructure(int points, Set<String> known, Map<String, KnowledgeDef> defs) {
        KnowledgeTree t = tree("infrastructure", "Infrastructure & Utilities", points, known);
        chain(t, defs, known, 80, 55, 200, 0, List.of(), "Civic Footpath Recognition", "Road Edge Orientation", "Plaza Layout Doctrine", "Road-to-Plaza Joining", "Traffic Channel Planning", "District Circulation Planning");
        chain(t, defs, known, 80, 145, 200, 0, List.of(), "Maintenance Access Recognition", "Utility Corridor Orientation", "Bulkhead Service Routes", "External Void Corridor Safety", "Maintenance Network Continuity", "District Service Spine Planning");
        chain(t, defs, known, 80, 235, 200, 0, List.of(), "Torch and Lamp Handling", "Local Fixture Illumination", "Wall-Respecting Light Casting", "Grid Light Discipline", "Emergency Lighting Networks", "Civic Lighting Authority");
        chain(t, defs, known, 80, 325, 200, 0, List.of(), "Waste Channel Recognition", "Sewer Corridor Navigation", "Drainage Tile Discipline", "Waste Treatment Machinery", "Water Recycler Operation", "Municipal Waste Authority");
        add(t, defs, known, "Public Works Familiarity", 680, 430, List.of("Road Edge Orientation", "Utility Corridor Orientation", "Local Fixture Illumination", "Sewer Corridor Navigation"));
        add(t, defs, known, "Zone Infrastructure Doctrine", 880, 430, List.of("Road-to-Plaza Joining", "Bulkhead Service Routes", "Wall-Respecting Light Casting", "Waste Treatment Machinery"));
        add(t, defs, known, "District Utility Authority", 1080, 430, List.of("District Circulation Planning", "Maintenance Network Continuity", "Civic Lighting Authority", "Municipal Waste Authority"));
        return t;
    }

    private static KnowledgeTree buildIndustry(int points, Set<String> known, Map<String, KnowledgeDef> defs) {
        KnowledgeTree t = tree("industry", "Industry & Production", points, known);
        chain(t, defs, known, 80, 55, 190, 0, List.of(), "Hand Assembly", "Bench Work", "Tool-Aided Crafting", "Workshop Procedure", "Production Bench Authority");
        chain(t, defs, known, 80, 145, 190, 0, List.of(), "Crude Machine Handling", "Basic Machine Operation", "Maintenance Cycle Awareness", "Machine Safety Discipline", "Industrial Apparatus Operation", "Automated Production Cells");
        chain(t, defs, known, 80, 235, 190, 0, List.of(), "Scrap Material Recognition", "Wood-Cloth-Metal Sorting", "Component Recovery", "Industrial Component Handling", "Refined Material Processing", "Specialist Material Chains");
        chain(t, defs, known, 80, 325, 190, 0, List.of(), "Container Discipline", "Stockpile Recognition", "Supply Shelf Practice", "Room Container Standards", "Warehouse Routing", "District Logistics Authority");
        add(t, defs, known, "Scrap-Forging Doctrine", 460, 430, List.of("Bench Work", "Scrap Material Recognition", "Junk Fabrication Patterns"));
        add(t, defs, known, "Workshop Labor Discipline", 650, 430, List.of("Workshop Procedure", "Basic Machine Operation"));
        add(t, defs, known, "Base Logistics Discipline", 840, 430, List.of("Room Container Standards", "Basic Civilian Logistics"));
        add(t, defs, known, "Workshop Doctrine", 650, 520, List.of("Tool-Aided Crafting", "Maintenance Cycle Awareness", "Component Recovery"));
        add(t, defs, known, "Industrial Workflow Doctrine", 840, 520, List.of("Workshop Doctrine", "Industrial Apparatus Operation", "Warehouse Routing"));
        add(t, defs, known, "Production Line Authority", 1030, 520, List.of("Industrial Workflow Doctrine", "Automated Production Cells", "District Logistics Authority"));
        return t;
    }

    private static KnowledgeTree buildSecurity(int points, Set<String> known, Map<String, KnowledgeDef> defs) {
        KnowledgeTree t = tree("security", "Security & Fieldcraft", points, known);
        chain(t, defs, known, 80, 55, 190, 0, List.of(), "Improvised Blades", "Scrap Knife Handling", "Club and Tool Fighting", "Trench Melee", "Shock Assault Doctrine");
        chain(t, defs, known, 80, 145, 190, 0, List.of(), "Stub Weapon Familiarity", "Ballistic Maintenance", "Ammunition Recognition", "Rifle Discipline", "Heavy Ballistic Weapon Handling");
        chain(t, defs, known, 80, 235, 190, 0, List.of(), "Power Cell Recognition", "Las Weapon Familiarity", "Energy Weapon Maintenance", "Overheat Discipline", "High-Energy Weapon Doctrine");
        chain(t, defs, known, 80, 325, 190, 0, List.of(), "Scavenger Clothing Familiarity", "Padded Protection", "Flak Layering", "Arbites Armor Recognition", "PDF Armor Familiarity", "Heavy Protective Doctrine");
        chain(t, defs, known, 80, 415, 190, 0, List.of(), "Trip Hazard Recognition", "Simple Trap Disarming", "Door Security Awareness", "Alarm Fixture Recognition", "Defensive Room Planning", "Security Network Authority");
        add(t, defs, known, "Security Cogitator Rites", 840, 505, List.of("Alarm Fixture Recognition", "Cogitator Interface Familiarity"));
        return t;
    }

    private static KnowledgeTree buildMedicine(int points, Set<String> known, Map<String, KnowledgeDef> defs) {
        KnowledgeTree t = tree("medicine", "Medicine & Biology", points, known);
        chain(t, defs, known, 80, 65, 195, 0, List.of(), "Wound Recognition", "Basic Bandaging", "Pain and Shock Management", "Field Treatment", "Clinic Procedure", "Emergency Medical Authority");
        chain(t, defs, known, 80, 165, 195, 0, List.of(), "Filth Recognition", "Corpse Handling Discipline", "Waste Isolation", "Disease Vector Awareness", "Sanitation Room Procedure", "Public Health Authority");
        chain(t, defs, known, 80, 265, 195, 0, List.of(), "Herbal-Scavenged Remedies", "Crude Medicine Preparation", "Standard Medicine Patterns", "Sterile Handling", "Advanced Pharmaceutical Practice");
        add(t, defs, known, "Field Medicae Practices", 470, 375, List.of("Basic Bandaging", "Commercial Ledgers"));
        add(t, defs, known, "Public Health Authority", 1055, 165, List.of("Sanitation Room Procedure", "Municipal Waste Authority"));
        return t;
    }

    private static KnowledgeTree buildCivic(int points, Set<String> known, Map<String, KnowledgeDef> defs) {
        KnowledgeTree t = tree("civic", "Civic, Faction & Commerce", points, known);
        chain(t, defs, known, 80, 55, 190, 0, List.of(), "Barter Familiarity", "Price Recognition", "Trade Ledger Reading", "Vendor Trust", "Contract Negotiation", "Licensed Commerce");
        add(t, defs, known, "Commercial Ledgers", 270, 115, List.of("Price Recognition"));
        add(t, defs, known, "Commerce Permits", 650, 115, List.of("Trade Ledger Reading", "Administratum Forms"));
        chain(t, defs, known, 80, 175, 190, 0, List.of(), "Civic Signage Recognition", "Hab Block Rules", "Administratum Forms", "Permit Filing", "Civic Room Authority", "District Governance Access");
        chain(t, defs, known, 80, 275, 190, 0, List.of(), "Faction Color Recognition", "Local Reputation Awareness", "Civilian Authority Customs", "Arbites Protocol", "Mechanicus Protocol", "Noble House Protocol");
        chain(t, defs, known, 80, 375, 190, 0, List.of(), "Trespass Awareness", "Door Permission Recognition", "Container Ownership Recognition", "Restricted Area Awareness", "Permit-Based Access", "Faction Access Authority");
        add(t, defs, known, "Civilian Provisioning Patterns", 650, 475, List.of("Licensed Commerce", "Civil Operating Doctrine"));
        add(t, defs, known, "Military Logistics Patterns", 840, 475, List.of("Arbites Protocol", "Contract Negotiation"));
        add(t, defs, known, "Civil Operating Doctrine", 650, 565, List.of("Permit Filing", "Civilian Authority Customs", "Container Ownership Recognition"));
        add(t, defs, known, "Faction Service Access", 840, 565, List.of("Civil Operating Doctrine", "Vendor Trust", "Local Reputation Awareness"));
        add(t, defs, known, "District Permit Authority", 1030, 565, List.of("District Governance Access", "Faction Access Authority", "Licensed Commerce"));
        add(t, defs, known, "Profile Special Unlocks", 1220, 565, List.of("Faction Service Access"));
        return t;
    }

    private static KnowledgeTree buildMechanist(int points, Set<String> known, Map<String, KnowledgeDef> defs) {
        KnowledgeTree t = tree("mechanist", "Mechanist / Archeotech", points, known);
        chain(t, defs, known, 80, 65, 200, 0, List.of(), "Machine Respect", "Maintenance Litany", "Machine Appeasement", "Sanctified Repair Practice", "Machine Spirit Interpretation");
        chain(t, defs, known, 80, 165, 200, 0, List.of(), "Terminal Recognition", "Data Slate Reading", "Cogitator Interface Familiarity", "Archive Query Discipline", "Machine Logic Interpretation", "Restricted Data Handling");
        chain(t, defs, known, 80, 265, 200, 0, List.of(), "Ancient Component Recognition", "Forbidden Pattern Caution", "Archeotech Signal Recognition", "Fragmentary STC Interpretation", "Dangerous Device Procedure");
        add(t, defs, known, "Scrap-Forging Doctrine", 480, 370, List.of("Machine Respect", "Hand Assembly", "Junk Fabrication Patterns"));
        add(t, defs, known, "Mechanist Initiate Doctrine", 680, 370, List.of("Maintenance Litany", "Data Slate Reading", "Scrap-Forging Doctrine"));
        add(t, defs, known, "Mechanicus Fabrication Rites", 880, 370, List.of("Mechanist Initiate Doctrine", "Serviceable Fabrication Patterns", "Mechanicus Protocol"));
        add(t, defs, known, "Sacred Systems Familiarity", 1080, 370, List.of("Machine Spirit Interpretation", "Machine Logic Interpretation", "Mechanicus Fabrication Rites"));
        add(t, defs, known, "Archeotech Pattern Recognition", 1080, 470, List.of("Archeotech Signal Recognition", "Restricted Data Handling", "Fine Fabrication Patterns"));
        add(t, defs, known, "Archeotech Production Rites", 1280, 470, List.of("Archeotech Pattern Recognition", "Sacred Systems Familiarity", "Noble Manufactury Patterns"));
        return t;
    }

    private static List<String> categoryRootPrerequisites(String category) {
        String c = category == null ? "" : category.toLowerCase(Locale.ROOT);
        ArrayList<String> p = new ArrayList<>();
        p.add("Junk Fabrication Patterns");
        if (c.contains("food") || c.contains("agricultural")) p.add("Ration Sorting");
        else if (c.contains("water")) p.add("Potable Water Discipline");
        else if (c.contains("medical")) p.add("Basic Bandaging");
        else if (c.contains("chemical")) p.add("Crude Medicine Preparation");
        else if (c.contains("ballistics")) p.add("Stub Weapon Familiarity");
        else if (c.contains("energy")) p.add("Power Cell Recognition");
        else if (c.contains("melee")) p.add("Scrap Knife Handling");
        else if (c.contains("armor")) p.add("Padded Protection");
        else if (c.contains("textile")) p.add("Cot Assembly");
        else if (c.contains("metallurgy") || c.contains("construction")) p.add("Scrap Material Recognition");
        else if (c.contains("industrial") || c.contains("machinery")) p.add("Basic Machine Operation");
        else if (c.contains("salvage") || c.contains("tools")) p.add("Hand Assembly");
        return p;
    }

    private static void addProductionCategory(KnowledgeTree tree, Map<String, KnowledgeDef> defs, Set<String> known, String category, int startX, int y, List<String> rootPrereqs) {
        String previous = null;
        int i = 0;
        for (String band : QualityAuthorityApi.KNOWLEDGE_BANDS) {
            String nodeName = band + " " + category + " Patterns";
            ArrayList<String> prereq = new ArrayList<>();
            if (previous == null) prereq.addAll(rootPrereqs == null ? List.of() : rootPrereqs);
            else prereq.add(previous);
            String spine = qualitySpineForBand(band);
            if (spine != null && !spine.equals(nodeName) && !prereq.contains(spine)) prereq.add(spine);
            add(tree, defs, known, nodeName, startX + i * 190, y, prereq);
            previous = nodeName;
            i++;
        }
    }

    private static String qualitySpineForBand(String band) {
        if (band == null) return null;
        return switch (band) {
            case "Junk" -> "Junk Fabrication Patterns";
            case "Common" -> "Common Fabrication Patterns";
            case "Serviceable" -> "Serviceable Fabrication Patterns";
            case "Fine" -> "Fine Fabrication Patterns";
            case "Masterwork" -> "Masterwork Fabrication Patterns";
            case "Noble" -> "Noble Manufactury Patterns";
            case "Archeotech" -> "Archeotech Pattern Recognition";
            default -> null;
        };
    }

    private static void chain(KnowledgeTree tree, Map<String, KnowledgeDef> defs, Set<String> known, int x, int y, int dx, int dy, List<String> rootPrereqs, String... names) {
        String previous = null;
        for (int i = 0; i < names.length; i++) {
            ArrayList<String> p = new ArrayList<>();
            if (previous == null) p.addAll(rootPrereqs == null ? List.of() : rootPrereqs);
            else p.add(previous);
            add(tree, defs, known, names[i], x + i * dx, y + i * dy, p);
            previous = names[i];
        }
    }

    private static void add(KnowledgeTree tree, Map<String, KnowledgeDef> defs, Set<String> known, String name, double x, double y, List<String> prerequisites) {
        KnowledgeDef def = defs.get(name);
        if (def == null || tree.containsNode(name)) return;
        String shortText = def.family + " — " + def.unlocks;
        String prereqText = prerequisites == null || prerequisites.isEmpty() ? "None" : String.join(", ", prerequisites);
        String longText = def.name + "\n\nFamily: " + def.family + "\nCost: " + def.cost + "\nPrerequisites: " + prereqText + "\nSources: " + def.source + "\n\nUnlocks: " + def.unlocks + "\n\nThis is a character-owned doctrine node. Later passes can attach recipe grants, construction permissions, perks, stat modifiers, services, and dialogue gates here without routing purchase state through the Infopedia.";
        tree.addNode(new KnowledgeNode(def.name, def.name, shortText, longText, def.cost, known.contains(def.name), prerequisites, x, y));
    }

    static CompositionAudit audit() {
        LinkedHashMap<String, KnowledgeDef> defs = KnowledgeDef.all();
        LinkedHashMap<String, KnowledgeTree> branches = createBranches(0, Set.of());
        LinkedHashMap<String, ArrayList<String>> placements = placements(branches);
        ArrayList<String> errors = new ArrayList<>();
        ArrayList<String> warnings = new ArrayList<>();
        for (String id : defs.keySet()) {
            if (!placements.containsKey(id)) errors.add("Registered active knowledge is not placed in any active branch: " + id);
        }
        LinkedHashSet<String> allPlaced = new LinkedHashSet<>(placements.keySet());
        for (KnowledgeTree tree : branches.values()) {
            for (KnowledgeNode node : tree.nodes()) {
                if (!defs.containsKey(node.id())) errors.add("Tree " + tree.displayName() + " contains unknown node id: " + node.id());
                for (String pre : node.prerequisiteNodeIds()) {
                    if (!defs.containsKey(pre)) errors.add("Node " + node.id() + " has missing prerequisite definition: " + pre);
                    if (!allPlaced.contains(pre)) errors.add("Node " + node.id() + " requires unplaced prerequisite: " + pre);
                }
            }
            if (tree.nodes().isEmpty()) errors.add("Knowledge branch is empty: " + tree.displayName());
            detectBranchCycles(tree, errors);
        }
        int crossListed = 0;
        for (Map.Entry<String, ArrayList<String>> e : placements.entrySet()) if (e.getValue().size() > 1) crossListed++;
        int totalUniqueCost = 0;
        for (String id : placements.keySet()) {
            KnowledgeDef def = defs.get(id);
            if (def != null) totalUniqueCost += Math.max(0, def.cost);
        }
        if (crossListed > 0) warnings.add("Cross-listed knowledge nodes share unlock state across branches: " + crossListed + ".");
        warnings.add("Total unique visible unlock cost is " + totalUniqueCost + " knowledge credits; full completion is intended as long-campaign optional breadth, not an early-game target.");
        return new CompositionAudit(Collections.unmodifiableList(errors), Collections.unmodifiableList(warnings), defs.size(), placements.size(), branches.size(), totalUniqueCost, crossListed);
    }

    static List<String> auditLines() { return audit().reportLines(); }

    private static LinkedHashMap<String, ArrayList<String>> placements(LinkedHashMap<String, KnowledgeTree> branches) {
        LinkedHashMap<String, ArrayList<String>> placements = new LinkedHashMap<>();
        for (KnowledgeTree tree : branches.values()) {
            for (KnowledgeNode node : tree.nodes()) placements.computeIfAbsent(node.id(), k -> new ArrayList<>()).add(tree.id());
        }
        return placements;
    }

    private static void detectBranchCycles(KnowledgeTree tree, ArrayList<String> errors) {
        Map<String, KnowledgeNode> byId = tree.nodeMap();
        LinkedHashSet<String> visiting = new LinkedHashSet<>();
        LinkedHashSet<String> visited = new LinkedHashSet<>();
        for (String id : byId.keySet()) detectBranchCycles(id, byId, visiting, visited, errors);
    }

    private static void detectBranchCycles(String id, Map<String, KnowledgeNode> byId, LinkedHashSet<String> visiting, LinkedHashSet<String> visited, ArrayList<String> errors) {
        if (visited.contains(id)) return;
        if (!visiting.add(id)) { errors.add("Cycle detected in knowledge branch at " + id + "."); return; }
        KnowledgeNode node = byId.get(id);
        if (node != null) {
            for (String pre : node.prerequisiteNodeIds()) if (byId.containsKey(pre)) detectBranchCycles(pre, byId, visiting, visited, errors);
        }
        visiting.remove(id);
        visited.add(id);
    }

    static Map<KnowledgeCategory, Integer> categoryCostSummary() {
        LinkedHashMap<KnowledgeCategory, Integer> out = new LinkedHashMap<>();
        for (KnowledgeCategory c : KnowledgeCategory.values()) out.put(c, 0);
        for (KnowledgeDef def : KnowledgeDef.all().values()) {
            KnowledgeMetadata md = metadataFor(def);
            out.put(md.category(), out.get(md.category()) + Math.max(0, def.cost));
        }
        return out;
    }

    static KnowledgeMetadata metadataFor(KnowledgeDef def) {
        String f = def == null || def.family == null ? "" : def.family.toLowerCase(Locale.ROOT);
        KnowledgeCategory category = KnowledgeCategory.SURVIVAL;
        if (f.contains("quality") || f.contains("knowledge-tree")) category = KnowledgeCategory.FABRICATION;
        else if (f.contains("infrastructure")) category = KnowledgeCategory.INFRASTRUCTURE;
        else if (f.contains("industry")) category = KnowledgeCategory.INDUSTRY;
        else if (f.contains("security")) category = KnowledgeCategory.SECURITY;
        else if (f.contains("medical")) category = KnowledgeCategory.MEDICAL;
        else if (f.contains("civic") || f.contains("commerce") || f.contains("faction") || f.contains("character/profile")) category = KnowledgeCategory.CIVIC;
        else if (f.contains("mechanist")) category = KnowledgeCategory.MECHANIST;
        int cost = def == null ? 0 : def.cost;
        KnowledgeTier tier = cost <= 1 ? KnowledgeTier.FOUNDATION : cost <= 3 ? KnowledgeTier.PRACTICAL : cost <= 6 ? KnowledgeTier.SKILLED : cost <= 10 ? KnowledgeTier.SPECIALIST : cost <= 12 ? KnowledgeTier.AUTHORITY : KnowledgeTier.FORBIDDEN;
        KnowledgePayloadType payload = f.contains("production") || f.contains("knowledge-tree") ? KnowledgePayloadType.RECIPE_UNLOCK : f.contains("faction") || f.contains("commerce") || f.contains("civic") ? KnowledgePayloadType.FACTION_SERVICE : f.contains("security") ? KnowledgePayloadType.EQUIPMENT_PERMISSION : KnowledgePayloadType.REFERENCE;
        return new KnowledgeMetadata(category, tier, KnowledgeVisibility.ACTIVE, payload);
    }

    static List<KnowledgeTree> developerPreviewBranches() {
        return createBranches(8, new LinkedHashSet<>(Set.of("Underhive Basics"))).values().stream()
                .sorted(Comparator.comparing(KnowledgeTree::displayName)).toList();
    }
}
