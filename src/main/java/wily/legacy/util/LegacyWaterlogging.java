package wily.legacy.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.sounds.SoundEvent;
import wily.legacy.Legacy4J;

//? if fabric {
import net.fabricmc.loader.api.FabricLoader;
//?} else if forge {
/*import net.minecraftforge.fml.loading.FMLPaths;
*///?} else if neoforge {
/*import net.neoforged.fml.loading.FMLPaths;
*///?}

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

public final class LegacyWaterlogging {
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    public static final IntegerProperty PASS_THROUGH_WATER_LEVEL = IntegerProperty.create("pass_through_water_level", 0, 8);

    private static final String WATERLOGGABLE_CONFIG_FILE = "waterloggable-blocks.txt";
    private static final String PASS_THROUGH_CONFIG_FILE = "water-pass-through-blocks.txt";
    private static final String DEFAULTS_RESOURCE_ROOT = "legacy/waterlogging/";

    private static final Config WATERLOGGABLE_CONFIG = loadConfig(WATERLOGGABLE_CONFIG_FILE, DEFAULTS_RESOURCE_ROOT + WATERLOGGABLE_CONFIG_FILE);
    private static final Config PASS_THROUGH_CONFIG = loadConfig(PASS_THROUGH_CONFIG_FILE, DEFAULTS_RESOURCE_ROOT + PASS_THROUGH_CONFIG_FILE);
    private static final ThreadLocal<String> CURRENT_BLOCK_ID = new ThreadLocal<>();

    private LegacyWaterlogging() {
    }

    public static void beginBlockRegistration(String blockId) {
        CURRENT_BLOCK_ID.set(blockId);
    }

    public static void endBlockRegistration() {
        CURRENT_BLOCK_ID.remove();
    }

    public static boolean supportsWaterlogging(Block block) {
        String blockId = getBlockId(block);
        return blockId != null && supportsWaterlogging(blockId);
    }

    public static boolean supportsPassThroughWaterLevel(Block block) {
        String blockId = getBlockId(block);
        return blockId != null && PASS_THROUGH_CONFIG.matches(blockId);
    }

    public static boolean isWaterloggableState(BlockState state) {
        return state.hasProperty(WATERLOGGED) && supportsWaterlogging(state.getBlock());
    }

    public static boolean letsWaterFlowThrough(BlockState state) {
        return state.hasProperty(PASS_THROUGH_WATER_LEVEL) && supportsPassThroughWaterLevel(state.getBlock());
    }

    public static boolean isPassThroughState(BlockState state) {
        return letsWaterFlowThrough(state) && isWaterloggableState(state);
    }

    public static boolean isWater(Fluid fluid) {
        return fluid == Fluids.WATER || fluid == Fluids.FLOWING_WATER;
    }

    public static boolean isWater(FluidState fluidState) {
        return isWater(fluidState.getType());
    }

    public static BlockState normalizeDefaultState(BlockState state) {
        if (!isWaterloggableState(state)) {
            return state;
        }

        state = setWaterlogged(state, false);
        if (isPassThroughState(state)) {
            state = state.setValue(PASS_THROUGH_WATER_LEVEL, 0);
        }
        return state;
    }

    public static BlockState applyPlacementFluid(BlockGetter level, BlockPos pos, BlockState state) {
        return resolvePlacementFluid(level, pos, state, false);
    }

    public static BlockState syncPlacementFluid(BlockGetter level, BlockPos pos, BlockState state) {
        return resolvePlacementFluid(level, pos, state, true);
    }

    public static boolean canPlaceWater(BlockState state, FluidState fluidState) {
        if (!isWater(fluidState) || !isWaterloggableState(state)) {
            return false;
        }
        if (isPassThroughState(state)) {
            return getStoredWaterLevel(fluidState) > getStoredPassThroughLevel(state);
        }
        return !state.getValue(WATERLOGGED);
    }

    public static boolean placeWater(LevelAccessor level, BlockPos pos, BlockState state, FluidState fluidState) {
        if (!canPlaceWater(state, fluidState)) {
            return false;
        }
        if (!level.isClientSide()) {
            BlockState updatedState = isSupportedCauldron(state)
                ? updateCauldronFluidState(level, pos, state, shouldWaterlogCauldron(level, pos, state, fluidState))
                : applyStoredWater(state, fluidState);
            level.setBlock(pos, updatedState, Block.UPDATE_ALL);
            Fluid fluid = fluidState.getType();
            level.scheduleTick(pos, fluid, fluid.getTickDelay(level));
        }
        return true;
    }

    public static ItemStack pickupWater(LevelAccessor level, BlockPos pos, BlockState state) {
        if (!isWaterloggableState(state) || !state.getValue(WATERLOGGED)) {
            return ItemStack.EMPTY;
        }
        if (isSupportedCauldron(state)) {
            BlockState updatedState = state.is(Blocks.WATER_CAULDRON)
                ? setWaterlogged(state, false)
                : Blocks.CAULDRON.defaultBlockState();
            level.setBlock(pos, updatedState, Block.UPDATE_ALL);
            return new ItemStack(Items.WATER_BUCKET);
        }

        level.setBlock(pos, clearStoredWater(state), Block.UPDATE_ALL);
        if (!state.canSurvive(level, pos)) {
            level.destroyBlock(pos, true);
        }
        return new ItemStack(Items.WATER_BUCKET);
    }

    public static boolean isSupportedCauldron(BlockState state) {
        return state.is(Blocks.CAULDRON) || state.is(Blocks.WATER_CAULDRON);
    }

    public static void updateCauldronFromWorld(LevelAccessor level, BlockPos pos, BlockState state) {
        if (!isSupportedCauldron(state) || level.isClientSide()) {
            return;
        }

        BlockState updatedState = updateCauldronFluidState(level, pos, state, hasExternalCauldronWater(level, pos));
        if (updatedState != state) {
            level.setBlock(pos, updatedState, Block.UPDATE_ALL);
        }
    }

    public static Optional<SoundEvent> getPickupSound() {
        return Fluids.WATER.getPickupSound();
    }

    public static BlockState preserveWaterloggedState(BlockState currentState, BlockState updatedState) {
        if (!isWaterloggableState(currentState) || !isWaterloggableState(updatedState)) {
            return updatedState;
        }

        BlockState preservedState = setWaterlogged(updatedState, currentState.getValue(WATERLOGGED));
        if (isPassThroughState(currentState) && isPassThroughState(updatedState)) {
            int storedLevel = currentState.getValue(PASS_THROUGH_WATER_LEVEL);
            if (preservedState.getValue(PASS_THROUGH_WATER_LEVEL) != storedLevel) {
                preservedState = preservedState.setValue(PASS_THROUGH_WATER_LEVEL, storedLevel);
            }
        }
        return preservedState;
    }

    public static FluidState getStoredFluidState(BlockState state) {
        if (!isWaterloggableState(state) || !state.getValue(WATERLOGGED)) {
            return Fluids.EMPTY.defaultFluidState();
        }
        if (isPassThroughState(state)) {
            int storedLevel = getStoredPassThroughLevel(state);
            return storedLevel <= 0
                ? Fluids.EMPTY.defaultFluidState()
                : storedLevel >= 8
                    ? Fluids.WATER.getSource(false)
                    : ((FlowingFluid) Fluids.WATER).getFlowing(storedLevel, false);
        }
        return Fluids.WATER.getSource(false);
    }

    public static BlockState recalculatePassThroughState(BlockGetter level, BlockPos pos, BlockState state) {
        if (!isPassThroughState(state)) {
            return state;
        }

        int storedLevel = computePassThroughLevel(level, pos);
        BlockState updatedState = setWaterlogged(state, storedLevel > 0);
        if (updatedState.getValue(PASS_THROUGH_WATER_LEVEL) != storedLevel) {
            updatedState = updatedState.setValue(PASS_THROUGH_WATER_LEVEL, storedLevel);
        }
        return updatedState;
    }

    private static BlockState resolvePlacementFluid(BlockGetter level, BlockPos pos, BlockState state, boolean syncExistingState) {
        FluidState fluidState = level.getFluidState(pos);
        if (isSupportedCauldron(state)) {
            return updateCauldronFluidState(level, pos, state, shouldWaterlogCauldron(level, pos, state, fluidState));
        }
        if (!isWaterloggableState(state)) {
            return state;
        }
        if (syncExistingState) {
            return isPassThroughState(state) ? recalculatePassThroughState(level, pos, state) : setWaterlogged(state, isWater(fluidState));
        }
        return isWater(fluidState) ? applyStoredWater(state, fluidState) : state;
    }

    private static boolean supportsWaterlogging(String blockId) {
        return WATERLOGGABLE_CONFIG.matches(blockId) || PASS_THROUGH_CONFIG.matches(blockId);
    }

    private static int computePassThroughLevel(BlockGetter level, BlockPos pos) {
        FluidState aboveFluidState = level.getFluidState(pos.above());
        if (isWater(aboveFluidState)) {
            return 8;
        }

        int storedLevel = 0;
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos neighborPos = pos.relative(direction);
            BlockState neighborState = level.getBlockState(neighborPos);
            FluidState neighborFluidState = level.getFluidState(neighborPos);
            storedLevel = Math.max(storedLevel, getHorizontalSpreadLevel(neighborState, neighborFluidState));
        }
        return storedLevel;
    }

    private static int getHorizontalSpreadLevel(BlockState neighborState, FluidState neighborFluidState) {
        if (!isWater(neighborFluidState)) {
            return 0;
        }
        if (isPassThroughState(neighborState)) {
            return Math.max(0, getStoredPassThroughLevel(neighborState) - 1);
        }
        if (neighborFluidState.isSource() || neighborFluidState.getValue(FlowingFluid.FALLING)) {
            return 7;
        }
        return Math.max(0, neighborFluidState.getAmount() - 1);
    }

    private static int getStoredPassThroughLevel(BlockState state) {
        return state.getValue(PASS_THROUGH_WATER_LEVEL);
    }

    private static int getStoredWaterLevel(FluidState fluidState) {
        if (!isWater(fluidState)) {
            return 0;
        }
        return fluidState.isSource() || fluidState.getValue(FlowingFluid.FALLING) ? 8 : fluidState.getAmount();
    }

    private static BlockState applyStoredWater(BlockState state, FluidState fluidState) {
        if (isPassThroughState(state)) {
            int storedLevel = getStoredWaterLevel(fluidState);
            return setWaterlogged(state, storedLevel > 0).setValue(PASS_THROUGH_WATER_LEVEL, storedLevel);
        }
        return setWaterlogged(state, true);
    }

    private static BlockState clearStoredWater(BlockState state) {
        if (isPassThroughState(state)) {
            return setWaterlogged(state, false).setValue(PASS_THROUGH_WATER_LEVEL, 0);
        }
        return setWaterlogged(state, false);
    }

    private static boolean shouldWaterlogCauldron(BlockGetter level, BlockPos pos, BlockState state, FluidState fluidState) {
        return isWater(fluidState)
            && (hasExternalCauldronWater(level, pos) || state.is(Blocks.CAULDRON) && !fluidState.getValue(FlowingFluid.FALLING));
    }

    private static boolean hasExternalCauldronWater(BlockGetter level, BlockPos pos) {
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            if (isWater(level.getFluidState(pos.relative(direction)))) {
                return true;
            }
        }
        return isWater(level.getFluidState(pos.below()));
    }

    private static BlockState updateCauldronFluidState(BlockGetter level, BlockPos pos, BlockState state, boolean waterlogged) {
        if (state.is(Blocks.CAULDRON) && isWater(level.getFluidState(pos.above()))) {
            return setWaterlogged(Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 3), waterlogged);
        }
        return setWaterlogged(state, waterlogged);
    }

    private static String getBlockId(Block block) {
        String blockId = CURRENT_BLOCK_ID.get();
        if (blockId != null) {
            return blockId;
        }
        try {
            return block.builtInRegistryHolder().getRegisteredName();
        } catch (IllegalStateException exception) {
            return null;
        }
    }

    private static Config loadConfig(String fileName, String defaultsResourcePath) {
        List<String> defaultLines = readBundledDefaults(defaultsResourcePath);
        Path configPath = getConfigDirectory().resolve(fileName);
        createDefaultConfig(configPath, defaultLines);
        try {
            LinkedHashSet<String> mergedEntries = new LinkedHashSet<>(defaultLines);
            LinkedHashSet<String> configuredEntries = readConfiguredEntries(configPath);
            mergedEntries.addAll(configuredEntries);
            if (!mergedEntries.equals(configuredEntries)) {
                writeConfig(configPath, mergedEntries);
            }
            return new Config(mergedEntries);
        } catch (IOException ignored) {
            return new Config(new LinkedHashSet<>(defaultLines));
        }
    }

    private static List<String> readBundledDefaults(String resourcePath) {
        try (InputStream stream = LegacyWaterlogging.class.getClassLoader().getResourceAsStream(resourcePath)) {
            if (stream == null) {
                return List.of();
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                return reader.lines().map(String::trim).filter(line -> !line.isEmpty()).toList();
            }
        } catch (IOException ignored) {
            return List.of();
        }
    }

    private static LinkedHashSet<String> readConfiguredEntries(Path configPath) throws IOException {
        LinkedHashSet<String> configuredEntries = new LinkedHashSet<>();
        for (String line : Files.readAllLines(configPath)) {
            String trimmedLine = line.trim();
            if (!trimmedLine.isEmpty()) {
                configuredEntries.add(trimmedLine);
            }
        }
        return configuredEntries;
    }

    private static void createDefaultConfig(Path configPath, List<String> defaultLines) {
        if (Files.exists(configPath)) {
            return;
        }
        try {
            Files.createDirectories(configPath.getParent());
            writeConfig(configPath, defaultLines);
        } catch (IOException ignored) {
        }
    }

    private static void writeConfig(Path configPath, Iterable<String> lines) throws IOException {
        Files.write(configPath, lines);
    }

    private static BlockState setWaterlogged(BlockState state, boolean waterlogged) {
        return state.hasProperty(WATERLOGGED) ? state.setValue(WATERLOGGED, waterlogged) : state;
    }

    private static Path getConfigDirectory() {
        //? if fabric {
        return FabricLoader.getInstance().getConfigDir().resolve(Legacy4J.MOD_ID);
        //?} else if forge || neoforge {
        /*return FMLPaths.CONFIGDIR.get().resolve(Legacy4J.MOD_ID);*/
        //?} else {
        /*throw new AssertionError();*/
        //?}
    }

    private static final class Config {
        private final Set<String> exactMatches;
        private final List<Pattern> wildcardMatches;

        private Config(Set<String> configuredEntries) {
            LinkedHashSet<String> exactMatches = new LinkedHashSet<>();
            List<Pattern> wildcardMatches = new java.util.ArrayList<>();
            for (String configuredEntry : configuredEntries) {
                if (configuredEntry.contains("*")) {
                    wildcardMatches.add(Pattern.compile(toRegex(configuredEntry)));
                } else {
                    exactMatches.add(configuredEntry);
                }
            }
            this.exactMatches = Set.copyOf(exactMatches);
            this.wildcardMatches = List.copyOf(wildcardMatches);
        }

        private boolean matches(String blockId) {
            if (blockId == null || blockId.isEmpty()) {
                return false;
            }
            if (exactMatches.contains(blockId)) {
                return true;
            }
            for (Pattern wildcardMatch : wildcardMatches) {
                if (wildcardMatch.matcher(blockId).matches()) {
                    return true;
                }
            }
            return false;
        }

        private static String toRegex(String wildcard) {
            return "^" + Pattern.quote(wildcard).replace("*", "\\E.*\\Q") + "$";
        }
    }
}
