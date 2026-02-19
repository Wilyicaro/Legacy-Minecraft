package wily.legacy.mixin.base.cpm.render;

/**
 * Mixin: console skins / CPM rendering glue.
 */


import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.EquipmentLayerRenderer;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.client.resources.model.EquipmentClientInfo.LayerType;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.EquipmentAsset;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.CustomModelSkins.cpm.client.CustomPlayerModelsClient;
import wily.legacy.CustomModelSkins.cpm.client.ModelTexture;
import wily.legacy.CustomModelSkins.cpm.client.PlayerRenderManager;
import wily.legacy.CustomModelSkins.cpm.shared.model.TextureSheetType;

@Mixin(EquipmentLayerRenderer.class)
public class EquipmentLayerRendererMixin {

    private static final String CPMRENDERLAYERSMETHOD = "renderLayers("
            + "Lnet/minecraft/client/resources/model/EquipmentClientInfo$LayerType;"
            + "Lnet/minecraft/resources/ResourceKey;Lnet/minecraft/client/model/Model;"
            + "Ljava/lang/Object;Lnet/minecraft/world/item/ItemStack;"
            + "Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;"
            + "ILnet/minecraft/resources/ResourceLocation;II)V";

    @Inject(
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/OrderedSubmitNodeCollector;submitModel("
                            + "Lnet/minecraft/client/model/Model;Ljava/lang/Object;"
                            + "Lcom/mojang/blaze3d/vertex/PoseStack;"
                            + "Lnet/minecraft/client/renderer/RenderType;III"
                            + "Lnet/minecraft/client/renderer/texture/TextureAtlasSprite;I"
                            + "Lnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;"
                            + ")V",
                    shift = Shift.BEFORE,
                    ordinal = 2
            ),
            method = CPMRENDERLAYERSMETHOD
    )
    private <S> void consoleskins$onSubmitArmorTrim(
            EquipmentClientInfo.LayerType layerType,
            ResourceKey<EquipmentAsset> assetKey,
            Model<? super S> model,
            S state,
            ItemStack stack,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            int packedLight,
            @Nullable ResourceLocation baseTexture,
            int tint,
            int overlay,
            CallbackInfo ci
    ) {
        if (CustomPlayerModelsClient.mc == null) return;
        CustomPlayerModelsClient.mc.getPlayerRenderManager().bindSkin(
                model,
                new ModelTexture(Sheets.ARMOR_TRIMS_SHEET, PlayerRenderManager.armor),
                layerType == LayerType.HUMANOID_LEGGINGS ? TextureSheetType.ARMOR2 : TextureSheetType.ARMOR1
        );
    }

    @Inject(
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/RenderType;armorCutoutNoCull(Lnet/minecraft/resources/ResourceLocation;)Lnet/minecraft/client/renderer/RenderType;",
                    shift = Shift.BEFORE
            ),
            method = CPMRENDERLAYERSMETHOD
    )
    private void consoleskins$bindArmorSheet(
            EquipmentClientInfo.LayerType layerType,
            ResourceKey<EquipmentAsset> assetKey,
            Model<Object> model,
            Object state,
            ItemStack stack,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            int packedLight,
            @Nullable ResourceLocation baseTexture,
            int tint,
            int overlay,
            CallbackInfo ci,
            @Local(ordinal = 1) LocalRef<ResourceLocation> resourceLocation2
    ) {
        if (layerType == LayerType.HUMANOID || layerType == LayerType.HUMANOID_LEGGINGS) {
            if (CustomPlayerModelsClient.mc == null) return;

            CustomPlayerModelsClient.mc.getPlayerRenderManager().bindSkin(
                    model,
                    new ModelTexture(resourceLocation2.get(), PlayerRenderManager.armor),
                    layerType == LayerType.HUMANOID_LEGGINGS ? TextureSheetType.ARMOR2 : TextureSheetType.ARMOR1
            );
        }
    }
}
