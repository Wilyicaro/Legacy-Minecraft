package wily.legacy.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.PlayerRideableJumping;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.LegacyMinecraft;
import wily.legacy.init.LegacyOptions;
import wily.legacy.util.ScreenUtil;

import java.util.List;

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

    @Shadow protected int toolHighlightTimer;

    @Inject(method = "renderHotbar", at = @At("HEAD"), cancellable = true)
    public void renderHotbar(float f, GuiGraphics guiGraphics, CallbackInfo ci) {
        if (minecraft.screen != null) {
            ci.cancel();
            return;
        }
        guiGraphics.setColor(1.0f,1.0f,1.0f, (float) Math.max(Math.min(255f,toolHighlightTimer * 38.4f)/ 255f, ((LegacyOptions)minecraft.options).hudOpacity().get()));
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0.0F,  ((LegacyOptions)minecraft.options).hudDistance().get() * -22.5F,0.0F);
        Player player = this.getCameraPlayer();
        if (player == null) {
            return;
        }
        guiGraphics.blitSprite(new ResourceLocation(LegacyMinecraft.MOD_ID,"hud/hotbar_selection"), this.screenWidth / 2 - 91 - 1 + player.getInventory().selected * 20, this.screenHeight - 22 - 1, 24, 24);
    }
    @Inject(method = "renderHotbar", at = @At("RETURN"))
    public void renderHotbarTail(float f, GuiGraphics guiGraphics, CallbackInfo ci) {
        if (minecraft.screen != null)
            return;
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
            InventoryScreen.renderEntityInInventory(guiGraphics, 40, 80, 15, new Vector3f(), new Quaternionf().rotationXYZ(0.0f, -0.43633232f, (float) Math.PI), null, player);
            player.setDeltaMovement(deltaMove);
            player.yBodyRot = bodyRot;
            player.setXRot(xRot);
            player.setYRot(yRot);
            player.yRotO = yRotO;
            player.yHeadRot = yHeadRot;
            player.yHeadRotO = yHeadRotO;
        }
        guiGraphics.pose().popPose();
    }

    @Inject(method = "renderJumpMeter", at = @At("HEAD"), cancellable = true)
    public void renderJumpMeter(PlayerRideableJumping playerRideableJumping, GuiGraphics guiGraphics, int i, CallbackInfo ci) {
        if (minecraft.screen != null){
            ci.cancel();
            return;
        }
        guiGraphics.setColor(1.0f,1.0f,1.0f, (float) Math.max(Math.min(255f,toolHighlightTimer * 38.4f)/ 255f, ((LegacyOptions)minecraft.options).hudOpacity().get()));
        RenderSystem.enableBlend();
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0.0F,((LegacyOptions)minecraft.options).hudDistance().value *-22.5F,0.0F);
    }
    @Inject(method = "renderJumpMeter", at = @At("RETURN"))
    public void renderJumpMeterReturn(PlayerRideableJumping playerRideableJumping, GuiGraphics guiGraphics, int i, CallbackInfo ci) {
        if (minecraft.screen != null)
            return;
        RenderSystem.disableBlend();
        guiGraphics.pose().popPose();
        guiGraphics.setColor(1.0f,1.0f,1.0f,1.0f);
    }
    @Inject(method = "renderExperienceBar", at = @At("HEAD"), cancellable = true)
    public void renderExperienceBar(GuiGraphics guiGraphics, int i, CallbackInfo ci) {
        if (minecraft.screen != null){
            ci.cancel();
            return;
        }
        guiGraphics.setColor(1.0f,1.0f,1.0f, (float) Math.max(Math.min(255f,toolHighlightTimer * 38.4f)/ 255f, ((LegacyOptions)minecraft.options).hudOpacity().get()));
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0.0F,((LegacyOptions)minecraft.options).hudDistance().value *-22.5F,0.0F);
        int m;
        int l;
        RenderSystem.enableBlend();
        this.minecraft.getProfiler().push("expBar");
        int j = this.minecraft.player.getXpNeededForNextLevel();
        if (j > 0) {
            l = (int)(this.minecraft.player.experienceProgress * 183.0f);
            m = this.screenHeight - 32 + 3;
            guiGraphics.blitSprite(EXPERIENCE_BAR_BACKGROUND_SPRITE, i, m, 182, 5);
            if (l > 0) {
                guiGraphics.blitSprite(EXPERIENCE_BAR_PROGRESS_SPRITE, 182, 5, 0, 0, i, m, l, 5);
            }
        }
        this.minecraft.getProfiler().pop();
        if (this.minecraft.player.experienceLevel > 0) {
            this.minecraft.getProfiler().push("expLevel");
            String string = "" + this.minecraft.player.experienceLevel;
            l = (this.screenWidth- this.getFont().width(string)) / 2;
            m = this.screenHeight - 39;
            ScreenUtil.drawOutlinedString(guiGraphics,getFont(), Component.literal(string),l,m,8453920,0,4/3F);
            this.minecraft.getProfiler().pop();
        }
        RenderSystem.disableBlend();
        guiGraphics.pose().popPose();
        guiGraphics.setColor(1.0f,1.0f,1.0f,1.0f);
        ci.cancel();
    }
    @Inject(method = "renderSavingIndicator", at = @At("HEAD"), cancellable = true)
    public void renderAutoSaveIndicator(GuiGraphics guiGraphics, CallbackInfo ci) {
        if (minecraft.options.showAutosaveIndicator().get().booleanValue() && (autosaveIndicatorValue > 0 || lastAutosaveIndicatorValue > 0) && Mth.clamp(Mth.lerp(this.minecraft.getFrameTime(), this.lastAutosaveIndicatorValue, this.autosaveIndicatorValue), 0.0f, 1.0f) > 0.02)
            ScreenUtil.drawAutoSavingIcon(guiGraphics,screenWidth - 66,44);
        ci.cancel();
    }

}
