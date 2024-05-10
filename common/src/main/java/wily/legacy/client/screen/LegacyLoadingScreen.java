package wily.legacy.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.GameNarrator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import wily.legacy.client.LegacyTip;
import wily.legacy.client.LegacyTipManager;
import wily.legacy.util.ScreenUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import static wily.legacy.util.LegacySprites.LOADING_BACKGROUND;
import static wily.legacy.util.LegacySprites.LOADING_BAR;

public class LegacyLoadingScreen extends LegacyScreen {
    public static final List<Supplier<LegacyTip>> usingLoadingTips = new ArrayList<>(LegacyTipManager.loadingTips);
    public static LegacyTip actualLoadingTip;
    protected int progress;
    protected Component lastLoadingHeader;
    protected Component lastLoadingStage;
    public boolean genericLoading;

    protected RandomSource random = RandomSource.create();
    public LegacyLoadingScreen() {
        super(GameNarrator.NO_TITLE);
        controlTooltipRenderer.tooltips.clear();
    }
    public LegacyLoadingScreen(Component loadingHeader, Component loadingStage) {
        this();
        this.lastLoadingHeader = loadingHeader;
        this.lastLoadingStage = loadingStage;
    }

    public void prepareRender(Minecraft minecraft,int width, int height,Component loadingHeader, Component loadingStage, int progress, boolean genericLoading){
        resize(minecraft,width,height);
        this.minecraft = minecraft;
        this.lastLoadingHeader = loadingHeader;
        this.lastLoadingStage = loadingStage;
        this.progress = progress;
        this.genericLoading = genericLoading;
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
    public void renderDefaultBackground(GuiGraphics guiGraphics, int i, int j, float f) {
        ScreenUtil.renderDefaultBackground(guiGraphics,true, true);
    }
    @Override
    public void render(GuiGraphics guiGraphics, int i, int j, float f) {
        RenderSystem.disableDepthTest();
        super.render(guiGraphics, i, j, f);
        int x = width / 2 - 160;
        int y = height / 2 + 16;
        if (!genericLoading) {
            if (progress != -1) {
                if (lastLoadingStage != null)
                    guiGraphics.drawString(minecraft.font, lastLoadingStage, x, height / 2 + 4, 16777215);
                guiGraphics.blitSprite(LOADING_BACKGROUND, x, y, 320, 10);
                if (progress >= 0)
                    guiGraphics.blitSprite(LOADING_BAR, x + 1, y + 1, (int) (318 * (progress / 100F)), 8);
                LegacyTip tip = getLoadingTip();
                if (tip != null) {
                    tip.setX((width - tip.width) / 2);
                    tip.setY(y + 8 + ((height - (y + 8)) - tip.height) / 2);
                    tip.render(guiGraphics, i, j, f);
                }
            }
        }else ScreenUtil.drawGenericLoading(guiGraphics,(width - 75 )/ 2, height / 2);

        guiGraphics.pose().scale(2.0F,2.0F,1.0F);
        if (lastLoadingHeader != null)
            ScreenUtil.drawOutlinedString(guiGraphics, minecraft.font, lastLoadingHeader, (width - minecraft.font.width(lastLoadingHeader) * 2) / 4, (height / 4 - 13), 0xFFFFFF, 0, 1);
        guiGraphics.pose().scale(0.5F,0.5F,1.0F);
        RenderSystem.enableDepthTest();
    }
}
