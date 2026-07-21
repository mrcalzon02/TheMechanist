# Local Java 17 Release Gate

This gate reproduces the portable Java release-verification sequence when GitHub Actions execution or telemetry is unavailable. It is a diagnostic and certification input, not a release publisher. It never edits development history, creates tags, or publishes artifacts.

## Linux x64

```bash
bash ROOT_build/ci/run_local_java_release_gate.sh
```

## Windows x64

```powershell
& .\ROOT_build\ci\run_local_java_release_gate.ps1
```

Both launchers build the release-hardened distribution by default. The Python entry point may be used directly for a non-hardened development diagnosis:

```bash
python ROOT_build/ci/run_local_java_release_gate.py
```

## Required toolchain

- Git
- Python 3
- Maven
- Java 17 JDK, including `java` and `javac`
- Bash on Linux
- PowerShell 7 on Windows

The gate fails closed when a required tool is absent or when the host is not Linux x64 or Windows x64.

## Governed sequence

The gate reads `ROOT_docs/STANDARDS_AND_PRACTICES.md`, then performs the following sequence without substituting an alternate build pipeline:

1. Compile-check all release Python under `ROOT_build/ci`.
2. Verify the ProGuard release policy.
3. Parse-check the target-platform packaging script.
4. Build the client and server with Maven under Java 17.
5. Run the packaged boot smoke.
6. Run the headless server operation smoke.
7. Build the canonical portable distribution.
8. Reopen and verify the distribution and archive.
9. Run packaged Gate 3 using the bundled runtime.
10. Run the packaged synthetic environment.
11. Validate the shared synthetic release contract, including native staging requirements.

## Evidence outputs

Default outputs:

```text
dist/local-java-gate-report.json
dist/local-java-gate/
```

The JSON report records:

- exact Git commit;
- platform;
- release-hardening mode;
- standards document identity;
- every completed or failed step;
- command, timestamps, return code, and log path for every step;
- the exact failed step and traceback when the gate stops;
- canonical distribution and archive paths after packaging succeeds.

A failed command remains represented in the step array and in `failedStepResult`; failure evidence is not discarded.

Environment overrides:

```text
MECHANIST_LOCAL_GATE_OUTPUT
MECHANIST_LOCAL_GATE_REPORT
PYTHON_BIN            # Linux launcher only
```

## Acceptance boundary

One platform passing this gate proves only that platform's portable Java package and synthetic contract. Limited-alpha release still requires:

- successful Linux x64 and Windows x64 evidence for the same exact source commit;
- populated and certified repository inventory;
- hash-bound asset and dependency clearance;
- native target-platform app-image and installer evidence;
- final immutable distribution certification;
- an explicitly authorized prerelease publication run.

Do not append a verified-release development-history entry or distribute a playtest build from a single local result.
