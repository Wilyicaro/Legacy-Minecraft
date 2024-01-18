package wily.legacy.fabric.mixin;

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
    @Inject(method = "renderVehicleHealth", at = @At("HEAD"), cancellable = true)
    public void renderVehicleHealth(GuiGraphics guiGraphics, CallbackInfo ci) {
        if (minecraft.screen != null){
            ci.cancel();
            return;
        }
        guiGraphics.setColor(1.0f,1.0f,1.0f, ScreenUtil.getInterfaceOpacity());
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0.0F,ScreenUtil.getHUDDistance(),0.0F);
        ScreenUtil.applyHUDScale(guiGraphics,i-> screenWidth = i,i-> screenHeight = i);
    }
    @Inject(method = "renderVehicleHealth", at = @At("RETURN"))
    public void renderVehicleHealthTail(GuiGraphics guiGraphics, CallbackInfo ci) {
        if (minecraft.screen != null)
            return;
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
        guiGraphics.setColor(1.0f,1.0f,1.0f, ScreenUtil.getInterfaceOpacity());
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0.0F,ScreenUtil.getHUDDistance(),0.0F);
        ScreenUtil.applyHUDScale(guiGraphics,i-> screenWidth = i,i-> screenHeight = i);
    }
    @Inject(method = "renderPlayerHealth", at = @At("RETURN"))
    public void renderPlayerHealthTail(GuiGraphics guiGraphics, CallbackInfo ci) {
        if (minecraft.screen != null)
            return;
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
                int k = this.screenHeight - 80 - getFont().lineHeight * (tooltipLines.size() - 1 - i);
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
        this.minecraft.getProfiler().pop();
        guiGraphics.pose().popPose();
        ci.cancel();
    }

}
