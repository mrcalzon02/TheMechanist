package mechanist.modapi;

public record AlignmentVector(int order, int autonomy, int extraction, int humanitarian, int machineOrthodoxy) {
    public AlignmentVector {
        order = clamp(order);
        autonomy = clamp(autonomy);
        extraction = clamp(extraction);
        humanitarian = clamp(humanitarian);
        machineOrthodoxy = clamp(machineOrthodoxy);
    }
    private static int clamp(int value) { return Math.max(-100, Math.min(100, value)); }
}
