package mechanist;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Audits whether zone room, corridor, and entity features land on the intended
 * semantic tile layer instead of being represented only by a legacy glyph.
 */
final class ZoneTileLayerMappingAuditAuthority {
    static final String VERSION = "0.9.10ke";

    private ZoneTileLayerMappingAuditAuthority() { }

    enum TileLayer {
        SURFACE,
        SPACE,
        OWNERSHIP,
        STRUCTURE,
        CONTENT,
        ACTOR,
        LIGHTING,
        OVERLAY
    }

    static TileLayer layerForSlot(ZoneTileState.TileSlot slot) {
        if (slot == null) return TileLayer.OVERLAY;
        return switch (slot) {
            case SURFACE -> TileLayer.SURFACE;
            case SPACE -> TileLayer.SPACE;
            case OWNER -> TileLayer.OWNERSHIP;
            case ROOM, CORRIDOR, ROAD_NETWORK, TRANSITION, VERTICAL_TRANSITION, RESERVATION -> TileLayer.STRUCTURE;
            case FIXTURE, CONTAINER, LOOSE_ITEM -> TileLayer.CONTENT;
            case ENTITY, PET, VEHICLE -> TileLayer.ACTOR;
            case LIGHT -> TileLayer.LIGHTING;
            case OVERLAY -> TileLayer.OVERLAY;
        };
    }

    static MappingAudit auditTile(ZoneTileState tile) {
        EnumMap<TileLayer, Integer> counts = new EnumMap<>(TileLayer.class);
        ArrayList<String> findings = new ArrayList<>();
        if (tile == null) {
            findings.add("No tile state supplied.");
            return new MappingAudit(false, Map.copyOf(counts), List.copyOf(findings));
        }

        for (Map.Entry<ZoneTileState.TileSlot, List<ZoneTileState.TileSlotRef>> entry : tile.slotSnapshot().entrySet()) {
            TileLayer layer = layerForSlot(entry.getKey());
            counts.put(layer, counts.getOrDefault(layer, 0) + entry.getValue().size());
        }

        requireSlot(tile, ZoneTileState.TileSlot.SURFACE, "surface", findings);
        if (tile.spaceType() == ZoneTileState.SpaceType.ROOM) requireSlot(tile, ZoneTileState.TileSlot.ROOM, "room", findings);
        if (tile.spaceType() == ZoneTileState.SpaceType.CORRIDOR) requireSlot(tile, ZoneTileState.TileSlot.CORRIDOR, "corridor", findings);
        if (tile.ownerFaction() != Faction.NONE) requireSlot(tile, ZoneTileState.TileSlot.OWNER, "ownership", findings);
        if (!tile.occupantEntityId().isBlank()) requireSlot(tile, ZoneTileState.TileSlot.ENTITY, "entity actor", findings);
        if (!tile.petEntityId().isBlank()) requireSlot(tile, ZoneTileState.TileSlot.PET, "pet actor", findings);
        if (!tile.vehicleId().isBlank()) requireSlot(tile, ZoneTileState.TileSlot.VEHICLE, "vehicle actor", findings);
        if (!tile.objects().isEmpty() && !(tile.hasSlot(ZoneTileState.TileSlot.FIXTURE) || tile.hasSlot(ZoneTileState.TileSlot.CONTAINER))) {
            findings.add("Object records exist without fixture/container content slots.");
        }
        if (!tile.lights().isEmpty() && !tile.hasSlot(ZoneTileState.TileSlot.LIGHT)) {
            findings.add("Light records exist without lighting slots.");
        }

        boolean valid = findings.stream().noneMatch(line -> line.startsWith("Missing") || line.contains("without"));
        if (valid) findings.add("Room, corridor, content, actor, and lighting data map to semantic tile layers.");
        return new MappingAudit(valid, Map.copyOf(counts), List.copyOf(findings));
    }

    static MappingAudit auditRoomTile(ZoneTileState tile) {
        MappingAudit audit = auditTile(tile);
        ArrayList<String> findings = new ArrayList<>(audit.findings());
        if (tile == null || tile.spaceType() != ZoneTileState.SpaceType.ROOM) findings.add("Missing room space type.");
        if (tile != null && !tile.hasSlot(ZoneTileState.TileSlot.ROOM)) findings.add("Missing room structure slot.");
        return audit.withFindings(findings);
    }

    static MappingAudit auditCorridorTile(ZoneTileState tile) {
        MappingAudit audit = auditTile(tile);
        ArrayList<String> findings = new ArrayList<>(audit.findings());
        if (tile == null || tile.spaceType() != ZoneTileState.SpaceType.CORRIDOR) findings.add("Missing corridor space type.");
        if (tile != null && !tile.hasSlot(ZoneTileState.TileSlot.CORRIDOR)) findings.add("Missing corridor structure slot.");
        return audit.withFindings(findings);
    }

    static MappingAudit auditEntityTile(ZoneTileState tile) {
        MappingAudit audit = auditTile(tile);
        ArrayList<String> findings = new ArrayList<>(audit.findings());
        if (tile == null || tile.occupantEntityId().isBlank()) findings.add("Missing occupant entity id.");
        if (tile != null && !tile.hasSlot(ZoneTileState.TileSlot.ENTITY)) findings.add("Missing entity actor slot.");
        return audit.withFindings(findings);
    }

    static String playerFacingSummary(ZoneTileState tile) {
        MappingAudit audit = auditTile(tile);
        String state = audit.valid() ? "Layer mapping ready" : "Layer mapping needs repair";
        return PlayerFacingText.sanitize(state + ": " + String.join(" ", audit.findings()));
    }

    static String auditSummary() {
        return "zoneTileLayerMappingAuditAuthority version=" + VERSION
                + " layers=surface+space+ownership+structure+content+actor+lighting+overlay"
                + " checks=room+corridor+entity+pet+vehicle+object+light legacyGlyph=bridgeOnly";
    }

    private static void requireSlot(ZoneTileState tile, ZoneTileState.TileSlot slot, String label, List<String> findings) {
        if (!tile.hasSlot(slot)) findings.add("Missing " + label + " slot.");
    }

    record MappingAudit(boolean valid, Map<TileLayer, Integer> layerCounts, List<String> findings) {
        MappingAudit withFindings(List<String> newFindings) {
            boolean newValid = newFindings.stream().noneMatch(line -> line.startsWith("Missing") || line.contains("without"));
            return new MappingAudit(newValid, layerCounts, List.copyOf(newFindings));
        }
    }
}
