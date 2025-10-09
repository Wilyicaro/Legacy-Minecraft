package wily.legacy.client;

import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.world.level.block.entity.BlockEntity;

public interface LegacyMovingBlockRenderState {
    static LegacyMovingBlockRenderState of(MovingBlockRenderState renderState) {
        return (LegacyMovingBlockRenderState) renderState;
    }

    void setEnhancedSubmit(Submit renderState);
    Submit getEnhancedSubmit();

    record Submit(BlockEntityRenderState renderState, BlockEntityRenderer<BlockEntity, BlockEntityRenderState> renderer) {

    }
}
