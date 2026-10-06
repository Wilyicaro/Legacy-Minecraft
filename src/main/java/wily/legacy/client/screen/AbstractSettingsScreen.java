package wily.legacy.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec2;
import wily.legacy.util.LegacyComponents;
import wily.legacy.util.client.LegacyRenderUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public abstract class AbstractSettingsScreen extends OptionsScreen implements TabList.Access {
    protected final TabList tabList = new TabList(accessor);
    protected final List<List<Renderable>> renderablesByTab = new ArrayList<>();
    protected final EditBox editBox = new EditBox(Minecraft.getInstance().font, 0, 0, 200, 20, Component.translatable("legacy.menu.filter.search"));
    protected final boolean useTabs;

    public AbstractSettingsScreen(Screen screen, boolean useTabs) {
        this.useTabs = useTabs;
        super(screen, s -> Panel.createPanel(s, p -> p.appearance(250, Math.min(250, s.height - 52)), p -> p.pos(p.centeredLeftPos(s) + 50, p.centeredTopPos(s))), CommonComponents.EMPTY);
        if (useTabs) {
            tabList.add(100, 25, LegacyTabButton.Type.MIDDLE, LegacyComponents.ALL, _ -> resetElements());
        }
        addSections();
        addActualRenderables();
    }

    protected abstract void addSections();

    protected void resetElements() {
        getRenderableVList().renderables.clear();
        addActualRenderables();
        getRenderableVList().scrolledList.set(0);
        repositionElements();
    }

    protected void addActualRenderables() {
        String value = editBox.getValue().toLowerCase(Locale.ROOT);
        if (value.isBlank()) {
            getRenderableVList().renderables.addAll(renderablesByTab.get(useTabs ? getTabList().getIndex() : 0));
        } else {
            for (Renderable renderable : renderablesByTab.get(useTabs ? getTabList().getIndex() : 0)) {
                if (renderable instanceof AbstractWidget w && w.getMessage().getString().toLowerCase(Locale.ROOT).contains(value)) {
                    getRenderableVList().renderables.add(w);
                }
            }
        }
    }

    @Override
    public void renderDefaultBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        LegacyRenderUtil.renderDefaultBackground(accessor, graphics, false);
    }

    @Override
    protected void init() {
        if (useTabs) addRenderableWidget(tabList);
        super.init();
        if (useTabs) addRenderableOnly(tabList::renderSelected);
        addRenderableWidget(editBox);
        editBox.setWidth(panel.getWidth() - 50);
        editBox.setPosition(panel.getX() + (panel.width - editBox.getWidth()) / 2, panel.getY() + 10);
        editBox.setResponder(_ -> resetElements());
        if (useTabs) {
            tabList.init((b, i) -> {
                b.spriteRender = accessor.getElementValue("tabList.sprites", LegacyTabButton.ToggleableTabSprites.VERTICAL, LegacyTabButton.Render.class);
                b.setX(panel.x - b.getWidth() + 6);
                b.setY(panel.y + i + 5);
                b.offset = (t1) -> new Vec2(t1.selected ? 0 : 3.4f, 0.4f);
            }, true);
        }
    }

    @Override
    public void renderableVListInit() {
        getRenderableVList().init(panel.x + 10, panel.y + 40, panel.width - 20, panel.height - 50);
    }

    @Override
    public boolean keyPressed(KeyEvent keyEvent) {
        if (useTabs && tabList.controlTab(keyEvent)) return true;
        return super.keyPressed(keyEvent);
    }

    @Override
    public TabList getTabList() {
        return tabList;
    }
}
