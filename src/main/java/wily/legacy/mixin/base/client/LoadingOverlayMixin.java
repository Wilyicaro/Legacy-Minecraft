package wily.legacy.mixin.base.client;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.LoadingOverlay;
import net.minecraft.client.gui.screens.Overlay;
import net.minecraft.server.packs.resources.ReloadInstance;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.factoryapi.base.client.FactoryGuiGraphics;
import wily.factoryapi.base.client.MinecraftAccessor;
import wily.legacy.client.LegacyIntro;
import wily.legacy.client.LegacyOptions;
import wily.legacy.client.LegacyResourceManager;
import wily.legacy.util.client.LegacyRenderUtil;

import java.util.Optional;
import java.util.function.Consumer;

@Mixin(LoadingOverlay.class)
public abstract class LoadingOverlayMixin extends Overlay {
    @Unique
    private static boolean finishedIntro = false;
    @Unique
    private static boolean loadIntroLocation = false;

    @Shadow
    @Final
    private ReloadInstance reload;

    @Shadow
    @Final
    private Consumer<Optional<Throwable>> onFinish;

    @Shadow
    @Final
    private Minecraft minecraft;
    @Shadow
    private long fadeOutStart;
    @Shadow
    @Final
    private boolean fadeIn;
    @Shadow
    private long fadeInStart;
    @Unique
    private long initTime;


    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    public void render(GuiGraphics guiGraphics, int i, int j, float f, CallbackInfo ci) {
        if (LegacyOptions.legacyIntroAndReloading.get()) {
            ci.cancel();
            if (!loadIntroLocation) {
                initTime = Util.getMillis();
                loadIntroLocation = true;
                LegacyResourceManager.loadIntroLocations(minecraft.getResourceManager());
            }
            float timer = LegacyIntro.getTimer(initTime, LegacyResourceManager.intro);
            if (!finishedIntro && LegacyIntro.canSkip(timer, LegacyResourceManager.intro) && reload.isDone())
                finishedIntro = true;
            if (!finishedIntro) {
                LegacyIntro.render(guiGraphics, LegacyResourceManager.intro, timer);
            }

            if (finishedIntro) {
                float h;
                long m = Util.getMillis();
                if (this.fadeIn && this.fadeInStart == -1L) {
                    this.fadeInStart = m;
                }
                float g = this.fadeOutStart > -1L ? (float) (m - this.fadeOutStart) / 1000.0f : -1.0f;
                h = this.fadeInStart > -1L ? (float) (m - this.fadeInStart) / 500.0f : -1.0f;
                if ((MinecraftAccessor.getInstance().hasGameLoaded() && reload.isDone()) && minecraft.screen != null) {
                    this.minecraft.screen.renderWithTooltipAndSubtitles(guiGraphics, 0, 0, f);
                    this.minecraft.setOverlay(null);
                    return;
                } else {
                    FactoryGuiGraphics.of(guiGraphics).blit(LegacyRenderUtil.LOADING_BACKGROUND, 0, 0, 0, 0, guiGraphics.guiWidth(), guiGraphics.guiHeight(), guiGraphics.guiWidth(), guiGraphics.guiHeight());
                }
                if (g < 1.0f && !reload.isDone() && MinecraftAccessor.getInstance().hasGameLoaded())
                    LegacyRenderUtil.drawGenericLoading(guiGraphics, (guiGraphics.guiWidth() - 75) / 2, (guiGraphics.guiHeight() - 75) / 2);

                if (g >= 2.0f)
                    this.minecraft.setOverlay(null);

                if (this.fadeOutStart == -1L && this.reload.isDone() && (!this.fadeIn || h >= 2.0f)) {
                    try {
                        this.reload.checkExceptions();
                        this.onFinish.accept(Optional.empty());
                    } catch (Throwable throwable) {
                        this.onFinish.accept(Optional.of(throwable));
                    }
                    this.fadeOutStart = Util.getMillis();
                    if (this.minecraft.screen != null) {
                        this.minecraft.screen.init(this.minecraft, guiGraphics.guiWidth(), guiGraphics.guiHeight());
                    }
                }
            }
        }
    }
}
