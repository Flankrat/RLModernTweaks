package com.flankrat.rlmoderntweaks.feature;

import com.flankrat.rlmoderntweaks.config.RLMTConfig;
import com.flankrat.rlmoderntweaks.util.ItemMatcher;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Hitting something with a torch or blaze rod sets it on fire. Port of torch_damage.js,
 * which never worked: it listened with EntityEvents.hurt('target', ...), which only
 * matches an entity type called minecraft:target, and there isn't one.
 */
public final class FireItems {

    private static volatile ItemMatcher items = ItemMatcher.EMPTY;

    private FireItems() {
    }

    public static void refresh() {
        items = ItemMatcher.of(RLMTConfig.FIRE_ITEMS.get());
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (!RLMTConfig.FIRE_ITEMS_ENABLED.get()) return;

        DamageSource source = event.getSource();
        // Direct melee only, so an arrow shot while holding a torch doesn't count.
        if (!(source.getEntity() instanceof LivingEntity attacker) || source.getDirectEntity() != attacker) return;
        if (!items.test(attacker.getMainHandItem())) return;

        LivingEntity target = event.getEntity();
        if (!target.fireImmune()) {
            target.setSecondsOnFire(RLMTConfig.FIRE_ITEMS_SECONDS.get());
        }
    }
}
