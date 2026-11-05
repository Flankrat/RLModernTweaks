package com.flankrat.rlmoderntweaks.init;

import com.flankrat.rlmoderntweaks.RLModernTweaks;
import com.flankrat.rlmoderntweaks.feature.socketing.SocketLootModifier;
import com.mojang.serialization.Codec;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** Items and loot modifier serializers. */
public final class ModItems {

    private static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, RLModernTweaks.MODID);
    private static final DeferredRegister<Codec<? extends IGlobalLootModifier>> LOOT_MODIFIERS =
            DeferredRegister.create(ForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, RLModernTweaks.MODID);

    public static final RegistryObject<Item> SOCKET_EXTRACTOR =
            ITEMS.register("socket_extractor", () -> new Item(new Item.Properties()));

    public static final RegistryObject<Codec<SocketLootModifier>> SOCKET_ROLLS =
            LOOT_MODIFIERS.register("socket_rolls", SocketLootModifier.CODEC);

    private ModItems() {
    }

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
        LOOT_MODIFIERS.register(modBus);
        modBus.addListener(ModItems::addToCreativeTabs);
    }

    private static void addToCreativeTabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) event.accept(SOCKET_EXTRACTOR);
    }
}
