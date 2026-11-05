package com.flankrat.rlmoderntweaks.feature;

import com.flankrat.rlmoderntweaks.config.RLMTConfig;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Strips banned enchantments (by default Grappling Hook's double jump) from items in a
 * player's inventory. Port of enchantment_removal.js, which checked both hands every
 * 5 ticks plus every inventory change; this checks the whole inventory on an interval,
 * which is cheap in Java and also catches armor.
 */
public final class BannedEnchantments {

    private static volatile Set<ResourceLocation> banned = Set.of();

    private BannedEnchantments() {
    }

    public static void refresh() {
        Set<ResourceLocation> ids = new HashSet<>();
        for (String entry : RLMTConfig.BANNED_ENCHANTMENTS.get()) {
            ResourceLocation id = ResourceLocation.tryParse(entry.trim());
            if (id != null) ids.add(id);
        }
        banned = Set.copyOf(ids);
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.side.isClient()) return;
        Set<ResourceLocation> current = banned;
        if (current.isEmpty()) return;

        Player player = event.player;
        if (player.tickCount % RLMTConfig.BANNED_ENCHANTMENTS_INTERVAL.get() != 0) return;

        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            strip(inventory.getItem(slot), current);
        }
    }

    private static void strip(ItemStack stack, Set<ResourceLocation> current) {
        // Books are left alone, like the original script (they're hidden from JEI instead).
        if (stack.isEmpty() || !stack.isEnchanted() || stack.is(Items.ENCHANTED_BOOK)) return;

        Map<Enchantment, Integer> enchantments = EnchantmentHelper.getEnchantments(stack);
        boolean changed = enchantments.keySet().removeIf(enchantment -> {
            ResourceLocation id = ForgeRegistries.ENCHANTMENTS.getKey(enchantment);
            return id != null && current.contains(id);
        });
        if (changed) {
            EnchantmentHelper.setEnchantments(enchantments, stack);
        }
    }
}
