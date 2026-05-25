package mechanist.server.admin;

import java.nio.file.Path;
import java.time.Instant;

public final class ServerRuntimeStatus {
    private final boolean clientsConnected;
    private final boolean savingActive;
    private final String activeWorld;
    private final Path activeWorldPath;
    private final int saveSlotCount;
    private final int backupCount;
    private final String hostStatus;
    private final String updateChannel;
    private final String adapterStatus;
    private final Instant sampledAt;

    public ServerRuntimeStatus(boolean clientsConnected, boolean savingActive, String activeWorld,
                               Path activeWorldPath, int saveSlotCount, int backupCount,
                               String hostStatus, String updateChannel, String adapterStatus) {
        this.clientsConnected = clientsConnected;
        this.savingActive = savingActive;
        this.activeWorld = activeWorld == null || activeWorld.isBlank() ? "<none>" : activeWorld;
        this.activeWorldPath = activeWorldPath;
        this.saveSlotCount = Math.max(0, saveSlotCount);
        this.backupCount = Math.max(0, backupCount);
        this.hostStatus = hostStatus == null || hostStatus.isBlank() ? "<unknown>" : hostStatus;
        this.updateChannel = updateChannel == null || updateChannel.isBlank() ? "main" : updateChannel;
        this.adapterStatus = adapterStatus == null || adapterStatus.isBlank() ? "<unknown>" : adapterStatus;
        this.sampledAt = Instant.now();
    }

    public boolean clientsConnected() { return clientsConnected; }
    public boolean savingActive() { return savingActive; }
    public String activeWorld() { return activeWorld; }
    public Path activeWorldPath() { return activeWorldPath; }
    public int saveSlotCount() { return saveSlotCount; }
    public int backupCount() { return backupCount; }
    public String hostStatus() { return hostStatus; }
    public String updateChannel() { return updateChannel; }
    public String adapterStatus() { return adapterStatus; }
    public Instant sampledAt() { return sampledAt; }

    public String summary() {
        return "clientsConnected=" + clientsConnected
                + " savingActive=" + savingActive
                + " activeWorld=" + activeWorld
                + " saveSlotCount=" + saveSlotCount
                + " backupCount=" + backupCount
                + " hostStatus=" + hostStatus
                + " channel=" + updateChannel
                + " adapter=" + adapterStatus;
    }
}
