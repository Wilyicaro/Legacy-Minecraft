package wily.legacy.mixin.base.client;

import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
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
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.legacy.util.client.LegacyRenderUtil;

@Mixin(AbstractButton.class)
public abstract class AbstractButtonMixin extends AbstractWidget {
    @Unique
    long lastTimePressed;

    public AbstractButtonMixin(int i, int j, int k, int l, Component component) {
        super(i, j, k, l, component);
    }

    @Inject(method = "onClick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/AbstractButton;onPress(Lnet/minecraft/client/input/InputWithModifiers;)V"))
    private void onPress(MouseButtonEvent mouseButtonEvent, boolean bl, CallbackInfo ci){
        lastTimePressed = Util.getMillis();
    }

    @Inject(method = "keyPressed", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/AbstractButton;onPress(Lnet/minecraft/client/input/InputWithModifiers;)V"))
    private void onPress(KeyEvent keyEvent, CallbackInfoReturnable<Boolean> cir){
        lastTimePressed = Util.getMillis();
    }

    @ModifyVariable(method = "renderWidget", at = @At(value = "STORE"), ordinal = 2)
    protected int renderWidget(int k) {
        return LegacyRenderUtil.getDefaultTextColor(!isHoveredOrFocused() || Util.getMillis() - lastTimePressed <= 150);
    }

    @Inject(method = "renderWidget", at = @At("HEAD"))
    protected void renderWidget(GuiGraphics guiGraphics, int i, int j, float f, CallbackInfo ci) {
        alpha = active ? 1 : 0.8f;
    }

    @Redirect(method = "renderWidget", at = @At(value = "FIELD", target = "Lnet/minecraft/client/gui/components/AbstractButton;active:Z", opcode = Opcodes.GETFIELD))
    protected boolean renderWidget(AbstractButton instance) {
        return true;
    }

}
