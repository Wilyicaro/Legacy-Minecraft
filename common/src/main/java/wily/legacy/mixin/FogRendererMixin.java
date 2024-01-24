package wily.legacy.mixin;

import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import wily.legacy.client.LegacyBiomeOverride;

@Mixin(FogRenderer.class)
public class FogRendererMixin {
    @Redirect(method = "setupColor",at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/biome/Biome;getWaterFogColor()I"))
    private static int getWaterFogColor(Biome instance, Camera camera, float f, ClientLevel clientLevel) {
        LegacyBiomeOverride o = LegacyBiomeOverride.getOrDefault(clientLevel.getBiome(BlockPos.containing(camera.getPosition())).unwrapKey());
        if (o.waterFogColor() != null || o.waterColor() != null) return o.waterFogColor() == null ? o.waterColor() : o.waterFogColor();
        return instance.getWaterFogColor();
    }
}
