package mechanist.modapi.examples;

import mechanist.modapi.ModIntegrationHook;
import mechanist.modapi.PlacementNode;
import mechanist.modapi.RoomDimensions;
import mechanist.modapi.RoomNode;
import mechanist.modapi.SimulationContext;
import mechanist.modapi.SimulationEvent;

/** Room Editor example: reinforced hydroponics laboratory with scrubbers and containment growth nodes. */
public final class ReinforcedHydroponicsLabMod implements ModIntegrationHook {
    public static final String MOD_ID = "mechanist.example.reinforced_hydroponics_lab";
    public static final String ROOM_ID = "room.reinforced-hydroponics-lab";

    @Override public String modId() { return MOD_ID; }

    @Override public void onRegister(SimulationContext context) {
        RoomNode room = new RoomNode(ROOM_ID, "Reinforced Hydroponics Laboratory", new RoomDimensions(14, 10), true, 2);
        room.addPlacementNode(new PlacementNode("growth-cell-a", 3, 3, "growth-node-containment-cell"));
        room.addPlacementNode(new PlacementNode("growth-cell-b", 10, 3, "growth-node-containment-cell"));
        room.addPlacementNode(new PlacementNode("scrubber-east", 12, 7, "reactive-atmosphere-scrubber"));
        room.addPlacementNode(new PlacementNode("scrubber-west", 1, 7, "reactive-atmosphere-scrubber"));
        room.setAttribute("reinforcedBulkheads", true);
        room.setAttribute("scrubberResponseRate", 1.75d);
        room.setAttribute("containmentCellCount", 2);
        context.registerRoom(room);
        context.audit(MOD_ID, "registered reinforced hydroponics laboratory archetype");
    }

    @Override public void onRoomTick(SimulationContext context, RoomNode room) {
        if (!ROOM_ID.equals(room.id())) return;
        double oldOxygen = room.oxygenPercent();
        double target = room.oxygenSealed() ? 21.0d : 19.0d;
        double rate = 1.75d;
        Object configured = room.attributes().get("scrubberResponseRate");
        if (configured instanceof Number number) rate = Math.max(0.1d, Math.min(5.0d, number.doubleValue()));
        double next = oldOxygen + Math.signum(target - oldOxygen) * Math.min(Math.abs(target - oldOxygen), rate);
        room.setOxygenPercent(next);
        if (Math.abs(next - oldOxygen) > 0.001d) {
            context.emit(new SimulationEvent.RoomAtmosphereChanged(room.id(), oldOxygen, next, "reactive atmosphere scrubbers stabilized hydroponics lab atmosphere"));
        }
    }
}
