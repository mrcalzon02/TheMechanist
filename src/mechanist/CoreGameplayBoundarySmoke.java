package mechanist;

import java.util.Arrays;
import java.util.Random;

/** Real client transactions and a second run in the same panel. */
final class CoreGameplayBoundarySmoke {
    public static void main(String[] args) {
        GamePanel game = new GamePanel();
        game.timer.stop();
        try {
            // Match the 500-tile setup restored by the full campaign decoder.
            game.world = new World(941L, 500, 500);
            game.seed = 941L;
            game.worldSetup.zoneSize = 0;
            game.worldSetup.npcDensity = 0;
            game.worldSetup.simulationAge = 0;
            for (char[] column : game.world.tiles) Arrays.fill(column, '.');
            game.active = Candidate.random(new Random(941));
            game.playerX = game.playerY = 12;
            ItemProvenanceRecord first = ItemProvenanceRecord.found("Machine part", game.world, 0, "first supplier");
            ItemProvenanceRecord second = ItemProvenanceRecord.found("Machine part", game.world, 0, "second supplier");
            game.addInventoryItem("Machine part", first);
            game.addInventoryItem("Machine part", second);
            check(ProductionContainerAuthority.countAvailable(game, "Machine part") == 2, "added units must be usable production inputs");
            var player = game.itemContainers.get(GamePanel.CONTAINER_PLAYER_INVENTORY);
            String firstId = player.itemInstanceIds.get(0);
            game.rebuildItemContainersFromLegacyLists();
            game.rebuildItemContainersFromLegacyLists();
            check(player.itemInstanceIds.size() == 2 && player.itemInstanceIds.get(0).equals(firstId), "rebuild preserves exact identities and multiplicity");
            check(game.itemInstances.get(firstId).provenance == first, "supplier provenance survives admission/rebuild");
            game.baseClaimed = true; game.baseX = game.playerX; game.baseY = game.playerY;
            ItemInstance moved = game.transferContainerItemByName(GamePanel.CONTAINER_PLAYER_INVENTORY,
                    game.inventory, "Machine part", GamePanel.CONTAINER_BASE_STORAGE, "Base storage", game.baseStorage, "test transfer");
            game.rebuildItemContainersFromLegacyLists();
            check(moved != null && moved.id.equals(firstId), "transfer must preserve identity");
            game.baseClaimed = false;
            check(game.inventory.size() == 1 && game.baseStorage.size() == 1 && game.itemInstances.size() == 2, "transfer/rebuild must not duplicate units");
            check(ProductionContainerAuthority.consumeOne(game, "Machine part", "test consumption").success, "consume transferred material");
            game.addInventoryItem("Wire bundle", null);
            check(game.itemInstances.size() == 2, "adding after deletion must not overwrite a surviving ID");
            check(game.itemInstances.values().stream().anyMatch(i -> i.provenance == second), "remaining supplier unit retained");
            game.inventory.clear();
            game.inventory.add("Ration wrapper");
            game.rebuildItemContainersFromLegacyLists();
            game.selectedInventoryIndex = 0;
            game.food = 10;
            int turn = game.turn;
            game.useSelectedInventoryItemBody();
            check(game.food == 10 && game.inventory.contains("Ration wrapper") && game.turn == turn, "junk wrappers must not become meals");
            game.inventory.clear();
            game.inventory.add("Emergency rations");
            game.rebuildItemContainersFromLegacyLists();
            game.useSelectedInventoryItemBody();
            check(game.food > 10 && game.inventory.isEmpty() && player.itemInstanceIds.isEmpty(), "real food consumes one unit in both views");
            game.addInventoryItem("Construction supplies", null);
            game.selectedInventoryIndex = 0;
            game.useSelectedInventoryItemBody();
            check(game.supplies == 1 && game.inventory.isEmpty() && player.itemInstanceIds.isEmpty(), "carried supplies pack into the construction pool exactly once");
            game.addInventoryItem("Reclaimed textile bundle", null);
            BaseObject cot = ProgressiveConstructionAuthority.createSite(BuildRecipe.cot(), 13, 12, 3);
            game.baseObjects.add(cot);
            ProgressiveConstructionAuthority.contribute(game, cot, 3, true);
            check(!cot.underConstruction && game.supplies == 0 && !game.inventory.contains("Reclaimed textile bundle"), "first cot can consume packed supplies, a registered textile and real labor");
            // Ordinary WAIT runs the same survival clock as movement and other turn actions.
            game.turn = 99;
            game.worldTurn = 99L;
            game.lastPlayerNeedsTurn = 99;
            game.food = 80;
            game.water = 80;
            game.sleepNeed = 20;
            game.fatigue = 8;
            game.waitOneTurn();
            check(game.turn == 100 && game.lastPlayerNeedsTurn == 100
                    && game.food == 79 && game.water == 79 && game.sleepNeed == 21
                    && game.fatigue == 9, "ordinary wait must advance food, hydration, sleep debt and fatigue once");
            // A longer action can advance several turns before the normal action-completion tick.
            game.turn = 249;
            game.worldTurn = 249L;
            game.waitOneTurn();
            check(game.turn == 250 && game.lastPlayerNeedsTurn == 250
                    && game.food == 78 && game.water == 76 && game.sleepNeed == 24
                    && game.fatigue == 10, "skipped turns must accrue every crossed survival threshold");

            // Hire a real adult, resident world actor after claiming an abandoned room.
            game.world.rooms.add(new java.awt.Rectangle(8, 8, 10, 10));
            game.world.roomProfiles.add(RoomProfile.generic());
            game.world.roomFactions.add(Faction.NONE);
            game.world.roomSpecials.add(Boolean.FALSE);
            for (int x = 8; x < 18; x++) for (int y = 8; y < 18; y++) game.world.roomIds[x][y] = 0;
            game.playerX = 12; game.playerY = 12;
            NpcEntity resident = NpcEntity.create(Faction.HIVER, game.world.zoneType, 11, 12, new Random(946));
            resident.id = "HIRED-RESIDENT-946";
            resident.name = "Tal Ren";
            resident.role = "Resident";
            resident.ageYears = 27;
            resident.hp = 12;
            resident.state = "Idle";
            game.world.npcs.add(resident);
            // This smoke constructs a panel without the new-game initializer:
            // give it the same 15 script the actual new-game path grants.
            game.carriedScript = 15;
            int startingScript = game.carriedScript;
            check(!PlayerRecruitmentAuthority.toggle(game, resident).success()
                    && game.carriedScript == startingScript && game.factionRecruits.isEmpty(),
                    "recruitment must require a real claimed base");
            check(RoomOwnershipAuthority.claimCurrentRoom(game).success(), "unowned room should become a founded base");
            NpcEntity underage = NpcEntity.create(Faction.HIVER, game.world.zoneType, 12, 11, new Random(947));
            underage.id = "MINOR-946"; underage.ageYears = 12; underage.name = "Child Applicant";
            game.world.npcs.add(underage);
            check(!PlayerRecruitmentAuthority.toggle(game, underage).success()
                    && game.carriedScript == startingScript, "minors may not sign work contracts");
            PlayerRecruitmentAuthority.Quote hire = PlayerRecruitmentAuthority.quote(game, resident);
            check(hire.ready() && hire.signingCost() > 0 && hire.capacity() >= 4,
                    "live resident recruitment should show a bounded affordable signing quote: " + hire.displayLine());
            PlayerRecruitmentAuthority.Result hired = PlayerRecruitmentAuthority.toggle(game, resident);
            check(hired.success() && hired.hired() && game.factionRecruits.size() == 1
                    && game.carriedScript == startingScript - hire.signingCost()
                    && resident.id.equals(game.factionRecruits.get(0).sourceNpcId),
                    "hiring must consume exact script and register the actor's persistent identity");
            check(!PlayerRecruitmentAuthority.toggle(game, underage).success()
                    && game.factionRecruits.size() == 1, "an invalid hire must not disturb the existing roster");
            game.playerX = game.playerY = 0;
            check(!PlayerRecruitmentAuthority.toggle(game, resident).success()
                    && game.factionRecruits.size() == 1, "remote dismissal must not mutate the roster");
            game.playerX = game.playerY = 12;
            BaseObject hireStation = new BaseObject("Crew Workbench", 'w', 14, 12, 0, 0);
            game.baseObjects.add(hireStation);
            check(ManualStaffingAssignmentAuthority.assign(game, hireStation, game.factionRecruits.get(0))
                            .contains("assigned"),
                    "hired world actor should be assignable to an actual built workbench");
            check(hireStation.assignedWorker.equals(resident.name),
                    "staffing control must bind the hired worker to a station");
            ItemProvenanceRecord carried = ItemProvenanceRecord.found("Machine part", game.world, game.turn, "saved supplier");
            game.addInventoryItem("Machine part", carried);
            String carriedId = player.itemInstanceIds.get(0);
            java.util.Properties checkpoint = new java.util.Properties();
            Persistence.writeCore(game, checkpoint);
            GamePanel resumed = new GamePanel();
            resumed.timer.stop();
            try {
                Persistence.readCore(resumed, checkpoint);
                check(resumed.factionRecruits.size() == 1
                        && "HIRED-RESIDENT-946".equals(resumed.factionRecruits.get(0).sourceNpcId),
                        "hired actor identity must survive campaign persistence");
                NpcEntity resumedResident = resumed.world.npcs.stream()
                        .filter(n -> "HIRED-RESIDENT-946".equals(n.id)).findFirst().orElseThrow();
                BaseObject resumedHireStation = resumed.baseObjects.stream()
                        .filter(b -> "Crew Workbench".equals(b.name)).findFirst().orElseThrow();
                check("Tal Ren".equals(resumedHireStation.assignedWorker),
                        "hired worker's station assignment must survive campaign persistence");
                check(resumed.turn == 250 && resumed.lastPlayerNeedsTurn == 250
                        && resumed.food == 78 && resumed.water == 76 && resumed.sleepNeed == 24
                        && resumed.fatigue == 10, "survival needs and the last-applied turn must persist");
                resumed.waitOneTurn();
                check(resumed.turn == 251 && resumed.lastPlayerNeedsTurn == 251
                        && resumed.food == 78 && resumed.water == 76 && resumed.sleepNeed == 24
                        && resumed.fatigue == 10, "first post-load turn must not replay already accounted survival costs");
                check(resumed.baseObjects.stream().anyMatch(b -> b.x == 13 && b.y == 12 && !b.underConstruction && b.symbol == 'c'), "completed cot survives full save/load");
                check(resumed.inventory.size() == 1 && resumed.containerItemCount(GamePanel.CONTAINER_PLAYER_INVENTORY) == 1, "saved inventory views resume with one exact unit");
                ItemInstance restoredUnit = resumed.itemInstances.get(carriedId);
                check(restoredUnit != null && restoredUnit.provenance != null && restoredUnit.provenance.unitId.equals(carried.unitId), "unit identity and supplier provenance survive full save/load");
                resumed.addInventoryItem("Wire bundle", null);
                check(resumed.itemInstances.size() == 2 && resumed.itemInstances.get(carriedId) == restoredUnit, "post-load additions cannot overwrite the restored unit");
                BaseObject restoredCot = resumed.baseObjects.stream().filter(b -> b.x == 13 && b.y == 12).findFirst().orElseThrow();
                resumed.fatigue = 8; resumed.sleepNeed = 30;
                resumed.setSize(1280, 720);
                resumed.lookX = restoredCot.x; resumed.lookY = restoredCot.y;
                resumed.confirmInteractionBody();
                java.awt.image.BufferedImage canvas = new java.awt.image.BufferedImage(1280, 720, java.awt.image.BufferedImage.TYPE_INT_ARGB);
                var graphics = canvas.createGraphics();
                try { resumed.paintComponent(graphics); } finally { graphics.dispose(); }
                check(resumed.buttons.stream().noneMatch(b -> "Craft".equals(b.label)), "cot must not offer a machine workbench");
                ButtonBox rest = resumed.buttons.stream().filter(b -> "Rest".equals(b.label)).findFirst().orElseThrow();
                int beforeRest = resumed.turn;
                rest.action.run();
                check(resumed.fatigue == 6 && resumed.sleepNeed == 20 && resumed.turn == beforeRest + 1, "rest action on the persisted cot uses existing restoration rates and one turn");
                resumed.playerX = resumed.playerY = 0;
                rest.action.run();
                check(resumed.fatigue == 6 && resumed.sleepNeed == 20 && resumed.turn == beforeRest + 1, "remote cot use is refused without mutation");
                resumed.playerX = resumed.playerY = 12;
                PlayerRecruitmentAuthority.Result dismissed = PlayerRecruitmentAuthority.toggle(resumed, resumedResident);
                check(dismissed.success() && !dismissed.hired() && resumed.factionRecruits.isEmpty()
                                && resumedHireStation.assignedWorker.isBlank(),
                        "dismissing a real NPC must free capacity and unstaff assigned machines");
                check(!PlayerRecruitmentAuthority.toggle(resumed, underage).success(),
                        "stale actors outside the restored world must not be hired");
            } finally { resumed.shutdownRuntime(); }

            game.seed = 941;
            game.worldSetup = WorldSetupSettings.standard();
            game.atlas = WorldAtlas.preview(game.seed, game.worldSetup);
            game.atlas.slices[0][0][1][1][6] = game.world;
            game.unlockedSkillNodes.add("investigation-trace-reading");
            game.factionStanding.put(Faction.BANDIT, 87);
            game.baseClaimed = true; game.claimedRoomId = 17;
            game.equippedRightHandItem = "Old weapon";
            game.loadedWeaponShots.put("Old weapon", 99);
            game.knowledgeXpProgress = 24;
            game.startPackagedClientNewGameWith(Candidate.random(new Random(942)), game.worldSetup);
            check(game.unlockedSkillNodes.isEmpty() && game.factionStanding.get(Faction.BANDIT) == 0, "new candidate must not inherit capabilities/standing");
            check(!game.baseClaimed && game.claimedRoomId == -1 && game.baseX == -1, "new run must not inherit base ownership");
            check(game.loadedWeaponShots.isEmpty() && game.equippedRightHandItem.equals("RIGHT EMPTY"), "new candidate must not inherit equipment/ammunition");
            check(game.knowledgeXpProgress == 0 && game.knowledgeCredits == 0, "new candidate must not inherit knowledge earning progress");
            check(game.lastPlayerNeedsTurn == 0, "new character must begin with a fresh survival clock");
            // Differentiate a sound world from one with defects rather than labelling
            // attempted repairs as an unconditional acceptance.
            check("ACCEPTED".equals(new World.GenerationValidationResult(
                            "test", 0, 3, 0, true, 4, 4).status()),
                    "zero-issue reachable world should be accepted");
            check("DEGRADED".equals(new World.GenerationValidationResult(
                            "test", 4, 3, 0, true, 4, 4).status()),
                    "remaining door/corridor issues must not be labelled accepted");
            check("BLOCKED".equals(new World.GenerationValidationResult(
                            "test", 1, 3, 1, true, 4, 4).status()),
                    "unreachable room must block structural acceptance");
            check("BLOCKED".equals(new World.GenerationValidationResult(
                            "test", 0, 3, 0, false, 4, 4).status()),
                    "unusable spawn must block structural acceptance");
            check(game.containerItemCount(GamePanel.CONTAINER_PLAYER_INVENTORY) == game.inventory.size(), "starting kit must be registered");
            System.out.println("CoreGameplayBoundarySmoke PASS inventoryIdentity=true provenance=true transfers=true noJunkMeals=true survivalClock=true savedNeeds=true finalWorldgenStatus=true recruitment=true hiredWorkerSave=true newRunIsolation=true");
        } finally {
            game.shutdownRuntime();
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
