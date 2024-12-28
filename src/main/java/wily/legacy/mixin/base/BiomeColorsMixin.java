package wily.legacy.mixin.base;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.ColorResolver;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.client.LegacyBiomeOverride;

import java.util.Optional;

@Mixin(BiomeColors.class)
public class BiomeColorsMixin {
    @Mutable
    @Shadow @Final public static ColorResolver WATER_COLOR_RESOLVER;

    @Inject(method = "<clinit>",at = @At("RETURN"))
    private static void replaceWaterColorResolver(CallbackInfo ci) {
        WATER_COLOR_RESOLVER = ((biome, d, e) -> {
            ClientPacketListener l = Minecraft.getInstance().getConnection();
            LegacyBiomeOverride o = LegacyBiomeOverride.getOrDefault(l != null ? l.registryAccess().lookupOrThrow(Registries.BIOME).filterElements(b-> b == biome).listElementIds().findFirst() : Optional.empty());
            return o.waterColor() == null  ? biome.getWaterColor() : o.waterColor();
        });
    }
}
