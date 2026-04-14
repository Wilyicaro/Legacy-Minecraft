package wily.legacy.Skins.skin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import wily.factoryapi.FactoryAPIClient;

public final class SkinFairness {
    private static final String MINIMEGA_MOD_ID = "minimega";

    private SkinFairness() {
    }

    public static String effectiveSkinId(Minecraft client, String skinId) {
        String id = SkinIdUtil.normalize(skinId);
        if (SkinIdUtil.isBlankOrAutoSelect(id)) return id;
        return shouldUseSafeSkin(client, id) ? SkinIdUtil.AUTO_SELECT : id;
    }

    public static boolean isFair(String skinId) {
        if (SkinIdUtil.isBlankOrAutoSelect(skinId)) return true;
        SkinEntry entry = SkinPackLoader.getSkin(skinId);
        return entry == null || entry.fair();
    }

    public static boolean shouldUseSafeSkin(Minecraft client, String skinId) {
        return isRestrictedServer(client) && !isFair(skinId);
    }

    public static boolean isRestrictedServer(Minecraft client) {
        return client != null
                && client.player != null
                && client.getConnection() != null
                && !client.hasSingleplayerServer()
                && FactoryAPIClient.hasModOnServer(MINIMEGA_MOD_ID);
    }

    public static String resolveServerKey(Minecraft client) {
        if (client == null || client.hasSingleplayerServer()) return null;
        ServerData server = client.getCurrentServer();
        if (server == null || server.ip == null || server.ip.isBlank()) return null;
        return normalizeServerKey(server.ip);
    }

    public static String normalizeServerKey(String value) {
        if (value == null) return null;
        String raw = value.trim();
        if (raw.isEmpty()) return null;
        try {
            ServerAddress address = ServerAddress.parseString(raw);
            if (address != null) {
                String host = normalizeHost(address.getHost());
                int port = address.getPort();
                if (host != null && !host.isBlank() && port > 0) return host + ":" + port;
            }
        } catch (RuntimeException ignored) {
        }
        String host = normalizeHost(raw);
        return host == null ? null : host + ":25565";
    }

    private static String normalizeHost(String value) {
        if (value == null) return null;
        String host = value.trim().toLowerCase();
        while (host.endsWith(".")) host = host.substring(0, host.length() - 1);
        return host.isEmpty() ? null : host;
    }
}
