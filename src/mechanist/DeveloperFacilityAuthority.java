package mechanist;

import java.awt.*;
import java.util.*;
import java.util.List;
import javax.swing.*;

/** Isolated game-owned facility using the normal inventory, production and trade paths. */
final class DeveloperFacilityAuthority {
    static final String SESSION = "mechanist.developerFacilitySession";
    static final String ACTIVE = "mechanist.activeDeveloperFacility";
    static final int SUPPLY_COUNT = 5;
    static final int SCRIPT_PER_STANDING = 20;
    private static final Map<World, Layout> FACILITIES = Collections.synchronizedMap(new WeakHashMap<>());

    record Vendor(Faction faction, FactionCriticalVendorPlacementAuthority.Category category, int quality, int x, int y) { }
    static final class Layout {
        final LinkedHashMap<String, Rectangle> rooms = new LinkedHashMap<>();
        final LinkedHashMap<String, Vendor> vendors = new LinkedHashMap<>();
        final LinkedHashMap<String, List<String>> supplies = new LinkedHashMap<>();
        final Set<String> arenaActors = new HashSet<>();
        int factionIndex, categoryIndex, quality = 2, spawnTier, spawnCount = 1;
        int combatPresetIndex;
        int cyberneticPercent=25;
        int coverCount;long coverSeed=81000;
        final java.util.List<BaseObject> coverObjects=new ArrayList<>();
        long spawnSerial;
    }
    record Session(GamePanel campaign, GamePanel facility, World facilityWorld, boolean campaignTimerRunning) { }
    private static final class FacilityWorld extends World {
        FacilityWorld(long seed,int width,int height) { super(seed,width,height); }
        @Override boolean walkable(int x,int y) {
            if(!super.walkable(x,y)) return false;
            MapObjectState object=tiles[x][y]=='E'?mapObjectAt(x,y):null;
            return object==null || !"developer-portal".equals(object.type);
        }
    }

    private DeveloperFacilityAuthority() { }
    static boolean isFacility(World world) { return world != null && FACILITIES.containsKey(world); }
    static Layout layout(GamePanel game) { return game == null ? null : FACILITIES.get(game.world); }
    static Session session(GamePanel game) { return game != null && game.getClientProperty(SESSION) instanceof Session s ? s : null; }

    static String help() {
        return "/devroom [enter|exit|hub|combat|systems|trade|supplies|prepare|restock|clear|fund amount|spawn FACTION count tier|animal PROFILE count tier|"
                + "standing FACTION value|reputation FACTION points|vendor FACTION CATEGORY quality|"
                + "select faction/category/quality delta|tier delta|count delta|match size preset seed|scenario delta|step turns|watch|combatstats|cybernetics percent|cover count seed|support A/B item|armor A/B item]. Reputation costs 20 Script per point.";
    }

    static String reject(GamePanel game, String action, List<String> args) {
        if (game == null || game.world == null || game.active == null) return "Start a local run before opening the developer facility.";
        if (action.equals("enter")) return session(game) != null || game.getClientProperty(ACTIVE) != null ? "A developer session is already open." : "";
        if (session(game) == null) return "Enter the developer facility first with /devroom.";
        if (action.equals("exit")) return "";
        if (layout(game) == null) return "Return to the campaign before leaving the developer facility world.";
        try {
            switch (action) {
                case "hub", "combat", "systems", "trade", "supplies", "prepare", "restock", "clear" -> { if (!args.isEmpty()) return help(); }
                case "watch", "combatstats" -> {if(!args.isEmpty()) return help();}
                case "cover" -> {if(args.size()!=2 || integer(args,0)<0 || integer(args,0)>80) return "Use cover count(0..80) seed.";Long.parseLong(args.get(1));}
                case "cybernetics" -> {if(args.size()!=1 || integer(args,0)<0 || integer(args,0)>100) return "Use cybernetics percent(0..100) for subsequent test candidates.";}
                case "step" -> {if(args.size()!=1 || integer(args,0)<1 || integer(args,0)>120) return "Use step turns(1..120).";}
                case "scenario" -> {if(args.size()!=1) return help();integer(args,0);}
                case "support", "armor" -> {
                    if(args.size()<2 || !Set.of("A","B").contains(args.get(0).toUpperCase(Locale.ROOT))) return "Use support/armor A/B item name.";
                    String item=String.join(" ",args.subList(1,args.size()));
                    if(action.equals("support")?!ActorConsumableAuthority.supported(item):!ActorArmorAuthority.wearable(item)) return "Select an item with an active support or armor profile.";
                }
                case "match" -> {if(args.size()!=3 || !DeveloperCombatScenarioAuthority.valid(integer(args,0),args.get(1))) return "Use match size(1/5/10/20) preset(knives/rifles/mixed/heavy/intellect/wall/skills) seed.";Long.parseLong(args.get(2));}
                case "fund" -> { if (args.size()!=1 || integer(args,0)<1 || integer(args,0)>1_000_000) return "Funding must be 1..1000000."; }
                case "spawn" -> {
                    if (args.size()!=3 || faction(args.get(0))==Faction.NONE || integer(args,1)<1 || integer(args,1)>20 || integer(args,2)<0 || integer(args,2)>=ItemQuality.NAMES.length) return "Use spawn FACTION count(1..20) tier(0..7).";
                }
                case "animal" -> {
                    if(args.size()!=3 || animal(args.get(0))==null || integer(args,1)<1 || integer(args,1)>20 || integer(args,2)<0 || integer(args,2)>=ItemQuality.NAMES.length) return "Use animal PROFILE count(1..20) tier(0..7).";
                }
                case "standing", "reputation" -> {
                    if (args.size()!=2 || faction(args.get(0))==Faction.NONE) return "Select a real faction and an integer standing amount.";
                    int value=integer(args,1);
                    if (action.equals("standing") && (value < -100 || value > 100)) return "Standing must be -100..100.";
                    if (action.equals("reputation") && (value<1 || value>25)) return "Purchase 1..25 standing points at a time.";
                }
                case "vendor" -> {
                    if (args.size()!=3) return "Use vendor FACTION CATEGORY quality(0..7).";
                    Faction f=faction(args.get(0)); category(args.get(1));
                    if (f==Faction.NONE || integer(args,2)<0 || integer(args,2)>=ItemQuality.NAMES.length) return help();
                }
                case "select" -> { if (args.size()!=2 || !Set.of("faction","category","quality").contains(args.get(0))) return help(); integer(args,1); }
                case "tier", "count" -> { if(args.size()!=1) return help(); integer(args,0); }
                default -> { return help(); }
            }
        } catch (IllegalArgumentException | IndexOutOfBoundsException failure) { return help(); }
        return "";
    }

    static void execute(GamePanel game, String action, List<String> args) {
        String refusal=reject(game,action,args);
        if(!refusal.isBlank()) { game.logEvent(refusal); return; }
        if(action.equals("enter")) { enter(game); return; }
        if(action.equals("exit")) { leave(game); return; }
        Layout layout=layout(game);
        switch(action) {
            case "hub", "combat", "systems", "trade", "supplies" -> warp(game,action);
            case "prepare" -> {
                game.unlockedKnowledges.addAll(KnowledgeDef.all().keySet());
                for(BuildRecipe recipe:BuildRecipe.allBuildRecipes()) game.unlockedConstructionBlueprints.add(ConstructionBlueprintOwnershipAuthority.blueprintId(recipe));
                game.active.stats.put("Mechanics",20); game.active.stats.put("Intellect",20);
                game.supplies=100000; game.machineParts=100000;
                game.logEvent("Systems prerequisites and workshop resources supplied to this test character.");
            }
            case "fund" -> game.addImperialScript(integer(args,0));
            case "restock" -> { restock(game); game.logEvent("Catalog containers refilled to five of every registered item at every quality."); }
            case "clear" -> clearArena(game);
            case "cover" -> {layout.coverCount=integer(args,0);layout.coverSeed=Long.parseLong(args.get(1));DeveloperCoverLayoutAuthority.scatter(game);}
            case "cybernetics" -> {layout.cyberneticPercent=integer(args,0);game.logEvent("Subsequent test candidates: "+layout.cyberneticPercent+"% seeded chance of fitted cybernetic limbs.");}
            case "match" -> DeveloperCombatScenarioAuthority.prepare(game,integer(args,0),args.get(1),Long.parseLong(args.get(2)));
            case "watch" -> DeveloperCombatScenarioAuthority.toggleWatch(game);
            case "combatstats" -> game.logEvent(DeveloperCombatScenarioAuthority.result(game)+" | "+ActorCombatAuthority.diagnostics(game).summary());
            case "support", "armor" -> {
                String team=args.get(0).toUpperCase(Locale.ROOT),item=String.join(" ",args.subList(1,args.size()));int supplied=0;
                for(NpcEntity npc:game.world.npcs) if(npc.hp>0 && team.equals(npc.combatTeam)) {
                    if(action.equals("support")) {if(npc.combatSupport.items.size()>=64) continue;npc.combatSupport.items.add(item);}
                    else {var slot=ActorArmorAuthority.slot(item);if(slot==CharacterEquipmentAndMedicalAuthority.EquipmentSlot.CLOTHES) npc.equippedArmor=item;else npc.combatSupport.worn.put(slot,item);}
                    supplied++;
                }
                game.logEvent("Team "+team+": "+supplied+" actors supplied with "+item+". Combat supplies are finite; using one spends an NPC action.");
            }
            case "step" -> {for(int i=0;i<integer(args,0);i++) game.advanceTurnBody("");game.logEvent(DeveloperCombatScenarioAuthority.result(game));}
            case "scenario" -> layout.combatPresetIndex=Math.floorMod(layout.combatPresetIndex+integer(args,0),DeveloperCombatScenarioAuthority.PRESETS.length);
            case "spawn" -> spawn(game,faction(args.get(0)),integer(args,1),integer(args,2));
            case "animal" -> spawn(game,Faction.MUTANT,integer(args,1),integer(args,2),animal(args.get(0)));
            case "standing" -> game.factionStanding.put(faction(args.get(0)),integer(args,1));
            case "reputation" -> buyStanding(game,faction(args.get(0)),integer(args,1));
            case "vendor" -> visitVendor(game,faction(args.get(0)),category(args.get(1)),integer(args,2));
            case "tier" -> layout.spawnTier=Math.floorMod(layout.spawnTier+integer(args,0),ItemQuality.NAMES.length);
            case "count" -> layout.spawnCount=Math.max(1,Math.min(20,layout.spawnCount+integer(args,0)));
            case "select" -> {
                int delta=integer(args,1);
                switch(args.get(0)) {
                    case "faction" -> layout.factionIndex=Math.floorMod(layout.factionIndex+delta,Faction.visibleFactions().length);
                    case "category" -> layout.categoryIndex=Math.floorMod(layout.categoryIndex+delta,FactionCriticalVendorPlacementAuthority.Category.values().length);
                    case "quality" -> layout.quality=Math.floorMod(layout.quality+delta,ItemQuality.NAMES.length);
                }
            }
        }
        game.repaint();
    }

    static Session enter(GamePanel campaign) {
        if(campaign.getClientProperty(ACTIVE) instanceof Session existing) return existing;
        GamePanel game=new GamePanel();
        game.timer.stop();
        game.userProfile=campaign.userProfile;
        game.active=campaign.active.copy();
        game.combatSupport.lostParts.addAll(campaign.combatSupport.lostParts);
        for(var limb:campaign.combatSupport.limbs.values()) {var copy=CyberneticLimbAuthority.Limb.read(limb.save());copy.lastTick=game.worldTurn;game.combatSupport.limbs.put(copy.root,copy);}
        game.installedBodyModifications.putAll(campaign.installedBodyModifications);
        game.inventory.addAll(campaign.inventory);
        game.unlockedKnowledges.addAll(campaign.unlockedKnowledges);
        game.equippedLeftHandItem=campaign.equippedLeftHandItem;
        game.equippedRightHandItem=campaign.equippedRightHandItem;
        game.equippedClothing=campaign.equippedClothing;
        game.carriedScript=100000; game.food=100; game.water=100;
        game.options.doomModeEnabled=campaign.options.doomModeEnabled;
        game.options.doomModeFovDegrees=campaign.options.doomModeFovDegrees;
        game.options.singlePlayerTickModeIndex=0;
        game.seed=0xDE7FAC1L;
        game.rng=new Random(game.seed);
        game.worldSetup=WorldSetupSettings.standard();
        game.worldSetup.zoneSize=0;
        Layout layout=new Layout();
        build(game,layout);
        game.atlas=WorldAtlas.preview(game.seed,game.worldSetup);
        game.atlas.slices[0][0][1][1][6]=game.world;
        game.singlePlayerSectorBridge=new SinglePlayerSectorRuntimeBridge(new SectorManager());
        game.singlePlayerSectorBridge.bindSession(game.userProfile);
        Session session=new Session(campaign,game,game.world,campaign.timer!=null && campaign.timer.isRunning());
        warp(game,"hub");
        restock(game);
        spawn(game,Faction.CULTIST,3,0);
        game.putClientProperty(SESSION,session);
        campaign.putClientProperty(ACTIVE,session);
        if(campaign.timer!=null) campaign.timer.stop();
        campaign.screen=GamePanel.Screen.PAUSE;
        campaign.clearPendingMovementInput("developer session entry");
        campaign.firstPersonRenderViewport.updateContinuousMotion(campaign);
        game.logEvent("Developer facility: isolated character and economy. Use the return portal or /devroom exit to resume the campaign. Saves are disabled here.");
        if(!GraphicsEnvironment.isHeadless()) SwingUtilities.invokeLater(()-> {
            Window window=SwingUtilities.getWindowAncestor(campaign);
            if(window instanceof JFrame frame) {
                frame.setContentPane(game); frame.revalidate(); frame.repaint(); game.timer.start(); game.requestFocusInWindow();
            }
        });
        return session;
    }

    static void leave(GamePanel game) {
        DeveloperCombatScenarioAuthority.stop(game);
        Session session=session(game);
        if(session==null) return;
        game.timer.stop();
        game.screen=GamePanel.Screen.PAUSE;
        game.firstPersonRenderViewport.updateContinuousMotion(game);
        SwingUtilities.invokeLater(()-> {
            GamePanel campaign=session.campaign();
            Window window=SwingUtilities.getWindowAncestor(game);
            if(window instanceof JFrame frame) { frame.setContentPane(campaign); frame.revalidate(); frame.repaint(); }
            campaign.putClientProperty(ACTIVE,null);
            game.putClientProperty(SESSION,null);
            FACILITIES.remove(session.facilityWorld());
            if(game.singlePlayerSectorBridge!=null) game.singlePlayerSectorBridge.close();
            game.shutdownRuntime();
            campaign.screen=GamePanel.Screen.GAME; campaign.panelMode=GamePanel.PanelMode.NONE;
            if(session.campaignTimerRunning()) campaign.timer.start();
            campaign.requestFocusInWindow();
            campaign.logEvent("Returned from the developer facility; campaign state resumed.");
        });
    }

    private static void build(GamePanel game, Layout layout) {
        LinkedHashMap<String,List<String>> catalog=new LinkedHashMap<>();
        for(ItemDef item:ItemCatalog.ITEMS.values()) {
            String category=item.category==null?"general":item.category.split("/")[0];
            catalog.computeIfAbsent(category,key->new ArrayList<>()).add(item.name);
        }
        int vendorCount=Faction.visibleFactions().length*FactionCriticalVendorPlacementAuthority.Category.values().length*ItemQuality.NAMES.length;
        int vendorRows=(vendorCount+23)/24;
        int catalogRows=(catalog.size()+19)/20;
        int tradeHeight=vendorRows*3+catalogRows*3+22;
        int buildRows=(BuildRecipe.allBuildRecipes().size()+11)/12;
        int fixtureRows=(FixtureInteractionRegistry.definitions().size()+11)/12;
        int systemsHeight=7+buildRows*5+5+8+4+fixtureRows*4+6;
        World world=new FacilityWorld(game.seed,154,Math.max(66+systemsHeight+4,tradeHeight+6));
        world.configureGenerationSettings(game.worldSetup);
        world.zoneType=ZoneType.NEUTRAL_CIVILIAN_FLOOR;
        world.zoneName="Developer Test Facility"; world.hiveName="Developer Test Facility"; world.floor=4;
        world.zoneX=2;world.zoneY=2;
        for(char[] column:world.tiles) Arrays.fill(column,'#');
        layout.rooms.put("hub",new Rectangle(2,2,56,18));
        layout.rooms.put("combat",new Rectangle(2,24,56,38));
        layout.rooms.put("systems",new Rectangle(2,66,56,systemsHeight));
        layout.rooms.put("trade",new Rectangle(64,2,86,tradeHeight));
        int roomId=0;
        for(var entry:layout.rooms.entrySet()) {
            Rectangle room=entry.getValue();world.rooms.add(room);
            for(int x=room.x;x<room.x+room.width;x++) for(int y=room.y;y<room.y+room.height;y++) { world.tiles[x][y]='.';world.roomIds[x][y]=roomId; }
            RoomProfile profile=RoomProfile.centralPlaza(world.zoneType,false,new Random(roomId));
            profile.name="Developer "+entry.getKey()+" chamber";
            world.roomProfiles.add(profile);
            world.roomFactions.add(Faction.NONE);world.roomSpecials.add(Boolean.TRUE);roomId++;
        }
        game.world=world; FACILITIES.put(world,layout);
        for(var entry:layout.rooms.entrySet()) {
            Rectangle room=entry.getValue();
            portal(world,room.x+1,room.y+2,"hub","Hub teleport door");
            control(world,room.x+3,room.y+2,entry.getKey());
            for(int x=room.x+3;x<room.x+room.width-1;x+=8) for(int y=room.y+4;y<room.y+room.height-1;y+=8)
                world.lightSources.add(new ZoneLightSourceRecord("DEV-LIGHT-"+x+"-"+y,"Developer work light",x,y,world.roomIds[x][y],10,90,"white",true,true,false,99,0,false,"developer"));
        }
        Rectangle hub=layout.rooms.get("hub");
        portal(world,hub.x+8,hub.y+2,"combat","Combat chamber teleport door");
        portal(world,hub.x+14,hub.y+2,"systems","Systems chamber teleport door");
        portal(world,hub.x+20,hub.y+2,"trade","Trade chamber teleport door");
        portal(world,hub.x+26,hub.y+2,"exit","Return to campaign");
        Rectangle systems=layout.rooms.get("systems");int index=0;
        for(BuildRecipe recipe:BuildRecipe.allBuildRecipes()) {
            int x=systems.x+4+(index%12)*4,y=systems.y+7+(index/12)*5;
            BaseObject object=new BaseObject(recipe.name,recipe.symbol,x,y,recipe.supplyCost,recipe.attention);
            object.qualityName=recipe.qualityName;object.description=recipe.description;
            game.configureBaseObject(object);game.baseObjects.add(object);index++;
        }
        int hardwareY=systems.y+7+buildRows*5+5;index=0;
        for(char glyph:new char[]{'1','2','3','4','5','Y','J','B','K','O','Z','P','F','U','x','I'}) {
            int x=systems.x+4+(index%12)*4,y=hardwareY+(index/12)*4;
            MapObjectState object=glyph>='1'&&glyph<='5'?MapObjectState.vending(x,y,glyph,world.zoneType)
                    :glyph=='I'?MapObjectState.shrine(x,y,glyph,world.zoneType):MapObjectState.emergencyMachine(x,y,glyph,world.zoneType);
            world.mapObjects.add(object);world.tiles[x][y]=glyph;index++;
        }
        int fixtureY=hardwareY+12;index=0;
        for(FixtureInteractionRegistry.Definition definition:FixtureInteractionRegistry.definitions()) {
            MapObjectState object=new MapObjectState();object.id="DEV-HARDWARE-"+index;object.type=definition.type;
            object.label=definition.type.replace('-',' ');object.glyph='o';
            object.x=systems.x+4+(index%12)*4;object.y=fixtureY+(index/12)*4;
            object.stockState=RoomFixtureInteractionAuthority.stockFor(definition.type,world.roomProfiles.get(2),world,2,'.');
            world.mapObjects.add(object);index++;
        }
        Rectangle trade=layout.rooms.get("trade");index=0;
        String token=UUID.randomUUID().toString();
        for(int quality=0;quality<ItemQuality.NAMES.length;quality++) for(var category:FactionCriticalVendorPlacementAuthority.Category.values()) for(Faction faction:Faction.visibleFactions()) {
            int x=trade.x+4+(index%24)*3,y=trade.y+7+(index/24)*3;
            NpcEntity npc=NpcEntity.create(faction,world.zoneType,x,y,new Random(game.seed+index));
            npc.id="DEV-VENDOR-"+token+"-"+index;npc.name=faction.label+" / "+category.role+" / "+ItemQuality.NAMES[quality];
            npc.role=category.role;npc.state="Trade";npc.developerStationary=true;npc.hp=9999;
            npc.ageYears=35;npc.ageBand="adult";
            npc.equippedMeleeWeapon="";npc.equippedRangedWeapon="";npc.equippedExplosive="";
            world.npcs.add(npc);layout.vendors.put(npc.id,new Vendor(faction,category,quality,x,y));index++;
        }
        int catalogY=trade.y+vendorRows*3+11;index=0;
        for(var entry:catalog.entrySet()) {
            int x=trade.x+4+(index%20)*4,y=catalogY+(index/20)*3;
            MapObjectState chest=new MapObjectState();chest.id="DEV-CATALOG-"+index;chest.type="developer-catalog-container";
            chest.label="Catalog supply: "+entry.getKey();chest.x=x;chest.y=y;chest.glyph='s';chest.stockState="developer-catalog";
            world.mapObjects.add(chest);layout.supplies.put(game.persistentContainerIdForObject(chest),List.copyOf(entry.getValue()));index++;
        }
        compileExhibitTiles(world);
        game.rebuildItemContainersFromLegacyLists();
    }

    private static void compileExhibitTiles(World world) {
        world.compiledTileDescriptors=new CompiledTileDescriptor[world.w][world.h];
        CompiledTileDescriptor wall=TileDataCompilationAuthority.resolveFresh(world,0,0,'#');
        for(CompiledTileDescriptor[] column:world.compiledTileDescriptors) Arrays.fill(column,wall);
        // The curated chambers deliberately share a fixed material within each room.
        for(Rectangle room:world.rooms) {
            CompiledTileDescriptor floor=TileDataCompilationAuthority.resolveFresh(world,room.x+1,room.y+1,'.');
            for(int x=room.x;x<room.x+room.width;x++) for(int y=room.y;y<room.y+room.height;y++) world.compiledTileDescriptors[x][y]=floor;
        }
        for(MapObjectState object:world.mapObjects) if(world.tiles[object.x][object.y]!='.')
            world.compiledTileDescriptors[object.x][object.y]=TileDataCompilationAuthority.resolveFresh(world,object.x,object.y,world.tiles[object.x][object.y]);
        TileDataCompilationAuthority.Result result=new TileDataCompilationAuthority.Result();
        for(CompiledTileDescriptor[] column:world.compiledTileDescriptors) for(CompiledTileDescriptor descriptor:column) TileDataCompilationAuthority.tally(result,descriptor);
        world.compiledTileDescriptorSummary=result.summary();
    }

    private static void portal(World world,int x,int y,String target,String label) {
        MapObjectState object=new MapObjectState();object.id="DEV-PORTAL-"+x+"-"+y;object.x=x;object.y=y;object.glyph='E';
        object.type="developer-portal";object.label=label;object.stockState="target="+target;
        world.mapObjects.add(object);world.tiles[x][y]='E';
    }
    private static void control(World world,int x,int y,String chamber) {
        MapObjectState object=new MapObjectState();object.id="DEV-CONTROL-"+chamber;object.x=x;object.y=y;object.glyph='x';
        object.type="developer-control";object.label="Developer "+chamber+" controls";object.stockState="chamber="+chamber;
        world.mapObjects.add(object);
    }

    static boolean tryInteract(GamePanel game,int x,int y) {
        if(layout(game)==null) return false;
        MapObjectState object=game.world.mapObjectAt(x,y);
        if(object==null || !(object.type.equals("developer-portal") || object.type.equals("developer-control") || object.type.equals("developer-catalog-container"))) return false;
        if(Math.max(Math.abs(game.playerX-x),Math.abs(game.playerY-y))>1) { game.logEvent("Approach this player-only developer control first.");return true; }
        if(object.type.equals("developer-portal")) execute(game,MapObjectState.stockValue(object.stockState,"target"),List.of());
        else if(object.type.equals("developer-catalog-container")) game.openDeveloperSupplyContainer(object);
        else game.openDeveloperFacilityControl(object);
        return true;
    }

    static void warp(GamePanel game,String chamber) {
        Rectangle room=layout(game).rooms.get(chamber.equals("supplies")?"trade":chamber);
        if(room==null) return;
        Point landing=chamber.equals("hub")?new Point(room.x+6,room.y+2):new Point(room.x+2,room.y+(chamber.equals("combat")?9:7));
        if(chamber.equals("supplies")) {
            MapObjectState first=game.world.mapObjects.stream().filter(o->"developer-catalog-container".equals(o.type)).findFirst().orElseThrow();
            landing=new Point(first.x-2,first.y);
        }
        for(int y=landing.y;y<room.y+room.height-1;y++) {
            if(game.world.walkable(landing.x,y) && game.world.npcAt(landing.x,y)==null && game.baseObjectAt(landing.x,y)==null) {
                WorldCommandRuntimeContexts.fromGamePanel(game).teleportPlayer(landing.x,y,"developer "+chamber+" portal");
                game.firstPersonRenderViewport=new FirstPersonRenderViewport();
                game.screen=GamePanel.Screen.GAME;game.panelMode=GamePanel.PanelMode.NONE;return;
            }
        }
        game.logEvent("Developer landing is occupied.");
    }

    static void restock(GamePanel game) {
        for(var entry:layout(game).supplies.entrySet()) {
            game.ensureContainer(entry.getKey(),"Developer catalog supply");
            ContainerRecord container=game.itemContainers.get(entry.getKey());
            HashMap<String,Integer> counts=new HashMap<>();
            for(String id:container.itemInstanceIds) { ItemInstance item=game.itemInstances.get(id);if(item!=null) counts.merge(item.displayName,1,Integer::sum); }
            for(String name:entry.getValue()) for(int quality=0;quality<ItemQuality.NAMES.length;quality++) {
                String item=quality==2?name:ItemQuality.NAMES[quality]+" "+name;
                for(int i=counts.getOrDefault(item,0);i<SUPPLY_COUNT;i++)
                    game.addItemToContainerResult(entry.getKey(),container.label,item,ItemProvenanceRecord.found(item,game.world,game.turn,"Developer catalog issue"),null,"developer restock");
            }
        }
    }

    static void configureTrade(GamePanel game,NpcEntity npc,TraderSession trader) {
        Layout layout=layout(game);
        Vendor vendor=layout==null||npc==null?null:layout.vendors.get(npc.id);
        if(vendor==null) return;
        trader.marketFaction=vendor.faction();trader.marketCategory=vendor.category().id;
        for(TradeOffer offer:trader.offers) {
            if(offer.constructionBlueprintId!=null && !offer.constructionBlueprintId.isBlank()) continue;
            ItemDef definition=ItemCatalog.get(offer.name);
            if(definition!=null) offer.name=vendor.quality()==2?definition.name:ItemQuality.NAMES[vendor.quality()]+" "+definition.name;
        }
    }

    static void visitVendor(GamePanel game,Faction faction,FactionCriticalVendorPlacementAuthority.Category category,int quality) {
        for(Vendor vendor:layout(game).vendors.values()) if(vendor.faction()==faction && vendor.category()==category && vendor.quality()==quality) {
            WorldCommandRuntimeContexts.fromGamePanel(game).teleportPlayer(vendor.x()+1,vendor.y(),"developer vendor index");
            game.firstPersonRenderViewport=new FirstPersonRenderViewport();
            game.screen=GamePanel.Screen.GAME;game.panelMode=GamePanel.PanelMode.NONE;return;
        }
    }

    static boolean buyStanding(GamePanel game,Faction faction,int points) {
        if(!isFacility(game.world) || points<1 || points>25) return false;
        int current=game.factionStanding.getOrDefault(faction,0);
        if(current+points>100) { game.logEvent("Standing purchase would exceed 100; no Script spent.");return false; }
        int cost=points*SCRIPT_PER_STANDING;
        if(!game.spendImperialScript(cost)) { game.logEvent("Standing purchase needs "+cost+" Script; no standing changed.");return false; }
        game.factionStanding.put(faction,current+points);
        game.logEvent("Purchased "+points+" standing with "+faction.label+" for "+cost+" Script in this test session.");return true;
    }

    static void spawn(GamePanel game,Faction faction,int count,int tier) {
        spawn(game,faction,count,tier,null);
    }
    private static void spawn(GamePanel game,Faction faction,int count,int tier,AnimalProfile animal) {
        Layout layout=layout(game);Rectangle arena=layout.rooms.get("combat");
        long existing=game.world.npcs.stream().filter(n->arena.contains(n.x,n.y)).count();
        if(existing+count>128) { game.logEvent("Arena is limited to 128 actors; clear it before spawning more.");return; }
        for(int i=0;i<count;i++) {
            Point p=null;
            for(int y=arena.y+9;y<arena.y+arena.height-2 && p==null;y++) for(int x=arena.x+8;x<arena.x+arena.width-2;x++)
                if(game.world.npcAt(x,y)==null && game.world.walkable(x,y) && !(game.playerX==x && game.playerY==y)) { p=new Point(x,y);break; }
            if(p==null) break;
            Random random=new Random(game.seed+(++layout.spawnSerial));
            NpcEntity npc=animal==null?NpcEntity.create(faction,ZoneType.GANGER_TURF,p.x,p.y,random)
                    :AnimalPopulationApi.createAnimal(animal,faction,ZoneType.MUTANT_WARRENS,p.x,p.y,random,null);
            npc.state="Hostile";npc.id="DEV-COMBAT-"+layout.spawnSerial;npc.equipmentTier=tier;
            npc.equippedMeleeWeapon="";npc.equippedRangedWeapon="";npc.equippedArmor="";npc.ensureCombatLoadout(new Random(layout.spawnSerial));
            if(tier!=2) {
                if(!npc.equippedMeleeWeapon.isBlank()) npc.equippedMeleeWeapon=ItemQuality.NAMES[tier]+" "+npc.equippedMeleeWeapon;
                if(!npc.equippedRangedWeapon.isBlank()) npc.equippedRangedWeapon=ItemQuality.NAMES[tier]+" "+npc.equippedRangedWeapon;
                if(!npc.equippedArmor.isBlank()) npc.equippedArmor=ItemQuality.NAMES[tier]+" "+npc.equippedArmor;
            }
            npc.hp=(animal==null?12:animal.hp)+tier*6;CyberneticLimbAuthority.seedCandidate(game,npc,layout.cyberneticPercent,Objects.hash(game.seed,npc.id,layout.spawnSerial));game.world.npcs.add(npc);layout.arenaActors.add(npc.id);
        }
        game.world.dirtyVisionRevision++;game.logEvent("Arena actors spawned hostile to the player: "+faction.label+", tier "+tier+".");
    }
    static void clearArena(GamePanel game) {
        DeveloperCombatScenarioAuthority.stop(game);
        Layout layout=layout(game);Rectangle arena=layout.rooms.get("combat");
        game.world.npcs.removeIf(n->arena.contains(n.x,n.y) && !n.developerStationary);
        game.world.mapObjects.removeIf(o->arena.contains(o.x,o.y) && (o.type==null || !o.type.startsWith("developer-")));
        DeveloperCoverLayoutAuthority.clear(game);
        layout.arenaActors.clear();game.world.dirtyVisionRevision++;game.logEvent("Combat chamber reset; traders and controls retained.");
    }
    static Faction faction(String value) { return Faction.valueOf(value.toUpperCase(Locale.ROOT)); }
    private static AnimalProfile animal(String id) {
        for(AnimalProfile[] group:new AnimalProfile[][]{AnimalPopulationApi.SEWER_WILD,AnimalPopulationApi.URBAN_WILD,AnimalPopulationApi.FARM,AnimalPopulationApi.KENNEL,AnimalPopulationApi.PETS})
            for(AnimalProfile profile:group) if(profile.id.equalsIgnoreCase(id)) return profile;
        return null;
    }
    static FactionCriticalVendorPlacementAuthority.Category category(String value) { return FactionCriticalVendorPlacementAuthority.Category.valueOf(value.toUpperCase(Locale.ROOT)); }
    private static int integer(List<String> args,int index) { return Integer.parseInt(args.get(index)); }
}
