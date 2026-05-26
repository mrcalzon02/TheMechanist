# Milestone Supplement - Illicit Narcotics Faction Production Preference

This supplement attaches to `MILESTONE_KNOWLEDGE_SKILLS_MEDICAL_AND_FABRICATION_SYSTEMS.md`. It exists to preserve a focused faction-production rule without replacing the larger milestone document.

## Scope

Gang groups, criminal syndicates, smuggling rings, black-market clinics, illicit laboratories, corrupt corporate cutouts, and other unlawful organizations should have explicit preference logic for manufacturing narcotics in bulk when their faction identity, resources, rooms, recipes, labor, and supply routes support it.

This is not merely a vendor-stock rule. It is a faction economic behavior rule: illicit factions should want to manufacture narcotics for their own use, for internal morale/control, for income, for leverage over other groups, and for sale to outsiders.

## Design rule

Illicit factions should be able to treat narcotics as a preferred production chain.

A faction with the correct identity, rooms, skills, recipes, staff, and supply access may prioritize:

- Producing narcotics in bulk.
- Stockpiling narcotics for internal use.
- Issuing narcotics to faction members where faction doctrine supports it.
- Selling narcotics through black-market vendors.
- Selling narcotics to other factions or neutral buyers.
- Trading narcotics for weapons, food, medicine, protection, favors, information, or territory.
- Using narcotics as leverage, bribery, addiction pressure, recruitment bait, or social control.
- Protecting narcotics production rooms as high-value facilities.
- Expanding narcotics operations when profitable or strategically useful.
- Hiding narcotics operations from hostile factions, law enforcement, inspectors, or rivals.

## Phase mapping

This supplement maps back to master-plan phases:

- Phase 6 - Production, machines, recipes, staffing, and operation queues.
- Phase 9 - Item provenance, facility origin, and supply ecology.
- Phase 11 - Ages of Control, faction leadership schemes, and zone generation history.
- Phase 16 - World generation, districts, facilities, and strategic simulation.
- Phase 17 - Economy, contracts, faction reputation, vendors, and illicit markets.
- Phase 18 - Editor, localization, modding, and content pipeline.
- Phase 19 - Polish, packaging, QA, and release audit.

## Phase 6 addition - Illicit narcotics production chain pass

Production systems should support narcotics recipes as real production chains rather than arbitrary loot-table outputs.

Required outcome:

Illicit narcotics production should consume recipe knowledge, ingredients, tools, machines, rooms, labor, and risk. Production should support quality variation and bad-batch risk where appropriate.

Production inputs may include:

- Raw chemical precursors.
- Plants, fungi, organic samples, industrial chemicals, pharmaceuticals, solvents, stabilizers, binders, packaging, or stolen medical supplies.
- Lab equipment.
- Improvised equipment.
- Skilled or semi-skilled operators.
- Security or secrecy requirements.
- Faction knowledge or black-market recipes.

Exit criteria:

Narcotics can be produced through a traceable recipe/facility path rather than appearing only as generic contraband.

## Phase 9 addition - Narcotics provenance and faction origin pass

Item provenance should record narcotics origin, production quality, faction source, facility source, batch identity, and legality where relevant.

Required outcome:

A narcotic item or batch should be able to answer:

- Which faction produced it, if known.
- Which room, lab, clinic, workshop, greenhouse, farm, or hidden facility produced it, if known.
- Which recipe or knowledge source produced it.
- Whether it is medical, recreational, combat-performance, sedative, stimulant, counterfeit, contaminated, experimental, or faction-specific.
- Whether it is legal, restricted, illicit, black-market, stolen, counterfeit, or military-controlled.
- Whether the item is high quality, standard quality, crude, unstable, diluted, contaminated, or dangerous.

Exit criteria:

Narcotics have provenance and faction identity, not just effects.

## Phase 11 addition - Narcotics leadership scheme pass

Faction leadership schemes should be able to prioritize narcotics production where the faction is inclined toward it.

Potential schemes include:

- Establish hidden narcotics lab.
- Expand drug production.
- Protect narcotics supply chain.
- Smuggle narcotics through rival territory.
- Bribe officials or guards with narcotics profits.
- Flood a district with narcotics to destabilize it.
- Use narcotics to recruit, pacify, or control dependents.
- Sabotage rival narcotics production.
- Poison or contaminate a rival batch.
- Seize a greenhouse, clinic, chemical store, or lab for production.
- Build front businesses for distribution.

Exit criteria:

Illicit factions can treat narcotics as strategic production and influence tools, not only shop inventory.

## Phase 16 addition - Illicit narcotics facility generation pass

World generation should place narcotics-relevant rooms and front operations where faction history and zone context justify them.

Possible facilities include:

- Hidden drug labs.
- Back-room chemical workshops.
- Corrupt clinics.
- Front pharmacies.
- Greenhouses or mushroom rooms for controlled substances.
- Storage rooms and stash rooms.
- Smuggling depots.
- Distribution rooms.
- Guarded dealer rooms.
- Contaminated or burned-out labs as historical scars.

Exit criteria:

A gang or illicit faction with a narcotics economy can leave visible room/facility traces in the world.

## Phase 17 addition - Illicit narcotics market preference pass

Gang groups and illicit organizations should prefer to use and sell narcotics when it matches their faction identity.

Required outcome:

Their economy and vendors should support:

- Bulk narcotics inventory.
- Internal faction use or distribution.
- External sale to the player.
- Sale to other factions or neutral buyers.
- Variable prices based on scarcity, heat, risk, law enforcement pressure, quality, and faction control.
- Reputation-gated access to better stock or safer supply.
- Black-market contacts and hidden vendors.
- Contracts involving delivery, protection, theft, sabotage, production ingredients, courier work, debt collection, contaminated batches, or rival disruption.

The player should be able to encounter this as trade, contraband, contracts, inspection findings, faction schemes, law/reputation consequences, and world-state pressure.

Exit criteria:

Illicit factions have a visible reason to manufacture narcotics: use, income, leverage, trade, and territorial influence.

## Phase 18 addition - Illicit narcotics editor/audit pass

Editor and audit surfaces should inspect:

- Faction narcotics preference flags.
- Narcotics recipes.
- Narcotics production rooms.
- Legal/illegal classifications.
- Vendor stock rules.
- Black-market access rules.
- Bulk production thresholds.
- Internal-use rules.
- External-sale rules.
- Faction scheme hooks.
- Provenance fields.
- Quality, contamination, counterfeit, and bad-batch rules.

Exit criteria:

Narcotics production preferences are data-auditable and not hidden in scattered source logic.

## Phase 19 addition - Illicit narcotics release audit

Before release claims, verify:

- Gang/illicit faction identities can express narcotics-production preference.
- Narcotics are produced through real recipe/facility paths where implemented.
- Bulk production has stockpile and vendor consequences.
- Internal faction use is represented where appropriate.
- External sale/trade is represented where appropriate.
- Provenance can identify faction/facility/batch/quality when known.
- Black-market vendors and contracts do not expose raw IDs.
- Player-facing text distinguishes medicine, restricted medicine, narcotics, counterfeit goods, contaminated batches, and black-market goods where relevant.

Exit criteria:

The game may claim illicit narcotics economies only when gangs and criminal factions can actually prefer, produce, stockpile, use, sell, and defend narcotics production in a traceable way.

## Deferred checkpoint line - Illicit narcotics faction economies

Gang groups and illicit organizations should have a factional preference for manufacturing narcotics in bulk for internal use, faction control, profit, and sale to outsiders. Narcotics should connect to production rooms, recipes, item provenance, black-market vendors, faction schemes, smuggling, contracts, heat, law/reputation consequences, and faction economic behavior rather than appearing as disconnected loot.

## Index integration target

During the next safe `MILESTONE_INDEX.md` patch, list this supplement beneath `MILESTONE_KNOWLEDGE_SKILLS_MEDICAL_AND_FABRICATION_SYSTEMS.md` as a linked supplement for illicit narcotics faction-production preference.
