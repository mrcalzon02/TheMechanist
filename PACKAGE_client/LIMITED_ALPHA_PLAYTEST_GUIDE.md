# The Mechanist — Limited Alpha Playtest Guide

This build is for specifically authorized playtest personnel. Do not redistribute the archive, installer, screenshots containing private build information, save files, logs, or diagnostic reports outside the approved test group.

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

## Native application image or installer

Native packages are built only on their target operating system. The directly runnable app image is the first native verification target. EXE, MSI, DEB, or RPM files may be included only when the matching target toolchain completed successfully.

Installation and uninstallation must not delete saves, profiles, settings, logs, or diagnostic reports. Report any installer that writes mutable user data into Program Files, `/opt`, or the application image.

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

## Independent host scope

The separately runnable host currently supports package/startup checks, exact-address binding, bounded relay transport, sequencing, connection, disconnect, and restart tests.

It is **not yet an authoritative remote gameplay server**. Do not report the absence of full remote world play as a new defect unless the candidate announcement explicitly opens that capability. Relevant defects include:

- A requested loopback bind listening on all interfaces.
- A host that cannot close or restart.
- Accepted replayed or badly sequenced frames.
- Unbounded frames or connections.
- Server storage appearing inside the installation directory.
- Misleading claims that remote gameplay or Steam relay is active.

## Save protection

Alpha save compatibility is not guaranteed between candidates.

Before installing a newer candidate:

1. Exit the launcher and game.
2. Copy the complete save directory shown by **Runtime Info** or the diagnostic report.
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
- Whether it occurs in a new save.
- Severity: blocker, major, moderate, minor, or cosmetic.
- The generated diagnostic report.
- A save only when requested and approved for the test channel.

Review every attachment for personal information before posting. Do not publish raw authentication tokens, private server addresses, personal directories, or unrelated files.

## Severity guide

**Blocker:** cannot install, verify, launch, create/load a world, save safely, or exit without data loss.

**Major:** major gameplay path fails, repeatable crash, severe state corruption, host exposure, or recovery failure.

**Moderate:** feature is usable only through a workaround or produces incorrect but recoverable state.

**Minor:** limited functional defect with low risk to progression or data.

**Cosmetic:** presentation issue without incorrect gameplay state.

## Candidate boundary

A candidate is ready for distribution only after its exact Windows and Linux artifacts pass the required build, Java 17, manifest, synthetic environment, single-player lifecycle, relay transport, native app-image, archive, and checksum gates. Possession of a development build or workflow artifact does not by itself make it an approved playtest candidate.
