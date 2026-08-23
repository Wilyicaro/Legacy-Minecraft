package wily.legacy.client;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

public interface LegacyItemInHandRenderer {
    void legacy$setRenderedItem(InteractionHand hand, ItemStack item);
}
