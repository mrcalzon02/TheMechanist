# ROOT_build

`ROOT_build/` is the checked-in build-operations root for The Mechanist.

Use this directory for durable build orchestration, build definitions, build process notes, and build helper entry points that are intentionally versioned with the repository.

Do not place ordinary generated build products here unless a release/package workflow explicitly identifies them as checked-in review artifacts. Transient compiler output, staged jars, package seed products, and local build directories should remain ignored runtime output or package-owned artifacts under the correct `PACKAGE_*` tree.

Canonical neighboring roots:

- `ROOT_docs/` for durable development documentation, standards, ledgers, governance, milestones, and archives.
- `ROOT_tools/` for durable tools, scanners, auditors, generators, packagers, and maintenance scripts.
- `ROOT_SRC_assets/` for protected source material.
- `PACKAGE_*` trees for package-owned runtime and distribution payloads.
