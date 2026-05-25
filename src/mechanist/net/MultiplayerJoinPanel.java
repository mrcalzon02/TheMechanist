package mechanist.net;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Objects;
import java.util.function.Consumer;

public final class MultiplayerJoinPanel extends JPanel {
    private final JTextField serverAddress = new JTextField("127.0.0.1", 24);
    private final JSpinner port = new JSpinner(new SpinnerNumberModel(25500, 1, 65535, 1));
    private final JTextField playerName = new JTextField("", 24);
    private final JPasswordField password = new JPasswordField("", 24);
    private final JCheckBox streamSafe = new JCheckBox("Stream safe address display", true);
    private final JLabel status = new JLabel("Enter server details. Address/password fields are protected for streaming by default.");

    public MultiplayerJoinPanel(Consumer<MultiplayerJoinRequest> onJoin) {
        super(new BorderLayout(8, 8));
        Objects.requireNonNull(onJoin, "onJoin");
        add(buildForm(), BorderLayout.CENTER);
        add(buildFooter(onJoin), BorderLayout.SOUTH);
    }

    private JPanel buildForm() {
        JPanel p = new JPanel(new GridBagLayout());
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(6, 6, 6, 6);
        g.fill = GridBagConstraints.HORIZONTAL;
        g.gridx = 0;
        g.gridy = 0;
        g.weightx = 0;
        p.add(new JLabel("Server"), g);
        g.gridx = 1;
        g.weightx = 1;
        serverAddress.setToolTipText("Server address. Keep stream-safe enabled when showing this screen on video or screenshots.");
        p.add(serverAddress, g);
        g.gridx = 2;
        g.weightx = 0;
        p.add(streamSafe, g);

        g.gridx = 0;
        g.gridy++;
        p.add(new JLabel("Port"), g);
        g.gridx = 1;
        g.gridwidth = 2;
        p.add(port, g);
        g.gridwidth = 1;

        g.gridx = 0;
        g.gridy++;
        p.add(new JLabel("Player"), g);
        g.gridx = 1;
        g.gridwidth = 2;
        p.add(playerName, g);
        g.gridwidth = 1;

        g.gridx = 0;
        g.gridy++;
        p.add(new JLabel("Password"), g);
        g.gridx = 1;
        g.gridwidth = 2;
        p.add(StreamSafeTextFields.passwordField("", password), g);
        g.gridwidth = 1;

        streamSafe.addActionListener(e -> updateStreamSafeVisual());
        updateStreamSafeVisual();
        return p;
    }

    private JPanel buildFooter(Consumer<MultiplayerJoinRequest> onJoin) {
        JPanel p = new JPanel(new BorderLayout(8, 8));
        JButton join = new JButton("Join Server");
        join.setToolTipText("Create a join request. Connection authority will validate and handle the request when networking is active.");
        join.addActionListener(e -> {
            MultiplayerJoinRequest request = request();
            status.setText("Join request prepared: " + request.redactedSummary());
            onJoin.accept(request);
        });
        p.add(status, BorderLayout.CENTER);
        p.add(join, BorderLayout.EAST);
        return p;
    }

    public MultiplayerJoinRequest request() {
        return new MultiplayerJoinRequest(realAddress(), (Integer) port.getValue(), playerName.getText(), password.getPassword(), streamSafe.isSelected());
    }

    private String realAddress() {
        Object stored = serverAddress.getClientProperty("mechanist.realAddress");
        return stored == null ? serverAddress.getText() : String.valueOf(stored);
    }

    private void updateStreamSafeVisual() {
        if (streamSafe.isSelected()) {
            serverAddress.putClientProperty("mechanist.realAddress", serverAddress.getText());
            serverAddress.setText(StreamSafeTextFields.redactAddress(String.valueOf(serverAddress.getClientProperty("mechanist.realAddress"))));
            serverAddress.setEditable(false);
        } else {
            Object stored = serverAddress.getClientProperty("mechanist.realAddress");
            if (stored != null) serverAddress.setText(String.valueOf(stored));
            serverAddress.setEditable(true);
        }
    }
}
