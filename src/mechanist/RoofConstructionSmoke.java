package mechanist;

import java.awt.Rectangle;
import java.util.*;

/** Pays for a real wall/door perimeter and its nine roof tiles through normal placement and labor. */
final class RoofConstructionSmoke {
    public static void main(String[] args) {
        GamePanel game=new GamePanel();game.timer.stop();
        try {
            game.world=new World(74193,14,14);game.world.zoneType=ZoneType.NEUTRAL_CIVILIAN_FLOOR;
            for(char[] column:game.world.tiles)Arrays.fill(column,'.');
            TileStructureAuthority.open(game.world,new Rectangle(0,0,14,14));
            game.active=Candidate.random(new Random(3));game.playerX=2;game.playerY=5;
            game.options.soundEnabled=false;game.options.musicEnabled=false;game.supplies=500;game.machineParts=500;
            BuildRecipe wall=BuildRecipe.reinforcedWallPanel(),door=BuildRecipe.reinforcedDoor(),roof=BuildRecipe.roofPanel();
            for(BuildRecipe recipe:List.of(wall,door,roof,BuildRecipe.cot()))for(var component:recipe.componentCosts.entrySet()) {
                require(ItemCatalog.get(component.getKey())!=null,"Unknown roof/wall component: "+component.getKey());
                for(int i=0;i<component.getValue()*40;i++)game.inventory.add(component.getKey());
            }
            int initialSupplies=game.supplies,initialParts=game.machineParts;
            int walls=0;
            for(int x=3;x<=7;x++)for(int y=3;y<=7;y++)if(x==3 || x==7 || y==3 || y==7) {
                BuildRecipe recipe=x==5 && y==3?door:wall;
                build(game,recipe,x,y);if(recipe==wall)walls++;
            }
            require(walls==15,"Room perimeter must contain fifteen walls and one door");
            require(!game.world.walkable(3,5) && !game.world.walkable(5,3),"Constructed walls/closed door must enclose the room");
            int paidRoofSupplies=game.supplies,paidRoofParts=game.machineParts;
            int plates=game.countProductionInput("Scrap plate");
            for(int x=4;x<=6;x++)for(int y=4;y<=6;y++) {
                BaseObject object=build(game,roof,x,y);
                require(RoofConstructionAuthority.overhead(object),"Completed roof must be an overhead receipt");
                require(game.world.tiles[x][y]=='.' && game.world.walkable(x,y),"Roof must preserve passable ground");
                require(game.baseObjectAt(x,y)==null,"Roof must not occupy furniture slot");
                require(TileStructureAuthority.at(game.world,x,y).roof()==TileStructureAuthority.Roof.COVERED,"Roof metadata missing");
            }
            require(game.supplies==paidRoofSupplies-9*roof.supplyCost && game.machineParts==paidRoofParts-9*roof.partCost,"Roof cost must scale with all nine tiles");
            require(game.countProductionInput("Scrap plate")==plates-18,"Roof must consume named components exactly once");
            require(game.supplies==initialSupplies-15*wall.supplyCost-door.supplyCost-9*roof.supplyCost,"Wall and roofing costs must be cumulative");
            require(game.machineParts==initialParts-15*wall.partCost-door.partCost-9*roof.partCost,"Wall and roof machine parts must be cumulative");
            game.pendingBuildRecipe=roof;game.buildX=5;game.buildY=5;
            int before=game.supplies;game.confirmBuildPlacement();
            require(game.supplies==before,"Duplicate roof must not charge materials");
            require(game.rawCanPlacePendingBuildAt(11,11).contains("supporting wall"),"Floating roof must be refused");
            build(game,BuildRecipe.cot(),5,5);
            require(game.baseObjectAt(5,5)!=null,"Furniture must remain buildable beneath completed roof");
            System.out.println("RoofConstructionSmoke PASS walls=15 door=1 roofTiles=9 paidPerTile=true duplicateAtomic=true unsupportedDenied=true furnitureBelow=true");
        } finally {game.shutdownRuntime();}
    }
    private static BaseObject build(GamePanel game,BuildRecipe recipe,int x,int y) {
        game.pendingBuildRecipe=recipe;game.buildX=x;game.buildY=y;
        require(game.rawCanPlacePendingBuildAt(x,y).equalsIgnoreCase("ok"),"Placement refused: "+recipe.name+" "+x+","+y+" "+game.rawCanPlacePendingBuildAt(x,y));
        game.confirmBuildPlacement();BaseObject site=game.baseObjects.get(game.baseObjects.size()-1);
        require(site.underConstruction,"New structure must require labor");
        if(RoofConstructionAuthority.isRoof(recipe))require(TileStructureAuthority.at(game.world,x,y).roof()==TileStructureAuthority.Roof.OPEN,"Unfinished roof must not provide coverage");
        ProgressiveConstructionAuthority.contribute(game,site,recipe.baseTurns,false);
        require(!site.underConstruction,"Paid labor must finish the structure");return site;
    }
    private static void require(boolean okay,String message){if(!okay)throw new AssertionError(message);}
}
