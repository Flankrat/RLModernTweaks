package com.flankrat.rlmoderntweaks.feature;

import com.flankrat.rlmoderntweaks.config.RLMTConfig;
import net.minecraft.resources.ResourceLocation;

import java.util.HashSet;
import java.util.Set;

/**
 * Structure templates that should not soak up the world's water when placed.
 * Used by {@code mixin.SinglePoolElementMixin}.
 */
public final class NoWaterlogging {

    private static volatile Set<ResourceLocation> templates = Set.of();

    private NoWaterlogging() {
    }

    public static void refresh() {
        Set<ResourceLocation> ids = new HashSet<>();
        for (String entry : RLMTConfig.NO_WATERLOGGING_TEMPLATES.get()) {
            ResourceLocation id = ResourceLocation.tryParse(entry.trim());
            if (id != null) ids.add(id);
        }
        templates = Set.copyOf(ids);
    }

    public static boolean applies(ResourceLocation templateId) {
        if (!RLMTConfig.NO_WATERLOGGING_ENABLED.get()) return false;
        return templates.contains(templateId);
    }
}
