package mechanist;

public class LayerJ {
    public LayerJ() {}

    static void cycleJvmRuntimeProfile(GamePanel panel) {
        JvmRuntimeOptionsSubsystem.cycleJvmRuntimeProfile(panel);
    }

    static void cycleJvmGarbageCollector(GamePanel panel) {
        JvmRuntimeOptionsSubsystem.cycleJvmGarbageCollector(panel);
    }

    static void cycleJvmPipelineProfile(GamePanel panel) {
        JvmRuntimeOptionsSubsystem.cycleJvmPipelineProfile(panel);
    }
}
