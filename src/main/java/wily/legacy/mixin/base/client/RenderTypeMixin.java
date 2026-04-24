package wily.legacy.mixin.base.client;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.OptionalDouble;

@Mixin(RenderType.class)
public abstract class RenderTypeMixin {
    @Shadow
    @Final
    public static RenderType.CompositeRenderType LINES;

    private static boolean legacy$skinpackCape(ResourceLocation texture) {
        if (texture == null) return false;
        String path = texture.getPath();
        return path != null && path.contains("skinpacks/") && path.contains("/capes/");
    }

    @ModifyArg(method = "<clinit>", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/RenderStateShard$LineStateShard;<init>(Ljava/util/OptionalDouble;)V", ordinal = 0))
    private static OptionalDouble init(OptionalDouble d) {
        return OptionalDouble.of(1.0);
    }

    @Inject(method = "entityTranslucent(Lnet/minecraft/resources/ResourceLocation;)Lnet/minecraft/client/renderer/RenderType;", at = @At("HEAD"), cancellable = true, require = 0)
    private static void legacy$entityTranslucent(ResourceLocation texture, CallbackInfoReturnable<RenderType> cir) {
        if (legacy$skinpackCape(texture)) cir.setReturnValue(RenderType.entityCutoutNoCull(texture));
    }

    @Inject(method = "entityTranslucent(Lnet/minecraft/resources/ResourceLocation;Z)Lnet/minecraft/client/renderer/RenderType;", at = @At("HEAD"), cancellable = true, require = 0)
    private static void legacy$entityTranslucent(ResourceLocation texture, boolean outline, CallbackInfoReturnable<RenderType> cir) {
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
