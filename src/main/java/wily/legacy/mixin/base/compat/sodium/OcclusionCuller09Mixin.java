//? fabric || neoforge {
package wily.legacy.mixin.base.compat.sodium;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.caffeinemc.mods.sodium.client.render.chunk.RenderSection;
import net.caffeinemc.mods.sodium.client.render.chunk.occlusion.OcclusionCuller;
import net.caffeinemc.mods.sodium.client.render.viewport.CameraTransform;
import net.caffeinemc.mods.sodium.client.render.viewport.Viewport;
import net.minecraft.core.SectionPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import wily.legacy.client.LegacyChunkLoading;
import wily.legacy.config.LegacyCommonOptions;

@Mixin(value = OcclusionCuller.class, remap = false)
public abstract class OcclusionCuller09Mixin {
    @Shadow
    private Viewport viewport;

    @WrapOperation(method = "visitNode", at = @At(value = "INVOKE", target = "Lnet/caffeinemc/mods/sodium/client/render/chunk/occlusion/OcclusionCuller;testDistance(FFF)Z"), require = 0)
    private boolean testDistance(float xzThreshold, float yThreshold, float maxDistance, Operation<Boolean> original, @Local(ordinal = 0) float dx, @Local(ordinal = 2) float dz) {
        if (!LegacyCommonOptions.squaredViewDistance.get()) return original.call(xzThreshold, yThreshold, maxDistance);
        return Math.abs(dx) < maxDistance && Math.abs(dz) < maxDistance && yThreshold < maxDistance;
    }

    @WrapWithCondition(method = {"processQueue", "visitAll"}, at = @At(value = "INVOKE", target = "Lnet/caffeinemc/mods/sodium/client/render/chunk/occlusion/OcclusionCuller$GraphOcclusionVisitor;visit(Lnet/caffeinemc/mods/sodium/client/render/chunk/RenderSection;Z)V"), require = 0)
    private boolean visit(@Coerce Object visitor, RenderSection section, boolean localVisible) {
        return isSlowChunkVisible(section, viewport);
    }

    @WrapWithCondition(method = "visitAll", at = @At(value = "INVOKE", target = "Lnet/caffeinemc/mods/sodium/client/render/chunk/occlusion/OcclusionCuller$VisibilityTestingVisitor;visit(Lnet/caffeinemc/mods/sodium/client/render/chunk/RenderSection;Z)V"), require = 0)
    private boolean visitLocal(@Coerce Object visitor, RenderSection section, boolean localVisible) {
        return isSlowChunkVisible(section, viewport);
    }

    @WrapOperation(method = "processQueue", at = @At(value = "INVOKE", target = "Lnet/caffeinemc/mods/sodium/client/render/chunk/occlusion/OcclusionCuller$VisibilityTestingVisitor;visitTestVisible(Lnet/caffeinemc/mods/sodium/client/render/chunk/RenderSection;)Z"), require = 0)
    private boolean visitTestVisible(@Coerce Object visitor, RenderSection section, Operation<Boolean> original) {
        if (!isSlowChunkVisible(section, viewport)) return false;
        return original.call(visitor, section);
    }

    @WrapWithCondition(method = {"addNearbySections", "initWithinWorld"}, at = @At(value = "INVOKE", target = "Lnet/caffeinemc/mods/sodium/client/render/chunk/occlusion/OcclusionCuller;visitAll(Lnet/caffeinemc/mods/sodium/client/render/chunk/RenderSection;)V"), require = 0)
    private boolean visitAll(OcclusionCuller culler, RenderSection section) {
        return isSlowChunkVisible(section, viewport);
    }

    @Unique
    private static boolean isSlowChunkVisible(RenderSection section, Viewport viewport) {
        if (viewport == null) return true;
        CameraTransform camera = viewport.getTransform();
        return LegacyChunkLoading.filterSection(SectionPos.asLong(section.getChunkX(), section.getChunkY(), section.getChunkZ()), section.getOriginX(), section.getOriginY(), section.getOriginZ(), camera.x, camera.y, camera.z);
    }
}
//?}
