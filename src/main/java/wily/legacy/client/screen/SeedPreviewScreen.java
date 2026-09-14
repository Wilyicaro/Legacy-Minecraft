package wily.legacy.client.screen;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.WorldCreationContext;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import wily.factoryapi.base.client.FactoryGuiGraphics;
import wily.legacy.Legacy4J;
import wily.legacy.client.seedpreview.SeedMap;
import wily.legacy.client.seedpreview.SeedMapTexture;
import wily.legacy.util.LegacySprites;
import wily.legacy.util.client.LegacyRenderUtil;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

public class SeedPreviewScreen extends LegacyScreen {
    private final WorldCreationContext settings;
    private CompletableFuture<Void> generation;
    private AtomicBoolean cancelled;
    private SeedMapTexture texture;
    private float scale;
    private int left;
    private int top;

    public SeedPreviewScreen(Screen parent, WorldCreationContext settings) {
        super(parent, Component.translatable("legacy.menu.seed_preview"));
        this.settings = settings;
    }

    @Override
    protected void init() {
        super.init();
        scale = Math.min(1, Math.min((width - 24) / 371f, (height - 48) / 265f));
        left = (width - scaled(371)) / 2;
        top = (height - scaled(265)) / 2 - scaled(17);
        Panel overview = addPanel("overviewPanel", LegacySprites.PANEL, 0, 0, 131, 141);
        Panel detail = addPanel("detailPanel", LegacySprites.PANEL, 136, 0, 234, 262);
        addPanel("helpPanel", LegacySprites.POINTER_PANEL, 0, 144, 133, 120);
        addRenderableOnly((graphics, mouseX, mouseY, partialTick) -> {
            renderMap(graphics, overview, 10, 20, 109);
            renderMap(graphics, detail, 12, 27, 211);
            if (generation != null && !generation.isDone()) {
                int blockSize = scaled(21);
                int blockDistance = scaled(6);
                int size = blockSize * 3 + blockDistance * 2;
                LegacyRenderUtil.drawGenericLoading(graphics, detail.getX() + (detail.getWidth() - size) / 2,
                        detail.getY() + (detail.getHeight() - size) / 2, blockSize, blockDistance);
            }
        });
        if (generation == null) startGeneration();
    }

    private void startGeneration() {
        AtomicBoolean cancelled = new AtomicBoolean();
        this.cancelled = cancelled;
        generation = CompletableFuture.supplyAsync(() -> SeedMap.generate(settings, cancelled::get), Util.backgroundExecutor()).thenAcceptAsync(map -> {
            if (!cancelled.get()) texture = new SeedMapTexture(map);
        }, minecraft).whenComplete((unused, error) -> {
            if (error != null && !cancelled.get()) {
                Legacy4J.LOGGER.warn("Failed to generate seed preview", error);
            }
        });
    }

    @Override
    public void removed() {
        super.removed();
        if (generation != null) {
            cancelled.set(true);
            generation = null;
        }
        if (texture != null) {
            texture.close();
            texture = null;
        }
    }

    private void renderMap(GuiGraphicsExtractor graphics, Panel panel, int x, int y, int size) {
        int mapX = panel.getX() + scaled(x);
        int mapY = panel.getY() + scaled(y);
        int mapSize = scaled(size);
        FactoryGuiGraphics.of(graphics).blitSprite(LegacySprites.SQUARE_RECESSED_PANEL, mapX - 2, mapY - 2, mapSize + 4, mapSize + 4);
        if (texture != null) {
            graphics.blit(texture.getTextureView(), texture.getSampler(), mapX, mapY, mapX + mapSize, mapY + mapSize, 0, 1, 0, 1);
        }
    }

    private int scaled(int value) {
        return Math.round(value * scale);
    }

    private Panel addPanel(String name, Identifier sprite, int x, int y, int width, int height) {
        Panel panel = new Panel(this);
        panel.init(name);
        panel.appearance(sprite, scaled(width), scaled(height));
        panel.pos(left + scaled(x), top + scaled(y));
        return addRenderableOnly(panel);
    }
}
