package wily.legacy.mixin.base;

import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.biome.Biome;
import org.joml.Vector4f;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import wily.legacy.client.LegacyBiomeOverride;
import wily.legacy.client.LegacyOption;

@Mixin(FogRenderer.class)
public abstract class FogRendererMixin {

    @Redirect(method = /*? if <1.21.2 {*//*"setupColor"*//*?} else {*/"computeFogColor"/*?}*/,at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/biome/Biome;getWaterFogColor()I"))
    private static int getWaterFogColor(Biome instance, Camera camera, float f, ClientLevel clientLevel) {
        LegacyBiomeOverride o = LegacyBiomeOverride.getOrDefault(clientLevel.getBiome(BlockPos.containing(camera.getPosition())).unwrapKey());
        if (o.waterFogColor() != null || o.waterColor() != null) return o.waterFogColor() == null ? o.waterColor() : o.waterFogColor();
        return instance.getWaterFogColor();
    }
    @Redirect(method = "setupFog",at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/FogRenderer$FogData;start:F", opcode = Opcodes.PUTFIELD, ordinal = 8))
    private static void setupFog(FogRenderer.FogData instance, float value) {
        instance.start = LegacyOption.overrideTerrainFogStart.get() ? LegacyOption.terrainFogStart.get() * 16 : value;
    }
    @Redirect(method = "setupFog",at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/FogRenderer$FogData;end:F", opcode = Opcodes.PUTFIELD, ordinal = 11))
    private static void setupFogEnd(FogRenderer.FogData instance, float value, Camera camera, FogRenderer.FogMode fogMode,/*? if >=1.21.2 {*/ Vector4f vector4f,/*?}*/ float f) {
        instance.end = f * LegacyOption.terrainFogEnd.get().floatValue() * 2;
    }
}
