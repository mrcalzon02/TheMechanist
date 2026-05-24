package mechanist;

final class GamepadControllerSnapshot {
    final boolean connected;
    final String name;
    final boolean a, b, x, y;
    final boolean dpadUp, dpadDown, dpadLeft, dpadRight;
    final boolean start, back, leftBumper, rightBumper;
    final float leftX, leftY, rightX, rightY;

    private GamepadControllerSnapshot(boolean connected, String name, boolean a, boolean b, boolean x, boolean y,
                                      boolean dpadUp, boolean dpadDown, boolean dpadLeft, boolean dpadRight,
                                      boolean start, boolean back, boolean leftBumper, boolean rightBumper,
                                      float leftX, float leftY, float rightX, float rightY) {
        this.connected = connected;
        this.name = name == null ? "generic controller" : name;
        this.a = a;
        this.b = b;
        this.x = x;
        this.y = y;
        this.dpadUp = dpadUp;
        this.dpadDown = dpadDown;
        this.dpadLeft = dpadLeft;
        this.dpadRight = dpadRight;
        this.start = start;
        this.back = back;
        this.leftBumper = leftBumper;
        this.rightBumper = rightBumper;
        this.leftX = leftX;
        this.leftY = leftY;
        this.rightX = rightX;
        this.rightY = rightY;
    }

    static GamepadControllerSnapshot disconnected() {
        return new GamepadControllerSnapshot(false, "no controller", false, false, false, false,
                false, false, false, false, false, false, false, false, 0, 0, 0, 0);
    }

    static GamepadControllerSnapshot of(String name, boolean a, boolean b, boolean x, boolean y,
                                        boolean dpadUp, boolean dpadDown, boolean dpadLeft, boolean dpadRight,
                                        boolean start, boolean back, boolean leftBumper, boolean rightBumper,
                                        float leftX, float leftY, float rightX, float rightY) {
        return new GamepadControllerSnapshot(true, name, a, b, x, y, dpadUp, dpadDown, dpadLeft, dpadRight,
                start, back, leftBumper, rightBumper, leftX, leftY, rightX, rightY);
    }
}
