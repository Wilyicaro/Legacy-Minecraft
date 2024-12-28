//? if <1.20.5 && (forge || neoforge) {
/*package wily.legacy.mixin.base;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.world.item.TooltipFlag;
//? forge {
/^import net.minecraftforge.client.gui.overlay.ForgeGui;
^///?} else {
import net.neoforged.neoforge.client.gui.overlay.ExtendedGui;
//?}
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.util.ScreenUtil;

@Mixin(/^? forge {^//^ForgeGui^//^?} else {^/ExtendedGui/^?}^/.class)
public abstract class ForgeGuiMixin extends Gui {
    public ForgeGuiMixin(Minecraft arg, ItemRenderer arg2) {
        super(arg, arg2);
    }

    @Shadow(remap = false) public abstract Minecraft getMinecraft();

    @Inject(method = "renderRecordOverlay", at = @At(value = "HEAD"), remap = false, cancellable = true)
    public void renderOverlayMessage(int width, int height, float partialTick, GuiGraphics guiGraphics, CallbackInfo ci) {
        if (getMinecraft().screen != null){
            ci.cancel();
            return;
        }
        ScreenUtil.prepareHUDRender(guiGraphics);
        guiGraphics.pose().translate(0, 63 - ScreenUtil.getHUDSize() - (this.lastToolHighlight.isEmpty() || this.toolHighlightTimer <= 0 ? 0 : (Math.min(4,lastToolHighlight.getTooltipLines(minecraft.player, TooltipFlag.NORMAL).stream().filter(c->!c.getString().isEmpty()).mapToInt(c->1).sum()) - 1) * 9),0);
    }
    @Inject(method = "renderRecordOverlay", at = @At(value = "RETURN"), remap = false)
    public void renderOverlayMessageReturn(int width, int height, float partialTick, GuiGraphics guiGraphics, CallbackInfo ci) {
        if (getMinecraft().screen != null) return;
        ScreenUtil.finalizeHUDRender(guiGraphics);
    }
    @Inject(method = {"renderHealth","renderFood","renderAir","renderHealthMount"}, at = @At("HEAD"), cancellable = true,remap = false)
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
    @Inject(method = {"renderHealth","renderFood","renderAir","renderHealthMount"}, at = @At("RETURN"),remap = false)
    public void renderHealthReturn(int width, int height, GuiGraphics guiGraphics, CallbackInfo ci) {
        if (minecraft.screen != null) return;
        ScreenUtil.finalizeHUDRender(guiGraphics);
    }
    @Inject(method = "renderArmor", at = @At("HEAD"), remap = false, cancellable = true)
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
    @Inject(method = "renderArmor", at = @At("RETURN"), remap = false)
    public void renderArmorReturn(GuiGraphics guiGraphics, int width, int height, CallbackInfo ci) {
        if (minecraft.screen != null) return;
        ScreenUtil.finalizeHUDRender(guiGraphics);
    }
    @Redirect(method="renderHealth", at = @At(value = "FIELD", target = /^? if forge {^//^"Lnet/minecraftforge/client/gui/overlay/ForgeGui;healthBlinkTime:J"^//^?} else {^/"Lnet/neoforged/neoforge/client/gui/overlay/ExtendedGui;healthBlinkTime:J"/^?}^/, opcode = Opcodes.PUTFIELD, ordinal = 1))
    private void renderHealth(/^? forge {^//^ForgeGui^//^?} else {^/ExtendedGui/^?}^/ instance, long value) {
        healthBlinkTime = value - 6;
    }
}
*///?}