package com.flankrat.rlmoderntweaks.feature.cataclysm;

import com.flankrat.rlmoderntweaks.config.RLMTConfig;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * The disabled Cataclysm weapons can't break blocks.
 * Port of the BlockEvents.broken part of cataclysm_gear_restrictions.js.
 */
public final class NoBlockBreaking {
    private NoBlockBreaking() {
    }

    @SubscribeEvent
    public static void onBreak(BlockEvent.BreakEvent event) {
        Player player = event.getPlayer();
        if (player == null) return;
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(player.getMainHandItem().getItem());
        if (id != null && RLMTConfig.NO_BLOCK_BREAKING_ITEMS.get().contains(id.toString())) event.setCanceled(true);
    }
}
