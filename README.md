# Nodebuff Utils

Client-side Fabric mod for Minecraft 26.3 with three 1.7/1.8 PvP input fixes, each toggleable from the config screen (Mod Menu → Nodebuff Utils → Config) or by editing `config/nodebuff-utils.json`:

- **Item Use Priority:** right-click starts item use while left-clicking (pot instead of punching), with the vanilla hold-repeat throttle intact.
- **Old Eating:** drops register in real time while the eat keeps running, and the eating slow-down flickers off per drop (little jitters at near-normal speed).
- **Drop Held Item On Close:** clears the held (cursor) item when the inventory closes so it does not linger as a ghost item on servers that drop it on close. On by default; disable it on vanilla servers, which return the item to the inventory instead.

Requires Mod Menu for the config screen and its usage is recommended alongisde **Animatium and ViaFabricPlus**.

Compile with:

```
.\gradlew.bat build --no-daemon
```

#

Licensed under GPL-3.0.
