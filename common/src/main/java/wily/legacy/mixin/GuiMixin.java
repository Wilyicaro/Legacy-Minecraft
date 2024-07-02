package wily.legacy.mixin;

import com.google.common.collect.Ordering;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.Util;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.MobEffectTextureManager;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PlayerRideableJumping;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
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
    @Shadow
    private ItemStack lastToolHighlight;
    @Shadow
    public int toolHighlightTimer;

    @Shadow public abstract Font getFont();

    @Shadow protected float autosaveIndicatorValue;

    @Shadow protected float lastAutosaveIndicatorValue;

    private int lastHotbarSelection = -1;
    private long animatedCharacterTime;
    private long remainingAnimatedCharacterTime;

    @Shadow protected abstract Player getCameraPlayer();

    @Shadow public abstract void render(GuiGraphics guiGraphics, DeltaTracker tracker);

    @Shadow protected abstract boolean isExperienceBarVisible();

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
    public void renderCrosshair(GuiGraphics guiGraphics, DeltaTracker tracker, CallbackInfo ci) {
        if (minecraft.screen != null) {
            ci.cancel();
            return;
        }

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(guiGraphics.guiWidth()/2f,guiGraphics.guiHeight()/2f,0);
        ScreenUtil.applyHUDScale(guiGraphics);
        guiGraphics.pose().translate(-guiGraphics.guiWidth()/2,-guiGraphics.guiHeight()/2,0);
    }
    @Redirect(method = "renderCrosshair", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;blendFuncSeparate(Lcom/mojang/blaze3d/platform/GlStateManager$SourceFactor;Lcom/mojang/blaze3d/platform/GlStateManager$DestFactor;Lcom/mojang/blaze3d/platform/GlStateManager$SourceFactor;Lcom/mojang/blaze3d/platform/GlStateManager$DestFactor;)V"))
    public void renderCrosshairBlendFunc(GlStateManager.SourceFactor sourceFactor, GlStateManager.DestFactor destFactor, GlStateManager.SourceFactor sourceFactor2, GlStateManager.DestFactor destFactor2, GuiGraphics guiGraphics) {
        if (((LegacyOptions)minecraft.options).hudOpacity().get() < 1.0) {
            guiGraphics.setColor(1.0f, 1.0f, 1.0f, ScreenUtil.getHUDOpacity());
            RenderSystem.enableBlend();
        } else RenderSystem.blendFuncSeparate(sourceFactor,destFactor,sourceFactor2,destFactor2);
    }
    @Inject(method = "renderCrosshair", at = @At("RETURN"))
    public void renderCrosshairReturn(GuiGraphics guiGraphics, DeltaTracker tracker, CallbackInfo ci) {
        if (minecraft.screen != null)
            return;
        guiGraphics.setColor(1.0f,1.0f,1.0f,1.0f);
        guiGraphics.pose().popPose();
    }
    @Inject(method = "renderEffects", at = @At("HEAD"), cancellable = true)
    public void renderEffects(GuiGraphics guiGraphics,DeltaTracker tracker, CallbackInfo ci) {
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
            Holder<MobEffect> mobEffect = mobEffectInstance.getEffect();
            if (!mobEffectInstance.showIcon()) continue;
            int k = guiGraphics.guiWidth() - 55;
            int l = 18;
            if (this.minecraft.isDemo()) {
                l += 15;
            }
            if (mobEffect.value().isBeneficial()) {
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
    @Inject(method = "renderItemHotbar", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;blitSprite(Lnet/minecraft/resources/ResourceLocation;IIII)V", ordinal = 1))
    private void renderHotbarSelection(GuiGraphics guiGraphics, DeltaTracker tracker, CallbackInfo ci) {
        guiGraphics.blitSprite(LegacySprites.HOTBAR_SELECTION,24,24,0,23,guiGraphics.guiWidth() / 2 - 91 - 1 + minecraft.player.getInventory().selected * 20, guiGraphics.guiHeight(), 24, 1);
    }
    @Inject(method = "renderItemHotbar", at = @At("HEAD"), cancellable = true)
    public void renderHotbar(GuiGraphics guiGraphics, DeltaTracker tracker, CallbackInfo ci) {
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
                ScreenUtil.applyHUDScale(guiGraphics);
                ScreenUtil.renderEntity(guiGraphics, 28f, 50f, 13, ScreenUtil.getLegacyOptions().smoothAnimatedCharacter().get() ? tracker.getGameTimeDeltaPartialTick(true) : 0,new Vector3f(), new Quaternionf().rotationXYZ(0.0f, -0.43633232f, (float) Math.PI), null, character);
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
        ScreenUtil.prepareHUDRender(guiGraphics);
        guiGraphics.pose().translate(guiGraphics.guiWidth()/2f, guiGraphics.guiHeight(),0.0F);
        ScreenUtil.applyHUDScale(guiGraphics);
        guiGraphics.pose().translate(-guiGraphics.guiWidth()/2,-guiGraphics.guiHeight(),0);
        Player player = this.getCameraPlayer();
        if (player == null) {
            return;
        }
        if (ScreenUtil.getHUDOpacity() < 1.0) {
            Legacy4JClient.guiBufferSourceOverride = BufferSourceWrapper.translucent(guiGraphics.bufferSource());
        }
    }

    @Inject(method = "renderItemHotbar", at = @At("RETURN"))
    public void renderHotbarTail(GuiGraphics guiGraphics, DeltaTracker tracker, CallbackInfo ci) {
        if (minecraft.screen != null) return;

        Legacy4JClient.guiBufferSourceOverride = null;
        ScreenUtil.finishHUDRender(guiGraphics);
        if (minecraft.player != null) ControlTooltip.guiControlRenderer.render(guiGraphics,0,0,tracker.getGameTimeDeltaPartialTick(true));
    }

    @Inject(method = "renderExperienceLevel", at = @At("HEAD"), cancellable = true)
    public void renderExperienceLevel(GuiGraphics guiGraphics, DeltaTracker tracker, CallbackInfo ci) {
        ci.cancel();
        if (minecraft.screen != null) return;
        int i = this.minecraft.player.experienceLevel;
        if (this.isExperienceBarVisible() && i > 0) {
            ScreenUtil.prepareHUDRender(guiGraphics);
            guiGraphics.pose().translate(guiGraphics.guiWidth() /2f, guiGraphics.guiHeight(),0);
            ScreenUtil.applyHUDScale(guiGraphics);
            this.minecraft.getProfiler().push("expLevel");
            String exp = "" + i;
            ScreenUtil.drawOutlinedString(guiGraphics,getFont(), Component.literal(exp),-this.getFont().width(exp) / 2,-39,8453920,0,4/3F);
            this.minecraft.getProfiler().pop();
            ScreenUtil.finishHUDRender(guiGraphics);
        }
    }
    @Inject(method = "renderOverlayMessage", at = @At(value = "HEAD"), cancellable = true)
    public void renderOverlayMessage(GuiGraphics guiGraphics, DeltaTracker tracker, CallbackInfo ci) {
        if (minecraft.screen != null){
            ci.cancel();
            return;
        }
        ScreenUtil.prepareHUDRender(guiGraphics);
        guiGraphics.pose().translate(0, 63 - ScreenUtil.getHUDSize() - (this.lastToolHighlight.isEmpty() || this.toolHighlightTimer <= 0 ? 0 : (Math.min(4,lastToolHighlight.getTooltipLines(Item.TooltipContext.of(minecraft.level),minecraft.player, TooltipFlag.NORMAL).stream().filter(c->!c.getString().isEmpty()).mapToInt(c->1).sum()) - 1) * 9),0);
    }
    @Inject(method = "renderOverlayMessage", at = @At(value = "RETURN"))
    public void renderOverlayMessageReturn(GuiGraphics guiGraphics, DeltaTracker tracker, CallbackInfo ci) {
        if (minecraft.screen != null) return;

        ScreenUtil.finishHUDRender(guiGraphics);
    }
    @Inject(method = {"renderVehicleHealth","renderPlayerHealth"}, at = @At("HEAD"), cancellable = true)
    public void renderHealth(GuiGraphics guiGraphics, CallbackInfo ci) {
        if (minecraft.screen != null){
            ci.cancel();
            return;
        }
        ScreenUtil.prepareHUDRender(guiGraphics);
        guiGraphics.pose().translate(guiGraphics.guiWidth() / 2f, guiGraphics.guiHeight(),0);
        ScreenUtil.applyHUDScale(guiGraphics);
        guiGraphics.pose().translate(-guiGraphics.guiWidth() / 2, -guiGraphics.guiHeight(),0);
    }
    @Inject(method = {"renderVehicleHealth","renderPlayerHealth"}, at = @At("RETURN"))
    public void renderHealthReturn(GuiGraphics guiGraphics, CallbackInfo ci) {
        if (minecraft.screen != null) return;
        ScreenUtil.finishHUDRender(guiGraphics);
    }
    @Inject(method = "renderExperienceBar", at = @At("HEAD"), cancellable = true)
    public void renderExperienceBar(GuiGraphics guiGraphics, int i, CallbackInfo ci) {
        if (minecraft.screen != null){
            ci.cancel();
            return;
        }
        ScreenUtil.prepareHUDRender(guiGraphics);
        guiGraphics.pose().translate(guiGraphics.guiWidth() / 2f, guiGraphics.guiHeight(),0);
        ScreenUtil.applyHUDScale(guiGraphics);
        guiGraphics.pose().translate(-guiGraphics.guiWidth() / 2, -guiGraphics.guiHeight(),0);
    }
    @Inject(method = "renderExperienceBar", at = @At("RETURN"))
    public void renderExperienceBarReturn(GuiGraphics guiGraphics, int i, CallbackInfo ci) {
        if (minecraft.screen != null) return;
        ScreenUtil.finishHUDRender(guiGraphics);
    }
    @Inject(method = "renderJumpMeter", at = @At("HEAD"), cancellable = true)
    public void renderJumpMeter(PlayerRideableJumping playerRideableJumping, GuiGraphics guiGraphics, int i, CallbackInfo ci) {
        if (minecraft.screen != null){
            ci.cancel();
            return;
        }
        ScreenUtil.prepareHUDRender(guiGraphics);
        guiGraphics.pose().translate(guiGraphics.guiWidth() / 2f, guiGraphics.guiHeight(),0);
        ScreenUtil.applyHUDScale(guiGraphics);
        guiGraphics.pose().translate(-guiGraphics.guiWidth() / 2, -guiGraphics.guiHeight(),0);
    }
    @Inject(method = "renderJumpMeter", at = @At("RETURN"))
    public void renderJumpMeterReturn(PlayerRideableJumping playerRideableJumping, GuiGraphics guiGraphics, int i, CallbackInfo ci) {
        if (minecraft.screen != null) return;
        ScreenUtil.finishHUDRender(guiGraphics);
    }

    @Inject(method = "renderSavingIndicator", at = @At("HEAD"), cancellable = true)
    public void renderAutoSaveIndicator(GuiGraphics guiGraphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        if (minecraft.options.showAutosaveIndicator().get().booleanValue() && (autosaveIndicatorValue > 0 || lastAutosaveIndicatorValue > 0) && Mth.clamp(Mth.lerp(deltaTracker.getGameTimeDeltaTicks(), this.lastAutosaveIndicatorValue, this.autosaveIndicatorValue), 0.0f, 1.0f) > 0.02)
            ScreenUtil.drawAutoSavingIcon(guiGraphics,guiGraphics.guiWidth() - 66,44);
        ci.cancel();
    }

}
