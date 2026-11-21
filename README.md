# Nodebuff Utils

Client-side Fabric mod for Minecraft 26.3 with two 1.7/1.8 PvP input fixes, each toggleable from the config screen (Mod Menu → Nodebuff Utils → Config) or by editing `config/nodebuff-utils.json`:

- **Item Use Priority:** right-click starts item use while left-clicking (pot instead of punching), with the vanilla hold-repeat throttle intact.
- **Old Eating:** drops register in real time while the eat keeps running, and the eating slow-down flickers off per drop (little jitters at near-normal speed).

Requires Mod Menu for the config screen and its usage is recommended alongisde **Animatium and ViaFabricPlus**.

Compile with:

```
.\gradlew.bat build --no-daemon
```

#

Licensed under GPL-3.0.
