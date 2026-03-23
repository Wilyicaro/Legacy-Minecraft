package wily.legacy.mixin.base.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.*;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.factoryapi.base.client.UIAccessor;
import wily.legacy.client.LegacyOptions;
import wily.legacy.client.screen.CreateWorldLoadingTracker;
import wily.legacy.client.screen.LegacyLoading;

@Mixin(/*? if <1.20.5 {*//*GenericDirtMessageScreen*//*?} else {*/GenericMessageScreen/*?}*/.class)
public class LegacyLoadingMessageScreenMixin extends Screen implements LegacyLoading {
    protected LegacyLoadingMessageScreenMixin(Component component) {
        super(component);
    }

    @Inject(method = /*? if <1.20.5 {*//*"render"*//*?} else {*/"renderBackground"/*?}*/, at = @At("HEAD"), cancellable = true)
    public void render(GuiGraphics guiGraphics, int i, int j, float f, CallbackInfo ci) {
        if (LegacyOptions.legacyLoadingAndConnecting.get()) {
            ci.cancel();
            Component header = getTitle();
            Component stage = null;
            float progress = 0.0F;
            if (CreateWorldLoadingTracker.isActive()) {
                CreateWorldLoadingTracker.State state = CreateWorldLoadingTracker.preparing(header, null, progress);
                header = state.header();
                stage = state.stage();
                progress = state.progress();
            }
            getLoadingRenderer().prepareRender(minecraft, UIAccessor.of(this), header, stage, progress, false);
            getLoadingRenderer().render(guiGraphics, i, j, f);
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
}
