# Milestone - Knowledge Quality, Skill Trees, Medical Systems, Cybernetics, and Fabrication Depth

This milestone document is a controlled phase-group expansion for the master development plan. It exists because knowledge-quality fabrication, machine knowledge, player skill-tree progression, medical systems, prosthetics, cybernetics, narcotics, and medical item depth all cross multiple roadmap phases and should not be reduced to a few loose notes inside `MASTER_DEVELOPMENT_PLAN.md`.

`MASTER_DEVELOPMENT_PLAN.md` remains the authoritative roadmap, active checkpoint, phase list, and dependency map. This milestone carries the detailed planning surface for knowledge-driven quality, durable skill progression, medical treatment, cybernetic replacement, prosthetic construction, narcotic/medicine item systems, and their ties into production, faction identity, and character development.

This document is not a changelog. Completed implementation still belongs in `DEVELOPMENT_HISTORY.md`. Durable coding or packaging rules still belong in `STANDARDS_AND_PRACTICES.md`. High-level doctrine still belongs in `MASTER_GOVERNANCE_REVISION_II.md`.

## Scope

This milestone covers:

- Knowledge-quality fabrication, where the quality of what the character or faction knows directly affects the quality of what can be built.
- Integration between knowledge records, machine knowledge, fabrication skills, item provenance, and production outcomes.
- Factional mutation and faction-style production variation systems already developed or cataloged elsewhere in the project.
- A player skill-tree system driven by accumulated experience points.
- Branching skill paths with dependencies and durable in-world capabilities.
- Skill-tree nodes that add character abilities, improve interaction options, or modify character stats.
- Medical system planning.
- Prosthetic and cybernetic replacement systems.
- Narcotics, medicines, drugs, stimulants, sedatives, painkillers, addiction-risk items, and other medical goods.
- Editor/audit and release-readiness requirements for all of the above.

This milestone maps back to master-plan phases:

- Phase 1 - Framework stabilization and code dissection.
- Phase 6 - Production, machines, recipes, staffing, and operation queues.
- Phase 8 - Population provenance, workforce context, and demographic continuity.
- Phase 9 - Item provenance, facility origin, and supply ecology.
- Phase 14 - Combat, health, unconsciousness, death, saves, and feedback.
- Phase 15 - Character creation, names, ranks, rosters, and social identity.
- Phase 17 - Economy, quests, contracts, faction reputation, pets, and ownership.
- Phase 18 - Editor, localization, modding, and content pipeline.
- Phase 19 - Polish, packaging, QA, and endgame readiness.

## Dependency rule

Knowledge quality must be preserved as a first-class production input. The quality of knowledge available to the builder, machine operator, faction, facility, or production chain should directly affect the quality, reliability, defect rate, efficiency, and variant behavior of the resulting item where the recipe supports quality variance.

Skill-tree progression must not duplicate or replace the Knowledge Tree. The Knowledge Tree represents what the character or faction knows about the world, machines, recipes, systems, and doctrines. The Skill Tree represents trained capability, practice, specialization, bodily conditioning, social capacity, technical competence, combat aptitude, medical competence, animal handling, investigation, fabrication craft, and other durable character abilities. The two systems should interact, but they are not the same system.

Medical, prosthetic, cybernetic, narcotic, and pharmaceutical systems depend on health/body-state modeling, item provenance, recipe/facility support, and player-facing inspection. They must not become a hidden status-effect dump with no readable treatment path.

## Knowledge quality and fabrication doctrine

The game should maintain a durable relationship between knowledge quality and production quality.

A character, worker, faction, machine, or facility may know how to produce an item at different levels of understanding. Better knowledge should improve the chance of better output and reduce the chance of defects, waste, breakdowns, unsafe variants, or factionally mutated oddities.

Knowledge quality may affect:

- Output item quality.
- Output item durability.
- Output item reliability.
- Output item safety.
- Crafting time.
- Resource efficiency.
- Waste or detritus generation.
- Defect chance.
- Faction-style mutation chance.
- Required supervision.
- Required specialist skill.
- Machine wear or sanctity/maintenance impact where relevant.
- Ability to produce advanced variants.
- Ability to repair, modify, reverse engineer, or inspect the item later.

Knowledge source should matter. A recipe learned from a faction blueprint, a black-market note, a salvaged schematic, a formal school, a corrupted archive, a field improvisation, a machine spirit/machine record, a faction doctrine, or a reverse-engineered item should not necessarily produce identical results.

## Phase 6 addition - Knowledge-quality fabrication pass

Add a Phase 6 child pass for knowledge-quality fabrication.

Required outcome:

Production recipes, machine operations, and manual crafting should consume knowledge quality as an input where the output has meaningful quality variation. The system should identify who or what supplied the production knowledge: player, worker, faction, facility, machine database, blueprint, recipe book, schematic, stolen plan, reverse-engineered sample, or inherited faction doctrine.

Production should be able to produce different outcomes based on:

- Character knowledge quality.
- Worker skill.
- Machine quality.
- Tool quality.
- Facility quality.
- Input material quality.
- Blueprint/schematic quality.
- Faction mutation rules.
- Environmental conditions.
- Time pressure.
- Maintenance state.
- Whether the work is manual, staffed, automated, improvised, or supervised.

The system should support explicit quality bands such as crude, poor, standard, good, refined, high-grade, masterwork, military-grade, industrial-grade, faction-specific, unstable, corrupted, counterfeit, improvised, damaged, or pristine where the content model requires them.

Exit criteria:

The game can explain why two production attempts for the same nominal item produced different quality results and can trace that difference to knowledge, skill, inputs, facility, machine, faction, or provenance conditions.

## Phase 6 addition - Machine knowledge and fabrication skill integration pass

Add a Phase 6 child pass for machine knowledge and fabrication skills.

Required outcome:

Machines and facilities should be able to store or reference production knowledge separate from the character. A player who owns the room but lacks the correct knowledge may still be unable to safely produce an item unless the machine, worker, faction license, blueprint, or specialist supplies the missing knowledge.

This pass should support:

- Machine knowledge records.
- Facility knowledge records.
- Blueprint-provided knowledge.
- Worker-provided knowledge.
- Faction doctrine knowledge.
- Player-learned knowledge.
- Skill modifiers to output quality.
- Skill gates for certain operations.
- Skill-based mitigation of bad input materials or damaged machines.
- Inspection surfaces showing why a recipe is available, degraded, unsafe, locked, or likely to produce poor quality.

Exit criteria:

Recipe availability and output quality can be traced to both knowledge and skill rather than one flat unlock flag.

## Phase 9 addition - Quality provenance and factional production mutation pass

Add a Phase 9 child pass for quality provenance and factional mutation.

Required outcome:

Item provenance should preserve enough context to explain quality. When an item is produced, the ledger should be able to record the faction style, production facility, machine, operator, knowledge source, blueprint source, input quality, and any factional mutation or production variation that matters later.

Factional mutations may include:

- Visual style.
- Durability tendencies.
- Reliability tendencies.
- Weight or material substitutions.
- Maintenance quirks.
- Compatibility quirks.
- Ritual, legal, industrial, corporate, black-market, counterfeit, military, or improvised traits.
- Known defects.
- Known bonuses.
- Hidden risks that can be revealed through examination, use, repair, or Infopedia/inspection knowledge.

Exit criteria:

The game can distinguish not only what an item is, but how well it was made, who made it, what knowledge produced it, and whether factional production tendencies changed it.

## Phase 15 addition - Skill tree system pass

Add a Phase 15 child pass for a dedicated skill-tree system.

Required outcome:

Create a skill-tree system that uses player accumulated experience points to unlock durable, meaningful in-world interaction capabilities. The skill tree should be separate from the Knowledge Tree but able to interact with it.

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
- Stat increases on certain stages or nodes.
- Unlocks that affect crafting, medicine, combat, social interaction, investigation, animal handling, vehicles, machines, construction, stealth, trade, leadership, and survival.

Skill nodes must not be empty numeric bonuses unless the game explicitly benefits from that. A good skill node should either add a new player capability, unlock a new interaction, improve a visible operation, or meaningfully alter success/failure/quality/readability.

Exit criteria:

The player can spend experience points into branching skill paths that produce durable character changes and meaningful world interaction capabilities.

## Skill tree branch candidates

Initial branch candidates include:

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
- Technical electronics or equivalent setting-specific machine systems.

Each branch should identify what it lets the character do in the world, not merely what number it increases.

## Phase 14 addition - Medical systems deep pass

Add a Phase 14 child pass for medical systems.

Required outcome:

Spend a dedicated development block on medical systems. The game should move beyond generic healing and support readable injuries, treatment paths, medical items, clinics, surgery, field treatment, pain, infection, bleeding, unconsciousness, long-term recovery, and consequences.

Medical systems should support:

- Wound types.
- Bleeding.
- Pain.
- Infection.
- Shock.
- Broken bones or structural injuries where supported.
- Organ or body-part damage where supported.
- Burns, toxins, radiation, disease, chemical exposure, and environmental injury where supported.
- Field treatment.
- Clinic treatment.
- Surgery.
- Recovery time.
- Medical skill impact.
- Medical item quality impact.
- Facility quality impact.
- Treatment risk and failure.
- Treatment inspection and patient state readability.

Exit criteria:

Injury, treatment, recovery, and failure are readable systems rather than a single hidden health refill.

## Phase 14 addition - Prosthetics and cybernetics pass

Add a Phase 14 child pass for prosthetics and cybernetics.

Required outcome:

Develop prosthetic and cybernetic systems tied to body plans, medical treatment, item provenance, crafting quality, surgery, and character capability.

The system should support:

- Lost or damaged body parts where body-plan simulation allows.
- Basic prosthetics.
- Advanced prosthetics.
- Cybernetic replacements.
- Crude, standard, refined, military, industrial, factional, black-market, or experimental variants.
- Installation requirements.
- Surgery or specialist requirements.
- Maintenance requirements.
- Power or calibration requirements where appropriate.
- Benefits.
- Drawbacks.
- Compatibility and rejection risks where appropriate.
- Quality impact from knowledge, skill, facility, and item provenance.
- Social, legal, factional, medical, or heat/suspicion effects where appropriate.

Exit criteria:

Prosthetics and cybernetics are not just equipment icons. They connect to injury, surgery, body plans, fabrication quality, knowledge quality, character capabilities, maintenance, and faction/economy systems.

## Phase 9 and Phase 17 addition - Narcotics, medicines, and medical item economy pass

Add a Phase 9/17 child pass for narcotics, medicines, and medical goods.

Required outcome:

Develop a broad medical and narcotic item system with item provenance, effects, risks, legality, vendors, clinics, black markets, and faction-specific supply chains.

Item categories may include:

- Bandages and dressings.
- Splints and braces.
- Antiseptics.
- Antibiotics.
- Painkillers.
- Stimulants.
- Sedatives.
- Anesthetics.
- Anti-toxins.
- Anti-radiation drugs where setting supports it.
- Blood substitutes or transfusion supplies where setting supports it.
- Surgical kits.
- Cybernetic installation supplies.
- Addiction-risk narcotics.
- Recreational narcotics.
- Performance drugs.
- Black-market medical goods.
- Counterfeit medicine.
- Faction-specific pharmaceuticals.

Medical and narcotic items should define:

- Intended use.
- Effect strength.
- Duration.
- Side effects.
- Addiction or dependency risk where appropriate.
- Overdose or misuse risk where appropriate.
- Legal status.
- Vendor/faction availability.
- Production origin.
- Required ingredients or facility.
- Quality variation.
- Interaction with medical skill and treatment context.

Exit criteria:

Medical items and narcotics become coherent gameplay objects with effects, risks, supply paths, legality, and quality/provenance rather than generic consumables.

## Phase 17 addition - Trainers, specialists, and skill access pass

Add a Phase 17 child pass for skill access through trainers and specialists.

Required outcome:

The skill tree may use XP as its core currency, but some branches or advanced stages should require trainers, specialists, institutions, faction access, facilities, books, blueprints, practice, quests, or field experience.

Examples:

- Surgery training from a clinic, doctor, or medical faction.
- Cybernetics training from a specialist or machine facility.
- Vehicle training from a driver, garage, or faction motor pool.
- Heavy weapon training from a military/security faction.
- Advanced fabrication from an industrial faction or master workshop.
- Animal handling from handlers, pet vendors, or agricultural factions.
- Investigation/examination training from inspectors, security offices, or field experience.

Exit criteria:

Skill progression feels embedded in the world rather than detached from the setting.

## Phase 18 addition - Knowledge, skill, medical, and quality editor/audit pass

Add a Phase 18 child pass for editor and audit support.

Required outcome:

Future data/editor/audit surfaces should define and inspect:

- Knowledge quality bands.
- Recipe knowledge sources.
- Machine knowledge records.
- Facility knowledge records.
- Blueprint quality.
- Skill-tree branches.
- Skill nodes.
- XP costs.
- Dependencies.
- Stat modifiers.
- New capabilities.
- Medical injuries.
- Treatments.
- Prosthetics.
- Cybernetics.
- Narcotics and medical items.
- Item quality outcomes.
- Factional production mutations.
- Quality provenance ledgers.
- Trainer and specialist requirements.

Editor/audit surfaces may expose semantic IDs where explicitly needed for development, but ordinary player UI must hide raw IDs.

Exit criteria:

Knowledge quality, skill trees, medical systems, cybernetics, prosthetics, narcotics, and quality fabrication can be audited through owned data surfaces rather than hardcoded central-class sprawl.

## Phase 18 addition - Infopedia knowledge, skill, medical, and quality reference pass

Add a Phase 18 child pass connecting this milestone to Infopedia depth.

Required outcome:

The Infopedia should explain knowledge quality, production quality, skill trees, medical treatment, prosthetics, cybernetics, narcotics, and medical items in player-facing language.

Infopedia entries should answer:

- What knowledge quality means.
- How knowledge affects output quality.
- How skills differ from knowledge.
- What a skill node unlocks.
- What a medical condition is.
- How a condition is treated.
- What a medical item does.
- What side effects or risks exist.
- What a prosthetic or cybernetic part does.
- What installation requires.
- What quality levels exist.
- Where the item or treatment is available.
- What faction, facility, trainer, vendor, blueprint, or machine is involved.

Exit criteria:

The player can look up how knowledge, skill, medicine, cybernetics, prosthetics, and narcotics work without relying on external notes.

## Phase 19 addition - Knowledge, skill, medical, and fabrication release audit

Add a Phase 19 child pass for release readiness.

Required audit checks:

- Production quality can consume knowledge quality where relevant.
- Machine knowledge and player/faction knowledge are distinguishable.
- Skill-tree progression exists separately from Knowledge Tree progression.
- Skill nodes have meaningful capabilities or visible effects.
- Skill nodes can add stats where appropriate.
- Skill dependencies are readable.
- Medical conditions are inspectable and treatable.
- Medical items have clear effects, risks, quality, and source.
- Prosthetics and cybernetics connect to body plans, surgery, and item quality.
- Narcotics have effects, risks, legality, and supply paths where present.
- Factional production mutations are recorded where relevant.
- Infopedia entries explain exposed mechanics.
- Player-facing text avoids raw IDs and placeholder labels.

Exit criteria:

The game may claim knowledge-quality fabrication, durable skill progression, medical depth, cybernetics, prosthetics, and narcotic/medical item systems only after the above systems are inspectable, player-facing, and connected to production, provenance, health, and character progression.

## Deferred checkpoint line - Knowledge quality, skill trees, medicine, prosthetics, and narcotics

The project must preserve a durable link between knowledge quality and production quality. The quality of knowledge available to the builder, worker, machine, facility, faction, or blueprint should affect the quality and reliability of the items produced. Machine knowledge, fabrication knowledge, production skill, factional production mutation, item provenance, and quality outcomes should remain integrated.

A separate skill-tree system should use accumulated experience points to unlock durable character capabilities. It should have branching paths, dependencies, capability unlocks, interaction changes, and selected stat increases. It should work alongside the Knowledge Tree without replacing it.

Medical systems, prosthetic/cybernetic replacement systems, narcotics, and medical items need a dedicated later development block. They should connect to body plans, injury, surgery, treatment, item quality, legality, vendors, factions, facilities, side effects, addiction or dependency risk where appropriate, and Infopedia reference entries.

## Master-plan index patch target

During the next safe `MASTER_DEVELOPMENT_PLAN.md` patch, add a brief milestone reference near `## Durable phase roadmap`:

```text
Detailed phase-group expansions may live in controlled milestone documents when the master plan becomes unsafe to edit monolithically. See `docs/MILESTONE_INDEX.md` for the active milestone map, `docs/MILESTONE_WORLD_CONTENT_AND_PET_SYSTEMS.md` for agriculture and pet systems, `docs/MILESTONE_EXAMINATION_AND_INFOPEDIA_SYSTEMS.md` for examination and Infopedia systems, `docs/MILESTONE_PARITY_BLUEPRINTS_AND_FACTION_MARKETS.md` for player/faction parity and faction markets, and `docs/MILESTONE_KNOWLEDGE_SKILLS_MEDICAL_AND_FABRICATION_SYSTEMS.md` for knowledge-quality fabrication, skill trees, medicine, prosthetics, cybernetics, and narcotics.
```

Also update the master plan dependency rule to include:

```text
knowledge-quality fabrication depends on stable recipe, blueprint, machine-knowledge, skill, item-provenance, and factional mutation definitions; skill-tree progression must remain separate from but interoperable with the Knowledge Tree; medical, prosthetic, cybernetic, narcotic, and pharmaceutical systems depend on body-state, treatment, item-quality, facility, vendor, and Infopedia definitions
```
