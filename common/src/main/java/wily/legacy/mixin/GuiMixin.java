package wily.legacy.mixin;

import com.google.common.collect.Ordering;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.MobEffectTextureManager;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.numbers.NumberFormat;
import net.minecraft.network.chat.numbers.StyledFormat;
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
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerScoreEntry;
import net.minecraft.world.scores.PlayerTeam;
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

@Mixin(Gui.class)

public abstract class GuiMixin implements ControlTooltip.Event {
    @Shadow @Final protected Minecraft minecraft;
    @Final
    @Shadow
    private static Comparator<? super PlayerScoreEntry> SCORE_DISPLAY_ORDER;

    @Shadow public abstract Font getFont();

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
    public void renderCrosshairReturn(GuiGraphics guiGraphics, CallbackInfo ci) {
        if (minecraft.screen != null)
            return;
        guiGraphics.setColor(1.0f,1.0f,1.0f,1.0f);
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
            int k = guiGraphics.guiWidth() - 55;
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
            if (mobEffectInstance.endsWithin(200)){
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
        guiGraphics.blitSprite(LegacySprites.HOTBAR_SELECTION,24,24,0,23,guiGraphics.guiWidth() / 2 - 91 - 1 + minecraft.player.getInventory().selected * 20, guiGraphics.guiHeight(), 24, 1);
    }
    @Inject(method = "renderHotbar", at = @At("HEAD"), cancellable = true)
    public void renderHotbar(float f, GuiGraphics guiGraphics, CallbackInfo ci) {
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
                guiGraphics.pose().pushPose();
                ScreenUtil.applyHUDScale(guiGraphics);
                f = ScreenUtil.getLegacyOptions().smoothAnimatedCharacter().get() ? f : 0;
                ScreenUtil.renderEntity(guiGraphics, 28f, 50f, 13, f,new Vector3f(), new Quaternionf().rotationXYZ(0, -(Mth.lerp(f, character.yBodyRotO, character.yBodyRot) * (float)(Math.PI/180f)) + (float) (Math.PI * 7/8f), (float) Math.PI), null, character);
                guiGraphics.pose().popPose();
                character.setXRot(xRot);
                character.xRotO = xRotO;
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
    @Inject(method = "displayScoreboardSidebar", at = @At("HEAD"), cancellable = true)
    private void displayScoreboardSidebar(GuiGraphics guiGraphics, Objective objective, CallbackInfo ci) {
        ci.cancel();
        Scoreboard scoreboard = objective.getScoreboard();
        NumberFormat numberFormat = objective.numberFormatOrDefault(StyledFormat.SIDEBAR_DEFAULT);
        List<PlayerScoreEntry> lvs = scoreboard.listPlayerScores(objective).stream().filter((playerScoreEntry) -> !playerScoreEntry.isHidden()).sorted(SCORE_DISPLAY_ORDER).limit(15L).toList();
        Component component = objective.getDisplayName();
        int i = this.getFont().width(component);
        int k = this.getFont().width(": ");
        int j = Math.max(i,lvs.stream().mapToInt(lv-> {
            int w = getFont().width(lv.formatValue(numberFormat));
            return this.getFont().width(PlayerTeam.formatNameForTeam(scoreboard.getPlayersTeam(lv.owner()), lv.ownerName())) + (w > 0 ? k + w : 0);
        }).max().orElse(0));

        Objects.requireNonNull(this.getFont());
        int l = lvs.size() * 9;
        int m = guiGraphics.guiHeight() / 2 + l / 3;
        int o = guiGraphics.guiWidth() - j - 3;
        int p = guiGraphics.guiWidth() - 3 + 2;
        Objects.requireNonNull(this.getFont());
        int s = m - lvs.size() * 9;
        int x = o - 2;
        Objects.requireNonNull(this.getFont());
        ScreenUtil.renderPointerPanel(guiGraphics,x,s - 12,j + 4, lvs.size() * 9 + 14);
        Font var18 = this.getFont();
        int var10003 = o + j / 2 - i / 2;
        Objects.requireNonNull(this.getFont());
        guiGraphics.drawString(var18, component, var10003, s - 9, -1, false);

        for(int t = 0; t < lvs.size(); ++t) {
            PlayerScoreEntry lv = lvs.get(t);
            x = lvs.size() - t;
            Objects.requireNonNull(this.getFont());
            int u = m - x * 9;
            guiGraphics.drawString(this.getFont(), PlayerTeam.formatNameForTeam(scoreboard.getPlayersTeam(lv.owner()), lv.ownerName()), o, u, -1, false);
            Component score = lv.formatValue(numberFormat);
            guiGraphics.drawString(this.getFont(), score, p - getFont().width(lv.formatValue(numberFormat)), u, -1, false);
        }


    }
    @Inject(method = "renderHotbar", at = @At("RETURN"))
    public void renderHotbarTail(float f, GuiGraphics guiGraphics, CallbackInfo ci) {
        if (minecraft.screen != null) return;

        Legacy4JClient.guiBufferSourceOverride = null;
        ScreenUtil.finishHUDRender(guiGraphics);
        if (minecraft.player != null) ControlTooltip.Renderer.of(this).render(guiGraphics,0,0,f);
    }

    @Redirect(method = "renderExperienceBar", at = @At(value = "FIELD", target = "Lnet/minecraft/client/player/LocalPlayer;experienceLevel:I", ordinal = 0))
    public int renderExperienceBar(LocalPlayer instance, GuiGraphics guiGraphics) {
        if (instance.experienceLevel > 0) {
            this.minecraft.getProfiler().push("expLevel");
            String exp = "" + instance.experienceLevel;
            int hudScale = ScreenUtil.getLegacyOptions().hudScale().get();
            boolean is720p = minecraft.getWindow().getHeight() % 720 == 0;
            guiGraphics.pose().translate(guiGraphics.guiWidth() / 2,guiGraphics.guiHeight() - 36f,0);
            if (!is720p && hudScale != 1) guiGraphics.pose().scale(7/8f,7/8f,7/8f);
            ScreenUtil.drawOutlinedString(guiGraphics,getFont(), Component.literal(exp),-this.getFont().width(exp) / 2,-2,8453920,0,is720p && hudScale == 3 || !is720p && hudScale == 2 || hudScale == 1 ? 1/2f : 2/3f);
            this.minecraft.getProfiler().pop();
        }
        return 0;
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
    public void renderAutoSaveIndicator(GuiGraphics guiGraphics, CallbackInfo ci) {
        ci.cancel();
    }

}
