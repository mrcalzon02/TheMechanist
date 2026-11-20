package mechanist;

import java.awt.Rectangle;

/** Pure geometry smoke; no game window, Java2D context, or assets required. */
final class OptionsLayoutAuthoritySmoke {
    private OptionsLayoutAuthoritySmoke() { }

    static void assertInside(Rectangle r, Rectangle outer, String label) {
        if (r.width < 0 || r.height < 0 || r.x < outer.x || r.y < outer.y
                || r.x + r.width > outer.x + outer.width
                || r.y + r.height > outer.y + outer.height) {
            throw new AssertionError(label + ": " + r + " outside " + outer);
        }
    }

    static void check(boolean result, String label) {
        if (!result) throw new AssertionError(label);
    }

    public static void main(String[] args) {
        for (int[] viewport : new int[][] {{1920,1080}, {1280,720}, {800,600}, {640,480}, {320,240}}) {
            for (int[] tab : new int[][] {{5,5}, {9,5}, {22,18}}) {
                var g = OptionsLayoutAuthority.measure(viewport[0], viewport[1], tab[0], tab[1]);
                var screen = new Rectangle(0, 0, viewport[0], viewport[1]);
                assertInside(g.panel(), screen, "options panel");
                assertInside(g.controls(), g.panel(), "controls");
                if (g.details().height > 0) assertInside(g.details(), g.panel(), "details");
                int tabRows = (OptionsLayoutAuthority.TAB_COUNT + g.tabColumns() - 1) / g.tabColumns();
                int commandRows = (tab[0] + g.commandColumns() - 1) / g.commandColumns();
                int required = OptionsLayoutAuthority.PAD * 3
                        + tabRows * OptionsLayoutAuthority.TAB_HEIGHT + (tabRows - 1) * OptionsLayoutAuthority.GAP
                        + commandRows * OptionsLayoutAuthority.COMMAND_HEIGHT + (commandRows - 1) * OptionsLayoutAuthority.GAP;
                if (g.commandsFit()) check(g.controls().height >= required, "commands should fit: " + viewport[0]);
                else check(g.details().height == 0, "too-small view must not reserve a details pane");
                var firstPage = OptionsLayoutAuthority.commandPage(g, tab[0], 0);
                if (g.commandsFit()) {
                    check(firstPage.pages() == 1 && firstPage.first() == 0
                            && firstPage.end() == tab[0], "fitting controls must remain in one page");
                } else if (!firstPage.unavailable()) {
                    int last = 0;
                    for (int index = 0; index < firstPage.pages(); index++) {
                        var page = OptionsLayoutAuthority.commandPage(g, tab[0], index);
                        check(page.first() == last && page.end() > last, "pages must be gap-free");
                        int y = g.controls().y + OptionsLayoutAuthority.commandTop(g)
                                + page.navRow() * (OptionsLayoutAuthority.COMMAND_HEIGHT + OptionsLayoutAuthority.GAP);
                        check(y + OptionsLayoutAuthority.COMMAND_HEIGHT
                                <= g.controls().y + g.controls().height - OptionsLayoutAuthority.PAD,
                                "Prev/Next must stay within the controls");
                        last = page.end();
                    }
                    check(last == tab[0], "every command must be reachable by paging");
                }
            }
        }
        var display = OptionsLayoutAuthority.measure(1920,1080,5,5);
        check(display.panel().height <= 350, "short tabs should not reserve a full-height page");
        check(display.panel().width <= 1100, "short tabs should not stretch indefinitely");
        check(OptionsLayoutAuthority.measure(800,600,22,18).commandsFit(), "ordinary small window keeps all actions");
        var narrow = OptionsLayoutAuthority.measure(320,240,22,18);
        check(!narrow.commandsFit(), "small size needs paging");
        check(OptionsLayoutAuthority.commandPage(narrow,22,0).pages() == 11,
                "320x240 provides two commands plus a navigation row");
        check(OptionsLayoutAuthority.commandPage(narrow,22,999).end() == 22,
                "page index clamps to final command");
        check(OptionsLayoutAuthority.commandPage(narrow,22,-5).first() == 0,
                "page index clamps to first command");
        System.out.println("PASS: compact options layout, viewport bounds, all commands paged");
    }
}
