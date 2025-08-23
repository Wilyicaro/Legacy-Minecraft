package wily.legacy.client.screen;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.logging.LogUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.CrashReport;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.*;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.LevelStorageException;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.LevelSummary;
import net.minecraft.world.level.validation.ContentValidationException;
import net.minecraft.world.level.validation.ForbiddenSymlinkInfo;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import wily.legacy.Legacy4J;
import wily.legacy.Legacy4JClient;
import wily.legacy.client.LegacyOptions;
import wily.legacy.util.ScreenUtil;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

public class SaveRenderableList extends RenderableVList {
    static final ResourceLocation ERROR_HIGHLIGHTED = new ResourceLocation("world_list/error_highlighted");
    static final ResourceLocation ERROR = new ResourceLocation("world_list/error");
    static final ResourceLocation MARKED_JOIN_HIGHLIGHTED = new ResourceLocation("world_list/marked_join_highlighted");
    static final ResourceLocation MARKED_JOIN = new ResourceLocation("world_list/marked_join");
    static final ResourceLocation WARNING_HIGHLIGHTED = new ResourceLocation("world_list/warning_highlighted");
    static final ResourceLocation WARNING = new ResourceLocation("world_list/warning");
    static final ResourceLocation JOIN_HIGHLIGHTED = new ResourceLocation("world_list/join_highlighted");
    static final ResourceLocation JOIN = new ResourceLocation("world_list/join");
    static final Logger LOGGER = LogUtils.getLogger();
    static final Component FROM_NEWER_TOOLTIP_1 = Component.translatable("selectWorld.tooltip.fromNewerVersion1").withStyle(ChatFormatting.RED);
    static final Component FROM_NEWER_TOOLTIP_2 = Component.translatable("selectWorld.tooltip.fromNewerVersion2").withStyle(ChatFormatting.RED);
    static final Component SNAPSHOT_TOOLTIP_1 = Component.translatable("selectWorld.tooltip.snapshot1").withStyle(ChatFormatting.GOLD);
    static final Component SNAPSHOT_TOOLTIP_2 = Component.translatable("selectWorld.tooltip.snapshot2").withStyle(ChatFormatting.GOLD);
    static final Component WORLD_LOCKED_TOOLTIP = Component.translatable("selectWorld.locked").withStyle(ChatFormatting.RED);
    static final Component WORLD_REQUIRES_CONVERSION = Component.translatable("selectWorld.conversion.tooltip").withStyle(ChatFormatting.RED);
    static final Component WORLD_EXPERIMENTAL = Component.translatable("selectWorld.experimental");
    static final Component INCOMPATIBLE_VERSION_TOOLTIP = Component.translatable("selectWorld.incompatible.tooltip").withStyle(ChatFormatting.RED);
    protected PlayGameScreen screen;
    protected Minecraft minecraft;
    private CompletableFuture<List<LevelSummary>> pendingLevels;
    @Nullable
    public List<LevelSummary> currentlyDisplayedLevels;
    private String filter;
    public static LoadingCache<LevelSummary, Long> sizeCache = CacheBuilder.newBuilder().build(new CacheLoader<>() {
        @Override
        public Long load(LevelSummary key) {
            return FileUtils.sizeOfDirectory(Minecraft.getInstance().getLevelSource().getLevelPath(key.getLevelId()).toFile());
        }
    });
    public static LoadingCache<LevelSummary, FaviconTexture> iconCache = CacheBuilder.newBuilder().build(new CacheLoader<>() {
        @Override
        public FaviconTexture load(LevelSummary key) {
            Path iconFile = key.getIcon();
            try {
                BasicFileAttributes basicFileAttributes = Files.readAttributes(iconFile, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
                if (basicFileAttributes.isSymbolicLink()) {
                    List<ForbiddenSymlinkInfo> list = Minecraft.getInstance().directoryValidator().validateSymlink(iconFile);
                    if (!list.isEmpty()) {
                        Legacy4J.LOGGER.warn("{}", ContentValidationException.getMessage(iconFile, list));
                        iconFile = null;
                    } else {
                        basicFileAttributes = Files.readAttributes(iconFile, BasicFileAttributes.class);
                    }
                }
                if (!basicFileAttributes.isRegularFile()) {
                    iconFile = null;
                }
            } catch (NoSuchFileException noSuchFileException) {
                iconFile = null;
            } catch (IOException iOException) {
                Legacy4J.LOGGER.error("could not validate symlink", iOException);
                iconFile = null;
            }
            FaviconTexture icon = FaviconTexture.forWorld(Minecraft.getInstance().getTextureManager(), key.getLevelId());
            boolean bl = iconFile != null && Files.isRegularFile(iconFile);
            if (bl) {
                try (InputStream inputStream = Files.newInputStream(iconFile)) {
                    icon.upload(NativeImage.read(inputStream));
                } catch (Throwable throwable) {
                    Legacy4J.LOGGER.error("Invalid icon for world {}", key.getLevelId(), throwable);
                }
            } else {
                icon.clear();
            }
            return icon;
        }
    });

    public SaveRenderableList(PlayGameScreen playGameScreen) {
        screen = playGameScreen;
        layoutSpacing(l->0);
        this.minecraft = Minecraft.getInstance();
        this.filter = "";
        reloadSaveList();
    }

    public static void resetIconCache(){
        SaveRenderableList.iconCache.asMap().forEach((s, i)-> i.close());
        SaveRenderableList.iconCache.invalidateAll();
    }


    @Nullable
    public List<LevelSummary> pollLevelsIgnoreErrors() {
        try {
            return this.pendingLevels.getNow(null);
        } catch (CancellationException | CompletionException runtimeException) {
            return null;
        }
    }

    void reloadSaveList() {
        this.pendingLevels = this.loadLevels();
    }

    public void updateFilter(String string) {
        if (this.currentlyDisplayedLevels != null && !string.equals(this.filter)) {
            this.fillLevels(string, this.currentlyDisplayedLevels);
        }
        this.filter = string;
    }

    private CompletableFuture<List<LevelSummary>> loadLevels() {
        LevelStorageSource.LevelCandidates levelCandidates;
        try {
            levelCandidates = this.minecraft.getLevelSource().findLevelCandidates();
        } catch (LevelStorageException levelStorageException) {
            Legacy4J.LOGGER.error("Couldn't load level list", levelStorageException);
            handleLevelLoadFailure(minecraft,levelStorageException.getMessageComponent());
            return CompletableFuture.completedFuture(List.of());
        }
        if (levelCandidates.isEmpty()) {
            screen.tabList.selectedTab = 1;
            return CompletableFuture.completedFuture(List.of());
        }

        if (currentlyDisplayedLevels == null || currentlyDisplayedLevels.isEmpty()) screen.isLoading = true;
        CompletableFuture<List<LevelSummary>> completableFuture = this.minecraft.getLevelSource().loadLevelSummaries(levelCandidates);
        completableFuture.thenAcceptAsync(l-> l.forEach(s->sizeCache.refresh(s)));
        return completableFuture.exceptionally(throwable -> {
            this.minecraft.delayCrash(CrashReport.forThrowable(throwable, "Couldn't load level list"));
            return List.of();
        });
    }

    public void fillLevels(String filter, List<LevelSummary> list) {
        screen.isLoading = false;
        renderables.clear();
        if (list != null) {
            filter = filter.toLowerCase(Locale.ROOT);
            for (LevelSummary summary : list) {
                if (!this.filterAccepts(filter, summary)) continue;
                addRenderable(new AbstractButton(0, 0, 270, 30, Component.literal(summary.getLevelName())) {
                    @Override
                    public void onPress() {
                        joinWorld(summary);
                    }
                    @Override
                    public void onClick(double d, double e) {
                        if (summary.isDisabled()) return;
                        boolean hoverIcon = ScreenUtil.isMouseOver(d, e, getX() + 5, getY() + 5, 20, height);
                        if (hoverIcon || isFocused()) onPress();
                    }

                    @Override
                    public boolean keyPressed(int i, int j, int k) {
                        if (i == InputConstants.KEY_O) {
                            minecraft.setScreen(new SaveOptionsScreen(screen, summary));
                            screen.setFocused(this);
                            return true;
                        }
                        return super.keyPressed(i, j, k);
                    }

                    @Override
                    protected MutableComponent createNarrationMessage() {
                        MutableComponent component = Component.translatable("narrator.select.world_info", summary.getLevelName(), new Date(summary.getLastPlayed()).toString(), summary.getInfo());
                        if (summary.isLocked()) {
                            component = CommonComponents.joinForNarration(component, WORLD_LOCKED_TOOLTIP);
                        }
                        if (summary.isExperimental()) {
                            component = CommonComponents.joinForNarration(component, WORLD_EXPERIMENTAL);
                        }
                        return Component.translatable("narrator.select", component);
                    }

                    @Override
                    protected void renderWidget(GuiGraphics guiGraphics, int i, int j, float f) {
                        super.renderWidget(guiGraphics, i, j, f);
                        RenderSystem.enableBlend();
                        guiGraphics.blit(iconCache.getUnchecked(summary).textureLocation(), getX() + 5, getY() + 5, 0, 0, 20, 20, 20, 20);
                        RenderSystem.disableBlend();
                        if (minecraft.options.touchscreen().get().booleanValue() || isHovered) {
                            guiGraphics.fill(getX() + 5, getY() + 5, getX() + 25, getY() + 25, -1601138544);

                            boolean hoverIcon = ScreenUtil.isMouseOver(i, j, getX() + 5, getY() + 5, 20, height);
                            ResourceLocation resourceLocation = hoverIcon ? JOIN_HIGHLIGHTED : JOIN;
                            ResourceLocation resourceLocation2 = hoverIcon ? WARNING_HIGHLIGHTED : WARNING;
                            ResourceLocation resourceLocation3 = hoverIcon ? ERROR_HIGHLIGHTED : ERROR;
                            ResourceLocation resourceLocation4 = hoverIcon ? MARKED_JOIN_HIGHLIGHTED : MARKED_JOIN;
                            if (summary instanceof LevelSummary.SymlinkLevelSummary || summary instanceof LevelSummary.CorruptedLevelSummary) {
                                guiGraphics.blitSprite(resourceLocation3, getX() + 5, getY() + 5, 20, 20);
                                guiGraphics.blitSprite(resourceLocation4, getX() + 5, getY() + 5, 20, 20);
                                return;
                            }
                            if (summary.isLocked()) {
                                guiGraphics.blitSprite(resourceLocation3, getX() + 5, getY() + 5, 20, 20);
                                if (hoverIcon) {
                                    screen.setTooltipForNextRenderPass(minecraft.font.split(WORLD_LOCKED_TOOLTIP, 175));
                                }
                            } else if (summary.requiresManualConversion()) {
                                guiGraphics.blitSprite(resourceLocation3, getX() + 5, getY() + 5, 20, 20);
                                if (hoverIcon) {
                                    screen.setTooltipForNextRenderPass(minecraft.font.split(WORLD_REQUIRES_CONVERSION, 175));
                                }
                            } else if (!summary.isCompatible()) {
                                guiGraphics.blitSprite(resourceLocation3, getX() + 5, getY() + 5, 20, 20);
                                if (hoverIcon) {
                                    screen.setTooltipForNextRenderPass(minecraft.font.split(INCOMPATIBLE_VERSION_TOOLTIP, 175));
                                }
                            } else if (summary.shouldBackup()) {
                                guiGraphics.blitSprite(resourceLocation4, getX() + 5, getY() + 5, 20, 20);
                                if (summary.isDowngrade()) {
                                    guiGraphics.blitSprite(resourceLocation3, getX() + 5, getY() + 5, 20, 20);
                                    if (hoverIcon) {
                                        screen.setTooltipForNextRenderPass(ImmutableList.of(FROM_NEWER_TOOLTIP_1.getVisualOrderText(), FROM_NEWER_TOOLTIP_2.getVisualOrderText()));
                                    }
                                } else if (!SharedConstants.getCurrentVersion().isStable()) {
                                    guiGraphics.blitSprite(resourceLocation2, getX() + 5, getY() + 5, 20, 20);
                                    if (hoverIcon) {
                                        screen.setTooltipForNextRenderPass(ImmutableList.of(SNAPSHOT_TOOLTIP_1.getVisualOrderText(), SNAPSHOT_TOOLTIP_2.getVisualOrderText()));
                                    }
                                }
                            } else {
                                guiGraphics.blitSprite(resourceLocation, getX() + 5, getY() + 5, 20, 20);
                            }
                        }
                    }

                    @Override
                    protected void renderScrollingString(GuiGraphics guiGraphics, Font font, int i, int j) {
                        ScreenUtil.renderScrollingString(guiGraphics, font, this.getMessage(), this.getX() + 35, this.getY(), this.getX() + this.getWidth(), this.getY() + this.getHeight(), j, true);
                    }

                    @Override
                    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
                        defaultButtonNarrationText(narrationElementOutput);
                    }
                });
            }

        }
        this.currentlyDisplayedLevels = list;
        screen.triggerImmediateNarration(true);
    }

    private boolean filterAccepts(String string, LevelSummary levelSummary) {
        return levelSummary.getLevelName().toLowerCase(Locale.ROOT).contains(string) || levelSummary.getLevelId().toLowerCase(Locale.ROOT).contains(string);
    }

    public static void handleLevelLoadFailure(Minecraft minecraft, Component component) {
        minecraft.setScreen(new ConfirmationScreen(new TitleScreen(), Component.translatable("selectWorld.futureworld.error.title"), component, (b)->{}){
            protected void addButtons() {
                renderableVList.addRenderable(okButton = Button.builder(Component.translatable("gui.ok"),(b)-> onClose()).bounds(panel.x + 15, panel.y + panel.height - 30,200,20).build());
            }
        });
    }


    public PlayGameScreen getScreen() {
        return this.screen;
    }

    public LevelSummary getLevelSummary(int saveButtonIndex){
        if (currentlyDisplayedLevels != null && renderables.get(saveButtonIndex) instanceof AbstractButton && saveButtonIndex < currentlyDisplayedLevels.size())
            return currentlyDisplayedLevels.get(saveButtonIndex);
        return null;
    }

    public void joinWorld(LevelSummary summary) {
        if (summary.primaryActionActive()) {
            if (summary instanceof LevelSummary.SymlinkLevelSummary) {
                this.minecraft.setScreen(NoticeWithLinkScreen.createWorldSymlinkWarningScreen(() -> {
                    this.minecraft.setScreen(this.screen);
                }));
            } else {
                SaveRenderableList.this.reloadSaveList();
                if (((LegacyOptions)minecraft.options).directSaveLoad().get()){
                    Legacy4JClient.copySaveBtwSources(LoadSaveScreen.getSummaryAccess(Minecraft.getInstance().getLevelSource(),summary),Legacy4JClient.currentWorldSource);
                    LoadSaveScreen.loadWorld(screen,minecraft, Legacy4JClient.currentWorldSource,summary.getLevelId());
                }else minecraft.setScreen(new LoadSaveScreen(screen, summary, Legacy4JClient.currentWorldSource){
                    @Override
                    public void onLoad() throws IOException {
                        Legacy4JClient.copySaveBtwSources(LoadSaveScreen.getSummaryAccess(Minecraft.getInstance().getLevelSource(),summary),Legacy4JClient.currentWorldSource);
                        super.onLoad();
                    }
                });
            }
        }
    }


    public void deleteSave(LevelSummary summary) {
        LevelStorageSource levelStorageSource = this.minecraft.getLevelSource();
        String string = summary.getLevelId();
        try (LevelStorageSource.LevelStorageAccess levelStorageAccess = levelStorageSource.createAccess(string);) {
            levelStorageAccess.deleteLevel();
        } catch (IOException iOException) {
            SystemToast.onWorldDeleteFailure(this.minecraft, string);
            LOGGER.error("Failed to delete world {}", string, iOException);
        }
        reloadSaveList();
        minecraft.setScreen(screen);
    }

}
