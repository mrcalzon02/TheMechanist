package mechanist;

import java.awt.Point;
import java.awt.Rectangle;
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

        Rectangle sheet = new Rectangle(100, 80, 420, 540);
        Rectangle portrait = CharacterCreationPortraitSelectionAuthority.portraitRectForSheet(sheet);
        Rectangle previous = CharacterCreationPortraitSelectionAuthority.previousControlRect(portrait);
        Rectangle next = CharacterCreationPortraitSelectionAuthority.nextControlRect(portrait);
        if (!portrait.contains(previous) || !portrait.contains(next)) {
            throw new AssertionError("visible portrait controls escaped the portrait frame");
        }
        if (previous.intersects(next)) {
            throw new AssertionError("visible portrait controls overlap each other");
        }
        if (previous.getCenterX() >= portrait.getCenterX() || next.getCenterX() <= portrait.getCenterX()) {
            throw new AssertionError("portrait controls are not clearly delineated left/right");
        }
        if (CharacterCreationPortraitSelectionAuthority.portraitControlDirection(portrait,
                new Point((int) previous.getCenterX(), (int) previous.getCenterY())) != -1) {
            throw new AssertionError("previous portrait mouse control does not resolve to -1");
        }
        if (CharacterCreationPortraitSelectionAuthority.portraitControlDirection(portrait,
                new Point((int) next.getCenterX(), (int) next.getCenterY())) != 1) {
            throw new AssertionError("next portrait mouse control does not resolve to +1");
        }
        if (CharacterCreationPortraitSelectionAuthority.portraitControlDirection(portrait,
                new Point((int) portrait.getCenterX(), (int) portrait.getCenterY())) != 0) {
            throw new AssertionError("portrait image body was incorrectly turned into a mouse control");
        }

        System.out.println("CharacterCreationPortraitSelectionSmoke PASS authority="
                + CharacterCreationPortraitSelectionAuthority.VERSION
                + " keys=[/] persistentIndex=true visibleControls=true");
    }

    private CharacterCreationPortraitSelectionSmoke() {}
}
