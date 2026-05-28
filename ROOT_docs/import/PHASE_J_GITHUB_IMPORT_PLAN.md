# Phase J â€” GitHub Core Import Plan

The trimmed 0.9.10kc core package has been imported into GitHub as the new project baseline.

## Incremental strategy

1. Core runtime/source import.
2. Bundled low_32 fallback assets remain in the core game.
3. standard_64, intermediate_128, and high_native art tiers may be imported later in controlled commits.
4. Installer/updater work should eventually pull changed files from GitHub instead of depending on monolithic chat artifacts.
