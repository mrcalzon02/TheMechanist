package mechanist;

final class PlayerFacingActionTextSmoke {
    public static void main(String[] args) {
        String inventory = PlayerFacingActionText.inventory(
                "Transferred items",
                "registryKey=debug.item uuid=123e4567-e89b-12d3-a456-426614174000"
        );

        if (inventory.contains("registryKey") || inventory.contains("123e4567")) {
            throw new AssertionError("Inventory action leaked implementation residue: " + inventory);
        }

        if (!inventory.contains("catalog")) {
            throw new AssertionError("Inventory action did not remap registry wording: " + inventory);
        }

        String travel = PlayerFacingActionText.travel(
                "Entered sector",
                "targetZoneKey=7,7,1,2,3,false path=/srv/mechanist/routes/debug.txt"
        );

        if (travel.contains("7,7,1,2,3,false") || travel.contains("/srv/mechanist/routes/debug.txt")) {
            throw new AssertionError("Travel action leaked implementation residue: " + travel);
        }

        String fallback = PlayerFacingActionText.trade("", "");
        if (!fallback.equals("Trade completed.")) {
            throw new AssertionError("Trade fallback wording changed unexpectedly: " + fallback);
        }
    }
}
