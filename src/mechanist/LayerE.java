package mechanist;

/** @deprecated Use DoomQualityOfLifeOptionsRuntime. */
@Deprecated
public class LayerE {
    public LayerE() {}

    static void applyQoL(GamePanel panel, String message) { DoomQualityOfLifeOptionsRuntime.applyQoL(panel, message); }
    static void changeDoomFov(GamePanel panel, int delta) { DoomQualityOfLifeOptionsRuntime.changeDoomFov(panel, delta); }
    static void requestDoomModeToggle(GamePanel panel) { DoomQualityOfLifeOptionsRuntime.requestDoomModeToggle(panel); }
    static void cycleDoomFogMode(GamePanel panel) { DoomQualityOfLifeOptionsRuntime.cycleDoomFogMode(panel); }
}
