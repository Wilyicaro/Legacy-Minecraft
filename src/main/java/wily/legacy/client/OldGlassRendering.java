package wily.legacy.client;

import net.minecraft.world.level.block.TransparentBlock;
import net.minecraft.world.level.block.state.BlockState;

public final class OldGlassRendering {
    private OldGlassRendering() {
    }

    
    public static boolean shouldRenderSharedFace(BlockState state, BlockState adjacentState) {
        return LegacyOptions.oldGlassRendering.get() && isGlassBlock(state) && isGlassBlock(adjacentState);
    }

    
    private static boolean isGlassBlock(BlockState state) {
        return state.getBlock() instanceof TransparentBlock;
    }
}
