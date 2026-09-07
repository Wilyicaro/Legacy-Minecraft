//? fabric || neoforge {
package wily.legacy.mixin.base.compat.sodium;

import net.caffeinemc.mods.sodium.client.model.color.DefaultColorProviders;
import net.caffeinemc.mods.sodium.client.model.quad.blender.BlendedColorProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(value = BlendedColorProvider.class, remap = false)
public class BlendedColorProviderMixin {
    @ModifyVariable(method = "getColors", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private boolean legacy$useBlockBiomeColors(boolean smooth) {
        return smooth && (Object) this != DefaultColorProviders.GrassColorProvider.BLOCKS && (Object) this != DefaultColorProviders.FoliageColorProvider.BLOCKS;
    }
}
//?}
