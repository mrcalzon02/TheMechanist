package mechanist;

/**
 * Verifies that the authoritative runtime preserves the submitted authenticated
 * player identity through direct snapshot construction, runtime fallback, and
 * GamePanel-backed snapshot construction paths.
 */
final class AuthoritativeWorldRuntimeFallbackPlayerIdentitySmoke {
    public static void main(String[] args) {
        verifyDirectSnapshotFactoryIdentity();
        verifyFallbackSnapshotIdentity();
        verifyPanelSnapshotIdentity();

        System.out.println(
                "AuthoritativeWorldRuntimeFallbackPlayerIdentitySmoke PASS"
                        + " directFactoryPlayerBound=true"
                        + " authenticatedPlayerBound=true"
                        + " worldPlayerBound=true"
                        + " panelPlayerBound=true"
                        + " canonicalSnapshotRetained=true");
    }

    private static void verifyDirectSnapshotFactoryIdentity() {
        String authenticatedPlayer = "direct-authenticated-player";
        WorldSnapshot snapshot = WorldSnapshot.fromGame(
                7L,
                null,
                null,
                authenticatedPlayer);
        require(snapshot != null,
                "direct snapshot factory did not return a snapshot");
        require(snapshot.player() != null,
                "direct snapshot factory did not create player state");
        require(authenticatedPlayer.equals(snapshot.player().id()),
                "direct snapshot factory relabeled or lost authenticated player identity");
        require(snapshot.version() == 7L,
                "direct snapshot factory changed requested world version");
    }

    private static void verifyFallbackSnapshotIdentity() {
        AuthoritativeWorldRuntime.SnapshotSource fallbackSource =
                new AuthoritativeWorldRuntime.SnapshotSource() {
                    @Override
                    public WorldSnapshot worldSnapshot(
                            long version,
                            SectorKey sector
                    ) {
                        return null;
                    }

                    @Override
                    public AuthoritativeWorldSnapshot authoritativeSnapshot(
                            long version,
                            String playerId,
                            SectorKey sector,
                            String reason,
                            SectorSnapshot sectorSnapshot,
                            WorldSnapshot worldSnapshot,
                            String mutationThread
                    ) {
                        return null;
                    }
                };

        try (AuthoritativeWorldRuntime runtime =
                     new AuthoritativeWorldRuntime(
                             "mechanist-fallback-player-identity-smoke")) {
            String authenticatedPlayer = "fallback-authenticated-player";
            AuthoritativeWorldSnapshot snapshot = runtime.submitAndJoin(
                    fallbackSource,
                    authenticatedPlayer,
                    null,
                    "fallback player identity smoke",
                    () -> null);

            requireIdentityBound(
                    runtime,
                    snapshot,
                    authenticatedPlayer,
                    "fallback");
        }
    }

    private static void verifyPanelSnapshotIdentity() {
        try (AuthoritativeWorldRuntime runtime =
                     new AuthoritativeWorldRuntime(
                             "mechanist-panel-player-identity-smoke")) {
            String authenticatedPlayer = "panel-authenticated-player";
            AuthoritativeWorldSnapshot snapshot = runtime.submitAndJoin(
                    (GamePanel) null,
                    authenticatedPlayer,
                    null,
                    "panel player identity smoke",
                    () -> null);

            requireIdentityBound(
                    runtime,
                    snapshot,
                    authenticatedPlayer,
                    "panel");
        }
    }

    private static void requireIdentityBound(
            AuthoritativeWorldRuntime runtime,
            AuthoritativeWorldSnapshot snapshot,
            String authenticatedPlayer,
            String path
    ) {
        require(snapshot != null,
                path + " publication did not return a snapshot");
        require(authenticatedPlayer.equals(snapshot.playerId()),
                path + " authoritative snapshot lost submitted player identity");
        require(snapshot.worldSnapshot() != null,
                path + " publication did not create a world snapshot");
        require(snapshot.worldSnapshot().player() != null,
                path + " world snapshot did not create player state");
        require(authenticatedPlayer.equals(
                        snapshot.worldSnapshot().player().id()),
                path + " world snapshot lost submitted player identity");
        require(runtime.latestSnapshot() == snapshot,
                path + " publication was not retained as latest snapshot");
        require(runtime.latestWorldSnapshot()
                        == snapshot.worldSnapshot(),
                path + " world publication was not retained canonically");
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }
}
