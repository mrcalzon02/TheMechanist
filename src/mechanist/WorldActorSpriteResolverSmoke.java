package mechanist;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import javax.imageio.ImageIO;
import mechanist.assets.AssetMetadata;
import mechanist.assets.AssetType;

/** Real PNG intake, directional placement, typed metadata, and fallback smoke. */
final class WorldActorSpriteResolverSmoke {
    public static void main(String[] args) {
        Path temp = null;
        try {
            temp = Files.createTempDirectory("mechanist-world-sprite-smoke-");
            run(temp);
        } catch (IOException failure) {
            throw new AssertionError("World actor sprite smoke could not create fixtures", failure);
        } finally {
            if (temp != null) {
                try (var paths = Files.walk(temp)) {
                    paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                        try { Files.deleteIfExists(path); } catch (IOException ignored) { }
                    });
                } catch (IOException ignored) { }
            }
        }
    }

    private static void run(Path dir) throws IOException {
        Path png = dir.resolve("approved-body.png");
        Path opaque = dir.resolve("portrait.png");
        BufferedImage acceptable = new BufferedImage(64, 80, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = acceptable.createGraphics();
        try {
            g.setColor(new Color(88, 110, 126, 255));
            g.fillOval(17, 6, 29, 23);
            g.fillRoundRect(12, 26, 39, 36, 9, 9);
            g.fillRect(18, 61, 9, 17);
            g.fillRect(37, 61, 9, 17);
        } finally { g.dispose(); }
        ImageIO.write(acceptable, "png", png.toFile());
        BufferedImage forbidden = new BufferedImage(64, 80, BufferedImage.TYPE_INT_RGB);
        Graphics2D bad = forbidden.createGraphics();
        try { bad.setColor(Color.BLACK); bad.fillRect(0, 0, 64, 80); } finally { bad.dispose(); }
        ImageIO.write(forbidden, "png", opaque.toFile());
        require(WorldActorSpriteResolver.validBodyImage(acceptable), "Transparent body fixture must pass");
        require(!WorldActorSpriteResolver.validBodyImage(forbidden), "Rectangular opaque portrait must fail");
        Path manifest = dir.resolve("world_actor_sprites.tsv");
        Files.writeString(manifest,
                "entity_key\tview\tasset_id\twidth_m\theight_m\n"
                + "sump-rat-swarm\tfront\tACTR-R001\t0.46\t0.31\n"
                + "sump-rat-swarm\tback\tACTR-R002\t0.47\t0.32\n"
                + "sump-rat-swarm\tleft\tACTR-R003\t0.48\t0.33\n"
                + "humanoid-guard\tfront\tACTR-H001\t0.64\t1.18\n"
                + "humanoid\tfront\tACTR-H002\t0.60\t1.12\n"
                + "sump-rat-swarm\tfront\tACTR-R004\t0.46\t0.31\n"
                + "bad path\tfront\tACTR-Z001\t0.45\t0.50\n"
                + "sump-rat-swarm\tinverted\tACTR-Z002\t0.45\t0.50\n",
                StandardCharsets.UTF_8);
        Map<String, AssetMetadata> assets = new HashMap<>();
        Map<String, Path> paths = new HashMap<>();
        for (String id : new String[]{"ACTR-R001","ACTR-R002","ACTR-R003","ACTR-H001","ACTR-H002"}) {
            assets.put(id, new AssetMetadata(id, png.toString(), "World actor body", AssetType.ENTITY_SPRITE, "artist approved body sprite"));
            paths.put(id, png);
        }
        WorldActorSpriteResolver resolver = new WorldActorSpriteResolver(manifest,
                id -> Optional.ofNullable(assets.get(id)), id -> Optional.ofNullable(paths.get(id)));
        NpcEntity rat = new NpcEntity();
        rat.id = "rat-101"; rat.name = "Sump rat"; rat.creatureKind = "wild-animal";
        rat.animalProfileId = "sump-rat-swarm"; rat.x = 8; rat.y = 8;
        rat.facingDx = 1; rat.facingDy = 0;
        FirstPersonCamera camera = new FirstPersonCamera();
        camera.setPosition(10.5, .58, 8.5);
        require("front".equals(WorldActorSpriteResolver.viewFor(rat,camera)), "Facing camera uses front view");
        WorldActorSpriteResolver.Sprite front = resolver.resolve(rat, camera);
        require(front != null && front.image() != null && front.heightMeters() == .31, "Exact species front body used");
        require(resolver.resolve(rat, camera) == front, "Image is cached, not decoded every frame");
        camera.setPosition(6.5, .58, 8.5);
        require("back".equals(WorldActorSpriteResolver.viewFor(rat,camera)), "Rear camera uses back view");
        require(resolver.resolve(rat, camera).heightMeters() == .32, "Rear artwork has independent metadata");
        camera.setPosition(8.5, .58, 7.5);
        require("left".equals(WorldActorSpriteResolver.viewFor(rat,camera)), "Left-side view uses left art");
        require(resolver.resolve(rat, camera).heightMeters() == .33, "Left artwork selected");
        camera.setPosition(8.5, .58, 10.5);
        require("right".equals(WorldActorSpriteResolver.viewFor(rat,camera)), "Right-side detection");
        require(resolver.resolve(rat, camera) == front, "Missing right art uses accepted front, not a random other sprite");

        NpcEntity guard = new NpcEntity();
        guard.creatureKind = "humanoid"; guard.role = "Civic Warden guard";
        require("humanoid-guard".equals(WorldActorSpriteResolver.entityKey(guard)), "Guard role classification");
        require(resolver.resolve(guard, null).heightMeters() == 1.18, "Guard body uses its own scale");
        guard.role = "Civilian technician"; // generic fallback is intentionally exact
        require(resolver.resolve(guard, null).heightMeters() == 1.12, "Generic approved humanoid fallback");
        require(resolver.registeredViews() == 5 && resolver.rejectedRows() == 3,
                "Duplicate and malformed manifest rows must not silently shadow entries");

        // Mismatched typed metadata is never allowed to masquerade as a world body.
        assets.put("ACTR-R001", new AssetMetadata("ACTR-R001", opaque.toString(), "Portrait", AssetType.PORTRAIT, "face card"));
        resolver.clear();
        require(resolver.resolve(rat, camera) == null, "A portrait semantic type must be rejected when selected");
        assets.put("ACTR-R001", new AssetMetadata("ACTR-R001", opaque.toString(), "Wrong image", AssetType.ENTITY_SPRITE, "opaque frame"));
        paths.put("ACTR-R001", opaque);
        resolver.clear();
        require(resolver.resolve(rat, camera) == null, "An opaque square PNG must not enter the world-body renderer");
    }

    private static void require(boolean condition, String why) {
        if (!condition) throw new AssertionError(why);
    }
}
