package wily.legacy.mixin.base;

//? if >=1.21.2 {
import com.mojang.blaze3d.resource.CrossFrameResourcePool;
//?}
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
//? if >=1.21 {
import net.minecraft.client.DeltaTracker;
//?}
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.factoryapi.FactoryAPIClient;
import wily.factoryapi.base.client.FactoryGuiGraphics;
import wily.factoryapi.base.client.MinecraftAccessor;
import wily.legacy.Legacy4JClient;
import wily.legacy.client.LegacyOption;
import wily.legacy.entity.PlayerYBobbing;
import wily.legacy.util.LegacySprites;
import wily.legacy.client.LegacyTip;
import wily.legacy.client.LegacyTipManager;
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

    @Shadow @Final private Camera mainCamera;

    @Shadow private boolean renderHand;

    @Shadow private ItemStack itemActivationItem;

    //? if >=1.21.2 {
    @Shadow @Final private CrossFrameResourcePool resourcePool;
    //?}

    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;flush()V"))
    private void render(GuiGraphics graphics){
        if (!MinecraftAccessor.getInstance().hasGameLoaded()) return;
        LegacyTip tip = LegacyTipManager.getActualTip();
        if (!LegacyTipManager.tips.isEmpty() || tip != null) {
            if (tip == null) tip = LegacyTipManager.getUpdateTip();
            tip.setX(graphics.guiWidth() - tip.getWidth() - 30);
            tip.render(graphics,0,0,FactoryAPIClient.getPartialTick());
            if (tip.visibility == Toast.Visibility.HIDE) LegacyTipManager.getUpdateTip();
        }
        if (minecraft.options.showAutosaveIndicator().get().booleanValue() && (minecraft.gui.autosaveIndicatorValue > 0 || minecraft.gui.lastAutosaveIndicatorValue > 0) && Mth.clamp(Mth.lerp(FactoryAPIClient.getPartialTick(), minecraft.gui.lastAutosaveIndicatorValue, minecraft.gui.autosaveIndicatorValue), 0.0f, 1.0f) > 0.02) {
            RenderSystem.disableDepthTest();
            ScreenUtil.drawAutoSavingIcon(graphics, graphics.guiWidth() - 66, 44);
            RenderSystem.enableDepthTest();
        }
        if (GLFW.glfwGetInputMode(minecraft.getWindow().getWindow(),GLFW.GLFW_CURSOR) == GLFW.GLFW_CURSOR_HIDDEN && !Legacy4JClient.controllerManager.isCursorDisabled) {
            RenderSystem.disableDepthTest();
            RenderSystem.enableBlend();
            graphics.pose().pushPose();
            graphics.pose().translate(Legacy4JClient.controllerManager.getPointerX() + LegacyTipManager.getTipXDiff(), Legacy4JClient.controllerManager.getPointerY(), 4000);
            FactoryGuiGraphics.of(graphics).blitSprite(minecraft.getWindow().getScreenWidth() >= 1920 ? LegacySprites.POINTER : LegacySprites.SMALL_POINTER, -8, -8, 16, 16);
            graphics.pose().popPose();
            RenderSystem.disableBlend();
            RenderSystem.enableDepthTest();
        }
        PostChain gammaEffect = Legacy4JClient.getGammaEffect();
        if (gammaEffect != null && LegacyOption.displayLegacyGamma.get()) {
            float gamma = LegacyOption.legacyGamma.get().floatValue();
            graphics.flush();
            RenderSystem.enableBlend();
            RenderSystem.disableDepthTest();
            gammaEffect.passes.forEach(p-> p./*? if <1.21.2 {*//*getEffect*//*?} else {*/getShader/*?}*/().safeGetUniform("gamma").set(gamma >= 0.5f ? (gamma - 0.5f) * 1.12f + 1.08f : gamma * 0.96f + 0.6f));
            gammaEffect.process(/*? if <1.21.2 {*//*FactoryAPIClient.getPartialTick()*//*?} else {*/this.minecraft.getMainRenderTarget(), this.resourcePool/*?}*/);
            RenderSystem.enableDepthTest();
            RenderSystem.disableBlend();
        }
        graphics.flush();
    }
    @Inject(method = "tick", at = @At("RETURN"))
    private void tick(CallbackInfo ci){
        if (itemActivationItem == null && Legacy4JClient.itemActivationRenderReplacement != null) Legacy4JClient.itemActivationRenderReplacement = null;
    }
    //? if >=1.20.5 && <1.21.2 {
    /*@Redirect(method = "renderItemActivationAnimation", at = @At(value = "NEW", target = "()Lcom/mojang/blaze3d/vertex/PoseStack;"))
    private PoseStack renderItemActivationAnimation(GuiGraphics graphics){
        return graphics.pose();
    }
    *///?}
    //? if >=1.20.5 {
    @Inject(method = "renderItemActivationAnimation", at = @At(value = "INVOKE", target = /*? if <1.21.2 {*//*"Lnet/minecraft/client/gui/GuiGraphics;drawManaged(Ljava/lang/Runnable;)V"*//*?} else {*/"Lnet/minecraft/client/gui/GuiGraphics;drawSpecial(Ljava/util/function/Consumer;)V"/*?}*/), cancellable = true)
    private void renderItemActivationAnimation(GuiGraphics guiGraphics, float f, CallbackInfo ci){
        if (Legacy4JClient.itemActivationRenderReplacement != null){
            ci.cancel();
            Legacy4JClient.itemActivationRenderReplacement.render(guiGraphics,0,0,f);
            guiGraphics.pose().popPose();
        }
    }
    //?} else {

    /*@Shadow protected abstract void renderItemActivationAnimation(int par1, int par2, float par3);

    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GameRenderer;renderItemActivationAnimation(IIF)V"))
    private void renderItemActivationAnimation(GameRenderer instance, int i, int j, float f){
        if (Legacy4JClient.itemActivationRenderReplacement == null) renderItemActivationAnimation(i,j,f);
    }
    *///?}
    @Inject(method = "bobView", at = @At("RETURN"))
    private void bobView(PoseStack poseStack, float f, CallbackInfo ci){
        if (this.minecraft.getCameraEntity() instanceof PlayerYBobbing p && !minecraft.player.getAbilities().flying) poseStack.mulPose(Axis.XP.rotationDegrees(p.getAngle(f)));
    }
    @Inject(method = "shouldRenderBlockOutline", at = @At("HEAD"), cancellable = true)
    private void renderLevel(CallbackInfoReturnable<Boolean> cir){
        if (!LegacyOption.displayHUD.get()) cir.setReturnValue(false);
    }
    @Redirect(method = "renderLevel", at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/GameRenderer;renderHand:Z"))
    private boolean renderLevel(GameRenderer instance){
        return renderHand && LegacyOption.displayHand.get();
    }
    //? if >=1.20.5 {
    @ModifyVariable(method = "renderLevel", at = @At(value = "STORE", remap = false),ordinal = /*? if <1.21.2 {*//*1*//*?} else {*/2/*?}*/)
    private Matrix4f renderLevel(Matrix4f original){
        float rot = getFlyingViewRollingRotation();
        if (rot != 0) {
            return new Matrix4f().rotationZ(rot).rotateXYZ(mainCamera.getXRot() * 0.017453292F, mainCamera.getYRot() * 0.017453292F + 3.1415927F,0);
        }
        return original;
    }
    //?} else {
    /*@Inject(method = "renderLevel", at = @At("HEAD"))
    private void renderLevel(float f, long l, PoseStack poseStack, CallbackInfo ci){
        float rot = getFlyingViewRollingRotation();
        if (rot != 0) {
            poseStack.mulPose(Axis.ZP.rotation(rot));
        }
    }
    *///?}
    @Unique
    private float getFlyingViewRollingRotation(){
        if (LegacyOption.flyingViewRolling.get() && minecraft.player != null && minecraft.player.isFallFlying()){
            float f = FactoryAPIClient.getGamePartialTick(false);
            Vec3 vec3 =  minecraft.player.getViewVector(f);
            Vec3 vec32 =  minecraft.player.getDeltaMovementLerped(f);
            double d = vec32.horizontalDistanceSqr();
            double e = vec3.horizontalDistanceSqr();
            if (d > 0.0 && e > 0.0) {
                int dir = (int) Math.signum(vec32.x * vec3.z - vec32.z * vec3.x);
                float z = (float) (Math.min(Math.PI / 8, Math.acos((vec32.x * vec3.x + vec32.z * vec3.z) / Math.sqrt(d * e)) / 2.5));
                if (z > 0) return dir*z;
            }
        }
        return 0;
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
    //? if <1.21.2 {
    /*@Inject(method = "resize",at = @At("RETURN"))
    public void resize(int i, int j, CallbackInfo ci) {
        if (Legacy4JClient.gammaEffect != null) Legacy4JClient.gammaEffect.resize(i,j);
    }
    *///?}
}
