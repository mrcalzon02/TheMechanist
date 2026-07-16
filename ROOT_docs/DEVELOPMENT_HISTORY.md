# Development History

This is the fresh active milestone-development history for The Mechanist after the prior active ledger was archived.

The previous active milestone ledger is archived at:

`ROOT_docs/archive/DEVELOPMENT_HISTORY_MILESTONE_LEDGER_ARCHIVE_2026-06-05.md`

Earlier pre-milestone development remains archived at:

`ROOT_docs/archive/DEVELOPMENT_HISTORY_PRE_MILESTONE_DEVELOPMENT.md`

Use this file for new completed work from this reset onward. Keep entries concise: record what changed, why it matters for the milestone sequence, and what verification was run. Do not restate the full roadmap here; roadmap authority remains in `MASTER_DEVELOPMENT_PLAN.md`, with detailed milestone targets indexed by `ROOT_docs/MILESTONE_INDEX.md`.

## Milestone 05 - Physical Faction Facility Construction Slice

Advanced the Phase 17.2 construction-parity lane from an instant live faction-site level mutation into a real staged facility in the loaded world. A Mechanist or Mechanicus `build or upgrade a factory` plan now requires the exact linked site to be local, selects a same-family controlled non-special room, requires assigned same-family workers from that exact room when local population ledgers exist, and finds a legal empty interior tile before it can acquire a plan or spend stock. Success learns or reuses the EMM Micro Forge plan, reserves the exact twelve-unit aggregate recipe cost from faction-site stock, and places a fully prepaid unfinished Micro Forge ghost. Site levels and strategic success remain unchanged while physical work is pending, and repeated strategy resolution resumes the same job without another stock debit or duplicate object.

Hourly faction simulation advances the physical site with the exact room-local workforce as absolute labor. It does not pull player materials, apply player tool multipliers, spend player turns, grant player experience, increment the player's crafted count, or change player blueprint, heat, suspicion, inventory, storage, or currency state. Completion converts the live tile into an operational Micro Forge, advances the linked site's base and machine levels exactly once through a durable completion receipt, and records one linked strategic success. Ordinary site production pauses while its construction crew is active and resumes after completion.

Faction construction ownership, material source, named plan source, linked site job, and linked strategic-plan identity now persist as append-only `BaseObject` fields; faction sites persist completed-construction receipts with legacy parsing retained. Normal Look and construction status expose staged materials, labor, faction custody, assigned crew, and plan provenance. Player Work, Dismantle, Operate, Craft, Repair, manual staffing, production-board, workbench, and background-production routes reject or exclude faction-managed sites and completed facilities, while completed Look retains the same custody and source readback after save/load.

Verification: Java 17 full-tree compile passed with the existing Netty unchecked note; the new faction physical-construction smoke passed exact-room staging, one-time stock reservation, duplicate resume, Look/status provenance, player isolation, full-core persistence, exact hourly labor, completion attribution, receipt idempotency, and atomic controlled-room/workforce blockers; the upgraded faction blueprint-site integration smoke passed physical staging and 4/8/10 hourly completion while retaining its direct acquisition and blocker coverage; adjacent production-control, staffed-production, repair, staged Work/Dismantle/tile, faction-workforce-market, blueprint-readiness, and room-plan-seller smokes passed. Expanded Gate 3 passed with authoritative exit code 0 alongside the existing generated-world save fallback, topology, and production-input warnings. The client package rebuilt from 792 Java sources with 1863 class files and 6096 package assets; the Java 17 package scan passed for 3726 classfiles across the unfolded package and JAR, highest major version 61; both faction physical-construction smokes passed from packaged classes; package boot stayed alive for 8 seconds; function and Mermaid maps refreshed at 792 Java modules and 0 unpositioned modules; repository manifest refreshed with zero errors.

## Milestone 05 - Live Faction Blueprint Requisition and Site Upgrade Slice

Advanced Phase 17.2 from an abstract successful strategy roll into a staffed, supplied faction-site mutation for the Mechanist and Mechanicus family. `Build or upgrade a factory` now selects the established EMM Micro Forge plan, requires a living same-family Industrial Blueprint Trader for first acquisition, checks the site's effective workforce, and spends the exact aggregate site stock represented by the recipe's supplies, parts, and five components. A known plan remains reusable if the original vendor later leaves. Successful work records the plan at the site, spends twelve stock, and raises the site's base and machine levels within their existing caps; failed acquisition, staffing, stock, ownership, cap, or ledger checks report every blocker before any mutation.

The strategy simulation now counts success only after that live authority succeeds. A blocked requisition records an exact faction outcome and market-pressure consequence instead of granting a false abstract success, while non-Mechanist factions retain the previous route until their own plan families are connected. Construction-plan knowledge is a durable faction-site property with backward-compatible save parsing, a player-facing named-plan summary, and no leakage into the player's script, inventory, materials, experience, construction unlocks, heat, or suspicion. The upgraded site also reaches the existing higher production cadence, proving that the mutation changes subsequent simulation rather than only its readback.

Verification: Java 17 full-tree compile passed with the existing Netty unchecked note; the focused faction blueprint-site upgrade smoke passed first acquisition, vendor departure reuse, missing vendor, zero workforce, insufficient stock, cap, wrong-faction, and ledger blockers; verified atomic stock/level/plan mutation, durable save/load, named summary, higher production cadence, and complete player-state isolation; adjacent room-plan seller, blueprint-offer readiness, faction-market, and faction-site workforce smokes passed; expanded Gate 3 passed with authoritative exit code 0 alongside the existing generated-world save fallback, topology, and production-input warnings. The consolidated package rebuild passed with 1856 class files and 6096 package assets; the Java 17 package scan passed for 3712 classfiles across the unfolded package and JAR, highest major version 61; the room-plan seller, blueprint-offer readiness, and faction blueprint-site upgrade smokes passed from packaged classes; package boot stayed alive for 8 seconds; function and Mermaid maps refreshed at 790 Java modules and 0 unpositioned modules; repository manifest refreshed with zero errors.

## Milestone 05 - Live Construction-Blueprint Offer Readiness Slice

Advanced Phase 17.1 at the actual purchase decision. Licensed construction folios now add a blueprint-specific Trade readback naming the exact room or asset unlocked, issuing faction, representative role, live counter price, legal class, current access requirement, one-per-run shelf behavior, quest-prerequisite state, construction effort, and forecast heat and suspicion. The panel gives separate `Purchase: READY/BLOCKED` and `Build after purchase: READY/BLOCKED` verdicts, so reputation, suspicion, duplicate ownership, script, and carrying capacity cannot be confused with doctrine, workbench, material, component, or later site-placement requirements. Partial staged materials and a complete absence of materials are reported truthfully without preventing an otherwise legal folio purchase.

Preview and execution consume the same blueprint preflight before the existing atomic trade fallbacks. A denied folio leaves script, time, inventory, unlocks, and the vendor shelf unchanged. A successful purchase still spends the displayed live price, retains the physical folio, records the durable construction unlock, removes the learned offer, and leaves unmet post-purchase construction requirements active. Ordinary trade offers remain outside this blueprint-only path.

Verification: Java 17 full-tree compile passed with the existing Netty unchecked note; the focused blueprint-offer readiness smoke passed reputation and high-suspicion denials, canonical live pricing, purchase-versus-build separation, absent doctrine/workbench/material readback, ordinary-offer isolation, denied non-mutation, and successful folio/unlock/shelf mutation; adjacent blueprint-ownership, faction-market, room-plan seller, and room-provenance smokes passed; expanded Gate 3 passed with authoritative exit code 0 alongside the existing generated-world save fallback, topology, and production-input warnings. Package, Java 17 classfile, packaged-smoke, boot, code-map, and manifest verification are consolidated with the next adjacent slice below.

## Milestone 05 - Live Room-Plan Seller Navigation Slice

Completed the live Phase 16.2 bridge from room-plan discovery to the physical specialist who actually stocks the plan. A locked mapped plan now names the nearest living same-family vendor of the exact required category, the licensed folio, direction, distance, and catalog price. Public and already-owned plans say that no licensed seller is needed, while absent stock names the issuing faction and representative type the player should seek. Dead staff and closer vendors of the wrong specialty cannot satisfy the match.

Normal Look exposes `Locate Seller` for a live match. The action spends no turn, replaces its prior plan-seller objective instead of duplicating it, opens the ordinary Map with an exact marker at the seller's current position, and reports whether a presently walkable path reaches an adjacent interaction tile. A sealed route retains the known target but explains the obstruction. Reaching the marker and using the normal adjacent Interact action opens that NPC's real Trade session with the requested folio; seller navigation does not fabricate or bypass market stock.

Verification: Java 17 full-tree compile passed with the existing Netty unchecked note; the focused room-plan seller guidance smoke passed connected, sealed, mismatched-vendor, dead-vendor, marker replacement, and real Interact-to-Trade cases; adjacent room-control provenance, purchase, ownership UI, and blueprint-ownership smokes passed; expanded Gate 3 passed with authoritative exit code 0 alongside the existing generated-world save fallback and topology warnings. Package, Java 17 classfile, packaged-smoke, boot, code-map, and manifest verification are consolidated with the next adjacent slice below.

## Milestone 05 - Live Room Origin and Construction-Plan Discovery Slice

Advanced Phase 16.1 from an audit catalog into ordinary room examination. Repeated Look now connects a generated room's original faction standard and current controller to the existing room-construction parity catalog. Mapped rooms name the matching construction plan and its established seller or representative/legal path; rooms intentionally excluded from ordinary construction say that no ordinary blueprint is offered and give the non-acquirable reason. The player-facing bridge consumes `RoomProfile`, `RoomConstructionParityAuthority`, and `BlueprintAcquisitionPathAuthority` rather than creating a second room or blueprint catalog.

This lets a captured or transferred room preserve and explain its construction origin even when current control differs. Internal mapping sentinels and future-development exception prose remain outside ordinary Look text.

Verification: Java 17 full-tree compile passed with the existing Netty unchecked note; focused room-control provenance, room purchase, room ownership UI/command, room-construction parity, and blueprint-acquisition path smokes passed; expanded Gate 3 passed with authoritative exit code 0 alongside the existing world-generation and save fallback warnings; package rebuild passed with 1843 class files and 6096 package assets; Java 17 package scan passed for 3686 classfiles, highest major version 61; the provenance, purchase, and ownership UI smokes passed from packaged classes; package boot stayed alive for 8 seconds; function and Mermaid maps refreshed at 784 Java modules and 0 unpositioned modules; repository manifest refreshed with 216168 indexed rows, reused=212426, hashed=3742, errors=0.

## Milestone 05 - Permanent Legal Room Purchase Slice

Expanded Phase 12.4 with a permanent legal purchase path. A faction-owned room now presents an exact deterministic quote based on room area, with a noble-property premium, and reports every unmet condition together. Purchase requires no other claimed base, a non-special room, no living occupants, a living same-family faction representative in the current zone, no active same-family hostility, standing 5 for ordinary property or 25 for noble property, and enough carried script. Faction aliases share representative, hostility, and standing authority.

The player-rank `room_buy` command and the normal Map `Buy Room` action use the same mutation. Success atomically spends the quoted script, transfers the authoritative world room-faction ledger, establishes the purchased room as the claimed base, records seller and price in durable ownership history, and spends one turn. Invalid arguments and every authorization, occupancy, standing, hostility, funds, special-room, or second-base denial preserve time, script, controller, base state, and history. The room profile's original faction remains unchanged as construction provenance, and purchase state plus paid currency survive save/load.

Verification: Java 17 full-tree compile, focused purchase/UI/command/provenance smokes, authoritative expanded Gate 3, maintained package rebuild, Java 17 classfile scan, packaged focused smokes, 8-second package boot, code-map refresh, and repository-manifest refresh all passed with the consolidated counts recorded above.

## Milestone 05 - Normal Room Ownership Look and Map Control Slice

Moved the Phase 12.4 claim/loss loop out of console-only discovery. Look now names the targeted room, current controller, access category, room use/features, the applicable acquisition or loss path, and the latest durable control receipt without exposing numeric room IDs. The Map panel shows the same information for the player's current room and provides context-sensitive `Claim Room`, `Abandon Room`, `Buy Room`, or `Room Control` actions.

Console and normal UI routes now share one success-only completion boundary for event logging and turn spending. Neutral claims and voluntary abandonment still use the authoritative room ledger and durable receipts; remote or rival denials remain free and non-mutating, while active owners lead into the quoted purchase path rather than a generic refusal.

Verification: Java 17 full-tree compile, focused ownership UI/command/purchase/provenance smokes, authoritative expanded Gate 3, maintained package rebuild, Java 17 classfile scan, packaged focused smokes, 8-second package boot, code-map refresh, and repository-manifest refresh all passed with the consolidated counts recorded above.

## Milestone 05 - Live Room Claim and Loss Slice

Started Phase 12.4 with a real room-control mutation instead of another construction flag. The player-rank `room_claim` command claims the neutral room the player is standing in through an abandonment claim; `room_abandon` relinquishes it only while the player is back inside; and `room_status` reports the current owner, claimed base, and latest control change. Successful claim and abandonment each spend one turn. Invalid arguments, corridor attempts, duplicate claims, active-faction rooms, remote abandonment, and attempts to establish a second base change neither time nor ownership.

Both commands mutate the world's authoritative room-faction ledger and keep the claimed-base pointer aligned with it. Acquisition and loss receipts retain turn, room, previous owner, next owner, and method across save/load. Claimed-room checks now use exact generated room membership, while mapless legacy state retains the prior coordinate fallback for compatibility. Ordinary base text uses the room's player-facing name instead of exposing its internal numeric ID.

Verification: Java 17 full-tree compile passed with the existing Netty unchecked note; focused room-control, construction-reaction, production-facility knowledge, production-location provenance, and legacy claimed-room compatibility smokes passed; expanded Gate 3 passed with authoritative exit code 0 alongside the existing sandbox campaign-save and production-input fallback warnings; package rebuild passed with 1838 class files and 6096 package assets; Java 17 package scan passed for 3676 classfiles, highest major version 61; the room-control smoke passed from packaged classes; package boot stayed alive for 8 seconds; function and Mermaid maps refreshed at 780 Java modules and 0 unpositioned modules; repository manifest refreshed with 216136 indexed rows, reused=212421, hashed=3715, errors=0.

## Milestone 05 - Cumulative Expansion Reaction Slice

Advanced Phase 12.3 from meter mutation into durable faction response. Construction exposure now crosses one-time noticeable, high, and critical thresholds. Local notice raises Civic Warden and ganger market pressure; high exposure adds established-faction and noble attention; critical exposure marks the holding as a rival power center. Player feedback names likely permit offers, protection demands, surveillance, fees, recruitment, diplomacy, sabotage, raids, and scheme targeting at the appropriate band instead of leaving the meters unexplained.

Threshold receipts persist with the run, prevent repeated pressure from every later construction start in the same band, and remain visible in the expansion readback after save/load. A legacy or already-high run receives the strongest unrecorded applicable response rather than replaying every lower notice.

Verification: Java 17 full-tree compile passed with the existing Netty unchecked note; focused construction-attention and cumulative expansion-reaction smokes passed; expanded Gate 3 passed with authoritative exit code 0 alongside the existing sandbox campaign-save and production-input fallback warnings; package rebuild passed with 1838 class files and 6096 package assets; Java 17 package scan passed for 3676 classfiles, highest major version 61; package boot stayed alive for 8 seconds; function and Mermaid maps refreshed at 780 Java modules and 0 unpositioned modules; repository manifest refreshed with 216136 indexed rows, reused=212421, hashed=3715, errors=0.

## Milestone 05 - Live Construction Attention Slice

Advanced Phase 12.3 from forecast-only text into a live player consequence. Starting a valid prepaid or partial staged-construction site now adds the blueprint's already-visible heat and suspicion impacts exactly once to the persisted global meters. The construction event names both before-and-after values and the concrete exposure drivers; the build detail continues to preview the same impacts before confirmation. Blocked placement leaves both meters unchanged, so failed attempts cannot create invisible attention.

Verification: Java 17 full-tree compile passed with the existing Netty unchecked note; the focused construction-attention, construction-readability, blueprint-ownership, and blueprint-heat audit smokes passed; expanded Gate 3 passed alongside the existing campaign-world save and production fallback warnings; package rebuild passed with 1831 class files and 6096 package assets; Java 17 package scan passed for 3662 classfiles, highest major version 61; the focused smoke passed from packaged classes; package boot smoke stayed alive for 8 seconds; function and Mermaid maps refreshed at 776 Java modules and 0 unpositioned modules; repository manifest refreshed with 214254 indexed rows; diff check passed apart from existing line-ending warnings.

## Milestone 05 - Licensed Construction Blueprint Ownership and Vendor Acquisition Slice

Started the Milestone 05 sequence with a live Phase 7.2/7.3, Phase 12.2, Phase 16.2, and Phase 17.1 acquisition path instead of extending the older blueprint audit chain. Every faction-bound live build recipe now has a stable construction-blueprint ID and a named, catalog-backed licensed folio. Concord Guard and Civic Wardens armories stock their own defensive plans, Mechanist works factors stock Mechanicus-family machinery plans, and noble brokers stock house plans. Offers retain their issuing faction, exact construction unlock, price derived from recipe complexity, physical document, and the existing faction-market access rule. Public starter construction remains available without a license.

Restricted recipes remain visible in the construction menu with a `LOCKED` marker and an exact acquisition path. Placement checks reject an unknown licensed plan before knowledge, workbench, material, component, labor, and terrain checks; after an authorized successful purchase the named folio enters inventory and the recipe becomes usable while all other requirements remain intact. Failed or duplicate purchases spend nothing and grant nothing, learned offers leave the current shelf and are filtered from reopened vendor sessions, and the learned-blueprint ledger survives save/load. Named folios are ordinary searchable item-catalog entries, so the existing item Infopedia exposes their source, price, purpose, and retained physical proof.

Verification: Java 17 full-tree compile passed with the existing Netty unchecked note; the focused blueprint ownership smoke passed an actual denied purchase, authorized purchase, physical folio transfer, build-menu transition, and save/load round trip; adjacent faction-vendor, faction-market, construction-readability, construction-category, keyboard, and staged-construction command smokes passed; expanded Gate 3 passed alongside the existing campaign-world save and production fallback warnings; package rebuild passed with 1829 class files and 6096 package assets; Java 17 package scan passed for 3658 classfiles, highest major version 61; the focused smoke passed from packaged classes; package boot smoke stayed alive for 8 seconds; function and Mermaid maps refreshed at 775 Java modules and 0 unpositioned modules; repository manifest refreshed with 214219 indexed rows, reused=208676, hashed=5543, errors=0.

## Milestone 04 - Release Audit and Protected Draught Custody Contract Slice

Completed Phase 19 and closed the Milestone 04 release-audit sequence against the existing gameplay smokes and player-facing surfaces. Population identity drives demand and labor; critical vendors are faction/facility backed; essential, security, medical, agricultural, construction, luxury, illicit, and raw-material stock retains finite source logic; reinforcements retain source-specific prices, timing, capacity, expiry, event, import-node, and personnel provenance; distant factions resolve through persisted probability ledgers; and top-down events remain distinct from officer schemes while exposing timing, restrictions, shipment/reinforcement effects, physical mutation, news, exceptions, and recovery. Editor snapshots and searchable item references make those systems inspectable without exposing implementation IDs in ordinary player text.

The audit found and closed one concrete gameplay gap rather than adding another audit framework. Protected noble draughts could already be vaulted, traced, withheld from normal sale, stolen, counterfeited, or exceptionally released, but faction work did not reference their custody. A noble representative can now offer a political support contract for the exact held draught. The player supplies a Noble Commerce Permit and must satisfy the existing trade-guilder, investigation, and Contract Negotiation proof gates. Unqualified turn-in consumes nothing; qualified completion renews the named custody record, pays through the atomic contract flow, and explicitly leaves the draught quantity and not-for-sale lock unchanged.

Verification: Java 17 full-tree compile passed with the existing Netty unchecked note; focused faction contract/news, noble draught provenance, and live editor/Infopedia smokes passed; expanded Gate 3 passed with zero unreachable rooms alongside the existing campaign-save storage error and production fallback warnings; final package rebuild passed with 1826 class files and 6096 package assets; Java 17 package scan passed for 3652 classfiles, highest major version 61; package boot smoke stayed alive for 8 seconds; the custody contract smoke passed from packaged classes; function and Mermaid maps refreshed at 773 Java modules and 0 unpositioned modules; repository manifest and final diff checks followed this entry.

## Milestone 04 - Live Economy, Population, Network, and World-Event Editor Slice

Completed Phases 18.1 through 18.3 as one authoring, audit, and player-reference slice. The shared tool suite now includes dedicated Population, Economy, Reinforcement, Deferred Network, and World Event editors alongside live Faction Editor refresh. Their schemas cover population origin, capacity, workforce, care and demand; reserves, provenance, legality, vendor policy, shipments and contract hooks; reinforcement sources, exact price and timing ranges, expiry and import nodes; distant-network probability inputs and explained outcomes; and world-event eligibility, severity, economy/population effects, route closures, facility mutation, news exposure, recovery, and aftermath.

Opening a supported editor against an active game now replaces stale runtime rows with structured snapshots of the real persisted world ledgers. Faction summaries cover every visible faction; population rows include room rosters, creche cohorts, NPC happiness and personnel origin; economy rows include reserves, shipments, market pressure, and faction contracts; reinforcement rows include requests, all source policies, and physical import nodes; and deferred/event rows include their current timers, factors, effects, and state. These records are editable mod-local overlays: changing or exporting a snapshot cannot mutate the running world, user-authored records survive refresh, and repeated refreshes do not duplicate runtime rows. The expanded Tools screen keeps all 17 editors, Zone Audit, packaging, and navigation reachable without overlap at a 900x600 window.

The Infopedia now exposes every catalog item as a directly searchable player-facing entry. Item pages explain category, value, common producers and facilities, uses, likely sellers, legal or protected access, reasons for scarcity, provenance alternatives, and quality, shipment, faction, event, cooldown, heat, or custody risks. Protected noble draughts, forbidden and illicit goods, regulated security stock, medical supplies, luxuries, and ordinary legal supplies receive distinct explanations rather than one generic market paragraph.

Verification: Java 17 full-tree compile passed with the existing Netty unchecked note; focused live-editor isolation/refresh/layout, 17-editor recovery, Infopedia hot-link/readability, faction-market access, and faction contract/news smokes passed; expanded Gate 3 passed including the new Phase 18 smoke with zero unreachable rooms alongside the existing campaign-save storage error and production fallback warnings; package rebuild passed with 1826 class files and 6096 package assets; Java 17 package scan passed for 3652 classfiles, highest major version 61; package boot smoke stayed alive for 8 seconds; function and Mermaid maps refreshed at 773 Java modules and 0 unpositioned modules; repository manifest and final diff checks followed this entry.

## Milestone 04 - Faction Markets, Pressure Contracts, and Event Trade Readback Slice

Completed Phases 17.1 through 17.9 as one faction-market gameplay loop. Specialist traders now retain their exact faction and market category, keep restricted stock visible with readable locked or risk labels, and enforce the same legality decision at purchase time before script, stock, inventory, or time can change. Standing, membership, permits, patronage, suspicion, underworld credibility, provenance, quarantine rules, and hostile relations govern access. Black-market factions visibly prefer several narcotics, noble brokers distinguish luxury stock from protected draught custody, and ordinary legal, military, blueprint, controlled-medical, illicit, forbidden, stolen, counterfeit, and event-rationed goods explain their requirements and consequences. The access classifier also proves that ordinary agricultural stock cannot be mistaken for cult contraband.

Faction representatives now offer market work from live economic pressure before falling back to established production orders. Active world events, interrupted shipments, reinforcement needs, depleted essential or raw-material reserves, recorded faction pressure, and illicit or noble market identity can each produce an explainable contract. Turning in the work updates the exact linked ledger: reserves recover, shipments resolve without duplicating shelf cargo, reinforcement windows advance, event relief reaches the affected reserve, or market pressure falls. The normal atomic payout and standing reward remain in force, and contract state survives save/load.

World-event activation and recovery now publish same-day newspaper and broadcast notices. Trade panels and faction conversations expose current restrictions and local exceptions. Export bans and similar closures explicitly stop off-map settlement while leaving permitted local or internal faction commerce available, and recovery reopens the affected route. The existing source-specific reinforcement costs, cooldowns, variance, expiry, and replacement behavior complete the middle of the Phase 17 sequence.

Verification: Java 17 full-tree compile passed with the existing Netty unchecked note; focused faction-market access, pressure-contract/news, production-contract fallback, contract turn-in, vertical floor trade, reinforcement lifecycle/source, top-down event, reversible facility mutation, and Infopedia smokes passed; expanded Gate 3 passed with zero unreachable rooms alongside the existing campaign-save storage error and production fallback warnings; package rebuild passed with 1823 class files and 6096 package assets; Java 17 package scan passed for 3646 classfiles, highest major version 61; package boot smoke stayed alive for 8 seconds; function and Mermaid maps refreshed at 771 Java modules and 0 unpositioned modules; repository manifest and diff checks passed.

## Milestone 04 - Top-Down World Events and Reversible Facility Mutation Slice

Completed Phases 16.8 and 16.9 as one world-state and physical-consequence sequence. Top-down events now use persisted records separate from faction officer schemes. Each event retains type, title, start and end turns, duration, scope, severity, eligibility reason, public notice channels, market category, economy and population modifiers, shipment and reinforcement timing, distant-network pressure, import/export/off-map restrictions, vendor exceptions, physical consequence, aftermath, application state, recovery state, and generation cadence. Curated families cover relief shipments, infrastructure repair, train outages, export bans, tithing decrees, quarantines, supply shocks, and civic observances. Eligibility favors relief or repair when shortages, losses, delayed freight, or structural harm are already present instead of stacking an arbitrary new penalty.

Active events now alter real shipment windows and risk, reinforcement availability, finite reserve relief, and deferred faction pressure. The player-rank `world_events` readback names timing, scope, severity, notices, effects, restrictions, exceptions, physical state, and recovery. Event selection and lifecycle survive save/load, same-turn ticks cannot duplicate effects, and event resolution leaves officer planning records untouched.

World events can also affect physical faction infrastructure. An active event binds to the exact generated import or market marker and room, records their original label, glyph, stock state, purpose, description, and feature text, then visibly closes or repurposes them. Train outages and quarantines block personnel intake at the affected import node; relief distribution remains locally accessible. Save/load reasserts active room state, and recovery restores the exact original marker and room data instead of leaving untracked permanent damage.

Verification: Java 17 full-tree compile passed with the existing Netty unchecked note; focused top-down event, facility mutation, import-node, shipment, reinforcement lifecycle/source, deferred-network, and Infopedia smokes passed; final expanded Gate 3 passed with zero unreachable rooms alongside the existing campaign-save storage error and production fallback warnings; package rebuild passed with 1813 class files and 6096 package assets; Java 17 package scan passed for 3626 classfiles, highest major version 61; package boot smoke stayed alive for 8 seconds; function and Mermaid maps refreshed at 767 Java modules and 0 unpositioned modules; repository manifest and diff checks passed.

## Milestone 04 - Individual Officer Scheme Cadence Slice

Faction strategy leaders now retain a personal scheme cadence between 80% and 125% instead of relying only on broad shared random phase ranges. Every planning cycle also receives deterministic signed jitter from minus four to plus four hours, keyed to the plan, officer, phase, cycle, and turn. The same personal pace and independent jitter apply to post-execution cooldown, so officers whose schemes begin or resolve together do not repeatedly make decisions in lockstep.

Cadence percentage, cycle count, current phase duration, and current jitter persist with the faction strategic plan while older plan records derive a stable cadence from their leader and deputy identities. Faction ledger rows and plan history name the pace, jitter, window, and next phase turn. The existing NPC happiness and officer-recruitment smoke now proves different planning and cooldown deadlines under identical random inputs, bounded timing, generated cadence limits, readback, and save parsing.

Verification: Java 17 full-tree compile passed with the existing Netty unchecked note; the expanded officer happiness/recruitment/cadence smoke and Infopedia mechanics smoke passed; Gate 3 passed including the new cadence assertions and large-world acceptance with zero unreachable rooms, alongside the existing campaign-save storage errors and production fallback warnings; package rebuild passed with 1806 class files and 6096 package assets; Java 17 package scan passed for 3612 classfiles, highest major version 61; package boot smoke stayed alive for 8 seconds; function and Mermaid maps refreshed at 763 Java modules and 0 unpositioned modules; repository manifest refreshed with 206788 indexed rows, reused=203127, hashed=3661, errors=0.

## Milestone 04 - NPC Happiness, Attrition, and Officer Recruitment Slice

Added a persisted 0-100 happiness statistic to every adult humanoid NPC, with hourly movement toward living-condition targets rather than instant swings. Food, water, pay recency, a bed, housing appropriate to rank, and faction-owned crèches contribute to the target. Look inspection and conversation surfaces show the exact score, condition drivers, and current recruitment susceptibility. NPC save records retain happiness, last evaluation, severe-deprivation duration, last pay, last faction change, and the readable cause summary while remaining backward-compatible with older saves.

Faction departure now requires extreme sustained neglect rather than one bad shift. A faction member must remain at 12 happiness or lower with at least three severe failures among food, water, pay, and a bed for seven continuous days before departure is possible; departure is guaranteed only after fourteen continuous days. The actor remains in the world as an unaffiliated resident. Rival recruitment is now an officer action inside the existing faction planning, execution, and cooldown lifecycle. Targets must be at 45 happiness or lower, and success chance rises with target unhappiness, destination-faction happiness, and officer standing. Recent transfers and full destination housing block the scheme; success transfers one named NPC and consumes destination population capacity.

Closed the population-integrity gaps found by milestone review. A shared strategic-family identity now governs distant ledgers, essential reserves, population capacity, reinforcement routing, and related subfactions. Existing duplicate family ledgers are collapsed before simulation, preventing doubled distant consequences. Faction population consumes finite food and water reserves once per world day, with immature crèche cohorts counted as additional mouths and double food demand. Neutral crèches no longer create faction children or grant every faction happiness; only operating faction-owned crèches support their strategic family.

Verification: Java 17 full-tree compile passed with the existing Netty unchecked note; the focused NPC happiness/faction recruitment smoke, Infopedia, essential supply, population pressure/origin, reinforcement lifecycle/source, crèche, and deferred-network smokes passed; expanded Gate 3 passed including the new smoke and large-world acceptance with zero unreachable rooms, alongside the existing campaign-save storage errors and production fallback warnings; package rebuild passed with 1805 class files and 6096 package assets; Java 17 package scan passed for 3610 classfiles, highest major version 61; package boot smoke stayed alive for 8 seconds; function and Mermaid maps refreshed at 763 Java modules and 0 unpositioned modules; repository manifest refreshed with 206773 indexed rows, reused=203103, hashed=3670, errors=0.

## Milestone 04 - Deferred Faction Networks and Explainable Resolution Slice

Completed Phases 16.6 and 16.7 as one strategic simulation slice. Each participating faction family now receives one compact persisted distant-network ledger instead of full remote actor, room, machine, and item ticking. The ledger observes faction strength, influence, wealth, population pressure, losses, reinforcement demand, supplier reliability, route safety, shipment pressure, warehouse vulnerability, raw materials, machine reliability, product quality, efficiency, import/export capacity, rival interference, leadership, schemes, top-down event pressure, and player disruption. Related subfactions share one family ledger so a shipment or reinforcement cannot receive the same distant consequence twice.

Networks resolve at bounded variable six-hour review windows. Each outcome records its support score, pressure score, full contributing factors, deterministic roll, success chance, focus, result, and next review turn. Shipment outcomes advance or delay existing incoming manifests; reinforcement outcomes advance or delay existing replacement arrivals; production outcomes replenish or disrupt linked raw-material reserves; and political outcomes shift persistent strength, influence, wealth, and rival pressure. Import-node inspection and the player-rank `distant_network` command expose the factors and latest result, while save/load preserves the ledger, timer, roll, causes, and outcome history.

Verification: Java 17 full-tree compile passed with the existing Netty unchecked note; the focused deferred-network smoke plus import-node, shipment, reinforcement-source, reinforcement-lifecycle, and Infopedia smokes passed; final expanded Gate 3 passed including the new smoke and large-world acceptance with zero unreachable rooms, alongside the existing campaign-save and production-fallback warnings; package rebuild passed with 1798 class files and 6096 package assets; Java 17 package scan passed for 3596 classfiles, highest major version 61; package boot smoke stayed alive for 8 seconds; function and Mermaid maps refreshed at 760 Java modules and 0 unpositioned modules; repository manifest and diff checks passed.

## Milestone 04 - Physical Import Node Generation Slice

Completed Phase 16.5 as a playable world-generation and logistics slice. Faction territory can now promote an appropriate receiving room and place a persistent physical import marker for sector exchanges, rail cargo stations, freight elevators, service lifts, customs checkpoints, road gates, air or void cargo docks, noble private imports, smuggling entries, and sewer freight hoists. The default neutral depot on Floor 5 Zone 2,2 is generated through the same data-driven node family instead of a one-off map exception. Generation is deterministic and idempotent, and every promoted receiving room contributes import-intake capacity to the faction facility roster.

External shipment manifests now bind to the exact generated marker used for arrival instead of stopping at an abstract room label. Free slow train reinforcements use that same faction node, preserve the node in their personnel provenance, and materialize beside it. Looking at or directly interacting with the marker reports current cargo and personnel traffic, consumes one turn on interaction, and remains coherent after save/load.

Verification: Java 17 full-tree compile passed with the existing Netty unchecked note; the focused import-node smoke plus shipment, reinforcement-source, reinforcement-lifecycle, faction-facility, and Infopedia smokes passed; expanded Gate 3 passed including large-world acceptance with zero unreachable rooms and the existing campaign-save and production-fallback warnings; package rebuild passed with 1793 class files and 6096 package assets; Java 17 package scan passed for 3586 classfiles, highest major version 61; package boot smoke stayed alive for 8 seconds; function and Mermaid maps refreshed at 758 Java modules and 0 unpositioned modules; repository manifest and diff checks passed.

## Milestone 04 - Faction Facilities and Critical Vendor Generation Slice

Completed Phases 16.1 through 16.4 as one player-visible generation slice. Controlled faction territory now receives physical, staffed market access suited to its identity: provisions for every established faction, Guard and Civic Wardens armories, faction dispensaries, Mechanist and civilian industrial-blueprint factors, animal-supply traders where agriculture exists, noble luxury brokers, and illicit black-market dealers. Their category remit guarantees the critical stock named by the facility instead of relying on a random trader roll. Room and machine blueprint folios and vehicle-service component crates are now concrete carried trade goods, and the previously referenced Guard Light Rifle is catalog-backed.

Appropriate controlled rooms are promoted before room fixtures are selected. Gang and illicit territory can become a concealed narcotics chem kitchen with a chemical fixture, production ledger, chem cook, and dealer. Noble territory can become a guarded luxury and draught vault whose exact room is used by protected draught custody while ordinary estate luxuries remain broker stock. Hiver, lower-hive, mutant, and noble territory can become a hydroponic, mushroom, garden, kennel, or animal-care facility with food/bio fixture, handler, feed, water, cleaning, and veterinary support. Promotions and vendor categories are idempotent, and specialist NPC roles survive save/load.

The slice also fixed two shared economy defects found through the integrated gameplay smoke: vendor placement now reserves an unoccupied floor tile instead of overwriting a room fixture, and canonical item names beginning with `Noble` take precedence over the same word as an item-quality prefix, preserving catalog access and Common pricing.

Verification: Java 17 full-tree compile passed with the existing Netty unchecked note; the focused five-territory facility/vendor smoke plus noble-draught, animal/agriculture, security, medical, population-demand, raw-material, shipment, and Infopedia smokes passed; expanded Gate 3 passed including large-world acceptance with zero unreachable rooms and the existing production fallback warnings; package rebuild passed with 1789 class files and 6096 package assets; Java 17 package scan passed for 3578 classfiles, highest major version 61; package boot smoke stayed alive for 8 seconds; function and Mermaid maps refreshed at 756 Java modules and 0 unpositioned modules; repository manifest refreshed with 204896 indexed rows and zero hash errors; diff check passed.

## Milestone 04 - Incoming Shipment and Import Lifecycle Slice

Completed a playable Phase 9.8 shipment pass and closed the Phase 9 provenance sequence. External trader stock now receives a durable manifest recording source faction or supplier, source site, destination faction and facility, arrival node, cargo manifest and value, legal/restricted/illicit status, quality or contamination/counterfeit risk, event modifier, interception and delay risk, departure and arrival window, operational mode, player visibility, route, linked finite reserve, and delivery sequence. External stock-movement ledgers can also create abstract operational manifests, allowing distant factories, mines, and merchants to support the local economy without full local simulation.

Off-map procurement is faction-specific and finite. Military, noble, black-market, Mechanist, civilian-merchant, and irregular salvage routes each define their own base cooldown, wide deterministic positive/negative variance, procurement-cost multiplier, and fixed route cost. Every manifest persists total source cost, landed unit-cost floor, effective cooldown, signed variance, and exact next-source turn. Landed cost places a floor under the player purchase price. Even if the linked reserve refills, delivered off-map cargo cannot create a replacement shipment before that faction source cooldown expires.

Shipment state now governs shelf availability. Arrived cargo is purchasable, delayed cargo remains withheld across reopened traders until its arrival window opens, and intercepted cargo never appears as delivered stock. A successful purchase consumes one exact unit from both the shipment manifest and its linked finite supply reserve; failed purchases consume neither stock nor time. Delivery, depletion, route, legality, risk, arrival window, procurement cost, cooldown variance, next-source turn, operational/abstract status, and player visibility survive save/load. Added explicit coverage for normal industrial imports, customs delay and eventual release, piracy interception, gang black-market freight, counterfeit controlled living samples, distant ledger-only cargo, cooldown withholding, and post-cooldown replacement shipment creation.

Verification: Java 17 full-tree compile passed with the existing Netty unchecked note; shipment, raw-material, animal/agriculture, essential, vertical-floor, security, medical, noble-draught, population-market, and Infopedia smokes passed; expanded Gate 3 passed including large-world acceptance with zero unreachable rooms and the existing production fallback warnings; package rebuild passed with 1782 class files and 6096 package assets; Java 17 package scan passed for 3564 classfiles, highest major version 61; package boot smoke stayed alive for 8 seconds; function and Mermaid maps refreshed at 753 Java modules and 0 unpositioned modules; repository manifest and diff checks passed.

## Milestone 04 - Raw Material Source and Fallback Provenance Slice

Completed a playable Phase 9.7 raw-material pass. Raw earth, quarried stone aggregate, ferric scrap, recovered industrial salvage, waste biomass, and refined metal stock now use independent finite persisted reserves. Sources resolve to compatible local mining, quarrying, salvage, recycling, scavenging, production output, stock movement, faction facility stockpiles, world-event relief or seizure, outside-sector rail freight, or a bounded faction reserve. Noble, military, black-market, and ordinary merchant imports retain distinct supplier classes; rival-controlled rooms and ledgers are rejected.

Generic unmatched history no longer masquerades as a source. When an existing production or trade chain requests material before its extraction chain is fully simulated, the system permits only a one-unit assumed faction reserve and records why it was allowed, the supplying faction, local/external status, event modifier, route, and a visible source-review requirement. Blockades close outside routes. Failed purchases consume nothing, successful purchases consume one exact unit, independent materials deplete separately, and depletion survives reopened vendors and save/load.

Verification: Java 17 full-tree compile passed with the existing Netty unchecked note; raw-material, animal/agriculture, essential, vertical-floor, security, medical, noble-draught, population-market, and Infopedia smokes passed; expanded Gate 3 passed including large-world acceptance with zero unreachable rooms and the existing production fallback warnings; package rebuild passed with 1778 class files and 6096 package assets; Java 17 package scan passed for 3556 classfiles, highest major version 61; package boot smoke stayed alive for 8 seconds; function and Mermaid maps refreshed at 751 Java modules and 0 unpositioned modules; repository manifest refreshed with 204837 indexed rows and zero hash errors; diff check passed.

## Milestone 04 - Pet, Animal, and Agricultural Goods Provenance Slice

Completed a playable Phase 9.6 animal and agriculture pass. NPC pet, animal-handler, farm, garden, fungus, cloning, and agricultural-freight markets now expose finite persisted reserves rather than isolated themed stock. Added concrete feed, farm-animal product, pet-care, veterinary, and cloning-sample goods alongside existing seed and fungus stock. Each offer resolves to compatible faction production, an operated room, a living farm/pet/kennel actor, or an open inspected rail import; rival-owned rooms, production, and animals cannot supply another faction's shelf.

Animal-linked stock preserves the exact animal, breeder or owner ledger, pen owner, handler, feed source, water station, veterinary or care source, facility, route, and remaining quantity. Disease screening and feed shortages visibly ration local animal output, while import restrictions close purely imported seed, breeding, and cloning stock without inventing a replacement source. Failed purchases consume nothing; successful purchases consume one exact reserve unit; independent depletion survives reopened vendors and save/load.

Verification: covered by the shared compile, focused economy suite, Gate 3, package, classfile, boot, map, and manifest loop recorded in the preceding raw-material slice.

## Milestone 04 - Noble Luxury and Protected Draught Provenance Slice

Completed a playable Phase 9.5 luxury and draught pass. Ordinary noble luxuries now use independent finite reserves tied to faction-controlled estate production, vault or store rooms, event-diverted intake, off-world merchant freight, or household reserves. Their provenance preserves house control, source facility and route, prestige, gifting, bargaining or household purpose, and blockade, tax, seizure, or tithe effects. Failed purchases consume nothing; successful purchases consume one exact reserve unit; depletion survives reopened traders and save/load.

Rare campaign draughts are no longer ordinary medical or luxury shelf goods. Their Common catalog value now has an 850-script floor. A noble vault may hold one protected draught while the trader explicitly withholds it from sale. Durable custody records preserve noble-house owner, including House Ashbourne where named; off-world origin; broker, smuggler, physician, merchant, or disputed custodian; exact import and vault route; quantity; genuine, diluted, counterfeit, contaminated, stolen, misdeclared, or house-certified identity; blockade, tax, seizure, or tithe status; and household use, prestige, gifting, bargaining, blackmail, medical privilege, private indulgence, inheritance, or hoarding purpose.

Only an explicit theft, smuggling, black-market, bargaining, or sale event can release one exceptional draught unit. That offer carries the custody chain and extreme value into trade and item provenance, consumes the sole held unit on purchase, and closes its release when empty. Generic draught offers are removed even when another stock generator attempted to add them. Added focused coverage for protected Ashbourne custody, ordinary luxury depletion, event restrictions, save/load, exceptional stolen release, counterfeit identity, purpose priority, and rival-controlled luxury production rejection.

Verification: Java 17 full-tree compile passed with the existing Netty unchecked note; noble draught, medical, security, essential, vertical-floor, population-market, trade-readability, and Infopedia smokes passed; expanded Gate 3 passed including large-world acceptance with zero unreachable rooms and the existing production fallback warnings; package rebuild passed with 1770 class files and 6096 package assets; Java 17 package scan passed for 3540 classfiles, highest major version 61; package boot smoke stayed alive for 8 seconds.

## Milestone 04 - Medical and Pharmaceutical Supply Provenance Slice

Completed a playable Phase 9.4 medical-supply pass. NPC medical, stimulant, and narcotic offers now bind to exact persisted reserves sourced from faction-controlled clinics or laboratories, local clinics, noble private physicians, illicit producers and diversions, displaced-population relief intake, outside-sector rail shipments, or a bounded faction medical cabinet. Rival-controlled laboratory output cannot silently supply another faction. Blockades close outside pharmaceutical routes and leave a one-unit local reserve with a long refill.

Medical stock distinguishes legal clinic treatment, restricted service medicine, noble physician supply, black-market performance drugs, sump sedatives, disaster relief medicine, outside-sector pharmaceuticals, counterfeit batches, and contaminated batches. Provenance preserves source, route, legality, risk, and unsafe batch warnings. Stimulants name the existing strain and sleep-debt risk, while illicit sedatives name dependency and contamination risk without inventing an addiction mechanic. Failed purchases consume nothing; successful purchases consume exactly one reserve unit; treatment and drug categories deplete independently and survive reopened traders and save/load.

Verification: Java 17 full-tree compile passed with the existing Netty unchecked note; medical, security, essential, vertical-floor, population-pressure, population-origin, staffed faction-site, trade-readability, and Infopedia smokes passed; Gate 3 passed with the expanded semantic intent/district checks and existing production fallback warnings; package rebuild passed with 1765 class files and 6096 package assets; Java 17 package scan passed for 3530 classfiles, highest major version 61; package boot smoke stayed alive for 8 seconds.

## Milestone 04 - Weapons, Ammunition, and Security Supply Provenance Slice

Completed a playable Phase 9.3 security-supply pass. Every NPC-shelf weapon and ammunition offer now binds to its own finite persisted reserve. Faction policy selects representative stock for Concord military forces, Civic Wardens, Mechanist custodians, noble households, gangs, hidden cells, mutant or scavenger markets, and ordinary civilian defense. Each offer names its civilian, security, military, restricted, luxury, improvised, stolen, black-market, confiscated, recovered, surplus, outside-sector, counterfeit, defective, or blockade-restricted class together with its legality.

Supply resolves to faction-controlled arms production, armories, munition stores, workshops, evidence stores, theft or smuggling events, battlefield recovery, open rail shipments, or a small faction reserve. Rival-controlled production is rejected across enum-style and player-facing faction names. Confiscation and theft retain their event actor and route; a blockade closes outside arms freight and leaves only one local reserve. Failed purchases do not consume stock or time, successful purchases consume the exact reserve, and independently depleted weapons or ammunition remain unavailable when the trader is reopened or the world is saved and loaded.

Verification: covered by the shared compile, focused economy suite, Gate 3, package, classfile, and boot loop recorded in the following medical-supply slice.

## Milestone 04 - Finite Essential Supply and Vertical Sewer Trade Slice

Advanced Phase 9.2 by replacing population-created food and water with persisted finite reserves. A reserve now resolves in priority order to a matching production ledger, faction provisioning room such as a farm, hydroponics, kitchen, food store, recycler, or purifier, an outside-sector rail shipment, or a small emergency allotment. Shelf provenance names the stock class, source facility or route, current reserve, and refill turn. Successful purchases consume exactly one unit; failed purchases consume nothing; depleted categories disappear from the active shelf and remain depleted when the vendor is reopened or the world is saved and loaded. Local production refills fastest, room stores more slowly, rail shipments take longer, and emergency relief has the smallest capacity and longest timer.

Added a paired floor-and-sewer economy. Every inhabited floor can receive finite `Fertilizer` and `Chemical reagent bottle` stock processed from universal waste runoff in its sewer layer below. Surface provenance names the sewer reclamation and freight-lift route, while sewer provenance names runoff from the floor above and local settling/reclamation. Sewer markets create reciprocal demand by paying at least +20% for food, clean water, filters, tools, and supplies brought down from above, with a visible scarcity premium on those local shelf goods. Both vertical export reserves persist, deplete independently, refill on source-specific timers, and identify the paired floor in trade context.

Verification: Java 17 full-tree compile passed with the existing Netty unchecked note; essential supply provenance, vertical floor trade, population pressure, population origin, staffed faction-site, creche generation, and Infopedia smokes passed; Gate 3 player-facing text smoke suite passed with the existing worldgen/save and production fallback warnings; package rebuild passed with 1752 class files and 6096 package assets; Java 17 package scan passed for 3504 classfiles, highest major version 61; package boot smoke stayed alive for 8 seconds; function and Mermaid maps refreshed at 738 Java modules and 0 unpositioned modules.

## Milestone 04 - Crèche Care, Happiness, and Generational Recruitment Slice

Added operating crèches as a long-form population source rather than another short reinforcement timer. A planned crèche counts only when its room has at least 24 floor tiles, one care provider, food storage, water storage, one dense child-bed unit, and one teaching station; blocked rooms name each missing requirement and receive no happiness, cohort, or recruitment benefit. One care provider supports up to 12 children, each child-bed unit holds four, and annual newborn intake fills only open care and bed capacity. Added a qualifying Civic Faction Creche room profile and persisted the care-provider, food, water, bed, and teaching facts with each population ledger.

Operating crèches now contribute a faction-happiness ledger that scales from +3 for one building to a maximum +25 at ten operating crèches. Children remain aggregate cohorts tied to `birthWorldTurn`, age through the existing 365-day world calendar, and cannot be recruited before age 16. Representatives show building count, capped happiness, caregivers, child capacity, cohort age, turns to maturity, and expose `Muster Cohort`; a successful muster materializes at most six mature young adults with crèche upbringing and birth-source provenance while preserving younger cohorts.

Newborn origin is explicit. A small yearly abstract intake is recorded as abandoned-or-ward intake rather than unexplained population. Faction NPCs can also persist a pregnancy due world turn; when due, an operating same-faction crèche with open care accepts one newborn cohort retaining the parent NPC identity. Parent births are processed before abstract intake, and a full crèche leaves the due pregnancy waiting rather than deleting or inventing a child. Immature cohorts add extra growth-food, clean-water, and pediatric-care market pressure; that child-specific pressure ends at young adulthood. Cohorts, parent origin, remaining children, pregnancy due turns, building requirements, and faction happiness survive save/load.

Verification: Java 17 full-tree compile passed with the existing Netty unchecked note; crèche generation/maturity, reinforcement source, reinforcement lifecycle, population origin demand, population market, and Infopedia mechanics smokes passed; Gate 3 player-facing text smoke suite passed with the existing worldgen/save and production fallback warnings; package rebuild passed with 1743 class files and 6096 package assets; Java 17 package scan passed for 3486 classfiles, highest major version 61; package boot smoke stayed alive for 8 seconds.

## Milestone 04 - Reinforcement Source Cost and Timer Slice

Made casualty replacement speed and cost depend on an actual selected recruitment source. A faction with a linked rail intake defaults to a free reinforcement train with a slow 150-250 turn availability timer and a long intake window. A linked barracks or duty facility enables the fastest 35-65 turn reserve muster for a modest 6-script equipment and processing charge per person. Any viable local population roster can support paid local recruitment at 24 script per person with a medium 65-110 turn timer. Unsupported train and barracks routes cannot be selected merely by paying money.

Faction representatives now show the selected source, exact per-person price, infrastructure prerequisite, next availability turn, and carried script. `Change Source` cycles only currently supported methods, reroutes every open faction manifest as one group, resets its timer and expiry window, and spends one turn. `Receive Reinforcements` checks affordability before changing personnel or ledgers, charges only for people who actually arrive, and keeps train arrivals free. Source mode, prerequisite, price, timer, and expiry persist with each manifest; older replacement records retain their free legacy-roster behavior. Added a 1280x720 representative workflow covering all three methods, missing-building refusal, exact timer bands, source switching, insufficient-funds non-mutation, one-time payment, free train reception, provenance, and save-format persistence; aligned the population-market Infopedia entry.

Verification: Java 17 full-tree compile passed with the existing Netty unchecked note; reinforcement source policy, reinforcement lifecycle, and Infopedia mechanics smokes passed; Gate 3 player-facing text smoke suite passed with the existing worldgen/save and production fallback warnings; package rebuild passed with 1737 class files and 6096 package assets; Java 17 package scan passed for 3474 classfiles, highest major version 61; package boot smoke stayed alive for 8 seconds.

## Milestone 04 - Faction Reinforcement Lifecycle Slice

Advanced Phase 8.5 and 8.6 from a persisted but dormant replacement list into a live casualty and reinforcement loop. Runtime population ticks now capture dead faction actors exactly once, remove them from local occupancy, release their assigned workforce slot, record the loss, and open a replacement manifest with a semi-random availability turn plus a six-hour arrival window. Existing replacement saves remain readable, while new request, due, and expiry turns persist with the world.

Faction representatives now report whether replacement personnel are inbound, ready, delayed by an unavailable roster/import intake, blocked by staffed housing and faction-room capacity, or expired. `Receive Reinforcements` admits up to four ready personnel in one turn through a linked population ledger, consumes one reserve slot per arrival, creates full NPC actors only at reception, and preserves replacement, casualty, route, room, population-pool, and facility provenance. Early, route-blocked, and capacity-blocked attempts leave time and manifests unchanged; unreceived manifests expire after their stated window. Added 1280x720 dialogue coverage plus focused casualty, bulk-arrival, save/load, legacy migration, provenance, capacity, non-mutation, and expiration checks, and aligned the population-market Infopedia entry.

Verification: Java 17 full-tree compile passed with the existing Netty unchecked note; faction reinforcement lifecycle, population market, staffed faction-site market, and Infopedia mechanics smokes passed; Gate 3 player-facing text smoke suite passed with the existing worldgen/save and production fallback warnings; package rebuild passed with 1734 class files and 6096 package assets; Java 17 package scan passed for 3468 classfiles, highest major version 61; package boot smoke stayed alive for 8 seconds; function and Mermaid maps refreshed at 731 Java modules and 0 unpositioned modules; repository manifest refreshed after this entry; diff check passed.

## Milestone 04 - Staffed Faction Site and Finite Shelf Supply Slice

Connected local faction production sites to matching persisted population rosters. A local site's effective worker count now comes from assigned workers in same-faction or facility-matching ledgers; no matching local ledger preserves distant abstract workforce accounting, while a matched but unassigned roster pauses production and exports. Trade context states the assigned workforce or explains that the source site is unstaffed.

Closed the infinite site-export fallback. Zero-worker sites cannot produce, zero-stock sites cannot export, each shelf export decrements site stock exactly once, and depleted sites contribute no replacement item. Traceable site-produced goods now reach their linked faction trader even when they are outside that faction's generic seeded-stock allowlist, while catalog validation and production/facility provenance remain required. Added focused coverage for unstaffed production refusal, staffed hourly output, finite depletion, shelf provenance, workforce and stock persistence, and readable unstaffed/depleted reasons.

Verification: Java 17 full-tree compile passed with the existing Netty unchecked note; faction-site workforce, population-origin demand, population-market workflow, trade readability, and Infopedia mechanics smokes passed; Gate 3 player-facing text smoke suite passed with authoritative exit code 0 and existing worldgen/save plus production fallback warnings; package rebuild passed with 1729 class files and 6096 package assets; Java 17 package scan passed for 3458 classfiles, highest major version 61; package boot smoke stayed alive for 8 seconds; function and Mermaid maps refreshed at 729 Java modules and 0 unpositioned modules; repository manifest refreshed after this entry; diff check passed.

## Milestone 04 - Population Origin Demand Identity Slice

Extended population provenance so custody blocks, pilgrim lodging, contract-labor dormitories, and displaced or relief housing receive distinct source kinds and durable origin modes alongside rail intake, creches, barracks, noble households, forge rosters, gang housing, hidden congregations, mutant broods, and ordinary hab workers.

Market demand now uses that identity as well as population size. Duty and custody rosters raise ammunition and medical demand; industrial and transport labor raises tools and work-food demand; noble households raise luxury and private-care demand; gang and hidden populations raise weapon and illicit-market pressure; arrivals, pilgrims, and displaced populations raise immediate food, water, and medicine demand; ordinary households and creches raise everyday provisioning demand. Those drivers are named in the trade panel and can allocate matching fallback stock such as ammunition, tools, or noble delicacies. A focused smoke proves equal-sized populations now produce different category demand, stock, and readable explanations.

Verification: Java 17 full-tree compile, focused origin-demand and adjacent population-market/trade/Infopedia smokes, Gate 3, package/classfile/boot checks, function/Mermaid map refresh, repository manifest refresh, and diff check passed as recorded in the following staffed-site slice.

## Milestone 04 - Population-Backed Essential Market Pressure Slice

Started Milestone 04 by connecting persisted room-population ledgers to live NPC trader sessions. Local population capacity, assigned workforce, recorded losses, and facility-linked ledgers now create category demand for food, water, medicine, tools and components, and security goods. A specialist vendor serving a viable population allocates missing food and water stock, plus basic medicine when population or casualty pressure warrants it; allocated goods retain provenance naming resident demand and local vendor allocation.

Demand is compared with the vendor's visible offers and category-matching production-site output. The resulting surplus, balanced, tight, strained, or severe-shortage band applies a bounded adjustment to both purchase prices and player sale value after existing quality, world-difficulty, markup, discount, and defect rules. Trade detail names the population target, workforce, losses, facility support, strongest pressure, and selected offer's exact price adjustment. Added a 1280x720 trade render and live-purchase smoke covering high-versus-low population pricing, essential-stock allocation, provenance, ledger persistence format, displayed price parity, inventory transfer, script spending, and turn cost; added a Population and Market Pressure Infopedia entry.

Verification: Java 17 full-tree compile passed with the existing Netty unchecked note; population-market workflow, trade readability, streetwise appraisal, certified appraisal, and Infopedia mechanics smokes passed; Gate 3 player-facing text smoke suite passed with authoritative exit code 0 and existing worldgen/save plus production fallback warnings; package rebuild passed with 1725 class files and 6096 package assets; Java 17 package scan passed for 3450 classfiles, highest major version 61; package boot smoke stayed alive for 8 seconds; function and Mermaid maps refreshed at 726 Java modules and 0 unpositioned modules; repository manifest refreshed after this entry; diff check passed.

## Milestone 03 - Completion Checkpoint

Closed the ordered Production, Knowledge, Skills, and Item Quality milestone after the faction production-order workflow connected the final Phase 17 contract requirement to live world interaction. Existing Phase 18 and 19 coverage now spans owned knowledge, skill, blueprint, quality, mutation, batch, and provenance definitions; player-facing Infopedia references; durable save/load; separate Skill and Knowledge progression; meaningful capability unlocks; quality-aware manual and staffed production; machine, material, facility, tool, operator, fatigue, batch, legality, source, repair, and faction traces; production controls; contract acquisition and hand-in; Gate 3 text checks; package checks; and generated ownership maps.

Milestone 03 is therefore complete at its documented exit criteria without adding another audit chain. Medical, cybernetic, narcotic, vehicle, broader population-market, faction-reinforcement, deferred-simulation, and world-event depth remain in their ordered later milestones.

## Milestone 03 - Faction Production Order Gameplay Slice

Completed the playable Phase 17.3 production-contract loop. Faction representatives now advertise production work and expose `Take Work` beside `Turn In`; accepting work records one active order for that faction and spends one turn, while duplicate requests leave time and contract state unchanged. Baseline orders request a Serviceable machine part, higher standing can raise the standard to Fine, and the objective overlay names the minimum quality, production record, inspection, skill, knowledge, script, standing, and skill-XP terms.

Production hand-in now selects the actual qualifying carried unit instead of accepting any item-family match. Low-quality, untraced, and defect-flagged alternatives are rejected without mutation; a valid output must carry the existing production provenance, meet the recorded quality floor, and have passed its batch inspection. Success consumes only that unit and its provenance record, then pays script, standing, and skill XP through the shared turn-in action. Production-order quality and reward fields persist through a backward-compatible extension of the faction-contract save record. Added a 1280x720 dialogue/control smoke covering acquisition, duplicate refusal, save/load, each rejection mode, selective consumption, completion, and rewards; aligned contract objectives, Infopedia, audit metadata, and the combined trainer/representative service layout.

Verification: Java 17 full-tree compile passed with the existing Netty unchecked note; eleven focused production-order, contract turn-in, skill-proof, objective, dialogue, Infopedia, Character Skills, display, and quality-provenance smokes passed; Gate 3 player-facing text smoke suite passed with authoritative exit code 0 and existing worldgen/save plus production fallback warnings; package rebuild passed with 1720 class files and 6096 package assets; Java 17 package scan passed for 3440 classfiles, highest major version 61; package boot smoke stayed alive for 8 seconds; function and Mermaid maps refreshed at 724 Java modules and 0 unpositioned modules; repository manifest refreshed after this entry; diff check passed.

## Milestone 03 - Skill-Gated Faction Contract Turn-In Slice

Converted contract skill proof from a readability-only forecast into a live completion loop. Matching faction representatives now describe the first eligible active contract and expose `Turn In` in ordinary conversation. The action requires the exact carried proof item, rechecks every listed skill and knowledge requirement at hand-in time, and recognizes established faction families such as Arbiters and Civic Wardens.

Successful hand-in consumes exactly one proof item, completes the contract, grants its script payout and faction standing, writes readable feedback, and advances one turn. Missing items, skills, knowledge, faction authority, or active work leave inventory, contract state, rewards, standing, and time unchanged. Completion, payout, and standing survive save/load. Added a 1280x720 representative-dialogue render and end-to-end mutation/persistence smoke, routed the shared contract forecast through the same proof authority, and aligned contract Infopedia and audit coverage without exposing private contract or target identifiers.

Verification: Java 17 full-tree compile passed with the existing Netty unchecked note; nine focused contract turn-in, skill-proof, audit, objective, dialogue, Infopedia, and Character Skills smokes passed; Gate 3 player-facing text smoke suite passed with authoritative exit code 0 and existing worldgen/save plus production fallback warnings; package rebuild passed with 1717 class files and 6096 package assets; Java 17 package scan passed for 3434 classfiles, highest major version 61; package boot smoke stayed alive for 8 seconds; function and Mermaid maps refreshed at 722 Java modules and 0 unpositioned modules; repository manifest refreshed after this entry; diff check passed.

## Milestone 03 - In-Person Forge Trainer and Repair Capability Slice

Made trainer access a live world interaction instead of a dormant skill-gate type. Humanoid NPCs with readable forge-tutor, artificer, mechanic, machinist, engineer, or repair-master roles now advertise training in conversation and expose a `Train` action. Training opens the Character Skills tab on `Forge-Tutored Repair` with temporary in-person trainer access; leaving the panel removes that access while learned skills remain durable.

Added `Forge-Tutored Repair` as a trainer-gated Fabrication and Repair node with a concrete world effect: after the prerequisite and 45 XP spend, one machine part can restore a fully broken owned production machine directly to serviceable integrity instead of stopping below it. The repair preview names the trained result before spending the part and turn. Expanded the live Character-panel smoke across trainer discovery, rendered Train control, matching-node routing, temporary access cleanup, unlock spending, and the stronger repair preview; aligned skill/production Infopedia and definition-audit coverage.

Verification: Java 17 full-tree compile passed with the existing Netty unchecked note; skill-tree Character-panel/trainer smoke, skill readability/spending/access/stat/specialization/capability/definition smokes, machine repair workflow, repair provenance, conversation readability, character equipment/medical integration, and Infopedia mechanics smokes passed; Gate 3 player-facing text smoke suite passed with authoritative exit code 0 and existing worldgen/save plus production fallback warnings; package rebuild, Java 17 package scan, package boot smoke, function/Mermaid map refresh, repository manifest refresh, and diff check passed.

## Milestone 03 - Character Skills Tree Gameplay Surface Slice

Added `Skills` as a fourth Character panel tab with branch browsing, readable node rows, current XP, unlock state, cost, prerequisite, world-access requirement, stat requirement/effect, specialization consequence, capability, visible effect, and skill-versus-knowledge boundary. The panel selects branches and nodes directly, keeps unavailable nodes inspectable with exact blockers, spends XP through `Unlock`, and links to the Skill Progression reference.

Extracted `SkillTreePanelAuthority` for bounded player-facing rows and previews, then routed both the Character panel and `skill_unlock` command through one `GamePanel.unlockSkillNode` action. XP deduction, prerequisite/access/stat/specialization validation, stat effects, durable unlocked-node state, event feedback, and knowledge isolation therefore remain identical across UI and command routes. Added a 1280x720 live-render smoke covering controls, readable previews, sequential unlocks, blocked non-mutating attempts, XP spending, and knowledge separation; updated the skill audit and Infopedia from console-only wording to the shared gameplay surface.

Verification: Java 17 full-tree compile passed with the existing Netty unchecked note; Character Skills render/spending smoke and all existing skill-tree, persistence, access, stat, specialization, capability, definition, Character equipment/medical, and Infopedia smokes passed; Gate 3 player-facing text smoke suite passed with authoritative exit code 0 and existing worldgen/save plus production fallback warnings; package rebuild, Java 17 package scan, package boot smoke, function/Mermaid map refresh, repository manifest refresh, and diff check passed.

## Milestone 03 - Base Production Control Board Slice

Added a base-wide `Production` board to the ordinary crafting panel and staffed workbench navigation. The board pages through built production machines, prioritizes blocked and running queues, and shows the selected machine's readable job, worker, queue, current-run progress, material/output/no-room policies, and last blocker without exposing generated assignment keys.

The selected machine can now recover or change its worker, add or remove queued runs, cycle all blocker/output policies, clear remaining work, write machine-scoped Status or History readback, or open that machine's staffed Workbench setup without visiting every station separately. These controls reuse the existing staffing validation, bounded queue mutation, policy, status/history, and staffed-job authorities, so base-wide management does not create a second execution or persistence model. Added a focused smoke that renders the live board at 1280x720 and covers attention ordering, unfinished-site exclusion, readable details, worker recovery, queue and policy controls, status/history scoping, clear, and selected-machine Workbench routing; aligned Production Forecast and the production-quality definition audit.

Verification: Java 17 full-tree compile passed with the existing Netty unchecked note; production control board render/control smoke passed; background staffed production/policy/routing/save-load smoke passed with existing worldgen/save and production fallback warnings; production status, manual operation record, fatigue/workbench surface, production readability, Infopedia mechanics readability, and production quality definition smokes passed; Gate 3 player-facing text smoke suite passed with authoritative exit code 0 and existing worldgen/save plus production fallback warnings; package rebuild, Java 17 package scan, package boot smoke, function/Mermaid map refresh, repository manifest refresh, and diff check passed.

## Milestone 03 - Background Staffed Production and Queue Policy Slice

Converted non-manual staffed production from workbench-triggered instant execution into persistent background work, superseding the earlier direct `Crew Run` and `Run Ready` controls. Each configured machine now accumulates per-run progress as ordinary game turns pass, continues while the player leaves the workbench, completes through the existing input/output/wear/provenance/history authority, and pauses non-mutatingly on blockers. Progress and scheduler synchronization persist through save/load without granting accidental offline catch-up.

Added per-machine workbench controls for material shortages, output destination, and unavailable output space. Material policy can wait with the worker, release the worker while preserving the order, or cancel the remaining queue. Output can route to unlimited Base Storage, the nearest capacity-checked claimed-room faction container, or an interactable floor pile. No-room policy can wait, release, cancel, or fall back to the floor; Clear removes remaining runs and progress. The workbench forecast, machine status, staffing readback, Infopedia, and production quality audit expose the current policies, progress, and last blocker. Expanded the staffed-production smoke for away-from-workbench progress, save/load, wait/resume, release/cancel, floor fallback, and nearest-container routing.

Verification: Java 17 full-tree compile passed with the existing Netty unchecked note; staffed background production/policy/routing/save-load smoke passed; production status command smoke passed with existing staffed-production fallback warnings; manual production operation record smoke passed; production fatigue/workbench surface smoke passed; production readability smoke passed; Infopedia mechanics readability smoke passed; production quality definition smoke passed; extended base-object persistence guard passed; Gate 3 player-facing text smoke suite passed with authoritative exit code 0 and existing worldgen/save plus production fallback warnings; package rebuild passed with 1709 class files and 6096 package assets; Java 17 package scan passed for 3418 classfiles, highest major version 61; package boot smoke stayed alive for 8 seconds; function and Mermaid maps refreshed at 715 Java modules and 0 unpositioned modules; repository manifest refreshed after this entry.

## Milestone 03 - Workbench Staffed Production Queue Shift Slice

Made the configured staffed queue playable as a queue by adding `Run Ready` to the operated machine's `Staff Jobs` workbench. One action now completes each currently input-ready queued run, up to the existing 20-run limit, and stops at the first input, access, machine, apparatus, staffing, or other production blocker while preserving the blocked remainder.

Each completed run still passes through the existing generated-production authority and retains its own input consumption, output provenance, machine wear, queue decrement, and shared completion-history record. The event log receives one aggregate crew-shift result instead of up to twenty repetitive completion messages. Staffed execution now revalidates the specifically assigned recruit at run time, so a departed or invalid worker blocks non-mutatingly and another available recruit cannot silently substitute. Expanded the end-to-end staffed-production smoke for partial ready-queue execution, bounded history/event behavior, and stale-worker rejection; aligned Production Forecast and production quality audit text.

Verification: Java 17 full-tree compile passed with the existing Netty unchecked note; staffed production setup/ready-queue/save-load/execution smoke passed; production status command smoke passed with existing staffed-production fallback warnings; manual production operation record smoke passed; production fatigue/workbench surface smoke passed; production readability smoke passed; Infopedia mechanics readability smoke passed; production quality definition smoke passed; Gate 3 player-facing text smoke suite passed with authoritative exit code 0 and existing worldgen/save plus production fallback warnings; package rebuild passed with 1703 class files and 6096 package assets; Java 17 package scan passed for 3406 classfiles, highest major version 61; package boot smoke stayed alive for 8 seconds; function and Mermaid maps refreshed at 713 Java modules and 0 unpositioned modules; repository manifest refreshed after this entry.

## Milestone 03 - Workbench Staffed Production Setup Slice

Completed the machine workbench staffed-production loop by adding a `Staff Jobs` mode with compatible known generated-job browsing, category/readiness filters, paging, detailed assignment and queue forecasts, validated job assignment, next-valid-recruit assignment, bounded queue add/remove controls, and direct `Crew Run` execution. Manual recipes remain one action away and ordinary crafting remains unchanged.

The setup reuses `ControlledProductionJobAuthority` for job visibility and access/doctrine/machine/apparatus validation, `ManualStaffingAssignmentAuthority` for role/skill-validated worker assignment, persisted `BaseObject` assignment/worker/queue fields, and `StaffedProductionExecutionAuthority` for outcomes. Changing jobs clears the previous queue, worker reassignment preserves one-station ownership, and queue controls hold remaining runs within 0 to 20. Generated assignment keys are now delimiter-safe in the backward-compatible base-object save format, and player readbacks consistently show job names instead of raw `GENVAR::` keys. Expanded `Milestone03StaffedProductionExecutionSmoke` into an end-to-end setup, save/load, and execution workflow; aligned Production Forecast, Faction Personnel, staffing result wording, status/readiness labels, and production quality definition text.

Verification: Java 17 full-tree compile passed with the existing Netty unchecked note; staffed production setup/save-load/execution smoke passed; production status command smoke passed with existing staffed-production fallback warnings; manual production operation record smoke passed; production fatigue/workbench surface smoke passed; production readability smoke passed; Infopedia mechanics readability smoke passed; production quality definition smoke passed; Gate 3 player-facing text smoke suite passed with authoritative exit code 0 and existing worldgen/save plus production fallback warnings; package rebuild passed with 1702 class files and 6096 package assets; Java 17 package scan passed for 3404 classfiles, highest major version 61; package boot smoke stayed alive for 8 seconds; function and Mermaid maps refreshed at 713 Java modules and 0 unpositioned modules; repository manifest refreshed after this entry.

## Milestone 03 - Workbench Staffed Production Run Slice

Added a machine-bound `Crew Run` workbench action that executes exactly one already-assigned generated-production batch at the operated machine. Ordinary crafting keeps its `Build` action, while a configured staffed machine can now consume its existing queue through normal gameplay and immediately review the resulting output, queue decrement, provenance, and completion history.

The action delegates to `StaffedProductionExecutionAuthority`, preserving existing worker, assignment, access, knowledge, machine quality, apparatus, input, facility, wear, output, provenance, and history rules. Missing assignments, blockers, and empty queues log readable failures without consuming inputs, adding output, wearing a machine, decrementing another machine, or writing completion history. Expanded `Milestone03StaffedProductionExecutionSmoke` for workbench success, target isolation, queue exhaustion, and missing-assignment failure; aligned Production Forecast Infopedia plus production quality definition text.

Verification: Java 17 full-tree compile passed with the existing Netty unchecked note; staffed production execution/workbench smoke passed with existing staffed-production fallback warnings; production status command smoke passed with the same warnings; manual production operation record smoke passed; production fatigue/workbench surface smoke passed; Infopedia mechanics readability smoke passed; production quality definition smoke passed; Gate 3 player-facing text smoke suite passed with authoritative exit code 0 and existing worldgen/save plus production fallback warnings; package rebuild passed with 1702 class files and 6096 package assets; Java 17 package scan passed for 3404 classfiles, highest major version 61; package boot smoke stayed alive for 8 seconds; function and Mermaid maps refreshed at 713 Java modules and 0 unpositioned modules; repository manifest refreshed after this entry.

## Milestone 03 - Machine-Bound Workbench Recipe Surface Slice

Updated built base-machine interaction so both `Operate` and `Craft` enter the same machine-bound workbench. That workbench now lists only known recipes compatible with the operated machine, retains the current compatible selection when possible, and shows machine-specific guidance to operate another machine or learn a matching recipe when no recipes match.

This removes the misleading global-crafting route and unrelated-machine recipe rows before production begins without changing ordinary crafting-panel visibility, recipe formulas, compatibility rules, production execution, queue ownership, provenance, or save authority. Expanded `Milestone03ProductionFatiguePressureSmoke` for route parity, forge-versus-lab filtering, and empty-workbench guidance, and aligned Production Forecast Infopedia plus production quality definition text.

Verification: Java 17 full-tree compile passed with the existing Netty unchecked note; production fatigue/workbench surface smoke passed; production readability smoke passed; Infopedia mechanics readability smoke passed; production quality definition smoke passed; production status command smoke passed with existing staffed-production fallback warnings; manual production operation record smoke passed; Gate 3 player-facing text smoke suite passed with authoritative exit code 0 and existing worldgen/save plus production fallback warnings; package rebuild passed with 1702 class files and 6096 package assets; Java 17 package scan passed for 3404 classfiles, highest major version 61; package boot smoke stayed alive for 8 seconds; function and Mermaid maps refreshed at 713 Java modules and 0 unpositioned modules; repository manifest refreshed after this entry.

## Milestone 03 - Workbench Craft Machine Target Slice

Updated machine workbenches opened through `Operate` so their recipe forecast and manual `Craft` action use the machine being operated instead of silently falling back to the first compatible base machine. Recipe selection now preserves workbench mode, the detail title names the operated machine, and incompatible recipes fail before spending time, consuming inputs, producing output, or wearing either machine.

This aligns forecast and execution across machine compatibility, condition, quality, fatigue, wear, provenance, and completion history without changing ordinary crafting-panel machine selection, production formulas, queue ownership, staffed execution, or save authority. Expanded `Milestone03ProductionFatiguePressureSmoke` with two-forge targeting and incompatible-workbench cases, and aligned Production Forecast Infopedia plus production quality definition text.

Verification: Java 17 full-tree compile passed with the existing Netty unchecked note; production fatigue/workbench target smoke passed; production readability smoke passed; Infopedia mechanics readability smoke passed; production quality definition smoke passed; manual production operation record smoke passed; production machine provenance smoke passed; Gate 3 player-facing text smoke suite passed with authoritative exit code 0 and existing worldgen/save plus production fallback warnings; package rebuild passed with 1702 class files and 6096 package assets; Java 17 package scan passed for 3404 classfiles, highest major version 61; package boot smoke stayed alive for 8 seconds; function and Mermaid maps refreshed at 713 Java modules and 0 unpositioned modules; repository manifest refreshed after this entry.

## Milestone 03 - Machine-Scoped Production Status Slice

Updated workbench and direct machine-interaction `Status` readbacks so they now scope queue counts, live operations, latest completion, readiness, and completed-history count to the machine being operated. The crafting-panel `Status` action and `production_status` command remain base-wide summaries, while machine views no longer surface another station's queued run, latest completion, or base-machine snapshot.

This closes the cross-machine leakage left after selecting the active workbench machine without changing production execution, recipe assignment, material consumption, fatigue, quality, provenance, queue ownership, history persistence, or save authority. Expanded `Milestone03ProductionStatusCommandSmoke` with two simultaneous machines and aligned Production Forecast Infopedia plus production quality definition text.

Verification: Java 17 full-tree compile passed with the existing Netty unchecked note; machine-scoped production status smoke passed with existing staffed-production fallback warnings; manual production operation record smoke passed; Infopedia mechanics readability smoke passed; production quality definition smoke passed; Gate 3 player-facing text smoke suite passed with authoritative exit code 0 and existing worldgen/save plus production fallback warnings; package rebuild passed with 1702 class files and 6096 package assets; Java 17 package scan passed for 3404 classfiles, highest major version 61; package boot smoke stayed alive for 8 seconds; function and Mermaid maps refreshed at 713 Java modules and 0 unpositioned modules; repository manifest refreshed after this entry.

## Milestone 03 - Workbench Production Readback Slice

Updated the shared crafting/workbench production controls so `Status` and `History` stay global in the normal crafting panel but become active-machine readbacks when the surface was opened through `Operate`. A player working at a specific machine now gets selected-machine status and filtered production history from the workbench panel itself instead of falling back to the default/global readbacks.

This keeps the production readback lane aligned across crafting, workbench, and direct machine interaction without changing production math, recipe visibility, material consumption, fatigue, quality caps, batch appraisal, item provenance, staffed execution, save ownership, or queue execution authority. Expanded `Milestone03ProductionStatusCommandSmoke` and `Milestone03ManualProductionOperationRecordSmoke` to prove the workbench Status and History actions use the active machine, and aligned Production Forecast Infopedia plus production quality definition audit text.

Verification: Java 17 full-tree compile passed with the existing Netty unchecked note; production status command smoke passed with existing staffed-production fallback warnings; manual production operation record smoke passed; Infopedia mechanics readability smoke passed; production quality definition smoke passed; Gate 3 player-facing text smoke suite passed with explicit `JAVA_EXIT:0` and existing worldgen/save warnings plus production fallback warnings; package rebuild passed with 1702 class files and 6096 package assets; Java 17 package scan passed for 3404 classfiles, highest major version 61; package boot smoke stayed alive for 8 seconds; function and Mermaid maps refreshed at 713 Java modules and 0 unpositioned modules; repository manifest refreshed after this entry.

## Milestone 03 - Crafting Panel Production History Slice

Added a `History` action to the crafting panel beside `Status` so recent completed production records can be reviewed from the normal crafting surface. The action writes the same bounded global production-history readback used by `production_history` into the event log, while machine interaction History continues to filter records to the active machine.

This keeps the production readback lane available during ordinary crafting without changing production math, recipe visibility, material consumption, fatigue, quality caps, batch appraisal, item provenance, staffed execution, save ownership, or queue execution authority. Expanded `Milestone03ManualProductionOperationRecordSmoke` to prove the crafting-panel History action logs the recent global completion records and aligned Production Forecast Infopedia plus production quality definition audit text.

Verification: Java 17 full-tree compile passed with the existing Netty unchecked note; manual production operation record smoke passed; production status command smoke passed with existing staffed-production fallback warnings; Infopedia mechanics readability smoke passed; production quality definition smoke passed; Gate 3 player-facing text smoke suite passed with explicit `JAVA_EXIT:0` and existing worldgen/save warnings plus production fallback warnings; package rebuild passed with 1702 class files and 6096 package assets; Java 17 package scan passed for 3404 classfiles, highest major version 61; package boot smoke stayed alive for 8 seconds; function and Mermaid maps refreshed at 713 Java modules and 0 unpositioned modules; repository manifest refreshed after this entry.

## Milestone 03 - Machine Interaction Production History Slice

Added a `History` action beside `Status` on base-machine interaction panels so a player standing at a machine can review that machine's recent completed production records from the event log. The readback reuses the shared production-history formatter and filters records to the active interaction machine, while the `production_history` command keeps its existing global recent-history behavior.

This continues the production readback lane from console-only access into ordinary machine interaction without changing production math, recipe visibility, material consumption, fatigue, quality caps, batch appraisal, item provenance, staffed execution, save ownership, or queue execution authority. Expanded `Milestone03ManualProductionOperationRecordSmoke` to prove the interaction History action shows active-machine records and excludes another machine's completion record, and aligned Production Forecast Infopedia plus production quality definition audit text.

Verification: Java 17 full-tree compile passed with the existing Netty unchecked note; manual production operation record smoke passed; production status command smoke passed with existing staffed-production fallback warnings; Infopedia mechanics readability smoke passed; production quality definition smoke passed; Gate 3 player-facing text smoke suite passed with explicit `JAVA_EXIT:0` and existing worldgen/save warnings plus production fallback warnings; package rebuild passed with 1702 class files and 6096 package assets; Java 17 package scan passed for 3404 classfiles, highest major version 61; package boot smoke stayed alive for 8 seconds; function and Mermaid maps refreshed at 713 Java modules and 0 unpositioned modules; repository manifest refreshed after this entry.

## Milestone 03 - Machine Interaction Production Status Slice

Added a `Status` action to base-machine interaction panels so a player standing at a machine can print that machine's live production status directly into the event log. The readback now uses the active interaction machine as the selected machine while preserving the same bounded live queue, recent completion, staffed forecast, and base-machine snapshot wording used by `production_status` and the crafting panel.

This keeps the production readback lane moving from command-only access toward ordinary play surfaces without changing production math, recipe visibility, material consumption, fatigue, quality caps, batch appraisal, item provenance, staffed execution, save ownership, or queue execution authority. Expanded `Milestone03ProductionStatusCommandSmoke` to prove the machine interaction action reports the active machine even when another base machine is first in the default list, and aligned Production Forecast Infopedia plus production quality definition audit text.

Verification: Java 17 full-tree compile passed with the existing Netty unchecked note; production status command smoke passed with existing staffed-production fallback warnings; Infopedia mechanics readability smoke passed; production quality definition smoke passed; manual production operation record smoke passed; Gate 3 player-facing text smoke suite passed with explicit `JAVA_EXIT:0` and existing worldgen/save warnings plus production fallback warnings; package rebuild passed with 1702 class files and 6096 package assets; Java 17 package scan passed for 3404 classfiles, highest major version 61; package boot smoke stayed alive for 8 seconds; function and Mermaid maps refreshed at 713 Java modules and 0 unpositioned modules; repository manifest refreshed after this entry.

## Milestone 03 - Crafting Panel Production Status Slice

Added a compact `Status` action to the crafting panel so live production state is available from the normal Crafting surface as well as the console command. The action writes the same bounded status readback into the event log: live queue counts, selected-machine readiness, latest completed production, staffed-production forecast state, and base-machine snapshot.

This advances the production, knowledge, skill, and quality milestone lane by making the shared production status visible during ordinary crafting without changing production math, recipe visibility, material consumption, fatigue, quality caps, batch appraisal, item provenance, staffed execution, save ownership, or queue execution authority. Expanded `Milestone03ProductionStatusCommandSmoke` to cover the crafting-panel status action and aligned Production Forecast Infopedia plus production quality definition audit text.

Verification: Java 17 full-tree compile passed with the existing Netty unchecked note; production status command smoke passed with existing staffed-production fallback warnings; Infopedia mechanics readability smoke passed; production quality definition smoke passed; manual production operation record smoke passed; Gate 3 player-facing text smoke suite passed with explicit `JAVA_EXIT:0` and existing worldgen/save warnings plus production fallback warnings; package rebuild passed with 1702 class files and 6096 package assets; Java 17 package scan passed for 3404 classfiles, highest major version 61; package boot smoke stayed alive for 8 seconds; function and Mermaid maps refreshed at 713 Java modules and 0 unpositioned modules; repository manifest refreshed after this entry.

## Milestone 03 - Production Status Command Readback Slice

Added the player-rank `production_status` command so live production state can be checked without opening an internal bridge/audit surface. The readback now reports pending and active shared operations, completed-history count, assigned-machine count, selected-machine readiness, staffed-production forecast state, latest completed production guidance, and a pointer to `production_history` for completed records.

This turns the shared machine-operation status bridge into player-readable production wording while leaving production math, recipe assignment, material consumption, fatigue, quality caps, batch appraisal, item provenance, staffed execution, save ownership, and queue execution authority unchanged. Added `Milestone03ProductionStatusCommandSmoke`, wired it into Gate 3, and aligned Production Forecast Infopedia plus production quality definition audit text.

Verification: Java 17 full-tree compile passed with the existing Netty unchecked note; production status command smoke passed with existing staffed-production fallback warnings; manual production operation record smoke passed; production quality definition smoke passed; Infopedia mechanics readability smoke passed; staffed production execution smoke passed with existing fallback warnings; production readability smoke passed; Gate 3 player-facing text smoke suite passed with explicit `JAVA_EXIT:0` and existing worldgen/save warnings plus production fallback warnings; package rebuild passed with 1702 class files and 6096 package assets; Java 17 package scan passed for 3404 classfiles, highest major version 61; package boot smoke stayed alive for 8 seconds; function and Mermaid maps refreshed at 713 Java modules and 0 unpositioned modules; repository manifest refreshed after this entry.

## Milestone 03 - Production History Command Readback Slice

Added the player-rank `production_history [count 1-5]` command so recent completed production records can be reviewed from shared machine-operation history. The readback shows bounded recent records, target, actor, completion turn, and saved production-result details such as quality, main limiter, fatigue band, batch state, and defect risk without exposing legacy outcome-authority wording.

This turns the prior manual Craft operation-history persistence into an inspectable player surface while leaving production math, recipe visibility, material consumption, fatigue, quality caps, batch appraisal, item provenance, staffed production execution, save ownership, and queue execution authority unchanged. Expanded `Milestone03ManualProductionOperationRecordSmoke` for command registration, empty history, bad-count guidance, count limiting, full result readback, and player-rank access; aligned Production Forecast Infopedia and production quality definition audit text.

Verification: Java 17 full-tree compile passed with the existing Netty unchecked note; manual production operation record smoke passed; production quality definition smoke passed; Infopedia mechanics readability smoke passed; production readability smoke passed; production fatigue pressure smoke passed; Gate 3 player-facing text smoke suite passed with explicit `JAVA_EXIT:0` and existing worldgen/save warnings plus production fallback warnings; package rebuild passed with 1701 class files and 6096 package assets; Java 17 package scan passed for 3402 classfiles, highest major version 61; package boot smoke stayed alive for 8 seconds; function and Mermaid maps refreshed at 712 Java modules and 0 unpositioned modules; repository manifest refreshed after this entry.

## Milestone 03 - Manual Craft Operation History Readback Slice

Updated successful immediate manual Craft completion so the production-result readback is preserved in the shared machine-operation completion record as well as the event log. The saved operation history now carries final output quality, main quality limiter, fatigue band, batch state, and defect risk after the event log scrolls away.

This keeps manual Craft history useful for the production, knowledge, skill, and quality milestone lane without changing production math, recipe visibility, material consumption, machine wear, fatigue cost, quality caps, batch appraisal consequences, item provenance, staffed production, or operation queue ownership. Expanded `Milestone03ManualProductionOperationRecordSmoke` to cover the enriched completion record and save/restore history, and aligned the production quality definition audit plus Production Forecast Infopedia text.

Verification: Java 17 full-tree compile passed with the existing Netty unchecked note; manual production operation record smoke passed; production fatigue pressure smoke passed; production quality definition smoke passed; Infopedia mechanics readability smoke passed; production readability smoke passed; Gate 3 player-facing text smoke suite passed with existing worldgen/save warnings and production fallback warnings; package rebuild passed with 1701 class files and 6096 package assets; Java 17 package scan passed for 3402 classfiles, highest major version 61; package boot smoke stayed alive for 8 seconds; function and Mermaid maps refreshed at 712 Java modules and 0 unpositioned modules; repository manifest refreshed after this entry.

## Milestone 03 - Manual Craft Completion Readback Slice

Updated successful immediate manual Craft output so the event log now includes a production-result readback with final output quality, main quality limiter, fatigue band, batch state, and defect risk. This makes the post-craft feedback match the forecast/provenance systems instead of only saying that an item was crafted.

This advances the production, knowledge, skill, and quality milestone lane without changing production math, recipe visibility, material consumption, machine wear, fatigue cost, quality caps, batch appraisal consequences, item provenance, staffed production, or shared machine-operation history. Expanded `Milestone03ProductionFatiguePressureSmoke` to cover successful Craft readback and exhausted Craft failure staying non-mutating, and aligned the production quality definition audit plus Production Forecast Infopedia text.

Verification: Java 17 full-tree compile passed with the existing Netty unchecked note; production fatigue pressure smoke passed; production quality definition smoke passed; Infopedia mechanics readability smoke passed; production quality trace smoke passed; production readability smoke passed; Gate 3 player-facing text smoke suite passed with existing worldgen/save warnings and production fallback warnings; package rebuild passed with 1701 class files and 6096 package assets; Java 17 package scan passed for 3402 classfiles, highest major version 61; package boot smoke stayed alive for 8 seconds; function and Mermaid maps refreshed at 712 Java modules and 0 unpositioned modules; repository manifest refreshed after this entry.

## Milestone 03 - Staged Construction Interaction Live-Site Revalidation Slice

Updated the staged-construction interaction-panel `Work` and `Dismantle` actions so they also re-check that the selected staged site still exists in the live base object list before changing the world. A stale interaction selection now fails free with re-open guidance instead of staging materials, adding labor, recovering components, removing the site, or spending a turn.

This closes the stale-reference side of staged construction interaction parity while preserving placement, reach checks, command targeting, work priority, material staging, held-tool labor multipliers, completion, valid adjacent dismantle recovery, status readback, faction construction jobs, heat, and room ownership. Expanded `Milestone03ProgressiveConstructionInteractionWorkSmoke` to cover stale-but-adjacent Work and Dismantle attempts, and aligned Construction Blueprints Infopedia plus progressive-construction definition text.

Verification: Java 17 full-tree compile passed with the existing Netty unchecked note; progressive construction interaction Work smoke passed; progressive construction definition/status smoke passed; Infopedia mechanics readability smoke passed; construction gameplay command smoke passed; Gate 3 player-facing text smoke suite passed with existing worldgen/save warnings and production fallback warnings; package rebuild passed with 1701 class files and 6096 package assets; Java 17 package scan passed for 3402 classfiles, highest major version 61; package boot smoke stayed alive for 8 seconds; function and Mermaid maps refreshed at 712 Java modules and 0 unpositioned modules; repository manifest refreshed after this entry.

## Milestone 03 - Staged Construction Interaction Reach Revalidation Slice

Updated the staged-construction interaction-panel `Work` and `Dismantle` actions so they re-check that the selected staged site is still adjacent before changing the world. A stale interaction selection now fails free with stand-adjacent guidance instead of staging materials, adding labor, recovering components, removing the site, or spending a turn from out of reach.

This closes the stale-panel side of staged construction interaction parity while preserving placement, command targeting, work priority, material staging, held-tool labor multipliers, completion, valid adjacent dismantle recovery, status readback, faction construction jobs, heat, and room ownership. Expanded `Milestone03ProgressiveConstructionInteractionWorkSmoke` to cover out-of-reach Work and Dismantle attempts, and aligned Construction Blueprints Infopedia plus progressive-construction definition text.

Verification: Java 17 full-tree compile passed with the existing Netty unchecked note; progressive construction interaction Work smoke passed; progressive construction definition/status smoke passed; Infopedia mechanics readability smoke passed; construction gameplay command smoke passed; Gate 3 player-facing text smoke suite passed with existing worldgen/save warnings and production fallback warnings; package rebuild passed with 1701 class files and 6096 package assets; Java 17 package scan passed for 3402 classfiles, highest major version 61; package boot smoke stayed alive for 8 seconds; function and Mermaid maps refreshed at 712 Java modules and 0 unpositioned modules; repository manifest refreshed after this entry.

## Milestone 03 - Staged Construction Interaction Work Blocked-Turn Slice

Updated the staged-construction interaction-panel `Work` action so it only spends a turn when the selected site actually changes. Clicking Work on a material-blocked staged site now reports that no progress was made and keeps the game clock unchanged, while material staging, labor progress, and completion still advance time as before.

This keeps the hands-on interaction path aligned with the player-rank `construction_work` command without changing placement, work range, material staging, held-tool labor multipliers, completion, dismantle recovery, status readback, faction construction jobs, heat, or room ownership. Added `Milestone03ProgressiveConstructionInteractionWorkSmoke`, wired it into Gate 3, and aligned the Construction Blueprints Infopedia and progressive-construction definition text.

Verification: Java 17 full-tree compile passed with the existing Netty unchecked note; progressive construction interaction Work smoke passed; progressive construction definition/status smoke passed; Infopedia mechanics readability smoke passed; construction gameplay command smoke passed; Gate 3 player-facing text smoke suite passed with existing worldgen/save warnings and production fallback warnings; package rebuild passed with 1701 class files and 6096 package assets; Java 17 package scan passed for 3402 classfiles, highest major version 61; package boot smoke stayed alive for 8 seconds; function and Mermaid maps refreshed at 712 Java modules and 0 unpositioned modules; repository manifest refreshed after this entry.

## Milestone 03 - Staged Construction Command Time-Cost Parity Slice

Updated the player-rank staged-construction command path so construction commands spend game turns when they actually change the world. `construction_work` now spends the productive work turns it uses after clamping the requested 1 to 20 turn range, respects held-tool labor multipliers, charges one turn for material-only staging, charges no time for blocked or out-of-reach attempts, and reports `Construction time spent` in the command result. `construction_dismantle` now spends one turn when it removes an unfinished staged site.

This closes a gameplay parity gap with the interaction panel: command-driven construction work and dismantling no longer grant free progress or free recovery while preserving the existing target priority, nearest-site guidance, material staging, labor contribution, completion, and dismantle recovery rules. Expanded the construction gameplay command smoke, progressive construction definition smoke, Infopedia mechanics smoke, and Construction Blueprints Infopedia wording.

Verification: Java 17 full-tree compile passed with the existing Netty unchecked note; construction gameplay command smoke passed; progressive construction definition/status smoke passed; Infopedia mechanics readability smoke passed; Gate 3 player-facing text smoke suite passed with existing worldgen/save warnings and production fallback warnings; function and Mermaid maps refreshed at 711 Java modules and 0 unpositioned modules; package rebuild passed with 1700 class files and 6096 package assets; Java 17 package scan passed for 3400 classfiles, highest major version 61; package boot smoke stayed alive for 8 seconds; repository manifest refreshed after this entry.

## Milestone 03 - Staged Construction Work Target Next-Action Slice

Updated the shared `construction_status` and `construction_progress` packet so the `Construction work target` line now repeats the target site's next action. Players can now see the adjacent site that `construction_work` will act on and whether that command will stage available materials, continue labor, or remain blocked before they spend turns.

This continues the live staged-construction readback lane without changing work range, target priority, material staging, labor contribution, turn clamping, completion, dismantle eligibility, recovery, room ownership, heat, or faction construction jobs. The implementation keeps the target choice inside `ProgressiveConstructionAuthority`, reuses the existing next-action wording, and expands the construction gameplay command smoke, progressive construction definition/status smoke, Infopedia mechanics smoke, and Construction Blueprints Infopedia wording.

Verification: Java 17 full-tree compile passed with the existing Netty unchecked note; construction gameplay command smoke passed; progressive construction definition/status smoke passed; Infopedia mechanics readability smoke passed; Gate 3 player-facing text smoke suite passed with existing worldgen/save warnings and production fallback warnings; function and Mermaid maps refreshed at 711 Java modules and 0 unpositioned modules; package rebuild passed with 1700 class files and 6096 package assets; Java 17 package scan passed for 3400 classfiles, highest major version 61; package boot smoke stayed alive for 8 seconds; repository manifest refresh wrote 206534 indexed file rows with reused=203084, hashed=3450, errors=0.

## Milestone 03 - Staged Construction Work Target Readback Slice

Updated the shared `construction_status` and `construction_progress` packet so it now names the exact adjacent staged site that `construction_work` will act on. When no staged site is adjacent, the packet says no work target is in reach, asks the player to stand adjacent, and points to the nearest staged site with the same distance, direction, and next-action guidance used by the work command failure path.

This makes the live construction loop less ambiguous when several staged sites are waiting: players can see the command target before spending work turns, and `construction_work` now uses the same `ProgressiveConstructionAuthority` target helper as the status packet. The slice does not change work range, target priority, material staging, labor contribution, turn clamping, completion, dismantle eligibility, recovery, room ownership, heat, or faction construction jobs. Expanded the construction gameplay command smoke, progressive construction definition/status smoke, Infopedia mechanics smoke, and Construction Blueprints Infopedia wording.

Verification: Java 17 full-tree compile passed with the existing Netty unchecked note; construction gameplay command smoke passed; progressive construction definition/status smoke passed; Infopedia mechanics readability smoke passed; Gate 3 player-facing text smoke suite passed with existing worldgen/save warnings and production fallback warnings; function and Mermaid maps refreshed at 711 Java modules and 0 unpositioned modules; package rebuild passed with 1700 class files and 6096 package assets; Java 17 package scan passed for 3400 classfiles, highest major version 61; package boot smoke stayed alive for 8 seconds; repository manifest refresh wrote 206522 indexed file rows with reused=203072, hashed=3450, errors=0, followed by a post-history manifest pass with reused=206521, hashed=1, errors=0.

## Milestone 03 - Staged Construction Empty Dismantle Target Readback Slice

Updated the shared `construction_status` and `construction_progress` empty-state packet so when no staged construction sites are waiting, it now says there is no next construction action and also reports `Construction dismantle target: none.`

This keeps the empty status/progress surface consistent with the no-adjacent and command-failure paths without changing work eligibility, status priority, material staging, labor contribution, turn clamping, completion, dismantle eligibility, recovery, room ownership, heat, or faction construction jobs. Expanded the construction gameplay command smoke, progressive construction definition/status smoke, and Construction Blueprints Infopedia coverage.

Verification: Java 17 full-tree compile passed with the existing Netty unchecked note; construction gameplay command smoke passed; progressive construction definition/status smoke passed; progressive construction dismantle smoke passed; Infopedia mechanics readability smoke passed; Gate 3 player-facing text smoke suite passed with existing worldgen/save warnings and production fallback warnings; function and Mermaid maps refreshed at 711 Java modules and 0 unpositioned modules; package rebuild passed with 1700 class files and 6096 package assets; Java 17 package scan passed for 3400 classfiles, highest major version 61; package boot smoke stayed alive for 8 seconds; final repository manifest refresh wrote 206510 indexed file rows with reused=206509, hashed=1, errors=0.

## Milestone 03 - Staged Construction Work Help Target Guidance Slice

Updated the player-rank `construction_work` command help so it names the supported 1 to 20 turn range, states that work chooses the same adjacent staged-site priority as `construction_progress`, and says the command points to the nearest staged site when none are adjacent.

This makes the discoverable help surface match construction-work targeting and failure readback without changing work eligibility, target priority, material staging, labor contribution, turn clamping, completion, dismantle behavior, room ownership, heat, or faction construction jobs. Expanded the construction gameplay command smoke and Construction Blueprints Infopedia coverage.

Verification: Java 17 full-tree compile passed with the existing Netty unchecked note; construction gameplay command smoke passed; Infopedia mechanics readability smoke passed; progressive construction definition/status smoke passed; progressive construction dismantle smoke passed; Gate 3 player-facing text smoke suite passed with existing worldgen/save warnings; function and Mermaid maps refreshed at 711 Java modules and 0 unpositioned modules; package rebuild passed with 1700 class files and 6096 package assets; Java 17 package scan passed for 3400 classfiles, highest major version 61; package boot smoke stayed alive for 8 seconds; final repository manifest refresh wrote 206498 indexed file rows with reused=206497, hashed=1, errors=0.

## Milestone 03 - Staged Construction Command Help Dismantle Guidance Slice

Updated the player-rank command help for `construction_status`, `construction_progress`, and `construction_dismantle` so the help text now names dismantle target guidance, the least-complete adjacent dismantle rule, and nearest-site guidance when no staged site is adjacent.

This makes the discoverable help surface match the staged-construction status and command behavior without changing dismantle eligibility, least-complete adjacent targeting, recovery counts, tile restoration, lost labor progress, work target priority, material staging, completion rules, room ownership, heat, or faction construction jobs. Expanded the construction gameplay command smoke, progressive construction definition/status smoke, dismantle smoke, and Construction Blueprints Infopedia coverage.

Verification: Java 17 full-tree compile passed with the existing Netty unchecked note; construction gameplay command smoke passed; Infopedia mechanics readability smoke passed; progressive construction definition/status smoke passed; progressive construction dismantle smoke passed; Gate 3 player-facing text smoke suite passed with existing worldgen/save warnings; function and Mermaid maps refreshed at 711 Java modules and 0 unpositioned modules; package rebuild passed with 1700 class files and 6096 package assets; Java 17 package scan passed for 3400 classfiles, highest major version 61; package boot smoke stayed alive for 8 seconds; final repository manifest refresh wrote 206486 indexed file rows with reused=206485, hashed=1, errors=0.

## Milestone 03 - Staged Construction Dismantle Command No-Target Guidance Slice

Aligned the `construction_dismantle` failure readback with the status/progress no-target guidance: when staged sites exist but none are adjacent, the command now tells the player to stand adjacent to remove a staged site and points to the nearest staged site.

This keeps the command path and status preview path consistent without changing dismantle eligibility, least-complete adjacent targeting, recovery counts, tile restoration, lost labor progress, work target priority, material staging, completion rules, room ownership, heat, or faction construction jobs. Expanded the construction gameplay command smoke, progressive construction definition/status smoke, dismantle smoke, and Construction Blueprints Infopedia coverage.

Verification: Java 17 full-tree compile passed with the existing Netty unchecked note; progressive construction definition/status smoke passed; construction gameplay command smoke passed; progressive construction dismantle smoke passed; Infopedia mechanics readability smoke passed; Gate 3 player-facing text smoke suite passed with existing worldgen/save warnings; function and Mermaid maps refreshed at 711 Java modules and 0 unpositioned modules; package rebuild passed with 1700 class files and 6096 package assets; Java 17 package scan passed for 3400 classfiles, highest major version 61; package boot smoke stayed alive for 8 seconds; final repository manifest refresh wrote 206474 indexed file rows with reused=206473, hashed=1, errors=0.

## Milestone 03 - Staged Construction Dismantle No-Target Readback Slice

Improved staged-construction status readback so when active staged sites exist but none are adjacent, `construction_status` and `construction_progress` now say there is no dismantle target in reach, tell the player to stand adjacent, and point to the nearest staged site.

This closes the companion gap to the dismantle target preview without changing dismantle eligibility, least-complete adjacent targeting, recovery counts, tile restoration, lost labor progress, work target priority, material staging, completion rules, room ownership, heat, or faction construction jobs. Expanded the construction gameplay command smoke, progressive construction definition/status smoke, dismantle smoke, and Construction Blueprints Infopedia coverage.

Verification: Java 17 full-tree compile passed with the existing Netty unchecked note; progressive construction definition/status smoke passed; construction gameplay command smoke passed; progressive construction dismantle smoke passed; Infopedia mechanics readability smoke passed; Gate 3 player-facing text smoke suite passed with existing worldgen/save warnings; function and Mermaid maps refreshed at 711 Java modules and 0 unpositioned modules; package rebuild passed with 1700 class files and 6096 package assets; Java 17 package scan passed for 3400 classfiles, highest major version 61; package boot smoke stayed alive for 8 seconds; final repository manifest refresh wrote 206462 indexed file rows with reused=206461, hashed=1, errors=0.

## Milestone 03 - Staged Construction Dismantle Target Preview Slice

Added a staged-construction status readback line that previews the adjacent site `construction_dismantle` will remove, using the same least-complete adjacent target helper as the command itself.

This lets players check the dismantle target through `construction_status` or `construction_progress` before removing anything, while preserving dismantle reach, recovery counts, tile restoration, lost labor progress, work target priority, material staging, completion rules, room ownership, heat, and faction construction jobs. Expanded the construction gameplay command smoke, progressive construction definition/status smoke, dismantle smoke, and Construction Blueprints Infopedia coverage.

Verification: Java 17 full-tree compile passed with the existing Netty unchecked note; construction gameplay command smoke passed; progressive construction definition/status smoke passed; progressive construction dismantle smoke passed; Infopedia mechanics readability smoke passed; Gate 3 player-facing text smoke suite passed with existing worldgen/save warnings; function and Mermaid maps refreshed at 711 Java modules and 0 unpositioned modules; package rebuild passed with 1700 class files and 6096 package assets; Java 17 package scan passed for 3400 classfiles, highest major version 61; package boot smoke stayed alive for 8 seconds; final repository manifest refresh wrote 206450 indexed file rows with reused=206449, hashed=1, errors=0.

## Milestone 03 - Staged Construction Dismantle Target Priority Slice

Adjusted `construction_dismantle` targeting so when several unfinished staged sites are adjacent, the command prefers the least-complete adjacent site instead of reusing the work command's most-actionable priority.

This reduces accidental loss of nearly finished work while keeping dismantle adjacent-only and preserving recovery counts, tile restoration, lost labor progress, completion rules, work target priority, work reach, room ownership, heat, and faction construction jobs. Expanded the construction gameplay command smoke, progressive construction definition/status smoke, dismantle smoke, and Construction Blueprints Infopedia coverage.

Verification: Java 17 full-tree compile passed with the existing Netty unchecked note; construction gameplay command smoke passed; progressive construction definition/status smoke passed; progressive construction dismantle smoke passed; Infopedia mechanics readability smoke passed; Gate 3 player-facing text smoke suite passed with existing worldgen/save warnings; function and Mermaid maps refreshed at 711 Java modules and 0 unpositioned modules; package rebuild passed with 1700 class files and 6096 package assets; Java 17 package scan passed for 3400 classfiles, highest major version 61; package boot smoke stayed alive for 8 seconds; final repository manifest refresh wrote 206438 indexed file rows with reused=206437, hashed=1, errors=0.

## Milestone 03 - Staged Construction Progress Command Alias Slice

Added `construction_progress` as a player-rank gameplay command alias for the same staged-construction packet exposed by `construction_status`, matching the milestone/admin-command wording already used for construction progress readback.

This keeps construction feedback easier to discover without changing status packet content, work target priority, work reach, material staging, labor contribution, completion, dismantle behavior, room ownership, heat, or faction construction jobs. Stray arguments to `construction_status` and `construction_progress` now return command-specific usage instead of silently ignoring input. Expanded the construction gameplay command smoke, progressive construction definition/status smoke, and Construction Blueprints Infopedia coverage.

Verification: Java 17 full-tree compile passed with the existing Netty unchecked note; construction gameplay command smoke passed; progressive construction definition/status smoke passed; Infopedia mechanics readability smoke passed; Gate 3 player-facing text smoke suite passed with existing worldgen/save warnings; function and Mermaid maps refreshed at 711 Java modules and 0 unpositioned modules; package rebuild passed with 1700 class files and 6096 package assets; Java 17 package scan passed for 3400 classfiles, highest major version 61; package boot smoke stayed alive for 8 seconds; final repository manifest refresh wrote 206426 indexed file rows with reused=206425, hashed=1, errors=0.

## Milestone 03 - Staged Construction Dismantle Command Slice

Added a `construction_dismantle` gameplay command that dismantles an adjacent unfinished staged-construction site through the existing recovery rules used by the Dismantle interaction.

This makes staged-site cleanup available through the same player command surface as construction progress and work, while preserving adjacent-only eligibility, recovered material counts, tile restoration, lost labor progress, completion rules, work reach, target priority, room ownership, heat, and faction construction jobs. Failed dismantle attempts now fail closed with nearest-site guidance when staged construction exists out of reach. Expanded the construction gameplay command smoke, progressive construction definition/status smoke, dismantle smoke, and Construction Blueprints Infopedia coverage.

Verification: Java 17 full-tree compile passed with the existing Netty unchecked note; construction gameplay command smoke passed; progressive construction definition/status smoke passed; progressive construction dismantle smoke passed; Infopedia mechanics readability smoke passed; Gate 3 player-facing text smoke suite passed with existing worldgen/save warnings; function and Mermaid maps refreshed at 711 Java modules and 0 unpositioned modules; package rebuild passed with 1700 class files and 6096 package assets; Java 17 package scan passed for 3400 classfiles, highest major version 61; package boot smoke stayed alive for 8 seconds; final repository manifest refresh wrote 206414 indexed file rows with reused=206413, hashed=1, errors=0.

## Milestone 03 - Staged Construction Status Overflow Readback Slice

Improved staged-construction progress overflow feedback so when more than five waiting sites exist, the overflow line now names the next unlisted site, its map location, progress, and next action instead of only reporting that additional sites are hidden.

This keeps crowded staged-construction queues actionable without adding pagination or changing status priority, work target priority, work reach, material staging, labor contribution, completion, dismantle behavior, room ownership, heat, or faction construction jobs. Expanded the progressive construction definition/status smoke and Construction Blueprints Infopedia coverage.

Verification: Java 17 full-tree compile passed with the existing Netty unchecked note; focused progressive construction definition/status smoke passed; construction gameplay command smoke passed; Infopedia mechanics readability smoke passed; Gate 3 player-facing text smoke suite passed with existing worldgen/save warnings; function and Mermaid maps refreshed at 711 Java modules and 0 unpositioned modules; package rebuild passed with 1700 class files and 6096 package assets; Java 17 package scan passed for 3400 classfiles, highest major version 61; package boot smoke stayed alive for 8 seconds; final repository manifest refresh wrote 206402 indexed file rows with reused=206401, hashed=1, errors=0.

## Milestone 03 - Staged Construction Dismantle Location Readback Slice

Improved staged-construction dismantle feedback so removing an unfinished site now names the dismantled structure and its map location before listing recovered materials and lost labor progress.

This makes cancellation cleanup easier to connect to the map without changing dismantle eligibility, recovered material counts, tile restoration, labor loss, completion rules, work reach, target priority, placement validation, room ownership, heat, or faction construction jobs. Expanded the progressive construction dismantle smoke, progressive construction definition/status smoke, and Construction Blueprints Infopedia coverage.

Verification: Java 17 full-tree compile passed with the existing Netty unchecked note; focused progressive construction dismantle smoke passed; progressive construction definition/status smoke passed; Infopedia mechanics readability smoke passed; Gate 3 player-facing text smoke suite passed with existing worldgen/save warnings; function and Mermaid maps refreshed at 711 Java modules and 0 unpositioned modules; package rebuild passed with 1700 class files and 6096 package assets; Java 17 package scan passed for 3400 classfiles, highest major version 61; package boot smoke stayed alive for 8 seconds; final repository manifest refresh wrote 206390 indexed file rows with reused=206389, hashed=1, errors=0.

## Milestone 03 - Staged Construction Work Progress Result Readback Slice

Improved staged-construction work-result feedback so partial construction work names staged materials and labor separately, then repeats the staged site's location, progress, materials, labor, reach, and next action in the same player-facing result.

This fixes the shared interaction result path so material-only staging no longer claims labor was added, while mixed material/labor work and completion still report accurately. The slice does not change work reach, target priority, placement validation, material consumption, labor amount, completion rules, dismantle behavior, room ownership, heat, or faction construction jobs. Expanded the progressive construction definition/status smoke, construction gameplay command smoke, and Construction Blueprints Infopedia coverage.

Verification: Java 17 full-tree compile passed with the existing Netty unchecked note; focused progressive construction definition/status smoke passed; construction gameplay command smoke passed; Infopedia mechanics readability smoke passed; Gate 3 player-facing text smoke suite passed with existing worldgen/save warnings; function and Mermaid maps refreshed at 711 Java modules and 0 unpositioned modules; package rebuild passed with 1700 class files and 6096 package assets; Java 17 package scan passed for 3400 classfiles, highest major version 61; package boot smoke stayed alive for 8 seconds; final repository manifest refresh wrote 206378 indexed file rows with reused=206377, hashed=1, errors=0.

## Milestone 03 - Staged Construction Completion Location Readback Slice

Improved `/construction_work` completion feedback so finishing a staged site reports the completed structure name and its map location in the same player-facing result line.

This makes the final construction-work readback easier to connect to the finished map object without changing work reach, target priority, placement validation, material staging, labor contribution amount, completion rules, dismantle behavior, room ownership, heat, or faction construction jobs. Expanded the construction gameplay command smoke, progressive construction definition/status smoke, and Construction Blueprints Infopedia coverage.

Verification: Java 17 full-tree compile passed with the existing Netty unchecked note; focused construction gameplay command smoke passed; progressive construction definition/status smoke passed; Infopedia mechanics readability smoke passed; Gate 3 player-facing text smoke suite passed with existing worldgen/save warnings; function and Mermaid maps refreshed at 711 Java modules and 0 unpositioned modules; package rebuild passed with 1700 class files and 6096 package assets; Java 17 package scan passed for 3400 classfiles, highest major version 61; package boot smoke stayed alive for 8 seconds; final repository manifest refresh wrote 206366 indexed file rows with reused=206365, hashed=1, errors=0.

## Milestone 03 - Staged Construction Work Turn Count Guidance Slice

Improved `/construction_work [turns]` readback so the command help now names the supported `1-20` turn range, numeric values outside that range report the adjusted turn count, and too-many-argument usage returns the same range-aware syntax. Non-numeric turn input still fails closed with the existing integer guidance.

This keeps construction work predictable without changing work reach, target priority, placement validation, material staging, labor contribution rules, completion, dismantle behavior, room ownership, heat, or faction construction jobs. Expanded the construction gameplay command smoke and Construction Blueprints Infopedia coverage.

Verification: Java 17 full-tree compile passed with the existing Netty unchecked note; focused construction gameplay command smoke passed; progressive construction definition/status smoke passed; Infopedia mechanics readability smoke passed; Gate 3 player-facing text smoke suite passed with existing worldgen/save warnings; function and Mermaid maps refreshed at 711 Java modules and 0 unpositioned modules; package rebuild passed with 1700 class files and 6096 package assets; Java 17 package scan passed for 3400 classfiles, highest major version 61; package boot smoke stayed alive for 8 seconds; final repository manifest refresh wrote 206354 indexed file rows with reused=206353, hashed=1, errors=0.

## Milestone 03 - Staged Construction Directional Work Guidance Slice

Improved distant staged-site readback so `/construction_progress`, `/construction_status`, and failed `/construction_work` guidance now include a simple movement direction such as `move east/south` alongside the existing tile-distance text.

This makes the previous nearest-site and work-reach guidance easier to act on without changing work reach, target priority, placement validation, material staging, labor contribution, completion, dismantle behavior, room ownership, heat, or faction construction jobs. Expanded the progressive construction, construction gameplay command, and Construction Blueprints Infopedia coverage.

Verification: Java 17 full-tree compile passed with the existing Netty unchecked note; focused progressive construction definition/status smoke passed; construction gameplay command smoke passed; Infopedia mechanics readability smoke passed; Gate 3 player-facing text smoke suite passed with existing worldgen/save warnings; function and Mermaid maps refreshed at 711 Java modules and 0 unpositioned modules; package rebuild passed with 1700 class files and 6096 package assets; Java 17 package scan passed for 3400 classfiles, highest major version 61; package boot smoke stayed alive for 8 seconds; final repository manifest refresh wrote 206342 indexed file rows with reused=206341, hashed=1, errors=0.

## Milestone 03 - Staged Construction Out-of-Reach Work Guidance Slice

Improved `/construction_work` failure feedback when no staged construction site is adjacent. Instead of only telling the player to stand beside an unfinished site, the command now points to the nearest staged site, includes its existing status line, and keeps distance guidance in the same player-facing format used by `/construction_progress` and `/construction_status`.

This keeps staged construction readback actionable without changing work reach, target priority, placement validation, material staging, labor contribution, completion, dismantle behavior, room ownership, heat, or faction construction jobs. Expanded the progressive construction, construction gameplay command, and Construction Blueprints Infopedia coverage.

Verification: Java 17 full-tree compile passed with the existing Netty unchecked note; focused progressive construction definition/status smoke passed; construction gameplay command smoke passed; Infopedia mechanics readability smoke passed; Gate 3 player-facing text smoke suite passed with existing worldgen/save warnings; function and Mermaid maps refreshed at 711 Java modules and 0 unpositioned modules; package rebuild passed with 1700 class files and 6096 package assets; Java 17 package scan passed for 3400 classfiles, highest major version 61; package boot smoke stayed alive for 8 seconds; repository manifest refresh passed after docs and generated maps were updated.

## Milestone 03 - Staged Construction Aggregate Readiness Readback Slice

Expanded the staged-construction progress summary so `/construction_progress` and `/construction_status` now count material-ready blocked sites and staged sites already in work reach alongside the existing active, labor-ready, blocked, and nearly complete counts.

This lets the player see whether waiting construction can be acted on immediately before reading the individual site lines. The slice does not change construction placement, work range, target priority, material consumption, labor amount, completion, dismantle behavior, room ownership, heat, or faction construction jobs. Expanded the progressive construction, construction gameplay command, and Construction Blueprints Infopedia smokes.

Verification: Java 17 full-tree compile passed with the existing Netty unchecked note; focused progressive construction definition/status smoke passed; construction gameplay command smoke passed; Infopedia mechanics readability smoke passed; Gate 3 player-facing text smoke suite passed with existing worldgen/save warnings; function and Mermaid maps refreshed at 711 Java modules and 0 unpositioned modules; package rebuild passed with 1700 class files and 6096 package assets; Java 17 package scan passed for 3400 classfiles, highest major version 61; package boot smoke stayed alive for 8 seconds; repository manifest refresh wrote 206318 indexed file rows with reused=202869, hashed=3449, errors=0.

## Milestone 03 - Staged Construction Work Target Priority Slice

Aligned `/construction_work` target selection with the staged-construction progress packet. When multiple unfinished sites are adjacent, the work command now uses the same action-priority ranking as `/construction_progress`, so reachable labor-ready or material-ready sites are chosen ahead of less useful blocked sites before falling back to distance, progress, and map order.

This makes the command behavior match the player's readback: the site surfaced as most actionable is also the site worked first when it is in reach. The slice does not alter work range, placement validation, material consumption rules, labor amount, completion, dismantle behavior, room ownership, heat, or faction construction jobs. Expanded the progressive construction, construction gameplay command, and Construction Blueprints Infopedia smokes.

Verification: Java 17 full-tree compile passed with the existing Netty unchecked note; focused progressive construction definition/status smoke passed; construction gameplay command smoke passed; Infopedia mechanics readability smoke passed; Gate 3 player-facing text smoke suite passed with existing worldgen/save warnings; function and Mermaid maps refreshed at 711 Java modules and 0 unpositioned modules; package rebuild passed with 1700 class files and 6096 package assets; Java 17 package scan passed for 3400 classfiles, highest major version 61; package boot smoke stayed alive for 8 seconds; repository manifest refresh wrote 206306 indexed file rows with reused=202855, hashed=3451, errors=0.

## Milestone 03 - Staged Construction Work Reach Readback Slice

Improved staged-construction status lines by showing whether each waiting site is already within `/construction_work` reach or requires the player to stand adjacent first. The progress packet now keeps the existing action priority while using reach as a tie-breaker, so reachable material-ready sites surface ahead of equally blocked distant sites.

This keeps construction status actionable without changing construction mechanics: players can tell whether the next step is work now, stage available materials now, move adjacent, or go find missing inputs. The slice does not alter work range, placement validation, material consumption, labor contribution, completion, dismantle behavior, room ownership, heat, or faction construction jobs. Expanded the progressive construction, construction gameplay command, and Construction Blueprints Infopedia smokes.

Verification: Java 17 full-tree compile passed with the existing Netty unchecked note; focused progressive construction definition/status smoke passed; construction gameplay command smoke passed; Infopedia mechanics readability smoke passed; Gate 3 player-facing text smoke suite passed with existing worldgen/save warnings; function and Mermaid maps refreshed at 711 Java modules and 0 unpositioned modules; package rebuild passed with 1700 class files and 6096 package assets; Java 17 package scan passed for 3400 classfiles, highest major version 61; package boot smoke stayed alive for 8 seconds; repository manifest refresh wrote 206294 indexed file rows with reused=202844, hashed=3450, errors=0.

## Milestone 03 - Staged Construction Material Availability Readback Slice

Improved live staged-construction feedback by making construction status and work-result lines distinguish missing materials that are currently available to stage from materials still absent from base storage. `/construction_progress` now promotes material-ready blocked sites ahead of fully blocked sites, and each material-blocked site can say which available materials can be staged now before listing what remains missing.

This keeps the staged construction loop clearer after partial placement: players can see whether the next useful action is Work, stage available supplies/components, or go acquire missing inputs. The slice does not change material consumption, labor contribution, placement validation, completion, dismantle behavior, room ownership, heat, or faction construction jobs. Expanded the progressive construction, construction gameplay command, and Construction Blueprints Infopedia smokes.

Verification: Java 17 full-tree compile passed with the existing Netty unchecked note; focused progressive construction definition/status smoke passed; construction gameplay command smoke passed; Infopedia mechanics readability smoke passed; Gate 3 player-facing text smoke suite passed with existing worldgen/save warnings; function and Mermaid maps refreshed at 711 Java modules and 0 unpositioned modules; package rebuild passed with 1700 class files and 6096 package assets; Java 17 package scan passed for 3400 classfiles, highest major version 61; package boot smoke stayed alive for 8 seconds; repository manifest refresh wrote 206282 indexed file rows with reused=202831, hashed=3451, errors=0.

## Milestone 03 - Staged Construction Progress Priority Slice

Improved the live `/construction_progress` readback by prioritizing staged-site lines after the aggregate counts. Nearly complete sites now appear before other labor-ready work, and labor-ready work appears before material-blocked sites, with coordinates and names still providing stable ordering inside each priority.

This keeps staged construction feedback useful once multiple builds are waiting: the command now surfaces the next most actionable site instead of listing only by map position. The slice does not change placement validation, material consumption, labor contribution, completion, dismantle behavior, room ownership, heat, or faction construction jobs. Expanded the progressive construction smoke and Construction Blueprints Infopedia wording.

Verification: Java 17 full-tree compile passed with the existing Netty unchecked note; focused progressive construction definition/status smoke passed; Infopedia mechanics readability smoke passed; Gate 3 player-facing text smoke suite passed with existing worldgen/save warnings; function and Mermaid maps refreshed at 711 Java modules and 0 unpositioned modules; package rebuild passed with 1700 class files and 6096 package assets; Java 17 package scan passed for 3400 classfiles, highest major version 61; package boot smoke stayed alive for 8 seconds; final repository manifest refresh wrote 206270 indexed file rows with reused=206269, hashed=1, errors=0.

## Infopedia - Player-Facing Mechanic Reference Cleanup Slice

Audited the game-owned Infopedia mechanic entries and removed the development-history debris that had accumulated there: smoke names, guard lines, authority names, audit ownership notes, future-owner boundaries, raw ID warnings, and construction tally prose. The Infopedia now describes current player-facing behavior for assets, Look/Examine, movement, context prompts, menu behavior, input rebinding, body/medical/inventory, production, skill progression, construction blueprints, expansion heat, interaction approach, contract evidence, transfer workflows, and faction staffing.

This keeps the Infopedia as an in-game reference instead of a construction ledger. Development tracking remains here in `DEVELOPMENT_HISTORY.md`; the rendered Infopedia detail path and search matching now reject process-language mechanic lines, and the readability smokes sweep every rendered mechanic detail so stale guard/audit text fails fast.

Verification: Java 17 full-tree compile passed with the existing Netty unchecked note; focused Infopedia mechanics, menu uniformity Infopedia detail, input rebinding Infopedia detail, and contract skill-proof Infopedia smokes passed; Gate 3 player-facing text smoke suite passed with existing worldgen/save warnings; function and Mermaid maps refreshed at 710 Java modules and 0 unpositioned modules; package rebuild passed with 1699 class files and 6096 package assets; Java 17 package scan passed for 3398 classfiles, highest major version 61; package boot smoke stayed alive for 8 seconds; repository manifest refresh wrote 206255 indexed file rows with reused=202804, hashed=3451, errors=0.

## Milestone 03 - Staged Construction Status Packet Slice

Pivoted the construction milestone back onto live player-facing staged construction by making `/construction_progress` report a readable staged-site status packet instead of an internal audit blob. The packet now summarizes active staged sites, sites ready for labor, sites blocked by materials, nearly complete sites, and the first few staged-site lines with location, material progress, labor progress, and the next action.

This moves construction tracking toward usable runtime feedback without adding another meta-audit layer: the slice does not create faction job queues, mutate rooms, assign workers, spend budget, change heat, alter placement validation, or complete construction. It reuses `ProgressiveConstructionAuthority`, updates the Construction Blueprints Infopedia wording, and expands the progressive construction smoke to cover empty status, blocked material status, ready-for-labor status, and the admin command path.

Verification: Java 17 full-tree compile passed with the existing Netty unchecked note; focused progressive construction definition/status smoke passed; Infopedia mechanics readability smoke passed; Gate 3 player-facing text smoke suite passed with existing worldgen/save warnings; function and Mermaid maps refreshed at 710 Java modules and 0 unpositioned modules; package rebuild passed with 1699 class files and 6096 package assets; Java 17 package scan passed for 3398 classfiles, highest major version 61; package boot smoke stayed alive for 8 seconds; repository manifest refresh wrote 206241 indexed file rows with reused=202792, hashed=3449, errors=0.

## Runtime Hygiene - Startup Sound Effects Silence Slice

Removed the startup `boot` sound effect from the client boot path so repeated smoke-test/package launches no longer play a machine-start/beep cue before the game settles into its intro music flow. The old path fired `sounds.play("boot", options)` from the `GamePanel` constructor, registered `assets/sound/wav/machine_start.wav` as the `boot` cue, and could synthesize a generated fallback tone if the asset was missing.

This makes startup sound effects inert instead of merely hidden behind a setting: `GamePanel` no longer plays the boot cue, `SoundManager` no longer registers the boot asset, the audio runtime suppresses the `boot` key and returns no generated boot cue bytes, legacy `bootSound` options are forced false, the Options audio tab no longer exposes a boot-sound toggle, and the packaged options file now persists `bootSound=false`. Intro/dynamic music remains on its separate channel.

Verification: Java 17 full-tree compile passed with the existing Netty unchecked note; `BootStartupAudioSilenceSmoke`, `BootMenuMusicDelaySmoke`, and `OptionsSwingComponentSmoke` passed; Gate 3 player-facing text smoke suite passed with existing worldgen/save warnings; function and Mermaid maps refreshed at 710 Java modules and 0 unpositioned modules; package rebuild passed with 1699 class files and 6096 package assets; Java 17 package scan passed for 3398 classfiles, highest major version 61; package boot smoke stayed alive for 8 seconds; repository manifest refresh wrote 206228 indexed file rows with reused=202777, hashed=3451, errors=0.

## Milestone 03 - Blueprint Faction Construction Response Cycle Review Action Review Review Action Audit Slice

Advanced the construction parity lane by adding `BlueprintFactionConstructionResponseCycleReviewActionReviewReviewActionAuthority`, an audit-only action contract for future archived faction construction response cycle review-action review reviews. The audit requires review readiness, permission readiness, confirmation readiness, evidence selection, audit text, and non-reopen boundary before any future review-action owner can offer an archived review-action review review action.

This keeps faction construction non-mutating and action-separated: the slice does not execute review-action review review actions, reopen review-action reviews, write archives, export evidence, reveal hidden faction data, update status, enqueue notifications, alter job state, release reservations, or complete construction. Added `Milestone03BlueprintFactionConstructionResponseCycleReviewActionReviewReviewActionAuditSmoke`, expanded the Construction Blueprints Infopedia wording, and wired the guard into Gate 3.

Verification: Java 17 full-tree compile passed with the existing Netty unchecked note; focused response cycle review action review review action smoke passed; Infopedia mechanics readability smoke passed; Gate 3 player-facing text smoke suite passed with existing worldgen/save warnings; function and Mermaid maps refreshed at 709 Java modules and 0 unpositioned modules; package rebuild passed with 1698 class files and 6096 package assets; Java 17 package scan passed for 3396 classfiles, highest major version 61; package boot smoke stayed alive for 8 seconds; repository manifest refresh wrote 206212 indexed file rows with reused=202767, hashed=3445, errors=0.

## Milestone 03 - Blueprint Faction Construction Response Cycle Review Action Review Review Audit Slice

Advanced the construction parity lane by adding `BlueprintFactionConstructionResponseCycleReviewActionReviewReviewAuthority`, an audit-only review contract for future archived faction construction response cycle review-action review readbacks. The audit requires readback readiness, readable reviewer context, evidence links, readable allowed actions, privacy reminder, and follow-up boundary before any future review owner can offer review of an archived review-action review.

This keeps faction construction non-mutating and review-separated: the slice does not reopen review-action reviews, execute review-action review actions, write archives, reveal hidden faction data, update status, enqueue notifications, alter job state, move evidence, refresh archives, release reservations, or complete construction. Added `Milestone03BlueprintFactionConstructionResponseCycleReviewActionReviewReviewAuditSmoke`, expanded the Construction Blueprints Infopedia wording, and wired the guard into Gate 3.

Verification: Java 17 full-tree compile passed with the existing Netty unchecked note; focused response cycle review action review review smoke passed; Infopedia mechanics readability smoke passed; Gate 3 player-facing text smoke suite passed with existing worldgen/save warnings; function and Mermaid maps refreshed at 707 Java modules and 0 unpositioned modules; package rebuild passed with 1695 class files and 6096 package assets; Java 17 package scan passed for 3390 classfiles, highest major version 61; package boot smoke stayed alive for 8 seconds; repository manifest refresh wrote 206192 indexed file rows with reused=202753, hashed=3439, errors=0.

## Milestone 03 - Blueprint Faction Construction Response Cycle Review Action Review Readback Audit Slice

Advanced the construction parity lane by adding `BlueprintFactionConstructionResponseCycleReviewActionReviewReadbackAuthority`, an audit-only readback contract for future archived faction construction response cycle review-action reviews. The audit requires archive readiness, archive lookup readiness, readable summary, privacy filter readiness, readable replay reference, and stale record marker before any future readback owner can present an archived review-action review.

This keeps faction construction non-mutating and readback-separated: the slice does not read archives from storage, write archives, replay commands, reveal hidden faction data, update status, enqueue notifications, alter job state, move evidence, refresh archives, release reservations, or complete construction. Added `Milestone03BlueprintFactionConstructionResponseCycleReviewActionReviewReadbackAuditSmoke`, expanded the Construction Blueprints Infopedia wording, and wired the guard into Gate 3.

Verification: Java 17 full-tree compile passed with the existing Netty unchecked note; focused response cycle review action review readback smoke passed; Infopedia mechanics readability smoke passed; Gate 3 player-facing text smoke suite passed with existing worldgen/save warnings; function and Mermaid maps refreshed at 705 Java modules and 0 unpositioned modules; package rebuild passed with 1692 class files and 6096 package assets; Java 17 package scan passed for 3384 classfiles, highest major version 61; package boot smoke stayed alive for 8 seconds; repository manifest refresh wrote 206172 indexed file rows with reused=202739, hashed=3433, errors=0.

## Milestone 03 - Blueprint Faction Construction Response Cycle Review Action Review Archive Audit Slice

Advanced the construction parity lane by adding `BlueprintFactionConstructionResponseCycleReviewActionReviewArchiveAuthority`, an audit-only archive contract for future closed archived faction construction response cycle review-action reviews. The audit requires close readiness, readable archive reason, retention label, privacy label, result snapshot, and replay reference before any future archive owner can preserve a closed review-action review.

This keeps faction construction non-mutating and archive-separated: the slice does not write archives, redact records, delete records, update status, enqueue notifications, move evidence, refresh archives, reveal hidden faction data, alter job state, release reservations, or complete construction. Added `Milestone03BlueprintFactionConstructionResponseCycleReviewActionReviewArchiveAuditSmoke`, expanded the Construction Blueprints Infopedia wording, and wired the guard into Gate 3.

Verification: Java 17 full-tree compile passed with the existing Netty unchecked note; focused response cycle review action review archive smoke passed; Infopedia mechanics readability smoke passed; Gate 3 player-facing text smoke suite passed with existing worldgen/save warnings; function and Mermaid maps refreshed at 703 Java modules and 0 unpositioned modules; package rebuild passed with 1689 class files and 6096 package assets; Java 17 package scan passed for 3378 classfiles, highest major version 61; package boot smoke stayed alive for 8 seconds; repository manifest refresh wrote 206151 indexed file rows with reused=202720, hashed=3431, errors=0.

## Milestone 03 - Blueprint Faction Construction Response Cycle Review Action Review Close Audit Slice

Advanced the construction parity lane by adding `BlueprintFactionConstructionResponseCycleReviewActionReviewCloseAuthority`, an audit-only close contract for future archived faction construction response cycle review-action review follow-ups. The audit requires follow-up readiness, readable close decision, status return declaration, notification return declaration, evidence retention boundary, and archive refresh boundary before any future close owner can close archived review-action review follow-up.

This keeps faction construction non-mutating and close-separated: the slice does not close follow-up, update status, enqueue notifications, move evidence, refresh archives, reveal hidden faction data, alter job state, release reservations, or complete construction. Added `Milestone03BlueprintFactionConstructionResponseCycleReviewActionReviewCloseAuditSmoke`, expanded the Construction Blueprints Infopedia wording, and wired the guard into Gate 3.

Verification: Java 17 full-tree compile passed with the existing Netty unchecked note; focused response cycle review action review close smoke passed; Infopedia mechanics readability smoke passed; Gate 3 player-facing text smoke suite passed with existing worldgen/save warnings; function and Mermaid maps refreshed at 701 Java modules and 0 unpositioned modules; package rebuild passed with 1686 class files and 6096 package assets; Java 17 package scan passed for 3372 classfiles, highest major version 61; package boot smoke stayed alive for 8 seconds; repository manifest refresh wrote 206128 indexed file rows with reused=202706, hashed=3422, errors=0.

## Milestone 03 - Blueprint Faction Construction Response Cycle Review Action Review Follow-up Audit Slice

Advanced the construction parity lane by adding `BlueprintFactionConstructionResponseCycleReviewActionReviewFollowupAuthority`, an audit-only follow-up contract for future archived faction construction response cycle review-action review results. The audit requires result readiness, reviewer summary, evidence disposition, status refresh, notification boundary, and closure boundary before any future follow-up owner can schedule archived review-action review follow-up.

This keeps faction construction non-mutating and follow-up-separated: the slice does not schedule follow-up, write summaries, move evidence, update status, enqueue notifications, close review actions, reveal hidden faction data, alter job state, release reservations, or complete construction. Added `Milestone03BlueprintFactionConstructionResponseCycleReviewActionReviewFollowupAuditSmoke`, expanded the Construction Blueprints Infopedia wording, and wired the guard into Gate 3.

Verification: Java 17 full-tree compile passed with the existing Netty unchecked note; focused response cycle review action review follow-up smoke passed; Infopedia mechanics readability smoke passed; Gate 3 player-facing text smoke suite passed with existing worldgen/save warnings; function and Mermaid maps refreshed at 699 Java modules and 0 unpositioned modules; package rebuild passed with 1683 class files and 6096 package assets; Java 17 package scan passed for 3366 classfiles, highest major version 61; package boot smoke stayed alive for 8 seconds; repository manifest refresh wrote 206108 indexed file rows with reused=202692, hashed=3416, errors=0.

## Milestone 03 - Blueprint Faction Construction Response Cycle Review Action Review Result Audit Slice

Advanced the construction parity lane by adding `BlueprintFactionConstructionResponseCycleReviewActionReviewResultAuthority`, an audit-only result contract for future archived faction construction response cycle review-action review actions. The audit requires handoff readiness, readable command outcome, audit ledger readiness, rollback outcome, follow-up boundary, and notification boundary before any future result owner can record an archived review-action review result.

This keeps faction construction non-mutating and result-separated: the slice does not record result rows, execute review-action review actions, reopen review actions, write archives, export evidence, reveal hidden faction data, update status, alter job state, release reservations, or complete construction. Added `Milestone03BlueprintFactionConstructionResponseCycleReviewActionReviewResultAuditSmoke`, expanded the Construction Blueprints Infopedia wording, and wired the guard into Gate 3.

Verification: Java 17 full-tree compile passed with the existing Netty unchecked note; focused response cycle review action review result smoke passed; Infopedia mechanics readability smoke passed; Gate 3 player-facing text smoke suite passed with existing worldgen/save warnings; function and Mermaid maps refreshed at 697 Java modules and 0 unpositioned modules; package rebuild passed with 1680 class files and 6096 package assets; Java 17 package scan passed for 3360 classfiles, highest major version 61; package boot smoke stayed alive for 8 seconds; repository manifest refresh wrote 206088 indexed file rows with reused=202678, hashed=3410, errors=0.

## Milestone 02 - Construction Initial Placement Readback Slice

Added immediate placement readback when a construction blueprint is selected. Selecting a blueprint still arms placement at the player tile, but now the event log also records the initial construction placement target and its current ready/blocked/staged status before the player moves the cursor.

This keeps construction selection from feeling silent or ambiguous: players see the starting target coordinate and the first placement explanation right after choosing a blueprint, matching the readback they already get from mouse and keyboard placement movement. The change does not alter placement validation, material consumption, construction creation, or turn advancement.

Verification: Java 17 full-tree compile passed with the existing Netty unchecked note; focused construction keyboard, construction readability, blueprint ownership/permission readability, and progressive construction definition smokes passed; Gate 3 player-facing text smoke suite passed with existing worldgen/save warnings; function and Mermaid maps refreshed at 695 Java modules and 0 unpositioned modules; package rebuild passed with 1677 class files and 6096 package assets; Java 17 package scan passed for 3354 classfiles, highest major version 61; package boot smoke stayed alive for 8 seconds; repository manifest refresh wrote 206068 indexed file rows with reused=202666, hashed=3402, errors=0.

## Milestone 02 - Construction Keyboard Placement Feedback Slice

Repaired and clarified keyboard movement for active construction placement. Arrow and WASD placement movement now take priority while a blueprint is armed, update the build cursor through the same target path used by mouse placement, and write a construction placement target readback to the event log.

This is a live construction control slice: keyboard users now get the same target coordinate and ready/blocked/staged feedback that mouse users already received, and the construction panel control text now says placement movement includes status readback. The change does not alter placement validation, material consumption, construction creation, or turn advancement.

Verification: Java 17 full-tree compile passed with the existing Netty unchecked note; focused construction keyboard, construction readability, blueprint ownership/permission readability, and progressive construction definition smokes passed; Gate 3 player-facing text smoke suite passed with existing worldgen/save warnings; function and Mermaid maps refreshed at 695 Java modules and 0 unpositioned modules; package rebuild passed with 1677 class files and 6096 package assets; Java 17 package scan passed for 3354 classfiles, highest major version 61; package boot smoke stayed alive for 8 seconds; repository manifest refresh wrote 206055 indexed file rows with reused=202652, hashed=3403, errors=0.

## Milestone 02 - Construction Start Summary Slice

Threaded the construction preview consequences into the live placement confirmation log. When a blueprint is started, the event log now keeps a `Construction summary` with the placed blueprint, target tile, staged-material state, labor turns, mishap risk, and heat/suspicion bands.

This keeps the player-facing construction workflow coherent after commitment: the same labor and attention facts shown before placement remain visible after materials are staged and the build site is created. The slice is log/readability-only and does not change placement validation, material consumption, staged-site creation, or turn advancement.

Verification: Java 17 full-tree compile passed with the existing Netty unchecked note; focused construction readability, construction keyboard, blueprint ownership/permission readability, and progressive construction definition smokes passed; Gate 3 player-facing text smoke suite passed with existing worldgen/save warnings; function and Mermaid maps refreshed at 695 Java modules and 0 unpositioned modules; package rebuild passed with 1677 class files and 6096 package assets; Java 17 package scan passed for 3354 classfiles, highest major version 61; package boot smoke stayed alive for 8 seconds; repository manifest refresh wrote 206042 indexed file rows with reused=202639, hashed=3403, errors=0.

## Milestone 02 - Construction Effort Preview Slice

Threaded construction effort expectations into the live blueprint preview. Selected blueprints now show labor turns, required placement support, Mechanics and Intellect expectations, and mishap risk before the player commits placement.

This supports the construction milestone goal that blueprints answer what time, manual effort, tools, and skill pressure are involved before construction begins. The preview is advisory and player-facing; it does not change placement validation, material consumption, or construction completion rules.

Verification: Java 17 full-tree compile passed with the existing Netty unchecked note; focused construction readability, blueprint ownership/permission readability, and progressive construction definition smokes passed; Gate 3 player-facing text smoke suite passed with existing worldgen/save warnings; function and Mermaid maps refreshed at 695 Java modules and 0 unpositioned modules; package rebuild passed with 1677 class files and 6096 package assets; Java 17 package scan passed for 3354 classfiles, highest major version 61; package boot smoke stayed alive for 8 seconds; repository manifest refresh wrote 206029 indexed file rows with reused=202627, hashed=3402, errors=0.

## Milestone 02 - Construction Attention Preview Slice

Threaded blueprint heat and suspicion projections into the live construction preview. Selected blueprints now show an `Attention preview` line with readable heat and suspicion bands, numeric projected impacts, and driver summaries such as armed defenses, visible commerce, industrial footprint, legality risk, or ordinary footprint.

This moves the existing expansion-attention model into the player-facing construction workflow without mutating live heat or suspicion meters. It supports the construction milestone goal that blueprints answer what attention or suspicion may be generated before construction begins.

Verification: Java 17 full-tree compile passed with the existing Netty unchecked note; focused construction readability, blueprint expansion heat, and blueprint ownership/permission readability smokes passed; Gate 3 player-facing text smoke suite passed with existing worldgen/save warnings; function and Mermaid maps refreshed at 695 Java modules and 0 unpositioned modules; package rebuild passed with 1677 class files and 6096 package assets; Java 17 package scan passed for 3354 classfiles, highest major version 61; package boot smoke stayed alive for 8 seconds; repository manifest refresh wrote 206016 indexed file rows with reused=202614, hashed=3402, errors=0.

## Milestone 02 - Construction Blocked Placement Guidance Slice

Made blocked construction placement previews more actionable. The live construction detail panel still shows the exact placement status and blocker, but blocked previews now add a `Next step:` line using the existing player-facing construction denial guidance so players can see how to recover before they press confirm.

This advances the milestone requirement that denied construction produce useful player-facing explanations. Material shortfalls now point the player toward gathering listed materials, while other placement blockers continue through the shared sanitized guidance path used by construction denial logs.

Verification: Java 17 full-tree compile passed with the existing Netty unchecked note; focused construction readability and blueprint ownership/permission readability smokes passed; Gate 3 player-facing text smoke suite passed with existing worldgen/save warnings; function and Mermaid maps refreshed at 695 Java modules and 0 unpositioned modules; package rebuild passed with 1677 class files and 6096 package assets; Java 17 package scan passed for 3354 classfiles, highest major version 61; package boot smoke stayed alive for 8 seconds; repository manifest refresh wrote 206003 indexed file rows with reused=202601, hashed=3402, errors=0.

## Milestone 02 - Construction Selected Blueprint Marker Slice

Made active construction blueprint selection visible in the live blueprint list. The selected pending blueprint row now renders with the same `> ` marker used by other local list surfaces, so players can see which recipe is armed for placement after keyboard or mouse selection.

This is live construction UI behavior. The marker compares stable recipe names so equivalent recipe factory instances still highlight correctly, and the focused construction smoke now verifies selected and unselected row labels.

Verification: Java 17 full-tree compile passed with the existing Netty unchecked note; focused construction keyboard smoke passed with selected-row marker coverage; Gate 3 player-facing text smoke suite passed with existing worldgen/save warnings; function and Mermaid maps refreshed at 695 Java modules and 0 unpositioned modules; package rebuild passed with 1677 class files and 6096 package assets; Java 17 package scan passed for 3354 classfiles, highest major version 61; package boot smoke stayed alive for 8 seconds; repository manifest refresh wrote 205990 indexed file rows with reused=202588, hashed=3402, errors=0.

## Milestone 02 - Construction Reverse Category Navigation Slice

Extended the live construction blueprint list with reverse category navigation. Shift+Tab now cycles blueprint categories backward, and the construction footer exposes separate `Cat <` and `Cat >` buttons so mouse users can move both directions without wrapping through the full category list.

This is live construction UI behavior. Category changes still reset the blueprint page, and the focused construction smoke now verifies backward category wrapping before the existing forward category, page, selection, placement, and cancel coverage.

Verification: Java 17 full-tree compile passed with the existing Netty unchecked note; focused construction keyboard smoke passed with Shift+Tab reverse-category coverage; Gate 3 player-facing text smoke suite passed with existing worldgen/save warnings; function and Mermaid maps refreshed at 695 Java modules and 0 unpositioned modules; package rebuild passed with 1677 class files and 6096 package assets; Java 17 package scan passed for 3354 classfiles, highest major version 61; package boot smoke stayed alive for 8 seconds; repository manifest refresh wrote 205977 indexed file rows with reused=202574, hashed=3403, errors=0.

## Milestone 02 - Construction Blueprint Page Jump Slice

Extended the live construction blueprint list with first-page and last-page keyboard navigation. Home now jumps to the first visible construction blueprint page, and End jumps to the final page, matching the existing Page Up/Page Down paging flow and making long construction catalogs faster to scan.

This is live construction UI behavior. The construction detail pane now names the Home/End shortcuts, and the focused construction smoke verifies both page-jump directions before selecting from the active page.

Verification: Java 17 full-tree compile passed with the existing Netty unchecked note; focused construction keyboard smoke passed with Home/End page-jump coverage; Gate 3 player-facing text smoke suite passed with existing worldgen/save warnings; function and Mermaid maps refreshed at 695 Java modules and 0 unpositioned modules; package rebuild passed with 1677 class files and 6096 package assets; Java 17 package scan passed for 3354 classfiles, highest major version 61; package boot smoke stayed alive for 8 seconds; repository manifest refresh wrote 205964 indexed file rows with reused=202561, hashed=3403, errors=0.

## Milestone 02 - Construction Visible Shortcut Labels Slice

Made the live construction blueprint list advertise the shortcut keys it already supports. Visible build rows now render with `1.` through `9.` prefixes, and the tenth row renders as `0.`, matching the number-key and numpad selection mapping from the previous construction-control slice.

This keeps the construction panel player-facing: the shortcut is visible where the player chooses blueprints, and mouse row selection still routes through the same selection helper as keyboard selection.

Verification: Java 17 full-tree compile passed with the existing Netty unchecked note; focused construction keyboard smoke passed with visible shortcut-label coverage; Gate 3 player-facing text smoke suite passed with existing worldgen/save warnings; function and Mermaid maps refreshed at 695 Java modules and 0 unpositioned modules; package rebuild passed with 1677 class files and 6096 package assets; Java 17 package scan passed for 3354 classfiles, highest major version 61; package boot smoke stayed alive for 8 seconds; repository manifest refresh wrote 205951 indexed file rows with reused=202549, hashed=3402, errors=0.

## Milestone 02 - Construction Number-Key Blueprint Selection Slice

Continued the live construction-panel usability lane by letting number keys select blueprints from the visible construction page. Pressing 1 through 9 selects rows 1 through 9, and 0 selects row 10; numpad digits follow the same mapping. Selection now uses the same helper as clickable build rows, so mouse and keyboard entry both start placement at the player tile, set the pending recipe, and log the selected blueprint.

This is player-facing construction workflow development, not a future contract pass. The construction detail pane now names the number-key selection shortcut alongside category, page, placement, confirmation, and cancel controls.

Verification: Java 17 full-tree compile passed with the existing Netty unchecked note; focused construction keyboard smoke passed with number-key first-page and active-page selection coverage; Gate 3 player-facing text smoke suite passed with existing worldgen/save warnings; function and Mermaid maps refreshed at 695 Java modules and 0 unpositioned modules; package rebuild passed with 1677 class files and 6096 package assets; Java 17 package scan passed for 3354 classfiles, highest major version 61; package boot smoke stayed alive for 8 seconds; repository manifest refresh wrote 205938 indexed file rows with reused=202535, hashed=3403, errors=0.

## Milestone 02 - Construction Mouse Placement Slice

Extended the live construction panel placement workflow so mouse input can target and cancel active placements. While a build placement is active, left-clicking the world map moves the construction cursor to that tile and right-click cancels placement; the construction detail pane now names the mouse and keyboard controls together.

This is live player-facing UI behavior, not a future audit contract. The existing construction keyboard smoke now also verifies direct placement targeting, and Gate 3 runs the updated construction control coverage.

Verification: Java 17 full-tree compile passed with the existing Netty unchecked note; focused construction keyboard smoke passed; Gate 3 player-facing text smoke suite passed with existing worldgen/save warnings; function and Mermaid maps refreshed at 695 Java modules and 0 unpositioned modules; package rebuild passed with 1677 class files and 6096 package assets; Java 17 package scan passed for 3354 classfiles, highest major version 61; package boot smoke stayed alive for 8 seconds; repository manifest refresh wrote 205925 indexed file rows with reused=202514, hashed=3411, errors=0.

## Milestone 02 - Construction Keyboard Control Slice

Returned the active lane to player-facing implementation by improving the live construction panel controls. The construction detail pane now names the keyboard controls directly, and `GamePanelKeyController` supports `C` or Tab to cycle blueprint categories, Page Up/Page Down to move between blueprint pages, Escape to cancel an active placement, and Enter/Space as construction placement confirmation alongside `E`.

This is live UI behavior rather than a future audit contract: category/page shortcuts work before selecting a placement, placement cancellation clears the selected blueprint, and the existing construction forecast remains visible while placing. Added `Milestone02ConstructionKeyboardSmoke` and wired it into Gate 3.

Verification: Java 17 full-tree compile passed with the existing Netty unchecked note; focused construction keyboard smoke passed; construction readability and construction category smokes passed; Gate 3 player-facing text smoke suite passed with existing worldgen/save warnings; function and Mermaid maps refreshed at 695 Java modules and 0 unpositioned modules; package rebuild passed with 1677 class files and 6096 package assets; Java 17 package scan passed for 3354 classfiles, highest major version 61; package boot smoke stayed alive for 8 seconds; repository manifest refresh wrote 205904 indexed file rows with reused=202500, hashed=3404, errors=0.

## Milestone 03 - Blueprint Faction Construction Response Cycle Review Action Review Handoff Audit Slice

Added `BlueprintFactionConstructionResponseCycleReviewActionReviewHandoffAuthority` and `Milestone03BlueprintFactionConstructionResponseCycleReviewActionReviewHandoffAuditSmoke` to continue the archived faction-construction response-cycle review-action chain after review-action review action readiness. The new audit requires review-action readiness, target resolution, command owner, rollback preview, turn cost preview, and result text before any future archived review-action review action can be handed off.

This remains an audit-only contract: it does not hand off commands, execute review-action review actions, reopen review actions, write archives, export evidence, reveal hidden faction data, update status, alter job state, release reservations, or complete construction. The Infopedia construction-blueprints entry and Gate 3 suite now name the review-action review handoff boundary and guard.

Verification: Java 17 full-tree compile passed with the existing Netty unchecked note; focused response cycle review action review handoff smoke passed; Infopedia mechanics readability smoke passed; Gate 3 player-facing text smoke suite passed with existing worldgen/save warnings; function and Mermaid maps refreshed at 694 Java modules and 0 unpositioned modules; package rebuild passed with 1676 class files and 6096 package assets; Java 17 package scan passed for 3352 classfiles, highest major version 61; package boot smoke stayed alive for 8 seconds; repository manifest refresh wrote 205888 indexed file rows with reused=202485, hashed=3403, errors=0.

## Milestone 03 - Blueprint Faction Construction Response Cycle Review Action Review Action Audit Slice

Added `BlueprintFactionConstructionResponseCycleReviewActionReviewActionAuthority` and `Milestone03BlueprintFactionConstructionResponseCycleReviewActionReviewActionAuditSmoke` to continue the archived faction-construction response-cycle review-action chain after review readiness. The new audit requires review readiness, permission readiness, confirmation readiness, evidence selection, audit text, and non-reopen boundary before any future archived review-action review action can be offered.

This remains an audit-only contract: it does not execute review-action review actions, reopen review actions, write archives, export evidence, reveal hidden faction data, update status, enqueue notifications, alter job state, release reservations, or complete construction. The Infopedia construction-blueprints entry and Gate 3 suite now name the review-action action boundary and guard.

Verification: Java 17 full-tree compile passed with the existing Netty unchecked note; focused response cycle review action review action smoke passed; Infopedia mechanics readability smoke passed; Gate 3 player-facing text smoke suite passed with existing worldgen/save warnings; function and Mermaid maps refreshed at 692 Java modules and 0 unpositioned modules; package rebuild passed with 1673 class files and 6096 package assets; Java 17 package scan passed for 3346 classfiles, highest major version 61; package boot smoke stayed alive for 8 seconds; repository manifest refresh wrote 205867 indexed file rows with reused=202470, hashed=3397, errors=0.

## Milestone 03 - Blueprint Faction Construction Response Cycle Review Action Review Audit Slice

Added `BlueprintFactionConstructionResponseCycleReviewActionReviewAuthority` and `Milestone03BlueprintFactionConstructionResponseCycleReviewActionReviewAuditSmoke` to continue the archived faction-construction response-cycle review-action chain after readback. The new audit requires readback readiness, readable reviewer context, evidence links, readable allowed actions, privacy reminder, and follow-up boundary before any future archived review action can be reviewed.

This remains an audit-only contract: it does not reopen review actions, execute review actions, write archives, reveal hidden faction data, update status, enqueue notifications, alter job state, move evidence, refresh archives, release reservations, or complete construction. The Infopedia construction-blueprints entry and Gate 3 suite now name the review boundary and guard.

Verification: Java 17 full-tree compile passed with the existing Netty unchecked note; focused response cycle review action review smoke passed; Infopedia mechanics readability smoke passed; Gate 3 player-facing text smoke suite passed with existing worldgen/save warnings; function and Mermaid maps refreshed at 690 Java modules and 0 unpositioned modules; package rebuild passed with 1670 class files and 6096 package assets; Java 17 package scan passed for 3340 classfiles, highest major version 61; package boot smoke stayed alive for 8 seconds; repository manifest refresh wrote 205846 indexed file rows with reused=202455, hashed=3391, errors=0.

## Milestone 03 - Blueprint Faction Construction Response Cycle Review Action Readback Audit Slice

Added `BlueprintFactionConstructionResponseCycleReviewActionReadbackAuthority` and `Milestone03BlueprintFactionConstructionResponseCycleReviewActionReadbackAuditSmoke` to continue the archived faction-construction response-cycle review-action chain after archive preservation. The new audit requires archive readiness, archive lookup readiness, readable summary, privacy filter readiness, readable replay reference, and stale record marker before any future archived review action can be presented.

This remains an audit-only contract: it does not read archives from storage, write archives, replay commands, reveal hidden faction data, update status, enqueue notifications, alter job state, move evidence, refresh archives, release reservations, or complete construction. The Infopedia construction-blueprints entry and Gate 3 suite now name the readback boundary and guard.

Verification: Java 17 full-tree compile passed with the existing Netty unchecked note; focused response cycle review action readback smoke passed; Infopedia mechanics readability smoke passed; Gate 3 player-facing text smoke suite passed with existing worldgen/save warnings; function and Mermaid maps refreshed at 688 Java modules and 0 unpositioned modules; package rebuild passed with 1667 class files and 6096 package assets; Java 17 package scan passed for 3334 classfiles, highest major version 61; package boot smoke stayed alive for 8 seconds; repository manifest refresh wrote 205825 indexed file rows with reused=202440, hashed=3385, errors=0.

## Milestone 03 - Blueprint Faction Construction Response Cycle Review Action Archive Audit Slice

Added `BlueprintFactionConstructionResponseCycleReviewActionArchiveAuthority` and `Milestone03BlueprintFactionConstructionResponseCycleReviewActionArchiveAuditSmoke` to continue the archived faction-construction response-cycle review-action chain after close readiness. The new audit requires close readiness, a readable archive reason, retention label, privacy label, result snapshot, and replay reference before any future closed archived review action can be preserved.

This remains an audit-only contract: it does not write archives, redact records, delete records, update status, enqueue notifications, move evidence, refresh archives, reveal hidden faction data, alter job state, release reservations, or complete construction. The Infopedia construction-blueprints entry and Gate 3 suite now name the archive boundary and guard.

Verification: Java 17 full-tree compile passed with the existing Netty unchecked note; focused response cycle review action archive smoke passed; Infopedia mechanics readability smoke passed; Gate 3 player-facing text smoke suite passed with existing worldgen/save warnings; function and Mermaid maps refreshed at 686 Java modules and 0 unpositioned modules; package rebuild passed with 1664 class files and 6096 package assets; Java 17 package scan passed for 3328 classfiles, highest major version 61; package boot smoke stayed alive for 8 seconds; repository manifest refresh wrote 205804 indexed file rows with reused=202425, hashed=3379, errors=0.

## Milestone 03 - Blueprint Faction Construction Response Cycle Review Action Close Audit Slice

Added `BlueprintFactionConstructionResponseCycleReviewActionCloseAuthority` and `Milestone03BlueprintFactionConstructionResponseCycleReviewActionCloseAuditSmoke` to continue the archived faction-construction response-cycle review-action chain. The new audit requires follow-up readiness, a readable close decision, status return declaration, notification return declaration, evidence retention boundary, and archive refresh boundary before any future archived review-action follow-up can close.

This remains an audit-only contract: it does not close follow-up, update status, enqueue notifications, move evidence, refresh archives, reveal hidden faction data, alter job state, release reservations, or complete construction. The Infopedia construction-blueprints entry and Gate 3 suite now name the close boundary and guard.

Verification: Java 17 full-tree compile passed with the existing Netty unchecked note; focused response cycle review action close smoke passed; Infopedia mechanics readability smoke passed; Gate 3 player-facing text smoke suite passed with existing worldgen/save warnings; function and Mermaid maps refreshed at 684 Java modules and 0 unpositioned modules; package rebuild passed with 1661 class files and 6096 package assets; Java 17 package scan passed for 3322 classfiles, highest major version 61; package boot smoke stayed alive for 8 seconds; repository manifest refresh wrote 205783 indexed file rows with reused=202410, hashed=3373, errors=0.

## Milestone 02 - Semantic Asset Audit Dev Room and Manual Tile Cycling

Added a curated asset smoke-test room to Tools / Zone Audit. The audit cursor can cycle backward or forward through compatible indexed semantic assets for the selected tile, immediately updating the rendered target and showing its asset ID, type, name, source, and candidate count.

Manual choices remain transient audit overrides, so visual diagnosis cannot silently rewrite production registry or world-generation data. The semantic runtime registry smoke now verifies that the room builds and that a floor tile can cycle to a different valid indexed asset.

Verification: fresh Java 17 full-tree compile passed; the focused semantic runtime registry smoke and complete Gate 3 player-facing suite passed. The package-seed gate staged 3,000 Java 17-compatible classfiles, and the function and Mermaid code maps were refreshed. Manual GUI inspection was not run.

## Milestone 02 - Streetlight Infrastructure Semantic Promotion Slice

Promoted the existing `RoadInfrastructureTileRules` meaning for `Road_infrastructure` row 5 columns 1-2 into the deep asset descriptor source. Those visually verified lamp-post cells now carry explicit streetlight, infrastructure, fixture, and sidewalk metadata instead of inheriting only the broad road-atlas category.

Runtime compiled-index classification now honors explicit fixture content types before broad source-group tags. The active semantic resolver therefore selects streetlight fixture art and rejects system inventory, item-icon, and UI-icon substitutions. Expanded the runtime registry smoke to guard that contract.

Verification: fresh Java 17 full-tree compile passed. Focused runtime registry, semantic resolver, migration coverage, and binding-doctrine smokes passed; the complete Gate 3 player-facing smoke suite passed. The active registry resolves `STREETLIGHT_FIXTURE` to `FIX-0151` and reports infrastructure coverage complete (`1/1`), raising total semantic render intent coverage to `17/44`. The package-seed gate passed with 2,998 Java 17-compatible classfiles, and the function and Mermaid code maps were refreshed. Manual GUI inspection was not run.

## Milestone 02 - Open and Closed Door Semantic Migration Slice

Preserved the `Doors-C` and `Doors-O` atlas state when compiled content-index rows become runtime asset metadata. Door alias scoring and `SemanticRenderAssetResolver` ranking now prefer dedicated fixture assets over mixed bulkhead wall sheets, so the live renderer receives distinct closed and open semantic assets for standard doors and archways.

Expanded the runtime registry smoke to require `FIXTURE`-typed closed and open door assets, reject opposite-state metadata, prove both states resolve to different IDs, and require the tile alias and general semantic resolver authorities to agree. Locked, security, vent-panel, and double-door aliases remain explicit missing-art cases until their specialized source art is semantically identified.

Verification: fresh Java 17 full-tree compile passed. Focused runtime registry, semantic resolver, migration coverage, and binding-doctrine smokes passed; the complete Gate 3 player-facing smoke suite passed. Active-registry migration coverage reports both door intents available (`2/2`) and `16/44` total semantic render intents available. The package-seed gate passed with 2,998 Java 17-compatible classfiles, and the function and Mermaid code maps were refreshed. Manual GUI inspection was not run.

## Milestone 02 - Themed Semantic Asset Registry Verification Slice

Corrected compiled-asset classification so structural atlas identity takes precedence over theme words such as `noble`. Noble floors, corridors, and walls now remain floor, corridor, and wall assets instead of being misclassified as portraits, allowing the live semantic tile authority to resolve them from the active registry.

Expanded `TileSemanticRuntimeRegistrySmoke` to verify generic, industrial, sewer, noble, road, and door representatives against their expected runtime asset type and theme while rejecting sewer/noble cross-theme substitution. Wired the smoke into the aggregate Gate 3 suite.

Verification: fresh Java 17 full-tree compile passed. Focused semantic binding, resolver, migration coverage, registry extension, and runtime registry smokes passed; the complete Gate 3 player-facing smoke suite passed. Runtime alias resolution increased from 98 of 210 aliases to 120 of 210. The package-seed gate passed with 2,998 Java 17-compatible classfiles, and the function and Mermaid code maps were refreshed. Manual GUI inspection was not run.

## Milestone 02 - Doom Viewport, Shared HUD, and Entity Facing Restoration Slice

Restored the live experimental Doom control mode by reconnecting `GamePanel` to the existing `FirstPersonRenderViewport` instead of the inert legacy stub. The active renderer now owns first-person painting, mouse look, continuous movement updates, movement-key release, ray-target clicks, and control-mode return to the normal 2D surface. Its diagnostic strip now renders above the shared HUD instead of underneath it.

The shared Doom HUD continues to use current body endurance, food, water, fatigue-derived energy, equipped left/right weapons, active hand, and player portrait state. Survival bars now expose their exact numeric values, and the active weapon remains visibly identified.

Added `FacingIndicatorAuthority` and integrated its small frame-joined cardinal triangle into the visible player and NPC world-sprite path. NPC movement, including actor-layer push/squeeze displacement, now persists facing direction through `NpcEntity.moveTo(...)`. Added `Milestone02DoomHudFacingSmoke` and wired it into the Gate 3 suite.

Verification: Java 17 full-source compile, focused Doom/HUD/facing smoke, and the complete Gate 3 player-facing smoke suite. Manual GUI launch and visual playthrough were not run in this pass.

## Milestone 02 - Cardinal Camera Drift and Shared 2D Zoom Repair Slice

Added a mouse-idle grace period to the Doom camera, followed by slow yaw drift toward the nearest cardinal heading and pitch drift toward a level view. Fresh mouse motion immediately suspends settling, preserving direct first-person camera control.

Reattached saved viewport zoom percentages to both the standard 2D game viewport and Zone Auditor tile renderers through `MapViewportOptionsSubsystem.scaledTileSize(...)`. Mouse wheel and `+/-` now adjust both active 2D views, while Doom mode ignores 2D zoom and Zone Auditor retains Home/End for replay navigation.

Verification: Java 17 full-source compile, expanded Doom/HUD/facing/zoom smoke, and the complete Gate 3 player-facing smoke suite. Manual GUI launch and visual feel tuning were not run in this pass.

## Milestone 02 - Doom Center-Ray Targeting and Trade Preview Slice

Promoted the Doom crosshair from a decorative mark into the shared center-ray targeting solution. The exact viewport center now reports target kind and action, changes color for look/use/weapon targets, and drives keyboard and mouse Look, Interact, and weapon-aim commands without opening the old 2D targeting panel. Ray targeting now includes visible map objects in addition to entities, doors, and blocking world geometry, with interaction range enforced separately from look and aim range.

Continued Phase 4.18 trade readability by adding `TradeReadabilityAuthority`. The live offer detail pane now previews vendor identity, price, affordability, remaining script, quality band, legality/restriction risk, carrying-capacity result, and provenance where known before purchase.

Verification: Java 17 full-source compile, expanded Doom targeting smoke, new trade readability smoke, and the complete Gate 3 player-facing smoke suite. Manual GUI aiming and trade-panel playthrough were not run in this pass.

## Milestone 02 - Doom Idle Centering, Movement Holds, Auto-Turn Clock, and Inventory Detail Slice

Extended Doom viewpoint settling to continuous player position: when no movement input is held, velocity damps and the player slowly slides toward the exact center of the current logical tile without crossing collision boundaries.

Added standard hold-based first-person movement modifiers. Holding Shift temporarily selects Sprint with higher continuous speed and acceleration. Holding Ctrl or C temporarily selects Sneak/crouch with reduced speed and a lower camera height. Releasing the modifier restores the prior movement mode, including when Doom mode is exited while a modifier remains held.

The Doom HUD now shows the live 2.6-second passive auto-turn countdown. Passive timing initialization was repaired so enabling or entering the mode begins with a full interval instead of immediately advancing a turn; turn-based or inactive states show that automatic turns are paused.

Continued Phase 4.19 inventory readability with InventoryReadabilityAuthority. The live inventory detail pane now reports quality, category, honest condition-record availability, legality/restriction state, equipped hand, transfer consequence, carried load, use summary, and provenance when known.

Verification: Java 17 full-source compile, expanded Doom movement/countdown smoke, new inventory readability smoke, and the complete Gate 3 player-facing smoke suite. Manual GUI movement feel and countdown observation were not run in this pass.

## Milestone 02 - Container and Storage Transfer Readability Slice

Continued Phase 4.20 by adding ContainerReadabilityAuthority and routing the live container transfer pane through it. The surface no longer displays raw container IDs. It now explains player-facing storage identity, current access assumptions, the absence of an enforced capacity record, carried load, Take and Put consequences, full-load denial, selected-item provenance, and warnings for mission/evidence, illicit, forbidden, or volatile goods.

Added Milestone02ContainerReadabilitySmoke and wired it into the Gate 3 suite.

Verification: Java 17 full-source compile, focused container readability smoke, and the complete Gate 3 player-facing smoke suite.

## Milestone 02 - Read-Only Faction Member Roster Slice

Advanced Phase 4.21 without inventing unsupported command authority. Added FactionRosterReadabilityAuthority and a compact read-only faction-member section to the existing Character surface. It reports available member count, identity, role, faction, duty, skill, and player-facing loyalty bands while explicitly stating that reassignment and member-equipment commands are not yet implemented.

Added Milestone02FactionRosterReadabilitySmoke and wired it into the Gate 3 suite.

Verification: Java 17 full-source compile, focused faction roster readability smoke, and the complete Gate 3 player-facing smoke suite.

## Milestone 02 - Manual Production Forecast Slice

Advanced Phase 4.23 by adding ProductionReadabilityAuthority and routing the live Crafting detail pane through it. A selected recipe now previews readiness or its exact blocker, resolved output count/name/quality, carried-inventory destination, selected machine quality and integrity after wear, adjusted manual turns and fatigue, XP, supplies/parts and named-item availability, required knowledge, and faction manufacturing pattern.

The surface explicitly distinguishes the immediate player-operated Craft action from the separate queued production system instead of implying that pressing Craft creates a machine job. Added Milestone02ProductionReadabilitySmoke and wired it into the Gate 3 suite.

Verification: Java 17 full-source compile, focused production readability smoke, and the complete Gate 3 player-facing smoke suite.

## Milestone 02 - Construction Blueprint Paging and Forecast Slice

Repaired the construction panel's flat-list reachability bug: all existing build recipes are now available through ten-blueprint Previous/Next pages instead of silently exposing only the first ten entries.

Added ConstructionReadabilityAuthority and routed the selected blueprint detail through it. The panel now reports the exact cursor placement result, material and named-component availability, quality, workbench/knowledge/faction requirements, permanent-placement consequence, and purpose before confirmation. Added Milestone02ConstructionReadabilitySmoke and wired it into the Gate 3 suite.

Verification: Java 17 full-source compile, focused construction readability smoke, and the complete Gate 3 player-facing smoke suite.

## Milestone 02 - Construction Category Organization Slice

Advanced Phase 4.25 by adding ConstructionCategoryAuthority and category-scoped paging to the live build panel. Existing blueprints are now grouped into Shelter and Storage, Defense, Machines and Utilities, Commerce and Medical, Logistics, and Laboratory, with an All view retained. Changing category resets paging while preserving the existing blueprint selection and placement authority.

Added Milestone02ConstructionCategorySmoke to verify that every live blueprint belongs to exactly one player-facing category, representative recipes resolve correctly, no category is empty, and category cycling wraps safely. Wired the smoke into the Gate 3 suite.

Verification: Java 17 full-source compile, focused construction category smoke, and the complete Gate 3 player-facing smoke suite.

## Milestone 02 - Crafting and Construction Menu Audit Truthfulness Slice

Continued Phase 4.26 by correcting UniversalWindowAuthority records that claimed tabs and progress bars for the live Crafting and Construction panels. Their player-facing audit definitions now describe the implemented manual crafting forecast, explicit separation from queued machine jobs, full-catalog construction category paging, and live placement/material feedback. Construction retains its real search/filter capability through category filtering.

Expanded Milestone02MenuUniformityReadabilitySmoke to guard the updated contracts and reject future fictional capability claims.

Verification: Java 17 full-source compile, focused menu uniformity smoke, and the complete Gate 3 player-facing smoke suite.

## Milestone 02 - Body Condition and Combat Readiness Slice

Advanced Phase 14.1 by replacing the Character panel's raw per-body-part Endurance and Agility dump with BodyConditionReadabilityAuthority. The live Body / Loadout pane now derives overall condition, combat readiness, trauma, bleeding/infection urgency, stamina and supply impairment, the worst affected body regions, clothing protection, and equipped hands from current runtime state.

The summary uses decision-oriented condition bands rather than raw body-stat leakage. Added Milestone02BodyConditionReadabilitySmoke for healthy and injured states and wired it into the Gate 3 suite.

Verification: Java 17 full-source compile, focused body-condition readability smoke, and the complete Gate 3 player-facing smoke suite.

## Milestone 02 - Medical Treatment Readiness Slice

Advanced Phase 14.2 with MedicalTreatmentReadabilityAuthority in the Character Body / Loadout pane. Current injuries now produce carried-treatment guidance based on the actual ordinary Use path: medkits, bandages, splints, and antiseptics are recognized, while unavailable treatment directs the player toward supplies or a clinic.

The panel explicitly states that cataloged named drugs and stimulants do not yet receive specialized runtime effects through ordinary Use, preventing descriptive catalog content from being mistaken for implemented simulation. Added Milestone02MedicalTreatmentReadabilitySmoke and wired it into the Gate 3 suite.

Verification: Java 17 full-source compile, focused medical-treatment readability smoke, and the complete Gate 3 player-facing smoke suite.

## Milestone 02 - Entity Identity and Social Context Slice

Advanced Phase 15.1 and 15.2 with EntityIdentityReadabilityAuthority. Ordinary NPC interaction and conversation portrait panes now expose readable name, role, faction, rank title and authority scope, current activity, relationship to the recorded home/duty position, condition approximation, visible equipment threat, age, and known provenance while explicitly preserving uncertainty about private motives.

Removed raw NPC HP from ordinary interaction and progressive examination text. Added Milestone02EntityIdentityReadabilitySmoke and wired it into the Gate 3 suite.

Verification: Java 17 full-source compile, focused entity-identity readability smoke, and the complete Gate 3 player-facing smoke suite.

## Milestone 02 - Personnel Roster Status Hygiene Slice

Advanced Phase 15.3 by expanding FactionRosterReadabilityAuthority with compact assignment, skill-band, availability, and loyalty-warning language. The roster now distinguishes general-labor availability from assigned members while preserving the existing read-only command boundary.

The panel explicitly states that recruit rank and current world location are not present in the recruit save record, avoiding fabricated command tier or workplace claims. Expanded Milestone02FactionRosterReadabilitySmoke to guard the new status and limitation wording.

Verification: Java 17 full-source compile, focused faction-roster readability smoke, and the complete Gate 3 player-facing smoke suite.

## Milestone 02 - Player Expansion Heat Readability Slice

Advanced Phase 17.4 with ExpansionHeatReadabilityAuthority in the Auspex Signals pane. The readout now translates suspicion and gang attention into readable bands, counts open commerce, defenses, production assets, laboratories/clinics, and restricted or military assets, totals recorded per-business heat, and explains likely attention drivers and available relief paths.

The UI explicitly states the current simulation boundary: asset exposure is advisory and is not yet automatically aggregated into the global suspicion or gang-attention meters. Added Milestone02ExpansionHeatReadabilitySmoke and wired it into the Gate 3 suite.

Verification: Java 17 full-source compile, focused expansion-heat readability smoke, and the complete Gate 3 player-facing smoke suite.

## Milestone 02 - Interaction Approach Planning Slice

Advanced Phase 17.6 with InteractionApproachAuthority. NPC, animal, vendor, machine, container-style object, and base-object interaction surfaces can now request an Approach plan. The authority evaluates the four adjacent tiles, selects the shortest reachable destination within the current movement mode, and opens the existing manual movement ghost/path for explicit confirmation.

Approach never teleports or auto-commits movement, and unavailable adjacency reports a clear range/path failure. Added Milestone02InteractionApproachSmoke and wired it into the Gate 3 suite.

Verification: Java 17 full-source compile, focused interaction-approach smoke, and the complete Gate 3 player-facing smoke suite.

## Milestone 02 - Contract Objective and Evidence Readability Slice

Advanced Phase 17.1 with ContractObjectiveReadabilityAuthority in the Map / Objectives pane. Active faction contracts now show a sanitized objective, readable route location and certainty, required proof or delivery item, whether that item is carried, stored, or missing, and the script/standing reward.

Internal contract IDs and target entity IDs remain hidden, and unconfirmed local identities or exact target positions are explicitly withheld until the route records them. Added Milestone02ContractObjectiveReadabilitySmoke and wired it into the Gate 3 suite.

Verification: Java 17 full-source compile, focused contract-objective readability smoke, and the complete Gate 3 player-facing smoke suite.

## Milestone 02 - Shared Transfer Workflow Grammar Slice

Advanced Phase 17.5 with TransferWorkflowReadabilityAuthority. Inventory/base storage, container transfer, and vendor purchase previews now share source, destination, one-item quantity, permission, destination capacity, mission/evidence protection, reversibility, and confirmation/cancel language while retaining each surface's real execution rules.

Purchases are explicitly marked as not automatically reversible, while storage/container moves identify the paired return action where access remains available. Added Milestone02TransferWorkflowConsistencySmoke and wired it into the Gate 3 suite.

Verification: Java 17 full-source compile, focused transfer-workflow consistency smoke, and the complete Gate 3 player-facing smoke suite.

## Milestone 02 - Live Context Binding Prompt Slice

Advanced Phase 17.7 by replacing the gameplay panel header's hardcoded Esc/Tab/Enter text with live context prompts from ControlReferenceTextSubsystem. Inventory, character, trade, container, conversation, object interaction, Look, Interact, combat, construction, crafting, Auspex, scavenge, map, Infopedia, pause, and movement planning now select appropriate named actions.

Prompts consume the current remappable keyboard profile. Controller views show their selected controller-family text while retaining current keyboard recovery bindings, and long prompts are fitted to the panel header. Added Milestone02LivePanelPromptSmoke and wired it into the Gate 3 suite.

Verification: Java 17 full-source compile, focused live-panel prompt smoke, and the complete Gate 3 player-facing smoke suite.

## Milestone 02 - Exposed Mechanics Infopedia Coverage Slice

Advanced Phase 18 Infopedia coverage by registering player-facing mechanic references for body condition, medical treatment readiness, production forecasts, construction blueprints, expansion heat, interaction approach planning, contract objectives and evidence, and shared transfer workflows. Each entry explains the implemented behavior, states important non-automatic or unsupported boundaries, names its validation guard, and links to related mechanics through the existing navigable Related action.

Expanded Milestone02InfopediaMechanicsReadabilitySmoke to cover every new row, searchable detail text, cross-system health filtering, leak prevention, and the corresponding focused smoke guard names.

Verification: Java 17 full-source compile, expanded Infopedia mechanics smoke, and the complete Gate 3 player-facing smoke suite.

## Milestone 02 - Gameplay-to-Infopedia Mechanic Hot-Link Slice

Advanced Phase 18.1 with InfopediaHotLinkAuthority. Gameplay panels can now resolve a stable mechanic key, open the existing game-owned InfoPedia mechanics tab, select the exact reference row, and reset list/detail scrolling without duplicating encyclopedia UI.

Added direct Body Condition, Contract Objectives and Evidence, and Expansion Heat reference buttons to the Character, Map, and Auspex panels. Added Milestone02InfopediaHotLinkSmoke and wired it into the Gate 3 suite.

Verification: Java 17 full-source compile, focused Infopedia hot-link smoke, expanded Infopedia mechanics smoke, and the complete Gate 3 player-facing smoke suite.

## Milestone 02 - Structured Menu Definition Audit Slice

Advanced Phase 18.4 with MenuDefinitionAuditAuthority. Every UniversalWindowAuthority registration now produces a player-readable audit definition covering menu ID/title, owning authority, purpose, data source, panes, actions, back behavior, declared capabilities, world-input behavior, text containment, and domain-owned permission/failure boundaries.

The Menu Uniformity Infopedia reference now contains the structured audit, while the tactical slate and pause session surface show a compact readiness summary and provide a direct Menu Audit hot-link. Added Milestone02MenuDefinitionAuditSmoke and wired it into the Gate 3 suite.

Verification: Java 17 full-source compile, focused structured-menu audit smoke, expanded menu/Infopedia smokes, and the complete Gate 3 player-facing smoke suite.

## Milestone 02 - Movement Planning Definition Audit Slice

Advanced Phase 18.5 with MovementPlanningDefinitionAuditAuthority. The Movement Planning Infopedia entry now audits movement modes, ghost visuals, placement inputs, live bindings, valid and invalid target rules, hazard limitations, interaction adjacency, controller verification status, overlay priority, and reset/persistence expectations.

The audit deliberately records that hazard exposure does not yet alter route acceptance, end-to-end gamepad ghost nudging still needs live verification, and save/load focus reset needs a dedicated persistence audit. Added a tactical-slate Move Audit hot-link plus Milestone02MovementPlanningDefinitionAuditSmoke in the Gate 3 suite.

Verification: Java 17 full-source compile, focused movement-definition audit smoke, expanded movement/Infopedia smokes, and the complete Gate 3 player-facing smoke suite.

## Milestone 02 - Accurate Current-versus-Default Input Audit Slice

Repaired Phase 18.6 input audit accuracy. Input rows no longer label a dynamically rebound key as the default or claim every profile is still using default mappings. The shared prompt subsystem now exposes explicit baseline keyboard/controller prompts separately from live current-profile prompts, and the audit displays both values for each action.

The tactical slate and pause session surface now include an Input Audit readiness summary and direct hot-link. Expanded Milestone02InputRebindingAuditSmoke to rebind Confirm, verify the live key changes while the baseline remains stable, and restore defaults before the remaining Gate 3 suite runs.

Verification: Java 17 full-source compile, focused input audit/current-binding/profile smokes, and the complete Gate 3 player-facing smoke suite.

## Milestone 02 - Movement Route Hazard Warning Slice

Advanced the Phase 19 movement-readiness checklist by connecting movement previews to the existing environmental hazard records. Manual, directional, Approach, and mouse preview routes now inspect crossed tiles, report the number of hazardous tiles and highest visible concern, and render valid hazardous paths in amber rather than presenting them as ordinary safe routes or invalid red routes.

Hazard warnings remain advisory: they do not yet block movement or calculate exposure cost. Cancellation, execution, recovery, and main-menu reset clear hazard-preview state with the route. Expanded movement readability and definition-audit smokes to cover severity selection, warning language, truthful non-blocking behavior, and the updated audit contract.

Verification: Java 17 full-source compile, focused movement readability/definition smokes, and the complete Gate 3 player-facing smoke suite.

## Milestone 02 - Hazardous Quick-Movement Confirmation Slice

Closed a Phase 19 quick-versus-planned movement inconsistency. Walk and Sneak inputs still execute ordinary safe steps immediately, but a step onto a recorded environmental hazard now opens the same one-tile amber movement ghost and warning used by deliberate planning. The player must confirm the risky step or can cancel it without moving.

Run and Sprint already route through multi-tile planning, so all movement modes now receive pre-commit warning when their known route crosses a recorded hazard. Expanded movement readability and definition-audit guards for direct-step hazard detection and safe-tile immediacy.

Verification: Java 17 full-source compile, focused movement readability/definition smokes, and the complete Gate 3 player-facing smoke suite.

## Milestone 02 - Movement Planning Focus Reset Slice

Closed the movement planning save/load focus-reset gap with MovementPlanningFocusResetAuthority. Entering save/load, successfully loading another game state, or returning to the main menu now clears manual ghosts, mouse previews, route lists, hazard flags, look-cursor ownership, and stale preview targets through one shared bridge.

Updated the movement definition audit to claim only the now-wired reset paths. Added Milestone02MovementPlanningFocusResetSmoke and wired it into Gate 3 alongside the existing movement and persistence guards.

Verification: Java 17 full-source compile, focused movement focus-reset/definition/persistence smokes, and the complete Gate 3 player-facing smoke suite.

## Milestone 02 - Faction Personnel Authority and Staffing Readability Slice

Advanced the Phase 19 faction-management checklist by repairing the Character roster's overly broad claim that all reassignment was unavailable. The roster now distinguishes player command membership from the separate NPC worker track, shows actual recorded machine/defense station assignments, and points to the existing validated station-management staffing route.

The surface also states the current privacy and authority boundary: the compact recruit record has no rank, current location, or personal item ledger, so direct duty editing, member inventory transfer, and member equipment commands remain unavailable. Added a Faction Personnel and Staffing Infopedia entry, Character-panel hot-link, and expanded roster/Infopedia smoke coverage.

Verification: Java 17 full-source compile, focused faction-roster and Infopedia mechanics smokes, and the complete Gate 3 player-facing smoke suite.

## Milestone 02 - Manual Craft and Machine Queue Context Slice

Advanced the Phase 19 production-management checklist by extending the Crafting detail forecast with the selected machine's recorded worker, assigned recipe, queue remaining/target count, and shared operation queue totals. These are shown as separate machine state rather than being falsely attributed to the immediate Craft command.

The panel now states that manual Craft remains player-operated, currently has no separate power/fuel gate beyond its existing readiness checks, routes output to carried inventory, and does not control queued-machine output routing. Expanded production and Infopedia guard coverage for staffing, utility, queue, and routing boundaries.

Verification: Java 17 full-source compile, focused production readability and Infopedia mechanics smokes, and the complete Gate 3 player-facing smoke suite.

## Milestone 02 - Vendor Context and Protected-Sale Enforcement Slice

Advanced the Phase 19 trade checklist with readable market affiliation, faction standing band, accessible-stock count, shipment/scarcity context, and an explicit service boundary stating that the trade panel supports item buying and selling rather than unrelated repairs, treatment, lodging, banking, or training.

Repaired a transaction safety mismatch: mission, evidence, and intelligence items previously displayed a protection warning but could still be sold. The sale preview and execution path now share one protection rule and refuse ordinary vendor sale until a dedicated hand-in or explicit release flow exists. Expanded trade and transfer Infopedia smoke coverage.

Verification: Java 17 full-source compile, focused trade/transfer readability smokes, and the complete Gate 3 player-facing smoke suite.

## Milestone 02 - Pause Menu Movement Recovery Slice

Added a visible `Unstuck` action to the pause command panel. The action routes through `PauseMovementRecoveryAuthority` into `MovementPlanningAuthority.applyNearestStandableRecovery(...)`, preserves the paused screen, reports the request and success or failure through the event log and targeting report, and refreshes movement cursors, visited-zone state, dirty-region state, sensory state, and progressive-look state after a successful relocation.

Added `Milestone02PauseMovementRecoverySmoke` and wired it into `Gate3PlayerFacingTextSmokeSuite`. The smoke covers the player-facing label and tooltip, the shared recovery bridge, visible feedback requirements, no-silent-teleport audit contract, and safe failure without a loaded world.

Verification: Java 17 full-source compile and the Gate 3 player-facing smoke suite.

## Milestone 02 - Movement Debug Overlay Slice

Added `MovementDebugOverlayAuthority` as a per-session trace for the latest movement attempt. Unified movement execution now records the destination, whether actor occupancy was encountered, whether push/squeeze displaced another actor, and the accepted or rejected execution result. Pause-menu recovery records whether relocation was applied and its destination.

The pause/session panel now exposes the latest movement destination, occupancy result, push/squeeze result, recovery result, and execution result for smoke testing and validation. Added `Milestone02MovementDebugOverlaySmoke` and wired it into the Gate 3 suite.

Verification: Java 17 full-source compile and the Gate 3 player-facing smoke suite.

## Milestone 02 - Quest Objective Guidance Overlay Slice

Added QuestObjectiveGuidanceAuthority as a rendering-neutral contract for exact, approximate, rumored, hidden, unsafe, and nearest-transition guidance without taking ownership of quest progression. The map panel now lists active objective guidance, exact visible current-slice targets receive a slow pulsing marker, unsafe targets use warning color, and rumored or hidden objectives deliberately withhold exact coordinates and direction.

Added Milestone02QuestObjectiveGuidanceSmoke and wired it into the Gate 3 suite. No placeholder quests are created when the runtime has no active quest records; later quest systems can publish guidance records into the shared player-facing list.

Verification: Java 17 full-source compile and the Gate 3 player-facing smoke suite.

## Milestone 02 - Pet Interaction Feedback Slice

Added PetInteractionFeedbackAuthority and replaced the generic animal Pet action with species-aware companion feedback: dog-like pets receive Head Pat, cat-like pets receive Scritch, mouse/rat pets receive Nose Boop, and other pets receive gentle affection. Hostile, injured, sleeping, restrained, and non-companion animals now provide compact in-world denial reasons instead of silently changing state.

Added Milestone02PetInteractionFeedbackSmoke and wired it into the Gate 3 suite. Successful affection still uses the existing Animal Handling XP and turn paths; denied interactions do not consume a turn.

Verification: Java 17 full-source compile and the Gate 3 player-facing smoke suite.

## Milestone 02 - Action Denial Guidance Slice

Added ActionDenialGuidanceAuthority for access, blueprint, construction, interaction, and movement refusal text. Live construction placement now keeps detailed governance diagnostics in the audit authority while ordinary UI receives a sanitized domain reason and one practical resolution path. Construction blueprint labels no longer expose Java class names.

Added Milestone02ActionDenialGuidanceSmoke and wired it into the Gate 3 suite. Coverage verifies occupied-tile, missing-knowledge, and locked-access guidance while rejecting class-name and context diagnostic leakage.

Verification: Java 17 full-source compile and the Gate 3 player-facing smoke suite.

## Milestone 02 - Live Menu Grammar Coverage Slice

Expanded UniversalWindowAuthority coverage to include the live character, container transfer, object interaction, targeting, crafting, Auspex, scavenge, pause, and console surfaces. Common panel opening and closing now updates the shared runtime lifecycle state, and direct dialogue, object, trade, and container entry points report their real focus context.

Expanded Milestone02MenuUniformityReadabilitySmoke to verify the added high-traffic menu definitions and open/focus/close lifecycle behavior. This remains an incremental wrapper migration rather than a broad visual rewrite.

Verification: Java 17 full-source compile and the Gate 3 player-facing smoke suite.

## Milestone 02 - Conversation Relationship and Service Context Slice

Added ConversationReadabilityAuthority and connected it to the live dialogue panel. Conversations now show a readable faction relationship band, available service category, and known standing or hostility consequence. Faction representatives explicitly state when no quest offer is listed instead of implying a functional quest submenu, while traders identify available stock access.

Added Milestone02ConversationReadabilitySmoke and wired it into the Gate 3 suite. Coverage includes trusted, hated, and active-hostility states plus the no-placeholder-quest boundary.

Verification: Java 17 full-source compile and the Gate 3 player-facing smoke suite.

## Active Ledger Reset - 2026-06-05

The previous active development history was archived and this fresh ledger was started to keep continuing milestone work reviewable.

Continuation context:

- Current active development lane: Milestone 02, Phase 4 / Phase 18 input, controls, controller support, player-facing readability, and validation surfaces.
- Recently completed before this reset: input profile persistence, controller tuning persistence, runtime controller tuning application, controller tap/hold interpretation, and controller connection fallback notices.
- Current validation need: restore a GitHub Actions workflow that performs Java 17 compile and smoke-test checks for push and pull-request changes.

Verification: documentation reset only through the connector. The archive marker was created and this active ledger was replaced. Local compile, smoke tests, function/Mermaid map regeneration, repository manifest regeneration, package seed build, classfile scan, native installers, and manual GUI launch were not run in this connector session.

## Milestone 02 - GitHub Validation Workflow Restoration Slice

Restored repository-hosted validation for the current milestone workflow by adding `.github/workflows/milestone-validation.yml`. The workflow runs on push, pull request, and manual dispatch, checks out the repository, installs Temurin Java 17, compiles the main `src` tree with `javac --release 17`, runs the Gate 3 player-facing smoke suite plus key standalone milestone smokes, and stages a local package seed through the existing `ROOT_tools/packaging/stage_local_package_seed.ps1` path.

The workflow deliberately reuses the project-owned package-seed builder and Java 17 classfile scanner instead of inventing a second packaging/check path. It uploads the staged manifest directory as a small artifact when available so failed or successful runs can be inspected from GitHub Actions.

Verification: workflow file was created through the connector and points at existing Java source and package-seed tool paths. GitHub Actions had not yet reported a workflow run in this connector session; local compile, smoke execution, package seed build, classfile scan, native installers, signing/trust metadata, and manual GUI launch were not run locally here.

## Milestone 02 - Controller Glyph Prompt Fallback Slice

Continued Milestone 02 controller/input readability work by adding `ControllerGlyphPromptAuthority`. The authority records controller-family prompt text for Xbox, PlayStation, Steam Deck, and generic controller views, states that packaged glyph art is not yet available, and keeps keyboard/mouse recovery prompts explicitly visible while text fallback is active.

`InputRebindingAuditAuthority` now exposes controller glyph fallback readiness in the Infopedia audit instead of leaving glyph status implied. Added `Milestone02ControllerGlyphPromptSmoke` and wired it into `Gate3PlayerFacingTextSmokeSuite` so glyph fallback wording, controller-family prompts, and keyboard/mouse recovery language are covered by the main player-facing smoke path.

Verification: source changes were committed through the connector and the new smoke was wired into the Gate 3 suite. GitHub Actions had not yet reported a workflow run in this connector session; local compile, smoke execution, package seed build, classfile scan, native installers, signing/trust metadata, and manual GUI launch were not run locally here.

## Milestone 02 - Workflow Reactivation and Controller Prompt Mode Slice

Reactivated the milestone validation workflow with a visible workflow refresh commit. `.github/workflows/milestone-validation.yml` now has an explicit run name, branch-aware pull request trigger, `contents: read` permission, manual-dispatch input, validation-profile environment marker, and a GitHub Step Summary section. The validation job still uses Java 17, the Gate 3 smoke suite, key standalone milestone smokes, and the local package-seed builder.

Continued the current controller prompt lane by expanding `ControllerGlyphPromptAuthority` from a simple glyph fallback record into an explicit prompt-mode authority. The prompt surface now distinguishes keyboard/mouse-only mode, controller text fallback mode, and the future packaged-glyph controller mode. `Milestone02ControllerGlyphPromptSmoke` now checks those modes in addition to controller-family text prompts and keyboard/mouse recovery wording.

Verification: workflow and source changes were committed through the connector. GitHub still had not surfaced a workflow run for the reactivation commit when checked through the connector; local compile, smoke execution, package seed build, classfile scan, native installers, signing/trust metadata, and manual GUI launch were not run locally here.

## Milestone 02 - Java Source Encoding Validation Repair

Repaired the Java 17 validation path after the Windows Actions runner reported unmappable UTF-8 characters in `ChatRuntimeAuthority.java` while compiling with the platform default `windows-1252` source encoding. The source file intentionally contains player-facing typographic characters, so the fix is to make the build path explicit rather than stripping readable text.

Updated `.github/workflows/milestone-validation.yml` so the CI compile step invokes `javac -encoding UTF-8 --release 17`. Updated `ROOT_tools/packaging/stage_local_package_seed.ps1` so both client and launcher package compiles also invoke `javac -encoding UTF-8 --release 17`. This keeps CI and package-seed builds aligned on the same source-encoding rule.

Verification: workflow and package-seed script changes were committed through the connector. GitHub still had not surfaced a workflow run for the encoding-fix commit when checked through the connector; local compile, smoke execution, package seed build, classfile scan, native installers, signing/trust metadata, and manual GUI launch were not run locally here.

## Milestone 02 - Zone Tile Slot State Transition Slice

Continued the staged move away from pure glyph-based zone encoding by expanding `ZoneTileState` into an explicit slot-bearing floor-space model. The legacy glyph remains available as an import/export bridge, but semantic state now has named slots for surface, space, owner, room, corridor, road network, transition, reservation, fixtures, containers, loose items, entities, pets, vehicles, lights, and overlays.

The immediate Java compile failure in the wall-glyph switch was repaired by replacing raw block-character literals with Unicode escapes. Added `Milestone02ZoneTileSlotStateSmoke` and wired it into `Gate3PlayerFacingTextSmokeSuite`; the smoke proves a single floor tile can retain a floor legacy glyph while also carrying room ownership, a container, loose item, occupant, pet, vehicle, light, and reservation data.

Verification: source changes were committed through the connector and the new smoke was wired into the Gate 3 suite. GitHub Actions had not yet reported a workflow run for the tile-slot commit when checked through the connector; local compile, smoke execution, package seed build, classfile scan, native installers, signing/trust metadata, and manual GUI launch were not run locally here.

## Milestone 02 - Zone Room Corridor Entity Layer Mapping Reevaluation Slice

Reevaluated the new tile-slot model for room, corridor, and entity placement by adding `ZoneTileLayerMappingAuditAuthority`. The authority defines the semantic layers that tile data should map onto: surface, space, ownership, structure, content, actor, lighting, and overlay. It audits whether rooms, corridors, objects, containers, loose items, entities, pets, vehicles, and lights are represented in their correct layer rather than only by the legacy glyph.

Added `Milestone02ZoneTileLayerMappingSmoke` and wired it into `Gate3PlayerFacingTextSmokeSuite`. The smoke constructs representative room, corridor, and entity tiles, then verifies that room/corridor records land in structure and space layers, faction ownership lands in ownership, fixtures/containers/items land in content, entities/pets/vehicles land in actor, and lights land in lighting. It also verifies that a broken entity tile is reported as missing actor-layer data.

Verification: source changes were committed through the connector and the new smoke was wired into the Gate 3 suite. GitHub Actions had not yet reported a workflow run for the layer-mapping commit when checked through the connector; local compile, smoke execution, package seed build, classfile scan, native installers, signing/trust metadata, and manual GUI launch were not run locally here.

## Milestone 02 - Actor-Layer Push Squeeze Movement Resolver Slice

Added `ZoneTileMovementResolutionAuthority` as a standalone actor-layer resolver for crowded and confined movement. The resolver reads occupancy from `ZoneTileState` actor slots instead of legacy glyphs, resolves ordinary open movement, shove/squeeze displacement, chain-push through narrow corridors, and blocked-crowd failsafe cases, and records routing debug traces with explicit failure reasons.

Added `Milestone02ZoneTilePushSqueezeMovementSmoke` and wired it into `Gate3PlayerFacingTextSmokeSuite`. The smoke covers open movement, pushing an occupied target into relief space, chain-pushing through a one-tile corridor into available end space, blocked-crowd fallback when no relief tile exists, and route-debug reporting for blocked destinations.

Verification: source changes were committed through the connector and the new smoke was wired into the Gate 3 suite. This slice restores the missing base-behavior bridge as a tested authority, but it has not yet been connected into the active `MovementPlanningAuthority.canEnter(...)` or runtime execute-move path. GitHub Actions had not yet reported a workflow run for the push/squeeze commit when checked through the connector; local compile, smoke execution, package seed build, classfile scan, native installers, signing/trust metadata, and manual GUI launch were not run locally here.

## Milestone 02 - Movement Recovery Search Slice

Added a neutral nearest-standable-tile recovery search inside `MovementPlanningAuthority` after standalone recovery-authority file creation was blocked by the connector. The helper searches outward by radius over `ZoneTileState` grids, requires the selected tile to be walkable, unoccupied by entity/pet/vehicle actor slots, and to have at least one adjacent standable exit, and returns a result for the caller to apply rather than mutating world state by itself.

Expanded `Milestone02MovementPlanningReadabilitySmoke` to cover both movement-commit bridge behavior and the recovery search. The smoke now verifies that occupied destinations remain denied without the actor-layer resolver, become routeable when the push/squeeze resolver is available, safe current tiles do not request recovery, blocked/trapped positions search outward to a valid standable tile, occupied candidates are ignored, and fully blocked grids fail safely without destination selection.

Verification: source changes were committed through the connector and the expanded smoke remains wired through the existing Gate 3 suite. GitHub Actions had not yet reported a workflow run for the movement-recovery commit when checked through the connector; local compile, smoke execution, package seed build, classfile scan, native installers, signing/trust metadata, and manual GUI launch were not run locally here.

## Phase 4 Cleanup - Connector-Limited Manifest and Mermaid Discipline Pass

Performed the authority-document cleanup pass required by the new conversation briefing. Re-read the active master plan, standards, governance, development history, milestone index, legacy source map, and Milestone 02 owner file before making cleanup changes. The pass targeted repository hygiene after recent Java additions in controller handling, zone tile layers, actor-layer movement resolution, and movement recovery search.

Updated `scripts/BUILD_MERMAID_CODE_MAP.py` so future local Mermaid regeneration positions the recent modules explicitly instead of relying on broad keyword heuristics. Added explicit ownership overrides for `ControllerGlyphPromptAuthority`, `ControllerConnectionStateTracker`, `ControllerTapHoldTracker`, `GamepadInputEngine`, `GenericControllerSchema`, `MovementPlanningAuthority`, `ZoneTileMovementResolutionAuthority`, `ZoneTileLayerMappingAuditAuthority`, and `ZoneTileState`.

Attempted to update `ROOT_docs/REPOSITORY_FILE_MANIFEST.tsv` with a connector-limited regeneration marker, but that write was blocked. The manifest was therefore left untouched rather than corrupting a generated checksum ledger with fabricated filesystem sizes, modified times, or SHA-256 values. `ROOT_docs/functionmap/Mermaid_Code_Map_Master.md` and the generated Mermaid ledgers were also not regenerated because the connector cannot run `py -3 scripts/BUILD_MERMAID_CODE_MAP.py --apply` over the full local tree.

Verification: documentation and script updates were committed through the GitHub connector only. Required local follow-up remains: run `ROOT_tools/update-repository-file-manifest.ps1`, run `py -3 scripts/BUILD_MERMAID_CODE_MAP.py --apply`, compile Java 17 with UTF-8 source encoding, run Gate 3 and touched movement/input smokes, rebuild jars/package seed, run the Java 17 classfile scan, and update this history with the real local verification results.

## Repository Storage Governance Cleanup Slice

Unified repository storage doctrine around `ROOT_docs/`, `ROOT_tools/`, `ROOT_build/`, `ROOT_SRC_assets/`, and the owning `PACKAGE_*` trees. Updated `ROOT_docs/DOCUMENTATION_STANDARDS.md` and `ROOT_docs/MASTER_GOVERNANCE_REVISION_II.md` so active documentation no longer points at root `docs/` or ad-hoc tooling/build locations.

Moved the active handoff briefing into `ROOT_docs/NEW_DEVELOPMENT_CONVERSATION_BRIEFING.md`, removed the old root-level handoff copy, added `ROOT_tools/functionmap/BUILD_MERMAID_CODE_MAP.py` as the canonical Mermaid generator entry point, established `ROOT_build/README_ROOT_BUILD.md`, and removed the empty stale `ROOT_tools/repo_file_index.txt` artifact.

Verification: connector-only repository cleanup. Local manifest regeneration, Mermaid regeneration, Java compile, smoke tests, package seed build, classfile scan, zip integrity, native package checks, and manual GUI launch were not run. Required local follow-up: run `ROOT_tools/update-repository-file-manifest.ps1`, run `py -3 ROOT_tools/functionmap/BUILD_MERMAID_CODE_MAP.py --apply`, then rerun Java 17 validation.

## Milestone 02 - Runtime Movement Recovery Bridge Slice

Evaluated current milestone progress against Milestone 02 and continued the player-movement safety lane. `MovementPlanningAuthority` now has `MovementRecoveryApplicationResult` and `applyNearestStandableRecovery(...)`, which builds a temporary `ZoneTileState` snapshot from the legacy world, marks NPC actor occupancy, selects the nearest standable recovery destination using the existing expanding-radius search, applies the destination to player position/motion state when a valid destination exists, and records an event/targeting report.

Expanded `Milestone02MovementPlanningReadabilitySmoke` to verify the runtime bridge audit marker and the null-world failsafe path. This does not yet wire a visible pause-menu button into `LegacyPanelContext`; it creates the safe authority method that the pause menu can call without duplicating movement-recovery rules in the Swing surface.

Verification: source and history changes were committed through the connector. Local Java 17 compile, Gate 3 smoke execution, package seed build, classfile scan, manifest regeneration, Mermaid regeneration, native package checks, and manual GUI launch were not run here.

## Milestone 02 - Inventory Mixed-Quality Stack Clarity Slice

Continued inventory readability by distinguishing duplicate units of the exact selected quality from related units in the same item family. The live Inventory / Storage detail pane now reports the exact-selection count, total related-family count, number of represented quality grades, and explicitly states that Use, Equip, Store, and Take affect one selected unit at a time.

Expanded `Milestone02InventoryReadabilitySmoke` with a mixed Common/Fine weapon list so exact count, family count, quality-grade count, and one-unit action wording remain covered. Verification follows this entry through the local Java 17 compile and smoke path.

## Milestone 02 - Inventory Equipment Infopedia Route Slice

Added an `Inventory and Equipment` mechanic reference covering individual-unit rows, mixed-quality item families, equipment matching, condition boundaries, protected goods, and one-unit action scope. The live Inventory / Storage panel now exposes an `Item Info` hot-link to that entry, and Transfer Workflows links back to it for durable navigation between item and movement rules.

Expanded `Milestone02InfopediaMechanicsReadabilitySmoke` to require the new row and its inventory smoke guard. Verification follows through a fresh Java 17 compile, focused Infopedia and hot-link smokes, and the full Gate 3 player-facing suite.

## Milestone 03 - Production Quality Cap and Trace Slice

Started the first bounded Milestone 03 implementation by repairing `cappedProductionQuality(...)`, which previously returned only the selected machine quality and bypassed the central doctrine and recipe ceilings. Immediate manual crafting now resolves output quality through `QualityAuthorityApi` using known doctrine, recipe requirement, and machine quality, while material, facility, and worker quality remain explicit open hooks.

Added `ProductionQualityTraceAuthority` so the live crafting forecast reports expected quality, each active cap, the main limiting input, and the inactive quality-ledger boundary. Added `Milestone03ProductionQualityTraceSmoke` and wired it into the Gate 3 player-facing suite to cover recipe-limited and machine-limited production outcomes.

Verification: fresh Java 17 full-tree compile passed. `Milestone03ProductionQualityTraceSmoke`, `Milestone02ProductionReadabilitySmoke`, and `Gate3PlayerFacingTextSmokeSuite` passed. Manual GUI launch was not run.

## Milestone 03 - Quality Provenance Persistence Slice

Extended item provenance so newly crafted units retain their resolved output quality, recipe knowledge source, producing machine quality, and active quality limiter. These fields survive provenance save encoding, decoding, and later transfers while older seven-field provenance records remain readable.

The live inventory detail pane now exposes the recorded production-quality context when it exists. Added `Milestone03QualityProvenanceSmoke` to cover production recording, persistence round-trip, transfer preservation, and legacy decode compatibility.

## Milestone 03 - Quality-Sensitive Production Outcome Forecast Slice

Corrected production defect estimates so they account for both faction manufacturing style and the resolved output quality's reliability and defect multipliers. Value and usable-charge estimates already consumed output quality; the live crafting forecast now exposes all three estimates together.

The forecast explicitly states that defect risk is comparative guidance and that immediate manual crafting does not yet create a separate defect-state record. Added `Milestone03ProductionOutcomeForecastSmoke` to verify that better quality increases estimated value and charges, reduces defect risk, and still preserves faction-specific manufacturing differences.

## Milestone 03 - Live Production Reference Route Slice

Retargeted the crafting panel's generic InfoPedia button into a direct `Production Info` hot-link. The Production Forecast reference now documents the live doctrine/recipe/machine quality cap, open material/facility/worker hooks, quality provenance persistence, value and charge estimates, quality-sensitive defect risk, and the current absence of instantiated defect-state records.

Expanded the Infopedia mechanic smoke so the durable production reference must retain both Milestone 02 crafting behavior and the new Milestone 03 outcome-forecast guard.

Verification: fresh Java 17 full-tree compile passed. Focused inventory, production, quality trace, provenance, outcome forecast, Infopedia mechanics, and Infopedia hot-link smokes passed across this iteration. `Gate3PlayerFacingTextSmokeSuite` passed after every completed slice. Manual GUI launch was not run.

## Milestone 03 - Machine Condition Production Risk Slice

Connected the existing absolute machine-integrity value to manual production. Broken machines at integrity zero now refuse crafting with a repair requirement; critical and worn machines remain usable but add visible defect-risk surcharges. Because the legacy object model has no maximum-integrity field, this slice uses explicit absolute bands instead of inventing percentage condition.

The crafting forecast now reports machine condition and its defect adjustment. Crafted-item provenance records the producing machine condition through save/load and transfers. Added `Milestone03MachineConditionProductionSmoke` and expanded the provenance smoke for this context.

## Milestone 03 - Owned Machine Field Repair Workflow Slice

Added a supported repair path to the live base-object interaction panel. A damaged owned machine can spend one machine part and one turn to restore up to two integrity, stopping at the serviceable threshold rather than fabricating an unknown original maximum-integrity value. The panel previews resource cost, projected integrity, missing-part refusal, and the no-repair-needed state before mutation.

Added `MachineRepairAuthority` and `Milestone03MachineRepairWorkflowSmoke` to keep repair cost, restoration amount, serviceable cap, and refusal wording explicit.

## Milestone 03 - Manual Production Operator Skill Slice

Connected each crafting recipe's named XP skill to the existing 4-11 core-stat scale for manual production forecasts. Recipe skills map onto Mechanics, Firearms, Melee, Charm, Endurance, or Intellect and produce transparent novice, practiced, skilled, or expert bands. These bands adjust comparative defect risk while leaving the doctrine/recipe/machine quality cap unchanged.

The live forecast shows the mapped stat, value, band, and risk adjustment. Newly crafted item provenance retains the operator skill and band through save/load and transfers. Added `Milestone03ProductionOperatorSkillSmoke` for mapping and risk behavior.

## Milestone 03 - Production Condition Skill Reference Consolidation Slice

Updated the live Production Forecast Infopedia reference to include broken-machine blocking, worn-machine defect risk, bounded owned-machine repair, and manual operator skill mapping. Expanded the mechanic-reference smoke so these rules remain reachable from the crafting panel's Production Info route.

Strengthened `Milestone03QualityProvenanceSmoke` to prove operator skill and band survive provenance encoding, decoding, and transfers alongside machine condition and quality-cap context.

Verification: fresh Java 17 full-tree compile passed. Machine condition, machine repair, operator skill, quality trace, quality provenance, production outcome, production readability, Infopedia mechanics, Infopedia hot-link, and full Gate 3 smokes passed. Manual GUI launch was not run.

## Milestone 03 - Named Input Material Quality Slice

Activated the material-quality cap for manual recipes that consume named item units. Forecasting now follows the actual legacy consumption order, selecting matching carried units before base-storage units, and uses the lowest quality among the units that will be consumed as the material ceiling. Recipes that consume only abstract supplies or machine parts leave the material hook open.

Moved quality-trace capture before input removal so forecast and execution inspect the same units. Crafted-item provenance now preserves the consumed material-quality cap. Added `Milestone03ProductionMaterialQualitySmoke` for carried-versus-storage selection, mixed-quality limiting, and abstract-input boundaries.

## Milestone 03 - Material Quality Reference and Persistence Guard Slice

Updated the live Production Forecast reference with named-material selection order, lowest-consumed-unit limiting, and the abstract supply/part boundary. Expanded the quality provenance smoke to prove an active material cap survives encoding, decoding, and transfers, and expanded the Infopedia mechanic smoke to require the material rule and guard.

## Milestone 03 - Compact Recipe Material Cap Alignment Slice

Aligned crafting recipe-list status rows with the same material-aware quality authority used by the detail forecast and final execution. The compact `cap` label can no longer advertise machine-only quality when a lower-grade named component will cap the actual result. Expanded the material-quality smoke to cover this live list-row path.

Verification: fresh Java 17 full-tree compile passed. Material quality, quality trace, provenance, operator skill, machine condition, machine repair, production outcome, production readability, Infopedia mechanics, Infopedia hot-link, and full Gate 3 smokes passed. Headless display detection emitted safe-default warnings only. Manual GUI launch was not run.

## Milestone 03 - Assigned Worker Quality Boundary Slice

Added `ProductionWorkerQualityAuthority` to translate recruit skill 1-4 into Common, Serviceable, Fine, or Masterwork potential worker tiers. The live crafting forecast now shows the assigned worker's potential while explicitly preserving the current ownership rule: immediate Craft is player-operated, so an assigned recruit neither caps nor improves that manual result.

The authority also exposes the future staffed-run cap without activating dormant automation. Added `Milestone03ProductionWorkerQualitySmoke` and updated the durable Production Forecast reference with this boundary.

## Milestone 03 - Machine-Installed Production Knowledge Slice

Added one append-only installed-doctrine slot to each base machine with backward-compatible save parsing. The crafting panel now offers `Teach Machine`: the player must currently know the selected recipe doctrine, and installing it replaces the machine's prior doctrine. A matching installed doctrine then keeps the recipe visible, satisfies its knowledge execution gate, and contributes the doctrine tier to quality tracing even if the player no longer knows it.

Added `ProductionKnowledgeSourceAuthority` and `Milestone03MachineKnowledgeSourceSmoke` for player-versus-machine source resolution, teaching refusal/success, recipe visibility, execution access, quality contribution, and save-line placement.

## Milestone 03 - Knowledge Provider Provenance Slice

Extended crafted-item provenance with the provider of recipe knowledge, distinguishing player knowledge, installed machine doctrine, or both from the doctrine name itself. The provider is captured from the same authority used by the execution gate, survives save/load and transfers through an append-only provenance field, and appears in live inventory quality context.

Corrected the machine-knowledge smoke fixture so its doctrine-bearing machine participates in required-machine discovery. Focused machine-knowledge and quality-provenance smokes passed after a fresh Java 17 full-tree compile.

## Milestone 03 - Production Batch and Defect Disposition Slice

Added `ProductionBatchAuthority` so each immediate Craft action receives one shared batch identity and one inspection roll using the existing quality-, faction-, machine-condition-, and operator-sensitive defect forecast. Every output unit from that action records the same batch ID and either `passed inspection` or `defect flagged` in provenance; both fields survive save/load and transfers.

The live forecast and Production Forecast Infopedia entry now explain that batch disposition is instantiated, while flagged defects remain traceability data and do not yet reduce item statistics. Added `Milestone03ProductionBatchProvenanceSmoke`, expanded provenance and readability guards, and wired the new smoke into Gate 3.

Verification: fresh Java 17 full-tree compile passed. Machine knowledge, batch provenance, quality provenance, production readability, and the full Gate 3 player-facing smoke suite passed. Headless display detection emitted safe-default warnings only. Manual GUI launch was not run.

## Options UI - Standard Swing Editors and Multiplayer Privacy Slice

Set milestone feature work aside for an emergency options-menu modernization pass. Replaced every Text/UI, audio-volume, world-zoom, Doom-FOV, JVM-heap, screen-shake, and selected-color plus/minus command cluster with standard Java 17 Swing editors. Bounded numeric settings now open `JSlider`; custom colors open `JColorChooser`; On/Off settings route through grouped `JRadioButton` choices while preserving their existing save, audio, rendering, and restart side effects.

Added `SwingOptionsEditorAuthority` as the reusable bridge between the custom-painted menu shell and native Swing editors. Added `ROOT_docs/JAVA17_SWING_OPTIONS_UI_REFERENCE.md` with the Java 17 Swing package reference and recommended component ownership for named modes, exact numbers, paths, tabs, and validated text.

Hardened multiplayer privacy with `MultiplayerPrivacyAuthority`. The live custom multiplayer screen now redacts direct addresses, recent servers, favorites, statuses, and local-host binding details. Direct connection entry uses a guarded `JPasswordField` with deliberate temporary reveal. The standalone `MultiplayerJoinPanel` now also uses `JPasswordField` for server addresses and grouped On/Off `JRadioButton`s for stream-safe display instead of a checkbox.

Added `OptionsSwingComponentSmoke` for Java 17 component availability, guarded address entry, IPv4/IPv6 masking, and recent-server display privacy, and wired it into Gate 3.

Verification: fresh Java 17 full-tree compile passed. `OptionsSwingComponentSmoke` and the complete `Gate3PlayerFacingTextSmokeSuite` passed. Headless display detection emitted safe-default warnings only. Manual visual inspection of native dialogs was not run.

## Milestone 03 - Claimed Production Facility Quality Slice

Activated the facility-quality cap for immediate manual crafting without inventing a separate facility save ledger. `ProductionFacilityQualityAuthority` evaluates the selected machine's claimed production room and counts serviceable production stations in that room. One station supports Common output, two or three support Serviceable, four or five support Fine, and six or more support Masterwork; broken stations do not contribute. Unclaimed work areas leave the facility hook open.

The central production-quality trace now applies and names the facility cap alongside doctrine, recipe, machine, and named-material limits. Live recipe rows, detailed forecasts, and final execution share that authority. Crafted-item provenance records facility quality through append-only save encoding, decoding, and transfers. The Production Forecast reference documents the rule.

Added `Milestone03ProductionFacilityQualitySmoke`, expanded quality trace and provenance coverage, and wired the new smoke into Gate 3. Verification: fresh Java 17 full-tree compile passed; focused facility, quality-trace, and provenance smokes passed; the complete `Gate3PlayerFacingTextSmokeSuite` passed. Manual GUI inspection was not run.

## Milestone 03 - Defective Batch Market Appraisal Slice

Converted recorded production defects from provenance-only warnings into a bounded market consequence. `ProductionDefectAppraisalAuthority` applies a visible 40% ordinary-trader resale penalty to units whose preserved batch disposition is `defect flagged`; passed batches retain ordinary pricing. Inventory inspection explains the consequence, trade preview shows the adjusted value before confirmation, and final sale execution uses the same appraisal authority.

The slice deliberately does not invent hidden combat, durability, charge, or use penalties before a per-item condition owner exists. Updated batch feedback, Production Forecast guidance, and Infopedia coverage to state that boundary. Added `Milestone03ProductionDefectAppraisalSmoke`, updated the existing batch/readability guards, and wired the smoke into Gate 3.

Verification: fresh Java 17 full-tree compile passed; focused appraisal, batch provenance, production readability, and trade readability smokes passed; the complete `Gate3PlayerFacingTextSmokeSuite` passed. Manual GUI inspection was not run.

## Milestone 03 - Equipped Production Tool Quality Slice

Activated the manual-production tool-quality cap through `ProductionToolQualityAuthority`. Only a deliberately equipped fabrication or repair tool participates; unrelated carried items and ordinary weapons do not silently affect production. If both hands hold qualifying tools, the better-quality tool governs. Empty hands leave the hook open under the selected machine's integrated tooling.

The central production-quality trace now names equipped tool quality alongside doctrine, recipe, machine, material, and facility caps. Compact recipe rows, detailed forecasts, final execution, and append-only item provenance share the same result. The Production Forecast reference documents the equipped-tool rule and its no-silent-inventory-cap boundary.

Added `Milestone03ProductionToolQualitySmoke`, expanded quality provenance and Infopedia guards, and wired the smoke into Gate 3. Verification: fresh Java 17 full-tree compile passed; focused tool-quality, quality-trace, provenance, and production-readability smokes passed; the complete `Gate3PlayerFacingTextSmokeSuite` passed. Manual GUI inspection was not run.

## Milestone 03 - Faction Production Mutation Traceability Slice

Made the existing faction manufacturing consequences explicit through `ProductionFactionMutationAuthority`. Live crafting forecasts now identify the faction profile and output prefix, then show the effective value, charge, and defect-pressure multipliers already used by `ProductionRecipe`. This does not add a second faction-stat model; it explains the established `FactionManufacturingProfile` math in player-facing terms.

Crafted-item provenance now preserves the faction production mutation through append-only field 23, legacy decoding, and inventory/storage transfers. The Production Forecast reference documents the rule. Added `Milestone03ProductionFactionMutationSmoke`, expanded provenance and Infopedia guards, and wired the smoke into Gate 3.

Verification: fresh Java 17 full-tree compile passed; focused faction-mutation, quality-provenance, production-readability, and Infopedia smokes passed; the complete `Gate3PlayerFacingTextSmokeSuite` passed. Function and Mermaid maps refreshed. The maintained `PACKAGE_client` rebuilt successfully; the Java 17 scan passed for 2,988 classfiles at highest major version 61; the packaged client stayed alive for the full eight-second boot-smoke window. Repository manifest regeneration was attempted twice but exceeded two-minute and five-minute command ceilings, so that generated ledger still requires a successful refresh.

## Milestone 03 - Claimed Facility Knowledge Sharing Slice

Activated facility-provided production knowledge without creating a parallel room save ledger. `ProductionFacilityKnowledgeAuthority` allows another serviceable production station in the selected machine's claimed room to supply the required installed doctrine. The selected machine, broken stations, stations outside the room, and unclaimed work areas cannot provide this shared facility source.

`ProductionKnowledgeSourceAuthority` now reports player, selected-machine, and claimed-facility sources through one execution-shared result. Facility doctrine can reveal a recipe, satisfy the manual Craft execution gate, contribute its knowledge-quality tier, and flow into the existing knowledge-provider provenance field. Added `Milestone03ProductionFacilityKnowledgeSmoke`, expanded the Production Forecast reference and Infopedia guard, and wired the smoke into Gate 3.

Verification: fresh Java 17 full-tree compile passed; focused facility-knowledge, machine-knowledge, quality-provenance, production-readability, and Infopedia smokes passed; the complete `Gate3PlayerFacingTextSmokeSuite` passed. Function and Mermaid maps refreshed. The maintained `PACKAGE_client` rebuilt successfully; the Java 17 scan passed for 2,994 classfiles at highest major version 61; the packaged client stayed alive for the full eight-second boot-smoke window. Final JAR SHA-256: `1006F4A9E1B3E8166BD0218F3D88261AFBBB53554722D812B7395489261105E8`. The repository manifest remains pending after the prior generator timeouts.

## Milestone 03 - Manual Operator Quality Cap Slice

Activated manual operator skill as an execution-shared production-quality cap. `ProductionOperatorSkillAuthority` now maps the recipe's existing core-stat resolution into visible quality support: novice operators support Common output, practiced operators Serviceable, skilled operators Fine, and expert operators Masterwork. The existing defect-risk adjustments remain active alongside the new cap.

`ProductionQualityTraceAuthority` now carries and names the operator tier with doctrine, recipe, machine, material, facility, and equipped-tool limits. Live recipe forecasts, compact status quality, and final Craft execution resolve the operator before output creation, while provenance continues to preserve the operator skill and band. Queued assigned-worker behavior remains separate and dormant until its own execution owner is implemented.

Expanded `Milestone03ProductionOperatorSkillSmoke`, `Milestone03ProductionQualityTraceSmoke`, batch/provenance fixtures, Production Forecast guidance, and the Infopedia guard. Verification: fresh Java 17 full-tree compile passed; focused operator-skill, quality-trace, batch, provenance, production-readability, and Infopedia smokes passed; the complete `Gate3PlayerFacingTextSmokeSuite` passed. Function and Mermaid maps refreshed. The maintained `PACKAGE_client` rebuilt successfully; the Java 17 scan passed for 2,994 classfiles at highest major version 61; the packaged client stayed alive for the full eight-second boot-smoke window. Final JAR SHA-256: `ABA7B73BEAA377B664B00D1B322E4AB781EDA5A82C5A981D57245964EB41EC82`. The repository manifest remains pending after the prior generator timeouts.

## Milestone 03 - Manual Production Fatigue Pressure Slice

Connected the existing body-readiness fatigue bands to immediate manual production through `ProductionFatiguePressureAuthority`. Ready operators receive no fatigue defect adjustment; slightly tired operators add two defect points; tired operators add five; and the established exhausted band at 75 fatigue blocks manual machinery operation until the player rests. The forecast shows current and projected fatigue before materials are consumed.

`ProductionBatchAuthority` now combines operator skill and live fatigue pressure into one batch defect risk. Final Craft execution resolves that pressure before the run, uses it for the shared batch inspection, and preserves it in item provenance through append-only field 24, legacy decoding, and transfers. Added `Milestone03ProductionFatiguePressureSmoke`, expanded Production Forecast guidance and its Infopedia guard, and wired the smoke into Gate 3.

Verification: fresh Java 17 full-tree compile passed; focused fatigue-pressure, batch, operator, provenance, production-readability, and Infopedia smokes passed; the complete `Gate3PlayerFacingTextSmokeSuite` passed. Function and Mermaid maps refreshed. The maintained `PACKAGE_client` rebuilt successfully; the Java 17 scan passed for 3,000 classfiles at highest major version 61; the packaged client stayed alive for the full eight-second boot-smoke window. Final JAR SHA-256: `86F3B91FE38B31286FF9642E8271A38D7822955B1E0E5AEE70BFF0F63E1EEA7B`. The repository manifest remains pending after the prior generator timeouts.

## Milestone 03 - Producing Room and Facility Provenance Slice

Added `ProductionLocationAuthority` so immediate manual Craft resolves its producing room and facility before materials are consumed. When world room metadata exists, the forecast names the room profile and room ID; otherwise a selected machine inside the claimed base retains the claimed production-room identity. Work outside that boundary is recorded as an unclaimed world workspace rather than inheriting the player's base identity.

Crafted-item provenance now preserves producing room and producing facility separately through append-only fields 25 and 26, legacy decoding, and inventory/storage transfers. This advances the provenance ledger without inventing a blueprint ownership or facility-save subsystem. Added `Milestone03ProductionLocationProvenanceSmoke`, expanded Production Forecast guidance and its Infopedia guard, and wired the smoke into Gate 3.

Verification: fresh Java 17 full-tree compile passed; focused production-location, fatigue-pressure, quality-provenance, production-readability, and Infopedia smokes passed; the complete `Gate3PlayerFacingTextSmokeSuite` passed. Function and Mermaid maps refreshed. The maintained `PACKAGE_client` rebuilt successfully; the Java 17 scan passed for 3,006 classfiles at highest major version 61; the packaged client stayed alive for the full eight-second boot-smoke window. Final JAR SHA-256: `B4A1A8ADF3669A1787E22257FA50C53D2AC97634B35C09A16B37D7EC220E41BD`. The repository manifest remains pending after the prior generator timeouts.

## Milestone 03 - Producing Machine Identity Provenance Slice

Added `ProductionMachineIdentityAuthority` so immediate manual Craft identifies the exact selected station by its player-facing name, machine role, and coordinates. Forecasts now show that station identity alongside room and facility origin, allowing two same-quality machines to remain distinguishable without inventing a new machine-ID save subsystem.

Crafted-item provenance preserves the producing machine through append-only field 27, legacy decoding, and inventory/storage transfers. Added `Milestone03ProductionMachineProvenanceSmoke`, expanded quality-provenance and Infopedia guards, and wired the smoke into Gate 3.

Verification: fresh Java 17 full-tree compile passed; focused producing-machine, quality-provenance, production-location, production-readability, and Infopedia smokes passed; the complete `Gate3PlayerFacingTextSmokeSuite` passed. Function and Mermaid maps refreshed. The maintained `PACKAGE_client` rebuilt successfully; the Java 17 scan passed for 3,012 classfiles at highest major version 61; the packaged client stayed alive for the full eight-second boot-smoke window. Final JAR SHA-256: `E3FF92E11259C44E9012383E49EB8A3000FAEC2545D777A4C5EEC19110EEED7F`.

## Milestone 03 - Producing Operator Identity Provenance Slice

Added `ProductionOperatorIdentityAuthority` so immediate manual Craft forecasts and provenance identify the character who actually performed the run. The operator identity is separate from the existing skill and skill-band records, allowing later contracts, investigations, and item inspection to distinguish who made an item from how capable they were.

Crafted-item provenance preserves the producing operator through append-only field 28, legacy decoding, and inventory/storage transfers. Assigned workers and supervisors remain explicitly outside this manual-Craft slice until a queued staffed-production owner executes their work. Added `Milestone03ProductionOperatorProvenanceSmoke`, expanded quality-provenance and Infopedia guards, and wired the smoke into Gate 3.

Verification: fresh Java 17 full-tree compile passed; focused operator-provenance, machine-provenance, quality-provenance, production-readability, and Infopedia smokes passed; the complete `Gate3PlayerFacingTextSmokeSuite` passed. Function and Mermaid maps refreshed. The maintained `PACKAGE_client` rebuilt successfully; the Java 17 scan passed for 3,018 classfiles at highest major version 61; the packaged client stayed alive for the full eight-second boot-smoke window. Final JAR SHA-256: `2491FA6307BCE4D09BDDE44E8A0BF05DDFCA26A71839493B89A7AB8B78A410D0`.

## Milestone 03 - Manual Craft Operation History Slice

Connected successful immediate manual Craft actions to the existing conservative `ProductionQueueRecordBridge`. After the live Craft path has consumed inputs, created outputs, applied wear and fatigue, awarded experience, and advanced through its full turn cost, it now records one completed operation in shared `MachineOperationQueue` history with the operator, producing station, recipe, output count, duration, and final completion turn.

This does not transfer outcome authority to the queue. The operation history remains audit and status metadata only, preventing duplicate consumption or output creation while making completed production visible to shared operation diagnostics and persistence. Added `Milestone03ManualProductionOperationRecordSmoke`, expanded the Production Forecast reference, and wired the smoke into Gate 3.

Verification: fresh Java 17 full-tree compile passed; focused manual-operation-record, operator-provenance, production-readability, and Infopedia smokes passed; the complete `Gate3PlayerFacingTextSmokeSuite` passed. Function and Mermaid maps refreshed. The maintained `PACKAGE_client` rebuilt successfully; the Java 17 scan passed for 3,020 classfiles at highest major version 61; the packaged client stayed alive for the full eight-second boot-smoke window. Final JAR SHA-256: `A073968EF7DFD95047CE794C957FCF5C50BB51AC64D92B95A4C05793B1AE913C`.

## Milestone 03 - Staffed Generated Production Execution Slice

Added `StaffedProductionExecutionAuthority` as the first bounded execution owner for queued staffed generated production. A machine with an existing generated assignment, assigned worker, claimed-room authorization, required knowledge, ready concrete inputs, and remaining queue count can complete one staffed run. The run consumes inputs through the existing production-container route, places output into base storage, preserves item provenance, applies bounded machine wear, decrements the machine queue, and records the completed operation through `ProductionQueueRecordBridge`.

This keeps outcome ownership narrow: it executes one validated run and does not create an open-ended background simulator or a second inventory system. Machine-operation status now reports whether the selected staffed assignment is ready or blocked. Added `Milestone03StaffedProductionExecutionSmoke`, expanded the Production Forecast reference, and wired the smoke into Gate 3.

Verification: fresh Java 17 full-tree compile passed; focused staffed-execution and Infopedia smokes passed; the complete `Gate3PlayerFacingTextSmokeSuite` passed. Function and Mermaid maps refreshed. The maintained `PACKAGE_client` rebuilt successfully; the Java 17 scan passed for 3,026 classfiles at highest major version 61; the packaged client stayed alive for the full eight-second boot-smoke window. Final JAR SHA-256: `087FA9F65D36B91D063A5626E0FF2A9A50B15C2AB1B1D11BB7D03A46A98D9C13`.

## Milestone 03 - Production Workforce Mode Provenance Slice

Added `ProductionWorkforceModeAuthority` and append-only provenance field 29 so produced items distinguish immediate manual Craft from staffed queued production. Manual Craft records the immediate operator mode by default; staffed generated-production output overrides the same field with the assigned-worker staffed mode.

The field survives save/load decoding and inventory/storage transfer, giving later contracts, investigations, and inspection text a stable workforce-mode hook without inventing supervisor mechanics. Added `Milestone03ProductionWorkforceModeProvenanceSmoke`, expanded quality-provenance, staffed-execution, and Infopedia guards, and wired the smoke into Gate 3.

Verification: fresh Java 17 full-tree compile passed; focused workforce-mode, staffed-execution, quality-provenance, and Infopedia smokes passed; the complete `Gate3PlayerFacingTextSmokeSuite` passed. Function and Mermaid maps refreshed. The maintained `PACKAGE_client` rebuilt successfully; the Java 17 scan passed for 3,030 classfiles at highest major version 61; the packaged client stayed alive for the full eight-second boot-smoke window. Final JAR SHA-256: `BD5075BBC0D482ECF551E8BD1E7F209E5FADFA1374D5D1E5EF430573028D2E89`.

## Milestone 03 - Generated Production Legal Status Provenance Slice

Added `ProductionLegalStatusAuthority` and append-only provenance field 30 so staffed generated-production output preserves the variant law/status classification already used by production access rules. Lawful issue, restricted stock, gray-market, black-market, contraband, profaned, and hostile-identity labels now survive item inspection after save/load and transfer.

This is deliberately provenance only: it does not invent law-enforcement, seizure, corruption, or faction-reputation consequences before those owners exist. Staffed generated production writes the field from `FactionRecipeVariant.lawStatus`; manual Craft leaves it absent unless a future manual recipe law source is implemented. Added `Milestone03ProductionLegalStatusProvenanceSmoke`, expanded staffed-execution and Infopedia guards, and wired the smoke into Gate 3.

Verification: fresh Java 17 full-tree compile passed; focused legal-status, staffed-execution, and Infopedia smokes passed; the complete `Gate3PlayerFacingTextSmokeSuite` passed. Function and Mermaid maps refreshed after one transient master-map write retry. The maintained `PACKAGE_client` rebuilt successfully; the Java 17 scan passed for 3,034 classfiles at highest major version 61; the packaged client stayed alive for the full eight-second boot-smoke window. Final JAR SHA-256: `BB6B6BA3D79F3F68B25FD22DA092D2A5603AFAA6A1A287B2862B96BC0337D370`.

## Milestone 03 - Generated Production Source Provenance Slice

Added `ProductionSourceProvenanceAuthority` and append-only provenance field 31 so staffed generated-production output preserves the generated recipe source, base note, and variant note that produced the item. This gives later inspections and investigations a stable source hook without claiming blueprint ownership or supervisor authorship before those owners exist.

Staffed generated production writes this field from `FactionRecipeVariant.base.source`, `base.note`, and `productionNote`; manual Craft leaves it absent unless a future manual recipe-source owner is implemented. Added `Milestone03ProductionSourceProvenanceSmoke`, expanded staffed-execution and Infopedia guards, and wired the smoke into Gate 3.

Verification: fresh Java 17 full-tree compile passed; focused source-provenance, staffed-execution, and Infopedia smokes passed; the complete `Gate3PlayerFacingTextSmokeSuite` passed. Function and Mermaid maps refreshed. The maintained `PACKAGE_client` rebuilt successfully; the Java 17 scan passed for 3,038 classfiles at highest major version 61; the packaged client stayed alive for the full eight-second boot-smoke window. Final JAR SHA-256: `CFB1EF8921A16DE07A1770D15E56C3502D37DE6CF0C493130776DC8EFE80B028`.

## Milestone 03 - Production Batch Issue Tags Slice

Added `ProductionBatchIssueAuthority` and append-only provenance field 32 so produced items can preserve Phase 9.3 batch issue tags. Manual production derives good or defective batch tags from the existing inspection disposition and adds evidence-backed contaminated, unstable, restricted, stolen-risk, counterfeit, or faction-certified tags from recipe and production metadata when those signals are present.

Staffed generated production records variant-level issue tags from law status, faction, source, and production notes when those signals are present, even though it does not yet roll a per-run manual batch inspection. This is provenance and readability only: the existing defect appraisal remains the only current gameplay consequence, so item statistics, law enforcement, contamination effects, recalls, and counterfeit enforcement remain reserved for later owners. Added `Milestone03ProductionBatchIssueTagsSmoke`, expanded batch, staffed-execution, quality-provenance, and Infopedia guards, and wired the smoke into Gate 3.

Verification: fresh Java 17 full-tree compile passed; focused batch-issue, batch-provenance, quality-provenance, staffed-execution, and Infopedia smokes passed; the complete `Gate3PlayerFacingTextSmokeSuite` passed. Function and Mermaid maps refreshed. The maintained `PACKAGE_client` rebuilt successfully; the Java 17 scan passed for 3,042 classfiles at highest major version 61; the packaged client stayed alive for the full eight-second boot-smoke window. Final JAR SHA-256: `D63F0764ACD360172FA4CDD4B9FBE1883F77B66E4F07AEBDF5BF2732ED7BE154`.

## Milestone 03 - Production Repair History Provenance Slice

Added `MachineRepairHistoryAuthority` and append-only provenance field 33 so items produced on a repaired machine can preserve the field-repair note that existed on that machine at production time. The existing owned-machine repair action now records a compact machine repair history line with turn, actor, machine, integrity change, and machine-part cost.

`BaseObject` persistence now carries the machine repair note as an append-only save field, and produced-item provenance copies it through save/load and transfer. This intentionally records repaired-machine provenance only; item modification history remains empty until a real item-modification owner exists. Added `Milestone03ProductionRepairHistoryProvenanceSmoke`, expanded the Production Forecast reference, and wired the smoke into Gate 3.

Verification: fresh Java 17 full-tree compile passed; focused repair-history, machine-repair, quality-provenance, and Infopedia smokes passed; the complete `Gate3PlayerFacingTextSmokeSuite` passed. Function and Mermaid maps refreshed. The maintained `PACKAGE_client` rebuilt successfully; the Java 17 scan passed for 3,046 classfiles at highest major version 61; the packaged client stayed alive for the full eight-second boot-smoke window. Final JAR SHA-256: `40F483BF125B27ECAD60E7F0439566A85E5219C994D4F1ECED0432085E002C99`.

## Milestone 03 - Skill Tree Progression Readability Slice

Added `SkillTreeProgressionAuthority` as the first Phase 15 skill-tree ownership surface. It defines player-facing skill branches, capability-bearing nodes, XP costs, prerequisites, visible effects, and explicit skill-versus-knowledge boundaries. Initial branches include fabrication and repair, machine operation, trade and appraisal, investigation and examination, and leadership and faction command.

This is deliberately readable and auditable before it mutates saves: spending UI, permanent unlocked-skill persistence, trainer gating, and stat mutation remain future owners. Added `Milestone03SkillTreeProgressionReadabilitySmoke`, expanded the Infopedia with a Skill Progression entry, and wired the smoke into Gate 3.

Verification: fresh Java 17 full-tree compile passed; focused skill-tree progression and Infopedia smokes passed; the complete `Gate3PlayerFacingTextSmokeSuite` passed. Function and Mermaid maps refreshed. The maintained `PACKAGE_client` rebuilt successfully; the Java 17 scan passed for 3,054 classfiles at highest major version 61; the packaged client stayed alive for the full eight-second boot-smoke window. Final JAR SHA-256: `6F3D8911B07C1BE22E77E69AABBA03345DA736BAF70FD7A19CD549A1CC6B4BDA`.

## Milestone 03 - Skill Tree Spending Persistence Slice

Added the first durable skill-node spending path. `SkillTreeProgressionAuthority` now validates node lookup, XP cost, duplicate unlocks, and prerequisite nodes, then returns a bounded spend result. `GamePanel` now carries `unlockedSkillNodes` separately from `unlockedKnowledges`, and save/load persistence stores skill nodes in `run.skillNodes` without granting or consuming knowledge credits.

Added player-rank console routes `skill_status` and `skill_unlock <node id>` so XP can be spent on capability nodes before a full character-screen skill UI exists. This keeps Phase 15 moving while preserving the Knowledge Tree boundary: spending XP on a skill never teaches recipe doctrine. Added `Milestone03SkillTreeSpendingPersistenceSmoke`, expanded the Skill Progression Infopedia wording, and wired the smoke into Gate 3.

Verification: `javac --release 17`, `Milestone03SkillTreeSpendingPersistenceSmoke`, `Milestone03SkillTreeProgressionReadabilitySmoke`, `Milestone02InfopediaMechanicsReadabilitySmoke`, and `Gate3PlayerFacingTextSmokeSuite` pass. Rebuilt `PACKAGE_client` with `scripts\PACKAGE_CLIENT_WINDOWS.ps1 -CleanClasses -BuildJar`, passed Java 17 classfile scan across 3058 classfiles, and passed the 8-second package boot smoke. Package jar SHA-256: `B00DC9076926C4053A8C12766117266353B591464DCFD5161920DC53450B722E`.

## Milestone 03 - Skill Tree Access Gate Slice

Added world-access validation to the skill-tree spending authority. Skill nodes can now declare access requirements for unlocked knowledge, faction standing, trainers, facilities, or equipment, and the player console route evaluates those gates from live game state before spending XP. Basic nodes remain spendable by XP and prerequisite alone; advanced nodes can now refuse with a player-facing missing-access reason instead of silently acting like detached menu upgrades.

Added gated example nodes for workshop fabrication, certified market appraisal, and supervisor authorization. The live game context derives facility access from owned base objects, faction access from faction standing, equipment access from carried/equipped/storage items, and knowledge access from the existing Knowledge Tree state. Trainer tokens remain explicit context entries until trainer NPC ownership exists. Added `Milestone03SkillTreeAccessGateSmoke`, expanded skill-tree readability and Infopedia wording, and wired the smoke into Gate 3.

Verification: `javac --release 17`, `Milestone03SkillTreeAccessGateSmoke`, `Milestone03SkillTreeProgressionReadabilitySmoke`, `Milestone03SkillTreeSpendingPersistenceSmoke`, `Milestone02InfopediaMechanicsReadabilitySmoke`, and `Gate3PlayerFacingTextSmokeSuite` pass. Rebuilt `PACKAGE_client` with `scripts\PACKAGE_CLIENT_WINDOWS.ps1 -CleanClasses -BuildJar`, passed Java 17 classfile scan across 3062 classfiles, and passed the 8-second package boot smoke. Package jar SHA-256: `ED4BFDF379DF9D67F57AD03D57B01AEBF506D09A7E53BBABBE95559B041BEBC9`.

## Milestone 03 - Skill Tree Stat Gate and Effect Slice

Added stat requirements and bounded stat effects to the skill-tree authority. Skill nodes now carry separate stat requirement and stat effect fields, so a node can require a live character stat threshold before XP spending and can apply a clamped stat increase only after a successful unlock. The player console route now passes the active candidate's stats into skill spending and applies the returned stat effect in the same transaction that spends XP and records the unlocked node.

Added stat-gated machine-operation nodes, including Pressure Discipline, and upgraded the advanced workshop node to require both a fabrication facility and Mechanics 8. Stat effects mutate the existing `Candidate.stats` map, so persistence continues through the established `char.stats` save path instead of adding a parallel character progression ledger. Added `Milestone03SkillTreeStatGateSmoke`, expanded skill-tree readability and Infopedia wording, and wired the smoke into Gate 3.

Verification: `javac --release 17`, `Milestone03SkillTreeStatGateSmoke`, `Milestone03SkillTreeAccessGateSmoke`, `Milestone03SkillTreeProgressionReadabilitySmoke`, `Milestone03SkillTreeSpendingPersistenceSmoke`, `Milestone02InfopediaMechanicsReadabilitySmoke`, and `Gate3PlayerFacingTextSmokeSuite` pass. Rebuilt `PACKAGE_client` with `scripts\PACKAGE_CLIENT_WINDOWS.ps1 -CleanClasses -BuildJar`, passed Java 17 classfile scan across 3064 classfiles, and passed the 8-second package boot smoke. Package jar SHA-256: `B3A2D3430D696E90A607970E48A97371E13E51F544EE2F453E4AA50147E4AD3B`.

## Boot Menu Studio Splash and Music Delay Slice

Added a nine-second boot-menu hold before main-menu music can start. The boot sequence now advertises placeholder stages for a studio intro and logo splash, keeps the visual boot/menu handoff under `BootMenuFlowAuthority`, and gates `MAIN_MENU` music through a timer-backed helper so skipping the boot screen does not start menu music before the nine-second mark.

Added `BootMenuMusicDelaySmoke` as a non-audio timing contract and wired it into Gate 3 for future aggregate runs. Also added a local-audio testing reminder to `STANDARDS_AND_PRACTICES.md`: prefer non-audio compile/focused smokes for boot timing, and do not run local audible boot/package smoke more than once unless explicitly requested.

Verification: one local Java 17 compile plus the non-audio `BootMenuMusicDelaySmoke` passed. No local GUI/package boot smoke was run for this slice.

## Milestone 03 - Skill Tree Mutual Specialization Slice

Added mutually exclusive specialization groups to the skill-tree authority. Skill nodes now carry an explicit exclusivity boundary, and XP spending refuses a node when a sibling specialization in the same group is already unlocked. The status surface includes the exclusive group so the player-facing console can show why related specializations are locked instead of hiding the rule behind a failed spend.

Added the first trade-appraisal specialization pair: Certified Market Appraisal for formal faction-facing certificates and Streetwise Appraisal for practical stolen-risk, counterfeit, and fence-market judgment. These share `trade-appraisal-specialization`, so the character can choose one lane after Batch Appraisal rather than stacking both appraisal identities. Added `Milestone03SkillTreeMutualExclusionSmoke`, expanded skill-tree readability and Infopedia wording, and wired the smoke into Gate 3.

Verification: `javac --release 17`, `Milestone03SkillTreeMutualExclusionSmoke`, `Milestone03SkillTreeProgressionReadabilitySmoke`, `Milestone03SkillTreeSpendingPersistenceSmoke`, and `Gate3PlayerFacingTextSmokeSuite` pass. Function and Mermaid maps refreshed after one transient master-map write retry. Rebuilt `PACKAGE_client` with `scripts\PACKAGE_CLIENT_WINDOWS.ps1 -CleanClasses -BuildJar` and passed Java 17 classfile scan across 3068 classfiles. Package jar SHA-256: `2EA044737F67AD1639E5DD79C1D198EE7BACC5DB65D2DE6E519D6202579479B0`. Local GUI/package boot smoke was not run.

## Milestone 03 - Skill Tree Capability Hook Slice

Added queryable capability hooks to the skill-tree authority. Skill nodes now expose a durable capability key plus optional passive bonus and active ability labels, so future production, inspection, trade, machine, combat, or social systems can ask for trained capabilities directly instead of scraping player-facing prose.

Unlocked skill nodes now produce `capabilityKeys`, `passiveBonusLines`, `activeAbilityLines`, and a `hasCapability` helper. Initial hooks include `machine-readiness-preview` as an active ability, `production-limiter-readability:+1` as a passive production-inspection bonus, and `street-market-risk-appraisal` as an active street-trade appraisal ability. Added `Milestone03SkillTreeCapabilityHooksSmoke`, expanded skill-tree readability and Infopedia wording, and wired the smoke into Gate 3.

Verification: `javac --release 17`, `Milestone03SkillTreeCapabilityHooksSmoke`, `Milestone03SkillTreeProgressionReadabilitySmoke`, and `Gate3PlayerFacingTextSmokeSuite` pass. Function and Mermaid maps refreshed. Rebuilt `PACKAGE_client` with `scripts\PACKAGE_CLIENT_WINDOWS.ps1 -CleanClasses -BuildJar` and passed Java 17 classfile scan across 3070 classfiles. Package jar SHA-256: `CDD80D62DF85A9BD9A5F71AD9967CC6986F97A66AC06F7D34E6189DA84867B97`. Local GUI/package boot smoke was not run.

## Milestone 03 - Production Skill Capability Preview Slice

Connected unlocked skill-tree capability hooks to the manual production preview surface. `ProductionReadabilityAuthority` now adds bounded skill capability context from `GamePanel.unlockedSkillNodes`, including passive production-inspection hooks and active machine-operation abilities, so the player can see trained capabilities beside knowledge, operator, material, machine, and facility explanations.

This slice is intentionally read-only for production outcomes: the preview says the hooks are context only until a later consuming authority applies them to execution math. Added `Milestone03ProductionSkillCapabilityPreviewSmoke` and wired it into Gate 3 so skill capability hooks remain visible without silently changing output quality or defect calculations.

Verification: `javac --release 17`, `Milestone03ProductionSkillCapabilityPreviewSmoke`, `Milestone03SkillTreeCapabilityHooksSmoke`, `Milestone02ProductionReadabilitySmoke`, and `Gate3PlayerFacingTextSmokeSuite` pass. Function and Mermaid maps refreshed. Rebuilt `PACKAGE_client` with `scripts\PACKAGE_CLIENT_WINDOWS.ps1 -CleanClasses -BuildJar` and passed Java 17 classfile scan across 3072 classfiles. Package jar SHA-256: `5FB25D19816F09C71ED811860E3C478551394CB1DD405AACB04D4716592E3630`. Local GUI/package boot smoke was not run.

## Milestone 03 - Trade Skill Appraisal Bridge Slice

Connected the skill-tree `Streetwise Appraisal` capability to visible trade appraisal text. Sale previews can now receive unlocked skill nodes and, when the street-appraisal specialization is trained, call out recorded defect, counterfeit, stolen-risk, restricted, or black-market provenance before the player confirms a sale.

The bridge is intentionally bounded: Streetwise Appraisal improves risk detection and buyer-facing explanation, but it does not override protected evidence hand-ins, invent law-enforcement consequences, or alter the existing defect resale math. The live trade panel now passes `unlockedSkillNodes` into sale preview, while the existing three-argument preview remains available for older tests and callers. Added `Milestone03TradeSkillAppraisalSmoke` and wired it into Gate 3.

Verification: `javac --release 17`, `Milestone03TradeSkillAppraisalSmoke`, `Milestone02TradeReadabilitySmoke`, `Milestone03ProductionDefectAppraisalSmoke`, `Milestone03SkillTreeCapabilityHooksSmoke`, and `Gate3PlayerFacingTextSmokeSuite` pass. Function and Mermaid maps refreshed. Rebuilt `PACKAGE_client` with `scripts\PACKAGE_CLIENT_WINDOWS.ps1 -CleanClasses -BuildJar` and passed Java 17 classfile scan across 3074 classfiles. Package jar SHA-256: `9F1960D7274E6C995C8DA70BC50127104FA2F4301034C1A996623696631643C4`. Local GUI/package boot smoke was not run.

## Milestone 03 - Certified Trade Appraisal Bridge Slice

Connected the formal side of trade appraisal to sale preview text. `Certified Market Appraisal` can now recognize faction-certified batch proof and recorded legal status as formal trade evidence, while untrained characters see that a certificate exists but cannot fully separate formal proof from ordinary item naming.

The bridge remains preview/readability only. It does not bypass faction access, protected hand-ins, buyer policy, law-enforcement ownership, or the existing defect resale appraisal. Added `Milestone03TradeCertifiedAppraisalSmoke` and wired it into Gate 3 beside the Streetwise Appraisal smoke, preserving the mutual-specialization split between institutional certification and informal street judgment.

Verification: `javac --release 17`, `Milestone03TradeCertifiedAppraisalSmoke`, `Milestone03TradeSkillAppraisalSmoke`, `Milestone02TradeReadabilitySmoke`, `Milestone03SkillTreeMutualExclusionSmoke`, and `Gate3PlayerFacingTextSmokeSuite` pass. Function and Mermaid maps refreshed. Rebuilt `PACKAGE_client` with `scripts\PACKAGE_CLIENT_WINDOWS.ps1 -CleanClasses -BuildJar` and passed Java 17 classfile scan across 3076 classfiles. Package jar SHA-256: `232BD430EC87C5E012EEE9F03B1E38CBDF60E90D1A1C46D509EEDE5C3D0EB4E0`. Local GUI/package boot smoke was not run.

## Milestone 03 - Contract Skill Proof Readability Slice

Connected skill and knowledge proof readiness to contract objective summaries. `ContractObjectiveReadabilityAuthority` now has a skill-aware overload that can explain whether a contract-relevant capability, such as Certified Market Appraisal, Investigation Trace Reading, Streetwise Appraisal, or fabrication inspection, is trained, and whether related knowledge such as Contract Negotiation is known.

The map/objective panel now passes live `unlockedSkillNodes` and `unlockedKnowledges` into the contract summary. This is deliberately readable proof only: contract completion, reward payout, and turn-in rules remain owned by the existing contract flow. Added `Milestone03ContractSkillProofSmoke` and wired it into Gate 3.

Verification: `javac --release 17`, `Milestone03ContractSkillProofSmoke`, `Milestone02ContractObjectiveReadabilitySmoke`, `Milestone03TradeCertifiedAppraisalSmoke`, `Milestone03SkillTreeCapabilityHooksSmoke`, and `Gate3PlayerFacingTextSmokeSuite` pass. Function and Mermaid maps refreshed. Rebuilt `PACKAGE_client` with `scripts\PACKAGE_CLIENT_WINDOWS.ps1 -CleanClasses -BuildJar` and passed Java 17 classfile scan across 3078 classfiles. Package jar SHA-256: `BF8CD5BF3DA5F3438B1AA9443153545C900443FA53F25F2036C7097D7264A29A`. Local GUI/package boot smoke was not run.

## Milestone 03 - Contract Skill Proof Infopedia Slice

Expanded the Contract Objectives and Evidence Infopedia entry so the new contract skill-proof lines are not hidden behavior. The entry now explains that contract summaries can show skill and knowledge proof readiness for Certified Market Appraisal, Investigation Trace Reading, Streetwise Appraisal, fabrication inspection, Contract Negotiation, and Scrap-Forging Doctrine where a contract implies those proof lanes.

The reference keeps the ownership boundary explicit: skill proof does not complete contracts, pay rewards, bypass hand-ins, or reveal hidden target identity. Added `Milestone03ContractSkillProofInfopediaSmoke`, strengthened `Milestone02InfopediaMechanicsReadabilitySmoke`, and wired the focused smoke into Gate 3.

Verification: `javac --release 17`, `Milestone03ContractSkillProofInfopediaSmoke`, `Milestone02InfopediaMechanicsReadabilitySmoke`, `Milestone03ContractSkillProofSmoke`, and `Gate3PlayerFacingTextSmokeSuite` pass. Function and Mermaid maps refreshed. Rebuilt `PACKAGE_client` with `scripts\PACKAGE_CLIENT_WINDOWS.ps1 -CleanClasses -BuildJar` and passed Java 17 classfile scan across 3080 classfiles. Package jar SHA-256: `618EF12512795F5C26160F01FC86B3D16B8364D246693BE861D6AD6349A6117C`. Local GUI/package boot smoke was not run.

## Milestone 02 - Character Paper Doll and Equipment Slot Restoration

Restored the player-character paper doll and exact limb hit-point/status display to the live character panel. The repair reuses `Candidate.body`, `BodyPart`, `BodyConditionReadabilityAuthority`, and existing equipment state rather than creating duplicate health data. The character panel now expands for the body display, colors each tracked region by condition, prints current/max hit points, and lists every tracked limb with its readable state.

Replaced the two blind hand-only unequip buttons with selectable Left Hand, Right Hand, and Body Protection slots. The selected equipped item receives a detail/icon surface and can be unequipped explicitly; hand slots route through the existing unequip implementation, while body protection is returned to carried inventory before the clothing slot is cleared. Added `CharacterPaperDollAuthority` and `Milestone02CharacterPaperDollSmoke`, with the smoke wired into Gate 3.

Verification: Java 17 UTF-8 full-source compile, focused paper-doll smoke, and complete Gate 3 smoke suite in GitHub Actions. Manual GUI review remains required for supported window sizes.

## Milestone 02 - Full Character Equipment and Medical Body Tabs

Expanded the restored character panel into Overview, Equipment, and Medical tabs. The Equipment tab now exposes Headgear, Underclothes, Clothes/Body, Gloves, Boots, Backpack, two Ring slots, two Accessory slots, and both Hand slots. It includes a live paper doll, selectable slots, carried-item navigation, compatibility feedback, selected-item icon/detail presentation, equip replacement, and explicit unequip-to-inventory behavior. Backpacks now contribute a visible additive carry-capacity bonus while preserving the existing Strength/world-settings capacity authority.

Added `CharacterEquipmentAndMedicalAuthority` as the narrow loadout and body-modification contract rather than embedding another subsystem in the legacy panel bridge. Existing hand and clothing fields remain authoritative for their established mechanics; new wearable slots live in an enum-keyed map. The Medical tab mirrors every tracked `Candidate.body` region and reserves independent Mutation, Modification, and Cybernetic records for each region. It intentionally does not invent surgery, rejection, power, maintenance, mutation, or wireless cybernetic mechanics; future systems can bind through `installCharacterMedicalRecord(...)` and `MedicalSlotKey.storageKey()` without redesigning the screen. Cybernetic presentation assumes isolated direct-interface hardware rather than wireless control.

Added property persistence hooks for the new wearable and medical maps where the existing `Persistence.writeCore/readCore` authority is available. Added focused authority and live GamePanel integration smokes, including equip/unequip transfer, backpack capacity, anatomical region mapping, and future medical-record binding. Verification uses Java 17 UTF-8 full-source compilation and the complete Gate 3 suite.

## Milestone 03 - Contract Skill Proof Audit Slice

Advanced Phase 18.1 by giving contract skill and knowledge proof readiness a dedicated audit surface beside the player-facing objective summary. `ContractObjectiveReadabilityAuthority.auditLines(...)` reports the owning authority, active/shown contract counts, readiness states for inferred skill and knowledge proof, evidence route confidence, and the explicit boundaries that proof audit does not complete contracts or mutate rewards.

The audit remains safe for ordinary surfaces: this slice records `rawIdsHidden=true` and verifies that contract IDs and target entity IDs do not leak while still showing useful named proof states such as Certified Market Appraisal and Contract Negotiation. Added `Milestone03ContractSkillProofAuditSmoke`, referenced it from the Contract Objectives and Evidence Infopedia entry, and wired the focused smoke into Gate 3.

Manifest generation was repaired after the legacy PowerShell hasher stalled on the full 153,289-file, 5.6 GB workspace. `ROOT_tools/update-repository-file-manifest.ps1` now defaults to an incremental Python generator that preserves the existing seven-column `ROOT_docs/REPOSITORY_FILE_MANIFEST.tsv` schema, reuses hashes when path, size, and UTC modified time match, and retains `-ForceHash` plus `-LegacyFullHash` escape hatches for full validation.

Verification: `javac --release 17`, `Milestone03ContractSkillProofAuditSmoke`, `Milestone03ContractSkillProofInfopediaSmoke`, `Milestone03ContractSkillProofSmoke`, `Milestone02InfopediaMechanicsReadabilitySmoke`, and `Gate3PlayerFacingTextSmokeSuite` pass. Function and Mermaid maps refreshed. Rebuilt `PACKAGE_client` with `scripts\PACKAGE_CLIENT_WINDOWS.ps1 -CleanClasses -BuildJar` and passed Java 17 classfile scan across 3082 classfiles. Package jar SHA-256: `3AAB4F4201F11F1DE9B02514558D16565AF474FAFDA3B147E5A2E9762A57079F`. The incremental manifest refresh wrote 153,289 indexed rows, reused 150,165 existing hashes, hashed 3,124 changed or new files, and reported 0 hash errors. Local GUI/package boot smoke was not run.

## Milestone 03 - Skill Tree Definition Audit Slice

Advanced Phase 18.1 by expanding the skill-tree audit surface from a compact summary into structured branch and node definition lines. `SkillTreeProgressionAuthority.definitionAuditLines()` now reports the owning authority, branch and node counts, XP-cost coverage, dependency coverage, access-gate coverage, stat modifiers, exclusive groups, capability hooks, skill/knowledge separation, and an explicit ordinary-UI raw-ID boundary.

Each branch audit names its world-use purpose, and each node audit names its readable branch, XP cost, prerequisite, access requirement, stat requirement/effect, specialization group, capability hook, and knowledge boundary. The default audit keeps ordinary UI free of raw node IDs while still making Phase 18 editor/audit facts inspectable. Added `Milestone03SkillTreeDefinitionAuditSmoke`, expanded the Skill Progression Infopedia wording, and wired the focused smoke into Gate 3.

Verification: `javac --release 17`, `Milestone03SkillTreeDefinitionAuditSmoke`, `Milestone03SkillTreeProgressionReadabilitySmoke`, `Milestone03SkillTreeCapabilityHooksSmoke`, and `Gate3PlayerFacingTextSmokeSuite` pass. Function and Mermaid maps refreshed. Rebuilt `PACKAGE_client` with `scripts\PACKAGE_CLIENT_WINDOWS.ps1 -CleanClasses -BuildJar` and passed Java 17 classfile scan across 3084 classfiles. Package jar SHA-256: `1774E6489B2DBC92AE08EBD8DCB19E3B4E2ABEA7FD97758A23AB4BAAE8F4F53E`. The incremental manifest refresh wrote 154,842 indexed rows, reused 151,719 existing hashes, hashed 3,123 changed or new files, and reported 0 hash errors. Local GUI/package boot smoke was not run.

## Milestone 03 - Production Quality Definition Audit Slice

Advanced Phase 18.1 by adding a production quality definition audit to `ProductionQualityTraceAuthority`. The audit names the active quality cap inputs, limiter owner, batch owner, issue-tag owner, provenance owner, material/facility/tool/operator boundaries, and the worker-quality handoff boundary between immediate manual Craft and staffed queued production.

The audit also records the batch definition and consequence contract: one manual Craft action creates one batch ID and inspection disposition, defect appraisal can reduce ordinary resale value by 40%, and item statistics, law enforcement, contamination effects, recalls, and counterfeit enforcement remain future owners. Batch issue tags and provenance fields are listed as inspectable data definitions rather than hidden effects. Added `Milestone03ProductionQualityDefinitionAuditSmoke`, expanded the Production Forecast Infopedia wording, and wired the focused smoke into Gate 3.

Verification: `javac --release 17`, `Milestone03ProductionQualityDefinitionAuditSmoke`, `Milestone03ProductionQualityTraceSmoke`, `Milestone03ProductionBatchIssueTagsSmoke`, `Milestone02InfopediaMechanicsReadabilitySmoke`, and `Gate3PlayerFacingTextSmokeSuite` pass. Function and Mermaid maps refreshed. Rebuilt `PACKAGE_client` with `scripts\PACKAGE_CLIENT_WINDOWS.ps1 -CleanClasses -BuildJar` and passed Java 17 classfile scan across 3086 classfiles. Package jar SHA-256: `8943DB2BF92F0FDB4A3E1FFD19352A47871646440D171D06938A2335A066D46E`. The incremental manifest refresh wrote 156,396 indexed rows, reused 153,271 existing hashes, hashed 3,125 changed or new files, and reported 0 hash errors. Local GUI/package boot smoke was not run.

## Milestone 03 - Production Knowledge Source Audit Slice

Advanced Phase 18.1 by adding a production knowledge-source definition audit to `ProductionKnowledgeSourceAuthority`. The audit names the player knowledge, selected-machine doctrine, and claimed-facility doctrine sources; records that the effective knowledge set is a union of those valid sources; and keeps ordinary UI free of raw IDs.

The audit also records ownership boundaries for Teach Machine and claimed-room doctrine sharing: machines preserve one installed recipe doctrine only after the player knows it, and facility doctrine can come only from another serviceable production station in the same claimed production room. Broken providers, stations outside the room, and unclaimed workspaces do not share doctrine. Added `Milestone03ProductionKnowledgeSourceAuditSmoke`, expanded the Production Forecast Infopedia wording, and wired the focused smoke into Gate 3.

Verification: `javac --release 17`, `Milestone03ProductionKnowledgeSourceAuditSmoke`, `Milestone03MachineKnowledgeSourceSmoke`, `Milestone03ProductionFacilityKnowledgeSmoke`, `Milestone02InfopediaMechanicsReadabilitySmoke`, and `Gate3PlayerFacingTextSmokeSuite` pass. Function and Mermaid maps refreshed. Rebuilt `PACKAGE_client` with `scripts\PACKAGE_CLIENT_WINDOWS.ps1 -CleanClasses -BuildJar` and passed Java 17 classfile scan across 3088 classfiles. Package jar SHA-256: `F971ADEDAAF359BCB06422A758DADA460BE1D6FD22D1CF93920249FC992CF050`. The incremental manifest refresh wrote 157,951 indexed rows, reused 154,824 existing hashes, hashed 3,127 changed or new files, and reported 0 hash errors. Local GUI/package boot smoke was not run.

## Milestone 03 - Faction Production Mutation Audit Slice

Advanced Phase 18.1 by adding a faction production mutation definition audit to `ProductionFactionMutationAuthority`. The audit names `FactionManufacturingProfile` as the profile owner, `ItemProvenanceRecord` as the provenance owner, and records the visible formula inputs for value, charges, and defect pressure without creating a parallel faction-stat model.

The audit also records the effect boundary: faction mutation affects the existing output prefix, value, charges, and defect pressure, while law enforcement, reputation changes, seizure, corruption, and faction hostility remain future owners. Added `Milestone03ProductionFactionMutationAuditSmoke`, expanded the Production Forecast Infopedia wording, and wired the focused smoke into Gate 3.

Verification: `javac --release 17`, `Milestone03ProductionFactionMutationAuditSmoke`, `Milestone03ProductionFactionMutationSmoke`, `Milestone03QualityProvenanceSmoke`, `Milestone02InfopediaMechanicsReadabilitySmoke`, and `Gate3PlayerFacingTextSmokeSuite` pass. Function and Mermaid maps refreshed. Rebuilt `PACKAGE_client` with `scripts\PACKAGE_CLIENT_WINDOWS.ps1 -CleanClasses -BuildJar` and passed Java 17 classfile scan across 3090 classfiles. Package jar SHA-256: `AAB0B3AC785AAC9DB66CBABD9A73EB267B332655C6D42EA567C3862AA08964C5`. The incremental manifest refresh wrote 159,507 indexed rows, reused 156,378 existing hashes, hashed 3,129 changed or new files, and reported 0 hash errors. Local GUI/package boot smoke was not run.

## Milestone 03 - Blueprint Definition Audit Slice

Advanced Phase 18.1 by expanding `BlueprintConstructionAuthority` with a structured blueprint definition audit. The audit names the room-blueprint schema, relative cell offsets, anchors, object matrix, blueprint quality boundary, supported cell kinds, tile build recipe mapping, itemized cost and labor estimates, preflight rules, and the collisionless ghost-placement contract.

The audit is deliberately schema and preflight only: it does not place objects, consume materials, mutate room ownership, upgrade schematic quality, or bypass live placement validation. Added `Milestone03BlueprintDefinitionAuditSmoke`, expanded the Construction Blueprints Infopedia wording, and wired the focused smoke into Gate 3.

Verification: `javac --release 17`, `Milestone03BlueprintDefinitionAuditSmoke`, `Milestone02ConstructionReadabilitySmoke`, `Milestone02ConstructionCategorySmoke`, `Milestone02InfopediaMechanicsReadabilitySmoke`, and `Gate3PlayerFacingTextSmokeSuite` pass. Function and Mermaid maps refreshed. Rebuilt `PACKAGE_client` with `scripts\PACKAGE_CLIENT_WINDOWS.ps1 -CleanClasses -BuildJar` and passed Java 17 classfile scan across 3092 classfiles. Package jar SHA-256: `D5D3A90BB85FBF711440C85B93FB0CC854C83AA6D0739779C20E1B9D1780DA1B`. The incremental manifest refresh wrote 161,064 indexed rows, reused 157,933 existing hashes, hashed 3,131 changed or new files, and reported 0 hash errors. Local GUI/package boot smoke was not run.

## Milestone 03 - Quality Band Definition Audit Slice

Advanced Phase 18.1 by adding a shared quality-band definition audit to `QualityAuthorityApi`. The audit names `QualityAuthorityApi` as the quality profile owner, `ItemQuality` as the item prefix/value/charge owner, the full item-quality order from Junk through Archeotech, and the doctrine-band order used by the Knowledge Tree.

The audit records the key boundary that Shoddy is a degradation quality rather than a target doctrine school, while Common remains the civic baseline for missing or ordinary doctrine. It also names item value/charge multipliers, production profile meanings, and the central production capping rule. Added `Milestone03QualityBandDefinitionAuditSmoke`, expanded Inventory and Production Forecast Infopedia wording, and wired the focused smoke into Gate 3.

Verification: `javac --release 17`, `Milestone03QualityBandDefinitionAuditSmoke`, `Milestone02InventoryReadabilitySmoke`, `Milestone03ProductionQualityTraceSmoke`, `Milestone02InfopediaMechanicsReadabilitySmoke`, and `Gate3PlayerFacingTextSmokeSuite` pass. Function and Mermaid maps refreshed. Rebuilt `PACKAGE_client` with `scripts\PACKAGE_CLIENT_WINDOWS.ps1 -CleanClasses -BuildJar` and passed Java 17 classfile scan across 3094 classfiles. Package jar SHA-256: `0EFC472C0841FF484B9CA0422ACE9DCB2AC1EF41E623F2E9C260F3056C916427`. The incremental manifest refresh wrote 162,622 indexed rows, reused 159,489 existing hashes, hashed 3,133 changed or new files, and reported 0 hash errors. Local GUI/package boot smoke was not run.

## Milestone 03 - Batch Issue Definition Audit Slice

Advanced Phase 18.1 by adding a batch issue definition audit to `ProductionBatchIssueAuthority`. The audit names the issue-tag owner, batch owner, and provenance owner; records the supported good, defective, contaminated, unstable, counterfeit, stolen-risk, restricted, and faction-certified batch tags; and reserves recall flags for a future owner.

The audit also records the effect boundary: issue tags preserve inspection and source-risk evidence, while only the existing defect appraisal changes ordinary resale value. Recall enforcement, seizure, law penalties, reputation effects, contamination damage, counterfeit penalties, item statistics, and ordinary use effects remain outside this owner. Added `Milestone03BatchIssueDefinitionAuditSmoke`, expanded the Production Forecast Infopedia wording, and wired the focused smoke into Gate 3.

Verification: `javac --release 17`, `Milestone03BatchIssueDefinitionAuditSmoke`, `Milestone03ProductionBatchIssueTagsSmoke`, `Milestone03ProductionQualityDefinitionAuditSmoke`, `Milestone02InfopediaMechanicsReadabilitySmoke`, and `Gate3PlayerFacingTextSmokeSuite` pass. Function and Mermaid maps refreshed. Rebuilt `PACKAGE_client` with `scripts\PACKAGE_CLIENT_WINDOWS.ps1 -CleanClasses -BuildJar` and passed Java 17 classfile scan across 3096 classfiles. Package jar SHA-256: `90727004D77EEE5FDDB42492BE193B61B7E20044C500A5534EBBE27E6AA36762`. The incremental manifest refresh wrote 164,181 indexed rows, reused 161,046 existing hashes, hashed 3,135 changed or new files, and reported 0 hash errors. Local GUI/package boot smoke was not run.

## Milestone 03 - Blueprint Parity Audit Slice

Advanced Phase 18.1 and Phase 18.2 by adding a construction blueprint parity audit to `BlueprintConstructionAuthority`. The audit names the BuildRecipe catalog owner, construction category owner, room-blueprint owner, and future acquisition owner; counts player-facing names, descriptions, category coverage, faction restrictions, knowledge gates, and workbench gates; and verifies the sample room blueprint mapping.

The audit deliberately exposes current gaps instead of inventing hidden systems: faction vendor stock categories, reputation gates, permits, acquisition paths, faction construction capability, heat, suspicion, non-acquirable rooms, player-only exceptions, and faction-only exceptions remain future data owners. Added `Milestone03BlueprintParityAuditSmoke`, expanded the Construction Blueprints Infopedia wording, and wired the focused smoke into Gate 3.

Verification: `javac --release 17`, `Milestone03BlueprintParityAuditSmoke`, `Milestone03BlueprintDefinitionAuditSmoke`, `Milestone02ConstructionCategorySmoke`, `Milestone02ConstructionReadabilitySmoke`, `Milestone02InfopediaMechanicsReadabilitySmoke`, and `Gate3PlayerFacingTextSmokeSuite` pass. Function and Mermaid maps refreshed. Rebuilt `PACKAGE_client` with `scripts\PACKAGE_CLIENT_WINDOWS.ps1 -CleanClasses -BuildJar` and passed Java 17 classfile scan across 3098 classfiles. Package jar SHA-256: `415AC40E7502D9F1B46687DB77E084E9399B76BAFD0CC275D9697DE2D685348F`. The incremental manifest refresh wrote 165,741 indexed rows, reused 162,604 existing hashes, hashed 3,137 changed or new files, and reported 0 hash errors. Local GUI/package boot smoke was not run.

## Milestone 03 - Blueprint Acquisition Path Audit Slice

Advanced Phase 18.2 by adding `BlueprintAcquisitionPathAuthority`, a data-owned acquisition audit for construction blueprints. The audit maps the existing BuildRecipe catalog to construction categories, representative/vendor archetypes, access labels, acquisition routes, legal labels, and player-facing explanations without adding live shop offers.

The acquisition audit distinguishes blueprint ownership from permission, reputation, license, permit, materials, workbench, knowledge, placement access, utilities, and construction labor. It also records future-owner boundaries for live vendor stock, reputation spending, permit purchases, theft resolution, heat and suspicion mutation, and faction construction execution. Added `Milestone03BlueprintAcquisitionPathAuditSmoke`, expanded the Construction Blueprints Infopedia wording, and wired the focused smoke into Gate 3.

Verification: `javac --release 17`, `Milestone03BlueprintAcquisitionPathAuditSmoke`, `Milestone03BlueprintParityAuditSmoke`, `Milestone03BlueprintDefinitionAuditSmoke`, `Milestone02ConstructionCategorySmoke`, `Milestone02ConstructionReadabilitySmoke`, `Milestone02InfopediaMechanicsReadabilitySmoke`, and `Gate3PlayerFacingTextSmokeSuite` pass. Function and Mermaid maps refreshed. Rebuilt `PACKAGE_client` with `scripts\PACKAGE_CLIENT_WINDOWS.ps1 -CleanClasses -BuildJar` and passed Java 17 classfile scan across 3104 classfiles. Package jar SHA-256: `11CE3C1DBF96F21BE7EBE43CF9FB9668A198AB4E9906B69B821D7F7BBE4E3F01`. The incremental manifest refresh wrote 167,307 indexed rows, reused 164,164 existing hashes, hashed 3,143 changed or new files, and reported 0 hash errors. Local GUI/package boot smoke was not run.

## Milestone 03 - Room Construction Parity Audit Slice

Advanced Phase 18.1 by adding `RoomConstructionParityAuthority`, an audit surface for room-stamp construction parity. The audit samples current zone room profiles, marks faction rooms with player acquisition status, marks player construction blueprints with faction-use status, maps common room functions to current BuildRecipe blueprints, and records documented exceptions for plazas, transitions, closets, and unsafe utility spaces.

The audit keeps consequences future-owned: it does not place rooms, unlock blueprints, mutate ownership, spend reputation, create faction construction jobs, or apply heat and suspicion. Added `Milestone03RoomConstructionParityAuditSmoke`, expanded the Construction Blueprints Infopedia wording, and wired the focused smoke into Gate 3.

Verification: `javac --release 17`, `Milestone03RoomConstructionParityAuditSmoke`, `Milestone03BlueprintAcquisitionPathAuditSmoke`, `Milestone03BlueprintParityAuditSmoke`, `Milestone03BlueprintDefinitionAuditSmoke`, `Milestone02ConstructionCategorySmoke`, `Milestone02ConstructionReadabilitySmoke`, `Milestone02InfopediaMechanicsReadabilitySmoke`, and `Gate3PlayerFacingTextSmokeSuite` pass. Function and Mermaid maps refreshed. Rebuilt `PACKAGE_client` with `scripts\PACKAGE_CLIENT_WINDOWS.ps1 -CleanClasses -BuildJar` and passed Java 17 classfile scan across 3110 classfiles. Package jar SHA-256: `75378ECA3181C3B23D78C4F207987926340BA6F8CD8080FDDCE8EB8CAC60E710`. The incremental manifest refresh wrote 168,876 indexed rows, reused 165,727 existing hashes, hashed 3,149 changed or new files, and reported 0 hash errors. Local GUI/package boot smoke was not run.

## Milestone 03 - Blueprint Expansion Heat Audit Slice

Advanced Phase 18.2 and Phase 19 release-audit readiness by adding `BlueprintExpansionHeatAuthority`, an audit-only heat and suspicion projection surface for construction blueprints. The audit scores current BuildRecipe blueprints through visible commerce, armed defenses, industrial footprint, laboratory or clinic footprint, access or legality risk, and faction-visible asset drivers while reusing the existing Expansion Heat readability bands.

The slice keeps gameplay mutation explicitly future-owned: projected heat and suspicion do not change `gangHeat`, suspicion, reputation, permits, law response, faction schemes, or construction completion. Added `Milestone03BlueprintExpansionHeatAuditSmoke`, expanded the Construction Blueprints and Expansion Heat Infopedia wording, and wired the focused smoke into Gate 3.

Verification: `javac --release 17`, `Milestone03BlueprintExpansionHeatAuditSmoke`, `Milestone02ExpansionHeatReadabilitySmoke`, `Milestone03RoomConstructionParityAuditSmoke`, `Milestone03BlueprintAcquisitionPathAuditSmoke`, `Milestone02InfopediaMechanicsReadabilitySmoke`, and `Gate3PlayerFacingTextSmokeSuite` pass. Function and Mermaid maps refreshed. Rebuilt `PACKAGE_client` with `scripts\PACKAGE_CLIENT_WINDOWS.ps1 -CleanClasses -BuildJar` and passed Java 17 classfile scan across 3,116 classfiles. Package jar SHA-256: `34F17B3C08DE436EE18B507AB7EDD1917C01AAFD50F98FB23E0B667456CE9274`. The repository manifest refresh wrote 170,448 indexed rows, reused 167,293 existing hashes, hashed 3,155 changed or new files, and reported 0 hash errors. Local GUI/package boot smoke was not run.

## Milestone 03 - Progressive Construction Definition Audit Slice

Advanced Phase 18.1, Phase 18.2, and the progressive construction standard by adding a definition audit to the existing `ProgressiveConstructionAuthority`. The audit names staged construction site ownership, saved BaseObject fields, required and inserted materials, labor progress, visual progress, final build symbol, quality, faction, held-tool timing, ghost-blue visual transition, and save/load restoration boundaries.

Live construction confirmation now creates a prepaid under-construction site instead of instantly completing the final base object. The existing placement checks and material consumption still run before placement, but labor completion now remains owned by staged construction; missing-material placement, worker dispatch, room ownership mutation, blueprint unlocks, and heat application remain future owners. Added `Milestone03ProgressiveConstructionDefinitionAuditSmoke`, expanded the Construction Blueprints Infopedia wording, and wired the focused smoke into Gate 3.

Verification: `javac --release 17`, `Milestone03ProgressiveConstructionDefinitionAuditSmoke`, `Milestone03BlueprintDefinitionAuditSmoke`, `Milestone03BlueprintExpansionHeatAuditSmoke`, `Milestone02ConstructionReadabilitySmoke`, `Milestone02InfopediaMechanicsReadabilitySmoke`, and `Gate3PlayerFacingTextSmokeSuite` pass. Function and Mermaid maps refreshed. Rebuilt `PACKAGE_client` with `scripts\PACKAGE_CLIENT_WINDOWS.ps1 -CleanClasses -BuildJar` and passed Java 17 classfile scan across 3,118 classfiles. Package jar SHA-256: `F041FA8D4DA5915B4E0019B672D81F395121BA9FE2C652D452F7AFBA4BFC0AFD`. The repository manifest refresh wrote 172,026 indexed rows, reused 168,868 existing hashes, hashed 3,158 changed or new files, and reported 0 hash errors. Local GUI/package boot smoke was not run.

## Milestone 03 - Progressive Construction Visibility Slice

Advanced the staged construction bridge by making under-construction sites visibly and textually distinct after placement. The world renderer now overlays staged base objects with the existing ghost-blue construction tint and a compact progress bar, while object interaction text reports staged-site status, material progress, labor progress, missing materials, and the final build target before offering only inspection/approach actions.

This keeps unfinished construction from being mistaken for a completed facility: machine operation, crafting, repair, business returns, room ownership mutation, worker dispatch, and heat application remain outside this visual/readability owner. Expanded `Milestone03ProgressiveConstructionDefinitionAuditSmoke`, updated the Construction Blueprints Infopedia wording, and kept the focused smoke wired into Gate 3.

Verification: `javac --release 17`, `Milestone03ProgressiveConstructionDefinitionAuditSmoke`, `Milestone03BlueprintDefinitionAuditSmoke`, `Milestone03BlueprintExpansionHeatAuditSmoke`, `Milestone02ConstructionReadabilitySmoke`, `Milestone02InfopediaMechanicsReadabilitySmoke`, and `Gate3PlayerFacingTextSmokeSuite` pass. Function and Mermaid maps refreshed. Rebuilt `PACKAGE_client` with `scripts\PACKAGE_CLIENT_WINDOWS.ps1 -CleanClasses -BuildJar` and passed Java 17 classfile scan across 3,118 classfiles. Package jar SHA-256: `DE7908CCEF8E2B003642000784566906E61C2986C948280C91CF9D2265192B4A`. The repository manifest refresh wrote 173,594 indexed rows, reused 170,437 existing hashes, hashed 3,157 changed or new files, and reported 0 hash errors. Local GUI/package boot smoke was not run.

## Milestone 03 - Progressive Construction Labor Action Slice

Advanced staged construction from visible state into a bounded player action. Under-construction sites now expose a Work button in the object interaction panel; each use contributes one turn of labor through `ProgressiveConstructionAuthority`, reports progress or completion, advances time, and lets the staged owner finalize the base object only when materials and labor are complete.

This still keeps scope narrow: the Work action does not dispatch workers, bypass missing materials, mutate room ownership, unlock blueprints, apply heat, or treat unfinished sites as machines, shops, repair targets, or craft stations. Expanded `Milestone03ProgressiveConstructionDefinitionAuditSmoke` and updated the Construction Blueprints Infopedia wording.

Verification: `javac --release 17`, `Milestone03ProgressiveConstructionDefinitionAuditSmoke`, `Milestone03BlueprintDefinitionAuditSmoke`, `Milestone03BlueprintExpansionHeatAuditSmoke`, `Milestone02ConstructionReadabilitySmoke`, `Milestone02InfopediaMechanicsReadabilitySmoke`, and `Gate3PlayerFacingTextSmokeSuite` pass. Function and Mermaid maps refreshed. Rebuilt `PACKAGE_client` with `scripts\PACKAGE_CLIENT_WINDOWS.ps1 -CleanClasses -BuildJar` and passed Java 17 classfile scan across 3,118 classfiles. Package jar SHA-256: `1D970F297A8CDB1980D9BDE5EB047FF7CF03BBB8B4E5ABE49727CDBCEC79AEFE`. The repository manifest refresh wrote 175,162 indexed rows, reused 172,005 existing hashes, hashed 3,157 changed or new files, and reported 0 hash errors. Local GUI/package boot smoke was not run.

## Milestone 03 - Progressive Construction Material Staging Slice

Extended the Work action so staged construction sites can pull available missing materials through the existing production-input consumption route before labor advances. A Work action can now stage construction supplies, machine parts, and named component units into the site's inserted-material ledger, then apply labor only once the staged materials satisfy the required-material ledger.

The slice still keeps first placement conservative: live blueprint placement continues to require available materials up front, while the reusable staged owner now supports partial-site material contribution for future missing-material placement and recovery flows. Worker dispatch, room ownership mutation, blueprint unlocks, and heat application remain outside this owner. Expanded `Milestone03ProgressiveConstructionDefinitionAuditSmoke` and updated the Construction Blueprints Infopedia wording.

Verification: `javac --release 17`, `Milestone03ProgressiveConstructionDefinitionAuditSmoke`, `Milestone03BlueprintDefinitionAuditSmoke`, `Milestone03BlueprintExpansionHeatAuditSmoke`, `Milestone02ConstructionReadabilitySmoke`, `Milestone02InfopediaMechanicsReadabilitySmoke`, and `Gate3PlayerFacingTextSmokeSuite` pass. Function and Mermaid maps refreshed. Rebuilt `PACKAGE_client` with `scripts\PACKAGE_CLIENT_WINDOWS.ps1 -CleanClasses -BuildJar` and passed Java 17 classfile scan across 3,118 classfiles. Package jar SHA-256: `58BDD0B9E7167C352A74972449573777CCCCF541FC5B2D022886A0FE92C3EB03`. The repository manifest refresh wrote 176,730 indexed rows, reused 173,573 existing hashes, hashed 3,157 changed or new files, and reported 0 hash errors. Local GUI/package boot smoke was not run.

## Milestone 03 - Progressive Construction Partial Placement Slice

Advanced live construction placement so material shortfalls no longer always block starting a staged site. If placement geometry, workbench, knowledge, and occupancy checks pass, and at least one required material unit is available, the build panel reports a STAGED START and confirmation creates a partial under-construction site with the available materials inserted. Additional materials and labor then continue through the existing Work action.

The slice keeps no-input and non-material failures honest: zero available construction inputs still block placement, while knowledge, workbench, occupied tile, map object, base object, and world-bound failures remain hard refusals. Worker dispatch, room ownership mutation, blueprint unlocks, and heat application remain outside this owner. Expanded construction readability and progressive construction smokes, and updated the Construction Blueprints Infopedia wording.

Verification: `javac --release 17`, `Milestone03ProgressiveConstructionDefinitionAuditSmoke`, `Milestone03BlueprintDefinitionAuditSmoke`, `Milestone03BlueprintExpansionHeatAuditSmoke`, `Milestone02ConstructionReadabilitySmoke`, `Milestone02InfopediaMechanicsReadabilitySmoke`, and `Gate3PlayerFacingTextSmokeSuite` pass. Function and Mermaid maps refreshed. Rebuilt `PACKAGE_client` with `scripts\PACKAGE_CLIENT_WINDOWS.ps1 -CleanClasses -BuildJar` and passed Java 17 classfile scan across 3,118 classfiles. Package jar SHA-256: `95CB5C094C144A8C049C0966523B25307503C40C76C63FC3DFE9A035720FCD32`. The repository manifest refresh wrote 178,298 indexed rows, reused 175,139 existing hashes, hashed 3,159 changed or new files, and reported 0 hash errors. Local GUI/package boot smoke was not run.

## Milestone 03 - Progressive Construction Persistence Guard Slice

Added a concrete save/load guard for staged construction sites. `Milestone03ProgressiveConstructionPersistenceSmoke` now writes a partial construction site through `Persistence.writeCore`, reloads it through `Persistence.readCore`, and verifies that under-construction status, placeholder symbol, final symbol, assigned recipe, required and inserted materials, labor progress, visual progress, quality, faction, inspection text, and later BaseObject fields survive the round trip.

The slice also checks that loaded staged sites are restored before completed-object configuration and keep the world tile on the construction placeholder rather than silently becoming finished furniture. Updated the progressive construction audit, Gate 3 suite, and Construction Blueprints Infopedia guard.

Verification: `javac --release 17`, `Milestone03ProgressiveConstructionPersistenceSmoke`, `Milestone03ProgressiveConstructionDefinitionAuditSmoke`, `Milestone02ConstructionReadabilitySmoke`, `Milestone02InfopediaMechanicsReadabilitySmoke`, and `Gate3PlayerFacingTextSmokeSuite` pass. Function and Mermaid maps refreshed. Rebuilt `PACKAGE_client` with `scripts\PACKAGE_CLIENT_WINDOWS.ps1 -CleanClasses -BuildJar` and passed Java 17 classfile scan across 3,120 classfiles. Package jar SHA-256: `5A39025C477A925396456E0575637451E64B8CF86A5179BADE11F522BD551E62`. The repository manifest refresh wrote 179,869 indexed rows, reused 176,709 existing hashes, hashed 3,160 changed or new files, and reported 0 hash errors. Local GUI/package boot smoke was not run.

## Milestone 03 - Progressive Construction Dismantle Slice

Added a bounded Dismantle action for unfinished staged construction sites. Staged sites now expose Dismantle beside Work in the object interaction panel; the action removes the unfinished placeholder, recovers inserted construction supplies and machine parts to pooled resources, returns named components to base storage, clears the construction placeholder tile when it owns that tile, and advances one turn.

The slice keeps completion authority honest: dismantling does not configure the final facility, recover labor progress, mutate room ownership, dispatch workers, unlock blueprints, apply heat, or target already-completed base objects. Added `Milestone03ProgressiveConstructionDismantleSmoke`, expanded the progressive construction audit, and updated the Construction Blueprints Infopedia guard.

Verification: `javac --release 17`, `Milestone03ProgressiveConstructionDismantleSmoke`, `Milestone03ProgressiveConstructionDefinitionAuditSmoke`, `Milestone03ProgressiveConstructionPersistenceSmoke`, `Milestone02ConstructionReadabilitySmoke`, `Milestone02InfopediaMechanicsReadabilitySmoke`, and `Gate3PlayerFacingTextSmokeSuite` pass. Function and Mermaid maps refreshed. Rebuilt `PACKAGE_client` with `scripts\PACKAGE_CLIENT_WINDOWS.ps1 -CleanClasses -BuildJar` and passed Java 17 classfile scan across 3,124 classfiles. Package jar SHA-256: `CF6A18B1EA8EB7DB2E4407E441538288A7230B4229CE5F2AAD89A4289B1B9BB3`. The repository manifest refresh wrote 181,443 indexed rows, reused 178,278 existing hashes, hashed 3,165 changed or new files, and reported 0 hash errors. Local GUI/package boot smoke was not run.

## Milestone 03 - Progressive Construction Tile Sync Slice

Aligned live staged-construction placement with the save/load behavior by synchronizing staged sites back to the world tile grid. Live placement now reserves the target tile with the construction placeholder immediately after the BaseObject is added, and staged completion restores the final built symbol before normal built-object configuration runs.

This keeps map rendering, same-tile placement denial, save/load restoration, dismantle cleanup, and completed-facility behavior consistent without adding room ownership, worker dispatch, blueprint unlocks, heat, or faction construction side effects. Added `Milestone03ProgressiveConstructionTileSyncSmoke`, expanded the progressive construction audit, and updated the Construction Blueprints Infopedia guard.

Verification: `javac --release 17`, `Milestone03ProgressiveConstructionTileSyncSmoke`, `Milestone03ProgressiveConstructionDismantleSmoke`, `Milestone03ProgressiveConstructionDefinitionAuditSmoke`, `Milestone03ProgressiveConstructionPersistenceSmoke`, `Milestone02ConstructionReadabilitySmoke`, `Milestone02InfopediaMechanicsReadabilitySmoke`, and `Gate3PlayerFacingTextSmokeSuite` pass. Function and Mermaid maps refreshed. Rebuilt `PACKAGE_client` with `scripts\PACKAGE_CLIENT_WINDOWS.ps1 -CleanClasses -BuildJar` and passed Java 17 classfile scan across 3,126 classfiles. Package jar SHA-256: `02902AD6BDC911F2BD5BC83D02F48A616F5A2DDF2EB35AFC1140BB434B668D5D`. The repository manifest refresh wrote 183,017 indexed rows, reused 179,850 existing hashes, hashed 3,167 changed or new files, and reported 0 hash errors. Local GUI/package boot smoke was not run.

## Milestone 03 - Progressive Construction Original Tile Restoration Slice

Preserved the walkable tile underneath unfinished staged construction. Live placement now records the original tile before writing the construction placeholder, staged-site save lines persist that original tile in an append-only BaseObject field, and dismantle restores the recorded tile instead of flattening every cancelled site to plain floor.

This keeps cancellation honest for roads, doors, floors, and other walkable construction surfaces while preserving the existing boundaries: completion still owns the final built symbol, dismantle still only targets unfinished staged sites, and no room ownership, worker dispatch, blueprint unlock, heat, or faction construction side effects are introduced. Added `Milestone03ProgressiveConstructionOriginalTileSmoke`, expanded persistence/tile-sync guards, and updated the Construction Blueprints Infopedia wording.

Verification: `javac --release 17`, `Milestone03ProgressiveConstructionOriginalTileSmoke`, `Milestone03ProgressiveConstructionTileSyncSmoke`, `Milestone03ProgressiveConstructionDismantleSmoke`, `Milestone03ProgressiveConstructionDefinitionAuditSmoke`, `Milestone03ProgressiveConstructionPersistenceSmoke`, `Milestone02ConstructionReadabilitySmoke`, `Milestone02InfopediaMechanicsReadabilitySmoke`, and `Gate3PlayerFacingTextSmokeSuite` pass. Function and Mermaid maps refreshed; Mermaid found 618 Java modules. Rebuilt `PACKAGE_client` with `scripts\PACKAGE_CLIENT_WINDOWS.ps1 -CleanClasses -BuildJar` and passed Java 17 classfile scan across 3,128 classfiles. Package jar SHA-256: `CCF8F81CCE960741BC9F7AE4D5E10B2E7C025E64483385A25699F33E868B8C92`. The repository manifest refresh wrote 184,592 indexed rows, reused 181,420 existing hashes, hashed 3,172 changed or new files, and reported 0 hash errors. Local GUI/package boot smoke was not run.

## Milestone 03 - Blueprint Ownership Permission Readability Slice

Advanced the construction release-audit wording by making live blueprint previews distinguish blueprint ownership from actual build permission and readiness. Construction details now state that owning a blueprint is separate from permission, reputation, license, permit, materials, workbench, knowledge, placement access, utilities, and labor, so the player can understand why a known plan may still be blocked or only partially startable.

This keeps the slice strictly readable and audit-backed: it does not add vendors, reputation spending, permit purchase, theft resolution, utility simulation, heat mutation, suspicion mutation, or faction construction execution. Added `Milestone03BlueprintOwnershipPermissionReadabilitySmoke`, expanded the Construction Blueprints Infopedia wording, and wired the guard into Gate 3.

Verification: `javac --release 17`, `Milestone03BlueprintOwnershipPermissionReadabilitySmoke`, `Milestone02ConstructionReadabilitySmoke`, `Milestone03BlueprintAcquisitionPathAuditSmoke`, `Milestone02InfopediaMechanicsReadabilitySmoke`, and `Gate3PlayerFacingTextSmokeSuite` pass. Function and Mermaid maps refreshed; Mermaid found 619 Java modules. Rebuilt `PACKAGE_client` with `scripts\PACKAGE_CLIENT_WINDOWS.ps1 -CleanClasses -BuildJar` and passed Java 17 classfile scan across 3,130 classfiles. Package jar SHA-256: `C4CB6BB8B32ACED3B1FD689A9878368474FB62FD33E5224465283F236FED1913`. The repository manifest refresh wrote 186,168 indexed rows, reused 182,998 existing hashes, hashed 3,170 changed or new files, and reported 0 hash errors. Local GUI/package boot smoke was not run.

## Milestone 03 - Blueprint Entombment Safety Guard Slice

Hardened construction placement against self-entombment. Room-blueprint preflight now warns when a stamp has no connection anchor or doorway, live placement now invokes the claimed-room exit-path guard before allowing OK or STAGED START placement, and the runtime guard rejects the player's current tile, NPC-occupied tiles, and claimed-room blocker placements that would leave no valid access path to a door or exit.

This keeps the slice safety-focused and bounded: it does not add vendor stock, permit purchase, utility simulation, worker dispatch, room ownership mutation, heat mutation, suspicion mutation, faction construction execution, or background path scans. Added `Milestone03BlueprintNoSelfEntombmentAuditSmoke` and `Milestone03SelfEntombmentRuntimeGuardSmoke`, expanded the Construction Blueprints Infopedia wording, and wired both guards into Gate 3.

Verification: `javac --release 17`, `Milestone03BlueprintNoSelfEntombmentAuditSmoke`, `Milestone03SelfEntombmentRuntimeGuardSmoke`, `Milestone03BlueprintDefinitionAuditSmoke`, `Milestone02ConstructionReadabilitySmoke`, `Milestone02InfopediaMechanicsReadabilitySmoke`, and `Gate3PlayerFacingTextSmokeSuite` pass. Function and Mermaid maps refreshed; Mermaid found 621 Java modules. Rebuilt `PACKAGE_client` with `scripts\PACKAGE_CLIENT_WINDOWS.ps1 -CleanClasses -BuildJar` and passed Java 17 classfile scan across 3,134 classfiles. Package jar SHA-256: `AA1E9C0C21D0FD87F0D076D64C2B1A4E557F2E918E4508F0F2243DBAC1A1D906`. The final repository manifest refresh wrote 189,323 indexed rows, reused 189,322 existing hashes, hashed 1 changed or new file, and reported 0 hash errors. Local GUI/package boot smoke was not run.

## Milestone 03 - Blueprint Utility Readiness Audit Slice

Advanced the construction governance lane by adding a metadata-only utility readiness audit. `ConstructionGovernanceAuthority` now reports utility-bearing room and blueprint coverage, tracked hook families, fail-closed missing utility validation, ready metadata validation, and passability interaction without claiming that a live utility network exists.

This keeps the slice explicitly bounded: it does not create utility grids, consume fuel or water, schedule workers, mutate room ownership, apply heat or suspicion, run background scans, or complete construction. Added `Milestone03BlueprintUtilityReadinessAuditSmoke`, expanded the Construction Blueprints Infopedia wording, and wired the guard into Gate 3.

Verification: `javac --release 17`, `Milestone03BlueprintUtilityReadinessAuditSmoke`, `Milestone03BlueprintDefinitionAuditSmoke`, `Milestone02ConstructionReadabilitySmoke`, `Milestone02InfopediaMechanicsReadabilitySmoke`, and `Gate3PlayerFacingTextSmokeSuite` pass. Function and Mermaid maps refreshed; Mermaid found 622 Java modules. Rebuilt `PACKAGE_client` with `scripts\PACKAGE_CLIENT_WINDOWS.ps1 -CleanClasses -BuildJar` and passed Java 17 classfile scan across 3,136 classfiles. Package jar SHA-256: `B10DFEB07313A84C68DA8FB2A5AAF0F3047006AE1C8A8E0D1E348775CD978C99`. The final repository manifest refresh wrote 190,902 indexed rows, reused 190,901 existing hashes, hashed 1 changed or new file, and reported 0 hash errors. Local GUI/package boot smoke was not run.

## Milestone 03 - Blueprint Permission Readiness Audit Slice

Advanced the construction acquisition lane by adding `BlueprintPermissionReadinessAuthority`, a forecast-only permission readiness layer over existing blueprint acquisition metadata. The audit classifies blueprints into public-ready, permit-or-license, faction-standing, restricted legal-access, and illicit or stolen-risk gates, then reports concrete blockers such as unowned blueprint, missing permit, missing license, missing faction standing, or missing legal access.

This keeps blueprint ownership honest without inventing live commerce: the slice does not add vendor offers, spend reputation, buy permits, grant licenses, resolve theft, mutate heat or suspicion, bypass placement validation, or execute faction construction. Added `Milestone03BlueprintPermissionReadinessAuditSmoke`, expanded the Construction Blueprints Infopedia wording, and wired the guard into Gate 3.

Verification: `javac --release 17`, `Milestone03BlueprintPermissionReadinessAuditSmoke`, `Milestone03BlueprintAcquisitionPathAuditSmoke`, `Milestone03BlueprintOwnershipPermissionReadabilitySmoke`, `Milestone02ConstructionReadabilitySmoke`, `Milestone02InfopediaMechanicsReadabilitySmoke`, and `Gate3PlayerFacingTextSmokeSuite` pass. Function and Mermaid maps refreshed; Mermaid found 624 Java modules. Rebuilt `PACKAGE_client` with `scripts\PACKAGE_CLIENT_WINDOWS.ps1 -CleanClasses -BuildJar` and passed Java 17 classfile scan across 3,142 classfiles. Package jar SHA-256: `84FA0AF1F5BB54E89F06E9F4834FA0DD9B8236ABDEBCCF8578B310D0DF34E723`. The final repository manifest refresh wrote 192,487 indexed rows, reused 192,486 existing hashes, hashed 1 changed or new file, and reported 0 hash errors. Local GUI/package boot smoke was not run.

## Milestone 03 - Blueprint Faction Construction Capability Audit Slice

Advanced the construction parity lane by adding `BlueprintFactionConstructionCapabilityAuthority`, an audit-only faction construction capability layer over existing blueprint, parity, and permission metadata. The audit marks plausible faction-construction candidates and reports planning blockers such as permission readiness, faction budget, construction crew, room claim, and construction materials.

This keeps faction construction strictly as planning metadata: the slice does not spawn faction construction jobs, mutate room ownership, reserve or consume materials, spend faction budget, grant permits, apply heat or suspicion, bypass placement validation, or complete construction. Added `Milestone03BlueprintFactionConstructionCapabilityAuditSmoke`, expanded the Construction Blueprints Infopedia wording, and wired the guard into Gate 3.

Verification: `javac --release 17`, `Milestone03BlueprintFactionConstructionCapabilityAuditSmoke`, `Milestone03BlueprintPermissionReadinessAuditSmoke`, `Milestone03RoomConstructionParityAuditSmoke`, `Milestone02ConstructionReadabilitySmoke`, `Milestone02InfopediaMechanicsReadabilitySmoke`, and `Gate3PlayerFacingTextSmokeSuite` pass. Function and Mermaid maps refreshed; Mermaid found 626 Java modules. Rebuilt `PACKAGE_client` with `scripts\PACKAGE_CLIENT_WINDOWS.ps1 -CleanClasses -BuildJar` and passed Java 17 classfile scan across 3,148 classfiles. Package jar SHA-256: `38F7AA4BB3DA257D653F214BE33CDEB60779D420C55A16CA1F7474A8D2E69B1D`. The final repository manifest refresh wrote 194,075 indexed rows, reused 194,074 existing hashes, hashed 1 changed or new file, and reported 0 hash errors. Local GUI/package boot smoke was not run.

## Milestone 03 - Blueprint Faction Construction Job Definition Audit Slice

Advanced the construction parity lane by adding `BlueprintFactionConstructionJobDefinitionAuthority`, a definition-only contract for future faction construction jobs. The audit names lifecycle states, required fields, sample definitions, capability and permission readiness sources, and the handoff to staged construction before any execution owner exists.

This keeps faction construction non-mutating: the slice does not create a live job queue, reserve or consume materials, assign workers, mutate room ownership, apply heat or suspicion, place objects, advance labor, or complete construction. Added `Milestone03BlueprintFactionConstructionJobDefinitionAuditSmoke`, expanded the Construction Blueprints Infopedia wording, and wired the guard into Gate 3.

Verification: `javac --release 17`, `Milestone03BlueprintFactionConstructionJobDefinitionAuditSmoke`, `Milestone03BlueprintFactionConstructionCapabilityAuditSmoke`, `Milestone03BlueprintPermissionReadinessAuditSmoke`, `Milestone02ConstructionReadabilitySmoke`, `Milestone02InfopediaMechanicsReadabilitySmoke`, and `Gate3PlayerFacingTextSmokeSuite` pass. Function and Mermaid maps refreshed; Mermaid found 628 Java modules. Rebuilt `PACKAGE_client` with `scripts\PACKAGE_CLIENT_WINDOWS.ps1 -CleanClasses -BuildJar` and passed Java 17 classfile scan across 3,154 classfiles. Package jar SHA-256: `44BD9BBD89CB117FD2AA01DDA6E70DA0CFB7A36A28CC54DC7B92A2176E72E94D`. The final repository manifest refresh wrote 195,666 indexed rows, reused 195,665 existing hashes, hashed 1 changed or new file, and reported 0 hash errors. Local GUI/package boot smoke was not run.

## Milestone 03 - Blueprint Faction Construction Material Reservation Audit Slice

Advanced the construction parity lane by adding `BlueprintFactionConstructionMaterialReservationAuthority`, an audit-only material reservation contract for future faction construction jobs. The audit derives required Construction supplies, Machine parts, and named component costs from `BuildRecipe`, then reports reserved-preview and missing-material ledgers without touching inventory.

This keeps faction construction non-mutating: the slice does not remove supplies, remove machine parts, remove named components, write reservation rows, stage materials into a site, assign crew, mutate room ownership, place objects, advance labor, or complete construction. Added `Milestone03BlueprintFactionConstructionMaterialReservationAuditSmoke`, expanded the Construction Blueprints Infopedia wording, and wired the guard into Gate 3.

Verification: `javac --release 17`, `Milestone03BlueprintFactionConstructionMaterialReservationAuditSmoke`, `Milestone03BlueprintFactionConstructionJobDefinitionAuditSmoke`, `Milestone03BlueprintFactionConstructionCapabilityAuditSmoke`, `Milestone02ConstructionReadabilitySmoke`, `Milestone02InfopediaMechanicsReadabilitySmoke`, and `Gate3PlayerFacingTextSmokeSuite` pass. Function and Mermaid maps refreshed; Mermaid found 630 Java modules. Rebuilt `PACKAGE_client` with `scripts\PACKAGE_CLIENT_WINDOWS.ps1 -CleanClasses -BuildJar` and passed Java 17 classfile scan across 3,160 classfiles. Package jar SHA-256: `EA46B26BAC290E703080127014FEF4FF93AD99B40A63BA1084373E17B5A64EF3`. The final repository manifest refresh wrote 198,838 indexed rows, reused 198,837 existing hashes, hashed 1 changed or new file, and reported 0 hash errors. Local GUI/package boot smoke was not run.

## Milestone 03 - Blueprint Faction Construction Crew Assignment Audit Slice

Advanced the construction parity lane by adding `BlueprintFactionConstructionCrewAssignmentAuthority`, an audit-only crew assignment contract for future faction construction jobs. The audit derives required crew profiles and labor turns from `BuildRecipe` workbench, faction restriction, attention, construction category, and base labor metadata before any execution owner can bind workers.

This keeps faction construction non-mutating: the slice does not assign recruits, move NPCs, reserve workers, create schedules, mutate room ownership, remove materials, place objects, advance labor, or complete construction. Added `Milestone03BlueprintFactionConstructionCrewAssignmentAuditSmoke`, expanded the Construction Blueprints Infopedia wording, and wired the guard into Gate 3.

Verification: `javac --release 17`, `Milestone03BlueprintFactionConstructionCrewAssignmentAuditSmoke`, `Milestone03BlueprintFactionConstructionMaterialReservationAuditSmoke`, `Milestone03BlueprintFactionConstructionJobDefinitionAuditSmoke`, `Milestone03BlueprintFactionConstructionCapabilityAuditSmoke`, `Milestone02ConstructionReadabilitySmoke`, `Milestone02InfopediaMechanicsReadabilitySmoke`, and `Gate3PlayerFacingTextSmokeSuite` pass. Function and Mermaid maps refreshed; Mermaid found 632 Java modules. Rebuilt `PACKAGE_client` with `scripts\PACKAGE_CLIENT_WINDOWS.ps1 -CleanClasses -BuildJar` and passed Java 17 classfile scan across 3,166 classfiles. Package jar SHA-256: `C401B0573D79A093B0AD6ADA6DA233D46BF0C6444F53704F65FCB637F9F7ADF3`. The final repository manifest refresh wrote 200,435 indexed rows, reused 200,434 existing hashes, hashed 1 changed or new file, and reported 0 hash errors. Local GUI/package boot smoke was not run.

## Milestone 03 - Blueprint Faction Construction Site Readiness Audit Slice

Advanced the construction parity lane by adding `BlueprintFactionConstructionSiteReadinessAuthority`, an audit-only room claim and placement-readiness contract for future faction construction jobs. The audit names room-claim, placement, access-route, no-self-entombment, utility, heat-preview, and staged-construction handoff checks before any execution owner can reserve a target site.

This keeps faction construction non-mutating: the slice does not claim rooms, reserve tiles, write ownership, bypass placement validation, bypass no-self-entombment checks, create staged sites, apply utilities, assign crew, remove materials, or complete construction. Added `Milestone03BlueprintFactionConstructionSiteReadinessAuditSmoke`, expanded the Construction Blueprints Infopedia wording, and wired the guard into Gate 3.

Verification: `javac --release 17`, `Milestone03BlueprintFactionConstructionSiteReadinessAuditSmoke`, `Milestone03BlueprintFactionConstructionCrewAssignmentAuditSmoke`, `Milestone03BlueprintFactionConstructionMaterialReservationAuditSmoke`, `Milestone03BlueprintFactionConstructionJobDefinitionAuditSmoke`, `Milestone03BlueprintFactionConstructionCapabilityAuditSmoke`, `Milestone02ConstructionReadabilitySmoke`, `Milestone02InfopediaMechanicsReadabilitySmoke`, and `Gate3PlayerFacingTextSmokeSuite` pass. Function and Mermaid maps refreshed; Mermaid found 634 Java modules. Rebuilt `PACKAGE_client` with `scripts\PACKAGE_CLIENT_WINDOWS.ps1 -CleanClasses -BuildJar` and passed Java 17 classfile scan across 3,172 classfiles. Package jar SHA-256: `08C8E613F1A13400C1B1FDC4D60D5CD59ED33613E3C2522E0FAA3E7C03CC32AB`. The final repository manifest refresh wrote 202,035 indexed rows, reused 202,034 existing hashes, hashed 1 changed or new file, and reported 0 hash errors. Local GUI/package boot smoke was not run.

## Milestone 03 - Blueprint Faction Construction Budget and Heat Authorization Audit Slice

Advanced the construction parity lane by adding `BlueprintFactionConstructionBudgetHeatAuthorizationAuthority`, an audit-only budget and heat authorization contract for future faction construction jobs. The audit estimates faction budget from `BuildRecipe` supplies, parts, named components, workbench need, faction restriction, and base labor turns, then reuses `BlueprintExpansionHeatAuthority` projections for heat and suspicion.

This keeps faction construction non-mutating: the slice does not spend faction budget, mutate heat, mutate suspicion, trigger law response, schedule faction schemes, reserve sites, assign crew, remove materials, place objects, advance labor, or complete construction. Added `Milestone03BlueprintFactionConstructionBudgetHeatAuthorizationAuditSmoke`, expanded the Construction Blueprints Infopedia wording, and wired the guard into Gate 3.

Verification: `javac --release 17`, `Milestone03BlueprintFactionConstructionBudgetHeatAuthorizationAuditSmoke`, `Milestone03BlueprintFactionConstructionSiteReadinessAuditSmoke`, `Milestone03BlueprintFactionConstructionCrewAssignmentAuditSmoke`, `Milestone03BlueprintFactionConstructionMaterialReservationAuditSmoke`, `Milestone03BlueprintFactionConstructionJobDefinitionAuditSmoke`, `Milestone03BlueprintFactionConstructionCapabilityAuditSmoke`, `Milestone03BlueprintExpansionHeatAuditSmoke`, `Milestone02ConstructionReadabilitySmoke`, `Milestone02InfopediaMechanicsReadabilitySmoke`, and `Gate3PlayerFacingTextSmokeSuite` pass. Function and Mermaid maps refreshed; Mermaid found 636 Java modules. Rebuilt `PACKAGE_client` with `scripts\PACKAGE_CLIENT_WINDOWS.ps1 -CleanClasses -BuildJar` and passed Java 17 classfile scan across 3,178 classfiles. Package jar SHA-256: `D6AE6B509C118A90DBA45D098A66704727BE29D4FA07B1A1F05722E22DBAE538`. The final repository manifest refresh wrote 203,638 indexed rows, reused 203,637 existing hashes, hashed 1 changed or new file, and reported 0 hash errors. Local GUI/package boot smoke was not run.

## Milestone 03 - Blueprint Faction Construction Cancellation Release Audit Slice

Advanced the construction parity lane by adding `BlueprintFactionConstructionCancellationReleaseAuthority`, an audit-only cancellation and release contract for future faction construction jobs. The audit requires cancelled or failed jobs to record a reason and declare release of site, crew, materials, budget hold, and attention preview before any future execution owner can retire a job.

This keeps faction construction non-mutating: the slice does not cancel live jobs, release live reservations, refund budget, mutate heat, mutate suspicion, move crew, return materials, remove staged sites, place objects, advance labor, or complete construction. Added `Milestone03BlueprintFactionConstructionCancellationReleaseAuditSmoke`, expanded the Construction Blueprints Infopedia wording, and wired the guard into Gate 3.

Verification: `javac --release 17`, `Milestone03BlueprintFactionConstructionCancellationReleaseAuditSmoke`, `Milestone03BlueprintFactionConstructionBudgetHeatAuthorizationAuditSmoke`, `Milestone03BlueprintFactionConstructionSiteReadinessAuditSmoke`, `Milestone03BlueprintFactionConstructionCrewAssignmentAuditSmoke`, `Milestone03BlueprintFactionConstructionMaterialReservationAuditSmoke`, `Milestone03BlueprintFactionConstructionJobDefinitionAuditSmoke`, `Milestone03BlueprintFactionConstructionCapabilityAuditSmoke`, `Milestone02ConstructionReadabilitySmoke`, `Milestone02InfopediaMechanicsReadabilitySmoke`, and `Gate3PlayerFacingTextSmokeSuite` pass. Function and Mermaid maps refreshed; Mermaid found 638 Java modules. Rebuilt `PACKAGE_client` with `scripts\PACKAGE_CLIENT_WINDOWS.ps1 -CleanClasses -BuildJar` and passed Java 17 classfile scan across 3,184 classfiles. Package jar SHA-256: `0C3A714FFE5AA3DCC8BFB64847183525D66D60FBBCF8F250F05474D8C0D78867`. The final repository manifest refresh wrote 205,244 indexed rows, reused 205,243 existing hashes, hashed 1 changed or new file, and reported 0 hash errors. Local GUI/package boot smoke was not run.

## Milestone 03 - Blueprint Faction Construction Execution Handoff Audit Slice

Advanced the construction parity lane by adding `BlueprintFactionConstructionExecutionHandoffAuthority`, an audit-only go/no-go handoff contract before any future faction construction execution owner can reserve a live job. The handoff aggregates job authorization, material reservation readiness, crew assignment readiness, site readiness, budget and heat authorization, and rollback release readiness into one explicit ready or blocked packet.

This keeps faction construction non-mutating: the slice does not create live job queues, reserve sites or workers, remove materials, spend budget, mutate heat or suspicion, place staged construction, advance labor, cancel jobs, release reservations, or complete construction. Added `Milestone03BlueprintFactionConstructionExecutionHandoffAuditSmoke` and wired the guard into Gate 3.

Verification: `javac --release 17`, `Milestone03BlueprintFactionConstructionExecutionHandoffAuditSmoke`, `Milestone02InfopediaMechanicsReadabilitySmoke`, and `Gate3PlayerFacingTextSmokeSuite` pass. Function and Mermaid maps refreshed; Mermaid found 640 Java modules. Rebuilt `PACKAGE_client` with `scripts\PACKAGE_CLIENT_WINDOWS.ps1 -CleanClasses -BuildJar` through process execution-policy bypass and passed Java 17 classfile scan across 3,190 classfiles. The package rebuild reported 1,595 class files and 6,096 package asset files. The final repository manifest refresh wrote 205,318 indexed rows, reused 202,088 existing hashes, hashed 3,230 changed or new files, and reported 0 hash errors. Package boot smoke passed by keeping the client alive for the 8-second smoke window before cleanup.

## Milestone 03 - Blueprint Faction Construction Queue Admission Audit Slice

Advanced the construction parity lane by adding `BlueprintFactionConstructionQueueAdmissionAuthority`, an audit-only queue admission contract for future faction construction jobs. The audit projects whether a ready execution handoff can enter a reserved job slot or must remain blocked because the handoff is not ready or no queue slot is available.

This keeps faction construction non-mutating: the slice does not create a live job queue, reserve live slots, reserve workers or sites, remove materials, spend budget, mutate heat or suspicion, place staged construction, advance labor, cancel jobs, release reservations, or complete construction. Added `Milestone03BlueprintFactionConstructionQueueAdmissionAuditSmoke`, expanded the Construction Blueprints Infopedia wording, and wired the guard into Gate 3.

Verification: `javac --release 17`, `Milestone03BlueprintFactionConstructionQueueAdmissionAuditSmoke`, `Milestone02InfopediaMechanicsReadabilitySmoke`, and `Gate3PlayerFacingTextSmokeSuite` pass. Function and Mermaid maps refreshed; Mermaid found 642 Java modules. Rebuilt `PACKAGE_client` with `scripts\PACKAGE_CLIENT_WINDOWS.ps1 -CleanClasses -BuildJar` through process execution-policy bypass and passed Java 17 classfile scan across 3,196 classfiles. The package rebuild reported 1,598 class files and 6,096 package asset files. Package boot smoke passed by keeping the client alive for the 8-second smoke window before cleanup. The final repository manifest refresh wrote 205,342 indexed rows, reused 202,096 existing hashes, hashed 3,246 changed or new files, and reported 0 hash errors.

## Milestone 03 - Blueprint Faction Construction Reservation Ledger Audit Slice

Advanced the construction parity lane by adding `BlueprintFactionConstructionReservationLedgerAuthority`, an audit-only reservation ledger contract for future faction construction jobs. The audit requires queue admission plus material, crew, site, budget, and attention holds to be declared together before a future job can be treated as reserved.

This keeps faction construction non-mutating: the slice does not write reservation rows, create a live job queue, reserve workers or sites, remove materials, spend budget, mutate heat or suspicion, place staged construction, advance labor, cancel jobs, release reservations, or complete construction. Added `Milestone03BlueprintFactionConstructionReservationLedgerAuditSmoke`, expanded the Construction Blueprints Infopedia wording, and wired the guard into Gate 3.

Verification: `javac --release 17`, `Milestone03BlueprintFactionConstructionReservationLedgerAuditSmoke`, `Milestone02InfopediaMechanicsReadabilitySmoke`, and `Gate3PlayerFacingTextSmokeSuite` pass. Function and Mermaid maps refreshed; Mermaid found 644 Java modules. Rebuilt `PACKAGE_client` with `scripts\PACKAGE_CLIENT_WINDOWS.ps1 -CleanClasses -BuildJar` through process execution-policy bypass and passed Java 17 classfile scan across 3,202 classfiles. The package rebuild reported 1,601 class files and 6,096 package asset files. Package boot smoke passed by keeping the client alive for the 8-second smoke window before cleanup. The final repository manifest refresh wrote 205,362 indexed rows, reused 202,110 existing hashes, hashed 3,252 changed or new files, and reported 0 hash errors.

## Milestone 03 - Blueprint Faction Construction Staged Handoff Audit Slice

Advanced the construction parity lane by adding `BlueprintFactionConstructionStagedHandoffAuthority`, an audit-only handoff contract between reserved faction construction jobs and future staged-construction placement. The audit requires reservation readiness, immediate placement recheck, original tile capture, material transfer planning, and rollback planning before any future execution owner can hand the job to `ProgressiveConstructionAuthority`.

This keeps faction construction non-mutating: the slice does not create staged sites, reserve tiles, mutate original tiles, transfer materials, assign workers, spend budget, mutate heat or suspicion, advance labor, cancel jobs, release reservations, or complete construction. Added `Milestone03BlueprintFactionConstructionStagedHandoffAuditSmoke`, expanded the Construction Blueprints Infopedia wording, and wired the guard into Gate 3.

Verification: `javac --release 17`, `Milestone03BlueprintFactionConstructionStagedHandoffAuditSmoke`, `Milestone02InfopediaMechanicsReadabilitySmoke`, and `Gate3PlayerFacingTextSmokeSuite` pass. Function and Mermaid maps refreshed; Mermaid found 646 Java modules. Rebuilt `PACKAGE_client` with `scripts\PACKAGE_CLIENT_WINDOWS.ps1 -CleanClasses -BuildJar` through process execution-policy bypass and passed Java 17 classfile scan across 3,208 classfiles. The package rebuild reported 1,604 class files and 6,096 package asset files. Package boot smoke passed by keeping the client alive for the 8-second smoke window before cleanup. The final repository manifest refresh wrote 205,382 indexed rows, reused 202,124 existing hashes, hashed 3,258 changed or new files, and reported 0 hash errors.

## Milestone 03 - Blueprint Faction Construction Placement Outcome Audit Slice

Advanced the construction parity lane by adding `BlueprintFactionConstructionPlacementOutcomeAuthority`, an audit-only placement outcome contract for future faction staged-construction jobs. The audit requires staged handoff readiness, construction placeholder reservation, final symbol recording, staged inspection text, and visible rollback outcome before any future execution owner can report a staged site as placed.

This keeps faction construction non-mutating: the slice does not write tiles, create staged sites, mutate original tiles, transfer materials, assign workers, spend budget, mutate heat or suspicion, advance labor, cancel jobs, release reservations, or complete construction. Added `Milestone03BlueprintFactionConstructionPlacementOutcomeAuditSmoke`, expanded the Construction Blueprints Infopedia wording, and wired the guard into Gate 3.

Verification: local Java 17 compile passed; `Milestone03BlueprintFactionConstructionPlacementOutcomeAuditSmoke`, `Milestone02InfopediaMechanicsReadabilitySmoke`, and `Gate3PlayerFacingTextSmokeSuite` passed; function and Mermaid maps refreshed with 648 Java modules and 0 unpositioned modules; package-client rebuild passed with 1607 class files and 6096 package asset files; Java 17 classfile scan passed across 3214 classfiles with highest major version 61; package boot smoke stayed alive for the 8-second window; repository manifest refresh wrote 205403 rows with reused=202138, hashed=3265, errors=0.

## Milestone 03 - Blueprint Faction Construction Progress Tick Audit Slice

Advanced the construction parity lane by adding `BlueprintFactionConstructionProgressTickAuthority`, an audit-only labor progress tick contract for future faction staged-construction jobs. The audit requires placement outcome readiness, crew presence, staged materials, an open work window, progress record readiness, and visible rollback outcome before any future execution owner can record labor progress.

This keeps faction construction non-mutating and completion-separated: the slice does not advance labor, consume staged materials, move crew, mutate room ownership, configure facilities, spend budget, mutate heat or suspicion, cancel jobs, release reservations, or complete construction. Added `Milestone03BlueprintFactionConstructionProgressTickAuditSmoke`, expanded the Construction Blueprints Infopedia wording, and wired the guard into Gate 3.

Verification: local Java 17 compile passed; `Milestone03BlueprintFactionConstructionProgressTickAuditSmoke`, `Milestone02InfopediaMechanicsReadabilitySmoke`, and `Gate3PlayerFacingTextSmokeSuite` passed; function and Mermaid maps refreshed with 650 Java modules and 0 unpositioned modules; package-client rebuild passed with 1610 class files and 6096 package asset files; Java 17 classfile scan passed across 3220 classfiles with highest major version 61; package boot smoke stayed alive for the 8-second window; repository manifest refresh wrote 205424 rows with reused=202153, hashed=3271, errors=0.

## Milestone 03 - Blueprint Faction Construction Completion Readiness Audit Slice

Advanced the construction parity lane by adding `BlueprintFactionConstructionCompletionReadinessAuthority`, an audit-only completion readiness contract for future faction staged-construction jobs. The audit requires progress tick readiness, labor completion, final symbol restoration, completion inspection text, save update readiness, operation boundary readiness, and reservation release planning before any future execution owner can mark a staged site complete.

This keeps faction construction non-mutating and operation-separated: the slice does not restore tiles, configure facilities, enable machine operation, mutate room ownership, spend budget, mutate heat or suspicion, move crew, release reservations, or complete construction. Added `Milestone03BlueprintFactionConstructionCompletionReadinessAuditSmoke`, expanded the Construction Blueprints Infopedia wording, and wired the guard into Gate 3.

Verification: local Java 17 compile passed; `Milestone03BlueprintFactionConstructionCompletionReadinessAuditSmoke`, `Milestone02InfopediaMechanicsReadabilitySmoke`, and `Gate3PlayerFacingTextSmokeSuite` passed; function and Mermaid maps refreshed with 652 Java modules and 0 unpositioned modules; package-client rebuild passed with 1613 class files and 6096 package asset files; Java 17 classfile scan passed across 3226 classfiles with highest major version 61; package boot smoke stayed alive for the 8-second window; repository manifest refresh wrote 205445 rows with reused=202168, hashed=3277, errors=0.

## Milestone 03 - Blueprint Faction Construction Job Closeout Audit Slice

Advanced the construction parity lane by adding `BlueprintFactionConstructionJobCloseoutAuthority`, an audit-only closeout contract for future faction staged-construction jobs. The audit requires completion readiness, readable site status, crew release readiness, material ledger closure, budget closeout, attention closeout, and a readable completed, failed, or blocked job record before any future execution owner can close a faction construction job.

This keeps faction construction non-mutating and closeout-separated: the slice does not release crew, return materials, refund budget, mutate heat or suspicion, write job records, enable facility operation, mutate room ownership, or complete construction. Added `Milestone03BlueprintFactionConstructionJobCloseoutAuditSmoke`, expanded the Construction Blueprints Infopedia wording, and wired the guard into Gate 3.

Verification: local Java 17 compile passed; `Milestone03BlueprintFactionConstructionJobCloseoutAuditSmoke`, `Milestone02InfopediaMechanicsReadabilitySmoke`, and `Gate3PlayerFacingTextSmokeSuite` passed; function and Mermaid maps refreshed with 654 Java modules and 0 unpositioned modules; package-client rebuild passed with 1616 class files and 6096 package asset files; Java 17 classfile scan passed across 3232 classfiles with highest major version 61; package boot smoke stayed alive for the 8-second window; repository manifest refresh wrote 205468 rows with reused=202183, hashed=3285, errors=0.

## Milestone 03 - Blueprint Faction Construction Status Report Audit Slice

Advanced the construction parity lane by adding `BlueprintFactionConstructionStatusReportAuthority`, an audit-only status reporting contract for future faction construction jobs. The audit requires closeout readiness, readable summary, readable blockers, readable next action, readable timeline, and hidden raw identifiers before any future reporting owner can expose faction construction job state to ordinary UI.

This keeps faction construction non-mutating and reporting-separated: the slice does not write UI state, write job records, release reservations, mutate room ownership, mutate heat or suspicion, enable facility operation, or complete construction. Added `Milestone03BlueprintFactionConstructionStatusReportAuditSmoke`, expanded the Construction Blueprints Infopedia wording, and wired the guard into Gate 3.

Verification: local Java 17 compile passed; `Milestone03BlueprintFactionConstructionStatusReportAuditSmoke`, `Milestone02InfopediaMechanicsReadabilitySmoke`, and `Gate3PlayerFacingTextSmokeSuite` passed; function and Mermaid maps refreshed with 656 Java modules and 0 unpositioned modules; package-client rebuild passed with 1619 class files and 6096 package asset files; Java 17 classfile scan passed across 3238 classfiles with highest major version 61; package boot smoke stayed alive for the 8-second window; repository manifest refresh wrote 205489 rows with reused=202200, hashed=3289, errors=0.

## Milestone 03 - Blueprint Faction Construction Notification Readiness Audit Slice

Advanced the construction parity lane by adding `BlueprintFactionConstructionNotificationReadinessAuthority`, an audit-only notification readiness contract for future faction construction job updates. The audit requires status report readiness, readable severity, declared audience, delivery text, dedupe key, and privacy redaction before any future notification owner can alert the UI about a faction construction update.

This keeps faction construction non-mutating and notification-separated: the slice does not write UI state, enqueue notifications, write job records, reveal hidden faction data, mutate heat or suspicion, release reservations, or complete construction. Added `Milestone03BlueprintFactionConstructionNotificationReadinessAuditSmoke`, expanded the Construction Blueprints Infopedia wording, and wired the guard into Gate 3.

Verification: local Java 17 compile passed; `Milestone03BlueprintFactionConstructionNotificationReadinessAuditSmoke`, `Milestone02InfopediaMechanicsReadabilitySmoke`, and `Gate3PlayerFacingTextSmokeSuite` passed; function and Mermaid maps refreshed with 658 Java modules and 0 unpositioned modules; package-client rebuild passed with 1622 class files and 6096 package asset files; Java 17 classfile scan passed across 3244 classfiles with highest major version 61; package boot smoke stayed alive for the 8-second window; repository manifest refresh wrote 205510 rows with reused=202215, hashed=3295, errors=0.

## Milestone 03 - Blueprint Faction Construction Response Action Audit Slice

Advanced the construction parity lane by adding `BlueprintFactionConstructionResponseActionAuthority`, an audit-only response command readiness contract for future faction construction job updates. The audit requires notification readiness, readable action labels, permission checks, safety confirmation, cooldown state, and audit text before any future UI can offer inspect, resolve blockers, pause, or cancel response commands.

This keeps faction construction non-mutating and command-separated: the slice does not write UI state, execute commands, pause jobs, cancel jobs, assign crew, release reservations, mutate heat or suspicion, or complete construction. Added `Milestone03BlueprintFactionConstructionResponseActionAuditSmoke`, expanded the Construction Blueprints Infopedia wording, and wired the guard into Gate 3.

Verification: local Java 17 compile passed; `Milestone03BlueprintFactionConstructionResponseActionAuditSmoke`, `Milestone02InfopediaMechanicsReadabilitySmoke`, and `Gate3PlayerFacingTextSmokeSuite` passed; function and Mermaid maps refreshed with 660 Java modules and 0 unpositioned modules; package-client rebuild passed with 1625 class files and 6096 package asset files; Java 17 classfile scan passed across 3250 classfiles with highest major version 61; package boot smoke stayed alive for the 8-second window; repository manifest refresh wrote 205531 rows with reused=202230, hashed=3301, errors=0.

## Milestone 03 - Blueprint Faction Construction Response Execution Handoff Audit Slice

Advanced the construction parity lane by adding `BlueprintFactionConstructionResponseExecutionHandoffAuthority`, an audit-only command execution handoff contract for future faction construction response commands. The audit requires response action readiness, target resolution, command owner declaration, rollback preview, turn cost preview, and result text before any future command owner can execute inspect, resolve blockers, pause, or cancel actions.

This keeps faction construction non-mutating and execution-separated: the slice does not execute commands, inspect sites, resolve blockers, pause jobs, cancel jobs, write UI state, mutate heat or suspicion, release reservations, or complete construction. Added `Milestone03BlueprintFactionConstructionResponseExecutionHandoffAuditSmoke`, expanded the Construction Blueprints Infopedia wording, and wired the guard into Gate 3.

Verification: local Java 17 compile passed; `Milestone03BlueprintFactionConstructionResponseExecutionHandoffAuditSmoke`, `Milestone02InfopediaMechanicsReadabilitySmoke`, and `Gate3PlayerFacingTextSmokeSuite` passed; function and Mermaid maps refreshed with 662 Java modules and 0 unpositioned modules; package-client rebuild passed with 1628 class files and 6096 package asset files; Java 17 classfile scan passed across 3256 classfiles with highest major version 61; package boot smoke stayed alive for the 8-second window; repository manifest refresh wrote 205552 rows with reused=202245, hashed=3307, errors=0.

## Milestone 03 - Blueprint Faction Construction Response Result Audit Slice

Advanced the construction parity lane by adding `BlueprintFactionConstructionResponseResultAuthority`, an audit-only response result contract for future faction construction response commands. The audit requires execution handoff readiness, readable command outcome, audit ledger readiness, rollback outcome, follow-up status, and notification refresh before any future result owner can record a response result.

This keeps faction construction non-mutating and result-separated: the slice does not write result records, execute commands, alter job status, refresh notifications, mutate heat or suspicion, release reservations, or complete construction. Added `Milestone03BlueprintFactionConstructionResponseResultAuditSmoke`, expanded the Construction Blueprints Infopedia wording, and wired the guard into Gate 3.

Verification: local Java 17 compile passed; `Milestone03BlueprintFactionConstructionResponseResultAuditSmoke`, `Milestone02InfopediaMechanicsReadabilitySmoke`, and `Gate3PlayerFacingTextSmokeSuite` passed; function and Mermaid maps refreshed with 664 Java modules and 0 unpositioned modules; package-client rebuild passed with 1631 class files and 6096 package asset files; Java 17 classfile scan passed across 3262 classfiles with highest major version 61; package boot smoke stayed alive for the 8-second window; repository manifest refresh wrote 205573 rows with reused=202260, hashed=3313, errors=0.

## Milestone 03 - Blueprint Faction Construction Response Follow-up Audit Slice

Advanced the construction parity lane by adding `BlueprintFactionConstructionResponseFollowupAuthority`, an audit-only follow-up contract for future faction construction response results. The audit requires response result readiness, readable continuation intent, status cycle readiness, notification refresh readiness, closeout consequence, and rollback consequence before any future follow-up owner can schedule another construction response cycle.

This keeps faction construction non-mutating and follow-up-separated: the slice does not schedule follow-up actions, write status reports, enqueue notifications, close jobs, roll back results, mutate heat or suspicion, release reservations, or complete construction. Added `Milestone03BlueprintFactionConstructionResponseFollowupAuditSmoke`, expanded the Construction Blueprints Infopedia wording, and wired the guard into Gate 3.

Verification: local Java 17 compile passed; `Milestone03BlueprintFactionConstructionResponseFollowupAuditSmoke`, `Milestone02InfopediaMechanicsReadabilitySmoke`, and `Gate3PlayerFacingTextSmokeSuite` passed; function and Mermaid maps refreshed with 666 Java modules and 0 unpositioned modules; package-client rebuild passed with 1634 class files and 6096 package asset files; Java 17 classfile scan passed across 3268 classfiles with highest major version 61; package boot smoke stayed alive for the 8-second window; repository manifest refresh wrote 205594 rows with reused=202275, hashed=3319, errors=0.

## Milestone 03 - Blueprint Faction Construction Response Cycle Close Audit Slice

Advanced the construction parity lane by adding `BlueprintFactionConstructionResponseCycleCloseAuthority`, an audit-only close decision contract for future faction construction response cycles. The audit requires follow-up readiness, readable cycle decision, status return declaration, notification return declaration, readable unresolved blockers, and archive boundary before any future cycle owner can close or return a construction response cycle.

This keeps faction construction non-mutating and cycle-close-separated: the slice does not close response cycles, schedule status returns, enqueue notifications, archive decisions, alter job state, mutate heat or suspicion, release reservations, or complete construction. Added `Milestone03BlueprintFactionConstructionResponseCycleCloseAuditSmoke`, expanded the Construction Blueprints Infopedia wording, and wired the guard into Gate 3.

Verification: local Java 17 compile passed; `Milestone03BlueprintFactionConstructionResponseCycleCloseAuditSmoke`, `Milestone02InfopediaMechanicsReadabilitySmoke`, and `Gate3PlayerFacingTextSmokeSuite` passed; function and Mermaid maps refreshed with 668 Java modules and 0 unpositioned modules; package-client rebuild passed with 1637 class files and 6096 package asset files; Java 17 classfile scan passed across 3274 classfiles with highest major version 61; package boot smoke stayed alive for the 8-second window; repository manifest refresh wrote 205615 rows with reused=202290, hashed=3325, errors=0.

## Milestone 03 - Blueprint Faction Construction Response Cycle Archive Audit Slice

Advanced the construction parity lane by adding `BlueprintFactionConstructionResponseCycleArchiveAuthority`, an audit-only archive contract for future faction construction response cycles. The audit requires cycle close readiness, readable archive reason, retention label, privacy label, status snapshot, and replay reference before any future archive owner can preserve a construction response cycle.

This keeps faction construction non-mutating and archive-separated: the slice does not write archives, redact records, delete records, update status, enqueue notifications, alter job state, mutate heat or suspicion, release reservations, or complete construction. Added `Milestone03BlueprintFactionConstructionResponseCycleArchiveAuditSmoke`, expanded the Construction Blueprints Infopedia wording, and wired the guard into Gate 3.

Verification: local Java 17 compile passed; `Milestone03BlueprintFactionConstructionResponseCycleArchiveAuditSmoke`, `Milestone02InfopediaMechanicsReadabilitySmoke`, and `Gate3PlayerFacingTextSmokeSuite` passed; function and Mermaid maps refreshed with 670 Java modules and 0 unpositioned modules; package-client rebuild passed with 1640 class files and 6096 package asset files; Java 17 classfile scan passed across 3280 classfiles with highest major version 61; package boot smoke stayed alive for the 8-second window; repository manifest refresh wrote 205636 rows with reused=202305, hashed=3331, errors=0.

## Milestone 03 - Blueprint Faction Construction Response Cycle Readback Audit Slice

Advanced the construction parity lane by adding `BlueprintFactionConstructionResponseCycleReadbackAuthority`, an audit-only readback contract for future archived faction construction response cycles. The audit requires archive readiness, archive lookup readiness, readable summary, privacy filter readiness, readable replay reference, and stale record marker before any future readback owner can present an archived construction response cycle.

This keeps faction construction non-mutating and readback-separated: the slice does not read archives from storage, write archives, replay commands, reveal hidden faction data, update status, enqueue notifications, alter job state, mutate heat or suspicion, release reservations, or complete construction. Added `Milestone03BlueprintFactionConstructionResponseCycleReadbackAuditSmoke`, expanded the Construction Blueprints Infopedia wording, and wired the guard into Gate 3.

Verification: local Java 17 compile passed; `Milestone03BlueprintFactionConstructionResponseCycleReadbackAuditSmoke`, `Milestone02InfopediaMechanicsReadabilitySmoke`, and `Gate3PlayerFacingTextSmokeSuite` passed; function and Mermaid maps refreshed with 672 Java modules and 0 unpositioned modules; package-client rebuild passed with 1643 class files and 6096 package asset files; Java 17 classfile scan passed across 3286 classfiles with highest major version 61; package boot smoke stayed alive for the 8-second window; repository manifest refresh wrote 205657 rows with reused=202320, hashed=3337, errors=0.

## Milestone 03 - Blueprint Faction Construction Response Cycle Review Audit Slice

Advanced the construction parity lane by adding `BlueprintFactionConstructionResponseCycleReviewAuthority`, an audit-only review contract for future archived faction construction response cycle readbacks. The audit requires readback readiness, readable reviewer context, evidence links, readable allowed actions, privacy reminder, and follow-up boundary before any future review owner can offer archived construction response cycle review.

This keeps faction construction non-mutating and review-separated: the slice does not reopen cycles, execute review actions, write archives, reveal hidden faction data, update status, enqueue notifications, alter job state, mutate heat or suspicion, release reservations, or complete construction. Added `Milestone03BlueprintFactionConstructionResponseCycleReviewAuditSmoke`, expanded the Construction Blueprints Infopedia wording, and wired the guard into Gate 3.

Verification: local Java 17 compile passed; `Milestone03BlueprintFactionConstructionResponseCycleReviewAuditSmoke`, `Milestone02InfopediaMechanicsReadabilitySmoke`, and `Gate3PlayerFacingTextSmokeSuite` passed; function and Mermaid maps refreshed with 674 Java modules and 0 unpositioned modules; package-client rebuild passed with 1646 class files and 6096 package asset files; Java 17 classfile scan passed across 3292 classfiles with highest major version 61; package boot smoke stayed alive for the 8-second window; repository manifest refresh wrote 205678 rows with reused=202335, hashed=3343, errors=0.

## Milestone 03 - Blueprint Faction Construction Response Cycle Review Action Audit Slice

Advanced the construction parity lane by adding `BlueprintFactionConstructionResponseCycleReviewActionAuthority`, an audit-only action-readiness contract for future archived faction construction response cycle reviews. The audit requires review readiness, permission readiness, confirmation readiness, evidence selection, audit text, and non-reopen boundary before any future review-action owner can offer archived review actions.

This keeps faction construction non-mutating and review-action-separated: the slice does not execute review actions, reopen cycles, write archives, export evidence, reveal hidden faction data, update status, enqueue notifications, alter job state, release reservations, or complete construction. Added `Milestone03BlueprintFactionConstructionResponseCycleReviewActionAuditSmoke`, expanded the Construction Blueprints Infopedia wording, and wired the guard into Gate 3.

Verification: local Java 17 compile passed; `Milestone03BlueprintFactionConstructionResponseCycleReviewActionAuditSmoke`, `Milestone02InfopediaMechanicsReadabilitySmoke`, and `Gate3PlayerFacingTextSmokeSuite` passed; function and Mermaid maps refreshed with 676 Java modules and 0 unpositioned modules; package-client rebuild passed with 1649 class files and 6096 package asset files; Java 17 classfile scan passed across 3298 classfiles with highest major version 61; package boot smoke stayed alive for the 8-second window; repository manifest refresh wrote 205699 rows with reused=202350, hashed=3349, errors=0.

## Milestone 03 - Blueprint Faction Construction Response Cycle Review Action Handoff Audit Slice

Advanced the construction parity lane by adding `BlueprintFactionConstructionResponseCycleReviewActionHandoffAuthority`, an audit-only handoff contract for future archived faction construction response cycle review actions. The audit requires review action readiness, target resolution, command owner, rollback preview, turn cost preview, and result text before any future handoff owner can pass an archived review action toward execution.

This keeps faction construction non-mutating and handoff-separated: the slice does not hand off commands, execute review actions, reopen cycles, write archives, export evidence, reveal hidden faction data, update status, alter job state, release reservations, or complete construction. Added `Milestone03BlueprintFactionConstructionResponseCycleReviewActionHandoffAuditSmoke`, expanded the Construction Blueprints Infopedia wording, and wired the guard into Gate 3.

Verification: local Java 17 compile passed; `Milestone03BlueprintFactionConstructionResponseCycleReviewActionHandoffAuditSmoke`, `Milestone02InfopediaMechanicsReadabilitySmoke`, and `Gate3PlayerFacingTextSmokeSuite` passed; function and Mermaid maps refreshed with 678 Java modules and 0 unpositioned modules; package-client rebuild passed with 1652 class files and 6096 package asset files; Java 17 classfile scan passed across 3304 classfiles with highest major version 61; package boot smoke stayed alive for the 8-second window; repository manifest refresh wrote 205720 rows with reused=202365, hashed=3355, errors=0.

## Milestone 03 - Blueprint Faction Construction Response Cycle Review Action Result Audit Slice

Advanced the construction parity lane by adding `BlueprintFactionConstructionResponseCycleReviewActionResultAuthority`, an audit-only result contract for future archived faction construction response cycle review actions. The audit requires handoff readiness, readable command outcome, audit ledger readiness, rollback outcome, follow-up boundary, and notification boundary before any future result owner can record an archived review action result.

This keeps faction construction non-mutating and result-separated: the slice does not record result rows, execute review actions, reopen cycles, write archives, export evidence, reveal hidden faction data, update status, alter job state, release reservations, or complete construction. Added `Milestone03BlueprintFactionConstructionResponseCycleReviewActionResultAuditSmoke`, expanded the Construction Blueprints Infopedia wording, and wired the guard into Gate 3.

Verification: local Java 17 compile passed; `Milestone03BlueprintFactionConstructionResponseCycleReviewActionResultAuditSmoke`, `Milestone02InfopediaMechanicsReadabilitySmoke`, and `Gate3PlayerFacingTextSmokeSuite` passed; function and Mermaid maps refreshed with 680 Java modules and 0 unpositioned modules; package-client rebuild passed with 1655 class files and 6096 package asset files; Java 17 classfile scan passed across 3310 classfiles with highest major version 61; package boot smoke stayed alive for the 8-second window; repository manifest refresh wrote 205741 rows with reused=202380, hashed=3361, errors=0.

## Milestone 03 - Blueprint Faction Construction Response Cycle Review Action Follow-up Audit Slice

Advanced the construction parity lane by adding `BlueprintFactionConstructionResponseCycleReviewActionFollowupAuthority`, an audit-only follow-up contract for future archived faction construction response cycle review action results. The audit requires result readiness, reviewer summary, evidence disposition, status refresh, notification boundary, and closure boundary before any future follow-up owner can schedule archived review action follow-up.

This keeps faction construction non-mutating and follow-up-separated: the slice does not schedule follow-up, write summaries, move evidence, update status, enqueue notifications, close cycles, reveal hidden faction data, alter job state, release reservations, or complete construction. Added `Milestone03BlueprintFactionConstructionResponseCycleReviewActionFollowupAuditSmoke`, expanded the Construction Blueprints Infopedia wording, and wired the guard into Gate 3.

Verification: local Java 17 compile passed; `Milestone03BlueprintFactionConstructionResponseCycleReviewActionFollowupAuditSmoke`, `Milestone02InfopediaMechanicsReadabilitySmoke`, and `Gate3PlayerFacingTextSmokeSuite` passed; function and Mermaid maps refreshed with 682 Java modules and 0 unpositioned modules; package-client rebuild passed with 1658 class files and 6096 package asset files; Java 17 classfile scan passed across 3316 classfiles with highest major version 61; package boot smoke stayed alive for the 8-second window; repository manifest refresh wrote 205762 rows with reused=202395, hashed=3367, errors=0.
