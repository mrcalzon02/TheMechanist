package mechanist;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Locale;

/** Applies and reverses event-owned changes to existing rooms and facility markers. */
final class WorldEventFacilityMutationAuthority {
    static final String MARKER_TYPE = "world-event-facility-marker";

    private WorldEventFacilityMutationAuthority() { }

    static void activate(World world, TopDownWorldEventRecord event) {
        if (world == null || event == null) return;
        if ("ACTIVE".equals(event.physicalStatus)) {
            ensureActive(world,event);
            return;
        }
        MapObjectState object=targetObject(world,event);
        int roomId=object==null?-1:roomId(world,object);
        if (roomId<0) roomId=targetRoom(world,event);
        if (object==null&&roomId>=0) {
            Point point=markerPoint(world,roomId);
            if(point!=null){
                object=new MapObjectState();object.id="WORLD-EVENT-MARKER-"+event.id;object.type=MARKER_TYPE;
                object.label="Temporary "+event.title+" facility marker";object.x=point.x;object.y=point.y;object.glyph='!';
                object.stockState="roomId="+roomId+";under="+(int)world.tiles[point.x][point.y];
                world.mapObjects.add(object);event.createdMutationObject=true;
            }
        }
        event.mutationObjectId=object==null?"":safe(object.id);
        event.mutationRoomId=roomId;
        event.mutationMode=mode(event);
        event.originalObjectLabel=object==null?"":safe(object.label);
        event.originalObjectStockState=object==null?"":safe(object.stockState);
        event.originalObjectGlyph=object==null?0:object.glyph;
        RoomProfile room=profile(world,roomId);
        if(room!=null){
            event.originalRoomName=safe(room.name);event.originalRoomDescriptor=safe(room.descriptor);
            event.originalRoomFeatureText=safe(room.featureText);
        }
        event.physicalStatus="ACTIVE";
        ensureActive(world,event);
        String objectName=object==null?"no physical marker":safe(object.label);
        String roomName=room==null?"no compatible room":safe(room.name);
        event.mutationSummary=event.mutationMode+" at "+objectName+" in "+roomName
                +(blocksIntake(event)?"; external personnel intake is closed.":"; the facility remains locally accessible.");
    }

    static void ensureActive(World world, TopDownWorldEventRecord event) {
        if(world==null||event==null||!"ACTIVE".equals(event.physicalStatus))return;
        MapObjectState object=objectById(world,event.mutationObjectId);
        if(object!=null){
            String base=safe(event.originalObjectLabel).isBlank()?"Temporary facility marker":event.originalObjectLabel;
            object.label=event.title+" / "+base;
            object.stockState=MapObjectState.setStockFlag(object.stockState,"status",event.mutationMode);
            object.stockState=MapObjectState.setStockFlag(object.stockState,"worldEventId",event.id);
            object.stockState=MapObjectState.setStockFlag(object.stockState,"eventClosed",Boolean.toString(blocksIntake(event)));
            object.glyph='!';
            if(world.inBounds(object.x,object.y))world.tiles[object.x][object.y]=object.glyph;
        }
        RoomProfile room=profile(world,event.mutationRoomId);
        if(room!=null){
            String originalName=safe(event.originalRoomName).isBlank()?safe(room.name):event.originalRoomName;
            String originalDescriptor=safe(event.originalRoomDescriptor).isBlank()?safe(room.descriptor):event.originalRoomDescriptor;
            String originalFeatures=safe(event.originalRoomFeatureText).isBlank()?safe(room.featureText):event.originalRoomFeatureText;
            room.name=event.title+" / "+originalName;
            room.descriptor=originalDescriptor+"; temporarily used for "+event.mutationMode+".";
            room.featureText=originalFeatures+" Event condition: "+event.title+"; "+event.roomMutationHook;
        }
    }

    static void recover(World world, TopDownWorldEventRecord event) {
        if(world==null||event==null||"RECOVERED".equals(event.physicalStatus))return;
        MapObjectState object=objectById(world,event.mutationObjectId);
        if(object!=null&&event.createdMutationObject){
            char under=MapObjectState.underlyingTileFromStock(object.stockState);
            if(world.inBounds(object.x,object.y))world.tiles[object.x][object.y]=under==0?'.':under;
            world.mapObjects.remove(object);
        }else if(object!=null){
            object.label=event.originalObjectLabel;object.stockState=event.originalObjectStockState;
            object.glyph=event.originalObjectGlyph==0?'q':(char)event.originalObjectGlyph;
            if(world.inBounds(object.x,object.y))world.tiles[object.x][object.y]=object.glyph;
        }
        RoomProfile room=profile(world,event.mutationRoomId);
        if(room!=null){
            if(!safe(event.originalRoomName).isBlank())room.name=event.originalRoomName;
            if(!safe(event.originalRoomDescriptor).isBlank())room.descriptor=event.originalRoomDescriptor;
            if(!safe(event.originalRoomFeatureText).isBlank())room.featureText=event.originalRoomFeatureText;
        }
        event.physicalStatus="RECOVERED";
        event.mutationSummary="Restored "+(safe(event.originalObjectLabel).isBlank()?"the temporary event facility":event.originalObjectLabel)
                +" and "+(safe(event.originalRoomName).isBlank()?"its room":event.originalRoomName)+" to ordinary use.";
    }

    static void restorePersistedState(World world) {
        if(world==null)return;
        for(TopDownWorldEventRecord event:world.topDownWorldEvents){
            if(event==null)continue;
            if("ACTIVE".equals(event.status)&&"ACTIVE".equals(event.physicalStatus))ensureActive(world,event);
            else if("RECOVERED".equals(event.status)&&!"RECOVERED".equals(event.physicalStatus))recover(world,event);
        }
    }

    private static MapObjectState targetObject(World world,TopDownWorldEventRecord event){
        MapObjectState node=FactionImportNodeGenerationAuthority.primaryNode(world,event.targetFaction);
        if(node!=null)return node;
        for(MapObjectState object:world.mapObjects)if(object!=null&&"faction-market".equals(object.type)){
            String category=MapObjectState.stockValue(object.stockState,"vendorCategory").toLowerCase(Locale.ROOT);
            if(event.marketCategory.toLowerCase(Locale.ROOT).contains("medicine")&&!category.contains("medical"))continue;
            return object;
        }
        return null;
    }

    private static int targetRoom(World world,TopDownWorldEventRecord event){
        for(int i=1;i<world.rooms.size();i++){
            RoomProfile room=profile(world,i);if(room==null)continue;
            String text=(safe(room.name)+" "+safe(room.descriptor)+" "+safe(room.featureText)).toLowerCase(Locale.ROOT);
            if(event.eventType.contains("QUARANTINE")&&contains(text,"clinic","medical"))return i;
            if(event.eventType.contains("TITHING")&&contains(text,"market","store","office"))return i;
            if(contains(text,"cargo","import","rail","receiving","warehouse"))return i;
        }
        return world.rooms.size()>1?1:(world.rooms.isEmpty()?-1:0);
    }

    private static Point markerPoint(World world,int roomId){
        if(roomId<0||roomId>=world.rooms.size())return null;Rectangle room=world.rooms.get(roomId);
        for(int y=room.y+1;y<room.y+room.height-1;y++)for(int x=room.x+1;x<room.x+room.width-1;x++)
            if(world.inBounds(x,y)&&world.walkable(x,y)&&world.mapObjectAt(x,y)==null&&world.npcAt(x,y)==null)return new Point(x,y);
        return null;
    }

    private static int roomId(World world,MapObjectState object){
        try{String stored=MapObjectState.stockValue(object.stockState,"roomId");if(!stored.isBlank())return Integer.parseInt(stored);}catch(Exception ignored){}
        for(int i=0;i<world.rooms.size();i++)if(world.rooms.get(i).contains(object.x,object.y))return i;return-1;
    }
    private static MapObjectState objectById(World world,String id){if(world==null||safe(id).isBlank())return null;for(MapObjectState object:world.mapObjects)if(object!=null&&id.equals(object.id))return object;return null;}
    private static RoomProfile profile(World world,int roomId){return world==null||roomId<0||roomId>=world.roomProfiles.size()?null:world.roomProfiles.get(roomId);}
    private static String mode(TopDownWorldEventRecord event){return switch(event.eventType){case"RELIEF_SHIPMENT"->"relief distribution";case"INFRASTRUCTURE_REPAIR"->"repair works";case"TRAIN_OUTAGE"->"closed train intake";case"EXPORT_BAN"->"export inspection";case"TITHING_DECREE"->"tithe collection";case"QUARANTINE"->"quarantine checkpoint";case"SUPPLY_SHOCK"->"shortage control";default->"civic observance";};}
    private static boolean blocksIntake(TopDownWorldEventRecord event){return event.importClosed||event.eventType.equals("TRAIN_OUTAGE")||event.eventType.equals("QUARANTINE");}
    private static boolean contains(String text,String...terms){for(String term:terms)if(text.contains(term))return true;return false;}
    private static String safe(String value){return value==null?"":value;}
}
