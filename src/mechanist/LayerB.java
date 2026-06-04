package mechanist;

/** @deprecated Use AudioVolumeOptionsRuntime. */
@Deprecated
public class LayerB {
    public LayerB() {}

    static void changeSfxVolume(GamePanel panel, int delta) { AudioVolumeOptionsRuntime.changeSfxVolume(panel, delta); }
    static void changeMusicVolume(GamePanel panel, int delta) { AudioVolumeOptionsRuntime.changeMusicVolume(panel, delta); }
    static void changeConversationVolume(GamePanel panel, int delta) { AudioVolumeOptionsRuntime.changeConversationVolume(panel, delta); }
}
