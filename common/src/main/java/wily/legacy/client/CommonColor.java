package wily.legacy.client;

import com.google.gson.JsonElement;
import wily.legacy.util.JsonUtil;
import wily.legacy.util.Stocker;

import java.util.HashMap;
import java.util.Map;

public class CommonColor extends Stocker<Integer> {
    public static final Map<String, CommonColor> COMMON_COLORS = new HashMap<>();

    public static final CommonColor CHAT_BACKGROUND = registerCommonColor("chat_background",0x323232);
    public static final CommonColor INVENTORY_GRAY_TEXT = registerCommonColor("inventory_gray_text", 0x323232);
    public static final CommonColor WIDGET_TEXT = registerCommonColor("widget_text", 0xFFFFFF);
    public static final CommonColor HIGHLIGHTED_WIDGET_TEXT = registerCommonColor("highlighted_widget_text", 0xFFFF00);
    public static final CommonColor TITLE_TEXT = registerCommonColor("title_text", 0xFFFFFF);
    public static final CommonColor STAGE_TEXT = registerCommonColor("stage_text", 0xFFFFFF);
    public static final CommonColor SELECTED_STORAGE_SAVE = registerCommonColor("selected_storage_save",0xFFFFFF00);
    public static final CommonColor STORAGE_SAVE = registerCommonColor("storage_save",0xFF8C9DE2);
    public static final CommonColor EXPERIENCE_TEXT = registerCommonColor("experience_text", 0x80FF20);
    public static final CommonColor INSUFFICIENT_EXPERIENCE_TEXT = registerCommonColor("insufficient_experience_text", 0x407F10);
    public static final CommonColor ANVIL_ERROR_TEXT = registerCommonColor("anvil_error_text", 0xFF6060);
    public static final CommonColor ENCHANTMENT_TEXT = registerCommonColor("enchantment_text",0x685E4A);
    public static final CommonColor HIGHLIGHTED_ENCHANTMENT_TEXT = registerCommonColor("highlighted_enchantment_text",0xFFFF80);

    public final int defaultColor;

    public CommonColor(Integer obj) {
        super(obj);
        defaultColor = obj;
    }

    public void tryParse(JsonElement e) {
        Integer i = JsonUtil.optionalJsonColor(e, null);
        if (i != null) set(i);
    }

    public void reset() {
        set(defaultColor);
    }

    public static CommonColor registerCommonColor(String id, int defaultValue) {
        CommonColor color = new CommonColor(defaultValue);
        COMMON_COLORS.put(id, color);
        return color;
    }
}
