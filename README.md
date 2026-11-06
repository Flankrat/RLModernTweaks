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
| Oversized structures | — | Vanilla only builds a structure within 8 chunks of the chunk it starts in, which cut the outer ships and coastline off Island City. Listed structures (default: Island City) can reach 12 chunks. Mixins into `ChunkGenerator.createReferences`, `StructureManager.fillStartsForStructure` and `ChunkSerializer.unpackStructureReferences` (vanilla drops references further than 8 chunks when a chunk is loaded); everything else is untouched. |
| Water-only check for sea structures | — | Island City, the Cult Island and the RLStructure ships only spawn where their whole footprint is open water (25 points, at least 3 blocks deep). Minecraft only checks the biome at a structure's start, so they could end up half on land. |
| No waterlogging for listed structures | RLStructure air fill | The Cult Island, Island City and the RLStructure ships have dry space below sea level. Normally every stair, slab, fence or leaf block placed where ocean water was becomes waterlogged and floods it; listed templates are placed without keeping that water. |
| Torch / blaze rod sets targets on fire | `torch_damage.js` | The script never worked (it listened for an entity type that doesn't exist). |
| Bark from stripping logs, bark burn time | `log_stripping.js`, `FuranceBurnTime_script.js` | No longer damages the axe twice. |
| Gem socketing (stand-in until fonnymunkey's Socketed is ported from 1.12.2) | `socketed_gems_startup.js`, `socketed_logic_server.js`, `socketed_tooltips_client.js` | Same behaviour and item NBT as the scripts, so already socketed gear keeps its gems. Loot rolls use a global loot modifier instead of LootJS. Adds the Socket Extraction Tool (`rlmoderntweaks:socket_extractor`, replaces `kubejs:socket_extractor`). Discovered gems carry over from the old KubeJS stages. Gems, rolls and name prefixes are in the config. |

## Phase 2

| Tweak | Replaces | Notes |
|---|---|---|
| Grave Scroll (`rlmoderntweaks:grave_scroll`) | `Gravescroll_function.js`, `Grave_Scroll_event.js`, `Gravescroll_client_sync.js`, the scroll in `Registries.js` | Hold right-click for 3 seconds to go back to where you last died, in any dimension. Used up on use. The death point survives respawning. `kubejs:grave_scroll` items in existing worlds turn into the new item. |
| Blade of Herobrine (`rlmoderntweaks:blade_of_herobrine`) | the three `blade_of_herobrine.js` scripts | Unbreakable gold-tier sword. Kills give souls (up to 10); right-click spends one to send out a wave of soul fire that hurts and burns everything in front of you. `kubejs:blade_of_herobrine` items turn into the new item. |
| Shivaxi boss | `ShivaxiBoss.js`, `ShivaxiBodyMasterKeyDrop.js` | Only the giant in the Shivaxi structure (`rec_migrated:shivaxi`) becomes Shivaxi; the giant in Ice City stays a normal giant. Gear, AI, a red boss bar, then the Shivaxi Dragon as a second phase, and a guaranteed Master Key. |
| Player bosses | `playerbosses.js` | 0.1% of newly spawned zombies become one of the RLCraft player bosses. Only fresh spawns are rolled, not zombies loaded with a chunk. Bosses and rewards are in the config. |
| Tidal Claws need full Neptunium armor | `cataclysm_tidal_claws_neptunium_requirement.js`, the tooltip in `tooltip_Events.js` | No grapple and no melee without the armor; chests and doors still open. |
| Cataclysm curios in the shared Curios slots | `cataclysm_curios_ring_attribute_compat.js` | Ring of Grudged, the belts and the Unbreakable Skull get their bonuses in the ring, belt and charm slots. Only active with Curios installed. |
| Disabled Cataclysm weapons can't break blocks | the block-break part of `cataclysm_gear_restrictions.js` | The list is in the config. |
| Pause menu cleanup | `hide_advancement_button.js` | Hides Advancements and Give Feedback and closes the gaps. |
| First-load gamerules, difficulty | `Global_Rules.js`, `set_difficulty.js` | Gamerules are set once per world; the difficulty (hard) every time the world loads. |

Item lists in the config accept `modid:item`, `modid:prefix*` and `#modid:tag`.

## Building

```
gradlew build
```

The mod jar is in `build/libs` (the one without `-sources`).

`src/stubs` holds compile-only copies of the few Curios API classes the mod uses, so Curios isn't
needed to build. They aren't put in the jar; at runtime the real classes come from Curios.

## License and permissions

Copyright (c) 2026 Flankrat. All Rights Reserved; see `LICENSE`.

The RLCraft development teams (including RLCraft, RLCraft Dregora and the RLCraft 1.20.1 port)
may use, modify and build on this mod as they see fit for their own packs, since each pack has
its own design goals, and may distribute the original or their modified versions as part of
those packs. Anyone else needs permission from Flankrat.
