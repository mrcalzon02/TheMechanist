package mechanist;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeListener;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Three-pane character knowledge/progression menu.  The Infopedia may still
 * describe doctrine, but purchases belong here so the character sheet owns the
 * progression workflow.
 */
public final class KnowledgeMenu extends JDialog {
    interface KnowledgeStateBridge {
        int availableKnowledgePoints();
        Set<String> unlockedKnowledgeIds();
        boolean unlockKnowledgeNode(KnowledgeTree tree, KnowledgeNode node);
        default void menuClosed() {}
    }

    private final KnowledgeStateBridge bridge;
    private LinkedHashMap<String, KnowledgeTree> branches;
    private KnowledgeTree activeTree;
    private final JTabbedPane branchTabs = new JTabbedPane();
    private final TreeCanvasPanel canvas;
    private final JLabel pointsLabel = new JLabel();
    private final JLabel branchLabel = new JLabel();
    private final JTextArea earnText = new JTextArea();
    private final JLabel nodeName = new JLabel("Select a node");
    private final JLabel nodeCost = new JLabel("Cost: --");
    private final JLabel nodeState = new JLabel("State: --");
    private final JTextArea nodeDescription = new JTextArea();
    private final JButton unlockButton = new JButton("Unlock Node");
    private KnowledgeNode selectedNode;

    @Deprecated
    public static KnowledgeMenu openFor(GamePanel panel) {
        if (panel != null) panel.openKnowledgeMenu();
        return null;
    }

    public KnowledgeMenu(Window owner, KnowledgeStateBridge bridge) {
        super(owner, "Character Knowledge", ModalityType.MODELESS);
        this.bridge = bridge;
        this.branches = createBranches(bridge.availableKnowledgePoints(), bridge.unlockedKnowledgeIds());
        this.activeTree = branches.values().stream().findFirst().orElseGet(() -> new KnowledgeTree("empty", "Knowledge", 0));
        this.canvas = new TreeCanvasPanel(activeTree);
        this.canvas.setSelectionListener(this::selectNode);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setMinimumSize(new Dimension(1050, 690));
        setPreferredSize(new Dimension(1180, 760));
        setLayout(new BorderLayout(8, 8));
        getRootPane().setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        buildTabs();
        add(branchTabs, BorderLayout.NORTH);
        add(buildLeftPanel(), BorderLayout.WEST);
        add(canvas, BorderLayout.CENTER);
        add(buildRightPanel(), BorderLayout.EAST);
        unlockButton.addActionListener(e -> attemptUnlockSelectedNode());
        pack();
        refreshPanels();
    }

    public void refreshFromGameState() {
        String activeId = activeTree == null ? null : activeTree.id();
        branches = createBranches(bridge.availableKnowledgePoints(), bridge.unlockedKnowledgeIds());
        activeTree = activeId == null ? firstTree() : branches.getOrDefault(activeId, firstTree());
        rebuildTabs(activeTree == null ? 0 : new ArrayList<>(branches.values()).indexOf(activeTree));
        canvas.setKnowledgeTree(activeTree);
        selectedNode = null;
        refreshPanels();
    }

    @Override public void dispose() {
        bridge.menuClosed();
        super.dispose();
    }

    private JComponent buildLeftPanel() {
        JPanel panel = new JPanel(new BorderLayout(6, 8));
        panel.setPreferredSize(new Dimension(275, 560));
        panel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(95, 78, 42)), BorderFactory.createEmptyBorder(12, 12, 12, 12)));
        JPanel top = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 1.0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(0, 0, 8, 0);
        pointsLabel.setFont(pointsLabel.getFont().deriveFont(Font.BOLD, 18f));
        top.add(pointsLabel, c);
        c.gridy++;
        branchLabel.setFont(branchLabel.getFont().deriveFont(Font.BOLD, 14f));
        top.add(branchLabel, c);
        panel.add(top, BorderLayout.NORTH);
        earnText.setEditable(false);
        earnText.setLineWrap(true);
        earnText.setWrapStyleWord(true);
        earnText.setOpaque(false);
        earnText.setText("How to earn more points:\n\nGain XP through survival, work, combat, research, contracts, studying data slates, operating labs, and faction services. Each 25 XP band grants a knowledge credit. Later systems can add branch-specific experience and perk payloads without moving purchases back into the Infopedia.");
        panel.add(new JScrollPane(earnText), BorderLayout.CENTER);
        JButton resetView = new JButton("Center Tree");
        resetView.addActionListener(e -> canvas.resetView());
        panel.add(resetView, BorderLayout.SOUTH);
        return panel;
    }

    private JComponent buildRightPanel() {
        JPanel panel = new JPanel(new BorderLayout(6, 8));
        panel.setPreferredSize(new Dimension(315, 560));
        panel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(95, 78, 42)), BorderFactory.createEmptyBorder(12, 12, 12, 12)));
        JPanel header = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 1.0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(0, 0, 8, 0);
        nodeName.setFont(nodeName.getFont().deriveFont(Font.BOLD, 18f));
        header.add(nodeName, c);
        c.gridy++;
        header.add(nodeCost, c);
        c.gridy++;
        header.add(nodeState, c);
        panel.add(header, BorderLayout.NORTH);
        nodeDescription.setEditable(false);
        nodeDescription.setLineWrap(true);
        nodeDescription.setWrapStyleWord(true);
        panel.add(new JScrollPane(nodeDescription), BorderLayout.CENTER);
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        unlockButton.setFont(unlockButton.getFont().deriveFont(Font.BOLD));
        bottom.add(unlockButton);
        panel.add(bottom, BorderLayout.SOUTH);
        return panel;
    }

    private void buildTabs() { rebuildTabs(0); }

    private void rebuildTabs(int selectedIndex) {
        for (ChangeListener listener : branchTabs.getChangeListeners()) branchTabs.removeChangeListener(listener);
        branchTabs.removeAll();
        int index = 0;
        for (KnowledgeTree tree : branches.values()) {
            final KnowledgeTree branch = tree;
            JPanel placeholder = new JPanel();
            branchTabs.addTab(tree.displayName(), placeholder);
            int tabIndex = index;
            branchTabs.setToolTipTextAt(tabIndex, tree.unlockedCount() + " known, " + tree.availableCount() + " available");
            index++;
        }
        if (branchTabs.getTabCount() > 0) branchTabs.setSelectedIndex(Math.max(0, Math.min(selectedIndex, branchTabs.getTabCount() - 1)));
        branchTabs.addChangeListener(e -> {
            int idx = branchTabs.getSelectedIndex();
            if (idx < 0 || idx >= branches.size()) return;
            KnowledgeTree next = new ArrayList<>(branches.values()).get(idx);
            if (next != activeTree) {
                activeTree = next;
                canvas.setKnowledgeTree(activeTree);
                selectedNode = null;
                refreshPanels();
            }
        });
    }

    private KnowledgeTree firstTree() {
        return branches.values().stream().findFirst().orElseGet(() -> new KnowledgeTree("empty", "Knowledge", bridge.availableKnowledgePoints()));
    }

    private void selectNode(KnowledgeNode node) {
        selectedNode = node;
        refreshPanels();
    }

    private void attemptUnlockSelectedNode() {
        if (selectedNode == null || activeTree == null) return;
        if (!activeTree.canUnlock(selectedNode.id())) {
            KnowledgeTree.UnlockResult result = activeTree.unlockNode(selectedNode.id());
            nodeState.setText("State: " + result.message());
            refreshPanels();
            return;
        }
        boolean committed = bridge.unlockKnowledgeNode(activeTree, selectedNode);
        if (committed) {
            String keepId = selectedNode.id();
            branches = createBranches(bridge.availableKnowledgePoints(), bridge.unlockedKnowledgeIds());
            activeTree = branches.getOrDefault(activeTree.id(), firstTree());
            canvas.setKnowledgeTree(activeTree);
            selectedNode = activeTree.node(keepId);
            canvas.setSelectedNode(selectedNode);
            rebuildTabs(new ArrayList<>(branches.values()).indexOf(activeTree));
        }
        refreshPanels();
    }

    private void refreshPanels() {
        if (activeTree == null) return;
        pointsLabel.setText("Knowledge Credits: " + bridge.availableKnowledgePoints());
        branchLabel.setText(activeTree.displayName() + " — " + activeTree.unlockedCount() + "/" + activeTree.nodes().size() + " known");
        KnowledgeNode node = selectedNode;
        if (node == null) {
            nodeName.setText("Select a node");
            nodeCost.setText("Cost: --");
            nodeState.setText("State: --");
            nodeDescription.setText("Click a knowledge node in the center tree. Yellow nodes can be purchased now, green nodes are already known, and gray nodes need prerequisite doctrine or more credits.");
            unlockButton.setEnabled(false);
        } else {
            KnowledgeTree.NodeState state = activeTree.stateOf(node);
            nodeName.setText(node.name());
            nodeCost.setText("Cost: " + node.costLabel());
            nodeState.setText("State: " + stateLabel(state));
            nodeDescription.setText(node.longDescription().isBlank() ? node.shortDescription() : node.longDescription());
            nodeDescription.setCaretPosition(0);
            unlockButton.setEnabled(state == KnowledgeTree.NodeState.AVAILABLE);
        }
        canvas.repaint();
    }

    private static String stateLabel(KnowledgeTree.NodeState state) {
        return switch (state) {
            case UNLOCKED -> "Known / unlocked";
            case AVAILABLE -> "Available to unlock";
            case LOCKED -> "Locked by prerequisites or credits";
        };
    }

    static LinkedHashMap<String, KnowledgeTree> createBranches(int points, Set<String> unlocked) {
        return KnowledgeBranchDefinitions.createBranches(points, unlocked);
    }


    public static List<KnowledgeTree> demoBranches() {
        return KnowledgeBranchDefinitions.developerPreviewBranches();
    }

}
