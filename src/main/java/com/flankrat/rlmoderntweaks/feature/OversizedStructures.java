package com.flankrat.rlmoderntweaks.feature;

import com.flankrat.rlmoderntweaks.RLModernTweaks;
import com.flankrat.rlmoderntweaks.config.RLMTConfig;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Lets listed structures be built further than 8 chunks from the chunk they start in.
 * <p>
 * Vanilla only builds the parts of a structure that are within 8 chunks of its start chunk:
 * each chunk looks for structure starts in the chunks up to 8 away ("references"), and only
 * referenced structures get built in it. Island City is wider than that, so its outer ships
 * and coastline were cut off.
 * <p>
 * For listed structures (which must be the only structure in a random_spread structure set),
 * this works out where their starts are further away (the same cheap math vanilla uses to
 * decide where they go), generates those starts the same way vanilla does (same seed, so the
 * same result), and adds references for the chunks they reach. When a chunk is built and a
 * referenced start lies outside what world generation can load around it, the start comes
 * from a small cache instead of the start chunk. Hooks: {@code mixin.ChunkGeneratorMixin}
 * and {@code mixin.StructureManagerMixin}.
 */
public final class OversizedStructures {

    /** Vanilla's reference radius, in chunks. */
    private static final int VANILLA_RADIUS = 8;
    private static final int CACHE_SIZE = 512;

    private record Target(Structure structure, RandomSpreadStructurePlacement placement) {
    }

    private record CacheKey(ResourceKey<Level> dimension, Structure structure, long chunk) {
    }

    private static volatile Set<ResourceLocation> ids = Set.of();
    private static volatile RegistryAccess targetsFor = null;
    private static volatile List<Target> targets = List.of();

    private static final Map<CacheKey, Optional<StructureStart>> CACHE =
            new LinkedHashMap<>(64, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<CacheKey, Optional<StructureStart>> eldest) {
                    return size() > CACHE_SIZE;
                }
            };

    private OversizedStructures() {
    }

    public static void refresh() {
        Set<ResourceLocation> newIds = new HashSet<>();
        for (String entry : RLMTConfig.OVERSIZED_STRUCTURES.get()) {
            ResourceLocation id = ResourceLocation.tryParse(entry.trim());
            if (id != null) newIds.add(id);
        }
        ids = Set.copyOf(newIds);
        targetsFor = null; // re-resolve against the registries next time
        synchronized (CACHE) {
            CACHE.clear();
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        targetsFor = null;
        targets = List.of();
        synchronized (CACHE) {
            CACHE.clear();
        }
    }

    private static boolean active() {
        return RLMTConfig.OVERSIZED_ENABLED.get() && !ids.isEmpty();
    }

    public static boolean applies(Structure structure, RegistryAccess registryAccess) {
        if (!active()) return false;
        ResourceLocation id = registryAccess.registryOrThrow(Registries.STRUCTURE).getKey(structure);
        return id != null && ids.contains(id);
    }

    private static List<Target> targets(RegistryAccess registryAccess) {
        if (targetsFor == registryAccess) return targets;
        Registry<Structure> structures = registryAccess.registryOrThrow(Registries.STRUCTURE);
        List<Target> found = new ArrayList<>();
        for (StructureSet set : registryAccess.registryOrThrow(Registries.STRUCTURE_SET)) {
            if (set.structures().size() != 1) continue;
            Structure structure = set.structures().get(0).structure().value();
            ResourceLocation id = structures.getKey(structure);
            if (id == null || !ids.contains(id)) continue;
            StructurePlacement placement = set.placement();
            if (placement instanceof RandomSpreadStructurePlacement spread) {
                found.add(new Target(structure, spread));
            } else {
                RLModernTweaks.LOGGER.warn("[OversizedStructures] {} doesn't use random_spread placement, skipping it", id);
            }
        }
        targets = List.copyOf(found);
        targetsFor = registryAccess;
        return targets;
    }

    /**
     * Called after vanilla adds the references from starts within 8 chunks. Adds references
     * from starts of listed structures further away whose bounding box reaches this chunk.
     */
    public static void addFarReferences(ChunkGenerator generator, WorldGenLevel level,
                                        StructureManager structureManager, ChunkAccess chunk) {
        if (!active()) return;
        ServerLevel serverLevel = level.getLevel();
        List<Target> current = targets(serverLevel.registryAccess());
        if (current.isEmpty()) return;

        ChunkGeneratorStructureState state = serverLevel.getChunkSource().getGeneratorState();
        int radius = RLMTConfig.OVERSIZED_RADIUS.get();
        ChunkPos pos = chunk.getPos();
        int minX = pos.getMinBlockX();
        int minZ = pos.getMinBlockZ();
        SectionPos section = SectionPos.bottomOf(chunk);

        for (Target target : current) {
            int spacing = target.placement().spacing();
            Set<Long> seen = new HashSet<>();
            // One possible start per spacing x spacing cell; visit every cell overlapping the radius.
            for (int cellX = Math.floorDiv(pos.x - radius, spacing); cellX <= Math.floorDiv(pos.x + radius, spacing); cellX++) {
                for (int cellZ = Math.floorDiv(pos.z - radius, spacing); cellZ <= Math.floorDiv(pos.z + radius, spacing); cellZ++) {
                    ChunkPos start = target.placement().getPotentialStructureChunk(state.getLevelSeed(), cellX * spacing, cellZ * spacing);
                    int dx = Math.abs(start.x - pos.x);
                    int dz = Math.abs(start.z - pos.z);
                    if (dx > radius || dz > radius) continue;
                    if (dx <= VANILLA_RADIUS && dz <= VANILLA_RADIUS) continue; // vanilla already handled it
                    if (!seen.add(start.toLong())) continue;
                    if (!target.placement().isStructureChunk(state, start.x, start.z)) continue;

                    StructureStart structureStart = getStart(serverLevel, generator, target.structure(), start);
                    if (structureStart == null || !structureStart.isValid()) continue;
                    if (!structureStart.getBoundingBox().intersects(minX, minZ, minX + 15, minZ + 15)) continue;
                    structureManager.addReferenceForStructure(section, target.structure(), start.toLong(), chunk);
                }
            }
        }
    }

    /** The start of a structure in the given chunk, generated like vanilla does (cached). */
    public static StructureStart getStart(ServerLevel level, Structure structure, ChunkPos chunkPos) {
        return getStart(level, level.getChunkSource().getGenerator(), structure, chunkPos);
    }

    private static StructureStart getStart(ServerLevel level, ChunkGenerator generator, Structure structure, ChunkPos chunkPos) {
        CacheKey key = new CacheKey(level.dimension(), structure, chunkPos.toLong());
        Optional<StructureStart> cached;
        synchronized (CACHE) {
            cached = CACHE.get(key);
        }
        if (cached != null) return cached.orElse(null);

        // Same call as ChunkGenerator.tryGenerateStructure, so the result matches the real start.
        StructureStart start = structure.generate(level.registryAccess(), generator, generator.getBiomeSource(),
                level.getChunkSource().randomState(), level.getServer().getStructureManager(), level.getSeed(),
                chunkPos, 0, level, structure.biomes()::contains);
        Optional<StructureStart> result = start.isValid() ? Optional.of(start) : Optional.empty();
        synchronized (CACHE) {
            CACHE.put(key, result);
        }
        return result.orElse(null);
    }
}
