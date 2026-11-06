package com.flankrat.rlmoderntweaks.feature.cataclysm;

import com.flankrat.rlmoderntweaks.config.RLMTConfig;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;

/**
 * Tidal Claws only work with the full Neptunium armor: no grapple (right-click) and no melee
 * (and so no on-hit tentacle). Port of cataclysm_tidal_claws_neptunium_requirement.js.
 * The left-click-in-the-air grapple is blocked on the client in {@link TidalClawsClient}.
 */
public final class TidalClaws {
    private static final EquipmentSlot[] SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    private TidalClaws() {
    }

    static boolean isClaws(ItemStack stack) {
        if (stack.isEmpty()) return false;
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        return id != null && id.toString().equals(RLMTConfig.TIDAL_CLAWS_ITEM.get());
    }

    static boolean hasArmor(Player player) {
        List<? extends String> armor = RLMTConfig.TIDAL_CLAWS_ARMOR.get();
        for (int i = 0; i < SLOTS.length && i < armor.size(); i++) {
            ResourceLocation id = ForgeRegistries.ITEMS.getKey(player.getItemBySlot(SLOTS[i]).getItem());
            if (id == null || !id.toString().equals(armor.get(i))) return false;
        }
        return true;
    }

    static boolean isLocked(Player player, ItemStack stack) {
        return RLMTConfig.TIDAL_CLAWS_ENABLED.get() && isClaws(stack) && !hasArmor(player);
    }

    /** Right-click grapple. Cancelling RightClickItem still lets you open chests, doors and so on. */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRightClick(PlayerInteractEvent.RightClickItem event) {
        if (isLocked(event.getEntity(), event.getItemStack())) event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onAttack(AttackEntityEvent event) {
        Player player = event.getEntity();
        if (isLocked(player, player.getMainHandItem())) event.setCanceled(true);
    }
}
