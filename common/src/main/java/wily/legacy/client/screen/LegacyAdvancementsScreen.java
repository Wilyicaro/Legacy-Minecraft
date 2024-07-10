package wily.legacy.client.screen;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.advancements.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.minecraft.client.multiplayer.ClientAdvancements;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import wily.legacy.Legacy4JClient;
import wily.legacy.client.CommonColor;
import wily.legacy.client.ControlType;
import wily.legacy.util.Offset;
import wily.legacy.client.controller.BindingState;
import wily.legacy.client.controller.ControllerBinding;
import wily.legacy.network.ClientAdvancementsPacket;
import wily.legacy.util.LegacySprites;
import wily.legacy.util.PagedList;
import wily.legacy.util.ScreenUtil;
import wily.legacy.util.Stocker;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.StreamSupport;

import static wily.legacy.client.screen.ControlTooltip.*;

public class LegacyAdvancementsScreen extends PanelBackgroundScreen{
    public static final Component TITLE = Component.translatable("gui.advancements");
    protected final Stocker.Sizeable page = new Stocker.Sizeable(0);
    protected final TabList tabList = new TabList(new PagedList<>(page,10));
    protected final Map<AdvancementNode,RenderableVList> renderableVLists = new LinkedHashMap<>();
    protected AdvancementNode selectedRoot;
    protected AdvancementNode selectedAdvancement;
    private final ClientAdvancements advancements;
    protected boolean showDescription = false;
    protected boolean oldLegacyTooltipsValue;
    public static final List<ResourceLocation> vanillaOrder = List.of(new ResourceLocation("story/root"),new ResourceLocation("adventure/root"),new ResourceLocation("husbandry/root"),new ResourceLocation("nether/root"),new ResourceLocation("end/root"));

    public LegacyAdvancementsScreen(Screen parent, ClientAdvancements advancements) {
        super(449,252,TITLE);
        this.parent = parent;
        this.advancements = advancements;
        StreamSupport.stream(getAdvancements().roots().spliterator(),false).sorted(Comparator.comparingInt(n->vanillaOrder.contains(n.holder().id()) ? vanillaOrder.indexOf(n.holder().id()): Integer.MAX_VALUE)).forEach(n-> n.advancement().display().ifPresent( d-> {
            tabList.addTabButton(43,0, LegacyTabButton.iconOf(d.getIcon()),d.getTitle(),b-> {
                this.selectedRoot = n;
                selectedAdvancement = null;
                repositionElements();
            });
            renderableVLists.put(n, new RenderableVList().layoutSpacing(l->4));
            renderableVLists.get(n).forceWidth = false;
        }));
        tabList.resetSelectedTab();
        getAdvancements().nodes().stream().filter(n-> !n.advancement().isRoot()).sorted(Comparator.comparingInt(LegacyAdvancementsScreen::getRootDistance)).forEach(node-> addAdvancementButton(node.root(),node));
    }

    public static int getRootDistance(AdvancementNode advancementNode) {
        AdvancementNode advancementNode2 = advancementNode;
        AdvancementNode advancementNode3;
        int i = 0;
        while ((advancementNode3 = advancementNode2.parent()) != null) {
            advancementNode2 = advancementNode3;
            i++;
        }
        return i;
    }
    protected void addAdvancementButton(AdvancementNode root, AdvancementNode node){
        node.advancement().display().ifPresent(info-> {
            AbstractWidget w;
            AdvancementHolder h;
            AdvancementProgress p = null;
            boolean unlocked = (h = advancements.get(node.holder().id())) != null && (p = advancements.progress.getOrDefault(h, null)) != null && p.isDone();

            renderableVLists.get(root).addRenderable(w = new AbstractWidget(0, 0, 38, 38, info.getTitle()) {

                @Override
                protected void renderWidget(GuiGraphics guiGraphics, int i, int j, float f) {
                    if (isFocused()) {
                        selectedAdvancement = node;
                        guiGraphics.pose().pushPose();
                        guiGraphics.pose().translate(-1.5f, -1.5f, 0);
                        guiGraphics.blitSprite(LegacySprites.PANEL_HIGHLIGHT, getX(), getY(), 41, 41);
                        guiGraphics.pose().popPose();
                    }
                    RenderSystem.enableBlend();
                    if (!unlocked) guiGraphics.setColor(1.0f, 1.0f, 1.0f, 0.5f);
                    guiGraphics.blitSprite(LegacySprites.PANEL, getX(), getY(), getWidth(), getHeight());
                    RenderSystem.disableDepthTest();
                    if (!unlocked)
                        guiGraphics.blitSprite(LegacySprites.PADLOCK, getX() + (getWidth() - 32) / 2, getY() + (getHeight() - 32) / 2, 32, 32);
                    RenderSystem.enableDepthTest();
                    RenderSystem.disableBlend();
                    guiGraphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);
                    if (!unlocked) return;
                    guiGraphics.pose().pushPose();
                    guiGraphics.pose().translate(getX() + (getWidth() - 32) / 2f, getY() + (getHeight() - 32) / 2f, 0);
                    guiGraphics.pose().scale(2f, 2f, 2f);
                    guiGraphics.renderFakeItem(info.getIcon(), 0, 0);
                    guiGraphics.pose().popPose();
                }


                @Override
                protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
                    defaultButtonNarrationText(narrationElementOutput);
                }
            });
            Component progressText = p == null ? null : p.getProgressText();
            w.setTooltip(progressText == null ? Tooltip.create(info.getDescription()) : new MultilineTooltip(List.of(info.getDescription().getVisualOrderText(),progressText.getVisualOrderText())));
        });
    }
    @Override
    public void bindingStateTick(BindingState state) {
        if (state.is(ControllerBinding.RIGHT_STICK) && state instanceof BindingState.Axis s && s.pressed && s.canClick()){
            tabList.controlPage(page,s.x < 0 && -s.x > Math.abs(s.y),s.x > 0 && s.x > Math.abs(s.y));
        }
    }

    @Override
    protected void init() {
        addRenderableWidget(tabList);
        super.init();
        panel.y +=18;
        addRenderableOnly(((guiGraphics, i, j, f) ->{
            guiGraphics.drawString(font,showDescription ? tabList.tabButtons.get(tabList.selectedTab).getMessage() : getTitle(),panel.x + (panel.width - font.width(showDescription ? tabList.tabButtons.get(tabList.selectedTab).getMessage() : getTitle()))/ 2,panel.y + 10, CommonColor.INVENTORY_GRAY_TEXT.get(),false);
            if (selectedRoot != null) selectedRoot.advancement().display().map(DisplayInfo::getBackground).ifPresent(b -> guiGraphics.blit(b,panel.x + 14, panel.y + 24,0,0,422,23,16,16));
            ScreenUtil.renderPanelTranslucentRecess(guiGraphics,panel.x + 12, panel.y + 22, 426, 27);
            if (selectedAdvancement != null) selectedAdvancement.advancement().display().ifPresent(info-> guiGraphics.drawString(font,info.getTitle(),panel.x + (panel.width - font.width(info.getTitle()))/ 2,panel.y + 32,0xFFFFFF));
            RenderSystem.disableBlend();
            guiGraphics.blitSprite(LegacySprites.PANEL_RECESS,panel.x + 12, panel.y + 50, 426, 186);
        }));
        tabList.init(panel.x,panel.y - 37,panel.width,(b,i)->{
            int index = tabList.tabButtons.indexOf(b);
            b.type = index == 0 ? 0 : index >= 9 ? 2 : 1;
            b.setWidth(45);
            b.offset = (t1) -> new Offset(-1 * tabList.tabButtons.indexOf(b), t1.selected ? 0 : 4.5, 0);
        });
        if (renderableVLists.containsKey(selectedRoot)) renderableVLists.get(selectedRoot).init(this,panel.x + 17, panel.y + 55, 420,196);
    }

    @Override
    public boolean keyPressed(int i, int j, int k) {
        if (i == InputConstants.KEY_X){
            showDescription = !showDescription;
            return true;
        }
        if (selectedRoot != null && renderableVLists.get(selectedRoot).keyPressed(i,false)) return true;
        if (tabList.controlTab(i)) return true;
        if (hasShiftDown()) tabList.controlPage(page,i == 263 , i == 262);
        return super.keyPressed(i, j, k);
    }

    @Override
    public boolean mouseScrolled(double d, double e, double f, double g) {
        if (selectedRoot != null) renderableVLists.get(selectedRoot).mouseScrolled(g);
        return super.mouseScrolled(d, e, f, g);
    }

    @Override
    public void addControlTooltips(Renderer renderer) {
        super.addControlTooltips(renderer);
        renderer.tooltips.remove(0);
        renderer.
                add(()-> ControlType.getActiveType().isKbm() ? getKeyIcon(InputConstants.KEY_X) : ControllerBinding.LEFT_BUTTON.bindingState.getIcon(),()-> getAction( "legacy.action.show_description")).
                add(()-> page.max > 0 ? ControlType.getActiveType().isKbm() ? COMPOUND_ICON_FUNCTION.apply(new Icon[]{ControlTooltip.getKeyIcon(InputConstants.KEY_LSHIFT),ControlTooltip.PLUS_ICON,ControlTooltip.getKeyIcon(InputConstants.KEY_LEFT),ControlTooltip.SPACE_ICON,ControlTooltip.getKeyIcon(InputConstants.KEY_RIGHT)}) : ControllerBinding.RIGHT_STICK.bindingState.getIcon() : null,()->getAction("legacy.action.page"));
    }

    @Override
    public void added() {
        super.added();
        oldLegacyTooltipsValue =ScreenUtil.getLegacyOptions().legacyItemTooltips().get();
        ScreenUtil.getLegacyOptions().legacyItemTooltips().set(false);
    }

    @Override
    public void removed() {
        super.removed();
        ScreenUtil.getLegacyOptions().legacyItemTooltips().set(oldLegacyTooltipsValue);
    }
    @Override
    public void setTooltipForNextRenderPass(List<FormattedCharSequence> list, ClientTooltipPositioner clientTooltipPositioner, boolean bl) {
        if (!showDescription) return;
        super.setTooltipForNextRenderPass(list, clientTooltipPositioner, bl);
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int i, int j, float f) {
        renderTransparentBackground(guiGraphics);
    }

    public AdvancementTree getAdvancements(){
        return Legacy4JClient.isModEnabledOnServer() ? ClientAdvancementsPacket.advancementTree : advancements.getTree();
    }
}
