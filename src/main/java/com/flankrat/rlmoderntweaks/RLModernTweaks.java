package com.flankrat.rlmoderntweaks;

import com.flankrat.rlmoderntweaks.config.RLMTConfig;
import com.flankrat.rlmoderntweaks.feature.BannedEnchantments;
import com.flankrat.rlmoderntweaks.feature.BarkTweaks;
import com.flankrat.rlmoderntweaks.feature.FireItems;
import com.flankrat.rlmoderntweaks.feature.GoldenItems;
import com.flankrat.rlmoderntweaks.feature.HollowLogHiding;
import com.flankrat.rlmoderntweaks.feature.MagicFocusGoldenWeapons;
import com.flankrat.rlmoderntweaks.feature.PlayerGravity;
import com.mojang.logging.LogUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(RLModernTweaks.MODID)
public final class RLModernTweaks {

    public static final String MODID = "rlmoderntweaks";
    public static final Logger LOGGER = LogUtils.getLogger();

    public RLModernTweaks() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, RLMTConfig.SPEC);

        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        modBus.addListener(this::onConfigLoad);
        modBus.addListener(this::onConfigReload);

        IEventBus forgeBus = MinecraftForge.EVENT_BUS;
        forgeBus.register(PlayerGravity.class);
        forgeBus.register(MagicFocusGoldenWeapons.class);
        forgeBus.register(HollowLogHiding.class);
        forgeBus.register(BannedEnchantments.class);
        forgeBus.register(FireItems.class);
        forgeBus.register(BarkTweaks.class);
    }

    private void onConfigLoad(ModConfigEvent.Loading event) {
        if (event.getConfig().getSpec() == RLMTConfig.SPEC) refreshConfigCaches();
    }

    private void onConfigReload(ModConfigEvent.Reloading event) {
        if (event.getConfig().getSpec() == RLMTConfig.SPEC) refreshConfigCaches();
    }

    /** Rebuilds the parsed item/enchantment lists after the config file is loaded or edited. */
    private static void refreshConfigCaches() {
        MagicFocusGoldenWeapons.refresh();
        GoldenItems.refresh();
        BannedEnchantments.refresh();
        FireItems.refresh();
        BarkTweaks.refresh();
    }
}
