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
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private OptionsSwingComponentSmoke() { }
}
