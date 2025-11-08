package wily.legacy.mixin.base.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.*;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.client.LegacyOptions;
import wily.legacy.client.screen.LegacyLoading;

import static wily.legacy.Legacy4JClient.legacyLoadingScreen;

@Mixin(/*? if <1.20.5 {*//*GenericDirtMessageScreen*//*?} else {*/GenericMessageScreen/*?}*/.class)
public class LegacyLoadingMessageScreenMixin extends Screen implements LegacyLoading {
    protected LegacyLoadingMessageScreenMixin(Component component) {
        super(component);
    }

    @Inject(method = /*? if <1.20.5 {*//*"render"*//*?} else {*/"renderBackground"/*?}*/, at = @At("HEAD"), cancellable = true)
    public void render(GuiGraphics guiGraphics, int i, int j, float f, CallbackInfo ci) {
        if (LegacyOptions.legacyLoadingAndConnecting.get()) {
            ci.cancel();
            legacyLoadingScreen.prepareRender(minecraft, width, height, getTitle(), null, 0, false);
            legacyLoadingScreen.render(guiGraphics, i, j, f);
        }
    }

    //? if >=1.20.5 {
    @Inject(method = "init", at = @At("HEAD"), cancellable = true)
    public void init(CallbackInfo ci) {
        if (LegacyOptions.legacyLoadingAndConnecting.get()) {
            ci.cancel();
        }
    }
    //?}

    @Override
    public float getProgress() {
        return legacyLoadingScreen.getProgress();
    }

    @Override
    public void setProgress(float progress) {
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
