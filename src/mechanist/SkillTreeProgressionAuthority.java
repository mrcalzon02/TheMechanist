package mechanist;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Defines the first player-facing durable skill-tree branch and node contract. */
final class SkillTreeProgressionAuthority {
    record SkillBranch(String id, String name, String worldUse, List<SkillNode> nodes) { }
    record SkillNode(String id, String branchId, String name, int xpCost, String prerequisite,
                     String capability, String visibleEffect, String knowledgeBoundary, String accessRequirement,
                     String statRequirement, String statEffect, String exclusiveGroup,
                     String capabilityKey, String passiveBonus, String activeAbility) { }
    record SkillAccessContext(Set<String> knowledges, Map<Faction, Integer> factionStanding,
                              Set<String> facilities, Set<String> trainers, Set<String> equipment,
                              boolean ignoreAccess) {
        static SkillAccessContext open() {
            return new SkillAccessContext(Set.of(), Map.of(), Set.of(), Set.of(), Set.of(), true);
        }

        static SkillAccessContext fromGame(GamePanel game) {
            if (game == null) return open();
            LinkedHashSet<String> knowledges = new LinkedHashSet<>(game.unlockedKnowledges);
            EnumMap<Faction, Integer> standings = new EnumMap<>(Faction.class);
            standings.putAll(game.factionStanding);
            LinkedHashSet<String> facilities = new LinkedHashSet<>();
            if (game.baseObjects != null) {
                for (BaseObject object : game.baseObjects) {
                    if (object == null || object.underConstruction || object.integrity <= 0) continue;
                    addToken(facilities, object.name);
                    addToken(facilities, object.businessName());
                    addToken(facilities, "symbol-" + object.symbol);
                    if (object.symbol == 'f') addToken(facilities, "forge-fabrication-stall");
                    if (object.symbol == 'l') addToken(facilities, "research-doctrine-desk");
                    if (object.symbol == 'M') addToken(facilities, "backroom-medicae-service");
                    if (object.symbol == 'w') addToken(facilities, "repair-scrap-work-stall");
                }
            }
            LinkedHashSet<String> equipment = new LinkedHashSet<>();
            if (game.inventory != null) for (String item : game.inventory) addToken(equipment, item);
            if (game.baseStorage != null) for (String item : game.baseStorage) addToken(equipment, item);
            if (game.equippedLeftHandItem != null) addToken(equipment, game.equippedLeftHandItem);
            if (game.equippedRightHandItem != null) addToken(equipment, game.equippedRightHandItem);
            if (game.equippedClothing != null) addToken(equipment, game.equippedClothing.name);
            if (game.equippedWearableSlots != null) {
                for (String item : game.equippedWearableSlots.values()) addToken(equipment, item);
            }
            LinkedHashSet<String> trainers = new LinkedHashSet<>(game.activeSkillTrainers);
            return new SkillAccessContext(knowledges, standings, facilities, trainers, equipment, false);
        }
    }
    record SpendResult(boolean success, String message, int remainingXp, String unlockedNodeId, String statEffect) { }

    private SkillTreeProgressionAuthority() { }

    static List<SkillBranch> branches() {
        return List.of(
                branch("fabrication-repair", "Fabrication and Repair",
                        "Build, repair, inspect, and improve production outcomes with visible machine and item consequences.",
                        node("fabrication-repair", "fab-repair-field-tech", "Field Repair Discipline", 25, "none",
                                "Can perform safer field repairs on owned production machines.",
                                "Repair previews and production provenance can identify repaired-machine history.",
                                "This is skill practice; machine doctrine remains a separate knowledge unlock."),
                        hookNode("fabrication-repair", "fab-repair-material-eye", "Material Eye", 40, "Field Repair Discipline",
                                "Can identify which material or tool cap limited a produced item.",
                                "Production inspection explains quality limiters before blaming the wrong system.",
                                "This does not teach recipe knowledge; it improves interpretation of known production.",
                                "production-limiter-readability:+1", "none"),
                        gatedNode("fabrication-repair", "fab-repair-master-workshop", "Master Workshop Practice", 60, "Material Eye",
                                "Can attempt advanced fabrication appraisal when a real workshop context exists.",
                                "Workshop-bound repair and production previews can expose advanced blockers instead of generic refusal.",
                                "This requires world access; knowledge still remains separate from practiced fabrication.",
                                "facility:forge-fabrication-stall",
                                "Mechanics:8", "Mechanics:+1"),
                        gatedNode("fabrication-repair", "fab-repair-forge-tutoring", "Forge-Tutored Repair", 45, "Field Repair Discipline",
                                "Can restore a broken owned production machine to serviceable integrity with one machine part.",
                                "Field-repair previews show the stronger trained restoration before the part and turn are spent.",
                                "This is practiced repair technique taught in person; it does not grant machine doctrine.",
                                "trainer:forge-tutor")),
                branch("machine-operation", "Machine Operation",
                        "Operate staffed or manual machinery with lower risk, clearer blockers, and better handoff discipline.",
                        hookNode("machine-operation", "machine-safe-start", "Safe Start Ritual", 30, "none",
                                "Can read machine readiness before committing inputs.",
                                "Machine operation previews surface blockers and defect pressure clearly.",
                                "Knowing a doctrine can unlock a recipe; this skill governs operation quality.",
                                "none", "machine-readiness-preview"),
                        statNode("machine-operation", "machine-pressure-discipline", "Pressure Discipline", 45, "Safe Start Ritual",
                                "Can keep machine operation readable under fatigue, alarm, or queued-production pressure.",
                                "Machine readiness text can distinguish nervous misoperation risk from missing materials.",
                                "Knowledge tells the operator what the machine is; nerve training governs keeping the ritual steady.",
                                "Nerve:7", "Nerve:+1")),
                branch("trade-appraisal", "Trade and Appraisal",
                        "Evaluate goods, bad batches, counterfeit risk, and ordinary resale consequences.",
                        node("trade-appraisal", "trade-batch-appraisal", "Batch Appraisal", 35, "none",
                                "Can identify defect, counterfeit, contamination, and restricted-batch hints.",
                                "Trade and inspection surfaces can explain why a batch is worth less or risky.",
                                "Knowledge may name the item; skill judges the item in front of you."),
                        gatedNode("trade-appraisal", "trade-guilder-certification", "Certified Market Appraisal", 55, "Batch Appraisal",
                                "Can treat faction-facing batch certificates as usable trade evidence.",
                                "Market and contract text can distinguish certified appraisal from ordinary item naming.",
                                "Knowledge may name the item; faction access confirms the practiced appraisal lane.",
                                "faction:MECHANIST_COLLEGIA:20",
                                "none", "none", "trade-appraisal-specialization"),
                        specializedNode("trade-appraisal", "trade-streetwise-appraisal", "Streetwise Appraisal", 55, "Batch Appraisal",
                                "Can judge stolen-risk, counterfeit, and ordinary fence consequences without a formal certificate.",
                                "Street-market trade text can emphasize practical risk and buyer suspicion instead of institutional proof.",
                                "Knowledge may name the item; street practice judges who will buy it and what trouble follows.",
                                "trade-appraisal-specialization",
                                "none", "street-market-risk-appraisal")),
                branch("investigation-examination", "Investigation and Examination",
                        "Read rooms, evidence, provenance, and production traces without exposing raw IDs.",
                        node("investigation-examination", "investigation-trace-reading", "Trace Reading", 30, "none",
                                "Can connect an item to who made it, where, and under what conditions.",
                                "Inspection summaries emphasize provenance lines that matter for decisions.",
                                "Knowledge identifies facts; this skill improves finding and applying evidence.")),
                branch("leadership-command", "Leadership and Faction Command",
                        "Coordinate assigned workers, supervisors, queues, and faction-facing production proof.",
                        node("leadership-command", "leadership-shift-discipline", "Shift Discipline", 45, "none",
                                "Can evaluate whether staffed production has worker, room, machine, and input ownership.",
                                "Staffing previews can explain readiness without running a hidden background simulator.",
                                "Faction doctrine and worker skill remain separate inputs."),
                        gatedNode("leadership-command", "leadership-supervisor-charter", "Supervisor Charter", 65, "Shift Discipline",
                                "Can present assigned-worker production proof as a faction-ready leadership record.",
                                "Staffed production summaries can explain supervisor authorization separately from worker skill.",
                                "Faction doctrine and worker skill remain separate; this node requires an access charter.",
                                "knowledge:Underhive Basics")));
    }

    static List<SkillNode> allNodes() {
        ArrayList<SkillNode> out = new ArrayList<>();
        for (SkillBranch branch : branches()) out.addAll(branch.nodes());
        out.sort(Comparator.comparing(SkillNode::branchId).thenComparing(SkillNode::xpCost).thenComparing(SkillNode::name));
        return out;
    }

    static List<String> summaryLines() {
        ArrayList<String> lines = new ArrayList<>();
        lines.add("Skill progression: XP buys durable capabilities; knowledge unlocks facts, recipes, doctrines, or recognition.");
        lines.add("Skill-node spending, save persistence, and access-gate validation exist in the Character Skills tab, which browses branches and nodes, previews requirements and visible effects, and spends XP through the same validated path as the console command.");
        for (SkillBranch branch : branches()) {
            lines.add("Branch: " + branch.name() + " - " + branch.worldUse());
            for (SkillNode node : branch.nodes()) {
                lines.add("Node: " + node.name() + " / cost " + node.xpCost() + " XP / prerequisite "
                        + node.prerequisite() + " / access " + node.accessRequirement()
                        + " / stat " + node.statRequirement() + " / effect " + node.statEffect()
                        + " / exclusive " + node.exclusiveGroup()
                        + " / capabilityKey " + node.capabilityKey()
                        + " / passive " + node.passiveBonus() + " / active " + node.activeAbility()
                        + " / capability " + node.capability());
                lines.add("Effect: " + node.visibleEffect());
                lines.add("Boundary: " + node.knowledgeBoundary());
            }
        }
        return lines;
    }

    static List<String> infopediaLines() {
        ArrayList<String> lines = new ArrayList<>();
        lines.add("Skill Tree: spend XP on durable capabilities, not recipe facts.");
        lines.add("Knowledge Tree: unlocks recipes, doctrines, recognition, and explanations; it does not by itself make the character practiced.");
        lines.add("Unlocked skill nodes persist separately from unlocked knowledge, and spending XP on a skill never grants recipe doctrine.");
        lines.add("Skill nodes must expose capabilities such as safer field repair, batch appraisal, trace reading, machine operation, or leadership handoff.");
        lines.add("Every initial node names an XP cost, prerequisite, visible effect, and boundary from knowledge.");
        lines.add("Advanced skill nodes may require faction standing, trainer proof, equipment, facilities, or unlocked knowledge before XP can be spent.");
        lines.add("Qualifying specialist conversations can offer Train, opening the Skills tab with temporary in-person trainer access for matching nodes.");
        lines.add("Some skill nodes may require a character stat threshold and may grant a bounded stat increase when unlocked.");
        lines.add("Some specialization nodes are mutually exclusive; unlocking one node in a group blocks the sibling specialization.");
        lines.add("Unlocked nodes expose capability keys, passive bonuses, and active abilities for later gameplay systems to query directly.");
        lines.add("Use the Character Skills tab to inspect branches, XP, requirements, and visible effects, then Unlock an available node. The skill_status and skill_unlock <node id> console routes use the same rules.");
        lines.add("The skill-tree definition audit names branch and node coverage, XP costs, dependencies, access gates, stat modifiers, exclusive groups, and capability hooks while keeping ordinary UI free of raw node IDs.");
        lines.add("Guard: Milestone03SkillTreeProgressionReadabilitySmoke checks branch count, node capabilities, XP costs, prerequisites, and skill-versus-knowledge wording; Milestone03SkillTreeSpendingPersistenceSmoke checks spending and save persistence; Milestone03SkillTreeAccessGateSmoke checks world access requirements; Milestone03SkillTreeStatGateSmoke checks stat gates and effects; Milestone03SkillTreeMutualExclusionSmoke checks exclusive specialization groups; Milestone03SkillTreeCapabilityHooksSmoke checks capability/passive/active hooks.");
        lines.add("Guard: Milestone03SkillTreeDefinitionAuditSmoke checks definition-audit branch/node coverage, XP costs, dependencies, access gates, stat modifiers, exclusive groups, capability hooks, knowledge separation, and raw-ID hiding.");
        return lines;
    }

    static String auditLine() {
        return "skillBranches=" + branches().size() + " skillNodes=" + allNodes().size()
                + " mutation=skill-node-save+stat-effect spendingUi=character-skills-tab+console-route accessGates=knowledge+faction+trainer+facility+equipment statGates=true exclusiveGroups=true capabilityHooks=true knowledgeDistinct=true";
    }

    static List<String> definitionAuditLines() {
        ArrayList<String> lines = new ArrayList<>();
        List<SkillBranch> branches = branches();
        List<SkillNode> nodes = allNodes();
        lines.add("Skill-tree definition audit: owner=SkillTreeProgressionAuthority, branches=" + branches.size()
                + ", nodes=" + nodes.size() + ", xpCosts=" + allNodesHaveXp(nodes)
                + ", dependencies=" + hasPrerequisites(nodes) + ", accessGates=" + hasAccessGates(nodes)
                + ", statModifiers=" + hasStatModifiers(nodes) + ", exclusiveGroups=" + exclusiveGroupCount(nodes)
                + ", capabilityHooks=" + hasCapabilityHooks(nodes) + ", knowledgeDistinct=true, ordinaryUiRawIds=false.");
        for (SkillBranch branch : branches) {
            lines.add("Skill branch audit: " + branch.name() + " nodes=" + branch.nodes().size()
                    + " worldUse=" + branch.worldUse());
            for (SkillNode node : branch.nodes()) {
                lines.add("Skill node audit: " + node.name()
                        + " branch=" + branch.name()
                        + " xpCost=" + node.xpCost()
                        + " prerequisite=" + friendlyNone(node.prerequisite())
                        + " access=" + friendlyAccessRequirement(node.accessRequirement())
                        + " statRequirement=" + friendlyNone(node.statRequirement())
                        + " statEffect=" + friendlyNone(node.statEffect())
                        + " exclusive=" + friendlyExclusiveGroup(node.exclusiveGroup())
                        + " capabilityHook=" + friendlyCapabilityHook(node)
                        + " boundary=" + node.knowledgeBoundary());
            }
        }
        lines.add("Guard: Milestone03SkillTreeDefinitionAuditSmoke checks branch/node coverage, XP costs, dependencies, access gates, stat modifiers, exclusive groups, capability hooks, knowledge separation, and raw-ID hiding.");
        return lines;
    }

    static SkillNode nodeByIdOrName(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String wanted = normalize(raw);
        for (SkillNode node : allNodes()) {
            if (normalize(node.id()).equals(wanted) || normalize(node.name()).equals(wanted)) return node;
        }
        return null;
    }

    static SpendResult spendXp(Set<String> unlockedNodes, int availableXp, String rawNode) {
        return spendXp(unlockedNodes, availableXp, rawNode, SkillAccessContext.open());
    }

    static SpendResult spendXp(Set<String> unlockedNodes, int availableXp, String rawNode, SkillAccessContext context) {
        return spendXp(unlockedNodes, availableXp, rawNode, context, Map.of());
    }

    static SpendResult spendXp(Set<String> unlockedNodes, int availableXp, String rawNode, SkillAccessContext context,
                               Map<String, Integer> stats) {
        SkillNode node = nodeByIdOrName(rawNode);
        if (node == null) return new SpendResult(false, "Unknown skill node: " + (rawNode == null ? "" : rawNode) + ".", Math.max(0, availableXp), "", "");
        Set<String> unlocked = unlockedNodes == null ? Set.of() : unlockedNodes;
        if (unlocked.contains(node.id())) return new SpendResult(false, "Skill node already unlocked: " + node.name() + ".", Math.max(0, availableXp), node.id(), "");
        String missing = missingPrerequisite(node, unlocked);
        if (!missing.isBlank()) return new SpendResult(false, "Skill prerequisite missing: " + missing + " before " + node.name() + ".", Math.max(0, availableXp), node.id(), "");
        missing = missingExclusiveGroup(node, unlocked);
        if (!missing.isBlank()) return new SpendResult(false, "Skill specialization blocked: " + missing + " before " + node.name() + ".", Math.max(0, availableXp), node.id(), "");
        missing = missingAccess(node, context == null ? SkillAccessContext.open() : context);
        if (!missing.isBlank()) return new SpendResult(false, "Skill access missing: " + missing + " before " + node.name() + ".", Math.max(0, availableXp), node.id(), "");
        missing = missingStat(node, stats);
        if (!missing.isBlank()) return new SpendResult(false, "Skill stat missing: " + missing + " before " + node.name() + ".", Math.max(0, availableXp), node.id(), "");
        if (availableXp < node.xpCost()) return new SpendResult(false, "Need " + node.xpCost() + " XP for " + node.name() + "; available " + Math.max(0, availableXp) + ".", Math.max(0, availableXp), node.id(), "");
        return new SpendResult(true, "Unlocked skill node: " + node.name() + " for " + node.xpCost() + " XP.", availableXp - node.xpCost(), node.id(), node.statEffect());
    }

    static List<String> statusLines(int xp, Set<String> unlockedNodes) {
        return statusLines(xp, unlockedNodes, SkillAccessContext.open());
    }

    static List<String> statusLines(int xp, Set<String> unlockedNodes, SkillAccessContext context) {
        return statusLines(xp, unlockedNodes, context, Map.of());
    }

    static List<String> statusLines(int xp, Set<String> unlockedNodes, SkillAccessContext context, Map<String, Integer> stats) {
        LinkedHashSet<String> unlocked = new LinkedHashSet<>(unlockedNodes == null ? Set.of() : unlockedNodes);
        SkillAccessContext access = context == null ? SkillAccessContext.open() : context;
        ArrayList<String> lines = new ArrayList<>();
        lines.add("Skill XP: " + Math.max(0, xp) + ".");
        lines.add("Unlocked skill nodes: " + (unlocked.isEmpty() ? "none" : String.join(", ", unlocked)) + ".");
        for (SkillNode node : allNodes()) {
            String state = unlocked.contains(node.id()) ? "unlocked" : canUnlock(node, unlocked, xp, access, stats) ? "available" : "locked";
            lines.add(node.id() + " | " + state + " | cost " + node.xpCost() + " XP | prerequisite " + node.prerequisite()
                    + " | access " + node.accessRequirement() + " | stat " + node.statRequirement()
                    + " | effect " + node.statEffect() + " | exclusive " + node.exclusiveGroup()
                    + " | key " + node.capabilityKey() + " | passive " + node.passiveBonus()
                    + " | active " + node.activeAbility()
                    + " | " + node.capability());
        }
        return lines;
    }

    static boolean hasCapability(Set<String> unlockedNodes, String capabilityKey) {
        return capabilityKeys(unlockedNodes).contains(capabilityKey == null ? "" : capabilityKey.trim());
    }

    static LinkedHashSet<String> capabilityKeys(Set<String> unlockedNodes) {
        LinkedHashSet<String> out = new LinkedHashSet<>();
        if (unlockedNodes == null || unlockedNodes.isEmpty()) return out;
        for (SkillNode node : allNodes()) {
            if (unlockedNodes.contains(node.id()) && node.capabilityKey() != null && !node.capabilityKey().isBlank()) {
                out.add(node.capabilityKey());
            }
        }
        return out;
    }

    static List<String> passiveBonusLines(Set<String> unlockedNodes) {
        ArrayList<String> out = new ArrayList<>();
        if (unlockedNodes == null || unlockedNodes.isEmpty()) return out;
        for (SkillNode node : allNodes()) {
            if (unlockedNodes.contains(node.id()) && node.passiveBonus() != null
                    && !node.passiveBonus().isBlank() && !"none".equalsIgnoreCase(node.passiveBonus())) {
                out.add(node.id() + " | passive " + node.passiveBonus());
            }
        }
        return out;
    }

    static List<String> activeAbilityLines(Set<String> unlockedNodes) {
        ArrayList<String> out = new ArrayList<>();
        if (unlockedNodes == null || unlockedNodes.isEmpty()) return out;
        for (SkillNode node : allNodes()) {
            if (unlockedNodes.contains(node.id()) && node.activeAbility() != null
                    && !node.activeAbility().isBlank() && !"none".equalsIgnoreCase(node.activeAbility())) {
                out.add(node.id() + " | active " + node.activeAbility());
            }
        }
        return out;
    }

    static boolean applyStatEffect(Candidate candidate, String statEffect) {
        if (candidate == null || candidate.stats == null || statEffect == null || statEffect.isBlank()
                || "none".equalsIgnoreCase(statEffect)) return false;
        String[] parts = statEffect.split(":", 2);
        if (parts.length != 2 || parts[0].isBlank()) return false;
        int delta;
        try { delta = Integer.parseInt(parts[1].replace("+", "").trim()); } catch (Exception e) { return false; }
        String stat = canonicalStatName(parts[0], candidate.stats);
        int old = candidate.stats.getOrDefault(stat, Candidate.statMin(stat));
        int next = Math.max(Candidate.statMin(stat), Math.min(Candidate.statMax(stat), old + delta));
        candidate.stats.put(stat, next);
        return next != old;
    }

    private static boolean canUnlock(SkillNode node, Set<String> unlocked, int xp, SkillAccessContext context,
                                     Map<String, Integer> stats) {
        return node != null && !unlocked.contains(node.id()) && missingPrerequisite(node, unlocked).isBlank()
                && missingExclusiveGroup(node, unlocked).isBlank() && missingAccess(node, context).isBlank()
                && missingStat(node, stats).isBlank() && xp >= node.xpCost();
    }

    private static boolean allNodesHaveXp(List<SkillNode> nodes) {
        return nodes != null && !nodes.isEmpty() && nodes.stream().allMatch(node -> node != null && node.xpCost() > 0);
    }

    private static boolean hasPrerequisites(List<SkillNode> nodes) {
        return nodes != null && nodes.stream().anyMatch(node -> node != null
                && node.prerequisite() != null && !"none".equalsIgnoreCase(node.prerequisite()));
    }

    private static boolean hasAccessGates(List<SkillNode> nodes) {
        return nodes != null && nodes.stream().anyMatch(node -> node != null
                && node.accessRequirement() != null && !"none".equalsIgnoreCase(node.accessRequirement()));
    }

    private static boolean hasStatModifiers(List<SkillNode> nodes) {
        return nodes != null && nodes.stream().anyMatch(node -> node != null
                && node.statEffect() != null && !"none".equalsIgnoreCase(node.statEffect()));
    }

    private static boolean hasCapabilityHooks(List<SkillNode> nodes) {
        return nodes != null && nodes.stream().anyMatch(node -> node != null
                && (node.capabilityKey() != null && !"none".equalsIgnoreCase(node.capabilityKey())
                || node.passiveBonus() != null && !"none".equalsIgnoreCase(node.passiveBonus())
                || node.activeAbility() != null && !"none".equalsIgnoreCase(node.activeAbility())));
    }

    private static int exclusiveGroupCount(List<SkillNode> nodes) {
        LinkedHashSet<String> groups = new LinkedHashSet<>();
        if (nodes != null) {
            for (SkillNode node : nodes) {
                if (node != null && node.exclusiveGroup() != null && !"none".equalsIgnoreCase(node.exclusiveGroup())) {
                    groups.add(friendlyExclusiveGroup(node.exclusiveGroup()));
                }
            }
        }
        return groups.size();
    }

    private static String friendlyAccessRequirement(String value) {
        if (value == null || value.isBlank() || "none".equalsIgnoreCase(value)) return "none";
        String[] parts = value.split(":", 3);
        String kind = parts.length > 0 ? parts[0] : value;
        String target = parts.length > 1 ? parts[1] : "";
        if ("knowledge".equalsIgnoreCase(kind)) return "knowledge " + target;
        if ("trainer".equalsIgnoreCase(kind)) return "trainer " + target.replace('-', ' ').replace('_', ' ');
        if ("facility".equalsIgnoreCase(kind)) return "facility " + target.replace('-', ' ');
        if ("equipment".equalsIgnoreCase(kind)) return "equipment " + target.replace('-', ' ');
        if ("faction".equalsIgnoreCase(kind)) {
            String standing = parts.length > 2 ? " standing " + parts[2] : "";
            Faction faction = parseFaction(target);
            return "faction " + (faction == null ? target.replace('_', ' ') : faction.label) + standing;
        }
        return value.replace('-', ' ').replace('_', ' ');
    }

    private static String friendlyExclusiveGroup(String value) {
        if (value == null || value.isBlank() || "none".equalsIgnoreCase(value)) return "none";
        return value.replace('-', ' ').replace('_', ' ');
    }

    private static String friendlyCapabilityHook(SkillNode node) {
        if (node == null) return "none";
        ArrayList<String> hooks = new ArrayList<>();
        if (node.capabilityKey() != null && !"none".equalsIgnoreCase(node.capabilityKey())) {
            hooks.add(node.capabilityKey().replace('-', ' '));
        }
        if (node.passiveBonus() != null && !"none".equalsIgnoreCase(node.passiveBonus())) {
            hooks.add("passive " + node.passiveBonus().replace('-', ' '));
        }
        if (node.activeAbility() != null && !"none".equalsIgnoreCase(node.activeAbility())) {
            hooks.add("active " + node.activeAbility().replace('-', ' '));
        }
        return hooks.isEmpty() ? "none" : String.join("; ", hooks);
    }

    private static String friendlyNone(String value) {
        return value == null || value.isBlank() ? "none" : value.replace('-', ' ').replace('_', ' ');
    }

    private static String missingPrerequisite(SkillNode node, Set<String> unlocked) {
        if (node == null || node.prerequisite() == null || node.prerequisite().isBlank()
                || "none".equalsIgnoreCase(node.prerequisite())) return "";
        for (SkillNode other : allNodes()) {
            if (other.name().equalsIgnoreCase(node.prerequisite()) && !unlocked.contains(other.id())) return other.name();
        }
        return "";
    }

    private static String missingExclusiveGroup(SkillNode node, Set<String> unlocked) {
        if (node == null || node.exclusiveGroup() == null || node.exclusiveGroup().isBlank()
                || "none".equalsIgnoreCase(node.exclusiveGroup()) || unlocked == null || unlocked.isEmpty()) return "";
        for (SkillNode other : allNodes()) {
            if (other.id().equals(node.id())) continue;
            if (node.exclusiveGroup().equalsIgnoreCase(other.exclusiveGroup()) && unlocked.contains(other.id())) {
                return other.name() + " is already selected in " + node.exclusiveGroup();
            }
        }
        return "";
    }

    private static String missingStat(SkillNode node, Map<String, Integer> stats) {
        if (node == null || node.statRequirement() == null || node.statRequirement().isBlank()
                || "none".equalsIgnoreCase(node.statRequirement())) return "";
        String[] parts = node.statRequirement().split(":", 2);
        if (parts.length != 2 || parts[0].isBlank()) return "recognized stat " + node.statRequirement();
        int required;
        try { required = Integer.parseInt(parts[1].trim()); } catch (Exception e) { return "recognized stat " + node.statRequirement(); }
        String stat = canonicalStatName(parts[0], stats);
        int have = stats == null ? 0 : stats.getOrDefault(stat, 0);
        return have >= required ? "" : stat + " " + have + "/" + required;
    }

    private static String missingAccess(SkillNode node, SkillAccessContext context) {
        if (node == null || context == null || context.ignoreAccess()) return "";
        String requirement = node.accessRequirement();
        if (requirement == null || requirement.isBlank() || "none".equalsIgnoreCase(requirement)) return "";
        String[] parts = requirement.split(":", 3);
        String kind = parts.length > 0 ? normalize(parts[0]) : "";
        String value = parts.length > 1 ? parts[1] : "";
        if ("knowledge".equals(kind)) return containsToken(context.knowledges(), value) ? "" : "knowledge " + value;
        if ("trainer".equals(kind)) return containsToken(context.trainers(), value) ? "" : "trainer " + value;
        if ("facility".equals(kind)) return containsToken(context.facilities(), value) ? "" : "facility " + value;
        if ("equipment".equals(kind)) return containsToken(context.equipment(), value) ? "" : "equipment " + value;
        if ("faction".equals(kind)) {
            int required = 1;
            if (parts.length > 2) {
                try { required = Integer.parseInt(parts[2]); } catch (Exception ignored) { required = 1; }
            }
            Faction faction = parseFaction(value);
            int standing = faction == null || context.factionStanding() == null ? 0 : context.factionStanding().getOrDefault(faction, 0);
            return standing >= required ? "" : "faction " + value + " standing " + required;
        }
        return "recognized access " + requirement;
    }

    private static Faction parseFaction(String value) {
        if (value == null || value.isBlank()) return null;
        String wanted = normalize(value);
        for (Faction faction : Faction.values()) {
            if (normalize(faction.name()).equals(wanted) || normalize(faction.label).equals(wanted)) return faction;
        }
        return null;
    }

    private static boolean containsToken(Set<String> values, String expected) {
        if (expected == null || expected.isBlank()) return true;
        String wanted = normalize(expected);
        if (values == null) return false;
        for (String value : values) if (normalize(value).equals(wanted)) return true;
        return false;
    }

    private static void addToken(Set<String> values, String value) {
        if (values != null && value != null && !value.isBlank()) values.add(value);
    }

    private static String canonicalStatName(String raw, Map<String, Integer> stats) {
        if (raw == null) return "";
        String wanted = normalize(raw);
        if (stats != null) {
            for (String stat : stats.keySet()) if (normalize(stat).equals(wanted)) return stat;
        }
        for (String stat : Candidate.statKeys()) if (normalize(stat).equals(wanted)) return stat;
        return raw.trim();
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(java.util.Locale.ROOT)
                .replace('_', '-').replace(' ', '-').replaceAll("-+", "-");
    }

    private static SkillBranch branch(String id, String name, String worldUse, SkillNode... nodes) {
        return new SkillBranch(id, name, worldUse, List.of(nodes));
    }

    private static SkillNode node(String branchId, String id, String name, int xpCost, String prerequisite,
                                  String capability, String visibleEffect, String knowledgeBoundary) {
        return new SkillNode(id, branchId, name, xpCost, prerequisite,
                capability, visibleEffect, knowledgeBoundary, "none", "none", "none", "none", id, "none", "none");
    }

    private static SkillNode hookNode(String branchId, String id, String name, int xpCost, String prerequisite,
                                      String capability, String visibleEffect, String knowledgeBoundary,
                                      String passiveBonus, String activeAbility) {
        return new SkillNode(id, branchId, name, xpCost, prerequisite,
                capability, visibleEffect, knowledgeBoundary, "none", "none", "none", "none",
                id, passiveBonus, activeAbility);
    }

    private static SkillNode gatedNode(String branchId, String id, String name, int xpCost, String prerequisite,
                                       String capability, String visibleEffect, String knowledgeBoundary,
                                       String accessRequirement) {
        return new SkillNode(id, branchId, name, xpCost, prerequisite,
                capability, visibleEffect, knowledgeBoundary, accessRequirement, "none", "none", "none",
                id, "none", "none");
    }

    private static SkillNode gatedNode(String branchId, String id, String name, int xpCost, String prerequisite,
                                       String capability, String visibleEffect, String knowledgeBoundary,
                                       String accessRequirement, String statRequirement, String statEffect) {
        return new SkillNode(id, branchId, name, xpCost, prerequisite,
                capability, visibleEffect, knowledgeBoundary, accessRequirement, statRequirement, statEffect, "none",
                id, "none", "none");
    }

    private static SkillNode gatedNode(String branchId, String id, String name, int xpCost, String prerequisite,
                                       String capability, String visibleEffect, String knowledgeBoundary,
                                       String accessRequirement, String statRequirement, String statEffect,
                                       String exclusiveGroup) {
        return new SkillNode(id, branchId, name, xpCost, prerequisite,
                capability, visibleEffect, knowledgeBoundary, accessRequirement, statRequirement, statEffect, exclusiveGroup,
                id, "none", "none");
    }

    private static SkillNode statNode(String branchId, String id, String name, int xpCost, String prerequisite,
                                      String capability, String visibleEffect, String knowledgeBoundary,
                                      String statRequirement, String statEffect) {
        return new SkillNode(id, branchId, name, xpCost, prerequisite,
                capability, visibleEffect, knowledgeBoundary, "none", statRequirement, statEffect, "none",
                id, "none", "none");
    }

    private static SkillNode specializedNode(String branchId, String id, String name, int xpCost, String prerequisite,
                                             String capability, String visibleEffect, String knowledgeBoundary,
                                             String exclusiveGroup) {
        return new SkillNode(id, branchId, name, xpCost, prerequisite,
                capability, visibleEffect, knowledgeBoundary, "none", "none", "none", exclusiveGroup,
                id, "none", "none");
    }

    private static SkillNode specializedNode(String branchId, String id, String name, int xpCost, String prerequisite,
                                             String capability, String visibleEffect, String knowledgeBoundary,
                                             String exclusiveGroup, String passiveBonus, String activeAbility) {
        return new SkillNode(id, branchId, name, xpCost, prerequisite,
                capability, visibleEffect, knowledgeBoundary, "none", "none", "none", exclusiveGroup,
                id, passiveBonus, activeAbility);
    }
}
