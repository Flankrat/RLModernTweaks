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
| Flat ground check for structures | RLStructure foundations | Plains camps, the windmill and the desert town only spawn where the ground is fairly flat (max 6 blocks difference across the footprint), so they don't get tall dirt or sand walls on slopes. |
| Oversized structures | — | Vanilla only builds a structure within 8 chunks of the chunk it starts in, which cut the outer ships and coastline off Island City. Listed structures (default: Island City) can reach 12 chunks. Mixins into `ChunkGenerator.createReferences` and `StructureManager.fillStartsForStructure`; everything else is untouched. |
| Water-only check for sea structures | — | Island City, the Cult Island and the RLStructure ships only spawn where their whole footprint is open water (25 points, at least 3 blocks deep). Minecraft only checks the biome at a structure's start, so they could end up half on land. |
| No waterlogging for listed structures | RLStructure air fill | The Cult Island, Island City and the RLStructure ships have dry space below sea level. Normally every stair, slab, fence or leaf block placed where ocean water was becomes waterlogged and floods it; listed templates are placed without keeping that water. |
| Torch / blaze rod sets targets on fire | `torch_damage.js` | The script never worked (it listened for an entity type that doesn't exist). |
| Bark from stripping logs, bark burn time | `log_stripping.js`, `FuranceBurnTime_script.js` | No longer damages the axe twice. |
| Gem socketing (stand-in until fonnymunkey's Socketed is ported from 1.12.2) | `socketed_gems_startup.js`, `socketed_logic_server.js`, `socketed_tooltips_client.js` | Same behaviour and item NBT as the scripts, so already socketed gear keeps its gems. Loot rolls use a global loot modifier instead of LootJS. Adds the Socket Extraction Tool (`rlmoderntweaks:socket_extractor`, replaces `kubejs:socket_extractor`). Discovered gems carry over from the old KubeJS stages. Gems, rolls and name prefixes are in the config. |

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
