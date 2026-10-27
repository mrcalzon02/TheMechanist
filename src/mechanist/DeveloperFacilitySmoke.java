package mechanist;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.lang.reflect.Method;
import java.nio.file.*;
import java.util.*;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;

/** Exercises the facility through its real local command, interaction and trade boundaries. */
final class DeveloperFacilitySmoke {
    public static void main(String[] args) throws Exception {
        GamePanel campaign=new GamePanel();campaign.timer.stop();
        campaign.world=new World(19191L,32,24);
        campaign.world.zoneType=ZoneType.NEUTRAL_CIVILIAN_FLOOR;
        for(char[] column:campaign.world.tiles) Arrays.fill(column,'.');
        campaign.active=Candidate.random(new Random(19191L));
        campaign.seed=19191L;campaign.playerX=4;campaign.playerY=5;campaign.carriedScript=73;
        campaign.inventory.add("Water flask");
        campaign.factionStanding.put(Faction.SCAVENGER,7);
        campaign.singlePlayerSectorBridge=new SinglePlayerSectorRuntimeBridge(new SectorManager());
        World originalWorld=campaign.world;Candidate originalCharacter=campaign.active;
        ArrayList<String> originalInventory=new ArrayList<>(campaign.inventory);
        HashMap<String,Integer> originalStats=new HashMap<>(campaign.active.stats);
        DeveloperFacilityAuthority.Session session=null;
        try {
            verifyAuthorization(campaign);
            String entered=campaign.singlePlayerSectorBridge.submitConsoleCommand(campaign,
                    new ConsoleCommandRequest(SinglePlayerSectorRuntimeBridge.LOCAL_PLAYER_ID,"/devroom"));
            session=(DeveloperFacilityAuthority.Session)campaign.getClientProperty(DeveloperFacilityAuthority.ACTIVE);
            require(session!=null,"Admin command must create a developer session: "+entered);
            GamePanel game=session.facility();game.setSize(1280,800);game.options.soundEnabled=false;game.options.musicEnabled=false;
            DeveloperFacilityAuthority.Layout layout=DeveloperFacilityAuthority.layout(game);
            int expectedVendors=Faction.visibleFactions().length*FactionCriticalVendorPlacementAuthority.Category.values().length*ItemQuality.NAMES.length;
            require(layout.vendors.size()==expectedVendors,"Every faction/category/quality combination must be indexed.");
            require(game.baseObjects.size()==BuildRecipe.allBuildRecipes().size(),"Every build recipe needs a usable systems exhibit.");
            for(FixtureInteractionRegistry.Definition definition:FixtureInteractionRegistry.definitions())
                require(game.world.mapObjects.stream().anyMatch(o->definition.type.equals(o.type)),"Missing registered hardware exhibit: "+definition.type);
            verifySupplies(game);
            verifyStationary(game);
            verifyPortals(game);
            verifyStanding(game);
            verifyTrade(game);
            verifyArena(game);
            verifyControls(game);
            verifySaveBoundary(game);
            command(game,"prepare");
            require(game.unlockedKnowledges.containsAll(KnowledgeDef.all().keySet()),"Systems preparation must use the knowledge registry.");
            require(BuildRecipe.allBuildRecipes().stream().allMatch(r->ConstructionBlueprintOwnershipAuthority.owns(game,r)),"Systems preparation must grant actual licensed blueprint IDs.");
            game.activeInteractionBaseObject=game.baseObjects.stream().filter(o->o.name.toLowerCase(Locale.ROOT).contains("workbench")).findFirst().orElseThrow();
            game.openActiveMachineWorkbench();
            require(game.panelMode==GamePanel.PanelMode.WORKBENCH,"Systems exhibits must route into the real workbench UI.");
            require(campaign.active==originalCharacter && campaign.active.stats.equals(originalStats),"Facility preparation must not mutate the campaign character.");
            capture(game);
            command(game,"exit");
            SwingUtilities.invokeAndWait(()->{});
            require(campaign.getClientProperty(DeveloperFacilityAuthority.ACTIVE)==null,"Exit must close the developer session.");
            require(campaign.world==originalWorld && campaign.active==originalCharacter,"Return must resume the exact campaign world and character objects.");
            require(campaign.playerX==4 && campaign.playerY==5 && campaign.carriedScript==73,"Campaign coordinates and money must remain unchanged.");
            require(campaign.inventory.equals(originalInventory) && campaign.factionStanding.get(Faction.SCAVENGER)==7,"Catalog items and reputation tests must not leak into the campaign.");
            System.out.println("DeveloperFacilitySmoke PASS rooms="+layout.rooms.size()+" vendors="+expectedVendors
                    +" buildables="+game.baseObjects.size()+" hardwareTypes="+FixtureInteractionRegistry.definitions().size()+" catalogItems="+ItemCatalog.ITEMS.size()+" stockedUnits="+game.itemInstances.size()
                    +" admin=true portals=player-only merchants=pinned purchases=real reputation=atomic return=isolated saves=blocked");
        } finally {
            if(session!=null && DeveloperFacilityAuthority.session(session.facility())!=null) {
                DeveloperFacilityAuthority.leave(session.facility());SwingUtilities.invokeAndWait(()->{});
            }
            campaign.singlePlayerSectorBridge.close();campaign.shutdownRuntime();
        }
    }

    private static void verifyAuthorization(GamePanel game) {
        final int[] mutations={0};
        AdminCommandDispatcher dispatcher=new AdminCommandDispatcher(new WorldTurnManager(),new PlayerActionRegistry(),new AdminCommandDispatcher.MutationSink(){
            public AuthoritativeWorldSnapshot mutate(GamePanel g,String reason,Runnable body) { mutations[0]++;return null; }
            public AuthoritativeWorldSnapshot submitCommand(GamePanel g,WorldCommandRequest command) { mutations[0]++;return null; }
            public String statusLine() { return "test"; }
        });
        String response=dispatcher.executeCommand(game,new InternalServerSessionAuthority.CommandContext("remote","remote",false,"test","test"),new ConsoleCommandRequest("remote","/devroom"));
        require(response.contains("Access denied") && mutations[0]==0,"Non-admin users must not create or mutate the facility.");
        require(new DeveloperFacilityCommand("owner","enter",List.of()).requiresAdminAuthority(),"Facility requests must retain admin authority.");
    }

    private static void verifySupplies(GamePanel game) throws Exception {
        HashMap<String,Integer> counts=new HashMap<>();
        for(ItemInstance item:game.itemInstances.values()) counts.merge(item.displayName,1,Integer::sum);
        for(String name:ItemCatalog.ITEMS.keySet()) for(int tier=0;tier<ItemQuality.NAMES.length;tier++) {
            String item=tier==2?name:ItemQuality.NAMES[tier]+" "+name;
            require(counts.getOrDefault(item,0)>=DeveloperFacilityAuthority.SUPPLY_COUNT,"Missing catalog supply: "+item);
        }
        int before=game.itemInstances.size();command(game,"restock");
        require(game.itemInstances.size()==before,"Restocking full containers must be idempotent.");
        MapObjectState chest=game.world.mapObjects.stream().filter(o->"developer-catalog-container".equals(o.type)).findFirst().orElseThrow();
        relocate(game,chest.x-1,chest.y);game.lookX=chest.x;game.lookY=chest.y;game.confirmInteractionBody();
        require(game.panelMode==GamePanel.PanelMode.CONTAINER && game.activeInteractionContainerId.equals(game.persistentContainerIdForObject(chest)),"Supply chests must use the actual inventory-container UI: mode="+game.panelMode+" kind="+game.activeInteractionKind+" target="+game.lookX+","+game.lookY+" chest="+chest.x+","+chest.y+" report="+game.eventLog);
        ContainerRecord container=game.itemContainers.get(game.activeInteractionContainerId);
        int units=container.itemInstanceIds.size(),carried=game.inventory.size();
        Method take=GamePanel.class.getDeclaredMethod("takeSelectedContainerItem");take.setAccessible(true);take.invoke(game);
        require(container.itemInstanceIds.size()==units-1 && game.inventory.size()==carried+1,"Catalog supplies must transfer through the real inventory authority.");
        command(game,"restock");require(container.itemInstanceIds.size()==units,"Restock must replenish units taken from a catalog container.");
    }

    private static void verifyStationary(GamePanel game) {
        for(NpcEntity npc:game.world.npcs) if(npc.developerStationary) {
            int x=npc.x,y=npc.y;Faction faction=npc.faction;
            npc.hunger=20;npc.thirst=20;npc.state="Hostile";
            for(int turn=1;turn<=30;turn++) { npc.tick(game.world,game.playerX,game.playerY,turn);npc.moveTo(x+1,y);npc.stepToward(game.world,x+3,y,game.playerX,game.playerY); }
            require(npc.x==x && npc.y==y && npc.faction==faction && npc.state.equals("Trade"),"Opposing merchants must remain pinned and non-hostile.");
        }
        NpcTurnBudgetScheduler.Selection selection=game.npcTurnBudgetScheduler.select(game.world.npcs,70,10,31,false,true);
        require(selection.selected.stream().noneMatch(n->n.developerStationary),"Pinned merchants must not consume the active AI budget.");
        NpcEntity ordinary=NpcEntity.create(Faction.SCAVENGER,ZoneType.NEUTRAL_CIVILIAN_FLOOR,3,3,new Random(4));
        ordinary.moveTo(4,3);require(ordinary.x==4,"Normal NPC movement must remain available.");
    }

    private static void verifyPortals(GamePanel game) {
        MapObjectState portal=game.world.mapObjects.stream().filter(o->"developer-portal".equals(o.type) && "trade".equals(MapObjectState.stockValue(o.stockState,"target"))).findFirst().orElseThrow();
        NpcEntity npc=game.world.npcs.stream().filter(n->!n.developerStationary).findFirst().orElseThrow();
        require(!game.world.walkable(portal.x,portal.y) && !npc.canNpcStep(game.world,portal.x,portal.y,-1,-1),"NPCs must not traverse teleport doors.");
        int x=game.playerX,y=game.playerY;DeveloperFacilityAuthority.tryInteract(game,portal.x,portal.y);
        require(game.playerX==x && game.playerY==y,"Distant interaction must not activate a teleport door.");
        relocate(game,portal.x-1,portal.y);game.lookX=portal.x;game.lookY=portal.y;game.confirmInteractionBody();
        require(DeveloperFacilityAuthority.layout(game).rooms.get("trade").contains(game.playerX,game.playerY),"Player interaction must activate the correct chamber door.");
    }

    private static void verifyStanding(GamePanel game) {
        Faction faction=Faction.SCAVENGER;game.factionStanding.put(faction,0);game.carriedScript=100;
        require(DeveloperFacilityAuthority.buyStanding(game,faction,5),"Funded standing purchase must succeed.");
        require(game.carriedScript==0 && game.factionStanding.get(faction)==5,"Standing transaction must charge exactly 20 Script per point.");
        require(!DeveloperFacilityAuthority.buyStanding(game,faction,5) && game.factionStanding.get(faction)==5,"Insufficient funds must not grant reputation.");
        game.carriedScript=1000;game.factionStanding.put(faction,98);
        require(!DeveloperFacilityAuthority.buyStanding(game,faction,5) && game.carriedScript==1000,"Standing cap must refuse without charging.");
        require(!DeveloperFacilityAuthority.reject(game,"spawn",List.of("CULTIST","100000","0")).isBlank(),"Spawn controls must reject oversized batches.");
    }

    private static void verifyTrade(GamePanel game) throws Exception {
        for(var vendor:DeveloperFacilityAuthority.layout(game).vendors.entrySet()) {
            DeveloperFacilityAuthority.Vendor specification=vendor.getValue();
            if(specification.faction()!=Faction.SCAVENGER) continue;
            NpcEntity actor=game.world.npcById(vendor.getKey());
            TraderSession stock=TraderSession.forNpc(actor,game.world.zoneType,new Random(19));
            DeveloperFacilityAuthority.configureTrade(game,actor,stock);
            require(stock.marketCategory.equals(specification.category().id),"Every registered merchant archetype must retain its stock category.");
            require(stock.offers.stream().filter(o->ItemCatalog.get(o.name)!=null).allMatch(o->ItemQuality.tierIndex(o.name)==specification.quality()),"All eight merchant quality grades must be deterministic.");
        }
        NpcEntity npc=game.world.npcs.stream().filter(n->{var v=DeveloperFacilityAuthority.layout(game).vendors.get(n.id);return v!=null && v.faction()==Faction.SCAVENGER && v.category()==FactionCriticalVendorPlacementAuthority.Category.PROVISIONS && v.quality()==4;}).findFirst().orElseThrow();
        Method build=GamePanel.class.getDeclaredMethod("buildTraderSession",NpcEntity.class,MapObjectState.class);build.setAccessible(true);
        TraderSession trader=(TraderSession)build.invoke(game,npc,null);
        require(trader.marketFaction==Faction.SCAVENGER && trader.marketCategory.equals("provisions"),"Vendor index must retain real market faction/category.");
        require(trader.offers.stream().anyMatch(o->ItemCatalog.get(o.name)!=null && ItemQuality.tierIndex(o.name)==4),"Fine vendor stock must expose the selected quality.");
        game.activeInteractionNpc=npc;game.activeTraderSession=trader;game.carriedScript=100000;game.factionStanding.put(Faction.SCAVENGER,25);
        Method access=GamePanel.class.getDeclaredMethod("marketAccessDecision",TradeOffer.class);access.setAccessible(true);
        int selected=-1;
        for(int i=0;i<trader.offers.size();i++) {
            TradeOffer offer=trader.offers.get(i);
            if(ItemCatalog.get(offer.name)!=null && ((FactionMarketAccessAuthority.Decision)access.invoke(game,offer)).allowed()) { selected=i;break; }
        }
        require(selected>=0,"A trusted developer provisions trader must offer purchasable stock.");
        TradeOffer offer=trader.offers.get(selected);game.selectedTradeOfferIndex=selected;
        int money=game.carriedScript,count=game.inventory.size(),price=trader.buyPrice(offer);
        Method buy=GamePanel.class.getDeclaredMethod("buySelectedTradeOffer");buy.setAccessible(true);buy.invoke(game);
        require(game.carriedScript==money-price && game.inventory.size()==count+1,"Developer trading must execute a real priced inventory purchase.");
        game.factionStanding.put(Faction.SCAVENGER,-50);
        require(!((FactionMarketAccessAuthority.Decision)access.invoke(game,offer)).allowed(),"Hostile standing must still enforce the normal faction gate.");
    }

    private static void verifyArena(GamePanel game) {
        var layout=DeveloperFacilityAuthority.layout(game);int vendors=layout.vendors.size();
        command(game,"clear");require(game.world.npcs.size()==vendors,"Arena reset must preserve every trader.");
        command(game,"spawn MUTANT 4 3");
        List<NpcEntity> actors=game.world.npcs.stream().filter(n->!n.developerStationary).toList();
        require(actors.size()==4 && actors.stream().allMatch(n->n.state.equals("Hostile") && n.equipmentTier==3 && layout.rooms.get("combat").contains(n.x,n.y)),"Combatants must spawn hostile, correctly graded and inside the arena.");
        command(game,"animal sump-rat-swarm 2 1");
        require(game.world.npcs.stream().filter(n->n.isAnimalActor() && n.state.equals("Hostile") && n.animalProfileId.equals("sump-rat-swarm")).count()==2,"Creature controls must use registered animal profiles and hostile AI.");
    }

    private static void verifyControls(GamePanel game) {
        for(MapObjectState control:game.world.mapObjects) if("developer-control".equals(control.type)) {
            game.openDeveloperFacilityControl(control);game.buttons.clear();
            BufferedImage image=new BufferedImage(960,600,BufferedImage.TYPE_INT_RGB);Graphics2D g=image.createGraphics();
            Rectangle body=new Rectangle(30,100,900,370);
            require(DeveloperFacilityControlSurface.paint(game,g,body),"Every physical console needs an in-game control surface.");g.dispose();
            require(!game.buttons.isEmpty(),"Developer controls must expose real command actions.");
            require(game.buttons.stream().allMatch(button->body.contains(button.r)),"Developer control buttons must remain inside the game panel.");
        }
    }

    private static void verifySaveBoundary(GamePanel game) {
        boolean blocked=false;
        try { Persistence.writeCore(game,new Properties()); } catch(IllegalStateException expected) { blocked=true; }
        require(blocked,"Developer sessions must not serialize over campaign saves.");
    }

    private static void capture(GamePanel game) throws Exception {
        String directory=System.getProperty("mechanist.devFacility.captureDir","");if(directory.isBlank()) return;
        Path output=Path.of(directory);Files.createDirectories(output);
        for(String chamber:List.of("hub","combat","systems","trade")) {
            command(game,chamber);
            for(boolean doom:new boolean[]{false,true}) {
                game.options.doomModeEnabled=doom;game.firstPersonRenderViewport=new FirstPersonRenderViewport();
                snapshot(game,output,chamber+(doom?"-doom":"-2d"));
            }
            MapObjectState console=game.world.mapObjects.stream().filter(o->("DEV-CONTROL-"+chamber).equals(o.id)).findFirst().orElseThrow();
            game.openDeveloperFacilityControl(console);snapshot(game,output,chamber+"-controls");
        }
        command(game,"standing SCAVENGER 25");command(game,"vendor SCAVENGER PROVISIONS 4");
        NpcEntity merchant=game.world.npcAt(game.playerX-1,game.playerY);
        game.lookX=merchant.x;game.lookY=merchant.y;game.confirmInteractionBody();snapshot(game,output,"trade-trusted-fine");
        command(game,"standing SCAVENGER -50");snapshot(game,output,"trade-hostile-gates");
        command(game,"supplies");
        MapObjectState chest=game.world.mapObjects.stream().filter(o->"developer-catalog-container".equals(o.type)).findFirst().orElseThrow();
        relocate(game,chest.x-1,chest.y);game.lookX=chest.x;game.lookY=chest.y;game.confirmInteractionBody();snapshot(game,output,"catalog-supply");
        StringBuilder plan=new StringBuilder("kind\tlabel\tx\ty\twidth\theight\ttarget\n");
        for(var entry:DeveloperFacilityAuthority.layout(game).rooms.entrySet()) {
            Rectangle room=entry.getValue();plan.append("room\t").append(entry.getKey()).append('\t').append(room.x).append('\t').append(room.y).append('\t').append(room.width).append('\t').append(room.height).append("\t\n");
        }
        for(MapObjectState object:game.world.mapObjects) if("developer-portal".equals(object.type))
            plan.append("portal\t").append(object.label).append('\t').append(object.x).append('\t').append(object.y).append("\t1\t1\t").append(MapObjectState.stockValue(object.stockState,"target")).append('\n');
        Files.writeString(output.resolve("facility-layout.tsv"),plan.toString());
        long stock=DeveloperFacilityAuthority.layout(game).supplies.keySet().stream().map(game.itemContainers::get).mapToLong(c->c.itemInstanceIds.size()).sum();
        Files.writeString(output.resolve("facility-counts.json"),"{\"rooms\":4,\"vendors\":"+DeveloperFacilityAuthority.layout(game).vendors.size()+",\"buildables\":"+game.baseObjects.size()+",\"hardwareTypes\":"+FixtureInteractionRegistry.definitions().size()+",\"catalogItems\":"+ItemCatalog.ITEMS.size()+",\"catalogUnits\":"+stock+"}\n");
    }
    private static void snapshot(GamePanel game,Path output,String name) throws Exception {
        BufferedImage image=new BufferedImage(1280,800,BufferedImage.TYPE_INT_RGB);Graphics2D g=image.createGraphics();
        game.paint(g);g.dispose();ImageIO.write(image,"png",output.resolve(name+".png").toFile());
    }
    private static void relocate(GamePanel game,int x,int y) { WorldCommandRuntimeContexts.fromGamePanel(game).teleportPlayer(x,y,"developer smoke"); }
    private static void command(GamePanel game,String command) { game.singlePlayerSectorBridge.submitConsoleCommand(game,new ConsoleCommandRequest(SinglePlayerSectorRuntimeBridge.LOCAL_PLAYER_ID,"/devroom "+command)); }
    private static void require(boolean condition,String message) { if(!condition) throw new AssertionError(message); }
}
