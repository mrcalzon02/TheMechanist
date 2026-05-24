package mechanist;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import javax.sound.sampled.*;
import java.io.*;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;

class DirtyRegionTracker {
    private World world;
    private Rectangle dirtyBounds;
    private int lightSeenRevision = -1, noiseSeenRevision = -1, visionSeenRevision = -1, hazardSeenRevision = -1;
    private String lastReason = "none";
    private long marks = 0L;

    void reset(World w, String reason) {
        world = w;
        dirtyBounds = null;
        lastReason = reason == null ? "reset" : reason;
        marks = 0L;
        if (w != null) {
            lightSeenRevision = w.dirtyLightRevision - 1;
            noiseSeenRevision = w.dirtyNoiseRevision - 1;
            visionSeenRevision = w.dirtyVisionRevision - 1;
            hazardSeenRevision = w.dirtyHazardRevision - 1;
        }
    }

    void mark(World w, int cx, int cy, int radius, boolean light, boolean noise, boolean vision, boolean hazard, String reason) {
        if (w == null) return;
        if (world != w) reset(w, "world-swap");
        int r = Math.max(1, radius);
        Rectangle next = new Rectangle(Math.max(0, cx-r), Math.max(0, cy-r), Math.min(w.w, cx+r+1) - Math.max(0, cx-r), Math.min(w.h, cy+r+1) - Math.max(0, cy-r));
        if (dirtyBounds == null) dirtyBounds = next; else dirtyBounds = dirtyBounds.union(next);
        if (light) w.dirtyLightRevision++;
        if (noise) w.dirtyNoiseRevision++;
        if (vision) w.dirtyVisionRevision++;
        if (hazard) w.dirtyHazardRevision++;
        lastReason = reason == null ? "unspecified" : reason;
        marks++;
    }

    boolean lightDirty(World w) { if (w == null) return false; if (world != w) reset(w, "light-world-swap"); return lightSeenRevision != w.dirtyLightRevision; }
    boolean noiseDirty(World w) { if (w == null) return false; if (world != w) reset(w, "noise-world-swap"); return noiseSeenRevision != w.dirtyNoiseRevision; }
    boolean visionDirty(World w) { if (w == null) return false; if (world != w) reset(w, "vision-world-swap"); return visionSeenRevision != w.dirtyVisionRevision; }
    boolean hazardDirty(World w) { if (w == null) return false; if (world != w) reset(w, "hazard-world-swap"); return hazardSeenRevision != w.dirtyHazardRevision; }

    void clearLight(World w) { if (w != null) lightSeenRevision = w.dirtyLightRevision; }
    void clearNoise(World w) { if (w != null) noiseSeenRevision = w.dirtyNoiseRevision; }
    void clearVision(World w) { if (w != null) visionSeenRevision = w.dirtyVisionRevision; dirtyBounds = null; }
    void clearHazard(World w) { if (w != null) hazardSeenRevision = w.dirtyHazardRevision; }

    Rectangle visibilityClearRect(World w, int playerX, int playerY, int span, Rectangle previousVisible) {
        if (w == null) return null;
        if (world != w) reset(w, "visibility-world-swap");
        Rectangle current = new Rectangle(Math.max(0, playerX-span-2), Math.max(0, playerY-span-2), Math.min(w.w, playerX+span+3)-Math.max(0, playerX-span-2), Math.min(w.h, playerY+span+3)-Math.max(0, playerY-span-2));
        Rectangle r = previousVisible == null ? current : previousVisible.union(current);
        if (dirtyBounds != null) r = r.union(dirtyBounds);
        if (r.width <= 0 || r.height <= 0) return null;
        if (r.width * r.height > (w.w * w.h * 2) / 3) return null;
        return r;
    }

    String summary() {
        String b = dirtyBounds == null ? "none" : (dirtyBounds.x + "," + dirtyBounds.y + " " + dirtyBounds.width + "x" + dirtyBounds.height);
        return "marks=" + marks + " bounds=" + b + " last=" + lastReason;
    }
}


class ScrollRegion {
    final String tag; final Rectangle track; final Rectangle thumb; final int maxScroll; final int visibleLines; final int value;
    ScrollRegion(String tag, Rectangle track, Rectangle thumb, int maxScroll, int visibleLines, int value){ this.tag=tag; this.track=track; this.thumb=thumb; this.maxScroll=maxScroll; this.visibleLines=visibleLines; this.value=value; }
}


class GuiLayoutApi {
    static int scaled(int base, double scale) { return Math.max(1, (int)Math.round(base * Math.max(0.5, Math.min(2.0, scale)))); }
    static int columnGap(double scale) { return Math.max(12, scaled(16, scale)); }
    static Rectangle optionsSafeRect(int screenWidth, double scale) {
        int margin = Math.max(52, scaled(52, scale));
        int half = scaled(440, scale);
        int left = Math.max(margin, screenWidth/2 - half);
        int right = Math.min(screenWidth - margin, screenWidth/2 + half);
        if (right <= left + scaled(360, scale)) { left = margin; right = Math.max(left + scaled(360, scale), screenWidth - margin); }
        return new Rectangle(left, 0, Math.max(scaled(360, scale), right-left), 1);
    }
    static int columnWidth(Rectangle safe, int columns, double scale) {
        int cols = Math.max(1, columns);
        int gap = columnGap(scale);
        return Math.max(scaled(112, scale), (safe.width - gap*(cols-1)) / cols);
    }
    static String fitLabel(String text, FontMetrics fm, int maxWidth) {
        String t = text == null ? "" : text;
        if (fm == null || fm.stringWidth(t) <= maxWidth) return t;
        String ell = "…";
        if (fm.stringWidth(ell) > maxWidth) return "";
        while (t.length() > 1 && fm.stringWidth(t + ell) > maxWidth) t = t.substring(0, t.length()-1);
        return t + ell;
    }
    static java.util.List<String> wrapText(String text, FontMetrics fm, int maxWidth) {
        java.util.ArrayList<String> out = new java.util.ArrayList<>();
        String t = text == null ? "" : text.trim();
        if (t.isEmpty()) { out.add(""); return out; }
        if (fm == null || maxWidth <= 8) { out.add(t); return out; }
        StringBuilder line = new StringBuilder();
        for (String word : t.split("\s+")) {
            String candidate = line.length() == 0 ? word : line + " " + word;
            if (fm.stringWidth(candidate) <= maxWidth) line.setLength(0);
            if (fm.stringWidth(candidate) <= maxWidth) line.append(candidate);
            else {
                if (line.length() > 0) out.add(line.toString());
                line.setLength(0);
                if (fm.stringWidth(word) <= maxWidth) line.append(word);
                else out.add(fitLabel(word, fm, maxWidth));
            }
        }
        if (line.length() > 0) out.add(line.toString());
        return out;
    }
}


class ButtonBox {
    String label; Rectangle r; String tip; Runnable action; int uiDebugId = -1; BufferedImage icon;
    ButtonBox(String label,int x,int y,int w,int h,String tip,Runnable action){this(label,x,y,w,h,tip,action,null);}
    ButtonBox(String label,int x,int y,int w,int h,String tip,Runnable action,BufferedImage icon){this.label=label;this.r=new Rectangle(x,y,w,h);this.tip=tip;this.action=action;this.icon=icon;}
    boolean contains(int x,int y){return r.contains(x,y);}
    void draw(Graphics2D g, Font f, boolean selected, ImageCache images, GameOptions options){
        if (g == null || r == null || r.width <= 0 || r.height <= 0) return;
        Shape oldClip = g.getClip();
        g.clipRect(r.x, r.y, Math.max(1, r.width), Math.max(1, r.height));
        try {
            BufferedImage bg = images == null ? null : images.get(selected ? "button_hover" : "button_normal");
            if (bg != null) {
                g.drawImage(bg, r.x, r.y, r.width, r.height, null);
                g.setColor(new Color(0,0,0, selected ? 132 : 172));
                g.fillRoundRect(r.x+6, r.y+6, Math.max(1,r.width-12), Math.max(1,r.height-12), 8, 8);
                g.setColor(new Color(130,105,55, selected ? 190 : 135));
                g.drawRoundRect(r.x+6, r.y+6, Math.max(1,r.width-12), Math.max(1,r.height-12), 8, 8);
            } else {
                g.setColor(selected ? new Color(42,36,28,235) : new Color(18,18,20,220)); g.fillRect(r.x,r.y,r.width,r.height);
                g.setColor(selected ? new Color(235,205,120) : new Color(170,150,95)); g.drawRect(r.x,r.y,r.width,r.height);
            }
            Font useFont = f == null ? new Font("SansSerif", Font.BOLD, 14) : f;
            int innerW = Math.max(18, r.width - 18);
            int innerH = Math.max(12, r.height - 10);
            g.setFont(useFont);
            FontMetrics fm=g.getFontMetrics();
            while ((fm.stringWidth(label == null ? "" : label) > innerW || fm.getHeight() > innerH) && useFont.getSize2D() > 6f) {
                useFont = useFont.deriveFont(useFont.getSize2D() - 1f);
                g.setFont(useFont);
                fm = g.getFontMetrics();
            }
            g.setColor(options == null ? (selected ? new Color(245,220,140) : new Color(210,190,125)) : new Color(options.colorValue(selected ? GameOptions.TEXT_HIGHLIGHT : GameOptions.TEXT_MAIN)));
            if(selected) { g.drawRect(r.x+2,r.y+2,Math.max(1,r.width-4),Math.max(1,r.height-4)); }
            int iconSize = icon == null ? 0 : Math.max(10, Math.min(r.height - 8, 26));
            int textW = Math.max(10, innerW - (iconSize > 0 ? iconSize + 8 : 0));
            String drawLabel = GuiLayoutApi.fitLabel(label, fm, textW);
            int labelW = fm.stringWidth(drawLabel);
            int groupW = labelW + (iconSize > 0 ? iconSize + 8 : 0);
            int tx = r.x + Math.max(4, (r.width-groupW)/2) + (iconSize > 0 ? iconSize + 8 : 0);
            int minTx = r.x + 8 + (iconSize > 0 ? iconSize + 8 : 0);
            int maxTx = r.x + r.width - labelW - 8;
            tx = Math.max(minTx, Math.min(tx, Math.max(minTx, maxTx)));
            int ty = r.y+(r.height+fm.getAscent())/2-4;
            int minTy = r.y + fm.getAscent() + 3;
            int maxTy = r.y + r.height - Math.max(3, fm.getDescent());
            ty = Math.max(minTy, Math.min(ty, maxTy));
            Color labelColor = g.getColor();
            int capsuleH = Math.min(Math.max(10, r.height-4), Math.max(10, fm.getHeight()+5));
            int capX = Math.max(r.x + 4, tx - 6);
            int capW = Math.min(r.x + r.width - 4 - capX, Math.max(1, labelW + 12));
            int capY = Math.max(r.y+3, Math.min(r.y + r.height - capsuleH - 3, ty-fm.getAscent()-3));
            g.setColor(new Color(0,0,0, selected ? 190 : 158));
            g.fillRoundRect(capX, capY, Math.max(1, capW), Math.max(10, capsuleH), 8, 8);
            g.setColor(new Color(135,112,60, selected ? 205 : 130));
            g.drawRoundRect(capX, capY, Math.max(1, capW), Math.max(10, capsuleH), 8, 8);
            if (iconSize > 0) {
                int ix = Math.max(r.x + 5, tx - iconSize - 8);
                int iy = r.y + (r.height - iconSize) / 2;
                g.drawImage(icon, ix, iy, iconSize, iconSize, null);
                g.setColor(new Color(175,145,78, selected ? 190 : 120));
                g.drawRect(ix, iy, iconSize, iconSize);
            }
            g.setColor(labelColor);
            g.drawString(drawLabel, tx, ty);
            if (uiDebugId > 0) {
                String id = "B" + String.format(java.util.Locale.US, "%03d", uiDebugId);
                Font old = g.getFont();
                Color oldC = g.getColor();
                g.setFont(new Font("Monospaced", Font.BOLD, 8));
                FontMetrics dfm = g.getFontMetrics();
                int idW = dfm.stringWidth(id);
                int ix = Math.max(r.x + 1, r.x + r.width - idW - 3);
                int iy = r.y + 2;
                g.setColor(new Color(0,0,0,200));
                g.fillRect(ix - 1, iy, idW + 2, dfm.getHeight());
                g.setColor(new Color(255,226,122));
                g.drawString(id, ix, iy + dfm.getAscent());
                g.setFont(old);
                g.setColor(oldC);
            }
        } finally {
            g.setClip(oldClip);
        }
    }

}



class TextSurfaceApi {
    static java.util.List<String> wrap(String text, int max) {
        ArrayList<String> out = new ArrayList<>();
        if (text == null) { out.add(""); return out; }
        StringBuilder line = new StringBuilder();
        int safeMax = Math.max(1, max);
        for (String word : text.split(" ")) {
            if (line.length() > 0 && line.length() + word.length() + 1 > safeMax) { out.add(line.toString()); line.setLength(0); }
            if (line.length() > 0) line.append(' ');
            line.append(word);
        }
        if (line.length() > 0) out.add(line.toString());
        if (out.isEmpty()) out.add("");
        return out;
    }

    static String ellipsize(String s, int maxChars) {
        if (s == null) return "";
        if (s.length() <= maxChars) return s;
        if (maxChars <= 3) return s.substring(0, Math.max(0, maxChars));
        return s.substring(0, maxChars - 3) + "...";
    }
}

class TextLayoutAuthority {
    private static final int MAX_CACHE_ENTRIES = 768;
    private static final LinkedHashMap<String, java.util.List<String>> WRAP_CACHE = new LinkedHashMap<>(128, 0.75f, true) {
        @Override protected boolean removeEldestEntry(Map.Entry<String, java.util.List<String>> eldest) {
            return size() > MAX_CACHE_ENTRIES;
        }
    };

    static java.util.List<String> wrapPixels(Graphics2D g, Font font, String text, int maxPixelWidth) {
        if (text == null) text = "";
        int width = Math.max(1, maxPixelWidth);
        Font useFont = font == null ? g.getFont() : font;
        String key = fontKey(useFont) + "|" + width + "|" + text;
        synchronized (WRAP_CACHE) {
            java.util.List<String> cached = WRAP_CACHE.get(key);
            if (cached != null) return cached;
        }
        Font old = g.getFont();
        if (useFont != null) g.setFont(useFont);
        FontMetrics fm = g.getFontMetrics();
        ArrayList<String> out = new ArrayList<>();
        for (String paragraph : text.replace("\r", "").split("\n", -1)) {
            wrapParagraph(paragraph, width, fm, out);
        }
        if (out.isEmpty()) out.add("");
        if (old != null) g.setFont(old);
        java.util.List<String> frozen = Collections.unmodifiableList(out);
        synchronized (WRAP_CACHE) { WRAP_CACHE.put(key, frozen); }
        return frozen;
    }

    static java.util.List<String> wrapAllPixels(Graphics2D g, Font font, java.util.List<String> lines, int maxPixelWidth) {
        ArrayList<String> out = new ArrayList<>();
        if (lines == null || lines.isEmpty()) { out.add(""); return out; }
        for (String line : lines) out.addAll(wrapPixels(g, font, line == null ? "" : line, maxPixelWidth));
        if (out.isEmpty()) out.add("");
        return out;
    }

    static int cacheSize() { synchronized (WRAP_CACHE) { return WRAP_CACHE.size(); } }

    private static String fontKey(Font f) {
        if (f == null) return "null";
        return f.getFontName() + ":" + f.getStyle() + ":" + Math.round(f.getSize2D() * 10.0f);
    }

    private static void wrapParagraph(String paragraph, int width, FontMetrics fm, ArrayList<String> out) {
        if (paragraph == null || paragraph.isBlank()) { out.add(""); return; }
        StringBuilder line = new StringBuilder();
        String[] words = paragraph.trim().split("\\s+");
        for (String word : words) {
            if (word == null || word.isEmpty()) continue;
            if (fm.stringWidth(word) > width) {
                if (line.length() > 0) { out.add(line.toString()); line.setLength(0); }
                splitLongWord(word, width, fm, out);
                continue;
            }
            String candidate = line.length() == 0 ? word : line + " " + word;
            if (line.length() > 0 && fm.stringWidth(candidate) > width) {
                out.add(line.toString());
                line.setLength(0);
                line.append(word);
            } else {
                line.setLength(0);
                line.append(candidate);
            }
        }
        if (line.length() > 0) out.add(line.toString());
    }

    private static void splitLongWord(String word, int width, FontMetrics fm, ArrayList<String> out) {
        StringBuilder chunk = new StringBuilder();
        for (int i = 0; i < word.length(); i++) {
            char c = word.charAt(i);
            String candidate = chunk.toString() + c;
            if (chunk.length() > 0 && fm.stringWidth(candidate) > width) {
                out.add(chunk.toString());
                chunk.setLength(0);
            }
            chunk.append(c);
        }
        if (chunk.length() > 0) out.add(chunk.toString());
    }
}


class RenderImageScaleCacheAuthority {
    private static final int MAX_CACHE_ENTRIES = 1536;
    private static final LinkedHashMap<String, BufferedImage> SCALED_CACHE = new LinkedHashMap<>(256, 0.75f, true) {
        @Override protected boolean removeEldestEntry(Map.Entry<String, BufferedImage> eldest) {
            return size() > MAX_CACHE_ENTRIES;
        }
    };
    private static long drawRequests = 0L;
    private static long directNativeDraws = 0L;
    private static long cacheHits = 0L;
    private static long cacheMisses = 0L;

    static void draw(Graphics2D g, BufferedImage img, int x, int y, int w, int h) {
        if (g == null || img == null) return;
        int dw = Math.max(1, w), dh = Math.max(1, h);
        BufferedImage scaled = scaled(img, dw, dh);
        g.drawImage(scaled, x, y, null);
    }

    static BufferedImage scaled(BufferedImage img, int w, int h) {
        if (img == null) return null;
        int dw = Math.max(1, w), dh = Math.max(1, h);
        synchronized (SCALED_CACHE) { drawRequests++; }
        if (img.getWidth() == dw && img.getHeight() == dh) {
            synchronized (SCALED_CACHE) { directNativeDraws++; }
            return img;
        }
        String key = System.identityHashCode(img) + ":" + img.getWidth() + "x" + img.getHeight() + ">" + dw + "x" + dh;
        synchronized (SCALED_CACHE) {
            BufferedImage cached = SCALED_CACHE.get(key);
            if (cached != null) { cacheHits++; return cached; }
            cacheMisses++;
        }
        BufferedImage out = new BufferedImage(dw, dh, BufferedImage.TYPE_INT_ARGB);
        Graphics2D gg = out.createGraphics();
        try {
            gg.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            gg.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
            gg.setComposite(AlphaComposite.Src);
            gg.drawImage(img, 0, 0, dw, dh, null);
        } finally {
            gg.dispose();
        }
        synchronized (SCALED_CACHE) { SCALED_CACHE.put(key, out); }
        return out;
    }

    static void clear() {
        synchronized (SCALED_CACHE) {
            SCALED_CACHE.clear();
            drawRequests = 0L;
            directNativeDraws = 0L;
            cacheHits = 0L;
            cacheMisses = 0L;
        }
    }
    static int cacheSize() { synchronized (SCALED_CACHE) { return SCALED_CACHE.size(); } }
    static long cacheHits() { synchronized (SCALED_CACHE) { return cacheHits; } }
    static long cacheMisses() { synchronized (SCALED_CACHE) { return cacheMisses; } }
    static String auditSummary() {
        synchronized (SCALED_CACHE) {
            return "render-image-scale-cache entries=" + SCALED_CACHE.size()
                    + " max=" + MAX_CACHE_ENTRIES
                    + " requests=" + drawRequests
                    + " direct=" + directNativeDraws
                    + " hits=" + cacheHits
                    + " misses=" + cacheMisses;
        }
    }
}

class TimeSurfaceApi {
    static String timeTextAtTurn(int t) {
        int safe = Math.max(0, t);
        int hour = safe / Math.max(1, GamePanel.TURNS_PER_HOUR);
        int day = hour / Math.max(1, GamePanel.HOURS_PER_DAY);
        int remHour = hour % Math.max(1, GamePanel.HOURS_PER_DAY);
        return "day " + day + " hour " + remHour + " turn " + safe;
    }
}

class SelectedContextSurfaceApi {
    static char selectedTile(World world, boolean lookCursorActive, int lookX, int lookY, int playerX, int playerY) {
        if (world == null) return '#';
        if (lookCursorActive && lookX >= 0 && lookY >= 0 && lookX < world.w && lookY < world.h) return world.tiles[lookX][lookY];
        if (playerX >= 0 && playerY >= 0 && playerX < world.w && playerY < world.h) return world.tiles[playerX][playerY];
        return '#';
    }

    static String selectedCoordText(boolean lookCursorActive, int lookX, int lookY, int playerX, int playerY) {
        int x = lookCursorActive ? lookX : playerX;
        int y = lookCursorActive ? lookY : playerY;
        return x + "," + y;
    }

    static String auditSummary() {
        return "selectedContextSurface selectedTile+coordText world-bounded look/player context";
    }
}

