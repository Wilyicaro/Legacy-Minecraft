package wily.legacy.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.GameNarrator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import wily.legacy.client.LegacyGuiGraphics;
import wily.legacy.client.LegacyTip;
import wily.legacy.client.LegacyTipManager;
import wily.legacy.util.ScreenUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import static wily.legacy.util.LegacySprites.LOADING_BACKGROUND;
import static wily.legacy.util.LegacySprites.LOADING_BAR;

public class LegacyLoadingScreen extends Screen{
    public static final List<Supplier<LegacyTip>> usingLoadingTips = new ArrayList<>(LegacyTipManager.loadingTips);
    public static LegacyTip actualLoadingTip;
    public int progress;
    public Component lastLoadingHeader;
    protected Component lastLoadingStage;
    public boolean genericLoading;

    protected RandomSource random = RandomSource.create();
    public LegacyLoadingScreen() {
        super(GameNarrator.NO_TITLE);
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
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public void render(PoseStack poseStack, int i, int j, float f) {
        RenderSystem.disableDepthTest();
        ScreenUtil.renderDefaultBackground(poseStack,true, true);
        super.render(poseStack, i, j, f);
        int x = width / 2 - 160;
        int y = height / 2 + 16;
        if (!genericLoading) {
            if (progress != -1) {
                if (lastLoadingStage != null)
                    poseStack.drawString(minecraft.font, lastLoadingStage, x, height / 2 + 4, 16777215);
                LegacyGuiGraphics.of(poseStack).blitSprite(LOADING_BACKGROUND, x, y, 320, 10);
                if (progress >= 0)
                    LegacyGuiGraphics.of(poseStack).blitSprite(LOADING_BAR, x + 1, y + 1, (int) (318 * Math.max(0,Math.min(progress / 100F,1))), 8);
                LegacyTip tip = getLoadingTip();
                if (tip != null) {
                    tip.setX((width - tip.width) / 2);
                    tip.setY(y + 8 + ((height - (y + 8)) - tip.height) / 2);
                    tip.render(poseStack, i, j, f);
                }
            }
        }else ScreenUtil.drawGenericLoading(poseStack,(width - 75 )/ 2, height / 2);

        poseStack.pose().scale(2.0F,2.0F,1.0F);
        if (lastLoadingHeader != null)
            ScreenUtil.drawOutlinedString(poseStack, minecraft.font, lastLoadingHeader, (width - minecraft.font.width(lastLoadingHeader) * 2) / 4, (height / 4 - 13), 0xFFFFFF, 0, 0.5f);
        poseStack.pose().scale(0.5F,0.5F,1.0F);
        RenderSystem.enableDepthTest();
    }
}
