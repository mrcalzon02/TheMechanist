package mechanist;

import java.util.ArrayList;
import java.util.List;

/**
 * Transitional audit authority used during movement-system unification.
 * Centralizes the known runtime movement channels so preview, planning,
 * execution, and smoke validation can compare the same routing model.
 */
final class MovementExecutionAuditAuthority {

    enum MovementChannel {
        KEYBOARD,
        MOUSE_PATH,
        CONTROLLER,
        QUEUED_PATH,
        SCRIPTED,
        RECOVERY
    }

    record MovementRouteAudit(MovementChannel channel,
                              boolean usesPlanningAuthority,
                              boolean usesActorLayerResolution,
                              boolean usesRecoveryBridge,
                              String status) {
    }

    static List<MovementRouteAudit> currentAuditSnapshot() {
        ArrayList<MovementRouteAudit> audits = new ArrayList<>();
        audits.add(new MovementRouteAudit(MovementChannel.KEYBOARD, true, false, false,
                "Requires execution-path verification."));
        audits.add(new MovementRouteAudit(MovementChannel.MOUSE_PATH, true, false, false,
                "Requires execution-path verification."));
        audits.add(new MovementRouteAudit(MovementChannel.CONTROLLER, true, false, false,
                "Requires execution-path verification."));
        audits.add(new MovementRouteAudit(MovementChannel.QUEUED_PATH, true, false, false,
                "Requires execution-path verification."));
        audits.add(new MovementRouteAudit(MovementChannel.SCRIPTED, false, false, false,
                "Legacy routing not yet audited."));
        audits.add(new MovementRouteAudit(MovementChannel.RECOVERY, true, false, true,
                "Runtime recovery bridge installed."));
        return audits;
    }

    static String milestoneSummary() {
        return "movement-unification-audit active channels=6 recovery-bridge=installed actor-layer-runtime-routing=pending";
    }

    private MovementExecutionAuditAuthority() {}
}
