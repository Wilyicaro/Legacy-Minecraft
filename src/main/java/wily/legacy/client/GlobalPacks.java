package wily.legacy.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.MultiLineLabel;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.navigation.CommonInputs;
import net.minecraft.client.gui.navigation.ScreenDirection;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.packs.PackSelectionModel;
import net.minecraft.client.gui.screens.packs.PackSelectionScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackRepository;
import org.jetbrains.annotations.Nullable;
import wily.factoryapi.FactoryAPIPlatform;
import wily.factoryapi.base.Stocker;
import wily.factoryapi.base.client.FactoryGuiGraphics;
import wily.factoryapi.base.config.FactoryConfig;
import wily.factoryapi.util.FactoryScreenUtil;
import wily.legacy.client.screen.ControlTooltip;
import wily.legacy.client.screen.LegacyScrollRenderer;
import wily.legacy.client.screen.ScrollableRenderer;
import wily.legacy.init.LegacyRegistries;
import wily.legacy.util.LegacyComponents;
import wily.legacy.util.LegacySprites;
import wily.legacy.util.client.LegacyRenderUtil;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public record GlobalPacks(List<String> list, boolean applyOnTop) {
    public static final GlobalPacks EMPTY = new GlobalPacks(Collections.emptyList(), true);

    public void applyPacks(PackRepository repository, List<String> additional){
        List<String> packs = new ArrayList<>(list());
        packs.removeIf(additional::contains);
        packs.addAll(applyOnTop ? 0 : packs.size(), additional);
        repository.setSelected(packs);
    }

    public GlobalPacks withPacks(List<String> packs){
        return new GlobalPacks(packs, applyOnTop);
    }

    public GlobalPacks withApplyOnTop(boolean applyOnTop){
        return new GlobalPacks(list, applyOnTop);
    }

    public static final Codec<GlobalPacks> CODEC = RecordCodecBuilder.create(i-> i.group(Codec.STRING.listOf().fieldOf("packs").forGetter(GlobalPacks::list), Codec.BOOL.fieldOf("applyOnTop").forGetter(GlobalPacks::applyOnTop)).apply(i, GlobalPacks::new));
    public static final FactoryConfig.StorageHandler STORAGE = new FactoryConfig.StorageHandler().withFile("legacy/global_packs.json");
    public static final FactoryConfig<GlobalPacks> globalResources = STORAGE.register(FactoryConfig.create("globalResources", null, ()-> CODEC, EMPTY.withPacks(List.of("legacy:legacy_resources")), v-> {}, STORAGE));


    public static class Selector extends AbstractWidget implements ControlTooltip.ActionHolder {
        private final Component screenComponent;
        public final FactoryConfig<GlobalPacks> globalPacks;
        public PackSelectionModel model;
        public final Stocker.Sizeable scrolledList;
        private final Path packPath;
        private final boolean hasTooltip;
        public int selectedIndex = -1;
        public Pack selectedPack;
        private final PackRepository packRepository;
        private final Minecraft minecraft;
        protected final LegacyScrollRenderer scrollRenderer = new LegacyScrollRenderer();
        public final ScrollableRenderer scrollableRenderer  = new ScrollableRenderer(scrollRenderer);
        public final BiFunction<Component,Integer,MultiLineLabel> labelsCache = Util.memoize((c, i)->MultiLineLabel.create(Minecraft.getInstance().font,c,i));

        public static Selector resources(int i, int j, int k, int l, boolean hasTooltip) {
            return new Selector(i,j,k,l, LegacyComponents.GLOBAL_RESOURCE_PACKS, LegacyComponents.SHOW_RESOURCE_PACKS, Minecraft.getInstance().getResourcePackRepository(),Minecraft.getInstance().getResourcePackDirectory(), globalResources, hasTooltip);
        }

        public Selector(int i, int j, int k, int l, Component component, Component screenComponent, PackRepository packRepository, Path packPath, FactoryConfig<GlobalPacks> globalPacks, boolean hasTooltip) {
            super(i, j, k, l,component);
            this.screenComponent = screenComponent;
            this.globalPacks = globalPacks;
            this.packPath = packPath;
            this.hasTooltip = hasTooltip;
            minecraft = Minecraft.getInstance();
            this.packRepository = packRepository;
            Collection<String> packs = packRepository.getSelectedIds();
            packRepository.setSelected(globalPacks.get().list());
            updateModel();
            packRepository.setSelected(packs);
            scrolledList = new Stocker.Sizeable(0);
            List<Pack> displayPacks = getDisplayPacks();
            int s = displayPacks.size();
            if (s > getMaxPacks())
                scrolledList.max = displayPacks.size() - getMaxPacks();
            setSelectedPack(0);
            updateTooltip();
        }

        public List<Pack> getDisplayPacks(){
            return Stream.concat(model.selected.stream(),model.unselected.stream()).toList();
        }

        public void updateTooltip(){
            if (hasTooltip) setTooltip(Tooltip.create(selectedPack.getDescription(), selectedPack.getTitle()));
        }

        public void renderTooltipBox(GuiGraphics guiGraphics, LayoutElement panel){
            renderTooltipBox(guiGraphics, panel, 0);
        }

        public void renderTooltipBox(GuiGraphics guiGraphics, LayoutElement panel, int xOffset){
            renderTooltipBox(guiGraphics,panel.getX() + panel.getWidth() - 2 + xOffset, panel.getY() + 5, 161, panel.getHeight() - 10);
        }

        public void renderTooltipBox(GuiGraphics graphics, int x, int y, int width, int height){
            if (hasTooltip) return;
            LegacyRenderUtil.renderPointerPanel(graphics,x, y,width,height);
            if (selectedPack != null){
                FactoryGuiGraphics.of(graphics).blit(PackAlbum.Selector.getPackIcon(selectedPack), x + 7,y + 5,0.0f, 0.0f, 32, 32, 32, 32);
                FactoryGuiGraphics.of(graphics).enableScissor(x + 40, y + 4,x + 148, y + 44);
                labelsCache.apply(selectedPack.getTitle(),108).renderLeftAligned(graphics,x + 43, y + 8,12,0xFFFFFFFF);
                graphics.disableScissor();
                ResourceLocation background = PackAlbum.Selector.getPackBackground(selectedPack);
                MultiLineLabel label = labelsCache.apply(selectedPack.getDescription(), 145);
                scrollableRenderer.render(graphics, x + 8,y + 40, 146, 12 * (background == null ? 14 : 7), ()->label.renderLeftAligned(graphics,x + 8, y + 40,12,0xFFFFFFFF));
                if (background != null) FactoryGuiGraphics.of(graphics).blit(background, x + 8,y + height - 78,0.0f, 0.0f, 145, 72, 145, 72);
            }
        }

        @Override
        public boolean keyPressed(int i, int j, int k) {
            if (isHoveredOrFocused() && active) {
                if (i == InputConstants.KEY_X){
                    openPackSelectionScreen();
                    return true;
                }
                if (CommonInputs.selected(i)) {
                    tryChangePackState(selectedIndex);
                    playDownSound(Minecraft.getInstance().getSoundManager());
                    return true;
                }
                if (i == 263) {
                    if (selectedIndex == scrolledList.get()) updateScroll(-1,true);
                    setSelectedPack(selectedIndex - 1);
                    LegacyRenderUtil.playSimpleUISound(LegacyRegistries.SCROLL.get(),1.0f);
                } else if (i == 262) {
                    if (selectedIndex == scrolledList.get() + getMaxPacks() - 1) updateScroll(1,true);
                    setSelectedPack(selectedIndex + 1);
                    LegacyRenderUtil.playSimpleUISound(LegacyRegistries.SCROLL.get(),1.0f);
                }
            }
            return super.keyPressed(i, j, k);
        }

        public void setSelectedPack(int index) {
            if (selectedIndex == index) return;
            List<Pack> displayPacks = getDisplayPacks();
            this.selectedIndex = Stocker.cyclic(0,index,displayPacks.size());
            selectedPack = displayPacks.get(selectedIndex);
            scrollableRenderer.scrolled.set(0);
            ResourceLocation background = PackAlbum.Selector.getPackBackground(selectedPack);
            scrollableRenderer.scrolled.max = Math.max(0,labelsCache.apply(selectedPack.getDescription(), 145).getLineCount() - (background == null ? 20 : 7));
            updateTooltip();
        }

        public void tryChangePackState(int index){
            Pack p = getDisplayPacks().get(index);
            if (p.isRequired()) return;
            if (model.selected.contains(p)){
                model.selected.remove(p);
                model.unselected.add(p);
            }else{
                model.unselected.remove(p);
                model.selected.add(0,p);
            }
        }

        public List<String> getSelectedIds(){
            return model.selected.stream().filter(p-> !FactoryAPIPlatform.isPackHidden(p) && !p.isRequired()).map(Pack::getId).collect(Collectors.collectingAndThen(Collectors.toList(), l -> {
                Collections.reverse(l);
                return l;
            }));
        }

        public boolean hasChanged(){
            return !getSelectedIds().equals(globalPacks.get());
        }

        public void applyChanges(){
            if (!hasChanged()) return;
            globalPacks.set(globalPacks.get().withPacks(getSelectedIds()));
            globalPacks.save();
        }

        public void updateModel(){
            model = new PackSelectionModel(()->{},PackAlbum.Selector::getPackIcon, packRepository,r-> {});
        }

        public void openPackSelectionScreen(){
            if (minecraft.screen != null) {
                Screen screen = minecraft.screen;
                Collection<String> packs = packRepository.getSelectedIds();
                packRepository.setSelected(getSelectedIds());
                minecraft.setScreen(new PackSelectionScreen(packRepository, p -> {
                    updateModel();
                    packRepository.setSelected(packs);
                    minecraft.setScreen(screen);
                }, packPath, getMessage()));
            }
        }

        @Override
        public void onClick(double d, double e) {
            if ((Screen.hasShiftDown())) {
                openPackSelectionScreen();
                return;
            }
            int visibleCount = 0;
            for (int index = 0; index < getDisplayPacks().size(); index++) {
                if (visibleCount>=getMaxPacks()) break;
                if (d >= getX() + 20 + 30 * index && e >= getY() +minecraft.font.lineHeight +  3 && d < getX()+minecraft.font.lineHeight + 49 + 30 * index && e < getY() + minecraft.font.lineHeight + 32) {
                    if (selectedIndex == index + scrolledList.get()) tryChangePackState(index + scrolledList.get());
                    setSelectedPack(index + scrolledList.get());
                }
                visibleCount++;
            }
            super.onClick(d, e);
        }

        @Override
        public boolean mouseScrolled(double d, double e/*? if >1.20.1 {*/, double f/*?}*/, double g) {
            if (updateScroll((int) Math.signum(g),false)) return true;
            return super.mouseScrolled(d, e/*? if >1.20.1 {*/, f/*?}*/, g);
        }

        public boolean updateScroll(int i, boolean cyclic){
            if (scrolledList.max > 0) {
                if ((scrolledList.get() <= scrolledList.max && i > 0) || (scrolledList.get() >= 0 && i < 0)) {;
                    return scrolledList.add(i,cyclic) != 0;
                }
            }
            return false;
        }

        protected int getMaxPacks(){
            return (width - 40) / 30;
        }

        @Override
        protected void renderWidget(GuiGraphics guiGraphics, int i, int j, float f) {
            Font font = minecraft.font;
            FactoryGuiGraphics.of(guiGraphics).blitSprite(LegacySprites.PANEL_RECESS,getX() -1,getY()+ font.lineHeight -1 , width + 2,height + 2 - minecraft.font.lineHeight);
            List<Pack> displayPacks = getDisplayPacks();
            int visibleCount = 0;
            FactoryScreenUtil.enableBlend();
            for (int index = 0; index < displayPacks.size(); index++) {
                if (visibleCount>=getMaxPacks()) break;
                FactoryGuiGraphics.of(guiGraphics).blit(PackAlbum.Selector.getPackIcon(displayPacks.get(scrolledList.get() + index)), getX() + 21 + 30 * index,getY() + font.lineHeight + 4,0.0f, 0.0f, 28, 28, 28, 28);
                if (model.selected.contains(displayPacks.get(scrolledList.get() + index)))  FactoryGuiGraphics.of(guiGraphics).blitSprite(LegacySprites.PACK_SELECTED, getX() + 20 + 30 * index,getY() +font.lineHeight + 3,30,30);
                if (scrolledList.get() + index == selectedIndex)
                    FactoryGuiGraphics.of(guiGraphics).blitSprite(LegacySprites.PACK_HIGHLIGHTED, getX() + 20 + 30 * index,getY() +font.lineHeight + 3,30,30);
                visibleCount++;
            }
            FactoryScreenUtil.disableBlend();
            guiGraphics.drawString(font,getMessage(),getX() + 1,getY(),isHoveredOrFocused() ? LegacyRenderUtil.getDefaultTextColor() : CommonColor.INVENTORY_GRAY_TEXT.get(),isHoveredOrFocused());
            if (scrolledList.max > 0){
                if (scrolledList.get() < scrolledList.max) scrollRenderer.renderScroll(guiGraphics, ScreenDirection.RIGHT, getX() + width - 12, getY() + font.lineHeight + (height - font.lineHeight - 11) / 2);
                if (scrolledList.get() > 0) scrollRenderer.renderScroll(guiGraphics,ScreenDirection.LEFT,getX() + 8, getY() + font.lineHeight + (height - font.lineHeight - 11) / 2);
            }
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
            defaultButtonNarrationText(narrationElementOutput);
        }

        @Override
        public @Nullable Component getAction(Context context) {
            return context.actionOfContext(KeyContext.class,k-> k.key() == InputConstants.KEY_X && isFocused() || k.key() == InputConstants.MOUSE_BUTTON_LEFT && isHovered() ? screenComponent : ControlTooltip.getSelectAction(this,context));
        }
    }
}
