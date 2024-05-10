package wily.legacy.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastComponent;
import net.minecraft.client.renderer.GameRenderer;
import org.lwjgl.glfw.GLFW;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.Legacy4JClient;
import wily.legacy.client.LegacyOptions;
import wily.legacy.util.LegacySprites;
import wily.legacy.client.LegacyTip;
import wily.legacy.client.LegacyTipManager;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Consumer;

import static wily.legacy.Legacy4JClient.gammaEffect;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    @Shadow @Final private Minecraft minecraft;

    @Shadow private boolean hasWorldScreenshot;

    @Shadow protected abstract void takeAutoScreenshot(Path path);

    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/toasts/ToastComponent;render(Lnet/minecraft/client/gui/GuiGraphics;)V"))
    private void render(ToastComponent instance, GuiGraphics graphics, float f, long l, boolean bl){
        if (!LegacyTipManager.tips.isEmpty()) {
            LegacyTip tip = LegacyTipManager.tips.get(0);
            tip.setX(graphics.guiWidth() - tip.getWidth() - 30);
            tip.renderTip(graphics,0,0,f);
            if (tip.visibility == Toast.Visibility.HIDE) LegacyTipManager.tips.remove(tip);
        }
        instance.render(graphics);
        if (GLFW.glfwGetInputMode(minecraft.getWindow().getWindow(),GLFW.GLFW_CURSOR) == GLFW.GLFW_CURSOR_HIDDEN && !Legacy4JClient.controllerManager.isCursorDisabled) {
            RenderSystem.disableDepthTest();
            RenderSystem.enableBlend();
            graphics.pose().pushPose();
            graphics.pose().translate(Legacy4JClient.controllerManager.getPointerX(), Legacy4JClient.controllerManager.getPointerY(), 0);
            graphics.blitSprite(minecraft.getWindow().getScreenWidth() >= 1920 ? LegacySprites.POINTER : LegacySprites.SMALL_POINTER, -8, -8, 16, 16);
            graphics.pose().popPose();
            RenderSystem.disableBlend();
            RenderSystem.enableDepthTest();
        }
        float gamma = ((LegacyOptions)minecraft.options).legacyGamma().get().floatValue();
        if (gammaEffect != null && gamma != 0.5) {
            graphics.flush();
            RenderSystem.enableBlend();
            RenderSystem.disableDepthTest();
            gammaEffect.passes.forEach(p-> p.getEffect().safeGetUniform("gamma").set(gamma >= 0.5f ? gamma * 1.7f : 0.5f + gamma));
            gammaEffect.process(this.minecraft.level != null && this.minecraft.level.tickRateManager().runsNormally() ? f : 1.0f);
            RenderSystem.enableDepthTest();
            RenderSystem.disableBlend();
        }
    }
    @Redirect(method = "tryTakeScreenshotIfNeeded",at = @At(value = "FIELD", opcode = Opcodes.GETFIELD, target = "Lnet/minecraft/client/renderer/GameRenderer;hasWorldScreenshot:Z"))
    private boolean canTakeWorldIcon(GameRenderer instance) {
        return hasWorldScreenshot && !Legacy4JClient.retakeWorldIcon;
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
    @Inject(method = "resize",at = @At("RETURN"))
    public void resize(int i, int j, CallbackInfo ci) {
        if (gammaEffect != null) gammaEffect.resize(i,j);

    }
}
