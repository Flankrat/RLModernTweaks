package com.flankrat.rlmoderntweaks.feature;

import com.flankrat.rlmoderntweaks.config.RLMTConfig;
import com.flankrat.rlmoderntweaks.util.ItemMatcher;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * While the attacker has Magic Focus, each effect level adds a share of extra damage to
 * direct melee hits with golden weapons. Port of the LivingHurtEvent half of
 * golden_sword_magic_and_spartan_osmosis.js.
 */
public final class MagicFocusGoldenWeapons {

    private static volatile ItemMatcher weapons = ItemMatcher.EMPTY;
    // Looked up on first use (the config can load before the effect is registered).
    private static volatile MobEffect effect;
    private static volatile boolean effectResolved;

    private MagicFocusGoldenWeapons() {
    }

    public static void refresh() {
        weapons = ItemMatcher.of(RLMTConfig.MAGIC_FOCUS_WEAPONS.get());
        effect = null;
        effectResolved = false;
    }

    private static MobEffect effect() {
        if (!effectResolved) {
            ResourceLocation id = ResourceLocation.tryParse(RLMTConfig.MAGIC_FOCUS_EFFECT.get());
            effect = id == null ? null : ForgeRegistries.MOB_EFFECTS.getValue(id);
            effectResolved = true;
        }
        return effect;
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (!RLMTConfig.MAGIC_FOCUS_ENABLED.get() || event.getAmount() <= 0) return;

        DamageSource source = event.getSource();
        // Only direct melee hits by a player (not projectiles, not thorns).
        if (!(source.getEntity() instanceof Player attacker) || source.getDirectEntity() != attacker) return;
        if (!weapons.test(attacker.getMainHandItem())) return;

        MobEffect focus = effect();
        if (focus == null) return;
        MobEffectInstance instance = attacker.getEffect(focus);
        if (instance == null) return;

        int level = instance.getAmplifier() + 1;
        double bonus = RLMTConfig.MAGIC_FOCUS_BONUS_PER_LEVEL.get() * level;
        event.setAmount((float) (event.getAmount() * (1.0 + bonus)));
    }
}
