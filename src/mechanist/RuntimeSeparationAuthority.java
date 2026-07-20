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
                + " singlePlayerInternalServer=supervised-in-process"
                + " singlePlayerAuthority=single-writer-world-lane"
                + " singlePlayerSaveResume=packaged-smoke-gated"
                + " independentHostTransport=exact-bind-relay-smoke-gated"
                + " independentHostWorldAuthority=not-implemented"
                + " remoteGameplaySession=not-yet-certified"
                + " modManifest=declared-only"
                + " hotRestart=not-yet-implemented";
    }

    static java.util.List<String> infopediaLines(RuntimeProfile profile) {
        RuntimeProfile p = profile == null ? RuntimeProfile.defaultProfile() : profile;
        ArrayList<String> out = new ArrayList<>();
        out.add("Runtime Separation Authority " + VERSION);
        out.add("Purpose: report the current launcher, client, and server boundaries without claiming unfinished remote-session behavior.");
        out.add("The thin launcher verifies package manifests, applies package selections, prepares profile identity, and starts the client package.");
        out.add("The client owns Java2D rendering, input, panels, presentation state, and local UI feedback.");
        out.add("Single-player mounts a supervised in-process internal host that routes world mutation through one authoritative world thread, local session identity, sector authority, and immutable snapshots.");
        out.add("Client shutdown closes the internal host, sector schedules, session state, and authoritative world executor before process exit.");
        out.add("The separately packaged headless server owns its own storage namespace and can bind an exact-address bounded relay transport.");
        out.add("The current independent-host transport can be certified for connection, sequencing, bounded frame relay, replay rejection, close, and restart. It does not yet exchange an authoritative world handshake or own remote gameplay state.");
        out.add("A packaged client-to-independent-host gameplay session is therefore not certified and remains distinct from the local single-player host.");
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
        out.add("singlePlayerInternalServer=supervised-in-process");
        out.add("singlePlayerWorldMutation=authoritative-single-writer");
        out.add("singlePlayerSession=local-bound");
        out.add("singlePlayerShutdown=supervised");
        out.add("headlessServer=packaged-and-bind-capable");
        out.add("independentHostTransport=bounded-relay-only");
        out.add("independentHostExactBind=required");
        out.add("independentHostWorldAuthority=false");
        out.add("remoteGameplaySessionCertified=false");
        out.add("modResolution=false");
        out.add("processRestartHandoff=false");
        out.add("plainJavacCompileRequired=true");
        out.add("profile=" + p.compactLine());
        return out;
    }

    private RuntimeSeparationAuthority() {}
}
