package mechanist;

import java.nio.file.Path;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Properties;
import java.util.Random;

/** End-to-end packaged smoke for the supervised in-process single-player host. */
final class SinglePlayerInternalHostLifecycleSmoke {
    public static void main(String[] args) throws Exception {
        System.setProperty("java.awt.headless", "true");
        String configuredRoot = System.getProperty(GameStorageManager.OVERRIDE_PROPERTY, "").trim();
        require(!configuredRoot.isBlank(),
                "single-player lifecycle smoke requires -D" + GameStorageManager.OVERRIDE_PROPERTY);
        Path expectedRoot = Path.of(configuredRoot).toAbsolutePath().normalize();
        require(GameStorageManager.get().rootDir().equals(expectedRoot),
                "single-player lifecycle smoke did not receive its isolated storage root");

        RuntimeProfile profile = RuntimeProfile.fromArgs(new String[] {"--internal-server"});
        Path checkpoint = expectedRoot.resolve("installed-world-checkpoint.properties");
        if (args.length == 1 && "--verify-existing-save".equals(args[0])) {
            verifyExisting(profile, checkpoint);
            return;
        }
        require(args.length == 0, "unknown lifecycle smoke arguments");
        require(!Files.exists(checkpoint), "refusing to replace an existing world checkpoint");
        require(!Files.exists(SaveSlotSurfaceApi.savePathForSlot(1)),
                "creation requires an empty isolated save slot");
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
            first.gainXp("Mechanics", 24, "installed persistence checkpoint");
            first.addInventoryItem("Machine part", ItemProvenanceRecord.found(
                    "Machine part", first.world, first.turn, "installed persistence checkpoint"));
            World visited = visitedFloor(first);
            boolean carved = false;
            outer: for (int x = 1; x < visited.w - 1; x++) for (int y = 1; y < visited.h - 1; y++) {
                if (visited.tiles[x][y] == '#' && visited.tiles[x + 1][y] == '.') {
                    visited.tiles[x][y] = '.';
                    carved = true;
                    break outer;
                }
            }
            require(carved, "visited-floor persistence fixture needs an excavatable wall");
            visited.npcs.clear();
            first.writeSaveFile(1, false);
            Properties receipt = state(first);
            receipt.setProperty("save.sha256", hash(Files.readAllBytes(SaveSlotSurfaceApi.savePathForSlot(1))));
            try (var out = Files.newOutputStream(checkpoint, StandardOpenOption.CREATE_NEW)) {
                receipt.store(out, "Immutable installed-world checkpoint");
            }
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
        verifyExisting(profile, checkpoint);

        System.out.println("SinglePlayerInternalHostLifecycleSmoke PASS root=" + expectedRoot
                + " turn=" + savedTurn
                + " worldTurn=" + savedWorldTurn
                + " character=" + savedCharacter);
    }

    private static void verifyExisting(RuntimeProfile profile, Path checkpoint) throws Exception {
        Properties expected = new Properties();
        try (var in = Files.newInputStream(checkpoint)) { expected.load(in); }
        require("1".equals(expected.getProperty("checkpoint.schema")), "unsupported checkpoint");
        Path slot = SaveSlotSurfaceApi.savePathForSlot(1);
        String before = hash(Files.readAllBytes(slot));
        require(before.equals(expected.getProperty("save.sha256")), "original save bytes changed");
        GamePanel game = new GamePanel(profile);
        if (game.timer != null) game.timer.stop();
        SinglePlayerInternalHostSupervisor host = SinglePlayerInternalHostSupervisor.mount(game, profile);
        try {
            require(game.loadSaveSlot(1, "existing installed-world checkpoint"), "existing world could not load");
            host.refreshNow("existing installed-world checkpoint");
            require(host.active() && host.bridge().sessionAuthority() != null,
                    "existing world did not rebind its authoritative host");
            Properties actual = state(game);
            expected.remove("save.sha256");
            require(actual.equals(expected), "resumed world state differs: expected=" + expected + " actual=" + actual);
            game.setSize(960, 600);
            var frame = new java.awt.image.BufferedImage(960, 600, java.awt.image.BufferedImage.TYPE_INT_RGB);
            var graphics = frame.createGraphics();
            try { game.paint(graphics); } finally { graphics.dispose(); }
            require(game.getClientProperty("mechanist.render.failure") == null,
                    "saved world could not render after resume");
        } finally {
            game.shutdownRuntime();
            host.close();
        }
        require(game.singlePlayerSectorBridge == null, "existing world host did not shut down");
        require(before.equals(hash(Files.readAllBytes(slot))), "read-only resume rewrote the original save");
        System.out.println("SinglePlayerInternalHostLifecycleSmoke EXISTING_SAVE_PASS sha256=" + before);
    }

    private static Properties state(GamePanel game) throws Exception {
        Properties result = new Properties();
        result.setProperty("checkpoint.schema", "1");
        result.setProperty("seed", Long.toString(game.seed));
        result.setProperty("turn", Integer.toString(game.turn));
        result.setProperty("worldTurn", Long.toString(game.worldTurn));
        result.setProperty("character", game.active.name);
        result.setProperty("player", game.playerX + "," + game.playerY);
        result.setProperty("worldSize", game.world.w + "," + game.world.h);
        result.setProperty("tiles.sha256", terrainHash(game.world));
        World visited = visitedFloor(game);
        result.setProperty("visitedFloor5.tiles.sha256", terrainHash(visited));
        result.setProperty("visitedFloor5.npcs", Integer.toString(visited.npcs.size()));
        result.setProperty("xp", Integer.toString(game.xp));
        result.setProperty("knowledgeProgress", Integer.toString(game.knowledgeXpProgress));
        result.setProperty("knowledgeCredits", Integer.toString(game.knowledgeCredits));
        result.setProperty("itemIds", new java.util.TreeSet<>(game.itemInstances.keySet()).toString());
        java.util.TreeSet<String> provenance = new java.util.TreeSet<>();
        game.itemInstances.forEach((id, item) -> {
            if (item.provenance != null) provenance.add(id + ":" + item.provenance.unitId);
        });
        result.setProperty("provenance", provenance.toString());
        result.setProperty("baseObjects", Integer.toString(game.baseObjects.size()));
        return result;
    }

    private static String hash(byte[] bytes) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    }

    private static World visitedFloor(GamePanel game) {
        WorldAtlas atlas = game.atlas;
        World visited = atlas.slices[atlas.sectorX-1][atlas.sectorY-1][atlas.zoneX-1][atlas.zoneY-1][8];
        if (visited == null) {
            atlas.createSlice(atlas.sectorX, atlas.sectorY, atlas.zoneX, atlas.zoneY, 5, false);
            visited = atlas.slices[atlas.sectorX-1][atlas.sectorY-1][atlas.zoneX-1][atlas.zoneY-1][8];
        }
        return visited;
    }

    private static String terrainHash(World world) throws Exception {
        MessageDigest tiles = MessageDigest.getInstance("SHA-256");
        for (char[] column : world.tiles) for (char tile : column) {
            tiles.update((byte) (tile >>> 8)); tiles.update((byte) tile);
        }
        return HexFormat.of().formatHex(tiles.digest());
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private SinglePlayerInternalHostLifecycleSmoke() { }
}
