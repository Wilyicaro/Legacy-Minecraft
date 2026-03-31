package wily.legacy.Skins.util;

import net.minecraft.client.Minecraft;

import java.nio.file.Path;

public final class SkinPaths {
    private SkinPaths() {
    }

    public static Path resolve(String fileName) {
        return configRoot().resolve("legacy").resolve("skins").resolve(fileName);
    }

    private static Path configRoot() {
        Minecraft mc = Minecraft.getInstance();
        if (mc != null && mc.gameDirectory != null) return mc.gameDirectory.toPath().resolve("config");
        return Path.of(System.getProperty("user.dir")).resolve("config");
    }
}
