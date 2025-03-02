package wily.legacy.mixin.base;

import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;
import dev.isxander.sdl3java.api.gamepad.SdlGamepad;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = SdlGamepad.class, remap = false)
public class SdlGamepadMixin {
    @Redirect(method = "SDL_GetGamepads", at = @At(value = "INVOKE", target = "Lcom/sun/jna/Pointer;getNativeLong(J)Lcom/sun/jna/NativeLong;"))
    private static NativeLong SDL_GetGamepads(Pointer instance, long offset) {
        return new NativeLong(instance.getInt(offset));
    }
}
