package wily.legacy.mixin.base.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
//? if <1.21.2 {
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import wily.legacy.Legacy4JClient;
//?}
//? if >=1.21.2 && <1.21.4 {
/*import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.resources.model.BakedModel;
import wily.factoryapi.base.client.FactoryRenderStateExtension;
import wily.legacy.client.LegacyLivingEntityRenderState;
*///?}
//? if >=1.21.4 {
/*import net.minecraft.client.renderer.entity.state.ArmedEntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import wily.factoryapi.base.client.FactoryRenderStateExtension;
import wily.legacy.client.LegacyLivingEntityRenderState;
*///?}
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.compat.cpm.CpmRenderCompat;
import wily.legacy.skins.client.render.RenderStateSkinIdAccess;
import wily.legacy.skins.client.render.boxloader.BoxModelManager;
import wily.legacy.skins.client.render.boxloader.ToolSlot;
import wily.legacy.skins.skin.ClientSkinAssets;
import wily.legacy.skins.skin.ClientSkinCache;
import wily.legacy.skins.skin.SkinFairness;
import wily.legacy.skins.skin.SkinIdUtil;

import java.util.EnumMap;

@Mixin(ItemInHandLayer.class)
public class ItemInHandLayerMixin {
    //? if <1.21.2 {
    @Inject(method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/world/entity/LivingEntity;FFFFFF)V", at = @At("HEAD"), cancellable = true)
    private void legacy$hideHeldItemsForHostInvisiblePlayers(PoseStack poseStack, MultiBufferSource multiBufferSource, int light, LivingEntity entity, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float headYaw, float headPitch, CallbackInfo ci) {
        if (entity instanceof Player player && Legacy4JClient.isHostInvisible(player)) {
            ci.cancel();
        }
    }

    @Inject(method = "renderArmWithItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/ArmedModel;translateToHand(Lnet/minecraft/world/entity/HumanoidArm;Lcom/mojang/blaze3d/vertex/PoseStack;)V", shift = At.Shift.AFTER), require = 0)
    private void consoleskins$applyToolOffset(LivingEntity entity, ItemStack stack, ItemDisplayContext context, HumanoidArm arm, PoseStack poseStack, MultiBufferSource bufferSource, int light, CallbackInfo ci) {
        consoleskins$applyToolOffset(consoleskins$modelId(entity), arm, poseStack);
    }
    //?}

    //? if >=1.21.2 && <1.21.4 {
    /*@Inject(method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;FF)V", at = @At("HEAD"), cancellable = true)
    private void legacy$hideHeldItemsForHostInvisiblePlayers(PoseStack poseStack, MultiBufferSource multiBufferSource, int light, LivingEntityRenderState renderState, float limbSwing, float limbSwingAmount, CallbackInfo ci) {
        LegacyLivingEntityRenderState legacyState = FactoryRenderStateExtension.Accessor.of(renderState).getExtension(LegacyLivingEntityRenderState.class);
        if (legacyState != null && legacyState.hostInvisible) {
            ci.cancel();
        }
    }

    @Inject(method = "renderArmWithItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/ArmedModel;translateToHand(Lnet/minecraft/world/entity/HumanoidArm;Lcom/mojang/blaze3d/vertex/PoseStack;)V", shift = At.Shift.AFTER), require = 0)
    private void consoleskins$applyToolOffset(LivingEntityRenderState renderState, BakedModel model, ItemStack stack, ItemDisplayContext context, HumanoidArm arm, PoseStack poseStack, MultiBufferSource bufferSource, int light, CallbackInfo ci) {
        if (CpmRenderCompat.isCpmModelActive(renderState)) return;
        consoleskins$applyToolOffset(consoleskins$modelId(renderState), arm, poseStack);
    }
    *///?}

    //? if >=1.21.4 {
    /*@Inject(method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/client/renderer/entity/state/ArmedEntityRenderState;FF)V", at = @At("HEAD"), cancellable = true)
    private void legacy$hideHeldItemsForHostInvisiblePlayers(PoseStack poseStack, MultiBufferSource multiBufferSource, int light, ArmedEntityRenderState renderState, float limbSwing, float limbSwingAmount, CallbackInfo ci) {
        LegacyLivingEntityRenderState legacyState = FactoryRenderStateExtension.Accessor.of(renderState).getExtension(LegacyLivingEntityRenderState.class);
        if (legacyState != null && legacyState.hostInvisible) {
            ci.cancel();
        }
    }

    @Inject(method = "renderArmWithItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/ArmedModel;translateToHand(Lnet/minecraft/world/entity/HumanoidArm;Lcom/mojang/blaze3d/vertex/PoseStack;)V", shift = At.Shift.AFTER), require = 0)
    private void consoleskins$applyToolOffset(ArmedEntityRenderState renderState, ItemStackRenderState itemState, HumanoidArm arm, PoseStack poseStack, MultiBufferSource bufferSource, int light, CallbackInfo ci) {
        if (CpmRenderCompat.isCpmModelActive(renderState)) return;
        consoleskins$applyToolOffset(consoleskins$modelId(renderState), arm, poseStack);
    }
    *///?}

    @Unique
    private static void consoleskins$applyToolOffset(ResourceLocation modelId, HumanoidArm arm, PoseStack poseStack) {
        if (modelId == null || arm == null || poseStack == null) return;
        EnumMap<ToolSlot, float[]> offsets = BoxModelManager.getToolOffsets(modelId);
        if (offsets == null || offsets.isEmpty()) return;
        ToolSlot slot = ToolSlot.fromArm(arm);
        float[] offset = slot == null ? null : offsets.get(slot);
        if (offset == null || offset.length < 3 || offset[0] == 0.0F && offset[1] == 0.0F && offset[2] == 0.0F) return;
        poseStack.translate(offset[0] / 16.0F, offset[1] / 16.0F, offset[2] / 16.0F);
    }

    @Unique
    private static ResourceLocation consoleskins$modelId(Object state) {
        if (!(state instanceof RenderStateSkinIdAccess access)) return null;
        String skinId = access.consoleskins$getSkinId();
        if (SkinIdUtil.isBlankOrAutoSelect(skinId)) return null;
        ClientSkinAssets.ResolvedSkin resolved = ClientSkinAssets.resolveSkin(access);
        return resolved == null ? null : resolved.modelId();
    }

    //? if <1.21.2 {
    @Unique
    private static ResourceLocation consoleskins$modelId(LivingEntity entity) {
        if (!(entity instanceof Player player)) return null;
        String skinId = SkinFairness.effectiveSkinId(Minecraft.getInstance(), ClientSkinCache.get(player.getUUID(), player.getScoreboardName()));
        if (SkinIdUtil.isBlankOrAutoSelect(skinId)) return null;
        ClientSkinAssets.ResolvedSkin resolved = ClientSkinAssets.resolveSkin(skinId, player.getUUID());
        return resolved == null ? null : resolved.modelId();
    }
    //?}
}
