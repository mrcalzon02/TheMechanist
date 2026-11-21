package mechanist;

/** Roof panels use existing paid, staged construction; completed panels occupy overhead space. */
final class RoofConstructionAuthority {
    static final String RECIPE="Roof Panel";
    static final String GLASS="Glass Roof Panel",GRATING="Grating Roof Panel";
    static final double CLEARANCE=1.2;
    private RoofConstructionAuthority() { }
    private static boolean roofName(String name){return RECIPE.equals(name) || GLASS.equals(name) || GRATING.equals(name);}
    static boolean isRoof(BuildRecipe recipe){return recipe!=null && roofName(recipe.name);}
    static boolean isRoof(BaseObject object){return object!=null && (roofName(object.assignedRecipe) || roofName(object.name));}
    static boolean overhead(BaseObject object){return isRoof(object) && !object.underConstruction;}
    static String placementProblem(GamePanel game,BuildRecipe recipe,int x,int y) {
        if(!isRoof(recipe))return "ok";
        if(TileStructureAuthority.at(game.world,x,y).hasRoof())return "tile already has a roof";
        for(int[] d:new int[][]{{1,0},{-1,0},{0,1},{0,-1}})for(int distance=1;distance<=3;distance++) {
            int wx=x+d[0]*distance,wy=y+d[1]*distance;
            if(!game.world.inBounds(wx,wy))break;
            char tile=game.world.tiles[wx][wy];
            if((tile=='#' || tile=='W' || tile=='D') && TileStructureAuthority.at(game.world,wx,wy).wallTopElevation()>=DeveloperBattleMapAuthority.height(game.world,x,y)+CLEARANCE)return "ok";
        }
        return "roof panel needs a supporting wall within three tiles";
    }
    static void complete(GamePanel game,BaseObject site) {
        if(!overhead(site))return;
        String name=site.assignedRecipe==null || site.assignedRecipe.isBlank()?site.name:site.assignedRecipe;
        var material=GLASS.equals(name)?TileStructureAuthority.Roof.GLASS:GRATING.equals(name)?TileStructureAuthority.Roof.GRATING:TileStructureAuthority.Roof.COVERED;
        TileStructureAuthority.roof(game.world,site.x,site.y,material,CLEARANCE);
        site.symbol=site.constructionOriginalTile==0?'.':site.constructionOriginalTile;
        site.description="Paid "+material.name().toLowerCase(java.util.Locale.ROOT)+" roofing over this tile, clearance "+CLEARANCE+" storeys. Ground surface and furniture space remain available.";
    }
}
