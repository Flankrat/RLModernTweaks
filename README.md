# RLModernTweaks

Gameplay tweaks for the RLCraft 1.20.1 port, moved out of KubeJS scripts into a small Forge mod.
Java is compiled (mistakes show up at build time, not in game) and much faster on events that fire
constantly, like player ticks and damage.

Forge 1.20.1 (47.x). Every tweak can be turned off or tuned in `config/rlmoderntweaks-common.toml`.

## Phase 1

| Tweak | Replaces | Notes |
|---|---|---|
| Player gravity (`forge:entity_gravity` = 0.084) | `/attribute` commands in `Global_Rules.js` | Set on world join and respawn, no commands. |
| Magic Focus bonus for golden weapons | `golden_sword_magic_and_spartan_osmosis.js` (hurt part) | +50% damage per level on direct melee hits. |
| Golden Osmosis for extra golden items | `golden_sword_magic_and_spartan_osmosis.js` (tick part) | Mixin into Talents' `isGolden`, so Talents repairs them at its normal rate. The script repaired them 3x faster than vanilla gold. |
| Hiding in hollow logs | `hidey_hollow_holes.js` | Mobs that can't see you drop aggro and can't hit you. |
| Banned enchantments | `enchantment_removal.js` | Strips Grappling Hook's double jump from inventory items. |
| Torch / blaze rod sets targets on fire | `torch_damage.js` | The script never worked (it listened for an entity type that doesn't exist). |
| Bark from stripping logs, bark burn time | `log_stripping.js`, `FuranceBurnTime_script.js` | No longer damages the axe twice. |

Item lists in the config accept `modid:item`, `modid:prefix*` and `#modid:tag`.

## Building

```
gradlew build
```

The mod jar is in `build/libs` (the one without `-sources`).

## Later phases

- Custom items: Grave Scroll, Blade of Herobrine, Wildberry Wine
- Cataclysm compatibility: Tidal Claws armor requirement, Curios slot attributes
- Shivaxi boss

## License and permissions

Copyright (c) 2026 Flankrat. All Rights Reserved; see `LICENSE`.

The RLCraft development teams (including RLCraft, RLCraft Dregora and the RLCraft 1.20.1 port)
may use, modify and build on this mod as they see fit for their own packs, since each pack has
its own design goals, and may distribute the original or their modified versions as part of
those packs. Anyone else needs permission from Flankrat.
