package wily.legacy.Skins.skin;

import wily.legacy.Skins.util.SkinPaths;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

public final class ClientSkinPersistence {
    private static final String FILE_NAME = "selected_skin.txt";
    private ClientSkinPersistence() { }
    private static Path filePath() { return SkinPaths.resolve(FILE_NAME); }
    public static void save(UUID userProfileId, String skinId) {
        if (userProfileId == null) return;
        Path p = filePath();
        try {
            Files.createDirectories(p.getParent());
            if (skinId == null || skinId.isBlank()) {
                if (Files.exists(p)) Files.delete(p);
                return;
            }
            String data = userProfileId + "\n" + skinId + "\n";
            Files.writeString(p, data, StandardCharsets.UTF_8);
        } catch (IOException ignored) { }
    }
    public static String load(UUID expectedUserProfileId) {
        if (expectedUserProfileId == null) return null;
        Path p = filePath();
        if (!Files.exists(p)) return null;
        try {
            String data = Files.readString(p, StandardCharsets.UTF_8);
            String[] lines = data.split("\\R", 3);
            if (lines.length < 2) return null;
            UUID fileUser = UUID.fromString(lines[0].trim());
            if (!expectedUserProfileId.equals(fileUser)) return null;
            String skinId = lines[1].trim();
            return skinId.isBlank() ? null : skinId;
        } catch (IOException | IllegalArgumentException ignored) { return null; }
    }
}
