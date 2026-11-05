package com.flankrat.rlmoderntweaks.network;

import com.flankrat.rlmoderntweaks.RLModernTweaks;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Optional;

public final class ModNetwork {

    private static final String VERSION = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(RLModernTweaks.MODID, "main"), () -> VERSION, VERSION::equals, VERSION::equals);

    private ModNetwork() {
    }

    public static void register() {
        CHANNEL.registerMessage(0, KnownGemsPacket.class, KnownGemsPacket::encode, KnownGemsPacket::decode,
                KnownGemsPacket::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
    }
}
