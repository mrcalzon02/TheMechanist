# LWJGL runtime jars

The Windows and Linux client launchers include every jar under `lib/` on the runtime classpath.

This folder is now a first-boot LWJGL runtime cache. If the required LWJGL jars are missing, the client launcher attempts to download and install them from Maven Central before the startup preflight runs. The game then launches through an explicit classpath so the jars are visible.

Pinned LWJGL version: `3.4.1`.

Required base modules:

- `org.lwjgl:lwjgl`
- `org.lwjgl:lwjgl-glfw`
- `org.lwjgl:lwjgl-opengl`
- `org.lwjgl:lwjgl-stb`

Platform native jars:

- Windows client launcher fetches `natives-windows`.
- Linux client launcher fetches `natives-linux`.

The launcher treats LWJGL as required for the desktop client after the bootstrap step. If the machine is offline and the jars are not already present, the launcher fails loudly instead of pretending the graphics backend is available. The server launcher remains headless and does not require these jars.

Use `tools/runtime/bootstrap_lwjgl_runtime.py --platform all` to pre-populate this directory during packaging.
