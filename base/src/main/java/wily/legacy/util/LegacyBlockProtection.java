package wily.legacy.util;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import wily.factoryapi.base.config.FactoryConfig;
import wily.legacy.config.LegacyCommonOptions;

public final class LegacyBlockProtection {
    private LegacyBlockProtection() {
    }

    public static boolean blocksBreak(Level level, BlockPos pos, BlockState state, boolean creative) {
        return blocksNetherPortalBreak(state) || creative && blocksCreativeBreak(level, pos, state);
    }

    public static boolean blocksNetherPortalBreak(BlockState state) {
        return isEnabled() && state.is(Blocks.NETHER_PORTAL);
    }

    public static boolean blocksCreativeBreak(Level level, BlockPos pos, BlockState state) {
        return isEnabled() && state.is(Blocks.BEDROCK) && (pos.getY() == level.getMinY() || Level.NETHER.equals(level.dimension()) && pos.getY() == netherRoofY(level));
    }

    public static boolean isEnabled() {
        return FactoryConfig.hasCommonConfigEnabled(LegacyCommonOptions.legacyBlockProtection);
    }

    private static int netherRoofY(Level level) {
        return level.getMinY() + level.dimensionType().logicalHeight() - 1;
    }
}
