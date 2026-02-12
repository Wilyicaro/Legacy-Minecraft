package wily.legacy.mixin.base.client;

//? <1.21.11 {
/*
import net.minecraft.client.renderer.RenderType;
*///?} else {
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
//?}
import net.minecraft.client.renderer.entity.EnderDragonRenderer;
import net.minecraft.resources.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(EnderDragonRenderer.class)
public class EndCrystalRendererMixin {
    //? <1.21.11 {
    /*
    @Redirect(method = "<clinit>", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/RenderType;entitySmoothCutout(Lnet/minecraft/resources/ResourceLocation;)Lnet/minecraft/client/renderer/RenderType;"))
    private static RenderType init(ResourceLocation resourceLocation) {
        return RenderType.entityTranslucentEmissive(resourceLocation);
    *///?} else {
    @Redirect(method = "<clinit>", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/rendertype/RenderTypes;entitySmoothCutout(Lnet/minecraft/resources/Identifier;)Lnet/minecraft/client/renderer/rendertype/RenderType;"))
    private static RenderType init(Identifier resourceLocation) {
        return RenderTypes.entityTranslucentEmissive(resourceLocation);
    //?}
    }
}
