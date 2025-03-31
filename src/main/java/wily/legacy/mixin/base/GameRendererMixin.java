package wily.legacy.mixin.base;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.item.ItemStack;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.legacy.Legacy4JClient;
import wily.legacy.client.LegacyOptions;
import wily.legacy.entity.PlayerYBobbing;
import wily.legacy.util.ScreenUtil;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Consumer;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    @Shadow @Final
    Minecraft minecraft;

    @Shadow private boolean hasWorldScreenshot;

    @Shadow protected abstract void takeAutoScreenshot(Path path);

    @Shadow private ItemStack itemActivationItem;

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;flush()V"))
    private void render(CallbackInfo ci, @Local(ordinal = 0) GuiGraphics graphics){
        ScreenUtil.renderGameOverlay(graphics);
    }

    @Inject(method = "tick", at = @At("RETURN"))
    private void tick(CallbackInfo ci){
        if (itemActivationItem == null && Legacy4JClient.itemActivationRenderReplacement != null) Legacy4JClient.itemActivationRenderReplacement = null;
    }

    //? if >=1.20.5 && <1.21.2 {
    @Redirect(method = "renderItemActivationAnimation", at = @At(value = "NEW", target = "()Lcom/mojang/blaze3d/vertex/PoseStack;"))
    private PoseStack renderItemActivationAnimation(GuiGraphics graphics){
        return graphics.pose();
    }
    //?}

    //? if >=1.20.5 {
    @Inject(method = "renderItemActivationAnimation", at = @At(value = "INVOKE", target = /*? if <1.21.2 {*/"Lnet/minecraft/client/gui/GuiGraphics;drawManaged(Ljava/lang/Runnable;)V"/*?} else {*//*"Lnet/minecraft/client/gui/GuiGraphics;drawSpecial(Ljava/util/function/Consumer;)V"*//*?}*/), cancellable = true)
    private void renderItemActivationAnimation(GuiGraphics guiGraphics, float f, CallbackInfo ci){
        if (Legacy4JClient.itemActivationRenderReplacement != null){
            ci.cancel();
            Legacy4JClient.itemActivationRenderReplacement.render(guiGraphics,0,0,f);
            guiGraphics.pose().popPose();
        }
    }
    //?} else {
    /*@WrapWithCondition(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GameRenderer;renderItemActivationAnimation(IIF)V"))
    private boolean renderItemActivationAnimation(GameRenderer instance, int i, int j, float f){
        return Legacy4JClient.itemActivationRenderReplacement == null;
    }

    @Inject(method = "renderLevel", at = @At("HEAD"))
    private void applyFlyingViewRolling(float f, long l, PoseStack poseStack, CallbackInfo ci){
        float z = ScreenUtil.getFlyingViewRollingRotation(0);
        if (z != 0){
            poseStack.mulPose(Axis.ZN.rotation(z));
        }
    }
    *///?}
    @Inject(method = "bobView", at = @At("RETURN"))
    private void bobView(PoseStack poseStack, float f, CallbackInfo ci){
        if (this.minecraft.getCameraEntity() instanceof PlayerYBobbing p && !minecraft.player.getAbilities().flying) poseStack.mulPose(Axis.XP.rotationDegrees(p.getAngle(f)));
    }

    @Inject(method = "shouldRenderBlockOutline", at = @At("HEAD"), cancellable = true)
    private void renderLevel(CallbackInfoReturnable<Boolean> cir){
        if (!LegacyOptions.displayHUD.get()) cir.setReturnValue(false);
    }

    @ModifyExpressionValue(method = "renderLevel", at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/GameRenderer;renderHand:Z"))
    private boolean renderLevel(boolean original){
        return original && LegacyOptions.displayHand.get();
    }

    @ModifyExpressionValue(method = "tryTakeScreenshotIfNeeded",at = @At(value = "FIELD", opcode = Opcodes.GETFIELD, target = "Lnet/minecraft/client/renderer/GameRenderer;hasWorldScreenshot:Z"))
    private boolean canTakeWorldIcon(boolean original) {
        return original && !Legacy4JClient.retakeWorldIcon;
    }

    @Redirect(method = "tryTakeScreenshotIfNeeded",at = @At(value = "INVOKE", target = "Ljava/util/Optional;ifPresent(Ljava/util/function/Consumer;)V"))
    private void tryTakeScreenshotIfNeeded(Optional<Path> instance, Consumer<? super Path> action) {
        instance.ifPresent(path->{
                    if (!Legacy4JClient.retakeWorldIcon && Files.isRegularFile(path)) {
                        this.hasWorldScreenshot = true;
                    } else {
                        this.takeAutoScreenshot(path);
                        Legacy4JClient.retakeWorldIcon = false;
                    }
                }
        );
    }

    //? if <1.21.2 {
    @Inject(method = "resize",at = @At("RETURN"))
    public void resize(int i, int j, CallbackInfo ci) {
        if (Legacy4JClient.gammaEffect != null) Legacy4JClient.gammaEffect.resize(i,j);
    }
    //?}
}
