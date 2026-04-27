package wily.legacy.mixin.base.client;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AbstractContainerScreen.class)
public interface AbstractContainerScreenAccessor {
    @Accessor("imageWidth")
    void legacy$setImageWidth(int imageWidth);

    @Accessor("imageHeight")
    void legacy$setImageHeight(int imageHeight);
}
