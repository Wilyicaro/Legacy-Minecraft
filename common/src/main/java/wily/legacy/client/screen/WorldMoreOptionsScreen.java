package wily.legacy.client.screen;

import com.mojang.datafixers.util.Pair;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.packs.PackSelectionScreen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.PresetEditor;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.WorldDataConfiguration;
import wily.legacy.util.ScreenUtil;
import wily.legacy.util.Stocker;

import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class WorldMoreOptionsScreen extends PanelVListScreen {
    protected final TabList tabList = new TabList().addTabButton(29,0,Component.translatable("createWorld.tab.world.title"),t-> rebuildWidgets()).addTabButton(29,2,Component.translatable("legacy.menu.game_options"), t-> rebuildWidgets());

    protected final RenderableVList gameRenderables = new RenderableVList();

    protected List<GameRules.Key<?>> worldOptionsRules = List.of(GameRules.RULE_MOBGRIEFING, GameRules.RULE_DOFIRETICK, GameRules.RULE_WATER_SOURCE_CONVERSION, GameRules.RULE_LAVA_SOURCE_CONVERSION);

    public WorldMoreOptionsScreen(CreateWorldScreen parent, Consumer<Boolean> setTrustPlayers) {
        super(parent, 244, 199, Component.translatable( "createWorld.tab.more.title"), SimpleLayoutRenderable.create(0,9,r-> ((guiGraphics, i, j, f) -> guiGraphics.drawString(Minecraft.getInstance().font, Component.translatable("selectWorld.enterSeed"),r.x + 1,r.y + 2,0x404040,false))));
       EditBox editBox = new EditBox(Minecraft.getInstance().font, 308, 20, Component.translatable("selectWorld.enterSeed")){
           protected MutableComponent createNarrationMessage() {
               return super.createNarrationMessage().append(CommonComponents.NARRATION_SEPARATOR).append(Component.translatable("selectWorld.seedInfo"));
           }
       };
        editBox.setValue(parent.getUiState().getSeed());
        editBox.setResponder(string -> parent.getUiState().setSeed(editBox.getValue()));
        renderableVList.addRenderable(editBox);
        renderableVList.addRenderable(SimpleLayoutRenderable.create(0,9,r-> ((guiGraphics, i, j, f) -> guiGraphics.drawString(Minecraft.getInstance().font, Component.translatable("selectWorld.seedInfo"),r.x + 1,r.y + 2,0x404040,false))));
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
        TickBox cheatsButton = new TickBox(0,0,parent.getUiState().isAllowCheats(),b->Component.translatable("selectWorld.allowCommands"),b->Tooltip.create(Component.translatable("selectWorld.allowCommands.info")),b->parent.getUiState().setAllowCheats(b.selected));
        parent.getUiState().addListener(s-> cheatsButton.active = !s.isDebug() && !s.isHardcore());
        GameRules gameRules = parent.getUiState().getGameRules();
        renderableVList.addRenderable(new TickBox(0,0,parent.getUiState().isAllowCheats(), b-> Component.translatable("legacy.menu.selectWorld.trust_players"),b-> null,t-> setTrustPlayers.accept(t.selected)));
        addGameRulesOptions(renderableVList,gameRules, worldOptionsRules::contains);
        gameRenderables.addRenderable(Button.builder(ExperimentsScreen.EXPERIMENTS_LABEL, button -> openExperimentsScreen(parent, parent.getUiState().getSettings().dataConfiguration())).build());
        gameRenderables.addRenderable(Button.builder(Component.translatable("selectWorld.dataPacks"), button -> openDataPackSelectionScreen(parent, parent.getUiState().getSettings().dataConfiguration())).build());
        gameRenderables.addRenderable(cheatsButton);
        addGameRulesOptions(gameRenderables,gameRules, k -> !worldOptionsRules.contains(k));
        parent.getUiState().onChanged();
    }
    public void addGameRulesOptions(RenderableVList list, GameRules gameRules, Predicate<GameRules.Key<?>> allowGamerule){
        GameRules.visitGameRuleTypes(new GameRules.GameRuleTypeVisitor() {

            @Override
            public void visitBoolean(GameRules.Key<GameRules.BooleanValue> key, GameRules.Type<GameRules.BooleanValue> type) {
                if (!allowGamerule.test(key)) return;
                GameRules.BooleanValue value = gameRules.getRule(key);
                GameRules.BooleanValue defaultValue = type.createRule();
                list.addRenderable(new TickBox(0,0,gameRules.getRule(key).get(),b-> Component.translatable(key.getDescriptionId()),b-> Tooltip.create(Component.translatable("editGamerule.default", defaultValue.serialize()).withStyle(ChatFormatting.GRAY)), b->value.set(b.selected,null)));
            }

            @Override
            public void visitInteger(GameRules.Key<GameRules.IntegerValue> key, GameRules.Type<GameRules.IntegerValue> type) {
                if (!allowGamerule.test(key)) return;
                GameRules.IntegerValue value = gameRules.getRule(key);
                GameRules.IntegerValue defaultValue = type.createRule();
                EditBox integerEdit = new EditBox(Minecraft.getInstance().font,220,20,Component.translatable(key.getDescriptionId()));
                integerEdit.setTooltip(Tooltip.create(Component.translatable("editGamerule.default", defaultValue.serialize()).withStyle(ChatFormatting.GRAY)));
                integerEdit.setValue(Integer.toString(value.get()));
                integerEdit.setFilter(value::tryDeserialize);
                integerEdit.setResponder(string -> value.set(Integer.parseInt(string),null));
                list.addRenderable(SimpleLayoutRenderable.create(0,9,r-> ((guiGraphics, i, j, f) -> guiGraphics.drawString(Minecraft.getInstance().font, Component.translatable(key.getDescriptionId()),r.x + 1,r.y + 2,0x404040,false))));
                list.addRenderable(integerEdit);
            }
        });
    }
    public WorldMoreOptionsScreen(LoadSaveScreen parent) {
        super(parent, 244, 199, Component.translatable("createWorld.tab.more.title"));
        tabList.selectedTab = 1;
        GameRules gameRules = parent.summary.getSettings().gameRules();
        renderableVList.addRenderable(new TickBox(0,0,parent.resetNether, b-> Component.translatable("legacy.menu.load_save.reset_nether"),b-> null,t-> parent.resetNether = t.selected));
        renderableVList.addRenderable(new TickBox(0,0,parent.resetEnd, b-> Component.translatable("legacy.menu.load_save.reset_end"),b-> null,t-> parent.resetEnd = t.selected));
        renderableVList.addRenderable(new TickBox(0,0,parent.trustPlayers, b-> Component.translatable("legacy.menu.selectWorld.trust_players"),b-> null,t-> parent.trustPlayers = t.selected));
        addGameRulesOptions(renderableVList,gameRules, worldOptionsRules::contains);
        gameRenderables.addRenderable(new TickBox(0,0,parent.allowCheats,b->Component.translatable("selectWorld.allowCommands"),b->Tooltip.create(Component.translatable("selectWorld.allowCommands.info")),b->parent.allowCheats = b.selected));
        addGameRulesOptions(gameRenderables,gameRules, k-> !worldOptionsRules.contains(k));

        parent.applyGameRules = (g,s)->{
          if (!g.equals(gameRules)) g.assignFrom(gameRules,s);
        };
    }
    public void renderBackground(GuiGraphics guiGraphics, int i, int j, float f) {
        ScreenUtil.renderDefaultBackground(guiGraphics,false);
    }
    @Override
    public boolean keyPressed(int i, int j, int k) {
        tabList.controlTab(i,j,k);
        return super.keyPressed(i, j, k);
    }

    @Override
    public RenderableVList getRenderableVList() {
        if (tabList.selectedTab == 1) return gameRenderables;
        return super.getRenderableVList();
    }

    @Override
    protected void init() {
        panel.height = 199;
        addRenderableWidget(tabList);
        super.init();
        tabList.init(panel.x,panel.y - 23,panel.width);
        if (tabList.selectedTab == 0 && parent instanceof LoadSaveScreen) panel.height = 122;

    }
    void openExperimentsScreen(CreateWorldScreen screen, WorldDataConfiguration worldDataConfiguration) {
        Pair<Path, PackRepository> pair = screen.getDataPackSelectionSettings(worldDataConfiguration);
        if (pair != null)
            this.minecraft.setScreen(new ExperimentsScreen(this, pair.getSecond(), packRepository -> screen.tryApplyNewDataPacks((PackRepository)packRepository, false, (d)->openExperimentsScreen(screen,d))));
    }

    void openDataPackSelectionScreen(CreateWorldScreen screen, WorldDataConfiguration worldDataConfiguration) {
        Pair<Path, PackRepository> pair = screen.getDataPackSelectionSettings(worldDataConfiguration);
        if (pair != null) {
            this.minecraft.setScreen(new PackSelectionScreen(pair.getSecond(), packRepository -> screen.tryApplyNewDataPacks((PackRepository)packRepository, true, d-> openDataPackSelectionScreen(screen,d)), pair.getFirst(), Component.translatable("dataPack.title")));
        }
    }
}
