package wily.legacy.Skins.util;

import net.minecraft.client.Minecraft;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public final class LegacySkinsPaths {
    private LegacySkinsPaths() {
    }

    public static Path resolve(String fileName, String... legacyRelativePaths) {
        Path primary = configRoot().resolve("legacy").resolve("skins").resolve(fileName);
        if (Files.exists(primary) || legacyRelativePaths.length == 0) return primary;
        for (String legacyRelativePath : legacyRelativePaths) {
            Path legacy = configRoot().resolve(legacyRelativePath);
            if (!Files.exists(legacy)) continue;
            try {
                Files.createDirectories(primary.getParent());
                Files.copy(legacy, primary, StandardCopyOption.REPLACE_EXISTING);
                return primary;
            } catch (Throwable ignored) {
                return legacy;
            }
        }
        return primary;
    }

    private static Path configRoot() {
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc != null && mc.gameDirectory != null) return mc.gameDirectory.toPath().resolve("config");
        } catch (Throwable ignored) {
        }
        return Path.of(System.getProperty("user.dir")).resolve("config");
    }
}
