package wily.legacy.forge.mixin;

import net.minecraft.client.gui.screens.TitleScreen;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.client.screen.MainMenuScreen;

@Mixin(TitleScreen.class)
public class TitleScreenMixin {
    @Shadow @Final private boolean fading;

    @Inject(method = "init", at = @At("HEAD"), cancellable = true)
    private void init(CallbackInfo ci){
        ci.cancel();
        ((TitleScreen)(Object) this).getMinecraft().setScreen(new MainMenuScreen(fading));
    }
}
