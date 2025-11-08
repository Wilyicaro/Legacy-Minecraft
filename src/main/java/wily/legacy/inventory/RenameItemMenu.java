package wily.legacy.inventory;

//? if >=1.20.5 {

import net.minecraft.core.component.DataComponents;
import net.minecraft.util.StringUtil;
        //?}
import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public interface RenameItemMenu {

    static String validateName(String string) {
        if (string == null || string.isBlank()) return null;
        String string2 = /*? if <1.20.5 {*//*SharedConstants*//*?} else {*/StringUtil/*?}*/.filterText(string);
        if (string2.length() <= 50) {
            return string2;
        }
        return null;
    }

    String getResultItemName();

    void setResultItemName(String name);
}
