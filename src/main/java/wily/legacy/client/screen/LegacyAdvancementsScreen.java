package wily.legacy.client.screen;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.advancements.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientAdvancements;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import wily.factoryapi.FactoryAPI;
import wily.factoryapi.base.Stocker;
import wily.factoryapi.base.client.FactoryGuiGraphics;
import wily.factoryapi.util.FactoryScreenUtil;
import wily.factoryapi.util.PagedList;
import wily.legacy.Legacy4JClient;
import wily.legacy.client.CommonColor;
import wily.legacy.client.ControlType;
import wily.legacy.client.LegacyOptions;
import wily.legacy.mixin.base.client.AbstractWidgetAccessor;import wily.legacy.util.*;
import wily.legacy.client.controller.BindingState;
import wily.legacy.client.controller.ControllerBinding;
import wily.legacy.network.ClientAdvancementsPayload;
import wily.legacy.util.client.LegacyRenderUtil;

import java.util.*;
import java.util.stream.StreamSupport;

import static wily.legacy.client.screen.ControlTooltip.*;

public class LegacyAdvancementsScreen extends PanelVListScreen implements TabList.Access {
    public static final Component TITLE = Component.translatable("gui.advancements");
    protected final Stocker.Sizeable page = new Stocker.Sizeable(0);
    protected final TabList tabList = new TabList(accessor, new PagedList<>(page,10));
    protected final List<DisplayInfo> displayInfos = new ArrayList<>();
    protected boolean showDescription = false;
    protected boolean oldLegacyTooltipsValue;
    public static final List<ResourceLocation> vanillaOrder = List.of(FactoryAPI.createVanillaLocation("story/root"),FactoryAPI.createVanillaLocation("adventure/root"),FactoryAPI.createVanillaLocation("husbandry/root"),FactoryAPI.createVanillaLocation("nether/root"),FactoryAPI.createVanillaLocation("end/root"));

    public LegacyAdvancementsScreen(Screen parent) {
        super(parent,s-> Panel.createPanel(s, p-> p.centeredLeftPos(s), p-> p.centeredTopPos(s) + (((LegacyAdvancementsScreen)s).displayInfos.isEmpty() ? 0 : 18), 450,252),TITLE);
        renderableVLists.clear();
        StreamSupport.stream(getActualAdvancements()./*? if >1.20.1 {*/roots/*?} else {*//*getRoots*//*?}*/().spliterator(),false).sorted(Comparator.comparingInt(n->vanillaOrder.contains(n./*? if >1.20.1 {*/holder().id/*?} else {*//*getId*//*?}*/()) ? vanillaOrder.indexOf(n./*? if >1.20.1 {*/holder().id/*?} else {*//*getId*//*?}*/()): Integer.MAX_VALUE)).forEach(a-> {
            DisplayInfo displayInfo = a./*? if >1.20.1 {*/advancement().display().orElse(null)/*?} else {*//*getDisplay()*//*?}*/;
            if (displayInfo == null) return;

            tabList.addTabButton(43, LegacyTabButton.Type.MIDDLE, LegacyTabButton.iconOf(displayInfo.getIcon()), displayInfo.getTitle(), b -> repositionElements());
            RenderableVList renderableVList = new RenderableVList(this).layoutSpacing(l -> 4).forceWidth(false).cyclic(false);
            renderableVLists.add(renderableVList);
            displayInfos.add(displayInfo);
            getActualAdvancements()./*? if >1.20.1 {*/nodes/*?} else {*//*getAllAdvancements*//*?}*/().stream().filter(n1 -> !n1.equals(a) && n1./*? if >1.20.1 {*/root/*?} else {*//*getRoot*//*?}*/().equals(a)).sorted(Comparator.comparingInt(LegacyAdvancementsScreen::getRootDistance)).forEach(node -> addAdvancementButton(renderableVList, node));
        });
    }

    public static int getRootDistance( /*? if >1.20.1 {*/AdvancementNode/*?} else {*//*Advancement*//*?}*/ advancement) {
        /*? if >1.20.1 {*/AdvancementNode/*?} else {*//*Advancement*//*?}*/ advancement2 = advancement;
        /*? if >1.20.1 {*/AdvancementNode/*?} else {*//*Advancement*//*?}*/ advancement3;
        int i = 0;
        while ((advancement3 = advancement2./*? if >1.20.1 {*/parent/*?} else {*//*getParent*//*?}*/()) != null) {
            advancement2 = advancement3;
            i++;
        }
        return i;
    }

    protected void addAdvancementButton(RenderableVList renderableVList, /*? if >1.20.1 {*/AdvancementNode advancementNode/*?} else {*//*Advancement advancement*//*?}*/){
        //? if >1.20.1 {
        advancementNode.advancement().display().ifPresent(info-> renderableVList.addRenderable(new AdvancementButton(0,0,38,38,advancementNode,info)));
        //?} else {
        /*if (advancement.getDisplay() != null) renderableVList.addRenderable(new AdvancementButton(0,0,38,38,advancement,advancement.getDisplay()));
        *///?}
    }

    @Override
    public TabList getTabList() {
        return tabList;
    }

    @Override
    public RenderableVList getRenderableVList() {
        return getRenderableVLists().get(tabList.selectedIndex);
    }

    public static class AdvancementButton extends AbstractWidget {
        public final ResourceLocation id;
        public final Advancement advancement;
        protected boolean lastUnlocked;
        protected boolean unlocked;
        public final DisplayInfo info;

        public AdvancementButton(int x, int y, int width, int height, /*? if >1.20.1 {*/AdvancementNode/*?} else {*//*Advancement*//*?}*/ advancement, DisplayInfo info) {
            super(x, y, width, height, info.getTitle());
            this.info = info;
            this.id = advancement./*? if >1.20.1 {*/holder().id/*?} else {*//*getId*//*?}*/();
            this.advancement = advancement/*? if >1.20.1 {*/.advancement()/*?}*/;
            update();
        }

        public void update(){
            /*? if >1.20.1 {*/AdvancementHolder/*?} else {*//*Advancement*//*?}*/ a;
            AdvancementProgress p = null;
            lastUnlocked = unlocked;
            unlocked = (a = getAdvancements()/*? if <=1.20.1 {*//*.getAdvancements()*//*?}*/.get(id)) != null && (p = getAdvancements().progress.getOrDefault(a, null)) != null && p.isDone();
            if (lastUnlocked == unlocked && ((AbstractWidgetAccessor)this).getTooltip() != null) return;
            Component progressText = p == null || p .getProgressText() == null ? null : /*? if >1.20.1 {*/p.getProgressText()/*?} else {*//*Component.literal(p.getProgressText())*//*?}*/;
            setTooltip(progressText == null ? Tooltip.create(info.getDescription()) : new MultilineTooltip(List.of(info.getDescription().getVisualOrderText(),progressText.getVisualOrderText())));
        }

        public boolean isUnlocked(){
            return unlocked;
        }

        @Override
        protected void renderWidget(GuiGraphics guiGraphics, int i, int j, float f) {
            if (isFocused()) {
                guiGraphics.pose().pushMatrix();
                guiGraphics.pose().translate(-1.5f, -1.5f);
                FactoryGuiGraphics.of(guiGraphics).blitSprite(LegacySprites.PANEL_HIGHLIGHT, getX(), getY(), 41, 41);
                guiGraphics.pose().popMatrix();
            }
            FactoryScreenUtil.enableBlend();
            if (!isUnlocked()) FactoryGuiGraphics.of(guiGraphics).setBlitColor(1.0f, 1.0f, 1.0f, 0.5f);
            FactoryGuiGraphics.of(guiGraphics).blitSprite(LegacySprites.PANEL, getX(), getY(), getWidth(), getHeight());
            FactoryScreenUtil.disableDepthTest();
            if (!isUnlocked())
                FactoryGuiGraphics.of(guiGraphics).blitSprite(LegacySprites.PADLOCK, getX() + (getWidth() - 32) / 2, getY() + (getHeight() - 32) / 2, 32, 32);
            FactoryScreenUtil.enableDepthTest();
            FactoryScreenUtil.disableBlend();
            FactoryGuiGraphics.of(guiGraphics).setBlitColor(1.0f, 1.0f, 1.0f, 1.0f);
            if (!isUnlocked()) return;
            guiGraphics.pose().pushMatrix();
            guiGraphics.pose().translate(getX() + (getWidth() - 32) / 2f, getY() + (getHeight() - 32) / 2f);
            guiGraphics.pose().scale(2f, 2f);
            guiGraphics.renderFakeItem(info.getIcon(), 0, 0);
            guiGraphics.pose().popMatrix();
        }


        @Override
        protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
            defaultButtonNarrationText(narrationElementOutput);
        }
    }

    @Override
    public void bindingStateTick(BindingState state) {
        if (state.is(ControllerBinding.RIGHT_STICK) && state instanceof BindingState.Axis s && s.pressed && s.canClick()){
            tabList.controlPage(page,s.x < 0 && -s.x > Math.abs(s.y),s.x > 0 && s.x > Math.abs(s.y));
        }
    }

    @Override
    protected void panelInit() {
        addRenderableWidget(tabList);
        super.panelInit();
        addRenderableOnly(tabList::renderSelected);
        addRenderableOnly(((guiGraphics, i, j, f) ->{
            guiGraphics.drawString(font,showDescription && !tabList.tabButtons.isEmpty() ? tabList.tabButtons.get(tabList.selectedIndex).getMessage() : getTitle(),panel.x + (panel.width - font.width(showDescription && !tabList.tabButtons.isEmpty() ? tabList.tabButtons.get(tabList.selectedIndex).getMessage() : getTitle()))/ 2,panel.y + 10, CommonColor.INVENTORY_GRAY_TEXT.get(),false);
            if (!displayInfos.isEmpty()) {
                ResourceLocation background = displayInfos.get(tabList.selectedIndex).getBackground()/*? if >1.20.1 {*/.orElse(null)/*?}*//*? if >=1.21.5 {*/.texturePath()/*?}*/;
                if (background != null) FactoryGuiGraphics.of(guiGraphics).blit(background,panel.x + 14, panel.y + 24,0,0,422,23,16,16);
            }
            LegacyRenderUtil.renderPanelTranslucentRecess(guiGraphics,panel.x + 12, panel.y + 22, 426, 27);
            if (getFocused() instanceof AdvancementButton a) guiGraphics.drawString(font,a.info.getTitle(),panel.x + (panel.width - font.width(a.info.getTitle()))/ 2,panel.y + 32,0xFFFFFFFF);
            FactoryScreenUtil.disableBlend();
            FactoryGuiGraphics.of(guiGraphics).blitSprite(LegacySprites.PANEL_RECESS,panel.x + 12, panel.y + 50, 426, 186);
        }));
        tabList.init(panel.x,panel.y - 37,panel.width,(b,i)->{
            int index = tabList.tabButtons.indexOf(b);
            b.type = LegacyTabButton.Type.bySize(index, 9);
            b.setWidth(45);
            b.offset = (t1) -> new Vec3(0, t1.selected ? 0 : 4.5, 0);
        });
    }

    @Override
    public void renderableVListInit() {
        getRenderableVList().init(panel.x + 17, panel.y + 55, 416,176);
    }

    @Override
    public boolean keyPressed(int i, int j, int k) {
        if (i == InputConstants.KEY_X){
            showDescription = !showDescription;
            return true;
        }
        if (tabList.controlTab(i)) return true;
        if (hasShiftDown()) tabList.controlPage(page,i == 263 , i == 262);
        return super.keyPressed(i, j, k);
    }

    @Override
    public void addControlTooltips(Renderer renderer) {
        super.addControlTooltips(renderer);
        renderer.tooltips.remove(0);
        renderer.
                add(ControlTooltip.EXTRA::get,()-> LegacyComponents.SHOW_DESCRIPTION).
                add(()-> page.max > 0 ? ControlType.getActiveType().isKbm() ? COMPOUND_ICON_FUNCTION.apply(new Icon[]{ControlTooltip.getKeyIcon(InputConstants.KEY_LSHIFT),ControlTooltip.PLUS_ICON,ControlTooltip.getKeyIcon(InputConstants.KEY_LEFT),ControlTooltip.SPACE_ICON,ControlTooltip.getKeyIcon(InputConstants.KEY_RIGHT)}) : ControllerBinding.RIGHT_STICK.getIcon() : null,()->LegacyComponents.PAGE);
    }

    @Override
    public void tick() {
        super.tick();
        if (!getRenderableVLists().isEmpty()) getRenderableVList().renderables.forEach(r-> {
            if (r instanceof AdvancementButton b) b.update();
        });
    }

    @Override
    public void added() {
        super.added();
        oldLegacyTooltipsValue = LegacyOptions.legacyItemTooltips.get();
        LegacyOptions.legacyItemTooltipScaling.set(false);
    }

    @Override
    public void removed() {
        super.removed();
        LegacyOptions.legacyItemTooltipScaling.set(oldLegacyTooltipsValue);
    }

    @Override
    public void renderDefaultBackground(GuiGraphics guiGraphics, int i, int j, float f) {
        LegacyRenderUtil.renderDefaultBackground(accessor, guiGraphics, false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int i, int j, float f) {
        super.render(guiGraphics, i, j, f);
        if (!showDescription) guiGraphics.deferredTooltip = null;
    }

    public static /*? if >1.20.1 {*/AdvancementTree/*?} else {*//*AdvancementList*//*?}*/ getActualAdvancements(){
        return Legacy4JClient.hasModOnServer() ? ClientAdvancementsPayload.advancements : getAdvancements(). /*? if >1.20.1 {*/getTree/*?} else {*//*getAdvancements*//*?}*/();
    }
    public static ClientAdvancements getAdvancements(){
        return Minecraft.getInstance().getConnection().getAdvancements();
    }
}
