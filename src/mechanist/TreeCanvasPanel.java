package mechanist;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.RoundRectangle2D;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Zoomable/pannable Swing canvas for KnowledgeTree visualization.  It performs
 * only presentation, picking, and camera math; unlocking remains model-owned.
 */
public final class TreeCanvasPanel extends JPanel {
    private static final double MIN_SCALE = 0.35;
    private static final double MAX_SCALE = 2.75;
    private static final double NODE_W = 156.0;
    private static final double NODE_H = 58.0;

    private KnowledgeTree tree;
    private KnowledgeNode selectedNode;
    private Consumer<KnowledgeNode> selectionListener;
    private double scale = 1.0;
    private double translateX = 120.0;
    private double translateY = 90.0;
    private Point lastDragPoint;

    public TreeCanvasPanel(KnowledgeTree tree) {
        this.tree = tree;
        setPreferredSize(new Dimension(720, 560));
        setBackground(new Color(8, 9, 8));
        setFocusable(true);
        MouseAdapter mouse = new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) {
                requestFocusInWindow();
                lastDragPoint = e.getPoint();
                if (SwingUtilities.isLeftMouseButton(e)) {
                    KnowledgeNode hit = pickNode(e.getPoint());
                    if (hit != null) setSelectedNode(hit);
                }
            }

            @Override public void mouseDragged(MouseEvent e) {
                if (lastDragPoint == null) {
                    lastDragPoint = e.getPoint();
                    return;
                }
                int dx = e.getX() - lastDragPoint.x;
                int dy = e.getY() - lastDragPoint.y;
                if (SwingUtilities.isMiddleMouseButton(e) || SwingUtilities.isRightMouseButton(e) || pickNode(lastDragPoint) == null) {
                    translateX += dx;
                    translateY += dy;
                    repaint();
                }
                lastDragPoint = e.getPoint();
            }

            @Override public void mouseReleased(MouseEvent e) { lastDragPoint = null; }

            @Override public void mouseWheelMoved(MouseWheelEvent e) { zoomAt(e.getPoint(), e.getPreciseWheelRotation()); }
        };
        addMouseListener(mouse);
        addMouseMotionListener(mouse);
        addMouseWheelListener(mouse);
    }

    public void setKnowledgeTree(KnowledgeTree tree) {
        this.tree = tree;
        selectedNode = null;
        resetView();
        notifySelection(null);
        repaint();
    }

    public KnowledgeTree knowledgeTree() { return tree; }

    public KnowledgeNode selectedNode() { return selectedNode; }

    public void setSelectionListener(Consumer<KnowledgeNode> selectionListener) { this.selectionListener = selectionListener; }

    public void setSelectedNode(KnowledgeNode node) {
        if (node != null && tree != null && !tree.containsNode(node.id())) return;
        selectedNode = node;
        notifySelection(node);
        repaint();
    }

    public void resetView() {
        scale = 1.0;
        translateX = Math.max(70.0, getWidth() * 0.12);
        translateY = Math.max(70.0, getHeight() * 0.14);
    }

    private void notifySelection(KnowledgeNode node) {
        if (selectionListener != null) selectionListener.accept(node);
    }

    private void zoomAt(Point mousePoint, double wheelRotation) {
        double oldScale = scale;
        double zoomFactor = Math.pow(1.10, -wheelRotation);
        double newScale = Math.max(MIN_SCALE, Math.min(MAX_SCALE, scale * zoomFactor));
        if (Math.abs(newScale - oldScale) < 0.0001) return;

        Point2D before = screenToWorld(mousePoint);
        scale = newScale;
        translateX = mousePoint.x - before.getX() * scale;
        translateY = mousePoint.y - before.getY() * scale;
        repaint();
    }

    private Point2D screenToWorld(Point p) {
        return new Point2D.Double((p.x - translateX) / scale, (p.y - translateY) / scale);
    }

    private KnowledgeNode pickNode(Point screenPoint) {
        if (tree == null) return null;
        Point2D worldPoint = screenToWorld(screenPoint);
        for (KnowledgeNode node : tree.nodes()) {
            if (nodeShape(node).contains(worldPoint)) return node;
        }
        return null;
    }

    private Shape nodeShape(KnowledgeNode node) {
        return new RoundRectangle2D.Double(node.x() - NODE_W / 2.0, node.y() - NODE_H / 2.0, NODE_W, NODE_H, 18.0, 18.0);
    }

    @Override protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        drawGrid(g);
        AffineTransform old = g.getTransform();
        g.translate(translateX, translateY);
        g.scale(scale, scale);
        drawTree(g);
        g.setTransform(old);
        drawHud(g);
        g.dispose();
    }

    private void drawGrid(Graphics2D g) {
        g.setColor(new Color(18, 20, 18));
        int step = 48;
        int ox = (int)Math.floor(translateX % step);
        int oy = (int)Math.floor(translateY % step);
        for (int x = ox; x < getWidth(); x += step) g.drawLine(x, 0, x, getHeight());
        for (int y = oy; y < getHeight(); y += step) g.drawLine(0, y, getWidth(), y);
    }

    private void drawTree(Graphics2D g) {
        if (tree == null) return;
        Map<String, KnowledgeNode> byId = new HashMap<>(tree.nodeMap());
        g.setStroke(new BasicStroke(4.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (KnowledgeNode node : tree.nodes()) {
            for (String prerequisiteId : node.prerequisiteNodeIds()) {
                KnowledgeNode prerequisite = byId.get(prerequisiteId);
                if (prerequisite == null) continue;
                g.setColor(edgeColor(prerequisite, node));
                g.drawLine((int)prerequisite.x(), (int)prerequisite.y(), (int)node.x(), (int)node.y());
            }
        }
        for (KnowledgeNode node : tree.nodes()) drawNode(g, node);
    }

    private Color edgeColor(KnowledgeNode prerequisite, KnowledgeNode node) {
        if (prerequisite.unlocked() && node.unlocked()) return new Color(40, 150, 70);
        if (tree.stateOf(node) == KnowledgeTree.NodeState.AVAILABLE) return new Color(194, 166, 55);
        return new Color(83, 83, 78);
    }

    private void drawNode(Graphics2D g, KnowledgeNode node) {
        KnowledgeTree.NodeState state = tree.stateOf(node);
        Shape shape = nodeShape(node);
        Color fill = switch (state) {
            case UNLOCKED -> new Color(30, 112, 55);
            case AVAILABLE -> new Color(156, 130, 34);
            case LOCKED -> new Color(55, 56, 54);
        };
        Color border = node == selectedNode ? new Color(230, 220, 150) : switch (state) {
            case UNLOCKED -> new Color(88, 218, 105);
            case AVAILABLE -> new Color(245, 215, 78);
            case LOCKED -> new Color(116, 116, 110);
        };
        g.setColor(fill);
        g.fill(shape);
        g.setStroke(new BasicStroke(node == selectedNode ? 4.0f : 2.4f));
        g.setColor(border);
        g.draw(shape);

        FontMetrics fm = g.getFontMetrics();
        String title = fit(node.name(), fm, (int)NODE_W - 18);
        String cost = state == KnowledgeTree.NodeState.UNLOCKED ? "KNOWN" : "Cost " + node.pointCost();
        g.setColor(new Color(245, 242, 205));
        g.drawString(title, (int)(node.x() - NODE_W / 2 + 10), (int)(node.y() - 7));
        g.setColor(state == KnowledgeTree.NodeState.LOCKED ? new Color(190, 188, 170) : new Color(255, 242, 156));
        g.drawString(cost, (int)(node.x() - NODE_W / 2 + 10), (int)(node.y() + 15));
    }

    private static String fit(String text, FontMetrics fm, int width) {
        if (text == null) return "";
        if (fm.stringWidth(text) <= width) return text;
        String suffix = "…";
        String t = text;
        while (!t.isEmpty() && fm.stringWidth(t + suffix) > width) t = t.substring(0, t.length() - 1);
        return t + suffix;
    }

    private void drawHud(Graphics2D g) {
        g.setColor(new Color(0, 0, 0, 150));
        g.fillRoundRect(12, 12, 310, 44, 12, 12);
        g.setColor(new Color(210, 200, 150));
        String line = tree == null ? "No knowledge branch loaded." : tree.displayName() + " | wheel zoom | drag pan | click node";
        g.drawString(line, 24, 39);
    }
}
