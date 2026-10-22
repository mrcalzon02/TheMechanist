package mechanist;

import java.awt.Rectangle;

/** Headless geometry test for early world and character creation stages. */
final class WorldStartLayoutAuthoritySmoke {
    private WorldStartLayoutAuthoritySmoke() {}
    private static void check(boolean yes, String what) {
        if (!yes) throw new AssertionError(what);
    }
    private static void within(Rectangle inner, Rectangle outer, String what) {
        check(inner.width > 0 && inner.height > 0
                && inner.x >= outer.x && inner.y >= outer.y
                && inner.x + inner.width <= outer.x + outer.width
                && inner.y + inner.height <= outer.y + outer.height,
                what + ": " + inner + " outside " + outer);
    }

    public static void main(String[] args) {
        for (int[] size : new int[][] {
                {1920, 1080}, {1280, 720}, {1024, 768}, {800, 600}, {760, 560}
        }) {
            Rectangle viewport = new Rectangle(0, 0, size[0], size[1]);
            for (boolean character : new boolean[] {false, true}) {
                Rectangle panel = WorldStartLayoutAuthority.panelRect(size[0], size[1], character);
                within(panel, viewport, "stage panel");
                Rectangle footer = WorldStartLayoutAuthority.footer(panel);
                within(footer, panel, "footer");
                Rectangle actions = new Rectangle(panel.x + 24, panel.y + panel.height - 58,
                        panel.width - 48, 34);
                check(!footer.intersects(actions), "footer/action overlap");
                if (character) {
                    Rectangle body = WorldStartLayoutAuthority.characterBody(panel);
                    within(body, panel, "character body");
                    check(!body.intersects(footer), "character body/footer overlap");
                    int rosterW = Math.max(128, Math.min(180, body.width / 5));
                    int rightW = Math.max(240, Math.min(380, body.width / 3));
                    int midW = body.width - rosterW - rightW - 28;
                    if (midW < 300) rightW = Math.max(240, rightW - (300 - midW));
                    int sheetW = body.width - rosterW - rightW - 28;
                    check(sheetW > 190, "character sheet must be readable");
                    Rectangle sheet = new Rectangle(body.x + rosterW + 14, body.y, sheetW, body.height);
                    within(sheet, body, "character sheet");
                    Rectangle dossier = new Rectangle(body.x + body.width - rightW,
                            body.y, rightW, body.height);
                    within(dossier, body, "character dossier");
                    check(!sheet.intersects(dossier), "character sheet/dossier overlap");
                    int portrait = Math.max(72, Math.min(128, Math.min(sheet.width - 24, sheet.height / 4)));
                    Rectangle portraitRect = new Rectangle(sheet.x + 12, sheet.y + 34, portrait, portrait);
                    within(portraitRect, sheet, "portrait");
                    int nameY = portraitRect.y + 22;
                    int metaY = nameY + 50;
                    int statsY = Math.max(portraitRect.y + portraitRect.height + 16, metaY + 38);
                    int statsH = Math.max(1, Math.min(256, sheet.y + sheet.height - statsY - 8));
                    Rectangle stats = new Rectangle(sheet.x + 12, statsY, sheet.width - 24, statsH);
                    within(stats, sheet, "stats panel");
                    for (Rectangle cell : WorldStartLayoutAuthority.statCells(stats, 12))
                        within(cell, stats, "stat cell");
                } else {
                    within(WorldStartLayoutAuthority.pickerPreview(panel), panel, "picker preview");
                    within(WorldStartLayoutAuthority.seed(panel), panel, "seed editor");
                    Rectangle preview = WorldStartLayoutAuthority.generationPreview(panel);
                    within(preview, panel, "generation preview");
                    check(!preview.intersects(footer), "preview/footer overlap");
                    for (Rectangle row : WorldStartLayoutAuthority.generationRows(panel)) {
                        within(row, panel, "generation setting");
                        check(!row.intersects(preview), "setting intersects preview");
                        check(!row.intersects(footer), "setting/footer overlap");
                    }
                }
            }
        }
        check(!WorldStartLayoutAuthority.usable(640, 480), "very small window needs visible fallback");
        check(WorldStartLayoutAuthority.usable(800, 600), "800x600 must remain usable");
        System.out.println("PASS: early UI viewport containment and character stats geometry");
    }
}
