package wily.legacy.client.screen;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.client.renderer.texture.DynamicTexture;
import wily.factoryapi.base.client.AdvancedTextWidget;
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
import wily.legacy.client.CommonColor;
import wily.legacy.client.ControlType;
import wily.legacy.client.controller.ControllerBinding;
import wily.legacy.util.LegacySprites;
import wily.legacy.util.client.LegacyRenderUtil;

import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class Legacy4JContentListScreen extends PanelVListScreen implements ControlTooltip.Event {
    
    protected final ContentManager.Category category;
    protected final List<ContentManager.Pack> packs;
    protected ContentManager.Pack hoveredPack;
    protected final Panel tooltipBox = Panel.tooltipBoxOf(panel, 273);
    
    // Legacy Scrolling System
    protected final LegacyScrollRenderer scrollRenderer = new LegacyScrollRenderer();
    public final ScrollableRenderer scrollableRenderer = new ScrollableRenderer(scrollRenderer);
    
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

    private final Map<String, RemoteImage> downloadedImages = new HashMap<>();
    private final Set<String> downloadingImages = new HashSet<>();

    public Legacy4JContentListScreen(Screen parent, ContentManager.Category category, List<ContentManager.Pack> packs) {
        super(s -> Panel.createPanel(s,
                p -> p.appearance(294, 274), 
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
        renderableVList.addRenderable(new LeftAlignedButton(254, 36, pack, category) {
            @Override
            public void onPress(InputWithModifiers inputWithModifiers) {
                if (ContentManager.isPackInstalled(pack, category.targetDirectoryName())) {
                    minecraft.setScreen(new PackActionScreen(Legacy4JContentListScreen.this, pack, category));
                } else {
                    ContentManager.downloadPack(pack, category.targetDirectoryName(), () -> needsReload = true);
                }
            }

            @Override
            public void setFocused(boolean focused) {
                super.setFocused(focused);
                if (focused) {
                    if (hoveredPack != pack) scrollableRenderer.resetScrolled();
                    hoveredPack = pack;
                }
            }
        }); 
    }

    @Override
    public void onClose() {
        if (this.needsReload && this.category.requiresResourceReload()) {
            minecraft.reloadResourcePacks();
        }
        
        downloadedImages.forEach((id, img) -> {
            if (img != null && img.id != null) {
                minecraft.getTextureManager().release(img.id);
            }
        });
        downloadedImages.clear();
        downloadingImages.clear();
        super.onClose();
    }

    private void deletePack(ContentManager.Pack pack) {
        File packDir = new File(minecraft.gameDirectory, category.targetDirectoryName() + "/" + pack.id());
        if (packDir.exists()) {
            deleteDirectoryRecursively(packDir);
        }
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

    private RemoteImage getOrDownloadImage(Optional<URI> url, String packId) {
        if (url.isEmpty()) return null;
        if (downloadedImages.containsKey(packId)) return downloadedImages.get(packId); 
        if (downloadingImages.contains(packId)) return null;
        
        downloadingImages.add(packId);
        CompletableFuture.runAsync(() -> {
            try (InputStream in = url.get().toURL().openStream()) {
                NativeImage nativeImage = NativeImage.read(in);
                int nativeWidth = nativeImage.getWidth();
                int nativeHeight = nativeImage.getHeight();
                
                minecraft.execute(() -> {
                    String cleanId = packId.toLowerCase().replaceAll("[^a-z0-9_.-]", "");
                    ResourceLocation textureId = ResourceLocation.fromNamespaceAndPath("legacy", "pack_image_" + cleanId);
                    
                    minecraft.getTextureManager().register(textureId, new DynamicTexture(() -> "pack_image_" + cleanId, nativeImage));
                    
                    downloadedImages.put(packId, new RemoteImage(textureId, nativeWidth, nativeHeight));
                    downloadingImages.remove(packId);
                });
            } catch (Exception e) {
                minecraft.execute(() -> downloadingImages.remove(packId));
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
            widget.setHeight(accessor.getInteger("buttonsHeight", 36));
    }

    @Override
    public void renderableVListInit() {
        tooltipBox.init();
        
        addRenderableOnly((guiGraphics, i, j, f) -> {
            guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, LegacySprites.PANEL_RECESS, 
                panel.getX() + 9, panel.getY() + 9, panel.getWidth() - 18, panel.getHeight() - 17);
            
            Component title = Component.translatable("legacy.menu.store_title");
            float textScale = 1.5f;
            int scaledTextWidth = (int)(font.width(title) * textScale);
            guiGraphics.pose().pushMatrix();
            guiGraphics.pose().translate(panel.getX() + (panel.getWidth() - scaledTextWidth) / 2, panel.getY() + 17);
            guiGraphics.pose().scale(textScale, textScale);
            guiGraphics.drawString(font, title, 0, 0, CommonColor.GRAY_TEXT.get(), false);
            guiGraphics.pose().popMatrix();
        });

        getRenderableVList().init("renderableVList", panel.getX() + 20, panel.getY() + 32, 254, 225);
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
            RemoteImage remoteImage = getOrDownloadImage(hoveredPack.imageUrl(), hoveredPack.id());
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
            AdvancedTextWidget label = Panel.getUILabel(hoveredPack.description(), width);
            
            int visibleLines = (tooltipBox.getY() + tooltipBox.getHeight() - 24 - descriptionY) / lineHeight;
            scrollableRenderer.scrolled.max = Math.max(0, label.getLines().size() - visibleLines);
            scrollableRenderer.lineHeight = lineHeight;

            scrollableRenderer.render(guiGraphics, x, descriptionY, width, visibleLines * lineHeight, () ->
                label.withPos(x, descriptionY).lineSpacing(lineHeight).render(guiGraphics, 0, 0, 0)
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
            super(parent, ConfirmationScreen::getPanelWidth, () -> 95, (pack.name()),
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

    private abstract static class LeftAlignedButton extends ListButton {
        private final ContentManager.Pack pack;
        private final ContentManager.Category category;

        public LeftAlignedButton(int width, int height, ContentManager.Pack pack, ContentManager.Category category) {
            super(0, 0, width, height, pack.name());
            this.pack = pack;
            this.category = category;
        }

        @Override
        public void renderButton(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            super.renderButton(guiGraphics, mouseX, mouseY, partialTick);
            if (ContentManager.isPackInstalled(pack, category.targetDirectoryName())) {
                int spriteSize = 20;
                int sx = this.getX() + this.width - spriteSize - 10;
                int sy = this.getY() + (this.height - spriteSize) / 2;
                guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, LegacySprites.BEACON_CONFIRM, sx, sy, spriteSize, spriteSize);
            }
        }

        @Override
        public void renderScrollingString(GuiGraphics guiGraphics, Font font, int offset, int color) {
            int textY = this.getY() + (this.getHeight() - font.lineHeight) / 2 + 1;
            guiGraphics.drawString(font, this.getMessage(), this.getX() + 12, textY, color, true);
        }
    }
}