package mechanist;

import javax.swing.JColorChooser;
import javax.swing.JPasswordField;
import javax.swing.JRadioButton;
import javax.swing.JSlider;

/** Structural smoke for standard Swing option editors and endpoint privacy. */
final class OptionsSwingComponentSmoke {
    public static void main(String[] args) {
        require(new JSlider() != null, "JSlider should be available in the Java 17 desktop module");
        require(new JRadioButton() != null, "JRadioButton should be available in the Java 17 desktop module");
        require(new JColorChooser() != null, "JColorChooser should be available in the Java 17 desktop module");
        JPasswordField address = new JPasswordField("192.168.1.10:25565");
        require(address.getEchoChar() != 0, "address entry should be guarded by default");
        require("address hidden:25565".equals(MultiplayerPrivacyAuthority.redactEndpoint("192.168.1.10:25565")),
                "IPv4 endpoint should retain only its port");
        require("[IPv6 address hidden]:25565".equals(MultiplayerPrivacyAuthority.redactEndpoint("[2001:db8::1]:25565")),
                "IPv6 endpoint should retain only its port");
        MultiplayerMenuController.ConnectionHistoryItem recent = new MultiplayerMenuController.ConnectionHistoryItem(
                "203.0.113.4:25565", "Remote", "now");
        require(!recent.shortLine().contains("203.0.113.4"), "recent-server rows must not expose addresses");
        OptionsLayoutAuthoritySmoke.main(args);
        verifyCompactOptions();
    }

    private static void verifyCompactOptions() {
        GamePanel panel = new GamePanel();
        if (panel.timer != null) panel.timer.stop();
        try {
            panel.setSize(320, 240);
            panel.screen = GamePanel.Screen.OPTIONS;
            var buffer = new java.awt.image.BufferedImage(320, 240,
                    java.awt.image.BufferedImage.TYPE_INT_ARGB);
            for (int tab : new int[]{4, 7}) {
                panel.optionsTab = tab;
                var reachable = new java.util.HashSet<String>();
                int pages = 0;
                while (pages++ < 30) {
                    var graphics = buffer.createGraphics();
                    try { OptionsScreenPainter.paintBody(panel, graphics); }
                    finally { graphics.dispose(); }
                    require(panel.buttons.stream().anyMatch(b -> b.label.equals("Graphics"))
                                    && panel.buttons.stream().anyMatch(b -> b.label.equals("QOL")),
                            "all tabs must stay visible on each page");
                    ButtonBox next = null;
                    for (ButtonBox button : panel.buttons) {
                        require(button.r.x >= 0 && button.r.y >= 0
                                        && button.r.x + button.r.width <= 320
                                        && button.r.y + button.r.height <= 240,
                                "button outside compact screen: " + button.label);
                        if (button.label.startsWith("Next ")) next = button;
                        else if (!button.label.startsWith("Prev ")
                                && !java.util.List.of("Display", "Text/UI", "Audio",
                                    "Controls", "Graphics", "JVM", "Access", "QOL")
                                        .contains(button.label)) reachable.add(button.label);
                    }
                    if (next == null) break;
                    next.action.run();
                }
                require(pages < 30 && reachable.size() >= 18 && reachable.contains("Back"),
                        "every " + (tab == 4 ? "Graphics" : "QOL")
                                + " command must be reachable: saw " + reachable.size());
            }
        } finally { panel.shutdownRuntime(); }
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private OptionsSwingComponentSmoke() { }
}
