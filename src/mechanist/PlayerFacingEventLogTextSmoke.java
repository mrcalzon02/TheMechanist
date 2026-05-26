package mechanist;

final class PlayerFacingEventLogTextSmoke {
    public static void main(String[] args) {
        String event = PlayerFacingEventLogText.event(
                "registryKey=debug.log",
                "targetZoneKey=9,9,9,9,false path=/srv/mechanist/log.txt Ordinary readable event body for validation.",
                30
        );

        if (event.contains("registryKey")
                || event.contains("9,9,9,9,false")
                || event.contains("/srv/mechanist/log.txt")) {
            throw new AssertionError("Event log leaked implementation residue: " + event);
        }

        String[] lines = event.split("\\n");
        for (int i = 1; i < lines.length; i++) {
            if (lines[i].length() > 30) {
                throw new AssertionError("Event log line exceeded wrap width: " + lines[i]);
            }
        }

        String categoryOnly = PlayerFacingEventLogText.event("Travel", "", 24);
        if (!categoryOnly.equals("Travel")) {
            throw new AssertionError("Category-only event formatting changed unexpectedly: " + categoryOnly);
        }
    }
}
