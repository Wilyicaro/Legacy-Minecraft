package wily.legacy.mixin.base.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.entity.HumanoidArm;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.Skins.client.render.SkinPoseRegistry;
import wily.legacy.Skins.skin.ClientSkinCache;

@Mixin(ItemInHandRenderer.class)
public class ItemInHandRendererHideHandMixin {
    @Inject(method = "renderPlayerArm", at = @At("HEAD"), cancellable = true, require = 0)
    private void renderPlayerArm(PoseStack poseStack, SubmitNodeCollector collector, int packedLight, float equippedProgress, float swingProgress, HumanoidArm arm, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.player == null) return;
        String skinId = ClientSkinCache.get(mc.player.getUUID());
        if (skinId == null || skinId.isBlank() || "auto_select".equals(skinId)) return;
        if (SkinPoseRegistry.hasPose(SkinPoseRegistry.PoseTag.HIDE_HAND, skinId)) ci.cancel();
    }
}
