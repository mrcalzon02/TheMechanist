package mechanist.launcher;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;

/** Headless regression for long install paths, task gating and failure feedback. */
final class LauncherPresentationSmoke {
    public static void main(String[] args) throws Exception {
        Path sandbox = Files.createTempDirectory("Mechanist launcher presentation ");
        String property = "mechanist.launcher.installRoot";
        String previous = System.getProperty(property);
        String previousData = System.getProperty("mechanist.launcher.userDataRoot");
        String previousStorage = System.getProperty("mechanist.storage.root");
        try {
            System.setProperty(property, sandbox.resolve("long installation directory ".repeat(6) + "game").toString());
            System.setProperty("mechanist.launcher.userDataRoot", sandbox.resolve("user data").toString());
            SwingUtilities.invokeAndWait(() -> {
                try { verify(); } catch (Exception exception) { throw new RuntimeException(exception); }
            });
            verifyStorageHandoff(sandbox, java.util.List.of(args).contains("--client-integration"));
        } finally {
            if (previous == null) System.clearProperty(property); else System.setProperty(property, previous);
            restore("mechanist.launcher.userDataRoot", previousData);
            restore("mechanist.storage.root", previousStorage);
            try (var paths = Files.walk(sandbox)) {
                for (Path path : paths.sorted(java.util.Comparator.reverseOrder()).toList()) Files.deleteIfExists(path);
            }
        }
        System.out.println("LauncherPresentationSmoke PASS longPaths=true boundedActions=true busyGating=true failurePreserved=true storageHandoff=true clientIntegration="
                + java.util.List.of(args).contains("--client-integration"));
    }

    private static void verifyStorageHandoff(Path sandbox, boolean clientIntegration) throws Exception {
        System.clearProperty("mechanist.launcher.userDataRoot");
        System.clearProperty("mechanist.storage.root");
        if (clientIntegration) {
            Class<?> optionsType = Class.forName("mechanist.GameOptions");
            Method settingsPath = optionsType.getDeclaredMethod("settingsPath"); settingsPath.setAccessible(true);
            require(LauncherConfig.defaults().clientSettingsDir.resolve("options.properties").equals(settingsPath.invoke(null)),
                    "Default launcher and client data roots differ on this platform");
        }
        System.setProperty("mechanist.storage.root", sandbox.resolve("existing client data").toString());
        System.setProperty("mechanist.launcher.installRoot", sandbox.resolve("installed game").toString());
        LauncherConfig config = LauncherConfig.defaults();
        require(config.saveDir.equals(sandbox.resolve("existing client data/saves")), "Launcher displays a different save root");
        Path legacy = config.clientPackageDir.resolve("settings/options.properties");
        Files.createDirectories(legacy.getParent());
        Files.writeString(legacy, "musicVolume=27\nfontScale=85\n");
        var graphics = PackageCatalog.defaultGraphicsTiers(config);
        RuntimeSelectionWriter.writeSelections(config, PackageCatalog.byId(graphics, "intermediate_128"), null);
        Path options = config.clientSettingsDir.resolve("options.properties");
        var stored = new java.util.Properties();
        try (var in = Files.newInputStream(options)) { stored.load(in); }
        require("2".equals(stored.getProperty("artQualityIndex")), "Client graphics setting was not written");
        require("27".equals(stored.getProperty("musicVolume")), "Migration lost an existing client preference");
        require(Files.readString(legacy).equals("musicVolume=27\nfontScale=85\n"), "Legacy settings were mutated");
        RuntimeSelectionWriter.writeSelections(config, PackageCatalog.byId(graphics, "low_32"), null);
        try (var paths = Files.list(config.clientSettingsDir)) {
            require(paths.count() == 1, "Atomic settings replacement left temporary files behind");
        }
        if (clientIntegration) {
            Class<?> optionsType = Class.forName("mechanist.GameOptions");
            Method settingsPath = optionsType.getDeclaredMethod("settingsPath"); settingsPath.setAccessible(true);
            require(options.equals(settingsPath.invoke(null)), "Launcher and real client options paths differ");
            Method load = optionsType.getDeclaredMethod("load"); load.setAccessible(true);
            Object loaded = load.invoke(null);
            Field quality = optionsType.getDeclaredField("artQualityIndex"); quality.setAccessible(true);
            Field volume = optionsType.getDeclaredField("musicVolume"); volume.setAccessible(true);
            require(quality.getInt(loaded) == 0 && volume.getInt(loaded) == 27,
                    "Real client failed to read launcher-selected graphics or preserved volume");
        }
        SwingUtilities.invokeAndWait(() -> {
            try {
                MechanistLauncherApp reopened = new MechanistLauncherApp();
                reopened.buildContent();
                invoke(reopened, "loadPackageSelections");
                PackageTier selected = (PackageTier) ((JComboBox<?>) field(reopened, "graphicsTier")).getSelectedItem();
                require(selected != null && selected.id.equals("low_32"), "Launcher restart reset the saved graphics choice");
            } catch (Exception exception) { throw new RuntimeException(exception); }
        });
    }

    private static void restore(String property, String previous) {
        if (previous == null) System.clearProperty(property); else System.setProperty(property, previous);
    }

    private static void verify() throws Exception {
        MechanistLauncherApp app = new MechanistLauncherApp();
        JPanel root = app.buildContent();
        invoke(app, "refreshState");
        for (int width : new int[] {944, 1004, 1404}) {
            root.setSize(width, 640);
            layout(root);
            checkBounds(root);
            BufferedImage image = new BufferedImage(width, 640, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = image.createGraphics();
            try { root.printAll(g); } finally { g.dispose(); }
            if (System.getProperty("mechanist.launcher.layoutEvidence") != null) {
                Path output = Path.of(System.getProperty("mechanist.launcher.layoutEvidence"));
                Files.createDirectories(output);
                javax.imageio.ImageIO.write(image, "png", output.resolve("launcher-" + width + ".png").toFile());
            }
        }
        java.util.List<?> buttons = (java.util.List<?>) field(app, "taskButtons");
        require(buttons.size() == 8, "Expected eight fully visible launcher actions");
        invoke(app, "setBusy", new Class<?>[] {boolean.class, String.class}, true, "Verifying packages...");
        for (Object button : buttons) require(!((JButton) button).isEnabled(), "Action enabled during verification");
        require(!((JComboBox<?>) field(app, "graphicsTier")).isEnabled(), "Graphics changed during task");
        invoke(app, "setBusy", new Class<?>[] {boolean.class, String.class}, false, "Verification failed. See log output.");
        invoke(app, "refreshState");
        require(((JTextArea) field(app, "status")).getText().startsWith("Verification failed"),
                "Refreshing selections erased the verification error");
        require(!((JButton) field(app, "launch")).isEnabled(), "Unverified client became launchable");
        require(!((JButton) field(app, "applySettings")).isEnabled(), "Unverified settings became writable");
        Field ready = MechanistLauncherApp.class.getDeclaredField("packagesReady");
        ready.setAccessible(true); ready.setBoolean(app, true);
        invoke(app, "setBusy", new Class<?>[] {boolean.class, String.class}, false, "Ready");
        require(((JButton) field(app, "launch")).isEnabled(), "Verified client stayed disabled");
        Field checking = MechanistLauncherApp.class.getDeclaredField("updateCheckBusy");
        checking.setAccessible(true); checking.setBoolean(app, true);
        invoke(app, "setBusy", new Class<?>[] {boolean.class, String.class}, false, "Ready");
        require(!((JButton) field(app, "updateButton")).isEnabled(), "Concurrent update checks became possible");
    }

    private static void layout(Container container) {
        container.doLayout();
        for (Component child : container.getComponents()) if (child instanceof Container nested) layout(nested);
    }

    private static void checkBounds(Container parent) {
        for (Component child : parent.getComponents()) {
            if (!child.isVisible()) continue;
            // Text views scroll internally; validate their enclosing controls instead.
            if (!(parent instanceof JViewport) && !(parent instanceof JTextField)) {
                Rectangle r = child.getBounds();
                require(r.width > 0 && r.height > 0 && r.x >= 0 && r.y >= 0
                                && r.x + r.width <= parent.getWidth() && r.y + r.height <= parent.getHeight(),
                        child.getClass().getSimpleName() + " clipped: " + r + " in " + parent.getSize());
                if (child instanceof JButton button) require(r.width >= button.getPreferredSize().width,
                        "Action text clipped: " + button.getText());
            }
            if (child instanceof Container nested) checkBounds(nested);
        }
    }

    private static Object field(Object target, String name) throws Exception {
        Field field = target.getClass().getDeclaredField(name); field.setAccessible(true); return field.get(target);
    }
    private static void invoke(Object target, String name) throws Exception { invoke(target, name, new Class<?>[0]); }
    private static void invoke(Object target, String name, Class<?>[] types, Object... args) throws Exception {
        Method method = target.getClass().getDeclaredMethod(name, types); method.setAccessible(true); method.invoke(target, args);
    }
    private static void require(boolean value, String message) { if (!value) throw new AssertionError(message); }
}
