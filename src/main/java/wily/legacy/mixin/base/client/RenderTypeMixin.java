package wily.legacy.mixin.base.client;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RenderType.class)
public class RenderTypeMixin {

    private static boolean legacy$skinpackCape(ResourceLocation texture) {
        if (texture == null) return false;
        String p = texture.getPath();
        return p != null && p.contains("skinpacks/") && p.contains("/capes/");
    }

    @Inject(method = "entityTranslucent(Lnet/minecraft/resources/ResourceLocation;)Lnet/minecraft/client/renderer/RenderType;", at = @At("HEAD"), cancellable = true, require = 0)
    private static void legacy$entityTranslucent_1(ResourceLocation texture, CallbackInfoReturnable<RenderType> cir) {
        if (legacy$skinpackCape(texture)) cir.setReturnValue(RenderType.entityCutoutNoCull(texture));
    }

    @Inject(method = "entityTranslucent(Lnet/minecraft/resources/ResourceLocation;Z)Lnet/minecraft/client/renderer/RenderType;", at = @At("HEAD"), cancellable = true, require = 0)
    private static void legacy$entityTranslucent_2(ResourceLocation texture, boolean outline, CallbackInfoReturnable<RenderType> cir) {
        if (legacy$skinpackCape(texture)) cir.setReturnValue(RenderType.entityCutoutNoCull(texture));
    }

    @Inject(method = "entitySolid(Lnet/minecraft/resources/ResourceLocation;)Lnet/minecraft/client/renderer/RenderType;", at = @At("HEAD"), cancellable = true, require = 0)
    private static void legacy$entitySolid(ResourceLocation texture, CallbackInfoReturnable<RenderType> cir) {
        if (legacy$skinpackCape(texture)) cir.setReturnValue(RenderType.entityCutoutNoCull(texture));
    }

    @Inject(method = "entityNoOutline(Lnet/minecraft/resources/ResourceLocation;)Lnet/minecraft/client/renderer/RenderType;", at = @At("HEAD"), cancellable = true, require = 0)
    private static void legacy$entityNoOutline(ResourceLocation texture, CallbackInfoReturnable<RenderType> cir) {
        if (legacy$skinpackCape(texture)) cir.setReturnValue(RenderType.entityCutoutNoCull(texture));
    }
}