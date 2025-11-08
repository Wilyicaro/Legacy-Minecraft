package wily.legacy.mixin.base.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollection;
import net.minecraft.client.renderer.feature.NameTagFeatureRenderer;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.client.LegacyNameTag;
import wily.legacy.client.LegacySubmitNodeCollector;
import wily.legacy.client.LoyaltyLinesRenderState;
import wily.legacy.client.LoyaltyLinesRenderer;

import java.util.ArrayList;
import java.util.List;

@Mixin(SubmitNodeCollection.class)
public class SubmitNodeCollectorMixin implements LegacySubmitNodeCollector {
    @Shadow
    private boolean wasUsed;

    @Shadow
    @Final
    private NameTagFeatureRenderer.Storage nameTagSubmits;

    @Unique
    private final List<LoyaltyLinesRenderer.Submit> loyaltyLinesSubmits = new ArrayList<>();

    @Override
    public void submitLegacyNameTag(PoseStack poseStack, @Nullable Vec3 vec3, int i, Component component, boolean bl, int j, double d, CameraRenderState cameraRenderState, float[] color) {
        this.wasUsed = true;
        ((LegacyNameTag.Storage) nameTagSubmits).add(poseStack, vec3, i, component, bl, j, d, cameraRenderState, color);
    }

    @Override
    public void submitLoyaltyLines(PoseStack poseStack, LoyaltyLinesRenderState loyaltyLinesRenderState) {
        this.wasUsed = true;
        loyaltyLinesSubmits.add(new LoyaltyLinesRenderer.Submit(new Matrix4f(poseStack.last().pose()), loyaltyLinesRenderState));
    }

    @Override
    public List<LoyaltyLinesRenderer.Submit> getLoyaltyLinesSubmits() {
        return loyaltyLinesSubmits;
    }

    @Inject(method = "clear", at = @At("RETURN"))
    private void clear(CallbackInfo ci) {
        loyaltyLinesSubmits.clear();
    }
}
