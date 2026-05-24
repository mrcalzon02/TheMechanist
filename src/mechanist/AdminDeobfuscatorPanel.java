package mechanist;

import mechanist.ProGuardMapParser.MappingMetrics;
import mechanist.StackTraceDeobfuscator.DeobfuscationResult;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

/** Swing admin panel for de-obfuscating ProGuard crash reports without freezing the EDT. */
public final class AdminDeobfuscatorPanel extends JPanel implements AutoCloseable {
    private final CrashDeobfuscatorEngine engine = new CrashDeobfuscatorEngine();
    private final DefaultComboBoxModel<String> versions = new DefaultComboBoxModel<>(new String[] { "manual-local", "client-release", "server-release" });
    private final JComboBox<String> versionCombo = new JComboBox<>(versions);
    private final JTextField mappingPath = new JTextField(34);
    private final JTextField keyPath = new JTextField(22);
    private final JCheckBox encryptedMapping = new JCheckBox("Encrypted mapping", true);
    private final JTextArea rawTrace = new JTextArea();
    private final JTextArea cleanTrace = new JTextArea();
    private final JProgressBar progress = new JProgressBar(0, 100);
    private final JLabel status = new JLabel("Load a mapping file, paste a crash report, then de-obfuscate.");

    public AdminDeobfuscatorPanel() {
        super(new BorderLayout(8, 8));
        setBorder(new EmptyBorder(8, 8, 8, 8));
        add(buildTopBar(), BorderLayout.NORTH);
        add(buildPanes(), BorderLayout.CENTER);
        add(buildBottomBar(), BorderLayout.SOUTH);
        configureTextAreas();
    }

    public static void openWindow() {
        Runnable task = () -> {
            JFrame frame = new JFrame("The Mechanist — Crash Log De-Obfuscator");
            AppIconAuthority.applyTo(frame);
            frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            frame.setContentPane(new AdminDeobfuscatorPanel());
            frame.setMinimumSize(new Dimension(1080, 680));
            frame.setPreferredSize(new Dimension(1280, 780));
            frame.pack();
            frame.setLocationByPlatform(true);
            frame.setVisible(true);
        };
        if (SwingUtilities.isEventDispatchThread()) task.run(); else SwingUtilities.invokeLater(task);
    }

    private JPanel buildTopBar() {
        JPanel outer = new JPanel(new BorderLayout(4, 4));
        JPanel row1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        row1.add(new JLabel("Build version:"));
        versionCombo.setEditable(true);
        row1.add(versionCombo);
        row1.add(new JLabel("Mapping:"));
        mappingPath.setEditable(false);
        row1.add(mappingPath);
        JButton chooseMapping = new JButton("Choose mapping.txt / encrypted map");
        chooseMapping.addActionListener(e -> chooseMappingFile());
        row1.add(chooseMapping);
        JButton load = new JButton("Load Mapping");
        load.addActionListener(e -> loadMapping());
        row1.add(load);

        JPanel row2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        row2.add(encryptedMapping);
        row2.add(new JLabel("Key file:"));
        keyPath.setEditable(false);
        row2.add(keyPath);
        JButton chooseKey = new JButton("Choose Key");
        chooseKey.addActionListener(e -> chooseKeyFile());
        row2.add(chooseKey);
        JButton clear = new JButton("Clear Output");
        clear.addActionListener(e -> cleanTrace.setText(""));
        row2.add(clear);

        outer.add(row1, BorderLayout.NORTH);
        outer.add(row2, BorderLayout.SOUTH);
        outer.setBorder(BorderFactory.createTitledBorder("Release mapping configuration"));
        return outer;
    }

    private JSplitPane buildPanes() {
        JScrollPane left = new JScrollPane(rawTrace);
        left.setBorder(BorderFactory.createTitledBorder("Raw obfuscated crash dump"));
        JScrollPane right = new JScrollPane(cleanTrace);
        right.setBorder(BorderFactory.createTitledBorder("Reconstructed developer trace"));
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, right);
        split.setResizeWeight(0.5d);
        return split;
    }

    private JPanel buildBottomBar() {
        JPanel bottom = new JPanel(new BorderLayout(8, 4));
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        JButton run = new JButton("De-Obfuscate Log");
        run.addActionListener(e -> deobfuscateLog());
        controls.add(run);
        controls.add(Box.createHorizontalStrut(10));
        progress.setStringPainted(true);
        progress.setString("Idle");
        progress.setPreferredSize(new Dimension(220, 22));
        controls.add(progress);
        bottom.add(controls, BorderLayout.WEST);
        bottom.add(status, BorderLayout.CENTER);
        return bottom;
    }

    private void configureTextAreas() {
        Font font = new Font(Font.MONOSPACED, Font.PLAIN, 12);
        rawTrace.setFont(font);
        rawTrace.setLineWrap(false);
        rawTrace.setTabSize(4);
        cleanTrace.setFont(font);
        cleanTrace.setLineWrap(false);
        cleanTrace.setTabSize(4);
        cleanTrace.setEditable(false);
    }

    private void chooseMappingFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select ProGuard mapping.txt or encrypted mapping payload");
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            mappingPath.setText(chooser.getSelectedFile().toPath().toAbsolutePath().normalize().toString());
        }
    }

    private void chooseKeyFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select mapping AES key file");
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            keyPath.setText(chooser.getSelectedFile().toPath().toAbsolutePath().normalize().toString());
        }
    }

    private String selectedVersion() {
        Object item = versionCombo.getEditor().getItem();
        String version = item == null ? "manual-local" : item.toString().trim();
        return version.isBlank() ? "manual-local" : version;
    }

    private void loadMapping() {
        if (mappingPath.getText().isBlank()) {
            JOptionPane.showMessageDialog(this, "Choose a mapping file first.", "Missing mapping", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (encryptedMapping.isSelected() && keyPath.getText().isBlank()) {
            JOptionPane.showMessageDialog(this, "Encrypted mapping mode requires the AES mapping key file.", "Missing key", JOptionPane.WARNING_MESSAGE);
            return;
        }
        progress.setIndeterminate(true);
        progress.setString("Loading map...");
        status.setText("Parsing mapping file in the background...");
        String version = selectedVersion();
        Path map = Path.of(mappingPath.getText());
        Optional<Path> key = encryptedMapping.isSelected() ? Optional.of(Path.of(keyPath.getText())) : Optional.empty();
        SwingWorker<MappingMetrics, Void> worker = new SwingWorker<>() {
            @Override protected MappingMetrics doInBackground() throws Exception {
                return engine.loadMappingAsync(version, map, key).get();
            }

            @Override protected void done() {
                progress.setIndeterminate(false);
                progress.setValue(100);
                progress.setString("Map loaded");
                try {
                    MappingMetrics metrics = get();
                    ensureVersionVisible(version);
                    status.setText("Mapping loaded: " + metrics.auditLine());
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    reportFailure("Mapping load interrupted", ex);
                } catch (ExecutionException ex) {
                    reportFailure("Mapping load failed", ex.getCause() == null ? ex : ex.getCause());
                }
            }
        };
        worker.execute();
    }

    private void deobfuscateLog() {
        String input = rawTrace.getText();
        if (input == null || input.isBlank()) {
            cleanTrace.setText("[MappingMissingAnomalie empty-crash-log]");
            return;
        }
        String version = selectedVersion();
        progress.setIndeterminate(true);
        progress.setString("De-obfuscating...");
        status.setText("Reconstructing stack trace in the background...");
        SwingWorker<DeobfuscationResult, Void> worker = new SwingWorker<>() {
            @Override protected DeobfuscationResult doInBackground() throws Exception {
                return engine.deobfuscateAsync(version, input).get();
            }

            @Override protected void done() {
                progress.setIndeterminate(false);
                progress.setValue(100);
                progress.setString("Done");
                try {
                    DeobfuscationResult result = get();
                    cleanTrace.setText(result.reconstructedTrace());
                    cleanTrace.setCaretPosition(0);
                    status.setText("De-obfuscation complete: " + result.auditLine());
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    reportFailure("De-obfuscation interrupted", ex);
                } catch (ExecutionException ex) {
                    Throwable cause = ex.getCause() == null ? ex : ex.getCause();
                    cleanTrace.setText("[MappingMissingAnomalie deobfuscation-failed] " + cause.getMessage());
                    reportFailure("De-obfuscation failed", cause);
                }
            }
        };
        worker.execute();
    }

    private void ensureVersionVisible(String version) {
        boolean present = false;
        for (int i = 0; i < versions.getSize(); i++) {
            if (version.equals(versions.getElementAt(i))) { present = true; break; }
        }
        if (!present) versions.addElement(version);
        versionCombo.setSelectedItem(version);
    }

    private void reportFailure(String title, Throwable ex) {
        progress.setIndeterminate(false);
        progress.setString("Failed");
        status.setText(title + ": " + ex.getMessage());
        JOptionPane.showMessageDialog(this, title + System.lineSeparator() + ex.getMessage(), "Crash De-Obfuscator", JOptionPane.ERROR_MESSAGE);
    }

    @Override public void close() { engine.close(); }
}
