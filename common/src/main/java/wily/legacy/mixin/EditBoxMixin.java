package wily.legacy.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.legacy.client.controller.ControllerBinding;
import wily.legacy.client.screen.KeyboardScreen;

@Mixin(EditBox.class)
public abstract class EditBoxMixin extends AbstractWidget {
    @Shadow @Final private static WidgetSprites SPRITES;

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
        if ((Screen.hasShiftDown() || ControllerBinding.DOWN_BUTTON.bindingState.pressed)) {
            Minecraft.getInstance().setScreen(KeyboardScreen.fromEditBox(screen.children().indexOf(this), screen));
            ci.cancel();
        }
    }

    @Redirect(method = "renderWidget", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;blitSprite(Lnet/minecraft/resources/ResourceLocation;IIII)V"))
    private void renderWidget(GuiGraphics guiGraphics, ResourceLocation resourceLocation, int i, int j, int k, int l){
        guiGraphics.blitSprite(SPRITES.get(this.isActive(), false), i, j, k, l);
        if (isHoveredOrFocused()) guiGraphics.blitSprite(SPRITES.get(this.isActive(), true), i - 1, j - 1, k + 2, l + 2);
    }
}
