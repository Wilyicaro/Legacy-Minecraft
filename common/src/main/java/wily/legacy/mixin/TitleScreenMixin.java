package wily.legacy.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.LogoRenderer;
import net.minecraft.client.gui.components.SplashRenderer;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.level.storage.LevelStorageSource;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.Legacy4JClient;
import wily.legacy.client.controller.ControllerManager;
import wily.legacy.client.screen.*;
import wily.legacy.util.ScreenUtil;

import java.io.IOException;

@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin extends Screen implements ControlTooltip.Event{
    @Shadow @Final private static Logger LOGGER;

    @Shadow protected abstract boolean checkDemoWorldPresence();

    @Shadow protected abstract void confirmDemo(boolean bl);

    @Shadow @Nullable private SplashRenderer splash;
    private RenderableVList renderableVList = new RenderableVList().layoutSpacing(l->5);

    protected TitleScreenMixin(Component component) {
        super(component);
    }

    @Inject(method = "<init>(ZLnet/minecraft/client/gui/components/LogoRenderer;)V", at = @At("RETURN"))
    public void init(boolean bl, LogoRenderer logoRenderer, CallbackInfo ci) {
        minecraft = Minecraft.getInstance();
        if (minecraft.isDemo()) createDemoMenuOptions();
        else this.createNormalMenuOptions();
        renderableVList.addRenderable(Button.builder(Component.translatable("legacy.menu.mods"), b -> minecraft.setScreen(new ModsScreen(this))).build());
        renderableVList.addRenderable(Button.builder(Component.translatable("options.language"), b -> minecraft.setScreen(new LegacyLanguageScreen(this, this.minecraft.getLanguageManager()))).build());
        renderableVList.addRenderable(Button.builder(Component.translatable("menu.options"), b -> minecraft.setScreen(new HelpOptionsScreen(this))).build());
        renderableVList.addRenderable(Button.builder(Component.translatable("menu.quit"), (button) -> minecraft.setScreen(new ExitConfirmationScreen(this))).build());
    }


    @Inject(method = "init", at = @At("HEAD"), cancellable = true)
    protected void init(CallbackInfo ci) {
        ci.cancel();
        super.init();
        renderableVList.init(this,width / 2 - 112,this.height / 3 + 10,225,0);
    }

    private void createNormalMenuOptions() {
        renderableVList.addRenderable(Button.builder(Component.translatable("legacy.menu.play_game"), (button) -> {
            this.minecraft.setScreen(new PlayGameScreen(this));
        }).build());
    }

    private void createDemoMenuOptions() {
        boolean bl = this.checkDemoWorldPresence();
        renderableVList.addRenderable(Button.builder(Component.translatable("menu.playdemo"), (button) -> {
            if (bl) {
                this.minecraft.createWorldOpenFlows().openWorld("Demo_World", ()-> this.minecraft.setScreen(this));
            } else {
                this.minecraft.createWorldOpenFlows().createFreshLevel("Demo_World", MinecraftServer.DEMO_SETTINGS, WorldOptions.DEMO_OPTIONS, WorldPresets::createNormalWorldDimensions, this);
            }

        }).build());
        Button secondButton;
        renderableVList.addRenderable(secondButton = Button.builder(Component.translatable("menu.resetdemo"), (button) -> {
            LevelStorageSource levelStorageSource = this.minecraft.getLevelSource();

            try {
                LevelStorageSource.LevelStorageAccess levelStorageAccess = levelStorageSource.createAccess("Demo_World");

                try {
                    if (levelStorageAccess.hasWorldData()) {
                        this.minecraft.setScreen(new ConfirmScreen(this::confirmDemo, Component.translatable("selectWorld.deleteQuestion"), Component.translatable("selectWorld.deleteWarning", new Object[]{MinecraftServer.DEMO_SETTINGS.levelName()}), Component.translatable("selectWorld.deleteButton"), CommonComponents.GUI_CANCEL));
                    }
                } catch (Throwable var7) {
                    if (levelStorageAccess != null) {
                        try {
                            levelStorageAccess.close();
                        } catch (Throwable var6) {
                            var7.addSuppressed(var6);
                        }
                    }

                    throw var7;
                }

                if (levelStorageAccess != null) {
                    levelStorageAccess.close();
                }
            } catch (IOException var8) {
                SystemToast.onWorldAccessFailure(this.minecraft, "Demo_World");
                LOGGER.warn("Failed to access demo world", var8);
            }

        }).build());
        secondButton.active = bl;
    }
    @Inject(method = "added", at = @At("RETURN"))
    public void added(CallbackInfo ci) {
        ControllerManager.getHandler().init();
    }
    @Override
    public void renderBackground(GuiGraphics guiGraphics, int i, int j, float f) {
        ScreenUtil.renderDefaultBackground(guiGraphics,true,true);
    }

    @Override
    public boolean keyPressed(int i, int j, int k) {
        if (renderableVList.keyPressed(i,true)) return true;
        return super.keyPressed(i, j, k);
    }

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    public void render(GuiGraphics guiGraphics, int i, int j, float f, CallbackInfo ci) {
        ci.cancel();
        super.render(guiGraphics, i, j, f);

        if (this.splash != null) {
            Legacy4JClient.legacyFont = false;
            this.splash.render(guiGraphics, this.width, this.font, 255 << 24);
            Legacy4JClient.legacyFont = true;
        }
    }


}
