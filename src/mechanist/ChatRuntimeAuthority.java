package mechanist;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.KeyPair;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/** Owns the bounded, sanitized desktop chat surface and local chat logging. */
final class ChatRuntimeAuthority {
    static final String VERSION = "chat-runtime-authority-0.9.10hr";
    static final int MAX_MESSAGE_CHARS = 256;
    static final int MAX_LOG_LINES_IN_UI = 500;
    static final long MIN_SEND_INTERVAL_MS = 900L;
    static final long DUPLICATE_SUPPRESSION_MS = 5_000L;
    static final long MAX_LOG_BYTES_BEFORE_ROTATE = 1_000_000L;

    private final GamePanel game;
    private final AsyncChatLogger logger;
    private final HybridEncryptionManager encryptionManager;
    private final KeyPair localChatIdentity;
    private JFrame window;
    private JTextArea chatLog;
    private JTextField input;
    private final Deque<String> visibleLines = new ArrayDeque<>();
    private long lastSendMillis = 0L;
    private String lastSanitizedMessage = "";
    private long lastDuplicateMillis = 0L;

    ChatRuntimeAuthority(GamePanel game) {
        this.game = game;
        this.logger = new AsyncChatLogger(Paths.get("logs", "chat.log"));
        this.encryptionManager = new HybridEncryptionManager();
        this.localChatIdentity = createLocalChatIdentity(this.encryptionManager);
    }

    private static KeyPair createLocalChatIdentity(HybridEncryptionManager manager) {
        try {
            return manager.generateIdentityKeyPair();
        } catch (HybridEncryptionManager.SecureChatCryptoException e) {
            DebugLog.error("CHAT_E2EE_IDENTITY", "Could not generate local RSA chat identity.", e);
            return null;
        }
    }

    void openOrFocus() {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(this::openOrFocus);
            return;
        }
        if (window != null && window.isDisplayable()) {
            window.setVisible(true);
            window.toFront();
            input.requestFocusInWindow();
            return;
        }
        window = new JFrame("The Mechanist — Chat");
        window.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        window.setAlwaysOnTop(false);
        window.setSize(520, 360);
        window.setLocationRelativeTo(game);
        window.setLayout(new BorderLayout(8, 8));

        JPanel root = new JPanel(new BorderLayout(8, 8));
        root.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        window.setContentPane(root);

        JLabel header = new JLabel("Chat — Enter sends, Esc or clicking away closes. Messages are capped at 256 characters.");
        header.setFont(header.getFont().deriveFont(Font.BOLD));
        root.add(header, BorderLayout.NORTH);

        chatLog = new JTextArea();
        chatLog.setEditable(false);
        chatLog.setLineWrap(true);
        chatLog.setWrapStyleWord(true);
        chatLog.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        root.add(new JScrollPane(chatLog), BorderLayout.CENTER);

        input = new JTextField();
        ((AbstractDocument) input.getDocument()).setDocumentFilter(new BoundedSanitizingDocumentFilter(MAX_MESSAGE_CHARS));
        JButton send = new JButton("Send");
        send.addActionListener(e -> sendCurrentInput());
        input.addActionListener(e -> sendCurrentInput());

        JPanel row = new JPanel(new BorderLayout(6, 0));
        row.add(input, BorderLayout.CENTER);
        row.add(send, BorderLayout.EAST);
        root.add(row, BorderLayout.SOUTH);

        KeyStroke esc = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
        root.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(esc, "close-chat");
        root.getActionMap().put("close-chat", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { closeWindow(); }
        });
        window.addWindowFocusListener(new WindowAdapter() {
            @Override public void windowLostFocus(WindowEvent e) { closeWindow(); }
        });
        window.addWindowListener(new WindowAdapter() {
            @Override public void windowClosed(WindowEvent e) { game.requestFocusInWindow(); }
        });
        appendSystemLine("Local chat surface ready. Network broadcast uses E2EE packets; the future server relay receives opaque ciphertext only.");
        window.setVisible(true);
        input.requestFocusInWindow();
    }

    void closeWindow() {
        if (window != null && window.isDisplayable()) {
            window.dispose();
        }
        if (game != null) game.requestFocusInWindow();
    }

    private void sendCurrentInput() {
        String sanitized = ChatSecurity.sanitizeChatText(input.getText());
        if (sanitized.isBlank()) {
            input.setText("");
            return;
        }
        long now = System.currentTimeMillis();
        if (now - lastSendMillis < MIN_SEND_INTERVAL_MS) {
            appendSystemLine("Message held: sending too quickly.");
            return;
        }
        if (sanitized.equalsIgnoreCase(lastSanitizedMessage) && now - lastDuplicateMillis < DUPLICATE_SUPPRESSION_MS) {
            appendSystemLine("Message held: duplicate resend suppressed.");
            return;
        }
        lastSendMillis = now;
        if (sanitized.equalsIgnoreCase(lastSanitizedMessage)) lastDuplicateMillis = now;
        else { lastSanitizedMessage = sanitized; lastDuplicateMillis = now; }

        ChatOutboundPayload payload = payloadFor(sanitized, now);
        if (payload.relayPacket() == null) {
            appendSystemLine("Message held: secure chat identity is unavailable.");
            return;
        }
        appendVisibleLine(payload.displayLine());
        logger.append(payload.localLogLine());
        input.setText("");
        DebugLog.audit("CHAT_E2EE_PAYLOAD", "prepared opaque chat relay packet chars=" + sanitized.length()
                + " provider=" + payload.provider()
                + " " + payload.relayPacket().safeRelayLogLine());
    }

    private ChatOutboundPayload payloadFor(String message, long nowMillis) {
        UserProfileAuthority.Profile profile = game == null ? UserProfileAuthority.detect() : game.userProfile;
        String provider = profile == null ? "Internal" : profile.provider;
        String installationId = profile == null ? "MECH-UNKNOWN" : profile.identifier;
        String name = displayName();
        int level = game == null ? 1 : Math.max(1, 1 + (game.xp / 100));
        SecureChatPacket relayPacket = null;
        if (localChatIdentity != null) {
            try {
                relayPacket = encryptionManager.encryptForRecipient(installationId, installationId, localChatIdentity.getPublic(), message);
            } catch (HybridEncryptionManager.SecureChatCryptoException e) {
                DebugLog.error("CHAT_E2EE_PAYLOAD", "Could not encrypt local chat payload for relay packet.", e);
            }
        }
        return new ChatOutboundPayload(installationId, provider, name, level, message, Instant.ofEpochMilli(nowMillis).toString(), relayPacket);
    }

    private String displayName() {
        if (game != null && game.active != null && game.active.name != null && !game.active.name.isBlank()) {
            return ChatSecurity.sanitizeDisplayName(game.active.name);
        }
        if (game != null && game.userProfile != null) return ChatSecurity.sanitizeDisplayName(game.userProfile.displayName);
        return "Local Operator";
    }

    private void appendSystemLine(String message) {
        appendVisibleLine("[System] " + ChatSecurity.sanitizeChatText(message));
    }

    private void appendVisibleLine(String line) {
        visibleLines.addLast(line);
        while (visibleLines.size() > MAX_LOG_LINES_IN_UI) visibleLines.removeFirst();
        if (chatLog != null) {
            StringBuilder sb = new StringBuilder();
            for (String l : visibleLines) sb.append(l).append('\n');
            chatLog.setText(sb.toString());
            chatLog.setCaretPosition(chatLog.getDocument().getLength());
        }
    }

    void shutdown() { logger.shutdown(); }

    static String auditSummary() {
        return "authority=" + VERSION + " maxChars=" + MAX_MESSAGE_CHARS + " uiLines=" + MAX_LOG_LINES_IN_UI + " rateMs=" + MIN_SEND_INTERVAL_MS + " duplicateMs=" + DUPLICATE_SUPPRESSION_MS + " crypto={" + HybridEncryptionManager.auditSummary() + "}";
    }

    record ChatOutboundPayload(String installationId, String provider, String displayName, int level, String message, String isoTimestamp, SecureChatPacket relayPacket) {
        ChatOutboundPayload {
            installationId = ChatSecurity.sanitizeIdentifier(installationId);
            provider = ChatSecurity.sanitizeIdentifier(provider);
            displayName = ChatSecurity.sanitizeDisplayName(displayName);
            level = Math.max(1, Math.min(9999, level));
            message = ChatSecurity.sanitizeChatText(message);
            isoTimestamp = ChatSecurity.sanitizeIdentifier(isoTimestamp);
        }
        String displayLine() { return "[Level " + level + "] " + displayName + ": " + message; }
        String localLogLine() {
            String relay = relayPacket == null ? "e2ee=unavailable" : relayPacket.safeRelayLogLine();
            return isoTimestamp + " provider=" + provider + " installation=" + installationId + " display=" + displayName + " level=" + level + " " + relay;
        }
    }

    static final class ChatSecurity {
        private ChatSecurity() {}

        static String sanitizeChatText(String input) {
            if (input == null) return "";
            StringBuilder out = new StringBuilder(Math.min(input.length(), MAX_MESSAGE_CHARS));
            for (int i = 0; i < input.length() && out.length() < MAX_MESSAGE_CHARS; i++) {
                char c = input.charAt(i);
                if (c == '\n' || c == '\r' || Character.isISOControl(c)) continue;
                switch (c) {
                    case '<' -> out.append('‹');
                    case '>' -> out.append('›');
                    case '&' -> out.append('＆');
                    case '"' -> out.append('”');
                    case '\'' -> out.append('’');
                    default -> out.append(c);
                }
            }
            return out.toString().trim().replaceAll("\\s{2,}", " ");
        }

        static String sanitizeDisplayName(String input) {
            String s = sanitizeChatText(input);
            if (s.isBlank()) return "Local Operator";
            return s.length() > 40 ? s.substring(0, 40) : s;
        }

        static String sanitizeLogLine(String input) {
            if (input == null) return "";
            StringBuilder out = new StringBuilder(Math.min(input.length(), 1024));
            for (int i = 0; i < input.length() && out.length() < 1024; i++) {
                char c = input.charAt(i);
                if (c == '\n' || c == '\r' || Character.isISOControl(c)) continue;
                switch (c) {
                    case '<' -> out.append('‹');
                    case '>' -> out.append('›');
                    case '&' -> out.append('＆');
                    case '"' -> out.append('”');
                    case '\'' -> out.append('’');
                    default -> out.append(c);
                }
            }
            return out.toString().trim().replaceAll("\\s{2,}", " ");
        }

        static String sanitizeIdentifier(String input) {
            if (input == null) return "UNKNOWN";
            String cleaned = input.replaceAll("[^A-Za-z0-9_.:@/-]", "_");
            if (cleaned.isBlank()) return "UNKNOWN";
            return cleaned.length() > 96 ? cleaned.substring(0, 96) : cleaned;
        }
    }

    static final class BoundedSanitizingDocumentFilter extends DocumentFilter {
        private final int maxChars;
        BoundedSanitizingDocumentFilter(int maxChars) { this.maxChars = Math.max(1, maxChars); }
        @Override public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr) throws BadLocationException {
            replace(fb, offset, 0, string, attr);
        }
        @Override public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
            String sanitized = ChatSecurity.sanitizeChatText(text);
            int current = fb.getDocument().getLength();
            int allowed = maxChars - (current - length);
            if (allowed <= 0) return;
            if (sanitized.length() > allowed) sanitized = sanitized.substring(0, allowed);
            if (!sanitized.isEmpty()) super.replace(fb, offset, length, sanitized, attrs);
            else if (length > 0) super.replace(fb, offset, length, "", attrs);
        }
    }

    static final class AsyncChatLogger {
        private final Path logPath;
        private final ExecutorService executor;
        AsyncChatLogger(Path logPath) {
            this.logPath = Objects.requireNonNull(logPath);
            this.executor = Executors.newSingleThreadExecutor(new ThreadFactory() {
                @Override public Thread newThread(Runnable r) {
                    Thread t = new Thread(r, "mechanist-chat-logger");
                    t.setDaemon(true);
                    return t;
                }
            });
        }
        void append(String line) {
            String safe = ChatSecurity.sanitizeLogLine(line);
            executor.submit(() -> {
                try {
                    Files.createDirectories(logPath.getParent());
                    rotateIfNeeded();
                    Files.writeString(logPath, safe + System.lineSeparator(), StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                } catch (IOException e) {
                    DebugLog.warn("CHAT_LOG", "Could not append chat log: " + e.getMessage());
                }
            });
        }
        private void rotateIfNeeded() throws IOException {
            if (!Files.exists(logPath) || Files.size(logPath) < MAX_LOG_BYTES_BEFORE_ROTATE) return;
            Path rotated = logPath.resolveSibling("chat-previous.log");
            Files.move(logPath, rotated, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        }
        void shutdown() { executor.shutdownNow(); }
    }

    private ChatRuntimeAuthority() { this.game = null; this.logger = null; this.encryptionManager = null; this.localChatIdentity = null; }
}
