package wily.legacy.mixin.base.client;

import net.minecraft.client.resources.sounds.BiomeAmbientSoundsHandler;
//? >=1.21.11 {
import net.minecraft.world.attribute.AmbientSounds;
//?}
import net.minecraft.world./*? if <1.21.11 {*//*level.biome*//*?} else {*/attribute/*?}*/.AmbientMoodSettings;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import wily.legacy.client.LegacyOptions;

import java.util.Optional;

//? <1.21.11 {
/*
@Mixin(BiomeAmbientSoundsHandler.class)
public class BiomeAmbientSoundsHandlerMixin {
    @Shadow
    private Optional<AmbientMoodSettings> moodSettings;

    @Redirect(method = "tick", at = @At(value = "FIELD", target = "Lnet/minecraft/client/resources/sounds/BiomeAmbientSoundsHandler;moodSettings:Ljava/util/Optional;", opcode = Opcodes.GETFIELD))
    public Optional<AmbientMoodSettings> tick(BiomeAmbientSoundsHandler instance) {
        return LegacyOptions.caveSounds.get() ? moodSettings : Optional.empty();
    }
}
*///?} else {
@Mixin(BiomeAmbientSoundsHandler.class)
public class BiomeAmbientSoundsHandlerMixin {
    
    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/attribute/AmbientSounds;mood()Ljava/util/Optional;"))
    public Optional<AmbientMoodSettings> tick(AmbientSounds instance) {
        return LegacyOptions.caveSounds.get() ? instance.mood() : Optional.empty();
    }
}
//?}
