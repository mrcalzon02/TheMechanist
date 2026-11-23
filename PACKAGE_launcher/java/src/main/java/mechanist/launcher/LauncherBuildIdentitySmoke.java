package mechanist.launcher;

import java.nio.file.Files;
import java.nio.file.Path;

/** Bounded smoke for offline installed version and source-commit readback. */
final class LauncherBuildIdentitySmoke {
    public static void main(String[] args) throws Exception {
        String commit = "b8758b49e7d04a1cb86c01ff1df8e7b0f3b41c08";
        Path install = Files.createTempDirectory("mechanist-launcher-identity-");
        String previous = System.getProperty("mechanist.launcher.installRoot");
        try {
            System.setProperty("mechanist.launcher.installRoot", install.toString());
            Path dir = Files.createDirectories(install.resolve("manifests"));
            Path manifest = dir.resolve("runtime-manifest.json");
            Files.writeString(manifest, "{\"version\":\"0.9.54\",\"commit\":\"" + commit
                    + "\",\"platform\":\"windows-x64\",\"javaRelease\":17,\"releaseHardened\":true}");
            LauncherBuildIdentity.Identity identity = LauncherBuildIdentity.read(LauncherConfig.defaults());
            require("0.9.54".equals(identity.version()), "version is missing from installed manifest");
            require(commit.equals(identity.commit()), "full source commit was lost");
            require("v0.9.54 / b8758b49e7d0".equals(identity.compactLabel()),
                    "launcher compact status must distinguish releases");
            require(identity.compactLine().contains(commit), "runtime diagnostics must show full SHA");

            Files.writeString(manifest, "{\"version\":\"0.9.54\",\"commit\":\"unknown\"}");
            LauncherBuildIdentity.Identity unknown = LauncherBuildIdentity.read(LauncherConfig.defaults());
            require(unknown.compactLabel().contains("commit unknown"),
                    "missing or invalid SHA must not masquerade as a release fingerprint");
            Files.deleteIfExists(manifest);
            requireNotInstalled(LauncherConfig.defaults());
        } finally {
            if (previous == null) System.clearProperty("mechanist.launcher.installRoot");
            else System.setProperty("mechanist.launcher.installRoot", previous);
            Files.deleteIfExists(install.resolve("manifests/runtime-manifest.json"));
            Files.deleteIfExists(install.resolve("manifests"));
            Files.deleteIfExists(install);
        }
        verifyClientProcessHandoff();
        System.out.println("LauncherBuildIdentitySmoke PASS");
    }

    private static void verifyClientProcessHandoff() throws Exception {
        Path root = Files.createTempDirectory("Mechanist client launch with spaces ");
        try {
            Path client = Files.createDirectories(root.resolve("packages/client"));
            Path jar = client.resolve("TheMechanist.jar");
            Path support = Files.createDirectories(root.resolve("packages/support/lib")).resolve("support.jar");
            writeProbeJar(jar, ClientProbe.class);
            writeProbeJar(support, SupportProbe.class);
            String executable = System.getProperty("os.name", "").toLowerCase(java.util.Locale.ROOT)
                    .contains("win") ? "java.exe" : "java";
            String javaCommand = Path.of(System.getProperty("java.home"), "bin", executable).toString();
            java.util.ArrayList<String> output = new java.util.ArrayList<>();
            ProcessRunner.runClient(jar, java.util.List.of(support), javaCommand,
                    ClientProbe.class.getName(), output::add);
            javax.swing.SwingUtilities.invokeAndWait(() -> {});
            require(output.contains("cwd=" + client.toRealPath()), "client working directory is not the package directory");
            require(output.contains("support=present"), "client lost its support classpath");
            Files.createFile(client.resolve("fail"));
            try {
                ProcessRunner.runClient(jar, java.util.List.of(support), javaCommand,
                        ClientProbe.class.getName(), output::add);
                throw new AssertionError("client nonzero exit was silently accepted");
            } catch (java.io.IOException expected) {
                require(expected.getMessage().contains("code 23"), "wrong client failure: " + expected);
            }
            javax.swing.SwingUtilities.invokeAndWait(() -> {});
        } finally {
            try (var files = Files.walk(root)) {
                for (Path path : files.sorted(java.util.Comparator.reverseOrder()).toList()) {
                    Files.deleteIfExists(path);
                }
            }
        }
    }

    private static void writeProbeJar(Path jar, Class<?> probe) throws Exception {
        String entry = probe.getName().replace('.', '/') + ".class";
        try (var out = new java.util.jar.JarOutputStream(Files.newOutputStream(jar));
             var in = probe.getResourceAsStream("/" + entry)) {
            if (in == null) throw new java.io.IOException("missing launch probe class: " + entry);
            out.putNextEntry(new java.util.jar.JarEntry(entry));
            in.transferTo(out);
            out.closeEntry();
        }
    }

    public static final class ClientProbe {
        public static void main(String[] args) throws Exception {
            System.out.println("cwd=" + Path.of(".").toRealPath());
            Class.forName("mechanist.launcher.LauncherBuildIdentitySmoke$SupportProbe");
            System.out.println("support=present");
            if (Files.exists(Path.of("fail"))) System.exit(23);
        }
    }

    public static final class SupportProbe {}

    private static void requireNotInstalled(LauncherConfig config) throws Exception {
        try {
            LauncherBuildIdentity.read(config);
            throw new AssertionError("missing manifest must not report an installed release");
        } catch (java.io.IOException expected) {
            // No installed runtime, not a silently accepted fake version.
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
