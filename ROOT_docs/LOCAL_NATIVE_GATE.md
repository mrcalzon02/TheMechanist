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

The report schema is currently `2`.

The command log is stored under:

```text
dist/local-native-gate/logs/
```

Native packaging evidence is stored under:

```text
dist/local-native-gate/installers/
```

A passing run requires all of the following:

- source distribution verification with `status=verified`;
- native staging identity matching the exact version, platform, and commit;
- reopened app-image verification with `status=verified`;
- exact platform identity throughout the evidence chain;
- exact current Git commit identity throughout the evidence chain;
- `releaseHardened=true` throughout the evidence chain;
- the governed remote-lobby entry point and native executable;
- no mutable storage inside the native payload;
- exactly one target-platform portable app-image archive;
- a non-empty SHA-256 ledger whose paths remain inside the installer output;
- a verified digest for every ledger entry;
- explicit checksum coverage of the portable app-image archive.

The Linux packaging script may currently write absolute paths to its checksum
ledger while the Windows script writes relative paths. The gate accepts both,
but rejects any path that resolves outside the governed installer-output root.

## Scope boundary

This gate certifies only the target-platform `app-image` composition and
reopening path. It does not claim installation, upgrade, repair, rollback,
uninstall, code signing, DEB, RPM, EXE, or MSI certification.

Those installer lifecycle claims require separate target-platform evidence.
