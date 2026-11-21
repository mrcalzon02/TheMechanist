package mechanist;

final class ViewportFogSmoke {
    public static void main(String[] args) {
        DoomDepthFog fog=new DoomDepthFog();DoomFogSettings settings=DoomFogSettings.fromOptions(null);
        int source=0xFFC0B0A0;
        require(fog.applyFogArgb(source,settings,12)==source,"Nearby surfaces must remain free of distance fog");
        int middle=fog.applyFogArgb(source,settings,32);
        require(((middle>>16)&255)>170,"Architecture at 32 tiles must retain most of its contrast");
        require(fog.applyFogArgb(source,settings,96)==ViewportDarknessAuthority.DARKEST,"Far fog must converge on the blue baseline");
        for(int d=0;d<=96;d++)require(fog.applyFogArgb(source,settings,d)==fog.applyFogArgbDistanceSquared(source,settings,d*d),"Radial/squared distance disagreement at "+d);
        int previous=255;
        for(int d=12;d<=96;d++){int red=(fog.applyFogArgb(source,settings,d)>>16)&255;require(red<=previous,"Fog must blend monotonically");previous=red;}
        System.out.println("ViewportFogSmoke PASS nearClear=12 gentleAt32=true farBlue=96 radialParity=true");
    }
    private static void require(boolean okay,String message){if(!okay)throw new AssertionError(message);}
}
