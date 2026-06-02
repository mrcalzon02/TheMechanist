package mechanist;

/**
 * Deterministic, seed-owned 2D Perlin noise for terrain-art variation.
 *
 * This is deliberately small and dependency-free. It does not place gameplay
 * content; it gives tile descriptor compilation a scalable smooth value so
 * floors/walls can choose variants from the level seed instead of noisy hash
 * squares.
 */
final class PerlinNoiseAuthority {
    private PerlinNoiseAuthority() {}

    static double noise(long seed, double x, double y, double scale, int octaves) {
        double s = Math.max(0.001, scale);
        int o = Math.max(1, Math.min(8, octaves));
        double amp = 1.0;
        double freq = 1.0 / s;
        double sum = 0.0;
        double norm = 0.0;
        for (int i = 0; i < o; i++) {
            sum += single(seed + i * 0x9E3779B97F4A7C15L, x * freq, y * freq) * amp;
            norm += amp;
            amp *= 0.5;
            freq *= 2.0;
        }
        return norm <= 0.0 ? 0.0 : Math.max(-1.0, Math.min(1.0, sum / norm));
    }

    static int variant(long seed, int x, int y, String family, int count, double scale, int octaves) {
        int c = Math.max(1, count);
        long mixed = seed ^ mix64(family == null ? 0L : family.hashCode() * 0x9E3779B97F4A7C15L);
        double n = noise(mixed, x, y, scale, octaves);
        int idx = (int)Math.floor(((n + 1.0) * 0.5) * c);
        if (idx < 0) idx = 0;
        if (idx >= c) idx = c - 1;
        return 1 + idx;
    }

    private static double single(long seed, double x, double y) {
        int x0 = fastFloor(x);
        int y0 = fastFloor(y);
        int x1 = x0 + 1;
        int y1 = y0 + 1;
        double sx = fade(x - x0);
        double sy = fade(y - y0);
        double n00 = grad(seed, x0, y0, x - x0, y - y0);
        double n10 = grad(seed, x1, y0, x - x1, y - y0);
        double n01 = grad(seed, x0, y1, x - x0, y - y1);
        double n11 = grad(seed, x1, y1, x - x1, y - y1);
        double ix0 = lerp(n00, n10, sx);
        double ix1 = lerp(n01, n11, sx);
        return lerp(ix0, ix1, sy) * 1.41421356237;
    }

    private static int fastFloor(double v) { int i = (int)v; return v < i ? i - 1 : i; }
    private static double fade(double t) { return t * t * t * (t * (t * 6.0 - 15.0) + 10.0); }
    private static double lerp(double a, double b, double t) { return a + (b - a) * t; }

    private static double grad(long seed, int x, int y, double dx, double dy) {
        long h = mix64(seed ^ (x * 0x632BE59BD9B4E019L) ^ (y * 0x9E3779B97F4A7C15L));
        switch ((int)(h & 7L)) {
            case 0: return  dx + dy;
            case 1: return -dx + dy;
            case 2: return  dx - dy;
            case 3: return -dx - dy;
            case 4: return  dx;
            case 5: return -dx;
            case 6: return  dy;
            default: return -dy;
        }
    }

    private static long mix64(long z) {
        z = (z ^ (z >>> 30)) * 0xbf58476d1ce4e5b9L;
        z = (z ^ (z >>> 27)) * 0x94d049bb133111ebL;
        return z ^ (z >>> 31);
    }
}
