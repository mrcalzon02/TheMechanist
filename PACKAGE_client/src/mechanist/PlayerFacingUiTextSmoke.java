package mechanist;

final class PlayerFacingUiTextSmoke {
    public static void main(String[] args) {
        String look = PlayerFacingUiText.lookDetail(
                "mechanist.runtime.Scanner",
                "targetZoneKey=1,1,2,3,4,false registryKey=debug.catalog path=/var/tmp/world/save.dat"
        );

        if (look.contains("targetZoneKey") || look.contains("1,1,2,3,4,false")) {
            throw new AssertionError("Look detail leaked a raw zone key: " + look);
        }
        if (look.contains("/var/tmp/world/save.dat")) {
            throw new AssertionError("Look detail leaked a filesystem path: " + look);
        }
        if (look.contains("mechanist.runtime.Scanner")) {
            throw new AssertionError("Look detail leaked a runtime class name: " + look);
        }

        String denied = PlayerFacingUiText.constructionDenied(
                "DebugLog registry mismatch at C:\\\\mechanist\\\\broken.txt"
        );

        if (denied.contains("C:\\\\mechanist")) {
            throw new AssertionError("Construction denial leaked a path: " + denied);
        }
        if (denied.contains("registry mismatch")) {
            throw new AssertionError("Construction denial leaked registry implementation wording: " + denied);
        }
        if (!denied.contains("catalog")) {
            throw new AssertionError("Construction denial did not remap registry wording: " + denied);
        }

        String summary = PlayerFacingUiText.saveLoadSummary(
                "Loaded java.lang.IllegalStateException from /srv/mechanist/debug.log"
        );

        if (summary.contains("java.lang.IllegalStateException") || summary.contains("/srv/mechanist/debug.log")) {
            throw new AssertionError("Save/load summary leaked implementation residue: " + summary);
        }

        String diagnostic = PlayerFacingUiText.diagnosticNotice("Render failure encountered.");
        if (!diagnostic.contains("Diagnostic details were recorded.")) {
            throw new AssertionError("Diagnostic notice did not include readable diagnostic wording: " + diagnostic);
        }
    }
}
