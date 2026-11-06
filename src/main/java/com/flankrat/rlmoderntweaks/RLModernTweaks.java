package com.flankrat.rlmoderntweaks;

import com.flankrat.rlmoderntweaks.config.RLMTConfig;
import com.flankrat.rlmoderntweaks.feature.BannedEnchantments;
import com.flankrat.rlmoderntweaks.feature.BarkTweaks;
import com.flankrat.rlmoderntweaks.feature.FireItems;
import com.flankrat.rlmoderntweaks.feature.FlatGroundCheck;
import com.flankrat.rlmoderntweaks.feature.GoldenItems;
import com.flankrat.rlmoderntweaks.feature.HollowLogHiding;
import com.flankrat.rlmoderntweaks.feature.MagicFocusGoldenWeapons;
import com.flankrat.rlmoderntweaks.feature.NoWaterlogging;
import com.flankrat.rlmoderntweaks.feature.OversizedStructures;
import com.flankrat.rlmoderntweaks.feature.PlayerGravity;
import com.flankrat.rlmoderntweaks.feature.WaterOnlyCheck;
import com.flankrat.rlmoderntweaks.feature.PauseMenuCleanup;
import com.flankrat.rlmoderntweaks.feature.PlayerBosses;
import com.flankrat.rlmoderntweaks.feature.ShivaxiBoss;
import com.flankrat.rlmoderntweaks.feature.WorldRules;
import com.flankrat.rlmoderntweaks.feature.cataclysm.NoBlockBreaking;
import com.flankrat.rlmoderntweaks.feature.cataclysm.TidalClaws;
import com.flankrat.rlmoderntweaks.feature.cataclysm.TidalClawsClient;
import com.flankrat.rlmoderntweaks.feature.items.BladeOfHerobrineItem;
import com.flankrat.rlmoderntweaks.feature.items.GraveScrollItem;
import net.minecraftforge.fml.ModList;
import com.flankrat.rlmoderntweaks.feature.socketing.Socketing;
import com.flankrat.rlmoderntweaks.feature.socketing.SocketingClient;
import com.flankrat.rlmoderntweaks.init.ModItems;
import com.flankrat.rlmoderntweaks.util.FarReferenceLogFilter;
import com.flankrat.rlmoderntweaks.network.ModNetwork;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.loading.FMLEnvironment;
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
        modBus.addListener(this::onCommonSetup);
        ModItems.register(modBus);

        IEventBus forgeBus = MinecraftForge.EVENT_BUS;
        forgeBus.register(PlayerGravity.class);
        forgeBus.register(MagicFocusGoldenWeapons.class);
        forgeBus.register(HollowLogHiding.class);
        forgeBus.register(BannedEnchantments.class);
        forgeBus.register(FireItems.class);
        forgeBus.register(BarkTweaks.class);
        forgeBus.register(Socketing.class);
        forgeBus.register(OversizedStructures.class);
        forgeBus.register(GraveScrollItem.class);
        forgeBus.register(BladeOfHerobrineItem.class);
        forgeBus.register(ShivaxiBoss.class);
        forgeBus.register(PlayerBosses.class);
        forgeBus.register(TidalClaws.class);
        forgeBus.register(NoBlockBreaking.class);
        forgeBus.register(WorldRules.class);
        if (ModList.get().isLoaded("curios")) {
            // Loaded by name so the Curios classes are only touched when Curios is installed.
            try {
                forgeBus.register(Class.forName("com.flankrat.rlmoderntweaks.feature.cataclysm.CataclysmCurios"));
            } catch (ClassNotFoundException e) {
                LOGGER.error("Couldn't load the Cataclysm Curios compat", e);
            }
        }
        FarReferenceLogFilter.install();
        if (FMLEnvironment.dist == Dist.CLIENT) {
            forgeBus.register(SocketingClient.class);
            forgeBus.register(TidalClawsClient.class);
            forgeBus.register(PauseMenuCleanup.class);
        }
    }

    private void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(ModNetwork::register);
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
        FlatGroundCheck.refresh();
        NoWaterlogging.refresh();
        WaterOnlyCheck.refresh();
        OversizedStructures.refresh();
        Socketing.refresh();
    }
}
