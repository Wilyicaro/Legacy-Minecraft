package wily.legacy.skins.client.util;

//? if >1.20.1 {
import net.minecraft.client.resources.PlayerSkin;
//?}
import net.minecraft.resources.ResourceLocation;
import wily.factoryapi.FactoryAPI;

import java.time.LocalDate;
//? if >1.20.1 {
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
//?}

public final class BirthdayCapeUtil {
    private static final String DEBUG_PROPERTY = "legacy.debugBirthdayCape";
    private static final ResourceLocation TEXTURE = FactoryAPI.createLocation("lce_skinpacks", "default_skinpacks/default/capes/l4j_birthday.png");
    //? if >1.20.1 {
    private static final Map<PlayerSkin, PlayerSkin> CACHED_SKINS = new ConcurrentHashMap<>();
    //?}

    private BirthdayCapeUtil() {
    }

    public static ResourceLocation texture() {
        return TEXTURE;
    }

    public static boolean isActiveNow() {
        if (Boolean.getBoolean(DEBUG_PROPERTY)) return true;
        LocalDate now = LocalDate.now();
        int month = now.getMonthValue();
        int day = now.getDayOfMonth();
        return month == 12 && day >= 28 || month == 1 && day <= 1;
    }

    //? if >1.20.1 {
    public static PlayerSkin apply(PlayerSkin skin, boolean blockedByElytra) {
        if (!isActiveNow() || blockedByElytra || skin == null || TEXTURE.equals(skin.capeTexture())) return skin;
        return CACHED_SKINS.computeIfAbsent(skin, BirthdayCapeUtil::withBirthdayCape);
    }

    private static PlayerSkin withBirthdayCape(PlayerSkin skin) {
        return new PlayerSkin(skin.texture(), skin.textureUrl(), TEXTURE, skin.elytraTexture(), skin.model(), skin.secure());
    }
    //?}
}
