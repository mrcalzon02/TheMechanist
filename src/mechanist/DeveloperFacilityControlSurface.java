package mechanist;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/** Physical consoles render their controls inside the game's existing object panel. */
final class DeveloperFacilityControlSurface {
    private record Action(String label,String command) { }
    private DeveloperFacilityControlSurface() { }

    static boolean paint(GamePanel game,Graphics2D g,Rectangle body) {
        DeveloperFacilityAuthority.Layout layout=DeveloperFacilityAuthority.layout(game);
        MapObjectState object=game.activeInteractionObject;
        if(layout==null || object==null || !"developer-control".equals(object.type)) return false;
        String chamber=MapObjectState.stockValue(object.stockState,"chamber");
        Faction faction=Faction.visibleFactions()[layout.factionIndex];
        var category=FactionCriticalVendorPlacementAuthority.Category.values()[layout.categoryIndex];
        g.setFont(game.smallFont);g.setColor(new Color(222,201,139));
        List<String> lines=List.of("Isolated developer session. Campaign character, inventory and economy remain paused.",
                "Chambers: combat / systems / trade. Teleport doors and consoles are player-only.",
                "Selected merchant: "+faction.label+" / "+category.role+" / "+ItemQuality.NAMES[layout.quality],
                "Faction standing: "+game.factionStanding.getOrDefault(faction,0)+" | Script: "+game.carriedScript,
                "combat".equals(chamber)?DeveloperCombatScenarioAuthority.result(game)+" | "+ActorCombatAuthority.diagnostics(game).summary():"Spawn batch: "+layout.spawnCount+" | equipment tier "+layout.spawnTier+" | pinned traders: "+layout.vendors.size(),
                "Catalog containers hold five of every registered item at every quality. /devroom help lists commands.");
        int textY=body.y+18;
        for(String line:lines) { g.drawString(GuiLayoutApi.fitLabel(line,g.getFontMetrics(),body.width-16),body.x+8,textY);textY+=17; }
        ArrayList<Action> actions=new ArrayList<>();
        if("combat".equals(chamber)) {
            String preset=DeveloperCombatScenarioAuthority.PRESETS[layout.combatPresetIndex];
            for(int percent:new int[]{0,25,50,100}) actions.add(new Action("Cybernetics: "+percent+"%"+(layout.cyberneticPercent==percent?" selected":""),"cybernetics "+percent));
            actions.add(new Action("Scenario: "+preset+" >","scenario 1"));
            for(int size:new int[]{1,5,10,20}) actions.add(new Action("Start "+size+"v"+size+" "+preset,"match "+size+" "+preset+" 71000"));
            actions.add(new Action("Cover: clear","cover 0 81000"));
            actions.add(new Action("Cover: scatter 12","cover 12 81000"));
            actions.add(new Action("Cover: scatter 24","cover 24 81001"));
            actions.add(new Action("Watch / pause match","watch"));actions.add(new Action("Advance one turn","step 1"));
            actions.add(new Action("Advance ten turns","step 10"));actions.add(new Action("Combat diagnostics","combatstats"));
            for(String team:new String[]{"A","B"}) {
                actions.add(new Action(team+": issue Medkits","support "+team+" Medkit"));
                actions.add(new Action(team+": issue Street Stimm","support "+team+" Street Stimm"));
                actions.add(new Action(team+": wear flak vests","armor "+team+" Guard flak vest"));
                actions.add(new Action(team+": wear flak helmets","armor "+team+" Guard flak helmet"));
            }
            for(Faction spawn:new Faction[]{Faction.CULTIST,Faction.MUTANT,Faction.ROGUE_MACHINE,Faction.IMPERIAL_GUARD})
                actions.add(new Action("Spawn "+spawn.label,"spawn "+spawn.name()+" "+layout.spawnCount+" "+layout.spawnTier));
            actions.add(new Action("Spawn sump rats","animal sump-rat-swarm "+layout.spawnCount+" "+layout.spawnTier));
            actions.add(new Action("Spawn feral dogs","animal feral-arcology-dog "+layout.spawnCount+" "+layout.spawnTier));
            actions.add(new Action("Equipment tier -","tier -1"));actions.add(new Action("Equipment tier +","tier 1"));
            actions.add(new Action("Batch size -","count -1"));actions.add(new Action("Batch size +","count 1"));
            actions.add(new Action("Clear combat chamber","clear"));
        } else if("trade".equals(chamber)) {
            actions.add(new Action("Faction <","select faction -1"));actions.add(new Action("Faction >","select faction 1"));
            actions.add(new Action("Vendor category <","select category -1"));actions.add(new Action("Vendor category >","select category 1"));
            actions.add(new Action("Quality <","select quality -1"));actions.add(new Action("Quality >","select quality 1"));
            actions.add(new Action("Visit selected merchant","vendor "+faction.name()+" "+category.name()+" "+layout.quality));
            actions.add(new Action("Standing: hostile (-50)","standing "+faction.name()+" -50"));
            actions.add(new Action("Standing: neutral (0)","standing "+faction.name()+" 0"));
            actions.add(new Action("Standing: trusted (25)","standing "+faction.name()+" 25"));
            actions.add(new Action("Buy +5 standing: 100 Script","reputation "+faction.name()+" 5"));
            actions.add(new Action("Refill all catalog containers","restock"));
            actions.add(new Action("Visit catalog supply containers","supplies"));
            actions.add(new Action("Issue 100000 test Script","fund 100000"));
        } else if("systems".equals(chamber)) {
            actions.add(new Action("Supply workshop prerequisites","prepare"));
            actions.add(new Action("Refill all catalog containers","restock"));
            actions.add(new Action("Visit supply and trade chamber","trade"));
        }
        for(String destination:new String[]{"hub","combat","systems","trade"})
            if(!destination.equals(chamber)) actions.add(new Action("Teleport: "+destination,destination));
        actions.add(new Action("Return to paused campaign","exit"));
        int columns=body.width>=720?3:2;
        int availableRows=Math.max(1,(body.height-122)/30);
        int rows=(actions.size()+columns-1)/columns;
        if(rows>availableRows) columns=Math.max(columns,(actions.size()+availableRows-1)/availableRows);
        int width=(body.width-16-(columns-1)*8)/columns;
        for(int i=0;i<actions.size();i++) {
            Action action=actions.get(i);int x=body.x+8+(i%columns)*(width+8),y=body.y+118+(i/columns)*30;
            String label=GuiLayoutApi.fitLabel(action.label(),g.getFontMetrics(),width-12);
            game.buttons.add(new ButtonBox(label,x,y,width,26,action.label(),()->command(game,action.command()),null));
        }
        return true;
    }

    private static void command(GamePanel game,String command) {
        if(game.singlePlayerSectorBridge==null) { game.logEvent("Developer authority is not mounted.");return; }
        String response=game.singlePlayerSectorBridge.submitConsoleCommand(game,
                new ConsoleCommandRequest(SinglePlayerSectorRuntimeBridge.LOCAL_PLAYER_ID,"/devroom "+command));
        game.logEvent(response);game.repaint();
    }
}
