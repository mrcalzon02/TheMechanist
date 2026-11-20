package mechanist;

import java.awt.image.BufferedImage;
import java.util.List;

/** Visible, floor-anchored representations shared by Doom drawing and inspection. */
final class FirstPersonBillboardAuthority {
    private FirstPersonBillboardAuthority() { }

    static void collect(GamePanel panel, List<RenderableSprite> out, boolean artwork) {
        out.clear();
        World world = panel.world;
        if (world == null) return;
        for (NpcEntity npc : world.npcs) {
            if (npc == null || npc.hp <= 0 || !panel.isVisible(npc.x, npc.y)) continue;
            out.add(new RenderableSprite(npc.x + .5, .02, npc.y + .5, npc.symbol,
                    label(npc.name, "Person"), 0xFFE2C46B, true,
                    artwork ? panel.images.getNpcPortraitFor(npc) : null));
        }
        for (MapObjectState object : world.mapObjects) {
            if (object == null || !panel.isVisible(object.x, object.y)) continue;
            if (object.glyph == '*' && !world.walkable(object.x, object.y)) continue;
            out.add(new RenderableSprite(object.x + .5, .02, object.y + .5,
                    object.glyph == 0 ? 'o' : object.glyph, label(object.label, "Object"),
                    0xFF8FD0A6, false, artwork ? panel.images.getMapObjectImage(object) : null));
        }
        for (BaseObject object : panel.baseObjects) {
            if (object == null || !panel.isVisible(object.x, object.y)) continue;
            out.add(new RenderableSprite(object.x + .5, .02, object.y + .5,
                    object.symbol == 0 ? 'o' : object.symbol, label(object.name, "Built object"),
                    0xFF8FD0A6, false, artwork ? panel.images.getBaseObjectImage(object) : null));
        }
        int range = panel.visionRange();
        for (int x = Math.max(0, panel.playerX-range); x <= Math.min(world.w-1, panel.playerX+range); x++) {
            for (int y = Math.max(0, panel.playerY-range); y <= Math.min(world.h-1, panel.playerY+range); y++) {
                if (!panel.isVisible(x,y) || represented(out,x,y)) continue;
                CompiledTileDescriptor descriptor = TileDataCompilationAuthority.resolve(world,x,y,world.tiles[x][y]);
                if (!descriptor.isFixture || descriptor.isDoor || descriptor.isWall) continue;
                BufferedImage image = null;
                if (artwork) {
                    if (descriptor.overlayAssetId != null) image = panel.images.getSemanticAssetImage(descriptor.overlayAssetId);
                    if (image == null) image = panel.images.getTile(descriptor.overlayArtKey,world.tiles[x][y]);
                }
                String name = descriptor.overlayArtKey == null || descriptor.overlayArtKey.contains("_or_")
                        ? "Fixture" : descriptor.overlayArtKey.replace('_',' ');
                out.add(new RenderableSprite(x+.5,.02,y+.5,world.tiles[x][y],name,0xFF8FD0A6,false,image));
            }
        }
    }

    private static boolean represented(List<RenderableSprite> sprites, int x, int y) {
        for (RenderableSprite sprite : sprites) {
            if (!sprite.entity() && (int)Math.floor(sprite.x()) == x && (int)Math.floor(sprite.z()) == y) return true;
        }
        return false;
    }

    private static String label(String value, String fallback) { return value == null || value.isBlank() ? fallback : value; }

    static double height(RenderableSprite sprite) {
        if (sprite.entity()) return 1.1;
        return switch (sprite.glyph()) {
            case '*', 'r', 'm', '?' -> .22;
            case 'c', 't' -> .45;
            default -> .72;
        };
    }

    static double width(RenderableSprite sprite) { return sprite.entity() ? .55 : .68; }

    static ScreenPoint project(FirstPersonCamera camera, double x, double y, double z, double fov, int w, int h) {
        double dx = x-camera.x(), dy = y-camera.y(), dz = z-camera.z();
        double sin = Math.sin(camera.yawRadians()), cos = Math.cos(camera.yawRadians());
        double right = dz*cos-dx*sin, forward = dx*cos+dz*sin;
        double sp = Math.sin(camera.pitchRadians()), cp = Math.cos(camera.pitchRadians());
        double up = dy*cp-forward*sp, depth = forward*cp+dy*sp;
        if (depth <= .05) return new ScreenPoint(0,0,false,depth);
        double focal = h*.5/Math.tan(fov*.5);
        int sx = (int)Math.round(w*.5+right*focal/depth);
        int sy = (int)Math.round(h*.5-up*focal/depth);
        return new ScreenPoint(sx,sy,sx>-w && sx<2*w && sy>-h && sy<2*h,depth);
    }

    static RaycastHit target(GamePanel panel, FirstPersonCamera camera, List<RenderableSprite> sprites,
                             ViewportRaycaster raycaster, double maxRange) {
        RaycastHit barrier = raycaster.cast(panel.world,camera.x(),camera.z(),camera.yawRadians(),maxRange,true);
        RaycastHit best = barrier;
        double nearest = barrier.hit() ? barrier.distance() : maxRange;
        double sin = Math.sin(camera.yawRadians()), cos = Math.cos(camera.yawRadians());
        double cp = Math.cos(camera.pitchRadians());
        if (cp <= .001) return barrier;
        for (RenderableSprite sprite : sprites) {
            int x = (int)Math.floor(sprite.x()), y = (int)Math.floor(sprite.z());
            if (!panel.isVisible(x,y)) continue;
            double dx = sprite.x()-camera.x(), dz = sprite.z()-camera.z();
            double forward = dx*cos+dz*sin, side = dz*cos-dx*sin;
            if (forward < .05 || forward > nearest || Math.abs(side) > width(sprite)*.5) continue;
            double hitY = camera.y()+Math.tan(camera.pitchRadians())*forward;
            if (hitY < sprite.y() || hitY > sprite.y()+height(sprite)) continue;
            nearest = forward;
            best = new RaycastHit(true,sprite.entity()?RaycastHitKind.ENTITY:RaycastHitKind.OBJECT,
                    x,y,panel.world.tiles[x][y],forward,sprite.x(),sprite.z(),-cos,-sin,0,sprite.label());
        }
        return best;
    }
}
