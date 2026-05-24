package mechanist;

import javax.swing.SwingUtilities;
import java.awt.Component;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

/** Orchestrates the irreversible local single-player-to-hosted-multiplayer conversion path. */
final class IntegratedLocalMultiplayerHost implements AutoCloseable {
    private final WorldSimulationClock clock;
    private final NatTraversalManager natTraversalManager;
    private final LocalHostAuthGate authGate;
    private final AtomicBoolean locked = new AtomicBoolean(false);
    private volatile HostBindingResult binding;
    private volatile NatDiscoveryResult natProfile;

    IntegratedLocalMultiplayerHost(WorldSimulationClock clock, NatTraversalManager natTraversalManager, LocalHostAuthGate authGate) {
        this.clock = Objects.requireNonNull(clock, "clock");
        this.natTraversalManager = Objects.requireNonNull(natTraversalManager, "natTraversalManager");
        this.authGate = Objects.requireNonNull(authGate, "authGate");
    }

    CompletableFuture<LocalHostOpenResult> open(Component parent,
                                                Path singlePlayerSave,
                                                PlayerIdentity hostIdentity,
                                                LocalWorldSaveStateSeparator separator,
                                                long seed,
                                                String worldName,
                                                String worldId,
                                                WorldSetupSettings settings,
                                                int maxPlayers,
                                                boolean preferSteam) {
        if (locked.get()) return CompletableFuture.completedFuture(new LocalHostOpenResult(false, null, binding, natProfile, "already locked as multiplayer session"));
        boolean confirmed = MultiplayerConversionWarningDialog.confirm(parent);
        if (!confirmed) return CompletableFuture.completedFuture(new LocalHostOpenResult(false, null, null, null, "host cancelled conversion warning"));
        locked.set(true);
        return CompletableFuture.supplyAsync(() -> {
            try {
                LocalWorldConversionManifest manifest = separator.splitSinglePlayerProfile(singlePlayerSave, hostIdentity);
                ClockTransitionResult clockResult = clock.openToMultiplayer(20);
                int port = NetworkPortAuthority.firstAvailableGamePort();
                ServerConfig config = MultiplayerHostBindingService.configFromWorld(seed, worldName, worldId, settings, maxPlayers, port, preferSteam);
                binding = MultiplayerHostBindingService.bind(config);
                natProfile = natTraversalManager.discover(port, Duration.ofSeconds(3));
                return new LocalHostOpenResult(binding.success(), manifest, binding, natProfile, clockResult.message() + "; " + binding.compactLine());
            } catch (SaveConversionException ex) {
                locked.set(false);
                DebugLog.error("LOCAL_HOST_CONVERSION", "Save split failed; local world remains single-player.", ex);
                return new LocalHostOpenResult(false, null, null, null, ex.getMessage());
            } catch (RuntimeException ex) {
                locked.set(false);
                DebugLog.error("LOCAL_HOST_OPEN", "Could not open local world to multiplayer.", ex);
                return new LocalHostOpenResult(false, null, null, null, ex.getClass().getSimpleName() + ": " + ex.getMessage());
            }
        });
    }

    boolean isMultiplayerSessionLocked() { return locked.get(); }
    LocalHostAuthGate authGate() { return authGate; }
    NatDiscoveryResult natProfile() { return natProfile; }
    HostBindingResult binding() { return binding; }

    @Override public void close() throws Exception {
        if (binding != null) binding.close();
        natTraversalManager.close();
    }
}

record LocalHostOpenResult(boolean success, LocalWorldConversionManifest conversionManifest, HostBindingResult binding, NatDiscoveryResult natProfile, String message) { }
