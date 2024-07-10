package wily.legacy.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.util.ScreenUtil;

@Mixin(AbstractSliderButton.class)
public abstract class AbstractSliderButtonMixin extends AbstractWidget {

    @Shadow protected abstract ResourceLocation getSprite();

    @Shadow protected double value;

    @Shadow @Final private static ResourceLocation HIGHLIGHTED_SPRITE;

    @Shadow @Final private static ResourceLocation SLIDER_SPRITE;

    @Shadow @Final private static ResourceLocation SLIDER_HANDLE_HIGHLIGHTED_SPRITE;

    @Shadow @Final private static ResourceLocation SLIDER_HANDLE_SPRITE;

    public AbstractSliderButtonMixin(int i, int j, int k, int l, Component component) {
        super(i, j, k, l, component);
    }
    @Inject(method = "renderWidget", at = @At("HEAD"), cancellable = true)
    public void renderWidget(GuiGraphics guiGraphics, int i, int j, float f, CallbackInfo ci) {
        ci.cancel();
        alpha = active ? 1.0f : 0.5f;
        Minecraft minecraft = Minecraft.getInstance();
        guiGraphics.setColor(1.0f, 1.0f, 1.0f, alpha);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        guiGraphics.blitSprite(SLIDER_SPRITE, this.getX(), this.getY(), this.getWidth(), this.getHeight());
        if (isHoveredOrFocused()) guiGraphics.blitSprite(HIGHLIGHTED_SPRITE, this.getX() - 1, this.getY() - 1, this.getWidth() + 2, this.getHeight() + 2);
        guiGraphics.blitSprite(isHovered() ? SLIDER_HANDLE_HIGHLIGHTED_SPRITE : SLIDER_HANDLE_SPRITE, this.getX() + (int)(this.value * (double)(this.width - 8)), this.getY(), 8, this.getHeight());
        guiGraphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);
        int k = ScreenUtil.getDefaultTextColor(!isHoveredOrFocused());
        this.renderScrollingString(guiGraphics, minecraft.font, 2, k | Mth.ceil(this.alpha * 255.0f) << 24);
    }
}
