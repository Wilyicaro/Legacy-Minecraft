package wily.legacy.client.screen;

import net.minecraft.Util;
import net.minecraft.client.GameNarrator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import wily.factoryapi.base.ArbitrarySupplier;
import wily.factoryapi.base.client.FactoryGuiGraphics;
import wily.factoryapi.base.client.UIAccessor;
import wily.factoryapi.util.FactoryScreenUtil;
import wily.legacy.Legacy4JClient;
import wily.legacy.client.CommonColor;
import wily.legacy.client.LegacyTip;
import wily.legacy.client.LegacyTipManager;
import wily.legacy.util.LegacyComponents;
import wily.legacy.util.client.LegacyFontUtil;
import wily.legacy.util.client.LegacyRenderUtil;

import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

import static wily.legacy.util.LegacySprites.LOADING_BACKGROUND;
import static wily.legacy.util.LegacySprites.LOADING_BAR;

public class LegacyLoadingScreen extends Screen implements LegacyLoading {
    private float progress;
    private Component loadingHeader;
    private Component loadingStage;
    private boolean genericLoading;
    private UIAccessor accessor = UIAccessor.of(this);

    public LegacyLoadingScreen() {
        super(GameNarrator.NO_TITLE);
    }

    public LegacyLoadingScreen(Component loadingHeader, Component loadingStage) {
        this();
        this.setLoadingHeader(loadingHeader);
        this.setLoadingStage(loadingStage);
    }

    public static LegacyLoadingScreen getDimensionChangeScreen(BooleanSupplier levelReady, ResourceKey<Level> lastLevel, ResourceKey<Level> newLevel) {
        long createdTime = Util.getMillis();
        boolean lastOd = isOtherDimension(lastLevel);
        boolean od = isOtherDimension(newLevel);
        LegacyLoadingScreen screen = new LegacyLoadingScreen(od || lastOd ? Component.translatable("legacy.menu." + (lastOd ? "leaving" : "entering"), LegacyComponents.getDimensionName((lastOd ? lastLevel : newLevel))) : Component.empty(), Component.empty()) {
            @Override
            public void tick() {
                if (levelReady.getAsBoolean() || Util.getMillis() - createdTime >= 30000) minecraft.setScreen(null);
            }

            @Override
            public boolean isPauseScreen() {
                return false;
            }
        };
        if (od || lastOd) {
            screen.setGenericLoading(true);

        }
        return screen;
    }

    public static boolean isOtherDimension(ResourceKey<Level> level) {
        return level != null && level != Level.OVERWORLD;
    }

    public static LegacyLoadingScreen getRespawningScreen(BooleanSupplier levelReady) {
        long createdTime = Util.getMillis();
        LegacyLoadingScreen screen = new LegacyLoadingScreen(LegacyComponents.RESPAWNING, Component.empty()) {
            @Override
            public void tick() {
                if (levelReady.getAsBoolean() || Util.getMillis() - createdTime >= 30000) minecraft.setScreen(null);
            }

            @Override
            public boolean isPauseScreen() {
                return false;
            }
        };
        screen.setGenericLoading(true);
        return screen;
    }

    public static LegacyLoadingScreen createWithExecutor(Component header, Runnable onClose, ExecutorService executor) {
        return new LegacyLoadingScreen(header, Component.empty()) {
            @Override
            public void onClose() {
                onClose.run();
                closeExecutor(executor);
            }

            @Override
            public boolean shouldCloseOnEsc() {
                return true;
            }
        };
    }

    public static void closeExecutor(ExecutorService executor) {
        executor.shutdown();
        boolean bl;
        try {
            bl = executor.awaitTermination(3L, TimeUnit.SECONDS);
        } catch (InterruptedException var3) {
            bl = false;
        }

        if (!bl) {
            executor.shutdownNow();
        }
    }

    public void prepareRender(Minecraft minecraft, int width, int height, Component loadingHeader, Component loadingStage, float progress, boolean genericLoading) {
        resize(minecraft, width, height);
        this.minecraft = minecraft;
        this.accessor = UIAccessor.of(minecraft.screen);
        this.setLoadingHeader(accessor.getElementValue("loadingHeader.component", loadingHeader, Component.class));
        this.setLoadingStage(accessor.getElementValue("loadingStage.component", loadingStage, Component.class));
        this.setProgress(accessor.getFloat("progress", progress));
        this.setGenericLoading(accessor.getBoolean("genericLoading", genericLoading));
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    //? if >1.20.1 {
    @Override
    public void renderBackground(GuiGraphics guiGraphics, int i, int j, float f) {
        LegacyRenderUtil.renderDefaultBackground(accessor, guiGraphics, true, true, false);
    }

    //?}
    @Override
    public void render(GuiGraphics guiGraphics, int i, int j, float f) {
        FactoryScreenUtil.disableDepthTest();
        LegacyRenderUtil.renderDefaultBackground(accessor, guiGraphics, true, true, false);
        super.render(guiGraphics, i, j, f);
        ArbitrarySupplier<ResourceLocation> fontOverride = accessor.getElement("fontOverride", ResourceLocation.class);
        if (!isGenericLoading()) {
            if (getProgress() != -1) {
                int loadingBarX = accessor.getInteger("loadingBar.x", width / 2 - 160);
                int loadingBarY = accessor.getInteger("loadingBar.y", height / 2 + 15);
                if (getLoadingStage() != null)
                    LegacyFontUtil.applySmallerFont(fontOverride.map(FontDescription.Resource::new).orElse(FontDescription.DEFAULT), b -> guiGraphics.drawString(minecraft.font, getLoadingStage(), accessor.getInteger("loadingStage.x", loadingBarX + 1), accessor.getInteger("loadingStage.y", height / 2 + 5), CommonColor.STAGE_TEXT.get()));
                try (SpriteContents contents = FactoryGuiGraphics.getSprites().getSprite(LOADING_BACKGROUND).contents()) {
                    FactoryGuiGraphics.of(guiGraphics).blitSprite(LOADING_BACKGROUND, loadingBarX, loadingBarY, 320, 320 * contents.height() / contents.width());
                }
                if (getProgress() >= 0) {
                    try (SpriteContents contents = FactoryGuiGraphics.getSprites().getSprite(LOADING_BAR).contents()) {
                        FactoryGuiGraphics.of(guiGraphics).blitSprite(LOADING_BAR, 318, 318 * contents.height() / contents.width(), 0, 0, loadingBarX + 1, loadingBarY + 1, 0, (int) (318 * Math.max(0, Math.min(getProgress(), 1))), 318 * contents.height() / contents.width());
                    }
                }
                LegacyTip tip = Legacy4JClient.legacyTipManager.getLoadingTip();
                if (tip != null) {
                    tip.setX(accessor.getInteger("loadingTip.x", (width - tip.width) / 2));
                    tip.setY(accessor.getInteger("loadingTip.y", loadingBarY + 10 + ((height - (loadingBarY + 10)) - tip.height) / 2));
                    tip.render(guiGraphics, i, j, f);
                }
            }
        } else LegacyRenderUtil.drawGenericLoading(guiGraphics, (width - 75) / 2, height / 2);
        if (getLoadingHeader() != null) {
            LegacyFontUtil.applySmallerFont(fontOverride.map(FontDescription.Resource::new).orElse(FontDescription.DEFAULT), b -> {
                guiGraphics.pose().pushMatrix();
                float scaleX = accessor.getFloat("loadingHeader.scaleX", 2.0f);
                guiGraphics.pose().translate(accessor.getFloat("loadingHeader.x", (width - minecraft.font.width(getLoadingHeader()) * scaleX) / 2), accessor.getFloat("loadingHeader.y", height / 2 - 23));
                guiGraphics.pose().scale(scaleX, accessor.getFloat("loadingHeader.scaleY", 2.0f));
                LegacyRenderUtil.drawOutlinedString(guiGraphics, minecraft.font, getLoadingHeader(), 0, 0, CommonColor.TITLE_TEXT.get(), CommonColor.TITLE_TEXT_OUTLINE.get(), accessor.getFloat("loadingHeader.outline", 0.5f));
                guiGraphics.pose().popMatrix();
            });
        }
        FactoryScreenUtil.enableDepthTest();
    }

    public float getProgress() {
        return progress;
    }

    public void setProgress(float progress) {
        this.progress = progress;
    }

    public Component getLoadingHeader() {
        return loadingHeader;
    }

    public void setLoadingHeader(Component loadingHeader) {
        this.loadingHeader = loadingHeader;
    }

    public Component getLoadingStage() {
        return loadingStage;
    }

    public void setLoadingStage(Component loadingStage) {
        this.loadingStage = loadingStage;
    }

    public boolean isGenericLoading() {
        return genericLoading;
    }

    public void setGenericLoading(boolean genericLoading) {
        this.genericLoading = genericLoading;
    }
}
