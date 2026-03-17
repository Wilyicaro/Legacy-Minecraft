package wily.legacy.mixin.base.client;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EnderDragonRenderer;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(EnderDragonRenderer.class)
public class EndCrystalRendererMixin {
    @Redirect(method = "<clinit>", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/RenderType;entitySmoothCutout(Lnet/minecraft/resources/Identifier;)Lnet/minecraft/client/renderer/RenderType;"))
    private static RenderType init(Identifier resourceLocation) {
        return RenderType.entityTranslucentEmissive(resourceLocation);
    }
}
