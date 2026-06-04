package wily.legacy.mixin.base.client;

import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.ARGB;
import net.minecraft.util.Util;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.legacy.client.CommonValue;
import wily.legacy.util.LegacySprites;
import wily.legacy.util.client.LegacyRenderUtil;

@Mixin(AbstractButton.class)
public abstract class AbstractButtonMixin extends AbstractWidget {
    @Unique
    long lastTimePressed;

    public AbstractButtonMixin(int i, int j, int k, int l, Component component) {
        super(i, j, k, l, component);
    }

    @Inject(method = "onClick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/AbstractButton;onPress(Lnet/minecraft/client/input/InputWithModifiers;)V"))
    private void onPress(MouseButtonEvent mouseButtonEvent, boolean bl, CallbackInfo ci) {
        lastTimePressed = Util.getMillis();
    }

    @Inject(method = "keyPressed", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/AbstractButton;onPress(Lnet/minecraft/client/input/InputWithModifiers;)V"))
    private void onPress(KeyEvent keyEvent, CallbackInfoReturnable<Boolean> cir) {
        lastTimePressed = Util.getMillis();
    }

    @ModifyArg(method = "extractDefaultLabel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/AbstractButton;extractScrollingStringOverContents(Lnet/minecraft/client/gui/ActiveTextCollector;Lnet/minecraft/network/chat/Component;I)V"))
    protected Component getMessage(Component original) {
        MutableComponent copy = original.copy().withColor(LegacyRenderUtil.getDefaultTextColor(!isHoveredOrFocused() || Util.getMillis() - lastTimePressed <= 150));
        if (!CommonValue.WIDGET_TEXT_SHADOW.get()) copy.withoutShadow();
        return copy;
    }

    @Inject(method = "extractWidgetRenderState", at = @At("HEAD"))
    protected void extractWidgetRenderState(GuiGraphicsExtractor GuiGraphicsExtractor, int i, int j, float f, CallbackInfo ci) {
        alpha = active ? 1 : 0.8f;
    }

    @Redirect(method = "extractDefaultSprite", at = @At(value = "FIELD", target = "Lnet/minecraft/client/gui/components/AbstractButton;active:Z", opcode = Opcodes.GETFIELD))
    protected boolean extractDefaultSprite(AbstractButton instance) {
        return true;
    }

    @Inject(method = "extractDefaultSprite", at = @At("HEAD"))
    protected void extractDefaultSpriteHead(GuiGraphicsExtractor graphics, CallbackInfo ci) {
        if (LegacyRenderUtil.hasAutoFocusButtonAnimation()) {
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, LegacySprites.BUTTON, this.getX(), this.getY(), this.getWidth(), this.getHeight(), ARGB.white(this.alpha));
            float timer = (Util.getMillis() % 1200) / 1200.0f;
            alpha *= 0.5f + (timer >= 0.5f ? 1 - timer : timer);
        }
    }

    @Inject(method = "extractDefaultSprite", at = @At("RETURN"))
    protected void extractDefaultSpriteReturn(GuiGraphicsExtractor graphics, CallbackInfo ci) {
        if (LegacyRenderUtil.hasAutoFocusButtonAnimation())
            alpha = active ? 1 : 0.8f;
    }

}
