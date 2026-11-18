package mechanist;

import java.awt.Rectangle;
import java.util.*;

/** Optical openings, supported movement, crowd safety and real paid material variants. */
final class StructureMaterialsSmoke {
    public static void main(String[] args) {
        World w=new World(1137,12,12);for(char[] c:w.tiles)Arrays.fill(c,'.');
        TileStructureAuthority.open(w,new Rectangle(0,0,12,12));
        TileStructureAuthority.roof(w,4,4,TileStructureAuthority.Roof.GLASS,1.2);
        var glass=ray(w,4.55,.58,4.55,0,1,0,.0001,true);
        require(glass!=null && close(glass.height(),1.2) && StructureSurfaceAuthority.glass(w,glass),"Glass canopy must produce a translucent layer");
        var behind=ray(w,4.55,.58,4.55,0,1,0,glass.distance()+.00001,true);
        require(behind!=null && close(behind.height(),10),"Glass must reveal the next level underside, not an empty local ceiling");
        require(!StructureSurfaceAuthority.glass(w,behind),"Distant concrete underside must remain opaque above a glass roof");
        TileStructureAuthority.roof(w,4,4,TileStructureAuthority.Roof.GRATING,1.2);
        require(close(ray(w,4.55,.58,4.55,0,1,0,.0001,true).height(),10),"Grating aperture must expose geometry beyond canopy");
        require(close(ray(w,4.505,.58,4.55,0,1,0,.0001,true).height(),1.2),"Grating bars must remain opaque");
        w.tiles[4][4]='#';var m=TileStructureAuthority.at(w,4,4);
        TileStructureAuthority.set(w,4,4,m.withWall(TileStructureAuthority.Wall.PILLAR,TileStructureAuthority.Finish.PANELS));
        require(ray(w,2.5,.58,4.1,1,0,0,.0001,true)==null,"View beside a pillar must remain open");
        require(ray(w,2.5,.58,4.5,1,0,0,.0001,true)!=null,"Pillar center must occlude");
        require(!w.walkable(4,4),"Pillar cell must retain grid collision");
        TileStructureAuthority.set(w,4,4,m.withWall(TileStructureAuthority.Wall.PIPE,TileStructureAuthority.Finish.STAINED));
        require(ray(w,2.5,.58,4.3,1,0,0,.0001,true)==null,"Pipe must have narrower round silhouette");
        require(ray(w,2.5,.58,4.5,1,0,0,.0001,true)!=null,"Pipe core must occlude");
        w.tiles[4][4]='.';
        TileStructureAuthority.set(w,4,4,m.withFloor(TileStructureAuthority.Floor.GRATING,2));
        require(w.walkable(4,4),"Grating floor must support weight even at a visible aperture");
        require(ray(w,4.55,.58,4.55,0,-1,0,.0001,true)==null,"Catwalk floor openings must be visible below");
        require(!DeveloperBattleMapAuthority.step(w,4,4,5,4) && !DeveloperBattleMapAuthority.step(w,5,4,4,4),"Rail crossing must be blocked in both directions");
        require(!DeveloperBattleMapAuthority.step(w,4,4,5,5),"Diagonal rail bypass must be blocked");
        require(ray(w,4.5,.63,4.7,1,0,0,.0001,true)==null,"Rail span must be see-through at eye height");
        var rail=ray(w,4.5,.84,4.7,1,0,0,.0001,true);
        require(rail!=null && rail.kind()==StructureSurfaceAuthority.Kind.RAIL,"Rail bar must be rendered as geometry");
        TileStructureAuthority.set(w,5,4,TileStructureAuthority.at(w,5,4).withFloor(TileStructureAuthority.Floor.VOID,0));
        require(!w.walkable(5,4) && !DeveloperBattleMapAuthority.step(w,5,3,5,4),"Unsupported abyss must reject movement without needing a rail");
        var grid=StructuredZoneGridRuntime.rebuildGridForWorld(w);
        require(grid.requireTile(5,4).hasFlag(ZoneTileState.TileFlag.BLOCKS_MOVEMENT),"Structured grid must preserve unsupported floor collision");
        ZoneTileState[][] crowd=new ZoneTileState[3][1];for(int x=0;x<3;x++)crowd[x][0]=ZoneTileState.fromLegacyGlyph('.');
        crowd[0][0].setOccupantEntityId("A");crowd[1][0].setOccupantEntityId("B");
        crowd[1][0].setStructure(TileStructureAuthority.DEFAULT.withFloor(TileStructureAuthority.Floor.SOLID,2));
        var shoved=ZoneTileMovementResolutionAuthority.resolve(crowd,List.of(new ZoneTileMovementResolutionAuthority.MoveIntent("A",0,0,1,0,100)));
        require(shoved.outcomeFor("A")==null || !shoved.outcomeFor("A").moved(),"Crowd resolver must not push a character through a rail");
        TileStructureAuthority.set(w,2,2,TileStructureAuthority.at(w,2,2).withWall(TileStructureAuthority.Wall.SOLID,TileStructureAuthority.Finish.LIT));
        require(w.lightSources.stream().filter(l->l.id.equals("STRUCTURE-LIGHT-2-2")).count()==1,"Lit finish must register a real light source");
        Properties saved=new Properties();TileStructureAuthority.write(w,saved);
        World restored=new World(1137,12,12);TileStructureAuthority.read(restored,saved);
        require(restored.tileStructures.equals(w.tileStructures),"All material and railing state must survive save/load");
        TileStructureAuthority.read(restored,saved);
        require(restored.lightSources.stream().filter(l->l.id.equals("STRUCTURE-LIGHT-2-2")).count()==1,"Reload must not duplicate wall lights");
        var snapshot=TileSnapshot.fromDescriptor(w,4,4,'.',true,true,null);
        require(snapshot.floorSurface().equals("GRATING") && snapshot.railingEdges()==2,"Snapshots must include floor and edge state");
        TileStructureAuthority.roof(w,4,4,TileStructureAuthority.Roof.COVERED,2);
        require(TileStructureAuthority.at(w,4,4).floor()==TileStructureAuthority.Floor.GRATING && TileStructureAuthority.at(w,4,4).railings()==2,"Roof edits must preserve independent materials and rails");
        buildVariants();
        System.out.println("StructureMaterialsSmoke PASS glassLayers=true grateApertures=true pillarPipe=true railsBothDirections=true diagonalSafe=true abyssSafe=true crowdSafe=true saveLoad=true snapshot=true paidVariants=true");
    }
    private static DeveloperBattleMapAuthority.Surface ray(World w,double x,double h,double y,double dx,double dh,double dy,double min,boolean optical) {
        return StructureSurfaceAuthority.cast(w,x,h,y,dx,dh,dy,12,min,optical);
    }
    private static void buildVariants() {
        GamePanel g=new GamePanel();g.timer.stop();
        try {
            g.world=new World(1137,12,12);for(char[] c:g.world.tiles)Arrays.fill(c,'.');
            TileStructureAuthority.open(g.world,new Rectangle(0,0,12,12));g.world.tiles[3][4]='#';g.world.tiles[3][5]='#';
            g.active=Candidate.random(new Random(3));g.playerX=2;g.playerY=5;g.supplies=100;g.machineParts=100;
            g.options.musicEnabled=false;g.options.soundEnabled=false;
            BuildRecipe[] recipes={BuildRecipe.glassRoofPanel(),BuildRecipe.gratingRoofPanel()};
            for(int i=0;i<recipes.length;i++) {
                BuildRecipe recipe=recipes[i];int y=4+i;
                for(var c:recipe.componentCosts.entrySet()) {
                    require(ItemCatalog.get(c.getKey())!=null,"Unknown glazing/grating construction input");
                    for(int n=0;n<c.getValue();n++)g.inventory.add(c.getKey());
                }
                int supplies=g.supplies,parts=g.machineParts;
                g.pendingBuildRecipe=recipe;g.buildX=4;g.buildY=y;
                require(g.rawCanPlacePendingBuildAt(4,y).equalsIgnoreCase("ok"),"Variant must use normal supported placement: "+g.rawCanPlacePendingBuildAt(4,y));
                g.confirmBuildPlacement();BaseObject site=g.baseObjects.get(g.baseObjects.size()-1);
                require(site.underConstruction && TileStructureAuthority.at(g.world,4,y).roof()==TileStructureAuthority.Roof.OPEN,"Unpaid labor must not provide a roof");
                ProgressiveConstructionAuthority.contribute(g,site,recipe.baseTurns,false);
                require(!site.underConstruction && RoofConstructionAuthority.overhead(site),"Completed variant must be overhead");
                require(TileStructureAuthority.at(g.world,4,y).roof()==(i==0?TileStructureAuthority.Roof.GLASS:TileStructureAuthority.Roof.GRATING),"Paid variant must apply the requested material");
                require(g.supplies==supplies-recipe.supplyCost && g.machineParts==parts-recipe.partCost,"Variant must charge supplies and parts once");
                for(var c:recipe.componentCosts.entrySet())require(g.countProductionInput(c.getKey())==0,"Variant named components must be consumed once");
                g.pendingBuildRecipe=recipe;int before=g.supplies;g.confirmBuildPlacement();require(g.supplies==before,"Duplicate variant must not charge");
            }
            g.world.tiles[4][2]='#';TileStructureAuthority.set(g.world,4,2,TileStructureAuthority.at(g.world,4,2).withWall(TileStructureAuthority.Wall.GLASS,TileStructureAuthority.Finish.PLAIN));
            require(g.hasLineOfSight(4,1,4,3),"Planner vision must see through glass wall");
            require(!DeveloperBattleMapAuthority.clearLine(g.world,4,1,4,3),"Glass visibility must not grant a clear firing line through intact glass");
            TileStructureAuthority.set(g.world,4,5,TileStructureAuthority.at(g.world,4,5).withFloor(TileStructureAuthority.Floor.SOLID,1));
            require(new MechanistContinuousCollisionGrid(g.world,4,5).tileAt(4,4)==ContinuousTileType.SOLID_WALL,"Full mouse-look movement must respect railing edges");
            g.playerX=4;g.playerY=5;
            require(!MovementExecutionAuthority.executeStep(g,0,-1,"railing test",false).success(),"Normal player movement must respect railing edges");
        } finally {g.shutdownRuntime();}
    }
    private static boolean close(double a,double b){return Math.abs(a-b)<1e-7;}
    private static void require(boolean okay,String message){if(!okay)throw new AssertionError(message);}
}
