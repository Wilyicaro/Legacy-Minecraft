package wily.legacy.client.screen;

import com.mojang.authlib.minecraft.BanDetails;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.RealmsMainScreen;
import com.mojang.realmsclient.gui.screens.RealmsNotificationsScreen;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.LanguageSelectScreen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.LevelSummary;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import wily.legacy.LegacyMinecraftClient;
import wily.legacy.util.ScreenUtil;

import java.io.IOException;
import java.util.Iterator;
import java.util.Objects;

import static wily.legacy.util.ScreenUtil.renderDefaultBackground;

public class MainMenuScreen extends RenderableVListScreen {
    private static final Logger LOGGER = LogUtils.getLogger();
    @Nullable
    private SplashRenderer splash;
    @Nullable
    private RealmsNotificationsScreen realmsNotificationsScreen;
    private final boolean fading;
    private long fadeInStart;
    @Nullable
    private WarningLabel warningLabel;

    protected final boolean secondButtonActive;

    public MainMenuScreen(boolean bl) {
        super(Component.translatable("narrator.screen.title"), b->{});
        this.fading = bl;
        minecraft = Minecraft.getInstance();
        secondButtonActive = minecraft.isDemo() ? createDemoMenuOptions() : this.createNormalMenuOptions();
        renderableVList.addRenderable(openScreenButton(Component.translatable("options.language"), () -> new LegacyLanguageScreen(this, this.minecraft.getLanguageManager())).build());
        renderableVList.addRenderable(openScreenButton(Component.translatable("menu.options"), () -> new HelpOptionsScreen(this)).build());
        renderableVList.addRenderable(Button.builder(Component.translatable("menu.quit"), (button) -> this.minecraft.stop()).build());
    }

    private boolean realmsNotificationsEnabled() {
        return this.realmsNotificationsScreen != null;
    }

    public void tick() {
        if (this.realmsNotificationsEnabled()) {
            this.realmsNotificationsScreen.tick();
        }

        this.minecraft.getRealms32BitWarningStatus().showRealms32BitWarningIfNeeded(this);
    }

    public boolean isPauseScreen() {
        return false;
    }

    public boolean shouldCloseOnEsc() {
        return false;
    }

    protected void init() {
        if (this.splash == null) {
            this.splash = this.minecraft.getSplashManager().getSplash();
        }
        int l = this.height / 4 + 48;

        super.init();
        if (children().get(1) instanceof  Button b) b.active = secondButtonActive;
        if (this.realmsNotificationsScreen == null) {
            this.realmsNotificationsScreen = new RealmsNotificationsScreen();
        }

        if (this.realmsNotificationsEnabled()) {
            this.realmsNotificationsScreen.init(this.minecraft, this.width, this.height);
        }

        if (!this.minecraft.is64Bit()) {
            this.warningLabel = new WarningLabel(this.font, MultiLineLabel.create(this.font, Component.translatable("title.32bit.deprecation"), 350, 2), this.width / 2, l - 24);
        }
    }

    private boolean createNormalMenuOptions() {
        renderableVList.addRenderable(Button.builder(Component.translatable("legacy.menu.play_game"), (button) -> {
            this.minecraft.setScreen(new PlayGameScreen(this));
        }).build());
        Component component = this.getMultiplayerDisabledReason();
        Tooltip tooltip = component != null ? Tooltip.create(component) : null;
        renderableVList.addRenderable(Button.builder(Component.translatable("menu.online"), (button) -> {
            this.realmsButtonClicked();
        }).tooltip(tooltip).build());
        return component == null;

    }

    @Nullable
    private Component getMultiplayerDisabledReason() {
        if (this.minecraft.allowsMultiplayer()) {
            return null;
        } else if (this.minecraft.isNameBanned()) {
            return Component.translatable("title.multiplayer.disabled.banned.name");
        } else {
            BanDetails banDetails = this.minecraft.multiplayerBan();
            if (banDetails != null) {
                return banDetails.expires() != null ? Component.translatable("title.multiplayer.disabled.banned.temporary") : Component.translatable("title.multiplayer.disabled.banned.permanent");
            } else {
                return Component.translatable("title.multiplayer.disabled");
            }
        }
    }

    private boolean createDemoMenuOptions() {
        boolean bl = this.checkDemoWorldPresence();
        renderableVList.addRenderable(Button.builder(Component.translatable("menu.playdemo"), (button) -> {
            if (bl) {
                this.minecraft.createWorldOpenFlows().checkForBackupAndLoad("Demo_World", ()-> this.minecraft.setScreen(this));
            } else {
                this.minecraft.createWorldOpenFlows().createFreshLevel("Demo_World", MinecraftServer.DEMO_SETTINGS, WorldOptions.DEMO_OPTIONS, WorldPresets::createNormalWorldDimensions, this);
            }

        }).build());
        renderableVList.addRenderable(Button.builder(Component.translatable("menu.resetdemo"), (button) -> {
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
        return bl;
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

    private void realmsButtonClicked() {
        this.minecraft.setScreen(new RealmsMainScreen(this));
    }

    public void render(GuiGraphics guiGraphics, int i, int j, float f) {
        if (this.fadeInStart == 0L && this.fading) {
            this.fadeInStart = Util.getMillis();
        }
        float g = this.fading ? (float)(Util.getMillis() - this.fadeInStart) / 1000.0F : 1.0F;
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        renderDefaultBackground(guiGraphics,i,j,f);
        RenderSystem.enableBlend();

        float h = this.fading ? Mth.clamp(g - 1.0F, 0.0F, 1.0F) : 1.0F;
        int k = Mth.ceil(h * 255.0F) << 24;
        if ((k & -67108864) != 0) {
            if (this.warningLabel != null) {
                this.warningLabel.render(guiGraphics, k);
            }

            if (this.splash != null) {
                LegacyMinecraftClient.FONT_SHADOW_OFFSET = 1.0F;
                this.splash.render(guiGraphics, this.width, this.font, k);
                LegacyMinecraftClient.FONT_SHADOW_OFFSET = 0.5F;
            }

            Iterator var9 = this.children().iterator();

            while(var9.hasNext()) {
                GuiEventListener guiEventListener = (GuiEventListener)var9.next();
                if (guiEventListener instanceof AbstractWidget) {
                    ((AbstractWidget)guiEventListener).setAlpha(h);
                }
            }

            super.render(guiGraphics, i, j, f);
            if (this.realmsNotificationsEnabled() && h >= 1.0F) {
                RenderSystem.enableDepthTest();
                this.realmsNotificationsScreen.render(guiGraphics, i, j, f);
            }

        }
    }

    public void renderBackground(GuiGraphics guiGraphics, int i, int j, float f) {
    }

    public boolean mouseClicked(double d, double e, int i) {
        if (super.mouseClicked(d, e, i)) {
            return true;
        } else {
            return this.realmsNotificationsEnabled() && this.realmsNotificationsScreen.mouseClicked(d, e, i);
        }
    }

    public void removed() {
        if (this.realmsNotificationsScreen != null) {
            this.realmsNotificationsScreen.removed();
        }

    }

    public void added() {
        super.added();
        if (this.realmsNotificationsScreen != null) {
            this.realmsNotificationsScreen.added();
        }

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
        public void render(GuiGraphics guiGraphics, int i) {
            MultiLineLabel var10000 = this.label;
            int var10002 = this.x;
            int var10003 = this.y;
            Objects.requireNonNull(this.font);
            var10000.renderBackgroundCentered(guiGraphics, var10002, var10003, 9, 2, 2097152 | Math.min(i, 1426063360));
            var10000 = this.label;
            var10002 = this.x;
            var10003 = this.y;
            Objects.requireNonNull(this.font);
            var10000.renderCentered(guiGraphics, var10002, var10003, 9, 16777215 | i);
        }
    }
}
