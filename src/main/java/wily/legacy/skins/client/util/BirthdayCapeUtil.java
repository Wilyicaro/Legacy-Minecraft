package wily.legacy.skins.client.util;

import net.minecraft.core.ClientAsset;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.PlayerSkin;

import java.time.LocalDate;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class BirthdayCapeUtil {
    private static final String DEBUG_PROPERTY = "legacy.debugBirthdayCape";
    private static final Identifier BIRTHDAY_CAPE_TEXTURE = Identifier.fromNamespaceAndPath(
            "lce_skinpacks",
            "default_skinpacks/default/capes/l4j_birthday.png"
    );
    private static final ClientAsset.ResourceTexture BIRTHDAY_CAPE = new ClientAsset.ResourceTexture(BIRTHDAY_CAPE_TEXTURE, BIRTHDAY_CAPE_TEXTURE);
    private static final Map<PlayerSkin, PlayerSkin> CACHED_SKINS = new ConcurrentHashMap<>();

    private BirthdayCapeUtil() {
    }

    public static boolean isActiveNow() {
        if (Boolean.getBoolean(DEBUG_PROPERTY)) return true;
        LocalDate now = LocalDate.now();
        int month = now.getMonthValue();
        int day = now.getDayOfMonth();
        return month == 12 && day >= 28 || month == 1 && day <= 1;
    }

    public static PlayerSkin apply(PlayerSkin skin, boolean blockedByElytra) {
        if (!isActiveNow() || blockedByElytra || skin == null) return skin;
        ClientAsset.Texture currentCape = skin.cape();
        if (currentCape != null && BIRTHDAY_CAPE_TEXTURE.equals(currentCape.texturePath())) return skin;
        return CACHED_SKINS.computeIfAbsent(skin, BirthdayCapeUtil::withBirthdayCape);
    }

    private static PlayerSkin withBirthdayCape(PlayerSkin skin) {
        return new PlayerSkin(skin.body(), BIRTHDAY_CAPE, skin.elytra(), skin.model(), skin.secure());
    }
}
