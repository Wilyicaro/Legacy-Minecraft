package wily.legacy.mixin.base;

import net.minecraft.util.RandomSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = {"net.minecraft.world.level.levelgen.structure.structures.WoodlandMansionPieces$SecondFloorRoomCollection"})
public class WoodlandMansionPiecesSecondFloor {
    @Inject(method = "get1x1",at = @At("HEAD"), cancellable = true)
    public void get1x1(RandomSource arg, CallbackInfoReturnable<String> cir) {
        cir.setReturnValue("1x1_b" + (arg.nextInt(5) + 1));
    }
}
