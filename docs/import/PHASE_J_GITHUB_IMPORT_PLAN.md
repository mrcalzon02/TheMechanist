# Phase J — GitHub Core Import Plan

This repository is now the working home for The Mechanist.

## Incremental upload order

1. Import core source, runtime settings, manifests, launch scripts, docs, and bundled low_32 fallback art.
2. Add standard_64 art assets in a separate commit series.
3. Add intermediate_128 art assets in a separate commit series.
4. Add high_native art assets by category/shard in later controlled commits.
5. Rebuild installer/update tooling so a local install can pull changed files from GitHub instead of relying on monolithic chat-surfaced zips.

## Core policy

The core game must remain runnable with bundled low_32 art. Higher tiers are optional enhancement layers until installer/update support matures.

## Current branch

import/0.9.10kc-core
