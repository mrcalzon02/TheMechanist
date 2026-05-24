# Art Assets

Core art assets are organized by role so the game can load compact runtime material and fall back cleanly when optional packs are absent.

Directory roles:

- `incoming/` — raw intake material.
- `source/` — retained source/reference material.
- `tiles/` — tile-scale map art.
- `sprites/` — entity/NPC/item sprites.
- `objects/` — furniture, machinery, equipment, and room-object sprites.
- `portraits/` — character portrait sheets and extracted portraits.
- `ui/` — panels, icons, cursors, buttons, and menu art.
- `materials/` — material reference plates and texture families.
- `atlases/` — packed sheets with matching slice metadata.

Missing art must fall back to ASCII/vector/text rendering and log a clear warning instead of blocking play.
