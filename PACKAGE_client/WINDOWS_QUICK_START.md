# The Mechanist — Windows x64 Quick Start

This guide applies only to an approved Windows x64 limited-alpha candidate.

## 1. Verify the package

1. Keep the original archive or installer until testing is complete.
2. Compare the supplied file against `SHA256SUMS.txt`.
3. Confirm the version, source commit, and platform match the private test announcement.
4. Do not use an archive that has been re-packed by another evaluator.

Windows may show a reputation warning because private alpha packages may not yet be code-signed. Confirm the checksum and approved source before proceeding. A warning is not permission to bypass package verification.

## 2. Portable extraction

Extract the entire archive to a new folder such as:

```text
C:\Users\<you>\Games\The Mechanist Alpha\
```

Do not extract only the client JAR. Keep `runtime`, `launcher`, `packages`, `manifests`, `docs`, and all command scripts together.

## 3. Start the launcher

Double-click:

```text
Run-The-Mechanist.cmd
```

Use **Verify Packages** before the first launch. The only package source should be `limited-alpha-bundled`.

## 4. Start single-player directly for diagnostics

When specifically instructed to bypass the launcher:

```text
Run-Client-Direct.cmd
```

This is not a replacement for package verification.

## 5. Start the independent-host lobby

Double-click:

```text
Run-Remote-Client.cmd
```

Enter the approved host, port, server key, and your assigned profile identity. The default allowed port is `25565`; the permitted alpha range is `25500`–`25599`.

The lobby displays a clear warning that it is a lobby and relay surface only. It should show connect/disconnect, readiness, presence, typing, roster, relay-message, and session-status controls. It should not display a world, character, map, inventory, movement, or combat controls.

## 6. Start a local headless host

Open Command Prompt or PowerShell in the package folder:

```text
Run-Server.cmd --host --no-steam --bind=127.0.0.1 --port=25565 --world-id=alpha-world --world-name="Alpha World" --max-players=4
```

Keep the terminal open while clients are connected. Press `Ctrl+C` to stop the host cleanly.

Use loopback unless the approved test specifically requires another interface. Do not expose a private alpha host to the internet without an explicit network and firewall plan.

## 7. Mutable data and credentials

The installation folder is not the save or credential folder. The client stores mutable data under the current Windows user profile, normally below `LOCALAPPDATA` when available.

Independent-host resume-token custody contains a reusable plaintext credential protected for the current user where Windows permits. Do not open, edit, publish, or attach this file to a report. Normal status and diagnostics must never print the token.

Uninstalling or replacing the application must not remove saves, profiles, logs, diagnostics, server ledgers, or remote-client credentials.

## 8. Reporting

Use the launcher diagnostics flow when possible. Include the candidate identity, exact launch script, reproduction steps, severity, and whether the defect occurs after restart or with a fresh profile.

Never include private server addresses unless required by the approved test channel, and never include resume-token custody files or token values.
