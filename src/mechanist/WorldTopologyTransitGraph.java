package mechanist;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Topology-level transit graph for NPCs, faction logistics, and faction schemes.
 *
 * This intentionally models only guaranteed generator infrastructure: central
 * plaza, cardinal exits, edge gates, and vertical transitions.  Room-level AI can
 * later attach faction rooms, delivery targets, patrol origins, and scheme target
 * rooms to the nearest graph node instead of hand-rolling cross-zone movement.
 */
final class WorldTopologyTransitGraph {
    private WorldTopologyTransitGraph() {}

    enum NodeKind {
        CENTRAL_PLAZA,
        ROAD_JUNCTION,
        EDGE_GATE,
        NEIGHBOR_ZONE_ENTRY,
        ELEVATOR_STOP,
        STAIR_STOP,
        MANHOLE,
        DRAIN,
        ROOM_ATTACHMENT
    }

    enum RoutePurpose {
        NPC_TRAVEL,
        FACTION_LOGISTICS_DELIVERY,
        FACTION_SCHEME_TARGETING,
        PATROL,
        EMERGENCY_RESPONSE
    }

    record ZoneRef(int zoneX, int zoneY, int floor) {
        ZoneRef neighbor(WorldTopologyContract.CardinalExit direction) {
            return switch (direction) {
                case NORTH -> new ZoneRef(zoneX, zoneY - 1, floor);
                case SOUTH -> new ZoneRef(zoneX, zoneY + 1, floor);
                case EAST -> new ZoneRef(zoneX + 1, zoneY, floor);
                case WEST -> new ZoneRef(zoneX - 1, zoneY, floor);
            };
        }

        ZoneRef floor(int nextFloor) { return new ZoneRef(zoneX, zoneY, nextFloor); }
        String key() { return zoneX + ":" + zoneY + ":" + floor; }
    }

    record NodeId(String value) {
        static NodeId of(ZoneRef zone, String local) { return new NodeId(zone.key() + ":" + local); }
    }

    record TransitNode(
            NodeId id,
            NodeKind kind,
            ZoneRef zone,
            WorldTopologyContract.TilePoint position,
            String label
    ) {}

    record TransitEdge(
            NodeId from,
            NodeId to,
            RoutePurpose purpose,
            int cost,
            String label
    ) {}

    record Graph(
            ZoneRef zone,
            List<TransitNode> nodes,
            List<TransitEdge> edges
    ) {
        TransitNode node(NodeId id) {
            for (TransitNode node : nodes) if (node.id().equals(id)) return node;
            return null;
        }

        boolean canReach(NodeId from, NodeId to, RoutePurpose purpose) {
            return route(from, to, purpose).reachable();
        }

        Route route(NodeId from, NodeId to, RoutePurpose purpose) {
            if (from == null || to == null) return Route.unreachable(from, to, "missing endpoint");
            Map<NodeId, List<TransitEdge>> adjacency = new HashMap<>();
            for (TransitEdge edge : edges) {
                if (!edgeAllowed(edge, purpose)) continue;
                adjacency.computeIfAbsent(edge.from(), k -> new ArrayList<>()).add(edge);
            }
            ArrayDeque<NodeId> queue = new ArrayDeque<>();
            HashMap<NodeId, TransitEdge> previous = new HashMap<>();
            HashSet<NodeId> seen = new HashSet<>();
            queue.add(from);
            seen.add(from);
            while (!queue.isEmpty()) {
                NodeId current = queue.removeFirst();
                if (current.equals(to)) break;
                for (TransitEdge edge : adjacency.getOrDefault(current, List.of())) {
                    if (seen.add(edge.to())) {
                        previous.put(edge.to(), edge);
                        queue.addLast(edge.to());
                    }
                }
            }
            if (!seen.contains(to)) return Route.unreachable(from, to, "no route through topology graph");
            ArrayList<TransitEdge> path = new ArrayList<>();
            NodeId cursor = to;
            while (!cursor.equals(from)) {
                TransitEdge edge = previous.get(cursor);
                if (edge == null) return Route.unreachable(from, to, "route reconstruction failed");
                path.add(0, edge);
                cursor = edge.from();
            }
            return new Route(true, from, to, List.copyOf(path), "reachable");
        }
    }

    record Route(
            boolean reachable,
            NodeId from,
            NodeId to,
            List<TransitEdge> edges,
            String reason
    ) {
        static Route unreachable(NodeId from, NodeId to, String reason) {
            return new Route(false, from, to, List.of(), reason == null ? "unreachable" : reason);
        }

        int totalCost() {
            int cost = 0;
            for (TransitEdge edge : edges) cost += Math.max(0, edge.cost());
            return cost;
        }

        String summary() {
            return "reachable=" + reachable + " edges=" + edges.size() + " cost=" + totalCost() + " reason=" + reason;
        }
    }

    static Graph fromTopology(WorldTopologyContract.ZoneTransitionPlan topology) {
        if (topology == null) throw new IllegalArgumentException("topology plan is required");
        ZoneRef zone = new ZoneRef(topology.zoneX(), topology.zoneY(), topology.floor());
        ArrayList<TransitNode> nodes = new ArrayList<>();
        ArrayList<TransitEdge> edges = new ArrayList<>();
        NodeId plaza = NodeId.of(zone, "plaza");
        nodes.add(new TransitNode(plaza, NodeKind.CENTRAL_PLAZA, zone, topology.centralPlaza(), "Central Plaza"));

        for (WorldTopologyContract.CardinalExit direction : WorldTopologyContract.CardinalExit.values()) {
            WorldTopologyContract.EdgeTransitionAnchor exit = topology.exit(direction);
            if (exit == null) continue;
            NodeId gate = NodeId.of(zone, "edge-" + direction.name().toLowerCase());
            ZoneRef neighborZone = zone.neighbor(direction);
            NodeId neighbor = NodeId.of(neighborZone, "edge-" + direction.opposite().name().toLowerCase());
            nodes.add(new TransitNode(gate, NodeKind.EDGE_GATE, zone, exit.roadCenter(), direction + " Edge Gate"));
            nodes.add(new TransitNode(neighbor, NodeKind.NEIGHBOR_ZONE_ENTRY, neighborZone, exit.matchingNeighborEntrance().roadCenter(), "Neighbor " + direction + " Entry"));
            connectBoth(edges, plaza, gate, RoutePurpose.NPC_TRAVEL, 10, "plaza-road-" + direction.name().toLowerCase());
            connectBoth(edges, plaza, gate, RoutePurpose.FACTION_LOGISTICS_DELIVERY, 8, "logistics-road-" + direction.name().toLowerCase());
            connectBoth(edges, plaza, gate, RoutePurpose.FACTION_SCHEME_TARGETING, 12, "scheme-road-" + direction.name().toLowerCase());
            edges.add(new TransitEdge(gate, neighbor, RoutePurpose.NPC_TRAVEL, 15, "zone-transition-" + direction.name().toLowerCase()));
            edges.add(new TransitEdge(gate, neighbor, RoutePurpose.FACTION_LOGISTICS_DELIVERY, 15, "logistics-zone-transition-" + direction.name().toLowerCase()));
            edges.add(new TransitEdge(gate, neighbor, RoutePurpose.FACTION_SCHEME_TARGETING, 18, "scheme-zone-transition-" + direction.name().toLowerCase()));
        }

        int verticalIndex = 0;
        for (WorldTopologyContract.VerticalTransitionAnchor vertical : topology.verticalTransitions()) {
            NodeKind kind = verticalNodeKind(vertical);
            NodeId node = NodeId.of(zone, kind.name().toLowerCase() + "-" + verticalIndex++);
            nodes.add(new TransitNode(node, kind, zone, vertical.position(), vertical.kind().name()));
            connectBoth(edges, plaza, node, RoutePurpose.NPC_TRAVEL, 8, "plaza-vertical-" + kind.name().toLowerCase());
            connectBoth(edges, plaza, node, RoutePurpose.FACTION_LOGISTICS_DELIVERY, 9, "logistics-vertical-" + kind.name().toLowerCase());
            connectBoth(edges, plaza, node, RoutePurpose.FACTION_SCHEME_TARGETING, 10, "scheme-vertical-" + kind.name().toLowerCase());
            NodeId linked = linkedVerticalNode(zone, vertical);
            if (linked != null) {
                edges.add(new TransitEdge(node, linked, RoutePurpose.NPC_TRAVEL, vertical.isElevator() ? 6 : 10, "vertical-link-" + vertical.kind().name().toLowerCase()));
                edges.add(new TransitEdge(node, linked, RoutePurpose.FACTION_LOGISTICS_DELIVERY, vertical.isElevator() ? 7 : 12, "logistics-vertical-link-" + vertical.kind().name().toLowerCase()));
                edges.add(new TransitEdge(node, linked, RoutePurpose.FACTION_SCHEME_TARGETING, vertical.isElevator() ? 8 : 14, "scheme-vertical-link-" + vertical.kind().name().toLowerCase()));
            }
        }

        return new Graph(zone, List.copyOf(nodes), List.copyOf(edges));
    }

    static NodeId attachRoom(Graph graph, List<TransitNode> mutableNodes, List<TransitEdge> mutableEdges, String roomId, WorldTopologyContract.TilePoint position, RoutePurpose purpose) {
        if (graph == null || mutableNodes == null || mutableEdges == null) return null;
        NodeId id = NodeId.of(graph.zone(), "room-" + safe(roomId));
        mutableNodes.add(new TransitNode(id, NodeKind.ROOM_ATTACHMENT, graph.zone(), position, "Room " + safe(roomId)));
        NodeId plaza = NodeId.of(graph.zone(), "plaza");
        connectBoth(mutableEdges, plaza, id, purpose == null ? RoutePurpose.NPC_TRAVEL : purpose, 5, "room-attachment-" + safe(roomId));
        return id;
    }

    static NodeKind verticalNodeKind(WorldTopologyContract.VerticalTransitionAnchor vertical) {
        if (vertical == null) return NodeKind.STAIR_STOP;
        if (vertical.isElevator()) return NodeKind.ELEVATOR_STOP;
        if (vertical.isStair()) return NodeKind.STAIR_STOP;
        if (vertical.kind() == WorldTopologyContract.VerticalTransitionKind.MANHOLE_DOWN_TO_SEWER) return NodeKind.MANHOLE;
        if (vertical.kind() == WorldTopologyContract.VerticalTransitionKind.DRAIN_DOWN_FROM_SEWER) return NodeKind.DRAIN;
        return NodeKind.STAIR_STOP;
    }

    static NodeId linkedVerticalNode(ZoneRef zone, WorldTopologyContract.VerticalTransitionAnchor vertical) {
        if (vertical == null) return null;
        int targetFloor;
        switch (vertical.kind()) {
            case ELEVATOR_BOTTOM -> targetFloor = zone.floor() + 1;
            case ELEVATOR_MIDDLE -> targetFloor = zone.floor() + 1;
            case ELEVATOR_TOP -> targetFloor = zone.floor() - 1;
            case STAIR_BOTTOM_GOES_UP -> targetFloor = zone.floor() + 1;
            case STAIR_TOP_GOES_DOWN -> targetFloor = zone.floor() - 1;
            case MANHOLE_DOWN_TO_SEWER -> targetFloor = zone.floor() - 1;
            case DRAIN_DOWN_FROM_SEWER -> targetFloor = zone.floor() - 1;
            default -> targetFloor = zone.floor();
        }
        ZoneRef target = zone.floor(targetFloor);
        return NodeId.of(target, verticalNodeKind(vertical).name().toLowerCase() + "-linked");
    }

    static boolean edgeAllowed(TransitEdge edge, RoutePurpose purpose) {
        if (edge == null) return false;
        if (purpose == null) return true;
        if (edge.purpose() == purpose) return true;
        return purpose == RoutePurpose.EMERGENCY_RESPONSE && edge.purpose() == RoutePurpose.NPC_TRAVEL;
    }

    static void connectBoth(ArrayList<TransitEdge> edges, NodeId a, NodeId b, RoutePurpose purpose, int cost, String label) {
        edges.add(new TransitEdge(a, b, purpose, cost, label));
        edges.add(new TransitEdge(b, a, purpose, cost, label + "-return"));
    }

    static void audit(Graph graph, String source) {
        if (graph == null) {
            DebugLog.warn("WORLD_TOPOLOGY_TRANSIT", "source=" + safe(source) + " graph missing");
            return;
        }
        Set<RoutePurpose> purposes = new HashSet<>();
        for (TransitEdge edge : graph.edges()) purposes.add(edge.purpose());
        DebugLog.audit("WORLD_TOPOLOGY_TRANSIT", "source=" + safe(source)
                + " zone=" + graph.zone().key()
                + " nodes=" + graph.nodes().size()
                + " edges=" + graph.edges().size()
                + " purposes=" + purposes);
    }

    private static String safe(String value) {
        if (value == null || value.isBlank()) return "unspecified";
        return value.replace('\n', ' ').trim().replaceAll("[^A-Za-z0-9_.:-]+", "_");
    }
}
