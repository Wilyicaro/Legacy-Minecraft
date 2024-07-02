package wily.legacy.client.screen;

import com.mojang.logging.LogUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.level.storage.LevelStorageSource;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import wily.legacy.Legacy4JClient;

import java.io.IOException;
import java.util.Objects;

import static wily.legacy.util.ScreenUtil.renderDefaultBackground;

public class MainMenuScreen extends RenderableVListScreen {
    private static final Logger LOGGER = LogUtils.getLogger();
    @Nullable
    private SplashRenderer splash;
    @Nullable
    private WarningLabel warningLabel;

    public MainMenuScreen() {
        super(Component.translatable("narrator.screen.title"), b->{});
        controlTooltipRenderer.tooltips.remove(1);
        minecraft = Minecraft.getInstance();
        if (minecraft.isDemo()) createDemoMenuOptions();
        else this.createNormalMenuOptions();
        renderableVList.addRenderable(openScreenButton(Component.translatable("legacy.menu.mods"), () -> new ModsScreen(this)).build());
        renderableVList.addRenderable(openScreenButton(Component.translatable("options.language"), () -> new LegacyLanguageScreen(this, this.minecraft.getLanguageManager())).build());
        renderableVList.addRenderable(openScreenButton(Component.translatable("menu.options"), () -> new HelpOptionsScreen(this)).build());
        renderableVList.addRenderable(Button.builder(Component.translatable("menu.quit"), (button) -> minecraft.setScreen(new ExitConfirmationScreen(this))).build());
    }

    public boolean isPauseScreen() {
        return false;
    }

    public boolean shouldCloseOnEsc() {
        return false;
    }

    protected void init() {
        int l = this.height / 4 + 48;

        super.init();

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

    private boolean checkDemoWorldPresence() {
        try {
            LevelStorageSource.LevelStorageAccess levelStorageAccess = this.minecraft.getLevelSource().createAccess("Demo_World");

            boolean var2;
            try {
                var2 = levelStorageAccess.hasWorldData();
            } catch (Throwable var5) {
                if (levelStorageAccess != null) {
                    try {
                        levelStorageAccess.close();
                    } catch (Throwable var4) {
                        var5.addSuppressed(var4);
                    }
                }

                throw var5;
            }

            if (levelStorageAccess != null) {
                levelStorageAccess.close();
            }

            return var2;
        } catch (IOException var6) {
            SystemToast.onWorldAccessFailure(this.minecraft, "Demo_World");
            LOGGER.warn("Failed to read demo world data", var6);
            return false;
        }
    }


    public void render(GuiGraphics guiGraphics, int i, int j, float f) {
        super.render(guiGraphics, i, j, f);

        if (this.warningLabel != null)
            this.warningLabel.render(guiGraphics, 255 << 24);


        if (this.splash != null) {
            Legacy4JClient.legacyFont = false;
            this.splash.render(guiGraphics, this.width, this.font, 255 << 24);
            Legacy4JClient.legacyFont = true;
        }
    }

    @Override
    public void added() {
        super.added();
        this.splash = this.minecraft.getSplashManager().getSplash();
    }

    private void confirmDemo(boolean bl) {
        if (bl) {
            try {
                LevelStorageSource.LevelStorageAccess levelStorageAccess = this.minecraft.getLevelSource().createAccess("Demo_World");

                try {
                    levelStorageAccess.deleteLevel();
                } catch (Throwable var6) {
                    if (levelStorageAccess != null) {
                        try {
                            levelStorageAccess.close();
                        } catch (Throwable var5) {
                            var6.addSuppressed(var5);
                        }
                    }

                    throw var6;
                }

                if (levelStorageAccess != null) {
                    levelStorageAccess.close();
                }
            } catch (IOException var7) {
                SystemToast.onWorldDeleteFailure(this.minecraft, "Demo_World");
                LOGGER.warn("Failed to delete demo world", var7);
            }
        }

        this.minecraft.setScreen(this);
    }

    @Environment(EnvType.CLIENT)
    record WarningLabel(Font font, MultiLineLabel label, int x, int y) {
        public void render(GuiGraphics guiGraphics, int alpha) {
//            this.label.renderBackgroundCentered(guiGraphics, this.x, this.y, 9, 2, 0x200000 | Math.min(alpha, 0x55000000));
            this.label.renderCentered(guiGraphics, this.x, this.y, 9, 0xFFFFFF | alpha);
        }
    }
}
