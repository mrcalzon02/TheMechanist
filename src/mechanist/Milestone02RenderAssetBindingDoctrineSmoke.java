package mechanist;

public final class Milestone02RenderAssetBindingDoctrineSmoke {
    public static void main(String[] args) {
        if (!RenderAssetBindingDoctrineAuthority.doctrinePasses()) {
            throw new AssertionError("render asset binding doctrine failed");
        }
        if (!RenderAssetBindingDoctrineAuthority.acceptsStreetLight("fixture infrastructure", "streetlight pole lamp", "semantic fallback")) {
            throw new AssertionError("streetlight fixture binding was rejected");
        }
        if (RenderAssetBindingDoctrineAuthority.acceptsStreetLight("item icon", "system inventory light", "system inventory")) {
            throw new AssertionError("streetlight accepted system inventory icon fallback");
        }
        if (!RenderAssetBindingDoctrineAuthority.acceptsClosedDoor("door tile", "closed door variant", "semantic fallback")) {
            throw new AssertionError("closed door tile binding was rejected");
        }
        if (!RenderAssetBindingDoctrineAuthority.acceptsOpenDoor("door tile", "open door variant", "semantic fallback")) {
            throw new AssertionError("open door tile binding was rejected");
        }
        if (!RenderAssetBindingDoctrineAuthority.acceptsPauseMainMenu("menu command", "return root main menu", "root")) {
            throw new AssertionError("pause main menu route was rejected");
        }
        if (RenderAssetBindingDoctrineAuthority.acceptsPauseMainMenu("menu command", "return root main menu", "infopedia")) {
            throw new AssertionError("pause main menu accepted Infopedia fallback");
        }
        if (!RenderAssetBindingDoctrineAuthority.acceptsThematicTile("sewer", "floor tile", "sewer floor wet utility tunnel", "sewer")) {
            throw new AssertionError("sewer floor thematic binding was rejected");
        }
        if (RenderAssetBindingDoctrineAuthority.acceptsThematicTile("sewer", "floor tile", "generic floor", "main floor")) {
            throw new AssertionError("sewer context accepted generic/main-floor tile fallback");
        }
        if (!RenderAssetBindingDoctrineAuthority.acceptsThematicTile("generic", "floor tile", "generic floor", "generic")) {
            throw new AssertionError("generic floor thematic binding was rejected");
        }
        if (RenderAssetBindingDoctrineAuthority.acceptsThematicTile("generic", "floor tile", "sewer floor", "sewer")) {
            throw new AssertionError("generic context accepted sewer tile fallback");
        }
        System.out.println("Milestone02RenderAssetBindingDoctrineSmoke PASS " + RenderAssetBindingDoctrineAuthority.VERSION);
    }

    private Milestone02RenderAssetBindingDoctrineSmoke() {}
}
