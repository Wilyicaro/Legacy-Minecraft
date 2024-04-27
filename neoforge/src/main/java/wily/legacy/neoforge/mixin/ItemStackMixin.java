package wily.legacy.neoforge.mixin;

import net.minecraft.ChatFormatting;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(ItemStack.class)
public class ItemStackMixin {
    @ModifyArg(method = "getTooltipLines",at = @At(value = "INVOKE", target = "Lnet/minecraft/network/chat/MutableComponent;withStyle(Lnet/minecraft/ChatFormatting;)Lnet/minecraft/network/chat/MutableComponent;", ordinal = 0))
    public ChatFormatting getTooltipLines(ChatFormatting arg) {
        return ChatFormatting.GOLD;
    }
}
