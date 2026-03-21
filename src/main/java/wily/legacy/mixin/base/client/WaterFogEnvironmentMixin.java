package wily.legacy.mixin.base.client;

import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.client.renderer.fog.environment.WaterFogEnvironment;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.client.LegacyBiomeOverride;

@Mixin(WaterFogEnvironment.class)
public abstract class WaterFogEnvironmentMixin {
    @Inject(method = "setupFog", at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/fog/FogData;environmentalEnd:F", opcode = Opcodes.PUTFIELD, shift = At.Shift.AFTER))
            //? if >=1.21.11 {
    private void setupWaterFogEnd(FogData fogData, Camera camera, ClientLevel clientLevel, float f, DeltaTracker deltaTracker, CallbackInfo ci) {
        LegacyBiomeOverride o = LegacyBiomeOverride.getOrDefault(clientLevel.getBiome(camera.blockPosition()).unwrapKey());
        //?} else {
    /*private void setupWaterFogEnd(FogData fogData, Entity entity, BlockPos blockPos, ClientLevel clientLevel, float f, DeltaTracker deltaTracker, CallbackInfo ci) {
        if (!(entity instanceof LocalPlayer localPlayer)) return;
            LegacyBiomeOverride o = LegacyBiomeOverride.getOrDefault(localPlayer.level().getBiome(entity.getOnPos()).unwrapKey());
    *///?}
        o.waterFogDistance().ifPresent(fogDistance -> fogData.environmentalEnd = fogDistance);
    }
}
