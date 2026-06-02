package mechanist;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;

/** Aggregates the zero-trust server security authorities introduced for the headless runtime. */
final class SecureServerNetworkingCore implements AutoCloseable {
    final NetworkThrottlingManager throttlingManager;
    final ServerPerformanceMonitor.BackgroundIoGate backgroundIoGate;
    final ServerPerformanceMonitor performanceMonitor;
    final AuthoritativeAeadPacketValidator aeadPacketValidator;
    final AdminSecurityLogger adminSecurityLogger;
    final ScheduledExecutorService scheduler;
    final CharacterSaveManager characterSaveManager;
    final IntrusionDetectionEngine intrusionDetectionEngine;
    final InventoryTransactionGuard inventoryTransactionGuard;
    final ModPackageValidator modPackageValidator;
    final ServerDisasterRecoveryEngine disasterRecoveryEngine;

    private SecureServerNetworkingCore(NetworkThrottlingManager throttlingManager, ServerPerformanceMonitor.BackgroundIoGate backgroundIoGate, ServerPerformanceMonitor performanceMonitor, AuthoritativeAeadPacketValidator aeadPacketValidator, AdminSecurityLogger adminSecurityLogger, ScheduledExecutorService scheduler, CharacterSaveManager characterSaveManager, IntrusionDetectionEngine intrusionDetectionEngine, InventoryTransactionGuard inventoryTransactionGuard, ModPackageValidator modPackageValidator, ServerDisasterRecoveryEngine disasterRecoveryEngine) {
        this.throttlingManager = throttlingManager;
        this.backgroundIoGate = backgroundIoGate;
        this.performanceMonitor = performanceMonitor;
        this.aeadPacketValidator = aeadPacketValidator;
        this.adminSecurityLogger = adminSecurityLogger;
        this.scheduler = scheduler;
        this.characterSaveManager = characterSaveManager;
        this.intrusionDetectionEngine = intrusionDetectionEngine;
        this.inventoryTransactionGuard = inventoryTransactionGuard;
        this.modPackageValidator = modPackageValidator;
        this.disasterRecoveryEngine = disasterRecoveryEngine;
    }

    static SecureServerNetworkingCore initialize(Path serverRoot) throws IOException {
        Objects.requireNonNull(serverRoot, "serverRoot");
        ThreadFactory factory = r -> {
            Thread t = new Thread(r, "mechanist-secure-server-background");
            t.setDaemon(true);
            return t;
        };
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2, factory);
        NetworkThrottlingManager throttling = new NetworkThrottlingManager();
        ServerPerformanceMonitor.BackgroundIoGate gate = new ServerPerformanceMonitor.BackgroundIoGate();
        ServerPerformanceMonitor monitor = new ServerPerformanceMonitor(gate);
        monitor.start();
        AuthoritativeAeadPacketValidator validator = new AuthoritativeAeadPacketValidator();
        AdminSecurityLogger logger = new AdminSecurityLogger(serverRoot.resolve("admin-logs"));
        CharacterSaveManager characters = new CharacterSaveManager(serverRoot.resolve("profiles"));
        IntrusionDetectionEngine intrusion = new IntrusionDetectionEngine(logger);
        InventoryTransactionGuard inventoryGuard = new InventoryTransactionGuard(logger);
        ModPackageValidator modValidator = new ModPackageValidator(scheduler);
        ServerDisasterRecoveryEngine disasterRecovery = new ServerDisasterRecoveryEngine(serverRoot.resolve("admin-logs"), characters, reason -> DebugLog.warn("SERVER_NETWORK_PAUSE", reason), java.util.List::of, () -> "mod-api-templates-local");
        disasterRecovery.install();
        NettyResourceSafetyBridge.configureAdvancedLeakDetectionIfPresent();
        SecurityPathGuard.RuntimePrivilegeReport report = SecurityPathGuard.inspectRuntimePrivilege();
        DebugLog.audit("ZERO_TRUST_BOOT", "linuxRoot=" + report.linuxRoot() + " user=" + report.userName() + " note=" + report.recommendation());
        return new SecureServerNetworkingCore(throttling, gate, monitor, validator, logger, scheduler, characters, intrusion, inventoryGuard, modValidator, disasterRecovery);
    }

    @Override public void close() {
        try { disasterRecoveryEngine.close(); } catch (RuntimeException ignored) { }
        try { intrusionDetectionEngine.close(); } catch (RuntimeException ignored) { }
        try { characterSaveManager.close(); } catch (RuntimeException ignored) { }
        try { performanceMonitor.close(); } catch (RuntimeException ignored) { }
        scheduler.shutdownNow();
    }
}
