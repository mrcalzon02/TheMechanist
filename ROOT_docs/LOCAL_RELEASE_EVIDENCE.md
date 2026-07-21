# Local Release Evidence Verification

After running the local Java, inventory, and native gates, verify that their reports describe one coherent release candidate:

```bash
python ROOT_build/ci/verify_local_release_evidence.py
```

On Windows:

```powershell
python .\ROOT_build\ci\verify_local_release_evidence.py
```

The verifier reads:

- `dist/local-java-gate-report.json`
- `dist/local-inventory-gate-report.json`
- `dist/local-native-gate-report.json`

It writes:

- `dist/local-release-evidence-report.json`

A passing result requires the same exact source commit across all three gates, the same target platform across the Java and native gates, `releaseHardened=true`, a structurally credible inventory with no blockers or registry errors, and a native app-image result that does not overclaim installer certification.

To require full release clearance as well:

```bash
python ROOT_build/ci/verify_local_release_evidence.py --require-clearance
```

This verifier is read-only. It does not build, package, update the repository manifest, publish, or write release history. A passing local evidence result is not a substitute for the exact GitHub prerelease publication workflow required by the limited-alpha release policy.
