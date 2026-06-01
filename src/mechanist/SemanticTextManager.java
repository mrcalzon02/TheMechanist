package mechanist;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

/**
 * High-level player-facing text authority.
 *
 * New UI/player-facing strings should resolve through semantic keys.  Locale
 * files live under PACKAGE_client/assets/locales and may be supplied by the
 * launcher or selected through the main-menu language selector.
 */
final class SemanticTextManager {
    static final String DEFAULT_LANGUAGE = "en";
    static final String LOCALE_DIRECTORY = "PACKAGE_client/assets/locales";

    private final Path localeDirectory;
    private final LinkedHashMap<String, Properties> loaded = new LinkedHashMap<>();
    private String languageCode;

    private SemanticTextManager(Path localeDirectory, String languageCode) {
        this.localeDirectory = localeDirectory == null ? defaultLocaleDirectory() : localeDirectory;
        this.languageCode = normalizeLanguage(languageCode == null || languageCode.isBlank() ? DEFAULT_LANGUAGE : languageCode);
        loadLanguage(DEFAULT_LANGUAGE);
        loadLanguage(this.languageCode);
    }

    static SemanticTextManager createDefault() {
        return new SemanticTextManager(defaultLocaleDirectory(), launcherOrSystemLanguage(""));
    }

    static SemanticTextManager create(Path localeDirectory, String launcherLanguage) {
        return new SemanticTextManager(localeDirectory, launcherOrSystemLanguage(launcherLanguage));
    }

    static String launcherOrSystemLanguage(String launcherLanguage) {
        if (launcherLanguage != null && !launcherLanguage.isBlank()) return normalizeLanguage(launcherLanguage);
        Locale locale = Locale.getDefault();
        String language = locale == null ? DEFAULT_LANGUAGE : locale.getLanguage();
        return language == null || language.isBlank() ? DEFAULT_LANGUAGE : normalizeLanguage(language);
    }

    static Path defaultLocaleDirectory() {
        return Paths.get(LOCALE_DIRECTORY);
    }

    String currentLanguage() {
        return languageCode;
    }

    void setLanguage(String code) {
        String normalized = normalizeLanguage(code);
        if (normalized.isBlank()) normalized = DEFAULT_LANGUAGE;
        languageCode = normalized;
        loadLanguage(languageCode);
    }

    ArrayList<String> availableLanguageCodes() {
        LinkedHashSet<String> codes = new LinkedHashSet<>();
        codes.add(DEFAULT_LANGUAGE);
        if (Files.isDirectory(localeDirectory)) {
            try {
                Files.list(localeDirectory)
                    .filter(p -> p.getFileName().toString().endsWith(".properties"))
                    .map(p -> p.getFileName().toString().replaceFirst("\\.properties$", ""))
                    .map(SemanticTextManager::normalizeLanguage)
                    .filter(s -> !s.isBlank())
                    .sorted()
                    .forEach(codes::add);
            } catch (IOException ignored) {
                // Missing locale folder is tolerated; fallback keys remain usable.
            }
        }
        return new ArrayList<>(codes);
    }

    String text(String semanticKey) {
        if (semanticKey == null || semanticKey.isBlank()) return "";
        String key = semanticKey.trim();
        Properties active = loadLanguage(languageCode);
        String value = active.getProperty(key);
        if (value == null) value = loadLanguage(DEFAULT_LANGUAGE).getProperty(key);
        if (value == null) value = fallbackEnglish().get(key);
        return value == null ? "⟦" + key + "⟧" : value;
    }

    String text(String semanticKey, Object... args) {
        String pattern = text(semanticKey);
        if (args == null || args.length == 0) return pattern;
        return MessageFormat.format(pattern, args);
    }

    boolean hasKey(String semanticKey) {
        if (semanticKey == null || semanticKey.isBlank()) return false;
        String key = semanticKey.trim();
        return loadLanguage(languageCode).containsKey(key) || loadLanguage(DEFAULT_LANGUAGE).containsKey(key) || fallbackEnglish().containsKey(key);
    }

    Map<String, String> fallbackSnapshot() {
        return Collections.unmodifiableMap(fallbackEnglish());
    }

    private Properties loadLanguage(String code) {
        String normalized = normalizeLanguage(code);
        Properties existing = loaded.get(normalized);
        if (existing != null) return existing;
        Properties p = new Properties();
        Path file = localeDirectory.resolve(normalized + ".properties");
        if (Files.isRegularFile(file)) {
            try (InputStream in = Files.newInputStream(file)) {
                p.load(in);
            } catch (IOException ignored) {
                // Fallback text remains authoritative if a locale file is missing or malformed.
            }
        }
        loaded.put(normalized, p);
        return p;
    }

    private static String normalizeLanguage(String code) {
        if (code == null) return DEFAULT_LANGUAGE;
        String c = code.trim().replace('_', '-').toLowerCase(Locale.ROOT);
        int dash = c.indexOf('-');
        if (dash > 0) c = c.substring(0, dash);
        return c.replaceAll("[^a-z]", "");
    }

    private static Map<String, String> fallbackEnglish() {
        LinkedHashMap<String, String> m = new LinkedHashMap<>();
        m.put("ui.main.language.button", "Language");
        m.put("ui.main.language.current", "Language: {0}");
        m.put("ui.main.language.dropdown.title", "Select language");
        m.put("ui.main.language.placeholder", "Translation placeholder loaded.");
        m.put("ui.error.missing_text_key", "Missing text key: {0}");
        m.put("ui.error.unmapped_module", "Unmapped module: {0}");
        m.put("ui.action.back", "Back");
        m.put("ui.action.confirm", "Confirm");
        m.put("ui.action.cancel", "Cancel");
        m.put("ui.menu.new_game", "New Game");
        m.put("ui.menu.load_game", "Load Game");
        m.put("ui.menu.options", "Options");
        m.put("ui.menu.exit", "Exit");
        m.put("ui.trade.stock_available", "Stock available: {0}");
        m.put("ui.trade.no_access", "This trader cannot access that stock.");
        m.put("ui.vending.empty", "The machine is empty.");
        m.put("ui.vending.dispensed", "Dispensed: {0}");
        return m;
    }
}
