package mechanist;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Locale;
import java.util.Objects;
import java.util.Random;

/**
 * Resolves the non-construction physical asset operations owned by faction
 * strategy: scheme-backed room seizure, salvage of captured machinery, and
 * deployment of an existing room worker as a visible facility specialist.
 */
final class FactionStrategicAssetAuthority {
    static final String ROOM_SEIZURE_GOAL = "take control of a room";
    static final String CAPTURED_ASSET_SALVAGE_GOAL = "salvage captured assets";
    static final String FACILITY_SPECIALIST_GOAL = "deploy a facility specialist";

    record Outcome(boolean handled, boolean success, String blocker, String message,
                   int roomId, int stockBefore, int stockAfter,
                   BaseObject asset, NpcEntity specialist) {
        static Outcome notHandled(String message) {
            return new Outcome(false, false, "not-handled", safe(message),
                    -1, 0, 0, null, null);
        }

        static Outcome blocked(String blocker, String message, NpcFactionSite site,
                               int roomId, BaseObject asset) {
            int stock = site == null ? 0 : Math.max(0, site.stock);
            return new Outcome(true, false, safe(blocker), safe(message),
                    roomId, stock, stock, asset, null);
        }
    }

    private record RoomTarget(int roomId, Faction owner, int score) { }

    private FactionStrategicAssetAuthority() { }

    static boolean handles(FactionStrategicPlan plan) {
        if (plan == null || plan.immediateGoal == null) return false;
        String goal = plan.immediateGoal.trim();
        return ROOM_SEIZURE_GOAL.equalsIgnoreCase(goal)
                || CAPTURED_ASSET_SALVAGE_GOAL.equalsIgnoreCase(goal)
                || FACILITY_SPECIALIST_GOAL.equalsIgnoreCase(goal);
    }

    static Outcome attempt(GamePanel game, FactionStrategicPlan plan, NpcFactionSite site) {
        if (!handles(plan)) {
            String goal = plan == null || plan.immediateGoal == null ? "none" : plan.immediateGoal;
            return Outcome.notHandled("Faction strategic asset authority does not handle goal: "
                    + goal + ".");
        }
        if (ROOM_SEIZURE_GOAL.equalsIgnoreCase(plan.immediateGoal.trim())) {
            return seizeRoom(game, plan, site);
        }
        if (CAPTURED_ASSET_SALVAGE_GOAL.equalsIgnoreCase(plan.immediateGoal.trim())) {
            return salvageCapturedAsset(game, plan, site);
        }
        return deployFacilitySpecialist(game, plan, site);
    }

    /**
     * Transfers one rival-controlled room only when the plan carries an actual
     * scheme, a local same-family site, workers, and the stock needed to execute
     * the takeover. Player-claimed rooms are never eligible.
     */
    private static Outcome seizeRoom(GamePanel game, FactionStrategicPlan plan,
                                     NpcFactionSite site) {
        Outcome preflight = preflight(game, plan, site, "room seizure");
        if (preflight != null) return preflight;
        if (plan.scheme == null || plan.scheme.isBlank()) {
            return Outcome.blocked("no-seizure-scheme",
                    factionLabel(plan.faction)
                            + " cannot seize property without a recorded scheme.",
                    site, -1, null);
        }

        int workers = Math.max(0,
                FactionSiteWorkforceAuthority.evaluate(site, game.world).effectiveWorkers());
        if (workers <= 0) {
            return Outcome.blocked("no-workforce",
                    siteLabel(site) + " has no effective workers available to execute "
                            + plan.scheme + ".", site, -1, null);
        }
        RoomTarget target = selectSeizureRoom(game, plan, site);
        if (target == null) {
            return Outcome.blocked("no-seizable-room",
                    factionLabel(plan.faction)
                            + " found no rival-controlled, non-special room eligible for seizure.",
                    site, -1, null);
        }

        int stockCost = 2 + Math.max(0, 60 - Math.max(0, plan.secrecy)) / 20;
        if (site.stock < stockCost) {
            return Outcome.blocked("insufficient-site-stock",
                    siteLabel(site) + " needs " + stockCost
                            + " operational stock to execute " + plan.scheme
                            + ", but has " + site.stock + ".",
                    site, target.roomId(), null);
        }

        int stockBefore = site.stock;
        site.stock -= stockCost;
        Faction previous = target.owner();
        game.world.roomFactions.set(target.roomId(), site.faction);
        RoomProfile profile = game.world.roomProfile(target.roomId());
        if (profile != null) {
            profile.faction = site.faction;
            profile.featureText = append(profile.featureText,
                    "Property control seized by " + factionLabel(site.faction)
                            + " through " + safe(plan.scheme) + " at turn "
                            + Math.max(0, game.turn) + ".");
        }
        String roomName = RoomOwnershipAuthority.roomName(game.world, target.roomId());
        rememberConflict(game.world,
                "LIVE-SEIZURE turn=" + Math.max(0, game.turn)
                        + " room=" + roomName + " previous=" + factionLabel(previous)
                        + " next=" + factionLabel(site.faction)
                        + " scheme=" + safe(plan.scheme)
                        + " stock=" + stockCost + " workers=" + workers);
        if (previous != null && previous != Faction.NONE) {
            game.addFactionMarketPressure(previous, 2 + Math.max(0, plan.aggression) / 35,
                    factionLabel(site.faction) + " seized " + roomName
                            + " through " + safe(plan.scheme));
        }
        return new Outcome(true, true, "",
                factionLabel(site.faction) + " seized " + roomName + " from "
                        + factionLabel(previous) + " through " + safe(plan.scheme)
                        + "; site stock " + stockBefore + " -> " + site.stock
                        + " and " + workers + " effective worker(s) executed the transfer.",
                target.roomId(), stockBefore, site.stock, null, null);
    }

    /**
     * Removes one foreign or unassigned completed machine from a room already
     * controlled by the faction. Salvage yield is credited to the linked site
     * and represented as provenance-aware Machine part stock.
     */
    private static Outcome salvageCapturedAsset(GamePanel game, FactionStrategicPlan plan,
                                                NpcFactionSite site) {
        Outcome preflight = preflight(game, plan, site, "captured-asset salvage");
        if (preflight != null) return preflight;

        BaseObject asset = capturedAsset(game, site);
        if (asset == null) {
            return Outcome.blocked("no-captured-asset",
                    siteLabel(site)
                            + " controls no room containing a completed foreign machine or facility to salvage.",
                    site, -1, null);
        }
        int roomId = game.world.roomIdAt(asset.x, asset.y);
        int workers = assignedWorkers(game.world, roomId, site.faction);
        if (workers <= 0) {
            return Outcome.blocked("captured-room-unstaffed",
                    RoomOwnershipAuthority.roomName(game.world, roomId)
                            + " contains captured machinery, but no same-family workers are assigned there.",
                    site, roomId, asset);
        }

        int integrity = Math.max(0, asset.integrity);
        int yield = Math.min(12, 2 + Math.min(5, integrity)
                + (MachineTierAuthority.isMachineOrFacilitySymbol(asset.symbol) ? 3 : 0));
        int stockBefore = site.stock;
        site.stock = Math.min(160, site.stock + yield);
        int credited = site.stock - stockBefore;
        String assetName = objectLabel(asset);
        Faction priorCustodian = asset.faction == null ? Faction.NONE : asset.faction;

        game.baseObjects.remove(asset);
        if (game.world.inBounds(asset.x, asset.y)) {
            char restored = asset.constructionOriginalTile;
            if (restored == 0 || restored == '?' || restored == asset.symbol
                    || MachineTierAuthority.isMachineOrFacilitySymbol(restored)) restored = '.';
            game.world.tiles[asset.x][asset.y] = restored;
        }
        if (credited > 0) {
            FactionStrategySimulationApi.materializeFactionStock(game, site,
                    "Machine part", Math.max(1, credited / 3),
                    "salvaged from captured " + assetName + " in "
                            + RoomOwnershipAuthority.roomName(game.world, roomId));
        }
        rememberConflict(game.world,
                "LIVE-SALVAGE turn=" + Math.max(0, game.turn)
                        + " room=" + RoomOwnershipAuthority.roomName(game.world, roomId)
                        + " asset=" + assetName + " prior=" + factionLabel(priorCustodian)
                        + " next=" + factionLabel(site.faction)
                        + " stockYield=" + credited + " workers=" + workers);
        return new Outcome(true, true, "",
                factionLabel(site.faction) + " salvaged captured " + assetName
                        + " from " + RoomOwnershipAuthority.roomName(game.world, roomId)
                        + "; faction-site stock " + stockBefore + " -> " + site.stock
                        + " with traceable Machine part recovery and " + workers
                        + " assigned worker(s).",
                roomId, stockBefore, site.stock, asset, null);
    }

    /**
     * Materializes one visible specialist from an existing room workforce. The
     * room ledger is not incremented: the NPC is a representation of one of the
     * already assigned workers, not a newly created population unit.
     */
    private static Outcome deployFacilitySpecialist(GamePanel game,
                                                    FactionStrategicPlan plan,
                                                    NpcFactionSite site) {
        Outcome preflight = preflight(game, plan, site, "facility specialist deployment");
        if (preflight != null) return preflight;

        BaseObject facility = operationalFacility(game, site);
        if (facility == null) {
            return Outcome.blocked("no-operational-facility",
                    siteLabel(site)
                            + " has no completed machine or facility inside a same-family controlled room.",
                    site, -1, null);
        }
        int roomId = game.world.roomIdAt(facility.x, facility.y);
        int workers = assignedWorkers(game.world, roomId, site.faction);
        if (workers <= 0) {
            return Outcome.blocked("facility-unstaffed",
                    RoomOwnershipAuthority.roomName(game.world, roomId)
                            + " has no assigned worker available to represent as a specialist.",
                    site, roomId, facility);
        }

        String specialistId = specialistId(site, facility);
        for (NpcEntity npc : game.world.npcs) {
            if (npc != null && specialistId.equals(npc.id)) {
                return new Outcome(true, true, "",
                        npc.name + " remains assigned as the visible specialist for "
                                + objectLabel(facility) + "; no population or stock was added.",
                        roomId, site.stock, site.stock, facility, npc);
            }
        }

        Point point = firstSpecialistPoint(game, roomId);
        if (point == null) {
            return Outcome.blocked("no-specialist-position",
                    RoomOwnershipAuthority.roomName(game.world, roomId)
                            + " has assigned workers but no legal open specialist position.",
                    site, roomId, facility);
        }
        String role = specialistRole(facility.symbol);
        Random rng = new Random(game.world.seed ^ Objects.hash(
                specialistId, facility.x, facility.y, roomId));
        NpcEntity specialist = NpcEntity.create(site.faction, game.world.zoneType,
                point.x, point.y, rng);
        specialist.id = specialistId;
        specialist.role = role;
        specialist.state = "Operate Facility";
        specialist.symbol = 'n';
        specialist.name = factionLabel(site.faction) + " " + role + " "
                + surname(specialist.name);
        specialist.factionRank = Math.max(1, specialist.factionRank);
        specialist.factionRankTitle = role;
        specialist.factionRankScope = objectLabel(facility) + " operations in "
                + RoomOwnershipAuthority.roomName(game.world, roomId);
        game.world.npcs.add(specialist);

        RoomProfile profile = game.world.roomProfile(roomId);
        if (profile != null) {
            profile.featureText = append(profile.featureText,
                    "Visible facility specialist: " + specialist.name + " operates "
                            + objectLabel(facility)
                            + " as one of the room's " + workers + " assigned worker(s).");
        }
        return new Outcome(true, true, "",
                specialist.name + " deployed to operate " + objectLabel(facility)
                        + " in " + RoomOwnershipAuthority.roomName(game.world, roomId)
                        + "; the room remains at " + workers
                        + " assigned worker(s) and no faction stock was created.",
                roomId, site.stock, site.stock, facility, specialist);
    }

    private static Outcome preflight(GamePanel game, FactionStrategicPlan plan,
                                     NpcFactionSite site, String operation) {
        if (game == null || game.world == null) {
            return Outcome.blocked("no-world",
                    "Physical " + operation + " requires a loaded world.",
                    site, -1, null);
        }
        if (site == null) {
            return Outcome.blocked("no-site",
                    factionLabel(plan == null ? Faction.NONE : plan.faction)
                            + " has no local site for " + operation + ".",
                    null, -1, null);
        }
        if (plan == null || !FactionIdentityAuthority.sameFamily(plan.faction, site.faction)) {
            return Outcome.blocked("wrong-faction-site",
                    "The strategic plan and faction site do not share a faction family.",
                    site, -1, null);
        }
        if (!sameLocation(site, game.world)) {
            return Outcome.blocked("site-not-local",
                    siteLabel(site) + " is not in the loaded zone.",
                    site, -1, null);
        }
        return null;
    }

    private static RoomTarget selectSeizureRoom(GamePanel game,
                                                FactionStrategicPlan plan,
                                                NpcFactionSite site) {
        RoomTarget best = null;
        for (int roomId = 0; roomId < game.world.rooms.size(); roomId++) {
            if (roomId >= game.world.roomFactions.size()) continue;
            Faction owner = game.world.roomFaction(roomId);
            if (owner == null || FactionIdentityAuthority.sameFamily(owner, site.faction)) continue;
            if (roomId < game.world.roomSpecials.size()
                    && Boolean.TRUE.equals(game.world.roomSpecials.get(roomId))) continue;
            RoomProfile profile = game.world.roomProfile(roomId);
            if (game.world.isFactionRepBarProfile(profile)) continue;
            if (owner == Faction.HIVER && game.baseClaimed
                    && game.claimedRoomId == roomId) continue;

            int score = 10;
            if (plan.schemeTargetFaction != null
                    && plan.schemeTargetFaction != Faction.NONE
                    && FactionIdentityAuthority.sameFamily(owner,
                    plan.schemeTargetFaction)) score += 50;
            String target = safe(plan.targetRoom).toLowerCase(Locale.ROOT);
            String roomText = roomText(profile).toLowerCase(Locale.ROOT);
            if (!target.isBlank() && roomText.contains(target)) score += 30;
            for (BaseObject object : game.baseObjects) {
                if (object != null && !object.underConstruction
                        && game.world.roomIdAt(object.x, object.y) == roomId) score += 4;
            }
            RoomTarget candidate = new RoomTarget(roomId, owner, score);
            if (best == null || candidate.score() > best.score()
                    || candidate.score() == best.score()
                    && candidate.roomId() < best.roomId()) best = candidate;
        }
        return best;
    }

    private static BaseObject capturedAsset(GamePanel game, NpcFactionSite site) {
        ArrayList<BaseObject> candidates = new ArrayList<>();
        for (BaseObject object : game.baseObjects) {
            if (object == null || object.underConstruction
                    || !MachineTierAuthority.isMachineOrFacilitySymbol(object.symbol)) continue;
            int roomId = game.world.roomIdAt(object.x, object.y);
            if (roomId < 0 || !FactionIdentityAuthority.sameFamily(
                    game.world.roomFaction(roomId), site.faction)) continue;
            Faction custodian = object.faction == null ? Faction.NONE : object.faction;
            if (custodian == Faction.HIVER
                    || FactionIdentityAuthority.sameFamily(custodian, site.faction)) continue;
            candidates.add(object);
        }
        candidates.sort(Comparator
                .comparingInt((BaseObject object) -> Math.max(0, object.integrity))
                .thenComparingInt(object -> object.y)
                .thenComparingInt(object -> object.x)
                .thenComparing(FactionStrategicAssetAuthority::objectLabel));
        return candidates.isEmpty() ? null : candidates.get(0);
    }

    private static BaseObject operationalFacility(GamePanel game, NpcFactionSite site) {
        ArrayList<BaseObject> candidates = new ArrayList<>();
        for (BaseObject object : game.baseObjects) {
            if (object == null || object.underConstruction
                    || !MachineTierAuthority.isMachineOrFacilitySymbol(object.symbol)) continue;
            int roomId = game.world.roomIdAt(object.x, object.y);
            if (roomId < 0 || !FactionIdentityAuthority.sameFamily(
                    game.world.roomFaction(roomId), site.faction)) continue;
            Faction custodian = object.faction == null ? Faction.NONE : object.faction;
            if (custodian != Faction.NONE
                    && !FactionIdentityAuthority.sameFamily(custodian, site.faction)) continue;
            candidates.add(object);
        }
        candidates.sort(Comparator.comparingInt((BaseObject object) -> object.y)
                .thenComparingInt(object -> object.x)
                .thenComparing(FactionStrategicAssetAuthority::objectLabel));
        return candidates.isEmpty() ? null : candidates.get(0);
    }

    private static int assignedWorkers(World world, int roomId, Faction faction) {
        if (world == null || roomId < 0) return 0;
        if (world.roomPopulationLedgers == null || world.roomPopulationLedgers.isEmpty()) return 0;
        long assigned = 0;
        for (RoomPopulationLedger ledger : world.roomPopulationLedgers) {
            if (ledger == null || ledger.roomId != roomId
                    || !FactionIdentityAuthority.sameFamily(ledger.faction, faction)) continue;
            assigned += Math.max(0, ledger.assigned);
        }
        return (int)Math.min(Integer.MAX_VALUE, assigned);
    }

    private static Point firstSpecialistPoint(GamePanel game, int roomId) {
        Rectangle room = game.world.roomRect(roomId);
        if (room == null) return null;
        int xStart = Math.max(0, room.x + 1);
        int xEnd = Math.min(game.world.w, room.x + room.width - 1);
        int yStart = Math.max(0, room.y + 1);
        int yEnd = Math.min(game.world.h, room.y + room.height - 1);
        for (int x = xStart; x < xEnd; x++) {
            for (int y = yStart; y < yEnd; y++) {
                if (game.world.roomIdAt(x, y) != roomId
                        || !game.world.walkable(x, y)) continue;
                if (x == game.playerX && y == game.playerY) continue;
                if (game.world.npcAt(x, y) != null
                        || game.world.mapObjectAt(x, y) != null
                        || game.baseObjectAt(x, y) != null
                        || game.world.isDoorAccessReservedForObject(x, y)) continue;
                return new Point(x, y);
            }
        }
        return null;
    }

    private static String specialistRole(char symbol) {
        return switch (symbol) {
            case 'f', 'w', 'l', 's' -> "Industrial Facility Specialist";
            case 'M' -> "Medicae Facility Specialist";
            case 'u', 'e', 'B' -> "Provision Facility Specialist";
            default -> "Facility Operations Specialist";
        };
    }

    private static String specialistId(NpcFactionSite site, BaseObject facility) {
        return "FACTION-FACILITY-SPECIALIST-"
                + FactionCriticalVendorPlacementAuthority.siteToken(site)
                + "-" + Integer.toUnsignedString(Objects.hash(
                facility.x, facility.y, facility.symbol, objectLabel(facility)), 36)
                .toUpperCase(Locale.ROOT);
    }

    private static boolean sameLocation(NpcFactionSite site, World world) {
        return site != null && world != null
                && site.sectorX == world.sectorX && site.sectorY == world.sectorY
                && site.zoneX == world.zoneX && site.zoneY == world.zoneY
                && site.floor == world.floor;
    }

    private static void rememberConflict(World world, String line) {
        if (world == null || line == null || line.isBlank()) return;
        world.zoneConflictLossHistory = appendLedger(world.zoneConflictLossHistory, line, 40);
    }

    private static String appendLedger(String current, String line, int maxEntries) {
        ArrayList<String> entries = new ArrayList<>();
        if (current != null && !current.isBlank()) {
            for (String entry : current.split(";;")) {
                if (entry != null && !entry.isBlank()) entries.add(entry.trim());
            }
        }
        entries.add(line.trim());
        while (entries.size() > Math.max(1, maxEntries)) entries.remove(0);
        return String.join(";;", entries);
    }

    private static String roomText(RoomProfile profile) {
        return profile == null ? "" : safe(profile.name) + " "
                + safe(profile.descriptor) + " " + safe(profile.featureText);
    }

    private static String objectLabel(BaseObject object) {
        if (object == null || object.name == null || object.name.isBlank()) {
            return "unnamed faction asset";
        }
        return object.name.replaceFirst("^Under construction: ", "").trim();
    }

    private static String surname(String name) {
        if (name == null || name.isBlank()) return "Operator";
        int cut = name.lastIndexOf(' ');
        return cut >= 0 && cut + 1 < name.length()
                ? name.substring(cut + 1) : name;
    }

    private static String factionLabel(Faction faction) {
        return faction == null ? Faction.NONE.label : faction.label;
    }

    private static String siteLabel(NpcFactionSite site) {
        return site == null || site.name == null || site.name.isBlank()
                ? "Unnamed faction site" : site.name.trim();
    }

    private static String append(String current, String line) {
        if (line == null || line.isBlank()) return safe(current);
        if (current == null || current.isBlank()) return line;
        return current.contains(line) ? current : current + " " + line;
    }

    private static String safe(String value) {
        return value == null ? "" : value.replace('|', '/');
    }
}
