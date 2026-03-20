package wily.legacy.client.screen;

//~ gamerule

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
import net.minecraft.locale.Language;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.repository.PackSource;
//? if >=1.21.11 {
/*import net.minecraft.client.gui.TextAlignment;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.gamerules.GameRule;
import net.minecraft.world.level.gamerules.GameRuleTypeVisitor;
import net.minecraft.world.level.gamerules.GameRules.Category;
import wily.legacy.mixin.base.GameRuleCategoryAccessor;
*///?} else {
import net.minecraft.world.level.GameRules;
//?}
import net.minecraft.world.level.WorldDataConfiguration;
import wily.factoryapi.base.Bearer;
import wily.factoryapi.base.client.DatapackRepositoryAccessor;
import wily.factoryapi.base.client.SimpleLayoutRenderable;
import wily.legacy.client.LegacyOptions;
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

    protected final Panel tooltipBox = Panel.tooltipBoxOf(panel, () -> LegacyOptions.getUIMode().isSD() ? 106 : 188);
    protected ScrollableRenderer scrollableRenderer = new ScrollableRenderer(new LegacyScrollRenderer());
    protected Runnable onClose = () -> {
    };

    public WorldMoreOptionsScreen(CreateWorldScreen parent, Bearer<Boolean> trustPlayers) {
        super(parent, 244, 199, Component.translatable("createWorld.tab.more.title"));
        renderableVLists.add(gameRenderables);
        renderableVList.addCategory(ENTER_SEED);
        EditBox editBox = new EditBox(Minecraft.getInstance().font, 0, 0, 308, 20, ENTER_SEED) {
            protected MutableComponent createNarrationMessage() {
                return super.createNarrationMessage().append(CommonComponents.NARRATION_SEPARATOR).append(SEED_INFO);
            }
        };
        editBox.setTooltip(Tooltip.create(ENTER_SEED_DESCRIPTION));
        editBox.setValue(parent.getUiState().getSeed());
        editBox.setResponder(string -> parent.getUiState().setSeed(editBox.getValue()));
        renderableVList.addRenderable(editBox);
        renderableVList.addCategory(SEED_INFO);
        renderableVList.addCategory(Component.translatable("selectWorld.mapType"));
        renderableVList.addRenderable(new LegacySliderButton<>(0, 0, 0, 16,
                s -> s.getObjectValue().describePreset(),
                b -> Tooltip.create(LegacyComponents.getWorldTypeDescription(b.getObjectValue().preset())),
                parent.getUiState().getWorldType(),
                () -> CycleButton.DEFAULT_ALT_LIST_SELECTOR.getAsBoolean() ? parent.getUiState().getAltPresetList()
                        : parent.getUiState().getNormalPresetList(),
                b -> parent.getUiState().setWorldType(b.objectValue)));
        Button customizeButton = new LegacyButton(Component.translatable("selectWorld.customizeType"), button -> {
            PresetEditor presetEditor = parent.getUiState().getPresetEditor();
            if (presetEditor != null)
                minecraft.setScreen(presetEditor.createEditScreen(parent, parent.getUiState().getSettings()));
        });
        parent.getUiState().addListener(s -> {
            customizeButton.active = !s.isDebug() && s.getPresetEditor() != null;
            customizeButton.setTooltip(Tooltip.create(LegacyComponents.getWorldPresetCustomizeDescription(s.getWorldType().preset())));
        });
        renderableVList.addRenderable(customizeButton);
        renderableVList.addRenderable(new TickBox(0, 0, parent.getUiState().isGenerateStructures(), b -> Component.translatable("selectWorld.mapFeatures"), b -> Tooltip.create(Component.translatable("selectWorld.mapFeatures.info")), b -> parent.getUiState().setGenerateStructures(b.selected)));
        renderableVList.addRenderable(new TickBox(0, 0, parent.getUiState().isBonusChest(), b -> Component.translatable("selectWorld.bonusItems"), b -> Tooltip.create(Component.translatable("legacy.menu.selectWorld.bonusItems.description")), b -> parent.getUiState().setBonusChest(b.selected)));
        renderableVList.addRenderable(SimpleLayoutRenderable.create(0, 9, r -> ((guiGraphics, i, j, f) -> {
        })));
        TickBox hostPrivileges = new TickBox(0, 0, parent.getUiState()./*? if <1.20.5 {*//*isAllowCheats*//*?} else {*/isAllowCommands/*?}*/(), b -> LegacyComponents.HOST_PRIVILEGES, b -> Tooltip.create(LegacyComponents.HOST_PRIVILEGES_INFO), b -> parent.getUiState()./*? if <1.20.5 {*//*setAllowCheats*//*?} else {*/setAllowCommands/*?}*/(b.selected));
        parent.getUiState().addListener(s -> hostPrivileges.active = !s.isDebug() && !s.isHardcore());
        GameRules gameRules = parent.getUiState().getGameRules();
        Pair<Path, PackRepository> pair = parent.getDataPackSelectionSettings(parent.getUiState().getSettings().dataConfiguration());
        if (pair != null) {
            renderableVList.addCategory(Component.translatable("selectWorld.experiments"));
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
        renderableVList.addRenderable(new LegacyButton(Component.translatable("selectWorld.dataPacks"), button -> openDataPackSelectionScreen(parent, parent.getUiState().getSettings().dataConfiguration()), Tooltip.create(Component.translatable("legacy.menu.selectWorld.dataPacks.description"))));
        renderableVList.addRenderable(new TickBox(0, 0, trustPlayers.get(), b -> Component.translatable("legacy.menu.selectWorld.trust_players"), b -> Tooltip.create(Component.translatable("legacy.menu.selectWorld.trust_players.description")), t -> trustPlayers.set(t.selected)));
        addGameRulesOptions(renderableVList, gameRules, k -> k.getCategory() == GameRules.Category.UPDATES);
        gameRenderables.addRenderable(hostPrivileges);
        for (GameRules.Category value : /*? if >=1.21.11 {*//*GameRuleCategoryAccessor.getSortOrder()*//*?} else {*/GameRules.Category.values()/*?}*/) {
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
        for (GameRules.Category value : /*? if >=1.21.11 {*//*GameRuleCategoryAccessor.getSortOrder()*//*?} else {*/GameRules.Category.values()/*?}*/) {
            if (value == GameRules.Category.UPDATES) continue;
            addGameRulesOptions(gameRenderables, gameRules, k -> k.getCategory() == value);
        }
        parent.applyGameRules = (g, s) -> {
            if (!g.equals(gameRules)) g./*? if >=1.21.11 {*//*setAll*//*?} else {*/assignFrom/*?}*/(gameRules, s);
        };
    }

    @Override
    public void addControlTooltips(ControlTooltip.Renderer renderer) {
        super.addControlTooltips(renderer);
        renderer.add(ControlTooltip.CONTROL_TAB::get, () -> tabList.getIndex() == 0 ? LegacyComponents.GAME_OPTIONS : LegacyComponents.WORLD_OPTIONS);
    }

    public void addGameRulesOptions(RenderableVList list, GameRules gameRules, Predicate<GameRules.Key<?>> allowGamerule) {
        gameRules.visitGameRuleTypes(new GameRules.GameRuleTypeVisitor() {
            //? if >=1.21.11 {
            /*@Override
            public void visitBoolean(GameRules.Key<Boolean> key) {
                if (!allowGamerule.test(key)) return;
                boolean defaultValue = key.defaultValue();
                Component message = Component.translatable(key.getDescriptionId());
                Tooltip tooltip = Tooltip.create(
                        Component.translatable(key.getDescriptionId() + ".description").append("\n").append(
                                Component.translatable("editGamerule.default", key.serialize(defaultValue)).withStyle(ChatFormatting.GRAY)));
                list.addRenderable(new TickBox(0, 0, gameRules.getRule(key).get(), b -> message, b -> tooltip, b -> gameRules.set(key, b.selected, null)));
            }

            @Override
            public void visitInteger(GameRules.Key<Integer> key) {
                if (!allowGamerule.test(key)) return;
                int value = gameRules.getRule(key).get();
                int defaultValue = key.defaultValue();
                Tooltip tooltip = Tooltip.create(
                        Component.translatable(key.getDescriptionId() + ".description").append("\n").append(
                                Component.translatable("editGamerule.default", key.serialize(defaultValue)).withStyle(ChatFormatting.GRAY)));
                EditBox integerEdit = new EditBox(Minecraft.getInstance().font, 0, 0, 220, 20, Component.translatable(key.getDescriptionId()));
                integerEdit.setTooltip(tooltip);
                integerEdit.setValue(Integer.toString(value));
                integerEdit.setResponder(string -> {
                    if (key.deserialize(string).isSuccess()) {
                        integerEdit.setTextColor(0xFFE0E0E0);
                        gameRules.set(key, Integer.parseInt(string), null);
                    } else {
                        integerEdit.setTextColor(0xFFFF0000);
                    }
                });
                list.addCategory(Component.translatable(key.getDescriptionId()));
                list.addRenderable(integerEdit);
            }
            *///?} else {
            @Override
            public void visitBoolean(GameRules.Key<GameRules.BooleanValue> key, GameRules.Type<GameRules.BooleanValue> type) {
                if (!allowGamerule.test(key)) return;
                GameRules.BooleanValue value = gameRules.getRule(key);
                GameRules.BooleanValue defaultValue = type.createRule();
                Component message = Component.translatable(key.getDescriptionId());
                Tooltip tooltip = Tooltip.create(
                        Component.translatable(key.getDescriptionId() + ".description").append("\n").append(
                        Component.translatable("editGamerule.default", defaultValue.serialize()).withStyle(ChatFormatting.GRAY)));
                list.addRenderable(new TickBox(0, 0, gameRules.getRule(key).get(), b -> message, b -> tooltip, b -> value.set(b.selected, null)));
            }

            @Override
            public void visitInteger(GameRules.Key<GameRules.IntegerValue> key, GameRules.Type<GameRules.IntegerValue> type) {
                if (!allowGamerule.test(key)) return;
                GameRules.IntegerValue value = gameRules.getRule(key);
                GameRules.IntegerValue defaultValue = type.createRule();
                Tooltip tooltip = Tooltip.create(
                        Component.translatable(key.getDescriptionId() + ".description").append("\n").append(
                        Component.translatable("editGamerule.default", defaultValue.serialize()).withStyle(ChatFormatting.GRAY)));
                EditBox integerEdit = new EditBox(Minecraft.getInstance().font, 0, 0, 220, 20, Component.translatable(key.getDescriptionId()));
                integerEdit.setTooltip(tooltip);
                integerEdit.setValue(Integer.toString(value.get()));
                integerEdit.setResponder(string -> {
                    if (value.tryDeserialize(string)) {
                        integerEdit.setTextColor(0xFFE0E0E0);
                        value.set(Integer.parseInt(string), null);
                    } else {
                        integerEdit.setTextColor(0xFFFF0000);
                    }
                });
                list.addCategory(Component.translatable(key.getDescriptionId()));
                list.addRenderable(integerEdit);
            }
            //?}
        });
    }

    @Override
    public void initRenderableVListEntry(RenderableVList renderableVList, Renderable renderable) {
        if (renderable instanceof EditBox editBox)
            editBox.setHeight(LegacyOptions.getUIMode().isSD() ? 16 : 20);
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
                scrollableRenderer.render(guiGraphics, panel.x + panel.width + 3, panel.y + 13, tooltipBox.width - 10, tooltipBox.getHeight() - 44, () ->
                        //? if >=1.21.11 {
                        /*label.visitLines(TextAlignment.LEFT, panel.x + panel.width + 3, panel.y + 13, lineHeight, guiGraphics.textRenderer())
                        *///?} else {
                        label.render(guiGraphics, MultiLineLabel.Align.LEFT, panel.x + panel.width + 3, panel.y + 13, lineHeight, true, 0xFFFFFFFF)
                         //?}
                );
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
