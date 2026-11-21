# Hit Desync Research — nodebuff-utils / Minecraft 26.3

**Date:** 2026-11-20
**Scope:** Read-only research for the client-side Fabric mod `nodebuff-utils` on Minecraft Java 26.3 ("Wilderness Bound"). No source code was changed by this research. Nothing here recommends or implements a code change (see verdict).

---

## 1. Scope note

`nodebuff-utils` is a deliberately tiny, client-side-only Fabric mod for Minecraft 26.3 with exactly two toggleable 1.7/1.8 PvP **input** features — **Item Use Priority** and **Old Eating** — plus a Mod Menu config screen. This report consolidates three completed research lanes:

- **T4** — attack-packet mechanics, server validation, five desync classes (sources S1–S8).
- **T5** — six ranked Fabric 26.3 tweak candidates, feasibility labels, exact 26.3 symbols (sources S9–S19).
- **T6** — existing solutions (ViaFabricPlus, Animatium→Vidalia), the 26.3 changelog, and the worth-implementing verdict (sources S20–S27).

Two in-repo precedents are also used: the 26.3 migration commit `94e26ef`, and the **T2 javap finding (msg-44)** that the two "obvious" inventory-desync fixes were no-ops / unsafe on 26.3 and were removed rather than shipped.

Claims are tagged **[FACT]** (documented by a cited source or direct bytecode), **[OBSERVATION]** (community-reported), or **[INFERENCE]** (reasoned, not explicitly cited). Unresolved items are listed in §11.

---

## 2. Executive summary

Hit registration in 26.3 is **server-authoritative**: the attack packet carries only an entity id, and the server independently re-validates line-of-sight, reach, and cooldown against its own entity positions and tick timing before applying damage. No client-only tweak can make a hit the server rejects register. Every plausible "hit-desync tweak" (hitbox inflation, old sneak height, client-sided entity movement, attack timing/re-send, reach tuning) is **already implemented** by Animatium/Vidalia or ViaFabricPlus — both of which `nodebuff-utils` already recommends in its own README — or is cosmetic only. 26.3 changed **no** combat or hit-registration code versus 26.2 (only SDL3 input). Adding attack/entity/packet mixins to a two-feature "just works" mod multiplies `defaultRequire:1` hard-crash risk on churn-prone unobfuscated Mojang names, with T2's removed no-op mixins as direct precedent.

> **VERDICT: DO NOT implement any hit-desync tweak in nodebuff-utils for 26.3.** Point users to Vidalia / ViaFabricPlus instead.

---

## 3. What hit desync is

**Premise correction [FACT]:** `ServerboundSwingPacket` (26.3: `ServerboundPunchPacket`) is the **arm-animation** packet (`handleAnimate`), **not** the attack. Attack is its own packet. [S1][S15][S19]

**Attack packet identity [FACT]:**
- Serverbound `attack`, Play ID **0x01**, carries a **single field: Entity ID (VarInt)** — no position, no raycast, no direction. Sent when the client attacks an entity. [S1]
- `interact` (0x1A, right-click) and `player_action` (0x29) are **separate** packets. [S1]
- Clientbound `swing_animation` (0x7B) / `hurt_animation` (0x2B) are animation feedback only. [S1]
- 26.3 renames `minecraft:swing_animation` → `attack_animation` + `interact_animation`. [S9]

**Version timeline of the attack packet [FACT]:**
- **1.8:** one `PacketPlayInUseEntity` "Use Item" (`C0A`/`0x0A`) with an action enum; server branches on `== ATTACK` then calls `this.player.attack(entity)`. [S4]
- **1.19–1.21:** folded into `ServerboundInteractPacket`, handled by `ServerGamePacketListenerImpl.handleInteract(...)`; `ATTACK` is one `Action`. [S2][S3]
- **26.x:** attack is **split out** into a dedicated `ServerboundAttackPacket(int entityId)` with `handleAttack()`, distinct from `handleAnimate(ServerboundSwingPacket)` and `handleInteract`. [S5][S15][S17]

**Desync definition [FACT architecture]:** the client raycasts/picks at **frame rate** against **interpolated** entity positions, plays swing/hit feedback optimistically, and sends the entity id. The server re-validates at **20 TPS** against authoritative state. When the two disagree, the client shows a hit but no damage lands (or, less often, shows a miss while damage lands). [S1][S2]

---

## 4. Server-authoritative validation

- **Entry point [FACT]:** `handleAttack()` / `handleInteract()` → resolves the target via `getEntityOrPart(packet.entityId())` → `ServerPlayer.attack(Entity)`. [S2][S3][S17]
- **Reach + LOS gate [FACT]:** vanilla server accepts the attack only if the entity is visible (unobstructed line-of-sight) and within ~4 units. Reach is governed by `entity_interaction_range` (default **3**, creative **5**; `block_interaction_range` 4.5). [S1][S11]
- **1.8 validation [FACT]:** invalid targets (items, experience orbs) rejected with disconnect "Attempting to attack an invalid entity"; reach via configurable `maxReach`. [S4]
- **Damage recomputed server-side [FACT]:** the server recomputes damage via `getAttackStrengthScale(0.5f)`; damage multiplier `0.2 + charge²·0.8`, enchant damage `·= charge`. **Damage is never client-trusted.** [S6][S7][S10]
- **Cooldown formula [FACT]:** `T = 20 / attack_speed` ticks; multiplier `0.2 + ((t+0.5)/T)² · 0.8`, clamped `0.2..1`; driven by `attackStrengthTicker` / `resetAttackStrengthTicker()` / `getAttackStrengthScale(float)`. Crit/sweep ≈ 84.8%. [S8][S10]
- **1.9 introduced the attack cooldown** (meter → 20%–100% damage; sword speed 1.6, axe 0.8). **1.8 had no cooldown** (fixed hit delay), enabling spam-clicking. This is the root of the "1.8 feels different" debate. [S8]

**Consequence [FACT]:** the client controls only *what it sends* (timing, claimed target, visual hitbox). It cannot make the server count a hit the server rejects on reach, LOS, or cooldown.

---

## 5. Five desync classes

| # | Class | Mechanism | Confidence |
|---|-------|-----------|------------|
| a | **Reach disagreement** | Client raycast/pick uses its own reach vs server's ~4u / `entity_interaction_range`=3. | [FACT] architecture [S1][S11] |
| b | **LOS from interpolation** | Interpolation can let the client see/attack around a corner while the server raycast is blocked. | [FACT] mechanism [S1] |
| c | **Position error over RTT** | Client renders delayed snapshots; over RTT the target was elsewhere server-side when the packet arrives. | [FACT] architecture [S1] |
| d | **Cooldown meter vs server tick** | Client meter reads full but the server tick counter disagrees; damage scaled server-side. | [FACT] [S8][S10] |
| e | **Latency** | Packet arrives ~RTT/2 later; server checks **current** positions — no rewind / lag compensation. | **[INFERENCE]** — no explicit vanilla "no lag comp" citation found. [S1][S8][inference] |

**Manifestation [FACT]:** local swing/hitmarker with no damage, or server damage when the client showed a miss. Protocol reconciliation is targeted (stale movement packets ignored until matching Confirm Teleportation) — there is **no general hit reconciliation**. [S1]

---

## 6. Candidate tweaks (all six from T5, ranked feasible → not feasible)

**Key point: NONE of these makes an invalid hit register.** They can only change perceived feedback, input timing, or client visuals. The two existing mod features work because item-use/eating are **client-timed and accepted at face value** — hit registration is categorically different (server-validated). [S14][S16]

| # | Candidate | What it changes | Feasibility | Risk |
|---|-----------|-----------------|-------------|------|
| 1 | **Predicted local hit feedback** (hitmarker / instant swing + sound) | Target `MultiPlayerGameMode.attack(Player,Entity)` or `Minecraft.startAttack` @HEAD/TAIL; play hitmarker/hurt sound at attack time instead of waiting for the server damage packet. | **Feasible — cosmetic**, moderate value | **False positives:** server may reject while the marker still flashes, *increasing* perceived desync. Gate on local raycast + reach + cooldown. [S14] |
| 2 | **Swing timing / attack–swing pairing** | Target `MultiPlayerGameMode.attack` @INVOKE of send; control ordering of `ServerboundAttackPacket` vs `ServerboundPunchPacket.INSTANCE` + local `player.swing(...getAttackAnimation(),false)`. | **Feasible**, low value | Vanilla sends both same tick → ~0 latency gain; reorder/suppress can trip anticheat or desync animations. [S15][S16][S19] |
| 3 | **Cooldown visual / 1.8-style instant feel** | Mixin `Player.getAttackStrengthScale(float)` / Gui indicator / `resetAttackStrengthTicker()`; hide/freeze the cooldown bar. | **Feasible — cosmetic only**, ZERO registration effect | Purely visual; server still enforces cooldown + damage scaling, so fast hits will NOT land. Only cosmetically useful on 1.8 servers via ViaFabricPlus. [S10] |
| 4 | **Client pick / raycast reach tuning** | Target `Minecraft.startAttack` selection / `LocalPlayer.raycastHitResult(float,Entity)` / `Entity.pick`; `@ModifyVariable` on reach arg to match server's 3-block reach and compensate interpolation. | **Borderline**, low value | **High risk:** extending client reach past server reach sends attacks the server rejects; mis-tuning targets the wrong entity (worse desync). Prefer aligning *to* server rules, not extending. [S12] |
| 5 | **Entity interpolation / latency compensation** | Target `Entity.lerpTo` / `AbstractClientPlayer` position lerp. | **NOT feasible for registration**, borderline cosmetic | Client only has delayed server snapshots — no authoritative position available client-side; can trade smoothness for slightly-less-stale render (jitter) but cannot change where the server thinks the target was. Skip. [S1] |
| 6 | **Packet-level via Fabric API / networking** | `ClientPreAttackCallback` (Fabric API 26.3, fires in `handleKeybinds`, cancel-only); synthetic `ServerboundAttackPacket`. | **NOT feasible for registration** | Callback is cancel-only (no reach/timing params). Fabric networking cannot alter vanilla packets as validated server-side; spoofed target still faces reach/cooldown/LOS checks. Do NOT build registration on this. [S12][S13] |

---

## 7. Existing solutions (both already in nodebuff's README)

- **ViaFabricPlus** [FACT] — Fabric-only, client-side, multiplayer-only, newest-client. Modrinth/GitHub/CurseForge. Support table: **MC 26.3 = Feature Updates YES + Bug Fixes YES**; 26.2 = bugfixes only. Translates server protocols 1.0–26.3 (Beta/Alpha/Classic/CPE, Combat Test 8c, +Bedrock addon) and "recreates the classic Minecraft experience" with fixes to movement, block/entity interactions. Code evidence (branch `ver/26.2`): version-gated mixins (`<= v1_8`) cancel offhand interact and emulate 1.8 item-use; `PositionInterpolator1_8` for 1.8 entity interpolation; ViaLegacy for 1.7 client-side entity movement. **Anti-cheat may flag it.** It already does 1.7/1.8 PvP input/protocol emulation on 26.3. [S20][S21][S22]
- **Animatium → renamed "Vidalia"** [FACT] — Modrinth `/mod/animatium` now redirects to `/mod/vidalia`; CurseForge likewise. "Legacy animations mod … 1.7/1.8 era and more"; supports 26.3/26.2/26.1.x/1.21.4–1.21.11; client-side, YACL config. Documented features include **Pick Inflation** (0.1 pick/hitbox inflation on attack), **Old Sneak Height** (≤1.8 sneaking hitbox/eye height), **Client-Sided Entity Movement** (1.7 movement), **Miss Penalty**, **Mining Item Usage**, and **Left Click Item Usage** — the latter **duplicates nodebuff's own Item Use Priority**. A server can toggle these via `animatium:set_features`. [S23][S24]
- **GroundedGrim** [FACT] — async predictive movement/anticheat, not a PvP client tweak. [S25]
- **No dedicated 26.3 "reach analyzer / hit-fix" mod surfaced** [OBSERVATION] — search engines rate-limited, Modrinth search blocked; **Medium confidence**. [S20]

Both ViaFabricPlus and Animatium/Vidalia are **already recommended in `README.md:8`** of nodebuff-utils.

---

## 8. 26.3 change assessment

**No combat / hit-registration change vs 26.2 [FACT].** The official 26.3 "Wilderness Bound" changelog and Wiki list biome/block/structure/UI/datapack changes; there is **no** combat, hit-registration, reach, or attack-packet change. Protocol **777**, Java 25, data version 5023. [S9][S26]

The only input-related deltas:
- **SDL3 replaces GLFW** for windowing/input (keyboard uses SDL scancodes / physical keys; Raw Input mouse setting removed; macOS Right-Click Emulation option). [S9][S26]
- `minecraft:swing_animation` → `attack_animation` + `interact_animation`. [S9]

**Conclusion [FACT]:** 26.3 introduces no new mechanic that creates a hit-desync opportunity. The only relevant delta is input handling, already covered by the migration's SDL3 smoke-test (migration commit `94e26ef`; T1/T2). Do not add a feature for it.

---

## 9. Scope / maintenance / fragility rationale

- **Scope [FACT]:** two unrelated input features + a config screen. A hit-desync tweak is a third, different domain (attack packets / entity positions), not input timing. [README.md:3-8]
- **Duplication [FACT]:** Vidalia already ships Pick Inflation, Old Sneak Height, Client-Sided Entity Movement, and Left Click Item Usage (= nodebuff's own feature); ViaFabricPlus already emulates 1.7/1.8 PvP on 26.3. Both are already recommended. Implementation would directly overlap them. [S20–S24]
- **Mojang de-obfuscation / Yarn retirement [FACT]:** Mojang stopped obfuscating Java builds ~2025-10-29; 26.x uses unobfuscated Mojang names, Yarn retired, LoomNoRemap (`net.fabricmc.fabric-loom`). Every new mixin must be javap-verified against the 26.3 jar; attack/entity/packet classes are large and change often. [S27][S9][migration commit `94e26ef`]
- **`defaultRequire:1` hard-crash risk [FACT]:** `nodebuffutils.mixins.json` sets `defaultRequire:1`; ANY injection-point miss = mod-load hard crash. T2 proved the pattern: a plausible `InventoryDelayMixin` (returning `getStateId()` in both branches) was a genuine **no-op** that still risked a hard crash — it was deleted rather than shipped. Similarly `InventoryCloseMixin` would have deleted legitimate cursor items because carried is server-owned. Net T2 code change was only config `load()` null-safety. This is direct precedent for not shipping speculative desync mixins. [T2 msg-44]
- **26.3 symbols confirmed present [FACT]:** `Minecraft.startAttack():boolean`, `continueAttack(boolean)`, `handleKeybinds`, `startUseItem`; `MultiPlayerGameMode.attack/interact/dropItem/useItemOn`; `LocalPlayer.raycastHitResult(float,Entity)`; `ServerboundAttackPacket(int entityId)`; `ServerboundPunchPacket.INSTANCE`; `SwingSource.PREDICTED`; `player.swing(InteractionHand, SwingAnimation, boolean sendPacket)`. [S12][S14][S15][S16][S18][S19]

---

## 10. Per-candidate verdict + overall recommendation

| Candidate | Verdict | One-line reason |
|-----------|---------|-----------------|
| Hitbox inflation (0.1 pick) | **NO** | Already shipped by Vidalia "Pick Inflation"; duplicating adds a fragile entity/attack mixin for zero unique value. |
| Old sneak hitbox/eye height | **NO** | Already in Vidalia "Old Sneak Height"; server owns the authoritative hitbox, client visuals alone cannot change sneaking hits. |
| Client-sided (1.7) entity movement | **NO** | Already in Vidalia + ViaFabricPlus (ViaLegacy); large, risky mixin surface, heavy overlap. |
| Attack re-send / timing / target override | **NO** | Validation is server-side (LOS + ~4u reach + cooldown); client cannot force a rejected hit to count; anticheat-flaggable; T2 proved analogous fixes are no-ops. |
| Reach extension / reach analyzer (visual) | **NO / MAYBE** | Purely visual overlay is not a hit-desync fix and changes no server registration; optional cosmetic at best, not worth scope creep. |
| SDL3 / physical-key input (new in 26.3) | **MAYBE (already covered)** | Only real 26.3 hit-PvP-adjacent change, but it is an input/keybind concern (not hit desync) and is already handled by the migration's SDL3 smoke-test. |

**OVERALL: DO NOT IMPLEMENT.** (1) The category is dominated by server-authoritative validation a client mod cannot override; (2) every plausible tweak is already provided by Animatium/Vidalia or ViaFabricPlus — both already in nodebuff's README; (3) 26.3 changed no combat/hit-registration code vs 26.2 (only SDL3 input); (4) added mixins to a two-feature "just works" mod multiply `defaultRequire:1` hard-crash risk on churn-prone Mojang names, with T2's removed no-op mixins as direct precedent. **Keep nodebuff-utils focused; point users to Vidalia / ViaFabricPlus.**

---

## 11. Confidence + caveats

- **High confidence:** version facts and attack-packet timeline, server-authoritative validation (LOS/reach/cooldown/damage), 26.3 changelog (no combat change), feature overlap with Vidalia/ViaFabricPlus, T2 no-op precedent. All backed by multiple independent 26.3-facing sources and/or direct bytecode.
- **Medium confidence:** exact benefit magnitudes of predicted-feedback / timing tweaks (no benchmark run); claim that "no other dedicated 26.3 hit-fix mod exists" (search engines rate-limited, Modrinth search blocked).
- **Unresolved / not cited:** exact client `pick()` / `startAttack()` bytecode bodies (code grep returned no hits; local jar could not be decompiled read-only); no explicit vanilla "no lag compensation" statement — class (e) in §5 is explicitly an **[INFERENCE]**; 1.8-vs-1.9 qualitative PvP "feel" debate is community opinion and is excluded from FACT claims.
- **Research-only:** no source files edited, no build run, no in-game verification. Findings rely on cited public sources and sibling lanes' javap work.

---

## 12. Sources

Protocol / attack packets / server validation (T4):
- **S1** https://minecraft.wiki/w/Java_Edition_protocol/Packets — Attack 0x01 (Entity ID VarInt, LOS + 4u), Interact 0x1A, Player Action 0x29, swing/hurt animations, targeted teleport reconciliation.
- **S2** https://mappings.dev/1.20.1/net/minecraft/server/network/ServerGamePacketListenerImpl.html — `handleInteract` → `ServerPlayer.attack`.
- **S3** https://mappings.dev/1.20.1/net/minecraft/server/level/ServerPlayer.html — `attack(Entity)` entry point.
- **S4** https://github.com/Wind-Development/WindSpigot — decompiled 1.8 `PlayerConnection.java` (`C0A`, `== ATTACK`, invalid-entity disconnect, `maxReach`).
- **S5** https://github.com/Patbox/polymer (dev/26.2 `FallbackServerPacketHandler.java`) — 26.x dedicated `ServerboundAttackPacket` / `handleAttack`.
- **S6** https://github.com/MeteorDevelopment/meteor-client `DamageUtils.java` — `0.2+charge²·0.8` damage formula.
- **S7** https://github.com/CCBlueX/LiquidBounce `CombatExtensions.kt` — cooldown/damage scaling corroboration.
- **S8** https://minecraft.wiki/w/Java_Edition_guides/Combat_Update — 1.9 cooldown vs 1.8 none; attack speeds.

26.3 facts / cooldown / reach / Fabric 26.3 symbols (T5):
- **S9** https://minecraft.wiki/w/Java_Edition_26.3 — SDL3, protocol 777, `attack_animation`/`interact_animation` replacing `swing_animation`.
- **S10** https://minecraft.wiki/w/Damage §s8 — cooldown formula, `getAttackStrengthScale`, crit ≈ 84.8%.
- **S11** https://minecraft.wiki/w/Reach §s2 — `entity_interaction_range`=3 (creative 5), `block_interaction_range`=4.5.
- **S12** https://raw.githubusercontent.com/FabricMC/fabric-api/26.3/fabric-events-interaction-v0/src/client/java/net/fabricmc/fabric/mixin/event/interaction/client/MinecraftMixin.java — `ClientPreAttackCallback`, `startAttack`/`continueAttack`, `SwingSource.PREDICTED`, exact 26.3 vanilla signatures.
- **S13** https://raw.githubusercontent.com/FabricMC/fabric-api/26.3/fabric-events-interaction-v0/src/main/java/net/fabricmc/fabric/api/event/player/UseEntityCallback.java — Fabric interaction callback surface.
- **S14** https://raw.githubusercontent.com/MeteorDevelopment/meteor-client/master/src/main/java/meteordevelopment/meteorclient/mixin/MultiPlayerGameModeMixin.java — `attack`/`interact`/`dropItem`/`useItemOn` injection points.
- **S15** https://raw.githubusercontent.com/MeteorDevelopment/meteor-client/master/src/main/java/meteordevelopment/meteorclient/systems/modules/combat/Criticals.java — `ServerboundAttackPacket` / `ServerboundPunchPacket`.
- **S16** https://raw.githubusercontent.com/MoriyaShiine/enchancement/main/common/src/main/java/moriyashiine/enchancement/mixin/config/coyotebite/MinecraftMixin.java — `startAttack` @HEAD, `gameMode.attack` + `player.swing(...getAttackAnimation(),false)` + `ServerboundPunchPacket.INSTANCE`.
- **S17** https://github.com/PaperMC/Paper/blob/main/paper-server/patches/sources/net/minecraft/server/network/ServerGamePacketListenerImpl.java.patch — `handleAttack` resolves `getEntityOrPart(packet.entityId())` (server authority).
- **S18** https://github.com/denizenscript/Denizen — v26_3 `handlePunch` corroboration.
- **S19** https://github.com/gnembon/fabric-carpet — `ServerboundPunchPacket` corroboration.

Existing solutions / 26.3 changelog (T6):
- **S20** https://modrinth.com/mod/viafabricplus — ViaFabricPlus support table (26.3 feature + bugfix), purpose, multiplayer-only disclaimer.
- **S21** https://github.com/ViaVersion/ViaFabricPlus — version-gated 1.7/1.8 PvP mixins, `PositionInterpolator1_8`, ViaLegacy.
- **S22** https://www.curseforge.com/minecraft/mc-mods/viafabricplus — ViaFabricPlus distribution / version support.
- **S23** https://modrinth.com/mod/vidalia — Animatium→Vidalia rename, feature list (Pick Inflation, Old Sneak Height, Client-Sided Entity Movement, Left Click Item Usage), 26.3 support.
- **S24** https://www.curseforge.com/minecraft/mc-mods/vidalia — Vidalia distribution corroboration.
- **S25** https://github.com/KaelusAI/GroundedGrim — predictive movement/anticheat (not a PvP tweak).
- **S26** https://www.minecraft.net/en-us/article/minecraft-java-edition-26-3 — official "Wilderness Bound" changelog: no combat/hit-registration change, SDL3 input.
- **S27** "Minecraft 26.3 unobfuscated Mojang names / Yarn retired" (Google search) plus https://mappings.dev/ — Mojang de-obfuscation ~2025-10-29, Yarn retired, LoomNoRemap.

In-repo / board evidence (not URLs):
- `README.md:3-8` (two features; recommends Animatium + ViaFabricPlus); `nodebuffutils.mixins.json` (`defaultRequire:1`); migration commit `94e26ef` (26.2→26.3, SDL3 smoke-test scope); T2 javap finding **msg-44** (inventory desync fixes were no-ops / unsafe on 26.3 and were removed).
