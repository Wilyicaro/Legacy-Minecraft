package wily.legacy.mixin.base;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.client.screen.LegacyLoading;

import static wily.legacy.Legacy4JClient.legacyLoadingScreen;

@Mixin({LevelLoadingScreen.class, ProgressScreen.class, ReceivingLevelScreen.class, ConnectScreen.class})
public class LegacyLoadingScreenMixin extends Screen implements LegacyLoading {
    protected LegacyLoadingScreenMixin(Component component) {
        super(component);
    }

    Screen self(){
        return this;
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(null);
    }


    @Inject(method = "render",at = @At("HEAD"), cancellable = true)
    public void render(GuiGraphics guiGraphics, int i, int j, float f, CallbackInfo ci) {
        ci.cancel();
        Component lastLoadingHeader = null;
        Component lastLoadingStage = null;
        boolean genericLoading = false;
        int progress = 0;
        if (self() instanceof ReceivingLevelScreen) progress = -1;
        if (self() instanceof LevelLoadingScreen loading) {
            lastLoadingHeader = Component.translatable("legacy.connect.initializing");
            lastLoadingStage = Component.translatable("legacy.loading_spawn_area");
            progress = loading.progressListener.getProgress();
        }
        if (self() instanceof ProgressScreen p) {
            lastLoadingHeader = p.header;
            lastLoadingStage = p.stage;
            if (minecraft.level != null && minecraft.level.dimension() != Level.OVERWORLD){
                genericLoading = true;
            }
        }
        if (self() instanceof ConnectScreen p) {
            lastLoadingHeader = p.status;
        }
        legacyLoadingScreen.prepareRender(minecraft,width, height,lastLoadingHeader,lastLoadingStage,progress,genericLoading);
        legacyLoadingScreen.render(guiGraphics,i,j,f);
    }

    @Override
    public int getProgress() {
        return legacyLoadingScreen.getProgress();
    }

    @Override
    public void setProgress(int progress) {
        legacyLoadingScreen.setProgress(progress);
    }

    @Override
    public Component getLoadingHeader() {
        return legacyLoadingScreen.getLoadingHeader();
    }

    @Override
    public void setLoadingHeader(Component loadingHeader) {
        legacyLoadingScreen.setLoadingHeader(loadingHeader);
    }

    @Override
    public Component getLoadingStage() {
        return legacyLoadingScreen.getLoadingStage();
    }

    @Override
    public void setLoadingStage(Component loadingStage) {
        legacyLoadingScreen.setLoadingStage(loadingStage);
    }

    @Override
    public boolean isGenericLoading() {
        return legacyLoadingScreen.isGenericLoading();
    }

    @Override
    public void setGenericLoading(boolean genericLoading) {
        legacyLoadingScreen.setGenericLoading(genericLoading);
    }
}
