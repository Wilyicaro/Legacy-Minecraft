package wily.legacy.client.screen;

import net.minecraft.Util;
import net.minecraft.client.GameNarrator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import wily.factoryapi.base.client.UIAccessor;
import wily.legacy.client.LegacyOptions;
import wily.legacy.network.TopMessage;
import wily.legacy.util.LegacyComponents;
import wily.legacy.util.client.LegacySoundUtil;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

public class LegacyLoadingScreen extends Screen implements LegacyLoading, ControlTooltip.Event {
    private final LegacyLoadingRenderer renderer = new LegacyLoadingRenderer();

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

    public static void startFakeAutoSave() {
        if (!LegacyOptions.fakeAutosaveScreen.get()) return;
        Minecraft.getInstance().gui.autosaveIndicatorValue = 0.0f;
        TopMessage.setMedium(null);
        Minecraft.getInstance().setScreen(getFakeAutoSaveScreen());
    }

    public static LegacyLoadingScreen getFakeAutoSaveScreen() {
        return new LegacyLoadingScreen(LegacyComponents.PREPARING_AUTOSAVE, LegacyComponents.PREPARING_CHUNKS) {
            int finalizingTicks = -1;

            @Override
            public void tick() {
                minecraft.gui.autosaveIndicatorValue = 0.0f;
                super.tick();

                if (finalizingTicks < 0) {
                    setProgress(getProgress() + 0.02f);

                    if (getProgress() >= 1.0) {
                        finalizingTicks = 80;
                        setProgress(0);
                        setLoadingStage(LegacyComponents.FINALIZING);
                    }
                } else if (finalizingTicks > 0) {
                    finalizingTicks--;
                } else {
                    onClose();
                    LegacySoundUtil.playBackSound();
                    minecraft.gui.autosaveIndicatorValue = 1.0f;
                }
            }
        };
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

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    //? if >1.20.1 {
    @Override
    public void renderBackground(GuiGraphics guiGraphics, int i, int j, float f) {
        renderer.prepareRender(minecraft, UIAccessor.of(this));
        renderer.renderBackground(guiGraphics, i, j, f);
    }
    //?}

    @Override
    public void render(GuiGraphics guiGraphics, int i, int j, float f) {
        super.render(guiGraphics, i, j, f);
        renderer.renderForeground(guiGraphics, i, j, f);
    }

    @Override
    public LegacyLoadingRenderer getLoadingRenderer() {
        return renderer;
    }
}
