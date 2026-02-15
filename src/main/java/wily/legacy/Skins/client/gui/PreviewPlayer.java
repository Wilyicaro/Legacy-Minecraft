package wily.legacy.Skins.client.gui;

import com.mojang.authlib.GameProfile;
import wily.legacy.CustomModelSkins.cpm.shared.config.Player;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.RemotePlayer;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PreviewPlayer extends RemotePlayer {
    private static final Map<UUID, PreviewPlayer> POOL = new ConcurrentHashMap<>();

    public PreviewPlayer(ClientLevel level, GameProfile profile) {
        super(level, profile);
    }

    @Override
    public boolean shouldShowName() {
        return false;
    }

    public static void remove(UUID id) {
        Player.removeClientPlayerOverride(id);
        POOL.remove(id);
    }

    public static void clear() {
        POOL.keySet().forEach(Player::removeClientPlayerOverride);
        POOL.clear();
    }
}
