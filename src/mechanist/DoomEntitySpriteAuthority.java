package mechanist;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Transparent world-space actor silhouettes, independent of conversation portraits.
 * Images are generated once per visual family/variant; no drawing or random rolls
 * occur per actor on the frame path. Game state and portrait selection are untouched.
 */
final class DoomEntitySpriteAuthority {
    record Profile(String family, double width, double height) { }

    private static final Map<String, BufferedImage> CACHE = new HashMap<>();
    private static final Color INK = new Color(21, 23, 29);
    private static final Color SHADOW = new Color(39, 44, 52);
    private static final Color EYE = new Color(243, 174, 99);
    private static final Color[] PALETTE = {
            new Color(119, 123, 117), new Color(109, 126, 136),
            new Color(130, 110, 91), new Color(103, 121, 94)
    };

    private DoomEntitySpriteAuthority() { }

    static Profile profile(NpcEntity npc) {
        if (npc == null) return new Profile("person", .60, 1.12);
        String identity = (safe(npc.animalProfileId) + " " + safe(npc.name) + " "
                + safe(npc.role) + " " + safe(npc.creatureKind)).toLowerCase(Locale.ROOT);
        if (!npc.isAnimalActor()) {
            if (has(identity, "automaton", "servitor", "cyborg", "robot", "construct")) return new Profile("machine", .64, 1.15);
            if (has(identity, "guard", "soldier", "security", "warden", "enforcer", "militia")
                    || npc.equipmentTier >= 4) return new Profile("guard", .68, 1.16);
            if (has(identity, "mechanic", "worker", "labor", "miner", "engineer", "technician", "fabricator")) return new Profile("worker", .63, 1.10);
            if (has(identity, "cultist", "priest", "seer", "mystic", "cleric")) return new Profile("robed", .67, 1.13);
            return new Profile("person", .60, npc.isChildActor() ? .82 : 1.12);
        }
        if (has(identity, "sump-beast")) return new Profile("beast", .96, .94);
        if (has(identity, "rat", "rodent")) return new Profile("rat", .49, .32);
        if (has(identity, "eel", "serpent")) return new Profile("eel", .72, .26);
        if (has(identity, "corpse-feeder")) return new Profile("feeder", .62, .68);
        if (has(identity, "hound", "mastiff", "dog", "malamute", "scrag-pup")) return new Profile("hound", .78, .70);
        if (has(identity, "hog", "boar", "pig")) return new Profile("hog", .78, .64);
        if (has(identity, "goat")) return new Profile("goat", .74, .79);
        if (has(identity, "crow", "raven")) return new Profile("crow", .60, .63);
        if (has(identity, "fowl", "chicken")) return new Profile("fowl", .50, .50);
        if (has(identity, "louse", "mite", "swarm", "cloud")) return new Profile("swarm", .56, .29);
        if (has(identity, "grub", "larva", "worm")) return new Profile("grub", .62, .34);
        if (has(identity, "cat", "feline")) return new Profile("cat", .60, .52);
        if (has(identity, "moth", "butterfly")) return new Profile("moth", .70, .67);
        if (has(identity, "lizard", "reptile")) return new Profile("lizard", .69, .36);
        if (has(identity, "fish")) return new Profile("fish", .58, .45);
        return new Profile("beast", .72, .65);
    }

    static BufferedImage imageFor(NpcEntity npc) {
        Profile p = profile(npc);
        // Deterministic four-tone variants; does not advance gameplay RNG.
        int variant = Math.floorMod(Objects.hash(npc == null ? "" : npc.id,
                npc == null ? "" : npc.name, npc == null ? 0 : npc.portraitIndex), PALETTE.length);
        String key = p.family() + ":" + variant;
        synchronized (CACHE) { return CACHE.computeIfAbsent(key, unused -> paint(p.family(), variant)); }
    }

    private static BufferedImage paint(String family, int variant) {
        BufferedImage image = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
            Color body = PALETTE[variant];
            g.setColor(INK);
            switch (family) {
                case "rat" -> {
                    line(g, 9, 21, 3, 18, 3); line(g, 3, 18, 2, 13, 2);
                    oval(g, 7, 16, 18, 10); oval(g, 19, 14, 9, 8);
                    triangle(g, 20, 17, 20, 10, 24, 15);
                    triangle(g, 25, 17, 24, 11, 28, 17);
                    rect(g, 11, 23, 4, 5); rect(g, 22, 22, 4, 5);
                    g.setColor(body); oval(g, 11, 18, 13, 5); eye(g, 26, 18);
                }
                case "eel" -> {
                    line(g, 3, 22, 10, 13, 5); line(g, 10, 13, 18, 22, 5);
                    line(g, 18, 22, 26, 15, 6); oval(g, 23, 12, 8, 7);
                    g.setColor(body); line(g, 6, 21, 11, 16, 2); line(g, 13, 16, 20, 23, 2); eye(g, 28, 14);
                }
                case "hound", "cat", "goat", "hog", "beast" -> {
                    int high = family.equals("beast") ? 8 : family.equals("goat") ? 13 : 16;
                    oval(g, 7, high, 18, 11); oval(g, 21, high + 1, 9, 8);
                    rect(g, 10, high + 8, 4, 30 - high - 8); rect(g, 22, high + 7, 4, 31 - high - 7);
                    if (family.equals("hog")) {
                        rect(g, 27, high + 3, 5, 4); line(g, 8, high + 5, 3, high + 2, 2);
                    } else if (family.equals("goat") || family.equals("beast")) {
                        triangle(g, 22, high + 2, 21, high - 7, 26, high);
                        triangle(g, 27, high + 2, 30, high - 6, 29, high + 3);
                        line(g, 26, high + 7, 25, high + 14, 2);
                    } else if (family.equals("cat")) {
                        triangle(g, 22, high + 4, 22, high - 3, 26, high + 1);
                        triangle(g, 27, high + 3, 30, high - 3, 30, high + 5);
                        line(g, 7, high + 7, 2, high + 2, 2); line(g, 2, high + 2, 3, high - 4, 2);
                    } else {
                        triangle(g, 22, high + 2, 21, high - 5, 26, high);
                        triangle(g, 28, high + 2, 29, high - 4, 30, high + 5);
                        line(g, 7, high + 7, 2, high + 2, 3);
                    }
                    g.setColor(body); oval(g, 10, high + 2, 13, 6); eye(g, 28, high + 5);
                }
                case "crow", "fowl" -> {
                    oval(g, 12, 12, 12, 14); oval(g, 19, 9, 8, 8);
                    triangle(g, 25, 12, 31, 14, 25, 16);
                    triangle(g, 16, 16, 4, 8, 10, 23);
                    line(g, 17, 23, 15, 30, 2); line(g, 23, 23, 24, 30, 2);
                    if (family.equals("crow")) triangle(g, 7, 19, 1, 23, 12, 23);
                    g.setColor(body); oval(g, 14, 17, 8, 5); eye(g, 24, 12);
                }
                case "swarm" -> {
                    for (int i = 0; i < 6; i++) {
                        int x = 3 + (i % 3) * 10 + (i % 2) * 2;
                        int y = 12 + (i / 3) * 10 + (i % 2) * 3;
                        oval(g, x, y, 6, 5); line(g, x + 1, y + 2, x - 1, y - 1, 1);
                        g.setColor(body); rect(g, x + 2, y + 1, 2, 2); g.setColor(INK);
                    }
                }
                case "grub" -> {
                    for (int i = 0; i < 5; i++) oval(g, 2 + 5 * i, 18 - (i % 2), 8, 9);
                    oval(g, 25, 17, 6, 9); g.setColor(body);
                    for (int i = 0; i < 5; i++) line(g, 5 + 5 * i, 20, 5 + 5 * i, 24, 1);
                    eye(g, 29, 20);
                }
                case "moth" -> {
                    triangle(g, 15, 13, 3, 7, 7, 24);
                    triangle(g, 17, 13, 29, 7, 25, 24);
                    oval(g, 13, 11, 6, 17); line(g, 15, 13, 11, 6, 2); line(g, 18, 13, 22, 6, 2);
                    g.setColor(body); oval(g, 5, 13, 7, 6); oval(g, 20, 13, 7, 6); eye(g, 15, 13);
                }
                case "lizard" -> {
                    line(g, 12, 20, 2, 27, 4); oval(g, 10, 15, 16, 9); oval(g, 23, 14, 8, 7);
                    line(g, 13, 22, 10, 28, 2); line(g, 23, 22, 26, 28, 2);
                    g.setColor(body); oval(g, 13, 17, 10, 4); eye(g, 29, 16);
                }
                case "fish" -> {
                    triangle(g, 12, 19, 2, 11, 2, 26); oval(g, 10, 12, 21, 15);
                    triangle(g, 15, 14, 20, 7, 22, 15);
                    g.setColor(body); oval(g, 14, 16, 12, 7); eye(g, 27, 18);
                }
                case "feeder" -> {
                    triangle(g, 10, 24, 19, 7, 24, 23); oval(g, 16, 8, 9, 9);
                    line(g, 12, 21, 5, 27, 3); line(g, 21, 22, 29, 29, 3);
                    line(g, 12, 24, 10, 31, 3); line(g, 22, 23, 24, 31, 3);
                    g.setColor(body); line(g, 19, 15, 17, 22, 3); eye(g, 23, 12);
                }
                default -> { // Clearly humanoid: head, shoulders, separated arms and legs.
                    boolean guard = family.equals("guard"), worker = family.equals("worker");
                    boolean robed = family.equals("robed"), machine = family.equals("machine");
                    oval(g, 12, 3, 9, 9); rect(g, 10, 12, 13, 13);
                    rect(g, 6, 13, 5, 12); rect(g, 23, 13, 5, 12);
                    rect(g, 11, 23, 5, 9); rect(g, 18, 23, 5, 9);
                    if (guard) {
                        rect(g, 8, 11, 17, 5); rect(g, 9, 4, 16, 5); rect(g, 27, 16, 3, 13);
                    } else if (worker) {
                        rect(g, 11, 3, 13, 4); rect(g, 25, 20, 6, 5);
                    } else if (robed) {
                        triangle(g, 15, 15, 5, 31, 29, 31); triangle(g, 9, 7, 16, 1, 24, 7);
                    } else if (machine) {
                        rect(g, 12, 3, 10, 9); line(g, 17, 3, 17, 0, 2);
                    }
                    g.setColor(body); rect(g, 13, 14, 7, 8);
                    if (guard) rect(g, 11, 15, 11, 4);
                    if (worker) rect(g, 12, 3, 10, 2);
                    eye(g, 18, 7);
                }
            }
            // Deliberately preserve fully transparent pixels outside the actor body.
        } finally { g.dispose(); }
        return image;
    }

    private static boolean has(String haystack, String... needles) {
        for (String needle : needles) if (haystack.contains(needle)) return true;
        return false;
    }
    private static String safe(String s) { return s == null ? "" : s; }
    private static void rect(Graphics2D g, int x, int y, int w, int h) { g.fillRect(x, y, w, h); }
    private static void oval(Graphics2D g, int x, int y, int w, int h) { g.fillOval(x, y, w, h); }
    private static void triangle(Graphics2D g, int ax, int ay, int bx, int by, int cx, int cy) {
        g.fillPolygon(new Polygon(new int[]{ax, bx, cx}, new int[]{ay, by, cy}, 3));
    }
    private static void line(Graphics2D g, int ax, int ay, int bx, int by, int stroke) {
        g.setStroke(new java.awt.BasicStroke(stroke, java.awt.BasicStroke.CAP_SQUARE, java.awt.BasicStroke.JOIN_MITER));
        g.drawLine(ax, ay, bx, by);
    }
    private static void eye(Graphics2D g, int x, int y) { g.setColor(EYE); rect(g, x, y, 2, 2); }
}
