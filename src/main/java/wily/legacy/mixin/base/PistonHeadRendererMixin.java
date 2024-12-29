package wily.legacy.mixin.base;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.PistonHeadRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.piston.PistonMovingBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import wily.legacy.client.LegacyOption;
import wily.legacy.inventory.LegacyPistonMovingBlockEntity;

@Mixin(PistonHeadRenderer.class)
public abstract class PistonHeadRendererMixin {
    @Shadow protected abstract void renderBlock(BlockPos arg, BlockState arg2, PoseStack arg3, MultiBufferSource arg4, Level arg5, boolean bl, int i);

    @Redirect(method = "render(Lnet/minecraft/world/level/block/piston/PistonMovingBlockEntity;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;II)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/blockentity/PistonHeadRenderer;renderBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/world/level/Level;ZI)V", ordinal = 3))
    private void renderBlock(PistonHeadRenderer instance, BlockPos blockPos, BlockState blockState, PoseStack poseStack1, MultiBufferSource data, Level arg, boolean arg2, int arg3, PistonMovingBlockEntity pistonMovingBlockEntity, float f, PoseStack poseStack, MultiBufferSource multiBufferSource, int i, int j) {
        BlockEntity be = ((LegacyPistonMovingBlockEntity)pistonMovingBlockEntity).getRenderingBlockEntity();
        BlockEntityRenderer<BlockEntity> beRenderer = be == null ? null : Minecraft.getInstance().getBlockEntityRenderDispatcher().getRenderer(be);
        if (be == null || beRenderer == null) {
            renderBlock(pistonMovingBlockEntity.getBlockPos(), blockState, poseStack1, data, arg, arg2, arg3);
        } else if (LegacyOption.enhancedPistonMovingRenderer.get()) {
            beRenderer.render(be, f, poseStack, multiBufferSource, i , j);
        }
    }
}
