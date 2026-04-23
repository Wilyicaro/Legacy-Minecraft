package wily.legacy.client.screen;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
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
import wily.factoryapi.base.client.UIAccessor;
import wily.factoryapi.util.FactoryScreenUtil;
import wily.legacy.Legacy4J;
import wily.legacy.client.*;
import wily.legacy.client.controller.ControllerBinding;
import wily.legacy.mixin.base.client.AbstractWidgetAccessor;
import wily.legacy.util.LegacyComponents;
import wily.legacy.util.LegacySprites;
import wily.legacy.util.client.LegacyFontUtil;
import wily.legacy.util.client.LegacyRenderUtil;
import wily.legacy.util.client.LegacySoundUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public class LegacyFlatWorldScreen extends PanelVListScreen implements ControlTooltip.Event {
    public final int maxOverworldHeight;
    protected final WorldCreationUiState uiState;
    protected final TabList tabList = new TabList(accessor).add(LegacyTabButton.Type.LEFT, Component.translatable("legacy.menu.create_flat_world.layers"), b -> rebuildWidgets()).add(LegacyTabButton.Type.MIDDLE, Component.translatable("legacy.menu.create_flat_world.biomes"), b -> rebuildWidgets()).add(LegacyTabButton.Type.RIGHT, Component.translatable("legacy.menu.create_flat_world.properties"), b -> rebuildWidgets());
    protected final RenderableVList displayLayers = new RenderableVList(accessor).layoutSpacing(l -> 0);
    protected final RenderableVList displayBiomes = new RenderableVList(accessor).layoutSpacing(l -> 0);
    protected final RenderableVList displayProperties = new RenderableVList(accessor);
    protected final List<Holder<StructureSet>> structuresOverrides;
    private final Consumer<FlatLevelGeneratorSettings> applySettings;
    protected final Panel panelRecess;
    protected final Panel tooltipBox = Panel.tooltipBoxOf(panel, ()-> LegacyOptions.getUIMode().isSD() ? 87 : 194);
    protected ScrollableRenderer scrollableRenderer = new ScrollableRenderer(new LegacyScrollRenderer());
    protected LayerButton movingLayer;
    FlatLevelGeneratorSettings generator;

    public LegacyFlatWorldScreen(Screen screen, WorldCreationUiState uiState, HolderLookup.RegistryLookup<Biome> biomeGetter, HolderLookup.RegistryLookup<StructureSet> structureGetter, Consumer<FlatLevelGeneratorSettings> consumer, FlatLevelGeneratorSettings flatLevelGeneratorSettings) {
        super(s -> Panel.createPanel(s, p -> p.appearance(282, Math.min(s.height - 48, 248)), p -> p.pos(p.centeredLeftPos(s), p.centeredTopPos(s))), Component.translatable("createWorld.customize.flat.title"));
        panelRecess = Panel.createPanel(this, p -> p.appearance(LegacySprites.PANEL_RECESS, panel.width - 14, panel.height - 14), p -> p.pos(panel.x + 7, panel.y + 7));
        this.parent = Minecraft.getInstance().screen instanceof WorldMoreOptionsScreen s ? s : screen;
        this.uiState = uiState;
        this.applySettings = consumer;
        this.generator = flatLevelGeneratorSettings;
        maxOverworldHeight = uiState.getSettings().worldgenLoadContext().lookupOrThrow(Registries.DIMENSION_TYPE).get(BuiltinDimensionTypes.OVERWORLD).map(l -> l.value().height()).orElse(384);
        structuresOverrides = new ArrayList<>(generator.structureOverrides().orElse(HolderSet.direct()).stream().toList());
        generator.getLayersInfo().forEach(this::addLayer);
        biomeGetter.listElements().forEach(this::addBiome);
        structureGetter.listElements().forEach(this::addStructure);
        renderableVLists.clear();
        renderableVLists.add(displayLayers);
        renderableVLists.add(displayBiomes);
        renderableVLists.add(displayProperties);
        displayProperties.addRenderable(new TickBox(0, 0, 260, generator.decoration, b -> LegacyComponents.DECORATIONS, b -> null, b -> generator.decoration = b.selected));
        displayProperties.addRenderable(new TickBox(0, 0, 260, generator.addLakes, b -> LegacyComponents.LAVA_LAKES, b -> null, b -> generator.addLakes = b.selected));
    }

    @Override
    public void addControlTooltips(ControlTooltip.Renderer renderer) {
        super.addControlTooltips(renderer);
        renderer.add(() -> movingLayer != null || tabList.getIndex() != 0 || getFocused() == null ? null : ControlType.getActiveType().isKbm() ? ControlTooltip.getKeyIcon(InputConstants.KEY_X) : ControllerBinding.LEFT_BUTTON.getIcon(), () -> LegacyComponents.MOVE_LAYER).
                add(() -> movingLayer != null ? null : ControlType.getActiveType().isKbm() ? ControlTooltip.getKeyIcon(InputConstants.KEY_O) : ControllerBinding.UP_BUTTON.getIcon(), () -> LegacyComponents.PRESETS).
                add(ControlTooltip.CONTROL_TAB::get, () -> movingLayer != null ? null : LegacyComponents.SELECT_TAB).
                add(() -> movingLayer == null ? null : ControlType.getActiveType().isKbm() ? ControlTooltip.getKeyIcon(InputConstants.KEY_PAGEUP) : ControllerBinding.LEFT_TRIGGER.getIcon(), () -> LegacyComponents.PAGE_UP).
                add(() -> movingLayer == null ? null : ControlType.getActiveType().isKbm() ? ControlTooltip.getKeyIcon(InputConstants.KEY_PAGEDOWN) : ControllerBinding.RIGHT_TRIGGER.getIcon(), () -> LegacyComponents.PAGE_DOWN).
                add(ControlTooltip.VERTICAL_NAVIGATION::get, () -> movingLayer == null ? null : LegacyComponents.MOVE_UP_DOWN);
    }

    public void addStructure(Holder.Reference<StructureSet> structure) {
        MutableComponent component = Component.empty();
        String nameKey = "structure." + structure.key().location().toLanguageKey();
        String descriptionKey = nameKey + ".description";
        Component name = Component.translatable(nameKey);
        if (LegacyTipManager.hasTip(nameKey)) component.append(name);
        if (LegacyTipManager.hasTip(descriptionKey)) {
            component.append("\n\n").append(Component.translatable(descriptionKey));
        }
        Tooltip t = Tooltip.create(component);
        displayProperties.addRenderable(new TickBox(0, 0, 260, structuresOverrides.contains(structure), b -> name, b -> t, b -> {
            if (b.selected) structuresOverrides.add(structure);
            else structuresOverrides.remove(structure);
        }));
    }

    public void addBiome(Holder.Reference<Biome> biome) {
        AbstractButton b;
        displayBiomes.addRenderable(b = new ItemIconButton(0, 0, 260, 30, Component.translatable("biome." + biome.key().location().toLanguageKey())) {
            @Override
            public void onPress(InputWithModifiers input) {
                generator.biome = biome;
            }

            @Override
            protected void renderWidget(GuiGraphics guiGraphics, int i, int j, float f) {
                super.renderWidget(guiGraphics, i, j, f);
                ItemStack s = LegacyBiomeOverride.getOrDefault(biome.unwrapKey()).icon();
                if (!s.isEmpty()) {
                    renderItem(guiGraphics, s, "biomeIcon", 26);
                }
                FactoryScreenUtil.enableBlend();
                FactoryGuiGraphics.of(guiGraphics).blitSprite(isHoveredOrFocused() ? LegacySprites.TICKBOX_HOVERED : LegacySprites.TICKBOX, this.getX() + 6, this.getY() + (height - TickBox.getDefaultHeight()) / 2, TickBox.getDefaultHeight(), TickBox.getDefaultHeight());
                if (generator.biome == biome) {
                    if (LegacyOptions.getUIMode().isSD())
                        FactoryGuiGraphics.of(guiGraphics).blitSprite(LegacySprites.SMALL_TICK, this.getX() + 6, this.getY() + (height - 9) / 2, 11, 9);
                    else FactoryGuiGraphics.of(guiGraphics).blitSprite(LegacySprites.TICK, this.getX() + 6, this.getY() + (height - 12) / 2, 14, 12);
                }
                FactoryScreenUtil.disableBlend();
            }

            @Override
            protected void renderScrollingString(GuiGraphics guiGraphics, Font font, int i, int j) {
                renderScrollingString(guiGraphics, font, "biomeMessage", 54, i, j);
            }
        });
        MutableComponent descr = b.getMessage().copy();
        String descriptionKey = "biome." + biome.key().location().toLanguageKey() + ".description";
        if (LegacyTipManager.hasTip(descriptionKey)) {
            descr.append("\n\n").append(Component.translatable(descriptionKey));
        }
        b.setTooltip(Tooltip.create(descr));
    }

    public void addLayer(FlatLayerInfo flatLayerInfo) {
        addLayer(flatLayerInfo, 0);
    }

    public void addLayer(FlatLayerInfo flatLayerInfo, int index) {
        displayLayers.renderables.add(index, new LayerButton(0, 0, 270, 30, flatLayerInfo));
    }

    public int getAllLayersHeight() {
        int height = 0;
        for (Renderable renderable : displayLayers.renderables) {
            if (renderable instanceof LayerButton layerButton) height += layerButton.flatLayerInfo.getHeight();
        }
        return height;
    }

    public void removeLayer(int index) {
        displayLayers.renderables.remove(index);
    }

    public void switchLayers(AbstractButton selected, AbstractButton aimPlace) {
        int selectedIndex = displayLayers.renderables.indexOf(selected);
        int aimIndex = displayLayers.renderables.indexOf(aimPlace);
        displayLayers.renderables.set(aimIndex, selected);
        displayLayers.renderables.set(selectedIndex, aimPlace);
        repositionElements();
        LegacySoundUtil.playSimpleUISound(SoundEvents.UI_BUTTON_CLICK.value(), 1.0f);
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
        LegacyRenderUtil.renderDefaultBackground(accessor, guiGraphics, false);
    }

    @Override
    protected void init() {
        if (movingLayer != null && tabList.getIndex() != 0) tabList.setSelected(0);
        addRenderableOnly(((guiGraphics, i, j, f) -> {
            if (LegacyRenderUtil.hasTooltipBoxes(accessor)) {
                Optional<GuiEventListener> listener;
                Component message = null;
                if (getFocused() instanceof AbstractWidgetAccessor widget && widget.getTooltip() != null && widget.getTooltip().get() != null)
                    message = widget.getTooltip().get().message;
                else if ((listener = getChildAt(i, j)).isPresent() && listener.get() instanceof AbstractWidgetAccessor widget && widget.getTooltip() != null && widget.getTooltip().get() != null)
                    message = widget.getTooltip().get().message;

                boolean sd = LegacyOptions.getUIMode().isSD();

                MultiLineLabel label = message == null ? null : (sd ? Panel.sdLabelsCache : Panel.labelsCache).apply(message, tooltipBox.getWidth() - 10);

                int lineHeight = sd ? 8 : 12;

                scrollableRenderer.lineHeight = lineHeight;

                if (label == null)
                    scrollableRenderer.resetScrolled();
                else
                    scrollableRenderer.scrolled.max = Math.max(0, label.getLineCount() - (tooltipBox.getHeight() - (sd ? 20 : 44)) / (lineHeight));

                tooltipBox.render(guiGraphics, i, j, f);
                if (label != null) {
                    scrollableRenderer.render(guiGraphics, panel.x + panel.width + 3, panel.y + 13, tooltipBox.width - 10, tooltipBox.getHeight() - 44, () -> label.render(guiGraphics, MultiLineLabel.Align.LEFT, panel.x + panel.width + 3, panel.y + 13, lineHeight, true, 0xFFFFFFFF));
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
        tooltipBox.init();
        panelRecess.init("panelRecess");
        addRenderableOnly(panelRecess);
    }

    @Override
    public void renderableVListInit() {
        getRenderableVList().init(panel.x + 11, panel.y + 11, panel.getWidth() - 22, panel.getHeight() - 22);
    }

    public RenderableVList getRenderableVList() {
        return getRenderableVLists().get(tabList.getIndex());
    }

    @Override
    public boolean mouseScrolled(double d, double e/*? if >1.20.1 {*/, double f/*?}*/, double g) {
        if (tooltipBox.isHovered(d, e) && scrollableRenderer.mouseScrolled(g)) return true;
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
    public boolean keyPressed(KeyEvent keyEvent) {
        if (movingLayer == null) {
            if (tabList.controlTab(keyEvent.key())) return true;
            if (keyEvent.key() == InputConstants.KEY_O)
                minecraft.setScreen(new LegacyFlatPresetsScreen(this, uiState.getSettings().worldgenLoadContext().lookupOrThrow(Registries.FLAT_LEVEL_GENERATOR_PRESET), uiState.getSettings().dataConfiguration().enabledFeatures(), f -> setPreset(f.value().settings())));
        }
        return super.keyPressed(keyEvent);
    }

    @Override
    public void onClose() {
        super.onClose();
        this.generator.getLayersInfo().clear();
        displayLayers.renderables.forEach(r -> {
            if (r instanceof LayerButton l) this.generator.getLayersInfo().add(0, l.flatLayerInfo);
        });
        this.generator.updateLayers();
        applySettings.accept(generator);
        generator.structureOverrides = Optional.of(HolderSet.direct(structuresOverrides));
    }

    @Override
    public void render(GuiGraphics guiGraphics, int i, int j, float f) {
        super.render(guiGraphics, i, j, f);
        if (LegacyRenderUtil.hasTooltipBoxes(accessor))
            guiGraphics.deferredTooltip = null;
    }

    public abstract static class ItemIconButton extends ListButton implements RenderableVListEntry {
        public ItemIconButton(int i, int j, int k, int l, Component component) {
            super(null, i, j, k, l, component);
        }

        @Override
        public void initRenderable(RenderableVList list) {
            this.list = list;
            setHeight(list.accessor.getInteger("buttonsHeight", 30));
        }

        protected void renderScrollingString(GuiGraphics guiGraphics, Font font, String messageName, int messageX, int xd, int color) {
            LegacyFontUtil.applySDFont(b -> LegacyRenderUtil.renderScrollingString(guiGraphics, font, this.getMessage(), this.getX() + list.accessor.getInteger("%s.%s.xOffset".formatted(list.name, messageName), messageX), this.getY(), getX() + this.getWidth() - xd, this.getY() + this.getHeight(), color, true));
        }

        public void renderItem(GuiGraphics guiGraphics, ItemStack itemStack, String iconName, int x) {
            guiGraphics.pose().pushMatrix();
            float itemScale = list.accessor.getFloat("%s.%s.scale".formatted(list.name, iconName), 1.25f);
            guiGraphics.pose().translate(getX() + list.accessor.getInteger("%s.%s.x".formatted(list.name, iconName), x), getY() + list.accessor.getInteger(list.name + ".buttonItem.y", (getHeight() - Math.round(16 * itemScale)) / 2));
            guiGraphics.pose().scale(itemScale, itemScale);
            guiGraphics.renderItem(itemStack, 0, 0);
            guiGraphics.pose().popMatrix();
        }
    }

    public class LayerButton extends ItemIconButton implements ControlTooltip.ActionHolder {
        public final FlatLayerInfo flatLayerInfo;

        public LayerButton(int i, int j, int k, int l, FlatLayerInfo flatLayerInfo) {
            super(i, j, k, l, flatLayerInfo.getBlockState().getBlock().getName());
            this.flatLayerInfo = flatLayerInfo;
            ItemStack s = flatLayerInfo.getBlockState().getBlock().asItem().getDefaultInstance();
            MutableComponent descr = flatLayerInfo.getBlockState().getBlock().getName();
            if (LegacyTipManager.hasTip(s)) {
                descr.append("\n\n").append(LegacyTipManager.getTipComponent(s));
            }
            setTooltip(Tooltip.create(descr));
        }

        @Override
        protected void renderWidget(GuiGraphics guiGraphics, int i, int j, float f) {
            super.renderWidget(guiGraphics, i, j, f);
            LegacyFontUtil.applySDFont(b -> guiGraphics.drawString(font, Component.translatable("legacy.menu.create_flat_world.layer_count", flatLayerInfo.getHeight()), getX() + list.accessor.getInteger(list.name + ".layerCount.xOffset", 12), getY() + 1 + (height - font.lineHeight) / 2, LegacyRenderUtil.getDefaultTextColor(!isHoveredOrFocused())));
            renderItem(guiGraphics, flatLayerInfo.getBlockState().getBlock().asItem().getDefaultInstance(), "layerIcon",39);
        }

        @Override
        protected void renderScrollingString(GuiGraphics guiGraphics, Font font, int i, int j) {
            renderScrollingString(guiGraphics, font, "layerMessage", 67, i, j);
        }

        @Override
        public void setFocused(boolean bl) {
            if (bl && movingLayer != null && movingLayer != this) return;
            super.setFocused(bl);
        }

        @Override
        public boolean keyPressed(KeyEvent keyEvent) {
            if (keyEvent.key() == InputConstants.KEY_X) {
                movingLayer = this;
                return true;
            }
            return super.keyPressed(keyEvent);
        }

        @Override
        public void onPress(InputWithModifiers inputWithModifiers) {
            if (movingLayer != null) {
                if (isFocused()) movingLayer = null;
                return;
            }
            int allHeight = getAllLayersHeight();
            int layerIndex = displayLayers.renderables.indexOf(this);
            minecraft.setScreen(new ConfirmationScreen(LegacyFlatWorldScreen.this, ConfirmationScreen::getPanelWidth, () -> LegacyOptions.getUIMode().isSD() ? 87 : 120, LegacyComponents.LAYER_OPTIONS, LegacyComponents.LAYER_MESSAGE, b -> {
            }) {
                @Override
                protected void addButtons() {
                    renderableVList.addRenderable(Button.builder(Component.translatable("legacy.menu.create_flat_world.edit_layer"), b -> {
                        this.minecraft.setScreen(new FlatWorldLayerSelector(LegacyFlatWorldScreen.this, flatLayerInfo, f -> {
                            removeLayer(layerIndex);
                            addLayer(f.getFlatLayerInfo(), layerIndex);
                        }, maxOverworldHeight - allHeight + flatLayerInfo.getHeight(), Component.translatable("legacy.menu.create_flat_world.edit_layer")));
                    }).bounds(panel.x + 15, panel.y + panel.height - 74, 200, 20).build());
                    Button addButton = Button.builder(Component.translatable("legacy.menu.create_flat_world.add_layer"), b -> this.minecraft.setScreen(new FlatWorldLayerSelector(LegacyFlatWorldScreen.this, f -> addLayer(f.getFlatLayerInfo(), layerIndex), maxOverworldHeight - allHeight, Component.translatable("legacy.menu.create_flat_world.add_layer")))).bounds(panel.x + 15, panel.y + panel.height - 52, 200, 20).build();
                    if (allHeight >= maxOverworldHeight) addButton.active = false;
                    renderableVList.addRenderable(addButton);
                    renderableVList.addRenderable(Button.builder(Component.translatable("legacy.menu.create_flat_world.delete_layer"), b -> {
                        removeLayer(layerIndex);
                        this.onClose();
                    }).bounds(panel.x + 15, panel.y + panel.height - 30, 200, 20).build());
                }
            });
        }

        @Override
        public @Nullable Component getAction(Context context) {
            return context.actionOfContext(KeyContext.class, c -> c.key() == InputConstants.KEY_RETURN ? movingLayer != null ? LegacyComponents.PLACE : LegacyComponents.LAYER_OPTIONS : null);
        }
    }

}

