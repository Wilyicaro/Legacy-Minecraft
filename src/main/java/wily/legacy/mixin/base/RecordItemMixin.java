//? if <1.20.5 {
/*package wily.legacy.mixin.base;

import net.minecraft.world.item.RecordItem;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import wily.legacy.util.ItemAccessor;

@Mixin(RecordItem.class)
public abstract class RecordItemMixin implements ItemAccessor {
    @Mutable
    @Shadow @Final private int lengthInTicks;

    @Override
    public void setRecordLengthInTicks(int i) {
        lengthInTicks = i;
    }
}
*///?}
