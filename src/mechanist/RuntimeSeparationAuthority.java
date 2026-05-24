package mechanist;

import java.util.*;

/** Defines the current launcher/client/server separation vocabulary without starting those runtimes. */
final class RuntimeSeparationAuthority {
    static final String VERSION = "runtime-separation-0.9.10fc";

    static String auditSummary(RuntimeProfile profile) {
        RuntimeProfile p = profile == null ? RuntimeProfile.defaultProfile() : profile;
        return "authority=" + VERSION
                + " requested=" + p.requestedMode
                + " effective=" + p.effectiveMode
                + " launcherShell=defined"
                + " client=active"
                + " headlessServer=not-yet-implemented"
                + " modManifest=declared-only"
                + " hotRestart=not-yet-implemented";
    }

    static java.util.List<String> infopediaLines(RuntimeProfile profile) {
        RuntimeProfile p = profile == null ? RuntimeProfile.defaultProfile() : profile;
        ArrayList<String> out = new ArrayList<>();
        out.add("Runtime Separation Authority " + VERSION);
        out.add("Purpose: define the launcher/client/server vocabulary before implementation splits the desktop application apart.");
        out.add("Launcher shell owns profile selection, save/mod-set selection, dependency validation, and eventual restart handoff.");
        out.add("Client owns Java2D rendering, input, panels, presentation state, and local UI feedback.");
        out.add("Server authority will own world state, ticking rules, save/load authority, and multiplayer/session truth once that gate opens.");
        out.add("Shared runtime profile carries mode, save/world identity, mod manifest path, and enabled mod tokens without loading mods yet.");
        out.add("Current build behavior: the desktop client remains the effective runtime; server, hot restart, and mod-source resolution are declared boundaries only.");
        out.add("");
        out.addAll(p.lines());
        return out;
    }

    static java.util.List<String> auditLines(RuntimeProfile profile) {
        RuntimeProfile p = profile == null ? RuntimeProfile.defaultProfile() : profile;
        ArrayList<String> out = new ArrayList<>();
        out.add("Runtime Separation Audit");
        out.add(auditSummary(p));
        out.add("clientRenderInputFoundation=present");
        out.add("runtimeProfileEnvelope=present");
        out.add("commandLineModeVocabulary=present");
        out.add("launcherOrchestration=false");
        out.add("headlessServer=false");
        out.add("modResolution=false");
        out.add("processRestartHandoff=false");
        out.add("plainJavacCompileRequired=true");
        out.add("profile=" + p.compactLine());
        return out;
    }

    private RuntimeSeparationAuthority() {}
}
