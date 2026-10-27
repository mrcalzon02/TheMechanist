package mechanist;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.*;
import javax.imageio.ImageIO;

/** Executable replacement lifecycle, resource, persistence and deterministic pool contracts. */
final class CyberneticLimbExecutionSmoke {
    private static int checks;
    private static void require(boolean ok,String message) {checks++;if(!ok) throw new IllegalStateException(message);}
    private static void supplies(GamePanel g,String item) {g.inventory.add(item);g.inventory.add("Sterile surgical kit");g.inventory.add("Augmetic calibration tools");}
    public static void main(String[] args) throws Exception {
        Path out=Path.of(args.length==0?"diagnostics/cybernetics-20261026":args[0]);Files.createDirectories(out);
        GamePanel campaign=new GamePanel();campaign.timer.stop();campaign.world=new World(19191,32,24);campaign.world.zoneType=ZoneType.NEUTRAL_CIVILIAN_FLOOR;
        for(char[] c:campaign.world.tiles) Arrays.fill(c,'.');campaign.active=Candidate.random(new Random(3));campaign.seed=19191;
        campaign.singlePlayerSectorBridge=new SinglePlayerSectorRuntimeBridge(new SectorManager());
        DeveloperFacilityAuthority.Session session=null;
        try {
            campaign.combatSupport.limbs.put("R Upper Arm",new CyberneticLimbAuthority.Limb("R Upper Arm","Industrial cybernetic arm","ISOLATION",2,500));
            session=DeveloperFacilityAuthority.enter(campaign);GamePanel g=session.facility();g.timer.stop();g.options.soundEnabled=false;g.options.musicEnabled=false;DeveloperFacilityAuthority.clearArena(g);
            require(g.combatSupport.limbs.get("R Upper Arm").lastTick==g.worldTurn,"Facility rebases copied hardware clock");
            g.combatSupport.limbs.get("R Upper Arm").power=1;require(campaign.combatSupport.limbs.get("R Upper Arm").power==100,"Facility hardware is deeply isolated from campaign");
            BaseObject clinic=g.baseObjects.stream().filter(b->b.symbol=='M').findFirst().orElseThrow();
            g.playerX=clinic.x+1;g.playerY=clinic.y;g.active.stats.put("Mechanics",12);g.active.stats.put("Intellect",12);
            g.unlockedKnowledges.add("Cybernetic Surgery");g.unlockedKnowledges.add("Emergency Medical Authority");
            StringBuilder report=new StringBuilder("Cybernetic lifecycle checks\n");
            for(var p:CyberneticLimbAuthority.PARTS.values()) for(int quality=0;quality<8;quality++) {
                g.active=Candidate.random(new Random(3));g.active.stats.put("Mechanics",12);g.active.stats.put("Intellect",12);g.combatSupport.restore("");g.inventory.clear();
                String root="R "+switch(p.segment()){case "Arm"->"Upper Arm";case "Leg"->"Upper Leg";default->p.segment();};
                String item=quality==2?p.item():ItemQuality.NAMES[quality]+" "+p.item();
                require(CyberneticLimbAuthority.part(item)==p,"Quality identity: "+item);
                MedicalTreatmentAuthority.sever(g.active,root);supplies(g,item);
                require(CyberneticLimbAuthority.execute(g,null,"install",root,item),"Fit "+item);
                var l=g.combatSupport.limbs.get(root);require(l!=null && l.online() && !g.active.body.get(root).missing,"Fitted assembly restores region");
                require(!g.inventory.contains(item) && !g.inventory.contains("Sterile surgical kit") && g.inventory.contains("Augmetic calibration tools"),"Finite surgery resources");
                String saved=g.combatSupport.save();var restored=new ActorConsumableAuthority.State();restored.restore(saved);
                require(restored.save().equals(saved),"Round-trip fitted hardware state");
                CyberneticLimbAuthority.damage(g,null,root,10);int integrity=l.integrity;
                MedicalTreatmentAuthority.heal(g,null,100);require(l.integrity==integrity,"Organic medicine cannot repair hardware");
                g.inventory.add("Augmetic repair kit");require(CyberneticLimbAuthority.execute(g,null,"repair",root,"") && l.integrity>integrity,"Finite mechanical repair");
                l.jammed=true;l.calibration=0;require(CyberneticLimbAuthority.execute(g,null,"calibrate",root,"") && l.online(),"Calibration clears lockup");
                if(p.powered()) {l.power=0;require(!l.online(),"Power loss disables hardware");g.inventory.add("Augmetic power cell");require(CyberneticLimbAuthority.execute(g,null,"recharge",root,"") && l.online(),"Power-cell restoration");}
                g.inventory.add("Sterile surgical kit");require(CyberneticLimbAuthority.execute(g,null,"remove",root,"") && g.active.body.get(root).missing && !g.combatSupport.limbs.containsKey(root),"Removal leaves absent limb");
                require(g.inventory.contains("Removed augmetic scrap") && CyberneticLimbAuthority.part("Removed augmetic scrap")==null,"Removal cannot mint reinstallable parts");
            }
            g.active=Candidate.random(new Random(7));g.active.stats.put("Mechanics",12);g.active.stats.put("Intellect",12);g.combatSupport.restore("");g.inventory.clear();
            supplies(g,"Industrial cybernetic arm");long turn=g.worldTurn;int count=g.inventory.size();
            require(!CyberneticLimbAuthority.execute(g,null,"install","Torso","Industrial cybernetic arm") && g.worldTurn==turn && g.inventory.size()==count,"Incompatible surgery refuses atomically");
            int x=g.playerX;g.playerX=2;require(!CyberneticLimbAuthority.execute(g,null,"install","R Upper Arm","Industrial cybernetic arm") && g.inventory.size()==count,"Clinic requirement");g.playerX=x;
            g.unlockedKnowledges.remove("Cybernetic Surgery");require(!CyberneticLimbAuthority.execute(g,null,"install","R Upper Arm","Industrial cybernetic arm"),"Training requirement");g.unlockedKnowledges.add("Cybernetic Surgery");
            // Exact quality consumption must not silently take a different copy of the same item.
            g.inventory.add(0,"Junk Industrial cybernetic arm");
            require(CyberneticLimbAuthority.execute(g,null,"install","R Upper Arm","Industrial cybernetic arm") && g.inventory.contains("Junk Industrial cybernetic arm"),"Exact physical quality copy consumed");
            var arm=g.combatSupport.limbs.get("R Upper Arm");arm.power=0;g.equippedRightHandItem="Stub pistol";g.activeWeaponHandIndex=1;
            require(!MedicalTreatmentAuthority.attackRejection(g,StructuralScaleCombatAuthority.weaponProfile("Stub pistol","SNAP")).isEmpty(),"Offline holding arm prevents firing");
            arm.power=100;g.inventory.add("Military cybernetic arm");g.inventory.add("Sterile surgical kit");
            require(CyberneticLimbAuthority.execute(g,null,"install","R Upper Arm","Military cybernetic arm") && g.combatSupport.limbs.size()==1,"Upgrade replaces assembly without stacking");
            require(CyberneticLimbAuthority.bonus(g.combatSupport,false)>0,"Powered arm changes effective combat skill");
            String limbSave=g.combatSupport.save();var one=new ActorConsumableAuthority.State();one.restore(limbSave);var two=new ActorConsumableAuthority.State();two.restore(limbSave);
            g.combatSupport.restore(one.save());long start=g.worldTurn;for(int i=0;i<100;i++){g.worldTurn++;CyberneticLimbAuthority.tick(g,null);}String stepped=g.combatSupport.save();
            g.combatSupport.restore(two.save());g.worldTurn=start+100;CyberneticLimbAuthority.tick(g,null);require(g.combatSupport.save().equals(stepped),"Catch-up and stepwise wear/power are identical");String stable=g.combatSupport.save();CyberneticLimbAuthority.tick(g,null);require(stable.equals(g.combatSupport.save()),"Same-clock upkeep is idempotent");
            NpcEntity npc=NpcEntity.create(Faction.IMPERIAL_GUARD,ZoneType.NEUTRAL_CIVILIAN_FLOOR,g.playerX,g.playerY+1,new Random(9));npc.id="SURGERY-PATIENT";npc.role="Patient";npc.state="Idle";npc.combatTeam="";npc.intellect=12;npc.combatSupport.items.clear();g.world.npcs.add(npc);
            npc.combatSupport.items.addAll(java.util.List.of("Industrial cybernetic arm","Sterile surgical kit","Augmetic calibration tools"));
            MedicalTreatmentAuthority.sever(npc,"L Upper Arm");require(CyberneticLimbAuthority.execute(g,npc,"install","L Upper Arm","Industrial cybernetic arm") && MedicalTreatmentAuthority.hands(npc)==2,"NPC replacement restores hand capability");
            var nl=npc.combatSupport.limbs.get("L Upper Arm");nl.power=0;require(MedicalTreatmentAuthority.hands(npc)==1,"NPC offline arm loses capability");
            npc.combatSupport.items.add("Augmetic power cell");g.turn++;npc.lastTickTurn=-1;require(CyberneticLimbAuthority.autoService(g,npc) && !npc.combatSupport.items.contains("Augmetic power cell") && !CyberneticLimbAuthority.autoService(g,npc),"NPC finite service takes one action");
            NpcEntity loaded=NpcEntity.parseLine(npc.saveLine(),g.world);require(loaded.combatSupport.save().equals(npc.combatSupport.save()),"NPC fitted state persists");
            MedicalTreatmentAuthority.sever(npc,"L Lower Arm");require(npc.combatSupport.limbs.isEmpty() && MedicalTreatmentAuthority.hands(npc)==1,"Severing fitted limb cannot be undone by later sync");g.world.npcs.remove(npc);
            report.append("16 part definitions x 8 qualities: lifecycle, treatment separation, resource use, fitting, repair, calibration and removal.\n");
            int realized=0;StringBuilder distribution=new StringBuilder("seed,selected,root,item,quality\n");
            for(int seed=0;seed<2000;seed++) {
                NpcEntity a=NpcEntity.create(Faction.IMPERIAL_GUARD,ZoneType.GANGER_TURF,10,10,new Random(seed));a.combatSupport.restore("");
                require(!CyberneticLimbAuthority.seedCandidate(g,a,0,seed) && a.combatSupport.limbs.isEmpty(),"Zero-percent organic controls");
                boolean chosen=CyberneticLimbAuthority.seedCandidate(g,a,25,seed);String first=a.combatSupport.save();a.combatSupport.restore("");
                require(CyberneticLimbAuthority.seedCandidate(g,a,25,seed)==chosen && first.equals(a.combatSupport.save()),"Seed replay");
                if(chosen) {realized++;var l=a.combatSupport.limbs.values().iterator().next();distribution.append(seed+",true,"+l.root+","+l.item+","+l.quality+"\n");}
                a.combatSupport.restore("");require(CyberneticLimbAuthority.seedCandidate(g,a,100,seed),"100-percent positive controls");
            }
            require(realized>400 && realized<600,"25-percent pool distribution: "+realized);Files.writeString(out.resolve("candidate-distribution.csv"),distribution);
            for(int size:new int[]{1,5,10,20}) {DeveloperFacilityAuthority.layout(g).cyberneticPercent=100;DeveloperCombatScenarioAuthority.prepare(g,size,"mixed",71000);require(g.world.npcs.stream().filter(n->n.id.startsWith("MATCH-")).allMatch(n->!n.combatSupport.limbs.isEmpty()),"Arena pool integration "+size);}
            DeveloperFacilityAuthority.clearArena(g);g.playerX=clinic.x+1;g.playerY=clinic.y;g.setSize(1280,800);g.buttons.clear();
            BufferedImage image=new BufferedImage(980,380,BufferedImage.TYPE_INT_RGB);Graphics2D graphics=image.createGraphics();graphics.setColor(new Color(22,24,27));graphics.fillRect(0,0,980,380);
            CyberneticControlSurface.paint(g,graphics,new Rectangle(8,8,960,350),"R Upper Arm");for(ButtonBox button:g.buttons) button.draw(graphics,g.smallFont,false,null,g.options);graphics.dispose();ImageIO.write(image,"png",out.resolve("medical-controls.png").toFile());
            report.append("25% seeded pool: "+realized+"/2000 fitted; 0% and 100% controls and exact replay passed.\n");report.append("PASS "+checks+" checks.\n");Files.writeString(out.resolve("cybernetic-results.txt"),report);System.out.println(report);
        } finally {if(session!=null){DeveloperCombatScenarioAuthority.stop(session.facility());session.facility().timer.stop();DeveloperFacilityAuthority.leave(session.facility());}campaign.timer.stop();campaign.singlePlayerSectorBridge.close();campaign.shutdownRuntime();}
    }
}
