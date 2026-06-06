package mechanist;

import javax.swing.SwingUtilities;
import java.lang.reflect.*;

/** Background polling engine. Uses reflection so javac builds still pass without Jamepad on the classpath. */
final class GamepadInputEngine implements Runnable {
    static final String VERSION = "0.9.10kb";
    private static final int POLL_SLEEP_MS = 16;

    private final InputRegistry registry;
    private final GenericControllerSchema schema = new GenericControllerSchema();
    private final ControllerConnectionStateTracker connectionTracker = new ControllerConnectionStateTracker();
    private volatile boolean running = true;
    private volatile String status = "not started";
    private volatile String playerFacingConnectionNotice = "No controller detected. Keyboard and mouse fallback remain active.";
    private OptionalJamepadBackend backend;

    GamepadInputEngine(InputRegistry registry) {
        this.registry = registry;
    }

    @Override public void run() {
        backend = new OptionalJamepadBackend();
        if (!backend.init()) {
            status = "Jamepad not available; keyboard remains active; add Maven/JitPack dependency for hardware polling.";
            playerFacingConnectionNotice = "No controller driver is available. Keyboard and mouse fallback remain active.";
            registry.clearSource(InputSource.GAMEPAD);
            connectionTracker.clear();
            return;
        }
        status = "Jamepad initialized; polling controllers.";
        try {
            while (running) {
                GamepadControllerSnapshot snapshot = backend.pollFirstConnected();
                ControllerConnectionStateTracker.ConnectionRead connection = connectionTracker.update(snapshot, System.currentTimeMillis());
                playerFacingConnectionNotice = connection.playerFacingNotice();
                if (connection.transition()) status = connection.playerFacingNotice();
                SwingUtilities.invokeLater(() -> schema.apply(snapshot, registry));
                try { Thread.sleep(POLL_SLEEP_MS); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); break; }
            }
        } finally {
            registry.clearSource(InputSource.GAMEPAD);
            backend.shutdown();
            status = "stopped";
            playerFacingConnectionNotice = "Controller polling stopped. Keyboard and mouse fallback remain active.";
        }
    }

    void stop() {
        running = false;
    }

    String status() { return status; }

    String playerFacingConnectionNotice() { return playerFacingConnectionNotice; }

    ControllerConnectionStateTracker.ConnectionRead inspectConnection(GamepadControllerSnapshot snapshot, long nowMs) {
        ControllerConnectionStateTracker.ConnectionRead read = connectionTracker.update(snapshot, nowMs);
        playerFacingConnectionNotice = read.playerFacingNotice();
        return read;
    }

    static String auditSummary() {
        return "gamepadInputEngine version=" + VERSION
                + " thread=background pollingHz=~60 swingUpdates=invokeLater optionalJamepad=reflection gracefulStop=true"
                + " connectionNotices=" + ControllerConnectionStateTracker.auditSummary();
    }

    private static final class OptionalJamepadBackend {
        private Object manager;
        private Class<?> buttonClass;
        private Class<?> axisClass;
        private boolean available = false;

        boolean init() {
            try {
                Class<?> managerClass = Class.forName("com.studiohartman.jamepad.ControllerManager");
                buttonClass = Class.forName("com.studiohartman.jamepad.ControllerButton");
                axisClass = Class.forName("com.studiohartman.jamepad.ControllerAxis");
                manager = managerClass.getDeclaredConstructor().newInstance();
                invokeNoArg(manager, "initSDLGamepad");
                available = true;
                return true;
            } catch (Throwable t) {
                available = false;
                return false;
            }
        }

        GamepadControllerSnapshot pollFirstConnected() {
            if (!available || manager == null) return GamepadControllerSnapshot.disconnected();
            try {
                invokeNoArg(manager, "update");
                Method getControllerIndex = manager.getClass().getMethod("getControllerIndex", int.class);
                for (int i = 0; i < 8; i++) {
                    Object index = getControllerIndex.invoke(manager, i);
                    if (index == null || !bool(index, "isConnected")) continue;
                    String name = stringValue(index, "getName", "controller " + i);
                    return GamepadControllerSnapshot.of(name,
                            button(index, "A"), button(index, "B"), button(index, "X"), button(index, "Y"),
                            button(index, "DPAD_UP"), button(index, "DPAD_DOWN"), button(index, "DPAD_LEFT"), button(index, "DPAD_RIGHT"),
                            buttonEither(index, "START", "RIGHTSTICK"), buttonEither(index, "BACK", "GUIDE"),
                            buttonEither(index, "LEFTBUMPER", "LEFT_BUMPER"), buttonEither(index, "RIGHTBUMPER", "RIGHT_BUMPER"),
                            axis(index, "LEFTX"), axis(index, "LEFTY"), axis(index, "RIGHTX"), axis(index, "RIGHTY"));
                }
            } catch (Throwable ignored) { }
            return GamepadControllerSnapshot.disconnected();
        }

        void shutdown() {
            try { if (manager != null) invokeNoArg(manager, "quitSDLGamepad"); } catch (Throwable ignored) { }
        }

        private static Object invokeNoArg(Object target, String name) throws Exception {
            Method m = target.getClass().getMethod(name);
            return m.invoke(target);
        }

        private static boolean bool(Object target, String name) {
            try { return Boolean.TRUE.equals(invokeNoArg(target, name)); } catch (Throwable t) { return false; }
        }

        private static String stringValue(Object target, String name, String fallback) {
            try { Object v = invokeNoArg(target, name); return v == null ? fallback : String.valueOf(v); } catch (Throwable t) { return fallback; }
        }

        private boolean buttonEither(Object controller, String a, String b) { return button(controller, a) || button(controller, b); }

        private boolean button(Object controller, String enumName) {
            try {
                @SuppressWarnings({"unchecked", "rawtypes"})
                Object button = Enum.valueOf((Class<Enum>)buttonClass.asSubclass(Enum.class), enumName);
                Method m = controller.getClass().getMethod("isButtonPressed", buttonClass);
                return Boolean.TRUE.equals(m.invoke(controller, button));
            } catch (Throwable t) { return false; }
        }

        private float axis(Object controller, String enumName) {
            try {
                @SuppressWarnings({"unchecked", "rawtypes"})
                Object axis = Enum.valueOf((Class<Enum>)axisClass.asSubclass(Enum.class), enumName);
                Method m = controller.getClass().getMethod("getAxisState", axisClass);
                Object v = m.invoke(controller, axis);
                if (v instanceof Number) return Math.max(-1f, Math.min(1f, ((Number)v).floatValue()));
            } catch (Throwable t) { }
            return 0f;
        }
    }
}
