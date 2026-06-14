package mechanist;

import mechanist.assets.AssetManager;

import java.util.List;

public final class RuntimeSemanticAssetHintSmoke {
    public static void main(String[] args) {
        if (AssetManager.registry().size() == 0) {
            throw new AssertionError("active semantic asset registry was empty");
        }
        int resolved = 0;
        for (String name : List.of("combat knife", "shotgun", "guard cot", "water barrel",
                "newspaper", "generator", "storage crate", "security sensor mast")) {
            var id = ItemSemanticAssetAuthority.runtimeAssetIdForItemName(name);
            if (id.isPresent()) {
                resolved++;
                if (AssetManager.metadata(id.get()).isEmpty()) {
                    throw new AssertionError("runtime item resolution escaped registry: " + name + " -> " + id.get());
                }
            }
        }
        if (resolved == 0) throw new AssertionError("no representative item hint resolved into the active registry");
        System.out.println("RuntimeSemanticAssetHintSmoke PASS registry="
                + AssetManager.registry().size() + " representativeResolved=" + resolved
                + " resolver=" + SemanticAssetHintResolver.VERSION);
    }

    private RuntimeSemanticAssetHintSmoke() {}
}
