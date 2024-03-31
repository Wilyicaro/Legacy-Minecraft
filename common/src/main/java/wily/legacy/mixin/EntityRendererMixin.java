package wily.legacy.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.LegacyMinecraftClient;
import wily.legacy.player.LegacyPlayerInfo;
import wily.legacy.util.ScreenUtil;

@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin {
    @Shadow @Final private Font font;

    @Inject(method = "renderNameTag", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;scale(FFF)V", shift = At.Shift.AFTER))
    protected void renderNameTag(Entity entity, Component component, PoseStack poseStack, MultiBufferSource multiBufferSource, int i, CallbackInfo ci) {
        if (!component.equals(entity.getDisplayName()) || !ScreenUtil.getLegacyOptions().displayNameTagBorder().get()) return;
        String name = component.getString();
        int j = "deadmau5".equals(name) ? -10 : 0;
        int h = (int) (-font.width(component) / 2f);
        Minecraft minecraft = Minecraft.getInstance();
        float[] color = !(entity instanceof AbstractClientPlayer p)  || minecraft.getConnection() == null || !(minecraft.getConnection().getPlayerInfo(p.getUUID()) instanceof LegacyPlayerInfo info) || info.getPosition() == 0 ?  new float[]{0,0,0} : LegacyMinecraftClient.getVisualPlayerColor(info);
        poseStack.pushPose();
        fill(RenderType.debugLineStrip(1.0),multiBufferSource, poseStack, h - 1, j - 1, h + font.width(component) + 1,j + 9, color[0],color[1],color[2],1.0f);
        poseStack.translate(0, 8,0);
        poseStack.scale(1,-1,1);
        fill(RenderType.debugLineStrip(1.0),multiBufferSource, poseStack, h - 1, j - 1, h + font.width(component) + 1,j + 9, color[0],color[1],color[2],1.0f);
        poseStack.popPose();
    }
    public void fill(RenderType renderType, MultiBufferSource multiBufferSource,PoseStack poseStack, float i, float j, float k, float l, float r, float g, float b , float a) {
        float o;
        Matrix4f matrix4f = poseStack.last().pose();
        if (i < k) {
            o = i;
            i = k;
            k = o;
        }
        if (j < l) {
            o = j;
            j = l;
            l = o;
        }
        VertexConsumer vertexConsumer = multiBufferSource.getBuffer(renderType);
        vertexConsumer.vertex(matrix4f, i, j, 0).color(r,g,b,a).endVertex();
        vertexConsumer.vertex(matrix4f, i, l, 0).color(r,g,b,a).endVertex();
        vertexConsumer.vertex(matrix4f, k, l, 0).color(r,g,b,a).endVertex();
        vertexConsumer.vertex(matrix4f, k, j, 0).color(r,g,b,a).endVertex();
    }
}
