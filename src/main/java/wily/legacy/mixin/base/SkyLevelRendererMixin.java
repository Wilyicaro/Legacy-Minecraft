package wily.legacy.mixin.base;

//? if >=1.21.2 {
/*import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
//? if >=1.21.5 {
/^import com.mojang.blaze3d.pipeline.RenderPipeline;
import wily.legacy.client.LegacyRenderPipelines;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.systems.RenderPass;
^///?}
import net.minecraft.client.renderer.SkyRenderer;
*///?} else {
import net.minecraft.client.renderer.LevelRenderer;
//?}
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.legacy.Legacy4JClient;
import wily.legacy.client.LegacyOptions;

@Mixin(/*? if <1.21.2 {*/LevelRenderer/*?} else {*//*SkyRenderer*//*?}*/.class)
public class SkyLevelRendererMixin {
    @ModifyVariable(method = /*? if <1.21.4 {*/"drawStars"/*?} else {*//*"buildStars"*//*?}*/, at = @At(value = "STORE"), ordinal = 4)
    private /*? if <1.20.5 {*//*double*//*?} else {*/float/*?}*/ drawStars(/*? if <1.20.5 {*//*double*//*?} else {*/float/*?}*/ original) {
        return original - 0.05f;
    }
    //? if >=1.21.4 {
    /*@Unique
    private boolean legacySkyShape = LegacyOptions.legacySkyShape.get();
    //? if <1.21.5 {
    @ModifyArg(method = "<init>", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/VertexBuffer;uploadStatic(Lcom/mojang/blaze3d/vertex/VertexFormat$Mode;Lcom/mojang/blaze3d/vertex/VertexFormat;Ljava/util/function/Consumer;)Lcom/mojang/blaze3d/vertex/VertexBuffer;", ordinal = 1))
    private VertexFormat.Mode changeLightSkyMode(VertexFormat.Mode par1){
        return LegacyOptions.legacySkyShape.get() ? VertexFormat.Mode.QUADS : par1;
    }
    @ModifyArg(method = "<init>", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/VertexBuffer;uploadStatic(Lcom/mojang/blaze3d/vertex/VertexFormat$Mode;Lcom/mojang/blaze3d/vertex/VertexFormat;Ljava/util/function/Consumer;)Lcom/mojang/blaze3d/vertex/VertexBuffer;", ordinal = 2))
    private VertexFormat.Mode changeDarkSkyMode(VertexFormat.Mode par1){
        return LegacyOptions.legacySkyShape.get() ? VertexFormat.Mode.QUADS : par1;
    }
    //?} else {
    /^@ModifyArg(method = "<init>", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/ByteBufferBuilder;<init>(I)V"))
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
            instance.drawIndexed(0, 864);
        }else original.call(instance, i, size);
    }

    @ModifyArg(method = {"renderDarkDisc", "renderSkyDisc"}, at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderPass;setPipeline(Lcom/mojang/blaze3d/pipeline/RenderPipeline;)V", remap = false))
    private RenderPipeline changeSkyRenderPipeline(RenderPipeline renderPipeline){
        return legacySkyShape ? LegacyRenderPipelines.LEGACY_SKY : renderPipeline;
    }
    ^///?}

    @Inject(method = "buildSkyDisc", at = @At("HEAD"), cancellable = true)
    private void buildSkyDisc(VertexConsumer vertexConsumer, float f, CallbackInfo ci) {
        if (legacySkyShape) {
            Legacy4JClient.buildLegacySkyDisc(vertexConsumer, f);
            ci.cancel();
        }
    }
    *///?} else if >=1.21.2 {
    /*@Inject(method = "buildSkyDisc", at = @At("HEAD"), cancellable = true)
    private static void buildSkyDisc(Tesselator tesselator, float f, CallbackInfoReturnable<MeshData> cir) {
        if (LegacyOptions.legacySkyShape.get()) {
            BufferBuilder bufferBuilder = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
            Legacy4JClient.buildLegacySkyDisc(bufferBuilder, f);
            cir.setReturnValue(bufferBuilder.buildOrThrow());
        }
    }
    *///?}

}
