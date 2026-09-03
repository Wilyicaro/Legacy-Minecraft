package wily.legacy.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import wily.factoryapi.util.ColorUtil;
import wily.legacy.mixin.base.client.RenderSetupAccessor;
import wily.legacy.mixin.base.client.RenderTypeAccessor;

// 26.2 replacement for the swappable MultiBufferSource.BufferSource that BufferSourceWrapper used to
// install on FeatureRenderDispatcher. 26.2 deleted the buffer-source abstraction entirely, so the
// remap is applied at the one choke point every feature renderer goes through:
// RenderTypeFeatureRenderer#getVertexBuilder(RenderType), via RenderTypeFeatureRendererMixin.
public final class LegacyItemTranslucency {
    private static float opacity = 1.0f;
    private static boolean active;

    private LegacyItemTranslucency() {
    }

    public static void push(float opacity) {
        LegacyItemTranslucency.opacity = opacity;
        active = true;
    }

    public static void pop() {
        active = false;
        opacity = 1.0f;
    }

    public static boolean isActive() {
        return active;
    }

    public static RenderType remapRenderType(RenderType renderType) {
        if (!active) return renderType;
        if (renderType == Sheets.cutoutBlockItemSheet()) return Sheets.translucentBlockItemSheet();
        if (renderType.format() == DefaultVertexFormat.ENTITY) {
            RenderSetup setup = ((RenderTypeAccessor) renderType).getState();
            RenderSetup.TextureBinding binding = ((RenderSetupAccessor) (Object) setup).getTextureBindings().get("Sampler0");
            if (binding != null) return RenderTypes.entityTranslucentCullItemTarget(binding.location());
        }
        return renderType;
    }

    public static VertexConsumer wrapVertexConsumer(VertexConsumer consumer) {
        return active ? new VertexConsumerWrapper(consumer).setColorMultiplier(ColorUtil.withAlpha(0xFFFFFF, opacity)) : consumer;
    }
}
