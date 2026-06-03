package mechanist;

import java.awt.Rectangle;

/** Mutable run-state for the split World.generate pipeline phases. */
final class WorldGenerationPipelineRunState {
    int target;
    int attempts;
    Rectangle centralPlaza;
    int repairs;
    int widenedCorridors;
    int doorwayObjectRepairs;
}
