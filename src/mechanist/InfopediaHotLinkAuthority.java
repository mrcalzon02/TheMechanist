package mechanist;

import java.util.List;
import java.util.Optional;

/** Opens a specific mechanic reference in the game-owned InfoPedia. */
final class InfopediaHotLinkAuthority {
    static Optional<String> resolveMechanicRow(String mechanicKey) {
        return SemanticAssetInfopediaAuthority.mechanicEntryRowByKey(mechanicKey);
    }

    static boolean openMechanic(GamePanel panel, String mechanicKey, String reason) {
        if (panel == null) return false;
        Optional<String> target = resolveMechanicRow(mechanicKey);
        if (target.isEmpty()) {
            panel.logEvent("No InfoPedia mechanic reference is registered for this panel.");
            panel.repaint();
            return false;
        }

        panel.infopediaTab = 0;
        panel.infopediaAssetFilter = "";
        List<String> entries = SemanticAssetInfopediaAuthority.entries(
                mechanist.assets.AssetManager.registry(), null, "");
        int index = entries.indexOf(target.get());
        if (index < 0) {
            panel.logEvent("The requested InfoPedia mechanic reference is not currently available.");
            panel.repaint();
            return false;
        }

        panel.infopediaSelectionIndex = index;
        panel.infopediaListScroll = Math.max(0, index - 2);
        panel.infopediaDetailScroll = 0;
        panel.openInfopediaPanel(reason);
        return true;
    }

    private InfopediaHotLinkAuthority() { }
}
