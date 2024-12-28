package wily.legacy.inventory;

//? if >=1.20.5 {
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.StringUtil;
 //?}
import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public interface RenameItemMenu {

    void setResultItemName(String name);
    String getResultItemName();
    static String validateName(String string) {
        if (string == null || string.isBlank()) return null;
        String string2 = /*? if <1.20.5 {*//*SharedConstants*//*?} else {*/StringUtil/*?}*/.filterText(string);
        if (string2.length() <= 50) {
            return string2;
        }
        return null;
    }

    static boolean hasCustomName(ItemStack stack){
        return /*? if <1.20.5 {*//*stack.hasCustomHoverName()*//*?} else {*/stack.has(DataComponents.CUSTOM_NAME)/*?}*/;
    }

    static void setCustomName(ItemStack stack, Component name){
        //? if <1.20.5 {
        /*if (name == null) stack.resetHoverName();
        else stack.setHoverName(name);
        *///?} else {
        stack.set(DataComponents.CUSTOM_NAME,name);
        //?}
    }
}
