package mechanist;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFilePermission;
import java.security.SecureRandom;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

/**
 * Minimal headless world-command authority for the independent host.
 *
 * This is the first remote command slice connected to the same
 * WorldCommandRequest and AuthoritativeWorldRuntime used by desktop
 * single-player. Only WaitCommand is open. Movement, interaction, combat,
 * inventory, zone transitions, and admin commands remain rejected until a
 * headless map/runtime context owns those operations.
 */
final class IndependentHostTurnAuthority implements AutoCloseable {
    static final String VERSION = "independent-host-turn-authority-1";
    private static final String PERSISTENCE_SCHEMA = "1";
    private static final int MAX_PERSISTED_PLAYERS = 10_000;
    private static final int MAX_RECENT_EVENTS = 12;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final String worldId;
    private final Path persistenceFile;
    private final SectorKey stagingSector;
    private final SectorManager sectorManager;
    private final AuthoritativeWorldRuntime worldRuntime;
    private final Map<String, MutablePlayerState> players = new LinkedHashMap<>();
    private final Object commandLock = new Object();

    private long worldTurn;
    private long acceptedCommands;
    private boolean closed;

    IndependentHostTurnAuthority(String worldId) {
        this(worldId, null);
    }

    IndependentHostTurnAuthority(
            String worldId,
            Path persistenceFile
    ) {
        this.worldId = safeToken(worldId, "remote-world");
        this.persistenceFile = persistenceFile == null
                ? null
                : persistenceFile.toAbsolutePath().normalize();
        this.stagingSector = SectorKey.of(
                1,
                0,
                0,
                0,
                "independent-host-staging-" + cleanForPath(this.worldId));
        this.sectorManager = new SectorManager();
        this.worldRuntime = new AuthoritativeWorldRuntime(
                "mechanist-independent-host-world-" + cleanForPath(this.worldId));
        try {
            loadIfPresent();
        } catch (IOException | RuntimeException failure) {
            worldRuntime.close();
            sectorManager.close();
            throw new IllegalStateException(
                    "independent-host turn authority could not restore persistence",
                    failure);
        }
    }

    TurnCommandResult applyCommand(
            String authenticatedPlayerId,
            long connectionGeneration,
            long commandId,
            WorldCommandRequest command
    ) {
        synchronized (commandLock) {
            requireOpen();
            String playerId = safePlayerId(authenticatedPlayerId);
            if (connectionGeneration < 1L) {
                throw new IllegalArgumentException(
                        "connection generation must be positive");
            }
            if (commandId < 0L) {
                throw new IllegalArgumentException(
                        "world command id must be non-negative");
            }
            if (command == null) {
                throw new IllegalArgumentException(
                        "world command is required");
            }
            if (!playerId.equals(command.playerId())) {
                throw new SecurityException(
                        "world command player does not match authenticated session");
            }
            if (!(command instanceof WaitCommand)) {
                throw new IllegalArgumentException(
                        command.getClass().getSimpleName()
                                + " is unavailable; only authoritative wait is open");
            }

            MutablePlayerState existing = players.get(playerId);
            MutablePlayerState prior = existing == null
                    ? null
                    : existing.copy();
            long priorWorldTurn = worldTurn;
            long priorAcceptedCommands = acceptedCommands;
            boolean created = existing == null;
            MutablePlayerState state = existing;
            if (state == null) {
                state = new MutablePlayerState(playerId);
                state.connectionGeneration = connectionGeneration;
                players.put(playerId, state);
            } else if (connectionGeneration < state.connectionGeneration) {
                throw new SecurityException(
                        "world command used a stale connection generation");
            } else if (connectionGeneration > state.connectionGeneration) {
                state.connectionGeneration = connectionGeneration;
                state.lastConnectionCommandId = -1L;
                state.lastEvent = "connection generation advanced";
            }

            long expected = state.lastConnectionCommandId + 1L;
            if (commandId != expected) {
                rollback(
                        playerId,
                        prior,
                        priorWorldTurn,
                        priorAcceptedCommands,
                        created);
                throw new IllegalStateException(
                        "world command sequence mismatch: incoming="
                                + commandId
                                + " expected="
                                + expected);
            }

            MutablePlayerState activeState = state;
            HeadlessCommandContext context =
                    new HeadlessCommandContext(activeState);
            String rejection = command.rejectionReason(context);
            if (rejection != null && !rejection.isBlank()) {
                rollback(
                        playerId,
                        prior,
                        priorWorldTurn,
                        priorAcceptedCommands,
                        created);
                throw new IllegalArgumentException(rejection);
            }

            HeadlessSnapshotSource snapshotSource =
                    new HeadlessSnapshotSource(activeState);
            try {
                AuthoritativeWorldSnapshot authoritative =
                        worldRuntime.submitAndJoin(
                                snapshotSource,
                                playerId,
                                stagingSector,
                                command.reason(),
                                () -> {
                                    sectorManager.playerEnteredSector(
                                            playerId,
                                            stagingSector);
                                    command.apply(context);
                                    activeState.lastConnectionCommandId =
                                            commandId;
                                    activeState.acceptedCommands++;
                                    acceptedCommands++;
                                    activeState.lastEvent =
                                            "authoritative wait accepted";
                                    try {
                                        persist();
                                    } catch (IOException failure) {
                                        throw new UncheckedIOException(failure);
                                    }
                                    SectorSnapshot sector =
                                            sectorManager.snapshot(stagingSector);
                                    return sector == null
                                            ? sectorManager.ensureSector(stagingSector)
                                            : sector;
                                });
                TurnSnapshot snapshot = snapshot(activeState, authoritative);
                return new TurnCommandResult(
                        commandId,
                        "WAIT",
                        snapshot,
                        authoritative);
            } catch (RuntimeException failure) {
                rollback(
                        playerId,
                        prior,
                        priorWorldTurn,
                        priorAcceptedCommands,
                        created);
                throw new IllegalStateException(
                        "independent-host world command could not commit atomically",
                        failure);
            }
        }
    }

    TurnSnapshot snapshotForPlayer(String playerId) {
        synchronized (commandLock) {
            requireOpen();
            MutablePlayerState state = players.get(
                    safePlayerId(playerId));
            return state == null
                    ? null
                    : snapshot(state, worldRuntime.latestSnapshot());
        }
    }

    void disconnectPlayer(String playerId) {
        synchronized (commandLock) {
            if (closed) return;
            String use = safePlayerId(playerId);
            sectorManager.playerLeftCurrentSector(use);
        }
    }

    long worldTurn() {
        synchronized (commandLock) {
            return worldTurn;
        }
    }

    long acceptedCommands() {
        synchronized (commandLock) {
            return acceptedCommands;
        }
    }

    int playerCount() {
        synchronized (commandLock) {
            return players.size();
        }
    }

    Path persistenceFile() {
        return persistenceFile;
    }

    boolean persistenceEnabled() {
        return persistenceFile != null;
    }

    String auditSummary() {
        synchronized (commandLock) {
            return "authority=" + VERSION
                    + " world=" + worldId
                    + " players=" + players.size()
                    + " worldTurn=" + worldTurn
                    + " acceptedCommands=" + acceptedCommands
                    + " openCommands=wait"
                    + " movementAuthority=false"
                    + " mapAuthority=false"
                    + " inventoryAuthority=false"
                    + " combatAuthority=false"
                    + " persistence="
                    + (persistenceFile == null
                    ? "disabled"
                    : persistenceFile)
                    + " lane={"
                    + worldRuntime.statusLine()
                    + "}";
        }
    }

    @Override
    public void close() {
        synchronized (commandLock) {
            if (closed) return;
            closed = true;
        }
        worldRuntime.close();
        sectorManager.close();
    }

    private void rollback(
            String playerId,
            MutablePlayerState prior,
            long priorWorldTurn,
            long priorAcceptedCommands,
            boolean created
    ) {
        worldTurn = priorWorldTurn;
        acceptedCommands = priorAcceptedCommands;
        try {
            sectorManager.playerLeftCurrentSector(playerId);
        } catch (RuntimeException ignored) {
        }
        if (created || prior == null) {
            players.remove(playerId);
        } else {
            players.put(playerId, prior);
        }
    }

    private TurnSnapshot snapshot(
            MutablePlayerState state,
            AuthoritativeWorldSnapshot authoritative
    ) {
        WorldSnapshot worldSnapshot = authoritative == null
                ? null
                : authoritative.worldSnapshot();
        long version = authoritative == null
                ? worldRuntime.worldVersion()
                : authoritative.version();
        return new TurnSnapshot(
                version,
                worldId,
                state.playerId,
                state.connectionGeneration,
                state.lastConnectionCommandId,
                state.turn,
                worldTurn,
                state.acceptedCommands,
                acceptedCommands,
                state.lastEvent,
                worldSnapshot,
                authoritative == null
                        ? "none"
                        : authoritative.mutationThread(),
                System.currentTimeMillis());
    }

    private void loadIfPresent() throws IOException {
        if (persistenceFile == null || !Files.exists(persistenceFile)) {
            return;
        }
        if (!Files.isRegularFile(persistenceFile)) {
            throw new IOException(
                    "independent-host turn authority path is not a regular file: "
                            + persistenceFile);
        }
        Properties properties = new Properties();
        try (InputStream input = Files.newInputStream(persistenceFile)) {
            properties.load(input);
        }
        String schema = properties.getProperty("schema", "").trim();
        if (!PERSISTENCE_SCHEMA.equals(schema)) {
            throw new IOException(
                    "unsupported independent-host turn schema: "
                            + schema);
        }
        String storedWorld = requiredProperty(
                properties,
                "worldId");
        if (!worldId.equals(storedWorld)) {
            throw new IOException(
                    "independent-host turn world mismatch: expected "
                            + worldId
                            + " but found "
                            + storedWorld);
        }
        worldTurn = parseNonNegativeLong(
                properties.getProperty("worldTurn"),
                "worldTurn");
        acceptedCommands = parseNonNegativeLong(
                properties.getProperty("acceptedCommands"),
                "acceptedCommands");
        int count = parsePlayerCount(
                properties.getProperty("player.count"));
        long summedCommands = 0L;
        for (int index = 0; index < count; index++) {
            String prefix = "player." + index + ".";
            String playerId = safePlayerId(
                    requiredProperty(
                            properties,
                            prefix + "playerId"));
            if (players.containsKey(playerId)) {
                throw new IOException(
                        "duplicate independent-host turn player: "
                                + playerId);
            }
            MutablePlayerState state =
                    new MutablePlayerState(playerId);
            state.connectionGeneration = Math.max(
                    1L,
                    parseNonNegativeLong(
                            properties.getProperty(
                                    prefix + "connectionGeneration"),
                            prefix + "connectionGeneration"));
            state.lastConnectionCommandId =
                    parseSignedSequence(
                            properties.getProperty(
                                    prefix + "lastConnectionCommandId"),
                            prefix + "lastConnectionCommandId");
            state.turn = parseNonNegativeLong(
                    properties.getProperty(prefix + "turn"),
                    prefix + "turn");
            state.acceptedCommands = parseNonNegativeLong(
                    properties.getProperty(
                            prefix + "acceptedCommands"),
                    prefix + "acceptedCommands");
            state.lastEvent = safeEvent(
                    properties.getProperty(
                            prefix + "lastEvent",
                            "restored after server restart"));
            summedCommands += state.acceptedCommands;
            players.put(playerId, state);
        }
        if (summedCommands != acceptedCommands) {
            throw new IOException(
                    "independent-host turn command accounting mismatch: players="
                            + summedCommands
                            + " global="
                            + acceptedCommands);
        }
        setOwnerOnlyPermissions(persistenceFile);
    }

    private void persist() throws IOException {
        if (persistenceFile == null) return;
        Path parent = persistenceFile.getParent();
        if (parent == null) {
            throw new IOException(
                    "independent-host turn persistence path has no parent: "
                            + persistenceFile);
        }
        Files.createDirectories(parent);
        Properties properties = new Properties();
        properties.setProperty("schema", PERSISTENCE_SCHEMA);
        properties.setProperty("worldId", worldId);
        properties.setProperty(
                "worldTurn",
                Long.toString(worldTurn));
        properties.setProperty(
                "acceptedCommands",
                Long.toString(acceptedCommands));
        List<MutablePlayerState> ordered =
                new ArrayList<>(players.values());
        ordered.sort(Comparator.comparing(
                state -> state.playerId));
        properties.setProperty(
                "player.count",
                Integer.toString(ordered.size()));
        for (int index = 0; index < ordered.size(); index++) {
            MutablePlayerState state = ordered.get(index);
            String prefix = "player." + index + ".";
            properties.setProperty(
                    prefix + "playerId",
                    state.playerId);
            properties.setProperty(
                    prefix + "connectionGeneration",
                    Long.toString(state.connectionGeneration));
            properties.setProperty(
                    prefix + "lastConnectionCommandId",
                    Long.toString(state.lastConnectionCommandId));
            properties.setProperty(
                    prefix + "turn",
                    Long.toString(state.turn));
            properties.setProperty(
                    prefix + "acceptedCommands",
                    Long.toString(state.acceptedCommands));
            properties.setProperty(
                    prefix + "lastEvent",
                    safeEvent(state.lastEvent));
        }

        Path temporary = parent.resolve(
                persistenceFile.getFileName()
                        + ".tmp-"
                        + randomHex(8));
        try {
            try (FileChannel channel = FileChannel.open(
                         temporary,
                         StandardOpenOption.CREATE_NEW,
                         StandardOpenOption.WRITE);
                 OutputStream output = new BufferedOutputStream(
                         Channels.newOutputStream(channel))) {
                setOwnerOnlyPermissions(temporary);
                properties.store(
                        output,
                        "The Mechanist independent-host authoritative wait/turn ledger");
                output.flush();
                channel.force(true);
            }
            Files.move(
                    temporary,
                    persistenceFile,
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);
            setOwnerOnlyPermissions(persistenceFile);
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private void requireOpen() {
        if (closed) {
            throw new IllegalStateException(
                    "independent-host turn authority is closed");
        }
    }

    private static int parsePlayerCount(
            String value
    ) throws IOException {
        long parsed = parseNonNegativeLong(
                value,
                "player.count");
        if (parsed > MAX_PERSISTED_PLAYERS) {
            throw new IOException(
                    "independent-host turn ledger exceeds maximum player count "
                            + MAX_PERSISTED_PLAYERS);
        }
        return (int) parsed;
    }

    private static long parseNonNegativeLong(
            String value,
            String label
    ) throws IOException {
        if (value == null || value.isBlank()) return 0L;
        try {
            long parsed = Long.parseLong(value.trim());
            if (parsed < 0L) {
                throw new NumberFormatException("negative");
            }
            return parsed;
        } catch (NumberFormatException failure) {
            throw new IOException(
                    "independent-host turn ledger contains invalid "
                            + label,
                    failure);
        }
    }

    private static long parseSignedSequence(
            String value,
            String label
    ) throws IOException {
        if (value == null || value.isBlank()) return -1L;
        try {
            long parsed = Long.parseLong(value.trim());
            if (parsed < -1L) {
                throw new NumberFormatException("below -1");
            }
            return parsed;
        } catch (NumberFormatException failure) {
            throw new IOException(
                    "independent-host turn ledger contains invalid "
                            + label,
                    failure);
        }
    }

    private static String requiredProperty(
            Properties properties,
            String key
    ) throws IOException {
        String value = properties.getProperty(key, "").trim();
        if (value.isBlank()) {
            throw new IOException(
                    "independent-host turn ledger is missing "
                            + key);
        }
        if (value.length() > 4096
                || value.indexOf('|') >= 0
                || value.indexOf('\n') >= 0
                || value.indexOf('\r') >= 0) {
            throw new IOException(
                    "independent-host turn ledger contains unsafe "
                            + key);
        }
        return value;
    }

    private static String safePlayerId(String value) {
        String playerId = Objects.requireNonNullElse(
                value,
                "").trim().toLowerCase(Locale.ROOT);
        if (!playerId.matches("remote-[a-f0-9]{20}")) {
            throw new IllegalArgumentException(
                    "invalid server-owned remote player id");
        }
        return playerId;
    }

    private static String safeToken(
            String value,
            String fallback
    ) {
        String use = Objects.requireNonNullElse(
                value,
                fallback).trim();
        if (use.isBlank()) use = fallback;
        if (use.length() > 240
                || use.indexOf('|') >= 0
                || use.indexOf('\n') >= 0
                || use.indexOf('\r') >= 0) {
            throw new IllegalArgumentException(
                    "unsafe independent-host turn token");
        }
        return use;
    }

    private static String safeEvent(String value) {
        String use = Objects.requireNonNullElse(
                value,
                "unspecified")
                .replace('|', '/')
                .replace('\n', ' ')
                .replace('\r', ' ')
                .trim();
        if (use.isBlank()) use = "unspecified";
        return use.substring(
                0,
                Math.min(240, use.length()));
    }

    private static String cleanForPath(String value) {
        String cleaned = safeToken(
                value,
                "remote-world")
                .replaceAll("[^A-Za-z0-9._-]", "_");
        return cleaned.substring(
                0,
                Math.min(100, cleaned.length()));
    }

    private static String randomHex(int bytes) {
        byte[] data = new byte[Math.max(1, bytes)];
        RANDOM.nextBytes(data);
        return java.util.HexFormat.of().formatHex(data);
    }

    private static void setOwnerOnlyPermissions(Path path) {
        if (path == null) return;
        try {
            Files.setPosixFilePermissions(
                    path,
                    EnumSet.of(
                            PosixFilePermission.OWNER_READ,
                            PosixFilePermission.OWNER_WRITE));
        } catch (UnsupportedOperationException | IOException ignored) {
        }
    }

    record TurnCommandResult(
            long commandId,
            String command,
            TurnSnapshot snapshot,
            AuthoritativeWorldSnapshot authoritativeSnapshot
    ) {
    }

    record TurnSnapshot(
            long version,
            String worldId,
            String playerId,
            long connectionGeneration,
            long lastConnectionCommandId,
            long playerTurn,
            long worldTurn,
            long acceptedPlayerCommands,
            long acceptedWorldCommands,
            String lastEvent,
            WorldSnapshot worldSnapshot,
            String mutationThread,
            long committedAtMillis
    ) {
        String compactLine() {
            return "v=" + version
                    + " world=" + worldId
                    + " player=" + playerId
                    + " generation=" + connectionGeneration
                    + " commandId=" + lastConnectionCommandId
                    + " playerTurn=" + playerTurn
                    + " worldTurn=" + worldTurn
                    + " playerCommands=" + acceptedPlayerCommands
                    + " worldCommands=" + acceptedWorldCommands
                    + " thread=" + mutationThread
                    + " event=" + lastEvent;
        }
    }

    private final class HeadlessCommandContext
            implements WorldCommandRuntimeContext {
        private final MutablePlayerState state;

        private HeadlessCommandContext(
                MutablePlayerState state
        ) {
            this.state = state;
        }

        @Override
        public boolean mounted() {
            return true;
        }

        @Override
        public World world() {
            return null;
        }

        @Override
        public boolean inBounds(int x, int y) {
            return false;
        }

        @Override
        public boolean walkable(int x, int y) {
            return false;
        }

        @Override
        public void movePlayer(
                int dx,
                int dy,
                String source
        ) {
            unsupported("movement");
        }

        @Override
        public void waitOneTurn(String line) {
            state.turn++;
            worldTurn++;
            state.events.addLast(safeEvent(line));
            while (state.events.size() > MAX_RECENT_EVENTS) {
                state.events.removeFirst();
            }
        }

        @Override
        public void settleAfterNoMove(String reason) {
        }

        @Override
        public void clearPendingMovement(String reason) {
        }

        @Override
        public void confirmInteraction() {
            unsupported("interaction");
        }

        @Override
        public void confirmCombatTarget() {
            unsupported("combat");
        }

        @Override
        public void useSelectedInventoryItem() {
            unsupported("inventory");
        }

        @Override
        public void unequipSelectedEquipmentSlot() {
            unsupported("equipment");
        }

        @Override
        public void addImperialScript(int amount) {
            unsupported("economy");
        }

        @Override
        public void logEvent(String line) {
            state.events.addLast(safeEvent(line));
            while (state.events.size() > MAX_RECENT_EVENTS) {
                state.events.removeFirst();
            }
        }

        @Override
        public void advanceTurn(String line) {
            waitOneTurn(line);
        }

        @Override
        public void teleportPlayer(
                int x,
                int y,
                String reason
        ) {
            unsupported("teleport");
        }

        @Override
        public void spawnInventoryItem(
                String item,
                int count
        ) {
            unsupported("inventory");
        }

        private void unsupported(String capability) {
            throw new UnsupportedOperationException(
                    capability
                            + " is not mounted in the independent host turn authority");
        }
    }

    private final class HeadlessSnapshotSource
            implements AuthoritativeWorldRuntime.SnapshotSource {
        private final MutablePlayerState state;

        private HeadlessSnapshotSource(
                MutablePlayerState state
        ) {
            this.state = state;
        }

        @Override
        public WorldSnapshot worldSnapshot(
                long version,
                SectorKey sector
        ) {
            PlayerSnapshot player = new PlayerSnapshot(
                    state.playerId,
                    state.playerId,
                    0,
                    0,
                    state.turn,
                    worldTurn,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    "E",
                    "stationary",
                    "none");
            return new WorldSnapshot(
                    version,
                    sector,
                    player,
                    List.of(),
                    List.of(),
                    List.of(),
                    List.copyOf(state.events),
                    new UiStateSnapshot(
                            "REMOTE_HOST",
                            "TURN_AUTHORITY",
                            0,
                            0,
                            false,
                            "independent-host-staging",
                            0,
                            "none"),
                    System.currentTimeMillis());
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
            return new AuthoritativeWorldSnapshot(
                    version,
                    playerId,
                    sector,
                    safeEvent(reason),
                    state.turn,
                    worldTurn,
                    0,
                    0,
                    "REMOTE_HOST",
                    "independent-host-staging",
                    0,
                    state.events.size(),
                    state.playerId,
                    "none",
                    worldSnapshot,
                    mutationThread,
                    sectorSnapshot,
                    System.currentTimeMillis());
        }
    }

    private static final class MutablePlayerState {
        private final String playerId;
        private final ArrayDeque<String> events =
                new ArrayDeque<>();
        private long connectionGeneration = 1L;
        private long lastConnectionCommandId = -1L;
        private long turn;
        private long acceptedCommands;
        private String lastEvent = "no authoritative command accepted";

        private MutablePlayerState(String playerId) {
            this.playerId = playerId;
        }

        private MutablePlayerState copy() {
            MutablePlayerState copy =
                    new MutablePlayerState(playerId);
            copy.connectionGeneration = connectionGeneration;
            copy.lastConnectionCommandId =
                    lastConnectionCommandId;
            copy.turn = turn;
            copy.acceptedCommands = acceptedCommands;
            copy.lastEvent = lastEvent;
            copy.events.addAll(events);
            return copy;
        }
    }
}
