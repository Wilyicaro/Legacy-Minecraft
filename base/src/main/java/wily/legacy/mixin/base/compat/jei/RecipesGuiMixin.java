//? if !forge {
package wily.legacy.mixin.base.compat.jei;

import mezz.jei.gui.recipes.RecipesGui;
import org.spongepowered.asm.mixin.Mixin;
import wily.legacy.client.control.ControllerListener;
import wily.legacy.client.control.tooltip.ControlTooltip;

@Mixin(RecipesGui.class)
public class RecipesGuiMixin implements ControllerListener, ControlTooltip.Listener {
    @Override
    public boolean disableCursorOnInit() {
        return false;
    }
}
//?}
