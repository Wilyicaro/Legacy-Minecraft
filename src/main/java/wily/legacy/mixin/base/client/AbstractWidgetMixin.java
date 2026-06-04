package wily.legacy.mixin.base.client;

import net.minecraft.util.Util;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
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
import wily.legacy.util.client.LegacySoundUtil;

@Mixin(AbstractWidget.class)
public abstract class AbstractWidgetMixin implements ControlTooltip.ActionHolder {
    @Shadow
    protected boolean isHovered;
    @Unique
    private long lastHovered = -1;
    @Unique
    private boolean playedFocusSound = false;

    @Shadow
    public abstract boolean isFocused();

    @Inject(method = "extractRenderState", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/AbstractWidget;extractWidgetRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIF)V"))
    private void extractRenderState(GuiGraphicsExtractor GuiGraphicsExtractor, int i, int j, float f, CallbackInfo ci) {
        if (isHovered) {
            if (lastHovered == -1) {
                lastHovered = Util.getMillis();
            }
            if (!playedFocusSound && Util.getMillis() - lastHovered >= 10 && LegacyOptions.hoverFocusSound.get()) {
                LegacySoundUtil.playSimpleUISound(LegacyRegistries.FOCUS.get(), 1.0f);
                playedFocusSound = true;
            }
        } else {
            lastHovered = -1;
            playedFocusSound = false;
        }
    }

    @Redirect(method = "nextFocusPath", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/AbstractWidget;isActive()Z"))
    public boolean nextFocusPath(AbstractWidget instance) {
        return instance.visible;
    }

    @Override
    public @Nullable Component getAction(Context context) {
        return ControlTooltip.getSelectAction((GuiEventListener) this, context);
    }
}
