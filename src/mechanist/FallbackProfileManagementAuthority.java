package mechanist;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/** Owns local fallback profile metadata and migration packages when no store/wrapper profile is available. */
final class FallbackProfileManagementAuthority {
    static final String VERSION = "0.9.10hn";
    private static final Path PROFILE_DIR = Paths.get("profiles");
    private static final Path SETTINGS_DIR = Paths.get("settings");
    private static final Path ACTIVE_PROFILE_PATH = PROFILE_DIR.resolve("active_profile.properties");
    private static final long MAX_IMPORT_BYTES = 16L * 1024L * 1024L;

    static final class LocalProfile {
        String profileId;
        String profileName;
        String hardwareSignature;
        String wrapperProvider;
        boolean wrapperDetected;
        int portraitIndex;
        int maxRamMb;
        int initialRamMb;
        String runtimeTarget;
        String cvdMode;
        boolean diagnosticsOverlay;
        int internalWidth;
        int internalHeight;
        String lastSyncedIso;
        boolean legalAdultCertified;

        static LocalProfile fromRuntime(UserProfileAuthority.Profile detected, GameOptions options, JvmRuntimeProfileAuthority.RuntimeConfig jvm, RenderScalingCrtAuthority renderScaling) {
            LocalProfile p = loadOrCreate(detected);
            p.wrapperProvider = detected == null ? "Internal" : detected.provider;
            p.wrapperDetected = detected != null && detected.wrapperDetected;
            p.maxRamMb = jvm == null ? p.maxRamMb : jvm.maxRamMb;
            p.initialRamMb = jvm == null ? p.initialRamMb : jvm.initialRamMb;
            p.runtimeTarget = jvm == null ? p.runtimeTarget : jvm.runtimeTarget.name();
            p.cvdMode = AccessibilityCompatibilityAuthority.cvdLabel(options == null ? 0 : options.cvdModeIndex);
            p.diagnosticsOverlay = options != null && options.diagnosticsOverlay;
            p.internalWidth = renderScaling == null ? p.internalWidth : renderScaling.internalWidth();
            p.internalHeight = renderScaling == null ? p.internalHeight : renderScaling.internalHeight();
            return p;
        }

        Properties toProperties() {
            Properties pr = new Properties();
            pr.setProperty("profile.schema", "fallback-local-profile-v1");
            pr.setProperty("profile.version", VERSION);
            pr.setProperty("profile.id", clean(profileId, generateId()));
            pr.setProperty("profile.name", clean(profileName, "Local Operator"));
            pr.setProperty("profile.hardwareSignature", clean(hardwareSignature, hardwareSignature()));
            pr.setProperty("profile.wrapperProvider", clean(wrapperProvider, "Internal"));
            pr.setProperty("profile.wrapperDetected", String.valueOf(wrapperDetected));
            pr.setProperty("profile.portraitIndex", String.valueOf(Math.max(0, portraitIndex)));
            pr.setProperty("profile.maxRamMb", String.valueOf(Math.max(256, maxRamMb)));
            pr.setProperty("profile.initialRamMb", String.valueOf(Math.max(256, initialRamMb)));
            pr.setProperty("profile.runtimeTarget", clean(runtimeTarget, "CLIENT_GRAPHICS"));
            pr.setProperty("profile.cvdMode", clean(cvdMode, "Normal"));
            pr.setProperty("profile.diagnosticsOverlay", String.valueOf(diagnosticsOverlay));
            pr.setProperty("profile.internalWidth", String.valueOf(Math.max(160, internalWidth)));
            pr.setProperty("profile.internalHeight", String.valueOf(Math.max(90, internalHeight)));
            pr.setProperty("profile.lastSyncedIso", clean(lastSyncedIso, Instant.now().toString()));
            pr.setProperty("profile.isLegalAdult", String.valueOf(legalAdultCertified));
            return pr;
        }

        static LocalProfile fromProperties(Properties pr) {
            LocalProfile p = defaults(null);
            if (pr == null) return p;
            p.profileId = pr.getProperty("profile.id", p.profileId);
            p.profileName = pr.getProperty("profile.name", p.profileName);
            p.hardwareSignature = pr.getProperty("profile.hardwareSignature", p.hardwareSignature);
            p.wrapperProvider = pr.getProperty("profile.wrapperProvider", p.wrapperProvider);
            p.wrapperDetected = Boolean.parseBoolean(pr.getProperty("profile.wrapperDetected", String.valueOf(p.wrapperDetected)));
            p.portraitIndex = parseInt(pr.getProperty("profile.portraitIndex"), p.portraitIndex, 0, 9999);
            p.maxRamMb = parseInt(pr.getProperty("profile.maxRamMb"), p.maxRamMb, 256, 262144);
            p.initialRamMb = parseInt(pr.getProperty("profile.initialRamMb"), p.initialRamMb, 256, 262144);
            p.runtimeTarget = pr.getProperty("profile.runtimeTarget", p.runtimeTarget);
            p.cvdMode = pr.getProperty("profile.cvdMode", p.cvdMode);
            p.diagnosticsOverlay = Boolean.parseBoolean(pr.getProperty("profile.diagnosticsOverlay", String.valueOf(p.diagnosticsOverlay)));
            p.internalWidth = parseInt(pr.getProperty("profile.internalWidth"), p.internalWidth, 160, 16384);
            p.internalHeight = parseInt(pr.getProperty("profile.internalHeight"), p.internalHeight, 90, 16384);
            p.lastSyncedIso = pr.getProperty("profile.lastSyncedIso", p.lastSyncedIso);
            p.legalAdultCertified = Boolean.parseBoolean(pr.getProperty("profile.isLegalAdult", String.valueOf(p.legalAdultCertified)));
            return p;
        }
    }

    static String auditSummary(UserProfileAuthority.Profile detected) {
        LocalProfile p = loadOrCreate(detected);
        return "authority=" + VERSION + " wrapperDetected=" + p.wrapperDetected + " provider=" + p.wrapperProvider + " localProfile=" + p.profileId + " legalAdultCertified=" + p.legalAdultCertified + " path=" + ACTIVE_PROFILE_PATH;
    }

    static void openProfileManager(Component parent, UserProfileAuthority.Profile detected, GameOptions options, JvmRuntimeProfileAuthority.RuntimeConfig jvm, RenderScalingCrtAuthority renderScaling, Runnable repaintCallback) {
        LocalProfile profile = LocalProfile.fromRuntime(detected, options, jvm, renderScaling);
        Window owner = parent == null ? null : SwingUtilities.getWindowAncestor(parent);
        ProfileDialog dialog = new ProfileDialog(owner, profile, detected, options, jvm, renderScaling, repaintCallback);
        dialog.setVisible(true);
    }

    static LocalProfile loadOrCreate(UserProfileAuthority.Profile detected) {
        try {
            Files.createDirectories(PROFILE_DIR);
            if (Files.exists(ACTIVE_PROFILE_PATH)) {
                Properties pr = new Properties();
                try (InputStream in = Files.newInputStream(ACTIVE_PROFILE_PATH)) { pr.load(in); }
                return LocalProfile.fromProperties(pr);
            }
            LocalProfile p = defaults(detected);
            save(p);
            return p;
        } catch (Throwable t) {
            DebugLog.warn("FALLBACK_PROFILE", "Could not load fallback profile; using memory defaults: " + t.getMessage());
            return defaults(detected);
        }
    }

    static void save(LocalProfile profile) throws IOException {
        Files.createDirectories(PROFILE_DIR);
        profile.lastSyncedIso = Instant.now().toString();
        Path tmp = ACTIVE_PROFILE_PATH.resolveSibling(ACTIVE_PROFILE_PATH.getFileName() + ".tmp");
        try (OutputStream out = Files.newOutputStream(tmp)) { profile.toProperties().store(out, "The Mechanist fallback local profile"); }
        try {
            Files.move(tmp, ACTIVE_PROFILE_PATH, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException ex) {
            Files.move(tmp, ACTIVE_PROFILE_PATH, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    static void exportMigrationZip(LocalProfile profile, File destination) throws IOException {
        if (destination == null) throw new IOException("No destination selected.");
        validateLegalAdultCertification(profile);
        save(profile);
        Path dest = destination.toPath();
        if (!dest.toString().toLowerCase(Locale.ROOT).endsWith(".zip")) dest = Paths.get(dest.toString() + ".zip");
        Path tmp = dest.resolveSibling(dest.getFileName() + ".tmp");
        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(tmp))) {
            writeZipText(zos, "manifest.properties", migrationManifest(profile));
            addIfExists(zos, ACTIVE_PROFILE_PATH, "profiles/active_profile.properties");
            addIfExists(zos, GameOptions.settingsPath(), "settings/options.properties");
            addIfExists(zos, Paths.get("settings", "jvm_runtime.properties"), "settings/jvm_runtime.properties");
            addIfExists(zos, Paths.get("settings", "profile.seed"), "settings/profile.seed");
        }
        try { Files.move(tmp, dest, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
        catch (AtomicMoveNotSupportedException ex) { Files.move(tmp, dest, StandardCopyOption.REPLACE_EXISTING); }
    }

    static LocalProfile importMigrationZip(File source) throws IOException {
        if (source == null || !source.isFile()) throw new IOException("No readable profile archive selected.");
        if (source.length() > MAX_IMPORT_BYTES) throw new IOException("Profile archive is too large for a configuration migration package.");
        Path staging = Files.createTempDirectory("mechanist-profile-import-");
        boolean sawManifest = false;
        boolean sawProfile = false;
        try (ZipInputStream zis = new ZipInputStream(new BufferedInputStream(new FileInputStream(source)))) {
            ZipEntry e;
            byte[] buffer = new byte[8192];
            long total = 0L;
            while ((e = zis.getNextEntry()) != null) {
                if (e.isDirectory()) continue;
                String name = safeEntryName(e.getName());
                if (name == null) throw new IOException("Profile archive contains an unsafe path: " + e.getName());
                if (name.equals("manifest.properties")) sawManifest = true;
                if (name.equals("profiles/active_profile.properties")) sawProfile = true;
                Path out = staging.resolve(name).normalize();
                if (!out.startsWith(staging)) throw new IOException("Profile archive path escaped staging directory.");
                Files.createDirectories(out.getParent());
                try (OutputStream os = Files.newOutputStream(out)) {
                    int read;
                    while ((read = zis.read(buffer)) >= 0) {
                        total += read;
                        if (total > MAX_IMPORT_BYTES) throw new IOException("Profile archive expanded beyond safe configuration size.");
                        os.write(buffer, 0, read);
                    }
                }
            }
        }
        if (!sawManifest || !sawProfile) throw new IOException("Profile archive did not contain a valid manifest and active profile.");
        Properties imported = new Properties();
        try (InputStream in = Files.newInputStream(staging.resolve("profiles/active_profile.properties"))) { imported.load(in); }
        LocalProfile p = LocalProfile.fromProperties(imported);
        Files.createDirectories(PROFILE_DIR);
        Files.createDirectories(SETTINGS_DIR);
        copyIfExists(staging.resolve("profiles/active_profile.properties"), ACTIVE_PROFILE_PATH);
        copyIfExists(staging.resolve("settings/options.properties"), GameOptions.settingsPath());
        copyIfExists(staging.resolve("settings/jvm_runtime.properties"), Paths.get("settings", "jvm_runtime.properties"));
        copyIfExists(staging.resolve("settings/profile.seed"), Paths.get("settings", "profile.seed"));
        return p;
    }

    private static final class ProfileDialog extends JDialog {
        private LocalProfile profile;
        private final UserProfileAuthority.Profile detected;
        private final GameOptions options;
        private final JvmRuntimeProfileAuthority.RuntimeConfig jvm;
        private final RenderScalingCrtAuthority renderScaling;
        private final Runnable repaintCallback;
        private final JTextField nameField = new JTextField(18);
        private final JCheckBox legalAdultCheck = new JCheckBox("I certify that I am a legal adult in my region.");
        private final JLabel profileId = new JLabel();
        private final JLabel portrait = new JLabel();
        private final JTextArea systemInfo = new JTextArea();
        private final JTextArea migrationInfo = new JTextArea();
        private static final Color[] portraitColors = {new Color(70,80,95), new Color(85,65,105), new Color(95,80,45), new Color(55,95,85), new Color(100,55,55), new Color(70,95,55), new Color(75,65,120)};

        ProfileDialog(Window owner, LocalProfile profile, UserProfileAuthority.Profile detected, GameOptions options, JvmRuntimeProfileAuthority.RuntimeConfig jvm, RenderScalingCrtAuthority renderScaling, Runnable repaintCallback) {
            super(owner, "Profile Management", ModalityType.APPLICATION_MODAL);
            this.profile = profile;
            this.detected = detected;
            this.options = options;
            this.jvm = jvm;
            this.renderScaling = renderScaling;
            this.repaintCallback = repaintCallback;
            setDefaultCloseOperation(DISPOSE_ON_CLOSE);
            setMinimumSize(new Dimension(760, 520));
            setLayout(new BorderLayout(10, 10));
            ((JPanel)getContentPane()).setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
            add(buildPortraitPanel(), BorderLayout.WEST);
            add(buildInfoPanel(), BorderLayout.CENTER);
            add(buildControlPanel(), BorderLayout.SOUTH);
            refresh();
            pack();
            setLocationRelativeTo(owner);
        }

        private JPanel buildPortraitPanel() {
            JPanel panel = new JPanel(new BorderLayout(6, 6));
            panel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Local Profile Asset", TitledBorder.CENTER, TitledBorder.TOP));
            panel.setPreferredSize(new Dimension(230, 420));
            portrait.setOpaque(true);
            portrait.setForeground(Color.WHITE);
            portrait.setHorizontalAlignment(SwingConstants.CENTER);
            portrait.setFont(new Font("SansSerif", Font.BOLD, 14));
            portrait.setFocusable(true);
            portrait.addMouseWheelListener(e -> cyclePortrait(e.getWheelRotation() > 0 ? 1 : -1));
            portrait.setToolTipText("Mouse wheel or buttons cycle the fallback profile portrait index.");
            panel.add(portrait, BorderLayout.CENTER);
            JPanel row = new JPanel(new GridLayout(1, 2, 6, 0));
            JButton prev = new JButton("◀ PREV");
            JButton next = new JButton("NEXT ▶");
            prev.addActionListener(e -> cyclePortrait(-1));
            next.addActionListener(e -> cyclePortrait(1));
            row.add(prev); row.add(next);
            panel.add(row, BorderLayout.SOUTH);
            return panel;
        }

        private JPanel buildInfoPanel() {
            JPanel outer = new JPanel(new GridLayout(2, 1, 8, 8));
            JPanel profilePanel = new JPanel(new BorderLayout(6, 6));
            profilePanel.setBorder(BorderFactory.createTitledBorder("Profile Identity and Runtime Signature"));
            JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
            header.add(new JLabel("Name:"));
            header.add(nameField);
            header.add(profileId);
            JPanel top = new JPanel(new BorderLayout(4, 4));
            top.add(header, BorderLayout.NORTH);
            legalAdultCheck.setToolTipText("Required for creating or syncing a local fallback profile.");
            top.add(legalAdultCheck, BorderLayout.SOUTH);
            profilePanel.add(top, BorderLayout.NORTH);
            systemInfo.setEditable(false);
            systemInfo.setLineWrap(true);
            systemInfo.setWrapStyleWord(true);
            systemInfo.setFont(new Font("Monospaced", Font.PLAIN, 12));
            profilePanel.add(new JScrollPane(systemInfo), BorderLayout.CENTER);
            JPanel migrationPanel = new JPanel(new BorderLayout());
            migrationPanel.setBorder(BorderFactory.createTitledBorder("Fallback Migration Package"));
            migrationInfo.setEditable(false);
            migrationInfo.setLineWrap(true);
            migrationInfo.setWrapStyleWord(true);
            migrationInfo.setFont(new Font("Monospaced", Font.PLAIN, 12));
            migrationPanel.add(new JScrollPane(migrationInfo), BorderLayout.CENTER);
            outer.add(profilePanel);
            outer.add(migrationPanel);
            return outer;
        }

        private JPanel buildControlPanel() {
            JPanel panel = new JPanel(new BorderLayout());
            JLabel notice = new JLabel("Fallback profile management is used when no wrapper environment owns profiles; import/export moves settings, JVM profile, and local profile metadata.");
            panel.add(notice, BorderLayout.CENTER);
            JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
            JButton importBtn = new JButton("Import Profile ZIP");
            JButton exportBtn = new JButton("Export Profile ZIP");
            JButton syncBtn = new JButton("Apply Changes & Sync");
            JButton closeBtn = new JButton("Close");
            importBtn.addActionListener(e -> importZip());
            exportBtn.addActionListener(e -> exportZip());
            syncBtn.addActionListener(e -> sync());
            closeBtn.addActionListener(e -> dispose());
            actions.add(importBtn); actions.add(exportBtn); actions.add(syncBtn); actions.add(closeBtn);
            panel.add(actions, BorderLayout.SOUTH);
            return panel;
        }

        private void cyclePortrait(int delta) { profile.portraitIndex = Math.floorMod(profile.portraitIndex + delta, Math.max(1, portraitChoiceCount())); refresh(); }

        private void refresh() {
            int count = Math.max(1, portraitChoiceCount());
            int idx = Math.floorMod(profile.portraitIndex, count);
            profile.portraitIndex = idx;
            nameField.setText(profile.profileName);
            legalAdultCheck.setSelected(profile.legalAdultCertified);
            profileId.setText("ID: " + profile.profileId);
            portrait.setText("");
            portrait.setIcon(new ImageIcon(fallbackPortraitIcon(idx, 218, 344, portraitLabel(idx), portraitColors[Math.floorMod(idx, portraitColors.length)])));
            portrait.setBackground(new Color(12, 14, 13));
            systemInfo.setText(systemText(idx));
            migrationInfo.setText(migrationText());
        }

        private BufferedImage fallbackPortraitIcon(int idx, int w, int h, String label, Color fallbackColor) {
            BufferedImage canvas = new BufferedImage(Math.max(64, w), Math.max(96, h), BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = canvas.createGraphics();
            try {
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                g.setColor(fallbackColor == null ? new Color(45, 60, 55) : fallbackColor.darker());
                g.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
                BufferedImage source = readProfilePortrait(idx);
                if (source != null) {
                    int pad = 10;
                    int titleBand = 44;
                    double scale = Math.min((canvas.getWidth() - pad * 2) / (double)Math.max(1, source.getWidth()),
                                            (canvas.getHeight() - titleBand - pad * 2) / (double)Math.max(1, source.getHeight()));
                    int dw = Math.max(1, (int)Math.round(source.getWidth() * scale));
                    int dh = Math.max(1, (int)Math.round(source.getHeight() * scale));
                    int dx = (canvas.getWidth() - dw) / 2;
                    int dy = pad + Math.max(0, (canvas.getHeight() - titleBand - pad * 2 - dh) / 2);
                    g.drawImage(source, dx, dy, dw, dh, null);
                } else {
                    // Graphic fallback remains a portrait silhouette, not a text-only swatch.
                    g.setColor(new Color(30, 28, 25, 220));
                    g.fillOval(canvas.getWidth()/2 - 42, 60, 84, 84);
                    g.setColor(new Color(185, 165, 105));
                    g.fillOval(canvas.getWidth()/2 - 28, 78, 56, 56);
                    g.setColor(new Color(50, 36, 30));
                    g.fillRect(canvas.getWidth()/2 - 54, 145, 108, 128);
                    g.setColor(new Color(120, 90, 48));
                    g.drawRect(canvas.getWidth()/2 - 54, 145, 108, 128);
                }
                g.setColor(new Color(0, 0, 0, 168));
                g.fillRoundRect(8, canvas.getHeight() - 42, canvas.getWidth() - 16, 34, 8, 8);
                g.setColor(Color.WHITE);
                g.setFont(new Font("SansSerif", Font.BOLD, 12));
                FontMetrics fm = g.getFontMetrics();
                String line1 = label == null ? "Profile Portrait" : label;
                if (fm.stringWidth(line1) > canvas.getWidth() - 24) line1 = line1.substring(0, Math.max(1, Math.min(line1.length(), 18))) + "...";
                g.drawString(line1, (canvas.getWidth() - fm.stringWidth(line1)) / 2, canvas.getHeight() - 22);
                String line2 = portraitSourceLine(idx);
                g.drawString(line2, (canvas.getWidth() - fm.stringWidth(line2)) / 2, canvas.getHeight() - 8);
            } finally {
                g.dispose();
            }
            return canvas;
        }

        private int portraitChoiceCount() {
            int standard = standardPortraitFiles().size();
            return Math.max(1, standard + (matchedNameLockedEntry() == null ? 0 : 1));
        }

        private String currentProfileNameForPortrait() {
            String typed = nameField == null ? null : nameField.getText();
            if (typed != null && !typed.isBlank()) return typed.trim();
            return profile == null || profile.profileName == null ? "" : profile.profileName.trim();
        }

        private NameLockedProfilePortraitAuthority.Entry matchedNameLockedEntry() {
            return NameLockedProfilePortraitAuthority.match(currentProfileNameForPortrait()).orElse(null);
        }

        private boolean hasNameLockedChoice() { return matchedNameLockedEntry() != null; }

        private String portraitLabel(int idx) {
            NameLockedProfilePortraitAuthority.Entry e = matchedNameLockedEntry();
            if (e != null && idx == 0) return e.displayName;
            int standardIdx = idx - (e == null ? 0 : 1);
            Path p = standardPortraitPath(standardIdx);
            if (p != null) {
                Path parent = p.getParent() == null ? null : p.getParent().getFileName();
                String family = parent == null ? "Standard" : parent.toString().replace('_', ' ');
                return "Standard " + family;
            }
            return "Standard Profile";
        }

        private String portraitSourceLine(int idx) {
            return hasNameLockedChoice() && idx == 0 ? "Name-locked" : "Standard #" + Math.max(0, idx - (hasNameLockedChoice() ? 1 : 0));
        }

        private BufferedImage readProfilePortrait(int idx) {
            try {
                NameLockedProfilePortraitAuthority.Entry e = matchedNameLockedEntry();
                if (e != null && idx == 0) {
                    BufferedImage locked = readNameLockedPortrait(e);
                    if (locked != null) return locked;
                }
                int standardIdx = idx - (e == null ? 0 : 1);
                Path p = standardPortraitPath(standardIdx);
                return p == null ? null : ImageIO.read(p.toFile());
            } catch (Throwable ignored) { return null; }
        }

        private BufferedImage readNameLockedPortrait(NameLockedProfilePortraitAuthority.Entry e) {
            try {
                if (e == null || e.assetPath == null || e.assetPath.isBlank()) return null;
                Path p = Paths.get(e.assetPath);
                if (!Files.isRegularFile(p)) return null;
                return ImageIO.read(p.toFile());
            } catch (Throwable ignored) { return null; }
        }

        private Path standardPortraitPath(int idx) {
            ArrayList<Path> files = standardPortraitFiles();
            if (files.isEmpty()) return null;
            return files.get(Math.floorMod(idx, files.size()));
        }

        private ArrayList<Path> standardPortraitFiles() {
            ArrayList<Path> files = new ArrayList<>();
            Path root = Paths.get("assets", "a", "r", "tiles", "quality", "low_32", "cells", "Protraits");
            String[] preferredHumanBuckets = {
                    "human_profiles", "humans", "human", "baseline_human", "base_human",
                    "player_human", "player_humans", "standard_human", "standard_profiles",
                    "profile_human", "profile_humans", "profiles"
            };
            for (String bucket : preferredHumanBuckets) collectStandardPortraits(root.resolve(bucket), files);
            if (files.isEmpty()) collectStandardPortraits(root.resolve("administratum"), files);
            collectStandardPortraits(Paths.get("assets", "imported_tech_priests", "graphics", "gui", "portraits", "cells_0560"), files);
            files.removeIf(path -> {
                String normalized = path.toString().replace('\\','/').toLowerCase(Locale.ROOT);
                return normalized.contains("/name_locked/") || normalized.contains("/nobles/") || normalized.contains("/mutants/")
                        || normalized.contains("/cultists/") || normalized.contains("/genestealer_cult/") || normalized.contains("/heretics/")
                        || normalized.contains("/enforcer_arebites/") || normalized.contains("/pdf_military/") || normalized.contains("/pets/")
                        || normalized.contains("/farm_beasts/") || normalized.contains("/exotic_pets_swamp_creatures/")
                        || normalized.contains("/rogue_automata_servitors/");
            });
            files.sort(Comparator.comparing(path -> path.toString().toLowerCase(Locale.ROOT)));
            return files;
        }

        private void collectStandardPortraits(Path dir, ArrayList<Path> out) {
            if (dir == null || out == null || !Files.isDirectory(dir)) return;
            try (java.util.stream.Stream<Path> stream = Files.walk(dir, 8)) {
                stream.filter(p -> Files.isRegularFile(p) && p.getFileName() != null && p.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".png"))
                        .forEach(out::add);
            } catch (Throwable ignored) {}
        }

        private String systemText(int portraitIndex) {
            return "Wrapper provider: " + (detected == null ? profile.wrapperProvider : detected.provider) + "\n"
                    + "Wrapper detected: " + (detected != null && detected.wrapperDetected) + "\n"
                    + "Fallback profile ID: " + profile.profileId + "\n"
                    + "Legal adult certification: " + (profile.legalAdultCertified ? "Certified" : "Not certified") + "\n"
                    + "Hardware signature: " + profile.hardwareSignature + "\n"
                    + "OS: " + System.getProperty("os.name") + " / " + System.getProperty("os.arch") + "\n"
                    + "Java: " + System.getProperty("java.version") + "\n"
                    + "Runtime target: " + profile.runtimeTarget + "\n"
                    + "Memory: -Xms" + profile.initialRamMb + "M / -Xmx" + profile.maxRamMb + "M\n"
                    + "CVD mode: " + profile.cvdMode + " | F3 diagnostics: " + profile.diagnosticsOverlay + "\n"
                    + "Backbuffer/internal bounds: " + profile.internalWidth + "x" + profile.internalHeight + "\n"
                    + "Selected portrait index: " + portraitIndex + " / choices: " + portraitChoiceCount() + "\n"
                    + "Portrait authority: player-human profile pool by default; name-locked portraits unlock only when the profile name matches the registered list.";
        }

        private String migrationText() {
            return "Migration archive contents:\n"
                    + " - profiles/active_profile.properties\n"
                    + " - settings/options.properties\n"
                    + " - settings/jvm_runtime.properties\n"
                    + " - settings/profile.seed\n\n"
                    + "Import uses a temporary staging directory, path-safety checks, a size cap, and atomic replacement where the OS supports it. "
                    + "This menu is intentionally local-only; world saves and character saves are not moved by profile migration.";
        }

        private void sync() {
            try {
                profile.profileName = nameField.getText().trim().isEmpty() ? "Local Operator" : nameField.getText().trim();
                profile.legalAdultCertified = legalAdultCheck.isSelected();
                validateLegalAdultCertification(profile);
                profile.hardwareSignature = hardwareSignature();
                profile.wrapperProvider = detected == null ? "Internal" : detected.provider;
                profile.wrapperDetected = detected != null && detected.wrapperDetected;
                if (jvm != null) { profile.maxRamMb = jvm.maxRamMb; profile.initialRamMb = jvm.initialRamMb; profile.runtimeTarget = jvm.runtimeTarget.name(); }
                if (options != null) { profile.cvdMode = AccessibilityCompatibilityAuthority.cvdLabel(options.cvdModeIndex); profile.diagnosticsOverlay = options.diagnosticsOverlay; }
                if (renderScaling != null) { profile.internalWidth = renderScaling.internalWidth(); profile.internalHeight = renderScaling.internalHeight(); }
                save(profile);
                DebugLog.audit("FALLBACK_PROFILE", "synced " + auditSummary(detected));
                if (repaintCallback != null) repaintCallback.run();
                JOptionPane.showMessageDialog(this, "Profile synced to " + ACTIVE_PROFILE_PATH + ".", "Profile Sync", JOptionPane.INFORMATION_MESSAGE);
                refresh();
            } catch (Throwable t) {
                JOptionPane.showMessageDialog(this, "Profile sync failed:\n" + t.getMessage(), "Profile Error", JOptionPane.ERROR_MESSAGE);
            }
        }

        private void exportZip() {
            syncQuietly();
            JFileChooser fc = new JFileChooser();
            fc.setDialogTitle("Export Profile Migration ZIP");
            fc.setSelectedFile(new File("mechanist_profile_" + profile.profileId + ".zip"));
            if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
            try {
                exportMigrationZip(profile, fc.getSelectedFile());
                JOptionPane.showMessageDialog(this, "Profile migration archive exported.", "Profile Export", JOptionPane.INFORMATION_MESSAGE);
            } catch (Throwable t) {
                JOptionPane.showMessageDialog(this, "Profile export failed:\n" + t.getMessage(), "Profile Error", JOptionPane.ERROR_MESSAGE);
            }
        }

        private void importZip() {
            JFileChooser fc = new JFileChooser();
            fc.setDialogTitle("Import Profile Migration ZIP");
            if (fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
            try {
                profile = importMigrationZip(fc.getSelectedFile());
                JOptionPane.showMessageDialog(this, "Profile archive imported. Restart is recommended if JVM settings changed.", "Profile Import", JOptionPane.INFORMATION_MESSAGE);
                refresh();
                if (repaintCallback != null) repaintCallback.run();
            } catch (Throwable t) {
                JOptionPane.showMessageDialog(this, "Profile import failed:\n" + t.getMessage(), "Profile Error", JOptionPane.ERROR_MESSAGE);
            }
        }

        private void syncQuietly() {
            profile.profileName = nameField.getText().trim().isEmpty() ? profile.profileName : nameField.getText().trim();
            profile.legalAdultCertified = legalAdultCheck.isSelected();
            try { validateLegalAdultCertification(profile); save(profile); } catch (Throwable t) { DebugLog.warn("FALLBACK_PROFILE", "Pre-export sync failed: " + t.getMessage()); }
        }
    }

    private static LocalProfile defaults(UserProfileAuthority.Profile detected) {
        LocalProfile p = new LocalProfile();
        p.profileId = generateId();
        p.profileName = detected != null && detected.displayName != null && !detected.displayName.isBlank() ? detected.displayName : "Local Operator";
        p.hardwareSignature = hardwareSignature();
        p.wrapperProvider = detected == null ? "Internal" : detected.provider;
        p.wrapperDetected = detected != null && detected.wrapperDetected;
        p.portraitIndex = 0;
        p.maxRamMb = 4096;
        p.initialRamMb = 1024;
        p.runtimeTarget = "CLIENT_GRAPHICS";
        p.cvdMode = "Normal";
        p.diagnosticsOverlay = false;
        p.internalWidth = 480;
        p.internalHeight = 270;
        p.lastSyncedIso = Instant.now().toString();
        p.legalAdultCertified = detected != null && detected.wrapperDetected;
        return p;
    }

    static void validateLegalAdultCertification(LocalProfile profile) {
        if (profile == null || !profile.legalAdultCertified) {
            throw new ProfileRegistrationException("Profile creation requires certification that the user is a legal adult in their region.");
        }
    }

    static final class ProfileRegistrationException extends RuntimeException {
        ProfileRegistrationException(String message) { super(message); }
    }

    private static String migrationManifest(LocalProfile profile) {
        return "manifest.schema=mechanist-profile-migration-v1\n"
                + "manifest.version=" + VERSION + "\n"
                + "profile.id=" + clean(profile.profileId, generateId()) + "\n"
                + "profile.hardwareSignature=" + clean(profile.hardwareSignature, hardwareSignature()) + "\n"
                + "createdIso=" + Instant.now() + "\n";
    }

    private static void addIfExists(ZipOutputStream zos, Path path, String entryName) throws IOException {
        if (path == null || !Files.exists(path) || Files.isDirectory(path)) return;
        ZipEntry e = new ZipEntry(entryName);
        zos.putNextEntry(e);
        Files.copy(path, zos);
        zos.closeEntry();
    }

    private static void writeZipText(ZipOutputStream zos, String entryName, String text) throws IOException {
        ZipEntry e = new ZipEntry(entryName);
        zos.putNextEntry(e);
        zos.write(text.getBytes(StandardCharsets.UTF_8));
        zos.closeEntry();
    }

    private static void copyIfExists(Path from, Path to) throws IOException {
        if (from == null || to == null || !Files.exists(from)) return;
        Files.createDirectories(to.getParent());
        Path tmp = to.resolveSibling(to.getFileName() + ".tmp");
        Files.copy(from, tmp, StandardCopyOption.REPLACE_EXISTING);
        try { Files.move(tmp, to, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
        catch (AtomicMoveNotSupportedException ex) { Files.move(tmp, to, StandardCopyOption.REPLACE_EXISTING); }
    }

    private static String safeEntryName(String name) {
        if (name == null || name.isBlank()) return null;
        String cleaned = name.replace('\\', '/');
        if (cleaned.startsWith("/") || cleaned.contains("../") || cleaned.equals("..") || cleaned.contains(":/")) return null;
        if (!(cleaned.equals("manifest.properties") || cleaned.equals("profiles/active_profile.properties") || cleaned.equals("settings/options.properties") || cleaned.equals("settings/jvm_runtime.properties") || cleaned.equals("settings/profile.seed"))) return null;
        return cleaned;
    }

    private static int parseInt(String s, int fallback, int min, int max) {
        try { return Math.max(min, Math.min(max, Integer.parseInt(String.valueOf(s).trim()))); }
        catch (Throwable t) { return fallback; }
    }

    private static String generateId() {
        return "MECH-PROFILE-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase(Locale.ROOT);
    }

    private static String hardwareSignature() {
        try {
            String raw = System.getProperty("os.name", "os") + "|" + System.getProperty("os.arch", "arch") + "|" + System.getProperty("user.name", "user") + "|" + Runtime.getRuntime().availableProcessors();
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] dig = md.digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder("HW-");
            for (int i = 0; i < 10 && i < dig.length; i++) sb.append(String.format(Locale.ROOT, "%02X", dig[i]));
            return sb.toString();
        } catch (Throwable t) {
            return "HW-" + UUID.nameUUIDFromBytes(String.valueOf(System.nanoTime()).getBytes(StandardCharsets.UTF_8)).toString().replace("-", "").substring(0, 20).toUpperCase(Locale.ROOT);
        }
    }

    private static String clean(String s, String fallback) { return s == null || s.trim().isEmpty() ? fallback : s.trim().replace('\n', ' '); }

    private FallbackProfileManagementAuthority() {}
}
