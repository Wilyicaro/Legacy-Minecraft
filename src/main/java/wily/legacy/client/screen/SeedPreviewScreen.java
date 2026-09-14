package wily.legacy.client.screen;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenDirection;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.WorldCreationContext;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.level.ChunkPos;
import wily.factoryapi.base.client.FactoryGuiGraphics;
import wily.legacy.Legacy4J;
import wily.legacy.client.control.BindingState;
import wily.legacy.client.control.ControlType;
import wily.legacy.client.control.ControllerBinding;
import wily.legacy.client.seedpreview.SeedMap;
import wily.legacy.client.seedpreview.SeedMapTexture;
import wily.legacy.util.LegacySprites;
import wily.legacy.util.client.LegacyRenderUtil;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

public class SeedPreviewScreen extends LegacyScreen {
    private static final int PAN_STEP = 4;
    private static final double PAN_SPEED = 16;
    private static final int REFRESH_STEP = SeedMap.PADDING / 2;
    private static final Arrow[] ARROWS = {
            new Arrow(ScreenDirection.UP, 111, 28, 13, 7),
            new Arrow(ScreenDirection.DOWN, 111, 230, 13, 7),
            new Arrow(ScreenDirection.LEFT, 13, 127, 6, 11),
            new Arrow(ScreenDirection.RIGHT, 216, 127, 6, 11)
    };
    private final WorldCreationContext settings;
    private final LegacyScrollRenderer scrollRenderer = new LegacyScrollRenderer();
    private CompletableFuture<Void> generation;
    private AtomicBoolean cancelled;
    private ChunkPos requested;
    private SeedMapTexture texture;
    private SeedMapTexture overviewTexture;
    private Panel detail;
    private double targetX;
    private double targetZ;
    private double viewX;
    private double viewZ;
    private float scale;
    private int left;
    private int top;

    public SeedPreviewScreen(Screen parent, WorldCreationContext settings) {
        super(parent, Component.translatable("legacy.menu.seed_preview"));
        this.settings = settings;
    }

    @Override
    public void renderDefaultBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        LegacyRenderUtil.renderDefaultBackground(accessor, graphics, false);
    }

    @Override
    protected void init() {
        super.init();
        setDragging(false);
        scale = Math.min(1, Math.min((width - 24) / 371f, (height - 48) / 265f));
        left = (width - scaled(371)) / 2;
        top = (height - scaled(265)) / 2 - scaled(17);
        Panel overview = addPanel("overviewPanel", LegacySprites.PANEL, 0, 0, 131, 141);
        detail = addPanel("detailPanel", LegacySprites.PANEL, 136, 0, 234, 262);
        addPanel("helpPanel", LegacySprites.POINTER_PANEL, 0, 144, 133, 120);
        addRenderableOnly((graphics, mouseX, mouseY, partialTick) -> {
            updateView();
            renderMap(graphics, overview, 10, 20, 109, overviewTexture, 0, 0);
            renderMap(graphics, detail, 12, 27, 211, texture, viewX, viewZ);
            for (Arrow arrow : ARROWS) {
                ScreenRectangle bounds = arrowBounds(arrow);
                scrollRenderer.renderScroll(graphics, arrow.direction, bounds.left(), bounds.top(),
                        LegacyScrollRenderer.SCROLLS[arrow.direction.ordinal()], bounds.width(), bounds.height());
            }
            if (texture == null && generation != null && !generation.isDone()) {
                int blockSize = scaled(21);
                int blockDistance = scaled(6);
                int size = blockSize * 3 + blockDistance * 2;
                LegacyRenderUtil.drawGenericLoading(graphics, detail.getX() + (detail.getWidth() - size) / 2,
                        detail.getY() + (detail.getHeight() - size) / 2, blockSize, blockDistance);
            }
        });
        updateMap();
    }

    private void updateMap() {
        if (generation != null && !generation.isDone()) return;
        int x = (int) Math.round(targetX / REFRESH_STEP) * REFRESH_STEP;
        int z = (int) Math.round(targetZ / REFRESH_STEP) * REFRESH_STEP;
        ChunkPos center = new ChunkPos(Mth.clamp(x, -SeedMap.MAX_SAMPLE_CENTER, SeedMap.MAX_SAMPLE_CENTER),
                Mth.clamp(z, -SeedMap.MAX_SAMPLE_CENTER, SeedMap.MAX_SAMPLE_CENTER));
        if (center.equals(requested)) return;
        requested = center;
        AtomicBoolean cancelled = new AtomicBoolean();
        this.cancelled = cancelled;
        SeedMap previous = texture == null ? null : texture.map;
        generation = CompletableFuture.supplyAsync(() -> SeedMap.generate(settings, center.x(), center.z(), previous, cancelled::get), Util.backgroundExecutor()).thenAcceptAsync(map -> {
            if (cancelled.get()) return;
            SeedMapTexture next = new SeedMapTexture(map);
            if (texture != null && texture != overviewTexture) texture.close();
            texture = next;
            if (overviewTexture == null) overviewTexture = next;
        }, minecraft).whenComplete((unused, error) -> {
            if (error != null && !cancelled.get()) {
                Legacy4J.LOGGER.warn("Failed to generate seed preview", error);
            }
        });
    }

    @Override
    public void tick() {
        super.tick();
        updateMap();
    }

    private void updateView() {
        double elapsed = Math.min(minecraft.getDeltaTracker().getRealtimeDeltaTicks() / 20.0, 0.1);
        double dx = targetX - viewX;
        double dz = targetZ - viewZ;
        double distance = Math.hypot(dx, dz);
        if (distance < 0.01) {
            viewX = targetX;
            viewZ = targetZ;
            return;
        }
        double step = Math.min(distance * (1 - Math.exp(-12 * elapsed)), PAN_SPEED * elapsed);
        viewX += dx * step / distance;
        viewZ += dz * step / distance;
    }

    @Override
    public void removed() {
        super.removed();
        if (cancelled != null) cancelled.set(true);
        generation = null;
        if (texture != null && texture != overviewTexture) texture.close();
        if (overviewTexture != null) overviewTexture.close();
        texture = null;
        overviewTexture = null;
        requested = null;
        targetX = 0;
        targetZ = 0;
        viewX = 0;
        viewZ = 0;
        setDragging(false);
    }

    private void renderMap(GuiGraphicsExtractor graphics, Panel panel, int x, int y, int size, SeedMapTexture texture, double viewX, double viewZ) {
        int mapX = panel.getX() + scaled(x);
        int mapY = panel.getY() + scaled(y);
        int mapSize = scaled(size);
        FactoryGuiGraphics.of(graphics).blitSprite(LegacySprites.SQUARE_RECESSED_PANEL, mapX - 2, mapY - 2, mapSize + 4, mapSize + 4);
        if (texture != null) {
            graphics.enableScissor(mapX, mapY, mapX + mapSize, mapY + mapSize);
            graphics.pose().pushMatrix();
            graphics.pose().translate(mapX, mapY);
            graphics.pose().scale(mapSize / (float) SeedMap.VIEW_SIZE);
            graphics.pose().translate((float) (texture.map.chunkX() - viewX - SeedMap.PADDING), (float) (texture.map.chunkZ() - viewZ - SeedMap.PADDING));
            graphics.blit(texture.getTextureView(), texture.getSampler(), 0, 0, SeedMap.SIZE, SeedMap.SIZE, 0, 1, 0, 1);
            graphics.pose().popMatrix();
            graphics.disableScissor();
        }
    }

    private void pan(ScreenDirection direction) {
        switch (direction) {
            case UP -> panBy(0, -PAN_STEP);
            case DOWN -> panBy(0, PAN_STEP);
            case LEFT -> panBy(-PAN_STEP, 0);
            case RIGHT -> panBy(PAN_STEP, 0);
        }
    }

    private void panBy(double x, double z) {
        panTo(Mth.clamp(targetX + x, viewX - PAN_STEP, viewX + PAN_STEP),
                Mth.clamp(targetZ + z, viewZ - PAN_STEP, viewZ + PAN_STEP));
    }

    private void jumpTo(double x, double z) {
        panTo(x, z);
        viewX = targetX;
        viewZ = targetZ;
    }

    private void panTo(double x, double z) {
        if (texture == null) return;
        x = Mth.clamp(x, -SeedMap.MAX_CENTER, SeedMap.MAX_CENTER);
        z = Mth.clamp(z, -SeedMap.MAX_CENTER, SeedMap.MAX_CENTER);
        if (x != targetX) scrollRenderer.updateScroll(x > targetX ? ScreenDirection.RIGHT : ScreenDirection.LEFT);
        if (z != targetZ) scrollRenderer.updateScroll(z > targetZ ? ScreenDirection.DOWN : ScreenDirection.UP);
        targetX = x;
        targetZ = z;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        switch (event.key()) {
            case InputConstants.KEY_UP -> pan(ScreenDirection.UP);
            case InputConstants.KEY_DOWN -> pan(ScreenDirection.DOWN);
            case InputConstants.KEY_LEFT -> pan(ScreenDirection.LEFT);
            case InputConstants.KEY_RIGHT -> pan(ScreenDirection.RIGHT);
            case InputConstants.KEY_HOME -> jumpTo(0, 0);
            default -> {
                return super.keyPressed(event);
            }
        }
        return true;
    }

    @Override
    public void bindingStateTick(BindingState state) {
        if (!state.canClick()) return;
        if (state.is(ControllerBinding.RIGHT_STICK_BUTTON)) {
            jumpTo(0, 0);
        } else if (state.is(ControllerBinding.RIGHT_STICK) && state instanceof BindingState.Axis stick && state.pressed
                && Math.abs(stick.x) >= Math.abs(stick.y)) {
            pan(stick.x > 0 ? ScreenDirection.RIGHT : ScreenDirection.LEFT);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() != 0 || texture == null || !mapBounds().containsPoint((int) event.x(), (int) event.y())) {
            return super.mouseClicked(event, doubleClick);
        }
        for (Arrow arrow : ARROWS) {
            ScreenRectangle bounds = arrowBounds(arrow);
            if (LegacyRenderUtil.isMouseOver(event.x(), event.y(), bounds.left() - 2, bounds.top() - 2, bounds.width() + 4, bounds.height() + 4)) {
                pan(arrow.direction);
                return true;
            }
        }
        setFocused(null);
        setDragging(true);
        jumpTo(viewX, viewZ);
        return true;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (!isDragging() || getFocused() != null || event.button() != 0) return super.mouseDragged(event, dx, dy);
        double chunksPerPixel = (double) SeedMap.VIEW_SIZE / mapBounds().width();
        jumpTo(viewX - dx * chunksPerPixel, viewZ - dy * chunksPerPixel);
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (texture == null || ControlType.getActiveType().isKbm() && !mapBounds().containsPoint((int) mouseX, (int) mouseY)) {
            return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        }
        int x = (int) Math.signum(scrollX);
        int z = -(int) Math.signum(scrollY);
        if (minecraft.hasShiftDown()) {
            x = z;
            z = 0;
        }
        panBy(x * PAN_STEP, z * PAN_STEP);
        return true;
    }

    private ScreenRectangle mapBounds() {
        return new ScreenRectangle(detail.getX() + scaled(12), detail.getY() + scaled(27), scaled(211), scaled(211));
    }

    private ScreenRectangle arrowBounds(Arrow arrow) {
        return new ScreenRectangle(detail.getX() + scaled(arrow.x), detail.getY() + scaled(arrow.y), scaled(arrow.width), scaled(arrow.height));
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

    private record Arrow(ScreenDirection direction, int x, int y, int width, int height) {
    }
}
