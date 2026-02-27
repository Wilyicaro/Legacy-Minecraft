package wily.legacy.Skins.skin;

import java.util.Locale;

public final class SkinIdUtil {

    private SkinIdUtil() {
    }

    public static boolean isAutoSelect(String skinId) {
        return SkinIds.AUTO_SELECT.equals(skinId);
    }

    public static boolean isFavouritesPack(String packId) {
        return SkinIds.PACK_FAVOURITES.equals(packId);
    }

    public static boolean containsMinecon(String packId) {
        return packId != null && packId.toLowerCase(Locale.ROOT).contains("minecon");
    }

    public static SkinType typeOf(String skinId) {
        return SkinType.JSON;
    }
}
