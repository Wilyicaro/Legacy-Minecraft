package wily.legacy.client.seedpreview;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.Holder;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.material.MapColor;

public class SeedMapTexture extends DynamicTexture {
    public SeedMapTexture(SeedMap map) {
        super(() -> "Seed preview", createImage(map));
        sampler = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST);
    }

    private static NativeImage createImage(SeedMap map) {
        NativeImage image = new NativeImage(SeedMap.SIZE, SeedMap.SIZE, false);
        for (int z = 0; z < SeedMap.SIZE; z++) {
            for (int x = 0; x < SeedMap.SIZE; x++) {
                Holder<Biome> biome = map.biomes().get(z * SeedMap.SIZE + x);
                image.setPixel(x, z, 0xFF000000 | color(biome, SeedMap.blockCoordinate(x), SeedMap.blockCoordinate(z)));
            }
        }
        return image;
    }

    private static int color(Holder<Biome> biome, int x, int z) {
        if (biome.is(BiomeTags.IS_OCEAN) || biome.is(BiomeTags.IS_RIVER)) return biome.value().getWaterColor();
        if (biome.value().hasPrecipitation() && biome.value().getBaseTemperature() < 0.15f) return MapColor.SNOW.col;
        if (biome.is(Biomes.STONY_SHORE) || biome.is(Biomes.STONY_PEAKS)) return MapColor.STONE.col;
        if (biome.is(Biomes.DESERT) || biome.is(BiomeTags.IS_BEACH)) return MapColor.SAND.col;
        if (biome.is(BiomeTags.IS_BADLANDS)) return MapColor.TERRACOTTA_ORANGE.col;
        if (biome.is(Biomes.MUSHROOM_FIELDS)) return MapColor.COLOR_PURPLE.col;
        if (biome.is(Biomes.CHERRY_GROVE)) return MapColor.COLOR_PINK.col;
        return biome.value().getGrassColor(x, z);
    }
}
