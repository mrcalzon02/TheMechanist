package mechanist;

import java.util.ArrayList;
import java.util.List;

/** Authoritative player room acquisition/loss mutations over the world's room-faction ledger. */
final class RoomOwnershipAuthority {
    private static final int HISTORY_LIMIT = 20;

    record OwnershipChange(int turn, int roomId, Faction previousOwner, Faction nextOwner,
                           String method, String roomName) {
        String saveLine() {
            return turn + "|" + roomId + "|" + factionName(previousOwner) + "|" + factionName(nextOwner)
                    + "|" + safe(method) + "|" + safe(roomName);
        }

        static OwnershipChange parse(String line) {
            if (line == null || line.isBlank()) return null;
            String[] parts = line.split("\\|", 6);
            if (parts.length < 6) return null;
            try {
                return new OwnershipChange(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]),
                        Faction.valueOf(parts[2]), Faction.valueOf(parts[3]), parts[4], parts[5]);
            } catch (RuntimeException ignored) {
                return null;
            }
        }

        String readback() {
            return roomName + ": " + ownerLabel(previousOwner) + " -> " + ownerLabel(nextOwner)
                    + " by " + method + " on turn " + turn + ".";
        }
    }

    record Result(boolean success, String summary, OwnershipChange change) { }
    record PurchaseQuote(int roomId, String roomName, Faction owner, int price, int standingRequired,
                         List<String> blockers) {
        boolean ready() { return blockers == null || blockers.isEmpty(); }
        String readback() {
            String lead = "Room purchase quote: " + roomName + " from " + ownerLabel(owner) + " for " + price
                    + " script; standing " + standingRequired + " required.";
            return ready() ? lead + " Ready: Buy Room completes the transfer and spends one turn."
                    : lead + " Blocked: " + String.join("; ", blockers) + ".";
        }
    }

    static List<String> inspectionLines(GamePanel game, int x, int y, int depth) {
        ArrayList<String> lines = new ArrayList<>();
        if (game == null || game.world == null || !game.world.inBounds(x, y)) return lines;
        int roomId = game.world.roomIdAt(x, y);
        if (!validRoom(game.world, roomId)) return lines;

        Faction owner = game.world.roomFaction(roomId);
        lines.add("Room: " + roomName(game.world, roomId) + ". Control: " + ownerLabel(owner)
                + ". Access: " + accessLabel(game, roomId, owner) + ".");
        ExplicitRoomTypeRequirementAuthority.Assessment purpose =
                ExplicitRoomTypeRequirementAuthority.assessRoom(game.world, roomId);
        if (purpose.declared() && purpose.definition() != null) {
            lines.add("Declared purpose: " + purpose.definition().label() + " ("
                    + purpose.definition().id() + ") via " + purpose.declarationSource() + ".");
            lines.add("Room-purpose status: " + purpose.status().name()
                    + ". Capacity: " + purpose.operatingCapacity() + " "
                    + purpose.capacityUnitLabel() + capacityContext(purpose) + ". Assigned staff: "
                    + purpose.assignedStaff() + ". Blockers: "
                    + (purpose.blockers().isEmpty() ? "none" : String.join("; ", purpose.blockers())) + ".");
        } else {
            lines.add("Declared purpose: none.");
            lines.add("Room-purpose status: " + purpose.status().name()
                    + ". Capacity: not defined. Blockers: no explicit room purpose is declared.");
        }
        if (depth >= 1) {
            RoomProfile profile = game.world.roomProfile(roomId);
            if (profile != null && profile.descriptor != null && !profile.descriptor.isBlank()) {
                lines.add("Room use: " + sentence(profile.descriptor));
            }
            lines.add(acquisitionGuidance(game, roomId, owner));
        }
        if (depth >= 2) {
            RoomProfile profile = game.world.roomProfile(roomId);
            if (profile != null && profile.featureText != null && !profile.featureText.isBlank()
                    && !profile.featureText.equalsIgnoreCase(profile.descriptor)) {
                lines.add("Room features: " + sentence(profile.featureText));
            }
            lines.addAll(RoomControlProvenanceAuthority.inspectionLines(game, roomId));
            OwnershipChange latest = latestChangeFor(game, roomId);
            if (latest != null) lines.add("Latest room control change: " + latest.readback());
            if (purpose.declared()) {
                lines.add("Room-purpose physical evidence: geometry " + purpose.width() + "x"
                        + purpose.height() + ", reachable interior " + purpose.reachableInteriorCells()
                        + ", reachable entrances " + purpose.entrances() + "; witnessed objects/personnel/control/hazards: "
                        + purpose.witnessSummary() + ".");
                for (ExplicitRoomTypeRequirementAuthority.RequirementResult result : purpose.results()) {
                    if (result == null) continue;
                    lines.add("Room-purpose requirement " + (result.satisfied() ? "PASS" : "FAIL")
                            + (result.physicalQualification() ? " [physical]: " : " [operational]: ")
                            + sentence(result.line()) + " Evidence: "
                            + (result.witnesses().isEmpty()
                            ? "authoritative live room state."
                            : String.join(", ", result.witnesses()) + "."));
                }
            } else {
                lines.add("Room-purpose physical evidence: no requirement definition is declared, so no use qualification is inferred from prose or glyphs.");
            }
        }
        return lines;
    }

    private static String capacityContext(ExplicitRoomTypeRequirementAuthority.Assessment purpose) {
        if (purpose == null || purpose.definition() == null) return "";
        if (ExplicitRoomTypeRequirementAuthority.CRECHE_ID.equals(purpose.definition().id())) {
            return " (child care)";
        }
        if (ExplicitRoomTypeRequirementAuthority.BARRACKS_ID.equals(purpose.definition().id())) {
            return " (barracks muster)";
        }
        return " (operating capacity)";
    }

    static String currentActionLabel(GamePanel game) {
        if (game == null || game.world == null) return "Room Control";
        int roomId = game.world.roomIdAt(game.playerX, game.playerY);
        if (!validRoom(game.world, roomId)) return "Room Control";
        Faction owner = game.world.roomFaction(roomId);
        if (owner == Faction.NONE && (!game.baseClaimed || game.claimedRoomId == roomId)) return "Claim Room";
        if (owner == Faction.HIVER && game.baseClaimed && game.claimedRoomId == roomId) return "Abandon Room";
        if (owner != Faction.NONE && owner != Faction.HIVER) return "Buy Room";
        return "Room Control";
    }

    static String currentActionTooltip(GamePanel game) {
        if (game == null || game.world == null) return "Inspect current room control and acquisition paths.";
        int roomId = game.world.roomIdAt(game.playerX, game.playerY);
        if (!validRoom(game.world, roomId)) return "Stand inside a recognized room to inspect or change its control.";
        return acquisitionGuidance(game, roomId, game.world.roomFaction(roomId));
    }

    static Result applyCurrentRoomAction(GamePanel game) {
        if (game == null || game.world == null) return blocked("Room control unavailable: no world is loaded.");
        int roomId = game.world.roomIdAt(game.playerX, game.playerY);
        if (!validRoom(game.world, roomId)) {
            return blocked("Room control blocked: stand inside a recognized room before changing control.");
        }
        Faction owner = game.world.roomFaction(roomId);
        if (owner == Faction.NONE) return claimCurrentRoom(game);
        if (owner == Faction.HIVER && game.baseClaimed && game.claimedRoomId == roomId) {
            return abandonClaimedRoom(game);
        }
        if (owner == Faction.HIVER) {
            return blocked("Room control blocked: " + roomName(game.world, roomId)
                    + " is controlled by your local faction, but it is not your claimed base.");
        }
        return purchaseCurrentRoom(game);
    }

    static PurchaseQuote purchaseQuote(GamePanel game) {
        if (game == null || game.world == null) {
            return new PurchaseQuote(-1, "unrecognized room", Faction.NONE, 0, 0,
                    List.of("no world is loaded"));
        }
        return purchaseQuote(game, game.world.roomIdAt(game.playerX, game.playerY));
    }

    static Result purchaseCurrentRoom(GamePanel game) {
        PurchaseQuote quote = purchaseQuote(game);
        if (!quote.ready()) return blocked("Room purchase blocked: " + quote.readback());
        if (game == null || game.world == null || !validRoom(game.world, quote.roomId())) {
            return blocked("Room purchase unavailable: no valid room is selected.");
        }
        Faction liveOwner = game.world.roomFaction(quote.roomId());
        if (liveOwner != quote.owner()) {
            return blocked("Room purchase blocked: room control changed before the transfer could complete. Inspect the room again.");
        }
        if (!game.spendImperialScript(quote.price())) {
            return blocked("Room purchase blocked: need " + quote.price() + " script, have " + game.carriedScript + ".");
        }

        game.world.roomFactions.set(quote.roomId(), Faction.HIVER);
        game.baseClaimed = true;
        game.claimedRoomId = quote.roomId();
        game.baseX = game.playerX;
        game.baseY = game.playerY;
        OwnershipChange change = new OwnershipChange(game.turn, quote.roomId(), quote.owner(), Faction.HIVER,
                "purchase from " + ownerLabel(quote.owner()) + " for " + quote.price() + " script", quote.roomName());
        remember(game, change);
        return new Result(true, "Room purchased: " + quote.roomName() + " from " + ownerLabel(quote.owner())
                + " for " + quote.price() + " script. Control transferred to your local faction.", change);
    }

    static String completePlayerAction(GamePanel game, Result result) {
        Result resolved = result == null ? blocked("Room control unavailable.") : result;
        if (game != null) game.logEvent(resolved.summary());
        if (!resolved.success() || game == null || resolved.change() == null) return resolved.summary();
        String method = resolved.change().method() == null ? "" : resolved.change().method().toLowerCase(java.util.Locale.ROOT);
        String verb = method.startsWith("purchase") ? "purchases "
                : resolved.change().nextOwner() == Faction.HIVER ? "claims " : "abandons ";
        game.advanceTurn(verb + resolved.change().roomName() + ".");
        return resolved.summary() + " One turn spent.";
    }

    static Result claimCurrentRoom(GamePanel game) {
        if (game == null || game.world == null) return blocked("Room claim unavailable: no world is loaded.");
        int roomId = game.world.roomIdAt(game.playerX, game.playerY);
        if (!validRoom(game.world, roomId)) {
            return blocked("Room claim blocked: stand inside a recognized room before claiming it.");
        }
        Faction owner = game.world.roomFaction(roomId);
        String roomName = roomName(game.world, roomId);
        if (owner == Faction.HIVER) {
            return blocked("Room claim blocked: " + roomName + " is already controlled by your local faction.");
        }
        if (owner != Faction.NONE) {
            return blocked("Room claim blocked: " + roomName + " is controlled by " + ownerLabel(owner)
                    + ". A peaceful abandonment claim cannot displace an active owner.");
        }
        if (game.baseClaimed && game.claimedRoomId >= 0 && game.claimedRoomId != roomId) {
            return blocked("Room claim blocked: abandon " + game.baseDisplayName()
                    + " before establishing another claimed base.");
        }

        game.world.roomFactions.set(roomId, Faction.HIVER);
        game.baseClaimed = true;
        game.claimedRoomId = roomId;
        game.baseX = game.playerX;
        game.baseY = game.playerY;
        OwnershipChange change = new OwnershipChange(game.turn, roomId, owner, Faction.HIVER,
                "abandonment claim", roomName);
        remember(game, change);
        return new Result(true, "Room claimed: " + roomName
                + ". Control changed from unowned to your local faction by abandonment claim.", change);
    }

    static Result abandonClaimedRoom(GamePanel game) {
        if (game == null || game.world == null) return blocked("Room abandonment unavailable: no world is loaded.");
        if (!game.baseClaimed || !validRoom(game.world, game.claimedRoomId)) {
            return blocked("Room abandonment blocked: you do not have a claimed room to relinquish.");
        }
        int currentRoom = game.world.roomIdAt(game.playerX, game.playerY);
        if (currentRoom != game.claimedRoomId) {
            return blocked("Room abandonment blocked: return to " + game.baseDisplayName()
                    + " before relinquishing control.");
        }
        int roomId = game.claimedRoomId;
        Faction owner = game.world.roomFaction(roomId);
        String roomName = roomName(game.world, roomId);
        if (owner != Faction.HIVER) {
            return blocked("Room abandonment blocked: " + roomName
                    + " is no longer controlled by your local faction.");
        }

        game.world.roomFactions.set(roomId, Faction.NONE);
        OwnershipChange change = new OwnershipChange(game.turn, roomId, owner, Faction.NONE,
                "voluntary abandonment", roomName);
        remember(game, change);
        game.baseClaimed = false;
        game.claimedRoomId = -1;
        game.baseX = -1;
        game.baseY = -1;
        return new Result(true, "Room abandoned: " + roomName
                + ". Control changed from your local faction to unowned.", change);
    }

    static String status(GamePanel game) {
        if (game == null || game.world == null) return "Room control unavailable: no world is loaded.";
        int currentRoom = game.world.roomIdAt(game.playerX, game.playerY);
        String current = validRoom(game.world, currentRoom)
                ? roomName(game.world, currentRoom) + " is controlled by " + ownerLabel(game.world.roomFaction(currentRoom)) + "."
                : "You are not standing inside a recognized room.";
        String claimed = game.baseClaimed && validRoom(game.world, game.claimedRoomId)
                ? "Claimed base: " + roomName(game.world, game.claimedRoomId) + "."
                : "Claimed base: none.";
        OwnershipChange latest = game.roomOwnershipHistory.peekLast();
        return "Current room: " + current + " " + claimed
                + (latest == null ? " Ownership history: none." : " Latest control change: " + latest.readback());
    }

    static List<String> saveHistory(GamePanel game) {
        ArrayList<String> lines = new ArrayList<>();
        if (game != null) for (OwnershipChange change : game.roomOwnershipHistory) if (change != null) lines.add(change.saveLine());
        return lines;
    }

    static void restoreHistory(GamePanel game, List<String> lines) {
        if (game == null) return;
        game.roomOwnershipHistory.clear();
        if (lines == null) return;
        for (String line : lines) {
            OwnershipChange change = OwnershipChange.parse(line);
            if (change != null) remember(game, change);
        }
    }

    static String roomName(World world, int roomId) {
        if (!validRoom(world, roomId)) return "unrecognized room";
        RoomProfile profile = world.roomProfile(roomId);
        String name = profile == null ? "" : profile.name;
        return name == null || name.isBlank() ? "Unnamed room" : name.trim();
    }

    static boolean hasAuthoritativePlayerClaim(GamePanel game, int roomId) {
        if (game == null || roomId < 0) return false;
        java.util.Iterator<OwnershipChange> changes = game.roomOwnershipHistory.descendingIterator();
        while (changes.hasNext()) {
            OwnershipChange change = changes.next();
            if (change != null && change.roomId() == roomId) return change.nextOwner() == Faction.HIVER;
        }
        return false;
    }

    private static String accessLabel(GamePanel game, int roomId, Faction owner) {
        if (owner == null || owner == Faction.NONE) return "abandoned and unowned";
        if (owner == Faction.HIVER && game != null && game.baseClaimed && game.claimedRoomId == roomId) {
            return "player-owned claimed base";
        }
        if (owner == Faction.HIVER) return "local-faction controlled";
        return "faction-owned; modification restricted";
    }

    private static String acquisitionGuidance(GamePanel game, int roomId, Faction owner) {
        int currentRoom = game.world.roomIdAt(game.playerX, game.playerY);
        String name = roomName(game.world, roomId);
        if (currentRoom != roomId) {
            if (owner == null || owner == Faction.NONE) {
                return "Acquisition path: enter " + name + " to make an abandonment claim.";
            }
            return "Acquisition path: enter the room and secure a purchase, lease, faction grant, legal permit, or conquest path from "
                    + ownerLabel(owner) + ".";
        }
        if (owner == null || owner == Faction.NONE) {
            if (game.baseClaimed && game.claimedRoomId != roomId) {
                return "Acquisition blocked: abandon " + game.baseDisplayName()
                        + " before establishing another claimed base.";
            }
            return "Acquisition available: Claim Room makes an abandonment claim and spends one turn.";
        }
        if (owner == Faction.HIVER && game.baseClaimed && game.claimedRoomId == roomId) {
            return "Loss available: Abandon Room relinquishes this claimed base and spends one turn.";
        }
        if (owner == Faction.HIVER) {
            return "Acquisition blocked: your local faction controls this room, but it is not your claimed base.";
        }
        return purchaseQuote(game, roomId).readback()
                + " Other paths may include lease, faction grant, legal permit, or conquest.";
    }

    private static PurchaseQuote purchaseQuote(GamePanel game, int roomId) {
        if (game == null || game.world == null || !validRoom(game.world, roomId)) {
            return new PurchaseQuote(roomId, "unrecognized room", Faction.NONE, 0, 0,
                    List.of("stand inside a recognized room"));
        }
        Faction owner = game.world.roomFaction(roomId);
        String name = roomName(game.world, roomId);
        if (owner == null || owner == Faction.NONE) {
            return new PurchaseQuote(roomId, name, Faction.NONE, 0, 0,
                    List.of("this room is unowned; use Claim Room instead"));
        }
        if (owner == Faction.HIVER) {
            return new PurchaseQuote(roomId, name, owner, 0, 0,
                    List.of("your local faction already controls this room"));
        }

        int price = purchasePrice(game.world, roomId, owner);
        int standingRequired = isNoble(owner) ? 25 : 5;
        ArrayList<String> blockers = new ArrayList<>();
        if (game.baseClaimed && game.claimedRoomId != roomId) {
            blockers.add("abandon " + game.baseDisplayName() + " before buying another base");
        }
        if (roomId < game.world.roomSpecials.size() && Boolean.TRUE.equals(game.world.roomSpecials.get(roomId))) {
            blockers.add("this protected or special-purpose room is not offered for permanent sale");
        }
        int occupants = livingOccupants(game.world, roomId);
        if (occupants > 0) blockers.add("the room still has " + occupants + " living occupant" + (occupants == 1 ? "" : "s"));
        if (!hasLivingRepresentative(game.world, owner)) {
            blockers.add("no living " + ownerLabel(owner) + " representative is available in this zone");
        }
        int hostileUntil = hostileUntil(game, owner);
        if (hostileUntil > game.turn) blockers.add("active hostility continues until turn " + hostileUntil);
        int standing = standingFor(game, owner);
        if (standing < standingRequired) blockers.add("standing is " + standing + "/" + standingRequired);
        if (game.carriedScript < price) blockers.add("need " + price + " script, have " + game.carriedScript);
        return new PurchaseQuote(roomId, name, owner, price, standingRequired, List.copyOf(blockers));
    }

    private static int purchasePrice(World world, int roomId, Faction owner) {
        java.awt.Rectangle room = world == null ? null : world.roomRect(roomId);
        int area = room == null ? 1 : Math.max(1, room.width) * Math.max(1, room.height);
        int price = 20 + area * 2;
        if (isNoble(owner)) price += 40 + area;
        return Math.max(20, Math.min(50_000, price));
    }

    private static boolean isNoble(Faction faction) {
        return FactionIdentityAuthority.strategicFamily(faction) == Faction.NOBLE;
    }

    private static boolean hasLivingRepresentative(World world, Faction owner) {
        if (world == null || world.npcs == null) return false;
        for (NpcEntity npc : world.npcs) {
            if (npc != null && npc.hp > 0 && npc.isFactionRepresentative()
                    && FactionIdentityAuthority.sameFamily(npc.faction, owner)) return true;
        }
        return false;
    }

    private static int livingOccupants(World world, int roomId) {
        if (world == null || world.npcs == null) return 0;
        int count = 0;
        for (NpcEntity npc : world.npcs) {
            if (npc != null && npc.hp > 0 && world.inBounds(npc.x, npc.y)
                    && world.roomIdAt(npc.x, npc.y) == roomId) count++;
        }
        return count;
    }

    private static int standingFor(GamePanel game, Faction owner) {
        int standing = Integer.MIN_VALUE;
        for (java.util.Map.Entry<Faction, Integer> entry : game.factionStanding.entrySet()) {
            if (entry.getKey() != null && FactionIdentityAuthority.sameFamily(entry.getKey(), owner)) {
                standing = Math.max(standing, entry.getValue() == null ? 0 : entry.getValue());
            }
        }
        return standing == Integer.MIN_VALUE ? 0 : standing;
    }

    private static int hostileUntil(GamePanel game, Faction owner) {
        int until = 0;
        for (java.util.Map.Entry<Faction, Integer> entry : game.temporaryHostileTurns.entrySet()) {
            if (entry.getKey() != null && FactionIdentityAuthority.sameFamily(entry.getKey(), owner)) {
                until = Math.max(until, entry.getValue() == null ? 0 : entry.getValue());
            }
        }
        return until;
    }

    private static OwnershipChange latestChangeFor(GamePanel game, int roomId) {
        if (game == null) return null;
        java.util.Iterator<OwnershipChange> changes = game.roomOwnershipHistory.descendingIterator();
        while (changes.hasNext()) {
            OwnershipChange change = changes.next();
            if (change != null && change.roomId() == roomId) return change;
        }
        return null;
    }

    private static void remember(GamePanel game, OwnershipChange change) {
        game.roomOwnershipHistory.addLast(change);
        while (game.roomOwnershipHistory.size() > HISTORY_LIMIT) game.roomOwnershipHistory.removeFirst();
    }

    private static boolean validRoom(World world, int roomId) {
        return world != null && roomId >= 0 && roomId < world.rooms.size() && roomId < world.roomFactions.size();
    }

    private static Result blocked(String summary) { return new Result(false, summary, null); }
    private static String ownerLabel(Faction faction) {
        return faction == null || faction == Faction.NONE ? "unowned" : faction.label;
    }
    private static String factionName(Faction faction) { return (faction == null ? Faction.NONE : faction).name(); }
    private static String sentence(String value) {
        String text = value == null ? "" : value.trim();
        if (text.isBlank()) return "unknown.";
        char last = text.charAt(text.length() - 1);
        return last == '.' || last == '!' || last == '?' ? text : text + ".";
    }
    private static String safe(String value) { return value == null ? "" : value.replace('|', '/').replace('\n', ' ').replace('\r', ' '); }

    private RoomOwnershipAuthority() { }
}
