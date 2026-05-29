package mechanist;

public class LayerB {
    public LayerB() {
    }

    static void changeSfxVolume(GamePanel panel, int delta) {
        panel.logEvent(OptionsBoundaryAuthority.changeSfxVolume(panel.options, delta));
        panel.sounds.play("button", panel.options);
        panel.repaint();
    }

    static void changeMusicVolume(GamePanel panel, int delta) {
        panel.logEvent(OptionsBoundaryAuthority.changeMusicVolume(panel.options, delta));
        panel.sounds.setMusicVolume(panel.options);
        panel.repaint();
    }

    static void changeConversationVolume(GamePanel panel, int delta) {
        panel.logEvent(OptionsBoundaryAuthority.changeConversationVolume(panel.options, delta));
        panel.sounds.play("type", panel.options);
        panel.repaint();
    }
}
