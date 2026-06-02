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

enum DoorType {
    OPEN_ARCHWAY("Open archway", '/', 0, "No lock; line-of-sight interruption only by smoke, crowds, or bad luck."),
    RAG_CURTAIN("Rag curtain", '/', 0, "Cheap civilian privacy. No real security."),
    HINGED_SCRAP_DOOR("Hinged scrap door", '|', 1, "Basic door; bash, open, or force later."),
    CHAINED_DOOR("Chained door", 'L', 2, "Requires key, cutters, lockpick, or a modest check."),
    PADLOCKED_GRATE("Padlocked grate", 'L', 3, "Lockpick/tool gated. Common in gang and maintenance rooms."),
    ADMINISTRATUM_COUNTER_GATE("Administratum counter gate", 'L', 3, "Paperwork barrier; intelligence and permits matter."),
    ARBITES_SECURITY_DOOR("Arbites security door", 'X', 5, "Keycard or serious intellect/security check."),
    GUARD_ARMORY_DOOR("Guard armory door", 'X', 5, "Issued-key or military access barrier."),
    MECHANICUS_RUNE_HATCH("Mechanicus rune hatch", 'X', 6, "Mechanics plus intellect; machine-cult access logic."),
    VENT_PANEL("Ventilation panel", 'V', 2, "Tool-gated crawlspace access; counts as a special corridor entry."),
    SEWER_BAR_GATE("Sewer bar gate", 'L', 3, "Rusted grate; tools, strength, or keys later."),
    NOBLE_SERVICE_LOCK("Noble service lock", 'X', 5, "High-security servant passage lock."),
    SEALED_VAULT_DOOR("Sealed vault door", 'X', 8, "Treasure-vault class security. Not for bare hands."),
    BROKEN_BULKHEAD("Broken bulkhead", '|', 1, "Damaged industrial door; noisy and unreliable.");
    final String label; final char symbol; final int security; final String note;
    DoorType(String label, char symbol, int security, String note){this.label=label;this.symbol=symbol;this.security=security;this.note=note;}
    static DoorType forZone(ZoneType z, Random r){
        if(z==ZoneType.SEWER_CONDUIT) return pick(r, OPEN_ARCHWAY, VENT_PANEL, SEWER_BAR_GATE, BROKEN_BULKHEAD);
        if(z==ZoneType.ARBITES_PRECINCT_EDGE) return pick(r, HINGED_SCRAP_DOOR, ARBITES_SECURITY_DOOR, ARBITES_SECURITY_DOOR, PADLOCKED_GRATE);
        if(z==ZoneType.IMPERIAL_GUARD_BILLET) return pick(r, HINGED_SCRAP_DOOR, GUARD_ARMORY_DOOR, PADLOCKED_GRATE, CHAINED_DOOR);
        if(z==ZoneType.MECHANICUS_FORGE_CLOISTER || z==ZoneType.MECHANICUS_RELIC_DUCT) return pick(r, MECHANICUS_RUNE_HATCH, BROKEN_BULKHEAD, VENT_PANEL, HINGED_SCRAP_DOOR);
        if(z==ZoneType.NOBLE_SERVICE_SPINE) return pick(r, NOBLE_SERVICE_LOCK, SEALED_VAULT_DOOR, ADMINISTRATUM_COUNTER_GATE, HINGED_SCRAP_DOOR);
        if(z==ZoneType.ADMINISTRATUM_ARCHIVE) return pick(r, ADMINISTRATUM_COUNTER_GATE, CHAINED_DOOR, HINGED_SCRAP_DOOR, RAG_CURTAIN);
        if(z==ZoneType.GANGER_TURF) return pick(r, RAG_CURTAIN, CHAINED_DOOR, PADLOCKED_GRATE, BROKEN_BULKHEAD);
        if(z==ZoneType.MUTANT_WARRENS || z==ZoneType.TRASH_WARREN) return pick(r, OPEN_ARCHWAY, RAG_CURTAIN, BROKEN_BULKHEAD, VENT_PANEL);
        return pick(r, OPEN_ARCHWAY, RAG_CURTAIN, HINGED_SCRAP_DOOR, CHAINED_DOOR);
    }
    static DoorType pick(Random r, DoorType... vals){ return vals[r.nextInt(vals.length)]; }
}
