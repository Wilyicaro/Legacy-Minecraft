//? fabric || neoforge {
package wily.legacy.mixin.base.compat.sodium;

import net.caffeinemc.mods.sodium.client.gl.shader.ShaderLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import org.apache.commons.io.IOUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.factoryapi.FactoryAPI;
import wily.legacy.Legacy4J;
import wily.legacy.client.screen.compat.IrisCompat;

import java.io.IOException;
import java.io.Reader;
import java.util.Optional;

@Mixin(value = ShaderLoader.class, remap = false)
public class ShaderLoaderMixin {
    @Inject(method = "getShaderSource", at = @At("HEAD"), cancellable = true)
    private static void legacy$loadWaterShader(Identifier name, CallbackInfoReturnable<String> cir) {
        if (!name.getNamespace().equals("sodium") || !name.getPath().equals("blocks/block_layer_opaque.vsh")) return;
        if (FactoryAPI.isModLoaded("iris") && IrisCompat.isShaderPackInUse()) return;
        Optional<Resource> resource = Minecraft.getInstance().getResourceManager().getResource(Legacy4J.createModLocation("shaders/core/sodium_terrain.vsh"));
        if (resource.isEmpty()) return;
        try (Reader reader = resource.get().openAsReader()) {
            cir.setReturnValue(IOUtils.toString(reader));
        } catch (IOException exception) {
            Legacy4J.LOGGER.warn("Unable to load Legacy water shader", exception);
        }
    }
}
//?}
