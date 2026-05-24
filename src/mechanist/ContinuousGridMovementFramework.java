package mechanist;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Objects;

/**
 * Continuous tile-space movement framework used by the experimental first-person viewport
 * and by a small Swing diagnostic panel. Positions are stored as true double precision
 * center points in logical grid units while collision remains rigid against whole blocked
 * tiles, mirroring classic raycaster/Doom-style movement over a server-authoritative grid.
 */
public final class ContinuousGridMovementFramework {
    private ContinuousGridMovementFramework() {}
}

enum ContinuousTileType {
    EMPTY, SOLID_WALL, ENTITY_BLOCK;

    boolean blocksMovement() {
        return switch (this) {
            case EMPTY -> false;
            case SOLID_WALL, ENTITY_BLOCK -> true;
        };
    }
}

record LogicalTile(int x, int y) {
    String label() { return "[" + x + ", " + y + "]"; }
}

record ContinuousPlayerState(double posX, double posY, double velocityX, double velocityY,
                             double lookAngleRadians, double radius, LogicalTile logicalTile) {}

interface ContinuousCollisionGrid {
    int width();
    int height();
    ContinuousTileType tileAt(int x, int y);

    default boolean inBounds(int x, int y) {
        return x >= 0 && y >= 0 && x < width() && y < height();
    }

    default boolean blocks(int x, int y) {
        return !inBounds(x, y) || tileAt(x, y).blocksMovement();
    }
}

final class ContinuousGridWorld implements ContinuousCollisionGrid {
    private final int width;
    private final int height;
    private final ContinuousTileType[][] tiles;

    ContinuousGridWorld(int width, int height) {
        if (width <= 0 || height <= 0) throw new IllegalArgumentException("world dimensions must be positive");
        this.width = width;
        this.height = height;
        this.tiles = new ContinuousTileType[width][height];
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) tiles[x][y] = ContinuousTileType.EMPTY;
        }
    }

    @Override public int width() { return width; }
    @Override public int height() { return height; }

    @Override public ContinuousTileType tileAt(int x, int y) {
        if (!inBounds(x, y)) return ContinuousTileType.SOLID_WALL;
        ContinuousTileType t = tiles[x][y];
        return t == null ? ContinuousTileType.EMPTY : t;
    }

    void setTile(int x, int y, ContinuousTileType type) {
        if (!inBounds(x, y)) throw new IndexOutOfBoundsException("tile outside world: " + x + "," + y);
        tiles[x][y] = Objects.requireNonNull(type, "type");
    }

    LogicalTile logicalTileAt(double posX, double posY) {
        return new LogicalTile((int)Math.floor(posX), (int)Math.floor(posY));
    }

    static ContinuousGridWorld sampleArena() {
        ContinuousGridWorld w = new ContinuousGridWorld(18, 13);
        for (int x = 0; x < w.width; x++) {
            w.setTile(x, 0, ContinuousTileType.SOLID_WALL);
            w.setTile(x, w.height - 1, ContinuousTileType.SOLID_WALL);
        }
        for (int y = 0; y < w.height; y++) {
            w.setTile(0, y, ContinuousTileType.SOLID_WALL);
            w.setTile(w.width - 1, y, ContinuousTileType.SOLID_WALL);
        }
        for (int y = 2; y < 10; y++) w.setTile(6, y, ContinuousTileType.SOLID_WALL);
        for (int x = 9; x < 15; x++) w.setTile(x, 7, ContinuousTileType.SOLID_WALL);
        w.setTile(10, 4, ContinuousTileType.ENTITY_BLOCK);
        w.setTile(13, 5, ContinuousTileType.ENTITY_BLOCK);
        return w;
    }
}

final class MechanistContinuousCollisionGrid implements ContinuousCollisionGrid {
    private final World world;
    private final int ignoredPlayerTileX;
    private final int ignoredPlayerTileY;

    MechanistContinuousCollisionGrid(World world, int ignoredPlayerTileX, int ignoredPlayerTileY) {
        this.world = Objects.requireNonNull(world, "world");
        this.ignoredPlayerTileX = ignoredPlayerTileX;
        this.ignoredPlayerTileY = ignoredPlayerTileY;
    }

    @Override public int width() { return world.w; }
    @Override public int height() { return world.h; }

    @Override public ContinuousTileType tileAt(int x, int y) {
        if (!world.inBounds(x, y)) return ContinuousTileType.SOLID_WALL;
        if (!world.walkable(x, y)) return ContinuousTileType.SOLID_WALL;
        if (!(x == ignoredPlayerTileX && y == ignoredPlayerTileY) && world.npcAt(x, y) != null) return ContinuousTileType.ENTITY_BLOCK;
        return ContinuousTileType.EMPTY;
    }
}

final class ContinuousGridPlayer {
    private double posX;
    private double posY;
    private double velocityX;
    private double velocityY;
    private double lookAngleRadians;
    private double radius = 0.28;
    private double maxSpeed = 3.15;
    private double acceleration = 17.0;
    private double dampingPerSecond = 8.5;
    private boolean forward;
    private boolean backward;
    private boolean strafeLeft;
    private boolean strafeRight;

    ContinuousGridPlayer(double posX, double posY) {
        this.posX = posX;
        this.posY = posY;
    }

    void setRadius(double radius) {
        this.radius = Math.max(0.05, Math.min(0.46, radius));
    }

    void setMotionTuning(double maxSpeed, double acceleration, double dampingPerSecond) {
        this.maxSpeed = Math.max(0.2, maxSpeed);
        this.acceleration = Math.max(0.2, acceleration);
        this.dampingPerSecond = Math.max(0.1, dampingPerSecond);
    }

    void setPosition(double x, double y) {
        this.posX = x;
        this.posY = y;
        this.velocityX = 0.0;
        this.velocityY = 0.0;
    }

    void snapToTileCenter(int tileX, int tileY) {
        setPosition(tileX + 0.5, tileY + 0.5);
    }

    void setLookAngleRadians(double radians) { this.lookAngleRadians = normalizeAngle(radians); }
    void rotateByMouseDelta(int deltaX, double sensitivityRadiansPerPixel) {
        if (deltaX != 0) setLookAngleRadians(lookAngleRadians + deltaX * sensitivityRadiansPerPixel);
    }

    void setMovementIntent(boolean forward, boolean backward, boolean strafeLeft, boolean strafeRight) {
        this.forward = forward;
        this.backward = backward;
        this.strafeLeft = strafeLeft;
        this.strafeRight = strafeRight;
    }

    void setForward(boolean value) { this.forward = value; }
    void setBackward(boolean value) { this.backward = value; }
    void setStrafeLeft(boolean value) { this.strafeLeft = value; }
    void setStrafeRight(boolean value) { this.strafeRight = value; }

    ContinuousPlayerState update(ContinuousCollisionGrid world, double dtSeconds) {
        Objects.requireNonNull(world, "world");
        double dt = Math.max(0.0, Math.min(0.10, dtSeconds));
        double lookX = Math.cos(lookAngleRadians);
        double lookY = Math.sin(lookAngleRadians);
        double sideX = -lookY;
        double sideY = lookX;
        double wishX = 0.0;
        double wishY = 0.0;
        if (forward) { wishX += lookX; wishY += lookY; }
        if (backward) { wishX -= lookX; wishY -= lookY; }
        if (strafeLeft) { wishX += sideX; wishY += sideY; }
        if (strafeRight) { wishX -= sideX; wishY -= sideY; }
        double wishLen = Math.hypot(wishX, wishY);
        if (wishLen > 1.0e-9) {
            wishX /= wishLen;
            wishY /= wishLen;
            velocityX += wishX * acceleration * dt;
            velocityY += wishY * acceleration * dt;
        } else {
            double damping = Math.max(0.0, Math.min(1.0, 1.0 - dampingPerSecond * dt));
            velocityX *= damping;
            velocityY *= damping;
            if (Math.abs(velocityX) < 0.0008) velocityX = 0.0;
            if (Math.abs(velocityY) < 0.0008) velocityY = 0.0;
        }
        double speed = Math.hypot(velocityX, velocityY);
        if (speed > maxSpeed) {
            double s = maxSpeed / speed;
            velocityX *= s;
            velocityY *= s;
        }
        double nextX = posX + velocityX * dt;
        double nextY = posY + velocityY * dt;
        boolean movedX = false;
        boolean movedY = false;
        if (isPositionClear(world, nextX, posY)) { posX = nextX; movedX = true; }
        else velocityX = 0.0;
        if (isPositionClear(world, posX, nextY)) { posY = nextY; movedY = true; }
        else velocityY = 0.0;
        if (!movedX && !movedY && !isPositionClear(world, posX, posY)) {
            resolveOutOfSolid(world);
        }
        return state();
    }

    boolean isPositionClear(ContinuousCollisionGrid world, double centerX, double centerY) {
        int minX = (int)Math.floor(centerX - radius);
        int maxX = (int)Math.floor(centerX + radius);
        int minY = (int)Math.floor(centerY - radius);
        int maxY = (int)Math.floor(centerY + radius);
        for (int tx = minX; tx <= maxX; tx++) {
            for (int ty = minY; ty <= maxY; ty++) {
                if (world.blocks(tx, ty)) return false;
            }
        }
        return true;
    }

    private void resolveOutOfSolid(ContinuousCollisionGrid world) {
        int originX = (int)Math.floor(posX);
        int originY = (int)Math.floor(posY);
        double bestX = posX;
        double bestY = posY;
        double bestD2 = Double.POSITIVE_INFINITY;
        for (int r = 0; r <= 4; r++) {
            for (int dy = -r; dy <= r; dy++) {
                for (int dx = -r; dx <= r; dx++) {
                    if (Math.abs(dx) != r && Math.abs(dy) != r) continue;
                    int tx = originX + dx;
                    int ty = originY + dy;
                    double cx = tx + 0.5;
                    double cy = ty + 0.5;
                    if (!isPositionClear(world, cx, cy)) continue;
                    double d2 = (cx - posX) * (cx - posX) + (cy - posY) * (cy - posY);
                    if (d2 < bestD2) { bestD2 = d2; bestX = cx; bestY = cy; }
                }
            }
            if (bestD2 < Double.POSITIVE_INFINITY) break;
        }
        posX = bestX;
        posY = bestY;
        velocityX = 0.0;
        velocityY = 0.0;
    }

    LogicalTile currentLogicalTile() { return new LogicalTile((int)Math.floor(posX), (int)Math.floor(posY)); }
    ContinuousPlayerState state() { return new ContinuousPlayerState(posX, posY, velocityX, velocityY, lookAngleRadians, radius, currentLogicalTile()); }

    double posX() { return posX; }
    double posY() { return posY; }
    double velocityX() { return velocityX; }
    double velocityY() { return velocityY; }
    double lookAngleRadians() { return lookAngleRadians; }
    double radius() { return radius; }

    private double normalizeAngle(double a) {
        double twoPi = Math.PI * 2.0;
        a %= twoPi;
        return a < 0.0 ? a + twoPi : a;
    }
}

final class ContinuousMovementGamePanel extends JPanel implements ActionListener, KeyListener, MouseMotionListener, MouseListener {
    private static final int TILE_PIXELS = 42;
    private final ContinuousGridWorld world = ContinuousGridWorld.sampleArena();
    private final ContinuousGridPlayer player = new ContinuousGridPlayer(2.5, 2.5);
    private final Timer timer = new Timer(33, this);
    private long lastNanos = System.nanoTime();
    private boolean mousePrimed;
    private int lastMouseX;

    ContinuousMovementGamePanel() {
        setFocusable(true);
        setBackground(Color.BLACK);
        addKeyListener(this);
        addMouseMotionListener(this);
        addMouseListener(this);
        player.setRadius(0.30);
        player.setMotionTuning(3.4, 18.0, 9.0);
        timer.start();
    }

    @Override public Dimension getPreferredSize() {
        return new Dimension(world.width() * TILE_PIXELS, world.height() * TILE_PIXELS + 36);
    }

    @Override protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            for (int x = 0; x < world.width(); x++) {
                for (int y = 0; y < world.height(); y++) {
                    ContinuousTileType tile = world.tileAt(x, y);
                    Color c = switch (tile) {
                        case EMPTY -> new Color(34, 38, 38);
                        case SOLID_WALL -> new Color(90, 78, 58);
                        case ENTITY_BLOCK -> new Color(128, 60, 52);
                    };
                    int px = x * TILE_PIXELS;
                    int py = y * TILE_PIXELS;
                    g2.setColor(c);
                    g2.fillRect(px, py, TILE_PIXELS, TILE_PIXELS);
                    g2.setColor(new Color(0, 0, 0, 80));
                    g2.drawRect(px, py, TILE_PIXELS, TILE_PIXELS);
                }
            }
            int cx = (int)Math.round(player.posX() * TILE_PIXELS);
            int cy = (int)Math.round(player.posY() * TILE_PIXELS);
            int r = (int)Math.round(player.radius() * TILE_PIXELS);
            g2.setColor(new Color(210, 220, 120));
            g2.fillOval(cx - r, cy - r, r * 2, r * 2);
            g2.setColor(Color.BLACK);
            g2.drawOval(cx - r, cy - r, r * 2, r * 2);
            int lx = (int)Math.round(cx + Math.cos(player.lookAngleRadians()) * TILE_PIXELS * 0.55);
            int ly = (int)Math.round(cy + Math.sin(player.lookAngleRadians()) * TILE_PIXELS * 0.55);
            g2.setColor(new Color(245, 240, 170));
            g2.drawLine(cx, cy, lx, ly);
            LogicalTile tile = player.currentLogicalTile();
            g2.setFont(new Font(Font.MONOSPACED, Font.BOLD, 15));
            g2.setColor(new Color(0, 0, 0, 190));
            g2.fillRect(0, world.height() * TILE_PIXELS, getWidth(), 36);
            g2.setColor(new Color(230, 218, 170));
            g2.drawString("Current Logical Tile: " + tile.label() + "  WASD move / mouse look", 12, world.height() * TILE_PIXELS + 23);
        } finally {
            g2.dispose();
        }
    }

    @Override public void actionPerformed(ActionEvent e) {
        long now = System.nanoTime();
        double dt = Math.max(0.0, Math.min(0.10, (now - lastNanos) / 1_000_000_000.0));
        lastNanos = now;
        player.update(world, dt);
        repaint();
    }

    @Override public void keyPressed(KeyEvent e) { setKey(e.getKeyCode(), true); }
    @Override public void keyReleased(KeyEvent e) { setKey(e.getKeyCode(), false); }
    @Override public void keyTyped(KeyEvent e) {}

    private void setKey(int code, boolean down) {
        switch (code) {
            case KeyEvent.VK_W, KeyEvent.VK_UP -> player.setForward(down);
            case KeyEvent.VK_S, KeyEvent.VK_DOWN -> player.setBackward(down);
            case KeyEvent.VK_A, KeyEvent.VK_LEFT -> player.setStrafeLeft(down);
            case KeyEvent.VK_D, KeyEvent.VK_RIGHT -> player.setStrafeRight(down);
            default -> { }
        }
    }

    @Override public void mouseMoved(MouseEvent e) { handleMouse(e); }
    @Override public void mouseDragged(MouseEvent e) { handleMouse(e); }
    private void handleMouse(MouseEvent e) {
        if (!mousePrimed) { lastMouseX = e.getX(); mousePrimed = true; return; }
        int dx = e.getX() - lastMouseX;
        lastMouseX = e.getX();
        player.rotateByMouseDelta(dx, 0.005);
    }
    @Override public void mouseClicked(MouseEvent e) { requestFocusInWindow(); }
    @Override public void mousePressed(MouseEvent e) { requestFocusInWindow(); }
    @Override public void mouseReleased(MouseEvent e) {}
    @Override public void mouseEntered(MouseEvent e) {}
    @Override public void mouseExited(MouseEvent e) { mousePrimed = false; }
}
