package wily.legacy.client.screen;

import com.mojang.datafixers.util.Pair;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.packs.PackSelectionScreen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.PresetEditor;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.core.Holder;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.levelgen.presets.WorldPreset;
import net.minecraft.world.level.WorldDataConfiguration;
import wily.factoryapi.base.Bearer;
import wily.factoryapi.base.client.DatapackRepositoryAccessor;
import wily.factoryapi.base.client.SimpleLayoutRenderable;
import wily.legacy.client.CommonColor;
import wily.legacy.mixin.base.client.AbstractWidgetAccessor;
import wily.legacy.util.LegacyComponents;
import wily.legacy.util.client.LegacyRenderUtil;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

public class WorldMoreOptionsScreen extends PanelVListScreen implements ControlTooltip.Event, DatapackRepositoryAccessor {
    public static final Component ENTER_SEED = Component.translatable("selectWorld.enterSeed");
    public static final Component SEED_INFO = Component.translatable("selectWorld.seedInfo");
    public static final Component ENTER_SEED_DESCRIPTION = Component.translatable("legacy.menu.selectWorld.enterSeed.description");
    protected final TabList tabList = new TabList(accessor).add(LegacyTabButton.Type.LEFT, Component.translatable("createWorld.tab.world.title"), t -> rebuildWidgets()).add(LegacyTabButton.Type.RIGHT, Component.translatable("legacy.menu.game_options"), t -> rebuildWidgets());

    protected final RenderableVList gameRenderables = new RenderableVList(accessor);

    protected final Panel tooltipBox = Panel.tooltipBoxOf(panel, 188);
    protected List<FormattedCharSequence> tooltipBoxLabel;
    protected ScrollableRenderer scrollableRenderer = new ScrollableRenderer(new LegacyScrollRenderer());
    protected Runnable onClose = () -> {
    };

    public WorldMoreOptionsScreen(CreateWorldScreen parent, Bearer<Boolean> trustPlayers) {
        super(parent, 244, 199, Component.translatable("createWorld.tab.more.title"));
        renderableVLists.add(gameRenderables);
        renderableVList.addRenderable(SimpleLayoutRenderable.createDrawString(ENTER_SEED, 0, 1, 2, 9, CommonColor.INVENTORY_GRAY_TEXT.get(), false));
        EditBox editBox = new EditBox(Minecraft.getInstance().font, 0, 0, 308, 20, ENTER_SEED) {
            protected MutableComponent createNarrationMessage() {
                return super.createNarrationMessage().append(CommonComponents.NARRATION_SEPARATOR).append(SEED_INFO);
            }
        };
        editBox.setTooltip(new MultilineTooltip(tooltipBox.getWidth() - 10, ENTER_SEED_DESCRIPTION));
        editBox.setValue(parent.getUiState().getSeed());
        editBox.setResponder(string -> parent.getUiState().setSeed(editBox.getValue()));
        renderableVList.addRenderable(editBox);
        renderableVList.addRenderable(SimpleLayoutRenderable.create(0, 9, r -> ((guiGraphics, i, j, f) -> guiGraphics.drawString(Minecraft.getInstance().font, SEED_INFO, r.x + 1, r.y + 2, CommonColor.INVENTORY_GRAY_TEXT.get(), false))));
        renderableVList.addRenderable(new TickBox(0, 0, parent.getUiState().isGenerateStructures(), b -> Component.translatable("selectWorld.mapFeatures"), b -> Tooltip.create(Component.translatable("selectWorld.mapFeatures.info")), b -> parent.getUiState().setGenerateStructures(b.selected)));
        renderableVList.addRenderable(new TickBox(0, 0, parent.getUiState().isBonusChest(), b -> Component.translatable("selectWorld.bonusItems"), b -> Tooltip.create(Component.translatable("legacy.menu.selectWorld.bonusItems.description")), b -> parent.getUiState().setBonusChest(b.selected)));
        renderableVList.addRenderable(new LegacySliderButton<>(0, 0, 0, 16,
                s -> s.getDefaultMessage(Component.translatable("selectWorld.mapType"),
                        s.getObjectValue().describePreset()),
                b -> {
                    Holder<WorldPreset> holder = parent.getUiState().getWorldType().preset();
                    if (holder == null)
                        return null;
                    String id = holder.unwrapKey().map(key -> key.location().toString()).orElse("minecraft:custom");
                    if (id.equals("minecraft:amplified")) {
                        return Tooltip.create(
                                Component.translatable("legacy.menu.selectWorld.mapType.amplified.description"));
                    } else if (id.equals("minecraft:flat")) {
                        return Tooltip
                                .create(Component.translatable("legacy.menu.selectWorld.mapType.flat.description"));
                    } else if (id.equals("minecraft:large_biomes")) {
                        return Tooltip.create(
                                Component.translatable("legacy.menu.selectWorld.mapType.large_biomes.description"));
                    } else if (id.equals("minecraft:single_biome_surface")) {
                        return Tooltip.create(Component
                                .translatable("legacy.menu.selectWorld.mapType.single_biome_surface.description"));
                    }
                    return Tooltip.create(Component.translatable("legacy.menu.selectWorld.mapType.description"));
                },
                parent.getUiState().getWorldType(),
                () -> CycleButton.DEFAULT_ALT_LIST_SELECTOR.getAsBoolean() ? parent.getUiState().getAltPresetList()
                        : parent.getUiState().getNormalPresetList(),
                b -> parent.getUiState().setWorldType(b.objectValue)));
        Button customizeButton = Button.builder(Component.translatable("selectWorld.customizeType"), button -> {
            PresetEditor presetEditor = parent.getUiState().getPresetEditor();
            if (presetEditor != null)
                minecraft.setScreen(presetEditor.createEditScreen(parent, parent.getUiState().getSettings()));
        }).tooltip(Tooltip.create(Component.empty())).build();
        parent.getUiState().addListener(s -> {customizeButton.active = !s.isDebug() && s.getPresetEditor() != null;
            Holder<WorldPreset> holder = s.getWorldType().preset();
            Component desc;
            if (holder == null) {
                desc = Component.translatable("legacy.menu.selectWorld.mapType.customize.description");
            } else {
                String id = holder.unwrapKey().map(key -> key.location().toString()).orElse("minecraft:custom");
                if (id.equals("minecraft:flat")) {
                    desc = Component.translatable("legacy.menu.selectWorld.mapType.flat.customize.description");
                } else if (id.equals("minecraft:single_biome_surface")) {
                    desc = Component
                            .translatable("legacy.menu.selectWorld.mapType.single_biome_surface.customize.description");
                } else {
                    desc = Component.translatable("legacy.menu.selectWorld.mapType.customize.description");
                }
            }
            customizeButton.setTooltip(Tooltip.create(desc));
        });
        renderableVList.addRenderable(customizeButton);
        renderableVList.addRenderable(SimpleLayoutRenderable.create(0, 9, r -> ((guiGraphics, i, j, f) -> {
            if (customizeButton.active) {
                guiGraphics.drawString(Minecraft.getInstance().font,
                        Component.translatable("legacy.menu.selectWorld.mapType.customize.current"), r.x + 1, r.y + 2,
                        CommonColor.INVENTORY_GRAY_TEXT.get(), false);
            }
        })));
        TickBox hostPrivileges = new TickBox(0, 0, parent.getUiState()./*? if <1.20.5 {*//*isAllowCheats*//*?} else {*/isAllowCommands/*?}*/(), b -> LegacyComponents.HOST_PRIVILEGES, b -> Tooltip.create(LegacyComponents.HOST_PRIVILEGES_INFO), b -> parent.getUiState()./*? if <1.20.5 {*//*setAllowCheats*//*?} else {*/setAllowCommands/*?}*/(b.selected));
        parent.getUiState().addListener(s -> hostPrivileges.active = !s.isDebug() && !s.isHardcore());
        GameRules gameRules = parent.getUiState().getGameRules();
        Pair<Path, PackRepository> pair = parent.getDataPackSelectionSettings(parent.getUiState().getSettings().dataConfiguration());
        if (pair != null) {
            renderableVList.addRenderable(SimpleLayoutRenderable.create(0, 9, r -> ((guiGraphics, i, j, f) -> guiGraphics.drawString(Minecraft.getInstance().font, Component.translatable("selectWorld.experiments"), r.x + 1, r.y + 2, CommonColor.INVENTORY_GRAY_TEXT.get(), false))));
            PackRepository dataRepository = pair.getSecond();
            List<String> selectedExperiments = new ArrayList<>(dataRepository.getSelectedIds());
            dataRepository.getAvailablePacks().forEach(p -> {
                if (p.getPackSource() != PackSource.FEATURE) return;
                String id = "dataPack." + p.getId() + ".name";
                Component name = Language.getInstance().has(id) ? Component.translatable(id) : p.getTitle();
                renderableVList.addRenderable(new TickBox(0, 0, selectedExperiments.contains(p.getId()), b -> name, b -> new MultilineTooltip(tooltipBox.getWidth() - 10, p.getDescription()), b -> {
                    if (b.selected && !selectedExperiments.contains(p.getId())) selectedExperiments.add(p.getId());
                    else if (!b.selected) selectedExperiments.remove(p.getId());
                }));
            });
            onClose = () -> {
                dataRepository.setSelected(selectedExperiments);
                parent.tryApplyNewDataPacks(dataRepository, false, w -> minecraft.setScreen(this));
            };
        }
        renderableVList.addRenderable(Button.builder(Component.translatable("selectWorld.dataPacks"), button -> openDataPackSelectionScreen(parent, parent.getUiState().getSettings().dataConfiguration())).tooltip(Tooltip.create(Component.translatable("legacy.menu.selectWorld.dataPacks.description"))).build());
        renderableVList.addRenderable(new TickBox(0, 0, trustPlayers.get(), b -> Component.translatable("legacy.menu.selectWorld.trust_players"), b -> Tooltip.create(Component.translatable("legacy.menu.selectWorld.trust_players.description")), t -> trustPlayers.set(t.selected)));
        addGameRulesOptions(renderableVList, gameRules, k -> k.getCategory() == GameRules.Category.UPDATES);
        gameRenderables.addRenderable(hostPrivileges);
        for (GameRules.Category value : GameRules.Category.values()) {
            if (value == GameRules.Category.UPDATES) continue;
            addGameRulesOptions(gameRenderables, gameRules, k -> k.getCategory() == value);
        }
        parent.getUiState().onChanged();
    }

    public WorldMoreOptionsScreen(LoadSaveScreen parent) {
        super(parent, 244, 199, Component.translatable("createWorld.tab.more.title"));
        renderableVLists.add(gameRenderables);
        tabList.setSelected(1);
        GameRules gameRules = parent.summary.getSettings().gameRules();
        LoadSaveScreen.RESETTABLE_DIMENSIONS.forEach(d -> renderableVList.addRenderable(new TickBox(0, 0, parent.dimensionsToReset.contains(d), b -> Component.translatable("legacy.menu.load_save.reset", LegacyComponents.getDimensionName(d)), b -> null, t -> {
            if (t.selected) parent.dimensionsToReset.add(d);
            else parent.dimensionsToReset.remove(d);
        })));
        renderableVList.addRenderable(new TickBox(0, 0, parent.trustPlayers, b -> Component.translatable("legacy.menu.selectWorld.trust_players"), b -> null, t -> parent.trustPlayers = t.selected));
        addGameRulesOptions(renderableVList, gameRules, k -> k.getCategory() == GameRules.Category.UPDATES);
        gameRenderables.addRenderable(new TickBox(0, 0, parent.hostPrivileges, b -> LegacyComponents.HOST_PRIVILEGES, b -> Tooltip.create(LegacyComponents.HOST_PRIVILEGES_INFO), b -> parent.hostPrivileges = b.selected));
        for (GameRules.Category value : GameRules.Category.values()) {
            if (value == GameRules.Category.UPDATES) continue;
            addGameRulesOptions(gameRenderables, gameRules, k -> k.getCategory() == value);
        }
        parent.applyGameRules = (g, s) -> {
            if (!g.equals(gameRules)) g.assignFrom(gameRules, s);
        };
    }

    @Override
    public void addControlTooltips(ControlTooltip.Renderer renderer) {
        super.addControlTooltips(renderer);
        renderer.add(ControlTooltip.CONTROL_TAB::get, () -> tabList.getIndex() == 0 ? LegacyComponents.GAME_OPTIONS : LegacyComponents.WORLD_OPTIONS);
    }

    public void addGameRulesOptions(RenderableVList list, GameRules gameRules, Predicate<GameRules.Key<?>> allowGamerule) {
        gameRules.visitGameRuleTypes(new GameRules.GameRuleTypeVisitor() {

            @Override
            public void visitBoolean(GameRules.Key<GameRules.BooleanValue> key, GameRules.Type<GameRules.BooleanValue> type) {
                if (!allowGamerule.test(key)) return;
                GameRules.BooleanValue value = gameRules.getRule(key);
                GameRules.BooleanValue defaultValue = type.createRule();
                Component tooltip = Component.translatable(key.getDescriptionId() + ".description");
                Component valueTooltip = Component.translatable("editGamerule.default", defaultValue.serialize()).withStyle(ChatFormatting.GRAY);
                list.addRenderable(new TickBox(0, 0, gameRules.getRule(key).get(), b -> Component.translatable(key.getDescriptionId()), b -> new MultilineTooltip(tooltipBox.width - 10, tooltip, valueTooltip), b -> value.set(b.selected, null)));
            }

            @Override
            public void visitInteger(GameRules.Key<GameRules.IntegerValue> key, GameRules.Type<GameRules.IntegerValue> type) {
                if (!allowGamerule.test(key)) return;
                GameRules.IntegerValue value = gameRules.getRule(key);
                GameRules.IntegerValue defaultValue = type.createRule();
                Component tooltip = Component.translatable(key.getDescriptionId() + ".description");
                Component valueTooltip = Component.translatable("editGamerule.default", defaultValue.serialize()).withStyle(ChatFormatting.GRAY);
                EditBox integerEdit = new EditBox(Minecraft.getInstance().font, 0, 0, 220, 20, Component.translatable(key.getDescriptionId()));
                integerEdit.setTooltip(new MultilineTooltip(tooltipBox.width - 10, tooltip, valueTooltip));
                integerEdit.setValue(Integer.toString(value.get()));
                integerEdit.setResponder(string -> {
                    if (value.tryDeserialize(string)) {
                        integerEdit.setTextColor(0xFFE0E0E0);
                        value.set(Integer.parseInt(string), null);
                    } else {
                        integerEdit.setTextColor(0xFFFF0000);
                    }
                });
                list.addRenderable(SimpleLayoutRenderable.create(0, 9, r -> ((guiGraphics, i, j, f) -> guiGraphics.drawString(Minecraft.getInstance().font, Component.translatable(key.getDescriptionId()), r.x + 1, r.y + 2, CommonColor.INVENTORY_GRAY_TEXT.get(), false))));
                list.addRenderable(integerEdit);
            }
        });
    }

    @Override
    public void onClose() {
        super.onClose();
        onClose.run();
    }

    @Override
    public boolean mouseScrolled(double d, double e, double f, double g) {
        if (tooltipBox.isHovered(d, e) && scrollableRenderer.mouseScrolled(g)) return true;
        return super.mouseScrolled(d, e, f, g);
    }

    @Override
    public void renderDefaultBackground(GuiGraphics guiGraphics, int i, int j, float f) {
        LegacyRenderUtil.renderDefaultBackground(accessor, guiGraphics, false);
        if (LegacyRenderUtil.hasTooltipBoxes(accessor)) {
            Optional<GuiEventListener> listener;
            if (getFocused() instanceof AbstractWidgetAccessor widget && widget.getTooltip() != null && widget.getTooltip().get() != null)
                tooltipBoxLabel = widget.getTooltip().get().toCharSequence(minecraft);
            else if ((listener = getChildAt(i, j)).isPresent() && listener.get() instanceof AbstractWidgetAccessor widget && widget.getTooltip() != null && widget.getTooltip().get() != null)
                tooltipBoxLabel = widget.getTooltip().get().toCharSequence(minecraft);
            else tooltipBoxLabel = null;

            if (tooltipBoxLabel == null)
                scrollableRenderer.resetScrolled();
            else
                scrollableRenderer.scrolled.max = Math.max(0, tooltipBoxLabel.size() - (tooltipBox.getHeight() - 44) / 12);

            tooltipBox.render(guiGraphics, i, j, f);
            if (tooltipBoxLabel != null) {
                scrollableRenderer.render(guiGraphics, panel.x + panel.width + 3, panel.y + 13, tooltipBox.width - 10, tooltipBox.getHeight() - 44, () -> tooltipBoxLabel.forEach(c -> guiGraphics.drawString(font, c, panel.x + panel.width + 3, panel.y + 13 + tooltipBoxLabel.indexOf(c) * 12, 0xFFFFFFFF)));
            }
        }
    }

    @Override
    public boolean keyPressed(KeyEvent keyEvent) {
        tabList.controlTab(keyEvent.key());
        return super.keyPressed(keyEvent);
    }

    @Override
    public RenderableVList getRenderableVList() {
        return renderableVLists.get(tabList.getIndex());
    }

    @Override
    protected void init() {
        addRenderableWidget(tabList);
        super.init();
        addRenderableOnly(tabList::renderSelected);
        tabList.init(panel.x, panel.y - 24, panel.width, 30);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int i, int j, float f) {
        super.render(guiGraphics, i, j, f);
        if (LegacyRenderUtil.hasTooltipBoxes(accessor))
            guiGraphics.deferredTooltip = null;
    }

    @Override
    public void renderableVListInit() {
        if (LegacyRenderUtil.hasTooltipBoxes(accessor)) tooltipBox.init();
        super.renderableVListInit();
    }

    protected void openDataPackSelectionScreen(CreateWorldScreen screen, WorldDataConfiguration worldDataConfiguration) {
        Pair<Path, PackRepository> pair = screen.getDataPackSelectionSettings(worldDataConfiguration);
        if (pair != null) {
            this.minecraft.setScreen(new PackSelectionScreen(pair.getSecond(), packRepository -> screen.tryApplyNewDataPacks(packRepository, true, d -> openDataPackSelectionScreen(screen, d)), pair.getFirst(), Component.translatable("dataPack.title")));
        }
    }

    @Override
    public PackRepository getDatapackRepository() {
        return parent instanceof CreateWorldScreen screen ? screen.getDataPackSelectionSettings(screen.getUiState().getSettings().dataConfiguration()).getSecond() : null;
    }

    @Override
    public void tryApplyNewDataPacks(PackRepository packRepository) {
        if (parent instanceof CreateWorldScreen screen) {
            screen.tryApplyNewDataPacks(packRepository, false, w -> minecraft.setScreen(this));
        }
    }
}
