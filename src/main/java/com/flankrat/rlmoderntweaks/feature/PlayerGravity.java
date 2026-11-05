package com.flankrat.rlmoderntweaks.feature;

import com.flankrat.rlmoderntweaks.config.RLMTConfig;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Gives players 1.12-style gravity by setting the base value of forge:entity_gravity.
 * Replaces the /attribute commands Global_Rules.js ran on first join and on every respawn.
 * The base value syncs to the client, so movement prediction matches.
 */
public final class PlayerGravity {

    private PlayerGravity() {
    }

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof Player player)) return;
        if (!RLMTConfig.GRAVITY_ENABLED.get()) return;

        AttributeInstance gravity = player.getAttribute(ForgeMod.ENTITY_GRAVITY.get());
        if (gravity == null) return;

        double value = RLMTConfig.GRAVITY_VALUE.get();
        if (gravity.getBaseValue() != value) {
            gravity.setBaseValue(value);
        }
    }
}
