package mechanist.modapi;

public record LoreQuery(String queryText, int maxResults) {
    public LoreQuery {
        queryText = SimulationContext.safe(queryText);
        maxResults = Math.max(1, Math.min(200, maxResults));
    }
}
