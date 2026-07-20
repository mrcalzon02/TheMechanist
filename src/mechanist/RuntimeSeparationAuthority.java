package mechanist;

import java.util.*;

/** Defines the current launcher/client/server separation vocabulary without starting those runtimes. */
final class RuntimeSeparationAuthority {
    static final String VERSION = "runtime-separation-" + BuildIdentityAuthority.version();

    static String auditSummary(RuntimeProfile profile) {
        RuntimeProfile p = profile == null ? RuntimeProfile.defaultProfile() : profile;
        return "authority=" + VERSION
                + " requested=" + p.requestedMode
                + " effective=" + p.effectiveMode
                + " launcherShell=implemented"
                + " launcherPackageVerification=implemented"
                + " client=active"
                + " headlessServer=implemented"
                + " singlePlayerInternalServer=not-yet-supervised"
                + " remoteGameplaySession=not-yet-certified"
                + " modManifest=declared-only"
                + " hotRestart=not-yet-implemented";
    }

    static java.util.List<String> infopediaLines(RuntimeProfile profile) {
        RuntimeProfile p = profile == null ? RuntimeProfile.defaultProfile() : profile;
        ArrayList<String> out = new ArrayList<>();
        out.add("Runtime Separation Authority " + VERSION);
        out.add("Purpose: report the current launcher, client, and server process boundaries without claiming unfinished session behavior.");
        out.add("The thin launcher verifies package manifests, applies package selections, prepares profile identity, and starts the client package.");
        out.add("The client owns Java2D rendering, input, panels, presentation state, and local UI feedback.");
        out.add("The packaged headless server owns its separate server-storage namespace and can initialize or bind an independent host transport.");
        out.add("Single-player does not yet supervise a separate internal server process, and a packaged client-to-independent-host gameplay session is not yet certified.");
        out.add("Shared runtime profiles carry mode, save/world identity, mod manifest path, and enabled mod tokens without activating external mod loading.");
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
        out.add("launcherOrchestration=package-verification-and-client-launch");
        out.add("headlessServer=packaged-and-bind-capable");
        out.add("singlePlayerInternalServer=false");
        out.add("remoteGameplaySessionCertified=false");
        out.add("modResolution=false");
        out.add("processRestartHandoff=false");
        out.add("plainJavacCompileRequired=true");
        out.add("profile=" + p.compactLine());
        return out;
    }

    private RuntimeSeparationAuthority() {}
}
