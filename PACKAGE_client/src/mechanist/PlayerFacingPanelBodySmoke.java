package mechanist;

final class PlayerFacingPanelBodySmoke {
    public static void main(String[] args) {
        String panel = PlayerFacingPanelBody.formatWithTitle(
                "registryKey=debug.panel",
                "targetZoneKey=4,4,4,4,false path=/srv/mechanist/panel.txt This is a long ordinary readable body intended for wrapping verification.",
                28
        );

        if (panel.contains("registryKey")
                || panel.contains("4,4,4,4,false")
                || panel.contains("/srv/mechanist/panel.txt")) {
            throw new AssertionError("Panel body leaked implementation residue: " + panel);
        }

        String[] lines = panel.split("\\n");
        for (int i = 1; i < lines.length; i++) {
            if (lines[i].length() > 28) {
                throw new AssertionError("Panel body line exceeded wrap width: " + lines[i]);
            }
        }

        String titleOnly = PlayerFacingPanelBody.formatWithTitle("Inventory", "", 24);
        if (!titleOnly.equals("Inventory")) {
            throw new AssertionError("Title-only panel formatting changed unexpectedly: " + titleOnly);
        }
    }
}
