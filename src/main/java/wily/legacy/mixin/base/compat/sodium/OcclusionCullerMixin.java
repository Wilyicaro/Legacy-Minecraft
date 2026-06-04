//? fabric || neoforge {
package wily.legacy.mixin.base.compat.sodium;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.sugar.Local;
import net.caffeinemc.mods.sodium.client.render.chunk.RenderSection;
import net.caffeinemc.mods.sodium.client.render.chunk.lists.RenderSectionVisitor;
import net.caffeinemc.mods.sodium.client.render.chunk.occlusion.OcclusionCuller;
import net.caffeinemc.mods.sodium.client.render.viewport.CameraTransform;
import net.caffeinemc.mods.sodium.client.render.viewport.Viewport;
import net.minecraft.core.SectionPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.legacy.Legacy4J;
import wily.legacy.client.LegacyChunkLoading;
import wily.legacy.config.LegacyCommonOptions;

@Mixin(value = OcclusionCuller.class, remap = false)
public abstract class OcclusionCullerMixin {
    @Shadow
    private static int nearestToZero(int min, int max) {
        return 0;
    }

    @Inject(method = "isWithinRenderDistance", at = @At("HEAD"), cancellable = true)
    private static void isWithinRenderDistance(CameraTransform camera, RenderSection section, float maxDistance, CallbackInfoReturnable<Boolean> cir) {
        if (LegacyCommonOptions.squaredViewDistance.get()) {
            int oy = section.getOriginY() - camera.intY;
            cir.setReturnValue(Legacy4J.isChunkPosVisibleInSquare(camera.intX, camera.intZ, Math.round(maxDistance), section.getOriginX(), section.getOriginZ(), false) && Math.abs(nearestToZero(oy, oy + 16) - camera.fracY) < maxDistance);
        }
    }

    @WrapWithCondition(method = "processQueue", at = @At(value = "INVOKE", target = "Lnet/caffeinemc/mods/sodium/client/render/chunk/lists/RenderSectionVisitor;visit(Lnet/caffeinemc/mods/sodium/client/render/chunk/RenderSection;)V"))
    private static boolean processQueue(RenderSectionVisitor visitor, RenderSection section, @Local(argsOnly = true) Viewport viewport) {
        return isSlowChunkVisible(section, viewport);
    }

    @WrapWithCondition(method = "addNearbySections", at = @At(value = "INVOKE", target = "Lnet/caffeinemc/mods/sodium/client/render/chunk/lists/RenderSectionVisitor;visit(Lnet/caffeinemc/mods/sodium/client/render/chunk/RenderSection;)V"))
    private boolean addNearbySections(RenderSectionVisitor visitor, RenderSection section, @Local(argsOnly = true) Viewport viewport) {
        return isSlowChunkVisible(section, viewport);
    }

    @WrapWithCondition(method = "initWithinWorld", at = @At(value = "INVOKE", target = "Lnet/caffeinemc/mods/sodium/client/render/chunk/lists/RenderSectionVisitor;visit(Lnet/caffeinemc/mods/sodium/client/render/chunk/RenderSection;)V"))
    private boolean initWithinWorld(RenderSectionVisitor visitor, RenderSection section, @Local(argsOnly = true) Viewport viewport) {
        return isSlowChunkVisible(section, viewport);
    }

    private static boolean isSlowChunkVisible(RenderSection section, Viewport viewport) {
        CameraTransform camera = viewport.getTransform();
        return LegacyChunkLoading.filterSection(SectionPos.asLong(section.getChunkX(), section.getChunkY(), section.getChunkZ()), section.getOriginX(), section.getOriginY(), section.getOriginZ(), camera.x, camera.y, camera.z);
    }
}
//?}
