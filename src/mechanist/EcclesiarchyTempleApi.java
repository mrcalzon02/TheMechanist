package mechanist;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import javax.sound.sampled.*;
import java.io.*;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;

class EcclesiarchyTempleApi {
    private EcclesiarchyTempleApi() {}

    static boolean isTempleRoom(RoomProfile rp) {
        if (rp == null) return false;
        String hay = ((rp.name == null ? "" : rp.name) + " " + (rp.descriptor == null ? "" : rp.descriptor)).toLowerCase(Locale.ROOT);
        return hay.contains("cult imperialis temple") || hay.contains("ecclesiarchy temple") || hay.contains("ministorum nave");
    }

    static boolean templeAlreadyStamped(java.util.List<RoomProfile> profiles) {
        if (profiles == null) return false;
        for (RoomProfile rp : profiles) if (isTempleRoom(rp)) return true;
        return false;
    }

    static RoomProfile templeProfile(ZoneType z, Random r) {
        String zone = z == null ? "hive zone" : z.label;
        String desc = "Cult Imperialis temple stamp near the central plaza: long wide nave, pillar rows, saint alcoves, candle racks, holy relic niches, prayer nooks, donation box, and supplicant kitchen feeding priests, pilgrims, and Sister guards in " + zone;
        RoomProfile rp = RoomProfile.themedRoom("Cult Imperialis Temple Nave", desc, 62, Faction.MINISTORUM,
            new String[]{"pilgrim ration", "prayer candle bundle", "saint token", "donation chit", "devotional pamphlet", "thin soup ration"},
            new char[]{'I','W','b','T','q','$'});
        rp.featureText = "Stamped Ministorum church: nave pillars/columns, relic alcoves, candle racks, prayer nooks, saint icons, donation box, supplicant kitchen, protected head cleric, priests, pilgrims, and Sororitas guard presence.";
        return rp;
    }

    static ArrayList<String> auditLines(World w) {
        ArrayList<String> lines = new ArrayList<>();
        int temples=0, head=0, priests=0, sisters=0;
        if (w != null) {
            for (RoomProfile rp : w.roomProfiles) if (isTempleRoom(rp)) temples++;
            for (NpcEntity n : w.npcs) {
                if (n.isProtectedCleric()) head++;
                else if (n.faction == Faction.MINISTORUM) priests++;
                else if (n.faction == Faction.SORORITAS) sisters++;
            }
        }
        lines.add("Ecclesiarchy temple audit: templeRooms=" + temples + " protectedHeadClerics=" + head + " priestsPilgrims=" + priests + " sororitasGuards=" + sisters);
        lines.add("Required service: head cleric is non-hostile/non-targetable and offers 24h Imperial forgiveness prayer with hunger/water/sleep/fatigue cost and civil Imperial standing recovery.");
        return lines;
    }
}
