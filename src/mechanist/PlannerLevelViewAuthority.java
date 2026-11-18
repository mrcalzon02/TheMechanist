package mechanist;

import java.awt.event.KeyEvent;
import java.util.WeakHashMap;

/** Inspection cursor only. Never changes the player's world, atlas coordinates or turn. */
final class PlannerLevelViewAuthority {
    private static final WeakHashMap<GamePanel,Selection> VIEWS=new WeakHashMap<>();
    private record Selection(World origin,int level,int panX,int panY) { }
    private PlannerLevelViewAuthority() { }
    static int occupied(GamePanel game) {
        if(DeveloperBattleMapAuthority.active(game.world))return (int)Math.floor(DeveloperBattleMapAuthority.height(game.world,game.playerX,game.playerY)+.001);
        return (game.world.floor-1)*2+(game.world.sewerLayer?0:1);
    }
    static int level(GamePanel game) {Selection s=VIEWS.get(game);return s==null || s.origin()!=game.world?occupied(game):s.level();}
    static boolean inspecting(GamePanel game) {return game.world!=null && level(game)!=occupied(game);}
    static int panX(GamePanel game){Selection s=VIEWS.get(game);return s!=null && s.origin()==game.world?s.panX():0;}
    static int panY(GamePanel game){Selection s=VIEWS.get(game);return s!=null && s.origin()==game.world?s.panY():0;}
    static void reset(GamePanel game){VIEWS.remove(game);game.repaint();}
    static boolean handleKey(GamePanel game,int code) {
        if(game.world==null || game.options.doomModeEnabled || game.screen!=GamePanel.Screen.GAME || game.panelMode!=GamePanel.PanelMode.NONE)return false;
        if(code==KeyEvent.VK_HOME || code==KeyEvent.VK_ESCAPE && inspecting(game)){reset(game);return true;}
        if(code==KeyEvent.VK_PAGE_UP || code==KeyEvent.VK_PAGE_DOWN) {
            boolean battle=DeveloperBattleMapAuthority.active(game.world);
            if(!battle && game.atlas==null){game.logEvent("No other Z levels are available in this world.");return true;}
            int next=Math.max(0,Math.min(battle?2:19,level(game)+(code==KeyEvent.VK_PAGE_UP?1:-1)));
            VIEWS.put(game,new Selection(game.world,next,panX(game),panY(game)));game.repaint();return true;
        }
        if(!inspecting(game))return false;
        int dx=code==KeyEvent.VK_LEFT || code==KeyEvent.VK_A?-1:code==KeyEvent.VK_RIGHT || code==KeyEvent.VK_D?1:0;
        int dy=code==KeyEvent.VK_UP || code==KeyEvent.VK_W?-1:code==KeyEvent.VK_DOWN || code==KeyEvent.VK_S?1:0;
        if(dx!=0 || dy!=0)VIEWS.put(game,new Selection(game.world,level(game),Math.max(-game.world.w,Math.min(game.world.w,panX(game)+dx)),Math.max(-game.world.h,Math.min(game.world.h,panY(game)+dy))));
        game.repaint();return true;
    }
    static World viewedWorld(GamePanel game) {
        if(DeveloperBattleMapAuthority.active(game.world) || !inspecting(game))return game.world;
        WorldAtlas atlas=game.atlas;if(atlas==null)return null;
        int selected=level(game),floor=selected/2+1;boolean sewer=selected%2==0;
        return atlas.slices[game.world.sectorX-1][game.world.sectorY-1][game.world.zoneX-1][game.world.zoneY-1][(floor-1)*2+(sewer?1:0)];
    }
    static String label(GamePanel game) {
        int z=level(game);
        return DeveloperBattleMapAuthority.active(game.world)?"Z +"+z:"Floor "+(z/2+1)+(z%2==0?" sewer":" ground");
    }
}
