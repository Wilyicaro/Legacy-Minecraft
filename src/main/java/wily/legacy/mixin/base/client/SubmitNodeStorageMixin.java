package wily.legacy.mixin.base.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollection;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import wily.legacy.client.LegacySubmitNodeCollector;
import wily.legacy.client.LoyaltyLinesRenderState;
import wily.legacy.client.LoyaltyLinesRenderer;

import java.util.List;

@Mixin(SubmitNodeStorage.class)
public abstract class SubmitNodeStorageMixin implements LegacySubmitNodeCollector {
    @Shadow public abstract SubmitNodeCollection order(int i);

    @Override
    public void submitLegacyNameTag(PoseStack poseStack, @Nullable Vec3 vec3, int i, Component component, boolean bl, int j, double d, CameraRenderState cameraRenderState, float[] color) {
        ((LegacySubmitNodeCollector)order(0)).submitLegacyNameTag(poseStack, vec3, i, component, bl, j, d, cameraRenderState, color);
    }

    @Override
    public void submitLoyaltyLines(PoseStack poseStack, LoyaltyLinesRenderState loyaltyLinesRenderState) {
        ((LegacySubmitNodeCollector)order(0)).submitLoyaltyLines(poseStack, loyaltyLinesRenderState);
    }

    @Override
    public List<LoyaltyLinesRenderer.Submit> getLoyaltyLinesSubmits() {
        return ((LegacySubmitNodeCollector)order(0)).getLoyaltyLinesSubmits();
    }
}
