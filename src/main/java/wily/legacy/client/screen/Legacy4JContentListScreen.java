package wily.legacy.client.screen;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineLabel;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import wily.factoryapi.base.client.FactoryGuiGraphics;
import wily.legacy.Legacy4J;
import wily.legacy.Legacy4JClient;
import wily.legacy.client.CommonColor;
import wily.legacy.client.ContentManager;
import wily.legacy.client.ControlType;
import wily.legacy.client.LegacyOptions;
import wily.legacy.client.controller.ControllerBinding;
import wily.legacy.skins.skin.CustomSkinPackStore;
import wily.legacy.skins.skin.DownloadedSkinPackStore;
import wily.legacy.util.LegacySprites;
import wily.legacy.util.ScreenUtil;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class Legacy4JContentListScreen extends PanelVListScreen {
    private static final int PANEL_WIDTH = 257;
    private static final int PANEL_HEIGHT = 226;
    private static final int TOOLTIP_WIDTH = 240;
    private static final int LIST_X = 20;
    private static final int LIST_Y = 48;
    private static final int LIST_WIDTH = 218;
    private static final int LIST_HEIGHT = 162;
    private static final int BUTTON_HEIGHT = 30;
    private static final int HD_STORE_IMAGE_SIZE = 120;
    private static final int SD_STORE_IMAGE_SIZE = 90;
    private static final int STORE_IMAGE_SIZE_TOLERANCE = 4;

    private final ContentManager.Category category;
    private final List<ContentManager.Pack> packs;
    private final Panel tooltipBox = Panel.tooltipBoxOf(panel, TOOLTIP_WIDTH);
    private final Panel panelRecess;
    private final LegacyScrollRenderer scrollRenderer = new LegacyScrollRenderer();
    private final ScrollableRenderer scrollableRenderer = new ScrollableRenderer(scrollRenderer);
    private static final Map<String, RemoteImage> downloadedImages = new ConcurrentHashMap<>();
    private static final Map<String, CompletableFuture<RemoteImage>> pendingImageLoads = new ConcurrentHashMap<>();
    private static final java.util.Set<String> downloadingImages = ConcurrentHashMap.newKeySet();
    private final java.util.Set<String> downloadingPacks = ConcurrentHashMap.newKeySet();
    private final Map<String, Boolean> installedPacks = new ConcurrentHashMap<>();
    private final Map<String, MultiLineLabel> descriptionLabels = new ConcurrentHashMap<>();
    private ContentManager.Pack hoveredPack;
    private boolean needsReload;

    private record RemoteImage(ResourceLocation id, int width, int height) {
    }

    private record ImageSize(int width, int height) {
    }

    private static class StorePreviewTexture extends DynamicTexture {
        public StorePreviewTexture(/*? if >=1.21.5 {*//*java.util.function.Supplier<String> name, *//*?}*/NativeImage image) {
            super(/*? if >=1.21.5 {*//*name, *//*?}*/image);
            setFilter(true, false);
            //? if >=1.21.5 {
            /*setClamp(true);
            *///?}
        }
    }

    private static ImageSize fitRemoteImage(RemoteImage image, int maxWidth, int maxHeight) {
        if (image.width == image.height) {
            int size = LegacyOptions.getUIMode().isSD() ? SD_STORE_IMAGE_SIZE : HD_STORE_IMAGE_SIZE;
            if (size <= maxWidth && size <= maxHeight + STORE_IMAGE_SIZE_TOLERANCE) {
                return new ImageSize(size, size);
            }
        }
        float scale = Math.min((float) maxWidth / image.width, (float) maxHeight / image.height);
        return new ImageSize(Math.max(1, Math.round(image.width * scale)), Math.max(1, Math.round(image.height * scale)));
    }

    public Legacy4JContentListScreen(Screen parent, ContentManager.Category category, List<ContentManager.Pack> packs) {
        super(parent, s -> Panel.createPanel(s,
            p -> p.appearance(PANEL_WIDTH, PANEL_HEIGHT),
            p -> p.pos(p.centeredLeftPos(s), p.centeredTopPos(s) + 17)
        ), Component.translatable("legacy.menu.store_title"));
        panelRecess = Panel.createPanel(this,
            p -> p.appearance(LegacySprites.PANEL_RECESS, panel.getWidth() - 20, panel.getHeight() - 40),
            p -> p.pos(panel.getX() + 10, panel.getY() + 30)
        );
        this.category = category;
        this.packs = packs;
        renderableVList.layoutSpacing(l -> 0);
        packs.forEach(pack -> installedPacks.put(pack.id(), ContentManager.isPackInstalled(pack, category)));
        if (!packs.isEmpty()) {
            hoveredPack = packs.get(0);
            requestImage(hoveredPack.imageUrl());
        }
        packs.forEach(this::addMenuButton);
    }

    private void addMenuButton(ContentManager.Pack pack) {
        renderableVList.addRenderable(new PackButton(0, 0, LIST_WIDTH, BUTTON_HEIGHT, pack) {
            @Override
            public void onPress() {
                if (isDownloading(pack)) return;
                selectPack(pack);
                if (!isFocused()) return;
                if (isInstalled(pack)) minecraft.setScreen(createDeleteScreen(pack));
                else if (prepareDownloadTarget(pack)) startDownload(pack);
            }

            @Override
            public void setFocused(boolean focused) {
                super.setFocused(focused);
                if (focused) selectPack(pack);
            }
        });
    }

    private Screen createDeleteScreen(ContentManager.Pack pack) {
        return new ConfirmationScreen(this, ConfirmationScreen::getPanelWidth, () -> 95, pack.nameComponent(), Component.translatable("legacy.menu.delete_message"), b -> {}) {
            @Override
            protected void addButtons() {
                renderableVList.addRenderable(Button.builder(Component.translatable("gui.cancel"), b -> minecraft.setScreen(parent))
                        .bounds(panel.x + 15, panel.getRectangle().bottom() - 52, 200, 20).build());
                renderableVList.addRenderable(Button.builder(Component.translatable("legacy.menu.delete"), b -> {
                    deletePack(pack);
                    minecraft.setScreen(parent);
                }).bounds(panel.x + 15, panel.getRectangle().bottom() - 30, 200, 20).build());
            }
        };
    }

    private void selectPack(ContentManager.Pack pack) {
        if (hoveredPack == pack) {
            requestImage(pack.imageUrl());
            return;
        }
        hoveredPack = pack;
        scrollableRenderer.scrolled.set(0);
        requestImage(pack.imageUrl());
    }

    private boolean isInstalled(ContentManager.Pack pack) {
        return installedPacks.computeIfAbsent(pack.id(), id -> ContentManager.isPackInstalled(pack, category));
    }

    private boolean isDownloading(ContentManager.Pack pack) {
        return downloadingPacks.contains(pack.id()) || ContentManager.isPackDownloading(pack, category);
    }

    private boolean prepareDownloadTarget(ContentManager.Pack pack) {
        if (!prepareManagedTarget(category)) return false;
        if (!pack.hasBundlePacks()) return true;
        for (ContentManager.Pack.BundlePack bundlePack : pack.bundlePacks()) {
            Optional<ContentManager.Category> bundleCategory = ContentManager.CATEGORIES.stream().filter(c -> c.id().equals(bundlePack.categoryId())).findFirst();
            if (bundleCategory.isPresent() && !bundleCategory.get().id().equals(category.id()) && !prepareManagedTarget(bundleCategory.get())) return false;
        }
        return true;
    }

    private boolean prepareManagedTarget(ContentManager.Category targetCategory) {
        if (!DownloadedSkinPackStore.managesTargetDirectory(targetCategory.targetDirectoryName()) && !CustomSkinPackStore.managesTargetDirectory(targetCategory.targetDirectoryName())) return true;
        try {
            if (DownloadedSkinPackStore.managesTargetDirectory(targetCategory.targetDirectoryName())) DownloadedSkinPackStore.enableResourcePack(minecraft);
            else CustomSkinPackStore.enableResourcePack(minecraft);
            return true;
        } catch (IOException e) {
            String message = e.getMessage();
            minecraft.setScreen(ConfirmationScreen.createInfoScreen(this, targetCategory.title(), Component.literal(message == null || message.isBlank() ? e.toString() : message)));
            return false;
        }
    }

    private void startDownload(ContentManager.Pack pack) {
        downloadingPacks.add(pack.id());
        ContentManager.downloadPack(pack, category, installedAnything -> finishDownload(pack, installedAnything));
    }

    private void finishDownload(ContentManager.Pack pack, boolean installedAnything) {
        downloadingPacks.remove(pack.id());
        refreshInstalledPacks();
        boolean appliedResourcePacks = installedAnything && ContentManager.applyAutoResourcePacks(pack, category);
        if (!installedAnything || (!category.requiresResourceReload() && !appliedResourcePacks)) return;
        if (minecraft.screen == this) needsReload = true;
        else minecraft.reloadResourcePacks();
    }

    private void refreshInstalledPacks() {
        packs.forEach(pack -> installedPacks.put(pack.id(), ContentManager.isPackInstalled(pack, category)));
    }

    private void deletePack(ContentManager.Pack pack) {
        ContentManager.deletePack(pack, category);
        installedPacks.put(pack.id(), false);
        needsReload = true;
    }

    private MultiLineLabel getDescriptionLabel(ContentManager.Pack pack, int width) {
        boolean sd = LegacyOptions.getUIMode().isSD();
        return descriptionLabels.computeIfAbsent(pack.id() + ":" + width + ":" + sd, id -> {
            MultiLineLabel[] label = new MultiLineLabel[1];
            Legacy4JClient.applyFontOverrideIf(sd, LegacyIconHolder.MOJANGLES_11_FONT, ignored -> label[0] = MultiLineLabel.create(font, pack.descriptionComponent(), width));
            return label[0];
        });
    }

    private static RemoteImage getOrDownloadImage(Optional<URI> url) {
        if (url.isEmpty()) return null;
        String key = url.get().toString();
        RemoteImage image = downloadedImages.get(key);
        if (image != null) return image;
        requestImage(url);
        return null;
    }

    private static CompletableFuture<RemoteImage> requestImage(Optional<URI> url) {
        if (url.isEmpty()) return CompletableFuture.completedFuture(null);
        String key = url.get().toString();
        RemoteImage image = downloadedImages.get(key);
        if (image != null) return CompletableFuture.completedFuture(image);
        CompletableFuture<RemoteImage> pending = pendingImageLoads.get(key);
        if (pending != null) return pending;
        CompletableFuture<RemoteImage> future = new CompletableFuture<>();
        CompletableFuture<RemoteImage> existing = pendingImageLoads.putIfAbsent(key, future);
        if (existing != null) return existing;
        if (downloadingImages.contains(key)) return future;
        downloadingImages.add(key);
        CompletableFuture.runAsync(() -> {
            Minecraft client = Minecraft.getInstance();
            try (InputStream in = ContentManager.openRemoteStream(url.get().toURL(), 5000, 10000)) {
                NativeImage nativeImage = NativeImage.read(in);
                int width = nativeImage.getWidth();
                int height = nativeImage.getHeight();
                client.execute(() -> {
                    ResourceLocation id = Legacy4J.createModLocation("pack_image/" + Integer.toHexString(key.hashCode()));
                    client.getTextureManager().register(id, new StorePreviewTexture(/*? if >=1.21.5 {*//*id::toString, *//*?}*/nativeImage));
                    RemoteImage remoteImage = new RemoteImage(id, width, height);
                    downloadedImages.put(key, remoteImage);
                    future.complete(remoteImage);
                });
            } catch (Exception e) {
                RemoteImage remoteImage = new RemoteImage(null, 0, 0);
                downloadedImages.put(key, remoteImage);
                Legacy4J.LOGGER.warn("Failed to load content image {}", key, e);
                client.execute(() -> future.complete(remoteImage));
            } finally {
                client.execute(() -> {
                    downloadingImages.remove(key);
                    pendingImageLoads.remove(key);
                });
            }
        });
        return future;
    }

    @Override
    public void onClose() {
        if (needsReload) minecraft.reloadResourcePacks();
        super.onClose();
    }

    @Override
    public void addControlTooltips(ControlTooltip.Renderer renderer) {
        super.addControlTooltips(renderer);
        renderer.add(() -> ControlType.getActiveType().isKbm() ? ControlTooltip.getKeyIcon(InputConstants.KEY_RETURN) : ControllerBinding.DOWN_BUTTON.getIcon(), () -> hoveredPack != null && isInstalled(hoveredPack) ? Component.translatable("legacy.menu.delete") : Component.translatable("legacy.menu.install"));
    }

    @Override
    protected void panelInit() {
        super.panelInit();
        tooltipBox.init();
        panelRecess.init("panelRecess");
        addRenderableOnly(panelRecess);
        addRenderableOnly((guiGraphics, i, j, f) -> Legacy4JClient.applyFontOverrideIf(LegacyOptions.getUIMode().isSD(), LegacyIconHolder.MOJANGLES_11_FONT, ignored ->
                guiGraphics.drawString(font, getTitle(), panel.x + (panel.width - font.width(getTitle())) / 2, panelRecess.y + 8, CommonColor.GRAY_TEXT.get(), false)));
    }

    @Override
    public Component getTitle() {
        return packs.isEmpty() ? Component.translatable("legacy.menu.store_no_content") : super.getTitle();
    }

    @Override
    public void renderableVListInit() {
        for (int i = 0; i < Math.min(4, packs.size()); i++) {
            requestImage(packs.get(i).imageUrl());
        }
        getRenderableVList().init("renderableVList", panel.x + LIST_X, panel.y + LIST_Y, LIST_WIDTH, LIST_HEIGHT);
    }

    @Override
    public void renderDefaultBackground(GuiGraphics guiGraphics, int i, int j, float f) {
        ScreenUtil.renderDefaultBackground(accessor, guiGraphics, false);
        ScreenUtil.renderLogo(guiGraphics);
        tooltipBox.render(guiGraphics, i, j, f);
        renderTooltip(guiGraphics);
    }

    private void renderTooltip(GuiGraphics guiGraphics) {
        if (hoveredPack == null) return;
        int x = tooltipBox.x + 8;
        int y = tooltipBox.y + 8;
        int width = tooltipBox.width - 16;
        int imageHeight = renderPreviewImage(guiGraphics, hoveredPack, x, y, width);
        MultiLineLabel label = getDescriptionLabel(hoveredPack, width);
        int lineHeight = LegacyOptions.getUIMode().isSD() ? 8 : 12;
        int descriptionY = y + imageHeight;
        int visibleLines = Math.max(0, (tooltipBox.y + tooltipBox.height - 24 - descriptionY) / lineHeight);
        scrollableRenderer.scrolled.max = Math.max(0, label.getLineCount() - visibleLines);
        scrollableRenderer.lineHeight = lineHeight;
        scrollableRenderer.render(guiGraphics, x, descriptionY, width, visibleLines * lineHeight, () ->
                Legacy4JClient.applyFontOverrideIf(LegacyOptions.getUIMode().isSD(), LegacyIconHolder.MOJANGLES_11_FONT, ignored ->
                        label.renderLeftAligned(guiGraphics, x, descriptionY, lineHeight, CommonColor.TIP_TEXT.get())));
    }

    private int renderPreviewImage(GuiGraphics guiGraphics, ContentManager.Pack pack, int x, int y, int width) {
        int maxHeight = (tooltipBox.height * 2) / 3;
        if (pack.imageUrl().isEmpty()) return 0;
        RemoteImage remoteImage = getOrDownloadImage(pack.imageUrl());
        if (remoteImage != null && remoteImage.id() != null && remoteImage.width() > 0 && remoteImage.height() > 0) {
            ImageSize imageSize = fitRemoteImage(remoteImage, width, maxHeight);
            int imageWidth = imageSize.width();
            int imageHeight = imageSize.height();
            FactoryGuiGraphics.of(guiGraphics).blit(remoteImage.id(), x + (width - imageWidth) / 2, y, 0.0f, 0.0f, imageWidth, imageHeight, imageWidth, imageHeight);
            return imageHeight + 10;
        }
        if (remoteImage == null) {
            ScreenUtil.drawGenericLoading(guiGraphics, x + (width - 30) / 2, y + Math.max(0, (maxHeight - 30) / 2), 6, 3);
            return maxHeight + 10;
        }
        return 0;
    }

    @Override
    public boolean mouseScrolled(double d, double e/*? if >1.20.1 {*/, double f/*?}*/, double g) {
        if ((tooltipBox.isHovered(d, e) || !ControlType.getActiveType().isKbm()) && scrollableRenderer.mouseScrolled(g)) return true;
        return super.mouseScrolled(d, e/*? if >1.20.1 {*/, f/*?}*/, g);
    }

    private abstract class PackButton extends AbstractButton {
        private final ContentManager.Pack pack;

        PackButton(int x, int y, int width, int height, ContentManager.Pack pack) {
            super(x, y, width, height, pack.nameComponent());
            this.pack = pack;
        }

        @Override
        protected void renderWidget(GuiGraphics guiGraphics, int i, int j, float f) {
            super.renderWidget(guiGraphics, i, j, f);
            if (isDownloading(pack)) {
                ScreenUtil.drawGenericLoading(guiGraphics, getX() + getWidth() - 24, getY() + 7, 4, 2);
            } else if (isInstalled(pack)) {
                FactoryGuiGraphics.of(guiGraphics).blitSprite(LegacySprites.BEACON_CONFIRM, getX() + getWidth() - 24, getY() + 6, 18, 18);
            }
        }

        @Override
        protected void renderScrollingString(GuiGraphics guiGraphics, Font font, int i, int color) {
            int textY = getY() + (getHeight() - font.lineHeight) / 2 + 1;
            int textX = getX() + 8;
            boolean hasStatusIcon = isDownloading(pack) || isInstalled(pack);
            int maxWidth = getWidth() - (hasStatusIcon ? 44 : 16);
            Legacy4JClient.applyFontOverrideIf(LegacyOptions.getUIMode().isSD(), LegacyIconHolder.MOJANGLES_11_FONT, ignored -> {
                String text = getMessage() == null ? "" : getMessage().getString();
                String clipped = font.width(text) <= maxWidth ? text : font.plainSubstrByWidth(text, Math.max(0, maxWidth - font.width("..."))) + "...";
                guiGraphics.drawString(font, clipped, textX, textY, color, true);
            });
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
            defaultButtonNarrationText(narrationElementOutput);
        }
    }
}
