package wily.legacy.mixin;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.legacy.client.CommonColor;
import wily.legacy.client.controller.ControllerBinding;
import wily.legacy.client.screen.KeyboardScreen;

@Mixin(EditBox.class)
public abstract class EditBoxMixin extends AbstractWidget {
    @Shadow @Final private static WidgetSprites SPRITES;

    @Shadow private long focusedTime;

    @Shadow private int cursorPos;

    @Shadow private int displayPos;

    @Shadow @Final private Font font;

    @Shadow private String value;

    @Shadow public abstract int getInnerWidth();

    @Shadow private int textColor;

    @Redirect(method = "<init>(Lnet/minecraft/client/gui/Font;IIIILnet/minecraft/client/gui/components/EditBox;Lnet/minecraft/network/chat/Component;)V", at = @At(value = "FIELD", target = "Lnet/minecraft/client/gui/components/EditBox;textColor:I", opcode = Opcodes.PUTFIELD))
    private void init(EditBox instance, int value){
        textColor = CommonColor.WIDGET_TEXT.get();
    }
    public EditBoxMixin(int i, int j, int k, int l, Component component) {
        super(i, j, k, l, component);
    }
    @Inject(method = "keyPressed", at = @At("HEAD"))
    private void keyPressed(int i, int j, int k, CallbackInfoReturnable<Boolean> cir){
        Screen screen = Minecraft.getInstance().screen;
        if (!Screen.hasShiftDown() && KeyboardScreen.isOpenKey(i) && screen != null && screen.children().contains(this)){
            Minecraft.getInstance().setScreen(KeyboardScreen.fromEditBox(screen.children().indexOf(this),screen));
        }
    }
    @Inject(method = "onClick", at = @At("HEAD"), cancellable = true)
    private void onClick(double d, double e, CallbackInfo ci){
        Screen screen = Minecraft.getInstance().screen;
        if ((Screen.hasShiftDown() || ControllerBinding.DOWN_BUTTON.bindingState.pressed) && screen.children().contains(this)) {
            Minecraft.getInstance().setScreen(KeyboardScreen.fromEditBox(screen.children().indexOf(this), screen));
            ci.cancel();
        }
    }

    @Redirect(method = "renderWidget", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;blitSprite(Lnet/minecraft/resources/ResourceLocation;IIII)V"))
    private void renderWidget(GuiGraphics guiGraphics, ResourceLocation resourceLocation, int i, int j, int k, int l){
        guiGraphics.blitSprite(SPRITES.get(this.isActive(), false), i, j, k, l);
        if (isHoveredOrFocused()) guiGraphics.blitSprite(SPRITES.get(this.isActive(), true), i - 1, j - 1, k + 2, l + 2);
    }
    @Redirect(method = "renderWidget", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Ljava/lang/String;III)I", ordinal = 1))
    public int renderWidget(GuiGraphics instance, Font arg, String string, int i, int j, int k) {
        instance.pose().pushPose();
        instance.pose().translate(i-(cursorPos == 0 ? 3 : 4),j+8.5f,0);
        instance.pose().scale(6,1.5f,1f);
        instance.fill(0,0,1,1, CommonColor.WIDGET_TEXT.get() | 0xFF000000);
        instance.pose().popPose();
        return 0;
    }
    @ModifyArg(method = "renderWidget", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;fill(Lnet/minecraft/client/renderer/RenderType;IIIII)V", ordinal = 0), index = 5)
    public int renderWidget(int i) {
        return CommonColor.WIDGET_TEXT.get() | 0xFF000000;
    }
    @ModifyVariable(method = "renderWidget", at = @At(value = "STORE"), ordinal = 1)
    public boolean renderWidget(boolean bl) {
        int l = this.cursorPos - this.displayPos;
        String string = this.font.plainSubstrByWidth(this.value.substring(this.displayPos), this.getInnerWidth());
        return l >= 0 && l <= string.length() && this.isFocused() && (Util.getMillis() - this.focusedTime) / 180L % 2 == 0L;
    }

}
