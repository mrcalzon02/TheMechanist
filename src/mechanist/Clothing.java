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
}
