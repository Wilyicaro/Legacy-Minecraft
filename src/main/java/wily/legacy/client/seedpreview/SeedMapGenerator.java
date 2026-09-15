package wily.legacy.client.seedpreview;

import net.minecraft.client.gui.screens.worldselection.WorldCreationContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.placement.ConcentricRingsStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;

import static wily.legacy.client.seedpreview.SeedMap.SIZE;
import static wily.legacy.client.seedpreview.SeedMap.checkCancelled;

public class SeedMapGenerator {
    private final WorldCreationContext context;
    private final RandomState random;
    private final BiomeManager biomes;
    private final ChunkGeneratorStructureState structures;
    private final SeedMapMarker spawn;
    private final int minY;
    private final int maxY;

    public SeedMapGenerator(WorldCreationContext context) {
        this.context = context;
        LevelStem dimension = context.selectedDimensions().bake(context.datapackDimensions()).dimensions().getValueOrThrow(LevelStem.OVERWORLD);
        ChunkGenerator generator = dimension.generator();
        long seed = context.options().seed();
        NoiseGeneratorSettings settings = generator instanceof NoiseBasedChunkGenerator noise ? noise.generatorSettings().value() : NoiseGeneratorSettings.dummy();
        random = RandomState.create(settings, context.worldgenLoadContext().lookupOrThrow(Registries.NOISE), seed);
        biomes = new BiomeManager((x, y, z) -> generator.getBiomeSource().getNoiseBiome(x, y, z, random.sampler()), BiomeManager.obfuscateSeed(seed));
        structures = context.options().generateStructures()
                ? generator.createState(context.worldgenLoadContext().lookupOrThrow(Registries.STRUCTURE_SET), random, seed) : null;
        spawn = SeedMapMarker.spawn(ChunkPos.containing(random.sampler().findSpawnPosition()).getWorldPosition().offset(8, 0, 8));
        minY = dimension.type().value().minY();
        maxY = Math.min(generator.getSeaLevel(), minY + dimension.type().value().height() - 1);
    }

    public SeedMap generate(int chunkX, int chunkZ, SeedMap previous, BooleanSupplier cancelled) {
        SeedMap map = SeedMap.generate(context, random, chunkX, chunkZ, previous, cancelled);
        if (map == previous) return previous;
        return new SeedMap(chunkX, chunkZ, map.biomes(), findMarkers(map, previous, cancelled));
    }

    private List<SeedMapMarker> findMarkers(SeedMap map, SeedMap previous, BooleanSupplier cancelled) {
        if (structures == null) return List.of(spawn);
        List<SeedMapMarker> markers = new ArrayList<>();
        if (previous != null) {
            for (SeedMapMarker marker : previous.markers()) {
                if (marker != spawn && map.contains(ChunkPos.containing(marker.pos()))) markers.add(marker);
            }
        }
        for (Holder<StructureSet> set : structures.possibleStructureSets()) {
            checkCancelled(cancelled);
            if (set.value().placement() instanceof RandomSpreadStructurePlacement spread) {
                int spacing = spread.spacing();
                int minX = Math.floorDiv(map.chunkX() - SIZE / 2, spacing);
                int minZ = Math.floorDiv(map.chunkZ() - SIZE / 2, spacing);
                int maxX = Math.floorDiv(map.chunkX() + SIZE / 2 - 1, spacing);
                int maxZ = Math.floorDiv(map.chunkZ() + SIZE / 2 - 1, spacing);
                for (int x = minX; x <= maxX; x++) {
                    for (int z = minZ; z <= maxZ; z++) {
                        checkCancelled(cancelled);
                        ChunkPos chunk = spread.getPotentialStructureChunk(structures.getLevelSeed(), x * spacing, z * spacing);
                        addMarker(markers, map, previous, set, chunk);
                    }
                }
            } else if (set.value().placement() instanceof ConcentricRingsStructurePlacement rings) {
                List<ChunkPos> positions = structures.getRingPositionsFor(rings);
                if (positions == null) continue;
                for (ChunkPos chunk : positions) {
                    checkCancelled(cancelled);
                    addMarker(markers, map, previous, set, chunk);
                }
            }
        }
        markers.add(spawn);
        return markers;
    }

    private void addMarker(List<SeedMapMarker> markers, SeedMap map, SeedMap previous, Holder<StructureSet> set, ChunkPos chunk) {
        if (!map.contains(chunk) || previous != null && previous.contains(chunk)) return;
        if (!set.value().placement().isStructureChunk(structures, chunk.x(), chunk.z())) return;
        BlockPos pos = chunk.getWorldPosition().offset(8, 0, 8);
        for (StructureSet.StructureSelectionEntry entry : set.value().structures()) {
            if (hasBiome(entry.structure().value(), map.biomeAt(chunk), pos)) {
                markers.add(SeedMapMarker.structure(pos, set, entry.structure()));
                return;
            }
        }
    }

    private boolean hasBiome(Structure structure, Holder<Biome> surface, BlockPos pos) {
        if (structure.biomes().contains(surface)) return true;
        if (structure.step() != GenerationStep.Decoration.UNDERGROUND_STRUCTURES
                && structure.step() != GenerationStep.Decoration.UNDERGROUND_DECORATION) return false;
        for (int y = minY; y <= maxY; y += SeedMap.BLOCKS_PER_PIXEL) {
            if (structure.biomes().contains(biomes.getBiome(pos.atY(y)))) return true;
        }
        return false;
    }
}
