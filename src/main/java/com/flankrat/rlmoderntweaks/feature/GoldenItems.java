package com.flankrat.rlmoderntweaks.feature;

import com.flankrat.rlmoderntweaks.config.RLMTConfig;
import com.flankrat.rlmoderntweaks.util.ItemMatcher;
import net.minecraft.world.item.ItemStack;

/**
 * Extra items that Talents' Golden Osmosis skill treats as golden. Used by
 * {@code mixin.talents.RegistryCommonEventsMixin}, so Talents repairs them with its own rules
 * (every 10 ticks, same durability and XP cost as vanilla gold). Replaces the PlayerTickEvent
 * half of golden_sword_magic_and_spartan_osmosis.js.
 */
public final class GoldenItems {

    private static volatile ItemMatcher extra = ItemMatcher.EMPTY;

    private GoldenItems() {
    }

    public static void refresh() {
        extra = ItemMatcher.of(RLMTConfig.GOLDEN_OSMOSIS_EXTRA_ITEMS.get());
    }

    public static boolean isExtraGolden(ItemStack stack) {
        return RLMTConfig.GOLDEN_OSMOSIS_ENABLED.get() && extra.test(stack);
    }
}
