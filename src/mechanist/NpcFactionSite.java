package mechanist;

/**
 * Lightweight faction-site state used by faction strategy and service systems.
 *
 * This model exists outside the temporary GamePanel bridge so faction strategy
 * work can move toward a real faction services manager rather than restoring
 * the old panel monolith as the state owner.
 */
final class NpcFactionSite {
    final String id;
    final String name;
    final Faction faction;
    final ZoneType zoneType;
    int baseLevel;
    int machineLevel;
    int workers;
    int stock;

    NpcFactionSite(String id, String name, Faction faction, ZoneType zoneType) {
        this.id = id == null || id.isBlank() ? "site.unassigned" : id;
        this.name = name == null || name.isBlank() ? "unassigned faction site" : name;
        this.faction = faction == null ? Faction.NONE : faction;
        this.zoneType = zoneType;
    }

    static NpcFactionSite create(Faction faction, ZoneType zoneType, String label) {
        Faction f = faction == null ? Faction.NONE : faction;
        String zone = zoneType == null ? "unknown-zone" : zoneType.name().toLowerCase(java.util.Locale.ROOT);
        String name = label == null || label.isBlank() ? f.label + " local site" : label;
        return new NpcFactionSite("npc-faction-site-" + f.name().toLowerCase(java.util.Locale.ROOT) + "-" + zone, name, f, zoneType);
    }

    String recipeSummaryFor(String item) {
        String safeItem = item == null || item.isBlank() ? "unlisted stock" : item;
        return "site=" + name + " faction=" + faction.label + " item=" + safeItem + " baseLevel=" + baseLevel + " machineLevel=" + machineLevel + " workers=" + workers + " stock=" + stock;
    }
}
