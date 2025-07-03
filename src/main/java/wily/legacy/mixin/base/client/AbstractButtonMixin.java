package wily.legacy.mixin.base.client;

import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.AbstractWidget;
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

    @Inject(method = "onClick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/AbstractButton;onPress()V"))
    private void onPress(double d, double e, CallbackInfo ci){
        lastTimePressed = Util.getMillis();
    }
    @Inject(method = "keyPressed", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/AbstractButton;onPress()V"))
    private void onPress(int i, int j, int k, CallbackInfoReturnable<Boolean> cir){
        lastTimePressed = Util.getMillis();
    }
    //? if >1.20.1 {
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
    //?} else {
    /*@Shadow public abstract void renderString(GuiGraphics arg, Font arg2, int i);

    @Inject(method = "renderWidget", at = @At("HEAD"), cancellable = true)
    protected void renderWidget(GuiGraphics guiGraphics, int i, int j, float f, CallbackInfo ci) {
        ci.cancel();
        alpha = active ? 1 : 0.8f;
        Minecraft minecraft = Minecraft.getInstance();
        guiGraphics.setColor(1.0F, 1.0F, 1.0F, this.alpha);
        FactoryScreenUtil.enableBlend();
        FactoryScreenUtil.enableDepthTest();
        FactoryGuiGraphics.of(guiGraphics).blitSprite(isHoveredOrFocused() ? LegacySprites.BUTTON_HIGHLIGHTED : LegacySprites.BUTTON , this.getX(), this.getY(), this.getWidth(), this.getHeight());
        guiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        int k = ScreenUtil.getDefaultTextColor(!isHoveredOrFocused() || Util.getMillis() - lastTimePressed <= 150);
        this.renderString(guiGraphics, minecraft.font, k | Mth.ceil(this.alpha * 255.0F) << 24);
    }
    *///?}

}
