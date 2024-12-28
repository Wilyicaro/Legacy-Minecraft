package wily.legacy.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.WorldOpenFlows;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.end.EndDragonFight;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.LevelSummary;
import org.jetbrains.annotations.Nullable;
import wily.factoryapi.base.client.FactoryGuiGraphics;
import wily.factoryapi.base.client.UIDefinition;
import wily.legacy.Legacy4J;
import wily.legacy.Legacy4JClient;
import wily.legacy.client.CommonColor;
import wily.legacy.client.LegacyClientWorldSettings;
import wily.legacy.util.LegacySprites;
import wily.legacy.util.ScreenUtil;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.function.BiConsumer;

import static wily.legacy.client.screen.ControlTooltip.*;

public class LoadSaveScreen extends PanelBackgroundScreen {
    public static final Component GAME_MODEL_LABEL = Component.translatable("selectWorld.gameMode");
    protected final boolean isLocked;
    public BiConsumer<GameRules, MinecraftServer> applyGameRules = (r,s)-> {};
    protected final LevelStorageSource.LevelStorageAccess access;
    protected final LegacySliderButton<GameType> gameTypeSlider;
    public boolean trustPlayers;
    public boolean allowCommands;
    public static final List<ResourceKey<Level>> RESETTABLE_DIMENSIONS = new ArrayList<>(List.of(Level.NETHER,Level.END));
    public final List<ResourceKey<Level>> dimensionsToReset = new ArrayList<>();
    public Difficulty difficulty;
    public final LevelSummary summary;
    protected final Assort.Selector resourceAssortSelector;
    protected final TickBox onlineTickBox;
    protected final PublishScreen publishScreen;
    public static final List<GameType> GAME_TYPES = Arrays.stream(GameType.values()).toList();

    public LoadSaveScreen(Screen screen, LevelSummary summary, LevelStorageSource.LevelStorageAccess access, boolean isLocked) {
        super(s-> Panel.createPanel(s, p-> (s.width - (p.width + (ScreenUtil.hasTooltipBoxes(UIDefinition.Accessor.of(s)) ? 160 : 0))) / 2, p-> (s.height - p.height) / 2 + 24, 245, 233), Component.translatable("legacy.menu.load_save.load"));
        this.isLocked = isLocked;
        this.parent = screen;
        this.summary = summary;
        this.access = access;
        difficulty = summary.getSettings().difficulty();
        gameTypeSlider = new LegacySliderButton<>(0,0, 220,16, b -> b.getDefaultMessage(GAME_MODEL_LABEL,b.getObjectValue().getLongDisplayName()),b->Tooltip.create(Component.translatable("selectWorld.gameMode."+b.getObjectValue().getName()+ ".info")),summary.getSettings().gameType(),()->GAME_TYPES, b-> {});
        gameTypeSlider.active = !summary.isHardcore();
        publishScreen = new PublishScreen(this, gameTypeSlider.getObjectValue());
        onlineTickBox = new TickBox(0,0,220,publishScreen.publish, b-> PublishScreen.PUBLISH, b->null, button -> {
            if (button.selected) minecraft.setScreen(publishScreen);
            button.selected = publishScreen.publish = false;
        });
        allowCommands = hasCommands(summary);
        trustPlayers = LegacyClientWorldSettings.of(summary.getSettings()).trustPlayers();
        (resourceAssortSelector = Assort.Selector.resources(panel.x + 13, panel.y + 112, 220,45, !ScreenUtil.hasTooltipBoxes(accessor),LegacyClientWorldSettings.of(summary.getSettings()).getSelectedResourceAssort())).active = !this.isLocked;
    }

    public LoadSaveScreen(Screen screen, LevelSummary summary, LevelStorageSource source) {
        this(screen,summary, getSummaryAccess(source,summary),false);
    }

    @Override
    public void addControlTooltips(Renderer renderer) {
        super.addControlTooltips(renderer);
        resourceAssortSelector.addControlTooltips(this,renderer);
    }

    public static boolean hasCommands(LevelSummary levelSummary){
        return levelSummary./*? if <1.20.5 {*//*hasCheats*//*?} else {*/hasCommands/*?}*/();
    }

    public static LevelStorageSource.LevelStorageAccess getSummaryAccess(LevelStorageSource source, LevelSummary summary){
        try (LevelStorageSource.LevelStorageAccess access = source.createAccess(summary.getLevelId())) {
            return access;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void init() {
        panel.init();
        gameTypeSlider.setPosition(panel.x + 13, panel.y + 65);
        addRenderableWidget(new LegacySliderButton<>(panel.x + 13, panel.y + 90, 220,16, b -> b.getDefaultMessage(Component.translatable("options.difficulty"),b.getObjectValue().getDisplayName()),b->Tooltip.create(difficulty.getInfo()), difficulty,()-> Arrays.asList(Difficulty.values()), b-> difficulty = b.getObjectValue())).active = !LegacyClientWorldSettings.of(summary.getSettings()).isDifficultyLocked() && !summary.isHardcore();
        addRenderableWidget(Button.builder(Component.translatable( "createWorld.tab.more.title"), button -> minecraft.setScreen(new WorldMoreOptionsScreen(this))).bounds(panel.x + 13, panel.y + 178,220,20).build());
        Button loadButton = addRenderableWidget(Button.builder(Component.translatable("legacy.menu.load_save.load"), button -> {
            try {
                this.onLoad();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }).bounds(panel.x + 13, panel.y + 203,220,20).build());
        addRenderableWidget(gameTypeSlider);
        onlineTickBox.selected = publishScreen.publish;
        onlineTickBox.setPosition(panel.x+ 14, panel.y+161);
        addRenderableWidget(onlineTickBox);
        setInitialFocus(loadButton);
        resourceAssortSelector.setX(panel.x + 13);
        resourceAssortSelector.setY(panel.y + 112);
        addRenderableWidget(resourceAssortSelector);
    }

    public void onLoad() throws IOException {
        dimensionsToReset.forEach(l-> {
            if (l == Level.OVERWORLD) return;
            try {
                deleteLevelDimension(access,l);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        completeLoad();
    }
    private void completeLoad(){
        LegacyClientWorldSettings.of(summary.getSettings()).setSelectedResourceAssort(resourceAssortSelector.getSelectedAssort());
        loadWorld(this,minecraft,Legacy4JClient.currentWorldSource,summary);
        Legacy4JClient.serverPlayerJoinConsumer = s-> {
            if (dimensionsToReset.contains(Level.END)) s.server.getLevel(Level.END).setDragonFight(new EndDragonFight(minecraft.getSingleplayerServer().getLevel(Level.END),minecraft.getSingleplayerServer().getWorldData().worldGenOptions().seed(), EndDragonFight.Data.DEFAULT));
            s.server.setDefaultGameType(gameTypeSlider.getObjectValue());
            s.server.setDifficulty(difficulty, false);
            applyGameRules.accept(s.server.getGameRules(), minecraft.getSingleplayerServer());
            publishScreen.publish((IntegratedServer) s.server);
            LegacyClientWorldSettings.of(s.server.getWorldData()).setAllowCommands(allowCommands);
            s.server.getPlayerList().sendPlayerPermissionLevel(s);
            LegacyClientWorldSettings.of(s.server.getWorldData()).setSelectedResourceAssort(resourceAssortSelector.getSelectedAssort());
            if (s.gameMode.getGameModeForPlayer() != gameTypeSlider.getObjectValue()) s.setGameMode(gameTypeSlider.getObjectValue());
        };
    }

    public static void deleteLevelDimension(LevelStorageSource.LevelStorageAccess access, ResourceKey<Level> dimension) throws IOException {
        Path path = access.getDimensionPath(dimension);
        Legacy4J.LOGGER.info("Deleting dimension {}", dimension);
        int i = 1;

        while(i <= 5) {
            Legacy4J.LOGGER.info("Attempt {}...", i);
            try {
                Files.walkFileTree(path,new SimpleFileVisitor<>(){
                    public FileVisitResult visitFile(Path pathx, BasicFileAttributes basicFileAttributes) throws IOException {
                        Legacy4J.LOGGER.debug("Deleting {}", pathx);
                        Files.delete(pathx);
                        return FileVisitResult.CONTINUE;
                    }
                    public FileVisitResult postVisitDirectory(Path pathx, @Nullable IOException iOException) throws IOException {
                        if (iOException != null) {
                            throw iOException;
                        } else {
                            Files.delete(pathx);
                            return FileVisitResult.CONTINUE;
                        }
                    }
                });
                break;
            } catch (IOException var6) {
                if (i >= 5) {
                    throw var6;
                }
                Legacy4J.LOGGER.warn("Failed to delete {}", path, var6);
                try {
                    Thread.sleep(500L);
                } catch (InterruptedException var5) {
                }
                ++i;
            }
        }

    }

    @Override
    public boolean mouseScrolled(double d, double e/*? if >1.20.1 {*/, double f/*?}*/, double g) {
        if (resourceAssortSelector.scrollableRenderer.mouseScrolled(g)) return true;
        return super.mouseScrolled(d, e/*? if >1.20.1 {*/, f/*?}*/, g);
    }

    @Override
    public void renderDefaultBackground(GuiGraphics guiGraphics, int i, int j, float f) {
        super.renderDefaultBackground(guiGraphics, i, j, f);
        resourceAssortSelector.renderTooltipBox(guiGraphics,panel);
        panel.render(guiGraphics,i,j,f);
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0.5f,0,0);
        FactoryGuiGraphics.of(guiGraphics).blitSprite(LegacySprites.SQUARE_ENTITY_PANEL,panel.x + 12, panel.y + 9, 32,32);
        guiGraphics.pose().popPose();
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0,0.5f,0);
        FactoryGuiGraphics.of(guiGraphics).blit(SaveRenderableList.iconCache.getUnchecked(summary).textureLocation(),panel.x + 14, panel.y + 10, 0,0,29,29,29,29);
        guiGraphics.drawString(font,summary.getLevelName(),panel.x + 48, panel.y + 12, CommonColor.INVENTORY_GRAY_TEXT.get(),false);
        guiGraphics.drawString(font,Component.translatable("legacy.menu.load_save.created_in", (hasCommands(summary) ? GameType.CREATIVE : GameType.SURVIVAL).getShortDisplayName()),panel.x + 48, panel.y + 29, CommonColor.INVENTORY_GRAY_TEXT.get(),false);
        guiGraphics.pose().popPose();
        if (!isLocked) guiGraphics.drawString(font,Component.translatable("commands.seed.success",LegacyClientWorldSettings.of(summary.getSettings()).getDisplaySeed()),panel.x + 13, panel.y + 49, CommonColor.INVENTORY_GRAY_TEXT.get(),false);
    }

    public static void loadWorld(Screen screen, Minecraft minecraft, LevelStorageSource source, String levelId) {
        try (LevelStorageSource.LevelStorageAccess access = source.createAccess(levelId)){
            loadWorld(screen,minecraft,source,access.getSummary(/*? if >1.20.2 {*/access.getDataTag()/*?}*/));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    public static void loadWorld(Screen screen, Minecraft minecraft, LevelStorageSource source, LevelSummary summary) {
        SaveRenderableList.resetIconCache();
        Assort.Selector.applyResourceChanges(minecraft,Assort.getSelectedIds(minecraft.getResourcePackRepository()),LegacyClientWorldSettings.of(summary.getSettings()).getSelectedResourceAssort().packs(),()->new WorldOpenFlows(minecraft,source)./*? if <1.20.3 {*//*loadLevel*//*?} else if <1.20.5 {*//*checkForBackupAndLoad*//*?} else {*/openWorld/*?}*/(/*? if <1.20.3 {*//*screen, *//*?}*/summary.getLevelId()/*? if >1.20.2 {*/, ()-> minecraft.setScreen(screen)/*?}*/));
    }
}
