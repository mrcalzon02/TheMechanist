package mechanist;

import java.util.List;
import java.util.Locale;

/** Shared descriptor authority for contextual management windows before full Swing extraction. */
final class UniversalManagementWindowAuthority {
    static final String VERSION = "universal-management-window-authority-0.9.10gv";

    private UniversalManagementWindowAuthority() {}

    static WindowDescriptor descriptor(String contextType) {
        String ctx = contextType == null ? "generic" : contextType.toLowerCase(Locale.ROOT).trim();
        return switch (ctx) {
            case "dialogue", "npc", "diplomacy" -> new WindowDescriptor(
                    "dialogue-standings", "Dialogue / Standings", "chronological dialogue log", "numbered choices and standings actions",
                    List.of("Escape/back closes one layer", "standing tags link to diplomacy", "locked choices expose requirements before selection"));
            case "container", "cargo", "routing", "inventory" -> new WindowDescriptor(
                    "container-routing", "Container / Cargo Routing", "target inventory or routing source", "player inventory anchor and transfer actions",
                    List.of("player inventory anchor remains stable", "shift-click transfers one stack", "ctrl-click transfers matching stacks", "routing view uses source-carrier-destination nodes"));
            case "machine", "recipe", "production" -> new WindowDescriptor(
                    "machine-production", "Machine / Recipe Selection", "available recipes and bill of materials", "production monitor and run policy",
                    List.of("recipe list is searchable", "BOM compares required vs detected resources", "production can queue or run continuously"));
            case "faction", "personnel", "assignments" -> new WindowDescriptor(
                    "faction-personnel", "Faction / Personnel Management", "player command roster", "NPC command roster and assignments",
                    List.of("player and NPC rosters stay separate", "rank tiers are shared for command parity", "founder is tier 0 and cannot be assigned to recruits"));
            default -> new WindowDescriptor(
                    "generic-context", "Contextual Window", "context data", "inspection and actions",
                    List.of("Escape/back closes one layer", "right-click empty background closes or steps back", "world view remains visible behind the window"));
        };
    }

    static String summary() {
        return "Universal management windows: base wrapper owns chrome/back/close/context; modules supply dialogue, inventory/routing, machine, and faction/personnel panes; inventory-moving windows preserve a stable player inventory anchor; personnel panes preserve separate player and NPC rosters while using shared command tiers.";
    }

    static String itemizedSummary() {
        StringBuilder sb = new StringBuilder(summary());
        for (String ctx : List.of("dialogue", "container", "machine", "faction")) {
            WindowDescriptor d = descriptor(ctx);
            sb.append('\n').append(" - ").append(d.moduleId()).append(": ").append(d.title())
                    .append(" | left=").append(d.leftPane()).append(" | right=").append(d.rightPane())
                    .append(" | rules=").append(String.join("; ", d.rules()));
        }
        return sb.toString();
    }

    record WindowDescriptor(String moduleId, String title, String leftPane, String rightPane, List<String> rules) { }
}
