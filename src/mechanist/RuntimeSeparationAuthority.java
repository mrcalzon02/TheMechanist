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
                + " independentHostTransport=handshake-and-exact-bind-smoke-gated"
                + " independentHostHandshake=client-driven-integrity-challenge"
                + " independentHostSessionLedger=server-owned-persistent"
                + " independentHostStablePlayerIdentity=implemented"
                + " independentHostReconnect=resume-token-host-restart-smoke-gated"
                + " independentHostSessionSnapshots=immutable-monotonic"
                + " independentHostSessionPersistence=atomic-hash-only"
                + " independentHostHostedSessionCommands=ready-presence-chat-state"
                + " independentHostHostedSessionCommandOrdering=per-connection-monotonic"
                + " independentHostHostedRoster=immutable-deterministic"
                + " independentHostHostedStatePersistence=lifetime-command-accounting"
                + " independentHostWorldCommands=rejected"
                + " independentHostAccess=relay-only"
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
        out.add("An independent relay client must participate in identity, manifest delivery, acquisition confirmation, restart completion, and a server-issued integrity challenge before receiving RELAY_ONLY access.");
        out.add("After authentication, the host assigns a stable player id and resume token. A disconnected client can recover the same session only with that token, while simultaneous duplicate attachment and invalid tokens are rejected.");
        out.add("Remote session identity, connection generation, lifetime relay accounting, and accepted hosted-session command counts are stored atomically in the dedicated server namespace. Only SHA-256 resume-token hashes are written; restored sessions always begin offline and require the original client token.");
        out.add("The host now owns a deliberately narrow pre-world lobby authority: ordered readiness, presence, and chat-state commands plus immutable deterministic roster snapshots. Stale readiness, presence, and typing state are reset when a connection or host process ends.");
        out.add("Clean host restart certification preserves player identity, advances connection generation, keeps immutable snapshot versions monotonic, preserves lifetime hosted-command accounting, and rejects corrupted or world-mismatched ledgers before binding.");
        out.add("Authenticated relay access does not initialize a remote world snapshot, grant movement or combat authority, mutate inventory or position, process world simulation commands, or persist a hosted game world. Unsupported world verbs are rejected at the hosted-session boundary.");
        out.add("A packaged client-to-independent-host authoritative gameplay session is therefore not certified and remains distinct from the local single-player host.");
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
        out.add("independentHostTransport=authenticated-bounded-relay-only");
        out.add("independentHostHandshake=client-driven-integrity-challenge");
        out.add("independentHostPreAuthenticationRelay=false");
        out.add("independentHostExactBind=required");
        out.add("independentHostSessionLedger=server-owned-persistent");
        out.add("independentHostStablePlayerIdentity=true");
        out.add("independentHostResumeTokenContinuity=true");
        out.add("independentHostDuplicateAttachment=false");
        out.add("independentHostSessionSnapshots=immutable-monotonic");
        out.add("independentHostAtomicPersistence=true");
        out.add("independentHostResumeTokenStorage=sha256-only");
        out.add("independentHostCorruptLedgerBind=false");
        out.add("independentHostSessionPersistenceAcrossProcessRestart=true");
        out.add("independentHostHostedSessionCommands=true");
        out.add("independentHostHostedSessionCommandVocabulary=ready,presence,chat-state");
        out.add("independentHostHostedSessionCommandOrdering=per-connection-monotonic");
        out.add("independentHostHostedSessionRoster=immutable-deterministic");
        out.add("independentHostHostedSessionStatePersistence=lifetime-accounting-only");
        out.add("independentHostStaleHostedLivenessRestored=false");
        out.add("independentHostUnsupportedWorldCommandsAccepted=false");
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
