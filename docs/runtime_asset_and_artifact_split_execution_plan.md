# Runtime Asset Binding and Client/Server Artifact Split Execution Plan

This plan defines the next repair sequence for The Mechanist runtime packaging and build system. The current test proved a critical fact: the same jar succeeds when launched from inside `PACKAGE_client`, but fails to resolve sound and graphics when launched from outside that directory. Therefore the immediate defect is not simply missing assets. The immediate defect is runtime asset lookup and artifact packaging being tied to process working directory and older folder assumptions.

## Non-negotiable package rule

The client package must be independently runnable. Runtime code must not require a repository root above it, source assets beside it, ROOT tools, placeholder manifests, README pointer files, or a particular shell working directory.

The server package must be independently runnable as a server. It must not be a renamed or copied full client jar. It must not ship client Swing panels, title art, GUI frames, portrait sheets, client sound effects, or graphics packages unless those assets are explicitly required by the server runtime.

## Desired end state

`PACKAGE_client/` contains a real client package with `TheMechanist.jar`, physical runtime assets under `PACKAGE_client/assets/`, launchers that set the process working directory to the client package root, cwd-independent Java asset resolution as a second layer of protection, and a startup preflight that fails loudly if required client assets are missing.

`PACKAGE_server/` contains a real server package with `TheMechanistServer.jar`, server configuration and data folders only, no client-only Swing UI classes, no client graphics packages, no title screen art, and no client audio payload unless explicitly required.

## Phase 1 — Lock down runtime asset resolution

Goal: make every asset lookup resolve from the installed client package, not from the shell working directory.

Tasks:

1. Integrate `RuntimePathResolver` into all image/audio/runtime path reads.
2. Replace hand-rolled `new File(path)` asset checks with `RuntimePathResolver.resolveAssetFile(path)`.
3. Patch `SoundManager.runtimeAssetFile` so sound works from repo root, client root, launcher, and packaged shortcuts.
4. Patch `ImageCache.read` so title art, backdrop, clouds, GUI frames, portraits, and loaded images resolve from the client package.
5. Patch `ArtPackManager.prepareAndResolveRoot`, `resolveBundledFallbackRoot`, `resolveQualityRoot`, and `resolveQualityCellsRoot` so the art root is located under the actual package root first.
6. Patch `AudioPackManager.prepareAndResolveMusicRoot` so music roots resolve from the actual package root first.
7. Add audit logging that prints cwd, jar directory, resolved client root, resolved art root, tile cell root, loaded tile alias count, loaded semantic icon count, and loaded sound handle count.

Acceptance criteria:

- `cd PACKAGE_client; java -jar TheMechanist.jar` works.
- `java -jar PACKAGE_client/TheMechanist.jar` works from repository root.
- The Windows launcher works.
- Tile art aliases load above zero.
- Sound handles load above zero.
- The title, subtitle, backdrop, and cloud images load from `PACKAGE_client/assets`.

## Phase 2 — Bind graphics packages to the running program

Goal: make the display layer use the installed asset package instead of silently falling back to ASCII or generated UI.

Tasks:

1. Treat `PACKAGE_client/assets/a/r` as the legacy runtime art root until the renderer is fully migrated.
2. Treat `PACKAGE_client/assets/graphics/packages/default_32` as the installed selectable graphics package.
3. Add one runtime graphics package resolver that returns the selected package root and defaults to `default_32`.
4. Make `TileArtSystem` load from that resolver instead of hardcoded legacy guesses.
5. Preserve compatibility with the current `tiles/quality/low_32/cells` structure.
6. Add a renderer preflight check that explicitly verifies representative tile files before the main panel draws.
7. Make the main panel show a visible asset warning if tile art fails, instead of silently rendering a blank/no-art game view.

Acceptance criteria:

- Main game panel renders actual tile assets.
- A missing tile package produces a clear in-game diagnostic message.
- `default_32` is the default selected package.
- Runtime does not require `ROOT_SRC_assets`.

## Phase 3 — Rebuild launch and package authority

Goal: stop mixing source rebuild tools, launchers, and runtime packages into ambiguous paths.

Tasks:

1. Keep source art in `ROOT_SRC_assets` as source authority only.
2. Keep runtime client assets in `PACKAGE_client/assets` as runtime authority only.
3. Keep build products in a build/output folder before they are copied into the package.
4. Keep the Windows PowerShell launcher working from package root.
5. Update the native launcher/bootstrapper to set `CreateProcessW` working directory to the package root.
6. Update direct client-run documentation so direct jar testing either runs from `PACKAGE_client` or relies on cwd-independent resolver support after Phase 1.

Acceptance criteria:

- Packaged launcher and direct jar invocation from repository root both resolve assets.
- Launcher logs include resolved runtime root and cwd.
- No launcher depends on ROOT folders above the client package.

## Phase 4 — Audit the current jars

Goal: prove what is inside `TheMechanist.jar` and `TheMechanistServer.jar`, and stop allowing duplicated client/server artifacts.

Tasks:

1. Add a jar auditor that records byte size, manifest main class, class count, package/class summary, Swing/AWT client class presence, server entrypoint presence, asset/resource payload presence, and hash digest.
2. Produce `docs/jar_artifact_audit.json` and `docs/jar_artifact_audit.tsv`.
3. Flag server jar failure if it is byte-identical or hash-identical to the client jar.
4. Flag server jar failure if it contains client-only packages/classes after the split.
5. Flag client jar failure if it lacks the client entrypoint.
6. Flag server jar failure if it lacks the server entrypoint.

Acceptance criteria:

- Audit states whether the current server jar is a duplicate client jar.
- Audit identifies the manifest main class for each jar.
- Audit distinguishes client-only classes from server/shared classes.

## Phase 5 — Split source ownership into client, server, and shared layers

Goal: make it possible to build different jars from different source sets.

Target organization:

```text
src/mechanist/shared/     Shared simulation, data models, map generation, save structures, networking DTOs.
src/mechanist/client/     Swing UI, GamePanel, rendering, input, audio playback, client launch health check.
src/mechanist/server/     Server main, server loop, networking listeners, headless simulation authority.
src/mechanist/            Temporary compatibility package while extraction continues.
```

Bridge rule: do not do a giant package rename all at once. First separate build lists and entrypoints while keeping existing package names where necessary. Then gradually move classes into real packages when dependencies are understood.

Tasks:

1. Identify current server entrypoint or create a minimal `mechanist.TheMechanistServer` if one does not exist.
2. Identify client entrypoint: `mechanist.TheMechanist`.
3. Identify client-only classes: GamePanel, Swing screens, ImageCache, SoundManager, visual/audio runtime classes.
4. Identify shared classes: simulation model, zones, factions, items, worldgen, serialization.
5. Identify server-only classes: server main, network host, headless loop.
6. Create explicit build source lists for client and server.
7. Keep shared code in both jars until a common shared jar is introduced.

Acceptance criteria:

- Client jar launches the game client.
- Server jar launches a server entrypoint or cleanly prints server usage/preflight.
- Server jar does not contain GamePanel/Swing UI classes after split.

## Phase 6 — Build distinct artifacts

Goal: produce a real client jar and a real server jar.

Tasks:

1. Update or create a build script that produces `PACKAGE_client/TheMechanist.jar` and `PACKAGE_server/TheMechanistServer.jar`.
2. Client jar manifest main class: `mechanist.TheMechanist`.
3. Server jar manifest main class: `mechanist.TheMechanistServer` or final server entrypoint.
4. Client package receives client assets.
5. Server package receives server config/data only.
6. The artifact auditor runs after build and fails if the jars are identical.

Acceptance criteria:

- Jars have different sizes and different hashes.
- Jars have different main classes.
- Server jar does not include client-only Swing/graphics/audio code after the split target is complete.
- Packages can be zipped independently.

## Phase 7 — Profile manager internalization

Goal: profile confirmation must be game-internal, not a separate OS window.

Tasks:

1. Locate current profile manager implementation.
2. Remove or disable standalone `JFrame` / external window behavior for normal game flow.
3. Add an internal game panel/screen state for profile management.
4. Require the player to open that panel before confirming the required profile acknowledgement.
5. Store the confirmation in profile/settings data, not as a transient external dialog.
6. Ensure it follows the same wrapped text and button layout standards as the rest of the UI.

Acceptance criteria:

- No separate profile manager OS window appears in normal flow.
- Profile confirmation occurs inside the main game window.
- The user can open the panel and explicitly confirm.

## Phase 8 — UI text/layout repair after asset path repair

Goal: repair visible UI standards only after the runtime is definitely loading the correct assets.

Tasks:

1. Extract shared wrapped-text helpers from `panelLines`.
2. Apply wrapped compact text to worldgen, sector intro, profile manager, options, and lore crawl surfaces.
3. Replace remaining borrowed/copyright-adjacent zone-intro terms with Concord terminology.
4. Enlarge main world viewport after tile rendering is confirmed.
5. Ensure lighting overlay is drawn only over the intended world viewport, not over the entire main panel.

Acceptance criteria:

- Worldgen text does not overrun panel bounds.
- Zone intro text uses Concord terminology.
- Main world viewport consumes more screen space.
- Lighting overlay no longer blankets unrelated UI panel areas.

## Execution order

1. Runtime path resolver integration.
2. Graphics package resolver binding.
3. Jar artifact audit.
4. Client/server build split.
5. Package/launcher authority cleanup.
6. Profile manager internalization.
7. Wrapped-text and panel-layout cleanup.
8. Final package verification.

## Immediate next implementation target

Patch source-level asset resolution first: `SoundManager`, `ImageCache`, `ArtPackManager`, and `AudioPackManager` must call a shared cwd-independent resolver. This is the first source-code repair because it explains why direct root launch fails while package-root launch succeeds.
