package wily.legacy.mixin.base.client;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.client.renderer.fog.environment.AtmosphericFogEnvironment;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.core.BlockPos;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import wily.legacy.client.LegacyOptions;

@Mixin(AtmosphericFogEnvironment.class)
public abstract class AtmosphericFogEnvironmentMixin {

    @Inject(method = "setupFog", at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/fog/FogData;environmentalStart:F", ordinal = 0, opcode = Opcodes.PUTFIELD, shift = At.Shift.AFTER))
    private void setupFogStart(FogData fogData, Entity entity, BlockPos blockPos, ClientLevel clientLevel, float f, DeltaTracker deltaTracker, CallbackInfo ci) {
        if (LegacyOptions.overrideTerrainFogStart.get())
            fogData.environmentalStart = LegacyOptions.getTerrainFogStart() * 16;
    }

    @Inject(method = "setupFog", at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/fog/FogData;environmentalEnd:F", ordinal = 0, opcode = Opcodes.PUTFIELD, shift = At.Shift.AFTER))
    private void setupFogEnd(FogData fogData, Entity entity, BlockPos blockPos, ClientLevel clientLevel, float f, DeltaTracker deltaTracker, CallbackInfo ci) {
        if (LegacyOptions.overrideTerrainFogStart.get())
            fogData.environmentalEnd = LegacyOptions.terrainFogEnd.get().floatValue() * 16;
    }
}
