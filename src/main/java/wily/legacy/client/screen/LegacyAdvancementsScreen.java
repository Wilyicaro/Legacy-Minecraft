package wily.legacy.client.screen;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.advancements.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.advancements.AdvancementsScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.multiplayer.ClientAdvancements;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec2;
import wily.factoryapi.FactoryAPI;
import wily.factoryapi.base.Stocker;
import wily.factoryapi.base.client.FactoryGuiGraphics;
import wily.factoryapi.util.FactoryScreenUtil;
import wily.factoryapi.util.PagedList;
import wily.legacy.Legacy4JClient;
import wily.legacy.client.CommonColor;
import wily.legacy.client.LegacyOptions;
import wily.legacy.mixin.base.client.AbstractWidgetAccessor;
import wily.legacy.util.*;
import wily.legacy.client.controller.BindingState;
import wily.legacy.client.controller.ControllerBinding;
import wily.legacy.network.ClientAdvancementsPayload;
import wily.legacy.util.client.LegacyFontUtil;
import wily.legacy.util.client.LegacyRenderUtil;

import java.util.*;
import java.util.stream.StreamSupport;

import static wily.legacy.client.screen.ControlTooltip.*;

public class LegacyAdvancementsScreen extends PanelVListScreen implements TabList.Access {
    public static final Component TITLE = Component.translatable("gui.advancements");
    public static final List<ResourceLocation> vanillaOrder = List.of(FactoryAPI.createVanillaLocation("story/root"), FactoryAPI.createVanillaLocation("adventure/root"), FactoryAPI.createVanillaLocation("husbandry/root"), FactoryAPI.createVanillaLocation("nether/root"), FactoryAPI.createVanillaLocation("end/root"));
    protected final Stocker.Sizeable page = new Stocker.Sizeable(0);
    protected final TabList tabList = new TabList(accessor, new PagedList<>(page, this::getMaxTabCount));
    protected final List<DisplayInfo> displayInfos = new ArrayList<>();
    protected final Panel panelRecess;
    protected boolean showDescription = false;
    protected boolean oldLegacyTooltipsValue;

    public LegacyAdvancementsScreen(Screen parent) {
        super(parent, s -> Panel.createPanel(s, p -> p.centeredLeftPos(s), p -> p.centeredTopPos(s) + (((LegacyAdvancementsScreen) s).displayInfos.isEmpty() ? 0 : ((LegacyAdvancementsScreen) s).getTabYOffset()), 450, 252), TITLE);
        renderableVLists.clear();
        StreamSupport.stream(getActualAdvancements().roots().spliterator(), false).sorted(Comparator.comparingInt(n -> vanillaOrder.contains(n.holder().id()) ? vanillaOrder.indexOf(n.holder().id()) : Integer.MAX_VALUE)).forEach(a -> {
            DisplayInfo displayInfo = a.advancement().display().orElse(null);
            if (displayInfo == null) return;

            tabList.add(LegacyTabButton.Type.MIDDLE, LegacyTabButton.iconOf(displayInfo.getIcon()), displayInfo.getTitle(), b -> repositionElements());
            RenderableVList renderableVList = new RenderableVList(this).layoutSpacing(l -> 4).forceWidth(false).cyclic(false);
            renderableVLists.add(renderableVList);
            displayInfos.add(displayInfo);
            getActualAdvancements().nodes().stream().filter(n1 -> !n1.equals(a) && n1.root().equals(a)).sorted(Comparator.comparingInt(LegacyAdvancementsScreen::getRootDistance)).forEach(node -> addAdvancementButton(renderableVList, node));
        });
        panelRecess = Panel.createPanel(this, p -> p.appearance(LegacySprites.PANEL_RECESS, panel.getWidth() - 24, panel.getHeight() - 66), p -> p.pos(panel.getX() + 12, panel.getY() + 50));
    }

    public static int getRootDistance(AdvancementNode advancement) {
        AdvancementNode advancement2 = advancement;
        AdvancementNode advancement3;
        int i = 0;
        while ((advancement3 = advancement2.parent()) != null) {
            advancement2 = advancement3;
            i++;
        }
        return i;
    }

    public static Screen getActualAdvancementsScreenInstance(Screen parent) {
        return LegacyOptions.legacyAdvancements.get() ? new LegacyAdvancementsScreen(parent) : new AdvancementsScreen(getAdvancements(), parent);
    }

    public static AdvancementTree getActualAdvancements() {
        return Legacy4JClient.hasModOnServer() ? ClientAdvancementsPayload.advancements : getAdvancements().getTree();
    }

    public static ClientAdvancements getAdvancements() {
        return Minecraft.getInstance().getConnection().getAdvancements();
    }

    protected void addAdvancementButton(RenderableVList renderableVList, AdvancementNode advancementNode) {
        advancementNode.advancement().display().ifPresent(info -> renderableVList.addRenderable(new AdvancementButton(0, 0, 38, 38, advancementNode, info)));
    }

    @Override
    public TabList getTabList() {
        return tabList;
    }

    @Override
    public int getTabYOffset() {
        return 18;
    }

    @Override
    public RenderableVList getRenderableVList() {
        return getRenderableVLists().get(page.get() * getMaxTabCount() + tabList.getIndex());
    }

    protected int getMaxTabCount() {
        return accessor.getInteger("maxTabCount", 10);
    }

    @Override
    public void bindingStateTick(BindingState state) {
        if (state.is(ControllerBinding.RIGHT_STICK) && state instanceof BindingState.Axis s && s.pressed && s.canClick()) {
            tabList.controlPage(page, s.x < 0 && -s.x > Math.abs(s.y), s.x > 0 && s.x > Math.abs(s.y));
        }
    }

    @Override
    protected void panelInit() {
        addRenderableWidget(tabList);
        super.panelInit();
        panelRecess.init("panelRecess");
        addRenderableOnly(tabList::renderSelected);
        addRenderableOnly(((guiGraphics, i, j, f) -> {
            LegacyFontUtil.applySDFont(b -> guiGraphics.drawString(font, showDescription && !tabList.tabButtons.isEmpty() ? tabList.tabButtons.get(tabList.getIndex()).getMessage() : getTitle(), panel.x + (panel.width - font.width(showDescription && !tabList.tabButtons.isEmpty() ? tabList.tabButtons.get(tabList.getIndex()).getMessage() : getTitle())) / 2, panel.y + 10, CommonColor.INVENTORY_GRAY_TEXT.get(), false));
            if (!displayInfos.isEmpty()) {
                ResourceLocation background = displayInfos.get(tabList.getIndex()).getBackground().orElse(null).texturePath();
                if (background != null)
                    FactoryGuiGraphics.of(guiGraphics).blit(background, panel.x + 14, panel.y + 24, 0, 0, panelRecess.width - 4, 23, 16, 16);
            }
            LegacyRenderUtil.renderPanelTranslucentRecess(guiGraphics, panel.x + 12, panel.y + 22, panelRecess.width, 27);
            if (getFocused() instanceof AdvancementButton a)
                guiGraphics.drawString(font, a.info.getTitle(), panel.x + (panel.width - font.width(a.info.getTitle())) / 2, panel.y + 32, 0xFFFFFFFF);
        }));
        addRenderableOnly(panelRecess);
        tabList.init(panel.x, panel.y - 37, panel.width, 43, (b, i) -> {
            int index = tabList.tabButtons.indexOf(b);
            b.type = LegacyTabButton.Type.bySize(index, getMaxTabCount());
            b.setWidth(accessor.getInteger("tabList.buttonWidth", 45));
            b.offset = (t1) -> new Vec2(0, t1.selected ? 0 : 4.4f);
        });
    }

    @Override
    public void renderableVListInit() {
        getRenderableVList().init(panel.x + 17, panel.y + 55, panelRecess.width - 10, panelRecess.height - 10);
    }

    @Override
    public boolean keyPressed(KeyEvent keyEvent) {
        if (keyEvent.key() == InputConstants.KEY_X) {
            showDescription = !showDescription;
            return true;
        }
        if (tabList.controlTab(keyEvent.key())) return true;
        if (keyEvent.hasShiftDown()) tabList.controlPage(page, keyEvent.isLeft(), keyEvent.isRight());
        return super.keyPressed(keyEvent);
    }

    @Override
    public void addControlTooltips(Renderer renderer) {
        super.addControlTooltips(renderer);
        renderer.tooltips.remove(0);
        renderer.
                add(EXTRA::get, () -> LegacyComponents.SHOW_DESCRIPTION).
                add(CONTROL_PAGE::get, () -> page.max > 0 ? LegacyComponents.PAGE : null);
    }

    @Override
    public void tick() {
        super.tick();
        if (!getRenderableVLists().isEmpty()) getRenderableVList().renderables.forEach(r -> {
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

    public static class AdvancementButton extends AbstractWidget {
        public final ResourceLocation id;
        public final Advancement advancement;
        public final DisplayInfo info;
        protected boolean lastUnlocked;
        protected boolean unlocked;

        public AdvancementButton(int x, int y, int width, int height, AdvancementNode advancement, DisplayInfo info) {
            super(x, y, width, height, info.getTitle());
            this.info = info;
            this.id = advancement.holder().id();
            this.advancement = advancement.advancement();
            update();
        }

        public void update() {
            AdvancementHolder a;
            AdvancementProgress p = null;
            lastUnlocked = unlocked;
            unlocked = (a = getAdvancements().get(id)) != null && (p = getAdvancements().progress.getOrDefault(a, null)) != null && p.isDone();
            if (lastUnlocked == unlocked && ((AbstractWidgetAccessor) this).getTooltip().get() != null) return;
            Component progressText = p == null || p.getProgressText() == null ? null : p.getProgressText();
            setTooltip(progressText == null ? Tooltip.create(info.getDescription()) : new MultilineTooltip(List.of(info.getDescription().getVisualOrderText(), progressText.getVisualOrderText())));
        }

        public boolean isUnlocked() {
            return unlocked;
        }

        @Override
        protected void renderWidget(GuiGraphics guiGraphics, int i, int j, float f) {
            if (isFocused()) {
                guiGraphics.pose().pushMatrix();
                guiGraphics.pose().translate(-1.5f, -1.4f);
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
}
