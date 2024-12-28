package wily.legacy.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.ResourceLocation;
import wily.factoryapi.base.client.FactoryGuiGraphics;
import wily.factoryapi.base.client.UIDefinition;
import wily.legacy.util.LegacySprites;
import wily.legacy.util.ScreenUtil;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class Panel extends SimpleLayoutRenderable {
    protected final UIDefinition.Accessor accessor;
    public ResourceLocation panelSprite = LegacySprites.SMALL_PANEL;
    String name;

    public Panel(Screen screen){
        this(UIDefinition.Accessor.of(screen));
    }
    public Panel(UIDefinition.Accessor accessor){
        this.accessor = accessor;
    }
    public static Panel createPanel(Screen screen, Function<Panel,Integer> leftPosGetter, Function<Panel,Integer> topPosGetter, int width, int height){
        return createPanel(screen, leftPosGetter, topPosGetter, ()-> width, ()-> height);
    }
    public static Panel createPanel(Screen screen, Function<Panel,Integer> leftPosGetter, Function<Panel,Integer> topPosGetter, Supplier<Integer> widthGetter, Supplier<Integer> heightGetter){
        return createPanel(screen, p->p.appearance(widthGetter.get(), heightGetter.get()), p->p.pos(leftPosGetter.apply(p),topPosGetter.apply(p)));
    }
    public static Panel createPanel(Screen screen, Consumer<Panel> initAppearance){
        return createPanel(screen, initAppearance, p->p.centered(screen));
    }
    public static Panel createPanel(Screen screen, Consumer<Panel> initAppearance, Consumer<Panel> initPos){
        return new Panel(UIDefinition.Accessor.of(screen)){
            @Override
            public void init(String name) {
                super.init(name);
                initAppearance.accept(this);
                initPos.accept(this);
            }
        };
    }

    public int centeredLeftPos(Screen screen){
        return (screen.width - width) / 2;
    }

    public int centeredTopPos(Screen screen){
        return (screen.height - height) / 2;
    }

    public static Panel centered(Screen screen, Supplier<Integer> width, Supplier<Integer> height){
        return centered(screen, width, height,0,0);
    }

    public static Panel centered(Screen screen, int width, int height){
        return centered(screen, width, height,0,0);
    }

    public static Panel centered(Screen screen, int width, int height, int xOffset, int yOffset){
        return centered(screen, ()->width, ()->height, xOffset, yOffset);
    }

    public static Panel centered(Screen screen, int width, int height, Supplier<Integer> xOffset, Supplier<Integer> yOffset){
        return centered(screen, ()->width, ()->height, xOffset, yOffset);
    }

    public static Panel centered(Screen screen, Supplier<Integer> widthGetter, Supplier<Integer> heightGetter, int xOffset, int yOffset){
        return centered(screen, widthGetter, heightGetter,()->xOffset,()->yOffset);
    }

    public static Panel centered(Screen screen, Supplier<Integer> imageWidth, Supplier<Integer> imageHeight, Supplier<Integer> xOffset, Supplier<Integer> yOffset){
        return Panel.createPanel(screen, p-> p.centeredLeftPos(screen) + xOffset.get(), p-> p.centeredTopPos(screen) + yOffset.get(), imageWidth, imageHeight);
    }

    public static Panel centered(Screen screen, ResourceLocation panelSprite, int imageWidth, int imageHeight, int xOffset, int yOffset){
        return Panel.createPanel(screen, p-> p.appearance(panelSprite,imageWidth,imageHeight), p-> p.pos(p.centeredLeftPos(screen) + xOffset,p.centeredTopPos(screen) + yOffset));
    }

    public static Panel centered(Screen screen, ResourceLocation panelSprite, int imageWidth, int imageHeight){
        return Panel.createPanel(screen, p-> p.appearance(panelSprite,imageWidth,imageHeight), p-> p.pos(p.centeredLeftPos(screen),p.centeredTopPos(screen)));
    }

    public static Panel tooltipBoxOf(Panel panel, int boxWidth){
        Panel p = new Panel(panel.accessor){
            @Override
            public void init(String name) {
                super.init(name);
                panel.x-=(boxWidth - 2)/ 2;
                appearance(LegacySprites.POINTER_PANEL, boxWidth, panel.height - 10);
                pos(panel.x + panel.width - 2,panel.y + 5);
            }

            @Override
            public void init() {
                init("tooltipBox");
            }

            @Override
            public void render(GuiGraphics guiGraphics, int i, int j, float f) {
                ScreenUtil.renderPointerPanel(guiGraphics,getX(),getY(),getWidth(),getHeight());
            }
        };
        p.init();
        return p;
    }

    public void appearance(int width, int height){
        appearance(LegacySprites.SMALL_PANEL,width,height);
    }

    public void appearance(ResourceLocation sprite, int width, int height){
        panelSprite = accessor.getElementValue(name+".sprite",sprite,ResourceLocation.class);
        size(accessor.putStaticElement(name+".width",accessor.getInteger(name+".width",width)),accessor.putStaticElement(name+".height",accessor.getInteger(name+".height",height)));
    }

    public void pos(int x, int y){
        setPosition(accessor.putStaticElement(name+".x",accessor.getInteger(name+".x",x)),accessor.putStaticElement(name+".y",accessor.getInteger(name+".y",y)));
    }

    public void centered(Screen screen){
        pos(centeredLeftPos(screen), centeredTopPos(screen));
    }

    public void init(String name){
        this.name = name;
    }

    public void init() {
        init("panel");
    }

    public void render(GuiGraphics guiGraphics, int i, int j, float f) {
        FactoryGuiGraphics.of(guiGraphics).blitSprite(panelSprite, x, y, width, height);
    }
}
