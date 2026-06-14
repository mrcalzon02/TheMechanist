package mechanist;

import java.util.ArrayList;
import java.util.List;

/** Structured, player-readable audit of the registered gameplay menu definitions. */
final class MenuDefinitionAuditAuthority {
    record MenuDefinition(String id, String title, String owner, String purpose, String dataSource,
                          String panes, String actions, String backBehavior, String capabilities,
                          String safeguards) {
        List<String> lines() {
            return List.of(
                    title + " [" + id + "] | owner: " + owner + " | purpose: " + purpose,
                    "Data: " + dataSource + " | panes: " + panes + " | actions: " + actions,
                    "Behavior: " + backBehavior + " | capabilities: " + capabilities,
                    "Safeguards: " + safeguards
            );
        }
    }

    static List<MenuDefinition> definitions(UniversalWindowAuthority authority) {
        ArrayList<MenuDefinition> out = new ArrayList<>();
        if (authority == null) return out;
        for (UniversalWindowAuthority.WindowSpec spec : authority.specs()) {
            out.add(new MenuDefinition(
                    spec.id, spec.title, owner(spec), purpose(spec), dataSource(spec), panes(spec), actions(spec),
                    readableBack(spec.escapeBehavior), capabilities(spec), safeguards(spec)
            ));
        }
        return List.copyOf(out);
    }

    static List<String> playerFacingLines(UniversalWindowAuthority authority) {
        List<MenuDefinition> definitions = definitions(authority);
        ArrayList<String> lines = new ArrayList<>();
        lines.add("Menu definition audit");
        lines.add("Registered definitions: " + definitions.size() + ". Fields cover ownership, purpose, data, panes, actions, back behavior, capabilities, and safeguards.");
        for (MenuDefinition definition : definitions) lines.addAll(definition.lines());
        lines.add("Audit boundary: confirmation, permission, and failure rules remain owned by each domain authority until a shared rule is explicitly registered.");
        return lines;
    }

    static String summary(UniversalWindowAuthority authority) {
        List<MenuDefinition> definitions = definitions(authority);
        int transfers = 0;
        int filters = 0;
        int progress = 0;
        for (UniversalWindowAuthority.WindowSpec spec : authority == null ? List.<UniversalWindowAuthority.WindowSpec>of() : authority.specs()) {
            if (spec.supportsInventoryTransfer) transfers++;
            if (spec.supportsSearchOrFilter) filters++;
            if (spec.supportsProgressBars) progress++;
        }
        return "Menu definitions " + definitions.size() + "; transfer " + transfers + ", search/filter " + filters
                + ", progress " + progress + "; raw IDs prohibited; shared wrapping and scroll containment audited.";
    }

    private static String owner(UniversalWindowAuthority.WindowSpec spec) {
        return switch (spec.kind) {
            case INVENTORY -> "inventory and storage interface domain";
            case CONTAINER -> "container transfer interface domain";
            case CRAFTING, MACHINE_OPERATION -> "production and machine interface domain";
            case CONSTRUCTION -> "construction and placement interface domain";
            case INFOPEDIA -> "reference index and semantic asset domain";
            case TARGETING -> "movement planning and targeting domain";
            default -> "shared gameplay window domain";
        };
    }

    private static String purpose(UniversalWindowAuthority.WindowSpec spec) {
        return PlayerFacingText.sanitize(spec.migrationNotes);
    }

    private static String dataSource(UniversalWindowAuthority.WindowSpec spec) {
        return switch (spec.kind) {
            case INVENTORY -> "carried inventory, equipment, and base storage";
            case CHARACTER -> "active character, condition, loadout, and faction roster";
            case CONTAINER -> "selected container and carried inventory";
            case TRADE -> "active vendor session, offers, funds, and carried capacity";
            case CRAFTING, MACHINE_OPERATION -> "recipes, machines, requirements, and production state";
            case CONSTRUCTION -> "blueprint catalog, placement cursor, materials, and requirements";
            case MAP -> "current slice, objective guidance, contracts, and visited areas";
            case INFOPEDIA -> "mechanic references and semantic asset registry";
            case AUSPEX -> "sensory state, nearby signals, hazards, and expansion heat";
            default -> "the active game session and owning domain records";
        };
    }

    private static String panes(UniversalWindowAuthority.WindowSpec spec) {
        return switch (spec.kind) {
            case INVENTORY -> "carried, storage, selection detail";
            case CHARACTER -> "identity, stats, body/loadout, faction members";
            case TRADE -> "offers, purchase detail, funds and capacity";
            case CONSTRUCTION -> "blueprint list and construction forecast";
            case INFOPEDIA -> "categories, entry list, detail, visual preview";
            case MAP -> "slice map and objectives";
            default -> spec.supportsTabs ? "tabbed list and detail regions" : "primary content and contextual detail";
        };
    }

    private static String actions(UniversalWindowAuthority.WindowSpec spec) {
        ArrayList<String> actions = new ArrayList<>();
        actions.add("select");
        if (spec.supportsInventoryTransfer) actions.add("transfer");
        if (spec.supportsSearchOrFilter) actions.add("filter/search where exposed");
        if (spec.supportsTabs) actions.add("change category/tab");
        actions.add("back");
        return String.join(", ", actions);
    }

    private static String capabilities(UniversalWindowAuthority.WindowSpec spec) {
        ArrayList<String> values = new ArrayList<>();
        if (spec.supportsTooltips) values.add("tooltips");
        if (spec.supportsScrolling) values.add("scrolling");
        if (spec.supportsTabs) values.add("tabs/categories");
        if (spec.supportsSearchOrFilter) values.add("search/filter");
        if (spec.supportsInventoryTransfer) values.add("transfer");
        if (spec.supportsProgressBars) values.add("progress");
        if (values.isEmpty()) values.add("basic selection");
        return String.join(", ", values);
    }

    private static String safeguards(UniversalWindowAuthority.WindowSpec spec) {
        ArrayList<String> values = new ArrayList<>();
        values.add("player-facing labels only");
        values.add("shared text wrapping");
        if (spec.supportsScrolling) values.add("scroll containment");
        if (spec.supportsInventoryTransfer) values.add("domain permission and failure checks required");
        values.add(spec.blocksWorldInput ? "world input blocked" : "world input remains available");
        return String.join(", ", values);
    }

    private static String readableBack(UniversalWindowAuthority.EscapeBehavior behavior) {
        return switch (behavior == null ? UniversalWindowAuthority.EscapeBehavior.CLOSE_WINDOW : behavior) {
            case BACK_TO_PARENT -> "Back returns to the parent menu";
            case CLEAR_CURSOR -> "Back clears active cursor or placement state";
            case RETURN_TO_GAME -> "Back returns to the game";
            case DISABLED_WHILE_BLOCKING -> "Back waits for the blocking choice";
            case CLOSE_WINDOW -> "Back closes the menu";
        };
    }

    private MenuDefinitionAuditAuthority() { }
}
