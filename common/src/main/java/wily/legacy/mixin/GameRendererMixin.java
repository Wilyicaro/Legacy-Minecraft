package wily.legacy.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.lwjgl.glfw.GLFW;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;
import wily.legacy.Legacy4JClient;
import wily.legacy.client.LegacyOptions;
import wily.legacy.player.PlayerYBobbing;
import wily.legacy.util.LegacySprites;
import wily.legacy.client.LegacyTip;
import wily.legacy.client.LegacyTipManager;
import wily.legacy.util.ScreenUtil;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Consumer;

import static wily.legacy.Legacy4JClient.gammaEffect;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    @Shadow @Final
    Minecraft minecraft;

    @Shadow private boolean hasWorldScreenshot;

    @Shadow protected abstract void takeAutoScreenshot(Path path);

    @Shadow private boolean renderHand;

    @Shadow private ItemStack itemActivationItem;

    @Shadow protected abstract void renderItemActivationAnimation(int i, int j, float f);

    @Shadow private int itemActivationTicks;

    @Shadow private float itemActivationOffX;

    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;flush()V"))
    private void render(GuiGraphics graphics, float f){
        if (!minecraft.isGameLoadFinished()) return;
        LegacyTip tip = LegacyTipManager.getActualTip();
        if (!LegacyTipManager.tips.isEmpty() || tip != null) {
            if (tip == null) tip = LegacyTipManager.getUpdateTip();
            tip.setX(graphics.guiWidth() - tip.getWidth() - 30);
            tip.render(graphics,0,0,f);
            if (tip.visibility == Toast.Visibility.HIDE) LegacyTipManager.getUpdateTip();
        }
        if (minecraft.options.showAutosaveIndicator().get().booleanValue() && (minecraft.gui.autosaveIndicatorValue > 0 || minecraft.gui.lastAutosaveIndicatorValue > 0) && Mth.clamp(Mth.lerp(f, minecraft.gui.lastAutosaveIndicatorValue, minecraft.gui.autosaveIndicatorValue), 0.0f, 1.0f) > 0.02) {
            RenderSystem.disableDepthTest();
            ScreenUtil.drawAutoSavingIcon(graphics, graphics.guiWidth() - 66, 44);
            RenderSystem.enableDepthTest();
        }
        if (GLFW.glfwGetInputMode(minecraft.getWindow().getWindow(),GLFW.GLFW_CURSOR) == GLFW.GLFW_CURSOR_HIDDEN && !Legacy4JClient.controllerManager.isCursorDisabled) {
            RenderSystem.disableDepthTest();
            RenderSystem.enableBlend();
            graphics.pose().pushPose();
            graphics.pose().translate(Legacy4JClient.controllerManager.getPointerX() + LegacyTipManager.getTipXDiff(), Legacy4JClient.controllerManager.getPointerY(), 0);
            graphics.blitSprite(minecraft.getWindow().getScreenWidth() >= 1920 ? LegacySprites.POINTER : LegacySprites.SMALL_POINTER, -8, -8, 16, 16);
            graphics.pose().popPose();
            RenderSystem.disableBlend();
            RenderSystem.enableDepthTest();
        }
        if (gammaEffect != null && ScreenUtil.getLegacyOptions().displayLegacyGamma().get()) {
            float gamma = ScreenUtil.getLegacyOptions().legacyGamma().get().floatValue();
            graphics.flush();
            RenderSystem.enableBlend();
            RenderSystem.disableDepthTest();
            gammaEffect.passes.forEach(p-> p.getEffect().safeGetUniform("gamma").set(gamma >= 0.5f ? gamma * 1.7f : 0.35f + gamma));
            gammaEffect.process(this.minecraft.level != null && this.minecraft.level.tickRateManager().runsNormally() ? f : 1.0f);
            RenderSystem.enableDepthTest();
            RenderSystem.disableBlend();
        }
        graphics.flush();
    }
    @Inject(method = "tick", at = @At("RETURN"))
    private void tick(CallbackInfo ci){
        if (itemActivationItem == null && Legacy4JClient.itemActivationRenderReplacement != null) Legacy4JClient.itemActivationRenderReplacement = null;
    }
    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GameRenderer;renderItemActivationAnimation(IIF)V"))
    private void renderItemActivationAnimation(GameRenderer instance, int i, int j, float f){
        if (Legacy4JClient.itemActivationRenderReplacement == null) renderItemActivationAnimation(i,j,f);
    }
    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Gui;render(Lnet/minecraft/client/gui/GuiGraphics;F)V"))
    private void render(Gui instance, GuiGraphics guiGraphics, float f){
        if (this.itemActivationItem != null && this.itemActivationTicks > 0 && Legacy4JClient.itemActivationRenderReplacement != null) {
            float g = ((float)40 - this.itemActivationTicks + f) / 40.0F;
            float h = g * g;
            float l = g * h;
            float m = 10.25F * l * h - 24.95F * h * h + 25.5F * l - 13.8F * h + 4.0F * g;
            float n = m * 3.1415927F;
            float o = this.itemActivationOffX * (float)(guiGraphics.guiWidth() / 4);
            float p = this.itemActivationOffX * (float)(guiGraphics.guiHeight() / 4);
            RenderSystem.enableDepthTest();
            RenderSystem.disableCull();
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate((float)(guiGraphics.guiWidth() / 2) + o * Mth.abs(Mth.sin(n * 2.0F)), (float)(guiGraphics.guiHeight() / 2) + p * Mth.abs(Mth.sin(n * 2.0F)), -50.0F);
            float q = 50.0F + 175.0F * Mth.sin(n);
            guiGraphics.pose().scale(q, -q, q);
            guiGraphics.pose().mulPose(Axis.YP.rotationDegrees(900.0F * Mth.abs(Mth.sin(n))));
            guiGraphics.pose().mulPose(Axis.XP.rotationDegrees(6.0F * Mth.cos(g * 8.0F)));
            guiGraphics.pose().mulPose(Axis.ZP.rotationDegrees(6.0F * Mth.cos(g * 8.0F)));
            Legacy4JClient.itemActivationRenderReplacement.render(guiGraphics,0,0,f);
            guiGraphics.pose().popPose();
        }
        if (((LegacyOptions)minecraft.options).displayHUD().get()) instance.render(guiGraphics,f);
    }
    @Inject(method = "bobView", at = @At("RETURN"))
    private void bobView(PoseStack poseStack, float f, CallbackInfo ci){
        if (this.minecraft.getCameraEntity() instanceof PlayerYBobbing p && !minecraft.player.getAbilities().flying) poseStack.mulPose(Axis.XP.rotationDegrees(p.getAngle(f)));
    }
    @Inject(method = "shouldRenderBlockOutline", at = @At("HEAD"), cancellable = true)
    private void renderLevel(CallbackInfoReturnable<Boolean> cir){
        if (!((LegacyOptions)minecraft.options).displayHUD().get()) cir.setReturnValue(false);
    }
    @Redirect(method = "renderLevel", at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/GameRenderer;renderHand:Z"))
    private boolean renderLevel(GameRenderer instance){
        return renderHand && ((LegacyOptions)minecraft.options).displayHand().get();
    }
    @Inject(method = "renderLevel", at = @At("HEAD"))
    private void renderLevel(float f, long l, PoseStack poseStack, CallbackInfo ci){
        if (ScreenUtil.getLegacyOptions().flyingViewRolling().get() && minecraft.player != null && minecraft.player.isFallFlying()){
            Vec3 vec3 =  minecraft.player.getViewVector(f);
            Vec3 vec32 =  minecraft.player.getDeltaMovementLerped(f);
            double d = vec32.horizontalDistanceSqr();
            double e = vec3.horizontalDistanceSqr();
            if (d > 0.0 && e > 0.0) {
                int dir = (int) Math.signum(vec32.x * vec3.z - vec32.z * vec3.x);
                float z = (float) (Math.min(Math.PI / 8, Math.acos((vec32.x * vec3.x + vec32.z * vec3.z) / Math.sqrt(d * e)) / 2.5));
                if (z > 0) poseStack.mulPose(Axis.ZP.rotation(dir*z));
            }
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
