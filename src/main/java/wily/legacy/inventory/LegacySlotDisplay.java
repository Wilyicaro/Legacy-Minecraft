package wily.legacy.inventory;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec2;
import wily.factoryapi.base.ArbitrarySupplier;

public interface LegacySlotDisplay {
    LegacySlotDisplay DEFAULT = new LegacySlotDisplay() {
    };
    ArbitrarySupplier<ResourceLocation> EMPTY_OVERRIDE = ArbitrarySupplier.empty();
    LegacySlotDisplay VANILLA = new LegacySlotDisplay() {
        @Override
        public int getWidth() {
            return 18;
        }

        @Override
        public ArbitrarySupplier<ResourceLocation> getIconHolderOverride() {
            return EMPTY_OVERRIDE;
        }
    };

    static LegacySlotDisplay of(Slot slot) {
        return slot instanceof LegacySlot legacySlot ? legacySlot.getDisplay() : VANILLA;
    }

    static boolean isVisibleAndActive(Slot slot) {
        return slot.isActive() && LegacySlotDisplay.of(slot).isVisible();
    }

    static Slot override(Slot slot) {
        return override(slot, DEFAULT);
    }

    static Slot override(Slot slot, int x, int y) {
        return override(slot, x, y, DEFAULT);
    }

    static Slot override(Slot slot, LegacySlotDisplay legacySlot) {
        if (slot instanceof LegacySlot c) {
            c.setDisplay(legacySlot);
        }
        return slot;
    }

    static Slot override(Slot slot, int x, int y, LegacySlotDisplay legacySlot) {
        if (slot instanceof LegacySlot c) {
            c.setDisplay(legacySlot);
            c.setX(x);
            c.setY(y);
        }
        return slot;
    }

    default int getWidth() {
        return 21;
    }

    default int getHeight() {
        return getWidth();
    }

    default boolean isVisible() {
        return true;
    }

    default Vec2 getOffset() {
        return Vec2.ZERO;
    }

    default ResourceLocation getIconSprite() {
        return null;
    }

    default ArbitrarySupplier<ResourceLocation> getIconHolderOverride() {
        return null;
    }

    default ItemStack getItemOverride() {
        return null;
    }

    default boolean isWarning() {
        return false;
    }

}
