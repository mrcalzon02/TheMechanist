package mechanist;

import mechanist.input.InputDevice;
import mechanist.input.InputToken;
import mechanist.input.KeyBindingManager;

import java.awt.event.KeyEvent;

final class Milestone02LivePanelPromptSmoke {
    public static void main(String[] args) {
        KeyBindingManager manager = KeyBindingManager.getInstance();
        manager.resetAllToDefaults();
        String inventory = ControlReferenceTextSubsystem.livePanelPrompt("Inventory", 0);
        requireContains(inventory, "Inventory", "inventory context");
        requireContains(inventory, "Keyboard: I", "inventory current binding");
        requireContains(inventory, "Keyboard: Escape", "inventory recovery binding");

        manager.rebind(InputDevice.KEYBOARD, "confirm", InputToken.keyboard(KeyEvent.VK_U, 0),
                KeyBindingManager.DuplicatePolicy.REJECT);
        String trade = ControlReferenceTextSubsystem.livePanelPrompt("Trade", 4);
        requireContains(trade, "Generic:", "controller prompt");
        requireContains(trade, "Keyboard: U", "rebound keyboard fallback");
        requireContains(trade, "Keyboard: Escape", "cancel fallback");

        String planning = ControlReferenceTextSubsystem.livePanelPrompt("Movement planning", 0);
        requireContains(planning, "Confirm / choose", "planning confirm action");
        for (String line : new String[]{inventory, trade, planning}) {
            if (PlayerFacingText.containsLikelyLeak(line)) throw new AssertionError("Live panel prompt leaked implementation text: " + line);
        }
        manager.resetAllToDefaults();
    }

    private static void requireContains(String text, String expected, String label) {
        if (text == null || !text.contains(expected)) throw new AssertionError("Expected " + label + " to contain '" + expected + "': " + text);
    }

    private Milestone02LivePanelPromptSmoke() {}
}
