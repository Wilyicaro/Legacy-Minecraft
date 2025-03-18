package wily.legacy.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.Util;
import net.minecraft.client.GameNarrator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.screens.ReceivingLevelScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import wily.factoryapi.base.client.FactoryGuiGraphics;
import wily.factoryapi.base.client.UIAccessor;
import wily.legacy.Legacy4JClient;
import wily.legacy.client.CommonColor;
import wily.legacy.client.LegacyTip;
import wily.legacy.client.LegacyTipManager;
import wily.legacy.util.LegacyComponents;
import wily.legacy.util.ScreenUtil;

import java.util.function.BooleanSupplier;

import static wily.legacy.util.LegacySprites.LOADING_BACKGROUND;
import static wily.legacy.util.LegacySprites.LOADING_BAR;

public class LegacyLoadingScreen extends Screen implements LegacyLoading {
    public static LegacyTip actualLoadingTip;
    private int progress;
    private Component loadingHeader;
    private Component loadingStage;
    private boolean genericLoading;
    private UIAccessor accessor = UIAccessor.of(this);

    protected RandomSource random = RandomSource.create();

    public LegacyLoadingScreen() {
        super(GameNarrator.NO_TITLE);
    }
    public LegacyLoadingScreen(Component loadingHeader, Component loadingStage) {
        this();
        this.setLoadingHeader(loadingHeader);
        this.setLoadingStage(loadingStage);
    }

    public void prepareRender(Minecraft minecraft,int width, int height,Component loadingHeader, Component loadingStage, int progress, boolean genericLoading){
        resize(minecraft,width,height);
        this.minecraft = minecraft;
        this.accessor = UIAccessor.of(minecraft.screen);
        this.setLoadingHeader(accessor.getElementValue("loadingHeader.component",loadingHeader,Component.class));
        this.setLoadingStage(accessor.getElementValue("loadingStage.component",loadingStage,Component.class));
        this.setProgress(accessor.getInteger("progress",progress));
        this.setGenericLoading(accessor.getBoolean("genericLoading",genericLoading));
    }

    public LegacyTip getLoadingTip(){
        if (usingLoadingTips.isEmpty()){
            if (LegacyTipManager.loadingTips.isEmpty()) return null;
            else usingLoadingTips.addAll(LegacyTipManager.loadingTips);
        }
        if (actualLoadingTip == null) {
            int i = random.nextInt(usingLoadingTips.size());
            actualLoadingTip = usingLoadingTips.get(i).get();
            usingLoadingTips.remove(i);
        }else if (actualLoadingTip.visibility == Toast.Visibility.HIDE) {
            actualLoadingTip = null;
            return getLoadingTip();
        }
        return actualLoadingTip;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    //? if >1.20.1 {
    @Override
    public void renderBackground(GuiGraphics guiGraphics, int i, int j, float f) {
        ScreenUtil.renderDefaultBackground(accessor, guiGraphics, true, true, false);
    }
    //?}
    @Override
    public void render(GuiGraphics guiGraphics, int i, int j, float f) {
        RenderSystem.disableDepthTest();
        //? if <=1.20.1
        /*ScreenUtil.renderDefaultBackground(accessor, guiGraphics, true, true, false);*/
        super.render(guiGraphics, i, j, f);
        int x = width / 2 - 160;
        int y = height / 2 + 16;
        ResourceLocation fontOverride = accessor.getElementValue("fontOverride",null,ResourceLocation.class);
        if (!isGenericLoading()) {
            if (getProgress() != -1) {
                if (getLoadingStage() != null)
                    Legacy4JClient.applyFontOverrideIf(fontOverride != null,fontOverride,b-> guiGraphics.drawString(minecraft.font, getLoadingStage(), accessor.getInteger("loadingStage.x",x + 1), accessor.getInteger("loadingStage.y",height / 2 + 5), CommonColor.STAGE_TEXT.get()));
                try (SpriteContents contents = FactoryGuiGraphics.getSprites().getSprite(LOADING_BACKGROUND).contents()){
                    FactoryGuiGraphics.of(guiGraphics).blitSprite(LOADING_BACKGROUND, x, y, 320, 320 * contents.height() / contents.width());
                }
                if (getProgress() >= 0) {
                    try (SpriteContents contents = FactoryGuiGraphics.getSprites().getSprite(LOADING_BAR).contents()) {
                        FactoryGuiGraphics.of(guiGraphics).blitSprite(LOADING_BAR, 318, 318 * contents.height() / contents.width(), 0, 0, x + 1, y + 1,0, (int) (318 * Math.max(0, Math.min(getProgress() / 100F, 1))), 318 * contents.height() / contents.width());
                    }
                }
                LegacyTip tip = getLoadingTip();
                if (tip != null) {
                    tip.setX((width - tip.width) / 2);
                    tip.setY(y + 8 + ((height - (y + 8)) - tip.height) / 2);
                    tip.render(guiGraphics, i, j, f);
                }
            }
        }else ScreenUtil.drawGenericLoading(guiGraphics,(width - 75 )/ 2, height / 2);
        if (getLoadingHeader() != null) {
            Legacy4JClient.applyFontOverrideIf(fontOverride != null, fontOverride, b -> {
                guiGraphics.pose().pushPose();
                float scaleX = accessor.getFloat("loadingHeader.scaleX", 2.0f);
                guiGraphics.pose().translate(accessor.getFloat("loadingHeader.x", (width - minecraft.font.width(getLoadingHeader()) * scaleX) / 2), accessor.getFloat("loadingHeader.y", height / 2 - 23), 0);
                guiGraphics.pose().scale(scaleX, accessor.getFloat("loadingHeader.scaleY", 2.0f), 1.0f);
                ScreenUtil.drawOutlinedString(guiGraphics, minecraft.font, getLoadingHeader(), 0, 0, CommonColor.TITLE_TEXT.get(), CommonColor.TITLE_TEXT_OUTLINE.get(), accessor.getFloat("loadingHeader.outline", 0.5f));
                guiGraphics.pose().popPose();
            });
        }
        RenderSystem.enableDepthTest();
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

    public int getProgress() {
        return progress;
    }

    public void setProgress(int progress) {
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
