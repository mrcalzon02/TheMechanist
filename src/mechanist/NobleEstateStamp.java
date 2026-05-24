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

class NobleEstateStamp {
    String estateName;
    NobleEstateSecurityTier securityTier = NobleEstateSecurityTier.MINOR_HOUSE;
    ArrayList<Rectangle> estateRooms = new ArrayList<>();
    ArrayList<Rectangle> servantQuarters = new ArrayList<>();
    ArrayList<Rectangle> trapCorridors = new ArrayList<>();
    Rectangle centralVault;
    boolean hasPanicRoom;
    boolean hasChapel;
    boolean hasServitorWing;
    boolean hasHiddenEscapeRoute;
    int estimatedWealthLevel;
    int trapDensity;

    NobleEstateStamp(String estateName) {
        this.estateName = estateName;
    }
}
