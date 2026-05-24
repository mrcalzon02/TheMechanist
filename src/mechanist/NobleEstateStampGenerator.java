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

class NobleEstateStampGenerator {
    static final String[] ESTATE_PREFIX = {
            "House", "Manor", "Spire", "Palace", "Estate", "Vault"
    };

    static final String[] ESTATE_SUFFIX = {
            "Vhal", "Mordane", "Kestrix", "Valcior", "Dravus", "Helican"
    };

    static NobleEstateStamp generate(Random rng, int zoneWidth, int zoneHeight) {
        NobleEstateStamp s = new NobleEstateStamp(
                ESTATE_PREFIX[rng.nextInt(ESTATE_PREFIX.length)] + " " +
                ESTATE_SUFFIX[rng.nextInt(ESTATE_SUFFIX.length)]
        );

        s.securityTier = NobleEstateSecurityTier.values()[
                rng.nextInt(NobleEstateSecurityTier.values().length)
        ];

        s.hasPanicRoom = rng.nextBoolean();
        s.hasChapel = rng.nextBoolean();
        s.hasServitorWing = rng.nextBoolean();
        s.hasHiddenEscapeRoute = rng.nextInt(100) < 35;
        s.estimatedWealthLevel = 50 + rng.nextInt(950);
        s.trapDensity = 1 + rng.nextInt(6);

        int centerX = Math.max(8, zoneWidth / 2);
        int centerY = Math.max(8, zoneHeight / 2);

        s.centralVault = new Rectangle(centerX - 3, centerY - 3, 6, 6);

        s.estateRooms.add(new Rectangle(centerX - 8, centerY - 8, 16, 16));
        s.estateRooms.add(new Rectangle(centerX - 14, centerY - 4, 6, 8));
        s.estateRooms.add(new Rectangle(centerX + 8, centerY - 4, 6, 8));

        s.servantQuarters.add(new Rectangle(centerX - 18, centerY + 6, 8, 6));

        s.trapCorridors.add(new Rectangle(centerX - 2, centerY - 14, 4, 10));

        return s;
    }
}
