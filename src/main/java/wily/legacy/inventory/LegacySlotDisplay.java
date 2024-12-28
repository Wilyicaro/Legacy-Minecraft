package wily.legacy.inventory;

import net.minecraft.Util;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import wily.factoryapi.base.ArbitrarySupplier;

import java.util.function.Function;

public interface LegacySlotDisplay {
    LegacySlotDisplay DEFAULT = new LegacySlotDisplay(){};

    ArbitrarySupplier<ResourceLocation> EMPTY_OVERRIDE = ArbitrarySupplier.empty();

    default int getWidth(){
        return 21;
    }

    default int getHeight(){
        return getWidth();
    }

    default boolean isVisible(){
        return true;
    }

    default Vec3 getOffset(){
        return Vec3.ZERO;
    }

    default ResourceLocation getIconSprite(){
        return null;
    }

    default ArbitrarySupplier<ResourceLocation> getIconHolderOverride(){
        return null;
    }

    default ItemStack getItemOverride(){
        return null;
    }

    static LegacySlotDisplay of(Slot slot){
        return slot instanceof LegacySlotDisplay d ? d : DEFAULT;
    }

    static Slot override(Slot slot){
        return override(slot,DEFAULT);
    }

    static Slot override(Slot slot,int x, int y){
        return override(slot,x,y,DEFAULT);
    }

    static Slot override(Slot slot, LegacySlotDisplay legacySlot){
        return override(slot,slot.x,slot.y,legacySlot);
    }

    static Slot override(Slot slot, int x, int y, LegacySlotDisplay legacySlot){
        if (slot instanceof LegacySlot c){
            c.setDisplay(legacySlot);
            c.setX(x);
            c.setY(y);
        }
        return slot;
    }

}
