package wily.legacy.mixin.base.client.gui;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
//? if <26.2 {
/*import net.minecraft.client.gui.contextualbar.ExperienceBarRenderer;
*///?} else {
import net.minecraft.client.gui.contextualbar.ExperienceBar;
//?}
import org.spongepowered.asm.mixin.Mixin;
import wily.factoryapi.base.client.UIAccessor;
import wily.factoryapi.util.FactoryGuiElement;
import wily.factoryapi.util.FactoryScreenUtil;

@Mixin(/*? if <26.2 {*//*ExperienceBarRenderer*//*?} else {*/ExperienceBar/*?}*/.class)
public class ExperienceBarRendererMixin {
    //? if forge {
    /*@WrapMethod(method = {"extractBackground", "extractRenderState"})
    private void extractBackground(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, Operation<Void> original) {
        UIAccessor accessor = FactoryScreenUtil.getGuiAccessor();
        FactoryGuiElement guiElement = FactoryGuiElement.EXPERIENCE_BAR;
        if (!guiElement.isVisible(accessor)) return;
        guiElement.prepareRender(graphics, accessor);
        original.call(graphics, deltaTracker);
        guiElement.finalizeRender(graphics, accessor);
    }
    *///?}

}
