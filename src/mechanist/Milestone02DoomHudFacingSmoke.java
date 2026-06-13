package mechanist;

import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.image.BufferedImage;

final class Milestone02DoomHudFacingSmoke {
    public static void main(String[] args) {
        verifyDoomUnlockRouting();
        verifyCardinalCameraDrift();
        verifySharedTwoDimensionalZoom();
        verifyCenterRayTargeting();
        verifyIdleTileCentering();
        verifyFirstPersonMovementModes();
        verifyPassiveTurnCountdown();
        verifyHudStateAndPaint();
        verifyNpcFacingPersistence();
        verifyTriangleDirections();
    }

    private static void verifyDoomUnlockRouting() {
        GameOptions options = new GameOptions();
        FirstPersonRenderViewport viewport = new FirstPersonRenderViewport();
        require(!viewport.isUnlocked(options), "Doom viewport must remain locked by default.");
        options.doomModeEnabled = true;
        require(viewport.isUnlocked(options), "Doom viewport must unlock from the saved control-mode option.");
    }

    private static void verifyCardinalCameraDrift() {
        FirstPersonCamera camera = new FirstPersonCamera();
        camera.addYawPitch(Math.toRadians(31.0), Math.toRadians(12.0));
        require(Math.abs(camera.nearestCardinalYawRadians()) < 0.00001,
                "A modest eastward offset must settle toward the east cardinal center.");
        double previousYaw = Math.abs(camera.yawRadians());
        double previousPitch = Math.abs(camera.pitchRadians());
        camera.driftTowardCardinal(0.10);
        require(Math.abs(camera.yawRadians()) < previousYaw, "Cardinal drift must reduce yaw error gradually.");
        require(Math.abs(camera.pitchRadians()) < previousPitch, "Cardinal drift must ease the view back toward level.");
        for (int i = 0; i < 240; i++) camera.driftTowardCardinal(0.10);
        require(Math.abs(camera.yawRadians()) < Math.toRadians(0.09), "Cardinal drift must eventually center yaw.");
        require(Math.abs(camera.pitchRadians()) < Math.toRadians(0.09), "Cardinal drift must eventually center pitch.");

        FirstPersonCamera north = new FirstPersonCamera();
        north.addYawPitch(Math.toRadians(73.0), 0.0);
        require(Math.abs(north.nearestCardinalYawRadians() - Math.PI / 2.0) < 0.00001,
                "A view nearest north must settle toward north rather than east.");
    }

    private static void verifySharedTwoDimensionalZoom() {
        GameOptions options = new GameOptions();
        options.worldZoomIndex = 0;
        require(MapViewportOptionsSubsystem.scaledTileSize(20, options, 6, 56) == 14,
                "Far zoom must shrink the standard game tile size.");
        options.worldZoomIndex = 2;
        require(MapViewportOptionsSubsystem.scaledTileSize(20, options, 6, 56) == 20,
                "Normal zoom must preserve the adaptive standard game tile size.");
        options.worldZoomIndex = 5;
        require(MapViewportOptionsSubsystem.scaledTileSize(20, options, 6, 56) == 40,
                "Maximum zoom must enlarge the standard game tile size.");
        require(MapViewportOptionsSubsystem.scaledTileSize(24, options, 8, 64) == 48,
                "Zone Auditor zoom must consume the same saved zoom percentage.");
    }

    private static void verifyCenterRayTargeting() {
        java.awt.Point center = DoomRayTarget.reticleCenter(1280, 720);
        require(center.x == 640 && center.y == 360, "Doom targeting reticle must use the exact viewport center.");

        RaycastHit nearEntity = new RaycastHit(true, RaycastHitKind.ENTITY, 4, 5, '.', 1.25, 4.5, 5.5, 0, 0, 0, "guard");
        DoomRayTarget entity = DoomRayTarget.from(nearEntity);
        require(entity.interactable(), "A nearby entity must be reachable by center-reticle interaction.");
        require(entity.weaponTarget(), "An entity must be a valid center-reticle weapon target.");
        require(entity.reticleLabel().contains("F AIM"), "Weapon targets must advertise the aim command.");

        RaycastHit farObject = new RaycastHit(true, RaycastHitKind.OBJECT, 8, 5, '.', 4.0, 8.5, 5.5, 0, 0, 0, "terminal");
        DoomRayTarget object = DoomRayTarget.from(farObject);
        require(!object.interactable(), "A distant object must not bypass interaction range.");
        require(!object.weaponTarget(), "Ordinary objects must not masquerade as entity weapon targets.");
        require(object.reticleLabel().contains("L LOOK"), "Distant visible targets must remain available to Look.");

        DoomRayTarget clear = DoomRayTarget.from(RaycastHit.none());
        require(!clear.hasTarget() && clear.reticleLabel().contains("E USE"),
                "A clear sightline must retain centered command guidance without inventing a hit.");
    }

    private static void verifyIdleTileCentering() {
        ContinuousGridWorld world = new ContinuousGridWorld(6, 6);
        ContinuousGridPlayer player = new ContinuousGridPlayer(2.14, 2.82);
        double before = Math.hypot(player.posX() - 2.5, player.posY() - 2.5);
        for (int i = 0; i < 30; i++) player.update(world, 0.05);
        double after = Math.hypot(player.posX() - 2.5, player.posY() - 2.5);
        require(after < before, "No-input Doom movement must slide the character toward the current tile center.");
        for (int i = 0; i < 80; i++) player.update(world, 0.05);
        require(Math.abs(player.posX() - 2.5) < 0.001 && Math.abs(player.posY() - 2.5) < 0.001,
                "Idle tile centering must settle exactly on the logical tile center.");
    }

    private static void verifyFirstPersonMovementModes() {
        require(FirstPersonRenderViewport.heldMovementMode(GamePanel.MOTION_WALK, true, false) == GamePanel.MOTION_SPRINT,
                "Holding Shift must temporarily select Sprint.");
        require(FirstPersonRenderViewport.heldMovementMode(GamePanel.MOTION_WALK, false, true) == GamePanel.MOTION_SNEAK,
                "Holding crouch must temporarily select Sneak.");
        require(FirstPersonRenderViewport.heldMovementMode(GamePanel.MOTION_RUN, false, false) == GamePanel.MOTION_RUN,
                "Releasing movement modifiers must restore the prior movement mode.");
        MotionTuning sneak = FirstPersonRenderViewport.motionTuningForMode(GamePanel.MOTION_SNEAK);
        MotionTuning walk = FirstPersonRenderViewport.motionTuningForMode(GamePanel.MOTION_WALK);
        MotionTuning sprint = FirstPersonRenderViewport.motionTuningForMode(GamePanel.MOTION_SPRINT);
        require(sneak.maxSpeed() < walk.maxSpeed() && walk.maxSpeed() < sprint.maxSpeed(),
                "Sneak, walk, and sprint continuous speeds must remain naturally ordered.");
    }

    private static void verifyPassiveTurnCountdown() {
        require(GamePanel.passiveTurnCountdownMillis(false, true, 1000L, 1200L) == -1L,
                "Turn-based mode must report no automatic-turn countdown.");
        require(GamePanel.passiveTurnCountdownMillis(true, true, 1000L, 1800L) == 1800L,
                "Passive countdown must report the exact remaining turn interval.");
        require(GamePanel.passiveTurnCountdownMillis(true, true, 0L, 1800L) == GamePanel.PASSIVE_TURN_INTERVAL_MILLIS,
                "A newly initialized passive clock must begin with a full countdown.");
    }

    private static void verifyHudStateAndPaint() {
        PlayerState state = new PlayerState(275, 400, 63, 42, 27,
                "Chain sword", "Stub pistol", 0, 2, null);
        require(Math.abs(state.healthRatio() - 0.6875) < 0.00001, "HUD health ratio must reflect body endurance state.");
        require(state.food() == 63 && state.water() == 42 && 100 - state.fatigue() == 73,
                "HUD survival and energy values must remain exact.");
        require(state.activeWeaponHandIndex() == 0, "HUD must retain the active weapon hand.");

        BufferedImage image = new BufferedImage(1280, 720, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        try {
            new GameHudOverlay().renderHUD(g, state, image.getWidth(), image.getHeight());
        } finally {
            g.dispose();
        }
        require((image.getRGB(640, 700) >>> 24) != 0, "HUD paint must reach the bottom control surface.");
    }

    private static void verifyNpcFacingPersistence() {
        NpcEntity npc = new NpcEntity();
        npc.x = 5;
        npc.y = 5;
        npc.moveTo(5, 4);
        require(npc.facingDx == 0 && npc.facingDy == -1, "NPC northward movement must persist north facing.");
        npc.moveTo(6, 4);
        require(npc.facingDx == 1 && npc.facingDy == 0, "NPC eastward movement must persist east facing.");
    }

    private static void verifyTriangleDirections() {
        assertTip(0, -1, 110, 100, "north");
        assertTip(1, 0, 120, 110, "east");
        assertTip(0, 1, 110, 120, "south");
        assertTip(-1, 0, 100, 110, "west");
        require(FacingIndicatorAuthority.tileTriangle(100, 100, 20, 2, 0, 0) == null,
                "Stationary entities must not invent a facing marker.");
    }

    private static void assertTip(int dx, int dy, int expectedX, int expectedY, String label) {
        Polygon p = FacingIndicatorAuthority.tileTriangle(100, 100, 20, 2, dx, dy);
        require(p != null && p.npoints == 3, label + " marker must be triangular.");
        require(p.xpoints[2] == expectedX && p.ypoints[2] == expectedY,
                label + " marker tip must point to the correct tile edge.");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private Milestone02DoomHudFacingSmoke() {}
}
