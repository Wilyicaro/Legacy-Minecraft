package wily.legacy.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.legacy.client.CommonColor;
import wily.legacy.client.screen.OverlayPanelScreen;
import wily.legacy.util.ScreenUtil;

@Mixin(ChatComponent.class)
public abstract class ChatComponentMixin {

    @Shadow @Final private Minecraft minecraft;

    @Shadow public abstract double getScale();

    @Shadow protected abstract int getLineHeight();

    @Shadow protected abstract boolean isChatFocused();

    @ModifyArg(method = "render",at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;fill(IIIII)V", ordinal = 0), index = 4)
    private int changeChatBackground(int i) {
        return CommonColor.CHAT_BACKGROUND.get() + i;
    }
    @Inject(method = "render",at = @At(value = "HEAD"), cancellable = true)
    private void stopRender(GuiGraphics guiGraphics, int i, int j, int k, CallbackInfo ci) {
        if (minecraft.screen != null && !isChatFocused()) ci.cancel();
    }
    @Inject(method = "render",at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;pushPose()V", shift = At.Shift.AFTER, ordinal = 0))
    private void changeRenderY(GuiGraphics guiGraphics, int i, int j, int k, CallbackInfo ci) {
        RenderSystem.setShaderColor(1.0f,1.0f,1.0f,1.0f);
        guiGraphics.pose().translate(0.0F, ScreenUtil.getHUDDistance() - 42,0.0F);
    }
    @Inject(method = "screenToChatY",at = @At("HEAD"), cancellable = true)
    private void screenToChatY(double d, CallbackInfoReturnable<Double> cir) {
        double e = (double)this.minecraft.getWindow().getGuiScaledHeight() - d - 40 + ScreenUtil.getHUDDistance() - 42;
        cir.setReturnValue(e / (this.getScale() * (double)this.getLineHeight()));
    }
    @Inject(method = "getWidth(D)I",at = @At(value = "HEAD"), cancellable = true)
    private static void getWidth(double d, CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(Mth.floor(d * (Minecraft.getInstance().getWindow().getGuiScaledWidth() - 8)));
    }
    @Inject(method = "isChatFocused",at = @At(value = "HEAD"), cancellable = true)
    private void isChatFocused(CallbackInfoReturnable<Boolean> cir) {
        if (minecraft.screen instanceof OverlayPanelScreen s && s.parent instanceof ChatScreen) cir.setReturnValue(true);
    }
}
