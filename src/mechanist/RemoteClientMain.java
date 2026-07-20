package mechanist;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Explicit packaged entry point for the limited-alpha independent-host lobby. */
public final class RemoteClientMain {
    public static void main(String[] args) {
        List<String> forwarded = new ArrayList<>();
        boolean modeDeclared = false;
        if (args != null) {
            forwarded.addAll(Arrays.asList(args));
            for (String arg : args) {
                if (arg != null && arg.trim().startsWith("--mode=")) {
                    modeDeclared = true;
                    break;
                }
            }
        }
        if (!modeDeclared) {
            forwarded.add(0, "--mode=remote-client");
        }
        TheMechanist.main(forwarded.toArray(String[]::new));
    }

    private RemoteClientMain() { }
}
