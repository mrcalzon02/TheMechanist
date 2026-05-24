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

class DebugLog {
    static PrintWriter currentOut;
    static final Object LOCK = new Object();
    static String version = "unknown";
    static long startMillis = System.currentTimeMillis();
    static boolean closed = false;

    static void init(String buildVersion){
        synchronized(LOCK){
            version = buildVersion;
            startMillis = System.currentTimeMillis();
            closed = false;
            try{
                Path dir = Paths.get("logs");
                Files.createDirectories(dir);
                Path current = dir.resolve("current.log");
                Path previous = dir.resolve("previous.log");
                if (Files.exists(current)) {
                    try { Files.deleteIfExists(previous); } catch (IOException ignored) {}
                    Files.move(current, previous, StandardCopyOption.REPLACE_EXISTING);
                }
                currentOut = new PrintWriter(new FileWriter(current.toFile(), false), true);
                writeHeader();
                log("The Mechanist logging initialized. Single active log is logs/current.log; prior run rotated to logs/previous.log.");
            } catch(IOException e){
                e.printStackTrace();
            }
        }
    }

    static void writeHeader(){
        section("RUN HEADER");
        raw("version=" + version);
        raw("started=" + stampLong());
        raw("java.version=" + System.getProperty("java.version"));
        raw("java.vendor=" + System.getProperty("java.vendor"));
        raw("java.vm.name=" + System.getProperty("java.vm.name"));
        raw("os.name=" + System.getProperty("os.name"));
        raw("os.version=" + System.getProperty("os.version"));
        raw("os.arch=" + System.getProperty("os.arch"));
        raw("user.dir=" + System.getProperty("user.dir"));
        raw("available.processors=" + Runtime.getRuntime().availableProcessors());
        raw("memory.max.bytes=" + Runtime.getRuntime().maxMemory());
        raw("memory.total.bytes=" + Runtime.getRuntime().totalMemory());
        raw("memory.free.bytes=" + Runtime.getRuntime().freeMemory());
        section("EVENT STREAM");
    }

    static String stamp(){ return new SimpleDateFormat("HH:mm:ss.SSS").format(new Date()); }
    static String stampLong(){ return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()); }

    static void log(String s){ write("INFO", s, true); }
    static void warn(String system, String s){ write("WARN/" + system, s, true); }
    static void audit(String system, String s){ write("AUDIT/" + system, s, false); }
    static void metric(String system, String s){ write("METRIC/" + system, s, false); }
    static void settings(String system, String s){ write("SETTINGS/" + system, s, false); }

    static void error(String system, String message, Throwable t){
        synchronized(LOCK){
            String head = "[" + stamp() + "] ERROR/" + system + " " + message;
            System.err.println(head);
            if(currentOut!=null) {
                currentOut.println(head);
                if(t!=null) t.printStackTrace(currentOut);
                currentOut.flush();
            }
        }
    }

    static void write(String level, String s, boolean console){
        String line="["+stamp()+"] "+level+" "+s;
        synchronized(LOCK){
            if(console) System.out.println(line);
            if(currentOut!=null) currentOut.println(line);
        }
    }

    static void section(String name){ raw("\n==== " + name + " ===="); }
    static void raw(String line){ if(currentOut!=null) currentOut.println(line); }

    static void shutdown(String reason){
        synchronized(LOCK){
            if(closed) return;
            closed = true;
            try{
                section("RUN FOOTER");
                raw("shutdown.reason=" + reason);
                raw("ended=" + stampLong());
                raw("uptime.ms=" + (System.currentTimeMillis() - startMillis));
                raw("memory.total.bytes=" + Runtime.getRuntime().totalMemory());
                raw("memory.free.bytes=" + Runtime.getRuntime().freeMemory());
                raw("memory.max.bytes=" + Runtime.getRuntime().maxMemory());
                if(currentOut!=null){ currentOut.flush(); currentOut.close(); currentOut=null; }
            } catch(Throwable t){
                t.printStackTrace();
            }
        }
    }
}


/* =========================================================
 * 0.9.07m — Noble Estate Stamp Foundation
 * ========================================================= */
