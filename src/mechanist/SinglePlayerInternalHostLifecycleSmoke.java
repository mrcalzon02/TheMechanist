package mechanist;

import java.nio.file.Path;
import java.util.Random;

/** End-to-end packaged smoke for the supervised in-process single-player host. */
final class SinglePlayerInternalHostLifecycleSmoke {
    public static void main(String[] args) {
        System.setProperty("java.awt.headless", "true");
        String configuredRoot = System.getProperty(GameStorageManager.OVERRIDE_PROPERTY, "").trim();
        require(!configuredRoot.isBlank(),
                "single-player lifecycle smoke requires -D" + GameStorageManager.OVERRIDE_PROPERTY);
        Path expectedRoot = Path.of(configuredRoot).toAbsolutePath().normalize();
        require(GameStorageManager.get().rootDir().equals(expectedRoot),
                "single-player lifecycle smoke did not receive its isolated storage root");

        RuntimeProfile profile = RuntimeProfile.fromArgs(new String[] {"--internal-server"});
        long seed = 0x51A6E2026L;
        int savedTurn;
        long savedWorldTurn;
        String savedCharacter;

        GamePanel first = new GamePanel(profile);
        if (first.timer != null) first.timer.stop();
        SinglePlayerInternalHostSupervisor firstHost =
                SinglePlayerInternalHostSupervisor.mount(first, profile);
        try {
            require(firstHost.state() == SinglePlayerInternalHostSupervisor.State.WAITING_FOR_WORLD,
                    "internal host should wait until a world and character exist");

            first.seed = seed;
            Candidate candidate = Candidate.random(new Random(seed ^ 0xC0FFEE51L));
            candidate.name = "Alpha Host Tester";
            first.startPackagedClientNewGameWith(candidate, WorldSetupSettings.standard());
            firstHost.refreshNow("new game ready");

            require(firstHost.active(), "internal host did not become active after new game creation");
            require(first.singlePlayerSectorBridge == firstHost.bridge(),
                    "GamePanel and supervisor do not share one authoritative bridge");
            require(firstHost.bridge().sessionAuthority() != null,
                    "local internal-server session was not bound");

            int beforeTurn = first.turn;
            long beforeWorldTurn = first.worldTurn;
            firstHost.bridge().runAuthoritativeTurn(first, "packaged lifecycle smoke turn", () -> {
                first.turn++;
                first.worldTurn++;
            });
            require(first.turn == beforeTurn + 1, "authoritative host did not commit the local turn");
            require(first.worldTurn == beforeWorldTurn + 1,
                    "authoritative host did not commit world time");
            require(firstHost.bridge().latestSnapshot() != null,
                    "authoritative host did not publish a committed snapshot");

            savedTurn = first.turn;
            savedWorldTurn = first.worldTurn;
            savedCharacter = first.active.name;
            first.writeSaveFile(1, false);
        } finally {
            first.shutdownRuntime();
            firstHost.close();
        }
        require(first.singlePlayerSectorBridge == null,
                "internal host bridge remained mounted after first client shutdown");

        GamePanel resumed = new GamePanel(profile);
        if (resumed.timer != null) resumed.timer.stop();
        SinglePlayerInternalHostSupervisor resumedHost =
                SinglePlayerInternalHostSupervisor.mount(resumed, profile);
        try {
            require(resumed.loadSaveSlot(1, "internal host lifecycle smoke"),
                    "packaged single-player save could not be loaded");
            resumedHost.refreshNow("loaded save ready");

            require(resumedHost.active(), "internal host did not rebind after save load");
            require(resumed.turn == savedTurn, "resumed turn does not match the saved turn");
            require(resumed.worldTurn == savedWorldTurn,
                    "resumed world time does not match the saved world time");
            require(resumed.active != null && savedCharacter.equals(resumed.active.name),
                    "resumed character identity does not match the saved character");
            require(resumedHost.bridge().sessionAuthority() != null,
                    "resumed local session was not rebound");
            require(resumedHost.statusLine().contains("state=ACTIVE"),
                    "internal host health line does not report ACTIVE after resume");
        } finally {
            resumed.shutdownRuntime();
            resumedHost.close();
        }
        require(resumed.singlePlayerSectorBridge == null,
                "internal host bridge remained mounted after resumed client shutdown");

        System.out.println("SinglePlayerInternalHostLifecycleSmoke PASS root=" + expectedRoot
                + " turn=" + savedTurn
                + " worldTurn=" + savedWorldTurn
                + " character=" + savedCharacter);
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private SinglePlayerInternalHostLifecycleSmoke() { }
}
