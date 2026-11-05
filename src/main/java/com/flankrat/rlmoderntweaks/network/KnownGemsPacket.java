package com.flankrat.rlmoderntweaks.network;

import com.flankrat.rlmoderntweaks.feature.socketing.SocketingClientState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** Server to client: the full list of gems the player has discovered (for gem tooltips). */
public record KnownGemsPacket(List<String> gems) {

    public static void encode(KnownGemsPacket packet, FriendlyByteBuf buf) {
        buf.writeVarInt(packet.gems.size());
        for (String gem : packet.gems) buf.writeUtf(gem);
    }

    public static KnownGemsPacket decode(FriendlyByteBuf buf) {
        int size = buf.readVarInt();
        List<String> gems = new ArrayList<>(size);
        for (int i = 0; i < size; i++) gems.add(buf.readUtf());
        return new KnownGemsPacket(gems);
    }

    public static void handle(KnownGemsPacket packet, Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> SocketingClientState.setKnown(packet.gems));
        context.get().setPacketHandled(true);
    }
}
