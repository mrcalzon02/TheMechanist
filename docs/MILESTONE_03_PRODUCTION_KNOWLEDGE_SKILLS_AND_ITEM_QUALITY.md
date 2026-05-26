# Milestone 03 - Production, Knowledge, Skills, and Item Quality

This ordered milestone document consolidates planning for knowledge-quality fabrication, machine and facility knowledge, blueprint and schematic quality, player/faction fabrication competence, item quality provenance, factional production mutations, the separate XP-based skill tree, trainers, specialists, and player-facing explanation of production quality.

`MASTER_DEVELOPMENT_PLAN.md` remains the authoritative roadmap, active checkpoint, phase list, and dependency map. This file is an ordered phase-aligned implementation target for the production/knowledge/skill/quality slice of that roadmap.

This document is not a changelog. Completed implementation still belongs in `DEVELOPMENT_HISTORY.md`. Durable implementation rules still belong in `STANDARDS_AND_PRACTICES.md`. High-level doctrine still belongs in `MASTER_GOVERNANCE_REVISION_II.md`.

## Source topical files absorbed or aligned

This milestone aligns and partially absorbs material from:

- `MILESTONE_KNOWLEDGE_SKILLS_MEDICAL_AND_FABRICATION_SYSTEMS.md`
- `MILESTONE_PARITY_BLUEPRINTS_AND_FACTION_MARKETS.md` where blueprint quality and acquisition affect construction or production capability
- `MILESTONE_02_UI_EXAMINATION_INFOPEDIA_AND_PLAYER_READABILITY.md` where production quality, skills, and knowledge need UI/Infopedia explanation
- `MILESTONE_04_POPULATION_PROVENANCE_ECONOMY_AND_FACTION_MARKETS.md` where workforce, provenance, markets, and vendor stock depend on produced item quality
- `MILESTONE_05_CONSTRUCTION_BLUEPRINTS_OWNERSHIP_AND_PLAYER_FACTION_PARITY.md` where blueprints, construction, and capability parity depend on knowledge and skill gates
- `MILESTONE_ROADMAP_REORGANIZATION_AND_PHASE_ALIGNMENT.md`

The older topical files should remain in place until their contents are fully absorbed into the ordered milestone sequence and the user explicitly approves archival, deprecation, or cleanup.

## Phase mapping

Primary phase groups:

- Phase 6 - Production, machines, recipes, staffing, and operation queues.
- Phase 9 - Item provenance, facility origin, and supply ecology.
- Phase 15 - Character creation, names, ranks, rosters, and social identity.
- Phase 17 - Economy, quests, contracts, faction reputation, trainers, specialists, and skill access.
- Phase 18 - Editor, localization, modding, Infopedia, and content pipeline.
- Phase 19 - Polish, packaging, QA, and release audit.

Secondary dependencies:

- Phase 1 - Authority extraction so production, knowledge, skills, and provenance do not regrow inside the central class.
- Phase 2 - Semantic asset IDs and publish-safe labels for produced goods, machines, tools, skill icons, and quality variants.
- Phase 4 - UI/inspection/readability for recipe availability, quality expectations, skill requirements, and production results.
- Phase 5 - Operation records, hauling, reservations, delivery intent, and interruption handling.
- Phase 7 - Ownership/access gates for restricted machines, blueprints, stolen plans, and faction facilities.
- Phase 8 - Workforce context for who operates machines and supplies skill/knowledge.
- Phase 11 - Faction schemes that expand production, acquire knowledge, sabotage facilities, or exploit quality advantages.
- Phase 12 - Construction knowledge and skill gates for room/facility building.
- Phase 14 - Medical/cybernetic/prosthetic production where item quality intersects body systems.

## Core doctrine

Knowledge quality must remain a first-class production input. The quality of what a character, faction, worker, machine, facility, blueprint, or doctrine knows about how to build a thing should directly affect the quality, reliability, safety, defect rate, efficiency, and factional variation of the resulting item wherever the item supports meaningful quality variation.

The Skill Tree must remain separate from, but interoperable with, the Knowledge Tree.

- The Knowledge Tree represents what the character, faction, machine, or facility knows.
- The Skill Tree represents trained capability, practice, specialization, bodily conditioning, social competence, technical competence, combat aptitude, medical competence, animal handling, investigation, fabrication craft, and other durable character abilities.

Knowledge can unlock or improve access to recipes and concepts. Skill should determine how well a character performs actions in the world and what interaction capabilities they can reliably execute.

## Dependency rule

Knowledge-quality fabrication depends on stable recipe, blueprint, machine-knowledge, facility-knowledge, skill, item-provenance, workforce, material-quality, and factional mutation definitions.

Skill-tree progression must not duplicate the Knowledge Tree. It may depend on Knowledge Tree nodes, trainers, faction access, tools, facilities, books, blueprints, quests, or field experience, but it should produce durable character capabilities and visible interaction changes rather than only hidden numeric modifiers.

Production quality must be explainable. If two production attempts create different results, the game should be able to explain the difference through knowledge source, operator skill, material quality, machine quality, facility quality, blueprint quality, maintenance state, faction doctrine, time pressure, environment, or provenance conditions.

## Phase 6 - Knowledge-quality fabrication and machine operation

### Phase 6.1 - Knowledge-quality fabrication pass

Production recipes, machine operations, construction operations, repairs, and manual crafting should consume knowledge quality as an input when output quality can vary meaningfully.

The system should identify who or what supplied the production knowledge:

- Player knowledge.
- Worker knowledge.
- Faction doctrine.
- Facility record.
- Machine database.
- Blueprint.
- Recipe book.
- Schematic.
- Stolen plan.
- Salvaged plan.
- Reverse-engineered sample.
- Oral training.
- Specialist supervision.
- Corrupted archive.
- Improvised field notes.

Knowledge quality may affect:

- Output item quality.
- Output durability.
- Output reliability.
- Output safety.
- Crafting time.
- Resource efficiency.
- Waste generation.
- Mechanical detritus or failed-output risk where relevant.
- Defect chance.
- Faction-style mutation chance.
- Required supervision.
- Required specialist skill.
- Machine wear or maintenance impact.
- Ability to produce advanced variants.
- Ability to repair, modify, reverse engineer, or inspect the item later.

Exit criteria:

The game can trace quality outcomes to knowledge inputs instead of treating recipes as flat unlock flags.

### Phase 6.2 - Machine and facility knowledge integration pass

Machines and facilities should be able to store or reference production knowledge separate from the character.

A player may own a room but lack the knowledge to operate it safely. A worker, machine, facility, faction license, blueprint, or specialist may supply knowledge the player lacks.

Support records for:

- Machine knowledge.
- Facility knowledge.
- Blueprint-provided knowledge.
- Worker-provided knowledge.
- Faction doctrine knowledge.
- Player-learned knowledge.
- Skill modifiers to output quality.
- Skill gates for operations.
- Knowledge decay, corruption, or obsolescence where future systems support it.
- Inspection output explaining recipe availability, degradation, unsafe operation, locked status, or likely poor quality.

Exit criteria:

Recipe availability and output quality can be traced to both knowledge and skill rather than one flat unlock flag.

### Phase 6.3 - Skill, tool, material, and machine quality stack pass

Production quality should account for the practical production stack.

Quality inputs may include:

- Operator skill.
- Supervisor skill.
- Machine quality.
- Tool quality.
- Facility quality.
- Input material quality.
- Blueprint/schematic quality.
- Knowledge quality.
- Maintenance state.
- Staffing state.
- Utility stability.
- Time pressure.
- Environmental hazards.
- Whether the work is manual, staffed, automated, improvised, supervised, or faction-standard.

The game should avoid opaque formulas in normal UI, but inspection should explain the main limiting factors in player-facing terms.

Example explanations:

- `Output quality limited by crude blueprint.`
- `Machine is worn; defects more likely.`
- `Operator lacks advanced fabrication skill.`
- `Good materials offset some tool limitations.`
- `Faction doctrine produces a durable but heavy variant.`

Exit criteria:

The player can understand why a production run is likely to be crude, standard, refined, dangerous, wasteful, or high-quality.

### Phase 6.4 - Quality bands and variant taxonomy pass

The production system should support explicit quality bands and variant tags where content requires them.

Candidate quality bands and variant states:

- Ruined.
- Crude.
- Poor.
- Improvised.
- Standard.
- Good.
- Refined.
- High-grade.
- Masterwork.
- Industrial-grade.
- Military-grade.
- Medical-grade.
- Noble-certified.
- Black-market.
- Counterfeit.
- Stolen.
- Faction-specific.
- Experimental.
- Unstable.
- Corrupted.
- Contaminated.
- Damaged.
- Pristine.

Not every item needs every band. The item definition should specify which quality dimensions matter.

Exit criteria:

Quality naming is consistent enough for production, Infopedia, vendors, examination, repair, and provenance systems to share.

### Phase 6.5 - Production inspection and forecast pass

Before production begins, the player should be able to inspect likely output and limiting factors where the character has enough knowledge.

Inspection may show:

- Known recipe.
- Unknown recipe gaps.
- Required materials.
- Required tools.
- Required machines.
- Required utilities.
- Required worker or manual time.
- Skill requirement.
- Knowledge requirement.
- Expected quality band.
- Main quality limiter.
- Known defect risks.
- Known waste outputs.
- Faction variant effects.
- Whether a better blueprint, machine, worker, or material would improve results.

Exit criteria:

The player is not forced to discover obvious production quality failures only after wasting all materials.

## Phase 9 - Quality provenance and factional production mutation

### Phase 9.1 - Quality provenance ledger pass

When an item is produced, the provenance ledger should preserve enough context to explain quality and later interactions.

Relevant provenance may include:

- Producing faction.
- Producing facility.
- Producing room.
- Producing machine.
- Operator or workforce group.
- Supervisor.
- Knowledge source.
- Blueprint source.
- Input material quality.
- Tool quality.
- Production time pressure.
- Batch identity.
- Output quality.
- Defects.
- Factional mutation tags.
- Legal/restricted/stolen/counterfeit status.
- Repair or modification history.

Exit criteria:

An item can be inspected or referenced later as something made by someone, somewhere, under specific conditions.

### Phase 9.2 - Factional production mutation pass

Factional production should be able to change item traits when the faction's doctrine, culture, material access, machine preference, or production style supports it.

Factional mutation examples:

- Visual style.
- Durability tendencies.
- Reliability tendencies.
- Weight or material substitutions.
- Maintenance quirks.
- Compatibility quirks.
- Ritual, legal, industrial, corporate, black-market, counterfeit, military, noble, or improvised traits.
- Known defects.
- Known bonuses.
- Hidden risks discoverable through examination, use, repair, or Infopedia knowledge.

A faction variant should not be random flavor only. It should matter through stats, requirements, maintenance, value, legality, reputation, compatibility, or inspection.

Exit criteria:

The game can distinguish not only what an item is, but how it was made, by whom, under what doctrine, and with what consequences.

### Phase 9.3 - Batch, defect, counterfeit, and contamination pass

Quality provenance should support batch-level issues.

Batch states may include:

- Good batch.
- Mixed batch.
- Defective batch.
- Counterfeit batch.
- Contaminated batch.
- Diluted batch.
- Unstable batch.
- Overbuilt batch.
- Underbuilt batch.
- Stolen batch.
- Recalled batch.
- Faction-certified batch.

This is especially important for medicines, narcotics, ammunition, machine parts, cybernetics, prosthetics, military goods, luxury goods, and draught or noble-certified goods.

Exit criteria:

Contracts, vendors, recalls, investigations, and faction schemes can reference the quality and origin of specific batches.

## Phase 15 - Skill tree and character capability progression

### Phase 15.1 - Dedicated XP-based skill tree pass

Create a skill-tree system that uses accumulated experience points to unlock durable, meaningful in-world interaction capabilities.

The skill tree should include:

- Branching paths.
- Interactive dependencies.
- Experience-point costs.
- Prerequisite skills.
- Stat prerequisites where appropriate.
- Knowledge prerequisites where appropriate.
- Faction, trainer, equipment, or facility prerequisites where appropriate.
- Mutually exclusive specializations where appropriate.
- Durable character capabilities.
- New or improved interaction options.
- Passive bonuses.
- Active abilities.
- Stat increases on selected nodes or stages.

Exit criteria:

The player can spend experience points into branching skill paths that produce durable character changes and meaningful world interaction capabilities.

### Phase 15.2 - Skill versus Knowledge separation pass

The Skill Tree and Knowledge Tree must remain distinct.

Knowledge examples:

- Knows a recipe.
- Understands a machine principle.
- Has studied a faction doctrine.
- Recognizes a medical condition.
- Knows how a vehicle component is supposed to work.
- Knows a blueprint or schematic.

Skill examples:

- Can fabricate precisely.
- Can operate machines safely.
- Can perform surgery.
- Can negotiate effectively.
- Can examine a target accurately.
- Can drive under stress.
- Can repair a damaged machine.
- Can handle animals safely.
- Can lead workers.

The two systems may interact. Knowledge may unlock a skill path; skill may improve use of knowledge; trainers may require knowledge prerequisites; advanced recipes may require both.

Exit criteria:

The player does not experience knowledge and skill as two duplicate unlock menus doing the same thing.

### Phase 15.3 - Skill branch candidate pass

Initial skill branch candidates include:

- Fabrication and repair.
- Machine operation.
- Construction and architecture.
- Medicine and surgery.
- Cybernetics and prosthetics.
- Combat and tactics.
- Firearms and heavy weapons.
- Melee and close defense.
- Investigation and examination.
- Social influence and negotiation.
- Trade and appraisal.
- Stealth and infiltration.
- Leadership and faction command.
- Logistics and hauling.
- Animal handling and pet care.
- Vehicles and driving.
- Scavenging and salvage.
- Survival and endurance.
- Electronics, machine systems, or setting-specific technical practice.

Each branch should identify what it lets the character do in the world, not merely what number it increases.

Exit criteria:

Skill branches can be evaluated by player-facing capabilities rather than just abstract stat lines.

### Phase 15.4 - Skill node capability pass

Skill nodes should not be empty numeric bonuses unless the game explicitly benefits from that.

A good skill node should do one or more of:

- Add a new interaction.
- Improve a visible operation.
- Unlock a new tool use.
- Unlock a new treatment.
- Unlock a new construction method.
- Unlock a new examination tier.
- Unlock a new negotiation option.
- Reduce risk or waste in a readable way.
- Improve output quality.
- Add a stat point or derived stat improvement.
- Add a passive capability that affects repeated world interactions.

Exit criteria:

Skill progression feels like gaining capabilities, not merely adjusting invisible arithmetic.

## Phase 17 - Trainers, specialists, world access, and economy tie-ins

### Phase 17.1 - Trainers and specialist access pass

Some skill branches or advanced stages should require trainers, specialists, institutions, faction access, facilities, books, blueprints, practice, quests, or field experience.

Examples:

- Surgery training from a clinic, doctor, or medical faction.
- Cybernetics training from a specialist or machine facility.
- Vehicle training from a driver, garage, or faction motor pool.
- Heavy weapon training from a military or security faction.
- Advanced fabrication from an industrial faction or master workshop.
- Animal handling from handlers, pet vendors, or agricultural factions.
- Investigation/examination training from inspectors, security offices, or field experience.
- Trade appraisal from merchants, brokers, or noble representatives.
- Leadership training from faction command roles or field command experience.

Exit criteria:

Skill progression feels embedded in the world rather than detached from the setting.

### Phase 17.2 - Skill, quality, and market value bridge pass

Skill and knowledge quality should influence market outcomes where appropriate.

Examples:

- Better fabrication skill produces higher-value goods.
- Appraisal skill improves detection of counterfeit or bad batches.
- Medical skill improves treatment outcomes and medicine use.
- Trade skill improves negotiation or access to better offers.
- Investigation skill improves examination reliability.
- Animal handling improves pet care and animal interactions.
- Machine skill reduces production risk.

Exit criteria:

Skills affect the economy and world interactions in ways the player can see.

### Phase 17.3 - Production contracts and skill proof pass

Contracts should be able to require or reward production skill and knowledge.

Examples:

- Produce a high-quality component.
- Repair a damaged machine.
- Identify a counterfeit batch.
- Reverse engineer a stolen item.
- Train under a specialist.
- Prove medical competence.
- Build to faction standards.
- Improve a faction production chain.

Exit criteria:

Production and skill systems participate in contracts rather than remaining isolated menus.

## Phase 18 - Editor, audit, Infopedia, and data ownership

### Phase 18.1 - Knowledge, skill, quality editor/audit pass

Future data/editor/audit surfaces should define and inspect:

- Knowledge quality bands.
- Recipe knowledge sources.
- Machine knowledge records.
- Facility knowledge records.
- Blueprint quality.
- Schematic quality.
- Skill-tree branches.
- Skill nodes.
- XP costs.
- Dependencies.
- Stat modifiers.
- New capabilities.
- Trainer requirements.
- Specialist requirements.
- Item quality outcomes.
- Factional production mutations.
- Quality provenance ledgers.
- Batch states.
- Counterfeit, contamination, defect, and recall flags.

Editor/audit surfaces may expose semantic IDs where explicitly needed for development, but ordinary player UI must hide raw IDs.

Exit criteria:

Knowledge quality, skill trees, quality fabrication, and factional mutations can be audited through owned data surfaces rather than hardcoded central-class sprawl.

### Phase 18.2 - Infopedia knowledge, skill, and quality reference pass

The Infopedia should explain knowledge quality, production quality, skill trees, blueprints, machine knowledge, facility knowledge, and item quality in player-facing language.

Entries should answer:

- What knowledge quality means.
- How knowledge affects output quality.
- How skills differ from knowledge.
- What a skill node unlocks.
- What stats or capabilities a skill adds.
- What a quality band means.
- What a factional variant means.
- What a counterfeit, contaminated, defective, or unstable batch means.
- Where an item is made.
- What it is made from.
- What it can be used to make.
- What machine, facility, trainer, vendor, faction, or blueprint is involved.

Exit criteria:

The player can look up how knowledge, skills, production quality, and item variants work without relying on external notes.

## Phase 19 - Release audit

Before release claims for this milestone, verify:

- Production quality can consume knowledge quality where relevant.
- Machine knowledge and player/faction knowledge are distinguishable.
- Facility knowledge can affect recipe access or quality where implemented.
- Skill-tree progression exists separately from Knowledge Tree progression.
- Skill nodes have meaningful capabilities, visible effects, or justified stat improvements.
- Skill dependencies are readable.
- Knowledge quality, skill, input quality, machine quality, and facility quality can all be represented without raw ID leakage.
- Item provenance can explain quality and production origin where relevant.
- Factional production mutations are recorded where relevant.
- Batch defects, counterfeit status, contamination, or quality differences can be represented where relevant.
- Production inspection gives useful quality expectations where implemented.
- Infopedia entries explain exposed mechanics.
- Player-facing text avoids raw IDs and placeholder labels.
- Editor/audit surfaces can inspect key definitions.

Exit criteria:

The game may claim knowledge-quality fabrication, durable skill progression, production quality depth, and factional production variation only after these systems are inspectable, player-facing, and connected to production, provenance, workforce, blueprints, and character progression.

## Non-goals for this milestone

This milestone does not require every item to support every quality band. It requires consistent quality rules where quality matters.

This milestone does not replace the Knowledge Tree. It defines how a separate Skill Tree should coexist with it.

This milestone does not complete medical, prosthetic, cybernetic, narcotic, or vehicle systems. It defines how production quality, skills, and knowledge should interface with those systems when their ordered milestones are implemented.

This milestone does not require full per-item provenance for every trivial item. It requires provenance when quality, legality, faction identity, contracts, inspection, repair, resale, or player decision-making depends on it.

## Deferred checkpoint summary

The project must preserve a durable link between knowledge quality and production quality. The quality of knowledge available to the builder, worker, machine, facility, faction, or blueprint should affect the quality, reliability, safety, and factional variation of produced items. Machine knowledge, fabrication knowledge, production skill, factional production mutation, item provenance, and quality outcomes should remain integrated.

A separate skill-tree system should use accumulated experience points to unlock durable character capabilities. It should have branching paths, dependencies, capability unlocks, interaction changes, and selected stat increases. It should work alongside the Knowledge Tree without replacing it.
