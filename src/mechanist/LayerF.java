package mechanist;

/** @deprecated Use AccessibilityVisualOptionsRuntime. */
@Deprecated
public class LayerF {
    public LayerF() {}

    static void togglePerformanceDiagnostics(GamePanel panel) { AccessibilityVisualOptionsRuntime.togglePerformanceDiagnostics(panel); }
    static void cycleCvdMode(GamePanel panel) { AccessibilityVisualOptionsRuntime.cycleCvdMode(panel); }
    static void toggleHighContrastText(GamePanel panel) { AccessibilityVisualOptionsRuntime.toggleHighContrastText(panel); }
    static void toggleInstantDialogueText(GamePanel panel) { AccessibilityVisualOptionsRuntime.toggleInstantDialogueText(panel); }
    static void adjustScreenShake(GamePanel panel, int delta) { AccessibilityVisualOptionsRuntime.adjustScreenShake(panel, delta); }
    static void pushCurrentScreenNarration(GamePanel panel) { AccessibilityVisualOptionsRuntime.pushCurrentScreenNarration(panel); }
    static void cycleColorTarget(GamePanel panel) { AccessibilityVisualOptionsRuntime.cycleColorTarget(panel); }
    static void cycleColorPreset(GamePanel panel) { AccessibilityVisualOptionsRuntime.cycleColorPreset(panel); }
    static void adjustSelectedColor(GamePanel panel, int delta) { AccessibilityVisualOptionsRuntime.adjustSelectedColor(panel, delta); }
    static java.awt.Color optionColor(GamePanel panel, int key) { return AccessibilityVisualOptionsRuntime.optionColor(panel, key); }
}
