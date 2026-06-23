package mechanist;

/** Labels who owned the production run without inventing supervisor mechanics. */
final class ProductionWorkforceModeAuthority {
    private ProductionWorkforceModeAuthority() { }

    static String manualLabel(String worker) {
        String name = ProductionOperatorIdentityAuthority.provenanceLabel(worker);
        return "immediate manual Craft / operator " + name;
    }

    static String staffedLabel(String worker) {
        String name = ProductionOperatorIdentityAuthority.provenanceLabel(worker);
        return "staffed queued production / assigned worker " + name;
    }
}
