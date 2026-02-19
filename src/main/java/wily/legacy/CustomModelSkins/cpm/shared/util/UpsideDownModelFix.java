package wily.legacy.CustomModelSkins.cpm.shared.util;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class UpsideDownModelFix {
    private static final Set<UUID> FLIPPED = ConcurrentHashMap.newKeySet();

    private UpsideDownModelFix() {
    }

    public static void setFlipped(UUID uuid, boolean flipped) {
        if (uuid == null) return;
        if (flipped) FLIPPED.add(uuid);
        else FLIPPED.remove(uuid);
    }

    public static boolean isFlipped(UUID uuid) {
        return uuid != null && FLIPPED.contains(uuid);
    }
}
