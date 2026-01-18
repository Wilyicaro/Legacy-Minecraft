package wily.legacy.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.LevelSummary;
import org.apache.commons.io.FileUtils;
import wily.factoryapi.base.Stocker;
import wily.factoryapi.base.client.FactoryGuiGraphics;
import wily.factoryapi.base.client.UIAccessor;
import wily.legacy.Legacy4J;
import wily.legacy.client.*;
import wily.legacy.util.LegacyComponents;
import wily.legacy.util.client.LegacyFontUtil;
import wily.legacy.util.client.LegacyRenderUtil;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class CreationList extends RenderableVList {
    protected final Minecraft minecraft;

    public CreationList(UIAccessor accessor) {
        super(accessor);
        layoutSpacing(l -> 0);
        minecraft = Minecraft.getInstance();
        addIconButton(this, Legacy4J.createModLocation("creation_list/create_world"), Component.translatable("legacy.menu.create_world"), c -> CreateWorldScreen.openFresh(this.minecraft, () -> minecraft.setScreen(getScreen())));
        LegacyWorldTemplate.list.forEach(t -> addIconButton(this, t.icon(), t.buttonMessage(), c -> {
            if (t.isGamePath() && !Files.exists(t.getPath())) {
                Path path = t.getValidPath();
                if (path == null || t.preDownload()) {
                    minecraft.setScreen(ConfirmationScreen.createInfoScreen(getScreen(), LegacyComponents.MISSING_WORLD_TEMPLATE, Component.translatable("legacy.menu.missing_world_template_message", t.buttonMessage())));
                } else {
                    File file = path.toFile();
                    Stocker<Long> fileSize = new Stocker<>(1L);
                    ExecutorService executor = Executors.newSingleThreadExecutor();
                    LegacyLoadingScreen screen = new LegacyLoadingScreen(LegacyComponents.DOWNLOADING_WORLD_TEMPLATE, CommonComponents.EMPTY) {
                        @Override
                        public void tick() {
                            setProgress(file.exists() ? Math.min(1, FileUtils.sizeOf(file) / (float) fileSize.get()) : 0);
                            super.tick();
                        }

                        @Override
                        public void onClose() {
                            if (file.exists()) file.delete();
                            minecraft.setScreen(getScreen());
                            LegacyLoadingScreen.closeExecutor(executor);
                        }

                        @Override
                        public boolean shouldCloseOnEsc() {
                            return true;
                        }
                    };
                    minecraft.setScreen(screen);
                    CompletableFuture.runAsync(() -> {
                        try {
                            URL url = t.downloadURI().get().toURL();
                            fileSize.set(url.openConnection().getContentLengthLong());
                            FileUtils.copyURLToFile(url, path.toFile());
                            minecraft.execute(() -> loadTemplate(getScreen(), minecraft, t));
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }, executor);
                }
            } else {
                loadTemplate(getScreen(), minecraft, t);
            }
        }));
    }

    public static void loadTemplate(Screen parent, Minecraft minecraft, LegacyWorldTemplate template) {
        try (LevelStorageSource.LevelStorageAccess access = LegacySaveCache.getLevelStorageSource().createAccess(LegacySaveCache.importSaveFile(template.open(), minecraft.getLevelSource()::levelExists, LegacySaveCache.getLevelStorageSource(), template.folderName()))) {
            LevelSummary summary = access.getSummary(/*? if >1.20.2 {*/access.getDataTag()/*?}*/);
            Optional<PackAlbum> album = template.albumId().map(PackAlbum::resourceById);
            album.ifPresent(LegacyClientWorldSettings.of(summary.getSettings())::setSelectedResourceAlbum);
            access.close();
            if (template.directJoin()) {
                LoadSaveScreen.loadWorld(parent, minecraft, LegacySaveCache.getLevelStorageSource(), summary);
            } else minecraft.setScreen(new LoadSaveScreen(parent, summary, access, (album.isPresent() || template.albumId().isEmpty()) && template.isLocked()) {
                @Override
                public void onClose() {
                    if (!LegacyOptions.saveCache.get())
                        FileUtils.deleteQuietly(access.getDimensionPath(Level.OVERWORLD).toFile());
                    super.onClose();
                }
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void addIconButton(RenderableVList list, ResourceLocation iconSprite, Component message, Consumer<AbstractButton> onPress) {
        addIconButton(list, iconSprite, message, onPress, null);
    }

    public static void addIconButton(RenderableVList list, ResourceLocation iconSprite, Component message, Consumer<AbstractButton> onPress, Tooltip tooltip) {
        AbstractButton button;
        list.addRenderable(button = new ContentButton(list, 0, 0, 270, 30, message) {
            @Override
            public void renderIcon(GuiGraphics guiGraphics, int mouseX, int mouseY, int x, int y, int width, int height) {
                FactoryGuiGraphics.of(guiGraphics).blitSprite(iconSprite, getX() + x, getY() + y, width, height);
            }

            @Override
            public void onPress(InputWithModifiers input) {
                onPress.accept(this);
            }
        });
        button.setTooltip(tooltip);
    }


    public static abstract class ContentButton extends AbstractButton {

        protected final RenderableVList list;

        public ContentButton(RenderableVList list, int x, int y, int width, int height, Component component) {
            super(x, y, width, height, component);
            this.list = list;
        }

        @Override
        protected void renderWidget(GuiGraphics guiGraphics, int i, int j, float f) {
            super.renderWidget(guiGraphics, i, j, f);
            if (list.accessor.getBoolean(list.name + ".buttonIcon.isVisible", true))
                renderIcon(guiGraphics, i, j, f);
        }

        public void renderIcon(GuiGraphics guiGraphics, int mouseX, int mouseY, float f) {
            int iconWidth = list.accessor.getInteger(list.name + ".buttonIcon.width", 20);
            int iconHeight = list.accessor.getInteger(list.name + ".buttonIcon.height", 20);
            int iconPos = (height - iconHeight) / 2;
            renderIcon(guiGraphics, mouseX, mouseY, iconPos, iconPos, iconWidth, iconHeight);
            if (Minecraft.getInstance().options.touchscreen().get().booleanValue() || isHovered) {
                renderIconHighlight(guiGraphics, mouseX, mouseY, iconPos, iconPos, iconWidth, iconHeight);
            }
        }

        public void renderIcon(GuiGraphics guiGraphics, int mouseX, int mouseY, int x, int y, int width, int height) {
        }

        public void renderIconHighlight(GuiGraphics guiGraphics, int mouseX, int mouseY, int x, int y, int width, int height) {
            guiGraphics.fill(getX() + x, getY() + y, getX() + x + width, getY() + y + height, -1601138544);
        }

        @Override
        protected void renderScrollingString(GuiGraphics guiGraphics, Font font, int i, int j) {
            int x = this.getX() + list.accessor.getInteger(list.name + ".buttonMessage.xOffset", 35);
            LegacyFontUtil.applySDFont(b -> LegacyRenderUtil.renderScrollingString(guiGraphics, font, this.getMessage(), x, this.getY(), x + this.getWidth(), this.getY() + this.getHeight(), j, true));
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
            defaultButtonNarrationText(narrationElementOutput);
        }
    }
}
