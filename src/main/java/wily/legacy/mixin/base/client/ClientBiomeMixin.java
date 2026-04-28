package wily.legacy.mixin.base.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.attribute.EnvironmentAttributeMap;
import net.minecraft.world.level.biome.Biome;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.legacy.client.BiomeHolder;
import wily.legacy.client.LegacyBiomeOverride;

import java.util.Optional;

@Mixin(Biome.class)
public class ClientBiomeMixin {

    @Shadow
    @Final
    private EnvironmentAttributeMap attributes;

    @Unique
    private Biome self() {
        return (Biome) (Object) this;
    }

    @Unique
    private LegacyBiomeOverride biomeOverride() {
        ClientPacketListener l = Minecraft.getInstance().getConnection();
        return LegacyBiomeOverride.getOrDefault(l != null ? l.registryAccess()./*? if <1.21.2 {*//*registryOrThrow*//*?} else {*/lookupOrThrow/*?}*/(Registries.BIOME).getResourceKey(self()) : Optional.empty());
    }

    @Inject(method = "getWaterColor", at = @At("HEAD"), cancellable = true)
    private void getWaterColor(CallbackInfoReturnable<Integer> cir) {
        biomeOverride().waterColor().ifPresent(cir::setReturnValue);
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void init(CallbackInfo ci) {
        if (((Object)attributes) instanceof BiomeHolder holder) holder.l4j$setBiome(self());
    }
}