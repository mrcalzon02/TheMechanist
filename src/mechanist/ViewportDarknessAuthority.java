package mechanist;

import java.awt.*;
import java.awt.image.*;

/** Shared final scene exposure. Black maps to #001a33; low-light texture differences survive. */
final class ViewportDarknessAuthority {
    static final int DARKEST=0xFF001A33;
    static int expose(int color) {
        return (color&0xFFFF0000) | ((26+((color>>>8)&255)*229/255)<<8) | (51+(color&255)*204/255);
    }
    static void expose(int[] pixels) {for(int i=0;i<pixels.length;i++)pixels[i]=expose(pixels[i]);}
    private static final Composite FLOOR=new Composite() {
        public CompositeContext createContext(ColorModel src,ColorModel dst,RenderingHints hints) {
            return new CompositeContext() {
                public void dispose() { }
                public void compose(Raster source,Raster destination,WritableRaster output) {
                    Object data=null;
                    for(int y=0;y<output.getHeight();y++)for(int x=0;x<output.getWidth();x++) {
                        data=destination.getDataElements(destination.getMinX()+x,destination.getMinY()+y,data);
                        int color=expose(dst.getRGB(data));
                        output.setDataElements(output.getMinX()+x,output.getMinY()+y,dst.getDataElements(color,null));
                    }
                }
            };
        }
    };
    static void apply(Graphics2D g,Rectangle scene) {
        Graphics2D layer=(Graphics2D)g.create();
        try{layer.setComposite(FLOOR);layer.setColor(Color.WHITE);layer.fill(scene);}finally{layer.dispose();}
    }
}
