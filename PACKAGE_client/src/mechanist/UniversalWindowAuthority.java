package mechanist;

import java.util.*;

/**
 * UniversalWindow/UI cohesion authority.
 *
 * Records canonical lifecycle behavior and shared window capabilities so
 * inventory, construction, dialogue, machine, faction, editor, and guidance
 * windows can converge without duplicated rules or heavy runtime overhead.
 */
final class UniversalWindowAuthority {
    static final String VERSION = "0.9.08s";

    enum WindowKind {
        INVENTORY,
        MACHINE_OPERATION,
        CONSTRUCTION,
        DIALOGUE,
        FACTION,
        MAP,
        INFOPEDIA,
        TRADE,
        OPTIONS,
        SAVE_LOAD,
        EDITOR_HELD,
        ADMIN_HELD,
        HINTS_HELD
    }

    enum LifecycleState {
        CLOSED,
        OPENING,
        OPEN,
        FOCUSED,
        BACKGROUND,
        CLOSING
    }

    enum EscapeBehavior {
        CLOSE_WINDOW,
        BACK_TO_PARENT,
        CLEAR_CURSOR,
        RETURN_TO_GAME,
        DISABLED_WHILE_BLOCKING
    }

    static final class WindowSpec {
        final WindowKind kind;
        final String id;
        final String title;
        final EscapeBehavior escapeBehavior;
        final boolean supportsTooltips;
        final boolean supportsScrolling;
        final boolean supportsTabs;
        final boolean supportsInventoryTransfer;
        final boolean supportsProgressBars;
        final boolean supportsSearchOrFilter;
        final boolean blocksWorldInput;
        final String migrationNotes;

        WindowSpec(WindowKind kind, String id, String title, EscapeBehavior escapeBehavior,
                   boolean supportsTooltips, boolean supportsScrolling, boolean supportsTabs,
                   boolean supportsInventoryTransfer, boolean supportsProgressBars,
                   boolean supportsSearchOrFilter, boolean blocksWorldInput, String migrationNotes) {
            this.kind = kind;
            this.id = clean(id, kind.name().toLowerCase(Locale.ROOT));
            this.title = clean(title, this.id);
            this.escapeBehavior = escapeBehavior == null ? EscapeBehavior.CLOSE_WINDOW : escapeBehavior;
            this.supportsTooltips = supportsTooltips;
            this.supportsScrolling = supportsScrolling;
            this.supportsTabs = supportsTabs;
            this.supportsInventoryTransfer = supportsInventoryTransfer;
            this.supportsProgressBars = supportsProgressBars;
            this.supportsSearchOrFilter = supportsSearchOrFilter;
            this.blocksWorldInput = blocksWorldInput;
            this.migrationNotes = clean(migrationNotes, "no migration notes recorded");
        }

        String compactLine() {
            return id + " kind=" + kind + " escape=" + escapeBehavior
                    + " ui[tooltip=" + supportsTooltips
                    + ",scroll=" + supportsScrolling
                    + ",tabs=" + supportsTabs
                    + ",transfer=" + supportsInventoryTransfer
                    + ",progress=" + supportsProgressBars
                    + ",filter=" + supportsSearchOrFilter
                    + "] blocksWorld=" + blocksWorldInput;
        }
    }

    static final class RuntimeWindowState {
        final WindowSpec spec;
        LifecycleState state = LifecycleState.CLOSED;
        int openedTurn = -1;
        int focusTurn = -1;
        int closeTurn = -1;
        String context = "none";

        RuntimeWindowState(WindowSpec spec) {
            this.spec = spec;
        }

        String auditLine() {
            return spec.id + " state=" + state + " opened=" + openedTurn + " focused=" + focusTurn + " closed=" + closeTurn + " context=" + context;
        }
    }

    private final LinkedHashMap<String, WindowSpec> specs = new LinkedHashMap<>();
    private final LinkedHashMap<String, RuntimeWindowState> states = new LinkedHashMap<>();
    private String focusedWindowId = "";
    private long openEvents = 0L;
    private long closeEvents = 0L;
    private long focusEvents = 0L;

    UniversalWindowAuthority() {
        registerDefaults();
    }

    private void registerDefaults() {
        register(new WindowSpec(WindowKind.INVENTORY, "inventory", "Inventory", EscapeBehavior.RETURN_TO_GAME, true, true, true, true, false, true, true,
                "Migrate carried inventory, base storage, trade shelves, corpse loot, and room caches to one shared transfer language."));
        register(new WindowSpec(WindowKind.MACHINE_OPERATION, "machine_operations", "Machine Operations", EscapeBehavior.BACK_TO_PARENT, true, true, true, true, true, true, true,
                "Expose MachineOperationQueue records through shared progress bars and state labels instead of bespoke machine panels."));
        register(new WindowSpec(WindowKind.CONSTRUCTION, "construction", "Construction", EscapeBehavior.CLEAR_CURSOR, true, true, true, false, true, true, false,
                "Unify build placement, blueprint validation, room metadata, cost summaries, and error messages."));
        register(new WindowSpec(WindowKind.DIALOGUE, "dialogue", "Dialogue", EscapeBehavior.BACK_TO_PARENT, true, true, false, false, false, false, true,
                "NPC conversation, faction representatives, quest offers, and tutorial guidance should use shared branching and return rules."));
        register(new WindowSpec(WindowKind.FACTION, "faction", "Faction Ledger", EscapeBehavior.RETURN_TO_GAME, true, true, true, false, true, true, true,
                "Faction standings, contracts, staffing, and sub-faction summaries should share one ledger surface."));
        register(new WindowSpec(WindowKind.MAP, "map", "Map", EscapeBehavior.RETURN_TO_GAME, true, true, true, false, false, true, true,
                "Zone, sector, road, infrastructure, and multi-sector views should reuse common tabs and filters."));
        register(new WindowSpec(WindowKind.INFOPEDIA, "infopedia", "Infopedia", EscapeBehavior.RETURN_TO_GAME, true, true, true, false, false, true, true,
                "Knowledge, hints, searched guidance, and tutorial references should eventually share a searchable help authority."));
        register(new WindowSpec(WindowKind.TRADE, "trade", "Trade", EscapeBehavior.BACK_TO_PARENT, true, true, true, true, false, true, true,
                "Vendor, faction stock, player storage, and market panels should use shared transfer and price display rules."));
        register(new WindowSpec(WindowKind.OPTIONS, "options", "Options", EscapeBehavior.BACK_TO_PARENT, true, true, true, false, false, false, true,
                "Display, sound, controls, and graphics settings should retain consistent tab/back behavior."));
        register(new WindowSpec(WindowKind.SAVE_LOAD, "save_load", "Save / Load", EscapeBehavior.BACK_TO_PARENT, true, true, true, false, true, false, true,
                "Manual slots, autosaves, loss-screen loading, and server/headless save surfaces should share slot rendering."));
        register(new WindowSpec(WindowKind.EDITOR_HELD, "editor", "Editor", EscapeBehavior.BACK_TO_PARENT, true, true, true, true, true, true, true,
                "In-game editors reuse the same window authority rather than creating a second UI framework."));
        register(new WindowSpec(WindowKind.ADMIN_HELD, "admin", "Administrator", EscapeBehavior.BACK_TO_PARENT, true, true, true, true, true, true, true,
                "Administrator tools remain a permissioned layer over normal inspection/action surfaces."));
        register(new WindowSpec(WindowKind.HINTS_HELD, "hints", "Guidance Hints", EscapeBehavior.BACK_TO_PARENT, true, true, true, false, false, true, false,
                "Non-intrusive searchable/clickable guidance anchors to existing game concepts and selections."));
    }

    void register(WindowSpec spec) {
        if (spec == null) return;
        specs.put(spec.id, spec);
        states.putIfAbsent(spec.id, new RuntimeWindowState(spec));
    }

    RuntimeWindowState open(String id, int turn, String context) {
        RuntimeWindowState s = state(id);
        if (s == null) return null;
        s.state = LifecycleState.OPEN;
        s.openedTurn = Math.max(0, turn);
        s.context = clean(context, "opened");
        openEvents++;
        focus(id, turn, s.context);
        return s;
    }

    RuntimeWindowState focus(String id, int turn, String context) {
        RuntimeWindowState s = state(id);
        if (s == null) return null;
        for (RuntimeWindowState other : states.values()) {
            if (other != s && other.state == LifecycleState.FOCUSED) other.state = LifecycleState.BACKGROUND;
        }
        s.state = LifecycleState.FOCUSED;
        s.focusTurn = Math.max(0, turn);
        s.context = clean(context, "focused");
        focusedWindowId = s.spec.id;
        focusEvents++;
        return s;
    }

    RuntimeWindowState close(String id, int turn, String context) {
        RuntimeWindowState s = state(id);
        if (s == null) return null;
        s.state = LifecycleState.CLOSED;
        s.closeTurn = Math.max(0, turn);
        s.context = clean(context, "closed");
        if (s.spec.id.equals(focusedWindowId)) focusedWindowId = "";
        closeEvents++;
        return s;
    }

    RuntimeWindowState state(String id) {
        if (id == null) return null;
        return states.get(id);
    }

    Collection<WindowSpec> specs() {
        return Collections.unmodifiableCollection(specs.values());
    }

    String focusedWindowId() {
        return focusedWindowId;
    }

    String migrationChecklist() {
        StringBuilder sb = new StringBuilder();
        for (WindowSpec spec : specs.values()) {
            if (sb.length() > 0) sb.append("\n");
            sb.append("- ").append(spec.compactLine()).append(" :: ").append(spec.migrationNotes);
        }
        return sb.toString();
    }

    String auditSummary() {
        EnumMap<LifecycleState, Integer> byState = new EnumMap<>(LifecycleState.class);
        int transferReady = 0;
        int progressReady = 0;
        int filterReady = 0;
        for (RuntimeWindowState s : states.values()) {
            byState.put(s.state, byState.getOrDefault(s.state, 0) + 1);
            if (s.spec.supportsInventoryTransfer) transferReady++;
            if (s.spec.supportsProgressBars) progressReady++;
            if (s.spec.supportsSearchOrFilter) filterReady++;
        }
        return "universalWindowAuthority version=" + VERSION + " specs=" + specs.size()
                + " focused=" + clean(focusedWindowId, "none")
                + " events[open=" + openEvents + ",focus=" + focusEvents + ",close=" + closeEvents + "]"
                + " capabilities[transfer=" + transferReady + ",progress=" + progressReady + ",filter=" + filterReady + "]"
                + " states=" + byState;
    }

    private static String clean(String text, String fallback) {
        return text == null || text.isBlank() ? fallback : text.trim();
    }
}
