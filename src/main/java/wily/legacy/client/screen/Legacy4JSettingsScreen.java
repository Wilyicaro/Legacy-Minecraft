package wily.legacy.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec2;
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
        super(screen, s -> Panel.createPanel(s, p -> p.appearance(250, Math.min(250, s.height - 52)), p -> p.pos(p.centeredLeftPos(s) + 50, p.centeredTopPos(s))), CommonComponents.EMPTY);
        tabList.add(100, 25, LegacyTabButton.Type.MIDDLE, LegacyComponents.ALL, b -> resetElements());
        renderablesByTab.add(new ArrayList<>());
        OptionsScreen.Section.list.forEach(this::addOptionSection);
        addActualRenderables();
    }

    protected void addOptionSection(OptionsScreen.Section section) {
        tabList.add(100, 25, LegacyTabButton.Type.MIDDLE, section.title(), b -> resetElements());
        section.elements().forEach(c -> c.accept(this));
        if (section == Section.GAME_OPTIONS)
            getRenderableVList().addRenderables(Button.builder(Component.translatable("controls.keybinds.title"), button -> this.minecraft.setScreen(new LegacyKeyMappingScreen(this))).build(), Button.builder(Component.translatable("legacy.options.selectedController"), button -> this.minecraft.setScreen(new ControllerMappingScreen(this))).build());
        section.advancedSection().ifPresent(s1 -> {
            getRenderableVList().addRenderable(SimpleLayoutRenderable.createDrawString(s1.title(), 0, 1, 200, 9, CommonColor.INVENTORY_GRAY_TEXT.get(), false));
            if (s1 == Section.ADVANCED_USER_INTERFACE)
                getRenderableVList().addOptions(LegacyOptions.advancedOptionsMode);
            s1.elements().forEach(c -> c.accept(this));
        });
        List<Renderable> renderables = List.copyOf(getRenderableVList().renderables);
        getRenderableVList().renderables.clear();
        renderablesByTab.get(0).addAll(renderables);
        renderablesByTab.add(renderables);
    }

    protected void resetElements() {
        getRenderableVList().renderables.clear();
        addActualRenderables();
        getRenderableVList().scrolledList.set(0);
        repositionElements();
    }

    protected void addActualRenderables() {
        String value = editBox.getValue().toLowerCase(Locale.ROOT);
        if (value.isBlank()) {
            getRenderableVList().renderables.addAll(renderablesByTab.get(getTabList().getIndex()));
        } else {
            for (Renderable renderable : renderablesByTab.get(getTabList().getIndex())) {
                if (renderable instanceof AbstractWidget w && w.getMessage().getString().toLowerCase(Locale.ROOT).contains(value)) {
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
        addRenderableOnly(tabList::renderSelected);
        addRenderableWidget(editBox);
        editBox.setWidth(panel.getWidth() - 50);
        editBox.setPosition(panel.getX() + (panel.width - editBox.getWidth()) / 2, panel.getY() + 10);
        editBox.setResponder(s -> resetElements());
        tabList.init((b, i) -> {
            b.spriteRender = accessor.getElementValue("tabList.sprites", LegacyTabButton.ToggleableTabSprites.VERTICAL, LegacyTabButton.Render.class);
            b.setX(panel.x - b.getWidth() + 6);
            b.setY(panel.y + i + 5);
            b.offset = (t1) -> new Vec2(t1.selected ? 0 : 3.4f, 0.4f);
        }, true);
    }

    @Override
    public void renderableVListInit() {
        getRenderableVList().init(panel.x + 10, panel.y + 40, panel.width - 20, panel.height - 50);
    }

    @Override
    public boolean keyPressed(KeyEvent keyEvent) {
        if (tabList.controlTab(keyEvent.key())) return true;
        return super.keyPressed(keyEvent);
    }

    @Override
    public TabList getTabList() {
        return tabList;
    }
}
