package com.flankrat.rlmoderntweaks.feature.socketing;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;
import java.util.Set;

/**
 * Socketing tooltips (port of socketed_tooltips_client.js). Only registered on the client.
 */
public final class SocketingClient {

    private SocketingClient() {
    }

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        if (!Socketing.enabled()) return;
        ItemStack stack = event.getItemStack();
        List<Component> lines = event.getToolTip();

        // ---- Gems: always "Socketable"; effect hidden until one has been socketed ----
        if (Socketing.isGem(stack)) {
            ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
            lines.add(Component.literal("§7Socketable"));
            if (id != null && SocketingClientState.isKnown(id.toString())) {
                lines.add(Component.literal("§7When socketed: §f" + Socketing.bonusFor(id).label()));
            } else {
                lines.add(Component.literal("§8When socketed: ??? §7(socket one to discover)"));
            }
            return;
        }

        // ---- Gear: show every socket, filled or empty, in the tier's color ----
        CompoundTag nbt = stack.getTag();
        int max = Socketing.maxSockets(nbt);
        if (max <= 0) return;
        int used = nbt.getInt(Socketing.TAG_USED);
        String c = Socketing.tierFor(max).color().toString();

        lines.add(Component.literal(c + "Sockets (" + Math.min(used, max) + "/" + max + "):"));
        for (int i = 1; i <= max; i++) {
            if (i <= used) {
                String label = nbt.getString(Socketing.TAG_LABEL + i);
                if (label.isEmpty()) label = "Active Buff";
                lines.add(Component.literal("  " + c + "[◆] §f" + label));
            } else {
                lines.add(Component.literal("  " + c + "[◇] §8Empty"));
            }
        }
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        SocketingClientState.setKnown(Set.of());
    }
}
