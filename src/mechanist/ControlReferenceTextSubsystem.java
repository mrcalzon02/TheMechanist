package mechanist;

import mechanist.input.InputDevice;
import mechanist.input.KeyBindingManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Stable subsystem for keyboard/controller reference text.
 */
final class ControlReferenceTextSubsystem {
    private ControlReferenceTextSubsystem() {
    }

    record ContextPrompt(String context, InputAction primary, InputAction secondary, String detail) { }

    static ArrayList<String> controlsReferenceLines(GamePanel panel) {
        ArrayList<String> lines = new ArrayList<>();
        lines.add(controlProfileTitle(panel.controlsTab));
        lines.add("Action names are shared across keyboard and controller paths; context decides whether a prompt moves, examines, interacts, confirms, or backs out.");
        lines.add("Required recovery actions: Move, Confirm, Cancel/Back, Pause/Menu, and a keyboard/mouse fallback.");
        for (InputAction action : InputAction.values()) {
            lines.add(inputActionLabel(action) + " | " + inputActionContext(action) + " | "
                    + keyboardPromptFor(action) + " | " + controllerPromptFor(panel.controlsTab, action)
                    + (requiredInputAction(action) ? " | required" : ""));
        }
        lines.add("Context overlap: E examines while the Look panel is open and interacts from the game surface; A/Cross confirms in menus and examines/interacts only where that panel owns the prompt.");
        lines.add("Safe fallback: Escape/B/Circle backs out, and keyboard movement remains available if a controller is missing or reconnecting.");
        lines.add("Context prompt examples:");
        lines.addAll(contextPromptLines(panel.controlsTab, defaultContextPrompts()));
        if (panel.rebindingTarget != null && !panel.rebindingTarget.isEmpty()) lines.add(panel.rebindingTarget);
        return lines;
    }

    static ArrayList<ContextPrompt> defaultContextPrompts() {
        ArrayList<ContextPrompt> prompts = new ArrayList<>();
        prompts.add(new ContextPrompt("Look", InputAction.EXAMINE, InputAction.CANCEL, "Examine the selected visible target or back out."));
        prompts.add(new ContextPrompt("Interact", InputAction.INTERACT, InputAction.CANCEL, "Use the selected adjacent target or back out."));
        prompts.add(new ContextPrompt("Movement planning", InputAction.CONFIRM, InputAction.CANCEL, "Confirm the ghost target, nudge it, or cancel safely."));
        prompts.add(new ContextPrompt("Inventory", InputAction.INVENTORY, InputAction.CANCEL, "Review carried items and return to the game."));
        prompts.add(new ContextPrompt("Trade", InputAction.CONFIRM, InputAction.CANCEL, "Choose a trade action or leave the offer list."));
        prompts.add(new ContextPrompt("Build", InputAction.BUILD, InputAction.CANCEL, "Open build tools, choose placement, or back out."));
        prompts.add(new ContextPrompt("Map", InputAction.MAP, InputAction.CANCEL, "Review route guidance and return to the game."));
        prompts.add(new ContextPrompt("Save/Load", InputAction.CONFIRM, InputAction.CANCEL, "Choose a slot or back out without changing saves."));
        return prompts;
    }

    static String contextPromptLine(String context, int controlsTab, InputAction primary, InputAction secondary, String detail) {
        String safeContext = safePromptPart(context, "Prompt");
        String safeDetail = safePromptPart(detail, "");
        String primaryPart = actionPromptPart(controlsTab, primary);
        String secondaryPart = secondary == null ? "" : actionPromptPart(controlsTab, secondary);
        StringBuilder out = new StringBuilder(safeContext).append(": ");
        out.append(primaryPart);
        if (!secondaryPart.isBlank()) out.append(" | ").append(secondaryPart);
        if (!safeDetail.isBlank()) out.append(" - ").append(safeDetail);
        return PlayerFacingText.sanitize(out.toString());
    }

    static ArrayList<String> contextPromptLines(int controlsTab, List<ContextPrompt> prompts) {
        ArrayList<String> lines = new ArrayList<>();
        if (prompts == null || prompts.isEmpty()) return lines;
        for (ContextPrompt prompt : prompts) {
            if (prompt == null) continue;
            lines.add(contextPromptLine(prompt.context(), controlsTab, prompt.primary(), prompt.secondary(), prompt.detail()));
        }
        return lines;
    }

    static String livePanelPrompt(String context, int controlsTab) {
        String key = context == null ? "Panel" : context;
        InputAction primary = switch (key) {
            case "Look" -> InputAction.EXAMINE;
            case "Interact" -> InputAction.INTERACT;
            case "Inventory" -> InputAction.INVENTORY;
            case "Build" -> InputAction.BUILD;
            case "Map" -> InputAction.MAP;
            case "Character" -> InputAction.CHARACTER;
            case "Auspex" -> InputAction.SENSES;
            case "Combat" -> InputAction.ATTACK;
            case "Movement planning", "Trade", "Container", "Conversation", "Object", "Crafting", "Scavenge", "Infopedia", "Pause", "Panel" -> InputAction.CONFIRM;
            default -> InputAction.CONFIRM;
        };
        return contextPromptLine(key, controlsTab, primary, InputAction.CANCEL, "");
    }

    static String controlProfileTitle(int controlsTab) {
        switch (controlsTab) {
            case 1: return "XBOX CONTROL REFERENCE";
            case 2: return "PLAYSTATION CONTROL REFERENCE";
            case 3: return "STEAM DECK CONTROL REFERENCE";
            case 4: return "GENERIC CONTROLLER REFERENCE";
            default: return "KEYBOARD AND MOUSE CONTROL REFERENCE";
        }
    }

    static boolean requiredInputAction(InputAction action) {
        return action == InputAction.MOVE_UP || action == InputAction.MOVE_DOWN || action == InputAction.MOVE_LEFT
                || action == InputAction.MOVE_RIGHT || action == InputAction.CONFIRM || action == InputAction.CANCEL
                || action == InputAction.PAUSE;
    }

    static String inputActionLabel(InputAction action) {
        switch (action) {
            case MOVE_UP: return "Move / nudge up";
            case MOVE_DOWN: return "Move / nudge down";
            case MOVE_LEFT: return "Move / nudge left";
            case MOVE_RIGHT: return "Move / nudge right";
            case CONFIRM: return "Confirm / choose";
            case CANCEL: return "Cancel / back";
            case WAIT: return "Wait one turn";
            case INTERACT: return "Interact";
            case EXAMINE: return "Examine visible target";
            case INVENTORY: return "Inventory";
            case CHARACTER: return "Character dossier";
            case BUILD: return "Build and base tools";
            case SENSES: return "Senses";
            case PLAN_MOVE: return "Plan exact movement";
            case PAUSE: return "Pause / menu";
            case LOOK: return "Look";
            case MAP: return "Map";
            case ZOOM_IN: return "Zoom in";
            case ZOOM_OUT: return "Zoom out";
            case ATTACK: return "Combat targeting";
            case RELOAD: return "Reload";
            default: return action.name();
        }
    }

    static String inputActionContext(InputAction action) {
        switch (action) {
            case MOVE_UP: case MOVE_DOWN: case MOVE_LEFT: case MOVE_RIGHT: return "movement, targeting, lists";
            case CONFIRM: return "menus, targeting, planning";
            case CANCEL: return "global recovery";
            case WAIT: return "game surface";
            case INTERACT: return "game surface and interact panel";
            case EXAMINE: return "Look panel";
            case INVENTORY: case CHARACTER: case BUILD: case SENSES: case LOOK: case MAP: return "game surface";
            case PLAN_MOVE: return "game surface movement planning";
            case PAUSE: return "global menu";
            case ZOOM_IN: case ZOOM_OUT: return "map and viewport";
            case ATTACK: case RELOAD: return "combat";
            default: return "general";
        }
    }

    static String keyboardPromptFor(InputAction action) {
        String dynamic = dynamicPrompt(InputDevice.KEYBOARD, action, "Keyboard");
        if (!dynamic.isBlank()) {
            if (action == InputAction.EXAMINE) return dynamic + " while looking";
            return dynamic;
        }
        return defaultKeyboardPromptFor(action);
    }

    static String defaultKeyboardPromptFor(InputAction action) {
        switch (action) {
            case MOVE_UP: return "Keyboard: W / Up";
            case MOVE_DOWN: return "Keyboard: S / Down";
            case MOVE_LEFT: return "Keyboard: A / Left";
            case MOVE_RIGHT: return "Keyboard: D / Right";
            case CONFIRM: return "Keyboard: Enter / Space";
            case CANCEL: return "Keyboard: Escape";
            case WAIT: return "Keyboard: . / Numpad 5";
            case INTERACT: return "Keyboard: E";
            case EXAMINE: return "Keyboard: E while looking";
            case INVENTORY: return "Keyboard: I";
            case CHARACTER: return "Keyboard: C";
            case BUILD: return "Keyboard: B";
            case SENSES: return "Keyboard: G";
            case PLAN_MOVE: return "Keyboard: P";
            case PAUSE: return "Keyboard: Escape";
            case LOOK: return "Keyboard: L";
            case MAP: return "Keyboard: M";
            case ZOOM_IN: return "Keyboard: Home / wheel up";
            case ZOOM_OUT: return "Keyboard: End / wheel down";
            case ATTACK: return "Keyboard: F";
            case RELOAD: return "Keyboard: X";
            default: return "Keyboard: unassigned";
        }
    }

    static String controllerPromptFor(int controlsTab, InputAction action) {
        switch (controlsTab) {
            case 1: return xboxPromptFor(action);
            case 2: return playstationPromptFor(action);
            case 3: return steamPromptFor(action);
            case 4: return genericPromptFor(action);
            default: return "Controller: see controller tabs";
        }
    }

    static String xboxPromptFor(InputAction action) {
        switch (action) {
            case MOVE_UP: case MOVE_DOWN: case MOVE_LEFT: case MOVE_RIGHT: return "Xbox: left stick / D-pad";
            case CONFIRM: case INTERACT: case EXAMINE: return "Xbox: A";
            case CANCEL: return "Xbox: B";
            case INVENTORY: return "Xbox: X";
            case CHARACTER: return "Xbox: Y";
            case PAUSE: return "Xbox: Menu";
            case LOOK: return "Xbox: View";
            case ZOOM_IN: return "Xbox: RB";
            case ZOOM_OUT: return "Xbox: LB";
            case ATTACK: return "Xbox: right stick up";
            case PLAN_MOVE: return "Xbox: right stick down";
            default: return "Xbox: keyboard fallback";
        }
    }

    static String playstationPromptFor(InputAction action) {
        switch (action) {
            case MOVE_UP: case MOVE_DOWN: case MOVE_LEFT: case MOVE_RIGHT: return "PlayStation: left stick / D-pad";
            case CONFIRM: case INTERACT: case EXAMINE: return "PlayStation: Cross";
            case CANCEL: return "PlayStation: Circle";
            case INVENTORY: return "PlayStation: Square";
            case CHARACTER: return "PlayStation: Triangle";
            case PAUSE: return "PlayStation: Options";
            case LOOK: return "PlayStation: Share/Create";
            case ZOOM_IN: return "PlayStation: R1";
            case ZOOM_OUT: return "PlayStation: L1";
            case ATTACK: return "PlayStation: right stick up";
            case PLAN_MOVE: return "PlayStation: right stick down";
            default: return "PlayStation: keyboard fallback";
        }
    }

    static String steamPromptFor(InputAction action) {
        switch (action) {
            case MOVE_UP: case MOVE_DOWN: case MOVE_LEFT: case MOVE_RIGHT: return "Steam: stick / D-pad";
            case CONFIRM: case INTERACT: case EXAMINE: return "Steam: A";
            case CANCEL: return "Steam: B";
            case INVENTORY: return "Steam: X";
            case CHARACTER: return "Steam: Y";
            case PAUSE: return "Steam: Menu";
            case LOOK: return "Steam: View";
            case ZOOM_IN: return "Steam: R1";
            case ZOOM_OUT: return "Steam: L1";
            case ATTACK: return "Steam: right stick up";
            case PLAN_MOVE: return "Steam: right stick down";
            default: return "Steam: keyboard fallback";
        }
    }

    static String genericPromptFor(InputAction action) {
        String dynamic = dynamicPrompt(InputDevice.GENERIC_CONTROLLER, action, "Generic");
        if (!dynamic.isBlank()) return dynamic;
        return defaultGenericPromptFor(action);
    }

    static String defaultControllerPromptFor(int controlsTab, InputAction action) {
        return switch (controlsTab) {
            case 1 -> xboxPromptFor(action);
            case 2 -> playstationPromptFor(action);
            case 3 -> steamPromptFor(action);
            case 4 -> defaultGenericPromptFor(action);
            default -> "Controller: see controller tabs";
        };
    }

    private static String defaultGenericPromptFor(InputAction action) {
        switch (action) {
            case MOVE_UP: case MOVE_DOWN: case MOVE_LEFT: case MOVE_RIGHT: return "Generic: left stick / D-pad";
            case CONFIRM: case INTERACT: case EXAMINE: return "Generic: south face";
            case CANCEL: return "Generic: east face";
            case INVENTORY: return "Generic: west face";
            case CHARACTER: return "Generic: north face";
            case PAUSE: return "Generic: Start";
            case LOOK: return "Generic: Back/Select";
            case ZOOM_IN: return "Generic: right bumper";
            case ZOOM_OUT: return "Generic: left bumper";
            case ATTACK: return "Generic: right stick up";
            case PLAN_MOVE: return "Generic: right stick down";
            default: return "Generic: keyboard fallback";
        }
    }

    private static String actionPromptPart(int controlsTab, InputAction action) {
        if (action == null) return "Action: unavailable";
        String label = inputActionLabel(action);
        String keyboard = keyboardPromptFor(action);
        String controller = controllerPromptFor(controlsTab, action);
        if (controlsTab <= 0) return label + " [" + keyboard + "]";
        return label + " [" + controller + "; " + keyboard + "]";
    }

    static String commandIdFor(InputAction action) {
        if (action == null) return "";
        return switch (action) {
            case MOVE_UP -> "move.up";
            case MOVE_DOWN -> "move.down";
            case MOVE_LEFT -> "move.left";
            case MOVE_RIGHT -> "move.right";
            case CONFIRM -> "confirm";
            case CANCEL -> "cancel";
            case WAIT -> "wait";
            case INTERACT -> "interact";
            case EXAMINE -> "examine";
            case INVENTORY -> "inventory";
            case CHARACTER -> "character";
            case BUILD -> "build";
            case SENSES -> "senses";
            case PLAN_MOVE -> "plan.move";
            case PAUSE -> "pause";
            case LOOK -> "look";
            case MAP -> "map";
            case ZOOM_IN -> "zoom.in";
            case ZOOM_OUT -> "zoom.out";
            case ATTACK -> "attack";
            case RELOAD -> "reload";
        };
    }

    private static String dynamicPrompt(InputDevice device, InputAction action, String prefix) {
        String commandId = commandIdFor(action);
        if (commandId.isBlank()) return "";
        return KeyBindingManager.getInstance()
                .getBinding(device, commandId)
                .map(bind -> prefix + ": " + bind.token().displayName())
                .orElse("");
    }

    private static String safePromptPart(String text, String fallback) {
        String cleaned = PlayerFacingText.sanitize(text)
                .replace(':', ' ')
                .replace('\r', ' ')
                .replace('\n', ' ')
                .replace('\t', ' ')
                .trim();
        if (cleaned.isBlank() || "No readable details are available yet.".equals(cleaned)) return fallback;
        return cleaned;
    }
}
