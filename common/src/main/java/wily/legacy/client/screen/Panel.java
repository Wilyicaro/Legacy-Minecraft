package wily.legacy.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import wily.legacy.util.ScreenUtil;

import java.util.function.Function;

public class Panel extends SimpleLayoutRenderable {
    protected final Function<Panel,Integer> updatedX;
    protected final Function<Panel,Integer> updatedY;
    public float dp = 2F;
    public Panel(Function<Panel,Integer> leftPosGetter, Function<Panel,Integer> topPosGetter){
        this(leftPosGetter, topPosGetter,200,160);
    }
    public Panel(Function<Panel,Integer> leftPosGetter, Function<Panel,Integer> topPosGetter, int width, int height) {
        this.width = width;
        this.height = height;
        this.updatedX = leftPosGetter;
        this.updatedY = topPosGetter;
    }
    public static Panel centered(Screen screen, int imageWidth, int imageHeight){
        return centered(screen,imageWidth,imageHeight,0,0);
    }
    public static Panel centered(Screen screen, int imageWidth, int imageHeight, int xOffset, int yOffset){
        return new Panel(g-> (screen.width - g.width) / 2 + xOffset, g-> (screen.height - g.height) / 2 + yOffset, imageWidth, imageHeight);
    }
    public void init(){
        setX(updatedX.apply(this));
        setY(updatedY.apply(this));
    }
    public void render(GuiGraphics guiGraphics, int i, int j, float f) {
        ScreenUtil.renderPanel(guiGraphics, x, y, width, height,dp);
    }
}
