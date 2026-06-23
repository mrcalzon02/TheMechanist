package mechanist;

/** Smoke for the boot-menu music delay without launching the audio client. */
final class BootMenuMusicDelaySmoke {
    public static void main(String[] args) {
        require(BootMenuFlowAuthority.MIN_BOOT_MILLIS == 9000L, "boot menu should hold for nine seconds");
        require(BootMenuFlowAuthority.MAIN_MENU_MUSIC_DELAY_MILLIS == 9000L,
                "main menu music should be gated for nine seconds");
        require(!BootMenuFlowAuthority.mainMenuMusicAllowed(8_999L, 9_000L),
                "music should remain delayed before the gate");
        require(BootMenuFlowAuthority.mainMenuMusicAllowed(9_000L, 9_000L),
                "music should be allowed at the gate");
        require(BootMenuFlowAuthority.auditSummary().contains("studio-intro+logo-splash"),
                "audit should name placeholder splash contract");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private BootMenuMusicDelaySmoke() { }
}
