package mechanist;

/** Diffuse arcology light in confirmed open spaces; no sun or sky texture is implied. */
final class WorldAmbientLightAuthority {
    static final int OPEN_SPACE_BASELINE=28;
    private WorldAmbientLightAuthority() { }
    static int at(World world,int x,int y,int enclosedAmbient) {
        if(world==null || !world.inBounds(x,y))return enclosedAmbient;
        var structure=TileStructureAuthority.at(world,x,y);
        int diffuse=switch(structure.roof()){case OPEN -> OPEN_SPACE_BASELINE;case GLASS -> 22;case GRATING -> 18;case COVERED -> enclosedAmbient;};
        if(structure.finish()==TileStructureAuthority.Finish.LIT)diffuse=Math.max(diffuse,35);
        return Math.max(enclosedAmbient,diffuse);
    }
}
