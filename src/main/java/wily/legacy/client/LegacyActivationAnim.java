package wily.legacy.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Holder;
import net.minecraft.util.ARGB;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;

public class LegacyActivationAnim {


    public static Render itemActivationRenderReplacement = null;

    public static void display(Render render){
        itemActivationRenderReplacement = render;
        Minecraft.getInstance().gameRenderer.displayItemActivation(ItemStack.EMPTY);
    }

    public static void displayEffect(Holder<MobEffect> effect){
        display(((pose, f, source) -> {
            pose.pushPose();
            pose.scale(0.5f,0.5f, 0.5f);
            TextureAtlasSprite sprite = Minecraft.getInstance().getGuiSprites().getSprite(Gui.getMobEffectSprite(effect));
            renderTex(sprite, pose, source);
            pose.pushPose();
            pose.translate(0.5f, 0.5f, 0.5f);
            pose.mulPose(Axis.ZP.rotationDegrees(180));
            pose.translate(-0.5f, -0.5f, -0.5f);
            renderTex(sprite, pose, source);
            pose.popPose();
            pose.popPose();
        }));
    }

    private static void renderTex(TextureAtlasSprite textureAtlasSprite, PoseStack poseStack, MultiBufferSource multiBufferSource) {
        int i = ARGB.colorFromFloat(1.0F, 0.1F, 0.1F, 0.1F);
        float m = textureAtlasSprite.getU0();
        float n = textureAtlasSprite.getU1();
        float o = textureAtlasSprite.getV0();
        float p = textureAtlasSprite.getV1();
        Matrix4f matrix4f = poseStack.last().pose();
        VertexConsumer vertexConsumer = multiBufferSource.getBuffer(RenderType.blockScreenEffect(textureAtlasSprite.atlasLocation()));
        vertexConsumer.addVertex(matrix4f, -1.0F, -1.0F, -0.5F).setUv(n, p).setColor(i);
        vertexConsumer.addVertex(matrix4f, 1.0F, -1.0F, -0.5F).setUv(m, p).setColor(i);
        vertexConsumer.addVertex(matrix4f, 1.0F, 1.0F, -0.5F).setUv(m, o).setColor(i);
        vertexConsumer.addVertex(matrix4f, -1.0F, 1.0F, -0.5F).setUv(n, o).setColor(i);
    }

    @FunctionalInterface
    public interface Render {
        void render(PoseStack poseStack, float partialTick, MultiBufferSource bufferSource);
    }
}
