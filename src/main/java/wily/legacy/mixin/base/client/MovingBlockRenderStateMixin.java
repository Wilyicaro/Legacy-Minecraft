package wily.legacy.mixin.base.client;

import net.minecraft.client.renderer.block.MovingBlockRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import wily.legacy.client.LegacyMovingBlockRenderState;

@Mixin(MovingBlockRenderState.class)
public class MovingBlockRenderStateMixin implements LegacyMovingBlockRenderState {
    @Unique
    private Submit submit;

    @Override
    public Submit getEnhancedSubmit() {
        return submit;
    }

    @Override
    public void setEnhancedSubmit(Submit submit) {
        this.submit = submit;
    }
}
