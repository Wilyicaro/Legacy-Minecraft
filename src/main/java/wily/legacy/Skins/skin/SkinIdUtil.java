package wily.legacy.Skins.skin;

public final class SkinIdUtil {
    public static final String PACK_DEFAULT = "default";
    public static final String PACK_FAVOURITES = "favourites";
    public static final String AUTO_SELECT = "auto_select";
    private SkinIdUtil() { }
    public static boolean hasSkin(String skinId) { return skinId != null && !skinId.isBlank(); }
    public static boolean isAutoSelect(String skinId) { return AUTO_SELECT.equals(skinId); }
    public static boolean isBlankOrAutoSelect(String skinId) { return !hasSkin(skinId) || isAutoSelect(skinId); }
    public static String normalize(String skinId) { return skinId == null ? "" : skinId; }
    public static boolean isFavouritesPack(String packId) { return PACK_FAVOURITES.equals(packId); }
}
