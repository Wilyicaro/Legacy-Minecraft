package wily.legacy.mixin;

import net.minecraft.server.Main;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.LegacyMinecraft;
import wily.legacy.init.LegacyServerProperties;

import java.nio.file.Paths;

@Mixin(Main.class)
public class ServerMainMixin {

    @Inject(method = "main", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/Bootstrap;bootStrap()V"))
    private static void mainMixin(String[] strings, CallbackInfo ci) {
        LegacyMinecraft.serverProperties = LegacyServerProperties.fromFile(Paths.get("server.properties"));
    }
}
