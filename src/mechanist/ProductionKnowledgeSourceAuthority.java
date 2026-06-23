package mechanist;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Resolves whether production knowledge comes from the player, machine, or claimed facility. */
final class ProductionKnowledgeSourceAuthority {
    record KnowledgeSource(boolean available, boolean playerSupplied, boolean machineSupplied,
                           boolean facilitySupplied, String facilityProvider, String requiredKnowledge,
                           String sourceLabel, Set<String> effectiveKnowledge,
                           List<String> lines) { }

    private ProductionKnowledgeSourceAuthority() { }

    static KnowledgeSource evaluate(GamePanel game, BaseObject machine, String requiredKnowledge) {
        String required = requiredKnowledge == null ? "" : requiredKnowledge.trim();
        HashSet<String> effective = new HashSet<>();
        if (game != null && game.unlockedKnowledges != null) effective.addAll(game.unlockedKnowledges);
        if (required.isBlank()) {
            return new KnowledgeSource(true, true, false, false, "", "none", "no doctrine required", Set.copyOf(effective),
                    List.of("Knowledge source: no doctrine required."));
        }
        boolean player = game != null && game.hasKnowledge(required);
        boolean machineSupplied = machine != null && machine.machineKnowledge != null
                && machine.machineKnowledge.equalsIgnoreCase(required);
        ProductionFacilityKnowledgeAuthority.FacilityKnowledge facility =
                ProductionFacilityKnowledgeAuthority.evaluate(game, machine, required);
        if (machineSupplied || facility.supplied()) effective.add(required);
        boolean available = player || machineSupplied || facility.supplied();
        String source = sourceLabel(player, machineSupplied, facility);
        return new KnowledgeSource(available, player, machineSupplied, facility.supplied(), facility.providerName(),
                required, source, Set.copyOf(effective), List.of(
                        "Knowledge source: " + required + " / " + source + ".",
                        machine == null ? "Machine doctrine slot: no required machine selected."
                                : "Machine doctrine slot: " + safe(machine.machineKnowledge, "empty") + ".",
                        facility.lines().get(0)));
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

    static List<String> definitionAuditLines() {
        return List.of(
                "Production knowledge source audit: owner=ProductionKnowledgeSourceAuthority, sources=player knowledge+selected machine doctrine+claimed facility doctrine, effectiveKnowledgeUnion=true, ordinaryUiRawIds=false.",
                "Player doctrine audit: unlocked knowledge can satisfy recipe visibility, execution readiness, and doctrine quality tier; spending skill XP never grants recipe doctrine.",
                "Machine doctrine audit: selected machines preserve one installed recipe doctrine in machineKnowledge; Teach Machine requires the player to know the doctrine before installing it.",
                "Facility doctrine audit: owner=ProductionFacilityKnowledgeAuthority, provider=another serviceable production station in the same claimed production room, brokenStations=false, outsideRoom=false, unclaimedWorkspace=false.",
                "Knowledge quality audit: doctrine tier comes from the effective knowledge set and contributes to production quality caps without replacing recipe, machine, material, facility, tool, or operator caps.",
                "Knowledge boundary audit: this audit records source ownership and readiness only; it does not mutate recipes, grant knowledge, consume inputs, start production, or reveal hidden contract targets.",
                "Guard: Milestone03ProductionKnowledgeSourceAuditSmoke checks source owners, machine/facility doctrine boundaries, effective knowledge union, knowledge-quality contribution, and raw-ID hiding."
        );
    }

    private static String safe(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private static String sourceLabel(boolean player, boolean machine,
                                      ProductionFacilityKnowledgeAuthority.FacilityKnowledge facility) {
        if (player && machine && facility.supplied()) return "player, machine, and claimed facility";
        if (player && machine) return "player and machine";
        if (player && facility.supplied()) return "player and claimed facility";
        if (machine && facility.supplied()) return "installed machine doctrine and claimed facility";
        if (player) return "player";
        if (machine) return "installed machine doctrine";
        if (facility.supplied()) return "claimed facility via " + facility.providerName();
        return "unavailable";
    }
}
