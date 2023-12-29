package wily.legacy.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.screens.Screen;
import wily.legacy.util.ScreenUtil;

import java.util.function.Consumer;
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
        return new Panel(g-> (screen.width - g.width) / 2, g-> (screen.height - g.height) / 2, imageWidth, imageHeight);
    }
    public void init(){
        setX(updatedX.apply(this));
        setY(updatedY.apply(this));
    }
    public void render(GuiGraphics guiGraphics, int i, int j, float f) {
        ScreenUtil.renderPanel(guiGraphics, x, y, width, height,dp);
    }
}
