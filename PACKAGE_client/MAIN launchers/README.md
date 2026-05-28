# The Mechanist — Client Launchers

This folder owns direct client launch helpers used for extracted-package and development-package testing.

The intended user path remains:

```text
installer → thin launcher → client → server
```

These scripts are not the final installer. They are direct client runtime helpers. Installer and native package work belongs under `installer/`; thin-launcher orchestration belongs under `launcher/`; server-only headless launch helpers belong under `client/server/launchers/`.

A packaged client runtime should place these helpers beside `TheMechanist.jar` and the runtime support-library folder they expect to verify.
