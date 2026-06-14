# Java 17 Swing Options UI Reference

Primary reference: <https://docs.oracle.com/en/java/javase/17/docs/api/java.desktop/javax/swing/package-summary.html>

Component policy for The Mechanist options and multiplayer menus:

- Use `JSlider` for bounded numeric values such as volume, scale, zoom, field of view, screen shake, and JVM heap.
- Use grouped `JRadioButton` controls for explicit On/Off choices.
- Use `JColorChooser` for custom UI and text colors; palette presets may remain a `JComboBox` or existing dropdown.
- Use `JPasswordField` for server addresses and passwords when a menu can appear on a stream or recording.
- Use `JComboBox` for short mutually exclusive named modes such as renderer quality, fog model, GC profile, and window mode.
- Use `JSpinner` for exact bounded integral values such as ports, player limits, and retry counts.
- Use `JFileChooser` for payload roots, save/import/export locations, and local package selection.
- Use `JTabbedPane` if the custom-painted options tabs are later replaced with a native Swing form.
- Use `JFormattedTextField` with validation for values that require a specific textual format but are not secrets.

Privacy rule: player-entered, recent, favorite, local-host, public-host, IPv4, and IPv6 endpoints are hidden by default. A deliberate reveal action may expose them temporarily; ordinary menu painting, logs intended for players, screenshots, and status summaries must remain redacted.
