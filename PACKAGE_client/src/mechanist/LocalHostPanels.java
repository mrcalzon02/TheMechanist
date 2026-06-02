package mechanist;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JSlider;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/** Swing settings surface for the integrated local host authentication gate. */
final class LocalHostSettingsPanel extends JPanel {
    private final LocalHostAuthGate authGate;
    private final JComboBox<LocalHostAuthGate.AuthMode> modeBox = new JComboBox<>(LocalHostAuthGate.AuthMode.values());
    private final JPasswordField passwordField = new JPasswordField(18);
    private final JTextField hostKeyField = new JTextField(10);
    private final JLabel statusLabel = new JLabel();
    private final GameOptions options;
    private final JSlider doomFovSlider = new JSlider(60, 110, 80);

    LocalHostSettingsPanel(LocalHostAuthGate authGate) {
        this(authGate, GameOptions.load());
    }

    LocalHostSettingsPanel(LocalHostAuthGate authGate, GameOptions options) {
        super(new GridBagLayout());
        this.authGate = Objects.requireNonNull(authGate, "authGate");
        this.options = options == null ? GameOptions.load() : options;
        setBorder(BorderFactory.createTitledBorder("Local Host Security Gate"));
        hostKeyField.setEditable(false);
        JButton generate = new JButton("Generate Host Key");
        generate.addActionListener(e -> { authGate.generateHostKey(); refresh(); });
        JButton apply = new JButton("Apply Authentication Mode");
        apply.addActionListener(e -> applyConfig());
        doomFovSlider.setMajorTickSpacing(10);
        doomFovSlider.setMinorTickSpacing(5);
        doomFovSlider.setPaintTicks(true);
        doomFovSlider.setPaintLabels(true);
        doomFovSlider.setValue(Math.max(60, Math.min(110, this.options.doomModeFovDegrees)));
        doomFovSlider.addChangeListener(e -> {
            if (!doomFovSlider.getValueIsAdjusting()) {
                this.options.doomModeFovDegrees = Math.max(60, Math.min(110, doomFovSlider.getValue()));
                this.options.save();
                statusLabel.setText(authGate.publicStatusLine() + " | doom FOV " + this.options.doomModeFovDegrees + "°");
            }
        });
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 4, 4, 4); c.anchor = GridBagConstraints.WEST;
        c.gridx = 0; c.gridy = 0; add(new JLabel("Mode"), c); c.gridx = 1; add(modeBox, c);
        c.gridx = 0; c.gridy = 1; add(new JLabel("Password"), c); c.gridx = 1; add(passwordField, c);
        c.gridx = 0; c.gridy = 2; add(new JLabel("Host Key"), c); c.gridx = 1; add(hostKeyField, c); c.gridx = 2; add(generate, c);
        c.gridx = 1; c.gridy = 3; add(apply, c);
        c.gridx = 0; c.gridy = 4; add(new JLabel("doom FOV"), c); c.gridx = 1; c.gridwidth = 2; c.fill = GridBagConstraints.HORIZONTAL; add(doomFovSlider, c); c.fill = GridBagConstraints.NONE; c.gridwidth = 1;
        c.gridx = 0; c.gridy = 5; c.gridwidth = 3; add(statusLabel, c);
        refresh();
    }

    void applyConfig() {
        LocalHostAuthGate.AuthMode mode = (LocalHostAuthGate.AuthMode) modeBox.getSelectedItem();
        if (mode == LocalHostAuthGate.AuthMode.PASSWORD) authGate.usePassword(new String(passwordField.getPassword()));
        else authGate.generateHostKey();
        refresh();
    }

    void refresh() {
        LocalHostAuthGate.AuthConfig cfg = authGate.currentConfig();
        modeBox.setSelectedItem(cfg.mode());
        hostKeyField.setText(cfg.mode() == LocalHostAuthGate.AuthMode.HOST_KEY ? cfg.secret() : "hidden");
        statusLabel.setText(authGate.publicStatusLine() + " | doom FOV " + Math.max(60, Math.min(110, options.doomModeFovDegrees)) + "°");
    }
}

/** Host-only Swing overlay showing credentials, NAT profile, Steam/direct transport status, roster, and kick controls. */
final class HostDashboardOverlay extends JPanel {
    record HostRosterEntry(String sessionId, String displayName, String address, boolean host, String state) {
        String line() { return (host ? "HOST " : "CLIENT ") + displayName + " / " + address + " / " + state; }
    }

    private final LocalHostAuthGate authGate;
    private final DefaultListModel<HostRosterEntry> rosterModel = new DefaultListModel<>();
    private final JList<HostRosterEntry> roster = new JList<>(rosterModel);
    private final JTextArea profile = new JTextArea(4, 36);
    private Consumer<HostRosterEntry> kickHandler = entry -> DebugLog.audit("HOST_DASHBOARD_KICK", "kick requested: " + entry.sessionId());

    HostDashboardOverlay(LocalHostAuthGate authGate) {
        super(new BorderLayout(8, 8));
        this.authGate = Objects.requireNonNull(authGate, "authGate");
        setBorder(BorderFactory.createTitledBorder("Local Host Management"));
        profile.setEditable(false);
        roster.setCellRenderer((list, value, index, selected, focus) -> new JLabel(value == null ? "" : value.line()));
        JButton kick = new JButton("Sever Connection / Kick");
        kick.addActionListener(e -> {
            HostRosterEntry selected = roster.getSelectedValue();
            if (selected != null && !selected.host()) kickHandler.accept(selected);
        });
        add(profile, BorderLayout.NORTH);
        add(new JScrollPane(roster), BorderLayout.CENTER);
        add(kick, BorderLayout.SOUTH);
        updateConnectionProfile(null, null, "No active host binding yet.");
    }

    void setKickHandler(Consumer<HostRosterEntry> handler) { this.kickHandler = handler == null ? this.kickHandler : handler; }

    void updateConnectionProfile(NatDiscoveryResult nat, String steamLobbyId, String transportLine) {
        StringBuilder sb = new StringBuilder();
        sb.append(authGate.publicStatusLine()).append('\n');
        sb.append("Transport: ").append(transportLine == null ? "not bound" : transportLine).append('\n');
        if (steamLobbyId != null && !steamLobbyId.isBlank()) sb.append("Steam lobby/session: ").append(steamLobbyId).append('\n');
        else sb.append("NAT: ").append(nat == null ? "not discovered" : nat.display()).append('\n');
        profile.setText(sb.toString());
    }

    void setRoster(List<HostRosterEntry> entries) {
        Runnable update = () -> {
            rosterModel.clear();
            for (HostRosterEntry entry : new ArrayList<>(Objects.requireNonNullElse(entries, List.of()))) rosterModel.addElement(entry);
        };
        if (SwingUtilities.isEventDispatchThread()) update.run(); else SwingUtilities.invokeLater(update);
    }
}
