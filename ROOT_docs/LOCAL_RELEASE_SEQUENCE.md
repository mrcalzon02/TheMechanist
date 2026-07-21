# Local Release Sequence

Use this command when GitHub Actions is unavailable and one full local release-readiness pass is required.

```bash
python ROOT_build/ci/run_local_release_sequence.py
```

The sequence runs the existing authoritative gates in this order:

1. release-hardened Java build and portable-distribution verification;
2. governed repository inventory generation and audit;
3. target-platform native app-image packaging and reopening;
4. cross-stage evidence verification.

It stops at the first failed stage and writes:

```text
dist/local-release-sequence-report.json
dist/local-release-sequence/logs/
```

Each underlying gate retains its own detailed report and logs.

The default command does not replace the committed repository manifest. When the inventory is stale, review its generated manifest and diff, then run:

```bash
python ROOT_build/ci/run_local_release_sequence.py --update-committed-manifest
```

Only after the exact release-clearance registry has been reviewed may the full clearance form be used:

```bash
python ROOT_build/ci/run_local_release_sequence.py \
  --update-committed-manifest \
  --require-clearance
```

This command never commits, pushes, publishes, tags, or updates release history. A passing local sequence is useful evidence but does not replace the required exact GitHub-hosted Windows and Linux publication run.
