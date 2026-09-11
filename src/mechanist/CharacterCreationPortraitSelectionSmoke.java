package mechanist;

import java.awt.event.KeyEvent;

public final class CharacterCreationPortraitSelectionSmoke {
    public static void main(String[] args) {
        if (CharacterCreationPortraitSelectionAuthority.directionForKey(KeyEvent.VK_OPEN_BRACKET) != -1) {
            throw new AssertionError("previous portrait key is not mapped to -1");
        }
        if (CharacterCreationPortraitSelectionAuthority.directionForKey(KeyEvent.VK_CLOSE_BRACKET) != 1) {
            throw new AssertionError("next portrait key is not mapped to +1");
        }
        if (CharacterCreationPortraitSelectionAuthority.directionForKey(KeyEvent.VK_P) != 0) {
            throw new AssertionError("unrelated character key was captured by portrait browsing");
        }
        if (CharacterCreationPortraitSelectionAuthority.shiftedPortraitIndex(41, 1) != 42) {
            throw new AssertionError("portrait forward selection did not advance existing identity");
        }
        if (CharacterCreationPortraitSelectionAuthority.shiftedPortraitIndex(41, -1) != 40) {
            throw new AssertionError("portrait backward selection did not preserve existing identity ordering");
        }
        if (CharacterCreationPortraitSelectionAuthority.shiftedPortraitIndex(0, -1) != Integer.MAX_VALUE) {
            throw new AssertionError("portrait backward selection did not wrap without producing a negative identity");
        }
        if (CharacterCreationPortraitSelectionAuthority.shiftedPortraitIndex(Integer.MAX_VALUE, 1) != 0) {
            throw new AssertionError("portrait forward selection did not wrap safely");
        }
        System.out.println("CharacterCreationPortraitSelectionSmoke PASS authority="
                + CharacterCreationPortraitSelectionAuthority.VERSION + " keys=[/] persistentIndex=true");
    }

    private CharacterCreationPortraitSelectionSmoke() {}
}
