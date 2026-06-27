package wily.legacy.mixin.base.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.client.LegacyChunkLoading;
import wily.legacy.client.LegacyRenderDistance;

@Mixin(BlockEntityRenderDispatcher.class)
public abstract class BlockEntityRenderDispatcherMixin {
    @Inject(method = "render(Lnet/minecraft/world/level/block/entity/BlockEntity;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;)V", at = @At("HEAD"), cancellable = true)
    private void render(BlockEntity blockEntity, float tickDelta, PoseStack poseStack, MultiBufferSource source, CallbackInfo ci) {
        if (blockEntity instanceof BeaconBlockEntity && !LegacyChunkLoading.isSectionVisible(blockEntity.getBlockPos())) ci.cancel();
        if (!LegacyRenderDistance.shouldRender(blockEntity, legacy$getCameraPosition())) ci.cancel();
    }

    @Unique
    private Vec3 legacy$getCameraPosition() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft == null || minecraft.gameRenderer == null ? null : minecraft.gameRenderer.getMainCamera().getPosition();
    }
}
