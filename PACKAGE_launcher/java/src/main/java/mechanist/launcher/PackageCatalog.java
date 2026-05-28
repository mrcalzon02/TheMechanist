package mechanist.launcher;

import java.util.ArrayList;
import java.util.List;

final class PackageCatalog {
    private PackageCatalog() {}

    static List<PackageTier> defaultGraphicsTiers(LauncherConfig config) {
        ArrayList<PackageTier> list = new ArrayList<>();
        list.add(new PackageTier(PackageTier.Kind.GRAPHICS, "low_32", "Core Low 32px",
                null, true, "", false,
                "Bundled fallback tier."));
        list.add(new PackageTier(PackageTier.Kind.GRAPHICS, "standard_64", "Standard 64px",
                null, false, "low_32", true,
                "Recommended default for most installs."));
        list.add(new PackageTier(PackageTier.Kind.GRAPHICS, "intermediate_128", "Intermediate 128px",
                null, false, "standard_64", false,
                "Higher quality package for stronger machines."));
        list.add(new PackageTier(PackageTier.Kind.GRAPHICS, "high_native", "Native / High",
                null, false, "intermediate_128", false,
                "Largest/highest source tier."));
        return list;
    }

    static List<PackageTier> defaultAudioTiers(LauncherConfig config) {
        ArrayList<PackageTier> list = new ArrayList<>();
        list.add(new PackageTier(PackageTier.Kind.AUDIO, "core_audio", "Core Audio / Main Menu", null,
                true, "", true, "Minimal always-present music target."));
        list.add(new PackageTier(PackageTier.Kind.AUDIO, "half_music", "Half Music Package", null,
                false, "core_audio", false, "One clean track per major zone/context."));
        list.add(new PackageTier(PackageTier.Kind.AUDIO, "full_dynamic_music", "Full Dynamic Music Package", null,
                false, "half_music", false, "Full sector variants, alternates, and combat spread."));
        return list;
    }

    static PackageTier defaultTier(List<PackageTier> tiers) {
        for (PackageTier tier : tiers) if (tier.installerDefault) return tier;
        return tiers.isEmpty() ? null : tiers.get(0);
    }

    static PackageTier effectiveGraphicsTier(List<PackageTier> tiers, PackageTier requested) {
        return requested == null ? defaultTier(tiers) : requested;
    }

    static PackageTier effectiveAudioTier(List<PackageTier> tiers, PackageTier requested) {
        return requested == null ? defaultTier(tiers) : requested;
    }

    static PackageTier byId(List<PackageTier> tiers, String id) {
        if (id == null || id.isBlank()) return null;
        for (PackageTier tier : tiers) if (id.equals(tier.id)) return tier;
        return null;
    }
}
