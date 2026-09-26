package wily.legacy.inventory;

//? if >=1.20.5 {

import net.minecraft.core.component.DataComponents;
import net.minecraft.util.StringUtil;
        //?}
import net.minecraft.SharedConstants;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public interface RenameItemMenu {

    static String validateName(String string) {
        if (string == null || string.isBlank()) return null;
        String string2 = /*? if <1.20.5 {*//*SharedConstants*//*?} else {*/StringUtil/*?}*/.filterText(string);
        if (string2.length() <= 50) {
            return string2;
        }
        return null;
    }

    static String getItemName(ItemStack stack) {
        return stack.is(Items.FILLED_MAP) && !stack.has(DataComponents.CUSTOM_NAME) ? stack.getItem().getName(stack).getString() : stack.getHoverName().getString();
    }

    String getResultItemName();

    void setResultItemName(String name);
}
