package wily.legacy.mixin.base.skins.client;

import wily.legacy.Skins.skin.ClientSkinCache;
import wily.legacy.Skins.skin.ClientSkinAssets;
import wily.legacy.Skins.skin.SkinEntry;
import wily.legacy.Skins.skin.SkinPackLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.ClientAsset;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.PlayerSkin;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractClientPlayer.class)
public abstract class AbstractClientPlayerMixin {

    @Inject(method = "getSkin", at = @At("RETURN"), cancellable = true, require = 0)
    private void consoleskins$overrideSkin(CallbackInfoReturnable<PlayerSkin> cir) {
        AbstractClientPlayer player = (AbstractClientPlayer) (Object) this;

        if (ClientSkinCache.isSkinOverrideBypassed()) return;

        String skinId = ClientSkinCache.get(player.getUUID());

        if (skinId == null || skinId.isBlank() || "auto_select".equals(skinId)) return;

PlayerSkin original = cir.getReturnValue();
        if (original == null) return;

        SkinEntry entry = SkinPackLoader.getSkin(skinId);

        ResourceLocation texturePath = ClientSkinAssets.getTexture(skinId);
        if (texturePath == null && entry != null) texturePath = entry.texture();
        if (texturePath == null) return;

        try {
            Minecraft.getInstance().getTextureManager().getTexture(texturePath);
        } catch (Throwable ignored) {
        }

        ClientAsset.ResourceTexture body = new ClientAsset.ResourceTexture(texturePath, texturePath);

        ClientAsset.ResourceTexture capeTex = null;
        if (entry != null && entry.cape() != null) {
            ResourceLocation capePath = entry.cape();
            try {
                Minecraft.getInstance().getTextureManager().getTexture(capePath);
            } catch (Throwable ignored) {
            }
            capeTex = new ClientAsset.ResourceTexture(capePath, capePath);
        }

        try {
            if (player.getItemBySlot(EquipmentSlot.CHEST).is(Items.ELYTRA)) capeTex = null;
        } catch (Throwable ignored) {
        }

        Boolean slim = ClientSkinAssets.getSlimFlag(skinId);
        PlayerModelType model = slim != null ? (slim ? PlayerModelType.SLIM : PlayerModelType.WIDE) : ((entry != null && entry.slimArms()) ? PlayerModelType.SLIM : PlayerModelType.WIDE);

        ClientAsset.Texture capeFinal = capeTex;
        PlayerSkin skin = PlayerSkin.insecure(body, capeFinal, null, model);
        cir.setReturnValue(skin);
    }

}
