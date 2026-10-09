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

        InputRegistry registry = new InputRegistry();
        registry.setAnalog(InputSource.GAMEPAD, InputAction.MOVE_RIGHT, 0.75f);
        require(registry.consumePressed(InputAction.MOVE_RIGHT, InputSource.GAMEPAD),
                "first analog movement press should produce an edge");
        require(!registry.consumePressed(InputAction.MOVE_RIGHT, InputSource.GAMEPAD),
                "held analog movement must not repeat an edge");
        registry.setAnalog(InputSource.GAMEPAD, InputAction.MOVE_RIGHT, 0.0f);
        registry.setAnalog(InputSource.GAMEPAD, InputAction.MOVE_RIGHT, 0.8f);
        require(registry.consumePressed(InputAction.MOVE_RIGHT, InputSource.GAMEPAD),
                "analog release and repress between reads must rearm the edge");

        registry.setDigital(InputSource.GAMEPAD, InputAction.MOVE_RIGHT, true);
        registry.setAnalog(InputSource.GAMEPAD, InputAction.MOVE_RIGHT, 0.0f);
        require(!registry.consumePressed(InputAction.MOVE_RIGHT, InputSource.GAMEPAD),
                "releasing analog must not rearm while digital remains held");
        registry.setDigital(InputSource.GAMEPAD, InputAction.MOVE_RIGHT, false);
        registry.setAnalog(InputSource.GAMEPAD, InputAction.MOVE_RIGHT, 1.0f);
        require(registry.consumePressed(InputAction.MOVE_RIGHT, InputSource.GAMEPAD),
                "releasing the last active channel must rearm a new press");

        registry.setAnalog(InputSource.GAMEPAD, InputAction.MOVE_RIGHT, Float.NaN);
        require(!registry.isActiveFromSource(InputAction.MOVE_RIGHT, InputSource.GAMEPAD),
                "invalid axis values must not remain active");
        registry.setAnalog(InputSource.GAMEPAD, InputAction.MOVE_RIGHT, Float.POSITIVE_INFINITY);
        require(!registry.isActiveFromSource(InputAction.MOVE_RIGHT, InputSource.GAMEPAD),
                "infinite axis values must be neutralized");
        registry.setDigital(InputSource.KEYBOARD, InputAction.CONFIRM, true);
        require(registry.consumePressed(InputAction.CONFIRM, InputSource.KEYBOARD),
                "keyboard presses must remain independent of gamepad input");
        registry.clearSource(InputSource.GAMEPAD);
        require(registry.isActiveFromSource(InputAction.CONFIRM, InputSource.KEYBOARD),
                "gamepad disconnection must not clear keyboard input");
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
