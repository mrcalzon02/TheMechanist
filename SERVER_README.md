# The Mechanist — Local Server Runtime

The server executable is a separate headless entry point for the launcher → client → internal-server line. It initializes the server-side save namespace and records where the dedicated server state and multiplayer/server world files live.

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

The desktop game still uses the local internal server lane for single-player world mutation. The headless server executable currently initializes and reports the server runtime namespace. Public network hosting, remote clients, external mod hot-loading, and remote console authority remain closed.

## Save locations

Single-player saves are separate from server saves:

```text
saves/singleplayer/slot1.mechsave
saves/singleplayer/slot2.mechsave
saves/singleplayer/slot3.mechsave
saves/singleplayer/slot4.mechsave
saves/singleplayer/autosave_hourly.mechsave
saves/singleplayer/autosave_zone_transition.mechsave
saves/singleplayer/worlds/*.mechworld
```

Server state and server/multiplayer files are separate:

```text
saves/server/server_state.properties
saves/server/slots/server_slot1.mechsave
saves/server/slots/server_slot2.mechsave
saves/server/slots/server_slot3.mechsave
saves/server/slots/server_slot4.mechsave
saves/server/worlds/*.mechworld
```

The server state file is the basic authoritative bookkeeping file for the headless runtime. The four server slots mirror the four manual single-player slots but do not share file paths with them. Desktop single-player `.mechsave` files reference their `.mechworld` definition file rather than embedding the full generated-world ledger; server save work should preserve the same slot/world-definition separation when server slots begin carrying live world state. Persistence review should use the same itemized namespace model as the desktop save audit before server slot payloads are expanded.

### Server persistence model

Server runtime files remain separate from desktop single-player files. Server state lives under `saves/server/`, server slots under `saves/server/slots/`, and server worlds under `saves/server/worlds/`. The same character-slot/world-state separation model applies when server-side save slots are opened in later networking phases.

## Save model

Single-player save slots are full bundled snapshots of the active character and the world at save time. Server/multiplayer worlds keep their world state in the server world file, while player save records remain separate character attachments.

## Server persistence model

Server character slots are character attachments. World-owned runtime namespaces are stored in the server `.mechworld` file, including player-faction membership, reserved player command slots, base state, faction strategic state, NPC-site anchors, news/bank/crime/scavenge/machine/logistics ledgers, and current mutable world state. Player-founded faction autonomous tick ledgers are also stored in the server `.mechworld` file so production, trade, defense, morale, stock/risk, public continuity, and reserved player IDs continue without requiring a specific player slot.


## Multiplayer host binding

The server executable now supports a direct host-binding check and host mode:

```bash
java -jar TheMechanistServer.jar --host-once --world-name="Test Hive" --seed=12345 --max-players=8
java -jar TheMechanistServer.jar --host --world-name="Persistent Hive" --seed=12345 --max-players=8 --port=25565
```

The direct TCP host path uses the configured game port range `25500-25599`, avoids system ports and Steam query ports, tries Steam detection first, then IPv6, then IPv4. Packaged Steam/Netty adapters are not faked when unavailable; the fallback relay treats encrypted multiplayer packets as opaque data.

## Zero-trust security foundation

The server runtime initializes a native Java 17 security core under `saves/server/`. It includes administrative security logs, server-authoritative profile storage, traversal-safe path guards, AES-GCM packet validation utilities, replay filtering, direct relay frame hard caps, per-session inbound/outbound packet policing, load-aware throttling for direct mod acquisition, graceful degraded-state background I/O gating, randomized AFK watchdogs, behavioral macro telemetry logging, and login lifecycle helpers. Steam and Netty adapters are still optional external runtime integrations; this packaged build keeps the real Java NIO fallback active rather than pretending absent adapters are loaded.



## 0.9.10hx security note

The headless server security core now exposes native Java authorities for mod package validation, instrumented intrusion detection, idempotent inventory transactions, and monotonic gameplay packet sequence enforcement. The native relay accepts legacy opaque relay lines for compatibility and validates sequenced gameplay frames when the `SEQ|<id>|<payload>` form is used.


### 0.9.10hy network optimization / local host conversion pass

This build adds the first staged implementation of low-bandwidth replication and local-host conversion infrastructure: server-side snapshot delta compression, client prediction/reconciliation, remote entity interpolation buffers, NAT/STUN discovery helpers, Linux systemd deployment assets, disaster-recovery crash dumps, explicit single-player-to-local-multiplayer warning/lock flow, host-key/password admission gates, local host dashboard panels, transactional host-profile/world-data splitting, and multiplayer respawn fallback placement. Netty and Steam remain optional seams; this build does not fake those runtimes when their dependencies are absent.

## User storage root

Server and single-player mutable data are routed through `GameStorageManager` instead of the application install folder. Linux defaults to `$XDG_DATA_HOME/TheMechanist/` when available, otherwise `$HOME/.local/share/TheMechanist/`. Windows defaults to the active user's `Documents\TheMechanist\` directory. Server state remains under the `saves/server/` namespace inside that user-storage root, and server-authoritative character profiles live under `saves/data/profiles/`.


## Handshake hot-restart gate

The authoritative join lifecycle is now: identity verification, manifest delivery, acquisition/sync, client hot restart, integrity challenge, live world initialization, and access granted. The server must not stream entity, chunk, room, sector, or simulation tick state during manifest delivery, asset acquisition, or hot restart. A validated client receives live world bytes only after its newly mounted mod set passes the post-restart salted integrity challenge.


## 0.9.10ib obfuscation note

Native installer builds now use ProGuard-obfuscated artifacts. Server operators running development zips may still use `TheMechanistServer.jar`; release packaging produces `target/TheMechanistServer-obfuscated.jar` and stores encrypted de-obfuscation maps under `dist/secure-maps/` for developer-only crash analysis.


## 0.9.10id client rendering note

The new experimental `doom` mode is a client-side QoL-locked viewport over the authoritative grid. Server/headless authority remains grid-based; door and attack requests coming from the viewport must still be verified by the normal server-side interaction and combat authorities.
