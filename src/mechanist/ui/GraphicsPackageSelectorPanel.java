package mechanist.ui;

import mechanist.assets.GraphicsPackage;
import mechanist.assets.GraphicsPackageRegistry;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Swing selector for installed graphics packages.
 *
 * This panel deliberately selects real installed packages from the package folder:
 * unzipped ready-to-use folders or compressed package zip bundles. The default 32px
 * package is expected to be unzipped and ready to use.
 */
public final class GraphicsPackageSelectorPanel extends JPanel {
    private final JComboBox<GraphicsPackage> packageCombo = new JComboBox<>();
    private final JLabel detailLabel = new JLabel(" ");
    private Consumer<GraphicsPackage> selectionConsumer = pack -> {};

    public GraphicsPackageSelectorPanel() {
        super(new BorderLayout(8, 6));
        buildUi();
        refreshPackages(GraphicsPackageRegistry.DEFAULT_PACKAGE_ID);
    }

    public void setSelectionConsumer(Consumer<GraphicsPackage> selectionConsumer) {
        this.selectionConsumer = Objects.requireNonNull(selectionConsumer, "selectionConsumer");
    }

    public void refreshPackages(String selectedId) {
        var packages = GraphicsPackageRegistry.discoverInstalledPackages();
        packageCombo.removeAllItems();
        GraphicsPackage selected = null;
        for (var pack : packages) {
            packageCombo.addItem(pack);
            if (pack.id().equals(selectedId)) {
                selected = pack;
            }
        }
        if (selected == null && packageCombo.getItemCount() > 0) {
            selected = packageCombo.getItemAt(0);
        }
        if (selected != null) {
            packageCombo.setSelectedItem(selected);
            updateDetail(selected);
        }
    }

    public GraphicsPackage selectedPackage() {
        return (GraphicsPackage) packageCombo.getSelectedItem();
    }

    private void buildUi() {
        var label = new JLabel("Graphics package:");
        add(label, BorderLayout.WEST);
        add(packageCombo, BorderLayout.CENTER);
        add(detailLabel, BorderLayout.SOUTH);
        packageCombo.addActionListener(e -> {
            var selected = selectedPackage();
            if (selected != null) {
                updateDetail(selected);
                selectionConsumer.accept(selected);
            }
        });
    }

    private void updateDetail(GraphicsPackage selected) {
        var mode = selected.compressed() ? "compressed bundle; install/extract before direct use" : "ready-to-use folder";
        detailLabel.setText("Selected: " + selected.id() + " — " + selected.nominalSizePx() + "px — " + mode + " — " + selected.path());
    }
}
