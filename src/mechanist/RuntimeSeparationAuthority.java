package mechanist;

import java.util.ArrayList;

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
                + " independentHostServerTokenPersistence=atomic-sha256-only"
                + " independentHostHostedSessionCommands=ready-presence-chat-state"
                + " independentHostHostedSessionCommandOrdering=per-connection-monotonic"
                + " independentHostHostedRoster=immutable-deterministic"
                + " independentHostHostedRosterBroadcasts=authenticated-peer-control-frames"
                + " independentHostHostedStatePersistence=lifetime-command-accounting"
                + " independentHostRosterClient=canonical-connected-only"
                + " independentHostClientSupervisor=handshake-token-roster-command-relay"
                + " independentHostClientTokenCustody=atomic-owner-only-plaintext"
                + " independentHostClientTokenDiagnostics=redacted"
                + " independentHostClientReconnect=host-restart-smoke-gated"
                + " independentHostRemoteEntry=explicit-remote-client-mode"
                + " independentHostRemoteLobby=player-facing-editable"
                + " independentHostRemoteLobbyStorage=user-mutable-outside-install"
                + " independentHostRemoteLobbyInternalHost=not-mounted"
                + " independentHostClientWorldCommandApi=not-implemented"
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
        out.add("The ordinary client owns Java2D rendering, input, panels, presentation state, and local UI feedback.");
        out.add("Single-player mounts a supervised in-process internal host that routes world mutation through one authoritative world thread, local session identity, sector authority, and immutable snapshots.");
        out.add("Client shutdown closes the internal host, sector schedules, session state, and authoritative world executor before process exit.");
        out.add("The separately packaged headless server owns its own storage namespace and can bind an exact-address bounded relay transport.");
        out.add("An independent relay client must participate in identity, manifest delivery, acquisition confirmation, restart completion, and a server-issued integrity challenge before receiving RELAY_ONLY access.");
        out.add("After authentication, the host assigns a stable player id and resume token. A disconnected client can recover the same session only with that token, while simultaneous duplicate attachment and invalid tokens are rejected.");
        out.add("The server stores remote identity, connection generation, lifetime relay accounting, and accepted hosted-session command counts atomically in its dedicated namespace. Only SHA-256 resume-token hashes are written on the server; restored sessions begin offline and require the original client token.");
        out.add("The supervised client owns handshake progression, hosted-command and relay sequencing, canonical connected-only roster parsing, asynchronous control-frame dispatch, reconnect, and shutdown. It exposes no movement, combat, inventory, world-snapshot, or gameplay-command API.");
        out.add("Because the client must present the reusable resume token during reconnect, it stores that plaintext credential only in its protected mutable profile using required atomic replacement and owner-only permissions where supported. Status and diagnostic text never include the token.");
        out.add("REMOTE_CLIENT mode opens a dedicated player-facing lobby window with editable host, port, server key, and profile fields; readiness, presence, and typing controls; a connected-only roster; a bounded relay console; and interrogatable session status.");
        out.add("The remote lobby writes mutable state outside the installation and never constructs GamePanel, the single-player internal host, or any remote world authority.");
        out.add("The host owns a deliberately narrow pre-world lobby authority: ordered readiness, presence, and chat-state commands plus immutable deterministic roster snapshots. Stale readiness, presence, and typing state are reset when a connection or host process ends.");
        out.add("Authenticated peers receive authoritative roster control frames when another client joins, changes hosted-lobby state, disconnects, or resumes. These asynchronous MECH control frames are not SEQ relay payloads and do not consume or grant gameplay command sequence authority.");
        out.add("Clean host restart certification preserves player identity, advances connection generation, keeps immutable snapshot versions monotonic, preserves lifetime hosted-command accounting, and rejects corrupted or world-mismatched ledgers before binding.");
        out.add("Authenticated relay access does not initialize a remote world snapshot, grant movement or combat authority, mutate inventory or position, process world simulation commands, or persist a hosted game world. Unsupported world verbs are rejected at the hosted-session boundary.");
        out.add("A packaged client-to-independent-host authoritative gameplay session is therefore not certified and remains distinct from the local single-player host.");
        out.add("Shared runtime profiles carry mode, save/world identity, mod manifest path, enabled mod tokens, and remote endpoint defaults without activating external mod loading.");
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
        out.add("independentHostServerResumeTokenStorage=sha256-only");
        out.add("independentHostClientResumeTokenStorage=owner-only-plaintext");
        out.add("independentHostClientResumeTokenAtomicMove=true");
        out.add("independentHostClientResumeTokenInDiagnostics=false");
        out.add("independentHostCorruptClientTokenAccepted=false");
        out.add("independentHostCorruptLedgerBind=false");
        out.add("independentHostSessionPersistenceAcrossProcessRestart=true");
        out.add("independentHostHostedSessionCommands=true");
        out.add("independentHostHostedSessionCommandVocabulary=ready,presence,chat-state");
        out.add("independentHostHostedSessionCommandOrdering=per-connection-monotonic");
        out.add("independentHostHostedSessionRoster=immutable-deterministic");
        out.add("independentHostHostedSessionRosterVisibility=connected-only");
        out.add("independentHostOfflineResumeIdentityVisibleToPeers=false");
        out.add("independentHostHostedSessionRosterBroadcasts=authenticated-peers");
        out.add("independentHostHostedRosterJoinBroadcast=true");
        out.add("independentHostHostedRosterCommandBroadcast=true");
        out.add("independentHostHostedRosterDisconnectBroadcast=true");
        out.add("independentHostHostedRosterResumeBroadcast=true");
        out.add("independentHostRosterControlFramesUseRelaySequence=false");
        out.add("independentHostRosterClientAuthority=canonical-fail-closed");
        out.add("independentHostRosterVisiblePlayerLimit=64");
        out.add("independentHostClientSupervisor=true");
        out.add("independentHostClientHandshakeOwnership=true");
        out.add("independentHostClientHostedCommandSequencing=true");
        out.add("independentHostClientRelayDispatch=true");
        out.add("independentHostClientHostRestartResume=true");
        out.add("independentHostRemoteClientEntryPoint=true");
        out.add("independentHostRemoteLobbyWindow=true");
        out.add("independentHostRemoteLobbyEditableConnection=true");
        out.add("independentHostRemoteLobbyMutableStorageOutsideInstall=true");
        out.add("independentHostRemoteLobbyGamePanelMounted=false");
        out.add("independentHostRemoteLobbyInternalHostMounted=false");
        out.add("independentHostClientWorldCommandApi=false");
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

    private RuntimeSeparationAuthority() { }
}
