package wily.legacy.client.seedpreview;

import net.minecraft.client.gui.screens.worldselection.WorldCreationContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.Mth;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.FixedBiomeSource;
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

public record SeedMap(List<Holder<Biome>> biomes) {
    public static final int SIZE = 128;
    private static final int BLOCKS_PER_PIXEL = 16;

    public SeedMap {
        if (biomes.size() != SIZE * SIZE) throw new IllegalArgumentException("Biome count must match the map size");
        biomes = List.copyOf(biomes);
    }

    public static SeedMap generate(WorldCreationContext context, BooleanSupplier cancelled) {
        checkCancelled(cancelled);
        LevelStem dimension = context.selectedDimensions().bake(context.datapackDimensions()).dimensions().getValueOrThrow(LevelStem.OVERWORLD);
        ChunkGenerator generator = dimension.generator();
        if (generator.getBiomeSource() instanceof FixedBiomeSource fixed) {
            return new SeedMap(Collections.nCopies(SIZE * SIZE, fixed.getNoiseBiome(0, 0, 0)));
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
        for (int z = 0; z < SIZE; z++) {
            for (int x = 0; x < SIZE; x++) {
                checkCancelled(cancelled);
                int blockX = blockCoordinate(x);
                int blockZ = blockCoordinate(z);
                int surface = Mth.floor(random.router().preliminarySurfaceLevel().compute(new DensityFunction.SinglePointContext(blockX, 0, blockZ)));
                surface = Mth.clamp(Math.max(generator.getSeaLevel() - 1, surface), minY, maxY);
                samples.add(biomes.getBiome(pos.set(blockX, surface, blockZ)));
            }
        }
        return new SeedMap(samples);
    }

    private static void checkCancelled(BooleanSupplier cancelled) {
        if (cancelled.getAsBoolean()) throw new CancellationException();
    }

    public static int blockCoordinate(int pixel) {
        return (pixel - SIZE / 2) * BLOCKS_PER_PIXEL + BLOCKS_PER_PIXEL / 2;
    }
}
