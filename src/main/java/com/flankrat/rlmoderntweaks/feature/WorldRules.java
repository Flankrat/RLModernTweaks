package com.flankrat.rlmoderntweaks.feature;

import com.flankrat.rlmoderntweaks.RLModernTweaks;
import com.flankrat.rlmoderntweaks.config.RLMTConfig;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Sets the pack's gamerules the first time a world is loaded, and the difficulty every time.
 * Port of Global_Rules.js and set_difficulty.js.
 */
public final class WorldRules {
    private WorldRules() {
    }

    /** Remembers that the first-load gamerules were set for this world. */
    private static final class Flag extends SavedData {
        boolean done;

        static Flag load(CompoundTag tag) {
            Flag flag = new Flag();
            flag.done = tag.getBoolean("firstLoadDone");
            return flag;
        }

        @Override
        public CompoundTag save(CompoundTag tag) {
            tag.putBoolean("firstLoadDone", done);
            return tag;
        }
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        MinecraftServer server = event.getServer();
        var source = server.createCommandSourceStack().withSuppressedOutput();

        Flag flag = server.overworld().getDataStorage().computeIfAbsent(Flag::load, Flag::new, "rlmoderntweaks_world_rules");
        if (!flag.done) {
            for (String rule : RLMTConfig.FIRST_LOAD_GAMERULES.get()) {
                String[] parts = rule.split("=", 2);
                if (parts.length != 2) continue;
                server.getCommands().performPrefixedCommand(source, "gamerule " + parts[0].trim() + " " + parts[1].trim());
            }
            flag.done = true;
            flag.setDirty();
            RLModernTweaks.LOGGER.info("Set the first-load gamerules for this world");
        }

        String name = RLMTConfig.FORCE_DIFFICULTY.get().trim();
        if (!name.isEmpty()) {
            Difficulty difficulty = Difficulty.byName(name.toLowerCase());
            if (difficulty != null) server.setDifficulty(difficulty, true);
            else RLModernTweaks.LOGGER.warn("Unknown difficulty \"{}\" in the world_rules config", name);
        }
    }
}
