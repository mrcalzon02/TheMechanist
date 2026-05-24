package mechanist.launcher;

import java.nio.file.Path;

final class PackageTier {
    enum Kind { GRAPHICS, AUDIO }

    final Kind kind;
    final String id;
    final String label;
    final Path runtimePath;
    final boolean required;
    final String fallbackTier;
    final boolean installerDefault;
    final String notes;

    PackageTier(Kind kind, String id, String label, Path runtimePath, boolean required,
                String fallbackTier, boolean installerDefault, String notes) {
        this.kind = kind;
        this.id = id;
        this.label = label;
        this.runtimePath = runtimePath;
        this.required = required;
        this.fallbackTier = fallbackTier == null ? "" : fallbackTier;
        this.installerDefault = installerDefault;
        this.notes = notes == null ? "" : notes;
    }

    @Override public String toString() {
        return label + " [" + id + "]";
    }
}
