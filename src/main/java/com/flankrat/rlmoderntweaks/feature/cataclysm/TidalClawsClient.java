package com.flankrat.rlmoderntweaks.feature.cataclysm;

import com.flankrat.rlmoderntweaks.config.RLMTConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Client side of {@link TidalClaws}. Left-clicking the air can't be cancelled in Forge 1.20.1, so
 * both hands are emptied while Cataclysm's listener looks at them and put back right after, which
 * stops Cataclysm from sending its grapple packet. Also adds the armor tooltip.
 */
public final class TidalClawsClient {
    private static Player hiddenPlayer;
    private static ItemStack hiddenMain = ItemStack.EMPTY;
    private static ItemStack hiddenOff = ItemStack.EMPTY;

    private TidalClawsClient() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void hideHands(PlayerInteractEvent.LeftClickEmpty event) {
        Player player = event.getEntity();
        if (!RLMTConfig.TIDAL_CLAWS_ENABLED.get() || TidalClaws.hasArmor(player)) return;
        if (!TidalClaws.isClaws(player.getMainHandItem()) && !TidalClaws.isClaws(player.getOffhandItem())) return;
        hiddenPlayer = player;
        hiddenMain = player.getMainHandItem();
        hiddenOff = player.getOffhandItem();
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void restoreHands(PlayerInteractEvent.LeftClickEmpty event) {
        if (hiddenPlayer == null || hiddenPlayer != event.getEntity()) return;
        hiddenPlayer.setItemInHand(InteractionHand.MAIN_HAND, hiddenMain);
        hiddenPlayer.setItemInHand(InteractionHand.OFF_HAND, hiddenOff);
        hiddenPlayer = null;
        hiddenMain = ItemStack.EMPTY;
        hiddenOff = ItemStack.EMPTY;
    }

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        if (RLMTConfig.TIDAL_CLAWS_ENABLED.get() && TidalClaws.isClaws(event.getItemStack())) {
            event.getToolTip().add(Component.literal("Requires Full Neptunium Armor to Use").withStyle(ChatFormatting.AQUA));
        }
    }
}
