package wily.legacy.client.screen;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.client.renderer.texture.DynamicTexture;
import wily.legacy.client.ContentManager;
import wily.legacy.client.StorePreviewAtlas;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;

import wily.factoryapi.base.client.FactoryGuiGraphics;
import wily.factoryapi.base.client.UIAccessor;
import wily.legacy.Legacy4J;
import wily.legacy.skins.skin.CustomSkinPackStore;
import wily.legacy.skins.skin.DownloadedSkinPackStore;
import wily.legacy.client.CommonColor;
import wily.legacy.client.ControlType;
import wily.legacy.client.controller.ControllerBinding;
import wily.legacy.skins.client.preview.PlayerSkinWidget;
import wily.legacy.util.LegacySprites;
import wily.legacy.util.client.LegacyRenderUtil;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class Legacy4JContentListScreen extends PanelVListScreen implements ControlTooltip.Event {
    private static final int PANEL_WIDTH = 257;
    private static final int PANEL_HEIGHT = 226;
    private static final int TOOLTIP_WIDTH = 240;
    private static final int LIST_X = 20;
    private static final int LIST_Y = 48;
    private static final int LIST_WIDTH = 218;
    private static final int LIST_HEIGHT = 162;
    private static final int BUTTON_HEIGHT = 30;
    
    protected final ContentManager.Category category;
    protected final List<ContentManager.Pack> packs;
    protected ContentManager.Pack hoveredPack;
    protected final Panel tooltipBox = Panel.tooltipBoxOf(panel, TOOLTIP_WIDTH);
    private final Panel panelRecess;
    
    // Legacy Scrolling System
    protected final LegacyScrollRenderer scrollRenderer = new LegacyScrollRenderer();
    public final ScrollableRenderer scrollableRenderer = new ScrollableRenderer(scrollRenderer);
    
    private static final Map<String, RemoteImage> downloadedImages = new ConcurrentHashMap<>();
    private static final java.util.Set<String> downloadingImages = ConcurrentHashMap.newKeySet();
    private static final Map<String, CompletableFuture<RemoteImage>> pendingImageLoads = new ConcurrentHashMap<>();
    private final java.util.Set<String> downloadingPacks = ConcurrentHashMap.newKeySet();
    private final Map<String, Boolean> installedPacks = new ConcurrentHashMap<>();
    private final Map<String, MultiLineLabel> descriptionLabels = new ConcurrentHashMap<>();
    private boolean needsReload = false;

    private record RemoteImage(Identifier id, int width, int height) {
    }

    public Legacy4JContentListScreen(Screen parent, ContentManager.Category category, List<ContentManager.Pack> packs) {
        super(s -> Panel.createPanel(s,
                p -> p.appearance(PANEL_WIDTH, PANEL_HEIGHT), 
                p -> p.pos(p.centeredLeftPos(s), p.centeredTopPos(s) + 17)),
                Component.translatable("legacy.menu.store_title")
        );
        panelRecess = Panel.createPanel(this,
                p -> p.appearance(LegacySprites.PANEL_RECESS, panel.getWidth() - 20, panel.getHeight() - 40),
                p -> p.pos(panel.getX() + 10, panel.getY() + 30));
        this.parent = parent;
        this.category = category;
        this.packs = packs;
        packs.forEach(pack -> installedPacks.put(pack.id(), ContentManager.isPackInstalled(pack, category)));
        if (!packs.isEmpty()) {
            hoveredPack = packs.get(0);
            if (getLocalPreview(category, hoveredPack) == null) requestImage(hoveredPack.imageUrl());
        }
        
        renderableVList.layoutSpacing(l -> 0);
        for (ContentManager.Pack pack : packs) {
            addMenuButton(pack);
        }
    }

    private boolean isInstalled(ContentManager.Pack pack) {
        return installedPacks.computeIfAbsent(pack.id(), id -> ContentManager.isPackInstalled(pack, category));
    }

    private MultiLineLabel getDescriptionLabel(ContentManager.Pack pack, int width) {
        return descriptionLabels.computeIfAbsent(pack.id(), id -> MultiLineLabel.create(font, pack.descriptionComponent(), width));
    }

    private boolean isDownloading(ContentManager.Pack pack) {
        return downloadingPacks.contains(pack.id()) || ContentManager.isPackDownloading(pack, category);
    }

    private void selectPack(ContentManager.Pack pack) {
        if (hoveredPack == pack) {
            if (getLocalPreview(category, pack) == null) requestImage(pack.imageUrl());
            return;
        }
        hoveredPack = pack;
        scrollableRenderer.resetScrolled();
        if (getLocalPreview(category, pack) == null) requestImage(pack.imageUrl());
    }

    private void addMenuButton(ContentManager.Pack pack) {
        renderableVList.addRenderable(new LeftAlignedButton(renderableVList, LIST_WIDTH, BUTTON_HEIGHT, pack, category, downloadingPacks, installedPacks) {
            @Override
            public void onPress(InputWithModifiers inputWithModifiers) {
                if (isDownloading(pack)) return;
                selectPack(pack);
                if (!isFocused()) {
                    return;
                }
                if (isInstalled(pack)) {
                    minecraft.setScreen(new PackActionScreen(Legacy4JContentListScreen.this, pack, category));
                } else {
                    if (!prepareDownloadTarget(pack)) return;
                    startDownload(pack, category.requiresResourceReload());
                }
            }

            @Override
            public void setFocused(boolean focused) {
                super.setFocused(focused);
                if (focused) selectPack(pack);
            }
        }); 
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

    private void startDownload(ContentManager.Pack pack, boolean reloadResources) {
        downloadingPacks.add(pack.id());
        ContentManager.downloadPack(pack, category, installedAnything -> finishDownload(pack, installedAnything, reloadResources));
    }

    private void finishDownload(ContentManager.Pack pack, boolean installedAnything, boolean reloadResources) {
        downloadingPacks.remove(pack.id());
        refreshInstalledPacks();
        if (installedAnything && reloadResources) {
            if (minecraft.screen == this) needsReload = true;
            else minecraft.reloadResourcePacks();
        }
    }

    private void refreshInstalledPacks() {
        packs.forEach(pack -> installedPacks.put(pack.id(), ContentManager.isPackInstalled(pack, category)));
    }

    @Override
    public void onClose() {
        if (this.needsReload && this.category.requiresResourceReload()) {
            minecraft.reloadResourcePacks();
        }
        super.onClose();
    }

    private void deletePack(ContentManager.Pack pack) {
        ContentManager.deletePack(pack, category);
        installedPacks.put(pack.id(), false);
        needsReload = true;
    }

    private static RemoteImage getOrDownloadImage(Optional<URI> url) {
        if (url.isEmpty()) return null;
        String key = url.get().toString();
        RemoteImage image = downloadedImages.get(key);
        if (image != null) return image;
        requestImage(url);
        return null;
    }

    private static StorePreviewAtlas.Entry getLocalPreview(ContentManager.Category category, ContentManager.Pack pack) {
        if ("skinpacks".equals(category.id())) {
            StorePreviewAtlas.Entry skinpackPreview = StorePreviewAtlas.getSkinpack(pack.id());
            if (skinpackPreview != null) return skinpackPreview;
        }
        return StorePreviewAtlas.get(pack.id());
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
                int nativeWidth = nativeImage.getWidth();
                int nativeHeight = nativeImage.getHeight();
                
                client.execute(() -> {
                    String cleanId = Integer.toHexString(key.hashCode());
                    Identifier textureId = Identifier.fromNamespaceAndPath("legacy", "pack_image_" + cleanId);
                    
                    client.getTextureManager().register(textureId, new DynamicTexture(() -> "pack_image_" + cleanId, nativeImage));
                    
                    RemoteImage remoteImage = new RemoteImage(textureId, nativeWidth, nativeHeight);
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
    public void addControlTooltips(ControlTooltip.Renderer renderer) {
        boolean isKbm = ControlType.getActiveType().isKbm();

        // Contextual Install/Delete Select
        renderer.add(() -> isKbm ? ControlTooltip.getKeyIcon(InputConstants.KEY_RETURN) : ControllerBinding.DOWN_BUTTON.bindingState.getIcon(), () -> {
            if (hoveredPack != null && isDownloading(hoveredPack)) {
                return Component.translatable("legacy.menu.install");
            }
            if (hoveredPack != null && isInstalled(hoveredPack)) {
                return Component.translatable("legacy.menu.delete");
            }
            return Component.translatable("legacy.menu.install");
        });

        // Cancel
        renderer.add(() -> isKbm ? ControlTooltip.getKeyIcon(InputConstants.KEY_ESCAPE) : ControllerBinding.RIGHT_BUTTON.bindingState.getIcon(), () -> Component.translatable("gui.back"));
    }

    @Override
    public void initRenderableVListEntry(RenderableVList renderableVList, Renderable renderable) {
        if (renderable instanceof AbstractWidget widget)
            widget.setHeight(accessor.getInteger("buttonsHeight", BUTTON_HEIGHT));
    }

    @Override
    protected void panelInit() {
        super.panelInit();
        tooltipBox.init();
        panelRecess.init("panelRecess");
        addRenderableOnly(panelRecess);
        addRenderableOnly(((GuiGraphicsExtractor, i, j, f) -> GuiGraphicsExtractor.text(font, getTitle(), panel.getX() + (panel.getWidth() - font.width(getTitle())) / 2, panelRecess.getY() + 8, CommonColor.GRAY_TEXT.get(), false)));
    }

    @Override
    public void renderableVListInit() {
        for (int i = 0; i < Math.min(4, packs.size()); i++) {
            if (getLocalPreview(category, packs.get(i)) == null) requestImage(packs.get(i).imageUrl());
        }

        getRenderableVList().init("renderableVList", panel.getX() + LIST_X, panel.getY() + LIST_Y, LIST_WIDTH, LIST_HEIGHT);
    }

    @Override
    public void renderDefaultBackground(GuiGraphicsExtractor GuiGraphicsExtractor, int mouseX, int mouseY, float partialTick) {
        LegacyRenderUtil.renderDefaultBackground(UIAccessor.of(this), GuiGraphicsExtractor, false);
        LegacyRenderUtil.renderLogo(GuiGraphicsExtractor);

        tooltipBox.extractRenderState(GuiGraphicsExtractor, mouseX, mouseY, partialTick);
        
        if (hoveredPack != null) {
            ContentManager.Pack displayPack = hoveredPack;
            int x = tooltipBox.getX() + 8;
            int y = tooltipBox.getY() + 8;
            int width = tooltipBox.getWidth() - 16;
            StorePreviewAtlas.Entry localPreview = getLocalPreview(category, displayPack);
            RemoteImage remoteImage = localPreview == null ? getOrDownloadImage(displayPack.imageUrl()) : null;
            int imageAreaHeight = 0;
            int maxImgHeight = (tooltipBox.getHeight() * 4) / 10;

            if (localPreview != null) {
                float scale = Math.min((float) width / localPreview.width(), (float) maxImgHeight / localPreview.height());
                int imgWidth = (int) (localPreview.width() * scale);
                int imgHeight = (int) (localPreview.height() * scale);
                GuiGraphicsExtractor.blit(RenderPipelines.GUI_TEXTURED, localPreview.resource(), x + (width - imgWidth) / 2, y, (float) localPreview.u(), localPreview.v(), imgWidth, imgHeight, localPreview.width(), localPreview.height(), localPreview.atlasWidth(), localPreview.atlasHeight());
                imageAreaHeight = imgHeight + 10;
            } else if (displayPack.imageUrl().isPresent()) {
                if (remoteImage != null && remoteImage.width > 0) {
                    float scale = Math.min((float) width / remoteImage.width, (float) maxImgHeight / remoteImage.height);
                    int imgWidth = (int) (remoteImage.width * scale);
                    int imgHeight = (int) (remoteImage.height * scale);
                    
                    FactoryGuiGraphics.of(GuiGraphicsExtractor).blit(remoteImage.id, x + (width - imgWidth) / 2, y, 0.0f, 0.0f, imgWidth, imgHeight, imgWidth, imgHeight);
                    imageAreaHeight = imgHeight + 10;
                } else if (remoteImage == null) {
                    LegacyRenderUtil.drawGenericLoading(GuiGraphicsExtractor, x + (width - 30) / 2, y + Math.max(0, (maxImgHeight - 30) / 2), 6, 3);
                    imageAreaHeight = maxImgHeight + 10;
                }
            }

            int lineHeight = 12;
            int descriptionY = y + imageAreaHeight;
            int descriptionWidth = width;
            MultiLineLabel label = getDescriptionLabel(displayPack, descriptionWidth);
            
            int visibleLines = (tooltipBox.getY() + tooltipBox.getHeight() - 24 - descriptionY) / lineHeight;
            scrollableRenderer.scrolled.max = Math.max(0, label.getLineCount() - visibleLines);
            scrollableRenderer.lineHeight = lineHeight;

            scrollableRenderer.extractRenderState(GuiGraphicsExtractor, x, descriptionY, descriptionWidth, visibleLines * lineHeight, () -> 
                label.visitLines(net.minecraft.client.gui.TextAlignment.LEFT, x, descriptionY, lineHeight, GuiGraphicsExtractor.textRenderer())
            );
        }
    }

    @Override
    public boolean mouseScrolled(double d, double e, double f, double g) {
        if ((tooltipBox.isHovered(d, e) || !ControlType.getActiveType().isKbm()) && scrollableRenderer.mouseScrolled(g))
            return true;
        return super.mouseScrolled(d, e, f, g);
    }

    private class PackActionScreen extends ConfirmationScreen {
        private final ContentManager.Pack pack;

        public PackActionScreen(Screen parent, ContentManager.Pack pack, ContentManager.Category category) {
            super(parent, ConfirmationScreen::getPanelWidth, () -> 95, pack.nameComponent(),
                  Component.translatable("legacy.menu.delete_message"), 
                  (b) -> {} 
            );
            this.pack = pack;
        }

        @Override
        protected void addButtons() {
            renderableVList.addRenderable(Button.builder(Component.translatable("gui.cancel"), b -> minecraft.setScreen(parent))
                .bounds(panel.x + 15, panel.getRectangle().bottom() - 52, 200, 20).build());

            renderableVList.addRenderable(Button.builder(Component.translatable("legacy.menu.delete"), b -> {
                deletePack(pack);
                minecraft.setScreen(parent);
            }).bounds(panel.x + 15, panel.getRectangle().bottom() - 30, 200, 20).build());
        }
    }

    private static abstract class LeftAlignedButton extends ListButton {
        private final ContentManager.Pack pack;
        private final ContentManager.Category category;
        private final java.util.Set<String> downloadingPacks;
        private final Map<String, Boolean> installedPacks;

        public LeftAlignedButton(RenderableVList list, int width, int height, ContentManager.Pack pack, ContentManager.Category category, java.util.Set<String> downloadingPacks, Map<String, Boolean> installedPacks) {
            super(list, 0, 0, width, height, pack.nameComponent());
            this.pack = pack;
            this.category = category;
            this.downloadingPacks = downloadingPacks;
            this.installedPacks = installedPacks;
        }

        @Override
        protected void extractContents(GuiGraphicsExtractor GuiGraphicsExtractor, int mouseX, int mouseY, float partialTick) {
            GuiGraphicsExtractor.blitSprite(RenderPipelines.GUI_TEXTURED, isHoveredOrFocused() ? LegacyRenderUtil.getSpriteOrFallback(LegacySprites.LIST_BUTTON_HIGHLIGHTED, LegacySprites.BUTTON_HIGHLIGHTED) : LegacyRenderUtil.getSpriteOrFallback(LegacySprites.LIST_BUTTON, LegacySprites.BUTTON), getX(), getY(), getWidth(), getHeight(), ARGB.white(alpha));
            renderString(GuiGraphicsExtractor, Minecraft.getInstance().font, LegacyRenderUtil.getDefaultTextColor(!isHoveredOrFocused()));
            if (downloadingPacks.contains(pack.id()) || ContentManager.isPackDownloading(pack, category)) {
                int size = 16;
                int x = this.getX() + this.width - size - 11;
                int y = this.getY() + (this.height - size) / 2;
                LegacyRenderUtil.drawGenericLoading(GuiGraphicsExtractor, x, y, 4, 2);
            } else if (installedPacks.getOrDefault(pack.id(), false)) {
                int spriteSize = 18;
                int sx = this.getX() + this.width - spriteSize - 7;
                int sy = this.getY() + (this.height - spriteSize) / 2;
                GuiGraphicsExtractor.blitSprite(RenderPipelines.GUI_TEXTURED, LegacySprites.BEACON_CONFIRM, sx, sy, spriteSize, spriteSize);
            }
        }

        public void renderString(GuiGraphicsExtractor GuiGraphicsExtractor, Font font, int color) {
            int textY = this.getY() + (this.getHeight() - font.lineHeight) / 2 + 1;
            int textX = this.getX() + 8;
            boolean hasStatusIcon = downloadingPacks.contains(pack.id()) || ContentManager.isPackDownloading(pack, category) || installedPacks.getOrDefault(pack.id(), false);
            int maxWidth = this.width - (hasStatusIcon ? 44 : 16);
            String clipped = PlayerSkinWidget.clipText(font, getMessage() == null ? "" : getMessage().getString(), Math.max(0, maxWidth));
            GuiGraphicsExtractor.text(font, clipped, textX, textY, color, true);
        }
    }
}
