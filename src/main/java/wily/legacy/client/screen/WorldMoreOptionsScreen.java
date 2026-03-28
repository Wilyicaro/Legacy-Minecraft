package wily.legacy.client.screen;

import com.mojang.datafixers.util.Pair;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.screens.packs.PackSelectionScreen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.PresetEditor;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.presets.WorldPreset;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import wily.factoryapi.base.Bearer;
import wily.factoryapi.base.client.DatapackRepositoryAccessor;
import wily.factoryapi.base.client.SimpleLayoutRenderable;
import wily.legacy.client.LegacyOptions;
import wily.legacy.init.LegacyGameRules;
import wily.legacy.mixin.base.client.AbstractWidgetAccessor;
import wily.legacy.util.LegacyComponents;
import wily.legacy.util.client.LegacyRenderUtil;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;

public class WorldMoreOptionsScreen extends PanelVListScreen implements ControlTooltip.Event, DatapackRepositoryAccessor {
    public static final Component ENTER_SEED = Component.translatable("selectWorld.enterSeed");
    public static final Component SEED_INFO = Component.translatable("selectWorld.seedInfo");
    public static final Component ENTER_SEED_DESCRIPTION = Component.translatable("legacy.menu.selectWorld.enterSeed.description");
    private static final List<ResourceKey<WorldPreset>> LEGACY_BIOME_SCALE_PRESETS = List.of(WorldPresets.NORMAL, WorldPresets.LARGE_BIOMES);
    private static final List<GameRules.Key<GameRules.BooleanValue>> HOST_PRIVILEGES_GATED_RULES = List.of(
            GameRules.RULE_DAYLIGHT,
            GameRules.RULE_WEATHER_CYCLE,
            GameRules.RULE_KEEPINVENTORY,
            GameRules.RULE_DOMOBSPAWNING,
            GameRules.RULE_MOBGRIEFING
    );
    protected final TabList tabList = new TabList(accessor).add(LegacyTabButton.Type.LEFT, Component.translatable("createWorld.tab.world.title"), t -> rebuildWidgets()).add(LegacyTabButton.Type.RIGHT, Component.translatable("legacy.menu.game_options"), t -> rebuildWidgets());

    protected final RenderableVList gameRenderables = new RenderableVList(accessor);
    protected final List<Runnable> hostPrivilegesStateUpdaters = new ArrayList<>();
    protected final java.util.Map<GameRules.Key<GameRules.BooleanValue>, Boolean> hostPrivilegesRuleSnapshot = new java.util.HashMap<>();
    protected Boolean lastHostPrivilegesState = null;

    protected final Panel tooltipBox = Panel.tooltipBoxOf(panel, () -> LegacyOptions.getUIMode().isSD() ? 106 : 188);
    protected ScrollableRenderer scrollableRenderer = new ScrollableRenderer(new LegacyScrollRenderer());
    protected Runnable onClose = () -> {
    };

    public WorldMoreOptionsScreen(CreateWorldScreen parent, Bearer<Boolean> trustPlayers, Bearer<Boolean> onlineGame, Bearer<ResourceKey<WorldPreset>> biomeScale) {
        super(parent, 244, 199, Component.translatable("createWorld.tab.more.title"));
        renderableVLists.add(gameRenderables);
        if (LegacyOptions.legacySettingsMenus.get()) {
            initLegacyCreateWorldOptions(parent, trustPlayers, onlineGame, biomeScale);
        } else {
            initDefaultCreateWorldOptions(parent, trustPlayers);
        }
        parent.getUiState().onChanged();
    }

    private void initDefaultCreateWorldOptions(CreateWorldScreen parent, Bearer<Boolean> trustPlayers) {
        renderableVList.addCategory(ENTER_SEED);
        EditBox editBox = createSeedEditBox(parent);
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
        Button customizeButton = createCustomizeButton(parent);
        renderableVList.addRenderable(customizeButton);
        renderableVList.addRenderable(new TickBox(0, 0, parent.getUiState().isGenerateStructures(), b -> Component.translatable("selectWorld.mapFeatures"), b -> Tooltip.create(Component.translatable("selectWorld.mapFeatures.info")), b -> parent.getUiState().setGenerateStructures(b.selected)));
        renderableVList.addRenderable(new TickBox(0, 0, parent.getUiState().isBonusChest(), b -> Component.translatable("selectWorld.bonusItems"), b -> Tooltip.create(Component.translatable("legacy.menu.selectWorld.bonusItems.description")), b -> parent.getUiState().setBonusChest(b.selected)));
        renderableVList.addRenderable(SimpleLayoutRenderable.create(0, 9, r -> ((guiGraphics, i, j, f) -> {
        })));
        TickBox hostPrivileges = createHostPrivilegesTickBox(parent);
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
        for (GameRules.Category value : GameRules.Category.values()) {
            if (value == GameRules.Category.UPDATES) continue;
            addGameRulesOptions(gameRenderables, gameRules, k -> k.getCategory() == value);
        }
    }

    private void initLegacyCreateWorldOptions(CreateWorldScreen parent, Bearer<Boolean> trustPlayers, Bearer<Boolean> onlineGame, Bearer<ResourceKey<WorldPreset>> biomeScale) {
        renderableVList.addCategory(ENTER_SEED);
        EditBox editBox = createSeedEditBox(parent);
        renderableVList.addRenderable(editBox);
        renderableVList.addCategory(SEED_INFO);

        TickBox amplifiedWorld = new TickBox(0, 0, isPresetSelected(parent, WorldPresets.AMPLIFIED), b -> Component.translatable("legacy.menu.selectWorld.amplified_world"), b -> Tooltip.create(LegacyComponents.AMPLIFIED_DESCRIPTION), b -> {
            if (b.selected) setWorldPreset(parent, WorldPresets.AMPLIFIED);
            else setWorldPreset(parent, biomeScale.get());
        });
        TickBox superflatWorld = new TickBox(0, 0, isPresetSelected(parent, WorldPresets.FLAT), b -> Component.translatable("legacy.menu.selectWorld.superflat_world"), b -> Tooltip.create(LegacyComponents.FLAT_DESCRIPTION), b -> {
            if (b.selected) setWorldPreset(parent, WorldPresets.FLAT);
            else setWorldPreset(parent, biomeScale.get());
        });
        parent.getUiState().addListener(s -> {
            amplifiedWorld.selected = isPreset(s.getWorldType().preset(), WorldPresets.AMPLIFIED);
            superflatWorld.selected = isPreset(s.getWorldType().preset(), WorldPresets.FLAT);
            if (!amplifiedWorld.selected && !superflatWorld.selected) {
                biomeScale.set(getLegacyBiomeScalePreset(s.getWorldType().preset()));
            }
        });
        renderableVList.addRenderable(amplifiedWorld);
        renderableVList.addRenderable(SimpleLayoutRenderable.create(0, 9, r -> ((guiGraphics, i, j, f) -> {
        })));
        renderableVList.addRenderable(new LegacySliderButton<>(0, 0, 0, 16,
                b -> b.getDefaultMessage(Component.translatable("legacy.menu.selectWorld.biome_scale"), getLegacyBiomeScaleName(b.getObjectValue())),
                b -> Tooltip.create(Component.translatable("legacy.menu.selectWorld.biome_scale.description")),
                biomeScale.get(),
                () -> LEGACY_BIOME_SCALE_PRESETS,
                b -> {
                    biomeScale.set(b.objectValue);
                    setWorldPreset(parent, b.objectValue);
                },
                biomeScale::get));
        renderableVList.addRenderable(new TickBox(0, 0, parent.getUiState().isGenerateStructures(), b -> Component.translatable("selectWorld.mapFeatures"), b -> Tooltip.create(Component.translatable("selectWorld.mapFeatures.info")), b -> parent.getUiState().setGenerateStructures(b.selected)));
        renderableVList.addRenderable(new TickBox(0, 0, parent.getUiState().isBonusChest(), b -> Component.translatable("selectWorld.bonusItems"), b -> Tooltip.create(Component.translatable("legacy.menu.selectWorld.bonusItems.description")), b -> parent.getUiState().setBonusChest(b.selected)));
        renderableVList.addRenderable(superflatWorld);

        renderableVList.addRenderable(createCustomizeButton(parent));
        renderableVList.addRenderable(new TickBox(0, 0, trustPlayers.get(), b -> Component.translatable("legacy.menu.selectWorld.trust_players"), b -> Tooltip.create(Component.translatable("legacy.menu.selectWorld.trust_players.description")), t -> trustPlayers.set(t.selected)));

        GameRules gameRules = parent.getUiState().getGameRules();
        addBooleanGameRuleOption(renderableVList, gameRules, GameRules.RULE_DOFIRETICK);
        addBooleanGameRuleOption(renderableVList, gameRules, LegacyGameRules.getTntExplodes());

        gameRenderables.addRenderable(new TickBox(0, 0, 200, onlineGame.get(), b -> PublishScreen.getPublishComponent(), b -> PublishScreen.getPublishTooltip(), b -> onlineGame.set(b.selected), onlineGame::get));
        addBooleanGameRuleOption(gameRenderables, gameRules, LegacyGameRules.getPvp());
        gameRenderables.addRenderable(createHostPrivilegesTickBox(parent));
        addBooleanGameRuleOption(gameRenderables, gameRules, GameRules.RULE_DAYLIGHT, () -> parent.getUiState()./*? if <1.20.5 {*//*isAllowCheats*//*?} else {*/isAllowCommands/*?}*/());
        addBooleanGameRuleOption(gameRenderables, gameRules, GameRules.RULE_WEATHER_CYCLE, () -> parent.getUiState()./*? if <1.20.5 {*//*isAllowCheats*//*?} else {*/isAllowCommands/*?}*/());
        addBooleanGameRuleOption(gameRenderables, gameRules, GameRules.RULE_KEEPINVENTORY, () -> parent.getUiState()./*? if <1.20.5 {*//*isAllowCheats*//*?} else {*/isAllowCommands/*?}*/());
        addBooleanGameRuleOption(gameRenderables, gameRules, GameRules.RULE_DOMOBSPAWNING, () -> parent.getUiState()./*? if <1.20.5 {*//*isAllowCheats*//*?} else {*/isAllowCommands/*?}*/());
        addBooleanGameRuleOption(gameRenderables, gameRules, GameRules.RULE_MOBGRIEFING, () -> parent.getUiState()./*? if <1.20.5 {*//*isAllowCheats*//*?} else {*/isAllowCommands/*?}*/());
        addBooleanGameRuleOption(gameRenderables, gameRules, GameRules.RULE_DOMOBLOOT);
        addBooleanGameRuleOption(gameRenderables, gameRules, GameRules.RULE_DOBLOCKDROPS);
        addBooleanGameRuleOption(gameRenderables, gameRules, GameRules.RULE_NATURAL_REGENERATION);
        addBooleanGameRuleOption(gameRenderables, gameRules, GameRules.RULE_DO_IMMEDIATE_RESPAWN);
    }

    private EditBox createSeedEditBox(CreateWorldScreen parent) {
        EditBox editBox = new EditBox(Minecraft.getInstance().font, 0, 0, 308, 20, ENTER_SEED) {
            protected MutableComponent createNarrationMessage() {
                return super.createNarrationMessage().append(CommonComponents.NARRATION_SEPARATOR).append(SEED_INFO);
            }
        };
        editBox.setTooltip(Tooltip.create(ENTER_SEED_DESCRIPTION));
        editBox.setValue(parent.getUiState().getSeed());
        editBox.setResponder(string -> parent.getUiState().setSeed(editBox.getValue()));
        return editBox;
    }

    private TickBox createHostPrivilegesTickBox(CreateWorldScreen parent) {
        TickBox hostPrivileges = new TickBox(0, 0, parent.getUiState()./*? if <1.20.5 {*//*isAllowCheats*//*?} else {*/isAllowCommands/*?}*/(), b -> LegacyComponents.HOST_PRIVILEGES, b -> Tooltip.create(LegacyComponents.HOST_PRIVILEGES_INFO), b -> {
            handleHostPrivilegesToggle(parent.getUiState().getGameRules(), b.selected);
            parent.getUiState()./*? if <1.20.5 {*//*setAllowCheats*//*?} else {*/setAllowCommands/*?}*/(b.selected);
        });
        parent.getUiState().addListener(s -> {
            hostPrivileges.active = !s.isDebug() && !s.isHardcore();
            handleHostPrivilegesToggle(s.getGameRules(), s./*? if <1.20.5 {*//*isAllowCheats*//*?} else {*/isAllowCommands/*?}*/());
        });
        return hostPrivileges;
    }

    private Button createCustomizeButton(CreateWorldScreen parent) {
        Component defaultMessage = LegacyOptions.legacySettingsMenus.get()
                ? Component.translatable("legacy.menu.selectWorld.customize_superflat")
                : Component.translatable("selectWorld.customizeType");
        Button customizeButton = new LegacyButton(defaultMessage, button -> {
            PresetEditor presetEditor = parent.getUiState().getPresetEditor();
            if (presetEditor != null)
                minecraft.setScreen(presetEditor.createEditScreen(parent, parent.getUiState().getSettings()));
        });
        parent.getUiState().addListener(s -> {
            customizeButton.active = !s.isDebug() && s.getPresetEditor() != null;
            customizeButton.setMessage(LegacyOptions.legacySettingsMenus.get()
                    ? Component.translatable("legacy.menu.selectWorld.customize_superflat")
                    : Component.translatable("selectWorld.customizeType"));
            customizeButton.setTooltip(Tooltip.create(LegacyComponents.getWorldPresetCustomizeDescription(s.getWorldType().preset())));
        });
        return customizeButton;
    }

    private void addBooleanGameRuleOption(RenderableVList list, GameRules gameRules, GameRules.Key<GameRules.BooleanValue> key) {
        addBooleanGameRuleOption(list, gameRules, key, null);
    }

    private void addBooleanGameRuleOption(RenderableVList list, GameRules gameRules, GameRules.Key<GameRules.BooleanValue> key, BooleanSupplier activeSupplier) {
        GameRules.BooleanValue value = gameRules.getRule(key);
        String descriptionId = key.getDescriptionId();
        Tooltip tooltip = Tooltip.create(Component.translatable(descriptionId + ".description"));
        TickBox tickBox = new TickBox(0, 0, value.get(), b -> LegacyComponents.getMenuGameRuleName(key), b -> tooltip, b -> value.set(b.selected, null));
        if (activeSupplier != null) {
            tickBox.active = activeSupplier.getAsBoolean();
            hostPrivilegesStateUpdaters.add(() -> {
                tickBox.active = activeSupplier.getAsBoolean();
                boolean currentValue = value.get();
                if (tickBox.selected != currentValue) {
                    tickBox.selected = currentValue;
                    tickBox.updateMessage();
                }
            });
        }
        list.addRenderable(tickBox);
    }

    private void handleHostPrivilegesToggle(GameRules gameRules, boolean enabled) {
        if (lastHostPrivilegesState == null) {
            lastHostPrivilegesState = enabled;
            if (enabled) captureHostPrivilegesRuleSnapshot(gameRules);
            return;
        }
        if (lastHostPrivilegesState == enabled) return;
        if (enabled) captureHostPrivilegesRuleSnapshot(gameRules);
        else restoreHostPrivilegesRuleSnapshot(gameRules);
        lastHostPrivilegesState = enabled;
    }

    private void captureHostPrivilegesRuleSnapshot(GameRules gameRules) {
        hostPrivilegesRuleSnapshot.clear();
        for (GameRules.Key<GameRules.BooleanValue> key : HOST_PRIVILEGES_GATED_RULES) {
            hostPrivilegesRuleSnapshot.put(key, gameRules.getRule(key).get());
        }
    }

    private void restoreHostPrivilegesRuleSnapshot(GameRules gameRules) {
        for (GameRules.Key<GameRules.BooleanValue> key : HOST_PRIVILEGES_GATED_RULES) {
            Boolean value = hostPrivilegesRuleSnapshot.get(key);
            if (value != null) {
                gameRules.getRule(key).set(value, null);
            }
        }
    }

    private Component getResetDimensionComponent(ResourceKey<Level> dimension) {
        if (LegacyOptions.legacySettingsMenus.get()) {
            if (Level.NETHER.equals(dimension)) return Component.translatable("legacy.menu.load_save.reset_nether");
            if (Level.END.equals(dimension)) return Component.translatable("legacy.menu.load_save.reset_end");
        }
        return Component.translatable("legacy.menu.load_save.reset", LegacyComponents.getDimensionName(dimension));
    }

    private Tooltip getResetDimensionTooltip(ResourceKey<Level> dimension) {
        return Tooltip.create(Component.translatable("legacy.menu.load_save.reset_" + (Level.NETHER.equals(dimension) ? "nether" : "end") + ".description"));
    }

    private void setWorldPreset(CreateWorldScreen parent, ResourceKey<WorldPreset> presetKey) {
        findWorldTypeEntry(parent, presetKey).ifPresent(parent.getUiState()::setWorldType);
    }

    public static ResourceKey<WorldPreset> getLegacyBiomeScalePreset(Holder<WorldPreset> preset) {
        return preset != null && preset.is(WorldPresets.LARGE_BIOMES) ? WorldPresets.LARGE_BIOMES : WorldPresets.NORMAL;
    }

    private Component getLegacyBiomeScaleName(ResourceKey<WorldPreset> presetKey) {
        return Component.translatable("legacy.menu.selectWorld.biome_scale." + (WorldPresets.LARGE_BIOMES.equals(presetKey) ? "large" : "medium"));
    }

    private Optional<WorldCreationUiState.WorldTypeEntry> findWorldTypeEntry(CreateWorldScreen parent, ResourceKey<WorldPreset> presetKey) {
        return parent.getUiState().getSettings().worldgenLoadContext().lookupOrThrow(Registries.WORLD_PRESET).get(presetKey)
                .map(WorldCreationUiState.WorldTypeEntry::new)
                .or(() -> List.of(parent.getUiState().getNormalPresetList(), parent.getUiState().getAltPresetList()).stream()
                        .flatMap(List::stream)
                        .filter(entry -> isPreset(entry.preset(), presetKey))
                        .findFirst());
    }

    private boolean isPresetSelected(CreateWorldScreen parent, ResourceKey<WorldPreset> presetKey) {
        return isPreset(parent.getUiState().getWorldType().preset(), presetKey);
    }

    private boolean isPreset(Holder<WorldPreset> preset, ResourceKey<WorldPreset> presetKey) {
        return preset != null && preset.is(presetKey);
    }

    public WorldMoreOptionsScreen(LoadSaveScreen parent) {
        super(parent,
                s -> LegacyOptions.legacySettingsMenus.get()
                        ? Panel.createPanel(s,
                                p -> p.appearance(244, ((WorldMoreOptionsScreen) s).getLegacyPanelHeight(199, true)),
                                p -> p.pos(p.centeredLeftPos(s), (s.height - 199) / 2))
                        : Panel.centered(s, 244, 199),
                Component.translatable("createWorld.tab.more.title"));
        renderableVLists.add(gameRenderables);
        tabList.setSelected(1);
        if (LegacyOptions.legacySettingsMenus.get()) {
            initLegacyLoadSaveOptions(parent);
        } else {
            initDefaultLoadSaveOptions(parent);
        }
        parent.applyGameRules = (g, s) -> {
            GameRules gameRules = parent.summary.getSettings().gameRules();
            if (!g.equals(gameRules)) g.assignFrom(gameRules, s);
        };
    }

    private void initDefaultLoadSaveOptions(LoadSaveScreen parent) {
        GameRules gameRules = parent.summary.getSettings().gameRules();
        LoadSaveScreen.RESETTABLE_DIMENSIONS.forEach(d -> renderableVList.addRenderable(new TickBox(0, 0, parent.dimensionsToReset.contains(d), b -> getResetDimensionComponent(d), b -> getResetDimensionTooltip(d), t -> {
            if (t.selected) parent.dimensionsToReset.add(d);
            else parent.dimensionsToReset.remove(d);
        })));
        renderableVList.addRenderable(new TickBox(0, 0, parent.trustPlayers, b -> Component.translatable("legacy.menu.selectWorld.trust_players"), b -> Tooltip.create(Component.translatable("legacy.menu.selectWorld.trust_players.description")), t -> parent.trustPlayers = t.selected));
        addGameRulesOptions(renderableVList, gameRules, k -> k.getCategory() == GameRules.Category.UPDATES);
        gameRenderables.addRenderable(new TickBox(0, 0, parent.hostPrivileges, b -> LegacyComponents.HOST_PRIVILEGES, b -> Tooltip.create(LegacyComponents.HOST_PRIVILEGES_INFO), b -> parent.hostPrivileges = b.selected));
        for (GameRules.Category value : GameRules.Category.values()) {
            if (value == GameRules.Category.UPDATES) continue;
            addGameRulesOptions(gameRenderables, gameRules, k -> k.getCategory() == value);
        }
    }

    private void initLegacyLoadSaveOptions(LoadSaveScreen parent) {
        GameRules gameRules = parent.summary.getSettings().gameRules();
        LoadSaveScreen.RESETTABLE_DIMENSIONS.forEach(d -> renderableVList.addRenderable(new TickBox(0, 0, parent.dimensionsToReset.contains(d), b -> getResetDimensionComponent(d), b -> getResetDimensionTooltip(d), t -> {
            if (t.selected) parent.dimensionsToReset.add(d);
            else parent.dimensionsToReset.remove(d);
        })));
        renderableVList.addRenderable(new TickBox(0, 0, parent.trustPlayers, b -> Component.translatable("legacy.menu.selectWorld.trust_players"), b -> Tooltip.create(Component.translatable("legacy.menu.selectWorld.trust_players.description")), t -> parent.trustPlayers = t.selected));
        addBooleanGameRuleOption(renderableVList, gameRules, GameRules.RULE_DOFIRETICK);
        addBooleanGameRuleOption(renderableVList, gameRules, LegacyGameRules.getTntExplodes());

        gameRenderables.addRenderable(new TickBox(0, 0, 200, parent.publishScreen.publish, b -> PublishScreen.getPublishComponent(), b -> PublishScreen.getPublishTooltip(), b -> {
            if (b.selected) parent.publishScreen.setGameType(parent.gameTypeSlider.getObjectValue());
            parent.publishScreen.publish = b.selected;
        }, () -> parent.publishScreen.publish));
        addBooleanGameRuleOption(gameRenderables, gameRules, LegacyGameRules.getPvp());
        handleHostPrivilegesToggle(gameRules, parent.hostPrivileges);
        gameRenderables.addRenderable(new TickBox(0, 0, parent.hostPrivileges, b -> LegacyComponents.HOST_PRIVILEGES, b -> Tooltip.create(LegacyComponents.HOST_PRIVILEGES_INFO), b -> {
            handleHostPrivilegesToggle(gameRules, b.selected);
            parent.hostPrivileges = b.selected;
        }));
        addBooleanGameRuleOption(gameRenderables, gameRules, GameRules.RULE_DAYLIGHT, () -> parent.hostPrivileges);
        addBooleanGameRuleOption(gameRenderables, gameRules, GameRules.RULE_WEATHER_CYCLE, () -> parent.hostPrivileges);
        addBooleanGameRuleOption(gameRenderables, gameRules, GameRules.RULE_KEEPINVENTORY, () -> parent.hostPrivileges);
        addBooleanGameRuleOption(gameRenderables, gameRules, GameRules.RULE_DOMOBSPAWNING, () -> parent.hostPrivileges);
        addBooleanGameRuleOption(gameRenderables, gameRules, GameRules.RULE_MOBGRIEFING, () -> parent.hostPrivileges);
        addBooleanGameRuleOption(gameRenderables, gameRules, GameRules.RULE_DOMOBLOOT);
        addBooleanGameRuleOption(gameRenderables, gameRules, GameRules.RULE_DOBLOCKDROPS);
        addBooleanGameRuleOption(gameRenderables, gameRules, GameRules.RULE_NATURAL_REGENERATION);
        addBooleanGameRuleOption(gameRenderables, gameRules, GameRules.RULE_DO_IMMEDIATE_RESPAWN);
    }

    protected int getLegacyPanelHeight(int baseHeight, boolean shrinkOnly) {
        if (!LegacyOptions.legacySettingsMenus.get()) return baseHeight;

        int contentHeight = 20;
        int entryCount = 0;
        for (Renderable renderable : getRenderableVList().renderables) {
            if (renderable instanceof LayoutElement element) {
                contentHeight += element.getHeight();
                entryCount++;
            }
        }
        if (entryCount > 1) contentHeight += (entryCount - 1) * 3;
        return shrinkOnly ? Math.min(baseHeight, contentHeight) : Math.max(baseHeight, contentHeight);
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
            boolean compactLegacyTooltip = LegacyOptions.legacySettingsMenus.get() && !sd;
            int tooltipContentPadding = sd ? 20 : compactLegacyTooltip ? 36 : 44;

            MultiLineLabel label = message == null ? null : (sd ? Panel.sdLabelsCache : Panel.labelsCache).apply(message, tooltipBox.getWidth() - 10);

            int lineHeight = sd ? 8 : compactLegacyTooltip ? 9 : 12;

            scrollableRenderer.lineHeight = lineHeight;

            if (label == null)
                scrollableRenderer.resetScrolled();
            else
                scrollableRenderer.scrolled.max = Math.max(0, label.getLineCount() - (tooltipBox.getHeight() - tooltipContentPadding) / lineHeight);

            tooltipBox.render(guiGraphics, i, j, f);
            if (label != null) {
                scrollableRenderer.render(guiGraphics, panel.x + panel.width + 3, panel.y + 13, tooltipBox.width - 10, tooltipBox.getHeight() - tooltipContentPadding, () -> label.render(guiGraphics, MultiLineLabel.Align.LEFT, panel.x + panel.width + 3, panel.y + 13, lineHeight, true, 0xFFFFFFFF));
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
        hostPrivilegesStateUpdaters.forEach(Runnable::run);
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
