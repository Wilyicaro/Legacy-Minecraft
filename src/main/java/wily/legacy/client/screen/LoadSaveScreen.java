package wily.legacy.client.screen;

import net.minecraft.ChatFormatting;
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
import wily.factoryapi.FactoryAPIPlatform;
import wily.factoryapi.base.client.FactoryGuiGraphics;
import wily.factoryapi.base.client.UIAccessor;
import wily.legacy.Legacy4J;
import wily.legacy.Legacy4JClient;
import wily.legacy.client.CommonColor;
import wily.legacy.client.LegacyClientWorldSettings;
import wily.legacy.client.LegacySaveCache;
import wily.legacy.client.PackAlbum;
import wily.legacy.util.LegacyComponents;
import wily.legacy.util.LegacySprites;
import wily.legacy.util.client.LegacyFontUtil;
import wily.legacy.util.client.LegacyRenderUtil;

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
    public static final List<ResourceKey<Level>> RESETTABLE_DIMENSIONS = new ArrayList<>(List.of(Level.NETHER, Level.END));
    public static final List<GameType> GAME_TYPES = Arrays.stream(GameType.values()).toList();
    public final List<ResourceKey<Level>> dimensionsToReset = new ArrayList<>();
    public final LevelSummary summary;
    protected final boolean isLocked;
    protected final LevelStorageSource.LevelStorageAccess access;
    protected final LegacySliderButton<GameType> gameTypeSlider;
    protected final PackAlbum.Selector resourceAlbumSelector;
    protected final TickBox onlineTickBox;
    protected final PublishScreen publishScreen;
    public BiConsumer<GameRules, MinecraftServer> applyGameRules = (r, s) -> {
    };
    public boolean trustPlayers;
    public boolean hostPrivileges;
    public Difficulty difficulty;

    public LoadSaveScreen(Screen screen, LevelSummary summary, LevelStorageSource.LevelStorageAccess access, boolean isLocked) {
        super(s -> Panel.createPanel(s, p -> (s.width - (p.width + (LegacyRenderUtil.hasTooltipBoxes(UIAccessor.of(s)) ? 160 : 0))) / 2, p -> (s.height - p.height) / 2 + 21, 245, 233), Component.translatable("legacy.menu.load_save.load"));
        this.isLocked = isLocked;
        this.parent = screen;
        this.summary = summary;
        this.access = access;
        difficulty = summary.getSettings().difficulty();
        gameTypeSlider = new LegacySliderButton<>(0, 0, 220, 16, b -> b.getDefaultMessage(GAME_MODEL_LABEL, b.getObjectValue().getShortDisplayName()), b -> Tooltip.create(Component.translatable("selectWorld.gameMode." + b.getObjectValue().getName() + ".info")), summary.getSettings().gameType(), () -> GAME_TYPES, b -> {
        });
        gameTypeSlider.active = !summary.isHardcore();
        publishScreen = new PublishScreen(this, gameTypeSlider.getObjectValue());
        onlineTickBox = new TickBox(0, 0, 220, publishScreen.publish, b -> PublishScreen.PUBLISH, b -> null, button -> {
            if (button.selected) minecraft.setScreen(publishScreen);
            button.selected = publishScreen.publish = false;
        });
        hostPrivileges = hasCommands(summary);
        trustPlayers = LegacyClientWorldSettings.of(summary.getSettings()).trustPlayers();
        (resourceAlbumSelector = PackAlbum.Selector.resources(panel.x + 13, panel.y + 112, 220, 45, !LegacyRenderUtil.hasTooltipBoxes(accessor), LegacyClientWorldSettings.of(summary.getSettings()).getSelectedResourceAlbum())).active = !this.isLocked;
    }

    public LoadSaveScreen(Screen screen, LevelSummary summary, LevelStorageSource source) {
        this(screen, summary, getSummaryAccess(source, summary), false);
    }

    public static boolean hasCommands(LevelSummary levelSummary) {
        return levelSummary.hasCommands();
    }

    public static LevelStorageSource.LevelStorageAccess getSummaryAccess(LevelStorageSource source, LevelSummary summary) {
        try (LevelStorageSource.LevelStorageAccess access = source.createAccess(summary.getLevelId())) {
            return access;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void deleteLevelDimension(LevelStorageSource.LevelStorageAccess access, ResourceKey<Level> dimension) throws IOException {
        Path path = access.getDimensionPath(dimension);
        Legacy4J.LOGGER.info("Deleting dimension {}", dimension);
        int i = 1;

        while (i <= 5) {
            Legacy4J.LOGGER.info("Attempt {}...", i);
            try {
                Files.walkFileTree(path, new SimpleFileVisitor<>() {
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

    public static void loadWorld(Screen screen, Minecraft minecraft, LevelStorageSource source, String levelId) {
        try (LevelStorageSource.LevelStorageAccess access = source.createAccess(levelId)) {
            loadWorld(screen, minecraft, source, access.getSummary(/*? if >1.20.2 {*/access.getDataTag()/*?}*/));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void loadWorld(Screen screen, Minecraft minecraft, LevelStorageSource source, LevelSummary summary) {
        SaveRenderableList.resetIconCache();
        PackAlbum.Selector.applyResourceChanges(minecraft, PackAlbum.getSelectedIds(minecraft.getResourcePackRepository()), LegacyClientWorldSettings.of(summary.getSettings()).getSelectedResourceAlbum().packs(), () -> new WorldOpenFlows(minecraft, source)./*? if <1.20.3 {*//*loadLevel*//*?} else if <1.20.5 {*//*checkForBackupAndLoad*//*?} else {*/openWorld/*?}*/(/*? if <1.20.3 {*//*screen, *//*?}*/summary.getLevelId()/*? if >1.20.2 {*/, () -> minecraft.setScreen(screen)/*?}*/));
    }

    @Override
    public void addControlTooltips(Renderer renderer) {
        super.addControlTooltips(renderer);
        OptionsScreen.setupSelectorControlTooltips(renderer, this);
    }

    public int getLayoutWidth() {
        return accessor.getInteger("layout.width", 220);
    }

    public int getLayoutX() {
        return accessor.getInteger("layout.x", panel.x + 13);
    }

    @Override
    protected void init() {
        panel.init();
        int layoutX = getLayoutX();
        gameTypeSlider.setPosition(layoutX, panel.y + 65);
        gameTypeSlider.setWidth(getLayoutWidth());
        addRenderableWidget(accessor.putWidget("difficultySlider", new LegacySliderButton<>(layoutX, panel.y + 90, getLayoutWidth(), 16, b -> b.getDefaultMessage(Component.translatable("options.difficulty"), b.getObjectValue().getDisplayName()), b -> Tooltip.create(difficulty.getInfo()), difficulty, () -> Arrays.asList(Difficulty.values()), b -> difficulty = b.getObjectValue()))).active = !LegacyClientWorldSettings.of(summary.getSettings()).isDifficultyLocked() && !summary.isHardcore();
        addRenderableWidget(accessor.putWidget("moreOptionsButton", Button.builder(Component.translatable("createWorld.tab.more.title"), button -> minecraft.setScreen(new WorldMoreOptionsScreen(this))).bounds(layoutX, panel.y + 178, getLayoutWidth(), 20).build()));
        Button loadButton = addRenderableWidget(accessor.putWidget("loadButton", Button.builder(Component.translatable("legacy.menu.load_save.load"), button -> onLoad()).bounds(layoutX, panel.y + 203, getLayoutWidth(), 20).build()));
        addRenderableWidget(accessor.putWidget("gameTypeSlider", gameTypeSlider));
        onlineTickBox.selected = publishScreen.publish;
        onlineTickBox.setPosition(layoutX + 1, panel.y + 161);
        onlineTickBox.updateHeight();
        onlineTickBox.setWidth(getLayoutWidth());
        addRenderableWidget(accessor.putWidget("onlineTickBox", onlineTickBox));
        setInitialFocus(loadButton);
        resourceAlbumSelector.setX(layoutX);
        resourceAlbumSelector.setY(panel.y + 112);
        resourceAlbumSelector.setWidth(getLayoutWidth());
        addRenderableWidget(accessor.putWidget("resourceAlbumSelector", resourceAlbumSelector));
    }

    public void onLoad() {
        if (dimensionsToReset.isEmpty()) {
            completeLoad();
        } else {
            confirmDimensionToReset(0);
        }
    }

    public void confirmDimensionToReset(int index) {
        ResourceKey<Level> level = dimensionsToReset.get(index);

        Component dimensionName = LegacyComponents.getDimensionName(level);
        minecraft.setScreen(new ConfirmationScreen(this, Component.translatable("legacy.menu.load_save.reset", dimensionName), Component.translatable("legacy.menu.load_save.reset_message", dimensionName, dimensionName), b -> {
            if (index == dimensionsToReset.size() - 1) {
                completeLoad();
            } else confirmDimensionToReset(index + 1);
        }) {
            @Override
            protected void addButtons() {
                renderableVList.addRenderable(Button.builder(Component.translatable("legacy.menu.load_save.reset_cancel", dimensionName), b -> {
                    dimensionsToReset.remove(index);
                    if (dimensionsToReset.isEmpty()) {
                        completeLoad();
                    } else confirmDimensionToReset(index);
                }).build());
                renderableVList.addRenderable(okButton = Button.builder(getTitle(), b -> okAction.accept(this)).build());
            }
        });

    }

    public void completeLoad() {
        dimensionsToReset.forEach(l -> {
            if (l == Level.OVERWORLD) return;
            try {
                deleteLevelDimension(access, l);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        LegacyClientWorldSettings.of(summary.getSettings()).setSelectedResourceAlbum(resourceAlbumSelector.getSelectedAlbum());
        loadWorld(this, minecraft, LegacySaveCache.getLevelStorageSource(), summary);
        Legacy4JClient.serverPlayerJoinConsumer = s -> {
            MinecraftServer server = FactoryAPIPlatform.getEntityServer(s);
            if (dimensionsToReset.contains(Level.END))
                server.getLevel(Level.END).setDragonFight(new EndDragonFight(minecraft.getSingleplayerServer().getLevel(Level.END), minecraft.getSingleplayerServer().getWorldData().worldGenOptions().seed(), EndDragonFight.Data.DEFAULT));
            server.setDefaultGameType(gameTypeSlider.getObjectValue());
            server.setDifficulty(difficulty, false);
            applyGameRules.accept(server.getGameRules(), minecraft.getSingleplayerServer());
            publishScreen.publish((IntegratedServer) server);
            LegacyClientWorldSettings.of(server.getWorldData()).setAllowCommands(hostPrivileges);
            server.getPlayerList().sendPlayerPermissionLevel(s);
            LegacyClientWorldSettings.of(server.getWorldData()).setSelectedResourceAlbum(resourceAlbumSelector.getSelectedAlbum());
            if (s.gameMode.getGameModeForPlayer() != gameTypeSlider.getObjectValue())
                s.setGameMode(gameTypeSlider.getObjectValue());
        };
    }

    @Override
    public boolean mouseScrolled(double d, double e/*? if >1.20.1 {*/, double f/*?}*/, double g) {
        if (resourceAlbumSelector.scrollableRenderer.mouseScrolled(g)) return true;
        return super.mouseScrolled(d, e/*? if >1.20.1 {*/, f/*?}*/, g);
    }

    @Override
    public void renderDefaultBackground(GuiGraphics guiGraphics, int i, int j, float f) {
        super.renderDefaultBackground(guiGraphics, i, j, f);
        resourceAlbumSelector.renderTooltipBox(guiGraphics, panel);
        panel.render(guiGraphics, i, j, f);
        guiGraphics.pose().pushMatrix();
        guiGraphics.pose().translate(LegacyRenderUtil.hasHorizontalArtifacts() ? 0.46f : 0.5f, 0);
        int iconSize = accessor.getInteger("saveIcon.size", 29);
        int iconX = accessor.getInteger("saveIcon.x", panel.x + 14);
        int iconY = accessor.getInteger("saveIcon.y", panel.y + 10);
        FactoryGuiGraphics.of(guiGraphics).blitSprite(LegacySprites.SQUARE_ENTITY_PANEL, iconX - 2, iconY - 1, iconSize + 3, iconSize + 3);
        guiGraphics.pose().popMatrix();
        guiGraphics.pose().pushMatrix();
        guiGraphics.pose().translate(0, 0.6f);
        FactoryGuiGraphics.of(guiGraphics).blit(SaveRenderableList.iconCache.getUnchecked(summary).textureLocation(), iconX, iconY, 0, 0, iconSize, iconSize, iconSize, iconSize);
        LegacyFontUtil.applySDFont(b -> {
            guiGraphics.drawString(font, summary.getLevelName(), accessor.getInteger("nameText.x", panel.x + 48), accessor.getInteger("nameText.y", panel.y + 12), CommonColor.INVENTORY_GRAY_TEXT.get(), false);
            guiGraphics.drawString(font, Component.translatable("legacy.menu.load_save.created_in", (hasCommands(summary) ? GameType.CREATIVE : GameType.SURVIVAL).getShortDisplayName()), accessor.getInteger("creationText.x", panel.x + 48), accessor.getInteger("creationText.y", panel.y + 29), CommonColor.INVENTORY_GRAY_TEXT.get(), false);
            guiGraphics.pose().popMatrix();
            if (!isLocked)
                guiGraphics.drawString(font, Component.translatable("commands.seed.success", LegacyClientWorldSettings.of(summary.getSettings()).getDisplaySeed()), accessor.getInteger("seedText.x", panel.x + 13), accessor.getInteger("seedText.y", panel.y + 49), CommonColor.INVENTORY_GRAY_TEXT.get(), false);
        });
    }

    @Override
    public void render(GuiGraphics guiGraphics, int i, int j, float f) {
        super.render(guiGraphics, i, j, f);
        if (LegacyRenderUtil.isMouseOver(i, j, panel.x + 14.5, panel.y + 10, 29, 29))
            guiGraphics.setTooltipForNextFrame(font, Component.translatable("selectWorld.targetFolder", Component.literal(summary.getLevelId()).withStyle(ChatFormatting.ITALIC)), i, j);
    }
}
