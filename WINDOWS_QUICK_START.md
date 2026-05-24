# The Mechanist — Windows Quick Start

This package is now meant to be tested directly from the extracted zip before worrying about native installers.

## Fast path

1. Right-click the downloaded zip and choose **Extract All...**.
2. Open the extracted folder.
3. Double-click `RUN_THE_MECHANIST_WINDOWS.bat`.

Do not run the batch file from inside the compressed zip preview. Windows Explorer can show the contents of a zip without actually extracting everything; Java cannot reliably run the game from there.

## What changed in 0.9.10in

The launcher no longer disappears silently. It now:

- checks that `TheMechanist.jar` is beside the launcher,
- tries to find Java from `runtime\\bin`, `JAVA_HOME`, common Java 17 vendor install folders, and finally `java.exe` on `PATH`,
- rejects Java 8/11 before attempting to load game classes,
- runs a Java 17 startup preflight,
- writes a launch log to `%LOCALAPPDATA%\\TheMechanist\\logs\\launch-client.log`,
- pauses and prints the log if startup fails.


## Confirmed failure fixed by 0.9.10in

If your log showed something like this, the launcher was finding Java 8 first:

```text
Using Java: C:\Program Files (x86)\Common Files\Oracle\Java\java8path\java.exe
java version "1.8.0_481"
UnsupportedClassVersionError: class file version 61.0 ... only recognizes up to 52.0
```

That means the game is fine enough to reach Java, but Windows is launching the wrong Java runtime. Java class file version 61 is Java 17. The 0.9.10in launcher now skips that Oracle Java 8 path and searches for Java 17+ instead. If Java 17 is not installed, it will now say that plainly and keep the window open.

## If the window still closes or nothing appears

Open the log file:

```text
%LOCALAPPDATA%\\TheMechanist\\logs\\launch-client.log
```

The most common causes are:

- Java 17 or newer is not installed.
- Java is installed but not on PATH and `JAVA_HOME` is not set.
- The zip was not extracted before running.
- Windows Defender or SmartScreen quarantined or blocked one of the files.
- The game threw a Java exception during startup; the log should now show the stack trace.

## Java requirement for the direct zip build

The direct zip launcher requires a local Java 17+ runtime. Native jpackage installers can eventually bundle a runtime, but for now the practical debug route is:

```text
RUN_THE_MECHANIST_WINDOWS.bat
```

That launcher is intentionally console-based and diagnostic-first so Windows testing failures are visible instead of appearing as a one-frame command prompt flicker.

## Java 17 installed but reported as unreadable

If the log says Java candidates such as `C:\Program Files\Java\jdk-17\bin\java.exe` or `jdk-26` were checked but reported as `version=unreadable :: major=-1`, that was a launcher probe bug. Java writes `-version` output to stderr, and Windows PowerShell can treat that as a failure under strict error handling. The 0.9.10in launcher captures stdout/stderr through `System.Diagnostics.ProcessStartInfo`, parses the text safely, and writes the log as UTF-8.
