package wily.legacy.mixin.base;

import net.minecraft.client.renderer.RenderType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import java.util.OptionalDouble;

@Mixin(RenderType.class)
public abstract class RenderTypeMixin {
    @Shadow @Final public static RenderType.CompositeRenderType LINES;

    @ModifyArg(method = "<clinit>", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/RenderStateShard$LineStateShard;<init>(Ljava/util/OptionalDouble;)V", ordinal = 0))
    private static OptionalDouble init(OptionalDouble d){
        return OptionalDouble.of(1.0);
    }
}
