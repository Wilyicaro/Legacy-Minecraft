package wily.legacy.mixin.base.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import org.joml.Matrix4f;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.legacy.client.LegacyOptions;
import wily.legacy.client.LegacySaveCache;
import wily.legacy.entity.PlayerYBobbing;
import wily.legacy.util.client.LegacyRenderUtil;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Consumer;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    @Shadow @Final
    private Minecraft minecraft;

    @Shadow private boolean hasWorldScreenshot;

    @Shadow protected abstract void takeAutoScreenshot(Path path);

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/toasts/ToastManager;render(Lnet/minecraft/client/gui/GuiGraphics;)V", shift = At.Shift.AFTER))
    private void render(CallbackInfo ci, @Local(ordinal = 0) GuiGraphics graphics){
        LegacyRenderUtil.renderGameOverlay(graphics);
    }

    @Inject(method = "bobView", at = @At("RETURN"))
    private void bobView(PoseStack poseStack, float f, CallbackInfo ci){
        float xAngle = PlayerYBobbing.getAngle(minecraft, f);
        if (xAngle != 0) poseStack.mulPose(Axis.XP.rotationDegrees(xAngle));
    }

    @Inject(method = "shouldRenderBlockOutline", at = @At("HEAD"), cancellable = true)
    private void renderLevel(CallbackInfoReturnable<Boolean> cir){
        if (!LegacyOptions.displayHUD.get()) cir.setReturnValue(false);
    }

    @WrapWithCondition(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GameRenderer;renderItemInHand(FZLorg/joml/Matrix4f;)V"))
    private boolean renderLevel(GameRenderer instance, float matrix4fstack, boolean b, Matrix4f f){
        return LegacyOptions.displayHand.get();
    }

    @ModifyExpressionValue(method = "tryTakeScreenshotIfNeeded",at = @At(value = "FIELD", opcode = Opcodes.GETFIELD, target = "Lnet/minecraft/client/renderer/GameRenderer;hasWorldScreenshot:Z"))
    private boolean canTakeWorldIcon(boolean original) {
        return original && !LegacySaveCache.retakeWorldIcon;
    }

    @Redirect(method = "tryTakeScreenshotIfNeeded",at = @At(value = "INVOKE", target = "Ljava/util/Optional;ifPresent(Ljava/util/function/Consumer;)V"))
    private void tryTakeScreenshotIfNeeded(Optional<Path> instance, Consumer<? super Path> action) {
        instance.ifPresent(path->{
                    if (!LegacySaveCache.retakeWorldIcon && Files.isRegularFile(path)) {
                        this.hasWorldScreenshot = true;
                    } else {
                        this.takeAutoScreenshot(path);
                        LegacySaveCache.retakeWorldIcon = false;
                    }
                }
        );
    }
}
