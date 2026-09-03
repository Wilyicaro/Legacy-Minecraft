package wily.legacy.mixin.base.client.chat;

import net.minecraft.ChatFormatting;
//? if >=26.2 {
import net.minecraft.network.chat.TextColor;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Shadow;
//?}
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.legacy.client.CommonColor;

// 26.2 gutted ChatFormatting to a bare enum: it no longer carries a colour, and getColor() is gone.
// The 16 legacy colours now live as TextColor constants built from int literals in TextColor.<clinit>,
// so the palette override moves to TextColor#getValue() — the single choke point every render path
// goes through. Note this deliberately does NOT touch the backing `value` field, so equals/hashCode
// and serialization keep reporting vanilla RGB.
//? if >=26.2 {
@Mixin(TextColor.class)
//?} else {
/*@Mixin(ChatFormatting.class)
 *///?}
public class ChatFormattingMixin {

    //? if >=26.2 {
    @Shadow
    @Final
    private String name;

    @Inject(method = "getValue", at = @At("HEAD"), cancellable = true)
    public void legacy$getValue(CallbackInfoReturnable<Integer> cir) {
        if (name == null) return;
        switch (name) {
            case "black" -> cir.setReturnValue(CommonColor.BLACK.get());
            case "dark_blue" -> cir.setReturnValue(CommonColor.DARK_BLUE.get());
            case "dark_green" -> cir.setReturnValue(CommonColor.DARK_GREEN.get());
            case "dark_aqua" -> cir.setReturnValue(CommonColor.DARK_AQUA.get());
            case "dark_red" -> cir.setReturnValue(CommonColor.DARK_RED.get());
            case "dark_purple" -> cir.setReturnValue(CommonColor.DARK_PURPLE.get());
            case "gold" -> cir.setReturnValue(CommonColor.GOLD.get());
            case "gray" -> cir.setReturnValue(CommonColor.GRAY.get());
            case "dark_gray" -> cir.setReturnValue(CommonColor.DARK_GRAY.get());
            case "blue" -> cir.setReturnValue(CommonColor.BLUE.get());
            case "green" -> cir.setReturnValue(CommonColor.GREEN.get());
            case "aqua" -> cir.setReturnValue(CommonColor.AQUA.get());
            case "red" -> cir.setReturnValue(CommonColor.RED.get());
            case "light_purple" -> cir.setReturnValue(CommonColor.LIGHT_PURPLE.get());
            case "yellow" -> cir.setReturnValue(CommonColor.YELLOW.get());
            case "white" -> cir.setReturnValue(CommonColor.WHITE.get());
            default -> {
            }
        }
    }
    //?} else {
    /*@Inject(method = "getColor", at = @At("HEAD"), cancellable = true)
    public void getColor(CallbackInfoReturnable<Integer> cir) {
        ChatFormatting legacy$format = (ChatFormatting) (Object) this;
        switch (legacy$format) {
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
    *///?}
}
