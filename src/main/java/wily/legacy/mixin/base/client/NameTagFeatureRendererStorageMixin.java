package wily.legacy.mixin.base.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.feature.NameTagFeatureRenderer;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import wily.legacy.client.LegacyNameTag;

import java.util.List;

@Mixin(NameTagFeatureRenderer.Storage.class)
public class NameTagFeatureRendererStorageMixin implements LegacyNameTag.Storage {
    @Shadow @Final
    List<SubmitNodeStorage.NameTagSubmit> nameTagSubmitsNormal;

    @Shadow @Final
    List<SubmitNodeStorage.NameTagSubmit> nameTagSubmitsSeethrough;

    @Override
    public void add(PoseStack poseStack, @Nullable Vec3 vec3, int i, Component component, boolean bl, int j, double d, CameraRenderState cameraRenderState, float[] color) {
        if (vec3 != null) {
            Minecraft minecraft = Minecraft.getInstance();
            poseStack.pushPose();
            poseStack.translate(vec3.x, vec3.y + 0.5, vec3.z);
            poseStack.mulPose(cameraRenderState.orientation);
            poseStack.scale(0.025F, -0.025F, 0.025F);
            Matrix4f matrix4f = new Matrix4f(poseStack.last().pose());
            float f = -minecraft.font.width(component) / 2.0F;
            int k = (int)(minecraft.options.getBackgroundOpacity(0.25F) * 255.0F) << 24;
            if (bl) {
                this.nameTagSubmitsNormal.add(LegacyNameTag.withColor(new SubmitNodeStorage.NameTagSubmit(matrix4f, f, i, component, LightTexture.lightCoordsWithEmission(j, 2), -1, 0, d), color));
                this.nameTagSubmitsSeethrough.add(LegacyNameTag.withColor(new SubmitNodeStorage.NameTagSubmit(matrix4f, f, i, component, j, -2130706433, k, d), color));
            } else {
                this.nameTagSubmitsNormal.add(LegacyNameTag.withColor(new SubmitNodeStorage.NameTagSubmit(matrix4f, f, i, component, j, -2130706433, k, d), color));
            }

            poseStack.popPose();
        }
    }
}
