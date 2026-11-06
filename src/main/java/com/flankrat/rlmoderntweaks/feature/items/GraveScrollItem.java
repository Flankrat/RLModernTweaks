package com.flankrat.rlmoderntweaks.feature.items;

import com.flankrat.rlmoderntweaks.config.RLMTConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Grave Scroll: hold right-click with it for a few seconds to go back to where you last died.
 * It works once per death and is used up. Port of Gravescroll_function.js, Grave_Scroll_event.js
 * and Gravescroll_client_sync.js.
 * <p>
 * The death point is kept in the player's persisted data, so it survives respawning.
 */
public class GraveScrollItem extends Item {
    private static final String KEY = "rlmoderntweaks_grave";

    public GraveScrollItem(Properties properties) {
        super(properties);
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 64;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BOW;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(player.getItemInHand(hand));
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (level.isClientSide || !RLMTConfig.GRAVE_SCROLL_ENABLED.get() || !(entity instanceof ServerPlayer player)) return stack;

        CompoundTag persisted = persisted(player);
        if (!persisted.contains(KEY)) {
            player.displayClientMessage(Component.literal("You have no grave to return to.").withStyle(ChatFormatting.GRAY), true);
            return stack;
        }
        CompoundTag grave = persisted.getCompound(KEY);
        ResourceLocation dimensionId = ResourceLocation.tryParse(grave.getString("dimension"));
        ServerLevel target = dimensionId == null ? null
                : player.server.getLevel(ResourceKey.create(Registries.DIMENSION, dimensionId));
        if (target == null) return stack;

        persisted.remove(KEY);
        player.teleportTo(target, grave.getInt("x") + 0.5, grave.getInt("y"), grave.getInt("z") + 0.5, player.getYRot(), player.getXRot());
        player.playNotifySound(SoundEvents.PORTAL_TRIGGER, SoundSource.PLAYERS, 1.0F, 1.0F);
        if (!player.getAbilities().instabuild) stack.shrink(1);
        return stack;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Hold right-click to return to where you last died.").withStyle(ChatFormatting.GRAY));
    }

    /** Remembers where a player died. */
    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        CompoundTag grave = new CompoundTag();
        grave.putString("dimension", player.level().dimension().location().toString());
        grave.putInt("x", player.getBlockX());
        grave.putInt("y", player.getBlockY());
        grave.putInt("z", player.getBlockZ());
        persisted(player).put(KEY, grave);
    }

    /** Forge copies this compound to the new player on respawn. */
    private static CompoundTag persisted(Player player) {
        CompoundTag data = player.getPersistentData();
        if (!data.contains(Player.PERSISTED_NBT_TAG)) data.put(Player.PERSISTED_NBT_TAG, new CompoundTag());
        return data.getCompound(Player.PERSISTED_NBT_TAG);
    }
}
