package mechanist.input;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.*;

/**
 * Global input binding authority for keyboard, mouse, and generic controller profiles.
 *
 * This class stores remappable bindings and exposes small bridges for Swing InputMap/ActionMap
 * integration and game polling. It does not own hardware polling directly.
 */
public final class KeyBindingManager {
    private static final KeyBindingManager INSTANCE = new KeyBindingManager();

    public static KeyBindingManager getInstance() {
        return INSTANCE;
    }

    private final EnumMap<InputDevice, LinkedHashMap<String, KeyBind>> profiles = new EnumMap<>(InputDevice.class);
    private final Map<String, Boolean> actionStates = Collections.synchronizedMap(new LinkedHashMap<>());
    private final PropertyChangeSupport changes = new PropertyChangeSupport(this);

    private KeyBindingManager() {
        for (var device : InputDevice.values()) {
            profiles.put(device, new LinkedHashMap<>());
        }
        installDefaultBindings();
    }

    public synchronized List<KeyBind> getBindings(InputDevice device) {
        return List.copyOf(profiles.get(device).values());
    }

    public synchronized Optional<KeyBind> getBinding(InputDevice device, String commandId) {
        return Optional.ofNullable(profiles.get(device).get(commandId));
    }

    public synchronized Optional<String> commandFor(InputToken token) {
        var profile = profiles.get(token.device());
        if (profile == null) {
            return Optional.empty();
        }
        return profile.values().stream()
                .filter(bind -> bind.token().equals(token))
                .map(KeyBind::commandId)
                .findFirst();
    }

    public synchronized RebindResult rebind(InputDevice device, String commandId, InputToken newToken, DuplicatePolicy duplicatePolicy) {
        Objects.requireNonNull(device, "device");
        Objects.requireNonNull(commandId, "commandId");
        Objects.requireNonNull(newToken, "newToken");
        Objects.requireNonNull(duplicatePolicy, "duplicatePolicy");
        if (newToken.device() != device) {
            return RebindResult.rejected("That input belongs to " + newToken.device().displayName() + ", not " + device.displayName() + ".");
        }

        var profile = profiles.get(device);
        var current = profile.get(commandId);
        if (current == null) {
            return RebindResult.rejected("Unknown command id: " + commandId);
        }

        var duplicate = profile.values().stream()
                .filter(bind -> !bind.commandId().equals(commandId))
                .filter(bind -> bind.token().equals(newToken))
                .findFirst();

        if (duplicate.isPresent() && duplicatePolicy == DuplicatePolicy.REJECT) {
            return RebindResult.duplicateRejected(duplicate.get());
        }

        var oldToken = current.token();
        profile.put(commandId, current.withToken(newToken));
        KeyBind swappedBind = null;
        if (duplicate.isPresent() && duplicatePolicy == DuplicatePolicy.SWAP) {
            var duplicateBind = duplicate.get();
            swappedBind = duplicateBind.withToken(oldToken);
            profile.put(duplicateBind.commandId(), swappedBind);
        }
        changes.firePropertyChange("bindings", null, device);
        return RebindResult.accepted(profile.get(commandId), swappedBind);
    }

    public void setActionState(String commandId, boolean active) {
        actionStates.put(commandId, active);
    }

    public boolean isActionActive(String commandId) {
        return Boolean.TRUE.equals(actionStates.get(commandId));
    }

    public void dispatchTokenPressed(InputToken token) {
        commandFor(token).ifPresent(command -> setActionState(command, true));
    }

    public void dispatchTokenReleased(InputToken token) {
        commandFor(token).ifPresent(command -> setActionState(command, false));
    }

    public void installKeyboardBindings(JComponent component) {
        Objects.requireNonNull(component, "component");
        var inputMap = component.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        var actionMap = component.getActionMap();
        for (var bind : getBindings(InputDevice.KEYBOARD)) {
            var keyStroke = keyStrokeFor(bind.token(), false);
            if (keyStroke.isEmpty()) {
                continue;
            }
            var pressedKey = "mechanist.pressed." + bind.commandId();
            inputMap.put(keyStroke.get(), pressedKey);
            actionMap.put(pressedKey, new AbstractAction() {
                @Override public void actionPerformed(ActionEvent e) {
                    setActionState(bind.commandId(), true);
                }
            });
        }
    }

    private Optional<KeyStroke> keyStrokeFor(InputToken token, boolean release) {
        if (token.device() != InputDevice.KEYBOARD || !token.code().startsWith("KEY_")) {
            return Optional.empty();
        }
        var code = token.code();
        int keyCode;
        int modifiers = 0;
        try {
            if (code.contains("_MOD_")) {
                var parts = code.substring(4).split("_MOD_", 2);
                keyCode = Integer.parseInt(parts[0]);
                modifiers = Integer.parseInt(parts[1]);
            } else {
                keyCode = Integer.parseInt(code.substring(4));
            }
            return Optional.of(KeyStroke.getKeyStroke(keyCode, modifiers, release));
        } catch (RuntimeException ex) {
            return Optional.empty();
        }
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        changes.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        changes.removePropertyChangeListener(listener);
    }

    private void installDefaultBindings() {
        var keyboard = profiles.get(InputDevice.KEYBOARD);
        put(keyboard, "move.up", "Move Up", InputToken.keyboard(KeyEvent.VK_W, 0));
        put(keyboard, "move.down", "Move Down", InputToken.keyboard(KeyEvent.VK_S, 0));
        put(keyboard, "move.left", "Move Left", InputToken.keyboard(KeyEvent.VK_A, 0));
        put(keyboard, "move.right", "Move Right", InputToken.keyboard(KeyEvent.VK_D, 0));
        put(keyboard, "interact", "Interact", InputToken.keyboard(KeyEvent.VK_E, 0));
        put(keyboard, "confirm", "Confirm", InputToken.keyboard(KeyEvent.VK_ENTER, 0));
        put(keyboard, "cancel", "Cancel / Back", InputToken.keyboard(KeyEvent.VK_ESCAPE, 0));
        put(keyboard, "inventory", "Inventory", InputToken.keyboard(KeyEvent.VK_I, 0));
        put(keyboard, "map", "Map", InputToken.keyboard(KeyEvent.VK_M, 0));
        put(keyboard, "pause", "Pause", InputToken.keyboard(KeyEvent.VK_P, 0));

        var mouse = profiles.get(InputDevice.MOUSE);
        put(mouse, "confirm", "Confirm / Select", InputToken.mouseButton(MouseEvent.BUTTON1));
        put(mouse, "cancel", "Cancel / Back", InputToken.mouseButton(MouseEvent.BUTTON3));
        put(mouse, "look", "Look / Inspect", InputToken.mouseButton(MouseEvent.BUTTON2));

        var pad = profiles.get(InputDevice.GENERIC_CONTROLLER);
        put(pad, "move.up", "Move Up", InputToken.controllerHat("up"));
        put(pad, "move.down", "Move Down", InputToken.controllerHat("down"));
        put(pad, "move.left", "Move Left", InputToken.controllerHat("left"));
        put(pad, "move.right", "Move Right", InputToken.controllerHat("right"));
        put(pad, "confirm", "Confirm", InputToken.controllerButton(0));
        put(pad, "cancel", "Cancel / Back", InputToken.controllerButton(1));
        put(pad, "interact", "Interact", InputToken.controllerButton(2));
        put(pad, "inventory", "Inventory", InputToken.controllerButton(3));
        put(pad, "pause", "Pause", InputToken.controllerButton(7));
    }

    private static void put(Map<String, KeyBind> map, String commandId, String displayName, InputToken token) {
        map.put(commandId, new KeyBind(commandId, displayName, token));
    }

    public enum DuplicatePolicy { REJECT, SWAP }

    public record RebindResult(boolean accepted, String message, KeyBind updated, KeyBind swapped) {
        static RebindResult accepted(KeyBind updated, KeyBind swapped) {
            var message = swapped == null ? "Binding updated." : "Binding updated and duplicate command swapped.";
            return new RebindResult(true, message, updated, swapped);
        }
        static RebindResult rejected(String message) {
            return new RebindResult(false, message, null, null);
        }
        static RebindResult duplicateRejected(KeyBind duplicate) {
            return new RebindResult(false, "Already bound to " + duplicate.displayName() + ".", null, duplicate);
        }
    }
}
