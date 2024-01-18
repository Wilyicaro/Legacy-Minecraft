package wily.legacy.mixin;

import net.minecraft.sounds.Musics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(Musics.class)
public class MusicsMixin {
    @ModifyArg(method = "<clinit>", at = @At(value = "INVOKE", target = "Lnet/minecraft/sounds/Music;<init>(Lnet/minecraft/core/Holder;IIZ)V"), index = 3)
    private static boolean init(boolean bl){
        return false;
    }
}
