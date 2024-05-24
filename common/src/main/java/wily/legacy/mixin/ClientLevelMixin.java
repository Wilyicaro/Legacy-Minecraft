package wily.legacy.mixin;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import wily.legacy.client.LegacyBiomeOverride;

@Mixin(ClientLevel.class)
public class ClientLevelMixin {
    private ClientLevel self(){
        return (ClientLevel) (Object) this;
    }
    @Redirect(method = "calculateBlockTint",at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/ColorResolver;getColor(Lnet/minecraft/world/level/biome/Biome;DD)I"))
    public int calculateBlockTint(ColorResolver instance, Biome biome, double x, double z, BlockPos pos) {
        BlockPos.MutableBlockPos m = pos.mutable();
        LegacyBiomeOverride o = LegacyBiomeOverride.getOrDefault(self().getBiome(m.setX((int) x).setZ((int) z)).unwrapKey());
        return instance == BiomeColors.WATER_COLOR_RESOLVER && self().getFluidState(m).is(FluidTags.WATER) && o.waterColor() != null ? o.waterColor() : instance.getColor(biome,x,z);
    }
}