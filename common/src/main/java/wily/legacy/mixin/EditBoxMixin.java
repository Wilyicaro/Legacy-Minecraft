package wily.legacy.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.legacy.client.LegacyGuiGraphics;
import wily.legacy.client.controller.ControllerBinding;
import wily.legacy.client.screen.KeyboardScreen;
import wily.legacy.util.LegacySprites;

@Mixin(EditBox.class)
public abstract class EditBoxMixin extends AbstractWidget {

    @Shadow protected abstract boolean isBordered();

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

    @Redirect(method = "renderWidget", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;fill(IIIII)V"))
    private void renderWidget(PoseStack instance, int i, int j, int k, int l, int m){
    }
    @Inject(method = "renderWidget", at = @At("HEAD"))
    private void renderWidget(PoseStack poseStack, int i, int j, float f, CallbackInfo ci){
        if (isBordered()) {
            LegacyGuiGraphics.of(poseStack).blitSprite(LegacySprites.TEXT_FIELD, getX(), getY(), getWidth(), getHeight());
            if (isHoveredOrFocused()) LegacyGuiGraphics.of(poseStack).blitSprite(LegacySprites.HIGHLIGHTED_TEXT_FIELD, getX() - 1, getY() - 1, getWidth() + 2, getHeight() + 2);
        }
    }
}
