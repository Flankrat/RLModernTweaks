package com.flankrat.rlmoderntweaks.mixin;

import com.flankrat.rlmoderntweaks.feature.OversizedStructures;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Adds structure references from further than 8 chunks away for oversized structures. */
@Mixin(ChunkGenerator.class)
public abstract class ChunkGeneratorMixin {

    @Inject(method = "createReferences", at = @At("TAIL"), require = 1)
    private void rlmoderntweaks$farReferences(WorldGenLevel level, StructureManager structureManager,
                                              ChunkAccess chunk, CallbackInfo ci) {
        OversizedStructures.addFarReferences((ChunkGenerator) (Object) this, level, structureManager, chunk);
    }
}
