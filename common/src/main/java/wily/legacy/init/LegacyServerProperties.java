package wily.legacy.init;

import net.minecraft.core.RegistryAccess;
import net.minecraft.server.dedicated.Settings;

import java.nio.file.Path;
import java.util.Properties;

public class LegacyServerProperties extends Settings<LegacyServerProperties> {
    public final boolean legacyRegistries = this.get("legacy-registries", true);

    public LegacyServerProperties(Properties properties) {
        super(properties);
    }
    public static LegacyServerProperties fromFile(Path path) {
        return new LegacyServerProperties(LegacyServerProperties.loadFromFile(path));
    }
    @Override
    protected LegacyServerProperties reload(RegistryAccess registryAccess, Properties properties) {
        return new LegacyServerProperties(properties);
    }
}
