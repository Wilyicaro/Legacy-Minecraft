package wily.legacy.mixin.base;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.components.MultilineTextField;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.legacy.Legacy4JClient;
import wily.legacy.client.CommonColor;
import wily.legacy.client.screen.ControlTooltip;
import wily.legacy.client.screen.KeyboardScreen;

@Mixin(MultiLineEditBox.class)
public abstract class MultilineEditBoxMixin extends AbstractWidget implements ControlTooltip.ActionHolder {
    public MultilineEditBoxMixin(int i, int j, int k, int l, Component component) {
        super(i, j, k, l, component);
    }
    //? if >1.20.2 {
    @Shadow
    private long focusedTime;
    //?}

    @Shadow @Final private MultilineTextField textField;

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void keyPressed(int i, int j, int k, CallbackInfoReturnable<Boolean> cir){
        Screen screen = Minecraft.getInstance().screen;
        if (KeyboardScreen.isOpenKey(i) && screen != null){
            Minecraft.getInstance().setScreen(KeyboardScreen.fromStaticListener(this,screen));
            cir.setReturnValue(true);
        }
    }
    //? if <1.21.4 {
    @Override
    public void onClick(double d, double e){
        Screen screen = Minecraft.getInstance().screen;
        if (Screen.hasShiftDown() || Legacy4JClient.controllerManager.isControllerTheLastInput()) {
            Minecraft.getInstance().setScreen(KeyboardScreen.fromStaticListener(this, screen));
        }
    }
    //?} else {
    /*@Inject(method = "onClick", at = @At("HEAD"), cancellable = true)
    private void onClick(double d, double e, CallbackInfo ci){
        Screen screen = Minecraft.getInstance().screen;
        if (Screen.hasShiftDown() || Legacy4JClient.controllerManager.isControllerTheLastInput()) {
            Minecraft.getInstance().setScreen(KeyboardScreen.fromStaticListener(this, screen));
            ci.cancel();
        }
    }
    *///?}

    @ModifyArg(method = "renderContents", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;fill(IIIII)V", ordinal = 0), index = 4)
    public int renderWidget(int i) {
        return CommonColor.WIDGET_TEXT.get() | 0xFF000000;
    }

    @ModifyVariable(method = "renderContents", at = @At(value = "STORE"), ordinal = 0)
    public boolean renderWidget(boolean bl) {
        return this.isFocused() && (Util.getMillis()/*? if >1.20.1 {*/ - this.focusedTime/*?}*/) / 180L % 2 == 0L;
    }

    @Redirect(method = "renderContents", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Ljava/lang/String;III)I", ordinal = 3))
    public int renderWidget(GuiGraphics instance, Font arg, String string, int i, int j, int k) {
        instance.pose().pushPose();
        instance.pose().translate(i-(textField.cursor() == 0 ? 3 : 4),j+8.5f,0);
        instance.pose().scale(6,1.5f,1f);
        instance.fill(0,0,1,1, CommonColor.WIDGET_TEXT.get() | 0xFF000000);
        instance.pose().popPose();
        return 0;
    }

    @Override
    public @Nullable Component getAction(Context context) {
        return isFocused() ? context.actionOfContext(KeyContext.class, ControlTooltip::getKeyboardAction) : null;
    }
}
