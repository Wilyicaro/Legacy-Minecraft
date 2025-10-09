package wily.legacy.mixin.base.piston;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.PistonHeadRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.blockentity.state.PistonHeadRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.piston.PistonMovingBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.client.LegacyMovingBlockRenderState;
import wily.legacy.client.LegacyOptions;
import wily.legacy.inventory.LegacyPistonMovingBlockEntity;

@Mixin(PistonHeadRenderer.class)
public abstract class PistonHeadRendererMixin {
    @WrapOperation(method = "extractRenderState(Lnet/minecraft/world/level/block/piston/PistonMovingBlockEntity;Lnet/minecraft/client/renderer/blockentity/state/PistonHeadRenderState;FLnet/minecraft/world/phys/Vec3;Lnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/blockentity/PistonHeadRenderer;createMovingBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/Holder;Lnet/minecraft/world/level/Level;)Lnet/minecraft/client/renderer/block/MovingBlockRenderState;", ordinal = 3))
    private MovingBlockRenderState renderBlock(BlockPos blockPos, BlockState blockState, Holder holder, Level level, Operation<MovingBlockRenderState> original, PistonMovingBlockEntity pistonMovingBlockEntity, PistonHeadRenderState pistonHeadRenderState, float f, Vec3 vec3, ModelFeatureRenderer.CrumblingOverlay crumblingOverlay) {
        MovingBlockRenderState renderState = original.call(blockPos, blockState, holder, level);

        if (LegacyOptions.enhancedPistonMovingRenderer.get()) {
            BlockEntity be = ((LegacyPistonMovingBlockEntity) pistonMovingBlockEntity).getRenderingBlockEntity();
            BlockEntityRenderer<BlockEntity, BlockEntityRenderState> beRenderer = be == null ? null : Minecraft.getInstance().getBlockEntityRenderDispatcher().getRenderer(be);
            if (beRenderer != null) {
                BlockEntityRenderState beRenderState = beRenderer.createRenderState();
                beRenderer.extractRenderState(be, beRenderState, f, vec3, crumblingOverlay);
                LegacyMovingBlockRenderState.of(renderState).setEnhancedSubmit(new LegacyMovingBlockRenderState.Submit(beRenderState, beRenderer));
            }

        }

        return renderState;
    }

    @WrapOperation(method = "submit(Lnet/minecraft/client/renderer/blockentity/state/PistonHeadRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/CameraRenderState;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/SubmitNodeCollector;submitMovingBlock(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/block/MovingBlockRenderState;)V", ordinal = 0))
    private void submit(SubmitNodeCollector instance, PoseStack poseStack, MovingBlockRenderState renderState, Operation<Void> original, @Local CameraRenderState cameraRenderState) {
        LegacyMovingBlockRenderState legacyMovingBlockRenderState = LegacyMovingBlockRenderState.of(renderState);

        if (legacyMovingBlockRenderState.getEnhancedSubmit() != null) {
            legacyMovingBlockRenderState.getEnhancedSubmit().renderer().submit(legacyMovingBlockRenderState.getEnhancedSubmit().renderState(), poseStack, instance, cameraRenderState);
        } else original.call(instance, poseStack, renderState);
    }
}
