package wily.legacy.mixin;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.LoadingOverlay;
import net.minecraft.client.gui.screens.Overlay;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.server.packs.resources.ReloadInstance;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import wily.legacy.client.LegacyResourceManager;
import wily.legacy.client.controller.ControllerBinding;
import wily.legacy.util.ScreenUtil;

import java.util.Optional;
import java.util.function.Consumer;

import static wily.legacy.client.LegacyResourceManager.INTROS;

@Mixin(LoadingOverlay.class)
public class LoadingOverlayMixin extends Overlay {
    @Unique
    private static boolean finishedIntro = false;
    @Unique
    private static boolean loadIntroLocation = false;

    @Shadow @Final private ReloadInstance reload;

    @Shadow @Final private Consumer<Optional<Throwable>> onFinish;

    @Shadow @Final private Minecraft minecraft;
    @Shadow private long fadeOutStart;
    @Shadow @Final private boolean fadeIn;
    @Shadow private long fadeInStart;
    private long initTime = Util.getMillis();
    @Override
    public void render(GuiGraphics guiGraphics, int i, int j, float f) {
        if (!loadIntroLocation){
            loadIntroLocation = true;
            LegacyResourceManager.registerIntroLocations(minecraft.getResourceManager());
        }
        float timer = (Util.getMillis() - initTime) / 3200f;
        if (!finishedIntro && timer % INTROS.size() >= INTROS.size() - 0.01f && reload.isDone()) finishedIntro = true;
        if (!finishedIntro) {
            if ((InputConstants.isKeyDown(minecraft.getWindow().getWindow(), InputConstants.KEY_RETURN) || ControllerBinding.DOWN_BUTTON.bindingState.pressed) && reload.isDone()) finishedIntro = true;
            if (timer % INTROS.size() >= INTROS.size() - 0.01f) finishedIntro = true;

            guiGraphics.fill(RenderType.guiOverlay(), 0, 0, guiGraphics.guiWidth(), guiGraphics.guiHeight(), 0xFFFFFFFF);
            RenderSystem.enableBlend();
            float last = (float) Math.ceil(timer) - timer;
            guiGraphics.setColor(1.0f, 1.0f, 1.0f, last <= 0.4f ? last * 2.5f : last > 0.6f ? (1 - last) * 2.5f : 1.0f);
            guiGraphics.blit(INTROS.get((int) (timer % INTROS.size())), (guiGraphics.guiWidth() - guiGraphics.guiHeight() * 320 / 180) / 2, 0, 0, 0, guiGraphics.guiHeight() * 320 / 180, guiGraphics.guiHeight(), guiGraphics.guiHeight() * 320 / 180, guiGraphics.guiHeight());
            guiGraphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);
            RenderSystem.disableBlend();
        }

        if (finishedIntro) {
            float h;
            long m = Util.getMillis();
            if (this.fadeIn && this.fadeInStart == -1L) {
                this.fadeInStart = m;
            }
            float g = this.fadeOutStart > -1L ? (float)(m - this.fadeOutStart) / 1000.0f : -1.0f;
            h = this.fadeInStart > -1L ? (float)(m - this.fadeInStart) / 500.0f : -1.0f;
            if ((minecraft.isGameLoadFinished() && reload.isDone()) && minecraft.screen != null) this.minecraft.screen.renderWithTooltip(guiGraphics, 0, 0, f);
            else {
                GlStateManager._clearColor(0, 0, 0, 1.0f);
                GlStateManager._clear(16384, Minecraft.ON_OSX);
                guiGraphics.fill(RenderType.guiOverlay(),0,0,guiGraphics.guiWidth(),guiGraphics.guiHeight(),0);
            }
            if (g < 1.0f && !reload.isDone() && minecraft.isGameLoadFinished())
                ScreenUtil.drawGenericLoading(guiGraphics, (guiGraphics.guiWidth() - 75) / 2, (guiGraphics.guiHeight() - 75) / 2);

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
