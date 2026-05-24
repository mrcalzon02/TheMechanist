package mechanist;

import javax.swing.AbstractCellEditor;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultCellEditor;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/** Native Java 17 Swing workspace for simulation editing and mod deployment. */
public final class SimulationEditorSuite extends JFrame {
    static final String VERSION = "simulation-editor-suite-0.9.10iq";

    private final EditorEventBus eventBus = new EditorEventBus();
    private final EditorUndoRedoController undoRedo = new EditorUndoRedoController(eventBus);
    private final SimulationEditorRepository repository = new SimulationEditorRepository();
    private final ModDeploymentManager deploymentManager = new ModDeploymentManager(eventBus);
    private final JLabel status = new JLabel("Editor suite ready.");
    private final JProgressBar progress = new JProgressBar(0, 100);

    public SimulationEditorSuite() {
        super("The Mechanist — Simulation Editor Suite");
        AppIconAuthority.applyTo(this);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setMinimumSize(new Dimension(1180, 760));
        setPreferredSize(new Dimension(1320, 840));
        setLayout(new BorderLayout(8, 8));
        setJMenuBar(null);
        add(buildToolbar(), BorderLayout.NORTH);
        add(buildTabs(), BorderLayout.CENTER);
        add(buildStatusBar(), BorderLayout.SOUTH);
        installEventBindings();
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosed(WindowEvent e) { DebugLog.audit("SIMULATION_EDITOR_SUITE", "closed " + repository.auditLine()); }
        });
        pack();
        setLocationByPlatform(true);
        DebugLog.audit("SIMULATION_EDITOR_SUITE", "opened " + repository.auditLine());
    }

    public static void openWindow() {
        DebugLog.warn("SIMULATION_EDITOR_SUITE_LEGACY_WINDOW",
                "Detached editor windows are disabled for player-facing builds. Use the in-game Editor screen from Mods / Tools instead.");
    }

    static String auditSummary() { return "authority=" + VERSION + " legacy-external-window=disabled development-only-code=true tabs=7 mvc=event-bus undo-redo=true room-visual-layout=true palette-tabs=5 export=zip+steam-reflection-swingworker"; }

    private JComponent buildToolbar() {
        JToolBar bar = new JToolBar("Simulation editor controls");
        bar.setFloatable(false);
        JButton undo = new JButton("Undo");
        undo.addActionListener(e -> undoRedo.undo());
        JButton redo = new JButton("Redo");
        redo.addActionListener(e -> undoRedo.redo());
        JButton export = new JButton("Open Packaging Tab");
        export.addActionListener(e -> selectTabByTitle("Mod Packaging Editor"));
        JButton statusButton = new JButton("Repository Status");
        statusButton.addActionListener(e -> eventBus.publish(new EditorEvent.StatusChanged(repository.auditLine())));
        JButton deobfuscator = new JButton("Crash De-Obfuscator");
        deobfuscator.addActionListener(e -> AdminDeobfuscatorPanel.openWindow());
        bar.add(undo);
        bar.add(redo);
        bar.addSeparator();
        bar.add(export);
        bar.add(deobfuscator);
        bar.add(statusButton);
        return bar;
    }

    private JComponent buildTabs() {
        JTabbedPane tabs = new JTabbedPane(JTabbedPane.TOP, JTabbedPane.SCROLL_TAB_LAYOUT);
        tabs.setName("mechanistSimulationEditorTabs");
        tabs.addTab("Sector Editor", new GenericEditorPanel("Sector Editor", repository, undoRedo, eventBus, LinkCatalog.sectorLinks(repository)));
        tabs.addTab("Room Editor", new RoomEditorPanel(repository, undoRedo, eventBus));
        tabs.addTab("Faction Editor", new GenericEditorPanel("Faction Editor", repository, undoRedo, eventBus, LinkCatalog.factionLinks(repository)));
        tabs.addTab("Item Editor", new GenericEditorPanel("Item Editor", repository, undoRedo, eventBus, LinkCatalog.itemLinks(repository)));
        tabs.addTab("Knowledge Editor", new GenericEditorPanel("Knowledge Editor", repository, undoRedo, eventBus, LinkCatalog.knowledgeLinks(repository)));
        tabs.addTab("Infopedia Editor", new InfopediaEditorPanel(repository, undoRedo, eventBus));
        tabs.addTab("Mod Packaging Editor", new ModPackagingPanel(repository, undoRedo, eventBus, deploymentManager));
        return tabs;
    }

    private JComponent buildStatusBar() {
        JPanel panel = new JPanel(new BorderLayout(8, 0));
        panel.setBorder(new EmptyBorder(4, 8, 6, 8));
        progress.setStringPainted(true);
        progress.setValue(0);
        progress.setString("Idle");
        panel.add(status, BorderLayout.CENTER);
        panel.add(progress, BorderLayout.EAST);
        return panel;
    }

    private void installEventBindings() {
        eventBus.subscribe(EditorEvent.StatusChanged.class, e -> status.setText(e.message()));
        eventBus.subscribe(EditorEvent.ModelChanged.class, e -> status.setText("Changed " + e.editorName() + " / " + e.entityId() + " / " + e.propertyName() + " = " + e.newValue()));
        eventBus.subscribe(EditorEvent.DeploymentProgress.class, e -> {
            progress.setValue(Math.max(0, Math.min(100, e.percent())));
            progress.setString(e.stage() + " " + e.percent() + "%");
            status.setText(e.detail());
        });
        eventBus.subscribe(EditorEvent.DeploymentFinished.class, e -> {
            progress.setValue(100);
            progress.setString("Complete");
            status.setText(e.summary());
            JOptionPane.showMessageDialog(this, e.summary(), "Mod deployment complete", JOptionPane.INFORMATION_MESSAGE);
        });
        eventBus.subscribe(EditorEvent.DeploymentFailed.class, e -> {
            progress.setString("Failed");
            status.setText(e.summary() + " " + e.detail());
            JOptionPane.showMessageDialog(this, e.summary() + "\n" + e.detail(), "Mod deployment failed", JOptionPane.ERROR_MESSAGE);
        });
    }

    private void selectTabByTitle(String title) {
        for (Component c : getContentPane().getComponents()) {
            if (c instanceof JTabbedPane tabs) {
                for (int i = 0; i < tabs.getTabCount(); i++) if (title.equals(tabs.getTitleAt(i))) { tabs.setSelectedIndex(i); return; }
            }
        }
    }
}

final class SimulationEditorSuiteMain {
    public static void main(String[] args) {
        DisplayDensityAuthority.configureJvmDisplayPropertiesBeforeSwing();
        DebugLog.init("0.9.10iq-editor-suite");
        SwingUtilities.invokeLater(SimulationEditorSuite::openWindow);
    }
}

class GenericEditorPanel extends JPanel {
    final String editorName;
    final SimulationEditorRepository repository;
    final EditorUndoRedoController undoRedo;
    final EditorEventBus eventBus;
    final EntityListModel entityModel;
    final PropertyTableModel propertyModel;
    final JList<SimulationEditorRepository.EditableEntity> entityList;
    final JTable propertyTable;
    final Map<String, List<String>> linkOptions;

    GenericEditorPanel(String editorName,
                       SimulationEditorRepository repository,
                       EditorUndoRedoController undoRedo,
                       EditorEventBus eventBus,
                       Map<String, List<String>> linkOptions) {
        super(new BorderLayout(8, 8));
        this.editorName = Objects.requireNonNull(editorName);
        this.repository = Objects.requireNonNull(repository);
        this.undoRedo = Objects.requireNonNull(undoRedo);
        this.eventBus = Objects.requireNonNull(eventBus);
        this.linkOptions = linkOptions == null ? Map.of() : new LinkedHashMap<>(linkOptions);
        setBorder(new EmptyBorder(8, 8, 8, 8));
        entityModel = new EntityListModel(repository.entities(editorName));
        entityList = new JList<>(entityModel);
        entityList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        entityList.setCellRenderer(new EntityCellRenderer());
        propertyModel = new PropertyTableModel(editorName, repository, undoRedo, eventBus);
        propertyTable = new PropertyTable(propertyModel, this.linkOptions);
        propertyTable.setRowHeight(28);
        propertyTable.setFillsViewportHeight(true);
        entityList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) selectEntity(entityList.getSelectedValue());
        });
        if (entityModel.getSize() > 0) entityList.setSelectedIndex(0);
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel(), rightPanel());
        split.setDividerLocation(310);
        add(split, BorderLayout.CENTER);
        eventBus.subscribe(EditorEvent.ModelChanged.class, e -> {
            if (editorName.equals(e.editorName())) refreshEntityListKeepingSelection();
        });
    }

    JComponent leftPanel() {
        JPanel left = new JPanel(new BorderLayout(6, 6));
        JLabel header = new JLabel(editorName + " Records");
        header.setFont(header.getFont().deriveFont(Font.BOLD));
        left.add(header, BorderLayout.NORTH);
        left.add(new JScrollPane(entityList), BorderLayout.CENTER);
        JTextArea help = new JTextArea(panelHelpText());
        help.setLineWrap(true);
        help.setWrapStyleWord(true);
        help.setEditable(false);
        help.setOpaque(false);
        help.setRows(4);
        left.add(help, BorderLayout.SOUTH);
        return left;
    }

    JComponent rightPanel() {
        JPanel right = new JPanel(new BorderLayout(6, 6));
        JLabel header = new JLabel("Properties — use tables, combo links, and bounded spinners for safe edits");
        right.add(header, BorderLayout.NORTH);
        right.add(new JScrollPane(propertyTable), BorderLayout.CENTER);
        return right;
    }

    String panelHelpText() {
        return switch (editorName) {
            case "Sector Editor" -> "Define coordinates, environmental hazards, and faction control. Numeric limits are bounded to avoid negative sector intensity or overflow.";
            case "Faction Editor" -> "Tune political alignment vectors, resources, aggression matrices, and cultural traits without coupling the view to runtime AI.";
            case "Item Editor" -> "Adjust tech tier, mass, components, durability, and decay. Mass and decay use bounded numeric editors.";
            case "Knowledge Editor" -> "Maintain progression nodes, blueprint unlocks, and research dependencies through link-aware property rows.";
            default -> "Edit structured simulation data through the shared repository and undoable commands.";
        };
    }

    void selectEntity(SimulationEditorRepository.EditableEntity entity) {
        if (entity == null) return;
        propertyModel.setEntity(new SimulationEditorRepository.EntityRef(editorName, entity.id()));
        eventBus.publish(new EditorEvent.SelectionChanged(editorName, entity.id()));
    }

    void refreshEntityListKeepingSelection() {
        SimulationEditorRepository.EditableEntity selected = entityList.getSelectedValue();
        String selectedId = selected == null ? null : selected.id();
        entityModel.setEntities(repository.entities(editorName));
        if (selectedId != null) {
            for (int i = 0; i < entityModel.getSize(); i++) if (selectedId.equals(entityModel.getElementAt(i).id())) { entityList.setSelectedIndex(i); break; }
        }
    }
}

final class RoomEditorPanel extends GenericEditorPanel {
    RoomEditorPanel(SimulationEditorRepository repository, EditorUndoRedoController undoRedo, EditorEventBus eventBus) {
        super("Room Editor", repository, undoRedo, eventBus, LinkCatalog.roomLinks(repository));
    }

    @Override JComponent rightPanel() {
        JPanel right = new JPanel(new BorderLayout(8, 8));
        JLabel header = new JLabel("Room layout — table properties plus visual stamp preview and palette categories");
        right.add(header, BorderLayout.NORTH);
        JSplitPane vertical = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                new JScrollPane(propertyTable),
                new RoomVisualLayoutPanel(repository, propertyModel, eventBus));
        vertical.setResizeWeight(0.42);
        vertical.setDividerLocation(260);
        right.add(vertical, BorderLayout.CENTER);
        return right;
    }

    @Override String panelHelpText() { return "Define interior dimensions, oxygen seals, security terminals, floor materials, and placement nodes. The right side includes a visual layout canvas and palette tabs for items, entity spawners, floor tiles, wall tiles, and objects."; }
}

final class RoomVisualLayoutPanel extends JPanel {
    private final SimulationEditorRepository repository;
    private final PropertyTableModel propertyModel;
    private final RoomStampCanvas canvas;
    private final JLabel selectionLabel = new JLabel("Palette selection: none");

    RoomVisualLayoutPanel(SimulationEditorRepository repository, PropertyTableModel propertyModel, EditorEventBus eventBus) {
        super(new BorderLayout(8, 8));
        this.repository = Objects.requireNonNull(repository);
        this.propertyModel = Objects.requireNonNull(propertyModel);
        setBorder(BorderFactory.createTitledBorder("Visual room layout editor scaffold"));
        canvas = new RoomStampCanvas(repository, propertyModel);
        add(canvas, BorderLayout.CENTER);
        add(buildPalette(), BorderLayout.EAST);
        add(selectionLabel, BorderLayout.SOUTH);
        eventBus.subscribe(EditorEvent.SelectionChanged.class, e -> { if ("Room Editor".equals(e.editorName())) canvas.repaint(); });
        eventBus.subscribe(EditorEvent.ModelChanged.class, e -> { if ("Room Editor".equals(e.editorName())) canvas.repaint(); });
    }

    private JComponent buildPalette() {
        JTabbedPane tabs = new JTabbedPane(JTabbedPane.TOP, JTabbedPane.SCROLL_TAB_LAYOUT);
        tabs.setPreferredSize(new Dimension(300, 220));
        addPaletteTab(tabs, "Items", List.of("sealed ration", "stubcarbine", "cogitator core", "repair kit", "water canister", "scrap bundle"));
        addPaletteTab(tabs, "Entity Spawners", List.of("civilian spawn", "guard patrol node", "scavenger camp node", "servitor route", "trader counter", "hostile ambush"));
        addPaletteTab(tabs, "Floor Tiles", List.of("worn-plasteel", "oil-stained iron", "stone and brass", "deck plating", "ceramite tile", "sump grate"));
        addPaletteTab(tabs, "Wall Tiles", List.of("hab partition", "bulkhead wall", "gothic stone", "corrugated barricade", "sealed hatch", "service conduit"));
        addPaletteTab(tabs, "Objects", List.of("cot", "sink", "dresser", "cabinet", "generator", "pillar", "altar", "terminal", "crate", "candle rack"));
        return tabs;
    }

    private void addPaletteTab(JTabbedPane tabs, String title, List<String> entries) {
        JList<String> list = new JList<>(entries.toArray(new String[0]));
        list.setVisibleRowCount(7);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                String value = list.getSelectedValue();
                selectionLabel.setText("Palette selection: " + title + (value == null ? "" : " / " + value));
                canvas.setGhostLabel(value == null ? title : value);
            }
        });
        tabs.addTab(title, new JScrollPane(list));
    }
}

final class RoomStampCanvas extends JComponent {
    private final SimulationEditorRepository repository;
    private final PropertyTableModel propertyModel;
    private String ghostLabel = "select palette entry";

    RoomStampCanvas(SimulationEditorRepository repository, PropertyTableModel propertyModel) {
        this.repository = repository;
        this.propertyModel = propertyModel;
        setMinimumSize(new Dimension(360, 220));
        setPreferredSize(new Dimension(520, 260));
        setToolTipText("Preview room footprint, walls, floor material, placement nodes, and current palette ghost.");
    }

    void setGhostLabel(String ghostLabel) {
        this.ghostLabel = ghostLabel == null || ghostLabel.isBlank() ? "select palette entry" : ghostLabel;
        repaint();
    }

    @Override protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth();
            int h = getHeight();
            g.setColor(new Color(20, 20, 20));
            g.fillRect(0, 0, w, h);
            SimulationEditorRepository.EditableEntity entity = repository.entity(propertyModel.ref());
            if (entity == null) {
                g.setColor(new Color(220, 205, 150));
                g.drawString("Select a room record to preview its stamp.", 18, 28);
                return;
            }
            Map<String, Object> props = entity.properties();
            int roomW = clampInt(props.get("width"), 1, 32, 6);
            int roomH = clampInt(props.get("height"), 1, 32, 6);
            int nodes = clampInt(props.get("placementNodes"), 0, 128, 0);
            boolean oxygenSeal = Boolean.TRUE.equals(props.get("oxygenSeal"));
            String floor = String.valueOf(props.getOrDefault("floorMaterial", "floor"));
            String terminal = String.valueOf(props.getOrDefault("securityTerminal", "None"));
            int titleH = 38;
            int infoH = 46;
            int usableW = Math.max(80, w - 32);
            int usableH = Math.max(80, h - titleH - infoH - 24);
            int cell = Math.max(8, Math.min(34, Math.min(usableW / Math.max(1, roomW), usableH / Math.max(1, roomH))));
            int gridW = cell * roomW;
            int gridH = cell * roomH;
            int ox = (w - gridW) / 2;
            int oy = titleH + Math.max(8, (usableH - gridH) / 2);
            g.setFont(getFont().deriveFont(Font.BOLD, 13f));
            g.setColor(new Color(235, 216, 142));
            g.drawString(entity.name() + "  [" + roomW + "x" + roomH + "]", 14, 24);
            g.setFont(getFont().deriveFont(12f));
            for (int y = 0; y < roomH; y++) {
                for (int x = 0; x < roomW; x++) {
                    boolean wall = x == 0 || y == 0 || x == roomW - 1 || y == roomH - 1;
                    int px = ox + x * cell;
                    int py = oy + y * cell;
                    g.setColor(wall ? new Color(92, 74, 48) : floorColor(floor));
                    g.fillRect(px, py, cell, cell);
                    g.setColor(new Color(14, 14, 12, 150));
                    g.drawRect(px, py, cell, cell);
                }
            }
            g.setColor(new Color(238, 215, 132));
            g.drawRect(ox - 1, oy - 1, gridW + 1, gridH + 1);
            paintPlacementNodes(g, ox, oy, cell, roomW, roomH, nodes);
            if (!"None".equalsIgnoreCase(terminal)) {
                g.setColor(new Color(90, 180, 110));
                int tx = ox + Math.max(1, roomW - 2) * cell + cell / 4;
                int ty = oy + Math.max(1, roomH / 2) * cell + cell / 4;
                g.fillRoundRect(tx, ty, Math.max(6, cell / 2), Math.max(6, cell / 2), 5, 5);
            }
            g.setColor(new Color(245, 230, 170, 205));
            int ghostW = Math.min(gridW - 8, Math.max(58, g.getFontMetrics().stringWidth(ghostLabel) + 16));
            g.drawRoundRect(ox + Math.max(4, gridW / 2 - ghostW / 2), oy + Math.max(4, gridH / 2 - 12), ghostW, 24, 8, 8);
            g.drawString(fit(ghostLabel, g.getFontMetrics(), ghostW - 12), ox + Math.max(10, gridW / 2 - ghostW / 2 + 8), oy + Math.max(20, gridH / 2 + 5));
            g.setColor(new Color(205, 190, 135));
            int infoY = h - infoH + 18;
            g.drawString("Floor: " + floor + "   Oxygen seal: " + oxygenSeal + "   Terminal: " + terminal, 14, infoY);
            g.setColor(new Color(150, 140, 105));
            g.drawString("Palette scaffold is read-only in this repair pass; it prevents blind room editing and prepares drag/drop placement wiring.", 14, infoY + 20);
        } finally {
            g.dispose();
        }
    }

    private void paintPlacementNodes(Graphics2D g, int ox, int oy, int cell, int roomW, int roomH, int nodes) {
        if (nodes <= 0) return;
        g.setColor(new Color(72, 160, 210));
        int interiorW = Math.max(1, roomW - 2);
        int interiorH = Math.max(1, roomH - 2);
        for (int i = 0; i < nodes; i++) {
            int px = 1 + (i * 3) % interiorW;
            int py = 1 + (i * 5 + i / Math.max(1, interiorW)) % interiorH;
            int cx = ox + px * cell + cell / 2;
            int cy = oy + py * cell + cell / 2;
            int r = Math.max(3, Math.min(7, cell / 5));
            g.fillOval(cx - r, cy - r, r * 2, r * 2);
        }
    }

    private Color floorColor(String floor) {
        String f = floor == null ? "" : floor.toLowerCase(Locale.ROOT);
        if (f.contains("oil")) return new Color(44, 48, 36);
        if (f.contains("stone")) return new Color(62, 55, 46);
        if (f.contains("deck")) return new Color(48, 56, 58);
        if (f.contains("ceramite")) return new Color(64, 64, 60);
        return new Color(50, 54, 48);
    }

    private static int clampInt(Object raw, int min, int max, int fallback) {
        int v = fallback;
        if (raw instanceof Number n) v = n.intValue();
        else if (raw != null) {
            try { v = Integer.parseInt(String.valueOf(raw).trim()); } catch (NumberFormatException ignored) { v = fallback; }
        }
        return Math.max(min, Math.min(max, v));
    }

    private static String fit(String text, FontMetrics fm, int maxWidth) {
        return GuiLayoutApi.fitLabel(text == null ? "" : text, fm, Math.max(8, maxWidth));
    }
}

final class InfopediaEditorPanel extends GenericEditorPanel {
    InfopediaEditorPanel(SimulationEditorRepository repository, EditorUndoRedoController undoRedo, EditorEventBus eventBus) {
        super("Infopedia Editor", repository, undoRedo, eventBus, LinkCatalog.infopediaLinks(repository));
    }
    @Override String panelHelpText() { return "Build nested wiki-style lore, historical text, taxonomy categories, revisions, and searchable tag lines."; }
}

final class ModPackagingPanel extends JPanel {
    private final SimulationEditorRepository repository;
    private final EditorUndoRedoController undoRedo;
    private final EditorEventBus eventBus;
    private final ModDeploymentManager deploymentManager;
    private final JTextField name = new JTextField();
    private final JTextField version = new JTextField();
    private final JTextField author = new JTextField();
    private final JTextField tags = new JTextField();
    private final JTextField dependencies = new JTextField();
    private final JTextField appId = new JTextField("0");
    private final JTextField publishedFileId = new JTextField("0");
    private final JTextArea description = new JTextArea(5, 40);
    private final JCheckBox preferSteam = new JCheckBox("Publish through Steam Workshop when the wrapper and Steam client are active");
    private final ScopeTableModel scopeModel;
    private final JProgressBar localProgress = new JProgressBar(0, 100);
    private SwingWorker<ModDeploymentManager.DeploymentResult, ModDeploymentManager.DeploymentProgress> activeWorker;

    ModPackagingPanel(SimulationEditorRepository repository, EditorUndoRedoController undoRedo, EditorEventBus eventBus, ModDeploymentManager deploymentManager) {
        super(new BorderLayout(8, 8));
        this.repository = repository;
        this.undoRedo = undoRedo;
        this.eventBus = eventBus;
        this.deploymentManager = deploymentManager;
        setBorder(new EmptyBorder(8, 8, 8, 8));
        loadMetadata();
        scopeModel = new ScopeTableModel(repository, undoRedo, eventBus);
        JTable scopeTable = new JTable(scopeModel);
        scopeTable.setRowHeight(26);
        scopeTable.setFillsViewportHeight(true);
        add(metadataPanel(), BorderLayout.NORTH);
        add(new JScrollPane(scopeTable), BorderLayout.CENTER);
        add(actionPanel(), BorderLayout.SOUTH);
        installEvents();
    }

    private JComponent metadataPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Mod metadata and Workshop routing"));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(3, 4, 3, 4);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1.0;
        addRow(panel, c, 0, "Name", name, "Version", version);
        addRow(panel, c, 1, "Author", author, "Tags", tags);
        addRow(panel, c, 2, "Dependencies", dependencies, "Steam App ID", appId);
        addRow(panel, c, 3, "Published File ID", publishedFileId, "", null);
        c.gridx = 0; c.gridy = 4; c.weightx = 0; panel.add(new JLabel("Description"), c);
        c.gridx = 1; c.gridy = 4; c.gridwidth = 3; c.weightx = 1; c.fill = GridBagConstraints.BOTH; c.weighty = 1;
        description.setLineWrap(true); description.setWrapStyleWord(true);
        panel.add(new JScrollPane(description), c);
        c.gridx = 0; c.gridy = 5; c.gridwidth = 4; c.weighty = 0; c.fill = GridBagConstraints.HORIZONTAL;
        panel.add(preferSteam, c);
        return panel;
    }

    private static void addRow(JPanel panel, GridBagConstraints c, int row, String leftLabel, JComponent left, String rightLabel, JComponent right) {
        c.gridwidth = 1; c.weighty = 0; c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 0; c.gridy = row; c.weightx = 0; panel.add(new JLabel(leftLabel), c);
        c.gridx = 1; c.gridy = row; c.weightx = 1; panel.add(left, c);
        c.gridx = 2; c.gridy = row; c.weightx = 0; if (!rightLabel.isEmpty()) panel.add(new JLabel(rightLabel), c);
        c.gridx = 3; c.gridy = row; c.weightx = 1; if (right != null) panel.add(right, c);
    }

    private JComponent actionPanel() {
        JPanel outer = new JPanel(new BorderLayout(8, 8));
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton refresh = new JButton("Refresh Scope");
        refresh.addActionListener(e -> scopeModel.refresh());
        JButton all = new JButton("Select All");
        all.addActionListener(e -> scopeModel.setAll(true));
        JButton none = new JButton("Select None");
        none.addActionListener(e -> scopeModel.setAll(false));
        JButton exportZip = new JButton("Export ZIP…");
        exportZip.addActionListener(this::exportZip);
        JButton deploy = new JButton("Deploy");
        deploy.addActionListener(this::deployPreferred);
        buttons.add(refresh); buttons.add(all); buttons.add(none); buttons.add(exportZip); buttons.add(deploy);
        localProgress.setStringPainted(true);
        localProgress.setString("Idle");
        outer.add(localProgress, BorderLayout.CENTER);
        outer.add(buttons, BorderLayout.EAST);
        return outer;
    }

    private void installEvents() {
        DocumentListener saver = new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { saveMetadata(); }
            @Override public void removeUpdate(DocumentEvent e) { saveMetadata(); }
            @Override public void changedUpdate(DocumentEvent e) { saveMetadata(); }
        };
        name.getDocument().addDocumentListener(saver);
        version.getDocument().addDocumentListener(saver);
        author.getDocument().addDocumentListener(saver);
        tags.getDocument().addDocumentListener(saver);
        dependencies.getDocument().addDocumentListener(saver);
        appId.getDocument().addDocumentListener(saver);
        publishedFileId.getDocument().addDocumentListener(saver);
        description.getDocument().addDocumentListener(saver);
        eventBus.subscribe(EditorEvent.DeploymentProgress.class, e -> {
            localProgress.setValue(e.percent());
            localProgress.setString(e.stage() + " " + e.percent() + "%");
        });
        eventBus.subscribe(EditorEvent.DeploymentFinished.class, e -> { localProgress.setValue(100); localProgress.setString("Complete"); scopeModel.refresh(); });
        eventBus.subscribe(EditorEvent.ModelChanged.class, e -> { if ("Mod Packaging".equals(e.editorName())) scopeModel.refresh(); });
    }

    private void loadMetadata() {
        SimulationEditorRepository.ModMetadata md = repository.metadata();
        name.setText(md.name()); version.setText(md.version()); author.setText(md.author()); description.setText(md.description());
        tags.setText(String.join(",", md.tags())); dependencies.setText(String.join(",", md.dependencies()));
        appId.setText(String.valueOf(md.steamAppId())); publishedFileId.setText(String.valueOf(md.publishedFileId()));
    }

    private void saveMetadata() {
        repository.updateModMetadata(name.getText(), version.getText(), author.getText(), description.getText(), tags.getText(), dependencies.getText(), parseLong(appId.getText()), parseLong(publishedFileId.getText()));
    }

    private void exportZip(ActionEvent event) { chooseAndDeploy(false); }
    private void deployPreferred(ActionEvent event) { chooseAndDeploy(preferSteam.isSelected()); }

    private void chooseAndDeploy(boolean preferSteamWorkshop) {
        if (activeWorker != null && !activeWorker.isDone()) {
            eventBus.publish(new EditorEvent.StatusChanged("A mod deployment is already running."));
            return;
        }
        saveMetadata();
        Path zipPath = null;
        if (!preferSteamWorkshop || !ModDeploymentManager.steamStatus().steamReady()) {
            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle("Export Mechanist Mod Archive");
            chooser.setSelectedFile(Path.of(SimulationEditorRepository.slug(name.getText()) + "-" + SimulationEditorRepository.slug(version.getText()) + ".zip").toFile());
            int result = chooser.showSaveDialog(this);
            if (result != JFileChooser.APPROVE_OPTION) return;
            zipPath = chooser.getSelectedFile().toPath();
        }
        ModDeploymentManager.DeploymentRequest request = new ModDeploymentManager.DeploymentRequest(
                repository.metadata(), repository.selectedEntities(), zipPath, preferSteamWorkshop,
                parseLong(publishedFileId.getText()) > 0 ? ModDeploymentManager.SteamPublicationMode.UPDATE_EXISTING_ITEM : ModDeploymentManager.SteamPublicationMode.CREATE_NEW_ITEM,
                parseLong(appId.getText()), parseLong(publishedFileId.getText()), null,
                "Published from The Mechanist Simulation Editor Suite.");
        localProgress.setValue(0); localProgress.setString("Starting");
        activeWorker = deploymentManager.deployAsync(request);
    }

    private static long parseLong(String text) {
        try { return Math.max(0L, Long.parseLong(text == null ? "0" : text.trim())); }
        catch (NumberFormatException ex) { return 0L; }
    }
}

final class EntityListModel extends javax.swing.AbstractListModel<SimulationEditorRepository.EditableEntity> {
    private final ArrayList<SimulationEditorRepository.EditableEntity> entities = new ArrayList<>();
    EntityListModel(List<SimulationEditorRepository.EditableEntity> entities) { setEntities(entities); }
    void setEntities(List<SimulationEditorRepository.EditableEntity> next) { entities.clear(); if (next != null) entities.addAll(next); fireContentsChanged(this, 0, Math.max(0, entities.size() - 1)); }
    @Override public int getSize() { return entities.size(); }
    @Override public SimulationEditorRepository.EditableEntity getElementAt(int index) { return entities.get(index); }
}

final class EntityCellRenderer extends DefaultListCellRenderer {
    @Override public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        if (value instanceof SimulationEditorRepository.EditableEntity entity) setText(entity.name() + "  [" + entity.id() + "]");
        return c;
    }
}

final class PropertyTableModel extends AbstractTableModel {
    private final String editorName;
    private final SimulationEditorRepository repository;
    private final EditorUndoRedoController undoRedo;
    private final EditorEventBus eventBus;
    private SimulationEditorRepository.EntityRef ref;
    private final ArrayList<String> keys = new ArrayList<>();
    private final ArrayList<Object> values = new ArrayList<>();

    PropertyTableModel(String editorName, SimulationEditorRepository repository, EditorUndoRedoController undoRedo, EditorEventBus eventBus) {
        this.editorName = editorName; this.repository = repository; this.undoRedo = undoRedo; this.eventBus = eventBus;
    }

    void setEntity(SimulationEditorRepository.EntityRef ref) {
        this.ref = ref;
        reload();
    }

    void reload() {
        keys.clear(); values.clear();
        SimulationEditorRepository.EditableEntity entity = repository.entity(ref);
        if (entity != null) {
            for (Map.Entry<String, Object> e : entity.properties().entrySet()) { keys.add(e.getKey()); values.add(e.getValue()); }
        }
        fireTableDataChanged();
    }

    SimulationEditorRepository.EntityRef ref() { return ref; }
    String propertyAt(int row) { return row >= 0 && row < keys.size() ? keys.get(row) : ""; }
    Object valueAt(int row) { return row >= 0 && row < values.size() ? values.get(row) : null; }

    @Override public int getRowCount() { return keys.size(); }
    @Override public int getColumnCount() { return 2; }
    @Override public String getColumnName(int column) { return column == 0 ? "Property" : "Value"; }
    @Override public boolean isCellEditable(int rowIndex, int columnIndex) { return columnIndex == 1 && ref != null; }
    @Override public Object getValueAt(int rowIndex, int columnIndex) { return columnIndex == 0 ? propertyAt(rowIndex) : valueAt(rowIndex); }
    @Override public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        if (columnIndex != 1 || ref == null || rowIndex < 0 || rowIndex >= keys.size()) return;
        String property = keys.get(rowIndex);
        Object old = repository.property(ref, property);
        Object next = coerce(old, aValue);
        undoRedo.execute(new EditorCommand.PropertyChange(repository, ref, property, old, next, eventBus));
        reload();
    }

    private Object coerce(Object old, Object value) {
        if (old instanceof Integer) {
            if (value instanceof Number n) return n.intValue();
            try { return Integer.parseInt(String.valueOf(value).trim()); } catch (NumberFormatException ex) { return old; }
        }
        if (old instanceof Long) {
            if (value instanceof Number n) return n.longValue();
            try { return Long.parseLong(String.valueOf(value).trim()); } catch (NumberFormatException ex) { return old; }
        }
        if (old instanceof Double) {
            if (value instanceof Number n) return n.doubleValue();
            try { return Double.parseDouble(String.valueOf(value).trim()); } catch (NumberFormatException ex) { return old; }
        }
        if (old instanceof Boolean) return value instanceof Boolean b ? b : Boolean.parseBoolean(String.valueOf(value));
        return value == null ? "" : String.valueOf(value);
    }
}

final class PropertyTable extends JTable {
    private final Map<String, List<String>> linkOptions;
    private final TableCellRenderer linkRenderer = new LinkValueRenderer();
    PropertyTable(PropertyTableModel model, Map<String, List<String>> linkOptions) {
        super(model);
        this.linkOptions = linkOptions == null ? Map.of() : linkOptions;
        getColumnModel().getColumn(0).setPreferredWidth(180);
        getColumnModel().getColumn(1).setPreferredWidth(420);
    }

    @Override public TableCellEditor getCellEditor(int row, int column) {
        if (column != 1) return super.getCellEditor(row, column);
        PropertyTableModel model = (PropertyTableModel)getModel();
        String property = model.propertyAt(convertRowIndexToModel(row));
        Object value = model.valueAt(convertRowIndexToModel(row));
        List<String> links = linkOptions.get(property);
        if (links != null && !links.isEmpty()) return new DefaultCellEditor(new JComboBox<>(new DefaultComboBoxModel<>(links.toArray(new String[0]))));
        if (value instanceof Boolean) return new DefaultCellEditor(new JCheckBox());
        if (value instanceof Number n) return new SpinnerCellEditor(n);
        return super.getCellEditor(row, column);
    }

    @Override public TableCellRenderer getCellRenderer(int row, int column) {
        if (column == 1) {
            PropertyTableModel model = (PropertyTableModel)getModel();
            String property = model.propertyAt(convertRowIndexToModel(row));
            if (linkOptions.containsKey(property)) return linkRenderer;
        }
        return super.getCellRenderer(row, column);
    }
}

final class LinkValueRenderer extends DefaultTableCellRenderer {
    @Override public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        JLabel c = (JLabel)super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        c.setText("↪ " + String.valueOf(value));
        if (!isSelected) c.setForeground(new Color(48, 84, 126));
        return c;
    }
}

final class SpinnerCellEditor extends AbstractCellEditor implements TableCellEditor {
    private final JSpinner spinner;
    SpinnerCellEditor(Number current) {
        double value = current == null ? 0d : current.doubleValue();
        double max = Math.max(1000d, Math.abs(value) * 20d + 100d);
        spinner = new JSpinner(new SpinnerNumberModel(value, 0d, max, value < 1d ? 0.01d : 1d));
    }
    @Override public Object getCellEditorValue() { return spinner.getValue(); }
    @Override public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
        if (value instanceof Number n) spinner.setValue(n.doubleValue());
        return spinner;
    }
}

final class ScopeTableModel extends AbstractTableModel {
    private final SimulationEditorRepository repository;
    private final EditorUndoRedoController undoRedo;
    private final EditorEventBus eventBus;
    private final ArrayList<SimulationEditorRepository.EntityRef> refs = new ArrayList<>();

    ScopeTableModel(SimulationEditorRepository repository, EditorUndoRedoController undoRedo, EditorEventBus eventBus) {
        this.repository = repository; this.undoRedo = undoRedo; this.eventBus = eventBus; refresh();
    }
    void refresh() { refs.clear(); refs.addAll(repository.allEntityRefs()); fireTableDataChanged(); }
    void setAll(boolean value) {
        for (SimulationEditorRepository.EntityRef ref : new ArrayList<>(refs)) {
            boolean old = repository.selected(ref);
            undoRedo.execute(new EditorCommand.ToggleProjectSelection(repository, ref, old, value, eventBus));
        }
        refresh();
    }
    @Override public int getRowCount() { return refs.size(); }
    @Override public int getColumnCount() { return 5; }
    @Override public String getColumnName(int column) { return switch (column) { case 0 -> "Selected"; case 1 -> "Editor"; case 2 -> "ID"; case 3 -> "Name"; default -> "Property Count"; }; }
    @Override public Class<?> getColumnClass(int columnIndex) { return columnIndex == 0 ? Boolean.class : columnIndex == 4 ? Integer.class : String.class; }
    @Override public boolean isCellEditable(int rowIndex, int columnIndex) { return columnIndex == 0; }
    @Override public Object getValueAt(int rowIndex, int columnIndex) {
        SimulationEditorRepository.EntityRef ref = refs.get(rowIndex);
        SimulationEditorRepository.EditableEntity entity = repository.entity(ref);
        return switch (columnIndex) {
            case 0 -> repository.selected(ref);
            case 1 -> ref.editorName();
            case 2 -> ref.entityId();
            case 3 -> entity == null ? "missing" : entity.name();
            default -> entity == null ? 0 : entity.properties().size();
        };
    }
    @Override public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        if (columnIndex != 0 || rowIndex < 0 || rowIndex >= refs.size()) return;
        SimulationEditorRepository.EntityRef ref = refs.get(rowIndex);
        boolean old = repository.selected(ref);
        boolean next = aValue instanceof Boolean b && b;
        undoRedo.execute(new EditorCommand.ToggleProjectSelection(repository, ref, old, next, eventBus));
        fireTableRowsUpdated(rowIndex, rowIndex);
    }
}

final class LinkCatalog {
    private LinkCatalog() { }
    static Map<String, List<String>> sectorLinks(SimulationEditorRepository repository) { return Map.of("factionControl", names(repository, "Faction Editor"), "hazard", List.of("Industrial Smog", "Ash Storms", "Vacuum Breach", "Radiation Leak", "Gang War", "None")); }
    static Map<String, List<String>> roomLinks(SimulationEditorRepository repository) { return Map.of("securityTerminal", List.of("None", "Local Panel", "Reliquary Seal", "Arbites Lock", "Guild Terminal"), "floorMaterial", List.of("worn-plasteel", "oil-stained iron", "stone and brass", "deck plating", "ceramite tile")); }
    static Map<String, List<String>> factionLinks(SimulationEditorRepository repository) { return Map.of("culture", List.of("Order / Deterrence", "Contracts / Leverage", "Survival / Salvage", "Devotion / Charity", "Technocracy / Maintenance")); }
    static Map<String, List<String>> itemLinks(SimulationEditorRepository repository) { return Map.of("components", List.of("steel, springs, firing pin", "nutrient brick, wax paper", "copper, crystal matrix, logic stack", "plasteel, seals, pressure gauge", "paper, ink, thread")); }
    static Map<String, List<String>> knowledgeLinks(SimulationEditorRepository repository) { return Map.of("parent", namesWithRoot(repository, "Knowledge Editor"), "blueprint", List.of("none", "micro-assembler", "sealed hatch", "water recycler", "security terminal", "orbital trade relay")); }
    static Map<String, List<String>> infopediaLinks(SimulationEditorRepository repository) { return Map.of("category", List.of("World", "Factions", "Lore", "Items", "Knowledge", "Rooms", "Threats")); }
    private static List<String> names(SimulationEditorRepository repository, String editor) {
        ArrayList<String> out = new ArrayList<>();
        for (SimulationEditorRepository.EditableEntity e : repository.entities(editor)) out.add(e.name());
        return out.isEmpty() ? List.of("None") : out;
    }
    private static List<String> namesWithRoot(SimulationEditorRepository repository, String editor) {
        ArrayList<String> out = new ArrayList<>(); out.add("root"); out.addAll(names(repository, editor)); return out;
    }
}
