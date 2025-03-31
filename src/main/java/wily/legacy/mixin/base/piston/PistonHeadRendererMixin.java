package wily.legacy.mixin.base.piston;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
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
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import wily.legacy.client.LegacyOptions;
import wily.legacy.inventory.LegacyPistonMovingBlockEntity;

@Mixin(PistonHeadRenderer.class)
public abstract class PistonHeadRendererMixin {
    @WrapOperation(method = /*? if >=1.21.5 {*//*"render(Lnet/minecraft/world/level/block/piston/PistonMovingBlockEntity;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;IILnet/minecraft/world/phys/Vec3;)V"*//*?} else {*/"render(Lnet/minecraft/world/level/block/piston/PistonMovingBlockEntity;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;II)V"/*?}*/, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/blockentity/PistonHeadRenderer;renderBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/world/level/Level;ZI)V", ordinal = 3))
    private void renderBlock(PistonHeadRenderer instance, BlockPos blockPos, BlockState blockState, PoseStack poseStack1, MultiBufferSource data, Level arg, boolean arg2, int arg3, Operation<Void> original, PistonMovingBlockEntity pistonMovingBlockEntity, float f, PoseStack poseStack, MultiBufferSource multiBufferSource, int i, int j/*? if >=1.21.5 {*//*, Vec3 vec3*//*?}*/) {
        BlockEntity be = ((LegacyPistonMovingBlockEntity)pistonMovingBlockEntity).getRenderingBlockEntity();
        BlockEntityRenderer<BlockEntity> beRenderer = be == null ? null : Minecraft.getInstance().getBlockEntityRenderDispatcher().getRenderer(be);
        if (be == null || beRenderer == null) {
            original.call(instance, pistonMovingBlockEntity.getBlockPos(), blockState, poseStack1, data, arg, arg2, arg3);
        } else if (LegacyOptions.enhancedPistonMovingRenderer.get()) {
            beRenderer.render(be, f, poseStack, multiBufferSource, i , j/*? if >=1.21.5 {*//*, vec3*//*?}*/);
        }
    }
}
