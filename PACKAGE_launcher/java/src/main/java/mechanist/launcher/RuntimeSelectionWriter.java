package mechanist.launcher;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Properties;

final class RuntimeSelectionWriter {
    private RuntimeSelectionWriter() {}

    static void writeSelections(LauncherConfig config, PackageTier graphicsTier, PackageTier audioTier) throws IOException {
        Files.createDirectories(config.clientSettingsDir);
        Path options = config.clientSettingsDir.resolve("options.properties");
        Properties props = new Properties();
        Path source = options;
        if (!Files.isRegularFile(source)) {
            for (Path legacy : java.util.List.of(config.clientPackageDir.resolve("settings/options.properties"),
                    config.installRoot.resolve("settings/options.properties"), config.settingsDir.resolve("options.properties"))) {
                if (Files.isRegularFile(legacy)) { source = legacy; break; }
            }
        }
        if (Files.isRegularFile(source)) {
            try (InputStream in = Files.newInputStream(source)) {
                props.load(in);
            }
        }

        if (graphicsTier != null) {
            props.setProperty("mechanist.graphicsTier", graphicsTier.id);
            props.setProperty("mechanist.assetTier", graphicsTier.id);
            props.setProperty("artQualityIndex", switch (graphicsTier.id) {
                case "standard_64" -> "1";
                case "intermediate_128" -> "2";
                case "high_native" -> "3";
                default -> "0";
            });
            Path clientAssetRoot = config.clientPackageDir.resolve("assets");
            props.setProperty("generatedAssetPayloadRoot", clientAssetRoot.toString());
            props.setProperty("mechanist.generatedAssetRoot", clientAssetRoot.toString());
            props.setProperty("mechanist.assetPayloadRoot", clientAssetRoot.toString());
        }

        if (audioTier != null) {
            props.setProperty("mechanist.audioTier", audioTier.id);
            Path clientAssetRoot = config.clientPackageDir.resolve("assets");
            props.setProperty("mechanist.musicRoot", clientAssetRoot.resolve("music/wav").toString());
            props.setProperty("mechanist.musicManifest", clientAssetRoot.resolve("music/music_manifest.tsv").toString());
        }

        props.setProperty("mechanist.installRoot", config.installRoot.toString());
        props.setProperty("mechanist.gameRoot", config.gameRoot.toString());
        props.setProperty("mechanist.packageRoot", config.packageRoot.toString());
        props.setProperty("mechanist.saveRoot", config.saveDir.toString());
        props.setProperty("mechanist.settingsRoot", config.clientSettingsDir.toString());
        props.setProperty("mechanist.profileRoot", config.profilesDir.toString());
        props.setProperty("mechanist.logRoot", config.logsDir.toString());
        props.setProperty("mechanist.cacheRoot", config.cacheDir.toString());

        Path temporary = Files.createTempFile(config.clientSettingsDir, "launcher-options-", ".tmp");
        try {
            try (OutputStream out = Files.newOutputStream(temporary)) {
                props.store(out, "The Mechanist launcher-selected runtime paths and package tiers");
            }
            try {
                Files.move(temporary, options, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (java.nio.file.AtomicMoveNotSupportedException unsupported) {
                Files.move(temporary, options, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally { Files.deleteIfExists(temporary); }
    }
}
