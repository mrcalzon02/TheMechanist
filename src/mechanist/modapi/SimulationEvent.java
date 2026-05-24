package mechanist.modapi;

/** Typed events emitted by mod integrations without requiring UI or server code to inspect raw strings. */
public sealed interface SimulationEvent permits SimulationEvent.AuditLog, SimulationEvent.NavigationVectorChanged, SimulationEvent.RoomAtmosphereChanged, SimulationEvent.FactionDiplomacyMutated, SimulationEvent.ItemStateChanged, SimulationEvent.ResearchUnlocked, SimulationEvent.LoreIndexed {
    record AuditLog(String source, String message, String instantUtc, long tick) implements SimulationEvent { }
    record NavigationVectorChanged(String sectorId, NavigationVector oldVector, NavigationVector newVector, String reason) implements SimulationEvent { }
    record RoomAtmosphereChanged(String roomId, double oldOxygen, double newOxygen, String reason) implements SimulationEvent { }
    record FactionDiplomacyMutated(String factionId, String targetFactionId, DiplomaticSignal signal, int oldAggression, int newAggression) implements SimulationEvent { }
    record ItemStateChanged(String itemId, int oldCharges, int newCharges, int oldDurability, int newDurability, String reason) implements SimulationEvent { }
    record ResearchUnlocked(String nodeId, String blueprintId, String reason) implements SimulationEvent { }
    record LoreIndexed(String entryId, String taxonomyPath, String searchTags) implements SimulationEvent { }
}
