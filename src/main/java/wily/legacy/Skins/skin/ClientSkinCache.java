package wily.legacy.Skins.skin;

import net.minecraft.client.Minecraft;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ClientSkinCache {
    private static final Map<UUID, String> SKINS_BY_UUID = new ConcurrentHashMap<>();
    private static final Map<String, String> SKINS_BY_NAME = new ConcurrentHashMap<>();
    private static final ThreadLocal<Integer> BYPASS_DEPTH = ThreadLocal.withInitial(() -> 0);

    private ClientSkinCache() {
    }

    public static void set(UUID uuid, String skinId) {
        if (uuid == null) return;
        if (skinId == null || skinId.isBlank()) {
            SKINS_BY_UUID.remove(uuid);
        } else {
            SKINS_BY_UUID.put(uuid, skinId);
        }


        try {
            var mc = Minecraft.getInstance();
            if (mc != null) {
                if (mc.level != null) {
                    var p = mc.level.getPlayerByUUID(uuid);
                    if (p != null) {
                        setName(p.getScoreboardName(), skinId);
                        return;
                    }
                }
                var conn = mc.getConnection();
                if (conn != null) {
                    var info = conn.getPlayerInfo(uuid);
                    if (info != null && info.getProfile() != null) {


                        setName(info.getProfile().name(), skinId);
                    }
                }
            }
        } catch (Throwable ignored) {
        }
    }

    public static void setName(String scoreboardName, String skinId) {
        if (scoreboardName == null || scoreboardName.isBlank()) return;
        if (skinId == null || skinId.isBlank()) {
            SKINS_BY_NAME.remove(scoreboardName);
        } else {
            SKINS_BY_NAME.put(scoreboardName, skinId);
        }
    }

    public static String get(UUID uuid) {
        return uuid == null ? null : SKINS_BY_UUID.get(uuid);
    }

    public static String getByName(String scoreboardName) {
        return scoreboardName == null ? null : SKINS_BY_NAME.get(scoreboardName);
    }


    public static String get(UUID uuid, String scoreboardName) {
        String v = get(uuid);
        return (v == null || v.isBlank()) ? getByName(scoreboardName) : v;
    }

    public static void clear() {
        SKINS_BY_UUID.clear();
        SKINS_BY_NAME.clear();
    }

    public static void pushBypassSkinOverride() {
        BYPASS_DEPTH.set(BYPASS_DEPTH.get() + 1);
    }

    public static void popBypassSkinOverride() {
        int d = BYPASS_DEPTH.get() - 1;
        BYPASS_DEPTH.set(Math.max(0, d));
    }

    public static boolean isSkinOverrideBypassed() {
        return BYPASS_DEPTH.get() > 0;
    }
}
