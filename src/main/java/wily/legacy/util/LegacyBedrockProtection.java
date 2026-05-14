package wily.legacy.util;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public final class LegacyBedrockProtection {
    private LegacyBedrockProtection() {
    }

    public static boolean blocksCreativeBreak(Level level, BlockPos pos, BlockState state) {
        return state.is(Blocks.BEDROCK) && (pos.getY() == level.getMinY() || Level.NETHER.equals(level.dimension()) && pos.getY() == netherRoofY(level));
    }

    private static int netherRoofY(Level level) {
        return level.getMinY() + level.dimensionType().logicalHeight() - 1;
    }
}
