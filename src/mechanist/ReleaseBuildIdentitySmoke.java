package mechanist;

import java.util.Properties;

/** Proves build identity resolution and release-facing runtime wording remain coherent. */
final class ReleaseBuildIdentitySmoke {
    public static void main(String[] args) throws Exception {
        Properties maven = new Properties();
        maven.setProperty("version", "3.4.5m");

        BuildIdentityAuthority.Resolution explicit =
                BuildIdentityAuthority.resolve(" 1.2.3a ", "2.0.0", maven);
        require("1.2.3a".equals(explicit.version()), "explicit build version should win");
        require("system-property".equals(explicit.source()), "explicit source should be reported");

        BuildIdentityAuthority.Resolution implementation =
                BuildIdentityAuthority.resolve("", " 2.0.0b ", maven);
        require("2.0.0b".equals(implementation.version()), "manifest version should be second");
        require("jar-manifest".equals(implementation.source()), "manifest source should be reported");

        BuildIdentityAuthority.Resolution pom =
                BuildIdentityAuthority.resolve(null, null, maven);
        require("3.4.5m".equals(pom.version()), "Maven properties should be third");

        BuildIdentityAuthority.Resolution fallback =
                BuildIdentityAuthority.resolve(null, null, new Properties());
        require("development".equals(fallback.version()), "development fallback should be honest");

        require(!BuildIdentityAuthority.version().isBlank(), "runtime build version must not be blank");
        require(BuildIdentityAuthority.clientWindowTitle().contains(BuildIdentityAuthority.version()),
                "window title must use the shared build version");
        require(BuildIdentityAuthority.componentVersion("server").contains(BuildIdentityAuthority.version()),
                "server identity must use the shared build version");

        String separation = RuntimeSeparationAuthority.auditSummary(RuntimeProfile.defaultProfile());
        require(separation.contains("headlessServer=implemented"),
                "runtime separation must acknowledge the packaged headless server");
        require(separation.contains("singlePlayerInternalServer=supervised-in-process"),
                "runtime separation must report the supervised local internal host");
        require(separation.contains("independentHostSessionLedger=server-owned-persistent"),
                "runtime separation must report the persistent remote session ledger");
        require(separation.contains("independentHostReconnect=resume-token-host-restart-smoke-gated"),
                "runtime separation must report host-restart reconnect continuity");
        require(separation.contains("independentHostSessionPersistence=atomic-hash-only"),
                "runtime separation must report atomic hash-only session persistence");
        require(separation.contains("independentHostHostedSessionCommands=ready-presence-chat-state"),
                "runtime separation must report the narrow hosted-session command vocabulary");
        require(separation.contains("independentHostHostedSessionCommandOrdering=per-connection-monotonic"),
                "runtime separation must report independent hosted-command ordering");
        require(separation.contains("independentHostHostedRoster=immutable-deterministic"),
                "runtime separation must report immutable hosted rosters");
        require(separation.contains("independentHostHostedRosterBroadcasts=authenticated-peer-control-frames"),
                "runtime separation must report authenticated peer roster broadcasts");
        require(separation.contains("independentHostWorldCommands=rejected"),
                "runtime separation must report explicit world-command rejection");
        require(separation.contains("independentHostWorldAuthority=not-implemented"),
                "runtime separation must keep remote world authority closed");
        require(separation.contains("remoteGameplaySession=not-yet-certified"),
                "runtime separation must not overclaim independent-host gameplay certification");
        require(SinglePlayerInternalHostSupervisor.auditSummary().contains("shutdown=supervised"),
                "internal-host authority must own shutdown");

        RemoteSessionLedgerAuthoritySmoke.main(args);
        HostedRosterClientAuthoritySmoke.main(args);
        IndependentHostHostedSessionWireSmoke.main(args);
        IndependentHostClientSupervisorSmoke.main(args);
        System.out.println("ReleaseBuildIdentitySmoke PASS " + BuildIdentityAuthority.auditSummary());
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private ReleaseBuildIdentitySmoke() { }
}
