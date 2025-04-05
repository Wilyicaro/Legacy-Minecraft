//? if <1.20.5 && (forge || neoforge) {
/*package wily.legacy.mixin.base.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.world.item.TooltipFlag;
//? forge {
import net.minecraftforge.client.gui.overlay.ForgeGui;
//?} else {
/^import net.neoforged.neoforge.client.gui.overlay.ExtendedGui;
^///?}
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.util.ScreenUtil;

@Mixin(/^? forge {^/ForgeGui/^?} else {^//^ExtendedGui^//^?}^/.class)
public abstract class ForgeGuiMixin extends Gui {
    public ForgeGuiMixin(Minecraft arg, ItemRenderer arg2) {
        super(arg, arg2);
    }

    @Redirect(method="renderHealth", at = @At(value = "FIELD", target = /^? if forge {^/"Lnet/minecraftforge/client/gui/overlay/ForgeGui;healthBlinkTime:J"/^?} else {^//^"Lnet/neoforged/neoforge/client/gui/overlay/ExtendedGui;healthBlinkTime:J"^//^?}^/, opcode = Opcodes.PUTFIELD, ordinal = 1))
    private void renderHealth(/^? forge {^/ForgeGui/^?} else {^//^ExtendedGui^//^?}^/ instance, long value) {
        healthBlinkTime = value - 6;
    }
}
*///?}