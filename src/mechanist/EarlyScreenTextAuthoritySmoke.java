package mechanist;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

/** Headless, pixel-level startup typography and rendering-state regression. */
final class EarlyScreenTextAuthoritySmoke {
    private EarlyScreenTextAuthoritySmoke() { }

    private static void check(boolean yes, String message) {
        if (!yes) throw new AssertionError(message);
    }

    public static void main(String[] args) {
        verifyLoadingPreset();
        BufferedImage image = new BufferedImage(1920, 1080, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        try {
            g.setFont(new Font("Dialog", Font.BOLD, 34));
            g.setColor(Color.ORANGE);
            Object antialiasBefore = g.getRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING);
            Object renderingBefore = g.getRenderingHint(RenderingHints.KEY_RENDERING);
            for (int width : new int[] {160, 320, 640, 800, 1280, 1920}) {
                Graphics2D scene = EarlyScreenTextAuthority.isolatedScene(g, width, 480);
                try {
                    Font title = EarlyScreenTextAuthority.fitFont(scene, g.getFont(),
                            "NEW WORLD INSERTION", width - 24, 14f);
                    scene.setFont(title);
                    String shown = EarlyScreenTextAuthority.fit(scene,
                            "World configuration and character creation options in the arcology", width - 24);
                    check(scene.getFontMetrics().stringWidth(shown) <= width - 24,
                            "Startup text overflow at " + width);
                    EarlyScreenTextAuthority.drawCentered(scene, shown, width / 2, 72, width - 24);
                    check(scene.getClipBounds().width <= width && scene.getClipBounds().height <= 480,
                            "Scene clipping escaped");
                } finally {
                    scene.dispose();
                }
                check(g.getRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING) == antialiasBefore
                                && g.getRenderingHint(RenderingHints.KEY_RENDERING) == renderingBefore,
                        "Restoring an initially unset rendering hint must not mutate original graphics");
                check(g.getColor().equals(Color.ORANGE), "Child paint changed parent color");
            }
            System.out.println("PASS: startup typography and isolated graphics contexts");
        } finally {
            g.dispose();
        }
    }

    private static void verifyLoadingPreset() {
        var metadata = mechanist.assets.AssetManager.metadata("UIX-0189").orElseThrow();
        check(metadata.pathOrUri().contains("Frameless_system_icons_r01c03"),
                "Loading preset must resolve to the unframed cog, not a toolbar refresh button");
        ImageCache media = new ImageCache();
        BufferedImage cog = media.getBootSpinnerFrame(0);
        check(cog != null && cog.getWidth() > 0, "Loading cog must ship in the client assets");
        check(media.getBootSpinnerFrame(Long.MIN_VALUE) == cog, "Cog fallback must remain stable");
        BufferedImage first = new BufferedImage(3, 3, BufferedImage.TYPE_INT_ARGB);
        BufferedImage second = new BufferedImage(4, 4, BufferedImage.TYPE_INT_ARGB);
        media.bootFrames.add(first);
        media.bootFrames.add(second);
        check(media.getBootSpinnerFrame(0) == first && media.getBootSpinnerFrame(1) == second
                        && media.getBootSpinnerFrame(2) == first && media.getBootSpinnerFrame(-1) == second,
                "Authored loading animation must retain frame order, priority and wrapping");
        GamePanel panel = new GamePanel();
        panel.timer.stop();
        try {
            BufferedImage image = new BufferedImage(640, 480, BufferedImage.TYPE_INT_ARGB);
            Graphics2D graphics = image.createGraphics();
            try {
                var transform = graphics.getTransform();
                var clip = graphics.getClip();
                new BootSurfacePainter().drawSpinner(graphics, panel, 640, 480, 0L);
                check((image.getRGB(262, 252) >>> 24) == 0 && (image.getRGB(377, 367) >>> 24) == 0,
                        "Loading cog must not draw a rotating square atlas border");
                new BootSurfacePainter().drawSpinner(graphics, panel, 640, 480, 575L);
                check(transform.equals(graphics.getTransform()), "Loading rotation leaked its transform");
                check(java.util.Objects.equals(clip, graphics.getClip()), "Loading medallion leaked its clip");
            } finally { graphics.dispose(); }
            for (int[] viewport : new int[][] {{640, 480}, {960, 600}, {1280, 800}, {1920, 1080}}) {
                int width = viewport[0], height = viewport[1];
                BufferedImage scene = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
                Graphics2D context = scene.createGraphics();
                try { new BootSurfacePainter().drawSpinner(context, panel, width, height, 0L); }
                finally { context.dispose(); }
                int firstPixel = height, lastPixel = -1;
                for (int y = 0; y < height; y++) for (int x = 0; x < width; x++) {
                    if ((scene.getRGB(x, y) >>> 24) != 0) {
                        firstPixel = Math.min(firstPixel, y); lastPixel = Math.max(lastPixel, y);
                    }
                }
                var title = panel.images.get("title_mechanist_rebase");
                var subtitle = panel.images.get("subtitle_rebase");
                check(title != null && subtitle != null, "Loading layout requires the authored title panels");
                int subtitleBottom = Math.max(42, height / 8)
                        + panel.mainMenuTitleDrawSize(title, width, height).height + 18
                        + panel.mainMenuSubtitleDrawSize(subtitle, width, height).height;
                check(firstPixel >= subtitleBottom + 10 && lastPixel >= firstPixel,
                        "Loading cog covered the second title panel at " + width + "x" + height);
                check(lastPixel < height - 100, "Loading cog covered the status text at " + width + "x" + height);
            }
        } finally { panel.shutdownRuntime(); }
    }
}
