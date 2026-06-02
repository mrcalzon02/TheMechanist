package mechanist;

final class PlayerFacingDenialTextSmoke {
    public static void main(String[] args) {
        String movement = PlayerFacingDenialText.movement(
                "targetZoneKey=9,9,4,1,0,false blocked by mechanist.runtime.PathNode path=C:\\\\debug\\\\path.txt"
        );

        if (movement.contains("9,9,4,1,0,false")) {
            throw new AssertionError("Movement denial leaked zone key: " + movement);
        }
        if (movement.contains("mechanist.runtime.PathNode")) {
            throw new AssertionError("Movement denial leaked runtime class name: " + movement);
        }
        if (movement.contains("C:\\\\debug\\\\path.txt")) {
            throw new AssertionError("Movement denial leaked filesystem path: " + movement);
        }
        if (!movement.startsWith("Movement unavailable:")) {
            throw new AssertionError("Movement denial lost readable prefix: " + movement);
        }

        String inventory = PlayerFacingDenialText.inventory(
                "registryKey=item.debug.raw missing id=AB-991244"
        );

        if (inventory.contains("registryKey") || inventory.contains("AB-991244")) {
            throw new AssertionError("Inventory denial leaked implementation residue: " + inventory);
        }
        if (!inventory.contains("catalog")) {
            throw new AssertionError("Inventory denial did not remap registry wording: " + inventory);
        }

        String fallback = PlayerFacingDenialText.trade(null);
        if (!fallback.equals("That trade cannot be completed right now.")) {
            throw new AssertionError("Trade fallback wording changed unexpectedly: " + fallback);
        }
    }
}
