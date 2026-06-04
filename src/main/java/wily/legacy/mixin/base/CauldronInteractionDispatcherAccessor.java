package wily.legacy.mixin.base;

import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.Map;

@Mixin(CauldronInteraction.Dispatcher.class)
public interface CauldronInteractionDispatcherAccessor {
    @Accessor("tags")
    Map<TagKey<Item>, CauldronInteraction> legacy$getTags();

    @Accessor("items")
    Map<Item, CauldronInteraction> legacy$getItems();

    @Invoker("put")
    void legacy$put(Item item, CauldronInteraction interaction);
}
