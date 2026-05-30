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

    static void changeJvmMemory(GamePanel panel, int deltaMb) {
        JvmRuntimeOptionsSubsystem.changeJvmMemory(panel, deltaMb);
    }

    static void toggleJvmStringDeduplication(GamePanel panel) {
        JvmRuntimeOptionsSubsystem.toggleJvmStringDeduplication(panel);
    }

    static void toggleJvmTransparentAcceleration(GamePanel panel) {
        JvmRuntimeOptionsSubsystem.toggleJvmTransparentAcceleration(panel);
    }
}
