package wily.legacy.mixin.base.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
//? if >1.21.4 {
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;
import com.mojang.blaze3d.systems.RenderPass;
//?} else if >=1.21.2 {
/*import net.minecraft.client.renderer.CompiledShaderProgram;
*///?}
import net.minecraft.client.renderer.LightTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import wily.factoryapi.util.ColorUtil;
import wily.legacy.client.CommonColor;

@Mixin(LightTexture.class)
public class LightTextureMixin {

    @ModifyArg(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/MappableRingBuffer;<init>(Ljava/util/function/Supplier;II)V"), index = 2)
    public int changeBufferSize(int i) {
        return (new Std140SizeCalculator()).putFloat().putFloat().putFloat().putInt().putFloat().putFloat().putFloat().putFloat().putVec3().putVec3().get();
    }

    @ModifyExpressionValue(method = "updateLightTexture", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/buffers/Std140Builder;putVec3(Lorg/joml/Vector3fc;)Lcom/mojang/blaze3d/buffers/Std140Builder;", remap = false))
    public Std140Builder updateLightTexture(Std140Builder original) {
        return original.putVec3(ColorUtil.getRed(CommonColor.BLOCK_LIGHT.get()), ColorUtil.getGreen(CommonColor.BLOCK_LIGHT.get()), ColorUtil.getBlue(CommonColor.BLOCK_LIGHT.get()));
    }
}
