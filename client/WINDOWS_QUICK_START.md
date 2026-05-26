# The Mechanist — Windows Client Quick Start

Use this path for direct Windows testing from an extracted package before the true thin-launcher entrypoint is complete.

## Intended delivery path

```text
installer → thin launcher → client → server
```

The installer installs the thin launcher. The launcher verifies manifests and acquires or updates the client package, server package, and support libraries. The client should not download dependencies during game launch.

## Direct development fast path

1. Right-click the downloaded package zip and choose **Extract All...**.
2. Open the extracted folder.
3. Double-click `RUN_THE_MECHANIST_WINDOWS.bat`.

Do not run the batch file from inside the compressed zip preview. Windows Explorer can display zip contents without actually extracting the package; Java cannot reliably run the game from that preview surface.

## What the direct launcher checks

The direct Windows launcher is diagnostic-first. It should keep the console visible and report what failed instead of closing immediately. It checks that:

- `TheMechanist.jar` is beside the launcher,
- Java can be found from `runtime\\bin`, `JAVA_HOME`, common Java 17 vendor install folders, or `java.exe` on `PATH`,
- the detected Java runtime is Java 17 or newer,
- required LWJGL runtime jars are already packaged,
- startup preflight can load the expected client classes,
- launch diagnostics are written to `%LOCALAPPDATA%\\TheMechanist\\logs\\launch-client.log`.

## Java requirement

The direct zip launcher requires a local Java 17+ runtime. Native installer packages may eventually carry a bundled runtime, but the practical debug route remains:

```text
RUN_THE_MECHANIST_WINDOWS.bat
```

Java class file version 61 means Java 17. If the log reports an older runtime, install Java 17 or newer, set `JAVA_HOME`, or make sure the desired Java runtime appears before older Java installs on `PATH`.

## If the window still closes or nothing appears

Open the log file:

```text
%LOCALAPPDATA%\\TheMechanist\\logs\\launch-client.log
```

Common causes are:

- Java 17 or newer is not installed.
- Java is installed but not on `PATH` and `JAVA_HOME` is not set.
- The zip was not extracted before running.
- Required LWJGL/support jars were not staged by the package build or launcher acquisition layer.
- Windows Defender or SmartScreen quarantined or blocked one of the files.
- The game threw a Java exception during startup; the log should show the stack trace.

The launcher is intentionally console-based and diagnostic-first so Windows testing failures are visible instead of appearing as a one-frame command-prompt flicker.
