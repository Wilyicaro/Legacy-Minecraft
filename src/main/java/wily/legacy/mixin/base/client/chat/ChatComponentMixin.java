package wily.legacy.mixin.base.client.chat;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.GuiMessage;
import net.minecraft.client.GuiMessageTag;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.util.ARGB;
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

import java.util.Objects;

@Mixin(ChatComponent.class)
public abstract class ChatComponentMixin {

    @Shadow
    @Final
    private Minecraft minecraft;

    @Inject(method = "getWidth(D)I", at = @At(value = "HEAD"), cancellable = true)
    private static void getWidth(double d, CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(Mth.floor(d * (Minecraft.getInstance().getWindow().getGuiScaledWidth() - (4 + LegacyRenderUtil.getChatSafeZone()) * 2)));
    }

    @Shadow
    public abstract double getScale();

    @Shadow
    protected abstract int getLineHeight();

    @Shadow
    public abstract boolean isChatFocused();

    @Shadow
    protected abstract void drawTagIcon(GuiGraphics guiGraphics, int i, int j, GuiMessageTag.Icon icon);

    @Shadow
    protected abstract int getTagIconLeft(GuiMessage.Line line);

    @ModifyArg(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/ChatComponent;forEachLine(IIZILnet/minecraft/client/gui/components/ChatComponent$LineConsumer;)I", ordinal = 0), index = 4)
    private ChatComponent.LineConsumer changeChatX(ChatComponent.LineConsumer lineConsumer, @Local(argsOnly = true) GuiGraphics guiGraphics, @Local(ordinal = 5) int n, @Local(ordinal = 8) int q, @Local(ordinal = 9) int r, @Local(ordinal = 1) float g, @Local(ordinal = 2) float h) {
        return (lx, mx, nx, line, ox, hx) -> {
            int safeZone = Math.round(LegacyRenderUtil.getChatSafeZone());
            guiGraphics.fill(lx - 4 - safeZone, mx, lx + n + 4 + 4 + safeZone, nx, ARGB.color(hx * h, CommonColor.CHAT_BACKGROUND.get().intValue()));
            GuiMessageTag guiMessageTag = line.tag();
            if (guiMessageTag != null) {
                int p = ARGB.color(hx * g, guiMessageTag.indicatorColor());
                guiGraphics.fill(lx - 4 - safeZone, mx, lx - 2 - safeZone, nx, p);
                if (ox == q && guiMessageTag.icon() != null) {
                    this.drawTagIcon(guiGraphics, this.getTagIconLeft(line), nx + r + 9, guiMessageTag.icon());
                }
            }
        };
    }

    @ModifyExpressionValue(method = "getMessageTagAt", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/ChatComponent;screenToChatX(D)D"))
    private double changeMessageTagXPos(double original) {
        return original + Math.round(LegacyRenderUtil.getChatSafeZone());
    }

    @Inject(method = "render", at = @At(value = "HEAD"), cancellable = true)
    private void stopRender(GuiGraphics guiGraphics, int i, int j, int k, boolean bl, CallbackInfo ci) {
        if (minecraft.screen != null && !isChatFocused()) ci.cancel();
    }

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lorg/joml/Matrix3x2fStack;pushMatrix()Lorg/joml/Matrix3x2fStack;", shift = At.Shift.AFTER, ordinal = 0, remap = false))
    private void changeRenderTranslation(GuiGraphics guiGraphics, int i, int j, int k, boolean bl, CallbackInfo ci) {
        guiGraphics.pose().translate(LegacyRenderUtil.getChatSafeZone(), LegacyRenderUtil.getHUDDistance() - 42);
    }

    @Inject(method = "screenToChatX", at = @At("RETURN"), cancellable = true)
    private void screenToChatX(double d, CallbackInfoReturnable<Double> cir) {
        cir.setReturnValue(cir.getReturnValue() - LegacyRenderUtil.getChatSafeZone());
    }

    @Inject(method = "screenToChatY", at = @At("HEAD"), cancellable = true)
    private void screenToChatY(double d, CallbackInfoReturnable<Double> cir) {
        double e = (double) this.minecraft.getWindow().getGuiScaledHeight() - d - 40 + LegacyRenderUtil.getHUDDistance() - 42;
        cir.setReturnValue(e / (this.getScale() * (double) this.getLineHeight()));
    }

    @Inject(method = "isChatFocused", at = @At(value = "HEAD"), cancellable = true)
    private void isChatFocused(CallbackInfoReturnable<Boolean> cir) {
        if (minecraft.screen instanceof OverlayPanelScreen s && s.parent instanceof ChatScreen)
            cir.setReturnValue(true);
    }
}
