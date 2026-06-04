package mechanist;

/** @deprecated Use JvmRuntimeOptionsBridge. */
@Deprecated
public class LayerJ {
    public LayerJ() {}

    static void cycleJvmRuntimeProfile(GamePanel panel) { JvmRuntimeOptionsBridge.cycleJvmRuntimeProfile(panel); }
    static void cycleJvmGarbageCollector(GamePanel panel) { JvmRuntimeOptionsBridge.cycleJvmGarbageCollector(panel); }
    static void cycleJvmPipelineProfile(GamePanel panel) { JvmRuntimeOptionsBridge.cycleJvmPipelineProfile(panel); }
    static void changeJvmMemory(GamePanel panel, int deltaMb) { JvmRuntimeOptionsBridge.changeJvmMemory(panel, deltaMb); }
    static void toggleJvmStringDeduplication(GamePanel panel) { JvmRuntimeOptionsBridge.toggleJvmStringDeduplication(panel); }
    static void toggleJvmTransparentAcceleration(GamePanel panel) { JvmRuntimeOptionsBridge.toggleJvmTransparentAcceleration(panel); }
    static void toggleJvmNoAa(GamePanel panel) { JvmRuntimeOptionsBridge.toggleJvmNoAa(panel); }
    static void acceptJvmSettingsAndRestart(GamePanel panel) { JvmRuntimeOptionsBridge.acceptJvmSettingsAndRestart(panel); }
    static boolean isWindowsHost() { return JvmRuntimeOptionsBridge.isWindowsHost(); }
}
