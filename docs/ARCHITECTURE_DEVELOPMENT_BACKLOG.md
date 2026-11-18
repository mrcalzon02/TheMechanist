# Architecture development backlog

## Vertical and underside clutter — initial implementation 2026-11-02

The existing renderer now supports seeded pipe risers, connected service bands, cable runs, clamped supports, sealed-door reliefs, repair plates, vents, lamps, hanging ducts and cable swags. New zones enable the recipe; `/devroom battlemap clutter` exposes the inspection bay. See [architecture clutter](ARCHITECTURAL_CLUTTER.md) for saved data, clearance rules, validation and follow-up inspirations. Original sign/marking art, network junctions, detailed structural repairs and actual abandoned storey geometry remain future work.

## Material transparency and guarded catwalks — implemented foundation 2026-11-02

See [the system specification](STRUCTURE_MATERIALS_AND_CATWALKS.md) for the saved data, geometry, construction costs and validation. Glass/grating roofs, wall forms and finishes, supported grating floors, unsupported gaps, and edge railings now work in the existing material test bay. Follow-up: procedural city material placement, paid catwalk/railing authoring tools, material destruction, and local stacked-storey occupancy. These additions do not complete the height-band work below.

## Height-based surface variation — requested 2026-10-29

Follow-up work; do not treat the current repeating tile surfaces as finished architecture.

- Vary materials and detailing by physical height and local building storey: foundation/plinth, occupied floors, service bands, parapets, and roof surfaces.
- Distinguish wall faces, undersides, roof tops, stair treads, and risers rather than stretching the same tile treatment over every surface.
- Add height-dependent wear, grime, water streaks, corrosion, and maintenance differences without obscuring the existing artwork.
- Keep variation deterministic from the world seed and building identity, consistent across adjacent tiles and floor transitions.
- Preserve deliberate building style and the #001a33 darkness floor; texture variation must not become flicker, arbitrary random noise, or a substitute for correct geometry.

Review this against archived city roof, facade, street, and elevated inspection views before choosing material bands and scale.

## Remaining geometry work

Separate local building storeys from main atlas levels. Existing blueprints link main atlas floors; true room-over-room geometry, stacked occupancy, cross-storey visibility, and correctly placed floor/roof slabs within a main level remain unfinished. Elevated inspection-camera screenshots demonstrate current surfaces, not completed traversable upper floors.
