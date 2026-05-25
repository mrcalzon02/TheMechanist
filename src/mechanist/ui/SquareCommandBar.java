package mechanist.ui;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;

/**
 * Two-column square command bar foundation for Phase 4 UI/presentation work.
 *
 * This intentionally replaces long single-column 3:1 command buttons with a compact
 * square-cell layout that can receive the new square GUI assets without stretching them.
 */
public final class SquareCommandBar extends JPanel {
    public static final int DEFAULT_COLUMNS = 2;
    public static final int DEFAULT_CELL_SIZE = 64;

    private final int columns;
    private final int cellSize;
    private final ArrayList<JButton> buttons = new ArrayList<>();

    public SquareCommandBar(List<SquareCommandAction> actions) {
        this(actions, DEFAULT_COLUMNS, DEFAULT_CELL_SIZE);
    }

    public SquareCommandBar(List<SquareCommandAction> actions, int columns, int cellSize) {
        super(new GridLayout(0, Math.max(1, columns), 6, 6));
        this.columns = Math.max(1, columns);
        this.cellSize = Math.max(32, cellSize);
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        setActions(actions == null ? List.of() : actions);
    }

    public void setActions(List<SquareCommandAction> actions) {
        removeAll();
        buttons.clear();
        for (SquareCommandAction action : actions == null ? List.<SquareCommandAction>of() : actions) {
            JButton button = buttonFor(action);
            buttons.add(button);
            add(button);
        }
        revalidate();
        repaint();
    }

    public List<JButton> buttons() {
        return List.copyOf(buttons);
    }

    public int columns() { return columns; }
    public int cellSize() { return cellSize; }

    private JButton buttonFor(SquareCommandAction action) {
        JButton b = new JButton(action.label(), action.icon());
        b.setName("command." + action.id());
        b.setEnabled(action.enabled());
        b.setToolTipText(htmlTooltip(action));
        b.setHorizontalTextPosition(JButton.CENTER);
        b.setVerticalTextPosition(JButton.BOTTOM);
        b.setFocusable(false);
        Dimension square = new Dimension(cellSize, cellSize);
        b.setMinimumSize(square);
        b.setPreferredSize(square);
        b.setMaximumSize(square);
        b.addActionListener(e -> {
            if (b.isEnabled()) action.action().run();
        });
        return b;
    }

    private static String htmlTooltip(SquareCommandAction action) {
        String label = escape(action.label());
        String text = escape(action.tooltip());
        if (text.isBlank()) return "<html><b>" + label + "</b></html>";
        return "<html><body style='width: 260px'><b>" + label + "</b><br>" + text + "</body></html>";
    }

    private static String escape(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
