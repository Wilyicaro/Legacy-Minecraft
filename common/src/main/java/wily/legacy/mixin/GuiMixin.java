package wily.legacy.mixin;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.PlayerRideableJumping;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.LegacyMinecraft;
import wily.legacy.LegacyMinecraftClient;
import wily.legacy.client.LegacyOptions;
import wily.legacy.util.ScreenUtil;

@Mixin(Gui.class)

public abstract class GuiMixin {
    @Shadow @Final protected Minecraft minecraft;

    @Shadow @Final protected static ResourceLocation EXPERIENCE_BAR_BACKGROUND_SPRITE;

    @Shadow @Final protected static ResourceLocation EXPERIENCE_BAR_PROGRESS_SPRITE;

    @Shadow protected int screenHeight;

    @Shadow public abstract Font getFont();

    @Shadow protected int screenWidth;

    @Shadow protected float autosaveIndicatorValue;

    @Shadow protected float lastAutosaveIndicatorValue;

    @Shadow protected abstract Player getCameraPlayer();
    @Inject(method = "renderCrosshair", at = @At("HEAD"), cancellable = true)
    public void renderCrosshair(GuiGraphics guiGraphics, CallbackInfo ci) {
        if (minecraft.screen != null){
            ci.cancel();
            return;
        }
        ScreenUtil.applyHUDScale(guiGraphics,i-> screenWidth = i,i-> screenHeight = i);
    }
    @Redirect(method = "renderCrosshair", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;blendFuncSeparate(Lcom/mojang/blaze3d/platform/GlStateManager$SourceFactor;Lcom/mojang/blaze3d/platform/GlStateManager$DestFactor;Lcom/mojang/blaze3d/platform/GlStateManager$SourceFactor;Lcom/mojang/blaze3d/platform/GlStateManager$DestFactor;)V"))
    public void renderCrosshairBlendFunc(GlStateManager.SourceFactor sourceFactor, GlStateManager.DestFactor destFactor, GlStateManager.SourceFactor sourceFactor2, GlStateManager.DestFactor destFactor2, GuiGraphics guiGraphics) {
        if (ScreenUtil.getInterfaceOpacity() < 1.0) {
            guiGraphics.setColor(1.0f, 1.0f, 1.0f, ScreenUtil.getInterfaceOpacity());
            RenderSystem.enableBlend();
        } else RenderSystem.blendFuncSeparate(sourceFactor,destFactor,sourceFactor2,destFactor2);
    }
    @Inject(method = "renderCrosshair", at = @At("RETURN"))
    public void renderCrosshairReturn(GuiGraphics guiGraphics, CallbackInfo ci) {
        if (minecraft.screen != null)
            return;
        guiGraphics.setColor(1.0f,1.0f,1.0f,1.0f);
        ScreenUtil.resetHUDScale(guiGraphics,i-> screenWidth = i,i-> screenHeight = i);
    }
    @Inject(method = "renderHotbar", at = @At("HEAD"), cancellable = true)
    public void renderHotbar(float f, GuiGraphics guiGraphics, CallbackInfo ci) {
        if (minecraft.screen != null) {
            ci.cancel();
            return;
        }
        guiGraphics.setColor(1.0f,1.0f,1.0f, ScreenUtil.getInterfaceOpacity());
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0.0F,  ScreenUtil.getHUDDistance(),0.0F);
        ScreenUtil.applyHUDScale(guiGraphics,i-> screenWidth = i,i-> screenHeight = i);
        Player player = this.getCameraPlayer();
        if (player == null) {
            return;
        }
        guiGraphics.blitSprite(new ResourceLocation(LegacyMinecraft.MOD_ID,"hud/hotbar_selection"), this.screenWidth / 2 - 91 - 1 + player.getInventory().selected * 20, this.screenHeight - 22 - 1, 24, 24);
        if (ScreenUtil.getInterfaceOpacity() < 1.0) {
            LegacyMinecraftClient.itemRenderTypeOverride = Sheets.translucentItemSheet();
            LegacyMinecraftClient.blockItemRenderTypeOverride = Sheets.translucentCullBlockSheet();
        }
    }
    @Inject(method = "renderHotbar", at = @At("RETURN"))
    public void renderHotbarTail(float f, GuiGraphics guiGraphics, CallbackInfo ci) {
        if (minecraft.screen != null)
            return;

        LegacyMinecraftClient.itemRenderTypeOverride = null;
        LegacyMinecraftClient.blockItemRenderTypeOverride = null;
        ScreenUtil.resetHUDScale(guiGraphics,i-> screenWidth = i,i-> screenHeight = i);
        guiGraphics.pose().popPose();
        Player player = this.getCameraPlayer();
        guiGraphics.setColor(1.0f,1.0f,1.0f,1.0f);
        if (((LegacyOptions)minecraft.options).animatedCharacter().get() && (player.isSprinting() || player.isShiftKeyDown() || player.isCrouching() || player.getAbilities().flying || player.isFallFlying())) {
            Vec3 deltaMove = player.getDeltaMovement();
            float bodyRot = player.yBodyRot;
            float xRot = player.getXRot();
            float yRot = player.getYRot();
            float yRotO = player.yRotO;
            float yHeadRotO = player.yHeadRotO;
            float yHeadRot = player.yHeadRot;
            player.setDeltaMovement(new Vec3(Math.min(Math.max(-0.1,deltaMove.x),0.1),0,Math.min(0,Math.max(-0.1,deltaMove.z))));
            player.yBodyRot = 180.0f;
            player.setYRot(180);
            player.yRotO = player.getYRot();
            player.setXRot(player.isFallFlying() ? xRot : 0);
            player.yHeadRot = 180 + (player.isFallFlying() ? 0 : yHeadRot - bodyRot);
            player.yHeadRotO = player.yHeadRot;
            InventoryScreen.renderEntityInInventory(guiGraphics, 40, 60f, 15, new Vector3f(), new Quaternionf().rotationXYZ(0.0f, -0.43633232f, (float) Math.PI), null, player);
            player.setDeltaMovement(deltaMove);
            player.yBodyRot = bodyRot;
            player.setXRot(xRot);
            player.setYRot(yRot);
            player.yRotO = yRotO;
            player.yHeadRot = yHeadRot;
            player.yHeadRotO = yHeadRotO;
        }
    }
    @ModifyVariable(method = "renderJumpMeter", at = @At(value="HEAD", ordinal = 0), argsOnly = true)
    public int modifyJumpMeterX(int value) {
        return screenWidth * 2 / 3 / 2 - 91;
    }

    @Inject(method = "renderJumpMeter", at = @At("HEAD"), cancellable = true)
    public void renderJumpMeter(PlayerRideableJumping playerRideableJumping, GuiGraphics guiGraphics, int i, CallbackInfo ci) {
        if (minecraft.screen != null){
            ci.cancel();
            return;
        }
        guiGraphics.setColor(1.0f,1.0f,1.0f, ScreenUtil.getInterfaceOpacity());
        RenderSystem.enableBlend();
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0.0F,ScreenUtil.getHUDDistance(),0.0F);
        ScreenUtil.applyHUDScale(guiGraphics,w-> screenWidth = w,h-> screenHeight = h);
    }
    @Inject(method = "renderJumpMeter", at = @At("RETURN"))
    public void renderJumpMeterReturn(PlayerRideableJumping playerRideableJumping, GuiGraphics guiGraphics, int i, CallbackInfo ci) {
        if (minecraft.screen != null)
            return;
        RenderSystem.disableBlend();
        ScreenUtil.resetHUDScale(guiGraphics,w-> screenWidth = w,h-> screenHeight = h);
        guiGraphics.pose().popPose();
        guiGraphics.setColor(1.0f,1.0f,1.0f,1.0f);
    }
    @ModifyVariable(method = "renderExperienceBar", at = @At(value="HEAD", ordinal = 0), argsOnly = true)
    public int modifyExperienceBarX(int value) {
        return screenWidth * 2 / 3 / 2 - 91;
    }
    @Inject(method = "renderExperienceBar", at = @At("HEAD"), cancellable = true)
    public void renderExperienceBar(GuiGraphics guiGraphics, int i, CallbackInfo ci) {
        if (minecraft.screen != null) {
            ci.cancel();
            return;
        }
        guiGraphics.setColor(1.0f, 1.0f, 1.0f, ScreenUtil.getInterfaceOpacity());
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0.0F, ScreenUtil.getHUDDistance(), 0.0F);
        ScreenUtil.applyHUDScale(guiGraphics,w-> screenWidth = w,h-> screenHeight = h);
    }
    @Inject(method = "renderExperienceBar", at = @At("RETURN"))
    public void renderExperienceBarReturn(GuiGraphics guiGraphics, int i, CallbackInfo ci) {
        if (minecraft.screen != null)
            return;
        RenderSystem.disableBlend();
        ScreenUtil.resetHUDScale(guiGraphics,w-> screenWidth = w,h-> screenHeight = h);
        guiGraphics.pose().popPose();
        guiGraphics.setColor(1.0f,1.0f,1.0f,1.0f);
    }
    @Redirect(method = "renderExperienceBar", at = @At(value = "FIELD", target = "Lnet/minecraft/client/player/LocalPlayer;experienceLevel:I", ordinal = 0))
    public int renderExperienceBar(LocalPlayer instance, GuiGraphics guiGraphics) {
        if (instance.experienceLevel > 0) {
            this.minecraft.getProfiler().push("expLevel");
            String string = "" + instance.experienceLevel;
            ScreenUtil.drawOutlinedString(guiGraphics,getFont(), Component.literal(string),(this.screenWidth- this.getFont().width(string)) / 2,this.screenHeight - 39,8453920,0,4/3F);
            this.minecraft.getProfiler().pop();
        }
        return 0;
    }
    @Inject(method = "renderSavingIndicator", at = @At("HEAD"), cancellable = true)
    public void renderAutoSaveIndicator(GuiGraphics guiGraphics, CallbackInfo ci) {
        if (minecraft.options.showAutosaveIndicator().get().booleanValue() && (autosaveIndicatorValue > 0 || lastAutosaveIndicatorValue > 0) && Mth.clamp(Mth.lerp(this.minecraft.getFrameTime(), this.lastAutosaveIndicatorValue, this.autosaveIndicatorValue), 0.0f, 1.0f) > 0.02)
            ScreenUtil.drawAutoSavingIcon(guiGraphics,screenWidth - 66,44);
        ci.cancel();
    }

}
