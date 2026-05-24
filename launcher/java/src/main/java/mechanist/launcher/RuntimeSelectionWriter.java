package mechanist.launcher;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

final class RuntimeSelectionWriter {
    private RuntimeSelectionWriter() {}

    static void writeSelections(LauncherConfig config, PackageTier graphicsTier, PackageTier audioTier) throws IOException {
        Files.createDirectories(config.repoDir.resolve("settings"));
        Path options = config.repoDir.resolve("settings/options.properties");
        Properties props = new Properties();
        if (Files.isRegularFile(options)) {
            try (InputStream in = Files.newInputStream(options)) {
                props.load(in);
            }
        }

        if (graphicsTier != null) {
            props.setProperty("mechanist.graphicsTier", graphicsTier.id);
            props.setProperty("mechanist.assetTier", graphicsTier.id);
            props.setProperty("generatedAssetPayloadRoot", config.repoDir.toString());
            props.setProperty("mechanist.generatedAssetRoot", config.repoDir.toString());
            props.setProperty("mechanist.assetPayloadRoot", config.repoDir.toString());
        }

        if (audioTier != null) {
            props.setProperty("mechanist.audioTier", audioTier.id);
            props.setProperty("mechanist.musicRoot", config.repoDir.resolve("assets/music/wav").toString());
            props.setProperty("mechanist.musicManifest", config.repoDir.resolve("assets/music/music_manifest.tsv").toString());
        }

        props.setProperty("mechanist.installRoot", config.installRoot.toString());
        props.setProperty("mechanist.gameRoot", config.gameRoot.toString());
        props.setProperty("mechanist.saveRoot", config.saveDir.toString());
        props.setProperty("mechanist.settingsRoot", config.settingsDir.toString());
        props.setProperty("mechanist.profileRoot", config.profilesDir.toString());
        props.setProperty("mechanist.logRoot", config.logsDir.toString());
        props.setProperty("mechanist.cacheRoot", config.cacheDir.toString());

        try (OutputStream out = Files.newOutputStream(options)) {
            props.store(out, "The Mechanist launcher-selected runtime paths and package tiers");
        }
    }
}
