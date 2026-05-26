package wily.legacy.mixin.base.client.chat;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalIntRef;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.client.CommonColor;
import wily.legacy.client.LegacyOptions;
import wily.legacy.util.client.LegacyRenderUtil;

@Mixin(targets = {"net.minecraft.client.gui.components.ChatComponent$DrawingBackgroundGraphicsAccess", "net.minecraft.client.gui.components.ChatComponent$DrawingFocusedGraphicsAccess"} )
public class ChatComponentDrawingMixin {

    @Inject(method = "handleTag", at = @At(value = "HEAD"), cancellable = true)
    private void handleTag(CallbackInfo ci, @Local(argsOnly = true, ordinal = 0) LocalIntRef x0, @Local(argsOnly = true, ordinal = 2) LocalIntRef x1) {
        if (!LegacyOptions.displayChatIndicators.get()) {
            ci.cancel();
            return;
        }

        int safeZone = Math.round(LegacyRenderUtil.getChatSafeZone());
        x0.set(x0.get()-safeZone);
        x1.set(x1.get()-safeZone);
    }

    @Inject(method = "handleTagIcon", at = @At(value = "HEAD"), cancellable = true)
    private void handleTagIcon(CallbackInfo ci) {
        if (!LegacyOptions.displayChatIndicators.get())
            ci.cancel();
    }

    @ModifyArg(method = "handleMessage", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/ActiveTextCollector;accept(Lnet/minecraft/client/gui/TextAlignment;IILnet/minecraft/client/gui/ActiveTextCollector$Parameters;Lnet/minecraft/util/FormattedCharSequence;)V"), index = 4)
    private FormattedCharSequence handleMessageTextColor(FormattedCharSequence text) {
        if (!CommonColor.CHAT_TEXT.isOverridden())
            return text;

        int color = CommonColor.CHAT_TEXT.get() & 0x00FFFFFF;
        return sink -> text.accept((index, style, codePoint) -> sink.accept(index, chatTextStyle(style, color), codePoint));
    }

    private static Style chatTextStyle(Style style, int color) {
        return style.getColor() == null ? style.withColor(color) : style;
    }
}
