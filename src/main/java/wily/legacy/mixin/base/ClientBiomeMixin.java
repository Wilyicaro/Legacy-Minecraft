package wily.legacy.mixin.base;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.legacy.client.LegacyBiomeOverride;

import java.util.Optional;

@Mixin(Biome.class)
public class ClientBiomeMixin {

    @Unique
    private Biome self(){
        return (Biome) (Object) this;
    }

    @Unique
    private LegacyBiomeOverride biomeOverride(){
        ClientPacketListener l = Minecraft.getInstance().getConnection();
        return LegacyBiomeOverride.getOrDefault(l != null ? l.registryAccess()./*? if <1.21.2 {*/registryOrThrow/*?} else {*//*lookupOrThrow*//*?}*/(Registries.BIOME).getResourceKey(self()) : Optional.empty());
    }

    @Inject(method = "getWaterColor", at = @At("HEAD"), cancellable = true)
    private void getWaterColor(CallbackInfoReturnable<Integer> cir){
        LegacyBiomeOverride o = biomeOverride();
        if (o.waterColor() != null) cir.setReturnValue(o.waterColor());
    }

    @Inject(method = "getWaterFogColor", at = @At("HEAD"), cancellable = true)
    private void getWaterFogColor(CallbackInfoReturnable<Integer> cir){
        LegacyBiomeOverride o = biomeOverride();
        if (o.waterFogColor() != null || o.waterColor() != null) cir.setReturnValue(o.waterFogColor() == null ? o.waterColor() : o.waterFogColor());
    }

    @Inject(method = "getFogColor", at = @At("HEAD"), cancellable = true)
    private void getFogColor(CallbackInfoReturnable<Integer> cir){
        LegacyBiomeOverride o = biomeOverride();
        if (o.fogColor() != null) cir.setReturnValue(o.fogColor());
    }

    @Inject(method = "getSkyColor", at = @At("HEAD"), cancellable = true)
    private void getSkyColor(CallbackInfoReturnable<Integer> cir){
        LegacyBiomeOverride o = biomeOverride();
        if (o.skyColor() != null) cir.setReturnValue(o.skyColor());
    }
}
