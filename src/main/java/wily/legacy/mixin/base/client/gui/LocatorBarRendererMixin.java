//? if >=26.1 {
package wily.legacy.mixin.base.client.gui;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
//? if <26.2 {
/*import net.minecraft.client.gui.contextualbar.LocatorBarRenderer;
*///?} else {
import net.minecraft.client.gui.contextualbar.LocatorBar;
//?}
import net.minecraft.util.ARGB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import wily.factoryapi.base.client.UIAccessor;
import wily.factoryapi.util.FactoryGuiElement;
import wily.factoryapi.util.FactoryScreenUtil;
import wily.legacy.util.client.LegacyRenderUtil;

@Mixin(/*? if <26.2 {*//*LocatorBarRenderer*//*?} else {*/LocatorBar/*?}*/.class)
public class LocatorBarRendererMixin {

    @ModifyArg(method = "lambda$extractRenderState$1", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blitSprite(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIIII)V"), index = 6)
    private int extractRenderState(int color) {
        return ARGB.color((int) (ARGB.alpha(color) * LegacyRenderUtil.getHUDOpacity()), ARGB.transparent(color));
    }

    //? if forge {
    /*@WrapMethod(method = {"extractBackground", "extractRenderState"})
    private void extractBackground(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, Operation<Void> original) {
        UIAccessor accessor = FactoryScreenUtil.getGuiAccessor();
        FactoryGuiElement guiElement = FactoryGuiElement.LOCATOR_BAR;
        if (!guiElement.isVisible(accessor)) return;
        guiElement.prepareRender(graphics, accessor);
        original.call(graphics, deltaTracker);
        guiElement.finalizeRender(graphics, accessor);
    }
    *///?}
}
//?}
