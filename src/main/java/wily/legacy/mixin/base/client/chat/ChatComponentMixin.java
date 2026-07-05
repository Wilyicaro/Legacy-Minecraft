package wily.legacy.mixin.base.client.chat;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.GuiMessage;
import net.minecraft.client.GuiMessageTag;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.screens.ChatScreen;
//? if <1.21.1 {
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MessageSignature;
//?}
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
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
import wily.legacy.Legacy4JClient;
import wily.legacy.client.CommonColor;
import wily.legacy.client.LegacyOptions;
import wily.legacy.client.screen.LegacyIconHolder;
import wily.legacy.client.screen.OverlayPanelScreen;
import wily.legacy.util.ScreenUtil;

@Mixin(ChatComponent.class)
public abstract class ChatComponentMixin {

    @Shadow @Final private Minecraft minecraft;

    @Shadow public abstract double getScale();

    @Shadow protected abstract int getLineHeight();

    @Shadow protected abstract boolean isChatFocused();

    @Shadow protected abstract double screenToChatX(double d);

    @ModifyArg(method = "render",at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;fill(IIIII)V", ordinal = 0), index = 4)
    private int changeChatBackground(int i) {
        return CommonColor.CHAT_BACKGROUND.get() + i;
    }
    @ModifyArg(method = "render",at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;fill(IIIII)V", ordinal = 0), index = 0)
    private int changeChatX(int i) {
        return i-Math.round(ScreenUtil.getChatSafeZone());
    }
    @ModifyArg(method = "render",at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;fill(IIIII)V", ordinal = 0), index = 2)
    private int changeChatXD(int i) {
        return i+Math.round(ScreenUtil.getChatSafeZone());
    }
    @ModifyArg(method = "render",at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;fill(IIIII)V", ordinal = 1), index = 0)
    private int changeMessageTagX(int i) {
        return i-Math.round(ScreenUtil.getChatSafeZone());
    }
    @ModifyArg(method = "render",at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;fill(IIIII)V", ordinal = 1), index = 2)
    private int changeMessageTagXD(int i) {
        return i-Math.round(ScreenUtil.getChatSafeZone());
    }
    @ModifyArg(method = "render",at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;fill(IIIII)V", ordinal = 1), index = 4)
    private int hideMessageTagColor(int i) {
        return LegacyOptions.displayChatIndicators.get() ? i : 0;
    }

    @ModifyArg(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/util/FormattedCharSequence;III)I"), index = 1)
    private FormattedCharSequence changeChatTextColor(FormattedCharSequence text) {
        if (!CommonColor.CHAT_TEXT.isOverridden()) return text;
        int color = CommonColor.CHAT_TEXT.get() & 0x00FFFFFF;
        return sink -> text.accept((index, style, codePoint) -> sink.accept(index, chatTextStyle(style, color), codePoint));
    }

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/util/FormattedCharSequence;III)I"))
    private void changeChatFont(GuiGraphics guiGraphics, int i, int j, int k, /*? if >=1.20.5 {*/boolean bl,/*?}*/ CallbackInfo ci) {
        if (LegacyOptions.getUIMode().isSD()) Legacy4JClient.defaultFontOverride = LegacyIconHolder.MOJANGLES_11_FONT;
    }

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/util/FormattedCharSequence;III)I", shift = At.Shift.AFTER))
    private void changeChatFontAfter(GuiGraphics guiGraphics, int i, int j, int k, /*? if >=1.20.5 {*/boolean bl,/*?}*/ CallbackInfo ci) {
        Legacy4JClient.defaultFontOverride = null;
    }

    private static Style chatTextStyle(Style style, int color) {
        return style.getColor() == null ? style.withColor(color) : style;
    }

    @Redirect(method = "getMessageTagAt",at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/ChatComponent;screenToChatX(D)D"))
    private double changeMessageTagXPos(ChatComponent instance, double d) {
        return screenToChatX(d) + Math.round(ScreenUtil.getChatSafeZone());
    }
    @Inject(method = "getMessageTagAt", at = @At("HEAD"), cancellable = true)
    private void getMessageTagAt(double d, double e, CallbackInfoReturnable<GuiMessageTag> cir) {
        if (!LegacyOptions.displayChatIndicators.get()) cir.setReturnValue(null);
    }

    @Inject(method = "render",at = @At(value = "HEAD"), cancellable = true)
    private void stopRender(GuiGraphics guiGraphics, int i, int j, int k, /*? if >=1.20.5 {*/boolean bl,/*?}*/ CallbackInfo ci) {
        if (minecraft.screen != null && !isChatFocused()) ci.cancel();
    }
    @Inject(method = "render",at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;pushPose()V", shift = At.Shift.AFTER, ordinal = 0))
    private void changeRenderTranslation(GuiGraphics guiGraphics, int i, int j, int k, /*? if >=1.20.5 {*/boolean bl,/*?}*/ CallbackInfo ci) {
        RenderSystem.setShaderColor(1.0f,1.0f,1.0f,1.0f);
        double scale = Math.max(getScale(), 0.0001D);
        guiGraphics.pose().translate(ScreenUtil.getChatSafeZone() / scale, (ScreenUtil.getHUDDistance() - 42) / scale,0.0F);
    }
    @Inject(method = "screenToChatX",at = @At("RETURN"), cancellable = true)
    private void screenToChatX(double d, CallbackInfoReturnable<Double> cir) {
        cir.setReturnValue(cir.getReturnValue() - ScreenUtil.getChatSafeZone());
    }
    @Inject(method = "screenToChatY",at = @At("HEAD"), cancellable = true)
    private void screenToChatY(double d, CallbackInfoReturnable<Double> cir) {
        double e = (double)this.minecraft.getWindow().getGuiScaledHeight() - d - 40 + ScreenUtil.getHUDDistance() - 42;
        cir.setReturnValue(e / (this.getScale() * (double)this.getLineHeight()));
    }
    @Inject(method = "getWidth(D)I",at = @At(value = "HEAD"), cancellable = true)
    private static void getWidth(double d, CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(Mth.floor(d * (Minecraft.getInstance().getWindow().getGuiScaledWidth() - (4 + ScreenUtil.getChatSafeZone()) * 2)));
    }

    //? if <1.21.1 {
    @Inject(method = "addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;ILnet/minecraft/client/GuiMessageTag;Z)V", at = @At("HEAD"))
    private void changeQueuedChatFont(Component component, MessageSignature messageSignature, int i, GuiMessageTag guiMessageTag, boolean bl, CallbackInfo ci) {
        if (LegacyOptions.getUIMode().isSD()) Legacy4JClient.defaultFontOverride = LegacyIconHolder.MOJANGLES_11_FONT;
    }

    @Inject(method = "addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;ILnet/minecraft/client/GuiMessageTag;Z)V", at = @At("RETURN"))
    private void changeQueuedChatFontAfter(Component component, MessageSignature messageSignature, int i, GuiMessageTag guiMessageTag, boolean bl, CallbackInfo ci) {
        Legacy4JClient.defaultFontOverride = null;
    }
    //?} else {
    /*@Inject(method = "addMessageToDisplayQueue(Lnet/minecraft/client/GuiMessage;)V", at = @At("HEAD"))
    private void changeQueuedChatFont(GuiMessage guiMessage, CallbackInfo ci) {
        if (LegacyOptions.getUIMode().isSD()) Legacy4JClient.defaultFontOverride = LegacyIconHolder.MOJANGLES_11_FONT;
    }

    @Inject(method = "addMessageToDisplayQueue(Lnet/minecraft/client/GuiMessage;)V", at = @At("RETURN"))
    private void changeQueuedChatFontAfter(GuiMessage guiMessage, CallbackInfo ci) {
        Legacy4JClient.defaultFontOverride = null;
    }
    *///?}

    @Inject(method = "isChatFocused",at = @At(value = "HEAD"), cancellable = true)
    private void isChatFocused(CallbackInfoReturnable<Boolean> cir) {
        if (minecraft.screen instanceof OverlayPanelScreen s && s.parent instanceof ChatScreen) cir.setReturnValue(true);
    }
}
