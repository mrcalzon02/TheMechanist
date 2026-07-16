package mechanist;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.List;
import java.util.Properties;
import java.util.Random;

/** End-to-end smoke for reversible world-event room and facility mutation. */
final class Milestone04WorldEventFacilityMutationSmoke {
    public static void main(String[] args) {
        System.setProperty("java.awt.headless","true");
        testClosurePersistenceAndRecovery();
        System.out.println("Milestone 04 world-event facility mutation smoke passed.");
    }

    private static void testClosurePersistenceAndRecovery() {
        World world=world(16901L);
        FactionImportNodeGenerationAuthority.promoteAndPlan(world,new Random(1));
        FactionImportNodeGenerationAuthority.placePhysicalNodes(world,new Random(2));
        MapObjectState node=FactionImportNodeGenerationAuthority.primaryNode(world,Faction.HIVER);
        require(node!=null,"test world should contain a physical Hiver import node");
        int roomId=Integer.parseInt(MapObjectState.stockValue(node.stockState,"roomId"));
        RoomProfile room=world.roomProfiles.get(roomId);
        String originalLabel=node.label,originalStock=node.stockState,originalRoom=room.name;
        char originalGlyph=node.glyph;
        require(FactionImportNodeGenerationAuthority.arrivalPoint(world,Faction.HIVER)!=null,
                "open import node should provide a reinforcement arrival point");

        TopDownWorldEventRecord outage=TopDownWorldEventAuthority.scheduleCurated(world,"TRAIN_OUTAGE",100L);
        TopDownWorldEventAuthority.tick(world,null,100L);
        require("ACTIVE".equals(outage.physicalStatus)&&outage.mutationObjectId.equals(node.id)
                        &&outage.mutationRoomId==roomId,
                "active outage should bind its physical mutation to the exact generated node and room");
        require(node.glyph=='!'&&world.tiles[node.x][node.y]=='!'
                        &&"true".equals(MapObjectState.stockValue(node.stockState,"eventClosed")),
                "active outage should visibly mark and close the import node");
        require(node.label.startsWith("Train outage /")&&room.name.startsWith("Train outage /"),
                "active outage should visibly name both the marker and its room");
        requireContains(room.featureText,"Event condition: Train outage","mutated room feature text");
        require(FactionImportNodeGenerationAuthority.arrivalPoint(world,Faction.HIVER)==null,
                "closed import node should block personnel arrival placement");
        requireContains(String.join(" ",FactionImportNodeGenerationAuthority.inspectionLines(world,node,100L)),
                "personnel intake closed by an active world event","closed-node inspection");
        requireContains(outage.mutationSummary,"external personnel intake is closed","physical mutation summary");

        Properties saved=new Properties();Persistence.writeWorldState(world,saved);
        World restored=world(16901L);
        FactionImportNodeGenerationAuthority.promoteAndPlan(restored,new Random(1));
        FactionImportNodeGenerationAuthority.placePhysicalNodes(restored,new Random(2));
        Persistence.readWorldState(restored,saved);
        TopDownWorldEventRecord restoredEvent=restored.topDownWorldEvents.get(0);
        MapObjectState restoredNode=FactionImportNodeGenerationAuthority.primaryNode(restored,Faction.HIVER);
        require(restoredNode!=null&&restoredNode.glyph=='!'
                        &&"true".equals(MapObjectState.stockValue(restoredNode.stockState,"eventClosed")),
                "save/load should retain the closed physical node");
        require(restored.roomProfiles.get(restoredEvent.mutationRoomId).name.startsWith("Train outage /"),
                "save/load should reassert the active room mutation on regenerated room data");
        require(restoredEvent.originalObjectLabel.equals(originalLabel)
                        &&restoredEvent.originalRoomName.equals(originalRoom),
                "save/load should retain exact restoration data");

        TopDownWorldEventAuthority.tick(world,null,outage.endWorldTurn);
        require("RECOVERED".equals(outage.physicalStatus)&&node.glyph==originalGlyph
                        &&originalLabel.equals(node.label)&&originalStock.equals(node.stockState),
                "outage recovery should restore exact node glyph, label, and stock state");
        require(originalRoom.equals(room.name)&&FactionImportNodeGenerationAuthority.isOperational(node),
                "outage recovery should restore the exact room name and reopen intake");
        require(FactionImportNodeGenerationAuthority.arrivalPoint(world,Faction.HIVER)!=null,
                "recovered import node should accept personnel again");

        TopDownWorldEventRecord relief=TopDownWorldEventAuthority.scheduleCurated(world,"RELIEF_SHIPMENT",outage.endWorldTurn+1L);
        TopDownWorldEventAuthority.tick(world,null,relief.startWorldTurn);
        require("ACTIVE".equals(relief.physicalStatus)&&node.glyph=='!'
                        &&"false".equals(MapObjectState.stockValue(node.stockState,"eventClosed")),
                "relief should visibly repurpose the same facility without closing local intake");
        require(room.name.startsWith("Relief shipment /")
                        &&FactionImportNodeGenerationAuthority.arrivalPoint(world,Faction.HIVER)!=null,
                "relief distribution should remain accessible and use the exact receiving room");
        requireContains(String.join(" ",TopDownWorldEventAuthority.summaryLines(world)),
                "relief distribution","world-event physical readback");
    }

    private static World world(long seed) {
        World world=new World(seed,66,48);world.zoneType=ZoneType.NEUTRAL_RAIL_DEPOT;
        world.zoneName="Mutation Exchange";world.floor=5;world.zoneX=2;world.zoneY=2;
        for(int x=0;x<world.w;x++)for(int y=0;y<world.h;y++)world.tiles[x][y]='#';
        addRoom(world,"Central Plaza","neutral transit plaza",Faction.NONE,4,4,20,14);
        addRoom(world,"Cargo Receiving Hall","rail cargo warehouse and receiving control",Faction.HIVER,30,4,26,16);
        return world;
    }

    private static void addRoom(World world,String name,String description,Faction faction,int x,int y,int width,int height) {
        int index=world.rooms.size();Rectangle room=new Rectangle(x,y,width,height);world.carve(room);world.rooms.add(room);
        world.roomProfiles.set(index,new RoomProfile(name,description,60,faction,new String[]{"Trade chit"},new char[]{'Q'}));
        world.roomFactions.set(index,faction);world.roomSpecials.set(index,Boolean.FALSE);
        for(int px=room.x+1;px<room.x+room.width-1;px++)for(int py=room.y+1;py<room.y+room.height-1;py++)world.tiles[px][py]='.';
    }

    private static void requireContains(String actual,String expected,String label){require(actual!=null&&actual.contains(expected),label+" missing '"+expected+"': "+actual);}
    private static void require(boolean condition,String message){if(!condition)throw new AssertionError(message);}
    private Milestone04WorldEventFacilityMutationSmoke(){}
}
