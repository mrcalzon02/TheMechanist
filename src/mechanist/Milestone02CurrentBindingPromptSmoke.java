package mechanist;

import mechanist.input.InputDevice;
import mechanist.input.InputToken;
import mechanist.input.KeyBindingManager;

import java.awt.event.KeyEvent;

/** Smoke for prompts reflecting the current remappable binding profile. */
final class Milestone02CurrentBindingPromptSmoke {
    public static void main(String[] args) {
        KeyBindingManager manager = KeyBindingManager.getInstance();
        manager.resetAllToDefaults();

        requireContains(ControlReferenceTextSubsystem.keyboardPromptFor(InputAction.INVENTORY), "Keyboard: I", "default inventory prompt");
        manager.rebind(InputDevice.KEYBOARD, "inventory", InputToken.keyboard(KeyEvent.VK_U, 0), KeyBindingManager.DuplicatePolicy.REJECT);
        requireContains(ControlReferenceTextSubsystem.keyboardPromptFor(InputAction.INVENTORY), "Keyboard: U", "rebound inventory prompt");
        String inventoryContext = ControlReferenceTextSubsystem.contextPromptLine(
                "Inventory", 0, InputAction.INVENTORY, InputAction.CANCEL, "Review carried items and return to the game."
        );
        requireContains(inventoryContext, "Keyboard: U", "context prompt should use rebound inventory key");
        rejectContains(inventoryContext, "Keyboard: I]", "context prompt should not keep stale inventory key");

        requireContains(ControlReferenceTextSubsystem.genericPromptFor(InputAction.INVENTORY), "Generic: Button 3", "default generic controller prompt");
        manager.rebind(InputDevice.GENERIC_CONTROLLER, "inventory", InputToken.controllerButton(8), KeyBindingManager.DuplicatePolicy.REJECT);
        requireContains(ControlReferenceTextSubsystem.genericPromptFor(InputAction.INVENTORY), "Generic: Button 8", "rebound generic controller prompt");
        String controllerContext = ControlReferenceTextSubsystem.contextPromptLine(
                "Inventory", 4, InputAction.INVENTORY, InputAction.CANCEL, "Review carried items and return to the game."
        );
        requireContains(controllerContext, "Generic: Button 8", "controller context prompt should use rebound button");
        requireContains(controllerContext, "Keyboard: U", "controller context prompt should keep keyboard fallback current");

        for (String line : ControlReferenceTextSubsystem.contextPromptLines(4, ControlReferenceTextSubsystem.defaultContextPrompts())) {
            if (PlayerFacingText.containsLikelyLeak(line)) throw new AssertionError("Prompt leak after rebind: " + line);
        }
        manager.resetAllToDefaults();
    }

    private static void requireContains(String text, String expected, String label) {
        if (text == null || !text.contains(expected)) {
            throw new AssertionError("Expected " + label + " to contain '" + expected + "': " + text);
        }
    }

    private static void rejectContains(String text, String forbidden, String label) {
        if (text != null && text.contains(forbidden)) throw new AssertionError(label + ": " + text);
    }

    private Milestone02CurrentBindingPromptSmoke() { }
}
