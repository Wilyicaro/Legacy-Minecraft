package wily.legacy.client.screen;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
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
import wily.factoryapi.util.FactoryScreenUtil;
import wily.legacy.Legacy4J;
import wily.legacy.Legacy4JClient;
import wily.legacy.client.ContentManager;
import wily.legacy.client.DownloadedResourceAlbums;
import wily.legacy.client.LegacyOptions;
import wily.legacy.client.LegacyWorldTemplate;
import wily.legacy.client.PackAlbum;
import wily.legacy.util.LegacyComponents;
import wily.legacy.util.ScreenUtil;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class CreationList extends RenderableVList {
    private static final String MASHUP_CATEGORY_ID = "mashup_packs";
    protected final Minecraft minecraft;
    private static final Map<String, ResourceLocation> remoteWorldIcons = new ConcurrentHashMap<>();
    private static final Set<String> pendingRemoteWorldIcons = ConcurrentHashMap.newKeySet();
    private ContentManager.Category mashupCategory;
    private List<ContentManager.Pack> mashupPacks = List.of();
    private static final LoadingCache<String, ResourceLocation> packIcons = CacheBuilder.newBuilder().build(new CacheLoader<>() {
        @Override
        public ResourceLocation load(String key) throws Exception {
            java.nio.file.Path path = Minecraft.getInstance().getResourcePackDirectory().resolve(key).resolve("pack.png");
            if (!Files.isRegularFile(path)) return PackAlbum.Selector.DEFAULT_ICON;
            try (InputStream inputStream = Files.newInputStream(path)) {
                NativeImage image = NativeImage.read(inputStream);
                ResourceLocation location = Legacy4J.createModLocation("template_pack_icon/" + Integer.toHexString(key.hashCode()));
                Minecraft.getInstance().getTextureManager().register(location, new DynamicTexture(/*? if >=1.21.5 {*//*location::toString, *//*?}*/image));
                return location;
            }
        }
    });
    private static final LoadingCache<String, ResourceLocation> worldIcons = CacheBuilder.newBuilder().build(new CacheLoader<>() {
        @Override
        public ResourceLocation load(String key) throws Exception {
            try (ZipFile zip = new ZipFile(Path.of(key).toFile())) {
                ZipEntry entry = zip.getEntry("icon.png");
                if (entry == null) return PackAlbum.Selector.DEFAULT_ICON;
                try (InputStream inputStream = zip.getInputStream(entry)) {
                    NativeImage image = NativeImage.read(inputStream);
                    ResourceLocation location = Legacy4J.createModLocation("template_world_icon/" + Integer.toHexString(key.hashCode()));
                    Minecraft.getInstance().getTextureManager().register(location, new DynamicTexture(/*? if >=1.21.5 {*//*location::toString, *//*?}*/image));
                    return location;
                }
            }
        }
    });

    public CreationList(UIAccessor accessor) {
        super(accessor);
        layoutSpacing(l -> 0);
        minecraft = Minecraft.getInstance();
        rebuildEntries();
        ContentManager.CATEGORIES.stream().filter(category -> MASHUP_CATEGORY_ID.equals(category.id())).findFirst().ifPresent(category -> {
            mashupCategory = category;
            ContentManager.fetchIndex(category).thenAccept(packs -> {
                List<ContentManager.Pack> fetchedMashupPacks = packs.stream()
                    .filter(ContentManager.Pack::hasWorldTemplate)
                    .sorted(Comparator.comparing(ContentManager.Pack::name, String.CASE_INSENSITIVE_ORDER))
                    .toList();
                fetchedMashupPacks.forEach(pack -> getRemoteWorldIcon(pack.worldTemplateIconUrl()));
                minecraft.execute(() -> {
                    mashupPacks = fetchedMashupPacks;
                    LegacyWorldTemplate.refreshDownloadedPacks();
                    rebuildEntries();
                    if (minecraft.screen == getScreen() && getScreen() instanceof RenderableVListScreen screen) screen.repositionElements();
                });
            });
        });
    }

    private void rebuildEntries() {
        renderables.clear();
        addIconButton(this, Legacy4J.createModLocation("creation_list/create_world"), Component.translatable("legacy.menu.create_world"), button -> CreateWorldScreen.openFresh(minecraft, getScreen()));
        List<LegacyWorldTemplate> localTemplates = new ArrayList<>(LegacyWorldTemplate.list);
        localTemplates.stream().filter(template -> !isDownloadedTemplate(template)).forEach(this::addLocalTemplateButton);
        if (mashupPacks.isEmpty()) {
            localTemplates.stream().filter(CreationList::isDownloadedTemplate).forEach(this::addLocalTemplateButton);
            return;
        }

        Set<String> remoteAlbumIds = new HashSet<>();
        for (ContentManager.Pack pack : mashupPacks) {
            String albumId = DownloadedResourceAlbums.albumId(pack.id());
            remoteAlbumIds.add(albumId);
            Optional<LegacyWorldTemplate> localTemplate = localTemplates.stream()
                .filter(template -> template.albumId().filter(albumId::equals).isPresent())
                .findFirst();
            addMashupButton(pack, localTemplate);
        }
        localTemplates.stream()
            .filter(CreationList::isDownloadedTemplate)
            .filter(template -> template.albumId().filter(remoteAlbumIds::contains).isEmpty())
            .forEach(this::addLocalTemplateButton);
    }

    private void addLocalTemplateButton(LegacyWorldTemplate template) {
        addTemplateButton(this, template, button -> openTemplate(template));
    }

    private static boolean isDownloadedTemplate(LegacyWorldTemplate template) {
        return template.albumId().filter(DownloadedResourceAlbums::isManagedAlbum).isPresent();
    }

    private void openTemplate(LegacyWorldTemplate template) {
        if (template.isGamePath() && !Files.exists(template.getPath())) {
            Path path = template.getDownloadPath();
            if (path == null || template.preDownload()) {
                minecraft.setScreen(ConfirmationScreen.createInfoScreen(getScreen(), LegacyComponents.MISSING_WORLD_TEMPLATE, Component.translatable("legacy.menu.missing_world_template_message", template.buttonMessage())));
                return;
            }
            File file = path.toFile();
            Stocker<Long> fileSize = new Stocker<>(1L);
            ExecutorService executor = Executors.newSingleThreadExecutor();
            LegacyLoadingScreen screen = new LegacyLoadingScreen(LegacyComponents.DOWNLOADING_WORLD_TEMPLATE, CommonComponents.EMPTY) {
                @Override
                public void tick() {
                    setProgress(file.exists() ? Math.round(Math.min(1.0F, FileUtils.sizeOf(file) / (float) fileSize.get()) * 100.0F) : 0);
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
                    URL url = template.downloadURI().orElseThrow().toURL();
                    fileSize.set(url.openConnection().getContentLengthLong());
                    FileUtils.copyURLToFile(url, path.toFile());
                    minecraft.execute(() -> loadTemplate(getScreen(), minecraft, template));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }, executor);
            return;
        }
        loadTemplate(getScreen(), minecraft, template);
    }

    private void addMashupButton(ContentManager.Pack pack, Optional<LegacyWorldTemplate> localTemplate) {
        addRenderable(new IconButton(this, 0, 0, 270, 30, pack.nameComponent()) {
            @Override
            public void renderIcon(GuiGraphics guiGraphics, int mouseX, int mouseY, int x, int y, int width, int height) {
                if (!accessor.getBoolean("allowButtonsWithIcons", true)) return;
                FactoryScreenUtil.enableBlend();
                ResourceLocation icon = getRemoteWorldIcon(pack.worldTemplateIconUrl());
                if (icon != null) {
                    FactoryGuiGraphics.of(guiGraphics).blit(icon, getX() + x, getY() + y, 0.0f, 0.0f, width, height, width, height);
                } else if (pack.worldTemplateIconUrl().map(URI::toString).filter(pendingRemoteWorldIcons::contains).isPresent()) {
                    ScreenUtil.drawGenericLoading(guiGraphics, getX() + x, getY() + y, (width - 2) / 3, 1);
                } else {
                    icon = localTemplate.map(CreationList::getTemplateWorldIcon).orElse(null);
                    if (icon != null) {
                        FactoryGuiGraphics.of(guiGraphics).blit(icon, getX() + x, getY() + y, 0.0f, 0.0f, width, height, width, height);
                    } else {
                        FactoryGuiGraphics.of(guiGraphics).blitSprite(Legacy4J.createModLocation("creation_list/create_world"), getX() + x, getY() + y, width, height);
                    }
                }
                FactoryScreenUtil.disableBlend();
            }

            @Override
            public void renderIconHighlight(GuiGraphics guiGraphics, int mouseX, int mouseY, int x, int y, int width, int height) {
                if (accessor.getBoolean("allowButtonsWithIcons", true)) super.renderIconHighlight(guiGraphics, mouseX, mouseY, x, y, width, height);
            }

            @Override
            public void onPress() {
                if (localTemplate.isPresent() && (mashupCategory == null || ContentManager.isPackInstalled(pack, mashupCategory))) {
                    openTemplate(localTemplate.get());
                } else {
                    installAndOpenMashup(pack);
                }
            }
        });
    }

    private void installAndOpenMashup(ContentManager.Pack pack) {
        if (mashupCategory == null) return;
        Screen parent = getScreen();
        try {
            ContentManager.prepareDownloadTarget(pack, mashupCategory);
        } catch (IOException e) {
            Component message = e.getMessage() == null || e.getMessage().isBlank() ? Component.literal(e.toString()) : Component.literal(e.getMessage());
            minecraft.setScreen(ConfirmationScreen.createInfoScreen(parent, pack.nameComponent(), message));
            return;
        }

        LegacyLoadingScreen loadingScreen = new LegacyLoadingScreen(LegacyComponents.DOWNLOADING_WORLD_TEMPLATE, pack.nameComponent());
        loadingScreen.setGenericLoading(true);
        minecraft.setScreen(loadingScreen);
        ContentManager.downloadPack(pack, mashupCategory, installedAnything -> {
            if (!ContentManager.isPackInstalled(pack, mashupCategory)) {
                showMashupInstallFailure(parent, pack);
                return;
            }
            ContentManager.applyAutoResourcePacks(pack, mashupCategory);
            minecraft.reloadResourcePacks().whenComplete((unused, throwable) -> minecraft.execute(() -> {
                if (throwable != null) {
                    Legacy4J.LOGGER.warn("Failed to reload resources after installing mash-up {}", pack.id(), throwable);
                    showMashupInstallFailure(parent, pack);
                    return;
                }
                LegacyWorldTemplate.refreshDownloadedPacks();
                Optional<LegacyWorldTemplate> downloadedTemplate = findDownloadedTemplate(pack);
                if (downloadedTemplate.isPresent()) loadTemplate(parent, minecraft, downloadedTemplate.get());
                else showMashupInstallFailure(parent, pack);
            }));
        });
    }

    private void showMashupInstallFailure(Screen parent, ContentManager.Pack pack) {
        minecraft.setScreen(ConfirmationScreen.createInfoScreen(parent, LegacyComponents.MISSING_WORLD_TEMPLATE, Component.translatable("legacy.menu.missing_world_template_message", pack.nameComponent())));
    }

    private static Optional<LegacyWorldTemplate> findDownloadedTemplate(ContentManager.Pack pack) {
        String albumId = DownloadedResourceAlbums.albumId(pack.id());
        return LegacyWorldTemplate.list.stream().filter(template -> template.albumId().filter(albumId::equals).isPresent()).findFirst();
    }

    private static ResourceLocation getRemoteWorldIcon(Optional<URI> iconUrl) {
        if (iconUrl.isEmpty()) return null;
        String key = iconUrl.get().toString();
        ResourceLocation cached = remoteWorldIcons.get(key);
        if (cached != null) return PackAlbum.Selector.DEFAULT_ICON.equals(cached) ? null : cached;
        if (!pendingRemoteWorldIcons.add(key)) return null;
        CompletableFuture.runAsync(() -> {
            Minecraft client = Minecraft.getInstance();
            try (InputStream stream = ContentManager.openRemoteStream(iconUrl.get().toURL(), 5000, 10000)) {
                NativeImage image = NativeImage.read(stream);
                client.execute(() -> {
                    ResourceLocation location = Legacy4J.createModLocation("remote_world_icon/" + Integer.toHexString(key.hashCode()));
                    client.getTextureManager().register(location, new DynamicTexture(/*? if >=1.21.5 {*//*location::toString, *//*?}*/image));
                    remoteWorldIcons.put(key, location);
                    pendingRemoteWorldIcons.remove(key);
                });
            } catch (Exception e) {
                Legacy4J.LOGGER.warn("Failed to load mash-up world icon {}", key, e);
                client.execute(() -> {
                    remoteWorldIcons.put(key, PackAlbum.Selector.DEFAULT_ICON);
                    pendingRemoteWorldIcons.remove(key);
                });
            }
        });
        return null;
    }

    public static void loadTemplate(Screen parent, Minecraft minecraft, LegacyWorldTemplate template) {
        try (LevelStorageSource.LevelStorageAccess access = Legacy4JClient.getLevelStorageSource().createAccess(Legacy4JClient.importSaveFile(template.open(), minecraft.getLevelSource()::levelExists, Legacy4JClient.getLevelStorageSource(), template.folderName()))) {
            LevelSummary summary = access.getSummary(/*? if >1.20.2 {*/access.getDataTag()/*?}*/);
            access.close();
            if (template.directJoin()) {
                Legacy4JClient.hideNextExperimentalWorldWarning(() -> LoadSaveScreen.loadWorld(parent, minecraft, Legacy4JClient.getLevelStorageSource(), summary));
            } else minecraft.setScreen(new LoadSaveScreen(parent, summary, access, template.isLocked()) {
                @Override
                public void onClose() {
                    if (!LegacyOptions.saveCache.get() || LegacyOptions.alwaysClearSaveCache.get()) FileUtils.deleteQuietly(access.getDimensionPath(Level.OVERWORLD).toFile());
                    super.onClose();
                }
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void addIconButton(RenderableVList list, ResourceLocation iconSprite, Component message, Consumer<AbstractButton> onPress){
        addIconButton(list,iconSprite,message,onPress,null);
    }

    public static void addIconButton(RenderableVList list, ResourceLocation iconSprite, Component message, Consumer<AbstractButton> onPress, Tooltip tooltip){
        AbstractButton button;
        list.addRenderable(button = new IconButton(list,0,0,270,30,message) {
            @Override
            public void renderIcon(GuiGraphics guiGraphics, int mouseX, int mouseY, int x, int y, int width, int height) {
                if (!list.accessor.getBoolean("allowButtonsWithIcons",true)) return;
                FactoryScreenUtil.enableBlend();
                FactoryGuiGraphics.of(guiGraphics).blitSprite(iconSprite, getX() + x, getY() + y, width, height);
                FactoryScreenUtil.disableBlend();
            }

            @Override
            public void renderIconHighlight(GuiGraphics guiGraphics, int mouseX, int mouseY, int x, int y, int width, int height) {
                if (list.accessor.getBoolean("allowButtonsWithIcons",true)) super.renderIconHighlight(guiGraphics, mouseX, mouseY, x, y, width, height);
            }

            @Override
            public void onPress() {
                onPress.accept(this);
            }
        });
        button.setTooltip(tooltip);
    }

    public static void addTemplateButton(RenderableVList list, LegacyWorldTemplate template, Consumer<AbstractButton> onPress){
        AbstractButton button;
        list.addRenderable(button = new IconButton(list,0,0,270,30,template.buttonMessage()) {
            @Override
            public void renderIcon(GuiGraphics guiGraphics, int mouseX, int mouseY, int x, int y, int width, int height) {
                if (!list.accessor.getBoolean("allowButtonsWithIcons",true)) return;
                FactoryScreenUtil.enableBlend();
                ResourceLocation icon = getTemplateWorldIcon(template);
                if (icon == null && !isDownloadedTemplate(template)) icon = getTemplatePackIcon(template);
                if (icon == null) FactoryGuiGraphics.of(guiGraphics).blitSprite(template.icon(), getX() + x, getY() + y, width, height);
                else FactoryGuiGraphics.of(guiGraphics).blit(icon, getX() + x, getY() + y, 0.0f, 0.0f, width, height, width, height);
                FactoryScreenUtil.disableBlend();
            }

            @Override
            public void renderIconHighlight(GuiGraphics guiGraphics, int mouseX, int mouseY, int x, int y, int width, int height) {
                if (list.accessor.getBoolean("allowButtonsWithIcons",true)) super.renderIconHighlight(guiGraphics, mouseX, mouseY, x, y, width, height);
            }

            @Override
            public void onPress() {
                onPress.accept(this);
            }
        });
    }

    private static ResourceLocation getTemplateWorldIcon(LegacyWorldTemplate template) {
        if (template.albumId().isEmpty() || !template.isGamePath()) return null;
        Path path = template.getPath().toAbsolutePath().normalize();
        if (!Files.isRegularFile(path)) return null;
        try {
            ResourceLocation icon = worldIcons.getUnchecked(path.toString());
            return PackAlbum.Selector.DEFAULT_ICON.equals(icon) ? null : icon;
        } catch (Exception e) {
            return null;
        }
    }

    private static ResourceLocation getTemplatePackIcon(LegacyWorldTemplate template) {
        String packId = template.albumId()
            .map(PackAlbum::resourceById)
            .map(PackAlbum::getDisplayPackId)
            .orElse(null);
        if (packId == null || packId.isBlank()) return null;
        if (packId.startsWith("file/")) packId = packId.substring(5);
        try {
            ResourceLocation icon = packIcons.getUnchecked(packId);
            return PackAlbum.Selector.DEFAULT_ICON.equals(icon) ? null : icon;
        } catch (Exception e) {
            return null;
        }
    }

    public static void invalidateWorldIcon(Path path) {
        invalidateIcon(worldIcons, path.toAbsolutePath().normalize().toString());
    }

    public static void invalidatePackIcon(String packId) {
        invalidateIcon(packIcons, packId.startsWith("file/") ? packId.substring(5) : packId);
    }

    private static void invalidateIcon(LoadingCache<String, ResourceLocation> cache, String key) {
        ResourceLocation icon = cache.getIfPresent(key);
        cache.invalidate(key);
        if (icon != null && !PackAlbum.Selector.DEFAULT_ICON.equals(icon)) {
            Minecraft.getInstance().getTextureManager().release(icon);
        }
    }

}
