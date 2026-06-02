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

class StampedModuleSpec {
    final String name, corridorDress;
    final int corridorLength, corridorWidth, roomWidth, roomHeight, maxRooms, preferredSide, laneOffset;
    final StampedRoomSpec[] rooms;
    StampedModuleSpec(String name, String corridorDress, int corridorLength, int corridorWidth, int roomWidth, int roomHeight, int preferredSide, int laneOffset, StampedRoomSpec... rooms){
        this.name=name; this.corridorDress=corridorDress; this.corridorLength=corridorLength; this.corridorWidth=corridorWidth; this.roomWidth=roomWidth; this.roomHeight=roomHeight; this.preferredSide=preferredSide; this.laneOffset=laneOffset; this.rooms=rooms; this.maxRooms=rooms==null?0:rooms.length;
    }
}
