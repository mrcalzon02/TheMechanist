package mechanist;

/** Normal authoritative player medical procedure, including all resource and location checks. */
record CyberneticCommand(String playerId,String action,String root,String item,String patientId) implements WorldCommandRequest {
    CyberneticCommand(String playerId,String action,String root,String item) {this(playerId,action,root,item,"");}
    public String reason() {return "limb procedure "+action+" "+root;}
    public String rejectionReason(WorldCommandRuntimeContext runtime) {
        String base=WorldCommandRequest.super.rejectionReason(runtime);
        return base.isEmpty()?runtime.cyberneticRejection(action,root,item,patientId):base;
    }
    public void apply(WorldCommandRuntimeContext runtime) {runtime.cyberneticAction(action,root,item,patientId);}
}
