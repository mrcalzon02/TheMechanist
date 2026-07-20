package mechanist;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.time.Duration;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Player-facing limited-alpha independent-host lobby.
 *
 * The window owns exactly one IndependentHostClientSupervisor at a time and
 * exposes only the certified lobby and relay control plane. It never constructs
 * GamePanel, SinglePlayerInternalHostSupervisor, or remote world authority.
 */
final class IndependentHostLobbyWindow implements AutoCloseable {
    static final String VERSION = "independent-host-lobby-window-2";
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(12);
    private static final Duration COMMAND_TIMEOUT = Duration.ofSeconds(8);

    private final RuntimeProfile startupProfile;
    private final JFrame frame = new JFrame(
            BuildIdentityAuthority.clientWindowTitle() + " — Independent Host Lobby");
    private final JTextField hostField = new JTextField(22);
    private final JTextField portField = new JTextField(6);
    private final JTextField serverKeyField = new JTextField(24);
    private final JTextField profileField = new JTextField(20);
    private final JButton connectButton = new JButton("Connect");
    private final JButton disconnectButton = new JButton("Disconnect");
    private final JButton refreshButton = new JButton("Refresh roster");
    private final JCheckBox readyBox = new JCheckBox("Ready");
    private final JComboBox<String> presenceBox = new JComboBox<>(
            new String[] {"available", "away", "busy"});
    private final JCheckBox typingBox = new JCheckBox("Typing");
    private final DefaultListModel<String> rosterModel = new DefaultListModel<>();
    private final JList<String> rosterList = new JList<>(rosterModel);
    private final JTextArea eventLog = new JTextArea();
    private final JTextArea statusArea = new JTextArea();
    private final JTextField relayField = new JTextField();
    private final JButton sendRelayButton = new JButton("Send relay message");
    private final JLabel boundaryLabel = new JLabel(
            "Limited alpha: authenticated lobby and relay only — no remote world gameplay.");
    private final ExecutorService ioExecutor = Executors.newSingleThreadExecutor(
            daemonThreadFactory("mechanist-remote-lobby-io"));
    private final Timer refreshTimer = new Timer(250, event -> refreshFromSupervisor());
    private final AtomicLong connectionEpoch = new AtomicLong();

    private volatile IndependentHostClientSupervisor supervisor;
    private volatile boolean closed;
    private long renderedRosterVersion = -1L;

    private IndependentHostLobbyWindow(RuntimeProfile startupProfile) {
        this.startupProfile = Objects.requireNonNull(startupProfile, "startupProfile");
        if (!startupProfile.remoteClientMode()) {
            throw new IllegalArgumentException(
                    "independent-host lobby requires REMOTE_CLIENT mode");
        }
        configureFrame();
        populateDefaults();
        bindActions();
        setConnectedControls(false);
        refreshTimer.setCoalesce(true);
        refreshTimer.start();
        appendEvent("Remote lobby initialized. " + auditSummary(startupProfile));
    }

    static void launch(RuntimeProfile profile) {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> launch(profile));
            return;
        }
        IndependentHostLobbyWindow window = new IndependentHostLobbyWindow(profile);
        window.frame.setVisible(true);
        window.frame.toFront();
        DebugLog.audit("INDEPENDENT_HOST_LOBBY", window.statusLine());
    }

    static String auditSummary(RuntimeProfile profile) {
        RuntimeProfile use = profile == null ? RuntimeProfile.defaultProfile() : profile;
        return "authority=" + VERSION
                + " mode=" + use.effectiveMode
                + " endpoint=" + use.remoteEndpointDisplay()
                + " mutableStorage=" + ClientMutableStorageAuthority.remoteClientRoot()
                + " handshakeOwner=IndependentHostClientSupervisor"
                + " rosterVisibility=connected-only"
                + " hostedCommands=ready,presence,chat-state"
                + " relayConsole=true"
                + " pendingConnectionCancellable=true"
                + " gamePanelMounted=false"
                + " internalHostMounted=false"
                + " worldCommandApi=false"
                + " worldAuthority=false";
    }

    private void configureFrame() {
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.setMinimumSize(new Dimension(900, 620));
        frame.setSize(new Dimension(1100, 760));
        frame.setLocationByPlatform(true);
        AppIconAuthority.applyTo(frame);

        JPanel root = new JPanel(new BorderLayout(10, 10));
        root.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        boundaryLabel.setFont(boundaryLabel.getFont().deriveFont(Font.BOLD));
        root.add(boundaryLabel, BorderLayout.NORTH);

        JSplitPane split = new JSplitPane(
                JSplitPane.HORIZONTAL_SPLIT,
                buildConnectionAndRosterPanel(),
                buildEventPanel());
        split.setResizeWeight(0.46);
        root.add(split, BorderLayout.CENTER);
        root.add(buildStatusPanel(), BorderLayout.SOUTH);
        frame.setContentPane(root);
        DisplayDensityAuthority.refreshSwingTree(frame);

        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent event) {
                close();
            }
        });
    }

    private JPanel buildConnectionAndRosterPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.add(buildConnectionPanel(), BorderLayout.NORTH);

        rosterList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane rosterScroll = new JScrollPane(rosterList);
        rosterScroll.setBorder(BorderFactory.createTitledBorder(
                "Authoritative connected-player roster"));
        panel.add(rosterScroll, BorderLayout.CENTER);

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT));
        controls.setBorder(BorderFactory.createTitledBorder("Hosted lobby state"));
        controls.add(readyBox);
        controls.add(new JLabel("Presence:"));
        controls.add(presenceBox);
        controls.add(typingBox);
        controls.add(refreshButton);
        panel.add(controls, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel buildConnectionPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Independent host connection"));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(3, 4, 3, 4);
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;

        addField(panel, c, 0, "Host", hostField);
        addField(panel, c, 1, "Port", portField);
        addField(panel, c, 2, "Server key", serverKeyField);
        addField(panel, c, 3, "Profile identity", profileField);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        buttons.add(connectButton);
        buttons.add(disconnectButton);
        c.gridx = 1;
        c.gridy = 4;
        c.weightx = 1.0;
        panel.add(buttons, c);
        return panel;
    }

    private static void addField(
            JPanel panel,
            GridBagConstraints c,
            int row,
            String label,
            JTextField field
    ) {
        c.gridx = 0;
        c.gridy = row;
        c.weightx = 0.0;
        panel.add(new JLabel(label + ":"), c);
        c.gridx = 1;
        c.weightx = 1.0;
        panel.add(field, c);
    }

    private JPanel buildEventPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        eventLog.setEditable(false);
        eventLog.setLineWrap(true);
        eventLog.setWrapStyleWord(true);
        JScrollPane scroll = new JScrollPane(eventLog);
        scroll.setBorder(BorderFactory.createTitledBorder(
                "Lobby and relay event stream"));
        panel.add(scroll, BorderLayout.CENTER);

        JPanel relay = new JPanel(new BorderLayout(6, 0));
        relay.setBorder(BorderFactory.createTitledBorder(
                "Authenticated relay message — not a gameplay command"));
        relay.add(relayField, BorderLayout.CENTER);
        relay.add(sendRelayButton, BorderLayout.EAST);
        panel.add(relay, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel buildStatusPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        statusArea.setEditable(false);
        statusArea.setLineWrap(true);
        statusArea.setWrapStyleWord(true);
        statusArea.setRows(4);
        statusArea.setText("Not connected. Credentials are stored outside the installation at "
                + ClientMutableStorageAuthority.resumeTokenRoot()
                + ". Resume tokens are never printed here.");
        JScrollPane scroll = new JScrollPane(statusArea);
        scroll.setBorder(BorderFactory.createTitledBorder("Interrogatable session status"));
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    private void populateDefaults() {
        hostField.setText(startupProfile.remoteHost);
        portField.setText(Integer.toString(startupProfile.remotePort));
        serverKeyField.setText(startupProfile.remoteServerKey);
        profileField.setText(startupProfile.profileName);
    }

    private void bindActions() {
        connectButton.addActionListener(event -> beginConnect());
        disconnectButton.addActionListener(event ->
                disconnect("user requested disconnect"));
        refreshButton.addActionListener(event -> submitOperation(
                "roster refresh",
                client -> client.requestRosterRefresh(COMMAND_TIMEOUT)));
        readyBox.addActionListener(event -> {
            boolean ready = readyBox.isSelected();
            submitOperation(
                    "readiness update",
                    client -> client.setReady(ready, COMMAND_TIMEOUT));
        });
        presenceBox.addActionListener(event -> {
            if (supervisor == null) return;
            String presence = Objects.toString(
                    presenceBox.getSelectedItem(),
                    "available");
            submitOperation(
                    "presence update",
                    client -> client.setPresence(presence, COMMAND_TIMEOUT));
        });
        typingBox.addActionListener(event -> {
            String chatState = typingBox.isSelected() ? "typing" : "idle";
            submitOperation(
                    "chat-state update",
                    client -> client.setChatState(chatState, COMMAND_TIMEOUT));
        });
        sendRelayButton.addActionListener(event -> sendRelay());
        relayField.addActionListener(event -> sendRelay());
    }

    private void beginConnect() {
        ConnectionRequest request;
        try {
            request = readConnectionRequest();
        } catch (RuntimeException failure) {
            showFailure("Invalid connection settings", failure);
            return;
        }
        long epoch = connectionEpoch.incrementAndGet();
        setConnectingControls();
        appendEvent("Connecting to " + request.host() + ":" + request.port()
                + " as profile " + request.profileIdentity() + ".");

        ioExecutor.execute(() -> {
            IndependentHostClientSupervisor next = null;
            try {
                IndependentHostResumeTokenStore tokenStore =
                        new IndependentHostResumeTokenStore(
                                ClientMutableStorageAuthority.resumeTokenRoot());
                next = new IndependentHostClientSupervisor(
                        tokenStore,
                        request.serverKey(),
                        request.profileIdentity());
                if (closed || connectionEpoch.get() != epoch) {
                    next.close();
                    return;
                }
                supervisor = next;
                IndependentHostClientSupervisor.ConnectionIdentity identity =
                        next.connect(request.host(), request.port(), CONNECT_TIMEOUT);
                if (closed || connectionEpoch.get() != epoch
                        || supervisor != next) {
                    next.close();
                    return;
                }
                IndependentHostClientSupervisor mounted = next;
                SwingUtilities.invokeLater(() -> {
                    if (closed || connectionEpoch.get() != epoch
                            || supervisor != mounted) {
                        mounted.close();
                        return;
                    }
                    setConnectedControls(true);
                    appendEvent("Authenticated " + identity.accessClass()
                            + " session for " + identity.playerId()
                            + "; generation=" + identity.connectionGeneration()
                            + "; resumed=" + identity.resumed() + ".");
                    refreshFromSupervisor();
                });
            } catch (Throwable failure) {
                if (next != null) next.close();
                if (supervisor == next) supervisor = null;
                if (connectionEpoch.get() == epoch && !closed) {
                    SwingUtilities.invokeLater(() -> {
                        setConnectedControls(false);
                        showFailure("Connection failed", failure);
                    });
                }
            }
        });
    }

    private ConnectionRequest readConnectionRequest() {
        String host = hostField.getText().trim();
        if (host.isBlank()
                || host.length() > 255
                || host.indexOf('|') >= 0
                || host.indexOf('\n') >= 0
                || host.indexOf('\r') >= 0) {
            throw new IllegalArgumentException("Host is blank or unsafe.");
        }
        int port;
        try {
            port = Integer.parseInt(portField.getText().trim());
        } catch (NumberFormatException failure) {
            throw new IllegalArgumentException("Port must be a number.", failure);
        }
        if (!NetworkPortAuthority.portWithinAllowedGameRange(port)) {
            throw new IllegalArgumentException(
                    "Port must be within "
                            + NetworkPortAuthority.CUSTOM_GAME_PORT_MIN
                            + "-"
                            + NetworkPortAuthority.CUSTOM_GAME_PORT_MAX
                            + " and outside reserved ranges.");
        }
        String profile = profileField.getText().trim();
        if (profile.length() < 8 || profile.length() > 128
                || !profile.matches("[A-Za-z0-9._:-]+")) {
            throw new IllegalArgumentException(
                    "Profile identity must be 8-128 characters using letters, numbers, dot, underscore, colon, or dash.");
        }
        String serverKey = serverKeyField.getText().trim();
        if (serverKey.isBlank()) {
            serverKey = host.toLowerCase(Locale.ROOT) + ":" + port;
            serverKeyField.setText(serverKey);
        }
        if (serverKey.length() > 256
                || serverKey.indexOf('|') >= 0
                || serverKey.indexOf('\n') >= 0
                || serverKey.indexOf('\r') >= 0) {
            throw new IllegalArgumentException("Server key is unsafe.");
        }
        return new ConnectionRequest(host, port, serverKey, profile);
    }

    private void sendRelay() {
        String message = relayField.getText().trim();
        if (message.isBlank()) return;
        submitOperation("relay send", client -> {
            long sequence = client.sendRelayPayload(message);
            SwingUtilities.invokeLater(() -> {
                relayField.setText("");
                appendEvent("You relayed [" + sequence + "]: " + message);
            });
            return sequence;
        });
    }

    private <T> void submitOperation(
            String label,
            ClientOperation<T> operation
    ) {
        IndependentHostClientSupervisor mounted = supervisor;
        if (mounted == null
                || (mounted.state() != IndependentHostClientSupervisor.State.AUTHENTICATED
                && mounted.state()
                != IndependentHostClientSupervisor.State.AUTHENTICATED_VOLATILE_TOKEN)) {
            appendEvent("Cannot perform " + label
                    + ": no authenticated session.");
            return;
        }
        ioExecutor.execute(() -> {
            try {
                T result = operation.execute(mounted);
                SwingUtilities.invokeLater(() -> {
                    appendEvent(label + " accepted"
                            + (result == null
                            ? "."
                            : ": " + safeText(result.toString())));
                    refreshFromSupervisor();
                });
            } catch (Throwable failure) {
                SwingUtilities.invokeLater(() -> {
                    appendEvent(label + " failed: "
                            + safeText(failure.getMessage()));
                    refreshFromSupervisor();
                });
            }
        });
    }

    private void disconnect(String reason) {
        connectionEpoch.incrementAndGet();
        IndependentHostClientSupervisor mounted = supervisor;
        supervisor = null;
        if (mounted != null) mounted.close();
        renderedRosterVersion = -1L;
        rosterModel.clear();
        setConnectedControls(false);
        appendEvent(reason + ".");
        statusArea.setText("Disconnected. "
                + ClientMutableStorageAuthority.auditSummary());
    }

    private void refreshFromSupervisor() {
        if (closed) return;
        IndependentHostClientSupervisor mounted = supervisor;
        if (mounted == null) return;

        statusArea.setText(mounted.statusLine()
                + "\nmutableStorage="
                + ClientMutableStorageAuthority.remoteClientRoot()
                + "\ncredentialDisclosure=resume token never rendered"
                + "\ncapabilityBoundary=lobby-and-relay-only; worldAuthority=false");

        HostedRosterClientAuthority.Snapshot roster = mounted.latestRoster();
        if (roster != null && roster.version() != renderedRosterVersion) {
            renderedRosterVersion = roster.version();
            rosterModel.clear();
            String localPlayer = mounted.identity() == null
                    ? ""
                    : mounted.identity().playerId();
            for (HostedRosterClientAuthority.Entry entry : roster.entries()) {
                String row = entry.playerId()
                        + (entry.playerId().equals(localPlayer) ? " [you]" : "")
                        + " | " + (entry.ready() ? "READY" : "not ready")
                        + " | " + entry.presence()
                        + " | " + entry.chatState()
                        + " | generation " + entry.connectionGeneration()
                        + " | hosted commands "
                        + entry.acceptedHostedCommands();
                rosterModel.addElement(row);
            }
            appendEvent("Accepted authoritative roster version "
                    + roster.version()
                    + " for world " + roster.worldId()
                    + "; visible players=" + roster.visiblePlayers() + ".");
        }

        for (int count = 0; count < 32; count++) {
            try {
                IndependentHostClientSupervisor.RelayFrame relay =
                        mounted.pollRelay(Duration.ofNanos(1));
                if (relay == null) break;
                appendEvent("Peer relay [" + relay.sequence()
                        + "]: " + relay.payload());
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        IndependentHostClientSupervisor.State current = mounted.state();
        if (current == IndependentHostClientSupervisor.State.FAILED) {
            setConnectedControls(false);
            appendEvent("Session failed closed. Inspect status and reconnect explicitly.");
        }
    }

    private void setConnectingControls() {
        connectButton.setEnabled(false);
        disconnectButton.setEnabled(true);
        hostField.setEnabled(false);
        portField.setEnabled(false);
        serverKeyField.setEnabled(false);
        profileField.setEnabled(false);
        setLobbyControls(false);
        statusArea.setText(
                "Connecting. No world or local GamePanel has been mounted.");
    }

    private void setConnectedControls(boolean connected) {
        connectButton.setEnabled(!connected);
        disconnectButton.setEnabled(connected);
        hostField.setEnabled(!connected);
        portField.setEnabled(!connected);
        serverKeyField.setEnabled(!connected);
        profileField.setEnabled(!connected);
        setLobbyControls(connected);
    }

    private void setLobbyControls(boolean enabled) {
        readyBox.setEnabled(enabled);
        presenceBox.setEnabled(enabled);
        typingBox.setEnabled(enabled);
        refreshButton.setEnabled(enabled);
        relayField.setEnabled(enabled);
        sendRelayButton.setEnabled(enabled);
    }

    private void showFailure(String title, Throwable failure) {
        String message = safeText(
                failure == null ? "unknown failure" : failure.getMessage());
        appendEvent(title + ": " + message);
        statusArea.setText(title + ": " + message
                + "\nThe session failed closed; no world authority was mounted.");
        if (!closed && frame.isDisplayable()) {
            JOptionPane.showMessageDialog(
                    frame,
                    message,
                    title,
                    JOptionPane.ERROR_MESSAGE);
        }
        DebugLog.error(
                "INDEPENDENT_HOST_LOBBY",
                title + ": " + message,
                failure);
    }

    private void appendEvent(String text) {
        String safe = safeText(text);
        eventLog.append(safe + System.lineSeparator());
        eventLog.setCaretPosition(eventLog.getDocument().getLength());
    }

    String statusLine() {
        IndependentHostClientSupervisor mounted = supervisor;
        return "authority=" + VERSION
                + " closed=" + closed
                + " connected=" + (mounted != null)
                + " supervisor="
                + (mounted == null ? "none" : mounted.statusLine())
                + " storage="
                + ClientMutableStorageAuthority.remoteClientRoot()
                + " gamePanelMounted=false"
                + " internalHostMounted=false"
                + " worldAuthority=false";
    }

    @Override
    public void close() {
        if (closed) return;
        closed = true;
        refreshTimer.stop();
        disconnect("window closed");
        ioExecutor.shutdownNow();
        frame.dispose();
        DebugLog.audit("INDEPENDENT_HOST_LOBBY_CLOSE", statusLine());
        DebugLog.shutdown("independent-host lobby closed");
    }

    private static ThreadFactory daemonThreadFactory(String name) {
        return runnable -> {
            Thread thread = new Thread(runnable, name);
            thread.setDaemon(true);
            return thread;
        };
    }

    private static String safeText(String value) {
        String text = Objects.requireNonNullElse(value, "unspecified")
                .replace('\n', ' ')
                .replace('\r', ' ')
                .trim();
        return text.isBlank()
                ? "unspecified"
                : text.substring(0, Math.min(600, text.length()));
    }

    private record ConnectionRequest(
            String host,
            int port,
            String serverKey,
            String profileIdentity
    ) { }

    @FunctionalInterface
    private interface ClientOperation<T> {
        T execute(IndependentHostClientSupervisor client) throws Exception;
    }
}
