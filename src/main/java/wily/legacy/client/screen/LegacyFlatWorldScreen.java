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
import wily.legacy.client.ControlType;
import wily.legacy.client.LegacyBiomeOverride;
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
    private final Consumer<FlatLevelGeneratorSettings> applySettings;
    protected final WorldCreationUiState uiState;
    FlatLevelGeneratorSettings generator;
    protected final TabList tabList = new TabList().add(30,0,Component.translatable("legacy.menu.create_flat_world.layers"), b-> rebuildWidgets()).add(30,1,Component.translatable("legacy.menu.create_flat_world.biomes"), b-> rebuildWidgets()).add(30,2,Component.translatable("legacy.menu.create_flat_world.properties"), b-> rebuildWidgets());

    protected final RenderableVList displayLayers = new RenderableVList(accessor).layoutSpacing(l->0);
    protected final RenderableVList displayBiomes = new RenderableVList(accessor).layoutSpacing(l->0);
    protected final RenderableVList displayProperties = new RenderableVList(accessor);
    protected final List<Holder<StructureSet>> structuresOverrides;
    protected LayerButton movingLayer;

    public LegacyFlatWorldScreen(Screen screen, WorldCreationUiState uiState, HolderLookup.RegistryLookup<Biome> biomeGetter, HolderLookup.RegistryLookup<StructureSet> structureGetter, Consumer<FlatLevelGeneratorSettings> consumer, FlatLevelGeneratorSettings flatLevelGeneratorSettings) {
        super(s->Panel.createPanel(s, p-> p.appearance(282,Math.min(s.height - 48,248)) , p-> p.pos((s.width - (p.width + (ScreenUtil.hasTooltipBoxes(UIDefinition.Accessor.of(s)) ? 194 : 0))) / 2, p.centeredTopPos(s))),Component.translatable("createWorld.customize.flat.title"));
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
        renderer.add(()-> movingLayer != null || tabList.selectedTab != 0 || getFocused() == null ? null : ControlType.getActiveType().isKbm() ? ControlTooltip.getKeyIcon(InputConstants.KEY_X) : ControllerBinding.LEFT_BUTTON.bindingState.getIcon(),()-> LegacyComponents.MOVE_LAYER).
                add(()-> movingLayer != null ? null : ControlType.getActiveType().isKbm() ? ControlTooltip.getKeyIcon(InputConstants.KEY_O) : ControllerBinding.UP_BUTTON.bindingState.getIcon(),()-> LegacyComponents.PRESETS).
                addCompound(()-> new ControlTooltip.Icon[]{ControlType.getActiveType().isKbm() ? ControlTooltip.getKeyIcon(InputConstants.KEY_LBRACKET) : ControllerBinding.LEFT_BUMPER.bindingState.getIcon(),ControlTooltip.SPACE_ICON, ControlType.getActiveType().isKbm() ? ControlTooltip.getKeyIcon(InputConstants.KEY_RBRACKET) : ControllerBinding.RIGHT_BUMPER.bindingState.getIcon()},()-> movingLayer != null ? null : LegacyComponents.SELECT_TAB).
                add(()-> movingLayer == null ? null : ControlType.getActiveType().isKbm() ? ControlTooltip.getKeyIcon(InputConstants.KEY_PAGEUP) : ControllerBinding.LEFT_TRIGGER.bindingState.getIcon(),()-> LegacyComponents.PAGE_UP).
                add(()-> movingLayer == null ? null : ControlType.getActiveType().isKbm() ? ControlTooltip.getKeyIcon(InputConstants.KEY_PAGEDOWN) : ControllerBinding.RIGHT_TRIGGER.bindingState.getIcon(),()-> LegacyComponents.PAGE_DOWN).
                addCompound(()-> ControlType.getActiveType().isKbm() ? new ControlTooltip.Icon[]{ControlTooltip.getKeyIcon(InputConstants.KEY_UP),ControlTooltip.SPACE_ICON,ControlTooltip.getKeyIcon(InputConstants.KEY_DOWN)}  : new ControlTooltip.Icon[]{ControllerBinding.LEFT_STICK.bindingState.getIcon()}, ()-> movingLayer == null ? null : LegacyComponents.MOVE_UP_DOWN);
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
        Tooltip t = descr.isEmpty() ? null : new MultilineTooltip(descr,182);
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
                    guiGraphics.pose().pushPose();
                    guiGraphics.pose().translate(getX() + 26, getY() + 5,0);
                    guiGraphics.pose().scale(1.25f,1.25f,1.25f);
                    guiGraphics.renderItem(s,0, 0);
                    guiGraphics.pose().popPose();
                }
                RenderSystem.enableBlend();
                FactoryGuiGraphics.of(guiGraphics).blitSprite(TickBox.SPRITES[isHoveredOrFocused() ? 1 : 0], this.getX() + 6, this.getY() + (height - 12) / 2, 12, 12);
                if (generator.biome == biome) FactoryGuiGraphics.of(guiGraphics).blitSprite(TickBox.TICK, this.getX() + 6, this.getY()  + (height - 12) / 2, 14, 12);
                RenderSystem.disableBlend();
            }
            @Override
            protected void renderScrollingString(GuiGraphics guiGraphics, Font font, int i, int j) {
                int k = this.getX() + 54;
                int l = this.getX() + this.getWidth();
                ScreenUtil.renderScrollingString(guiGraphics, font, this.getMessage(), k, this.getY(), l, this.getY() + this.getHeight(), j,true);
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
        b.setTooltip(new MultilineTooltip(descr,182));
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
            setTooltip(new MultilineTooltip(descr,182));
        }
        @Override
        protected void renderWidget(GuiGraphics guiGraphics, int i, int j, float f) {
            super.renderWidget(guiGraphics, i, j, f);
            guiGraphics.drawString(font,Component.translatable("legacy.menu.create_flat_world.layer_count",flatLayerInfo.getHeight()),getX() + 12, getY() + 1 + (height - font.lineHeight) / 2, 0xFFFFFF);
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(getX() + 39, getY() + 5,0);
            guiGraphics.pose().scale(1.25f,1.25f,1.25f);
            guiGraphics.renderItem(flatLayerInfo.getBlockState().getBlock().asItem().getDefaultInstance(),0, 0);
            guiGraphics.pose().popPose();
        }
        @Override
        protected void renderScrollingString(GuiGraphics guiGraphics, Font font, int i, int j) {
            int k = this.getX() + 67;
            int l = this.getX() + this.getWidth();
            ScreenUtil.renderScrollingString(guiGraphics, font, this.getMessage(), k, this.getY(), l, this.getY() + this.getHeight(), j,true);
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
            minecraft.setScreen(new ConfirmationScreen(LegacyFlatWorldScreen.this,LegacyComponents.LAYER_OPTIONS, LegacyComponents.LAYER_MESSAGE,b->{}){
                @Override
                protected void addButtons() {
                    renderableVList.addRenderable(Button.builder(Component.translatable("legacy.menu.create_flat_world.edit_layer"), b-> {
                        this.minecraft.setScreen(new FlatWorldLayerSelector(LegacyFlatWorldScreen.this, flatLayerInfo, f-> {
                            removeLayer(layerIndex);
                            addLayer(f.getFlatLayerInfo(),layerIndex);
                        }, maxOverworldHeight - allHeight + flatLayerInfo.getHeight(),Component.translatable("legacy.menu.create_flat_world.edit_layer")));
                    }).bounds(panel.x + 15, panel.y + panel.height - 74,200,20).build());
                    renderableVList.addRenderable(Button.builder(Component.translatable("legacy.menu.create_flat_world.add_layer"), b-> {
                        if (allHeight < maxOverworldHeight) this.minecraft.setScreen(new FlatWorldLayerSelector(LegacyFlatWorldScreen.this, f-> addLayer(f.getFlatLayerInfo(),layerIndex), maxOverworldHeight - allHeight,Component.translatable("legacy.menu.create_flat_world.add_layer")));
                        else this.onClose();
                    }).bounds(panel.x + 15, panel.y + panel.height - 52,200,20).build());
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
        for (FlatLayerInfo flatLayerInfo : generator.getLayersInfo()) {
            height += flatLayerInfo.getHeight();
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
            tooltipBoxLabel = tooltip.toCharSequence(minecraft);
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
                if (tooltipBoxLabel != null && getChildAt(i,j).map(g-> g instanceof AbstractWidget w ? w.getTooltip() : null).isEmpty() && (!(getFocused() instanceof AbstractWidget w) || w.getTooltip() == null)) tooltipBoxLabel = null;
                ScreenUtil.renderPointerPanel(guiGraphics,panel.x + panel.width - 2, panel.y + 5,194,panel.height - 10);
                if (tooltipBoxLabel != null) tooltipBoxLabel.forEach(c-> guiGraphics.drawString(font,c,panel.x + panel.width + 3, panel.y + 13 + 12 * tooltipBoxLabel.indexOf(c),0xFFFFFF));
            }
        }));
        addRenderableWidget(tabList);
        super.init();
        tabList.init(panel.x,panel.y - 24, panel.width);
        this.generator.updateLayers();
    }

    @Override
    protected void panelInit() {
        super.panelInit();
        addRenderableOnly(((guiGraphics, i, j, f) -> FactoryGuiGraphics.of(guiGraphics).blitSprite(LegacySprites.PANEL_RECESS, panel.x + 7, panel.y + 7, panel.width - 14, panel.height - 14)));
    }

    @Override
    public void renderableVListInit() {
        getRenderableVList().init(panel.x + 11,panel.y + 11,260, panel.height - 22);
    }

    public RenderableVList getRenderableVList(){
        return getRenderableVLists().get(tabList.selectedTab);
    }

    @Override
    public boolean mouseScrolled(double d, double e/*? if >1.20.1 {*/, double f/*?}*/, double g) {
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

