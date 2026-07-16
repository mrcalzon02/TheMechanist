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
    static final String VERSION = "simulation-editor-suite-0.9.10ir";

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

    static String auditSummary() {
        return "authority=" + VERSION + " legacy-external-window=disabled development-only-code=true tabs="
                + (SimulationToolSuiteRegistry.specs().size() + 1)
                + " mvc=event-bus undo-redo=true room-visual-layout=true palette-tabs=5 export=zip+steam-reflection-swingworker "
                + SimulationToolSuiteRegistry.auditSummary();
    }

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
        for (SimulationToolSuiteRegistry.ToolSpec spec : SimulationToolSuiteRegistry.specs()) {
            tabs.addTab(spec.editorName(), panelFor(spec.editorName()));
        }
        tabs.addTab("Mod Packaging Editor", new ModPackagingPanel(repository, undoRedo, eventBus, deploymentManager));
        return tabs;
    }

    private JComponent panelFor(String editorName) {
        if (SimulationToolSuiteRegistry.SECTOR_EDITOR.equals(editorName)) {
            return new SectorEditorPanel(repository, undoRedo, eventBus);
        }
        if (SimulationToolSuiteRegistry.ROOM_EDITOR.equals(editorName)) {
            return new RoomEditorPanel(repository, undoRedo, eventBus);
        }
        if (SimulationToolSuiteRegistry.FACTION_EDITOR.equals(editorName)) {
            return new FactionEditorPanel(repository, undoRedo, eventBus);
        }
        if (SimulationToolSuiteRegistry.QUEST_EDITOR.equals(editorName)) {
            return new QuestEditorPanel(repository, undoRedo, eventBus);
        }
        if (SimulationToolSuiteRegistry.ITEM_EDITOR.equals(editorName)) {
            return new ItemEditorPanel(repository, undoRedo, eventBus);
        }
        if (SimulationToolSuiteRegistry.TILE_EDITOR.equals(editorName)) {
            return new TileEditorPanel(repository, undoRedo, eventBus);
        }
        if (SimulationToolSuiteRegistry.OBJECT_EDITOR.equals(editorName)) {
            return new ObjectEditorPanel(repository, undoRedo, eventBus);
        }
        if (SimulationToolSuiteRegistry.INFOPEDIA_EDITOR.equals(editorName)) {
            return new InfopediaEditorPanel(repository, undoRedo, eventBus);
        }
        return new GenericEditorPanel(editorName, repository, undoRedo, eventBus,
                SimulationToolSuiteRegistry.linkOptionsFor(editorName, repository));
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
        return SimulationToolSuiteRegistry.panelHelpText(editorName);
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

final class SectorEditorPanel extends GenericEditorPanel {
    SectorEditorPanel(SimulationEditorRepository repository, EditorUndoRedoController undoRedo, EditorEventBus eventBus) {
        super(SimulationToolSuiteRegistry.SECTOR_EDITOR, repository, undoRedo, eventBus, LinkCatalog.sectorLinks(repository));
    }

    @Override JComponent rightPanel() {
        JPanel right = new JPanel(new BorderLayout(8, 8));
        JLabel header = new JLabel("Sector world-generation editor - whole-sector map, generation walk, and overlay contracts");
        right.add(header, BorderLayout.NORTH);
        JSplitPane vertical = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                new JScrollPane(propertyTable),
                new SectorMapEditorPanel(repository, propertyModel, eventBus));
        vertical.setResizeWeight(0.38);
        vertical.setDividerLocation(260);
        right.add(vertical, BorderLayout.CENTER);
        return right;
    }

    @Override String panelHelpText() {
        return "Dedicated world-generation surface for sector-wide settings. Zone Auditor remains the slice audit/replay tool; Sector Editor owns whole-sector layout, generation sequencing, overlays, and tweakable worldgen contracts.";
    }
}

final class SectorMapEditorPanel extends JPanel {
    private final SectorMapCanvas canvas;
    private final JList<String> generationList;
    private final JCheckBox zones = new JCheckBox("Zones", true);
    private final JCheckBox rooms = new JCheckBox("Rooms", true);
    private final JCheckBox objects = new JCheckBox("Objects", true);
    private final JCheckBox interactions = new JCheckBox("Interactions", true);
    private final JCheckBox lights = new JCheckBox("Lights", true);

    SectorMapEditorPanel(SimulationEditorRepository repository, PropertyTableModel propertyModel, EditorEventBus eventBus) {
        super(new BorderLayout(8, 8));
        setBorder(BorderFactory.createTitledBorder("Whole-sector map and generation walk scaffold"));
        canvas = new SectorMapCanvas(repository, propertyModel, this);
        generationList = new JList<>(new String[]{
                "1. Seed spawning zone and sector envelope",
                "2. Lay transit spine and zone grid",
                "3. Generate each zone by density and zone type",
                "4. Place plaza, roads, boundaries, and maintenance bands",
                "5. Carve rooms, corridors, frontage, and faction claims",
                "6. Stamp objects, containers, interactions, and lights",
                "7. Populate entities, traps, hazards, and transitions",
                "8. Compile tile descriptors and run Zone Auditor findings"
        });
        generationList.setVisibleRowCount(8);
        generationList.setSelectedIndex(0);
        generationList.addListSelectionListener(e -> { if (!e.getValueIsAdjusting()) canvas.setGenerationStep(generationList.getSelectedIndex()); });
        add(canvas, BorderLayout.CENTER);
        add(controlPanel(), BorderLayout.EAST);
        eventBus.subscribe(EditorEvent.SelectionChanged.class, e -> { if (SimulationToolSuiteRegistry.SECTOR_EDITOR.equals(e.editorName())) canvas.repaint(); });
        eventBus.subscribe(EditorEvent.ModelChanged.class, e -> { if (SimulationToolSuiteRegistry.SECTOR_EDITOR.equals(e.editorName())) canvas.repaint(); });
    }

    private JComponent controlPanel() {
        JPanel panel = new JPanel(new BorderLayout(6, 6));
        panel.setPreferredSize(new Dimension(320, 220));
        JPanel overlays = new JPanel(new GridBagLayout());
        overlays.setBorder(BorderFactory.createTitledBorder("Overlays"));
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0; c.gridy = 0; c.anchor = GridBagConstraints.WEST; c.insets = new Insets(2, 2, 2, 2);
        for (JCheckBox box : List.of(zones, rooms, objects, interactions, lights)) {
            box.addActionListener(e -> canvas.repaint());
            overlays.add(box, c);
            c.gridy++;
        }
        panel.add(overlays, BorderLayout.NORTH);
        panel.add(new JScrollPane(generationList), BorderLayout.CENTER);
        return panel;
    }

    boolean showZones() { return zones.isSelected(); }
    boolean showRooms() { return rooms.isSelected(); }
    boolean showObjects() { return objects.isSelected(); }
    boolean showInteractions() { return interactions.isSelected(); }
    boolean showLights() { return lights.isSelected(); }
}

final class SectorMapCanvas extends JComponent {
    private final SimulationEditorRepository repository;
    private final PropertyTableModel propertyModel;
    private final SectorMapEditorPanel controls;
    private int generationStep = 0;

    SectorMapCanvas(SimulationEditorRepository repository, PropertyTableModel propertyModel, SectorMapEditorPanel controls) {
        this.repository = repository;
        this.propertyModel = propertyModel;
        this.controls = controls;
        setMinimumSize(new Dimension(420, 260));
        setPreferredSize(new Dimension(620, 320));
        setToolTipText("Whole-sector preview: spawning zone, zone grid, route spine, rooms, objects, interactions, and lights.");
    }

    void setGenerationStep(int generationStep) {
        this.generationStep = Math.max(0, generationStep);
        repaint();
    }

    @Override protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth();
            int h = getHeight();
            g.setColor(new Color(18, 20, 22));
            g.fillRect(0, 0, w, h);
            SimulationEditorRepository.EditableEntity entity = repository.entity(propertyModel.ref());
            if (entity == null) {
                g.setColor(new Color(220, 205, 150));
                g.drawString("Select a sector record to preview the whole-sector worldgen map.", 18, 28);
                return;
            }
            Map<String, Object> props = entity.properties();
            int cols = clampInt(props.get("sectorWidthZones"), 2, 12, 5);
            int rows = clampInt(props.get("sectorHeightZones"), 2, 12, 5);
            int titleH = 34;
            int footerH = 36;
            int cell = Math.max(18, Math.min(58, Math.min((w - 48) / cols, (h - titleH - footerH - 28) / rows)));
            int gridW = cols * cell;
            int gridH = rows * cell;
            int ox = Math.max(16, (w - gridW) / 2);
            int oy = titleH + Math.max(10, (h - titleH - footerH - gridH) / 2);
            int spawnX = clampInt(props.get("spawnZoneX"), 0, cols - 1, cols / 2);
            int spawnY = clampInt(props.get("spawnZoneY"), 0, rows - 1, rows / 2);
            g.setFont(getFont().deriveFont(Font.BOLD, 13f));
            g.setColor(new Color(235, 216, 142));
            g.drawString(entity.name() + "  [" + cols + "x" + rows + " zones]  step " + (generationStep + 1), 14, 23);
            for (int y = 0; y < rows; y++) {
                for (int x = 0; x < cols; x++) {
                    int sx = ox + x * cell;
                    int sy = oy + y * cell;
                    paintZoneCell(g, sx, sy, cell, x, y, spawnX, spawnY);
                }
            }
            if (controls.showZones()) {
                g.setColor(new Color(238, 215, 132));
                g.drawRect(ox - 1, oy - 1, gridW + 1, gridH + 1);
            }
            if (controls.showRooms()) paintRoomOverlay(g, ox, oy, cell, cols, rows);
            if (controls.showObjects()) paintDotOverlay(g, ox, oy, cell, cols, rows, new Color(124, 230, 144, 190), 0);
            if (controls.showInteractions()) paintDotOverlay(g, ox, oy, cell, cols, rows, new Color(116, 188, 235, 190), 1);
            if (controls.showLights()) paintDotOverlay(g, ox, oy, cell, cols, rows, new Color(245, 214, 118, 205), 2);
            g.setColor(new Color(205, 190, 135));
            String route = String.valueOf(props.getOrDefault("routeSpine", "main transit spine"));
            String tuning = String.valueOf(props.getOrDefault("worldgenTuning", "standard"));
            g.drawString("Spawn " + spawnX + "," + spawnY + " | route " + route + " | tuning " + tuning, 14, h - 14);
        } finally {
            g.dispose();
        }
    }

    private void paintZoneCell(Graphics2D g, int sx, int sy, int cell, int x, int y, int spawnX, int spawnY) {
        boolean spawn = x == spawnX && y == spawnY;
        boolean spine = y == spawnY || x == spawnX;
        g.setColor(spawn ? new Color(86, 72, 42) : spine ? new Color(40, 54, 58) : new Color(36, 38, 36));
        g.fillRect(sx, sy, cell, cell);
        g.setColor(new Color(0, 0, 0, 120));
        g.drawRect(sx, sy, cell, cell);
        if (spawn) {
            g.setColor(new Color(245, 214, 118));
            g.fillOval(sx + cell / 3, sy + cell / 3, Math.max(6, cell / 3), Math.max(6, cell / 3));
        }
    }

    private void paintRoomOverlay(Graphics2D g, int ox, int oy, int cell, int cols, int rows) {
        g.setColor(new Color(116, 188, 235, 82));
        for (int y = 0; y < rows; y++) for (int x = 0; x < cols; x++) {
            int pad = Math.max(4, cell / 6);
            g.fillRect(ox + x * cell + pad, oy + y * cell + pad, Math.max(4, cell - pad * 2), Math.max(4, cell - pad * 2));
        }
    }

    private void paintDotOverlay(Graphics2D g, int ox, int oy, int cell, int cols, int rows, Color color, int offset) {
        g.setColor(color);
        int r = Math.max(3, cell / 10);
        for (int y = 0; y < rows; y++) for (int x = 0; x < cols; x++) {
            int cx = ox + x * cell + cell / 4 + ((x + offset) % 3) * cell / 5;
            int cy = oy + y * cell + cell / 4 + ((y + offset) % 3) * cell / 5;
            g.fillOval(cx - r, cy - r, r * 2, r * 2);
        }
    }

    private static int clampInt(Object raw, int min, int max, int fallback) {
        int v = fallback;
        if (raw instanceof Number n) v = n.intValue();
        else if (raw != null) {
            try { v = Integer.parseInt(String.valueOf(raw).trim()); } catch (NumberFormatException ignored) { v = fallback; }
        }
        return Math.max(min, Math.min(max, v));
    }
}

final class FactionEditorPanel extends GenericEditorPanel {
    FactionEditorPanel(SimulationEditorRepository repository, EditorUndoRedoController undoRedo, EditorEventBus eventBus) {
        super(SimulationToolSuiteRegistry.FACTION_EDITOR, repository, undoRedo, eventBus, LinkCatalog.factionLinks(repository));
    }

    @Override JComponent rightPanel() {
        JPanel right = new JPanel(new BorderLayout(8, 8));
        JLabel header = new JLabel("Faction editor - alignment, diplomacy, resources, schemes, leaders, and journals");
        right.add(header, BorderLayout.NORTH);
        JSplitPane vertical = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                new JScrollPane(propertyTable),
                new FactionProfilePanel(repository, propertyModel, eventBus));
        vertical.setResizeWeight(0.42);
        vertical.setDividerLocation(270);
        right.add(vertical, BorderLayout.CENTER);
        return right;
    }

    @Override String panelHelpText() {
        return "Recovered faction editor surface for alignment, resources, aggression, diplomacy, scheme posture, leader roles, journal policy, home rooms, and standing effects.";
    }
}

final class FactionProfilePanel extends JPanel {
    private final SimulationEditorRepository repository;
    private final PropertyTableModel propertyModel;

    FactionProfilePanel(SimulationEditorRepository repository, PropertyTableModel propertyModel, EditorEventBus eventBus) {
        super(new BorderLayout(8, 8));
        this.repository = repository;
        this.propertyModel = propertyModel;
        setBorder(BorderFactory.createTitledBorder("Faction profile and scheme preview"));
        add(new FactionProfileCanvas(repository, propertyModel), BorderLayout.CENTER);
        eventBus.subscribe(EditorEvent.SelectionChanged.class, e -> { if (SimulationToolSuiteRegistry.FACTION_EDITOR.equals(e.editorName())) repaint(); });
        eventBus.subscribe(EditorEvent.ModelChanged.class, e -> { if (SimulationToolSuiteRegistry.FACTION_EDITOR.equals(e.editorName())) repaint(); });
    }
}

final class FactionProfileCanvas extends JComponent {
    private final SimulationEditorRepository repository;
    private final PropertyTableModel propertyModel;

    FactionProfileCanvas(SimulationEditorRepository repository, PropertyTableModel propertyModel) {
        this.repository = repository;
        this.propertyModel = propertyModel;
        setMinimumSize(new Dimension(420, 230));
        setPreferredSize(new Dimension(620, 270));
        setToolTipText("Preview faction alignment vectors, resource posture, scheme posture, leadership, journal policy, and standing effects.");
    }

    @Override protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D)graphics.create();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth();
            int h = getHeight();
            g.setColor(new Color(18, 20, 22));
            g.fillRect(0, 0, w, h);
            SimulationEditorRepository.EditableEntity entity = repository.entity(propertyModel.ref());
            if (entity == null) {
                g.setColor(new Color(220, 205, 150));
                g.drawString("Select a faction record to preview alignment and scheme posture.", 18, 28);
                return;
            }
            Map<String, Object> props = entity.properties();
            g.setFont(getFont().deriveFont(Font.BOLD, 13f));
            g.setColor(new Color(235, 216, 142));
            g.drawString(entity.name(), 14, 24);
            int barX = 24;
            int barY = 48;
            int barW = Math.max(160, w / 3);
            paintBar(g, "Law", clampInt(props.get("lawful"), 0, 10, 5), barX, barY, barW, new Color(116, 188, 235));
            paintBar(g, "Trade", clampInt(props.get("mercantile"), 0, 10, 5), barX, barY + 34, barW, new Color(124, 230, 144));
            paintBar(g, "Tech", clampInt(props.get("technocratic"), 0, 10, 5), barX, barY + 68, barW, new Color(172, 120, 235));
            paintBar(g, "Aggression", clampInt(props.get("aggression"), 0, 10, 3), barX, barY + 102, barW, new Color(230, 82, 68));
            int resource = clampInt(props.get("resources"), 0, 2000, 100);
            paintBar(g, "Resources", Math.min(10, resource / 100), barX, barY + 136, barW, new Color(245, 214, 118));
            int textX = barX + barW + 36;
            g.setFont(getFont().deriveFont(12f));
            g.setColor(new Color(205, 190, 135));
            drawLine(g, "Culture: " + props.getOrDefault("culture", "unknown"), textX, 56, w - textX - 16);
            drawLine(g, "Diplomacy: " + props.getOrDefault("diplomacyProfile", "neutral"), textX, 80, w - textX - 16);
            drawLine(g, "Scheme: " + props.getOrDefault("schemePosture", "none"), textX, 104, w - textX - 16);
            drawLine(g, "Leader: " + props.getOrDefault("leaderRole", "unknown"), textX, 128, w - textX - 16);
            drawLine(g, "Journal: " + props.getOrDefault("journalPolicy", "none"), textX, 152, w - textX - 16);
            drawLine(g, "Rooms: " + props.getOrDefault("homeRooms", "unassigned"), textX, 176, w - textX - 16);
            drawLine(g, "Standing: " + props.getOrDefault("standingEffects", "standard"), textX, 200, w - textX - 16);
        } finally {
            g.dispose();
        }
    }

    private void paintBar(Graphics2D g, String label, int value, int x, int y, int w, Color color) {
        int v = Math.max(0, Math.min(10, value));
        g.setColor(new Color(38, 39, 36));
        g.fillRect(x + 82, y - 12, w, 16);
        g.setColor(color);
        g.fillRect(x + 82, y - 12, Math.max(2, w * v / 10), 16);
        g.setColor(new Color(205, 190, 135));
        g.drawString(label, x, y);
        g.drawRect(x + 82, y - 12, w, 16);
    }

    private void drawLine(Graphics2D g, String text, int x, int y, int maxWidth) {
        g.drawString(GuiLayoutApi.fitLabel(text, g.getFontMetrics(), Math.max(60, maxWidth)), x, y);
    }

    private static int clampInt(Object raw, int min, int max, int fallback) {
        int v = fallback;
        if (raw instanceof Number n) v = n.intValue();
        else if (raw != null) {
            try { v = Integer.parseInt(String.valueOf(raw).trim()); } catch (NumberFormatException ignored) { v = fallback; }
        }
        return Math.max(min, Math.min(max, v));
    }
}

final class QuestEditorPanel extends GenericEditorPanel {
    QuestEditorPanel(SimulationEditorRepository repository, EditorUndoRedoController undoRedo, EditorEventBus eventBus) {
        super(SimulationToolSuiteRegistry.QUEST_EDITOR, repository, undoRedo, eventBus, LinkCatalog.questLinks(repository));
    }

    @Override JComponent rightPanel() {
        JPanel right = new JPanel(new BorderLayout(8, 8));
        JLabel header = new JLabel("Quest editor - lifecycle, objectives, evidence, guidance, rewards, consequences, and validation");
        right.add(header, BorderLayout.NORTH);
        JSplitPane vertical = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                new JScrollPane(propertyTable),
                new QuestLifecyclePanel(repository, propertyModel, eventBus));
        vertical.setResizeWeight(0.42);
        vertical.setDividerLocation(270);
        right.add(vertical, BorderLayout.CENTER);
        return right;
    }

    @Override String panelHelpText() {
        return "Recovered Quest Editor authoring surface for quest IDs, localization, lifecycle state, objectives, evidence, guidance, rewards, consequences, timers, missed-window rules, and validation.";
    }
}

final class QuestLifecyclePanel extends JPanel {
    private final QuestLifecycleCanvas canvas;
    private final JList<String> validationList;

    QuestLifecyclePanel(SimulationEditorRepository repository, PropertyTableModel propertyModel, EditorEventBus eventBus) {
        super(new BorderLayout(8, 8));
        setBorder(BorderFactory.createTitledBorder("Quest lifecycle and validation preview"));
        canvas = new QuestLifecycleCanvas(repository, propertyModel);
        validationList = new JList<>(new String[]{
                "Stable quest ID and localization key",
                "Lifecycle state and timer window",
                "Objective type, target reference, and guidance mode",
                "Evidence/proof rule for critical items or death targets",
                "Reward and consequence definitions",
                "Missed-window behavior is explicit",
                "Validation notes are present before export"
        });
        validationList.setVisibleRowCount(7);
        validationList.setPreferredSize(new Dimension(300, 180));
        add(canvas, BorderLayout.CENTER);
        add(new JScrollPane(validationList), BorderLayout.EAST);
        eventBus.subscribe(EditorEvent.SelectionChanged.class, e -> { if (SimulationToolSuiteRegistry.QUEST_EDITOR.equals(e.editorName())) canvas.repaint(); });
        eventBus.subscribe(EditorEvent.ModelChanged.class, e -> { if (SimulationToolSuiteRegistry.QUEST_EDITOR.equals(e.editorName())) canvas.repaint(); });
    }
}

final class QuestLifecycleCanvas extends JComponent {
    private static final String[] STATES = {"planning", "execution", "active", "cooldown", "completed", "failed", "expired", "aftermath"};
    private final SimulationEditorRepository repository;
    private final PropertyTableModel propertyModel;

    QuestLifecycleCanvas(SimulationEditorRepository repository, PropertyTableModel propertyModel) {
        this.repository = repository;
        this.propertyModel = propertyModel;
        setMinimumSize(new Dimension(420, 230));
        setPreferredSize(new Dimension(620, 270));
        setToolTipText("Preview quest lifecycle, objective guidance, evidence rule, rewards, consequences, and validation status.");
    }

    @Override protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D)graphics.create();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth();
            int h = getHeight();
            g.setColor(new Color(18, 20, 22));
            g.fillRect(0, 0, w, h);
            SimulationEditorRepository.EditableEntity entity = repository.entity(propertyModel.ref());
            if (entity == null) {
                g.setColor(new Color(220, 205, 150));
                g.drawString("Select a quest record to preview lifecycle and validation.", 18, 28);
                return;
            }
            Map<String, Object> props = entity.properties();
            String state = String.valueOf(props.getOrDefault("lifecycleState", "planning")).toLowerCase(Locale.ROOT);
            g.setFont(getFont().deriveFont(Font.BOLD, 13f));
            g.setColor(new Color(235, 216, 142));
            g.drawString(entity.name(), 14, 24);
            int trackX = 24;
            int trackY = 58;
            int stepW = Math.max(58, (w - 48) / STATES.length);
            for (int i = 0; i < STATES.length; i++) {
                int x = trackX + i * stepW;
                boolean current = STATES[i].equals(state);
                g.setColor(current ? new Color(245, 214, 118) : new Color(47, 48, 43));
                g.fillRect(x, trackY, Math.max(34, stepW - 8), 24);
                g.setColor(new Color(0, 0, 0, 120));
                g.drawRect(x, trackY, Math.max(34, stepW - 8), 24);
                g.setColor(current ? new Color(18, 20, 22) : new Color(205, 190, 135));
                g.drawString(GuiLayoutApi.fitLabel(STATES[i], g.getFontMetrics(), Math.max(30, stepW - 12)), x + 4, trackY + 17);
            }
            int y = trackY + 60;
            g.setFont(getFont().deriveFont(12f));
            g.setColor(new Color(205, 190, 135));
            drawLine(g, "Family: " + props.getOrDefault("questFamily", "unknown") + " | Source: " + props.getOrDefault("sourceFaction", "none"), 24, y, w - 48);
            drawLine(g, "Objective: " + props.getOrDefault("objectiveType", "none") + " -> " + props.getOrDefault("targetRef", "unassigned"), 24, y + 24, w - 48);
            drawLine(g, "Guidance: " + props.getOrDefault("objectiveGuidance", "none") + " | Evidence: " + props.getOrDefault("evidenceRule", "none"), 24, y + 48, w - 48);
            drawLine(g, "Reward: " + props.getOrDefault("reward", "none") + " | Consequence: " + props.getOrDefault("consequence", "none"), 24, y + 72, w - 48);
            drawLine(g, "Missed window: " + props.getOrDefault("missedWindowRule", "unspecified"), 24, y + 96, w - 48);
        } finally {
            g.dispose();
        }
    }

    private void drawLine(Graphics2D g, String text, int x, int y, int maxWidth) {
        g.drawString(GuiLayoutApi.fitLabel(text, g.getFontMetrics(), Math.max(60, maxWidth)), x, y);
    }
}

final class RoomEditorPanel extends GenericEditorPanel {
    RoomEditorPanel(SimulationEditorRepository repository, EditorUndoRedoController undoRedo, EditorEventBus eventBus) {
        super(SimulationToolSuiteRegistry.ROOM_EDITOR, repository, undoRedo, eventBus, LinkCatalog.roomLinks(repository));
    }

    @Override JComponent rightPanel() {
        JPanel right = new JPanel(new BorderLayout(8, 8));
        JLabel header = new JLabel("Room layout — table properties plus visual stamp preview and palette categories");
        right.add(header, BorderLayout.NORTH);
        JSplitPane vertical = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                new JScrollPane(propertyTable),
                new RoomVisualLayoutPanel(repository, propertyModel, undoRedo, eventBus));
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
    private final EditorUndoRedoController undoRedo;
    private final EditorEventBus eventBus;
    private final RoomStampCanvas canvas;
    private final JLabel selectionLabel = new JLabel("Palette selection: none");

    RoomVisualLayoutPanel(SimulationEditorRepository repository, PropertyTableModel propertyModel, EditorUndoRedoController undoRedo, EditorEventBus eventBus) {
        super(new BorderLayout(8, 8));
        this.repository = Objects.requireNonNull(repository);
        this.propertyModel = Objects.requireNonNull(propertyModel);
        this.undoRedo = Objects.requireNonNull(undoRedo);
        this.eventBus = Objects.requireNonNull(eventBus);
        setBorder(BorderFactory.createTitledBorder("Visual room layout editor scaffold"));
        canvas = new RoomStampCanvas(repository, propertyModel, undoRedo, eventBus);
        add(buildShapeControls(), BorderLayout.NORTH);
        add(canvas, BorderLayout.CENTER);
        add(buildPalette(), BorderLayout.EAST);
        add(selectionLabel, BorderLayout.SOUTH);
        eventBus.subscribe(EditorEvent.SelectionChanged.class, e -> { if (SimulationToolSuiteRegistry.ROOM_EDITOR.equals(e.editorName())) canvas.repaint(); });
        eventBus.subscribe(EditorEvent.ModelChanged.class, e -> { if (SimulationToolSuiteRegistry.ROOM_EDITOR.equals(e.editorName())) canvas.repaint(); });
    }

    private JComponent buildShapeControls() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        JButton widthMinus = new JButton("Width -");
        widthMinus.addActionListener(e -> adjustIntProperty("width", -1, 1, 32));
        JButton widthPlus = new JButton("Width +");
        widthPlus.addActionListener(e -> adjustIntProperty("width", 1, 1, 32));
        JButton heightMinus = new JButton("Height -");
        heightMinus.addActionListener(e -> adjustIntProperty("height", -1, 1, 32));
        JButton heightPlus = new JButton("Height +");
        heightPlus.addActionListener(e -> adjustIntProperty("height", 1, 1, 32));
        JButton square = new JButton("Square");
        square.addActionListener(e -> squareRoom());
        JButton zoomOut = new JButton("Zoom -");
        zoomOut.addActionListener(e -> canvas.adjustZoom(-1));
        JButton zoomIn = new JButton("Zoom +");
        zoomIn.addActionListener(e -> canvas.adjustZoom(1));
        panel.add(widthMinus); panel.add(widthPlus); panel.add(heightMinus); panel.add(heightPlus); panel.add(square);
        panel.add(zoomOut); panel.add(zoomIn);
        return panel;
    }

    private void adjustIntProperty(String property, int delta, int min, int max) {
        SimulationEditorRepository.EntityRef ref = propertyModel.ref();
        if (ref == null) return;
        Object old = repository.property(ref, property);
        int current = RoomStampCanvas.clampInt(old, min, max, property.equals("width") ? 8 : 6);
        int next = Math.max(min, Math.min(max, current + delta));
        undoRedo.execute(new EditorCommand.PropertyChange(repository, ref, property, old, next, eventBus));
        propertyModel.reload();
        canvas.repaint();
    }

    private void squareRoom() {
        SimulationEditorRepository.EntityRef ref = propertyModel.ref();
        if (ref == null) return;
        int width = RoomStampCanvas.clampInt(repository.property(ref, "width"), 1, 32, 8);
        Object old = repository.property(ref, "height");
        undoRedo.execute(new EditorCommand.PropertyChange(repository, ref, "height", old, width, eventBus));
        propertyModel.reload();
        canvas.repaint();
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
    private final EditorUndoRedoController undoRedo;
    private final EditorEventBus eventBus;
    private String ghostLabel = "select palette entry";
    private int zoomPercent = 100;
    private Rectangle lastGrid = new Rectangle();
    private int lastCell = 16;
    private int lastRoomW = 1;
    private int lastRoomH = 1;

    RoomStampCanvas(SimulationEditorRepository repository, PropertyTableModel propertyModel, EditorUndoRedoController undoRedo, EditorEventBus eventBus) {
        this.repository = repository;
        this.propertyModel = propertyModel;
        this.undoRedo = undoRedo;
        this.eventBus = eventBus;
        setMinimumSize(new Dimension(360, 220));
        setPreferredSize(new Dimension(520, 260));
        setToolTipText("Preview room footprint, walls, floor material, placement nodes, and current palette ghost.");
        addMouseWheelListener(e -> adjustZoom(e.getWheelRotation() < 0 ? 1 : -1));
        addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                placePaletteAt(e.getX(), e.getY());
            }
        });
    }

    void setGhostLabel(String ghostLabel) {
        this.ghostLabel = ghostLabel == null || ghostLabel.isBlank() ? "select palette entry" : ghostLabel;
        repaint();
    }

    void adjustZoom(int delta) {
        zoomPercent = Math.max(60, Math.min(220, zoomPercent + delta * 10));
        repaint();
    }

    private void placePaletteAt(int sx, int sy) {
        SimulationEditorRepository.EntityRef ref = propertyModel.ref();
        if (ref == null || !lastGrid.contains(sx, sy)) return;
        int x = Math.max(0, Math.min(lastRoomW - 1, (sx - lastGrid.x) / Math.max(1, lastCell)));
        int y = Math.max(0, Math.min(lastRoomH - 1, (sy - lastGrid.y) / Math.max(1, lastCell)));
        String clean = ghostLabel == null || ghostLabel.isBlank() ? "marker" : ghostLabel.replace(',', '_').replace(';', '_');
        Object old = repository.property(ref, "layoutCells");
        String prefix = old == null || String.valueOf(old).isBlank() ? "" : String.valueOf(old) + ";";
        String next = prefix + x + "," + y + "," + clean;
        undoRedo.execute(new EditorCommand.PropertyChange(repository, ref, "layoutCells", old, next, eventBus));
        propertyModel.reload();
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
            int baseCell = Math.max(8, Math.min(34, Math.min(usableW / Math.max(1, roomW), usableH / Math.max(1, roomH))));
            int cell = Math.max(6, Math.min(64, baseCell * zoomPercent / 100));
            int gridW = cell * roomW;
            int gridH = cell * roomH;
            int ox = (w - gridW) / 2;
            int oy = titleH + Math.max(8, (usableH - gridH) / 2);
            lastGrid = new Rectangle(ox, oy, gridW, gridH);
            lastCell = cell;
            lastRoomW = roomW;
            lastRoomH = roomH;
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
            paintPlacedCells(g, props, ox, oy, cell);
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
            g.drawString("Click a grid cell to place the selected palette entry. Wheel or Zoom buttons adjust preview scale.", 14, infoY + 20);
        } finally {
            g.dispose();
        }
    }

    private void paintPlacedCells(Graphics2D g, Map<String, Object> props, int ox, int oy, int cell) {
        String layout = String.valueOf(props.getOrDefault("layoutCells", ""));
        if (layout.isBlank()) return;
        g.setFont(getFont().deriveFont(Font.BOLD, Math.max(9f, cell * 0.35f)));
        FontMetrics fm = g.getFontMetrics();
        for (String part : layout.split(";")) {
            String[] pieces = part.split(",", 3);
            if (pieces.length < 3) continue;
            try {
                int x = Integer.parseInt(pieces[0].trim());
                int y = Integer.parseInt(pieces[1].trim());
                if (x < 0 || y < 0 || x >= lastRoomW || y >= lastRoomH) continue;
                int px = ox + x * cell;
                int py = oy + y * cell;
                g.setColor(new Color(124, 230, 144, 138));
                g.fillRect(px + 3, py + 3, Math.max(4, cell - 6), Math.max(4, cell - 6));
                g.setColor(new Color(18, 20, 22));
                String label = GuiLayoutApi.fitLabel(pieces[2], fm, Math.max(10, cell - 8));
                g.drawString(label, px + 4, py + Math.max(12, cell / 2));
            } catch (NumberFormatException ignored) {
            }
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

    static int clampInt(Object raw, int min, int max, int fallback) {
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
        super(SimulationToolSuiteRegistry.INFOPEDIA_EDITOR, repository, undoRedo, eventBus, LinkCatalog.infopediaLinks(repository));
    }

    @Override JComponent rightPanel() {
        JPanel right = new JPanel(new BorderLayout(8, 8));
        JLabel header = new JLabel("Infopedia text editor - structured fields plus multiline article body");
        right.add(header, BorderLayout.NORTH);
        JSplitPane vertical = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                new JScrollPane(propertyTable),
                new TextDetailEditorPanel(SimulationToolSuiteRegistry.INFOPEDIA_EDITOR, "body", "Article Body", repository, propertyModel, undoRedo, eventBus));
        vertical.setResizeWeight(0.42);
        vertical.setDividerLocation(260);
        right.add(vertical, BorderLayout.CENTER);
        return right;
    }

    @Override String panelHelpText() { return "Build nested wiki-style lore, historical text, taxonomy categories, revisions, and searchable tag lines."; }
}

final class ItemEditorPanel extends GenericEditorPanel {
    ItemEditorPanel(SimulationEditorRepository repository, EditorUndoRedoController undoRedo, EditorEventBus eventBus) {
        super(SimulationToolSuiteRegistry.ITEM_EDITOR, repository, undoRedo, eventBus, LinkCatalog.itemLinks(repository));
    }

    @Override JComponent rightPanel() {
        JPanel right = new JPanel(new BorderLayout(8, 8));
        JLabel header = new JLabel("Item editor - structured stats plus multiline description and provenance text");
        right.add(header, BorderLayout.NORTH);
        JSplitPane vertical = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                new JScrollPane(propertyTable),
                new TextDetailEditorPanel(SimulationToolSuiteRegistry.ITEM_EDITOR, "description", "Item Description", repository, propertyModel, undoRedo, eventBus));
        vertical.setResizeWeight(0.45);
        vertical.setDividerLocation(270);
        right.add(vertical, BorderLayout.CENTER);
        return right;
    }

    @Override String panelHelpText() {
        return "Edit item stats, components, durability, unlock state, semantic tags, and player-facing description text.";
    }
}

final class TileEditorPanel extends GenericEditorPanel {
    TileEditorPanel(SimulationEditorRepository repository, EditorUndoRedoController undoRedo, EditorEventBus eventBus) {
        super(SimulationToolSuiteRegistry.TILE_EDITOR, repository, undoRedo, eventBus, LinkCatalog.tileLinks(repository));
    }

    @Override JComponent rightPanel() {
        JPanel right = new JPanel(new BorderLayout(8, 8));
        JLabel header = new JLabel("Tile editor - glyph, walkability, opacity, semantic asset, light, hazard, and interaction properties");
        right.add(header, BorderLayout.NORTH);
        JSplitPane vertical = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                new JScrollPane(propertyTable),
                new TilePreviewPanel(repository, propertyModel, eventBus));
        vertical.setResizeWeight(0.45);
        vertical.setDividerLocation(270);
        right.add(vertical, BorderLayout.CENTER);
        return right;
    }

    @Override String panelHelpText() {
        return "Edit tile definitions used by world generation, rendering, collision, light blocking, road/room/boundary roles, hazards, semantic assets, and inspection hints.";
    }
}

final class TilePreviewPanel extends JPanel {
    TilePreviewPanel(SimulationEditorRepository repository, PropertyTableModel propertyModel, EditorEventBus eventBus) {
        super(new BorderLayout(8, 8));
        setBorder(BorderFactory.createTitledBorder("Tile property preview"));
        TilePreviewCanvas canvas = new TilePreviewCanvas(repository, propertyModel);
        add(canvas, BorderLayout.CENTER);
        eventBus.subscribe(EditorEvent.SelectionChanged.class, e -> { if (SimulationToolSuiteRegistry.TILE_EDITOR.equals(e.editorName())) canvas.repaint(); });
        eventBus.subscribe(EditorEvent.ModelChanged.class, e -> { if (SimulationToolSuiteRegistry.TILE_EDITOR.equals(e.editorName())) canvas.repaint(); });
    }
}

final class TilePreviewCanvas extends JComponent {
    private final SimulationEditorRepository repository;
    private final PropertyTableModel propertyModel;
    private int zoomPercent = 100;

    TilePreviewCanvas(SimulationEditorRepository repository, PropertyTableModel propertyModel) {
        this.repository = repository;
        this.propertyModel = propertyModel;
        setMinimumSize(new Dimension(420, 220));
        setPreferredSize(new Dimension(620, 260));
        addMouseWheelListener(e -> { zoomPercent = Math.max(60, Math.min(220, zoomPercent + (e.getWheelRotation() < 0 ? 10 : -10))); repaint(); });
    }

    @Override protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D)graphics.create();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth();
            int h = getHeight();
            g.setColor(new Color(18, 20, 22));
            g.fillRect(0, 0, w, h);
            SimulationEditorRepository.EditableEntity entity = repository.entity(propertyModel.ref());
            if (entity == null) {
                g.setColor(new Color(220, 205, 150));
                g.drawString("Select a tile record to preview its runtime properties.", 18, 28);
                return;
            }
            Map<String, Object> props = entity.properties();
            int tile = Math.max(48, Math.min(140, 84 * zoomPercent / 100));
            int ox = 34;
            int oy = 46;
            boolean walkable = Boolean.TRUE.equals(props.get("walkable"));
            boolean opaque = Boolean.TRUE.equals(props.get("opaque"));
            String glyph = String.valueOf(props.getOrDefault("glyph", "?"));
            g.setColor(walkable ? new Color(47, 54, 44) : new Color(64, 48, 42));
            g.fillRect(ox, oy, tile, tile);
            g.setColor(opaque ? new Color(230, 82, 68) : new Color(124, 230, 144));
            g.drawRect(ox, oy, tile, tile);
            g.setFont(getFont().deriveFont(Font.BOLD, Math.max(24f, tile * 0.55f)));
            g.setColor(new Color(235, 216, 142));
            String s = glyph.isBlank() ? "?" : glyph.substring(0, 1);
            FontMetrics fm = g.getFontMetrics();
            g.drawString(s, ox + (tile - fm.stringWidth(s)) / 2, oy + (tile + fm.getAscent() - fm.getDescent()) / 2);
            int tx = ox + tile + 34;
            g.setFont(getFont().deriveFont(12f));
            g.setColor(new Color(205, 190, 135));
            drawLine(g, entity.name() + " [" + props.getOrDefault("semanticAssetId", "no asset") + "]", tx, 60, w - tx - 20);
            drawLine(g, "Family: " + props.getOrDefault("family", "unknown") + " | Road: " + props.getOrDefault("roadRole", "none"), tx, 84, w - tx - 20);
            drawLine(g, "Room: " + props.getOrDefault("roomRole", "none") + " | Boundary: " + props.getOrDefault("boundaryRole", "none"), tx, 108, w - tx - 20);
            drawLine(g, "Walkable: " + walkable + " | Opaque: " + opaque, tx, 132, w - tx - 20);
            drawLine(g, "Light: " + props.getOrDefault("lightProfile", "none") + " | Hazard: " + props.getOrDefault("hazardProfile", "none"), tx, 156, w - tx - 20);
            drawLine(g, "Interaction: " + props.getOrDefault("interactionHint", "inspect"), tx, 180, w - tx - 20);
        } finally {
            g.dispose();
        }
    }

    private void drawLine(Graphics2D g, String text, int x, int y, int maxWidth) {
        g.drawString(GuiLayoutApi.fitLabel(text, g.getFontMetrics(), Math.max(60, maxWidth)), x, y);
    }
}

final class ObjectEditorPanel extends GenericEditorPanel {
    ObjectEditorPanel(SimulationEditorRepository repository, EditorUndoRedoController undoRedo, EditorEventBus eventBus) {
        super(SimulationToolSuiteRegistry.OBJECT_EDITOR, repository, undoRedo, eventBus, LinkCatalog.objectLinks(repository));
    }

    @Override JComponent rightPanel() {
        JPanel right = new JPanel(new BorderLayout(8, 8));
        JLabel header = new JLabel("Object editor - footprint, properties, functions, interactions, lights, Infopedia, and blocking");
        right.add(header, BorderLayout.NORTH);
        JSplitPane vertical = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                new JScrollPane(propertyTable),
                new ObjectPreviewPanel(repository, propertyModel, eventBus));
        vertical.setResizeWeight(0.45);
        vertical.setDividerLocation(270);
        right.add(vertical, BorderLayout.CENTER);
        return right;
    }

    @Override String panelHelpText() {
        return "Attach object properties, function hooks, interaction verbs, light profiles, footprint/blocking behavior, material identity, and Infopedia links.";
    }
}

final class ObjectPreviewPanel extends JPanel {
    ObjectPreviewPanel(SimulationEditorRepository repository, PropertyTableModel propertyModel, EditorEventBus eventBus) {
        super(new BorderLayout(8, 8));
        setBorder(BorderFactory.createTitledBorder("Object footprint, function, and light preview"));
        ObjectPreviewCanvas canvas = new ObjectPreviewCanvas(repository, propertyModel);
        add(canvas, BorderLayout.CENTER);
        eventBus.subscribe(EditorEvent.SelectionChanged.class, e -> { if (SimulationToolSuiteRegistry.OBJECT_EDITOR.equals(e.editorName())) canvas.repaint(); });
        eventBus.subscribe(EditorEvent.ModelChanged.class, e -> { if (SimulationToolSuiteRegistry.OBJECT_EDITOR.equals(e.editorName())) canvas.repaint(); });
    }
}

final class ObjectPreviewCanvas extends JComponent {
    private final SimulationEditorRepository repository;
    private final PropertyTableModel propertyModel;
    private int zoomPercent = 100;

    ObjectPreviewCanvas(SimulationEditorRepository repository, PropertyTableModel propertyModel) {
        this.repository = repository;
        this.propertyModel = propertyModel;
        setMinimumSize(new Dimension(420, 220));
        setPreferredSize(new Dimension(620, 260));
        addMouseWheelListener(e -> { zoomPercent = Math.max(60, Math.min(220, zoomPercent + (e.getWheelRotation() < 0 ? 10 : -10))); repaint(); });
    }

    @Override protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D)graphics.create();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth();
            int h = getHeight();
            g.setColor(new Color(18, 20, 22));
            g.fillRect(0, 0, w, h);
            SimulationEditorRepository.EditableEntity entity = repository.entity(propertyModel.ref());
            if (entity == null) {
                g.setColor(new Color(220, 205, 150));
                g.drawString("Select an object record to preview footprint, functions, and light.", 18, 28);
                return;
            }
            Map<String, Object> props = entity.properties();
            int cell = Math.max(28, Math.min(72, 42 * zoomPercent / 100));
            int fw = footprintPart(props.get("footprint"), 0, 1);
            int fh = footprintPart(props.get("footprint"), 1, 1);
            int ox = 42;
            int oy = 52;
            g.setColor(new Color(47, 48, 43));
            g.fillRect(ox, oy, fw * cell, fh * cell);
            g.setColor(Boolean.TRUE.equals(props.get("blocksMovement")) ? new Color(230, 82, 68) : new Color(124, 230, 144));
            g.drawRect(ox, oy, fw * cell, fh * cell);
            g.setColor(new Color(116, 188, 235, 90));
            int radius = RoomStampCanvas.clampInt(props.get("lightRadius"), 0, 12, 0) * cell;
            if (radius > 0) g.fillOval(ox + fw * cell / 2 - radius / 2, oy + fh * cell / 2 - radius / 2, radius, radius);
            g.setColor(new Color(245, 214, 118));
            g.fillOval(ox + fw * cell / 2 - 5, oy + fh * cell / 2 - 5, 10, 10);
            int tx = ox + Math.max(160, fw * cell + 34);
            g.setFont(getFont().deriveFont(Font.BOLD, 13f));
            g.drawString(entity.name(), 14, 24);
            g.setFont(getFont().deriveFont(12f));
            g.setColor(new Color(205, 190, 135));
            drawLine(g, "Type: " + props.getOrDefault("objectType", "unknown") + " | Material: " + props.getOrDefault("material", "unknown"), tx, 60, w - tx - 20);
            drawLine(g, "Interaction: " + props.getOrDefault("interaction", "Inspect") + " | Function: " + props.getOrDefault("functionHook", "inspect"), tx, 84, w - tx - 20);
            drawLine(g, "Properties: " + props.getOrDefault("properties", "none"), tx, 108, w - tx - 20);
            drawLine(g, "Lighting: " + props.getOrDefault("lightingHook", "none") + " radius " + props.getOrDefault("lightRadius", 0) + " color " + props.getOrDefault("lightColor", "warm"), tx, 132, w - tx - 20);
            drawLine(g, "Infopedia: " + props.getOrDefault("infopediaEntry", "none") + " | Blocks: " + props.getOrDefault("blocksMovement", false), tx, 156, w - tx - 20);
        } finally {
            g.dispose();
        }
    }

    private static int footprintPart(Object raw, int index, int fallback) {
        String[] parts = String.valueOf(raw == null ? "" : raw).toLowerCase(Locale.ROOT).split("x");
        if (index >= parts.length) return fallback;
        try { return Math.max(1, Math.min(8, Integer.parseInt(parts[index].trim()))); }
        catch (NumberFormatException ex) { return fallback; }
    }

    private void drawLine(Graphics2D g, String text, int x, int y, int maxWidth) {
        g.drawString(GuiLayoutApi.fitLabel(text, g.getFontMetrics(), Math.max(60, maxWidth)), x, y);
    }
}

final class TextDetailEditorPanel extends JPanel {
    private final String editorName;
    private final String propertyName;
    private final SimulationEditorRepository repository;
    private final PropertyTableModel propertyModel;
    private final EditorUndoRedoController undoRedo;
    private final EditorEventBus eventBus;
    private final JTextArea text = new JTextArea();
    private boolean loading;

    TextDetailEditorPanel(String editorName, String propertyName, String title,
                          SimulationEditorRepository repository, PropertyTableModel propertyModel,
                          EditorUndoRedoController undoRedo, EditorEventBus eventBus) {
        super(new BorderLayout(6, 6));
        this.editorName = editorName;
        this.propertyName = propertyName;
        this.repository = repository;
        this.propertyModel = propertyModel;
        this.undoRedo = undoRedo;
        this.eventBus = eventBus;
        setBorder(BorderFactory.createTitledBorder(title));
        text.setLineWrap(true);
        text.setWrapStyleWord(true);
        text.setRows(7);
        add(new JScrollPane(text), BorderLayout.CENTER);
        JButton apply = new JButton("Apply Text");
        apply.addActionListener(e -> applyText());
        add(apply, BorderLayout.SOUTH);
        eventBus.subscribe(EditorEvent.SelectionChanged.class, e -> { if (editorName.equals(e.editorName())) loadText(); });
        eventBus.subscribe(EditorEvent.ModelChanged.class, e -> {
            if (editorName.equals(e.editorName()) && propertyName.equals(e.propertyName())) loadText();
        });
        loadText();
    }

    private void loadText() {
        loading = true;
        try {
            Object value = repository.property(propertyModel.ref(), propertyName);
            text.setText(value == null ? "" : String.valueOf(value));
            text.setCaretPosition(0);
        } finally {
            loading = false;
        }
    }

    private void applyText() {
        if (loading || propertyModel.ref() == null) return;
        Object old = repository.property(propertyModel.ref(), propertyName);
        String next = text.getText() == null ? "" : text.getText();
        undoRedo.execute(new EditorCommand.PropertyChange(repository, propertyModel.ref(), propertyName, old, next, eventBus));
        propertyModel.reload();
    }
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
        double min = Math.min(-1000d, value * 20d - 100d);
        double max = Math.max(1000d, Math.abs(value) * 20d + 100d);
        spinner = new JSpinner(new SpinnerNumberModel(value, min, max, Math.abs(value) < 1d ? 0.01d : 1d));
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
    static Map<String, List<String>> sectorLinks(SimulationEditorRepository repository) { return Map.of("factionControl", names(repository, SimulationToolSuiteRegistry.FACTION_EDITOR), "hazard", List.of("Industrial Smog", "Ash Storms", "Vacuum Breach", "Radiation Leak", "Gang War", "None"), "routeSpine", List.of("main transit spine", "exposed service road", "cargo transit ring", "maintenance corridor web", "market artery"), "worldgenTuning", List.of("standard", "dense civic-industrial", "sparse hazardous", "dock-heavy", "hab-heavy", "utility-heavy"), "overlaySet", List.of("zones,rooms,objects,interactions,lights,transitions,findings", "zones,rooms,objects,interactions,lights,hazards,findings")); }
    static Map<String, List<String>> zoneLinks(SimulationEditorRepository repository) { return Map.of("sector", names(repository, SimulationToolSuiteRegistry.SECTOR_EDITOR), "zoneType", List.of("industrial", "wastes", "dock", "hab", "market", "utility"), "density", List.of("low", "medium", "high"), "auditOverlay", List.of("zones", "hazards", "routes", "rooms", "findings"), "lightProfile", List.of("civil hab functional", "neutral civilian maintained", "upper noble maintained", "civic sanctioned", "industrial duty", "lower arcology scavenged", "sump/sewer sparse")); }
    static Map<String, List<String>> roomLinks(SimulationEditorRepository repository) { return Map.of("securityTerminal", List.of("None", "Local Panel", "Reliquary Seal", "Arbites Lock", "Guild Terminal"), "floorMaterial", List.of("worn-plasteel", "oil-stained iron", "stone and brass", "deck plating", "ceramite tile")); }
    static Map<String, List<String>> factionLinks(SimulationEditorRepository repository) { return Map.of("culture", List.of("Order / Deterrence", "Contracts / Leverage", "Survival / Salvage", "Devotion / Charity", "Technocracy / Maintenance"), "diplomacyProfile", List.of("neutral", "lawful enforcement", "contract pressure", "opportunist", "devotional", "isolationist"), "schemePosture", List.of("consolidation", "containment patrol", "market expansion", "raid preparation", "defense", "production surge", "sabotage", "recovery"), "leaderRole", List.of("cell leader", "precinct marshal", "factor-master", "crew boss", "magos delegate", "guild broker"), "journalPolicy", List.of("office ledger", "sealed evidence ledger", "trade ledger", "stolen notebook", "encrypted data-slate", "verbal only"), "standingEffects", List.of("standard reputation", "law reputation, heat reduction if sanctioned", "trade discounts, debt pressure", "black-market access, ambush risk")); }
    static Map<String, List<String>> populationLinks(SimulationEditorRepository repository) { return Map.of("faction", factionIds(), "originMode", List.of("arcology-born", "faction birth", "ward intake", "contract labor", "train import", "paid local", "refugee", "prisoner"), "sourceKind", List.of("resident roster", "duty roster", "industrial workforce", "immature cohort", "imported personnel", "custody population"), "workforceProfile", List.of("residents and civilian services", "industrial labor", "security duty", "medical care", "agricultural work", "noble household", "one care provider per twelve children"), "demandProfile", List.of("food,water,bed,pay", "food,water,medicine,temporary housing", "ammunition,medicine,duty food", "tools,components,work food", "luxury,security,private medicine", "double food,water,pediatric care,teaching")); }
    static Map<String, List<String>> economyLinks(SimulationEditorRepository repository) { return Map.of("faction", factionIds(), "ledgerType", List.of("essential reserve", "security reserve", "medical reserve", "noble luxury reserve", "draught custody", "animal and agriculture reserve", "raw material reserve", "vertical trade reserve", "shipment", "market pressure"), "sourceKind", List.of("local production", "local salvage", "local stockpile", "train import", "outside-sector shipment", "off-world import", "event relief", "confiscation", "unresolved fallback"), "legality", List.of("ordinary legal stock", "restricted", "military issue", "noble-only", "black-market", "contraband", "stolen", "counterfeit", "contaminated", "event-restricted"), "vendorCategory", List.of("provisions", "armory", "medical", "industrial", "blueprint", "animal-care", "luxury", "black-market"), "marketPreference", List.of("essential", "security", "production input", "controlled medicine", "narcotics", "luxury", "prestige custody", "external sale"), "contractHook", List.of("depleted reserve delivery", "delayed shipment recovery", "reinforcement support", "event response", "market pressure relief", "none")); }
    static Map<String, List<String>> reinforcementLinks(SimulationEditorRepository repository) { return Map.of("faction", factionIds(), "sourceMode", List.of("train-import", "barracks-muster", "paid-local", "legacy-roster"), "prerequisite", List.of("faction-linked rail intake", "faction barracks or duty building", "available local population roster", "linked population roster"), "importNode", List.of("sector exchange", "rail cargo station", "freight elevator", "service lift", "customs checkpoint", "road loading bay", "cargo dock", "private estate import", "smuggling entry", "sewer freight hoist", "local roster")); }
    static Map<String, List<String>> deferredNetworkLinks(SimulationEditorRepository repository) { return Map.of("faction", factionIds(), "lastFocus", List.of("network establishment", "shipment support", "reinforcement support", "raw material supply", "warehouse security", "rival interference", "leadership recovery"), "lastOutcome", List.of("No outcome yet.", "supply improved", "shipment delayed", "reinforcement advanced", "raw materials disrupted", "influence increased", "wealth reduced")); }
    static Map<String, List<String>> worldEventLinks(SimulationEditorRepository repository) { return Map.of("eventType", List.of("RELIEF_SHIPMENT", "INFRASTRUCTURE_REPAIR", "TRAIN_OUTAGE", "EXPORT_BAN", "TITHING_DECREE", "QUARANTINE", "SUPPLY_SHOCK", "CIVIC_OBSERVANCE"), "status", List.of("SCHEDULED", "ACTIVE", "RECOVERED"), "scope", List.of("room", "zone", "level", "sector", "outside-sector trade"), "targetFaction", factionIds(), "marketCategory", List.of("all local commerce", "food, water, and medicine", "freight and reinforcement traffic", "off-map exports", "general commerce", "medicine and passenger traffic", "raw materials and critical supplies", "local services"), "roomMutationHook", List.of("none", "temporary relief intake", "repair worksite", "closed import platform", "inspection queue", "tithe collection point", "quarantine checkpoint", "shortage-control receiving room"), "newsExposure", List.of("civic notices", "market notices and broadcasts", "newspapers, broadcasts, and vendor notices", "rumors before confirmation")); }
    static Map<String, List<String>> questLinks(SimulationEditorRepository repository) { return Map.of("questFamily", List.of("scheme quest", "contract", "delivery", "kill/evidence", "investigation", "theft", "escort", "construction", "medical", "vehicle", "pet/animal", "tutorial", "intelligence sale", "recovery", "sabotage", "defense", "assault", "aftermath"), "lifecycleState", List.of("planning", "execution", "active", "cooldown", "completed", "failed", "cancelled", "leaked", "compromised", "expired", "aftermath", "repeatable"), "sourceFaction", names(repository, SimulationToolSuiteRegistry.FACTION_EDITOR), "questGiver", List.of("NPC", "faction representative", "precinct marshal", "rival factor", "crew boss", "terminal/document", "journal", "vendor", "hidden trigger"), "objectiveType", List.of("recover evidence", "deliver item", "disable machine", "recover proof-of-death", "investigate room", "escort actor", "defend room", "sabotage target", "sell intelligence", "construct object"), "objectiveGuidance", List.of("exact target highlight", "target corpse highlight", "objective arrow", "nearest transition arrow", "approximate search area", "rumored target", "hidden target", "intentionally vague"), "evidenceRule", List.of("zero-weight proof item", "proof item appears regardless of killer", "optional copied plan", "consumed on turn-in", "retained after turn-in", "copy/forge allowed"), "missedWindowRule", List.of("neutral if unaccepted", "scheme completes if ignored", "aftermath recovery remains available", "accepted failure affects standing", "expires without standing loss"), "reward", List.of("money,reputation", "money,law reputation", "money,trade leverage", "money,scavenger standing", "blueprint,knowledge", "item,reputation"), "consequence", List.of("heat,scheme disruption", "reduced scheme heat,scavenger retaliation", "production delay,guard search", "target faction hostility", "press scandal", "rival counterattack")); }
    static Map<String, List<String>> itemLinks(SimulationEditorRepository repository) { return Map.of("components", List.of("steel, springs, firing pin", "nutrient brick, wax paper", "copper, crystal matrix, logic stack", "plasteel, seals, pressure gauge", "paper, ink, thread"), "infopediaEntry", names(repository, SimulationToolSuiteRegistry.INFOPEDIA_EDITOR)); }
    static Map<String, List<String>> tileLinks(SimulationEditorRepository repository) { return Map.of("family", List.of("floor", "wall", "road", "sidewalk", "door", "water", "void", "hazard", "fixture-underlay"), "roadRole", List.of("none", "lane", "sidewalk", "intersection", "maintenance throat", "transition approach"), "roomRole", List.of("none", "interior floor", "room shell", "corridor", "plaza", "service floor"), "boundaryRole", List.of("none", "solid", "outer bulkhead", "inner bulkhead", "void edge", "transition seal"), "lightProfile", List.of("none", "blocks", "street", "dim", "glow", "hazard warning"), "hazardProfile", List.of("none", "vehicle traffic", "sludge", "electric", "gas", "heat", "radiation"), "interactionHint", List.of("inspect", "inspect wall", "inspect road", "open", "search", "avoid")); }
    static Map<String, List<String>> objectLinks(SimulationEditorRepository repository) { return Map.of("objectType", List.of("workstation", "terminal", "utility", "furniture", "cover", "container", "street light"), "interaction", List.of("Inspect", "Hack", "Repair", "Loot", "Open", "Use"), "functionHook", List.of("inspect", "security-terminal", "water-service", "container-loot", "crafting-station", "light-switch", "street-light", "vehicle-staging"), "properties", List.of("durable,serviceable", "locked,powered,inspectable", "repairable,utility,pipe-sound", "container,lootable", "cover,passive-defense"), "lightingHook", List.of("none", "zone fixture", "street light", "portable light", "intrinsic glow"), "lightColor", List.of("warm", "green", "blue", "red", "sickly", "amber"), "infopediaEntry", names(repository, SimulationToolSuiteRegistry.INFOPEDIA_EDITOR)); }
    static Map<String, List<String>> entityLinks(SimulationEditorRepository repository) { return Map.of("archetype", List.of("civilian", "guard", "trader", "worker", "hostile", "servitor"), "faction", names(repository, SimulationToolSuiteRegistry.FACTION_EDITOR), "spawnZone", names(repository, SimulationToolSuiteRegistry.ZONE_EDITOR), "inventory", namesWithNone(repository, SimulationToolSuiteRegistry.ITEM_EDITOR)); }
    static Map<String, List<String>> knowledgeLinks(SimulationEditorRepository repository) { return Map.of("parent", namesWithRoot(repository, SimulationToolSuiteRegistry.KNOWLEDGE_EDITOR), "blueprint", List.of("none", "micro-assembler", "sealed hatch", "water recycler", "security terminal", "orbital trade relay")); }
    static Map<String, List<String>> infopediaLinks(SimulationEditorRepository repository) { return Map.of("category", List.of("World", "Factions", "Lore", "Items", "Knowledge", "Rooms", "Threats")); }
    static Map<String, List<String>> skillLinks(SimulationEditorRepository repository) { return Map.of("attribute", List.of("Technical", "Awareness", "Social", "Combat", "Endurance"), "knowledgePrerequisite", namesWithRoot(repository, SimulationToolSuiteRegistry.KNOWLEDGE_EDITOR), "unlocks", List.of("none", "diagnose machines", "surface zone hazards", "repair queue", "trade leverage", "combat stance")); }
    private static List<String> names(SimulationEditorRepository repository, String editor) {
        ArrayList<String> out = new ArrayList<>();
        for (SimulationEditorRepository.EditableEntity e : repository.entities(editor)) out.add(e.name());
        return out.isEmpty() ? List.of("None") : out;
    }
    private static List<String> namesWithNone(SimulationEditorRepository repository, String editor) {
        ArrayList<String> out = new ArrayList<>(); out.add("none"); out.addAll(names(repository, editor)); return out;
    }
    private static List<String> namesWithRoot(SimulationEditorRepository repository, String editor) {
        ArrayList<String> out = new ArrayList<>(); out.add("root"); out.addAll(names(repository, editor)); return out;
    }
    private static List<String> factionIds() {
        ArrayList<String> out = new ArrayList<>();
        out.add(Faction.NONE.name());
        for (Faction faction : Faction.visibleFactions()) out.add(faction.name());
        return List.copyOf(out);
    }
}
