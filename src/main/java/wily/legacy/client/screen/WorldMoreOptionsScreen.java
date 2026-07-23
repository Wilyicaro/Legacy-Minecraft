package wily.legacy.client.screen;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.datafixers.util.Pair;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.minecraft.client.gui.screens.packs.PackSelectionScreen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.PresetEditor;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.Holder;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.presets.WorldPreset;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import wily.factoryapi.base.Bearer;
import wily.factoryapi.base.client.DatapackRepositoryAccessor;
import wily.factoryapi.base.client.SimpleLayoutRenderable;
import wily.factoryapi.base.client.WidgetAccessor;
import wily.legacy.client.CommonColor;
import wily.legacy.client.ControlType;
import wily.legacy.client.LegacyOptions;
import wily.legacy.client.controller.ControllerBinding;
import wily.legacy.init.LegacyGameRules;
import wily.legacy.util.LegacyComponents;
import wily.legacy.util.ScreenUtil;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class WorldMoreOptionsScreen extends PanelVListScreen implements ControlTooltip.Event, DatapackRepositoryAccessor {
    protected List<FormattedCharSequence> tooltipBoxLabel;
    protected boolean expandedResetDimensionTooltip;
    protected ScrollableRenderer scrollableRenderer =  new ScrollableRenderer(new LegacyScrollRenderer());

    protected final TabList tabList = new TabList(accessor).add(30, LegacyTabButton.Type.LEFT,Component.translatable("createWorld.tab.world.title"), t-> rebuildWidgets()).add(30, LegacyTabButton.Type.RIGHT,Component.translatable("legacy.menu.game_options"), t-> rebuildWidgets());

    protected final RenderableVList gameRenderables = new RenderableVList(accessor);

    protected final Panel tooltipBox = Panel.tooltipBoxOf(panel, () -> LegacyOptions.getUIMode().isSD() ? 106 : 188);
    protected Runnable onClose = ()->{};
    protected final Supplier<WorldMoreOptionsScreen> advancedOptionsScreen;

    public static final Component ENTER_SEED = Component.translatable("selectWorld.enterSeed");
    public static final Component SEED_INFO = Component.translatable("selectWorld.seedInfo");
    public static final Component ENTER_SEED_DESCRIPTION = Component.translatable("legacy.menu.selectWorld.enterSeed.description");
    private static final List<ResourceKey<WorldPreset>> LEGACY_BIOME_SCALE_PRESETS = List.of(WorldPresets.NORMAL, WorldPresets.LARGE_BIOMES);

    @Override
    public void addControlTooltips(ControlTooltip.Renderer renderer) {
        super.addControlTooltips(renderer);
        renderer.addCompound(()-> new ControlTooltip.Icon[]{ControlType.getActiveType().isKbm() ? ControlTooltip.getKeyIcon(InputConstants.KEY_LBRACKET) : ControllerBinding.LEFT_BUMPER.getIcon(),ControlTooltip.SPACE_ICON, ControlType.getActiveType().isKbm() ? ControlTooltip.getKeyIcon(InputConstants.KEY_RBRACKET) : ControllerBinding.RIGHT_BUMPER.getIcon()},()->tabList.selectedTab == 0 ? LegacyComponents.GAME_OPTIONS : LegacyComponents.WORLD_OPTIONS);
    }

    public WorldMoreOptionsScreen(CreateWorldScreen parent, Bearer<Boolean> trustPlayers) {
        this(parent, trustPlayers, Bearer.of(false), Bearer.of(WorldPresets.NORMAL));
    }

    public WorldMoreOptionsScreen(CreateWorldScreen parent, Bearer<Boolean> trustPlayers, Bearer<Boolean> onlineGame, Bearer<ResourceKey<WorldPreset>> biomeScale) {
        super(parent,244, 199, Component.translatable("createWorld.tab.more.title"));
        advancedOptionsScreen = () -> new WorldMoreOptionsScreen(parent, trustPlayers, onlineGame, biomeScale);
        renderableVLists.add(gameRenderables);
        if (LegacyOptions.useLegacyWorldOptions()) initLegacyCreateWorldOptions(parent, trustPlayers, onlineGame, biomeScale);
        else initDefaultCreateWorldOptions(parent, trustPlayers);
        parent.getUiState().onChanged();
    }

    private void initDefaultCreateWorldOptions(CreateWorldScreen parent, Bearer<Boolean> trustPlayers) {
        renderableVList.addCategory(ENTER_SEED);
        EditBox editBox = createSeedEditBox(parent);
        renderableVList.addRenderable(editBox);
        renderableVList.addCategory(SEED_INFO);
        renderableVList.addRenderable(new TickBox(0,0,parent.getUiState().isGenerateStructures(),b-> Component.translatable("selectWorld.mapFeatures"),b-> tooltip(Component.translatable("selectWorld.mapFeatures.info")),b->parent.getUiState().setGenerateStructures(b.selected)));
        renderableVList.addRenderable(new TickBox(0,0,parent.getUiState().isBonusChest(),b-> Component.translatable("selectWorld.bonusItems"),b-> tooltip(Component.translatable("legacy.menu.selectWorld.bonusItems.description")),b->parent.getUiState().setBonusChest(b.selected)));
        renderableVList.addRenderable(new LegacySliderButton<>(0, 0, 0, 16, s -> s.getDefaultMessage(Component.translatable("selectWorld.mapType"), parent.getUiState().getWorldType().describePreset()), b -> parent.getUiState().getWorldType().isAmplified() ? tooltip(Component.translatable("generator.minecraft.amplified.info")) : null, parent.getUiState().getWorldType(), () -> hasAltDown() ? parent.getUiState().getAltPresetList() : parent.getUiState().getNormalPresetList(), b -> parent.getUiState().setWorldType(b.objectValue)));
        renderableVList.addRenderable(createCustomizeButton(parent));
        renderableVList.addRenderable(SimpleLayoutRenderable.create(0,9,r-> ((guiGraphics, i, j, f) -> {})));
        TickBox hostPrivileges = createHostPrivilegesTickBox(parent);
        GameRules gameRules = parent.getUiState().getGameRules();
        Pair<Path,PackRepository> pair = parent.getDataPackSelectionSettings(parent.getUiState().getSettings().dataConfiguration());
        if (pair != null){
            renderableVList.addCategory(Component.translatable("selectWorld.experiments"));
            PackRepository dataRepository = pair.getSecond();
            List<String> selectedExperiments = new ArrayList<>(dataRepository.getSelectedIds());
            dataRepository.getAvailablePacks().forEach(p->{
                if (p.getPackSource()!= PackSource.FEATURE) return;
                String id = "dataPack." + p.getId() + ".name";
                Component name = Language.getInstance().has(id) ? Component.translatable(id) : p.getTitle();
                renderableVList.addRenderable(new TickBox(0,0,selectedExperiments.contains(p.getId()),b-> name,b->new MultilineTooltip(tooltipBox.getWidth() - 10,p.getDescription()),b->{
                    if (b.selected && !selectedExperiments.contains(p.getId())) selectedExperiments.add(p.getId());
                    else if (!b.selected) selectedExperiments.remove(p.getId());
                }));
            });
            onClose = ()->{
                dataRepository.setSelected(selectedExperiments);
                parent.tryApplyNewDataPacks(dataRepository, false, w -> minecraft.setScreen(this));
            };
        }
        renderableVList.addRenderable(Button.builder(Component.translatable("selectWorld.dataPacks"), button -> openDataPackSelectionScreen(parent, parent.getUiState().getSettings().dataConfiguration())).build());
        renderableVList.addRenderable(new TickBox(0,0, trustPlayers.get(), b-> Component.translatable("legacy.menu.selectWorld.trust_players"),b-> tooltip(Component.translatable("legacy.menu.selectWorld.trust_players.description")),t-> trustPlayers.set(t.selected)));
        addGameRulesOptions(renderableVList,gameRules, k-> k.getCategory() == GameRules.Category.UPDATES);
        gameRenderables.addRenderable(hostPrivileges);
        for (GameRules.Category value : GameRules.Category.values()) {
            if (value == GameRules.Category.UPDATES) continue;
            addGameRulesOptions(gameRenderables,gameRules, k-> k.getCategory() == value);
        }
    }

    private void initLegacyCreateWorldOptions(CreateWorldScreen parent, Bearer<Boolean> trustPlayers, Bearer<Boolean> onlineGame, Bearer<ResourceKey<WorldPreset>> biomeScale) {
        renderableVList.addCategory(ENTER_SEED);
        renderableVList.addRenderable(createSeedEditBox(parent));
        renderableVList.addCategory(SEED_INFO);

        TickBox amplifiedWorld = new TickBox(0,0,isPresetSelected(parent, WorldPresets.AMPLIFIED),b-> Component.translatable("legacy.menu.selectWorld.amplified_world"),b-> tooltip(Component.translatable("generator.minecraft.amplified.info")),b-> {
            if (b.selected) setWorldPreset(parent, WorldPresets.AMPLIFIED);
            else setWorldPreset(parent, biomeScale.get());
        });
        TickBox superflatWorld = new TickBox(0,0,isPresetSelected(parent, WorldPresets.FLAT),b-> Component.translatable("legacy.menu.selectWorld.superflat_world"),b-> null,b-> {
            if (b.selected) setWorldPreset(parent, WorldPresets.FLAT);
            else setWorldPreset(parent, biomeScale.get());
        });
        parent.getUiState().addListener(s -> {
            amplifiedWorld.selected = isPreset(s.getWorldType().preset(), WorldPresets.AMPLIFIED);
            superflatWorld.selected = isPreset(s.getWorldType().preset(), WorldPresets.FLAT);
            if (!amplifiedWorld.selected && !superflatWorld.selected) biomeScale.set(getLegacyBiomeScalePreset(s.getWorldType().preset()));
        });
        renderableVList.addRenderable(amplifiedWorld);
        renderableVList.addRenderable(SimpleLayoutRenderable.create(0,9,r-> ((guiGraphics, i, j, f) -> {})));
        renderableVList.addRenderable(new LegacySliderButton<>(0, 0, 0, 16, b -> b.getDefaultMessage(Component.translatable("legacy.menu.selectWorld.biome_scale"), getLegacyBiomeScaleName(b.getObjectValue())), b -> tooltip(Component.translatable("legacy.menu.selectWorld.biome_scale.description")), biomeScale.get(), () -> LEGACY_BIOME_SCALE_PRESETS, b -> {
            biomeScale.set(b.getObjectValue());
            setWorldPreset(parent, b.getObjectValue());
        }));
        renderableVList.addRenderable(new TickBox(0,0,parent.getUiState().isGenerateStructures(),b-> Component.translatable("selectWorld.mapFeatures"),b-> tooltip(Component.translatable("selectWorld.mapFeatures.info")),b->parent.getUiState().setGenerateStructures(b.selected)));
        renderableVList.addRenderable(new TickBox(0,0,parent.getUiState().isBonusChest(),b-> Component.translatable("selectWorld.bonusItems"),b-> tooltip(Component.translatable("legacy.menu.selectWorld.bonusItems.description")),b->parent.getUiState().setBonusChest(b.selected)));
        renderableVList.addRenderable(superflatWorld);
        renderableVList.addRenderable(createCustomizeButton(parent));
        renderableVList.addRenderable(new TickBox(0,0, trustPlayers.get(), b-> Component.translatable("legacy.menu.selectWorld.trust_players"),b-> tooltip(Component.translatable("legacy.menu.selectWorld.trust_players.description")),t-> trustPlayers.set(t.selected)));

        GameRules gameRules = parent.getUiState().getGameRules();
        addBooleanGameRuleOption(renderableVList, gameRules, GameRules.RULE_DOFIRETICK);
        addBooleanGameRuleOption(renderableVList, gameRules, LegacyGameRules.getTntExplodes());

        gameRenderables.addRenderable(new TickBox(0,0,onlineGame.get(), b-> PublishScreen.getPublishComponent(), b-> PublishScreen.getPublishTooltip(), b-> onlineGame.set(b.selected)));
        addBooleanGameRuleOption(gameRenderables, gameRules, LegacyGameRules.PLAYER_VS_PLAYER);
        gameRenderables.addRenderable(createHostPrivilegesTickBox(parent));
        addBooleanGameRuleOption(gameRenderables, gameRules, GameRules.RULE_DAYLIGHT);
        addBooleanGameRuleOption(gameRenderables, gameRules, GameRules.RULE_WEATHER_CYCLE);
        addBooleanGameRuleOption(gameRenderables, gameRules, GameRules.RULE_KEEPINVENTORY);
        addBooleanGameRuleOption(gameRenderables, gameRules, GameRules.RULE_DOMOBSPAWNING);
        addBooleanGameRuleOption(gameRenderables, gameRules, GameRules.RULE_MOBGRIEFING);
        addBooleanGameRuleOption(gameRenderables, gameRules, GameRules.RULE_DOMOBLOOT);
        addBooleanGameRuleOption(gameRenderables, gameRules, GameRules.RULE_DOBLOCKDROPS);
        addBooleanGameRuleOption(gameRenderables, gameRules, GameRules.RULE_NATURAL_REGENERATION);
        addBooleanGameRuleOption(gameRenderables, gameRules, GameRules.RULE_DO_IMMEDIATE_RESPAWN);
    }

    private EditBox createSeedEditBox(CreateWorldScreen parent) {
        EditBox editBox = new EditBox(Minecraft.getInstance().font, 0, 0, 308, 20, ENTER_SEED){
            protected MutableComponent createNarrationMessage() {
                return super.createNarrationMessage().append(CommonComponents.NARRATION_SEPARATOR).append(SEED_INFO);
            }
        };
        editBox.setTooltip(tooltip(ENTER_SEED_DESCRIPTION));
        editBox.setValue(parent.getUiState().getSeed());
        editBox.setResponder(string -> parent.getUiState().setSeed(editBox.getValue()));
        return editBox;
    }

    private TickBox createHostPrivilegesTickBox(CreateWorldScreen parent) {
        TickBox hostPrivileges = new TickBox(0,0,parent.getUiState()./*? if <1.20.5 {*//*isAllowCheats*//*?} else {*/isAllowCommands/*?}*/(),b->LegacyComponents.HOST_PRIVILEGES,b->tooltip(LegacyComponents.HOST_PRIVILEGES_INFO),b->parent.getUiState()./*? if <1.20.5 {*//*setAllowCheats*//*?} else {*/setAllowCommands/*?}*/(b.selected));
        parent.getUiState().addListener(s-> hostPrivileges.active = !s.isDebug() && !s.isHardcore());
        return hostPrivileges;
    }

    private Button createCustomizeButton(CreateWorldScreen parent) {
        Button customizeButton = Button.builder(LegacyOptions.useLegacyWorldOptions() ? Component.translatable("legacy.menu.selectWorld.customize_superflat") : Component.translatable("selectWorld.customizeType"), button -> {
            PresetEditor presetEditor = parent.getUiState().getPresetEditor();
            if (presetEditor != null)
                minecraft.setScreen(presetEditor.createEditScreen(parent, parent.getUiState().getSettings()));
        }).build();
        parent.getUiState().addListener(s -> {
            customizeButton.active = !s.isDebug() && s.getPresetEditor() != null;
            customizeButton.setMessage(LegacyOptions.useLegacyWorldOptions() ? Component.translatable("legacy.menu.selectWorld.customize_superflat") : Component.translatable("selectWorld.customizeType"));
        });
        return customizeButton;
    }

    private void addBooleanGameRuleOption(RenderableVList list, GameRules gameRules, GameRules.Key<GameRules.BooleanValue> key) {
        GameRules.BooleanValue value = gameRules.getRule(key);
        list.addRenderable(new TickBox(0,0,value.get(), b-> LegacyComponents.getMenuGameRuleName(key), b-> tooltip(Component.translatable(key.getDescriptionId() + ".description")), b-> value.set(b.selected,null)));
    }

    public static ResourceKey<WorldPreset> getLegacyBiomeScalePreset(Holder<WorldPreset> preset) {
        return preset != null && preset.is(WorldPresets.LARGE_BIOMES) ? WorldPresets.LARGE_BIOMES : WorldPresets.NORMAL;
    }

    private Component getLegacyBiomeScaleName(ResourceKey<WorldPreset> presetKey) {
        return Component.translatable("legacy.menu.selectWorld.biome_scale." + (WorldPresets.LARGE_BIOMES.equals(presetKey) ? "large" : "medium"));
    }

    private void setWorldPreset(CreateWorldScreen parent, ResourceKey<WorldPreset> presetKey) {
        findWorldTypeEntry(parent, presetKey).ifPresent(parent.getUiState()::setWorldType);
    }

    private Optional<WorldCreationUiState.WorldTypeEntry> findWorldTypeEntry(CreateWorldScreen parent, ResourceKey<WorldPreset> presetKey) {
        return List.of(parent.getUiState().getNormalPresetList(), parent.getUiState().getAltPresetList()).stream()
                .flatMap(List::stream)
                .filter(entry -> isPreset(entry.preset(), presetKey))
                .findFirst();
    }

    private boolean isPresetSelected(CreateWorldScreen parent, ResourceKey<WorldPreset> presetKey) {
        return isPreset(parent.getUiState().getWorldType().preset(), presetKey);
    }

    private boolean isPreset(Holder<WorldPreset> preset, ResourceKey<WorldPreset> presetKey) {
        return preset != null && preset.is(presetKey);
    }
    public void addGameRulesOptions(RenderableVList list, GameRules gameRules, Predicate<GameRules.Key<?>> allowGamerule){
        gameRules.visitGameRuleTypes(new GameRules.GameRuleTypeVisitor() {

            @Override
            public void visitBoolean(GameRules.Key<GameRules.BooleanValue> key, GameRules.Type<GameRules.BooleanValue> type) {
                if (!allowGamerule.test(key)) return;
                GameRules.BooleanValue value = gameRules.getRule(key);
                GameRules.BooleanValue defaultValue = type.createRule();
                Component tooltip = Component.translatable(key.getDescriptionId() + ".description");
                Component valueTooltip = Component.translatable("editGamerule.default", defaultValue.serialize()).withStyle(ChatFormatting.GRAY);
                list.addRenderable(new TickBox(0,0,gameRules.getRule(key).get(),b-> LegacyComponents.getMenuGameRuleName(key),b-> new MultilineTooltip(tooltipBox.width - 10, tooltip, valueTooltip), b->value.set(b.selected,null)));
            }

            @Override
            public void visitInteger(GameRules.Key<GameRules.IntegerValue> key, GameRules.Type<GameRules.IntegerValue> type) {
                if (!allowGamerule.test(key)) return;
                GameRules.IntegerValue value = gameRules.getRule(key);
                GameRules.IntegerValue defaultValue = type.createRule();
                Component tooltip = Component.translatable(key.getDescriptionId() + ".description");
                Component valueTooltip = Component.translatable("editGamerule.default", defaultValue.serialize()).withStyle(ChatFormatting.GRAY);
                EditBox integerEdit = new EditBox(Minecraft.getInstance().font,0, 0, 220,20,Component.translatable(key.getDescriptionId()));
                integerEdit.setTooltip(new MultilineTooltip(tooltipBox.width - 10,tooltip,valueTooltip));
                integerEdit.setValue(Integer.toString(value.get()));
                integerEdit.setResponder(string -> {
                    if (value.tryDeserialize(string)){
                        integerEdit.setTextColor(14737632);
                        value.set(Integer.parseInt(string), null);
                    } else {
                        integerEdit.setTextColor(-65536);
                    }
                });
                list.addCategory(Component.translatable(key.getDescriptionId()));
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
    public boolean mouseScrolled(double d, double e/*? if >1.20.1 {*/, double f/*?}*/, double g) {
        if (tooltipBox.isHovered(d,e) && scrollableRenderer.mouseScrolled(g)) return true;
        return super.mouseScrolled(d, e/*? if >1.20.1 {*/, f/*?}*/, g);
    }

    @Override
    public void setTooltipForNextRenderPass(Tooltip tooltip, ClientTooltipPositioner clientTooltipPositioner, boolean bl) {
        if (ScreenUtil.hasTooltipBoxes(accessor)) {
            ScreenUtil.applySDFont(ignored -> tooltipBoxLabel = tooltip.toCharSequence(minecraft));
            expandedResetDimensionTooltip = !LegacyOptions.getUIMode().isSD() && tooltip instanceof ResetDimensionTooltip;
            int lineHeight = getTooltipLineHeight();
            int tooltipContentPadding = getTooltipContentPadding(expandedResetDimensionTooltip);
            scrollableRenderer.lineHeight = lineHeight;
            if (expandedResetDimensionTooltip) {
                scrollableRenderer.scrolled.max = 0;
                scrollableRenderer.scrolled.set(0);
            } else {
                scrollableRenderer.scrolled.max = Math.max(0, tooltipBoxLabel.size() - (tooltipBox.getHeight() - tooltipContentPadding) / lineHeight);
            }
        }else super.setTooltipForNextRenderPass(tooltip, clientTooltipPositioner, bl);
    }

    public WorldMoreOptionsScreen(LoadSaveScreen parent) {
        super(parent,
                s -> LegacyOptions.useLegacyWorldOptions()
                        ? Panel.createPanel(s,
                                p -> p.appearance(244, ((WorldMoreOptionsScreen) s).getLegacyPanelHeight(199, true)),
                                p -> p.pos(p.centeredLeftPos(s), (s.height - 199) / 2))
                        : Panel.centered(s, 244, 199),
                Component.translatable("createWorld.tab.more.title"));
        advancedOptionsScreen = () -> new WorldMoreOptionsScreen(parent);
        renderableVLists.add(gameRenderables);
        tabList.selectedTab = 1;
        GameRules gameRules = parent.summary.getSettings().gameRules();
        if (LegacyOptions.useLegacyWorldOptions()) initLegacyLoadSaveOptions(parent, gameRules);
        else initDefaultLoadSaveOptions(parent, gameRules);
        parent.applyGameRules = (g,s)->{
          if (!g.equals(gameRules)) g.assignFrom(gameRules,s);
        };
    }

    private void initDefaultLoadSaveOptions(LoadSaveScreen parent, GameRules gameRules) {
        LoadSaveScreen.RESETTABLE_DIMENSIONS.forEach(d-> renderableVList.addRenderable(new TickBox(0,0, parent.dimensionsToReset.contains(d), b-> Component.translatable("legacy.menu.load_save.reset", LegacyComponents.getDimensionName(d)), b-> null, t-> {
            if (t.selected) parent.dimensionsToReset.add(d);
            else parent.dimensionsToReset.remove(d);
        })));
        renderableVList.addRenderable(new TickBox(0,0,parent.trustPlayers, b-> Component.translatable("legacy.menu.selectWorld.trust_players"),b-> tooltip(Component.translatable("legacy.menu.selectWorld.trust_players.description")),t-> parent.trustPlayers = t.selected));
        addGameRulesOptions(renderableVList,gameRules, k-> k.getCategory() == GameRules.Category.UPDATES);
        gameRenderables.addRenderable(new TickBox(0,0,parent.hostPrivileges, b->LegacyComponents.HOST_PRIVILEGES, b->tooltip(LegacyComponents.HOST_PRIVILEGES_INFO), b-> parent.hostPrivileges = b.selected));
        for (GameRules.Category value : GameRules.Category.values()) {
            if (value == GameRules.Category.UPDATES) continue;
            addGameRulesOptions(gameRenderables,gameRules, k-> k.getCategory() == value);
        }
    }

    private void initLegacyLoadSaveOptions(LoadSaveScreen parent, GameRules gameRules) {
        LoadSaveScreen.RESETTABLE_DIMENSIONS.forEach(d-> renderableVList.addRenderable(new TickBox(0,0, parent.dimensionsToReset.contains(d), b-> getResetDimensionComponent(d), b-> getResetDimensionTooltip(d), t-> {
            if (t.selected) parent.dimensionsToReset.add(d);
            else parent.dimensionsToReset.remove(d);
        })));
        renderableVList.addRenderable(new TickBox(0,0,parent.trustPlayers, b-> Component.translatable("legacy.menu.selectWorld.trust_players"),b-> tooltip(Component.translatable("legacy.menu.selectWorld.trust_players.description")),t-> parent.trustPlayers = t.selected));
        addBooleanGameRuleOption(renderableVList, gameRules, GameRules.RULE_DOFIRETICK);
        addBooleanGameRuleOption(renderableVList, gameRules, LegacyGameRules.getTntExplodes());

        gameRenderables.addRenderable(new TickBox(0,0,parent.publishScreen.publish, b-> PublishScreen.getPublishComponent(), b-> PublishScreen.getPublishTooltip(), b-> {
            if (b.selected) parent.publishScreen.setGameType(parent.gameTypeSlider.getObjectValue());
            parent.publishScreen.publish = b.selected;
        }));
        addBooleanGameRuleOption(gameRenderables, gameRules, LegacyGameRules.PLAYER_VS_PLAYER);
        gameRenderables.addRenderable(new TickBox(0,0,parent.hostPrivileges, b->LegacyComponents.HOST_PRIVILEGES, b->tooltip(LegacyComponents.HOST_PRIVILEGES_INFO), b-> parent.hostPrivileges = b.selected));
        addBooleanGameRuleOption(gameRenderables, gameRules, GameRules.RULE_DAYLIGHT);
        addBooleanGameRuleOption(gameRenderables, gameRules, GameRules.RULE_WEATHER_CYCLE);
        addBooleanGameRuleOption(gameRenderables, gameRules, GameRules.RULE_KEEPINVENTORY);
        addBooleanGameRuleOption(gameRenderables, gameRules, GameRules.RULE_DOMOBSPAWNING);
        addBooleanGameRuleOption(gameRenderables, gameRules, GameRules.RULE_MOBGRIEFING);
        addBooleanGameRuleOption(gameRenderables, gameRules, GameRules.RULE_DOMOBLOOT);
        addBooleanGameRuleOption(gameRenderables, gameRules, GameRules.RULE_DOBLOCKDROPS);
        addBooleanGameRuleOption(gameRenderables, gameRules, GameRules.RULE_NATURAL_REGENERATION);
        addBooleanGameRuleOption(gameRenderables, gameRules, GameRules.RULE_DO_IMMEDIATE_RESPAWN);
    }

    protected int getLegacyPanelHeight(int baseHeight, boolean shrinkOnly) {
        if (!LegacyOptions.useLegacyWorldOptions()) return baseHeight;

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

    private Component getResetDimensionComponent(ResourceKey<Level> dimension) {
        if (LegacyOptions.useLegacyWorldOptions()) {
            if (Level.NETHER.equals(dimension)) return Component.translatable("legacy.menu.load_save.reset_nether");
            if (Level.END.equals(dimension)) return Component.translatable("legacy.menu.load_save.reset_end");
        }
        return Component.translatable("legacy.menu.load_save.reset", LegacyComponents.getDimensionName(dimension));
    }

    private Tooltip getResetDimensionTooltip(ResourceKey<Level> dimension) {
        if (Level.NETHER.equals(dimension)) return new ResetDimensionTooltip(Component.translatable("legacy.menu.load_save.reset_nether.description"), tooltipBox.getWidth() - 10);
        if (Level.END.equals(dimension)) return new ResetDimensionTooltip(Component.translatable("legacy.menu.load_save.reset_end.description"), tooltipBox.getWidth() - 10);
        return null;
    }

    private Tooltip tooltip(Component message) {
        return ScreenUtil.hasTooltipBoxes(accessor) ? new MultilineTooltip(message, tooltipBox.getWidth() - 10) : Tooltip.create(message);
    }

    private int getTooltipLineHeight() {
        return LegacyOptions.getUIMode().isSD() ? 8 : 12;
    }

    private int getTooltipContentPadding(boolean expandedResetDimensionTooltip) {
        int padding = LegacyOptions.getUIMode().isSD() ? 20 : LegacyOptions.useLegacyWorldOptions() ? 36 : 44;
        return expandedResetDimensionTooltip ? Math.max(24, padding - getTooltipLineHeight()) : padding;
    }

    @Override
    public void renderDefaultBackground(GuiGraphics guiGraphics, int i, int j, float f) {
        ScreenUtil.renderDefaultBackground(accessor, guiGraphics, false);
        if (ScreenUtil.hasTooltipBoxes(accessor)) {
            if (tooltipBoxLabel != null && getChildAt(i,j).map(g-> g instanceof AbstractWidget w ? w.getTooltip() : null).isEmpty() && (!(getFocused() instanceof AbstractWidget w) || w.getTooltip() == null)) {
                tooltipBoxLabel = null;
                expandedResetDimensionTooltip = false;
                scrollableRenderer.scrolled.set(0);
            }
            tooltipBox.render(guiGraphics,i,j,f);
            if (tooltipBoxLabel != null) {
                int lineHeight = getTooltipLineHeight();
                int tooltipContentPadding = getTooltipContentPadding(expandedResetDimensionTooltip);
                scrollableRenderer.lineHeight = lineHeight;
                ScreenUtil.applySDFont(ignored -> scrollableRenderer.render(guiGraphics,panel.x + panel.width + 3, panel.y + 13,tooltipBox.width - 10, tooltipBox.getHeight() - tooltipContentPadding, ()-> {
                    for (int line = 0; line < tooltipBoxLabel.size(); line++) {
                        guiGraphics.drawString(font, tooltipBoxLabel.get(line), panel.x + panel.width + 3, panel.y + 13 + line * lineHeight, 0xFFFFFF);
                    }
                }));
            }
        }
    }

    private static class ResetDimensionTooltip extends MultilineTooltip {
        public ResetDimensionTooltip(Component message, int width) {
            super(message, width);
        }
    }

    @Override
    public boolean keyPressed(int i, int j, int k) {
        if (i == InputConstants.KEY_O && LegacyOptions.revealAdvancedWorldOptions()) {
            minecraft.setScreen(advancedOptionsScreen.get());
            return true;
        }
        tabList.controlTab(i);
        return super.keyPressed(i, j, k);
    }

    @Override
    public RenderableVList getRenderableVList() {
        return renderableVLists.get(tabList.selectedTab);
    }

    @Override
    protected void init() {
        addRenderableWidget(tabList);
        super.init();
        addRenderableOnly(tabList::renderSelected);
        tabList.init(panel.x, panel.y - 24, panel.width, 30);
    }

    @Override
    public void renderableVListInit() {
        if (ScreenUtil.hasTooltipBoxes(accessor)) tooltipBox.init();
        super.renderableVListInit();
    }

    @Override
    public void initRenderableVListEntry(RenderableVList renderableVList, Renderable renderable) {
        if (renderable instanceof EditBox editBox) {
            int height = LegacyOptions.getUIMode().isSD() ? 16 : 20;
            //? if <=1.20.1 {
            /*((WidgetAccessor) editBox).setHeight(height);
            *///?} else {
            editBox.setHeight(height);
            //?}
        }
    }

    protected void openDataPackSelectionScreen(CreateWorldScreen screen, WorldDataConfiguration worldDataConfiguration) {
        Pair<Path, PackRepository> pair = screen.getDataPackSelectionSettings(worldDataConfiguration);
        if (pair != null) {
            this.minecraft.setScreen(new PackSelectionScreen(pair.getSecond(), packRepository -> screen.tryApplyNewDataPacks(packRepository, true, d-> openDataPackSelectionScreen(screen,d)), pair.getFirst(), Component.translatable("dataPack.title")));
        }
    }

    @Override
    public PackRepository getDatapackRepository() {
        return parent instanceof CreateWorldScreen screen ? screen.getDataPackSelectionSettings(screen.getUiState().getSettings().dataConfiguration()).getSecond() : null;
    }

    @Override
    public void tryApplyNewDataPacks(PackRepository packRepository) {
        if (parent instanceof CreateWorldScreen screen) {
            screen.tryApplyNewDataPacks(packRepository, false, w-> minecraft.setScreen(this));
        }
    }
}
