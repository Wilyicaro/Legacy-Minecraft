package wily.legacy.mixin;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import wily.legacy.util.ScreenUtil;

@Mixin(AbstractButton.class)
public abstract class AbstractButtonMixin extends AbstractWidget {
    @Shadow @Final private static WidgetSprites SPRITES;

    @Shadow public abstract void renderString(GuiGraphics arg, Font arg2, int i);

    public AbstractButtonMixin(int i, int j, int k, int l, Component component) {
        super(i, j, k, l, component);
    }

    @Redirect(method = "renderWidget", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/AbstractButton;renderString(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/gui/Font;I)V"))
    protected void renderWidget(AbstractButton instance, GuiGraphics graphics, Font font, int c) {
        renderString(graphics,font,c);
        if (isHoveredOrFocused()){
            float timer = Util.getMillis() / 1200f % 1;
            renderString(graphics,font,ScreenUtil.getDefaultTextColor(false) |  Mth.ceil(255* (timer >= 0.5f ? 1 - timer  : timer) * 2) << 24 );
        }
    }
    @Redirect(method = "renderWidget", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;blitSprite(Lnet/minecraft/resources/ResourceLocation;IIII)V"))
    protected void renderWidget(GuiGraphics guiGraphics, ResourceLocation resourceLocation, int i, int j, int k, int l) {
        alpha = active ? 1 : 0.8f;
        guiGraphics.blitSprite(SPRITES.get(true, false), this.getX(), this.getY(), this.getWidth(), this.getHeight());
        if (isHoveredOrFocused()) {
            float timer = Util.getMillis() / 1200f % 1;
            guiGraphics.setColor(1.0f,1.0f,1.0f, (timer >= 0.5f ? 1 - timer  : timer) * 2);
            guiGraphics.blitSprite(SPRITES.get(true, true), this.getX(), this.getY(), this.getWidth(), this.getHeight());
            this.renderString(guiGraphics, Minecraft.getInstance().font, k | Mth.ceil(this.alpha * 255.0F) << 24);
        }
    }
}
