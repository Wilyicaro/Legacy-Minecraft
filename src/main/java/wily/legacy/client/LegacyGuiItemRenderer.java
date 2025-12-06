package wily.legacy.client;

import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.TextureFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.gui.render.pip.OversizedItemRenderer;
import net.minecraft.client.gui.render.state.BlitRenderState;
import net.minecraft.client.gui.render.state.GuiItemRenderState;
import net.minecraft.client.gui.render.state.GuiRenderState;
import net.minecraft.client.gui.render.state.pip.OversizedItemRenderState;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.item.TrackingItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3x2f;
import wily.factoryapi.FactoryAPI;
import wily.factoryapi.FactoryAPIPlatform;
import wily.factoryapi.util.ColorUtil;

import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

public class LegacyGuiItemRenderer implements AutoCloseable {
    public static final Logger LOGGER = LogManager.getLogger("legacy_gui_item_renderer");
    private static final int MAXIMUM_ITEM_ATLAS_SIZE = RenderSystem.getDevice().getMaxTextureSize();
    public static float OPACITY = 1;
    private final int size;
    private final float opacity;
    private final Map<Object, AtlasPosition> atlasPositions = new Object2ObjectOpenHashMap<>();
    private final Map<Object, OversizedItemRenderer> oversizedItemRenderers = new Object2ObjectOpenHashMap<>();
    private final CachedOrthoProjectionMatrixBuffer itemsProjectionMatrixBuffer = new CachedOrthoProjectionMatrixBuffer("items", -1000.0F, 1000.0F, true);
    @Nullable
    private GpuTexture itemsAtlas;
    @Nullable
    private GpuTextureView itemsAtlasView;
    @Nullable
    private GpuTexture itemsAtlasDepth;
    @Nullable
    private GpuTextureView itemsAtlasDepthView;
    private int itemAtlasX;
    private int itemAtlasY;
    private int cachedGuiScale;
    private boolean isValid = true;

    public LegacyGuiItemRenderer(int size, float opacity) {
        this.size = size;
        this.opacity = opacity;
    }

    public LegacyGuiItemRenderer(long sizopacity) {
        this((int) (sizopacity >> 32), Float.intBitsToFloat((int) sizopacity));
    }

    public static void pushOpacity(float hudOpacity) {
        OPACITY = hudOpacity;
    }

    public static void popOpacity() {
        OPACITY = 1;
    }

    public static void secureTranslucentRender(boolean translucent, float alpha, Consumer<Boolean> render) {
        if (!translucent) {
            render.accept(false);
            return;
        }

        pushOpacity(alpha);
        render.accept(true);
        popOpacity();
    }

    public static float getXScale(Matrix3x2f matrix) {
        return Mth.sqrt(matrix.m00() * matrix.m00() + matrix.m01() * matrix.m01());
    }

    public static float getYScale(Matrix3x2f matrix) {
        return Mth.sqrt(matrix.m10() * matrix.m10() + matrix.m11() * matrix.m11());
    }

    public static int getScale(Matrix3x2f matrix) {
        return Math.round(Math.max(getXScale(matrix), getYScale(matrix)) * 16);
    }

    private void createAtlasTextures(int i) {
        GpuDevice gpuDevice = RenderSystem.getDevice();
        this.itemsAtlas = gpuDevice.createTexture("UI items atlas", 12, TextureFormat.RGBA8, i, i, 1, 1);
        this.itemsAtlas.setTextureFilter(FilterMode.NEAREST, false);
        this.itemsAtlasView = gpuDevice.createTextureView(this.itemsAtlas);
        this.itemsAtlasDepth = gpuDevice.createTexture("UI items atlas depth", 8, TextureFormat.DEPTH32, i, i, 1, 1);
        this.itemsAtlasDepthView = gpuDevice.createTextureView(this.itemsAtlasDepth);
        gpuDevice.createCommandEncoder().clearColorAndDepthTextures(this.itemsAtlas, 0, this.itemsAtlasDepth, 1.0);
    }

    public void prepareItemElements(FeatureRenderDispatcher dispatcher, SubmitNodeCollector submitNodeCollector, MultiBufferSource.BufferSource bufferSource, GuiRenderState renderState, int frameNumber) {
        if (!renderState.getItemModelIdentities().isEmpty()) {
            int i = this.getGuiScaleInvalidatingItemAtlasIfChanged();
            int j = size * i;
            int k = this.calculateAtlasSizeInPixels(renderState, j);
            if (this.itemsAtlas == null) {
                this.createAtlasTextures(k);
            }

            RenderSystem.outputColorTextureOverride = this.itemsAtlasView;
            RenderSystem.outputDepthTextureOverride = this.itemsAtlasDepthView;
            RenderSystem.setProjectionMatrix(this.itemsProjectionMatrixBuffer.getBuffer(k, k), ProjectionType.ORTHOGRAPHIC);
            Minecraft.getInstance().gameRenderer.getLighting().setupFor(Lighting.Entry.ITEMS_3D);
            PoseStack poseStack = new PoseStack();
            MutableBoolean mutableBoolean = new MutableBoolean(false);
            MutableBoolean mutableBoolean2 = new MutableBoolean(false);
            renderState
                    .forEachItem(
                            guiItemRenderState -> {
                                if (LegacyGuiItemRenderState.of(guiItemRenderState).size() != size) return;
                                if (!LegacyOptions.enhancedItemTranslucency.get() && LegacyGuiItemRenderState.of(guiItemRenderState).opacity() != opacity)
                                    return;
                                if (guiItemRenderState.oversizedItemBounds() != null) {
                                    mutableBoolean2.setTrue();
                                } else {
                                    TrackingItemStackRenderState trackingItemStackRenderState = guiItemRenderState.itemStackRenderState();
                                    AtlasPosition atlasPosition = this.atlasPositions.get(trackingItemStackRenderState.getModelIdentity());
                                    if (atlasPosition == null || trackingItemStackRenderState.isAnimated() && atlasPosition.lastAnimatedOnFrame != frameNumber) {
                                        if (this.itemAtlasX + j > k) {
                                            this.itemAtlasX = 0;
                                            this.itemAtlasY += j;
                                        }

                                        boolean bl = trackingItemStackRenderState.isAnimated() && atlasPosition != null;
                                        if (!bl && this.itemAtlasY + j > k) {
                                            if (mutableBoolean.isFalse()) {
                                                LOGGER.warn("Trying to render too many items in GUI at the same time. Skipping some of them.");
                                                mutableBoolean.setTrue();
                                            }
                                        } else {
                                            int kx = bl ? atlasPosition.x : this.itemAtlasX;
                                            int l = bl ? atlasPosition.y : this.itemAtlasY;
                                            if (bl) {
                                                RenderSystem.getDevice().createCommandEncoder().clearColorAndDepthTextures(this.itemsAtlas, 0, this.itemsAtlasDepth, 1.0, kx, k - l - j, j, j);
                                            }

                                            this.renderItemToAtlas(dispatcher, submitNodeCollector, bufferSource, trackingItemStackRenderState, poseStack, kx, l, j);
                                            float f = (float) kx / k;
                                            float g = (float) (k - l) / k;
                                            this.submitBlitFromItemAtlas(renderState, guiItemRenderState, f, g, j, k);
                                            if (bl) {
                                                atlasPosition.lastAnimatedOnFrame = frameNumber;
                                            } else {
                                                this.atlasPositions
                                                        .put(
                                                                guiItemRenderState.itemStackRenderState().getModelIdentity(),
                                                                new AtlasPosition(this.itemAtlasX, this.itemAtlasY, f, g, frameNumber)
                                                        );
                                                this.itemAtlasX += j;
                                            }
                                        }
                                    } else {
                                        this.submitBlitFromItemAtlas(renderState, guiItemRenderState, atlasPosition.u, atlasPosition.v, j, k);
                                    }
                                }
                            }
                    );
            RenderSystem.outputColorTextureOverride = null;
            RenderSystem.outputDepthTextureOverride = null;
            if (mutableBoolean2.getValue()) {
                renderState
                        .forEachItem(
                                guiItemRenderState -> {
                                    if (LegacyGuiItemRenderState.of(guiItemRenderState).size() != size) return;
                                    if (!LegacyOptions.enhancedItemTranslucency.get() && LegacyGuiItemRenderState.of(guiItemRenderState).opacity() != opacity)
                                        return;
                                    if (guiItemRenderState.oversizedItemBounds() != null) {
                                        TrackingItemStackRenderState trackingItemStackRenderState = guiItemRenderState.itemStackRenderState();
                                        OversizedItemRenderer oversizedItemRenderer = oversizedItemRenderers
                                                .computeIfAbsent(trackingItemStackRenderState.getModelIdentity(), object -> new OversizedItemRenderer(bufferSource));
                                        ScreenRectangle screenRectangle = guiItemRenderState.oversizedItemBounds();
                                        OversizedItemRenderState oversizedItemRenderState = new OversizedItemRenderState(
                                                guiItemRenderState, screenRectangle.left(), screenRectangle.top(), screenRectangle.right(), screenRectangle.bottom()
                                        );
                                        oversizedItemRenderer.prepare(oversizedItemRenderState, renderState, i);
                                    }
                                }
                        );
            }
        }
    }

    private void submitBlitFromItemAtlas(GuiRenderState renderState, GuiItemRenderState guiItemRenderState, float u, float v, int size, int atlasSize) {
        float h = u + (float) size / atlasSize;
        float k = v + (float) (-size) / atlasSize;
        //Compatibility with VulkanMod rendering
        if (FactoryAPI.isModLoaded("vulkanmod")) {
            v = 1 - v;
            k = v + (float) (size) / atlasSize;
        }
        float opacity = LegacyGuiItemRenderState.of(guiItemRenderState).opacity();
        renderState
                .submitBlitToCurrentLayer(
                        new BlitRenderState(
                                opacity == 1.0f || !LegacyOptions.enhancedItemTranslucency.get() ? RenderPipelines.GUI_TEXTURED_PREMULTIPLIED_ALPHA : RenderPipelines.GUI_TEXTURED,
                                TextureSetup.singleTexture(this.itemsAtlasView),
                                guiItemRenderState.pose(),
                                guiItemRenderState.x(),
                                guiItemRenderState.y(),
                                guiItemRenderState.x() + 16,
                                guiItemRenderState.y() + 16,
                                u,
                                h,
                                v,
                                k,
                                opacity == 1.0f || !LegacyOptions.enhancedItemTranslucency.get() ? 0xFFFFFFFF : ColorUtil.withAlpha(0xFFFFFF, opacity),
                                guiItemRenderState.scissorArea(),
                                null
                        )
                );
    }

    private void renderItemToAtlas(FeatureRenderDispatcher featureRenderDispatcher, SubmitNodeCollector submitNodeCollector, MultiBufferSource.BufferSource bufferSource, TrackingItemStackRenderState trackingItemStackRenderState, PoseStack poseStack, int x, int y, int k) {
        poseStack.pushPose();
        poseStack.translate(x + k / 2.0F, y + k / 2.0F, 0.0F);
        poseStack.scale(k, -k, k);
        boolean bl = !trackingItemStackRenderState.usesBlockLight();
        if (bl) {
            Minecraft.getInstance().gameRenderer.getLighting().setupFor(Lighting.Entry.ITEMS_FLAT);
        } else {
            Minecraft.getInstance().gameRenderer.getLighting().setupFor(Lighting.Entry.ITEMS_3D);
        }
        if (opacity != 1.0f && !LegacyOptions.enhancedItemTranslucency.get())
            LegacyFeatureRenderDispatcher.of(featureRenderDispatcher).setBufferSource(BufferSourceWrapper.translucent(bufferSource, opacity));
        RenderSystem.enableScissorForRenderTypeDraws(x, this.itemsAtlas.getHeight(0) - y - k, k, k);
        trackingItemStackRenderState.submit(poseStack, submitNodeCollector, 15728880, OverlayTexture.NO_OVERLAY, 0);
        featureRenderDispatcher.renderAllFeatures();
        bufferSource.endBatch();
        LegacyFeatureRenderDispatcher.of(featureRenderDispatcher).setBufferSource(bufferSource);
        RenderSystem.disableScissorForRenderTypeDraws();
        poseStack.popPose();
    }

    private int calculateAtlasSizeInPixels(GuiRenderState renderState, int i) {
        Set<Object> set = renderState.getItemModelIdentities();
        int j;
        if (this.atlasPositions.isEmpty()) {
            j = set.size();
        } else {
            j = this.atlasPositions.size();

            for (Object object : set) {
                if (!this.atlasPositions.containsKey(object)) {
                    j++;
                }
            }
        }

        if (this.itemsAtlas != null) {
            int k = this.itemsAtlas.getWidth(0) / i;
            int l = k * k;
            if (j < l) {
                return this.itemsAtlas.getWidth(0);
            }

            this.invalidateItemAtlas();
        }

        int k = set.size();
        int l = Mth.smallestSquareSide(k + k / 2);
        return Math.clamp(Mth.smallestEncompassingPowerOfTwo(l * i), 512, MAXIMUM_ITEM_ATLAS_SIZE);
    }

    private int getGuiScaleInvalidatingItemAtlasIfChanged() {
        int i = Minecraft.getInstance().getWindow().getGuiScale();
        if (i != this.cachedGuiScale) {
            this.invalidateItemAtlas();

            for (OversizedItemRenderer oversizedItemRenderer : this.oversizedItemRenderers.values()) {
                oversizedItemRenderer.invalidateTexture();
            }

            this.cachedGuiScale = i;
        }

        return i;
    }

    @Override
    public void close() {
        invalidateItemAtlas();
    }

    private void invalidateItemAtlas() {
        this.itemAtlasX = 0;
        this.itemAtlasY = 0;
        this.atlasPositions.clear();
        if (this.itemsAtlas != null) {
            this.itemsAtlas.close();
            this.itemsAtlas = null;
        }

        if (this.itemsAtlasView != null) {
            this.itemsAtlasView.close();
            this.itemsAtlasView = null;
        }

        if (this.itemsAtlasDepth != null) {
            this.itemsAtlasDepth.close();
            this.itemsAtlasDepth = null;
        }

        if (this.itemsAtlasDepthView != null) {
            this.itemsAtlasDepthView.close();
            this.itemsAtlasDepthView = null;
        }
    }

    public boolean isValid() {
        return isValid;
    }

    public void markValid() {
        isValid = true;
    }

    public void markInvalid() {
        isValid = false;
    }

    public static final class AtlasPosition {
        final int x;
        final int y;
        final float u;
        final float v;
        int lastAnimatedOnFrame;

        AtlasPosition(int i, int j, float f, float g, int k) {
            this.x = i;
            this.y = j;
            this.u = f;
            this.v = g;
            this.lastAnimatedOnFrame = k;
        }
    }
}
