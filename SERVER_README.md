# The Mechanist — Local Server Runtime

The server executable is a separate headless entry point for the launcher → client → internal-server architecture line. It initializes the server-side save namespace, reports server runtime paths, and preserves the separation between local desktop saves and server/world authority files.

## Run

Linux/macOS:

```bash
./RUN_MECHANIST_SERVER_LINUX.sh --status
```

Windows:

```bat
RUN_MECHANIST_SERVER_WINDOWS.bat --status
```

Direct Java:

```bash
java -jar TheMechanistServer.jar --status
```

## Current authority boundary

The desktop game uses a local internal server lane for single-player world mutation. The separate headless server executable currently initializes and reports the server runtime namespace and supports controlled host-binding checks where implemented.

Remote public hosting, remote client authority, external mod hot-loading, and remote console authority remain closed unless a later verified networking pass explicitly opens them.

## Save locations

Single-player files remain separate from server files:

```text
saves/singleplayer/slot1.mechsave
saves/singleplayer/slot2.mechsave
saves/singleplayer/slot3.mechsave
saves/singleplayer/slot4.mechsave
saves/singleplayer/autosave_hourly.mechsave
saves/singleplayer/autosave_zone_transition.mechsave
saves/singleplayer/worlds/*.mechworld
```

Server state and server/world files remain separate:

```text
saves/server/server_state.properties
saves/server/slots/server_slot1.mechsave
saves/server/slots/server_slot2.mechsave
saves/server/slots/server_slot3.mechsave
saves/server/slots/server_slot4.mechsave
saves/server/worlds/*.mechworld
```

The server state file is the basic authoritative bookkeeping file for the headless runtime. The four server slots mirror the four manual single-player slots but do not share file paths with them.

## Persistence model

Single-player save slots are full bundled snapshots of the active character and world at save time.

Server-oriented character slots are character attachments. World-owned runtime namespaces belong in the server `.mechworld` file, including world state, faction continuity, player-founded organization records, reserved player command slots, site anchors, logistics ledgers, economic ledgers, news/public-continuity records, machine ledgers, and autonomous tick records. This preserves the principle that a living world authority can continue without requiring a specific player save slot to be loaded.

## Host binding

Where host-binding checks are available, the server can be launched with world and port parameters such as:

```bash
java -jar TheMechanistServer.jar --host-once --world-name="Test Spirehold" --seed=12345 --max-players=8
java -jar TheMechanistServer.jar --host --world-name="Persistent Spirehold" --seed=12345 --max-players=8 --port=25565
```

The direct TCP host path uses the configured game port range `25500-25599`, avoids system ports, and should not claim unavailable optional adapters are active. Optional network/runtime integrations must be detected honestly and fall back or fail loudly when absent.

## Security foundation

The server runtime initializes Java 17 security and authority helpers under the server storage namespace. These include administrative security logs, server-authoritative profile storage, traversal-safe path guards, packet validation utilities, replay filtering, direct relay frame caps, packet policing, load-aware throttling for acquisition traffic, degraded-state background I/O gates, AFK watchdogs, behavioral telemetry hooks, mod package validation, instrumented intrusion detection, idempotent inventory transactions, and monotonic gameplay packet sequencing where those systems are active.

Optional external adapters must not be faked. If a packaged build does not initialize a given adapter, the server must report that honestly and use the verified fallback path.

## User storage root

Server and single-player mutable data are routed through `GameStorageManager` rather than the application install folder. Linux defaults to `$XDG_DATA_HOME/TheMechanist/` when available, otherwise `$HOME/.local/share/TheMechanist/`. Windows defaults to the active user's `Documents\\TheMechanist\\` directory.

Server state remains under the `saves/server/` namespace inside that user-storage root. Server-authoritative character profiles live under `saves/data/profiles/`.

## Handshake gate

The intended authoritative join lifecycle is:

```text
identity verification → manifest delivery → acquisition/sync → client hot restart → integrity challenge → live world initialization → access granted
```

The server must not stream entity, chunk, room, sector, or simulation tick state during manifest delivery, asset acquisition, or hot restart. A validated client receives live world data only after its mounted content set passes the post-restart integrity challenge.

## Rendering note

Experimental first-person or alternative client viewports are presentation surfaces over the authoritative grid. Server/headless authority remains grid-based. Door, movement, attack, and interaction requests must still be verified by normal server-side authorities.
