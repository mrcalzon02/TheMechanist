package mechanist;

import java.util.Map;

/** Reports systemd socket-activation environment state without pretending Java 17 can adopt fd=3 natively. */
final class SystemdSocketActivationBridge {
    record ActivationReport(boolean listenFdsPresent, int listenFdCount, String pidValue, boolean nativeFdAdoptionSupported, String message) { }

    private SystemdSocketActivationBridge() { }

    static ActivationReport inspect(Map<String, String> env) {
        Map<String, String> use = env == null ? System.getenv() : env;
        int count = parseInt(use.get("LISTEN_FDS"), 0);
        String pid = use.getOrDefault("LISTEN_PID", "");
        boolean present = count > 0;
        String message = present
                ? "systemd socket activation variables are present, but Java 17 standard APIs cannot safely wrap inherited listening fd=3 without native/internal bindings; service should use direct binding."
                : "systemd socket activation variables are absent; use direct Java binding or a native fd-adoption adapter.";
        return new ActivationReport(present, count, pid, false, message);
    }

    private static int parseInt(String raw, int fallback) { try { return Integer.parseInt(raw == null ? "" : raw.trim()); } catch (RuntimeException ex) { return fallback; } }
}
