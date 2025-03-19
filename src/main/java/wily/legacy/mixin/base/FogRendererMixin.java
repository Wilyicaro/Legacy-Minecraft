package wily.legacy.mixin.base;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
//? if >=1.21.2 {
/*import net.minecraft.client.renderer.FogParameters;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
*///?} else {
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//?}
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.core.BlockPos;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import wily.legacy.client.LegacyBiomeOverride;
import wily.legacy.client.LegacyOptions;

@Mixin(FogRenderer.class)
public abstract class FogRendererMixin {

    @Inject(method = "setupFog",at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/FogRenderer$FogData;start:F", opcode = Opcodes.PUTFIELD, ordinal = 8, shift = At.Shift.AFTER))
    private static void setupFogStart(/*? if <1.21.2 {*/CallbackInfo ci/*?} else {*//*CallbackInfoReturnable<FogParameters> cir*//*?}*/, @Local FogRenderer.FogData fogData) {
        if (LegacyOptions.overrideTerrainFogStart.get()) fogData.start = LegacyOptions.getTerrainFogStart() * 16;
    }

    @Inject(method = "setupFog",at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/FogRenderer$FogData;end:F", opcode = Opcodes.PUTFIELD, ordinal = 11, shift = At.Shift.AFTER))
    private static void setupFogEnd(/*? if <1.21.2 {*/CallbackInfo ci/*?} else {*//*CallbackInfoReturnable<FogParameters> cir*//*?}*/, @Local FogRenderer.FogData fogData) {
        if (LegacyOptions.overrideTerrainFogStart.get()) fogData.end = LegacyOptions.terrainFogEnd.get().floatValue() * 16;
    }

    @Inject(method = "setupFog",at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/FogRenderer$FogData;start:F", opcode = Opcodes.PUTFIELD, ordinal = 7, shift = At.Shift.AFTER))
    private static void setupSkyFogStart(/*? if <1.21.2 {*/CallbackInfo ci/*?} else {*//*CallbackInfoReturnable<FogParameters> cir*//*?}*/, @Local FogRenderer.FogData fogData) {
        if (LegacyOptions.overrideTerrainFogStart.get()) fogData.start = LegacyOptions.getTerrainFogStart() * 16;
    }
    @Inject(method = "setupFog",at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/FogRenderer$FogData;end:F", opcode = Opcodes.PUTFIELD, ordinal = 10, shift = At.Shift.AFTER))
    private static void setupSkyFogEnd(/*? if <1.21.2 {*/CallbackInfo ci/*?} else {*//*CallbackInfoReturnable<FogParameters> cir*//*?}*/, @Local FogRenderer.FogData fogData) {
        if (LegacyOptions.overrideTerrainFogStart.get()) fogData.end = LegacyOptions.terrainFogEnd.get().floatValue() * 16;
    }

    @Inject(method = "setupFog",at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/FogRenderer$FogData;end:F", opcode = Opcodes.PUTFIELD, ordinal = 5, shift = At.Shift.AFTER))
    private static void setupWaterFogEnd(/*? if <1.21.2 {*/CallbackInfo ci/*?} else {*//*CallbackInfoReturnable<FogParameters> cir*//*?}*/, @Local(argsOnly = true) Camera camera, @Local FogRenderer.FogData fogData) {
        if (camera.getEntity() instanceof LocalPlayer localPlayer){
            LegacyBiomeOverride o = LegacyBiomeOverride.getOrDefault(localPlayer.clientLevel.getBiome(BlockPos.containing(camera.getPosition())).unwrapKey());
            if (o.waterFogDistance() != null) fogData.end = o.waterFogDistance();
        }
    }
}
