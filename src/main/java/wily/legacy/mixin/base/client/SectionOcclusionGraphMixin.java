package wily.legacy.mixin.base.client;

import net.minecraft.client.renderer.SectionOcclusionGraph;
import net.minecraft.client.renderer.ViewArea;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.SectionPos;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.legacy.Legacy4J;
import wily.legacy.client.LegacyChunkLoading;
import wily.legacy.config.LegacyCommonOptions;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@Mixin(SectionOcclusionGraph.class)
public class SectionOcclusionGraphMixin {
    @Shadow
    private @Nullable ViewArea viewArea;

    @Shadow
    private AtomicBoolean needsFrustumUpdate;

    @Inject(method = "isInViewDistance", at = @At("HEAD"), cancellable = true)
    private void isWithinDistance(long l, long m, CallbackInfoReturnable<Boolean> cir) {
        if (LegacyCommonOptions.squaredViewDistance.get())
            cir.setReturnValue(Legacy4J.isChunkPosVisibleInSquare(SectionPos.x(l), SectionPos.z(l), viewArea.getViewDistance(), SectionPos.x(m), SectionPos.z(m), false));
    }

    @Inject(method = "addSectionsInFrustum", at = @At("TAIL"))
    private void addSectionsInFrustum(Frustum frustum, List<SectionRenderDispatcher.RenderSection> visibleSections, List<SectionRenderDispatcher.RenderSection> nearbyVisibleSections, CallbackInfo ci) {
        if (LegacyChunkLoading.filter(visibleSections, nearbyVisibleSections)) {
            needsFrustumUpdate.set(true);
        }
    }
}
