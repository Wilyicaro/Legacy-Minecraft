package wily.legacy.client.screen;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.TabButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import wily.legacy.init.LegacySoundEvents;
import wily.legacy.util.ScreenUtil;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class TabList implements Renderable,GuiEventListener, NarratableEntry {

    public final List<LegacyTabButton> tabButtons;
    public int selectedTab = 0;
    boolean focused = false;
    public TabList(){
        this(new ArrayList<>());
    }
    public TabList(List<LegacyTabButton> list){
        this.tabButtons = list;
    }
    public LegacyTabButton addTabButton(LegacyTabButton button){
        tabButtons.add(button);
        return button;
    }
    public LegacyTabButton addTabButton(int x, int y, int width, int height, int type, ResourceLocation icon, CompoundTag itemIconTag, Component message, Tooltip tooltip, Consumer<LegacyTabButton> onPress){
        return this.addTabButton(new LegacyTabButton(x,y,width,height,type,icon,itemIconTag,message,tooltip, t-> {if (selectedTab != tabButtons.indexOf(t)) {
            selectedTab = tabButtons.indexOf(t);
            onPress.accept(t);
        }}));
    }
    public LegacyTabButton addTabButton(int height, int type, ResourceLocation icon, Component component, Consumer<LegacyTabButton> onPress){
        return addTabButton(0,0,0,height,type,icon, null,component, null,onPress);
    }
    public LegacyTabButton addTabButton(int height, int type, ResourceLocation icon, CompoundTag itemIconTag, Component component, Consumer<LegacyTabButton> onPress){
        return addTabButton(0,0,0,height,type,icon, itemIconTag,component, null,onPress);
    }
    public TabList add(int x, int y, int width, int height, int type, ResourceLocation icon, CompoundTag itemIconTag, Component message, Tooltip tooltip, Consumer<LegacyTabButton> onPress){
        this.addTabButton(x,y,width,height,type,icon,itemIconTag,message,tooltip,onPress);
        return this;
    }

    public TabList add(int x, int y, int width, int height, int type, Component message, Consumer<LegacyTabButton> onPress){
        return add(x,y,width,height,type,null,null,message,null,onPress);
    }
    public TabList add(int x, int y, int height, int type, Component message, Consumer<LegacyTabButton> onPress){
        return add(x,y,0,height,type,null,null,message,null,onPress);
    }
    public TabList add(int height, int type, Component message, Consumer<LegacyTabButton> onPress){
        return add(0,0,0,height,type,null,null,message,null,onPress);
    }
    public void init(int leftPos, int topPos, int width){
        init(leftPos,topPos,width,(t,i)->{});
    }
    public void init(int leftPos, int topPos, int width,BiConsumer<LegacyTabButton, Integer> buttonManager){
        init((b,i)->{
            b.setWidth(width / tabButtons.size());
            b.setX(leftPos + i);
            b.setY(topPos);
            buttonManager.accept(b,i);
        },false);
    }
    public void init(BiConsumer<LegacyTabButton, Integer> buttonManager, boolean vertical) {
        int x = 0;
        for (LegacyTabButton b : tabButtons) {
            buttonManager.accept(b,x);
            x+=vertical ? b.getHeight() : b.getWidth();
        }
    }
    @Override
    public void render(GuiGraphics graphics, int i, int j, float f) {
        for (int index = 0; index < tabButtons.size(); index++) {
            LegacyTabButton tabButton = tabButtons.get(index);
            tabButton.selected = selectedTab == index;
            tabButton.render(graphics,i, j, f);
        }
    }
    public void resetSelectedTab(){
        if (!tabButtons.isEmpty()){
            selectedTab = -1;
            tabButtons.get(0).onPress();
        }
    }

    @Override
    public void setFocused(boolean bl) {
        focused = bl;
    }

    @Override
    public boolean isFocused() {
        return focused;
    }

    @Override
    public boolean keyPressed(int i, int j, int k) {
        return tabButtons.stream().anyMatch(t -> t.isHoveredOrFocused() && t.keyPressed(i, j, k));
    }
    public boolean controlTab(int i){
        return controlTab(i,InputConstants.KEY_LBRACKET,InputConstants.KEY_RBRACKET);
    }
    public void directionalControlTab(int i){
        controlTab(i,InputConstants.KEY_LEFT,InputConstants.KEY_RIGHT);
    }

    public boolean controlTab(int i, int leftButton, int rightButton){
        return controlTab(i == leftButton, i == rightButton);
    }
    public boolean controlTab(boolean left, boolean right){
        if (!left && !right) return false;
        Optional<LegacyTabButton> opt = tabButtons.stream().filter(LegacyTabButton::isActive).min(Comparator.comparingInt(t -> {
            int diff = tabButtons.indexOf(t) - selectedTab;
            return left ? diff < 0 ? -diff : tabButtons.size() * 2 - diff : diff > 0 ? diff : tabButtons.size() * 2 + diff;
        }));
        if (opt.isPresent()){
            if (tabButtons.indexOf(opt.get()) != selectedTab){
                opt.get().onPress();
                ScreenUtil.playSimpleUISound(LegacySoundEvents.FOCUS.get(),1.0f);
                return true;
            }
        }
        return false;
    }

    public void numberControlTab(int i){
        if (i <= 57 && i > 48 && i - 49 < tabButtons.size()) {
            tabButtons.get(i - 49).onPress();
            ScreenUtil.playSimpleUISound(LegacySoundEvents.FOCUS.get(),1.0f);
        }
    }
    @Override
    public boolean mouseClicked(double d, double e, int i) {
        return !tabButtons.stream().filter(t-> t.mouseClicked(d,e,i)).toList().isEmpty();
    }
    public boolean isMouseOver(double d, double e) {
        return !tabButtons.stream().filter(t-> t.isMouseOver(d,e)).toList().isEmpty();
    }

    @Override
    public NarrationPriority narrationPriority() {
        return this.tabButtons.stream().map(AbstractWidget::narrationPriority).max(Comparator.naturalOrder()).orElse(NarrationPriority.NONE);
    }

    @Override
    public void updateNarration(NarrationElementOutput narrationElementOutput) {
        Optional<LegacyTabButton> optional = this.tabButtons.stream().filter(AbstractWidget::isHovered).findFirst().or(() -> Optional.ofNullable(tabButtons.get(selectedTab)));
        optional.ifPresent(tabButton -> {
            narrationElementOutput.add(NarratedElementType.POSITION, Component.translatable("narrator.position.tab", selectedTab + 1, tabButtons.size()));
            tabButton.updateNarration(narrationElementOutput);
        });
        if (this.isFocused()) {
            narrationElementOutput.add(NarratedElementType.USAGE,  Component.translatable("narration.tab_navigation.usage"));
        }
    }
}
