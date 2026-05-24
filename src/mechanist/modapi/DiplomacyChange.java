package mechanist.modapi;

import java.util.Objects;

public record DiplomacyChange(String sourceFactionId, String targetFactionId, DiplomaticSignal signal, int severity) {
    public DiplomacyChange {
        sourceFactionId = SimulationContext.cleanId(sourceFactionId);
        targetFactionId = SimulationContext.cleanId(targetFactionId);
        signal = Objects.requireNonNullElse(signal, DiplomaticSignal.UNKNOWN);
        severity = Math.max(0, Math.min(100, severity));
    }
}
