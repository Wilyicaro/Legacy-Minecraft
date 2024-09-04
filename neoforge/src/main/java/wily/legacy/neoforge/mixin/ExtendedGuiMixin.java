package wily.legacy.neoforge.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.neoforge.client.gui.overlay.ExtendedGui;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.util.ScreenUtil;

@Mixin(ExtendedGui.class)
public abstract class ExtendedGuiMixin extends Gui {
    public ExtendedGuiMixin(Minecraft arg, ItemRenderer arg2) {
        super(arg, arg2);
    }

    @Shadow public abstract Minecraft getMinecraft();

    @Inject(method = "renderRecordOverlay", at = @At(value = "HEAD"), cancellable = true)
    public void renderOverlayMessage(int width, int height, float partialTick, GuiGraphics guiGraphics, CallbackInfo ci) {
        if (getMinecraft().screen != null){
            ci.cancel();
            return;
        }
        ScreenUtil.prepareHUDRender(guiGraphics);
        guiGraphics.pose().translate(0, 63 - ScreenUtil.getHUDSize() - (this.lastToolHighlight.isEmpty() || this.toolHighlightTimer <= 0 ? 0 : (Math.min(4,lastToolHighlight.getTooltipLines(minecraft.player, TooltipFlag.NORMAL).stream().filter(c->!c.getString().isEmpty()).mapToInt(c->1).sum()) - 1) * 9),0);
    }
    @Inject(method = "renderRecordOverlay", at = @At(value = "RETURN"))
    public void renderOverlayMessageReturn(int width, int height, float partialTick, GuiGraphics guiGraphics, CallbackInfo ci) {
        if (getMinecraft().screen != null) return;
        ScreenUtil.finishHUDRender(guiGraphics);
    }
    @Inject(method = {"renderHealth","renderFood","renderAir","renderHealthMount"}, at = @At("HEAD"), cancellable = true)
    public void renderHealth(int width, int height, GuiGraphics guiGraphics, CallbackInfo ci) {
        if (minecraft.screen != null){
            ci.cancel();
            return;
        }
        ScreenUtil.prepareHUDRender(guiGraphics);
        guiGraphics.pose().translate(guiGraphics.guiWidth() / 2f, guiGraphics.guiHeight(),0);
        ScreenUtil.applyHUDScale(guiGraphics);
        guiGraphics.pose().translate(-guiGraphics.guiWidth() / 2, -guiGraphics.guiHeight(),0);
    }
    @Inject(method = {"renderHealth","renderFood","renderAir","renderHealthMount"}, at = @At("RETURN"))
    public void renderHealthReturn(int width, int height, GuiGraphics guiGraphics, CallbackInfo ci) {
        if (minecraft.screen != null) return;
        ScreenUtil.finishHUDRender(guiGraphics);
    }
    @Inject(method = "renderArmor", at = @At("HEAD"), cancellable = true)
    public void renderArmor(GuiGraphics guiGraphics, int width, int height, CallbackInfo ci) {
        if (minecraft.screen != null){
            ci.cancel();
            return;
        }
        ScreenUtil.prepareHUDRender(guiGraphics);
        guiGraphics.pose().translate(guiGraphics.guiWidth() / 2f, guiGraphics.guiHeight(),0);
        ScreenUtil.applyHUDScale(guiGraphics);
        guiGraphics.pose().translate(-guiGraphics.guiWidth() / 2, -guiGraphics.guiHeight(),0);
    }
    @Inject(method = "renderArmor", at = @At("RETURN"))
    public void renderArmorReturn(GuiGraphics guiGraphics, int width, int height, CallbackInfo ci) {
        if (minecraft.screen != null) return;
        ScreenUtil.finishHUDRender(guiGraphics);
    }
    @Redirect(method="renderHealth", at = @At(value = "FIELD", target = "Lnet/neoforged/neoforge/client/gui/overlay/ExtendedGui;healthBlinkTime:J", opcode = Opcodes.PUTFIELD, ordinal = 1))
    private void renderHealth(ExtendedGui instance, long value) {
        healthBlinkTime = value - 5;
    }
}