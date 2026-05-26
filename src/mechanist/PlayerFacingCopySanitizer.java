package mechanist;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Shared Gate 3 helper for ordinary player-facing copy.
 *
 * <p>This class does not decide game truth. It only prepares strings that are already about to be
 * rendered to normal UI surfaces so diagnostics, save keys, package paths, Java class names, and
 * other implementation residue do not leak into player comprehension panels.</p>
 */
final class PlayerFacingCopySanitizer {
    private static final Pattern RAW_ZONE_KEY = Pattern.compile("\\b-?\\d+,-?\\d+,-?\\d+,-?\\d+,-?\\d+,(?:true|false)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern UUID_TOKEN = Pattern.compile("\\b[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern INTERNAL_KEY_VALUE = Pattern.compile("\\b(?:id|uuid|guid|targetZoneKey|registryKey|assetPath|atlasKey|manifestKey|packageName|className)\\s*[=:]\\s*[^\\s,;)]+", Pattern.CASE_INSENSITIVE);
    private static final Pattern WINDOWS_PATH = Pattern.compile("\\b[A-Za-z]:\\\\[^\\r\\n\\t ]+");
    private static final Pattern UNIX_ABSOLUTE_PATH = Pattern.compile("(?<!:)\\B/(?:[^\\s/]+/)+[^\\s]+");
    private static final Pattern JAVA_QUALIFIED_NAME = Pattern.compile("\\b(?:mechanist|java|javax|com|org)(?:\\.[A-Za-z_$][A-Za-z0-9_$]*){2,}\\b");
    private static final Pattern INTERNAL_CONTRACT_ID = Pattern.compile("\\b[A-Z]{1,4}-\\d{4,}\\b");
    private static final Pattern GENERATED_IDENT_CHIP = Pattern.compile("(?i)\\bident\\s+chip\\s+[A-Z]{1,4}-\\d{4,}\\b");
    private static final Pattern WHITESPACE = Pattern.compile("[ \\t]{2,}");

    private PlayerFacingCopySanitizer() { }

    /**
     * Returns a copy suitable for normal player UI. Developer diagnostics should keep the original
     * text in logs/audit surfaces before calling this method.
     */
    static String forOrdinaryPlayer(String text) {
        if (text == null || text.isBlank()) return "No readable details are available yet.";
        String cleaned = Objects.toString(text, "");
        cleaned = GENERATED_IDENT_CHIP.matcher(cleaned).replaceAll("the target's ident chip");
        cleaned = RAW_ZONE_KEY.matcher(cleaned).replaceAll("the marked route");
        cleaned = INTERNAL_KEY_VALUE.matcher(cleaned).replaceAll("internal record");
        cleaned = UUID_TOKEN.matcher(cleaned).replaceAll("internal record");
        cleaned = WINDOWS_PATH.matcher(cleaned).replaceAll("diagnostic details");
        cleaned = UNIX_ABSOLUTE_PATH.matcher(cleaned).replaceAll("diagnostic details");
        cleaned = JAVA_QUALIFIED_NAME.matcher(cleaned).replaceAll("runtime service");
        cleaned = INTERNAL_CONTRACT_ID.matcher(cleaned).replaceAll("internal record");
        cleaned = cleaned.replace("DebugLog", "diagnostics");
        cleaned = cleaned.replace("debug log", "diagnostic record");
        cleaned = cleaned.replace("log file", "diagnostic record");
        cleaned = cleaned.replace("registry", "catalog");
        cleaned = cleaned.replace("Registry", "Catalog");
        cleaned = WHITESPACE.matcher(cleaned).replaceAll(" ").trim();
        if (cleaned.isBlank()) return "No readable details are available yet.";
        return cleaned;
    }

    /** Returns true when the text still contains known implementation residue after sanitization. */
    static boolean containsLikelyPlayerFacingLeak(String text) {
        if (text == null || text.isBlank()) return false;
        return RAW_ZONE_KEY.matcher(text).find()
                || UUID_TOKEN.matcher(text).find()
                || INTERNAL_KEY_VALUE.matcher(text).find()
                || WINDOWS_PATH.matcher(text).find()
                || UNIX_ABSOLUTE_PATH.matcher(text).find()
                || JAVA_QUALIFIED_NAME.matcher(text).find()
                || GENERATED_IDENT_CHIP.matcher(text).find();
    }
}
