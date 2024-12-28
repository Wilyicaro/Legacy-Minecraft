package wily.legacy.mixin.base;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.factoryapi.base.client.FactoryGuiGraphics;
import wily.legacy.Legacy4JClient;
import wily.legacy.client.CommonColor;
import wily.legacy.client.screen.ControlTooltip;
import wily.legacy.client.screen.KeyboardScreen;
import wily.legacy.util.LegacySprites;

@Mixin(EditBox.class)
public abstract class EditBoxMixin extends AbstractWidget implements ControlTooltip.ActionHolder {
    //? if >1.20.2 {
    @Shadow private long focusedTime;
    //?}

    @Shadow private int cursorPos;

    @Shadow private int displayPos;

    @Shadow @Final private Font font;

    @Shadow private String value;

    @Shadow public abstract int getInnerWidth();

    @Shadow private int textColor;

    @Shadow public abstract boolean isBordered();

    @Redirect(method = "<init>(Lnet/minecraft/client/gui/Font;IIIILnet/minecraft/client/gui/components/EditBox;Lnet/minecraft/network/chat/Component;)V", at = @At(value = "FIELD", target = "Lnet/minecraft/client/gui/components/EditBox;textColor:I", opcode = Opcodes.PUTFIELD))
    private void init(EditBox instance, int value){
        textColor = CommonColor.WIDGET_TEXT.get();
    }
    public EditBoxMixin(int i, int j, int k, int l, Component component) {
        super(i, j, k, l, component);
    }
    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void keyPressed(int i, int j, int k, CallbackInfoReturnable<Boolean> cir){
        Screen screen = Minecraft.getInstance().screen;
        if (KeyboardScreen.isOpenKey(i) && screen != null){
            Minecraft.getInstance().setScreen(KeyboardScreen.fromStaticListener(this,screen));
            cir.setReturnValue(true);
        }
    }
    @Inject(method = "onClick", at = @At("HEAD"), cancellable = true)
    private void onClick(double d, double e, CallbackInfo ci){
        Screen screen = Minecraft.getInstance().screen;
        if (Screen.hasShiftDown() || Legacy4JClient.controllerManager.isControllerTheLastInput) {
            Minecraft.getInstance().setScreen(KeyboardScreen.fromStaticListener(this, screen));
            ci.cancel();
        }
    }

    @Redirect(method = "renderWidget", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/EditBox;isBordered()Z"))
    private boolean renderWidget(EditBox instance, GuiGraphics guiGraphics){
        if (isBordered()) {
            FactoryGuiGraphics.of(guiGraphics).blitSprite(LegacySprites.TEXT_FIELD, getX(), getY(), getWidth(), getHeight());
            if (isHoveredOrFocused())
                FactoryGuiGraphics.of(guiGraphics).blitSprite(LegacySprites.HIGHLIGHTED_TEXT_FIELD, getX() - 1, getY() - 1, getWidth() + 2, getHeight() + 2);
        }
        return false;
    }

    @ModifyArg(method = "renderWidget", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;fill(Lnet/minecraft/client/renderer/RenderType;IIIII)V", ordinal = 0), index = 5)
    public int renderWidget(int i) {
        return CommonColor.WIDGET_TEXT.get() | 0xFF000000;
    }
    @ModifyVariable(method = "renderWidget", at = @At(value = "STORE"), ordinal = 1)
    public boolean renderWidget(boolean bl) {
        int l = this.cursorPos - this.displayPos;
        String string = this.font.plainSubstrByWidth(this.value.substring(this.displayPos), this.getInnerWidth());
        return l >= 0 && l <= string.length() && this.isFocused() && (Util.getMillis()/*? if >1.20.1 {*/ - this.focusedTime/*?}*/) / 180L % 2 == 0L;
    }

    @Redirect(method = "renderWidget", at = @At(value = "INVOKE", target = /*? if neoforge && >=1.21 {*//*"Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Ljava/lang/String;IIIZ)I"*//*?} else {*/"Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Ljava/lang/String;III)I"/*?}*/, ordinal = 1))
    public int renderWidget(GuiGraphics instance, Font arg, String string, int i, int j, int k/*? if neoforge && >=1.21 {*//*, boolean bl *//*?}*/) {
        instance.pose().pushPose();
        instance.pose().translate(i-(cursorPos == 0 ? 3 : 4),j+8.5f,0);
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
