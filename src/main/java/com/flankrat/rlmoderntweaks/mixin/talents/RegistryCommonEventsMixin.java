package com.flankrat.rlmoderntweaks.mixin.talents;

import com.flankrat.rlmoderntweaks.feature.GoldenItems;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Talents only counts vanilla gold tools and armor for Golden Osmosis (isGolden checks
 * Tiers.GOLD / ArmorMaterials.GOLD). This adds the items from the golden_osmosis config list.
 * isGolden is only used by Golden Osmosis.
 * <p>
 * {@code @Pseudo} so the mod loads fine without Talents installed.
 */
@Pseudo
@Mixin(targets = "com.seniors.talents.registry.RegistryCommonEvents", remap = false)
public abstract class RegistryCommonEventsMixin {

    @Inject(method = "isGolden", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void rlmoderntweaks$countExtraGoldenItems(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (GoldenItems.isExtraGolden(stack)) {
            cir.setReturnValue(true);
        }
    }
}
