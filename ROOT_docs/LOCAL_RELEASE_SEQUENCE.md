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

The Java stage requires a clean source worktree. Untracked generated files under `dist/` may remain, but tracked changes, staged changes, and untracked source files outside `dist/` block the candidate so an exact commit cannot be attached to modified source.

The sequence stops at the first failed stage and writes:

```text
dist/local-release-sequence-report.json
dist/local-release-sequence/logs/
```

Each underlying gate retains its own detailed report and logs. The coordinator also verifies that repository `HEAD` does not change between stages.

## Governed manifest review boundary

The default command does not replace the committed repository manifest. When the inventory is stale, review the generated manifest and diff, then run:

```bash
python ROOT_build/ci/run_local_release_sequence.py --update-committed-manifest
```

This form intentionally stops after the inventory stage with exit code `2` and top-level status `review-required`. It does not continue into native packaging or final evidence verification under the old commit identity.

Review:

```text
dist/local-inventory-gate/manifest-diff.txt
dist/local-inventory-gate/generated.tsv
ROOT_docs/REPOSITORY_FILE_MANIFEST.tsv
```

After review, commit the governed manifest directly to `main`, restore a clean worktree, and rerun the sequence without `--update-committed-manifest`.

Only after the committed inventory and exact release-clearance registry have both been reviewed may clearance be required:

```bash
python ROOT_build/ci/run_local_release_sequence.py --require-clearance
```

Do not combine manifest replacement and final release evidence into one candidate pass. A generated but uncommitted manifest belongs to the review boundary, not to native or clearance certification.

This command never commits, pushes, publishes, tags, or updates release history. A passing local sequence is useful evidence but does not replace the required exact GitHub-hosted Windows and Linux publication run.
