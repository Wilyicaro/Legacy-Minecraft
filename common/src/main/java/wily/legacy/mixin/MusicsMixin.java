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
    @ModifyArg(method = "<clinit>", at = @At(value = "INVOKE", target = "Lnet/minecraft/sounds/Music;<init>(Lnet/minecraft/core/Holder;IIZ)V", ordinal = 1), index = 1)
    private static int creativeMin(int i){
        return 400;
    }
    @ModifyArg(method = "<clinit>", at = @At(value = "INVOKE", target = "Lnet/minecraft/sounds/Music;<init>(Lnet/minecraft/core/Holder;IIZ)V", ordinal = 1), index = 2)
    private static int creativeMax(int i){
        return 1200;
    }
    @ModifyArg(method = "<clinit>", at = @At(value = "INVOKE", target = "Lnet/minecraft/sounds/Music;<init>(Lnet/minecraft/core/Holder;IIZ)V", ordinal = 3), index = 1)
    private static int endMin(int i){
        return 400;
    }
    @ModifyArg(method = "<clinit>", at = @At(value = "INVOKE", target = "Lnet/minecraft/sounds/Music;<init>(Lnet/minecraft/core/Holder;IIZ)V", ordinal = 4), index = 2)
    private static int endMax(int i){
        return 1200;
    }
    @ModifyArg(method = "createGameMusic", at = @At(value = "INVOKE", target = "Lnet/minecraft/sounds/Music;<init>(Lnet/minecraft/core/Holder;IIZ)V"), index = 1)
    private static int gameMin(int i){
        return 400;
    }
    @ModifyArg(method = "createGameMusic", at = @At(value = "INVOKE", target = "Lnet/minecraft/sounds/Music;<init>(Lnet/minecraft/core/Holder;IIZ)V"), index = 2)
    private static int gameMax(int i){
        return 1200;
    }
}
