package wily.legacy.client.screen;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.renderer.texture.DynamicTexture;
import wily.legacy.client.ContentManager;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import wily.factoryapi.base.client.FactoryGuiGraphics;
import wily.factoryapi.base.client.UIAccessor;
import wily.legacy.Legacy4J;
import wily.legacy.Skins.skin.DownloadedSkinPackStore;
import wily.legacy.client.CommonColor;
import wily.legacy.client.ControlType;
import wily.legacy.client.controller.ControllerBinding;
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
    private static final int PANEL_WIDTH = 240;
    private static final int PANEL_HEIGHT = 222;
    private static final int TOOLTIP_WIDTH = 222;
    private static final int LIST_X = 18;
    private static final int LIST_Y = 34;
    private static final int LIST_WIDTH = 204;
    private static final int LIST_HEIGHT = 184;
    private static final int BUTTON_HEIGHT = 26;
    
    protected final ContentManager.Category category;
    protected final List<ContentManager.Pack> packs;
    protected ContentManager.Pack hoveredPack;
    protected final Panel tooltipBox = Panel.tooltipBoxOf(panel, TOOLTIP_WIDTH);
    
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

    private static class RemoteImage {
        public final int width;
        public final int height;
        public final ResourceLocation id;
        public RemoteImage(ResourceLocation id, int width, int height) {
            this.id = id;
            this.width = width;
            this.height = height;
        }
    }

    public Legacy4JContentListScreen(Screen parent, ContentManager.Category category, List<ContentManager.Pack> packs) {
        super(s -> Panel.createPanel(s,
                p -> p.appearance(PANEL_WIDTH, PANEL_HEIGHT), 
                p -> p.pos(p.centeredLeftPos(s), p.centeredTopPos(s) + 17)), 
                Component.empty()
        );
        this.parent = parent;
        this.category = category;
        this.packs = packs;
        packs.forEach(pack -> installedPacks.put(pack.id(), ContentManager.isPackInstalled(pack, category)));
        if (!packs.isEmpty()) {
            hoveredPack = packs.get(0);
            requestImage(hoveredPack.imageUrl());
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
        return descriptionLabels.computeIfAbsent(pack.id(), id -> MultiLineLabel.create(font, Component.literal(pack.description()), width));
    }

    private void selectPack(ContentManager.Pack pack) {
        if (hoveredPack == pack) {
            requestImage(pack.imageUrl());
            return;
        }
        hoveredPack = pack;
        scrollableRenderer.resetScrolled();
        requestImage(pack.imageUrl());
    }

    private void addMenuButton(ContentManager.Pack pack) {
        renderableVList.addRenderable(new LeftAlignedButton(LIST_WIDTH, BUTTON_HEIGHT, pack, category, downloadingPacks, installedPacks, b -> {
            if (downloadingPacks.contains(pack.id())) return;
            selectPack(pack);
            if (isInstalled(pack)) {
                minecraft.setScreen(new PackActionScreen(this, pack, category));
            } else {
                if (!prepareDownloadTarget()) return;
                downloadingPacks.add(pack.id());
                ContentManager.downloadPack(pack, category, () -> {
                    downloadingPacks.remove(pack.id());
                    boolean installed = ContentManager.isPackInstalled(pack, category);
                    installedPacks.put(pack.id(), installed);
                    if (installed) needsReload = true;
                });
            }
        }) {
            @Override
            public void setFocused(boolean focused) {
                super.setFocused(focused);
                if (focused) selectPack(pack);
            }
        }); 
    }

    private boolean prepareDownloadTarget() {
        if (!DownloadedSkinPackStore.managesTargetDirectory(category.targetDirectoryName())) return true;
        try {
            DownloadedSkinPackStore.enableResourcePack(minecraft);
            return true;
        } catch (IOException e) {
            String message = e.getMessage();
            minecraft.setScreen(ConfirmationScreen.createInfoScreen(this, category.title(), Component.literal(message == null || message.isBlank() ? e.toString() : message)));
            return false;
        }
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
                    ResourceLocation textureId = ResourceLocation.fromNamespaceAndPath("legacy", "pack_image_" + cleanId);
                    
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
            if (hoveredPack != null && downloadingPacks.contains(hoveredPack.id())) {
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
    public void renderableVListInit() {
        tooltipBox.init();
        for (int i = 0; i < Math.min(4, packs.size()); i++) {
            requestImage(packs.get(i).imageUrl());
        }
        
        addRenderableOnly((guiGraphics, i, j, f) -> {
            guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, LegacySprites.PANEL_RECESS, 
                panel.getX() + 9, panel.getY() + 9, panel.getWidth() - 18, panel.getHeight() - 17);
            
            Component title = Component.translatable("legacy.menu.store_title");
            float textScale = 0.75f;
            int scaledTextWidth = (int)(font.width(title) * textScale);
            guiGraphics.pose().pushMatrix();
            guiGraphics.pose().translate(panel.getX() + (panel.getWidth() - scaledTextWidth) / 2, panel.getY() + 17);
            guiGraphics.pose().scale(textScale, textScale);
            guiGraphics.drawString(font, title, 0, 0, CommonColor.GRAY_TEXT.get(), false);
            guiGraphics.pose().popMatrix();
        });

        getRenderableVList().scrollArrowYOffset(-19).init("renderableVList", panel.getX() + LIST_X, panel.getY() + LIST_Y, LIST_WIDTH, LIST_HEIGHT);
    }

    @Override
    public void renderDefaultBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        LegacyRenderUtil.renderDefaultBackground(UIAccessor.of(this), guiGraphics, false);
        LegacyRenderUtil.renderLogo(guiGraphics);

        tooltipBox.render(guiGraphics, mouseX, mouseY, partialTick);
        
        if (hoveredPack != null) {
            ContentManager.Pack displayPack = hoveredPack;
            int x = tooltipBox.getX() + 8;
            int y = tooltipBox.getY() + 8;
            int width = tooltipBox.getWidth() - 16;
            RemoteImage remoteImage = getOrDownloadImage(displayPack.imageUrl());
            int imageAreaHeight = 0;
            int maxImgHeight = (tooltipBox.getHeight() * 4) / 10;

            if (displayPack.imageUrl().isPresent()) {
                if (remoteImage != null && remoteImage.width > 0) {
                    float scale = Math.min((float) width / remoteImage.width, (float) maxImgHeight / remoteImage.height);
                    int imgWidth = (int) (remoteImage.width * scale);
                    int imgHeight = (int) (remoteImage.height * scale);
                    
                    FactoryGuiGraphics.of(guiGraphics).blit(remoteImage.id, x + (width - imgWidth) / 2, y, 0.0f, 0.0f, imgWidth, imgHeight, imgWidth, imgHeight);
                    imageAreaHeight = imgHeight + 10;
                } else if (remoteImage == null) {
                    LegacyRenderUtil.drawGenericLoading(guiGraphics, x + (width - 30) / 2, y + Math.max(0, (maxImgHeight - 30) / 2), 6, 3);
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

            scrollableRenderer.render(guiGraphics, x, descriptionY, descriptionWidth, visibleLines * lineHeight, () -> 
                label.render(guiGraphics, MultiLineLabel.Align.LEFT, x, descriptionY, lineHeight, true, 0xFFFFFFFF)
            );
        }

        panel.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseScrolled(double d, double e, double f, double g) {
        boolean isMouseOverTooltip = d >= tooltipBox.getX() && d < tooltipBox.getX() + tooltipBox.getWidth() && 
                                     e >= tooltipBox.getY() && e < tooltipBox.getY() + tooltipBox.getHeight();
        if (isMouseOverTooltip) {
            scrollableRenderer.scrolled.add((int) -Math.signum(g));
            return true;
        }
        return super.mouseScrolled(d, e, f, g);
    }

    private class PackActionScreen extends ConfirmationScreen {
        private final ContentManager.Pack pack;

        public PackActionScreen(Screen parent, ContentManager.Pack pack, ContentManager.Category category) {
            super(parent, ConfirmationScreen::getPanelWidth, () -> 95, Component.literal(pack.name()), 
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

    private static class LeftAlignedButton extends Button {
        private final ContentManager.Pack pack;
        private final ContentManager.Category category;
        private final java.util.Set<String> downloadingPacks;
        private final Map<String, Boolean> installedPacks;

        public LeftAlignedButton(int width, int height, ContentManager.Pack pack, ContentManager.Category category, java.util.Set<String> downloadingPacks, Map<String, Boolean> installedPacks, OnPress onPress) {
            super(0, 0, width, height, Component.literal(pack.name()), onPress, DEFAULT_NARRATION);
            this.pack = pack;
            this.category = category;
            this.downloadingPacks = downloadingPacks;
            this.installedPacks = installedPacks;
        }

        @Override
        public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            super.renderWidget(guiGraphics, mouseX, mouseY, partialTick);
            if (downloadingPacks.contains(pack.id())) {
                int size = 16;
                int x = this.getX() + this.width - size - 11;
                int y = this.getY() + (this.height - size) / 2;
                LegacyRenderUtil.drawGenericLoading(guiGraphics, x, y, 4, 2);
            } else if (installedPacks.getOrDefault(pack.id(), false)) {
                int spriteSize = 18;
                int sx = this.getX() + this.width - spriteSize - 10;
                int sy = this.getY() + (this.height - spriteSize) / 2;
                guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, LegacySprites.BEACON_CONFIRM, sx, sy, spriteSize, spriteSize);
            }
        }

        @Override
        public void renderString(GuiGraphics guiGraphics, Font font, int color) {
            int textY = this.getY() + (this.getHeight() - font.lineHeight) / 2 + 1;
            int textX = this.getX() + 12;
            int maxWidth = this.width - 44;
            String text = this.getMessage().getString();
            String clipped = font.width(text) <= maxWidth ? text : font.plainSubstrByWidth(text, Math.max(0, maxWidth - font.width("..."))) + "...";
            guiGraphics.drawString(font, clipped, textX, textY, color, true);
        }
    }
}
