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
import wily.factoryapi.base.client.WidgetAccessor;
import wily.legacy.client.CommonColor;
import wily.legacy.client.LegacyOptions;
import wily.legacy.util.LegacyComponents;
import wily.legacy.util.ScreenUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class Legacy4JSettingsScreen extends OptionsScreen implements TabList.Access {
    protected final TabList tabList = new TabList(accessor);
    protected final List<List<Renderable>> renderablesByTab = new ArrayList<>();
    protected final EditBox editBox = new EditBox(Minecraft.getInstance().font, 0, 0, 200, 20, Component.translatable("legacy.menu.filter.search"));


    public Legacy4JSettingsScreen(Screen screen) {
        super(screen, s -> Panel.createPanel(s,
                p -> p.appearance(LegacyOptions.getUIMode().isSD() ? 180 : 250, Math.min(LegacyOptions.getUIMode().isSD() ? 178 : 250, s.height - (LegacyOptions.getUIMode().isSD() ? 36 : 52))),
                p -> p.pos(p.centeredLeftPos(s) + (LegacyOptions.getUIMode().isSD() ? 32 : 50), p.centeredTopPos(s))), CommonComponents.EMPTY);
        tabList.add(0,0,100, 25, LegacyTabButton.Type.MIDDLE, null, LegacyComponents.ALL,null, b->resetElements());
        renderablesByTab.add(new ArrayList<>());
        OptionsScreen.Section.list.forEach(this::addOptionSection);
        addActualRenderables();
    }

    protected void addOptionSection(OptionsScreen.Section section){
        tabList.add(0,0,100, 25, LegacyTabButton.Type.MIDDLE, null, section.title(),null, b->resetElements());
        section.elements().forEach(c->c.accept(this));
        section.advancedSection().ifPresent(s1-> {
            getRenderableVList().addCategory(s1.title());
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
            getRenderableVList().renderables.addAll(renderablesByTab.get(getTabList().selectedTab));
        } else {
            for (Renderable renderable : renderablesByTab.get(getTabList().selectedTab)) {
                if (renderable instanceof AbstractWidget w && w.getMessage().getString().toLowerCase(Locale.ROOT).contains(value)){
                    getRenderableVList().renderables.add(w);
                }
            }
        }
    }

    @Override
    public void renderDefaultBackground(GuiGraphics guiGraphics, int i, int j, float f) {
        ScreenUtil.renderDefaultBackground(accessor, guiGraphics, false);
    }

    @Override
    protected void init() {
        addRenderableWidget(tabList);
        super.init();
        addRenderableOnly(tabList::renderSelected);
        addRenderableWidget(editBox);
        editBox.setWidth(accessor.getInteger("searchBox.width", panel.width - 50));
        //? if <=1.20.1 {
        /*((WidgetAccessor) editBox).setHeight(accessor.getInteger("searchBox.height", LegacyOptions.getUIMode().isSD() ? 16 : 20));
        *///?} else {
        editBox.setHeight(accessor.getInteger("searchBox.height", LegacyOptions.getUIMode().isSD() ? 16 : 20));
        //?}
        editBox.setPosition(accessor.getInteger("searchBox.x", panel.getX() + (panel.width - editBox.getWidth()) / 2), accessor.getInteger("searchBox.y", panel.getY() + 10));
        editBox.setResponder(s->resetElements());
        tabList.init((b, i)->{
            b.spriteRender = accessor.getElementValue("tabList.sprites", LegacyTabButton.ToggleableTabSprites.VERTICAL, LegacyTabButton.Render.class);
            b.setWidth(accessor.getInteger("tabList.buttonWidth", 100));
            //? if <=1.20.1 {
            /*((WidgetAccessor) b).setHeight(accessor.getInteger("tabList.buttonHeight", 25));
            *///?} else {
            b.setHeight(accessor.getInteger("tabList.buttonHeight", 25));
            //?}
            b.setX(accessor.getInteger("tabList.x", panel.x - b.getWidth() + 6));
            b.setY(accessor.getInteger("tabList.y", panel.y + 4) + i);
            b.offset = accessor.getElementValue("tabList.offset", new LegacyTabButton.StateOffset(Vec3.ZERO, new Vec3(3.4, 0.4, 0), LegacyTabButton.DEFAULT_DESACTIVE_OFFSET), LegacyTabButton.StateOffset.class);
            b.textXPadding = accessor.getInteger("tabList.textXPadding", 6);
            b.textYOffset = accessor.getInteger("tabList.textYOffset", -2);
            b.textBottomPadding = accessor.getInteger("tabList.textBottomPadding", 1);
        },true);
    }

    @Override
    public void renderableVListInit() {
        getRenderableVList().init(
                accessor.getInteger("renderableVList.x", panel.x + 10),
                accessor.getInteger("renderableVList.y", panel.y + 40),
                accessor.getInteger("renderableVList.width", panel.width - 20),
                accessor.getInteger("renderableVList.height", panel.height - 50));
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
