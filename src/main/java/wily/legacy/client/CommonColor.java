package wily.legacy.client;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.minecraft.resources.ResourceLocation;
import wily.factoryapi.FactoryAPI;
import wily.factoryapi.util.ListMap;

import java.util.Locale;

public class CommonColor extends CommonValue<Integer> {
    public static final Codec<Integer> RGBA_INT_COLOR_CODEC = Codec.STRING.comapFlatMap((string) -> {
        if (!string.startsWith("#")) {
            return DataResult.error(() -> "Not a color code: " + string);
        } else {
            try {
                int i = (int)Long.parseLong(string.substring(1), 16);
                return DataResult.success(i);
            } catch (NumberFormatException var2) {
                return DataResult.error(() -> "Exception parsing color code: " + var2.getMessage());
            }
        }
    }, i->String.format(Locale.ROOT, "#%08X", i));
    public static final Codec<Integer> INT_COLOR_CODEC = Codec.either(RGBA_INT_COLOR_CODEC,Codec.INT).xmap(e->e.right().orElseGet(e.left()::get), Either::right);

    public static final ListMap<ResourceLocation, CommonColor> COMMON_COLORS = new ListMap<>();

    public static final CommonColor CHAT_BACKGROUND = registerCommonColor("chat_background",0xFF323232);
    public static final CommonColor INVENTORY_GRAY_TEXT = registerCommonColor("inventory_gray_text", 0xFF323232);
    public static final CommonColor WIDGET_TEXT = registerCommonColor("widget_text", 0xFFFFFFFF);
    public static final CommonColor HIGHLIGHTED_WIDGET_TEXT = registerCommonColor("highlighted_widget_text", 0xFFFFFF00);
    public static final CommonColor TITLE_TEXT = registerCommonColor("title_text", 0xFFFFFFFF);
    public static final CommonColor TITLE_TEXT_OUTLINE = registerCommonColor("title_text_outline", 0xFF000000);
    public static final CommonColor STAGE_TEXT = registerCommonColor("stage_text", 0xFFFFFFFF);
    public static final CommonColor TIP_TITLE_TEXT = registerCommonColor("tip_title_text", 0xFFFFFFFF);
    public static final CommonColor TIP_TEXT = registerCommonColor("tip_text", 0xFFFFFFFF);
    public static final CommonColor ACTION_TEXT = registerCommonColor("action_text", 0xFFFFFFFF);
    public static final CommonColor SELECTED_STORAGE_SAVE = registerCommonColor("selected_storage_save",0xFFFFFF00);
    public static final CommonColor STORAGE_SAVE = registerCommonColor("storage_save",0xFF8C9DE2);
    public static final CommonColor EXPERIENCE_TEXT = registerCommonColor("experience_text", 0xFF80FF20);
    public static final CommonColor INSUFFICIENT_EXPERIENCE_TEXT = registerCommonColor("insufficient_experience_text", 0xFFCF1F1D);
    public static final CommonColor ANVIL_ERROR_TEXT = registerCommonColor("anvil_error_text", 0xFFFF6060);
    public static final CommonColor ENCHANTMENT_TEXT = registerCommonColor("enchantment_text",0xFF685E4A);
    public static final CommonColor HIGHLIGHTED_ENCHANTMENT_TEXT = registerCommonColor("highlighted_enchantment_text",0xFFFFFF80);
    public static final CommonColor BLACK = registerCommonColor("black", 0x000000);
    public static final CommonColor DARK_BLUE = registerCommonColor("dark_blue", 0x0000AA);
    public static final CommonColor DARK_GREEN = registerCommonColor("dark_green", 0x00AA00);
    public static final CommonColor DARK_AQUA = registerCommonColor("dark_aqua", 0x00AAAA);
    public static final CommonColor DARK_RED = registerCommonColor("dark_red", 0xAA0000);
    public static final CommonColor DARK_PURPLE = registerCommonColor("dark_purple", 0xAA00AA);
    public static final CommonColor GOLD = registerCommonColor("gold", 0xFFAA00);
    public static final CommonColor GRAY = registerCommonColor("gray", 0xAAAAAA);
    public static final CommonColor DARK_GRAY = registerCommonColor("dark_gray", 0x555555);
    public static final CommonColor BLUE = registerCommonColor("blue", 0x7878ff);
    public static final CommonColor GREEN = registerCommonColor("green", 0x55FF55);
    public static final CommonColor AQUA = registerCommonColor("aqua", 0x55FFFF);
    public static final CommonColor RED = registerCommonColor("red", 0xFF5555);
    public static final CommonColor LIGHT_PURPLE = registerCommonColor("light_purple", 0xFF55FF);
    public static final CommonColor YELLOW = registerCommonColor("yellow", 0xFFFF55);
    public static final CommonColor WHITE = registerCommonColor("white", 0xFFFFFF);
    public static final CommonColor BLOCK_LIGHT = registerCommonColor("block_light", 0xFFFFFF);

    public CommonColor(Integer obj) {
        super(obj, INT_COLOR_CODEC);
    }

    public static CommonColor registerCommonColor(String path, int defaultValue) {
        return registerCommonColor(FactoryAPI.createVanillaLocation(path),defaultValue);
    }

    public static CommonColor registerCommonColor(ResourceLocation id, int defaultValue) {
        CommonColor color = new CommonColor(defaultValue);
        COMMON_COLORS.put(id, color);
        return color;
    }
}
