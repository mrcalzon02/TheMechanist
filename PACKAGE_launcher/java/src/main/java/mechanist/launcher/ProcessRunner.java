package mechanist.launcher;

import javax.swing.SwingUtilities;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

final class ProcessRunner {
    private ProcessRunner() {}

    static void runClient(Path clientJar, List<Path> supportJars, String javaCommand,
                          String mainClass, Consumer<String> output) throws IOException, InterruptedException {
        runClient(clientJar, supportJars, javaCommand, mainClass, null, output);
    }

    static void runClient(Path clientJar, List<Path> supportJars, String javaCommand,
                          String mainClass, Path userDataRoot, Consumer<String> output) throws IOException, InterruptedException {
        Path jar = clientJar.toAbsolutePath().normalize();
        ArrayList<String> classpath = new ArrayList<>();
        classpath.add(jar.toString());
        for (Path support : supportJars) classpath.add(support.toAbsolutePath().normalize().toString());
        ArrayList<String> command = new ArrayList<>();
        command.add(javaCommand);
        if (userDataRoot != null) command.add("-Dmechanist.storage.root=" + userDataRoot.toAbsolutePath().normalize());
        command.addAll(List.of("-cp", String.join(System.getProperty("path.separator"), classpath), mainClass));
        int exit = run(jar.getParent(), output, command.toArray(String[]::new));
        if (exit != 0) throw new IOException("Client process exited with code " + exit + ". See launch log.");
    }

    static int run(Path workingDirectory, Consumer<String> output, String... command) throws IOException, InterruptedException {
        List<String> args = new ArrayList<>(List.of(command));
        ProcessBuilder pb = new ProcessBuilder(args);
        if (workingDirectory != null) pb.directory(workingDirectory.toFile());
        pb.redirectErrorStream(true);
        Process process = pb.start();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String captured = line;
                SwingUtilities.invokeLater(() -> output.accept(captured));
            }
        }
        return process.waitFor();
    }
}
