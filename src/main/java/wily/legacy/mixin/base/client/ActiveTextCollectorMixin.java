//? if >=1.21.11 {
/*package wily.legacy.mixin.base.client;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.gui.ActiveTextCollector;
import net.minecraft.client.gui.TextAlignment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(ActiveTextCollector.class)
public class ActiveTextCollectorMixin {
    @ModifyArgs(method = "defaultScrollingHelper", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/ActiveTextCollector;accept(Lnet/minecraft/client/gui/TextAlignment;IILnet/minecraft/network/chat/Component;)V"))
    private void renderNotCentered(Args args, @Local(ordinal = 5, argsOnly = true) int width) {
        args.set(0, TextAlignment.LEFT);
        args.set(1, (int) args.get(1) - width / 2);
    }
}
*///?}