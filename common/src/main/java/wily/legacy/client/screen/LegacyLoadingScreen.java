package wily.legacy.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.GameNarrator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import wily.legacy.LegacyMinecraft;
import wily.legacy.util.ScreenUtil;

public class LegacyLoadingScreen extends DefaultScreen{
    public static ResourceLocation LOADING_BACKGROUND_SPRITE = new ResourceLocation(LegacyMinecraft.MOD_ID,"widget/loading_background");
    public static ResourceLocation LOADING_BAR_SPRITE = new ResourceLocation(LegacyMinecraft.MOD_ID,"widget/loading_bar");
    protected int progress;
    protected Component lastLoadingHeader;
    protected Component lastLoadingStage;
    public LegacyLoadingScreen() {
        super(GameNarrator.NO_TITLE);
    }
    public LegacyLoadingScreen(Component loadingHeader, Component loadingStage) {
        this();
        this.lastLoadingHeader = loadingHeader;
        this.lastLoadingStage = loadingStage;
    }

    public void prepareRender(Minecraft minecraft,int width, int height,Component loadingHeader, Component loadingStage, int progress){
        resize(minecraft,width,height);
        this.minecraft = minecraft;
        this.lastLoadingHeader = loadingHeader;
        this.lastLoadingStage = loadingStage;
        this.progress = progress;
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
        if (lastLoadingStage != null)
            guiGraphics.drawString(minecraft.font,lastLoadingStage, x, height / 2 + 4, 16777215);
        guiGraphics.pose().scale(2.0F,2.0F,1.0F);
        if (lastLoadingHeader != null)
            ScreenUtil.drawOutlinedString(guiGraphics,  minecraft.font,lastLoadingHeader, (width - minecraft.font.width(lastLoadingHeader) * 2) / 4, (height / 4 - 15), 0xFFFFFF, 0,1);
        guiGraphics.pose().scale(0.5F,0.5F,1.0F);
        guiGraphics.blitSprite(LOADING_BACKGROUND_SPRITE,x,y,320,10);
        if(progress >= 0)
            guiGraphics.blitSprite(LOADING_BAR_SPRITE,x + 1,y + 1,(int) (318 * (progress / 100F)),8);
        RenderSystem.enableDepthTest();
    }
}
