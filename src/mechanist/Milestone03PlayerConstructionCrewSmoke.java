package mechanist;

import java.awt.Rectangle;
import java.util.Arrays;
import java.util.Properties;
import java.util.Random;

/** Player claims a room, hires a real NPC, supplies a site and watches physical crew labor. */
final class Milestone03PlayerConstructionCrewSmoke {
    public static void main(String[] args) {
        GamePanel g = new GamePanel();
        g.timer.stop();
        try {
            g.world = new World(17731L, 24, 24);
            g.seed = 17731L;
            g.worldSetup.zoneSize = 0;
            g.worldSetup.npcDensity = 0;
            g.worldSetup.simulationAge = 0;
            for (char[] column : g.world.tiles) Arrays.fill(column, '.');
            g.world.rooms.add(new Rectangle(6, 6, 14, 14));
            g.world.roomProfiles.add(RoomProfile.generic());
            g.world.roomFactions.add(Faction.NONE);
            g.world.roomSpecials.add(Boolean.FALSE);
            for(int x=6;x<20;x++) for(int y=6;y<20;y++) g.world.roomIds[x][y]=0;
            g.active = Candidate.random(new Random(17731));
            g.playerX = 10; g.playerY = 10; g.carriedScript = 15;
            require(RoomOwnershipAuthority.claimCurrentRoom(g).success(), "claim an actual unowned room");

            NpcEntity n = NpcEntity.create(Faction.HIVER, g.world.zoneType, 12, 10, new Random(17732));
            n.id="CREW-ACTOR-17732"; n.name="Nyla Worker"; n.role="Resident"; n.ageYears=30;
            n.idleBias=1; n.routineOffset=10; n.hp=12; n.state="Idle";
            g.world.npcs.add(n);
            g.playerX=11; // hiring requires immediate adjacency to the actual NPC
            PlayerRecruitmentAuthority.Result signed=PlayerRecruitmentAuthority.toggle(g,n);
            require(signed.success(), "hire a living nearby adult: " + signed.message());
            g.playerX=10; // test must still reject an order issued too far from the site

            BaseObject site=ProgressiveConstructionAuthority.createSite(BuildRecipe.cot(),16,10,3);
            g.baseObjects.add(site);
            ProgressiveConstructionAuthority.syncSiteTile(g,site);
            require(!PlayerConstructionCrewAuthority.toggle(g,site).success() && site.assignedWorker.isBlank(),
                    "remote construction orders must be denied without mutation");

            g.supplies=1;
            g.addInventoryItem("Reclaimed textile bundle",null);
            int supplies=g.supplies;
            int textile=g.countProductionInput("Reclaimed textile bundle");
            int inserted=ProgressiveConstructionAuthority.contribute(g,site,0,true);
            require(inserted>=2 && g.supplies<supplies
                    && g.countProductionInput("Reclaimed textile bundle")<textile
                    && site.constructionLaborDone==0
                    && "none".equals(ProgressiveConstructionAuthority.missingMaterials(site)),
                    "staging requires physical supplies and textile, no free materials or labor");

            g.playerX=16; g.playerY=9;
            PlayerConstructionCrewAuthority.Result order=PlayerConstructionCrewAuthority.toggle(g,site);
            require(order.success() && n.name.equals(site.assignedWorker)
                    && PlayerConstructionCrewAuthority.hasDuty(g,n),"hire must become an actual assigned NPC duty");

            int before=Math.abs(n.x-site.x)+Math.abs(n.y-site.y);
            g.waitOneTurn();
            int after=Math.abs(n.x-site.x)+Math.abs(n.y-site.y);
            require(after<before && site.constructionLaborDone==0,
                    "employee must walk toward the site, not complete remote work");

            Properties save=new Properties();
            Persistence.writeCore(g,save);
            GamePanel loaded=new GamePanel();
            loaded.timer.stop();
            try {
                Persistence.readCore(loaded,save);
                BaseObject pending=loaded.baseObjects.stream().filter(b->b.x==16&&b.y==10&&b.underConstruction)
                        .findFirst().orElseThrow();
                require("Nyla Worker".equals(pending.assignedWorker)
                        && pending.constructionLaborDone==0
                        && "none".equals(ProgressiveConstructionAuthority.missingMaterials(pending)),
                        "work order and real staged inputs must persist through full campaign save/load");
                require(loaded.factionRecruits.stream().anyMatch(w->
                            "CREW-ACTOR-17732".equals(w.sourceNpcId)),
                        "persisted crew assignment must point back to a hired NPC");
            } finally { loaded.shutdownRuntime(); }

            for(int t=0;t<24&&site.underConstruction;t++)g.waitOneTurn();
            require(!site.underConstruction && site.symbol=='c' && site.constructionVisualProgress==100,
                    "on-site NPC labor must complete a real structure");
            require(site.assignedWorker.isBlank(), "completed structure must release construction crew assignment");
            require(g.world.tiles[site.x][site.y]=='c',"completion must synchronize actual world tile");
            require(g.factionRecruits.size()==1 && "CREW-ACTOR-17732".equals(g.factionRecruits.get(0).sourceNpcId),
                    "completion cannot destroy actor or employment record");

            verifyRosterRecovery(g, n);
            System.out.println("Milestone03PlayerConstructionCrewSmoke PASS claim=true hire=true stage=true walking=true labor=true save=true recovery=true");
        } finally { g.shutdownRuntime(); }
    }


    private static void verifyRosterRecovery(GamePanel g, NpcEntity worker) {
        g.playerX = 10;
        g.playerY = 10;
        int initialTurn = g.turn;
        int initialScript = g.carriedScript;
        require(PlayerRecruitmentAuthority.unavailableWorker(g) == null
                        && !PlayerRecruitmentAuthority.releaseUnavailableFromBase(g).success()
                        && g.turn == initialTurn && g.carriedScript == initialScript,
                "healthy employee must not be dismissed remotely from the roster");

        BaseObject pending = ProgressiveConstructionAuthority.createSite(
                BuildRecipe.cot(), 17, 10, 3);
        pending.assignedWorker = worker.name;
        pending.constructionLaborDone = 1;
        pending.constructionInsertedItems = "Construction supplies=1";
        g.baseObjects.add(pending);
        BaseObject machine = new BaseObject("Recovery Test Machine", 'w', 18, 10, 0, 0);
        machine.assignedWorker = worker.name;
        g.baseObjects.add(machine);
        require(ManualStaffingAssignmentAuthority.liveActorProblem(
                        g, g.factionRecruits.get(0)) == null,
                "a living recruited world actor must be eligible for actual staffed work");
        worker.hp = 0;
        require(ManualStaffingAssignmentAuthority.activeAssignmentProblem(g, machine)
                        .contains("deceased"),
                "a deceased recruit cannot keep a machine marked as ready for production");
        BaseObject spareMachine = new BaseObject("Unstaffed Backup Machine", 'w', 19, 10, 0, 0);
        g.baseObjects.add(spareMachine);
        String deniedDeadAssignment = ManualStaffingAssignmentAuthority.assign(
                g, spareMachine, g.factionRecruits.get(0));
        require(deniedDeadAssignment.contains("deceased")
                        && spareMachine.assignedWorker.isBlank()
                        && worker.name.equals(machine.assignedWorker),
                "dead worker assignment must refuse without removing an existing queued station duty");
        int labor = pending.constructionLaborDone;
        String inserted = pending.constructionInsertedItems;
        require(PlayerRecruitmentAuthority.unavailableWorker(g) != null
                        && PlayerRecruitmentAuthority.recoverySummary(g).contains("deceased"),
                "deceased real NPC must be surfaced as a reviewable roster record");

        // A UI button, not a developer/admin-only command, must expose recovery.
        g.setSize(1480, 920);
        g.screen = GamePanel.Screen.PANEL;
        g.panelMode = GamePanel.PanelMode.CHARACTER;
        g.characterTab = CharacterEquipmentAndMedicalAuthority.CharacterTab.OVERVIEW.ordinal();
        java.awt.image.BufferedImage canvas = new java.awt.image.BufferedImage(
                1480, 920, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        var graphics = canvas.createGraphics();
        try { g.paintComponent(graphics); } finally { graphics.dispose(); }
        require(g.buttons.stream().anyMatch(b -> "Review Lost Crew".equals(b.label)),
                "Character > Overview must expose an actionable lost-crew review button");

        g.playerX = 0; g.playerY = 0;
        require(!PlayerRecruitmentAuthority.releaseUnavailableFromBase(g).success()
                        && g.factionRecruits.size() == 1
                        && worker.name.equals(pending.assignedWorker),
                "roster release outside the claimed room must refuse without unstaffing");
        g.playerX = 10; g.playerY = 10;
        PlayerRecruitmentAuthority.Result preview =
                PlayerRecruitmentAuthority.releaseUnavailableFromBase(g);
        require(!preview.success() && g.factionRecruits.size() == 1
                        && worker.name.equals(machine.assignedWorker)
                        && g.pendingUnavailableWorkerTurn == g.turn,
                "first click must only stage confirmation and preserve worker duty");
        // Advancing the turn invalidates a pending one-click confirmation.
        g.turn++;
        require(!PlayerRecruitmentAuthority.releaseUnavailableFromBase(g).success()
                        && g.factionRecruits.size() == 1,
                "stale confirmation from a previous turn must not commit");

        Properties save = new Properties();
        Persistence.writeCore(g, save);
        GamePanel loaded = new GamePanel();
        loaded.timer.stop();
        try {
            loaded.pendingUnavailableWorkerKey = g.pendingUnavailableWorkerKey;
            loaded.pendingUnavailableWorkerTurn = g.pendingUnavailableWorkerTurn;
            Persistence.readCore(loaded, save);
            require(loaded.pendingUnavailableWorkerKey.isEmpty()
                            && loaded.pendingUnavailableWorkerTurn < 0,
                    "save/load must discard the transient release confirmation");
            require(loaded.factionRecruits.size() == 1,
                    "save/load must preserve one recruited worker until explicit release");
            BaseObject loadedSite = loaded.baseObjects.stream()
                    .filter(b -> b.x == 17 && b.y == 10 && b.underConstruction)
                    .findFirst().orElseThrow();
            BaseObject loadedMachine = loaded.baseObjects.stream()
                    .filter(b -> "Recovery Test Machine".equals(b.name))
                    .findFirst().orElseThrow();
            // The smoke's source world is a hand-built 24x24 room fixture;
            // Persistence.readCore recreates the normal deterministic atlas.
            // Find the actual claimed room in the restored atlas rather than
            // assuming that (10,10) still belongs to room zero.
            moveIntoLoadedClaimedRoom(loaded);
            int restoredTurn = loaded.turn;
            int restoredScript = loaded.carriedScript;
            PlayerRecruitmentAuthority.Result firstAfterLoad =
                    PlayerRecruitmentAuthority.releaseUnavailableFromBase(loaded);
            require(!firstAfterLoad.success()
                            && loaded.factionRecruits.size() == 1
                            && loaded.turn == restoredTurn
                            && !loaded.pendingUnavailableWorkerKey.isBlank(),
                    "first roster click after reload must stage confirmation, not dismiss: "
                            + firstAfterLoad.message());
            PlayerRecruitmentAuthority.Result released =
                    PlayerRecruitmentAuthority.releaseUnavailableFromBase(loaded);
            require(released.success() && !released.hired()
                            && loaded.factionRecruits.isEmpty()
                            && loadedSite.assignedWorker.isBlank()
                            && loadedMachine.assignedWorker.isBlank()
                            && loadedSite.underConstruction
                            && loadedSite.constructionLaborDone == labor
                            && inserted.equals(loadedSite.constructionInsertedItems)
                            && loaded.carriedScript == restoredScript
                            && loaded.turn == restoredTurn,
                    "confirmed deceased-worker release frees capacity and assignments without losing work or script: "
                            + "result=" + released.message()
                            + " roster=" + loaded.factionRecruits.size()
                            + " siteAssignment=" + loadedSite.assignedWorker
                            + " machineAssignment=" + loadedMachine.assignedWorker
                            + " unfinished=" + loadedSite.underConstruction
                            + " labor=" + loadedSite.constructionLaborDone + "/" + labor
                            + " staged=" + loadedSite.constructionInsertedItems + "/" + inserted
                            + " script=" + loaded.carriedScript + "/" + restoredScript
                            + " turn=" + loaded.turn + "/" + restoredTurn);
            require(!PlayerRecruitmentAuthority.releaseUnavailableFromBase(loaded).success(),
                    "released contract cannot be removed or refunded twice");
        } finally { loaded.shutdownRuntime(); }

        PlayerRecruitmentAuthority.Result removed =
                PlayerRecruitmentAuthority.releaseUnavailableFromBase(g);
        require(removed.success() && g.factionRecruits.isEmpty()
                        && pending.assignedWorker.isBlank()
                        && machine.assignedWorker.isBlank()
                        && pending.constructionLaborDone == labor
                        && inserted.equals(pending.constructionInsertedItems)
                        && g.carriedScript == initialScript,
                "deceased employee recovery must preserve initial player funds and queued work");
        require(g.world.npcs.contains(worker) && worker.hp <= 0,
                "contract release must not remove or resurrect the deceased world NPC");

        // A replacement can be hired normally after capacity is freed.
        NpcEntity replacement = NpcEntity.create(Faction.HIVER,
                g.world.zoneType, 12, 10, new Random(17734));
        replacement.id = "CREW-REPLACEMENT-17734";
        replacement.name = "Replacement Worker";
        replacement.role = "Resident";
        replacement.ageYears = 32;
        replacement.hp = 12;
        replacement.state = "Idle";
        g.world.npcs.add(replacement);
        g.playerX = 11; g.playerY = 10;
        g.carriedScript = 30;
        require(PlayerRecruitmentAuthority.toggle(g, replacement).success()
                        && g.factionRecruits.size() == 1,
                "ordinary live replacement hiring must fill the recovered roster slot");
        NpcEntity forged = NpcEntity.create(Faction.HIVER,
                g.world.zoneType, 10, 10, new Random(17735));
        forged.id = replacement.id;
        forged.name = replacement.name;
        require(!PlayerRecruitmentAuthority.toggle(g, forged).success()
                        && g.factionRecruits.size() == 1,
                "a spoofed detached NPC with a matching ID must not remotely dismiss a healthy hire");

        replacement.state = "Left Faction";
        require(ManualStaffingAssignmentAuthority.liveActorProblem(
                        g, g.factionRecruits.get(0)).contains("departed"),
                "a departed NPC cannot satisfy an active staffed-production requirement");
        g.playerX = 10; g.playerY = 10;
        require(!PlayerRecruitmentAuthority.releaseUnavailableFromBase(g).success(),
                "departed worker requires explicit confirmation");
        require(PlayerRecruitmentAuthority.releaseUnavailableFromBase(g).success()
                        && g.factionRecruits.isEmpty(),
                "confirmed departed worker must free the roster slot");

        replacement.state = "Idle";
        g.playerX = 11; g.playerY = 10;
        require(PlayerRecruitmentAuthority.toggle(g, replacement).success(),
                "departed worker may sign a new ordinary contract if eligible");
        g.playerX = 10; g.playerY = 10;
        g.world.npcs.remove(replacement);
        require(ManualStaffingAssignmentAuthority.liveActorProblem(
                        g, g.factionRecruits.get(0)).contains("absent"),
                "an unloaded NPC cannot operate a staffed machine from a roster string");
        PlayerRecruitmentAuthority.RecoveryTarget missing =
                PlayerRecruitmentAuthority.unavailableWorker(g);
        require(missing != null && missing.cause().contains("may be alive elsewhere"),
                "missing cross-zone actor must be labeled uncertain, not declared deceased");
        require(!PlayerRecruitmentAuthority.releaseUnavailableFromBase(g).success()
                        && g.factionRecruits.size() == 1,
                "unverified missing actor must require explicit first click");
        require(PlayerRecruitmentAuthority.releaseUnavailableFromBase(g).success()
                        && g.factionRecruits.isEmpty(),
                "confirmed administrative release must recover missing roster capacity");
        require(!g.world.npcs.contains(replacement),
                "administrative roster cleanup must not mutate or teleport world actors");
    }


    /** The atlas loader regenerates geometry, so an in-base test must use loaded coordinates. */
    private static void moveIntoLoadedClaimedRoom(GamePanel loaded) {
        require(loaded.baseClaimed && loaded.world != null
                        && loaded.claimedRoomId >= 0
                        && loaded.claimedRoomId < loaded.world.rooms.size()
                        && loaded.claimedRoomId < loaded.world.roomFactions.size()
                        && loaded.world.roomFaction(loaded.claimedRoomId) == Faction.HIVER
                        && RoomOwnershipAuthority.hasAuthoritativePlayerClaim(
                                loaded, loaded.claimedRoomId),
                "the authoritative room claim must survive a full save/atlas regeneration");
        Rectangle room = loaded.world.rooms.get(loaded.claimedRoomId);
        for (int x = Math.max(0, room.x); x < Math.min(loaded.world.w, room.x + room.width); x++) {
            for (int y = Math.max(0, room.y); y < Math.min(loaded.world.h, room.y + room.height); y++) {
                if (loaded.world.roomIdAt(x, y) == loaded.claimedRoomId
                        && loaded.world.walkable(x, y)) {
                    loaded.playerX = x;
                    loaded.playerY = y;
                    return;
                }
            }
        }
        throw new AssertionError("Saved player claim has no walkable cell in the restored world");
    }

    private static void require(boolean okay,String reason) {
        if(!okay)throw new AssertionError(reason);
    }
}
