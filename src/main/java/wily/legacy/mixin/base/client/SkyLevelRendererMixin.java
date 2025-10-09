package wily.legacy.mixin.base.client;

//? if >=1.21.2 {
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import wily.legacy.client.LegacyRenderPipelines;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.systems.RenderPass;
import net.minecraft.client.renderer.SkyRenderer;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.Legacy4JClient;
import wily.legacy.client.LegacyOptions;

@Mixin(SkyRenderer.class)
public class SkyLevelRendererMixin {
    @Unique
    private boolean legacySkyShape = LegacyOptions.legacySkyShape.get();

    @ModifyArg(method = "<init>", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/ByteBufferBuilder;exactlySized(I)Lcom/mojang/blaze3d/vertex/ByteBufferBuilder;"))
    private int changeSkyBufferVertexCount(int vertices){
        return legacySkyShape ? 576 * DefaultVertexFormat.POSITION.getVertexSize() : vertices;
    }

    @Inject(method = {"renderDarkDisc", "renderSkyDisc"}, at = @At("HEAD"))
    private void addShareParams(CallbackInfo ci, @Share("autoStorageIndexBuffer") LocalRef<RenderSystem.AutoStorageIndexBuffer> autoStorageIndexBuffer, @Share("gpuBuffer") LocalRef<GpuBuffer> gpuBuffer){
        if (legacySkyShape){
            autoStorageIndexBuffer.set(RenderSystem.getSequentialBuffer(VertexFormat.Mode.QUADS));
            gpuBuffer.set(autoStorageIndexBuffer.get().getBuffer(864));
        }
    }

    @WrapOperation(method = {"renderDarkDisc", "renderSkyDisc"}, at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderPass;draw(II)V", remap = false))
    private void changeSkyRenderVertexCount(RenderPass instance, int i, int size, Operation<Void> original, @Local RenderPass renderPass, @Share("autoStorageIndexBuffer") LocalRef<RenderSystem.AutoStorageIndexBuffer> autoStorageIndexBuffer, @Share("gpuBuffer") LocalRef<GpuBuffer> gpuBuffer){
        if (legacySkyShape){
            instance.setIndexBuffer(gpuBuffer.get(), autoStorageIndexBuffer.get().type());
            instance.drawIndexed(0, 0, 864, 1);
        }else original.call(instance, i, size);
    }

    @ModifyArg(method = {"renderDarkDisc", "renderSkyDisc"}, at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderPass;setPipeline(Lcom/mojang/blaze3d/pipeline/RenderPipeline;)V", remap = false))
    private RenderPipeline changeSkyRenderPipeline(RenderPipeline renderPipeline){
        return legacySkyShape ? LegacyRenderPipelines.LEGACY_SKY : renderPipeline;
    }

    @Inject(method = "buildSkyDisc", at = @At("HEAD"), cancellable = true)
    private void buildSkyDisc(VertexConsumer vertexConsumer, float f, CallbackInfo ci) {
        if (legacySkyShape) {
            Legacy4JClient.buildLegacySkyDisc(vertexConsumer, f);
            ci.cancel();
        }
    }
}
