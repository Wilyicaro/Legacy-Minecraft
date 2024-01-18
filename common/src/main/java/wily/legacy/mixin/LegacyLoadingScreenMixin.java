package wily.legacy.mixin;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;

import static wily.legacy.LegacyMinecraftClient.legacyLoadingScreen;

@Mixin({LevelLoadingScreen.class, ProgressScreen.class, GenericDirtMessageScreen.class, ReceivingLevelScreen.class})
public class LegacyLoadingScreenMixin extends Screen {
    protected LegacyLoadingScreenMixin(Component component) {
        super(component);
    }

    Object obj(){
        return this;
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(null);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int i, int j, float f) {
        Component lastLoadingHeader = null;
        Component lastLoadingStage = null;
        boolean genericLoading = false;
        int progress = 0;
        if (obj() instanceof ReceivingLevelScreen) progress = -1;
        if (obj() instanceof LevelLoadingScreen loading) {
            lastLoadingHeader = Component.translatable("connect.connecting");
            lastLoadingStage = Component.translatable("legacy.loading_spawn_area");
            progress = loading.progressListener.getProgress();
        }
        if (obj() instanceof GenericDirtMessageScreen p)
            lastLoadingHeader = p.getTitle();
        if (obj() instanceof ProgressScreen p) {
            lastLoadingHeader = p.header;
            lastLoadingStage = p.stage;
            if (minecraft.level != null && minecraft.level.dimension() != Level.OVERWORLD){
                genericLoading = true;
            }
        }
        legacyLoadingScreen.prepareRender(minecraft,width, height,lastLoadingHeader,lastLoadingStage,progress,genericLoading);
        legacyLoadingScreen.render(guiGraphics,i,j,f);
    }
}
