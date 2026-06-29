package mechanist;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import javax.sound.sampled.*;
import java.io.*;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;

class GameOptions {
    static final int TEXT_TITLE = 0;
    static final int TEXT_MAIN = 1;
    static final int TEXT_DIM = 2;
    static final int TEXT_HIGHLIGHT = 3;
    static final int BACKGROUND = 4;
    static final String[] COLOR_KEYS = {"Title Text", "Main Text", "Dim Text", "Highlight Text", "Panel Background"};
    static final String[] PALETTE_NAMES = {"Mechanist Amber", "High Contrast", "Terminal Green", "Cold Auspex", "Protan Ember", "Deutan Steel", "Tritan Brass", "Legibility Slate"};
    static final int[][] PALETTES = {
        {0xC8B884, 0xDCCD91, 0xA09470, 0xEBD078, 0x08080A},
        {0xFFFFFF, 0xF4F4F4, 0xC8C8C8, 0xFFFF66, 0x000000},
        {0xCFFFCF, 0xB7F0B7, 0x7FBF7F, 0xF0E68C, 0x001100},
        {0xC8E6FF, 0xB8D8FF, 0x8AA8C8, 0xFFD37A, 0x020712},
        {0xFFE7A3, 0xF5D77A, 0xB9A45E, 0x67D7FF, 0x090704},
        {0xE7F2FF, 0xD4E6F6, 0x9CB8D0, 0xFFB45E, 0x05080B},
        {0xFFF0BC, 0xF4E2A0, 0xBDAE78, 0x4DE6D1, 0x080706},
        {0xFAFAFA, 0xECECEC, 0xB8C4D8, 0xFFCF4A, 0x050505}
    };
    static final int[][] RESOLUTIONS = {
        {800,600}, {1024,768}, {1152,864}, {1280,720}, {1280,800}, {1366,768}, {1440,900}, {1600,900}, {1680,1050}, {1920,1080}, {1920,1200}, {2560,1080}, {2560,1440}, {3440,1440}
    };
    int fontScale = 75; // compact crisp default; users can scale up from Options > Graphics/Display
    int uiScale = 90; // compact default so command panels do not start overgrown
    boolean soundEnabled = true;
    boolean musicEnabled = true;
    boolean conversationSound = true;
    boolean bootSound = false;
    boolean importedPortraits = true;
    boolean tileIconRendering = true;
    int artQualityIndex = 0; // 0 low_32 bundled, 1 standard_64 pack, 2 intermediate_128 pack, 3 high_native pack
    String generatedAssetPayloadRoot = System.getProperty("mechanist.generatedAssetRoot", System.getProperty("mechanist.assetPayloadRoot", "")); // optional external generated-art payload root(s)
    int mapTileSizeIndex = 1; // compact/normal/intermediate/high on-screen footprint
    int worldZoomIndex = 2; // tactical viewport zoom; 2 is neutral/100 percent
    boolean hoverHelp = true;
    boolean screenSaver = true;
    int sfxVolume = 80;
    int musicVolume = 80;
    int conversationVolume = 80;
    int volume = 80; // legacy alias retained for existing sound-play calls
    int windowMode = 1; // 0 windowed, 1 borderless fullscreen-sized window, 2 exclusive fullscreen
    int resolutionIndex = 5;
    boolean resolutionUserSelected = false;
    int downscaleIndex = 0; // 0 native, 1 75%, 2 50%, 3 emergency 33%
    int targetFpsIndex = 0; // 30 / 60 / 120 / uncapped; Swing timer is best-effort frame pacing, not simulation authority.
    boolean isFrameLimited = true;
    int renderQualityIndex = 2; // performance / balanced / crisp text; default to crisp, not enlarged.
    boolean reducedMotion = false;
    boolean diagnosticsOverlay = false;
    int lightingFxIndex = 2; // 0 off, 1 static render-only lightmap, 2 deterministic flicker plus low-res bloom.
    int cvdModeIndex = 0; // 0 normal, 1 protanopia, 2 deuteranopia, 3 tritanopia render-side correction.
    boolean highContrastText = false;
    boolean instantDialogueText = false;
    int screenShakePercent = 100;
    boolean subtitlesEnabled = true;
    boolean skipRepeatLogoSplashes = true;
    boolean autoLootEnabled = false;
    boolean omniDirectionalGhostBuild = true;
    boolean holdToRepeatConstruction = true;
    boolean constructionTrapWarnings = true;
    boolean smartStorageFilters = true;
    boolean proxyCraftingFromLinkedStorage = true;
    boolean machineOutputAutoRouting = true;
    boolean productionBlockerWarnings = true;
    boolean globalScarcityWarnings = true;
    boolean recipeHudPinning = true;
    boolean favoredItemProtection = true;
    boolean lowQualityPickupWarnings = true;
    boolean noMixedQualityStacking = true;
    int itemSafetyProfileIndex = 1;
    boolean underAttackSupplyLock = true;
    boolean safeWorkerPriorities = true;
    boolean namedDeathAlerts = true;
    boolean economicDisruptionAlerts = true;
    boolean localGlobalPriceHints = true;
    boolean invalidPlacementDiagnostics = true;
    boolean ghostRoomStamps = true;
    boolean blueprintPreflightChecklist = true;
    boolean hollowBoxTool = true;
    boolean anchorPointSnapping = true;
    boolean resourceEstimateTooltips = true;
    boolean standaloneBlueprintSandbox = true;
    boolean blueprintCaptureTool = true;
    boolean materialSubstitutionPrompts = true;
    int singlePlayerTickModeIndex = 0; // 0 turn based, 1 passive constant ticking.
    boolean doomModeEnabled = false;
    int doomModeFovDegrees = 80;
    int doomFogModeIndex = 0; // 0 linear Z-depth, 1 radial Euclidean distance.
    int colorPreset = 0;
    int colorTarget = 1;
    int[] colors = Arrays.copyOf(PALETTES[0], PALETTES[0].length);


    static final String[] ART_QUALITY_LABELS = {"32px textures", "64px textures", "128px textures", "Native textures"};
    static final String[] ART_QUALITY_FOLDERS = {"low_32", "standard_64", "intermediate_128", "high_native"};
    static final int[] ART_QUALITY_RESOLUTIONS = {32, 64, 128, 256};
    static final String[] MAP_TILE_SIZE_LABELS = {"Compact", "Normal", "Large", "Huge"};
    static final int[] MAP_TILE_PIXEL_SIZES = {24, 28, 40, 64};
    static final String[] WORLD_ZOOM_LABELS = {"FAR 70%", "WIDE 85%", "NORMAL 100%", "CLOSE 125%", "INSPECT 150%", "MAX 200%"};
    static final int[] WORLD_ZOOM_PERCENTS = {70, 85, 100, 125, 150, 200};
    static final String[] DOWNSCALE_LABELS = {"NATIVE 100%", "BALANCED 75%", "PERFORMANCE 50%", "EMERGENCY 33%"};
    static final float[] DOWNSCALE_FACTORS = {1.00f, 0.75f, 0.50f, 0.33f};
    static final String[] TARGET_FPS_LABELS = {"30 FPS", "60 FPS", "120 FPS", "UNCAPPED"};
    static final int[] TARGET_FPS_VALUES = {30, 60, 120, 0};
    static final String[] RENDER_QUALITY_LABELS = {"PERFORMANCE", "BALANCED", "CRISP TEXT"};
    static final String[] LIGHTING_FX_LABELS = {"OFF", "STATIC LIGHTMAP", "FLICKER + BLOOM"};
    static final String[] SINGLE_PLAYER_TICK_MODE_LABELS = {"TURN BASED", "PASSIVE TICKING"};
    static final String[] DOOM_FOG_MODE_LABELS = {"LINEAR", "RADIAL"};
    static final int COMPACT_DEFAULT_FONT_SCALE = 75;
    static final int COMPACT_DEFAULT_UI_SCALE = 90;
    static final String TEXT_DENSITY_POLICY_VERSION = "compact-text-0.9.10it";
    static final String DISPLAY_RESOLUTION_POLICY_VERSION = "highest-detected-on-launch-0.9.10jn";


    String artQualityLabel() { return ART_QUALITY_LABELS[Math.max(0, Math.min(ART_QUALITY_LABELS.length-1, artQualityIndex))]; }
    String artQualityFolder() { return ART_QUALITY_FOLDERS[Math.max(0, Math.min(ART_QUALITY_FOLDERS.length-1, artQualityIndex))]; }
    int artQualityResolution() { return ART_QUALITY_RESOLUTIONS[Math.max(0, Math.min(ART_QUALITY_RESOLUTIONS.length-1, artQualityIndex))]; }
    String artQualityResolutionLabel() { return artQualityResolution() + "px"; }
    String generatedAssetPayloadRoot() { return generatedAssetPayloadRoot == null ? "" : generatedAssetPayloadRoot.trim(); }
    boolean hasGeneratedAssetPayloadRoot() { return !generatedAssetPayloadRoot().isEmpty(); }
    String generatedAssetPayloadRootLabel() {
        String root = generatedAssetPayloadRoot();
        if (root.isEmpty()) return "Bundled low_32 fallback only";
        return root;
    }
    String generatedAssetPayloadRootShortLabel() {
        String root = generatedAssetPayloadRoot();
        if (root.isEmpty()) return "BUNDLED";
        String normalized = root.replace('\\', '/');
        int semi = normalized.indexOf(';');
        if (semi >= 0) normalized = normalized.substring(0, semi) + ";...";
        int slash = normalized.lastIndexOf('/');
        String tail = slash >= 0 ? normalized.substring(slash + 1) : normalized;
        if (tail.isBlank()) tail = normalized;
        return tail.length() > 18 ? tail.substring(0, 15) + "..." : tail;
    }
    void applyGeneratedAssetRuntimeProperties() {
        System.setProperty("mechanist.assetTier", artQualityFolder());
        System.setProperty("mechanist.graphicsTier", artQualityFolder());
        System.setProperty("mechanist.assetResolution", Integer.toString(artQualityResolution()));
        String root = generatedAssetPayloadRoot();
        if (root.isEmpty()) {
            System.clearProperty("mechanist.generatedAssetRoot");
            System.clearProperty("mechanist.assetPayloadRoot");
        } else {
            System.setProperty("mechanist.generatedAssetRoot", root);
            System.setProperty("mechanist.assetPayloadRoot", root);
        }
    }
    String mapTileSizeLabel() { return MAP_TILE_SIZE_LABELS[Math.max(0, Math.min(MAP_TILE_SIZE_LABELS.length-1, mapTileSizeIndex))]; }
    int mapTilePixelSize() { return MAP_TILE_PIXEL_SIZES[Math.max(0, Math.min(MAP_TILE_PIXEL_SIZES.length-1, mapTileSizeIndex))]; }
    String worldZoomLabel() { return WORLD_ZOOM_LABELS[Math.max(0, Math.min(WORLD_ZOOM_LABELS.length-1, worldZoomIndex))]; }
    int worldZoomPercent() { return WORLD_ZOOM_PERCENTS[Math.max(0, Math.min(WORLD_ZOOM_PERCENTS.length-1, worldZoomIndex))]; }
    String downscaleLabel() { return DOWNSCALE_LABELS[Math.max(0, Math.min(DOWNSCALE_LABELS.length-1, downscaleIndex))]; }
    float downscaleFactor() { return DOWNSCALE_FACTORS[Math.max(0, Math.min(DOWNSCALE_FACTORS.length-1, downscaleIndex))]; }
    String targetFpsLabel() { return TARGET_FPS_LABELS[Math.max(0, Math.min(TARGET_FPS_LABELS.length-1, targetFpsIndex))]; }
    int targetFpsValue() { return TARGET_FPS_VALUES[Math.max(0, Math.min(TARGET_FPS_VALUES.length-1, targetFpsIndex))]; }
    int targetTimerDelayMs() { return FramePacingAndStressFramework.preferredTimerDelayMillis(this); }
    String frameLimitLabel() { return isFrameLimited && targetFpsValue() > 0 ? "ON" : "OFF"; }
    String renderQualityLabel() { return RENDER_QUALITY_LABELS[Math.max(0, Math.min(RENDER_QUALITY_LABELS.length-1, renderQualityIndex))]; }
    String lightingFxLabel() { return LIGHTING_FX_LABELS[Math.max(0, Math.min(LIGHTING_FX_LABELS.length-1, lightingFxIndex))]; }
    String singlePlayerTickModeLabel() { return SINGLE_PLAYER_TICK_MODE_LABELS[Math.max(0, Math.min(SINGLE_PLAYER_TICK_MODE_LABELS.length-1, singlePlayerTickModeIndex))]; }
    boolean passiveSinglePlayerTicking() { return singlePlayerTickModeIndex == 1; }
    String doomFogModeLabel() { return DOOM_FOG_MODE_LABELS[Math.max(0, Math.min(DOOM_FOG_MODE_LABELS.length-1, doomFogModeIndex))]; }

    static Path settingsPath() { return Paths.get("settings", "options.properties"); }
    static GameOptions load() {
        GameOptions o = new GameOptions();
        try {
            Path p = settingsPath();
            if (Files.exists(p)) {
                Properties pr = new Properties();
                try (InputStream in = Files.newInputStream(p)) { pr.load(in); }
                o.fontScale = Math.max(50, Math.min(200, Integer.parseInt(pr.getProperty("fontScale", String.valueOf(o.fontScale)))));
                o.uiScale = Math.max(50, Math.min(200, Integer.parseInt(pr.getProperty("uiScale", String.valueOf(o.uiScale)))));
                String textPolicy = pr.getProperty("textDensityPolicyVersion", "");
                if (!TEXT_DENSITY_POLICY_VERSION.equals(textPolicy)) {
                    // Earlier development archives shipped with 100% as the silent default. In practice that made
                    // every menu start oversized and encouraged layout crowding. Treat untouched legacy 100% as the
                    // old default and migrate it down once; explicit user-chosen smaller/larger values are preserved.
                    if (o.fontScale == 100) o.fontScale = COMPACT_DEFAULT_FONT_SCALE;
                    if (o.uiScale == 100) o.uiScale = COMPACT_DEFAULT_UI_SCALE;
                }
                o.soundEnabled = Boolean.parseBoolean(pr.getProperty("soundEnabled", String.valueOf(o.soundEnabled)));
                o.musicEnabled = Boolean.parseBoolean(pr.getProperty("musicEnabled", String.valueOf(o.musicEnabled)));
                o.conversationSound = Boolean.parseBoolean(pr.getProperty("conversationSound", String.valueOf(o.conversationSound)));
                o.bootSound = false;
                o.importedPortraits = Boolean.parseBoolean(pr.getProperty("importedPortraits", String.valueOf(o.importedPortraits)));
                o.tileIconRendering = Boolean.parseBoolean(pr.getProperty("tileIconRendering", String.valueOf(o.tileIconRendering)));
                o.artQualityIndex = Math.max(0, Math.min(ART_QUALITY_LABELS.length-1, Integer.parseInt(pr.getProperty("artQualityIndex", String.valueOf(o.artQualityIndex)))));
                o.generatedAssetPayloadRoot = pr.getProperty("generatedAssetPayloadRoot", o.generatedAssetPayloadRoot == null ? "" : o.generatedAssetPayloadRoot).trim();
                o.mapTileSizeIndex = Math.max(0, Math.min(MAP_TILE_SIZE_LABELS.length-1, Integer.parseInt(pr.getProperty("mapTileSizeIndex", String.valueOf(o.mapTileSizeIndex)))));
                o.worldZoomIndex = Math.max(0, Math.min(WORLD_ZOOM_LABELS.length-1, Integer.parseInt(pr.getProperty("worldZoomIndex", String.valueOf(o.worldZoomIndex)))));
                o.hoverHelp = Boolean.parseBoolean(pr.getProperty("hoverHelp", String.valueOf(o.hoverHelp)));
                o.screenSaver = Boolean.parseBoolean(pr.getProperty("screenSaver", String.valueOf(o.screenSaver)));
                o.sfxVolume = Integer.parseInt(pr.getProperty("sfxVolume", String.valueOf(o.sfxVolume)));
                o.musicVolume = Integer.parseInt(pr.getProperty("musicVolume", String.valueOf(o.musicVolume)));
                o.conversationVolume = Integer.parseInt(pr.getProperty("conversationVolume", String.valueOf(o.conversationVolume)));
                o.windowMode = Math.max(0, Math.min(2, Integer.parseInt(pr.getProperty("windowMode", String.valueOf(o.windowMode)))));
                o.resolutionIndex = Math.max(0, Math.min(RESOLUTIONS.length-1, Integer.parseInt(pr.getProperty("resolutionIndex", String.valueOf(o.resolutionIndex)))));
                o.resolutionUserSelected = Boolean.parseBoolean(pr.getProperty("resolutionUserSelected", String.valueOf(o.resolutionUserSelected)));
                o.downscaleIndex = Math.max(0, Math.min(DOWNSCALE_LABELS.length-1, Integer.parseInt(pr.getProperty("downscaleIndex", String.valueOf(o.downscaleIndex)))));
                o.targetFpsIndex = Math.max(0, Math.min(TARGET_FPS_LABELS.length-1, Integer.parseInt(pr.getProperty("targetFpsIndex", String.valueOf(o.targetFpsIndex)))));
                o.isFrameLimited = Boolean.parseBoolean(pr.getProperty("isFrameLimited", String.valueOf(o.isFrameLimited)));
                o.renderQualityIndex = Math.max(0, Math.min(RENDER_QUALITY_LABELS.length-1, Integer.parseInt(pr.getProperty("renderQualityIndex", String.valueOf(o.renderQualityIndex)))));
                o.reducedMotion = Boolean.parseBoolean(pr.getProperty("reducedMotion", String.valueOf(o.reducedMotion)));
                o.diagnosticsOverlay = Boolean.parseBoolean(pr.getProperty("diagnosticsOverlay", String.valueOf(o.diagnosticsOverlay)));
                o.lightingFxIndex = Math.max(0, Math.min(LIGHTING_FX_LABELS.length-1, Integer.parseInt(pr.getProperty("lightingFxIndex", String.valueOf(o.lightingFxIndex)))));
                o.cvdModeIndex = Math.max(0, Math.min(3, Integer.parseInt(pr.getProperty("cvdModeIndex", String.valueOf(o.cvdModeIndex)))));
                o.highContrastText = Boolean.parseBoolean(pr.getProperty("highContrastText", String.valueOf(o.highContrastText)));
                o.instantDialogueText = Boolean.parseBoolean(pr.getProperty("instantDialogueText", String.valueOf(o.instantDialogueText)));
                o.screenShakePercent = Math.max(0, Math.min(100, Integer.parseInt(pr.getProperty("screenShakePercent", String.valueOf(o.screenShakePercent)))));
                o.subtitlesEnabled = Boolean.parseBoolean(pr.getProperty("subtitlesEnabled", String.valueOf(o.subtitlesEnabled)));
                o.skipRepeatLogoSplashes = Boolean.parseBoolean(pr.getProperty("skipRepeatLogoSplashes", String.valueOf(o.skipRepeatLogoSplashes)));
                o.autoLootEnabled = Boolean.parseBoolean(pr.getProperty("autoLootEnabled", String.valueOf(o.autoLootEnabled)));
                o.omniDirectionalGhostBuild = Boolean.parseBoolean(pr.getProperty("omniDirectionalGhostBuild", String.valueOf(o.omniDirectionalGhostBuild)));
                o.holdToRepeatConstruction = Boolean.parseBoolean(pr.getProperty("holdToRepeatConstruction", String.valueOf(o.holdToRepeatConstruction)));
                o.constructionTrapWarnings = Boolean.parseBoolean(pr.getProperty("constructionTrapWarnings", String.valueOf(o.constructionTrapWarnings)));
                o.smartStorageFilters = Boolean.parseBoolean(pr.getProperty("smartStorageFilters", String.valueOf(o.smartStorageFilters)));
                o.proxyCraftingFromLinkedStorage = Boolean.parseBoolean(pr.getProperty("proxyCraftingFromLinkedStorage", String.valueOf(o.proxyCraftingFromLinkedStorage)));
                o.machineOutputAutoRouting = Boolean.parseBoolean(pr.getProperty("machineOutputAutoRouting", String.valueOf(o.machineOutputAutoRouting)));
                o.productionBlockerWarnings = Boolean.parseBoolean(pr.getProperty("productionBlockerWarnings", String.valueOf(o.productionBlockerWarnings)));
                o.globalScarcityWarnings = Boolean.parseBoolean(pr.getProperty("globalScarcityWarnings", String.valueOf(o.globalScarcityWarnings)));
                o.recipeHudPinning = Boolean.parseBoolean(pr.getProperty("recipeHudPinning", String.valueOf(o.recipeHudPinning)));
                o.favoredItemProtection = Boolean.parseBoolean(pr.getProperty("favoredItemProtection", String.valueOf(o.favoredItemProtection)));
                o.lowQualityPickupWarnings = Boolean.parseBoolean(pr.getProperty("lowQualityPickupWarnings", String.valueOf(o.lowQualityPickupWarnings)));
                o.noMixedQualityStacking = Boolean.parseBoolean(pr.getProperty("noMixedQualityStacking", String.valueOf(o.noMixedQualityStacking)));
                o.itemSafetyProfileIndex = Math.max(0, Math.min(GameplayQualityOfLifeAuthority.PROTECTION_LABELS.length-1, Integer.parseInt(pr.getProperty("itemSafetyProfileIndex", String.valueOf(o.itemSafetyProfileIndex)))));
                o.underAttackSupplyLock = Boolean.parseBoolean(pr.getProperty("underAttackSupplyLock", String.valueOf(o.underAttackSupplyLock)));
                o.safeWorkerPriorities = Boolean.parseBoolean(pr.getProperty("safeWorkerPriorities", String.valueOf(o.safeWorkerPriorities)));
                o.namedDeathAlerts = Boolean.parseBoolean(pr.getProperty("namedDeathAlerts", String.valueOf(o.namedDeathAlerts)));
                o.economicDisruptionAlerts = Boolean.parseBoolean(pr.getProperty("economicDisruptionAlerts", String.valueOf(o.economicDisruptionAlerts)));
                o.localGlobalPriceHints = Boolean.parseBoolean(pr.getProperty("localGlobalPriceHints", String.valueOf(o.localGlobalPriceHints)));
                o.invalidPlacementDiagnostics = Boolean.parseBoolean(pr.getProperty("invalidPlacementDiagnostics", String.valueOf(o.invalidPlacementDiagnostics)));
                o.ghostRoomStamps = Boolean.parseBoolean(pr.getProperty("ghostRoomStamps", String.valueOf(o.ghostRoomStamps)));
                o.blueprintPreflightChecklist = Boolean.parseBoolean(pr.getProperty("blueprintPreflightChecklist", String.valueOf(o.blueprintPreflightChecklist)));
                o.hollowBoxTool = Boolean.parseBoolean(pr.getProperty("hollowBoxTool", String.valueOf(o.hollowBoxTool)));
                o.anchorPointSnapping = Boolean.parseBoolean(pr.getProperty("anchorPointSnapping", String.valueOf(o.anchorPointSnapping)));
                o.resourceEstimateTooltips = Boolean.parseBoolean(pr.getProperty("resourceEstimateTooltips", String.valueOf(o.resourceEstimateTooltips)));
                o.standaloneBlueprintSandbox = Boolean.parseBoolean(pr.getProperty("standaloneBlueprintSandbox", String.valueOf(o.standaloneBlueprintSandbox)));
                o.blueprintCaptureTool = Boolean.parseBoolean(pr.getProperty("blueprintCaptureTool", String.valueOf(o.blueprintCaptureTool)));
                o.materialSubstitutionPrompts = Boolean.parseBoolean(pr.getProperty("materialSubstitutionPrompts", String.valueOf(o.materialSubstitutionPrompts)));
                o.singlePlayerTickModeIndex = Math.max(0, Math.min(SINGLE_PLAYER_TICK_MODE_LABELS.length-1, Integer.parseInt(pr.getProperty("singlePlayerTickModeIndex", String.valueOf(o.singlePlayerTickModeIndex)))));
                o.doomModeEnabled = Boolean.parseBoolean(pr.getProperty("doomModeEnabled", String.valueOf(o.doomModeEnabled)));
                o.doomModeFovDegrees = Math.max(60, Math.min(110, Integer.parseInt(pr.getProperty("doomModeFovDegrees", String.valueOf(o.doomModeFovDegrees)))));
                o.doomFogModeIndex = Math.max(0, Math.min(DOOM_FOG_MODE_LABELS.length-1, Integer.parseInt(pr.getProperty("doomFogModeIndex", String.valueOf(o.doomFogModeIndex)))));
                DisplayResolutionAuthority.reconcileSelectedMode(o);
                String displayPolicy = pr.getProperty("displayResolutionPolicyVersion", "");
                if (!o.resolutionUserSelected || !DISPLAY_RESOLUTION_POLICY_VERSION.equals(displayPolicy)) {
                    o.resolutionIndex = DisplayResolutionAuthority.highestSupportedChoiceIndex();
                }
                o.colorPreset = Integer.parseInt(pr.getProperty("colorPreset", String.valueOf(o.colorPreset)));
                o.colorTarget = Integer.parseInt(pr.getProperty("colorTarget", String.valueOf(o.colorTarget)));
                for (int i=0;i<o.colors.length;i++) o.colors[i] = Integer.parseInt(pr.getProperty("color"+i, String.valueOf(o.colors[i])));
                o.volume = o.sfxVolume;
            }
        } catch (Throwable t) { DebugLog.error("OPTIONS_LOAD", "Failed to load settings/options.properties", t); }
        try {
            if (!Files.exists(settingsPath())) { o.resolutionIndex = DisplayResolutionAuthority.highestSupportedChoiceIndex(); o.resolutionUserSelected = false; }
            DisplayResolutionAuthority.reconcileSelectedMode(o);
        } catch (Throwable ignored) {}
        o.applyGeneratedAssetRuntimeProperties();
        return o;
    }
    void save() {
        try {
            Files.createDirectories(settingsPath().getParent());
            Properties pr = new Properties();
            pr.setProperty("fontScale", String.valueOf(fontScale));
            pr.setProperty("uiScale", String.valueOf(uiScale));
            pr.setProperty("textDensityPolicyVersion", TEXT_DENSITY_POLICY_VERSION);
            pr.setProperty("soundEnabled", String.valueOf(soundEnabled));
            pr.setProperty("musicEnabled", String.valueOf(musicEnabled));
            pr.setProperty("conversationSound", String.valueOf(conversationSound));
            pr.setProperty("bootSound", String.valueOf(bootSound));
            pr.setProperty("importedPortraits", String.valueOf(importedPortraits));
            pr.setProperty("tileIconRendering", String.valueOf(tileIconRendering));
            pr.setProperty("artQualityIndex", String.valueOf(artQualityIndex));
            pr.setProperty("generatedAssetPayloadRoot", generatedAssetPayloadRoot());
            pr.setProperty("mapTileSizeIndex", String.valueOf(mapTileSizeIndex));
            pr.setProperty("worldZoomIndex", String.valueOf(worldZoomIndex));
            pr.setProperty("hoverHelp", String.valueOf(hoverHelp));
            pr.setProperty("screenSaver", String.valueOf(screenSaver));
            pr.setProperty("sfxVolume", String.valueOf(sfxVolume));
            pr.setProperty("musicVolume", String.valueOf(musicVolume));
            pr.setProperty("conversationVolume", String.valueOf(conversationVolume));
            pr.setProperty("windowMode", String.valueOf(windowMode));
            pr.setProperty("resolutionIndex", String.valueOf(resolutionIndex));
            pr.setProperty("displayResolutionPolicyVersion", DISPLAY_RESOLUTION_POLICY_VERSION);
            pr.setProperty("resolutionUserSelected", String.valueOf(resolutionUserSelected));
            pr.setProperty("downscaleIndex", String.valueOf(downscaleIndex));
            pr.setProperty("targetFpsIndex", String.valueOf(targetFpsIndex));
            pr.setProperty("isFrameLimited", String.valueOf(isFrameLimited));
            pr.setProperty("renderQualityIndex", String.valueOf(renderQualityIndex));
            pr.setProperty("reducedMotion", String.valueOf(reducedMotion));
            pr.setProperty("diagnosticsOverlay", String.valueOf(diagnosticsOverlay));
            pr.setProperty("lightingFxIndex", String.valueOf(lightingFxIndex));
            pr.setProperty("cvdModeIndex", String.valueOf(cvdModeIndex));
            pr.setProperty("highContrastText", String.valueOf(highContrastText));
            pr.setProperty("instantDialogueText", String.valueOf(instantDialogueText));
            pr.setProperty("screenShakePercent", String.valueOf(screenShakePercent));
            pr.setProperty("subtitlesEnabled", String.valueOf(subtitlesEnabled));
            pr.setProperty("skipRepeatLogoSplashes", String.valueOf(skipRepeatLogoSplashes));
            pr.setProperty("autoLootEnabled", String.valueOf(autoLootEnabled));
            pr.setProperty("omniDirectionalGhostBuild", String.valueOf(omniDirectionalGhostBuild));
            pr.setProperty("holdToRepeatConstruction", String.valueOf(holdToRepeatConstruction));
            pr.setProperty("constructionTrapWarnings", String.valueOf(constructionTrapWarnings));
            pr.setProperty("smartStorageFilters", String.valueOf(smartStorageFilters));
            pr.setProperty("proxyCraftingFromLinkedStorage", String.valueOf(proxyCraftingFromLinkedStorage));
            pr.setProperty("machineOutputAutoRouting", String.valueOf(machineOutputAutoRouting));
            pr.setProperty("productionBlockerWarnings", String.valueOf(productionBlockerWarnings));
            pr.setProperty("globalScarcityWarnings", String.valueOf(globalScarcityWarnings));
            pr.setProperty("recipeHudPinning", String.valueOf(recipeHudPinning));
            pr.setProperty("favoredItemProtection", String.valueOf(favoredItemProtection));
            pr.setProperty("lowQualityPickupWarnings", String.valueOf(lowQualityPickupWarnings));
            pr.setProperty("noMixedQualityStacking", String.valueOf(noMixedQualityStacking));
            pr.setProperty("itemSafetyProfileIndex", String.valueOf(itemSafetyProfileIndex));
            pr.setProperty("underAttackSupplyLock", String.valueOf(underAttackSupplyLock));
            pr.setProperty("safeWorkerPriorities", String.valueOf(safeWorkerPriorities));
            pr.setProperty("namedDeathAlerts", String.valueOf(namedDeathAlerts));
            pr.setProperty("economicDisruptionAlerts", String.valueOf(economicDisruptionAlerts));
            pr.setProperty("localGlobalPriceHints", String.valueOf(localGlobalPriceHints));
            pr.setProperty("invalidPlacementDiagnostics", String.valueOf(invalidPlacementDiagnostics));
            pr.setProperty("ghostRoomStamps", String.valueOf(ghostRoomStamps));
            pr.setProperty("blueprintPreflightChecklist", String.valueOf(blueprintPreflightChecklist));
            pr.setProperty("hollowBoxTool", String.valueOf(hollowBoxTool));
            pr.setProperty("anchorPointSnapping", String.valueOf(anchorPointSnapping));
            pr.setProperty("resourceEstimateTooltips", String.valueOf(resourceEstimateTooltips));
            pr.setProperty("standaloneBlueprintSandbox", String.valueOf(standaloneBlueprintSandbox));
            pr.setProperty("blueprintCaptureTool", String.valueOf(blueprintCaptureTool));
            pr.setProperty("materialSubstitutionPrompts", String.valueOf(materialSubstitutionPrompts));
            pr.setProperty("singlePlayerTickModeIndex", String.valueOf(Math.max(0, Math.min(SINGLE_PLAYER_TICK_MODE_LABELS.length-1, singlePlayerTickModeIndex))));
            pr.setProperty("doomModeEnabled", String.valueOf(doomModeEnabled));
            pr.setProperty("doomModeFovDegrees", String.valueOf(Math.max(60, Math.min(110, doomModeFovDegrees))));
            pr.setProperty("doomFogModeIndex", String.valueOf(Math.max(0, Math.min(DOOM_FOG_MODE_LABELS.length-1, doomFogModeIndex))));
            pr.setProperty("colorPreset", String.valueOf(colorPreset));
            pr.setProperty("colorTarget", String.valueOf(colorTarget));
            for (int i=0;i<colors.length;i++) pr.setProperty("color"+i, String.valueOf(colors[i]));
            try (OutputStream out = Files.newOutputStream(settingsPath())) { pr.store(out, "The Mechanist persistent options"); }
            applyGeneratedAssetRuntimeProperties();
        } catch (Throwable t) { DebugLog.error("OPTIONS_SAVE", "Failed to save settings/options.properties", t); }
    }

    String summary() {
        return "fontScale=" + fontScale + "% uiScale=" + uiScale + "% displayDensity=" + DisplayDensityAuthority.auditSummary(this) + " sound=" + soundEnabled +
               " sfxVolume=" + sfxVolume + "% music=" + musicEnabled + " musicVolume=" + musicVolume +
               "% conversation=" + conversationSound + " conversationVolume=" + conversationVolume +
               "% startupSfx=false portraits=" + importedPortraits + " tileIconRendering=" + tileIconRendering + " artQuality=" + artQualityLabel() + " generatedPayload=" + generatedAssetPayloadRootShortLabel() + " mapTileSize=" + mapTileSizeLabel() + " worldZoom=" + worldZoomLabel() + " hoverHelp=" + hoverHelp +
               " screenSaver=" + screenSaver + " windowMode=" + windowModeLabel() + " resolution=" + resolutionLabel() + " resolutionUserSelected=" + resolutionUserSelected + " downscale=" + downscaleLabel() +
               " targetFps=" + targetFpsLabel() + " frameLimited=" + frameLimitLabel() + " renderQuality=" + renderQualityLabel() + " lightingFx=" + lightingFxLabel() + " reducedMotion=" + reducedMotion + " diagnosticsOverlay=" + diagnosticsOverlay +
               " cvdMode=" + AccessibilityCompatibilityAuthority.cvdLabel(cvdModeIndex) + " highContrastText=" + highContrastText + " instantDialogueText=" + instantDialogueText + " screenShake=" + screenShakePercent + "%" +
               " " + GameplayQualityOfLifeAuthority.auditSummary(this) +
               " constructionEditor=" + BlueprintConstructionAuthority.auditSummary() +
               " singlePlayerTickMode=" + singlePlayerTickModeLabel() +
               " doomMode=" + doomModeEnabled + " doomFov=" + doomModeFovDegrees + " doomFog=" + doomFogModeLabel() +
               " colorPreset=" + PALETTE_NAMES[Math.max(0, Math.min(PALETTE_NAMES.length-1, colorPreset))] +
               " colorTarget=" + colorTargetLabel();
    }

    String windowModeLabel() {
        switch(windowMode) { case 1: return "Borderless Windowed"; case 2: return "Exclusive Fullscreen"; default: return "Windowed"; }
    }
    static String resolutionLabel(int idx) { return DisplayResolutionAuthority.modeLabel(idx); }
    String resolutionLabel() { return DisplayResolutionAuthority.modeLabel(resolutionIndex); }
    String colorTargetLabel() { return COLOR_KEYS[Math.max(0, Math.min(COLOR_KEYS.length-1, colorTarget))]; }
    int colorValue(int idx) { return colors[Math.max(0, Math.min(colors.length-1, idx))]; }
    void applyPalette() { colors = Arrays.copyOf(PALETTES[colorPreset], PALETTES[colorPreset].length); }
    void adjustSelectedColor(int delta) {
        int idx = Math.max(0, Math.min(colors.length-1, colorTarget));
        Color c = new Color(colors[idx]);
        int r = Math.max(0, Math.min(255, c.getRed() + delta));
        int g = Math.max(0, Math.min(255, c.getGreen() + delta));
        int b = Math.max(0, Math.min(255, c.getBlue() + delta));
        colors[idx] = new Color(r,g,b).getRGB() & 0xFFFFFF;
    }
}

class OptionsBoundaryAuthority {
    private static int pct(int value) { return Math.max(0, Math.min(100, value)); }

    static String changeSfxVolume(GameOptions options, int delta) {
        if (options == null) return "SFX volume unchanged: options unavailable.";
        options.sfxVolume = pct(options.sfxVolume + delta);
        options.volume = options.sfxVolume;
        options.save();
        return "SFX volume now " + options.sfxVolume + "%.";
    }

    static String changeMusicVolume(GameOptions options, int delta) {
        if (options == null) return "Music volume unchanged: options unavailable.";
        options.musicVolume = pct(options.musicVolume + delta);
        options.save();
        return "Music volume now " + options.musicVolume + "%. Dynamic music channel updates immediately.";
    }

    static String changeConversationVolume(GameOptions options, int delta) {
        if (options == null) return "Conversation volume unchanged: options unavailable.";
        options.conversationVolume = pct(options.conversationVolume + delta);
        options.save();
        return "Conversation volume now " + options.conversationVolume + "%.";
    }

    static String cycleWindowMode(GameOptions options) {
        if (options == null) return "Window mode unchanged: options unavailable.";
        options.windowMode = (options.windowMode + 1) % 3;
        return "Window mode selected: " + options.windowModeLabel() + ". Press APPLY WINDOW to change the frame.";
    }

    static String changeResolution(GameOptions options, int delta) {
        if (options == null) return "Resolution unchanged: options unavailable.";
        options.resolutionIndex = Math.max(0, Math.min(DisplayResolutionAuthority.choiceCount() - 1, options.resolutionIndex + delta));
        return "Resolution preset selected: " + options.resolutionLabel() + ". Press APPLY WINDOW to resize.";
    }

    static String setWindowMode(GameOptions options, int mode) {
        if (options == null) return "Window mode unchanged: options unavailable.";
        options.windowMode = Math.max(0, Math.min(2, mode));
        options.save();
        return "Window mode selected: " + options.windowModeLabel() + ". Press APPLY WINDOW to change the frame.";
    }

    static String setResolutionIndex(GameOptions options, int idx) {
        if (options == null) return "Resolution unchanged: options unavailable.";
        options.resolutionIndex = Math.max(0, Math.min(DisplayResolutionAuthority.choiceCount() - 1, idx));
        options.resolutionUserSelected = true;
        options.save();
        return "Resolution applied to pending window target: " + options.resolutionLabel() + ".";
    }

    static String setDownscaleIndex(GameOptions options, int idx) {
        if (options == null) return "Downscale unchanged: options unavailable.";
        options.downscaleIndex = Math.max(0, Math.min(GameOptions.DOWNSCALE_LABELS.length - 1, idx));
        options.save();
        return "Render downscale now " + options.downscaleLabel() + ".";
    }

    static String setColorPreset(GameOptions options, int idx) {
        if (options == null) return "Color preset unchanged: options unavailable.";
        options.colorPreset = Math.max(0, Math.min(GameOptions.PALETTES.length - 1, idx));
        options.applyPalette();
        options.save();
        return "Color preset applied globally: " + GameOptions.PALETTE_NAMES[options.colorPreset] + ".";
    }

    static String cycleColorTarget(GameOptions options) {
        if (options == null) return "Color key unchanged: options unavailable.";
        options.colorTarget = (options.colorTarget + 1) % GameOptions.COLOR_KEYS.length;
        return "Editing color key: " + options.colorTargetLabel() + ".";
    }

    static String cycleColorPreset(GameOptions options) {
        if (options == null) return "Color preset unchanged: options unavailable.";
        options.colorPreset = (options.colorPreset + 1) % GameOptions.PALETTES.length;
        options.applyPalette();
        return "Color preset: " + GameOptions.PALETTE_NAMES[options.colorPreset] + ".";
    }

    static String adjustSelectedColor(GameOptions options, int delta) {
        if (options == null) return "Color unchanged: options unavailable.";
        options.adjustSelectedColor(delta);
        options.save();
        return options.colorTargetLabel() + " color now #" + String.format("%06X", options.colorValue(options.colorTarget) & 0xFFFFFF) + ".";
    }

    static Color optionColor(GameOptions options, int key) {
        Color raw = new Color(options == null ? 0xDCCD91 : options.colorValue(key));
        if (key == GameOptions.BACKGROUND) return raw;
        Color background = new Color(options == null ? GameOptions.PALETTES[0][GameOptions.BACKGROUND] : options.colorValue(GameOptions.BACKGROUND));
        double minimum = key == GameOptions.TEXT_DIM ? 3.0 : 4.5;
        if (contrastRatio(raw, background) >= minimum) return raw;
        return fallbackReadableColor(key, background);
    }

    private static Color fallbackReadableColor(int key, Color background) {
        boolean darkBackground = luminance(background) < 0.38;
        if (!darkBackground) {
            return switch (key) {
                case GameOptions.TEXT_TITLE -> new Color(30, 26, 18);
                case GameOptions.TEXT_DIM -> new Color(72, 70, 62);
                case GameOptions.TEXT_HIGHLIGHT -> new Color(96, 58, 0);
                default -> new Color(24, 24, 22);
            };
        }
        return switch (key) {
            case GameOptions.TEXT_TITLE -> new Color(245, 232, 170);
            case GameOptions.TEXT_DIM -> new Color(172, 176, 156);
            case GameOptions.TEXT_HIGHLIGHT -> new Color(255, 218, 92);
            default -> new Color(224, 226, 208);
        };
    }

    private static double contrastRatio(Color a, Color b) {
        double la = luminance(a) + 0.05;
        double lb = luminance(b) + 0.05;
        return Math.max(la, lb) / Math.min(la, lb);
    }

    private static double luminance(Color color) {
        return channel(color.getRed()) * 0.2126 + channel(color.getGreen()) * 0.7152 + channel(color.getBlue()) * 0.0722;
    }

    private static double channel(int value) {
        double c = Math.max(0, Math.min(255, value)) / 255.0;
        return c <= 0.03928 ? c / 12.92 : Math.pow((c + 0.055) / 1.055, 2.4);
    }

    static String setTargetFpsIndex(GameOptions options, int idx) {
        if (options == null) return "Frame pacing unchanged: options unavailable.";
        options.targetFpsIndex = Math.max(0, Math.min(GameOptions.TARGET_FPS_LABELS.length - 1, idx));
        options.save();
        return "Target frame pacing set to " + options.targetFpsLabel() + " with frame limiter " + options.frameLimitLabel() + ". Simulation remains server-authoritative.";
    }

    static String toggleFrameLimiter(GameOptions options) {
        if (options == null) return "Frame limiter unchanged: options unavailable.";
        options.isFrameLimited = !options.isFrameLimited;
        options.save();
        return "Frame limiter " + options.frameLimitLabel() + ". " + (options.isFrameLimited ? "The render pulse is paced to the selected target FPS." : "The render pulse is uncapped for diagnostics/stress testing.");
    }

    static String setRenderQualityIndex(GameOptions options, int idx) {
        if (options == null) return "Render quality unchanged: options unavailable.";
        options.renderQualityIndex = Math.max(0, Math.min(GameOptions.RENDER_QUALITY_LABELS.length - 1, idx));
        options.save();
        return "Render quality set to " + options.renderQualityLabel() + ".";
    }

    static String setLightingFxIndex(GameOptions options, int idx) {
        if (options == null) return "Lighting effects unchanged: options unavailable.";
        options.lightingFxIndex = Math.max(0, Math.min(GameOptions.LIGHTING_FX_LABELS.length-1, idx));
        options.save();
        return "Visual lighting effects set to " + options.lightingFxLabel() + ".";
    }

    static String toggleReducedMotion(GameOptions options) {
        if (options == null) return "Reduced motion unchanged: options unavailable.";
        options.reducedMotion = !options.reducedMotion;
        options.save();
        return "Reduced motion " + (options.reducedMotion ? "enabled" : "disabled") + ".";
    }

    static String toggleDiagnosticsOverlay(GameOptions options) {
        if (options == null) return "Diagnostics overlay unchanged: options unavailable.";
        options.diagnosticsOverlay = !options.diagnosticsOverlay;
        options.save();
        return "Performance diagnostics overlay " + (options.diagnosticsOverlay ? "enabled" : "disabled") + ". Press F3 to toggle during play.";
    }

    static String auditSummary(GameOptions options) {
        return "optionsBoundary " + (options == null ? "unavailable" : options.summary());
    }
}

class DisplayResolutionAuthority {
    static final class DisplayChoice {
        final int width; final int height; final int refreshRate; final boolean detected;
        DisplayChoice(int width, int height, int refreshRate, boolean detected) { this.width=width; this.height=height; this.refreshRate=refreshRate; this.detected=detected; }
        String label() { return width + " x " + height + (refreshRate > 0 ? " @ " + refreshRate + "Hz" : ""); }
        String key() { return width + "x" + height + "@" + refreshRate; }
    }

    private static java.util.List<DisplayChoice> cachedChoices;

    static synchronized java.util.List<DisplayChoice> choices() {
        if (cachedChoices != null && !cachedChoices.isEmpty()) return cachedChoices;
        java.util.LinkedHashMap<String, DisplayChoice> map = new java.util.LinkedHashMap<>();
        try {
            GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
            DisplayMode current = gd == null ? null : gd.getDisplayMode();
            if (current != null) add(map, current.getWidth(), current.getHeight(), safeRefresh(current), true);
            if (gd != null) {
                for (DisplayMode dm : gd.getDisplayModes()) {
                    if (dm == null || dm.getWidth() < 800 || dm.getHeight() < 540) continue;
                    add(map, dm.getWidth(), dm.getHeight(), safeRefresh(dm), true);
                }
            }
        } catch (Throwable t) { DebugLog.warn("DISPLAY_RESOLUTION_DETECT", "Could not enumerate display modes; using safe defaults."); }
        Rectangle b = primaryBounds();
        int[][] safe = GameOptions.RESOLUTIONS;
        for (int[] r : safe) add(map, r[0], r[1], 0, false);
        add(map, 960, 540, 0, false);
        java.util.ArrayList<DisplayChoice> list = new java.util.ArrayList<>(map.values());
        list.sort(java.util.Comparator.comparingInt((DisplayChoice c) -> c.width).thenComparingInt(c -> c.height).thenComparingInt(c -> c.refreshRate));
        cachedChoices = java.util.Collections.unmodifiableList(list);
        return cachedChoices;
    }

    private static void add(java.util.Map<String, DisplayChoice> map, int w, int h, int hz, boolean detected) {
        if (w <= 0 || h <= 0) return;
        DisplayChoice c = new DisplayChoice(w, h, hz, detected);
        map.putIfAbsent(c.key(), c);
    }

    private static int safeRefresh(DisplayMode dm) {
        int hz = dm.getRefreshRate();
        return hz == DisplayMode.REFRESH_RATE_UNKNOWN ? 0 : Math.max(0, hz);
    }

    static Rectangle primaryBounds() {
        try {
            GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
            GraphicsConfiguration cfg = gd == null ? null : gd.getDefaultConfiguration();
            Rectangle b = cfg == null ? null : cfg.getBounds();
            if (b != null && b.width > 0 && b.height > 0) return b;
        } catch (Throwable ignored) {}
        return new Rectangle(0, 0, 1366, 768);
    }

    static DisplayChoice choice(int idx) {
        java.util.List<DisplayChoice> list = choices();
        if (list.isEmpty()) return new DisplayChoice(1366,768,0,false);
        return list.get(Math.max(0, Math.min(list.size()-1, idx)));
    }

    static int choiceCount() { return choices().size(); }
    static String modeLabel(int idx) { return choice(idx).label(); }
    static int[] modeSize(int idx) { DisplayChoice c = choice(idx); return new int[]{c.width, c.height}; }

    static int highestSupportedChoiceIndex() {
        java.util.List<DisplayChoice> list = choices();
        if (list.isEmpty()) return 0;
        int best = 0;
        long bestPixels = -1L;
        int bestRefresh = -1;
        for (int i=0; i<list.size(); i++) {
            DisplayChoice c = list.get(i);
            long pixels = (long)c.width * (long)c.height;
            if (pixels > bestPixels || (pixels == bestPixels && c.refreshRate > bestRefresh)) {
                best = i;
                bestPixels = pixels;
                bestRefresh = c.refreshRate;
            }
        }
        return best;
    }

    static void reconcileSelectedMode(GameOptions options) {
        if (options == null) return;
        options.resolutionIndex = Math.max(0, Math.min(choiceCount() - 1, options.resolutionIndex));
    }

    static String auditSummary(GameOptions options) {
        DisplayChoice c = choice(options == null ? 0 : options.resolutionIndex);
        return "displayResolution choices=" + choiceCount() + " selected=" + c.label() + " detected=" + c.detected;
    }
}

class WindowModeSurfaceAuthority {
    private static int requestedMode(GameOptions options) {
        return options == null ? 1 : Math.max(0, Math.min(2, options.windowMode));
    }

    private static boolean trueExclusiveFullscreenAllowed() {
        return Boolean.getBoolean("mechanist.allowExclusiveFullscreen");
    }

    private static int mode(GameOptions options) {
        int requested = requestedMode(options);
        return requested == 2 && !trueExclusiveFullscreenAllowed() ? 1 : requested;
    }

    private static String modeLabel(int mode) {
        return switch (Math.max(0, Math.min(2, mode))) {
            case 1 -> "Borderless Windowed";
            case 2 -> "Exclusive Fullscreen";
            default -> "Windowed";
        };
    }

    private static boolean exclusiveDowngraded(GameOptions options) {
        return requestedMode(options) == 2 && mode(options) != 2;
    }

    private static int[] resolution(GameOptions options) {
        int idx = options == null ? 0 : options.resolutionIndex;
        return DisplayResolutionAuthority.modeSize(idx);
    }

    private static Rectangle primaryDisplayBounds() {
        try {
            GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
            GraphicsConfiguration cfg = gd.getDefaultConfiguration();
            Rectangle b = cfg == null ? null : cfg.getBounds();
            if (b != null && b.width > 0 && b.height > 0) return b;
        } catch (Throwable ignored) {}
        return new Rectangle(0, 0, 1366, 768);
    }

    static void configureInitialFrame(JFrame f, GameOptions options) {
        if (f == null) return;
        int m = mode(options);
        int[] r = resolution(options);
        try {
            f.setMinimumSize(new Dimension(960, 540));
            f.setUndecorated(m == 1 || m == 2);
            if (m == 1) {
                Rectangle b = primaryDisplayBounds();
                f.setBounds(b);
            } else {
                f.setSize(r[0], r[1]);
                f.setLocationRelativeTo(null);
            }
            DebugLog.audit("WINDOW_STARTUP", "initialMode=" + (options == null ? "Borderless Windowed" : options.windowModeLabel())
                    + (exclusiveDowngraded(options) ? " appliedMode=Borderless Windowed screenshotSafe=true" : "")
                    + " resolution=" + (options == null ? "1366 x 768" : options.resolutionLabel())
                    + " undecorated=" + (m == 1 || m == 2));
        } catch (Throwable t) {
            DebugLog.error("WINDOW_STARTUP", "Failed initial window-mode configuration; using safe decorated window.", t);
            try {
                f.setUndecorated(false);
                f.setSize(r[0], r[1]);
                f.setLocationRelativeTo(null);
            } catch (Throwable ignored) {}
        }
    }

    static void activateInitialFrame(JFrame f, GameOptions options) {
        if (f == null) return;
        if (mode(options) != 2) {
            if (exclusiveDowngraded(options)) DebugLog.audit("WINDOW_STARTUP", "exclusiveFullscreenSkipped=screenshot-safe-borderless");
            return;
        }
        try {
            GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
            gd.setFullScreenWindow(f);
            DebugLog.audit("WINDOW_STARTUP", "exclusiveFullscreenActivated=true");
        } catch (Throwable t) {
            DebugLog.error("WINDOW_STARTUP", "Failed to activate exclusive fullscreen at startup; retaining visible frame.", t);
        }
    }

    static String apply(Component owner, GameOptions options) {
        if (owner == null) return "Could not apply window mode: panel unavailable.";
        if (options == null) return "Could not apply window mode: options unavailable.";
        Window win = SwingUtilities.getWindowAncestor(owner);
        if (!(win instanceof JFrame)) return "Could not apply window mode: parent frame unavailable.";
        JFrame f = (JFrame) win;
        GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        int m = mode(options);
        int[] r = resolution(options);
        GamePanel panelOwner = owner instanceof GamePanel ? (GamePanel) owner : null;
        if (panelOwner != null) panelOwner.beginWindowModeReconfigure();
        try {
            gd.setFullScreenWindow(null);
            f.dispose(); // Required when changing undecorated state; runtime shutdown is suppressed during this controlled reconfigure.
            f.setUndecorated(m == 1 || m == 2);
            if (m == 2) {
                gd.setFullScreenWindow(f);
            } else if (m == 1) {
                Rectangle b = primaryDisplayBounds();
                f.setBounds(b);
                f.setVisible(true);
            } else {
                f.setSize(r[0], r[1]);
                f.setLocationRelativeTo(null);
                f.setVisible(true);
            }
            owner.requestFocusInWindow();
            String applied = modeLabel(m);
            if (exclusiveDowngraded(options)) return "Applied " + applied + " at " + options.resolutionLabel() + " (screenshot-safe; true exclusive fullscreen is disabled).";
            return "Applied " + applied + " at " + options.resolutionLabel() + ".";
        } catch (Throwable t) {
            DebugLog.error("GRAPHICS_OPTIONS", "Failed to apply window mode/resolution", t);
            try { f.setUndecorated(false); f.setVisible(true); } catch (Throwable ignored) {}
            return "Graphics option failed; see logs/errors.log.";
        } finally {
            if (panelOwner != null) panelOwner.endWindowModeReconfigure();
        }
    }

    static String auditSummary() {
        return "windowModeSurface owner=Swing frame options=GameOptions dynamicDisplayModes=true borderlessWindowedUsesPrimaryDisplayBounds=true resolutionBounds=detectedOrSafeFallback";
    }
}

