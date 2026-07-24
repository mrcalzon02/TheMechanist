# The Mechanist — Limited Alpha Playtest Guide

This build is for specifically authorized playtest personnel. Do not redistribute the archive, installer, screenshots containing private build information, save files, logs, diagnostic reports, or resume-token custody files outside the approved test group.

## Verify the candidate

Before installation or extraction:

1. Confirm the file name includes the expected version and platform.
2. Verify the archive or installer against the supplied `SHA256SUMS.txt`.
3. Keep the machine-readable verification reports with the candidate.
4. Do not use a package whose checksum, version, platform, or source commit differs from the test announcement.

Every valid candidate contains a bundled Java 17 runtime. A separate Java installation should not be required.

## Supported alpha platforms

The initial limited-alpha target is:

- Windows x64.
- Linux x64.

macOS, ARM systems, mobile devices, and other operating systems are not part of this test line unless a later candidate explicitly says otherwise.

## Portable start

Extract the complete platform archive into a new folder. Paths containing spaces are supported.

Normal launcher and single-player start:

Windows:

```text
Run-The-Mechanist.cmd
```

Linux:

```text
./run-the-mechanist.sh
```

The launcher should report `limited-alpha-bundled` as the only package source. It verifies the included launcher, client, server, and support libraries before enabling normal launch.

Do not copy only the client JAR out of the package. The launcher, manifests, bundled runtime, native libraries, client, and server are one verified candidate set.

## Independent-host lobby start

The remote-client lobby is a separate player-facing mode. It does not start a local world or mount the single-player internal host.

Windows PowerShell or Command Prompt from the extracted package root:

```text
runtime\bin\java.exe -cp "packages\client\TheMechanist.jar;packages\support\lib\*" mechanist.RemoteClientMain
```

Linux terminal from the extracted package root:

```text
./runtime/bin/java -cp "packages/client/TheMechanist.jar:packages/support/lib/*" mechanist.RemoteClientMain
```

Optional defaults may be supplied on the same command:

```text
--host=127.0.0.1 --port=25565 --server-key=limited-alpha/primary-host --profile=alpha.tester.0001
```

The lobby opens with editable host, port, server-key, and profile fields. It provides:

- Connect and disconnect controls.
- Readiness, presence, and typing-state controls.
- A connected-only authoritative roster.
- A bounded authenticated relay-message console.
- Interrogatable connection, generation, roster, and credential-custody status.
- An explicit statement that remote world authority and gameplay commands are unavailable.

The server key identifies the retained credential relationship. Testers should keep it stable for the same host and profile. Changing it intentionally creates a different local credential record.

## Native application image or installer

Native packages are built only on their target operating system. The directly runnable app image is the first native verification target. EXE, MSI, DEB, or RPM files may be included only when the matching target toolchain completed successfully.

Installation and uninstallation must not delete saves, profiles, settings, logs, diagnostic reports, or remote-client resume-token custody. Report any installer that writes mutable user data into Program Files, `/opt`, or the application image.

## Single-player test sequence

For a clean single-player pass:

1. Start the launcher and press **Verify Packages**.
2. Launch the game.
3. Create a new world and character.
4. Perform ordinary movement, interaction, inventory, and save actions.
5. Save to a numbered slot.
6. Exit normally.
7. Relaunch from the same launcher.
8. Load the save and verify character identity, location, inventory, world time, and recent actions.
9. Repeat after an unexpected client termination when specifically testing recovery.

Single-player uses a supervised in-process internal host with one authoritative world-mutation lane. Report freezes, duplicate turns, lost saves, orphan processes, shutdown delays, or state that changes after the client is closed.

## Independent-host two-tester sequence

Use two approved client profiles and one separately started headless host.

1. Start the headless host on one exact address and allowed game port.
2. Open `RemoteClientMain` on tester A and tester B.
3. Enter the same endpoint and server key, but use different profile identities.
4. Connect tester A and verify a one-player roster.
5. Connect tester B and verify both clients receive the two-player roster.
6. Change readiness, presence, and typing state on each client and verify peer roster updates.
7. Send relay messages in both directions and verify sequence numbers and payload text.
8. Disconnect tester A and verify tester A disappears from tester B's public roster.
9. Reconnect tester A and verify the same stable player ID returns at a higher connection generation.
10. Close and restart the headless host cleanly, reconnect both testers, and verify retained identities and command accounting while readiness, presence, and typing reset.
11. Confirm neither client presents movement, combat, inventory, character, map, or world-snapshot controls.

Relevant independent-host defects include:

- A requested loopback bind listening on all interfaces.
- A host that cannot close or restart.
- A client that silently falls back from invalid connection settings.
- A client that remains stuck in `CONNECTING` after credential or handshake failure.
- Accepted replayed or badly sequenced frames.
- Unbounded frames, queues, rosters, or connections.
- Offline historical identities visible to later peers.
- Resume tokens appearing in status text, logs, screenshots, or diagnostics.
- Server or client mutable storage appearing inside the installation directory.
- A lobby that mounts `GamePanel`, a local internal host, or claims remote gameplay.

The independent host is **not yet an authoritative remote gameplay server**. Reconnecting restores authenticated lobby identity and accounting, not a character inside a living remote world.

## Credential protection

The server stores only SHA-256 resume-token hashes. The client must retain the reusable plaintext token in its protected mutable profile so it can prove identity on reconnect.

Do not open, edit, rename, copy between profiles, publish, or attach the resume-token custody file to an ordinary report. A missing, corrupted, wrong-profile, or wrong-server record fails closed. If credential deletion is deliberately required, record the test reason before removing the affected client record.

## Save protection

Alpha save compatibility is not guaranteed between candidates.

Before installing a newer candidate:

1. Exit the launcher, game, lobby, and headless host.
2. Copy the complete save and mutable-profile directories shown by **Runtime Info** or the diagnostic report.
3. Label the backup with version, commit, platform, and date.
4. Keep the previous candidate until the new candidate has loaded and saved successfully.

Never use a valuable sole copy of a long-running world for destructive migration testing.

## Modified content

External mod downloading, hot loading, and public Workshop integration are not open for this alpha. Modified files can invalidate package identity and make a report unsuitable for base-game triage.

The launcher still writes a local diagnostic report when modified content is detected, but it does not automatically open a base-game issue draft.

## Reporting a problem

Use the launcher **Diagnostics** button whenever possible. Include:

- What you were trying to do.
- What you expected.
- What happened instead.
- Reproduction steps.
- Whether it occurs after a clean restart.
- Whether it occurs in a new save or a fresh remote profile.
- Severity: blocker, major, moderate, minor, or cosmetic.
- The generated diagnostic report.
- A save only when requested and approved for the test channel.

Review every attachment for personal information before posting. Do not publish raw authentication tokens, resume-token custody files, private server addresses, personal directories, or unrelated files.

## Severity guide

**Blocker:** cannot install, verify, launch, create/load a world, save safely, open the remote lobby, authenticate to the approved host, or exit without data loss.

**Major:** major gameplay path fails, repeatable crash, severe state corruption, host exposure, credential disclosure, reconnect failure, or authoritative roster failure.

**Moderate:** feature is usable only through a workaround or produces incorrect but recoverable state.

**Minor:** limited functional defect with low risk to progression or data.

**Cosmetic:** presentation issue without incorrect gameplay or session state.

## Candidate and evaluation boundary

A candidate is ready for distribution only after its exact Windows and Linux artifacts pass the required build, Java 17, manifest, synthetic environment, single-player lifecycle, supervised remote-client startup, client-to-host handshake, credential custody, roster privacy, reconnect, relay transport, native app-image, archive, and checksum gates. Possession of a development build or workflow artifact does not by itself make it an approved playtest candidate.

For investor or backer evaluation, the current alpha may demonstrate the verified installer/launcher/client/server chain, standalone single-player operation, supervised local world authority, a separately runnable exact-bind host, authenticated remote lobby presence, stable reconnect identity, authoritative connected rosters, and bounded relay messaging. It must not be presented as completed networked world gameplay until server-owned world snapshots and gameplay-command authority are separately implemented and certified.
