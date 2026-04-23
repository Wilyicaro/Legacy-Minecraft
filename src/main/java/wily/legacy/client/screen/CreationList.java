package wily.legacy.client.screen;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelResource;
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
import java.io.InputStream;
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
    private static final LoadingCache<String, ResourceLocation> packIcons = CacheBuilder.newBuilder().build(new CacheLoader<>() {
        @Override
        public ResourceLocation load(String key) throws Exception {
            Path path = Minecraft.getInstance().getResourcePackDirectory().resolve(key).resolve("pack.png");
            if (!Files.isRegularFile(path)) return PackAlbum.Selector.DEFAULT_ICON;
            try (InputStream inputStream = Files.newInputStream(path)) {
                NativeImage image = NativeImage.read(inputStream);
                ResourceLocation location = ResourceLocation.fromNamespaceAndPath("legacy", "template_pack_icon/" + Integer.toHexString(key.hashCode()));
                Minecraft.getInstance().getTextureManager().register(location, new DynamicTexture(location::toString, image));
                return location;
            }
        }
    });

    public CreationList(UIAccessor accessor) {
        super(accessor);
        layoutSpacing(l -> 0);
        minecraft = Minecraft.getInstance();
        addIconButton(this, Legacy4J.createModLocation("creation_list/create_world"), Component.translatable("legacy.menu.create_world"), c -> CreateWorldScreen.openFresh(this.minecraft, () -> minecraft.setScreen(getScreen())));
        LegacyWorldTemplate.list.forEach(t -> addTemplateButton(this, t, c -> {
            if (t.isGamePath() && !Files.exists(t.getPath())) {
                Path path = t.getDownloadPath();
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
            template.albumId().ifPresent(id -> updateSelectedResourceAlbum(access, id));
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

    private static void updateSelectedResourceAlbum(LevelStorageSource.LevelStorageAccess access, String albumId) {
        try {
            CompoundTag root = NbtIo.readCompressed(access.getLevelPath(LevelResource.LEVEL_DATA_FILE), NbtAccounter.unlimitedHeap());
            CompoundTag data = root.getCompound("Data").orElseGet(CompoundTag::new);
            data.putString("SelectedResourceAssort", albumId);
            root.put("Data", data);
            NbtIo.writeCompressed(root, access.getLevelPath(LevelResource.LEVEL_DATA_FILE));
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

    public static void addTemplateButton(RenderableVList list, LegacyWorldTemplate template, Consumer<AbstractButton> onPress) {
        AbstractButton button;
        list.addRenderable(button = new ContentButton(list, 0, 0, 270, 30, template.buttonMessage()) {
            @Override
            public void renderIcon(GuiGraphics guiGraphics, int mouseX, int mouseY, int x, int y, int width, int height) {
                ResourceLocation icon = getTemplatePackIcon(template);
                if (icon != null) {
                    FactoryGuiGraphics.of(guiGraphics).blit(icon, getX() + x, getY() + y, 0.0f, 0.0f, width, height, width, height);
                    return;
                }
                FactoryGuiGraphics.of(guiGraphics).blitSprite(template.icon(), getX() + x, getY() + y, width, height);
            }

            @Override
            public void onPress(InputWithModifiers input) {
                onPress.accept(this);
            }
        });
    }

    private static ResourceLocation getTemplatePackIcon(LegacyWorldTemplate template) {
        String packId = template.albumId()
            .map(PackAlbum::resourceById)
            .map(PackAlbum::getDisplayPackId)
            .orElse(null);
        if (packId == null || packId.isBlank()) return null;
        if (packId.startsWith("file/")) packId = packId.substring(5);
        try {
            return packIcons.getUnchecked(packId);
        } catch (Exception e) {
            return null;
        }
    }


    public static abstract class ContentButton extends ListButton {
        public ContentButton(RenderableVList list, int x, int y, int width, int height, Component component) {
            super(list, x, y, width, height, component);
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
    }
}
