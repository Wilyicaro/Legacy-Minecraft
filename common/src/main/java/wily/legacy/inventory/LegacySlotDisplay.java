package wily.legacy.inventory;

import net.minecraft.Util;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.Slot;
import wily.legacy.util.Offset;

import java.util.function.Function;

public interface LegacySlotDisplay {
    LegacySlotDisplay DEFAULT = new LegacySlotDisplay(){};

    record IconHolderOverride(ResourceLocation sprite){
        public static Function<ResourceLocation,IconHolderOverride> CACHE = Util.memoize(IconHolderOverride::new);
        public static IconHolderOverride create(ResourceLocation sprite){
            return CACHE.apply(sprite);
        }
        public static IconHolderOverride EMPTY = new IconHolderOverride(null);
    }
    default int getWidth(){
        return 21;
    }
    default int getHeight(){
        return getWidth();
    }
    default boolean isVisible(){
        return true;
    }
    default Offset getOffset(){
        return Offset.ZERO;
    }
    default ResourceLocation getIconSprite(){
        return null;
    }
    default IconHolderOverride getIconHolderOverride(){
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
            c.setLegacySlot(legacySlot);
            c.setX(x);
            c.setY(y);
        }
        return slot;
    }

}
