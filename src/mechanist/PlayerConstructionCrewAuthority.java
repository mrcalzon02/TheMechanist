package mechanist;

import java.awt.Point;
import java.util.ArrayDeque;
import java.util.HashMap;

/** Real recruited actors, ordinary NPC turns and player-owned staged construction. */
final class PlayerConstructionCrewAuthority {
    static final int MAX_ROUTE_CELLS = 256;
    private PlayerConstructionCrewAuthority() { }

    record Result(boolean success, String message) { }

    static boolean validSite(GamePanel g, BaseObject s) {
        return g != null && g.world != null && s != null && g.baseObjects.contains(s)
                && s.underConstruction && !FactionPhysicalConstructionAuthority.isFactionManaged(s)
                && g.baseClaimed && g.claimedRoomId >= 0 && g.claimedRoomId < g.world.roomFactions.size()
                && g.world.roomFaction(g.claimedRoomId) == Faction.HIVER
                && RoomOwnershipAuthority.hasAuthoritativePlayerClaim(g, g.claimedRoomId)
                && g.world.inBounds(s.x, s.y) && g.world.roomIdAt(s.x, s.y) == g.claimedRoomId;
    }

    static NpcEntity actor(GamePanel g, RecruitWorker worker) {
        if (g == null || g.world == null || worker == null
                || worker.sourceNpcId == null || worker.sourceNpcId.isBlank()) return null;
        for (NpcEntity npc : g.world.npcs)
            if (npc != null && worker.sourceNpcId.equals(npc.id)) return npc;
        return null;
    }

    static boolean available(NpcEntity n) {
        return n != null && n.hp > 0 && !n.isAnimalActor() && !n.isMinorActor()
                && !n.developerStationary && !"Hostile".equalsIgnoreCase(n.state)
                && !"Left Faction".equalsIgnoreCase(n.state);
    }

    static BaseObject assignedSite(GamePanel g, NpcEntity n) {
        if (g == null || !available(n) || n.id == null || n.id.isBlank()) return null;
        for (RecruitWorker worker : g.factionRecruits) {
            if (worker == null || !n.id.equals(worker.sourceNpcId)) continue;
            for (BaseObject site : g.baseObjects)
                if (site != null && worker.name.equals(site.assignedWorker) && validSite(g, site)) return site;
            return null;
        }
        return null;
    }

    static Result toggle(GamePanel g, BaseObject site) {
        if (!validSite(g, site)) return new Result(false,
                "Crew order blocked: select an unfinished player-owned site inside your claimed base.");
        if (Math.abs(g.playerX - site.x) + Math.abs(g.playerY - site.y) > 1)
            return new Result(false, "Crew order blocked: approach this site to issue or revoke a work order.");
        if (site.assignedWorker != null && !site.assignedWorker.isBlank()) {
            String old = site.assignedWorker;
            site.assignedWorker = "";
            return new Result(true, "Released " + old + "; staged materials and labor progress preserved.");
        }
        for (RecruitWorker worker : g.factionRecruits) {
            NpcEntity n = actor(g, worker);
            if (!available(n)) continue;
            ManualStaffingAssignmentAuthority.clearWorkerFromOtherStations(g, worker.name, site);
            site.assignedWorker = worker.name;
            return new Result(true, "Assigned " + worker.name + " to " + site.name + "."
                    + " The worker must physically approach the site; stage materials with Work first.");
        }
        return new Result(false, "Crew order blocked: no living hired adult is present in this zone. Hire a resident first.");
    }

    static String status(GamePanel g, BaseObject site) {
        if (site == null || !site.underConstruction) return "Construction crew: no unfinished site.";
        if (site.assignedWorker == null || site.assignedWorker.isBlank())
            return "Construction crew: unassigned. Hire an NPC, then Assign Crew at this site.";
        if (!validSite(g, site)) return "Construction crew: work order suspended by base ownership.";
        for (RecruitWorker w : g.factionRecruits) {
            if (w == null || !w.name.equals(site.assignedWorker)) continue;
            NpcEntity n = actor(g, w);
            if (!available(n)) return "Construction crew: " + w.name + " absent or incapacitated.";
            return "Construction crew: " + w.name + " distance "
                    + (Math.abs(n.x - site.x) + Math.abs(n.y - site.y))
                    + "; missing materials: " + ProgressiveConstructionAuthority.missingMaterials(site) + ".";
        }
        return "Construction crew: stale work order. Release and assign a hired NPC.";
    }

    static boolean hasDuty(GamePanel g, NpcEntity n) { return assignedSite(g, n) != null; }

    /** Called only on this actor's selected NPC turn, after normal needs/routine updates. */
    static void takeNpcTurn(GamePanel g, NpcEntity n) {
        BaseObject site = assignedSite(g, n);
        if (site == null || g == null || n == null) return;
        if ("Sleep".equals(n.state) || "Search Needs".equals(n.state)
                || n.hunger > 14 || n.thirst > 14 || n.sleepDebt > 18) return;
        if (g.turn % Math.max(1, n.idleBias) != 0) return;
        if (!"none".equals(ProgressiveConstructionAuthority.missingMaterials(site))) {
            n.state = "Awaiting Construction Materials";
            return;
        }
        if (Math.abs(n.x-site.x) + Math.abs(n.y-site.y) > 1) {
            Point p = nextStep(g, n, site);
            if (p == null || !n.canNpcStep(g.world, p.x, p.y, g.playerX, g.playerY)) {
                n.state = "Construction Route Blocked";
                return;
            }
            n.moveTo(p.x, p.y);
            n.state = "Travel To Construction";
            return;
        }
        ProgressiveConstructionAuthority.WorkerWorkResult work =
                ProgressiveConstructionAuthority.contributeWorker(g, site, 1, n.name);
        n.state = work.completed() ? "Construction Complete"
                : (work.advanced() ? "Constructing" : "Awaiting Construction Materials");
    }

    /** Deterministic bounded BFS to a walkable adjacent tile: never steps into a ghost site. */
    private static Point nextStep(GamePanel g, NpcEntity n, BaseObject site) {
        World w = g.world;
        ArrayDeque<Point> queue = new ArrayDeque<>();
        HashMap<Long, Point> first = new HashMap<>();
        Point start = new Point(n.x, n.y);
        queue.add(start);
        first.put(key(start.x, start.y), start);
        int[] dx = {1, 0, -1, 0}, dy = {0, 1, 0, -1};
        while (!queue.isEmpty() && first.size() <= MAX_ROUTE_CELLS) {
            Point at = queue.removeFirst();
            if (Math.abs(at.x-site.x)+Math.abs(at.y-site.y) == 1)
                return at.equals(start) ? null : first.get(key(at.x, at.y));
            for (int k=0;k<4;k++) {
                int x=at.x+dx[k], y=at.y+dy[k];
                long id=key(x,y);
                if (first.containsKey(id) || !w.inBounds(x,y) || !w.walkable(x,y)
                        || (x==site.x && y==site.y)
                        || (x==g.playerX && y==g.playerY)
                        || !DeveloperBattleMapAuthority.step(w,at.x,at.y,x,y)) continue;
                NpcEntity occupant=w.npcAt(x,y);
                if (occupant!=null && occupant!=n) continue;
                first.put(id, at.equals(start) ? new Point(x,y) : first.get(key(at.x,at.y)));
                queue.addLast(new Point(x,y));
                if (first.size()>=MAX_ROUTE_CELLS) break;
            }
        }
        return null;
    }

    private static long key(int x,int y) { return ((long)x << 32) | (y & 0xffffffffL); }
}
