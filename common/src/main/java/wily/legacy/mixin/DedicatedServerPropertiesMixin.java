package wily.legacy.mixin;

import net.minecraft.server.dedicated.DedicatedServerProperties;
import net.minecraft.server.dedicated.Settings;
import org.spongepowered.asm.mixin.Mixin;
import wily.legacy.init.LegacyServerProperties;

import java.util.Properties;

@Mixin(DedicatedServerProperties.class)
public abstract class DedicatedServerPropertiesMixin extends Settings<DedicatedServerProperties> {
    public boolean legacyRegistries = this.get("legacy-registries", true);

    public DedicatedServerPropertiesMixin(Properties properties) {
        super(properties);
    }

}
