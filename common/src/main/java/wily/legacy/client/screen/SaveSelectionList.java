package wily.legacy.client.screen;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.ChatFormatting;
import net.minecraft.CrashReport;
import net.minecraft.SharedConstants;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.navigation.CommonInputs;
import net.minecraft.client.gui.screens.*;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.EditWorldScreen;
import net.minecraft.client.gui.screens.worldselection.WorldCreationContext;
import net.minecraft.client.gui.screens.worldselection.WorldSelectionList;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.LevelStorageException;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.LevelSummary;
import net.minecraft.world.level.validation.ContentValidationException;
import net.minecraft.world.level.validation.ForbiddenSymlinkInfo;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

public class SaveSelectionList extends SlotButtonList<SaveSelectionList.Entry> {
    static final ResourceLocation ERROR_HIGHLIGHTED_SPRITE = new ResourceLocation("world_list/error_highlighted");
    static final ResourceLocation ERROR_SPRITE = new ResourceLocation("world_list/error");
    static final ResourceLocation MARKED_JOIN_HIGHLIGHTED_SPRITE = new ResourceLocation("world_list/marked_join_highlighted");
    static final ResourceLocation MARKED_JOIN_SPRITE = new ResourceLocation("world_list/marked_join");
    static final ResourceLocation WARNING_HIGHLIGHTED_SPRITE = new ResourceLocation("world_list/warning_highlighted");
    static final ResourceLocation WARNING_SPRITE = new ResourceLocation("world_list/warning");
    static final ResourceLocation JOIN_HIGHLIGHTED_SPRITE = new ResourceLocation("world_list/join_highlighted");
    static final ResourceLocation JOIN_SPRITE = new ResourceLocation("world_list/join");
    static final Logger LOGGER = LogUtils.getLogger();
    static final DateFormat DATE_FORMAT = new SimpleDateFormat();
    static final Component FROM_NEWER_TOOLTIP_1 = Component.translatable("selectWorld.tooltip.fromNewerVersion1").withStyle(ChatFormatting.RED);
    static final Component FROM_NEWER_TOOLTIP_2 = Component.translatable("selectWorld.tooltip.fromNewerVersion2").withStyle(ChatFormatting.RED);
    static final Component SNAPSHOT_TOOLTIP_1 = Component.translatable("selectWorld.tooltip.snapshot1").withStyle(ChatFormatting.GOLD);
    static final Component SNAPSHOT_TOOLTIP_2 = Component.translatable("selectWorld.tooltip.snapshot2").withStyle(ChatFormatting.GOLD);
    static final Component WORLD_LOCKED_TOOLTIP = Component.translatable("selectWorld.locked").withStyle(ChatFormatting.RED);
    static final Component WORLD_REQUIRES_CONVERSION = Component.translatable("selectWorld.conversion.tooltip").withStyle(ChatFormatting.RED);
    static final Component WORLD_EXPERIMENTAL = Component.translatable("selectWorld.experimental");
    static final Component INCOMPATIBLE_VERSION_TOOLTIP = Component.translatable("selectWorld.incompatible.tooltip").withStyle(ChatFormatting.RED);
    private final PlayGameScreen screen;
    private CompletableFuture<List<LevelSummary>> pendingLevels;
    @Nullable
    private List<LevelSummary> currentlyDisplayedLevels;
    private String filter;
    private final LoadingHeader loadingHeader;

    public SaveSelectionList(PlayGameScreen playGameScreen, Minecraft minecraft, int i, int j, int k, int l, String string, @Nullable SaveSelectionList saveSelectionList) {
        super(()->playGameScreen.tabList.selectedTab == 0,minecraft, i, j, k, l);
        this.screen = playGameScreen;
        this.loadingHeader = new LoadingHeader(minecraft);
        this.filter = string;
        this.pendingLevels = saveSelectionList != null ? saveSelectionList.pendingLevels : this.loadLevels();
        this.handleNewLevels(this.pollLevelsIgnoreErrors());
        setRenderBackground(false);
    }


    @Override
    protected void clearEntries() {
        this.children().forEach(Entry::close);
        super.clearEntries();
    }

    @Nullable
    private List<LevelSummary> pollLevelsIgnoreErrors() {
        try {
            return this.pendingLevels.getNow(null);
        } catch (CancellationException | CompletionException runtimeException) {
            return null;
        }
    }

    void reloadWorldList() {
        this.pendingLevels = this.loadLevels();
    }

    @Override
    public boolean keyPressed(int i, int j, int k) {
        Optional<WorldListEntry> optional;
        if (CommonInputs.selected(i) && (optional = this.getSelectedOpt()).isPresent()) {
            optional.get().joinWorld();
            return true;
        }
        if (i == InputConstants.KEY_E && (optional = this.getSelectedOpt()).isPresent()) {
            minecraft.setScreen(new SaveOptionsScreen(screen,optional.get()));
            return true;
        }

        return super.keyPressed(i, j, k);
    }

    @Override
    public void renderWidget(GuiGraphics guiGraphics, int i, int j, float f) {
        List<LevelSummary> list = this.pollLevelsIgnoreErrors();
        if (list != this.currentlyDisplayedLevels) {
            this.handleNewLevels(list);
        }
        super.renderWidget(guiGraphics, i, j, f);
    }

    @Override
    protected void renderSelection(GuiGraphics guiGraphics, int i, int j, int k, int l, int m) {

    }

    private void handleNewLevels(@Nullable List<LevelSummary> list) {
        if (list == null) {
            this.fillLoadingLevels();
        } else {
            this.fillLevels(this.filter, list);
        }
        this.currentlyDisplayedLevels = list;
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
            LOGGER.error("Couldn't load level list", levelStorageException);
            this.handleLevelLoadFailure(levelStorageException.getMessageComponent());
            return CompletableFuture.completedFuture(List.of());
        }
        if (levelCandidates.isEmpty()) {
            screen.tabList.tabButtons.get(1).onPress();
            return CompletableFuture.completedFuture(List.of());
        }
        return this.minecraft.getLevelSource().loadLevelSummaries(levelCandidates).exceptionally(throwable -> {
            this.minecraft.delayCrash(CrashReport.forThrowable(throwable, "Couldn't load level list"));
            return List.of();
        });
    }

    private void fillLevels(String string, List<LevelSummary> list) {
        this.clearEntries();
        string = string.toLowerCase(Locale.ROOT);
        for (LevelSummary levelSummary : list) {
            if (!this.filterAccepts(string, levelSummary)) continue;
            this.addEntry(new SaveSelectionList.WorldListEntry(this, levelSummary));
        }
        this.notifyListUpdated();
    }

    private boolean filterAccepts(String string, LevelSummary levelSummary) {
        return levelSummary.getLevelName().toLowerCase(Locale.ROOT).contains(string) || levelSummary.getLevelId().toLowerCase(Locale.ROOT).contains(string);
    }

    private void fillLoadingLevels() {
        this.clearEntries();
        this.addEntry(this.loadingHeader);
        this.notifyListUpdated();
    }

    private void notifyListUpdated() {
        this.screen.triggerImmediateNarration(true);
    }

    private void handleLevelLoadFailure(Component component) {
        this.minecraft.setScreen(new ErrorScreen(Component.translatable("selectWorld.unable_to_load"), component));
    }

    @Override
    protected int getScrollbarPosition() {
        return super.getScrollbarPosition() + 20;
    }

    @Override
    public int getRowWidth() {
        return 270;
    }

    public Optional<WorldListEntry> getSelectedOpt() {
       Entry entry = this.getSelected();
        if (entry instanceof WorldListEntry e) {
            return Optional.of(e);
        }
        return Optional.empty();
    }

    public PlayGameScreen getScreen() {
        return this.screen;
    }

    @Override
    public void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
        if (this.children().contains(this.loadingHeader)) {
            this.loadingHeader.updateNarration(narrationElementOutput);
        } else {
            super.updateWidgetNarration(narrationElementOutput);
        }
    }

    @Environment(value= EnvType.CLIENT)
    public static class LoadingHeader extends Entry {
        private static final Component LOADING_LABEL = Component.translatable("selectWorld.loading_list");
        private final Minecraft minecraft;

        public LoadingHeader(Minecraft minecraft) {
            this.minecraft = minecraft;
        }

        @Override
        public void render(GuiGraphics guiGraphics, int i, int j, int k, int l, int m, int n, int o, boolean bl, float f) {
            int p = (this.minecraft.screen.width - this.minecraft.font.width(LOADING_LABEL)) / 2;
            int q = j + (m - this.minecraft.font.lineHeight) / 2;
            guiGraphics.drawString(this.minecraft.font, LOADING_LABEL, p, q, 0xFFFFFF, false);
            String string = LoadingDotsText.get(Util.getMillis());
            int r = (this.minecraft.screen.width - this.minecraft.font.width(string)) / 2;
            int s = q + this.minecraft.font.lineHeight;
            guiGraphics.drawString(this.minecraft.font, string, r, s, -8355712, false);
        }

        @Override
        public Component getNarration() {
            return LOADING_LABEL;
        }

        @Override
        public boolean isSelectable() {
            return false;
        }
    }

    @Environment(value=EnvType.CLIENT)
    public final class WorldListEntry extends SaveSelectionList.Entry {
        private final Minecraft minecraft;
        private final PlayGameScreen screen;
        private final LevelSummary summary;
        private final FaviconTexture icon;
        @Nullable
        private Path iconFile;
        private long lastClickTime;

        public WorldListEntry(SaveSelectionList worldSelectionList2, LevelSummary levelSummary) {
            this.minecraft = worldSelectionList2.minecraft;
            this.screen = worldSelectionList2.getScreen();
            this.summary = levelSummary;
            this.icon = FaviconTexture.forWorld(this.minecraft.getTextureManager(), levelSummary.getLevelId());
            this.iconFile = levelSummary.getIcon();
            this.validateIconFile();
            this.loadIcon();
        }

        private void validateIconFile() {
            if (this.iconFile == null) {
                return;
            }
            try {
                BasicFileAttributes basicFileAttributes = Files.readAttributes(this.iconFile, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
                if (basicFileAttributes.isSymbolicLink()) {
                    List<ForbiddenSymlinkInfo> list = this.minecraft.directoryValidator().validateSymlink(this.iconFile);
                    if (!list.isEmpty()) {
                        LOGGER.warn("{}", (Object) ContentValidationException.getMessage(this.iconFile, list));
                        this.iconFile = null;
                    } else {
                        basicFileAttributes = Files.readAttributes(this.iconFile, BasicFileAttributes.class, new LinkOption[0]);
                    }
                }
                if (!basicFileAttributes.isRegularFile()) {
                    this.iconFile = null;
                }
            } catch (NoSuchFileException noSuchFileException) {
                this.iconFile = null;
            } catch (IOException iOException) {
                LOGGER.error("could not validate symlink", iOException);
                this.iconFile = null;
            }
        }

        @Override
        public Component getNarration() {
            MutableComponent component = Component.translatable("narrator.select.world_info", this.summary.getLevelName(), new Date(this.summary.getLastPlayed()), this.summary.getInfo());
            if (this.summary.isLocked()) {
                component = CommonComponents.joinForNarration(component, WORLD_LOCKED_TOOLTIP);
            }
            if (this.summary.isExperimental()) {
                component = CommonComponents.joinForNarration(component, WORLD_EXPERIMENTAL);
            }
            return Component.translatable("narrator.select", component);
        }

        @Override
        public void render(GuiGraphics guiGraphics, int i, int j, int k, int l, int m, int n, int o, boolean bl, float f) {
            String string = this.summary.getLevelName();
            String string2 = this.summary.getLevelId();
            long p = this.summary.getLastPlayed();
            if (p != -1L) {
                string2 = string2 + " (" + DATE_FORMAT.format(new Date(p)) + ")";
            }
            if (StringUtils.isEmpty(string)) {
                string = I18n.get("selectWorld.world") + " " + (i + 1);
            }
            Component component = this.summary.getInfo();
            guiGraphics.drawString(this.minecraft.font, string2, k + 265 - minecraft.font.width(string2), j + (itemHeight - 7) / 2 - minecraft.font.lineHeight, -8355712, false);
            guiGraphics.drawString(this.minecraft.font, component, k + 265 - minecraft.font.width(component), j + (itemHeight - 7) / 2 + minecraft.font.lineHeight, -8355712, false);
            guiGraphics.drawString(this.minecraft.font, string, k + 35,j + (itemHeight - 7) / 2, 0xFFFFFF, true);
            RenderSystem.enableBlend();
            guiGraphics.blit(this.icon.textureLocation(), k + 5, j + 5, 0.0f, 0.0f, 20, 20, 20, 20);
            RenderSystem.disableBlend();
            if (this.minecraft.options.touchscreen().get().booleanValue() || bl) {
                guiGraphics.fill(k + 5, j + 5, k + 25, j + 25, -1601138544);
                int q = n - k;
                boolean bl2 = q < 32;
                ResourceLocation resourceLocation = bl2 ? JOIN_HIGHLIGHTED_SPRITE : JOIN_SPRITE;
                ResourceLocation resourceLocation2 = bl2 ? WARNING_HIGHLIGHTED_SPRITE : WARNING_SPRITE;
                ResourceLocation resourceLocation3 = bl2 ? ERROR_HIGHLIGHTED_SPRITE : ERROR_SPRITE;
                ResourceLocation resourceLocation4= bl2 ? MARKED_JOIN_HIGHLIGHTED_SPRITE : MARKED_JOIN_SPRITE;
                if (this.summary instanceof LevelSummary.SymlinkLevelSummary) {
                    guiGraphics.blitSprite(resourceLocation3, k, j, 32, 32);
                    guiGraphics.blitSprite(resourceLocation4, k, j, 32, 32);
                    return;
                }
                if (this.summary.isLocked()) {
                    guiGraphics.blitSprite(resourceLocation3, k, j, 32, 32);
                    if (bl2) {
                        this.screen.setTooltipForNextRenderPass(this.minecraft.font.split(WORLD_LOCKED_TOOLTIP, 175));
                    }
                } else if (this.summary.requiresManualConversion()) {
                    guiGraphics.blitSprite(resourceLocation3, k, j, 32, 32);
                    if (bl2) {
                        this.screen.setTooltipForNextRenderPass(this.minecraft.font.split(WORLD_REQUIRES_CONVERSION, 175));
                    }
                } else if (!this.summary.isCompatible()) {
                    guiGraphics.blitSprite(resourceLocation3, k, j, 32, 32);
                    if (bl2) {
                        this.screen.setTooltipForNextRenderPass(this.minecraft.font.split(INCOMPATIBLE_VERSION_TOOLTIP, 175));
                    }
                } else if (this.summary.shouldBackup()) {
                    guiGraphics.blitSprite(resourceLocation4, k, j, 32, 32);
                    if (this.summary.isDowngrade()) {
                        guiGraphics.blitSprite(resourceLocation3, k, j, 32, 32);
                        if (bl2) {
                            this.screen.setTooltipForNextRenderPass(ImmutableList.of(FROM_NEWER_TOOLTIP_1.getVisualOrderText(), FROM_NEWER_TOOLTIP_2.getVisualOrderText()));
                        }
                    } else if (!SharedConstants.getCurrentVersion().isStable()) {
                        guiGraphics.blitSprite(resourceLocation2, k, j, 32, 32);
                        if (bl2) {
                            this.screen.setTooltipForNextRenderPass(ImmutableList.of(SNAPSHOT_TOOLTIP_1.getVisualOrderText(), SNAPSHOT_TOOLTIP_2.getVisualOrderText()));
                        }
                    }
                } else {
                    guiGraphics.blitSprite(resourceLocation, k, j, 32, 32);
                }
            }
        }

        @Override
        public boolean mouseClicked(double d, double e, int i) {
            if (screen.tabList.selectedTab != 0) return false;
            if (this.summary.isDisabled()) {
                return true;
            }
            SaveSelectionList.this.setSelected(this);
            if (d - (double) SaveSelectionList.this.getRowLeft() <= 32.0) {
                this.joinWorld();
                return true;
            }
            if (Util.getMillis() - this.lastClickTime < 250L) {
                this.joinWorld();
                return true;
            }
            this.lastClickTime = Util.getMillis();
            return true;
        }

        public void joinWorld() {
            if (this.summary.primaryActionActive()) {
                if (this.summary instanceof LevelSummary.SymlinkLevelSummary) {
                    this.minecraft.setScreen(NoticeWithLinkScreen.createWorldSymlinkWarningScreen(() -> {
                        this.minecraft.setScreen(this.screen);
                    }));
                } else {
                    this.minecraft.createWorldOpenFlows().checkForBackupAndLoad(this.summary.getLevelId(), () -> {
                        SaveSelectionList.this.reloadWorldList();
                        this.minecraft.setScreen(this.screen);
                    });
                }
            }
        }


        public void doDeleteWorld() {

            LevelStorageSource levelStorageSource = this.minecraft.getLevelSource();
            String string = this.summary.getLevelId();
            try (LevelStorageSource.LevelStorageAccess levelStorageAccess = levelStorageSource.createAccess(string);){
                levelStorageAccess.deleteLevel();
            } catch (IOException iOException) {
                SystemToast.onWorldDeleteFailure(this.minecraft, string);
                LOGGER.error("Failed to delete world {}", string, iOException);
            }
            screen.saveSelectionList.reloadWorldList();
            minecraft.setScreen(screen);
        }

        public LevelSummary getSummary() {
            return summary;
        }


        private void queueLoadScreen() {
            this.minecraft.forceSetScreen(new GenericDirtMessageScreen(Component.translatable("selectWorld.data_read")));
        }

        private void loadIcon() {
            boolean bl;
            boolean bl2 = bl = this.iconFile != null && Files.isRegularFile(this.iconFile, new LinkOption[0]);
            if (bl) {
                try (InputStream inputStream = Files.newInputStream(this.iconFile, new OpenOption[0]);){
                    this.icon.upload(NativeImage.read(inputStream));
                } catch (Throwable throwable) {
                    LOGGER.error("Invalid icon for world {}", (Object)this.summary.getLevelId(), (Object)throwable);
                    this.iconFile = null;
                }
            } else {
                this.icon.clear();
            }
        }

        @Override
        public void close() {
            this.icon.close();
        }

        public String getLevelName() {
            return this.summary.getLevelName();
        }

        @Override
        public boolean isSelectable() {
            return !this.summary.isDisabled();
        }
    }

    @Environment(value=EnvType.CLIENT)
    public static abstract class Entry extends SlotButtonList.SlotEntry<SaveSelectionList.Entry> implements AutoCloseable {
        public abstract boolean isSelectable();

        @Override
        public void close() {
        }
    }
}
