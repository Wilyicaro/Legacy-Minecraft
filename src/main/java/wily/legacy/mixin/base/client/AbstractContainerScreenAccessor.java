package wily.legacy.mixin.base.client;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AbstractContainerScreen.class)
public interface AbstractContainerScreenAccessor {
    @Accessor("imageWidth")
    @Mutable
    void legacy$setImageWidth(int imageWidth);

    @Accessor("imageHeight")
    @Mutable
    void legacy$setImageHeight(int imageHeight);
}
