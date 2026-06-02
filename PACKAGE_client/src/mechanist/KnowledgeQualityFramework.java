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

class KnowledgeDef {
    final String name, family, source, unlocks; final int cost;
    KnowledgeDef(String name, String family, int cost, String source, String unlocks){ this.name=name; this.family=family; this.cost=cost; this.source=source; this.unlocks=unlocks; }
    static LinkedHashMap<String,KnowledgeDef> all(){
        LinkedHashMap<String,KnowledgeDef> m = new LinkedHashMap<>();
        registerCoreDoctrineDefs(m);
        KnowledgeTreeApi.registerKnowledgeDefs(m);
        return m;
    }
    private static void registerCoreDoctrineDefs(LinkedHashMap<String,KnowledgeDef> m) {
        // Existing game-owned unlocks and durable doctrine records.
        add(m, "Underhive Basics", "survival/foundation", 0, "starting doctrine", "basic scavenging literacy, crude research organization, and the first character-owned progression root");
        add(m, "Underhive Research Methods", "research/foundation", 1, "Micro Lab work, data slates, teachers", "better knowledge-credit conversion and recipe discovery profile");
        add(m, "Scavenged Schematics", "research/salvage", 1, "data slates, technical manuals, shop trash, and recovered work notes", "discovered schematic literacy and early recipe hints");
        add(m, "Profile Special Unlocks", "character/profile", 0, "explicit profile matching and save-owned character history", "special bounded profile rewards and durable knowledge records");

        // Survival and habitation.
        add(m, "Foraged Subsistence", "survival/food", 1, "hiver kitchens, scavver camps, market scraps", "safe enough scavenged food identification and emergency subsistence recipes");
        add(m, "Ration Sorting", "survival/food", 2, "ration stalls, field manuals, hab kitchens", "ration sorting, spoilage recognition, and basic food-stock management");
        add(m, "Food Preservation", "survival/food", 3, "communal kitchens, depot clerks, preservation slates", "preserved food handling, storage discipline, and pantry construction permissions");
        add(m, "Communal Kitchen Practice", "survival/food", 4, "hab cooks, charity kitchens, supplicant kitchens", "communal cooking rooms, bulk meal routines, and low-end morale support");
        add(m, "Industrial Food Processing", "survival/food/industry", 6, "factory canteens, vat tenders, agricultural workers", "industrial food processing workflows and higher-volume food recipes");
        add(m, "Nutrient Batch Standardization", "survival/food/industry", 8, "munitorum ration ledgers, hydroponic overseers, vat logs", "standardized nutrient batches and safer long-run food production");
        add(m, "Noble-Military Provisioning", "survival/food/faction", 11, "noble factors, military quartermasters, restricted provisioning schools", "prestige food, military rationing, and high-status provisioning permissions");

        add(m, "Condensation Handling", "survival/water/machine", 1, "trainers, water guild notes, Micro Lab, condenser manuals", "Atmospheric Condenser construction and water production recipes");
        add(m, "Water Barrel Handling", "survival/water/storage", 1, "water carriers, storehouses, hab stewards", "water barrel recognition, placement, and storage handling");
        add(m, "Crude Filtration", "survival/water", 2, "sump workers, filter notes, emergency kits", "crude filter recipes and bad-water triage");
        add(m, "Potable Water Discipline", "survival/water", 3, "water guild notes, medicae warnings, purifier slates", "potable water labeling, safe storage, and sanitation-adjacent water practices");
        add(m, "Water Recycler Operation", "survival/water/infrastructure", 5, "utility workers, recycler rooms, municipal slates", "water recycler operation and medium-scale water recovery systems");
        add(m, "Municipal Purification Systems", "survival/water/infrastructure", 7, "public works ledgers, guild engineers, district utility plans", "municipal purifiers and district water-room support");
        add(m, "Closed-Cycle Water Authority", "survival/water/infrastructure", 10, "sealed utility schools, Mechanist Collegia supervision, district charters", "closed-cycle water systems and high-trust district water authority");

        add(m, "Rough Bedding", "survival/habitation", 1, "dormitories, shanties, work camps", "rough bedding, rest recognition, and very basic sleep-space setup");
        add(m, "Cot Assembly", "survival/habitation", 1, "hab workers, medicae triage areas, barracks stores", "cot assembly and correct generic cot usage");
        add(m, "Dormitory Layout", "survival/habitation", 2, "hab wardens, barracks supervisors, labor bosses", "dormitory room stamps and predictable cot/sink/dresser layout logic");
        add(m, "Hab-Cell Furnishing", "survival/habitation", 3, "tenement stewards, hab repair crews, cheap landlords", "basic hab-cell furniture, cabinets, and small apartment support");
        add(m, "Communal Sanitation", "survival/habitation/sanitation", 4, "washrooms, civic ordinances, medicae warnings", "shared sanitation fixtures and disease-risk mitigation in habitation spaces");
        add(m, "Apartment Block Planning", "survival/habitation/civic", 6, "hab planners, civic Ledger Office surveys, slum improvement charters", "multi-room apartment stamps and predictable residential room clusters");
        add(m, "Civilian Habitation Authority", "survival/habitation/civic", 8, "civic permits, hab-block administrators, faction housing offices", "civilian housing permissions and higher-density habitation planning");

        add(m, "Scrap Recognition", "survival/salvage", 1, "trash heaps, work yards, scavver camps", "recognition of useful scrap and junk containers");
        add(m, "Useful Junk Sorting", "survival/salvage", 2, "scrap bosses, scavenger caches, market junk stalls", "sorting of useful junk from dead weight");
        add(m, "Salvage Carrying Discipline", "survival/salvage/logistics", 2, "haulers, porters, warehouse labor", "safer manual salvage hauling and early logistics carry rules");
        add(m, "Repairable Goods Identification", "survival/salvage/craft", 3, "repair benches, broken stores, reclaimed goods clerks", "recognition of repairable goods and refurbishable equipment");
        add(m, "Reclaimable Components", "survival/salvage/industry", 5, "machine rooms, maintenance bins, workshop manuals", "component recovery and reclaimable industrial part identification");
        add(m, "Trade-Grade Salvage Assessment", "survival/salvage/commerce", 7, "market assessors, guild sorters, pawn ledgers", "salvage valuation and trade-grade scrap routing");
        add(m, "Settlement Self-Sufficiency", "survival/convergence", 8, "local survival practice, kitchens, water rooms, salvage stores", "combined food, water, shelter, and salvage doctrine for independent settlement survival");
        add(m, "Urban Survival Doctrine", "survival/convergence", 6, "underhive mentors, hostile district travel, lived experience", "practical survival decisions across food, water, shelter, danger, and scavenging");
        add(m, "Basic Civilian Logistics", "survival/logistics", 4, "hab quartermasters, porter crews, supply ledgers", "basic civilian supply movement and storage planning");
        add(m, "Recruit Berthing Doctrine", "faction/base", 1, "base managers, labor bosses, hab wardens", "recruit capacity profile based on built cots and assigned bunks");
        add(m, "Base Logistics Discipline", "faction/logistics", 3, "quartermasters, cargo scribes, supply-route ledgers", "carrying stations, supply posts, logistics centers, and later inter-base cargo transfer");

        // Fabrication quality spine and faction production.
        add(m, "Scrap-Forging Doctrine", "mechanist/fabrication", 2, "Mechanist Collegia traders, forge tutors, data slates, Micro Lab", "Micro Forge construction and supply conversion recipes");
        add(m, "Junk Fabrication Patterns", "quality/production", 1, "trash slates, bad tutors, broken manuals, desperate practice", "Junk recipes and Junk machine operation; low value, low charges, high defect risk");
        add(m, "Common Fabrication Patterns", "quality/production", 2, "working manuals, guild basics, civic workshops", "Common recipes, common workshop repeatability, and ordinary civic production");
        add(m, "Serviceable Fabrication Patterns", "quality/production", 4, "guild trainers, shop ledgers, working manuals, stable Micro Lab research", "Serviceable recipes and baseline reliable faction production");
        add(m, "Fine Fabrication Patterns", "quality/production", 6, "skilled artisans, better instruments, inspected shop ledgers", "Fine recipes, better tolerances, and skilled-workshop outputs");
        add(m, "Masterwork Fabrication Patterns", "quality/production", 8, "expert artificers, master shops, rare ledgers", "Masterwork recipes and rare expert production patterns");
        add(m, "Noble Manufactury Patterns", "quality/production", 11, "noble factors, restricted guild schools, high-status contracts", "Noble recipes, high-value goods, comfort infrastructure, and prestige outputs");
        add(m, "Archeotech Pattern Recognition", "quality/production/archeotech", 14, "sealed vault data, Mechanist Collegia custody, forbidden manuals", "recognition of archeotech pattern risks, impossible tolerances, and relic production ceilings");
        add(m, "Archeotech Production Rites", "quality/production/archeotech", 15, "Mechanist Collegia vaults, forbidden data, relic machines, extreme faction trust", "Archeotech recipes, relic machinery ceilings, exceptional power/medical/weapon/clothing outputs");
        add(m, "Civilian Provisioning Patterns", "faction/production", 2, "markets, hab kitchens, civic stores", "civilian faction equivalents for rations, water, cots, tools, and shop goods");
        add(m, "Military Logistics Patterns", "faction/production", 4, "Astra Militarum quartermasters, depots, contraband manuals", "military ration packs, rugged tools, armor, weapons, and field infrastructure");
        add(m, "Mechanist Collegia Fabrication Rites", "faction/production", 5, "forge cloisters, tech-adepts, machine cult data", "Mechanist Collegia machine goods, relays, boilers, assemblers, and efficient production recipes");

        // Infrastructure and utilities.
        add(m, "Civic Footpath Recognition", "infrastructure/roads", 1, "street use, signs, maintenance paint", "recognition of sidewalks, road edges, and safe footpath tiles");
        add(m, "Road Edge Orientation", "infrastructure/roads", 2, "road crews, paver markings, district maps", "directional road/sidewalk adjacency and edge-facing logic");
        add(m, "Plaza Layout Doctrine", "infrastructure/plaza", 3, "civic planners, plaza maps, district center surveys", "central plaza placement and civic-zone anchor logic");
        add(m, "Road-to-Plaza Joining", "infrastructure/plaza/roads", 4, "road crews, plaza plans, survey stakes", "joining roads inward so plazas connect to the circulation fabric");
        add(m, "Traffic Channel Planning", "infrastructure/roads", 6, "watch posts, hauler routes, municipal ledgers", "traffic channels, road continuity, and better road-based room frontage");
        add(m, "District Circulation Planning", "infrastructure/roads/district", 8, "district plans, Civic Ledger Office zoning records, utility surveys", "large-scale movement planning across plazas, roads, corridors, and transition routes");
        add(m, "Maintenance Access Recognition", "infrastructure/maintenance", 1, "service hatches, worker markings, utility rooms", "recognition of maintenance corridors and access strips");
        add(m, "Utility Corridor Orientation", "infrastructure/maintenance", 2, "utility workers, corridor stamps, service plans", "directional maintenance corridor orientation and same-family tile joining");
        add(m, "Bulkhead Service Routes", "infrastructure/maintenance", 4, "bulkhead crews, void-safety manuals, service maps", "bulkhead-adjacent service routes and safer transition placement");
        add(m, "External Void Corridor Safety", "infrastructure/void", 6, "voidside workers, hazard briefings, exterior maintenance doctrine", "external void corridor rules and edge-safety discipline");
        add(m, "Maintenance Network Continuity", "infrastructure/maintenance", 8, "district utility offices, maintenance supervisors, route ledgers", "connected maintenance service networks instead of isolated bands");
        add(m, "District Service Spine Planning", "infrastructure/maintenance/district", 10, "public works planners, Mechanist Collegia utility surveys", "district-scale utility spines and long-run maintenance access planning");
        add(m, "Torch and Lamp Handling", "infrastructure/lighting", 1, "lamplighters, hab stewards, emergency lamps", "basic lamp recognition and hand-placed light safety");
        add(m, "Local Fixture Illumination", "infrastructure/lighting", 2, "workshop lamps, wall fixtures, dormitory lighting", "local light fixtures and room-level illumination planning");
        add(m, "Wall-Respecting Light Casting", "infrastructure/lighting", 4, "lighting audits, maintenance workers, line-of-sight repair notes", "light that illuminates blocking walls without bleeding through them");
        add(m, "Grid Light Discipline", "infrastructure/lighting", 6, "utility diagrams, emergency lighting tests", "consistent grid lighting, improved falloff, and dark-space readability");
        add(m, "Emergency Lighting Networks", "infrastructure/lighting", 8, "generator rooms, safety officers, alarm stations", "emergency light networks and power-loss visibility support");
        add(m, "Civic Lighting Authority", "infrastructure/lighting/civic", 10, "municipal departments, Civic Ledger Office lighting budgets", "district lighting authority and larger safe-lit civic spaces");
        add(m, "Waste Channel Recognition", "infrastructure/sewer", 1, "sewer grates, sump drains, bad smells with doctrine", "waste channel and sewer tile recognition");
        add(m, "Sewer Corridor Navigation", "infrastructure/sewer", 2, "sump guides, maintenance workers, hazard maps", "sewer corridor navigation and specialized sewer corridor awareness");
        add(m, "Drainage Tile Discipline", "infrastructure/sewer", 3, "drain plans, utility stamps, flood marks", "drainage-tile placement, orientation, and obstruction warnings");
        add(m, "Waste Treatment Machinery", "infrastructure/sewer/machine", 5, "waste plants, recycler workers, municipal manuals", "waste treatment machinery and sewer utility rooms");
        add(m, "Municipal Waste Authority", "infrastructure/sewer/civic", 8, "district utility authority, public health records", "municipal waste permissions and high-scale treatment planning");
        add(m, "Zone Infrastructure Doctrine", "infrastructure/convergence", 6, "Zone Audit study, public works experience, construction notes", "combined roads, plaza, utility, sewer, and lighting doctrine for zone construction");
        add(m, "Public Works Familiarity", "infrastructure/convergence", 5, "civic crews, signage, maintenance supervisors", "public works literacy and safer interpretation of utility spaces");
        add(m, "District Utility Authority", "infrastructure/convergence", 12, "district charters, utility ledgers, high-trust civic access", "district-wide utility permissions and advanced infrastructure planning");

        // Industry and production workflow.
        add(m, "Hand Assembly", "industry/manual", 1, "workbenches, salvaged tools, practical necessity", "hand assembly and very early manual crafting discipline");
        add(m, "Bench Work", "industry/manual", 2, "bench workers, shop foremen, repair desks", "bench work, repair surfaces, and small-craft reliability");
        add(m, "Tool-Aided Crafting", "industry/manual", 3, "toolboxes, workshop ledgers, experienced laborers", "tool-aided crafting and improved manual operation");
        add(m, "Workshop Procedure", "industry/manual", 4, "workshop foremen, labor rotas, safety notes", "workshop procedure and repeatable small production queues");
        add(m, "Production Bench Authority", "industry/manual", 6, "shop charters, guild tolerances, foreman approval", "authorized production benches and more reliable work orders");
        add(m, "Crude Machine Handling", "industry/machine", 1, "bad machines, emergency equipment, first burns", "crude machine handling and risky machine operation");
        add(m, "Basic Machine Operation", "industry/machine", 2, "machine rooms, manuals, foreman supervision", "basic machine operation and early apparatus use");
        add(m, "Maintenance Cycle Awareness", "industry/machine", 4, "maintenance logs, tool cabinets, machine faults", "maintenance cycle awareness and machine status interpretation");
        add(m, "Machine Safety Discipline", "industry/machine", 5, "factory warnings, injury reports, safety briefings", "safer machine use, jam/fault recognition, and reduced catastrophic errors");
        add(m, "Industrial Apparatus Operation", "industry/machine", 7, "factory supervisors, Mechanist Collegia notes, production lines", "industrial apparatus operation and larger machinery permissions");
        add(m, "Automated Production Cells", "industry/machine/automation", 10, "logic Engine stations, factory relays, automation records", "automated production-cell authority and later staffed/automated machine queues");
        add(m, "Scrap Material Recognition", "industry/materials", 1, "scrap bins, warehouses, refit yards", "scrap material recognition and material-family sorting");
        add(m, "Wood-Cloth-Metal Sorting", "industry/materials", 2, "warehouse sorters, workshops, recycler bins", "wood, cloth, and metal sorting without confusing base material families");
        add(m, "Component Recovery", "industry/materials", 3, "machine junk, toolboxes, repair rooms", "component recovery from salvage and ruined machinery");
        add(m, "Industrial Component Handling", "industry/materials", 5, "depots, fabrication rooms, stockroom clerks", "industrial component handling and safer supply-room routing");
        add(m, "Refined Material Processing", "industry/materials", 7, "refineries, smelters, chemical rooms", "refined material processing and stronger production chains");
        add(m, "Specialist Material Chains", "industry/materials", 10, "guild schools, restricted manifests, Mechanist Collegia tutors", "specialist material chains and rare input handling");
        add(m, "Container Discipline", "industry/logistics", 1, "stockrooms, scavenging, warehouse floors", "container recognition and basic storage behavior");
        add(m, "Stockpile Recognition", "industry/logistics", 2, "haulers, stockrooms, cargo clerks", "stockpile recognition and local supply grouping");
        add(m, "Supply Shelf Practice", "industry/logistics", 3, "shop shelves, storehouse workers, item ledgers", "supply shelf use and correct shelf/barrel icon semantics");
        add(m, "Room Container Standards", "industry/logistics", 4, "warehouse plans, room audits, supply officers", "container standards by room purpose and faction ownership");
        add(m, "Warehouse Routing", "industry/logistics", 6, "warehouse ledgers, hauler routes, cargo doors", "warehouse routing and supply-flow literacy");
        add(m, "District Logistics Authority", "industry/logistics", 9, "district depots, transit offices, munitorum ledgers", "district-scale logistics permissions and supply planning");
        add(m, "Workshop Doctrine", "industry/convergence", 5, "manual craft, machines, and materials studied together", "workshop-level industrial workflow doctrine");
        add(m, "Industrial Workflow Doctrine", "industry/convergence", 8, "factories, production rooms, queue audits", "multi-room industrial workflow and operation queues");
        add(m, "Production Line Authority", "industry/convergence", 12, "major factories, civic Ledger Office audits, Mechanist Collegia approval", "production line authority and advanced manufacturing orchestration");
        add(m, "Workshop Labor Discipline", "faction/automation", 2, "factory foremen, Mechanist Collegia adepts, labor unions", "recruit assignment to machines and production control");

        // Security and fieldcraft.
        add(m, "Improvised Blades", "security/melee", 1, "street fights, scrap knives, desperate lessons", "improvised blade recognition and crude melee handling");
        add(m, "Scrap Knife Handling", "security/melee", 1, "scavenger camps, gang alleys, tool abuse", "scrap knife use, recognition, and correct knife equipment/icon literacy");
        add(m, "Club and Tool Fighting", "security/melee", 2, "labor violence, tool yards, underhive brawls", "club, pipe, maul, and tool-weapon familiarity");
        add(m, "Trench Melee", "security/melee", 4, "military veterans, gang drills, pit fights", "close fighting, confined-room melee, and combat feedback hooks");
        add(m, "Shock Assault Doctrine", "security/melee", 7, "elite fighters, assault patrols, brutal experience", "shock assault melee doctrine and later morale/charge effects");
        add(m, "Stub Weapon Familiarity", "security/ballistics", 1, "hivers, gangers, PDF instructors, weapons lockers", "stub weapon identification and safe crude firearm handling");
        add(m, "Ballistic Maintenance", "security/ballistics", 3, "armorers, guard depots, evidence lockers", "ballistic weapon maintenance and ammunition recognition");
        add(m, "Ammunition Recognition", "security/ballistics", 3, "range tables, quartermasters, corpses searched carefully", "ammunition identification and safer ballistic supply handling");
        add(m, "Rifle Discipline", "security/ballistics", 5, "PDF drills, guard veterans, training ranges", "rifle use, range discipline, and field maintenance");
        add(m, "Heavy Ballistic Weapon Handling", "security/ballistics", 8, "heavy weapon crews, munitorum stores, dangerous tutors", "heavy stubber/autocannon class familiarity and crewed-weapon literacy");
        add(m, "Power Cell Recognition", "security/energy", 1, "power rooms, las cells, Mechanist Collegia warnings", "power cell recognition and energy weapon caution");
        add(m, "Las Weapon Familiarity", "security/energy", 3, "PDF instructors, light Rifle drills, forge tutors", "las weapon familiarity and cell handling");
        add(m, "Energy Weapon Maintenance", "security/energy", 5, "Mechanist Collegia notes, charged benches, tech-adepts", "energy weapon maintenance and risk management");
        add(m, "Overheat Discipline", "security/energy", 6, "weapon failure reports, forge warnings, field drills", "overheat discipline and thermal risk literacy");
        add(m, "High-Energy Weapon Doctrine", "security/energy", 9, "restricted armories, tech-priest supervision, elite training", "high-energy weapon doctrine and late-game weapon permissions");
        add(m, "Scavenger Clothing Familiarity", "security/armor", 1, "rags, labor clothes, bad weather", "scavenger clothing recognition and basic equipment/clothing literacy");
        add(m, "Padded Protection", "security/armor", 2, "tailors, street armorers, workwear stores", "padded protection and early armor layering");
        add(m, "Flak Layering", "security/armor", 4, "PDF stores, militarized tailors, armorers", "flak armor layering and medium protective gear literacy");
        add(m, "Civic Wardens Armor Recognition", "security/armor/faction", 5, "Civic Wardens precincts, evidence lockers, legal warnings", "Civic Wardens armor recognition and restricted armor awareness");
        add(m, "PDF Armor Familiarity", "security/armor/faction", 5, "PDF depots, quartermasters, veterans", "PDF armor familiarity and militia-grade protection literacy");
        add(m, "Heavy Protective Doctrine", "security/armor", 8, "heavy armor stores, military tutors, battlefield experience", "heavy protective doctrine and late armor permissions");
        add(m, "Trip Hazard Recognition", "security/traps", 1, "bad alleys, worksite hazards, obvious wires", "trap and trip-hazard recognition");
        add(m, "Simple Trap Disarming", "security/traps", 3, "gang tricks, safety drills, patient fingers", "simple trap disarming and trap-inspection literacy");
        add(m, "Door Security Awareness", "security/traps/doors", 3, "locks, bad doors, patrol notes", "door security awareness and transition/access risk reading");
        add(m, "Alarm Fixture Recognition", "security/traps/alarm", 5, "alarm stations, security rooms, electrics", "alarm fixture recognition and later installation permissions");
        add(m, "Defensive Room Planning", "security/traps/rooms", 7, "guards, checkpoint planners, patrol supervisors", "defensive room planning with doors, cover, traps, and sightlines");
        add(m, "Security Network Authority", "security/traps/network", 11, "Civic Wardens, Mechanist Collegia security, district command", "security-network authority and advanced base-defense orchestration");
        add(m, "Security Logic Engine Rites", "security/logic Engine", 3, "Civic Wardens/Mechanist Collegia services, restricted slates", "Security Logic Engine Node construction, powered defense turrets, and base-defense automation");

        // Medicine and biology.
        add(m, "Wound Recognition", "medical/first-aid", 1, "injuries, medicae signs, practical fear", "wound recognition and first-aid targeting");
        add(m, "Basic Bandaging", "medical/first-aid", 2, "bandage rolls, medicae stalls, field notes", "basic bandaging, bleeding control, and early medical recipes");
        add(m, "Pain and Shock Management", "medical/first-aid", 3, "field medics, bad recoveries, clinic slates", "pain and shock management in injuries and treatment text");
        add(m, "Field Treatment", "medical/first-aid", 4, "military aid manuals, medicae workers, battlefield triage", "field treatment and portable care practice");
        add(m, "Clinic Procedure", "medical/first-aid", 6, "clinic slates, medicae queues, sterile-room discipline", "clinic procedure and treatment-room permissions");
        add(m, "Emergency Medical Authority", "medical/first-aid", 9, "emergency clinics, medicae sanction, triage command", "emergency medical authority and stronger recovery services");
        add(m, "Filth Recognition", "medical/sanitation", 1, "trash rooms, corpse odor, sewer life", "filth recognition and contamination warnings");
        add(m, "Corpse Handling Discipline", "medical/sanitation/corpse", 2, "Civic Wardens cleanup, medicae warnings, death rooms", "corpse containers, cleanup handling, and decay/skeleton flow literacy");
        add(m, "Waste Isolation", "medical/sanitation", 3, "sewer crews, waste rooms, public health warnings", "waste isolation and safer dirty-room handling");
        add(m, "Disease Vector Awareness", "medical/sanitation", 5, "medicae warnings, plague stories, sanitation records", "disease vector awareness and infection-risk literacy");
        add(m, "Sanitation Room Procedure", "medical/sanitation", 6, "washroom supervisors, medicae sanitation, waste ledgers", "sanitation room procedure and safer communal wash spaces");
        add(m, "Public Health Authority", "medical/sanitation", 10, "district health offices, high-trust medicae charters", "public health authority and district-level sanitation response");
        add(m, "Herbal-Scavenged Remedies", "medical/craft", 1, "market herbs, scavenged supplies, field improvisation", "scavenged remedy preparation and risky low-tier medicine");
        add(m, "Crude Medicine Preparation", "medical/craft", 3, "backroom medics, chem stalls, field packs", "crude medicine recipes and basic treatment supply production");
        add(m, "Standard Medicine Patterns", "medical/craft", 5, "medicae slates, clinic manuals, pharmacy ledgers", "standard medicine patterns and safer medical-processing recipes");
        add(m, "Sterile Handling", "medical/craft", 7, "sterile rooms, medicae training, clean benches", "sterile handling and reduced infection risk in medicine production");
        add(m, "Advanced Pharmaceutical Practice", "medical/craft", 11, "restricted clinics, chem guilds, medicae authorities", "advanced pharmaceutical practice and high-tier medicine production");
        add(m, "Field Medicae Practices", "medical/business", 2, "medicae stalls, rogue doctors, clinic slates, military aid manuals", "backroom medicae stalls, paid treatment services, and medical-stock business returns");

        // Civic, faction, and commerce.
        add(m, "Barter Familiarity", "civic/commerce", 1, "markets, barter stalls, desperate negotiations", "barter familiarity and simple trade literacy");
        add(m, "Price Recognition", "civic/commerce", 2, "vendors, ledgers, repeated mistakes", "price recognition and shop-value awareness");
        add(m, "Trade Ledger Reading", "civic/commerce", 3, "shopkeepers, scribe ledgers, guild records", "trade ledger reading and more reliable buy/sell decisions");
        add(m, "Vendor Trust", "civic/commerce", 5, "repeated business, faction reputation, shop records", "vendor trust and better service access");
        add(m, "Contract Negotiation", "civic/commerce/contracts", 6, "contract desks, faction reps, job boards", "contract negotiation and better reward/risk literacy");
        add(m, "Licensed Commerce", "civic/commerce/legal", 8, "permits, counters, noble factors, Civic Ledger Office offices", "licensed commerce and lawful business operation");
        add(m, "Commercial Ledgers", "commerce", 1, "shopkeepers, traders, guild records", "faction stores, pricing bonuses, business operation literacy");
        add(m, "Commerce Permits", "commerce/legal", 2, "noble factors, civic Ledger Office counters, traders with paperwork", "shop counters, lawful business licensing, and illegal-commerce risk literacy");
        add(m, "Civic Signage Recognition", "civic/civic Ledger Office", 1, "signs, placards, public warnings", "civic signage recognition and legal-zone reading");
        add(m, "Hab Block Rules", "civic/civic Ledger Office", 2, "hab notices, rent ledgers, wardens", "hab block rules and civilian housing legality");
        add(m, "Civic Ledger Office Forms", "civic/civic Ledger Office", 3, "forms, queues, stamps, clerk hostility", "Civic Ledger Office forms and basic paper authority");
        add(m, "Permit Filing", "civic/civic Ledger Office", 4, "permit desks, civic counters, faction paperwork", "permit filing and restricted construction/service requests");
        add(m, "Civic Room Authority", "civic/civic Ledger Office", 6, "zoning offices, civic ledgers, room audits", "civic room authority and lawful room-purpose understanding");
        add(m, "District Governance Access", "civic/civic Ledger Office", 10, "district offices, senior clerks, high standing", "district governance access and advanced civil services");
        add(m, "Faction Color Recognition", "civic/faction", 1, "banners, uniforms, signs, hard lessons", "faction color and symbol recognition");
        add(m, "Local Reputation Awareness", "civic/faction", 2, "rumor, shop reactions, patrol treatment", "local reputation awareness and faction consequence literacy");
        add(m, "Civilian Authority Customs", "civic/faction", 3, "market stewards, clerks, hab bosses", "civilian authority customs and basic lawful interaction");
        add(m, "Civic Wardens Protocol", "civic/faction/security", 5, "precinct warnings, legal briefings, survivor stories", "Civic Wardens protocol and law-enforcement interaction literacy");
        add(m, "Mechanist Collegia Protocol", "civic/faction/mechanist Collegia", 5, "forge etiquette, tech-adept warnings, machine cult rites", "Mechanist Collegia protocol and safer machine-cult service access");
        add(m, "Noble House Protocol", "civic/faction/noble", 7, "estate rules, servants, livery, social punishment", "Noble house protocol and high-status social access");
        add(m, "Trespass Awareness", "civic/access", 1, "locked doors, angry guards, bad exits", "trespass awareness and restricted-zone reading");
        add(m, "Door Permission Recognition", "civic/access", 2, "door signs, guards, civic maps", "door permission recognition and access checks");
        add(m, "Container Ownership Recognition", "civic/access", 3, "shopkeepers, warehouses, law warnings", "container ownership recognition and stolen-item risk literacy");
        add(m, "Restricted Area Awareness", "civic/access", 4, "patrols, painted lines, legal briefings", "restricted-area awareness and faction-room boundary reading");
        add(m, "Permit-Based Access", "civic/access", 6, "permits, access papers, work orders", "permit-based access to doors, rooms, containers, and services");
        add(m, "Faction Access Authority", "civic/access", 10, "high trust, faction reps, permits, service charters", "faction access authority and advanced restricted-service entry");
        add(m, "Civil Operating Doctrine", "civic/convergence", 6, "commerce, permits, faction practice, survival civics", "civil operating doctrine across law, commerce, and faction interaction");
        add(m, "Faction Service Access", "civic/convergence", 8, "faction representatives, vendors, service desks", "faction service access and broader legal services");
        add(m, "District Permit Authority", "civic/convergence", 12, "district offices, senior faction trust, high standing", "district permit authority and late civic permissions");

        // Mechanist and archeotech doctrine.
        add(m, "Machine Respect", "mechanist/reverence", 1, "machine rooms, foreman warnings, pain from mistakes", "machine respect and basic caution around machinery");
        add(m, "Maintenance Litany", "mechanist/reverence", 2, "repair prayers, machine oil notes, worker tradition", "maintenance litany and simple machine appeasement routines");
        add(m, "Machine Appeasement", "mechanist/reverence", 4, "Mechanist Collegia adepts, ritual notes, machine misbehavior", "machine appeasement and better repair/maintenance interpretation");
        add(m, "Sanctified Repair Practice", "mechanist/reverence", 6, "sacred oil, sanctioned repair benches, forge tutors", "sanctified repair practice and later machine-sanctity hooks");
        add(m, "Machine Spirit Interpretation", "mechanist/reverence", 9, "tech-priests, fault patterns, machine behavior", "machine spirit interpretation and high-trust machine diagnostics");
        add(m, "Terminal Recognition", "mechanist/data", 1, "terminals, data plates, shop consoles", "terminal recognition and safe interface caution");
        add(m, "Data Slate Reading", "mechanist/data", 2, "data slates, manuals, corrupted notes", "data slate reading and improved doctrine discovery");
        add(m, "Logic Engine Interface Familiarity", "mechanist/data", 4, "logic Engine stations, clerks, tech-adepts", "logic Engine interface familiarity and simple terminal use");
        add(m, "Archive Query Discipline", "mechanist/data", 6, "archives, search forms, data indexes", "archive query discipline and better information retrieval");
        add(m, "Machine Logic Interpretation", "mechanist/data", 8, "logic diagrams, failure analysis, Mechanist Collegia instruction", "machine logic interpretation and supervised automation literacy");
        add(m, "Restricted Data Handling", "mechanist/data", 11, "restricted archives, sealed access, dangerous data", "restricted data handling and late knowledge-gate interactions");
        add(m, "Ancient Component Recognition", "mechanist/archeotech", 3, "old parts, strange power signatures, Mechanist Collegia warnings", "ancient component recognition and salvage caution");
        add(m, "Forbidden Pattern Caution", "mechanist/archeotech", 5, "bad stories, sealed warnings, tech-priest refusal", "forbidden pattern caution and safer relic handling");
        add(m, "Archeotech Signal Recognition", "mechanist/archeotech", 8, "scanner anomalies, vault slates, relic rooms", "archeotech signal recognition and relic trace reading");
        add(m, "Fragmentary STC Interpretation", "mechanist/archeotech", 12, "fragmentary prints, vault data, Mechanist Collegia oversight", "fragmentary STC interpretation and partial relic-pattern decoding");
        add(m, "Dangerous Device Procedure", "mechanist/archeotech", 14, "sealed devices, explosion reports, sanctioned supervision", "dangerous device procedure and late relic operation caution");
        add(m, "Mechanist Initiate Doctrine", "mechanist/convergence", 4, "machine respect, data slates, early fabrication", "combined machine, data, and fabrication initiation");
        add(m, "Sacred Systems Familiarity", "mechanist/convergence", 9, "Mechanist Collegia service, cogitators, machine rites", "sacred systems familiarity and cross-linked advanced machine doctrine");
    }
    static void add(LinkedHashMap<String,KnowledgeDef> m, String n, String f, int c, String s, String u){ m.put(n, new KnowledgeDef(n,f,c,s,u)); }
    static KnowledgeDef get(String n){ return all().get(n); }
    static java.util.List<String> names(){ return new ArrayList<String>(all().keySet()); }
    java.util.List<String> detailLines(){
        ArrayList<String> l = new ArrayList<>();
        l.add("Knowledge: " + name);
        l.add("Family: " + family);
        l.add("Credit cost: " + cost);
        l.add("Sources: " + source);
        l.add("Unlocks: " + unlocks);
        l.add("Implementation rule: knowledge gates recipes, machine operation, services, business paths, and conversation options rather than only being flavor text.");
        return l;
    }

}


class QualityAuthorityProfile {
    final String name, role;
    final int tier;
    final double valueMultiplier, usefulnessMultiplier, reliabilityMultiplier, efficiencyMultiplier, defectMultiplier, comfortMultiplier, prestigeMultiplier;
    QualityAuthorityProfile(String name, int tier, double value, double useful, double reliable, double efficiency, double defect, double comfort, double prestige, String role) {
        this.name=name; this.tier=tier; this.valueMultiplier=value; this.usefulnessMultiplier=useful; this.reliabilityMultiplier=reliable; this.efficiencyMultiplier=efficiency; this.defectMultiplier=defect; this.comfortMultiplier=comfort; this.prestigeMultiplier=prestige; this.role=role;
    }
    String auditLine() {
        return name + " tier=" + tier + " value x" + fmt(valueMultiplier) + " usefulness x" + fmt(usefulnessMultiplier) + " reliability x" + fmt(reliabilityMultiplier) + " efficiency x" + fmt(efficiencyMultiplier) + " defects x" + fmt(defectMultiplier) + " comfort x" + fmt(comfortMultiplier) + " prestige x" + fmt(prestigeMultiplier) + " :: " + role;
    }
    static String fmt(double d){ return String.format(Locale.US, "%.2f", d); }
}


class QualityAuthorityApi {
    static final String[] QUALITY_ORDER = {"Junk", "Shoddy", "Common", "Serviceable", "Fine", "Masterwork", "Noble", "Archeotech"};
    static final String[] KNOWLEDGE_BANDS = {"Junk", "Common", "Serviceable", "Fine", "Masterwork", "Noble", "Archeotech"};
    static final int UNLIMITED_TIER = 7;
    static ArrayList<QualityAuthorityProfile> profiles() {
        ArrayList<QualityAuthorityProfile> p = new ArrayList<>();
        p.add(new QualityAuthorityProfile("Junk",0,0.20,0.35,0.45,0.55,2.20,0.35,0.15,"trash-grade emergency output; legal mostly because nobody admits owning it"));
        p.add(new QualityAuthorityProfile("Shoddy",1,0.45,0.60,0.65,0.72,1.55,0.55,0.30,"bad but repeatable output; the lowest sane production floor"));
        p.add(new QualityAuthorityProfile("Common",2,1.00,1.00,1.00,1.00,1.00,1.00,1.00,"ordinary reliable civic production"));
        p.add(new QualityAuthorityProfile("Serviceable",3,1.35,1.20,1.15,1.12,0.78,1.10,1.10,"workable professional production suitable for faction stores"));
        p.add(new QualityAuthorityProfile("Fine",4,1.90,1.55,1.32,1.25,0.55,1.30,1.45,"skilled workshop production with better tolerances"));
        p.add(new QualityAuthorityProfile("Masterwork",5,2.80,2.10,1.55,1.45,0.35,1.55,2.05,"rare expert production; expensive to maintain and hard to replace"));
        p.add(new QualityAuthorityProfile("Noble",6,4.25,3.00,1.70,1.30,0.30,2.50,3.25,"luxury/status production; comfort and prestige outrank price sanity"));
        p.add(new QualityAuthorityProfile("Archeotech",7,7.00,4.50,2.10,1.90,0.15,1.90,4.00,"relic-grade production ceiling; exceptional output with doctrine and access risk"));
        return p;
    }
    static QualityAuthorityProfile profile(String quality) {
        int t = tierIndex(quality);
        for (QualityAuthorityProfile p : profiles()) if (p.tier == t) return p;
        return profiles().get(2);
    }
    static String qualityName(int tier) { int i=Math.max(0, Math.min(QUALITY_ORDER.length-1, tier)); return QUALITY_ORDER[i]; }
    static int tierIndex(String text) {
        if (text == null || text.isBlank()) return 2;
        String s = text.trim().toLowerCase(Locale.ROOT);
        for (int i=0;i<QUALITY_ORDER.length;i++) if (s.equals(QUALITY_ORDER[i].toLowerCase(Locale.ROOT)) || s.startsWith(QUALITY_ORDER[i].toLowerCase(Locale.ROOT) + " ") || s.contains(" " + QUALITY_ORDER[i].toLowerCase(Locale.ROOT) + " ")) return i;
        return 2;
    }
    static int tierForBand(String band) { return tierIndex(band); }
    static int bestKnownTier(Set<String> unlocked) {
        int tier = 2; // Common is the civic baseline; specific doctrine raises or lowers it.
        if (unlocked == null) return tier;
        for (String k : unlocked) {
            String s = k == null ? "" : k.toLowerCase(Locale.ROOT);
            if (s.contains("junk fabrication")) tier = Math.max(tier, 0);
            if (s.contains("serviceable fabrication")) tier = Math.max(tier, 3);
            if (s.contains("fine ") && s.contains(" patterns")) tier = Math.max(tier, 4);
            if (s.contains("masterwork ") && s.contains(" patterns")) tier = Math.max(tier, 5);
            if (s.contains("noble manufactury") || (s.contains("noble ") && s.contains(" patterns"))) tier = Math.max(tier, 6);
            if (s.contains("archeotech production") || (s.contains("archeotech ") && s.contains(" patterns"))) tier = Math.max(tier, 7);
        }
        return Math.max(0, Math.min(UNLIMITED_TIER, tier));
    }
    static int requiredTierForRecipeKnowledge(String recipeKnowledge) {
        if (recipeKnowledge == null || recipeKnowledge.isBlank()) return 2;
        String s = recipeKnowledge.toLowerCase(Locale.ROOT);
        if (s.contains("archeotech")) return 7;
        if (s.contains("noble")) return 6;
        if (s.contains("masterwork")) return 5;
        if (s.contains("fine")) return 4;
        if (s.contains("serviceable")) return 3;
        if (s.contains("shoddy")) return 1;
        if (s.contains("junk")) return 0;
        return 2;
    }
    static int cappedTier(int knownTier, int recipeTier, int machineTier, int inputTier, int facilityTier, int workerTier) {
        int cap = Math.min(Math.min(knownTier, recipeTier), machineTier);
        cap = Math.min(cap, inputTier < 0 ? UNLIMITED_TIER : inputTier);
        cap = Math.min(cap, facilityTier < 0 ? UNLIMITED_TIER : facilityTier);
        cap = Math.min(cap, workerTier < 0 ? UNLIMITED_TIER : workerTier);
        return Math.max(0, Math.min(UNLIMITED_TIER, cap));
    }
    static String cappedQualityName(String recipeKnowledge, int knownTier, int machineTier, int inputTier, int facilityTier, int workerTier) {
        return qualityName(cappedTier(knownTier, Math.min(knownTier, requiredTierForRecipeKnowledge(recipeKnowledge)), machineTier, inputTier, facilityTier, workerTier));
    }
    static ArrayList<String> detailLines() {
        ArrayList<String> l = new ArrayList<>();
        l.add("Quality Authority Layer: centralized quality meanings, modifiers, and production ceilings.");
        l.add("Capping rule: output quality = min(known doctrine tier, recipe requirement tier, machine ceiling tier, input-material tier, facility tier, worker tier). Input/facility/worker tiers are authority hooks and default open until their ledgers are activated.");
        for (QualityAuthorityProfile p : profiles()) l.add(p.auditLine());
        return l;
    }
    static ArrayList<String> auditLines() {
        ArrayList<String> l = new ArrayList<>();
        l.add("QualityAuthority tiers=" + profiles().size() + " order=" + String.join(" > ", QUALITY_ORDER));
        for (QualityAuthorityProfile p : profiles()) l.add("QUALITY " + p.auditLine());
        return l;
    }
}


class KnowledgeTreeApi {
    static final String[] CATEGORIES = {"Food Processing", "Water Purification", "Metallurgy", "Textile Fabrication", "Medical Processing", "Chemical Synthesis", "Ballistics", "Energy Systems", "Industrial Maintenance", "Agricultural Processing", "Salvage Processing", "Tools", "Melee Weapons", "Armor", "Machinery", "Construction Materials"};
    static void registerKnowledgeDefs(LinkedHashMap<String,KnowledgeDef> m) {
        for (String cat : CATEGORIES) for (String band : QualityAuthorityApi.KNOWLEDGE_BANDS) {
            String n = band + " " + cat + " Patterns";
            if (m.containsKey(n)) continue;
            int tier = QualityAuthorityApi.tierForBand(band);
            int cost = costForBand(band);
            String fam = "knowledge-tree/" + cat.toLowerCase(Locale.ROOT).replace(' ', '-');
            String src = sourceFor(cat, band);
            String unlock = band + "-tier " + cat.toLowerCase(Locale.ROOT) + " recipes, inspection text, machine permissions, and faction-production variants.";
            KnowledgeDef.add(m, n, fam, cost, src, unlock);
        }
    }

    static int costForBand(String band) {
        String b = band == null ? "" : band.trim().toLowerCase(Locale.ROOT);
        if (b.equals("junk")) return 1;
        if (b.equals("common")) return 2;
        if (b.equals("serviceable")) return 4;
        if (b.equals("fine")) return 6;
        if (b.equals("masterwork")) return 8;
        if (b.equals("noble")) return 11;
        if (b.equals("archeotech")) return 14;
        return 3;
    }
    static String sourceFor(String cat, String band) {
        String c = cat.toLowerCase(Locale.ROOT);
        String b = band.toLowerCase(Locale.ROOT);
        if (b.contains("archeotech")) return "sealed vault data, Mechanist Collegia relic custody, forbidden manuals, extreme faction trust";
        if (b.contains("noble")) return "noble factors, guild tutors, estate ledgers, restricted manufactury schools";
        if (c.contains("ballistics")) return "armorers, guard quartermasters, gangers, Civic Wardens evidence lockers";
        if (c.contains("energy")) return "Mechanist Collegia tutors, power-room slates, forge diagnostic benches";
        if (c.contains("water")) return "water guild notes, condenser manuals, sump reclamation workers";
        if (c.contains("food") || c.contains("agricultural")) return "kitchens, vat tenders, hydroponic workers, orchard serfs";
        if (c.contains("textile")) return "tailors, uniform stores, armorers, hab laundries";
        if (c.contains("medical")) return "medicae stalls, clinic slates, military aid manuals";
        if (c.contains("chemical")) return "chem kitchens, sump stills, lab notes, guild chemical ledgers";
        if (c.contains("salvage")) return "scrap bosses, scavver caches, machine-room junk ledgers";
        return "workshops, trainers, data slates, faction ledgers";
    }
    static ArrayList<String> detailLines() {
        ArrayList<String> l = new ArrayList<>();
        l.add("Knowledge Tree Authority: the composed progression branches register production doctrine categories as buyable/inspectable character-owned knowledge families.");
        l.add("Bands per category: " + String.join(", ", QualityAuthorityApi.KNOWLEDGE_BANDS) + ". Shoddy remains a degradation quality, not a target doctrine school.");
        for (String cat : CATEGORIES) l.add(cat + " -> " + String.join(" / ", QualityAuthorityApi.KNOWLEDGE_BANDS));
        return l;
    }
    static ArrayList<String> auditLines() {
        ArrayList<String> l = new ArrayList<>();
        LinkedHashMap<String,KnowledgeDef> all = KnowledgeDef.all();
        int expected = CATEGORIES.length * QualityAuthorityApi.KNOWLEDGE_BANDS.length;
        int present = 0;
        for (String cat : CATEGORIES) for (String band : QualityAuthorityApi.KNOWLEDGE_BANDS) if (all.containsKey(band + " " + cat + " Patterns")) present++;
        l.add("KnowledgeTree categories=" + CATEGORIES.length + " qualityBands=" + QualityAuthorityApi.KNOWLEDGE_BANDS.length + " expected=" + expected + " present=" + present);
        for (String cat : CATEGORIES) l.add("KNOWLEDGE_TREE " + cat + " bands=" + String.join(",", QualityAuthorityApi.KNOWLEDGE_BANDS));
        l.addAll(KnowledgeBranchDefinitions.auditLines());
        return l;
    }
}


