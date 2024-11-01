package wily.legacy.fabric.mixin.legacy;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;
import wily.legacy.client.CommonColor;

@Mixin(EditBox.class)
public abstract class EditBoxMixin extends AbstractWidget {

    @Shadow private int cursorPos;

    public EditBoxMixin(int i, int j, int k, int l, Component component) {
        super(i, j, k, l, component);
    }

    @Redirect(method = "renderWidget", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Ljava/lang/String;III)I", ordinal = 1))
    public int renderWidget(GuiGraphics instance, Font arg, String string, int i, int j, int k) {
        instance.pose().pushPose();
        instance.pose().translate(i-(cursorPos == 0 ? 3 : 4),j+8.5f,0);
        instance.pose().scale(6,1.5f,1f);
        instance.fill(0,0,1,1, CommonColor.WIDGET_TEXT.get() | 0xFF000000);
        instance.pose().popPose();
        return 0;
    }
}
