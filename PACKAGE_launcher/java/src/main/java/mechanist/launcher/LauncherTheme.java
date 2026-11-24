package mechanist.launcher;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.Border;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.Dimension;

final class LauncherTheme {
    static final Color VOID = new Color(18, 18, 20);
    static final Color PANEL = new Color(30, 31, 34);
    static final Color PANEL_DEEP = new Color(12, 14, 14);
    static final Color PANEL_EDGE = new Color(76, 72, 60);
    static final Color TEXT = new Color(226, 220, 198);
    static final Color TEXT_DIM = new Color(170, 168, 158);
    static final Color TEXT_MUTED = new Color(130, 132, 126);
    static final Color CRT = new Color(178, 238, 190);
    static final Color WARNING = new Color(238, 190, 118);

    static final Font TITLE = new Font(Font.SERIF, Font.BOLD, 34);
    static final Font SECTION = new Font(Font.SANS_SERIF, Font.BOLD, 14);
    static final Font BODY = new Font(Font.SANS_SERIF, Font.PLAIN, 12);
    static final Font MONO = new Font(Font.MONOSPACED, Font.PLAIN, 12);

    private LauncherTheme() {}

    static JPanel rootPanel() {
        JPanel p = new JPanel(new BorderLayout(14, 14));
        p.setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18));
        p.setBackground(VOID);
        return p;
    }

    static JPanel panel() {
        JPanel p = new JPanel(new BorderLayout(8, 8));
        p.setOpaque(true);
        p.setBackground(PANEL);
        p.setBorder(frameBorder());
        p.setMinimumSize(new Dimension(0, 0));
        return p;
    }

    static Border frameBorder() {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(PANEL_EDGE),
                BorderFactory.createEmptyBorder(10, 12, 10, 12));
    }

    static JLabel title(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(TEXT);
        l.setFont(TITLE);
        return l;
    }

    static JLabel subtitle(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(TEXT_DIM);
        l.setFont(BODY);
        return l;
    }

    static JLabel label(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(TEXT_DIM);
        l.setFont(BODY);
        return l;
    }

    static JLabel value(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(TEXT);
        l.setFont(MONO);
        l.setMinimumSize(new Dimension(0, l.getPreferredSize().height));
        l.setToolTipText(text);
        return l;
    }

    static JTextField path(String text) {
        JTextField field = new JTextField(text, 12);
        field.setEditable(false);
        field.setBorder(BorderFactory.createEmptyBorder());
        field.setBackground(PANEL);
        field.setForeground(TEXT);
        field.setFont(MONO);
        field.setCaretPosition(0);
        field.setMinimumSize(new Dimension(0, field.getPreferredSize().height));
        field.setToolTipText(text);
        return field;
    }

    static JTextArea notice(String text, int rows) {
        JTextArea area = new JTextArea(text, rows, 20);
        area.setEditable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setOpaque(false);
        area.setForeground(TEXT_DIM);
        area.setFont(BODY);
        area.setMinimumSize(new Dimension(0, area.getPreferredSize().height));
        return area;
    }

    static JTextArea logArea() {
        JTextArea log = new JTextArea(16, 80);
        log.setEditable(false);
        log.setLineWrap(true);
        log.setWrapStyleWord(true);
        log.setBackground(PANEL_DEEP);
        log.setForeground(CRT);
        log.setFont(MONO);
        return log;
    }

    static void progress(JProgressBar bar) {
        bar.setStringPainted(true);
        bar.setString("Idle");
        bar.setToolTipText("Shows install, update, repair, and launch progress without taking over the whole launcher window.");
    }

    static void tooltip(JComponent c, String text) {
        c.setToolTipText("<html><body style='width: 320px'>" + text + "</body></html>");
    }
}
