package mechanist;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/** Current main-menu launcher-shell state; owns route labels/status without starting future systems. */
final class LauncherShellStateAuthority {
    static final String VERSION = "launcher-shell-state-0.9.10hs";

    enum Route {
        START,
        LOAD_AUTO,
        OPTIONS,
        EXIT,
        MODS,
        MULTIPLAYER,
        INFOPEDIA,
        PROFILE
    }

    static final class RouteDescriptor {
        final Route route;
        final String label;
        final String tip;
        final boolean primaryColumn;

        RouteDescriptor(Route route, String label, String tip, boolean primaryColumn) {
            this.route = route;
            this.label = label == null ? route.name() : label;
            this.tip = tip == null ? "" : tip;
            this.primaryColumn = primaryColumn;
        }
    }

    private final RuntimeProfile profile;
    private Route focusedRoute = Route.START;
    private String routeStatus = "Launcher shell idle: choose a world, profile, route, or local-runtime action.";
    private String lastReason = "initialized";
    private long routeTransitions = 0L;

    LauncherShellStateAuthority(RuntimeProfile profile) {
        this.profile = profile == null ? RuntimeProfile.defaultProfile() : profile;
    }

    List<RouteDescriptor> menuRoutes(UserProfileAuthority.Profile userProfile) {
        String profileLabel = "PROFILE";
        if (userProfile != null && userProfile.displayName != null && !userProfile.displayName.isBlank()) {
            profileLabel = "PROFILE: " + userProfile.displayName;
        }
        ArrayList<RouteDescriptor> out = new ArrayList<>();
        out.add(new RouteDescriptor(Route.START, "START", "Prepare the local client/server boundary, then select or generate a arcology world.", true));
        out.add(new RouteDescriptor(Route.LOAD_AUTO, "LOAD", "Open manual saves and autosave slots.", true));
        out.add(new RouteDescriptor(Route.OPTIONS, "OPTIONS", "Open performance, input, graphics, and profile options.", true));
        out.add(new RouteDescriptor(Route.EXIT, "EXIT", "Exit The Mechanist.", true));
        out.add(new RouteDescriptor(Route.MODS, "EDITOR / MODS", "Open the native Swing simulation editor suite and mod packaging exporter.", false));
        out.add(new RouteDescriptor(Route.MULTIPLAYER, "MULTIPLAYER", "Open the launcher network route. This run remains local single-player.", false));
        out.add(new RouteDescriptor(Route.INFOPEDIA, "INFOPEDIA", "Open the underhive reference atlas and recorded rules.", false));
        out.add(new RouteDescriptor(Route.PROFILE, profileLabel, "Open the detected wrapper profile or local operator identity.", false));
        return Collections.unmodifiableList(out);
    }

    String activate(Route route, String reason) {
        focusedRoute = route == null ? Route.START : route;
        lastReason = sanitize(reason);
        routeTransitions++;
        routeStatus = explanationFor(focusedRoute);
        DebugLog.audit("LAUNCHER_SHELL_STATE", statusLine());
        return routeStatus;
    }

    Route focusedRoute() { return focusedRoute; }

    String statusLine() {
        return "authority=" + VERSION
                + " focusedRoute=" + focusedRoute
                + " transitions=" + routeTransitions
                + " requestedMode=" + profile.requestedMode
                + " effectiveMode=" + profile.effectiveMode
                + " lastReason=" + lastReason;
    }

    List<String> displayLines(LauncherClientServerRuntimeAuthority runtime, UserProfileAuthority.Profile userProfile) {
        ArrayList<String> out = new ArrayList<>();
        out.add("Route: " + human(focusedRoute) + " | mode " + profile.effectiveMode + ".");
        if (runtime != null) out.add("Runtime: " + runtime.compactStateLine());
        if (userProfile != null) out.add("Profile: " + userProfile.displayName + " // " + userProfile.shortId() + ".");
        out.add("Routes: local client active; editor/mod suite active; network host binding available.");
        return out;
    }

    List<String> infopediaLines(LauncherClientServerRuntimeAuthority runtime, UserProfileAuthority.Profile userProfile) {
        ArrayList<String> out = new ArrayList<>();
        out.add("Launcher Shell State Authority " + VERSION);
        out.add("Purpose: keep the main menu as a launcher-facing state shell instead of direct scattered button behavior.");
        out.add("Current focused route: " + focusedRoute + ".");
        out.add("Transitions this run: " + routeTransitions + ".");
        out.add("Last reason: " + lastReason + ".");
        out.add("Current status: " + routeStatus);
        out.add("Runtime boundary: " + (runtime == null ? "unavailable" : runtime.compactStateLine()));
        out.add("Profile surface: " + (userProfile == null ? "not detected" : userProfile.provider + " / " + userProfile.displayName + " / " + userProfile.shortId()));
        out.add("Active tools: native Swing simulation editor suite, undoable forms, zip mod export, and Steam wrapper detection. Closed gates: live mod hot-loading, public matchmaking, and broad client-side simulation worker-threading.");
        return out;
    }

    static String auditSummary() {
        return "authority=" + VERSION + " menuState=extracted routes=start+load+options+exit+mods-tools+multiplayer+infopedia+profile editorSuite=true zoneAudit=true liveMods=false multiplayer=menu+local-bind authoritativeServer=sector-boundary";
    }

    private static String explanationFor(Route route) {
        if (route == null) route = Route.START;
        switch (route) {
            case START:
                return "Start route selected: local client remains active while the internal server boundary is prepared before world selection.";
            case LOAD_AUTO:
                return "Load route selected: open the persistence surface with manual saves and autosaves; no save migration is required for this test line.";
            case OPTIONS:
                return "Options route selected: graphics, input, profile, and display settings are handled before runtime separation deepens.";
            case EXIT:
                return "Exit route selected: runtime teardown should happen only for real application close, not controlled display reconfiguration.";
            case MODS:
                return "Mods/tools route selected: native simulation editor suite, property panels, mod scope binding, and deployment manager are available.";
            case MULTIPLAYER:
                return "Multiplayer route selected: direct join preparation, server favorites, Steam detection, and local host binding are available from the launcher surface.";
            case INFOPEDIA:
                return "Infopedia route selected: reference surfaces expose current authorities and audits without mutating runtime content.";
            case PROFILE:
                return "Profile route selected: wrapper identity or internal SHA profile identity is visible to the launcher shell.";
            default:
                return "Launcher shell route selected.";
        }
    }

    private static String human(Route route) {
        String raw = route == null ? "START" : route.name();
        return raw.toLowerCase(Locale.ROOT).replace('_', ' ');
    }

    private static String sanitize(String s) {
        if (s == null || s.trim().isEmpty()) return "unspecified";
        return s.replace('\n', ' ').trim();
    }
}
