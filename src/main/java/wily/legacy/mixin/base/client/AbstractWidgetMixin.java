package wily.legacy.mixin.base.client;

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
import wily.legacy.client.LegacyOptions;
import wily.legacy.client.screen.ControlTooltip;
import wily.legacy.init.LegacyRegistries;
import wily.legacy.util.client.LegacyRenderUtil;

@Mixin(AbstractWidget.class)
public abstract class AbstractWidgetMixin implements ControlTooltip.ActionHolder {
    @Shadow public abstract boolean isFocused();

    @Shadow protected boolean isHovered;


    @Unique
    private long lastHovered = -1;

    @Unique
    private boolean playedFocusSound = false;

    @Inject(method = "render",at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/AbstractWidget;renderWidget(Lnet/minecraft/client/gui/GuiGraphics;IIF)V"))
    private void render(GuiGraphics guiGraphics, int i, int j, float f, CallbackInfo ci){
        if (isHovered) {
            if (lastHovered == -1){
                lastHovered = Util.getMillis();
            }
            if (!playedFocusSound && Util.getMillis() - lastHovered >= 10 && LegacyOptions.hoverFocusSound.get()) {
                LegacyRenderUtil.playSimpleUISound(LegacyRegistries.FOCUS.get(), 1.0f);
                playedFocusSound = true;
            }
        } else {
            lastHovered = -1;
            playedFocusSound = false;
        }
    }

    @Redirect(method = "nextFocusPath", at = @At(value = "FIELD", target = "Lnet/minecraft/client/gui/components/AbstractWidget;active:Z"))
    public boolean nextFocusPath(AbstractWidget instance) {
        return true;
    }

    @Override
    public @Nullable Component getAction(Context context) {
        return ControlTooltip.getSelectAction((GuiEventListener) this,context);
    }

    @Redirect(method = "renderScrollingString(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;IIIIII)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;III)V"))
    private static void renderScrollingString(GuiGraphics instance, Font arg, Component arg2, int i, int j, int k) {
        instance.drawString(arg, arg2, i, j, k, CommonValue.WIDGET_TEXT_SHADOW.get());
    }

    @Redirect(method = "renderScrollingString(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;IIIIII)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;drawCenteredString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;III)V"))
    private static void renderCenteredScrollingString(GuiGraphics instance, Font arg, Component arg2, int i, int j, int k) {
        instance.drawString(arg, arg2, i - arg.width(arg2) / 2, j, k, CommonValue.WIDGET_TEXT_SHADOW.get());
    }
}
