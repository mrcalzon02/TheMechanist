package mechanist;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

final class ContractObjectiveReadabilityAuthority {
    private ContractObjectiveReadabilityAuthority() {}

    static List<String> summary(List<FactionContract> contracts, List<String> carriedItems, List<String> storedItems, int limit) {
        return summary(contracts, carriedItems, storedItems, limit, Set.of(), Set.of());
    }

    static List<String> summary(List<FactionContract> contracts, List<String> carriedItems, List<String> storedItems,
                                int limit, Set<String> unlockedSkillNodes, Set<String> unlockedKnowledges) {
        ArrayList<String> lines = new ArrayList<>();
        int active = 0;
        if (contracts != null) for (FactionContract contract : contracts) if (contract != null && !contract.completed) active++;
        lines.add("Active contracts: " + active + ".");
        if (active == 0) {
            lines.add("No active faction contract objectives are recorded.");
            return lines;
        }

        int shown = 0;
        for (FactionContract contract : contracts) {
            if (contract == null || contract.completed || shown >= Math.max(1, limit)) continue;
            String item = contract.publicRequiredItem();
            boolean carried = containsNamed(carriedItems, contract.requiredTurnInItem);
            boolean stored = containsNamed(storedItems, contract.requiredTurnInItem);
            lines.add(contract.displayFactionName() + " / " + contract.displayType() + ": " + objectiveText(contract) + ".");
            lines.add("Route: " + contract.displayLocation() + "; "
                    + (contract.spawned ? "target or objective confirmed" : "contract route recorded; exact local target not confirmed") + ".");
            lines.add("Required proof or delivery: " + item + " / "
                    + (carried ? "carried and ready for turn-in" : stored ? "held in base storage; retrieve before turn-in" : "not currently held") + ".");
            lines.addAll(skillProofLines(contract, unlockedSkillNodes, unlockedKnowledges));
            lines.add("Reward: " + contract.payout + " script and faction standing +" + contract.repReward + ".");
            shown++;
        }
        if (active > shown) lines.add((active - shown) + " additional active contract(s) not shown in this compact view.");
        lines.add("Evidence boundary: hidden identities and exact local targets remain undisclosed until the contract route confirms them.");
        return lines;
    }

    static List<String> skillProofLines(FactionContract contract, Set<String> unlockedSkillNodes,
                                        Set<String> unlockedKnowledges) {
        ArrayList<String> lines = new ArrayList<>();
        if (contract == null) return lines;
        LinkedHashSet<String> neededSkills = requiredCapabilityKeys(contract);
        LinkedHashSet<String> neededKnowledge = requiredKnowledgeNames(contract);
        if (neededSkills.isEmpty() && neededKnowledge.isEmpty()) {
            lines.add("Skill proof: this contract has no explicit skill or knowledge proof gate yet.");
            return lines;
        }
        for (String skill : neededSkills) {
            boolean ready = SkillTreeProgressionAuthority.hasCapability(unlockedSkillNodes, skill);
            lines.add("Skill proof: " + capabilityLabel(skill) + " / " + (ready ? "trained" : "not trained") + ".");
        }
        Set<String> knowledges = unlockedKnowledges == null ? Set.of() : unlockedKnowledges;
        for (String knowledge : neededKnowledge) {
            boolean known = containsKnowledge(knowledges, knowledge);
            lines.add("Knowledge proof: " + knowledge + " / " + (known ? "known" : "not known") + ".");
        }
        lines.add("Contract proof boundary: these lines explain readiness; contract completion and reward rules remain owned by the contract turn-in flow.");
        return lines;
    }

    static List<String> auditLines(List<FactionContract> contracts, Set<String> unlockedSkillNodes,
                                   Set<String> unlockedKnowledges, int limit) {
        ArrayList<String> lines = new ArrayList<>();
        int active = 0;
        if (contracts != null) for (FactionContract contract : contracts) if (contract != null && !contract.completed) active++;
        lines.add("Contract proof audit: activeContracts=" + active + ", shownLimit=" + Math.max(1, limit)
                + ", owner=ContractObjectiveReadabilityAuthority, completionMutation=false, rewardMutation=false, rawIdsHidden=true.");
        int shown = 0;
        if (contracts != null) {
            for (FactionContract contract : contracts) {
                if (contract == null || contract.completed || shown >= Math.max(1, limit)) continue;
                LinkedHashSet<String> neededSkills = requiredCapabilityKeys(contract);
                LinkedHashSet<String> neededKnowledge = requiredKnowledgeNames(contract);
                if (neededSkills.isEmpty() && neededKnowledge.isEmpty()) {
                    lines.add("Contract proof audit: " + contract.displayFactionName() + " / " + contract.displayType()
                            + " has no explicit skill or knowledge proof gate.");
                    shown++;
                    continue;
                }
                ArrayList<String> skillStates = new ArrayList<>();
                for (String skill : neededSkills) {
                    skillStates.add(capabilityLabel(skill) + ":"
                            + (SkillTreeProgressionAuthority.hasCapability(unlockedSkillNodes, skill) ? "trained" : "not trained"));
                }
                ArrayList<String> knowledgeStates = new ArrayList<>();
                Set<String> knowledges = unlockedKnowledges == null ? Set.of() : unlockedKnowledges;
                for (String knowledge : neededKnowledge) {
                    knowledgeStates.add(knowledge + ":" + (containsKnowledge(knowledges, knowledge) ? "known" : "not known"));
                }
                lines.add("Contract proof audit: " + contract.displayFactionName() + " / " + contract.displayType()
                        + " skillProof=" + joinStates(skillStates) + ", knowledgeProof=" + joinStates(knowledgeStates)
                        + ", evidenceLocation=" + (contract.spawned ? "route confirmed" : "route unconfirmed")
                        + ", boundary=readiness only.");
                shown++;
            }
        }
        if (active > shown) lines.add("Contract proof audit: additionalHiddenByCompactLimit=" + (active - shown) + ".");
        lines.add("Guard: Milestone03ContractSkillProofAuditSmoke checks proof audit ownership, readiness states, mutation boundaries, and raw-ID hiding.");
        return lines;
    }

    private static String objectiveText(FactionContract contract) {
        if ("BOUNTY".equals(contract.type)) return "find " + safe(contract.targetName, "the marked target") + " and recover " + contract.publicRequiredItem();
        if ("LOCKBOX".equals(contract.type)) return "acquire " + contract.publicRequiredItem() + " from the named vault route";
        return "retrieve " + safe(contract.targetName, contract.publicRequiredItem()) + " without compromising it";
    }

    private static LinkedHashSet<String> requiredCapabilityKeys(FactionContract contract) {
        LinkedHashSet<String> keys = new LinkedHashSet<>();
        String text = contractText(contract);
        if ("LOCKBOX".equals(contract.type) || containsAny(text, "certificate", "permit", "commerce", "bank", "noble")) {
            keys.add("trade-guilder-certification");
        }
        if ("FETCH".equals(contract.type) || containsAny(text, "sealed object", "evidence", "retrieve", "vault")) {
            keys.add("investigation-trace-reading");
        }
        if (containsAny(text, "counterfeit", "stolen", "black-market", "contraband")) {
            keys.add("trade-streetwise-appraisal");
        }
        if (containsAny(text, "machine part", "component", "tool", "fabrication", "repair", "build")) {
            keys.add("fab-repair-material-eye");
        }
        return keys;
    }

    private static LinkedHashSet<String> requiredKnowledgeNames(FactionContract contract) {
        LinkedHashSet<String> names = new LinkedHashSet<>();
        String text = contractText(contract);
        if (containsAny(text, "contract", "bank", "certificate", "permit", "commerce", "noble")) {
            names.add("Contract Negotiation");
        }
        if (containsAny(text, "machine part", "component", "tool", "fabrication", "repair", "build")) {
            names.add("Scrap-Forging Doctrine");
        }
        return names;
    }

    private static String capabilityLabel(String key) {
        SkillTreeProgressionAuthority.SkillNode node = SkillTreeProgressionAuthority.nodeByIdOrName(key);
        return node == null ? key : node.name();
    }

    private static String joinStates(List<String> states) {
        if (states == null || states.isEmpty()) return "none";
        return String.join("; ", states);
    }

    private static boolean containsKnowledge(Set<String> knowledges, String wanted) {
        if (knowledges == null || wanted == null || wanted.isBlank()) return false;
        for (String knowledge : knowledges) if (knowledge != null && knowledge.equalsIgnoreCase(wanted)) return true;
        return false;
    }

    private static boolean containsNamed(List<String> items, String wanted) {
        if (items == null || wanted == null || wanted.isBlank()) return false;
        for (String item : items) if (ItemQuality.namesMatch(item, wanted)) return true;
        return false;
    }

    private static String contractText(FactionContract contract) {
        if (contract == null) return "";
        return safe(contract.type, "") + " " + safe(contract.description, "") + " " + safe(contract.targetName, "")
                + " " + safe(contract.requiredTurnInItem, "") + " " + contract.publicRequiredItem();
    }

    private static boolean containsAny(String text, String... needles) {
        String value = text == null ? "" : text.toLowerCase(java.util.Locale.ROOT);
        for (String needle : needles) if (needle != null && value.contains(needle.toLowerCase(java.util.Locale.ROOT))) return true;
        return false;
    }

    private static String safe(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
