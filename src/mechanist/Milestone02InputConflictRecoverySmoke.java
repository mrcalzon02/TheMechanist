package mechanist;

import mechanist.input.InputDevice;
import mechanist.input.InputToken;
import mechanist.input.KeyBindingManager;

import java.awt.event.KeyEvent;

/** Smoke for live input conflict review and recovery-safe keybinding manager behavior. */
final class Milestone02InputConflictRecoverySmoke {
    public static void main(String[] args) {
        KeyBindingManager manager = KeyBindingManager.getInstance();
        manager.resetAllToDefaults();

        InputToken defaultConfirm = manager.getBinding(InputDevice.KEYBOARD, "confirm")
                .orElseThrow(() -> new AssertionError("confirm binding missing"))
                .token();
        InputToken defaultCancel = manager.getBinding(InputDevice.KEYBOARD, "cancel")
                .orElseThrow(() -> new AssertionError("cancel binding missing"))
                .token();

        KeyBindingManager.RebindResult rejected = manager.rebind(
                InputDevice.KEYBOARD,
                "confirm",
                defaultCancel,
                KeyBindingManager.DuplicatePolicy.REJECT
        );
        require(!rejected.accepted(), "duplicate reject should refuse the conflict");
        requireContains(rejected.message(), "Already bound to Cancel / Back", "duplicate owner");
        requireContains(rejected.message(), "Conflict reviewed", "conflict classification");
        requireContains(rejected.message(), "recovery-critical", "required-action conflict marker");
        require(manager.getBinding(InputDevice.KEYBOARD, "confirm").orElseThrow().token().equals(defaultConfirm), "reject should not change confirm");

        KeyBindingManager.RebindResult swapped = manager.rebind(
                InputDevice.KEYBOARD,
                "confirm",
                defaultCancel,
                KeyBindingManager.DuplicatePolicy.SWAP
        );
        require(swapped.accepted(), "duplicate swap should be accepted");
        requireContains(swapped.message(), "duplicate command swapped", "swap message");
        require(manager.getBinding(InputDevice.KEYBOARD, "confirm").orElseThrow().token().equals(defaultCancel), "confirm should receive cancel token after swap");
        require(manager.requiredCommandBound(InputDevice.KEYBOARD, "confirm"), "confirm should remain bound after swap");
        require(manager.requiredCommandBound(InputDevice.KEYBOARD, "cancel"), "cancel should remain bound after swap");

        KeyBindingManager.RebindResult restored = manager.restoreLastGoodProfile(InputDevice.KEYBOARD);
        require(restored.accepted(), "restore last good should be accepted");
        require(manager.getBinding(InputDevice.KEYBOARD, "confirm").orElseThrow().token().equals(defaultConfirm), "restore should recover default confirm token");

        InputToken customInventory = InputToken.keyboard(KeyEvent.VK_U, 0);
        KeyBindingManager.RebindResult changed = manager.rebind(
                InputDevice.KEYBOARD,
                "inventory",
                customInventory,
                KeyBindingManager.DuplicatePolicy.REJECT
        );
        require(changed.accepted(), "non-conflicting optional rebind should work");
        require(manager.getBinding(InputDevice.KEYBOARD, "inventory").orElseThrow().token().equals(customInventory), "inventory should receive custom token");
        KeyBindingManager.RebindResult reset = manager.resetDeviceToDefaults(InputDevice.KEYBOARD);
        require(reset.accepted(), "reset tab should be accepted");
        require(!manager.getBinding(InputDevice.KEYBOARD, "inventory").orElseThrow().token().equals(customInventory), "reset should remove custom inventory token");

        manager.resetAllToDefaults();
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static void requireContains(String text, String expected, String label) {
        if (text == null || !text.contains(expected)) {
            throw new AssertionError("Expected " + label + " to contain '" + expected + "': " + text);
        }
    }

    private Milestone02InputConflictRecoverySmoke() { }
}
