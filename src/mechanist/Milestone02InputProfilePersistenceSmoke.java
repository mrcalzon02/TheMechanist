package mechanist;

import mechanist.input.InputDevice;
import mechanist.input.InputToken;
import mechanist.input.KeyBindingManager;

import java.awt.event.KeyEvent;
import java.util.Properties;

/** Smoke for saved keybinding profile export, import, validation, and prompt refresh. */
final class Milestone02InputProfilePersistenceSmoke {
    public static void main(String[] args) {
        KeyBindingManager manager = KeyBindingManager.getInstance();
        manager.resetAllToDefaults();

        InputToken customKeyboardInventory = InputToken.keyboard(KeyEvent.VK_U, 0);
        InputToken customControllerInventory = InputToken.controllerButton(8);
        require(manager.rebind(InputDevice.KEYBOARD, "inventory", customKeyboardInventory, KeyBindingManager.DuplicatePolicy.REJECT).accepted(), "keyboard inventory rebind");
        require(manager.rebind(InputDevice.GENERIC_CONTROLLER, "inventory", customControllerInventory, KeyBindingManager.DuplicatePolicy.REJECT).accepted(), "controller inventory rebind");

        Properties saved = manager.exportProfileProperties();
        require(saved.getProperty("binding.KEYBOARD.inventory").contains("KEYBOARD:KEY_85:U"), "keyboard inventory should be serialized");
        require(saved.getProperty("binding.GENERIC_CONTROLLER.inventory").contains("GENERIC_CONTROLLER:PAD_BUTTON_8:Button 8"), "controller inventory should be serialized");

        manager.resetAllToDefaults();
        requireContains(ControlReferenceTextSubsystem.keyboardPromptFor(InputAction.INVENTORY), "Keyboard: I", "default keyboard prompt after reset");
        requireContains(ControlReferenceTextSubsystem.genericPromptFor(InputAction.INVENTORY), "Generic: Button 3", "default controller prompt after reset");

        KeyBindingManager.RebindResult imported = manager.importProfileProperties(saved);
        require(imported.accepted(), "saved profile import should be accepted");
        requireContains(ControlReferenceTextSubsystem.keyboardPromptFor(InputAction.INVENTORY), "Keyboard: U", "keyboard prompt after import");
        requireContains(ControlReferenceTextSubsystem.genericPromptFor(InputAction.INVENTORY), "Generic: Button 8", "controller prompt after import");

        Properties malformed = manager.exportProfileProperties();
        malformed.setProperty("binding.KEYBOARD.confirm", "not-a-token");
        KeyBindingManager.RebindResult malformedResult = manager.importProfileProperties(malformed);
        require(!malformedResult.accepted(), "malformed saved token should be rejected");
        requireContains(malformedResult.message(), "malformed", "malformed rejection message");
        requireContains(ControlReferenceTextSubsystem.keyboardPromptFor(InputAction.INVENTORY), "Keyboard: U", "malformed import should preserve current profile");

        Properties wrongDevice = manager.exportProfileProperties();
        wrongDevice.setProperty("binding.KEYBOARD.cancel", InputToken.controllerButton(9).storageValue());
        KeyBindingManager.RebindResult wrongDeviceResult = manager.importProfileProperties(wrongDevice);
        require(!wrongDeviceResult.accepted(), "wrong-device saved token should be rejected");
        requireContains(wrongDeviceResult.message(), "not Keyboard", "wrong-device rejection message");
        require(manager.requiredCommandBound(InputDevice.KEYBOARD, "cancel"), "cancel should remain bound after rejected import");

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

    private Milestone02InputProfilePersistenceSmoke() { }
}
