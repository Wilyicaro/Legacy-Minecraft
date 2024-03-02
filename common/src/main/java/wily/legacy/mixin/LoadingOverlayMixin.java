package wily.legacy.mixin;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.LoadingOverlay;
import net.minecraft.client.gui.screens.Overlay;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ReloadInstance;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import wily.legacy.LegacyMinecraft;
import wily.legacy.client.controller.ControllerComponent;

import java.util.Optional;
import java.util.function.Consumer;

@Mixin(LoadingOverlay.class)
public class LoadingOverlayMixin extends Overlay {
    private static final ResourceLocation[] INTROS = new ResourceLocation[]{new ResourceLocation(LegacyMinecraft.MOD_ID, "textures/gui/intro/eula.png"),new ResourceLocation(LegacyMinecraft.MOD_ID,"textures/gui/intro/mojang.png"),new ResourceLocation(LegacyMinecraft.MOD_ID,"textures/gui/intro/4j.png")};
    @Unique
    private static boolean finishedIntro = false;

    @Shadow @Final private ReloadInstance reload;

    @Shadow @Final private Consumer<Optional<Throwable>> onFinish;

    @Shadow @Final private Minecraft minecraft;
    private long initTime = Util.getMillis();

    @Override
    public void render(GuiGraphics guiGraphics, int i, int j, float f) {
        float timer = (Util.getMillis() - initTime) / 3200f;
        if (!finishedIntro && timer % INTROS.length >= INTROS.length - 0.01f) finishedIntro = true;
        if (!finishedIntro) {
            if ((InputConstants.isKeyDown(minecraft.getWindow().getWindow(), InputConstants.KEY_RETURN) || ControllerComponent.DOWN_BUTTON.componentState.pressed) && reload.isDone())
                finishedIntro = true;
            if (timer % INTROS.length >= INTROS.length - 0.01f) finishedIntro = true;

            guiGraphics.fill(RenderType.guiOverlay(), 0, 0, guiGraphics.guiWidth(), guiGraphics.guiHeight(), 0xFFFFFFFF);
            RenderSystem.enableBlend();
            float last = (float) Math.ceil(timer) - timer;
            guiGraphics.setColor(1.0f, 1.0f, 1.0f, last <= 0.4f ? last * 2.5f : last > 0.6f ? (1 - last) * 2.5f : 1.0f);
            guiGraphics.blit(INTROS[(int) (timer % INTROS.length)], (guiGraphics.guiWidth() - guiGraphics.guiHeight() * 320 / 180) / 2, 0, 0, 0, guiGraphics.guiHeight() * 320 / 180, guiGraphics.guiHeight(), guiGraphics.guiHeight() * 320 / 180, guiGraphics.guiHeight());
            guiGraphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);
            RenderSystem.disableBlend();
        }

        if (finishedIntro) {
            if (this.minecraft.screen != null) {
                this.minecraft.screen.render(guiGraphics, 0, 0, f);
            }
            if (timer >= 1) minecraft.setOverlay(null);
            if (reload.isDone()) {
                try {
                    this.reload.checkExceptions();
                    this.onFinish.accept(Optional.empty());
                } catch (Throwable throwable) {
                    this.onFinish.accept(Optional.of(throwable));
                }
            }
        }
    }
}
