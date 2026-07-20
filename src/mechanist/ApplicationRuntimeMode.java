package mechanist;

/**
 * Stable runtime entry modes shared by command-line startup, launcher handoff,
 * desktop execution, and verification batches.
 */
enum ApplicationRuntimeMode {
    LAUNCHER,
    CLIENT,
    REMOTE_CLIENT,
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
