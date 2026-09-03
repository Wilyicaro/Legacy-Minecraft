package wily.legacy.mixin.base.client.gui;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
//? if <26.2 {
/*import net.minecraft.client.gui.contextualbar.JumpableVehicleBarRenderer;
*///?} else {
import net.minecraft.client.gui.contextualbar.JumpableVehicleBar;
//?}
import org.spongepowered.asm.mixin.Mixin;
import wily.factoryapi.base.client.UIAccessor;
import wily.factoryapi.util.FactoryGuiElement;
import wily.factoryapi.util.FactoryScreenUtil;

@Mixin(/*? if <26.2 {*//*JumpableVehicleBarRenderer*//*?} else {*/JumpableVehicleBar/*?}*/.class)
public class JumpableVehicleBarRendererMixin {
    //? if forge {
    /*@WrapMethod(method = {"extractBackground", "extractRenderState"})
    private void extractBackground(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, Operation<Void> original) {
        UIAccessor accessor = FactoryScreenUtil.getGuiAccessor();
        FactoryGuiElement guiElement = FactoryGuiElement.JUMP_METER;
        if (!guiElement.isVisible(accessor)) return;
        guiElement.prepareRender(graphics, accessor);
        original.call(graphics, deltaTracker);
        guiElement.finalizeRender(graphics, accessor);
    }
    *///?}

}
