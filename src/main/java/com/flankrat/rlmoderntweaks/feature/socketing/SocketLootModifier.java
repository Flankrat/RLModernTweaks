package com.flankrat.rlmoderntweaks.feature.socketing;

import com.google.common.base.Suppliers;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSet;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.common.loot.LootModifier;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

/**
 * Gives damageable loot from chests, mob drops and fishing a chance to roll sockets, with a name
 * prefix in the socket tier's color. Replaces the LootJS part of socketed_logic_server.js
 * (which used LootType.CHEST, ENTITY and FISHING, i.e. the loot table's parameter set).
 */
public class SocketLootModifier extends LootModifier {

    public static final Supplier<Codec<SocketLootModifier>> CODEC = Suppliers.memoize(() ->
            RecordCodecBuilder.create(inst -> codecStart(inst).apply(inst, SocketLootModifier::new)));

    public SocketLootModifier(LootItemCondition[] conditions) {
        super(conditions);
    }

    @Override
    protected @NotNull ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> loot, LootContext context) {
        if (!Socketing.enabled() || !isSocketableLoot(context)) return loot;
        for (ItemStack stack : loot) {
            if (stack.isDamageableItem()) rollSockets(stack);
        }
        return loot;
    }

    private static boolean isSocketableLoot(LootContext context) {
        ResourceLocation id = context.getQueriedLootTableId();
        LootTable table = context.getLevel().getServer().getLootData().getLootTable(id);
        LootContextParamSet params = table.getParamSet();
        return params == LootContextParamSets.CHEST
                || params == LootContextParamSets.ENTITY
                || params == LootContextParamSets.FISHING;
    }

    private static void rollSockets(ItemStack stack) {
        if (stack.getTag() != null && stack.getTag().contains(Socketing.TAG_SOCKETS)) return; // already rolled
        int count = Socketing.rollSocketCount();
        if (count <= 0) return;
        stack.getOrCreateTag().putInt(Socketing.TAG_SOCKETS, count);

        // Name prefix from this tier's list, in the tier's color (keeps names set by the loot table).
        if (!stack.hasCustomHoverName()) {
            Socketing.Tier tier = Socketing.tierFor(count);
            String prefix = tier.names().get(ThreadLocalRandom.current().nextInt(tier.names().size()));
            MutableComponent name = Component.literal(prefix + " ")
                    .append(Component.translatable(stack.getDescriptionId()))
                    .withStyle(style -> style.withColor(tier.color()).withItalic(false));
            stack.setHoverName(name);
        }
    }

    @Override
    public Codec<? extends IGlobalLootModifier> codec() {
        return CODEC.get();
    }
}
