package mechanist;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.*;
import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;

/** Seeded structural combat matrix plus lifecycle checks of the real developer-room targets. */
final class CombatFixtureExecutionSmoke {
    private static int checks;
    private static void require(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }

    public static void main(String[] args) throws Exception {
        System.setProperty("java.awt.headless", "true");
        Path out = Path.of(args.length == 0 ? "diagnostics/combat-fixtures-20261027" : args[0]);
        Files.createDirectories(out);
        GamePanel game = new GamePanel();
        game.shutdownRuntime();
        game.options.soundEnabled = false;
        game.options.musicEnabled = false;
        game.active = Candidate.random(new Random(91000));
        try {
            String first = matrix(game);
            require(first.equals(matrix(game)), "Entire structural matrix must replay exactly");
            Files.writeString(out.resolve("attacks.tsv"), first);
            boundaries(game);
            components(game, out);
            facility(game, out);
            Files.writeString(out.resolve("capabilities.tsv"),
                    "capability\tstatus\tevidence\n"
                    + "Player attacks against four turret mounts\tEXECUTED\tattacks.tsv; actual BaseObject integrity\n"
                    + "Player attacks against five vehicle classes\tEXECUTED\tattacks.tsv; actual vehicle armor component\n"
                    + "Armor exhaustion then chassis damage\tEXECUTED\tbounded repeated Arc Rifle attacks until wreck\n"
                    + "Vehicle component disable and persisted readback\tEXECUTED\tcomponents.tsv; direct component-authority fault injection, not aimed gunfire\n"
                    + "Autonomous turret targeting, power, ammunition, crew\tGAP\tPassiveDefenseEffectsAuthority turret doctrine reserves these systems\n"
                    + "Vehicle-mounted firing against actors or vehicles\tGAP\tNo vehicle weapon execution authority; handheld mounted-name classification is not a mounted gun\n"
                    + "NPC attacks against structural fixtures\tGAP\tActorCombatAuthority currently selects actor targets\n"
                    + "Aimed component selection\tGAP\tStructural combat selects armor, then frame; other components tested by explicit fault injection\n"
                    + "Vehicle road movement, crew, fuel and service\tSEPARATE REGRESSIONS\tRun included existing vehicle transit/access/manifest/fuel/maintenance entry points\n"
                    + "Destroyed turret cover and raid resistance\tEXECUTED\tZero integrity removes both passive bonuses\n");
            String result = "CombatFixtureExecutionSmoke PASS checks=" + checks
                    + " attacks=486 replayAttacks=486 turretTypes=4 vehicleClasses=5 seeds=3"
                    + " realStructuralExecution=true autonomousTurretFire=GAP mountedVehicleFire=GAP\n";
            Files.writeString(out.resolve("results.txt"), result);
            System.out.print(result);
        } finally { game.shutdownRuntime(); }
    }

    private static void reset(GamePanel game, long seed) {
        game.world = new World(seed, 24, 16);
        game.world.zoneType = ZoneType.NEUTRAL_CIVILIAN_FLOOR;
        game.world.npcs.clear(); game.world.mapObjects.clear();
        for (char[] column : game.world.tiles) Arrays.fill(column, '.');
        game.baseObjects.clear(); game.terrainIntegrity.clear(); game.loadedWeaponShots.clear();
        game.combatSupport.restore(""); game.inventory.clear();
        game.playerX=5; game.playerY=8; game.combatX=6; game.combatY=8;
        game.turn=100; game.worldTurn=100; game.seed=seed; game.rng=new Random(seed);
        ActorCombatFeedbackAuthority.clear(game);
    }

    private static void equip(GamePanel game, String weapon, int mode, int shots) {
        game.equippedLeftHandItem="LEFT EMPTY"; game.equippedRightHandItem=weapon;
        game.activeWeaponHandIndex=1; game.selectedFireModeIndex=mode;
        game.loadedWeaponShots.put(weapon,shots);
    }

    private static String matrix(GamePanel game) {
        StringBuilder rows = new StringBuilder("seed\ttarget\tweapon\tmode\tcomponent\tbefore\tafter\treportedDamage\tammoSpent\tturnsSpent\n");
        for (long seed : new long[]{91000,91001,91002})
            for (var target : DeveloperCombatFixtureAuthority.TARGETS)
                for (String weapon : new String[]{"Scrap knife","Chainsword","Needle Pistol","Light Rifle","Heavy stubber","Arc Rifle"})
                    for (int mode=0;mode<3;mode++) {
                        reset(game,seed); DeveloperCombatFixtureAuthority.create(game,target,6,8,seed);
                        equip(game,weapon,mode,12);
                        var before=StructuralScaleCombatAuthority.preview(game);
                        require(before.available(),"Prepared target must be attackable: "+target.id()+" / "+weapon);
                        int ammo=game.loadedWeaponShots.get(weapon), turn=game.turn;
                        var result=StructuralScaleCombatAuthority.confirm(game);
                        var after=StructuralScaleCombatAuthority.preview(game);
                        int lost=before.target().currentIntegrity()-after.target().currentIntegrity();
                        require(result.success() && game.turn==turn+1,"Executed structural attack spends one turn");
                        require(ammo-game.loadedWeaponShots.get(weapon)==result.ammunitionSpent(),"Finite ammunition matches attack record");
                        require(lost==result.damage() && result.changed()==(lost>0),"Exact physical integrity loss matches record");
                        require(lost>=0 && lost<=30,"Structural damage remains bounded");
                        if (target.vehicle()==VehicleRuntimeAuthority.VehicleClass.TANK
                                && !weapon.equals("Arc Rifle")) require(lost==0,"Light weapons cannot penetrate tank armor");
                        rows.append(seed).append('\t').append(target.id()).append('\t').append(weapon).append('\t')
                                .append(game.fireModeLabel()).append('\t').append(before.target().vehicleComponent()).append('\t')
                                .append(before.target().currentIntegrity()).append('\t').append(after.target().currentIntegrity()).append('\t')
                                .append(result.damage()).append('\t').append(result.ammunitionSpent()).append('\t').append(game.turn-turn).append('\n');
                    }
        return rows.toString();
    }

    private static void boundaries(GamePanel game) {
        var tank = DeveloperCombatFixtureAuthority.TARGETS.get(8);
        reset(game,91000); DeveloperCombatFixtureAuthority.create(game,tank,9,8,91000);
        game.combatX=9; equip(game,"Arc Rifle",1,100);
        game.world.tiles[7][8]='#';
        String state=game.world.mapObjectAt(9,8).stockState; int turn=game.turn;
        require(!StructuralScaleCombatAuthority.confirm(game).success() && game.turn==turn
                && game.loadedWeaponShots.get("Arc Rifle")==100 && state.equals(game.world.mapObjectAt(9,8).stockState),"Wall refusal is atomic");
        game.world.tiles[7][8]='.'; game.loadedWeaponShots.put("Arc Rifle",0);
        require(!StructuralScaleCombatAuthority.confirm(game).success() && game.turn==turn
                && state.equals(game.world.mapObjectAt(9,8).stockState),"Dry-fire refusal is atomic");
        equip(game,"Scrap knife",0,0);
        require(!StructuralScaleCombatAuthority.confirm(game).success() && game.turn==turn,"Out-of-range melee refusal spends no turn");
        equip(game,"Arc Rifle",1,100);
        MapObjectState vehicle=game.world.mapObjectAt(9,8);
        boolean frameHit=false;
        for(int i=0;i<60 && !VehicleRuntimeAuthority.inspect(game.world,vehicle).condition().equals("wreck");i++) {
            var before=StructuralScaleCombatAuthority.preview(game).target();
            var hit=StructuralScaleCombatAuthority.confirm(game);
            require(hit.success(),"Repeated loaded anti-armor fire must execute");
            var snap=VehicleRuntimeAuthority.inspect(game.world,vehicle);
            require(hit.damage()==before.currentIntegrity()-snap.components().get(before.vehicleComponent()),"Final hit reports actual integrity lost, not unclamped overkill");
            require(snap.components().values().stream().allMatch(v->v>=0 && v<=100),"Vehicle components never underflow");
            if(before.vehicleComponent()==VehicleRuntimeAuthority.Component.FRAME) frameHit=true;
        }
        require(frameHit && VehicleRuntimeAuthority.inspect(game.world,vehicle).condition().equals("wreck"),"Armor must be exhausted before chassis damage creates a wreck");
        for(var target:DeveloperCombatFixtureAuthority.TARGETS) if(target.vehicle()==null) {
            reset(game,91000); DeveloperCombatFixtureAuthority.create(game,target,6,8,91000);
            BaseObject turret=game.baseObjectAt(6,8);
            require(PassiveDefenseEffectsAuthority.coverPenalty(turret)>0 && PassiveDefenseEffectsAuthority.raidResistance(turret)>0,"Intact turret supplies its passive defenses");
            turret.integrity=3; equip(game,"Arc Rifle",1,12);
            var result=StructuralScaleCombatAuthority.confirm(game);
            require(result.success() && result.damage()==3 && turret.integrity==0,"Destruction records exact remaining turret integrity");
            require(PassiveDefenseEffectsAuthority.coverPenalty(turret)==0 && PassiveDefenseEffectsAuthority.raidResistance(turret)==0,"Destroyed turret supplies no passive cover or raid resistance");
        }
    }

    private static void components(GamePanel game, Path out) throws Exception {
        StringBuilder rows=new StringBuilder("vehicle\tcomponent\tbefore\tafter\tcondition\toperationState\tmethod\n");
        for(var target:DeveloperCombatFixtureAuthority.TARGETS) if(target.vehicle()!=null)
            for(var component:VehicleRuntimeAuthority.Component.values()) {
                reset(game,91000); DeveloperCombatFixtureAuthority.create(game,target,6,8,91000);
                MapObjectState vehicle=game.world.mapObjectAt(6,8);
                var result=VehicleRuntimeAuthority.applyDamage(vehicle,component,100,game.turn,"controlled component-disable test");
                var snapshot=VehicleRuntimeAuthority.inspect(game.world,vehicle);
                require(result.success() && result.amount()==100 && snapshot.components().get(component)==0,"Component fault injection mutates the real vehicle");
                if(Set.of(VehicleRuntimeAuthority.Component.FRAME,VehicleRuntimeAuthority.Component.POWERPLANT,
                        VehicleRuntimeAuthority.Component.DRIVE,VehicleRuntimeAuthority.Component.MOBILITY).contains(component))
                    require(snapshot.operationState().equals("disabled"),"Destroyed critical component disables operation");
                MapObjectState restored=new MapObjectState(); restored.id=vehicle.id; restored.type=vehicle.type;
                restored.label=vehicle.label; restored.x=vehicle.x; restored.y=vehicle.y; restored.stockState=vehicle.stockState;
                require(VehicleRuntimeAuthority.inspect(game.world,restored).components().equals(snapshot.components()),"Component truth reconstructs from persisted stock state");
                rows.append(target.id()).append('\t').append(component).append("\t100\t0\t").append(snapshot.condition()).append('\t')
                        .append(snapshot.operationState()).append("\tcomponent-authority fault injection\n");
            }
        Files.writeString(out.resolve("components.tsv"),rows);
    }

    private static void facility(GamePanel campaign, Path out) throws Exception {
        reset(campaign,91000);
        campaign.singlePlayerSectorBridge=new SinglePlayerSectorRuntimeBridge(new SectorManager());
        var session=DeveloperFacilityAuthority.enter(campaign);
        GamePanel game=session.facility(); game.timer.stop(); game.options.soundEnabled=false;
        try {
            var layout=DeveloperFacilityAuthority.layout(game);
            require(!DeveloperFacilityAuthority.reject(game,"fixtures",List.of("bogus","1")).isBlank(),"Invalid fixture requests are rejected");
            DeveloperFacilityAuthority.execute(game,"fixtures",List.of("all","91000"));
            require(layout.combatFixtures.size()==4 && layout.combatVehicles.size()==5,"All nine fixture types are placed");
            var first=layout.combatVehicles.stream().map(v->v.type+":"+v.x+","+v.y+":"+v.stockState).toList();
            DeveloperFacilityAuthority.execute(game,"fixtures",List.of("all","91000"));
            require(first.equals(layout.combatVehicles.stream().map(v->v.type+":"+v.x+","+v.y+":"+v.stockState).toList()),"Fixture identity and initial state replay by seed without duplicates");
            for(BaseObject b:layout.combatFixtures) require(PassiveDefenseEffectsAuthority.profile(b.symbol)!=null,"Turrets retain their existing passive profiles");
            layout.cyberneticPercent=0; DeveloperCombatScenarioAuthority.prepare(game,5,"rifles",71000);
            require(layout.combatFixtures.size()==4 && layout.combatVehicles.size()==5,"Fixture selection persists into prepared actor battles");
            require(game.world.npcs.stream().filter(n->!n.combatTeam.isBlank()).count()==10,"Actor match remains intact with fixtures");
            game.setSize(1280,800); game.options.worldZoomIndex=2; game.options.doomModeFovDegrees=100;
            game.screen=GamePanel.Screen.GAME; game.panelMode=GamePanel.PanelMode.NONE; game.active.stats.put("Vision",42);
            for(int i=0;i<9;i++) {
                int x,y; String id=DeveloperCombatFixtureAuthority.TARGETS.get(i).id();
                if(i<4){var target=layout.combatFixtures.get(i);x=target.x;y=target.y;}
                else{var target=layout.combatVehicles.get(i-4);x=target.x;y=target.y;}
                game.playerX=x; game.playerY=y-3; game.setFacingToward(x,y,"fixture inspection");
                game.firstPersonRenderViewport=new FirstPersonRenderViewport();
                game.putClientProperty("mechanist.captureClock",1000000L+i*1000);
                equip(game,"Arc Rifle",1,12); game.combatX=x; game.combatY=y;
                require(StructuralScaleCombatAuthority.confirm(game).success(),"Inspected fixture accepts an actual native attack");
                game.putClientProperty("mechanist.captureClock",1000050L+i*1000);
                for(boolean doom:new boolean[]{false,true}) {
                    game.options.doomModeEnabled=doom;
                    BufferedImage image=new BufferedImage(1280,800,BufferedImage.TYPE_INT_RGB);
                    Graphics2D graphics=image.createGraphics(); game.paint(graphics); graphics.dispose();
                    ImageIO.write(image,"png",out.resolve(id+(doom?"-doom.png":"-tiles.png")).toFile());
                }
            }
            DeveloperFacilityAuthority.execute(game,"fixtures",List.of("none","91000"));
            require(layout.combatFixtures.isEmpty() && layout.combatVehicles.isEmpty(),"Clearing targets removes owned fixtures");
            require(game.baseObjects.stream().noneMatch(b->layout.rooms.get("combat").contains(b.x,b.y)),"No orphaned turret bases remain");
            require(!game.baseObjects.isEmpty(),"Systems-room buildables survive target cleanup");
        } finally {
            DeveloperFacilityAuthority.leave(game); SwingUtilities.invokeAndWait(()->{});
            campaign.singlePlayerSectorBridge.close();
        }
    }
    private CombatFixtureExecutionSmoke() { }
}
