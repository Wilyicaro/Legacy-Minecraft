package wily.legacy.mixin.base;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.factoryapi.base.client.FactoryGuiGraphics;
import wily.legacy.client.screen.ControlTooltip;
import wily.legacy.util.LegacyComponents;
import wily.legacy.util.LegacySprites;
import wily.legacy.util.ScreenUtil;

@Mixin(AbstractSliderButton.class)
public abstract class AbstractSliderButtonMixin extends AbstractWidget implements ControlTooltip.ActionHolder {


    @Shadow protected double value;

    @Shadow public boolean canChangeValue;

    public AbstractSliderButtonMixin(int i, int j, int k, int l, Component component) {
        super(i, j, k, l, component);
    }
    @Inject(method = "renderWidget", at = @At("HEAD"), cancellable = true)
    public void renderWidget(GuiGraphics guiGraphics, int i, int j, float f, CallbackInfo ci) {
        ci.cancel();
        alpha = active ? 1.0f : 0.8f;
        Minecraft minecraft = Minecraft.getInstance();
        FactoryGuiGraphics.of(guiGraphics).setColor(1.0f, 1.0f, 1.0f, alpha);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        FactoryGuiGraphics.of(guiGraphics).blitSprite(LegacySprites.SLIDER, this.getX(), this.getY(), this.getWidth(), this.getHeight());
        if (isHoveredOrFocused()) FactoryGuiGraphics.of(guiGraphics).blitSprite(LegacySprites.HIGHLIGHTED_SLIDER, this.getX() - 1, this.getY() - 1, this.getWidth() + 2, this.getHeight() + 2);
        FactoryGuiGraphics.of(guiGraphics).blitSprite(isHovered() ? LegacySprites.SLIDER_HANDLE_HIGHLIGHTED : LegacySprites.SLIDER_HANDLE, this.getX() + (int)(this.value * (double)(this.width - 8)), this.getY(), 8, this.getHeight());
        FactoryGuiGraphics.of(guiGraphics).setColor(1.0f, 1.0f, 1.0f, 1.0f);
        int k = ScreenUtil.getDefaultTextColor(!isHoveredOrFocused());
        this.renderScrollingString(guiGraphics, minecraft.font, 2, k | Mth.ceil(this.alpha * 255.0f) << 24);
    }

    @Override
    public @Nullable Component getAction(Context context) {
        return isFocused() && context instanceof KeyContext c && c.key() == InputConstants.KEY_RETURN ? canChangeValue ? LegacyComponents.LOCK : LegacyComponents.UNLOCK : null;
    }
}
