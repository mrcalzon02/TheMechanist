package mechanist;

final class PlayerFacingMenuOptionTextSmoke {
    public static void main(String[] args) {
        String option = PlayerFacingMenuOptionText.option(
                "registryKey=debug.option",
                "targetZoneKey=8,8,8,8,false path=/srv/mechanist/menu.txt Compact option validation text.",
                false,
                26
        );

        if (option.contains("registryKey")
                || option.contains("8,8,8,8,false")
                || option.contains("/srv/mechanist/menu.txt")) {
            throw new AssertionError("Menu option leaked implementation residue: " + option);
        }

        if (!option.startsWith("[Unavailable]")) {
            throw new AssertionError("Unavailable option prefix missing: " + option);
        }

        String enabled = PlayerFacingMenuOptionText.option("Travel", "", true, 20);
        if (!enabled.equals("[Available] Travel")) {
            throw new AssertionError("Enabled option formatting changed unexpectedly: " + enabled);
        }
    }
}
