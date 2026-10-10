package wily.legacy.client;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import wily.factoryapi.FactoryAPI;
import wily.factoryapi.FactoryAPIClient;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public final class BedrockSnowyLeaves {
    public static final ColorResolver SNOWY_BIOME_RESOLVER = (biome, x, z) -> isSnowyBiome(biome) ? 0 : 0xFFFFFF;
    private static final List<Block> LEAVES = List.of(Blocks.OAK_LEAVES, Blocks.SPRUCE_LEAVES, Blocks.BIRCH_LEAVES, Blocks.JUNGLE_LEAVES, Blocks.ACACIA_LEAVES, Blocks.DARK_OAK_LEAVES, Blocks.MANGROVE_LEAVES);
    private static final Map<Block, Identifier> MODELS = new IdentityHashMap<>();
    private static final Map<Block, Identifier> FAST_MODELS = new IdentityHashMap<>();
    private static final Map<Identifier, Block> SPRITES = new HashMap<>();
    private static final Map<Block, Float> TINT_SCALES = new ConcurrentHashMap<>();
    private static final int STEPS = 20;
    private static ClientLevel lastLevel;
    private static int levelTicks;
    private static float snowAmount;
    private static int step;
    private static volatile float renderedAmount;

    static {
        for (Block block : LEAVES) {
            String name = BuiltInRegistries.BLOCK.getKey(block).getPath();
            MODELS.put(block, FactoryAPI.createVanillaLocation("snowy_" + name));
            FAST_MODELS.put(block, FactoryAPI.createVanillaLocation("snowy_fast_" + name));
            SPRITES.put(FactoryAPI.createVanillaLocation("block/snowy_" + name), block);
        }
    }

    private BedrockSnowyLeaves() {
    }

    public static void registerModels(Consumer<Identifier> register) {
        MODELS.values().forEach(register);
        FAST_MODELS.values().forEach(register);
    }

    public static boolean isSnowyBiome(Biome biome) {
        return biome.hasPrecipitation() && biome.getBaseTemperature() < 0.15f;
    }

    public static void tick(Minecraft minecraft) {
        var level = minecraft.level;
        float target = level == null ? 0 : level.getRainLevel(1.0f);
        if (level != lastLevel) {
            lastLevel = level;
            levelTicks = 0;
        }
        if (levelTicks < 20) {
            levelTicks++;
            snowAmount = target;
        } else snowAmount = Mth.approach(snowAmount, target, 1 / 600f);

        int newStep = Math.round(snowAmount * STEPS);
        if (newStep == step) return;
        step = newStep;
        renderedAmount = (float) newStep / STEPS;
        if (level != null && LegacyOptions.bedrockSnowyLeaves.get()) markSnowySectionsDirty(minecraft, level);
    }

    public static BlockStateModel getModel(BlockGetter blockGetter, BlockPos pos, BlockState state, BlockStateModel model, boolean fast) {
        if (renderedAmount <= 0 || !(blockGetter instanceof BlockAndTintGetter level)) return model;
        Identifier snowyModel = (fast ? FAST_MODELS : MODELS).get(state.getBlock());
        return snowyModel == null || getSnow(state, level, pos) <= 0 ? model : FactoryAPIClient.getExtraModel(snowyModel);
    }

    public static int applyTint(BlockState state, BlockAndTintGetter level, BlockPos pos, int color) {
        float snow = getSnow(state, level, pos);
        if (snow <= 0) return color;
        Float scale = TINT_SCALES.get(state.getBlock());
        return ARGB.srgbLerp(snow, scale == null ? color : ARGB.scaleRGB(color, scale), -1);
    }

    public static int applyParticleTint(BlockState state, BlockAndTintGetter level, BlockPos pos, int color) {
        float snow = getSnow(state, level, pos);
        return snow <= 0 ? color : ARGB.srgbLerp(snow, color, -1);
    }

    public static void brighten(Identifier name, NativeImage image) {
        if (!name.getNamespace().equals("minecraft") || !name.getPath().startsWith("block/snowy_") || image.format().components() != 4) return;
        long before = 0;
        long after = 0;
        for (int x = 0; x < image.getWidth(); x++) {
            for (int y = 0; y < image.getHeight(); y++) {
                int pixel = image.getPixel(x, y);
                int brightened = ARGB.color(ARGB.alpha(pixel), Math.min(255, ARGB.red(pixel) * 2), Math.min(255, ARGB.green(pixel) * 2), Math.min(255, ARGB.blue(pixel) * 2));
                image.setPixel(x, y, brightened);
                if (ARGB.alpha(pixel) < 128) continue;
                before += ARGB.red(pixel) + ARGB.green(pixel) + ARGB.blue(pixel);
                after += ARGB.red(brightened) + ARGB.green(brightened) + ARGB.blue(brightened);
            }
        }
        Block block = SPRITES.get(name);
        if (block != null && after > 0) TINT_SCALES.put(block, (float) before / after);
    }

    private static float getSnow(BlockState state, BlockAndTintGetter level, BlockPos pos) {
        float amount = renderedAmount;
        if (amount <= 0 || !LegacyOptions.bedrockSnowyLeaves.get() || !(state.getBlock() instanceof LeavesBlock)) return 0;
        return amount * (1 - (level.getBlockTint(pos, SNOWY_BIOME_RESOLVER) & 0xFF) / 255f);
    }

    private static void markSnowySectionsDirty(Minecraft minecraft, ClientLevel level) {
        SectionPos center = SectionPos.of(minecraft.gameRenderer.getMainCamera().position());
        int radius = minecraft.options.getEffectiveRenderDistance() + 1;
        for (int x = center.x() - radius; x <= center.x() + radius; x++) {
            for (int z = center.z() - radius; z <= center.z() + radius; z++) {
                LevelChunk chunk = level.getChunkSource().getChunk(x, z, false);
                if (chunk == null) continue;
                LevelChunkSection[] sections = chunk.getSections();
                for (int i = 0; i < sections.length; i++) {
                    LevelChunkSection section = sections[i];
                    if (section.hasOnlyAir() || !section.maybeHas(s -> s.getBlock() instanceof LeavesBlock)) continue;
                    int y = level.getSectionYFromSectionIndex(i);
                    if (hasSnowyBiomeAround(level, x, y, z)) minecraft.levelRenderer.setSectionDirty(x, y, z);
                }
            }
        }
    }

    private static boolean hasSnowyBiomeAround(ClientLevel level, int sectionX, int sectionY, int sectionZ) {
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                LevelChunk chunk = level.getChunkSource().getChunk(sectionX + x, sectionZ + z, false);
                if (chunk == null) continue;
                for (int y = -1; y <= 1; y++) {
                    int index = level.getSectionIndexFromSectionY(sectionY + y);
                    if (index >= 0 && index < chunk.getSectionsCount()
                            && chunk.getSection(index).getBiomes().maybeHas(biome -> isSnowyBiome(biome.value()))) return true;
                }
            }
        }
        return false;
    }
}
