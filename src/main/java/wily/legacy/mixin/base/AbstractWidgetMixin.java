package wily.legacy.mixin.base;

import net.minecraft.Util;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.client.CommonValue;
import wily.legacy.client.screen.ControlTooltip;

@Mixin(AbstractWidget.class)
public abstract class AbstractWidgetMixin implements ControlTooltip.ActionHolder {
    @Shadow public abstract boolean isFocused();

    @Unique
    long lastTimePressed;

    @Redirect(method = "nextFocusPath", at = @At(value = "FIELD", target = "Lnet/minecraft/client/gui/components/AbstractWidget;active:Z"))
    public boolean nextFocusPath(AbstractWidget instance) {
        return true;
    }

    @Inject(method = "setFocused", at = @At("HEAD"))
    private void setFocused(boolean bl, CallbackInfo ci){
        if (bl) lastTimePressed = Util.getMillis();
    }

    @Override
    public @Nullable Component getAction(Context context) {
        return ControlTooltip.getSelectAction((GuiEventListener) this,context);
    }

    @Redirect(method = /*? if >1.20.1 {*/"renderScrollingString(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;IIIIII)V"/*?} else {*//*"renderScrollingString(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;IIIII)V"*//*?}*/, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;III)I"))
    private static int renderScrollingString(GuiGraphics instance, Font arg, Component arg2, int i, int j, int k) {
        return instance.drawString(arg, arg2, i, j, k, CommonValue.WIDGET_TEXT_SHADOW.get());
    }
    @Redirect(method = /*? if >1.20.1 {*/"renderScrollingString(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;IIIIII)V"/*?} else {*//*"renderScrollingString(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;IIIII)V"*//*?}*/, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;drawCenteredString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;III)V"))
    private static void renderCenteredScrollingString(GuiGraphics instance, Font arg, Component arg2, int i, int j, int k) {
        instance.drawString(arg, arg2, i - arg.width(arg2) / 2, j, k, CommonValue.WIDGET_TEXT_SHADOW.get());
    }
}
