package mechanist;

/**
 * Deterministic frontage depth choices for plaza/road-first room generation.
 * Attempts a varied preferred depth and then fallback depths when the road
 * block is too narrow, retaining strict collision and corridor validation.
 */
final class RoadFrontageDepthAuthority {
    static final int DIRECT_SETBACK = 2;

    private RoadFrontageDepthAuthority() { }

    static int[] candidateSetbacks(long seed, int salt, int streetX, int streetY, int dx, int dy) {
        long hash = seed ^ (0x9e3779b97f4a7c15L * (salt + 1L))
                ^ (0xbf58476d1ce4e5b9L * (streetX + 1L))
                ^ (0x94d049bb133111ebL * (streetY + 1L))
                ^ ((long) dx << 17) ^ ((long) dy << 39);
        hash ^= hash >>> 30;
        hash *= 0xbf58476d1ce4e5b9L;
        hash ^= hash >>> 27;
        hash *= 0x94d049bb133111ebL;
        hash ^= hash >>> 31;
        int tier = Math.floorMod(hash, 10);
        int variation = Math.floorMod(hash >>> 12, 5);
        if (tier < 3) return new int[]{DIRECT_SETBACK, 5 + variation % 3, 9 + variation};
        if (tier < 7) return new int[]{5 + variation % 3, 9 + variation, DIRECT_SETBACK};
        return new int[]{9 + variation, 5 + variation % 3, DIRECT_SETBACK};
    }
}
