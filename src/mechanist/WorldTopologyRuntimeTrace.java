package mechanist;

final class WorldTopologyRuntimeTrace {
    private WorldTopologyRuntimeTrace() {}

    static void auditSetupPlan(long seed, Object setup, int zoneX, int zoneY, int floor, String source) {
        try {
            WorldTopologyContract.ZoneTransitionPlan plan = WorldTopologySettingsBridge.planForSetup(seed, setup, zoneX, zoneY, floor);
            DebugLog.audit("WORLD_TOPOLOGY_CONTRACT", "source=" + safe(source) + " " + WorldTopologyAudit.validate(plan));
            WorldTopologyPreplacementPlan.audit(plan, source);
        } catch (Throwable t) {
            DebugLog.warn("WORLD_TOPOLOGY_CONTRACT", "Could not audit topology plan from " + safe(source) + ": " + t.getMessage());
        }
    }

    static WorldTopologyContract.ZoneTransitionPlan planFor(long seed, Object setup, int zoneX, int zoneY, int floor, String source) {
        WorldTopologyContract.ZoneTransitionPlan plan = WorldTopologySettingsBridge.planForSetup(seed, setup, zoneX, zoneY, floor);
        DebugLog.audit("WORLD_TOPOLOGY_CONTRACT", "source=" + safe(source) + " " + WorldTopologyAudit.validate(plan));
        WorldTopologyPreplacementPlan.audit(plan, source);
        return plan;
    }

    static WorldTopologyPreplacementPlan.Plan preplacementPlanFor(long seed, Object setup, int zoneX, int zoneY, int floor, String source) {
        WorldTopologyContract.ZoneTransitionPlan topology = planFor(seed, setup, zoneX, zoneY, floor, source);
        return WorldTopologyPreplacementPlan.fromTopology(topology);
    }

    private static String safe(String value) {
        return value == null || value.isBlank() ? "unspecified" : value.replace('\n', ' ').trim();
    }
}
