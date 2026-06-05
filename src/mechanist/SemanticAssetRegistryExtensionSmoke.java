package mechanist;

import mechanist.assets.AssetRegistry;
import mechanist.assets.AssetType;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Smoke for Milestone 01 Stage 10 controlled art-pack registry extension. */
final class SemanticAssetRegistryExtensionSmoke {
    public static void main(String[] args) throws Exception {
        Path root = Files.createTempDirectory("mechanist-asset-extension-smoke");
        Path indexes = root.resolve("assets/indexes");
        Path artpack = root.resolve("PACKAGE_client/assets/artpacks/test-pack");
        Files.createDirectories(indexes);
        Files.createDirectories(artpack.resolve("assets"));
        Files.writeString(artpack.resolve("assets/example.png"), "not-an-image-but-path-owned", StandardCharsets.UTF_8);

        Files.writeString(indexes.resolve("semantic_asset_registry.tsv"), """
                id	type	path	name	description
                BASE-A01	OBJECT	assets/base.png	Base Object	Base registry object.
                """, StandardCharsets.UTF_8);
        Files.writeString(artpack.resolve(AssetRegistry.EXTENSION_REGISTRY), """
                id	type	path	name	description
                MODA-A01	UI_ICON	assets/example.png	Example Pack Icon	Controlled art-pack semantic entry.
                """, StandardCharsets.UTF_8);

        AssetRegistry registry = AssetRegistry.loadDefault(root);
        if (registry.find("BASE-A01").isEmpty()) throw new AssertionError("Base registry entry did not load.");
        if (registry.find("MODA-A01").isEmpty()) throw new AssertionError("Art-pack extension registry entry did not load.");
        if (registry.require("MODA-A01").type() != AssetType.UI_ICON) throw new AssertionError("Extension type was not preserved.");

        Files.writeString(artpack.resolve(AssetRegistry.EXTENSION_REGISTRY), """
                id	type	path	name	description
                BASE-A01	UI_ICON	assets/example.png	Duplicate Entry	Duplicate ID should fail.
                """, StandardCharsets.UTF_8);
        try {
            AssetRegistry.loadDefault(root);
            throw new AssertionError("Duplicate extension ID was accepted.");
        } catch (java.io.IOException expected) {
            if (!expected.getMessage().contains("Duplicate semantic asset ID BASE-A01")) throw expected;
        }

        Files.writeString(artpack.resolve(AssetRegistry.EXTENSION_REGISTRY), """
                id	type	path	name	description
                MODB-A01	UI_ICON	../escape.png	Escaping Entry	Escaping paths should fail.
                """, StandardCharsets.UTF_8);
        try {
            AssetRegistry.loadDefault(root);
            throw new AssertionError("Escaping extension path was accepted.");
        } catch (java.io.IOException expected) {
            if (!expected.getMessage().contains("escapes package root")) throw expected;
        }

        System.out.println("Semantic asset registry extension smoke passed.");
    }

    private SemanticAssetRegistryExtensionSmoke() { }
}
