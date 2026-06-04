package mechanist;

/** @deprecated Use DisplayPerformanceOptionsRuntime. */
@Deprecated
public class LayerC {
    public LayerC() {}

    static void cycleLightingFx(GamePanel panel) { DisplayPerformanceOptionsRuntime.cycleLightingFx(panel); }
    static void toggleReducedMotion(GamePanel panel) { DisplayPerformanceOptionsRuntime.toggleReducedMotion(panel); }
    static void toggleFrameLimiter(GamePanel panel) { DisplayPerformanceOptionsRuntime.toggleFrameLimiter(panel); }
    static void setRenderQualityIndex(GamePanel panel, int idx) { DisplayPerformanceOptionsRuntime.setRenderQualityIndex(panel, idx); }
    static void toggleRenderStressTest(GamePanel panel) { DisplayPerformanceOptionsRuntime.toggleRenderStressTest(panel); }
}
