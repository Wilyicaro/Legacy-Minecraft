package wily.legacy.client.screen;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.TextAlignment;
import net.minecraft.client.gui.components.MultiLineLabel;
import net.minecraft.client.gui.navigation.ScreenDirection;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.WorldCreationContext;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import wily.factoryapi.base.Bearer;
import wily.factoryapi.base.client.FactoryGuiGraphics;
import wily.legacy.Legacy4J;
import wily.legacy.client.CommonColor;
import wily.legacy.client.LegacyTipManager;
import wily.legacy.client.control.BindingState;
import wily.legacy.client.control.ControlType;
import wily.legacy.client.control.ControllerBinding;
import wily.legacy.client.control.tooltip.ControlTooltip;
import wily.legacy.client.control.tooltip.ControlTooltipList;
import wily.legacy.client.seedpreview.SeedMap;
import wily.legacy.client.seedpreview.SeedMapGenerator;
import wily.legacy.client.seedpreview.SeedMapMarker;
import wily.legacy.client.seedpreview.SeedMapTexture;
import wily.legacy.util.LegacySprites;
import wily.legacy.util.client.LegacyRenderUtil;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

public class SeedPreviewScreen extends LegacyScreen {
    private static final Component WORLD_CENTER = Component.translatable("legacy.menu.seed_preview.world_center");
    private static final Component WORLD_MAP = Component.translatable("legacy.menu.seed_preview.world_map");
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
    private final Bearer<BlockPos> seedStart;
    private SeedMapMarker startMarker;
    private BlockPos hovered;
    private boolean showStructures = true;
    private final LegacyScrollRenderer scrollRenderer = new LegacyScrollRenderer();
    private final ScrollableRenderer helpScroll = new ScrollableRenderer();
    private CompletableFuture<SeedMapGenerator> generator;
    private CompletableFuture<Void> generation;
    private AtomicBoolean cancelled;
    private ChunkPos requested;
    private SeedMapTexture texture;
    private SeedMapTexture overviewTexture;
    private Panel detail;
    private Panel help;
    private Component helpText;
    private MultiLineLabel helpLabel;
    private double targetX;
    private double targetZ;
    private double viewX;
    private double viewZ;
    private float scale;
    private int left;
    private int top;

    public SeedPreviewScreen(Screen parent, WorldCreationContext settings, Bearer<BlockPos> seedStart) {
        super(parent, Component.translatable("legacy.menu.seed_preview"));
        this.settings = settings;
        this.seedStart = seedStart;
        setSeedStart(seedStart.get());
    }

    @Override
    public void addControlTooltips(ControlTooltipList list) {
        super.addControlTooltips(list);
        list.add(ControlTooltip.EXTRA::get, () -> hovered != null && !hovered.equals(seedStart.get())
                ? Component.translatable("legacy.menu.seed_preview.set_start") : null);
        list.add(ControlTooltip.OPTION::get, () -> seedStart.get() != null
                ? Component.translatable("legacy.menu.seed_preview.reset_start") : null);
        list.add(() -> ControlType.getActiveType().isKbm() ? ControlTooltip.getKeyIcon(InputConstants.KEY_HOME)
                        : ControllerBinding.RIGHT_STICK_BUTTON.getIcon(),
                () -> texture != null ? Component.translatable("legacy.menu.seed_preview.recenter") : null);
        list.add(() -> ControlType.getActiveType().isKbm() ? ControlTooltip.getKeyIcon(InputConstants.KEY_S)
                        : ControllerBinding.LEFT_STICK_BUTTON.getIcon(),
                () -> settings.options().generateStructures() ? Component.translatable(showStructures
                        ? "legacy.menu.seed_preview.hide_structures" : "legacy.menu.seed_preview.show_structures") : null);
    }

    private void setSeedStart(BlockPos pos) {
        seedStart.set(pos);
        startMarker = pos == null ? null : SeedMapMarker.seedStart(pos);
    }

    private SeedMapMarker previewMarker(SeedMapMarker marker) {
        return startMarker != null && marker.isSpawn() ? startMarker : marker;
    }

    private boolean isVisible(SeedMapMarker marker, boolean overview) {
        return marker.isSpawn() || !overview && showStructures;
    }

    private void toggleStructures() {
        showStructures = !showStructures;
        helpText = null;
        helpLabel = null;
        helpScroll.resetScrolled();
    }

    @Override
    public boolean disableCursorOnInit() {
        return false;
    }

    @Override
    public void renderDefaultBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        LegacyRenderUtil.renderDefaultBackground(accessor, graphics, false);
    }

    @Override
    protected void init() {
        super.init();
        setDragging(false);
        helpLabel = null;
        scale = Math.min(1, Math.min((width - 24) / 371f, (height - 48) / 265f));
        left = (width - scaled(371)) / 2;
        top = (height - scaled(265)) / 2 - scaled(17);
        Panel overview = addPanel("overviewPanel", LegacySprites.PANEL, 0, 0, 131, 141);
        detail = addPanel("detailPanel", LegacySprites.PANEL, 136, 0, 234, 262);
        help = addPanel("helpPanel", LegacySprites.POINTER_PANEL, 0, 144, 133, 120);
        addRenderableOnly((graphics, mouseX, mouseY, partialTick) -> {
            updateView();
            renderLabel(graphics, overview, WORLD_CENTER, 7);
            renderLabel(graphics, detail, WORLD_MAP, 7);
            ScreenRectangle overviewBounds = mapBounds(overview, 10, 20, 109);
            ScreenRectangle detailBounds = mapBounds();
            hovered = texture != null && detailBounds.containsPoint(mouseX, mouseY)
                    ? mapChunk(detailBounds, viewX, viewZ, SeedMap.VIEW_SIZE, mouseX, mouseY).getWorldPosition().offset(8, 0, 8) : null;
            renderMap(graphics, overviewBounds, overviewTexture, 0, 0, true);
            renderGuide(graphics, overviewBounds);
            renderMap(graphics, detailBounds, texture, viewX, viewZ, false);
            updateHelp(overviewBounds, overviewTexture, 0, 0, true, mouseX, mouseY);
            updateHelp(detailBounds, texture, viewX, viewZ, false, mouseX, mouseY);
            renderHelp(graphics);
            Component coordinates = Component.translatable("legacy.menu.seed_preview.coordinates",
                    Mth.floor(viewX * SeedMap.BLOCKS_PER_PIXEL), Mth.floor(viewZ * SeedMap.BLOCKS_PER_PIXEL));
            renderLabel(graphics, detail, coordinates, 244);
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
        if (generator == null) {
            generator = CompletableFuture.supplyAsync(() -> new SeedMapGenerator(settings), Util.backgroundExecutor());
        }
        generation = generator.thenApplyAsync(source -> source.generate(center.x(), center.z(), previous, cancelled::get), Util.backgroundExecutor()).thenAcceptAsync(map -> {
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
        generator = null;
        generation = null;
        if (texture != null && texture != overviewTexture) texture.close();
        if (overviewTexture != null) overviewTexture.close();
        texture = null;
        overviewTexture = null;
        requested = null;
        helpText = null;
        helpLabel = null;
        helpScroll.resetScrolled();
        hovered = null;
        targetX = 0;
        targetZ = 0;
        viewX = 0;
        viewZ = 0;
        setDragging(false);
    }

    private void renderLabel(GuiGraphicsExtractor graphics, Panel panel, Component text, int y) {
        graphics.pose().pushMatrix();
        graphics.pose().translate(panel.getX() + panel.getWidth() / 2f, panel.getY() + scaled(y));
        graphics.pose().scale(scale);
        graphics.text(font, text, -font.width(text) / 2, 0, CommonColor.GRAY_TEXT.get(), false);
        graphics.pose().popMatrix();
    }

    private void renderMap(GuiGraphicsExtractor graphics, ScreenRectangle bounds, SeedMapTexture texture, double viewX, double viewZ, boolean overview) {
        int mapX = bounds.left();
        int mapY = bounds.top();
        int mapSize = bounds.width();
        int viewSize = overview ? SeedMap.SIZE : SeedMap.VIEW_SIZE;
        FactoryGuiGraphics.of(graphics).blitSprite(LegacySprites.SQUARE_RECESSED_PANEL, mapX - 2, mapY - 2, mapSize + 4, mapSize + 4);
        if (texture != null) {
            graphics.enableScissor(mapX, mapY, mapX + mapSize, mapY + mapSize);
            graphics.pose().pushMatrix();
            graphics.pose().translate(mapX, mapY);
            graphics.pose().scale(mapSize / (float) viewSize);
            graphics.pose().translate((float) (texture.map.chunkX() - viewX + (viewSize - SeedMap.SIZE) / 2), (float) (texture.map.chunkZ() - viewZ + (viewSize - SeedMap.SIZE) / 2));
            graphics.blit(texture.getTextureView(), texture.getSampler(), 0, 0, SeedMap.SIZE, SeedMap.SIZE, 0, 1, 0, 1);
            graphics.pose().popMatrix();
            for (SeedMapMarker entry : texture.map.markers()) {
                SeedMapMarker marker = previewMarker(entry);
                if (!isVisible(marker, overview)) continue;
                ScreenRectangle icon = markerBounds(bounds, marker, viewX, viewZ, viewSize);
                if (!bounds.overlaps(icon)) continue;
                graphics.blit(RenderPipelines.GUI_TEXTURED, marker.texture(), icon.left(), icon.top(), 0, 0,
                        icon.width(), icon.height(), icon.width(), icon.height());
            }
            graphics.disableScissor();
        }
    }

    private void renderGuide(GuiGraphicsExtractor graphics, ScreenRectangle bounds) {
        if (overviewTexture == null) return;
        int size = bounds.width();
        double pixelsPerChunk = (double) size / SeedMap.SIZE;
        int guideSize = (int) Math.round(SeedMap.VIEW_SIZE * pixelsPerChunk);
        int x = Mth.floor(bounds.left() + (viewX + SeedMap.PADDING) * pixelsPerChunk);
        int y = Mth.floor(bounds.top() + (viewZ + SeedMap.PADDING) * pixelsPerChunk);
        graphics.enableScissor(bounds.left(), bounds.top(), bounds.left() + size, bounds.top() + size);
        graphics.outline(x, y, guideSize, guideSize, CommonColor.BLACK.get());
        graphics.outline(x + 1, y + 1, guideSize - 2, guideSize - 2, CommonColor.WHITE.get());
        graphics.disableScissor();
    }

    private ScreenRectangle markerBounds(ScreenRectangle map, SeedMapMarker marker, double viewX, double viewZ, int viewSize) {
        double pixelsPerSample = (double) map.width() / viewSize;
        int x = Mth.floor(map.left() + (marker.pos().getX() / (double) SeedMap.BLOCKS_PER_PIXEL - viewX + viewSize / 2) * pixelsPerSample);
        int y = Mth.floor(map.top() + (marker.pos().getZ() / (double) SeedMap.BLOCKS_PER_PIXEL - viewZ + viewSize / 2) * pixelsPerSample);
        int size = scaled(marker.size());
        return new ScreenRectangle(x - size / 2, y - size / 2, size, size);
    }

    private Component tooltipAt(ScreenRectangle bounds, SeedMap map, double viewX, double viewZ, boolean overview, int mouseX, int mouseY) {
        int viewSize = overview ? SeedMap.SIZE : SeedMap.VIEW_SIZE;
        for (int i = map.markers().size() - 1; i >= 0; i--) {
            SeedMapMarker marker = previewMarker(map.markers().get(i));
            if (!isVisible(marker, overview)) continue;
            if (markerBounds(bounds, marker, viewX, viewZ, viewSize).containsPoint(mouseX, mouseY)) return marker.tooltip();
        }
        ChunkPos chunk = mapChunk(bounds, viewX, viewZ, viewSize, mouseX, mouseY);
        if (!map.contains(chunk)) return null;
        ResourceKey<Biome> biome = map.biomeAt(chunk).unwrapKey().orElse(null);
        if (biome == null) return null;
        String key = "biome." + biome.identifier().toLanguageKey();
        MutableComponent message = Component.translatableWithFallback(key, biome.identifier().toString());
        if (LegacyTipManager.hasTip(key + ".description")) {
            message.append("\n\n").append(Component.translatable(key + ".description"));
        }
        return message;
    }

    private void updateHelp(ScreenRectangle bounds, SeedMapTexture texture, double viewX, double viewZ, boolean overview, int mouseX, int mouseY) {
        if (!bounds.containsPoint(mouseX, mouseY)) return;
        Component hovered = texture == null ? null : tooltipAt(bounds, texture.map, viewX, viewZ, overview, mouseX, mouseY);
        if (Objects.equals(helpText, hovered)) return;
        helpText = hovered;
        helpLabel = null;
        helpScroll.resetScrolled();
    }

    private void renderHelp(GuiGraphicsExtractor graphics) {
        if (helpText == null) return;
        int width = (int) (help.getWidth() / scale) - 14;
        int height = (int) (help.getHeight() / scale) - 24;
        if (helpLabel == null) helpLabel = MultiLineLabel.create(font, helpText, width);
        helpScroll.scrolled.max = Math.max(0, helpLabel.getLineCount() - height / 12);
        graphics.pose().pushMatrix();
        graphics.pose().translate(help.getX(), help.getY());
        graphics.pose().scale(scale);
        helpScroll.extractRenderState(graphics, 7, 7, width, height,
                () -> helpLabel.visitLines(TextAlignment.LEFT, 7, 7, 12, graphics.textRenderer()));
        graphics.pose().popMatrix();
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

    private void recenter() {
        if (texture == null) return;
        for (SeedMapMarker marker : texture.map.markers()) {
            if (!marker.isSpawn()) continue;
            BlockPos spawn = previewMarker(marker).pos();
            jumpTo(spawn.getX() / (double) SeedMap.BLOCKS_PER_PIXEL, spawn.getZ() / (double) SeedMap.BLOCKS_PER_PIXEL);
            return;
        }
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
            case InputConstants.KEY_HOME -> recenter();
            case InputConstants.KEY_S -> toggleStructures();
            case InputConstants.KEY_X -> {
                if (hovered != null) setSeedStart(hovered);
            }
            case InputConstants.KEY_O -> setSeedStart(null);
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
            recenter();
        } else if (state.is(ControllerBinding.LEFT_STICK_BUTTON) && state.justPressed) {
            toggleStructures();
        } else if (state.is(ControllerBinding.RIGHT_STICK) && state instanceof BindingState.Axis stick && state.pressed
                && Math.abs(stick.x) >= Math.abs(stick.y)) {
            pan(stick.x > 0 ? ScreenDirection.RIGHT : ScreenDirection.LEFT);
        }
    }

    @Override
    public int getBindingMouseClick(BindingState state) {
        return state.is(ControllerBinding.DOWN_BUTTON) ? 0 : -1;
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
        if (LegacyRenderUtil.isMouseOver(mouseX, mouseY, help.getX(), help.getY(), help.getWidth(), help.getHeight())) {
            return helpText != null && helpScroll.mouseScrolled(scrollY);
        }
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
        return mapBounds(detail, 12, 27, 211);
    }

    private ChunkPos mapChunk(ScreenRectangle bounds, double viewX, double viewZ, int viewSize, int mouseX, int mouseY) {
        double samplesPerPixel = (double) viewSize / bounds.width();
        int x = Mth.floor(viewX + (mouseX - bounds.left()) * samplesPerPixel - viewSize / 2);
        int z = Mth.floor(viewZ + (mouseY - bounds.top()) * samplesPerPixel - viewSize / 2);
        return new ChunkPos(x, z);
    }

    private ScreenRectangle mapBounds(Panel panel, int x, int y, int size) {
        return new ScreenRectangle(panel.getX() + scaled(x), panel.getY() + scaled(y), scaled(size), scaled(size));
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
