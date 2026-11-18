package mechanist;

import java.awt.Rectangle;
import java.util.*;

final class ArchitecturalClutterSmoke {
    public static void main(String[] args) {
        World w=hall(74193),other=hall(74193);
        ArchitecturalClutterAuthority.enable(w,true);ArchitecturalClutterAuthority.enable(other,true);
        EnumSet<ArchitecturalClutterAuthority.Feature> features=EnumSet.noneOf(ArchitecturalClutterAuthority.Feature.class);
        Set<Integer> generations=new HashSet<>();int pieces=0;
        for(int x=1;x<w.w-1;x++)for(int z=1;z<w.h-1;z++) {
            var cell=ArchitecturalClutterAuthority.pieces(w,x,z);
            require(Arrays.equals(cell,ArchitecturalClutterAuthority.pieces(other,x,z)),"Same saved seed and recipe must reproduce identical geometry");
            for(var p:cell) {
                features.add(p.feature());generations.add(p.generation());pieces++;
                require(p.x0()>=x && p.x1()<=x+1 && p.z0()>=z && p.z1()<=z+1,"Piece escaped its spatial cell");
                require(p.x1()>p.x0() && p.h1()>p.h0() && p.z1()>p.z0(),"Degenerate clutter box");
                if(p.feature()==ArchitecturalClutterAuthority.Feature.DUCT || p.feature()==ArchitecturalClutterAuthority.Feature.SWAG || p.feature()==ArchitecturalClutterAuthority.Feature.HANGER)
                    require(p.h0()>=1.12-1e-8,"Hanging services must clear the existing 1.1-high character geometry");
            }
            require(w.walkable(x,z),"Wall/ceiling detail must not take over walkable ground");
        }
        require(features.size()==ArchitecturalClutterAuthority.Feature.values().length,"Missing feature examples: "+features);
        require(generations.size()==4,"Architecture must retain several repair generations");
        for(int x=1;x<w.w-1;x++)for(int z=8;z<12;z++) {
            TileStructureAuthority.roof(w,x,z,TileStructureAuthority.Roof.COVERED,1.2);
            for(var piece:ArchitecturalClutterAuthority.pieces(w,x,z))if(piece.feature()==ArchitecturalClutterAuthority.Feature.DUCT || piece.feature()==ArchitecturalClutterAuthority.Feature.SWAG || piece.feature()==ArchitecturalClutterAuthority.Feature.HANGER)
                require(piece.h0()>=1.12-1e-8,"Low-roof services must flatten above character head clearance");
        }
        var hit=StructureSurfaceAuthority.cast(w,4.5,.28,1.5,0,0,-1,4,.0001,true);
        require(hit!=null && hit.kind()==StructureSurfaceAuthority.Kind.CLUTTER && hit.distance()<.5,"Wall pipe must protrude and depth-occlude before its host wall");
        require(hit.clutter()!=null,"Viewport hit must identify the actual clutter feature");
        require(DeveloperBattleMapAuthority.step(w,4,1,5,1),"Service aisle must remain traversable");
        var left=Arrays.stream(ArchitecturalClutterAuthority.pieces(w,4,1)).filter(p->p.feature()==ArchitecturalClutterAuthority.Feature.SERVICE_PIPE && p.h0()<.3).findFirst().orElseThrow();
        var right=Arrays.stream(ArchitecturalClutterAuthority.pieces(w,5,1)).filter(p->p.feature()==ArchitecturalClutterAuthority.Feature.SERVICE_PIPE && p.h0()<.3).findFirst().orElseThrow();
        require(left.x1()==right.x0() && left.h0()==right.h0() && left.z0()==right.z0(),"Adjacent pipe sections must connect, not scatter randomly");
        TileStructureAuthority.roof(w,4,4,TileStructureAuthority.Roof.GLASS,1.6);
        require(Arrays.stream(ArchitecturalClutterAuthority.pieces(w,4,4)).noneMatch(p->p.feature()==ArchitecturalClutterAuthority.Feature.DUCT || p.feature()==ArchitecturalClutterAuthority.Feature.SWAG || p.feature()==ArchitecturalClutterAuthority.Feature.HANGER),"Glass must not acquire opaque hanging clutter across its aperture");
        w.tiles[4][0]='.';w.dirtyVisionRevision++;
        require(Arrays.stream(ArchitecturalClutterAuthority.pieces(w,4,1)).noneMatch(p->p.feature()==ArchitecturalClutterAuthority.Feature.SERVICE_PIPE),"Removing a wall must invalidate attached geometry");
        Properties p=new Properties();Persistence.writeWorldState(w,p);
        World restored=hall(74193);restored.tiles[4][0]='.';
        Persistence.readWorldState(restored,p);
        require(restored.architecturalClutterVersion==ArchitecturalClutterAuthority.VERSION,"Clutter recipe must persist");
        for(int x=1;x<w.w-1;x++)for(int z=1;z<w.h-1;z++)require(Arrays.equals(ArchitecturalClutterAuthority.pieces(w,x,z),ArchitecturalClutterAuthority.pieces(restored,x,z)),"Save/load changed attached geometry");
        ArchitecturalClutterAuthority.synchronizeLights(w);long lights=w.lightSources.stream().filter(l->l.id.startsWith("CLUTTER-LIGHT-")).count();
        ArchitecturalClutterAuthority.synchronizeLights(w);require(w.lightSources.stream().filter(l->l.id.startsWith("CLUTTER-LIGHT-")).count()==lights && lights>0 && lights<=96,"Clutter lights must be bounded and deduplicated");
        ArchitecturalClutterAuthority.enable(w,false);
        require(ArchitecturalClutterAuthority.pieces(w,5,1).length==0 && w.lightSources.stream().noneMatch(l->l.id.startsWith("CLUTTER-LIGHT-")),"Disabling clutter must remove meshes and its derived lights");
        System.out.println("ArchitecturalClutterSmoke PASS features="+features.size()+" generations="+generations.size()+" pieces="+pieces+" connectedPipes=true depth=true clearAisles=true glassClear=true editInvalidation=true saveReplay=true lightBudget=true");
    }
    private static World hall(long seed) {
        World w=new World(seed,42,18);w.zoneType=ZoneType.NEUTRAL_CIVILIAN_FLOOR;
        for(int x=0;x<w.w;x++)for(int z=0;z<w.h;z++)w.tiles[x][z]=x==0 || z==0 || x==w.w-1 || z==w.h-1?'#':'.';
        TileStructureAuthority.open(w,new Rectangle(0,0,w.w,w.h));
        for(int x=1;x<w.w-1;x++)for(int z=1;z<8;z++)TileStructureAuthority.roof(w,x,z,TileStructureAuthority.Roof.COVERED,1.6);
        return w;
    }
    private static void require(boolean okay,String message){if(!okay)throw new AssertionError(message);}
}
