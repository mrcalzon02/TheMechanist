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

class StampedRoomSpec {
    final String kind, name, descriptor, declaredPurposeId; final int scavengeChance; final Faction faction; final String[] loot; final char[] contents;
    StampedRoomSpec(String kind, String name, String descriptor, int scavengeChance, Faction faction, String[] loot, char[] contents){
        this.kind=kind; this.name=name; this.descriptor=descriptor; this.declaredPurposeId=ExplicitRoomTypeRequirementAuthority.purposeIdForStamp(kind, name, descriptor); this.scavengeChance=scavengeChance; this.faction=faction; this.loot=loot; this.contents=contents;
    }
    RoomProfile toProfile(ZoneType z, Random r){
        RoomProfile rp = RoomProfile.themedRoom(name, descriptor, scavengeChance, faction, loot, contents);
        rp.declaredPurposeId = declaredPurposeId;
        rp.featureText = InspectableFeatureTable.combinedFor(z, r, descriptor);
        return rp;
    }
    char primaryGlyph(){ return contents != null && contents.length > 0 ? contents[0] : 'q'; }
}
