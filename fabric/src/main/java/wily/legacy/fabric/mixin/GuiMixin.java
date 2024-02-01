package wily.legacy.fabric.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.util.ScreenUtil;

import java.util.List;

@Mixin(Gui.class)

public abstract class GuiMixin {
    @Shadow @Final protected Minecraft minecraft;

    @Shadow protected int screenHeight;

    @Shadow public abstract Font getFont();

    @Shadow protected int screenWidth;

    @Shadow protected int toolHighlightTimer;

    @Shadow
    private ItemStack lastToolHighlight;

    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;III)I"))
    public int renderActionBar(GuiGraphics instance, Font arg, Component arg2, int i, int j, int k) {
        if (minecraft.screen != null) return 0;
        RenderSystem.enableBlend();
        instance.pose().pushPose();
        instance.pose().translate(0,ScreenUtil.getHUDDistance(),0);
        instance.setColor(1.0f,1.0f,1.0f,ScreenUtil.getHUDOpacity());
        int r = instance.drawString(arg,arg2,i,j - (int)(3f / ScreenUtil.getHUDScale()* 40 - 40) - (lastToolHighlight.isEmpty() ? 0 : (lastToolHighlight.getTooltipLines(null, TooltipFlag.NORMAL).stream().filter(c->!c.getString().isEmpty()).mapToInt(c->1).sum() - 1) * 9),k);
        instance.pose().popPose();
        instance.setColor(1.0f,1.0f,1.0f,1.0f);
        RenderSystem.disableBlend();
        return r;
    }
    @Inject(method = "renderVehicleHealth", at = @At("HEAD"), cancellable = true)
    public void renderVehicleHealth(GuiGraphics guiGraphics, CallbackInfo ci) {
        if (minecraft.screen != null){
            ci.cancel();
            return;
        }
        RenderSystem.enableBlend();
        guiGraphics.setColor(1.0f,1.0f,1.0f, ScreenUtil.getHUDOpacity());
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0.0F,ScreenUtil.getHUDDistance(),0.0F);
        ScreenUtil.applyHUDScale(guiGraphics,i-> screenWidth = i,i-> screenHeight = i);
    }
    @Inject(method = "renderVehicleHealth", at = @At("RETURN"))
    public void renderVehicleHealthTail(GuiGraphics guiGraphics, CallbackInfo ci) {
        if (minecraft.screen != null)
            return;
        RenderSystem.disableBlend();
        guiGraphics.setColor(1.0f,1.0f,1.0f,1.0f);
        ScreenUtil.resetHUDScale(guiGraphics, i-> screenWidth = i, i-> screenHeight = i);
        guiGraphics.pose().popPose();
    }
    @Inject(method = "renderPlayerHealth", at = @At("HEAD"), cancellable = true)
    public void renderPlayerHealth(GuiGraphics guiGraphics, CallbackInfo ci) {
        if (minecraft.screen != null){
            ci.cancel();
            return;
        }
        RenderSystem.enableBlend();
        guiGraphics.setColor(1.0f,1.0f,1.0f, ScreenUtil.getHUDOpacity());
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0.0F,ScreenUtil.getHUDDistance(),0.0F);
        ScreenUtil.applyHUDScale(guiGraphics,i-> screenWidth = i,i-> screenHeight = i);
    }
    @Inject(method = "renderPlayerHealth", at = @At("RETURN"))
    public void renderPlayerHealthTail(GuiGraphics guiGraphics, CallbackInfo ci) {
        if (minecraft.screen != null)
            return;
        RenderSystem.disableBlend();
        guiGraphics.setColor(1.0f,1.0f,1.0f,1.0f);
        ScreenUtil.resetHUDScale(guiGraphics, i-> screenWidth = i, i-> screenHeight = i);
        guiGraphics.pose().popPose();
    }
    @Inject(method = "renderSelectedItemName", at = @At("HEAD"), cancellable = true)
    public void renderSelectedItemName(GuiGraphics guiGraphics, CallbackInfo ci) {
        if (minecraft.screen != null){
            ci.cancel();
            return;
        }
        RenderSystem.enableBlend();
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0.0F,ScreenUtil.getHUDDistance(),0.0F);
        this.minecraft.getProfiler().push("selectedItemName");
        if (this.toolHighlightTimer > 0 && !this.lastToolHighlight.isEmpty()) {
            List<Component> tooltipLines = this.lastToolHighlight.getTooltipLines(null, TooltipFlag.NORMAL).stream().filter(c->!c.getString().isEmpty()).toList();
            for (int i = 0; i < tooltipLines.size(); i++) {
                int l;
                Component mutableComponent = tooltipLines.get(i);
                if (this.lastToolHighlight.hasCustomHoverName()) {
                    mutableComponent.copy().withStyle(ChatFormatting.ITALIC);
                }
                int width = this.getFont().width(mutableComponent);
                int j = (this.screenWidth - width) / 2;
                int k = this.screenHeight - (int)(3f / ScreenUtil.getHUDScale()* 40) - 20 - getFont().lineHeight * (tooltipLines.size() - 1 - i);
                if (!this.minecraft.gameMode.canHurtPlayer()) {
                    k += 14;
                }
                if ((l = (int)((float)this.toolHighlightTimer * 256.0f / 10.0f)) > 255) {
                    l = 255;
                }
                if (l > 0) {
                    guiGraphics.fill(j - 2, k - 2, j + width + 2, k + this.getFont().lineHeight + 2, this.minecraft.options.getBackgroundColor(0));
                    guiGraphics.drawString(this.getFont(), mutableComponent, j, k, 0xFFFFFF + (l << 24));
                }
            }
        }
        RenderSystem.disableBlend();
        this.minecraft.getProfiler().pop();
        guiGraphics.pose().popPose();
        ci.cancel();
    }

}
