package mechanist.assets;

import java.nio.file.Path;
import java.util.Objects;

/**
 * Client-facing graphics package descriptor.
 *
 * A package is either an unzipped ready-to-use folder or a compressed bundle zip in
 * PACKAGE_client/assets/graphics/packages. The default 32px package is expected to be
 * unzipped and ready to use.
 */
public record GraphicsPackage(
        String id,
        String displayName,
        int nominalSizePx,
        Path path,
        boolean compressed,
        boolean readyToUse
) {
    public GraphicsPackage {
        id = Objects.requireNonNull(id, "id").trim();
        displayName = Objects.requireNonNull(displayName, "displayName").trim();
        Objects.requireNonNull(path, "path");
        if (id.isEmpty()) {
            throw new IllegalArgumentException("Graphics package id cannot be blank.");
        }
        if (displayName.isEmpty()) {
            displayName = id;
        }
    }

    @Override
    public String toString() {
        var state = compressed ? "bundle" : "folder";
        return displayName + " (" + nominalSizePx + "px, " + state + ")";
    }
}
