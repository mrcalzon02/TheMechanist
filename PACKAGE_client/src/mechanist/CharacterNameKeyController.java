package mechanist;

import java.awt.event.KeyEvent;

final class CharacterNameKeyController {
    private CharacterNameKeyController() {}

    static boolean handleCharacterNameEditKey(GamePanel panel, int code) {
        if (panel.screen != GamePanel.Screen.CHARACTER || !panel.characterNameEditActive) return false;
        Candidate candidate = panel.candidates.isEmpty() ? null : panel.candidates.get(panel.candidateIndex);
        if (code == KeyEvent.VK_ESCAPE || code == KeyEvent.VK_ENTER) {
            if (candidate != null) {
                candidate.name = CharacterCreationAuthority.sanitizePlayerName(candidate.name, panel.rng);
                panel.refreshNameLockedCandidateState(candidate);
            }
            panel.characterNameEditActive = false;
            panel.repaint();
            return true;
        }
        if (code == KeyEvent.VK_BACK_SPACE) {
            if (candidate != null && candidate.name != null && !candidate.name.isEmpty()) {
                candidate.name = candidate.name.substring(0, candidate.name.length() - 1);
                panel.refreshNameLockedCandidateState(candidate);
            }
            panel.repaint();
            return true;
        }
        if (code == KeyEvent.VK_DELETE) {
            if (candidate != null) {
                candidate.name = "";
                panel.refreshNameLockedCandidateState(candidate);
            }
            panel.repaint();
            return true;
        }
        return true;
    }
}
