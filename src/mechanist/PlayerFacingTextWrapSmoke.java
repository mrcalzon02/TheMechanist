package mechanist;

import java.util.List;

final class PlayerFacingTextWrapSmoke {
    public static void main(String[] args) {
        List<String> wrapped = PlayerFacingTextWrap.wrap(
                "registryKey=debug.item targetZoneKey=5,5,5,5,false path=/srv/debug/world.txt ordinary readable sentence for wrapping validation",
                32
        );

        if (wrapped.isEmpty()) {
            throw new AssertionError("Wrapped output was empty.");
        }

        for (String line : wrapped) {
            if (line.length() > 32) {
                throw new AssertionError("Wrapped line exceeded max width: " + line);
            }

            if (line.contains("registryKey") || line.contains("5,5,5,5,false") || line.contains("/srv/debug/world.txt")) {
                throw new AssertionError("Wrapped line leaked implementation residue: " + line);
            }
        }

        List<String> minimum = PlayerFacingTextWrap.wrap("short text", 1);
        for (String line : minimum) {
            if (line.length() > 12) {
                throw new AssertionError("Minimum wrap width enforcement failed: " + line);
            }
        }
    }
}
