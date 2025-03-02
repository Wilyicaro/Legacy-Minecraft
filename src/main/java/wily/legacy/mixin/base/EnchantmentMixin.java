package wily.legacy.mixin.base;

import net.minecraft.ChatFormatting;
import net.minecraft.world.item.enchantment.Enchantment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(Enchantment.class)
public class EnchantmentMixin {
    //? if >=1.20.5 {
    @ModifyArg(method = "getFullname", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/chat/Style;withColor(Lnet/minecraft/ChatFormatting;)Lnet/minecraft/network/chat/Style;", ordinal = 0))
    private static ChatFormatting getCurseFullname(ChatFormatting chatFormatting) {
        return ChatFormatting.DARK_RED;
    }
    @ModifyArg(method = "getFullname", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/chat/Style;withColor(Lnet/minecraft/ChatFormatting;)Lnet/minecraft/network/chat/Style;", ordinal = 1))
    private static ChatFormatting getFullname(ChatFormatting chatFormatting) {
        return ChatFormatting.WHITE;
    }
    //?} else {
    /*@ModifyArg(method = "getFullname", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/chat/MutableComponent;withStyle(Lnet/minecraft/ChatFormatting;)Lnet/minecraft/network/chat/MutableComponent;", ordinal = 0))
    private ChatFormatting getCurseFullname(ChatFormatting chatFormatting) {
        return ChatFormatting.DARK_RED;
    }
    @ModifyArg(method = "getFullname", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/chat/MutableComponent;withStyle(Lnet/minecraft/ChatFormatting;)Lnet/minecraft/network/chat/MutableComponent;", ordinal = 1))
    private ChatFormatting getFullname(ChatFormatting chatFormatting) {
        return ChatFormatting.WHITE;
    }
    *///?}
}
