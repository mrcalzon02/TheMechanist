# The Mechanist

Java 17 hive simulation / management prototype.

This repository is being imported from the 0.9.10kc trimmed core handoff package.

## Current import strategy

The project is moving to an incremental GitHub workflow:

1. Core runtime files and bundled `low_32` fallback art are imported into the repo first.
2. Higher graphical tiers are added in controlled commits later, by tier and category rather than as monolithic archive files.
3. Future installer/update tooling should be able to pull only changed files from GitHub instead of requiring full package downloads.

## Art tier policy

- `low_32` remains bundled with the core game as the guaranteed fallback tier.
- `standard_64`, `intermediate_128`, and `high_native` are planned as later incremental imports.
- The project already has runtime support for generated-art payload roots and selectable graphics tiers.

## Java requirement

Java 17 is the target runtime.

## Status

Phase J GitHub import is underway on branch:

`import/0.9.10kc-core`
