package wily.legacy.mixin.base.client;

//? if >=1.21.11 {

/*import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
*///?} else {
import net.minecraft.client.renderer.RenderType;
 //?}
import net.minecraft.client.renderer.entity.EnderDragonRenderer;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(EnderDragonRenderer.class)
public class EndCrystalRendererMixin {
    //? if >=1.21.11 {
    /*@Redirect(method = "<clinit>", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/rendertype/RenderTypes;entitySmoothCutout(Lnet/minecraft/resources/ResourceLocation;)Lnet/minecraft/client/renderer/rendertype/RenderType;"))
    private static RenderType init(ResourceLocation resourceLocation) {
        return RenderTypes.entityTranslucentEmissive(resourceLocation);
    }
    *///?} else {
    @Redirect(method = "<clinit>", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/RenderType;entitySmoothCutout(Lnet/minecraft/resources/ResourceLocation;)Lnet/minecraft/client/renderer/RenderType;"))
    private static RenderType init(ResourceLocation resourceLocation) {
        return RenderType.entityTranslucentEmissive(resourceLocation);
    }
     //?}
}
