//? if <1.20.5 {
/*package wily.legacy.mixin.base;

import net.minecraft.world.item.Item;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import wily.legacy.util.ItemAccessor;

@Mixin(Item.class)
public class ItemMixin implements ItemAccessor {

    @Mutable
    @Shadow @Final private int maxStackSize;

    @Override
    public void setMaxStackSize(int i) {
        maxStackSize = i;
    }

    @Override
    public void setRecordLengthInTicks(int i) {
    }
}
*///?}
