package wily.legacy.mixin.base.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.EquipmentLayerRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ARGB;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.EquipmentAsset;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(EquipmentLayerRenderer.class)
public class EquipmentLayerRendererMixin {
    @Unique
    private static final ThreadLocal<LivingEntityRenderState> legacy$wingsRenderState = new ThreadLocal<>();

    @Inject(method = "renderLayers(Lnet/minecraft/client/resources/model/EquipmentClientInfo$LayerType;Lnet/minecraft/resources/ResourceKey;Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lnet/minecraft/world/item/ItemStack;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/resources/ResourceLocation;II)V", at = @At("HEAD"))
    private <S> void legacy$storeWingsRenderState(EquipmentClientInfo.LayerType layerType, ResourceKey<EquipmentAsset> equipmentAsset, Model<? super S> model, S state, ItemStack itemStack, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int light, ResourceLocation texture, int outlineColor, int order, CallbackInfo ci) {
        if (layerType == EquipmentClientInfo.LayerType.WINGS && state instanceof LivingEntityRenderState renderState) legacy$wingsRenderState.set(renderState);
        else legacy$wingsRenderState.remove();
    }

    @Inject(method = "renderLayers(Lnet/minecraft/client/resources/model/EquipmentClientInfo$LayerType;Lnet/minecraft/resources/ResourceKey;Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lnet/minecraft/world/item/ItemStack;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/resources/ResourceLocation;II)V", at = @At("RETURN"))
    private <S> void legacy$clearWingsRenderState(EquipmentClientInfo.LayerType layerType, ResourceKey<EquipmentAsset> equipmentAsset, Model<? super S> model, S state, ItemStack itemStack, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int light, ResourceLocation texture, int outlineColor, int order, CallbackInfo ci) {
        legacy$wingsRenderState.remove();
    }

    @ModifyArgs(method = "renderLayers(Lnet/minecraft/client/resources/model/EquipmentClientInfo$LayerType;Lnet/minecraft/resources/ResourceKey;Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lnet/minecraft/world/item/ItemStack;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/resources/ResourceLocation;II)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/OrderedSubmitNodeCollector;submitModel(Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/RenderType;IIILnet/minecraft/client/renderer/texture/TextureAtlasSprite;ILnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;)V"))
    private void legacy$applyWingsHurtOverlay(Args args) {
        LivingEntityRenderState renderState = legacy$wingsRenderState.get();
        if (renderState == null) return;
        int overlay = LivingEntityRenderer.getOverlayCoords(renderState, 0.0F);
        args.set(5, overlay);
        if (overlay != OverlayTexture.NO_OVERLAY) args.set(6, ARGB.multiply(args.get(6), 0xFFFF8080));
    }
}
