package mechanist;

import java.util.ArrayList;
import java.util.List;

/** Explicit packaged entry point for the limited-alpha independent-host lobby. */
public final class RemoteClientMain {
    public static void main(String[] args) {
        List<String> forwarded = new ArrayList<>();
        forwarded.add("--mode=remote-client");
        if (args != null) {
            for (String arg : args) {
                if (arg == null || arg.trim().startsWith("--mode=")) continue;
                forwarded.add(arg);
            }
        }
        TheMechanist.main(forwarded.toArray(String[]::new));
    }

    private RemoteClientMain() { }
}
