package wily.legacy.client.screen;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonElement;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.Lifecycle;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.GenericDirtMessageScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.minecraft.client.gui.screens.packs.PackSelectionScreen;
import net.minecraft.client.gui.screens.worldselection.*;
import net.minecraft.commands.Commands;
import net.minecraft.core.registries.Registries;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.WorldLoader;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.DataPackConfig;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldGenSettings;
import org.slf4j.Logger;
import wily.legacy.client.controller.ControllerBinding;
import wily.legacy.util.ScreenUtil;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import static wily.legacy.client.screen.ControlTooltip.CONTROL_ACTION_CACHE;

public class WorldMoreOptionsScreen extends PanelVListScreen {
    protected MultiLineLabel tooltipBoxLabel;
    protected ScrollableRenderer scrollableRenderer =  new ScrollableRenderer(new LegacyScrollRenderer());

    protected final TabList tabList = new TabList().add(29,0,Component.translatable("createWorld.tab.world.title"), t-> rebuildWidgets()).add(29,2,Component.translatable("legacy.menu.game_options"), t-> rebuildWidgets());

    protected final RenderableVList gameRenderables = new RenderableVList();

    protected Runnable onClose = ()->{};
    protected WorldMoreOptionsScreen(Screen parent, Function<Panel,Integer> posHeight) {
        super(s -> new Panel(p -> (s.width - (p.width + (ScreenUtil.hasTooltipBoxes() ? 188 : 0))) / 2, p -> (s.height - posHeight.apply(p)) / 2, 244, 199), Component.translatable("createWorld.tab.more.title"));
        this.parent = parent;
        controlTooltipRenderer.addCompound(()-> new Component[]{ControlTooltip.getActiveType().isKeyboard() ? ControlTooltip.getKeyIcon(InputConstants.KEY_LBRACKET,true) : ControllerBinding.LEFT_BUMPER.bindingState.getIcon(true),ControlTooltip.SPACE,ControlTooltip.getActiveType().isKeyboard() ? ControlTooltip.getKeyIcon(InputConstants.KEY_RBRACKET,true) : ControllerBinding.RIGHT_BUMPER.bindingState.getIcon(true)},()->CONTROL_ACTION_CACHE.getUnchecked(tabList.selectedTab == 0 ?  "legacy.menu.game_options" : "createWorld.tab.world.title"));
    }
    public WorldMoreOptionsScreen(CreateWorldScreen parent, Consumer<Boolean> setTrustPlayers) {
        this(parent, p-> p.height);
       renderableVList.addRenderable(SimpleLayoutRenderable.create(0,9,r-> ((guiGraphics, i, j, f) -> guiGraphics.drawString(Minecraft.getInstance().font, Component.translatable("selectWorld.enterSeed"),r.x + 1,r.y + 2,0x383838,false))));
        EditBox editBox = new EditBox(Minecraft.getInstance().font, 308, 20, Component.translatable("selectWorld.enterSeed")){
           protected MutableComponent createNarrationMessage() {
               return super.createNarrationMessage().append(CommonComponents.NARRATION_SEPARATOR).append(Component.translatable("selectWorld.seedInfo"));
           }
       };
        editBox.setValue(parent.getUiState().getSeed());
        editBox.setResponder(string -> parent.getUiState().setSeed(editBox.getValue()));
        renderableVList.addRenderable(editBox);
        renderableVList.addRenderable(SimpleLayoutRenderable.create(0,9,r-> ((guiGraphics, i, j, f) -> guiGraphics.drawString(Minecraft.getInstance().font, Component.translatable("selectWorld.seedInfo"),r.x + 1,r.y + 2,0x383838,false))));
        renderableVList.addRenderable(new TickBox(0,0,parent.getUiState().isGenerateStructures(),b-> Component.translatable("selectWorld.mapFeatures"),b-> Tooltip.create(Component.translatable("selectWorld.mapFeatures.info")),b->parent.getUiState().setGenerateStructures(b.selected)));
        renderableVList.addRenderable(new TickBox(0,0,parent.getUiState().isBonusChest(),b-> Component.translatable("selectWorld.bonusItems"),b-> null,b->parent.getUiState().setBonusChest(b.selected)));
        renderableVList.addRenderable(new LegacySliderButton<>(0, 0, 0, 16, s -> s.getDefaultMessage(Component.translatable("selectWorld.mapType"), parent.getUiState().getWorldType().describePreset()), () -> parent.getUiState().getWorldType().isAmplified() ? Tooltip.create(Component.translatable("generator.minecraft.amplified.info")) : null, parent.getUiState().getWorldType(), () -> hasAltDown() ? parent.getUiState().getAltPresetList() : parent.getUiState().getNormalPresetList(), b -> parent.getUiState().setWorldType(b.objectValue)));
        Button customizeButton = Button.builder(Component.translatable("selectWorld.customizeType"), button -> {
            PresetEditor presetEditor = parent.getUiState().getPresetEditor();
            if (presetEditor != null)
                minecraft.setScreen(presetEditor.createEditScreen(parent, parent.getUiState().getSettings()));
        }).build();
        parent.getUiState().addListener( s->customizeButton.active = !s.isDebug() && s.getPresetEditor() != null);
        renderableVList.addRenderable(customizeButton);
        SimpleLayoutRenderable.create(0,9,r-> ((guiGraphics, i, j, f) -> {}));
        TickBox hostPrivilleges = new TickBox(0,0,parent.getUiState().isAllowCheats(),b->Component.translatable("selectWorld.allowCommands"),b->Tooltip.create(Component.translatable("selectWorld.allowCommands.info")),b->parent.getUiState().setAllowCheats(b.selected));
        parent.getUiState().addListener(s-> hostPrivilleges.active = !s.isDebug() && !s.isHardcore());
        GameRules gameRules = parent.getUiState().getGameRules();
        Pair<Path,PackRepository> pair = parent.getDataPackSelectionSettings(parent.getUiState().getSettings().dataConfiguration());
        if (pair != null){
            renderableVList.addRenderable(SimpleLayoutRenderable.create(0,9,r-> ((guiGraphics, i, j, f) -> guiGraphics.drawString(Minecraft.getInstance().font, Component.translatable("selectWorld.experiments"),r.x + 1,r.y + 2,0x383838,false))));
            PackRepository dataRepository = pair.getSecond();
            List<String> selectedExperiments = new ArrayList<>(dataRepository.getSelectedIds());
            dataRepository.getAvailablePacks().forEach(p->{
                if (p.getPackSource()!= PackSource.FEATURE) return;
                String id = "dataPack." + p.getId() + ".name";
                Component name = Language.getInstance().has(id) ? Component.translatable(id) : p.getTitle();
                renderableVList.addRenderable(new TickBox(0,0,selectedExperiments.contains(p.getId()),b-> name,b->new MultilineTooltip(178,p.getDescription()),b->{
                    if (b.selected && !selectedExperiments.contains(p.getId())) selectedExperiments.add(p.getId());
                    else if (!b.selected) selectedExperiments.remove(p.getId());
                }));
            });
            onClose = ()->{
                if (!dataRepository.getSelectedIds().equals(selectedExperiments)) {
                    dataRepository.setSelected(selectedExperiments);
                    parent.tryApplyNewDataPacks(dataRepository, false, w -> minecraft.setScreen(this));
                }
            };
        }
        renderableVList.addRenderable(Button.builder(Component.translatable("selectWorld.dataPacks"), button -> openDataPackSelectionScreen(parent, parent.getUiState().getSettings().dataConfiguration())).build());
        renderableVList.addRenderable(new TickBox(0,0,parent.getUiState().isAllowCheats(), b-> Component.translatable("legacy.menu.selectWorld.trust_players"),b-> null,t-> setTrustPlayers.accept(t.selected)));
        addGameRulesOptions(renderableVList,gameRules, k-> k.getCategory() == GameRules.Category.UPDATES);
        gameRenderables.addRenderable(hostPrivilleges);
        for (GameRules.Category value : GameRules.Category.values()) {
            if (value == GameRules.Category.UPDATES) continue;
            addGameRulesOptions(gameRenderables,gameRules, k-> k.getCategory() == value);
        }
        parent.getUiState().onChanged();
    }
    public void addGameRulesOptions(RenderableVList list, GameRules gameRules, Predicate<GameRules.Key<?>> allowGamerule){
        GameRules.visitGameRuleTypes(new GameRules.GameRuleTypeVisitor() {

            @Override
            public void visitBoolean(GameRules.Key<GameRules.BooleanValue> key, GameRules.Type<GameRules.BooleanValue> type) {
                if (!allowGamerule.test(key)) return;
                GameRules.BooleanValue value = gameRules.getRule(key);
                GameRules.BooleanValue defaultValue = type.createRule();
                Component tooltip = Component.translatable(key.getDescriptionId() + ".description");
                Component valueTooltip = Component.translatable("editGamerule.default", defaultValue.serialize()).withStyle(ChatFormatting.GRAY);
                list.addRenderable(new TickBox(0,0,gameRules.getRule(key).get(),b-> Component.translatable(key.getDescriptionId()),b-> new MultilineTooltip(178, tooltip, valueTooltip), b->value.set(b.selected,null)));
            }

            @Override
            public void visitInteger(GameRules.Key<GameRules.IntegerValue> key, GameRules.Type<GameRules.IntegerValue> type) {
                if (!allowGamerule.test(key)) return;
                GameRules.IntegerValue value = gameRules.getRule(key);
                GameRules.IntegerValue defaultValue = type.createRule();
                Component tooltip = Component.translatable(key.getDescriptionId() + ".description");
                Component valueTooltip = Component.translatable("editGamerule.default", defaultValue.serialize()).withStyle(ChatFormatting.GRAY);
                EditBox integerEdit = new EditBox(Minecraft.getInstance().font,220,20,Component.translatable(key.getDescriptionId()));
                integerEdit.setTooltip(new MultilineTooltip(178,tooltip,valueTooltip));
                integerEdit.setValue(Integer.toString(value.get()));
                integerEdit.setFilter(value::tryDeserialize);
                integerEdit.setResponder(string -> value.set(Integer.parseInt(string),null));
                list.addRenderable(SimpleLayoutRenderable.create(0,9,r-> ((guiGraphics, i, j, f) -> guiGraphics.drawString(Minecraft.getInstance().font, Component.translatable(key.getDescriptionId()),r.x + 1,r.y + 2,0x383838,false))));
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
    public void setTooltipForNextRenderPass(Tooltip tooltip, ClientTooltipPositioner clientTooltipPositioner, boolean bl) {
        if (ScreenUtil.hasTooltipBoxes())
            tooltipBoxLabel =  MultiLineLabel.createFixed(font, tooltip.toCharSequence(minecraft).stream().map(formattedCharSequence -> new MultiLineLabel.TextWithWidth(formattedCharSequence, font.width(formattedCharSequence))).toList());
        else super.setTooltipForNextRenderPass(tooltip, clientTooltipPositioner, bl);
    }

    public WorldMoreOptionsScreen(LoadSaveScreen parent) {
        this(parent, p-> 199);
        tabList.selectedTab = 1;
        GameRules gameRules = parent.summary.getSettings().gameRules();
        renderableVList.addRenderable(new TickBox(0,0,parent.resetNether, b-> Component.translatable("legacy.menu.load_save.reset_nether"),b-> null,t-> parent.resetNether = t.selected));
        renderableVList.addRenderable(new TickBox(0,0,parent.resetEnd, b-> Component.translatable("legacy.menu.load_save.reset_end"),b-> null,t-> parent.resetEnd = t.selected));
        renderableVList.addRenderable(new TickBox(0,0,parent.trustPlayers, b-> Component.translatable("legacy.menu.selectWorld.trust_players"),b-> null,t-> parent.trustPlayers = t.selected));
        addGameRulesOptions(renderableVList,gameRules, k-> k.getCategory() == GameRules.Category.UPDATES);
        gameRenderables.addRenderable(new TickBox(0,0,parent.allowCheats,b->Component.translatable("selectWorld.allowCommands"),b->Tooltip.create(Component.translatable("selectWorld.allowCommands.info")),b-> parent.allowCheats = b.selected));
        for (GameRules.Category value : GameRules.Category.values()) {
            if (value == GameRules.Category.UPDATES) continue;
            addGameRulesOptions(gameRenderables,gameRules, k-> k.getCategory() == value);
        }
        parent.applyGameRules = (g,s)->{
          if (!g.equals(gameRules)) g.assignFrom(gameRules,s);
        };
    }

    @Override
    public void renderDefaultBackground(GuiGraphics guiGraphics, int i, int j, float f) {
        ScreenUtil.renderDefaultBackground(guiGraphics,false);
        if (ScreenUtil.hasTooltipBoxes()) {
            if (tooltipBoxLabel != null && getChildAt(i,j).map(g-> g instanceof AbstractWidget w ? w.getTooltip() : null).isEmpty() && (!(getFocused() instanceof AbstractWidget w) || w.getTooltip() == null)) tooltipBoxLabel = null;
            ScreenUtil.renderPointerPanel(guiGraphics,panel.x + panel.width - 2, panel.y + 5,188,panel.height - 10);
            if (tooltipBoxLabel != null) tooltipBoxLabel.renderLeftAligned(guiGraphics, panel.x + panel.width + 3, panel.y + 13,12,0xFFFFFF);
        }
    }

    @Override
    public boolean keyPressed(int i, int j, int k) {
        tabList.controlTab(i);
        return super.keyPressed(i, j, k);
    }

    @Override
    public RenderableVList getRenderableVList() {
        if (tabList.selectedTab == 1) return gameRenderables;
        return super.getRenderableVList();
    }

    @Override
    protected void init() {
        addRenderableWidget(tabList);
        super.init();
        tabList.init(panel.x,panel.y - 23,panel.width);
    }
    void openDataPackSelectionScreen(CreateWorldScreen screen, WorldDataConfiguration worldDataConfiguration) {
        Pair<Path, PackRepository> pair = screen.getDataPackSelectionSettings(worldDataConfiguration);
        if (pair != null) {
            this.minecraft.setScreen(new PackSelectionScreen(pair.getSecond(), packRepository -> screen.tryApplyNewDataPacks(packRepository, true, d-> openDataPackSelectionScreen(screen,d)), pair.getFirst(), Component.translatable("dataPack.title")));
        }
    }


}
