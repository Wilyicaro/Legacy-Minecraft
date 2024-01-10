package wily.legacy.init;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import wily.legacy.LegacyMinecraft;

public class LegacySoundEvents {

    private static final DeferredRegister<SoundEvent> SOUND_EVENT_REGISTER = DeferredRegister.create(LegacyMinecraft.MOD_ID, Registries.SOUND_EVENT);

    public static final RegistrySupplier<SoundEvent> FOCUS = SOUND_EVENT_REGISTER.register("random.focus",()->SoundEvent.createVariableRangeEvent(new ResourceLocation(LegacyMinecraft.MOD_ID,"random.focus")));
    public static final RegistrySupplier<SoundEvent> BACK = SOUND_EVENT_REGISTER.register("random.back",()->SoundEvent.createVariableRangeEvent(new ResourceLocation(LegacyMinecraft.MOD_ID,"random.back")));

    public static void register(){
        SOUND_EVENT_REGISTER.register();
    }
}
