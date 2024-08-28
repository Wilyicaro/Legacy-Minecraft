package wily.legacy.mixin;

import com.google.common.collect.Ordering;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.MobEffectTextureManager;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PlayerRideableJumping;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Score;
import net.minecraft.world.scores.Scoreboard;
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

import java.util.*;

import static net.minecraft.client.gui.components.AbstractWidget.WIDGETS_LOCATION;

@Mixin(Gui.class)

public abstract class GuiMixin implements ControlTooltip.Event {
    @Shadow @Final protected Minecraft minecraft;
    @Final

    @Shadow public abstract Font getFont();

    private int lastHotbarSelection = -1;
    private long animatedCharacterTime;
    private long remainingAnimatedCharacterTime;

    @Shadow protected abstract Player getCameraPlayer();

    @Shadow public abstract void render(PoseStack poseStack, float f);

    @Redirect(method = "renderSlot", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;getPopTime()I"))
    public int renderSlot(ItemStack instance) {
        return 0;
    }
    @Inject(method = "renderVignette", at = @At("HEAD"), cancellable = true)
    public void renderVignette(PoseStack poseStack, Entity entity, CallbackInfo ci) {
        if (minecraft.screen != null || !((LegacyOptions)minecraft.options).vignette().get())
            ci.cancel();
    }
    @Inject(method = "renderCrosshair", at = @At("HEAD"), cancellable = true)
    public void renderCrosshair(PoseStack poseStack, CallbackInfo ci) {
        if (minecraft.screen != null) {
            ci.cancel();
            return;
        }

        poseStack.pose().pushPose();
        poseStack.pose().translate(poseStack.guiWidth()/2f,poseStack.guiHeight()/2f,0);
        ScreenUtil.applyHUDScale(poseStack);
        poseStack.pose().translate(-poseStack.guiWidth()/2,-poseStack.guiHeight()/2,0);
    }
    @Redirect(method = "renderCrosshair", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;blendFuncSeparate(Lcom/mojang/blaze3d/platform/GlStateManager$SourceFactor;Lcom/mojang/blaze3d/platform/GlStateManager$DestFactor;Lcom/mojang/blaze3d/platform/GlStateManager$SourceFactor;Lcom/mojang/blaze3d/platform/GlStateManager$DestFactor;)V"))
    public void renderCrosshairBlendFunc(GlStateManager.SourceFactor sourceFactor, GlStateManager.DestFactor destFactor, GlStateManager.SourceFactor sourceFactor2, GlStateManager.DestFactor destFactor2, PoseStack poseStack) {
        if (((LegacyOptions)minecraft.options).hudOpacity().get() < 1.0) {
            poseStack.setColor(1.0f, 1.0f, 1.0f, ScreenUtil.getHUDOpacity());
            RenderSystem.enableBlend();
        } else RenderSystem.blendFuncSeparate(sourceFactor,destFactor,sourceFactor2,destFactor2);
    }
    @Inject(method = "renderCrosshair", at = @At("RETURN"))
    public void renderCrosshairReturn(PoseStack poseStack, CallbackInfo ci) {
        if (minecraft.screen != null)
            return;
        poseStack.setColor(1.0f,1.0f,1.0f,1.0f);
        poseStack.pose().popPose();
    }
    @Inject(method = "renderEffects", at = @At("HEAD"), cancellable = true)
    public void renderEffects(PoseStack poseStack, CallbackInfo ci) {
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
            int k = poseStack.guiWidth() - 55;
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
            poseStack.setColor(1.0f, 1.0f, 1.0f, backAlpha);
            ScreenUtil.renderPointerPanel(poseStack, k, l, 24, 24);
            if (mobEffectInstance.endsWithin(200)){
                int m = mobEffectInstance.getDuration();
                f = Mth.clamp((float)m / 10.0f / 5.0f * 0.5f, 0.0f, 0.5f) + Mth.cos((float)m * (float)Math.PI / 5.0f) * Mth.clamp((10 - m / 20) / 10.0f * 0.25f, 0.0f, 0.25f);
            }
            RenderSystem.enableBlend();
            TextureAtlasSprite textureAtlasSprite = mobEffectTextureManager.get(mobEffect);
            poseStack.setColor(1.0f, 1.0f, 1.0f, f * backAlpha);
            poseStack.blit(k + 3, l + 3, 0, 18, 18, textureAtlasSprite);
            RenderSystem.disableBlend();
        }
        poseStack.setColor(1.0f, 1.0f, 1.0f, 1.0f);
    }
    @Redirect(method = "renderHotbar", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;blit(Lnet/minecraft/resources/ResourceLocation;IIIIII)V", ordinal = 1))
    private void renderHotbarSelection(PoseStack instance, ResourceLocation resourceLocation, int i, int j, int k, int l, int m, int n) {
        instance.blit(resourceLocation, i, j, k, l, m, 24);
    }
    @Inject(method = "renderHotbar", at = @At("HEAD"), cancellable = true)
    public void renderHotbar(float f, PoseStack poseStack, CallbackInfo ci) {
        if (minecraft.screen != null) {
            ci.cancel();
            return;
        }
        if (minecraft.getCameraEntity() instanceof LivingEntity character) {
            boolean hasRemainingTime = character.isSprinting() || character.isCrouching() || character.isFallFlying() || character.isVisuallySwimming() || !(character instanceof Player);
            if (((LegacyOptions) minecraft.options).animatedCharacter().get() && (hasRemainingTime || character instanceof Player p && p.getAbilities().flying) && !character.isSleeping()) {
                animatedCharacterTime = Util.getMillis();
                remainingAnimatedCharacterTime = hasRemainingTime ? 450 : 0;
            }
            if (Util.getMillis() - animatedCharacterTime <= remainingAnimatedCharacterTime) {
                float xRot = character.getXRot();
                float xRotO = character.xRotO;
                if (!character.isFallFlying()) character.setXRot(character.xRotO = 0);
                poseStack.pose().pushPose();
                ScreenUtil.applyHUDScale(poseStack);
                f = ScreenUtil.getLegacyOptions().smoothAnimatedCharacter().get() ? f : 0;
                ScreenUtil.renderEntity(poseStack, 28f, 50f, 13, f,new Vector3f(), new Quaternionf().rotationXYZ(0, -(Mth.lerp(f, character.yBodyRotO, character.yBodyRot) * (float)(Math.PI/180f)) + (float) (Math.PI * 7/8f), (float) Math.PI), null, character);
                poseStack.pose().popPose();
                character.setXRot(xRot);
                character.xRotO = xRotO;
            }
        }
        int newSelection = minecraft.player != null ? minecraft.player.getInventory().selected : -1;
        if (lastHotbarSelection >= 0 && lastHotbarSelection != newSelection) ScreenUtil.lastHotbarSelectionChange = Util.getMillis();
        lastHotbarSelection = newSelection;
        ScreenUtil.prepareHUDRender(poseStack);
        poseStack.pose().translate(poseStack.guiWidth()/2f, poseStack.guiHeight(),0.0F);
        ScreenUtil.applyHUDScale(poseStack);
        poseStack.pose().translate(-poseStack.guiWidth()/2,-poseStack.guiHeight(),0);
        Player player = this.getCameraPlayer();
        if (player == null) {
            return;
        }
        if (ScreenUtil.getHUDOpacity() < 1.0) {
            Legacy4JClient.guiBufferSourceOverride = BufferSourceWrapper.translucent(poseStack.bufferSource());
        }
    }
    @Inject(method = "displayScoreboardSidebar", at = @At("HEAD"), cancellable = true)
    private void displayScoreboardSidebar(PoseStack poseStack, Objective objective, CallbackInfo ci) {
        ci.cancel();
        Scoreboard scoreboard = objective.getScoreboard();
        Collection<Score> collection = scoreboard.getPlayerScores(objective);
        List<Score> scores = collection.stream().filter((scorex) -> scorex.getOwner() != null && !scorex.getOwner().startsWith("#")).limit(15L).toList();;
        Component component = objective.getDisplayName();
        int i = this.getFont().width(component);
        int k = this.getFont().width(": ");
        int j = Math.max(i,scores.stream().mapToInt(lv-> {
            int w = getFont().width(lv.getObjective().getFormattedDisplayName());
            return this.getFont().width(PlayerTeam.formatNameForTeam(scoreboard.getPlayersTeam(lv.getOwner()), Component.literal(lv.getOwner()))) + (w > 0 ? k + w : 0);
        }).max().orElse(0));

        Objects.requireNonNull(this.getFont());
        int l = scores.size() * 9;
        int m = poseStack.guiHeight() / 2 + l / 3;
        int o = poseStack.guiWidth() - j - 3;
        int p = poseStack.guiWidth() - 3 + 2;
        Objects.requireNonNull(this.getFont());
        int s = m - scores.size() * 9;
        int x = o - 2;
        Objects.requireNonNull(this.getFont());
        ScreenUtil.renderPointerPanel(poseStack,x,s - 12,j + 4, scores.size() * 9 + 14);
        Font var18 = this.getFont();
        int var10003 = o + j / 2 - i / 2;
        Objects.requireNonNull(this.getFont());
        poseStack.drawString(var18, component, var10003, s - 9, -1, false);

        for(int t = 0; t < scores.size(); ++t) {
            Score lv = scores.get(t);
            x = scores.size() - t;
            Objects.requireNonNull(this.getFont());
            int u = m - x * 9;
            poseStack.drawString(this.getFont(), PlayerTeam.formatNameForTeam(scoreboard.getPlayersTeam(lv.getOwner()), Component.literal(lv.getOwner())), o, u, -1, false);
            Component score = lv.getObjective().getFormattedDisplayName();
            poseStack.drawString(this.getFont(), score, p - getFont().width(score), u, -1, false);
        }


    }
    @Inject(method = "renderHotbar", at = @At("RETURN"))
    public void renderHotbarTail(float f, PoseStack poseStack, CallbackInfo ci) {
        if (minecraft.screen != null) return;

        Legacy4JClient.guiBufferSourceOverride = null;
        ScreenUtil.finishHUDRender(poseStack);
        if (minecraft.player != null) ControlTooltip.Renderer.of(this).render(poseStack,0,0,f);
    }

    @Redirect(method = "renderExperienceBar", at = @At(value = "FIELD", target = "Lnet/minecraft/client/player/LocalPlayer;experienceLevel:I", ordinal = 0))
    public int renderExperienceBar(LocalPlayer instance, PoseStack poseStack) {
        if (instance.experienceLevel > 0) {
            this.minecraft.getProfiler().push("expLevel");
            String exp = "" + instance.experienceLevel;
            int hudScale = ScreenUtil.getLegacyOptions().hudScale().get();
            boolean is720p = minecraft.getWindow().getHeight() % 720 == 0;
            poseStack.pose().translate(poseStack.guiWidth() / 2,poseStack.guiHeight() - 36f,0);
            if (!is720p && hudScale != 1) poseStack.pose().scale(7/8f,7/8f,7/8f);
            ScreenUtil.drawOutlinedString(poseStack,getFont(), Component.literal(exp),-this.getFont().width(exp) / 2,-2,8453920,0,is720p && hudScale == 3 || !is720p && hudScale == 2 || hudScale == 1 ? 1/2f : 2/3f);
            this.minecraft.getProfiler().pop();
        }
        return 0;
    }
    @Inject(method = {"renderVehicleHealth","renderPlayerHealth"}, at = @At("HEAD"), cancellable = true)
    public void renderHealth(PoseStack poseStack, CallbackInfo ci) {
        if (minecraft.screen != null){
            ci.cancel();
            return;
        }
        ScreenUtil.prepareHUDRender(poseStack);
        poseStack.pose().translate(poseStack.guiWidth() / 2f, poseStack.guiHeight(),0);
        ScreenUtil.applyHUDScale(poseStack);
        poseStack.pose().translate(-poseStack.guiWidth() / 2, -poseStack.guiHeight(),0);
    }
    @Inject(method = {"renderVehicleHealth","renderPlayerHealth"}, at = @At("RETURN"))
    public void renderHealthReturn(PoseStack poseStack, CallbackInfo ci) {
        if (minecraft.screen != null) return;
        ScreenUtil.finishHUDRender(poseStack);
    }
    @Inject(method = "renderExperienceBar", at = @At("HEAD"), cancellable = true)
    public void renderExperienceBar(PoseStack poseStack, int i, CallbackInfo ci) {
        if (minecraft.screen != null){
            ci.cancel();
            return;
        }
        ScreenUtil.prepareHUDRender(poseStack);
        poseStack.pose().translate(poseStack.guiWidth() / 2f, poseStack.guiHeight(),0);
        ScreenUtil.applyHUDScale(poseStack);
        poseStack.pose().translate(-poseStack.guiWidth() / 2, -poseStack.guiHeight(),0);
    }
    @Inject(method = "renderExperienceBar", at = @At("RETURN"))
    public void renderExperienceBarReturn(PoseStack poseStack, int i, CallbackInfo ci) {
        if (minecraft.screen != null) return;
        ScreenUtil.finishHUDRender(poseStack);
    }
    @Inject(method = "renderJumpMeter", at = @At("HEAD"), cancellable = true)
    public void renderJumpMeter(PlayerRideableJumping playerRideableJumping, PoseStack poseStack, int i, CallbackInfo ci) {
        if (minecraft.screen != null){
            ci.cancel();
            return;
        }
        ScreenUtil.prepareHUDRender(poseStack);
        poseStack.pose().translate(poseStack.guiWidth() / 2f, poseStack.guiHeight(),0);
        ScreenUtil.applyHUDScale(poseStack);
        poseStack.pose().translate(-poseStack.guiWidth() / 2, -poseStack.guiHeight(),0);
    }
    @Inject(method = "renderJumpMeter", at = @At("RETURN"))
    public void renderJumpMeterReturn(PlayerRideableJumping playerRideableJumping, PoseStack poseStack, int i, CallbackInfo ci) {
        if (minecraft.screen != null) return;
        ScreenUtil.finishHUDRender(poseStack);
    }

    @Inject(method = "renderSavingIndicator", at = @At("HEAD"), cancellable = true)
    public void renderAutoSaveIndicator(PoseStack poseStack, CallbackInfo ci) {
        ci.cancel();
    }

}
