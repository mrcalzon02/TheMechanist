package mechanist;

import java.awt.Rectangle;
import java.util.*;

/** Seeded placement of existing passive-cover buildables in the isolated combat chamber. */
final class DeveloperCoverLayoutAuthority {
    static void clear(GamePanel game) {
        var layout=DeveloperFacilityAuthority.layout(game);
        for(BaseObject b:layout.coverObjects){game.baseObjects.remove(b);game.world.tiles[b.x][b.y]='.';refresh(game,b.x,b.y);}
        layout.coverObjects.clear();game.world.dirtyVisionRevision++;
    }
    static void scatter(GamePanel game) {
        clear(game);var layout=DeveloperFacilityAuthority.layout(game);Random random=new Random(layout.coverSeed);
        Rectangle room=layout.rooms.get("combat");int cx=room.x+25,cy=room.y+13;
        for(int attempt=0;attempt<2000 && layout.coverObjects.size()<layout.coverCount;attempt++) {
            int x=cx-10+random.nextInt(22),y=cy-3+random.nextInt(12);
            if(!room.contains(x,y) || game.world.tiles[x][y]!='.' || game.world.npcAt(x,y)!=null || game.baseObjectAt(x,y)!=null || x==game.playerX && y==game.playerY)continue;
            boolean sandbags=random.nextBoolean();BaseObject b=new BaseObject(sandbags?"Sandbag Line":"Scrap Barricade",sandbags?'S':'d',x,y,0,0);
            game.configureBaseObject(b);b.integrity=Math.max(40,b.integrity);b.description+=" Seeded developer cover; seed "+layout.coverSeed+".";
            game.baseObjects.add(b);layout.coverObjects.add(b);game.world.tiles[x][y]=b.symbol;refresh(game,x,y);
        }
        game.world.dirtyVisionRevision++;game.logEvent("Seeded cover: "+layout.coverObjects.size()+" objects / seed "+layout.coverSeed+". Existing passive-cover protection and tile traversal apply.");
    }
    private static void refresh(GamePanel game,int x,int y) {if(game.world.compiledTileDescriptors!=null)game.world.compiledTileDescriptors[x][y]=TileDataCompilationAuthority.resolveFresh(game.world,x,y,game.world.tiles[x][y]);}
}
