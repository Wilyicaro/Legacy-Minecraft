package wily.legacy.client.screen;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.ResourceLocation;
import wily.legacy.client.LegacyGuiGraphics;
import wily.legacy.util.LegacySprites;
import wily.legacy.util.ScreenUtil;

import java.util.function.Function;

public class Panel extends SimpleLayoutRenderable {
    public final Function<Panel,Integer> updatedX;
    public final Function<Panel,Integer> updatedY;
    public ResourceLocation panelSprite = LegacySprites.SMALL_PANEL;
    public Panel(Function<Panel,Integer> leftPosGetter, Function<Panel,Integer> topPosGetter){
        this(leftPosGetter, topPosGetter,200,160);
    }
    public Panel(Function<Panel,Integer> leftPosGetter, Function<Panel,Integer> topPosGetter, int width, int height) {
        this.width = width;
        this.height = height;
        this.updatedX = leftPosGetter;
        this.updatedY = topPosGetter;
    }
    public int centeredLeftPos(Screen screen){
        return (screen.width - width) / 2;
    }
    public int centeredTopPos(Screen screen){
        return (screen.height - height) / 2;
    }
    public static Panel centered(Screen screen, int imageWidth, int imageHeight){
        return centered(screen,imageWidth,imageHeight,0,0);
    }
    public static Panel centered(Screen screen, int imageWidth, int imageHeight, int xOffset, int yOffset){
        return new Panel(g-> g.centeredLeftPos(screen) + xOffset, g-> g.centeredTopPos(screen) + yOffset, imageWidth, imageHeight);
    }
    public static Panel tooltipBoxOf(Panel panel, int width){
        Panel p = new Panel(pn->panel.x + panel.width - 2,pn-> panel.y + 5,width,panel.height - 10){
            @Override
            public void init() {
                panel.x-=(width - 2)/ 2;
                height = panel.height - 10;
                super.init();
            }

            @Override
            public void render(PoseStack poseStack, int i, int j, float f) {
                ScreenUtil.renderPointerPanel(poseStack,getX(),getY(),getWidth(),getHeight());
            }
        };
        return p;
    }
    public void init(){
        setX(updatedX.apply(this));
        setY(updatedY.apply(this));
    }
    public void render(PoseStack poseStack, int i, int j, float f) {
        LegacyGuiGraphics.of(poseStack).blitSprite(panelSprite, x, y, width, height);
    }
}
