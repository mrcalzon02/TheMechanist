package mechanist;

import mechanist.assets.AssetManager;

import java.io.File;
import javax.swing.JFileChooser;

final class GeneratedArtPayloadOptionsSubsystem {
    private GeneratedArtPayloadOptionsSubsystem() {
    }

    static void cycleArtQuality(GamePanel panel) {
        panel.options.artQualityIndex = Math.floorMod(panel.options.artQualityIndex + 1, GameOptions.ART_QUALITY_LABELS.length);
        panel.options.applyGeneratedAssetRuntimeProperties();
        panel.options.save();
        AssetManager.reloadGeneratedAssetRuntime();
        panel.images.reloadArtQuality(panel.options);
        panel.sounds.play("tab", panel.options);
        panel.logEvent("Texture package now " + panel.options.artQualityResolutionLabel() + ". Generated semantic assets request tier " + panel.options.artQualityFolder() + "; external payload " + panel.options.generatedAssetPayloadRootShortLabel() + ".");
        panel.repaint();
    }

    static void chooseGeneratedAssetPayloadRoot(GamePanel panel) {
        try {
            JFileChooser chooser = new JFileChooser(panel.options.hasGeneratedAssetPayloadRoot() ? new File(panel.options.generatedAssetPayloadRoot()) : new File("."));
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            chooser.setDialogTitle("Select The Mechanist generated-art payload root");
            int result = chooser.showOpenDialog(panel);
            if (result != JFileChooser.APPROVE_OPTION || chooser.getSelectedFile() == null) {
                panel.logEvent("Generated-art payload root unchanged.");
                return;
            }
            File selected = chooser.getSelectedFile();
            panel.options.generatedAssetPayloadRoot = selected.getAbsolutePath();
            panel.options.applyGeneratedAssetRuntimeProperties();
            panel.options.save();
            AssetManager.reloadGeneratedAssetRuntime();
            panel.images.reloadArtQuality(panel.options);
            panel.sounds.play("tab", panel.options);
            panel.logEvent("Generated-art payload root set to " + panel.options.generatedAssetPayloadRootLabel() + ". Requested tier: " + panel.options.artQualityFolder() + ".");
            panel.repaint();
        } catch (Throwable t) {
            panel.logEvent("Could not select generated-art payload root: " + t.getMessage());
        }
    }

    static void clearGeneratedAssetPayloadRoot(GamePanel panel) {
        panel.options.generatedAssetPayloadRoot = "";
        panel.options.applyGeneratedAssetRuntimeProperties();
        panel.options.save();
        AssetManager.reloadGeneratedAssetRuntime();
        panel.images.reloadArtQuality(panel.options);
        panel.sounds.play("tab", panel.options);
        panel.logEvent("External generated-art payload root cleared. Runtime will use bundled generated low_32 fallback unless a JVM flag supplies another root.");
        panel.repaint();
    }
}
