package mechanist;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.function.Consumer;

/** Shared final scene exposure. Black maps to #001a33; low-light texture differences survive. */
final class ViewportDarknessAuthority {
    static final int DARKEST=0xFF001A33;
    static int expose(int color) {
        return (color&0xFFFF0000) | ((26+((color>>>8)&255)*229/255)<<8) | (51+(color&255)*204/255);
    }
    static void expose(int[] pixels) {for(int i=0;i<pixels.length;i++)pixels[i]=expose(pixels[i]);}
    /** One reusable software viewport per panel. Never read back an accelerated surface's raster. */
    static final class SceneBuffer {
        private BufferedImage image;

        void paint(Graphics2D target, Rectangle scene, Consumer<Graphics2D> painter) {
            if (scene.width <= 0 || scene.height <= 0) return;
            if (image == null || image.getWidth() != scene.width || image.getHeight() != scene.height) {
                if (image != null) image.flush();
                image = new BufferedImage(scene.width, scene.height, BufferedImage.TYPE_INT_RGB);
            }
            Graphics2D layer = image.createGraphics();
            try {
                layer.setColor(new Color(10, 11, 10));
                layer.fillRect(0, 0, scene.width, scene.height);
                layer.setRenderingHints(target.getRenderingHints());
                layer.setFont(target.getFont());
                layer.setStroke(target.getStroke());
                layer.translate(-scene.x, -scene.y);
                layer.clip(scene);
                painter.accept(layer);
            } finally {
                layer.dispose();
            }
            expose(((DataBufferInt) image.getRaster().getDataBuffer()).getData());
            // Ordinary image blits work on XRender, D3D and software targets.
            target.drawImage(image, scene.x, scene.y, null);
        }
    }
}
