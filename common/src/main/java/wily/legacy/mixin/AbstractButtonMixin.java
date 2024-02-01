package wily.legacy.mixin;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import wily.legacy.util.ScreenUtil;

@Mixin(AbstractButton.class)
public abstract class AbstractButtonMixin extends AbstractWidget {
    @Shadow @Final private static WidgetSprites SPRITES;

    public AbstractButtonMixin(int i, int j, int k, int l, Component component) {
        super(i, j, k, l, component);
    }

    @ModifyVariable(method = "renderWidget", at = @At(value = "STORE"), ordinal = 2)
    protected int renderWidget(int k) {
        return ScreenUtil.getDefaultTextColor(!isHoveredOrFocused());
    }
    @Redirect(method = "renderWidget", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;blitSprite(Lnet/minecraft/resources/ResourceLocation;IIII)V"))
    protected void renderWidget(GuiGraphics guiGraphics, ResourceLocation resourceLocation, int i, int j, int k, int l) {
        alpha = active ? 1 : 0.8f;
        guiGraphics.blitSprite(SPRITES.get(true, this.isHoveredOrFocused()), this.getX(), this.getY(), this.getWidth(), this.getHeight());
    }
}
