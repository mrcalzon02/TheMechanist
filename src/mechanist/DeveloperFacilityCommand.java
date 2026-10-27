package mechanist;

import java.util.List;

/** Host-only entry and mutation requests for the isolated developer facility. */
record DeveloperFacilityCommand(String playerId, String action, List<String> arguments) implements WorldCommandRequest {
    DeveloperFacilityCommand {
        action = action == null ? "enter" : action.toLowerCase(java.util.Locale.ROOT);
        arguments = arguments == null ? List.of() : List.copyOf(arguments);
    }
    public String reason() { return "developer facility " + action; }
    public boolean requiresAdminAuthority() { return true; }
    public boolean requiresUngatedPlayer() { return false; }
    public String rejectionReason(WorldCommandRuntimeContext runtime) {
        String base = WorldCommandRequest.super.rejectionReason(runtime);
        return base.isBlank() ? runtime.developerFacilityRejection(action, arguments) : base;
    }
    public void apply(WorldCommandRuntimeContext runtime) { runtime.developerFacilityAction(action, arguments); }
}
