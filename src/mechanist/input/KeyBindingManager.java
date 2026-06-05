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
    private static final Set<String> REQUIRED_COMMANDS = Set.of("move.up", "move.down", "move.left", "move.right", "confirm", "cancel", "pause");

    public static KeyBindingManager getInstance() {
        return INSTANCE;
    }

    private final EnumMap<InputDevice, LinkedHashMap<String, KeyBind>> profiles = new EnumMap<>(InputDevice.class);
    private final EnumMap<InputDevice, LinkedHashMap<String, KeyBind>> defaultProfiles = new EnumMap<>(InputDevice.class);
    private final EnumMap<InputDevice, LinkedHashMap<String, KeyBind>> lastGoodProfiles = new EnumMap<>(InputDevice.class);
    private final Map<String, Boolean> actionStates = Collections.synchronizedMap(new LinkedHashMap<>());
    private final PropertyChangeSupport changes = new PropertyChangeSupport(this);

    private KeyBindingManager() {
        for (var device : InputDevice.values()) {
            profiles.put(device, new LinkedHashMap<>());
            defaultProfiles.put(device, new LinkedHashMap<>());
            lastGoodProfiles.put(device, new LinkedHashMap<>());
        }
        installDefaultBindings();
        for (var device : InputDevice.values()) {
            defaultProfiles.put(device, copyProfile(profiles.get(device)));
            lastGoodProfiles.put(device, copyProfile(profiles.get(device)));
        }
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
            return RebindResult.duplicateRejected(duplicate.get(), conflictMessage(current, duplicate.get()));
        }

        var oldToken = current.token();
        lastGoodProfiles.put(device, copyProfile(profile));
        profile.put(commandId, current.withToken(newToken));
        KeyBind swappedBind = null;
        if (duplicate.isPresent() && duplicatePolicy == DuplicatePolicy.SWAP) {
            var duplicateBind = duplicate.get();
            swappedBind = duplicateBind.withToken(oldToken);
            profile.put(duplicateBind.commandId(), swappedBind);
        }
        changes.firePropertyChange("bindings", null, device);
        return RebindResult.accepted(profile.get(commandId), swappedBind, duplicate.map(d -> conflictMessage(current, d)).orElse(""));
    }

    public synchronized RebindResult resetDeviceToDefaults(InputDevice device) {
        Objects.requireNonNull(device, "device");
        lastGoodProfiles.put(device, copyProfile(profiles.get(device)));
        profiles.put(device, copyProfile(defaultProfiles.get(device)));
        changes.firePropertyChange("bindings", null, device);
        return RebindResult.accepted("Reset " + device.displayName() + " bindings to defaults.");
    }

    public synchronized RebindResult resetAllToDefaults() {
        for (var device : InputDevice.values()) {
            lastGoodProfiles.put(device, copyProfile(profiles.get(device)));
            profiles.put(device, copyProfile(defaultProfiles.get(device)));
        }
        changes.firePropertyChange("bindings", null, null);
        return RebindResult.accepted("Reset all bindings to defaults.");
    }

    public synchronized RebindResult restoreLastGoodProfile(InputDevice device) {
        Objects.requireNonNull(device, "device");
        LinkedHashMap<String, KeyBind> lastGood = lastGoodProfiles.get(device);
        if (lastGood == null || lastGood.isEmpty()) {
            return RebindResult.rejected("No last working " + device.displayName() + " profile is available.");
        }
        LinkedHashMap<String, KeyBind> current = copyProfile(profiles.get(device));
        profiles.put(device, copyProfile(lastGood));
        lastGoodProfiles.put(device, current);
        changes.firePropertyChange("bindings", null, device);
        return RebindResult.accepted("Restored last working " + device.displayName() + " bindings.");
    }

    public synchronized RebindResult restoreAllLastGoodProfiles() {
        for (var device : InputDevice.values()) {
            LinkedHashMap<String, KeyBind> lastGood = lastGoodProfiles.get(device);
            if (lastGood == null || lastGood.isEmpty()) continue;
            LinkedHashMap<String, KeyBind> current = copyProfile(profiles.get(device));
            profiles.put(device, copyProfile(lastGood));
            lastGoodProfiles.put(device, current);
        }
        changes.firePropertyChange("bindings", null, null);
        return RebindResult.accepted("Restored last working bindings for all input families.");
    }

    public synchronized Properties exportProfileProperties() {
        Properties out = new Properties();
        for (var device : InputDevice.values()) {
            var profile = profiles.get(device);
            if (profile == null) continue;
            for (var bind : profile.values()) {
                out.setProperty(storageKey(device, bind.commandId()), bind.token().storageValue());
            }
        }
        return out;
    }

    public synchronized RebindResult importProfileProperties(Properties source) {
        Objects.requireNonNull(source, "source");
        EnumMap<InputDevice, LinkedHashMap<String, KeyBind>> candidate = new EnumMap<>(InputDevice.class);
        for (var device : InputDevice.values()) candidate.put(device, copyProfile(profiles.get(device)));

        for (var device : InputDevice.values()) {
            var profile = candidate.get(device);
            if (profile == null) continue;
            for (var commandId : List.copyOf(profile.keySet())) {
                String value = source.getProperty(storageKey(device, commandId));
                if (value == null || value.isBlank()) continue;
                InputToken token;
                try {
                    token = InputToken.fromStorageValue(value);
                } catch (RuntimeException ex) {
                    return RebindResult.rejected("Saved binding for " + device.displayName() + " " + commandId + " is malformed.");
                }
                if (token.device() != device) {
                    return RebindResult.rejected("Saved binding for " + commandId + " belongs to " + token.device().displayName() + ", not " + device.displayName() + ".");
                }
                profile.put(commandId, profile.get(commandId).withToken(token));
            }
        }

        String missing = missingRequiredCommand(candidate);
        if (!missing.isBlank()) {
            return RebindResult.rejected("Saved profile is missing required recovery action: " + missing + ".");
        }

        for (var device : InputDevice.values()) {
            lastGoodProfiles.put(device, copyProfile(profiles.get(device)));
            profiles.put(device, copyProfile(candidate.get(device)));
        }
        changes.firePropertyChange("bindings", null, null);
        return RebindResult.accepted("Imported saved keybinding profile.");
    }

    public synchronized boolean requiredCommandBound(InputDevice device, String commandId) {
        if (!isRequiredCommand(commandId)) return true;
        var profile = profiles.get(device);
        return profile != null && profile.containsKey(commandId) && profile.get(commandId) != null;
    }

    public static boolean isRequiredCommand(String commandId) {
        return commandId != null && REQUIRED_COMMANDS.contains(commandId.trim());
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
        put(keyboard, "wait", "Wait One Turn", InputToken.keyboard(KeyEvent.VK_PERIOD, 0));
        put(keyboard, "interact", "Interact", InputToken.keyboard(KeyEvent.VK_E, 0));
        put(keyboard, "examine", "Examine", InputToken.keyboard(KeyEvent.VK_E, 0));
        put(keyboard, "confirm", "Confirm", InputToken.keyboard(KeyEvent.VK_ENTER, 0));
        put(keyboard, "cancel", "Cancel / Back", InputToken.keyboard(KeyEvent.VK_ESCAPE, 0));
        put(keyboard, "inventory", "Inventory", InputToken.keyboard(KeyEvent.VK_I, 0));
        put(keyboard, "character", "Character Dossier", InputToken.keyboard(KeyEvent.VK_C, 0));
        put(keyboard, "build", "Build and Base Tools", InputToken.keyboard(KeyEvent.VK_B, 0));
        put(keyboard, "senses", "Senses", InputToken.keyboard(KeyEvent.VK_G, 0));
        put(keyboard, "plan.move", "Plan Exact Movement", InputToken.keyboard(KeyEvent.VK_P, 0));
        put(keyboard, "look", "Look", InputToken.keyboard(KeyEvent.VK_L, 0));
        put(keyboard, "map", "Map", InputToken.keyboard(KeyEvent.VK_M, 0));
        put(keyboard, "zoom.in", "Zoom In", InputToken.keyboard(KeyEvent.VK_HOME, 0));
        put(keyboard, "zoom.out", "Zoom Out", InputToken.keyboard(KeyEvent.VK_END, 0));
        put(keyboard, "attack", "Combat Targeting", InputToken.keyboard(KeyEvent.VK_F, 0));
        put(keyboard, "reload", "Reload", InputToken.keyboard(KeyEvent.VK_X, 0));
        put(keyboard, "pause", "Pause", InputToken.keyboard(KeyEvent.VK_ESCAPE, 0));

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
        put(pad, "examine", "Examine", InputToken.controllerButton(0));
        put(pad, "inventory", "Inventory", InputToken.controllerButton(3));
        put(pad, "character", "Character Dossier", InputToken.controllerButton(4));
        put(pad, "look", "Look", InputToken.controllerButton(6));
        put(pad, "plan.move", "Plan Exact Movement", InputToken.controllerAxis("right_stick_y", 1));
        put(pad, "attack", "Combat Targeting", InputToken.controllerAxis("right_stick_y", -1));
        put(pad, "zoom.in", "Zoom In", InputToken.controllerButton(5));
        put(pad, "zoom.out", "Zoom Out", InputToken.controllerButton(4));
        put(pad, "pause", "Pause", InputToken.controllerButton(7));
    }

    private static void put(Map<String, KeyBind> map, String commandId, String displayName, InputToken token) {
        map.put(commandId, new KeyBind(commandId, displayName, token));
    }

    private static LinkedHashMap<String, KeyBind> copyProfile(Map<String, KeyBind> source) {
        LinkedHashMap<String, KeyBind> copy = new LinkedHashMap<>();
        if (source == null) return copy;
        for (var entry : source.entrySet()) copy.put(entry.getKey(), entry.getValue());
        return copy;
    }

    private static String storageKey(InputDevice device, String commandId) {
        return "binding." + device.name() + "." + commandId;
    }

    private static String missingRequiredCommand(EnumMap<InputDevice, LinkedHashMap<String, KeyBind>> candidate) {
        for (var device : InputDevice.values()) {
            var defaults = INSTANCE.defaultProfiles.get(device);
            var profile = candidate.get(device);
            if (defaults == null || profile == null) continue;
            for (var commandId : REQUIRED_COMMANDS) {
                if (!defaults.containsKey(commandId)) continue;
                KeyBind bind = profile.get(commandId);
                if (bind == null || bind.token() == null) return device.displayName() + " " + commandId;
            }
        }
        return "";
    }

    private static String conflictMessage(KeyBind target, KeyBind duplicate) {
        if (target == null || duplicate == null) return "";
        String targetContext = commandContext(target.commandId());
        String duplicateContext = commandContext(duplicate.commandId());
        String scope = Objects.equals(targetContext, duplicateContext) ? "same-context" : "cross-context";
        String risk = (isRequiredCommand(target.commandId()) || isRequiredCommand(duplicate.commandId())) ? "recovery-critical" : "reviewed";
        return "Conflict reviewed: " + scope + " " + risk + " overlap with " + duplicate.displayName() + ".";
    }

    private static String commandContext(String commandId) {
        if (commandId == null) return "general";
        if (commandId.startsWith("move.")) return "movement";
        return switch (commandId) {
            case "confirm", "cancel", "pause" -> "global recovery";
            case "interact", "examine", "look", "wait", "build", "senses", "plan.move" -> "game surface";
            case "inventory", "character", "map" -> "menu access";
            case "zoom.in", "zoom.out" -> "map and viewport";
            case "attack", "reload" -> "combat";
            default -> "general";
        };
    }

    public enum DuplicatePolicy { REJECT, SWAP }

    public record RebindResult(boolean accepted, String message, KeyBind updated, KeyBind swapped) {
        static RebindResult accepted(KeyBind updated, KeyBind swapped, String conflictMessage) {
            var message = swapped == null ? "Binding updated." : "Binding updated and duplicate command swapped. " + conflictMessage;
            return new RebindResult(true, message, updated, swapped);
        }
        static RebindResult accepted(String message) {
            return new RebindResult(true, message, null, null);
        }
        static RebindResult rejected(String message) {
            return new RebindResult(false, message, null, null);
        }
        static RebindResult duplicateRejected(KeyBind duplicate, String conflictMessage) {
            return new RebindResult(false, "Already bound to " + duplicate.displayName() + ". " + conflictMessage, null, duplicate);
        }
    }
}
