package mechanist;

/**
 * Current runtime entry modes understood by the desktop shell.
 *
 * These identifiers are intentionally small and stable so command-line startup,
 * future launcher handoff, and test batches can agree on the same vocabulary.
 */
enum ApplicationRuntimeMode {
    LAUNCHER,
    CLIENT,
    LOCAL_SERVER,
    DEDICATED_SERVER,
    EDITOR,
    TEST_BATCH;

    static ApplicationRuntimeMode parse(String raw) {
        if (raw == null || raw.isBlank()) return CLIENT;
        String key = raw.trim().toUpperCase(java.util.Locale.ROOT).replace('-', '_');
        for (ApplicationRuntimeMode mode : values()) {
            if (mode.name().equals(key)) return mode;
        }
        return CLIENT;
    }
}
