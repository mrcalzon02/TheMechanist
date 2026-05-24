package mechanist;

import java.awt.*;
import java.util.*;

/**
 * Room-interior service fixture authority.
 *
 * Places and handles current room-interior fixture families for medicae,
 * laboratory, forge, food/bio, domestic hab, bar, Arbites precinct, noble estate security, Guard/PDF defense, and civic service surfaces. Fixtures expose stable
 * semantic handles for inspection, feedback, and operation handoff.
 */
final class RoomFixtureInteractionAuthority {
    static final String VERSION = "0.9.10ir";

    static final String MEDICAE_FIXTURE = AssetIntegrationDisciplineAuthority.MEDICAE_ROOM_FIXTURE;
    static final String LAB_FIXTURE = AssetIntegrationDisciplineAuthority.LABORATORY_ROOM_FIXTURE;
    static final String FORGE_FIXTURE = AssetIntegrationDisciplineAuthority.FORGE_ROOM_MACHINE;
    static final String BAR_FIXTURE = AssetIntegrationDisciplineAuthority.FACTION_BAR_INTERIOR_FIXTURE;
    static final String FOOD_BIO_FIXTURE = AssetIntegrationDisciplineAuthority.HYDROPONICS_BED_FIXTURE;
    static final String DOMESTIC_HAB_FIXTURE = AssetIntegrationDisciplineAuthority.HAB_COT_FIXTURE;
    static final String CIVIC_FIXTURE = AssetIntegrationDisciplineAuthority.CIVIC_INTERIOR_SERVICE_FIXTURE;

    private RoomFixtureInteractionAuthority() {}

    static final class Result {
        int medicae, labs, forge, foodBio, domesticHab, bars, arbites, nobleSecurity, guardPdfDefense, civic, skipped;
        String summary(){
            return "roomFixtures medicae="+medicae+" labs="+labs+" forge="+forge+" foodBio="+foodBio+" domesticHab="+domesticHab+" bars="+bars+" arbites="+arbites+" nobleSecurity="+nobleSecurity+" guardPdfDefense="+guardPdfDefense+" civic="+civic+" skipped="+skipped+
                    " rule=interior fixtures are readable semantic hooks for shared operations and service surfaces";
        }
    }

    static Result apply(World w, Random r) {
        Result res = new Result();
        if (w == null || w.rooms == null || w.rooms.isEmpty()) return res;
        if (r == null) r = new Random(w.seed ^ 0x9080DL);
        int placed = 0;
        for (int i = 1; i < w.rooms.size() && placed < 16; i++) {
            Rectangle rr = w.rooms.get(i);
            if (rr == null || rr.width < 3 || rr.height < 3) { res.skipped++; continue; }
            RoomProfile rp = i < w.roomProfiles.size() ? w.roomProfiles.get(i) : null;
            String type = fixtureTypeFor(w, rp, i, r);
            if (type == null) { res.skipped++; continue; }
            Point p = fixturePoint(w, rr, r);
            if (p == null) { res.skipped++; continue; }
            char under = w.tiles[p.x][p.y];
            MapObjectState m = roomFixture(p.x, p.y, type, labelFor(type, rp, w.zoneType), stockFor(type, rp, w, i, under), glyphFor(type));
            w.tiles[p.x][p.y] = m.glyph;
            w.mapObjects.add(m);
            placed++;
            if (MedicaeFixtureAuthority.isFamilyType(type)) res.medicae++;
            else if (LabChemicalFixtureAuthority.isFamilyType(type)) res.labs++;
            else if (IndustrialForgeFixtureAuthority.isFamilyType(type)) res.forge++;
            else if (FoodBioProductionFixtureAuthority.isFamilyType(type)) res.foodBio++;
            else if (DomesticHabFixtureAuthority.isFamilyType(type)) res.domesticHab++;
            else if (BarMarketSocialFixtureAuthority.isFamilyType(type)) res.bars++;
            else if (ArbitesPrecinctFixtureAuthority.isFamilyType(type)) res.arbites++;
            else if (NobleEstateSecurityFixtureAuthority.isFamilyType(type)) res.nobleSecurity++;
            else if (GuardPdfDefenseFixtureAuthority.isFamilyType(type)) res.guardPdfDefense++;
            else if (CIVIC_FIXTURE.equals(type)) res.civic++;
        }
        return res;
    }

    static String fixtureTypeFor(World w, RoomProfile rp, int roomId, Random r) {
        String text = ((rp == null || rp.name == null) ? "" : rp.name) + " " + ((rp == null || rp.descriptor == null) ? "" : rp.descriptor) + " " + ((rp == null || rp.featureText == null) ? "" : rp.featureText);
        String low = text.toLowerCase(Locale.ROOT);
        if (contains(low, "clinic", "medicae", "surgery", "triage", "hospital", "apothec", "chop-shop")) return MedicaeFixtureAuthority.chooseRoomFixtureType(w, rp, roomId, r);
        if (contains(low, "laboratorium", "laboratory", "sample", "research", "analysis", "chemical", "chem", "reagent", "distill", "fume", "injector", "ampoule", "toxin", "solvent")) return LabChemicalFixtureAuthority.chooseRoomFixtureType(w, rp, roomId, r);
        if (contains(low, "forge", "workshop", "workbench", "machine", "assembler", "smelter", "press", "repair booth", "maintenance", "condenser", "reclamation", "boiler", "component warehouse")) return IndustrialForgeFixtureAuthority.chooseRoomFixtureType(w, rp, roomId, r);
        if (contains(low, "hab", "apartment", "dormitory", "dorm", "bedroom", "cot", "bunk", "quarters", "washroom", "bathroom", "lavatory", "kitchenette", "dresser", "wardrobe", "domestic", "living room", "common room")) return DomesticHabFixtureAuthority.chooseRoomFixtureType(w, rp, roomId, r);
        if (contains(low, "food", "kitchen", "cafeteria", "canteen", "mess", "galley", "pantry", "ration", "nutrient", "hydroponic", "greenhouse", "orchard", "garden", "algae", "fungus", "fungal", "animal pen", "livestock", "cloning", "bio-vat", "refrigerator", "freezer", "cooler")) return FoodBioProductionFixtureAuthority.chooseRoomFixtureType(w, rp, roomId, r);
        if (contains(low, "holding", "cell", "detention", "interrogation", "evidence", "contraband", "armory", "complaint", "precinct", "arbites", "baton", "perp")) return ArbitesPrecinctFixtureAuthority.chooseRoomFixtureType(w, rp, roomId, r);
        if (contains(low, "estate security", "noble security", "gilded sentry", "private turret", "shield relay", "void shield", "energy fence", "laser pylon", "security panel", "panic room", "treasury", "heirloom vault", "noble gate", "house guard")) return NobleEstateSecurityFixtureAuthority.chooseRoomFixtureType(w, rp, roomId, r);
        if (contains(low, "pdf", "astra militarum", "imperial guard", "guard", "barracks", "billet", "muster", "sandbag", "checkpoint", "watch post", "field defense", "munition", "quartermaster", "turret lane")) return GuardPdfDefenseFixtureAuthority.chooseRoomFixtureType(w, rp, roomId, r);
        if (contains(low, "bar", "tavern", "amasec", "dining", "food court", "market", "storefront", "vendor", "barter", "trade", "shop", "counter", "representative")) return BarMarketSocialFixtureAuthority.chooseRoomFixtureType(w, rp, roomId, r);
        if (contains(low, "office", "permit", "public", "service", "queue", "administratum")) return CIVIC_FIXTURE;
        if (w.zoneType == ZoneType.MECHANICUS_FORGE_CLOISTER || w.zoneType == ZoneType.MECHANICUS_RELIC_DUCT) return r.nextDouble() < 0.35 ? IndustrialForgeFixtureAuthority.chooseRoomFixtureType(w, rp, roomId, r) : (r.nextDouble() < 0.20 ? LabChemicalFixtureAuthority.chooseRoomFixtureType(w, rp, roomId, r) : null);
        if (w.zoneType == ZoneType.ARBITES_PRECINCT_EDGE) return r.nextDouble() < 0.58 ? ArbitesPrecinctFixtureAuthority.chooseRoomFixtureType(w, rp, roomId, r) : (r.nextDouble() < 0.26 ? CIVIC_FIXTURE : null);
        if (w.zoneType == ZoneType.IMPERIAL_GUARD_BILLET) {
            double roll = r.nextDouble();
            if (roll < 0.58) return GuardPdfDefenseFixtureAuthority.chooseRoomFixtureType(w, rp, roomId, r);
            if (roll < 0.76) return FoodBioProductionFixtureAuthority.chooseRoomFixtureType(w, rp, roomId, r);
            if (roll < 0.88) return DomesticHabFixtureAuthority.chooseRoomFixtureType(w, rp, roomId, r);
            if (roll < 0.94) return BarMarketSocialFixtureAuthority.chooseRoomFixtureType(w, rp, roomId, r);
            return null;
        }
        if (w.zoneType == ZoneType.ADMINISTRATUM_ARCHIVE || w.zoneType == ZoneType.IMPERIAL_NEWS_NETWORK) return r.nextDouble() < 0.24 ? CIVIC_FIXTURE : null;
        if (w.zoneType == ZoneType.NOBLE_SERVICE_SPINE || w.zoneType == ZoneType.SECTOR_GOVERNORS_MANSION) {
            double roll = r.nextDouble();
            if (roll < 0.32) return NobleEstateSecurityFixtureAuthority.chooseRoomFixtureType(w, rp, roomId, r);
            if (roll < 0.48) return BarMarketSocialFixtureAuthority.chooseRoomFixtureType(w, rp, roomId, r);
            if (roll < 0.62) return MedicaeFixtureAuthority.chooseRoomFixtureType(w, rp, roomId, r);
            if (roll < 0.72) return FoodBioProductionFixtureAuthority.chooseRoomFixtureType(w, rp, roomId, r);
            if (roll < 0.84) return DomesticHabFixtureAuthority.chooseRoomFixtureType(w, rp, roomId, r);
            if (roll < 0.90) return CIVIC_FIXTURE;
            return null;
        }
        if (w.zoneType == ZoneType.NEUTRAL_CIVILIAN_FLOOR || w.zoneType == ZoneType.HAB_STACK || w.zoneType == ZoneType.SUMP_MARKET) {
            double roll = r.nextDouble();
            if (roll < 0.28) return DomesticHabFixtureAuthority.chooseRoomFixtureType(w, rp, roomId, r);
            if (roll < 0.39) return CIVIC_FIXTURE;
            if (roll < 0.52) return BarMarketSocialFixtureAuthority.chooseRoomFixtureType(w, rp, roomId, r);
            if (roll < 0.63) return MedicaeFixtureAuthority.chooseRoomFixtureType(w, rp, roomId, r);
            if (roll < 0.74) return FoodBioProductionFixtureAuthority.chooseRoomFixtureType(w, rp, roomId, r);
            return null;
        }
        return null;
    }

    static Point fixturePoint(World w, Rectangle rr, Random r) {
        for (int tries = 0; tries < 30; tries++) {
            int x = rr.x + 1 + r.nextInt(Math.max(1, rr.width - 2));
            int y = rr.y + 1 + r.nextInt(Math.max(1, rr.height - 2));
            if (!w.inBounds(x,y)) continue;
            if (w.tiles[x][y] != '.') continue;
            if (w.mapObjectAt(x,y) != null) continue;
            if (w.isDoorAccessReservedForObject(x,y)) continue;
            return new Point(x,y);
        }
        int cx = Math.max(rr.x, Math.min(rr.x + rr.width - 1, rr.x + rr.width / 2));
        int cy = Math.max(rr.y, Math.min(rr.y + rr.height - 1, rr.y + rr.height / 2));
        if (w.inBounds(cx,cy) && w.tiles[cx][cy] == '.' && w.mapObjectAt(cx,cy) == null && !w.isDoorAccessReservedForObject(cx,cy)) return new Point(cx,cy);
        return null;
    }

    static MapObjectState roomFixture(int x, int y, String type, String label, String stock, char glyph) {
        MapObjectState m = new MapObjectState();
        m.x = x; m.y = y; m.type = type; m.label = label; m.stockState = stock; m.glyph = glyph;
        m.cooldownUntilTurn = 0; m.vendCount = 0;
        m.id = "ROOM-FIX-" + Math.abs(Objects.hash(x,y,type,label));
        return m;
    }

    static String labelFor(String type, RoomProfile rp, ZoneType z) {
        String room = rp == null || rp.name == null ? "unclassified room" : rp.name;
        String zone = z == null ? "Unknown zone" : z.label;
        if (MedicaeFixtureAuthority.isFamilyType(type)) return MedicaeFixtureAuthority.roomLabel(type, rp, z);
        if (LabChemicalFixtureAuthority.isFamilyType(type)) return LabChemicalFixtureAuthority.roomLabel(type, rp, z);
        if (IndustrialForgeFixtureAuthority.isFamilyType(type)) return IndustrialForgeFixtureAuthority.roomLabel(type, rp, z);
        if (FoodBioProductionFixtureAuthority.isFamilyType(type)) return FoodBioProductionFixtureAuthority.roomLabel(type, rp, z);
        if (DomesticHabFixtureAuthority.isFamilyType(type)) return DomesticHabFixtureAuthority.roomLabel(type, rp, z);
        if (BarMarketSocialFixtureAuthority.isFamilyType(type)) return BarMarketSocialFixtureAuthority.roomLabel(type, rp, z);
        if (ArbitesPrecinctFixtureAuthority.isFamilyType(type)) return ArbitesPrecinctFixtureAuthority.roomLabel(type, rp, z);
        if (NobleEstateSecurityFixtureAuthority.isFamilyType(type)) return NobleEstateSecurityFixtureAuthority.roomLabel(type, rp, z);
        if (GuardPdfDefenseFixtureAuthority.isFamilyType(type)) return GuardPdfDefenseFixtureAuthority.roomLabel(type, rp, z);
        return "Civic interior service fixture / " + room + " / " + zone;
    }

    static String stockFor(String type, RoomProfile rp, World w, int roomId, char under) {
        String room = rp == null || rp.name == null ? "room" : rp.name.replace(';', ',');
        if (MedicaeFixtureAuthority.isFamilyType(type)) return MedicaeFixtureAuthority.roomStock(type, rp, w, roomId, under);
        if (LabChemicalFixtureAuthority.isFamilyType(type)) return LabChemicalFixtureAuthority.roomStock(type, rp, w, roomId, under);
        if (IndustrialForgeFixtureAuthority.isFamilyType(type)) return IndustrialForgeFixtureAuthority.roomStock(type, rp, w, roomId, under);
        if (FoodBioProductionFixtureAuthority.isFamilyType(type)) return FoodBioProductionFixtureAuthority.roomStock(type, rp, w, roomId, under);
        if (DomesticHabFixtureAuthority.isFamilyType(type)) return DomesticHabFixtureAuthority.roomStock(type, rp, w, roomId, under);
        if (BarMarketSocialFixtureAuthority.isFamilyType(type)) return BarMarketSocialFixtureAuthority.roomStock(type, rp, w, roomId, under);
        if (ArbitesPrecinctFixtureAuthority.isFamilyType(type)) return ArbitesPrecinctFixtureAuthority.roomStock(type, rp, w, roomId, under);
        if (NobleEstateSecurityFixtureAuthority.isFamilyType(type)) return NobleEstateSecurityFixtureAuthority.roomStock(type, rp, w, roomId, under);
        if (GuardPdfDefenseFixtureAuthority.isFamilyType(type)) return GuardPdfDefenseFixtureAuthority.roomStock(type, rp, w, roomId, under);
        return "room-fixture;roomId="+roomId+";room="+room+";handoff=operation-profile;under="+(int)under+";type="+type;
    }

    static char glyphFor(String type) {
        if (MedicaeFixtureAuthority.isFamilyType(type)) return MedicaeFixtureAuthority.glyphForType(type);
        if (LabChemicalFixtureAuthority.isFamilyType(type)) return LabChemicalFixtureAuthority.glyphForType(type);
        if (IndustrialForgeFixtureAuthority.isFamilyType(type)) return IndustrialForgeFixtureAuthority.glyphForType(type);
        if (FoodBioProductionFixtureAuthority.isFamilyType(type)) return FoodBioProductionFixtureAuthority.glyphForType(type);
        if (DomesticHabFixtureAuthority.isFamilyType(type)) return DomesticHabFixtureAuthority.glyphForType(type);
        if (BarMarketSocialFixtureAuthority.isFamilyType(type)) return BarMarketSocialFixtureAuthority.glyphForType(type);
        if (ArbitesPrecinctFixtureAuthority.isFamilyType(type)) return ArbitesPrecinctFixtureAuthority.glyphForType(type);
        if (NobleEstateSecurityFixtureAuthority.isFamilyType(type)) return NobleEstateSecurityFixtureAuthority.glyphForType(type);
        if (GuardPdfDefenseFixtureAuthority.isFamilyType(type)) return GuardPdfDefenseFixtureAuthority.glyphForType(type);
        return 'q';
    }

    static boolean tryInteract(GamePanel g, int tx, int ty) {
        if (g == null || g.world == null) return false;
        MapObjectState m = g.world.mapObjectAt(tx, ty);
        if (m == null || m.type == null) return false;
        String type = AssetIntegrationDisciplineAuthority.canonicalType(m.type);
        if (MedicaeFixtureAuthority.isFamilyType(type)) { interactMedicae(g, m); return true; }
        if (LabChemicalFixtureAuthority.isFamilyType(type)) { interactLab(g, m); return true; }
        if (IndustrialForgeFixtureAuthority.isFamilyType(type)) { interactForge(g, m); return true; }
        if (FoodBioProductionFixtureAuthority.isFamilyType(type)) { interactFoodBio(g, m); return true; }
        if (DomesticHabFixtureAuthority.isFamilyType(type)) { interactDomesticHab(g, m); return true; }
        if (BarMarketSocialFixtureAuthority.isFamilyType(type)) { interactBar(g, m); return true; }
        if (ArbitesPrecinctFixtureAuthority.isFamilyType(type)) { interactArbites(g, m); return true; }
        if (NobleEstateSecurityFixtureAuthority.isFamilyType(type)) { interactNobleSecurity(g, m); return true; }
        if (GuardPdfDefenseFixtureAuthority.isFamilyType(type)) { interactGuardPdfDefense(g, m); return true; }
        switch (type) {
            case CIVIC_FIXTURE: interactCivic(g, m); return true;
            default: return false;
        }
    }

    static void interactMedicae(GamePanel g, MapObjectState m) {
        g.logEvent(MedicaeFixtureAuthority.inspectionLine(m.type));
        if (g.fatigue > 0) g.fatigue = Math.max(0, g.fatigue - 1);
        String promo = InfrastructurePromotionRegistry.feedbackForFixture(m.type);
        if (!promo.isBlank()) g.logEvent(promo);
        g.gainXp("Medicine", 1, "inspected medicae room fixture");
        m.vendCount++; m.cooldownUntilTurn = g.turn + FixtureInteractionRegistry.cooldownFor(m.type, 18);
        g.sounds.playDistantCue(FixtureInteractionRegistry.soundFor(m.type, "ambient_chime"), 4, g.options);
        g.advanceTurn(MedicaeFixtureAuthority.interactionVerb(m.type));
        g.repaint();
    }

    static void interactLab(GamePanel g, MapObjectState m) {
        g.logEvent(LabChemicalFixtureAuthority.inspectionLine(m.type));
        String promo = InfrastructurePromotionRegistry.feedbackForFixture(m.type);
        if (!promo.isBlank()) g.logEvent(promo);
        g.gainXp("Knowledge", 1, "inspected " + LabChemicalFixtureAuthority.shortLabel(m).toLowerCase(Locale.ROOT));
        m.vendCount++; m.cooldownUntilTurn = g.turn + FixtureInteractionRegistry.cooldownFor(m.type, 18);
        g.sounds.playDistantCue(FixtureInteractionRegistry.soundFor(m.type, "ambient_sparks"), 4, g.options);
        g.advanceTurn(LabChemicalFixtureAuthority.interactionVerb(m.type));
        g.repaint();
    }

    static void interactForge(GamePanel g, MapObjectState m) {
        g.logEvent(IndustrialForgeFixtureAuthority.inspectionLine(m.type));
        String promo = InfrastructurePromotionRegistry.feedbackForFixture(m.type);
        if (!promo.isBlank()) g.logEvent(promo);
        g.gainXp("Mechanics", 1, "inspected " + IndustrialForgeFixtureAuthority.shortLabel(m).toLowerCase(Locale.ROOT));
        m.vendCount++; m.cooldownUntilTurn = g.turn + FixtureInteractionRegistry.cooldownFor(m.type, 18);
        g.sounds.playDistantCue(FixtureInteractionRegistry.soundFor(m.type, "ambient_press"), 3, g.options);
        g.advanceTurn(IndustrialForgeFixtureAuthority.interactionVerb(m.type));
        g.repaint();
    }

    static void interactFoodBio(GamePanel g, MapObjectState m) {
        g.logEvent(FoodBioProductionFixtureAuthority.inspectionLine(m.type));
        String promo = InfrastructurePromotionRegistry.feedbackForFixture(m.type);
        if (!promo.isBlank()) g.logEvent(promo);
        g.gainXp("Survival", 1, "inspected " + FoodBioProductionFixtureAuthority.shortLabel(m).toLowerCase(Locale.ROOT));
        m.vendCount++; m.cooldownUntilTurn = g.turn + FixtureInteractionRegistry.cooldownFor(m.type, 18);
        g.sounds.playDistantCue(FixtureInteractionRegistry.soundFor(m.type, "ambient_machine"), 3, g.options);
        g.advanceTurn(FoodBioProductionFixtureAuthority.interactionVerb(m.type));
        g.repaint();
    }

    static void interactDomesticHab(GamePanel g, MapObjectState m) {
        g.logEvent(DomesticHabFixtureAuthority.inspectionLine(m.type));
        String promo = InfrastructurePromotionRegistry.feedbackForFixture(m.type);
        if (!promo.isBlank()) g.logEvent(promo);
        g.gainXp("Survival", 1, "inspected " + DomesticHabFixtureAuthority.shortLabel(m).toLowerCase(Locale.ROOT));
        m.vendCount++; m.cooldownUntilTurn = g.turn + FixtureInteractionRegistry.cooldownFor(m.type, 14);
        g.sounds.playDistantCue(FixtureInteractionRegistry.soundFor(m.type, "ambient_pipe"), 3, g.options);
        g.advanceTurn(DomesticHabFixtureAuthority.interactionVerb(m.type));
        g.repaint();
    }

    static void interactBar(GamePanel g, MapObjectState m) {
        Faction f = g.world == null ? Faction.NONE : g.world.dominantContinuityFactionForZone();
        g.logEvent(BarMarketSocialFixtureAuthority.inspectionLine(m.type, f));
        String promo = InfrastructurePromotionRegistry.feedbackForFixture(m.type);
        if (!promo.isBlank()) g.logEvent(promo);
        g.gainXp("Social", 1, "inspected " + BarMarketSocialFixtureAuthority.shortLabel(m).toLowerCase(Locale.ROOT));
        m.vendCount++; m.cooldownUntilTurn = g.turn + FixtureInteractionRegistry.cooldownFor(m.type, 18);
        g.sounds.playDistantCue(FixtureInteractionRegistry.soundFor(m.type, "ambient_radio"), 3, g.options);
        g.advanceTurn(BarMarketSocialFixtureAuthority.interactionVerb(m.type));
        g.repaint();
    }

    static void interactArbites(GamePanel g, MapObjectState m) {
        g.logEvent(ArbitesPrecinctFixtureAuthority.inspectionLine(m.type));
        String promo = InfrastructurePromotionRegistry.feedbackForFixture(m.type);
        if (!promo.isBlank()) g.logEvent(promo);
        g.gainXp("Investigation", 1, "inspected " + ArbitesPrecinctFixtureAuthority.shortLabel(m).toLowerCase(Locale.ROOT));
        m.vendCount++; m.cooldownUntilTurn = g.turn + FixtureInteractionRegistry.cooldownFor(m.type, 20);
        g.sounds.playDistantCue(FixtureInteractionRegistry.soundFor(m.type, "ambient_door_servo"), 3, g.options);
        g.advanceTurn(ArbitesPrecinctFixtureAuthority.interactionVerb(m.type));
        g.repaint();
    }


    static void interactNobleSecurity(GamePanel g, MapObjectState m) {
        g.logEvent(NobleEstateSecurityFixtureAuthority.inspectionLine(m.type));
        String promo = InfrastructurePromotionRegistry.feedbackForFixture(m.type);
        if (!promo.isBlank()) g.logEvent(promo);
        g.gainXp("Security", 1, "inspected " + NobleEstateSecurityFixtureAuthority.shortLabel(m).toLowerCase(Locale.ROOT));
        m.vendCount++; m.cooldownUntilTurn = g.turn + FixtureInteractionRegistry.cooldownFor(m.type, 24);
        g.sounds.playDistantCue(FixtureInteractionRegistry.soundFor(m.type, "ambient_door_servo"), 3, g.options);
        g.advanceTurn(NobleEstateSecurityFixtureAuthority.interactionVerb(m.type));
        g.repaint();
    }

    static void interactGuardPdfDefense(GamePanel g, MapObjectState m) {
        g.logEvent(GuardPdfDefenseFixtureAuthority.inspectionLine(m.type));
        String promo = InfrastructurePromotionRegistry.feedbackForFixture(m.type);
        if (!promo.isBlank()) g.logEvent(promo);
        g.gainXp("Security", 1, "inspected " + GuardPdfDefenseFixtureAuthority.shortLabel(m).toLowerCase(Locale.ROOT));
        m.vendCount++; m.cooldownUntilTurn = g.turn + FixtureInteractionRegistry.cooldownFor(m.type, 22);
        g.sounds.playDistantCue(FixtureInteractionRegistry.soundFor(m.type, "ambient_door_servo"), 3, g.options);
        g.advanceTurn(GuardPdfDefenseFixtureAuthority.interactionVerb(m.type));
        g.repaint();
    }

    static void interactCivic(GamePanel g, MapObjectState m) {
        g.logEvent("CIVIC FIXTURE: counter, forms, terminal faceplate, and public-service machinery designed to convert urgency into queue position. The fixture identifies an interior civic service point.");
        g.gainXp("Knowledge", 1, "inspected civic room fixture");
        m.vendCount++; m.cooldownUntilTurn = g.turn + FixtureInteractionRegistry.cooldownFor(m.type, 18);
        g.sounds.playDistantCue(FixtureInteractionRegistry.soundFor(m.type, "ambient_door_servo"), 4, g.options);
        g.advanceTurn("inspects a civic service fixture.");
        g.repaint();
    }

    static boolean contains(String s, String... parts) {
        if (s == null) return false;
        for (String p : parts) if (p != null && !p.isBlank() && s.contains(p)) return true;
        return false;
    }}
