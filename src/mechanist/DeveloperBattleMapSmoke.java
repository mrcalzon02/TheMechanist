package mechanist;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.*;
import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;

/** Real facility, commands, NPC turns, assets and GamePanel paint; no demonstration renderer. */
final class DeveloperBattleMapSmoke {
    public static void main(String[] args)throws Exception {
        verifyExposureBuffer();
        Path output=Path.of(args.length==0?"diagnostics/multilevel-battle-20261028/captures":args[0]);Files.createDirectories(output);
        GamePanel campaign=new GamePanel();campaign.timer.stop();campaign.world=new World(19191,32,24);
        for(char[] c:campaign.world.tiles)Arrays.fill(c,'.');campaign.active=Candidate.random(new Random(3));campaign.seed=19191;
        campaign.singlePlayerSectorBridge=new SinglePlayerSectorRuntimeBridge(new SectorManager());
        DeveloperFacilityAuthority.Session session=null;
        try {
            session=DeveloperFacilityAuthority.enter(campaign);GamePanel game=session.facility();game.timer.stop();
            game.options.soundEnabled=false;game.options.musicEnabled=false;game.options.tileIconRendering=true;game.setSize(1280,800);
            DeveloperFacilityAuthority.execute(game,"battlemap",java.util.List.of("multi"));
            require(DeveloperBattleMapAuthority.active(game.world),"Battle map command did not activate terrain");
            Rectangle room=DeveloperFacilityAuthority.layout(game).rooms.get("combat");int cx=room.x+25,cy=room.y+13;
            require(DeveloperBattleMapAuthority.height(game.world,cx,cy)==0,"Ground lane missing");
            require(DeveloperBattleMapAuthority.height(game.world,cx-7,cy)==1,"Terrace missing");
            require(DeveloperBattleMapAuthority.height(game.world,cx,cy+11)==2,"Upper gallery missing");
            require(!DeveloperBattleMapAuthority.step(game.world,cx-4,cy+3,cx-5,cy+3),"Ledge must block movement");
            game.playerX=cx-4;game.playerY=cy+3;
            require(!MovementExecutionAuthority.executeStep(game,-1,0,"battle-map ledge test",false).success(),"Normal player movement must reject the ledge");
            for(int i=0;i<8;i++)require(DeveloperBattleMapAuthority.step(game.world,cx,cy+1+i,cx,cy+2+i),"Stair disconnected at "+i);
            require(!StructuralScaleCombatAuthority.clearLine(game.world,cx-4,cy+3,cx-7,cy+3),"Terrace side must block low firing lane");
            var ceiling=DeveloperBattleMapAuthority.cast(game.world,cx+.5,.58,cy+.5,0,1,0,20);
            require(ceiling!=null && Math.abs(ceiling.height()-10)<1e-8,"Open arena must retain ten-storey headspace");
            require(WorldAmbientLightAuthority.at(game.world,cx,cy,4)==28,"Open arena must receive passive light");
            require(WorldAmbientLightAuthority.at(game.world,3,3,4)==4,"Enclosed hub must retain its own ambient");
            World sewer=new World(3,3,3);sewer.sewerLayer=true;sewer.tiles[1][1]='_';
            require(WorldAmbientLightAuthority.at(sewer,1,1,2)==2,"Enclosed sewer must not receive open-space light");
            require(ViewportDarknessAuthority.expose(0xFF000000)==ViewportDarknessAuthority.DARKEST,"Darkest exposure must be #001a33");
            require(ViewportDarknessAuthority.expose(0xFF080808)!=ViewportDarknessAuthority.expose(0xFF000000),"Low-light detail must survive exposure");
            BufferedImage black=new BufferedImage(2,2,BufferedImage.TYPE_INT_RGB);Graphics2D blackPainter=black.createGraphics();
            try{new ViewportDarknessAuthority.SceneBuffer().paint(blackPainter,new Rectangle(0,0,2,2),
                    layer -> { layer.setColor(Color.BLACK); layer.fillRect(0,0,2,2); });}finally{blackPainter.dispose();}
            require(black.getRGB(0,0)==ViewportDarknessAuthority.DARKEST,"2D compositing must produce the exact blue darkness floor");
            var layout=DeveloperFacilityAuthority.layout(game);layout.cyberneticPercent=0;
            DeveloperCombatScenarioAuthority.prepare(game,10,"rifles",71000);
            require(game.world.npcs.stream().filter(n->n.id.startsWith("MATCH-")).map(n->DeveloperBattleMapAuthority.height(game.world,n.x,n.y)).distinct().count()==3,"Teams must occupy all three heights");
            require(game.images.getCompiledTileImage(game.world,cx,cy,'.',true)!=null,"Existing floor art missing");
            require(StairTileArtAuthority.direction(game.world,cx,cy+2)==StairTileArtAuthority.Direction.SOUTH,"Gallery stairs must ascend south");
            require(StairTileArtAuthority.direction(game.world,cx+1,cy)==StairTileArtAuthority.Direction.EAST,"East stairs must ascend east");
            require(StairTileArtAuthority.direction(game.world,cx-2,cy)==StairTileArtAuthority.Direction.WEST,"West stairs must ascend west");
            BufferedImage stairs=StairTileArtAuthority.image(game.images,game.world,cx+1,cy);
            require(stairs!=null && stairs!=game.images.getTile('.'),"Stairs must use authored artwork, not the floor fallback");
            BufferedImage artPreview=new BufferedImage(1024,280,BufferedImage.TYPE_INT_RGB);Graphics2D artPainter=artPreview.createGraphics();
            try {
                BufferedImage source=game.images.getTile("stair_flight",'.');
                for(var direction:StairTileArtAuthority.Direction.values()) {
                    artPainter.drawImage(StairTileArtAuthority.rotated(source,direction),direction.ordinal()*256,0,256,256,null);
                    artPainter.setColor(Color.WHITE);artPainter.drawString("ASCENT "+direction,direction.ordinal()*256+8,273);
                }
            }finally{artPainter.dispose();}
            ImageIO.write(artPreview,"png",output.resolve("09-stair-orientations.png").toFile());
            BufferedImage marker=new BufferedImage(3,3,BufferedImage.TYPE_INT_ARGB);marker.setRGB(1,0,0xFFFF0000);
            require(StairTileArtAuthority.rotated(marker,StairTileArtAuthority.Direction.EAST).getRGB(2,1)==0xFFFF0000,"North marker must rotate to east");
            require(StairTileArtAuthority.rotated(marker,StairTileArtAuthority.Direction.SOUTH).getRGB(1,2)==0xFFFF0000,"North marker must rotate to south");
            require(StairTileArtAuthority.rotated(marker,StairTileArtAuthority.Direction.WEST).getRGB(0,1)==0xFFFF0000,"North marker must rotate to west");
            NpcEntity npc=game.world.npcs.stream().filter(n->n.id.startsWith("MATCH-")).findFirst().orElseThrow();
            require(game.images.getNpcPortraitFor(npc)!=null,"Existing character art missing");
            game.screen=GamePanel.Screen.GAME;game.panelMode=GamePanel.PanelMode.NONE;game.active.stats.put("Vision",42);
            double[][] poses={{cx,cy-3,90,0},{cx-7,cy-2,35,-12},{cx,cy+11,-90,-20},{cx,cy,90,70}};
            for(int i=0;i<poses.length;i++) {
                game.playerX=(int)poses[i][0];game.playerY=(int)poses[i][1];game.firstPersonRenderViewport=new FirstPersonRenderViewport();
                var field=FirstPersonRenderViewport.class.getDeclaredField("camera");field.setAccessible(true);FirstPersonCamera camera=(FirstPersonCamera)field.get(game.firstPersonRenderViewport);
                camera.addYawPitch(Math.toRadians(poses[i][2]),Math.toRadians(poses[i][3]));
                game.options.doomModeEnabled=true;capture(game,output,String.format("%02d-first-person",i+1));
            }
            game.options.doomModeEnabled=false;capture(game,output,"05-planner-existing-floor");
            World occupied=game.world;int beforeX=game.playerX,beforeY=game.playerY,beforeTurn=game.turn;
            require(PlannerLevelViewAuthority.handleKey(game,java.awt.event.KeyEvent.VK_PAGE_UP),"Planner Z-up key must be handled");
            require(PlannerLevelViewAuthority.level(game)==1 && PlannerLevelViewAuthority.inspecting(game),"Planner must inspect the next height");
            require(game.world==occupied && game.playerX==beforeX && game.playerY==beforeY && game.turn==beforeTurn,"Z browsing must not move or advance the player");
            capture(game,output,"07-planner-z-plus-1");
            PlannerLevelViewAuthority.handleKey(game,java.awt.event.KeyEvent.VK_PAGE_UP);capture(game,output,"08-planner-z-plus-2");
            PlannerLevelViewAuthority.handleKey(game,java.awt.event.KeyEvent.VK_HOME);
            verifyRenderErrorReporting(campaign);
            require(!PlannerLevelViewAuthority.inspecting(game),"Home must return to occupied level");
            for(int turn=0;turn<20;turn++)game.advanceTurnBody("");
            require(ActorCombatAuthority.diagnostics(game).damage>0,"Normal NPC combat must execute on raised terrain");
            capture(game,output,"06-planner-after-20-turns");
            Files.writeString(output.resolve("results.txt"),"PASS: same GamePanel/world/actors/assets; elevations 0/1/2; stairs; ledges; vertical LOS; ambient; NPC combat.\n"+ActorCombatAuthority.diagnostics(game).summary()+"\n");
            DeveloperFacilityAuthority.execute(game,"battlemap",java.util.List.of("flat"));
            require(!DeveloperBattleMapAuthority.active(game.world),"Flat reset did not clear terrain");
            require(StairTileArtAuthority.direction(game.world,cx,cy+2)==null,"Flat reset must remove stair artwork metadata");
            WorldAtlas atlas=WorldAtlas.preview(19191,WorldSetupSettings.standard());game.atlas=atlas;
            World upperSewer=new World(92,12,12);upperSewer.floor=5;upperSewer.sewerLayer=true;
            atlas.slices[0][0][1][1][9]=upperSewer;
            int actualFloor=atlas.floor;boolean actualSewer=atlas.sewer;
            PlannerLevelViewAuthority.handleKey(game,java.awt.event.KeyEvent.VK_PAGE_UP);
            require(PlannerLevelViewAuthority.viewedWorld(game)==upperSewer,"Main ground must browse upward to the next level's sewer");
            require(game.world==occupied && atlas.floor==actualFloor && atlas.sewer==actualSewer,"Atlas browsing must not change authoritative location");
            require(!MultiFloorBuildingTransitAuthority.rememberedAt(game,upperSewer,1,1),"Browsing must not expose unexplored levels");
            PlannerLevelViewAuthority.handleKey(game,java.awt.event.KeyEvent.VK_HOME);
            System.out.println("PASS: multi-level battle map, native assets, lighting and 20 authoritative combat turns");
        } finally {
            if(session!=null){DeveloperFacilityAuthority.leave(session.facility());SwingUtilities.invokeAndWait(()->{});}
            campaign.singlePlayerSectorBridge.close();campaign.shutdownRuntime();
        }
    }
    private static void capture(GamePanel game,Path output,String name)throws Exception {
        BufferedImage image=new BufferedImage(1280,800,BufferedImage.TYPE_INT_RGB);Graphics2D g=image.createGraphics();
        try{game.paint(g);}finally{g.dispose();}
        require(game.getClientProperty("mechanist.render.failure")==null,"GamePanel caught a rendering failure");
        ImageIO.write(image,"png",output.resolve(name+".png").toFile());
    }

    private static void verifyExposureBuffer() {
        BufferedImage target = new BufferedImage(12, 10, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = target.createGraphics();
        try {
            graphics.setColor(Color.MAGENTA); graphics.fillRect(0, 0, 12, 10);
            var transform = graphics.getTransform(); var composite = graphics.getComposite();
            var clip = graphics.getClip();
            var buffer = new ViewportDarknessAuthority.SceneBuffer();
            Rectangle area = new Rectangle(2, 3, 4, 3);
            Color authored = new Color(173, 23, 43);
            buffer.paint(graphics, area, layer -> { layer.setColor(authored); layer.fill(area); });
            require(target.getRGB(2,3)==ViewportDarknessAuthority.expose(authored.getRGB()),
                    "Software viewport must retain exact channel exposure at translated coordinates");
            require(target.getRGB(1,3)==Color.MAGENTA.getRGB() && target.getRGB(6,3)==Color.MAGENTA.getRGB(),
                    "Exposure escaped its viewport");
            buffer.paint(graphics, area, layer -> {});
            require(target.getRGB(2,3)==ViewportDarknessAuthority.expose(new Color(10,11,10).getRGB()),
                    "Reused viewport retained stale pixels");
            buffer.paint(graphics, new Rectangle(1,1,6,4), layer -> {
                layer.setColor(Color.BLACK); layer.fillRect(1,1,6,4);
            });
            require(target.getRGB(1,1)==ViewportDarknessAuthority.DARKEST, "Resized viewport lost the darkness floor");
            require(transform.equals(graphics.getTransform()) && composite.equals(graphics.getComposite())
                            && Objects.equals(clip, graphics.getClip()), "Exposure changed target graphics state");
        } finally { graphics.dispose(); }
    }

    private static void verifyRenderErrorReporting(GamePanel panel) throws Exception {
        var draw = GamePanel.class.getDeclaredMethod("drawBridgeException", Graphics2D.class,
                int.class, int.class, Throwable.class);
        draw.setAccessible(true);
        var expected = new IllegalStateException("synthetic render-error reporting regression");
        BufferedImage image = new BufferedImage(32,32,BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        try { draw.invoke(panel,g,32,32,expected); } finally { g.dispose(); }
        require(panel.getClientProperty("mechanist.render.failure")==expected,
                "Error-frame rendering must publish the failure for graphical acceptance checks");
        panel.putClientProperty("mechanist.render.failure",null);
    }
    private static void require(boolean okay,String message){if(!okay)throw new AssertionError(message);}
}
