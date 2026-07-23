package wily.legacy.mixin.base.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.TntMinecartRenderer;
import net.minecraft.client.renderer.entity.state.MinecartTntRenderState;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.util.client.LegacyTntFlash;

@Mixin(TntMinecartRenderer.class)
public class TntMinecartRendererMixin {
    @ModifyVariable(method = "submitWhiteSolidBlock", at = @At("HEAD"), argsOnly = true)
    private static boolean legacy$removeOverlay(boolean flashing) {
        return false;
    }

    @Inject(method = "submitMinecartContents", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/TntMinecartRenderer;submitWhiteSolidBlock(Lnet/minecraft/world/level/block/state/BlockState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;IZI)V", shift = At.Shift.AFTER))
    private void legacy$submitFlash(MinecartTntRenderState state, BlockState blockState, PoseStack poseStack, SubmitNodeCollector collector, int light, CallbackInfo ci) {
        LegacyTntFlash.submit(poseStack, collector, state.fuseRemainingInTicks, false);
    }
}
