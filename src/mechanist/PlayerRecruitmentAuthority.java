package mechanist;

import java.util.Locale;

/** Player-facing civilian recruitment, tied to a live NPC and the staffed-work roster. */
final class PlayerRecruitmentAuthority {
    private PlayerRecruitmentAuthority() { }

    record Quote(boolean ready, boolean hired, int signingCost, int assigned, int capacity, String explanation) {
        String actionLabel() { return hired ? "Dismiss Worker" : "Hire Worker"; }
        String displayLine() {
            return "Freehold crew " + assigned + "/" + capacity + ": " + explanation;
        }
    }
    record Result(boolean success, boolean hired, String message) { }

    static boolean dialogueCandidate(NpcEntity npc) {
        return npc != null && !npc.isAnimalActor()
                && !npc.isUntargetableAnchor() && !npc.isTrader();
    }

    static int capacity(GamePanel game) {
        if (game == null || !validBase(game)) return 0;
        int completeCots = 0;
        for (BaseObject object : game.baseObjects) {
            if (NeedSupplyInteractionAuthority.isBuiltBed(object)
                    && game.world.inBounds(object.x, object.y)
                    && game.world.roomIdAt(object.x, object.y) == game.claimedRoomId) completeCots++;
        }
        return Math.max(game.factionRecruits.size(), Math.min(20, 4 + Math.min(16, completeCots * 2)));
    }

    static RecruitWorker recruited(GamePanel game, NpcEntity npc) {
        if (game == null || npc == null) return null;
        for (RecruitWorker worker : game.factionRecruits) {
            if (worker == null) continue;
            if (npc.id != null && !npc.id.isBlank() && npc.id.equals(worker.sourceNpcId)) return worker;
            // Imported pre-identity rosters must not silently duplicate a named worker.
            if ((worker.sourceNpcId == null || worker.sourceNpcId.isBlank())
                    && worker.name != null && worker.name.equalsIgnoreCase(npc.name)) return worker;
        }
        return null;
    }

    static Quote quote(GamePanel game, NpcEntity npc) {
        int occupied = game == null ? 0 : game.factionRecruits.size();
        int limit = capacity(game);
        if (game == null || game.world == null || npc == null) return new Quote(false, false, 0, occupied, limit, "No live recruitment target.");
        RecruitWorker current = recruited(game, npc);
        boolean nearby = game.world.npcs.contains(npc)
                && Math.abs(game.playerX - npc.x) + Math.abs(game.playerY - npc.y) <= 1;
        if (current != null) {
            if (!nearby) return new Quote(false, true, 0, occupied, limit, "Approach this employee before releasing their contract.");
            return new Quote(true, true, 0, occupied, limit,
                    current.name + " is hired. Dismissal releases staffing assignments but preserves queued jobs.");
        }
        if (!dialogueCandidate(npc) || npc.hp <= 0 || npc.isMinorActor()
                || SkillTrainerAuthority.canTrain(npc))
            return new Quote(false, false, 0, occupied, limit, "This person is not available for civilian work.");
        if (npc.id == null || npc.id.isBlank())
            return new Quote(false, false, 0, occupied, limit, "This person has no persistent identity.");
        if (!nearby) return new Quote(false, false, 0, occupied, limit, "Approach this person to offer work.");
        if (!validBase(game)) return new Quote(false, false, 0, occupied, limit, "Claim an unowned room to found a freehold first.");
        if ("Hostile".equalsIgnoreCase(npc.state) || (npc.combatTargetId != null && !npc.combatTargetId.isBlank()))
            return new Quote(false, false, 0, occupied, limit, "A combatant cannot negotiate civilian work.");
        if (npc.faction != null && game.temporaryHostileTurns.getOrDefault(npc.faction, 0) > game.turn)
            return new Quote(false, false, 0, occupied, limit, "Active faction hostilities prevent hiring.");
        boolean local = npc.faction == null || npc.faction == Faction.NONE || npc.faction == Faction.SCAVENGER
                || FactionIdentityAuthority.sameFamily(npc.faction, Faction.HIVER);
        if (!local && (npc.factionRank < 7 || game.factionStanding.getOrDefault(npc.faction, 0) < 25))
            return new Quote(false, false, 0, occupied, limit, "Foreign faction hires require ordinary rank and standing of at least 25.");
        if (occupied >= limit) return new Quote(false, false, 0, occupied, limit,
                "Crew capacity reached. Complete more sleeping cots in your claimed room.");
        for (RecruitWorker worker : game.factionRecruits) {
            if (worker != null && worker.name != null && worker.name.equalsIgnoreCase(npc.name))
                return new Quote(false, false, 0, occupied, limit,
                        "A worker with this name is already on the roster; station assignments use names.");
        }
        int signingCost = 3 + RecruitWorker.fromNpc(npc, occupied + 1).skill;
        if (game.carriedScript < signingCost) return new Quote(false, false, signingCost, occupied, limit,
                "Need " + signingCost + " script signing pay; carrying " + game.carriedScript + ".");
        return new Quote(true, false, signingCost, occupied, limit,
                "Hire " + npc.name + " for " + signingCost + " script signing pay. Assign through a built machine's Worker control.");
    }

    static Result toggle(GamePanel game, NpcEntity npc) {
        Quote q = quote(game, npc);
        if (!q.ready()) return new Result(false, false, "Recruitment blocked: " + q.displayLine());
        if (q.hired()) {
            RecruitWorker worker = recruited(game, npc);
            if (worker == null) return new Result(false, false, "Dismissal blocked: roster changed; select the employee again.");
            ManualStaffingAssignmentAuthority.clearWorkerFromOtherStations(game, worker.name, null);
            game.factionRecruits.remove(worker);
            return new Result(true, false, "Dismissed " + worker.name
                    + ". Assigned stations are now unstaffed; queued production orders remain pending.");
        }
        if (!game.spendImperialScript(q.signingCost()))
            return new Result(false, false, "Recruitment blocked: insufficient script; no roster change.");
        RecruitWorker employee = RecruitWorker.fromNpc(npc, game.factionRecruits.size() + 1);
        game.factionRecruits.add(employee);
        return new Result(true, true, "Hired " + employee.name + " (" + employee.role + ") for "
                + q.signingCost() + " script. Crew " + game.factionRecruits.size()
                + "/" + capacity(game) + ". Open a built machine's Worker control to assign a job.");
    }


    /**
     * A worker may still occupy a roster slot after death, departure, or an
     * unloaded/missing actor record. Missing does NOT prove death: release is
     * an explicit administrative contract closure, never an NPC-state edit.
     */
    record RecoveryTarget(RecruitWorker worker, String cause, String key) {
        String displayName() {
            return worker.name == null || worker.name.isBlank() ? "Unnamed worker" : worker.name;
        }
    }

    static RecoveryTarget unavailableWorker(GamePanel game) {
        if (game == null || game.world == null || game.factionRecruits == null) return null;
        for (RecruitWorker worker : game.factionRecruits) {
            if (worker == null || worker.name == null || worker.name.isBlank()) continue;
            NpcEntity actor = null;
            if (worker.sourceNpcId != null && !worker.sourceNpcId.isBlank()) {
                for (NpcEntity npc : game.world.npcs) {
                    if (npc != null && worker.sourceNpcId.equals(npc.id)) {
                        actor = npc;
                        break;
                    }
                }
            } else {
                // Older roster records have only names. An ambiguous matching
                // live name is protected; it cannot prove which NPC was hired.
                for (NpcEntity npc : game.world.npcs) {
                    if (npc != null && worker.name.equalsIgnoreCase(npc.name)) {
                        actor = npc;
                        if (npc.hp > 0 && !"Left Faction".equalsIgnoreCase(npc.state))
                            break;
                    }
                }
            }
            String cause;
            if (actor == null) cause = "missing from this zone (may be alive elsewhere)";
            else if (actor.hp <= 0) cause = "deceased";
            else if ("Left Faction".equalsIgnoreCase(actor.state)) cause = "departed";
            else continue; // Healthy, present recruits cannot be released remotely.
            String key = SaveEfficiencyAuthority.playerIdFor(game) + "|" + game.seed
                    + "|" + game.claimedRoomId + "|" + worker.name
                    + "|" + (worker.sourceNpcId == null ? "" : worker.sourceNpcId) + "|" + cause;
            return new RecoveryTarget(worker, cause, key);
        }
        return null;
    }

    static String recoverySummary(GamePanel game) {
        RecoveryTarget target = unavailableWorker(game);
        if (target == null) return "Crew records: no unavailable hires need review.";
        return "Crew attention: " + target.displayName() + " is " + target.cause()
                + ". Use the Character roster to review this contract from your claimed room.";
    }

    /** Two intentional clicks in the same turn, physically inside the owned base. */
    static Result releaseUnavailableFromBase(GamePanel game) {
        if (game == null) return new Result(false, false, "Roster review unavailable: no active game.");
        if (!validBase(game) || !game.world.inBounds(game.playerX, game.playerY)
                || game.world.roomIdAt(game.playerX, game.playerY) != game.claimedRoomId) {
            game.pendingUnavailableWorkerKey = "";
            return new Result(false, false,
                    "Roster review blocked: stand inside your actively claimed freehold room.");
        }
        RecoveryTarget target = unavailableWorker(game);
        if (target == null) {
            game.pendingUnavailableWorkerKey = "";
            return new Result(false, false, "No deceased, departed, or missing work contracts need review.");
        }
        if (!target.key().equals(game.pendingUnavailableWorkerKey)
                || game.pendingUnavailableWorkerTurn != game.turn) {
            game.pendingUnavailableWorkerKey = target.key();
            game.pendingUnavailableWorkerTurn = game.turn;
            return new Result(false, false,
                    "Confirm release of " + target.displayName() + " (" + target.cause() + "). "
                    + "Click Confirm Release again this turn. Missing workers may still be alive elsewhere. "
                    + "Existing construction and production work will stay queued.");
        }
        // Re-read the exact live record on confirmation; never accept a stale
        // reference, a changed actor state, or an unrelated similarly named NPC.
        RecoveryTarget current = unavailableWorker(game);
        if (current == null || current.worker() != target.worker()
                || !current.key().equals(game.pendingUnavailableWorkerKey)) {
            game.pendingUnavailableWorkerKey = "";
            return new Result(false, false, "Roster changed; select the unavailable worker again.");
        }
        game.pendingUnavailableWorkerKey = "";
        ManualStaffingAssignmentAuthority.clearWorkerFromOtherStations(game, target.worker().name, null);
        game.factionRecruits.remove(target.worker());
        return new Result(true, false,
                "Released unavailable contract for " + target.displayName()
                + " (" + target.cause() + "). Vacant station/crew assignments were cleared, "
                + "but material reservations, labor progress and queued work remain intact. "
                + "Crew " + game.factionRecruits.size() + "/" + capacity(game) + ".");
    }

    private static boolean validBase(GamePanel game) {
        return game != null && game.world != null && game.baseClaimed
                && game.claimedRoomId >= 0 && game.claimedRoomId < game.world.roomFactions.size()
                && game.world.roomFaction(game.claimedRoomId) == Faction.HIVER
                && RoomOwnershipAuthority.hasAuthoritativePlayerClaim(game, game.claimedRoomId);
    }
}
