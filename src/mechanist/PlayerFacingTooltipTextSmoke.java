package mechanist;

final class PlayerFacingTooltipTextSmoke {
    public static void main(String[] args) {
        String tooltip = PlayerFacingTooltipText.tooltip(
                "registryKey=debug.tooltip",
                "targetZoneKey=2,2,2,2,false path=/srv/mechanist/tooltip.txt Compact tooltip validation text for wrapping.",
                26
        );

        if (tooltip.contains("registryKey")
                || tooltip.contains("2,2,2,2,false")
                || tooltip.contains("/srv/mechanist/tooltip.txt")) {
            throw new AssertionError("Tooltip leaked implementation residue: " + tooltip);
        }

        String[] lines = tooltip.split("\\n");
        for (int i = 1; i < lines.length; i++) {
            if (lines[i].length() > 26) {
                throw new AssertionError("Tooltip line exceeded wrap width: " + lines[i]);
            }
        }

        String titleOnly = PlayerFacingTooltipText.tooltip("Inventory", "", 20);
        if (!titleOnly.equals("Inventory")) {
            throw new AssertionError("Tooltip title-only formatting changed unexpectedly: " + titleOnly);
        }
    }
}
