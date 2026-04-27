package wily.legacy.mixin.base.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.attribute.EnvironmentAttribute;
import net.minecraft.world.attribute.EnvironmentAttributeMap;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.attribute.modifier.AttributeModifier;
import net.minecraft.world.level.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.legacy.client.BiomeHolder;
import wily.legacy.client.LegacyBiomeOverride;

import java.util.Optional;

@Mixin(EnvironmentAttributeMap.class)
public class EnvironmentAttributeMapMixin implements BiomeHolder {
    @Unique
    private Biome biome;

    @Inject(method = "get", at = @At("HEAD"), cancellable = true)
    public <Value> void get(EnvironmentAttribute<Value> environmentAttribute, CallbackInfoReturnable<EnvironmentAttributeMap.Entry<Value, ?>> cir) {
        if (biome == null) return;
        Optional<Integer> override = Optional.empty();
        if (environmentAttribute == EnvironmentAttributes.SKY_COLOR) {
            override = biomeOverride().skyColor();
        } else if (environmentAttribute == EnvironmentAttributes.FOG_COLOR) {
            override = biomeOverride().fogColor();
        } else if (environmentAttribute == EnvironmentAttributes.WATER_FOG_COLOR) {
            LegacyBiomeOverride o = biomeOverride();
            if (o.waterFogColor().isPresent() || o.waterColor().isPresent())
                override = (o.waterFogColor().isEmpty() ? o.waterColor() : o.waterFogColor());
        }
        override.ifPresent(color -> cir.setReturnValue(new EnvironmentAttributeMap.Entry<>((Value) color, AttributeModifier.override())));
    }

    @Override
    public Biome l4j$getBiome() {
        return biome;
    }

    @Override
    public void l4j$setBiome(Biome biome) {
        this.biome = biome;
    }

    @Unique
    private LegacyBiomeOverride biomeOverride() {
        ClientPacketListener l = Minecraft.getInstance().getConnection();
        return LegacyBiomeOverride.getOrDefault(l != null ? l.registryAccess().lookupOrThrow(Registries.BIOME).getResourceKey(l4j$getBiome()) : Optional.empty());
    }
}
