# Developer combat chamber: raised battle map

The existing developer combat chamber now supports an optional three-height terrain layout. It uses the same facility session, `World`, `GamePanel`, first-person viewport, 2D planner, semantic tile textures, character artwork, and turn-owned combat as the flat arena.

## Use

In a local run with developer permissions:

1. `/devroom` enters the existing developer facility.
2. `/devroom combat` visits its combat chamber.
3. `/devroom battlemap multi` creates the raised layout.
4. `/devroom match 10 rifles 71000` places both teams across all three heights.
5. `/devroom step 1` advances one normal turn; `/devroom watch` toggles continuous observation.

The combat console also has **Battle map: multi-level terraces** and **Battle map: flat arena** buttons. `/devroom battlemap flat` clears the test combatants and restores the flat layout. Ordinary `clear` and new matches retain the selected terrain.

The first-person and 2D views use the existing control-mode setting. Mouse pitch remains under player control; the camera follows terrain elevation. The 2D view remains the current main floor's planner, with `+1`, `+2`, and stair markers over its existing artwork.

In the normal 2D view, **Page Up / Page Down** browse Z levels. **Arrows / WASD** pan while inspecting another level. **Home / Escape** return to the player's occupied level. Browsing does not move the player, change atlas coordinates, or advance a turn. The battle arena supports its three terrain heights; campaign browsing reads loaded atlas slices in physical sewer/ground order. Unloaded or unexplored slices remain unavailable rather than generating terrain or revealing hidden actors. Gameplay clicks are disabled during inspection.

Stair flights use the shipped stair-sheet cell through the normal tile registry, rotated by authored ascent direction. The gallery flight points south; east and west terrace flights point toward their respective terraces. The building blueprints author north-ascending flights. Both the 2D tile painter and first-person floor sampler consume this shared artwork, with rotations cached per source image.

## Physical layout and rules

- Central fighting lane: ground height 0.
- Side firing terraces: height +1.
- Rear gallery: height +2.
- Four quarter-height lateral steps reach the terraces; eight quarter-height steps reach the gallery.
- Player and NPC movement reject abrupt ledges. Combat routing follows the stairs.
- Terrain surfaces block firing lines in 3D. Weapon range includes elevation difference, and intervening actors/cover must intersect the firing height to obstruct it.
- Existing character/object sprites and weapon feedback are raised to their physical surface height and clipped against per-pixel terrain depth.

This first iteration is stepped terrain within the combat chamber, with ten storeys of headspace. It does not yet create overlapping rooms/floors, sewer interiors, or a second main world level. Main atlas floor identifiers are unchanged.

## Ambient light and darkness

Explicitly open tiles receive a baseline gameplay light of 28/100. Generated roads, sidewalks, parking, parks and plazas default open; roads and parks can be explicitly roofed. Buildings and enclosed rooms default covered. Roof coverage is independent of surface type, saved with the world and carried in tile snapshots. Enclosed rooms retain their existing ambient. This is diffuse arcology light, not sunlight.

Both viewport modes apply the same final exposure curve to world imagery: black becomes **#001a33**, white remains white, and low-light texture differences survive. UI panels retain their normal theme. Unseen regions remain unexposed terrain information; the navy floor does not reveal hidden actors or grant visibility through walls.

Distance fog now begins at 12 tiles and blends gradually toward 96 using a quadratic curve. Geometry traces extend to 120 tiles so the fog can fade it before the drawing limit. Radial and depth fog agree on equivalent distance samples. Rooftop upper surfaces receive passive open-area light independently of the covered rooms below them.

## Roofing construction

The 2026-11-02 extension adds `/devroom battlemap materials`, glass/grating roof recipes, wall forms/finishes and guarded grating catwalk examples. See [Structure materials and catwalks](STRUCTURE_MATERIALS_AND_CATWALKS.md) for authoritative data, collision behavior, costs and limits.

**Roof Panel** is a public recipe in **Shelter and Storage**, using the existing paid placement, staged-material and labor path. Each covered tile costs 3 supplies, 1 machine part, 2 Scrap plates, 1 Reinforced fastener set, 1 Sealing gasket set, and 6 labor turns. Wall and door construction costs remain separate; there is no free room-wide roof operation.

A panel needs a supporting wall within three tiles. Duplicate roofs and unsupported placements are refused before payment. Coverage applies only after materials and labor are complete. Completed roof receipts preserve the ground tile and leave its furniture slot free. A smoke builds a perimeter of fifteen Reinforced Wall Panels plus a Reinforced Door and pays for nine interior roof tiles, then builds furniture underneath.

## City architecture inspection

`/devroom battlemap clutter` now adds layered wall and underside service geometry in the existing chamber. See [the clutter system](ARCHITECTURAL_CLUTTER.md) and [its facility/city gallery](../diagnostics/architecture-clutter-gallery-20261102/README.md).

`CityArchitectureCaptureProbe` uses the normal structural generation stages and the existing GamePanel viewport with a seeded 500x500 city. Population/economy simulation is omitted from these architectural captures. Street, upward, facade, roof-oblique, downward and planner views record seed and camera poses alongside an audit. Elevated developer inspection cameras are labeled and do not certify reachable upper floors.

The first capture pass found late-generated roads retaining covered defaults and door tiles extending as full-height columns. Defaults now distinguish explicit roof overrides, and doors retain local height while generic wall columns extend to the next main level. Authored building plates explicitly cap their own wall segments. Room-over-room local storeys remain a separate unfinished task, described in [the architecture backlog](ARCHITECTURE_DEVELOPMENT_BACKLOG.md).

## Verification

Full Java 17 source compilation passed. The new `DeveloperBattleMapSmoke` exercises the existing facility command, three elevations, stair edges, actual player ledge rejection, vertical firing occlusion, shared ambient light, exact #001a33 compositing, native tile/character artwork, and 20 authoritative NPC combat turns. It captures four first-person angles and two 2D planner frames through `GamePanel.paint`.

Additional passing regression checks: `ActorCombatExecutionSmoke`, `DeveloperFacilitySmoke`, `Milestone02DoomHudFacingSmoke`, and `LightingFalloffStreetLightSmoke`.

Evidence: `diagnostics/multilevel-battle-20261028/captures/`. Compiled development classes: `diagnostics/multilevel-battle-20261028/classes/`. These tests do not replace the packaged client JAR.

To run the compiled development game from `PACKAGE_client` with the installed JDK:

```powershell
& 'C:/Program Files/Java/jdk-17/bin/java.exe' -cp '../diagnostics/multilevel-battle-20261028/classes' mechanist.TheMechanist
```

This invokes the game's existing main entry point. Start/load a local run and use the commands above.
