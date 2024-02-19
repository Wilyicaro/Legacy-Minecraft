package wily.legacy.forge.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.TooltipFlag;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.util.ScreenUtil;

@Mixin(ForgeGui.class)
public abstract class ForgeGuiMixin extends Gui {
    public ForgeGuiMixin(Minecraft arg, ItemRenderer arg2) {
        super(arg, arg2);
    }

    @Inject(method = "setupOverlayRenderState", at = @At("RETURN"),remap = false)
    private void setupOverlayRenderState(boolean blend, boolean depthTest, CallbackInfo ci) {
        RenderSystem.setShaderColor(1.0f,1.0f,1.0f, ScreenUtil.getHUDOpacity());
    }
    @Redirect(method = "renderRecordOverlay", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/util/FormattedCharSequence;III)I"))
    public int renderActionBar(GuiGraphics instance, Font arg, FormattedCharSequence arg2, int i, int j, int k) {
        if (minecraft.screen != null) return 0;
        instance.pose().pushPose();
        instance.pose().translate(0,ScreenUtil.getHUDDistance() - ScreenUtil.getHUDSize(),0);
        instance.setColor(1.0f,1.0f,1.0f,ScreenUtil.getHUDOpacity());
        int r = instance.drawString(arg,arg2,i,j + 63 - (lastToolHighlight.isEmpty() || this.toolHighlightTimer <= 0 ? 0 : (lastToolHighlight.getTooltipLines(null, TooltipFlag.NORMAL).stream().filter(c->!c.getString().isEmpty()).mapToInt(c->1).sum() - 1) * 9),k);
        instance.pose().popPose();
        instance.setColor(1.0f,1.0f,1.0f,1.0f);
        return r;
    }
}
