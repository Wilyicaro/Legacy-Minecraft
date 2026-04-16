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
import wily.legacy.client.DownloadedPackMetadata;
import wily.legacy.util.LegacySprites;
import wily.legacy.util.client.LegacyRenderUtil;

import java.io.File;
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
    private boolean needsReload = false;
    private ContentManager.Pack armedPack;

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
        
        renderableVList.layoutSpacing(l -> 0);
        for (ContentManager.Pack pack : packs) {
            addMenuButton(pack);
        }
    }

    private void addMenuButton(ContentManager.Pack pack) {
        renderableVList.addRenderable(new LeftAlignedButton(LIST_WIDTH, BUTTON_HEIGHT, pack, category, b -> {
            if (armedPack != pack) {
                armedPack = pack;
                if (hoveredPack != pack) scrollableRenderer.resetScrolled();
                hoveredPack = pack;
                return;
            }
            if (ContentManager.isPackInstalled(pack, category.targetDirectoryName())) {
                minecraft.setScreen(new PackActionScreen(this, pack, category));
            } else {
                armedPack = null;
                if (!prepareDownloadTarget()) return;
                ContentManager.downloadPack(pack, category.targetDirectoryName(), () -> needsReload = true);
            }
        }) {
            @Override
            public void setFocused(boolean focused) {
                super.setFocused(focused);
                if (focused) {
                    if (hoveredPack != pack) armedPack = null;
                    if (hoveredPack != pack) scrollableRenderer.resetScrolled();
                    hoveredPack = pack;
                }
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
        File packDir = new File(minecraft.gameDirectory, category.targetDirectoryName() + "/" + pack.id());
        if (packDir.exists()) {
            deleteDirectoryRecursively(packDir);
        }
        armedPack = null;
        DownloadedPackMetadata.clear(pack.id());
        needsReload = true;
    }

    private void deleteDirectoryRecursively(File directory) {
        File[] allContents = directory.listFiles();
        if (allContents != null) {
            for (File file : allContents) {
                deleteDirectoryRecursively(file);
            }
        }
        directory.delete();
    }

    public static void warmImages(List<ContentManager.Pack> packs) {
        packs.forEach(pack -> getOrDownloadImage(pack.imageUrl()));
    }

    private static RemoteImage getOrDownloadImage(Optional<URI> url) {
        if (url.isEmpty()) return null;
        String key = url.get().toString();
        RemoteImage image = downloadedImages.get(key);
        if (image != null) return image;
        if (downloadingImages.contains(key)) return null;
        downloadingImages.add(key);
        CompletableFuture.runAsync(() -> {
            Minecraft client = Minecraft.getInstance();
            try (InputStream in = url.get().toURL().openStream()) {
                NativeImage nativeImage = NativeImage.read(in);
                int nativeWidth = nativeImage.getWidth();
                int nativeHeight = nativeImage.getHeight();
                
                client.execute(() -> {
                    String cleanId = Integer.toHexString(key.hashCode());
                    ResourceLocation textureId = ResourceLocation.fromNamespaceAndPath("legacy", "pack_image_" + cleanId);
                    
                    client.getTextureManager().register(textureId, new DynamicTexture(() -> "pack_image_" + cleanId, nativeImage));
                    
                    downloadedImages.put(key, new RemoteImage(textureId, nativeWidth, nativeHeight));
                });
            } catch (Exception e) {
                downloadedImages.put(key, new RemoteImage(null, 0, 0));
                Legacy4J.LOGGER.warn("Failed to load content image {}", key, e);
            } finally {
                client.execute(() -> downloadingImages.remove(key));
            }
        });
        return null;
    }

    @Override
    public void addControlTooltips(ControlTooltip.Renderer renderer) {
        boolean isKbm = ControlType.getActiveType().isKbm();

        // Contextual Install/Delete Select
        renderer.add(() -> isKbm ? ControlTooltip.getKeyIcon(InputConstants.KEY_RETURN) : ControllerBinding.DOWN_BUTTON.bindingState.getIcon(), () -> {
            if (hoveredPack != null && ContentManager.isPackInstalled(hoveredPack, category.targetDirectoryName())) {
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
        warmImages(packs);
        
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
            int x = tooltipBox.getX() + 8;
            int y = tooltipBox.getY() + 8;
            int width = tooltipBox.getWidth() - 16;
            int height = tooltipBox.getHeight() - 16;
            
            // 1. Render Icon/Image
            RemoteImage remoteImage = getOrDownloadImage(hoveredPack.imageUrl());
            int imageAreaHeight = 0;
            
            if (remoteImage != null && remoteImage.width > 0) {
                int maxImgHeight = (tooltipBox.getHeight() * 4) / 10;
                float scale = Math.min((float) width / remoteImage.width, (float) maxImgHeight / remoteImage.height);
                int imgWidth = (int) (remoteImage.width * scale);
                int imgHeight = (int) (remoteImage.height * scale);
                
                FactoryGuiGraphics.of(guiGraphics).blit(remoteImage.id, x + (width - imgWidth) / 2, y, 0.0f, 0.0f, imgWidth, imgHeight, imgWidth, imgHeight);
                imageAreaHeight = imgHeight + 10;
            }

            // 2. Render Description with ScrollableRenderer
            int lineHeight = 12;
            int descriptionY = y + imageAreaHeight;
            int descriptionWidth = width;
            MultiLineLabel label = MultiLineLabel.create(font, Component.literal(hoveredPack.description()), descriptionWidth);
            
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

        public LeftAlignedButton(int width, int height, ContentManager.Pack pack, ContentManager.Category category, OnPress onPress) {
            super(0, 0, width, height, Component.literal(pack.name()), onPress, DEFAULT_NARRATION);
            this.pack = pack;
            this.category = category;
        }

        @Override
        public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            super.renderWidget(guiGraphics, mouseX, mouseY, partialTick);
            if (ContentManager.isPackInstalled(pack, category.targetDirectoryName())) {
                int spriteSize = 18;
                int sx = this.getX() + this.width - spriteSize - 10;
                int sy = this.getY() + (this.height - spriteSize) / 2;
                guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, LegacySprites.BEACON_CONFIRM, sx, sy, spriteSize, spriteSize);
            }
        }

        @Override
        public void renderString(GuiGraphics guiGraphics, Font font, int color) {
            int textY = this.getY() + (this.getHeight() - font.lineHeight) / 2 + 1;
            guiGraphics.drawString(font, this.getMessage(), this.getX() + 12, textY, color, true);
        }
    }
}
