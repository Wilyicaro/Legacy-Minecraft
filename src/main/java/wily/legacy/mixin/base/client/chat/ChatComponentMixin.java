package wily.legacy.mixin.base.client.chat;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import net.minecraft.client.GuiMessage;
import net.minecraft.client.GuiMessageTag;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.network.chat.Style;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import org.joml.Matrix3x2f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;
import wily.legacy.client.CommonColor;
import wily.legacy.client.LegacyOptions;
import wily.legacy.client.screen.OverlayPanelScreen;
import wily.legacy.util.client.LegacyFontUtil;
import wily.legacy.util.client.LegacyRenderUtil;

@Mixin(ChatComponent.class)
public abstract class ChatComponentMixin {

    @Shadow
    @Final
    private Minecraft minecraft;

    @Shadow
    public abstract double getScale();

    @Shadow
    public abstract boolean isChatFocused();

    @Inject(method = "render(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/gui/Font;IIIZZ)V", at = @At(value = "HEAD"), cancellable = true)
    private void renderWithFont(CallbackInfo ci) {
        if (minecraft.screen != null && !isChatFocused()) {
            ci.cancel();
            return;
        }

        if (LegacyOptions.getUIMode().isSD()) LegacyFontUtil.defaultFontOverride = LegacyFontUtil.MOJANGLES_11_FONT;
    }

    @Inject(method = "render(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/gui/Font;IIIZZ)V", at = @At(value = "RETURN"))
    private void renderWithFontReturn(CallbackInfo ci) {
        LegacyFontUtil.defaultFontOverride = null;
    }

    //? if fabric {
    @ModifyArgs(method = "method_75802", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/ChatComponent$ChatGraphicsAccess;fill(IIIII)V"))
    private static void renderChatBg(Args args) {
        int safeZone = Math.round(LegacyRenderUtil.getChatSafeZone());
        args.set(0, (int) args.get(0) - safeZone);
        args.set(2, (int) args.get(2) + safeZone);
        args.set(4, ARGB.color(ARGB.alpha(args.get(4)), CommonColor.CHAT_BACKGROUND.get()));
    }

    @Inject(method = "method_75801", at = @At(value = "INVOKE", target = "Lorg/joml/Matrix3x2f;translate(FF)Lorg/joml/Matrix3x2f;"), remap = false)
    private static void offsetChat(CallbackInfo ci, @Local(argsOnly = true) LocalRef<Matrix3x2f> matrix3x2f) {
        matrix3x2f.set(matrix3x2f.get().translate(LegacyRenderUtil.getChatSafeZone(), LegacyRenderUtil.getHUDDistance() - 42));
    }
    //?} else {
    /*@ModifyArgs(method = "lambda$render$1", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/ChatComponent$ChatGraphicsAccess;fill(IIIII)V"))
    private static void renderChatBg(Args args) {
        int safeZone = Math.round(LegacyRenderUtil.getChatSafeZone());
        args.set(0, (int) args.get(0) - safeZone);
        args.set(2, (int) args.get(2) + safeZone);
        args.set(4, ARGB.color(ARGB.alpha(args.get(4)), CommonColor.CHAT_BACKGROUND.get()));
    }

    @Inject(method = "lambda$render$0", at = @At(value = "INVOKE", target = "Lorg/joml/Matrix3x2f;translate(FF)Lorg/joml/Matrix3x2f;"), remap = false)
    private static void offsetChat(CallbackInfo ci, @Local(argsOnly = true) LocalRef<Matrix3x2f> matrix3x2f) {
        matrix3x2f.set(matrix3x2f.get().translate(LegacyRenderUtil.getChatSafeZone(), LegacyRenderUtil.getHUDDistance() - 42));
    }
    *///?}

    @ModifyArgs(method = "render(Lnet/minecraft/client/gui/components/ChatComponent$ChatGraphicsAccess;IIZ)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/ChatComponent$ChatGraphicsAccess;fill(IIIII)V", ordinal = 0))
    private void renderChatBgSecond(Args args) {
        renderChatBg(args);
    }

    @Inject(method = "getWidth(D)I", at = @At(value = "HEAD"), cancellable = true)
    private static void getWidth(double d, CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(Mth.floor(d * (Minecraft.getInstance().getWindow().getGuiScaledWidth() - (4 + LegacyRenderUtil.getChatSafeZone()) * 2)));
    }

    @Inject(method = "addMessageToDisplayQueue", at = @At("HEAD"))
    private void changeClickedChatFontWidth(GuiMessage guiMessage, CallbackInfo ci) {
        if (LegacyOptions.getUIMode().isSD())
            LegacyFontUtil.defaultFontOverride = LegacyFontUtil.MOJANGLES_11_FONT;
    }

    @Inject(method = "addMessageToDisplayQueue", at = @At("RETURN"))
    private void changeClickedChatFontWidthAfter(GuiMessage guiMessage, CallbackInfo ci) {
        LegacyFontUtil.defaultFontOverride = null;
    }

    @Inject(method = "isChatFocused", at = @At(value = "HEAD"), cancellable = true)
    private void isChatFocused(CallbackInfoReturnable<Boolean> cir) {
        if (minecraft.screen instanceof OverlayPanelScreen s && s.parent instanceof ChatScreen)
            cir.setReturnValue(true);
    }
}