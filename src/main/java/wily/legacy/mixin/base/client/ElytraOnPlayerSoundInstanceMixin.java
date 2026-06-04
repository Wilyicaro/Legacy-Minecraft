package wily.legacy.mixin.base.client;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.ElytraOnPlayerSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.factoryapi.base.config.FactoryConfig;
import wily.legacy.config.LegacyCommonOptions;

@Mixin(ElytraOnPlayerSoundInstance.class)
public abstract class ElytraOnPlayerSoundInstanceMixin extends AbstractTickableSoundInstance {
    @Unique
    private static final int LEGACY_FADE_TICKS = 180;

    @Shadow
    @Final
    private LocalPlayer player;

    @Unique
    private boolean legacy$fading;

    @Unique
    private int legacy$fadeTicks;

    @Unique
    private float legacy$fadeVolume;

    @Unique
    private double legacy$fadeX;

    @Unique
    private double legacy$fadeY;

    @Unique
    private double legacy$fadeZ;

    protected ElytraOnPlayerSoundInstanceMixin(SoundEvent soundEvent, SoundSource soundSource, RandomSource randomSource) {
        super(soundEvent, soundSource, randomSource);
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void resetFade(CallbackInfo ci) {
        if (player.isFallFlying()) {
            legacy$fading = false;
            legacy$fadeTicks = 0;
            legacy$fadeVolume = 0.0f;
        }
    }

    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/resources/sounds/ElytraOnPlayerSoundInstance;stop()V"))
    private void fadeOut(ElytraOnPlayerSoundInstance instance) {
        if (player.isRemoved() || !FactoryConfig.hasCommonConfigEnabled(LegacyCommonOptions.legacyAudio)) {
            stop();
            return;
        }

        if (!legacy$fading) {
            legacy$fading = true;
            legacy$fadeTicks = LEGACY_FADE_TICKS;
            legacy$fadeVolume = volume;
            legacy$fadeX = player.getX();
            legacy$fadeY = player.getY();
            legacy$fadeZ = player.getZ();
        }

        if (legacy$fadeTicks <= 0 || legacy$fadeVolume <= 0.001f) {
            stop();
            return;
        }

        legacy$useLandingPosition();
        volume = legacy$fadeVolume * legacy$fadeTicks / (float) LEGACY_FADE_TICKS;
        legacy$fadeTicks--;
    }

    @Unique
    private void legacy$useLandingPosition() {
        x = legacy$fadeX;
        y = legacy$fadeY;
        z = legacy$fadeZ;
    }
}
