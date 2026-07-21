# Local Native App-Image Gate

This gate provides the target-platform native verification path when GitHub
Actions execution or visibility is unavailable.

It consumes an existing release-hardened canonical distribution produced for
the exact current Git commit. It does not rebuild an alternate package tree.

## Prerequisite

Run the release-hardened Java gate first:

```bash
bash ROOT_build/ci/run_local_java_release_gate.sh
```

On Windows:

```powershell
& .\ROOT_build\ci\run_local_java_release_gate.ps1
```

The native gate searches this directory by default:

```text
dist/local-java-gate/releases/
```

The directory must contain exactly one canonical distribution for the current
platform and exact Git HEAD.

## Linux

```bash
bash ROOT_build/ci/run_local_native_gate.sh
```

To select a distribution explicitly:

```bash
bash ROOT_build/ci/run_local_native_gate.sh \
  --distribution '/path/to/TheMechanist-<version>-linux-x64'
```

## Windows

```powershell
& .\ROOT_build\ci\run_local_native_gate.ps1
```

To select a distribution explicitly:

```powershell
& .\ROOT_build\ci\run_local_native_gate.ps1 `
  -Distribution 'C:\path\to\TheMechanist-<version>-windows-x64'
```

## Evidence

The machine-readable result is:

```text
dist/local-native-gate-report.json
```

The command log is stored under:

```text
dist/local-native-gate/logs/
```

Native packaging evidence is stored under:

```text
dist/local-native-gate/installers/
```

A passing run requires all of the following:

- source distribution verification;
- native staging report;
- reopened app-image verification;
- SHA-256 ledger;
- exact platform identity;
- exact current Git commit identity;
- `releaseHardened=true`.

## Scope boundary

This gate certifies only the target-platform `app-image` composition and
reopening path. It does not claim installation, upgrade, repair, rollback,
uninstall, code signing, DEB, RPM, EXE, or MSI certification.

Those installer lifecycle claims require separate target-platform evidence.
