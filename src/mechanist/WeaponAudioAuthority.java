package mechanist;

import java.util.Locale;

/** Weapon sound identity follows the authored catalog family before legacy display-name hints. */
final class WeaponAudioAuthority {
    private WeaponAudioAuthority() { }
    static String cue(String weapon,boolean ranged) {
        ItemDef definition=ItemCatalog.get(weapon);
        String name=(definition==null?ItemQuality.stripManufacturingIdentity(weapon==null?"":weapon):definition.name).toLowerCase(Locale.ROOT);
        String category=definition==null?"":definition.category.toLowerCase(Locale.ROOT);
        if(category.contains("/plasma"))return "weapon_plasma";
        if(category.contains("/flame") || category.contains("/melta"))return "weapon_flame";
        if(category.contains("/las"))return "weapon_las";
        if(category.contains("/chain"))return "weapon_chain";
        if(category.contains("/bolt") || category.contains("/explosive") || name.contains("thunder"))return "weapon_thundering";
        if(category.contains("/shock") || category.contains("/arc") || category.contains("/power"))return "weapon_lightning";
        if(category.contains("/ranged") && (category.contains("/heavy") || name.contains("autocannon")))return "weapon_thundering";
        // Uncatalogued legacy equipment still has a conservative name-based route.
        if(has(name,"plasma"))return "weapon_plasma";
        if(has(name,"flamer","flame","melta","inferno","promethium"))return "weapon_flame";
        if(has(name,"lasgun","laspistol","lascarbine","longlas","laser","las-","las "))return "weapon_las";
        if(has(name,"chainsword","chainaxe","chainsaw","chain bayonet","chainblade","chain sword","chain axe"))return "weapon_chain";
        if(has(name,"thunder","bolter","autocannon","cannon","rocket","grenade","heavy stubber","mass-reactive"))return "weapon_thundering";
        if(has(name,"arc ","shock","lightning","electr","voltaic"))return "weapon_lightning";
        if(has(name,"sword","knife","blade","axe","dagger","claw","talon","cutter","saw","bayonet","spear"))return "weapon_slice";
        if(ranged || category.contains("/ranged") || has(name,"pistol","gun","rifle","stub","shot","revolver","webber","needle"))return "weapon_shot";
        return "weapon_bash";
    }
    private static boolean has(String value,String... terms){for(String term:terms)if(value.contains(term))return true;return false;}
}
