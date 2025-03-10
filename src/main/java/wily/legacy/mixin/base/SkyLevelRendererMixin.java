package wily.legacy.mixin.base;

//? if >=1.21.2 {
/*import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.renderer.SkyRenderer;
*///?} else {
import net.minecraft.client.renderer.LevelRenderer;
//?}
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
    /*@ModifyArg(method = "<init>", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/VertexBuffer;uploadStatic(Lcom/mojang/blaze3d/vertex/VertexFormat$Mode;Lcom/mojang/blaze3d/vertex/VertexFormat;Ljava/util/function/Consumer;)Lcom/mojang/blaze3d/vertex/VertexBuffer;", ordinal = 1))
    private VertexFormat.Mode changeLightSkyMode(VertexFormat.Mode par1){
        return LegacyOptions.legacySkyShape.get() ? VertexFormat.Mode.QUADS : par1;
    }

    @ModifyArg(method = "<init>", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/VertexBuffer;uploadStatic(Lcom/mojang/blaze3d/vertex/VertexFormat$Mode;Lcom/mojang/blaze3d/vertex/VertexFormat;Ljava/util/function/Consumer;)Lcom/mojang/blaze3d/vertex/VertexBuffer;", ordinal = 2))
    private VertexFormat.Mode changeDarkSkyMode(VertexFormat.Mode par1){
        return LegacyOptions.legacySkyShape.get() ? VertexFormat.Mode.QUADS : par1;
    }

    @Inject(method = "buildSkyDisc", at = @At("HEAD"), cancellable = true)
    private void buildSkyDisc(VertexConsumer vertexConsumer, float f, CallbackInfo ci) {
        if (LegacyOptions.legacySkyShape.get()) {
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
