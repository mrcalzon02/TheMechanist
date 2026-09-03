package mechanist;

/**
 * Verifies that the authoritative runtime preserves the submitted authenticated
 * player identity when a snapshot source delegates world and authoritative
 * snapshot construction back to the runtime fallback path.
 */
final class AuthoritativeWorldRuntimeFallbackPlayerIdentitySmoke {
    public static void main(String[] args) {
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

            require(snapshot != null,
                    "fallback publication did not return a snapshot");
            require(authenticatedPlayer.equals(snapshot.playerId()),
                    "authoritative snapshot lost submitted player identity");
            require(snapshot.worldSnapshot() != null,
                    "fallback publication did not create a world snapshot");
            require(snapshot.worldSnapshot().player() != null,
                    "fallback world snapshot did not create player state");
            require(authenticatedPlayer.equals(
                            snapshot.worldSnapshot().player().id()),
                    "fallback world snapshot lost submitted player identity");
            require(runtime.latestSnapshot() == snapshot,
                    "fallback publication was not retained as latest snapshot");
            require(runtime.latestWorldSnapshot()
                            == snapshot.worldSnapshot(),
                    "fallback world publication was not retained canonically");
        }

        System.out.println(
                "AuthoritativeWorldRuntimeFallbackPlayerIdentitySmoke PASS"
                        + " authenticatedPlayerBound=true"
                        + " worldPlayerBound=true"
                        + " canonicalSnapshotRetained=true");
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }
}
