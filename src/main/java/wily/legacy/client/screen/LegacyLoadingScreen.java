package wily.legacy.client.screen;

import net.minecraft.Util;
import net.minecraft.client.GameNarrator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import wily.factoryapi.base.client.UIAccessor;
import wily.legacy.client.LegacyOptions;
import wily.legacy.network.TopMessage;
import wily.legacy.util.LegacyComponents;
import wily.legacy.util.ScreenUtil;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

public class LegacyLoadingScreen extends Screen implements LegacyLoading, ControlTooltip.Event {
    private final LegacyLoadingRenderer renderer = new LegacyLoadingRenderer();
    private boolean blackBackground;

    protected RandomSource random = RandomSource.create();

    public LegacyLoadingScreen() {
        super(GameNarrator.NO_TITLE);
    }
    public LegacyLoadingScreen(Component loadingHeader, Component loadingStage) {
        this();
        this.setLoadingHeader(loadingHeader);
        this.setLoadingStage(loadingStage);
    }

    public void setBlackBackground(boolean blackBackground) {
        this.blackBackground = blackBackground;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    //? if >1.20.1 {
    @Override
    public void renderBackground(GuiGraphics guiGraphics, int i, int j, float f) {
        renderer.prepareRender(minecraft, UIAccessor.of(this));
        if (blackBackground) guiGraphics.fill(0, 0, guiGraphics.guiWidth(), guiGraphics.guiHeight(), 0xFF000000);
        else ScreenUtil.renderDefaultBackground(UIAccessor.of(this), guiGraphics, true, true, false);
    }
    //?}
    @Override
    public void render(GuiGraphics guiGraphics, int i, int j, float f) {
        //? if <=1.20.1 {
        /*renderer.prepareRender(minecraft, UIAccessor.of(this));
        if (blackBackground) guiGraphics.fill(0, 0, guiGraphics.guiWidth(), guiGraphics.guiHeight(), 0xFF000000);
        else ScreenUtil.renderDefaultBackground(UIAccessor.of(this), guiGraphics, true, true, false);
        *///?}
       super.render(guiGraphics, i, j, f);
       renderer.renderForeground(guiGraphics, i, j, f);
    }

    public static LegacyLoadingScreen getDimensionChangeScreen(ClientLevel lastLevel, ClientLevel newLevel){
        boolean lastOd = isOtherDimension(lastLevel);
        boolean od = isOtherDimension(newLevel);
        LegacyLoadingScreen screen = new LegacyLoadingScreen(od || lastOd ? Component.translatable("legacy.menu." + (lastOd ? "leaving" : "entering"), LegacyComponents.getDimensionName((lastOd ? lastLevel : newLevel).dimension())) : Component.empty(), Component.empty());
        if (od || lastOd) screen.setGenericLoading(true);
        return screen;
    }

    public static boolean isOtherDimension(Level level){
        return level != null && level.dimension() != Level.OVERWORLD;
    }

    public static LegacyLoadingScreen getRespawningScreen(BooleanSupplier levelReady){
        long createdTime = Util.getMillis();
        LegacyLoadingScreen screen = new LegacyLoadingScreen(LegacyComponents.RESPAWNING, Component.empty()){
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

    public static void openFakeManualSaveScreen(Screen nextScreen) {
        if (!LegacyOptions.fakeManualSaveScreen.get()) {
            Minecraft.getInstance().setScreen(nextScreen);
            return;
        }
        Minecraft.getInstance().gui.autosaveIndicatorValue = 0.0f;
        Minecraft.getInstance().setScreen(getFakeManualSaveScreen(nextScreen));
    }

    public static LegacyLoadingScreen getFakeAutoSaveScreen() {
        return createFakeSaveScreen(LegacyComponents.PREPARING_AUTOSAVE, true, () -> Minecraft.getInstance().setScreen(null));
    }

    public static LegacyLoadingScreen getFakeManualSaveScreen(Screen nextScreen) {
        return createFakeSaveScreen(LegacyComponents.PREPARING_MANUAL_SAVE, true, () -> Minecraft.getInstance().setScreen(nextScreen));
    }

    private static LegacyLoadingScreen createFakeSaveScreen(Component loadingHeader, boolean controlsAutosaveIndicator, Runnable onClose) {
        return new LegacyLoadingScreen(loadingHeader, LegacyComponents.PREPARING_CHUNKS) {
            int finalizingTicks = -1;

            @Override
            public void onClose() {
                onClose.run();
            }

            @Override
            public void tick() {
                if (controlsAutosaveIndicator) minecraft.gui.autosaveIndicatorValue = 0.0f;
                super.tick();

                if (finalizingTicks < 0) {
                    setProgress(getProgress() + 2);

                    if (getProgress() >= 100) {
                        finalizingTicks = 80;
                        setProgress(0);
                        setLoadingStage(LegacyComponents.FINALIZING);
                    }
                } else if (finalizingTicks > 0) {
                    finalizingTicks--;
                } else {
                    onClose();
                    ScreenUtil.playBackSound();
                    if (controlsAutosaveIndicator) minecraft.gui.autosaveIndicatorValue = 1.0f;
                }
            }
        };
    }

    public static LegacyLoadingScreen createWithExecutor(Component header, Runnable onClose, ExecutorService executor){
        return new LegacyLoadingScreen(header,Component.empty()){
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

    public static void closeExecutor(ExecutorService executor){
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
    public LegacyLoadingRenderer getLoadingRenderer() {
        return renderer;
    }
}
