package wily.legacy.mixin.base;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.Util;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
//? if >=1.21.2 {
/*import net.minecraft.client.renderer.FogParameters;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
*///?} else {
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//?}
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.material.FogType;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;
import wily.legacy.client.LegacyBiomeOverride;
import wily.legacy.client.LegacyOptions;
import wily.legacy.client.LegacyUnderwaterFog;

@Mixin(FogRenderer.class)
public abstract class FogRendererMixin {
    @Shadow
    private static int targetBiomeFog;

    @Shadow
    private static int previousBiomeFog;

    @Shadow
    private static long biomeChangedTime;

    @Inject(method = /*? if <1.21.2 {*/"setupColor"/*?} else {*//*"computeFogColor"*//*?}*/, at = @At("HEAD"))
    private static void legacy$prepareUnderwaterFogColor(
            Camera camera, float partialTick, ClientLevel level, int renderDistance, float darkenWorldAmount,
            /*? if <1.21.2 {*/CallbackInfo ci/*?} else {*//*CallbackInfoReturnable<Vector4f> cir*//*?}*/) {
        if (LegacyUnderwaterFog.isEnabled() && camera.getFluidInCamera() == FogType.WATER) {
            int color = LegacyUnderwaterFog.getFogColor(level, camera);
            targetBiomeFog = color;
            previousBiomeFog = color;
            biomeChangedTime = Util.getMillis();
        } else {
            LegacyUnderwaterFog.reset();
        }
    }

    @ModifyExpressionValue(method = /*? if <1.21.2 {*/"setupColor"/*?} else {*//*"computeFogColor"*//*?}*/, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getWaterVision()F"))
    private static float legacy$preserveUnderwaterFogColor(float original) {
        return LegacyUnderwaterFog.isEnabled() ? 0.0F : original;
    }

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
            if (LegacyUnderwaterFog.setupFog(fogData, localPlayer, localPlayer.clientLevel)) return;
            LegacyBiomeOverride override = LegacyBiomeOverride.getOrDefault(localPlayer.clientLevel.getBiome(BlockPos.containing(camera.getPosition())).unwrapKey());
            if (override.waterFogDistance() != null) fogData.end = override.waterFogDistance();
        }
    }
}
