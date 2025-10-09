package wily.legacy.mixin.base.client;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.components.MultilineTextField;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
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

    @Shadow
    private long focusedTime;

    @Shadow @Final private MultilineTextField textField;

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void keyPressed(KeyEvent keyEvent, CallbackInfoReturnable<Boolean> cir){
        Screen screen = Minecraft.getInstance().screen;
        if (KeyboardScreen.isOpenKey(keyEvent.key()) && screen != null){
            Minecraft.getInstance().setScreen(KeyboardScreen.fromStaticListener(this,screen));
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "onClick", at = @At("HEAD"), cancellable = true)
    private void onClick(MouseButtonEvent event, boolean bl, CallbackInfo ci){
        Screen screen = Minecraft.getInstance().screen;
        if (event.hasShiftDown() || Legacy4JClient.controllerManager.isControllerTheLastInput()) {
            Minecraft.getInstance().setScreen(KeyboardScreen.fromStaticListener(this, screen));
            ci.cancel();
        }
    }



    @ModifyVariable(method = "renderContents", at = @At(value = "STORE"), ordinal = 0)
    public boolean renderWidget(boolean bl) {
        return this.isFocused() && (Util.getMillis()/*? if >1.20.1 {*/ - this.focusedTime/*?}*/) / 180L % 2 == 0L;
    }

    @Redirect(method = "renderContents", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Ljava/lang/String;IIIZ)V", ordinal = 3))
    public void renderWidget(GuiGraphics instance, Font arg, String string, int i, int j, int k, boolean bl) {
        instance.pose().pushMatrix();
        instance.pose().translate(i-(textField.cursor() == 0 ? 3 : 4),j + 8.5f);
        instance.pose().scale(6,1.5f);
        instance.fill(0,0,1,1, k);
        instance.pose().popMatrix();
    }

    @Override
    public @Nullable Component getAction(Context context) {
        return isFocused() ? context.actionOfContext(KeyContext.class, ControlTooltip::getKeyboardAction) : null;
    }
}
