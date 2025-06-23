package wily.legacy.mixin.base;

import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.ChannelAccess;
import net.minecraft.client.sounds.SoundEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.Map;

@Mixin(SoundEngine.class)
public interface SoundEngineAccessor {
    //? if <1.21.4 {
    @Accessor
    Map<SoundInstance, ChannelAccess.ChannelHandle> getInstanceToChannel();
    @Invoker("calculateVolume")
    float invokeCalculateVolume(SoundInstance soundInstance);
    //? } else {
    /* @Invoker("setVolume")
    void invokeSetVolume(SoundInstance soundInstance, float f);
    *///? }
}
