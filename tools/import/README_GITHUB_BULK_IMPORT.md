# GitHub Bulk Import Helper

This repository is being moved to an incremental GitHub workflow.

The large initial file import should be done with a local Git client rather than through chat tool calls. After that, ChatGPT can work against the repository through issues, pull requests, and targeted patches.

## Recommended first import

Use the trimmed core handoff package:

`Mech_0.9.10kc_core_trimmed_handoff.zip`

Push it to branch:

`import/0.9.10kc-core`

Then open or update a pull request into `main`.

## Art-tier strategy

- low_32 stays in the core game.
- standard_64, intermediate_128, and high_native should be imported later in controlled tier/category commits.
- future installer/updater work should pull changed files from GitHub instead of relying on monolithic chat-surfaced archives.
