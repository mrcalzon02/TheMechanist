package mechanist;

final class WorldTopologyAudit {
    private WorldTopologyAudit() {}

    static String validate(WorldTopologyContract.ZoneTransitionPlan plan) {
        if (plan == null) return "topology plan missing";
        StringBuilder out = new StringBuilder(plan.auditSummary());
        for (WorldTopologyContract.CardinalExit direction : WorldTopologyContract.CardinalExit.values()) {
            WorldTopologyContract.EdgeTransitionAnchor exit = plan.exit(direction);
            if (exit == null) out.append(" | missing ").append(direction);
            else if (!exit.isDoubleDoorCenteredInRoad()) out.append(" | off-center double door ").append(direction);
        }
        int elevatorCount = 0;
        int stairCount = 0;
        int manholeCount = 0;
        int drainCount = 0;
        for (WorldTopologyContract.VerticalTransitionAnchor anchor : plan.verticalTransitions()) {
            if (anchor.isElevator()) elevatorCount++;
            else if (anchor.isStair()) stairCount++;
            else if (anchor.kind() == WorldTopologyContract.VerticalTransitionKind.MANHOLE_DOWN_TO_SEWER) manholeCount++;
            else if (anchor.kind() == WorldTopologyContract.VerticalTransitionKind.DRAIN_DOWN_FROM_SEWER) drainCount++;
        }
        if (elevatorCount < 1) out.append(" | missing elevator anchor");
        if (stairCount < 2) out.append(" | missing paired stair anchors");
        if (manholeCount < 1) out.append(" | missing manhole anchor");
        if (drainCount < 1) out.append(" | missing drain anchor");
        return out.toString();
    }

    static void audit(long worldSeed, int zoneX, int zoneY, int floor, WorldTopologyContract.SectorSize size) {
        WorldTopologyContract.ZoneTransitionPlan plan = WorldTopologyContract.planFor(worldSeed, zoneX, zoneY, floor, size);
        DebugLog.audit("WORLD_TOPOLOGY_CONTRACT", validate(plan));
    }
}
