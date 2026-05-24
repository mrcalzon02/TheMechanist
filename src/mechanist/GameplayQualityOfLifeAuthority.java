package mechanist;

import java.util.*;

/**
 * Central owner for quality-of-life defaults that prevent common simulation,
 * single-entity base-builder, inventory, logistics, and UI friction.
 */
final class GameplayQualityOfLifeAuthority {
    private GameplayQualityOfLifeAuthority() {}

    static final String[] PROTECTION_LABELS = {"LOW", "BALANCED", "STRICT"};

    static String protectionLabel(int idx) {
        return PROTECTION_LABELS[Math.max(0, Math.min(PROTECTION_LABELS.length - 1, idx))];
    }

    static String toggleAutoLoot(GameOptions options) {
        if (options == null) return "Auto-loot unchanged: options unavailable.";
        options.autoLootEnabled = !options.autoLootEnabled;
        options.save();
        return "Auto-loot " + onOff(options.autoLootEnabled) + ".";
    }

    static String toggleSmartStorage(GameOptions options) {
        if (options == null) return "Smart storage unchanged: options unavailable.";
        options.smartStorageFilters = !options.smartStorageFilters;
        options.save();
        return "Smart storage filters " + onOff(options.smartStorageFilters) + ".";
    }

    static String toggleHoldRepeatBuild(GameOptions options) {
        if (options == null) return "Hold-to-repeat construction unchanged: options unavailable.";
        options.holdToRepeatConstruction = !options.holdToRepeatConstruction;
        options.save();
        return "Hold-to-repeat construction " + onOff(options.holdToRepeatConstruction) + ".";
    }

    static String toggleOmniGhostBuild(GameOptions options) {
        if (options == null) return "Omni-directional ghost build unchanged: options unavailable.";
        options.omniDirectionalGhostBuild = !options.omniDirectionalGhostBuild;
        options.save();
        return "Omni-directional ghost build " + onOff(options.omniDirectionalGhostBuild) + ".";
    }

    static String toggleProxyCrafting(GameOptions options) {
        if (options == null) return "Proxy crafting unchanged: options unavailable.";
        options.proxyCraftingFromLinkedStorage = !options.proxyCraftingFromLinkedStorage;
        options.save();
        return "Proxy crafting from linked storage " + onOff(options.proxyCraftingFromLinkedStorage) + ".";
    }

    static String toggleProductionWarnings(GameOptions options) {
        if (options == null) return "Production warnings unchanged: options unavailable.";
        options.productionBlockerWarnings = !options.productionBlockerWarnings;
        options.save();
        return "Production blocker warnings " + onOff(options.productionBlockerWarnings) + ".";
    }

    static String toggleMarketAlerts(GameOptions options) {
        if (options == null) return "Market alerts unchanged: options unavailable.";
        options.economicDisruptionAlerts = !options.economicDisruptionAlerts;
        options.save();
        return "Economic disruption alerts " + onOff(options.economicDisruptionAlerts) + ".";
    }

    static String toggleFavoredProtection(GameOptions options) {
        if (options == null) return "Favored-item protection unchanged: options unavailable.";
        options.favoredItemProtection = !options.favoredItemProtection;
        options.save();
        return "Favored-item protection " + onOff(options.favoredItemProtection) + ".";
    }

    static String cycleItemSafetyProfile(GameOptions options) {
        if (options == null) return "Item safety profile unchanged: options unavailable.";
        options.itemSafetyProfileIndex = (options.itemSafetyProfileIndex + 1) % PROTECTION_LABELS.length;
        options.save();
        return "Item safety profile set to " + protectionLabel(options.itemSafetyProfileIndex) + ".";
    }

    static String toggleSubtitles(GameOptions options) {
        if (options == null) return "Subtitles unchanged: options unavailable.";
        options.subtitlesEnabled = !options.subtitlesEnabled;
        options.save();
        return "Subtitles " + onOff(options.subtitlesEnabled) + ".";
    }

    static String toggleSkipSplashes(GameOptions options) {
        if (options == null) return "Intro skip preference unchanged: options unavailable.";
        options.skipRepeatLogoSplashes = !options.skipRepeatLogoSplashes;
        options.save();
        return "Repeat logo skipping " + onOff(options.skipRepeatLogoSplashes) + ".";
    }

    static String setDoomMode(GameOptions options, boolean enabled) {
        if (options == null) return "doom mode unchanged: options unavailable.";
        options.doomModeEnabled = enabled;
        options.save();
        return "doom mode " + onOff(options.doomModeEnabled) + ".";
    }

    static String setDoomFov(GameOptions options, int degrees) {
        if (options == null) return "doom mode FOV unchanged: options unavailable.";
        options.doomModeFovDegrees = Math.max(60, Math.min(110, degrees));
        options.save();
        return "doom mode FOV set to " + options.doomModeFovDegrees + " degrees.";
    }

    static String cycleDoomFogMode(GameOptions options) {
        if (options == null) return "doom mode fog unchanged: options unavailable.";
        options.doomFogModeIndex = Math.floorMod(options.doomFogModeIndex + 1, GameOptions.DOOM_FOG_MODE_LABELS.length);
        options.save();
        return "doom mode fog set to " + options.doomFogModeLabel() + ".";
    }

    static java.util.List<String> optionLines(GameOptions options) {
        if (options == null) return java.util.List.of("Quality-of-life defaults unavailable until options load.");
        java.util.ArrayList<String> lines = new java.util.ArrayList<>();
        lines.add("Quality-of-life defaults prevent common base-builder and survival-RPG friction before it hardens into habit.");
        lines.add("Subtitles: " + onOff(options.subtitlesEnabled) + "; repeat logo skip: " + onOff(options.skipRepeatLogoSplashes) + "; auto-loot: " + onOff(options.autoLootEnabled) + ".");
        lines.add("Construction: omni ghost build " + onOff(options.omniDirectionalGhostBuild) + "; hold-to-repeat " + onOff(options.holdToRepeatConstruction) + "; trap-prevention warnings " + onOff(options.constructionTrapWarnings) + ".");
        lines.add("Storage/logistics: smart filters " + onOff(options.smartStorageFilters) + "; proxy crafting " + onOff(options.proxyCraftingFromLinkedStorage) + "; linked output routing " + onOff(options.machineOutputAutoRouting) + ".");
        lines.add("Production intelligence: blocker warnings " + onOff(options.productionBlockerWarnings) + "; global scarcity alerts " + onOff(options.globalScarcityWarnings) + "; recipe pinning " + onOff(options.recipeHudPinning) + ".");
        lines.add("Item safety: favored protection " + onOff(options.favoredItemProtection) + "; low-quality pickup warnings " + onOff(options.lowQualityPickupWarnings) + "; no mixed-quality stacking " + onOff(options.noMixedQualityStacking) + "; profile " + protectionLabel(options.itemSafetyProfileIndex) + ".");
        lines.add("Faction safety: under-attack supply lock " + onOff(options.underAttackSupplyLock) + "; safe worker priorities " + onOff(options.safeWorkerPriorities) + "; named death alerts " + onOff(options.namedDeathAlerts) + ".");
        lines.add("Experimental rendering: doom mode " + onOff(options.doomModeEnabled) + "; FOV " + Math.max(60, Math.min(110, options.doomModeFovDegrees)) + " degrees; fog " + options.doomFogModeLabel() + "; LWJGL dependency " + LwjglRenderBackendProbe.statusLine() + ".");
        lines.add("Market intelligence: economic disruption alerts " + onOff(options.economicDisruptionAlerts) + "; local/global price hints " + onOff(options.localGlobalPriceHints) + ".");
        lines.addAll(BlueprintConstructionAuthority.optionLines(options));
        return lines;
    }

    static String auditSummary(GameOptions options) {
        if (options == null) return "qol unavailable";
        return "qol subtitles=" + options.subtitlesEnabled
                + " skipSplashes=" + options.skipRepeatLogoSplashes
                + " autoLoot=" + options.autoLootEnabled
                + " smartStorage=" + options.smartStorageFilters
                + " proxyCrafting=" + options.proxyCraftingFromLinkedStorage
                + " productionWarnings=" + options.productionBlockerWarnings
                + " itemSafety=" + protectionLabel(options.itemSafetyProfileIndex)
                + " marketAlerts=" + options.economicDisruptionAlerts
                + " doomMode=" + options.doomModeEnabled
                + " doomFov=" + options.doomModeFovDegrees
                + " doomFog=" + options.doomFogModeLabel()
                + " blueprintInvalidDiagnostics=" + options.invalidPlacementDiagnostics
                + " ghostRoomStamps=" + options.ghostRoomStamps
                + " preflight=" + options.blueprintPreflightChecklist;
    }

    private static String onOff(boolean value) { return value ? "ON" : "OFF"; }
}
