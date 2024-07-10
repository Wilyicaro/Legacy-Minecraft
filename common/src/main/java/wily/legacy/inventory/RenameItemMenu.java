package wily.legacy.inventory;

import net.minecraft.SharedConstants;
import net.minecraft.Util;
import net.minecraft.util.StringUtil;

public interface RenameItemMenu {
    void setResultItemName(String name);
    String getResultItemName();
    static String validateName(String string) {
        if (string == null || Util.isBlank(string)) return null;
        String string2 = SharedConstants.filterText(string);
        if (string2.length() <= 50) {
            return string2;
        }
        return null;
    }
}
