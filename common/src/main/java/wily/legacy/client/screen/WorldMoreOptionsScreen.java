package wily.legacy.client.screen;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.worldselection.PresetEditor;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.level.GameRules;
import wily.legacy.util.ScreenUtil;
import wily.legacy.util.Stocker;

public class WorldMoreOptionsScreen extends PanelVListScreen {
    protected final TabList tabList = new TabList().addTabButton(29,0,Component.translatable("createWorld.tab.world.title"),t-> rebuildWidgets()).addTabButton(29,2,Component.translatable("legacy.menu.game_options"), t-> rebuildWidgets());

    protected final RenderableVList gameRenderables = new RenderableVList();
    protected final Stocker.Sizeable scrolledGameList = new Stocker.Sizeable(0);
    public WorldMoreOptionsScreen(LegacyCreateWorldScreen parent) {
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
            LegacyPresetEditor presetEditor = parent.getPresetEditor();
            if (presetEditor != null)
                minecraft.setScreen(presetEditor.createEditScreen(this, parent.getUiState()));
        }).build();
        parent.getUiState().addListener( s->customizeButton.active = !s.isDebug() && s.getPresetEditor() != null);
        renderableVList.addRenderable(customizeButton);
        TickBox cheatsButton = new TickBox(0,0,parent.getUiState().isAllowCheats(),b->Component.translatable("selectWorld.allowCommands"),b->Tooltip.create(Component.translatable("selectWorld.allowCommands.info")),b->parent.getUiState().setAllowCheats(b.selected));
        parent.getUiState().addListener(s-> cheatsButton.active = !s.isDebug() && !s.isHardcore());
        gameRenderables.addRenderable(Button.builder(ExperimentsScreen.EXPERIMENTS_LABEL, button -> parent.openExperimentsScreen(this,parent.getUiState().getSettings().dataConfiguration())).build());
        gameRenderables.addRenderable(Button.builder(Component.translatable("selectWorld.dataPacks"), button -> parent.openDataPackSelectionScreen(parent.getUiState().getSettings().dataConfiguration())).build());
        gameRenderables.addRenderable(cheatsButton);
        GameRules gameRules = parent.getUiState().getGameRules();
        GameRules.visitGameRuleTypes(new GameRules.GameRuleTypeVisitor() {

            @Override
            public void visitBoolean(GameRules.Key<GameRules.BooleanValue> key, GameRules.Type<GameRules.BooleanValue> type) {
                GameRules.BooleanValue value = gameRules.getRule(key);
                GameRules.BooleanValue defaultValue = type.createRule();
                gameRenderables.addRenderable(new TickBox(0,0,gameRules.getRule(key).get(),b-> Component.translatable(key.getDescriptionId()),b-> Tooltip.create(Component.translatable("editGamerule.default", defaultValue.serialize()).withStyle(ChatFormatting.GRAY)), b->value.set(b.selected,null)));
            }

            @Override
            public void visitInteger(GameRules.Key<GameRules.IntegerValue> key, GameRules.Type<GameRules.IntegerValue> type) {
                GameRules.IntegerValue value = gameRules.getRule(key);
                GameRules.IntegerValue defaultValue = type.createRule();
                EditBox integerEdit = new EditBox(Minecraft.getInstance().font,220,20,Component.translatable(key.getDescriptionId()));
                integerEdit.setTooltip(Tooltip.create(Component.translatable("editGamerule.default", defaultValue.serialize()).withStyle(ChatFormatting.GRAY)));
                integerEdit.setValue(Integer.toString(value.get()));
                integerEdit.setFilter(value::tryDeserialize);
                integerEdit.setResponder(string -> value.set(Integer.parseInt(string),null));
                gameRenderables.addRenderable(SimpleLayoutRenderable.create(0,9,r-> ((guiGraphics, i, j, f) -> guiGraphics.drawString(Minecraft.getInstance().font, Component.translatable(key.getDescriptionId()),r.x + 1,r.y + 2,0x404040,false))));
                gameRenderables.addRenderable(integerEdit);
            }
        });
        parent.getUiState().onChanged();
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
        addRenderableWidget(tabList);
        super.init();
        tabList.init(panel.x,panel.y - 23,panel.width);
    }
}
