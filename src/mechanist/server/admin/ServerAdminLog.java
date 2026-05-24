package mechanist.server.admin;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ServerAdminLog {
    private final Path logFile;
    private final int maxMemoryEvents;
    private final ArrayList<ServerAdminEvent> recent = new ArrayList<>();

    public ServerAdminLog(Path logFile) {
        this(logFile, 500);
    }

    public ServerAdminLog(Path logFile, int maxMemoryEvents) {
        this.logFile = logFile;
        this.maxMemoryEvents = Math.max(50, maxMemoryEvents);
    }

    public synchronized void record(ServerAdminEvent event) {
        recent.add(event);
        while (recent.size() > maxMemoryEvents) recent.remove(0);
        try {
            if (logFile.getParent() != null) Files.createDirectories(logFile.getParent());
            Files.writeString(logFile, event.toLogLine() + System.lineSeparator(), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException ignored) {
            // Admin log writes must not crash the server authority path.
        }
    }

    public synchronized List<ServerAdminEvent> recentEvents() {
        return Collections.unmodifiableList(new ArrayList<>(recent));
    }

    public Path logFile() {
        return logFile;
    }
}
