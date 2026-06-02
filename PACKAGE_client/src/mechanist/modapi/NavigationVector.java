package mechanist.modapi;

public record NavigationVector(double x, double y, double z) {
    public NavigationVector add(NavigationVector other) {
        if (other == null) return this;
        return new NavigationVector(x + other.x, y + other.y, z + other.z);
    }

    public NavigationVector scale(double scalar) { return new NavigationVector(x * scalar, y * scalar, z * scalar); }

    public double magnitude() { return Math.sqrt((x * x) + (y * y) + (z * z)); }

    public NavigationVector clampMagnitude(double maxMagnitude) {
        double safeMax = Math.max(0.0, maxMagnitude);
        double mag = magnitude();
        if (mag <= safeMax || mag == 0.0) return this;
        return scale(safeMax / mag);
    }
}
