package wily.legacy.mixin;

import com.google.common.collect.Ordering;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.MobEffectTextureManager;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PlayerRideableJumping;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
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
import wily.legacy.Legacy4JClient;
import wily.legacy.client.BufferSourceWrapper;
import wily.legacy.client.LegacyOptions;
import wily.legacy.util.LegacySprites;
import wily.legacy.client.screen.ControlTooltip;
import wily.legacy.util.ScreenUtil;

import java.util.Collection;

@Mixin(Gui.class)

public abstract class GuiMixin {
    @Shadow @Final protected Minecraft minecraft;

    @Shadow protected int screenHeight;

    @Shadow public abstract Font getFont();

    @Shadow protected int screenWidth;

    @Shadow protected float autosaveIndicatorValue;

    @Shadow protected float lastAutosaveIndicatorValue;

    private int lastHotbarSelection = -1;
    private long animatedCharacterTime;
    private long remainingAnimatedCharacterTime;

    @Shadow protected abstract Player getCameraPlayer();

    @Shadow public abstract void render(GuiGraphics guiGraphics, float f);
    @Redirect(method = "renderSlot", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;getPopTime()I"))
    public int renderSlot(ItemStack instance) {
        return 0;
    }
    @Inject(method = "renderVignette", at = @At("HEAD"), cancellable = true)
    public void renderVignette(GuiGraphics guiGraphics, Entity entity, CallbackInfo ci) {
        if (minecraft.screen != null || !((LegacyOptions)minecraft.options).vignette().get())
            ci.cancel();
    }
    @Inject(method = "renderCrosshair", at = @At("HEAD"), cancellable = true)
    public void renderCrosshair(GuiGraphics guiGraphics, CallbackInfo ci) {
        if (minecraft.screen != null) {
            ci.cancel();
            return;
        }

        guiGraphics.pose().pushPose();
        ScreenUtil.applyHUDScale(guiGraphics,i-> screenWidth = i,i-> screenHeight = i);
    }
    @Redirect(method = "renderCrosshair", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;blendFuncSeparate(Lcom/mojang/blaze3d/platform/GlStateManager$SourceFactor;Lcom/mojang/blaze3d/platform/GlStateManager$DestFactor;Lcom/mojang/blaze3d/platform/GlStateManager$SourceFactor;Lcom/mojang/blaze3d/platform/GlStateManager$DestFactor;)V"))
    public void renderCrosshairBlendFunc(GlStateManager.SourceFactor sourceFactor, GlStateManager.DestFactor destFactor, GlStateManager.SourceFactor sourceFactor2, GlStateManager.DestFactor destFactor2, GuiGraphics guiGraphics) {
        if (((LegacyOptions)minecraft.options).hudOpacity().get() < 1.0) {
            guiGraphics.setColor(1.0f, 1.0f, 1.0f, ScreenUtil.getHUDOpacity());
            RenderSystem.enableBlend();
        } else RenderSystem.blendFuncSeparate(sourceFactor,destFactor,sourceFactor2,destFactor2);
    }
    @Inject(method = "renderCrosshair", at = @At("RETURN"))
    public void renderCrosshairReturn(GuiGraphics guiGraphics, CallbackInfo ci) {
        if (minecraft.screen != null)
            return;
        guiGraphics.setColor(1.0f,1.0f,1.0f,1.0f);
        ScreenUtil.resetHUDScale(guiGraphics,i-> screenWidth = i,i-> screenHeight = i);
        guiGraphics.pose().popPose();
    }
    @Inject(method = "renderEffects", at = @At("HEAD"), cancellable = true)
    public void renderEffects(GuiGraphics guiGraphics, CallbackInfo ci) {
        ci.cancel();
        Collection<MobEffectInstance> collection = this.minecraft.player.getActiveEffects();
        if (minecraft.screen != null || collection.isEmpty()) {
            return;
        }
        int i = 0;
        int j = 0;
        float backAlpha = ScreenUtil.getHUDOpacity();
        MobEffectTextureManager mobEffectTextureManager = this.minecraft.getMobEffectTextures();
        for (MobEffectInstance mobEffectInstance : Ordering.natural().reverse().sortedCopy(collection)) {
            MobEffect mobEffect = mobEffectInstance.getEffect();
            if (!mobEffectInstance.showIcon()) continue;
            int k = this.screenWidth - 55;
            int l = 18;
            if (this.minecraft.isDemo()) {
                l += 15;
            }
            if (mobEffect.isBeneficial()) {
                k -= 24 * ++i;
            } else {
                k -= 24 * ++j;
                l += 24;
            }
            float f = 1.0f;
            guiGraphics.setColor(1.0f, 1.0f, 1.0f, backAlpha);
            ScreenUtil.renderPointerPanel(guiGraphics, k, l, 24, 24);
            if (!mobEffectInstance.isAmbient() && mobEffectInstance.endsWithin(200)){
                int m = mobEffectInstance.getDuration();
                f = Mth.clamp((float)m / 10.0f / 5.0f * 0.5f, 0.0f, 0.5f) + Mth.cos((float)m * (float)Math.PI / 5.0f) * Mth.clamp((10 - m / 20) / 10.0f * 0.25f, 0.0f, 0.25f);
            }
            RenderSystem.enableBlend();
            TextureAtlasSprite textureAtlasSprite = mobEffectTextureManager.get(mobEffect);
            guiGraphics.setColor(1.0f, 1.0f, 1.0f, f * backAlpha);
            guiGraphics.blit(k + 3, l + 3, 0, 18, 18, textureAtlasSprite);
            RenderSystem.disableBlend();
        }
        guiGraphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);
    }
    @Inject(method = "renderHotbar", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;blitSprite(Lnet/minecraft/resources/ResourceLocation;IIII)V", ordinal = 1))
    private void renderHotbarSelection(float f, GuiGraphics guiGraphics, CallbackInfo ci) {
        guiGraphics.blitSprite(LegacySprites.HOTBAR_SELECTION,24,24,0,23,this.screenWidth / 2 - 91 - 1 + minecraft.player.getInventory().selected * 20, this.screenHeight, 24, 1);
    }
    @Inject(method = "renderHotbar", at = @At("HEAD"), cancellable = true)
    public void renderHotbar(float f, GuiGraphics guiGraphics, CallbackInfo ci) {
        if (minecraft.screen != null) {
            ci.cancel();
            return;
        }
        if (minecraft.getCameraEntity() instanceof LivingEntity character) {
            boolean hasRemainingTime = character.isSprinting() || character.isShiftKeyDown() || character.isCrouching() || character.isFallFlying();
            if (((LegacyOptions) minecraft.options).animatedCharacter().get() && (hasRemainingTime || character instanceof Player p && p.getAbilities().flying) && !character.isSleeping()) {
                animatedCharacterTime = Util.getMillis();
                remainingAnimatedCharacterTime = hasRemainingTime ? 450 : 0;
            }
            if (Util.getMillis() - animatedCharacterTime <= remainingAnimatedCharacterTime) {
                Vec3 deltaMove = character.getDeltaMovement();
                float bodyRotO = character.yBodyRotO;
                float bodyRot = character.yBodyRot;
                float xRot = character.getXRot();
                float xRotO = character.xRotO;
                float yRot = character.getYRot();
                float yRotO = character.yRotO;
                float yHeadRotO = character.yHeadRotO;
                float yHeadRot = character.yHeadRot;
                character.setDeltaMovement(new Vec3(Math.min(Math.max(-0.1, deltaMove.x), 0.1), 0, Math.min(0, Math.max(-0.1, deltaMove.z))));
                character.yBodyRotO = character.yBodyRot = 180.0f;
                character.setYRot(180);
                character.yRotO = character.getYRot();
                character.setXRot(character.xRotO = character.isFallFlying() ? xRot : 0);
                character.yHeadRot = 180 + (character.isFallFlying() ? 0 : yHeadRot - bodyRot);
                character.yHeadRotO = character.yHeadRot;
                guiGraphics.pose().pushPose();
                ScreenUtil.applyHUDScale(guiGraphics, w -> {
                }, h -> {
                });
                ScreenUtil.renderEntity(guiGraphics, 28f, 50f, 13, ScreenUtil.getLegacyOptions().smoothAnimatedCharacter().get() ? f : 0,new Vector3f(), new Quaternionf().rotationXYZ(0.0f, -0.43633232f, (float) Math.PI), null, character);
                guiGraphics.pose().popPose();
                character.setDeltaMovement(deltaMove);
                character.yBodyRotO = bodyRotO;
                character.yBodyRot = bodyRot;
                character.setXRot(xRot);
                character.xRotO = xRotO;
                character.setYRot(yRot);
                character.yRotO = yRotO;
                character.yHeadRot = yHeadRot;
                character.yHeadRotO = yHeadRotO;
            }
        }
        int newSelection = minecraft.player != null ? minecraft.player.getInventory().selected : -1;
        if (lastHotbarSelection >= 0 && lastHotbarSelection != newSelection) ScreenUtil.lastHotbarSelectionChange = Util.getMillis();
        lastHotbarSelection = newSelection;
        RenderSystem.enableBlend();
        guiGraphics.setColor(1.0f,1.0f,1.0f, ScreenUtil.getHUDOpacity());
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0.0F,  ScreenUtil.getHUDDistance(),0.0F);
        ScreenUtil.applyHUDScale(guiGraphics,i-> screenWidth = i,i-> screenHeight = i);
        Player player = this.getCameraPlayer();
        if (player == null) {
            return;
        }
        if (ScreenUtil.getHUDOpacity() < 1.0) {
            Legacy4JClient.guiBufferSourceOverride = BufferSourceWrapper.translucent(guiGraphics.bufferSource());
        }
    }

    @Inject(method = "renderHotbar", at = @At("RETURN"))
    public void renderHotbarTail(float f, GuiGraphics guiGraphics, CallbackInfo ci) {
        if (minecraft.screen != null)
            return;
        RenderSystem.disableBlend();
        Legacy4JClient.guiBufferSourceOverride = null;
        ScreenUtil.resetHUDScale(guiGraphics,i-> screenWidth = i,i-> screenHeight = i);
        guiGraphics.pose().popPose();
        guiGraphics.setColor(1.0f,1.0f,1.0f,1.0f);
        if (minecraft.player != null) ControlTooltip.guiControlRenderer.render(guiGraphics,0,0,f);
    }
    @ModifyVariable(method = "renderJumpMeter", at = @At(value="HEAD", ordinal = 0), argsOnly = true)
    public int modifyJumpMeterX(int value) {
        return (int) (screenWidth * ScreenUtil.getHUDScale() / 3 / 2 - 91);
    }

    @Inject(method = "renderJumpMeter", at = @At("HEAD"), cancellable = true)
    public void renderJumpMeter(PlayerRideableJumping playerRideableJumping, GuiGraphics guiGraphics, int i, CallbackInfo ci) {
        if (minecraft.screen != null){
            ci.cancel();
            return;
        }
        guiGraphics.setColor(1.0f,1.0f,1.0f, ScreenUtil.getHUDOpacity());
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
        return (int) (screenWidth * ScreenUtil.getHUDScale() / 3 / 2 - 91);
    }
    @Inject(method = "renderExperienceBar", at = @At("HEAD"), cancellable = true)
    public void renderExperienceBar(GuiGraphics guiGraphics, int i, CallbackInfo ci) {
        if (minecraft.screen != null) {
            ci.cancel();
            return;
        }
        guiGraphics.setColor(1.0f, 1.0f, 1.0f, ScreenUtil.getHUDOpacity());
        RenderSystem.enableBlend();
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
