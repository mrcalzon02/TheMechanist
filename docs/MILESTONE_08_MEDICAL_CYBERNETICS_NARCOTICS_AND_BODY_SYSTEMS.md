# Milestone 08 - Medical, Cybernetics, Narcotics, and Body Systems

This ordered milestone document consolidates planning for body-state simulation, medical treatment, wounds, pain, infection, shock, drugs, narcotics as character effects, medicine quality, surgery, prosthetics, cybernetics, cybernetic installation, medical facilities, medical skills, addiction or dependency risk where appropriate, medical item provenance, and player-facing health inspection.

`MASTER_DEVELOPMENT_PLAN.md` remains the authoritative roadmap, active checkpoint, phase list, and dependency map. This file is an ordered phase-aligned implementation target for the health/body/medical/cybernetic/narcotic slice of that roadmap.

This document is not a changelog. Completed implementation still belongs in `DEVELOPMENT_HISTORY.md`. Durable implementation rules still belong in `STANDARDS_AND_PRACTICES.md`. High-level doctrine still belongs in `MASTER_GOVERNANCE_REVISION_II.md`.

## Source topical files absorbed or aligned

This milestone aligns and partially absorbs material from:

- `MILESTONE_KNOWLEDGE_SKILLS_MEDICAL_AND_FABRICATION_SYSTEMS.md` where medical systems, prosthetics, cybernetics, and narcotics were first grouped with skills and fabrication.
- `MILESTONE_SUPPLEMENT_ILLICIT_NARCOTICS_FACTION_PRODUCTION.md` where narcotics production and markets touch body-effect systems.
- `MILESTONE_SUPPLEMENT_NOBLE_LUXURY_NARCOTICS_AND_DRAUGHT_TRADE.md` where noble narcotics, draught goods, private medicine, and rare substances require medical/body-effect interpretation.
- `MILESTONE_02_UI_EXAMINATION_INFOPEDIA_AND_PLAYER_READABILITY.md` where body plans, injuries, medicine, and narcotic states need readable inspection and Infopedia explanation.
- `MILESTONE_03_PRODUCTION_KNOWLEDGE_SKILLS_AND_ITEM_QUALITY.md` where medical item quality, prosthetic quality, cybernetic quality, and surgery skill connect to fabrication and skill trees.
- `MILESTONE_04_POPULATION_PROVENANCE_ECONOMY_AND_FACTION_MARKETS.md` where medical goods, narcotics, restricted medicine, clinics, black markets, and noble/private medicine participate in economy.
- `MILESTONE_06_VEHICLES_STRUCTURAL_SCALE_AND_STRATEGIC_POWER.md` where crash injury, crew harm, and vehicle damage may affect bodies.
- Master-plan Phase 14 combat, health, unconsciousness, death, saves, and feedback notes.
- `MILESTONE_ROADMAP_REORGANIZATION_AND_PHASE_ALIGNMENT.md`.

The older topical files should remain in place until their contents are fully absorbed into the ordered milestone sequence and the user explicitly approves archival, deprecation, or cleanup.

## Phase mapping

Primary phase groups:

- Phase 9 - Item provenance, medical goods provenance, narcotics provenance, and supply ecology.
- Phase 14 - Combat, health, unconsciousness, death, saves, body states, medical treatment, prosthetics, cybernetics, and feedback.
- Phase 15 - Character identity, body plan, stats, skills, and long-term character capability.
- Phase 17 - Economy, vendors, trainers, clinics, contracts, narcotics trade, medical services, and faction reputation.
- Phase 18 - Editor, localization, modding, Infopedia, and content pipeline.
- Phase 19 - Polish, packaging, QA, and release audit.

Secondary dependencies:

- Phase 2 - Asset promotion for medical items, body icons, prosthetics, cybernetics, narcotics, clinics, surgery tools, and drug/medicine labels.
- Phase 4 - UI examination, health inspection, body-state readability, treatment feedback, and Infopedia hot links.
- Phase 6 - Production, recipes, machine operations, medical item fabrication, prosthetic/cybernetic fabrication, and quality outcomes.
- Phase 7 - Access, legality, restricted medicine, controlled narcotics, stolen medical goods, and clinic ownership.
- Phase 8 - Population health demand, doctors, patients, surgeons, addicts/dependents where implemented, and clinic workforce.
- Phase 10 - Vehicle injury, crew harm, cybernetic/prosthetic transport and repair supply chains where relevant.
- Phase 11 - Faction schemes involving clinics, medicine hoarding, narcotics, poisoning, contamination, cybernetic capture, and medical scandals.
- Phase 12 - Clinics, surgery rooms, medical storage, cybernetic labs, and player-owned medical facilities.
- Phase 13 - Hazards, toxins, gas, chemicals, radiation, disease, burns, and environmental injury.

## Core doctrine

Health should not be a single invisible number with a healing item slapped on top. The body system should be readable enough for the player to understand injuries, risks, treatment paths, recovery expectations, and consequences.

Medical systems should create decisions. Field treatment, clinic treatment, surgery, drugs, prosthetics, cybernetics, narcotics, counterfeit medicine, contaminated batches, poor-quality tools, bad facilities, and unskilled operators should all be able to matter where the simulation supports them.

Narcotics and medicines should not be only market goods. When consumed or administered, they should have body-state effects, risks, duration, quality, legality, source, side effects, and possible dependency/addiction consequences where appropriate.

Prosthetics and cybernetics should not be simple equipment icons. They should connect to body plans, injury, surgery, fabrication quality, installation skill, maintenance, power/calibration where appropriate, legality, faction preference, and character capabilities.

## Dependency rule

Medical, prosthetic, cybernetic, narcotic, and pharmaceutical systems depend on stable body-state definitions, treatment definitions, item-quality definitions, medical facility definitions, vendor/access definitions, skill/knowledge integration, and Infopedia entries.

The body system should support aggregation where detail is not needed, but player-visible injuries, major NPCs, quest targets, companions, pets, vehicles crews, and surgery/cybernetic cases should be able to expose more detailed state.

Medical item quality should influence treatment outcome where meaningful. A clean high-quality medical kit, a counterfeit painkiller, a contaminated narcotic batch, or a crude prosthetic should not behave identically unless the game explicitly abstracts that category.

## Phase 9 - Medical, narcotic, and body-related provenance

### Phase 9.1 - Medical item provenance pass

Medical goods should preserve provenance when the source matters.

Medical item provenance may include:

- Producing clinic.
- Producing lab.
- Producing faction.
- Producing machine or facility.
- Medical supplier.
- Noble physician source.
- Gang or illicit producer.
- Black-market broker.
- Off-world import.
- Batch identity.
- Quality band.
- Legal/restricted/illicit status.
- Counterfeit, diluted, contaminated, expired, or stolen status.
- Intended treatment use.
- Known side effect or risk profile where identified.

Exit criteria:

Medical goods can be traced to source, quality, legality, and risk when those facts affect gameplay.

### Phase 9.2 - Narcotics and drug-effect provenance pass

Narcotics, stimulants, sedatives, painkillers, anesthetics, performance drugs, recreational drugs, and controlled substances should track source and risk where relevant.

Provenance should distinguish:

- Medical narcotic.
- Recreational narcotic.
- Combat-performance drug.
- Sedative.
- Stimulant.
- Painkiller.
- Anesthetic.
- Anti-toxin.
- Restricted medicine.
- Noble luxury substance.
- Rare draught item.
- Black-market drug.
- Counterfeit drug.
- Contaminated batch.
- Illicit faction product.

Exit criteria:

Drug items can produce body effects and market consequences based on source, quality, legality, and faction context.

### Phase 9.3 - Prosthetic and cybernetic provenance pass

Prosthetic and cybernetic parts should carry enough provenance to explain quality, compatibility, value, legality, and maintenance.

Provenance may include:

- Manufacturer.
- Fabricating facility.
- Faction or corporation source.
- Surgeon/installer.
- Installation clinic.
- Component quality.
- Blueprint or schematic source.
- New, refurbished, salvage, stolen, black-market, military, noble, experimental, or counterfeit status.
- Maintenance history.
- Calibration history.
- Prior owner where relevant.

Exit criteria:

Prosthetics and cybernetics are produced, installed, maintained, and valued as traceable objects.

## Phase 14 - Body states, injury, treatment, and recovery

### Phase 14.1 - Body-state model pass

Define body-state categories that can support combat injury, environmental harm, medicine, surgery, prosthetics, cybernetics, and examination.

Body states may include:

- Healthy.
- Bruised.
- Cut.
- Bleeding.
- Broken or fractured where supported.
- Burned.
- Infected.
- Poisoned.
- Toxin-exposed.
- Sedated.
- Stimulated.
- Exhausted.
- Shocked.
- In pain.
- Numbed.
- Unconscious.
- Recovering.
- Disabled limb or region.
- Missing limb or region where supported.
- Prosthetic installed.
- Cybernetic installed.
- Cybernetic malfunction.
- Rejection/compatibility issue where supported.

Exit criteria:

Body conditions have named states that can be inspected, treated, and referenced by UI and Infopedia systems.

### Phase 14.2 - Wound, pain, bleeding, infection, and shock pass

The medical model should move beyond generic healing.

Supported injury effects may include:

- Wounds.
- Bleeding.
- Pain.
- Infection.
- Shock.
- Burn damage.
- Toxic exposure.
- Chemical exposure.
- Disease where future content supports it.
- Radiation or setting-specific exposure where future content supports it.
- Organ or body-region injury where body-plan detail supports it.

Treatment should differ by condition. A bandage, antiseptic, painkiller, surgery kit, anti-toxin, sedative, or cybernetic implant should not all solve the same generic bar.

Exit criteria:

Injuries create distinct treatment needs and player-readable consequences.

### Phase 14.3 - Field treatment pass

Field treatment should provide emergency stabilization rather than perfect recovery.

Field treatment may include:

- Stop bleeding.
- Reduce pain.
- Apply antiseptic.
- Splint or brace.
- Administer stimulant.
- Administer sedative.
- Administer painkiller.
- Administer anti-toxin.
- Stabilize unconscious patient.
- Prepare patient for transport.

Field treatment should consider medical skill, item quality, lighting/conditions, time pressure, hostile surroundings, and available tools.

Exit criteria:

The player can stabilize or improve conditions outside clinics without making clinics irrelevant.

### Phase 14.4 - Clinic treatment and surgery pass

Clinics, surgery rooms, hospitals, field hospitals, faction medical rooms, and private noble medical rooms should support deeper treatment.

Clinic treatment may include:

- Diagnosis.
- Cleaning wounds.
- Treating infection.
- Surgery.
- Setting bones where supported.
- Removing foreign objects where supported.
- Blood/fluids support where supported.
- Anesthesia.
- Post-op recovery.
- Prosthetic installation.
- Cybernetic installation.
- Long-term care.
- Specialist consultation.

Exit criteria:

Medical facilities provide meaningful treatment paths beyond field stabilization.

### Phase 14.5 - Recovery, risk, and failure pass

Treatment should have outcomes and risks.

Outcome factors include:

- Condition severity.
- Time untreated.
- Medical skill.
- Facility quality.
- Tool quality.
- Medicine quality.
- Patient state.
- Infection/contamination.
- Surgery difficulty.
- Prosthetic/cybernetic compatibility.
- Drug side effects.
- Counterfeit or contaminated items.

Possible outcomes:

- Full recovery.
- Stabilized but not healed.
- Slow recovery.
- Complication.
- Infection.
- Permanent impairment.
- Death.
- Cybernetic/prosthetic malfunction.
- Addiction/dependency development where supported.
- Faction/legal consequence where relevant.

Exit criteria:

Medicine and surgery have stakes without becoming unreadable punishment.

### Phase 14.6 - Medical inspection and feedback pass

Player-facing medical UI should explain conditions and treatment possibilities compactly.

Inspection should show:

- Current condition.
- Approximate severity.
- Urgency.
- Known cause where identified.
- Available treatment.
- Missing treatment.
- Risk factors.
- Recovery state.
- Whether better tools, facility, skill, medicine, or transport would help.

Exit criteria:

The player understands what is wrong and what broad treatment path exists.

## Phase 14 - Prosthetics and cybernetics

### Phase 14.7 - Prosthetic replacement pass

Prosthetics should connect to body plans and injury.

Supported concepts:

- Missing or disabled body part where body simulation supports it.
- Basic prosthetic.
- Crude prosthetic.
- Standard prosthetic.
- Refined prosthetic.
- Industrial prosthetic.
- Medical-grade prosthetic.
- Military prosthetic.
- Faction-specific prosthetic.
- Black-market prosthetic.
- Salvaged prosthetic.
- Maintenance requirements.
- Fit and comfort.
- Skill or stat effects.
- Social/legal/faction effects where appropriate.

Exit criteria:

Prosthetics restore, alter, or compensate for body-state changes through inspectable objects and treatment paths.

### Phase 14.8 - Cybernetic installation pass

Cybernetics should be more demanding than ordinary prosthetics.

Cybernetic systems may require:

- Specialist surgeon.
- Cybernetics skill.
- Surgery room.
- Calibration tools.
- Power or maintenance systems.
- Compatible body state.
- Sterile or controlled environment.
- High-quality parts.
- Legal/faction permission where restricted.
- Follow-up maintenance.

Cybernetics may provide:

- Stat improvements.
- New capabilities.
- Sensory changes.
- Combat or labor advantages.
- Social/faction reactions.
- Maintenance burdens.
- Power/calibration needs.
- Failure modes.
- Rejection or compatibility risk where supported.

Exit criteria:

Cybernetics are durable body modifications with benefits, costs, risks, and infrastructure requirements.

### Phase 14.9 - Prosthetic/cybernetic quality and malfunction pass

Quality should affect prosthetic and cybernetic behavior.

Quality effects may include:

- Reliability.
- Comfort.
- Maintenance interval.
- Stat effect.
- Precision.
- Power use.
- Noise.
- Failure chance.
- Repair difficulty.
- Compatibility.
- Value.
- Legality.

Malfunctions may include:

- Reduced function.
- Pain.
- Twitching or misfire.
- Power drain.
- Lockup.
- Sensory distortion.
- Increased injury risk.
- Medical complication.

Exit criteria:

A crude prosthetic, refined prosthetic, military cybernetic, and counterfeit implant do not behave identically.

## Phase 15 - Character stats, skills, and long-term body identity

### Phase 15.1 - Body plan and character identity bridge

Character identity should include enough body-plan information to support examination, injuries, prosthetics, cybernetics, and long-term effects.

Body identity may include:

- Species/body type.
- Major body regions.
- Permanent injuries.
- Prosthetics.
- Cybernetics.
- Scars or visible markers where relevant.
- Medical history where relevant.
- Known dependencies or treatment needs where implemented.
- Stat changes from injuries, treatment, prosthetics, or cybernetics.

Exit criteria:

Long-term body changes can persist and affect character identity.

### Phase 15.2 - Medical and cybernetic skill integration pass

Skill trees should include branches or nodes that affect medical and cybernetic outcomes.

Potential skill areas:

- First aid.
- Surgery.
- Diagnosis.
- Pharmacology.
- Prosthetic fitting.
- Cybernetic installation.
- Cybernetic maintenance.
- Addiction/dependency management where implemented.
- Trauma care.
- Field medicine.
- Veterinary or animal care if pet/animal health later uses the system.

Exit criteria:

Medical and cybernetic work depends on trained capability, not only item possession.

## Phase 17 - Medical economy, clinics, trainers, and narcotic effects

### Phase 17.1 - Medical vendor and clinic service pass

Medical factions, clinics, hospitals, black-market doctors, noble physicians, gang medics, and field doctors should provide appropriate goods and services.

Services may include:

- Diagnosis.
- Field supplies.
- Surgery.
- Recovery bed access.
- Prosthetic fitting.
- Cybernetic installation.
- Detox or dependency care where implemented.
- Restricted medicine access.
- Black-market treatment.
- Noble private medicine.

Exit criteria:

Medical treatment has social, economic, factional, and facility contexts.

### Phase 17.2 - Narcotic, medicine, and drug-effect pass

Drugs and medicines should define effects, duration, side effects, quality, and risk.

Item categories include:

- Bandages and dressings.
- Splints and braces.
- Antiseptics.
- Antibiotics.
- Painkillers.
- Stimulants.
- Sedatives.
- Anesthetics.
- Anti-toxins.
- Anti-radiation or setting-specific counteragents where supported.
- Surgical kits.
- Cybernetic installation supplies.
- Addiction-risk narcotics.
- Recreational narcotics.
- Performance drugs.
- Black-market medical goods.
- Counterfeit medicine.
- Faction-specific pharmaceuticals.
- Noble luxury substances.
- Draught items where future definitions assign medical or narcotic effects.

Exit criteria:

Medical and narcotic items have effects, risks, supply paths, legality, quality, and body-state consequences.

### Phase 17.3 - Addiction, dependency, tolerance, and withdrawal framework pass

If addiction/dependency mechanics are implemented, they must be readable and fair enough to support player decision-making.

Framework concepts may include:

- Dependency risk.
- Tolerance.
- Withdrawal.
- Craving.
- Medical treatment.
- Substitution treatment.
- Illegal supply pressure.
- Faction exploitation.
- Noble indulgence.
- Gang control.
- Performance-drug tradeoff.
- Player-facing warning.

This pass should be implemented carefully and only when supporting UI, treatment, and player agency exist.

Exit criteria:

Dependency systems, if present, are inspectable, treatable, and tied to meaningful choices rather than hidden punishment.

### Phase 17.4 - Medical and narcotic contract pass

Contracts should be able to reference medical and drug systems.

Examples:

- Deliver medicine.
- Find antibiotics.
- Recover stolen prosthetic parts.
- Smuggle restricted medicine.
- Identify counterfeit drugs.
- Investigate contaminated narcotics.
- Guard a clinic.
- Treat injured faction members.
- Escort a surgeon.
- Recover cybernetic components.
- Steal medical supplies.
- Disrupt gang narcotics production.
- Retrieve noble draught medicine.

Exit criteria:

Medical and narcotic systems participate in quests and faction markets.

## Phase 18 - Editor, audit, and Infopedia support

### Phase 18.1 - Medical/body editor and audit pass

Editor/audit surfaces should define and inspect:

- Body plans.
- Body regions.
- Injury states.
- Pain states.
- Bleeding states.
- Infection states.
- Drug effects.
- Treatment definitions.
- Medical item definitions.
- Medicine quality.
- Narcotic quality.
- Addiction/dependency flags where implemented.
- Prosthetic definitions.
- Cybernetic definitions.
- Surgery requirements.
- Clinic service definitions.
- Doctor/surgeon/trainer requirements.
- Recovery rules.
- Failure/complication rules.
- Provenance fields.

Editor/audit surfaces may expose semantic IDs where explicitly needed for development, but ordinary player UI must hide raw IDs.

Exit criteria:

Medical, body, drug, prosthetic, and cybernetic systems are data-auditable rather than hidden in source sprawl.

### Phase 18.2 - Medical/body Infopedia pass

Infopedia entries should explain:

- Body states.
- Injuries.
- Medical treatments.
- Field treatment.
- Clinic treatment.
- Surgery.
- Prosthetics.
- Cybernetics.
- Drug categories.
- Narcotic effects.
- Side effects.
- Dependency/addiction risk where implemented.
- Medicine quality.
- Counterfeit or contaminated medicine.
- Required tools and facilities.
- Relevant skills.
- Vendor/faction availability.
- Legal/restricted/illicit status.

Exit criteria:

The player can look up medical, body, narcotic, prosthetic, and cybernetic mechanics without external notes.

## Phase 19 - Release audit

Before release claims for this milestone, verify:

- Body states are named and inspectable where implemented.
- Injury types have readable treatment paths.
- Field treatment stabilizes without replacing clinics.
- Clinic treatment, surgery, and recovery have meaningful requirements where implemented.
- Medical skill, facility quality, tool quality, and medicine quality can affect outcomes where relevant.
- Medical items have clear effects, risks, quality, and source.
- Narcotics have effects, risks, legality, and supply paths where present.
- Addiction/dependency systems, if implemented, are readable, treatable, and not hidden punishment.
- Prosthetics and cybernetics connect to body plans, surgery, item quality, maintenance, and character capability.
- Prosthetic/cybernetic quality affects reliability or capability where relevant.
- Medical vendors, clinics, black-market doctors, and noble/private medicine contexts are represented where implemented.
- Medical/narcotic contracts can reference items, treatments, and facilities where implemented.
- Infopedia entries explain exposed mechanics.
- Player-facing text avoids raw IDs and placeholder labels.
- Editor/audit surfaces can inspect key definitions.

Exit criteria:

The game may claim medical depth, body-state systems, prosthetics, cybernetics, narcotics, and medical item systems only after they are inspectable, player-facing, and connected to body state, treatment, item quality, facility access, vendors, skills, provenance, and Infopedia reference.

## Non-goals for this milestone

This milestone does not require every injury, disease, drug, or prosthetic to be implemented at once. It defines the system boundaries and audit rules so later implementation does not become generic status-effect sprawl.

This milestone does not require addiction/dependency mechanics to exist before the UI and treatment pathways can support them responsibly.

This milestone does not turn narcotics economy into the same thing as narcotic body effects. Economy and market behavior belong primarily to Milestone 04; this milestone owns the body-state, treatment, risk, effect, and inspection side.

This milestone does not require every background NPC to have a full detailed body simulation. Background actors may remain abstracted until combat, examination, treatment, surgery, quest relevance, companion status, or player visibility requires detail.

## Deferred checkpoint summary

Medical systems need a dedicated development block. Injury, treatment, recovery, drugs, prosthetics, cybernetics, narcotics, and medical goods should connect to body plans, item quality, facilities, skills, vendors, factions, legality, side effects, dependency/addiction risk where appropriate, and Infopedia entries. Prosthetics and cybernetics should be inspectable body modifications with installation, maintenance, quality, risk, and character-capability consequences. Narcotics and medicines should be coherent gameplay objects with effects, risks, provenance, legality, and supply paths rather than generic consumables or disconnected contraband.
