package wily.legacy.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import wily.factoryapi.base.client.SimpleLayoutRenderable;
import wily.legacy.client.CommonColor;
import wily.legacy.client.LegacyOptions;
import wily.legacy.util.LegacyComponents;
import wily.legacy.util.LegacySprites;
import wily.legacy.util.client.LegacyRenderUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class Legacy4JSettingsScreen extends OptionsScreen implements TabList.Access {
    protected final TabList tabList = new TabList(accessor);
    protected final List<List<Renderable>> renderablesByTab = new ArrayList<>();
    protected final EditBox editBox = new EditBox(Minecraft.getInstance().font, 0, 0, 200, 20, Component.translatable("legacy.menu.filter.search"));


    public Legacy4JSettingsScreen(Screen screen) {
        super(screen,s-> Panel.centered(s,250,250, 50, 0), CommonComponents.EMPTY);
        tabList.add(0,0,100, 25, LegacyTabButton.Type.MIDDLE, null, LegacyComponents.ALL,null, b->resetElements());
        renderablesByTab.add(new ArrayList<>());
        OptionsScreen.Section.list.forEach(this::addOptionSection);
        addActualRenderables();
    }

    protected void addOptionSection(OptionsScreen.Section section){
        tabList.add(0,0,100, 25, LegacyTabButton.Type.MIDDLE, null, section.title(),null, b->resetElements());
        section.elements().forEach(c->c.accept(this));
        section.advancedSection().ifPresent(s1-> {
            getRenderableVList().addRenderable(SimpleLayoutRenderable.createDrawString(s1.title(),0,1,200,9, CommonColor.INVENTORY_GRAY_TEXT.get(), false));
            if (s1 == Section.ADVANCED_USER_INTERFACE) getRenderableVList().addOptions(LegacyOptions.advancedOptionsMode);
            s1.elements().forEach(c -> c.accept(this));
        });
        List<Renderable> renderables = List.copyOf(getRenderableVList().renderables);
        getRenderableVList().renderables.clear();
        renderablesByTab.get(0).addAll(renderables);
        renderablesByTab.add(renderables);
    }

    protected void resetElements(){
        getRenderableVList().renderables.clear();
        addActualRenderables();
        getRenderableVList().scrolledList.set(0);
        repositionElements();
    }

    protected void addActualRenderables(){
        String value = editBox.getValue().toLowerCase(Locale.ROOT);
        if (value.isBlank()) {
            getRenderableVList().renderables.addAll(renderablesByTab.get(getTabList().selectedIndex));
        } else {
            for (Renderable renderable : renderablesByTab.get(getTabList().selectedIndex)) {
                if (renderable instanceof AbstractWidget w && w.getMessage().getString().toLowerCase(Locale.ROOT).contains(value)){
                    getRenderableVList().renderables.add(w);
                }
            }
        }
    }

    @Override
    public void renderDefaultBackground(GuiGraphics guiGraphics, int i, int j, float f) {
        LegacyRenderUtil.renderDefaultBackground(accessor, guiGraphics, false);
    }

    @Override
    protected void init() {
        addRenderableWidget(tabList);
        super.init();
        panel.panelSprite = LegacySprites.SMALL_PANEL;
        addRenderableOnly(tabList::renderSelected);
        addRenderableWidget(editBox);
        editBox.setPosition(panel.getX() + (panel.width - editBox.getWidth()) / 2, panel.getY() + 10);
        editBox.setResponder(s->resetElements());
        tabList.init((b, i)->{
            b.spriteRender = LegacyTabButton.ToggleableTabSprites.VERTICAL;
            b.setX(panel.x - b.getWidth() + 6);
            b.setY(panel.y + i + 4);
            b.offset = (t1) -> new Vec3(t1.selected ? 0 : 3.4, 0.4, 0);
        },true);
    }

    @Override
    public void renderableVListInit() {
        getRenderableVList().init(panel.x + 10,panel.y + 40,panel.width - 20,panel.height-50);
    }

    @Override
    public boolean keyPressed(int i, int j, int k) {
        if (tabList.controlTab(i)) return true;
        return super.keyPressed(i, j, k);
    }

    @Override
    public TabList getTabList() {
        return tabList;
    }
}
