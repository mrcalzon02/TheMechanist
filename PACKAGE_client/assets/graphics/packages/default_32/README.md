# Default 32px Graphics Package

This folder is the default uncompressed, ready-to-use client graphics package.

The client graphics package selector treats this package as the baseline package. Higher-resolution packages may be installed beside it either as unzipped folders or compressed bundle zip files.

Expected package root:

```text
PACKAGE_client/assets/graphics/packages/default_32/
```

Default rule:

```text
default_32 = ready-to-use, uncompressed, installed with the client
```

Do not replace this folder with a manifest pointing somewhere else. Package assets must live here directly or in an adjacent compressed package bundle under `assets/graphics/packages/`.
