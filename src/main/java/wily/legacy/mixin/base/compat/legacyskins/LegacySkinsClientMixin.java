package wily.legacy.mixin.base.compat.legacyskins;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.legacy.Skins.SkinsClientBootstrap;

@Pseudo
@Mixin(targets = "io.github.redrain0o0.legacyskins.client.LegacySkinsClient", remap = false)
public class LegacySkinsClientMixin {
    @Inject(method = "getSkinsScreen", at = @At("HEAD"), cancellable = true, require = 0, remap = false)
    private static void consoleskins$preferIntegratedChangeSkinScreen(Screen previousScreen, CallbackInfoReturnable<Screen> cir) {
        Screen screen = SkinsClientBootstrap.createChangeSkinScreen(previousScreen);
        if (screen != null && screen != previousScreen) cir.setReturnValue(screen);
    }

    @Inject(method = "openScreen", at = @At("HEAD"), cancellable = true, require = 0, remap = false)
    private static void consoleskins$openIntegratedChangeSkinScreen(Screen previousScreen, CallbackInfo ci) {
        Screen screen = SkinsClientBootstrap.createChangeSkinScreen(previousScreen);
        if (screen == null || screen == previousScreen) return;

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null) return;

        minecraft.setScreen(screen);
        ci.cancel();
    }
}
