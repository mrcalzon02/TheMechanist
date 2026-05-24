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

enum Faction {
    NONE("None"),
    SCAVENGER("Scavenger"),
    ADMINISTRATUM("Administratum"),
    ARBITES("Adeptus Arbites"),
    IMPERIAL_GUARD("Imperial Guard"),
    MINISTORUM("Adeptus Ministorum / Ecclesiarchy"),
    INN("Imperial News Network"),
    SORORITAS("Adepta Sororitas Temple Guard"),
    MECHANICUS("Local Mechanicus Forge"),
    MECHANICUS_CLOISTER_RED("Red Cloister"),
    MECHANICUS_CLOISTER_RUST("Rust Cloister"),
    MECHANICUS_CLOISTER_VOID("Void Cloister"),
    HIVER("Hiver"),
    HIVER_BLOCK_AUREL("Hiver Block Aurel"),
    HIVER_BLOCK_MARROW("Hiver Block Marrow"),
    HIVER_BLOCK_SUMPLEDGER("Hiver Block Sumpledger"),
    BANDIT("Ganger"),
    GANGER_IRON_RATS("Iron Rats"),
    GANGER_BLACK_SUMP("Black Sump Knives"),
    GANGER_CANDLE_JACKS("Candle Jacks"),
    GANGER_RED_GRIN("Red Grin Crew"),
    GANGER_CHAIN_SAINTS("Chain Saints"),
    GANGER_ASH_MARKET("Ash Market Teeth"),
    GANGER_WIRE_WOLVES("Wire Wolves"),
    GANGER_DROWNED_9TH("Drowned Ninth"),
    NOBLE("Noble"),
    NOBLE_HOUSE_VARN("House Varn"),
    NOBLE_HOUSE_KASTOR("House Kastor"),
    NOBLE_HOUSE_MORVAIN("House Morvain"),
    NOBLE_HOUSE_CYRA("House Cyra"),
    NOBLE_HOUSE_DRAKE("House Drake"),
    NOBLE_HOUSE_TOLL("House Toll"),
    NOBLE_HOUSE_OSSUARY("House Ossuary"),
    MUTANT("Local Mutant Pack"),
    CULTIST("Local Cult Cell"),
    ROGUE_MACHINE("Rogue Machine"),
    HERETIC("Heretic");
    final String label;
    Faction(String label){ this.label = label; }
    static Faction[] visibleFactions(){ return new Faction[]{SCAVENGER,ADMINISTRATUM,ARBITES,IMPERIAL_GUARD,MINISTORUM,INN,SORORITAS,MECHANICUS,HIVER,HIVER_BLOCK_AUREL,HIVER_BLOCK_MARROW,HIVER_BLOCK_SUMPLEDGER,BANDIT,GANGER_IRON_RATS,GANGER_BLACK_SUMP,GANGER_CANDLE_JACKS,GANGER_RED_GRIN,GANGER_CHAIN_SAINTS,GANGER_ASH_MARKET,GANGER_WIRE_WOLVES,GANGER_DROWNED_9TH,NOBLE,NOBLE_HOUSE_VARN,NOBLE_HOUSE_KASTOR,NOBLE_HOUSE_MORVAIN,NOBLE_HOUSE_CYRA,NOBLE_HOUSE_DRAKE,NOBLE_HOUSE_TOLL,NOBLE_HOUSE_OSSUARY,MUTANT,CULTIST,ROGUE_MACHINE,HERETIC}; }
}
