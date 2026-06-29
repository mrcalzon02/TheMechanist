package mechanist;

import java.util.ArrayList;
import java.util.List;

/** Audit-only queue admission contract for future faction construction execution. */
final class BlueprintFactionConstructionQueueAdmissionAuthority {
    record QueueAdmission(String jobId, String blueprintName, String factionName, String requestedState,
                          String admittedState, int requestedPriority, boolean queueSlotAvailable,
                          boolean handoffReady, boolean admissionReady, String blockerLine,
                          String boundaryLine) { }

    private BlueprintFactionConstructionQueueAdmissionAuthority() { }

    static List<QueueAdmission> sampleAdmissions() {
        List<BlueprintFactionConstructionExecutionHandoffAuthority.ExecutionHandoff> handoffs =
                BlueprintFactionConstructionExecutionHandoffAuthority.sampleHandoffs();
        ArrayList<QueueAdmission> rows = new ArrayList<>();
        rows.add(admissionFor(handoffs.get(0), 3, true));
        rows.add(admissionFor(handoffs.get(1), 5, true));
        rows.add(admissionFor(handoffs.get(2), 1, false));
        return List.copyOf(rows);
    }

    static QueueAdmission admissionFor(BlueprintFactionConstructionExecutionHandoffAuthority.ExecutionHandoff handoff,
                                       int requestedPriority, boolean queueSlotAvailable) {
        String id = handoff == null ? "job-unassigned" : clean(handoff.jobId(), "job-unassigned");
        String blueprint = handoff == null ? "Unknown blueprint" : clean(handoff.blueprintName(), "Unknown blueprint");
        String faction = handoff == null ? "Unaffiliated" : clean(handoff.factionName(), "Unaffiliated");
        String requestedState = handoff == null ? "REQUESTED" : clean(handoff.lifecycleState(), "REQUESTED");
        int priority = Math.max(0, Math.min(9, requestedPriority));
        boolean handoffReady = handoff != null && handoff.handoffReady();
        boolean slotReady = queueSlotAvailable;
        ArrayList<String> blockers = new ArrayList<>();
        if (!handoffReady) blockers.add("execution handoff blocked");
        if (!slotReady) blockers.add("queue slot unavailable");
        boolean ready = blockers.isEmpty();
        String admittedState = ready ? "RESERVED" : "BLOCKED";
        String blockerLine = ready ? "none" : String.join("; ", blockers);
        String boundary = id + " queue admission for " + blueprint
                + " by " + faction
                + " requested=" + requestedState
                + " admitted=" + admittedState
                + " priority=" + priority
                + "; queueSlot=" + slotReady
                + ", handoffReady=" + handoffReady
                + "; blockers=" + blockerLine
                + "; audit only, no live queue mutation.";
        return new QueueAdmission(id, blueprint, faction, requestedState, admittedState, priority, slotReady,
                handoffReady, ready, blockerLine, boundary);
    }

    static List<String> definitionAuditLines() {
        List<QueueAdmission> samples = sampleAdmissions();
        int ready = 0;
        int blocked = 0;
        int reserved = 0;
        int slotBlocked = 0;
        for (QueueAdmission admission : samples) {
            if (admission.admissionReady()) ready++;
            else blocked++;
            if ("RESERVED".equals(admission.admittedState())) reserved++;
            if (!admission.queueSlotAvailable()) slotBlocked++;
        }
        return List.of(
                "Blueprint faction construction queue admission audit: owner=BlueprintFactionConstructionQueueAdmissionAuthority, handoffOwner=BlueprintFactionConstructionExecutionHandoffAuthority, jobOwner=BlueprintFactionConstructionJobDefinitionAuthority, stagedConstructionOwner=ProgressiveConstructionAuthority, readiness=audit-only, ordinaryUiRawIds=false.",
                "Blueprint faction construction queue admission audit: future execution may reserve a job slot only after the execution handoff is ready and a queue slot is available; blocked admissions must remain readable instead of silently disappearing.",
                "Blueprint faction construction queue admission sample audit: sampleAdmissions=" + samples.size()
                        + ", ready=" + ready
                        + ", blocked=" + blocked
                        + ", reserved=" + reserved
                        + ", slotBlocked=" + slotBlocked + ".",
                "Blueprint faction construction queue admission examples: " + samples.get(0).boundaryLine()
                        + " | " + samples.get(1).boundaryLine()
                        + " | " + samples.get(2).boundaryLine(),
                "Blueprint faction construction queue admission rule: a future execution owner must keep queue admission separate from material removal, worker dispatch, site reservation, heat mutation, and staged-construction placement.",
                "Blueprint faction construction queue admission boundary: this audit does not create a live job queue, reserve a live queue slot, reserve workers, reserve sites, remove materials, spend budget, mutate heat, mutate suspicion, place staged construction, advance labor, cancel jobs, or complete construction.",
                "Guard: Milestone03BlueprintFactionConstructionQueueAdmissionAuditSmoke checks queue admission readiness, reserved-state projection, slot blockers, handoff blockers, future execution boundaries, and raw-ID hiding."
        );
    }

    private static String clean(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
