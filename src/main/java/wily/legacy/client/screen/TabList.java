package wily.legacy.client.screen;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import wily.factoryapi.base.Stocker;
import wily.factoryapi.base.client.UIAccessor;
import wily.legacy.client.NavigationElement;
import wily.legacy.init.LegacyRegistries;
import wily.legacy.util.client.LegacySoundUtil;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class TabList implements Renderable, GuiEventListener, NarratableEntry, NavigationElement {
    public final List<LegacyTabButton> tabButtons;
    protected final UIAccessor accessor;
    public LegacyTabButton selected = null;
    boolean focused = false;
    private int selectedIndex = 0;

    public TabList(UIAccessor uiAccessor) {
        this(uiAccessor, new ArrayList<>());
    }

    public TabList(UIAccessor accessor, List<LegacyTabButton> list) {
        this.accessor = accessor;
        this.tabButtons = list;
    }

    public LegacyTabButton addTabButton(LegacyTabButton button) {
        tabButtons.add(button);
        if (selected == null) selected = button;
        return button;
    }

    @Override
    public void applyFocus(ComponentPath.Path path, boolean apply) {
    }

    public LegacyTabButton addTabButton(int x, int y, int width, int height, LegacyTabButton.Type type, LegacyTabButton.Render icon, Component message, Tooltip tooltip, Consumer<LegacyTabButton> onPress) {
        return this.addTabButton(new LegacyTabButton(x, y, width, height, type, icon, message, tooltip, t -> {
            if (selected != t) {
                selectedIndex = tabButtons.indexOf(t);
                selected = t;
                onPress.accept(t);
            }
        }));
    }

    public LegacyTabButton addTabButton(int height, LegacyTabButton.Type type, LegacyTabButton.Render icon, Component component, Consumer<LegacyTabButton> onPress) {
        return addTabButton(0, 0, 0, height, type, icon, component, null, onPress);
    }

    public TabList add(int x, int y, int width, int height, LegacyTabButton.Type type, LegacyTabButton.Render icon, Component message, Tooltip tooltip, Consumer<LegacyTabButton> onPress) {
        this.addTabButton(x, y, width, height, type, icon, message, tooltip, onPress);
        return this;
    }

    public TabList add(LegacyTabButton.Type type, LegacyTabButton.Render icon, Component component, Consumer<LegacyTabButton> onPress) {
        return add(0, 0, 0, 0, type, icon, component, null, onPress);
    }

    public TabList add(LegacyTabButton.Type type, Component component, Consumer<LegacyTabButton> onPress) {
        return add(type, null, component, onPress);
    }

    public TabList add(int width, int height, LegacyTabButton.Type type, LegacyTabButton.Render icon, Component message, Consumer<LegacyTabButton> onPress) {
        return add(0, 0, width, height, type, icon, message, null, onPress);
    }

    public TabList add(int x, int y, int height, LegacyTabButton.Type type, Component message, Consumer<LegacyTabButton> onPress) {
        return add(x, y, 0, height, type, null, message, null, onPress);
    }

    public TabList add(int width, int height, LegacyTabButton.Type type, Component message, Consumer<LegacyTabButton> onPress) {
        return add(0, 0, width, height, type, null, message, null, onPress);
    }

    public void init(int leftPos, int topPos, int width, int height) {
        init(leftPos, topPos, width, height, (t, i) -> {
        });
    }

    public void init(int leftPos, int topPos, int width, int height, BiConsumer<LegacyTabButton, Integer> buttonManager) {
        init("tabList", leftPos, topPos, width, height, buttonManager);
    }

    public void init(String name, int leftPos, int topPos, int width, int height, BiConsumer<LegacyTabButton, Integer> buttonManager) {
        init((b, i) -> {
            b.setHeight(accessor.getInteger(name + ".height", height));
            b.setWidth(width / tabButtons.size());
            b.setX(accessor.getInteger(name + ".x", leftPos) + i);
            b.setY(accessor.getInteger(name + ".y", topPos));
            b.offset = accessor.getElementValue(name + ".offset", LegacyTabButton.StateOffset.DEFAULT, LegacyTabButton.StateOffset.class);
            b.spriteRender = accessor.getElementValue(name + ".sprites", LegacyTabButton.ToggleableTabSprites.DEFAULT, LegacyTabButton.Render.class);
            buttonManager.accept(b, i);
        }, false);
    }

    public void init(BiConsumer<LegacyTabButton, Integer> buttonManager, boolean vertical) {
        int position = 0;
        for (LegacyTabButton b : tabButtons) {
            buttonManager.accept(b, position);
            position += vertical ? b.getHeight() : b.getWidth();
        }
    }

    @Override
    public void render(GuiGraphics graphics, int i, int j, float f) {
        for (LegacyTabButton tabButton : tabButtons) {
            tabButton.selected = tabButton == selected;
            if (tabButton == selected) continue;
            tabButton.render(graphics, i, j, f);
        }
    }

    public void renderSelected(GuiGraphics graphics, int i, int j, float f) {
        if (selected != null) selected.render(graphics, i, j, f);
    }

    public void resetSelectedTab() {
        if (!tabButtons.isEmpty()) {
            selected = null;
            tabButtons.get(0).onPress(new KeyEvent(InputConstants.KEY_RETURN, 0, 0));
        }
    }

    public void setSelected(int selected) {
        if (!tabButtons.isEmpty() && selected < tabButtons.size()) {
            tabButtons.get(selected).onPress(new KeyEvent(InputConstants.KEY_RETURN, 0, 0));
        }
    }

    @Override
    public boolean isFocused() {
        return focused;
    }

    @Override
    public void setFocused(boolean bl) {
        focused = bl;
    }

    @Override
    public boolean keyPressed(KeyEvent keyEvent) {
        return tabButtons.stream().anyMatch(t -> t.isHoveredOrFocused() && t.keyPressed(keyEvent));
    }

    public boolean controlTab(int i) {
        return controlTab(i, InputConstants.KEY_LBRACKET, InputConstants.KEY_RBRACKET);
    }

    public boolean directionalControlTab(int i) {
        return controlTab(i, InputConstants.KEY_LEFT, InputConstants.KEY_RIGHT);
    }

    public boolean controlTab(int i, int leftButton, int rightButton) {
        return controlTab(i == leftButton, i == rightButton);
    }

    public boolean controlTab(boolean left, boolean right) {
        if (!left && !right || tabButtons.isEmpty()) return false;
        Optional<LegacyTabButton> opt = tabButtons.stream().filter(LegacyTabButton::isActive).min(Comparator.comparingInt(t -> {
            int diff = tabButtons.indexOf(t) - getIndex();
            return left ? diff < 0 ? -diff : tabButtons.size() * 2 - diff : diff > 0 ? diff : tabButtons.size() * 2 + diff;
        }));
        if (opt.isPresent()) {
            if (tabButtons.indexOf(opt.get()) != getIndex()) {
                opt.get().onPress(new KeyEvent(InputConstants.KEY_RETURN, 0, 0));
                LegacySoundUtil.playSimpleUISound(LegacyRegistries.FOCUS.get(), true);
                return true;
            }
        }
        return false;
    }

    protected boolean controlPage(Stocker.Sizeable page, boolean left, boolean right) {
        if ((left || right) && page.max > 0) {
            int lastPage = page.get();
            page.add(left ? -1 : 1);
            if (lastPage != page.get()) {
                resetSelectedTab();
                return true;
            }
        }
        return false;
    }

    public void numberControlTab(int i) {
        if (i <= 57 && i > 48 && i - 49 < tabButtons.size()) {
            tabButtons.get(i - 49).onPress(new KeyEvent(InputConstants.KEY_RETURN, 0, 0));
            LegacySoundUtil.playSimpleUISound(LegacyRegistries.FOCUS.get(), true);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean bl) {
        return !tabButtons.stream().filter(t -> t.mouseClicked(event, bl)).toList().isEmpty();
    }

    public boolean isMouseOver(double d, double e) {
        return !tabButtons.stream().filter(t -> t.isMouseOver(d, e)).toList().isEmpty();
    }

    @Override
    public NarrationPriority narrationPriority() {
        return this.tabButtons.stream().map(AbstractWidget::narrationPriority).max(Comparator.naturalOrder()).orElse(NarrationPriority.NONE);
    }

    @Override
    public void updateNarration(NarrationElementOutput narrationElementOutput) {
        Optional<LegacyTabButton> optional = this.tabButtons.stream().filter(AbstractWidget::isHovered).findFirst().or(() -> Optional.ofNullable(tabButtons.get(getIndex())));
        optional.ifPresent(tabButton -> {
            narrationElementOutput.add(NarratedElementType.POSITION, Component.translatable("narrator.position.tab", getIndex() + 1, tabButtons.size()));
            tabButton.updateNarration(narrationElementOutput);
        });
        if (this.isFocused()) {
            narrationElementOutput.add(NarratedElementType.USAGE, Component.translatable("narration.tab_navigation.usage"));
        }
    }

    public int getIndex() {
        return selectedIndex;
    }

    public interface Access {
        TabList getTabList();

        default int getTabYOffset() {
            int offset = 0;
            for (LegacyTabButton tabButton : getTabList().tabButtons) {
                if (tabButton.getHeight() > offset) offset = tabButton.getHeight();
            }
            return offset / 2;
        }

        default int getTabXOffset() {
            return 0;
        }
    }
}
