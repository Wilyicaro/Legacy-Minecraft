package wily.legacy.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Holder;
import net.minecraft.util.ARGB;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;
import wily.factoryapi.base.client.FactoryGuiGraphics;

public class LegacyActivationAnim {


    public static Render itemActivationRenderReplacement = null;

    public static void display(Render render) {
        itemActivationRenderReplacement = render;
        Minecraft.getInstance().gameRenderer.displayItemActivation(ItemStack.EMPTY);
    }

    public static void displayEffect(Holder<MobEffect> effect) {
        display(((pose, f, source) -> {
            pose.pushPose();
            pose.scale(0.5f, 0.5f, 0.1f);
            TextureAtlasSprite sprite = FactoryGuiGraphics.getSprites().getSprite(Gui.getMobEffectSprite(effect));
            renderTex(sprite, pose, source, false);
            pose.pushPose();
            pose.translate(0.5f, 0.5f, 0.5f);
            pose.mulPose(Axis.YP.rotationDegrees(180));
            pose.translate(-0.5f, -0.5f, -0.5f);
            renderTex(sprite, pose, source, true);
            pose.popPose();
            pose.popPose();
        }));
    }

    private static void renderTex(TextureAtlasSprite textureAtlasSprite, PoseStack poseStack, MultiBufferSource multiBufferSource, boolean invertHorizontally) {
        int i = ARGB.gray(1.0f);
        float u0 = textureAtlasSprite.getU0();
        float u1 = textureAtlasSprite.getU1();
        float v0 = textureAtlasSprite.getV0();
        float v1 = textureAtlasSprite.getV1();

        if (invertHorizontally) {
            u0 = textureAtlasSprite.getU1();
            u1 = textureAtlasSprite.getU0();
        }

        Matrix4f matrix4f = poseStack.last().pose();
        VertexConsumer vertexConsumer = multiBufferSource.getBuffer(RenderTypes.blockScreenEffect(textureAtlasSprite.atlasLocation()));
        vertexConsumer.addVertex(matrix4f, -1.0F, -1.0F, 0).setUv(u1, v1).setColor(i);
        vertexConsumer.addVertex(matrix4f, 1.0F, -1.0F, 0).setUv(u0, v1).setColor(i);
        vertexConsumer.addVertex(matrix4f, 1.0F, 1.0F, 0).setUv(u0, v0).setColor(i);
        vertexConsumer.addVertex(matrix4f, -1.0F, 1.0F, 0).setUv(u1, v0).setColor(i);
    }

    @FunctionalInterface
    public interface Render {
        void render(PoseStack poseStack, float partialTick, MultiBufferSource bufferSource);
    }
}
