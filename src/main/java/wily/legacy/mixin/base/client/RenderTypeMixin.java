package wily.legacy.mixin.base.client;

import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RenderTypes.class)
public abstract class RenderTypeMixin {

    private static boolean legacy$skinpackCape(Identifier texture) {
        if (texture == null) return false;
        String path = texture.getPath();
        return path != null && path.contains("skinpacks/") && path.contains("/capes/");
    }

    @Inject(method = "entityTranslucent(Lnet/minecraft/resources/Identifier;)Lnet/minecraft/client/renderer/rendertype/RenderType;", at = @At("HEAD"), cancellable = true, require = 0)
    private static void legacy$entityTranslucent(Identifier texture, CallbackInfoReturnable<RenderType> cir) {
        if (legacy$skinpackCape(texture)) cir.setReturnValue(RenderTypes.entityCutout(texture));
    }

    @Inject(method = "entityTranslucent(Lnet/minecraft/resources/Identifier;Z)Lnet/minecraft/client/renderer/rendertype/RenderType;", at = @At("HEAD"), cancellable = true, require = 0)
    private static void legacy$entityTranslucent(Identifier texture, boolean outline, CallbackInfoReturnable<RenderType> cir) {
        if (legacy$skinpackCape(texture)) cir.setReturnValue(RenderTypes.entityCutout(texture));
    }

    @Inject(method = "entitySolid(Lnet/minecraft/resources/Identifier;)Lnet/minecraft/client/renderer/rendertype/RenderType;", at = @At("HEAD"), cancellable = true, require = 0)
    private static void legacy$entitySolid(Identifier texture, CallbackInfoReturnable<RenderType> cir) {
        if (legacy$skinpackCape(texture)) cir.setReturnValue(RenderTypes.entityCutout(texture));
    }

    @Inject(method = "entityNoOutline(Lnet/minecraft/resources/Identifier;)Lnet/minecraft/client/renderer/rendertype/RenderType;", at = @At("HEAD"), cancellable = true, require = 0)
    private static void legacy$entityNoOutline(Identifier texture, CallbackInfoReturnable<RenderType> cir) {
        if (legacy$skinpackCape(texture)) cir.setReturnValue(RenderTypes.entityCutout(texture));
    }
}
