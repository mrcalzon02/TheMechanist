package mechanist;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/** Owns casualty capture and capacity-limited faction reinforcement arrivals. */
final class FactionReinforcementAuthority {
    static final int ARRIVAL_WINDOW_TURNS = GamePanel.TURNS_PER_HOUR * 6;
    static final int MAX_ARRIVAL_GROUP = 4;

    enum SourceMethod {
        TRAIN_IMPORT("train-import", "Reinforcement train", 150, 101, 0,
                GamePanel.TURNS_PER_HOUR * 8, "owned or faction-linked rail intake building"),
        BARRACKS_MUSTER("barracks-muster", "Barracks reserve muster", 35, 31, 6,
                GamePanel.TURNS_PER_HOUR * 4, "owned or faction-linked barracks/duty building"),
        PAID_LOCAL("paid-local", "Paid local recruitment", 65, 46, 24,
                GamePanel.TURNS_PER_HOUR * 6, "available local population roster"),
        LEGACY_ROSTER("legacy-roster", "Legacy roster replacement", 36, 49, 0,
                ARRIVAL_WINDOW_TURNS, "linked population roster");

        final String id, label, prerequisite;
        final int minimumDelay, delaySpread, scriptCost, arrivalWindow;

        SourceMethod(String id, String label, int minimumDelay, int delaySpread, int scriptCost,
                     int arrivalWindow, String prerequisite) {
            this.id = id;
            this.label = label;
            this.minimumDelay = minimumDelay;
            this.delaySpread = delaySpread;
            this.scriptCost = scriptCost;
            this.arrivalWindow = arrivalWindow;
            this.prerequisite = prerequisite;
        }

        int delay(Random random) {
            return minimumDelay + (random == null || delaySpread <= 1 ? 0 : random.nextInt(delaySpread));
        }

        String costLine() {
            return scriptCost == 0 ? "free" : scriptCost + " script per person";
        }

        static SourceMethod fromId(String id) {
            if (id != null) for (SourceMethod method : values()) if (method.id.equals(id)) return method;
            return LEGACY_ROSTER;
        }
    }

    record Status(int requested, int inbound, int ready, int routeBlocked, int capacityBlocked,
                  int capacity, int living, int nextDueTurn, int nextExpiryTurn, String line) { }

    record ReceptionResult(boolean success, int arrived, int scriptCost, String message, List<NpcEntity> personnel) { }

    record SourceChangeResult(boolean success, String message, SourceMethod method, int changed) { }

    record TickResult(int casualties, int expired, String message) { }

    private FactionReinforcementAuthority() { }

    static void configureInitialSource(World world, PersonnelReplacementRequest request, int turn, Random random) {
        if (world == null || request == null) return;
        List<SourceMethod> methods = availableMethods(world, request.faction);
        SourceMethod method = methods.isEmpty() ? SourceMethod.LEGACY_ROSTER : methods.get(0);
        if (!applySource(world, request, method, turn, random)) {
            request.sourceMode = SourceMethod.LEGACY_ROSTER.id;
            request.scriptCost = 0;
            request.sourcePrerequisite = SourceMethod.LEGACY_ROSTER.prerequisite;
            request.dueTurn = turn + SourceMethod.LEGACY_ROSTER.delay(random);
            request.expiresTurn = request.dueTurn + SourceMethod.LEGACY_ROSTER.arrivalWindow;
        }
    }

    static SourceChangeResult cycleSource(World world, Faction faction, int turn, Random random) {
        Faction target = faction == null ? Faction.NONE : faction;
        if (world == null || target == Faction.NONE) {
            return new SourceChangeResult(false, "Reinforcement source unavailable: no faction roster is selected.", null, 0);
        }
        ArrayList<PersonnelReplacementRequest> requests = new ArrayList<>();
        for (PersonnelReplacementRequest request : world.replacementQueue) {
            if (matches(request, target) && !expired(request, turn)) requests.add(request);
        }
        if (requests.isEmpty()) {
            return new SourceChangeResult(false, "No open reinforcement manifest is available to reroute.", null, 0);
        }
        List<SourceMethod> methods = availableMethods(world, target);
        if (methods.size() <= 1) {
            SourceMethod only = methods.isEmpty() ? SourceMethod.fromId(requests.get(0).sourceMode) : methods.get(0);
            return new SourceChangeResult(false, "No alternative reinforcement source is currently supported; "
                    + only.label + " requires " + only.prerequisite + ".", only, 0);
        }
        SourceMethod current = SourceMethod.fromId(requests.get(0).sourceMode);
        int index = methods.indexOf(current);
        SourceMethod next = methods.get((index + 1 + methods.size()) % methods.size());
        int changed = 0;
        Random rng = random == null ? new Random(world.seed ^ turn ^ target.ordinal()) : random;
        for (PersonnelReplacementRequest request : requests) if (applySource(world, request, next, turn, rng)) changed++;
        if (changed == 0) {
            return new SourceChangeResult(false, "Could not bind the manifest to " + next.label + ".", next, 0);
        }
        return new SourceChangeResult(true, "Reinforcement source changed for " + changed + " manifest"
                + (changed == 1 ? "" : "s") + ": " + next.label + " / " + next.costLine()
                + " / requires " + next.prerequisite + " / new availability begins in "
                + next.minimumDelay + "-" + (next.minimumDelay + next.delaySpread - 1) + " turns.", next, changed);
    }

    static TickResult tick(World world, int turn, Random random) {
        if (world == null) return new TickResult(0, 0, "No reinforcement world is loaded.");
        int casualties = captureCasualties(world, turn, random);
        int expired = expireRequests(world, turn);
        String message = "Reinforcement lifecycle: casualties " + casualties + ", expired manifests " + expired + ".";
        return new TickResult(casualties, expired, message);
    }

    static int captureCasualties(World world, int turn, Random random) {
        if (world == null || world.npcs == null || world.npcs.isEmpty()) return 0;
        ArrayList<NpcEntity> casualties = new ArrayList<>();
        for (NpcEntity npc : world.npcs) {
            if (npc != null && npc.hp <= 0 && !npc.isUntargetableAnchor()
                    && npc.faction != null && npc.faction != Faction.NONE) casualties.add(npc);
        }
        int captured = 0;
        for (NpcEntity dead : casualties) {
            if (hasRequestFor(world, dead.id)) {
                world.npcs.remove(dead);
                continue;
            }
            PersonnelReplacementRequest request = PersonnelProvenanceApi.recordDeathAndScheduleReplacement(
                    world, dead, turn, random == null ? new Random(world.seed ^ turn) : random,
                    "recorded faction casualty");
            if (request != null) {
                world.npcs.remove(dead);
                captured++;
            }
        }
        return captured;
    }

    static Status status(World world, Faction faction, int turn) {
        Faction target = faction == null ? Faction.NONE : faction;
        if (world == null || target == Faction.NONE) {
            return new Status(0, 0, 0, 0, 0, 0, 0, -1, -1,
                    "Reinforcements unavailable: this representative has no faction roster.");
        }
        int requested = 0;
        int inbound = 0;
        int ready = 0;
        int routeBlocked = 0;
        int capacityBlocked = 0;
        int nextDue = Integer.MAX_VALUE;
        int nextExpiry = Integer.MAX_VALUE;
        PersonnelReplacementRequest nextRequest = null;
        int capacity = PersonnelPopulationApi.replacementCapacityForFaction(world, target);
        int living = PersonnelPopulationApi.countLivingFactionActors(world, target);
        int availableSlots = Math.max(0, capacity - living);
        for (PersonnelReplacementRequest request : world.replacementQueue) {
            if (!matches(request, target) || expired(request, turn)) continue;
            requested++;
            nextDue = Math.min(nextDue, request.dueTurn);
            nextExpiry = Math.min(nextExpiry, expiryTurn(request));
            if (nextRequest == null || request.dueTurn < nextRequest.dueTurn) nextRequest = request;
            if (request.dueTurn > turn) inbound++;
            else if (availableSlots <= 0) capacityBlocked++;
            else if (!routeReady(world, request)) routeBlocked++;
            else ready++;
        }
        String line;
        String sourceTerms = nextRequest == null ? "" : " Source: " + sourceTerms(nextRequest) + ".";
        if (requested == 0) {
            line = "Reinforcements: no casualty replacement manifest is open for " + target.label + ".";
        } else if (ready > 0 && availableSlots > 0) {
            int receivable = Math.min(Math.min(ready, availableSlots), MAX_ARRIVAL_GROUP);
            line = "Reinforcements ready: receive " + receivable + " of " + ready
                    + " waiting personnel; staffed capacity " + living + "/" + capacity
                    + "; arrival window closes on turn " + nextExpiry + "." + sourceTerms;
        } else if (capacityBlocked > 0) {
            line = "Reinforcements waiting: " + capacityBlocked + " personnel are at intake, but staffed capacity is full at "
                    + living + "/" + capacity + "; the earliest manifest expires on turn " + nextExpiry + "." + sourceTerms;
        } else if (routeBlocked > 0) {
            line = "Reinforcements delayed: " + routeBlocked
                    + " ready manifest(s) lack an available local roster or import intake slot; earliest expiry turn "
                    + nextExpiry + "." + sourceTerms;
        } else {
            line = "Reinforcements inbound: " + inbound + " personnel requested; next availability turn "
                    + nextDue + "; staffed capacity " + living + "/" + capacity + "." + sourceTerms;
        }
        return new Status(requested, inbound, ready, routeBlocked, capacityBlocked, capacity, living,
                nextDue == Integer.MAX_VALUE ? -1 : nextDue,
                nextExpiry == Integer.MAX_VALUE ? -1 : nextExpiry, line);
    }

    static String representativeLine(GamePanel game, NpcEntity representative) {
        if (game == null || representative == null || !representative.isFactionRepresentative()) {
            return "Reinforcements unavailable: speak with a faction representative.";
        }
        return status(game.world, representative.faction, game.turn).line()
                + " Carried script: " + Math.max(0, game.carriedScript) + ".";
    }

    static ReceptionResult receive(World world, Faction faction, int turn, Random random) {
        return receive(world, faction, turn, Integer.MAX_VALUE, random);
    }

    static ReceptionResult receive(World world, Faction faction, int turn, int availableScript, Random random) {
        Faction target = faction == null ? Faction.NONE : faction;
        if (world == null || target == Faction.NONE) {
            return new ReceptionResult(false, 0, 0, "Reinforcement reception unavailable: no faction roster is selected.", List.of());
        }
        expireRequests(world, turn);
        ArrayList<PersonnelReplacementRequest> ordered = new ArrayList<>();
        for (PersonnelReplacementRequest request : world.replacementQueue) if (matches(request, target)) ordered.add(request);
        ordered.sort(Comparator.comparingInt(request -> request.dueTurn));
        if (ordered.isEmpty()) {
            return new ReceptionResult(false, 0, 0, "No casualty replacement manifest is open for " + target.label + ".", List.of());
        }

        int capacity = PersonnelPopulationApi.replacementCapacityForFaction(world, target);
        int living = PersonnelPopulationApi.countLivingFactionActors(world, target);
        int inbound = 0;
        int routeBlocked = 0;
        int capacityBlocked = 0;
        int fundsBlocked = 0;
        int spent = 0;
        ArrayList<NpcEntity> arrived = new ArrayList<>();
        Random rng = random == null ? new Random(world.seed ^ turn ^ target.ordinal()) : random;
        for (PersonnelReplacementRequest request : ordered) {
            if (arrived.size() >= MAX_ARRIVAL_GROUP) break;
            if (request.dueTurn > turn) {
                inbound++;
                continue;
            }
            if (living + arrived.size() >= capacity) {
                capacityBlocked++;
                continue;
            }
            if (!routeReady(world, request)) {
                routeBlocked++;
                continue;
            }
            int cost = Math.max(0, request.scriptCost);
            if (spent + cost > Math.max(0, availableScript)) {
                fundsBlocked++;
                continue;
            }
            Point point = PersonnelPopulationApi.spawnPointForReplacement(world, request, rng);
            if (point == null || world.npcAt(point.x, point.y) != null) {
                routeBlocked++;
                continue;
            }
            NpcEntity npc = NpcEntity.create(target, world.zoneType, point.x, point.y, rng);
            npc.state = "Reinforcement Intake";
            PersonnelProvenanceApi.assignReplacementProvenance(npc, world, request, rng);
            NpcPortraitSelectionAuthority.assignForSpawn(npc, world);
            PersonnelPopulationApi.consumeReplacementSlot(world, request, rng);
            world.npcs.add(npc);
            world.replacementQueue.remove(request);
            arrived.add(npc);
            spent += cost;
        }
        if (!arrived.isEmpty()) {
            String source = arrived.get(0).provenance == null ? "recorded intake route" : arrived.get(0).provenance.arrivalRoute;
            String payment = spent == 0 ? "free intake" : "paid " + spent + " script";
            return new ReceptionResult(true, arrived.size(), spent, "Received " + arrived.size() + " " + target.label
                    + " reinforcement" + (arrived.size() == 1 ? "" : "s") + " through " + source
                    + "; " + payment + "; staffed capacity is now " + (living + arrived.size()) + "/" + capacity + ".", List.copyOf(arrived));
        }
        if (capacityBlocked > 0) {
            return new ReceptionResult(false, 0, 0, "Reinforcement reception blocked: staffed housing and faction-room capacity is full at "
                    + living + "/" + capacity + "; the manifest remains open until its expiry turn.", List.of());
        }
        if (routeBlocked > 0) {
            return new ReceptionResult(false, 0, 0, "Reinforcement reception delayed: the selected source prerequisite or intake slot is unavailable; the manifest remains open until expiry.", List.of());
        }
        if (fundsBlocked > 0) {
            int lowest = ordered.stream().filter(request -> request.dueTurn <= turn).mapToInt(request -> Math.max(0, request.scriptCost)).min().orElse(0);
            return new ReceptionResult(false, 0, 0, "Reinforcement reception blocked: selected recruitment costs "
                    + lowest + " script per person; carried funds " + Math.max(0, availableScript)
                    + "; no personnel or payment changed.", List.of());
        }
        return new ReceptionResult(false, 0, 0, "Reinforcements are still inbound; next availability turn "
                + ordered.get(0).dueTurn + ".", List.of());
    }

    static int expireRequests(World world, int turn) {
        if (world == null || world.replacementQueue == null) return 0;
        int expired = 0;
        Iterator<PersonnelReplacementRequest> iterator = world.replacementQueue.iterator();
        while (iterator.hasNext()) {
            PersonnelReplacementRequest request = iterator.next();
            if (request == null || expired(request, turn)) {
                iterator.remove();
                expired++;
            }
        }
        return expired;
    }

    static List<SourceMethod> availableMethods(World world, Faction faction) {
        ArrayList<SourceMethod> methods = new ArrayList<>();
        if (findLedger(world, faction, SourceMethod.TRAIN_IMPORT) != null) methods.add(SourceMethod.TRAIN_IMPORT);
        if (findLedger(world, faction, SourceMethod.BARRACKS_MUSTER) != null) methods.add(SourceMethod.BARRACKS_MUSTER);
        if (findLedger(world, faction, SourceMethod.PAID_LOCAL) != null) methods.add(SourceMethod.PAID_LOCAL);
        return List.copyOf(methods);
    }

    private static boolean applySource(World world, PersonnelReplacementRequest request, SourceMethod method,
                                       int turn, Random random) {
        if (world == null || request == null || method == null) return false;
        RoomPopulationLedger ledger = findLedger(world, request.faction, method);
        if (ledger == null && method != SourceMethod.LEGACY_ROSTER) return false;
        if (ledger != null) {
            request.sourceLedgerId = ledger.id;
            request.sourceRoomId = ledger.roomId;
            request.source = method.label + " through " + ledger.sourceLabel
                    + (ledger.facilityId == null || ledger.facilityId.isBlank() ? "" : " / " + ledger.facilitySummary());
        }
        request.sourceMode = method.id;
        request.scriptCost = method.scriptCost;
        request.sourcePrerequisite = method.prerequisite;
        request.requestedTurn = turn;
        request.dueTurn = turn + method.delay(random);
        request.expiresTurn = request.dueTurn + method.arrivalWindow;
        return true;
    }

    private static RoomPopulationLedger findLedger(World world, Faction faction, SourceMethod method) {
        if (world == null || world.roomPopulationLedgers == null || method == null) return null;
        RoomPopulationLedger fallback = null;
        for (RoomPopulationLedger ledger : world.roomPopulationLedgers) {
            if (ledger == null || ledger.capacity <= 0) continue;
            if (!(ledger.faction == faction || ledger.faction == Faction.NONE)) continue;
            if (method == SourceMethod.PAID_LOCAL) {
                if (fallback == null) fallback = ledger;
                if (!isTrainLedger(ledger) && !isBarracksLedger(ledger)) return ledger;
            } else if (method == SourceMethod.TRAIN_IMPORT && isTrainLedger(ledger)) {
                return ledger;
            } else if (method == SourceMethod.BARRACKS_MUSTER && isBarracksLedger(ledger)) {
                return ledger;
            } else if (method == SourceMethod.LEGACY_ROSTER) {
                return ledger;
            }
        }
        return method == SourceMethod.PAID_LOCAL ? fallback : null;
    }

    private static boolean isTrainLedger(RoomPopulationLedger ledger) {
        String text = ledgerText(ledger);
        return text.contains("rail") || text.contains("train") || text.contains("platform") || text.contains("import intake");
    }

    private static boolean isBarracksLedger(RoomPopulationLedger ledger) {
        String text = ledgerText(ledger);
        return text.contains("barracks") || text.contains("billet") || text.contains("duty")
                || text.contains("drill") || text.contains("guard") || text.contains("security")
                || text.contains("precinct");
    }

    private static String ledgerText(RoomPopulationLedger ledger) {
        if (ledger == null) return "";
        return (safe(ledger.sourceKind) + " " + safe(ledger.sourceLabel) + " " + safe(ledger.roomName)
                + " " + safe(ledger.facilityPurpose) + " " + safe(ledger.facilityProductFocus)).toLowerCase(Locale.ROOT);
    }

    private static String sourceTerms(PersonnelReplacementRequest request) {
        SourceMethod method = SourceMethod.fromId(request == null ? null : request.sourceMode);
        int cost = request == null ? method.scriptCost : Math.max(0, request.scriptCost);
        String prerequisite = request == null || request.sourcePrerequisite == null || request.sourcePrerequisite.isBlank()
                ? method.prerequisite : request.sourcePrerequisite;
        return method.label + " / " + (cost == 0 ? "free" : cost + " script per person")
                + " / requires " + prerequisite;
    }

    static boolean routeReady(World world, PersonnelReplacementRequest request) {
        if (world == null || request == null) return false;
        RoomPopulationLedger ledger = PersonnelPopulationApi.ledgerById(world, request.sourceLedgerId);
        SourceMethod method = SourceMethod.fromId(request.sourceMode);
        boolean sourceMatches = method == SourceMethod.LEGACY_ROSTER
                || method == SourceMethod.PAID_LOCAL
                || (method == SourceMethod.TRAIN_IMPORT && isTrainLedger(ledger))
                || (method == SourceMethod.BARRACKS_MUSTER && isBarracksLedger(ledger));
        return ledger != null && sourceMatches && ledger.available > 0 && ledger.capacity > 0
                && (ledger.faction == request.faction || ledger.faction == Faction.NONE);
    }

    private static boolean hasRequestFor(World world, String npcId) {
        if (world == null || npcId == null) return false;
        for (PersonnelReplacementRequest request : world.replacementQueue) {
            if (request != null && npcId.equals(request.deadNpcId)) return true;
        }
        return false;
    }

    private static boolean matches(PersonnelReplacementRequest request, Faction faction) {
        return request != null && request.faction == faction;
    }

    private static boolean expired(PersonnelReplacementRequest request, int turn) {
        return request == null || turn > expiryTurn(request);
    }

    private static int expiryTurn(PersonnelReplacementRequest request) {
        if (request == null) return 0;
        return request.expiresTurn > 0 ? request.expiresTurn : request.dueTurn + ARRIVAL_WINDOW_TURNS;
    }

    private static String safe(String text) {
        return text == null ? "" : text;
    }
}
