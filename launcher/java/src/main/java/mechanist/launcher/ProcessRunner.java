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
