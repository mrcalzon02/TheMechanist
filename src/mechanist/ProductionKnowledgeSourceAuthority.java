package mechanist;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Resolves whether production knowledge comes from the player or the selected machine. */
final class ProductionKnowledgeSourceAuthority {
    record KnowledgeSource(boolean available, boolean playerSupplied, boolean machineSupplied,
                           String requiredKnowledge, String sourceLabel, Set<String> effectiveKnowledge,
                           List<String> lines) { }

    private ProductionKnowledgeSourceAuthority() { }

    static KnowledgeSource evaluate(GamePanel game, BaseObject machine, String requiredKnowledge) {
        String required = requiredKnowledge == null ? "" : requiredKnowledge.trim();
        HashSet<String> effective = new HashSet<>();
        if (game != null && game.unlockedKnowledges != null) effective.addAll(game.unlockedKnowledges);
        if (required.isBlank()) {
            return new KnowledgeSource(true, true, false, "none", "no doctrine required", Set.copyOf(effective),
                    List.of("Knowledge source: no doctrine required."));
        }
        boolean player = game != null && game.hasKnowledge(required);
        boolean machineSupplied = machine != null && machine.machineKnowledge != null
                && machine.machineKnowledge.equalsIgnoreCase(required);
        if (machineSupplied) effective.add(required);
        boolean available = player || machineSupplied;
        String source = player && machineSupplied ? "player and machine"
                : player ? "player"
                : machineSupplied ? "installed machine doctrine"
                : "unavailable";
        return new KnowledgeSource(available, player, machineSupplied, required, source, Set.copyOf(effective),
                List.of("Knowledge source: " + required + " / " + source + ".",
                        machine == null ? "Machine doctrine slot: no required machine selected."
                                : "Machine doctrine slot: " + safe(machine.machineKnowledge, "empty") + "."));
    }

    static String install(GamePanel game, BaseObject machine, String requiredKnowledge) {
        String required = requiredKnowledge == null ? "" : requiredKnowledge.trim();
        if (game == null) return "Machine teaching failed: no active game.";
        if (machine == null) return "Machine teaching failed: build or select the required machine first.";
        if (required.isBlank()) return "Machine teaching unnecessary: this recipe requires no doctrine.";
        if (!game.hasKnowledge(required)) return "Machine teaching failed: learn " + required + " before installing it.";
        String prior = safe(machine.machineKnowledge, "empty");
        machine.machineKnowledge = required;
        return "Installed " + required + " in " + machine.name + "; replaced " + prior + ".";
    }

    private static String safe(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
