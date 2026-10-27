package mechanist;

import java.awt.*;
import java.util.*;
import java.util.List;

/** Procedures live in the existing Medical tab and use the sector command bridge. */
final class CyberneticControlSurface {
    private static final Map<GamePanel,Integer> SELECTION=new WeakHashMap<>();
    private static final Map<GamePanel,String> PATIENT=new WeakHashMap<>();
    private CyberneticControlSurface() { }
    static void paint(GamePanel game,Graphics2D g,Rectangle box,String region) {
        ArrayList<NpcEntity> nearby=new ArrayList<>();
        for(NpcEntity n:game.world.npcs) if(n.hp>0 && !n.developerStationary && !n.isAnimalActor() && n.faction!=Faction.ROGUE_MACHINE && Math.abs(n.x-game.playerX)+Math.abs(n.y-game.playerY)<=2) nearby.add(n);
        NpcEntity selected=null;for(NpcEntity n:nearby) if(n.id.equals(PATIENT.get(game))) selected=n;
        final NpcEntity patient=selected;String patientId=patient==null?"":patient.id;
        var state=CyberneticLimbAuthority.state(game,patient);
        var limb=CyberneticLimbAuthority.covering(state,region);
        String root=limb==null?region:limb.root;
        ArrayList<String> carried=new ArrayList<>();
        for(String item:patient==null?game.inventory:patient.combatSupport.items) if(CyberneticLimbAuthority.fits(CyberneticLimbAuthority.part(item),root) && !carried.contains(item)) carried.add(item);
        int index=Math.floorMod(SELECTION.getOrDefault(game,0),Math.max(1,carried.size()));
        String item=carried.isEmpty()?"":carried.get(index);
        g.setColor(new Color(222,201,139));g.setFont(game.smallFont);
        List<String> lines=List.of("Patient: "+(patient==null?"Player":patient.name)+" / "+root,
                limb==null?"No fitted hardware.":limb.item+" / "+ItemQuality.NAMES[limb.quality]+" / "+(limb.online()?"online":"OFFLINE"),
                limb==null?"Select a compatible carried replacement.":"Integrity "+limb.integrity+"% / power "+limb.power+"% / calibration "+limb.calibration+"%",
                "Replacement: "+(item.isEmpty()?"none carried":item),
                "Install consumes part + sterile kit. Removal leaves an absent limb and scrap.");
        int columns=3,height=24,rows=3;
        int maxLines=Math.max(1,Math.min(lines.size(),(box.height-rows*(height+4)-12)/17));
        int y=box.y+16;for(int line=0;line<maxLines;line++){g.drawString(GuiLayoutApi.fitLabel(lines.get(line),g.getFontMetrics(),box.width-12),box.x+6,y);y+=17;}
        String[] actions={"patient","next","install","remove","repair","recharge","calibrate"};
        int width=(box.width-12-(columns-1)*6)/columns;
        for(int i=0;i<actions.length;i++) {String action=actions[i];int bx=box.x+6+(i%columns)*(width+6),by=y+4+(i/columns)*(height+4);
            String tooltip=action.equals("patient")?"Cycle player and adjacent patients. Supplies must be carried by the patient.":action.equals("next")?"Cycle compatible carried replacements.":CyberneticLimbAuthority.rejection(game,patient,action,root,item);
            game.buttons.add(new ButtonBox(GuiLayoutApi.fitLabel(action.equals("install")?"Install / upgrade":action,g.getFontMetrics(),width-8),bx,by,width,height,tooltip,()->{
                if(action.equals("patient")) {int next=patient==null?0:nearby.indexOf(patient)+1;PATIENT.put(game,next>=nearby.size()?"":nearby.get(next).id);}
                else if(action.equals("next")) SELECTION.put(game,index+1);
                else game.singlePlayerSectorBridge.submitCommand(game,new CyberneticCommand(SinglePlayerSectorRuntimeBridge.LOCAL_PLAYER_ID,action,root,item,patientId));
                game.repaint();
            },null));
        }
    }
}
