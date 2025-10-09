package wily.legacy.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface LegacySubmitNodeCollector {
    void submitLegacyNameTag(PoseStack poseStack, @Nullable Vec3 vec3, int i, Component component, boolean bl, int j, double d, CameraRenderState cameraRenderState, float[] color);

    void submitLoyaltyLines(PoseStack poseStack, LoyaltyLinesRenderState loyaltyLinesRenderState);

    List<LoyaltyLinesRenderer.Submit> getLoyaltyLinesSubmits();
}
