package mechanist;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import javax.sound.sampled.*;
import java.io.*;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;

class FactionStrategicPlan {
    String id = "plan.unassigned";
    Faction faction = Faction.NONE;
    Faction schemeTargetFaction = Faction.NONE;
    String leaderName = "unrecorded leader";
    String deputyName = "unrecorded deputy";
    String phase = "PLANNING";
    String immediateGoal = "stockpile a useful item";
    String longTermGoal = "increase faction control";
    String personalGoal = "survive local politics";
    String scheme = "";
    String targetItem = "Emergency rations";
    String targetRoom = "unselected room";
    int phaseUntilTurn = 0;
    int nextDecisionTurn = 0;
    int success = 0;
    int failure = 0;
    int secrecy = 35;
    int aggression = 35;
    int ambition = 35;
    String lastOutcome = "No action has resolved yet.";
    final ArrayList<String> history = new ArrayList<>();

    static FactionStrategicPlan create(Faction f, Random r, int turn) {
        if (r == null) r = new Random();
        FactionStrategicPlan p = new FactionStrategicPlan();
        p.faction = f == null ? Faction.NONE : f;
        p.id = "STRAT-" + p.faction.name() + "-" + Math.abs(Objects.hash(p.faction.name(), turn, r.nextInt()));
        p.leaderName = CharacterCreationAuthority.randomNpcName(p.faction, r);
        p.deputyName = CharacterCreationAuthority.randomNpcName(p.faction, r);
        p.ambition = 25 + r.nextInt(65);
        p.aggression = aggressionFor(p.faction, r);
        p.secrecy = secrecyFor(p.faction, r);
        p.longTermGoal = pick(longTermGoals(p.faction), r);
        p.personalGoal = pick(personalGoals(p.faction), r);
        p.chooseNewImmediateGoal(r, turn);
        p.history.add("Founded strategic ledger under " + p.leaderName + "; long-term goal: " + p.longTermGoal + ".");
        return p;
    }

    void chooseNewImmediateGoal(Random r, int turn) {
        if (r == null) r = new Random();
        immediateGoal = pick(immediateGoals(faction), r);
        schemeTargetFaction = pickTargetFaction(faction, r);
        targetItem = pick(targetItemsForGoal(immediateGoal, faction), r);
        targetRoom = pick(targetRoomsForGoal(immediateGoal, faction), r);
        scheme = r.nextInt(100) < schemeChance() ? pick(schemesFor(faction), r) : "";
        phase = "PLANNING";
        int planHours = 18 + r.nextInt(54);
        phaseUntilTurn = turn + planHours * GamePanel.TURNS_PER_HOUR;
        nextDecisionTurn = phaseUntilTurn;
        addHistory(turn, "Planning selected: " + immediateGoal + "; target item " + targetItem + "; target room " + targetRoom + (scheme.isBlank()?"":"; private scheme " + scheme + " vs " + schemeTargetFaction.label) + ".");
    }

    int schemeChance() {
        int base = 6 + secrecy/5 + ambition/8;
        if (faction == Faction.CULTIST || faction == Faction.HERETIC || faction == Faction.BANDIT || (faction != null && faction.name().startsWith("GANGER"))) base += 18;
        if (faction == Faction.NOBLE || (faction != null && faction.name().startsWith("NOBLE"))) base += 12;
        return Math.max(4, Math.min(70, base));
    }

    void advancePhase(Random r, int turn) {
        if ("PLANNING".equals(phase)) {
            phase = "EXECUTION";
            int hours = 10 + (r == null ? 0 : r.nextInt(38));
            phaseUntilTurn = turn + hours * GamePanel.TURNS_PER_HOUR;
            addHistory(turn, "Execution opened for: " + immediateGoal + ".");
        } else if ("EXECUTION".equals(phase)) {
            phase = "COOLDOWN";
            int hours = 16 + (r == null ? 0 : r.nextInt(42));
            phaseUntilTurn = turn + hours * GamePanel.TURNS_PER_HOUR;
            addHistory(turn, "Execution resolved: " + lastOutcome + ". Cooling down and covering paperwork.");
        } else {
            chooseNewImmediateGoal(r, turn);
        }
        nextDecisionTurn = phaseUntilTurn;
    }

    void addHistory(int turn, String line) {
        history.add("T" + turn + " " + line);
        while (history.size() > 24) history.remove(0);
    }

    String journalHeader() { return faction.label + " ledger of " + leaderName + " with deputy " + deputyName; }
    String shortLine() { return faction.label + " | " + leaderName + " | " + phase + " until " + phaseUntilTurn + " | " + immediateGoal + " | long: " + longTermGoal + (scheme == null || scheme.isBlank() ? "" : " | scheme: " + scheme); }
    String publicLine() { return faction.label + " is rumored to be in " + phase.toLowerCase(Locale.ROOT) + " around " + publicGoalPhrase(immediateGoal) + "."; }

    String saveLine() {
        return enc(id)+"|"+(faction==null?Faction.NONE.name():faction.name())+"|"+enc(leaderName)+"|"+enc(deputyName)+"|"+enc(phase)+"|"+enc(immediateGoal)+"|"+enc(longTermGoal)+"|"+enc(personalGoal)+"|"+enc(scheme)+"|"+(schemeTargetFaction==null?Faction.NONE.name():schemeTargetFaction.name())+"|"+enc(targetItem)+"|"+enc(targetRoom)+"|"+phaseUntilTurn+"|"+nextDecisionTurn+"|"+success+"|"+failure+"|"+secrecy+"|"+aggression+"|"+ambition+"|"+enc(lastOutcome)+"|"+enc(String.join("~~", history));
    }
    static FactionStrategicPlan parse(String s) {
        try {
            String[] a=s.split("\\|",21); if(a.length<20) return null;
            FactionStrategicPlan p=new FactionStrategicPlan();
            p.id=dec(a[0]); p.faction=Faction.valueOf(a[1]); p.leaderName=dec(a[2]); p.deputyName=dec(a[3]); p.phase=dec(a[4]); p.immediateGoal=dec(a[5]); p.longTermGoal=dec(a[6]); p.personalGoal=dec(a[7]); p.scheme=dec(a[8]); p.schemeTargetFaction=Faction.valueOf(a[9]); p.targetItem=dec(a[10]); p.targetRoom=dec(a[11]); p.phaseUntilTurn=Integer.parseInt(a[12]); p.nextDecisionTurn=Integer.parseInt(a[13]); p.success=Integer.parseInt(a[14]); p.failure=Integer.parseInt(a[15]); p.secrecy=Integer.parseInt(a[16]); p.aggression=Integer.parseInt(a[17]); p.ambition=Integer.parseInt(a[18]); p.lastOutcome=dec(a[19]);
            if(a.length>=21){ String h=dec(a[20]); if(!h.isBlank()) for(String line:h.split("~~")) if(!line.isBlank()) p.history.add(line); }
            return p;
        } catch(Exception e) { return null; }
    }
    static String enc(String s){ return Base64.getUrlEncoder().withoutPadding().encodeToString((s==null?"":s).getBytes(java.nio.charset.StandardCharsets.UTF_8)); }
    static String dec(String s){ try{return new String(Base64.getUrlDecoder().decode(s), java.nio.charset.StandardCharsets.UTF_8);}catch(Exception e){return "";} }
    static String pick(String[] a, Random r){ return a[Math.floorMod(r==null?0:r.nextInt(), a.length)]; }
    static int aggressionFor(Faction f, Random r){ int b = (f==Faction.MUTANT||f==Faction.CULTIST||f==Faction.HERETIC||f==Faction.BANDIT||(f!=null&&f.name().startsWith("GANGER")))?62:32; if(f==Faction.ARBITES||f==Faction.IMPERIAL_GUARD||f==Faction.SORORITAS) b=48; return Math.max(5, Math.min(95, b+(r==null?0:r.nextInt(31)-15))); }
    static int secrecyFor(Faction f, Random r){ int b = (f==Faction.CULTIST||f==Faction.HERETIC||f==Faction.NOBLE||(f!=null&&f.name().startsWith("NOBLE")))?64:35; if(f==Faction.ARBITES||f==Faction.ADMINISTRATUM) b=48; return Math.max(5, Math.min(95, b+(r==null?0:r.nextInt(31)-15))); }
    static String publicGoalPhrase(String g){ if(g==null) return "unreadable business"; if(g.toLowerCase(Locale.ROOT).contains("assassin")) return "a sudden death"; if(g.toLowerCase(Locale.ROOT).contains("steal")) return "missing stock"; if(g.toLowerCase(Locale.ROOT).contains("attack")) return "open violence"; if(g.toLowerCase(Locale.ROOT).contains("stockpile")) return "quiet accumulation"; return g; }
    static Faction pickTargetFaction(Faction self, Random r){ Faction[] pool={Faction.HIVER,Faction.BANDIT,Faction.ARBITES,Faction.IMPERIAL_GUARD,Faction.MECHANICUS,Faction.NOBLE,Faction.CULTIST,Faction.MUTANT,Faction.ADMINISTRATUM}; Faction f=pool[Math.floorMod(r==null?1:r.nextInt(), pool.length)]; return f==self?Faction.BANDIT:f; }
    static String[] immediateGoals(Faction f){ return new String[]{"take control of a room","build or upgrade a factory","begin production of a recipe","stockpile a strategic item","gain more followers","attack an enemy faction","steal from an enemy stockpile","assassinate a rival officer","bribe a local official","open a trade route","repair a damaged facility","sabotage a rival machine","secure food and water reserves","secure ammunition reserves","recruit guards for a room"}; }
    static String[] longTermGoals(Faction f){ return new String[]{"dominate local room control","secure a stable production chain","monopolize a useful commodity","expand follower count","weaken nearest rival","gain leverage over civil authorities","protect faction leadership","capture a profitable corridor route","build an armed reserve","create a hidden contingency stockpile","make the zone dependent on faction services","prepare for a larger political coup"}; }
    static String[] personalGoals(Faction f){ return new String[]{"advance one rank before the next audit","remove a deputy who knows too much","be seen as indispensable","hide evidence of misused stock","secure a safer room and better guards","increase personal wealth","win favor from a superior","pay off a dangerous obligation","avoid being blamed for the next failure","make a rival look incompetent"}; }
    static String[] schemesFor(Faction f){ return new String[]{"sell information","arrange a betrayal","frame a subordinate","hire an assassin","divert faction stock","leak patrol routes","plant contraband evidence","make a private deal","blackmail a rival officer","poison a contract negotiation","open a back door during an attack","fake a shortage to gain leverage"}; }
    static String[] targetItemsForGoal(String g, Faction f){ String l=g==null?"":g.toLowerCase(Locale.ROOT); if(l.contains("ammo")||l.contains("attack")||l.contains("guard")) return new String[]{"Las charge pack","Autogun magazine","Stub cartridge box","Frag grenade","Guard flak vest"}; if(l.contains("food")||l.contains("water")) return new String[]{"Clean water","Water bottle","Emergency rations","Amino culture broth"}; if(l.contains("factory")||l.contains("production")||l.contains("repair")) return new String[]{"Machine part","Sacred wire bundle","Construction supplies","Assembled component"}; if(l.contains("steal")||l.contains("bribe")) return new String[]{"Trade chit","Lockpicks","Data spike","Sealed bank lockbox"}; return new String[]{"Emergency rations","Clean water","Machine part","Autogun magazine","Medkit"}; }
    static String[] targetRoomsForGoal(String g, Faction f){ return new String[]{"watch post","stockroom","machine room","shrine annex","dormitory block","service corridor","kitchen","barracks room","archive desk","trade counter","water room","munition locker"}; }
}


class FactionStrategySimulationApi {
    private FactionStrategySimulationApi() {}
    static void ensurePlans(GamePanel g) {
        if (g == null) return;
        if (!g.factionStrategicPlans.isEmpty()) return;
        Random r = new Random((g.seed == 0 ? System.currentTimeMillis() : g.seed) ^ 0x5FACC10EL);
        Faction[] core = {Faction.HIVER, Faction.BANDIT, Faction.ARBITES, Faction.IMPERIAL_GUARD, Faction.MECHANICUS, Faction.NOBLE, Faction.CULTIST, Faction.MUTANT, Faction.ADMINISTRATUM, Faction.MINISTORUM, Faction.INN, Faction.SORORITAS};
        for (Faction f : core) g.factionStrategicPlans.add(FactionStrategicPlan.create(f, r, Math.max(0, g.turn)));
        g.lastFactionSimulationReport = "seeded " + g.factionStrategicPlans.size() + " faction leader ledgers with intent/execution/cooldown stages";
        g.lastPublicNewsBulletin = buildPublicNews(g, r);
        ImperialNewsNetworkApi.ensureDailyIssue(g, Math.max(0, g.turn / Math.max(1, GamePanel.TURNS_PER_HOUR * GamePanel.HOURS_PER_DAY)), r);
        DebugLog.audit("FACTION_STRATEGY_SEED", g.lastFactionSimulationReport);
    }
    static void tick(GamePanel g, boolean sleeping) {
        if (g == null) return;
        ensurePlans(g);
        if (g.turn <= 0) return;
        int day = g.turn / (GamePanel.TURNS_PER_HOUR * GamePanel.HOURS_PER_DAY);
        if (day != g.lastFactionSimulationDay && g.turn % (6 * GamePanel.TURNS_PER_HOUR) == 0) {
            g.lastFactionSimulationDay = day;
            g.lastPublicNewsBulletin = buildPublicNews(g, g.rng);
            ImperialNewsNetworkApi.ensureDailyIssue(g, day, g.rng);
        }
        int changed = 0;
        for (FactionStrategicPlan p : g.factionStrategicPlans) {
            if (p == null || g.turn < p.phaseUntilTurn) continue;
            if ("EXECUTION".equals(p.phase)) resolveExecution(g, p);
            p.advancePhase(g.rng, g.turn);
            changed++;
        }
        if (changed > 0) {
            g.lastFactionSimulationReport = "advanced " + changed + " faction plan(s) at turn " + g.turn + "; " + g.lastPublicNewsBulletin;
            DebugLog.audit("FACTION_STRATEGY_TICK", g.lastFactionSimulationReport);
        }
        if (g.world != null && g.turn % (2 * GamePanel.TURNS_PER_HOUR) == 0) ensureLocalJournals(g);
    }
    static void resolveExecution(GamePanel g, FactionStrategicPlan p) {
        if (g == null || p == null) return;
        NpcFactionSite site = g.siteForFaction(p.faction, g.world == null ? null : g.world.zoneType);
        int strength = 30 + p.ambition/2 + p.aggression/4 + (site == null ? 0 : site.baseLevel*6 + site.workers);
        int difficulty = 35 + Math.abs(Objects.hash(p.immediateGoal, p.schemeTargetFaction, g.turn)) % 55;
        boolean ok = strength + g.rng.nextInt(40) >= difficulty;
        if (ok) { p.success++; applySuccess(g, p, site); }
        else { p.failure++; applyFailure(g, p); }
    }
    static void applySuccess(GamePanel g, FactionStrategicPlan p, NpcFactionSite site) {
        String goal = p.immediateGoal == null ? "" : p.immediateGoal.toLowerCase(Locale.ROOT);
        if (site != null) {
            if (goal.contains("stockpile") || goal.contains("secure") || goal.contains("production")) {
                site.stock = Math.min(160, site.stock + 6 + p.ambition/12);
                materializeFactionStock(g, site, p.targetItem, 1 + p.ambition/35, "strategic plan success: " + p.immediateGoal);
            }
            if (goal.contains("factory") || goal.contains("repair")) { site.baseLevel = Math.min(6, site.baseLevel + 1); site.machineLevel = Math.min(7, site.machineLevel + 1); }
            if (goal.contains("follower") || goal.contains("guard")) site.workers = Math.min(24, site.workers + 1 + p.ambition/40);
        }
        if (goal.contains("attack") || goal.contains("steal") || goal.contains("sabotage") || goal.contains("assassin")) g.addFactionMarketPressure(p.schemeTargetFaction, 2 + p.aggression/25, p.faction.label + " strategic action: " + p.immediateGoal);
        p.lastOutcome = "SUCCESS: " + p.faction.label + " completed " + p.immediateGoal + " around " + p.targetRoom + ".";
        p.addHistory(g.turn, p.lastOutcome);
        if (g.rng.nextInt(100) < Math.max(10, 65 - p.secrecy/2)) g.logEvent("RUMOR: " + p.publicLine());
    }
    static void applyFailure(GamePanel g, FactionStrategicPlan p) {
        p.lastOutcome = "FAILURE: " + p.faction.label + " failed to complete " + p.immediateGoal + "; blame is being routed through subordinate ledgers.";
        p.addHistory(g.turn, p.lastOutcome);
        g.addFactionMarketPressure(p.faction, 1, "failed internal plan spilling into local politics");
        if (g.rng.nextInt(100) < 30) g.logEvent("RUMOR: " + p.faction.label + " botched something involving " + p.targetRoom + ".");
    }
    static void materializeFactionStock(GamePanel g, NpcFactionSite site, String item, int count, String route) {
        if (g == null || site == null || item == null || item.isBlank() || count <= 0) return;
        String cid = g.factionStockContainerId(site);
        g.ensureContainer(cid, site.name + " strategic stock");
        for (int i=0; i<count; i++) {
            ItemProvenanceRecord pr = ItemProvenanceRecord.of(item, site.faction, site.name, g.world, g.turn, site.recipeSummaryFor(item), route);
            g.addItemToContainerResult(cid, site.name + " strategic stock", item, pr, null, "materialize faction strategic stock");
        }
        DebugLog.audit("FACTION_STRATEGIC_STOCK", "site=" + site.name + " item=" + item + " count=" + count + " container=" + cid + " turn=" + g.turn);
    }
    static void ensureLocalJournals(GamePanel g) {
        if (g == null || g.world == null || g.world.rooms == null || g.world.rooms.isEmpty()) return;
        int placed = 0;
        for (FactionStrategicPlan p : g.factionStrategicPlans) {
            if (p == null || p.faction == Faction.NONE) continue;
            if (journalExists(g.world, p.id)) continue;
            int room = roomForFaction(g.world, p.faction);
            if (room < 0) continue;
            Point pt = g.world.randomOpenPointInRoom(g.world.rooms.get(room));
            if (pt == null || g.world.npcAt(pt.x, pt.y) != null) continue;
            g.world.tiles[pt.x][pt.y] = 'o';
            g.world.mapObjects.add(MapObjectState.factionJournal(pt.x, pt.y, p, g.world.zoneType));
            placed++;
            if (placed >= 2) break; // avoid flooding one slice; journals appear in rooms the faction actually holds
        }
        if (placed > 0) DebugLog.audit("FACTION_JOURNAL_PLACE", "placed=" + placed + " zone=" + g.world.zoneType.label + " turn=" + g.turn);
    }
    static boolean journalExists(World w, String id) { if (w == null || id == null) return false; for (MapObjectState m : w.mapObjects) if (m != null && "faction-journal".equals(m.type) && id.equals(m.stockState)) return true; return false; }
    static int roomForFaction(World w, Faction f) { if (w == null || f == null) return -1; for (int i=1; i<w.roomFactions.size() && i<w.rooms.size(); i++) { Faction rf=w.roomFactions.get(i); if (rf == f || GamePanel.sameFactionFamilyStatic(rf, f)) return i; } return -1; }
    static String buildPublicNews(GamePanel g, Random r) {
        ensurePlans(g);
        if (g.factionStrategicPlans.isEmpty()) return "No public faction activity detected.";
        ArrayList<FactionStrategicPlan> copy = new ArrayList<>(g.factionStrategicPlans);
        Collections.shuffle(copy, r == null ? new Random() : r);
        ArrayList<String> bits = new ArrayList<>();
        for (FactionStrategicPlan p : copy) {
            if (bits.size() >= 3) break;
            int discover = 35 + ("EXECUTION".equals(p.phase) ? 20 : 0) - p.secrecy/3;
            if ((r == null ? 50 : r.nextInt(100)) < Math.max(8, discover)) bits.add(p.publicLine());
        }
        if (bits.isEmpty()) return "Public news is all hymns, price bulletins, and careful omissions.";
        return String.join(" ", bits);
    }
}



class InnBountyApi {
    private InnBountyApi() {}
    static int journalBounty(String item, FactionStrategicPlan p) {
        if (p != null) return 5000 + Math.max(0, p.ambition - 50) * 20 + Math.max(0, p.secrecy - 50) * 15;
        if (item == null) return 5000;
        String low = item.toLowerCase(Locale.ROOT);
        if (low.contains("deputy")) return 2500;
        if (low.contains("supervisor") || low.contains("officer")) return 1200;
        return 5000;
    }
}


class BankProfile {
    final String id, label, noblePatron, reputation;
    final int openFee, kioskFee, branchFee;
    final boolean lowerHive, midHive, upperHive, atms, branches, arbitesFineProtected;
    BankProfile(String id, String label, String noblePatron, String reputation, int openFee, int kioskFee, int branchFee, boolean lowerHive, boolean midHive, boolean upperHive, boolean atms, boolean branches, boolean protectedFromFines) {
        this.id=id; this.label=label; this.noblePatron=noblePatron; this.reputation=reputation; this.openFee=openFee; this.kioskFee=kioskFee; this.branchFee=branchFee;
        this.lowerHive=lowerHive; this.midHive=midHive; this.upperHive=upperHive; this.atms=atms; this.branches=branches; this.arbitesFineProtected=protectedFromFines;
    }
    static final BankProfile[] ALL = new BankProfile[]{
        new BankProfile("sump-ledger-mutual", "SumpLedger Mutual Credit", "House Toll", "lower-hive reach, high kiosk coverage, rough service, and an alarming comfort with risk", 25, 2, 1, true, true, false, true, true, false),
        new BankProfile("kastor-civic-credit", "Kastor Civic Credit", "House Kastor", "mid-hive respectability, tolerable queues, better clerks, and branch-led service", 100, 1, 0, false, true, true, true, true, false),
        new BankProfile("varn-crown-trust", "Varn Crown Trust", "House Varn", "upper-hive security, marble contempt, and accounts difficult for Arbites fines to touch", 1000, 0, 0, false, false, true, false, true, true)
    };
    static BankProfile byId(String id) { for (BankProfile b : ALL) if (b.id.equals(id)) return b; return ALL[0]; }
    boolean validFor(World w) {
        if (w == null) return false;
        if (w.floor <= 3) return lowerHive;
        if (w.floor <= 6) return midHive;
        return upperHive;
    }
}


class BankingApi {
    private BankingApi() {}
    static void seedBankObjects(World w, Random r) {
        if (w == null || w.rooms == null || w.rooms.size() <= 1) return;
        int placed = 0;
        for (BankProfile b : BankProfile.ALL) {
            if (!b.validFor(w)) continue;
            boolean upperNoble = w.zoneType == ZoneType.NOBLE_SERVICE_SPINE || w.zoneType == ZoneType.SECTOR_GOVERNORS_MANSION || w.floor >= 7;
            boolean civic = w.zoneType == ZoneType.NEUTRAL_CIVILIAN_FLOOR || w.zoneType == ZoneType.HAB_STACK || w.zoneType == ZoneType.NEUTRAL_RAIL_DEPOT || w.zoneType == ZoneType.SUMP_MARKET || w.zoneType == ZoneType.ADMINISTRATUM_ARCHIVE || w.zoneType == ZoneType.IMPERIAL_NEWS_NETWORK;
            boolean wantBranch = b.branches && (upperNoble || (b.midHive && civic && w.floor >= 4) || (b.lowerHive && civic && (r == null || r.nextInt(100) < 55)));
            boolean wantAtm = b.atms && !upperNoble && (civic || w.floor <= 5) && (r == null || r.nextInt(100) < (b.lowerHive ? 88 : 58));
            if (wantBranch) placed += placeOne(w, r, b, true);
            if (wantAtm) placed += placeOne(w, r, b, false);
        }
        boolean civicZone = w.zoneType == ZoneType.NEUTRAL_CIVILIAN_FLOOR || w.zoneType == ZoneType.HAB_STACK || w.zoneType == ZoneType.NEUTRAL_RAIL_DEPOT || w.zoneType == ZoneType.SUMP_MARKET || w.zoneType == ZoneType.ADMINISTRATUM_ARCHIVE || w.zoneType == ZoneType.IMPERIAL_NEWS_NETWORK;
        if (civicZone && placed < 2) {
            for (BankProfile b : BankProfile.ALL) {
                if (placed >= 2) break;
                if (b.validFor(w) && b.atms) placed += placeOne(w, r, b, false);
            }
        }
        if (placed > 0) DebugLog.audit("BANK_ZONE_OBJECTS", "zone=" + w.zoneType.label + " floor=" + w.floor + " terminals=" + placed + " layer=" + w.layerText());
    }
    static int placeOne(World w, Random r, BankProfile b, boolean branch) {
        for (int tries=0; tries<12; tries++) {
            int idx = 1 + Math.floorMod(r == null ? tries : r.nextInt(Math.max(1, w.rooms.size()-1)), Math.max(1, w.rooms.size()-1));
            Rectangle rr = w.rooms.get(idx);
            Point p = w.randomObjectPointInRoom(rr);
            if (p == null || w.npcAt(p.x,p.y) != null || w.mapObjectAt(p.x,p.y) != null || w.isDoorAccessReservedForObject(p.x,p.y)) continue;
            char under = w.tiles[p.x][p.y];
            w.tiles[p.x][p.y] = '$';
            w.mapObjects.add(MapObjectState.bankTerminal(p.x,p.y,b,branch,w.zoneType,under));
            if (branch) placeBranchSecurity(w, r, b, rr, p);
            return 1;
        }
        return 0;
    }
    static void placeBranchSecurity(World w, Random r, BankProfile b, Rectangle rr, Point terminal) {
        if (w == null || rr == null || b == null) return;
        Point vault = null;
        for (int tries=0; tries<20 && vault==null; tries++) {
            Point q = w.randomObjectPointInRoom(rr);
            if (q == null || q.equals(terminal) || w.npcAt(q.x,q.y)!=null || w.mapObjectAt(q.x,q.y)!=null || w.isDoorAccessReservedForObject(q.x,q.y)) continue;
            if (Math.abs(q.x-terminal.x)+Math.abs(q.y-terminal.y) < 2) continue;
            vault = q;
        }
        if (vault != null) {
            char under = w.tiles[vault.x][vault.y];
            w.tiles[vault.x][vault.y] = 'X';
            w.mapObjects.add(MapObjectState.bankVault(vault.x, vault.y, b, w.zoneType, under));
        }
        Point panel = null;
        for (int tries=0; tries<18 && panel==null; tries++) {
            Point q = w.randomObjectPointInRoom(rr);
            if (q == null || q.equals(terminal) || (vault != null && q.equals(vault)) || w.npcAt(q.x,q.y)!=null || w.mapObjectAt(q.x,q.y)!=null || w.isDoorAccessReservedForObject(q.x,q.y)) continue;
            panel = q;
        }
        if (panel != null) {
            char under = w.tiles[panel.x][panel.y];
            w.tiles[panel.x][panel.y] = 'a';
            w.mapObjects.add(MapObjectState.bankAlarmPanel(panel.x, panel.y, b, w.zoneType, under));
        }
        int guards = b.upperHive ? 3 : (b.midHive ? 2 : 1);
        for (int gi=0; gi<guards; gi++) placeBankGuard(w, r, b, rr, terminal, vault);
        Point manager = null;
        for (int tries=0; tries<20 && manager==null; tries++) {
            Point q = w.randomOpenPointInRoom(rr);
            if (q == null || q.equals(terminal) || (vault != null && q.equals(vault)) || w.npcAt(q.x,q.y)!=null || w.mapObjectAt(q.x,q.y)!=null) continue;
            manager = q;
        }
        if (manager != null) {
            NpcEntity n = NpcEntity.create(Faction.NOBLE, w.zoneType, manager.x, manager.y, r == null ? new Random(Objects.hash(b.id, manager.x, manager.y)) : r);
            n.role = "Bank Manager";
            n.state = "Vault Desk";
            n.symbol = 'n';
            n.name = "Manager " + (n.name == null ? b.label : n.name.split(" ")[0]) + " of " + b.label;
            n.factionRank = b.upperHive ? 3 : (b.midHive ? 4 : 5);
            n.factionRankTitle = "Bank Manager";
            n.factionRankScope = b.label + " branch authority and vault access control";
            n.intellect = Math.max(7, n.intellect + (b.upperHive ? 4 : (b.midHive ? 2 : 0)));
            n.id = "BANK-MANAGER-" + Math.abs(Objects.hash(b.id, manager.x, manager.y, w.locationKey()));
            PersonnelPopulationApi.attachExistingNpcToRoomLedger(n, w, Math.max(0, w.roomIdAt(manager.x, manager.y)), r == null ? new Random(1) : r);
            w.npcs.add(n);
        }
    }
    static void placeBankGuard(World w, Random r, BankProfile b, Rectangle rr, Point terminal, Point vault) {
        if (w == null || rr == null || b == null) return;
        Point guard = null;
        for (int tries=0; tries<24 && guard==null; tries++) {
            Point q = w.randomOpenPointInRoom(rr);
            if (q == null || q.equals(terminal) || (vault != null && q.equals(vault)) || w.npcAt(q.x,q.y)!=null || w.mapObjectAt(q.x,q.y)!=null) continue;
            guard = q;
        }
        if (guard == null) return;
        NpcEntity n = NpcEntity.create(Faction.NOBLE, w.zoneType, guard.x, guard.y, r == null ? new Random(Objects.hash(b.id, guard.x, guard.y, "guard")) : r);
        n.role = b.upperHive ? "Vault Guard" : "Bank Security Guard";
        n.state = "Branch Guard";
        n.symbol = 'n';
        n.name = (b.upperHive ? "Vault Guard " : "Bank Guard ") + (n.name == null ? b.label : n.name.split(" ")[0]) + " of " + b.label;
        n.factionRank = b.upperHive ? 4 : (b.midHive ? 5 : 6);
        n.factionRankTitle = n.role;
        n.factionRankScope = b.label + " branch security and vault alarm response";
        n.intellect = Math.max(5, n.intellect + (b.upperHive ? 2 : 0));
        n.equippedMeleeWeapon = b.upperHive ? "Shock maul" : "Club";
        n.equippedRangedWeapon = b.upperHive ? "Lasgun" : (b.midHive ? "Autopistol" : "Stub pistol");
        n.equippedArmor = b.upperHive ? "Carapace vest" : (b.midHive ? "Flak vest" : "Padded armor");
        n.loadedShots = Math.max(6, n.loadedShots);
        n.ammoReloadsRemaining = Math.max(2, n.ammoReloadsRemaining);
        n.id = "BANK-GUARD-" + Math.abs(Objects.hash(b.id, guard.x, guard.y, w.locationKey(), n.role));
        PersonnelPopulationApi.attachExistingNpcToRoomLedger(n, w, Math.max(0, w.roomIdAt(guard.x, guard.y)), r == null ? new Random(2) : r);
        w.npcs.add(n);
    }
}


class BankHeistApi {
    private BankHeistApi() {}
    static ArrayList<String> vaultLootFor(BankProfile b, Random r) {
        if (r == null) r = new Random(1);
        ArrayList<String> out = new ArrayList<>();
        int rolls = b == null ? 2 : (b.upperHive ? 4 : (b.midHive ? 3 : 2));
        String[] low = {"Trade chit", "Sealed bank lockbox", "Bank manager keycard", "Stock certificate bundle"};
        String[] mid = {"Sealed bank lockbox", "Stock certificate bundle", "Noble Commerce Permit", "Secure vault key"};
        String[] high = {"House gold ingot", "Sealed bank lockbox", "Stock certificate bundle", "Governor signet", "Noble Commerce Permit"};
        String[] pool = b != null && b.upperHive ? high : (b != null && b.midHive ? mid : low);
        for (int i=0;i<rolls;i++) out.add(pool[Math.floorMod(r.nextInt(), pool.length)]);
        return out;
    }
    static int vaultCashFor(BankProfile b, Random r) {
        if (r == null) r = new Random(2);
        int base = b == null ? 150 : (b.upperHive ? 1200 : (b.midHive ? 500 : 180));
        return Math.max(25, base + r.nextInt(Math.max(40, base)));
    }
}


class PlayerNewsEvent {
    int turn, day, publicDay, severity;
    String category = "misc", subject = "player activity", factionName = Faction.NONE.name(), zone = "unknown zone", detail = "", id = "";

    static PlayerNewsEvent create(int turn, int day, String category, String subject, Faction faction, String zone, String detail, int severity, long seed) {
        PlayerNewsEvent ev = new PlayerNewsEvent();
        ev.turn = Math.max(0, turn);
        ev.day = Math.max(0, day);
        ev.category = clean(category, "misc");
        ev.subject = clean(subject, "player activity");
        ev.factionName = (faction == null ? Faction.NONE : faction).name();
        ev.zone = clean(zone, "unknown zone");
        ev.detail = clean(detail, "");
        ev.severity = Math.max(1, Math.min(12, severity));
        ev.publicDay = ev.day + 1 + Math.floorMod(Objects.hash(seed, ev.turn, ev.category, ev.subject), 2);
        ev.id = "PNEWS-" + Math.abs(Objects.hash(ev.turn, ev.category, ev.subject, ev.zone, ev.detail));
        return ev;
    }
    int ageDays(int currentDay) { return Math.max(0, currentDay - day); }
    boolean shouldBePublicBy(int currentDay) { return currentDay >= publicDay; }
    Faction faction() { try { return Faction.valueOf(factionName); } catch(Exception e) { return Faction.NONE; } }
    String shortLine() { return "day " + day + " -> public day " + publicDay + " | " + category + " | " + subject + " | " + zone; }
    String saveLine() { return enc(id)+"|"+turn+"|"+day+"|"+publicDay+"|"+severity+"|"+enc(category)+"|"+enc(subject)+"|"+enc(factionName)+"|"+enc(zone)+"|"+enc(detail); }
    static PlayerNewsEvent parse(String s) {
        try {
            String[] a=s.split("\\|",10); if(a.length<10) return null;
            PlayerNewsEvent ev=new PlayerNewsEvent(); ev.id=dec(a[0]); ev.turn=Integer.parseInt(a[1]); ev.day=Integer.parseInt(a[2]); ev.publicDay=Integer.parseInt(a[3]); ev.severity=Integer.parseInt(a[4]); ev.category=dec(a[5]); ev.subject=dec(a[6]); ev.factionName=dec(a[7]); ev.zone=dec(a[8]); ev.detail=dec(a[9]); return ev;
        } catch(Exception e) { return null; }
    }
    static String clean(String s, String fallback) { return s == null || s.isBlank() ? fallback : s.replace('|','/').trim(); }
    static String enc(String s){ return Base64.getUrlEncoder().withoutPadding().encodeToString((s==null?"":s).getBytes(java.nio.charset.StandardCharsets.UTF_8)); }
    static String dec(String s){ try{return new String(Base64.getUrlDecoder().decode(s), java.nio.charset.StandardCharsets.UTF_8);}catch(Exception e){return "";} }
}


class ImperialNewsNetworkApi {
    private ImperialNewsNetworkApi() {}
    static final int MAX_ISSUE_HISTORY = 7;

    static String ensureDailyIssue(GamePanel g, int day, Random r) {
        if (g == null) return "No game state is available for the Imperial News Network.";
        FactionStrategySimulationApi.ensurePlans(g);
        int safeDay = Math.max(0, day);
        String existing = g.innDailyIssues.get(safeDay);
        if (existing != null && !existing.isBlank()) return existing;
        String issue = generateIssue(g, safeDay, r == null ? new Random(safeDay) : r);
        g.innDailyIssues.put(safeDay, issue);
        g.innLastIssueDay = safeDay;
        g.lastInnNewsIssue = issue;
        g.lastPublicNewsBulletin = issue;
        while (g.innDailyIssues.size() > MAX_ISSUE_HISTORY) {
            Integer first = g.innDailyIssues.keySet().iterator().next();
            g.innDailyIssues.remove(first);
        }
        DebugLog.audit("INN_DAILY_ISSUE", "day=" + safeDay + " issue=" + issue + " state=" + g.stateSummary());
        return issue;
    }

    static String generateIssue(GamePanel g, int day, Random r) {
        ArrayList<FactionStrategicPlan> plans = new ArrayList<>(g.factionStrategicPlans);
        Collections.shuffle(plans, r);
        ArrayList<String> articles = new ArrayList<>();
        articles.add("INN DAY " + day + " — sanctioned civic edition.");
        for (FactionStrategicPlan p : plans) {
            if (p == null || p.faction == Faction.NONE || p.faction == Faction.INN) continue;
            if (articles.size() >= 5) break;
            int chance = publicDiscoveryChance(p, day);
            if (r.nextInt(100) < chance) articles.add(articleFor(p, r));
        }
        String playerArticle = playerArticleFor(g, day, r);
        if (playerArticle != null && !playerArticle.isBlank()) articles.add(playerArticle);
        if (articles.size() == 1) articles.add("Public order remains statistically excellent, according to officials declining to define either public, order, or excellent.");
        articles.add("Classifieds: INN dispensers sell fresh papers for 1 Imperial Script. Old copies may be found wherever citizens threw yesterday's certainty.");
        return String.join(" ", articles);
    }

    static int publicDiscoveryChance(FactionStrategicPlan p, int day) {
        int base = 18 + p.ambition/5 + p.aggression/7 - p.secrecy/3;
        if ("EXECUTION".equals(p.phase)) base += 26;
        if ("COOLDOWN".equals(p.phase)) base += 12;
        String g = p.immediateGoal == null ? "" : p.immediateGoal.toLowerCase(Locale.ROOT);
        if (g.contains("attack") || g.contains("assassin") || g.contains("steal") || g.contains("sabotage")) base += 16;
        if (p.scheme != null && !p.scheme.isBlank()) base += 10;
        return Math.max(5, Math.min(85, base + Math.floorMod(Objects.hash(p.id, day), 11) - 5));
    }

    static String articleFor(FactionStrategicPlan p, Random r) {
        String goal = p.immediateGoal == null ? "" : p.immediateGoal.toLowerCase(Locale.ROOT);
        String targetFaction = p.schemeTargetFaction == null ? "a rival faction" : p.schemeTargetFaction.label;
        String desk = editorialDeskFor(p.faction);
        String frame = editorialFrameFor(p.faction);
        if (goal.contains("assassin") || (p.scheme != null && p.scheme.toLowerCase(Locale.ROOT).contains("assassin"))) {
            return desk + ": " + frame + " Sources allege " + p.leaderName + " of " + p.faction.label + " is linked to a murder arrangement against " + targetFaction + "; INN legal counsel recommends the word alleged be worshipped.";
        }
        if (goal.contains("steal") || (p.scheme != null && p.scheme.toLowerCase(Locale.ROOT).contains("information"))) {
            return desk + ": " + frame + " Missing stock and whispered documents suggest " + p.faction.label + " activity around " + p.targetRoom + ", with " + targetFaction + " named in off-record corridor talk.";
        }
        if (goal.contains("stockpile") || goal.contains("secure")) {
            return desk + ": " + frame + " Unusual purchasing by " + p.faction.label + " centers on " + p.targetItem + "; citizens are advised that shortages are impossible until officially scheduled.";
        }
        if (goal.contains("factory") || goal.contains("production")) {
            return desk + ": " + frame + " Permit movement suggests " + p.faction.label + " may be converting " + p.targetRoom + " toward production or repair work; machine noise will be described as prosperity.";
        }
        if (goal.contains("attack") || goal.contains("sabotage")) {
            return desk + ": " + frame + " INN analysts observe pressure between " + p.faction.label + " and " + targetFaction + "; officials recommend avoiding corridors that sound expensive.";
        }
        if (goal.contains("followers") || goal.contains("guards")) {
            return desk + ": " + frame + " " + p.faction.label + " appears to be increasing personnel under " + p.leaderName + "; the official explanation is readiness, which is what danger calls itself in uniform.";
        }
        if (goal.contains("trade")) {
            return desk + ": " + frame + " " + p.faction.label + " is seeking route access through " + p.targetRoom + "; prices may adjust themselves upward in respect of opportunity.";
        }
        return desk + ": " + frame + " " + p.faction.label + " remains in " + p.phase.toLowerCase(Locale.ROOT) + " around " + FactionStrategicPlan.publicGoalPhrase(p.immediateGoal) + "; senior sources deny all knowledge with practiced timing.";
    }


    static boolean isGangMutantOrHeretic(Faction f) {
        if (f == null) return false;
        return f == Faction.BANDIT || f == Faction.MUTANT || f == Faction.CULTIST || f == Faction.HERETIC || f.name().startsWith("GANGER");
    }
    static boolean isNobleFaction(Faction f) { return f == Faction.NOBLE || (f != null && f.name().startsWith("NOBLE")); }
    static boolean isMilitaryFaction(Faction f) { return f == Faction.IMPERIAL_GUARD || f == Faction.SORORITAS; }
    static boolean isCivilFaction(Faction f) { return f == Faction.ADMINISTRATUM || f == Faction.MINISTORUM || f == Faction.INN; }
    static boolean isHiverFaction(Faction f) { return f == Faction.HIVER || (f != null && f.name().startsWith("HIVER")); }
    static String editorialDeskFor(Faction f) {
        if (isGangMutantOrHeretic(f)) return "Crime desk";
        if (f == Faction.ARBITES) return "Law column";
        if (isMilitaryFaction(f)) return "Defense notice";
        if (isNobleFaction(f)) return "Upper-hive society ledger";
        if (isCivilFaction(f)) return "Civic administration page";
        if (isHiverFaction(f)) return "Hab civic column";
        if (f == Faction.MECHANICUS) return "Industrial Mechanicus ledger";
        return "Faction watch";
    }
    static String editorialFrameFor(Faction f) {
        if (isGangMutantOrHeretic(f)) return "Outside witnesses describe criminal agitation, territorial intimidation, and unsanctioned movement.";
        if (f == Faction.ARBITES) return "Official phrasing emphasizes lawful procedure and omits the sound of the boots.";
        if (isMilitaryFaction(f)) return "Military sources frame the matter as readiness, logistics, and necessary discipline.";
        if (isNobleFaction(f)) return "Noble spokesmen describe the affair as estate management, never desperation.";
        if (isCivilFaction(f)) return "Clerks call it civic continuity; citizens call it another form to survive.";
        if (isHiverFaction(f)) return "Hab witnesses report queue talk, stairwell rumors, and tired practical arithmetic.";
        if (f == Faction.MECHANICUS) return "Forge-adjacent observers translate the matter into maintenance, output, and machine propriety.";
        return "Public sources present partial facts through sanctioned fog.";
    }
    static String playerArticleFor(GamePanel g, int day, Random r) {
        if (g == null || g.playerNewsEvents == null || g.playerNewsEvents.isEmpty()) return null;
        ArrayList<PlayerNewsEvent> candidates = new ArrayList<>();
        for (PlayerNewsEvent ev : g.playerNewsEvents) {
            if (ev == null) continue;
            int age = ev.ageDays(day);
            if (!ev.shouldBePublicBy(day) || age > 2) continue;
            // Player-facing consequences should surface within a day or two once public.
            // Keep it to one article per issue rather than hiding every qualifying event behind another roll.
            candidates.add(ev);
        }
        if (candidates.isEmpty()) return null;
        PlayerNewsEvent ev = candidates.get(Math.floorMod(Objects.hash(day, candidates.size(), g.seed), candidates.size()));
        String subject = ev.subject == null ? "an unnamed local" : ev.subject;
        String zone = ev.zone == null ? "an unnamed zone" : ev.zone;
        String cat = ev.category == null ? "" : ev.category.toLowerCase(Locale.ROOT);
        String authTail = " Inspectors may review licenses, purchase permits, and facility papers where public attention lingers.";
        if (cat.contains("kill")) return "Public incident column: authorities report a violent incident involving " + subject + " in " + zone + ". Names are withheld pending retaliation, identification, or a better headline." + authTail;
        if (cat.contains("room")) return "Property notice: a previously unremarkable room in " + zone + " has acquired a private claimant, visible traffic, and the expensive smell of reserved paperwork." + authTail;
        if (cat.contains("facility") || cat.contains("production")) return "Permit desk: local observers note new machinery or production activity tied to " + subject + " in " + zone + ". Officials remind citizens that useful work remains illegal until licensed." + authTail;
        if (cat.contains("service")) return "Commerce page: a new service offer, " + subject + ", is circulating through " + zone + ". Customers are advised to ask whether the permit is real before paying in advance." + authTail;
        return "Local interest column: public reports mention " + subject + " in " + zone + "; the INN will provide more detail once it becomes profitable, legal, or impossible to hide." + authTail;
    }

    static String broadcastBulletin(GamePanel g, String deviceKind, Random r) {
        if (g == null) return "The receiver hisses. No game-state carrier wave is available.";
        FactionStrategySimulationApi.ensurePlans(g);
        int day = Math.max(0, g.currentInnDay());
        Random rr = r == null ? new Random(Objects.hash(day, deviceKind)) : r;
        String kind = deviceKind == null ? "radio" : deviceKind.toLowerCase(Locale.ROOT);
        FactionStrategicPlan p = pickBroadcastPlan(g, rr, day);
        String prefix;
        if (kind.contains("pict")) prefix = "PICT-SCREEN BULLETIN day " + day + ": ";
        else if (kind.contains("bar")) prefix = "BAR VOX NEWS day " + day + ": ";
        else prefix = "RADIO BULLETIN day " + day + ": ";
        String body;
        String playerBrief = rr.nextInt(100) < 18 ? playerBroadcastLineFor(g, day, rr) : null;
        if (playerBrief != null && !playerBrief.isBlank()) {
            body = playerBrief;
        } else if (p == null) {
            body = "approved hymns, market prices, rail-delay denials, and a reminder that public order remains excellent because the definition is classified.";
        } else {
            body = broadcastLineFor(p, rr, kind);
        }
        String issue = ensureDailyIssue(g, day, rr);
        String suffix = kind.contains("portable") ? " Portable reception is thin; buy a fresh paper for detail." : " Fresh papers carry the longer version.";
        String report = prefix + body + suffix;
        g.lastPublicNewsBulletin = report;
        g.lastBroadcastReport = report;
        if (issue != null && !issue.isBlank()) g.lastInnNewsIssue = issue;
        return report;
    }


    static String playerBroadcastLineFor(GamePanel g, int day, Random r) {
        if (g == null || g.playerNewsEvents == null || g.playerNewsEvents.isEmpty()) return null;
        ArrayList<PlayerNewsEvent> candidates = new ArrayList<>();
        for (PlayerNewsEvent ev : g.playerNewsEvents) if (ev != null && ev.shouldBePublicBy(day) && ev.ageDays(day) <= 2) candidates.add(ev);
        if (candidates.isEmpty()) return null;
        PlayerNewsEvent ev = candidates.get(Math.floorMod(r == null ? candidates.size() : r.nextInt(), candidates.size()));
        String cat = ev.category == null ? "" : ev.category.toLowerCase(Locale.ROOT);
        if (cat.contains("kill")) return "breaking local report: an altercation involving " + ev.subject + " has authorities reviewing witness statements in " + ev.zone + ".";
        if (cat.contains("facility") || cat.contains("production") || cat.contains("service")) return "local licensing reminder: new activity around " + ev.subject + " in " + ev.zone + " may prompt permit checks.";
        if (cat.contains("room")) return "property irregularity reported in " + ev.zone + "; claimants are reminded that possession is not paperwork.";
        return "local report mentions " + ev.subject + " in " + ev.zone + "; details remain subject to revision, censorship, and fear.";
    }

    static FactionStrategicPlan pickBroadcastPlan(GamePanel g, Random r, int day) {
        if (g == null || g.factionStrategicPlans == null || g.factionStrategicPlans.isEmpty()) return null;
        ArrayList<FactionStrategicPlan> plans = new ArrayList<>(g.factionStrategicPlans);
        Collections.shuffle(plans, r);
        FactionStrategicPlan fallback = null;
        for (FactionStrategicPlan p : plans) {
            if (p == null || p.faction == Faction.NONE || p.faction == Faction.INN) continue;
            if (fallback == null) fallback = p;
            int chance = Math.max(8, publicDiscoveryChance(p, day) - 8);
            if (r.nextInt(100) < chance) return p;
        }
        return fallback;
    }

    static String broadcastLineFor(FactionStrategicPlan p, Random r, String kind) {
        if (p == null) return "no actionable faction item survives censor review.";
        String goal = p.immediateGoal == null ? "" : p.immediateGoal.toLowerCase(Locale.ROOT);
        String tf = p.schemeTargetFaction == null ? "a rival faction" : p.schemeTargetFaction.label;
        String frame = editorialDeskFor(p.faction).toLowerCase(Locale.ROOT) + ": ";
        if (goal.contains("assassin") || (p.scheme != null && p.scheme.toLowerCase(Locale.ROOT).contains("assassin"))) return frame + "unconfirmed reports connect " + p.faction.label + " leadership to a plot against " + tf + "; citizens should not investigate murder unless credentialed, armored, or already doomed.";
        if (goal.contains("steal") || (p.scheme != null && p.scheme.toLowerCase(Locale.ROOT).contains("information"))) return frame + "security chatter names " + p.faction.label + " around missing stock near " + p.targetRoom + "; officials insist nothing was stolen from anything important.";
        if (goal.contains("stockpile") || goal.contains("secure")) return frame + "market monitors note abnormal demand for " + p.targetItem + " by " + p.faction.label + "; ration rumors are false until profitable.";
        if (goal.contains("factory") || goal.contains("production")) return frame + "industrial vox traffic suggests " + p.faction.label + " is preparing work around " + p.targetRoom + "; laborers nearby should enjoy the sound of opportunity.";
        if (goal.contains("attack") || goal.contains("sabotage")) return frame + "public safety recommends avoiding contested corridors between " + p.faction.label + " and " + tf + "; this is not panic, merely informed routing.";
        if (goal.contains("followers") || goal.contains("guards")) return frame + p.faction.label + " appears to be adding bodies to the roster under " + p.leaderName + "; the bureau calls it staffing, not mobilization.";
        if (goal.contains("trade")) return frame + p.faction.label + " seeks fresh route access through " + p.targetRoom + "; prices may rise out of patriotic respect for logistics.";
        return frame + p.faction.label + " remains in " + p.phase.toLowerCase(Locale.ROOT) + " around " + FactionStrategicPlan.publicGoalPhrase(p.immediateGoal) + "; denials are available in bulk.";
    }

    static void seedNewsObjects(World w, Random r) {
        if (w == null || w.rooms == null || w.rooms.size() <= 1) return;
        int vendors = 0, oldPapers = 0, broadcastDevices = 0;
        boolean civic = w.zoneType == ZoneType.IMPERIAL_NEWS_NETWORK || w.zoneType == ZoneType.NEUTRAL_CIVILIAN_FLOOR || w.zoneType == ZoneType.NEUTRAL_RAIL_DEPOT || w.zoneType == ZoneType.SUMP_MARKET || w.zoneType == ZoneType.ADMINISTRATUM_ARCHIVE || w.zoneType == ZoneType.HAB_STACK;
        int desiredVendors = w.zoneType == ZoneType.IMPERIAL_NEWS_NETWORK ? 5 : (civic ? (w.zoneType == ZoneType.NEUTRAL_CIVILIAN_FLOOR || w.zoneType == ZoneType.HAB_STACK ? 3 : 2) : 0);
        for (int i=0; i<desiredVendors; i++) {
            Rectangle rr = w.rooms.get(Math.min(w.rooms.size()-1, 1 + Math.floorMod(i, Math.max(1, w.rooms.size()-1))));
            Point p = w.randomObjectPointInRoom(rr);
            if (p == null || w.npcAt(p.x,p.y) != null || w.mapObjectAt(p.x,p.y) != null || w.isDoorAccessReservedForObject(p.x,p.y)) continue;
            char under = w.tiles[p.x][p.y];
            w.tiles[p.x][p.y] = '1';
            w.mapObjects.add(MapObjectState.newsVending(p.x,p.y,w.zoneType,under));
            vendors++;
        }
        int paperTries = w.zoneType == ZoneType.IMPERIAL_NEWS_NETWORK ? 8 : (w.zoneType == ZoneType.TRASH_WARREN ? 10 : 3);
        int today = Math.max(0, Math.floorMod((int)(w.seed ^ (w.floor*31L)), 12));
        for (int i=0; i<paperTries && oldPapers<6; i++) {
            Rectangle rr = w.rooms.get(1 + Math.floorMod((r==null?i:r.nextInt(Math.max(1,w.rooms.size()-1))), Math.max(1,w.rooms.size()-1)));
            Point p = w.randomObjectPointInRoom(rr);
            if (p == null || w.npcAt(p.x,p.y) != null || w.mapObjectAt(p.x,p.y) != null || w.isDoorAccessReservedForObject(p.x,p.y)) continue;
            if (r != null && r.nextInt(100) > (w.zoneType == ZoneType.TRASH_WARREN ? 55 : 22)) continue;
            char under = w.tiles[p.x][p.y];
            w.tiles[p.x][p.y] = 'o';
            w.mapObjects.add(MapObjectState.oldNewspaper(p.x,p.y, Math.max(0, today - 1 - (r==null?0:r.nextInt(7))), w.zoneType, under));
            oldPapers++;
        }
        int desiredBroadcast = w.zoneType == ZoneType.IMPERIAL_NEWS_NETWORK ? 4 : ((civic || w.zoneType == ZoneType.NOBLE_SERVICE_SPINE || w.zoneType == ZoneType.SECTOR_GOVERNORS_MANSION || w.zoneType == ZoneType.IMPERIAL_GUARD_BILLET || w.zoneType == ZoneType.ARBITES_PRECINCT_EDGE) ? 1 : 0);
        for (int i=0; i<desiredBroadcast; i++) {
            Rectangle rr = w.rooms.get(1 + Math.floorMod((r==null?i*3:r.nextInt(Math.max(1,w.rooms.size()-1))), Math.max(1,w.rooms.size()-1)));
            Point p = w.randomObjectPointInRoom(rr);
            if (p == null || w.npcAt(p.x,p.y) != null || w.mapObjectAt(p.x,p.y) != null || w.isDoorAccessReservedForObject(p.x,p.y)) continue;
            String kind = w.zoneType == ZoneType.IMPERIAL_NEWS_NETWORK ? (i % 2 == 0 ? "pict-screen" : "radio") : (w.zoneType == ZoneType.SUMP_MARKET || w.zoneType == ZoneType.NEUTRAL_RAIL_DEPOT || w.zoneType == ZoneType.HAB_STACK ? "bar-vox" : (w.floor >= 6 ? "pict-screen" : "radio"));
            char under = w.tiles[p.x][p.y];
            w.tiles[p.x][p.y] = 'N';
            w.mapObjects.add(MapObjectState.broadcastDevice(p.x,p.y,kind,w.zoneType,under));
            broadcastDevices++;
        }
        if (vendors + oldPapers + broadcastDevices > 0) DebugLog.audit("INN_ZONE_OBJECTS", "zone=" + w.zoneType.label + " vendors=" + vendors + " oldPapers=" + oldPapers + " broadcasts=" + broadcastDevices + " layer=" + w.layerText());
    }
}


class NpcFactionSite {
    String name = "Unregistered faction site";
    Faction faction = Faction.NONE;
    String facilityType = "facility";
    int sectorX = 1, sectorY = 1, zoneX = 2, zoneY = 2, floor = 4;
    int baseLevel = 1, workers = 2, machineLevel = 2, stock = 0, lastProductionTurn = 0;
    ArrayList<String> outputs = new ArrayList<>();
    String knowledge = "local technique";

    static NpcFactionSite create(String name, Faction faction, String facilityType, int sx, int sy, int zx, int zy, int floor, String primary, String secondary, String knowledge) {
        NpcFactionSite site = new NpcFactionSite();
        site.name = name; site.faction = faction == null ? Faction.NONE : faction; site.facilityType = facilityType;
        site.sectorX = sx; site.sectorY = sy; site.zoneX = zx; site.zoneY = zy; site.floor = floor; site.knowledge = knowledge;
        site.baseLevel = Math.max(1, ItemQuality.tierIndex(primary) + 1); site.machineLevel = Math.max(2, site.baseLevel);
        site.workers = 2 + Math.abs(Objects.hash(name, faction)) % 5;
        site.outputs.add(primary); if (secondary != null && !secondary.isBlank()) site.outputs.add(secondary);
        return site;
    }

    boolean produceHour(int turn, Random rng) {
        if (outputs.isEmpty() || workers <= 0) return false;
        int cadence = Math.max(1, 4 - Math.min(3, baseLevel));
        if (turn <= 0 || turn % (cadence * GamePanel.TURNS_PER_HOUR) != 0) return false;
        int units = Math.max(1, workers / 2 + machineLevel / 3);
        stock = Math.min(120, stock + units);
        lastProductionTurn = turn;
        return true;
    }

    ArrayList<String> exportSample(Random rng) {
        ArrayList<String> list = new ArrayList<>();
        if (outputs.isEmpty() || workers <= 0 || stock <= 0) return list;
        int n = Math.min(stock, Math.max(1, Math.min(3, 1 + stock / 20)));
        for (int i=0;i<n;i++) {
            String base = outputs.get(Math.abs(Objects.hash(name, i, stock)) % outputs.size());
            list.add(qualityStamped(base, i));
        }
        stock = Math.max(0, stock - n);
        return list;
    }

    String qualityStamped(String item, int salt) {
        if (ItemQuality.tierIndex(item) != 2 || item.toLowerCase(Locale.ROOT).startsWith("common ")) return item;
        int tier = Math.max(0, Math.min(ItemQuality.NAMES.length-1, machineLevel - 1 + (salt % 2 == 0 ? 0 : -1)));
        return ItemQuality.NAMES[tier] + " " + item;
    }

    String recipeSummaryFor(String item) {
        String base = ItemQuality.stripManufacturingIdentity(item);
        return knowledge + "; facility=" + facilityType + "; machine tier " + machineLevel + "; workers " + workers + "; output=" + base;
    }

    String locationKey() { return "sector " + sectorX + "," + sectorY + " zone " + zoneX + "," + zoneY + " floor " + floor; }
    String summaryLine() { return name + " — " + faction.label + " — " + facilityType + " — " + locationKey() + " — level " + baseLevel + " machine " + machineLevel + " workers " + workers + " stock " + stock + " outputs " + String.join(", ", outputs); }
    String saveLine() { return name+"|"+faction.name()+"|"+facilityType+"|"+sectorX+"|"+sectorY+"|"+zoneX+"|"+zoneY+"|"+floor+"|"+baseLevel+"|"+workers+"|"+machineLevel+"|"+stock+"|"+lastProductionTurn+"|"+knowledge+"|"+String.join(",", outputs); }
    static NpcFactionSite parse(String s) {
        try {
            String[] a = s.split("\\|",15); if (a.length < 15) return null;
            NpcFactionSite site = new NpcFactionSite(); site.name=a[0]; site.faction=Faction.valueOf(a[1]); site.facilityType=a[2]; site.sectorX=Integer.parseInt(a[3]); site.sectorY=Integer.parseInt(a[4]); site.zoneX=Integer.parseInt(a[5]); site.zoneY=Integer.parseInt(a[6]); site.floor=Integer.parseInt(a[7]); site.baseLevel=Integer.parseInt(a[8]); site.workers=Integer.parseInt(a[9]); site.machineLevel=Integer.parseInt(a[10]); site.stock=Integer.parseInt(a[11]); site.lastProductionTurn=Integer.parseInt(a[12]); site.knowledge=a[13]; site.outputs.clear(); for(String o:a[14].split(",")) if(!o.isBlank()) site.outputs.add(o); return site;
        } catch(Exception ex) { return null; }
    }
}


class FactionContract {
    String id, type, description, targetZoneKey, targetEntityId, targetName, requiredTurnInItem;
    Faction faction = Faction.NONE;
    int payout = 75, repReward = 1;
    int minimumQualityTier = -1, skillXpReward = 0;
    boolean spawned = false, completed = false;

    static FactionContract create(String type, Faction f, WorldAtlas atlas, World world, Random rng, int standing) {
        FactionContract c = new FactionContract();
        c.type = "FETCH".equals(type) ? "FETCH" : "BOUNTY";
        c.faction = f == null ? Faction.NONE : f;
        Random r = rng == null ? new Random() : rng;
        c.id = c.type.substring(0,1) + "-" + Math.abs(Objects.hash(c.type, c.faction.name(), System.nanoTime(), r.nextInt()));
        c.targetZoneKey = adjacentZoneKey(atlas, r);
        int bonus = Math.max(0, standing) * 6;
        c.payout = Math.max(50, Math.min(220, 50 + r.nextInt(131) + bonus));
        c.repReward = 1 + Math.max(0, standing / 5);
        if (c.type.equals("BOUNTY")) {
            c.targetName = bountyName(c.faction, r);
            c.targetEntityId = "CONTRACT-NPC-" + c.id;
            c.requiredTurnInItem = "Ident chip " + c.id;
            c.description = "Kill " + c.targetName + " in the adjacent contract zone and return " + c.requiredTurnInItem + ".";
        } else {
            c.targetName = "sealed object " + (1000 + r.nextInt(9000));
            c.targetEntityId = "CONTRACT-OBJ-" + c.id;
            c.requiredTurnInItem = "Sealed object " + c.id;
            c.description = "Retrieve " + c.targetName + " from a world container in the adjacent contract zone and return it unopened.";
        }
        return c;
    }


    static FactionContract createBankLockboxContract(BankProfile b, WorldAtlas atlas, World world, Random rng, int standing) {
        if (rng == null) rng = new Random();
        FactionContract c = new FactionContract();
        c.type = "LOCKBOX";
        c.faction = Faction.NOBLE;
        c.id = "L-" + Math.abs(Objects.hash("LOCKBOX", b == null ? "bank" : b.id, System.nanoTime(), rng.nextInt()));
        c.targetZoneKey = atlas == null ? "1,1,2,2,6,false" : (atlas.sectorX + "," + atlas.sectorY + "," + atlas.zoneX + "," + atlas.zoneY + "," + atlas.floor + "," + atlas.sewer);
        String[] wanted = b != null && b.upperHive ? new String[]{"Sealed bank lockbox","House gold ingot","Stock certificate bundle","Noble Commerce Permit"} : (b != null && b.midHive ? new String[]{"Sealed bank lockbox","Stock certificate bundle","Noble Commerce Permit"} : new String[]{"Sealed bank lockbox","Stock certificate bundle","Trade chit"});
        c.requiredTurnInItem = wanted[Math.floorMod(rng.nextInt(), wanted.length)];
        c.targetName = c.requiredTurnInItem + " from " + (b == null ? "a bank vault" : b.label);
        c.targetEntityId = "CONTRACT-BANK-" + c.id;
        int base = b == null ? 350 : (b.upperHive ? 2200 : (b.midHive ? 1100 : 450));
        c.payout = base + rng.nextInt(Math.max(50, base / 2)) + Math.max(0, standing) * 10;
        c.repReward = 1 + Math.max(0, standing / 8);
        c.description = "Acquire " + c.requiredTurnInItem + " from a bank vault and return it to the private noble courier. Vault alarms, guards, public news, and Arbites paperwork remain your problem.";
        return c;
    }

    static String adjacentZoneKey(WorldAtlas a, Random r) {
        if (a == null) return "1,1,2,2,4,false";
        int[][] dirs = {{1,0},{-1,0},{0,1},{0,-1}};
        ArrayList<int[]> legal = new ArrayList<>();
        for (int[] d : dirs) {
            int zx = a.zoneX + d[0], zy = a.zoneY + d[1];
            if (zx >= 1 && zx <= 3 && zy >= 1 && zy <= 3) legal.add(d);
        }
        int[] d = legal.isEmpty() ? new int[]{0,0} : legal.get(r.nextInt(legal.size()));
        int zx = Math.max(1, Math.min(3, a.zoneX + d[0]));
        int zy = Math.max(1, Math.min(3, a.zoneY + d[1]));
        return a.sectorX + "," + a.sectorY + "," + zx + "," + zy + "," + a.floor + "," + a.sewer;
    }

    static String bountyName(Faction f, Random r) {
        String[] tags = {"Mord Vane", "Kessel Grint", "Sorn Ash", "Vex Marrow", "Juno Crake", "Pell Knife", "Orrik Sump", "Hale Nines"};
        String prefix = (f == Faction.ARBITES || f == Faction.IMPERIAL_GUARD) ? "Wanted fugitive " : (f != null && f.name().startsWith("GANGER") ? "Rival enforcer " : "Marked target ");
        return prefix + tags[r.nextInt(tags.length)];
    }

    String shortLine(){ return displayType() + " for " + displayFactionName() + " pays " + payout + " script" + (spawned ? "; target confirmed" : "; route pending"); }
    String longLine(){ return displayFactionName() + " " + displayType() + ": " + displayDescription() + " Location: " + displayLocation() + ". Reward " + payout + " script, rep +" + repReward + "."; }
    String displayType(){ if ("LOCKBOX".equals(type)) return "lockbox contract"; if ("FETCH".equals(type)) return "fetch contract"; if ("PRODUCTION".equals(type)) return "production order"; return "bounty contract"; }
    String displayFactionName(){
        if (faction == Faction.CIVIC_WARDENS || faction == Faction.ARBITES) return "Adeptus Civic Wardens";
        return faction == null ? "local faction" : faction.label;
    }
    String displayDescription(){
        if (description == null || description.isBlank()) return "Contract details pending.";
        String publicItem = publicRequiredItem();
        return requiredTurnInItem == null || requiredTurnInItem.isBlank() ? description : description.replace(requiredTurnInItem, publicItem);
    }
    String publicRequiredItem(){
        if (requiredTurnInItem == null || requiredTurnInItem.isBlank()) return "the required item";
        String lower = requiredTurnInItem.toLowerCase(Locale.ROOT);
        if (lower.startsWith("ident chip ")) return "the target's ident chip";
        if (lower.startsWith("sealed object ")) return "the sealed object";
        return requiredTurnInItem;
    }
    boolean requiresProductionProof() { return "PRODUCTION".equals(type); }
    String minimumQualityName() {
        int tier = Math.max(0, Math.min(ItemQuality.NAMES.length - 1,
                minimumQualityTier < 0 ? ItemQuality.tierIndex("Common item") : minimumQualityTier));
        return ItemQuality.NAMES[tier];
    }
    String displayLocation(){
        String[] a = targetZoneKey == null ? new String[0] : targetZoneKey.split(",");
        if (a.length < 6) return "nearby contract zone";
        try {
            int sx = Integer.parseInt(a[0]), sy = Integer.parseInt(a[1]), zx = Integer.parseInt(a[2]), zy = Integer.parseInt(a[3]), floor = Integer.parseInt(a[4]);
            boolean sewer = Boolean.parseBoolean(a[5]);
            return (sewer ? "sewer route" : "surface route") + " near sector " + sx + "," + sy + " zone " + zx + "," + zy + " floor " + floor;
        } catch (RuntimeException ex) {
            return "nearby contract zone";
        }
    }
    String saveLine(){ return String.join("|", id,type,faction.name(),targetZoneKey,targetEntityId,targetName,requiredTurnInItem,Integer.toString(payout),Integer.toString(repReward),Boolean.toString(spawned),Boolean.toString(completed),description,Integer.toString(minimumQualityTier),Integer.toString(skillXpReward)); }
    static FactionContract parse(String s){
        String[] a=s.split("\\|",-1); if(a.length<12) return null;
        try { FactionContract c=new FactionContract(); c.id=a[0]; c.type=a[1]; c.faction=Faction.valueOf(a[2]); c.targetZoneKey=a[3]; c.targetEntityId=a[4]; c.targetName=a[5]; c.requiredTurnInItem=a[6]; c.payout=Integer.parseInt(a[7]); c.repReward=Integer.parseInt(a[8]); c.spawned=Boolean.parseBoolean(a[9]); c.completed=Boolean.parseBoolean(a[10]); c.description=a[11]; if(a.length>=13)c.minimumQualityTier=Integer.parseInt(a[12]); if(a.length>=14)c.skillXpReward=Integer.parseInt(a[13]); return c; } catch(Exception e){ return null; }
    }
}


class RecruitWorker {
    String name, role; Faction faction; int skill; int loyalty; String duty = "labor";
    RecruitWorker(String name, String role, Faction faction, int skill, int loyalty){ this.name=name; this.role=role; this.faction=faction==null?Faction.NONE:faction; this.skill=skill; this.loyalty=loyalty; }
    static RecruitWorker fromNpc(NpcEntity n, int index){
        String nm = (n==null || n.name==null || n.name.isBlank()) ? ("Recruit " + index) : n.name;
        String rl = (n==null || n.role==null || n.role.isBlank()) ? "base worker" : n.role;
        Faction f = n==null ? Faction.NONE : n.faction;
        int sk = 1 + Math.abs(Objects.hash(nm, rl)) % 4;
        int lo = 2 + Math.abs(Objects.hash(rl, nm)) % 5;
        return new RecruitWorker(nm, rl, f, sk, lo);
    }
    String saveLine(){ return name+"|"+role+"|"+(faction==null?Faction.NONE.name():faction.name())+"|"+skill+"|"+loyalty+"|"+(duty==null?"labor":duty); }
    static RecruitWorker parse(String s){
        String[] a=s.split("\\|",6);
        if(a.length<5) return null;
        try { RecruitWorker r = new RecruitWorker(a[0], a[1], Faction.valueOf(a[2]), Integer.parseInt(a[3]), Integer.parseInt(a[4])); if(a.length>=6 && !a[5].isBlank()) r.duty=a[5]; return r; } catch(Exception e){ return null; }
    }
}


class BaseObject {
    String name; char symbol; int x, y, cost, attention;
    int integrity = 5, capacity = 0, charges = 0, cover = 0;
    boolean armed = false;
    boolean businessOpen = false;
    boolean permittedBusiness = false;
    int businessHeat = 0;
    String description = "Built base object.";
    String qualityName = "Common";
    String assignedRecipe = "";
    String assignedWorker = "";
    String machineKnowledge = "";
    String machineRepairHistory = "";
    int productionQueueTarget = 1;
    int productionQueueRemaining = 0;
    int productionProgressTurns = 0;
    String productionMaterialPolicy = "WAIT";
    String productionOutputPolicy = "BASE";
    String productionNoRoomPolicy = "WAIT";
    String productionLastBlocker = "";
    boolean underConstruction = false;
    char finalSymbol = 0;
    String constructionRequiredItems = "";
    String constructionInsertedItems = "";
    int constructionLaborRequired = 0;
    int constructionLaborDone = 0;
    int constructionVisualProgress = 0;
    char constructionOriginalTile = 0;
    Faction faction = Faction.NONE;
    BaseObject(String name, char symbol, int x, int y, int cost, int attention){ this.name=name; this.symbol=symbol; this.x=x; this.y=y; this.cost=cost; this.attention=attention; }
    boolean isBusinessAsset(){ return symbol=='w' || symbol=='u' || symbol=='e' || symbol=='f' || symbol=='l' || symbol=='s' || symbol=='B' || symbol=='M'; }
    String businessName(){
        switch(symbol){
            case 'w': return "Repair and scrap-work stall";
            case 'u': return "Water counter";
            case 'e': return "Condensed-water service";
            case 'f': return "Forge and fabrication stall";
            case 'l': return "Research and doctrine desk";
            case 's': return "Market stockroom";
            case 'B': return "General goods shop counter";
            case 'M': return "Backroom medicae service";
            default: return name + " service";
        }
    }
    String businessReturnLine(GamePanel g){
        int q = ItemQuality.tierIndex((qualityName==null?"Common":qualityName) + " asset");
        int base = Math.max(1, 1 + q);
        switch(symbol){
            case 'w': return "uses Scrap bit / Mechanical detritus; returns repair access, parts conversion, and modest service income potential " + base + " script per shift.";
            case 'u': return "uses stored water; returns local thirst relief and water sales if reserves exist.";
            case 'e': return "uses air and machine integrity; returns water goods and potable sales stock.";
            case 'f': return "uses machine parts and salvage; returns construction supplies, tools, and repair-stock value.";
            case 'l': return "uses machine parts and time; returns XP, knowledge progress, tutoring, and doctrine sale potential.";
            case 's': return "uses stored goods; returns organized shop stock capacity and counter inventory.";
            case 'B': return "uses base storage and carried goods; returns shop income, legal if permitted or hotter if illegal.";
            case 'M': return "uses bandages, medkits, antiseptic, and splints; returns treatment income and reserved recovery services.";
            default: return "uses general service accounting until a specific return profile is assigned.";
        }
    }
    String saveLine(){ return name+"|"+symbol+"|"+x+"|"+y+"|"+capacity+"|"+qualityName+"|"+(faction==null?Faction.NONE.name():faction.name())+"|"+charges+"|"+integrity+"|"+encodeDelimitedField(assignedRecipe)+"|"+(assignedWorker==null?"":assignedWorker)+"|"+businessOpen+"|"+permittedBusiness+"|"+businessHeat+"|"+productionQueueTarget+"|"+productionQueueRemaining+"|"+underConstruction+"|"+(finalSymbol==0?"":String.valueOf(finalSymbol))+"|"+(constructionRequiredItems==null?"":constructionRequiredItems)+"|"+(constructionInsertedItems==null?"":constructionInsertedItems)+"|"+constructionLaborRequired+"|"+constructionLaborDone+"|"+constructionVisualProgress+"|"+(machineKnowledge==null?"":machineKnowledge)+"|"+(machineRepairHistory==null?"":machineRepairHistory)+"|"+(constructionOriginalTile==0?"":String.valueOf(constructionOriginalTile))+"|"+productionProgressTurns+"|"+productionMaterialPolicy+"|"+productionOutputPolicy+"|"+productionNoRoomPolicy+"|"+encodeDelimitedField(productionLastBlocker); }
    static String encodeDelimitedField(String value) {
        String text = value == null ? "" : value;
        if (!text.contains("|") && !text.startsWith("~b64~")) return text;
        return "~b64~" + java.util.Base64.getUrlEncoder().withoutPadding()
                .encodeToString(text.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }
    static String decodeDelimitedField(String value) {
        if (value == null || !value.startsWith("~b64~")) return value == null ? "" : value;
        try {
            return new String(java.util.Base64.getUrlDecoder().decode(value.substring(5)),
                    java.nio.charset.StandardCharsets.UTF_8);
        } catch (IllegalArgumentException ignored) {
            return "";
        }
    }
}



