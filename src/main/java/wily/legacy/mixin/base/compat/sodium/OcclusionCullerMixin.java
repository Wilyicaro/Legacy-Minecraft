//? if (fabric || >=1.21 && neoforge) {
package wily.legacy.mixin.base.compat.sodium;

//? if >=1.21 {
import net.caffeinemc.mods.sodium.client.render.chunk.RenderSection;
import net.caffeinemc.mods.sodium.client.render.chunk.occlusion.OcclusionCuller;
import net.caffeinemc.mods.sodium.client.render.viewport.CameraTransform;
//?} else {
/*import me.jellysquid.mods.sodium.client.render.chunk.RenderSection;
import me.jellysquid.mods.sodium.client.render.chunk.occlusion.OcclusionCuller;
import me.jellysquid.mods.sodium.client.render.viewport.CameraTransform;
*///?}
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.legacy.Legacy4J;
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
}
//?}