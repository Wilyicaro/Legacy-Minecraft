package wily.legacy.mixin.base.client.chat;

import net.minecraft.ChatFormatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.legacy.client.CommonColor;

@Mixin(ChatFormatting.class)
public class ChatFormattingMixin {

    @Inject(method = "getColor", at = @At("HEAD"), cancellable = true)
    public void getColor(CallbackInfoReturnable<Integer> cir) {
        switch ((ChatFormatting) (Object) this) {
            case BLACK -> cir.setReturnValue(CommonColor.BLACK.get());
            case DARK_BLUE -> cir.setReturnValue(CommonColor.DARK_BLUE.get());
            case DARK_GREEN -> cir.setReturnValue(CommonColor.DARK_GREEN.get());
            case DARK_AQUA -> cir.setReturnValue(CommonColor.DARK_AQUA.get());
            case DARK_RED -> cir.setReturnValue(CommonColor.DARK_RED.get());
            case DARK_PURPLE -> cir.setReturnValue(CommonColor.DARK_PURPLE.get());
            case GOLD -> cir.setReturnValue(CommonColor.GOLD.get());
            case GRAY -> cir.setReturnValue(CommonColor.GRAY.get());
            case DARK_GRAY -> cir.setReturnValue(CommonColor.DARK_GRAY.get());
            case BLUE -> cir.setReturnValue(CommonColor.BLUE.get());
            case GREEN -> cir.setReturnValue(CommonColor.GREEN.get());
            case AQUA -> cir.setReturnValue(CommonColor.AQUA.get());
            case RED -> cir.setReturnValue(CommonColor.RED.get());
            case LIGHT_PURPLE -> cir.setReturnValue(CommonColor.LIGHT_PURPLE.get());
            case YELLOW -> cir.setReturnValue(CommonColor.YELLOW.get());
            case WHITE -> cir.setReturnValue(CommonColor.WHITE.get());
        }
    }
}
