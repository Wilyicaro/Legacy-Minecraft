package wily.legacy.mixin.base.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.entity.state.ArmedEntityRenderState;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.factoryapi.base.client.FactoryRenderStateExtension;
import wily.legacy.client.LegacyLivingEntityRenderState;
import wily.legacy.compat.cpm.CpmRenderCompat;
import wily.legacy.skins.client.render.RenderStateSkinIdAccess;
import wily.legacy.skins.client.render.boxloader.BoxModelManager;
import wily.legacy.skins.client.render.boxloader.ToolSlot;
import wily.legacy.skins.skin.ClientSkinAssets;
import wily.legacy.skins.skin.SkinIdUtil;

import java.util.EnumMap;

@Mixin(ItemInHandLayer.class)
public class ItemInHandLayerMixin {
    @Inject(method = "submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/client/renderer/entity/state/ArmedEntityRenderState;FF)V", at = @At("HEAD"), cancellable = true)
    private void hideHeldItemsForHostInvisiblePlayers(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int light, ArmedEntityRenderState renderState, float limbAngle, float limbDistance, CallbackInfo ci) {
        if (!(renderState instanceof AvatarRenderState)) {
            return;
        }
        LegacyLivingEntityRenderState legacyState = FactoryRenderStateExtension.Accessor.of(renderState).getExtension(LegacyLivingEntityRenderState.class);
        if (legacyState != null && legacyState.hostInvisible) {
            ci.cancel();
        }
    }

    @Inject(method = "submitArmWithItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/ArmedModel;translateToHand(Lnet/minecraft/client/renderer/entity/state/EntityRenderState;Lnet/minecraft/world/entity/HumanoidArm;Lcom/mojang/blaze3d/vertex/PoseStack;)V", shift = At.Shift.AFTER))
    private void applyToolOffset(ArmedEntityRenderState renderState, ItemStackRenderState itemState, ItemStack stack, HumanoidArm arm, PoseStack poseStack, SubmitNodeCollector collector, int light, CallbackInfo ci) {
        if (renderState instanceof AvatarRenderState avatar && CpmRenderCompat.isCpmModelActive(avatar)) return;
        if (!(renderState instanceof RenderStateSkinIdAccess access)) return;
        String skinId = access.consoleskins$getSkinId();
        if (SkinIdUtil.isBlankOrAutoSelect(skinId)) return;
        ClientSkinAssets.ResolvedSkin resolved = ClientSkinAssets.resolveSkin(access);
        Identifier modelId = resolved == null ? null : resolved.modelId();
        if (modelId == null) return;
        EnumMap<ToolSlot, float[]> offsets = BoxModelManager.getToolOffsets(modelId);
        if (offsets == null || offsets.isEmpty()) return;
        ToolSlot slot = ToolSlot.fromArm(arm);
        float[] offset = slot == null ? null : offsets.get(slot);
        if (offset == null || offset.length < 3 || (offset[0] == 0 && offset[1] == 0 && offset[2] == 0)) return;
        poseStack.translate(offset[0] / 16f, offset[1] / 16f, offset[2] / 16f);
    }
}

