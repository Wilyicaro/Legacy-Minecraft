package wily.legacy.mixin.base.compat.legacyskins.cpm;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Redirect;
import wily.legacy.Skins.client.compat.legacyskins.LegacySkinsCompat;

import java.util.function.Function;

@Pseudo
@Mixin(targets = "com.tom.cpm.client.SelfRenderer$RenderCollector", remap = false)
public abstract class LegacySkinsCpmRenderCollectorMixin {
    @Redirect(
            method = "submitVanilla",
            at = @At(
                    value = "FIELD",
                    target = "Lcom/tom/cpm/client/PlayerRenderManager;entity:Ljava/util/function/Function;",
                    opcode = Opcodes.GETSTATIC,
                    remap = false
            ),
            require = 0,
            remap = false
    )
    private Function<ResourceLocation, RenderType> legacy$forceOpaqueCarouselVanillaParts() {
        if (LegacySkinsCompat.isRenderingEmbeddedCpmPreview()) {
            return LegacySkinsCompat::opaqueEmbeddedCpmRenderType;
        }
        return RenderType::entityTranslucent;
    }

    @WrapOperation(
            method = "lambda$recordBuffer$0",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/tom/cpl/render/VBuffers$NativeRenderType;getNativeType()Ljava/lang/Object;",
                    remap = false
            ),
            require = 0,
            remap = false
    )
    private Object legacy$forceOpaqueCarouselCustomParts(@Coerce Object nativeRenderType,
                                                         Operation<Object> original) {
        if (LegacySkinsCompat.isRenderingEmbeddedCpmPreview()) {
            return null;
        }
        return original.call(nativeRenderType);
    }
}
