package wily.legacy.client.screen;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.datafixers.util.Pair;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.commands.PublishCommand;
import net.minecraft.util.HttpUtil;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.LevelSummary;
import org.jetbrains.annotations.Nullable;
import wily.legacy.Legacy4J;
import wily.legacy.Legacy4JClient;
import wily.legacy.client.LegacyWorldSettings;
import wily.legacy.client.controller.ControllerBinding;
import wily.legacy.util.ScreenUtil;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;

import static wily.legacy.Legacy4JClient.publishUnloadedServer;
import static wily.legacy.client.screen.ControlTooltip.*;
import static wily.legacy.client.screen.ControlTooltip.CONTROL_ACTION_CACHE;

public class LoadSaveScreen extends PanelBackgroundScreen{
    public static final Component GAME_MODEL_LABEL = Component.translatable("selectWorld.gameMode");
    private final boolean deleteOnClose;
    public BiConsumer<GameRules, MinecraftServer> applyGameRules = (r,s)-> {};
    protected final LevelStorageSource.LevelStorageAccess access;
    public boolean trustPlayers;
    public boolean allowCheats;
    public boolean resetNether = false;
    public boolean resetEnd = false;
    public Difficulty difficulty;
    public GameType gameType;
    protected boolean changedGameType = false;
    public final LevelSummary summary;
    protected boolean onlineOnStart = false;
    private int port = HttpUtil.getAvailablePort();
    protected final PackSelector resourcePackSelector;
    protected Collection<String> originalSelectedPacks = Collections.emptyList();

    public LoadSaveScreen(Screen screen, LevelSummary summary, LevelStorageSource.LevelStorageAccess access, boolean deleteOnClose) {
        super(s-> new Panel(p-> (s.width - (p.width + (ScreenUtil.hasTooltipBoxes() ? 160 : 0))) / 2, p-> (s.height - p.height) / 2 + 20,245,233), Component.translatable("legacy.menu.load_save.load"));
        this.deleteOnClose = deleteOnClose;
        this.parent = screen;
        this.summary = summary;
        this.access = access;
        difficulty = summary.getSettings().difficulty();
        gameType = summary.getSettings().gameType();
        allowCheats = summary.hasCheats();
        trustPlayers = ((LegacyWorldSettings)(Object)summary.getSettings()).trustPlayers();
        List<String> packs = ((LegacyWorldSettings)(Object)summary.getSettings()).getSelectedResourcePacks();
        if (!packs.isEmpty()){
            originalSelectedPacks = Minecraft.getInstance().getResourcePackRepository().getSelectedIds();
            Minecraft.getInstance().getResourcePackRepository().setSelected(packs);
        }
        resourcePackSelector = PackSelector.resources(panel.x + 13, panel.y + 112, 220,45, !ScreenUtil.hasTooltipBoxes());
        controlTooltipRenderer.add(()-> getActiveType().isKeyboard() ? COMPOUND_COMPONENT_FUNCTION.apply(new Component[]{getKeyIcon(InputConstants.KEY_LSHIFT,true), PLUS,getKeyIcon(InputConstants.MOUSE_BUTTON_LEFT,true)}) : ControllerBinding.LEFT_BUTTON.bindingState.getIcon(true), ()-> getFocused() == resourcePackSelector ? CONTROL_ACTION_CACHE.getUnchecked("legacy.action.resource_packs_screen") : null);
        if (!originalSelectedPacks.isEmpty()) Minecraft.getInstance().getResourcePackRepository().setSelected(originalSelectedPacks);

    }
    public LoadSaveScreen(Screen screen, LevelSummary summary) {
        this(screen,summary, getSummaryAccess(summary),false);
    }
    public static LevelStorageSource.LevelStorageAccess getSummaryAccess(LevelSummary summary){
        try {
            return Minecraft.getInstance().getLevelSource().createAccess(summary.getLevelId());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void init() {
        panel.init();
        List<GameType> gameTypes = Arrays.stream(GameType.values()).toList();
        addRenderableWidget(new LegacySliderButton<>(panel.x + 13, panel.y + 65, 220,16, b -> b.getDefaultMessage(GAME_MODEL_LABEL,b.getObjectValue().getLongDisplayName()),()->Tooltip.create(Component.translatable("selectWorld.gameMode."+gameType.getName()+ ".info")),gameType,()->gameTypes, b->{
            changedGameType = true;
            gameType =b.objectValue;
        })).active = !summary.isHardcore();
        addRenderableWidget(new LegacySliderButton<>(panel.x + 13, panel.y + 90, 220,16, b -> b.getDefaultMessage(Component.translatable("options.difficulty"),b.getObjectValue().getDisplayName()),()->Tooltip.create(difficulty.getInfo()), difficulty,()-> Arrays.asList(Difficulty.values()), b-> difficulty = b.objectValue)).active = !((LegacyWorldSettings)(Object)summary.getSettings()).isDifficultyLocked() && !summary.isHardcore();
        EditBox portEdit = addRenderableWidget(new EditBox(minecraft.font, panel.x + 124, panel.y + 157,100,20,Component.translatable("lanServer.port")));
        portEdit.visible = onlineOnStart;

        portEdit.setHint(Component.literal("" + this.port).withStyle(ChatFormatting.DARK_GRAY));
        addRenderableWidget(Button.builder(Component.translatable( "createWorld.tab.more.title"), button -> minecraft.setScreen(new WorldMoreOptionsScreen(this))).bounds(panel.x + 13, panel.y + 178,220,20).build());
        Button loadButton = addRenderableWidget(Button.builder(Component.translatable("legacy.menu.load_save.load"), button -> {
            try {
                this.onLoad();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }).bounds(panel.x + 13, panel.y + 203,220,20).build());
        addRenderableWidget(new TickBox(panel.x+ 14, panel.y+161,100,onlineOnStart, b-> Component.translatable("menu.shareToLan"), b->null, button -> {
            if (!(portEdit.visible = onlineOnStart = button.selected)) {
                loadButton.active = true;
                portEdit.setValue("");
            }
        }));
        portEdit.setResponder(string -> {
            Pair<Integer,Component> p = Legacy4JClient.tryParsePort(string);
            if(p.getFirst() != null) port = p.getFirst();
            portEdit.setHint(Component.literal("" + this.port).withStyle(ChatFormatting.DARK_GRAY));
            if (p.getSecond() == null) {
                portEdit.setTextColor(0xE0E0E0);
                portEdit.setTooltip(null);
                loadButton.active = true;
            } else {
                portEdit.setTextColor(0xFF5555);
                portEdit.setTooltip(Tooltip.create(p.getSecond()));
                loadButton.active = false;
            }
        });
        setInitialFocus(loadButton);
        resourcePackSelector.setX(panel.x + 13);
        resourcePackSelector.setY(panel.y + 112);
        addRenderableWidget(resourcePackSelector);
    }

    private void onLoad() throws IOException {
        access.close();
        if (resetNether) deleteLevelDimension(access,Level.NETHER);
        if (resetEnd) deleteLevelDimension(access,Level.END);
        if (changedGameType && summary.getGameMode() != gameType) Legacy4JClient.enterWorldGameType = gameType;
        resourcePackSelector.applyChanges(true, ()->minecraft.reloadResourcePacks().thenRun(this::completeLoad), this::completeLoad);
        if (!originalSelectedPacks.isEmpty()) Minecraft.getInstance().getResourcePackRepository().setSelected(originalSelectedPacks);
    }
    private void completeLoad(){
        loadWorld(this,minecraft,summary);
        minecraft.execute(()-> {
            if (minecraft.hasSingleplayerServer() && minecraft.getSingleplayerServer().isReady()){
                minecraft.getSingleplayerServer().setDefaultGameType(gameType);
                minecraft.getSingleplayerServer().setDifficulty(difficulty, false);
                applyGameRules.accept(minecraft.getSingleplayerServer().getGameRules(), minecraft.getSingleplayerServer());
                if (onlineOnStart) {
                    MutableComponent component = publishUnloadedServer(minecraft, gameType, allowCheats && trustPlayers, this.port) ? PublishCommand.getSuccessMessage(this.port) : Component.translatable("commands.publish.failed");
                    this.minecraft.gui.getChat().addMessage(component);
                }
                ((LegacyWorldSettings)minecraft.getSingleplayerServer().getWorldData()).setAllowCommands(allowCheats);
                if (resourcePackSelector.hasChanged()) ((LegacyWorldSettings)minecraft.getSingleplayerServer().getWorldData()).setSelectedResourcePacks(resourcePackSelector.getSelectedIds());
            }});
    }

    @Override
    public void onClose() {
        if (deleteOnClose) {
            try {
                access.deleteLevel();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        access.safeClose();
        super.onClose();
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
    public boolean mouseScrolled(double d, double e, double f, double g) {
        if (resourcePackSelector.scrollableRenderer.mouseScrolled(g)) return true;
        return super.mouseScrolled(d, e, f, g);
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int i, int j, float f) {
        super.renderBackground(guiGraphics, i, j, f);
        resourcePackSelector.renderTooltipBox(guiGraphics,panel);
        panel.render(guiGraphics,i,j,f);
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0.5f,0,0);
        ScreenUtil.renderSquareEntityPanel(guiGraphics,panel.x + 12, panel.y + 9, 32,32,2f);
        guiGraphics.pose().popPose();
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0,0.5f,0);
        guiGraphics.blit(SaveRenderableList.iconCache.getUnchecked(summary).textureLocation(),panel.x + 14, panel.y + 10, 0,0,29,29,29,29);
        guiGraphics.drawString(font,summary.getLevelName(),panel.x + 48, panel.y + 12, 0x383838,false);
        guiGraphics.drawString(font,Component.translatable("legacy.menu.load_save.created_in", (summary.hasCheats() ? GameType.CREATIVE : GameType.SURVIVAL).getShortDisplayName()),panel.x + 48, panel.y + 29, 0x383838,false);
        guiGraphics.pose().popPose();
        guiGraphics.drawString(font,Component.translatable("commands.seed.success",((LegacyWorldSettings)(Object)summary.getSettings()).getDisplaySeed()),panel.x + 13, panel.y + 49, 0x383838,false);
    }

    public static void loadWorld(Screen screen, Minecraft minecraft, LevelSummary summary) {
        SaveRenderableList.resetIconCache();
        minecraft.createWorldOpenFlows().checkForBackupAndLoad(summary.getLevelId(), ()-> minecraft.setScreen(screen));
    }
}
