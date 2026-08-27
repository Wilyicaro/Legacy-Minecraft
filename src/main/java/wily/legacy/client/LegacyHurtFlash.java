package wily.legacy.client;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.LayeringTransform;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import wily.legacy.Legacy4J;

public class LegacyHurtFlash {
    private static final Identifier TEXTURE = Legacy4J.createModLocation("hurt_flash");
    private static final RenderType RENDER_TYPE = RenderType.create("legacy_hurt_flash", RenderSetup.builder(LegacyRenderPipelines.LEGACY_HURT_FLASH).withTexture("Sampler0", TEXTURE).useLightmap().sortOnUpload().createRenderSetup());
    private static final RenderType EQUIPMENT_RENDER_TYPE = RenderType.create("legacy_equipment_hurt_flash", RenderSetup.builder(LegacyRenderPipelines.LEGACY_HURT_FLASH).withTexture("Sampler0", TEXTURE).useLightmap().setLayeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING).sortOnUpload().createRenderSetup());
    private static boolean registered;

    public static RenderType renderType() {
        return renderType(false);
    }

    private static RenderType renderType(boolean equipment) {
        if (!registered) {
            NativeImage image = new NativeImage(1, 1, false);
            image.setPixel(0, 0, -1);
            Minecraft.getInstance().getTextureManager().register(TEXTURE, new DynamicTexture(TEXTURE::toString, image));
            registered = true;
        }
        return equipment ? EQUIPMENT_RENDER_TYPE : RENDER_TYPE;
    }

    public static <S> void submitModel(OrderedSubmitNodeCollector collector, Model<? super S> model, S state, PoseStack poseStack, RenderType renderType, int light, int overlay, int color, TextureAtlasSprite sprite, int outlineColor, ModelFeatureRenderer.CrumblingOverlay crumblingOverlay, boolean equipment) {
        if (!(state instanceof LivingEntityRenderState renderState) || !renderState.hasRedOverlay || renderType.isOutline() || renderType.format() != DefaultVertexFormat.ENTITY || ARGB.alpha(color) != 255) {
            collector.submitModel(model, state, poseStack, renderType, light, overlay, color, sprite, outlineColor, crumblingOverlay);
            return;
        }
        collector.submitModel(model, state, poseStack, renderType, light, OverlayTexture.NO_OVERLAY, color, sprite, outlineColor, crumblingOverlay);
        collector.submitModel(model, state, poseStack, renderType(equipment), light, OverlayTexture.NO_OVERLAY, ARGB.multiply(color, 0x4DFF0000), null, 0, null);
    }
}
