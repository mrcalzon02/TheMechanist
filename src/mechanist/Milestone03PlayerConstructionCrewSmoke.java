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
            System.out.println("Milestone03PlayerConstructionCrewSmoke PASS claim=true hire=true stage=true walking=true labor=true save=true");
        } finally { g.shutdownRuntime(); }
    }

    private static void require(boolean okay,String reason) {
        if(!okay)throw new AssertionError(reason);
    }
}
