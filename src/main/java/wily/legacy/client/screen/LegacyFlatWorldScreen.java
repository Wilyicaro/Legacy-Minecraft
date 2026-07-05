package wily.legacy.client.screen;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import net.minecraft.world.level.levelgen.flat.FlatLayerInfo;
import net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorSettings;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import org.jetbrains.annotations.Nullable;
import wily.factoryapi.base.client.FactoryGuiGraphics;
import wily.factoryapi.base.client.UIDefinition;
import wily.factoryapi.base.client.WidgetAccessor;
import wily.factoryapi.util.FactoryScreenUtil;
import wily.legacy.Legacy4J;
import wily.legacy.client.ControlType;
import wily.legacy.client.LegacyBiomeOverride;
import wily.legacy.client.LegacyOptions;
import wily.legacy.client.LegacyTipManager;
import wily.legacy.client.controller.ControllerBinding;
import wily.legacy.util.LegacyComponents;
import wily.legacy.util.LegacySprites;
import wily.legacy.util.ScreenUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public class LegacyFlatWorldScreen extends PanelVListScreen implements ControlTooltip.Event {
    public final int maxOverworldHeight;
    protected List<FormattedCharSequence> tooltipBoxLabel;
    protected ScrollableRenderer scrollableRenderer = new ScrollableRenderer(new LegacyScrollRenderer());
    protected final Panel tooltipBox = Panel.tooltipBoxOf(panel, () -> LegacyOptions.getUIMode().isSD() ? 87 : 194);
    private final Consumer<FlatLevelGeneratorSettings> applySettings;
    protected final WorldCreationUiState uiState;
    FlatLevelGeneratorSettings generator;
    protected final TabList tabList = new TabList(accessor).add(30, LegacyTabButton.Type.LEFT,Component.translatable("legacy.menu.create_flat_world.layers"), b-> rebuildWidgets()).add(30, LegacyTabButton.Type.MIDDLE,Component.translatable("legacy.menu.create_flat_world.biomes"), b-> rebuildWidgets()).add(30, LegacyTabButton.Type.RIGHT,Component.translatable("legacy.menu.create_flat_world.properties"), b-> rebuildWidgets());

    protected final RenderableVList displayLayers = new RenderableVList(accessor).layoutSpacing(l->0);
    protected final RenderableVList displayBiomes = new RenderableVList(accessor).layoutSpacing(l->0);
    protected final RenderableVList displayProperties = new RenderableVList(accessor);
    protected final List<Holder<StructureSet>> structuresOverrides;
    protected LayerButton movingLayer;

    public LegacyFlatWorldScreen(Screen screen, WorldCreationUiState uiState, HolderLookup.RegistryLookup<Biome> biomeGetter, HolderLookup.RegistryLookup<StructureSet> structureGetter, Consumer<FlatLevelGeneratorSettings> consumer, FlatLevelGeneratorSettings flatLevelGeneratorSettings) {
        super(s->Panel.createPanel(s, p-> p.appearance(282,Math.min(s.height - 48,248)) , p-> p.pos(p.centeredLeftPos(s), p.centeredTopPos(s))),Component.translatable("createWorld.customize.flat.title"));
        this.parent = Minecraft.getInstance().screen instanceof WorldMoreOptionsScreen s ? s : screen;
        this.uiState = uiState;
        this.applySettings = consumer;
        this.generator = flatLevelGeneratorSettings;
        maxOverworldHeight = uiState.getSettings().worldgenLoadContext().lookupOrThrow(Registries.DIMENSION_TYPE).get(BuiltinDimensionTypes.OVERWORLD).map(l-> l.value().height()).orElse(384);
        structuresOverrides = new ArrayList<>(generator.structureOverrides().orElse(HolderSet.direct()).stream().toList());
        generator.getLayersInfo().forEach(this::addLayer);
        biomeGetter.listElements().forEach(this::addBiome);
        structureGetter.listElements().forEach(this::addStructure);
        renderableVLists.clear();
        renderableVLists.add(displayLayers);
        renderableVLists.add(displayBiomes);
        renderableVLists.add(displayProperties);
        displayProperties.addRenderable(new TickBox(0,0,260,12, generator.decoration, b-> LegacyComponents.DECORATIONS, b-> null, b-> generator.decoration = b.selected));
        displayProperties.addRenderable(new TickBox(0,0,260,12, generator.addLakes, b-> LegacyComponents.LAVA_LAKES, b-> null, b-> generator.addLakes = b.selected));
    }

    @Override
    public void addControlTooltips(ControlTooltip.Renderer renderer) {
        super.addControlTooltips(renderer);
        renderer.add(()-> movingLayer != null || tabList.selectedTab != 0 || getFocused() == null ? null : ControlType.getActiveType().isKbm() ? ControlTooltip.getKeyIcon(InputConstants.KEY_X) : ControllerBinding.LEFT_BUTTON.getIcon(),()-> LegacyComponents.MOVE_LAYER).
                add(()-> movingLayer != null ? null : ControlType.getActiveType().isKbm() ? ControlTooltip.getKeyIcon(InputConstants.KEY_O) : ControllerBinding.UP_BUTTON.getIcon(),()-> LegacyComponents.PRESETS).
                addCompound(()-> new ControlTooltip.Icon[]{ControlType.getActiveType().isKbm() ? ControlTooltip.getKeyIcon(InputConstants.KEY_LBRACKET) : ControllerBinding.LEFT_BUMPER.getIcon(),ControlTooltip.SPACE_ICON, ControlType.getActiveType().isKbm() ? ControlTooltip.getKeyIcon(InputConstants.KEY_RBRACKET) : ControllerBinding.RIGHT_BUMPER.getIcon()},()-> movingLayer != null ? null : LegacyComponents.SELECT_TAB).
                add(()-> movingLayer == null ? null : ControlType.getActiveType().isKbm() ? ControlTooltip.getKeyIcon(InputConstants.KEY_PAGEUP) : ControllerBinding.LEFT_TRIGGER.getIcon(),()-> LegacyComponents.PAGE_UP).
                add(()-> movingLayer == null ? null : ControlType.getActiveType().isKbm() ? ControlTooltip.getKeyIcon(InputConstants.KEY_PAGEDOWN) : ControllerBinding.RIGHT_TRIGGER.getIcon(),()-> LegacyComponents.PAGE_DOWN).
                addCompound(()-> ControlType.getActiveType().isKbm() ? new ControlTooltip.Icon[]{ControlTooltip.getKeyIcon(InputConstants.KEY_UP),ControlTooltip.SPACE_ICON,ControlTooltip.getKeyIcon(InputConstants.KEY_DOWN)}  : new ControlTooltip.Icon[]{ControllerBinding.LEFT_STICK.getIcon()}, ()-> movingLayer == null ? null : LegacyComponents.MOVE_UP_DOWN);
    }

    private int tooltipWidth() {
        return tooltipBox.getWidth() - 10;
    }

    private MultilineTooltip tooltip(List<Component> lines) {
        return new MultilineTooltip(lines, tooltipWidth());
    }

    private int tooltipLineHeight() {
        return LegacyOptions.getUIMode().isSD() ? 8 : 12;
    }

    private int tooltipContentPadding() {
        return LegacyOptions.getUIMode().isSD() ? 20 : 44;
    }

    public void addStructure(Holder.Reference<StructureSet> structure){
        List<Component> descr = new ArrayList<>();
        String nameKey = "structure."+structure.key().location().toLanguageKey();
        String descriptionKey = nameKey+".description";
        if (LegacyTipManager.hasTip(nameKey)) descr.add(Component.translatable(nameKey));
        if (LegacyTipManager.hasTip(descriptionKey)){
            descr.add(ControlTooltip.SPACE);
            descr.add(Component.translatable(descriptionKey));
        }
        Tooltip t = descr.isEmpty() ? null : tooltip(descr);
        displayProperties.addRenderable(new TickBox(0,0,260,12,structuresOverrides.contains(structure), b-> descr.isEmpty() ? Component.translatable(nameKey) : descr.get(0), b-> t, b-> {
            if (b.selected) structuresOverrides.add(structure);
            else structuresOverrides.remove(structure);
        }));
    }

    public void addBiome(Holder.Reference<Biome> biome){
        AbstractButton b;
        displayBiomes.addRenderable(b = new AbstractButton(0,0,260,30, Component.translatable("biome."+biome.key().location().toLanguageKey())) {
            @Override
            public void onPress() {
                generator.biome = biome;
            }

            @Override
            protected void renderWidget(GuiGraphics guiGraphics, int i, int j, float f) {
                super.renderWidget(guiGraphics, i, j, f);
                ItemStack s = LegacyBiomeOverride.getOrDefault(biome.unwrapKey()).icon();
                if (!s.isEmpty()){
                    renderItemIcon(this, guiGraphics, s, "biomeIcon", 26);
                }
                FactoryScreenUtil.enableBlend();
                int tickHeight = TickBox.getDefaultHeight();
                FactoryGuiGraphics.of(guiGraphics).blitSprite(TickBox.SPRITES[isHoveredOrFocused() ? 1 : 0], this.getX() + 6, this.getY() + (height - tickHeight) / 2, tickHeight, tickHeight);
                if (generator.biome == biome) {
                    if (LegacyOptions.getUIMode().isSD())
                        FactoryGuiGraphics.of(guiGraphics).blitSprite(LegacySprites.SMALL_TICK, this.getX() + 6, this.getY() + (height - 9) / 2, 11, 9);
                    else FactoryGuiGraphics.of(guiGraphics).blitSprite(TickBox.TICK, this.getX() + 6, this.getY()  + (height - 12) / 2, 14, 12);
                }
                FactoryScreenUtil.disableBlend();
            }
            @Override
            protected void renderScrollingString(GuiGraphics guiGraphics, Font font, int i, int j) {
                int k = this.getX() + getListInteger("biomeMessage.xOffset", 54);
                int l = this.getX() + this.getWidth();
                ScreenUtil.applySDFont(ignored -> ScreenUtil.renderScrollingString(guiGraphics, font, this.getMessage(), k, this.getY(), l, this.getY() + this.getHeight(), j,true));
            }
            @Override
            protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
                defaultButtonNarrationText(narrationElementOutput);
            }
        });
        List<Component> descr = new ArrayList<>();
        descr.add(b.getMessage());
        String descriptionKey = "biome."+biome.key().location().toLanguageKey()+".description";
        if (LegacyTipManager.hasTip(descriptionKey)){
            descr.add(ControlTooltip.SPACE);
            descr.add(Component.translatable(descriptionKey));
        }
        b.setTooltip(tooltip(descr));
    }
    public void addLayer(FlatLayerInfo flatLayerInfo){
        addLayer(flatLayerInfo,0);
    }
    public void addLayer(FlatLayerInfo flatLayerInfo, int index){
        displayLayers.renderables.add(index,new LayerButton(0,0,270,30,flatLayerInfo));
    }
    public class LayerButton extends AbstractButton implements ControlTooltip.ActionHolder {
        public final FlatLayerInfo flatLayerInfo;
        public LayerButton(int i, int j, int k, int l, FlatLayerInfo flatLayerInfo) {
            super(i, j, k, l, flatLayerInfo.getBlockState().getBlock().getName());
            this.flatLayerInfo = flatLayerInfo;
            ItemStack s = flatLayerInfo.getBlockState().getBlock().asItem().getDefaultInstance();
            List<Component> descr = new ArrayList<>();
            descr.add(getMessage());
            if (LegacyTipManager.hasTip(s)){
                descr.add(ControlTooltip.SPACE);
                descr.add(LegacyTipManager.getTipComponent(s));
            }
            setTooltip(tooltip(descr));
        }
        @Override
        protected void renderWidget(GuiGraphics guiGraphics, int i, int j, float f) {
            super.renderWidget(guiGraphics, i, j, f);
            ScreenUtil.applySDFont(ignored -> guiGraphics.drawString(font,Component.translatable("legacy.menu.create_flat_world.layer_count",flatLayerInfo.getHeight()),getX() + getListInteger("layerCount.xOffset", 12), getY() + 1 + (height - font.lineHeight) / 2, ScreenUtil.getDefaultTextColor(!isHoveredOrFocused())));
            renderItemIcon(this, guiGraphics, flatLayerInfo.getBlockState().getBlock().asItem().getDefaultInstance(), "layerIcon", 39);
        }
        @Override
        protected void renderScrollingString(GuiGraphics guiGraphics, Font font, int i, int j) {
            int k = this.getX() + getListInteger("layerMessage.xOffset", 67);
            int l = this.getX() + this.getWidth();
            ScreenUtil.applySDFont(ignored -> ScreenUtil.renderScrollingString(guiGraphics, font, this.getMessage(), k, this.getY(), l, this.getY() + this.getHeight(), j,true));
        }

        @Override
        public void setFocused(boolean bl) {
            if (bl && movingLayer != null && movingLayer != this) return;
            super.setFocused(bl);
        }

        @Override
        public boolean keyPressed(int i, int j, int k) {
            if (i == InputConstants.KEY_X){
                movingLayer = this;
                return true;
            }
            return super.keyPressed(i, j, k);
        }

        @Override
        public void onPress() {
            if (movingLayer != null){
                if (isFocused()) movingLayer = null;
                return;
            }
            int allHeight = getAllLayersHeight();
            int layerIndex = displayLayers.renderables.indexOf(this);
            minecraft.setScreen(new ConfirmationScreen(LegacyFlatWorldScreen.this, ConfirmationScreen::getPanelWidth, () -> LegacyOptions.getUIMode().isSD() ? 87 : 120, LegacyComponents.LAYER_OPTIONS, LegacyComponents.LAYER_MESSAGE,b->{}){
                @Override
                protected void addButtons() {
                    renderableVList.addRenderable(Button.builder(Component.translatable("legacy.menu.create_flat_world.edit_layer"), b-> {
                        this.minecraft.setScreen(new FlatWorldLayerSelector(LegacyFlatWorldScreen.this, flatLayerInfo, f-> {
                            removeLayer(layerIndex);
                            addLayer(f.getFlatLayerInfo(),layerIndex);
                        }, maxOverworldHeight - allHeight + flatLayerInfo.getHeight(),Component.translatable("legacy.menu.create_flat_world.edit_layer")));
                    }).bounds(panel.x + 15, panel.y + panel.height - 74,200,20).build());
                    Button addButton = Button.builder(Component.translatable("legacy.menu.create_flat_world.add_layer"), b-> this.minecraft.setScreen(new FlatWorldLayerSelector(LegacyFlatWorldScreen.this, f-> addLayer(f.getFlatLayerInfo(),layerIndex), maxOverworldHeight - allHeight,Component.translatable("legacy.menu.create_flat_world.add_layer")))).bounds(panel.x + 15, panel.y + panel.height - 52,200,20).build();
                    if (allHeight >= maxOverworldHeight) addButton.active = false;
                    renderableVList.addRenderable(addButton);
                    renderableVList.addRenderable(Button.builder(Component.translatable("legacy.menu.create_flat_world.delete_layer"),b-> {
                        removeLayer(layerIndex);
                        this.onClose();
                    }).bounds(panel.x + 15, panel.y + panel.height - 30,200,20).build());
                }
            });
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
            defaultButtonNarrationText(narrationElementOutput);
        }
        @Override
        public @Nullable Component getAction(Context context) {
            return context.actionOfContext(KeyContext.class, c-> c.key() == InputConstants.KEY_RETURN ? movingLayer != null ? LegacyComponents.PLACE : LegacyComponents.LAYER_OPTIONS : null);
        }
    }

    public int getAllLayersHeight(){
        int height = 0;
        for (Renderable renderable : displayLayers.renderables) {
            if (renderable instanceof LayerButton layerButton) height += layerButton.flatLayerInfo.getHeight();
        }
        return height;
    }

    public void removeLayer(int index){
        displayLayers.renderables.remove(index);
    }

    public void switchLayers(AbstractButton selected, AbstractButton aimPlace){
        int selectedIndex = displayLayers.renderables.indexOf(selected);
        int aimIndex = displayLayers.renderables.indexOf(aimPlace);
        displayLayers.renderables.set(aimIndex,selected);
        displayLayers.renderables.set(selectedIndex,aimPlace);
        repositionElements();
        ScreenUtil.playSimpleUISound(SoundEvents.UI_BUTTON_CLICK.value(),1.0f);
    }

    @Override
    public void setTooltipForNextRenderPass(Tooltip tooltip, ClientTooltipPositioner clientTooltipPositioner, boolean bl) {
        if (ScreenUtil.hasTooltipBoxes(accessor))
            ScreenUtil.applySDFont(ignored -> tooltipBoxLabel = tooltip.toCharSequence(minecraft));
        else super.setTooltipForNextRenderPass(tooltip, clientTooltipPositioner, bl);
    }

    public FlatLevelGeneratorSettings settings() {
        return this.generator;
    }

    public void setPreset(FlatLevelGeneratorSettings flatLevelGeneratorSettings) {
        generator = flatLevelGeneratorSettings;
        displayLayers.renderables.clear();
        generator.getLayersInfo().forEach(this::addLayer);
    }

    @Override
    public void renderDefaultBackground(GuiGraphics guiGraphics, int i, int j, float f) {
        ScreenUtil.renderDefaultBackground(accessor, guiGraphics, false);
    }

    @Override
    protected void init() {
        if (movingLayer != null && tabList.selectedTab != 0) tabList.selectedTab = 0;
        addRenderableOnly(((guiGraphics, i, j, f) -> {
            if (ScreenUtil.hasTooltipBoxes(accessor)) {
                if (tooltipBoxLabel != null && getChildAt(i,j).map(g-> g instanceof AbstractWidget w ? w.getTooltip() : null).isEmpty() && (!(getFocused() instanceof AbstractWidget w) || w.getTooltip() == null)) {
                    tooltipBoxLabel = null;
                    scrollableRenderer.scrolled.set(0);
                }
                tooltipBox.render(guiGraphics, i, j, f);
                if (tooltipBoxLabel != null) {
                    int lineHeight = tooltipLineHeight();
                    int visibleHeight = tooltipBox.getHeight() - tooltipContentPadding();
                    scrollableRenderer.lineHeight = lineHeight;
                    scrollableRenderer.scrolled.max = Math.max(0, tooltipBoxLabel.size() - visibleHeight / lineHeight);
                    ScreenUtil.applySDFont(ignored -> scrollableRenderer.render(guiGraphics, panel.x + panel.width + 3, panel.y + 13, tooltipWidth(), visibleHeight, () -> {
                        for (int line = 0; line < tooltipBoxLabel.size(); line++) {
                            guiGraphics.drawString(font, tooltipBoxLabel.get(line), panel.x + panel.width + 3, panel.y + 13 + line * lineHeight, 0xFFFFFF);
                        }
                    }));
                }
            }
        }));
        addRenderableWidget(tabList);
        super.init();
        addRenderableOnly(tabList::renderSelected);
        tabList.init(panel.x, panel.y - 24, panel.width, 30);
        this.generator.updateLayers();
    }

    @Override
    protected void panelInit() {
        super.panelInit();
        if (ScreenUtil.hasTooltipBoxes(accessor)) tooltipBox.init();
        addRenderableOnly(((guiGraphics, i, j, f) -> ScreenUtil.renderPanelRecess(accessor, guiGraphics, "panelRecess", panel.x + 7, panel.y + 7, panel.width - 14, panel.height - 14)));
    }

    @Override
    public void renderableVListInit() {
        getRenderableVList().init(
                accessor.getInteger("renderableVList.x", panel.x + 11),
                accessor.getInteger("renderableVList.y", panel.y + 11),
                accessor.getInteger("renderableVList.width", panel.width - 22),
                accessor.getInteger("renderableVList.height", panel.height - 22));
    }

    @Override
    public void initRenderableVListEntry(RenderableVList renderableVList, Renderable renderable) {
        if (renderable instanceof AbstractWidget widget) {
            //? if <=1.20.1 {
            /*((WidgetAccessor) widget).setHeight(accessor.getInteger("buttonsHeight", 30));
            *///?} else {
            widget.setHeight(accessor.getInteger("buttonsHeight", 30));
            //?}
        }
    }

    private int getListInteger(String key, int fallback) {
        String name = getRenderableVList().name == null ? "renderableVList" : getRenderableVList().name;
        return accessor.getInteger(name + "." + key, fallback);
    }

    private float getListFloat(String key, float fallback) {
        String name = getRenderableVList().name == null ? "renderableVList" : getRenderableVList().name;
        return accessor.getFloat(name + "." + key, fallback);
    }

    private void renderItemIcon(AbstractWidget widget, GuiGraphics guiGraphics, ItemStack stack, String key, int fallbackX) {
        guiGraphics.pose().pushPose();
        float scale = getListFloat(key + ".scale", 1.25f);
        int iconHeight = Math.round(16 * scale);
        int x = widget.getX() + getListInteger(key + ".x", fallbackX);
        int y = widget.getY() + getListInteger("buttonItem.y", (widget.getHeight() - iconHeight) / 2);
        guiGraphics.pose().translate(x, y,0);
        guiGraphics.pose().scale(scale, scale, scale);
        guiGraphics.renderItem(stack,0, 0);
        guiGraphics.pose().popPose();
    }

    public RenderableVList getRenderableVList(){
        return getRenderableVLists().get(tabList.selectedTab);
    }

    @Override
    public boolean mouseScrolled(double d, double e/*? if >1.20.1 {*/, double f/*?}*/, double g) {
        if (tooltipBox.isHovered(d, e) && scrollableRenderer.mouseScrolled(g)) return true;
        getRenderableVList().mouseScrolled(g);
        return super.mouseScrolled(d, e/*? if >1.20.1 {*/, f/*?}*/, g);
    }

    @Override
    public void setFocused(@Nullable GuiEventListener guiEventListener) {
        super.setFocused(guiEventListener);
        if (movingLayer != null && getFocused() instanceof AbstractButton b2 && displayLayers.renderables.contains(b2) && getFocused() != movingLayer) {
            super.setFocused(movingLayer);
            switchLayers(movingLayer, b2);
        }
    }

    @Override
    public boolean keyPressed(int i, int j, int k) {
        if (movingLayer == null) {
            if (tabList.controlTab(i)) return true;
            if (i == InputConstants.KEY_O)
                minecraft.setScreen(new LegacyFlatPresetsScreen(this, uiState.getSettings().worldgenLoadContext().lookupOrThrow(Registries.FLAT_LEVEL_GENERATOR_PRESET), uiState.getSettings().dataConfiguration().enabledFeatures(), f -> setPreset(f.value().settings())));
        }
        return super.keyPressed(i, j, k);
    }

    @Override
    public void onClose() {
        super.onClose();
        this.generator.getLayersInfo().clear();
        displayLayers.renderables.forEach(r->{
            if (r instanceof LayerButton l) this.generator.getLayersInfo().add(0,l.flatLayerInfo);
        });
        this.generator.updateLayers();
        applySettings.accept(generator);
        generator.structureOverrides = Optional.of(HolderSet.direct(structuresOverrides));
    }

}

