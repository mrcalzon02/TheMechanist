package mechanist;

import java.awt.KeyboardFocusManager;
import java.awt.KeyEventDispatcher;
import java.awt.event.KeyEvent;

/**
 * Restores direct player-portrait browsing during staged character creation.
 *
 * Candidate.portraitIndex remains the sole persistent portrait-selection state;
 * this authority only exposes input over that existing identity before the
 * staged WorldStartFlow dispatcher consumes otherwise-unhandled keys.
 */
final class CharacterCreationPortraitSelectionAuthority implements KeyEventDispatcher {
    static final String VERSION = "character-creation-portrait-selection-1.0";
    static final String CLIENT_PROPERTY = "mechanist.characterCreationPortraitSelectionAuthority";

    private final GamePanel panel;

    private CharacterCreationPortraitSelectionAuthority(GamePanel panel) {
        this.panel = panel;
    }

    static void installBeforeWorldStartFlow(GamePanel panel) {
        if (panel == null) return;
        if (panel.getClientProperty(CLIENT_PROPERTY) instanceof CharacterCreationPortraitSelectionAuthority) return;
        CharacterCreationPortraitSelectionAuthority authority = new CharacterCreationPortraitSelectionAuthority(panel);
        panel.putClientProperty(CLIENT_PROPERTY, authority);
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(authority);
        DebugLog.audit("CHARACTER_PORTRAIT_SELECTION", "installed " + VERSION + " keys=[/]");
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event == null || event.getID() != KeyEvent.KEY_PRESSED) return false;
        if (panel == null || panel.screen != GamePanel.Screen.CHARACTER || !panel.newGameSetupActive) return false;
        if (!WorldStartFlowAuthority.isActive(panel)) return false;

        int direction = directionForKey(event.getKeyCode());
        if (direction == 0) return false;
        if (!cycleSelectedPortrait(panel, direction)) return false;

        event.consume();
        return true;
    }

    static int directionForKey(int keyCode) {
        if (keyCode == KeyEvent.VK_OPEN_BRACKET) return -1;
        if (keyCode == KeyEvent.VK_CLOSE_BRACKET) return 1;
        return 0;
    }

    static boolean cycleSelectedPortrait(GamePanel panel, int direction) {
        if (panel == null || direction == 0) return false;
        Candidate candidate = panel.selectedNewGameCandidate();
        if (candidate == null) return false;

        candidate.portraitIndex = shiftedPortraitIndex(candidate.portraitIndex, direction);
        panel.active = candidate;
        updateStartFlowStatus(panel, candidate.portraitIndex);
        panel.repaint();
        return true;
    }

    static int shiftedPortraitIndex(int current, int direction) {
        int safe = Math.max(0, current);
        if (direction > 0) return safe == Integer.MAX_VALUE ? 0 : safe + 1;
        if (direction < 0) return safe == 0 ? Integer.MAX_VALUE : safe - 1;
        return safe;
    }

    private static void updateStartFlowStatus(GamePanel panel, int portraitIndex) {
        Object value = panel.getClientProperty(WorldStartFlowAuthority.WorldStartFlowOverlay.CLIENT_PROPERTY);
        if (value instanceof WorldStartFlowAuthority.WorldStartFlowOverlay overlay) {
            overlay.status = "Portrait profile " + (portraitIndex + 1L) + " selected. Use [ and ] to browse portraits.";
        }
    }

    private CharacterCreationPortraitSelectionAuthority() {
        this.panel = null;
    }
}
