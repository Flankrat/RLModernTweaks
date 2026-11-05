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
 * Only lets listed structures spawn where their whole footprint is open water. Minecraft only
 * checks the biome at a structure's start point, so big sea structures can still end up with
 * an edge sitting on a beach, a jungle or a hillside.
 * <p>
 * Water depth at a spot = world surface height (counts water) minus ocean floor height (doesn't).
 * The footprint is sampled on a 5x5 grid (corners, edges and inside); every sample needs at
 * least the configured depth. Used by {@code mixin.StructureMixin}, so /locate agrees.
 */
public final class WaterOnlyCheck {

    private static final int GRID = 5;

    private static volatile Set<ResourceLocation> structures = Set.of();

    private WaterOnlyCheck() {
    }

    public static void refresh() {
        Set<ResourceLocation> ids = new HashSet<>();
        for (String entry : RLMTConfig.WATER_ONLY_STRUCTURES.get()) {
            ResourceLocation id = ResourceLocation.tryParse(entry.trim());
            if (id != null) ids.add(id);
        }
        structures = Set.copyOf(ids);
    }

    public static boolean applies(Structure structure, Structure.GenerationContext context) {
        if (!RLMTConfig.WATER_ONLY_ENABLED.get()) return false;
        Set<ResourceLocation> current = structures;
        if (current.isEmpty()) return false;
        ResourceLocation id = context.registryAccess().registryOrThrow(Registries.STRUCTURE).getKey(structure);
        return id != null && current.contains(id);
    }

    public static boolean isAllWater(StructurePiecesBuilder pieces, Structure.GenerationContext context) {
        if (pieces.isEmpty()) return true;
        BoundingBox box = pieces.getBoundingBox();
        ChunkGenerator generator = context.chunkGenerator();
        LevelHeightAccessor heightAccessor = context.heightAccessor();
        RandomState randomState = context.randomState();
        int minDepth = RLMTConfig.WATER_ONLY_MIN_DEPTH.get();

        for (int i = 0; i < GRID; i++) {
            int x = box.minX() + (box.maxX() - box.minX()) * i / (GRID - 1);
            for (int j = 0; j < GRID; j++) {
                int z = box.minZ() + (box.maxZ() - box.minZ()) * j / (GRID - 1);
                int surface = generator.getBaseHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, heightAccessor, randomState);
                int floor = generator.getBaseHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, heightAccessor, randomState);
                if (surface - floor < minDepth) return false;
            }
        }
        return true;
    }
}
