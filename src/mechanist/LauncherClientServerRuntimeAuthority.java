package mechanist;

import java.util.ArrayList;
import java.util.List;

final class LauncherClientServerRuntimeAuthority {
    static final String VERSION = "launcher-client-server-0.9.10hu";

    private final RuntimeProfile profile;
    private boolean launcherShellActive = true;
    private boolean clientRuntimeActive = true;
    private boolean localServerPrepared = false;
    private boolean localServerAuthoritative = false;
    private String lastAction = "Launcher shell initialized around the current desktop client.";

    LauncherClientServerRuntimeAuthority(RuntimeProfile profile) {
        this.profile = profile == null ? RuntimeProfile.defaultProfile() : profile;
        if (this.profile.internalServerRequested || this.profile.requestedMode == ApplicationRuntimeMode.LOCAL_SERVER) {
            prepareLocalServer("startup profile requested local server");
        }
    }

    String prepareLocalServer(String reason) {
        localServerPrepared = true;
        localServerAuthoritative = false;
        lastAction = "Local headless-server boundary prepared for client handoff; sector-authoritative simulation authority is available. reason=" + safe(reason);
        DebugLog.audit("LAUNCHER_CLIENT_SERVER", statusLine());
        return "Local runtime prepared: client active, internal server boundary ready, sector-authoritative simulation authority available.";
    }

    String startLocalAuthoritativeServer(String reason) {
        localServerPrepared = true;
        localServerAuthoritative = true;
        lastAction = "Desktop single-player now routes world turns through the local sector-authoritative runtime. Public hosting and joining remain closed. reason=" + safe(reason);
        DebugLog.audit("LAUNCHER_CLIENT_SERVER", statusLine());
        return "Local authoritative sector runtime active for single-player; network hosting and joining remain closed.";
    }

    String openModsSurface() {
        lastAction = "Mods/tools surface requested; native Swing simulation editor suite and mod deployment manager are available.";
        DebugLog.audit("MODS_TOOLS_SURFACE", statusLine() + " " + SimulationEditorSuite.auditSummary());
        return "Mods/tools route opened: simulation editor suite, undoable property forms, mod scope binding, zip export, and Steam wrapper detection are available.";
    }

    String openMultiplayerSurface() {
        lastAction = "Multiplayer surface requested from launcher shell; connection history, favorites, direct endpoint parsing, Steam detection, and local host binding are available.";
        DebugLog.audit("MULTIPLAYER_SURFACE", statusLine() + " " + MultiplayerHostBindingService.auditSummary());
        return "Multiplayer surface opened: direct endpoints, recent servers, favorites, Steam-environment detection, and local host binding are available.";
    }

    String statusLine() {
        return "authority=" + VERSION
                + " " + compactStateLine()
                + " requestedMode=" + profile.requestedMode
                + " effectiveMode=" + profile.effectiveMode;
    }

    String compactStateLine() {
        return "client=" + (clientRuntimeActive ? "on" : "off")
                + " | server boundary=" + (localServerPrepared ? "ready" : "idle")
                + " | server tick=" + (localServerAuthoritative ? "on" : "off");
    }

    List<String> infopediaLines() {
        ArrayList<String> out = new ArrayList<>();
        out.add("Launcher / Client / Server Runtime Authority " + VERSION);
        out.add("Launcher shell: active as the main-menu shell around the current desktop client.");
        out.add("Client runtime: active; owns Swing/Java2D rendering, input, UI, and local presentation.");
        out.add("Local server boundary: " + (localServerPrepared ? "prepared" : "idle") + "; sector-authoritative runtime is " + (localServerAuthoritative ? "active for single-player turns" : "available but idle") + ".");
        out.add("Sector simulation: " + SectorManager.auditSummary());
        out.add("Mods/tools surface: active in-game editor route owned by the main client UI; external live mod hot-loading remains closed.");
        out.add("Multiplayer surface: direct endpoint history, favorites, Steam detection, configurable ports, and local host binding are active; real Steam relay awaits a verified wrapper adapter.");
        out.add("Last action: " + lastAction);
        return out;
    }

    static String auditSummary() {
        return "authority=" + VERSION + " shell=active client=current-runtime localServer=single-player-authoritative-sector-runtime mods=ingame-editor+zip-export multiplayer=menu+host-binding+native-relay sectorSim=" + SectorManager.VERSION;
    }

    private static String safe(String s) { return s == null ? "unspecified" : s.replace('\n', ' '); }
}
