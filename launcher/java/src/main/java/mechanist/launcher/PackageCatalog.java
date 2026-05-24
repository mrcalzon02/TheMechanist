package mechanist.launcher;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

final class PackageCatalog {
    private PackageCatalog() {}

    static List<PackageTier> defaultGraphicsTiers(LauncherConfig config) {
        ArrayList<PackageTier> list = new ArrayList<>();
        list.add(new PackageTier(PackageTier.Kind.GRAPHICS, "low_32", "Core Low 32px",
                config.repoDir.resolve("assets/graphics/generated/low_32"), true, "", false,
                "Bundled fallback tier."));
        list.add(new PackageTier(PackageTier.Kind.GRAPHICS, "standard_64", "Standard 64px",
                config.repoDir.resolve("assets/graphics/generated/standard_64"), false, "low_32", true,
                "Recommended default for most installs."));
        list.add(new PackageTier(PackageTier.Kind.GRAPHICS, "intermediate_128", "Intermediate 128px",
                config.repoDir.resolve("assets/graphics/generated/intermediate_128"), false, "standard_64", false,
                "Higher quality package for stronger machines."));
        list.add(new PackageTier(PackageTier.Kind.GRAPHICS, "high_native", "Native / High",
                config.repoDir.resolve("assets/graphics/generated/high_native"), false, "intermediate_128", false,
                "Largest/highest source tier."));
        return list;
    }

    static List<PackageTier> defaultAudioTiers(LauncherConfig config) {
        ArrayList<PackageTier> list = new ArrayList<>();
        Path wav = config.repoDir.resolve("assets/music/wav");
        list.add(new PackageTier(PackageTier.Kind.AUDIO, "core_audio", "Core Audio / Main Menu", wav,
                true, "", true, "Minimal always-present music target."));
        list.add(new PackageTier(PackageTier.Kind.AUDIO, "half_music", "Half Music Package", wav,
                false, "core_audio", false, "One clean track per major zone/context."));
        list.add(new PackageTier(PackageTier.Kind.AUDIO, "full_dynamic_music", "Full Dynamic Music Package", wav,
                false, "half_music", false, "Full sector variants, alternates, and combat spread."));
        return list;
    }

    static PackageTier defaultTier(List<PackageTier> tiers) {
        for (PackageTier tier : tiers) if (tier.installerDefault) return tier;
        return tiers.isEmpty() ? null : tiers.get(0);
    }

    static PackageTier effectiveGraphicsTier(List<PackageTier> tiers, PackageTier requested) {
        PackageTier cursor = requested;
        while (cursor != null) {
            if (Files.isDirectory(cursor.runtimePath)) return cursor;
            cursor = byId(tiers, cursor.fallbackTier);
        }
        return byId(tiers, "low_32");
    }

    static PackageTier effectiveAudioTier(List<PackageTier> tiers, PackageTier requested) {
        PackageTier cursor = requested;
        while (cursor != null) {
            if (Files.isDirectory(cursor.runtimePath)) return cursor;
            cursor = byId(tiers, cursor.fallbackTier);
        }
        return byId(tiers, "core_audio");
    }

    static PackageTier byId(List<PackageTier> tiers, String id) {
        if (id == null || id.isBlank()) return null;
        for (PackageTier tier : tiers) if (id.equals(tier.id)) return tier;
        return null;
    }
}
