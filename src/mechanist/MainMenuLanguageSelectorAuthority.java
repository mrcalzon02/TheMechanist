package mechanist;

import java.awt.Rectangle;
import java.util.ArrayList;

/**
 * Main-menu language selector state.
 *
 * Rendering remains with the main UI/painter layer; this authority owns the
 * dropdown state and semantic text manager binding so a launcher language can be
 * overridden from the upper-left main-menu control.
 */
final class MainMenuLanguageSelectorAuthority {
    private final SemanticTextManager textManager;
    private boolean open;
    private int selectedIndex;

    MainMenuLanguageSelectorAuthority(SemanticTextManager textManager) {
        this.textManager = textManager == null ? SemanticTextManager.createDefault() : textManager;
        this.open = false;
        this.selectedIndex = Math.max(0, languages().indexOf(this.textManager.currentLanguage()));
    }

    String buttonText() {
        return textManager.text("ui.main.language.current", textManager.currentLanguage().toUpperCase());
    }

    String titleText() {
        return textManager.text("ui.main.language.dropdown.title");
    }

    boolean isOpen() {
        return open;
    }

    void toggle() {
        open = !open;
    }

    void close() {
        open = false;
    }

    ArrayList<String> languages() {
        return textManager.availableLanguageCodes();
    }

    String currentLanguage() {
        return textManager.currentLanguage();
    }

    String selectLanguage(int index) {
        ArrayList<String> codes = languages();
        if (codes.isEmpty()) return textManager.currentLanguage();
        selectedIndex = Math.max(0, Math.min(index, codes.size() - 1));
        textManager.setLanguage(codes.get(selectedIndex));
        open = false;
        return textManager.currentLanguage();
    }

    int selectedIndex() {
        return selectedIndex;
    }

    Rectangle defaultButtonBounds() {
        return new Rectangle(12, 12, 168, 28);
    }

    Rectangle dropdownBounds() {
        int rows = Math.max(1, languages().size());
        return new Rectangle(12, 44, 220, 28 + rows * 24);
    }

    SemanticTextManager textManager() {
        return textManager;
    }
}
