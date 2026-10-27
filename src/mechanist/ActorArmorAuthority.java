package mechanist;

import java.util.*;

/** Shared worn-item coverage and mitigation, independent of player/NPC identity. */
final class ActorArmorAuthority {
    enum Region { HEAD, TORSO, ARMS, LEGS }
    private ActorArmorAuthority() { }
    static Region region(String part) {
        String text=part==null?"":part.toLowerCase(Locale.ROOT);
        if(text.contains("head")) return Region.HEAD;
        if(text.contains("arm") || text.contains("hand")) return Region.ARMS;
        if(text.contains("leg") || text.contains("foot")) return Region.LEGS;
        return Region.TORSO;
    }
    static CharacterEquipmentAndMedicalAuthority.EquipmentSlot slot(String item) {
        ItemDef definition=ItemCatalog.get(item);
        String text=item==null?"":(item+" "+(definition==null?"":definition.category)).toLowerCase(Locale.ROOT);
        if(contains(text,"helmet","hat","visor","goggle","mask","/head")) return CharacterEquipmentAndMedicalAuthority.EquipmentSlot.HEADGEAR;
        if(contains(text,"glove","gauntlet","bracer","shoulder","/arms")) return CharacterEquipmentAndMedicalAuthority.EquipmentSlot.GLOVES;
        if(contains(text,"boot","greave","/legs")) return CharacterEquipmentAndMedicalAuthority.EquipmentSlot.BOOTS;
        return CharacterEquipmentAndMedicalAuthority.EquipmentSlot.CLOTHES;
    }
    static boolean wearable(String item) {
        ItemDef definition=ItemCatalog.get(item);
        return definition!=null && (definition.category.startsWith("armor") || definition.category.startsWith("clothing"));
    }
    static boolean covers(String item,Region region) {
        if(item==null || item.isBlank()) return false;
        var slot=slot(item);
        if(slot==CharacterEquipmentAndMedicalAuthority.EquipmentSlot.HEADGEAR) return region==Region.HEAD;
        if(slot==CharacterEquipmentAndMedicalAuthority.EquipmentSlot.GLOVES) return region==Region.ARMS;
        if(slot==CharacterEquipmentAndMedicalAuthority.EquipmentSlot.BOOTS) return region==Region.LEGS;
        String text=item.toLowerCase(Locale.ROOT);
        return region==Region.TORSO || contains(text,"power armor","power armour","carapace","suppression armor") && region!=Region.HEAD;
    }
    static int rating(String item) {
        if(item==null || item.isBlank()) return 0;
        ItemDef definition=ItemCatalog.get(item);
        String text=(item+" "+(definition==null?"":definition.category)).toLowerCase(Locale.ROOT);
        int base=contains(text,"power armor","power armour")?8:contains(text,"carapace")?6:
                contains(text,"flak","riot","suppression","arbites patrol")?4:
                contains(text,"plate","rigid","breastplate","cuirass","harness")?3:
                contains(text,"padded","leather","patch armor","vest","armored","armour")?2:1;
        int quality=(ItemQuality.tierIndex(item)-2)/2;
        return Math.max(0,base+quality-(contains(text,"damaged","torn","battered","ruined")?1:0));
    }
    static int worn(String body,Map<CharacterEquipmentAndMedicalAuthority.EquipmentSlot,String> other,String part) {
        int rating=coversPart(body,part)?rating(body):0;
        if(other!=null) for(var entry:other.entrySet()) {
            if(!wearable(entry.getValue())) continue;
            if(entry.getKey()==CharacterEquipmentAndMedicalAuthority.EquipmentSlot.BACKPACK
                    || entry.getKey()==CharacterEquipmentAndMedicalAuthority.EquipmentSlot.LEFT_HAND
                    || entry.getKey()==CharacterEquipmentAndMedicalAuthority.EquipmentSlot.RIGHT_HAND) continue;
            if(coversPart(entry.getValue(),part)) rating=Math.max(rating,rating(entry.getValue()));
        }
        return rating;
    }
    static boolean coversPart(String item,String part) {
        if(item==null || item.isBlank()) return false;
        String text=item.toLowerCase(Locale.ROOT),body=part==null?"":part.toLowerCase(Locale.ROOT);
        var slot=slot(item);
        if(slot==CharacterEquipmentAndMedicalAuthority.EquipmentSlot.GLOVES) {
            if(text.contains("shoulder")) return body.contains("upper arm");
            if(text.contains("bracer")) return body.contains("lower arm");
            return body.contains("hand") || body.contains("finger") || text.contains("gauntlet") && body.contains("lower arm");
        }
        if(slot==CharacterEquipmentAndMedicalAuthority.EquipmentSlot.BOOTS) return body.contains("foot") || body.contains("toe") || text.contains("greave") && body.contains("lower leg");
        return covers(item,region(part));
    }
    static int protectionScore(String item) {int score=0;for(Region region:Region.values()) if(covers(item,region)) score+=rating(item);return score;}
    static int player(GamePanel game,String part) {return worn(game.equippedClothing==null?"":game.equippedClothing.name,game.equippedWearableSlots,part);}
    static int npc(NpcEntity npc,String part) {return worn(npc.equippedArmor,npc.combatSupport.worn,part);}
    static String hitPart(Random random) {
        int roll=random.nextInt(100);
        return roll<10?"Head":roll<70?"Torso":roll<85?(roll%2==0?"L Lower Arm":"R Hand"):(roll%2==0?"L Lower Leg":"R Foot");
    }
    private static boolean contains(String value,String... parts) {for(String part:parts) if(value.contains(part)) return true;return false;}
}
