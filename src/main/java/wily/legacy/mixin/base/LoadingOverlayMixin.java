package wily.legacy.mixin.base;

import com.mojang.blaze3d.platform.GlStateManager;
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
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.factoryapi.FactoryAPI;
import wily.factoryapi.base.client.FactoryGuiGraphics;
import wily.factoryapi.base.client.MinecraftAccessor;
import wily.legacy.Legacy4J;
import wily.legacy.client.LegacyResourceManager;
import wily.legacy.client.controller.ControllerBinding;
import wily.legacy.util.ScreenUtil;

import java.util.Optional;
import java.util.function.Consumer;

import static wily.legacy.client.LegacyResourceManager.INTROS;

@Mixin(LoadingOverlay.class)
public abstract class LoadingOverlayMixin extends Overlay {
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
    @Unique
    private long initTime;
    private static ResourceLocation BACKGROUND = Legacy4J.createModLocation("textures/gui/intro/background.png");
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    public void render(GuiGraphics guiGraphics, int i, int j, float f, CallbackInfo ci) {
        ci.cancel();
        if (!loadIntroLocation){
            initTime = Util.getMillis();
            loadIntroLocation = true;
            LegacyResourceManager.registerIntroLocations(minecraft.getResourceManager());
        }
        float timer = (Util.getMillis() - initTime) / 3200f;
        if (!finishedIntro && timer % INTROS.size() >= INTROS.size() - 0.01f && reload.isDone()) finishedIntro = true;
        if (!finishedIntro) {
            if ((InputConstants.isKeyDown(minecraft.getWindow().getWindow(), InputConstants.KEY_RETURN) || GLFW.glfwGetMouseButton(minecraft.getWindow().getWindow(),GLFW.GLFW_MOUSE_BUTTON_1) == GLFW.GLFW_PRESS || ControllerBinding.DOWN_BUTTON.bindingState.pressed) && reload.isDone()) finishedIntro = true;
            if (timer % INTROS.size() >= INTROS.size() - 0.01f) finishedIntro = true;

            guiGraphics.fill(RenderType.guiOverlay(), 0, 0, guiGraphics.guiWidth(), guiGraphics.guiHeight(), 0xFFFFFFFF);
            //? if <1.21.2 {
            /*FactoryGuiGraphics.of(guiGraphics).blit(BACKGROUND, 0, 0,0,0, guiGraphics.guiWidth(), guiGraphics.guiHeight(),guiGraphics.guiWidth(), guiGraphics.guiHeight());
            *///?}
            RenderSystem.enableBlend();
            float last = (float) Math.ceil(timer) - timer;
            FactoryGuiGraphics.of(guiGraphics).setColor(1.0f, 1.0f, 1.0f, last <= 0.4f ? last * 2.5f : last > 0.6f ? (1 - last) * 2.5f : 1.0f);
            FactoryGuiGraphics.of(guiGraphics).blit(INTROS.get((int) (timer % INTROS.size())), (guiGraphics.guiWidth() - guiGraphics.guiHeight() * 320 / 180) / 2, 0, 0, 0, guiGraphics.guiHeight() * 320 / 180, guiGraphics.guiHeight(), guiGraphics.guiHeight() * 320 / 180, guiGraphics.guiHeight());
            FactoryGuiGraphics.of(guiGraphics).clearColor();
            //? if >=1.21.2 {
            FactoryGuiGraphics.of(guiGraphics).blit(BACKGROUND, 0, 0,0,0, guiGraphics.guiWidth(), guiGraphics.guiHeight(),guiGraphics.guiWidth(), guiGraphics.guiHeight());
            //?}
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
            if ((MinecraftAccessor.getInstance().hasGameLoaded() && reload.isDone()) && minecraft.screen != null) this.minecraft.screen.renderWithTooltip(guiGraphics, 0, 0, f);
            else {
                GlStateManager._clearColor(0, 0, 0, 1.0f);
                GlStateManager._clear(16384/*? if <1.21.2 {*//*, Minecraft.ON_OSX*//*?}*/);
                guiGraphics.fill(RenderType.guiOverlay(),0,0,guiGraphics.guiWidth(),guiGraphics.guiHeight(),0);
            }
            if (g < 1.0f && !reload.isDone() && MinecraftAccessor.getInstance().hasGameLoaded())
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
