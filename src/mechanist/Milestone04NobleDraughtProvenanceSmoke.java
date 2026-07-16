package mechanist;

import java.util.Properties;

/** End-to-end smoke for finite noble luxury and protected exceptional draught custody. */
final class Milestone04NobleDraughtProvenanceSmoke {
    public static void main(String[] args) {
        System.setProperty("java.awt.headless", "true");
        require(ItemCatalog.priceFor("Black Sun Draught") >= 850,
                "rare draughts should be explicitly far more valuable than ordinary luxury drugs");

        World estate = world(98001L, ZoneType.SECTOR_GOVERNORS_MANSION);
        addRoom(estate, "House Ashbourne Product Vault",
                "gene-locked secure storage for inherited off-world substances and estate luxuries",
                Faction.NOBLE_HOUSE_VARN);
        estate.zoneConflictLossHistory = "L1: source=spire-import :: event=blockade tax and tithe edict"
                + " :: actor=estate revenue office :: affected=luxury imports"
                + " :: destination=House Ashbourne intake :: severity=major :: household hoarding order";
        TraderSession estateTrader = trader("House Ashbourne Estate Counter");
        estateTrader.offers.add(new TradeOffer("Black Sun Draught", "chem/rare-campaign", 85,
                "generic offer that must be withheld."));
        NobleLuxuryProvenanceAuthority.apply(estateTrader, estate, Faction.NOBLE_HOUSE_VARN, 500L, 500);

        DraughtCustodyRecord held = firstCustody(estate, Faction.NOBLE_HOUSE_VARN);
        require(held != null && "House Ashbourne".equals(held.houseOwner)
                        && held.heldQuantity == 1 && !held.releasedForSale,
                "Ashbourne vault should hold one protected draught outside ordinary trade");
        require(held.offWorldOrigin.contains("off-world") && held.importRoute.contains("house broker")
                        && held.vaultLabel.contains("Ashbourne Product Vault"),
                "vault custody should preserve off-world origin, broker route, and exact vault");
        require(held.eventStatus.contains("blockade affected") && held.eventStatus.contains("taxed")
                        && held.eventStatus.contains("tithe edict affected")
                        && "hoarding".equals(held.purpose),
                "draught custody should retain blockade, tax, tithe, and hoarding purpose");
        require(estateTrader.offers.stream().noneMatch(o -> isDraught(o.name)),
                "protected noble draught must not remain on an ordinary trader shelf");
        requireContains(estateTrader.supplyChainSummary, "protected household property, not for sale",
                "protected custody readback");

        TradeOffer amasec = offer(estateTrader, "High-quality amasec bottle");
        NobleLuxuryReserveRecord amasecReserve = NobleLuxuryProvenanceAuthority.reserveFor(
                estate, Faction.NOBLE_HOUSE_VARN, "High-quality amasec bottle");
        require(amasec != null && amasecReserve != null
                        && "event-diverted luxury".equals(amasecReserve.sourceKind)
                        && "House Ashbourne intake".equals(amasecReserve.sourceLabel)
                        && amasecReserve.eventStatus.contains("blockade affected"),
                "ordinary noble luxury should use the active taxed and blockaded intake reserve: "
                        + (amasecReserve == null ? "none" : amasecReserve.playerLine()));
        amasecReserve.remaining = 1;
        GamePanel game = new GamePanel();
        if (game.timer != null) game.timer.stop();
        try {
            game.world=estate; game.worldTurn=500L; game.turn=500; game.activeTraderSession=estateTrader;
            game.activeInteractionTitle="House Ashbourne Estate Counter"; game.panelMode=GamePanel.PanelMode.TRADE;
            game.screen=GamePanel.Screen.PANEL; game.selectedTradeOfferIndex=estateTrader.offers.indexOf(amasec);
            game.carriedScript=0; game.setSize(1280,720); render(game); int before=game.turn;
            runButton(game,"Buy");
            require(amasecReserve.remaining==1&&game.turn==before,
                    "failed luxury purchase must not consume reserve or time");
            game.carriedScript=1000; runButton(game,"Buy");
            require(amasecReserve.remaining==0&&game.turn==before+1,
                    "successful luxury purchase should consume one unit and one turn");
        } finally { game.shutdownRuntime(); }
        TraderSession reopened=trader("Reopened Estate Counter");
        NobleLuxuryProvenanceAuthority.apply(reopened,estate,Faction.NOBLE_HOUSE_VARN,501L,501);
        require(offer(reopened,"High-quality amasec bottle")==null&&reopened.offers.stream().noneMatch(o->isDraught(o.name)),
                "reopened estate should preserve luxury depletion and draught protection");

        Properties saved=new Properties(); Persistence.writeWorldState(estate,saved);
        World loaded=world(98001L,ZoneType.SECTOR_GOVERNORS_MANSION); Persistence.readWorldState(loaded,saved);
        DraughtCustodyRecord loadedCustody=firstCustody(loaded,Faction.NOBLE_HOUSE_VARN);
        NobleLuxuryReserveRecord loadedLuxury=NobleLuxuryProvenanceAuthority.reserveFor(
                loaded,Faction.NOBLE_HOUSE_VARN,"High-quality amasec bottle");
        require(loadedCustody!=null&&!loadedCustody.releasedForSale&&loadedCustody.eventStatus.contains("tithe")
                        &&loadedLuxury!=null&&loadedLuxury.remaining==0,
                "draught custody and ordinary luxury depletion should survive save/load");

        World theft=world(98002L,ZoneType.GANGER_TURF);
        theft.zoneConflictLossHistory="L1: source=House Ashbourne void intake :: event=smuggler theft"
                + " :: actor=Ashbourne broker turned smuggler :: affected=Black Sun Draught"
                + " :: destination=black-market shelf :: severity=major :: stolen for bargaining and sale";
        TraderSession fence=trader("Exceptional Draught Fence");
        NobleLuxuryProvenanceAuthority.apply(fence,theft,Faction.GANGER_ASH_MARKET,600L,600);
        DraughtCustodyRecord stolen=NobleLuxuryProvenanceAuthority.custodyFor(
                theft,Faction.GANGER_ASH_MARKET,"Black Sun Draught");
        TradeOffer released=offer(fence,"Black Sun Draught");
        require(stolen!=null&&stolen.releasedForSale&&"House Ashbourne".equals(stolen.houseOwner)
                        &&"stolen".equals(stolen.authenticity)&&"smuggler".equals(stolen.sourceRole)
                        &&"bargaining".equals(stolen.purpose),
                "smuggling event should create one exceptional stolen Ashbourne draught release");
        require(released!=null&&released.draughtCustodyId.equals(stolen.id)
                        &&released.provenance.shortChain().contains("House Ashbourne")
                        &&fence.buyPrice(released)>=850,
                "exceptional draught offer should retain custody provenance and extreme value");
        require(NobleLuxuryProvenanceAuthority.consume(theft,released,600L)
                        &&stolen.heldQuantity==0&&!stolen.releasedForSale,
                "exceptional draught sale should consume the sole custody unit and close release");

        World counterfeit=world(98003L,ZoneType.GANGER_TURF);
        counterfeit.zoneConflictLossHistory="L1: source=misdeclared merchant cargo :: event=black-market sale"
                + " :: actor=void merchant broker :: affected=Lucid Null :: destination=private bargaining room"
                + " :: severity=major :: counterfeit contaminated draught for blackmail";
        TraderSession counterfeitFence=trader("Counterfeit Draught Broker");
        NobleLuxuryProvenanceAuthority.apply(counterfeitFence,counterfeit,Faction.BANDIT,700L,700);
        DraughtCustodyRecord falseDraught=NobleLuxuryProvenanceAuthority.custodyFor(counterfeit,Faction.BANDIT,"Lucid Null");
        require(falseDraught!=null&&"counterfeit".equals(falseDraught.authenticity)
                        &&"broker".equals(falseDraught.sourceRole)&&"blackmail".equals(falseDraught.purpose),
                "counterfeit draught should preserve authenticity, broker source, and blackmail purpose: "
                        +(falseDraught==null?"none":falseDraught.playerLine()));

        World production=world(98004L,ZoneType.SECTOR_GOVERNORS_MANSION);
        production.zoneProductionHistory="P0: facility=rival-luxury-workshop :: purpose=luxury provisioning"
                + " :: controller=BANDIT :: focus=noble food and amasec :: cadence=daily :: batches=8"
                + " :: retained=3 :: samples=Noble preserved delicacy, High-quality amasec bottle :: rival output;;"
                + "P1: facility=house-cellar :: purpose=luxury provisioning :: controller=NOBLE"
                + " :: focus=noble food and amasec :: cadence=daily :: batches=3 :: retained=2"
                + " :: samples=Noble preserved delicacy, High-quality amasec bottle :: certified output";
        TraderSession productionTrader=trader("House Cellar Counter");
        NobleLuxuryProvenanceAuthority.apply(productionTrader,production,Faction.NOBLE,800L,800);
        NobleLuxuryReserveRecord produced=NobleLuxuryProvenanceAuthority.reserveFor(
                production,Faction.NOBLE,"High-quality amasec bottle");
        require(produced!=null&&"house-cellar".equals(produced.sourceLabel),
                "luxury production should reject rival output and use house-controlled cellar stock");

        System.out.println("Milestone 04 noble draught provenance smoke passed.");
    }

    private static World world(long seed,ZoneType zone){World w=new World(seed,40,40);w.zoneType=zone;return w;}
    private static void addRoom(World w,String name,String desc,Faction faction){w.roomProfiles.add(new RoomProfile(name,desc,60,faction,new String[]{"trade chit"},new char[]{'Q'}));w.roomFactions.add(faction);}
    private static TraderSession trader(String name){TraderSession t=new TraderSession();t.name=name;t.archetype="luxury trader";t.zoneLabel="Estate Market";return t;}
    private static TradeOffer offer(TraderSession t,String item){for(TradeOffer o:t.offers)if(o!=null&&ItemQuality.namesMatch(o.name,item))return o;return null;}
    private static DraughtCustodyRecord firstCustody(World w,Faction f){for(DraughtCustodyRecord c:w.draughtCustodyRecords)if(c!=null&&c.ownerFaction==f)return c;return null;}
    private static boolean isDraught(String item){ItemDef d=ItemCatalog.get(item);return d!=null&&"chem/rare-campaign".equalsIgnoreCase(d.category);}
    private static void render(GamePanel game){java.awt.image.BufferedImage c=new java.awt.image.BufferedImage(1280,720,java.awt.image.BufferedImage.TYPE_INT_ARGB);java.awt.Graphics2D g=c.createGraphics();game.paintComponent(g);g.dispose();}
    private static void runButton(GamePanel game,String label){for(ButtonBox b:game.buttons)if(b!=null&&label.equals(b.label)&&b.action!=null){b.action.run();return;}throw new AssertionError("Button not found: "+label);}
    private static void requireContains(String actual,String expected,String label){require(actual!=null&&actual.contains(expected),label+" missing '"+expected+"': "+actual);}
    private static void require(boolean condition,String message){if(!condition)throw new AssertionError(message);}
    private Milestone04NobleDraughtProvenanceSmoke(){}
}
