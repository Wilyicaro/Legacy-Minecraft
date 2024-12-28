package wily.legacy.mixin.base;

import net.minecraft.client.resources.sounds.BiomeAmbientSoundsHandler;
import net.minecraft.world.level.biome.AmbientMoodSettings;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import wily.legacy.client.LegacyOption;

import java.util.Optional;

@Mixin(BiomeAmbientSoundsHandler.class)
public class BiomeAmbientSoundsHandlerMixin {
    @Shadow private Optional<AmbientMoodSettings> moodSettings;

    @Redirect(method = "tick",at = @At(value = "FIELD", target = "Lnet/minecraft/client/resources/sounds/BiomeAmbientSoundsHandler;moodSettings:Ljava/util/Optional;", opcode = Opcodes.GETFIELD))
    public Optional<AmbientMoodSettings> tick(BiomeAmbientSoundsHandler instance) {
        return LegacyOption.caveSounds.get() ? moodSettings : Optional.empty();
    }
}
