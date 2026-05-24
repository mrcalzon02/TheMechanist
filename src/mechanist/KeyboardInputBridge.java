package mechanist;

import java.awt.event.KeyEvent;

final class KeyboardInputBridge {
    static final String VERSION = "0.9.10et";
    private final InputRegistry registry;

    KeyboardInputBridge(InputRegistry registry) {
        this.registry = registry;
    }

    void keyPressed(KeyEvent e) { apply(e.getKeyCode(), true); }
    void keyReleased(KeyEvent e) { apply(e.getKeyCode(), false); }

    private void apply(int code, boolean down) {
        switch (code) {
            case KeyEvent.VK_W:
            case KeyEvent.VK_UP:
                registry.setDigital(InputSource.KEYBOARD, InputAction.MOVE_UP, down); break;
            case KeyEvent.VK_S:
            case KeyEvent.VK_DOWN:
                registry.setDigital(InputSource.KEYBOARD, InputAction.MOVE_DOWN, down); break;
            case KeyEvent.VK_A:
            case KeyEvent.VK_LEFT:
                registry.setDigital(InputSource.KEYBOARD, InputAction.MOVE_LEFT, down); break;
            case KeyEvent.VK_D:
            case KeyEvent.VK_RIGHT:
                registry.setDigital(InputSource.KEYBOARD, InputAction.MOVE_RIGHT, down); break;
            case KeyEvent.VK_ENTER:
            case KeyEvent.VK_SPACE:
                registry.setDigital(InputSource.KEYBOARD, InputAction.CONFIRM, down); break;
            case KeyEvent.VK_ESCAPE:
                registry.setDigital(InputSource.KEYBOARD, InputAction.CANCEL, down); registry.setDigital(InputSource.KEYBOARD, InputAction.PAUSE, down); break;
            case KeyEvent.VK_E:
                registry.setDigital(InputSource.KEYBOARD, InputAction.INTERACT, down); break;
            case KeyEvent.VK_I:
                registry.setDigital(InputSource.KEYBOARD, InputAction.INVENTORY, down); break;
            case KeyEvent.VK_C:
                registry.setDigital(InputSource.KEYBOARD, InputAction.CHARACTER, down); break;
            case KeyEvent.VK_L:
                registry.setDigital(InputSource.KEYBOARD, InputAction.LOOK, down); break;
            case KeyEvent.VK_M:
                registry.setDigital(InputSource.KEYBOARD, InputAction.MAP, down); break;
            case KeyEvent.VK_HOME:
                registry.setDigital(InputSource.KEYBOARD, InputAction.ZOOM_IN, down); break;
            case KeyEvent.VK_END:
                registry.setDigital(InputSource.KEYBOARD, InputAction.ZOOM_OUT, down); break;
            case KeyEvent.VK_F:
                registry.setDigital(InputSource.KEYBOARD, InputAction.ATTACK, down); break;
            case KeyEvent.VK_X:
                registry.setDigital(InputSource.KEYBOARD, InputAction.RELOAD, down); break;
            default:
                break;
        }
    }

    static String auditSummary() {
        return "keyboardInputBridge version=" + VERSION + " updates=inputRegistry directLegacyKeyHandling=preserved";
    }
}
