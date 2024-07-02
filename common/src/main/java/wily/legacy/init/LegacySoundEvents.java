package wily.legacy.init;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import wily.legacy.Legacy4J;

public class LegacySoundEvents {

    private static final DeferredRegister<SoundEvent> SOUND_EVENT_REGISTER = DeferredRegister.create(Legacy4J.MOD_ID, Registries.SOUND_EVENT);

    public static final RegistrySupplier<SoundEvent> FOCUS = SOUND_EVENT_REGISTER.register("random.focus",()->SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Legacy4J.MOD_ID,"random.focus")));
    public static final RegistrySupplier<SoundEvent> BACK = SOUND_EVENT_REGISTER.register("random.back",()->SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Legacy4J.MOD_ID,"random.back")));
    public static final RegistrySupplier<SoundEvent> CRAFT_FAIL = SOUND_EVENT_REGISTER.register("random.craft_fail",()->SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Legacy4J.MOD_ID,"random.craft_fail")));

    public static final RegistrySupplier<SoundEvent> SCROLL = SOUND_EVENT_REGISTER.register("random.scroll",()->SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Legacy4J.MOD_ID,"random.scroll")));
    public static void register(){
        if (Legacy4J.serverProperties != null && !Legacy4J.serverProperties.legacyRegistries) return;
        SOUND_EVENT_REGISTER.register();
    }
}
