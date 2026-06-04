package wily.legacy.mixin.base.client;

import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.legacy.client.LegacyChunkLoading;
import wily.legacy.client.LegacyRenderDistance;

@Mixin(BlockEntityRenderDispatcher.class)
public abstract class BlockEntityRenderDispatcherMixin {
    @Shadow
    private Vec3 cameraPos;

    @Inject(method = "tryExtractRenderState", at = @At("HEAD"), cancellable = true)
    private void tryExtractRenderState(BlockEntity blockEntity, float tickDelta, ModelFeatureRenderer.CrumblingOverlay crumblingOverlay, CallbackInfoReturnable<BlockEntityRenderState> cir) {
        if (blockEntity instanceof BeaconBlockEntity && !LegacyChunkLoading.isSectionVisible(blockEntity.getBlockPos())) {
            cir.setReturnValue(null);
            return;
        }
        if (!LegacyRenderDistance.shouldRender(blockEntity, cameraPos)) cir.setReturnValue(null);
    }
}
