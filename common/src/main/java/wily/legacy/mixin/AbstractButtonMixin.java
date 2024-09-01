package wily.legacy.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.Util;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.legacy.util.ScreenUtil;

@Mixin(AbstractButton.class)
public abstract class AbstractButtonMixin extends AbstractWidget {
    @Unique
    long lastTimePressed;

    public AbstractButtonMixin(int i, int j, int k, int l, Component component) {
        super(i, j, k, l, component);
    }

    @Inject(method = "onClick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/AbstractButton;onPress()V"))
    private void onPress(double d, double e, CallbackInfo ci){
        lastTimePressed = Util.getMillis();
    }
    @Inject(method = "keyPressed", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/AbstractButton;onPress()V"))
    private void onPress(int i, int j, int k, CallbackInfoReturnable<Boolean> cir){
        lastTimePressed = Util.getMillis();
    }
    @ModifyVariable(method = "renderWidget", at = @At(value = "STORE"), ordinal = 2)
    protected int renderWidget(int k) {
        return ScreenUtil.getDefaultTextColor(!isHoveredOrFocused() || Util.getMillis() - lastTimePressed <= 150);
    }
    @Inject(method = "renderWidget", at = @At("HEAD"))
    protected void renderWidget(PoseStack poseStack, int i, int j, float f, CallbackInfo ci) {
        alpha = active ? 1 : 0.8f;
    }
    @Inject(method = "getTextureY", at = @At("HEAD"), cancellable = true)
    protected void renderWidget(CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(46 + (isHoveredOrFocused() ? 2 : 1) * 20);
    }
}
