package wily.legacy.mixin.base.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.factoryapi.base.client.FactoryGuiGraphics;
import wily.factoryapi.util.FactoryScreenUtil;
import wily.legacy.client.screen.LegacySliderButton;
import wily.legacy.client.screen.ControlTooltip;
import wily.legacy.util.LegacyComponents;
import wily.legacy.util.LegacySprites;
import wily.legacy.util.client.LegacyFontUtil;
import wily.legacy.util.client.LegacyRenderUtil;

@Mixin(AbstractSliderButton.class)
public abstract class AbstractSliderButtonMixin extends AbstractWidget implements ControlTooltip.ActionHolder {


    @Shadow
    public boolean canChangeValue;
    @Shadow
    protected double value;

    public AbstractSliderButtonMixin(int i, int j, int k, int l, Component component) {
        super(i, j, k, l, component);
    }

    @Inject(method = "extractWidgetRenderState", at = @At("HEAD"), cancellable = true)
    public void extractWidgetRenderState(GuiGraphicsExtractor GuiGraphicsExtractor, int i, int j, float f, CallbackInfo ci) {
        ci.cancel();
        alpha = active ? 1.0f : 0.8f;
        FactoryGuiGraphics.of(GuiGraphicsExtractor).setBlitColor(1.0f, 1.0f, 1.0f, alpha);
        FactoryScreenUtil.enableBlend();
        FactoryScreenUtil.enableDepthTest();
        FactoryGuiGraphics.of(GuiGraphicsExtractor).blitSprite(LegacySprites.SLIDER, this.getX(), this.getY(), this.getWidth(), this.getHeight());
        if (isHoveredOrFocused())
            FactoryGuiGraphics.of(GuiGraphicsExtractor).blitSprite(LegacySprites.HIGHLIGHTED_SLIDER, this.getX() - 1, this.getY() - 1, this.getWidth() + 2, this.getHeight() + 2);
        FactoryGuiGraphics.of(GuiGraphicsExtractor).blitSprite(isHovered() ? LegacySprites.SLIDER_HANDLE_HIGHLIGHTED : LegacySprites.SLIDER_HANDLE, this.getX() + (int) (this.value * (double) (this.width - 8)), this.getY(), 8, this.getHeight());
        FactoryGuiGraphics.of(GuiGraphicsExtractor).setBlitColor(1.0f, 1.0f, 1.0f, 1.0f);
        renderCenteredText(GuiGraphicsExtractor);
    }

    private void renderCenteredText(GuiGraphicsExtractor GuiGraphicsExtractor) {
        Component message = getMessage().copy().withColor(LegacyRenderUtil.getDefaultTextColor(!isHoveredOrFocused()));
        if ((Object) this instanceof LegacySliderButton<?> slider) {
            LegacyFontUtil.applyFontOverride(slider.fontOverrideSupplier.get(), b -> extractScrollingStringOverContents(GuiGraphicsExtractor.textRendererForWidget(this, net.minecraft.client.gui.GuiGraphicsExtractor.HoveredTextEffects.NONE), message, 2));
        } else {
            extractScrollingStringOverContents(GuiGraphicsExtractor.textRendererForWidget(this, net.minecraft.client.gui.GuiGraphicsExtractor.HoveredTextEffects.NONE), message, 2);
        }
    }

    @Override
    public @Nullable Component getAction(Context context) {
        return isFocused() && context instanceof KeyContext c && c.key() == InputConstants.KEY_RETURN ? canChangeValue ? LegacyComponents.LOCK : LegacyComponents.UNLOCK : null;
    }
}
