package wily.legacy.mixin.base.client.chat;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
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
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.legacy.client.CommonColor;
import wily.legacy.client.screen.OverlayPanelScreen;
import wily.legacy.util.client.LegacyRenderUtil;

@Mixin(ChatComponent.class)
public abstract class ChatComponentMixin {

    @Shadow @Final private Minecraft minecraft;

    @Shadow public abstract double getScale();

    @Shadow protected abstract int getLineHeight();

    @Shadow
    public abstract boolean isChatFocused();

    @Shadow protected abstract double screenToChatX(double d);

    @WrapOperation(method = "method_71992",at = @At(value = "INVOKE", target = "Lnet/minecraft/util/ARGB;color(FI)I", ordinal = 0), remap = false)
    private int changeChatBackground(float f, int i, Operation<Integer> original) {
        return original.call(f, CommonColor.CHAT_BACKGROUND.get().intValue());
    }

    @ModifyArg(method = "method_71992",at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;fill(IIIII)V", ordinal = 0), index = 0, remap = false)
    private int changeChatX(int i) {
        return i-Math.round(LegacyRenderUtil.getChatSafeZone());
    }

    @ModifyArg(method = "method_71992",at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;fill(IIIII)V", ordinal = 0), index = 2, remap = false)
    private int changeChatXD(int i) {
        return i+Math.round(LegacyRenderUtil.getChatSafeZone());
    }

    @ModifyArg(method = "method_71992",at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;fill(IIIII)V", ordinal = 1), index = 0, remap = false)
    private int changeMessageTagX(int i) {
        return i-Math.round(LegacyRenderUtil.getChatSafeZone());
    }

    @ModifyArg(method = "method_71992",at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;fill(IIIII)V", ordinal = 1), index = 2, remap = false)
    private int changeMessageTagXD(int i) {
        return i-Math.round(LegacyRenderUtil.getChatSafeZone());
    }

    @Redirect(method = "getMessageTagAt",at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/ChatComponent;screenToChatX(D)D"))
    private double changeMessageTagXPos(ChatComponent instance, double d) {
        return screenToChatX(d) + Math.round(LegacyRenderUtil.getChatSafeZone());
    }

    @Inject(method = "render",at = @At(value = "HEAD"), cancellable = true)
    private void stopRender(GuiGraphics guiGraphics, int i, int j, int k, /*? if >=1.20.5 {*/boolean bl,/*?}*/ CallbackInfo ci) {
        if (minecraft.screen != null && !isChatFocused()) ci.cancel();
    }
    @Inject(method = "render",at = @At(value = "INVOKE", target = "Lorg/joml/Matrix3x2fStack;pushMatrix()Lorg/joml/Matrix3x2fStack;", shift = At.Shift.AFTER, ordinal = 0, remap = false))
    private void changeRenderTranslation(GuiGraphics guiGraphics, int i, int j, int k, /*? if >=1.20.5 {*/boolean bl,/*?}*/ CallbackInfo ci) {
        guiGraphics.pose().translate(LegacyRenderUtil.getChatSafeZone(), LegacyRenderUtil.getHUDDistance() - 42);
    }
    @Inject(method = "screenToChatX",at = @At("RETURN"), cancellable = true)
    private void screenToChatX(double d, CallbackInfoReturnable<Double> cir) {
        cir.setReturnValue(cir.getReturnValue() - LegacyRenderUtil.getChatSafeZone());
    }
    @Inject(method = "screenToChatY",at = @At("HEAD"), cancellable = true)
    private void screenToChatY(double d, CallbackInfoReturnable<Double> cir) {
        double e = (double)this.minecraft.getWindow().getGuiScaledHeight() - d - 40 + LegacyRenderUtil.getHUDDistance() - 42;
        cir.setReturnValue(e / (this.getScale() * (double)this.getLineHeight()));
    }
    @Inject(method = "getWidth(D)I",at = @At(value = "HEAD"), cancellable = true)
    private static void getWidth(double d, CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(Mth.floor(d * (Minecraft.getInstance().getWindow().getGuiScaledWidth() - (4 + LegacyRenderUtil.getChatSafeZone()) * 2)));
    }
    @Inject(method = "isChatFocused",at = @At(value = "HEAD"), cancellable = true)
    private void isChatFocused(CallbackInfoReturnable<Boolean> cir) {
        if (minecraft.screen instanceof OverlayPanelScreen s && s.parent instanceof ChatScreen) cir.setReturnValue(true);
    }
}
