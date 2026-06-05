package mechanist;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Random;

/** Smoke coverage for gradual lighting falloff and road-section street lights. */
public final class LightingFalloffStreetLightSmoke {
    private LightingFalloffStreetLightSmoke() { }

    public static void main(String[] args) {
        LightFalloffProfile street = LightFalloffProfile.forProfile("streetlight civic sanctioned");
        LightFalloffProfile flashlight = LightFalloffProfile.forProfile("Flashlight");
        require(street.factor(0.78) > flashlight.factor(0.78), "street light should retain a broader far band");
        require(street.factor(0.95) > 0.10, "street light tail should be gradual");

        World world = new World(12345L, 24, 18);
        for (int x = 0; x < world.w; x++) {
            for (int y = 0; y < world.h; y++) world.tiles[x][y] = '#';
        }
        for (int x = 2; x < world.w - 2; x++) {
            world.tiles[x][8] = RoadGridIntegrationAuthority.ROAD_LANE;
            world.tiles[x][7] = RoadGridIntegrationAuthority.SIDEWALK;
            world.tiles[x][9] = RoadGridIntegrationAuthority.SIDEWALK;
        }
        LightingNoiseMetadataApi.ZoneLightingProfile profile = LightingNoiseMetadataApi.profileFor(ZoneType.NEUTRAL_CIVILIAN_FLOOR, false);
        int made = LightingNoiseMetadataApi.seedStreetLights(world, new Random(77L), profile, new ArrayList<Point>());
        require(made > 0, "expected street lights along road sidewalks");
        require(world.lightSources.stream().anyMatch(l -> l != null && l.profile != null && l.profile.contains("streetlight")), "street light profile not retained on source");

        System.out.println("LightingFalloffStreetLightSmoke OK streetLights=" + made);
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }
}
