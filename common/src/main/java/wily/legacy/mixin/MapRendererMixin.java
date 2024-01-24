package wily.legacy.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.MapRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.saveddata.maps.MapDecoration;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.LegacyMinecraftClient;

import java.util.Objects;

@Mixin(MapRenderer.MapInstance.class)
public abstract class MapRendererMixin {
    @Shadow private boolean requiresUpload;

    @Shadow protected abstract void updateTexture();

    @Shadow @Final private RenderType renderType;
    @Shadow private MapItemSavedData data;
    @Unique
    private static final RenderType MAP_ICONS = RenderType.text(new ResourceLocation("textures/map/map_icons.png"));

    @Inject(method = "draw", at = @At("HEAD"), cancellable = true)
    void draw(PoseStack poseStack, MultiBufferSource multiBufferSource, boolean bl, int i, CallbackInfo ci) {
        ci.cancel();
        if (this.requiresUpload) {
            this.updateTexture();
            this.requiresUpload = false;
        }
        Minecraft minecraft = Minecraft.getInstance();
        Font font = minecraft.font;
        if (!bl) {
            poseStack.pushPose();
            poseStack.translate(-0.2f,0.4f,-0.1f);
            poseStack.scale(1f, 0.95f, 1);
            font.drawInBatch("X:%s,Y:%s,Z:%s".formatted((int) minecraft.player.getX(), (int) minecraft.player.getY(), (int) minecraft.player.getZ()), 0.0f, 0.0f, 0, false, poseStack.last().pose(), multiBufferSource, Font.DisplayMode.NORMAL, 0, i);
            poseStack.popPose();
        }
        poseStack.pushPose();
        if (!bl) {
            poseStack.translate(10, 11, 0);
            poseStack.scale(98 / 116f, 98 / 116f, 1);
        }
        Matrix4f matrix4f = poseStack.last().pose();
        VertexConsumer vertexConsumer = multiBufferSource.getBuffer(this.renderType);
        vertexConsumer.vertex(matrix4f, 0.0f, 128.0f, -0.01f).color(255, 255, 255, 255).uv(0.0f, 1.0f).uv2(i).endVertex();
        vertexConsumer.vertex(matrix4f, 128.0f, 128.0f, -0.01f).color(255, 255, 255, 255).uv(1.0f, 1.0f).uv2(i).endVertex();
        vertexConsumer.vertex(matrix4f, 128.0f, 0.0f, -0.01f).color(255, 255, 255, 255).uv(1.0f, 0.0f).uv2(i).endVertex();
        vertexConsumer.vertex(matrix4f, 0.0f, 0.0f, -0.01f).color(255, 255, 255, 255).uv(0.0f, 0.0f).uv2(i).endVertex();
        int l = 0;
        for (MapDecoration mapDecoration : this.data.getDecorations()) {
            if (bl && !mapDecoration.renderOnFrame()) continue;
            poseStack.pushPose();
            poseStack.translate(0.0f + (float)mapDecoration.x() / 2.0f + 64.0f, 0.0f + (float)mapDecoration.y() / 2.0f + 64.0f, -0.02f);
            poseStack.mulPose(Axis.ZP.rotationDegrees((float)(mapDecoration.rot() * 360) / 16.0f));
            poseStack.scale(4.0f, 4.0f, 3.0f);
            poseStack.translate(-0.125f, 0.125f, 0.0f);
            byte b = mapDecoration.getImage();
            float g = (float)(b % 16) / 16.0f;
            float h = (float)(b / 16) / 16.0f;
            float m = (float)(b % 16 + 1) / 16.0f;
            float n = (float)(b / 16 + 1) / 16.0f;
            Matrix4f matrix4f2 = poseStack.last().pose();
            boolean isPlayer = (mapDecoration.type() == MapDecoration.Type.PLAYER || mapDecoration.type() == MapDecoration.Type.PLAYER_OFF_MAP || mapDecoration.type() == MapDecoration.Type.PLAYER_OFF_LIMITS);
            int[] color = isPlayer && mapDecoration.name() != null ? LegacyMinecraftClient.getVisualPlayerColor(mapDecoration.name().getString()) : new int[]{255,255,255};
            VertexConsumer vertexConsumer2 = multiBufferSource.getBuffer(MAP_ICONS);
            vertexConsumer2.vertex(matrix4f2, -1.0f, 1.0f, (float)l * -0.001f).color(color[0], color[1], color[2], 255).uv(g, h).uv2(i).endVertex();
            vertexConsumer2.vertex(matrix4f2, 1.0f, 1.0f, (float)l * -0.001f).color(color[0], color[1], color[2], 255).uv(m, h).uv2(i).endVertex();
            vertexConsumer2.vertex(matrix4f2, 1.0f, -1.0f, (float)l * -0.001f).color(color[0], color[1], color[2], 255).uv(m, n).uv2(i).endVertex();
            vertexConsumer2.vertex(matrix4f2, -1.0f, -1.0f, (float)l * -0.001f).color(color[0], color[1], color[2], 255).uv(g, n).uv2(i).endVertex();
            poseStack.popPose();
            if (mapDecoration.name() != null && !isPlayer) {
                Component component = mapDecoration.name();
                float p = font.width(component);
                float f2 = 25.0f / p;
                Objects.requireNonNull(font);
                float q = Mth.clamp(f2, 0.0f, 6.0f / 9.0f);
                poseStack.pushPose();
                poseStack.translate(0.0f + (float)mapDecoration.x() / 2.0f + 64.0f - p * q / 2.0f, 0.0f + (float)mapDecoration.y() / 2.0f + 64.0f + 4.0f, -0.025f);
                poseStack.scale(q, q, 1.0f);
                poseStack.translate(0.0f, 0.0f, -0.1f);
                font.drawInBatch(component, 0.0f, 0.0f, -1, false, poseStack.last().pose(), multiBufferSource, Font.DisplayMode.NORMAL, Integer.MIN_VALUE, i);
                poseStack.popPose();
            }
            ++l;
        }
        poseStack.popPose();
    }
}
