package mechanist.ui;

import javax.swing.Icon;
import java.util.Objects;

public final class SquareCommandAction {
    private final String id;
    private final String label;
    private final String tooltip;
    private final Icon icon;
    private final Runnable action;
    private final boolean enabled;

    public SquareCommandAction(String id, String label, String tooltip, Icon icon, Runnable action) {
        this(id, label, tooltip, icon, action, true);
    }

    public SquareCommandAction(String id, String label, String tooltip, Icon icon, Runnable action, boolean enabled) {
        this.id = requireText(id, "id");
        this.label = requireText(label, "label");
        this.tooltip = tooltip == null ? "" : tooltip;
        this.icon = icon;
        this.action = Objects.requireNonNull(action, "action");
        this.enabled = enabled;
    }

    public String id() { return id; }
    public String label() { return label; }
    public String tooltip() { return tooltip; }
    public Icon icon() { return icon; }
    public Runnable action() { return action; }
    public boolean enabled() { return enabled; }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(name + " is required");
        return value.trim();
    }
}
