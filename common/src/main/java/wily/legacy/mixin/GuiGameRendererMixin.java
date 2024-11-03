package wily.legacy.mixin;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import wily.legacy.client.LegacyOption;

@Mixin(GameRenderer.class)
public class GuiGameRendererMixin {
    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Gui;render(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/DeltaTracker;)V"))
    private void render(Gui instance, GuiGraphics guiGraphics, DeltaTracker deltaTracker){
        if (LegacyOption.displayHUD.get()) instance.render(guiGraphics,deltaTracker);
    }
}
