package com.flankrat.rlmoderntweaks.mixin;

import com.flankrat.rlmoderntweaks.feature.OversizedStructures;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.StructureAccess;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Consumer;

/**
 * During world generation, a chunk can only load chunks up to 8 away. For oversized structures
 * whose start chunk is further, take the start from {@link OversizedStructures}' cache instead
 * of loading the start chunk (which would crash). Everything else uses the vanilla code.
 */
@Mixin(StructureManager.class)
public abstract class StructureManagerMixin {

    @Shadow
    @Final
    private LevelAccessor level;

    @Shadow
    public abstract StructureStart getStartForStructure(SectionPos sectionPos, Structure structure, StructureAccess access);

    @Inject(method = "fillStartsForStructure", at = @At("HEAD"), cancellable = true, require = 1)
    private void rlmoderntweaks$farStarts(Structure structure, LongSet references, Consumer<StructureStart> consumer,
                                         CallbackInfo ci) {
        if (!(this.level instanceof WorldGenRegion region)) return;
        if (!OversizedStructures.applies(structure, region.registryAccess())) return;
        ci.cancel();

        LongIterator iterator = references.iterator();
        while (iterator.hasNext()) {
            ChunkPos chunkPos = new ChunkPos(iterator.nextLong());
            StructureStart start;
            if (region.hasChunk(chunkPos.x, chunkPos.z)) {
                start = this.getStartForStructure(SectionPos.of(chunkPos, region.getMinSection()), structure,
                        region.getChunk(chunkPos.x, chunkPos.z, ChunkStatus.STRUCTURE_STARTS));
            } else {
                start = OversizedStructures.getStart(region.getLevel(), structure, chunkPos);
            }
            if (start != null && start.isValid()) consumer.accept(start);
        }
    }
}
