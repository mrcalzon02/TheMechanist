package mechanist;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Player-character body and equipment presentation authority.
 *
 * The previous character screen exposed limb hit points and equipment slots.
 * This authority restores that information without recreating a second body or
 * inventory state: Candidate.body and the live equipment fields remain the
 * source of truth.
 */
final class CharacterPaperDollAuthority {
    static final String VERSION = "character-paper-doll-0.1";

    enum EquipmentSlot {
        LEFT_HAND("Left Hand"),
        RIGHT_HAND("Right Hand"),
        BODY("Body Protection");

        private final String label;

        EquipmentSlot(String label) {
            this.label = label;
        }

        String label() {
            return label;
        }

        static EquipmentSlot at(int index) {
            EquipmentSlot[] values = values();
            return values[Math.max(0, Math.min(index, values.length - 1))];
        }
    }

    record RegionView(String bodyPartName, String shortLabel, Rectangle bounds,
                      double healthRatio, int currentHealth, int maximumHealth,
                      String status, boolean destroyed) {
        String readout() {
            return shortLabel + " " + currentHealth + "/" + maximumHealth + " — " + status;
        }
    }

    record EquipmentView(EquipmentSlot slot, String itemName, boolean empty) {
        String rowLabel(boolean selected) {
            return (selected ? "> " : "  ") + slot.label() + ": " + itemName;
        }
    }

    private record Template(String id, String label, double x, double y, double width, double height) {}

    private static final List<Template> TEMPLATES = List.of(
            new Template("head", "Head", 0.41, 0.02, 0.18, 0.11),
            new Template("chest", "Chest", 0.35, 0.15, 0.30, 0.18),
            new Template("abdomen", "Abdomen", 0.38, 0.34, 0.24, 0.13),
            new Template("pelvis", "Pelvis", 0.39, 0.48, 0.22, 0.10),
            new Template("l upper arm", "L Upper Arm", 0.20, 0.17, 0.13, 0.18),
            new Template("l lower arm", "L Lower Arm", 0.12, 0.36, 0.13, 0.18),
            new Template("l hand", "L Hand", 0.08, 0.55, 0.13, 0.10),
            new Template("r upper arm", "R Upper Arm", 0.67, 0.17, 0.13, 0.18),
            new Template("r lower arm", "R Lower Arm", 0.75, 0.36, 0.13, 0.18),
            new Template("r hand", "R Hand", 0.79, 0.55, 0.13, 0.10),
            new Template("l upper leg", "L Upper Leg", 0.34, 0.59, 0.14, 0.17),
            new Template("l lower leg", "L Lower Leg", 0.31, 0.77, 0.14, 0.15),
            new Template("l foot", "L Foot", 0.25, 0.93, 0.20, 0.06),
            new Template("r upper leg", "R Upper Leg", 0.52, 0.59, 0.14, 0.17),
            new Template("r lower leg", "R Lower Leg", 0.55, 0.77, 0.14, 0.15),
            new Template("r foot", "R Foot", 0.55, 0.93, 0.20, 0.06)
    );

    private CharacterPaperDollAuthority() {}

    static List<RegionView> regions(Candidate candidate, Rectangle bounds) {
        if (candidate == null || candidate.body == null || candidate.body.isEmpty() || bounds == null) return List.of();
        LinkedHashMap<String, BodyPart> unmatched = new LinkedHashMap<>();
        for (BodyPart part : candidate.body.values()) {
            if (part != null) unmatched.put(normalize(part.name), part);
        }

        ArrayList<RegionView> result = new ArrayList<>();
        for (Template template : TEMPLATES) {
            BodyPart part = removeBestMatch(unmatched, template.id());
            if (part == null) continue;
            result.add(view(part, template.label(), scale(bounds, template)));
        }

        // Preserve unusual body plans instead of silently hiding their tracked regions.
        if (!unmatched.isEmpty()) {
            int index = 0;
            int columns = 2;
            int cellWidth = Math.max(36, bounds.width / columns);
            int cellHeight = Math.max(18, Math.min(30, bounds.height / Math.max(1, (unmatched.size() + 1) / 2)));
            for (BodyPart part : unmatched.values()) {
                int col = index % columns;
                int row = index / columns;
                Rectangle r = new Rectangle(bounds.x + col * cellWidth,
                        Math.max(bounds.y, bounds.y + bounds.height - (row + 1) * cellHeight),
                        Math.max(20, cellWidth - 3), Math.max(14, cellHeight - 3));
                result.add(view(part, compactLabel(part.name), r));
                index++;
            }
        }
        return List.copyOf(result);
    }

    static List<String> regionReadouts(Candidate candidate, Rectangle bounds) {
        ArrayList<String> lines = new ArrayList<>();
        for (RegionView view : regions(candidate, bounds)) lines.add(view.readout());
        return List.copyOf(lines);
    }

    static List<EquipmentView> equipment(String leftHand, String rightHand, Clothing clothing) {
        return List.of(
                equipment(EquipmentSlot.LEFT_HAND, leftHand, "Empty"),
                equipment(EquipmentSlot.RIGHT_HAND, rightHand, "Empty"),
                equipment(EquipmentSlot.BODY, clothing == null ? null : clothing.name, "Unarmored")
        );
    }

    static EquipmentView selectedEquipment(int selectedIndex, String leftHand, String rightHand, Clothing clothing) {
        return equipment(leftHand, rightHand, clothing).get(EquipmentSlot.at(selectedIndex).ordinal());
    }

    static void paint(Graphics2D g, Rectangle bounds, Candidate candidate, Font font) {
        paint(g, bounds, candidate, font, null);
    }

    static void paint(Graphics2D g, Rectangle bounds, Candidate candidate, Font font, String selectedBodyPart) {
        if (g == null || bounds == null) return;
        g.setColor(new Color(8, 10, 10, 230));
        g.fillRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, 8, 8);
        g.setColor(new Color(145, 118, 64, 180));
        g.drawRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, 8, 8);

        Font useFont = font == null ? new Font("Monospaced", Font.PLAIN, 11) : font;
        g.setFont(useFont.deriveFont(Font.BOLD, Math.max(10f, useFont.getSize2D())));
        g.setColor(new Color(225, 205, 140));
        g.drawString("BODY CONDITION", bounds.x + 10, bounds.y + 20);

        Rectangle doll = new Rectangle(bounds.x + 8, bounds.y + 28,
                Math.max(80, bounds.width - 16), Math.max(120, bounds.height - 36));
        List<RegionView> regions = regions(candidate, doll);
        if (regions.isEmpty()) {
            g.setColor(new Color(170, 170, 155));
            g.drawString("No tracked body regions.", doll.x + 8, doll.y + 20);
            return;
        }

        for (RegionView region : regions) {
            Rectangle r = region.bounds();
            g.setColor(colorFor(region.healthRatio(), region.destroyed()));
            g.fillRoundRect(r.x, r.y, r.width, r.height, 5, 5);
            g.setColor(new Color(235, 225, 188, 190));
            g.setStroke(new BasicStroke(region.destroyed() ? 2.2f : 1.0f));
            g.drawRoundRect(r.x, r.y, r.width, r.height, 5, 5);
            drawCentered(g, r, region.currentHealth() + "/" + region.maximumHealth());
        }
        g.setStroke(new BasicStroke(1.0f));
    }

    static String statusFor(double ratio, boolean destroyed) {
        if (destroyed || ratio <= 0.0) return "Disabled";
        if (ratio < 0.25) return "Critical";
        if (ratio < 0.50) return "Severely injured";
        if (ratio < 0.75) return "Injured";
        if (ratio < 0.95) return "Hurt";
        return "Healthy";
    }

    private static EquipmentView equipment(EquipmentSlot slot, String itemName, String emptyLabel) {
        String safe = itemName == null || itemName.isBlank() || itemName.toUpperCase(Locale.ROOT).contains("EMPTY")
                ? emptyLabel : itemName;
        return new EquipmentView(slot, safe, safe.equals(emptyLabel));
    }

    private static RegionView view(BodyPart part, String shortLabel, Rectangle bounds) {
        double maximum = Math.max(1.0, part.maxHealth());
        double current = Math.max(0.0, Math.min(maximum, part.currentHealth()));
        double ratio = current / maximum;
        boolean destroyed = part.destroyed();
        return new RegionView(part.name, shortLabel, bounds, ratio,
                (int)Math.round(current), (int)Math.round(maximum), statusFor(ratio, destroyed), destroyed);
    }

    private static BodyPart removeBestMatch(Map<String, BodyPart> unmatched, String templateId) {
        String wanted = normalize(templateId);
        BodyPart exact = unmatched.remove(wanted);
        if (exact != null) return exact;

        String bestKey = null;
        int bestScore = Integer.MIN_VALUE;
        for (String key : unmatched.keySet()) {
            int score = matchScore(key, wanted);
            if (score > bestScore) {
                bestScore = score;
                bestKey = key;
            }
        }
        if (bestScore < 2 || bestKey == null) return null;
        return unmatched.remove(bestKey);
    }

    private static int matchScore(String candidate, String wanted) {
        if (candidate.equals(wanted)) return 100;
        int score = 0;
        for (String token : wanted.split(" ")) if (!token.isBlank() && candidate.contains(token)) score += 3;
        if (candidate.contains("torso") && wanted.equals("chest")) score += 8;
        if (candidate.contains("body") && wanted.equals("chest")) score += 5;
        if (candidate.contains("stomach") && wanted.equals("abdomen")) score += 7;
        if (candidate.contains("hip") && wanted.equals("pelvis")) score += 7;
        return score;
    }

    private static Rectangle scale(Rectangle bounds, Template template) {
        return new Rectangle(
                bounds.x + (int)Math.round(bounds.width * template.x()),
                bounds.y + (int)Math.round(bounds.height * template.y()),
                Math.max(18, (int)Math.round(bounds.width * template.width())),
                Math.max(12, (int)Math.round(bounds.height * template.height()))
        );
    }

    private static Color colorFor(double ratio, boolean destroyed) {
        if (destroyed || ratio <= 0.0) return new Color(55, 45, 48);
        if (ratio < 0.25) return new Color(150, 45, 40);
        if (ratio < 0.50) return new Color(190, 76, 42);
        if (ratio < 0.75) return new Color(195, 139, 49);
        if (ratio < 0.95) return new Color(132, 151, 70);
        return new Color(70, 145, 91);
    }

    private static void drawCentered(Graphics2D g, Rectangle r, String text) {
        FontMetrics fm = g.getFontMetrics();
        String value = text == null ? "" : text;
        if (fm.stringWidth(value) > r.width - 4) value = Integer.toString(extractCurrent(value));
        int x = r.x + Math.max(2, (r.width - fm.stringWidth(value)) / 2);
        int y = r.y + (r.height + fm.getAscent() - fm.getDescent()) / 2;
        g.drawString(value, x, y);
    }

    private static int extractCurrent(String value) {
        int slash = value.indexOf('/');
        try { return Integer.parseInt(slash < 0 ? value : value.substring(0, slash)); }
        catch (RuntimeException ignored) { return 0; }
    }

    private static String compactLabel(String name) {
        if (name == null || name.isBlank()) return "Region";
        return name.length() <= 15 ? name : name.substring(0, 15);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT)
                .replace("left", "l").replace("right", "r")
                .replace('-', ' ').replace('_', ' ').replaceAll("\\s+", " ").trim();
    }
}
