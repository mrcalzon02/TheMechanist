package mechanist.modapi;

import java.util.List;

public record LoreEntry(String id, String title, String taxonomyPath, String body, List<String> searchTags, List<String> crossLinks) {
    public LoreEntry {
        id = SimulationContext.cleanId(id);
        title = SimulationContext.safe(title).isEmpty() ? id : SimulationContext.safe(title);
        taxonomyPath = SimulationContext.safe(taxonomyPath).isEmpty() ? "uncategorized" : SimulationContext.safe(taxonomyPath);
        body = SimulationContext.safe(body);
        searchTags = searchTags == null ? List.of() : List.copyOf(searchTags);
        crossLinks = crossLinks == null ? List.of() : List.copyOf(crossLinks);
    }
}
