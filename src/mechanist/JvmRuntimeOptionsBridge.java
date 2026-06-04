package mechanist;

final class JvmRuntimeOptionsBridge {
    private JvmRuntimeOptionsBridge() {}

    static void cycleJvmRuntimeProfile(GamePanel panel) { JvmRuntimeOptionsSubsystem.cycleJvmRuntimeProfile(panel); }
    static void cycleJvmGarbageCollector(GamePanel panel) { JvmRuntimeOptionsSubsystem.cycleJvmGarbageCollector(panel); }
    static void cycleJvmPipelineProfile(GamePanel panel) { JvmRuntimeOptionsSubsystem.cycleJvmPipelineProfile(panel); }
    static void changeJvmMemory(GamePanel panel, int deltaMb) { JvmRuntimeOptionsSubsystem.changeJvmMemory(panel, deltaMb); }
    static void toggleJvmStringDeduplication(GamePanel panel) { JvmRuntimeOptionsSubsystem.toggleJvmStringDeduplication(panel); }
    static void toggleJvmTransparentAcceleration(GamePanel panel) { JvmRuntimeOptionsSubsystem.toggleJvmTransparentAcceleration(panel); }
    static void toggleJvmNoAa(GamePanel panel) { JvmRuntimeOptionsSubsystem.toggleJvmNoAa(panel); }
    static void acceptJvmSettingsAndRestart(GamePanel panel) { JvmRuntimeOptionsSubsystem.acceptJvmSettingsAndRestart(panel); }
    static boolean isWindowsHost() { return JvmRuntimeOptionsSubsystem.isWindowsHost(); }
}
