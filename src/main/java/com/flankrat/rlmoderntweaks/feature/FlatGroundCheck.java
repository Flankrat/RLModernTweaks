package com.flankrat.rlmoderntweaks.feature;

import com.flankrat.rlmoderntweaks.config.RLMTConfig;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;

import java.util.HashSet;
import java.util.Set;

/**
 * Stops listed structures from spawning on sloped ground. Before one of them is placed, the
 * ground height is sampled at the corners, edge midpoints and center of its footprint; if the
 * highest and lowest samples differ by more than the configured amount, that spot is skipped.
 * <p>
 * Used by {@code mixin.StructureMixin} on {@link Structure#findValidGenerationPoint}, which is
 * also what /locate uses, so /locate never points at a spot this check would reject.
 */
public final class FlatGroundCheck {

    private static volatile Set<ResourceLocation> structures = Set.of();

    private FlatGroundCheck() {
    }

    public static void refresh() {
        Set<ResourceLocation> ids = new HashSet<>();
        for (String entry : RLMTConfig.FLAT_GROUND_STRUCTURES.get()) {
            ResourceLocation id = ResourceLocation.tryParse(entry.trim());
            if (id != null) ids.add(id);
        }
        structures = Set.copyOf(ids);
    }

    /** True if the structure is covered by the check (so the caller should test the footprint). */
    public static boolean applies(Structure structure, Structure.GenerationContext context) {
        if (!RLMTConfig.FLAT_GROUND_ENABLED.get()) return false;
        Set<ResourceLocation> current = structures;
        if (current.isEmpty()) return false;
        ResourceLocation id = context.registryAccess().registryOrThrow(Registries.STRUCTURE).getKey(structure);
        return id != null && current.contains(id);
    }

    /** True if the footprint of the given pieces is flat enough. */
    public static boolean isFlatEnough(StructurePiecesBuilder pieces, Structure.GenerationContext context) {
        if (pieces.isEmpty()) return true;
        BoundingBox box = pieces.getBoundingBox();

        ChunkGenerator generator = context.chunkGenerator();
        LevelHeightAccessor heightAccessor = context.heightAccessor();
        RandomState randomState = context.randomState();

        int[] xs = {box.minX(), (box.minX() + box.maxX()) / 2, box.maxX()};
        int[] zs = {box.minZ(), (box.minZ() + box.maxZ()) / 2, box.maxZ()};

        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        for (int x : xs) {
            for (int z : zs) {
                int y = generator.getBaseHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, heightAccessor, randomState);
                min = Math.min(min, y);
                max = Math.max(max, y);
            }
        }
        return max - min <= RLMTConfig.FLAT_GROUND_MAX_DIFFERENCE.get();
    }
}
