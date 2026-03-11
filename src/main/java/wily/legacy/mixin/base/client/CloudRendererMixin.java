package wily.legacy.mixin.base.client;

import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.CloudRenderer;
import net.minecraft.util.ARGB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(CloudRenderer.class)
public class CloudRendererMixin {
    @ModifyArg(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/MappableRingBuffer;<init>(Ljava/util/function/Supplier;II)V"), index = 2)
    private int expandCloudInfoBuffer(int originalSize) {
        return new Std140SizeCalculator().putVec4().putVec3().putVec3().putVec3().get();
    }

    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/buffers/Std140Builder;putVec3(FFF)Lcom/mojang/blaze3d/buffers/Std140Builder;", ordinal = 1, remap = false))
    private Std140Builder appendSkyColor(Std140Builder builder, float x, float y, float z, int cloudColor, CloudStatus cloudStatus, float cloudHeight, Vec3 cameraPos, float partialTick) {
        Std140Builder result = builder.putVec3(x, y, z);
        ClientLevel level = Minecraft.getInstance().level;
        int skyColor = level != null ? level.getSkyColor(cameraPos, partialTick) : cloudColor;
        return result.putVec3(ARGB.redFloat(skyColor), ARGB.greenFloat(skyColor), ARGB.blueFloat(skyColor));
    }
}
