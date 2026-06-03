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

    static boolean handleCharacterNameEditTyped(GamePanel panel, char ch) {
        if (panel.screen != GamePanel.Screen.CHARACTER || !panel.characterNameEditActive) return false;
        if (Character.isISOControl(ch)) return false;
        Candidate candidate = panel.candidates.isEmpty() ? null : panel.candidates.get(panel.candidateIndex);
        if (candidate == null) return true;
        if (candidate.name == null) candidate.name = "";
        if (candidate.name.length() >= 32) return true;
        if (Character.isLetterOrDigit(ch) || ch == ' ' || ch == '-' || ch == '\'') {
            candidate.name = candidate.name + ch;
            panel.refreshNameLockedCandidateState(candidate);
            panel.repaint();
            return true;
        }
        return true;
    }
}
