package wily.legacy.Skins.client.util;

import net.minecraft.client.Minecraft;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class SessionSkinStore {
    private static final String FILE_NAME = "consoleskins_session_skin.txt";

    private SessionSkinStore() {
    }

    private static Path filePath() {
        return Minecraft.getInstance().gameDirectory.toPath().resolve("config").resolve(FILE_NAME);
    }

    public static String loadTexturesB64() {
        try {
            Path p = filePath();
            if (!Files.exists(p)) return null;
            String s = Files.readString(p, StandardCharsets.UTF_8);
            s = s == null ? null : s.trim();
            return (s == null || s.isEmpty()) ? null : s;
        } catch (Throwable ignored) {
            return null;
        }
    }

    public static void saveTexturesB64(String b64) {
        if (b64 == null) return;
        b64 = b64.trim();
        if (b64.isEmpty()) return;
        try {
            Path p = filePath();
            Files.createDirectories(p.getParent());
            Files.writeString(p, b64, StandardCharsets.UTF_8);
        } catch (IOException ignored) {
        } catch (Throwable ignored) {
        }
    }
}
