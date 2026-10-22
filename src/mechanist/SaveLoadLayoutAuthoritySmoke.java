package mechanist;

import java.awt.Rectangle;

final class SaveLoadLayoutAuthoritySmoke {
    private SaveLoadLayoutAuthoritySmoke() { }
    private static void within(Rectangle child, Rectangle parent) {
        if (child.x < parent.x || child.y < parent.y || child.width <= 0 || child.height <= 0
                || child.x + child.width > parent.x + parent.width
                || child.y + child.height > parent.y + parent.height) {
            throw new AssertionError(child + " outside " + parent);
        }
    }
    private static void check(boolean ok, String why) {
        if (!ok) throw new AssertionError(why);
    }
    public static void main(String[] args) {
        for (int[] size : new int[][] {{1920,1080}, {1280,720}, {800,600}, {640,480}, {480,500}, {320,240}}) {
            var g = SaveLoadLayoutAuthority.measure(size[0],size[1],5);
            within(g.panel(),new Rectangle(0,0,size[0],size[1]));
            within(g.back(),g.panel());
            if (g.fits()) {
                within(g.status(),g.panel());
                within(g.footer(),g.panel());
                within(g.delete(),g.panel());
                within(g.newGame(),g.panel());
                for (int i=0;i<5;i++) {
                    within(g.slot(i),g.panel());
                    check(!g.slot(i).intersects(g.footer()), "slots overlap instructions");
                    check(!g.slot(i).intersects(g.back()), "slots overlap Back");
                    check(!g.slot(i).intersects(g.delete()), "slots overlap Delete");
                    check(!g.slot(i).intersects(g.newGame()), "slots overlap New Game");
                }
                check(!g.footer().intersects(g.back()), "footer overlaps actions");
                check(!g.footer().intersects(g.newGame()), "footer overlaps new game");
                check(!g.back().intersects(g.delete()), "actions overlap");
                check(!g.newGame().intersects(g.delete()), "new game overlaps Delete");
            } else {
                check(g.slot(0).isEmpty() && g.delete().isEmpty() && g.newGame().isEmpty(),
                    "too-small view should expose only a bounded Back action");
            }
        }
        var big = SaveLoadLayoutAuthority.measure(1920,1080,5);
        check(big.panel().height < 500, "avoid old fixed 560px save dialog");
        check(!SaveLoadLayoutAuthority.measure(320,240,5).fits(), "tiny viewport must fail closed");
        System.out.println("PASS: compact save/load window, aligned slots and commands, safe fallback");
    }
}
