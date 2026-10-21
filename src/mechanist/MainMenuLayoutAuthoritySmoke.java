package mechanist;

import java.awt.Rectangle;

/** The same bounds are used by main-menu paint and pointer/keyboard route selection. */
final class MainMenuLayoutAuthoritySmoke {
    private MainMenuLayoutAuthoritySmoke() {}
    private static void check(boolean result, String message) {
        if (!result) throw new AssertionError(message);
    }
    public static void main(String[] args) {
        for (int[] viewport : new int[][] {
                {1920, 1080}, {1280, 720}, {1024, 768},
                {800, 600}, {760, 560}, {640, 480}
        }) {
            Rectangle frame = MainMenuLayoutAuthority.frame(viewport[0], viewport[1], 8);
            check(frame.x >= 0 && frame.y >= 0
                    && frame.x + frame.width <= viewport[0]
                    && frame.y + frame.height <= viewport[1],
                    "main menu frame escaped viewport " + frame);
            Rectangle previous = null;
            for (int i = 0; i < 8; i++) {
                Rectangle row = MainMenuLayoutAuthority.route(frame, 8, i);
                check(row.width > 0 && row.height > 0
                        && row.x >= frame.x && row.y >= frame.y
                        && row.x + row.width <= frame.x + frame.width
                        && row.y + row.height <= frame.y + frame.height,
                        "main menu route " + i + " escaped frame at " + viewport[0] + "x" + viewport[1]);
                if (previous != null) check(!row.intersects(previous), "main menu routes overlap");
                int iconSize = MainMenuSurfacePainter.routeIconSize(row);
                check(iconSize > 0 && iconSize <= row.height,
                        "main menu icon taller than its clickable route at " + viewport[0] + "x" + viewport[1]);
                Rectangle icon = new Rectangle(row.x + 12,
                        row.y + (row.height - iconSize) / 2, iconSize, iconSize);
                check(row.contains(icon), "main menu icon escaped its button at "
                        + viewport[0] + "x" + viewport[1] + ": " + icon);
                if (previous != null) check(!previous.intersects(icon),
                        "main menu route icon visually overlapped the preceding button");
                previous = row;
            }
        }
        System.out.println("PASS: main menu routes contained from 640x480 through 1920x1080");
    }
}
