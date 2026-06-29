package mechanist;

import java.util.ArrayList;
import java.util.List;

/** Audit-only job closeout contract for future faction staged construction jobs. */
final class BlueprintFactionConstructionJobCloseoutAuthority {
    record JobCloseout(String jobId, String blueprintName, String factionName, String closeoutState,
                       boolean completionReady, boolean siteStatusReadable, boolean crewReleaseReady,
                       boolean materialLedgerClosed, boolean budgetCloseoutReady, boolean attentionCloseoutReady,
                       boolean jobRecordReady, boolean closeoutReady, String blockerLine, String boundaryLine) { }

    private BlueprintFactionConstructionJobCloseoutAuthority() { }

    static List<JobCloseout> sampleCloseouts() {
        List<BlueprintFactionConstructionCompletionReadinessAuthority.CompletionReadiness> completions =
                BlueprintFactionConstructionCompletionReadinessAuthority.sampleCompletions();
        ArrayList<JobCloseout> rows = new ArrayList<>();
        rows.add(closeoutFor(completions.get(0), true, true, true, true, true, true));
        rows.add(closeoutFor(completions.get(1), true, true, true, true, true, true));
        rows.add(closeoutFor(completions.get(2), true, false, true, false, false, false));
        return List.copyOf(rows);
    }

    static JobCloseout closeoutFor(BlueprintFactionConstructionCompletionReadinessAuthority.CompletionReadiness completion,
                                   boolean siteStatusReadable, boolean crewReleaseReady,
                                   boolean materialLedgerClosed, boolean budgetCloseoutReady,
                                   boolean attentionCloseoutReady, boolean jobRecordReady) {
        String id = completion == null ? "job-unassigned" : clean(completion.jobId(), "job-unassigned");
        String blueprint = completion == null ? "Unknown blueprint" : clean(completion.blueprintName(), "Unknown blueprint");
        String faction = completion == null ? "Unaffiliated" : clean(completion.factionName(), "Unaffiliated");
        boolean readyCompletion = completion != null && completion.completionReady();
        ArrayList<String> blockers = new ArrayList<>();
        if (!readyCompletion) blockers.add("completion readiness blocked");
        if (!siteStatusReadable) blockers.add("site status not readable");
        if (!crewReleaseReady) blockers.add("crew release not ready");
        if (!materialLedgerClosed) blockers.add("material ledger not closed");
        if (!budgetCloseoutReady) blockers.add("budget closeout not ready");
        if (!attentionCloseoutReady) blockers.add("attention closeout not ready");
        if (!jobRecordReady) blockers.add("job record not ready");
        boolean ready = blockers.isEmpty();
        String state = ready ? "JOB_CLOSEOUT_READY" : "JOB_CLOSEOUT_BLOCKED";
        String blockerLine = ready ? "none" : String.join("; ", blockers);
        String boundary = id + " job closeout for " + blueprint
                + " by " + faction
                + " state=" + state
                + "; completionReady=" + readyCompletion
                + ", siteStatusReadable=" + siteStatusReadable
                + ", crewReleaseReady=" + crewReleaseReady
                + ", materialLedgerClosed=" + materialLedgerClosed
                + ", budgetCloseoutReady=" + budgetCloseoutReady
                + ", attentionCloseoutReady=" + attentionCloseoutReady
                + ", jobRecordReady=" + jobRecordReady
                + "; blockers=" + blockerLine
                + "; audit only, no closeout mutation.";
        return new JobCloseout(id, blueprint, faction, state, readyCompletion, siteStatusReadable,
                crewReleaseReady, materialLedgerClosed, budgetCloseoutReady, attentionCloseoutReady,
                jobRecordReady, ready, blockerLine, boundary);
    }

    static List<String> definitionAuditLines() {
        List<JobCloseout> samples = sampleCloseouts();
        int ready = 0;
        int blocked = 0;
        int completionBlocked = 0;
        int crewBlocked = 0;
        int attentionBlocked = 0;
        for (JobCloseout closeout : samples) {
            if (closeout.closeoutReady()) ready++;
            else blocked++;
            if (!closeout.completionReady()) completionBlocked++;
            if (!closeout.crewReleaseReady()) crewBlocked++;
            if (!closeout.attentionCloseoutReady()) attentionBlocked++;
        }
        return List.of(
                "Blueprint faction construction job closeout audit: owner=BlueprintFactionConstructionJobCloseoutAuthority, completionOwner=BlueprintFactionConstructionCompletionReadinessAuthority, releaseOwner=BlueprintFactionConstructionCancellationReleaseAuthority, budgetOwner=BlueprintFactionConstructionBudgetHeatAuthorizationAuthority, readiness=audit-only, ordinaryUiRawIds=false.",
                "Blueprint faction construction job closeout audit: future execution may close a faction construction job only after completion readiness, readable site status, crew release readiness, material ledger closure, budget closeout, attention closeout, and job record readiness are all declared.",
                "Blueprint faction construction job closeout sample audit: sampleCloseouts=" + samples.size()
                        + ", ready=" + ready
                        + ", blocked=" + blocked
                        + ", completionBlocked=" + completionBlocked
                        + ", crewBlocked=" + crewBlocked
                        + ", attentionBlocked=" + attentionBlocked + ".",
                "Blueprint faction construction job closeout examples: " + samples.get(0).boundaryLine()
                        + " | " + samples.get(1).boundaryLine()
                        + " | " + samples.get(2).boundaryLine(),
                "Blueprint faction construction job closeout rule: a future execution owner must leave a readable completed, failed, or blocked job record and keep reserve release separate from facility operation.",
                "Blueprint faction construction job closeout boundary: this audit does not release crew, return materials, refund budget, mutate heat, mutate suspicion, write job records, enable facility operation, mutate room ownership, or complete construction.",
                "Guard: Milestone03BlueprintFactionConstructionJobCloseoutAuditSmoke checks closeout readiness coverage, completion and release blockers, readable job records, future execution boundaries, and raw-ID hiding."
        );
    }

    private static String clean(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
