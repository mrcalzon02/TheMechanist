package mechanist;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Locale;

/** Verifies remote-client startup without constructing Swing or opening a socket. */
final class RemoteClientStartupSmoke {
    public static void main(String[] args) throws Exception {
        Path root = Files.createTempDirectory(
                "Mechanist Remote Client Profile With Spaces ");
        String previous = System.getProperty("mechanist.client.storage.root");
        System.setProperty("mechanist.client.storage.root", root.toString());
        try {
            RuntimeProfile remote = RuntimeProfile.fromArgs(new String[] {
                    "--mode=remote-client",
                    "--host=127.0.0.1",
                    "--port=25565",
                    "--server-key=limited-alpha/primary-host",
                    "--profile=alpha.tester.0001",
                    "--internal-server"
            });
            require(remote.requestedMode == ApplicationRuntimeMode.REMOTE_CLIENT,
                    "remote mode was not retained as the requested runtime");
            require(remote.effectiveMode == ApplicationRuntimeMode.REMOTE_CLIENT,
                    "remote mode was not selected as the effective runtime");
            require(remote.remoteClientMode(),
                    "remote profile did not report remote-client mode");
            require("127.0.0.1".equals(remote.remoteHost)
                            && remote.remotePort == 25565,
                    "remote endpoint settings changed during parsing");
            require("limited-alpha/primary-host".equals(remote.remoteServerKey),
                    "remote server key changed during parsing");
            require("alpha.tester.0001".equals(remote.profileName),
                    "remote profile identity changed during parsing");
            require(!remote.internalServerRequested,
                    "remote-client mode must not mount the single-player internal host");
            require(TheMechanist.remoteClientEntryRequested(remote),
                    "desktop entry did not route remote-client mode to the lobby");
            require(!TheMechanist.remoteClientEntryRequested(
                            RuntimeProfile.defaultProfile()),
                    "ordinary client startup was incorrectly routed to the remote lobby");

            String audit = IndependentHostLobbyWindow.auditSummary(remote);
            require(audit.contains("gamePanelMounted=false")
                            && audit.contains("internalHostMounted=false")
                            && audit.contains("worldCommandApi=false")
                            && audit.contains("worldAuthority=false"),
                    "remote lobby audit overclaimed a world or local-host boundary");
            require(audit.contains("hostedCommands=ready,presence,chat-state")
                            && audit.contains("relayConsole=true")
                            && audit.contains("pendingConnectionCancellable=true")
                            && audit.contains("failedSessionTeardown=single-transition"),
                    "remote lobby audit omitted certified lifecycle or player controls");

            Path expectedRoot = root.toAbsolutePath().normalize();
            require(ClientMutableStorageAuthority.root().equals(expectedRoot),
                    "client storage override was not authoritative");
            Path tokenRoot = ClientMutableStorageAuthority.resumeTokenRoot();
            IndependentHostResumeTokenStore tokenStore =
                    new IndependentHostResumeTokenStore(tokenRoot);
            require(Files.isDirectory(tokenRoot),
                    "remote token custody directory was not created in mutable storage");
            require(tokenStore.statusLine().contains("diagnosticsIncludeToken=false"),
                    "token-store status does not guarantee credential redaction");
            require(!ClientMutableStorageAuthority.auditSummary()
                            .toLowerCase(Locale.ROOT)
                            .contains("resumetoken="),
                    "client storage audit exposed a credential value");

            for (Field field : IndependentHostLobbyWindow.class.getDeclaredFields()) {
                String type = field.getType().getName();
                require(!type.endsWith("GamePanel")
                                && !type.endsWith("SinglePlayerInternalHostSupervisor")
                                && !type.endsWith("SinglePlayerSectorRuntimeBridge"),
                        "remote lobby mounted a local world authority field: "
                                + field.getName());
            }
            for (Method method : IndependentHostLobbyWindow.class.getDeclaredMethods()) {
                String name = method.getName().toLowerCase(Locale.ROOT);
                require(!name.contains("move")
                                && !name.contains("combat")
                                && !name.contains("inventory")
                                && !name.contains("worldcommand")
                                && !name.contains("worldsnapshot"),
                        "remote lobby exposed unfinished world API: "
                                + method.getName());
            }

            expectFailure(
                    () -> RuntimeProfile.fromArgs(new String[] {
                            "--mode=remote-client",
                            "--port=80",
                            "--profile=alpha.tester.0001"
                    }),
                    "remote port");
            expectFailure(
                    () -> RuntimeProfile.fromArgs(new String[] {
                            "--mode=remote-client",
                            "--port=not-a-number",
                            "--profile=alpha.tester.0001"
                    }),
                    "remote port");

            System.out.println("RemoteClientStartupSmoke PASS"
                    + " explicitMode=true"
                    + " editableEndpointDefaults=true"
                    + " mutableStorageOutsideInstall=true"
                    + " tokenDiagnosticsRedacted=true"
                    + " internalHostMounted=false"
                    + " gamePanelMounted=false"
                    + " hostedLobbyControls=true"
                    + " relayConsole=true"
                    + " pendingConnectionCancellable=true"
                    + " failedSessionTeardownSingleTransition=true"
                    + " invalidPortRejected=true"
                    + " worldCommandApi=false"
                    + " worldAuthority=false");
        } finally {
            if (previous == null) {
                System.clearProperty("mechanist.client.storage.root");
            } else {
                System.setProperty("mechanist.client.storage.root", previous);
            }
            deleteRecursively(root);
        }
    }

    private static void expectFailure(
            ThrowingAction action,
            String expectedText
    ) throws Exception {
        Throwable failure = null;
        try {
            action.run();
        } catch (Throwable expected) {
            failure = expected;
        }
        require(failure != null,
                "expected failure containing: " + expectedText);
        String message = failure.getMessage() == null
                ? ""
                : failure.getMessage();
        require(message.toLowerCase(Locale.ROOT).contains(
                        expectedText.toLowerCase(Locale.ROOT)),
                "unexpected failure: " + message);
    }

    private static void deleteRecursively(Path root) {
        if (root == null || !Files.exists(root)) return;
        try (var paths = Files.walk(root)) {
            paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (Exception ignored) {
                }
            });
        } catch (Exception ignored) {
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    @FunctionalInterface
    private interface ThrowingAction {
        void run() throws Exception;
    }

    private RemoteClientStartupSmoke() { }
}
