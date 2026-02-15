package wily.legacy.Skins.client.gui;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

final class ProfilePropertyUtil {
    private ProfilePropertyUtil() {
    }

    static void putProfileProperty(GameProfile profile, String key, Property value) {
        if (profile == null || key == null || value == null) return;
        try {
            Method m = GameProfile.class.getMethod("getProperties");
            Object pm = m.invoke(profile);
            if (pm instanceof PropertyMap map) {
                map.put(key, value);
                return;
            }
        } catch (Throwable ignored) {
        }
        try {
            Method m = GameProfile.class.getMethod("properties");
            Object pm = m.invoke(profile);
            if (pm instanceof PropertyMap map) {
                map.put(key, value);
            }
        } catch (Throwable ignored) {
        }
    }

    static PropertyMap getPropertyMap(GameProfile profile) {
        if (profile == null) return null;
        try {
            Method m = GameProfile.class.getMethod("getProperties");
            Object pm = m.invoke(profile);
            if (pm instanceof PropertyMap map) return map;
        } catch (Throwable ignored) {
        }
        try {
            Method m = GameProfile.class.getMethod("properties");
            Object pm = m.invoke(profile);
            if (pm instanceof PropertyMap map) return map;
        } catch (Throwable ignored) {
        }
        return null;
    }

    static boolean hasTexturesProperty(GameProfile profile) {
        PropertyMap pm = getPropertyMap(profile);
        if (pm == null) return false;
        try {
            if (pm.containsKey("textures")) {
                try {
                    var c = pm.get("textures");
                    return c != null && !c.isEmpty();
                } catch (Throwable ignored) {
                    return true;
                }
            }
        } catch (Throwable ignored) {
        }
        try {
            var c = pm.get("textures");
            return c != null && !c.isEmpty();
        } catch (Throwable ignored) {
            return false;
        }
    }

    static String getTexturesB64(GameProfile profile) {
        PropertyMap pm = getPropertyMap(profile);
        if (pm == null) return null;
        try {
            var c = pm.get("textures");
            if (c == null) return null;
            for (Object o : c) {
                if (!(o instanceof Property p)) continue;
                try {
                    Method m = Property.class.getMethod("value");
                    Object v = m.invoke(p);
                    if (v != null) return String.valueOf(v);
                } catch (Throwable ignored) {
                }
                try {
                    Method m = Property.class.getMethod("getValue");
                    Object v = m.invoke(p);
                    if (v != null) return String.valueOf(v);
                } catch (Throwable ignored) {
                }
                try {
                    String s = p.toString();
                    if (s != null && s.contains("value=")) {
                        int i = s.indexOf("value=");
                        if (i >= 0) {
                            String sub = s.substring(i + 6);
                            int j = sub.indexOf(',');
                            if (j > 0) sub = sub.substring(0, j);
                            sub = sub.replace("}", "").trim();
                            if (!sub.isEmpty()) return sub;
                        }
                    }
                } catch (Throwable ignored) {
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    static String getProfileName(GameProfile profile) {
        if (profile == null) return "Player";
        for (String mn : new String[]{"getName", "name", "getProfileName"}
        ) {
            try {
                Method m = GameProfile.class.getMethod(mn);
                Object r = m.invoke(profile);
                if (r != null) return String.valueOf(r);
            } catch (Throwable ignored) {
            }
        }
        return "Player";
    }

    static UUID getProfileUUID(GameProfile profile) {
        if (profile == null) return null;
        for (String mn : new String[]{"getId", "id", "getUuid", "getUUID", "uuid"}
        ) {
            try {
                Method m = GameProfile.class.getMethod(mn);
                Object r = m.invoke(profile);
                UUID u = coerceToUUID(r);
                if (u != null) return u;
            } catch (Throwable ignored) {
            }
        }
        for (String fn : new String[]{"id", "uuid", "UUID"}
        ) {
            try {
                var f = GameProfile.class.getDeclaredField(fn);
                f.setAccessible(true);
                Object r = f.get(profile);
                UUID u = coerceToUUID(r);
                if (u != null) return u;
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    static UUID coerceToUUID(Object r) {
        if (r == null) return null;
        if (r instanceof UUID u) return u;
        if (r instanceof java.util.Optional<?> opt) {
            return coerceToUUID(opt.orElse(null));
        }
        if (r instanceof String s) {
            s = s.trim();
            if (s.isEmpty()) return null;
            if (s.length() == 32 && s.matches("(?i)[0-9a-f]{32}")) {
                s = s.substring(0, 8) + "-" + s.substring(8, 12) + "-" + s.substring(12, 16) + "-" + s.substring(16, 20) + "-" + s.substring(20);
            }
            try {
                return UUID.fromString(s);
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    static UUID stablePreviewUUID(String key) {
        return UUID.nameUUIDFromBytes(("consoleskins:" + key).getBytes(StandardCharsets.UTF_8));
    }
}
