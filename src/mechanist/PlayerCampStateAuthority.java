package mechanist;

import java.io.*;
import java.util.*;

/** Keeps physical camps and storage on their own map while the faction follows its founder. */
final class PlayerCampStateAuthority {
    private static final String PREFIX = "base.camp.v1.";
    private static final Map<GamePanel, Owner> OWNERS = new WeakHashMap<>();
    private record Owner(WorldAtlas atlas, LinkedHashMap<String, Camp> camps) { }
    private record Camp(Properties data, List<BaseObject> liveObjects) { }
    record Home(String world, int room, int x, int y) { }

    static String key(World world) {
        return world.sectorX + "," + world.sectorY + "," + world.zoneX + "," + world.zoneY
                + "," + world.floor + "," + world.sewerLayer;
    }
    static String storage(String key) { return GamePanel.CONTAINER_BASE_STORAGE + "@" + key; }
    private static Owner owner(GamePanel game) {
        Owner owner = OWNERS.get(game);
        if (owner == null || owner.atlas != game.atlas) {
            owner = new Owner(game.atlas, new LinkedHashMap<>());
            OWNERS.put(game, owner);
        }
        return owner;
    }
    static boolean founded(GamePanel game) {
        return game != null && (!game.playerFactionIdentity.isBlank() || home(game) != null);
    }
    static Home home(GamePanel game) {
        if (game == null) return null;
        if (game.baseClaimed) return new Home(game.world == null ? "" : key(game.world),
                game.claimedRoomId, game.baseX, game.baseY);
        for (var entry : new TreeMap<>(owner(game).camps).entrySet()) {
            Properties data = entry.getValue().data;
            if (Boolean.parseBoolean(data.getProperty("base.claimed")))
                return new Home(entry.getKey(), Persistence.getInt(data,"base.room",-1),
                        Persistence.getInt(data,"base.x",-1), Persistence.getInt(data,"base.y",-1));
        }
        return null;
    }
    private static Camp capture(GamePanel game) {
        Properties data = new Properties();
        data.setProperty("base.claimed", Boolean.toString(game.baseClaimed));
        data.setProperty("base.room", Integer.toString(game.claimedRoomId));
        data.setProperty("base.x", Integer.toString(game.baseX));
        data.setProperty("base.y", Integer.toString(game.baseY));
        var objects = new ArrayList<String>();
        for (BaseObject object : game.baseObjects) objects.add(object.saveLine());
        data.setProperty("base.objects", Persistence.encList(objects));
        data.setProperty("run.roomOwnershipHistory", Persistence.encList(RoomOwnershipAuthority.saveHistory(game)));
        var cooldowns = new ArrayList<String>();
        game.scavengeCooldownUntilTurn.forEach((tile, turn) -> cooldowns.add(tile + ":" + turn));
        data.setProperty("scavenge.cooldowns", Persistence.encList(cooldowns));
        return new Camp(data, new ArrayList<>(game.baseObjects));
    }
    static void enter(GamePanel game, World destination) {
        if (game.world == destination) return;
        Owner owner = owner(game);
        String from = key(game.world), to = key(destination);
        Camp incoming = owner.camps.get(to);
        ContainerRecord stored = incoming == null ? null : game.itemContainers.get(storage(to));
        if (incoming != null && stored == null)
            throw new IllegalStateException("Stored camp has no authoritative item container");
        if (stored != null) for (String id : stored.itemInstanceIds) {
            ItemInstance item = game.itemInstances.get(id);
            if (item == null || !stored.id.equals(item.containerId))
                throw new IllegalStateException("Stored camp unit identity is inconsistent");
        }
        List<BaseObject> objects = incoming == null ? List.of()
                : incoming.liveObjects != null ? incoming.liveObjects : Persistence.decodeBaseObjects(incoming.data);
        if (incoming != null && objects.size() != Persistence.decList(incoming.data.getProperty("base.objects", "")).size())
            throw new IllegalStateException("Stored camp construction records are incomplete");
        var cooldowns = new HashMap<Integer,Integer>();
        if (incoming != null) for (String line : Persistence.decList(incoming.data.getProperty("scavenge.cooldowns", ""))) {
            String[] pair = line.split(":",2);
            cooldowns.put(Integer.parseInt(pair[0]), Integer.parseInt(pair[1]));
        }
        for (BaseObject object : objects) if (!object.underConstruction) game.configureBaseObject(object);
        game.rebuildItemContainersFromLegacyLists();
        SaveEfficiencyAuthority.playerFactionIdFor(game, SaveEfficiencyAuthority.playerIdFor(game));
        owner.camps.put(from, capture(game));
        ContainerRecord active = game.itemContainers.get(GamePanel.CONTAINER_BASE_STORAGE);
        ContainerRecord outgoing = new ContainerRecord(storage(from), "Stored camp " + from);
        outgoing.itemInstanceIds.addAll(active.itemInstanceIds);
        for (String id : outgoing.itemInstanceIds) game.itemInstances.get(id).containerId = outgoing.id;
        game.itemContainers.put(outgoing.id, outgoing);
        active.itemInstanceIds.clear();
        game.world = destination;
        game.baseObjects.clear(); game.baseObjects.addAll(objects);
        game.baseStorage.clear();
        if (stored != null) {
            for (String id : stored.itemInstanceIds) {
                ItemInstance item = game.itemInstances.get(id);
                item.containerId = active.id;
                active.itemInstanceIds.add(id);
                game.baseStorage.add(item.displayName);
            }
            game.itemContainers.remove(stored.id);
        }
        Properties data = incoming == null ? new Properties() : incoming.data;
        game.baseClaimed = Boolean.parseBoolean(data.getProperty("base.claimed", "false"));
        game.claimedRoomId = Persistence.getInt(data,"base.room",-1);
        game.baseX = Persistence.getInt(data,"base.x",-1); game.baseY = Persistence.getInt(data,"base.y",-1);
        RoomOwnershipAuthority.restoreHistory(game, Persistence.decList(data.getProperty("run.roomOwnershipHistory", "")));
        game.scavengeCooldownUntilTurn.clear();
        game.scavengeCooldownUntilTurn.putAll(cooldowns);
        game.activeInteractionBaseObject = null;
        game.pendingUnavailableWorkerKey = ""; game.pendingUnavailableWorkerTurn = -1;
        game.lastStaffedProductionBackgroundTurn = game.turn;
        owner.camps.remove(to);
    }
    static void write(GamePanel game, Properties save) {
        save.keySet().removeIf(key -> key instanceof String name && name.startsWith(PREFIX));
        for (var entry : owner(game).camps.entrySet()) {
            try {
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                entry.getValue().data.store(bytes, "Local player camp");
                save.setProperty(PREFIX + entry.getKey(), Base64.getEncoder().encodeToString(bytes.toByteArray()));
            } catch (IOException ex) { throw new UncheckedIOException(ex); }
        }
    }
    static void read(GamePanel game, Properties save) {
        Owner owner = new Owner(game.atlas, new LinkedHashMap<>());
        Set<String> storedUnits = new HashSet<>();
        for (String name : save.stringPropertyNames()) if (name.startsWith(PREFIX)) {
            String key = name.substring(PREFIX.length());
            if (!key.matches("[1-3],[1-3],[1-3],[1-3],(?:[1-9]|10),(?:true|false)") || key.equals(key(game.world)))
                throw new IllegalStateException("Invalid stored camp location");
            try {
                Properties data = new Properties();
                data.load(new ByteArrayInputStream(Base64.getDecoder().decode(save.getProperty(name))));
                if (Persistence.decodeBaseObjects(data).size()
                        != Persistence.decList(data.getProperty("base.objects", "")).size())
                    throw new IllegalStateException("Invalid stored camp construction records");
                for (String line : Persistence.decList(data.getProperty("scavenge.cooldowns", ""))) {
                    String[] pair = line.split(":", 2);
                    if (pair.length != 2) throw new IllegalArgumentException("Invalid stored camp cooldown");
                    Integer.parseInt(pair[0]); Integer.parseInt(pair[1]);
                }
                ContainerRecord container = game.itemContainers.get(storage(key));
                if (container == null) throw new IllegalStateException("Stored camp container is missing");
                for (String id : container.itemInstanceIds) {
                    ItemInstance item = game.itemInstances.get(id);
                    if (!storedUnits.add(id) || item == null || !container.id.equals(item.containerId))
                        throw new IllegalStateException("Invalid or duplicated stored camp item identity");
                }
                owner.camps.put(key, new Camp(data, null));
            } catch (IOException | IllegalArgumentException ex) { throw new IllegalStateException("Invalid stored camp", ex); }
        }
        OWNERS.put(game, owner);
    }
    private PlayerCampStateAuthority() { }
}
