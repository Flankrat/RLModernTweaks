package com.flankrat.rlmoderntweaks.mixin;

import com.flankrat.rlmoderntweaks.feature.OversizedStructures;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.storage.ChunkSerializer;
import net.minecraft.world.level.levelgen.structure.Structure;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;

/**
 * When a chunk is loaded, vanilla throws away structure references to starts more than 8 chunks
 * away ("Found invalid structure reference"). For oversized structures those references are
 * real, so put them back (up to the configured radius). Without this, a chunk that was saved
 * halfway through world generation and loaded again never got its part of the structure.
 */
@Mixin(ChunkSerializer.class)
public abstract class ChunkSerializerMixin {

    @Inject(method = "unpackStructureReferences", at = @At("RETURN"), require = 1)
    private static void rlmoderntweaks$keepFarReferences(RegistryAccess registryAccess, ChunkPos chunkPos, CompoundTag tag,
                                                         CallbackInfoReturnable<Map<Structure, LongSet>> cir) {
        int radius = OversizedStructures.activeRadius();
        if (radius <= 8) return;
        Map<Structure, LongSet> references = cir.getReturnValue();
        Registry<Structure> registry = registryAccess.registryOrThrow(Registries.STRUCTURE);

        for (String key : tag.getAllKeys()) {
            ResourceLocation id = ResourceLocation.tryParse(key);
            if (id == null || !OversizedStructures.isListed(id)) continue;
            Structure structure = registry.get(id);
            if (structure == null) continue;
            for (long reference : tag.getLongArray(key)) {
                if (new ChunkPos(reference).getChessboardDistance(chunkPos) <= radius) {
                    references.computeIfAbsent(structure, s -> new LongOpenHashSet()).add(reference);
                }
            }
        }
    }
}
