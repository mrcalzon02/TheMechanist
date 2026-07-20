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
                + " independentHostClientSupervisor=handshake-token-roster-command-relay-wait"
                + " independentHostClientTokenCustody=atomic-owner-only-plaintext"
                + " independentHostClientTokenDiagnostics=redacted"
                + " independentHostClientReconnect=host-restart-smoke-gated"
                + " independentHostRemoteEntry=explicit-remote-client-mode"
                + " independentHostRemoteLobby=player-facing-editable"
                + " independentHostRemoteLobbyStorage=user-mutable-outside-install"
                + " independentHostRemoteLobbyInternalHost=not-mounted"
                + " independentHostTurnAuthority=headless-persistent-smoke-gated"
                + " independentHostTurnCommand=wait-only-network-exposed"
                + " independentHostClientGenericWorldCommandApi=not-implemented"
                + " independentHostNetworkWorldCommands=wait-only"
                + " independentHostTransportAccess=relay-only"
                + " independentHostControlAccess=authenticated-wait-only"
                + " independentHostWorldAuthority=wait-turn-only"
                + " independentHostMovementAuthority=false"
                + " independentHostMapAuthority=false"
                + " independentHostFullWorldAuthority=false"
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
        out.add("An independent client must participate in identity, manifest delivery, acquisition confirmation, restart completion, and a server-issued integrity challenge before receiving transport access or the wait-only control capability.");
        out.add("The ACCESS frame continues to label the transport lane RELAY_ONLY. A separate authenticated WORLD_COMMAND control lane accepts only WAIT; the transport label does not grant movement, map, combat, inventory, or general gameplay authority.");
        out.add("After authentication, the host assigns a stable player id and resume token. A disconnected client can recover the same session only with that token, while simultaneous duplicate attachment and invalid tokens are rejected.");
        out.add("The server stores remote identity, connection generation, lifetime relay accounting, accepted hosted-session command counts, and wait-turn state atomically in dedicated server namespaces. Only SHA-256 resume-token hashes are written on the server; restored sessions begin offline and require the original client token.");
        out.add("The supervised client owns handshake progression, hosted-command, relay, and wait-command sequencing, canonical connected-only roster parsing, asynchronous control-frame dispatch, reconnect, and shutdown. It exposes one dedicated waitAuthoritativeTurn operation but no generic world-command, movement, map, combat, inventory, or world-snapshot request API.");
        out.add("Because the client must present the reusable resume token during reconnect, it stores that plaintext credential only in its protected mutable profile using required atomic replacement and owner-only permissions where supported. Status and diagnostic text never include the token.");
        out.add("REMOTE_CLIENT mode opens a dedicated player-facing lobby window with editable host, port, server key, and profile fields; readiness, presence, and typing controls; a connected-only roster; a bounded relay console; a Wait / Advance Turn control; and interrogatable player/world turn counters.");
        out.add("The remote lobby writes mutable state outside the installation and never constructs GamePanel or the single-player internal host.");
        out.add("The headless remote turn authority reuses the same WorldCommandRequest and AuthoritativeWorldRuntime single-writer lane as desktop single-player. Its only open command is WaitCommand, with exact per-connection ordering, immutable snapshots, atomic persistence, clean-restart continuity, corruption rejection, and authenticated network exposure.");
        out.add("The wait control increments persisted player and global world turns. It does not initialize or deliver a remote map, move a character, resolve interaction or combat, mutate inventory or economy, teleport, or expose arbitrary commands.");
        out.add("The host also owns a narrow lobby authority: ordered readiness, presence, and chat-state commands plus immutable deterministic roster snapshots. Stale readiness, presence, and typing state are reset when a connection or host process ends.");
        out.add("Authenticated peers receive authoritative roster control frames when another client joins, changes hosted-lobby state, disconnects, or resumes. These asynchronous MECH control frames are separate from SEQ relay payloads and from the independently ordered wait-command lane.");
        out.add("Clean host restart certification preserves player identity, advances connection generation, preserves lifetime hosted-command accounting and persisted wait turns, resets per-connection command ids, and rejects corrupted or world-mismatched ledgers before binding.");
        out.add("A packaged client-to-independent-host full gameplay session is not certified. Movement, maps, interaction, combat, inventory, position, and general world simulation remain distinct from the available wait-only authority.");
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
        out.add("independentHostTransport=authenticated-bounded-relay-plus-wait-control");
        out.add("independentHostHandshake=client-driven-integrity-challenge");
        out.add("independentHostPreAuthenticationRelay=false");
        out.add("independentHostPreAuthenticationWait=false");
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
        out.add("independentHostClientWaitCommandSequencing=true");
        out.add("independentHostClientRelayDispatch=true");
        out.add("independentHostClientHostRestartResume=true");
        out.add("independentHostRemoteClientEntryPoint=true");
        out.add("independentHostRemoteLobbyWindow=true");
        out.add("independentHostRemoteLobbyEditableConnection=true");
        out.add("independentHostRemoteLobbyMutableStorageOutsideInstall=true");
        out.add("independentHostRemoteLobbyWaitControl=true");
        out.add("independentHostRemoteLobbyGamePanelMounted=false");
        out.add("independentHostRemoteLobbyInternalHostMounted=false");
        out.add("independentHostTurnAuthority=true");
        out.add("independentHostTurnAuthorityPersistence=atomic");
        out.add("independentHostTurnAuthorityCommand=wait-only");
        out.add("independentHostTurnAuthorityNetworkExposed=true");
        out.add("independentHostTurnAuthorityRestartContinuity=true");
        out.add("independentHostMovementAuthority=false");
        out.add("independentHostMapAuthority=false");
        out.add("independentHostClientGenericWorldCommandApi=false");
        out.add("independentHostHostedSessionStatePersistence=lifetime-accounting-only");
        out.add("independentHostStaleHostedLivenessRestored=false");
        out.add("independentHostUnsupportedWorldCommandsAccepted=false");
        out.add("independentHostWaitAuthorityNetworkExposed=true");
        out.add("independentHostFullWorldAuthority=false");
        out.add("remoteGameplaySessionCertified=false");
        out.add("modResolution=false");
        out.add("processRestartHandoff=false");
        out.add("plainJavacCompileRequired=true");
        out.add("profile=" + p.compactLine());
        return out;
    }

    private RuntimeSeparationAuthority() { }
}
