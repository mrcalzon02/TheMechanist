package mechanist;

final class PlayerFacingListRowTextSmoke {
    public static void main(String[] args) {
        String row = PlayerFacingListRowText.row(
                "registryKey=debug.contract",
                "targetZoneKey=6,6,6,6,false path=/srv/mechanist/list.txt Compact row validation text.",
                24
        );

        if (row.contains("registryKey")
                || row.contains("6,6,6,6,false")
                || row.contains("/srv/mechanist/list.txt")) {
            throw new AssertionError("List row leaked implementation residue: " + row);
        }

        String[] lines = row.split("\\n");
        for (String line : lines) {
            if (line.length() > 80) {
                throw new AssertionError("List row formatting expanded unexpectedly: " + line);
            }
        }

        String primaryOnly = PlayerFacingListRowText.row("Inventory", "", 20);
        if (!primaryOnly.equals("Inventory")) {
            throw new AssertionError("Primary-only row formatting changed unexpectedly: " + primaryOnly);
        }
    }
}
