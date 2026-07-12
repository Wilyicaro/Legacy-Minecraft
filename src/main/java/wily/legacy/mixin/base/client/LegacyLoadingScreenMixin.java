package wily.legacy.mixin.base.client;

import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.factoryapi.base.client.UIAccessor;
import wily.legacy.client.LegacyOptions;
import wily.legacy.client.screen.LegacyLoading;
import wily.legacy.util.LegacyComponents;

@Mixin({LevelLoadingScreen.class, ProgressScreen.class, ConnectScreen.class})
public class LegacyLoadingScreenMixin extends Screen implements LegacyLoading {
    @Unique
    private static final long LEGACY_PROGRESS_CYCLE_MS = 1000L;
    @Unique
    private long legacy$loadingStarted;

    protected LegacyLoadingScreenMixin(Component component) {
        super(component);
    }

    @Unique
    private Screen self() {
        return this;
    }

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    public void render(GuiGraphics guiGraphics, int i, int j, float f, CallbackInfo ci) {
        if (LegacyOptions.legacyLoadingAndConnecting.get()) {
            ci.cancel();
            Component lastLoadingHeader = null;
            Component lastLoadingStage = null;
            boolean genericLoading = false;
            float progress = 0;
            if (self() instanceof LevelLoadingScreen) {
                lastLoadingHeader = LegacyComponents.INITIALIZING;
                lastLoadingStage = LegacyComponents.LOADING_SPAWN_AREA;
                progress = legacy$loadingProgress();
            }
            if (self() instanceof ProgressScreen p) {
                lastLoadingHeader = p.header;
                lastLoadingStage = p.stage;
                if (minecraft.level != null && minecraft.level.dimension() != Level.OVERWORLD) {
                    genericLoading = true;
                }
                progress = p.progress / 100f;
            }
            if (self() instanceof ConnectScreen p) {
                lastLoadingHeader = p.status;
                progress = -1.0F;
            }
            getLoadingRenderer().prepareRender(minecraft, UIAccessor.of(this), lastLoadingHeader, lastLoadingStage, progress, genericLoading);
            getLoadingRenderer().render(guiGraphics, i, j, f);
        }
    }

    @Unique
    private float legacy$loadingProgress() {
        long now = Util.getMillis();
        if (legacy$loadingStarted == 0L) legacy$loadingStarted = now;
        long elapsed = (now - legacy$loadingStarted) % LEGACY_PROGRESS_CYCLE_MS;
        return elapsed * 100 / LEGACY_PROGRESS_CYCLE_MS / 100.0F;
    }
}
