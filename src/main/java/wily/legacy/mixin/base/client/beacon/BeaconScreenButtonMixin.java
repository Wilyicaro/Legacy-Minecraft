package wily.legacy.mixin.base.client.beacon;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.factoryapi.base.client.FactoryGuiGraphics;
import wily.legacy.util.LegacySprites;

@Mixin(targets = { "net.minecraft.client.gui.screens.inventory.BeaconScreen$BeaconScreenButton" })
public abstract class BeaconScreenButtonMixin extends AbstractButton {

    public BeaconScreenButtonMixin(int i, int j, int k, int l, Component component) {
        super(i, j, k, l, component);
    }

    //? if <=1.20.1 {
    @Shadow
    private boolean selected;

    @Shadow
    protected abstract void renderIcon(GuiGraphics guiGraphics);

    @Inject(method = "renderWidget", at = @At("RETURN"))
    private void renderWidget(GuiGraphics graphics, int i, int j, float f, CallbackInfo ci) {
        ResourceLocation sprite;
        if (!active) {
            sprite = LegacySprites.BEACON_BUTTON_DISABLED_SPRITE;
        } else if (selected) {
            sprite = LegacySprites.BEACON_BUTTON_SELECTED_SPRITE;
        } else if (isHoveredOrFocused()) {
            sprite = LegacySprites.BEACON_BUTTON_HIGHLIGHTED_SPRITE;
        } else {
            sprite = LegacySprites.BEACON_BUTTON_SPRITE;
        }

        FactoryGuiGraphics.of(graphics).blitSprite(sprite, getX(), getY(), getWidth(), getHeight());
        renderIcon(graphics);
    }
    //?}
}
