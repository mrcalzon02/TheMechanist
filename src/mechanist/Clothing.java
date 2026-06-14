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

class Clothing {
    String name;
    Faction alignedFaction;
    int disguiseBase;
    int defense;
    boolean damaged;
    Clothing(String name, Faction alignedFaction, int disguiseBase, int defense, boolean damaged) {
        this.name=name; this.alignedFaction=alignedFaction; this.disguiseBase=disguiseBase; this.defense=defense; this.damaged=damaged;
    }
    static Clothing scavengerRags(){ return new Clothing("Scavenger rags", Faction.SCAVENGER, 35, 1, false); }
    static Clothing banditColors(boolean damaged){ return new Clothing(damaged ? "Damaged bandit colors" : "Bandit colors", Faction.BANDIT, damaged ? 28 : 52, damaged ? 1 : 2, damaged); }
    static Clothing arbitesCoat(boolean damaged){ return new Clothing(damaged ? "Damaged Arbites coat" : "Arbites patrol coat", Faction.ARBITES, damaged ? 18 : 45, damaged ? 2 : 4, damaged); }
    static Clothing hiverWorkwear(boolean damaged){ return new Clothing(damaged ? "Damaged hiver workwear" : "Hiver workwear", Faction.HIVER, damaged ? 35 : 58, damaged ? 1 : 2, damaged); }
    static Clothing fromItemName(String itemName) {
        String safe = itemName == null || itemName.isBlank() ? "Unidentified clothing" : itemName;
        String low = safe.toLowerCase(java.util.Locale.ROOT);
        boolean damaged = low.contains("damaged") || low.contains("torn") || low.contains("ruined");
        if (low.contains("scavenger") && low.contains("rag")) return scavengerRags();
        if (low.contains("bandit") && low.contains("color")) return banditColors(damaged);
        if (low.contains("arbites") && low.contains("coat")) return arbitesCoat(damaged);
        if (low.contains("hiver") && low.contains("workwear")) return hiverWorkwear(damaged);
        int defense = low.contains("power armor") || low.contains("power armour") ? 5
      : low.contains("armor") || low.contains("armour") ? 3
      : low.contains("coat") || low.contains("uniform") ? 2 : 1;
        return new Clothing(safe, Faction.NONE, 0, Math.max(0, defense - (damaged ? 1 : 0)), damaged);
    }
}
