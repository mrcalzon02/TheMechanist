package mechanist;

final class PlayerFacingCopySanitizerSmoke {
    public static void main(String[] args) {
        String raw = "DebugLog failure in mechanist.runtime.Loader id=B-99124 targetZoneKey=1,1,2,3,4,false path=C:\\\\games\\\\TheMechanist\\\\debug.log return Ident chip B-99124 and inspect java.lang.IllegalStateException";
        String cleaned = PlayerFacingCopySanitizer.forOrdinaryPlayer(raw);

        if (cleaned.contains("B-99124")) {
            throw new AssertionError("Internal contract identifier leaked: " + cleaned);
        }
        if (cleaned.contains("1,1,2,3,4,false")) {
            throw new AssertionError("Raw zone key leaked: " + cleaned);
        }
        if (cleaned.contains("C:\\\\games")) {
            throw new AssertionError("Filesystem path leaked: " + cleaned);
        }
        if (cleaned.contains("mechanist.runtime.Loader") || cleaned.contains("java.lang.IllegalStateException")) {
            throw new AssertionError("Java/runtime implementation wording leaked: " + cleaned);
        }
        if (!cleaned.contains("diagnostic")) {
            throw new AssertionError("Expected readable diagnostic wording: " + cleaned);
        }
        if (!cleaned.contains("the target's ident chip")) {
            throw new AssertionError("Expected sanitized ident-chip wording: " + cleaned);
        }
        if (PlayerFacingCopySanitizer.containsLikelyPlayerFacingLeak(cleaned)) {
            throw new AssertionError("Leak detector still found implementation residue: " + cleaned);
        }
    }
}
