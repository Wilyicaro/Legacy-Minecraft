package wily.legacy.mixin.base.client;

import net.minecraft.client.resources.sounds.BiomeAmbientSoundsHandler;
import net.minecraft.world.attribute.AmbientMoodSettings;
import net.minecraft.world.attribute.AmbientSounds;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import wily.legacy.client.LegacyOptions;

import java.util.Optional;

@Mixin(BiomeAmbientSoundsHandler.class)
public class BiomeAmbientSoundsHandlerMixin {
    //? if >=1.21.11 {
    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/attribute/AmbientSounds;mood()Ljava/util/Optional;"))
    public Optional<AmbientMoodSettings> tick(AmbientSounds instance) {
        return LegacyOptions.caveSounds.get() ? instance.mood() : Optional.empty();
    }
    //?} else {
    /*@Shadow
    private Optional<AmbientMoodSettings> moodSettings;

    @Redirect(method = "tick", at = @At(value = "FIELD", target = "Lnet/minecraft/client/resources/sounds/BiomeAmbientSoundsHandler;moodSettings:Ljava/util/Optional;", opcode = Opcodes.GETFIELD))
    public Optional<AmbientMoodSettings> tick(BiomeAmbientSoundsHandler instance) {
        return LegacyOptions.caveSounds.get() ? moodSettings : Optional.empty();
    }
    *///?}
}
