package wily.legacy.client.seedpreview;

import net.minecraft.client.gui.screens.worldselection.WorldCreationContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.Mth;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.FixedBiomeSource;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.function.BooleanSupplier;

public record SeedMap(int chunkX, int chunkZ, List<Holder<Biome>> biomes) {
    public static final int VIEW_SIZE = 128;
    public static final int PADDING = 32;
    public static final int SIZE = VIEW_SIZE + PADDING * 2;
    private static final int BLOCKS_PER_PIXEL = 16;
    public static final int MAX_CENTER = (int) WorldBorder.MAX_CENTER_COORDINATE / BLOCKS_PER_PIXEL - VIEW_SIZE / 2;
    public static final int MAX_SAMPLE_CENTER = MAX_CENTER - PADDING;

    public SeedMap {
        if (biomes.size() != SIZE * SIZE) throw new IllegalArgumentException("Biome count must match the map size");
        biomes = List.copyOf(biomes);
    }

    public static SeedMap generate(WorldCreationContext context, int chunkX, int chunkZ, SeedMap previous, BooleanSupplier cancelled) {
        checkCancelled(cancelled);
        if (previous != null && previous.chunkX == chunkX && previous.chunkZ == chunkZ) return previous;
        LevelStem dimension = context.selectedDimensions().bake(context.datapackDimensions()).dimensions().getValueOrThrow(LevelStem.OVERWORLD);
        ChunkGenerator generator = dimension.generator();
        if (generator.getBiomeSource() instanceof FixedBiomeSource fixed) {
            return new SeedMap(chunkX, chunkZ, Collections.nCopies(SIZE * SIZE, fixed.getNoiseBiome(0, 0, 0)));
        }
        if (!(generator instanceof NoiseBasedChunkGenerator noise)) {
            throw new UnsupportedOperationException("Unsupported preview generator: " + generator.getClass().getName());
        }
        long seed = context.options().seed();
        RandomState random = RandomState.create(noise.generatorSettings().value(), context.worldgenLoadContext().lookupOrThrow(Registries.NOISE), seed);
        BiomeManager biomes = new BiomeManager((x, y, z) -> generator.getBiomeSource().getNoiseBiome(x, y, z, random.sampler()), BiomeManager.obfuscateSeed(seed));
        int minY = dimension.type().value().minY();
        int maxY = minY + dimension.type().value().height() - 1;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        List<Holder<Biome>> samples = new ArrayList<>(SIZE * SIZE);
        int offsetX = previous == null ? SIZE : chunkX - previous.chunkX;
        int offsetZ = previous == null ? SIZE : chunkZ - previous.chunkZ;
        for (int z = 0; z < SIZE; z++) {
            for (int x = 0; x < SIZE; x++) {
                checkCancelled(cancelled);
                int oldX = x + offsetX;
                int oldZ = z + offsetZ;
                if (previous != null && oldX >= 0 && oldX < SIZE && oldZ >= 0 && oldZ < SIZE) {
                    samples.add(previous.biomes.get(oldZ * SIZE + oldX));
                    continue;
                }
                int blockX = blockCoordinate(chunkX, x);
                int blockZ = blockCoordinate(chunkZ, z);
                int surface = Mth.floor(random.router().preliminarySurfaceLevel().compute(new DensityFunction.SinglePointContext(blockX, 0, blockZ)));
                surface = Mth.clamp(Math.max(generator.getSeaLevel() - 1, surface), minY, maxY);
                samples.add(biomes.getBiome(pos.set(blockX, surface, blockZ)));
            }
        }
        return new SeedMap(chunkX, chunkZ, samples);
    }

    private static void checkCancelled(BooleanSupplier cancelled) {
        if (cancelled.getAsBoolean()) throw new CancellationException();
    }

    public static int blockCoordinate(int chunk, int pixel) {
        return (chunk + pixel - SIZE / 2) * BLOCKS_PER_PIXEL + BLOCKS_PER_PIXEL / 2;
    }
}
