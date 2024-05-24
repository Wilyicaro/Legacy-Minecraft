package wily.legacy.mixin;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static wily.legacy.Legacy4JClient.legacyLoadingScreen;

@Mixin(GenericMessageScreen.class)
public class LegacyLoadingMessageScreenMixin extends Screen {
    protected LegacyLoadingMessageScreenMixin(Component component) {
        super(component);
    }

    public void render(GuiGraphics guiGraphics, int i, int j, float f) {
        legacyLoadingScreen.prepareRender(minecraft,width, height,getTitle(), null,0,false);
        legacyLoadingScreen.render(guiGraphics,i,j,f);
    }
}
