package mechanist;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/** Server-side per-client snapshot history and fixed-point delta encoder for low-bandwidth replication. */
final class SnapshotDeltaCompressor {
    static final int DEFAULT_HISTORY = 96;
    static final int POSITION_PRECISION = 100; // centimeters if world units are meters/tiles.
    static final int VELOCITY_PRECISION = 1000;
    private static final byte PACKET_VERSION = 1;

    private final int historyCapacity;
    private final Map<String, SnapshotRingBuffer> histories = new ConcurrentHashMap<>();

    SnapshotDeltaCompressor() { this(DEFAULT_HISTORY); }

    SnapshotDeltaCompressor(int historyCapacity) {
        this.historyCapacity = Math.max(8, Math.min(1024, historyCapacity));
    }

    void recordSnapshotForClient(String clientSessionId, WorldStateRecord state) {
        Objects.requireNonNull(state, "state");
        histories.computeIfAbsent(clean(clientSessionId), id -> new SnapshotRingBuffer(historyCapacity)).add(state);
    }

    DeltaPacket encodeDeltaForClient(String clientSessionId, WorldStateRecord current, long lastAckedSequenceId) {
        Objects.requireNonNull(current, "current");
        String client = clean(clientSessionId);
        SnapshotRingBuffer ring = histories.computeIfAbsent(client, id -> new SnapshotRingBuffer(historyCapacity));
        Optional<WorldStateRecord> baseOpt = ring.find(lastAckedSequenceId);
        WorldStateRecord base = baseOpt.orElse(null);
        DeltaPacket packet = base == null ? fullStateDelta(current, lastAckedSequenceId, "base snapshot unavailable") : deltaFromBase(base, current, lastAckedSequenceId);
        ring.add(current);
        return packet;
    }

    Optional<WorldStateRecord> latestForClient(String clientSessionId) {
        SnapshotRingBuffer ring = histories.get(clean(clientSessionId));
        return ring == null ? Optional.empty() : ring.latest();
    }

    void removeClient(String clientSessionId) { histories.remove(clean(clientSessionId)); }

    static DeltaPacket fullStateDelta(WorldStateRecord current, long lastAckedSequenceId, String reason) {
        ArrayList<EntitySpawn> spawns = new ArrayList<>();
        current.entities().values().stream().sorted(Comparator.comparingLong(EntityNetworkState::entityId)).forEach(e -> spawns.add(EntitySpawn.fromState(e)));
        return new DeltaPacket(current.sequenceId(), lastAckedSequenceId, current.serverTimeNanos(), true, List.copyOf(spawns), List.of(), List.of(), reason);
    }

    private static DeltaPacket deltaFromBase(WorldStateRecord base, WorldStateRecord current, long lastAckedSequenceId) {
        Map<Long, EntityNetworkState> oldEntities = base.entities();
        Map<Long, EntityNetworkState> newEntities = current.entities();
        ArrayList<EntitySpawn> spawns = new ArrayList<>();
        ArrayList<EntityDestroy> destroys = new ArrayList<>();
        ArrayList<EntityDelta> deltas = new ArrayList<>();
        HashSet<Long> visited = new HashSet<>();
        for (EntityNetworkState now : newEntities.values()) {
            visited.add(now.entityId());
            EntityNetworkState before = oldEntities.get(now.entityId());
            if (before == null) {
                spawns.add(EntitySpawn.fromState(now));
            } else {
                EntityDelta delta = EntityDelta.between(before, now);
                if (!delta.fields().isEmpty()) deltas.add(delta);
            }
        }
        for (long id : oldEntities.keySet()) {
            if (!visited.contains(id)) destroys.add(new EntityDestroy(id, "missing from current authoritative snapshot"));
        }
        spawns.sort(Comparator.comparingLong(EntitySpawn::entityId));
        destroys.sort(Comparator.comparingLong(EntityDestroy::entityId));
        deltas.sort(Comparator.comparingLong(EntityDelta::entityId));
        return new DeltaPacket(current.sequenceId(), lastAckedSequenceId, current.serverTimeNanos(), false, List.copyOf(spawns), List.copyOf(destroys), List.copyOf(deltas), "delta from base sequence " + base.sequenceId());
    }

    static int quantizePosition(double value) { return clampInt(Math.round(value * POSITION_PRECISION)); }
    static int quantizeVelocity(double value) { return clampInt(Math.round(value * VELOCITY_PRECISION)); }
    static double dequantizePosition(int value) { return value / (double)POSITION_PRECISION; }
    static double dequantizeVelocity(int value) { return value / (double)VELOCITY_PRECISION; }

    static byte[] encodeBinary(DeltaPacket packet) {
        Objects.requireNonNull(packet, "packet");
        ByteArrayOutputStream out = new ByteArrayOutputStream(128 + packet.deltas().size() * 48 + packet.spawns().size() * 64);
        ByteBuffer header = ByteBuffer.allocate(1 + 8 + 8 + 8 + 1 + 2 + 2 + 2).order(ByteOrder.BIG_ENDIAN);
        header.put(PACKET_VERSION).putLong(packet.sequenceId()).putLong(packet.baseSequenceId()).putLong(packet.serverTimeNanos()).put((byte)(packet.fullState() ? 1 : 0));
        header.putShort((short)Math.min(Short.MAX_VALUE, packet.spawns().size()));
        header.putShort((short)Math.min(Short.MAX_VALUE, packet.destroys().size()));
        header.putShort((short)Math.min(Short.MAX_VALUE, packet.deltas().size()));
        out.writeBytes(header.array());
        for (EntitySpawn spawn : packet.spawns()) writeSpawn(out, spawn);
        for (EntityDestroy destroy : packet.destroys()) writeDestroy(out, destroy);
        for (EntityDelta delta : packet.deltas()) writeDelta(out, delta);
        return out.toByteArray();
    }

    static DeltaPacket decodeBinary(byte[] bytes) {
        Objects.requireNonNull(bytes, "bytes");
        if (bytes.length < 30) throw new IllegalArgumentException("delta packet too small: " + bytes.length);
        ByteBuffer buffer = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN);
        byte version = buffer.get();
        if (version != PACKET_VERSION) throw new IllegalArgumentException("unsupported delta packet version " + version);
        long sequence = buffer.getLong();
        long base = buffer.getLong();
        long time = buffer.getLong();
        boolean full = buffer.get() != 0;
        int spawnCount = Short.toUnsignedInt(buffer.getShort());
        int destroyCount = Short.toUnsignedInt(buffer.getShort());
        int deltaCount = Short.toUnsignedInt(buffer.getShort());
        ArrayList<EntitySpawn> spawns = new ArrayList<>(spawnCount);
        ArrayList<EntityDestroy> destroys = new ArrayList<>(destroyCount);
        ArrayList<EntityDelta> deltas = new ArrayList<>(deltaCount);
        for (int i = 0; i < spawnCount; i++) spawns.add(readSpawn(buffer));
        for (int i = 0; i < destroyCount; i++) destroys.add(readDestroy(buffer));
        for (int i = 0; i < deltaCount; i++) deltas.add(readDelta(buffer));
        if (buffer.hasRemaining()) throw new IllegalArgumentException("delta packet contained " + buffer.remaining() + " trailing bytes");
        return new DeltaPacket(sequence, base, time, full, List.copyOf(spawns), List.copyOf(destroys), List.copyOf(deltas), "decoded binary packet");
    }

    private static void writeSpawn(ByteArrayOutputStream out, EntitySpawn spawn) {
        byte[] type = utf8Limit(spawn.entityType(), 64);
        ByteBuffer b = ByteBuffer.allocate(8 + 1 + type.length + 4 * 9 + 4).order(ByteOrder.BIG_ENDIAN);
        b.putLong(spawn.entityId()).put((byte)type.length).put(type)
                .putInt(spawn.qx()).putInt(spawn.qy()).putInt(spawn.qz())
                .putInt(spawn.qvx()).putInt(spawn.qvy()).putInt(spawn.qvz())
                .putInt(spawn.qyaw()).putInt(spawn.qpitch()).putInt(spawn.qroll()).putInt(spawn.stateFlags());
        out.writeBytes(b.array());
    }

    private static EntitySpawn readSpawn(ByteBuffer b) {
        long id = b.getLong();
        int len = Byte.toUnsignedInt(b.get());
        byte[] type = new byte[len];
        b.get(type);
        return new EntitySpawn(id, new String(type, java.nio.charset.StandardCharsets.UTF_8), b.getInt(), b.getInt(), b.getInt(), b.getInt(), b.getInt(), b.getInt(), b.getInt(), b.getInt(), b.getInt(), b.getInt());
    }

    private static void writeDestroy(ByteArrayOutputStream out, EntityDestroy destroy) {
        ByteBuffer b = ByteBuffer.allocate(8).order(ByteOrder.BIG_ENDIAN).putLong(destroy.entityId());
        out.writeBytes(b.array());
    }

    private static EntityDestroy readDestroy(ByteBuffer b) { return new EntityDestroy(b.getLong(), "decoded destroy"); }

    private static void writeDelta(ByteArrayOutputStream out, EntityDelta delta) {
        ByteBuffer b = ByteBuffer.allocate(8 + 1 + delta.fields().size() * 5).order(ByteOrder.BIG_ENDIAN);
        b.putLong(delta.entityId()).put((byte)Math.min(255, delta.fields().size()));
        for (DeltaField f : delta.fields()) b.put(f.code()).putInt(f.value());
        out.writeBytes(b.array());
    }

    private static EntityDelta readDelta(ByteBuffer b) {
        long id = b.getLong();
        int count = Byte.toUnsignedInt(b.get());
        ArrayList<DeltaField> fields = new ArrayList<>(count);
        for (int i = 0; i < count; i++) fields.add(new DeltaField(b.get(), b.getInt()));
        return new EntityDelta(id, List.copyOf(fields));
    }

    private static byte[] utf8Limit(String value, int max) {
        byte[] raw = (value == null ? "" : value).getBytes(java.nio.charset.StandardCharsets.UTF_8);
        if (raw.length <= max) return raw;
        byte[] copy = new byte[max];
        System.arraycopy(raw, 0, copy, 0, max);
        return copy;
    }

    private static int clampInt(long value) {
        if (value > Integer.MAX_VALUE) return Integer.MAX_VALUE;
        if (value < Integer.MIN_VALUE) return Integer.MIN_VALUE;
        return (int)value;
    }

    private static String clean(String id) { return id == null || id.isBlank() ? "anonymous-client" : id.trim(); }

    private static final class SnapshotRingBuffer {
        private final WorldStateRecord[] buffer;
        private int cursor = 0;
        private int size = 0;
        private final ReentrantLock lock = new ReentrantLock();

        SnapshotRingBuffer(int capacity) { this.buffer = new WorldStateRecord[capacity]; }

        void add(WorldStateRecord state) {
            lock.lock();
            try {
                buffer[cursor] = state;
                cursor = (cursor + 1) % buffer.length;
                size = Math.min(size + 1, buffer.length);
            } finally { lock.unlock(); }
        }

        Optional<WorldStateRecord> find(long sequenceId) {
            lock.lock();
            try {
                for (int i = 0; i < size; i++) {
                    WorldStateRecord state = buffer[i];
                    if (state != null && state.sequenceId() == sequenceId) return Optional.of(state);
                }
                return Optional.empty();
            } finally { lock.unlock(); }
        }

        Optional<WorldStateRecord> latest() {
            lock.lock();
            try {
                if (size == 0) return Optional.empty();
                int index = (cursor - 1 + buffer.length) % buffer.length;
                return Optional.ofNullable(buffer[index]);
            } finally { lock.unlock(); }
        }
    }
}

record WorldStateRecord(long sequenceId, long serverTimeNanos, Map<Long, EntityNetworkState> entities) {
    WorldStateRecord {
        if (sequenceId < 0) throw new IllegalArgumentException("sequenceId cannot be negative");
        serverTimeNanos = serverTimeNanos <= 0 ? System.nanoTime() : serverTimeNanos;
        entities = Map.copyOf(Objects.requireNonNullElse(entities, Map.of()));
    }

    static WorldStateRecord of(long sequenceId, Collection<EntityNetworkState> states) {
        LinkedHashMap<Long, EntityNetworkState> map = new LinkedHashMap<>();
        if (states != null) for (EntityNetworkState state : states) if (state != null) map.put(state.entityId(), state);
        return new WorldStateRecord(sequenceId, System.nanoTime(), map);
    }
}

record EntityNetworkState(long entityId, String entityType, double x, double y, double z, double vx, double vy, double vz, double yaw, double pitch, double roll, int stateFlags) {
    EntityNetworkState {
        if (entityId < 0) throw new IllegalArgumentException("entityId cannot be negative");
        entityType = entityType == null || entityType.isBlank() ? "entity" : entityType.trim();
    }
}

record DeltaPacket(long sequenceId, long baseSequenceId, long serverTimeNanos, boolean fullState, List<EntitySpawn> spawns, List<EntityDestroy> destroys, List<EntityDelta> deltas, String reason) {
    DeltaPacket {
        spawns = List.copyOf(Objects.requireNonNullElse(spawns, List.of()));
        destroys = List.copyOf(Objects.requireNonNullElse(destroys, List.of()));
        deltas = List.copyOf(Objects.requireNonNullElse(deltas, List.of()));
        reason = reason == null ? "" : reason;
    }

    int estimatedBinaryBytes() { return SnapshotDeltaCompressor.encodeBinary(this).length; }
}

record EntitySpawn(long entityId, String entityType, int qx, int qy, int qz, int qvx, int qvy, int qvz, int qyaw, int qpitch, int qroll, int stateFlags) {
    static EntitySpawn fromState(EntityNetworkState e) {
        return new EntitySpawn(e.entityId(), e.entityType(), SnapshotDeltaCompressor.quantizePosition(e.x()), SnapshotDeltaCompressor.quantizePosition(e.y()), SnapshotDeltaCompressor.quantizePosition(e.z()), SnapshotDeltaCompressor.quantizeVelocity(e.vx()), SnapshotDeltaCompressor.quantizeVelocity(e.vy()), SnapshotDeltaCompressor.quantizeVelocity(e.vz()), SnapshotDeltaCompressor.quantizePosition(e.yaw()), SnapshotDeltaCompressor.quantizePosition(e.pitch()), SnapshotDeltaCompressor.quantizePosition(e.roll()), e.stateFlags());
    }
}

record EntityDestroy(long entityId, String reason) { }

record EntityDelta(long entityId, List<DeltaField> fields) {
    static EntityDelta between(EntityNetworkState before, EntityNetworkState now) {
        ArrayList<DeltaField> fields = new ArrayList<>(12);
        addIfChanged(fields, DeltaField.X, SnapshotDeltaCompressor.quantizePosition(before.x()), SnapshotDeltaCompressor.quantizePosition(now.x()));
        addIfChanged(fields, DeltaField.Y, SnapshotDeltaCompressor.quantizePosition(before.y()), SnapshotDeltaCompressor.quantizePosition(now.y()));
        addIfChanged(fields, DeltaField.Z, SnapshotDeltaCompressor.quantizePosition(before.z()), SnapshotDeltaCompressor.quantizePosition(now.z()));
        addIfChanged(fields, DeltaField.VX, SnapshotDeltaCompressor.quantizeVelocity(before.vx()), SnapshotDeltaCompressor.quantizeVelocity(now.vx()));
        addIfChanged(fields, DeltaField.VY, SnapshotDeltaCompressor.quantizeVelocity(before.vy()), SnapshotDeltaCompressor.quantizeVelocity(now.vy()));
        addIfChanged(fields, DeltaField.VZ, SnapshotDeltaCompressor.quantizeVelocity(before.vz()), SnapshotDeltaCompressor.quantizeVelocity(now.vz()));
        addIfChanged(fields, DeltaField.YAW, SnapshotDeltaCompressor.quantizePosition(before.yaw()), SnapshotDeltaCompressor.quantizePosition(now.yaw()));
        addIfChanged(fields, DeltaField.PITCH, SnapshotDeltaCompressor.quantizePosition(before.pitch()), SnapshotDeltaCompressor.quantizePosition(now.pitch()));
        addIfChanged(fields, DeltaField.ROLL, SnapshotDeltaCompressor.quantizePosition(before.roll()), SnapshotDeltaCompressor.quantizePosition(now.roll()));
        addIfChanged(fields, DeltaField.FLAGS, before.stateFlags(), now.stateFlags());
        return new EntityDelta(now.entityId(), List.copyOf(fields));
    }

    EntityDelta { fields = List.copyOf(Objects.requireNonNullElse(fields, List.of())); }

    private static void addIfChanged(ArrayList<DeltaField> fields, byte code, int before, int now) { if (before != now) fields.add(new DeltaField(code, now)); }
}

record DeltaField(byte code, int value) {
    static final byte X = 1, Y = 2, Z = 3, VX = 4, VY = 5, VZ = 6, YAW = 7, PITCH = 8, ROLL = 9, FLAGS = 10;
}
